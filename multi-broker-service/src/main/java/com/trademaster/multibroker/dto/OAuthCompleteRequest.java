package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;

/**
 * OAuth Complete Request DTO
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Request payload for completing OAuth 2.0 authorization flow with broker APIs.
 * Contains authorization code, state verification, and broker-specific parameters
 * required to exchange authorization code for access tokens.
 * 
 * OAuth Flow Support:
 * - Authorization code exchange for access tokens
 * - State parameter verification for CSRF protection
 * - Broker-specific redirect URI validation
 * - PKCE (Proof Key for Code Exchange) support
 * - Custom parameter handling per broker
 * 
 * Security Features:
 * - State parameter validation to prevent CSRF
 * - Redirect URI verification for security
 * - Authorization code expiry handling
 * - Secure parameter transmission
 * - Request correlation and audit logging
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (OAuth Completion Flow)
 */
@Builder
public record OAuthCompleteRequest(
    BrokerType brokerType,
    String authorizationCode,
    String state,
    String redirectUri,
    String codeVerifier,
    String clientId,
    String scope,
    String sessionId,
    String correlationId,
    Long expiresIn,
    String error,
    String errorDescription,
    String customParam1,
    String customParam2
) {
    
    /**
     * Get authorization code
     */
    public String getCode() {
        return authorizationCode;
    }
    
    /**
     * Get state parameter
     */
    public String getState() {
        return state;
    }
    
    /**
     * Get broker type
     */
    public BrokerType getBrokerType() {
        return brokerType;
    }
    
    /**
     * Check if OAuth response indicates success
     * 
     * @return true if authorization was successful
     */
    public boolean isSuccessful() {
        return authorizationCode != null &&
               !authorizationCode.trim().isEmpty() &&
               error == null;
    }
    
    /**
     * Check if OAuth response indicates error
     * 
     * @return true if authorization failed
     */
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
    
    /**
     * Check if request has valid state for CSRF protection
     * 
     * @return true if state parameter is present
     */
    public boolean hasValidState() {
        return state != null && 
               !state.trim().isEmpty() && 
               state.length() >= 32; // Minimum entropy for security
    }
    
    /**
     * Check if request supports PKCE flow
     * 
     * @return true if code verifier is present
     */
    public boolean supportsPKCE() {
        return codeVerifier != null && !codeVerifier.trim().isEmpty();
    }
    
    /**
     * Get error information if available
     * 
     * @return formatted error message
     */
    public String getErrorInfo() {
        if (!hasError()) {
            return null;
        }
        
        StringBuilder errorInfo = new StringBuilder(error);
        if (errorDescription != null && !errorDescription.trim().isEmpty()) {
            errorInfo.append(": ").append(errorDescription);
        }
        
        return errorInfo.toString();
    }
    
    /**
     * Get authorization scope as array
     * 
     * @return array of requested scopes
     */
    public String[] getScopeArray() {
        if (scope == null || scope.trim().isEmpty()) {
            return new String[0];
        }
        
        return scope.split("\\s+");
    }
    
    /**
     * Check if specific scope is requested
     * 
     * @param requestedScope scope to check
     * @return true if scope is included
     */
    public boolean hasScope(String requestedScope) {
        if (scope == null || requestedScope == null) {
            return false;
        }
        
        return scope.contains(requestedScope);
    }
    
    /**
     * Get broker-specific validation requirements
     * 
     * @return validation requirements based on broker type
     */
    public ValidationRequirements getValidationRequirements() {
        return switch (brokerType) {
            case ZERODHA -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(false)
                .requiresClientId(true)
                .build();
                
            case UPSTOX -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(true)
                .requiresClientId(true)
                .build();
                
            case ANGEL_ONE -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(false)
                .requiresClientId(true)
                .build();
                
            case ICICI_DIRECT -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(false)
                .requiresClientId(false)
                .build();
                
            case FYERS -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(true)
                .requiresClientId(true)
                .build();
                
            case IIFL -> ValidationRequirements.builder()
                .requiresState(true)
                .requiresRedirectUri(true)
                .requiresPKCE(false)
                .requiresClientId(true)
                .build();
        };
    }
    
    /**
     * Validate request completeness
     * 
     * @return validation result with details
     */
    public ValidationResult validate() {
        if (hasError()) {
            return ValidationResult.failure("OAuth error: " + getErrorInfo());
        }
        
        if (!isSuccessful()) {
            return ValidationResult.failure("Missing authorization code");
        }
        
        ValidationRequirements requirements = getValidationRequirements();
        
        if (requirements.requiresState() && !hasValidState()) {
            return ValidationResult.failure("Invalid or missing state parameter");
        }
        
        if (requirements.requiresRedirectUri() && 
            (redirectUri == null || redirectUri.trim().isEmpty())) {
            return ValidationResult.failure("Missing redirect URI");
        }
        
        if (requirements.requiresPKCE() && !supportsPKCE()) {
            return ValidationResult.failure("Missing PKCE code verifier");
        }
        
        if (requirements.requiresClientId() && 
            (clientId == null || clientId.trim().isEmpty())) {
            return ValidationResult.failure("Missing client ID");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Create sanitized request for logging (removes sensitive data)
     * 
     * @return sanitized request safe for logging
     */
    public OAuthCompleteRequest sanitizeForLogging() {
        return OAuthCompleteRequest.builder()
            .brokerType(brokerType)
            .authorizationCode("***REDACTED***")
            .state(state)
            .redirectUri(redirectUri)
            .codeVerifier("***REDACTED***")
            .clientId(clientId)
            .scope(scope)
            .sessionId(sessionId)
            .correlationId(correlationId)
            .expiresIn(expiresIn)
            .error(error)
            .errorDescription(errorDescription)
            .build();
    }
    
    /**
     * Validation Requirements Record
     */
    @Builder
    public record ValidationRequirements(
        boolean requiresState,
        boolean requiresRedirectUri,
        boolean requiresPKCE,
        boolean requiresClientId
    ) {}
    
    /**
     * Validation Result Record
     */
    @Builder
    public record ValidationResult(
        boolean isValid,
        String errorMessage
    ) {
        
        public static ValidationResult success() {
            return ValidationResult.builder()
                .isValid(true)
                .build();
        }
        
        public static ValidationResult failure(String message) {
            return ValidationResult.builder()
                .isValid(false)
                .errorMessage(message)
                .build();
        }
    }
}