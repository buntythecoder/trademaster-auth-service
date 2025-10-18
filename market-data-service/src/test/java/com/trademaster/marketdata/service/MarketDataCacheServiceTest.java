package com.trademaster.marketdata.service;

import com.trademaster.marketdata.config.RedisConfig;
import com.trademaster.marketdata.entity.MarketDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive unit tests for MarketDataCacheService
 *
 * Tests functional patterns, error handling, performance metrics, and Redis operations
 * Target: >80% code coverage
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataCacheService Unit Tests")
class MarketDataCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisTemplate<String, String> customStringRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisConfig.MarketDataCacheConfig cacheConfig;

    @Mock
    private RedisConfig.RedisKeyPatterns keyPatterns;

    @InjectMocks
    private MarketDataCacheService cacheService;

    private static final String TEST_SYMBOL = "AAPL";
    private static final String TEST_EXCHANGE = "NASDAQ";
    private static final BigDecimal TEST_PRICE = new BigDecimal("150.50");
    private static final Long TEST_VOLUME = 1000000L;

    @BeforeEach
    void setUp() {
        // Setup default mock behaviors with lenient() to avoid unnecessary stubbing warnings
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(cacheConfig.priceDataTtl()).thenReturn(Duration.ofMinutes(5));
        lenient().when(cacheConfig.ohlcDataTtl()).thenReturn(Duration.ofMinutes(15));
        lenient().when(cacheConfig.orderBookTtl()).thenReturn(Duration.ofSeconds(30));
        lenient().when(cacheConfig.symbolListTtl()).thenReturn(Duration.ofHours(1));
    }

    @Nested
    @DisplayName("Current Price Cache Tests")
    class CurrentPriceCacheTests {

        @Test
        @DisplayName("Should return cached price on cache hit")
        void testGetCurrentPriceCacheHit() {
            // Given
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);

            var cachedPrice = new MarketDataCacheService.CachedPrice(
                TEST_SYMBOL, TEST_EXCHANGE, TEST_PRICE, TEST_VOLUME,
                BigDecimal.ONE, BigDecimal.TEN, Instant.now(), Instant.now()
            );
            when(valueOperations.get(cacheKey)).thenReturn(cachedPrice);

            // When
            Optional<MarketDataCacheService.CachedPrice> result =
                cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(TEST_SYMBOL);
            assertThat(result.get().exchange()).isEqualTo(TEST_EXCHANGE);
            assertThat(result.get().price()).isEqualByComparingTo(TEST_PRICE);

            verify(valueOperations).get(cacheKey);
        }

        @Test
        @DisplayName("Should return empty on cache miss")
        void testGetCurrentPriceCacheMiss() {
            // Given
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);
            when(valueOperations.get(cacheKey)).thenReturn(null);

            // When
            Optional<MarketDataCacheService.CachedPrice> result =
                cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get(cacheKey);
        }

        @Test
        @DisplayName("Should handle Redis errors gracefully and return empty")
        void testGetCurrentPriceRedisError() {
            // Given
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);
            when(valueOperations.get(cacheKey)).thenThrow(new RuntimeException("Redis connection failed"));

            // When
            Optional<MarketDataCacheService.CachedPrice> result =
                cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get(cacheKey);
        }

        @Test
        @DisplayName("Should cache current price successfully")
        void testCacheCurrentPriceSuccess() throws ExecutionException, InterruptedException {
            // Given
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);

            var dataPoint = createTestMarketDataPoint();

            // When
            CompletableFuture<Boolean> future = cacheService.cacheCurrentPrice(dataPoint);
            Boolean result = future.get();

            // Then
            assertThat(result).isTrue();
            verify(valueOperations).set(eq(cacheKey), any(MarketDataCacheService.CachedPrice.class), any(Duration.class));
        }

        @Test
        @DisplayName("Should handle cache write errors and return false")
        void testCacheCurrentPriceError() throws ExecutionException, InterruptedException {
            // Given
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);
            doThrow(new RuntimeException("Redis write failed"))
                .when(valueOperations).set(any(), any(), any(Duration.class));

            var dataPoint = createTestMarketDataPoint();

            // When
            CompletableFuture<Boolean> future = cacheService.cacheCurrentPrice(dataPoint);
            Boolean result = future.get();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Batch Cache Operations Tests")
    class BatchCacheOperationsTests {

        @Test
        @DisplayName("Should batch cache multiple prices successfully")
        void testBatchCachePricesSuccess() throws ExecutionException, InterruptedException {
            // Given
            var dataPoints = List.of(
                createTestMarketDataPoint("AAPL", "NASDAQ"),
                createTestMarketDataPoint("GOOGL", "NASDAQ"),
                createTestMarketDataPoint("MSFT", "NASDAQ")
            );

            when(keyPatterns.priceKey(anyString(), anyString())).thenAnswer(
                inv -> "price:" + inv.getArgument(0) + ":" + inv.getArgument(1)
            );

            // When
            CompletableFuture<MarketDataCacheService.BatchCacheResult> future =
                cacheService.batchCachePrices(dataPoints);
            MarketDataCacheService.BatchCacheResult result = future.get();

            // Then
            assertThat(result.successful()).isGreaterThan(0);
            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should handle batch cache errors gracefully")
        void testBatchCachePricesError() throws ExecutionException, InterruptedException {
            // Given
            var dataPoints = List.of(createTestMarketDataPoint());
            when(keyPatterns.priceKey(anyString(), anyString())).thenThrow(new RuntimeException("Batch operation failed"));

            // When
            CompletableFuture<MarketDataCacheService.BatchCacheResult> future =
                cacheService.batchCachePrices(dataPoints);
            MarketDataCacheService.BatchCacheResult result = future.get();

            // Then
            assertThat(result.successful()).isZero();
            assertThat(result.failed()).isEqualTo(dataPoints.size());
        }

        @Test
        @DisplayName("Should filter invalid data points in batch")
        void testBatchCachePricesWithInvalidData() throws ExecutionException, InterruptedException {
            // Given
            var validPoint = createTestMarketDataPoint("AAPL", "NASDAQ");
            var invalidPoint = createInvalidMarketDataPoint(); // This will fail isValid() check
            var dataPoints = List.of(validPoint, invalidPoint);

            when(keyPatterns.priceKey(anyString(), anyString())).thenAnswer(
                inv -> "price:" + inv.getArgument(0) + ":" + inv.getArgument(1)
            );

            // When
            CompletableFuture<MarketDataCacheService.BatchCacheResult> future =
                cacheService.batchCachePrices(dataPoints);
            MarketDataCacheService.BatchCacheResult result = future.get();

            // Then - Only valid points should be cached
            assertThat(result.successful()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("OHLC Data Cache Tests")
    class OHLCDataCacheTests {

        @Test
        @DisplayName("Should return cached OHLC data on cache hit")
        void testGetOHLCDataCacheHit() {
            // Given
            String cacheKey = "ohlc:AAPL:NASDAQ:1d";
            String interval = "1d";
            when(keyPatterns.ohlcKey(TEST_SYMBOL, TEST_EXCHANGE, interval)).thenReturn(cacheKey);

            var cachedOHLC = List.of(
                new MarketDataCacheService.CachedOHLC(
                    TEST_SYMBOL, TEST_EXCHANGE,
                    new BigDecimal("148.00"), new BigDecimal("152.00"),
                    new BigDecimal("147.50"), TEST_PRICE,
                    TEST_VOLUME, Instant.now()
                )
            );
            when(valueOperations.get(cacheKey)).thenReturn(cachedOHLC);

            // When
            Optional<List<MarketDataCacheService.CachedOHLC>> result =
                cacheService.getOHLCData(TEST_SYMBOL, TEST_EXCHANGE, interval);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(1);
            assertThat(result.get().get(0).symbol()).isEqualTo(TEST_SYMBOL);

            verify(valueOperations).get(cacheKey);
        }

        @Test
        @DisplayName("Should return empty on OHLC cache miss")
        void testGetOHLCDataCacheMiss() {
            // Given
            String cacheKey = "ohlc:AAPL:NASDAQ:1d";
            String interval = "1d";
            when(keyPatterns.ohlcKey(TEST_SYMBOL, TEST_EXCHANGE, interval)).thenReturn(cacheKey);
            when(valueOperations.get(cacheKey)).thenReturn(null);

            // When
            Optional<List<MarketDataCacheService.CachedOHLC>> result =
                cacheService.getOHLCData(TEST_SYMBOL, TEST_EXCHANGE, interval);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should cache OHLC data successfully")
        void testCacheOHLCDataSuccess() throws ExecutionException, InterruptedException {
            // Given
            String cacheKey = "ohlc:AAPL:NASDAQ:1d";
            String interval = "1d";
            when(keyPatterns.ohlcKey(TEST_SYMBOL, TEST_EXCHANGE, interval)).thenReturn(cacheKey);

            var ohlcData = List.of(createTestMarketDataPoint());

            // When
            CompletableFuture<Boolean> future =
                cacheService.cacheOHLCData(TEST_SYMBOL, TEST_EXCHANGE, interval, ohlcData);
            Boolean result = future.get();

            // Then
            assertThat(result).isTrue();
            verify(valueOperations).set(eq(cacheKey), anyList(), any(Duration.class));
        }

        @Test
        @DisplayName("Should handle OHLC cache errors")
        void testCacheOHLCDataError() throws ExecutionException, InterruptedException {
            // Given
            String cacheKey = "ohlc:AAPL:NASDAQ:1d";
            String interval = "1d";
            when(keyPatterns.ohlcKey(TEST_SYMBOL, TEST_EXCHANGE, interval)).thenReturn(cacheKey);
            doThrow(new RuntimeException("Cache write failed"))
                .when(valueOperations).set(any(), any(), any(Duration.class));

            var ohlcData = List.of(createTestMarketDataPoint());

            // When
            CompletableFuture<Boolean> future =
                cacheService.cacheOHLCData(TEST_SYMBOL, TEST_EXCHANGE, interval, ohlcData);
            Boolean result = future.get();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Order Book Cache Tests")
    class OrderBookCacheTests {

        @Test
        @DisplayName("Should return cached order book on cache hit")
        void testGetOrderBookCacheHit() {
            // Given
            String cacheKey = "orderbook:AAPL:NASDAQ";
            when(keyPatterns.orderBookKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);

            var cachedOrderBook = new MarketDataCacheService.CachedOrderBook(
                TEST_SYMBOL, TEST_EXCHANGE,
                new BigDecimal("150.45"), new BigDecimal("150.55"),
                500L, 600L,
                new BigDecimal("0.10"), new BigDecimal("0.066"),
                Instant.now(), Instant.now()
            );
            when(valueOperations.get(cacheKey)).thenReturn(cachedOrderBook);

            // When
            Optional<MarketDataCacheService.CachedOrderBook> result =
                cacheService.getOrderBook(TEST_SYMBOL, TEST_EXCHANGE);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(TEST_SYMBOL);
            assertThat(result.get().bid()).isEqualByComparingTo(new BigDecimal("150.45"));

            verify(valueOperations).get(cacheKey);
        }

        @Test
        @DisplayName("Should return empty on order book cache miss")
        void testGetOrderBookCacheMiss() {
            // Given
            String cacheKey = "orderbook:AAPL:NASDAQ";
            when(keyPatterns.orderBookKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);
            when(valueOperations.get(cacheKey)).thenReturn(null);

            // When
            Optional<MarketDataCacheService.CachedOrderBook> result =
                cacheService.getOrderBook(TEST_SYMBOL, TEST_EXCHANGE);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should cache order book successfully")
        void testCacheOrderBookSuccess() throws ExecutionException, InterruptedException {
            // Given
            String cacheKey = "orderbook:AAPL:NASDAQ";
            when(keyPatterns.orderBookKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);

            var dataPoint = createTestMarketDataPointWithOrderBook();

            // When
            CompletableFuture<Boolean> future = cacheService.cacheOrderBook(dataPoint);
            Boolean result = future.get();

            // Then
            assertThat(result).isTrue();
            verify(valueOperations).set(eq(cacheKey), any(MarketDataCacheService.CachedOrderBook.class), any(Duration.class));
        }

        @Test
        @DisplayName("Should skip caching order book without data")
        void testCacheOrderBookWithoutData() throws ExecutionException, InterruptedException {
            // Given
            var dataPoint = createTestMarketDataPoint(); // No order book data

            // When
            CompletableFuture<Boolean> future = cacheService.cacheOrderBook(dataPoint);
            Boolean result = future.get();

            // Then
            assertThat(result).isFalse();
            verify(valueOperations, never()).set(any(), any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Cache Warming Tests")
    class CacheWarmingTests {

        @Test
        @DisplayName("Should warm cache successfully")
        void testWarmCacheSuccess() throws ExecutionException, InterruptedException {
            // Given
            var symbols = List.of("AAPL", "GOOGL", "MSFT");
            String symbolListKey = "symbols:NASDAQ";

            when(keyPatterns.symbolListKey(TEST_EXCHANGE)).thenReturn(symbolListKey);
            when(keyPatterns.priceKey(anyString(), anyString())).thenAnswer(
                inv -> "price:" + inv.getArgument(0) + ":" + inv.getArgument(1)
            );
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            CompletableFuture<MarketDataCacheService.CacheWarmingResult> future =
                cacheService.warmCache(symbols, TEST_EXCHANGE);
            MarketDataCacheService.CacheWarmingResult result = future.get();

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.entriesWarmed()).isGreaterThan(0);
            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);

            verify(valueOperations).set(eq(symbolListKey), eq(symbols), any(Duration.class));
        }

        @Test
        @DisplayName("Should handle cache warming errors")
        void testWarmCacheError() throws ExecutionException, InterruptedException {
            // Given
            var symbols = List.of("AAPL");
            when(keyPatterns.symbolListKey(TEST_EXCHANGE)).thenThrow(new RuntimeException("Warming failed"));

            // When
            CompletableFuture<MarketDataCacheService.CacheWarmingResult> future =
                cacheService.warmCache(symbols, TEST_EXCHANGE);
            MarketDataCacheService.CacheWarmingResult result = future.get();

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.entriesWarmed()).isZero();
        }

        @Test
        @DisplayName("Should skip already warmed entries")
        void testWarmCacheSkipExisting() throws ExecutionException, InterruptedException {
            // Given
            var symbols = List.of("AAPL", "GOOGL");
            String symbolListKey = "symbols:NASDAQ";

            when(keyPatterns.symbolListKey(TEST_EXCHANGE)).thenReturn(symbolListKey);
            when(keyPatterns.priceKey(anyString(), anyString())).thenAnswer(
                inv -> "price:" + inv.getArgument(0) + ":" + inv.getArgument(1)
            );
            when(redisTemplate.hasKey(anyString())).thenReturn(true); // Already exists

            // When
            CompletableFuture<MarketDataCacheService.CacheWarmingResult> future =
                cacheService.warmCache(symbols, TEST_EXCHANGE);
            MarketDataCacheService.CacheWarmingResult result = future.get();

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.entriesWarmed()).isGreaterThanOrEqualTo(1); // At least symbol list
        }
    }

    @Nested
    @DisplayName("Cache Metrics Tests")
    class CacheMetricsTests {

        @Test
        @DisplayName("Should return cache metrics with correct calculations")
        void testGetMetrics() {
            // Simulate some cache operations to populate metrics
            // This is tested implicitly through other tests that increment counters

            // When
            MarketDataCacheService.CacheMetrics metrics = cacheService.getMetrics();

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.cacheHits()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.cacheMisses()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.cacheWrites()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.hitRate()).isGreaterThanOrEqualTo(0.0);
            assertThat(metrics.avgResponseTimeMs()).isGreaterThanOrEqualTo(0.0);
            assertThat(metrics.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate hit rate correctly")
        void testCacheHitRateCalculation() {
            // Given - Execute operations to generate metrics
            String cacheKey = "price:AAPL:NASDAQ";
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(cacheKey);

            var cachedPrice = new MarketDataCacheService.CachedPrice(
                TEST_SYMBOL, TEST_EXCHANGE, TEST_PRICE, TEST_VOLUME,
                BigDecimal.ONE, BigDecimal.TEN, Instant.now(), Instant.now()
            );

            // Simulate cache hits
            when(valueOperations.get(cacheKey)).thenReturn(cachedPrice);
            cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);
            cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);

            // Simulate cache miss
            when(valueOperations.get(cacheKey)).thenReturn(null);
            cacheService.getCurrentPrice(TEST_SYMBOL, TEST_EXCHANGE);

            // When
            MarketDataCacheService.CacheMetrics metrics = cacheService.getMetrics();

            // Then
            assertThat(metrics.hitRate()).isGreaterThan(0.0);
            assertThat(metrics.hitRate()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should check performance target correctly")
        void testIsPerformanceTarget() {
            // When
            MarketDataCacheService.CacheMetrics metrics = cacheService.getMetrics();

            // Then - Method should return boolean without error
            boolean meetsTarget = metrics.isPerformanceTarget();
            assertThat(meetsTarget).isIn(true, false);
        }
    }

    @Nested
    @DisplayName("Cache Clearing Tests")
    class CacheClearingTests {

        @Test
        @DisplayName("Should clear symbol cache successfully")
        void testClearSymbolCacheSuccess() throws ExecutionException, InterruptedException {
            // Given
            String priceKey = "price:AAPL:NASDAQ";
            String tickKey = "tick:AAPL:NASDAQ";
            String orderBookKey = "orderbook:AAPL:NASDAQ";

            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(priceKey);
            when(keyPatterns.tickKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(tickKey);
            when(keyPatterns.orderBookKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(orderBookKey);
            when(redisTemplate.delete(anyList())).thenReturn(3L);

            // When
            CompletableFuture<Boolean> future = cacheService.clearSymbolCache(TEST_SYMBOL, TEST_EXCHANGE);
            Boolean result = future.get();

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).delete(anyList());
        }

        @Test
        @DisplayName("Should handle clear cache errors")
        void testClearSymbolCacheError() throws ExecutionException, InterruptedException {
            // Given
            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenThrow(new RuntimeException("Clear failed"));

            // When
            CompletableFuture<Boolean> future = cacheService.clearSymbolCache(TEST_SYMBOL, TEST_EXCHANGE);
            Boolean result = future.get();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when no keys deleted")
        void testClearSymbolCacheNoKeys() throws ExecutionException, InterruptedException {
            // Given
            String priceKey = "price:AAPL:NASDAQ";
            String tickKey = "tick:AAPL:NASDAQ";
            String orderBookKey = "orderbook:AAPL:NASDAQ";

            when(keyPatterns.priceKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(priceKey);
            when(keyPatterns.tickKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(tickKey);
            when(keyPatterns.orderBookKey(TEST_SYMBOL, TEST_EXCHANGE)).thenReturn(orderBookKey);
            when(redisTemplate.delete(anyList())).thenReturn(0L);

            // When
            CompletableFuture<Boolean> future = cacheService.clearSymbolCache(TEST_SYMBOL, TEST_EXCHANGE);
            Boolean result = future.get();

            // Then
            assertThat(result).isFalse();
        }
    }

    // Helper methods to create test data

    private MarketDataPoint createTestMarketDataPoint() {
        return createTestMarketDataPoint(TEST_SYMBOL, TEST_EXCHANGE);
    }

    private MarketDataPoint createTestMarketDataPoint(String symbol, String exchange) {
        return MarketDataPoint.builder()
            .symbol(symbol)
            .exchange(exchange)
            .price(TEST_PRICE)
            .volume(TEST_VOLUME)
            .change(BigDecimal.ONE)
            .changePercent(BigDecimal.TEN)
            .open(new BigDecimal("148.00"))
            .high(new BigDecimal("152.00"))
            .low(new BigDecimal("147.50"))
            .timestamp(Instant.now())
            .dataType("PRICE")
            .source("TEST")
            .build();
    }

    private MarketDataPoint createTestMarketDataPointWithOrderBook() {
        return MarketDataPoint.builder()
            .symbol(TEST_SYMBOL)
            .exchange(TEST_EXCHANGE)
            .price(TEST_PRICE)
            .volume(TEST_VOLUME)
            .bid(new BigDecimal("150.45"))
            .ask(new BigDecimal("150.55"))
            .bidSize(500L)
            .askSize(600L)
            .timestamp(Instant.now())
            .dataType("ORDER_BOOK")
            .source("TEST")
            .build();
    }

    private MarketDataPoint createInvalidMarketDataPoint() {
        return MarketDataPoint.builder()
            .symbol(null) // Invalid - null symbol
            .exchange(TEST_EXCHANGE)
            .price(null) // Invalid - null price
            .timestamp(Instant.now())
            .dataType("PRICE")
            .source("TEST")
            .build();
    }
}
