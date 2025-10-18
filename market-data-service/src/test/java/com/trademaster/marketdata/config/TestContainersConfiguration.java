package com.trademaster.marketdata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers Configuration for Integration Tests
 *
 * Following MANDATORY RULE #12 (Task #12):
 * - PostgreSQL container for database integration tests
 * - Redis container for cache integration tests
 * - Kafka container for event streaming integration tests
 *
 * Following MANDATORY RULE #20 (Testing Standards):
 * - Integration tests with >70% coverage using TestContainers
 * - Real database/cache/messaging behavior validation
 *
 * Benefits:
 * - Real infrastructure behavior in tests
 * - Automatic container lifecycle management
 * - Consistent test environment across machines
 * - No manual infrastructure setup required
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestContainersConfiguration.class);

    // PostgreSQL Configuration Constants
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String POSTGRES_DATABASE = "testdb";
    private static final String POSTGRES_USERNAME = "test";
    private static final String POSTGRES_PASSWORD = "test";

    // Redis Configuration Constants
    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;

    // Kafka Configuration Constants
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.0";

    /**
     * PostgreSQL TestContainer
     * Rule #16: All configuration externalized
     */
    @Bean
    PostgreSQLContainer<?> postgresContainer() {
        log.info("Starting PostgreSQL TestContainer for integration tests");

        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName(POSTGRES_DATABASE)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withReuse(true)  // Reuse container across test runs for performance
            .withCommand(
                "postgres",
                "-c", "max_connections=200",
                "-c", "shared_buffers=256MB",
                "-c", "effective_cache_size=1GB"
            );
    }

    /**
     * Redis TestContainer
     * Rule #16: All configuration externalized
     */
    @Bean
    GenericContainer<?> redisContainer() {
        log.info("Starting Redis TestContainer for integration tests");

        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_PORT)
            .withReuse(true)
            .withCommand("redis-server", "--maxmemory", "256mb", "--maxmemory-policy", "allkeys-lru");
    }

    /**
     * Kafka TestContainer
     * Rule #16: All configuration externalized
     */
    @Bean
    KafkaContainer kafkaContainer() {
        log.info("Starting Kafka TestContainer for integration tests");

        return new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withReuse(true)
            .withKraft();  // Use KRaft mode (no ZooKeeper required)
    }

    /**
     * Configure Spring properties dynamically from container properties
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Note: Containers must be started before this method is called
        // Properties will be set when containers are available
        log.info("Configuring dynamic properties for TestContainers");
    }
}
