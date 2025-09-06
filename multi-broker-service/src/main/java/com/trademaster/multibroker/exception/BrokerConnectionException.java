package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * Broker Connection Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Comprehensive Context
 * 
 * Base exception for broker connection-related errors. Provides structured
 * error information with broker context, correlation IDs, and recovery
 * guidance for robust error handling and debugging.
 * 
 * Error Categories:
 * - Connection establishment failures
 * - Authentication and authorization errors
 * - Network and timeout issues
 * - API version compatibility problems
 * - Rate limiting and quota exceeded
 * 
 * Recovery Features:
 * - Suggested recovery actions
 * - Retry strategies and backoff guidance
 * - Alternative broker recommendations
 * - User-friendly error messages
 * - Detailed technical information for debugging
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Broker Connection Error Handling)
 */
public class BrokerConnectionException extends RuntimeException {
    
    private final BrokerType brokerType;
    private final String errorCode;
    private final String correlationId;
    private final ErrorSeverity severity;
    private final String userMessage;
    private final String technicalMessage;
    private final String recoveryAction;
    private final boolean isRetryable;
    private final Long retryAfterSeconds;
    
    /**
     * Error Severity Levels
     */
    public enum ErrorSeverity {
        LOW("Low - Service degradation"),
        MEDIUM("Medium - Feature unavailable"),
        HIGH("High - Critical functionality impacted"),
        CRITICAL("Critical - System failure");
        
        private final String description;
        
        ErrorSeverity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Create broker connection exception
     * 
     * @param message exception message
     * @param brokerType affected broker
     * @param errorCode specific error code
     * @param correlationId request correlation ID
     */
    public BrokerConnectionException(String message, 
                                   BrokerType brokerType, 
                                   String errorCode, 
                                   String correlationId) {
        super(message);
        this.brokerType = brokerType;
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.severity = ErrorSeverity.MEDIUM;
        this.userMessage = message;
        this.technicalMessage = message;
        this.recoveryAction = "Retry the operation after a short delay";
        this.isRetryable = true;
        this.retryAfterSeconds = 30L;
    }
    
    /**
     * Create broker connection exception with cause
     * 
     * @param message exception message
     * @param cause underlying cause
     * @param brokerType affected broker
     * @param errorCode specific error code
     * @param correlationId request correlation ID
     */
    public BrokerConnectionException(String message, 
                                   Throwable cause, 
                                   BrokerType brokerType, 
                                   String errorCode, 
                                   String correlationId) {
        super(message, cause);
        this.brokerType = brokerType;
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.severity = determineSeverity(cause);
        this.userMessage = generateUserMessage(message, brokerType);
        this.technicalMessage = message;
        this.recoveryAction = generateRecoveryAction(errorCode, brokerType);
        this.isRetryable = determineRetryability(errorCode, cause);
        this.retryAfterSeconds = calculateRetryDelay(errorCode);
    }
    
    /**
     * Create comprehensive broker connection exception
     * 
     * @param message exception message
     * @param cause underlying cause
     * @param brokerType affected broker
     * @param errorCode specific error code
     * @param correlationId request correlation ID
     * @param severity error severity level
     * @param userMessage user-friendly message
     * @param recoveryAction suggested recovery action
     * @param isRetryable whether operation can be retried
     */
    public BrokerConnectionException(String message,
                                   Throwable cause,
                                   BrokerType brokerType,
                                   String errorCode,
                                   String correlationId,
                                   ErrorSeverity severity,
                                   String userMessage,
                                   String recoveryAction,
                                   boolean isRetryable) {
        super(message, cause);
        this.brokerType = brokerType;
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.severity = severity;
        this.userMessage = userMessage;
        this.technicalMessage = message;
        this.recoveryAction = recoveryAction;
        this.isRetryable = isRetryable;
        this.retryAfterSeconds = calculateRetryDelay(errorCode);
    }
    
    /**
     * Get affected broker type
     * 
     * @return broker type
     */
    public BrokerType getBrokerType() {
        return brokerType;
    }
    
    /**
     * Get specific error code
     * 
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get request correlation ID
     * 
     * @return correlation ID for tracking
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Get error severity level
     * 
     * @return severity level
     */
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Get user-friendly error message
     * 
     * @return message suitable for end users
     */
    public String getUserMessage() {
        return userMessage;
    }
    
    /**
     * Get technical error message
     * 
     * @return detailed technical information
     */
    public String getTechnicalMessage() {
        return technicalMessage;
    }
    
    /**
     * Get suggested recovery action
     * 
     * @return recovery guidance
     */
    public String getRecoveryAction() {
        return recoveryAction;
    }
    
    /**
     * Check if operation can be retried
     * 
     * @return true if retryable
     */
    public boolean isRetryable() {
        return isRetryable;
    }
    
