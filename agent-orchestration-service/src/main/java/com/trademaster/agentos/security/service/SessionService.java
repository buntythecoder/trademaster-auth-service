package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Session Service - Manages user sessions with secure defaults.
 * Implements session lifecycle management and validation.
 */
@Slf4j
@Service
public class SessionService {
    
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> userToSession = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final Duration sessionTimeout;
    private final Duration maxSessionDuration;
    private final boolean singleSessionPerUser;
    
    public SessionService(
            @Value("${security.session.timeout:1800}") int timeoutSeconds,
            @Value("${security.session.max-duration:86400}") int maxDurationSeconds,
            @Value("${security.session.single-per-user:true}") boolean singleSessionPerUser) {
        
        this.sessionTimeout = Duration.ofSeconds(timeoutSeconds);
        this.maxSessionDuration = Duration.ofSeconds(maxDurationSeconds);
        this.singleSessionPerUser = singleSessionPerUser;
        
        // Schedule periodic cleanup of expired sessions
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Create new session for user.
     */
    public Result<String, SecurityError> createSession(SecurityContext context) {
        String userId = context.userId();
        
        if (userId == null || userId.isBlank()) {
            return Result.failure(SecurityError.authenticationFailed(
                "User ID required for session creation", context.correlationId()));
        }
        
        // Check for existing session if single session mode
        if (singleSessionPerUser) {
            String existingSessionId = userToSession.get(userId);
            if (existingSessionId != null) {
                invalidateSession(existingSessionId);
                log.info("Invalidated existing session for user: {}", userId);
            }
        }
        
        // Create new session
        String sessionId = UUID.randomUUID().toString();
        SessionInfo session = new SessionInfo(
            sessionId,
            userId,
            context.ipAddress(),
            context.userAgent(),
            Instant.now(),
            Instant.now(),
            context.securityLevel()
        );
        
        activeSessions.put(sessionId, session);
        userToSession.put(userId, sessionId);
        
        log.info("Created new session: sessionId={}, userId={}", sessionId, userId);
        return Result.success(sessionId);
    }
    
    /**
     * Validate session.
     */
    public Result<SecurityContext, SecurityError> validateSession(SecurityContext context) {
        String sessionId = context.sessionId();
        
        if (sessionId == null || sessionId.isBlank()) {
            return Result.failure(SecurityError.authenticationFailed(
                "No session ID provided", context.correlationId()));
        }
        
        SessionInfo session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.warn("Session not found: sessionId={}", sessionId);
            return Result.failure(SecurityError.authenticationFailed(
                "Invalid session", context.correlationId()));
        }
        
        // Check if session expired
        if (isSessionExpired(session)) {
            invalidateSession(sessionId);
            log.warn("Session expired: sessionId={}", sessionId);
            return Result.failure(SecurityError.authenticationFailed(
                "Session expired", context.correlationId()));
        }
        
        // Check if session exceeded max duration
        if (isSessionTooOld(session)) {
            invalidateSession(sessionId);
            log.warn("Session exceeded maximum duration: sessionId={}", sessionId);
            return Result.failure(SecurityError.authenticationFailed(
                "Session exceeded maximum duration", context.correlationId()));
        }
        
        // Validate session binding (IP and User-Agent)
        if (!validateSessionBinding(session, context)) {
            log.warn("Session binding validation failed: sessionId={}", sessionId);
            return Result.failure(SecurityError.authenticationFailed(
                "Session binding mismatch", context.correlationId()));
        }
        
        // Update last activity
        session.lastActivity = Instant.now();
        
        return Result.success(context);
    }
    
    /**
     * Extend session timeout.
     */
    public Result<Void, SecurityError> extendSession(String sessionId) {
        SessionInfo session = activeSessions.get(sessionId);
        
        if (session == null) {
            return Result.failure(SecurityError.authenticationFailed(
                "Session not found", UUID.randomUUID().toString()));
        }
        
        session.lastActivity = Instant.now();
        log.debug("Extended session: sessionId={}", sessionId);
        
        return Result.success(null);
    }
    
    /**
     * Invalidate session.
     */
    public void invalidateSession(String sessionId) {
        SessionInfo session = activeSessions.remove(sessionId);
        
        if (session != null) {
            userToSession.remove(session.userId);
            log.info("Invalidated session: sessionId={}, userId={}", sessionId, session.userId);
        }
    }
    
    /**
     * Invalidate all sessions for user.
     */
    public void invalidateUserSessions(String userId) {
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().userId.equals(userId)) {
                log.info("Invalidating session for user: sessionId={}, userId={}", 
                    entry.getKey(), userId);
                return true;
            }
            return false;
        });
        
        userToSession.remove(userId);
    }
    
    /**
     * Get session info.
     */
    public SessionInfo getSessionInfo(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Get active session count for user.
     */
    public int getUserSessionCount(String userId) {
        return (int) activeSessions.values().stream()
            .filter(session -> session.userId.equals(userId))
            .count();
    }
    
    /**
     * Get total active sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    // Private helper methods
    
    private boolean isSessionExpired(SessionInfo session) {
        Duration idle = Duration.between(session.lastActivity, Instant.now());
        return idle.compareTo(sessionTimeout) > 0;
    }
    
    private boolean isSessionTooOld(SessionInfo session) {
        Duration age = Duration.between(session.createdAt, Instant.now());
        return age.compareTo(maxSessionDuration) > 0;
    }
    
    private boolean validateSessionBinding(SessionInfo session, SecurityContext context) {
        // Strict IP binding for elevated security levels
        if (session.securityLevel.isHigherThan(SecurityContext.SecurityLevel.STANDARD)) {
            if (!session.ipAddress.equals(context.ipAddress())) {
                return false;
            }
        }
        
        // User-Agent validation (less strict)
        if (session.userAgent != null && context.userAgent() != null) {
            // Check if User-Agent drastically changed (potential session hijack)
            if (!session.userAgent.contains(extractBrowserInfo(context.userAgent()))) {
                log.warn("User-Agent mismatch: expected={}, actual={}", 
                    session.userAgent, context.userAgent());
                // Don't fail for User-Agent mismatch alone
            }
        }
        
        return true;
    }
    
    private String extractBrowserInfo(String userAgent) {
        // Extract browser name from User-Agent string
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        return "Unknown";
    }
    
    private void cleanupExpiredSessions() {
        int removed = 0;
        
        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            if (isSessionExpired(entry.getValue()) || isSessionTooOld(entry.getValue())) {
                invalidateSession(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cleaned up {} expired sessions", removed);
        }
    }
    
    /**
     * Session information.
     */
    public static class SessionInfo {
        public final String sessionId;
        public final String userId;
        public final String ipAddress;
        public final String userAgent;
        public final Instant createdAt;
        public volatile Instant lastActivity;
        public final SecurityContext.SecurityLevel securityLevel;
        
        public SessionInfo(String sessionId, String userId, String ipAddress, 
                          String userAgent, Instant createdAt, Instant lastActivity,
                          SecurityContext.SecurityLevel securityLevel) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.createdAt = createdAt;
            this.lastActivity = lastActivity;
            this.securityLevel = securityLevel;
        }
    }
}