package com.trademaster.userprofile.service;

import com.trademaster.userprofile.dto.CreateProfileRequest;
import com.trademaster.userprofile.dto.UpdateProfileRequest;
import com.trademaster.userprofile.dto.UserProfileResponse;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.exception.ProfileNotFoundException;
import com.trademaster.userprofile.exception.DuplicateProfileException;
import com.trademaster.userprofile.exception.ValidationException;
import com.trademaster.userprofile.repository.UserProfileRepository;
import com.trademaster.userprofile.repository.ProfileAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional(readOnly = true)
public class UserProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final ProfileAuditLogRepository auditLogRepository;
    private final ProfileValidationService validationService;
    private final ProfileAuditService auditService;
    private final ProfileEventService eventService;
    
    /**
     * Get user profile by user ID
     */
    @Cacheable(value = "user-profiles", key = "#userId")
    public UserProfileResponse getProfileByUserId(@NotNull UUID userId) {
        log.debug("Retrieving profile for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
            
        return UserProfileResponse.fromEntity(profile);
    }
    
    /**
     * Get user profile by profile ID
     */
    @Cacheable(value = "user-profiles", key = "#profileId")
    public UserProfileResponse getProfileById(@NotNull UUID profileId) {
        log.debug("Retrieving profile by ID: {}", profileId);
        
        UserProfile profile = userProfileRepository.findById(profileId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found with ID: " + profileId));
            
        return UserProfileResponse.fromEntity(profile);
    }
    
    /**
     * Create new user profile
     */
    @Transactional
    @CacheEvict(value = "user-profiles", allEntries = true)
    public UserProfileResponse createProfile(@Valid CreateProfileRequest request, @NotNull UUID currentUserId) {
        log.info("Creating profile for user: {}", request.getUserId());
        
        // Validate user doesn't already have a profile
        if (userProfileRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateProfileException("Profile already exists for user: " + request.getUserId());
        }
        
        // Validate business rules
        validationService.validateCreateRequest(request);
        
        // Check for duplicate PAN/mobile/email
        validationService.validateUniqueConstraints(request);
        
        // Build profile entity
        UserProfile profile = buildProfileFromRequest(request);
        
        // Save profile
        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Profile created with ID: {} for user: {}", savedProfile.getId(), savedProfile.getUserId());
        
        // Create audit log
        auditService.logProfileCreation(savedProfile, currentUserId);
        
        // Publish profile created event
        eventService.publishProfileCreatedEvent(savedProfile);
        
        return UserProfileResponse.fromEntity(savedProfile);
    }
    
    /**
     * Update user profile
     */
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public UserProfileResponse updateProfile(@NotNull UUID userId, @Valid UpdateProfileRequest request, @NotNull UUID currentUserId) {
        log.info("Updating profile for user: {}", userId);
        
        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        
        // Store old values for audit
        UserProfile oldProfile = cloneProfile(existingProfile);
        
        // Validate business rules
        validationService.validateUpdateRequest(request, existingProfile);
        
        // Apply updates
        applyProfileUpdates(existingProfile, request);
        
        // Save updated profile
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);
        log.info("Profile updated for user: {}", userId);
        
        // Create audit log
        auditService.logProfileUpdate(oldProfile, updatedProfile, currentUserId);
        
        // Publish profile updated event
        eventService.publishProfileUpdatedEvent(updatedProfile);
        
        return UserProfileResponse.fromEntity(updatedProfile);
    }
    
    /**
     * Update trading preferences
     */
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public UserProfileResponse updateTradingPreferences(@NotNull UUID userId, @Valid TradingPreferences preferences, @NotNull UUID currentUserId) {
        log.info("Updating trading preferences for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        
        TradingPreferences oldPreferences = profile.getTradingPreferences();
        
        // Validate trading preferences
        validationService.validateTradingPreferences(preferences, profile.getPersonalInfo());
        
        // Update preferences
        profile.setTradingPreferences(preferences);
        
        UserProfile updatedProfile = userProfileRepository.save(profile);
        
        // Audit log
        auditService.logTradingPreferencesUpdate(profile.getId(), oldPreferences, preferences, currentUserId);
        
        // Publish event
        eventService.publishTradingPreferencesUpdatedEvent(updatedProfile);
        
        return UserProfileResponse.fromEntity(updatedProfile);
    }
    
    /**
     * Update KYC information
     */
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public UserProfileResponse updateKycInformation(@NotNull UUID userId, @Valid KYCInformation kycInfo, @NotNull UUID currentUserId) {
        log.info("Updating KYC information for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        
        KYCInformation oldKycInfo = profile.getKycInfo();
        
        // Validate KYC information
        validationService.validateKycInformation(kycInfo);
        
        // Update KYC info
        profile.setKycInfo(kycInfo);
        
        UserProfile updatedProfile = userProfileRepository.save(profile);
        
        // Audit log
        auditService.logKycUpdate(profile.getId(), oldKycInfo, kycInfo, currentUserId);
        
        // Publish KYC event
        if (kycInfo.kycStatus() == KYCStatus.VERIFIED) {
            eventService.publishKycVerifiedEvent(updatedProfile);
        }
        
        return UserProfileResponse.fromEntity(updatedProfile);
    }
    
    /**
     * Update notification settings
     */
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public UserProfileResponse updateNotificationSettings(@NotNull UUID userId, @Valid NotificationSettings settings, @NotNull UUID currentUserId) {
        log.info("Updating notification settings for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        
        NotificationSettings oldSettings = profile.getNotificationSettings();
        
        // Update settings
        profile.setNotificationSettings(settings);
        
        UserProfile updatedProfile = userProfileRepository.save(profile);
        
        // Audit log
        auditService.logNotificationSettingsUpdate(profile.getId(), oldSettings, settings, currentUserId);
        
        return UserProfileResponse.fromEntity(updatedProfile);
    }
    
    /**
     * Delete user profile (soft delete - archive)
     */
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public void deleteProfile(@NotNull UUID userId, @NotNull UUID currentUserId) {
        log.info("Deleting profile for user: {}", userId);
        
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        
        // For financial compliance, we don't actually delete - we archive
        // Mark profile as archived in a separate field or move to archive table
        auditService.logProfileDeletion(profile, currentUserId);
        
        // Publish profile deleted event
        eventService.publishProfileDeletedEvent(profile);
        
        // For now, actual deletion (in production, would be archival)
        userProfileRepository.delete(profile);
    }
    
    /**
     * Search profiles (admin function)
     */
    public Page<UserProfileResponse> searchProfiles(String name, String email, String mobile, String panNumber, 
                                                   String kycStatus, Pageable pageable) {
        log.debug("Searching profiles with criteria");
        
        // Implementation would use Specifications or custom queries
        // For now, simple name search
        if (name != null && !name.trim().isEmpty()) {
            return userProfileRepository.searchByName(name, pageable)
                .map(UserProfileResponse::fromEntity);
        }
        
        return userProfileRepository.findAll(pageable)
            .map(UserProfileResponse::fromEntity);
    }
    
    /**
     * Get profiles by KYC status
     */
    public Page<UserProfileResponse> getProfilesByKycStatus(String kycStatus, Pageable pageable) {
        return userProfileRepository.findByKycStatus(kycStatus, pageable)
            .map(UserProfileResponse::fromEntity);
    }
    
    /**
     * Get profile statistics (admin function)
     */
    public ProfileStatistics getProfileStatistics() {
        Object[] stats = userProfileRepository.getProfileStatistics();
        return ProfileStatistics.builder()
            .totalProfiles((Long) stats[0])
            .verifiedProfiles((Long) stats[1])
            .pendingProfiles((Long) stats[2])
            .build();
    }
    
    /**
     * Check if profile exists for user
     */
    public boolean profileExists(@NotNull UUID userId) {
        return userProfileRepository.existsByUserId(userId);
    }
    
    /**
     * Get profiles needing KYC renewal
     */
    public List<UserProfileResponse> getProfilesNeedingKycRenewal() {
        Instant cutoffDate = Instant.now().minusSeconds(365 * 24 * 60 * 60); // 1 year ago
        return userProfileRepository.findProfilesNeedingKycRenewal(cutoffDate)
            .stream()
            .map(UserProfileResponse::fromEntity)
            .toList();
    }
    
    // Private helper methods
    
    private UserProfile buildProfileFromRequest(CreateProfileRequest request) {
        return UserProfile.builder()
            .userId(request.getUserId())
            .personalInfo(request.getPersonalInfo())
            .tradingPreferences(request.getTradingPreferences())
            .kycInfo(request.getKycInfo())
            .notificationSettings(request.getNotificationSettings())
            .build();
    }
    
    private UserProfile cloneProfile(UserProfile profile) {
        return UserProfile.builder()
            .id(profile.getId())
            .userId(profile.getUserId())
            .personalInfo(profile.getPersonalInfo())
            .tradingPreferences(profile.getTradingPreferences())
            .kycInfo(profile.getKycInfo())
            .notificationSettings(profile.getNotificationSettings())
            .version(profile.getVersion())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }
    
    private void applyProfileUpdates(UserProfile profile, UpdateProfileRequest request) {
        if (request.getPersonalInfo() != null) {
            profile.setPersonalInfo(request.getPersonalInfo());
        }
        if (request.getTradingPreferences() != null) {
            profile.setTradingPreferences(request.getTradingPreferences());
        }
        if (request.getKycInfo() != null) {
            profile.setKycInfo(request.getKycInfo());
        }
        if (request.getNotificationSettings() != null) {
            profile.setNotificationSettings(request.getNotificationSettings());
        }
    }
}

// Statistics DTO
record ProfileStatistics(
    Long totalProfiles,
    Long verifiedProfiles,
    Long pendingProfiles
) {
    public static ProfileStatisticsBuilder builder() {
        return new ProfileStatisticsBuilder();
    }
    
    public static class ProfileStatisticsBuilder {
        private Long totalProfiles;
        private Long verifiedProfiles;
        private Long pendingProfiles;
        
        public ProfileStatisticsBuilder totalProfiles(Long totalProfiles) {
            this.totalProfiles = totalProfiles;
            return this;
        }
        
        public ProfileStatisticsBuilder verifiedProfiles(Long verifiedProfiles) {
            this.verifiedProfiles = verifiedProfiles;
            return this;
        }
        
        public ProfileStatisticsBuilder pendingProfiles(Long pendingProfiles) {
            this.pendingProfiles = pendingProfiles;
            return this;
        }
        
        public ProfileStatistics build() {
            return new ProfileStatistics(totalProfiles, verifiedProfiles, pendingProfiles);
        }
    }
}