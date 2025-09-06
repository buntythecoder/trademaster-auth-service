package com.trademaster.agentos.security.model;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result type for railway-oriented programming.
 * Represents either a successful value or an error.
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    // Pattern matching support
    boolean isSuccess();
    boolean isFailure();
    
    // Functional transformations
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);
    <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper);
    Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier);
    
    // Side effects
    Result<T, E> onSuccess(Consumer<? super T> action);
    Result<T, E> onFailure(Consumer<? super E> action);
    
    // Value extraction
    T orElse(T defaultValue);
    T orElseGet(Supplier<? extends T> supplier);
    <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X;
    
    // Conversion
    Optional<T> toOptional();
    
    // Factory methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    static <T, E> Result<T, E> of(Supplier<T> supplier, E error) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(error);
        }
    }
    
    // Success implementation
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return new Success<>(mapper.apply(value));
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier) {
            return predicate.test(value) ? this : new Failure<>(errorSupplier.get());
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<? super T> action) {
            action.accept(value);
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<? super E> action) {
            return this;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return value;
        }
        
        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return value;
        }
        
        @Override
        public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) {
            return value;
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }
    }
    
    // Failure implementation
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return (Result<U, E>) this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
            return (Result<U, E>) this;
        }
        
        @Override
        public Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier) {
            return this;
        }
        
        @Override
        public Result<T, E> onSuccess(Consumer<? super T> action) {
            return this;
        }
        
        @Override
        public Result<T, E> onFailure(Consumer<? super E> action) {
            action.accept(error);
            return this;
        }
        
        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return supplier.get();
        }
        
        @Override
        public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X {
            throw exceptionMapper.apply(error);
        }
        
        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }
    }
}