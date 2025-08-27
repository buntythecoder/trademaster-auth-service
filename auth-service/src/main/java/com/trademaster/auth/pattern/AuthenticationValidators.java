package com.trademaster.auth.pattern;

import com.trademaster.auth.dto.AuthenticationRequest;
import com.trademaster.auth.dto.RegistrationRequest;

/**
 * Functional Authentication Validators
 * 
 * Replaces if-else validation logic with composable functional validators
 * following TradeMaster Advanced Design Patterns standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class AuthenticationValidators {

    private AuthenticationValidators() {}
    
    // Registration validation chains
    public static final ValidationChain<RegistrationRequest> validateRegistrationRequest = 
        ValidationChain.<RegistrationRequest>notNull("Registration request cannot be null")
            .andThen(validateEmail())
            .andThen(validatePassword())
            .andThen(validateFirstName())
            .andThen(validateLastName());
    
    // Authentication validation chains  
    public static final ValidationChain<AuthenticationRequest> validateAuthenticationRequest =
        ValidationChain.<AuthenticationRequest>notNull("Authentication request cannot be null")
            .andThen(validateLoginEmail())
            .andThen(validateLoginPassword());
    
    // Individual field validators
    private static ValidationChain<RegistrationRequest> validateEmail() {
        return ValidationChain.of(
            req -> req.getEmail() != null && 
                   !req.getEmail().trim().isEmpty() &&
                   req.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") &&
                   req.getEmail().length() <= 255,
            "Email must be valid and not exceed 255 characters"
        );
    }
    
    private static ValidationChain<RegistrationRequest> validatePassword() {
        return ValidationChain.of(
            req -> req.getPassword() != null && 
                   req.getPassword().length() >= 8 &&
                   req.getPassword().length() <= 128 &&
                   req.getPassword().matches(".*[A-Z].*") &&
                   req.getPassword().matches(".*[a-z].*") &&
                   req.getPassword().matches(".*[0-9].*") &&
                   req.getPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"),
            "Password must be 8-128 characters with uppercase, lowercase, number and special character"
        );
    }
    
    private static ValidationChain<RegistrationRequest> validateFirstName() {
        return ValidationChain.of(
            req -> req.getFirstName() != null && 
                   !req.getFirstName().trim().isEmpty() &&
                   req.getFirstName().length() <= 50 &&
                   req.getFirstName().matches("^[A-Za-z\\s'-]+$"),
            "First name must be 1-50 characters containing only letters, spaces, apostrophes and hyphens"
        );
    }
    
    private static ValidationChain<RegistrationRequest> validateLastName() {
        return ValidationChain.of(
            req -> req.getLastName() != null && 
                   !req.getLastName().trim().isEmpty() &&
                   req.getLastName().length() <= 50 &&
                   req.getLastName().matches("^[A-Za-z\\s'-]+$"),
            "Last name must be 1-50 characters containing only letters, spaces, apostrophes and hyphens"
        );
    }
    
    private static ValidationChain<AuthenticationRequest> validateLoginEmail() {
        return ValidationChain.of(
            req -> req.getEmail() != null && 
                   !req.getEmail().trim().isEmpty() &&
                   req.getEmail().length() <= 255,
            "Email cannot be blank and must not exceed 255 characters"
        );
    }
    
    private static ValidationChain<AuthenticationRequest> validateLoginPassword() {
        return ValidationChain.of(
            req -> req.getPassword() != null && 
                   !req.getPassword().trim().isEmpty(),
            "Password cannot be blank"
        );
    }
    
    // String validators
    public static final ValidationChain<String> validateToken = 
        ValidationChain.<String>notNull("Token cannot be null")
            .andThen(ValidationChain.notBlank("Token cannot be blank"))
            .andThen(ValidationChain.minLength(10, "Token must be at least 10 characters"));
            
    public static final ValidationChain<String> validateDeviceFingerprint =
        ValidationChain.<String>notNull("Device fingerprint cannot be null")
            .andThen(ValidationChain.notBlank("Device fingerprint cannot be blank"))
            .andThen(ValidationChain.minLength(5, "Device fingerprint must be at least 5 characters"));
}