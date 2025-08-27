package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * User Profile entity for extended user information, KYC data, and personalization settings
 * 
 * This entity stores:
 * - Personal information for KYC compliance
 * - Trading preferences and risk tolerance
 * - Behavioral AI settings and preferences
 * - Compliance and regulatory flags
 * - Encrypted sensitive data storage
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profiles_user_id", columnList = "userId"),
    @Index(name = "idx_user_profiles_risk_tolerance", columnList = "riskTolerance"),
    @Index(name = "idx_user_profiles_trading_experience", columnList = "tradingExperience"),
    @Index(name = "idx_user_profiles_country_code", columnList = "countryCode")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // Personal Information (Encrypted in production)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "country_code", length = 3)
    private String countryCode;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    // Trading Profile
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tolerance")
    private RiskTolerance riskTolerance;

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_experience")
    private TradingExperience tradingExperience;

    @Column(name = "annual_income_range", length = 50)
    private String annualIncomeRange;

    @Column(name = "net_worth_range", length = 50)
    private String netWorthRange;

    // Investment Goals (stored as array)
    @Type(JsonType.class)
    @Column(name = "investment_goals", columnDefinition = "text[]")
    private List<String> investmentGoals;

    // Behavioral AI Settings (JSON)
    @Type(JsonType.class)
    @Column(name = "behavioral_settings", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> behavioralSettings = Map.of();

    // User Preferences (JSON)
    @Type(JsonType.class)
    @Column(name = "preferences", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> preferences = Map.of();

    // KYC Documents Metadata (JSON - encrypted references)
    @Type(JsonType.class)
    @Column(name = "kyc_documents", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> kycDocuments = Map.of();

    // Compliance Flags (JSON)
    @Type(JsonType.class)
    @Column(name = "compliance_flags", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> complianceFlags = Map.of();

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by", length = 100)
    @Builder.Default
    private String updatedBy = "system";

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    // Business logic methods
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        return String.join(" ", 
            firstName != null ? firstName : "", 
            lastName != null ? lastName : "").trim();
    }

    public boolean isKycComplete() {
        return kycDocuments != null && 
               kycDocuments.containsKey("identity_verified") &&
               Boolean.TRUE.equals(kycDocuments.get("identity_verified")) &&
               kycDocuments.containsKey("address_verified") &&
               Boolean.TRUE.equals(kycDocuments.get("address_verified"));
    }

    public boolean hasComplianceFlags() {
        return complianceFlags != null && !complianceFlags.isEmpty();
    }

    public void updateBehavioralSetting(String key, Object value) {
        if (behavioralSettings == null) {
            behavioralSettings = Map.of();
        }
        behavioralSettings.put(key, value);
    }

    public void updatePreference(String key, Object value) {
        if (preferences == null) {
            preferences = Map.of();
        }
        preferences.put(key, value);
    }

    public Object getBehavioralSetting(String key) {
        return behavioralSettings != null ? behavioralSettings.get(key) : null;
    }

    public Object getPreference(String key) {
        return preferences != null ? preferences.get(key) : null;
    }

    // Risk scoring for behavioral AI
    public int calculateRiskScore() {
        int baseScore = 50; // Neutral starting point
        
        if (riskTolerance != null) {
            switch (riskTolerance) {
                case CONSERVATIVE -> baseScore -= 20;
                case MODERATE -> baseScore += 0;
                case AGGRESSIVE -> baseScore += 20;
                case VERY_AGGRESSIVE -> baseScore += 30;
            }
        }
        
        if (tradingExperience != null) {
            switch (tradingExperience) {
                case BEGINNER -> baseScore -= 10;
                case INTERMEDIATE -> baseScore += 0;
                case ADVANCED -> baseScore += 10;
                case PROFESSIONAL -> baseScore += 15;
            }
        }
        
        return Math.max(0, Math.min(100, baseScore));
    }

    // Enums
    public enum RiskTolerance {
        CONSERVATIVE("conservative"),
        MODERATE("moderate"),
        AGGRESSIVE("aggressive"),
        VERY_AGGRESSIVE("very_aggressive");

        private final String value;

        RiskTolerance(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum TradingExperience {
        BEGINNER("beginner"),
        INTERMEDIATE("intermediate"),
        ADVANCED("advanced"),
        PROFESSIONAL("professional");

        private final String value;

        TradingExperience(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserProfile{id=%d, userId=%d, riskTolerance=%s, tradingExperience=%s, countryCode='%s'}", 
                           id, userId, riskTolerance, tradingExperience, countryCode);
    }
}