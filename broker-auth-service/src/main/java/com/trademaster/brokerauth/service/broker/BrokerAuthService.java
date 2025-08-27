package com.trademaster.brokerauth.service.broker;

import com.trademaster.brokerauth.entity.BrokerSession;
import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.service.CredentialManagementService.BrokerCredentials;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Broker Authentication Service Interface
 * 
 * Defines the contract for implementing broker-specific authentication flows.
 * Each broker implementation handles its unique authentication mechanism.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface BrokerAuthService {
    
    /**
     * Get the broker type this service handles
     */
    BrokerType getBrokerType();
    
    /**
     * Generate authorization URL for OAuth flow
     * 
     * @param credentials Broker credentials
     * @param state Optional state parameter for security
     * @return Authorization URL to redirect user to
     */
    String getAuthorizationUrl(BrokerCredentials credentials, String state);
    
    /**
     * Exchange authorization code for access tokens
     * 
     * @param credentials Broker credentials
     * @param authorizationCode Authorization code from broker
     * @param state State parameter for validation
     * @return Authentication result with tokens
     */
    CompletableFuture<AuthResult> exchangeCodeForTokens(
            BrokerCredentials credentials, String authorizationCode, String state);
    
    /**
     * Refresh expired access token
     * 
     * @param credentials Broker credentials
     * @param refreshToken Current refresh token
     * @return New authentication result with refreshed tokens
     */
    CompletableFuture<AuthResult> refreshToken(BrokerCredentials credentials, String refreshToken);
    
    /**
     * Validate if session is still active
     * 
     * @param credentials Broker credentials
     * @param session Current session
     * @return true if session is valid
     */
    CompletableFuture<Boolean> validateSession(BrokerCredentials credentials, BrokerSession session);
    
    /**
     * Revoke/logout session
     * 
     * @param credentials Broker credentials
     * @param session Session to revoke
     * @return true if successfully revoked
     */
    CompletableFuture<Boolean> revokeSession(BrokerCredentials credentials, BrokerSession session);
    
    /**
     * Check if broker supports token refresh
     */
    boolean supportsTokenRefresh();
    
    /**
     * Get session validity duration in seconds
     */
    long getSessionValiditySeconds();
    
    /**
     * Get maximum concurrent sessions per user
     */
    int getMaxSessionsPerUser();
    
    /**
     * Authentication Result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AuthResult {
        private boolean success;
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresAt;
        private String errorMessage;
        private String errorCode;
        
        // Additional metadata
        @Builder.Default
        private String tokenType = "Bearer";
        private String scope;
        private Long expiresIn; // seconds
        
        /**
         * Create successful result
         */
        public static AuthResult success(String accessToken, String refreshToken, LocalDateTime expiresAt) {
            return AuthResult.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();
        }
        
        /**
         * Create successful result with expiration seconds
         */
        public static AuthResult success(String accessToken, String refreshToken, Long expiresIn) {
            LocalDateTime expiresAt = expiresIn != null ? 
                LocalDateTime.now().plusSeconds(expiresIn) : 
                LocalDateTime.now().plusHours(24); // Default 24 hours
                
            return AuthResult.builder()
                    .success(true)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .expiresIn(expiresIn)
                    .build();
        }
        
        /**
         * Create failure result
         */
        public static AuthResult failure(String errorMessage) {
            return AuthResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
        
        /**
         * Create failure result with error code
         */
        public static AuthResult failure(String errorMessage, String errorCode) {
            return AuthResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .errorCode(errorCode)
                    .build();
        }
        
        /**
         * Check if result has valid tokens
         */
        public boolean hasValidTokens() {
            return success && accessToken != null && !accessToken.isEmpty();
        }
        
        /**
         * Check if tokens are expired
         */
        public boolean isExpired() {
            return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
        }
        
        /**
         * Check if tokens need refresh (within 5 minutes of expiry)
         */
        public boolean needsRefresh() {
            if (expiresAt == null) {
                return false;
            }
            LocalDateTime refreshThreshold = LocalDateTime.now().plusMinutes(5);
            return expiresAt.isBefore(refreshThreshold);
        }
    }
    
    /**
     * Authentication Request Context
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AuthContext {
        private Long userId;
        private BrokerType brokerType;
        private String clientIp;
        private String userAgent;
        private String state;
        private String redirectUri;
        
        // Request metadata
        private String correlationId;
        private LocalDateTime requestTime;
        
        /**
         * Create auth context
         */
        public static AuthContext create(Long userId, BrokerType brokerType, String clientIp, String userAgent) {
            return AuthContext.builder()
                    .userId(userId)
                    .brokerType(brokerType)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .requestTime(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * Session Validation Result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ValidationResult {
        private boolean valid;
        private String reason;
        private boolean needsRefresh;
        private LocalDateTime nextValidation;
        
        public static ValidationResult valid() {
            return ValidationResult.builder().valid(true).build();
        }
        
        public static ValidationResult invalid(String reason) {
            return ValidationResult.builder()
                    .valid(false)
                    .reason(reason)
                    .build();
        }
        
        public static ValidationResult needsRefresh(String reason) {
            return ValidationResult.builder()
                    .valid(false)
                    .needsRefresh(true)
                    .reason(reason)
                    .build();
        }
    }
}