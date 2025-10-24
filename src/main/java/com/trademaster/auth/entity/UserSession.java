package com.trademaster.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserSession {

    @Id
    @Column(name = "session_id", length = 255)
    @EqualsAndHashCode.Include
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "location", length = 255)
    private String location;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity")
    @Builder.Default
    private LocalDateTime lastActivity = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "attributes", columnDefinition = "text")
    @Builder.Default
    private String attributes = "";

    // Business logic methods
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active) && !isExpired();
    }

    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public void extendSession(int minutes) {
        this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
        updateLastActivity();
    }

    public void terminate() {
        this.active = false;
    }

    public void reactivate(int sessionTimeoutMinutes) {
        this.active = true;
        this.expiresAt = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes);
        updateLastActivity();
    }

    public long getMinutesUntilExpiry() {
        return Optional.ofNullable(expiresAt)
            .map(expiry -> java.time.Duration.between(LocalDateTime.now(), expiry).toMinutes())
            .orElse(0L);
    }

    public long getMinutesSinceLastActivity() {
        return Optional.ofNullable(lastActivity)
            .map(activity -> java.time.Duration.between(activity, LocalDateTime.now()).toMinutes())
            .orElse(Long.MAX_VALUE);
    }

    public void setAttribute(String key, Object value) {
        // Simple key-value storage in string format
        if (this.attributes == null || this.attributes.isEmpty()) {
            this.attributes = key + "=" + value.toString();
        } else {
            this.attributes = this.attributes + ";" + key + "=" + value.toString();
        }
    }

    public Object getAttribute(String key) {
        if (this.attributes == null || this.attributes.isEmpty()) {
            return null;
        }
        String[] pairs = this.attributes.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals(key)) {
                return keyValue[1];
            }
        }
        return null;
    }

    public boolean hasAttribute(String key) {
        return getAttribute(key) != null;
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserSession{sessionId=%s, userId=%s, active=%s, expiresAt=%s}", 
                           sessionId, userId, active, expiresAt);
    }
}