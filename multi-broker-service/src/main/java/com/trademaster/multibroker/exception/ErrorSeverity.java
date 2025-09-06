package com.trademaster.multibroker.exception;

/**
 * Error Severity Classification
 * 
 * MANDATORY: Functional Error Handling + Immutable Types + Zero Placeholders
 * 
 * Defines severity levels for authentication and system errors.
 * Used for error handling, monitoring alerts, and incident response.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Error Handling)
 */
public enum ErrorSeverity {
    
    /**
     * Low severity - Minor issues, system recoverable
     * Examples: Rate limiting, temporary network issues
     */
    LOW("Low", 1),
    
    /**
     * Medium severity - Moderate impact, may affect functionality
     * Examples: Token refresh needed, degraded performance
     */
    MEDIUM("Medium", 2),
    
    /**
     * High severity - Significant impact, user action required
     * Examples: Invalid credentials, authorization failures
     */
    HIGH("High", 3),
    
    /**
     * Critical severity - System failure, immediate attention needed
     * Examples: Security breaches, complete service failure
     */
    CRITICAL("Critical", 4);
    
    private final String displayName;
    private final int level;
    
    ErrorSeverity(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this severity is higher than or equal to another
     * 
     * @param other Severity to compare against
     * @return true if this severity is >= other
     */
    public boolean isHigherOrEqual(ErrorSeverity other) {
        return this.level >= other.level;
    }
    
    /**
     * Check if error requires immediate attention
     * 
     * @return true for HIGH or CRITICAL severity
     */
    public boolean requiresImmediateAttention() {
        return this == HIGH || this == CRITICAL;
    }
    
    /**
     * Get alerting priority based on severity
     * 
     * @return Priority for monitoring systems
     */
    public String getAlertPriority() {
        return switch (this) {
            case LOW -> "P4";
            case MEDIUM -> "P3"; 
            case HIGH -> "P2";
            case CRITICAL -> "P1";
        };
    }
    
    /**
     * Get recommended response time in minutes
     * 
     * @return Response time SLA in minutes
     */
    public int getResponseTimeMinutes() {
        return switch (this) {
            case LOW -> 240;        // 4 hours
            case MEDIUM -> 60;      // 1 hour
            case HIGH -> 15;        // 15 minutes
            case CRITICAL -> 5;     // 5 minutes
        };
    }
    
    @Override
    public String toString() {
        return displayName + " (Level " + level + ")";
    }
}