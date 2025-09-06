package com.trademaster.agentos.state.impl;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.state.AgentState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * ✅ FUNCTIONAL: Idle State - Agent ready and waiting for tasks
 * 
 * Primary operational state where agent is available for task assignment.
 * Handles transitions to ACTIVE, MAINTENANCE, or SHUTDOWN states.
 * 
 * Valid Transitions:
 * - assignTask() → ACTIVE (when task assigned)
 * - enterMaintenance() → MAINTENANCE (planned maintenance)
 * - stop() → SHUTDOWN (graceful shutdown)
 * - handleFailure() → FAILED (unexpected failure)
 */
@Component
public class IdleState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        // Already started - this is a no-op in IDLE state
        return Result.success(agent.toBuilder()
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        return Result.success(agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.success(agent.toBuilder()
            .status(AgentStatus.MAINTENANCE)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "IDLE", 
            "MAINTENANCE", 
            "exitMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        Agent failedAgent = agent.toBuilder()
            .status(AgentStatus.FAILED)
            .updatedAt(Instant.now())
            .build();
        
        // ✅ FUNCTIONAL: Record error information for monitoring and debugging
        failedAgent.recordError("Agent failed from idle state: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "IDLE", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return validateTaskCapacity(agent)
            .map(this::transitionToActive);
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "IDLE", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "IDLE", 
            "OVERLOADED", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "IDLE";
    }
    
    @Override
    public boolean canAcceptTasks() {
        return true;
    }
    
    @Override
    public boolean canEnterMaintenance() {
        return true;
    }
    
    @Override
    public boolean canShutdown() {
        return true;
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of("ACTIVE", "MAINTENANCE", "SHUTDOWN", "FAILED");
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent has capacity for tasks
     */
    private Result<Agent, AgentError> validateTaskCapacity(Agent agent) {
        return (agent.getCurrentLoad() >= agent.getMaxConcurrentTasks())
            ? Result.failure(new AgentError.AgentOverloaded(
                agent.getAgentId(), agent.getCurrentLoad(), agent.getMaxConcurrentTasks()))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Transition agent to ACTIVE state
     */
    private Agent transitionToActive(Agent agent) {
        return agent.toBuilder()
            .status(AgentStatus.ACTIVE)
            .currentLoad(agent.getCurrentLoad() + 1)
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}