package com.trademaster.payment.exception;

/**
 * Exception thrown when payment gateway integration fails
 */
public class GatewayIntegrationException extends PaymentServiceException {
    
    public GatewayIntegrationException(String message) {
        super(message);
    }
    
    public GatewayIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public GatewayIntegrationException(String gateway, String operation, Throwable cause) {
        super("Gateway integration failed for " + gateway + " during " + operation, cause);
    }
}