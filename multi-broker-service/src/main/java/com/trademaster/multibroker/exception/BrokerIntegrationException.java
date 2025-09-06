package com.trademaster.multibroker.exception;

/**
 * Broker Integration Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Security Compliance
 * 
 * General exception for broker integration failures that don't fit into
 * specific categories like authentication or connection errors.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Error Handling)
 */
public class BrokerIntegrationException extends RuntimeException {
    
    /**
     * Create integration exception with message
     * 
     * @param message Error message
     */
    public BrokerIntegrationException(String message) {
        super(message);
    }
    
    /**
     * Create integration exception with message and cause
     * 
     * @param message Error message
     * @param cause Underlying cause
     */
    public BrokerIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}