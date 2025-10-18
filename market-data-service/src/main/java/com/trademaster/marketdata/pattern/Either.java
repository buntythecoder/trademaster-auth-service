package com.trademaster.marketdata.pattern;

import com.trademaster.common.functional.Result;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Either type for functional error handling
 * Represents a value that can be one of two types - Left (error) or Right (success)
 */
public sealed interface Either<L, R> permits Either.Left, Either.Right {
    
    record Left<L, R>(L value) implements Either<L, R> {}
    record Right<L, R>(R value) implements Either<L, R> {}
    
    // Factory methods
    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }
    
    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }
    
    // Safe execution
    static <R> Either<Exception, R> safely(Supplier<R> operation) {
        try {
            return right(operation.get());
        } catch (Exception e) {
            return left(e);
        }
    }
    
    // Type checks
    default boolean isLeft() {
        return this instanceof Left;
    }
    
    default boolean isRight() {
        return this instanceof Right;
    }
    
    // Value extraction
    default L getLeft() {
        return switch (this) {
            case Left(var value) -> value;
            case Right(var value) -> throw new NoSuchElementException("Either is Right");
        };
    }
    
    default R getRight() {
        return switch (this) {
            case Left(var value) -> throw new NoSuchElementException("Either is Left");
            case Right(var value) -> value;
        };
    }
    
    default Optional<L> leftOption() {
        return switch (this) {
            case Left(var value) -> Optional.of(value);
            case Right(var value) -> Optional.empty();
        };
    }
    
    default Optional<R> rightOption() {
        return switch (this) {
            case Left(var value) -> Optional.empty();
            case Right(var value) -> Optional.of(value);
        };
    }
    
    // Monadic operations
    default <T> Either<L, T> map(Function<R, T> mapper) {
        return switch (this) {
            case Left(var value) -> left(value);
            case Right(var value) -> right(mapper.apply(value));
        };
    }
    
    default <T> Either<T, R> mapLeft(Function<L, T> mapper) {
        return switch (this) {
            case Left(var value) -> left(mapper.apply(value));
            case Right(var value) -> right(value);
        };
    }
    
    default <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper) {
        return switch (this) {
            case Left(var value) -> left(value);
            case Right(var value) -> mapper.apply(value);
        };
    }
    
    // Filtering
    default Either<L, R> filter(Predicate<R> predicate, L leftValue) {
        return switch (this) {
            case Left(var value) -> this;
            case Right(var value) -> predicate.test(value) ? this : left(leftValue);
        };
    }
    
    // Side effects
    default Either<L, R> peek(Consumer<R> action) {
        if (this instanceof Right(var value)) {
            action.accept(value);
        }
        return this;
    }
    
    default Either<L, R> peekLeft(Consumer<L> action) {
        if (this instanceof Left(var value)) {
            action.accept(value);
        }
        return this;
    }
    
    // Folding
    default <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
        return switch (this) {
            case Left(var value) -> leftMapper.apply(value);
            case Right(var value) -> rightMapper.apply(value);
        };
    }
    
    // Convert to Result
    default Result<R, L> toResult() {
        return switch (this) {
            case Left(var value) -> Result.failure(value);
            case Right(var value) -> Result.success(value);
        };
    }
    
    // Swap left and right
    default Either<R, L> swap() {
        return switch (this) {
            case Left(var value) -> right(value);
            case Right(var value) -> left(value);
        };
    }
    
    // Equality and toString
    @Override
    boolean equals(Object obj);
    
    @Override
    int hashCode();
    
    @Override
    String toString();
}