    /**
     * Get recommended retry delay
     * 
     * @return retry delay in seconds
     */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    /**
     * Determine error severity based on cause
     * 
     * @param cause exception cause
     * @return appropriate severity level
     */
    private ErrorSeverity determineSeverity(Throwable cause) {
        if (cause == null) {
            return ErrorSeverity.MEDIUM;
        }
        
        String causeMessage = cause.getMessage();
        if (causeMessage != null) {
            String lowerMessage = causeMessage.toLowerCase();
            
            if (lowerMessage.contains("timeout") || lowerMessage.contains("connect")) {
                return ErrorSeverity.HIGH;
            } else if (lowerMessage.contains("unauthorized") || lowerMessage.contains("forbidden")) {
                return ErrorSeverity.HIGH;
            } else if (lowerMessage.contains("rate limit") || lowerMessage.contains("quota")) {
                return ErrorSeverity.MEDIUM;
            }
        }
        
        return ErrorSeverity.MEDIUM;
    }
    
    /**
     * Generate user-friendly error message
     * 
     * @param technicalMessage technical message
     * @param brokerType affected broker
     * @return user-friendly message
     */
    private String generateUserMessage(String technicalMessage, BrokerType brokerType) {
        String brokerName = brokerType != null ? brokerType.getDisplayName() : "broker";
        
        if (technicalMessage != null) {
            String lowerMessage = technicalMessage.toLowerCase();
            
            if (lowerMessage.contains("timeout")) {
                return String.format("Connection to %s timed out. Please check your internet connection and try again.", brokerName);
            } else if (lowerMessage.contains("unauthorized")) {
                return String.format("Authentication failed with %s. Please re-authorize your account.", brokerName);
            } else if (lowerMessage.contains("rate limit")) {
                return String.format("%s API rate limit exceeded. Please wait a moment and try again.", brokerName);
            } else if (lowerMessage.contains("unavailable")) {
                return String.format("%s services are temporarily unavailable. Please try again later.", brokerName);
            }
        }
        
        return String.format("Unable to connect to %s. Please try again or contact support if the problem persists.", brokerName);
    }
    
    /**
     * Generate recovery action guidance
     * 
     * @param errorCode specific error code
     * @param brokerType affected broker
     * @return recovery action suggestion
     */
    private String generateRecoveryAction(String errorCode, BrokerType brokerType) {
        if (errorCode == null) {
            return "Retry the operation after a short delay";
        }
        
        return switch (errorCode) {
            case "CONNECTION_TIMEOUT" -> "Check your internet connection and retry";
            case "AUTHENTICATION_FAILED" -> "Re-authorize your account with " + 
                (brokerType != null ? brokerType.getDisplayName() : "the broker");
            case "RATE_LIMIT_EXCEEDED" -> "Wait for the rate limit to reset and retry";
            case "SERVICE_UNAVAILABLE" -> "Wait for the service to become available and retry";
            case "INVALID_TOKEN" -> "Refresh your authentication token and retry";
            case "PERMISSION_DENIED" -> "Check your account permissions and retry";
            default -> "Review the error details and retry with corrected parameters";
        };
    }
    
    /**
     * Determine if operation can be retried
     * 
     * @param errorCode specific error code
     * @param cause exception cause
     * @return true if retryable
     */
    private boolean determineRetryability(String errorCode, Throwable cause) {
        if (errorCode != null) {
            return switch (errorCode) {
                case "CONNECTION_TIMEOUT", "SERVICE_UNAVAILABLE", "RATE_LIMIT_EXCEEDED" -> true;
                case "AUTHENTICATION_FAILED", "PERMISSION_DENIED", "INVALID_REQUEST" -> false;
                default -> true;
            };
        }
        
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null) {
                String lowerMessage = causeMessage.toLowerCase();
                return !lowerMessage.contains("unauthorized") && 
                       !lowerMessage.contains("forbidden") &&
                       !lowerMessage.contains("bad request");
            }
        }
        
        return true; // Default to retryable
    }
    
    /**
     * Calculate appropriate retry delay
     * 
     * @param errorCode specific error code
     * @return retry delay in seconds
     */
    private Long calculateRetryDelay(String errorCode) {
        if (errorCode == null) {
            return 30L;
        }
        
        return switch (errorCode) {
            case "RATE_LIMIT_EXCEEDED" -> 60L;
            case "SERVICE_UNAVAILABLE" -> 300L; // 5 minutes
            case "CONNECTION_TIMEOUT" -> 15L;
            default -> 30L;
        };
    }
    
    /**
     * Get comprehensive error information
     * 
     * @return formatted error details
     */
    public String getErrorDetails() {
        StringBuilder details = new StringBuilder();
        details.append("Broker: ").append(brokerType != null ? brokerType.getDisplayName() : "Unknown");
        details.append(", Error Code: ").append(errorCode != null ? errorCode : "Unknown");
        details.append(", Severity: ").append(severity.getDescription());
        details.append(", Retryable: ").append(isRetryable);
        
        if (correlationId != null) {
            details.append(", Correlation ID: ").append(correlationId);
        }
        
        return details.toString();
    }
    
    /**
     * Generate unique correlation ID for error tracking
     * 
     * @return unique correlation ID
     */
    protected static String generateCorrelationId() {
        return "TM-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int) (Math.random() * 65536));
    }
}