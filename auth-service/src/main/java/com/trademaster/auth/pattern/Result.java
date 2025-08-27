package com.trademaster.auth.pattern;

import java.util.function.Function;

/**
 * Result Type for Railway-Oriented Programming
 * 
 * Functional error handling pattern that eliminates try-catch blocks
 * and provides composable error handling following TradeMaster standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface Result<T, E> {
    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}
    
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(mapper.apply(value));
            case Failure<T, E>(var error) -> failure(error);
        };
    }
    
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> failure(error);
        };
    }
    
    default boolean isSuccess() {
        return this instanceof Success<T, E>;
    }
    
    default boolean isFailure() {
        return this instanceof Failure<T, E>;
    }
    
    default T getValue() {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> throw new IllegalStateException("Cannot get value from failure: " + error);
        };
    }
    
    default E getError() {
        return switch (this) {
            case Success<T, E>(var value) -> throw new IllegalStateException("Cannot get error from success");
            case Failure<T, E>(var error) -> error;
        };
    }
    
    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> defaultValue;
        };
    }
    
    default <X extends Throwable> T orElseThrow(Function<E, X> exceptionMapper) throws X {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> throw exceptionMapper.apply(error);
        };
    }
    
    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(value);
            case Failure<T, E>(var error) -> failure(mapper.apply(error));
        };
    }
    
    default <F> Result<T, F> mapError(Class<? extends E> errorType, Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(value);
            case Failure<T, E>(var error) -> errorType.isInstance(error) ? 
                failure(mapper.apply(error)) : failure((F) error);
        };
    }
    
    default <U> U fold(Function<T, U> successMapper, Function<E, U> errorMapper) {
        return switch (this) {
            case Success<T, E>(var value) -> successMapper.apply(value);
            case Failure<T, E>(var error) -> errorMapper.apply(error);
        };
    }
}