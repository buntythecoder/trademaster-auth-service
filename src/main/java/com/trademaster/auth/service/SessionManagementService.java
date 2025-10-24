package com.trademaster.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.auth.context.SessionCreationContext;
import com.trademaster.auth.dto.SessionInfo;
import com.trademaster.auth.dto.SessionTimestamp;
import com.trademaster.auth.entity.SessionSettings;
import com.trademaster.auth.entity.UserSession;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import com.trademaster.auth.repository.SessionSettingsRepository;
import com.trademaster.auth.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Session Management Service using Redis
 * 
 * Features:
 * - Session creation and validation
 * - Concurrent session management
 * - Session cleanup and expiration
 * - Device-based session tracking
 * - Security monitoring
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final RedisTemplate<String, String> sessionRedisTemplate;
    private final UserSessionRepository userSessionRepository;
    private final SessionSettingsRepository sessionSettingsRepository;
    private final AuditService auditService;
    private final CircuitBreakerService circuitBreakerService;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .readTimeout(java.time.Duration.ofSeconds(30))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${trademaster.security.session.max-concurrent:3}")
    private int defaultMaxConcurrentSessions;

    @Value("${trademaster.security.session.timeout:30}") // 30 minutes default
    private int defaultSessionTimeoutMinutes;

    private static final String SESSION_PREFIX = "trademaster:session:";
    private static final String USER_SESSIONS_PREFIX = "trademaster:user_sessions:";
    private static final String DEVICE_SESSIONS_PREFIX = "trademaster:device_sessions:";
    
    // Context cache for legacy session operations
    private final Map<String, LegacySessionContext> contextCache = new ConcurrentHashMap<>();

    /**
     * Create a new session with enhanced settings support
     */
    @Transactional(readOnly = false)
    public UserSession createUserSession(Long userId, String deviceFingerprint, HttpServletRequest request) {
        return SafeOperations.safelyToResult(() -> {
            log.info("Creating session for user: {}", userId);
            
            SessionSettings settings = getOrCreateSessionSettings(userId);
            SessionCreationContext context = new SessionCreationContext(String.valueOf(userId), deviceFingerprint, request, settings);
            
            // Sequential function application instead of compose to avoid type issues
            context = enforceConcurrentSessionLimit(context);
            context = createNewSession(context);
            context = saveAndCacheSession(context);
            
            return context.getUserSession();
        }).orElseThrow(error -> new RuntimeException("Failed to create user session: " + error));
    }

    /**
     * Create a new session (legacy method for backward compatibility)
     */
    public String createSession(Long userId, String deviceFingerprint, String ipAddress, 
                               String userAgent, Map<String, Object> sessionData) {
        Result<String, String> result = SafeOperations.safelyToResult(() -> {
            String sessionId = generateSessionId(userId);
            LegacySessionContext context = new LegacySessionContext(userId, deviceFingerprint, ipAddress, userAgent, sessionData, sessionId);
            
            // Cache context for pipeline methods
            cacheLegacyContext(context);
            
            // Enforce session limit
            enforceSessionLimit(userId, deviceFingerprint);
            
            // Build and store session info
            SessionInfo sessionInfo = SessionInfo.builder()
                .sessionId(sessionId)
                .userId(String.valueOf(userId))
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .active(true)
                .metadata(sessionData != null ? sessionData : new HashMap<>())
                .build();

            // Store in Redis
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson;
            try {
                sessionJson = objectMapper.writeValueAsString(sessionInfo);
            } catch (Exception e) {
                log.warn("Failed to serialize session info: {}", e.getMessage());
                sessionJson = "{}";
            }
            sessionRedisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofMinutes(defaultSessionTimeoutMinutes));

            // Track sessions
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            sessionRedisTemplate.opsForSet().add(userSessionsKey, sessionId);
            sessionRedisTemplate.expire(userSessionsKey, Duration.ofMinutes(defaultSessionTimeoutMinutes));

            String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + deviceFingerprint;
            sessionRedisTemplate.opsForSet().add(deviceSessionsKey, sessionId);
            sessionRedisTemplate.expire(deviceSessionsKey, Duration.ofMinutes(defaultSessionTimeoutMinutes));

            // Audit
            auditService.logAuthenticationEvent(userId, "SESSION_CREATED", "SUCCESS", 
                ipAddress, userAgent, deviceFingerprint, 
                Map.of("session_id", sessionId, "concurrent_sessions", (int) getUserSessionCount(Long.valueOf(userId))), 
                sessionId);

            log.info("Session created for user {} with ID: {}", userId, sessionId);
            
            // Clean up cache
            contextCache.remove(sessionId);
            
            return sessionId;
        });
        
        return result
            .mapError(error -> {
                log.error("Failed to create session for user {}: {}", userId, error);
                return new RuntimeException("Session creation failed: " + error);
            })
            .orElseThrow(RuntimeException::new);
    }

    /**
     * Validate and retrieve session
     */
    public SessionInfo getSession(String sessionId) {
        return SafeOperations.safely(() -> {
            String sessionKey = SESSION_PREFIX + sessionId;
            return Optional.ofNullable(sessionRedisTemplate.opsForValue().get(sessionKey))
                .flatMap(sessionJson -> deserializeSessionInfo(sessionId, sessionJson))
                .filter(sessionInfo -> sessionInfo.isActive())
                .orElseGet(() -> {
                    log.debug("Session not found or inactive: {}", sessionId);
                    return null;
                });
        }).orElse(null);
    }

    /**
     * Update session activity
     */
    public void updateSessionActivity(String sessionId, String ipAddress) {
        Optional.ofNullable(getSession(sessionId))
            .map(sessionInfo -> updateSessionInfo(sessionInfo, ipAddress))
            .ifPresent(sessionInfo -> {
                Result<SessionInfo, String> result = SafeOperations.safelyToResult(() -> {
                    String sessionKey = SESSION_PREFIX + sessionId;
                    String sessionJson;
                    try {
                        sessionJson = objectMapper.writeValueAsString(sessionInfo);
                    } catch (Exception e) {
                        log.warn("Failed to serialize session info for update: {}", e.getMessage());
                        sessionJson = "{}";
                    }
                    
                    sessionRedisTemplate.opsForValue().set(
                        sessionKey, 
                        sessionJson, 
                        Duration.ofMinutes(defaultSessionTimeoutMinutes)
                    );
                    log.debug("Updated activity for session: {}", sessionId);
                    return sessionInfo;
                });
                
                result.mapError(error -> {
                    log.error("Failed to update session activity {}: {}", sessionId, error);
                    return error;
                });
            });
    }

    /**
     * Invalidate a specific session
     */
    public void invalidateSession(String sessionId, String reason) {
        try {
            Optional.ofNullable(getSession(sessionId))
                .ifPresent(sessionInfo -> {
                    // Remove from Redis
                    String sessionKey = SESSION_PREFIX + sessionId;
                    sessionRedisTemplate.delete(sessionKey);

                    // Remove from user sessions set
                    String userSessionsKey = USER_SESSIONS_PREFIX + sessionInfo.getUserId();
                    sessionRedisTemplate.opsForSet().remove(userSessionsKey, sessionId);

                    // Remove from device sessions set
                    String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + sessionInfo.getDeviceFingerprint();
                    sessionRedisTemplate.opsForSet().remove(deviceSessionsKey, sessionId);

                    // Log session invalidation
                    SafeOperations.safely(() -> Long.parseLong(sessionInfo.getUserId()))
                        .ifPresent(userId -> auditService.logAuthenticationEvent(userId, "SESSION_EXPIRED", "SUCCESS",
                            sessionInfo.getIpAddress(), sessionInfo.getUserAgent(), sessionInfo.getDeviceFingerprint(),
                            Map.of("reason", reason, "session_id", sessionId), sessionId));

                    log.info("Session invalidated: {} (reason: {})", sessionId, reason);
                });

        } catch (Exception e) {
            log.error("Failed to invalidate session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Invalidate all sessions for a user
     */
    public void invalidateAllUserSessions(Long userId, String reason) {
        VirtualThreadFactory.INSTANCE.runAsync(() -> {
            SafeOperations.safelyToResult(() -> {
                String userSessionsKey = USER_SESSIONS_PREFIX + userId;
                Set<String> sessionIds = sessionRedisTemplate.opsForSet().members(userSessionsKey);
                
                Optional.ofNullable(sessionIds)
                    .map(Set::stream)
                    .ifPresent(stream -> stream.forEach(sessionId -> invalidateSession(sessionId, reason)));

                sessionRedisTemplate.delete(userSessionsKey);

                log.info("Invalidated {} sessions for user {} (reason: {})", 
                       Optional.ofNullable(sessionIds).map(Set::size).orElse(0), userId, reason);
                       
                return sessionIds != null ? sessionIds.size() : 0;
            })
            .mapError(error -> {
                log.error("Failed to invalidate all sessions for user {}: {}", userId, error);
                return error;
            });
        });
    }

    /**
     * Get active session count for user
     */
    public long getUserSessionCount(Long userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Long count = sessionRedisTemplate.opsForSet().size(userSessionsKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to get session count for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        try {
            // This is handled automatically by Redis TTL, but we can add additional cleanup logic here
            log.debug("Session cleanup completed");
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage());
        }
    }

    /**
     * Enforce concurrent session limit
     */
    private void enforceSessionLimit(Long userId, String deviceFingerprint) {
        Optional.of((int) getUserSessionCount(userId))
            .filter(count -> count >= defaultMaxConcurrentSessions)
            .ifPresent(count -> {
                String userSessionsKey = USER_SESSIONS_PREFIX + userId;
                Optional.ofNullable(sessionRedisTemplate.opsForSet().members(userSessionsKey))
                    .filter(sessionIds -> !sessionIds.isEmpty())
                    .map(this::findOldestSession)
                    .ifPresent(oldestSessionId -> {
                        invalidateSession(oldestSessionId, "CONCURRENT_LIMIT");
                        log.info("Removed oldest session {} for user {} due to concurrent limit", 
                            oldestSessionId, userId);
                    });
            });
    }
    
    private void enforceSessionLimit(String userId, String deviceFingerprint) {
        // String version for legacy compatibility
        enforceSessionLimit(Long.parseLong(userId), deviceFingerprint);
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId(Long userId) {
        return String.format("%d_%d_%s", 
            userId, 
            System.currentTimeMillis(), 
            java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Enhanced session validation and retrieval
     */
    public UserSession getUserSession(String sessionId) {
        return userSessionRepository.findBySessionIdAndActiveTrue(sessionId).orElse(null);
    }

    /**
     * Update session activity with settings-aware extension
     */
    @Transactional(readOnly = false)
    public void updateUserSessionActivity(String sessionId, HttpServletRequest request) {
        Optional.ofNullable(getUserSession(sessionId))
            .map(session -> processSessionActivityUpdate(session, request))
            .ifPresent(session -> {
                userSessionRepository.save(session);
                SessionSettings settings = getOrCreateSessionSettings(Long.valueOf(session.getUserId()));
                storeSessionInRedis(session, settings.getSessionTimeoutMinutes());
            });
    }

    /**
     * Terminate a specific session
     */
    @Transactional(readOnly = false)
    public void terminateSession(String sessionId, String reason) {
        log.info("Terminating session: {} (reason: {})", sessionId, reason);
        
        Optional.ofNullable(getUserSession(sessionId))
            .ifPresent(session -> {
                session.terminate();
                userSessionRepository.save(session);

                // Remove from Redis
                sessionRedisTemplate.delete(SESSION_PREFIX + sessionId);

                log.info("Session terminated: {} for user: {}", sessionId, session.getUserId());
            });
    }

    /**
     * Terminate all sessions for user
     */
    @Transactional(readOnly = false)
    public void terminateAllUserSessions(Long userId, String reason) {
        log.info("Terminating all sessions for user: {} (reason: {})", userId, reason);
        
        VirtualThreadFactory.INSTANCE.runAsync(() -> {
            userSessionRepository.terminateAllSessionsForUser(userId);
            
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Optional.ofNullable(sessionRedisTemplate.opsForSet().members(userSessionsKey))
                .map(Set::stream)
                .ifPresent(stream -> stream.forEach(sessionId -> 
                    sessionRedisTemplate.delete(SESSION_PREFIX + sessionId)));
            
            sessionRedisTemplate.delete(userSessionsKey);
            
            log.info("All sessions terminated for user: {}", userId);
        });
    }

    /**
     * Get session settings for user
     */
    public SessionSettings getSessionSettings(Long userId) {
        return getOrCreateSessionSettings(userId);
    }

    /**
     * Update session settings
     */
    @Transactional(readOnly = false)
    public SessionSettings updateSessionSettings(Long userId, SessionSettings settings) {
        settings.setUserId(userId);
        return sessionSettingsRepository.save(settings);
    }

    /**
     * Cleanup expired sessions (scheduled task)
     */
    @Scheduled(cron = "0 */5 * * * ?") // Every 5 minutes
    @Transactional(readOnly = false)
    public void cleanupExpiredUserSessions() {
        try {
            performSessionCleanup();
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = false)
    public void performSessionCleanup() {
        log.debug("Cleaning up expired sessions");

        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.deactivateExpiredSessions(now);

        LocalDateTime cutoffDate = now.minusDays(7);
        userSessionRepository.deleteByExpiresAtBefore(cutoffDate);

        log.debug("Session cleanup completed successfully");
    }

    // Helper methods and functional support classes
    
    // Using imported SessionCreationContext class instead of local record
    
    private record LegacySessionContext(Long userId, String deviceFingerprint, String ipAddress,
                                       String userAgent, Map<String, Object> sessionData, String sessionId) {}
    
    private SessionCreationContext enforceConcurrentSessionLimit(SessionCreationContext context) {
        long activeSessions = userSessionRepository.countActiveSessionsForUser(Long.valueOf(context.getUserId()), LocalDateTime.now());
        
        Optional.of(context.getSettings())
            .filter(settings -> !settings.isWithinConcurrentSessionLimit((int) activeSessions))
            .ifPresent(settings -> {
                List<UserSession> sessions = userSessionRepository.findSessionsByUserIdOrderByLastActivity(Long.valueOf(context.getUserId()));
                sessions.stream()
                    .reduce((first, second) -> second) // Get last element (oldest)
                    .ifPresent(oldestSession -> terminateSession(oldestSession.getSessionId(), "CONCURRENT_LIMIT"));
            });
        
        return context;
    }
    
    private SessionCreationContext createNewSession(SessionCreationContext context) {
        String sessionId = UUID.randomUUID().toString();
        String ipAddress = extractIpAddress(context.getRequest());
        String userAgent = context.getRequest().getHeader("User-Agent");
        String location = extractLocationFromRequest(context.getRequest());

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(Long.valueOf(context.getUserId()))
                .deviceFingerprint(context.getDeviceFingerprint())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .location(location)
                .active(true)
                .build();
                
        // Set the session in the context
        SessionCreationContext updatedContext = new SessionCreationContext(
            context.getUserId(), 
            context.getDeviceFingerprint(), 
            context.getRequest(), 
            context.getSettings()
        );
        updatedContext.setUserSession(session);
        return updatedContext;
    }
    
    private SessionCreationContext saveAndCacheSession(SessionCreationContext context) {
        UserSession session = context.getUserSession();
        UserSession savedSession = userSessionRepository.save(session);

        SessionSettings settings = getOrCreateSessionSettings(Long.valueOf(session.getUserId()));
        storeSessionInRedis(savedSession, settings.getSessionTimeoutMinutes());
        
        log.info("Session created: {} for user: {}", savedSession.getSessionId(), savedSession.getUserId());
        
        // Update the context with the saved session
        context.setUserSession(savedSession);
        return context;
    }
    
    private SessionSettings getOrCreateSessionSettings(Long userId) {
        return sessionSettingsRepository.findByUserId(userId)
            .orElseGet(() -> {
                SessionSettings settings = SessionSettings.builder()
                    .userId(userId)
                    .maxConcurrentSessions(defaultMaxConcurrentSessions)
                    .sessionTimeoutMinutes(defaultSessionTimeoutMinutes)
                    .build();
                return sessionSettingsRepository.save(settings);
            });
    }
    
    private String enforceSessionLimit(LegacySessionContext context) {
        enforceSessionLimit(context.userId(), context.deviceFingerprint());
        return context.sessionId();
    }
    
    private String buildSessionInfo(LegacySessionContext context) {
        return context.sessionId(); // Context carries through the pipeline
    }
    
    private String storeSessionInRedis(LegacySessionContext context) {
        SessionInfo sessionInfo = SessionInfo.builder()
            .sessionId(context.sessionId())
            .userId(context.userId().toString())
            .deviceFingerprint(context.deviceFingerprint())
            .ipAddress(context.ipAddress())
            .userAgent(context.userAgent())
            .createdAt(LocalDateTime.now())
            .lastAccessedAt(LocalDateTime.now())
            .active(true)
            .metadata(context.sessionData() != null ? context.sessionData() : new HashMap<>())
            .build();

        return SafeOperations.safely(() -> {
            String sessionKey = SESSION_PREFIX + context.sessionId();
            String sessionJson;
            try {
                sessionJson = objectMapper.writeValueAsString(sessionInfo);
            } catch (Exception e) {
                log.warn("Failed to serialize session info for Redis storage: {}", e.getMessage());
                sessionJson = "{}";
            }
            
            sessionRedisTemplate.opsForValue().set(
                sessionKey, 
                sessionJson, 
                Duration.ofMinutes(defaultSessionTimeoutMinutes)
            );
            return context.sessionId();
        }).orElse(context.sessionId());
    }
    
    private String trackUserSession(String sessionId) {
        LegacySessionContext context = getCurrentLegacyContext(sessionId);
        String userSessionsKey = USER_SESSIONS_PREFIX + context.userId();
        sessionRedisTemplate.opsForSet().add(userSessionsKey, sessionId);
        sessionRedisTemplate.expire(userSessionsKey, Duration.ofMinutes(defaultSessionTimeoutMinutes));
        return sessionId;
    }
    
    private String trackDeviceSession(String sessionId) {
        LegacySessionContext context = getCurrentLegacyContext(sessionId);
        String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + context.deviceFingerprint();
        sessionRedisTemplate.opsForSet().add(deviceSessionsKey, sessionId);
        sessionRedisTemplate.expire(deviceSessionsKey, Duration.ofMinutes(defaultSessionTimeoutMinutes));
        return sessionId;
    }
    
    private String auditSessionCreation(String sessionId) {
        LegacySessionContext context = getCurrentLegacyContext(sessionId);
        auditService.logAuthenticationEvent(context.userId(), "SESSION_CREATED", "SUCCESS", 
            context.ipAddress(), context.userAgent(), context.deviceFingerprint(), 
            Map.of("session_id", sessionId, "concurrent_sessions", getUserSessionCount(context.userId())), 
            sessionId);

        log.info("Session created for user {} with ID: {}", context.userId(), sessionId);
        return sessionId;
    }
    
    // Session context cache for pipeline processing
    
    private LegacySessionContext getCurrentLegacyContext(String sessionId) {
        return contextCache.get(sessionId);
    }
    
    private void cacheLegacyContext(LegacySessionContext context) {
        contextCache.put(context.sessionId(), context);
    }
    
   
    
    private UserSession processSessionActivityUpdate(UserSession session, HttpServletRequest request) {
        SessionSettings settings = getOrCreateSessionSettings(Long.valueOf(session.getUserId()));
        
        session.updateLastActivity();
        session.setIpAddress(extractIpAddress(request));
        
        return Optional.of(settings)
            .filter(SessionSettings::shouldExtendOnActivity)
            .map(s -> {
                session.extendSession(s.getSessionTimeoutMinutes());
                return session;
            })
            .orElse(session);
    }
    
    private SessionInfo buildSessionInfo(UserSession session) {
        return SessionInfo.builder()
                .sessionId(session.getSessionId())
                .userId(String.valueOf(session.getUserId()))
                .deviceFingerprint(session.getDeviceFingerprint())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .lastAccessedAt(session.getLastActivity())
                .active(session.isActive())
                .metadata(session.getAttributes() != null ?
                    Map.of("attributes", session.getAttributes()) : Map.of())
                .build();
    }
    
    private final Map<String, Function<UserSession, SessionManagementResult>> sessionActionStrategies = Map.of(
        "REFRESH", session -> processSessionRefresh(session),
        "EXTEND", session -> processSessionRefresh(session),
        "TERMINATE", session -> processSessionTermination(session),
        "INVALIDATE", session -> processSessionTermination(session),
        "VALIDATE", session -> processSessionValidation(session)
    );
    
    private SessionManagementResult processSessionAction(UserSession session, String action) {
        Long userId = Long.valueOf(session.getUserId());
        
        return sessionActionStrategies.entrySet().stream()
            .filter(entry -> entry.getKey().equals(action.toUpperCase()))
            .findFirst()
            .map(entry -> entry.getValue().apply(session))
            .orElse(SessionManagementResult.builder()
                .sessionId(session.getSessionId())
                .userId(String.valueOf(userId))
                .action(action)
                .success(false)
                .message("Unknown action: " + action)
                .build());
    }
    
    private SessionManagementResult processSessionRefresh(UserSession session) {
        SessionSettings settings = getOrCreateSessionSettings(Long.valueOf(session.getUserId()));
        session.extendSession(settings.getSessionTimeoutMinutes());
        userSessionRepository.save(session);
        storeSessionInRedis(session, settings.getSessionTimeoutMinutes());
        
        return SessionManagementResult.builder()
            .sessionId(session.getSessionId())
            .userId(String.valueOf(session.getUserId()))
            .action("REFRESH")
            .success(true)
            .message("Session extended successfully")
            .build();
    }
    
    private SessionManagementResult processSessionTermination(UserSession session) {
        terminateSession(session.getSessionId(), "USER_REQUESTED");
        
        return SessionManagementResult.builder()
            .sessionId(session.getSessionId())
            .userId(String.valueOf(session.getUserId()))
            .action("TERMINATE")
            .success(true)
            .message("Session terminated successfully")
            .build();
    }
    
    private SessionManagementResult processSessionValidation(UserSession session) {
        boolean isValid = session.isActive() && session.getExpiresAt().isAfter(LocalDateTime.now());
        
        return SessionManagementResult.builder()
            .sessionId(session.getSessionId())
            .userId(String.valueOf(session.getUserId()))
            .action("VALIDATE")
            .success(isValid)
            .message(isValid ? "Session is valid" : "Session is expired or inactive")
            .build();
    }
    
    private SessionManagementResult createNotFoundResult(String sessionId, String action) {
        return SessionManagementResult.builder()
            .sessionId(sessionId)
            .userId(null)
            .action(action)
            .success(false)
            .message("Session not found")
            .build();
    }
    

    private String parseGeoIpResponse(Response response, String ipAddress) {
        return SafeOperations.safely(() -> {
            return Optional.of(response)
                .filter(Response::isSuccessful)
                .map(Response::body)
                .filter(Objects::nonNull)
                .flatMap(body -> SafeOperations.safely(() -> {
                    try {
                        String bodyString = body.string();
                        return objectMapper.readValue(bodyString, Map.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse geo IP response: {}", e.getMessage());
                        return Map.of();
                    }
                }))
                .filter(responseMap -> "success".equals(responseMap.get("status")))
                .map(responseMap -> String.format("%s, %s, %s", 
                    responseMap.getOrDefault("city", "Unknown"),
                    responseMap.getOrDefault("regionName", "Unknown"),
                    responseMap.getOrDefault("country", "Unknown")))
                .orElse(String.format("External IP: %s", ipAddress));
        }).orElse(String.format("External IP: %s", ipAddress));
    }
    

    private void storeSessionInRedis(UserSession session, int timeoutMinutes) {
        SafeOperations.safelyToResult(() -> {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            SessionInfo sessionInfo = buildSessionInfo(session);
            
            String sessionJson;
            try {
                sessionJson = objectMapper.writeValueAsString(sessionInfo);
            } catch (Exception e) {
                log.warn("Failed to serialize session info for Redis: {}", e.getMessage());
                sessionJson = "{}";
            }
            sessionRedisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofMinutes(timeoutMinutes));
            return sessionInfo;
        })
        .mapError(error -> {
            log.warn("Failed to store session in Redis: {}", error);
            return error;
        });
    }

    private String extractIpAddress(HttpServletRequest request) {
        return SafeOperations.safelyToResult(() -> {
            return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .filter(header -> !header.isEmpty())
                .map(header -> header.split(",")[0].trim())
                .or(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                    .filter(header -> !header.isEmpty()))
                .orElse(request.getRemoteAddr());
        })
        .mapError(error -> {
            log.warn("Error extracting IP address: {}", error);
            return request.getRemoteAddr();
        })
        .orElse("127.0.0.1");
    }

    /**
     * Manage session with different actions
     */
    public SessionManagementResult manageSession(String sessionId, String action) {
        return SafeOperations.safelyToResult(() ->
            Optional.ofNullable(getUserSession(sessionId))
                .map(session -> processSessionAction(session, action))
                .orElseGet(() -> createNotFoundResult(sessionId, action))
        )
        .mapError(error -> {
            log.error("Failed to manage session {} with action {}: {}", sessionId, action, error);
            return createErrorResult(sessionId, action, error);
        })
        .orElse(createErrorResult(sessionId, action, "Unknown error"));
    }

    private String extractLocationFromRequest(HttpServletRequest request) {
        return SafeOperations.safelyToResult(() -> {
            String ipAddress = getClientIpAddress(request);
            
            return Optional.of(ipAddress)
                .filter(ip -> !isPrivateIpAddress.test(ip))
                .map(this::performGeoIpLookup)
                .orElse(String.format("Internal Network - IP: %s", ipAddress));
        })
        .mapError(error -> {
            log.warn("Failed to extract location from request: {}", error);
            return "Location extraction failed";
        })
        .orElse("Location extraction failed");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .filter(header -> !header.isEmpty())
            .map(header -> header.split(",")[0].trim())
            .orElse(request.getRemoteAddr());
    }
    
    /**
     * Deserialize session info from JSON string using functional approach
     */
    private Optional<SessionInfo> deserializeSessionInfo(String sessionId, String sessionJson) {
        return SafeOperations.safely(() -> {
            SessionInfo sessionInfo;
            try {
                sessionInfo = objectMapper.readValue(sessionJson, SessionInfo.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize session info: {}", e.getMessage());
                sessionInfo = null;
            }
            // Ensure sessionId is set in case it's missing from JSON
            Optional.of(sessionInfo)
                .filter(info -> info.getSessionId() == null)
                .ifPresent(info -> info.setSessionId(sessionId));
            return sessionInfo;
        });
    }

    private final Predicate<String> isPrivateIpAddress = ip ->
        List.of("192.168.", "10.", "172.", "127.0.0.1")
            .stream()
            .anyMatch(pattern -> ip.equals(pattern) || ip.startsWith(pattern));

    /**
     * Perform geo IP lookup with circuit breaker protection
     *
     * MANDATORY: Circuit Breaker - Rule #25
     * MANDATORY: Functional Programming - Rule #3 (no try-catch)
     * MANDATORY: Virtual Threads - Rule #12
     */
    private String performGeoIpLookup(String ipAddress) {
        return circuitBreakerService.executeExternalApiOperation(
            "geoIpLookup",
            () -> {
                try {
                    String apiUrl = String.format("http://ip-api.com/json/%s?fields=country,city,regionName", ipAddress);

                    Request request = new Request.Builder()
                        .url(apiUrl)
                        .build();

                    Response response = httpClient.newCall(request).execute();
                    String result = parseGeoIpResponse(response, ipAddress);
                    response.close();
                    return result;
                } catch (IOException e) {
                    throw new RuntimeException("Geo IP lookup HTTP call failed: " + e.getMessage(), e);
                }
            }
        )
        .thenApply(result -> result
            .map(geoInfo -> geoInfo)
            .mapError(error -> {
                log.warn("Geo IP lookup failed (circuit breaker): {}", error);
                return String.format("External IP: %s (lookup failed)", ipAddress);
            })
            .orElse(String.format("External IP: %s", ipAddress))
        )
        .join(); // Block to maintain synchronous API (safe with virtual threads)
    }

    private String findOldestSession(Set<String> sessionIds) {
        return sessionIds.stream()
            .map(sessionId -> {
                String timeKey = SESSION_PREFIX + sessionId + ":created";
                return SafeOperations.safely(() -> sessionRedisTemplate.opsForValue().get(timeKey))
                    .flatMap(timeStr -> Optional.ofNullable(timeStr)
                        .map(str -> {
                            long time = Long.parseLong(str);
                            return new SessionTimestamp(sessionId, LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC));
                        }))
                    .orElse(null);
            })
            .filter(Objects::nonNull)
            .min(Comparator.comparing(SessionTimestamp::timestamp))
            .map(SessionTimestamp::sessionId)
            .orElse(sessionIds.iterator().next());
    }
    
    // Duplicate methods removed - using existing implementations above
    
    private SessionInfo updateSessionInfo(SessionInfo sessionInfo, String ipAddress) {
        sessionInfo.setLastAccessedAt(LocalDateTime.now());
        Optional.ofNullable(ipAddress)
            .ifPresent(sessionInfo::setIpAddress);
        return sessionInfo;
    }
    
    private SessionManagementResult createErrorResult(String sessionId, String action, String message) {
        return SessionManagementResult.builder()
            .sessionId(sessionId)
            .action(action)
            .success(false)
            .message(message)
            .build();
    }

    /**
     * Session management result data class
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionManagementResult {
        private String sessionId;
        private String userId;
        private String action;
        private boolean success;
        private String message;
    }
}