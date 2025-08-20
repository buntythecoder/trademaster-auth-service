package com.trademaster.notification.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notification_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @NotBlank
    @Column(nullable = false)
    private String recipient;
    
    @Email(message = "Email should be valid when type is EMAIL")
    private String emailRecipient;
    
    private String phoneRecipient;
    
    @NotBlank
    @Column(nullable = false)
    private String subject;
    
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    private String templateName;
    
    @Column(columnDefinition = "TEXT")
    private String templateVariables; // JSON string of variables
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts = 3;
    
    @Column(name = "reference_id")
    private String referenceId; // External reference (e.g., trade ID, user ID)
    
    @Column(name = "reference_type")
    private String referenceType; // Type of reference (TRADE, USER, KYC, etc.)
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (scheduledAt == null) {
            scheduledAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean canRetry() {
        return retryCount < maxRetryAttempts && 
               (status == NotificationStatus.FAILED || status == NotificationStatus.PENDING);
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    public enum NotificationType {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
    
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
    
    public enum NotificationStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        CANCELLED
    }
}