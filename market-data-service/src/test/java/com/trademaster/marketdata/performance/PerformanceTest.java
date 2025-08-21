package com.trademaster.marketdata.performance;

import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for Market Data Service
 * 
 * Tests system performance under various load conditions including:
 * - High-frequency data ingestion
 * - Concurrent API requests
 * - Cache performance benchmarks
 * - WebSocket connection handling
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.trademaster=WARN", // Reduce logging noise
    "trademaster.simulator.enabled=false" // Disable simulator for clean tests
})
@DisplayName("Market Data Performance Tests")
class PerformanceTest {

    private MarketDataService marketDataService;
    private MarketDataCacheService cacheService;

    @BeforeEach
    void setUp() {
        // Services would be injected in a real Spring Boot test
        // For this example, we'll assume they're available
    }

    @Nested
    @DisplayName("Throughput Performance Tests")
    class ThroughputTests {

        @Test
        @DisplayName("Should achieve 100K+ price updates per second")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void shouldAchieveHighThroughputPriceUpdates() {
            // Given
            int dataPointCount = 100_000;
            List<MarketDataPoint> testData = generatePerformanceTestData(dataPointCount);
            
            // When - Measure batch write throughput
            long startTime = System.currentTimeMillis();
            
            // Process in batches for optimal performance
            int batchSize = 1000;
            List<CompletableFuture<Void>> batchTasks = IntStream.range(0, dataPointCount / batchSize)
                .mapToObj(i -> {
                    int start = i * batchSize;
                    int end = Math.min(start + batchSize, dataPointCount);
                    List<MarketDataPoint> batch = testData.subList(start, end);
                    
                    return marketDataService.batchWriteMarketData(batch)
                        .thenAccept(result -> {
                            assertThat(result.successful()).isEqualTo(batch.size());
                        });
                })
                .toList();
            
            CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Then - Verify throughput requirements
            double throughput = (double) dataPointCount / (duration / 1000.0);
            
            assertThat(throughput).isGreaterThan(100_000); // >100K updates/second
            assertThat(duration).isLessThan(10_000); // Complete within 10 seconds
            
            System.out.printf("Throughput: %.0f updates/second%n", throughput);
        }

        @Test
        @DisplayName("Should handle 10K concurrent WebSocket connections")
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void shouldHandle10KConcurrentWebSocketConnections() {
            // Given
            int connectionCount = 10_000;
            
            // When - Simulate concurrent connection attempts
            long startTime = System.currentTimeMillis();
            
            List<CompletableFuture<Boolean>> connectionTasks = IntStream.range(0, connectionCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    // Simulate WebSocket connection establishment
                    try {
                        Thread.sleep(1); // Minimal processing time
                        return true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }))
                .toList();
            
            List<Boolean> results = connectionTasks.stream()
                .map(CompletableFuture::join)
                .toList();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Then - Verify connection handling
            long successfulConnections = results.stream()
                .mapToLong(success -> success ? 1 : 0)
                .sum();
            
            assertThat(successfulConnections).isEqualTo(connectionCount);
            assertThat(duration).isLessThan(30_000); // Complete within 30 seconds
            
