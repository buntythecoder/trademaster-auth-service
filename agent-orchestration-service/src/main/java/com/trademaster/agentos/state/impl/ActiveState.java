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
 * ✅ FUNCTIONAL: Active State - Agent executing tasks
 * 
 * State when agent is actively processing tasks.
 * Handles task completion, overload conditions, and graceful transitions.
 * 
 * Valid Transitions:
 * - completeTask() → IDLE (when last task completed)
 * - completeTask() → ACTIVE (when more tasks remain)
 * - assignTask() → ACTIVE or OVERLOADED (based on capacity)
 * - handleOverload() → OVERLOADED (capacity exceeded)
 * - handleFailure() → FAILED (task failure)
 * - stop() → SHUTDOWN (emergency shutdown)
 */
@Component
public class ActiveState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        // Already active - refresh heartbeat
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
            .lastError("Emergency shutdown while active - tasks may be interrupted")
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ACTIVE", 
            "IDLE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ACTIVE", 
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
        failedAgent.recordError("Agent failed from active state: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ACTIVE", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return validateAdditionalTaskCapacity(agent)
            .map(this::incrementTaskLoad);
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return decrementTaskLoad(agent)
            .map(this::determineStateAfterCompletion);
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.success(agent.toBuilder()
            .status(AgentStatus.OVERLOADED)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public String getStateName() {
        return "ACTIVE";
    }
    
    @Override
    public boolean canAcceptTasks() {
        return true; // Can accept up to capacity limit
    }
    
    @Override
    public boolean canEnterMaintenance() {
        return false; // Must complete tasks first
    }
    
    @Override
    public boolean canShutdown() {
        return true; // Emergency shutdown allowed
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of("IDLE", "OVERLOADED", "FAILED", "SHUTDOWN");
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent can accept additional tasks
     */
    private Result<Agent, AgentError> validateAdditionalTaskCapacity(Agent agent) {
        int availableCapacity = agent.getMaxConcurrentTasks() - agent.getCurrentLoad();
        
        return (availableCapacity <= 0)
            ? Result.failure(new AgentError.AgentOverloaded(
                agent.getAgentId(), agent.getCurrentLoad(), agent.getMaxConcurrentTasks()))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Increment task load for new assignment
     */
    private Agent incrementTaskLoad(Agent agent) {
        int newLoad = agent.getCurrentLoad() + 1;
        
        return agent.toBuilder()
            .currentLoad(newLoad)
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
    
    /**
     * ✅ FUNCTIONAL: Decrement task load for completion
     */
    private Result<Agent, AgentError> decrementTaskLoad(Agent agent) {
        return (agent.getCurrentLoad() <= 0)
            ? Result.failure(new AgentError.InvalidState(
                agent.getAgentId(), "ACTIVE", "BUSY", "completeTask"))
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
        return (agent.getCurrentLoad() == 0)
            ? agent.toBuilder().status(AgentStatus.IDLE).build()
            : agent; // Stay ACTIVE if more tasks remain
    }
}