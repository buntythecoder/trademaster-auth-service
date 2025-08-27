package com.trademaster.brokerauth.entity;

import com.trademaster.brokerauth.enums.BrokerType;
import com.trademaster.brokerauth.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Broker Session Entity
 * 
 * Represents an active authentication session with a broker.
 * Contains access tokens, refresh tokens, and session metadata.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "broker_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_account_id", nullable = false)
    private BrokerAccount brokerAccount;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.PENDING;
    
    // Token Information (encrypted)
    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;
    
    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;
    
    @Column(name = "token_type")
    @Builder.Default
    private String tokenType = "Bearer";
    
    // Session Timing
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;
    
    // Request Information
    @Column(name = "request_token")
    private String requestToken;
    
    @Column(name = "authorization_code")
    private String authorizationCode;
    
    @Column(name = "redirect_uri")
    private String redirectUri;
    
    // Session Metadata
    @Column(name = "client_ip")
    private String clientIp;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "login_method")
    private String loginMethod;
    
    // Usage Statistics
    @Column(name = "api_calls_count")
    @Builder.Default
    private Integer apiCallsCount = 0;
    
    @Column(name = "rate_limit_hits")
    @Builder.Default
    private Integer rateLimitHits = 0;
    
    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;
    
    // Status Information
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "revocation_reason")
    private String revocationReason;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Business Methods
    
    /**
     * Check if session is active and usable
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE && 
               expiresAt != null && 
               expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * Check if session is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if session needs refresh
     */
    public boolean needsRefresh() {
        if (expiresAt == null) {
            return false;
        }
        
        // Check if expires within next 5 minutes
        LocalDateTime refreshThreshold = LocalDateTime.now().plusMinutes(5);
        return expiresAt.isBefore(refreshThreshold);
    }
    
    /**
     * Check if session can be refreshed
     */
    public boolean canBeRefreshed() {
        if (!brokerType.supportsTokenRefresh()) {
            return false;
        }
        
        if (encryptedRefreshToken == null) {
            return false;
        }
        
        if (refreshExpiresAt != null && refreshExpiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return status == SessionStatus.EXPIRED || status == SessionStatus.ACTIVE;
    }
    
    /**
     * Activate session with tokens
     */
    public void activate(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        if (!status.canTransitionTo(SessionStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot activate session in " + status + " state");
        }
        
        this.status = SessionStatus.ACTIVE;
        this.encryptedAccessToken = accessToken; // Should be encrypted before calling this
        this.encryptedRefreshToken = refreshToken; // Should be encrypted before calling this
        this.expiresAt = expiresAt;
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Clear any previous error
        this.errorMessage = null;
        
        // Update broker account stats
        if (brokerAccount != null) {
            brokerAccount.recordSuccessfulConnection();
        }
    }
    
    /**
     * Mark session as expired
     */
    public void expire() {
        if (status.canTransitionTo(SessionStatus.EXPIRED)) {
            this.status = SessionStatus.EXPIRED;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Mark session as error state
     */
    public void markError(String errorMessage) {
        if (status.canTransitionTo(SessionStatus.ERROR)) {
            this.status = SessionStatus.ERROR;
            this.errorMessage = errorMessage;
            this.errorCount++;
            this.updatedAt = LocalDateTime.now();
            
            // Update broker account stats
            if (brokerAccount != null) {
                brokerAccount.recordFailedConnection(errorMessage);
            }
        }
    }
    
    /**
     * Revoke session
     */
    public void revoke(String reason) {
        if (status.canTransitionTo(SessionStatus.REVOKED)) {
            this.status = SessionStatus.REVOKED;
            this.revocationReason = reason;
            this.revokedAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            
            // Clear sensitive data
            this.encryptedAccessToken = null;
            this.encryptedRefreshToken = null;
        }
    }
    
    /**
     * Start token refresh process
     */
    public void startRefresh() {
        if (status.canTransitionTo(SessionStatus.REFRESHING)) {
            this.status = SessionStatus.REFRESHING;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Complete token refresh
     */
    public void completeRefresh(String newAccessToken, String newRefreshToken, LocalDateTime newExpiresAt) {
        if (status != SessionStatus.REFRESHING) {
            throw new IllegalStateException("Cannot complete refresh when not in REFRESHING state");
        }
        
        this.encryptedAccessToken = newAccessToken; // Should be encrypted
        this.encryptedRefreshToken = newRefreshToken; // Should be encrypted
        this.expiresAt = newExpiresAt;
        this.lastRefreshedAt = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record API call
     */
    public void recordApiCall() {
        this.apiCallsCount++;
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record rate limit hit
     */
    public void recordRateLimitHit() {
        this.rateLimitHits++;
        this.updatedAt = LocalDateTime.now();
        
        // Temporarily mark as rate limited if too many hits
        if (rateLimitHits > 10 && status.canTransitionTo(SessionStatus.RATE_LIMITED)) {
            this.status = SessionStatus.RATE_LIMITED;
        }
    }
    
    /**
     * Clear rate limit status
     */
    public void clearRateLimit() {
        if (status == SessionStatus.RATE_LIMITED && status.canTransitionTo(SessionStatus.ACTIVE)) {
            this.status = SessionStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Get session age in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Check if session is healthy (low error rate)
     */
    public boolean isHealthy() {
        if (apiCallsCount == 0) {
            return true;
        }
        return (double) errorCount / apiCallsCount < 0.1; // Less than 10% error rate
    }
    
    @PrePersist
    private void prePersist() {
        if (sessionId == null) {
            sessionId = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    private void preUpdate() {
        // Auto-expire if past expiration time
        if (status == SessionStatus.ACTIVE && isExpired()) {
            this.status = SessionStatus.EXPIRED;
        }
    }
}