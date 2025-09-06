package com.trademaster.agentos.decorator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ FUNCTIONAL: Caching Decorator
 * 
 * Decorates agent operations with intelligent caching to improve performance.
 * Implements cache-aside pattern with TTL-based invalidation and LRU eviction.
 * 
 * Features:
 * - Automatic cache management
 * - TTL-based cache invalidation
 * - LRU eviction policy
 * - Cache hit/miss analytics
 */
@Component
@RequiredArgsConstructor
public class CachingDecorator implements AgentServiceDecorator {
    
    private final StructuredLoggingService structuredLogger;
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final long DEFAULT_TTL_MINUTES = 5;
    private static final int MAX_CACHE_SIZE = 1000;
    
    @Override
    public <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
            java.util.function.Function<Agent, Result<T, AgentError>> operation) {
        
        return agent -> {
            String cacheKey = generateCacheKey(agent, operation);
            
            // Try to get from cache first
            java.util.Optional<Result<T, AgentError>> cached = getCachedResult(cacheKey, agent);
            if (cached.isPresent()) {
                return cached.get();
            }
            
            // Cache miss - execute operation and cache result
            Result<T, AgentError> result = operation.apply(agent);
            
            result.onSuccess((T value) -> cacheResult(cacheKey, value, agent))
                  .onFailure((AgentError error) -> logCacheMiss(agent, cacheKey, error.getMessage()));
            
            return result;
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Generate cache key for operation
     */
    private <T> String generateCacheKey(Agent agent, 
                                        java.util.function.Function<Agent, Result<T, AgentError>> operation) {
        return String.format("agent_%d_op_%s", 
            agent.getAgentId(), 
            operation.getClass().getSimpleName());
    }
    
    /**
     * ✅ FUNCTIONAL: Get cached result if available and valid
     */
    @SuppressWarnings("unchecked")
    private <T> java.util.Optional<Result<T, AgentError>> getCachedResult(String cacheKey, Agent agent) {
        CacheEntry<?> entry = cache.get(cacheKey);
        
        if (entry == null) {
            logCacheMiss(agent, cacheKey, "Cache entry not found");
            return java.util.Optional.empty();
        }
        
        if (entry.isExpired()) {
            cache.remove(cacheKey);
            logCacheMiss(agent, cacheKey, "Cache entry expired");
            return java.util.Optional.empty();
        }
        
        // Update last accessed time for LRU
        entry.updateLastAccessed();
        
        logCacheHit(agent, cacheKey);
        return java.util.Optional.of((Result<T, AgentError>) entry.getValue());
    }
    
    /**
     * ✅ FUNCTIONAL: Cache successful operation result
     */
    private <T> void cacheResult(String cacheKey, T value, Agent agent) {
        // Ensure cache doesn't exceed maximum size
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictLeastRecentlyUsed();
        }
        
        Result<T, AgentError> result = Result.success(value);
        CacheEntry<Result<T, AgentError>> entry = new CacheEntry<>(result, DEFAULT_TTL_MINUTES);
        cache.put(cacheKey, entry);
        
        structuredLogger.logDebug("cache_entry_stored", 
            Map.of("agentId", agent.getAgentId(),
                   "cacheKey", cacheKey,
                   "ttlMinutes", DEFAULT_TTL_MINUTES,
                   "cacheSize", cache.size()));
    }
    
    /**
     * ✅ FUNCTIONAL: Evict least recently used cache entries
     */
    private void evictLeastRecentlyUsed() {
        cache.entrySet().stream()
            .min(Map.Entry.<String, CacheEntry<?>>comparingByValue(
                (a, b) -> a.getLastAccessed().compareTo(b.getLastAccessed())))
            .ifPresent(entry -> {
                cache.remove(entry.getKey());
                structuredLogger.logDebug("cache_entry_evicted_lru", 
                    Map.of("cacheKey", entry.getKey(),
                           "lastAccessed", entry.getValue().getLastAccessed()));
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Log cache hit event
     */
    private void logCacheHit(Agent agent, String cacheKey) {
        structuredLogger.logDebug("cache_hit", 
            Map.of("agentId", agent.getAgentId(),
                   "cacheKey", cacheKey,
                   "cacheSize", cache.size()));
    }
    
    /**
     * ✅ FUNCTIONAL: Log cache miss event
     */
    private void logCacheMiss(Agent agent, String cacheKey, Object reason) {
        structuredLogger.logDebug("cache_miss", 
            Map.of("agentId", agent.getAgentId(),
                   "cacheKey", cacheKey,
                   "reason", reason.toString(),
                   "cacheSize", cache.size()));
    }
    
    /**
     * ✅ FUNCTIONAL: Create caching decorator instance
     */
    public static AgentServiceDecorator create(StructuredLoggingService logger) {
        return new CachingDecorator(logger);
    }
    
    /**
     * ✅ FUNCTIONAL: Cache entry with TTL and LRU tracking
     */
    private static class CacheEntry<T> {
        private final T value;
        private final Instant expirationTime;
        private Instant lastAccessed;
        
        public CacheEntry(T value, long ttlMinutes) {
            this.value = value;
            this.lastAccessed = Instant.now();
            this.expirationTime = this.lastAccessed.plus(ttlMinutes, ChronoUnit.MINUTES);
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expirationTime);
        }
        
        public void updateLastAccessed() {
            this.lastAccessed = Instant.now();
        }
        
        public Instant getLastAccessed() {
            return lastAccessed;
        }
    }
}