package com.trademaster.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

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
    public void validatePassword(String password, String email) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        // Length validation
        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must not exceed " + MAX_LENGTH + " characters");
        }

        // Complexity validation
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }

        // Check if password contains email
        if (email != null && password.toLowerCase().contains(email.toLowerCase().split("@")[0])) {
            throw new IllegalArgumentException("Password cannot contain parts of your email address");
        }

        // Check for common weak passwords
        if (isCommonPassword(password)) {
            throw new IllegalArgumentException("Password is too common. Please choose a stronger password");
        }

        log.debug("Password validation passed for user email: {}", email);
    }

    /**
     * Check against common weak passwords
     */
    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        
        // Common weak passwords
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "dragon", "password1",
            "123456789", "football", "iloveyou", "admin123", "welcome123"
        };

        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }

        // Check for simple patterns
        return lowerPassword.matches("^(.)\\1{7,}$") || // Repeated characters
               lowerPassword.matches("^12345+") ||      // Sequential numbers
               lowerPassword.matches("^abcde+");        // Sequential letters
    }

    /**
     * Generate password strength score (0-100)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;

        // Character variety scoring
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 15;

        // Uniqueness scoring
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= password.length() * 0.7) score += 5;

        return Math.min(100, score);
    }
}