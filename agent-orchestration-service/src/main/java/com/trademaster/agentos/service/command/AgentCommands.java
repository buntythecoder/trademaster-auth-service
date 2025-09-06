package com.trademaster.agentos.service.command;

import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.repository.AgentRepository;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ COMMAND PATTERN: Concrete Agent Commands Implementation
 * 
 * Factory for creating specific agent commands with proper error handling,
 * metrics, logging, and Virtual Thread execution.
 */
@Component
@RequiredArgsConstructor
public class AgentCommands {
    
    private final AgentRepository agentRepository;
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ COMMAND: Create Agent Registration Command
     */
    public AgentCommand<Agent> createRegisterCommand(Agent agent) {
        return () -> CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_registration");
            
            try {
                // ✅ VALIDATION: Check uniqueness
                if (agentRepository.existsByAgentName(agent.getAgentName())) {
                    throw new IllegalArgumentException("Agent name already exists: " + agent.getAgentName());
                }
                
                // ✅ IMMUTABLE PATTERN: Create initialized agent
                Agent initializedAgent = Agent.builder()
                    .agentName(agent.getAgentName())
                    .agentType(agent.getAgentType())
                    .description(agent.getDescription())
                    .capabilities(agent.getCapabilities())
                    .maxConcurrentTasks(agent.getMaxConcurrentTasks())
                    .userId(agent.getUserId())
                    .status(AgentStatus.INITIALIZING)
                    .currentLoad(0)
                    .successRate(0.0)
                    .averageResponseTime(0L)
                    .totalTasksCompleted(0L)
                    .lastHeartbeat(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
                
                Agent savedAgent = agentRepository.save(initializedAgent);
                
                // ✅ METRICS & LOGGING
                timer.stop(metrics.getApiResponseTime());
                metrics.recordAgentCreated(savedAgent.getAgentType().toString());
                structuredLogger.logAgentCreated(
                    savedAgent.getAgentId().toString(),
                    savedAgent.getAgentType().toString(),
                    savedAgent.getUserId().toString()
                );
                
                return savedAgent;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_registration", e.getClass().getSimpleName());
                structuredLogger.logError("agent_registration", e.getMessage(), e,
                    Map.of("agentName", agent.getAgentName(), "agentType", agent.getAgentType()));
                throw e;
            }
        });
    }
    
    /**
     * ✅ COMMAND: Create Agent Deregistration Command
     */
    public AgentCommand<Void> createDeregisterCommand(Long agentId, String reason) {
        return () -> CompletableFuture.runAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_deregistration");
            
            try {
                Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
                
                // ✅ STATE TRANSITION: Update status to stopping
                agent.setStatus(AgentStatus.SHUTDOWN);
                agent.setUpdatedAt(Instant.now());
                agentRepository.save(agent);
                
                // ✅ METRICS & LOGGING
                timer.stop(metrics.getApiResponseTime());
                metrics.recordAgentDestroyed(agent.getAgentType().toString());
                structuredLogger.logAgentDestroyed(
                    agent.getAgentId().toString(),
                    agent.getAgentType().toString(),
                    agent.getUserId().toString(),
                    reason
                );
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_deregistration", e.getClass().getSimpleName());
                structuredLogger.logError("agent_deregistration", e.getMessage(), e,
                    Map.of("agentId", agentId, "reason", reason));
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * ✅ COMMAND: Create Heartbeat Processing Command
     */
    public AgentCommand<Agent> createHeartbeatCommand(Long agentId) {
        return () -> CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_heartbeat");
            
            try {
                Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
                
                // ✅ STATE MANAGEMENT: Update heartbeat and status
                agent.setLastHeartbeat(Instant.now());
                agent.setUpdatedAt(Instant.now());
                
                // ✅ CONDITIONAL LOGIC: Recover from unresponsive state
                if (agent.getStatus() == AgentStatus.FAILED) {
                    agent.setStatus(AgentStatus.ACTIVE);
                    structuredLogger.logAgentCreated(
                        agent.getAgentId().toString(),
                        agent.getAgentType().toString(),
                        "system_recovery"
                    );
                }
                
                Agent updatedAgent = agentRepository.save(agent);
                
                // ✅ METRICS
                timer.stop(metrics.getApiResponseTime());
                
                return updatedAgent;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_heartbeat", e.getClass().getSimpleName());
                structuredLogger.logError("agent_heartbeat", e.getMessage(), e,
                    Map.of("agentId", agentId));
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * ✅ COMMAND: Create Load Update Command
     */
    public AgentCommand<Agent> createLoadUpdateCommand(Long agentId, int loadDelta) {
        return () -> CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_load_update");
            
            try {
                Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
                
                // ✅ ATOMIC UPDATE: Calculate new load
                int newLoad = Math.max(0, agent.getCurrentLoad() + loadDelta);
                agent.setCurrentLoad(newLoad);
                agent.setUpdatedAt(Instant.now());
                
                // ✅ STATE MANAGEMENT: Update status based on load
                AgentStatus newStatus = determineStatusFromLoad(agent);
                if (newStatus != agent.getStatus()) {
                    agent.setStatus(newStatus);
                    structuredLogger.logBusinessTransaction(
                        "agent_status_change",
                        agent.getAgentId().toString(),
                        "status_update",
                        "system",
                        Map.of("oldStatus", agent.getStatus(), "newStatus", newStatus)
                    );
                }
                
                Agent updatedAgent = agentRepository.save(agent);
                
                // ✅ METRICS
                timer.stop(metrics.getApiResponseTime());
                
                return updatedAgent;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_load_update", e.getClass().getSimpleName());
                structuredLogger.logError("agent_load_update", e.getMessage(), e,
                    Map.of("agentId", agentId, "loadDelta", loadDelta));
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * ✅ COMMAND: Create Performance Update Command
     */
    public AgentCommand<Agent> createPerformanceUpdateCommand(Long agentId, boolean taskSuccess, long responseTimeMs) {
        return () -> CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_performance_update");
            
            try {
                Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
                
                // ✅ FUNCTIONAL PROGRAMMING: Update metrics using pure functions
                agent.updatePerformanceMetrics(taskSuccess, responseTimeMs);
                agent.setUpdatedAt(Instant.now());
                
                Agent updatedAgent = agentRepository.save(agent);
                
                // ✅ METRICS & LOGGING
                timer.stop(metrics.getApiResponseTime());
                structuredLogger.logBusinessTransaction(
                    "agent_performance_update",
                    agent.getAgentId().toString(),
                    "metrics_update",
                    "system",
                    Map.of(
                        "taskSuccess", taskSuccess,
                        "responseTimeMs", responseTimeMs,
                        "newSuccessRate", updatedAgent.getSuccessRate(),
                        "newAverageResponseTime", updatedAgent.getAverageResponseTime()
                    )
                );
                
                return updatedAgent;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_performance_update", e.getClass().getSimpleName());
                structuredLogger.logError("agent_performance_update", e.getMessage(), e,
                    Map.of("agentId", agentId, "taskSuccess", taskSuccess, "responseTimeMs", responseTimeMs));
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * ✅ COMMAND: Create Batch Health Check Command
     */
    public AgentCommand<Map<String, Object>> createHealthCheckCommand() {
        return () -> CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("agent_health_check");
            
            try {
                Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(2));
                
                // ✅ STREAM PROCESSING: Find and update stale agents
                var staleAgents = agentRepository.findAgentsWithStaleHeartbeat(staleThreshold)
                    .stream()
                    .peek(agent -> {
                        agent.setStatus(AgentStatus.FAILED);
                        agent.setUpdatedAt(Instant.now());
                        structuredLogger.logSecurityIncident(
                            "agent_unresponsive",
                            "warning",
                            agent.getUserId().toString(),
                            null,
                            Map.of("agentId", agent.getAgentId(), "agentName", agent.getAgentName())
                        );
                    })
                    .map(agentRepository::save)
                    .toList();
                
                // ✅ BATCH OPERATION: Find overloaded agents
                var overloadedAgents = agentRepository.findOverloadedAgents();
                overloadedAgents.forEach(agent -> 
                    structuredLogger.logSlowOperation(
                        "agent_overloaded",
                        "Agent load: " + agent.getCurrentLoad() + "/" + agent.getMaxConcurrentTasks(),
                        agent.getCurrentLoad(),
                        agent.getMaxConcurrentTasks()
                    )
                );
                
                Map<String, Object> healthReport = Map.of(
                    "staleAgentsUpdated", staleAgents.size(),
                    "overloadedAgents", overloadedAgents.size(),
                    "checkTimestamp", Instant.now()
                );
                
                // ✅ METRICS
                timer.stop(metrics.getApiResponseTime());
                
                return healthReport;
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("agent_health_check", e.getClass().getSimpleName());
                structuredLogger.logError("agent_health_check", e.getMessage(), e, Map.of());
                throw new RuntimeException(e);
            }
        });
    }
    
    // ✅ HELPER METHOD: Pure function for status determination
    private AgentStatus determineStatusFromLoad(Agent agent) {
        if (agent.getCurrentLoad() >= agent.getMaxConcurrentTasks()) {
            return AgentStatus.OVERLOADED;
        } else if (agent.getCurrentLoad() > 0) {
            return AgentStatus.ACTIVE;
        } else {
            return AgentStatus.ACTIVE;
        }
    }
}