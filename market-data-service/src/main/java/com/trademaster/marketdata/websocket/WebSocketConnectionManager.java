package com.trademaster.marketdata.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket Connection Manager
 * 
 * Features:
 * - Connection lifecycle management
 * - Session tracking and limits
 * - Connection health monitoring
 * - Load balancing support
 * - Performance metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class WebSocketConnectionManager {

    // Connection storage
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // User session mapping
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // Connection limits
    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private static final int MAX_TOTAL_CONNECTIONS = 10000;
    
    // Performance metrics
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private final AtomicLong totalDisconnections = new AtomicLong(0);

    /**
     * Add new WebSocket connection
     */
    public boolean addConnection(WebSocketSession session) {
        try {
            String sessionId = session.getId();
            String userId = extractUserId(session);
            
            // Check global connection limit
            if (currentConnections.get() >= MAX_TOTAL_CONNECTIONS) {
                log.warn("Connection limit exceeded. Rejecting session {}", sessionId);
                return false;
            }
            
            // Check per-user connection limit
            if (userId != null) {
                Set<String> existingSessions = userSessions.getOrDefault(userId, Set.of());
                if (existingSessions.size() >= MAX_CONNECTIONS_PER_USER) {
                    log.warn("User {} has reached connection limit. Rejecting session {}", 
                        userId, sessionId);
                    return false;
                }
            }
            
            // Add session
            activeSessions.put(sessionId, session);
            currentConnections.incrementAndGet();
            totalConnections.incrementAndGet();
            
            // Track by user
            if (userId != null) {
                userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);
            }
            
            log.info("WebSocket connection added - Session: {}, User: {}, Total: {}", 
                sessionId, userId, currentConnections.get());
            
            return true;
            
        } catch (Exception e) {
            log.error("Error adding WebSocket connection: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Remove WebSocket connection
     */
    public void removeConnection(WebSocketSession session) {
        try {
            String sessionId = session.getId();
            String userId = extractUserId(session);
            
            WebSocketSession removed = activeSessions.remove(sessionId);
            if (removed != null) {
                currentConnections.decrementAndGet();
                totalDisconnections.incrementAndGet();
                
                // Remove from user sessions
                if (userId != null) {
                    Set<String> userSessionSet = userSessions.get(userId);
                    if (userSessionSet != null) {
                        userSessionSet.remove(sessionId);
                        if (userSessionSet.isEmpty()) {
                            userSessions.remove(userId);
                        }
                    }
                }
                
                log.info("WebSocket connection removed - Session: {}, User: {}, Total: {}", 
                    sessionId, userId, currentConnections.get());
            }
            
        } catch (Exception e) {
            log.error("Error removing WebSocket connection: {}", e.getMessage());
        }
    }

    /**
     * Get WebSocket session by ID
     */
    public WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Get all active sessions for a user
     */
    public List<WebSocketSession> getUserSessions(String userId) {
        Set<String> sessionIds = userSessions.getOrDefault(userId, Set.of());
        return sessionIds.stream()
            .map(activeSessions::get)
            .filter(session -> session != null && session.isOpen())
            .toList();
    }

    /**
     * Get all active session IDs
     */
    public Set<String> getSessionIds() {
        return Set.copyOf(activeSessions.keySet());
    }

    /**
     * Get active connection count
     */
    public int getActiveConnections() {
        return currentConnections.get();
    }

    /**
     * Check if session exists and is open
     */
    public boolean isSessionActive(String sessionId) {
        WebSocketSession session = activeSessions.get(sessionId);
        return session != null && session.isOpen();
    }

    /**
     * Broadcast message to all active sessions
     */
    public void broadcastToAll(String message) {
        activeSessions.values().parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                } catch (Exception e) {
                    log.error("Error broadcasting to session {}: {}", session.getId(), e.getMessage());
                }
            });
    }

    /**
     * Broadcast message to specific user sessions
     */
    public void broadcastToUser(String userId, String message) {
        List<WebSocketSession> sessions = getUserSessions(userId);
        sessions.parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                } catch (Exception e) {
                    log.error("Error broadcasting to user {} session {}: {}", 
                        userId, session.getId(), e.getMessage());
                }
            });
    }

    /**
     * Close all sessions for a user
     */
    public void closeUserSessions(String userId, String reason) {
        List<WebSocketSession> sessions = getUserSessions(userId);
        sessions.forEach(session -> {
            try {
                session.close(org.springframework.web.socket.CloseStatus.NORMAL.withReason(reason));
                log.info("Closed session {} for user {} - Reason: {}", 
                    session.getId(), userId, reason);
            } catch (Exception e) {
                log.error("Error closing session {} for user {}: {}", 
                    session.getId(), userId, e.getMessage());
            }
        });
    }

    /**
     * Clean up stale connections
     */
    public void cleanupStaleConnections() {
        List<String> staleSessionIds = activeSessions.entrySet().stream()
            .filter(entry -> !entry.getValue().isOpen())
            .map(entry -> entry.getKey())
            .toList();
        
        staleSessionIds.forEach(sessionId -> {
            WebSocketSession session = activeSessions.remove(sessionId);
            if (session != null) {
                removeConnection(session);
            }
        });
        
        if (!staleSessionIds.isEmpty()) {
            log.info("Cleaned up {} stale WebSocket connections", staleSessionIds.size());
        }
    }

    /**
     * Get connection statistics
     */
    public ConnectionStats getConnectionStats() {
        // Calculate user distribution
        int totalUsers = userSessions.size();
        double avgSessionsPerUser = totalUsers > 0 ? 
            (double) currentConnections.get() / totalUsers : 0.0;
        
        // Find max sessions per user
        int maxSessionsPerUser = userSessions.values().stream()
            .mapToInt(Set::size)
            .max()
            .orElse(0);
        
        return new ConnectionStats(
            currentConnections.get(),
            totalConnections.get(),
            totalDisconnections.get(),
            totalUsers,
            avgSessionsPerUser,
            maxSessionsPerUser,
            MAX_TOTAL_CONNECTIONS,
            MAX_CONNECTIONS_PER_USER,
            System.currentTimeMillis()
        );
    }

    /**
     * Check connection health
     */
    public ConnectionHealth checkConnectionHealth() {
        int active = currentConnections.get();
        double utilizationPercent = (double) active / MAX_TOTAL_CONNECTIONS * 100;
        
        HealthStatus status;
        if (utilizationPercent < 70) {
            status = HealthStatus.HEALTHY;
        } else if (utilizationPercent < 90) {
            status = HealthStatus.WARNING;
        } else {
            status = HealthStatus.CRITICAL;
        }
        
        return new ConnectionHealth(
            status,
            active,
            utilizationPercent,
            getStaleConnectionCount(),
            System.currentTimeMillis()
        );
    }

    /**
     * Extract user ID from WebSocket session
     */
    private String extractUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * Count stale connections
     */
    private int getStaleConnectionCount() {
        return (int) activeSessions.values().stream()
            .filter(session -> !session.isOpen())
            .count();
    }

    /**
     * Connection statistics record
     */
    public record ConnectionStats(
        int activeConnections,
        long totalConnections,
        long totalDisconnections,
        int totalUsers,
        double avgSessionsPerUser,
        int maxSessionsPerUser,
        int connectionLimit,
        int userConnectionLimit,
        long timestamp
    ) {
        public double getUtilizationPercent() {
            return (double) activeConnections / connectionLimit * 100;
        }
        
        public boolean isNearLimit() {
            return getUtilizationPercent() > 80;
        }
    }

    /**
     * Connection health record
     */
    public record ConnectionHealth(
        HealthStatus status,
        int activeConnections,
        double utilizationPercent,
        int staleConnections,
        long timestamp
    ) {}

    /**
     * Health status enumeration
     */
    public enum HealthStatus {
        HEALTHY("System operating normally"),
        WARNING("High resource utilization"),
        CRITICAL("System at capacity limit");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}