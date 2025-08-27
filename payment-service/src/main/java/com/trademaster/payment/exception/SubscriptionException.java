package com.trademaster.payment.exception;

/**
 * Subscription Exception
 * 
 * Thrown when subscription operations encounter errors.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class SubscriptionException extends RuntimeException {

    public SubscriptionException(String message) {
        super(message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}