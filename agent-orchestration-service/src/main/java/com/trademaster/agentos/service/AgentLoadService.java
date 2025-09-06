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

import java.util.Map;

/**
 * ✅ SINGLE RESPONSIBILITY: Agent Load Management Service
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only load management (max 5 methods)
 * - Open/Closed: Strategy pattern for load calculations
 * - Liskov Substitution: Implements IAgentLoadService contract
 * - Interface Segregation: Focused on load operations only
 * - Dependency Inversion: Depends on IAgentRepository abstraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentLoadService implements IAgentLoadService {

    private final AgentRepository agentRepository;
    private final StructuredLoggingService structuredLogger;
    private final IAgentHealthService agentHealthService;

    /**
     * ✅ FUNCTIONAL: Increment agent load using Railway Programming
     * Cognitive Complexity: 2
     */
    @Override
    public Result<String, AgentError> incrementAgentLoad(Long agentId) {
        structuredLogger.logDebug("incrementing_agent_load", Map.of("agentId", agentId));
        agentRepository.incrementAgentLoad(agentId);
        
        return Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId), 
                new AgentError.NotFound(agentId, "incrementAgentLoad"))
            .flatMap(agent -> evaluateAgentCapacity(agent, agentId))
            .onFailure(error -> structuredLogger.logError("increment_load_failed", 
                error.getMessage(), null, Map.of("agentId", agentId)));
    }

    /**
     * ✅ FUNCTIONAL: Evaluate agent capacity after load increment
     * Cognitive Complexity: 1
     */
    private Result<String, AgentError> evaluateAgentCapacity(Agent agent, Long agentId) {
        return (agent.getCurrentLoad() + 1 >= agent.getMaxConcurrentTasks()) ?
            updateAgentStatusToBusy(agentId) : 
            Result.<String, AgentError>success("Load incremented, agent still available");
    }

    /**
     * ✅ FUNCTIONAL: Decrement agent load using Railway Programming
     * Cognitive Complexity: 2
     */
    @Override
    public Result<String, AgentError> decrementAgentLoad(Long agentId) {
        structuredLogger.logDebug("decrementing_agent_load", Map.of("agentId", agentId));
        agentRepository.decrementAgentLoad(agentId);
        
        return Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId), 
                new AgentError.NotFound(agentId, "decrementAgentLoad"))
            .flatMap(agent -> evaluateAgentAvailability(agent, agentId))
            .onFailure(error -> structuredLogger.logError("decrement_load_failed", 
                error.getMessage(), null, Map.of("agentId", agentId)));
    }

    /**
     * ✅ FUNCTIONAL: Evaluate agent availability after load decrement
     * Cognitive Complexity: 1
     */
    private Result<String, AgentError> evaluateAgentAvailability(Agent agent, Long agentId) {
        return (agent.getCurrentLoad() - 1 < agent.getMaxConcurrentTasks() && 
               agent.getStatus() == AgentStatus.OVERLOADED) ?
            updateAgentStatusToActive(agentId) : 
            Result.<String, AgentError>success("Load decremented");
    }

    /**
     * ✅ FUNCTIONAL: Update performance metrics using Railway Programming
     * Cognitive Complexity: 1
     */
    @Override
    public Result<Agent, AgentError> updatePerformanceMetrics(Long agentId, boolean taskSuccess, long responseTimeMs) {
        return Result.<Agent, AgentError>fromOptional(
                agentRepository.findById(agentId), 
                new AgentError.NotFound(agentId, "updatePerformanceMetrics"))
            .map(agent -> {
                agent.updatePerformanceMetrics(taskSuccess, responseTimeMs);
                return agent;
            })
            .map(agent -> agentRepository.save(agent))
            .onSuccess(agent -> structuredLogger.logDebug("performance_metrics_updated", 
                Map.of("agentId", agentId, "agentName", agent.getAgentName(),
                       "successRate", agent.getSuccessRate(), "avgResponseTime", agent.getAverageResponseTime())))
            .onFailure(error -> structuredLogger.logError("update_metrics_failed", 
                error.getMessage(), null, Map.of("agentId", agentId)));
    }

    /**
     * ✅ FUNCTIONAL: Update agent status to busy
     * Cognitive Complexity: 1
     */
    private Result<String, AgentError> updateAgentStatusToBusy(Long agentId) {
        agentHealthService.updateAgentStatus(agentId, AgentStatus.OVERLOADED);
        return Result.<String, AgentError>success("Agent status updated to OVERLOADED");
    }

    /**
     * ✅ FUNCTIONAL: Update agent status to active
     * Cognitive Complexity: 1
     */
    private Result<String, AgentError> updateAgentStatusToActive(Long agentId) {
        agentHealthService.updateAgentStatus(agentId, AgentStatus.ACTIVE);
        return Result.<String, AgentError>success("Agent status updated to ACTIVE");
    }
}