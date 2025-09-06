package com.trademaster.agentos.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ✅ MANDATORY: TestContainers Configuration for Integration Tests
 * 
 * Provides real Redis and PostgreSQL containers for testing
 * Eliminates Redis connection issues in tests
 */
@TestConfiguration
public class TestContainersConfig {
    
    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final String POSTGRES_IMAGE = "postgres:15";
    
    /**
     * ✅ REDIS CONTAINER: Real Redis instance for integration tests
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .withReuse(true);
    }
    
    /**
     * ✅ POSTGRES CONTAINER: Real PostgreSQL instance for integration tests
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName("trademaster_agentos_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);
    }
    
    /**
     * ✅ REDIS CONNECTION: Configure Redis connection for tests
     */
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory(GenericContainer<?> redisContainer) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            redisContainer.getHost(), 
            redisContainer.getMappedPort(6379)
        );
        factory.afterPropertiesSet();
        return factory;
    }
    
    /**
     * ✅ REDIS TEMPLATE: Configured Redis template for tests
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}