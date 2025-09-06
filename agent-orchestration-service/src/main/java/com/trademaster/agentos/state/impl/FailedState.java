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
 * ✅ FUNCTIONAL: Failed State - Agent in error condition
 * 
 * State when agent has encountered a failure and needs recovery.
 * Only allows recovery or shutdown operations.
 * 
 * Valid Transitions:
 * - recover() → IDLE (successful recovery)
 * - stop() → SHUTDOWN (abandon recovery)
 */
@Component
public class FailedState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "FAILED", 
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
            "FAILED", 
            "IDLE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "FAILED", 
            "MAINTENANCE", 
            "exitMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        Agent failedAgent = agent.toBuilder()
            .updatedAt(Instant.now())
            .build();
        
        // ✅ FUNCTIONAL: Record error information for monitoring and debugging
        failedAgent.recordError("Additional failure in failed state: " + reason);
        
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
            "FAILED", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "FAILED", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "FAILED", 
            "ACTIVE", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "FAILED";
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
            .flatMap(this::resetAgentState)
            .flatMap(this::validateRecoverySuccess);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate preconditions for recovery
     */
    private Result<Agent, AgentError> validateRecoveryPreconditions(Agent agent) {
        // Check if agent is in a recoverable state
        return (agent.getLastError() != null && agent.getLastError().contains("FATAL"))
            ? Result.failure(new AgentError.InvalidState(
                agent.getAgentId(), "FAILED", "IDLE", "recover"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Reset agent state for recovery
     */
    private Result<Agent, AgentError> resetAgentState(Agent agent) {
        Agent resetAgent = agent.toBuilder()
            .currentLoad(0) // Reset load
            .build();
        
        // ✅ FUNCTIONAL: Clear error information for recovery
        resetAgent.clearError();
        
        return Result.success(resetAgent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate recovery was successful
     */
    private Result<Agent, AgentError> validateRecoverySuccess(Agent agent) {
        return validateAgentConfiguration(agent)
            .flatMap(this::validateAgentCapabilities)
            .flatMap(this::validateAgentHealth);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent configuration
     */
    private Result<Agent, AgentError> validateAgentConfiguration(Agent agent) {
        return (agent.getMaxConcurrentTasks() == null || agent.getMaxConcurrentTasks() <= 0)
            ? Result.failure(new AgentError.ConfigurationError(
                "RecoveryValidator", "maxConcurrentTasks", "Invalid configuration detected during recovery"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent capabilities
     */
    private Result<Agent, AgentError> validateAgentCapabilities(Agent agent) {
        return (agent.getCapabilities() == null || agent.getCapabilities().isEmpty())
            ? Result.failure(new AgentError.ConfigurationError(
                "RecoveryValidator", "capabilities", "Agent capabilities missing during recovery"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent health
     */
    private Result<Agent, AgentError> validateAgentHealth(Agent agent) {
        // Simulate health check - in real implementation:
        // - Check service connectivity
        // - Validate resource availability
        // - Test basic functionality
        return Result.success(agent);
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