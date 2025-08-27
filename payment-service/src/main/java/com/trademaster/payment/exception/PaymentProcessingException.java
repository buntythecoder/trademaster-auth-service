package com.trademaster.payment.exception;

/**
 * Payment Processing Exception
 * 
 * Custom exception for payment-related errors.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}