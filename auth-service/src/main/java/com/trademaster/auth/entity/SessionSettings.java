package com.trademaster.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SessionSettings {

    @Id
    @Column(name = "user_id", length = 50)
    @EqualsAndHashCode.Include
    private String userId;

    @Column(name = "max_concurrent_sessions")
    @Builder.Default
    private Integer maxConcurrentSessions = 3;

    @Column(name = "session_timeout_minutes")
    @Builder.Default
    private Integer sessionTimeoutMinutes = 30;

    @Column(name = "extend_on_activity")
    @Builder.Default
    private Boolean extendOnActivity = true;

    @Column(name = "require_mfa_on_new_device")
    @Builder.Default
    private Boolean requireMfaOnNewDevice = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic methods
    public boolean shouldExtendOnActivity() {
        return Boolean.TRUE.equals(this.extendOnActivity);
    }

    public boolean shouldRequireMfaOnNewDevice() {
        return Boolean.TRUE.equals(this.requireMfaOnNewDevice);
    }

    public boolean isWithinConcurrentSessionLimit(int currentSessionCount) {
        return currentSessionCount < this.maxConcurrentSessions;
    }

    public LocalDateTime calculateSessionExpiry() {
        return LocalDateTime.now().plusMinutes(this.sessionTimeoutMinutes);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("SessionSettings{userId=%s, maxSessions=%d, timeoutMinutes=%d, extendOnActivity=%s}", 
                           userId, maxConcurrentSessions, sessionTimeoutMinutes, extendOnActivity);
    }

    // Static factory method for default settings
    public static SessionSettings createDefault(String userId) {
        return SessionSettings.builder()
                .userId(userId)
                .maxConcurrentSessions(3)
                .sessionTimeoutMinutes(30)
                .extendOnActivity(true)
                .requireMfaOnNewDevice(false)
                .build();
    }
}