package com.trademaster.multibroker.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Broker Connection Entity
 * 
 * MANDATORY: Immutable Records + JPA + Zero Placeholders + Zero Trust Security
 * 
 * Represents a secure connection between a user and a broker with encrypted token storage.
 * Implements Zero Trust security with comprehensive audit trail and health monitoring.
 * 
 * Security Features:
 * - AES-256 encrypted token storage
 * - Token rotation with expiry tracking
 * - Comprehensive audit logging
 * - Rate limit tracking per broker
 * - Connection health monitoring
 * 
 * Performance Features:
 * - JSONB storage for capabilities (PostgreSQL optimized)
 * - Optimized indexes for frequent queries
 * - Connection pooling support
 * - Circuit breaker integration
 * 
 * Database Constraints:
 * - Unique constraint on (userId, brokerType, accountId)
 * - Encrypted tokens never null when status = CONNECTED
 * - lastHealthCheck updated on every API call
 * - consecutiveFailures reset on successful call
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + JPA + Encrypted Storage)
 */
@Entity
@Table(
    name = "broker_connections",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_broker_account", 
        columnNames = {"user_id", "broker_type", "account_id"}
    ),
    indexes = {
        @Index(name = "idx_broker_connections_user_id", columnList = "user_id"),
        @Index(name = "idx_broker_connections_status", columnList = "status"),
        @Index(name = "idx_broker_connections_broker_type", columnList = "broker_type"),
        @Index(name = "idx_broker_connections_health_check", columnList = "last_health_check"),
        @Index(name = "idx_broker_connections_token_expiry", columnList = "token_expires_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 20)
    private BrokerType brokerType;
    
    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    // OAuth Token Storage (AES-256 Encrypted)
    @JsonIgnore // Never serialize tokens in JSON responses
    @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
    private String encryptedAccessToken;
    
    @JsonIgnore // Never serialize refresh tokens
    @Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
    private String encryptedRefreshToken;
    
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    
    // Connection Status and Health Monitoring
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    
    @Column(name = "last_successful_call")
    private Instant lastSuccessfulCall;
    
    @Column(name = "last_health_check")
    private Instant lastHealthCheck;
    
    @Column(name = "connected_at")
    private Instant connectedAt;
    
    @Column(name = "disconnected_at")
    private Instant disconnectedAt;
    
    @Column(name = "last_synced")
    private Instant lastSynced;
    
    @Column(name = "sync_count")
    @Builder.Default
    private Long syncCount = 0L;
    
    @Column(name = "error_count")
    @Builder.Default
    private Long errorCount = 0L;
    
    @Column(name = "is_healthy")
    @Builder.Default
    private Boolean healthy = true;
    
    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
    
    // Broker Capabilities (JSONB for PostgreSQL optimization)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private BrokerCapabilities capabilities;
    
    // Connection Configuration (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_config", columnDefinition = "jsonb")
    private BrokerConnectionConfig config;
    
    // Rate Limiting Tracking
    @Column(name = "api_calls_today")
    @Builder.Default
    private Long apiCallsToday = 0L;
    
    @Column(name = "last_api_call")
    private Instant lastApiCall;
    
    @Column(name = "rate_limit_reset_at")
    private Instant rateLimitResetAt;
    
    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    // Functional Methods for Business Logic
    
    /**
     * Check if connection is healthy and can process requests
     */
    public boolean isHealthy() {
        return (healthy != null ? healthy : true) &&
               status.isHealthy() && 
               !isTokenExpired() && 
               !isRateLimited();
    }
    
    /**
     * Get boolean wrapper for isHealthy (for compatibility)
     */
    public Boolean getIsHealthy() {
        return isHealthy();
    }
    
    /**
     * Get broker ID (account ID)
     */
    public String getBrokerId() {
        return accountId;
    }
    
    /**
     * Get last synced timestamp
     */
    public Instant getLastSynced() {
        return lastSynced != null ? lastSynced : lastSuccessfulCall;
    }
    
    /**
     * Get error count
     */
    public Long getErrorCount() {
        return errorCount != null ? errorCount : 
               (consecutiveFailures != null ? consecutiveFailures.longValue() : 0L);
    }
    
    /**
     * Create builder from current instance
     */
    public BrokerConnectionBuilder toBuilder() {
        return BrokerConnection.builder()
            .id(this.id)
            .userId(this.userId)
            .brokerType(this.brokerType)
            .accountId(this.accountId)
            .displayName(this.displayName)
            .encryptedAccessToken(this.encryptedAccessToken)
            .encryptedRefreshToken(this.encryptedRefreshToken)
            .tokenExpiresAt(this.tokenExpiresAt)
            .status(this.status)
            .lastSuccessfulCall(this.lastSuccessfulCall)
            .lastHealthCheck(this.lastHealthCheck)
            .connectedAt(this.connectedAt)
            .disconnectedAt(this.disconnectedAt)
            .lastSynced(this.lastSynced)
            .syncCount(this.syncCount)
            .errorCount(this.errorCount)
            .healthy(this.healthy)
            .consecutiveFailures(this.consecutiveFailures)
            .lastErrorMessage(this.lastErrorMessage)
            .capabilities(this.capabilities)
            .config(this.config)
            .apiCallsToday(this.apiCallsToday)
            .lastApiCall(this.lastApiCall)
            .rateLimitResetAt(this.rateLimitResetAt)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .version(this.version);
    }
    
    /**
     * Check if OAuth token has expired
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && 
               Instant.now().isAfter(tokenExpiresAt);
    }
    
    /**
     * Check if connection is rate limited
     */
    public boolean isRateLimited() {
        if (rateLimitResetAt == null || brokerType == null) {
            return false;
        }
        
        return Instant.now().isBefore(rateLimitResetAt) &&
               apiCallsToday >= brokerType.getRateLimitPerMinute();
    }
    
    /**
     * Check if connection requires user attention
     */
    public boolean requiresAttention() {
        return status.isRequiresAttention() ||
               isTokenExpired() ||
               (consecutiveFailures != null && consecutiveFailures >= 3);
    }
    
    /**
     * Get time until next allowed API call (for rate limiting)
     */
    public long getSecondsUntilNextCall() {
        if (!isRateLimited()) {
            return 0L;
        }
        
        return rateLimitResetAt.getEpochSecond() - Instant.now().getEpochSecond();
    }
    
    /**
     * Record successful API call for health monitoring
     */
    public void recordSuccessfulCall() {
        this.lastSuccessfulCall = Instant.now();
        this.lastHealthCheck = Instant.now();
        this.consecutiveFailures = 0;
        this.lastErrorMessage = null;
        this.lastApiCall = Instant.now();
        this.apiCallsToday = (this.apiCallsToday != null ? this.apiCallsToday : 0L) + 1L;
        
        if (this.status == ConnectionStatus.ERROR || this.status == ConnectionStatus.RATE_LIMITED) {
            this.status = ConnectionStatus.CONNECTED;
        }
    }
    
    /**
     * Record failed API call for health monitoring
     */
    public void recordFailedCall(String errorMessage) {
        this.lastHealthCheck = Instant.now();
        this.consecutiveFailures = (this.consecutiveFailures != null ? this.consecutiveFailures : 0) + 1;
        this.lastErrorMessage = errorMessage;
        this.lastApiCall = Instant.now();
        
        // Update status based on failure count and error type
        if (errorMessage != null && errorMessage.toLowerCase().contains("rate limit")) {
            this.status = ConnectionStatus.RATE_LIMITED;
            this.rateLimitResetAt = Instant.now().plusSeconds(brokerType.getRateLimitWindowMs() / 1000);
        } else if (errorMessage != null && errorMessage.toLowerCase().contains("token")) {
            this.status = ConnectionStatus.TOKEN_EXPIRED;
        } else if (this.consecutiveFailures >= 5) {
            this.status = ConnectionStatus.ERROR;
        }
    }
    
    /**
     * Reset daily API call counter (called by scheduled task)
     */
    public void resetDailyApiCalls() {
        this.apiCallsToday = 0L;
        this.rateLimitResetAt = null;
    }
    
    /**
     * Broker Capabilities Record
     */
    public record BrokerCapabilities(
        Set<String> supportedOrderTypes,
        Set<String> supportedExchanges,
        java.math.BigDecimal maxOrderValue,
        Long maxOrderQuantity,
        boolean supportsMarginTrading,
        boolean supportsOptionsTrading,
        boolean supportsCommodityTrading,
        boolean supportsInternationalTrading,
        Map<String, Object> rateLimits,
        Set<String> supportedFeatures
    ) {}
    
    /**
     * Broker Connection Configuration Record
     */
    public record BrokerConnectionConfig(
        String clientId,
        String clientSecret,
        String apiKey,
        List<String> scopes,
        Map<String, String> additionalParams,
        boolean sandboxMode,
        String webhookUrl,
        int timeoutSeconds,
        int retryAttempts
    ) {}
}