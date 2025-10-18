package com.trademaster.marketdata.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Circuit Breaker Service - Functional Wrapper for Resilience4j
 *
 * Provides functional programming patterns for circuit breaker usage
 * following Rule #3: Functional Programming First and Rule #25: Circuit Breaker Implementation
 *
 * Features:
 * - Functional composition with circuit breakers
 * - Automatic fallback handling
 * - Metrics collection
 * - Result type integration (ready for functional error handling)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreakerMetrics> metricsCache = new ConcurrentHashMap<>();

    /**
     * Execute operation with circuit breaker protection
     * Functional pattern: decorateSupplier
     *
     * @param circuitBreakerName Circuit breaker identifier
     * @param operation Supplier to execute
     * @param <T> Return type
     * @return CompletableFuture with result
     */
    public <T> CompletableFuture<T> executeWithCircuitBreaker(
            String circuitBreakerName,
            Supplier<T> operation) {

        return CompletableFuture.supplyAsync(() -> {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

            Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                operation
            );

            try {
                T result = decoratedSupplier.get();
                recordSuccess(circuitBreakerName);
                return result;
            } catch (Exception e) {
                recordFailure(circuitBreakerName, e);
                throw e;
            }
        });
    }

    /**
     * Execute operation with circuit breaker and fallback
     * Functional pattern: decorateSupplier with recovery (Rule #3: Pattern matching)
     *
     * @param circuitBreakerName Circuit breaker identifier
     * @param operation Primary operation
     * @param fallback Fallback operation if circuit is open
     * @param <T> Return type
     * @return CompletableFuture with result
     */
    public <T> CompletableFuture<T> executeWithFallback(
            String circuitBreakerName,
            Supplier<T> operation,
            Supplier<T> fallback) {

        return CompletableFuture.supplyAsync(() -> {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

            // Rule #3: Use pattern matching instead of if-else
            return switch (circuitBreaker.getState()) {
                case OPEN -> {
                    log.warn("Circuit breaker {} is OPEN, using fallback", circuitBreakerName);
                    yield fallback.get();
                }
                case CLOSED, HALF_OPEN, DISABLED, METRICS_ONLY, FORCED_OPEN -> {
                    Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        operation
                    );

                    try {
                        T result = decoratedSupplier.get();
                        recordSuccess(circuitBreakerName);
                        yield result;
                    } catch (Exception e) {
                        recordFailure(circuitBreakerName, e);
                        log.error("Circuit breaker {} operation failed, using fallback: {}",
                            circuitBreakerName, e.getMessage());
                        yield fallback.get();
                    }
                }
            };
        });
    }

    /**
     * Execute NSE API call with circuit breaker
     */
    public <T> CompletableFuture<T> executeNSECall(Supplier<T> operation) {
        return executeWithCircuitBreaker("nseDataProvider", operation);
    }

    /**
     * Execute NSE API call with fallback
     */
    public <T> CompletableFuture<T> executeNSECallWithFallback(
            Supplier<T> operation,
            Supplier<T> fallback) {
        return executeWithFallback("nseDataProvider", operation, fallback);
    }

    /**
     * Execute BSE API call with circuit breaker
     */
    public <T> CompletableFuture<T> executeBSECall(Supplier<T> operation) {
        return executeWithCircuitBreaker("bseDataProvider", operation);
    }

    /**
     * Execute BSE API call with fallback
     */
    public <T> CompletableFuture<T> executeBSECallWithFallback(
            Supplier<T> operation,
            Supplier<T> fallback) {
        return executeWithFallback("bseDataProvider", operation, fallback);
    }

    /**
     * Execute Alpha Vantage API call with circuit breaker
     */
    public <T> CompletableFuture<T> executeAlphaVantageCall(Supplier<T> operation) {
        return executeWithCircuitBreaker("alphaVantageProvider", operation);
    }

    /**
     * Execute database operation with circuit breaker
     */
    public <T> CompletableFuture<T> executeDatabaseOperation(Supplier<T> operation) {
        return executeWithCircuitBreaker("database", operation);
    }

    /**
     * Execute database operation with fallback
     */
    public <T> CompletableFuture<T> executeDatabaseOperationWithFallback(
            Supplier<T> operation,
            Supplier<T> fallback) {
        return executeWithFallback("database", operation, fallback);
    }

    /**
     * Execute InfluxDB operation with circuit breaker
     */
    public <T> CompletableFuture<T> executeInfluxDBOperation(Supplier<T> operation) {
        return executeWithCircuitBreaker("influxDb", operation);
    }

    /**
     * Execute Kafka operation with circuit breaker
     */
    public <T> CompletableFuture<T> executeKafkaOperation(Supplier<T> operation) {
        return executeWithCircuitBreaker("kafka", operation);
    }

    /**
     * Execute Redis cache operation with circuit breaker
     */
    public <T> CompletableFuture<T> executeRedisCacheOperation(Supplier<T> operation) {
        return executeWithCircuitBreaker("redisCache", operation);
    }

    /**
     * Execute Redis cache operation with fallback
     */
    public <T> CompletableFuture<T> executeRedisCacheOperationWithFallback(
            Supplier<T> operation,
            Supplier<T> fallback) {
        return executeWithFallback("redisCache", operation, fallback);
    }

    /**
     * Get circuit breaker status for all registered circuit breakers
     */
    public Map<String, CircuitBreakerStatus> getAllCircuitBreakerStatus() {
        Map<String, CircuitBreakerStatus> statusMap = new ConcurrentHashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreakerStatus status = new CircuitBreakerStatus(
                cb.getName(),
                cb.getState().toString(),
                cb.getMetrics().getNumberOfSuccessfulCalls(),
                cb.getMetrics().getNumberOfFailedCalls(),
                cb.getMetrics().getFailureRate(),
                cb.getMetrics().getNumberOfNotPermittedCalls()
            );
            statusMap.put(cb.getName(), status);
        });

        return statusMap;
    }

    /**
     * Get specific circuit breaker status
     */
    public CircuitBreakerStatus getCircuitBreakerStatus(String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);

        return new CircuitBreakerStatus(
            cb.getName(),
            cb.getState().toString(),
            cb.getMetrics().getNumberOfSuccessfulCalls(),
            cb.getMetrics().getNumberOfFailedCalls(),
            cb.getMetrics().getFailureRate(),
            cb.getMetrics().getNumberOfNotPermittedCalls()
        );
    }

    /**
     * Check if circuit breaker is healthy (CLOSED state)
     * Rule #3: Pattern matching instead of == comparison
     */
    public boolean isCircuitBreakerHealthy(String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        return switch (cb.getState()) {
            case CLOSED -> true;
            case OPEN, HALF_OPEN, DISABLED, METRICS_ONLY, FORCED_OPEN -> false;
        };
    }

    /**
     * Reset circuit breaker to CLOSED state
     * Use with caution - only for manual recovery
     */
    public void resetCircuitBreaker(String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.reset();
        log.warn("Circuit breaker {} manually reset to CLOSED state", name);
    }

    // Private helper methods
    private void recordSuccess(String circuitBreakerName) {
        CircuitBreakerMetrics metrics = metricsCache.computeIfAbsent(
            circuitBreakerName,
            k -> new CircuitBreakerMetrics(circuitBreakerName)
        );
        metrics.recordSuccess();
    }

    private void recordFailure(String circuitBreakerName, Exception e) {
        CircuitBreakerMetrics metrics = metricsCache.computeIfAbsent(
            circuitBreakerName,
            k -> new CircuitBreakerMetrics(circuitBreakerName)
        );
        metrics.recordFailure(e.getClass().getSimpleName());
    }

    // Inner classes for metrics
    private static class CircuitBreakerMetrics {
        private final String name;
        private long successCount = 0;
        private long failureCount = 0;
        private final Map<String, Long> failuresByType = new ConcurrentHashMap<>();

        CircuitBreakerMetrics(String name) {
            this.name = name;
        }

        void recordSuccess() {
            successCount++;
        }

        void recordFailure(String exceptionType) {
            failureCount++;
            failuresByType.merge(exceptionType, 1L, Long::sum);
        }
    }

    /**
     * Circuit Breaker Status Record (Rule #9: Records for immutability)
     * Rule #3: Pattern matching for state checks
     */
    public record CircuitBreakerStatus(
        String name,
        String state,
        long successfulCalls,
        long failedCalls,
        float failureRate,
        long notPermittedCalls
    ) {
        public boolean isHealthy() {
            return switch (state) {
                case "CLOSED" -> true;
                case "OPEN", "HALF_OPEN" -> false;
                default -> false;
            };
        }

        public boolean isOpen() {
            return switch (state) {
                case "OPEN" -> true;
                case "CLOSED", "HALF_OPEN" -> false;
                default -> false;
            };
        }

        public boolean isHalfOpen() {
            return switch (state) {
                case "HALF_OPEN" -> true;
                case "CLOSED", "OPEN" -> false;
                default -> false;
            };
        }
    }
}
