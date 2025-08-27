package com.trademaster.agentos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ✅ MANDATORY: Agent Registry Service with Caching
 * 
 * Phase 1 Requirement from AGENT_OS_MVP_SPEC.md:
 * - Agent registry and lifecycle management
 * - Redis-based caching for fast agent discovery
 * - Agent health monitoring and availability tracking
 * - Load balancing and agent selection optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRegistryService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    // ✅ REDIS KEYS: Agent registry structure
    private static final String AGENT_REGISTRY_PREFIX = "agentos:agent:registry:";
    private static final String AGENT_BY_TYPE_PREFIX = "agentos:agent:type:";
    private static final String AGENT_BY_STATUS_PREFIX = "agentos:agent:status:";
    private static final String AGENT_HEARTBEAT_PREFIX = "agentos:agent:heartbeat:";
    private static final String AGENT_CAPABILITIES_PREFIX = "agentos:agent:capabilities:";
    private static final String AGENT_LOAD_PREFIX = "agentos:agent:load:";
    
    // ✅ CACHE TTL
    private static final Duration AGENT_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofMinutes(2);
    
    /**
     * ✅ REGISTER: Add agent to registry with caching
     */
    @Async
    public CompletableFuture<Boolean> registerAgent(Agent agent) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_registry_register");
            
            try {
                // ✅ VALIDATION: Check if agent already registered
                if (isAgentRegistered(agent.getAgentId())) {
                    throw new IllegalArgumentException("Agent already registered: " + agent.getAgentId());
                }
                
                // ✅ SERIALIZATION: Store agent data
                String agentJson = objectMapper.writeValueAsString(agent);
                String agentKey = AGENT_REGISTRY_PREFIX + agent.getAgentId();
                redisTemplate.opsForValue().set(agentKey, agentJson, AGENT_CACHE_TTL);
                
                // ✅ INDEXING: Add to type-based index
                String typeKey = AGENT_BY_TYPE_PREFIX + agent.getAgentType();
                redisTemplate.opsForSet().add(typeKey, agent.getAgentId().toString());
                redisTemplate.expire(typeKey, AGENT_CACHE_TTL);
                
                // ✅ STATUS INDEXING: Add to status-based index
                String statusKey = AGENT_BY_STATUS_PREFIX + agent.getStatus();
                redisTemplate.opsForSet().add(statusKey, agent.getAgentId().toString());
                redisTemplate.expire(statusKey, AGENT_CACHE_TTL);
                
                // ✅ CAPABILITIES INDEXING: Index agent capabilities
                if (agent.getCapabilities() != null) {
                    String capabilitiesKey = AGENT_CAPABILITIES_PREFIX + agent.getAgentId();
                    String capabilitiesJson = objectMapper.writeValueAsString(agent.getCapabilities());
                    redisTemplate.opsForValue().set(capabilitiesKey, capabilitiesJson, AGENT_CACHE_TTL);
                }
                
                // ✅ LOAD TRACKING: Initialize load tracking
                String loadKey = AGENT_LOAD_PREFIX + agent.getAgentId();
                redisTemplate.opsForHash().put(loadKey, "current", "0");
                redisTemplate.opsForHash().put(loadKey, "max", agent.getMaxConcurrentTasks().toString());
                redisTemplate.expire(loadKey, AGENT_CACHE_TTL);
                
                // ✅ HEARTBEAT: Initialize heartbeat
                updateHeartbeat(agent.getAgentId());
                
                timer.stop(metrics.getApiResponseTime());
                metrics.recordAgentCreated(agent.getAgentType().toString());
                structuredLogger.logAgentCreated(
                    agent.getAgentId().toString(),
                    agent.getAgentType().toString(),
                    "registry"
                );
                
                return true;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_registry_register", e.getClass().getSimpleName());
                structuredLogger.logError("agent_registry_register", e.getMessage(), e,
                    Map.of("agentId", agent.getAgentId(), "agentType", agent.getAgentType()));
                return false;
            }
        });
    }
    
    /**
     * ✅ DEREGISTER: Remove agent from registry
     */
    @Async
    public CompletableFuture<Boolean> deregisterAgent(Long agentId) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_registry_deregister");
            
            try {
                Optional<Agent> agentOpt = getAgent(agentId);
                if (agentOpt.isEmpty()) {
                    return false;
                }
                
                Agent agent = agentOpt.get();
                
                // ✅ CLEANUP: Remove from all indexes
                String agentKey = AGENT_REGISTRY_PREFIX + agentId;
                redisTemplate.delete(agentKey);
                
                String typeKey = AGENT_BY_TYPE_PREFIX + agent.getAgentType();
                redisTemplate.opsForSet().remove(typeKey, agentId.toString());
                
                String statusKey = AGENT_BY_STATUS_PREFIX + agent.getStatus();
                redisTemplate.opsForSet().remove(statusKey, agentId.toString());
                
                String capabilitiesKey = AGENT_CAPABILITIES_PREFIX + agentId;
                redisTemplate.delete(capabilitiesKey);
                
                String loadKey = AGENT_LOAD_PREFIX + agentId;
                redisTemplate.delete(loadKey);
                
                String heartbeatKey = AGENT_HEARTBEAT_PREFIX + agentId;
                redisTemplate.delete(heartbeatKey);
                
                timer.stop(metrics.getApiResponseTime());
                metrics.recordAgentDestroyed(agent.getAgentType().toString());
                structuredLogger.logAgentDestroyed(
                    agentId.toString(),
                    agent.getAgentType().toString(),
                    "registry",
                    "deregistration"
                );
                
                return true;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_registry_deregister", e.getClass().getSimpleName());
                structuredLogger.logError("agent_registry_deregister", e.getMessage(), e,
                    Map.of("agentId", agentId));
                return false;
            }
        });
    }
    
    /**
     * ✅ LOOKUP: Get agent by ID with caching
     */
    public Optional<Agent> getAgent(Long agentId) {
        try {
            String agentKey = AGENT_REGISTRY_PREFIX + agentId;
            String agentJson = redisTemplate.opsForValue().get(agentKey);
            
            if (agentJson == null) {
                return Optional.empty();
            }
            
            Agent agent = objectMapper.readValue(agentJson, Agent.class);
            return Optional.of(agent);
            
        } catch (Exception e) {
            structuredLogger.logError("agent_registry_get", e.getMessage(), e,
                Map.of("agentId", agentId));
            return Optional.empty();
        }
    }
    
    /**
     * ✅ DISCOVERY: Find agents by type with caching
     */
    @Async
    public CompletableFuture<List<Agent>> getAgentsByType(AgentType agentType) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_registry_get_by_type");
            
            try {
                String typeKey = AGENT_BY_TYPE_PREFIX + agentType;
                Set<String> agentIds = redisTemplate.opsForSet().members(typeKey);
                
                if (agentIds == null || agentIds.isEmpty()) {
                    timer.stop(metrics.getApiResponseTime());
                    return List.of();
                }
                
                List<Agent> agents = agentIds.stream()
                    .map(Long::parseLong)
                    .map(this::getAgent)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logDataAccess("agent_registry", agentType.toString(), "list_by_type", "registry");
                
                return agents;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_registry_get_by_type", e.getClass().getSimpleName());
                structuredLogger.logError("agent_registry_get_by_type", e.getMessage(), e,
                    Map.of("agentType", agentType));
                return List.of();
            }
        });
    }
    
    /**
     * ✅ DISCOVERY: Find agents by status with caching
     */
    @Async
    public CompletableFuture<List<Agent>> getAgentsByStatus(AgentStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_registry_get_by_status");
            
            try {
                String statusKey = AGENT_BY_STATUS_PREFIX + status;
                Set<String> agentIds = redisTemplate.opsForSet().members(statusKey);
                
                if (agentIds == null || agentIds.isEmpty()) {
                    timer.stop(metrics.getApiResponseTime());
                    return List.of();
                }
                
                List<Agent> agents = agentIds.stream()
                    .map(Long::parseLong)
                    .map(this::getAgent)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(agent -> isAgentHealthy(agent.getAgentId())) // ✅ HEALTH CHECK
                    .collect(Collectors.toList());
                
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logDataAccess("agent_registry", status.toString(), "list_by_status", "registry");
                
                return agents;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_registry_get_by_status", e.getClass().getSimpleName());
                structuredLogger.logError("agent_registry_get_by_status", e.getMessage(), e,
                    Map.of("status", status));
                return List.of();
            }
        });
    }
    
    /**
     * ✅ LOAD BALANCING: Find available agents with load balancing
     */
    @Async
    public CompletableFuture<List<Agent>> getAvailableAgents(AgentType agentType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Agent> activeAgents = getAgentsByStatus(AgentStatus.ACTIVE).join();
                
                return activeAgents.stream()
                    .filter(agent -> agent.getAgentType() == agentType)
                    .filter(agent -> isAgentHealthy(agent.getAgentId()))
                    .filter(agent -> hasCapacity(agent.getAgentId()))
                    .sorted((a1, a2) -> Double.compare(getLoadRatio(a1.getAgentId()), getLoadRatio(a2.getAgentId())))
                    .collect(Collectors.toList());
                    
            } catch (Exception e) {
                structuredLogger.logError("agent_registry_get_available", e.getMessage(), e,
                    Map.of("agentType", agentType));
                return List.of();
            }
        });
    }
    
    /**
     * ✅ HEARTBEAT: Update agent heartbeat
     */
    public void updateHeartbeat(Long agentId) {
        try {
            String heartbeatKey = AGENT_HEARTBEAT_PREFIX + agentId;
            redisTemplate.opsForValue().set(heartbeatKey, Instant.now().toString(), HEARTBEAT_TTL);
            
            structuredLogger.logBusinessTransaction(
                "agent_heartbeat",
                agentId.toString(),
                "heartbeat",
                "agent",
                Map.of("timestamp", Instant.now())
            );
            
        } catch (Exception e) {
            structuredLogger.logError("agent_registry_heartbeat", e.getMessage(), e,
                Map.of("agentId", agentId));
        }
    }
    
    /**
     * ✅ LOAD UPDATE: Update agent load
     */
    public void updateAgentLoad(Long agentId, int currentLoad) {
        try {
            String loadKey = AGENT_LOAD_PREFIX + agentId;
            redisTemplate.opsForHash().put(loadKey, "current", String.valueOf(currentLoad));
            redisTemplate.expire(loadKey, AGENT_CACHE_TTL);
            
        } catch (Exception e) {
            structuredLogger.logError("agent_registry_update_load", e.getMessage(), e,
                Map.of("agentId", agentId, "currentLoad", currentLoad));
        }
    }
    
    /**
     * ✅ STATUS UPDATE: Update agent status with index maintenance
     */
    public void updateAgentStatus(Long agentId, AgentStatus newStatus) {
        try {
            Optional<Agent> agentOpt = getAgent(agentId);
            if (agentOpt.isEmpty()) {
                return;
            }
            
            Agent agent = agentOpt.get();
            AgentStatus oldStatus = agent.getStatus();
            
            // ✅ UPDATE AGENT
            agent.setStatus(newStatus);
            agent.setUpdatedAt(Instant.now());
            
            // ✅ PERSISTENCE: Update cached agent
            String agentJson = objectMapper.writeValueAsString(agent);
            String agentKey = AGENT_REGISTRY_PREFIX + agentId;
            redisTemplate.opsForValue().set(agentKey, agentJson, AGENT_CACHE_TTL);
            
            // ✅ INDEX MAINTENANCE: Update status indexes
            if (oldStatus != newStatus) {
                String oldStatusKey = AGENT_BY_STATUS_PREFIX + oldStatus;
                redisTemplate.opsForSet().remove(oldStatusKey, agentId.toString());
                
                String newStatusKey = AGENT_BY_STATUS_PREFIX + newStatus;
                redisTemplate.opsForSet().add(newStatusKey, agentId.toString());
                redisTemplate.expire(newStatusKey, AGENT_CACHE_TTL);
            }
            
            structuredLogger.logBusinessTransaction(
                "agent_status_update",
                agentId.toString(),
                "status_change",
                "registry",
                Map.of("oldStatus", oldStatus, "newStatus", newStatus)
            );
            
        } catch (Exception e) {
            structuredLogger.logError("agent_registry_update_status", e.getMessage(), e,
                Map.of("agentId", agentId, "newStatus", newStatus));
        }
    }
    
    /**
     * ✅ REGISTRY STATS: Get registry statistics
     */
    public RegistryStats getRegistryStats() {
        try {
            // Count agents by type
            long marketAnalysisCount = getAgentsByType(AgentType.MARKET_ANALYSIS).join().size();
            long portfolioCount = getAgentsByType(AgentType.PORTFOLIO_MANAGEMENT).join().size();
            long tradingCount = getAgentsByType(AgentType.TRADING_EXECUTION).join().size();
            long riskCount = getAgentsByType(AgentType.RISK_MANAGEMENT).join().size();
            
            // Count agents by status
            long activeCount = getAgentsByStatus(AgentStatus.ACTIVE).join().size();
            long busyCount = getAgentsByStatus(AgentStatus.BUSY).join().size();
            long errorCount = getAgentsByStatus(AgentStatus.ERROR).join().size();
            
            return RegistryStats.builder()
                .totalAgents(marketAnalysisCount + portfolioCount + tradingCount + riskCount)
                .marketAnalysisAgents(marketAnalysisCount)
                .portfolioAgents(portfolioCount)
                .tradingAgents(tradingCount)
                .riskAgents(riskCount)
                .activeAgents(activeCount)
                .busyAgents(busyCount)
                .errorAgents(errorCount)
                .build();
                
        } catch (Exception e) {
            structuredLogger.logError("agent_registry_stats", e.getMessage(), e, Map.of());
            return RegistryStats.builder().build();
        }
    }
    
    // ✅ PRIVATE METHODS
    
    private boolean isAgentRegistered(Long agentId) {
        String agentKey = AGENT_REGISTRY_PREFIX + agentId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(agentKey));
    }
    
    private boolean isAgentHealthy(Long agentId) {
        String heartbeatKey = AGENT_HEARTBEAT_PREFIX + agentId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey));
    }
    
    private boolean hasCapacity(Long agentId) {
        try {
            String loadKey = AGENT_LOAD_PREFIX + agentId;
            String currentStr = (String) redisTemplate.opsForHash().get(loadKey, "current");
            String maxStr = (String) redisTemplate.opsForHash().get(loadKey, "max");
            
            if (currentStr == null || maxStr == null) {
                return false;
            }
            
            int current = Integer.parseInt(currentStr);
            int max = Integer.parseInt(maxStr);
            
            return current < max;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private double getLoadRatio(Long agentId) {
        try {
            String loadKey = AGENT_LOAD_PREFIX + agentId;
            String currentStr = (String) redisTemplate.opsForHash().get(loadKey, "current");
            String maxStr = (String) redisTemplate.opsForHash().get(loadKey, "max");
            
            if (currentStr == null || maxStr == null) {
                return 1.0; // Treat unknown load as fully loaded
            }
            
            int current = Integer.parseInt(currentStr);
            int max = Integer.parseInt(maxStr);
            
            return max > 0 ? (double) current / max : 1.0;
            
        } catch (Exception e) {
            return 1.0;
        }
    }
    
    // ✅ DATA MODEL
    
    @lombok.Data
    @lombok.Builder
    public static class RegistryStats {
        private long totalAgents;
        private long marketAnalysisAgents;
        private long portfolioAgents;
        private long tradingAgents;
        private long riskAgents;
        private long activeAgents;
        private long busyAgents;
        private long errorAgents;
    }
}