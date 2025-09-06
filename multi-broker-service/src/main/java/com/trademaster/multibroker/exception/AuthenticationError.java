package com.trademaster.multibroker.exception;

/**
 * Authentication Error Enumeration
 * 
 * MANDATORY: Functional Error Handling + Immutable Types + Zero Placeholders
 * 
 * Defines specific types of authentication errors that can occur during
 * broker connection and API interaction processes. Used for precise
 * error handling and user feedback.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Functional Error Handling)
 */
public enum AuthenticationError {
    
    INVALID_CREDENTIALS("Invalid credentials", "AUTH_001", ErrorSeverity.HIGH),
    EXPIRED_TOKEN("Token has expired", "AUTH_002", ErrorSeverity.MEDIUM),
    INVALID_TOKEN("Invalid token format", "AUTH_003", ErrorSeverity.HIGH),
    TOKEN_REFRESH_FAILED("Token refresh failed", "AUTH_004", ErrorSeverity.MEDIUM),
    AUTHORIZATION_CODE_INVALID("Invalid authorization code", "AUTH_005", ErrorSeverity.HIGH),
    STATE_PARAMETER_MISMATCH("State parameter mismatch", "AUTH_006", ErrorSeverity.HIGH),
    REDIRECT_URI_MISMATCH("Redirect URI mismatch", "AUTH_007", ErrorSeverity.HIGH),
    INSUFFICIENT_PERMISSIONS("Insufficient permissions", "AUTH_008", ErrorSeverity.MEDIUM),
    ACCOUNT_LOCKED("Account is locked", "AUTH_009", ErrorSeverity.HIGH),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded", "AUTH_010", ErrorSeverity.LOW),
    NETWORK_ERROR("Network communication error", "AUTH_011", ErrorSeverity.LOW),
    BROKER_UNAVAILABLE("Broker service unavailable", "AUTH_012", ErrorSeverity.MEDIUM),
    INVALID_CLIENT("Invalid client configuration", "AUTH_013", ErrorSeverity.HIGH),
    ACCESS_DENIED("Access denied by broker", "AUTH_014", ErrorSeverity.HIGH),
    UNKNOWN_ERROR("Unknown authentication error", "AUTH_999", ErrorSeverity.MEDIUM);
    
    private final String message;
    private final String code;
    private final ErrorSeverity severity;
    
    AuthenticationError(String message, String code, ErrorSeverity severity) {
        this.message = message;
        this.code = code;
        this.severity = severity;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getCode() {
        return code;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    /**
     * Check if error is recoverable (can be retried)
     */
    public boolean isRecoverable() {
        return switch (this) {
            case NETWORK_ERROR, BROKER_UNAVAILABLE, RATE_LIMIT_EXCEEDED -> true;
            case INVALID_CREDENTIALS, INVALID_TOKEN, AUTHORIZATION_CODE_INVALID, 
                 STATE_PARAMETER_MISMATCH, REDIRECT_URI_MISMATCH, ACCOUNT_LOCKED,
                 INSUFFICIENT_PERMISSIONS, INVALID_CLIENT, ACCESS_DENIED -> false;
            case EXPIRED_TOKEN, TOKEN_REFRESH_FAILED -> true; // Can be resolved with token refresh
            default -> false;
        };
    }
    
    /**
     * Get recommended retry delay in seconds
     */
    public long getRetryDelaySeconds() {
        return switch (this) {
            case RATE_LIMIT_EXCEEDED -> 60L;
            case NETWORK_ERROR -> 5L;
            case BROKER_UNAVAILABLE -> 30L;
            case EXPIRED_TOKEN, TOKEN_REFRESH_FAILED -> 1L;
            default -> 0L;
        };
    }
    
    /**
     * Get user-friendly error message
     */
    public String getUserMessage() {
        return switch (this) {
            case INVALID_CREDENTIALS -> "Invalid username or password. Please check your credentials.";
            case EXPIRED_TOKEN -> "Your session has expired. Please log in again.";
            case INVALID_TOKEN -> "Authentication token is invalid. Please log in again.";
            case TOKEN_REFRESH_FAILED -> "Unable to refresh your session. Please log in again.";
            case AUTHORIZATION_CODE_INVALID -> "Authorization failed. Please try connecting again.";
            case STATE_PARAMETER_MISMATCH -> "Security validation failed. Please try again.";
            case REDIRECT_URI_MISMATCH -> "Configuration error. Please contact support.";
            case INSUFFICIENT_PERMISSIONS -> "You don't have permission to access this broker.";
            case ACCOUNT_LOCKED -> "Your broker account is locked. Please contact your broker.";
            case RATE_LIMIT_EXCEEDED -> "Too many requests. Please wait a moment and try again.";
            case NETWORK_ERROR -> "Network connection failed. Please check your internet connection.";
            case BROKER_UNAVAILABLE -> "Broker service is temporarily unavailable. Please try again later.";
            case INVALID_CLIENT -> "Application configuration error. Please contact support.";
            case ACCESS_DENIED -> "Access denied by your broker. Please check your account status.";
            default -> "Authentication failed. Please try again or contact support.";
        };
    }
}