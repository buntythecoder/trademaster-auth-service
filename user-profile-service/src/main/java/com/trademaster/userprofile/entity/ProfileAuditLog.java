package com.trademaster.userprofile.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.trademaster.userprofile.entity.ChangeType;
import com.trademaster.userprofile.entity.EntityType;
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
    
    @Column(name = "correlation_id")
    private String correlationId;
    
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
        return changeType == ChangeType.LOGIN || changeType == ChangeType.LOGOUT || 
               changeType == ChangeType.PROFILE_ACTIVATE || changeType == ChangeType.PROFILE_DEACTIVATE;
    }
    
    public boolean isKYCEvent() {
        return changeType == ChangeType.KYC_SUBMIT || changeType == ChangeType.KYC_VERIFY || 
               changeType == ChangeType.DOCUMENT_VERIFY;
    }
}