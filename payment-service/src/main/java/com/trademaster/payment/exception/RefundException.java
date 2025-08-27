package com.trademaster.payment.exception;

/**
 * Refund Exception
 * 
 * Thrown when refund operations encounter errors.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class RefundException extends RuntimeException {

    public RefundException(String message) {
        super(message);
    }

    public RefundException(String message, Throwable cause) {
        super(message, cause);
    }
}