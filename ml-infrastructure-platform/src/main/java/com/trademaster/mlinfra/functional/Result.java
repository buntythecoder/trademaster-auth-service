package com.trademaster.mlinfra.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming
 * 
 * Represents either a successful value or an error in a functional way.
 * Enables error handling without exceptions and provides monadic operations.
 * 
 * @param <T> The type of the success value
 * @param <E> The type of the error
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * Success case containing a value
     */
    record Success<T, E>(T value) implements Result<T, E> {}

    /**
     * Failure case containing an error
     */
    record Failure<T, E>(E error) implements Result<T, E> {}

    /**
     * Create a successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create a failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Check if this is a success
     */
    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }

    /**
     * Check if this is a failure
     */
    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }

    /**
     * Get the success value or throw if failure
     */
    default T getValue() {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> throw new IllegalStateException("Result is failure: " + error);
        };
    }

    /**
     * Get the error or throw if success
     */
    default E getError() {
        return switch (this) {
            case Success<T, E>(var value) -> throw new IllegalStateException("Result is success: " + value);
            case Failure<T, E>(var error) -> error;
        };
    }

    /**
     * Get the success value or return a default
     */
    default T getValueOr(T defaultValue) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> defaultValue;
        };
    }

    /**
     * Get the success value or compute from supplier
     */
    default T getValueOr(Supplier<T> supplier) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> supplier.get();
        };
    }

    /**
     * Transform the success value
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(mapper.apply(value));
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    /**
     * Transform the error
     */
    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(value);
            case Failure<T, E>(var error) -> failure(mapper.apply(error));
        };
    }

    /**
     * Chain operations (flatMap)
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    /**
     * Filter success value with predicate
     */
    default Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
        return switch (this) {
            case Success<T, E>(var value) -> 
                predicate.test(value) ? this : failure(errorSupplier.get());
            case Failure<T, E> failure -> failure;
        };
    }

    /**
     * Execute action on success value
     */
    default Result<T, E> onSuccess(Consumer<T> action) {
        if (this instanceof Success<T, E>(var value)) {
            action.accept(value);
        }
        return this;
    }

    /**
     * Execute action on error
     */
    default Result<T, E> onFailure(Consumer<E> action) {
        if (this instanceof Failure<T, E>(var error)) {
            action.accept(error);
        }
        return this;
    }

    /**
     * Execute different actions based on success/failure
     */
    default Result<T, E> match(Consumer<T> onSuccess, Consumer<E> onFailure) {
        switch (this) {
            case Success<T, E>(var value) -> onSuccess.accept(value);
            case Failure<T, E>(var error) -> onFailure.accept(error);
        }
        return this;
    }

    /**
     * Transform to different type based on success/failure
     */
    default <R> R fold(Function<T, R> onSuccess, Function<E, R> onFailure) {
        return switch (this) {
            case Success<T, E>(var value) -> onSuccess.apply(value);
            case Failure<T, E>(var error) -> onFailure.apply(error);
        };
    }

    /**
     * Recover from failure with a value
     */
    default Result<T, E> recover(Function<E, T> recovery) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E>(var error) -> success(recovery.apply(error));
        };
    }

    /**
     * Recover from failure with another Result
     */
    default Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }

    /**
     * Combine two Results
     */
    default <U, R> Result<R, E> combine(Result<U, E> other, 
                                         java.util.function.BiFunction<T, U, R> combiner) {
        return switch (this) {
            case Success<T, E>(var value1) -> switch (other) {
                case Success<U, E>(var value2) -> success(combiner.apply(value1, value2));
                case Failure<U, E>(var error) -> failure(error);
            };
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    /**
     * Try to execute a function that may throw
     */
    static <T> Result<T, Exception> tryExecute(java.util.concurrent.Callable<T> callable) {
        try {
            return success(callable.call());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Try to execute a supplier that may throw
     */
    static <T> Result<T, Exception> trySupply(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Try to execute a runnable that may throw
     */
    static Result<Void, Exception> tryRun(Runnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Convert to Optional (loses error information)
     */
    default java.util.Optional<T> toOptional() {
        return switch (this) {
            case Success<T, E>(var value) -> java.util.Optional.ofNullable(value);
            case Failure<T, E>(var error) -> java.util.Optional.empty();
        };
    }
}