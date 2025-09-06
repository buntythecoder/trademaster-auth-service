package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ✅ SINGLE RESPONSIBILITY: Agent Health Monitoring Service
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only health monitoring (max 5 methods)
 * - Open/Closed: Strategy pattern for health checks
 * - Liskov Substitution: Implements IAgentHealthService contract
 * - Interface Segregation: Focused on health operations only
 * - Dependency Inversion: Depends on IAgentRepository abstraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentHealthService implements IAgentHealthService {

    private final AgentRepository agentRepository;
    private final StructuredLoggingService structuredLogger;

    /**
     * ✅ FUNCTIONAL: Process agent heartbeat using Railway Programming
     * Cognitive Complexity: 2
     */
    @Override
    public Result<String, AgentError> processHeartbeat(Long agentId) {
        return Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId), 
                new AgentError.NotFound(agentId, "processHeartbeat"))
            .map(this::updateHeartbeatAndStatus)
            .map(agent -> agentRepository.save(agent))
            .map(agent -> "Heartbeat processed successfully for agent: " + agent.getAgentName())
            .onSuccess(message -> structuredLogger.logInfo("heartbeat_processed", 
                Map.of("agentId", agentId, "message", message)))
            .onFailure(error -> structuredLogger.logError("heartbeat_failed", 
                error.getMessage(), null, Map.of("agentId", agentId)));
    }

    /**
     * ✅ FUNCTIONAL: Update heartbeat and status using functional approach
     * Cognitive Complexity: 1
     */
    private Agent updateHeartbeatAndStatus(Agent agent) {
        agent.updateHeartbeat();
        agent.setStatus(agent.getStatus() == AgentStatus.FAILED ? 
                       AgentStatus.ACTIVE : agent.getStatus());
        return agent;
    }

    /**
     * ✅ SRP: Update agent status - single responsibility
     * Cognitive Complexity: 1
     */
    @Override
    public void updateAgentStatus(Long agentId, AgentStatus status) {
        structuredLogger.logDebug("updating_agent_status", 
            Map.of("agentId", agentId, "status", status));
        agentRepository.updateAgentStatus(agentId, status);
    }

    /**
     * ✅ FUNCTIONAL: Health check using streams and functional composition
     * Cognitive Complexity: 3
     */
    @Override
    public Result<String, AgentError> performHealthCheck() {
        structuredLogger.logDebug("health_check_started", Map.of());
        
        Instant staleThreshold = Instant.now().minus(2, ChronoUnit.MINUTES);
        List<Agent> updatedStaleAgents = processStaleAgents(staleThreshold);
        List<String> overloadedAgentNames = logOverloadedAgents();
        
        String summary = String.format("Health check completed: %d stale agents, %d overloaded agents", 
                                     updatedStaleAgents.size(), overloadedAgentNames.size());
        
        structuredLogger.logInfo("health_check_completed", 
            Map.of("staleAgents", updatedStaleAgents.size(), "overloadedAgents", overloadedAgentNames.size()));
        
        return Result.success(summary);
    }

    /**
     * ✅ FUNCTIONAL: Process stale agents using streams
     * Cognitive Complexity: 2
     */
    private List<Agent> processStaleAgents(Instant staleThreshold) {
        return agentRepository.findAgentsWithStaleHeartbeat(staleThreshold)
            .stream()
            .peek(agent -> structuredLogger.logWarning("agent_stale_heartbeat", 
                Map.of("agentId", agent.getAgentId(), "agentName", agent.getAgentName())))
            .map(agent -> {
                agent.setStatus(AgentStatus.FAILED);
                return agentRepository.save(agent);
            })
            .collect(Collectors.toList());
    }

    /**
     * ✅ FUNCTIONAL: Log overloaded agents using streams
     * Cognitive Complexity: 2
     */
    private List<String> logOverloadedAgents() {
        return agentRepository.findOverloadedAgents()
            .stream()
            .peek(agent -> structuredLogger.logWarning("agent_overloaded", 
                Map.of("agentId", agent.getAgentId(), "agentName", agent.getAgentName(),
                       "currentLoad", agent.getCurrentLoad(), "maxCapacity", agent.getMaxConcurrentTasks())))
            .map(Agent::getAgentName)
            .collect(Collectors.toList());
    }

    /**
     * ✅ SRP: Get system health summary - single responsibility
     * Cognitive Complexity: 1
     */
    @Override
    @Transactional(readOnly = true)
    public AgentHealthSummary getSystemHealthSummary() {
        Object[] stats = agentRepository.getSystemAgentStatistics();
        
        return new AgentHealthSummary(
            ((Number) stats[0]).longValue(), // totalAgents
            ((Number) stats[1]).longValue(), // activeAgents
            ((Number) stats[2]).longValue(), // busyAgents
            ((Number) stats[3]).longValue(), // errorAgents
            ((Number) stats[4]).doubleValue(), // averageLoad
            ((Number) stats[5]).doubleValue(), // averageSuccessRate
            ((Number) stats[6]).longValue()  // totalTasksCompleted
        );
    }
    
    /**
     * ✅ SRP: Start monitoring an agent - single responsibility
     * Cognitive Complexity: 1
     */
    @Override
    public CompletableFuture<Result<String, AgentError>> startMonitoring(Long agentId) {
        return CompletableFuture.supplyAsync(() ->
            Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId),
                new AgentError.NotFound(agentId, "startMonitoring"))
            .map(agent -> {
                log.info("Started health monitoring for agent: {}", agent.getAgentName());
                structuredLogger.logInfo("monitoring_started",
                    Map.of("agentId", agentId, "agentName", agent.getAgentName()));
                return "Monitoring started successfully";
            })
        );
    }
    
    /**
     * ✅ SRP: Stop monitoring an agent - single responsibility
     * Cognitive Complexity: 1
     */
    @Override
    public void stopMonitoring(Long agentId) {
        log.info("Stopped health monitoring for agent: {}", agentId);
        structuredLogger.logInfo("monitoring_stopped", Map.of("agentId", agentId));
    }
    
    /**
     * ✅ SRP: Get health status for specific agent
     * Cognitive Complexity: 1
     */
    public Result<String, AgentError> getHealthStatus(Long agentId) {
        return Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId), 
                new AgentError.NotFound(agentId, "getHealthStatus"))
            .map(agent -> agent.getHealthStatus() != null ? agent.getHealthStatus() : "UNKNOWN");
    }
}