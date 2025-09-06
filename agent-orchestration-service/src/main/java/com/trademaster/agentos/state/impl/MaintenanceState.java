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
 * ✅ FUNCTIONAL: Maintenance State - Agent undergoing planned maintenance
 * 
 * State for planned maintenance, updates, or configuration changes.
 * Rejects new tasks and waits for maintenance completion.
 * 
 * Valid Transitions:
 * - exitMaintenance() → IDLE (maintenance completed successfully)
 * - handleFailure() → FAILED (maintenance failure)
 * - stop() → SHUTDOWN (maintenance cancelled/shutdown)
 */
@Component
public class MaintenanceState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "MAINTENANCE", 
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
        // Already in maintenance - refresh timestamp
        return Result.success(agent.toBuilder()
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return performMaintenanceValidation(agent)
            .map(this::transitionToIdle);
    }
    
    @Override
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        Agent failedAgent = agent.toBuilder()
            .status(AgentStatus.FAILED)
            .updatedAt(Instant.now())
            .build();
        
        // ✅ FUNCTIONAL: Record error information for monitoring and debugging
        failedAgent.recordError("Maintenance failed: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "MAINTENANCE", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "MAINTENANCE", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "MAINTENANCE", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "MAINTENANCE", 
            "OVERLOADED", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "MAINTENANCE";
    }
    
    @Override
    public boolean canAcceptTasks() {
        return false;
    }
    
    @Override
    public boolean canEnterMaintenance() {
        return true; // Already in maintenance
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
     * ✅ FUNCTIONAL: Perform post-maintenance validation
     */
    private Result<Agent, AgentError> performMaintenanceValidation(Agent agent) {
        return validateAgentHealth(agent)
            .flatMap(this::validateAgentConfiguration)
            .flatMap(this::validateAgentCapabilities);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent health after maintenance
     */
    private Result<Agent, AgentError> validateAgentHealth(Agent agent) {
        // Simulate health check - in real implementation, this would check:
        // - Network connectivity
        // - Resource availability
        // - Service dependencies
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent configuration after maintenance
     */
    private Result<Agent, AgentError> validateAgentConfiguration(Agent agent) {
        return (agent.getMaxConcurrentTasks() == null || agent.getMaxConcurrentTasks() <= 0)
            ? Result.failure(new AgentError.ConfigurationError(
                "MaintenanceValidator", "maxConcurrentTasks", "Invalid max concurrent tasks configuration"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate agent capabilities after maintenance
     */
    private Result<Agent, AgentError> validateAgentCapabilities(Agent agent) {
        return (agent.getCapabilities() == null || agent.getCapabilities().isEmpty())
            ? Result.failure(new AgentError.ConfigurationError(
                "MaintenanceValidator", "capabilities", "Agent capabilities not properly configured"))
            : Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Transition agent to IDLE state
     */
    private Agent transitionToIdle(Agent agent) {
        return agent.toBuilder()
            .status(AgentStatus.IDLE)
            .currentLoad(0) // Reset load after maintenance
            .lastHeartbeat(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}