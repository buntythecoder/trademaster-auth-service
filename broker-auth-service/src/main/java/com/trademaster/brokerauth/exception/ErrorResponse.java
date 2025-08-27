package com.trademaster.brokerauth.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized Error Response
 * 
 * Consistent error response structure for all API endpoints.
 * Includes correlation tracking and detailed error information.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    private int status;
    
    private String error;
    
    private String message;
    
    private String errorCode;
    
    private String path;
    
    private Map<String, Object> details;
    
    private Map<String, List<String>> fieldErrors;
    
    /**
     * Create a simple error response
     */
    public static ErrorResponse of(int status, String error, String message, String errorCode, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .build();
    }
    
    /**
     * Create an error response with details
     */
    public static ErrorResponse of(int status, String error, String message, String errorCode, 
                                 String path, Map<String, Object> details) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .details(details)
                .build();
    }
    
    /**
     * Create a validation error response
     */
    public static ErrorResponse validationError(String path, Map<String, List<String>> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Validation Error")
                .message("Request validation failed")
                .errorCode("VALIDATION_ERROR")
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
    
    /**
     * Create an internal server error response
     */
    public static ErrorResponse internalError(String path, String correlationId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(500)
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .errorCode("INTERNAL_SERVER_ERROR")
                .path(path)
                .details(Map.of("correlationId", correlationId))
                .build();
    }
    
    /**
     * Create an unauthorized error response
     */
    public static ErrorResponse unauthorized(String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("Unauthorized")
                .message("Authentication required")
                .errorCode("AUTHENTICATION_REQUIRED")
                .path(path)
                .build();
    }
    
    /**
     * Create a forbidden error response
     */
    public static ErrorResponse forbidden(String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(403)
                .error("Forbidden")
                .message("Access denied")
                .errorCode("ACCESS_DENIED")
                .path(path)
                .build();
    }
    
    /**
     * Create a not found error response
     */
    public static ErrorResponse notFound(String path, String resource) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(404)
                .error("Not Found")
                .message(resource + " not found")
                .errorCode("RESOURCE_NOT_FOUND")
                .path(path)
                .details(Map.of("resource", resource))
                .build();
    }
    
    /**
     * Create a rate limit exceeded error response
     */
    public static ErrorResponse rateLimitExceeded(String path, long retryAfterSeconds) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(429)
                .error("Too Many Requests")
                .message("Rate limit exceeded")
                .errorCode("RATE_LIMIT_EXCEEDED")
                .path(path)
                .details(Map.of("retryAfterSeconds", retryAfterSeconds))
                .build();
    }
    
    /**
     * Create a service unavailable error response
     */
    public static ErrorResponse serviceUnavailable(String path, String service) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(503)
                .error("Service Unavailable")
                .message(service + " is temporarily unavailable")
                .errorCode("SERVICE_UNAVAILABLE")
                .path(path)
                .details(Map.of("service", service))
                .build();
    }
}