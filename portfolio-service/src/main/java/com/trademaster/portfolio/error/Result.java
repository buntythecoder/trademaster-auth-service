package com.trademaster.portfolio.error;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Result Type for Functional Error Handling
 *
 * Railway-oriented programming pattern for handling success and failure cases.
 * Eliminates need for try-catch blocks and null checks in business logic.
 *
 * Key Features:
 * - Sealed interface with Success and Failure variants
 * - Monadic operations (map, flatMap, recover)
 * - Pattern matching support with Java 24
 * - Railway programming for error propagation
 * - Functional composition of operations
 *
 * Design Patterns:
 * - Either Monad: Represents success or failure
 * - Railway Programming: Operations stay on success track or failure track
 * - Sealed Types: Type-safe pattern matching
 *
 * Rule Compliance:
 * - Rule #3: Functional programming (no if-else)
 * - Rule #9: Immutable records for data
 * - Rule #11: Functional error handling (no try-catch)
 * - Rule #14: Pattern matching with sealed types
 *
 * @param <T> Success value type
 * @param <E> Error type
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    // ==================== FACTORY METHODS ====================

    /**
     * Create a successful result
     *
     * Rule #3: Functional factory method
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create a failed result
     *
     * Rule #3: Functional factory method
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Create result from Optional
     *
     * Rule #3: Functional conversion from Optional
     */
    static <T, E> Result<T, E> fromOptional(Optional<T> optional, Supplier<E> errorSupplier) {
        return optional
            .map(Result::<T, E>success)
            .orElseGet(() -> failure(errorSupplier.get()));
    }

    // ==================== MONADIC OPERATIONS ====================

    /**
     * Map successful value to new value
     *
     * Rule #3: Functor map operation
     * Rule #14: Pattern matching with switch
     *
     * @param mapper Function to transform success value
     * @return New Result with transformed value or original error
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> new Success<>(mapper.apply(value));
            case Failure<T, E> failure -> new Failure<>(failure.error());
        };
    }

    /**
     * FlatMap for chaining operations that return Results
     *
     * Rule #3: Monad flatMap operation
     * Rule #14: Pattern matching with switch
     *
     * @param mapper Function that returns Result
     * @return Flattened Result
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(T value) -> mapper.apply(value);
            case Failure<T, E> failure -> new Failure<>(failure.error());
        };
    }

    /**
     * Map error to different error type
     *
     * Rule #3: Error transformation
     * Rule #14: Pattern matching with switch
     *
     * @param errorMapper Function to transform error
     * @return Result with transformed error or original success
     */
    default <F> Result<T, F> mapError(Function<E, F> errorMapper) {
        return switch (this) {
            case Success<T, E> success -> new Success<>(success.value());
            case Failure<T, E>(E error) -> new Failure<>(errorMapper.apply(error));
        };
    }

    /**
     * Recover from failure with default value
     *
     * Rule #3: Functional recovery pattern
     * Rule #14: Pattern matching with switch
     *
     * @param defaultValue Value to use if result is failure
     * @return Success with original or default value
     */
    default Result<T, E> recover(T defaultValue) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E> failure -> new Success<>(defaultValue);
        };
    }

    /**
     * Recover from failure using supplier
     *
     * Rule #3: Lazy recovery pattern
     * Rule #14: Pattern matching with switch
     *
     * @param supplier Supplier of default value
     * @return Success with original or supplied value
     */
    default Result<T, E> recoverWith(Supplier<T> supplier) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E> failure -> new Success<>(supplier.get());
        };
    }

    /**
     * Recover from failure with another Result
     *
     * Rule #3: Alternative result pattern
     * Rule #14: Pattern matching with switch
     *
     * @param alternative Alternative result to use if this fails
     * @return This result if success, alternative if failure
     */
    default Result<T, E> orElse(Result<T, E> alternative) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E> failure -> alternative;
        };
    }

    /**
     * Filter success value with predicate
     *
     * Rule #3: Functional filtering
     * Rule #14: Pattern matching with switch
     *
     * @param predicate Test to apply to success value
     * @param errorSupplier Error supplier if predicate fails
     * @return Original result if predicate passes, failure otherwise
     */
    default Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
        return switch (this) {
            case Success<T, E>(T value) -> predicate.test(value) ? this : failure(errorSupplier.get());
            case Failure<T, E> failure -> failure;
        };
    }

    // ==================== SIDE EFFECT OPERATIONS ====================

    /**
     * Execute action on success value
     *
     * Rule #3: Side effect handling
     * Rule #14: Pattern matching with switch
     *
     * @param action Action to execute on success
     * @return This result for chaining
     */
    default Result<T, E> onSuccess(Consumer<T> action) {
        switch (this) {
            case Success<T, E>(T value) -> action.accept(value);
            case Failure<T, E> failure -> {}
        }
        return this;
    }

    /**
     * Execute action on failure error
     *
     * Rule #3: Side effect handling
     * Rule #14: Pattern matching with switch
     *
     * @param action Action to execute on failure
     * @return This result for chaining
     */
    default Result<T, E> onFailure(Consumer<E> action) {
        switch (this) {
            case Success<T, E> success -> {}
            case Failure<T, E>(E error) -> action.accept(error);
        }
        return this;
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Check if result is successful
     *
     * Rule #3: Query method
     * Rule #14: Pattern matching with switch
     */
    default boolean isSuccess() {
        return switch (this) {
            case Success<T, E> success -> true;
            case Failure<T, E> failure -> false;
        };
    }

    /**
     * Check if result is failure
     *
     * Rule #3: Query method
     * Rule #14: Pattern matching with switch
     */
    default boolean isFailure() {
        return switch (this) {
            case Success<T, E> success -> false;
            case Failure<T, E> failure -> true;
        };
    }

    /**
     * Get success value or throw
     *
     * Rule #3: Unsafe extraction
     * Rule #14: Pattern matching with switch
     *
     * @return Success value
     * @throws IllegalStateException if result is failure
     */
    default T getOrThrow() {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E>(E error) -> throw new IllegalStateException("Result is failure: " + error);
        };
    }

    /**
     * Get success value or default
     *
     * Rule #3: Safe extraction with default
     * Rule #14: Pattern matching with switch
     *
     * @param defaultValue Default value if failure
     * @return Success value or default
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E> failure -> defaultValue;
        };
    }

    /**
     * Get success value or compute from error
     *
     * Rule #3: Computed default from error
     * Rule #14: Pattern matching with switch
     *
     * @param errorMapper Function to compute value from error
     * @return Success value or computed value
     */
    default T getOrElseGet(Function<E, T> errorMapper) {
        return switch (this) {
            case Success<T, E>(T value) -> value;
            case Failure<T, E>(E error) -> errorMapper.apply(error);
        };
    }

    /**
     * Convert to Optional
     *
     * Rule #3: Conversion to Optional
     * Rule #14: Pattern matching with switch
     */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T, E>(T value) -> Optional.of(value);
            case Failure<T, E> failure -> Optional.empty();
        };
    }

    // ==================== VARIANT TYPES ====================

    /**
     * Success variant
     *
     * Rule #9: Immutable record for success
     * Rule #14: Sealed type variant
     *
     * @param value Success value
     */
    record Success<T, E>(T value) implements Result<T, E> {}

    /**
     * Failure variant
     *
     * Rule #9: Immutable record for failure
     * Rule #14: Sealed type variant
     *
     * @param error Error value
     */
    record Failure<T, E>(E error) implements Result<T, E> {}
}
