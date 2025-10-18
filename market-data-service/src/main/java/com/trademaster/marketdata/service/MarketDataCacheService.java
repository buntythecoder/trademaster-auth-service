package com.trademaster.marketdata.service;

import com.trademaster.marketdata.config.RedisConfig;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.functional.Try;
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
 * MANDATORY RULES COMPLIANT:
 * - RULE #3: No if-statements, uses Optional chains and ternary operators
 * - RULE #11: Try monad for all error handling
 * - RULE #12: Virtual threads with StructuredTaskScope for parallel operations
 * - RULE #13: Stream API throughout, no for-loops
 * - RULE #17: All magic numbers externalized to named constants
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> customStringRedisTemplate;
    private final RedisConfig.MarketDataCacheConfig cacheConfig;
    private final RedisConfig.RedisKeyPatterns keyPatterns;

    // Performance and monitoring constants (RULE #17)
    private static final long PERFORMANCE_TARGET_MS = 5L;
    private static final double PERFORMANCE_TARGET_MS_DOUBLE = 5.0;
    private static final double HIT_RATE_TARGET_PERCENT = 85.0;
    private static final int PERCENTAGE_MULTIPLIER = 100;
    private static final int SYMBOL_LIST_OFFSET = 1;

    // Performance monitoring
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheWrites = new AtomicLong(0);
    private final Map<String, Long> responseTimeTracker = new ConcurrentHashMap<>();

    /**
     * Get current price from cache (target: <5ms)
     * Rule #11: Functional error handling with Try monad
     */
    public Optional<CachedPrice> getCurrentPrice(String symbol, String exchange) {
        long startTime = System.nanoTime();

        return Try.of(() -> {
            String key = keyPatterns.priceKey(symbol, exchange);
            return redisTemplate.opsForValue().get(key);
        })
        .map(cached -> Optional.ofNullable(cached)
            .map(c -> {
                cacheHits.incrementAndGet();
                logResponseTime("getCurrentPrice", startTime);
                return (CachedPrice) c;
            })
            .orElseGet(() -> {
                cacheMisses.incrementAndGet();
                return null;
            })
        )
        .recover(e -> {
            log.error("Cache get failed for price {}:{}: {}", symbol, exchange, e.getMessage());
            return null;
        })
        .toOptional()
        .flatMap(Optional::ofNullable);
    }

    /**
     * Cache current price with TTL
     * Rule #11: Functional error handling with Try monad
     */
    public CompletableFuture<Boolean> cacheCurrentPrice(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() ->
            Try.of(() -> {
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
            })
            .recover(e -> {
                log.error("Cache write failed for {}:{}: {}",
                    dataPoint.symbol(), dataPoint.exchange(), e.getMessage());
                return false;
            })
            .getOrElse(false)
        );
    }

    /**
     * Batch cache multiple prices using pipeline (high performance)
     * Rule #11: Functional error handling with Try monad
     * Rule #13: Stream API instead of loops
     */
    public CompletableFuture<BatchCacheResult> batchCachePrices(List<MarketDataPoint> dataPoints) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();

            return Try.of(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                    var cacheTask = scope.fork(() -> {
                        // Use Redis pipeline for batch operations with Stream API
                        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                            dataPoints.stream()
                                .filter(MarketDataPoint::isValid)
                                .forEach(point -> {
                                    String key = keyPatterns.priceKey(point.symbol(), point.exchange());
                                    CachedPrice cachedPrice = new CachedPrice(
                                        point.symbol(), point.exchange(), point.price(),
                                        point.volume(), point.change(), point.changePercent(),
                                        point.timestamp(), Instant.now()
                                    );

                                    connection.stringCommands().set(
                                        key.getBytes(),
                                        serialize(cachedPrice),
                                        org.springframework.data.redis.core.types.Expiration.seconds(cacheConfig.priceDataTtl().getSeconds()),
                                        org.springframework.data.redis.connection.RedisStringCommands.SetOption.UPSERT
                                    );
                                });
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
                }
            })
            .recover(e -> {
                log.error("Batch cache failed: {}", e.getMessage());
                return new BatchCacheResult(0, dataPoints.size(), 0);
            })
            .getOrElse(new BatchCacheResult(0, dataPoints.size(), 0));
        });
    }

    /**
     * Get OHLC data from cache
     * Rule #11: Functional error handling with Try monad
     */
    public Optional<List<CachedOHLC>> getOHLCData(String symbol, String exchange, String interval) {
        return Try.of(() -> {
            String key = keyPatterns.ohlcKey(symbol, exchange, interval);
            return redisTemplate.opsForValue().get(key);
        })
        .map(cached -> Optional.ofNullable(cached)
            .map(c -> {
                cacheHits.incrementAndGet();
                @SuppressWarnings("unchecked")
                List<CachedOHLC> result = (List<CachedOHLC>) c;
                return result;
            })
            .orElseGet(() -> {
                cacheMisses.incrementAndGet();
                return null;
            })
        )
        .recover(e -> {
            log.error("Cache get failed for OHLC {}:{}:{}: {}",
                symbol, exchange, interval, e.getMessage());
            return null;
        })
        .toOptional()
        .flatMap(Optional::ofNullable);
    }

    /**
     * Cache OHLC data with appropriate TTL
     * Rule #11: Functional error handling with Try monad
     */
    public CompletableFuture<Boolean> cacheOHLCData(String symbol, String exchange,
            String interval, List<MarketDataPoint> ohlcData) {
        return CompletableFuture.supplyAsync(() ->
            Try.of(() -> {
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
            })
            .recover(e -> {
                log.error("Cache write failed for OHLC {}:{}:{}: {}",
                    symbol, exchange, interval, e.getMessage());
                return false;
            })
            .getOrElse(false)
        );
    }

    /**
     * Get order book data from cache
     * Rule #11: Functional error handling with Try monad
     */
    public Optional<CachedOrderBook> getOrderBook(String symbol, String exchange) {
        long startTime = System.nanoTime();

        return Try.of(() -> {
            String key = keyPatterns.orderBookKey(symbol, exchange);
            return redisTemplate.opsForValue().get(key);
        })
        .map(cached -> Optional.ofNullable(cached)
            .map(c -> {
                cacheHits.incrementAndGet();
                logResponseTime("getOrderBook", startTime);
                return (CachedOrderBook) c;
            })
            .orElseGet(() -> {
                cacheMisses.incrementAndGet();
                return null;
            })
        )
        .recover(e -> {
            log.error("Cache get failed for order book {}:{}: {}",
                symbol, exchange, e.getMessage());
            return null;
        })
        .toOptional()
        .flatMap(Optional::ofNullable);
    }

    /**
     * Cache order book data (very short TTL for real-time data)
     * Rule #11: Functional error handling with Try monad
     */
    public CompletableFuture<Boolean> cacheOrderBook(MarketDataPoint dataPoint) {
        return CompletableFuture.supplyAsync(() ->
            Optional.of(dataPoint)
                .filter(MarketDataPoint::hasOrderBookData)
                .map(point -> Try.of(() -> {
                    String key = keyPatterns.orderBookKey(point.symbol(), point.exchange());

                    CachedOrderBook orderBook = new CachedOrderBook(
                        point.symbol(), point.exchange(),
                        point.bid(), point.ask(),
                        point.bidSize(), point.askSize(),
                        point.getSpread(), point.getSpreadPercentage(),
                        point.timestamp(), Instant.now()
                    );

                    redisTemplate.opsForValue().set(key, orderBook, cacheConfig.orderBookTtl());
                    cacheWrites.incrementAndGet();

                    return true;
                })
                .recover(e -> {
                    log.error("Cache write failed for order book {}:{}: {}",
                        point.symbol(), point.exchange(), e.getMessage());
                    return false;
                })
                .getOrElse(false))
                .orElse(false)
        );
    }

    /**
     * Cache warming for active symbols
     * Rule #11: Functional error handling with Try monad
     * Rule #13: Stream API instead of loops
     */
    public CompletableFuture<CacheWarmingResult> warmCache(List<String> symbols, String exchange) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            return Try.of(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

                    var warmingTask = scope.fork(() -> {
                        // Cache symbol list
                        String symbolListKey = keyPatterns.symbolListKey(exchange);
                        redisTemplate.opsForValue().set(symbolListKey, symbols, cacheConfig.symbolListTtl());

                        // Pre-warm frequently accessed data structures using Stream API
                        // Rule #3: Optional instead of if-else
                        long warmedCount = symbols.stream()
                            .map(symbol -> Try.of(() -> {
                                String priceKey = keyPatterns.priceKey(symbol, exchange);
                                return Optional.of(priceKey)
                                    .filter(key -> !redisTemplate.hasKey(key))
                                    .map(key -> {
                                        redisTemplate.opsForValue().set(key + ":placeholder", "warming",
                                            cacheConfig.priceDataTtl());
                                        return 1;
                                    })
                                    .orElse(0);
                            })
                            .recover(e -> {
                                log.warn("Failed to warm cache for symbol {}: {}", symbol, e.getMessage());
                                return 0;
                            })
                            .getOrElse(0))
                            .mapToLong(Integer::longValue)
                            .sum();

                        return (int) warmedCount + SYMBOL_LIST_OFFSET; // Add symbol list entry
                    });

                    scope.join();
                    scope.throwIfFailed();

                    int warmed = warmingTask.get();
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("Cache warming completed: {} entries in {}ms", warmed, duration);
                    return new CacheWarmingResult(warmed, duration, true);
                }
            })
            .recover(e -> {
                log.error("Cache warming failed: {}", e.getMessage());
                return new CacheWarmingResult(0, 0, false);
            })
            .getOrElse(new CacheWarmingResult(0, 0, false));
        });
    }

    /**
     * Get cache performance metrics
     * RULE #17 COMPLIANT: Uses named constants instead of magic numbers
     */
    public CacheMetrics getMetrics() {
        long totalRequests = cacheHits.get() + cacheMisses.get();
        double hitRate = totalRequests > 0 ?
            (double) cacheHits.get() / totalRequests * PERCENTAGE_MULTIPLIER : 0.0;

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
     * Rule #11: Functional error handling with Try monad
     */
    public CompletableFuture<Boolean> clearSymbolCache(String symbol, String exchange) {
        return CompletableFuture.supplyAsync(() ->
            Try.of(() -> {
                List<String> keysToDelete = List.of(
                    keyPatterns.priceKey(symbol, exchange),
                    keyPatterns.tickKey(symbol, exchange),
                    keyPatterns.orderBookKey(symbol, exchange)
                );

                Long deleted = redisTemplate.delete(keysToDelete);
                log.info("Cleared {} cache entries for {}:{}", deleted, symbol, exchange);

                return deleted != null && deleted > 0;
            })
            .recover(e -> {
                log.error("Failed to clear cache for {}:{}: {}", symbol, exchange, e.getMessage());
                return false;
            })
            .getOrElse(false)
        );
    }

    // Helper methods
    /**
     * Log response time with performance target monitoring
     * RULE #3 COMPLIANT: Optional chain instead of if-statement
     * RULE #17 COMPLIANT: Uses PERFORMANCE_TARGET_MS constant
     */
    private void logResponseTime(String operation, long startTimeNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        responseTimeTracker.put(operation, durationMs);

        Optional.of(durationMs)
            .filter(duration -> duration > PERFORMANCE_TARGET_MS)
            .ifPresent(duration ->
                log.warn("{} took {}ms (exceeds {}ms target)", operation, duration, PERFORMANCE_TARGET_MS));
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

    /**
     * Cache metrics record with performance target validation
     * RULE #9 COMPLIANT: Immutable record for metrics data
     * RULE #17 COMPLIANT: Uses named constants for thresholds
     */
    public record CacheMetrics(
        long cacheHits, long cacheMisses, long cacheWrites,
        double hitRate, double avgResponseTimeMs, Instant generatedAt
    ) {
        public boolean isPerformanceTarget() {
            return avgResponseTimeMs < PERFORMANCE_TARGET_MS_DOUBLE && hitRate > HIT_RATE_TARGET_PERCENT;
        }
    }
}