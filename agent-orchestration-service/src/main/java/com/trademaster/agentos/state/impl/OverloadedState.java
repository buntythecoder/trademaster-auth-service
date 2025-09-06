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
 * ✅ FUNCTIONAL: Overloaded State - Agent at maximum capacity
 * 
 * State when agent is processing maximum concurrent tasks.
 * Rejects new tasks and waits for task completion to reduce load.
 * 
 * Valid Transitions:
 * - completeTask() → ACTIVE (when load drops below maximum)
 * - completeTask() → IDLE (when all tasks completed)
 * - handleFailure() → FAILED (system failure under load)
 * - stop() → SHUTDOWN (emergency shutdown)
 */
@Component
public class OverloadedState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        // Already running - refresh heartbeat
        return Result.success(agent.toBuilder()
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        // Emergency shutdown - allow but log warning
        return Result.success(agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .lastError("Emergency shutdown while overloaded - tasks may be interrupted")
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "OVERLOADED", 
            "ACTIVE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "OVERLOADED", 
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
        failedAgent.recordError("Agent failed from overloaded state: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "OVERLOADED", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.AgentOverloaded(
            agent.getAgentId(), 
            agent.getCurrentLoad(), 
            agent.getMaxConcurrentTasks()));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return decrementTaskLoad(agent)
            .map(this::determineStateAfterCompletion);
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        // Already overloaded - refresh timestamp
        return Result.success(agent.toBuilder()
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public String getStateName() {
        return "OVERLOADED";
    }
    
    @Override
    public boolean canAcceptTasks() {
        return false;
    }
    
    @Override
    public boolean canEnterMaintenance() {
        return false;
    }
    
    @Override
    public boolean canShutdown() {
        return true; // Emergency shutdown allowed
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of("ACTIVE", "IDLE", "FAILED", "SHUTDOWN");
    }
    
    /**
     * ✅ FUNCTIONAL: Decrement task load for completion
     */
    private Result<Agent, AgentError> decrementTaskLoad(Agent agent) {
        return (agent.getCurrentLoad() <= 0)
            ? Result.failure(new AgentError.InvalidState(
                agent.getAgentId(), "OVERLOADED", "ACTIVE", "completeTask"))
            : Result.success(agent.toBuilder()
                .currentLoad(agent.getCurrentLoad() - 1)
                .totalTasksCompleted(agent.getTotalTasksCompleted() + 1)
                .lastHeartbeat(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }
    
    /**
     * ✅ FUNCTIONAL: Determine state after task completion
     */
    private Agent determineStateAfterCompletion(Agent agent) {
        if (agent.getCurrentLoad() == 0) {
            // All tasks completed - transition to IDLE
            return agent.toBuilder().status(AgentStatus.IDLE).build();
        } else if (agent.getCurrentLoad() < agent.getMaxConcurrentTasks()) {
            // Below capacity - transition to ACTIVE
            return agent.toBuilder().status(AgentStatus.ACTIVE).build();
        } else {
            // Still at capacity - remain OVERLOADED
            return agent;
        }
    }
}