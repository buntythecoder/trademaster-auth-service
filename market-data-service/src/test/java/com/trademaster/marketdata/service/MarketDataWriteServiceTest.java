package com.trademaster.marketdata.service;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.repository.MarketDataRepository;
import com.trademaster.marketdata.resilience.CircuitBreakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for MarketDataWriteService
 *
 * Testing Strategy:
 * - Single write operations (DB + cache coordination)
 * - Batch write operations (parallel processing)
 * - Circuit breaker integration (resilience)
 * - Error handling and fallback behavior
 * - Data integrity validation
 *
 * Coverage Target: >80% for critical write path
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Market Data Write Service Tests")
class MarketDataWriteServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private MarketDataCacheService cacheService;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    private MarketDataWriteService writeService;

    @BeforeEach
    void setUp() {
        // Configure circuit breaker to bypass for most tests
        // This allows testing the actual service logic
        lenient().when(circuitBreakerService.executeDatabaseOperationWithFallback(any(), any()))
            .thenAnswer(invocation -> {
                var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
                try {
                    return CompletableFuture.completedFuture(supplier.get());
                } catch (Exception e) {
                    var fallback = invocation.getArgument(1, java.util.function.Supplier.class);
                    return CompletableFuture.completedFuture(fallback.get());
                }
            });

        lenient().when(circuitBreakerService.executeRedisCacheOperationWithFallback(any(), any()))
            .thenAnswer(invocation -> {
                var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
                try {
                    return CompletableFuture.completedFuture(supplier.get());
                } catch (Exception e) {
                    var fallback = invocation.getArgument(1, java.util.function.Supplier.class);
                    return CompletableFuture.completedFuture(fallback.get());
                }
            });

        writeService = new MarketDataWriteService(marketDataRepository, cacheService, circuitBreakerService);
    }

    @Nested
    @DisplayName("Single Write Operations")
    class SingleWriteOperationsTest {

        @Test
        @DisplayName("Should successfully write data to both database and cache")
        void shouldSuccessfullyWriteToDatabaseAndCache() {
            // Given
            var dataPoint = createTestDataPoint("RELIANCE", "NSE", new BigDecimal("2500.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            doNothing().when(cacheService).cacheCurrentPrice(dataPoint);

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isTrue();
            verify(marketDataRepository).writeMarketData(dataPoint);
            verify(cacheService).cacheCurrentPrice(dataPoint);
            verify(circuitBreakerService).executeDatabaseOperationWithFallback(any(), any());
            verify(circuitBreakerService).executeRedisCacheOperationWithFallback(any(), any());
        }

        @Test
        @DisplayName("Should succeed when database writes but cache fails")
        void shouldSucceedWhenDatabaseWritesButCacheFails() {
            // Given
            var dataPoint = createTestDataPoint("TCS", "NSE", new BigDecimal("3400.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // Simulate cache failure by making circuit breaker return fallback for cache operations
            when(circuitBreakerService.executeRedisCacheOperationWithFallback(any(), any()))
                .thenAnswer(invocation -> {
                    var fallback = invocation.getArgument(1, java.util.function.Supplier.class);
                    return CompletableFuture.completedFuture(fallback.get());
                });

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isTrue(); // DB write succeeded, cache failure is non-critical
            verify(marketDataRepository).writeMarketData(dataPoint);
        }

        @Test
        @DisplayName("Should fail when database write fails")
        void shouldFailWhenDatabaseWriteFails() {
            // Given
            var dataPoint = createTestDataPoint("INFY", "NSE", new BigDecimal("1450.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Failed("Database error")));

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isFalse();
            verify(marketDataRepository).writeMarketData(dataPoint);
        }

        @Test
        @DisplayName("Should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() {
            // Given
            var dataPoint = createTestDataPoint("HDFC", "NSE", new BigDecimal("1600.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database connection lost")));

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isFalse();
            verify(marketDataRepository).writeMarketData(dataPoint);
        }

        @Test
        @DisplayName("Should cache order book data when present")
        void shouldCacheOrderBookDataWhenPresent() {
            // Given
            var dataPoint = createTestDataPointWithOrderBook("SBIN", "NSE", new BigDecimal("550.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            doNothing().when(cacheService).cacheCurrentPrice(dataPoint);
            doNothing().when(cacheService).cacheOrderBook(dataPoint);

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isTrue();
            verify(cacheService).cacheCurrentPrice(dataPoint);
            verify(cacheService).cacheOrderBook(dataPoint);
        }

        @Test
        @DisplayName("Should not cache order book when not present")
        void shouldNotCacheOrderBookWhenNotPresent() {
            // Given
            var dataPoint = createTestDataPoint("AXIS", "NSE", new BigDecimal("900.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            doNothing().when(cacheService).cacheCurrentPrice(dataPoint);

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isTrue();
            verify(cacheService).cacheCurrentPrice(dataPoint);
            verify(cacheService, never()).cacheOrderBook(any());
        }
    }

    @Nested
    @DisplayName("Batch Write Operations")
    class BatchWriteOperationsTest {

        @Test
        @DisplayName("Should successfully write batch of data points")
        void shouldSuccessfullyWriteBatchOfDataPoints() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("RELIANCE", "NSE", new BigDecimal("2500.00")),
                createTestDataPoint("TCS", "NSE", new BigDecimal("3400.00")),
                createTestDataPoint("INFY", "NSE", new BigDecimal("1450.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(3, 0, 3)));

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(3);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.cacheUpdates()).isEqualTo(3);
            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
            verify(marketDataRepository).batchWriteMarketData(dataPoints);
            verify(cacheService).batchCachePrices(dataPoints);
        }

        @Test
        @DisplayName("Should handle database failure in batch write")
        void shouldHandleDatabaseFailureInBatchWrite() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("HDFC", "NSE", new BigDecimal("1600.00")),
                createTestDataPoint("ICICI", "NSE", new BigDecimal("900.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Failed("Database error")));
            when(cacheService.batchCachePrices(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(0, 2, 0)));

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(2);
            verify(marketDataRepository).batchWriteMarketData(dataPoints);
        }

        @Test
        @DisplayName("Should handle cache failure in batch write")
        void shouldHandleCacheFailureInBatchWrite() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("SBIN", "NSE", new BigDecimal("550.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // Simulate cache failure
            when(circuitBreakerService.executeRedisCacheOperationWithFallback(any(), any()))
                .thenAnswer(invocation -> {
                    var fallback = invocation.getArgument(1, java.util.function.Supplier.class);
                    return CompletableFuture.completedFuture(fallback.get());
                });

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(1);
            assertThat(result.cacheUpdates()).isEqualTo(0); // Cache failed
        }

        @Test
        @DisplayName("Should handle empty batch gracefully")
        void shouldHandleEmptyBatchGracefully() {
            // Given
            List<MarketDataPoint> emptyList = Collections.emptyList();
            when(marketDataRepository.batchWriteMarketData(emptyList))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(emptyList))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(0, 0, 0)));

            // When
            var result = writeService.batchWriteMarketData(emptyList).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should complete batch write within acceptable time")
        void shouldCompleteBatchWriteWithinAcceptableTime() {
            // Given
            List<MarketDataPoint> largeDataSet = Arrays.asList(
                createTestDataPoint("SYM1", "NSE", new BigDecimal("100.00")),
                createTestDataPoint("SYM2", "NSE", new BigDecimal("200.00")),
                createTestDataPoint("SYM3", "NSE", new BigDecimal("300.00")),
                createTestDataPoint("SYM4", "NSE", new BigDecimal("400.00")),
                createTestDataPoint("SYM5", "NSE", new BigDecimal("500.00"))
            );
            when(marketDataRepository.batchWriteMarketData(largeDataSet))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(largeDataSet))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(5, 0, 5)));

            // When
            long startTime = System.currentTimeMillis();
            var result = writeService.batchWriteMarketData(largeDataSet).join();
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(5);
            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
            assertThat(duration).isLessThan(5000); // Should complete within 5 seconds for 5 items
        }

        @Test
        @DisplayName("Should handle complete batch write exception")
        void shouldHandleCompleteBatchWriteException() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("ERROR", "NSE", new BigDecimal("1.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Complete failure")));

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.successful()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Integration")
    class CircuitBreakerIntegrationTest {

        @Test
        @DisplayName("Should use circuit breaker for database operations")
        void shouldUseCircuitBreakerForDatabaseOperations() {
            // Given
            var dataPoint = createTestDataPoint("TEST", "NSE", new BigDecimal("100.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // When
            writeService.writeMarketData(dataPoint).join();

            // Then
            verify(circuitBreakerService).executeDatabaseOperationWithFallback(any(), any());
        }

        @Test
        @DisplayName("Should use circuit breaker for cache operations")
        void shouldUseCircuitBreakerForCacheOperations() {
            // Given
            var dataPoint = createTestDataPoint("TEST", "NSE", new BigDecimal("100.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // When
            writeService.writeMarketData(dataPoint).join();

            // Then
            verify(circuitBreakerService).executeRedisCacheOperationWithFallback(any(), any());
        }

        @Test
        @DisplayName("Should fallback when circuit breaker opens for database")
        void shouldFallbackWhenCircuitBreakerOpensForDatabase() {
            // Given
            var dataPoint = createTestDataPoint("CIRCUIT", "NSE", new BigDecimal("999.00"));

            // Simulate circuit breaker open - always use fallback
            when(circuitBreakerService.executeDatabaseOperationWithFallback(any(), any()))
                .thenAnswer(invocation -> {
                    var fallback = invocation.getArgument(1, java.util.function.Supplier.class);
                    return CompletableFuture.completedFuture(fallback.get());
                });

            // When
            var result = writeService.writeMarketData(dataPoint).join();

            // Then
            assertThat(result).isFalse(); // Fallback returns Failed result
            verify(circuitBreakerService).executeDatabaseOperationWithFallback(any(), any());
            verify(marketDataRepository, never()).writeMarketData(any());
        }

        @Test
        @DisplayName("Should use circuit breaker for batch database operations")
        void shouldUseCircuitBreakerForBatchDatabaseOperations() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("BATCH1", "NSE", new BigDecimal("100.00")),
                createTestDataPoint("BATCH2", "NSE", new BigDecimal("200.00"))
            );
            when(marketDataRepository.batchWriteMarketData(anyList()))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(anyList()))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(2, 0, 2)));

            // When
            writeService.batchWriteMarketData(dataPoints).join();

            // Then
            verify(circuitBreakerService, atLeast(2)).executeDatabaseOperationWithFallback(any(), any());
        }

        @Test
        @DisplayName("Should use circuit breaker for batch cache operations")
        void shouldUseCircuitBreakerForBatchCacheOperations() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("CACHE1", "NSE", new BigDecimal("100.00"))
            );
            when(marketDataRepository.batchWriteMarketData(anyList()))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(anyList()))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(1, 0, 1)));

            // When
            writeService.batchWriteMarketData(dataPoints).join();

            // Then
            verify(circuitBreakerService, atLeast(2)).executeRedisCacheOperationWithFallback(any(), any());
        }
    }

    @Nested
    @DisplayName("Data Integrity Validation")
    class DataIntegrityValidationTest {

        @Test
        @DisplayName("Should preserve all data point fields during write")
        void shouldPreserveAllDataPointFieldsDuringWrite() {
            // Given
            var originalDataPoint = createTestDataPointWithAllFields();
            when(marketDataRepository.writeMarketData(originalDataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // When
            writeService.writeMarketData(originalDataPoint).join();

            // Then
            verify(marketDataRepository).writeMarketData(argThat(dataPoint ->
                dataPoint.symbol().equals(originalDataPoint.symbol()) &&
                dataPoint.exchange().equals(originalDataPoint.exchange()) &&
                dataPoint.price().compareTo(originalDataPoint.price()) == 0 &&
                dataPoint.volume().equals(originalDataPoint.volume())
            ));
        }

        @Test
        @DisplayName("Should handle concurrent writes without data corruption")
        void shouldHandleConcurrentWritesWithoutDataCorruption() {
            // Given
            var dataPoint1 = createTestDataPoint("CONCURRENT1", "NSE", new BigDecimal("100.00"));
            var dataPoint2 = createTestDataPoint("CONCURRENT2", "NSE", new BigDecimal("200.00"));
            when(marketDataRepository.writeMarketData(any()))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // When
            var future1 = writeService.writeMarketData(dataPoint1);
            var future2 = writeService.writeMarketData(dataPoint2);
            CompletableFuture.allOf(future1, future2).join();

            // Then
            assertThat(future1.join()).isTrue();
            assertThat(future2.join()).isTrue();
            verify(marketDataRepository, times(2)).writeMarketData(any());
        }

        @Test
        @DisplayName("Should validate batch data consistency")
        void shouldValidateBatchDataConsistency() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("VALID1", "NSE", new BigDecimal("100.00")),
                createTestDataPoint("VALID2", "NSE", new BigDecimal("200.00")),
                createTestDataPoint("VALID3", "NSE", new BigDecimal("300.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(3, 0, 3)));

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result.successful()).isEqualTo(dataPoints.size());
            verify(marketDataRepository).batchWriteMarketData(argThat(list -> list.size() == 3));
        }
    }

    @Nested
    @DisplayName("Performance Validation")
    class PerformanceValidationTest {

        @Test
        @DisplayName("Should execute parallel database and cache writes")
        void shouldExecuteParallelDatabaseAndCacheWrites() {
            // Given
            var dataPoint = createTestDataPoint("PARALLEL", "NSE", new BigDecimal("500.00"));
            when(marketDataRepository.writeMarketData(dataPoint))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));

            // When
            long startTime = System.currentTimeMillis();
            writeService.writeMarketData(dataPoint).join();
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertThat(duration).isLessThan(2000); // Parallel execution should be faster
            verify(marketDataRepository).writeMarketData(dataPoint);
            verify(cacheService).cacheCurrentPrice(dataPoint);
        }

        @Test
        @DisplayName("Should report accurate batch processing time")
        void shouldReportAccurateBatchProcessingTime() {
            // Given
            List<MarketDataPoint> dataPoints = Arrays.asList(
                createTestDataPoint("TIME1", "NSE", new BigDecimal("100.00")),
                createTestDataPoint("TIME2", "NSE", new BigDecimal("200.00"))
            );
            when(marketDataRepository.batchWriteMarketData(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataRepository.WriteResult.Success(1, Instant.now())));
            when(cacheService.batchCachePrices(dataPoints))
                .thenReturn(CompletableFuture.completedFuture(
                    new MarketDataCacheService.BatchCacheResult(2, 0, 2)));

            // When
            var result = writeService.batchWriteMarketData(dataPoints).join();

            // Then
            assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
            assertThat(result.durationMs()).isLessThan(5000);
        }
    }

    // ============ Test Helper Methods ============

    private MarketDataPoint createTestDataPoint(String symbol, String exchange, BigDecimal price) {
        // Use factory method for TICK data (real-time price update)
        return MarketDataPoint.createTickData(
            symbol,
            exchange,
            price,
            100000L,  // volume
            Instant.now()
        );
    }

    private MarketDataPoint createTestDataPointWithOrderBook(String symbol, String exchange, BigDecimal price) {
        // Use factory method for ORDER_BOOK data
        return MarketDataPoint.createOrderBookData(
            symbol,
            exchange,
            price,  // bid
            price.add(BigDecimal.ONE),  // ask
            1000L,  // bidSize
            1000L,  // askSize
            Instant.now()
        );
    }

    private MarketDataPoint createTestDataPointWithAllFields() {
        // Create comprehensive data point with all fields using builder
        return MarketDataPoint.builder()
            .symbol("COMPREHENSIVE")
            .exchange("NSE")
            .dataType("TICK")
            .source("REALTIME")
            .price(new BigDecimal("2500.00"))
            .volume(500000L)
            .bid(new BigDecimal("2499.50"))
            .ask(new BigDecimal("2500.50"))
            .high(new BigDecimal("2550.00"))
            .low(new BigDecimal("2450.00"))
            .open(new BigDecimal("2475.00"))
            .previousClose(new BigDecimal("2400.00"))
            .change(new BigDecimal("100.00"))
            .changePercent(new BigDecimal("4.17"))
            .bidSize(1000L)
            .askSize(1000L)
            .marketStatus("OPEN")
            .qualityScore(0.95)
            .timestamp(Instant.now())
            .build();
    }
}
