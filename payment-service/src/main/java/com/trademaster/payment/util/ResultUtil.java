package com.trademaster.payment.util;

import com.trademaster.common.functional.Result;
import com.trademaster.common.functional.Try;
import com.trademaster.common.functional.Railway;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Result Utility Functions
 * Functional wrappers for common Result operations
 * Delegates to common library Try and Railway utilities
 *
 * Compliance:
 * - Rule 3: Functional Programming - uses Try/Railway from common library
 * - Rule 11: Railway programming patterns
 * - Rule 10: @Slf4j for structured logging
 * - Rule 15: Correlation IDs in error logging
 * - Rule 27: Common library code reuse
 */
@Slf4j
public final class ResultUtil {

    private ResultUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Wrap operation that may throw exception
     * Delegates to Try.of() from common library
     */
    public static <T> Result<T, Throwable> tryExecute(Callable<T> operation) {
        return Try.of(operation::call)
            .onFailure(e -> log.error("Operation failed: {}", e.getMessage(), e))
            .toResult()
            .mapError(e -> (Throwable) e);
    }

    /**
     * Wrap supplier that may throw exception
     * Delegates to Try.of() from common library
     */
    public static <T> Result<T, Throwable> safely(Supplier<T> supplier) {
        return Try.of(supplier::get)
            .onFailure(e -> log.error("Supplier execution failed: {}", e.getMessage(), e))
            .toResult()
            .mapError(e -> (Throwable) e);
    }

    /**
     * Combine two Results
     * Delegates to Railway.combine() from common library
     */
    public static <T, U, E> Result<Pair<T, U>, E> combine(
            Result<T, E> first,
            Result<U, E> second
    ) {
        return Railway.combine(first, second, t -> u -> new Pair<>(t, u));
    }

    /**
     * Simple pair record for combine operation
     */
    public record Pair<T, U>(T first, U second) {}

    /**
     * Execute operation with correlation ID logging
     * Functional error handling with structured logging
     */
    public static <T> Result<T, String> executeWithCorrelation(
            String correlationId,
            String operationName,
            Callable<T> operation
    ) {
        try {
            log.debug("Executing operation: {} | correlation_id={}", operationName, correlationId);
            T result = operation.call();
            log.debug("Operation succeeded: {} | correlation_id={}", operationName, correlationId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Operation failed: {} | correlation_id={} | error={}",
                    operationName, correlationId, e.getMessage(), e);
            return Result.failure("Operation failed: " + e.getMessage());
        }
    }

    /**
     * Validate with predicate
     * Returns failure with error message if validation fails
     */
    public static <T> Result<T, String> validate(
            T value,
            java.util.function.Predicate<T> predicate,
            String errorMessage
    ) {
        return predicate.test(value)
            ? Result.success(value)
            : Result.failure(errorMessage);
    }

    /**
     * Chain validation
     * Accumulates multiple validation failures
     */
    public static <T> Result<T, java.util.List<String>> validateAll(
            T value,
            java.util.function.Predicate<T>... validators
    ) {
        java.util.List<String> errors = java.util.Arrays.stream(validators)
            .filter(validator -> !validator.test(value))
            .map(validator -> "Validation failed")
            .toList();

        return errors.isEmpty()
            ? Result.success(value)
            : Result.failure(errors);
    }

    /**
     * Convert Optional to Result
     * Delegates to Railway.fromOptional() from common library
     */
    public static <T> Result<T, String> fromOptional(
            java.util.Optional<T> optional,
            String errorMessage
    ) {
        return Railway.fromOptional(optional, () -> errorMessage);
    }

    /**
     * Retry operation with exponential backoff
     * Uses functional Stream-based retry with Railway pattern
     * Compliance: Rule 3 - No loops, uses Stream instead
     */
    public static <T> Result<T, String> retryWithBackoff(
            Callable<T> operation,
            int maxAttempts,
            long initialDelayMs
    ) {
        return java.util.stream.Stream.iterate(0, attempt -> attempt < maxAttempts, attempt -> attempt + 1)
            .map(attempt -> attemptWithBackoff(operation, attempt, maxAttempts, initialDelayMs))
            .filter(Try::isSuccess)
            .findFirst()
            .map(Try::toResult)
            .map(result -> result.mapError(Exception::getMessage))
            .orElseGet(() -> Result.failure("Operation failed after " + maxAttempts + " attempts"));
    }

    /**
     * Execute single retry attempt with exponential backoff
     * Helper for retryWithBackoff using functional composition
     */
    private static <T> Try<T> attemptWithBackoff(
            Callable<T> operation,
            int attempt,
            int maxAttempts,
            long initialDelayMs
    ) {
        return Try.of(() -> {
            if (attempt > 0) {
                long delay = initialDelayMs * (1L << (attempt - 1));
                log.warn("Retrying operation (attempt {}/{}), waiting {}ms",
                         attempt + 1, maxAttempts, delay);
                Thread.sleep(delay);
            }
            return operation.call();
        }).onSuccess(result -> {
            if (attempt > 0) {
                log.info("Operation succeeded after {} retries", attempt);
            }
        }).onFailure(e -> {
            if (attempt + 1 >= maxAttempts) {
                log.error("Operation failed after {} attempts", maxAttempts, e);
            }
        });
    }
}
