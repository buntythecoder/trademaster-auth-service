package com.trademaster.marketdata.pattern;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Functional validation combinators for composable validation chains
 * Eliminates if-else validation logic with functional approach
 */
public final class Validation<T> {
    private final T value;
    private final List<String> errors;
    
    private Validation(T value, List<String> errors) {
        this.value = value;
        this.errors = List.copyOf(errors);
    }
    
    // Factory methods
    public static <T> Validation<T> valid(T value) {
        return new Validation<>(value, List.of());
    }
    
    public static <T> Validation<T> invalid(String error) {
        return new Validation<>(null, List.of(error));
    }
    
    public static <T> Validation<T> invalid(List<String> errors) {
        return new Validation<>(null, errors);
    }
    
    // Predicate-based validation factory
    public static <T> Function<T, Validation<T>> validate(
            Predicate<T> predicate, String errorMessage) {
        return value -> predicate.test(value) ? 
            valid(value) : invalid(errorMessage);
    }
    
    // State queries
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public boolean isInvalid() {
        return !errors.isEmpty();
    }
    
    public T getValue() {
        if (isInvalid()) {
            throw new IllegalStateException("Cannot get value from invalid validation: " + errors);
        }
        return value;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    // Monadic operations
    public <U> Validation<U> map(Function<T, U> mapper) {
        return isValid() ? 
            valid(mapper.apply(value)) : 
            invalid(errors);
    }
    
    public <U> Validation<U> flatMap(Function<T, Validation<U>> mapper) {
        if (isInvalid()) {
            return invalid(errors);
        }
        
        Validation<U> result = mapper.apply(value);
        if (result.isValid()) {
            return result;
        }
        
        List<String> combinedErrors = new ArrayList<>(errors);
        combinedErrors.addAll(result.errors);
        return invalid(combinedErrors);
    }
    
    // Validation chaining
    public Validation<T> and(Function<T, Validation<T>> validator) {
        if (isInvalid()) {
            return this;
        }
        
        Validation<T> result = validator.apply(value);
        if (result.isValid()) {
            return this; // Keep original value
        }
        
        return invalid(result.errors);
    }
    
    public Validation<T> also(Function<T, Validation<T>> validator) {
        if (isInvalid()) {
            return this;
        }
        
        Validation<T> result = validator.apply(value);
        if (result.isValid()) {
            return this;
        }
        
        List<String> combinedErrors = new ArrayList<>(errors);
        combinedErrors.addAll(result.errors);
        return invalid(combinedErrors);
    }
    
    // Combine multiple validations (accumulate all errors)
    public static <T> Validation<T> combine(List<Validation<T>> validations) {
        List<String> allErrors = validations.stream()
            .flatMap(v -> v.errors.stream())
            .collect(Collectors.toList());
            
        if (allErrors.isEmpty()) {
            return validations.stream()
                .filter(Validation::isValid)
                .findFirst()
                .orElse(invalid(List.of("No valid values found")));
        }
        
        return invalid(allErrors);
    }
    
    // Apply validation with multiple validators
    public static <T> Validation<T> validateWith(T value, List<Function<T, Validation<T>>> validators) {
        return validators.stream()
            .reduce(valid(value), 
                (acc, validator) -> acc.flatMap(validator),
                (v1, v2) -> v1.isValid() ? v2 : v1);
    }
    
    // Convert to Result type
    public Result<T, List<String>> toResult() {
        return isValid() ? 
            Result.success(value) : 
            Result.failure(errors);
    }
    
    // Convert from Result type
    public static <T> Validation<T> fromResult(Result<T, String> result) {
        return result.fold(
            error -> invalid(error),
            value -> valid(value)
        );
    }
    
    // Pattern matching
    public <R> R fold(Function<List<String>, R> onInvalid, Function<T, R> onValid) {
        return isValid() ? onValid.apply(value) : onInvalid.apply(errors);
    }
    
    @Override
    public String toString() {
        return isValid() ? 
            "Valid(" + value + ")" : 
            "Invalid(" + String.join(", ", errors) + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Validation<?> that = (Validation<?>) obj;
        return Objects.equals(value, that.value) && 
               Objects.equals(errors, that.errors);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, errors);
    }
}