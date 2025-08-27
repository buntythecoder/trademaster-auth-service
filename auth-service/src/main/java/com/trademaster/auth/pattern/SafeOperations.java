package com.trademaster.auth.pattern;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Safe Operations for Functional Error Handling
 * 
 * Eliminates try-catch blocks in business logic by wrapping operations
 * in functional constructs following TradeMaster standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class SafeOperations {

    private SafeOperations() {}
    
    /**
     * Safely execute an operation returning Optional
     */
    public static <T> Optional<T> safely(Supplier<T> operation) {
        try {
            return Optional.ofNullable(operation.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Safely execute an operation returning Result
     */
    public static <T> Result<T, String> safelyToResult(Supplier<T> operation) {
        try {
            T result = operation.get();
            return result != null ? Result.success(result) : Result.failure("Operation returned null");
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }
    
    /**
     * Safely execute an operation asynchronously
     */
    public static <T> CompletableFuture<Optional<T>> safelyAsync(Supplier<T> operation) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() -> safely(operation));
    }
    
    /**
     * Safely execute an operation asynchronously returning Result
     */
    public static <T> CompletableFuture<Result<T, String>> safelyAsyncToResult(Supplier<T> operation) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() -> safelyToResult(operation));
    }
    
    /**
     * Lift a throwing function to a safe function
     */
    public static <T, R> Function<T, Optional<R>> lift(ThrowingFunction<T, R> fn) {
        return t -> {
            try {
                return Optional.ofNullable(fn.apply(t));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
    
    /**
     * Lift a throwing function to Result
     */
    public static <T, R> Function<T, Result<R, String>> liftToResult(ThrowingFunction<T, R> fn) {
        return t -> {
            try {
                R result = fn.apply(t);
                return result != null ? Result.success(result) : Result.failure("Function returned null");
            } catch (Exception e) {
                return Result.failure(e.getMessage());
            }
        };
    }
    
    /**
     * Safely execute a void operation
     */
    public static <T> Result<T, String> safelyExecute(Supplier<T> operation) {
        try {
            T result = operation.get();
            return result != null ? Result.success(result) : Result.failure("Operation returned null");
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }
    
    /**
     * Execute operation with resource management
     */
    public static <T, R extends AutoCloseable> Result<T, String> withResource(
            Supplier<R> resourceSupplier, 
            Function<R, T> operation) {
        try (R resource = resourceSupplier.get()) {
            T result = operation.apply(resource);
            return result != null ? Result.success(result) : Result.failure("Operation returned null");
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }
    
    /**
     * Execute operation with resource management (non-AutoCloseable)
     */
    public static <T, R> Result<T, String> withResource(
            Supplier<R> resourceSupplier, 
            Function<R, T> operation,
            java.util.function.Consumer<R> cleanup) {
        R resource = null;
        try {
            resource = resourceSupplier.get();
            T result = operation.apply(resource);
            return result != null ? Result.success(result) : Result.failure("Operation returned null");
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        } finally {
            if (resource != null) {
                try {
                    cleanup.accept(resource);
                } catch (Exception e) {
                    // Log cleanup failure but don't override main result
                }
            }
        }
    }
    
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
    
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}