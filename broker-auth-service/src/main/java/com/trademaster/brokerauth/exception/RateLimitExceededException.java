package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.enums.BrokerType;
import lombok.Getter;

/**
 * Rate Limit Exceeded Exception
 * 
 * Custom exception for rate limiting violations.
 * Includes retry information and broker context.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public class RateLimitExceededException extends RuntimeException {
    
    private final BrokerType brokerType;
    private final Long userId;
    private final String rateLimitType;
    private final long retryAfterSeconds;
    private final int currentRequests;
    private final int maxRequests;
    
    public RateLimitExceededException(String message, BrokerType brokerType, Long userId, 
                                    String rateLimitType, long retryAfterSeconds) {
        super(message);
        this.brokerType = brokerType;
        this.userId = userId;
        this.rateLimitType = rateLimitType;
        this.retryAfterSeconds = retryAfterSeconds;
        this.currentRequests = 0;
        this.maxRequests = 0;
    }
    
    public RateLimitExceededException(String message, BrokerType brokerType, Long userId, 
                                    String rateLimitType, long retryAfterSeconds, 
                                    int currentRequests, int maxRequests) {
        super(message);
        this.brokerType = brokerType;
        this.userId = userId;
        this.rateLimitType = rateLimitType;
        this.retryAfterSeconds = retryAfterSeconds;
        this.currentRequests = currentRequests;
        this.maxRequests = maxRequests;
    }
    
    public RateLimitExceededException(String message, BrokerType brokerType, Long userId, 
                                    String rateLimitType, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.brokerType = brokerType;
        this.userId = userId;
        this.rateLimitType = rateLimitType;
        this.retryAfterSeconds = retryAfterSeconds;
        this.currentRequests = 0;
        this.maxRequests = 0;
    }
    
    // Static factory methods for common scenarios
    
    public static RateLimitExceededException apiRateLimit(BrokerType brokerType, Long userId, 
                                                        int current, int max, long retryAfter) {
        return new RateLimitExceededException(
            String.format("API rate limit exceeded for %s: %d/%d requests", brokerType, current, max),
            brokerType,
            userId,
            "API_RATE_LIMIT",
            retryAfter,
            current,
            max
        );
    }
    
    public static RateLimitExceededException authenticationRateLimit(BrokerType brokerType, Long userId, long retryAfter) {
        return new RateLimitExceededException(
            String.format("Authentication rate limit exceeded for %s", brokerType),
            brokerType,
            userId,
            "AUTH_RATE_LIMIT",
            retryAfter
        );
    }
    
    public static RateLimitExceededException sessionCreationRateLimit(BrokerType brokerType, Long userId, long retryAfter) {
        return new RateLimitExceededException(
            String.format("Session creation rate limit exceeded for %s", brokerType),
            brokerType,
            userId,
            "SESSION_CREATION_RATE_LIMIT",
            retryAfter
        );
    }
    
    public static RateLimitExceededException tokenRefreshRateLimit(BrokerType brokerType, Long userId, long retryAfter) {
        return new RateLimitExceededException(
            String.format("Token refresh rate limit exceeded for %s", brokerType),
            brokerType,
            userId,
            "TOKEN_REFRESH_RATE_LIMIT",
            retryAfter
        );
    }
    
    public static RateLimitExceededException globalRateLimit(Long userId, long retryAfter) {
        return new RateLimitExceededException(
            "Global rate limit exceeded",
            null,
            userId,
            "GLOBAL_RATE_LIMIT",
            retryAfter
        );
    }
}