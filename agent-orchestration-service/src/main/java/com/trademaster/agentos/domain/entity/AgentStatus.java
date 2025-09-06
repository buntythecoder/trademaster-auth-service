package com.trademaster.agentos.domain.entity;

import lombok.Getter;

/**
 * Agent Status Enumeration
 * 
 * Defines the operational states of AI agents in the TradeMaster Agent OS.
 * Used for lifecycle management, health monitoring, and task distribution.
 */
@Getter
public enum AgentStatus {
    /**
     * Agent is initializing and bootstrapping
     * - Initial state when agent is created and starting up
     * - Agent is performing initialization and validation tasks
     * - Not yet ready to accept tasks
     */
    INITIALIZING("Initializing", "Agent is starting up and performing initialization"),
    
    /**
     * Agent is idle and ready to accept tasks
     * - Primary operational state when not processing tasks
     * - Agent can accept new tasks up to its capacity
     * - Health checks are passing
     */
    IDLE("Idle", "Agent is ready and waiting for tasks"),
    
    /**
     * Agent is actively processing tasks
     * - Agent is currently executing one or more tasks
     * - Can accept additional tasks up to capacity limit
     * - Normal operational state under load
     */
    ACTIVE("Active", "Agent is actively processing tasks"),
    
    /**
     * Agent is overloaded and at maximum capacity
     * - All task slots are occupied, cannot accept more tasks
     * - New tasks will be queued or routed to other agents
     * - Agent is healthy but fully utilized
     */
    OVERLOADED("Overloaded", "Agent is at maximum capacity and overloaded"),
    
    /**
     * Agent is in planned maintenance mode
     * - Scheduled maintenance, updates, or configuration changes
     * - Not accepting new tasks but may complete existing ones
     * - Will return to operational state after maintenance
     */
    MAINTENANCE("Maintenance", "Agent is undergoing planned maintenance"),
    
    /**
     * Agent has failed and needs recovery
     * - Critical error state requiring intervention
     * - Agent cannot process tasks until recovered
     * - May require manual recovery or restart
     */
    FAILED("Failed", "Agent has failed and requires recovery"),
    
    /**
     * Agent has been shutdown (terminal state)
     * - Final state when agent is permanently stopped
     * - Cannot be restarted without recreating the agent
     * - All resources have been released
     */
    SHUTDOWN("Shutdown", "Agent has been permanently shut down"),
    
    // Legacy states for backward compatibility
    /**
     * @deprecated Use IDLE instead
     */
    @Deprecated
    INACTIVE("Inactive", "Agent is not running and not accepting tasks"),
    
    /**
     * @deprecated Use INITIALIZING instead
     */
    @Deprecated
    STARTING("Starting", "Agent is initializing and starting up"),
    
    /**
     * @deprecated Use OVERLOADED instead
     */
    @Deprecated
    BUSY("Busy", "Agent is at maximum capacity"),
    
    /**
     * @deprecated Use FAILED instead
     */
    @Deprecated
    ERROR("Error", "Agent has encountered an error"),
    
    /**
     * @deprecated Use SHUTDOWN instead
     */
    @Deprecated
    STOPPING("Stopping", "Agent is shutting down gracefully"),
    
    /**
     * @deprecated Use FAILED instead
     */
    @Deprecated
    UNRESPONSIVE("Unresponsive", "Agent is not responding to heartbeat checks");

    private final String displayName;
    private final String description;

    AgentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Check if the agent can accept new tasks in this status
     */
    public boolean canAcceptTasks() {
        return this == IDLE || this == ACTIVE || this == ACTIVE; // Legacy compatibility
    }

    /**
     * Check if the agent is in a healthy operational state
     */
    public boolean isHealthy() {
        return this == IDLE || this == ACTIVE || this == OVERLOADED || this == INITIALIZING ||
           this == BUSY || this == STARTING; // Include legacy states
    }

    /**
     * Check if the agent is in an error state requiring attention
     */
    public boolean isErrorState() {
        return this == FAILED || this == ERROR || this == UNRESPONSIVE;
    }

    /**
     * Check if the agent is in a transitional state
     */
    public boolean isTransitional() {
        return this == INITIALIZING || this == STARTING || this == STOPPING;
    }

    /**
     * Check if the agent is in a terminal state
     */
    public boolean isTerminal() {
        return this == SHUTDOWN;
    }

    /**
     * Get the next allowed status transitions from current status
     */
    public AgentStatus[] getAllowedTransitions() {
        return switch (this) {
            case INITIALIZING -> new AgentStatus[]{IDLE, FAILED};
            case IDLE -> new AgentStatus[]{ACTIVE, MAINTENANCE, SHUTDOWN, FAILED};
            case ACTIVE -> new AgentStatus[]{IDLE, OVERLOADED, FAILED, SHUTDOWN};
            case OVERLOADED -> new AgentStatus[]{ACTIVE, IDLE, FAILED, SHUTDOWN};
            case MAINTENANCE -> new AgentStatus[]{IDLE, FAILED, SHUTDOWN};
            case FAILED -> new AgentStatus[]{IDLE, SHUTDOWN};
            case SHUTDOWN -> new AgentStatus[]{}; // Terminal state
            // Legacy state transitions for backward compatibility
            case INACTIVE -> new AgentStatus[]{STARTING, INITIALIZING};
            case STARTING -> new AgentStatus[]{ACTIVE, IDLE, ERROR, FAILED, STOPPING};
            case BUSY -> new AgentStatus[]{ACTIVE, OVERLOADED, ERROR, FAILED, STOPPING};
            case ERROR -> new AgentStatus[]{ACTIVE, IDLE, FAILED, STOPPING};
            case STOPPING -> new AgentStatus[]{INACTIVE, SHUTDOWN};
            case UNRESPONSIVE -> new AgentStatus[]{ACTIVE, IDLE, FAILED, ERROR, INACTIVE};
        };
    }

    /**
     * Check if transition from current status to target status is allowed
     */
    public boolean canTransitionTo(AgentStatus targetStatus) {
        AgentStatus[] allowedTransitions = getAllowedTransitions();
        for (AgentStatus allowed : allowedTransitions) {
            if (allowed == targetStatus) {
                return true;
            }
        }
        return false;
    }
}