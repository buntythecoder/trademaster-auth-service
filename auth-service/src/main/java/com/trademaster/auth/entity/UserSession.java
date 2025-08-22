package com.trademaster.auth.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

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

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

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

    @Type(JsonType.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> attributes = new java.util.HashMap<>();

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
        if (expiresAt == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    public long getMinutesSinceLastActivity() {
        if (lastActivity == null) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
    }

    public void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new java.util.HashMap<>();
        }
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes != null ? this.attributes.get(key) : null;
    }

    public boolean hasAttribute(String key) {
        return this.attributes != null && this.attributes.containsKey(key);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("UserSession{sessionId=%s, userId=%s, active=%s, expiresAt=%s}", 
                           sessionId, userId, active, expiresAt);
    }
}