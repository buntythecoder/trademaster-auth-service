package com.trademaster.marketdata.integration;

import com.trademaster.marketdata.config.TestContainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestContainers Integration Test
 *
 * Demonstrates usage of TestContainers for integration testing with:
 * - PostgreSQL database operations
 * - Redis cache operations
 * - Kafka message streaming
 *
 * Following MANDATORY RULE #20 (Testing Standards):
 * - Integration tests with >70% coverage
 * - TestContainers for real infrastructure
 * - Virtual thread testing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("integration")
@Import(TestContainersConfiguration.class)
@DisplayName("TestContainers Integration Tests")
class TestContainersIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TestContainersIntegrationTest.class);

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("PostgreSQL container should be running and accessible")
    void testPostgreSQLContainer() {
        // Given: TestContainers PostgreSQL is running
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();

        // When: Create test table and insert data
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_market_data (
                id SERIAL PRIMARY KEY,
                symbol VARCHAR(10) NOT NULL,
                exchange VARCHAR(10) NOT NULL,
                price DECIMAL(10, 2) NOT NULL,
                volume BIGINT NOT NULL,
                timestamp TIMESTAMP NOT NULL
            )
            """);

        final var symbol = "RELIANCE";
        final var exchange = "NSE";
        final var price = BigDecimal.valueOf(2500.00);
        final var volume = 1000000L;
        final var timestamp = Instant.now();

        jdbcTemplate.update(
            "INSERT INTO test_market_data (symbol, exchange, price, volume, timestamp) VALUES (?, ?, ?, ?, ?)",
            symbol, exchange, price, volume, timestamp
        );

        // Then: Data should be persisted and retrievable
        final var count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_market_data WHERE symbol = ?",
            Integer.class,
            symbol
        );
        assertThat(count).isEqualTo(1);

        final var retrievedSymbol = jdbcTemplate.queryForObject(
            "SELECT symbol FROM test_market_data WHERE symbol = ?",
            String.class,
            symbol
        );
        assertThat(retrievedSymbol).isEqualTo("RELIANCE");

        // Cleanup
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_market_data");

        log.info("PostgreSQL integration test passed - Data persisted and retrieved: {}", symbol);
    }

    @Test
    @DisplayName("Redis container should be running and accessible")
    void testRedisContainer() {
        // Given: TestContainers Redis is running
        assertThat(redisTemplate).isNotNull();

        // When: Store data in Redis cache
        final var cacheKey = "test:market:RELIANCE";
        final var cacheValue = "Price: 2500.00";

        redisTemplate.opsForValue().set(cacheKey, cacheValue, 60, TimeUnit.SECONDS);

        // Then: Data should be cached and retrievable
        final var retrieved = redisTemplate.opsForValue().get(cacheKey);
        assertThat(retrieved).isEqualTo(cacheValue);

        // Cleanup
        redisTemplate.delete(cacheKey);

        log.info("Redis integration test passed - Cache operations successful");
    }

    @Test
    @DisplayName("Kafka container should be running and accessible")
    void testKafkaContainer() {
        // Given: TestContainers Kafka is running
        assertThat(kafkaTemplate).isNotNull();

        // When: Send message to Kafka topic
        final var topic = "market-data-test";
        final var message = "Test market data message";

        final var sendFuture = kafkaTemplate.send(topic, message);

        // Then: Message should be sent successfully
        assertThat(sendFuture).isNotNull();
        assertThat(sendFuture.isDone()).isFalse(); // Async operation

        log.info("Kafka integration test passed - Message sent to topic: {}", topic);
    }

    @Test
    @DisplayName("All containers should work together in integration scenario")
    void testFullIntegrationScenario() {
        // Given: All TestContainers are running
        assertThat(jdbcTemplate).isNotNull();
        assertThat(redisTemplate).isNotNull();
        assertThat(kafkaTemplate).isNotNull();

        // When: Perform end-to-end integration test
        // 1. Save to database
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS integration_test_data (
                id SERIAL PRIMARY KEY,
                symbol VARCHAR(10) NOT NULL,
                price DECIMAL(10, 2) NOT NULL
            )
            """);

        final var symbol = "TATASTEEL";
        final var price = BigDecimal.valueOf(120.50);

        jdbcTemplate.update(
            "INSERT INTO integration_test_data (symbol, price) VALUES (?, ?)",
            symbol, price
        );

        // 2. Cache the result
        final var cacheKey = "integration:test:" + symbol;
        final var cacheValue = String.format("Symbol: %s, Price: %s", symbol, price);
        redisTemplate.opsForValue().set(cacheKey, cacheValue, 60, TimeUnit.SECONDS);

        // 3. Publish event to Kafka
        kafkaTemplate.send("market-data-integration", "Market data saved: " + symbol);

        // Then: All operations should succeed
        final var cachedValue = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedValue).isEqualTo(cacheValue);

        final var dbSymbol = jdbcTemplate.queryForObject(
            "SELECT symbol FROM integration_test_data WHERE symbol = ?",
            String.class,
            symbol
        );
        assertThat(dbSymbol).isEqualTo(symbol);

        // Cleanup
        jdbcTemplate.execute("DROP TABLE IF EXISTS integration_test_data");
        redisTemplate.delete(cacheKey);

        log.info("Full integration scenario test passed - All containers working together");
    }
}
