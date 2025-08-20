package com.trademaster.userprofile.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_audit_logs", indexes = {
    @Index(name = "idx_audit_logs_profile_id", columnList = "user_profile_id"),
    @Index(name = "idx_audit_logs_changed_by", columnList = "changed_by"),
    @Index(name = "idx_audit_logs_change_type", columnList = "change_type"),
    @Index(name = "idx_audit_logs_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "userProfile")
public class ProfileAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonBackReference
    private UserProfile userProfile;
    
    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 50)
    private ChangeType changeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Type(JsonType.class)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Object oldValues;
    
    @Type(JsonType.class)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Object newValues;
    
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Helper methods
    public boolean isCreate() {
        return changeType == ChangeType.CREATE;
    }
    
    public boolean isUpdate() {
        return changeType == ChangeType.UPDATE;
    }
    
    public boolean isDelete() {
        return changeType == ChangeType.DELETE;
    }
    
    public boolean isSecurityEvent() {
        return changeType == ChangeType.LOGIN || changeType == ChangeType.LOGOUT;
    }
    
    public boolean isKYCEvent() {
        return changeType == ChangeType.KYC_SUBMIT || changeType == ChangeType.KYC_VERIFY;
    }
}

enum ChangeType {
    CREATE("Create", "Entity created"),
    UPDATE("Update", "Entity updated"),
    DELETE("Delete", "Entity deleted"),
    LOGIN("Login", "User logged in"),
    LOGOUT("Logout", "User logged out"),
    KYC_SUBMIT("KYC Submit", "KYC documentation submitted"),
    KYC_VERIFY("KYC Verify", "KYC verification completed");
    
    private final String displayName;
    private final String description;
    
    ChangeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

enum EntityType {
    PROFILE("Profile", "User profile information"),
    DOCUMENT("Document", "User uploaded documents"),
    TRADING_PREFERENCES("Trading Preferences", "Trading configuration and preferences"),
    KYC("KYC", "Know Your Customer information"),
    NOTIFICATION_SETTINGS("Notification Settings", "User notification preferences");
    
    private final String displayName;
    private final String description;
    
    EntityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}