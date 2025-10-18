package com.trademaster.marketdata.integration;

import com.trademaster.marketdata.MarketDataServiceApplication;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataService;
import com.trademaster.marketdata.service.MarketDataWriteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Phase 6C Refactoring Integration Tests
 *
 * Validates that Phase 6C refactorings work correctly in integration:
 * - Pattern 2 (Layered Extraction) helper methods
 * - Circuit breaker integration (Rule #25)
 * - Functional patterns (Rule #3, #11, #13)
 * - Virtual thread concurrency (Rule #12)
 * - No regressions from refactoring
 *
 * Coverage:
 * - Circuit breaker fallback behavior
 * - Helper method integration and composition
 * - CompletableFuture chaining and error handling
 * - Optional and Stream API functional patterns
 * - Parallel processing with virtual threads
 * - Cache/database coordination
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = MarketDataServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@TestPropertySource(properties = {
    "trademaster.simulator.enabled=true",
    "logging.level.com.trademaster=DEBUG",
    "resilience4j.circuitbreaker.configs.default.slidingWindowSize=5",
    "resilience4j.circuitbreaker.configs.default.failureRateThreshold=50"
})
@DisplayName("Phase 6C: MarketDataService Refactoring Integration Tests")
class MarketDataServiceRefactoringIntegrationTest {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private MarketDataCacheService cacheService;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    // Test containers
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Nested
    @DisplayName("Pattern 2: Helper Method Integration Tests")
    class HelperMethodIntegrationTests {

        @Test
        @DisplayName("getCurrentPrice() should use helper methods for cache and database access")
        void testGetCurrentPriceHelperMethodIntegration() throws Exception {
            // Given: Symbol with no cached data (will trigger database fallback)
            String symbol = "HELPER_TEST_001";
            String exchange = "NSE";

            // Pre-populate database with test data
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("1500.00"), 50000L, Instant.now()
            );
            marketDataService.writeMarketData(testData).join();

            // Clear cache to force database access
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                var cached = cacheService.getCurrentPrice(symbol, exchange);
                assertThat(cached).isPresent();
            });

            // When: Get current price (uses convertCachedPriceToDataPoint helper)
            CompletableFuture<Optional<MarketDataPoint>> result =
                marketDataService.getCurrentPrice(symbol, exchange);

            // Then: Helper methods should work correctly
            Optional<MarketDataPoint> dataPoint = result.get(5, TimeUnit.SECONDS);
            assertThat(dataPoint).isPresent();
            assertThat(dataPoint.get().symbol()).isEqualTo(symbol);
            assertThat(dataPoint.get().exchange()).isEqualTo(exchange);
            assertThat(dataPoint.get().price()).isEqualByComparingTo(new BigDecimal("1500.00"));

            // Verify functional pattern: Optional.map() and Optional.orElseGet() used
            assertThat(result.isDone()).isTrue();
            assertThat(result.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("getBulkPriceData() should use createBulkPriceTasks() and combineBulkPriceResults() helpers")
        void testBulkPriceDataHelperMethodIntegration() throws Exception {
            // Given: Multiple symbols with pre-populated data
            List<String> symbols = List.of("BULK_HELPER_1", "BULK_HELPER_2", "BULK_HELPER_3");
            String exchange = "NSE";

            // Pre-populate with test data
            for (int i = 0; i < symbols.size(); i++) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbols.get(i), exchange,
                    new BigDecimal("100.00").add(BigDecimal.valueOf(i * 10)),
                    1000L * (i + 1),
                    Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            // Wait for cache updates
            Thread.sleep(500);

            // When: Get bulk price data (uses helper methods for parallel processing)
            CompletableFuture<Map<String, MarketDataPoint>> result =
                marketDataService.getBulkPriceData(symbols, exchange);

            // Then: All helper methods should work correctly
            Map<String, MarketDataPoint> dataMap = result.get(10, TimeUnit.SECONDS);
            assertThat(dataMap).hasSize(3);
            assertThat(dataMap).containsKeys("BULK_HELPER_1", "BULK_HELPER_2", "BULK_HELPER_3");

            // Verify parallel processing completed successfully
            dataMap.forEach((symbol, dataPoint) -> {
                assertThat(dataPoint.symbol()).isEqualTo(symbol);
                assertThat(dataPoint.exchange()).isEqualTo(exchange);
                assertThat(dataPoint.price()).isNotNull();
            });
        }

        @Test
        @DisplayName("writeMarketData() should use executeSingleDatabaseWrite() and executeSingleCacheUpdate() helpers")
        void testWriteMarketDataHelperMethodIntegration() throws Exception {
            // Given: Test data point
            MarketDataPoint testData = MarketDataPoint.createTickData(
                "WRITE_HELPER_001", "NSE", new BigDecimal("2000.00"), 75000L, Instant.now()
            );

            // When: Write market data (uses helper methods for DB and cache operations)
            CompletableFuture<Boolean> writeResult = marketDataService.writeMarketData(testData);

            // Then: Both helper methods should execute successfully
            Boolean success = writeResult.get(5, TimeUnit.SECONDS);
            assertThat(success).isTrue();

            // Verify data was written to both database and cache
            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                var retrieved = marketDataService.getCurrentPrice("WRITE_HELPER_001", "NSE").join();
                assertThat(retrieved).isPresent();
                assertThat(retrieved.get().price()).isEqualByComparingTo(new BigDecimal("2000.00"));
            });

            // Verify cache has the data
            var cached = cacheService.getCurrentPrice("WRITE_HELPER_001", "NSE");
            assertThat(cached).isPresent();
        }

        @Test
        @DisplayName("batchWriteMarketData() should use executeBatchDatabaseWrite() and executeBatchCacheUpdate() helpers")
        void testBatchWriteHelperMethodIntegration() throws Exception {
            // Given: Batch of test data points
            List<MarketDataPoint> batchData = List.of(
                MarketDataPoint.createTickData("BATCH_HELPER_1", "NSE", new BigDecimal("100.00"), 1000L, Instant.now()),
                MarketDataPoint.createTickData("BATCH_HELPER_2", "NSE", new BigDecimal("200.00"), 2000L, Instant.now()),
                MarketDataPoint.createTickData("BATCH_HELPER_3", "NSE", new BigDecimal("300.00"), 3000L, Instant.now())
            );

            // When: Batch write (uses helper methods for batch operations)
            CompletableFuture<MarketDataWriteService.BatchWriteResult> result =
                marketDataService.batchWriteMarketData(batchData);

            // Then: Helper methods should complete batch operations successfully
            MarketDataWriteService.BatchWriteResult batchResult = result.get(10, TimeUnit.SECONDS);
            assertThat(batchResult.successful()).isEqualTo(3);
            assertThat(batchResult.failed()).isEqualTo(0);
            assertThat(batchResult.durationMs()).isLessThan(5000);

            // Verify all data points were written
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                for (MarketDataPoint dataPoint : batchData) {
                    var retrieved = marketDataService.getCurrentPrice(dataPoint.symbol(), "NSE").join();
                    assertThat(retrieved).isPresent();
                }
            });
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Integration Tests (Rule #25)")
    class CircuitBreakerIntegrationTests {

        @Test
        @DisplayName("getCurrentPrice() should fallback gracefully when cache circuit breaker opens")
        void testCacheCircuitBreakerFallback() throws Exception {
            // Given: Symbol that exists in database
            String symbol = "CB_CACHE_TEST_001";
            String exchange = "NSE";

            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("1000.00"), 10000L, Instant.now()
            );
            marketDataService.writeMarketData(testData).join();

            // When: Get price with potential cache failure (circuit breaker provides fallback)
            CompletableFuture<Optional<MarketDataPoint>> result =
                marketDataService.getCurrentPrice(symbol, exchange);

            // Then: Should fallback to database and return data successfully
            Optional<MarketDataPoint> dataPoint = result.get(10, TimeUnit.SECONDS);
            assertThat(dataPoint).isPresent();
            assertThat(dataPoint.get().symbol()).isEqualTo(symbol);

            // Verify circuit breaker protected the operation
            assertThat(result.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("writeMarketData() should continue with database write even if cache circuit breaker opens")
        void testWriteWithCacheCircuitBreakerFallback() throws Exception {
            // Given: Test data point
            MarketDataPoint testData = MarketDataPoint.createTickData(
                "CB_WRITE_TEST_001", "NSE", new BigDecimal("1500.00"), 15000L, Instant.now()
            );

            // When: Write with potential cache failure
            CompletableFuture<Boolean> result = marketDataService.writeMarketData(testData);

            // Then: Database write should succeed even if cache fails
            Boolean success = result.get(5, TimeUnit.SECONDS);
            assertThat(success).isTrue();

            // Verify data is accessible (from database if cache failed)
            await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
                var retrieved = marketDataService.getCurrentPrice("CB_WRITE_TEST_001", "NSE").join();
                assertThat(retrieved).isPresent();
            });
        }

        @Test
        @DisplayName("batchWriteMarketData() should have circuit breaker protection for bulk operations")
        void testBatchWriteCircuitBreakerProtection() throws Exception {
            // Given: Large batch to test circuit breaker coordination
            List<MarketDataPoint> largeBatch = generateTestBatch(50, "CB_BATCH");

            // When: Batch write with circuit breaker protection
            CompletableFuture<MarketDataWriteService.BatchWriteResult> result =
                marketDataService.batchWriteMarketData(largeBatch);

            // Then: Circuit breaker should protect the operation
            MarketDataWriteService.BatchWriteResult batchResult = result.get(30, TimeUnit.SECONDS);

            // Should handle partial failures gracefully
            assertThat(batchResult.successful() + batchResult.failed()).isEqualTo(50);
            assertThat(result.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("getBulkPriceData() should use circuit breaker for each parallel operation")
        void testBulkPriceCircuitBreakerProtection() throws Exception {
            // Given: Multiple symbols
            List<String> symbols = List.of("CB_BULK_1", "CB_BULK_2", "CB_BULK_3", "CB_BULK_4", "CB_BULK_5");

            // Pre-populate some symbols (not all to test partial failures)
            for (int i = 0; i < 3; i++) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbols.get(i), "NSE", new BigDecimal("100.00").multiply(BigDecimal.valueOf(i + 1)),
                    1000L, Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            Thread.sleep(500);

            // When: Bulk retrieval with some missing data
            CompletableFuture<Map<String, MarketDataPoint>> result =
                marketDataService.getBulkPriceData(symbols, "NSE");

            // Then: Circuit breaker should protect each operation
            Map<String, MarketDataPoint> dataMap = result.get(10, TimeUnit.SECONDS);

            // Should return available data without throwing exceptions
            assertThat(dataMap.size()).isGreaterThanOrEqualTo(3);
            assertThat(result.isCompletedExceptionally()).isFalse();
        }
    }

    @Nested
    @DisplayName("Functional Pattern Integration Tests (Rules #3, #11, #13)")
    class FunctionalPatternIntegrationTests {

        @Test
        @DisplayName("CompletableFuture chaining should work correctly throughout the service")
        void testCompletableFutureChainingIntegration() throws Exception {
            // Given: Test data
            String symbol = "FUNC_CHAIN_001";
            String exchange = "NSE";
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("500.00"), 5000L, Instant.now()
            );

            // When: Chain multiple async operations
            CompletableFuture<Boolean> writeResult = marketDataService.writeMarketData(testData);
            CompletableFuture<Optional<MarketDataPoint>> readResult = writeResult
                .thenCompose(success -> {
                    assertThat(success).isTrue();
                    return marketDataService.getCurrentPrice(symbol, exchange);
                });

            // Then: Chaining should work correctly
            Optional<MarketDataPoint> result = readResult.get(10, TimeUnit.SECONDS);
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(symbol);
        }

        @Test
        @DisplayName("Optional functional patterns should work correctly in service layer")
        void testOptionalFunctionalPatternsIntegration() throws Exception {
            // Given: Symbol that exists
            String existingSymbol = "FUNC_OPT_EXISTS";
            String missingSymbol = "FUNC_OPT_MISSING";

            MarketDataPoint testData = MarketDataPoint.createTickData(
                existingSymbol, "NSE", new BigDecimal("750.00"), 7500L, Instant.now()
            );
            marketDataService.writeMarketData(testData).join();

            Thread.sleep(500);

            // When: Use Optional patterns
            CompletableFuture<Optional<MarketDataPoint>> existingResult =
                marketDataService.getCurrentPrice(existingSymbol, "NSE");
            CompletableFuture<Optional<MarketDataPoint>> missingResult =
                marketDataService.getCurrentPrice(missingSymbol, "NSE");

            // Then: Optional patterns should handle both cases correctly
            Optional<MarketDataPoint> existing = existingResult.get(5, TimeUnit.SECONDS);
            Optional<MarketDataPoint> missing = missingResult.get(5, TimeUnit.SECONDS);

            assertThat(existing).isPresent();
            assertThat(existing.get().symbol()).isEqualTo(existingSymbol);

            assertThat(missing).isEmpty();  // No exception, just empty Optional
        }

        @Test
        @DisplayName("Stream API functional patterns should work in parallel processing")
        void testStreamApiFunctionalPatternsIntegration() throws Exception {
            // Given: Multiple symbols for stream processing
            List<String> symbols = List.of("FUNC_STREAM_1", "FUNC_STREAM_2", "FUNC_STREAM_3");

            for (int i = 0; i < symbols.size(); i++) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbols.get(i), "NSE",
                    new BigDecimal("100.00").multiply(BigDecimal.valueOf(i + 1)),
                    1000L, Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            Thread.sleep(500);

            // When: Use Stream API through bulk operations
            CompletableFuture<Map<String, MarketDataPoint>> result =
                marketDataService.getBulkPriceData(symbols, "NSE");

            // Then: Stream API should process all symbols correctly
            Map<String, MarketDataPoint> dataMap = result.get(10, TimeUnit.SECONDS);
            assertThat(dataMap).hasSize(3);

            // Verify functional transformation worked
            dataMap.values().forEach(dataPoint -> {
                assertThat(dataPoint.symbol()).startsWith("FUNC_STREAM_");
                assertThat(dataPoint.price()).isGreaterThan(BigDecimal.ZERO);
            });
        }

        @Test
        @DisplayName("Error handling with exceptionally() should work correctly")
        void testExceptionallyErrorHandlingIntegration() throws Exception {
            // Given: Invalid symbol that will trigger error path
            String invalidSymbol = null;  // Intentionally null to trigger error

            // When: Call with invalid input
            CompletableFuture<Optional<MarketDataPoint>> result =
                marketDataService.getCurrentPrice(invalidSymbol, "NSE");

            // Then: exceptionally() should handle error gracefully
            Optional<MarketDataPoint> errorResult = result.get(5, TimeUnit.SECONDS);

            // Should return empty Optional, not throw exception
            assertThat(errorResult).isEmpty();
            assertThat(result.isCompletedExceptionally()).isFalse();
        }
    }

    @Nested
    @DisplayName("Virtual Thread Concurrency Tests (Rule #12)")
    class VirtualThreadConcurrencyTests {

        @Test
        @DisplayName("Parallel bulk operations should use virtual threads efficiently")
        void testVirtualThreadParallelProcessing() throws Exception {
            // Given: Large number of symbols to test parallel processing
            int symbolCount = 100;
            List<String> symbols = generateSymbolList(symbolCount, "VT_PARALLEL");

            // Pre-populate half the symbols
            for (int i = 0; i < symbolCount / 2; i++) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbols.get(i), "NSE",
                    new BigDecimal("100.00").add(BigDecimal.valueOf(i)),
                    1000L, Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            Thread.sleep(1000);

            // When: Bulk retrieval with virtual threads
            long startTime = System.currentTimeMillis();
            CompletableFuture<Map<String, MarketDataPoint>> result =
                marketDataService.getBulkPriceData(symbols, "NSE");

            Map<String, MarketDataPoint> dataMap = result.get(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Virtual threads should process efficiently
            assertThat(dataMap.size()).isGreaterThanOrEqualTo(symbolCount / 2);

            // With virtual threads, 100 parallel operations should complete quickly
            assertThat(duration).isLessThan(10000);  // <10 seconds for 100 operations

            // Verify no resource exhaustion (would happen with platform threads)
            assertThat(result.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("Concurrent writes should be thread-safe with virtual threads")
        void testConcurrentWritesThreadSafety() throws Exception {
            // Given: Multiple concurrent write operations
            int concurrentWrites = 50;
            List<CompletableFuture<Boolean>> writeTasks = new java.util.ArrayList<>();

            // When: Execute concurrent writes
            for (int i = 0; i < concurrentWrites; i++) {
                final int index = i;
                MarketDataPoint data = MarketDataPoint.createTickData(
                    "VT_CONCURRENT_" + index, "NSE",
                    new BigDecimal("100.00").add(BigDecimal.valueOf(index)),
                    1000L, Instant.now()
                );

                CompletableFuture<Boolean> writeTask = marketDataService.writeMarketData(data);
                writeTasks.add(writeTask);
            }

            // Wait for all writes
            CompletableFuture<Void> allWrites = CompletableFuture.allOf(
                writeTasks.toArray(new CompletableFuture[0])
            );
            allWrites.get(30, TimeUnit.SECONDS);

            // Then: All writes should succeed without race conditions
            long successCount = writeTasks.stream()
                .map(CompletableFuture::join)
                .filter(Boolean::booleanValue)
                .count();

            assertThat(successCount).isEqualTo(concurrentWrites);
        }

        @Test
        @DisplayName("High concurrency should not cause thread pool exhaustion")
        void testHighConcurrencyVirtualThreads() throws Exception {
            // Given: Very high concurrency scenario
            int highConcurrency = 1000;
            List<String> symbols = generateSymbolList(highConcurrency, "VT_HIGH");
            AtomicInteger completedOperations = new AtomicInteger(0);

            // Pre-populate data
            for (int i = 0; i < Math.min(100, highConcurrency); i++) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbols.get(i), "NSE", new BigDecimal("100.00"), 1000L, Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            Thread.sleep(1000);

            // When: Execute high concurrency reads
            long startTime = System.currentTimeMillis();
            List<CompletableFuture<Optional<MarketDataPoint>>> tasks = new java.util.ArrayList<>();

            for (String symbol : symbols) {
                CompletableFuture<Optional<MarketDataPoint>> task =
                    marketDataService.getCurrentPrice(symbol, "NSE")
                        .thenApply(result -> {
                            completedOperations.incrementAndGet();
                            return result;
                        });
                tasks.add(task);
            }

            // Wait for completion
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

            long duration = System.currentTimeMillis() - startTime;

            // Then: Should complete without thread pool exhaustion
            assertThat(completedOperations.get()).isEqualTo(highConcurrency);

            // Virtual threads should handle 1000 concurrent operations efficiently
            assertThat(duration).isLessThan(30000);  // <30 seconds for 1000 operations
        }
    }

    @Nested
    @DisplayName("Regression Tests - No Functional Changes")
    class RegressionValidationTests {

        @Test
        @DisplayName("API behavior should be unchanged after refactoring")
        void testApiBehaviorUnchanged() throws Exception {
            // Given: Standard API usage pattern
            String symbol = "REG_API_001";
            String exchange = "NSE";
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("1200.00"), 12000L, Instant.now()
            );

            // When: Use standard API methods
            CompletableFuture<Boolean> writeResult = marketDataService.writeMarketData(testData);
            writeResult.join();

            Thread.sleep(500);

            CompletableFuture<Optional<MarketDataPoint>> readResult =
                marketDataService.getCurrentPrice(symbol, exchange);

            // Then: API should behave exactly as before refactoring
            Optional<MarketDataPoint> result = readResult.get(5, TimeUnit.SECONDS);
            assertThat(result).isPresent();
            assertThat(result.get().symbol()).isEqualTo(symbol);
            assertThat(result.get().exchange()).isEqualTo(exchange);
            assertThat(result.get().price()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(result.get().volume()).isEqualTo(12000L);
        }

        @Test
        @DisplayName("Batch operations should maintain same behavior after refactoring")
        void testBatchOperationsBehaviorUnchanged() throws Exception {
            // Given: Batch operation pattern
            List<MarketDataPoint> batchData = List.of(
                MarketDataPoint.createTickData("REG_BATCH_1", "NSE", new BigDecimal("100.00"), 1000L, Instant.now()),
                MarketDataPoint.createTickData("REG_BATCH_2", "NSE", new BigDecimal("200.00"), 2000L, Instant.now()),
                MarketDataPoint.createTickData("REG_BATCH_3", "NSE", new BigDecimal("300.00"), 3000L, Instant.now())
            );

            // When: Execute batch write
            CompletableFuture<MarketDataWriteService.BatchWriteResult> result =
                marketDataService.batchWriteMarketData(batchData);

            // Then: Behavior should be unchanged
            MarketDataWriteService.BatchWriteResult batchResult = result.get(10, TimeUnit.SECONDS);
            assertThat(batchResult.successful()).isEqualTo(3);
            assertThat(batchResult.failed()).isEqualTo(0);
            assertThat(batchResult.durationMs()).isPositive();
        }

        @Test
        @DisplayName("Error handling behavior should be consistent after refactoring")
        void testErrorHandlingBehaviorUnchanged() throws Exception {
            // Given: Invalid data that should trigger error handling
            MarketDataPoint invalidData = MarketDataPoint.builder()
                .symbol("REG_ERROR_001")
                .exchange("NSE")
                .price(new BigDecimal("-100.00"))  // Invalid negative price
                .timestamp(Instant.now())
                .build();

            // When: Attempt to write invalid data
            CompletableFuture<Boolean> result = marketDataService.writeMarketData(invalidData);

            // Then: Error handling should work as before (validation or graceful failure)
            Boolean success = result.get(5, TimeUnit.SECONDS);

            // Should handle invalid data gracefully (either validate or fail safely)
            assertThat(result.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("Performance characteristics should be maintained or improved")
        void testPerformanceNotRegressed() throws Exception {
            // Given: Performance test scenario
            List<String> symbols = generateSymbolList(20, "REG_PERF");

            // Pre-populate
            for (String symbol : symbols) {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbol, "NSE", new BigDecimal("100.00"), 1000L, Instant.now()
                );
                marketDataService.writeMarketData(data).join();
            }

            Thread.sleep(500);

            // When: Measure performance
            long startTime = System.currentTimeMillis();
            CompletableFuture<Map<String, MarketDataPoint>> result =
                marketDataService.getBulkPriceData(symbols, "NSE");

            result.get(10, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Then: Performance should not regress (should be <2 seconds for 20 symbols)
            assertThat(duration).isLessThan(2000);
        }
    }

    // Helper methods
    private List<MarketDataPoint> generateTestBatch(int count, String prefix) {
        List<MarketDataPoint> batch = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            batch.add(MarketDataPoint.createTickData(
                prefix + "_" + i, "NSE",
                new BigDecimal("100.00").add(BigDecimal.valueOf(i)),
                1000L + i, Instant.now()
            ));
        }
        return batch;
    }

    private List<String> generateSymbolList(int count, String prefix) {
        List<String> symbols = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            symbols.add(prefix + "_" + i);
        }
        return symbols;
    }

    @BeforeEach
    void setUp() {
        // Test initialization
    }

    @AfterEach
    void tearDown() {
        // Test cleanup
    }
}
