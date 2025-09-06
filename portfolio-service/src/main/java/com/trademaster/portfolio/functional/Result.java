package com.trademaster.portfolio.functional;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming
 * 
 * Represents either success (value) or failure (error) state.
 * Eliminates need for try-catch blocks and null returns.
 * 
 * Follows Rule #11 (Error Handling Patterns) - MANDATORY functional error handling.
 * 
 * @param <T> Success value type
 * @param <E> Error type
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Functional Programming)
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Create success result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Execute operation safely, wrapping in Result
     */
    static <T> Result<T, Exception> safely(Supplier<T> operation) {
        try {
            return success(operation.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    /**
     * Map success value using function
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * FlatMap for chaining operations (Railway Programming)
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    
    /**
     * Map error value
     */
    <F> Result<T, F> mapError(Function<E, F> mapper);
    
    /**
     * Fold result into single value
     */
    <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure);
    
    /**
     * Check if result is success
     */
    boolean isSuccess();
    
    /**
     * Check if result is failure
     */
    boolean isFailure();
    
    /**
     * Get success value if present
     */
    Optional<T> getSuccess();
    
    /**
     * Get error value if present
     */
    Optional<E> getFailure();
    
    /**
     * Get value or throw exception
     */
    T getOrThrow();
    
    /**
     * Get value or return default
     */
    T getOrDefault(T defaultValue);
    
    /**
     * Execute consumer if success
     */
    Result<T, E> onSuccess(Consumer<T> consumer);
    
    /**
     * Execute consumer if failure
     */
    Result<T, E> onFailure(Consumer<E> consumer);
    
    /**
     * Success implementation
     */
    record Success<T, E>(T value) implements Result<T, E> {
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return success(value);
        }
        
        @Override
        public <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onSuccess.apply(value);
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public Optional<T> getSuccess() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<E> getFailure() {
            return Optional.empty();
        }
        
        @Override
        public T getOrThrow() {
            return value;
        }
        
        @Override
        public T getOrDefault(T defaultValue) {
            return value;
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> consumer) {
            consumer.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> consumer) {
            return this;
        }
    }
    
    /**
     * Failure implementation
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return failure(error);
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return failure(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return failure(mapper.apply(error));
        }
        
        @Override
        public <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onFailure.apply(error);
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public Optional<T> getSuccess() {
            return Optional.empty();
        }
        
        @Override
        public Optional<E> getFailure() {
            return Optional.of(error);
        }
        
        @Override
        public T getOrThrow() {
            switch (error) {
                case Exception e -> throw new RuntimeException(e);
                case String msg -> throw new RuntimeException(msg);
                default -> throw new RuntimeException(error.toString());
            }
        }
        
        @Override
        public T getOrDefault(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> consumer) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> consumer) {
            consumer.accept(error);
            return this;
        }
    }
}