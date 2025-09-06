package com.trademaster.userprofile.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for User Profile Service
 * 
 * MANDATORY: Circuit Breaker Implementation - Rule #25
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Dynamic Configuration - Rule #16
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Configuration
public class CircuitBreakerConfig {

    @Value("${trademaster.user-profile.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;
    
    @Value("${trademaster.user-profile.circuit-breaker.wait-duration:60}")
    private int waitDurationInOpenState;
    
    @Value("${trademaster.user-profile.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;
    
    @Value("${trademaster.user-profile.circuit-breaker.minimum-calls:5}")
    private int minimumNumberOfCalls;

    /**
     * File Storage Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer fileStorageCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("file-storage", builder -> {
            log.info("Configuring file-storage circuit breaker with failure rate: {}%, wait duration: {}s", 
                failureRateThreshold, waitDurationInOpenState);
            
            builder
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .automaticTransitionFromOpenToHalfOpenEnabled(true);
        });
    }

    /**
     * Database Operations Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer databaseCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("database", builder -> {
            log.info("Configuring database circuit breaker with failure rate: {}%, wait duration: {}s", 
                failureRateThreshold, waitDurationInOpenState);
            
            builder
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .automaticTransitionFromOpenToHalfOpenEnabled(true);
        });
    }

    /**
     * Kafka Operations Circuit Breaker Configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer kafkaCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("kafka", builder -> {
            log.info("Configuring kafka circuit breaker with failure rate: {}%, wait duration: {}s", 
                failureRateThreshold, waitDurationInOpenState);
            
            builder
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .automaticTransitionFromOpenToHalfOpenEnabled(true);
        });
    }

    /**
     * Circuit Breaker Registry for programmatic access
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}