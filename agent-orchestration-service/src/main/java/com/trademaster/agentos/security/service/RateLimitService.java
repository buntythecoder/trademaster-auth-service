package com.trademaster.agentos.security.service;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limit Service - Implements rate limiting and DDoS protection.
 * Uses token bucket algorithm for flexible rate limiting.
 */
@Slf4j
@Service
public class RateLimitService {
    
    private final Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final int userRateLimit;
    private final int ipRateLimit;
    private final int burstSize;
    private final Duration refillInterval;
    
    public RateLimitService(
            @Value("${security.ratelimit.user:100}") int userRateLimit,
            @Value("${security.ratelimit.ip:500}") int ipRateLimit,
            @Value("${security.ratelimit.burst:20}") int burstSize,
            @Value("${security.ratelimit.refill-interval:60}") int refillIntervalSeconds) {
        
        this.userRateLimit = userRateLimit;
        this.ipRateLimit = ipRateLimit;
        this.burstSize = burstSize;
        this.refillInterval = Duration.ofSeconds(refillIntervalSeconds);
        
        // Schedule periodic cleanup of inactive buckets
        scheduler.scheduleAtFixedRate(this::cleanupInactiveBuckets, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Check rate limit for security context.
     */
    public Result<SecurityContext, SecurityError> checkLimit(SecurityContext context) {
        String userId = context.userId();
        String ipAddress = context.ipAddress();
        
        // Check user rate limit
        if (userId != null && !userId.isBlank()) {
            TokenBucket userBucket = userBuckets.computeIfAbsent(userId, 
                k -> new TokenBucket(userRateLimit, burstSize, refillInterval));
            
            if (!userBucket.tryConsume(1)) {
                log.warn("User rate limit exceeded: userId={}", userId);
                return Result.failure(SecurityError.rateLimitExceeded(userId, context.correlationId()));
            }
        }
        
        // Check IP rate limit
        if (ipAddress != null && !ipAddress.isBlank()) {
            TokenBucket ipBucket = ipBuckets.computeIfAbsent(ipAddress, 
                k -> new TokenBucket(ipRateLimit, burstSize, refillInterval));
            
            if (!ipBucket.tryConsume(1)) {
                log.warn("IP rate limit exceeded: ip={}", ipAddress);
                return Result.failure(SecurityError.rateLimitExceeded(ipAddress, context.correlationId()));
            }
        }
        
        return Result.success(context);
    }
    
    /**
     * Check if specific user is rate limited.
     */
    public boolean isUserRateLimited(String userId) {
        TokenBucket bucket = userBuckets.get(userId);
        return bucket != null && bucket.getAvailableTokens() <= 0;
    }
    
    /**
     * Check if specific IP is rate limited.
     */
    public boolean isIPRateLimited(String ipAddress) {
        TokenBucket bucket = ipBuckets.get(ipAddress);
        return bucket != null && bucket.getAvailableTokens() <= 0;
    }
    
    /**
     * Reset rate limit for user.
     */
    public void resetUserLimit(String userId) {
        TokenBucket bucket = userBuckets.get(userId);
        if (bucket != null) {
            bucket.reset();
            log.info("Reset rate limit for user: {}", userId);
        }
    }
    
    /**
     * Reset rate limit for IP.
     */
    public void resetIPLimit(String ipAddress) {
        TokenBucket bucket = ipBuckets.get(ipAddress);
        if (bucket != null) {
            bucket.reset();
            log.info("Reset rate limit for IP: {}", ipAddress);
        }
    }
    
    /**
     * Get current rate limit status for user.
     */
    public RateLimitStatus getUserStatus(String userId) {
        TokenBucket bucket = userBuckets.get(userId);
        if (bucket == null) {
            return new RateLimitStatus(userRateLimit, userRateLimit, 0);
        }
        return new RateLimitStatus(
            bucket.getAvailableTokens(),
            bucket.capacity,
            bucket.getSecondsUntilRefill()
        );
    }
    
    /**
     * Apply custom rate limit for specific user.
     */
    public void setCustomUserLimit(String userId, int limit, int burst) {
        userBuckets.put(userId, new TokenBucket(limit, burst, refillInterval));
        log.info("Set custom rate limit for user {}: limit={}, burst={}", userId, limit, burst);
    }
    
    // Private helper methods
    
    private void cleanupInactiveBuckets() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(30));
        
        // Clean up user buckets
        userBuckets.entrySet().removeIf(entry -> 
            entry.getValue().lastAccess.isBefore(cutoff));
        
        // Clean up IP buckets
        ipBuckets.entrySet().removeIf(entry -> 
            entry.getValue().lastAccess.isBefore(cutoff));
        
        log.debug("Cleaned up inactive rate limit buckets");
    }
    
    /**
     * Token bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final Duration refillInterval;
        private final AtomicInteger tokens;
        private volatile Instant lastRefill;
        private volatile Instant lastAccess;
        
        TokenBucket(int capacity, int burstSize, Duration refillInterval) {
            this.capacity = capacity;
            this.refillTokens = capacity;
            this.refillInterval = refillInterval;
            this.tokens = new AtomicInteger(burstSize);
            this.lastRefill = Instant.now();
            this.lastAccess = Instant.now();
        }
        
        synchronized boolean tryConsume(int count) {
            lastAccess = Instant.now();
            refill();
            
            if (tokens.get() >= count) {
                tokens.addAndGet(-count);
                return true;
            }
            return false;
        }
        
        private void refill() {
            Instant now = Instant.now();
            Duration elapsed = Duration.between(lastRefill, now);
            
            if (elapsed.compareTo(refillInterval) >= 0) {
                int tokensToAdd = (int) (elapsed.toMillis() / refillInterval.toMillis()) * refillTokens;
                int newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
                tokens.set(newTokens);
                lastRefill = now;
            }
        }
        
        int getAvailableTokens() {
            refill();
            return tokens.get();
        }
        
        long getSecondsUntilRefill() {
            Duration elapsed = Duration.between(lastRefill, Instant.now());
            Duration remaining = refillInterval.minus(elapsed);
            return remaining.isNegative() ? 0 : remaining.getSeconds();
        }
        
        void reset() {
            tokens.set(capacity);
            lastRefill = Instant.now();
            lastAccess = Instant.now();
        }
    }
    
    /**
     * Rate limit status information.
     */
    public record RateLimitStatus(
        int availableTokens,
        int capacity,
        long secondsUntilRefill
    ) {}
}