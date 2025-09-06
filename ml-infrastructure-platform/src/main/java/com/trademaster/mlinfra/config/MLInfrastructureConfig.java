package com.trademaster.mlinfra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ML Infrastructure Configuration
 * 
 * Configures core infrastructure components:
 * - Object storage (MinIO)
 * - Caching (Redis)
 * - JSON serialization
 * - Virtual thread executors
 * - ML platform integration
 */
@Slf4j
@Configuration
@EnableCaching
@EnableAsync
@EnableConfigurationProperties({
    MLInfrastructureProperties.class,
    MLflowProperties.class,
    FeatureStoreProperties.class
})
public class MLInfrastructureConfig {

    /**
     * Primary ObjectMapper for JSON serialization/deserialization
     * Configured for ML data structures and temporal types
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .findAndRegisterModules();
    }

    /**
     * MinIO client for S3-compatible object storage
     * Used for model artifacts, datasets, and experiment logs
     */
    @Bean
    public MinioClient minioClient(MLInfrastructureProperties properties) {
        var minio = properties.minio();
        
        log.info("Configuring MinIO client - endpoint: {}, bucket: {}", 
                minio.endpoint(), minio.bucket());
        
        return MinioClient.builder()
            .endpoint(minio.endpoint())
            .credentials(minio.accessKey(), minio.secretKey())
            .build();
    }

    /**
     * Redis template for feature store and caching
     * Optimized for ML feature serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);
        
        var stringSerializer = new StringRedisSerializer();
        var jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        log.info("Redis template configured for ML feature storage");
        
        return template;
    }

    /**
     * Redis cache manager for application-level caching
     * Configured with appropriate TTL for ML operations
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var cacheConfiguration = org.springframework.data.redis.cache.RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()))
            );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build();
    }

    /**
     * Virtual thread executor for ML operations
     * Optimized for high-throughput I/O intensive tasks
     */
    @Bean("mlExecutor")
    public Executor mlExecutor() {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Virtual thread executor configured for ML operations");
        return executor;
    }

    /**
     * High-performance executor for model inference
     * Uses virtual threads for optimal concurrency
     */
    @Bean("inferenceExecutor")
    public Executor inferenceExecutor() {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Virtual thread executor configured for model inference");
        return executor;
    }

    /**
     * Batch processing executor for training pipelines
     * Platform threads for CPU-intensive ML training
     */
    @Bean("trainingExecutor")
    public ThreadPoolTaskExecutor trainingExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ml-training-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        log.info("Training executor configured - core: {}, max: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * Feature computation executor
     * Virtual threads for feature store operations
     */
    @Bean("featureExecutor")
    public Executor featureExecutor() {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Virtual thread executor configured for feature computation");
        return executor;
    }
}