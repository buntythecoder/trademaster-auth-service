package com.trademaster.payment.config;

import com.trademaster.payment.repository.WebhookLogRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test Configuration for External Dependencies
 *
 * Provides mock beans for testing when external services are disabled.
 * Includes mock KafkaTemplate and WebhookLogRepository to bypass PostgreSQL-specific queries.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@TestConfiguration
public class TestKafkaConfig {

    /**
     * Provide mock KafkaTemplate for tests
     * This allows services to be created even when Kafka autoconfiguration is disabled
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    /**
     * Provide mock WebhookLogRepository for tests
     * This bypasses PostgreSQL-specific HQL queries that don't work with H2
     */
    @Bean
    @Primary
    public WebhookLogRepository webhookLogRepository() {
        return mock(WebhookLogRepository.class);
    }
}
