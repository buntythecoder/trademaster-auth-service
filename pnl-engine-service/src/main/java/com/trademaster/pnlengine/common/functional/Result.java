package com.trademaster.pnlengine.common.functional;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional Result type for Railway Programming pattern
 * 
 * MANDATORY: Java 24 + Functional Programming + Zero if-else statements
 * 
 * Implements monadic Result type to replace try-catch blocks and null returns
 * with composable, functional error handling patterns.
 * 
 * @param <T> Success value type
 * @param <E> Error value type
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Refactoring)
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Success case containing value
     */
    record Success<T, E>(T value) implements Result<T, E> {}
    
    /**
     * Failure case containing error
     */
    record Failure<T, E>(E error) implements Result<T, E> {}
    
    // ============================================================================
    // FACTORY METHODS
    // ============================================================================
    
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    static <T> Result<T, Exception> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }
    
    // ============================================================================
    // MONADIC OPERATIONS (FUNCTIONAL COMPOSITION)
    // ============================================================================
    
    /**
     * Map success value to new type, preserving failure
     */
    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(mapper.apply(value));
            case Failure<T, E>(var error) -> failure(error);
        };
    }
    
    /**
     * FlatMap for chaining Result-returning operations
     */
    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> failure(error);
        };
    }
    
    /**
     * Map error to new error type, preserving success
     */
    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(value);
            case Failure<T, E>(var error) -> failure(mapper.apply(error));
        };
    }
    
    /**
     * Filter success values with predicate
     */
    default Result<T, E> filter(Predicate<T> predicate, Supplier<E> errorSupplier) {
        return switch (this) {
            case Success<T, E>(var value) -> predicate.test(value) ? 
                this : failure(errorSupplier.get());
            case Failure<T, E> failure -> failure;
        };
    }
    
    /**
     * Recover from failure with alternative value
     */
    default T recover(Function<E, T> recovery) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }
    
    /**
     * Recover from failure with alternative Result
     */
    default Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }
    
    // ============================================================================
    // SIDE EFFECTS (FUNCTIONAL APPROACH)
    // ============================================================================
    
    /**
     * Execute side effect on success, return original Result
     */
    default Result<T, E> peek(Consumer<T> action) {
        return switch (this) {
            case Success<T, E>(var value) -> {
                action.accept(value);
                yield this;
            }
            case Failure<T, E> failure -> failure;
        };
    }
    
    /**
     * Execute side effect on failure, return original Result
     */
    default Result<T, E> peekError(Consumer<E> action) {
        return switch (this) {
            case Success<T, E> success -> success;
            case Failure<T, E>(var error) -> {
                action.accept(error);
                yield this;
            }
        };
    }
    
    // ============================================================================
    // QUERY OPERATIONS
    // ============================================================================
    
    /**
     * Check if Result is success
     */
    default boolean isSuccess() {
        return switch (this) {
            case Success<T, E> ignored -> true;
            case Failure<T, E> ignored -> false;
        };
    }
    
    /**
     * Check if Result is failure
     */
    default boolean isFailure() {
        return !isSuccess();
    }
    
    /**
     * Get success value as Optional
     */
    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T, E>(var value) -> Optional.of(value);
            case Failure<T, E> ignored -> Optional.empty();
        };
    }
    
    /**
     * Get error as Optional
     */
    default Optional<E> getError() {
        return switch (this) {
            case Success<T, E> ignored -> Optional.empty();
            case Failure<T, E>(var error) -> Optional.of(error);
        };
    }
    
    /**
     * Get success value or throw exception
     */
    default T getOrThrow() {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> {
                throw switch (error) {
                    case RuntimeException re -> re;
                    case Exception e -> new RuntimeException(e);
                    default -> new RuntimeException(error.toString());
                };
            }
        };
    }
    
    /**
     * Get success value or default
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E> ignored -> defaultValue;
        };
    }
    
    /**
     * Get success value or compute default
     */
    default T getOrElse(Supplier<T> defaultSupplier) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E> ignored -> defaultSupplier.get();
        };
    }
}