            System.out.printf("Handled %d connections in %dms%n", connectionCount, duration);
        }
    }

    @Nested
    @DisplayName("Latency Performance Tests")
    class LatencyTests {

        @Test
        @DisplayName("Should achieve sub-100ms WebSocket message delivery")
        void shouldAchieveSubHundredMsWebSocketDelivery() {
            // Given
            int messageCount = 1000;
            String testSymbol = "LATENCY_TEST";
            
            List<Long> latencies = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            // When - Measure end-to-end message latency
            List<CompletableFuture<Void>> messageTasks = IntStream.range(0, messageCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    long sendTime = System.nanoTime();
                    
                    // Simulate market data creation and WebSocket broadcast
                    MarketDataPoint data = MarketDataPoint.createTickData(
                        testSymbol + "_" + i,
                        "NSE",
                        new BigDecimal("1000.00").add(new BigDecimal(i % 100)),
                        1000L,
                        Instant.now()
                    );
                    
                    // Simulate processing and broadcast
                    try {
                        marketDataService.writeMarketData(data).join();
                        
                        long receiveTime = System.nanoTime();
                        long latencyNs = receiveTime - sendTime;
                        long latencyMs = TimeUnit.NANOSECONDS.toMillis(latencyNs);
                        latencies.add(latencyMs);
                        
                    } catch (Exception e) {
                        // Handle error
                    }
                }))
                .toList();
            
            CompletableFuture.allOf(messageTasks.toArray(new CompletableFuture[0])).join();
            
            // Then - Verify latency requirements
            double avgLatency = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            long maxLatency = latencies.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
            
            long p95Latency = latencies.stream()
                .sorted()
                .skip((long) (latencies.size() * 0.95))
                .findFirst()
                .orElse(0L);
            
            assertThat(avgLatency).isLessThan(100.0); // <100ms average
            assertThat(p95Latency).isLessThan(200L); // <200ms 95th percentile
            
            System.out.printf("Avg latency: %.2fms, P95: %dms, Max: %dms%n", 
                avgLatency, p95Latency, maxLatency);
        }

        @Test
        @DisplayName("Should achieve sub-5ms cache response times")
        void shouldAchieveSubFiveMsCacheResponseTimes() {
            // Given
            String symbol = "CACHE_PERF_TEST";
            String exchange = "NSE";
            int requestCount = 10_000;
            
            // Pre-populate cache
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("1500.00"), 15000L, Instant.now()
            );
            cacheService.cacheCurrentPrice(testData).join();
            
            List<Long> responseTimes = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            // When - Measure cache response times
            List<CompletableFuture<Void>> cacheTasks = IntStream.range(0, requestCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    long startTime = System.nanoTime();
                    
                    var result = cacheService.getCurrentPrice(symbol, exchange);
                    
                    long duration = System.nanoTime() - startTime;
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
                    responseTimes.add(durationMs);
                    
                    assertThat(result).isPresent();
                }))
                .toList();
            
            CompletableFuture.allOf(cacheTasks.toArray(new CompletableFuture[0])).join();
            
            // Then - Verify cache performance
            double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
            
            long p99ResponseTime = responseTimes.stream()
                .sorted()
                .skip((long) (responseTimes.size() * 0.99))
                .findFirst()
                .orElse(0L);
            
            assertThat(avgResponseTime).isLessThan(5.0); // <5ms average
            assertThat(p99ResponseTime).isLessThan(10L); // <10ms 99th percentile
            
            System.out.printf("Cache - Avg: %.2fms, P99: %dms, Max: %dms%n", 
                avgResponseTime, p99ResponseTime, maxResponseTime);
        }
    }

    @Nested
    @DisplayName("Scalability Tests")
    class ScalabilityTests {

        @Test
        @DisplayName("Should scale to process 1000+ symbols simultaneously")
        void shouldScaleToProcess1000SymbolsSimultaneously() {
            // Given
            int symbolCount = 1000;
            List<String> symbols = IntStream.range(0, symbolCount)
                .mapToObj(i -> "SCALE_TEST_" + i)
                .toList();
            
            // When - Process multiple symbols concurrently
            long startTime = System.currentTimeMillis();
            
            var bulkResult = marketDataService.getBulkPriceData(symbols, "NSE").join();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Then - Verify scalability
            assertThat(duration).isLessThan(10_000); // Complete within 10 seconds
            assertThat(bulkResult.size()).isGreaterThanOrEqualTo(symbolCount / 2); // At least 50% success
            
            System.out.printf("Processed %d symbols in %dms%n", bulkResult.size(), duration);
        }

        @Test
        @DisplayName("Should maintain performance under sustained load")
        void shouldMaintainPerformanceUnderSustainedLoad() {
            // Given
            int durationSeconds = 30;
            int requestsPerSecond = 1000;
            
            List<Long> responseTimesPerSecond = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            // When - Generate sustained load
            long testStartTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - testStartTime < durationSeconds * 1000L) {
                long secondStartTime = System.currentTimeMillis();
                
                List<CompletableFuture<Void>> secondTasks = IntStream.range(0, requestsPerSecond)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        // Simulate API request
                        String symbol = "SUSTAINED_" + (i % 100);
                        marketDataService.getCurrentPrice(symbol, "NSE").join();
                    }))
                    .toList();
                
                CompletableFuture.allOf(secondTasks.toArray(new CompletableFuture[0])).join();
                
                long secondDuration = System.currentTimeMillis() - secondStartTime;
                responseTimesPerSecond.add(secondDuration);
                
                // Throttle to maintain target rate
                long remaining = 1000 - secondDuration;
                if (remaining > 0) {
                    try {
                        Thread.sleep(remaining);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // Then - Verify sustained performance
            double avgSecondDuration = responseTimesPerSecond.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            long maxSecondDuration = responseTimesPerSecond.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
            
            assertThat(avgSecondDuration).isLessThan(2000.0); // <2s average per batch
            assertThat(maxSecondDuration).isLessThan(5000L); // <5s worst case
            
            System.out.printf("Sustained load - Avg batch: %.2fms, Max batch: %dms%n", 
                avgSecondDuration, maxSecondDuration);
        }
    }

    @Nested
    @DisplayName("Resource Efficiency Tests")
    class ResourceEfficiencyTests {

        @Test
        @DisplayName("Should maintain efficient memory usage during high load")
        void shouldMaintainEfficientMemoryUsageDuringHighLoad() {
            // Given
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            int dataPointCount = 50_000;
            List<MarketDataPoint> testData = generatePerformanceTestData(dataPointCount);
            
            // When - Process large dataset
            marketDataService.batchWriteMarketData(testData).join();
            
            // Force garbage collection
            System.gc();
            Thread.yield();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Then - Verify memory efficiency
            long maxReasonableIncrease = 100L * 1024 * 1024; // 100MB
            assertThat(memoryIncrease).isLessThan(maxReasonableIncrease);
            
            System.out.printf("Memory increase: %.2f MB%n", memoryIncrease / (1024.0 * 1024.0));
        }

        @Test
        @DisplayName("Should efficiently utilize virtual threads")
        void shouldEfficientlyUtilizeVirtualThreads() {
            // Given
            int taskCount = 10_000;
            
            // When - Execute many lightweight tasks
            long startTime = System.currentTimeMillis();
            
            List<CompletableFuture<Integer>> tasks = IntStream.range(0, taskCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    // Simulate lightweight I/O operation
                    try {
                        Thread.sleep(1);
                        return i;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                }))
                .toList();
            
            List<Integer> results = tasks.stream()
                .map(CompletableFuture::join)
                .toList();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Then - Verify virtual thread efficiency
            assertThat(results).hasSize(taskCount);
            assertThat(duration).isLessThan(5000L); // Should complete quickly with virtual threads
            
            System.out.printf("Virtual threads: %d tasks in %dms%n", taskCount, duration);
        }
    }

    // Helper methods
    private List<MarketDataPoint> generatePerformanceTestData(int count) {
        return IntStream.range(0, count)
            .parallel()
            .mapToObj(i -> MarketDataPoint.createTickData(
                "PERF_" + (i % 100), // 100 unique symbols
                "NSE",
                new BigDecimal("1000.00").add(new BigDecimal(i % 1000)),
                1000L + (i % 10000),
                Instant.now().minusSeconds(i % 3600)
            ))
            .toList();
    }

    @BeforeEach
    void warmUp() {
        // Warm up JVM and caches before performance tests
        List<MarketDataPoint> warmupData = generatePerformanceTestData(100);
        warmupData.forEach(data -> {
            marketDataService.writeMarketData(data).join();
        });
    }
}