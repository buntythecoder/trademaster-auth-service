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
 * ✅ FUNCTIONAL: Initializing State - Agent starting up
 * 
 * State when agent is being initialized and prepared for operation.
 * Handles initial configuration validation and startup procedures.
 * 
 * Valid Transitions:
 * - start() → IDLE (successful initialization)
 * - handleFailure() → FAILED (initialization failure)
 * - stop() → SHUTDOWN (startup cancelled)
 */
@Component
public class InitializingState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return performInitializationValidation(agent)
            .map(this::transitionToIdle);
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        // Allow shutdown during initialization
        return Result.success(agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
            "IDLE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
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
        failedAgent.recordError("Initialization failed: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
            "ACTIVE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "INITIALIZING", 
            "ACTIVE", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "INITIALIZING";
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
        return List.of("IDLE", "FAILED", "SHUTDOWN");
    }
    
    /**
     * ✅ FUNCTIONAL: Validate initialization requirements
     */
    private Result<Agent, AgentError> performInitializationValidation(Agent agent) {
        return validateAgentConfiguration(agent)
            .flatMap(this::validateAgentCapabilities)
            .flatMap(this::validateResourceAvailability);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent configuration
     */
    private Result<Agent, AgentError> validateAgentConfiguration(Agent agent) {
        return (agent.getMaxConcurrentTasks() == null || agent.getMaxConcurrentTasks() <= 0)
            ? Result.failure(new AgentError.ConfigurationError(
                "AgentInitializer", "maxConcurrentTasks", "Invalid max concurrent tasks configuration"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent capabilities
     */
    private Result<Agent, AgentError> validateAgentCapabilities(Agent agent) {
        return (agent.getCapabilities() == null || agent.getCapabilities().isEmpty())
            ? Result.failure(new AgentError.ConfigurationError(
                "AgentInitializer", "capabilities", "Agent capabilities not configured"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate resource availability
     */
    private Result<Agent, AgentError> validateResourceAvailability(Agent agent) {
        // Simulate resource validation - in real implementation:
        // - Check memory availability
        // - Validate network connectivity
        // - Verify external service dependencies
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Transition agent to IDLE state
     */
    private Agent transitionToIdle(Agent agent) {
        return agent.toBuilder()
            .status(AgentStatus.IDLE)
            .currentLoad(0) // Initialize load to zero
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}