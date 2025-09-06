package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * ✅ FUNCTIONAL: Unresponsive State - Agent not responding to heartbeats
 * 
 * State when agent has missed heartbeat timeouts and is considered unresponsive.
 * Recovery attempts and forced shutdown are available options.
 * 
 * Valid Transitions:
 * - recover() → IDLE (successful recovery)
 * - handleFailure() → FAILED (mark as failed)
 * - stop() → SHUTDOWN (force shutdown)
 */
@Component
public class UnresponsiveState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
            "IDLE", 
            "start"));
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        // Force shutdown of unresponsive agent
        return Result.success(agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
            "IDLE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
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
        failedAgent.recordError("Unresponsive agent failure: " + reason);
        
        return Result.success(failedAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return performUnresponsiveRecovery(agent)
            .map(this::transitionToIdle);
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "UNRESPONSIVE", 
            "ACTIVE", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "UNRESPONSIVE";
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
     * ✅ FUNCTIONAL: Perform unresponsive agent recovery
     */
    private Result<Agent, AgentError> performUnresponsiveRecovery(Agent agent) {
        return validateHeartbeatRecovery(agent)
            .flatMap(this::resetAgentHeartbeat);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate heartbeat recovery conditions
     */
    private Result<Agent, AgentError> validateHeartbeatRecovery(Agent agent) {
        Instant now = Instant.now();
        Instant lastHeartbeat = agent.getLastHeartbeat();
        
        // Check if agent has been unresponsive too long (more than 5 minutes)
        if (lastHeartbeat != null && now.isAfter(lastHeartbeat.plusSeconds(300))) {
            return Result.failure(new AgentError.InvalidState(
                agent.getAgentId(), "UNRESPONSIVE", "FAILED", "recover"));
        }
        
        return Result.success(agent);
    }
    
    /**
     * ✅ FUNCTIONAL: Reset agent heartbeat for recovery
     */
    private Result<Agent, AgentError> resetAgentHeartbeat(Agent agent) {
        Agent recoveredAgent = agent.toBuilder()
            .currentLoad(0) // Reset load
            .lastHeartbeat(Instant.now()) // Update heartbeat
            .build();
        
        // ✅ FUNCTIONAL: Clear error information for recovery
        recoveredAgent.clearError();
        
        return Result.success(recoveredAgent);
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