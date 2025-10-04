package com.trademaster.auth.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for External Services
 *
 * MANDATORY: Circuit Breaker Implementation - Rule #25
 * MANDATORY: External service protection for all I/O operations
 * MANDATORY: Functional circuit breaker patterns with CompletableFuture
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    /**
     * Circuit Breaker Registry with default configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)               // Open when 50% failures
            .waitDurationInOpenState(Duration.ofSeconds(30))  // Wait 30s before half-open
            .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)                     // Last 10 calls
            .minimumNumberOfCalls(5)                   // Minimum 5 calls to calculate rate
            .slowCallRateThreshold(50.0f)              // 50% slow calls trigger open
            .slowCallDurationThreshold(Duration.ofSeconds(2))  // >2s is slow
            .permittedNumberOfCallsInHalfOpenState(3)  // 3 test calls in half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Circuit breaker registry configured

        return registry;
    }

    /**
     * Email Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker emailServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig emailConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(60.0f)               // Email can tolerate more failures
            .waitDurationInOpenState(Duration.ofMinutes(2))  // Longer wait for email
            .slidingWindowSize(20)                     // More calls for email assessment
            .minimumNumberOfCalls(10)
            .slowCallDurationThreshold(Duration.ofSeconds(5))  // Email can be slower
            .build();

        return registry.circuitBreaker("emailService", emailConfig);
    }

    /**
     * MFA Service Circuit Breaker (TOTP/SMS)
     */
    @Bean
    public CircuitBreaker mfaServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig mfaConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(40.0f)               // MFA must be highly available
            .waitDurationInOpenState(Duration.ofSeconds(15))  // Quick recovery for MFA
            .slidingWindowSize(15)
            .minimumNumberOfCalls(5)
            .slowCallDurationThreshold(Duration.ofSeconds(3))  // MFA should be fast
            .build();

        return registry.circuitBreaker("mfaService", mfaConfig);
    }

    /**
     * External API Circuit Breaker (Social Auth, KMS, etc.)
     */
    @Bean
    public CircuitBreaker externalApiCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig externalConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(30.0f)               // External APIs must be reliable
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .slidingWindowSize(20)
            .minimumNumberOfCalls(8)
            .slowCallDurationThreshold(Duration.ofSeconds(4))
            .build();

        return registry.circuitBreaker("externalApi", externalConfig);
    }

    /**
     * Database Circuit Breaker
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig dbConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .failureRateThreshold(25.0f)               // Database must be highly available
            .waitDurationInOpenState(Duration.ofSeconds(10))  // Quick recovery
            .slidingWindowSize(30)                     // More samples for DB
            .minimumNumberOfCalls(15)
            .slowCallDurationThreshold(Duration.ofMillis(500))  // DB should be fast
            .build();

        return registry.circuitBreaker("database", dbConfig);
    }

    /**
     * Time Limiter for async operations
     */
    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))    // 10s timeout for all operations
            .cancelRunningFuture(true)                  // Cancel on timeout
            .build();

        return TimeLimiter.of("defaultTimeLimiter", config);
    }

    /**
     * Fast Time Limiter for critical operations
     */
    @Bean
    public TimeLimiter fastTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))     // 3s timeout for critical ops
            .cancelRunningFuture(true)
            .build();

        return TimeLimiter.of("fastTimeLimiter", config);
    }
}