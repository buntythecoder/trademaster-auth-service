package com.trademaster.userprofile.service;

import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEventService {
    
    private final RabbitTemplate rabbitTemplate;
    
    // Exchange and routing keys
    private static final String PROFILE_EXCHANGE = "trademaster.profiles";
    private static final String PROFILE_CREATED_ROUTING_KEY = "profile.created";
    private static final String PROFILE_UPDATED_ROUTING_KEY = "profile.updated";
    private static final String PROFILE_DELETED_ROUTING_KEY = "profile.deleted";
    private static final String TRADING_PREFERENCES_UPDATED_ROUTING_KEY = "profile.trading-preferences.updated";
    private static final String KYC_VERIFIED_ROUTING_KEY = "profile.kyc.verified";
    private static final String DOCUMENT_UPLOADED_ROUTING_KEY = "profile.document.uploaded";
    private static final String DOCUMENT_VERIFIED_ROUTING_KEY = "profile.document.verified";
    
    /**
     * Publish profile created event
     */
    public void publishProfileCreatedEvent(UserProfile profile) {
        try {
            ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getPersonalInfo().firstName())
                .lastName(profile.getPersonalInfo().lastName())
                .email(profile.getPersonalInfo().emailAddress())
                .mobile(profile.getPersonalInfo().mobileNumber())
                .panNumber(profile.getPersonalInfo().panNumber())
                .kycStatus(profile.getKycInfo().kycStatus().toString())
                .createdAt(profile.getCreatedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, PROFILE_CREATED_ROUTING_KEY, event);
            log.info("Published profile created event for user: {}", profile.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to publish profile created event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish profile updated event
     */
    public void publishProfileUpdatedEvent(UserProfile profile) {
        try {
            ProfileUpdatedEvent event = ProfileUpdatedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getPersonalInfo().firstName())
                .lastName(profile.getPersonalInfo().lastName())
                .email(profile.getPersonalInfo().emailAddress())
                .mobile(profile.getPersonalInfo().mobileNumber())
                .kycStatus(profile.getKycInfo().kycStatus().toString())
                .version(profile.getVersion())
                .updatedAt(profile.getUpdatedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, PROFILE_UPDATED_ROUTING_KEY, event);
            log.info("Published profile updated event for user: {}", profile.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to publish profile updated event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish profile deleted event
     */
    public void publishProfileDeletedEvent(UserProfile profile) {
        try {
            ProfileDeletedEvent event = ProfileDeletedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .deletedAt(Instant.now())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, PROFILE_DELETED_ROUTING_KEY, event);
            log.info("Published profile deleted event for user: {}", profile.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to publish profile deleted event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish trading preferences updated event
     */
    public void publishTradingPreferencesUpdatedEvent(UserProfile profile) {
        try {
            TradingPreferencesUpdatedEvent event = TradingPreferencesUpdatedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .preferredSegments(profile.getTradingPreferences().preferredSegments())
                .riskLevel(profile.getTradingPreferences().riskProfile().riskLevel().toString())
                .riskToleranceScore(profile.getTradingPreferences().riskProfile().riskToleranceScore())
                .defaultOrderType(profile.getTradingPreferences().defaultOrderSettings().defaultOrderType().toString())
                .updatedAt(profile.getUpdatedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, TRADING_PREFERENCES_UPDATED_ROUTING_KEY, event);
            log.info("Published trading preferences updated event for user: {}", profile.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to publish trading preferences updated event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish KYC verified event
     */
    public void publishKycVerifiedEvent(UserProfile profile) {
        try {
            KycVerifiedEvent event = KycVerifiedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getPersonalInfo().firstName())
                .lastName(profile.getPersonalInfo().lastName())
                .panNumber(profile.getPersonalInfo().panNumber())
                .panVerified(profile.getKycInfo().panVerified())
                .aadhaarVerified(profile.getKycInfo().aadhaarVerified())
                .documentsVerified(profile.getKycInfo().documentsVerified())
                .bankAccountVerified(profile.getKycInfo().bankAccountVerified())
                .kycCompletionPercentage(profile.getKycInfo().getKYCCompletionPercentage())
                .verifiedAt(profile.getKycInfo().kycVerifiedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, KYC_VERIFIED_ROUTING_KEY, event);
            log.info("Published KYC verified event for user: {}", profile.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to publish KYC verified event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish document uploaded event
     */
    public void publishDocumentUploadedEvent(UserProfile profile, com.trademaster.userprofile.entity.UserDocument document) {
        try {
            DocumentUploadedEvent event = DocumentUploadedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .documentId(document.getId())
                .documentType(document.getDocumentType().toString())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .uploadedAt(document.getUploadedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, DOCUMENT_UPLOADED_ROUTING_KEY, event);
            log.info("Published document uploaded event for user: {}, document type: {}", 
                    profile.getUserId(), document.getDocumentType());
            
        } catch (Exception e) {
            log.error("Failed to publish document uploaded event for user: {}", profile.getUserId(), e);
        }
    }
    
    /**
     * Publish document verified event
     */
    public void publishDocumentVerifiedEvent(UserProfile profile, com.trademaster.userprofile.entity.UserDocument document) {
        try {
            DocumentVerifiedEvent event = DocumentVerifiedEvent.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .documentId(document.getId())
                .documentType(document.getDocumentType().toString())
                .verificationStatus(document.getVerificationStatus().toString())
                .verificationRemarks(document.getVerificationRemarks())
                .verifiedAt(document.getVerifiedAt())
                .eventTimestamp(Instant.now())
                .build();
                
            rabbitTemplate.convertAndSend(PROFILE_EXCHANGE, DOCUMENT_VERIFIED_ROUTING_KEY, event);
            log.info("Published document verified event for user: {}, document type: {}, status: {}", 
                    profile.getUserId(), document.getDocumentType(), document.getVerificationStatus());
            
        } catch (Exception e) {
            log.error("Failed to publish document verified event for user: {}", profile.getUserId(), e);
        }
    }
}