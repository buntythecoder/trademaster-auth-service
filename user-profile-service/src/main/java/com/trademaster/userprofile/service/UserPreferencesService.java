package com.trademaster.userprofile.service;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.repository.UserPreferencesRepository;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Functional User Preferences Service with Caching
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Virtual Threads & Concurrency - Rule #12
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Stream API Mastery - Rule #13
 * MANDATORY: Dynamic Configuration - Rule #16
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferencesService {
    
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserProfileRepository userProfileRepository;
    
    // Error types for functional error handling
    public sealed interface PreferencesError permits
        PreferencesNotFoundError, ValidationError, SystemError, ConcurrencyError {
        String message();
    }
    
    public record PreferencesNotFoundError(String message) implements PreferencesError {}
    public record ValidationError(String message, List<String> details) implements PreferencesError {}
    public record SystemError(String message, Throwable cause) implements PreferencesError {}
    public record ConcurrencyError(String message) implements PreferencesError {}
    
    // ========== QUERY OPERATIONS (FUNCTIONAL READ-ONLY) ==========
    
    /**
     * Find preferences by user profile ID with caching
     */
    @Cacheable(value = "userPreferences", key = "#userProfileId")
    public Result<UserPreferences, PreferencesError> findByUserProfileId(UUID userProfileId) {
        return Result.tryExecute(() -> {
            log.debug("Finding preferences for user profile: {}", userProfileId);
            
            return userPreferencesRepository.findByUserProfileId(userProfileId)
                .map(preferences -> {
                    log.debug("Preferences found for user profile: {}", userProfileId);
                    return preferences;
                })
                .orElseGet(() -> {
                    log.debug("Creating default preferences for user profile: {}", userProfileId);
                    return createDefaultPreferences(userProfileId)
                        .getValue().orElse(getDefaultPreferences());
                });
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Get preferences with default fallback using functional composition
     */
    public Result<UserPreferences, PreferencesError> getPreferencesWithDefaults(UUID userProfileId) {
        return findByUserProfileId(userProfileId)
            .map(preferences -> preferences != null ? preferences : getDefaultPreferences())
            .onSuccess(preferences -> log.debug("Retrieved preferences for user: {}", userProfileId))
            .onFailure(error -> log.warn("Failed to retrieve preferences for user: {}, error: {}", 
                userProfileId, error.message()));
    }
    
    /**
     * Find users by theme preference using functional filtering
     */
    public Result<List<UserPreferences>, PreferencesError> findByTheme(String theme) {
        return Result.tryExecute(() -> {
            log.debug("Finding users with theme: {}", theme);
            
            return userPreferencesRepository.findByTheme(theme)
                .stream()
                .filter(createThemeFilter(theme))
                .toList();
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Find users with notifications enabled
     */
    public Result<List<UserPreferences>, PreferencesError> findUsersWithNotifications() {
        return Result.tryExecute(() -> {
            log.debug("Finding users with notifications enabled");
            
            return userPreferencesRepository.findWithNotificationsEnabled()
                .stream()
                .filter(UserPreferences::hasAnyNotifications)
                .toList();
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Find users with trading alerts enabled
     */
    public Result<List<UserPreferences>, PreferencesError> findUsersWithTradingAlerts() {
        return Result.tryExecute(() -> {
            log.debug("Finding users with trading alerts enabled");
            
            return userPreferencesRepository.findWithTradingAlertsEnabled()
                .stream()
                .filter(UserPreferences::hasAnyAlerts)
                .toList();
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Find high security users (2FA + short session timeout)
     */
    public Result<List<UserPreferences>, PreferencesError> findHighSecurityUsers() {
        return Result.tryExecute(() -> {
            log.debug("Finding high security users");
            
            return userPreferencesRepository.findWithTwoFactorEnabled()
                .stream()
                .filter(UserPreferences::isHighSecurityMode)
                .toList();
        }).mapError(this::mapToPreferencesError);
    }
    
    // ========== COMMAND OPERATIONS (TRANSACTIONAL WRITES) ==========
    
    /**
     * Create preferences with validation and defaults
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userProfileId")
    public Result<UserPreferences, PreferencesError> createPreferences(UUID userProfileId) {
        return validateUserProfileExists(userProfileId)
            .flatMap(userProfile -> createDefaultPreferencesInternal(userProfile))
            .onSuccess(preferences -> log.info("Preferences created successfully for user: {}", userProfileId))
            .onFailure(error -> log.error("Failed to create preferences for user: {}, error: {}", 
                userProfileId, error.message()));
    }
    
    /**
     * Update preferences using functional composition
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userProfileId")
    public Result<UserPreferences, PreferencesError> updatePreferences(
            UUID userProfileId,
            Function<UserPreferences, UserPreferences> updateFunction) {
        
        return findByUserProfileId(userProfileId)
            .flatMap(existingPreferences -> applyPreferencesUpdate(existingPreferences, updateFunction))
            .flatMap(this::savePreferences)
            .onSuccess(preferences -> log.info("Preferences updated successfully for user: {}", userProfileId))
            .onFailure(error -> log.error("Failed to update preferences for user: {}, error: {}", 
                userProfileId, error.message()));
    }
    
    /**
     * Update theme preferences with validation
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userProfileId")
    public Result<UserPreferences, PreferencesError> updateTheme(UUID userProfileId, String theme) {
        return validateTheme(theme)
            .flatMap(validTheme -> updatePreferences(userProfileId, 
                preferences -> preferences.withTheme(validTheme)))
            .onSuccess(preferences -> log.info("Theme updated to {} for user: {}", theme, userProfileId));
    }
    
    /**
     * Update notification settings using functional approach
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userProfileId")
    public Result<UserPreferences, PreferencesError> updateNotificationSettings(
            UUID userProfileId,
            Boolean email,
            Boolean sms,
            Boolean push) {
        
        return validateNotificationSettings(email, sms, push)
            .flatMap(validSettings -> updatePreferences(userProfileId,
                preferences -> preferences.withNotificationSettings(email, sms, push)))
            .onSuccess(preferences -> log.info("Notification settings updated for user: {}", userProfileId));
    }
    
    /**
     * Update security settings with validation
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userProfileId")  
    public Result<UserPreferences, PreferencesError> updateSecuritySettings(
            UUID userProfileId,
            Boolean twoFactorEnabled,
            Integer sessionTimeout) {
        
        return validateSecuritySettings(twoFactorEnabled, sessionTimeout)
            .flatMap(validSettings -> updatePreferences(userProfileId,
                preferences -> preferences.withSecuritySettings(twoFactorEnabled, sessionTimeout)))
            .onSuccess(preferences -> log.info("Security settings updated for user: {}", userProfileId));
    }
    
    /**
     * Bulk update preferences for multiple users
     */
    @Transactional
    @CacheEvict(value = "userPreferences", allEntries = true)
    public Result<List<UserPreferences>, PreferencesError> bulkUpdatePreferences(
            List<UUID> userProfileIds,
            Function<UserPreferences, UserPreferences> updateFunction) {
        
        return Result.tryExecute(() -> {
            log.info("Starting bulk preferences update for {} users", userProfileIds.size());
            
            List<UserPreferences> updatedPreferences = userProfileIds.stream()
                .map(userId -> updatePreferences(userId, updateFunction))
                .filter(Result::isSuccess)
                .map(result -> result.getValue().orElse(null))
                .filter(Objects::nonNull)
                .toList();
            
            log.info("Bulk update completed. Updated {} of {} preferences", 
                updatedPreferences.size(), userProfileIds.size());
            
            return updatedPreferences;
        }).mapError(this::mapToPreferencesError);
    }
    
    // ========== ASYNCHRONOUS OPERATIONS WITH VIRTUAL THREADS ==========
    
    /**
     * Async preferences retrieval using Virtual Threads
     */
    public CompletableFuture<Result<UserPreferences, PreferencesError>> findByUserProfileIdAsync(UUID userProfileId) {
        return CompletableFuture.supplyAsync(
            () -> findByUserProfileId(userProfileId),
            runnable -> Thread.ofVirtual().start(runnable)
        );
    }
    
    /**
     * Async preferences notification processing
     */
    public CompletableFuture<Result<List<UserPreferences>, PreferencesError>> processNotificationPreferences() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing notification preferences for eligible users");
            
            return Result.tryExecute(() -> {
                return userPreferencesRepository.findWithNotificationsEnabled()
                    .parallelStream()
                    .filter(this::shouldProcessNotifications)
                    .toList();
            }).mapError(this::mapToPreferencesError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Default preferences factory method
     */
    private UserPreferences getDefaultPreferences() {
        return UserPreferences.builder()
            .theme("LIGHT")
            .language("en-US")
            .timezone("UTC")
            .currency("USD")
            .emailNotifications(true)
            .smsNotifications(false)
            .pushNotifications(true)
            .tradingAlerts(true)
            .priceAlerts(true)
            .twoFactorEnabled(false)
            .sessionTimeout(30)
            .build();
    }
    
    /**
     * Create default preferences for user
     */
    private Result<UserPreferences, PreferencesError> createDefaultPreferences(UUID userProfileId) {
        return validateUserProfileExists(userProfileId)
            .flatMap(this::createDefaultPreferencesInternal);
    }
    
    /**
     * Validate user profile exists
     */
    private Result<UserProfile, PreferencesError> validateUserProfileExists(UUID userProfileId) {
        return Result.tryExecute(() -> {
            return userProfileRepository.findByUserId(userProfileId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + userProfileId));
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Create default preferences internal
     */
    private Result<UserPreferences, PreferencesError> createDefaultPreferencesInternal(UserProfile userProfile) {
        return Result.tryExecute(() -> {
            UserPreferences preferences = UserPreferences.builder()
                .userProfile(userProfile)
                .theme("LIGHT")
                .language("en-US")
                .timezone("UTC")
                .currency("USD")
                .emailNotifications(true)
                .smsNotifications(false)
                .pushNotifications(true)
                .tradingAlerts(true)
                .priceAlerts(true)
                .twoFactorEnabled(false)
                .sessionTimeout(30)
                .build();
            
            return userPreferencesRepository.save(preferences);
        }).mapError(this::mapToPreferencesError);
    }
    
    /**
     * Apply preferences update functionally
     */
    private Result<UserPreferences, PreferencesError> applyPreferencesUpdate(
            UserPreferences existingPreferences,
            Function<UserPreferences, UserPreferences> updateFunction) {
        
        return Result.tryExecute(() -> updateFunction.apply(existingPreferences))
            .mapError(this::mapToPreferencesError);
    }
    
    /**
     * Save preferences with error handling
     */
    private Result<UserPreferences, PreferencesError> savePreferences(UserPreferences preferences) {
        return Result.tryExecute(() -> userPreferencesRepository.save(preferences))
            .mapError(this::mapToPreferencesError);
    }
    
    /**
     * Theme filter using functional composition
     */
    private Predicate<UserPreferences> createThemeFilter(String theme) {
        return preferences -> theme.equalsIgnoreCase(preferences.getTheme());
    }
    
    /**
     * Check if user should receive notifications
     */
    private boolean shouldProcessNotifications(UserPreferences preferences) {
        return preferences.hasAnyNotifications() && 
               preferences.hasAnyAlerts();
    }
    
    // ========== VALIDATION METHODS ==========
    
    /**
     * Theme validation using pattern matching
     */
    private Result<String, PreferencesError> validateTheme(String theme) {
        return Result.tryExecute(() -> {
            List<String> allowedThemes = List.of("LIGHT", "DARK", "AUTO");
            
            if (allowedThemes.contains(theme.toUpperCase())) {
                return theme.toUpperCase();
            } else {
                throw new RuntimeException("Invalid theme: " + theme + ". Allowed: " + allowedThemes);
            }
        }).mapError(throwable -> new ValidationError("Theme validation failed", List.of(throwable.getMessage())));
    }
    
    /**
     * Notification settings validation
     */
    private Result<NotificationSettingsRequest, PreferencesError> validateNotificationSettings(
            Boolean email, Boolean sms, Boolean push) {
        
        return Result.tryExecute(() -> {
            // At least one notification type must be enabled
            if (Boolean.FALSE.equals(email) && Boolean.FALSE.equals(sms) && Boolean.FALSE.equals(push)) {
                throw new RuntimeException("At least one notification type must be enabled");
            }
            
            return new NotificationSettingsRequest(
                email != null ? email : false,
                sms != null ? sms : false,
                push != null ? push : false
            );
        }).mapError(throwable -> new ValidationError("Notification settings validation failed", 
            List.of(throwable.getMessage())));
    }
    
    /**
     * Security settings validation
     */
    private Result<SecuritySettingsRequest, PreferencesError> validateSecuritySettings(
            Boolean twoFactorEnabled, Integer sessionTimeout) {
        
        return Result.tryExecute(() -> {
            // Session timeout validation
            if (sessionTimeout != null && (sessionTimeout < 5 || sessionTimeout > 180)) {
                throw new RuntimeException("Session timeout must be between 5 and 180 minutes");
            }
            
            return new SecuritySettingsRequest(
                twoFactorEnabled != null ? twoFactorEnabled : false,
                sessionTimeout != null ? sessionTimeout : 30
            );
        }).mapError(throwable -> new ValidationError("Security settings validation failed", 
            List.of(throwable.getMessage())));
    }
    
    // ========== REQUEST RECORDS ==========
    
    private record NotificationSettingsRequest(Boolean email, Boolean sms, Boolean push) {}
    private record SecuritySettingsRequest(Boolean twoFactorEnabled, Integer sessionTimeout) {}
    
    // ========== ERROR MAPPING ==========
    
    /**
     * Map exceptions to functional error types using pattern matching
     */
    private PreferencesError mapToPreferencesError(Throwable throwable) {
        return switch (throwable) {
            case IllegalArgumentException iae -> 
                new ValidationError("Invalid argument: " + iae.getMessage(), List.of());
            case RuntimeException re when re.getMessage().contains("not found") -> 
                new PreferencesNotFoundError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("validation") -> 
                new ValidationError(re.getMessage(), List.of());
            case RuntimeException re when re.getMessage().contains("concurrency") -> 
                new ConcurrencyError(re.getMessage());
            default -> 
                new SystemError("System error occurred", throwable);
        };
    }
}