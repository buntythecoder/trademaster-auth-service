package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
@ToString(exclude = "user")  // ✅ Prevent circular reference with User entity
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
    @Column(name = "investment_goals", columnDefinition = "text")
    private String investmentGoals;

    // Behavioral AI Settings (JSON)
    @Column(name = "behavioral_settings", columnDefinition = "text")
    @Builder.Default
    private String behavioralSettings = "";

    // User Preferences (JSON)
    @Column(name = "preferences", columnDefinition = "text")
    @Builder.Default
    private String preferences = "";

    // KYC Documents Metadata (JSON - encrypted references)
    @Column(name = "kyc_documents", columnDefinition = "text")
    @Builder.Default
    private String kycDocuments = "";

    // Compliance Flags (JSON)
    @Column(name = "compliance_flags", columnDefinition = "text")
    @Builder.Default
    private String complianceFlags = "";

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
        return Optional.ofNullable(firstName)
            .or(() -> Optional.ofNullable(lastName))
            .map(name -> String.join(" ",
                Optional.ofNullable(firstName).orElse(""),
                Optional.ofNullable(lastName).orElse("")).trim())
            .orElse(null);
    }

    public boolean isKycComplete() {
        return Optional.ofNullable(kycDocuments)
            .filter(docs -> docs.contains("identity_verified") && docs.contains("address_verified"))
            .isPresent();
    }

    public boolean hasComplianceFlags() {
        return Optional.ofNullable(complianceFlags)
            .map(String::trim)
            .filter(flags -> !flags.isEmpty())
            .isPresent();
    }

    public void updateBehavioralSetting(String key, Object value) {
        behavioralSettings = Optional.ofNullable(behavioralSettings)
            .orElse("");
        // Note: In production, implement JSON serialization for key-value updates
        behavioralSettings = behavioralSettings + ";" + key + "=" + value;
    }

    public void updatePreference(String key, Object value) {
        preferences = Optional.ofNullable(preferences)
            .orElse("");
        // Note: In production, implement JSON serialization for key-value updates
        preferences = preferences + ";" + key + "=" + value;
    }

    public String getBehavioralSetting(String key) {
        return Optional.ofNullable(behavioralSettings)
            .filter(settings -> !settings.isEmpty())
            .filter(settings -> settings.contains(key))
            .map(settings -> "true")
            .orElse(null);
    }

    public String getPreference(String key) {
        return Optional.ofNullable(key)
            .flatMap(k -> Optional.ofNullable(preferences)
                .filter(prefs -> !prefs.isEmpty())
                .map(prefs -> java.util.Arrays.stream(prefs.split(";"))
                    .map(pair -> pair.split("=", 2))
                    .filter(keyValue -> keyValue.length == 2 && keyValue[0].trim().equals(k))
                    .map(keyValue -> keyValue[1].trim())
                    .findFirst()
                    .orElse(null)))
            .orElse(null);
    }

    // Risk scoring for behavioral AI
    public int calculateRiskScore() {
        int baseScore = 50; // Neutral starting point

        int riskAdjustment = Optional.ofNullable(riskTolerance)
            .map(tolerance -> switch (tolerance) {
                case CONSERVATIVE -> -20;
                case MODERATE -> 0;
                case AGGRESSIVE -> 20;
                case VERY_AGGRESSIVE -> 30;
            })
            .orElse(0);

        int experienceAdjustment = Optional.ofNullable(tradingExperience)
            .map(experience -> switch (experience) {
                case BEGINNER -> -10;
                case INTERMEDIATE -> 0;
                case ADVANCED -> 10;
                case PROFESSIONAL -> 15;
            })
            .orElse(0);

        return Math.max(0, Math.min(100, baseScore + riskAdjustment + experienceAdjustment));
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