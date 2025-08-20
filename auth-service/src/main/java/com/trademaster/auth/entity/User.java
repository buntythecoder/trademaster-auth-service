package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User entity representing the primary authentication and account management table
 * 
 * Implements Spring Security UserDetails for seamless integration with
 * authentication and authorization mechanisms.
 * 
 * Features:
 * - Email-based authentication
 * - Account status management (active, suspended, locked, deactivated)
 * - KYC status tracking
 * - Subscription tier management
 * - Security tracking (failed attempts, device fingerprinting)
 * - Audit trail with creation/modification timestamps
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_kyc_status", columnList = "kycStatus"),
    @Index(name = "idx_users_subscription_tier", columnList = "subscriptionTier"),
    @Index(name = "idx_users_account_status", columnList = "accountStatus"),
    @Index(name = "idx_users_created_at", columnList = "createdAt"),
    @Index(name = "idx_users_last_login_at", columnList = "lastLoginAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier")
    @Builder.Default
    private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private InetAddress lastLoginIp;

    @Column(name = "device_fingerprint", length = 512)
    private String deviceFingerprint;

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
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoleAssignment> roleAssignments = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MfaConfiguration> mfaConfigurations = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserDevice> userDevices = new HashSet<>();

    // UserDetails implementation
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleAssignments.stream()
                .filter(assignment -> assignment.getIsActive() && 
                        (assignment.getExpiresAt() == null || assignment.getExpiresAt().isAfter(LocalDateTime.now())))
                .map(assignment -> new SimpleGrantedAuthority("ROLE_" + assignment.getRole().getRoleName()))
                .collect(Collectors.toSet());
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return accountStatus != AccountStatus.DEACTIVATED;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.LOCKED && 
               (accountLockedUntil == null || accountLockedUntil.isBefore(LocalDateTime.now()));
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        // Password expires after 90 days for compliance
        return passwordChangedAt == null || 
               passwordChangedAt.isAfter(LocalDateTime.now().minusDays(90));
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE && emailVerified;
    }

    // Business logic methods
    public boolean isMfaEnabled() {
        return mfaConfigurations.stream()
                .anyMatch(MfaConfiguration::getIsEnabled);
    }

    public boolean isSubscriptionActive() {
        return subscriptionTier != SubscriptionTier.FREE || 
               accountStatus == AccountStatus.ACTIVE;
    }

    public boolean requiresPasswordChange() {
        return passwordChangedAt == null || 
               passwordChangedAt.isBefore(LocalDateTime.now().minusDays(90));
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void lockAccount(int lockDurationMinutes) {
        this.accountStatus = AccountStatus.LOCKED;
        this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }

    public void updateLastLogin(InetAddress ipAddress, String deviceFingerprint) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.deviceFingerprint = deviceFingerprint;
        resetFailedLoginAttempts();
    }

    // Enums
    public enum KycStatus {
        PENDING("pending"),
        IN_PROGRESS("in_progress"),
        APPROVED("approved"),
        REJECTED("rejected");

        private final String value;

        KycStatus(String value) {
            this.value = value;
        }

        @JsonProperty
        public String getValue() {
            return value;
        }
    }

    public enum SubscriptionTier {
        FREE("free"),
        PREMIUM("premium"),
        PROFESSIONAL("professional"),
        ENTERPRISE("enterprise");

        private final String value;

        SubscriptionTier(String value) {
            this.value = value;
        }

        @JsonProperty
        public String getValue() {
            return value;
        }
    }

    public enum AccountStatus {
        ACTIVE("active"),
        SUSPENDED("suspended"),
        LOCKED("locked"),
        DEACTIVATED("deactivated");

        private final String value;

        AccountStatus(String value) {
            this.value = value;
        }

        @JsonProperty
        public String getValue() {
            return value;
        }
    }

    // JSON serialization control
    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("User{id=%d, email='%s', kycStatus=%s, subscriptionTier=%s, accountStatus=%s}", 
                           id, email, kycStatus, subscriptionTier, accountStatus);
    }
}