package com.trademaster.payment;

import com.trademaster.payment.config.TestKafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TestContainer-based Integration Test for Payment Service
 *
 * Uses PostgreSQL TestContainer to test against real database.
 * This ensures PostgreSQL-specific queries and features work correctly.
 *
 * MANDATORY: Spring Boot 3.5+ Application Context Test with TestContainers
 * MANDATORY: Real database integration testing
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.consul.enabled=false",
        "spring.cloud.consul.discovery.enabled=false",
        "spring.cloud.consul.config.enabled=false",
        "spring.kafka.enabled=false",
        "spring.flyway.enabled=false"
    }
)
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@Testcontainers
class PaymentServiceApplicationTest {

    /**
     * PostgreSQL TestContainer
     * Automatically starts PostgreSQL in Docker before tests run
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * Configure Spring to use TestContainer database
     * Dynamically sets datasource properties from the running container
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    /**
     * Verify that the Spring Boot application context loads successfully with real PostgreSQL.
     * This test ensures:
     * - All beans are properly configured
     * - No circular dependencies exist
     * - Database connections can be established with real PostgreSQL
     * - PostgreSQL-specific queries are valid
     * - Security configuration is valid
     */
    @Test
    void contextLoads() {
        // This test will fail if the application context cannot be loaded
        // It validates the entire Spring configuration and dependency injection
        // with a real PostgreSQL database via TestContainers
    }

    /**
     * Verify that the application can start with test profile and real database.
     * This ensures test-specific configuration works with PostgreSQL TestContainer.
     */
    @Test
    void applicationStartsWithTestProfile() {
        // Spring Boot test will verify application starts successfully
        // with the test profile active and PostgreSQL TestContainer running
    }
}