package com.trademaster.marketdata.dto;

/**
 * Validation Result - Railway Oriented Programming
 * 
 * Sealed interface for validation results following functional programming patterns.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface ValidationResult permits Valid, Invalid {
    
    default boolean isValid() {
        return this instanceof Valid;
    }
    
    default boolean isInvalid() {
        return this instanceof Invalid;
    }
    
    default String getErrorMessage() {
        return switch (this) {
            case Valid v -> "";
            case Invalid i -> i.message();
        };
    }
}

/**
 * Successful validation result
 */
record Valid() implements ValidationResult {}

/**
 * Failed validation result with error message
 */
record Invalid(String message) implements ValidationResult {
    public Invalid {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
    }
}