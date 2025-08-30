package com.trademaster.marketdata.pattern;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Railway Oriented Programming Result type for functional error handling
 * Eliminates try-catch blocks and null returns from business logic
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}
    
    // Factory methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    // Safe execution wrapper
    static <T> Result<T, String> safely(Supplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e.getMessage());
        }
    }
    
    // Monadic operations
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success(var value) -> success(mapper.apply(value));
            case Failure(var error) -> failure(error);
        };
    }
    
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var error) -> failure(error);
        };
    }
    
    default Result<T, E> filter(Predicate<T> predicate, E errorOnFalse) {
        return switch (this) {
            case Success(var value) -> predicate.test(value) ? 
                success(value) : failure(errorOnFalse);
            case Failure(var error) -> failure(error);
        };
    }
    
    // Pattern matching operations
    default <R> R fold(Function<E, R> onFailure, Function<T, R> onSuccess) {
        return switch (this) {
            case Success(var value) -> onSuccess.apply(value);
            case Failure(var error) -> onFailure.apply(error);
        };
    }
    
    default void match(Consumer<E> onFailure, Consumer<T> onSuccess) {
        switch (this) {
            case Success(var value) -> onSuccess.accept(value);
            case Failure(var error) -> onFailure.accept(error);
        }
    }
    
    // Side effects
    default Result<T, E> peek(Consumer<T> action) {
        if (this instanceof Success(var value)) {
            action.accept(value);
        }
        return this;
    }
    
    default Result<T, E> peekError(Consumer<E> action) {
        if (this instanceof Failure(var error)) {
            action.accept(error);
        }
        return this;
    }
    
    // Utility methods
    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }
    
    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }
    
    default Optional<T> toOptional() {
        return switch (this) {
            case Success(var value) -> Optional.of(value);
            case Failure(var error) -> Optional.empty();
        };
    }
    
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> defaultValue;
        };
    }
    
    default T orElseGet(Supplier<T> supplier) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> supplier.get();
        };
    }
    
    default T orElseThrow() {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw new RuntimeException(error.toString());
        };
    }
    
    // Combine multiple results
    static <T, E> Result<T, E> combine(Result<T, E> result1, Result<T, E> result2, 
            java.util.function.BinaryOperator<T> combiner) {
        return result1.flatMap(val1 -> 
            result2.map(val2 -> combiner.apply(val1, val2)));
    }
    
    // Railway operations for chaining
    static <T, E> Function<T, Result<T, E>> lift(Predicate<T> predicate, E error) {
        return input -> predicate.test(input) ? 
            success(input) : failure(error);
    }
    
    static <T, U, E> Function<T, Result<U, E>> liftFunction(
            Function<T, U> function, Function<Exception, E> errorMapper) {
        return input -> {
            try {
                return success(function.apply(input));
            } catch (Exception e) {
                return failure(errorMapper.apply(e));
            }
        };
    }
}