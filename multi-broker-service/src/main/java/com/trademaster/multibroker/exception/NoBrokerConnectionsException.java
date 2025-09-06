package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * No Broker Connections Exception
 * 
 * MANDATORY: Functional Error Handling + Immutable Exception + Zero Placeholders
 * 
 * Thrown when a user has no broker connections available for operations.
 * Used for portfolio aggregation when user hasn't connected any brokers.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Error Handling)
 */
public class NoBrokerConnectionsException extends BrokerConnectionException {
    
    /**
     * Create exception for user with no connections
     * 
     * @param userId User identifier
     */
    public NoBrokerConnectionsException(String userId) {
        super(
            String.format("No broker connections found for user: %s", userId),
            null,
            BrokerType.ZERODHA, // Default broker type
            "NO_CONNECTIONS",
            generateCorrelationId(),
            ErrorSeverity.MEDIUM,
            "You haven't connected any brokers yet",
            "Please connect to at least one broker to continue",
            true
        );
    }
    
    /**
     * Create exception for user with no active connections
     * 
     * @param userId User identifier
     * @param totalConnections Total number of connections (inactive)
     */
    public NoBrokerConnectionsException(String userId, int totalConnections) {
        super(
            String.format("No active broker connections for user: %s (total: %d)", userId, totalConnections),
            null,
            BrokerType.ZERODHA, // Default broker type
            "NO_ACTIVE_CONNECTIONS",
            generateCorrelationId(),
            ErrorSeverity.MEDIUM,
            "All your broker connections are currently inactive",
            "Please reconnect to your brokers or check their status",
            true
        );
    }
}