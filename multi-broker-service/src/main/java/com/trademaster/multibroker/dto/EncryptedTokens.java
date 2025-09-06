package com.trademaster.multibroker.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Encrypted Tokens Record
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Represents encrypted OAuth tokens for secure storage. Contains AES-256
 * encrypted access and refresh tokens along with metadata required for
 * token lifecycle management and security validation.
 * 
 * Security Features:
 * - AES-256-GCM encryption for token storage
 * - Initialization vector (IV) for crypto security
 * - Token expiration tracking
 * - Secure token rotation support
 * - No plaintext token exposure
 * 
 * @param encryptedAccessToken AES-256 encrypted access token
 * @param encryptedRefreshToken AES-256 encrypted refresh token (optional)
 * @param initializationVector Crypto IV for secure decryption
 * @param tokenType Bearer, Basic, or custom token type
 * @param expiresAt Token expiration timestamp
 * @param scope Granted OAuth scopes
 * @param encryptedAt When tokens were encrypted
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Secure Token Storage)
 */
@Builder
public record EncryptedTokens(
    String encryptedAccessToken,
    String encryptedRefreshToken,
    String initializationVector,
    String tokenType,
    Instant expiresAt,
    String scope,
    Instant encryptedAt
) {
    
    /**
     * Check if tokens are expired
     * 
     * @return true if tokens have expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if tokens are about to expire
     * 
     * @param thresholdMinutes Minutes before expiry to consider "about to expire"
     * @return true if tokens expire within threshold
     */
    public boolean isAboutToExpire(long thresholdMinutes) {
        if (expiresAt == null) {
            return true;
        }
        
        Instant threshold = Instant.now().plusSeconds(thresholdMinutes * 60);
        return expiresAt.isBefore(threshold);
    }
    
    /**
     * Get remaining validity duration in seconds
     * 
     * @return Seconds until expiration, or 0 if expired
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) {
            return 0L;
        }
        
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0L, remaining);
    }
    
    /**
     * Check if refresh token is available
     * 
     * @return true if refresh token is present
     */
    public boolean hasRefreshToken() {
        return encryptedRefreshToken != null && !encryptedRefreshToken.trim().isEmpty();
    }
    
    /**
     * Validate encryption metadata
     * 
     * @return true if encryption metadata is valid
     */
    public boolean hasValidEncryption() {
        return initializationVector != null && 
               !initializationVector.trim().isEmpty() &&
               encryptedAccessToken != null && 
               !encryptedAccessToken.trim().isEmpty() &&
               encryptedAt != null;
    }
    
    /**
     * Get token age in seconds
     * 
     * @return Seconds since tokens were encrypted
     */
    public long getTokenAgeSeconds() {
        if (encryptedAt == null) {
            return Long.MAX_VALUE;
        }
        
        return java.time.Duration.between(encryptedAt, Instant.now()).getSeconds();
    }
    
    /**
     * Check if tokens need rotation based on age
     * 
     * @param maxAgeHours Maximum token age in hours
     * @return true if tokens should be rotated
     */
    public boolean needsRotation(long maxAgeHours) {
        return getTokenAgeSeconds() > (maxAgeHours * 3600);
    }
    
    /**
     * Create safe version for logging (removes sensitive data)
     * 
     * @return Safe version without encrypted tokens
     */
    public EncryptedTokens sanitizeForLogging() {
        return EncryptedTokens.builder()
            .encryptedAccessToken("***REDACTED***")
            .encryptedRefreshToken(hasRefreshToken() ? "***REDACTED***" : null)
            .initializationVector("***REDACTED***")
            .tokenType(tokenType)
            .expiresAt(expiresAt)
            .scope(scope)
            .encryptedAt(encryptedAt)
            .build();
    }
    
    /**
     * Override toString to prevent accidental token logging
     */
    @Override
    public String toString() {
        return String.format("EncryptedTokens[type=%s, expires=%s, scope=%s, encrypted=%s]",
                           tokenType, expiresAt, scope, encryptedAt);
    }
}