package com.trademaster.userprofile.service;

import com.trademaster.userprofile.dto.UserPreferencesDto;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.entity.UserPreferences;
import com.trademaster.userprofile.exception.ResourceNotFoundException;
import com.trademaster.userprofile.mapper.UserPreferencesMapper;
import com.trademaster.userprofile.repository.UserPreferencesRepository;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user preferences
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesMapper userPreferencesMapper;

    /**
     * Get user preferences by user profile ID
     */
    @Transactional(readOnly = true)
    public UserPreferencesDto getUserPreferences(UUID userProfileId) {
        log.debug("Getting preferences for user profile ID: {}", userProfileId);
        
        Optional<UserPreferences> preferencesOpt = userPreferencesRepository.findByUserProfileId(userProfileId);
        
        if (preferencesOpt.isPresent()) {
            return userPreferencesMapper.toDto(preferencesOpt.get());
        } else {
            // Create default preferences if they don't exist
            return createDefaultPreferences(userProfileId);
        }
    }

    /**
     * Get user preferences by email
     */
    @Transactional(readOnly = true)
    public UserPreferencesDto getUserPreferencesByEmail(String email) {
        log.debug("Getting preferences for user email: {}", email);
        
        Optional<UserPreferences> preferencesOpt = userPreferencesRepository.findByUserEmail(email);
        
        if (preferencesOpt.isPresent()) {
            return userPreferencesMapper.toDto(preferencesOpt.get());
        } else {
            // Find user profile and create default preferences
            List<UserProfile> userProfiles = userProfileRepository.findByEmailAddress(email);
            if (userProfiles.isEmpty()) {
                throw new ResourceNotFoundException("User profile not found for email: " + email);
            }
            UserProfile userProfile = userProfiles.get(0); // Take the first one
            return createDefaultPreferences(userProfile.getId());
        }
    }

    /**
     * Update theme preferences specifically
     */
    public UserPreferencesDto updateThemePreferences(UUID userProfileId, String theme) {
        log.debug("Updating theme preferences for user profile ID: {} to theme: {}", userProfileId, theme);
        
        UserPreferences preferences = getUserPreferencesEntity(userProfileId);
        preferences.setTheme(theme);
        
        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        log.info("Theme preferences updated for user profile ID: {}", userProfileId);
        
        return userPreferencesMapper.toDto(savedPreferences);
    }

    /**
     * Create or update user preferences
     */
    public UserPreferencesDto saveUserPreferences(UUID userProfileId, UserPreferencesDto preferencesDto) {
        log.debug("Saving preferences for user profile ID: {}", userProfileId);
        
        // Verify user profile exists
        UserProfile userProfile = userProfileRepository.findById(userProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        // Find existing preferences or create new
        UserPreferences preferences = userPreferencesRepository.findByUserProfileId(userProfileId)
            .orElse(UserPreferences.builder()
                .userProfile(userProfile)
                .build());

        // Update preferences with DTO values
        updatePreferencesFromDto(preferences, preferencesDto);

        // Save preferences
        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        log.info("Preferences saved successfully for user profile ID: {}", userProfileId);

        return userPreferencesMapper.toDto(savedPreferences);
    }

    /**
     * Create default preferences for a user
     */
    private UserPreferencesDto createDefaultPreferences(UUID userProfileId) {
        log.debug("Creating default preferences for user profile ID: {}", userProfileId);
        
        UserProfile userProfile = userProfileRepository.findById(userProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        UserPreferences defaultPreferences = UserPreferences.builder()
            .userProfile(userProfile)
            .build(); // Builder will set all defaults

        UserPreferences savedPreferences = userPreferencesRepository.save(defaultPreferences);
        log.info("Default preferences created for user profile ID: {}", userProfileId);
        
        return userPreferencesMapper.toDto(savedPreferences);
    }

    /**
     * Get user preferences entity (create if doesn't exist)
     */
    private UserPreferences getUserPreferencesEntity(UUID userProfileId) {
        return userPreferencesRepository.findByUserProfileId(userProfileId)
            .orElseGet(() -> {
                UserProfile userProfile = userProfileRepository.findById(userProfileId)
                    .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));
                
                UserPreferences newPreferences = UserPreferences.builder()
                    .userProfile(userProfile)
                    .build();
                    
                return userPreferencesRepository.save(newPreferences);
            });
    }

    /**
     * Update preferences entity from DTO (simplified version)
     */
    private void updatePreferencesFromDto(UserPreferences preferences, UserPreferencesDto dto) {
        if (dto.getTheme() != null) preferences.setTheme(dto.getTheme());
        if (dto.getLanguage() != null) preferences.setLanguage(dto.getLanguage());
        if (dto.getTimezone() != null) preferences.setTimezone(dto.getTimezone());
        if (dto.getCurrency() != null) preferences.setCurrency(dto.getCurrency());
        
        // Notification preferences
        if (dto.getEmailNotifications() != null) preferences.setEmailNotifications(dto.getEmailNotifications());
        if (dto.getSmsNotifications() != null) preferences.setSmsNotifications(dto.getSmsNotifications());
        if (dto.getPushNotifications() != null) preferences.setPushNotifications(dto.getPushNotifications());
        if (dto.getTradingAlerts() != null) preferences.setTradingAlerts(dto.getTradingAlerts());
        if (dto.getMarketNews() != null) preferences.setMarketNews(dto.getMarketNews());
        if (dto.getPriceAlerts() != null) preferences.setPriceAlerts(dto.getPriceAlerts());
        
        // Trading preferences  
        if (dto.getDefaultOrderType() != null) preferences.setDefaultOrderType(dto.getDefaultOrderType());
        if (dto.getConfirmationDialogs() != null) preferences.setConfirmationDialogs(dto.getConfirmationDialogs());
        if (dto.getRiskWarnings() != null) preferences.setRiskWarnings(dto.getRiskWarnings());
        if (dto.getChartType() != null) preferences.setChartType(dto.getChartType());
        if (dto.getDefaultTimeFrame() != null) preferences.setDefaultTimeFrame(dto.getDefaultTimeFrame());
    }
}