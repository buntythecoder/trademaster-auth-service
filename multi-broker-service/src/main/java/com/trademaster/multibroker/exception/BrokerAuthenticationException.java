package com.trademaster.multibroker.exception;

import com.trademaster.multibroker.entity.BrokerType;

/**
 * Broker Authentication Exception
 * 
 * MANDATORY: Functional Error Handling + Zero Placeholders + Security Compliance
 * 
 * Specialized exception for broker authentication and authorization failures.
 * Provides detailed authentication error context while maintaining security
 * best practices and preventing information disclosure.
 * 
 * Authentication Error Types:
 * - OAuth token exchange failures
 * - Token expiration and refresh errors
 * - Invalid credentials or API keys
 * - Scope and permission violations
 * - Two-factor authentication requirements
 * 
 * Security Features:
 * - Safe error messaging (no credential exposure)
 * - Audit trail correlation
 * - Rate limiting awareness
 * - Account lockout detection
 * - Compliance with security standards
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Authentication Error Handling)
 */
public class BrokerAuthenticationException extends BrokerConnectionException {
    
    private final AuthenticationError authError;
    private final String tokenHint;
    private final Long tokenExpiresAt;
    private final boolean requiresReauthorization;
    private final boolean requires2FA;
    private final String authUrl;
    
    /**
     * Authentication Error Types
     */
    public enum AuthenticationError {
        INVALID_CREDENTIALS("Invalid username or password"),
        TOKEN_EXPIRED("Access token has expired"),
        TOKEN_REVOKED("Access token has been revoked"),
        REFRESH_TOKEN_INVALID("Refresh token is invalid or expired"),
        INSUFFICIENT_SCOPE("Insufficient permissions for this operation"),
        ACCOUNT_LOCKED("Account is temporarily locked"),
        REQUIRES_2FA("Two-factor authentication required"),
        API_KEY_INVALID("API key is invalid or expired"),
        RATE_LIMITED("Authentication rate limit exceeded"),
        BROKER_MAINTENANCE("Broker authentication service under maintenance");
        
        private final String description;
        
