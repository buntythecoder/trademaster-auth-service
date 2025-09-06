package com.trademaster.userprofile.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_documents", indexes = {
    @Index(name = "idx_user_documents_profile_id", columnList = "user_profile_id"),
    @Index(name = "idx_user_documents_type", columnList = "document_type"),
    @Index(name = "idx_user_documents_status", columnList = "verification_status"),
    @Index(name = "idx_user_documents_uploaded_at", columnList = "uploaded_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "userProfile")
public class UserDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonBackReference
    private UserProfile userProfile;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    
    @Column(name = "verification_remarks")
    private String verificationRemarks;
    
    @CreatedDate
    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    // Helper methods
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }
    
    public boolean isPending() {
        return verificationStatus == VerificationStatus.PENDING;
    }
    
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }
    
    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }
    
    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
    
    // Helper methods for controller compatibility
    public UUID getUserId() {
        return userProfile != null ? userProfile.getUserId() : null;
    }
    
    public String getContentType() {
        return mimeType;
    }
    
    public String getStoragePath() {
        return filePath;
    }
    
    public String getDescription() {
        return verificationRemarks;
    }
    
    public String getVerifierNotes() {
        return verificationRemarks;
    }
    
    public java.time.LocalDateTime getUploadTimestamp() {
        return uploadedAt != null ? java.time.LocalDateTime.ofInstant(uploadedAt, java.time.ZoneOffset.UTC) : null;
    }
    
    public java.time.LocalDateTime getVerificationTimestamp() {
        return verifiedAt != null ? java.time.LocalDateTime.ofInstant(verifiedAt, java.time.ZoneOffset.UTC) : null;
    }
    
    public Integer getVersion() {
        return 1; // Default version
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return getUploadTimestamp();
    }
    
    public java.time.LocalDateTime getUpdatedAt() {
        return getVerificationTimestamp() != null ? getVerificationTimestamp() : getUploadTimestamp();
    }
    
    // Helper method for verification status update (service compatibility)
    public UserDocument withVerificationStatus(VerificationStatus status, String remarks) {
        this.verificationStatus = status;
        this.verificationRemarks = remarks;
        if (status == VerificationStatus.VERIFIED || status == VerificationStatus.REJECTED) {
            this.verifiedAt = java.time.Instant.now();
        }
        return this;
    }
    
    // Builder pattern support for service compatibility
    public UserDocumentBuilder toBuilder() {
        return UserDocument.builder()
            .id(this.id)
            .userProfile(this.userProfile)
            .documentType(this.documentType)
            .fileName(this.fileName)
            .filePath(this.filePath)
            .fileSize(this.fileSize)
            .mimeType(this.mimeType)
            .verificationStatus(this.verificationStatus)
            .verificationRemarks(this.verificationRemarks)
            .uploadedAt(this.uploadedAt)
            .verifiedAt(this.verifiedAt);
    }
}