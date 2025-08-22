package com.trademaster.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.auth.entity.SessionSettings;
import com.trademaster.auth.entity.UserSession;
import com.trademaster.auth.repository.SessionSettingsRepository;
import com.trademaster.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final ObjectMapper objectMapper;

    @Value("${trademaster.security.session.max-concurrent:3}")
    private int defaultMaxConcurrentSessions;

    @Value("${trademaster.security.session.timeout:30}") // 30 minutes default
    private int defaultSessionTimeoutMinutes;

    private static final String SESSION_PREFIX = "trademaster:session:";
    private static final String USER_SESSIONS_PREFIX = "trademaster:user_sessions:";
    private static final String DEVICE_SESSIONS_PREFIX = "trademaster:device_sessions:";

    /**
     * Create a new session with enhanced settings support
     */
    @Transactional
    public UserSession createUserSession(String userId, String deviceFingerprint, HttpServletRequest request) {
        log.info("Creating session for user: {}", userId);
        
        SessionSettings settings = getOrCreateSessionSettings(userId);
        
        // Check concurrent session limit
        long activeSessions = userSessionRepository.countActiveSessionsForUser(userId, LocalDateTime.now());
        if (!settings.isWithinConcurrentSessionLimit((int) activeSessions)) {
            // Terminate oldest session
            List<UserSession> sessions = userSessionRepository.findSessionsByUserIdOrderByLastActivity(userId);
            if (!sessions.isEmpty()) {
                UserSession oldestSession = sessions.get(sessions.size() - 1);
                terminateSession(oldestSession.getSessionId(), "CONCURRENT_LIMIT");
            }
        }
        
        String sessionId = UUID.randomUUID().toString();
        InetAddress ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String location = extractLocationFromRequest(request);
        
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .location(location)
                .expiresAt(settings.calculateSessionExpiry())
                .active(true)
                .attributes(new HashMap<>())
                .build();
        
        session = userSessionRepository.save(session);
        
        // Also store in Redis for fast access
        storeSessionInRedis(session, settings.getSessionTimeoutMinutes());
        
        log.info("Session created: {} for user: {}", sessionId, userId);
        return session;
    }

    /**
     * Create a new session (legacy method for backward compatibility)
     */
    public String createSession(Long userId, String deviceFingerprint, String ipAddress, 
                               String userAgent, Map<String, Object> sessionData) {
        try {
            String sessionId = generateSessionId(userId);
            
            // Check concurrent session limit
            enforceSessionLimit(userId, deviceFingerprint);
            
            // Create session data
            SessionInfo sessionInfo = SessionInfo.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .isActive(true)
                .sessionData(sessionData != null ? sessionData : new HashMap<>())
                .build();

            // Store session
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = objectMapper.writeValueAsString(sessionInfo);
            
            sessionRedisTemplate.opsForValue().set(
                sessionKey, 
                sessionJson, 
                Duration.ofMinutes(sessionTimeoutMinutes)
            );

            // Track user sessions
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            sessionRedisTemplate.opsForSet().add(userSessionsKey, sessionId);
            sessionRedisTemplate.expire(userSessionsKey, Duration.ofMinutes(sessionTimeoutMinutes));

            // Track device sessions
            String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + deviceFingerprint;
            sessionRedisTemplate.opsForSet().add(deviceSessionsKey, sessionId);
            sessionRedisTemplate.expire(deviceSessionsKey, Duration.ofMinutes(sessionTimeoutMinutes));

            // Log session creation
            auditService.logAuthenticationEvent(userId, "SESSION_CREATED", "SUCCESS", 
                ipAddress, userAgent, deviceFingerprint, 
                Map.of("session_id", sessionId, "concurrent_sessions", getUserSessionCount(userId)), 
                sessionId);

            log.info("Session created for user {} with ID: {}", userId, sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("Failed to create session for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Session creation failed", e);
        }
    }

    /**
     * Validate and retrieve session
     */
    public SessionInfo getSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = sessionRedisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson == null) {
                log.debug("Session not found: {}", sessionId);
                return null;
            }

            SessionInfo sessionInfo = objectMapper.readValue(sessionJson, SessionInfo.class);
            
            if (!sessionInfo.isActive()) {
                log.debug("Session is inactive: {}", sessionId);
                return null;
            }

            return sessionInfo;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * Update session activity
     */
    public void updateSessionActivity(String sessionId, String ipAddress) {
        try {
            SessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                return;
            }

            sessionInfo.setLastActivityAt(LocalDateTime.now());
            sessionInfo.setIpAddress(ipAddress); // Update IP if changed

            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = objectMapper.writeValueAsString(sessionInfo);
            
            // Refresh TTL and update data
            sessionRedisTemplate.opsForValue().set(
                sessionKey, 
                sessionJson, 
                Duration.ofMinutes(sessionTimeoutMinutes)
            );

            log.debug("Updated activity for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to update session activity {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Invalidate a specific session
     */
    public void invalidateSession(String sessionId, String reason) {
        try {
            SessionInfo sessionInfo = getSession(sessionId);
            if (sessionInfo == null) {
                return;
            }

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
            auditService.logAuthenticationEvent(sessionInfo.getUserId(), "SESSION_EXPIRED", "SUCCESS", 
                sessionInfo.getIpAddress(), sessionInfo.getUserAgent(), sessionInfo.getDeviceFingerprint(), 
                Map.of("reason", reason, "session_id", sessionId), sessionId);

            log.info("Session invalidated: {} (reason: {})", sessionId, reason);

        } catch (Exception e) {
            log.error("Failed to invalidate session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Invalidate all sessions for a user
     */
    public void invalidateAllUserSessions(Long userId, String reason) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<String> sessionIds = sessionRedisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds != null) {
                for (String sessionId : sessionIds) {
                    invalidateSession(sessionId, reason);
                }
            }

            // Clean up the user sessions set
            sessionRedisTemplate.delete(userSessionsKey);

            log.info("Invalidated {} sessions for user {} (reason: {})", 
                   sessionIds != null ? sessionIds.size() : 0, userId, reason);

        } catch (Exception e) {
            log.error("Failed to invalidate all sessions for user {}: {}", userId, e.getMessage());
        }
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
        long currentSessions = getUserSessionCount(userId);
        
        if (currentSessions >= maxConcurrentSessions) {
            // Remove oldest session for this user
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<String> sessionIds = sessionRedisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds != null && !sessionIds.isEmpty()) {
                // Find oldest session (this is simplified - in production, you'd want to track creation time)
                String oldestSessionId = sessionIds.iterator().next();
                invalidateSession(oldestSessionId, "CONCURRENT_LIMIT");
                
                log.info("Removed oldest session {} for user {} due to concurrent limit", oldestSessionId, userId);
            }
        }
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
    @Transactional
    public void updateUserSessionActivity(String sessionId, HttpServletRequest request) {
        UserSession session = getUserSession(sessionId);
        if (session == null) {
            return;
        }

        SessionSettings settings = getOrCreateSessionSettings(session.getUserId());
        
        session.updateLastActivity();
        session.setIpAddress(extractIpAddress(request));
        
        if (settings.shouldExtendOnActivity()) {
            session.extendSession(settings.getSessionTimeoutMinutes());
        }
        
        userSessionRepository.save(session);
        
        // Update Redis cache
        storeSessionInRedis(session, settings.getSessionTimeoutMinutes());
    }

    /**
     * Terminate a specific session
     */
    @Transactional
    public void terminateSession(String sessionId, String reason) {
        log.info("Terminating session: {} (reason: {})", sessionId, reason);
        
        UserSession session = getUserSession(sessionId);
        if (session != null) {
            session.terminate();
            userSessionRepository.save(session);
            
            // Remove from Redis
            sessionRedisTemplate.delete(SESSION_PREFIX + sessionId);
            
            log.info("Session terminated: {} for user: {}", sessionId, session.getUserId());
        }
    }

    /**
     * Terminate all sessions for user
     */
    @Transactional
    public void terminateAllUserSessions(String userId, String reason) {
        log.info("Terminating all sessions for user: {} (reason: {})", userId, reason);
        
        userSessionRepository.terminateAllSessionsForUser(userId);
        
        // Clean up Redis
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        Set<String> sessionIds = sessionRedisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                sessionRedisTemplate.delete(SESSION_PREFIX + sessionId);
            }
        }
        sessionRedisTemplate.delete(userSessionsKey);
        
        log.info("All sessions terminated for user: {}", userId);
    }

    /**
     * Get session settings for user
     */
    public SessionSettings getSessionSettings(String userId) {
        return getOrCreateSessionSettings(userId);
    }

    /**
     * Update session settings
     */
    @Transactional
    public SessionSettings updateSessionSettings(String userId, SessionSettings settings) {
        settings.setUserId(userId);
        return sessionSettingsRepository.save(settings);
    }

    /**
     * Cleanup expired sessions (scheduled task)
     */
    @Scheduled(cron = "0 */5 * * * ?") // Every 5 minutes
    @Transactional
    public void cleanupExpiredUserSessions() {
        log.debug("Cleaning up expired sessions");
        
        // Deactivate expired sessions in database
        userSessionRepository.deactivateExpiredSessions(LocalDateTime.now());
        
        // Clean up old expired sessions
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        userSessionRepository.deleteByExpiresAtBefore(cutoffDate);
    }

    // Helper methods
    
    private SessionSettings getOrCreateSessionSettings(String userId) {
        return sessionSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    SessionSettings settings = SessionSettings.createDefault(userId);
                    return sessionSettingsRepository.save(settings);
                });
    }

    private void storeSessionInRedis(UserSession session, int timeoutMinutes) {
        try {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            SessionInfo sessionInfo = SessionInfo.builder()
                    .sessionId(session.getSessionId())
                    .userId(Long.parseLong(session.getUserId()))
                    .deviceFingerprint(session.getDeviceFingerprint())
                    .ipAddress(session.getIpAddress().getHostAddress())
                    .userAgent(session.getUserAgent())
                    .createdAt(session.getCreatedAt())
                    .lastActivityAt(session.getLastActivity())
                    .isActive(session.isActive())
                    .sessionData(session.getAttributes())
                    .build();
            
            String sessionJson = objectMapper.writeValueAsString(sessionInfo);
            sessionRedisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofMinutes(timeoutMinutes));
        } catch (Exception e) {
            log.warn("Failed to store session in Redis: {}", e.getMessage());
        }
    }

    private InetAddress extractIpAddress(HttpServletRequest request) {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return InetAddress.getByName(xForwardedFor.split(",")[0].trim());
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return InetAddress.getByName(xRealIp);
            }
            
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (Exception e) {
            log.warn("Error extracting IP address", e);
            try {
                return InetAddress.getByName(request.getRemoteAddr());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String extractLocationFromRequest(HttpServletRequest request) {
        // In a real implementation, you would use a GeoIP service
        return "Unknown Location";
    }

    /**
     * Session information data class
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionInfo {
        private String sessionId;
        private Long userId;
        private String deviceFingerprint;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;
        private LocalDateTime lastActivityAt;
        private boolean isActive;
        private Map<String, Object> sessionData;
    }
}