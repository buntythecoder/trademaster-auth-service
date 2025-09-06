package com.trademaster.userprofile.common;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Functional Result Type for Railway Programming Pattern
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: No try-catch in business logic, use functional constructs
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    // Factory Methods
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    static <T> Result<T, Exception> tryExecute(Supplier<T> supplier) {
        return switch (tryOperation(supplier)) {
            case Success<T, Exception>(T value) -> success(value);
            case Failure<T, Exception>(Exception error) -> failure(error);
        };
    }
    
    private static <T> Result<T, Exception> tryOperation(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    // Functional Operations
    <U> Result<U, E> map(Function<T, U> mapper);
    <F> Result<T, F> mapError(Function<E, F> errorMapper);
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    Result<T, E> filter(Function<T, Boolean> predicate, E errorOnFalse);
    
    // Side Effects
    Result<T, E> onSuccess(Consumer<T> action);
    Result<T, E> onFailure(Consumer<E> action);
    
    // Fold operation for railway programming
    <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure);
    
    // Value Access
    boolean isSuccess();
    boolean isFailure();
    Optional<T> getValue();
    Optional<E> getError();
    T getOrThrow();
    T getOrElse(T defaultValue);
    T getOrElse(Supplier<T> defaultSupplier);
    
    // Async Operations
    static <T, E> CompletableFuture<Result<T, E>> async(Supplier<Result<T, E>> operation) {
        return CompletableFuture.supplyAsync(operation, 
            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }
    
    // Implementation Records
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return success(mapper.apply(value));
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
            return success(value);
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public Result<T, E> filter(Function<T, Boolean> predicate, E errorOnFalse) {
            return predicate.apply(value) ? this : failure(errorOnFalse);
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
        public <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onSuccess.apply(value);
        }
        
        @Override
        public boolean isSuccess() { return true; }
        
        @Override
        public boolean isFailure() { return false; }
        
        @Override
        public Optional<T> getValue() { return Optional.of(value); }
        
        @Override
        public Optional<E> getError() { return Optional.empty(); }
        
        @Override
        public T getOrThrow() { return value; }
        
        @Override
        public T getOrElse(T defaultValue) { return value; }
        
        @Override
        public T getOrElse(Supplier<T> defaultSupplier) { return value; }
    }
    
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return failure(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
            return failure(errorMapper.apply(error));
        }
        
        @Override
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return failure(error);
        }
        
        @Override
        public Result<T, E> filter(Function<T, Boolean> predicate, E errorOnFalse) {
            return this;
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
        public <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
            return onFailure.apply(error);
        }
        
        @Override
        public boolean isSuccess() { return false; }
        
        @Override
        public boolean isFailure() { return true; }
        
        @Override
        public Optional<T> getValue() { return Optional.empty(); }
        
        @Override
        public Optional<E> getError() { return Optional.of(error); }
        
        @Override
        public T getOrThrow() {
            throw switch (error) {
                case Exception e -> new CompletionException(e);
                case String msg -> new RuntimeException(msg);
                default -> new RuntimeException(error.toString());
            };
        }
        
        @Override
        public T getOrElse(T defaultValue) { return defaultValue; }
        
        @Override
        public T getOrElse(Supplier<T> defaultSupplier) { return defaultSupplier.get(); }
    }
}