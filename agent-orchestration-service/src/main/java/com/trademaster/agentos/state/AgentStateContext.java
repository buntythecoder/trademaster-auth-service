package com.trademaster.agentos.state;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import com.trademaster.agentos.state.impl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ✅ MANDATORY: State Pattern Context for Agent Lifecycle Management
 * 
 * Central coordinator for agent state transitions using functional composition.
 * Manages state instances and delegates operations to appropriate state handlers.
 * 
 * Features:
 * - Intelligent state resolution based on AgentStatus
 * - Functional error handling with comprehensive logging
 * - State transition validation and enforcement
 * - Thread-safe state operations
 */
@Service
@RequiredArgsConstructor
public class AgentStateContext {
    
    private final InitializingState initializingState;
    private final IdleState idleState;
    private final ActiveState activeState;
    private final OverloadedState overloadedState;
    private final MaintenanceState maintenanceState;
    private final FailedState failedState;
    private final ShutdownState shutdownState;
    private final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ FUNCTIONAL: Execute state operation with logging and validation
     */
    public Result<Agent, AgentError> executeStateOperation(
            Agent agent, 
            String operation,
            java.util.function.Function<AgentState, Result<Agent, AgentError>> stateOperation) {
        
        structuredLogger.logDebug("state_operation_started", 
            Map.of("agentId", agent.getAgentId(),
                   "currentStatus", agent.getStatus(),
                   "operation", operation));
        
        return getStateHandler(agent.getStatus())
            .flatMap(state -> stateOperation.apply(state))
            .onSuccess(updatedAgent -> 
                structuredLogger.logInfo("state_transition_completed",
                    Map.of("agentId", agent.getAgentId(),
                           "oldStatus", agent.getStatus(),
                           "newStatus", updatedAgent.getStatus(),
                           "operation", operation)))
            .onFailure(error -> 
                structuredLogger.logWarning("state_operation_failed",
                    Map.of("agentId", agent.getAgentId(),
                           "currentStatus", agent.getStatus(),
                           "operation", operation,
                           "error", error.getMessage())));
    }
    
    /**
     * ✅ FUNCTIONAL: Start agent with state-aware handling
     */
    public Result<Agent, AgentError> startAgent(Agent agent) {
        return executeStateOperation(agent, "start", state -> state.start(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Stop agent with state-aware handling
     */
    public Result<Agent, AgentError> stopAgent(Agent agent) {
        return executeStateOperation(agent, "stop", state -> state.stop(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Enter maintenance mode
     */
    public Result<Agent, AgentError> enterMaintenance(Agent agent) {
        return executeStateOperation(agent, "enterMaintenance", state -> state.enterMaintenance(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Exit maintenance mode
     */
    public Result<Agent, AgentError> exitMaintenance(Agent agent) {
        return executeStateOperation(agent, "exitMaintenance", state -> state.exitMaintenance(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Handle agent failure
     */
    public Result<Agent, AgentError> handleFailure(Agent agent, String reason) {
        return executeStateOperation(agent, "handleFailure", state -> state.handleFailure(agent, reason));
    }
    
    /**
     * ✅ FUNCTIONAL: Recover agent from failure
     */
    public Result<Agent, AgentError> recoverAgent(Agent agent) {
        return executeStateOperation(agent, "recover", state -> state.recover(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Assign task to agent
     */
    public Result<Agent, AgentError> assignTask(Agent agent) {
        return executeStateOperation(agent, "assignTask", state -> state.assignTask(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Complete task on agent
     */
    public Result<Agent, AgentError> completeTask(Agent agent) {
        return executeStateOperation(agent, "completeTask", state -> state.completeTask(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Handle agent overload
     */
    public Result<Agent, AgentError> handleOverload(Agent agent) {
        return executeStateOperation(agent, "handleOverload", state -> state.handleOverload(agent));
    }
    
    /**
     * ✅ FUNCTIONAL: Check if agent can accept tasks
     */
    public Result<Boolean, AgentError> canAcceptTasks(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(AgentState::canAcceptTasks);
    }
    
    /**
     * ✅ FUNCTIONAL: Check if agent can enter maintenance
     */
    public Result<Boolean, AgentError> canEnterMaintenance(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(AgentState::canEnterMaintenance);
    }
    
    /**
     * ✅ FUNCTIONAL: Check if agent can shutdown
     */
    public Result<Boolean, AgentError> canShutdown(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(AgentState::canShutdown);
    }
    
    /**
     * ✅ FUNCTIONAL: Get allowed transitions for current state
     */
    public Result<java.util.List<String>, AgentError> getAllowedTransitions(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(AgentState::getAllowedTransitions);
    }
    
    /**
     * ✅ FUNCTIONAL: Get current state name
     */
    public Result<String, AgentError> getCurrentStateName(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(AgentState::getStateName);
    }
    
    /**
     * ✅ FUNCTIONAL: Resolve appropriate state handler based on AgentStatus
     */
    private Result<AgentState, AgentError> getStateHandler(AgentStatus status) {
        AgentState handler = switch (status) {
            case INITIALIZING -> initializingState;
            case IDLE -> idleState;
            case ACTIVE -> activeState;
            case OVERLOADED -> overloadedState;
            case MAINTENANCE -> maintenanceState;
            case FAILED -> failedState;
            case SHUTDOWN -> shutdownState;
            // Legacy state mappings for backward compatibility
            case STARTING -> initializingState;
            case INACTIVE -> idleState;
            case BUSY -> overloadedState;
            case ERROR -> failedState;
            case STOPPING -> shutdownState;
            case UNRESPONSIVE -> failedState;
        };
        
        return Result.success(handler);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate state transition
     */
    public Result<Boolean, AgentError> isValidTransition(Agent agent, AgentStatus targetStatus) {
        return getAllowedTransitions(agent)
            .map(transitions -> transitions.contains(targetStatus.toString()));
    }
    
    /**
     * ✅ FUNCTIONAL: Get state information for monitoring
     */
    public Result<Map<String, Object>, AgentError> getStateInfo(Agent agent) {
        return getStateHandler(agent.getStatus())
            .map(state -> Map.of(
                "currentState", state.getStateName(),
                "canAcceptTasks", state.canAcceptTasks(),
                "canEnterMaintenance", state.canEnterMaintenance(),
                "canShutdown", state.canShutdown(),
                "allowedTransitions", state.getAllowedTransitions(),
                "currentLoad", agent.getCurrentLoad(),
                "maxConcurrentTasks", agent.getMaxConcurrentTasks(),
                "lastHeartbeat", agent.getLastHeartbeat(),
                "lastError", agent.getLastError()
            ));
    }
}