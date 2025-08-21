package com.trademaster.notification.service;

import com.trademaster.notification.model.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Service for Notifications
 * 
 * Implements rate limiting using sliding window approach to prevent notification spam
 * and comply with external service rate limits.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class RateLimitService {
    
    // Rate limits per notification type (per minute)
    private static final int EMAIL_RATE_LIMIT = 60;
    private static final int SMS_RATE_LIMIT = 10; 
    private static final int PUSH_RATE_LIMIT = 100;
    private static final int IN_APP_RATE_LIMIT = 200;
    
    // Sliding window counters
    private final ConcurrentHashMap<String, AtomicInteger> windowCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> windowStarts = new ConcurrentHashMap<>();
    
    /**
     * Check if a notification request is allowed under current rate limits
     */
    public boolean isAllowed(NotificationRequest request) {
        String rateLimitKey = getRateLimitKey(request);
        int limit = getRateLimitForType(request.getType());
        
        return checkRateLimit(rateLimitKey, limit);
    }
    
    /**
     * Record a notification attempt (regardless of success/failure)
     */
    public void recordAttempt(NotificationRequest request) {
        String rateLimitKey = getRateLimitKey(request);
        incrementCounter(rateLimitKey);
    }
    
    private String getRateLimitKey(NotificationRequest request) {
        // Rate limit by type and recipient to prevent spam to specific users
        return request.getType().name() + ":" + request.getRecipient();
    }
    
    private int getRateLimitForType(NotificationRequest.NotificationType type) {
        return switch (type) {
            case EMAIL -> EMAIL_RATE_LIMIT;
            case SMS -> SMS_RATE_LIMIT;
            case PUSH -> PUSH_RATE_LIMIT;
            case IN_APP -> IN_APP_RATE_LIMIT;
        };
    }
    
    private boolean checkRateLimit(String key, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = windowStarts.get(key);
        
        // Initialize or reset window if more than a minute has passed
        if (windowStart == null || windowStart.isBefore(now.minusMinutes(1))) {
            windowStarts.put(key, now);
            windowCounts.put(key, new AtomicInteger(0));
            return true;
        }
        
        // Check current count against limit
        AtomicInteger currentCount = windowCounts.get(key);
        int count = currentCount != null ? currentCount.get() : 0;
        
        if (count >= limit) {
            log.warn("Rate limit exceeded for key: {} (current: {}, limit: {})", key, count, limit);
            return false;
        }
        
        return true;
    }
    
    private void incrementCounter(String key) {
        windowCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Clean up old rate limit entries (should be called periodically)
     */
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        
        windowStarts.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Remove counters for cleaned up windows
        windowStarts.keySet().forEach(key -> {
            if (!windowStarts.containsKey(key)) {
                windowCounts.remove(key);
            }
        });
        
        log.debug("Cleaned up rate limit entries older than 2 minutes");
    }
}