        AuthenticationError(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Create authentication exception
     * 
     * @param authError authentication error type
     * @param brokerType affected broker
     * @param correlationId request correlation ID
     */
    public BrokerAuthenticationException(AuthenticationError authError,
                                       BrokerType brokerType,
                                       String correlationId) {
        super(
            authError.getDescription(),
            null,
            brokerType,
            authError.name(),
            correlationId,
            ErrorSeverity.HIGH,
            generateUserMessage(authError, brokerType),
            generateRecoveryAction(authError, brokerType),
            determineRetryability(authError)
        );
        
        this.authError = authError;
        this.tokenHint = null;
        this.tokenExpiresAt = null;
        this.requiresReauthorization = determineReauthorizationRequired(authError);
        this.requires2FA = authError == AuthenticationError.REQUIRES_2FA;
        this.authUrl = generateAuthUrl(brokerType);
    }
    
    /**
     * Create authentication exception with token information
     * 
     * @param authError authentication error type
     * @param brokerType affected broker
     * @param correlationId request correlation ID
     * @param tokenHint partial token for identification (last 4 chars)
     * @param tokenExpiresAt token expiration timestamp
     */
    public BrokerAuthenticationException(AuthenticationError authError,
                                       BrokerType brokerType,
                                       String correlationId,
                                       String tokenHint,
                                       Long tokenExpiresAt) {
        super(
            authError.getDescription(),
            null,
            brokerType,
            authError.name(),
            correlationId,
            ErrorSeverity.HIGH,
            generateUserMessage(authError, brokerType),
            generateRecoveryAction(authError, brokerType),
            determineRetryability(authError)
        );
        
        this.authError = authError;
        this.tokenHint = tokenHint;
        this.tokenExpiresAt = tokenExpiresAt;
        this.requiresReauthorization = determineReauthorizationRequired(authError);
        this.requires2FA = authError == AuthenticationError.REQUIRES_2FA;
        this.authUrl = generateAuthUrl(brokerType);
    }
    
    /**
     * Create authentication exception with cause
     * 
     * @param authError authentication error type
     * @param cause underlying cause
     * @param brokerType affected broker
     * @param correlationId request correlation ID
     */
    public BrokerAuthenticationException(AuthenticationError authError,
                                       Throwable cause,
                                       BrokerType brokerType,
                                       String correlationId) {
        super(
            authError.getDescription(),
            cause,
            brokerType,
            authError.name(),
            correlationId,
            ErrorSeverity.HIGH,
            generateUserMessage(authError, brokerType),
            generateRecoveryAction(authError, brokerType),
            determineRetryability(authError)
        );
        
        this.authError = authError;
        this.tokenHint = null;
        this.tokenExpiresAt = null;
        this.requiresReauthorization = determineReauthorizationRequired(authError);
        this.requires2FA = authError == AuthenticationError.REQUIRES_2FA;
        this.authUrl = generateAuthUrl(brokerType);
    }
    
    /**
     * Get authentication error type
     * 
     * @return authentication error
     */
    public AuthenticationError getAuthError() {
        return authError;
    }
    
    /**
     * Get token hint for identification (safe partial token)
     * 
     * @return token hint (last 4 characters)
     */
    public String getTokenHint() {
        return tokenHint;
    }
    
    /**
     * Get token expiration timestamp
     * 
     * @return expiration timestamp in milliseconds
     */
    public Long getTokenExpiresAt() {
        return tokenExpiresAt;
    }
    
    /**
     * Check if reauthorization is required
     * 
     * @return true if user must reauthorize
     */
    public boolean requiresReauthorization() {
        return requiresReauthorization;
    }
    
    /**
     * Check if two-factor authentication is required
     * 
     * @return true if 2FA is needed
     */
    public boolean requires2FA() {
        return requires2FA;
    }
    
    /**
     * Get authorization URL for reauthorization
     * 
     * @return broker authorization URL
     */
    public String getAuthUrl() {
        return authUrl;
    }
    
    /**
     * Check if token has expired
     * 
     * @return true if token is expired
     */
    public boolean isTokenExpired() {
        return authError == AuthenticationError.TOKEN_EXPIRED ||
               (tokenExpiresAt != null && tokenExpiresAt < System.currentTimeMillis());
    }
    
    /**
     * Check if error is recoverable without user intervention
     * 
     * @return true if can be automatically recovered
     */
    public boolean isAutoRecoverable() {
        return authError == AuthenticationError.TOKEN_EXPIRED ||
               authError == AuthenticationError.RATE_LIMITED ||
               authError == AuthenticationError.BROKER_MAINTENANCE;
    }
    
    /**
     * Generate user-friendly authentication error message
     * 
     * @param authError authentication error type
     * @param brokerType affected broker
     * @return user-friendly message
     */
    private static String generateUserMessage(AuthenticationError authError, BrokerType brokerType) {
        String brokerName = brokerType != null ? brokerType.getDisplayName() : "broker";
        
        return switch (authError) {
            case INVALID_CREDENTIALS -> 
                String.format("Invalid credentials for %s. Please check your login details.", brokerName);
            case TOKEN_EXPIRED -> 
                String.format("Your %s session has expired. Please log in again.", brokerName);
            case TOKEN_REVOKED -> 
                String.format("Your %s access has been revoked. Please reauthorize your account.", brokerName);
            case REFRESH_TOKEN_INVALID -> 
                String.format("Unable to refresh your %s session. Please log in again.", brokerName);
            case INSUFFICIENT_SCOPE -> 
                String.format("Insufficient permissions for this %s operation. Please contact support.", brokerName);
            case ACCOUNT_LOCKED -> 
                String.format("Your %s account is temporarily locked. Please try again later.", brokerName);
            case REQUIRES_2FA -> 
                String.format("Two-factor authentication is required for %s. Please complete 2FA setup.", brokerName);
            case API_KEY_INVALID -> 
                String.format("Your %s API credentials are invalid. Please update your API settings.", brokerName);
            case RATE_LIMITED -> 
                String.format("Too many authentication attempts with %s. Please wait and try again.", brokerName);
            case BROKER_MAINTENANCE -> 
                String.format("%s authentication service is under maintenance. Please try again later.", brokerName);
        };
    }
    
    /**
     * Generate recovery action for authentication errors
     * 
     * @param authError authentication error type
     * @param brokerType affected broker
     * @return recovery action guidance
     */
    private static String generateRecoveryAction(AuthenticationError authError, BrokerType brokerType) {
        return switch (authError) {
            case INVALID_CREDENTIALS -> 
                "Verify your login credentials and try again";
            case TOKEN_EXPIRED, TOKEN_REVOKED -> 
                "Re-authorize your account to obtain new access tokens";
            case REFRESH_TOKEN_INVALID -> 
                "Complete the full authorization flow again";
            case INSUFFICIENT_SCOPE -> 
                "Contact support to request additional permissions";
            case ACCOUNT_LOCKED -> 
                "Wait for the account lockout period to expire, then try again";
            case REQUIRES_2FA -> 
                "Enable two-factor authentication in your broker account settings";
            case API_KEY_INVALID -> 
                "Generate new API credentials from your broker's developer portal";
            case RATE_LIMITED -> 
                "Wait for the rate limit to reset (typically 15-60 minutes)";
            case BROKER_MAINTENANCE -> 
                "Monitor the broker's status page and retry when maintenance is complete";
        };
    }
    
    /**
     * Determine if authentication error is retryable
     * 
     * @param authError authentication error type
     * @return true if retryable
     */
    private static boolean determineRetryability(AuthenticationError authError) {
        return switch (authError) {
            case RATE_LIMITED, BROKER_MAINTENANCE -> true;
            case TOKEN_EXPIRED -> true; // Can retry after refresh
            case INVALID_CREDENTIALS, TOKEN_REVOKED, REFRESH_TOKEN_INVALID, 
                 INSUFFICIENT_SCOPE, ACCOUNT_LOCKED, REQUIRES_2FA, API_KEY_INVALID -> false;
        };
    }
    
    /**
     * Determine if reauthorization is required
     * 
     * @param authError authentication error type
     * @return true if reauthorization needed
     */
    private static boolean determineReauthorizationRequired(AuthenticationError authError) {
        return switch (authError) {
            case TOKEN_EXPIRED, TOKEN_REVOKED, REFRESH_TOKEN_INVALID, 
                 INSUFFICIENT_SCOPE, REQUIRES_2FA, API_KEY_INVALID -> true;
            case INVALID_CREDENTIALS, ACCOUNT_LOCKED, RATE_LIMITED, BROKER_MAINTENANCE -> false;
        };
    }
    
    /**
     * Generate authorization URL for broker
     * 
     * @param brokerType broker type
     * @return authorization URL
     */
    private static String generateAuthUrl(BrokerType brokerType) {
        if (brokerType == null) {
            return null;
        }
        
        return switch (brokerType) {
            case ZERODHA -> "https://kite.trade/connect/login";
            case UPSTOX -> "https://api.upstox.com/v2/login/authorization/dialog";
            case ANGEL_ONE -> "https://smartapi.angelbroking.com/publisher-login";
            case ICICI_DIRECT -> "https://api.icicidirect.com/oauth/authorize";
            case FYERS -> "https://api.fyers.in/api/v2/generate-authcode";
            case IIFL -> "https://ttblaze.iifl.com/apimarketdata/auth/login";
        };
    }
    
    /**
     * Get authentication context information
     * 
     * @return formatted authentication context
     */
    public String getAuthenticationContext() {
        StringBuilder context = new StringBuilder();
        context.append("Authentication Error: ").append(authError.getDescription());
        
        if (tokenHint != null) {
            context.append(", Token: ...").append(tokenHint);
        }
        
        if (tokenExpiresAt != null) {
            context.append(", Expires: ").append(new java.util.Date(tokenExpiresAt));
        }
        
        context.append(", Reauth Required: ").append(requiresReauthorization);
        context.append(", 2FA Required: ").append(requires2FA);
        
        return context.toString();
    }
}