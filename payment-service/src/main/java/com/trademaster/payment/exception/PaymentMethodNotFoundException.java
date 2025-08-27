package com.trademaster.payment.exception;

/**
 * Payment Method Not Found Exception
 * 
 * Thrown when a requested payment method cannot be found.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(String message) {
        super(message);
    }

    public PaymentMethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}