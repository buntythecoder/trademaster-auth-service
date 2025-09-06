package com.trademaster.multibroker.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.Instant;

/**
 * Broker OAuth Tokens Record
 * 
 * MANDATORY: Immutable Record + Security + Zero Placeholders
 * 
 * Represents OAuth 2.0 tokens from broker authentication flows.
 * Contains access token, refresh token, and expiry information.
 * 
 * Security Features:
 * - JsonIgnore on sensitive fields prevents accidental serialization
 * - Immutable record prevents token modification
 * - Built-in validation for token expiry
 * - No logging of sensitive data
 * 
 * Token Lifecycle:
 * 1. Obtained from broker OAuth callback
 * 2. Validated for completeness and expiry
 * 3. Encrypted before database storage
 * 4. Used for API authentication
 * 5. Refreshed when expired
 * 
 * @param accessToken OAuth access token for API calls
 * @param refreshToken OAuth refresh token for token renewal  
 * @param tokenType Token type (typically "Bearer")
 * @param expiresAt Token expiration timestamp
 * @param scope OAuth scope granted by broker
 * @param userId User identifier associated with tokens
 * @param brokerId Broker identifier for token association
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (OAuth Token Management)
 */
@Builder
public record BrokerTokens(
    @JsonIgnore String accessToken,  // Never serialize access tokens
    @JsonIgnore String refreshToken, // Never serialize refresh tokens
    String tokenType,
    Instant expiresAt,
    String scope,
    String userId,
    String brokerId
) {
    
    /**
     * Validate tokens are complete and not expired
     * 
     * @return true if tokens are valid for use
     */
    public boolean isValid() {
        return accessToken != null && 
               !accessToken.trim().isEmpty() &&
               expiresAt != null &&
               !isExpired();
    }
    
    /**
     * Check if access token has expired
     * 
     * @return true if token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && 
               Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is about to expire (within 5 minutes)
     * 
     * @return true if token expires soon
     */
    public boolean isExpiringSoon() {
        return expiresAt != null &&
               Instant.now().plusSeconds(300).isAfter(expiresAt);
    }
    
    /**
     * Get seconds until token expiry
     * 
     * @return seconds until expiry, 0 if already expired
     */
    public long getSecondsUntilExpiry() {
        if (expiresAt == null) {
            return Long.MAX_VALUE; // No expiry
        }
        
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }
    
    /**
     * Check if refresh token is available for renewal
     * 
     * @return true if refresh token exists
     */
    public boolean canRefresh() {
        return refreshToken != null && !refreshToken.trim().isEmpty();
    }
    
    /**
     * Create tokens for specific broker and user
     * 
     * @param accessToken OAuth access token
     * @param refreshToken OAuth refresh token  
     * @param expiresInSeconds Token lifetime in seconds
     * @param scope OAuth scope
     * @param userId User identifier
     * @param brokerId Broker identifier
     * @return BrokerTokens instance
     */
    public static BrokerTokens create(String accessToken, 
                                    String refreshToken,
                                    long expiresInSeconds, 
                                    String scope,
                                    String userId, 
                                    String brokerId) {
        return BrokerTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresAt(Instant.now().plusSeconds(expiresInSeconds))
            .scope(scope)
            .userId(userId)
            .brokerId(brokerId)
            .build();
    }
    
    /**
     * Create tokens from broker OAuth response
     * 
     * @param accessToken OAuth access token
     * @param refreshToken OAuth refresh token
     * @param expiresAt Token expiration time
     * @param scope OAuth scope
     * @param userId User identifier  
     * @param brokerId Broker identifier
     * @return BrokerTokens instance
     */
    public static BrokerTokens of(String accessToken,
                                String refreshToken, 
                                Instant expiresAt,
                                String scope,
                                String userId,
                                String brokerId) {
        return BrokerTokens.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresAt(expiresAt)
            .scope(scope)
            .userId(userId)
            .brokerId(brokerId)
            .build();
    }
    
    /**
     * Create masked version for logging (no sensitive data)
     * 
     * @return Safe version for logging
     */
    public String toSafeString() {
        return String.format("BrokerTokens(brokerId=%s, userId=%s, tokenType=%s, scope=%s, expiresAt=%s, valid=%s)", 
                           brokerId, userId, tokenType, scope, expiresAt, isValid());
    }
    
    /**
     * Override toString to prevent accidental token logging
     */
    @Override
    public String toString() {
        return toSafeString();
    }
}