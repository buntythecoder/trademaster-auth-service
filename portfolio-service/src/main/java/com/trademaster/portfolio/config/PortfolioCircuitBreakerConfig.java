package com.trademaster.portfolio.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Circuit Breaker Configuration for Portfolio Service
 * 
 * Implements Rule #25 (Circuit Breaker Implementation) - MANDATORY resilience patterns.
 * 
 * Circuit breakers for all external operations:
 * - Database operations with high latency risk
 * - External API calls (broker APIs, market data)
 * - Message queue operations
 * - File I/O operations
 * - Network operations
 * 
 * Features:
 * - Functional circuit breaker with CompletableFuture
 * - Resilience4j Spring Boot integration
 * - Meaningful fallback strategies
 * - Metrics and monitoring with correlation IDs
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@RequiredArgsConstructor
@Slf4j
public class PortfolioCircuitBreakerConfig {
    
    private static final String DATABASE_CB = "database";
    private static final String MARKET_DATA_CB = "marketData";
    private static final String BROKER_API_CB = "brokerApi";
    private static final String MESSAGE_QUEUE_CB = "messageQueue";
    private static final String FILE_IO_CB = "fileIo";
    
    private final CircuitBreakerProperties circuitBreakerProperties;
    
    /**
     * Circuit Breaker Registry with default configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
            .waitDurationInOpenState(circuitBreakerProperties.getWaitDurationInOpenState())
            .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
            .minimumNumberOfCalls(circuitBreakerProperties.getMinimumNumberOfCalls())
            .slowCallRateThreshold(circuitBreakerProperties.getSlowCallRateThreshold())
            .slowCallDurationThreshold(circuitBreakerProperties.getSlowCallDurationThreshold())
            .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getPermittedCallsInHalfOpenState())
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
        
        return CircuitBreakerRegistry.of(defaultConfig);
    }
    
    /**
     * Database Circuit Breaker - for critical database operations
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig dbConfig = CircuitBreakerConfig.from(registry.getDefaultConfig())
            .failureRateThreshold(70.0f) // Higher threshold for database
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slowCallDurationThreshold(Duration.ofMillis(2000)) // 2s for DB operations
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker(DATABASE_CB, dbConfig);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Database Circuit Breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event -> 
                log.error("Database Circuit Breaker failure rate exceeded: {}%", 
                    event.getFailureRate()))
            .onSlowCallRateExceeded(event -> 
                log.warn("Database Circuit Breaker slow call rate exceeded: {}%", 
                    event.getSlowCallRate()));
        
        return circuitBreaker;
    }
    
    /**
     * Market Data Circuit Breaker - for external market data calls
     */
    @Bean
    public CircuitBreaker marketDataCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig marketDataConfig = CircuitBreakerConfig.from(registry.getDefaultConfig())
            .failureRateThreshold(60.0f) // More sensitive for external APIs
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slowCallDurationThreshold(Duration.ofMillis(5000)) // 5s for external APIs
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker(MARKET_DATA_CB, marketDataConfig);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Market Data Circuit Breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event -> 
                log.error("Market Data Circuit Breaker call not permitted - using cached data"));
        
        return circuitBreaker;
    }
    
    /**
     * Broker API Circuit Breaker - for broker integration calls
     */
    @Bean
    public CircuitBreaker brokerApiCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig brokerConfig = CircuitBreakerConfig.from(registry.getDefaultConfig())
            .failureRateThreshold(50.0f) // Very sensitive for broker APIs
            .waitDurationInOpenState(Duration.ofSeconds(120))
            .slowCallDurationThreshold(Duration.ofMillis(10000)) // 10s for broker APIs
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker(BROKER_API_CB, brokerConfig);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.error("Broker API Circuit Breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event -> 
                log.error("Broker API Circuit Breaker call not permitted - broker unavailable"));
        
        return circuitBreaker;
    }
    
    /**
     * Message Queue Circuit Breaker - for Kafka operations
     */
    @Bean
    public CircuitBreaker messageQueueCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig mqConfig = CircuitBreakerConfig.from(registry.getDefaultConfig())
            .failureRateThreshold(80.0f) // Higher threshold for message queues
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .slowCallDurationThreshold(Duration.ofMillis(3000)) // 3s for message operations
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker(MESSAGE_QUEUE_CB, mqConfig);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Message Queue Circuit Breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()));
        
        return circuitBreaker;
    }
    
    /**
     * File I/O Circuit Breaker - for file system operations
     */
    @Bean
    public CircuitBreaker fileIoCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig fileConfig = CircuitBreakerConfig.from(registry.getDefaultConfig())
            .failureRateThreshold(75.0f)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .slowCallDurationThreshold(Duration.ofMillis(1000)) // 1s for file operations
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker(FILE_IO_CB, fileConfig);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("File I/O Circuit Breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState()));
        
        return circuitBreaker;
    }
    
    /**
     * Retry Registry for failed operations
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultRetryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2.0))
            .build();
        
        return RetryRegistry.of(defaultRetryConfig);
    }
    
    /**
     * Time Limiter for operation timeouts
     */
    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .build();
        
        return TimeLimiter.of(config);
    }
}