package com.trademaster.multibroker.exception;

/**
 * Order Validation Exception
 * 
 * MANDATORY: Error Handling Patterns - Rule #11
 * 
 * Specialized exception for order validation failures with detailed context
 * for client-side error handling and correction.
 */
public class OrderValidationException extends RuntimeException {
    
    public OrderValidationException(String message) {
        super(message);
    }
    
    public OrderValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}