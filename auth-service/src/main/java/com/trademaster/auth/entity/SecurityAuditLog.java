package com.trademaster.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "security_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SecurityAuditLog {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "location", length = 255)
    private String location;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Type(JsonType.class)
    @Column(name = "event_details", columnDefinition = "jsonb")
    private Map<String, Object> eventDetails;

    @Column(name = "risk_level", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Helper methods for event types
    public boolean isLoginEvent() {
        return "LOGIN".equals(eventType) || "LOGIN_SUCCESS".equals(eventType) || "LOGIN_FAILED".equals(eventType);
    }

    public boolean isLogoutEvent() {
        return "LOGOUT".equals(eventType);
    }

    public boolean isMfaEvent() {
        return eventType != null && eventType.startsWith("MFA_");
    }

    public boolean isDeviceEvent() {
        return eventType != null && eventType.startsWith("DEVICE_");
    }

    public boolean isSecurityEvent() {
        return eventType != null && (eventType.startsWith("SECURITY_") || 
               eventType.contains("SUSPICIOUS") || 
               eventType.contains("BREACH"));
    }

    // Helper methods for risk assessment
    public boolean isHighRisk() {
        return RiskLevel.HIGH.equals(riskLevel) || RiskLevel.CRITICAL.equals(riskLevel);
    }

    public boolean isCriticalRisk() {
        return RiskLevel.CRITICAL.equals(riskLevel);
    }

    // Helper method for audit logging
    public String toAuditString() {
        return String.format("SecurityAuditLog{id=%s, userId=%s, eventType=%s, riskLevel=%s, timestamp=%s}", 
                           id, userId, eventType, riskLevel, timestamp);
    }

    // Static factory methods for common events
    public static SecurityAuditLog loginSuccess(String userId, String sessionId, InetAddress ipAddress, String userAgent) {
        return SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("LOGIN_SUCCESS")
                .description("User successfully logged in")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .riskLevel(RiskLevel.LOW)
                .build();
    }

    public static SecurityAuditLog loginFailed(String userId, InetAddress ipAddress, String userAgent, String reason) {
        return SecurityAuditLog.builder()
                .userId(userId)
                .eventType("LOGIN_FAILED")
                .description("Login attempt failed: " + reason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .riskLevel(RiskLevel.MEDIUM)
                .build();
    }

    public static SecurityAuditLog mfaEnabled(String userId, String sessionId, MfaConfiguration.MfaType mfaType) {
        return SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("MFA_ENABLED")
                .description("MFA enabled for type: " + mfaType)
                .riskLevel(RiskLevel.LOW)
                .build();
    }

    public static SecurityAuditLog deviceTrusted(String userId, String sessionId, String deviceFingerprint) {
        return SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("DEVICE_TRUSTED")
                .description("Device marked as trusted: " + deviceFingerprint)
                .riskLevel(RiskLevel.LOW)
                .build();
    }

    public static SecurityAuditLog suspiciousActivity(String userId, String sessionId, String description, InetAddress ipAddress) {
        return SecurityAuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType("SECURITY_SUSPICIOUS_ACTIVITY")
                .description(description)
                .ipAddress(ipAddress)
                .riskLevel(RiskLevel.HIGH)
                .build();
    }
}