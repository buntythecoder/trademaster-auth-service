package com.trademaster.auth.validator;

import com.trademaster.auth.dto.RegistrationRequest;
import com.trademaster.auth.pattern.Result;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Functional Authentication Validators
 *
 * Provides validation functions for authentication operations using:
 * - Result types for error handling
 * - Functional composition
 * - No if-else statements
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class AuthenticationValidators {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private AuthenticationValidators() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates registration request using functional composition
     */
    public static RegistrationRequestValidator validateRegistrationRequest() {
        return new RegistrationRequestValidator();
    }

    /**
     * Functional validator for RegistrationRequest
     */
    public static class RegistrationRequestValidator implements Function<RegistrationRequest, Result<RegistrationRequest, String>> {

        @Override
        public Result<RegistrationRequest, String> apply(RegistrationRequest request) {
            return validateEmailFormat(request)
                .flatMap(this::validatePasswordStrength);
        }

        private Result<RegistrationRequest, String> validateEmailFormat(RegistrationRequest request) {
            return Optional.ofNullable(request.getEmail())
                .filter(EMAIL_PATTERN.asPredicate())
                .map(email -> Result.<RegistrationRequest, String>success(request))
                .orElse(Result.<RegistrationRequest, String>failure("Invalid email format"));
        }

        private Result<RegistrationRequest, String> validatePasswordStrength(RegistrationRequest request) {
            return Optional.ofNullable(request.getPassword())
                .filter(password -> password.length() >= MIN_PASSWORD_LENGTH)
                .filter(PASSWORD_PATTERN.asPredicate())
                .map(password -> Result.<RegistrationRequest, String>success(request))
                .orElse(Result.<RegistrationRequest, String>failure(
                    "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
                ));
        }
    }

    /**
     * Validates password strength
     */
    public static Function<String, Result<String, String>> validatePassword() {
        return password -> Optional.ofNullable(password)
            .filter(p -> p.length() >= MIN_PASSWORD_LENGTH)
            .filter(PASSWORD_PATTERN.asPredicate())
            .map(p -> Result.<String, String>success(p))
            .orElse(Result.<String, String>failure("Password does not meet strength requirements"));
    }

    /**
     * Validates email format
     */
    public static Function<String, Result<String, String>> validateEmail() {
        return email -> Optional.ofNullable(email)
            .filter(EMAIL_PATTERN.asPredicate())
            .map(e -> Result.<String, String>success(e))
            .orElse(Result.<String, String>failure("Invalid email format"));
    }

    /**
     * Validates username
     */
    public static Function<String, Result<String, String>> validateUsername() {
        return username -> Optional.ofNullable(username)
            .filter(u -> u.length() >= MIN_USERNAME_LENGTH)
            .filter(u -> u.length() <= MAX_USERNAME_LENGTH)
            .map(u -> Result.<String, String>success(u))
            .orElse(Result.<String, String>failure("Username must be between 3 and 50 characters"));
    }

    /**
     * Functional predicate for email validation
     */
    public static Predicate<String> isValidEmail() {
        return email -> Optional.ofNullable(email)
            .filter(EMAIL_PATTERN.asPredicate())
            .isPresent();
    }

    /**
     * Functional predicate for password validation
     */
    public static Predicate<String> isStrongPassword() {
        return password -> Optional.ofNullable(password)
            .filter(p -> p.length() >= MIN_PASSWORD_LENGTH)
            .filter(PASSWORD_PATTERN.asPredicate())
            .isPresent();
    }
}
