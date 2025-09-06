package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * ✅ FUNCTIONAL: Stopping State - Agent shutdown in progress (Deprecated - Use ShutdownState)
 * 
 * This state is maintained for compatibility but delegates to standard shutdown state behavior.
 * New implementations should use ShutdownState.java for consistency.
 * 
 * Valid Transitions:
 * - stop() → SHUTDOWN (shutdown completed)
 * - handleFailure() → SHUTDOWN (force shutdown on failure)
 */
@Component
public class StoppingState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "IDLE", 
            "start"));
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        // Complete the shutdown process
        return Result.success(agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .updatedAt(Instant.now())
            .build());
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "MAINTENANCE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "MAINTENANCE", 
            "exitMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        // Force shutdown on failure during stopping
        Agent shutdownAgent = agent.toBuilder()
            .status(AgentStatus.SHUTDOWN)
            .updatedAt(Instant.now())
            .build();
        
        // ✅ FUNCTIONAL: Record error information for monitoring and debugging
        shutdownAgent.recordError("Failed during shutdown: " + reason);
        
        return Result.success(shutdownAgent);
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "STOPPING", 
            "ACTIVE", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "STOPPING";
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
        return true; // Already shutting down
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of("SHUTDOWN");
    }
}