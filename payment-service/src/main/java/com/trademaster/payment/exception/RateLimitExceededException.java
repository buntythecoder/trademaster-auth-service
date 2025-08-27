package com.trademaster.payment.exception;

/**
 * Rate Limit Exceeded Exception
 * 
 * Thrown when API rate limits are exceeded.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}