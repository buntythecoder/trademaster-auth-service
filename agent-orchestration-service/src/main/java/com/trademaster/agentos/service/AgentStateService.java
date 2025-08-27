package com.trademaster.agentos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Agent State Service
 * 
 * Redis-based service for managing agent runtime state, session data,
 * and temporary caching for high-performance agent operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    // Redis Key Patterns
    private static final String AGENT_STATE_KEY = "agentos:agent:state:";
    private static final String AGENT_SESSION_KEY = "agentos:agent:session:";
    private static final String AGENT_HEARTBEAT_KEY = "agentos:agent:heartbeat:";
    private static final String AGENT_METRICS_KEY = "agentos:agent:metrics:";
    private static final String AGENT_LOCK_KEY = "agentos:agent:lock:";
    private static final String SYSTEM_STATS_KEY = "agentos:system:stats";
    
    // Agent State Management

    /**
     * Store agent runtime state
     */
    public void storeAgentState(Long agentId, AgentRuntimeState state) {
        String key = AGENT_STATE_KEY + agentId;
        
        try {
            redisTemplate.opsForValue().set(key, state, Duration.ofMinutes(30));
            log.debug("Stored state for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error storing agent state for agent: {}", agentId, e);
        }
    }

    /**
     * Get agent runtime state
     */
    public AgentRuntimeState getAgentState(Long agentId) {
        String key = AGENT_STATE_KEY + agentId;
        
        try {
            Object state = redisTemplate.opsForValue().get(key);
            if (state instanceof AgentRuntimeState) {
                return (AgentRuntimeState) state;
            }
        } catch (Exception e) {
            log.error("Error getting agent state for agent: {}", agentId, e);
        }
        
        return null;
    }

    /**
     * Remove agent state
     */
    public void removeAgentState(Long agentId) {
        String key = AGENT_STATE_KEY + agentId;
        
        try {
            redisTemplate.delete(key);
            log.debug("Removed state for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error removing agent state for agent: {}", agentId, e);
        }
    }

    // Agent Session Management

    /**
     * Store agent session data
     */
    public void storeAgentSession(Long agentId, Map<String, Object> sessionData) {
        String key = AGENT_SESSION_KEY + agentId;
        
        try {
            redisTemplate.opsForHash().putAll(key, sessionData);
            redisTemplate.expire(key, Duration.ofHours(24));
            log.debug("Stored session data for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error storing agent session for agent: {}", agentId, e);
        }
    }

    /**
     * Get agent session data
     */
    public Map<Object, Object> getAgentSession(Long agentId) {
        String key = AGENT_SESSION_KEY + agentId;
        
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("Error getting agent session for agent: {}", agentId, e);
            return Map.of();
        }
    }

    /**
     * Update agent session field
     */
    public void updateAgentSessionField(Long agentId, String field, Object value) {
        String key = AGENT_SESSION_KEY + agentId;
        
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Updated session field '{}' for agent: {}", field, agentId);
        } catch (Exception e) {
            log.error("Error updating agent session field for agent: {}", agentId, e);
        }
    }

    /**
     * Remove agent session
     */
    public void removeAgentSession(Long agentId) {
        String key = AGENT_SESSION_KEY + agentId;
        
        try {
            redisTemplate.delete(key);
            log.debug("Removed session for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error removing agent session for agent: {}", agentId, e);
        }
    }

    // Agent Heartbeat Management

    /**
     * Record agent heartbeat
     */
    public void recordHeartbeat(Long agentId) {
        String key = AGENT_HEARTBEAT_KEY + agentId;
        
        try {
            stringRedisTemplate.opsForValue().set(key, Instant.now().toString(), Duration.ofMinutes(5));
            log.debug("Recorded heartbeat for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error recording heartbeat for agent: {}", agentId, e);
        }
    }

    /**
     * Get last heartbeat time
     */
    public Instant getLastHeartbeat(Long agentId) {
        String key = AGENT_HEARTBEAT_KEY + agentId;
        
        try {
            String heartbeatStr = stringRedisTemplate.opsForValue().get(key);
            if (heartbeatStr != null) {
                return Instant.parse(heartbeatStr);
            }
        } catch (Exception e) {
            log.error("Error getting last heartbeat for agent: {}", agentId, e);
        }
        
        return null;
    }

    /**
     * Check if agent heartbeat is alive (within last 2 minutes)
     */
    public boolean isHeartbeatAlive(Long agentId) {
        Instant lastHeartbeat = getLastHeartbeat(agentId);
        if (lastHeartbeat == null) {
            return false;
        }
        
        Duration timeSinceHeartbeat = Duration.between(lastHeartbeat, Instant.now());
        return timeSinceHeartbeat.toMinutes() < 2;
    }

    // Agent Performance Metrics Caching

    /**
     * Cache agent performance metrics
     */
    public void cacheAgentMetrics(Long agentId, AgentPerformanceMetrics metrics) {
        String key = AGENT_METRICS_KEY + agentId;
        
        try {
            redisTemplate.opsForValue().set(key, metrics, Duration.ofMinutes(15));
            log.debug("Cached metrics for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error caching agent metrics for agent: {}", agentId, e);
        }
    }

    /**
     * Get cached agent performance metrics
     */
    public AgentPerformanceMetrics getCachedAgentMetrics(Long agentId) {
        String key = AGENT_METRICS_KEY + agentId;
        
        try {
            Object metrics = redisTemplate.opsForValue().get(key);
            if (metrics instanceof AgentPerformanceMetrics) {
                return (AgentPerformanceMetrics) metrics;
            }
        } catch (Exception e) {
            log.error("Error getting cached agent metrics for agent: {}", agentId, e);
        }
        
        return null;
    }

    // Agent Locking (for exclusive operations)

    /**
     * Acquire lock for agent operation
     */
    public boolean acquireAgentLock(Long agentId, String operation, Duration lockDuration) {
        String key = AGENT_LOCK_KEY + agentId + ":" + operation;
        
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, Instant.now().toString(), lockDuration);
            
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired lock for agent {} operation: {}", agentId, operation);
                return true;
            } else {
                log.debug("Failed to acquire lock for agent {} operation: {}", agentId, operation);
                return false;
            }
        } catch (Exception e) {
            log.error("Error acquiring lock for agent {} operation: {}", agentId, operation, e);
            return false;
        }
    }

    /**
     * Release lock for agent operation
     */
    public void releaseAgentLock(Long agentId, String operation) {
        String key = AGENT_LOCK_KEY + agentId + ":" + operation;
        
        try {
            stringRedisTemplate.delete(key);
            log.debug("Released lock for agent {} operation: {}", agentId, operation);
        } catch (Exception e) {
            log.error("Error releasing lock for agent {} operation: {}", agentId, operation, e);
        }
    }

    // System-wide Caching

    /**
     * Cache system statistics
     */
    public void cacheSystemStats(SystemStatistics stats) {
        try {
            redisTemplate.opsForValue().set(SYSTEM_STATS_KEY, stats, Duration.ofMinutes(5));
            log.debug("Cached system statistics");
        } catch (Exception e) {
            log.error("Error caching system statistics", e);
        }
    }

    /**
     * Get cached system statistics
     */
    public SystemStatistics getCachedSystemStats() {
        try {
            Object stats = redisTemplate.opsForValue().get(SYSTEM_STATS_KEY);
            if (stats instanceof SystemStatistics) {
                return (SystemStatistics) stats;
            }
        } catch (Exception e) {
            log.error("Error getting cached system statistics", e);
        }
        
        return null;
    }

    // Utility Methods

    /**
     * Get all active agent IDs (based on heartbeats)
     */
    public Set<String> getActiveAgentIds() {
        try {
            return stringRedisTemplate.keys(AGENT_HEARTBEAT_KEY + "*");
        } catch (Exception e) {
            log.error("Error getting active agent IDs", e);
            return Set.of();
        }
    }

    /**
     * Clear all agent state data for cleanup
     */
    public void clearAgentData(Long agentId) {
        try {
            removeAgentState(agentId);
            removeAgentSession(agentId);
            
            String heartbeatKey = AGENT_HEARTBEAT_KEY + agentId;
            String metricsKey = AGENT_METRICS_KEY + agentId;
            
            redisTemplate.delete(heartbeatKey);
            redisTemplate.delete(metricsKey);
            
            log.info("Cleared all Redis data for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error clearing agent data for agent: {}", agentId, e);
        }
    }

    // Helper Classes

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AgentRuntimeState {
        private Long agentId;
        private String currentStatus;
        private Integer currentLoad;
        private Instant lastActivity;
        private Map<String, Object> contextData;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AgentPerformanceMetrics {
        private Long agentId;
        private Double successRate;
        private Long averageResponseTime;
        private Long totalTasksCompleted;
        private Long totalTasksFailed;
        private Instant lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SystemStatistics {
        private Long totalAgents;
        private Long activeAgents;
        private Double systemUtilization;
        private Long totalTasks;
        private Long activeTasks;
        private Instant lastUpdated;
    }
}