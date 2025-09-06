package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * Broker Not Found Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Security Compliance
 */
public class BrokerNotFoundException extends BrokerConnectionException {
    
    public BrokerNotFoundException(BrokerType brokerType) {
        super(
            "Broker connection not found",
            brokerType,
            "BROKER_NOT_FOUND",
            generateCorrelationId()
        );
    }
    
    public BrokerNotFoundException(String connectionId) {
        super(
            "Broker connection not found: " + connectionId,
            null,
            "CONNECTION_NOT_FOUND",
            generateCorrelationId()
        );
    }
    
}