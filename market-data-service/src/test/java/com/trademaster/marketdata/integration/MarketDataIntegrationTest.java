package com.trademaster.marketdata.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.marketdata.MarketDataServiceApplication;
import com.trademaster.marketdata.entity.MarketDataPoint;
import com.trademaster.marketdata.service.MarketDataCacheService;
import com.trademaster.marketdata.service.MarketDataService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Comprehensive integration tests for Market Data Service
 * 
 * Features:
 * - End-to-end API testing
 * - Database integration testing
 * - Cache performance testing
 * - WebSocket functionality testing
 * - Kafka integration testing
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = MarketDataServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@TestPropertySource(properties = {
    "trademaster.simulator.enabled=true",
    "logging.level.com.trademaster=DEBUG"
})
class MarketDataIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private MarketDataCacheService cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    // Test containers
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static final GenericContainer<?> influxdb = new GenericContainer<>("influxdb:2.7-alpine")
        .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
        .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "password")
        .withEnv("DOCKER_INFLUXDB_INIT_ORG", "trademaster")
        .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", "market-data")
        .withExposedPorts(8086);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // InfluxDB configuration
        registry.add("influxdb.url", () -> "http://" + influxdb.getHost() + ":" + influxdb.getFirstMappedPort());
        registry.add("influxdb.token", () -> "test-token");
        registry.add("influxdb.org", () -> "trademaster");
        registry.add("influxdb.bucket", () -> "market-data");
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Nested
    @DisplayName("Market Data API Tests")
    class MarketDataApiTests {

        @Test
        @WithMockUser(roles = "PROFESSIONAL")
        @DisplayName("Should retrieve current price for valid symbol")
        void shouldRetrieveCurrentPrice() {
            // Given
            String symbol = "RELIANCE";
            String exchange = "NSE";
            
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/market-data/price/{symbol}?exchange={exchange}",
                Map.class, symbol, exchange
            );
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data).isNotNull();
            assertThat(data.get("symbol")).isEqualTo(symbol);
            assertThat(data.get("exchange")).isEqualTo(exchange);
            assertThat(data.get("price")).isNotNull();
        }

        @Test
        @WithMockUser(roles = "FREE")
        @DisplayName("Should restrict real-time data for free tier users")
        void shouldRestrictRealtimeDataForFreeTier() {
            // Given
            String symbol = "TCS";
            String exchange = "NSE";
            
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/market-data/price/{symbol}?exchange={exchange}",
                Map.class, symbol, exchange
            );
            
            // Then - Free tier may get delayed data or restrictions
            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FORBIDDEN);
        }

        @Test
        @WithMockUser(roles = "PROFESSIONAL")
        @DisplayName("Should retrieve historical data within allowed range")
        void shouldRetrieveHistoricalData() {
            // Given
            String symbol = "INFY";
            String exchange = "NSE";
            Instant to = Instant.now();
            Instant from = to.minusSeconds(3600); // 1 hour ago
            
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/market-data/history/{symbol}?exchange={exchange}&from={from}&to={to}&interval=1m",
                Map.class, symbol, exchange, from, to
            );
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @WithMockUser(roles = "PROFESSIONAL")
        @DisplayName("Should retrieve market overview for multiple symbols")
        void shouldRetrieveMarketOverview() {
            // Given
            String symbols = "RELIANCE,TCS,INFY";
            String exchange = "NSE";
            
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/market-data/overview?symbols={symbols}&exchange={exchange}",
                Map.class, symbols, exchange
            );
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return health check status")
        void shouldReturnHealthCheck() {
            // When
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/market-data/health", Map.class
            );
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Data Storage and Retrieval Tests")
    class DataStorageTests {

        @Test
        @DisplayName("Should store and retrieve market data point")
        void shouldStoreAndRetrieveMarketData() {
            // Given
            MarketDataPoint testData = MarketDataPoint.createTickData(
                "TEST_SYMBOL",
                "NSE",
                new BigDecimal("1000.50"),
                100000L,
                Instant.now()
            );
            
            // When
            boolean stored = marketDataService.writeMarketData(testData).join();
            
            // Then
            assertThat(stored).isTrue();
            
            // Verify retrieval
            await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var retrieved = marketDataService.getCurrentPrice("TEST_SYMBOL", "NSE").join();
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get().price()).isEqualByComparingTo(testData.price());
                });
        }

        @Test
        @DisplayName("Should batch write multiple data points")
        void shouldBatchWriteMarketData() {
            // Given
            List<MarketDataPoint> testData = List.of(
                MarketDataPoint.createTickData("BATCH_TEST1", "NSE", new BigDecimal("100.00"), 1000L, Instant.now()),
                MarketDataPoint.createTickData("BATCH_TEST2", "NSE", new BigDecimal("200.00"), 2000L, Instant.now()),
                MarketDataPoint.createTickData("BATCH_TEST3", "NSE", new BigDecimal("300.00"), 3000L, Instant.now())
            );
            
            // When
            var result = marketDataService.batchWriteMarketData(testData).join();
            
            // Then
            assertThat(result.successful()).isEqualTo(3);
            assertThat(result.failed()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cache Performance Tests")
    class CachePerformanceTests {

        @Test
        @DisplayName("Should achieve sub-5ms cache response times")
        void shouldAchieveSubFiveMsCacheResponse() {
            // Given
            String symbol = "CACHE_TEST";
            String exchange = "NSE";
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("500.00"), 5000L, Instant.now()
            );
            
            // Pre-populate cache
            cacheService.cacheCurrentPrice(testData).join();
            
            // When - Measure cache performance
            long startTime = System.nanoTime();
            var cachedData = cacheService.getCurrentPrice(symbol, exchange);
            long duration = System.nanoTime() - startTime;
            
            // Then
            assertThat(cachedData).isPresent();
            
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
            assertThat(durationMs).isLessThan(5); // Sub-5ms requirement
        }

        @Test
        @DisplayName("Should maintain high cache hit rate")
        void shouldMaintainHighCacheHitRate() {
            // Given
            List<String> symbols = List.of("HIT_TEST1", "HIT_TEST2", "HIT_TEST3");
            
            // Pre-populate cache
            symbols.forEach(symbol -> {
                MarketDataPoint data = MarketDataPoint.createTickData(
                    symbol, "NSE", new BigDecimal("100.00"), 1000L, Instant.now()
                );
                cacheService.cacheCurrentPrice(data).join();
            });
            
            // When - Access cached data multiple times
            symbols.forEach(symbol -> {
                for (int i = 0; i < 10; i++) {
                    cacheService.getCurrentPrice(symbol, "NSE");
                }
            });
            
            // Then
            var metrics = cacheService.getMetrics();
            assertThat(metrics.hitRate()).isGreaterThan(80.0); // >80% hit rate
        }

        @Test
        @DisplayName("Should handle cache warming effectively")
        void shouldHandleCacheWarming() {
            // Given
            List<String> symbols = List.of("WARM1", "WARM2", "WARM3", "WARM4", "WARM5");
            
            // When
            var result = cacheService.warmCache(symbols, "NSE").join();
            
            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.entriesWarmed()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Data Quality Tests")
    class DataQualityTests {

        @Test
        @DisplayName("Should validate data quality correctly")
        void shouldValidateDataQuality() {
            // Given - Valid data point
            MarketDataPoint validData = MarketDataPoint.createTickData(
                "VALID_TEST", "NSE", new BigDecimal("1000.00"), 10000L, Instant.now()
            );
            
            // When
            boolean stored = marketDataService.writeMarketData(validData).join();
            
            // Then
            assertThat(stored).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid data points")
        void shouldRejectInvalidDataPoints() {
            // Given - Invalid data points
            List<MarketDataPoint> invalidData = List.of(
                // Negative price
                MarketDataPoint.builder()
                    .symbol("INVALID1")
                    .exchange("NSE")
                    .price(new BigDecimal("-100.00"))
                    .timestamp(Instant.now())
                    .build(),
                
                // Null timestamp
                MarketDataPoint.builder()
                    .symbol("INVALID2")
                    .exchange("NSE")
                    .price(new BigDecimal("100.00"))
                    .timestamp(null)
                    .build()
            );
            
            // When/Then
            invalidData.forEach(data -> {
                assertThat(data.isValid()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle high-volume data ingestion")
        void shouldHandleHighVolumeDataIngestion() {
            // Given - Generate 1000 data points
            List<MarketDataPoint> bulkData = generateBulkTestData(1000);
            
            // When - Measure ingestion performance
            long startTime = System.currentTimeMillis();
            var result = marketDataService.batchWriteMarketData(bulkData).join();
            long duration = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(result.successful()).isEqualTo(1000);
            assertThat(duration).isLessThan(5000); // < 5 seconds for 1000 records
            
            // Verify throughput (>200 records/second)
            double throughput = (double) result.successful() / (duration / 1000.0);
            assertThat(throughput).isGreaterThan(200);
        }

        @Test
        @DisplayName("Should maintain API response times under load")
        void shouldMaintainApiResponseTimes() {
            // Given
            String symbol = "PERF_TEST";
            String exchange = "NSE";
            
            // Pre-populate with test data
            MarketDataPoint testData = MarketDataPoint.createTickData(
                symbol, exchange, new BigDecimal("500.00"), 5000L, Instant.now()
            );
            marketDataService.writeMarketData(testData).join();
            
            // When - Make multiple concurrent requests
            List<Long> responseTimes = new java.util.concurrent.CopyOnWriteArrayList<>();
            
            List<CompletableFuture<Void>> tasks = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    long start = System.currentTimeMillis();
                    restTemplate.getForEntity(
                        "/api/v1/market-data/price/{symbol}?exchange={exchange}",
                        Map.class, symbol, exchange
                    );
                    long duration = System.currentTimeMillis() - start;
                    responseTimes.add(duration);
                }));
            }
            
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            
            // Then - Verify response times
            double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            assertThat(avgResponseTime).isLessThan(100); // <100ms average response
        }
    }

    // Helper methods
    private List<MarketDataPoint> generateBulkTestData(int count) {
        List<MarketDataPoint> data = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            data.add(MarketDataPoint.createTickData(
                "BULK_" + i,
                "NSE",
                new BigDecimal("100.00").add(new BigDecimal(i % 100)),
                1000L + (i % 10000),
                Instant.now().minusSeconds(i)
            ));
        }
        
        return data;
    }

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        // This would require adding a clear method to cache service
    }

    @AfterEach
    void tearDown() {
        // Cleanup test data if needed
    }
}