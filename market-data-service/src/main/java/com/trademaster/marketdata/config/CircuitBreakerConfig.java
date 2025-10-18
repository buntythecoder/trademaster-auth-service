package com.trademaster.marketdata.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for Market Data Service
 *
 * MANDATORY Implementation per Rule #25: Circuit Breaker Implementation
 *
 * Implements circuit breakers for:
 * - External API calls (NSE, BSE, Alpha Vantage)
 * - Database operations (PostgreSQL, InfluxDB)
 * - Message queue operations (Kafka)
 * - Cache operations (Redis)
 *
 * Circuit Breaker Pattern:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failures exceeded threshold, requests fail fast
 * - HALF_OPEN: Testing if service recovered, limited requests allowed
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerConfig {

    private final CircuitBreakerProperties properties;

    /**
     * Circuit Breaker Registry
     * Central registry for all circuit breakers with default configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("Initializing Circuit Breaker Registry with properties: {}", properties);

        return CircuitBreakerRegistry.of(
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getFailureRateThreshold())
                .slowCallRateThreshold(properties.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(properties.getSlowCallDurationMs()))
                .waitDurationInOpenState(Duration.ofMillis(properties.getWaitDurationInOpenStateMs()))
                .slidingWindowSize(properties.getSlidingWindowSize())
                .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                    java.io.IOException.class,
                    java.net.SocketTimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .ignoreExceptions(
                    IllegalArgumentException.class,
                    IllegalStateException.class
                )
                .build()
        );
    }

    /**
     * Circuit Breaker for NSE Data Provider
     * Protects against NSE API failures and timeouts
     */
    @Bean
    public CircuitBreaker nseDataProviderCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("nseDataProvider");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("NSE Data Provider Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("NSE Data Provider Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onSuccess(event ->
                log.trace("NSE Data Provider Circuit Breaker recorded success"))
            .onCallNotPermitted(event ->
                log.warn("NSE Data Provider Circuit Breaker blocked call - Circuit is OPEN"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for BSE Data Provider
     * Protects against BSE API failures and timeouts
     */
    @Bean
    public CircuitBreaker bseDataProviderCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("bseDataProvider");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("BSE Data Provider Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("BSE Data Provider Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.warn("BSE Data Provider Circuit Breaker blocked call - Circuit is OPEN"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Alpha Vantage Data Provider
     * Protects against Alpha Vantage API failures and rate limits
     */
    @Bean
    public CircuitBreaker alphaVantageProviderCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("alphaVantageProvider");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Alpha Vantage Provider Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Alpha Vantage Provider Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.warn("Alpha Vantage Provider Circuit Breaker blocked call - Circuit is OPEN"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Database Operations
     * Protects against PostgreSQL connection and query failures
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("database");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.error("Database Circuit Breaker state changed: {} -> {} - DATABASE CONNECTIVITY ISSUE",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Database Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.error("Database Circuit Breaker blocked call - DATABASE UNAVAILABLE"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for InfluxDB Time-Series Operations
     * Protects against InfluxDB write/read failures
     */
    @Bean
    public CircuitBreaker influxDbCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("influxDb");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("InfluxDB Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("InfluxDB Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.warn("InfluxDB Circuit Breaker blocked call - InfluxDB UNAVAILABLE"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Kafka Message Publishing
     * Protects against Kafka connectivity and publishing failures
     */
    @Bean
    public CircuitBreaker kafkaCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("kafka");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Kafka Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Kafka Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.warn("Kafka Circuit Breaker blocked call - Kafka UNAVAILABLE"));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Redis Cache Operations
     * Protects against Redis connectivity failures
     */
    @Bean
    public CircuitBreaker redisCacheCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("redisCache");

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Redis Cache Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.error("Redis Cache Circuit Breaker recorded error: {}",
                    event.getThrowable().getMessage()))
            .onCallNotPermitted(event ->
                log.warn("Redis Cache Circuit Breaker blocked call - Redis UNAVAILABLE"));

        return circuitBreaker;
    }

    /**
     * Time Limiter for API Calls
     * Enforces maximum duration for external API calls
     */
    @Bean
    public TimeLimiter apiCallTimeLimiter() {
        return TimeLimiterRegistry.ofDefaults()
            .timeLimiter("apiCall", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build());
    }

    /**
     * Time Limiter for Database Operations
     * Enforces maximum duration for database queries
     */
    @Bean
    public TimeLimiter databaseTimeLimiter() {
        return TimeLimiterRegistry.ofDefaults()
            .timeLimiter("database", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build());
    }
}
