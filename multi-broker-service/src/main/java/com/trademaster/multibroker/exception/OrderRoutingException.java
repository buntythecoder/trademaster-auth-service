package com.trademaster.multibroker.exception;

/**
 * Order Routing Exception
 * 
 * MANDATORY: Error Handling Patterns - Rule #11
 * 
 * Specialized exception for order routing failures with detailed context
 * for troubleshooting and recovery mechanisms.
 */
public class OrderRoutingException extends RuntimeException {
    
    public OrderRoutingException(String message) {
        super(message);
    }
    
    public OrderRoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}