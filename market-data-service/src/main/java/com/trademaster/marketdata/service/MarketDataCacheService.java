package com.trademaster.marketdata.service;

import com.trademaster.marketdata.config.RedisConfig;
import com.trademaster.marketdata.entity.MarketDataPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Market Data Caching Service with <5ms performance
 * 
 * Features:
 * - Sub-5ms response times for cached data
 * - Intelligent cache warming strategies
 * - Pipeline operations for batch updates
 * - Cache hit rate monitoring and optimization
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisConfig.MarketDataCacheConfig cacheConfig;
    private final RedisConfig.RedisKeyPatterns keyPatterns;

    // Performance monitoring
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheWrites = new AtomicLong(0);
    private final Map<String, Long> responseTimeTracker = new ConcurrentHashMap<>();

    /**
     * Get current price from cache (target: <5ms)
     */
    public Optional<CachedPrice> getCurrentPrice(String symbol, String exchange) {
        long startTime = System.nanoTime();
        
        try {
            String key = keyPatterns.priceKey(symbol, exchange);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                cacheHits.incrementAndGet();
                logResponseTime("getCurrentPrice", startTime);
                return Optional.of((CachedPrice) cached);
            } else {
                cacheMisses.incrementAndGet();
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Cache get failed for price {}:{}: {}", symbol, exchange, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache current price with TTL
     */
    public CompletableFuture<Boolean> cacheCurrentPrice(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = keyPatterns.priceKey(dataPoint.symbol(), dataPoint.exchange());
                
                CachedPrice cachedPrice = new CachedPrice(
                    dataPoint.symbol(),
                    dataPoint.exchange(),
                    dataPoint.price(),
                    dataPoint.volume(),
                    dataPoint.change(),
                    dataPoint.changePercent(),
                    dataPoint.timestamp(),
                    Instant.now()
                );
                
                redisTemplate.opsForValue().set(key, cachedPrice, cacheConfig.priceDataTtl());
                cacheWrites.incrementAndGet();
                
                log.trace("Cached price for {}:{} = {}", 
                    dataPoint.symbol(), dataPoint.exchange(), dataPoint.price());
                
                return true;
                
            } catch (Exception e) {
                log.error("Cache write failed for {}:{}: {}", 
                    dataPoint.symbol(), dataPoint.exchange(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * Batch cache multiple prices using pipeline (high performance)
     */
    public CompletableFuture<BatchCacheResult> batchCachePrices(List<MarketDataPoint> dataPoints) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                var cacheTask = scope.fork(() -> {
                    // Use Redis pipeline for batch operations
                    redisTemplate.executePipelined(connection -> {
                        for (MarketDataPoint point : dataPoints) {
                            if (point.isValid()) {
                                String key = keyPatterns.priceKey(point.symbol(), point.exchange());
                                CachedPrice cachedPrice = new CachedPrice(
                                    point.symbol(), point.exchange(), point.price(),
                                    point.volume(), point.change(), point.changePercent(),
                                    point.timestamp(), Instant.now()
                                );
                                
                                connection.setEx(
                                    key.getBytes(),
                                    cacheConfig.priceDataTtl().getSeconds(),
                                    serialize(cachedPrice)
                                );
                            }
                        }
                        return null;
                    });
                    
                    return dataPoints.size();
                });
                
                scope.join();
                scope.throwIfFailed();
                
                int cached = cacheTask.get();
                cacheWrites.addAndGet(cached);
                
                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                log.info("Batch cached {} prices in {}ms", cached, duration);
                
                return new BatchCacheResult(cached, 0, duration);
                
            } catch (Exception e) {
                log.error("Batch cache failed: {}", e.getMessage());
                return new BatchCacheResult(0, dataPoints.size(), 0);
            }
        });
    }

    /**
     * Get OHLC data from cache
     */
    public Optional<List<CachedOHLC>> getOHLCData(String symbol, String exchange, String interval) {
        try {
            String key = keyPatterns.ohlcKey(symbol, exchange, interval);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                cacheHits.incrementAndGet();
                @SuppressWarnings("unchecked")
                List<CachedOHLC> result = (List<CachedOHLC>) cached;
                return Optional.of(result);
            } else {
                cacheMisses.incrementAndGet();
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Cache get failed for OHLC {}:{}:{}: {}", 
                symbol, exchange, interval, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache OHLC data with appropriate TTL
     */
    public CompletableFuture<Boolean> cacheOHLCData(String symbol, String exchange, 
            String interval, List<MarketDataPoint> ohlcData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = keyPatterns.ohlcKey(symbol, exchange, interval);
                
                List<CachedOHLC> cachedData = ohlcData.stream()
                    .map(point -> new CachedOHLC(
                        point.symbol(), point.exchange(),
                        point.open(), point.high(), point.low(), point.price(),
                        point.volume(), point.timestamp()
                    ))
                    .toList();
                
                redisTemplate.opsForValue().set(key, cachedData, cacheConfig.ohlcDataTtl());
                cacheWrites.incrementAndGet();
                
                return true;
                
            } catch (Exception e) {
                log.error("Cache write failed for OHLC {}:{}:{}: {}", 
                    symbol, exchange, interval, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get order book data from cache
     */
    public Optional<CachedOrderBook> getOrderBook(String symbol, String exchange) {
        long startTime = System.nanoTime();
        
        try {
            String key = keyPatterns.orderBookKey(symbol, exchange);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                cacheHits.incrementAndGet();
                logResponseTime("getOrderBook", startTime);
                return Optional.of((CachedOrderBook) cached);
            } else {
                cacheMisses.incrementAndGet();
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Cache get failed for order book {}:{}: {}", 
                symbol, exchange, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache order book data (very short TTL for real-time data)
     */
    public CompletableFuture<Boolean> cacheOrderBook(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() -> {
            if (!dataPoint.hasOrderBookData()) {
                return false;
            }
            
            try {
                String key = keyPatterns.orderBookKey(dataPoint.symbol(), dataPoint.exchange());
                
                CachedOrderBook orderBook = new CachedOrderBook(
                    dataPoint.symbol(), dataPoint.exchange(),
                    dataPoint.bid(), dataPoint.ask(),
                    dataPoint.bidSize(), dataPoint.askSize(),
                    dataPoint.getSpread(), dataPoint.getSpreadPercentage(),
                    dataPoint.timestamp(), Instant.now()
                );
                
                redisTemplate.opsForValue().set(key, orderBook, cacheConfig.orderBookTtl());
                cacheWrites.incrementAndGet();
                
                return true;
                
            } catch (Exception e) {
                log.error("Cache write failed for order book {}:{}: {}", 
                    dataPoint.symbol(), dataPoint.exchange(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * Cache warming for active symbols
     */
    public CompletableFuture<CacheWarmingResult> warmCache(List<String> symbols, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                var warmingTask = scope.fork(() -> {
                    int warmed = 0;
                    
                    // Cache symbol list
                    String symbolListKey = keyPatterns.symbolListKey(exchange);
                    redisTemplate.opsForValue().set(symbolListKey, symbols, cacheConfig.symbolListTtl());
                    warmed++;
                    
                    // Pre-warm frequently accessed data structures
                    for (String symbol : symbols) {
                        try {
                            // Initialize price cache entry
                            String priceKey = keyPatterns.priceKey(symbol, exchange);
                            if (!redisTemplate.hasKey(priceKey)) {
                                // Set placeholder to reserve key space
                                redisTemplate.opsForValue().set(priceKey + ":placeholder", "warming", 
                                    cacheConfig.priceDataTtl());
                                warmed++;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to warm cache for symbol {}: {}", symbol, e.getMessage());
                        }
                    }
                    
                    return warmed;
                });
                
                scope.join();
                scope.throwIfFailed();
                
                int warmed = warmingTask.get();
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("Cache warming completed: {} entries in {}ms", warmed, duration);
                return new CacheWarmingResult(warmed, duration, true);
                
            } catch (Exception e) {
                log.error("Cache warming failed: {}", e.getMessage());
                return new CacheWarmingResult(0, 0, false);
            }
        });
    }

    /**
     * Get cache performance metrics
     */
    public CacheMetrics getMetrics() {
        long totalRequests = cacheHits.get() + cacheMisses.get();
        double hitRate = totalRequests > 0 ? 
            (double) cacheHits.get() / totalRequests * 100 : 0.0;
        
        double avgResponseTime = responseTimeTracker.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        return new CacheMetrics(
            cacheHits.get(),
            cacheMisses.get(),
            cacheWrites.get(),
            hitRate,
            avgResponseTime,
            Instant.now()
        );
    }

    /**
     * Clear cache for symbol
     */
    public CompletableFuture<Boolean> clearSymbolCache(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> keysToDelete = List.of(
                    keyPatterns.priceKey(symbol, exchange),
                    keyPatterns.tickKey(symbol, exchange),
                    keyPatterns.orderBookKey(symbol, exchange)
                );
                
                Long deleted = redisTemplate.delete(keysToDelete);
                log.info("Cleared {} cache entries for {}:{}", deleted, symbol, exchange);
                
                return deleted != null && deleted > 0;
                
            } catch (Exception e) {
                log.error("Failed to clear cache for {}:{}: {}", symbol, exchange, e.getMessage());
                return false;
            }
        });
    }

    // Helper methods
    private void logResponseTime(String operation, long startTimeNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        responseTimeTracker.put(operation, durationMs);
        
        if (durationMs > 5) { // Log if exceeding 5ms target
            log.warn("{} took {}ms (exceeds 5ms target)", operation, durationMs);
        }
    }
    
    private byte[] serialize(Object obj) {
        // Simplified serialization - in production, use proper serialization
        return obj.toString().getBytes();
    }

    // Data classes
    public record CachedPrice(
        String symbol, String exchange, BigDecimal price, Long volume,
        BigDecimal change, BigDecimal changePercent, Instant marketTime, Instant cachedAt
    ) {}

    public record CachedOHLC(
        String symbol, String exchange, BigDecimal open, BigDecimal high, 
        BigDecimal low, BigDecimal close, Long volume, Instant timestamp
    ) {}

    public record CachedOrderBook(
        String symbol, String exchange, BigDecimal bid, BigDecimal ask,
        Long bidSize, Long askSize, BigDecimal spread, BigDecimal spreadPercent,
        Instant marketTime, Instant cachedAt
    ) {}

    public record BatchCacheResult(
        int successful, int failed, long durationMs
    ) {}

    public record CacheWarmingResult(
        int entriesWarmed, long durationMs, boolean success
    ) {}

    public record CacheMetrics(
        long cacheHits, long cacheMisses, long cacheWrites, 
        double hitRate, double avgResponseTimeMs, Instant generatedAt
    ) {
        public boolean isPerformanceTarget() {
            return avgResponseTimeMs < 5.0 && hitRate > 85.0;
        }
    }
}