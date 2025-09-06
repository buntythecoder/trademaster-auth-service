package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * Unauthorized Broker Access Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Security Compliance
 */
public class UnauthorizedBrokerAccessException extends BrokerConnectionException {
    
    public UnauthorizedBrokerAccessException(String userId, String connectionId) {
        super(
            "Unauthorized access to broker connection",
            null,
            BrokerType.ZERODHA, // Default broker type
            "UNAUTHORIZED_ACCESS",
            generateCorrelationId(),
            ErrorSeverity.HIGH,
            "You don't have permission to access this broker connection",
            "Verify your account permissions and try again",
            false
        );
    }
    
}