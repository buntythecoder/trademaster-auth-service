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

/**
 * Broker Entity
 * 
 * Represents a supported broker configuration in the system.
 * Contains broker-specific settings, API endpoints, and rate limits.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "brokers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Broker {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, unique = true)
    private BrokerType brokerType;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "base_url", nullable = false)
    private String baseUrl;
    
    @Column(name = "login_url")
    private String loginUrl;
    
    // Rate Limiting Configuration
    @Column(name = "rate_limit_per_second")
    private Integer rateLimitPerSecond;
    
    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;
    
    @Column(name = "rate_limit_per_day")
    private Integer rateLimitPerDay;
    
    // Session Configuration
    @Column(name = "session_validity_seconds")
    private Long sessionValiditySeconds;
    
    @Column(name = "max_sessions_per_user")
    private Integer maxSessionsPerUser;
    
    @Column(name = "supports_token_refresh")
    private Boolean supportsTokenRefresh;
    
    // Status and Configuration
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
    
    @Column(name = "is_maintenance", nullable = false)
    @Builder.Default
    private Boolean isMaintenance = false;
    
    @Column(name = "maintenance_message")
    private String maintenanceMessage;
    
    // Broker-specific Configuration (JSON stored as text)
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Business Methods
    
    /**
     * Check if broker is available for new connections
     */
    public boolean isAvailable() {
        return isEnabled && !isMaintenance;
    }
    
    /**
     * Check if broker supports real-time data
     */
    public boolean supportsRealTimeData() {
        return brokerType != null && brokerType.supportsRealTimeData();
    }
    
    /**
     * Get authentication type for this broker
     */
    public BrokerType.AuthType getAuthType() {
        return brokerType != null ? brokerType.getAuthType() : null;
    }
    
    /**
     * Initialize broker with default values based on broker type
     */
    public static Broker createDefault(BrokerType brokerType) {
        return Broker.builder()
                .brokerType(brokerType)
                .displayName(brokerType.getDisplayName())
                .baseUrl(brokerType.getBaseUrl())
                .sessionValiditySeconds(brokerType.getSessionValiditySeconds())
                .maxSessionsPerUser(brokerType.getMaxSessionsPerUser())
                .supportsTokenRefresh(brokerType.supportsTokenRefresh())
                .isEnabled(true)
                .isMaintenance(false)
                .build();
    }
    
    /**
     * Update maintenance status
     */
    public void setMaintenanceMode(boolean maintenance, String message) {
        this.isMaintenance = maintenance;
        this.maintenanceMessage = maintenance ? message : null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Update rate limits
     */
    public void updateRateLimits(Integer perSecond, Integer perMinute, Integer perDay) {
        this.rateLimitPerSecond = perSecond;
        this.rateLimitPerMinute = perMinute;
        this.rateLimitPerDay = perDay;
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        
        // Set defaults based on broker type if not specified
        if (brokerType != null) {
            if (sessionValiditySeconds == null) {
                sessionValiditySeconds = brokerType.getSessionValiditySeconds();
            }
            if (maxSessionsPerUser == null) {
                maxSessionsPerUser = brokerType.getMaxSessionsPerUser();
            }
            if (supportsTokenRefresh == null) {
                supportsTokenRefresh = brokerType.supportsTokenRefresh();
            }
        }
    }
}