package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * ✅ FUNCTIONAL: Error State - Agent in error condition (Deprecated - Use FailedState)
 * 
 * This state is maintained for compatibility but delegates to standard failed state behavior.
 * New implementations should use FailedState.java for consistency.
 * 
 * Valid Transitions:
 * - recover() → IDLE (successful recovery)
 * - stop() → SHUTDOWN (abandon recovery)
 */
@Component
public class ErrorState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
            "IDLE", 
            "start"));
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
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
            "IDLE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
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
        failedAgent.recordError("Additional error in error state: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return performRecoveryProcedure(agent)
            .map(this::transitionToIdle);
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "ERROR", 
            "ACTIVE", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "ERROR";
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
        return true;
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of("IDLE", "SHUTDOWN");
    }
    
    /**
     * ✅ FUNCTIONAL: Perform agent recovery procedure
     */
    private Result<Agent, AgentError> performRecoveryProcedure(Agent agent) {
        return validateRecoveryPreconditions(agent)
            .flatMap(this::resetAgentState);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate preconditions for recovery
     */
    private Result<Agent, AgentError> validateRecoveryPreconditions(Agent agent) {
        return (agent.getLastError() != null && agent.getLastError().contains("FATAL"))
            ? Result.failure(new AgentError.InvalidState(
                agent.getAgentId(), "ERROR", "IDLE", "recover"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Reset agent state for recovery
     */
    private Result<Agent, AgentError> resetAgentState(Agent agent) {
        Agent resetAgent = agent.toBuilder()
            .currentLoad(0)
            .build();
        
        // ✅ FUNCTIONAL: Clear error information for recovery
        resetAgent.clearError();
        
        return Result.success(resetAgent);
    }
    
    /**
     * ✅ FUNCTIONAL: Transition agent to IDLE state
     */
    private Agent transitionToIdle(Agent agent) {
        return agent.toBuilder()
            .status(AgentStatus.IDLE)
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}