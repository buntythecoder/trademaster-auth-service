package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * Broker Connection Not Found Exception
 * 
 * MANDATORY: Functional Error Handling + Immutable Exception + Zero Placeholders
 * 
 * Thrown when a requested broker connection cannot be found.
 * Provides contextual information for debugging and user feedback.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Error Handling)
 */
public class BrokerConnectionNotFoundException extends BrokerConnectionException {
    
    /**
     * Create exception for connection not found
     * 
     * @param connectionId Connection identifier that was not found
     * @param userId User identifier
     */
    public BrokerConnectionNotFoundException(String connectionId, String userId) {
        super(
            String.format("Broker connection not found: connectionId=%s, userId=%s", connectionId, userId),
            null,
            BrokerType.ZERODHA, // Default broker type
            "CONNECTION_NOT_FOUND",
            generateCorrelationId(),
            ErrorSeverity.MEDIUM,
            "The requested broker connection could not be found",
            "Please verify the connection ID and try again, or create a new connection",
            true
        );
    }
    
    /**
     * Create exception for user's connection not found
     * 
     * @param userId User identifier
     * @param brokerType Broker type that was not found
     */
    public BrokerConnectionNotFoundException(String userId, BrokerType brokerType) {
        super(
            String.format("No connection found for user: userId=%s, brokerType=%s", userId, brokerType),
            null,
            brokerType,
            "USER_CONNECTION_NOT_FOUND",
            generateCorrelationId(),
            ErrorSeverity.MEDIUM,
            String.format("No %s connection found for your account", brokerType.getDisplayName()),
            String.format("Please connect to %s first before using this feature", brokerType.getDisplayName()),
            true
        );
    }
}