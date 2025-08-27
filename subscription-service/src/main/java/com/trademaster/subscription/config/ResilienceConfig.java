package com.trademaster.subscription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience Configuration
 * 
 * Configures circuit breakers, retries, and timeouts for external service calls.
 * Implements TradeMaster resilience standards for fault tolerance.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    /**
     * Circuit breaker for payment gateway calls
     */
    @Bean
    public CircuitBreaker paymentGatewayCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)                    // Open when 50% of requests fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Stay open for 30 seconds
                .slidingWindowSize(10)                          // Use last 10 requests for calculation
                .minimumNumberOfCalls(5)                        // Minimum calls before opening
                .slowCallRateThreshold(80.0f)                   // Slow call threshold
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls slower than 5s are slow
                .permittedNumberOfCallsInHalfOpenState(3)       // Allow 3 calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("payment-gateway", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Payment Gateway Circuit Breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                .onSuccess(event -> 
                    log.debug("Payment Gateway Circuit Breaker success: duration={}ms", 
                            event.getElapsedDuration().toMillis()))
                .onError(event -> 
                    log.warn("Payment Gateway Circuit Breaker error: {}", 
                            event.getThrowable().getMessage()))
                .onFailureRateExceeded(event -> 
                    log.error("Payment Gateway Circuit Breaker failure rate exceeded: {}%", 
                            event.getFailureRate()));

        return circuitBreaker;
    }

    /**
     * Retry configuration for payment gateway calls
     */
    @Bean
    public Retry paymentGatewayRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                 // Maximum 3 attempts
                .waitDuration(Duration.ofSeconds(1))            // Wait 1 second between attempts
                .exponentialBackoffMultiplier(2.0)              // Exponential backoff
                .retryOnException(throwable -> {
                    // Retry on network errors and 5xx responses
                    return throwable instanceof java.net.ConnectException ||
                           throwable instanceof java.net.SocketTimeoutException ||
                           throwable.getMessage().contains("5");
                })
                .build();

        Retry retry = Retry.of("payment-gateway", config);
        
        retry.getEventPublisher()
                .onRetry(event -> 
                    log.warn("Payment Gateway retry attempt {} due to: {}", 
                            event.getNumberOfRetryAttempts(), 
                            event.getLastThrowable().getMessage()))
                .onSuccess(event -> 
                    log.info("Payment Gateway retry succeeded after {} attempts", 
                            event.getNumberOfRetryAttempts()))
                .onError(event -> 
                    log.error("Payment Gateway retry failed after {} attempts: {}", 
                            event.getNumberOfRetryAttempts(), 
                            event.getLastThrowable().getMessage()));

        return retry;
    }

    /**
     * Time limiter for payment gateway calls
     */
    @Bean
    public TimeLimiter paymentGatewayTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))        // 10 second timeout
                .cancelRunningFuture(true)                      // Cancel if timeout
                .build();

        TimeLimiter timeLimiter = TimeLimiter.of("payment-gateway", config);
        
        timeLimiter.getEventPublisher()
                .onTimeout(event -> 
                    log.warn("Payment Gateway call timed out after {}ms", 
                            event.getTimeLimiterConfig().getTimeoutDuration().toMillis()))
                .onSuccess(event -> 
                    log.debug("Payment Gateway call completed in {}ms", 
                            event.getElapsedDuration().toMillis()));

        return timeLimiter;
    }

    /**
     * Circuit breaker for external API calls (market data, etc.)
     */
    @Bean
    public CircuitBreaker externalApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f)                    // More lenient for external APIs
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer wait for external APIs
                .slidingWindowSize(20)                          // Larger window
                .minimumNumberOfCalls(10)
                .slowCallRateThreshold(90.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("external-api", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("External API Circuit Breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    /**
     * Database circuit breaker for resilience
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(70.0f)                    // Higher threshold for database
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Shorter wait for database
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .slowCallRateThreshold(95.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("database", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.error("Database Circuit Breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()))
                .onError(event -> 
                    log.error("Database Circuit Breaker error: {}", 
                            event.getThrowable().getMessage()));

        return circuitBreaker;
    }

    /**
     * Kafka circuit breaker for event publishing
     */
    @Bean
    public CircuitBreaker kafkaCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(40.0f)                    // Lower threshold for Kafka
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .slowCallRateThreshold(75.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("kafka", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Kafka Circuit Breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    /**
     * Generic retry configuration for general use
     */
    @Bean
    public Retry genericRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .exponentialBackoffMultiplier(1.5)
                .retryOnException(throwable -> {
                    // Retry on transient exceptions
                    return throwable instanceof java.util.concurrent.TimeoutException ||
                           throwable instanceof org.springframework.dao.TransientDataAccessException;
                })
                .build();

        return Retry.of("generic", config);
    }

    /**
     * Generic time limiter for async operations
     */
    @Bean
    public TimeLimiter genericTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiter.of("generic", config);
    }
}