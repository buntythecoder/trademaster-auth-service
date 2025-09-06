package com.trademaster.multibroker.exception;

/**
 * Order Execution Exception
 * 
 * MANDATORY: Error Handling Patterns - Rule #11
 * 
 * Specialized exception for order execution failures with detailed context
 * for troubleshooting and recovery mechanisms.
 */
public class OrderExecutionException extends RuntimeException {
    
    public OrderExecutionException(String message) {
        super(message);
    }
    
    public OrderExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}