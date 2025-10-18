package com.trademaster.marketdata.functional;

import com.trademaster.common.functional.Result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Railway-Oriented Programming Utilities
 *
 * Helper utilities for composing functional operations with Result, Try, and Validation.
 * Provides clean APIs for building functional pipelines and error handling chains.
 *
 * Features:
 * - Functional composition helpers
 * - CompletableFuture integration
 * - Optional integration
 * - Stream processing utilities
 * - Validation chains
 *
 * Usage:
 * <pre>{@code
 * // Compose operations safely
 * Result<User, Error> user = Railway.pipe(
 *     getUserId(),
 *     this::fetchUser,
 *     this::validateUser,
 *     this::enrichUser
 * );
 *
 * // Async operations with virtual threads
 * CompletableFuture<Result<Data, Error>> future = Railway.async(
 *     () -> fetchData(),
 *     virtualThreadExecutor
 * );
 *
 * // Validate multiple conditions
 * Result<Order, ValidationError> validated = Railway.validateAll(
 *     order,
 *     List.of(
 *         o -> o.amount() > 0,
 *         o -> o.symbol() != null,
 *         o -> o.quantity() > 0
 *     ),
 *     ValidationError::new
 * );
 * }</pre>
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class Railway {

    private Railway() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Pipe operations through a functional chain
     */
    @SafeVarargs
    public static <T, E> Result<T, E> pipe(
            Result<T, E> initial,
            Function<T, Result<T, E>>... operations) {

        Objects.requireNonNull(initial, "Initial result cannot be null");
        return Stream.of(operations)
            .reduce(initial,
                (result, op) -> result.flatMap(op),
                (r1, r2) -> r1
            );
    }

    /**
     * Execute Try operation and convert to Result
     */
    public static <T> Result<T, Exception> safely(Try.ThrowingSupplier<T> supplier) {
        return Try.of(supplier).toResult();
    }

    /**
     * Execute async operation with CompletableFuture
     */
    public static <T, E> CompletableFuture<Result<T, E>> async(
            Supplier<Result<T, E>> operation,
            java.util.concurrent.Executor executor) {

        Objects.requireNonNull(operation, "Operation cannot be null");
        Objects.requireNonNull(executor, "Executor cannot be null");
        return CompletableFuture.supplyAsync(operation, executor);
    }

    /**
     * Validate all predicates and accumulate errors
     */
    public static <T, E> Result<T, List<E>> validateAll(
            T value,
            List<Predicate<T>> predicates,
            Function<Integer, E> errorMapper) {

        Objects.requireNonNull(value, "Value cannot be null");
        Objects.requireNonNull(predicates, "Predicates cannot be null");
        Objects.requireNonNull(errorMapper, "Error mapper cannot be null");

        List<E> errors = Stream.iterate(0, i -> i < predicates.size(), i -> i + 1)
            .filter(i -> !predicates.get(i).test(value))
            .map(errorMapper)
            .toList();

        return errors.isEmpty()
            ? Result.success(value)
            : Result.failure(errors);
    }

    /**
     * Convert Optional to Result with error supplier
     */
    public static <T, E> Result<T, E> fromOptional(
            Optional<T> optional,
            Supplier<E> errorSupplier) {

        Objects.requireNonNull(optional, "Optional cannot be null");
        Objects.requireNonNull(errorSupplier, "Error supplier cannot be null");
        return optional
            .map(Result::<T, E>success)
            .orElseGet(() -> Result.failure(errorSupplier.get()));
    }

    /**
     * Convert Result to Optional (loses error information)
     */
    public static <T, E> Optional<T> toOptional(Result<T, E> result) {
        Objects.requireNonNull(result, "Result cannot be null");
        return result.toOptional();
    }

    /**
     * Sequence list of Results into Result of list
     */
    public static <T, E> Result<List<T>, E> sequence(List<Result<T, E>> results) {
        Objects.requireNonNull(results, "Results list cannot be null");

        return results.stream()
            .filter(Result::isFailure)
            .findFirst()
            .map(r -> Result.<List<T>, E>failure(r.getError()))
            .orElseGet(() -> Result.success(
                results.stream()
                    .map(Result::getValue)
                    .toList()
            ));
    }

    /**
     * Traverse list with function that returns Result
     */
    public static <T, U, E> Result<List<U>, E> traverse(
            List<T> values,
            Function<T, Result<U, E>> mapper) {

        Objects.requireNonNull(values, "Values list cannot be null");
        Objects.requireNonNull(mapper, "Mapper function cannot be null");

        return sequence(values.stream()
            .map(mapper)
            .toList()
        );
    }

    /**
     * Combine two Results with a binary function
     */
    public static <T1, T2, R, E> Result<R, E> combine(
            Result<T1, E> r1,
            Result<T2, E> r2,
            Function<T1, Function<T2, R>> combiner) {

        Objects.requireNonNull(r1, "First result cannot be null");
        Objects.requireNonNull(r2, "Second result cannot be null");
        Objects.requireNonNull(combiner, "Combiner cannot be null");

        return r1.flatMap(v1 -> r2.map(v2 -> combiner.apply(v1).apply(v2)));
    }

    /**
     * Combine three Results with a ternary function
     */
    public static <T1, T2, T3, R, E> Result<R, E> combine(
            Result<T1, E> r1,
            Result<T2, E> r2,
            Result<T3, E> r3,
            Function3<T1, T2, T3, R> combiner) {

        Objects.requireNonNull(r1, "First result cannot be null");
        Objects.requireNonNull(r2, "Second result cannot be null");
        Objects.requireNonNull(r3, "Third result cannot be null");
        Objects.requireNonNull(combiner, "Combiner cannot be null");

        return r1.flatMap(v1 ->
            r2.flatMap(v2 ->
                r3.map(v3 -> combiner.apply(v1, v2, v3))
            )
        );
    }

    /**
     * Apply function if condition is true, otherwise return value unchanged
     */
    public static <T, E> Result<T, E> when(
            Result<T, E> result,
            Predicate<T> condition,
            Function<T, Result<T, E>> operation) {

        Objects.requireNonNull(result, "Result cannot be null");
        Objects.requireNonNull(condition, "Condition cannot be null");
        Objects.requireNonNull(operation, "Operation cannot be null");

        return result.flatMap(value ->
            condition.test(value) ? operation.apply(value) : Result.success(value)
        );
    }

    /**
     * Retry operation with maximum attempts
     */
    public static <T> Try<T> retry(
            Try.ThrowingSupplier<T> operation,
            int maxAttempts) {

        Objects.requireNonNull(operation, "Operation cannot be null");

        return Stream.iterate(1, i -> i <= maxAttempts, i -> i + 1)
            .map(attempt -> Try.of(operation))
            .filter(Try::isSuccess)
            .findFirst()
            .orElseGet(() -> Try.of(operation));
    }

    /**
     * Execute operation with timeout
     */
    public static <T> CompletableFuture<Result<T, Exception>> withTimeout(
            Supplier<T> operation,
            long timeoutMillis,
            java.util.concurrent.Executor executor) {

        Objects.requireNonNull(operation, "Operation cannot be null");
        Objects.requireNonNull(executor, "Executor cannot be null");

        return CompletableFuture
            .supplyAsync(() -> Result.<T, Exception>success(operation.get()), executor)
            .orTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Result.failure(
                ex instanceof java.util.concurrent.TimeoutException
                    ? new java.util.concurrent.TimeoutException("Operation timed out")
                    : (Exception) ex
            ));
    }

    /**
     * Parallel execution of multiple operations
     */
    @SafeVarargs
    public static <T, E> CompletableFuture<Result<List<T>, E>> parallel(
            java.util.concurrent.Executor executor,
            Supplier<Result<T, E>>... operations) {

        Objects.requireNonNull(executor, "Executor cannot be null");

        CompletableFuture<Result<T, E>>[] futures = Stream.of(operations)
            .map(op -> CompletableFuture.supplyAsync(op, executor))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
            .thenApply(v -> sequence(
                Stream.of(futures)
                    .map(CompletableFuture::join)
                    .toList()
            ));
    }

    /**
     * Functional interface for three-argument functions
     */
    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }

    /**
     * Functional interface for four-argument functions
     */
    @FunctionalInterface
    public interface Function4<T1, T2, T3, T4, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    /**
     * Tap into a Result for side effects without changing the value
     */
    public static <T, E> Result<T, E> tap(
            Result<T, E> result,
            java.util.function.Consumer<T> sideEffect) {

        Objects.requireNonNull(result, "Result cannot be null");
        Objects.requireNonNull(sideEffect, "Side effect cannot be null");
        return result.onSuccess(sideEffect);
    }

    /**
     * Tap into errors for logging or monitoring
     */
    public static <T, E> Result<T, E> tapError(
            Result<T, E> result,
            java.util.function.Consumer<E> errorHandler) {

        Objects.requireNonNull(result, "Result cannot be null");
        Objects.requireNonNull(errorHandler, "Error handler cannot be null");
        return result.onFailure(errorHandler);
    }
}
