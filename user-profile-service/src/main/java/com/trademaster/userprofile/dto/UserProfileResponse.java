package com.trademaster.userprofile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.userprofile.entity.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    
    private UUID id;
    private UUID userId;
    private PersonalInformation personalInfo;
    private TradingPreferences tradingPreferences;
    private KYCInformation kycInfo;
    private NotificationSettings notificationSettings;
    private Integer version;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
    
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
            .kycInfo(profile.getKycInfo())
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
        PersonalInformation maskedPersonalInfo = PersonalInformation.builder()
            .firstName(profile.getPersonalInfo().firstName())
            .lastName(profile.getPersonalInfo().lastName())
            .emailAddress(maskEmail(profile.getPersonalInfo().emailAddress()))
            .mobileNumber(maskMobile(profile.getPersonalInfo().mobileNumber()))
            .panNumber(maskPan(profile.getPersonalInfo().panNumber()))
            .profilePhotoUrl(profile.getPersonalInfo().profilePhotoUrl())
            .occupation(profile.getPersonalInfo().occupation())
            .tradingExperience(profile.getPersonalInfo().tradingExperience())
            // Exclude sensitive fields like full address, Aadhaar, etc.
            .build();
            
        return UserProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .personalInfo(maskedPersonalInfo)
            .kycInfo(profile.getKycInfo()) // KYC status is okay to show
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
        if (profile.getKycInfo().isKYCComplete()) completedSteps++;
        if (isNotificationSettingsComplete(profile.getNotificationSettings())) completedSteps++;
        
        int kycCompletionPercentage = profile.getKycInfo().getKYCCompletionPercentage();
        
        return ProfileCompletionStatus.builder()
            .overallPercentage((completedSteps * 100) / totalSteps)
            .personalInfoComplete(isPersonalInfoComplete(profile.getPersonalInfo()))
            .tradingPreferencesComplete(isTradingPreferencesComplete(profile.getTradingPreferences()))
            .kycComplete(profile.getKycInfo().isKYCComplete())
            .kycCompletionPercentage(kycCompletionPercentage)
            .notificationSettingsComplete(isNotificationSettingsComplete(profile.getNotificationSettings()))
            .canTrade(profile.getKycInfo().canTrade())
            .nextSteps(getNextSteps(profile))
            .build();
    }
    
    private static boolean isPersonalInfoComplete(PersonalInformation info) {
        return info != null && 
               info.firstName() != null && !info.firstName().trim().isEmpty() &&
               info.lastName() != null && !info.lastName().trim().isEmpty() &&
               info.emailAddress() != null && !info.emailAddress().trim().isEmpty() &&
               info.mobileNumber() != null && !info.mobileNumber().trim().isEmpty() &&
               info.panNumber() != null && !info.panNumber().trim().isEmpty() &&
               info.address() != null;
    }
    
    private static boolean isTradingPreferencesComplete(TradingPreferences prefs) {
        return prefs != null &&
               prefs.preferredSegments() != null && !prefs.preferredSegments().isEmpty() &&
               prefs.riskProfile() != null &&
               prefs.defaultOrderSettings() != null;
    }
    
    private static boolean isNotificationSettingsComplete(NotificationSettings settings) {
        return settings != null; // Basic settings are enough
    }
    
    private static List<String> getNextSteps(UserProfile profile) {
        List<String> nextSteps = new java.util.ArrayList<>();
        
        if (!isPersonalInfoComplete(profile.getPersonalInfo())) {
            nextSteps.add("Complete personal information");
        }
        
        if (!isTradingPreferencesComplete(profile.getTradingPreferences())) {
            nextSteps.add("Set trading preferences and risk profile");
        }
        
        if (!profile.getKycInfo().isKYCComplete()) {
            if (!profile.getKycInfo().panVerified()) {
                nextSteps.add("Verify PAN card");
            }
            if (!profile.getKycInfo().aadhaarVerified()) {
                nextSteps.add("Verify Aadhaar card");
            }
            if (!profile.getKycInfo().documentsVerified()) {
                nextSteps.add("Upload and verify KYC documents");
            }
            if (!profile.getKycInfo().bankAccountVerified()) {
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