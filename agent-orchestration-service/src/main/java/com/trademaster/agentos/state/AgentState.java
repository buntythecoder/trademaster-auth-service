package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;

/**
 * ✅ MANDATORY: State Pattern Interface for Agent Lifecycle Management
 * 
 * Defines the contract for all agent states in the TradeMaster Agent OS.
 * Each state manages its own transitions and behaviors using functional programming.
 * 
 * State Transitions:
 * - INITIALIZING → IDLE, FAILED
 * - IDLE → ACTIVE, MAINTENANCE, SHUTDOWN
 * - ACTIVE → IDLE, OVERLOADED, FAILED, SHUTDOWN
 * - OVERLOADED → ACTIVE, IDLE, FAILED
 * - MAINTENANCE → IDLE, FAILED
 * - FAILED → IDLE (after recovery)
 * - SHUTDOWN → (terminal state)
 */
public interface AgentState {
    
    /**
     * ✅ FUNCTIONAL: Start the agent from current state
     */
    Result<Agent, AgentError> start(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Stop the agent from current state
     */
    Result<Agent, AgentError> stop(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Put agent into maintenance mode
     */
    Result<Agent, AgentError> enterMaintenance(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Resume agent from maintenance
     */
    Result<Agent, AgentError> exitMaintenance(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Handle agent failure
     */
    Result<Agent, AgentError> handleFailure(Agent agent, String reason);
    
    /**
     * ✅ FUNCTIONAL: Recover agent from failure
     */
    Result<Agent, AgentError> recover(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Assign task to agent
     */
    Result<Agent, AgentError> assignTask(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Complete task assignment
     */
    Result<Agent, AgentError> completeTask(Agent agent);
    
    /**
     * ✅ FUNCTIONAL: Handle overload condition
     */
    Result<Agent, AgentError> handleOverload(Agent agent);
    
    /**
     * Get the current state name
     */
    String getStateName();
    
    /**
     * Check if state allows task assignment
     */
    boolean canAcceptTasks();
    
    /**
     * Check if state allows maintenance operations
     */
    boolean canEnterMaintenance();
    
    /**
     * Check if state allows shutdown
     */
    boolean canShutdown();
    
    /**
     * Get allowed transitions from current state
     */
    java.util.List<String> getAllowedTransitions();
}