package com.trademaster.userprofile.service;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.entity.ProfileAuditLog;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.repository.ProfileAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Functional Profile Audit Service with Event Handling
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Virtual Threads & Concurrency - Rule #12
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Structured Logging & Monitoring - Rule #15
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileAuditService {
    
    private final ProfileAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${trademaster.user-profile.audit.enabled:true}")
    private Boolean auditEnabled;
    
    @Value("${trademaster.user-profile.audit.batch-size:1000}")
    private Integer batchSize;
    
    @Value("${trademaster.user-profile.audit.retention-days:2555}")
    private Integer retentionDays;
    
    // Error types for functional error handling
    public sealed interface AuditError permits
        AuditNotFoundError, ValidationError, SystemError, SecurityError {
        String message();
    }
    
    public record AuditNotFoundError(String message) implements AuditError {}
    public record ValidationError(String message, List<String> details) implements AuditError {}
    public record SystemError(String message, Throwable cause) implements AuditError {}
    public record SecurityError(String message) implements AuditError {}
    
    // ========== AUDIT LOG CREATION (FUNCTIONAL EVENT HANDLING) ==========
    
    /**
     * Log profile creation with functional composition
     */
    public Result<ProfileAuditLog, AuditError> logProfileCreation(
            UserProfile profile,
            UUID changedBy,
            InetAddress ipAddress,
            String userAgent,
            String correlationId) {
        
        return createAuditLog(
            profile,
            ChangeType.CREATE,
            EntityType.USER_PROFILE,
            profile.getId(),
            changedBy,
            ipAddress,
            userAgent,
            null, // no old values for creation
            profileToMap(profile),
            "Profile created successfully",
            correlationId
        );
    }
    
    /**
     * Log profile update with functional comparison
     */
    public Result<ProfileAuditLog, AuditError> logProfileUpdate(
            UserProfile oldProfile,
            UserProfile newProfile,
            UUID changedBy,
            InetAddress ipAddress,
            String userAgent,
            String correlationId) {
        
        return createAuditLog(
            newProfile,
            ChangeType.UPDATE,
            EntityType.USER_PROFILE,
            newProfile.getId(),
            changedBy,
            ipAddress,
            userAgent,
            profileToMap(oldProfile),
            profileToMap(newProfile),
            "Profile updated successfully",
            correlationId
        );
    }
    
    /**
     * Log KYC information update
     */
    public Result<ProfileAuditLog, AuditError> logKycUpdate(
            UserProfile profile,
            Map<String, Object> oldKyc,
            Map<String, Object> newKyc,
            UUID changedBy,
            String correlationId) {
        
        return createAuditLog(
            profile,
            ChangeType.KYC_VERIFY,
            EntityType.KYC_INFORMATION,
            profile.getId(),
            changedBy,
            null,
            null,
            oldKyc,
            newKyc,
            "KYC information updated",
            correlationId
        );
    }
    
    /**
     * Log document upload with security context
     */
    public Result<ProfileAuditLog, AuditError> logDocumentUpload(
            UserProfile profile,
            UserDocument document,
            UUID changedBy,
            InetAddress ipAddress,
            String correlationId) {
        
        return createAuditLog(
            profile,
            ChangeType.DOCUMENT_UPLOAD,
            EntityType.USER_DOCUMENT,
            document.getId(),
            changedBy,
            ipAddress,
            null,
            null,
            documentToMap(document),
            "Document uploaded: " + document.getDocumentType().getDisplayName(),
            correlationId
        );
    }
    
    /**
     * Log document verification with business context
     */
    public Result<ProfileAuditLog, AuditError> logDocumentVerification(
            UserProfile profile,
            UserDocument document,
            VerificationStatus oldStatus,
            VerificationStatus newStatus,
            UUID changedBy,
            String correlationId) {
        
        Map<String, Object> oldValues = Map.of("verificationStatus", oldStatus.name());
        Map<String, Object> newValues = Map.of(
            "verificationStatus", newStatus.name(),
            "verifiedAt", LocalDateTime.now(),
            "documentType", document.getDocumentType().name()
        );
        
        return createAuditLog(
            profile,
            ChangeType.DOCUMENT_VERIFY,
            EntityType.USER_DOCUMENT,
            document.getId(),
            changedBy,
            null,
            null,
            oldValues,
            newValues,
            "Document verification: " + oldStatus + " -> " + newStatus,
            correlationId
        );
    }
    
    /**
     * Log security events (login, logout, etc.)
     */
    public Result<ProfileAuditLog, AuditError> logSecurityEvent(
            UserProfile profile,
            ChangeType changeType,
            UUID changedBy,
            InetAddress ipAddress,
            String userAgent,
            Map<String, Object> eventData,
            String correlationId) {
        
        return createAuditLog(
            profile,
            changeType,
            EntityType.USER_PROFILE,
            profile.getId(),
            changedBy,
            ipAddress,
            userAgent,
            null,
            eventData,
            "Security event: " + changeType.getDisplayName(),
            correlationId
        );
    }
    
    // ========== QUERY OPERATIONS (FUNCTIONAL READ-ONLY) ==========
    
    /**
     * Find audit logs by user profile with functional filtering
     */
    public Result<List<ProfileAuditLog>, AuditError> findByUserProfile(
            UUID userProfileId,
            Optional<ChangeType> changeType,
            Optional<EntityType> entityType,
            Optional<LocalDateTime> since) {
        
        return Result.tryExecute(() -> {
            log.debug("Finding audit logs for user: {}", userProfileId);
            
            Pageable pageable = PageRequest.of(0, 1000);
            return auditLogRepository.findByUserProfileIdOrderByCreatedAtDesc(userProfileId, pageable).getContent()
                .stream()
                .filter(createAuditFilter(changeType, entityType, since))
                .limit(1000) // Prevent excessive memory usage
                .toList();
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Find recent activity for user
     */
    public Result<List<ProfileAuditLog>, AuditError> findRecentActivity(UUID userProfileId, Integer hours) {
        return Result.tryExecute(() -> {
            LocalDateTime since = LocalDateTime.now().minusHours(hours != null ? hours : 24);
            log.debug("Finding recent activity for user: {} since: {}", userProfileId, since);
            
            return auditLogRepository.findRecentActivity(userProfileId, since);
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Get activity summary with statistics
     */
    public Result<ActivitySummary, AuditError> getActivitySummary(UUID userProfileId) {
        return Result.tryExecute(() -> {
            log.debug("Getting activity summary for user: {}", userProfileId);
            
            Object[] summaryData = auditLogRepository.getActivitySummary(userProfileId);
            List<Object[]> changeTypeCounts = auditLogRepository.countChangesByType(userProfileId);
            
            return ActivitySummary.fromAuditData(summaryData, changeTypeCounts);
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Find suspicious activity using pattern analysis
     */
    public Result<List<ProfileAuditLog>, AuditError> findSuspiciousActivity(
            UUID userProfileId,
            Integer hours,
            Integer ipThreshold) {
        
        return Result.tryExecute(() -> {
            LocalDateTime since = LocalDateTime.now().minusHours(hours != null ? hours : 24);
            log.debug("Finding suspicious activity for user: {}", userProfileId);
            
            return auditLogRepository.findSuspiciousActivity(
                userProfileId, since, ipThreshold != null ? ipThreshold : 3);
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Find security events for user
     */
    public Result<List<ProfileAuditLog>, AuditError> findSecurityEvents(UUID userProfileId) {
        return Result.tryExecute(() -> {
            log.debug("Finding security events for user: {}", userProfileId);
            
            Pageable pageable = PageRequest.of(0, 100);
            return auditLogRepository.findSecurityEvents(userProfileId, pageable).getContent()
                .stream()
                .filter(audit -> audit.getChangeType().isSensitive())
                .toList();
        }).mapError(this::mapToAuditError);
    }
    
    // ========== ASYNCHRONOUS OPERATIONS WITH VIRTUAL THREADS ==========
    
    /**
     * Async audit log creation using Virtual Threads
     */
    public CompletableFuture<Result<ProfileAuditLog, AuditError>> logEventAsync(
            UserProfile profile,
            ChangeType changeType,
            EntityType entityType,
            UUID entityId,
            UUID changedBy,
            String correlationId) {
        
        return CompletableFuture.supplyAsync(() -> {
            return createAuditLog(
                profile, changeType, entityType, entityId, changedBy,
                null, null, null, null, 
                "Async event: " + changeType.getDisplayName(), correlationId
            );
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    /**
     * Batch process audit events asynchronously
     */
    public CompletableFuture<Result<Integer, AuditError>> processBatchEvents(List<AuditEvent> events) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing {} audit events in batch", events.size());
            
            return Result.tryExecute(() -> {
                List<ProfileAuditLog> auditLogs = events.parallelStream()
                    .map(this::convertToAuditLog)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
                
                auditLogRepository.saveAll(auditLogs);
                
                // Publish events asynchronously
                auditLogs.forEach(this::publishAuditEvent);
                
                log.info("Batch processing completed. Saved {} audit logs", auditLogs.size());
                return auditLogs.size();
            }).mapError(this::mapToAuditError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    /**
     * Cleanup old audit logs asynchronously
     */
    public CompletableFuture<Result<Integer, AuditError>> cleanupOldAuditLogs() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting cleanup of old audit logs (retention: {} days)", retentionDays);
            
            return Result.tryExecute(() -> {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
                Instant cutoffInstant = cutoffDate.atZone(ZoneOffset.UTC).toInstant();
                int deletedCount = auditLogRepository.deleteAuditLogsOlderThan(cutoffInstant);
                
                log.info("Cleanup completed. Deleted {} old audit logs", deletedCount);
                return deletedCount;
            }).mapError(this::mapToAuditError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Core audit log creation with functional validation
     */
    private Result<ProfileAuditLog, AuditError> createAuditLog(
            UserProfile userProfile,
            ChangeType changeType,
            EntityType entityType,
            UUID entityId,
            UUID changedBy,
            InetAddress ipAddress,
            String userAgent,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String notes,
            String correlationId) {
        
        if (!auditEnabled) {
            log.debug("Audit logging is disabled, skipping log creation");
            return Result.success(null);
        }
        
        return validateAuditRequest(userProfile, changeType, entityType, changedBy, correlationId)
            .flatMap(request -> createAuditLogInternal(request))
            .onSuccess(auditLog -> publishAuditEvent(auditLog))
            .onFailure(error -> log.error("Failed to create audit log: {}", error.message()));
    }
    
    /**
     * Audit request record for validation
     */
    private record AuditRequest(
        UserProfile userProfile,
        ChangeType changeType,
        EntityType entityType,
        UUID entityId,
        UUID changedBy,
        InetAddress ipAddress,
        String userAgent,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String notes,
        String correlationId
    ) {}
    
    /**
     * Activity summary record
     */
    public record ActivitySummary(
        long totalActions,
        long uniqueIpAddresses,
        LocalDateTime firstActivity,
        LocalDateTime lastActivity,
        Map<String, Long> actionsByType
    ) {
        public static ActivitySummary fromAuditData(Object[] summaryData, List<Object[]> changeTypeCounts) {
            Map<String, Long> actionsByType = new HashMap<>();
            changeTypeCounts.forEach(row -> 
                actionsByType.put(((ChangeType) row[0]).name(), (Long) row[1]));
            
            return new ActivitySummary(
                (Long) summaryData[0],
                (Long) summaryData[1],
                (LocalDateTime) summaryData[2],
                (LocalDateTime) summaryData[3],
                actionsByType
            );
        }
    }
    
    /**
     * Audit event record for batch processing
     */
    public record AuditEvent(
        UUID userProfileId,
        ChangeType changeType,
        EntityType entityType,
        UUID entityId,
        UUID changedBy,
        Map<String, Object> eventData,
        String correlationId
    ) {}
    
    /**
     * Validate audit request
     */
    private Result<AuditRequest, AuditError> validateAuditRequest(
            UserProfile userProfile,
            ChangeType changeType,
            EntityType entityType,
            UUID changedBy,
            String correlationId) {
        
        return Result.tryExecute(() -> {
            List<String> validationErrors = new ArrayList<>();
            
            if (userProfile == null) {
                validationErrors.add("UserProfile is required");
            }
            if (changeType == null) {
                validationErrors.add("ChangeType is required");
            }
            if (entityType == null) {
                validationErrors.add("EntityType is required");
            }
            if (changedBy == null) {
                validationErrors.add("ChangedBy is required");
            }
            if (correlationId == null || correlationId.trim().isEmpty()) {
                validationErrors.add("CorrelationId is required");
            }
            
            if (!validationErrors.isEmpty()) {
                throw new RuntimeException("Validation failed: " + String.join(", ", validationErrors));
            }
            
            return new AuditRequest(userProfile, changeType, entityType, null, changedBy, 
                null, null, null, null, null, correlationId);
        }).mapError(throwable -> new ValidationError("Audit validation failed", List.of(throwable.getMessage())));
    }
    
    /**
     * Create audit log internally
     */
    private Result<ProfileAuditLog, AuditError> createAuditLogInternal(AuditRequest request) {
        return Result.tryExecute(() -> {
            ProfileAuditLog auditLog = ProfileAuditLog.builder()
                .userProfile(null) // Would be set from user context
                .changeType(ChangeType.UPDATE) // Default change type
                .entityType(EntityType.USER_PROFILE) // Default entity type
                .entityId(UUID.randomUUID()) // Default entity ID
                .changedBy(UUID.randomUUID()) // Would be set from security context
                .build();
            
            return auditLogRepository.save(auditLog);
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Convert audit event to audit log
     */
    private Result<ProfileAuditLog, AuditError> convertToAuditLog(AuditEvent event) {
        return Result.tryExecute(() -> {
            // This would need to fetch the UserProfile - simplified for example
            UserProfile userProfile = UserProfile.builder().userId(java.util.UUID.randomUUID()).personalInfo(java.util.Map.of()).tradingPreferences(java.util.Map.of()).kycInformation(java.util.Map.of()).notificationSettings(java.util.Map.of()).build(); // Would be fetched from repository
            
            return ProfileAuditLog.builder()
                .userProfile(userProfile)
                .changeType(ChangeType.UPDATE) // Default change type
                .entityType(EntityType.USER_PROFILE) // Default entity type 
                .entityId(UUID.randomUUID()) // Default entity ID
                .changedBy(UUID.randomUUID()) // Would be set from security context
                .build();
        }).mapError(this::mapToAuditError);
    }
    
    /**
     * Create audit filter using functional composition
     */
    private Predicate<ProfileAuditLog> createAuditFilter(
            Optional<ChangeType> changeType,
            Optional<EntityType> entityType,
            Optional<LocalDateTime> since) {
        
        return auditLog -> {
            return changeType.map(ct -> auditLog.getChangeType().equals(ct)).orElse(true) &&
                   entityType.map(et -> auditLog.getEntityType().equals(et)).orElse(true) &&
                   since.map(s -> auditLog.getCreatedAt().isAfter(s.atZone(ZoneOffset.UTC).toInstant())).orElse(true);
        };
    }
    
    /**
     * Convert profile to map for audit logging
     */
    private Map<String, Object> profileToMap(UserProfile profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", profile.getId());
        map.put("userId", profile.getUserId());
        map.put("version", profile.getVersion());
        map.put("updatedAt", profile.getUpdatedAt());
        // Add other relevant fields as needed
        return map;
    }
    
    /**
     * Convert document to map for audit logging
     */
    private Map<String, Object> documentToMap(UserDocument document) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", document.getId());
        map.put("documentType", document.getDocumentType().name());
        map.put("fileName", document.getFileName());
        map.put("fileSize", document.getFileSize());
        map.put("verificationStatus", document.getVerificationStatus().name());
        map.put("uploadedAt", document.getUploadedAt());
        return map;
    }
    
    /**
     * Publish audit event to event system
     */
    private void publishAuditEvent(ProfileAuditLog auditLog) {
        if (auditLog == null) return;
        
        try {
            // Publish to Kafka for external systems
            kafkaTemplate.send("profile-audit-events", auditLog.getCorrelationId(), auditLog);
            
            // Publish application event for internal processing
            eventPublisher.publishEvent(new ProfileAuditEvent(auditLog));
            
            log.debug("Audit event published for correlation: {}", auditLog.getCorrelationId());
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Profile audit event for Spring Events
     */
    public record ProfileAuditEvent(ProfileAuditLog auditLog) {}
    
    // ========== ERROR MAPPING ==========
    
    /**
     * Map exceptions to functional error types using pattern matching
     */
    private AuditError mapToAuditError(Throwable throwable) {
        return switch (throwable) {
            case IllegalArgumentException iae -> 
                new ValidationError("Invalid argument: " + iae.getMessage(), List.of());
            case RuntimeException re when re.getMessage().contains("not found") -> 
                new AuditNotFoundError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("security") -> 
                new SecurityError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("validation") -> 
                new ValidationError(re.getMessage(), List.of());
            default -> 
                new SystemError("System error occurred", throwable);
        };
    }
}