package com.trademaster.auth.security;

import java.util.Optional;
import java.util.function.Function;

/**
 * Security Result - Functional result type for security operations
 *
 * MANDATORY: Result Pattern - Rule #11
 * MANDATORY: Railway Programming - Rule #11
 * MANDATORY: Immutability - Rule #9
 */
public sealed interface SecurityResult<T> permits SecurityResult.Success, SecurityResult.Failure {

    /**
     * Success implementation
     */
    record Success<T>(T value, Optional<SecurityContext> context) implements SecurityResult<T> {
        public Success(T value, SecurityContext context) {
            this(value, Optional.ofNullable(context));
        }

        public Success(T value) {
            this(value, Optional.empty());
        }
    }

    /**
     * Failure implementation
     */
    record Failure<T>(SecurityError error, String message, Optional<SecurityContext> context) implements SecurityResult<T> {
        public Failure(SecurityError error, String message) {
            this(error, message, Optional.empty());
        }
    }

    /**
     * Factory methods
     */
    static <T> SecurityResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> SecurityResult<T> success(T value, SecurityContext context) {
        return new Success<>(value, context);
    }

    static <T> SecurityResult<T> failure(SecurityError error, String message) {
        return new Failure<>(error, message);
    }

    static <T> SecurityResult<T> failure(SecurityError error, String message, SecurityContext context) {
        return new Failure<>(error, message, Optional.ofNullable(context));
    }

    /**
     * Functional operations using pattern matching - Rule #14
     */
    default boolean isSuccess() {
        return switch (this) {
            case Success<T> success -> true;
            case Failure<T> failure -> false;
        };
    }

    default boolean isFailure() {
        return !isSuccess();
    }

    default Optional<T> getValue() {
        return switch (this) {
            case Success<T> success -> Optional.of(success.value());
            case Failure<T> failure -> Optional.empty();
        };
    }

    default Optional<SecurityError> getError() {
        return switch (this) {
            case Success<T> success -> Optional.empty();
            case Failure<T> failure -> Optional.of(failure.error());
        };
    }

    default Optional<String> getMessage() {
        return switch (this) {
            case Success<T> success -> Optional.empty();
            case Failure<T> failure -> Optional.of(failure.message());
        };
    }

    default Optional<SecurityContext> getContext() {
        return switch (this) {
            case Success<T> success -> success.context();
            case Failure<T> failure -> failure.context();
        };
    }

    /**
     * Functional transformations - Railway Programming
     */
    default <U> SecurityResult<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T> success -> SecurityResult.success(mapper.apply(success.value()), success.context().orElse(null));
            case Failure<T> failure -> new Failure<>(failure.error(), failure.message(), failure.context());
        };
    }

    default <U> SecurityResult<U> flatMap(Function<T, SecurityResult<U>> mapper) {
        return switch (this) {
            case Success<T> success -> mapper.apply(success.value());
            case Failure<T> failure -> new Failure<>(failure.error(), failure.message(), failure.context());
        };
    }

    default SecurityResult<T> mapError(Function<SecurityError, SecurityError> errorMapper) {
        return switch (this) {
            case Success<T> success -> success;
            case Failure<T> failure -> new Failure<>(errorMapper.apply(failure.error()), failure.message(), failure.context());
        };
    }

    default T orElse(T defaultValue) {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> defaultValue;
        };
    }

    default T orElseThrow() {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> throw new SecurityException(failure.message());
        };
    }

    default T orElseThrow(Function<String, RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> throw exceptionMapper.apply(failure.message());
        };
    }
}