package com.trademaster.auth.pattern;

import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Service Operations Utilities for Functional Service Layer
 * 
 * Provides common functional patterns for service layer operations
 * following TradeMaster Advanced Design Patterns standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
public final class ServiceOperations {

    private ServiceOperations() {}
    
    /**
     * Execute service operation with functional error handling
     */
    public static <T> Result<T, String> execute(String operationName, Supplier<T> operation) {
        return SafeOperations.safelyToResult(() -> {
            log.debug("Executing operation: {}", operationName);
            T result = operation.get();
            log.debug("Operation completed successfully: {}", operationName);
            return result;
        }).mapError(error -> {
            log.error("Operation failed: {} - {}", operationName, error);
            return error;
        });
    }
    
    /**
     * Execute async service operation
     */
    public static <T> CompletableFuture<Result<T, String>> executeAsync(String operationName, Supplier<T> operation) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() -> execute(operationName, operation));
    }
    
    /**
     * Conditional execution based on predicate - replaces if-else
     */
    public static <T> Function<T, T> when(java.util.function.Predicate<T> condition, Function<T, T> action) {
        return input -> condition.test(input) ? action.apply(input) : input;
    }
    
    /**
     * Optional-based conditional execution
     */
    public static <T> Function<T, T> whenPresent(Function<T, Optional<?>> extractor, Function<T, T> action) {
        return input -> extractor.apply(input).isPresent() ? action.apply(input) : input;
    }
    
    /**
     * Validation pipeline for service inputs
     */
    public static <T> Result<T, String> validate(T input, ValidationChain<T> validation) {
        return validation.validate(input);
    }
    
    /**
     * Transform service result with error preservation
     */
    public static <T, R> Function<Result<T, String>, Result<R, String>> transform(Function<T, R> mapper) {
        return result -> result.map(mapper);
    }
    
    /**
     * Chain service operations with functional composition
     */
    @SafeVarargs
    public static <T, R> Function<T, Result<R, String>> chain(Function<T, Result<T, String>>... operations) {
        return input -> {
            Result<T, String> current = Result.success(input);
            for (Function<T, Result<T, String>> operation : operations) {
                if (current.isFailure()) break;
                current = current.flatMap(operation);
            }
            return current.map(value -> (R) value);
        };
    }
    
    /**
     * Fallback execution with alternative operation
     */
    public static <T> Supplier<T> withFallback(Supplier<T> primary, Supplier<T> fallback) {
        return () -> SafeOperations.safely(primary).orElseGet(fallback);
    }
    
    /**
     * Retry operation with exponential backoff
     */
    public static <T> CompletableFuture<Optional<T>> retryAsync(String operationName, Supplier<T> operation, int maxAttempts) {
        return retryAsyncInternal(operationName, operation, 0, maxAttempts, 1000L);
    }
    
    private static <T> CompletableFuture<Optional<T>> retryAsyncInternal(
            String operationName, Supplier<T> operation, int currentAttempt, int maxAttempts, long delayMs) {
        
        return VirtualThreadFactory.INSTANCE.supplyAsync(() -> SafeOperations.safely(operation))
            .thenCompose(result -> {
                if (result.isPresent() || currentAttempt >= maxAttempts - 1) {
                    return CompletableFuture.completedFuture(result);
                }
                
                log.warn("Operation failed (attempt {}/{}): {}", currentAttempt + 1, maxAttempts, operationName);
                
                return VirtualThreadFactory.INSTANCE.supplyAsync(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return Optional.<T>empty();
                    }
                    return Optional.<T>empty();
                }).thenCompose(ignored -> 
                    retryAsyncInternal(operationName, operation, currentAttempt + 1, maxAttempts, delayMs * 2)
                );
            });
    }
}