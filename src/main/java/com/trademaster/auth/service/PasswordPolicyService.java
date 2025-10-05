package com.trademaster.auth.service;

import com.trademaster.auth.context.PasswordValidationContext;
import com.trademaster.auth.pattern.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Password Policy Service for enforcing password security requirements
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class PasswordPolicyService {

    // Password policy patterns
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    /**
     * Validate password against policy requirements
     */
    public Result<Boolean, String> validatePassword(String password, String email) {
        return validatePasswordWithContext(
            PasswordValidationContext.builder()
                .password(password)
                .email(email)
                .requireSpecialCharacters(true)
                .requireNumbers(true)
                .requireUppercase(true)
                .requireLowercase(true)
                .minimumLength(8)
                .maximumLength(128)
                .passwordHistorySize(5)
                .build()
        );
    }
    
    /**
     * Validate password with full context
     */
    public Result<Boolean, String> validatePasswordWithContext(PasswordValidationContext context) {
        return Stream.of(
                validateNotEmpty(context),
                validateLength(context),
                validateComplexity(context),
                validateEmailConstraint(context),
                validateCommonPasswords(context)
            )
            .filter(Result::isFailure)
            .findFirst()
            .map(result -> (Result<Boolean, String>) result)
            .orElse(Result.success(true));
    }
    
    private Result<Boolean, String> validateNotEmpty(PasswordValidationContext context) {
        return Optional.ofNullable(context.getPassword())
            .filter(p -> !p.trim().isEmpty())
            .map(p -> Result.<Boolean, String>success(true))
            .orElse(Result.failure("Password cannot be empty"));
    }
    
    private Result<Boolean, String> validateLength(PasswordValidationContext context) {
        String password = context.getPassword();
        int minLength = context.getMinimumLength();
        int maxLength = context.getMaximumLength();

        return Optional.of(password.length())
                .filter(len -> len >= minLength)
                .filter(len -> len <= maxLength)
                .<Result<Boolean, String>>map(len -> Result.success(true))
                .orElseGet(() -> password.length() < minLength
                        ? Result.<Boolean, String>failure("Password must be at least " + minLength + " characters long")
                        : Result.<Boolean, String>failure("Password cannot exceed " + maxLength + " characters"));
    }
    
    private Result<Boolean, String> validateComplexity(PasswordValidationContext context) {
        String password = context.getPassword();

        List<String> errors = Stream.of(
                createComplexityCheck(context.isRequireUppercase(), UPPERCASE_PATTERN, password, "uppercase letter"),
                createComplexityCheck(context.isRequireLowercase(), LOWERCASE_PATTERN, password, "lowercase letter"),
                createComplexityCheck(context.isRequireNumbers(), DIGIT_PATTERN, password, "number"),
                createComplexityCheck(context.isRequireSpecialCharacters(), SPECIAL_CHAR_PATTERN, password, "special character")
        )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return Optional.of(errors)
                .filter(List::isEmpty)
                .<Result<Boolean, String>>map(e -> Result.success(true))
                .orElse(Result.<Boolean, String>failure("Password must contain at least one " + String.join(", ", errors)));
    }

    /**
     * Helper method for complexity checks using functional approach
     */
    private Optional<String> createComplexityCheck(boolean required, java.util.regex.Pattern pattern,
                                                   String password, String requirement) {
        return Optional.of(required)
                .filter(Boolean::booleanValue)
                .filter(req -> !pattern.matcher(password).find())
                .map(req -> requirement);
    }
    
    private Result<Boolean, String> validateEmailConstraint(PasswordValidationContext context) {
        String password = context.getPassword().toLowerCase();

        // Check email constraint using functional approach
        Result<Boolean, String> emailCheck = Optional.ofNullable(context.getEmail())
                .filter(email -> !email.isEmpty())
                .map(email -> email.split("@")[0].toLowerCase())
                .filter(localPart -> password.contains(localPart))
                .map(localPart -> Result.<Boolean, String>failure("Password cannot contain parts of your email address"))
                .orElse(Result.success(true));

        // Check username constraint using functional composition
        return emailCheck.isSuccess()
                ? Optional.ofNullable(context.getUsername())
                    .filter(username -> !username.isEmpty())
                    .filter(username -> password.contains(username.toLowerCase()))
                    .map(username -> Result.<Boolean, String>failure("Password cannot contain your username"))
                    .orElse(Result.success(true))
                : emailCheck;
    }
    
    private Result<Boolean, String> validateCommonPasswords(PasswordValidationContext context) {
        return isCommonPassword(context.getPassword())
            ? Result.failure("Password is too common, please choose a stronger password")
            : Result.success(true);
    }

    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "password123", "admin", "qwerty",
        "letmein", "welcome", "monkey", "dragon", "password1",
        "123456789", "football", "iloveyou", "admin123", "welcome123"
    );
    
    private static final List<Pattern> WEAK_PATTERNS = List.of(
        Pattern.compile("^(.)\\1{7,}$"),  // Repeated characters
        Pattern.compile("^12345+"),       // Sequential numbers
        Pattern.compile("^abcde+")        // Sequential letters
    );
    
    /**
     * Check against common weak passwords
     */
    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        
        return COMMON_PASSWORDS.contains(lowerPassword) ||
               WEAK_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(lowerPassword).matches());
    }

    private static final Map<Predicate<String>, Integer> STRENGTH_CRITERIA = Map.of(
        password -> password.length() >= 8, 25,
        password -> password.length() >= 12, 15,
        password -> password.length() >= 16, 10,
        password -> UPPERCASE_PATTERN.matcher(password).find(), 10,
        password -> LOWERCASE_PATTERN.matcher(password).find(), 10,
        password -> DIGIT_PATTERN.matcher(password).find(), 10,
        password -> SPECIAL_CHAR_PATTERN.matcher(password).find(), 15,
        password -> password.chars().distinct().count() >= password.length() * 0.7, 5
    );
    
    /**
     * Generate password strength score (0-100)
     */
    public int calculatePasswordStrength(String password) {
        return Optional.ofNullable(password)
            .filter(p -> !p.isEmpty())
            .map(p -> STRENGTH_CRITERIA.entrySet().stream()
                .filter(entry -> entry.getKey().test(p))
                .mapToInt(Map.Entry::getValue)
                .sum())
            .map(score -> Math.min(100, score))
            .orElse(0);
    }
    
    /**
     * Validate password with context (alternative method signature)
     */
    public Result<Boolean, String> validate(PasswordValidationContext context) {
        return validatePasswordWithContext(context);
    }
}