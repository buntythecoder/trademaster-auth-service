package com.trademaster.agentos.state.impl;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ✅ FUNCTIONAL: Shutdown State - Agent terminated (Terminal State)
 * 
 * Final state when agent has been shut down.
 * This is a terminal state with no valid transitions.
 * All operations except information retrieval are rejected.
 */
@Component
public class ShutdownState implements AgentState {
    
    @Override
    public Result<Agent, AgentError> start(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "INITIALIZING", 
            "start"));
    }
    
    @Override
    public Result<Agent, AgentError> stop(Agent agent) {
        // Already shutdown - this is a no-op
        return Result.success(agent);
    }
    
    @Override
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "MAINTENANCE", 
            "enterMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "MAINTENANCE", 
            "exitMaintenance"));
    }
    
    @Override
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "FAILED", 
            "handleFailure"));
    }
    
    @Override
    public Result<Agent, AgentError> recover(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "FAILED", 
            "recover"));
    }
    
    @Override
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "IDLE", 
            "assignTask"));
    }
    
    @Override
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "ACTIVE", 
            "completeTask"));
    }
    
    @Override
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return Result.failure(new AgentError.InvalidState(
            agent.getAgentId(), 
            "SHUTDOWN", 
            "OVERLOADED", 
            "handleOverload"));
    }
    
    @Override
    public String getStateName() {
        return "SHUTDOWN";
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
        return false; // Already shutdown
    }
    
    @Override
    public List<String> getAllowedTransitions() {
        return List.of(); // Terminal state - no transitions allowed
    }
}