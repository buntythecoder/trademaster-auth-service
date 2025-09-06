package com.trademaster.agentos.functional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Railway Oriented Programming Result Monad for TradeMaster Agent OS
 * 
 * Represents either a successful result (Success) or a failure (Failure).
 * This enables functional error handling without exceptions and try-catch blocks.
 * 
 * @param <T> The type of the success value
 * @param <E> The type of the error value
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Success case containing the successful result value
     */
    record Success<T, E>(T value) implements Result<T, E> {
        public Success {
            Objects.requireNonNull(value, "Success value cannot be null");
        }
    }
    
    /**
     * Failure case containing the error value
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        public Failure {
            Objects.requireNonNull(error, "Error value cannot be null");
        }
    }
    
    /**
     * Creates a successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Creates a failed result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Creates a result from a value that might be null
     */
    static <T, E> Result<T, E> fromNullable(T value, E errorIfNull) {
        return value != null ? success(value) : failure(errorIfNull);
    }
    
    /**
     * Creates a result from an Optional
     */
    static <T, E> Result<T, E> fromOptional(Optional<T> optional, E errorIfEmpty) {
        return optional.map(Result::<T, E>success)
                      .orElse(failure(errorIfEmpty));
    }
    
    /**
     * Creates a result by executing a supplier that might throw an exception
     */
    static <T> Result<T, Exception> catching(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Alias for catching - more descriptive for business operations
     */
    static <T> Result<T, Exception> tryExecute(Supplier<T> supplier) {
        return catching(supplier);
    }
    
    /**
     * Unit type for void operations
     */
    record Unit() {
        public static final Unit INSTANCE = new Unit();
    }
    
    /**
     * Execute a runnable that might throw and return void result
     */
    static Result<Unit, Exception> tryRun(Runnable runnable) {
        try {
            runnable.run();
            return success(Unit.INSTANCE);
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Creates a result by executing a supplier with custom error mapping
     */
    static <T, E> Result<T, E> tryExecute(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(errorMapper.apply(e));
        }
    }
    
    /**
     * Creates a result from a boolean condition
     */
    static <E> Result<Unit, E> fromCondition(boolean condition, E errorIfFalse) {
        return condition ? success(Unit.INSTANCE) : failure(errorIfFalse);
    }
    
    /**
     * Checks if this is a success
     */
    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }
    
    /**
     * Checks if this is a failure
     */
    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }
    
    /**
     * Gets the success value if present
     */
    default Optional<T> getValue() {
        return switch (this) {
            case Success(var value) -> Optional.of(value);
            case Failure(var error) -> Optional.empty();
        };
    }
    
    /**
     * Gets the error value if present
     */
    default Optional<E> getError() {
        return switch (this) {
            case Success(var value) -> Optional.empty();
            case Failure(var error) -> Optional.of(error);
        };
    }
    
    /**
     * Maps the success value to another type
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success(var value) -> success(mapper.apply(value));
            case Failure(var error) -> failure(error);
        };
    }
    
    /**
     * Maps the error value to another type
     */
    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success(var value) -> success(value);
            case Failure(var error) -> failure(mapper.apply(error));
        };
    }
    
    /**
     * Alias for mapError - maps the failure value to another type
     */
    default <F> Result<T, F> mapFailure(Function<E, F> mapper) {
        return mapError(mapper);
    }
    
    /**
     * FlatMap operation for monadic composition
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var error) -> failure(error);
        };
    }
    
    /**
     * Filters the success value with a predicate
     */
    default Result<T, E> filter(Predicate<T> predicate, E errorIfFalse) {
        return switch (this) {
            case Success(var value) -> predicate.test(value) ? this : failure(errorIfFalse);
            case Failure(var error) -> this;
        };
    }
    
    /**
     * Combines two results - both must be successful
     */
    default <U, V> Result<V, E> combine(Result<U, E> other, java.util.function.BiFunction<T, U, V> combiner) {
        return switch (this) {
            case Success(var value1) -> switch (other) {
                case Success(var value2) -> success(combiner.apply(value1, value2));
                case Failure(var error) -> failure(error);
            };
            case Failure(var error) -> failure(error);
        };
    }
    
    /**
     * Sequence multiple Results - all must be successful
     */
    static <T, E> Result<java.util.List<T>, E> sequence(java.util.List<Result<T, E>> results) {
        java.util.List<T> values = new java.util.ArrayList<>();
        for (Result<T, E> result : results) {
            switch (result) {
                case Success(var value) -> values.add(value);
                case Failure(var error) -> { return failure(error); }
            }
        }
        return success(values);
    }
    
    /**
     * Folds the result into a single value
     */
    default <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
        return switch (this) {
            case Success(var value) -> onSuccess.apply(value);
            case Failure(var error) -> onFailure.apply(error);
        };
    }
    
    /**
     * Gets the success value or returns the default
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> defaultValue;
        };
    }
    
    /**
     * Gets the success value or computes it from the error
     */
    default T getOrElse(Function<E, T> defaultComputer) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> defaultComputer.apply(error);
        };
    }
    
    /**
     * Recovers from failure with another result
     */
    default Result<T, E> orElse(Result<T, E> alternative) {
        return switch (this) {
            case Success(var value) -> this;
            case Failure(var error) -> alternative;
        };
    }
    
    /**
     * Recovers from failure by computing another result
     */
    default Result<T, E> recover(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success(var value) -> this;
            case Failure(var error) -> recovery.apply(error);
        };
    }
    
    /**
     * Performs a side effect on success
     */
    default Result<T, E> onSuccess(Consumer<T> action) {
        if (this instanceof Success(var value)) {
            action.accept(value);
        }
        return this;
    }
    
    /**
     * Performs a side effect on failure
     */
    default Result<T, E> onFailure(Consumer<E> action) {
        if (this instanceof Failure(var error)) {
            action.accept(error);
        }
        return this;
    }
    
    /**
     * Converts to Optional, discarding error information
     */
    default Optional<T> toOptional() {
        return getValue();
    }
    
    /**
     * Throws the error as a RuntimeException if this is a failure
     */
    default T getOrThrow() {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> {
                if (error instanceof RuntimeException re) {
                    throw re;
                } else if (error instanceof Exception e) {
                    throw new RuntimeException(e);
                } else {
                    throw new RuntimeException(error.toString());
                }
            }
        };
    }
    
    /**
     * Throws a custom exception if this is a failure
     */
    default T getOrThrow(Function<E, RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw exceptionMapper.apply(error);
        };
    }
    
    @Override
    String toString();
}