package com.trademaster.agentos.domain.entity;

/**
 * Task Priority Enumeration
 * 
 * Defines the priority levels for tasks in the TradeMaster Agent OS.
 * Used for task scheduling, queue management, and resource allocation.
 */
public enum TaskPriority {
    /**
     * Critical priority - Immediate execution required
     * - System-critical tasks that must execute immediately
     * - Tasks that prevent system operation if delayed
     * - Risk management alerts and emergency stops
     * - Typical SLA: Execute within 5 seconds
     */
    CRITICAL(5, "Critical", "Must execute immediately - system critical"),
    
    /**
     * High priority - Execute as soon as possible
     * - Time-sensitive trading operations
     * - Real-time alerts and notifications
     * - Order execution and routing tasks
     * - Compliance monitoring and validation
     * - Typical SLA: Execute within 30 seconds
     */
    HIGH(4, "High", "Time-sensitive operations requiring immediate attention"),
    
    /**
     * Normal priority - Standard execution priority
     * - Regular trading and analysis tasks
     * - Portfolio optimization and rebalancing
     * - Market analysis and screening
     * - Standard user requests
     * - Typical SLA: Execute within 5 minutes
     */
    NORMAL(3, "Normal", "Standard priority for regular operations"),
    
    /**
     * Low priority - Execute when resources available
     * - Background analysis and reporting
     * - Historical data processing
     * - Non-urgent optimization tasks
     * - Cleanup and maintenance tasks
     * - Typical SLA: Execute within 30 minutes
     */
    LOW(2, "Low", "Execute when system resources are available"),
    
    /**
     * Deferred priority - Execute during off-peak hours
     * - Heavy computational tasks
     * - Large data exports and reports
     * - System maintenance operations
     * - Batch processing tasks
     * - Typical SLA: Execute within 24 hours
     */
    DEFERRED(1, "Deferred", "Execute during off-peak hours or low system load");

    private final int level;
    private final String displayName;
    private final String description;

    TaskPriority(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the default timeout multiplier for this priority level
     * Higher priority tasks get proportionally longer timeouts
     */
    public double getTimeoutMultiplier() {
        return switch (this) {
            case CRITICAL -> 0.5;   // Shorter timeout for critical tasks
            case HIGH -> 0.8;       // Slightly shorter timeout
            case NORMAL -> 1.0;     // Standard timeout
            case LOW -> 1.5;        // Longer timeout allowed
            case DEFERRED -> 3.0;   // Much longer timeout allowed
        };
    }

    /**
     * Get the queue weight for priority-based scheduling
     * Higher values mean higher priority in the queue
     */
    public int getQueueWeight() {
        return switch (this) {
            case CRITICAL -> 100;
            case HIGH -> 75;
            case NORMAL -> 50;
            case LOW -> 25;
            case DEFERRED -> 10;
        };
    }

    /**
     * Get the resource allocation percentage for this priority
     * Higher priority tasks can use more system resources
     */
    public double getResourceAllocationPercentage() {
        return switch (this) {
            case CRITICAL -> 1.0;   // Can use 100% of available resources
            case HIGH -> 0.8;       // Can use 80% of available resources
            case NORMAL -> 0.6;     // Can use 60% of available resources
            case LOW -> 0.4;        // Can use 40% of available resources
            case DEFERRED -> 0.2;   // Can use 20% of available resources
        };
    }

    /**
     * Get the maximum retry attempts for this priority level
     */
    public int getMaxRetryAttempts() {
        return switch (this) {
            case CRITICAL -> 5;     // More retries for critical tasks
            case HIGH -> 4;         // More retries for high priority
            case NORMAL -> 3;       // Standard retry count
            case LOW -> 2;          // Fewer retries for low priority
            case DEFERRED -> 1;     // Minimal retries for deferred tasks
        };
    }

    /**
     * Get the execution SLA in seconds for this priority level
     */
    public int getExecutionSlaSeconds() {
        return switch (this) {
            case CRITICAL -> 5;         // 5 seconds
            case HIGH -> 30;            // 30 seconds
            case NORMAL -> 300;         // 5 minutes
            case LOW -> 1800;           // 30 minutes
            case DEFERRED -> 86400;     // 24 hours
        };
    }

    /**
     * Check if this priority level can preempt lower priority tasks
     */
    public boolean canPreempt(TaskPriority otherPriority) {
        return this.level > otherPriority.level && 
               (this == CRITICAL || (this == HIGH && otherPriority.level <= 2));
    }

    /**
     * Get the escalation priority when task exceeds SLA
     */
    public TaskPriority getEscalationPriority() {
        return switch (this) {
            case CRITICAL -> CRITICAL;  // Already highest
            case HIGH -> CRITICAL;      // Escalate to critical
            case NORMAL -> HIGH;        // Escalate to high
            case LOW -> NORMAL;         // Escalate to normal
            case DEFERRED -> LOW;       // Escalate to low
        };
    }

    /**
     * Check if this priority requires immediate agent allocation
     */
    public boolean requiresImmediateAllocation() {
        return this == CRITICAL || this == HIGH;
    }

    /**
     * Check if this priority can wait for optimal agent selection
     */
    public boolean canWaitForOptimalAgent() {
        return this == LOW || this == DEFERRED;
    }

    /**
     * Get the color code for UI display
     */
    public String getColorCode() {
        return switch (this) {
            case CRITICAL -> "#dc3545";    // Red
            case HIGH -> "#fd7e14";        // Orange
            case NORMAL -> "#007bff";      // Blue
            case LOW -> "#28a745";         // Green
            case DEFERRED -> "#6c757d";    // Gray
        };
    }

    /**
     * Get the icon name for UI display
     */
    public String getIconName() {
        return switch (this) {
            case CRITICAL -> "alert-triangle";
            case HIGH -> "zap";
            case NORMAL -> "circle";
            case LOW -> "minus-circle";
            case DEFERRED -> "clock";
        };
    }

    /**
     * Get the notification urgency level
     */
    public String getNotificationUrgency() {
        return switch (this) {
            case CRITICAL -> "URGENT";
            case HIGH -> "HIGH";
            case NORMAL -> "NORMAL";
            case LOW -> "LOW";
            case DEFERRED -> "NONE";
        };
    }

    /**
     * Compare priorities for sorting (higher priority first)
     */
    public static java.util.Comparator<TaskPriority> priorityComparator() {
        return (p1, p2) -> Integer.compare(p2.level, p1.level);
    }

    /**
     * Get priority from level number
     */
    public static TaskPriority fromLevel(int level) {
        return switch (level) {
            case 5 -> CRITICAL;
            case 4 -> HIGH;
            case 3 -> NORMAL;
            case 2 -> LOW;
            case 1 -> DEFERRED;
            default -> NORMAL;
        };
    }

    /**
     * Get priority from string name (case insensitive)
     */
    public static TaskPriority fromString(String priority) {
        if (priority == null) return NORMAL;
        
        return switch (priority.toUpperCase().trim()) {
            case "CRITICAL", "URGENT", "EMERGENCY" -> CRITICAL;
            case "HIGH", "IMPORTANT" -> HIGH;
            case "NORMAL", "MEDIUM", "STANDARD" -> NORMAL;
            case "LOW", "MINOR" -> LOW;
            case "DEFERRED", "BACKGROUND" -> DEFERRED;
            default -> NORMAL;
        };
    }
}