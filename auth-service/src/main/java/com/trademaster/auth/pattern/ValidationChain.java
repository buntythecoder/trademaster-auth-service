package com.trademaster.auth.pattern;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional Validation Chain Pattern
 * 
 * Replaces if-else validation logic with composable functional validators
 * following TradeMaster Advanced Design Patterns standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface ValidationChain<T> {
    
    Result<T, String> validate(T input);
    
    /**
     * Chain multiple validations together
     */
    default ValidationChain<T> andThen(ValidationChain<T> next) {
        return input -> {
            Result<T, String> result = this.validate(input);
            return result.isSuccess() ? next.validate(input) : result;
        };
    }
    
    /**
     * Create a validation from a predicate
     */
    static <T> ValidationChain<T> of(Predicate<T> predicate, String errorMessage) {
        return input -> predicate.test(input) ? 
            Result.success(input) : 
            Result.failure(errorMessage);
    }
    
    /**
     * Create a validation that always passes
     */
    static <T> ValidationChain<T> valid() {
        return Result::success;
    }
    
    /**
     * Create a validation that always fails
     */
    static <T> ValidationChain<T> invalid(String errorMessage) {
        return input -> Result.failure(errorMessage);
    }
    
    /**
     * Optional-based validation for nullable inputs
     */
    static <T> ValidationChain<T> notNull(String errorMessage) {
        return input -> Optional.ofNullable(input)
            .map(Result::<T, String>success)
            .orElse(Result.failure(errorMessage));
    }
    
    /**
     * String validation utilities
     */
    static ValidationChain<String> notBlank(String errorMessage) {
        return of(s -> s != null && !s.trim().isEmpty(), errorMessage);
    }
    
    static ValidationChain<String> maxLength(int maxLength, String errorMessage) {
        return of(s -> s != null && s.length() <= maxLength, errorMessage);
    }
    
    static ValidationChain<String> minLength(int minLength, String errorMessage) {
        return of(s -> s != null && s.length() >= minLength, errorMessage);
    }
    
    static ValidationChain<String> matches(String pattern, String errorMessage) {
        return of(s -> s != null && s.matches(pattern), errorMessage);
    }
    
    /**
     * Numeric validation utilities
     */
    static ValidationChain<Number> positive(String errorMessage) {
        return of(n -> n != null && n.doubleValue() > 0, errorMessage);
    }
    
    static ValidationChain<Number> nonNegative(String errorMessage) {
        return of(n -> n != null && n.doubleValue() >= 0, errorMessage);
    }
    
    static ValidationChain<Number> range(double min, double max, String errorMessage) {
        return of(n -> n != null && n.doubleValue() >= min && n.doubleValue() <= max, errorMessage);
    }
    
    /**
     * Transform validation result
     */
    default <U> ValidationChain<T> map(Function<T, U> mapper) {
        return input -> this.validate(input).map(mapper).map(u -> input);
    }
}