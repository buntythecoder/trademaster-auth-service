package com.trademaster.userprofile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.userprofile.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    
    private UUID id;
    private UUID userId;
    private Map<String, Object> personalInfo;
    private Map<String, Object> tradingPreferences;
    private Map<String, Object> kycInfo;
    private Map<String, Object> notificationSettings;
    private Integer version;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime updatedAt;
    
    private List<UserDocumentResponse> documents;
    private ProfileCompletionStatus completionStatus;
    
    /**
     * Convert entity to response DTO
     */
    public static UserProfileResponse fromEntity(UserProfile profile) {
        return UserProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .personalInfo(profile.getPersonalInfo())
            .tradingPreferences(profile.getTradingPreferences())
            .kycInfo(profile.getKycInformation())
            .notificationSettings(profile.getNotificationSettings())
            .version(profile.getVersion())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .documents(profile.getDocuments() != null ? 
                profile.getDocuments().stream()
                    .map(UserDocumentResponse::fromEntity)
                    .toList() : null)
            .completionStatus(calculateCompletionStatus(profile))
            .build();
    }
    
    /**
     * Convert entity to summary response (without sensitive data)
     */
    public static UserProfileResponse summaryFromEntity(UserProfile profile) {
        // Create masked personal info for summary view
        Map<String, Object> maskedPersonalInfo = new java.util.HashMap<>(profile.getPersonalInfo());
        
        // Mask sensitive fields
        if (maskedPersonalInfo.containsKey("emailAddress")) {
            maskedPersonalInfo.put("emailAddress", maskEmail((String) maskedPersonalInfo.get("emailAddress")));
        }
        if (maskedPersonalInfo.containsKey("mobileNumber")) {
            maskedPersonalInfo.put("mobileNumber", maskMobile((String) maskedPersonalInfo.get("mobileNumber")));
        }
        if (maskedPersonalInfo.containsKey("panNumber")) {
            maskedPersonalInfo.put("panNumber", maskPan((String) maskedPersonalInfo.get("panNumber")));
        }
        // Remove sensitive fields like full address, Aadhaar, etc.
        maskedPersonalInfo.remove("address");
        maskedPersonalInfo.remove("aadhaarNumber");
        maskedPersonalInfo.remove("dateOfBirth");
            
        return UserProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .personalInfo(maskedPersonalInfo)
            .kycInfo(profile.getKycInformation()) // KYC status is okay to show
            .completionStatus(calculateCompletionStatus(profile))
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }
    
    private static ProfileCompletionStatus calculateCompletionStatus(UserProfile profile) {
        int totalSteps = 4; // Personal Info, Trading Prefs, KYC, Notifications
        int completedSteps = 0;
        
        if (isPersonalInfoComplete(profile.getPersonalInfo())) completedSteps++;
        if (isTradingPreferencesComplete(profile.getTradingPreferences())) completedSteps++;
        if (isKycComplete(profile.getKycInformation())) completedSteps++;
        if (isNotificationSettingsComplete(profile.getNotificationSettings())) completedSteps++;
        
        int kycCompletionPercentage = getKycCompletionPercentage(profile.getKycInformation());
        
        return ProfileCompletionStatus.builder()
            .overallPercentage((completedSteps * 100) / totalSteps)
            .personalInfoComplete(isPersonalInfoComplete(profile.getPersonalInfo()))
            .tradingPreferencesComplete(isTradingPreferencesComplete(profile.getTradingPreferences()))
            .kycComplete(isKycComplete(profile.getKycInformation()))
            .kycCompletionPercentage(kycCompletionPercentage)
            .notificationSettingsComplete(isNotificationSettingsComplete(profile.getNotificationSettings()))
            .canTrade(canTrade(profile.getKycInformation()))
            .nextSteps(getNextSteps(profile))
            .build();
    }
    
    private static boolean isPersonalInfoComplete(Map<String, Object> info) {
        return info != null && 
               isValidString(info.get("firstName")) &&
               isValidString(info.get("lastName")) &&
               isValidString(info.get("emailAddress")) &&
               isValidString(info.get("mobileNumber")) &&
               isValidString(info.get("panNumber")) &&
               info.containsKey("address") && info.get("address") != null;
    }
    
    private static boolean isValidString(Object obj) {
        return obj instanceof String str && !str.trim().isEmpty();
    }
    
    private static boolean isTradingPreferencesComplete(Map<String, Object> prefs) {
        return prefs != null &&
               prefs.containsKey("preferredSegments") && prefs.get("preferredSegments") != null &&
               prefs.containsKey("riskProfile") && prefs.get("riskProfile") != null &&
               prefs.containsKey("defaultOrderSettings") && prefs.get("defaultOrderSettings") != null;
    }
    
    private static boolean isNotificationSettingsComplete(Map<String, Object> settings) {
        return settings != null; // Basic settings are enough
    }
    
    private static boolean isKycComplete(Map<String, Object> kycInfo) {
        return kycInfo != null && 
               "VERIFIED".equals(kycInfo.get("kycStatus"));
    }
    
    private static int getKycCompletionPercentage(Map<String, Object> kycInfo) {
        if (kycInfo == null) return 0;
        
        int totalSteps = 4; // PAN, Aadhaar, Documents, Bank Account
        int completedSteps = 0;
        
        if (Boolean.TRUE.equals(kycInfo.get("panVerified"))) completedSteps++;
        if (Boolean.TRUE.equals(kycInfo.get("aadhaarVerified"))) completedSteps++;
        if (Boolean.TRUE.equals(kycInfo.get("documentsVerified"))) completedSteps++;
        if (Boolean.TRUE.equals(kycInfo.get("bankAccountVerified"))) completedSteps++;
        
        return (completedSteps * 100) / totalSteps;
    }
    
    private static boolean canTrade(Map<String, Object> kycInfo) {
        return isKycComplete(kycInfo) && 
               Boolean.TRUE.equals(kycInfo.get("panVerified")) && 
               Boolean.TRUE.equals(kycInfo.get("bankAccountVerified"));
    }
    
    private static List<String> getNextSteps(UserProfile profile) {
        List<String> nextSteps = new java.util.ArrayList<>();
        
        if (!isPersonalInfoComplete(profile.getPersonalInfo())) {
            nextSteps.add("Complete personal information");
        }
        
        if (!isTradingPreferencesComplete(profile.getTradingPreferences())) {
            nextSteps.add("Set trading preferences and risk profile");
        }
        
        Map<String, Object> kycInfo = profile.getKycInformation();
        if (!isKycComplete(kycInfo)) {
            if (!Boolean.TRUE.equals(kycInfo.get("panVerified"))) {
                nextSteps.add("Verify PAN card");
            }
            if (!Boolean.TRUE.equals(kycInfo.get("aadhaarVerified"))) {
                nextSteps.add("Verify Aadhaar card");
            }
            if (!Boolean.TRUE.equals(kycInfo.get("documentsVerified"))) {
                nextSteps.add("Upload and verify KYC documents");
            }
            if (!Boolean.TRUE.equals(kycInfo.get("bankAccountVerified"))) {
                nextSteps.add("Verify bank account details");
            }
        }
        
        return nextSteps;
    }
    
    private static String maskEmail(String email) {
        if (email == null || email.length() < 3) return email;
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domainPart;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + domainPart;
    }
    
    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 10) return mobile;
        return mobile.substring(0, 2) + "****" + mobile.substring(6);
    }
    
    private static String maskPan(String pan) {
        if (pan == null || pan.length() != 10) return pan;
        return pan.substring(0, 3) + "****" + pan.substring(7);
    }
}

@Data
@Builder
class ProfileCompletionStatus {
    private int overallPercentage;
    private boolean personalInfoComplete;
    private boolean tradingPreferencesComplete;
    private boolean kycComplete;
    private int kycCompletionPercentage;
    private boolean notificationSettingsComplete;
    private boolean canTrade;
    private List<String> nextSteps;
}

@Data
@Builder
class UserDocumentResponse {
    private UUID id;
    private DocumentType documentType;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private VerificationStatus verificationStatus;
    private String verificationRemarks;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant uploadedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant verifiedAt;
    
    public static UserDocumentResponse fromEntity(UserDocument document) {
        return UserDocumentResponse.builder()
            .id(document.getId())
            .documentType(document.getDocumentType())
            .fileName(document.getFileName())
            .fileSize(document.getFileSize())
            .mimeType(document.getMimeType())
            .verificationStatus(document.getVerificationStatus())
            .verificationRemarks(document.getVerificationRemarks())
            .uploadedAt(document.getUploadedAt())
            .verifiedAt(document.getVerifiedAt())
            .build();
    }
}