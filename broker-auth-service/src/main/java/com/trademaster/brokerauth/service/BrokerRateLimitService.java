package com.trademaster.brokerauth.service;

import com.trademaster.brokerauth.entity.Broker;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.repository.BrokerRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Broker Rate Limiting Service
 * 
 * Implements rate limiting for broker API calls according to each broker's limits.
 * Uses Redis for distributed rate limiting and circuit breakers for protection.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerRateLimitService {
    
    private final BrokerRepository brokerRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry meterRegistry;
    
    // Cache for rate limiters per broker
    private final Map<BrokerType, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    // Metrics
    private final Counter rateLimitExceededCounter;
    private final Counter rateLimitCheckCounter;
    
    public BrokerRateLimitService(BrokerRepository brokerRepository,
                                 RedisTemplate<String, String> redisTemplate,
                                 RateLimiterRegistry rateLimiterRegistry,
                                 MeterRegistry meterRegistry) {
        this.brokerRepository = brokerRepository;
        this.redisTemplate = redisTemplate;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.rateLimitExceededCounter = Counter.builder("broker.rate.limit.exceeded")
                .description("Number of rate limit violations")
                .register(meterRegistry);
                
        this.rateLimitCheckCounter = Counter.builder("broker.rate.limit.check")
                .description("Number of rate limit checks performed")
                .register(meterRegistry);
    }
    
    /**
     * Check if API call is within rate limits
     */
    public CompletableFuture<RateLimitResult> checkRateLimit(BrokerType brokerType, Long userId, String operation) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimitCheckCounter.increment();
            
            try {
                // Get or create rate limiter for broker
                RateLimiter rateLimiter = getRateLimiter(brokerType);
                
                // Check global broker rate limit
                boolean globalAllowed = rateLimiter.acquirePermission();
                if (!globalAllowed) {
                    rateLimitExceededCounter.increment();
                    return RateLimitResult.denied("Global rate limit exceeded for broker " + brokerType);
                }
                
                // Check user-specific rate limit using Redis
                String userKey = String.format("rate_limit:user:%d:broker:%s", userId, brokerType.getCode());
                boolean userAllowed = checkRedisRateLimit(userKey, getUserRateLimit(brokerType), Duration.ofMinutes(1));
                
                if (!userAllowed) {
                    rateLimitExceededCounter.increment();
                    return RateLimitResult.denied("User rate limit exceeded for broker " + brokerType);
                }
                
                // Check operation-specific rate limit if specified
                if (operation != null && !operation.isEmpty()) {
                    String operationKey = String.format("rate_limit:operation:%s:broker:%s", operation, brokerType.getCode());
                    boolean operationAllowed = checkRedisRateLimit(operationKey, getOperationRateLimit(operation), Duration.ofMinutes(1));
                    
                    if (!operationAllowed) {
                        rateLimitExceededCounter.increment();
                        return RateLimitResult.denied("Operation rate limit exceeded for " + operation);
                    }
                }
                
                log.debug("Rate limit check passed for user {} and broker {}", userId, brokerType);
                return RateLimitResult.allowed();
                
            } catch (Exception e) {
                log.error("Error checking rate limits for user {} and broker {}", userId, brokerType, e);
                // Fail open - allow request if rate limiting fails
                return RateLimitResult.allowed();
            }
        });
    }
    
    /**
     * Record API call for rate limiting
     */
    public CompletableFuture<Void> recordApiCall(BrokerType brokerType, Long userId, String operation) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Record user call
                String userKey = String.format("rate_limit:user:%d:broker:%s", userId, brokerType.getCode());
                incrementRedisCounter(userKey, Duration.ofMinutes(1));
                
                // Record operation call if specified
                if (operation != null && !operation.isEmpty()) {
                    String operationKey = String.format("rate_limit:operation:%s:broker:%s", operation, brokerType.getCode());
                    incrementRedisCounter(operationKey, Duration.ofMinutes(1));
                }
                
                // Record broker-level metrics
                meterRegistry.counter("broker.api.calls", "broker", brokerType.getCode()).increment();
                
                log.debug("Recorded API call for user {} and broker {}", userId, brokerType);
                
            } catch (Exception e) {
                log.error("Error recording API call for user {} and broker {}", userId, brokerType, e);
            }
        });
    }
    
    /**
     * Get current usage stats for user and broker
     */
    public CompletableFuture<UsageStats> getUsageStats(BrokerType brokerType, Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userKey = String.format("rate_limit:user:%d:broker:%s", userId, brokerType.getCode());
                String countStr = redisTemplate.opsForValue().get(userKey);
                
                long currentCount = countStr != null ? Long.parseLong(countStr) : 0;
                long limit = getUserRateLimit(brokerType);
                long remaining = Math.max(0, limit - currentCount);
                
                return UsageStats.builder()
                        .brokerType(brokerType)
                        .userId(userId)
                        .currentUsage(currentCount)
                        .limit(limit)
                        .remaining(remaining)
                        .windowMinutes(1)
                        .build();
                
            } catch (Exception e) {
                log.error("Error getting usage stats for user {} and broker {}", userId, brokerType, e);
                return UsageStats.builder()
                        .brokerType(brokerType)
                        .userId(userId)
                        .currentUsage(0)
                        .limit(getUserRateLimit(brokerType))
                        .remaining(getUserRateLimit(brokerType))
                        .windowMinutes(1)
                        .build();
            }
        });
    }
    
    /**
     * Reset rate limits for user (admin function)
     */
    public CompletableFuture<Boolean> resetUserLimits(BrokerType brokerType, Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userKey = String.format("rate_limit:user:%d:broker:%s", userId, brokerType.getCode());
                redisTemplate.delete(userKey);
                
                log.info("Reset rate limits for user {} and broker {}", userId, brokerType);
                return true;
                
            } catch (Exception e) {
                log.error("Error resetting rate limits for user {} and broker {}", userId, brokerType, e);
                return false;
            }
        });
    }
    
    /**
     * Get or create rate limiter for broker
     */
    private RateLimiter getRateLimiter(BrokerType brokerType) {
        return rateLimiters.computeIfAbsent(brokerType, this::createRateLimiter);
    }
    
    /**
     * Create rate limiter for broker based on its configuration
     */
    private RateLimiter createRateLimiter(BrokerType brokerType) {
        Broker broker = brokerRepository.findByBrokerType(brokerType)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerType));
        
        int limitPerSecond = broker.getRateLimitPerSecond() != null ? broker.getRateLimitPerSecond() : 10;
        
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(limitPerSecond)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        String rateLimiterName = "broker-" + brokerType.getCode();
        return rateLimiterRegistry.rateLimiter(rateLimiterName, config);
    }
    
    /**
     * Check rate limit using Redis sliding window
     */
    private boolean checkRedisRateLimit(String key, long limit, Duration window) {
        try {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - window.toMillis();
            
            // Use Redis sorted set for sliding window
            String windowKey = key + ":window";
            
            // Remove old entries
            redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart);
            
            // Count current entries in window
            Long currentCount = redisTemplate.opsForZSet().count(windowKey, windowStart, currentTime);
            
            if (currentCount >= limit) {
                return false; // Rate limit exceeded
            }
            
            // Add current request
            redisTemplate.opsForZSet().add(windowKey, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(windowKey, window);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error checking Redis rate limit for key {}", key, e);
            return true; // Fail open
        }
    }
    
    /**
     * Increment Redis counter with expiration
     */
    private void incrementRedisCounter(String key, Duration expiration) {
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, expiration);
        } catch (Exception e) {
            log.error("Error incrementing Redis counter for key {}", key, e);
        }
    }
    
    /**
     * Get user rate limit for broker
     */
    private long getUserRateLimit(BrokerType brokerType) {
        return switch (brokerType) {
            case ZERODHA -> 100; // 100 requests per minute per user
            case UPSTOX -> 150;  // 150 requests per minute per user
            case ANGEL_ONE -> 120; // 120 requests per minute per user
            case ICICI_DIRECT -> 60; // 60 requests per minute per user
        };
    }
    
    /**
     * Get operation-specific rate limit
     */
    private long getOperationRateLimit(String operation) {
        return switch (operation.toLowerCase()) {
            case "orders" -> 10; // Order placement more restrictive
            case "positions" -> 60;
            case "holdings" -> 30;
            case "margins" -> 20;
            default -> 100; // Default limit
        };
    }
    
    /**
     * Rate limit check result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RateLimitResult {
        private boolean allowed;
        private String reason;
        private long retryAfterSeconds;
        
        public static RateLimitResult allowed() {
            return RateLimitResult.builder().allowed(true).build();
        }
        
        public static RateLimitResult denied(String reason) {
            return RateLimitResult.builder()
                    .allowed(false)
                    .reason(reason)
                    .retryAfterSeconds(60) // Default retry after 1 minute
                    .build();
        }
        
        public static RateLimitResult denied(String reason, long retryAfterSeconds) {
            return RateLimitResult.builder()
                    .allowed(false)
                    .reason(reason)
                    .retryAfterSeconds(retryAfterSeconds)
                    .build();
        }
    }
    
    /**
     * Usage statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UsageStats {
        private BrokerType brokerType;
        private Long userId;
        private long currentUsage;
        private long limit;
        private long remaining;
        private int windowMinutes;
        
        /**
         * Calculate usage percentage
         */
        public double getUsagePercentage() {
            if (limit == 0) return 0.0;
            return (double) currentUsage / limit * 100.0;
        }
        
        /**
         * Check if close to limit
         */
        public boolean isNearLimit() {
            return getUsagePercentage() > 80.0;
        }
        
        /**
         * Check if at limit
         */
        public boolean isAtLimit() {
            return currentUsage >= limit;
        }
    }
}