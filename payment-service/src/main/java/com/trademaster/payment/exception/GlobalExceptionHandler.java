package com.trademaster.payment.exception;

import com.trademaster.payment.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global Exception Handler
 * 
 * Centralized exception handling for payment service with proper error responses
 * and security considerations for financial data.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "An error occurred while processing your request";
    private static final String VALIDATION_ERROR_MESSAGE = "Validation failed";
    
    /**
     * Handle payment-specific exceptions
     */
    @ExceptionHandler(PaymentServiceException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServiceException(
            PaymentServiceException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Payment service error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Payment Error")
                .message(sanitizeErrorMessage(ex.getMessage()))
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle payment not found exceptions
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(
            PaymentNotFoundException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Payment not found [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Payment Not Found")
                .message("The requested payment transaction was not found")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handle payment method not found exceptions
     */
    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentMethodNotFoundException(
            PaymentMethodNotFoundException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Payment method not found [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Payment Method Not Found")
                .message("The requested payment method was not found")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handle refund exceptions
     */
    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ErrorResponse> handleRefundException(
            RefundException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Refund error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Refund Error")
                .message(sanitizeErrorMessage(ex.getMessage()))
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle subscription exceptions
     */
    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionException(
            SubscriptionException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Subscription error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Subscription Error")
                .message(sanitizeErrorMessage(ex.getMessage()))
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle gateway integration exceptions
     */
    @ExceptionHandler(GatewayIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleGatewayIntegrationException(
            GatewayIntegrationException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Gateway integration error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        // Don't expose internal gateway errors to clients
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Payment Service Temporarily Unavailable")
                .message("Payment processing is temporarily unavailable. Please try again later.")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Validation error [{}]: {}", correlationId, ex.getMessage());
        
        List<String> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(VALIDATION_ERROR_MESSAGE)
                .path(getPath(request))
                .correlationId(correlationId)
                .details(validationErrors)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Constraint violation [{}]: {}", correlationId, ex.getMessage());
        
        List<String> violations = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(VALIDATION_ERROR_MESSAGE)
                .path(getPath(request))
                .correlationId(correlationId)
                .details(violations)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Missing parameter [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Type mismatch [{}]: {}", correlationId, ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Parameter")
                .message(message)
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle message not readable exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Message not readable [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Request")
                .message("Request body is not readable or contains invalid data")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handle data integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Data integrity violation [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Conflict")
                .message("The operation conflicts with existing data")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Access denied [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    /**
     * Handle rate limiting exceptions
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.warn("Rate limit exceeded [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Rate Limit Exceeded")
                .message("Too many requests. Please try again later.")
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(GENERIC_ERROR_MESSAGE)
                .path(getPath(request))
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return GENERIC_ERROR_MESSAGE;
        }
        
        // Remove sensitive information from error messages
        String sanitized = message
                .replaceAll("\\b\\d{16}\\b", "****-****-****-****") // Credit card numbers
                .replaceAll("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b", "****-****-****-****")
                .replaceAll("(?i)cvv\\s*:?\\s*\\d{3,4}", "cvv: ***") // CVV numbers
                .replaceAll("(?i)pin\\s*:?\\s*\\d{4,6}", "pin: ****") // PIN numbers
                .replaceAll("(?i)password\\s*:?\\s*\\S+", "password: ****") // Passwords
                .replaceAll("(?i)secret\\s*:?\\s*\\S+", "secret: ****") // Secrets
                .replaceAll("(?i)token\\s*:?\\s*[a-zA-Z0-9_-]{20,}", "token: ****"); // Long tokens
        
        // Limit message length to prevent information leakage
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 197) + "...";
        }
        
        return sanitized;
    }
}