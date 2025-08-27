package com.trademaster.auth.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication Audit Log entity for comprehensive event tracking
 * 
 * Features blockchain-style integrity verification for financial compliance.
 * Stores all authentication, authorization, and security events.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "auth_audit_log", indexes = {
    @Index(name = "idx_auth_audit_log_user_id", columnList = "userId"),
    @Index(name = "idx_auth_audit_log_event_type", columnList = "eventType"),
    @Index(name = "idx_auth_audit_log_event_status", columnList = "eventStatus"),
    @Index(name = "idx_auth_audit_log_created_at", columnList = "createdAt"),
    @Index(name = "idx_auth_audit_log_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_auth_audit_log_risk_score", columnList = "riskScore"),
    @Index(name = "idx_auth_audit_log_correlation_id", columnList = "correlationId"),
    @Index(name = "idx_auth_audit_log_session_id", columnList = "sessionId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 20)
    @Builder.Default
    private EventStatus eventStatus = EventStatus.SUCCESS;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_fingerprint", length = 512)
    private String deviceFingerprint;

    @Type(JsonType.class)
    @Column(name = "location", columnDefinition = "jsonb")
    private Map<String, Object> location;

    @Type(JsonType.class)
    @Column(name = "details", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> details = Map.of();

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "correlation_id")
    @Builder.Default
    private UUID correlationId = UUID.randomUUID();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Blockchain integrity fields
    @Column(name = "blockchain_hash", length = 64)
    private String blockchainHash;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "signature", length = 512)
    private String signature;

    // Event Type enum
    @Getter
    public enum EventType {
        // Authentication events
        LOGIN_SUCCESS("login_success"),
        LOGIN_FAILED("login_failed"),
        LOGOUT("logout"),
        REGISTRATION("registration"),
        
        // Password events
        PASSWORD_CHANGE("password_change"),
        PASSWORD_RESET("password_reset"),
        
        // MFA events
        MFA_ENABLED("mfa_enabled"),
        MFA_DISABLED("mfa_disabled"),
        MFA_SUCCESS("mfa_success"),
        MFA_FAILED("mfa_failed"),
        
        // Account events
        ACCOUNT_LOCKED("account_locked"),
        ACCOUNT_UNLOCKED("account_unlocked"),
        ACCOUNT_SUSPENDED("account_suspended"),
        ACCOUNT_REACTIVATED("account_reactivated"),
        
        // Verification events
        EMAIL_VERIFIED("email_verified"),
        PHONE_VERIFIED("phone_verified"),
        
        // Device events
        DEVICE_REGISTERED("device_registered"),
        DEVICE_REMOVED("device_removed"),
        
        // Security events
        SUSPICIOUS_ACTIVITY("suspicious_activity"),
        SECURITY_VIOLATION("security_violation"),
        
        // Session events
        SESSION_CREATED("session_created"),
        SESSION_EXPIRED("session_expired"),
        
        // Token events
        TOKEN_ISSUED("token_issued"),
        TOKEN_REFRESHED("token_refreshed"),
        TOKEN_REVOKED("token_revoked");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

    }

    // Event Status enum
    @Getter
    public enum EventStatus {
        SUCCESS("success"),
        FAILED("failed"),
        PENDING("pending"),
        BLOCKED("blocked");

        private final String value;

        EventStatus(String value) {
            this.value = value;
        }

    }

    // Business logic methods
    public boolean isHighRisk() {
        return riskScore != null && riskScore >= 75;
    }

    public boolean isCriticalEvent() {
        return eventType == EventType.SECURITY_VIOLATION ||
               eventType == EventType.SUSPICIOUS_ACTIVITY ||
               eventType == EventType.ACCOUNT_LOCKED ||
               (eventType == EventType.LOGIN_FAILED && isHighRisk());
    }

    public boolean isAuthenticationEvent() {
        return eventType == EventType.LOGIN_SUCCESS ||
               eventType == EventType.LOGIN_FAILED ||
               eventType == EventType.LOGOUT ||
               eventType == EventType.REGISTRATION;
    }

    public boolean isMfaEvent() {
        return eventType == EventType.MFA_ENABLED ||
               eventType == EventType.MFA_DISABLED ||
               eventType == EventType.MFA_SUCCESS ||
               eventType == EventType.MFA_FAILED;
    }

    public boolean isSessionEvent() {
        return eventType == EventType.SESSION_CREATED ||
               eventType == EventType.SESSION_EXPIRED;
    }

    public boolean isTokenEvent() {
        return eventType == EventType.TOKEN_ISSUED ||
               eventType == EventType.TOKEN_REFRESHED ||
               eventType == EventType.TOKEN_REVOKED;
    }

    public void markProcessed() {
        this.processedAt = LocalDateTime.now();
    }

    // Helper method for compliance reporting
    public String toComplianceString() {
        return String.format("AuditLog{id=%d, userId=%d, eventType=%s, eventStatus=%s, timestamp=%s, ip=%s, riskScore=%d}", 
                           id, userId, eventType, eventStatus, createdAt, ipAddress, riskScore);
    }
}