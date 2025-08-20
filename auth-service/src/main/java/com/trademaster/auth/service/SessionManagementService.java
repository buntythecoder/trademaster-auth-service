package com.trademaster.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${trademaster.security.session.max-concurrent:5}")
    private int maxConcurrentSessions;

    @Value("${trademaster.security.session.timeout:1440}") // 24 hours in minutes
    private int sessionTimeoutMinutes;

    private static final String SESSION_PREFIX = "trademaster:session:";
    private static final String USER_SESSIONS_PREFIX = "trademaster:user_sessions:";
    private static final String DEVICE_SESSIONS_PREFIX = "trademaster:device_sessions:";

    /**
     * Create a new session
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