package com.trademaster.userprofile.service;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Functional User Profile Service
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Virtual Threads & Concurrency - Rule #12
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Stream API Mastery - Rule #13
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final ProfileValidationService validationService;
    private final ProfileBusinessLogicService businessLogicService;
    
    // Error types for functional error handling
    public sealed interface ProfileError permits
        ProfileNotFoundError, ValidationError, SystemError, ConcurrencyError {
        String message();
    }
    
    public record ProfileNotFoundError(String message) implements ProfileError {}
    public record ValidationError(String message, List<String> details) implements ProfileError {}
    public record SystemError(String message, Throwable cause) implements ProfileError {}
    public record ConcurrencyError(String message) implements ProfileError {}
    
    // ========== QUERY OPERATIONS (FUNCTIONAL READ-ONLY) ==========
    
    /**
     * Find profile by user ID using functional composition
     */
    @Cacheable(value = "userProfiles", key = "#userId")
    public Result<UserProfile, ProfileError> findByUserId(UUID userId) {
        return Result.tryExecute(() -> {
            log.debug("Finding profile for user: {}", userId);
            
            return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    log.debug("Profile found for user: {}", userId);
                    return profile;
                })
                .orElseGet(() -> {
                    log.debug("Profile not found for user: {}", userId);
                    throw new RuntimeException("Profile not found for user: " + userId);
                });
        }).mapError(this::mapToProfileError);
    }
    
    /**
     * Check if profile exists using functional approach
     */
    public Result<Boolean, ProfileError> existsByUserId(UUID userId) {
        return Result.tryExecute(() -> {
            log.debug("Checking profile existence for user: {}", userId);
            return userProfileRepository.existsByUserId(userId);
        }).mapError(this::mapToProfileError);
    }
    
    /**
     * Find profiles by KYC status with functional filtering
     */
    public Result<List<UserProfile>, ProfileError> findByKycStatus(String kycStatus) {
        return Result.tryExecute(() -> {
            log.debug("Finding profiles with KYC status: {}", kycStatus);
            
            Pageable pageable = PageRequest.of(0, 1000);
            return userProfileRepository.findByKycStatus(kycStatus, pageable)
                .stream()
                .filter(profile -> profile.getKycInformation() != null)
                .toList();
        }).mapError(this::mapToProfileError);
    }
    
    /**
     * Find profiles by risk level using pattern matching
     */
    public Result<List<UserProfile>, ProfileError> findByRiskLevel(RiskLevel riskLevel) {
        return Result.tryExecute(() -> {
            log.debug("Finding profiles with risk level: {}", riskLevel);
            
            return userProfileRepository.findByRiskLevel(riskLevel.name())
                .stream()
                .filter(businessLogicService.createRiskLevelFilter(riskLevel.name()))
                .toList();
        }).mapError(this::mapToProfileError);
    }
    
    /**
     * Search profiles with functional composition
     */
    public Result<List<UserProfile>, ProfileError> searchProfiles(String searchTerm) {
        return Result.tryExecute(() -> {
            log.debug("Searching profiles with term: {}", searchTerm);
            
            return Optional.ofNullable(searchTerm)
                .filter(term -> !term.trim().isEmpty())
                .map(term -> userProfileRepository.findAll()
                    .stream()
                    .filter(businessLogicService.createSearchFilter(term))
                    .limit(50)
                    .toList())
                .orElse(List.of());
        }).mapError(this::mapToProfileError);
    }
    
    // ========== COMMAND OPERATIONS (TRANSACTIONAL WRITES) ==========
    
    /**
     * Create new profile using functional validation and builders
     */
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public Result<UserProfile, ProfileError> createProfile(
            UUID userId,
            Map<String, Object> personalInfo,
            Map<String, Object> tradingPreferences,
            Map<String, Object> kycInformation,
            Map<String, Object> notificationSettings) {
        
        return validateCreateProfileInput(userId, personalInfo, tradingPreferences, kycInformation, notificationSettings)
            .flatMap(validatedInput -> createProfileInternal(validatedInput))
            .onSuccess(profile -> log.info("Profile created successfully for user: {}", userId))
            .onFailure(error -> log.error("Failed to create profile for user: {}, error: {}", userId, error.message()));
    }
    
    /**
     * Update profile using functional composition
     */
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public Result<UserProfile, ProfileError> updateProfile(
            UUID userId,
            Function<UserProfile, UserProfile> updateFunction) {
        
        return findByUserId(userId)
            .flatMap(existingProfile -> applyProfileUpdate(existingProfile, updateFunction))
            .flatMap(this::saveProfile)
            .onSuccess(profile -> log.info("Profile updated successfully for user: {}", userId))
            .onFailure(error -> log.error("Failed to update profile for user: {}, error: {}", userId, error.message()));
    }
    
    /**
     * Update KYC information with validation
     */
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public Result<UserProfile, ProfileError> updateKycInformation(
            UUID userId,
            Map<String, Object> kycInformation) {
        
        Result<Map<String, Object>, ProfileError> validationResult = Result.tryExecute(() -> {
                if (kycInformation == null) {
                    throw new RuntimeException("KYC information is required");
                }
                return kycInformation;
            })
            .mapError(throwable -> new ValidationError("KYC validation failed", List.of(throwable.getMessage())));
            
        return validationResult
            .flatMap(validatedKyc -> updateProfile(userId, profile -> 
                profile.withKycInformation(validatedKyc)))
            .onSuccess(profile -> log.info("KYC information updated for user: {}", userId));
    }
    
    /**
     * Activate profile with business rule validation
     */
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public Result<UserProfile, ProfileError> activateProfile(UUID userId) {
        return findByUserId(userId)
            .flatMap(businessLogicService::validateProfileForActivation)
            .flatMap(profile -> updateProfile(userId, businessLogicService.createProfileActivator()))
            .onSuccess(profile -> log.info("Profile activated for user: {}", userId));
    }
    
    // ========== ASYNCHRONOUS OPERATIONS WITH VIRTUAL THREADS ==========
    
    /**
     * Async profile operations using Virtual Threads
     */
    public CompletableFuture<Result<UserProfile, ProfileError>> findByUserIdAsync(UUID userId) {
        return CompletableFuture.supplyAsync(
            () -> findByUserId(userId),
            runnable -> Thread.ofVirtual().start(runnable)
        );
    }
    
    /**
     * Batch process profiles asynchronously
     */
    public CompletableFuture<Result<List<UserProfile>, ProfileError>> batchUpdateProfiles(
            List<UUID> userIds,
            Function<UserProfile, UserProfile> updateFunction) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting batch update for {} profiles", userIds.size());
            
            return Result.tryExecute(() -> {
                List<UserProfile> updatedProfiles = userIds.parallelStream()
                    .map(userId -> updateProfile(userId, updateFunction))
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
                
                log.info("Batch update completed. Updated {} of {} profiles", 
                    updatedProfiles.size(), userIds.size());
                
                return updatedProfiles;
            }).mapError(this::mapToProfileError);
        }, runnable -> Thread.ofVirtual().start(runnable));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Validation input record for type safety
     */
    private record CreateProfileInput(
        UUID userId,
        Map<String, Object> personalInfo,
        Map<String, Object> tradingPreferences,
        Map<String, Object> kycInformation,
        Map<String, Object> notificationSettings
    ) {}
    
    /**
     * Functional validation for profile creation - delegates to validation service
     */
    private Result<CreateProfileInput, ProfileError> validateCreateProfileInput(
            UUID userId,
            Map<String, Object> personalInfo,
            Map<String, Object> tradingPreferences,
            Map<String, Object> kycInformation,
            Map<String, Object> notificationSettings) {
        
        return Result.tryExecute(() -> {
            List<String> validationErrors = new ArrayList<>();
            
            // Functional validation using stream operations
            Stream.of(
                validationService.validateUserId(userId),
                validationService.validatePersonalInfo(personalInfo),
                validationService.validateTradingPreferences(tradingPreferences),
                validationService.validateKycInformationOptional(kycInformation),
                validationService.validateNotificationSettings(notificationSettings)
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(validationErrors::add);
            
            if (validationErrors.isEmpty()) {
                return new CreateProfileInput(userId, personalInfo, tradingPreferences, kycInformation, notificationSettings);
            } else {
                throw new RuntimeException("Validation failed: " + String.join(", ", validationErrors));
            }
        }).mapError(throwable -> new ValidationError("Validation failed", List.of(throwable.getMessage())));
    }
    
    /**
     * Internal profile creation with builder pattern
     */
    private Result<UserProfile, ProfileError> createProfileInternal(CreateProfileInput input) {
        return Result.tryExecute(() -> {
            UserProfile profile = UserProfile.builder()
                .userId(input.userId())
                .personalInfo(input.personalInfo())
                .tradingPreferences(input.tradingPreferences())
                .kycInformation(input.kycInformation())
                .notificationSettings(input.notificationSettings())
                .build();
            
            return userProfileRepository.save(profile);
        }).mapError(this::mapToProfileError);
    }
    
    /**
     * Apply profile updates functionally
     */
    private Result<UserProfile, ProfileError> applyProfileUpdate(
            UserProfile existingProfile,
            Function<UserProfile, UserProfile> updateFunction) {
        
        return Result.tryExecute(() -> updateFunction.apply(existingProfile))
            .mapError(this::mapToProfileError);
    }
    
    /**
     * Save profile with error handling
     */
    private Result<UserProfile, ProfileError> saveProfile(UserProfile profile) {
        return Result.tryExecute(() -> userProfileRepository.save(profile))
            .mapError(this::mapToProfileError);
    }
    
    
    // ========== ERROR MAPPING ==========
    
    /**
     * Map exceptions to functional error types using pattern matching
     */
    private ProfileError mapToProfileError(Throwable throwable) {
        return switch (throwable) {
            case IllegalArgumentException iae -> 
                new ValidationError("Invalid argument: " + iae.getMessage(), List.of());
            case RuntimeException re when re.getMessage().contains("not found") -> 
                new ProfileNotFoundError(re.getMessage());
            case RuntimeException re when re.getMessage().contains("Validation") -> 
                new ValidationError(re.getMessage(), List.of());
            default -> 
                new SystemError("System error occurred", throwable);
        };
    }
}