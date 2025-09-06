package com.trademaster.behavioralai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Behavioral AI Service Response Wrapper
 * 
 * Standard response wrapper for all Behavioral AI service responses.
 * Immutable record following TradeMaster functional programming standards.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BehavioralAIResponse<T>(
    boolean success,
    T data,
    String message,
    String correlationId,
    Instant timestamp,
    ErrorDetails error
) {
    
    public BehavioralAIResponse {
        // Validate required fields
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId cannot be null or blank");
        }
    }
    
    public static <T> BehavioralAIResponse<T> success(T data, String message, String correlationId) {
        return new BehavioralAIResponse<>(
            true,
            data,
            message,
            correlationId,
            Instant.now(),
            null
        );
    }
    
    public static <T> BehavioralAIResponse<T> success(T data, String correlationId) {
        return success(data, "Operation completed successfully", correlationId);
    }
    
    public static <T> BehavioralAIResponse<T> error(String message, String correlationId, ErrorDetails errorDetails) {
        return new BehavioralAIResponse<>(
            false,
            null,
            message,
            correlationId,
            Instant.now(),
            errorDetails
        );
    }
    
    public static <T> BehavioralAIResponse<T> error(String message, String correlationId) {
        return error(message, correlationId, null);
    }
    
    public record ErrorDetails(
        String errorCode,
        String errorType,
        String description,
        String technicalDetails
    ) {
        
        public ErrorDetails {
            // Ensure non-null values
            if (errorCode == null || errorCode.isBlank()) {
                throw new IllegalArgumentException("errorCode cannot be null or blank");
            }
            if (errorType == null || errorType.isBlank()) {
                throw new IllegalArgumentException("errorType cannot be null or blank");
            }
        }
        
        public static ErrorDetails create(String errorCode, String errorType, String description) {
            return new ErrorDetails(errorCode, errorType, description, null);
        }
        
        public static ErrorDetails create(String errorCode, String errorType, String description, String technicalDetails) {
            return new ErrorDetails(errorCode, errorType, description, technicalDetails);
        }
    }
}