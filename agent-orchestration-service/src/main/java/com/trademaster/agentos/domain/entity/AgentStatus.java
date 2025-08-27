package com.trademaster.agentos.domain.entity;

/**
 * Agent Status Enumeration
 * 
 * Defines the operational states of AI agents in the TradeMaster Agent OS.
 * Used for lifecycle management, health monitoring, and task distribution.
 */
public enum AgentStatus {
    /**
     * Agent is inactive and not accepting tasks
     * - Initial state when agent is created
     * - State when agent is intentionally stopped
     * - No tasks will be assigned to inactive agents
     */
    INACTIVE("Inactive", "Agent is not running and not accepting tasks"),
    
    /**
     * Agent is starting up and initializing
     * - Transitional state during agent startup
     * - Agent is performing initialization tasks
     * - Not yet ready to accept new tasks
     */
    STARTING("Starting", "Agent is initializing and starting up"),
    
    /**
     * Agent is active and ready to accept tasks
     * - Normal operational state
     * - Agent can accept new tasks up to its capacity
     * - Health checks are passing
     */
    ACTIVE("Active", "Agent is running and accepting tasks"),
    
    /**
     * Agent is busy and at maximum capacity
     * - All task slots are occupied
     * - New tasks will be queued or routed to other agents
     * - Agent is healthy but fully utilized
     */
    BUSY("Busy", "Agent is at maximum capacity"),
    
    /**
     * Agent has encountered an error but may recover
     * - Temporary error state
     * - Agent will attempt to recover automatically
     * - No new tasks assigned during error state
     */
    ERROR("Error", "Agent has encountered an error"),
    
    /**
     * Agent is being gracefully shut down
     * - Transitional state during shutdown
     * - Completing current tasks but not accepting new ones
     * - Will transition to INACTIVE when all tasks complete
     */
    STOPPING("Stopping", "Agent is shutting down gracefully"),
    
    /**
     * Agent is unresponsive and may have failed
     * - No heartbeat received within timeout period
     * - Tasks may need to be reassigned to other agents
     * - Requires manual intervention or restart
     */
    UNRESPONSIVE("Unresponsive", "Agent is not responding to heartbeat checks");

    private final String displayName;
    private final String description;

    AgentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the agent can accept new tasks in this status
     */
    public boolean canAcceptTasks() {
        return this == ACTIVE;
    }

    /**
     * Check if the agent is in a healthy operational state
     */
    public boolean isHealthy() {
        return this == ACTIVE || this == BUSY || this == STARTING;
    }

    /**
     * Check if the agent is in an error state requiring attention
     */
    public boolean isErrorState() {
        return this == ERROR || this == UNRESPONSIVE;
    }

    /**
     * Check if the agent is in a transitional state
     */
    public boolean isTransitional() {
        return this == STARTING || this == STOPPING;
    }

    /**
     * Get the next allowed status transitions from current status
     */
    public AgentStatus[] getAllowedTransitions() {
        return switch (this) {
            case INACTIVE -> new AgentStatus[]{STARTING};
            case STARTING -> new AgentStatus[]{ACTIVE, ERROR, STOPPING};
            case ACTIVE -> new AgentStatus[]{BUSY, ERROR, STOPPING, UNRESPONSIVE};
            case BUSY -> new AgentStatus[]{ACTIVE, ERROR, STOPPING, UNRESPONSIVE};
            case ERROR -> new AgentStatus[]{ACTIVE, STOPPING, UNRESPONSIVE};
            case STOPPING -> new AgentStatus[]{INACTIVE};
            case UNRESPONSIVE -> new AgentStatus[]{ACTIVE, ERROR, INACTIVE};
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