package com.trademaster.payment.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Functional Circuit Breaker Utility
 * Provides functional wrappers for circuit breaker operations
 *
 * Compliance:
 * - Rule 3: Functional Programming First - no if-else, functional wrappers
 * - Rule 24: Circuit breaker for ALL external calls
 * - Rule 11: Railway programming with Result types
 * - Rule 12: CompletableFuture with Virtual Threads
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerUtil {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Execute operation with circuit breaker protection
     * Returns Optional for functional error handling
     *
     * @param circuitBreakerName Name of circuit breaker configuration
     * @param operation Supplier to execute
     * @return Optional containing result or empty on failure
     */
    public <T> Optional<T> executeWithCircuitBreaker(
            String circuitBreakerName,
            Supplier<T> operation
    ) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        try {
            T result = circuitBreaker.executeSupplier(operation);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Circuit breaker execution failed: {} | error={}",
                    circuitBreakerName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Execute async operation with circuit breaker protection
     * Returns CompletableFuture for async execution with Virtual Threads
     *
     * @param circuitBreakerName Name of circuit breaker configuration
     * @param operation Supplier to execute
     * @return CompletableFuture containing result or failed future
     */
    public <T> CompletableFuture<T> executeAsyncWithCircuitBreaker(
            String circuitBreakerName,
            Supplier<T> operation
    ) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        return CompletableFuture.supplyAsync(
            circuitBreaker.decorateSupplier(operation)
        ).exceptionally(throwable -> {
            log.error("Async circuit breaker execution failed: {} | error={}",
                    circuitBreakerName, throwable.getMessage(), throwable);
            return null;
        });
    }

    /**
     * Execute operation with fallback
     * Functional pattern for fallback handling
     *
     * @param circuitBreakerName Name of circuit breaker configuration
     * @param operation Primary operation
     * @param fallback Fallback operation
     * @return Result from primary or fallback
     */
    public <T> T executeWithFallback(
            String circuitBreakerName,
            Supplier<T> operation,
            Supplier<T> fallback
    ) {
        return executeWithCircuitBreaker(circuitBreakerName, operation)
            .orElseGet(() -> {
                log.info("Executing fallback for circuit breaker: {}", circuitBreakerName);
                return fallback.get();
            });
    }

    /**
     * Execute operation with default value on failure
     * Simplified fallback pattern
     *
     * @param circuitBreakerName Name of circuit breaker configuration
     * @param operation Operation to execute
     * @param defaultValue Default value on failure
     * @return Result or default value
     */
    public <T> T executeWithDefault(
            String circuitBreakerName,
            Supplier<T> operation,
            T defaultValue
    ) {
        return executeWithCircuitBreaker(circuitBreakerName, operation)
            .orElse(defaultValue);
    }

    /**
     * Check circuit breaker state
     * Functional predicate for state checking
     *
     * @param circuitBreakerName Name of circuit breaker
     * @return true if circuit is closed (healthy), false otherwise
     */
    public boolean isCircuitClosed(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        CircuitBreaker.State state = circuitBreaker.getState();

        return state == CircuitBreaker.State.CLOSED;
    }

    /**
     * Get circuit breaker metrics
     * Functional approach to metrics retrieval
     *
     * @param circuitBreakerName Name of circuit breaker
     * @return Optional containing metrics or empty
     */
    public Optional<CircuitBreaker.Metrics> getCircuitBreakerMetrics(String circuitBreakerName) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
            return Optional.of(circuitBreaker.getMetrics());
        } catch (Exception e) {
            log.error("Failed to get circuit breaker metrics: {}", circuitBreakerName, e);
            return Optional.empty();
        }
    }
}
