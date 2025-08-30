package com.trademaster.marketdata.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional Chain of Responsibility pattern for validation
 * Eliminates if-else validation logic with composable validation chains
 */
@FunctionalInterface
public interface ValidationChain<T> {
    
    Result<T, String> validate(T input);
    
    // Chain composition
    default ValidationChain<T> andThen(ValidationChain<T> next) {
        return input -> {
            Result<T, String> result = this.validate(input);
            return result.isSuccess() ? next.validate(input) : result;
        };
    }
    
    default ValidationChain<T> or(ValidationChain<T> alternative) {
        return input -> {
            Result<T, String> result = this.validate(input);
            return result.isSuccess() ? result : alternative.validate(input);
        };
    }
    
    // Static factory methods
    static <T> ValidationChain<T> of(Predicate<T> predicate, String errorMessage) {
        return input -> predicate.test(input) ? 
            Result.success(input) : 
            Result.failure(errorMessage);
    }
    
    static <T> ValidationChain<T> notNull(String errorMessage) {
        return of(input -> input != null, errorMessage);
    }
    
    static <T> ValidationChain<T> always() {
        return Result::success;
    }
    
    static <T> ValidationChain<T> never(String errorMessage) {
        return input -> Result.failure(errorMessage);
    }
    
    // Combine multiple validators
    static <T> ValidationChain<T> all(ValidationChain<T>... validators) {
        return Arrays.stream(validators)
            .reduce(ValidationChain.always(), ValidationChain::andThen);
    }
    
    static <T> ValidationChain<T> any(ValidationChain<T>... validators) {
        return Arrays.stream(validators)
            .reduce(ValidationChain.never("No validator passed"), ValidationChain::or);
    }
    
    // Transform validation result
    default <U> ValidationChain<U> contramap(Function<U, T> mapper) {
        return input -> this.validate(mapper.apply(input))
            .map(validatedValue -> input); // Return original input type
    }
    
    // Conditional validation
    default ValidationChain<T> when(Predicate<T> condition) {
        return input -> condition.test(input) ? 
            this.validate(input) : 
            Result.success(input);
    }
    
    default ValidationChain<T> unless(Predicate<T> condition) {
        return when(condition.negate());
    }
    
    // Validation builder
    static <T> ValidationChainBuilder<T> builder() {
        return new ValidationChainBuilder<>();
    }
    
    class ValidationChainBuilder<T> {
        private final List<ValidationChain<T>> validators = new ArrayList<>();
        
        public ValidationChainBuilder<T> add(ValidationChain<T> validator) {
            validators.add(validator);
            return this;
        }
        
        public ValidationChainBuilder<T> add(Predicate<T> predicate, String errorMessage) {
            return add(ValidationChain.of(predicate, errorMessage));
        }
        
        public ValidationChainBuilder<T> notNull(String errorMessage) {
            return add(ValidationChain.notNull(errorMessage));
        }
        
        public ValidationChainBuilder<T> when(Predicate<T> condition, ValidationChain<T> validator) {
            return add(validator.when(condition));
        }
        
        public ValidationChain<T> build() {
            return validators.stream()
                .reduce(ValidationChain.always(), ValidationChain::andThen);
        }
        
        // Build with failure accumulation
        public ValidationChain<T> buildAccumulating() {
            return input -> {
                List<String> errors = new ArrayList<>();
                T currentValue = input;
                
                for (ValidationChain<T> validator : validators) {
                    Result<T, String> result = validator.validate(currentValue);
                    if (result.isFailure()) {
                        errors.add(result.fold(error -> error, success -> "Unknown error"));
                    } else {
                        currentValue = result.orElse(currentValue);
                    }
                }
                
                return errors.isEmpty() ? 
                    Result.success(currentValue) : 
                    Result.failure(String.join("; ", errors));
            };
        }
    }
    
    // Pre-built common validators
    class Common {
        
        public static ValidationChain<String> notBlank() {
            return of(s -> s != null && !s.trim().isEmpty(), "Value cannot be blank");
        }
        
        public static ValidationChain<String> maxLength(int max) {
            return of(s -> s == null || s.length() <= max, 
                "Value cannot exceed " + max + " characters");
        }
        
        public static ValidationChain<String> minLength(int min) {
            return of(s -> s != null && s.length() >= min, 
                "Value must be at least " + min + " characters");
        }
        
        public static ValidationChain<String> matches(String regex) {
            return of(s -> s != null && s.matches(regex), 
                "Value must match pattern: " + regex);
        }
        
        public static <T extends Comparable<T>> ValidationChain<T> min(T minimum) {
            return of(value -> value != null && value.compareTo(minimum) >= 0,
                "Value must be at least " + minimum);
        }
        
        public static <T extends Comparable<T>> ValidationChain<T> max(T maximum) {
            return of(value -> value != null && value.compareTo(maximum) <= 0,
                "Value must be at most " + maximum);
        }
        
        public static <T extends Comparable<T>> ValidationChain<T> between(T min, T max) {
            return min(min).andThen(max(max));
        }
        
        public static ValidationChain<String> alphanumeric() {
            return matches("[A-Za-z0-9]+");
        }
        
        public static ValidationChain<String> numeric() {
            return matches("\\d+");
        }
        
        public static ValidationChain<String> alphabetic() {
            return matches("[A-Za-z]+");
        }
        
        public static ValidationChain<String> uppercase() {
            return matches("[A-Z]+");
        }
        
        public static ValidationChain<String> email() {
            return matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        }
        
        public static <T> ValidationChain<List<T>> notEmpty() {
            return of(list -> list != null && !list.isEmpty(), "List cannot be empty");
        }
        
        public static <T> ValidationChain<List<T>> maxSize(int maxSize) {
            return of(list -> list == null || list.size() <= maxSize, 
                "List cannot contain more than " + maxSize + " elements");
        }
    }
}