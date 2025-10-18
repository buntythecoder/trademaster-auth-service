package com.trademaster.marketdata.functional;

import com.trademaster.common.functional.Result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Try Type for Exception-Safe Operations
 *
 * Represents a computation that may either succeed with a value or fail with an exception.
 * Replaces try-catch blocks with functional composition patterns.
 *
 * Features:
 * - Exception-safe computation execution
 * - Monadic composition (map, flatMap)
 * - Recovery strategies
 * - Pattern matching via sealed interface
 * - Conversion to Result type
 *
 * Usage:
 * <pre>{@code
 * // Execute risky operation safely
 * Try<String> result = Try.of(() -> riskyOperation());
 *
 * // Chain operations
 * Try<Integer> length = Try.of(() -> readFile())
 *     .map(String::trim)
 *     .map(String::length);
 *
 * // Recover from failures
 * String value = Try.of(() -> riskyOperation())
 *     .recover(ex -> "default value")
 *     .getOrElse("fallback");
 *
 * // Convert to Result
 * Result<String, Exception> result = Try.of(() -> operation())
 *     .toResult();
 * }</pre>
 *
 * @param <T> The type of the success value
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface Try<T> permits Try.Success, Try.Failure {

    /**
     * Success case containing a value
     */
    record Success<T>(T value) implements Try<T> {
        public Success {
            Objects.requireNonNull(value, "Success value cannot be null");
        }
    }

    /**
     * Failure case containing an exception
     */
    record Failure<T>(Exception exception) implements Try<T> {
        public Failure {
            Objects.requireNonNull(exception, "Failure exception cannot be null");
        }
    }

    /**
     * Execute a supplier that may throw an exception
     */
    static <T> Try<T> of(ThrowingSupplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    /**
     * Create a successful Try
     */
    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create a failed Try
     */
    static <T> Try<T> failure(Exception exception) {
        return new Failure<>(exception);
    }

    /**
     * Execute a runnable that may throw an exception
     */
    static Try<Void> run(ThrowingRunnable runnable) {
        Objects.requireNonNull(runnable, "Runnable cannot be null");
        return Try.of(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Check if this is a Success
     */
    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    /**
     * Check if this is a Failure
     */
    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    /**
     * Get the success value or throw exception
     */
    default T get() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new NoSuchElementException("Called get() on Failure", f.exception());
        };
    }

    /**
     * Get the value or a default
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> defaultValue;
        };
    }

    /**
     * Get the value or compute from exception
     */
    default T getOrElseGet(Function<Exception, T> exceptionHandler) {
        Objects.requireNonNull(exceptionHandler, "Exception handler cannot be null");
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> exceptionHandler.apply(f.exception());
        };
    }

    /**
     * Map the success value (Functor)
     */
    default <U> Try<U> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return switch (this) {
            case Success<T> s -> Try.of(() -> mapper.apply(s.value()));
            case Failure<T> f -> failure(f.exception());
        };
    }

    /**
     * Map with a throwing function
     */
    default <U> Try<U> mapTry(ThrowingFunction<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return switch (this) {
            case Success<T> s -> Try.of(() -> mapper.apply(s.value()));
            case Failure<T> f -> failure(f.exception());
        };
    }

    /**
     * FlatMap for monadic composition (Monad)
     */
    default <U> Try<U> flatMap(Function<T, Try<U>> mapper) {
        Objects.requireNonNull(mapper, "FlatMap function cannot be null");
        return switch (this) {
            case Success<T> s -> Try.of(() -> mapper.apply(s.value()).get());
            case Failure<T> f -> failure(f.exception());
        };
    }

    /**
     * Apply a predicate and convert to Failure if false
     */
    default Try<T> filter(Predicate<T> predicate, Supplier<Exception> exceptionSupplier) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        Objects.requireNonNull(exceptionSupplier, "Exception supplier cannot be null");
        return switch (this) {
            case Success<T> s -> predicate.test(s.value())
                ? this
                : failure(exceptionSupplier.get());
            case Failure<T> f -> this;
        };
    }

    /**
     * Execute consumer if Success
     */
    default Try<T> onSuccess(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "Consumer cannot be null");
        if (this instanceof Success<T> s) {
            consumer.accept(s.value());
        }
        return this;
    }

    /**
     * Execute consumer if Failure
     */
    default Try<T> onFailure(Consumer<Exception> consumer) {
        Objects.requireNonNull(consumer, "Consumer cannot be null");
        if (this instanceof Failure<T> f) {
            consumer.accept(f.exception());
        }
        return this;
    }

    /**
     * Recover from failure with a value
     */
    default Try<T> recover(Function<Exception, T> recovery) {
        Objects.requireNonNull(recovery, "Recovery function cannot be null");
        return switch (this) {
            case Success<T> s -> this;
            case Failure<T> f -> Try.of(() -> recovery.apply(f.exception()));
        };
    }

    /**
     * Recover from failure with another Try
     */
    default Try<T> recoverWith(Function<Exception, Try<T>> recovery) {
        Objects.requireNonNull(recovery, "Recovery function cannot be null");
        return switch (this) {
            case Success<T> s -> this;
            case Failure<T> f -> Try.of(() -> recovery.apply(f.exception()).get());
        };
    }

    /**
     * Convert to Optional (loses exception information)
     */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T> s -> Optional.of(s.value());
            case Failure<T> f -> Optional.empty();
        };
    }

    /**
     * Convert to Result type
     */
    default Result<T, Exception> toResult() {
        return switch (this) {
            case Success<T> s -> Result.success(s.value());
            case Failure<T> f -> Result.failure(f.exception());
        };
    }

    /**
     * Fold both cases into a single value
     */
    default <U> U fold(Function<T, U> successMapper, Function<Exception, U> failureMapper) {
        Objects.requireNonNull(successMapper, "Success mapper cannot be null");
        Objects.requireNonNull(failureMapper, "Failure mapper cannot be null");
        return switch (this) {
            case Success<T> s -> successMapper.apply(s.value());
            case Failure<T> f -> failureMapper.apply(f.exception());
        };
    }

    /**
     * Functional interface for throwing suppliers
     */
    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for throwing functions
     */
    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Functional interface for throwing runnables
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Combine two Tries into a tuple Try
     */
    static <T1, T2> Try<Tuple2<T1, T2>> zip(Try<T1> try1, Try<T2> try2) {
        return try1.flatMap(v1 ->
            try2.map(v2 -> new Tuple2<>(v1, v2))
        );
    }

    /**
     * Simple Tuple2 for zipping Tries
     */
    record Tuple2<T1, T2>(T1 first, T2 second) {}
}
