package com.trademaster.brokerauth.entity;

import com.trademaster.brokerauth.enums.BrokerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Broker Account Entity
 * 
 * Represents a user's account connection to a specific broker.
 * Contains encrypted credentials and account metadata.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "broker_accounts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "broker_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    private Broker broker;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;
    
    // Account Identification
    @Column(name = "broker_user_id")
    private String brokerUserId;
    
    @Column(name = "broker_username")
    private String brokerUsername;
    
    @Column(name = "account_name")
    private String accountName;
    
    // Encrypted Credentials (using AES encryption)
    @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
    private String encryptedApiKey;
    
    @Column(name = "encrypted_api_secret", columnDefinition = "TEXT")
    private String encryptedApiSecret;
    
    @Column(name = "encrypted_password", columnDefinition = "TEXT")
    private String encryptedPassword;
    
    @Column(name = "encrypted_totp_secret", columnDefinition = "TEXT")
    private String encryptedTotpSecret;
    
    // OAuth specific fields
    @Column(name = "client_id")
    private String clientId;
    
    @Column(name = "redirect_uri")
    private String redirectUri;
    
    // Account Status
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;
    
    // Connection Statistics
    @Column(name = "total_connections")
    @Builder.Default
    private Integer totalConnections = 0;
    
    @Column(name = "successful_connections")
    @Builder.Default
    private Integer successfulConnections = 0;
    
    @Column(name = "failed_connections")
    @Builder.Default
    private Integer failedConnections = 0;
    
    @Column(name = "last_connection_at")
    private LocalDateTime lastConnectionAt;
    
    @Column(name = "last_error_message")
    private String lastErrorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "brokerAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BrokerSession> sessions = new ArrayList<>();
    
    // Business Methods
    
    /**
     * Check if account can be used for authentication
     */
    public boolean canAuthenticate() {
        return isActive && isVerified && hasRequiredCredentials();
    }
    
    /**
     * Check if account has required credentials based on broker type
     */
    public boolean hasRequiredCredentials() {
        if (brokerType == null) {
            return false;
        }
        
        return switch (brokerType) {
            case ZERODHA -> encryptedApiKey != null && encryptedApiSecret != null;
            case UPSTOX -> clientId != null && encryptedApiSecret != null;
            case ANGEL_ONE -> encryptedApiKey != null && encryptedPassword != null && encryptedTotpSecret != null;
            case ICICI_DIRECT -> brokerUserId != null && encryptedPassword != null;
        };
    }
    
    /**
     * Record successful connection
     */
    public void recordSuccessfulConnection() {
        this.totalConnections++;
        this.successfulConnections++;
        this.lastConnectionAt = LocalDateTime.now();
        this.lastErrorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Record failed connection
     */
    public void recordFailedConnection(String errorMessage) {
        this.totalConnections++;
        this.failedConnections++;
        this.lastConnectionAt = LocalDateTime.now();
        this.lastErrorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        if (totalConnections == 0) {
            return 0.0;
        }
        return (double) successfulConnections / totalConnections * 100.0;
    }
    
    /**
     * Check if account is considered healthy (good success rate)
     */
    public boolean isHealthy() {
        return getSuccessRate() >= 80.0 && failedConnections < 5;
    }
    
    /**
     * Get active sessions count
     */
    public long getActiveSessionsCount() {
        return sessions.stream()
                .filter(session -> session.isActive())
                .count();
    }
    
    /**
     * Check if max sessions limit is reached
     */
    public boolean hasReachedMaxSessions() {
        if (broker == null) {
            return false;
        }
        return getActiveSessionsCount() >= broker.getMaxSessionsPerUser();
    }
    
    /**
     * Verify account credentials (marks as verified)
     */
    public void verify() {
        this.isVerified = true;
        this.lastVerifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Deactivate account
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.lastErrorMessage = reason;
        this.updatedAt = LocalDateTime.now();
        
        // Invalidate all active sessions
        sessions.forEach(session -> {
            if (session.isActive()) {
                session.revoke("Account deactivated");
            }
        });
    }
    
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}