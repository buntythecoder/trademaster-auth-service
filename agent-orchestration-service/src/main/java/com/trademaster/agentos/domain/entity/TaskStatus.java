package com.trademaster.agentos.domain.entity;

/**
 * Task Status Enumeration
 * 
 * Defines the lifecycle states of tasks in the TradeMaster Agent OS.
 * Used for task monitoring, progress tracking, and workflow management.
 */
public enum TaskStatus {
    /**
     * Task has been created but not yet assigned to an agent
     * - Initial state when task is created
     * - Waiting in the task queue for agent assignment
     * - Can be cancelled or modified at this stage
     */
    PENDING("Pending", "Task is waiting to be assigned to an agent"),
    
    /**
     * Task has been assigned to an agent and is queued for execution
     * - Agent has been selected but hasn't started execution yet
     * - Task is in the agent's work queue
     * - Agent may be finishing other tasks first
     */
    QUEUED("Queued", "Task is assigned to an agent and queued for execution"),
    
    /**
     * Task is currently being executed by an agent
     * - Agent is actively working on the task
     * - Progress updates may be available
     * - Can be cancelled but may have partial results
     */
    IN_PROGRESS("In Progress", "Task is being executed by an agent"),
    
    /**
     * Task has been paused by the agent or system
     * - Execution temporarily halted
     * - Can be resumed later
     * - May be waiting for external dependencies
     */
    PAUSED("Paused", "Task execution has been temporarily paused"),
    
    /**
     * Task has been completed successfully
     * - Final state for successful execution
     * - Results are available in outputResult field
     * - Cannot be modified after completion
     */
    COMPLETED("Completed", "Task has been completed successfully"),
    
    /**
     * Task execution failed but can be retried
     * - Execution failed due to recoverable error
     * - Will be automatically retried if retries remain
     * - Error details available in errorMessage field
     */
    FAILED("Failed", "Task execution failed but may be retried"),
    
    /**
     * Task encountered an error and cannot be retried
     * - Final error state
     * - Maximum retry attempts reached or unrecoverable error
     * - Requires manual intervention
     */
    ERROR("Error", "Task encountered an unrecoverable error"),
    
    /**
     * Task was cancelled by user or system
     * - Execution was intentionally stopped
     * - Final state, cannot be resumed
     * - May have partial results if cancelled during execution
     */
    CANCELLED("Cancelled", "Task was cancelled by user or system"),
    
    /**
     * Task exceeded its timeout limit
     * - Execution took longer than allowed timeout
     * - May be retried if retries remain
     * - Often indicates resource constraints or complex processing
     */
    TIMEOUT("Timeout", "Task execution exceeded timeout limit"),
    
    /**
     * Task is waiting for external dependencies
     * - Blocked by external system or resource
     * - Will resume when dependencies are available
     * - May timeout if dependencies don't resolve
     */
    WAITING("Waiting", "Task is waiting for external dependencies");

    private final String displayName;
    private final String description;

    TaskStatus(String displayName, String description) {
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
     * Check if the task is in an active execution state
     */
    public boolean isActive() {
        return this == IN_PROGRESS || this == QUEUED || this == PAUSED || this == WAITING;
    }

    /**
     * Check if the task is in a final state (cannot be modified)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == ERROR || this == CANCELLED;
    }

    /**
     * Check if the task can be retried
     */
    public boolean canRetry() {
        return this == FAILED || this == TIMEOUT;
    }

    /**
     * Check if the task can be cancelled
     */
    public boolean canCancel() {
        return !isFinal() && this != CANCELLED;
    }

    /**
     * Check if the task can be paused
     */
    public boolean canPause() {
        return this == IN_PROGRESS || this == WAITING;
    }

    /**
     * Check if the task can be resumed
     */
    public boolean canResume() {
        return this == PAUSED;
    }

    /**
     * Check if the task is in an error state
     */
    public boolean isErrorState() {
        return this == FAILED || this == ERROR || this == TIMEOUT;
    }

    /**
     * Check if the task was successful
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }

    /**
     * Get the next allowed status transitions from current status
     */
    public TaskStatus[] getAllowedTransitions() {
        return switch (this) {
            case PENDING -> new TaskStatus[]{QUEUED, CANCELLED, ERROR};
            case QUEUED -> new TaskStatus[]{IN_PROGRESS, CANCELLED, ERROR, WAITING};
            case IN_PROGRESS -> new TaskStatus[]{COMPLETED, FAILED, ERROR, PAUSED, CANCELLED, TIMEOUT, WAITING};
            case PAUSED -> new TaskStatus[]{IN_PROGRESS, CANCELLED, ERROR};
            case COMPLETED -> new TaskStatus[]{}; // Final state
            case FAILED -> new TaskStatus[]{QUEUED, ERROR, CANCELLED}; // Can retry
            case ERROR -> new TaskStatus[]{}; // Final state
            case CANCELLED -> new TaskStatus[]{}; // Final state
            case TIMEOUT -> new TaskStatus[]{QUEUED, ERROR, CANCELLED}; // Can retry
            case WAITING -> new TaskStatus[]{IN_PROGRESS, QUEUED, CANCELLED, ERROR, TIMEOUT};
        };
    }

    /**
     * Check if transition from current status to target status is allowed
     */
    public boolean canTransitionTo(TaskStatus targetStatus) {
        TaskStatus[] allowedTransitions = getAllowedTransitions();
        for (TaskStatus allowed : allowedTransitions) {
            if (allowed == targetStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the priority order for status (higher number = higher priority for sorting)
     */
    public int getPriorityOrder() {
        return switch (this) {
            case ERROR -> 10;           // Highest priority - needs attention
            case FAILED -> 9;           // High priority - needs retry
            case TIMEOUT -> 8;          // High priority - needs retry
            case IN_PROGRESS -> 7;      // Active execution
            case PAUSED -> 6;           // Paused execution
            case WAITING -> 5;          // Waiting for dependencies
            case QUEUED -> 4;           // Ready to execute
            case PENDING -> 3;          // Waiting for assignment
            case CANCELLED -> 2;        // Cancelled tasks
            case COMPLETED -> 1;        // Lowest priority - already done
        };
    }

    /**
     * Get the color code for UI display
     */
    public String getColorCode() {
        return switch (this) {
            case COMPLETED -> "#28a745";    // Green
            case IN_PROGRESS -> "#007bff";  // Blue
            case PAUSED -> "#ffc107";       // Yellow
            case QUEUED -> "#17a2b8";       // Cyan
            case PENDING -> "#6c757d";      // Gray
            case WAITING -> "#fd7e14";      // Orange
            case FAILED -> "#dc3545";       // Red
            case ERROR -> "#dc3545";        // Red
            case TIMEOUT -> "#e83e8c";      // Pink
            case CANCELLED -> "#6c757d";    // Gray
        };
    }

    /**
     * Get the icon name for UI display
     */
    public String getIconName() {
        return switch (this) {
            case COMPLETED -> "check-circle";
            case IN_PROGRESS -> "play-circle";
            case PAUSED -> "pause-circle";
            case QUEUED -> "clock";
            case PENDING -> "hourglass";
            case WAITING -> "hourglass-half";
            case FAILED -> "x-circle";
            case ERROR -> "alert-circle";
            case TIMEOUT -> "clock";
            case CANCELLED -> "x-circle";
        };
    }
}