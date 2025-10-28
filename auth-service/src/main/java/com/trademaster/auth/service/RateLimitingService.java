package com.trademaster.auth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Bucket4j for token bucket algorithm
 * 
 * Provides different rate limiting policies for various operations:
 * - Login attempts: Stricter limits to prevent brute force attacks
 * - Registration: Moderate limits to prevent spam
 * - Password reset: Conservative limits to prevent abuse
 * - Email verification: Lenient limits for legitimate use
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class RateLimitingService {

    // Rate limit configurations from application properties
    @Value("${trademaster.rate-limit.login.requests:5}")
    private int loginRequestsPerMinute;
    
    @Value("${trademaster.rate-limit.login.window-minutes:1}")
    private int loginWindowMinutes;
    
    @Value("${trademaster.rate-limit.registration.requests:3}")
    private int registrationRequestsPerHour;
    
    @Value("${trademaster.rate-limit.registration.window-minutes:60}")
    private int registrationWindowMinutes;
    
    @Value("${trademaster.rate-limit.password-reset.requests:2}")
    private int passwordResetRequestsPerHour;
    
    @Value("${trademaster.rate-limit.password-reset.window-minutes:60}")
    private int passwordResetWindowMinutes;
    
    @Value("${trademaster.rate-limit.email-verification.requests:10}")
    private int emailVerificationRequestsPerHour;
    
    @Value("${trademaster.rate-limit.email-verification.window-minutes:60}")
    private int emailVerificationWindowMinutes;

    /**
     * In-memory bucket storage using ConcurrentHashMap for thread-safe rate limiting.
     *
     * Design Decision: In-memory storage is appropriate for rate limiting because:
     * 1. Rate limits are short-lived (minutes to hours) - loss of state on restart is acceptable
     * 2. ConcurrentHashMap provides thread-safe operations without external dependencies
     * 3. Performance is optimal with local memory access vs network calls to Redis
     * 4. Simplifies deployment and reduces operational complexity
     * 5. Bucket4j automatically handles token refill and expiration
     *
     * Note: If horizontal scaling across multiple instances is required, consider:
     * - Bucket4j Redis integration with spring-data-redis
     * - Sticky sessions at load balancer to route users to same instance
     * - Distributed rate limiting with bucket4j-redis module
     */
    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registrationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> passwordResetBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> emailVerificationBuckets = new ConcurrentHashMap<>();

    /**
     * Check if login attempt is allowed for the given identifier (IP or email)
     * 
     * @param identifier The IP address or email to check
     * @return true if login attempt is allowed, false if rate limited
     */
    public boolean isLoginAllowed(String identifier) {
        return consumeToken(loginBuckets, identifier, loginRequestsPerMinute, loginWindowMinutes, "LOGIN");
    }

    /**
     * Check if registration attempt is allowed for the given IP address
     * 
     * @param ipAddress The IP address to check
     * @return true if registration attempt is allowed, false if rate limited
     */
    public boolean isRegistrationAllowed(String ipAddress) {
        return consumeToken(registrationBuckets, ipAddress, registrationRequestsPerHour, 
                          registrationWindowMinutes, "REGISTRATION");
    }

    /**
     * Check if password reset request is allowed for the given identifier
     * 
     * @param identifier The email or IP address to check
     * @return true if password reset request is allowed, false if rate limited
     */
    public boolean isPasswordResetAllowed(String identifier) {
        return consumeToken(passwordResetBuckets, identifier, passwordResetRequestsPerHour, 
                          passwordResetWindowMinutes, "PASSWORD_RESET");
    }

    /**
     * Check if email verification request is allowed for the given identifier
     * 
     * @param identifier The email or IP address to check
     * @return true if email verification request is allowed, false if rate limited
     */
    public boolean isEmailVerificationAllowed(String identifier) {
        return consumeToken(emailVerificationBuckets, identifier, emailVerificationRequestsPerHour, 
                          emailVerificationWindowMinutes, "EMAIL_VERIFICATION");
    }

    /**
     * Get remaining tokens for login attempts
     * 
     * @param identifier The IP address or email to check
     * @return number of remaining tokens
     */
    public long getRemainingLoginAttempts(String identifier) {
        Bucket bucket = getOrCreateBucket(loginBuckets, identifier, loginRequestsPerMinute, loginWindowMinutes);
        return bucket.getAvailableTokens();
    }

    /**
     * Get time until next token is available for login attempts
     * 
     * @param identifier The IP address or email to check
     * @return seconds until next token is available
     */
    public long getSecondsUntilNextLoginAttempt(String identifier) {
        Bucket bucket = getOrCreateBucket(loginBuckets, identifier, loginRequestsPerMinute, loginWindowMinutes);
        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }

    /**
     * Reset rate limiting for a specific identifier (admin function)
     * 
     * @param identifier The identifier to reset
     * @param limitType The type of limit to reset (LOGIN, REGISTRATION, etc.)
     */
    public void resetRateLimit(String identifier, String limitType) {
        log.info("Resetting rate limit for identifier: {} type: {}", identifier, limitType);
        
        switch (limitType.toUpperCase()) {
            case "LOGIN" -> loginBuckets.remove(identifier);
            case "REGISTRATION" -> registrationBuckets.remove(identifier);
            case "PASSWORD_RESET" -> passwordResetBuckets.remove(identifier);
            case "EMAIL_VERIFICATION" -> emailVerificationBuckets.remove(identifier);
            case "ALL" -> {
                loginBuckets.remove(identifier);
                registrationBuckets.remove(identifier);
                passwordResetBuckets.remove(identifier);
                emailVerificationBuckets.remove(identifier);
            }
            default -> log.warn("Unknown rate limit type: {}", limitType);
        }
    }

    /**
     * Generic method to consume a token from the appropriate bucket
     */
    private boolean consumeToken(ConcurrentHashMap<String, Bucket> bucketMap, String identifier, 
                                int requests, int windowMinutes, String operation) {
        Bucket bucket = getOrCreateBucket(bucketMap, identifier, requests, windowMinutes);
        boolean allowed = bucket.tryConsume(1);
        Optional.of(allowed)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                        a -> log.debug("Rate limit check passed for {} operation. Identifier: {}, Remaining: {}",
                                operation, identifier, bucket.getAvailableTokens()),
                        () -> log.warn("Rate limit exceeded for {} operation. Identifier: {}", operation, identifier)
                );

        return allowed;
    }

    /**
     * Get or create a bucket for the given identifier with specified limits
     */
    private Bucket getOrCreateBucket(ConcurrentHashMap<String, Bucket> bucketMap, String identifier, 
                                   int requests, int windowMinutes) {
        return bucketMap.computeIfAbsent(identifier, k -> createBucket(requests, windowMinutes));
    }

    /**
     * Create a new bucket with the specified rate limits
     */
    private Bucket createBucket(int requests, int windowMinutes) {
        Bandwidth limit = Bandwidth.classic(requests, Refill.intervally(requests, Duration.ofMinutes(windowMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Get rate limiting statistics for monitoring
     */
    public RateLimitStats getStats() {
        return RateLimitStats.builder()
                .loginBucketsActive(loginBuckets.size())
                .registrationBucketsActive(registrationBuckets.size())
                .passwordResetBucketsActive(passwordResetBuckets.size())
                .emailVerificationBucketsActive(emailVerificationBuckets.size())
                .loginRequestsPerMinute(loginRequestsPerMinute)
                .registrationRequestsPerHour(registrationRequestsPerHour)
                .passwordResetRequestsPerHour(passwordResetRequestsPerHour)
                .emailVerificationRequestsPerHour(emailVerificationRequestsPerHour)
                .build();
    }

    /**
     * Statistics data class for monitoring
     */
    public static class RateLimitStats {
        public final int loginBucketsActive;
        public final int registrationBucketsActive;
        public final int passwordResetBucketsActive;
        public final int emailVerificationBucketsActive;
        public final int loginRequestsPerMinute;
        public final int registrationRequestsPerHour;
        public final int passwordResetRequestsPerHour;
        public final int emailVerificationRequestsPerHour;

        private RateLimitStats(Builder builder) {
            this.loginBucketsActive = builder.loginBucketsActive;
            this.registrationBucketsActive = builder.registrationBucketsActive;
            this.passwordResetBucketsActive = builder.passwordResetBucketsActive;
            this.emailVerificationBucketsActive = builder.emailVerificationBucketsActive;
            this.loginRequestsPerMinute = builder.loginRequestsPerMinute;
            this.registrationRequestsPerHour = builder.registrationRequestsPerHour;
            this.passwordResetRequestsPerHour = builder.passwordResetRequestsPerHour;
            this.emailVerificationRequestsPerHour = builder.emailVerificationRequestsPerHour;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int loginBucketsActive;
            private int registrationBucketsActive;
            private int passwordResetBucketsActive;
            private int emailVerificationBucketsActive;
            private int loginRequestsPerMinute;
            private int registrationRequestsPerHour;
            private int passwordResetRequestsPerHour;
            private int emailVerificationRequestsPerHour;

            public Builder loginBucketsActive(int loginBucketsActive) {
                this.loginBucketsActive = loginBucketsActive;
                return this;
            }

            public Builder registrationBucketsActive(int registrationBucketsActive) {
                this.registrationBucketsActive = registrationBucketsActive;
                return this;
            }

            public Builder passwordResetBucketsActive(int passwordResetBucketsActive) {
                this.passwordResetBucketsActive = passwordResetBucketsActive;
                return this;
            }

            public Builder emailVerificationBucketsActive(int emailVerificationBucketsActive) {
                this.emailVerificationBucketsActive = emailVerificationBucketsActive;
                return this;
            }

            public Builder loginRequestsPerMinute(int loginRequestsPerMinute) {
                this.loginRequestsPerMinute = loginRequestsPerMinute;
                return this;
            }

            public Builder registrationRequestsPerHour(int registrationRequestsPerHour) {
                this.registrationRequestsPerHour = registrationRequestsPerHour;
                return this;
            }

            public Builder passwordResetRequestsPerHour(int passwordResetRequestsPerHour) {
                this.passwordResetRequestsPerHour = passwordResetRequestsPerHour;
                return this;
            }

            public Builder emailVerificationRequestsPerHour(int emailVerificationRequestsPerHour) {
                this.emailVerificationRequestsPerHour = emailVerificationRequestsPerHour;
                return this;
            }

            public RateLimitStats build() {
                return new RateLimitStats(this);
            }
        }
    }
}