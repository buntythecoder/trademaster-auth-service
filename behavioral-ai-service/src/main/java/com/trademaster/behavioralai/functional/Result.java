package com.trademaster.behavioralai.functional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result Type Implementation
 * 
 * Either<Error, Success> pattern for functional error handling without exceptions.
 * Follows railway programming principles for composable error handling.
 */
public sealed interface Result<T, E extends Error> 
    permits Result.Success, Result.Failure {
    
    /**
     * Create successful result
     */
    static <T, E extends Error> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create failure result
     */
    static <T, E extends Error> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Try operation with functional error handling
     */
    static <T, E extends Error> Result<T, E> tryExecute(
            Supplier<T> operation, 
            Function<Exception, E> errorMapper) {
        try {
            return success(operation.get());
        } catch (Exception ex) {
            return failure(errorMapper.apply(ex));
        }
    }
    
    /**
     * Chain operations with flatMap (monadic bind)
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    
    /**
     * Transform success value
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * Transform error value
     */
    <F extends Error> Result<T, F> mapError(Function<E, F> mapper);
    
    /**
     * Handle both success and failure cases
     */
    <U> U fold(Function<E, U> errorHandler, Function<T, U> successHandler);
    
    /**
     * Execute side effect on success
     */
    Result<T, E> onSuccess(Consumer<T> action);
    
    /**
     * Execute side effect on failure
     */
    Result<T, E> onFailure(Consumer<E> action);
    
    /**
     * Filter success value with predicate
     */
    Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier);
    
    /**
     * Provide fallback value on failure
     */
    T orElse(T defaultValue);
    
    /**
     * Provide fallback value with supplier on failure
     */
    T orElseGet(Supplier<T> defaultSupplier);
    
    /**
     * Convert to Optional (loses error information)
     */
    Optional<T> toOptional();
    
    /**
     * Check if result is successful
     */
    boolean isSuccess();
    
    /**
     * Check if result is failure
     */
    boolean isFailure();
    
    /**
     * Get success value (unsafe - use with isSuccess check)
     */
    T getValue();
    
    /**
     * Get error value (unsafe - use with isFailure check)
     */
    E getError();
    
    /**
     * Success case implementation
     */
    record Success<T, E extends Error>(T value) implements Result<T, E> {
        
        public Success {
            if (value == null) {
                throw new IllegalArgumentException("Success value cannot be null");
            }
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }
        
        @Override
        public <F extends Error> Result<T, F> mapError(Function<E, F> mapper) {
            @SuppressWarnings("unchecked")
            Result<T, F> result = (Result<T, F>) this;
            return result;
        }
        
        @Override
        public <U> U fold(Function<E, U> errorHandler, Function<T, U> successHandler) {
            return successHandler.apply(value);
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            return this;
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
            return predicate.test(value) ? this : failure(errorSupplier.get());
        }
        
        @Override
        public T orElse(T defaultValue) {
            return value;
        }
        
        @Override
        public T orElseGet(Supplier<T> defaultSupplier) {
            return value;
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
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
        public T getValue() {
            return value;
        }
        
        @Override
        public E getError() {
            throw new IllegalStateException("Success result has no error");
        }
    }
    
    /**
     * Failure case implementation
     */
    record Failure<T, E extends Error>(E error) implements Result<T, E> {
        
        public Failure {
            if (error == null) {
                throw new IllegalArgumentException("Error cannot be null");
            }
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) this;
            return result;
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) this;
            return result;
        }
        
        @Override
        public <F extends Error> Result<T, F> mapError(Function<E, F> mapper) {
            return failure(mapper.apply(error));
        }
        
        @Override
        public <U> U fold(Function<E, U> errorHandler, Function<T, U> successHandler) {
            return errorHandler.apply(error);
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<T> action) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<E> action) {
            action.accept(error);
            return this;
        }
        
        @Override
        public Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
            return this;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public T orElseGet(Supplier<T> defaultSupplier) {
            return defaultSupplier.get();
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
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
        public T getValue() {
            throw new IllegalStateException("Failure result has no value");
        }
        
        @Override
        public E getError() {
            return error;
        }
    }
}