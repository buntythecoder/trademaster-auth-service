package com.trademaster.agentos.security.validator;

import com.trademaster.agentos.security.model.Result;
import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.model.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Input Validator - Functional validation chains for secure input processing.
 * Implements comprehensive input validation and sanitization.
 */
@Slf4j
@Component
public class InputValidator {
    
    // Common validation patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    private static final Pattern ALPHANUMERIC_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9]+$");
    
    private static final Pattern UUID_PATTERN = 
        Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    
    // SQL injection patterns
    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
        Pattern.compile("('.+--)|(--)|(;)|(\\|\\|)|(\\*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript)", 
            Pattern.CASE_INSENSITIVE)
    );
    
    // XSS patterns
    private static final List<Pattern> XSS_PATTERNS = List.of(
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Validate input with security context.
     */
    public <T> Result<T, SecurityError> validate(T input, SecurityContext context) {
        if (input == null) {
            return Result.failure(SecurityError.authorizationDenied(
                "Null input not allowed", context.correlationId()));
        }
        
        // Apply validation based on input type
        return switch (input) {
            case String s -> validateString(s, context).map(v -> (T) v);
            case Map<?, ?> m -> validateMap(m, context).map(v -> (T) v);
            case Collection<?> c -> validateCollection(c, context).map(v -> (T) v);
            default -> validateObject(input, context);
        };
    }
    
    /**
     * Create validation chain builder.
     */
    public <T> ValidationChain<T> chain(Class<T> type) {
        return new ValidationChain<>();
    }
    
    /**
     * Validate string input.
     */
    private Result<String, SecurityError> validateString(String input, SecurityContext context) {
        // Check for SQL injection
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("SQL injection attempt detected: correlationId={}", context.correlationId());
                return Result.failure(SecurityError.authorizationDenied(
                    "Invalid input detected", context.correlationId()));
            }
        }
        
        // Check for XSS
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("XSS attempt detected: correlationId={}", context.correlationId());
                return Result.failure(SecurityError.authorizationDenied(
                    "Invalid input detected", context.correlationId()));
            }
        }
        
        return Result.success(sanitizeString(input));
    }
    
    /**
     * Validate map input.
     */
    private Result<Map<?, ?>, SecurityError> validateMap(Map<?, ?> input, SecurityContext context) {
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            // Validate keys
            if (entry.getKey() instanceof String key) {
                var keyResult = validateString(key, context);
                if (keyResult.isFailure()) {
                    return Result.failure(((Result.Failure<?, SecurityError>) keyResult).error());
                }
            }
            
            // Validate values
            if (entry.getValue() instanceof String value) {
                var valueResult = validateString(value, context);
                if (valueResult.isFailure()) {
                    return Result.failure(((Result.Failure<?, SecurityError>) valueResult).error());
                }
            }
        }
        
        return Result.success(input);
    }
    
    /**
     * Validate collection input.
     */
    private Result<Collection<?>, SecurityError> validateCollection(Collection<?> input, SecurityContext context) {
        for (Object item : input) {
            if (item instanceof String s) {
                var result = validateString(s, context);
                if (result.isFailure()) {
                    return Result.failure(((Result.Failure<?, SecurityError>) result).error());
                }
            }
        }
        
        return Result.success(input);
    }
    
    /**
     * Validate generic object.
     */
    private <T> Result<T, SecurityError> validateObject(T input, SecurityContext context) {
        // For complex objects, would use reflection or specific validators
        return Result.success(input);
    }
    
    /**
     * Sanitize string input.
     */
    private String sanitizeString(String input) {
        return input
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#x27;")
            .replaceAll("/", "&#x2F;");
    }
    
    /**
     * Functional validation chain builder.
     */
    public class ValidationChain<T> {
        private final List<ValidationRule<T>> rules = new ArrayList<>();
        
        public ValidationChain<T> notNull(String errorMessage) {
            rules.add(new ValidationRule<>(
                Objects::nonNull,
                errorMessage
            ));
            return this;
        }
        
        public ValidationChain<T> matches(Predicate<T> predicate, String errorMessage) {
            rules.add(new ValidationRule<>(predicate, errorMessage));
            return this;
        }
        
        public ValidationChain<T> transform(Function<T, T> transformer) {
            rules.add(new ValidationRule<>(
                t -> true,
                null,
                transformer
            ));
            return this;
        }
        
        public ValidationChain<T> stringPattern(Pattern pattern, String errorMessage) {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && pattern.matcher(s).matches(),
                errorMessage
            ));
            return this;
        }
        
        public ValidationChain<T> stringLength(int min, int max, String errorMessage) {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && s.length() >= min && s.length() <= max,
                errorMessage
            ));
            return this;
        }
        
        public ValidationChain<T> email() {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && EMAIL_PATTERN.matcher(s).matches(),
                "Invalid email format"
            ));
            return this;
        }
        
        public ValidationChain<T> phone() {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && PHONE_PATTERN.matcher(s).matches(),
                "Invalid phone number format"
            ));
            return this;
        }
        
        public ValidationChain<T> alphanumeric() {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && ALPHANUMERIC_PATTERN.matcher(s).matches(),
                "Only alphanumeric characters allowed"
            ));
            return this;
        }
        
        public ValidationChain<T> uuid() {
            rules.add(new ValidationRule<>(
                t -> t instanceof String s && UUID_PATTERN.matcher(s).matches(),
                "Invalid UUID format"
            ));
            return this;
        }
        
        public ValidationChain<T> noSqlInjection() {
            rules.add(new ValidationRule<>(
                t -> {
                    if (t instanceof String s) {
                        return SQL_INJECTION_PATTERNS.stream()
                            .noneMatch(pattern -> pattern.matcher(s).find());
                    }
                    return true;
                },
                "Invalid characters detected"
            ));
            return this;
        }
        
        public ValidationChain<T> noXss() {
            rules.add(new ValidationRule<>(
                t -> {
                    if (t instanceof String s) {
                        return XSS_PATTERNS.stream()
                            .noneMatch(pattern -> pattern.matcher(s).find());
                    }
                    return true;
                },
                "Invalid content detected"
            ));
            return this;
        }
        
        public Result<T, ValidationError> validate(T input) {
            T current = input;
            List<String> errors = new ArrayList<>();
            
            for (ValidationRule<T> rule : rules) {
                // Apply transformation if present
                if (rule.transformer != null) {
                    current = rule.transformer.apply(current);
                }
                
                // Check validation
                if (!rule.predicate.test(current)) {
                    errors.add(rule.errorMessage);
                }
            }
            
            if (!errors.isEmpty()) {
                return Result.failure(new ValidationError(errors));
            }
            
            return Result.success(current);
        }
    }
    
    /**
     * Validation rule.
     */
    private record ValidationRule<T>(
        Predicate<T> predicate,
        String errorMessage,
        Function<T, T> transformer
    ) {
        ValidationRule(Predicate<T> predicate, String errorMessage) {
            this(predicate, errorMessage, null);
        }
    }
    
    /**
     * Validation error with multiple error messages.
     */
    public record ValidationError(List<String> errors) {
        @Override
        public String toString() {
            return errors.stream().collect(Collectors.joining(", "));
        }
    }
}