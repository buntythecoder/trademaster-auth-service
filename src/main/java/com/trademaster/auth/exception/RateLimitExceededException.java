package com.trademaster.auth.exception;

/**
 * Exception thrown when rate limiting is exceeded
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class RateLimitExceededException extends RuntimeException {
    
    private final String operation;
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String operation, long retryAfterSeconds) {
        super(String.format("Rate limit exceeded for %s operation. Try again in %d seconds.", operation, retryAfterSeconds));
        this.operation = operation;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitExceededException(String operation, long retryAfterSeconds, String message) {
        super(message);
        this.operation = operation;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}