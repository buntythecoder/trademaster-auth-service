package com.trademaster.userprofile.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profiles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_profiles_created_at", columnList = "created_at"),
    @Index(name = "idx_user_profiles_updated_at", columnList = "updated_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"documents", "auditLogs"})
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Type(JsonType.class)
    @Column(name = "personal_info", columnDefinition = "jsonb not null")
    private PersonalInformation personalInfo;
    
    @Type(JsonType.class)
    @Column(name = "trading_preferences", columnDefinition = "jsonb not null")
    private TradingPreferences tradingPreferences;
    
    @Type(JsonType.class)
    @Column(name = "kyc_information", columnDefinition = "jsonb not null")
    private KYCInformation kycInfo;
    
    @Type(JsonType.class)
    @Column(name = "notification_settings", columnDefinition = "jsonb not null")
    private NotificationSettings notificationSettings;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<UserDocument> documents = new ArrayList<>();
    
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<ProfileAuditLog> auditLogs = new ArrayList<>();
    
    @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;
    
    // Helper methods
    public void addDocument(UserDocument document) {
        documents.add(document);
        document.setUserProfile(this);
    }
    
    public void removeDocument(UserDocument document) {
        documents.remove(document);
        document.setUserProfile(null);
    }
    
    public void addAuditLog(ProfileAuditLog auditLog) {
        auditLogs.add(auditLog);
        auditLog.setUserProfile(this);
    }
}