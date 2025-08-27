package com.trademaster.brokerauth.exception;

import com.trademaster.brokerauth.config.CorrelationConfig;
import com.trademaster.brokerauth.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * 
 * Handles all exceptions across the broker authentication service with proper HTTP status codes
 * and structured error responses with correlation tracking.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final StructuredLoggingService loggingService;

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Authentication failed",
            "AUTHENTICATION_FAILED",
            request,
            null
        );

        log.warn("Authentication failed: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Forbidden",
            "Access denied - insufficient privileges",
            "ACCESS_DENIED",
            request,
            null
        );

        log.warn("Access denied: {} (correlationId: {}, userId: {})", 
                ex.getMessage(), 
                CorrelationConfig.CorrelationContext.getCorrelationId(),
                CorrelationConfig.CorrelationContext.getUserId());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle broker authentication specific exceptions
     */
    @ExceptionHandler(BrokerAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleBrokerAuthentication(
            BrokerAuthenticationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            ex.getHttpStatus(),
            ex.getHttpStatus().getReasonPhrase(),
            ex.getMessage(),
            ex.getErrorCode(),
            request,
            ex.getAdditionalData()
        );

        // Log broker auth errors
        loggingService.logError(
            "broker_auth_exception",
            ex.getMessage(),
            ex.getErrorCode(),
            ex,
            Map.of(
                "brokerType", ex.getBrokerType() != null ? ex.getBrokerType().toString() : "unknown",
                "userId", ex.getUserId() != null ? ex.getUserId().toString() : "unknown",
                "correlationId", CorrelationConfig.CorrelationContext.getCorrelationId()
            )
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * Handle session management exceptions
     */
    @ExceptionHandler(SessionManagementException.class)
    public ResponseEntity<ErrorResponse> handleSessionManagement(
            SessionManagementException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Session Error",
            ex.getMessage(),
            "SESSION_ERROR",
            request,
            Map.of("sessionId", ex.getSessionId() != null ? ex.getSessionId() : "unknown")
        );

        log.warn("Session management error: {} (correlationId: {}, sessionId: {})", 
                ex.getMessage(), 
                CorrelationConfig.CorrelationContext.getCorrelationId(),
                ex.getSessionId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle rate limit exceeded exceptions
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            ex.getMessage(),
            "RATE_LIMIT_EXCEEDED",
            request,
            Map.of(
                "retryAfterSeconds", ex.getRetryAfterSeconds(),
                "brokerType", ex.getBrokerType() != null ? ex.getBrokerType().toString() : "unknown"
            )
        );

        // Log rate limit violations as security incidents
        loggingService.logSecurityIncident(
            "rate_limit_exceeded",
            "medium",
            ex.getUserId() != null ? ex.getUserId().toString() : null,
            null,
            null,
            Map.of(
                "brokerType", ex.getBrokerType() != null ? ex.getBrokerType().toString() : "unknown",
                "retryAfterSeconds", ex.getRetryAfterSeconds()
            )
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handle credential management exceptions
     */
    @ExceptionHandler(CredentialManagementException.class)
    public ResponseEntity<ErrorResponse> handleCredentialManagement(
            CredentialManagementException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Credential Error",
            ex.getMessage(),
            "CREDENTIAL_ERROR",
            request,
            null
        );

        log.warn("Credential management error: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle WebClient response exceptions (broker API errors)
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientResponse(
            WebClientResponseException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_GATEWAY,
            "Broker API Error",
            "Broker service temporarily unavailable",
            "BROKER_API_ERROR",
            request,
            Map.of(
                "brokerResponseStatus", ex.getStatusCode().value(),
                "brokerErrorBody", ex.getResponseBodyAsString()
            )
        );

        log.error("Broker API error: {} {} (correlationId: {})", 
                ex.getStatusCode(), ex.getResponseBodyAsString(),
                CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handle method argument validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));

        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            "Invalid request data",
            "VALIDATION_ERROR",
            request,
            Map.of("validationErrors", fieldErrors)
        );
        
        errorResponse.setFieldErrors(fieldErrors);

        log.warn("Method argument validation error: {} (correlationId: {})", 
                fieldErrors, CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            request,
            null
        );

        log.warn("Illegal argument: {} (correlationId: {})", 
                ex.getMessage(), CorrelationConfig.CorrelationContext.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle completion exceptions (from CompletableFuture)
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponse> handleCompletionException(
            CompletionException ex, WebRequest request) {
        
        // Unwrap the cause and handle it appropriately
        Throwable cause = ex.getCause();
        if (cause instanceof BrokerAuthenticationException) {
            return handleBrokerAuthentication((BrokerAuthenticationException) cause, request);
        } else if (cause instanceof RateLimitExceededException) {
            return handleRateLimitExceeded((RateLimitExceededException) cause, request);
        } else if (cause instanceof WebClientResponseException) {
            return handleWebClientResponse((WebClientResponseException) cause, request);
        }
        
        // Generic completion exception handling
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An async operation failed",
            "ASYNC_OPERATION_FAILED",
            request,
            Map.of("causedBy", cause != null ? cause.getClass().getSimpleName() : "unknown")
        );

        log.error("Completion exception (correlationId: {}): ", 
                CorrelationConfig.CorrelationContext.getCorrelationId(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            request,
            Map.of("exceptionType", ex.getClass().getSimpleName())
        );

        // Log error with full context
        loggingService.logError(
            "runtime_exception",
            ex.getMessage(),
            "RUNTIME_EXCEPTION",
            ex,
            Map.of(
                "path", extractPath(request),
                "correlationId", CorrelationConfig.CorrelationContext.getCorrelationId(),
                "requestId", CorrelationConfig.CorrelationContext.getRequestId()
            )
        );
        
        log.error("Unexpected runtime exception (correlationId: {}): ", 
                CorrelationConfig.CorrelationContext.getCorrelationId(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
            "INTERNAL_SERVER_ERROR",
            request,
            Map.of("exceptionType", ex.getClass().getSimpleName())
        );

        // Log error with full context
        loggingService.logError(
            "generic_exception",
            ex.getMessage(),
            "GENERIC_EXCEPTION",
            ex,
            Map.of(
                "path", extractPath(request),
                "correlationId", CorrelationConfig.CorrelationContext.getCorrelationId(),
                "requestId", CorrelationConfig.CorrelationContext.getRequestId()
            )
        );
        
        log.error("Unexpected exception (correlationId: {}): ", 
                CorrelationConfig.CorrelationContext.getCorrelationId(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Create standardized error response with correlation tracking
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String error, String message, 
                                            String errorCode, WebRequest request, Map<String, Object> additionalDetails) {
        
        String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
        String requestId = CorrelationConfig.CorrelationContext.getRequestId();
        String userId = CorrelationConfig.CorrelationContext.getUserId();
        
        Map<String, Object> details = new HashMap<>();
        details.put("correlationId", correlationId);
        details.put("requestId", requestId);
        details.put("timestamp", Instant.now().toString());
        
        if (userId != null) {
            details.put("userId", userId);
        }
        
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }
        
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .errorCode(errorCode)
                .path(extractPath(request))
                .details(details)
                .build();
    }

    /**
     * Extract clean path from web request
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}