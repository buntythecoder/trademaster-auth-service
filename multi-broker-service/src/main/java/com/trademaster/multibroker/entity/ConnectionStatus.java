package com.trademaster.multibroker.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Broker Connection Status Enumeration
 * 
 * MANDATORY: Functional Programming + Pattern Matching + Zero Placeholders
 * 
 * Defines all possible states for broker connections with health monitoring.
 * Each status includes priority level for UI display and alerting systems.
 * 
 * Status Flow:
 * DISCONNECTED -> CONNECTING -> CONNECTED -> (HEALTHY/UNHEALTHY cycles)
 * Any state can transition to ERROR or SUSPENDED based on issues
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Connection Health Monitoring)
 */
@Getter
@RequiredArgsConstructor
public enum ConnectionStatus {
    
    /**
     * Broker connection is active and healthy
     * API calls succeeding, real-time data flowing
     */
    CONNECTED("Connected", "success", 1, false),
    
    /**
     * Connection being established
     * OAuth flow in progress, tokens being exchanged
     */
    CONNECTING("Connecting", "info", 2, false),
    
    /**
     * No active connection to broker
     * User has not connected or manually disconnected
     */
    DISCONNECTED("Disconnected", "secondary", 3, false),
    
    /**
     * OAuth tokens have expired
     * Requires user re-authentication or token refresh
     */
    TOKEN_EXPIRED("Token Expired", "warning", 4, true),
    
    /**
     * Rate limit exceeded temporarily
     * Automatic retry after cool-down period
     */
    RATE_LIMITED("Rate Limited", "warning", 5, true),
    
    /**
     * Broker is under maintenance
     * Temporary outage, automatic reconnection when available
     */
    MAINTENANCE("Under Maintenance", "info", 6, true),
    
    /**
     * Connection degraded but functional
     * Some features may be limited, monitoring required
     */
    DEGRADED("Degraded", "warning", 7, true),
    
    /**
     * Connection error or API failure
     * Requires investigation and manual intervention
     */
    ERROR("Error", "danger", 8, true),
    
    /**
     * Account suspended by broker
     * Manual resolution required, contact broker support
     */
    SUSPENDED("Suspended", "danger", 9, true);

    private final String displayName;
    private final String alertLevel; // Bootstrap alert classes
    private final int priority; // Lower = higher priority for sorting
    private final boolean requiresAttention;
    
    /**
     * Check if connection is healthy and can process requests
     * 
     * @return true if connection can handle API calls
     */
    public boolean isHealthy() {
        return switch (this) {
            case CONNECTED -> true;
            case CONNECTING, DISCONNECTED, TOKEN_EXPIRED, RATE_LIMITED, 
                 MAINTENANCE, DEGRADED, ERROR, SUSPENDED -> false;
        };
    }
    
    /**
     * Check if connection can be automatically recovered
     * 
     * @return true if automatic recovery is possible
     */
    public boolean canAutoRecover() {
        return switch (this) {
            case TOKEN_EXPIRED, RATE_LIMITED, MAINTENANCE, DEGRADED -> true;
            case CONNECTED, CONNECTING, DISCONNECTED, ERROR, SUSPENDED -> false;
        };
    }
    
    /**
     * Check if status indicates a temporary issue
     * 
     * @return true if issue is expected to resolve automatically
     */
    public boolean isTemporaryIssue() {
        return switch (this) {
            case RATE_LIMITED, MAINTENANCE, CONNECTING -> true;
            case CONNECTED, DISCONNECTED, TOKEN_EXPIRED, ERROR, SUSPENDED, DEGRADED -> false;
        };
    }
    
    /**
     * Get next status after successful recovery
     * 
     * @return Expected status after recovery
     */
    public ConnectionStatus getRecoveryStatus() {
        return switch (this) {
            case TOKEN_EXPIRED, RATE_LIMITED, MAINTENANCE, DEGRADED, ERROR -> CONNECTED;
            case CONNECTING -> CONNECTED;
            case SUSPENDED -> DISCONNECTED; // Manual reconnection required
            case CONNECTED, DISCONNECTED -> this; // No change needed
        };
    }
    
    /**
     * Get recommended retry delay for this status
     * 
     * @return Retry delay in seconds
     */
    public long getRetryDelaySeconds() {
        return switch (this) {
            case RATE_LIMITED -> 60L; // Wait for rate limit reset
            case TOKEN_EXPIRED -> 5L; // Quick retry after token refresh
            case MAINTENANCE -> 300L; // 5 minutes for maintenance
            case ERROR -> 30L; // Standard error retry
            case CONNECTING -> 10L; // Connection timeout retry
            default -> 0L; // No automatic retry
        };
    }
    
    /**
     * Check if status should trigger user notification
     * 
     * @return true if user should be notified
     */
    public boolean shouldNotifyUser() {
        return requiresAttention && switch (this) {
            case TOKEN_EXPIRED, ERROR, SUSPENDED -> true;
            case RATE_LIMITED, MAINTENANCE, DEGRADED, CONNECTING, CONNECTED, DISCONNECTED -> false; // System handles these
        };
    }
    
    /**
     * Get status from string value using pattern matching
     * 
     * @param statusValue String representation
     * @return Optional ConnectionStatus
     */
    public static java.util.Optional<ConnectionStatus> fromString(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        
        return java.util.Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(statusValue.trim()) ||
                             status.displayName.equalsIgnoreCase(statusValue.trim()))
            .findFirst();
    }
}