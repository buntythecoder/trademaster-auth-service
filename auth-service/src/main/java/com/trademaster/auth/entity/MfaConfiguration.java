package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MFA Configuration entity for Multi-Factor Authentication settings
 * 
 * Supports multiple MFA types:
 * - SMS OTP
 * - Email OTP
 * - TOTP (Time-based One-Time Password) via authenticator apps
 * - Biometric authentication
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "mfa_configurations",
       indexes = {
           @Index(name = "idx_mfa_configurations_user_id", columnList = "userId"),
           @Index(name = "idx_mfa_configurations_type", columnList = "mfaType"),
           @Index(name = "idx_mfa_configurations_enabled", columnList = "isEnabled")
       },
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "mfa_type"})
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MfaConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false, length = 20)
    private MfaType mfaType;

    @JsonIgnore
    @Column(name = "secret_key", length = 512)
    private String secretKey;

    @Type(JsonType.class)
    @Column(name = "backup_codes", columnDefinition = "text[]")
    private List<String> backupCodes;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    // Business logic methods
    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void enable() {
        this.isEnabled = true;
        if (this.verifiedAt == null) {
            this.verifiedAt = LocalDateTime.now();
        }
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void verify() {
        this.verifiedAt = LocalDateTime.now();
        if (this.isEnabled == null || !this.isEnabled) {
            this.isEnabled = true;
        }
    }

    public boolean hasBackupCodes() {
        return backupCodes != null && !backupCodes.isEmpty();
    }

    public int getBackupCodesCount() {
        return backupCodes != null ? backupCodes.size() : 0;
    }

    public void regenerateBackupCodes(List<String> newBackupCodes) {
        this.backupCodes = newBackupCodes;
    }

    // MFA Type enum
    public enum MfaType {
        SMS("sms"),
        EMAIL("email"),
        TOTP("totp"),
        BIOMETRIC("biometric");

        private final String value;

        MfaType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MfaType fromValue(String value) {
            for (MfaType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown MFA type: " + value);
        }
    }

    // Helper methods for different MFA types
    public boolean isSmsType() {
        return MfaType.SMS.equals(mfaType);
    }

    public boolean isEmailType() {
        return MfaType.EMAIL.equals(mfaType);
    }

    public boolean isTotpType() {
        return MfaType.TOTP.equals(mfaType);
    }

    public boolean isBiometricType() {
        return MfaType.BIOMETRIC.equals(mfaType);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("MfaConfiguration{id=%d, userId=%d, mfaType=%s, isEnabled=%s, isVerified=%s}", 
                           id, userId, mfaType, isEnabled, isVerified());
    }
}