package com.trademaster.userprofile.service;

import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.repository.ProfileAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileAuditService {
    
    private final ProfileAuditLogRepository auditLogRepository;
    
    /**
     * Log profile creation
     */
    public void logProfileCreation(UserProfile profile, UUID currentUserId) {
        ProfileAuditLog auditLog = createBaseAuditLog(profile, currentUserId, ChangeType.CREATE, EntityType.PROFILE)
            .entityId(profile.getId())
            .newValues(createProfileSnapshot(profile))
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged profile creation for user: {}", profile.getUserId());
    }
    
    /**
     * Log profile update
     */
    public void logProfileUpdate(UserProfile oldProfile, UserProfile newProfile, UUID currentUserId) {
        ProfileAuditLog auditLog = createBaseAuditLog(newProfile, currentUserId, ChangeType.UPDATE, EntityType.PROFILE)
            .entityId(newProfile.getId())
            .oldValues(createProfileSnapshot(oldProfile))
            .newValues(createProfileSnapshot(newProfile))
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged profile update for user: {}", newProfile.getUserId());
    }
    
    /**
     * Log trading preferences update
     */
    public void logTradingPreferencesUpdate(UUID profileId, TradingPreferences oldPreferences, 
                                          TradingPreferences newPreferences, UUID currentUserId) {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);
        
        ProfileAuditLog auditLog = createBaseAuditLog(profile, currentUserId, ChangeType.UPDATE, EntityType.TRADING_PREFERENCES)
            .entityId(profileId)
            .oldValues(oldPreferences)
            .newValues(newPreferences)
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged trading preferences update for profile: {}", profileId);
    }
    
    /**
     * Log KYC information update
     */
    public void logKycUpdate(UUID profileId, KYCInformation oldKyc, KYCInformation newKyc, UUID currentUserId) {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);
        
        ChangeType changeType = determineKycChangeType(oldKyc, newKyc);
        
        ProfileAuditLog auditLog = createBaseAuditLog(profile, currentUserId, changeType, EntityType.KYC)
            .entityId(profileId)
            .oldValues(oldKyc)
            .newValues(newKyc)
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged KYC update for profile: {}, change type: {}", profileId, changeType);
    }
    
    /**
     * Log notification settings update
     */
    public void logNotificationSettingsUpdate(UUID profileId, NotificationSettings oldSettings, 
                                            NotificationSettings newSettings, UUID currentUserId) {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);
        
        ProfileAuditLog auditLog = createBaseAuditLog(profile, currentUserId, ChangeType.UPDATE, EntityType.NOTIFICATION_SETTINGS)
            .entityId(profileId)
            .oldValues(oldSettings)
            .newValues(newSettings)
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged notification settings update for profile: {}", profileId);
    }
    
    /**
     * Log document upload
     */
    public void logDocumentUpload(UserDocument document, UUID currentUserId) {
        ProfileAuditLog auditLog = createBaseAuditLog(document.getUserProfile(), currentUserId, ChangeType.CREATE, EntityType.DOCUMENT)
            .entityId(document.getId())
            .newValues(createDocumentSnapshot(document))
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged document upload for profile: {}, document type: {}", 
                document.getUserProfile().getId(), document.getDocumentType());
    }
    
    /**
     * Log document verification
     */
    public void logDocumentVerification(UserDocument document, VerificationStatus oldStatus, 
                                      VerificationStatus newStatus, UUID verifiedBy) {
        Map<String, Object> oldValues = Map.of(
            "verificationStatus", oldStatus,
            "verifiedAt", document.getVerifiedAt()
        );
        
        Map<String, Object> newValues = Map.of(
            "verificationStatus", newStatus,
            "verifiedAt", Instant.now(),
            "verificationRemarks", document.getVerificationRemarks() != null ? document.getVerificationRemarks() : ""
        );
        
        ProfileAuditLog auditLog = createBaseAuditLog(document.getUserProfile(), verifiedBy, ChangeType.UPDATE, EntityType.DOCUMENT)
            .entityId(document.getId())
            .oldValues(oldValues)
            .newValues(newValues)
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged document verification for profile: {}, document: {}, status: {}", 
                document.getUserProfile().getId(), document.getId(), newStatus);
    }
    
    /**
     * Log profile deletion
     */
    public void logProfileDeletion(UserProfile profile, UUID currentUserId) {
        ProfileAuditLog auditLog = createBaseAuditLog(profile, currentUserId, ChangeType.DELETE, EntityType.PROFILE)
            .entityId(profile.getId())
            .oldValues(createProfileSnapshot(profile))
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged profile deletion for user: {}", profile.getUserId());
    }
    
    /**
     * Log security event (login/logout)
     */
    public void logSecurityEvent(UUID profileId, UUID userId, ChangeType changeType, boolean success, String sessionId) {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);
        
        Map<String, Object> eventDetails = Map.of(
            "success", success,
            "sessionId", sessionId != null ? sessionId : "",
            "timestamp", Instant.now().toString()
        );
        
        ProfileAuditLog auditLog = createBaseAuditLog(profile, userId, changeType, EntityType.PROFILE)
            .newValues(eventDetails)
            .sessionId(sessionId)
            .build();
            
        auditLogRepository.save(auditLog);
        log.info("Logged security event for profile: {}, event: {}, success: {}", profileId, changeType, success);
    }
    
    // Private helper methods
    
    private ProfileAuditLog.ProfileAuditLogBuilder createBaseAuditLog(UserProfile profile, UUID currentUserId, 
                                                                     ChangeType changeType, EntityType entityType) {
        ProfileAuditLog.ProfileAuditLogBuilder builder = ProfileAuditLog.builder()
            .userProfile(profile)
            .changedBy(currentUserId)
            .changeType(changeType)
            .entityType(entityType);
            
        // Add request context information
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Get IP address
                String ipAddress = getClientIpAddress(request);
                if (ipAddress != null) {
                    builder.ipAddress(InetAddress.getByName(ipAddress));
                }
                
                // Get user agent
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 1000) {
                    userAgent = userAgent.substring(0, 1000); // Truncate if too long
                }
                builder.userAgent(userAgent);
                
                // Get session ID
                String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
                builder.sessionId(sessionId);
            }
        } catch (Exception e) {
            log.warn("Failed to get request context for audit log", e);
        }
        
        return builder;
    }
    
    private Map<String, Object> createProfileSnapshot(UserProfile profile) {
        return Map.of(
            "userId", profile.getUserId().toString(),
            "personalInfo", profile.getPersonalInfo(),
            "tradingPreferences", profile.getTradingPreferences(),
            "kycInfo", profile.getKycInfo(),
            "notificationSettings", profile.getNotificationSettings(),
            "version", profile.getVersion(),
            "updatedAt", profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : ""
        );
    }
    
    private Map<String, Object> createDocumentSnapshot(UserDocument document) {
        return Map.of(
            "documentType", document.getDocumentType().toString(),
            "fileName", document.getFileName(),
            "fileSize", document.getFileSize(),
            "mimeType", document.getMimeType(),
            "verificationStatus", document.getVerificationStatus().toString(),
            "uploadedAt", document.getUploadedAt().toString()
        );
    }
    
    private ChangeType determineKycChangeType(KYCInformation oldKyc, KYCInformation newKyc) {
        // If KYC status changed to VERIFIED, log as KYC_VERIFY
        if (oldKyc.kycStatus() != KYCStatus.VERIFIED && newKyc.kycStatus() == KYCStatus.VERIFIED) {
            return ChangeType.KYC_VERIFY;
        }
        
        // If documents were submitted (status changed to PENDING/IN_PROGRESS), log as KYC_SUBMIT
        if ((oldKyc.kycStatus() == KYCStatus.NOT_STARTED || oldKyc.kycStatus() == KYCStatus.IN_PROGRESS) &&
            (newKyc.kycStatus() == KYCStatus.PENDING || newKyc.kycStatus() == KYCStatus.UNDER_REVIEW)) {
            return ChangeType.KYC_SUBMIT;
        }
        
        // Otherwise, it's a regular update
        return ChangeType.UPDATE;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP address in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded;
        }
        
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded;
        }
        
        return request.getRemoteAddr();
    }
}