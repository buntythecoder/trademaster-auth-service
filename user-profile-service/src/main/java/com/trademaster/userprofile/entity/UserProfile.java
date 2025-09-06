package com.trademaster.userprofile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User Profile Entity
 * 
 * MANDATORY: Java 24 + Virtual Threads Architecture - Rule #1
 * MANDATORY: Immutability & Records Usage - Rule #9
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: Dynamic Configuration externalization - Rule #16
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Getter
@Entity
@Table(name = "user_profiles",
    indexes = {
        @Index(name = "idx_user_profiles_user_id", columnList = "user_id"),
        @Index(name = "idx_user_profiles_created_at", columnList = "created_at"),
        @Index(name = "idx_user_profiles_updated_at", columnList = "updated_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "user_profiles_user_id_unique", columnNames = "user_id")
    })
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_info", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> personalInfo;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trading_preferences", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> tradingPreferences;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kyc_information", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> kycInformation;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_settings", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> notificationSettings;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserDocument> documents = new ArrayList<>();
    
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileAuditLog> auditLogs = new ArrayList<>();
    
    @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;
    
    // Default constructor for JPA
    protected UserProfile() {}
    
    // Builder pattern for controlled creation - Rule #9
    public static UserProfileBuilder builder() {
        return new UserProfileBuilder();
    }

    // Functional updates (immutable-style) - Rule #3
    public UserProfile withPersonalInfo(Map<String, Object> personalInfo) {
        this.personalInfo = personalInfo;
        return this;
    }
    
    public UserProfile withTradingPreferences(Map<String, Object> tradingPreferences) {
        this.tradingPreferences = tradingPreferences;
        return this;
    }
    
    public UserProfile withKycInformation(Map<String, Object> kycInformation) {
        this.kycInformation = kycInformation;
        return this;
    }
    
    public UserProfile withNotificationSettings(Map<String, Object> notificationSettings) {
        this.notificationSettings = notificationSettings;
        return this;
    }
    
    // Business Logic Methods - Rule #3 Functional Programming
    public boolean isKycCompleted() {
        return kycInformation != null && 
               "VERIFIED".equals(kycInformation.get("kycStatus"));
    }
    
    public RiskLevel getRiskLevel() {
        if (tradingPreferences == null) return RiskLevel.LOW;
        
        Object riskLevel = tradingPreferences.get("riskLevel");
        return switch (riskLevel) {
            case String s when "LOW".equals(s) -> RiskLevel.LOW;
            case String s when "MODERATE".equals(s) -> RiskLevel.MODERATE;
            case String s when "HIGH".equals(s) -> RiskLevel.HIGH;
            case String s when "VERY_HIGH".equals(s) -> RiskLevel.VERY_HIGH;
            default -> RiskLevel.LOW;
        };
    }
    
    // Helper methods for relationships
    public void addDocument(UserDocument document) {
        documents.add(document);
        document.setUserProfile(this);
    }
    
    public void removeDocument(UserDocument document) {
        documents.remove(document);
        document.setUserProfile(null);
    }
    
    public void addAuditLog(ProfileAuditLog auditLog) {
        auditLogs.add(auditLog);
        auditLog.setUserProfile(this);
    }
    
    // Builder class for controlled object creation - Rule #9
    public static class UserProfileBuilder {
        private final UserProfile profile = new UserProfile();
        
        public UserProfileBuilder userId(UUID userId) {
            profile.userId = userId;
            return this;
        }
        
        public UserProfileBuilder personalInfo(Map<String, Object> personalInfo) {
            profile.personalInfo = personalInfo;
            return this;
        }
        
        public UserProfileBuilder tradingPreferences(Map<String, Object> tradingPreferences) {
            profile.tradingPreferences = tradingPreferences;
            return this;
        }
        
        public UserProfileBuilder kycInformation(Map<String, Object> kycInformation) {
            profile.kycInformation = kycInformation;
            return this;
        }
        
        public UserProfileBuilder notificationSettings(Map<String, Object> notificationSettings) {
            profile.notificationSettings = notificationSettings;
            return this;
        }
        
        public UserProfile build() {
            // Validation in build method per TradeMaster standards
            if (profile.userId == null) {
                throw new IllegalStateException("UserId is required");
            }
            if (profile.personalInfo == null) {
                throw new IllegalStateException("PersonalInfo is required");
            }
            if (profile.tradingPreferences == null) {
                throw new IllegalStateException("TradingPreferences is required");
            }
            if (profile.kycInformation == null) {
                throw new IllegalStateException("KycInformation is required");
            }
            if (profile.notificationSettings == null) {
                throw new IllegalStateException("NotificationSettings is required");
            }
            
            log.debug("Built UserProfile for user: {}", profile.userId);
            return profile;
        }
    }
}
