package com.trademaster.agentos.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional Validation Framework for TradeMaster Agent OS
 * 
 * Accumulates validation errors and provides functional composition for validation logic.
 * Supports validation combinators and monadic operations.
 * 
 * @param <T> The type being validated
 */
public sealed interface ValidationResult<T> permits ValidationResult.Valid, ValidationResult.Invalid {
    
    /**
     * Valid case containing the validated value
     */
    record Valid<T>(T value) implements ValidationResult<T> {
        public Valid {
            Objects.requireNonNull(value, "Valid value cannot be null");
        }
    }
    
    /**
     * Invalid case containing accumulated validation errors
     */
    record Invalid<T>(List<String> errors) implements ValidationResult<T> {
        public Invalid {
            Objects.requireNonNull(errors, "Error list cannot be null");
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("Error list cannot be empty for Invalid result");
            }
        }
        
        public Invalid(String error) {
            this(List.of(error));
        }
    }
    
    /**
     * Creates a valid result
     */
    static <T> ValidationResult<T> valid(T value) {
        return new Valid<>(value);
    }
    
    /**
     * Creates an invalid result with a single error
     */
    static <T> ValidationResult<T> invalid(String error) {
        return new Invalid<>(error);
    }
    
    /**
     * Creates an invalid result with multiple errors
     */
    static <T> ValidationResult<T> invalid(List<String> errors) {
        return new Invalid<>(List.copyOf(errors));
    }
    
    /**
     * Creates a validation result from a predicate
     */
    static <T> ValidationResult<T> of(T value, Predicate<T> predicate, String errorMessage) {
        return predicate.test(value) ? valid(value) : invalid(errorMessage);
    }
    
    /**
     * Creates a validation result from a condition
     */
    static <T> ValidationResult<T> when(T value, boolean condition, String errorMessage) {
        return condition ? valid(value) : invalid(errorMessage);
    }
    
    /**
     * Checks if this validation is valid
     */
    default boolean isValid() {
        return this instanceof Valid<T>;
    }
    
    /**
     * Checks if this validation is invalid
     */
    default boolean isInvalid() {
        return this instanceof Invalid<T>;
    }
    
    /**
     * Gets the validated value if valid
     */
    default T getValue() {
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> throw new IllegalStateException("Cannot get value from invalid validation: " + errors);
        };
    }
    
    /**
     * Gets the validation errors if invalid
     */
    default List<String> getErrors() {
        return switch (this) {
            case Valid(var value) -> List.of();
            case Invalid(var errors) -> List.copyOf(errors);
        };
    }
    
    /**
     * Adds another validation check
     */
    default ValidationResult<T> validate(Predicate<T> predicate, String errorMessage) {
        return switch (this) {
            case Valid(var value) -> predicate.test(value) ? this : invalid(errorMessage);
            case Invalid(var errors) -> {
                // Continue validation even if already invalid to collect all errors
                List<String> newErrors = new ArrayList<>(errors);
                newErrors.add(errorMessage);
                yield invalid(newErrors);
            }
        };
    }
    
    /**
     * Adds a validation check with access to the current value
     */
    default ValidationResult<T> validateWith(Function<T, Boolean> validator, String errorMessage) {
        return switch (this) {
            case Valid(var value) -> validator.apply(value) ? this : invalid(errorMessage);
            case Invalid(var errors) -> {
                List<String> newErrors = new ArrayList<>(errors);
                newErrors.add(errorMessage);
                yield invalid(newErrors);
            }
        };
    }
    
    /**
     * Maps the validated value to another type
     */
    default <U> ValidationResult<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Valid(var value) -> valid(mapper.apply(value));
            case Invalid(var errors) -> invalid(errors);
        };
    }
    
    /**
     * FlatMap for monadic composition of validations
     */
    default <U> ValidationResult<U> flatMap(Function<T, ValidationResult<U>> mapper) {
        return switch (this) {
            case Valid(var value) -> mapper.apply(value);
            case Invalid(var errors) -> invalid(errors);
        };
    }
    
    /**
     * Combines this validation with another, accumulating all errors
     */
    default <U, R> ValidationResult<R> combine(ValidationResult<U> other, Function<T, Function<U, R>> combiner) {
        return switch (this) {
            case Valid(var thisValue) -> switch (other) {
                case Valid(var otherValue) -> valid(combiner.apply(thisValue).apply(otherValue));
                case Invalid(var otherErrors) -> invalid(otherErrors);
            };
            case Invalid(var thisErrors) -> switch (other) {
                case Valid(var otherValue) -> invalid(thisErrors);
                case Invalid(var otherErrors) -> {
                    List<String> allErrors = new ArrayList<>(thisErrors);
                    allErrors.addAll(otherErrors);
                    yield invalid(allErrors);
                }
            };
        };
    }
    
    /**
     * Accumulates multiple validations
     */
    @SafeVarargs
    static <T> ValidationResult<List<T>> sequence(ValidationResult<T>... validations) {
        return sequence(List.of(validations));
    }
    
    /**
     * Accumulates multiple validations from a list
     */
    static <T> ValidationResult<List<T>> sequence(List<ValidationResult<T>> validations) {
        List<T> values = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();
        
        for (ValidationResult<T> validation : validations) {
            switch (validation) {
                case Valid(var value) -> values.add(value);
                case Invalid(var errors) -> allErrors.addAll(errors);
            }
        }
        
        return allErrors.isEmpty() ? valid(List.copyOf(values)) : invalid(allErrors);
    }
    
    /**
     * Converts to a Result type
     */
    default Result<T, List<String>> toResult() {
        return switch (this) {
            case Valid(var value) -> Result.success(value);
            case Invalid(var errors) -> Result.failure(errors);
        };
    }
    
    /**
     * Converts to a Result with a single error message
     */
    default Result<T, String> toResultWithSingleError() {
        return switch (this) {
            case Valid(var value) -> Result.success(value);
            case Invalid(var errors) -> Result.failure(String.join("; ", errors));
        };
    }
    
    /**
     * Gets the value or throws an exception with all validation errors
     */
    default T getValueOrThrow() {
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> throw new IllegalArgumentException("Validation failed: " + String.join("; ", errors));
        };
    }
    
    /**
     * Gets the value or returns a default
     */
    default T getValueOrDefault(T defaultValue) {
        return switch (this) {
            case Valid(var value) -> value;
            case Invalid(var errors) -> defaultValue;
        };
    }
    
    @Override
    String toString();
}

