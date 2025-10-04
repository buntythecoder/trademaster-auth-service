package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User Device entity for device fingerprinting and trusted device management
 * 
 * Tracks user devices for security purposes:
 * - Device fingerprinting for fraud detection
 * - Trusted device management
 * - Location tracking for suspicious activity detection
 * - Device type classification
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_devices",
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_user_device", columnNames = {"user_id", "device_fingerprint"})
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private java.util.UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @Column(name = "device_fingerprint", nullable = false, length = 255)
    private String deviceFingerprint;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean trusted = false;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "trust_expiry")
    private LocalDateTime trustExpiry;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public boolean isTrusted() {
        return Boolean.TRUE.equals(this.trusted) && 
               (this.trustExpiry == null || this.trustExpiry.isAfter(LocalDateTime.now()));
    }

    public void trust(int durationDays) {
        this.trusted = true;
        this.trustExpiry = LocalDateTime.now().plusDays(durationDays);
    }

    public void revokeTrust() {
        this.trusted = false;
        this.trustExpiry = null;
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.trustExpiry != null && this.trustExpiry.isBefore(LocalDateTime.now());
    }

    public boolean isNewDevice() {
        return this.firstSeen == null || this.firstSeen.isAfter(LocalDateTime.now().minusHours(1));
    }

    public boolean isRecentlyActive() {
        return lastSeen != null && 
               lastSeen.isAfter(LocalDateTime.now().minusDays(30));
    }

    public boolean isStaleDevice() {
        return lastSeen != null && 
               lastSeen.isBefore(LocalDateTime.now().minusDays(90));
    }

    public long getDaysSinceLastSeen() {
        if (lastSeen == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(lastSeen, LocalDateTime.now()).toDays();
    }

    public String getLocationString() {
        return location != null ? location : "Unknown";
    }

    public String toAuditString() {
        return String.format("UserDevice{id=%s, userId=%s, deviceName=%s, trusted=%s, location=%s}", 
                           id, userId, deviceName, trusted, location);
    }
}