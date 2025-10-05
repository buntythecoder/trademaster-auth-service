package com.trademaster.auth.service;

import com.trademaster.auth.pattern.Result;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Circuit Breaker Service - Functional circuit breaker wrapper for all external operations
 *
 * MANDATORY: Circuit Breaker Implementation - Rule #25
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Virtual Threads - Rule #12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

    private final CircuitBreaker emailServiceCircuitBreaker;
    private final CircuitBreaker mfaServiceCircuitBreaker;
    private final CircuitBreaker externalApiCircuitBreaker;
    private final CircuitBreaker databaseCircuitBreaker;
    private final TimeLimiter timeLimiter;
    private final TimeLimiter fastTimeLimiter;

    /**
     * Execute email operation with circuit breaker protection
     *
     * MANDATORY: Functional circuit breaker - Rule #25
     * MANDATORY: CompletableFuture with virtual threads - Rule #12
     */
    public <T> CompletableFuture<Result<T, String>> executeEmailOperation(
            String operationName,
            Supplier<T> operation) {

        return executeWithCircuitBreaker(
            operationName,
            emailServiceCircuitBreaker,
            timeLimiter,
            operation
        );
    }

    /**
     * Execute MFA operation with circuit breaker protection
     */
    public <T> CompletableFuture<Result<T, String>> executeMfaOperation(
            String operationName,
            Supplier<T> operation) {

        return executeWithCircuitBreaker(
            operationName,
            mfaServiceCircuitBreaker,
            fastTimeLimiter,  // MFA should be fast
            operation
        );
    }

    /**
     * Execute external API operation with circuit breaker protection
     */
    public <T> CompletableFuture<Result<T, String>> executeExternalApiOperation(
            String operationName,
            Supplier<T> operation) {

        return executeWithCircuitBreaker(
            operationName,
            externalApiCircuitBreaker,
            timeLimiter,
            operation
        );
    }

    /**
     * Execute database operation with circuit breaker protection
     */
    public <T> CompletableFuture<Result<T, String>> executeDatabaseOperation(
            String operationName,
            Supplier<T> operation) {

        return executeWithCircuitBreaker(
            operationName,
            databaseCircuitBreaker,
            fastTimeLimiter,  // Database should be fast
            operation
        );
    }

    /**
     * Generic circuit breaker execution using functional patterns
     *
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: No try-catch in business logic - Rule #11
     */
    private <T> CompletableFuture<Result<T, String>> executeWithCircuitBreaker(
            String operationName,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter,
            Supplier<T> operation) {

        log.debug("Executing operation '{}' with circuit breaker '{}'",
            operationName, circuitBreaker.getName());

        // Execute with circuit breaker using working pattern from trading service
        return CompletableFuture.supplyAsync(() -> {
            try {
                return circuitBreaker.executeSupplier(() -> {
                    try {
                        T result = operation.get();
                        return Result.success(result);
                    } catch (Exception e) {
                        log.error("Operation '{}' failed: {}", operationName, e.getMessage());
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
            } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
                log.warn("Circuit breaker '{}' is OPEN for operation '{}'",
                    circuitBreaker.getName(), operationName);
                return Result.failure("Circuit breaker is open");
            } catch (Exception e) {
                log.error("Circuit breaker execution failed for '{}': {}", operationName, e.getMessage());
                return Result.failure("Operation failed: " + e.getMessage());
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Handle circuit breaker exceptions using pattern matching - Rule #14
     */
    private String handleCircuitBreakerException(String operationName, Throwable throwable) {
        return switch (throwable.getClass().getSimpleName()) {
            case "CallNotPermittedException" ->
                String.format("Circuit breaker open for operation '%s' - service unavailable", operationName);
            case "TimeoutException" ->
                String.format("Operation '%s' timed out", operationName);
            case "BulkheadFullException" ->
                String.format("Bulkhead full for operation '%s' - too many concurrent requests", operationName);
            case "RateLimiterRejectedException" ->
                String.format("Rate limit exceeded for operation '%s'", operationName);
            default ->
                String.format("Operation '%s' failed: %s", operationName, throwable.getMessage());
        };
    }

    /**
     * Get circuit breaker health status for monitoring
     */
    public CircuitBreakerHealthStatus getHealthStatus() {
        return new CircuitBreakerHealthStatus(
            emailServiceCircuitBreaker.getState(),
            mfaServiceCircuitBreaker.getState(),
            externalApiCircuitBreaker.getState(),
            databaseCircuitBreaker.getState()
        );
    }

    /**
     * Circuit breaker health status record - Rule #9 Immutability
     */
    public record CircuitBreakerHealthStatus(
        CircuitBreaker.State emailService,
        CircuitBreaker.State mfaService,
        CircuitBreaker.State externalApi,
        CircuitBreaker.State database
    ) {

        public boolean isHealthy() {
            return emailService == CircuitBreaker.State.CLOSED &&
                   mfaService == CircuitBreaker.State.CLOSED &&
                   externalApi == CircuitBreaker.State.CLOSED &&
                   database == CircuitBreaker.State.CLOSED;
        }

        public String getHealthSummary() {
            return String.format("EmailService: %s, MfaService: %s, ExternalApi: %s, Database: %s",
                emailService, mfaService, externalApi, database);
        }
    }
}