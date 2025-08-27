package com.trademaster.payment.exception;

import java.util.UUID;

/**
 * Exception thrown when a payment is not found
 */
public class PaymentNotFoundException extends PaymentServiceException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String message, UUID transactionId) {
        super("Payment not found for transaction ID: " + transactionId + " - " + message);
    }
}