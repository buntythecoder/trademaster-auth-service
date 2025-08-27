package com.trademaster.payment.exception;

/**
 * Base exception for payment service operations
 */
public class PaymentServiceException extends RuntimeException {
    
    public PaymentServiceException(String message) {
        super(message);
    }
    
    public PaymentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}