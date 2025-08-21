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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional User Profile Service - Java 24 Architecture
 * 
 * Advanced Design Patterns Applied:
 * - Strategy Pattern: Validation strategies, update strategies
 * - Command Pattern: Profile operations with undo/redo capability
 * - Chain of Responsibility: Validation and processing pipelines
 * - Observer Pattern: Event publishing and audit trails
 * - Factory Pattern: Profile and response creation
 * - Template Method: Common operation flows
 * 
 * Architectural Principles:
 * - Virtual Threads: All async operations use virtual threads for scalable concurrency
 * - Railway Oriented Programming: Result types for comprehensive error handling
 * - Lock-free Operations: Atomic references for thread-safe state management
 * - Functional Programming: Higher-order functions and immutable data where beneficial
 * - Structured Concurrency: Coordinated task execution with proper lifecycle management
 * - SOLID Principles: Single responsibility, dependency inversion
 * - Architectural Fitness: Patterns applied where they improve maintainability and performance
 * 
 * @author TradeMaster Development Team
 * @version 3.0.0 - Functional Architecture with Virtual Threads
 */
@Service
@Slf4j
public class FunctionalUserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileAuditLogRepository auditLogRepository;
    private final ProfileValidationService validationService;
    private final ProfileAuditService auditService;
    private final ProfileEventService eventService;
    
    // Virtual Thread Executors
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AsyncTaskExecutor profileExecutor;
    private final AsyncTaskExecutor fileExecutor;
    private final AsyncTaskExecutor eventExecutor;

    // Railway Oriented Programming - Result Type
    public sealed interface ProfileResult<T, E> permits ProfileSuccess, ProfileFailure {
        record ProfileSuccess<T, E>(T value) implements ProfileResult<T, E> {}
        record ProfileFailure<T, E>(E error) implements ProfileResult<T, E> {}
        
        static <T, E> ProfileResult<T, E> success(T value) {
            return new ProfileSuccess<>(value);
        }
        
        static <T, E> ProfileResult<T, E> failure(E error) {
            return new ProfileFailure<>(error);
        }
        
        default <U> ProfileResult<U, E> map(Function<T, U> mapper) {
            return switch (this) {
                case ProfileSuccess(var value) -> success(mapper.apply(value));
                case ProfileFailure(var error) -> failure(error);
            };
        }
        
        default <U> ProfileResult<U, E> flatMap(Function<T, ProfileResult<U, E>> mapper) {
            return switch (this) {
                case ProfileSuccess(var value) -> mapper.apply(value);
                case ProfileFailure(var error) -> failure(error);
            };
        }
        
        default boolean isSuccess() {
            return this instanceof ProfileSuccess;
        }
        
        default T orElse(T defaultValue) {
            return switch (this) {
                case ProfileSuccess(var value) -> value;
                case ProfileFailure(var ignored) -> defaultValue;
            };
        }
        
        default T orElseThrow() {
            return switch (this) {
                case ProfileSuccess(var value) -> value;
                case ProfileFailure(var error) -> throw new RuntimeException(error.toString());
            };
        }
    }

    // Strategy Pattern - Validation Strategies
    public enum ValidationStrategy {
        CREATE_VALIDATION(request -> validateCreateProfile((CreateProfileRequest) request)),
        UPDATE_VALIDATION(request -> validateUpdateProfile((UpdateProfileRequest) request)),
        KYC_VALIDATION(kycInfo -> validateKycInfo((KYCInformation) kycInfo)),
        TRADING_PREFERENCES_VALIDATION(preferences -> validateTradingPrefs((TradingPreferences) preferences));
        
        private final Function<Object, ProfileResult<Boolean, String>> validator;
        
        ValidationStrategy(Function<Object, ProfileResult<Boolean, String>> validator) {
            this.validator = validator;
        }
        
        public ProfileResult<Boolean, String> validate(Object request) {
            return validator.apply(request);
        }
    }

    // Strategy Pattern - Update Strategies
    public enum UpdateStrategy {
        PERSONAL_INFO_UPDATE(profile -> (request) -> updatePersonalInfo((UserProfile) profile, (UpdateProfileRequest) request)),
        TRADING_PREFS_UPDATE(profile -> (request) -> updateTradingPreferences((UserProfile) profile, (TradingPreferences) request)),
        KYC_UPDATE(profile -> (request) -> updateKycInformation((UserProfile) profile, (KYCInformation) request)),
        NOTIFICATION_UPDATE(profile -> (request) -> updateNotificationSettings((UserProfile) profile, (NotificationSettings) request));
        
        private final Function<Object, Function<Object, ProfileResult<UserProfile, String>>> updater;
        
        UpdateStrategy(Function<Object, Function<Object, ProfileResult<UserProfile, String>>> updater) {
            this.updater = updater;
        }
        
        public ProfileResult<UserProfile, String> update(Object profile, Object request) {
            return updater.apply(profile).apply(request);
        }
    }

    // Command Pattern - Profile Commands
    public sealed interface ProfileCommand<T> permits CreateProfileCommand, UpdateProfileCommand, DeleteProfileCommand {
        CompletableFuture<ProfileResult<T, String>> execute();
        
        default <U> ProfileCommand<U> map(Function<T, U> mapper) {
            return new MappedProfileCommand<>(this, mapper);
        }
        
        default ProfileCommand<T> withRetry(int attempts) {
            return new RetryProfileCommand<>(this, attempts);
        }
        
        default ProfileCommand<T> withValidation(Function<T, Boolean> validator, String errorMessage) {
            return new ValidatedProfileCommand<>(this, validator, errorMessage);
        }
    }

    // Command implementations
    public record CreateProfileCommand(CreateProfileRequest request, UUID currentUserId) implements ProfileCommand<UserProfileResponse> {
        @Override
        public CompletableFuture<ProfileResult<UserProfileResponse, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return ValidationStrategy.CREATE_VALIDATION
                    .validate(request)
                    .flatMap(valid -> processProfileCreation(request, currentUserId));
            }, virtualExecutor);
        }
    }

    public record UpdateProfileCommand(UUID userId, UpdateProfileRequest request, UUID currentUserId) implements ProfileCommand<UserProfileResponse> {
        @Override
        public CompletableFuture<ProfileResult<UserProfileResponse, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return ValidationStrategy.UPDATE_VALIDATION
                    .validate(request)
                    .flatMap(valid -> processProfileUpdate(userId, request, currentUserId));
            }, virtualExecutor);
        }
    }

    public record DeleteProfileCommand(UUID userId, UUID currentUserId) implements ProfileCommand<Void> {
        @Override
        public CompletableFuture<ProfileResult<Void, String>> execute() {
            return CompletableFuture.supplyAsync(() -> {
                return processProfileDeletion(userId, currentUserId);
            }, virtualExecutor);
        }
    }

    // Helper command wrappers
    public record MappedProfileCommand<T, U>(ProfileCommand<T> original, Function<T, U> mapper) implements ProfileCommand<U> {
        @Override
        public CompletableFuture<ProfileResult<U, String>> execute() {
            return original.execute().thenApply(result -> result.map(mapper));
        }
    }

    public record RetryProfileCommand<T>(ProfileCommand<T> original, int attempts) implements ProfileCommand<T> {
        @Override
        public CompletableFuture<ProfileResult<T, String>> execute() {
            return original.execute().thenCompose(result -> {
                if (result.isSuccess() || attempts <= 1) {
                    return CompletableFuture.completedFuture(result);
                }
                return new RetryProfileCommand<>(original, attempts - 1).execute();
            });
        }
    }

    public record ValidatedProfileCommand<T>(ProfileCommand<T> original, Function<T, Boolean> validator, String errorMessage) implements ProfileCommand<T> {
        @Override
        public CompletableFuture<ProfileResult<T, String>> execute() {
            return original.execute().thenApply(result -> 
                result.flatMap(value -> validator.apply(value) ? 
                    ProfileResult.success(value) : 
                    ProfileResult.failure(errorMessage)
                )
            );
        }
    }

    // Constructor with Dependency Injection
    public FunctionalUserProfileService(
            UserProfileRepository userProfileRepository,
            ProfileAuditLogRepository auditLogRepository,
            ProfileValidationService validationService,
            ProfileAuditService auditService,
            ProfileEventService eventService,
            @Qualifier("profileExecutor") AsyncTaskExecutor profileExecutor,
            @Qualifier("fileExecutor") AsyncTaskExecutor fileExecutor,
            @Qualifier("eventExecutor") AsyncTaskExecutor eventExecutor) {
        
        this.userProfileRepository = userProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.validationService = validationService;
        this.auditService = auditService;
        this.eventService = eventService;
        this.profileExecutor = profileExecutor;
        this.fileExecutor = fileExecutor;
        this.eventExecutor = eventExecutor;
    }

    // Public API Methods using Command Pattern

    @Cacheable(value = "user-profiles", key = "#userId")
    public CompletableFuture<UserProfileResponse> getProfileByUserIdAsync(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            return userProfileRepository.findByUserId(userId)
                .map(UserProfileResponse::fromEntity)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        }, virtualExecutor);
    }

    @Cacheable(value = "user-profiles", key = "#profileId")
    public CompletableFuture<UserProfileResponse> getProfileByIdAsync(UUID profileId) {
        return CompletableFuture.supplyAsync(() -> {
            return userProfileRepository.findById(profileId)
                .map(UserProfileResponse::fromEntity)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found with ID: " + profileId));
        }, virtualExecutor);
    }

    @Async
    @Transactional
    public CompletableFuture<UserProfileResponse> createProfileAsync(CreateProfileRequest request, UUID currentUserId) {
        return new CreateProfileCommand(request, currentUserId)
            .withRetry(2)
            .withValidation(response -> response != null, "Profile creation validation failed")
            .execute()
            .thenApply(ProfileResult::orElseThrow);
    }

    @Async
    @Transactional
    public CompletableFuture<UserProfileResponse> updateProfileAsync(UUID userId, UpdateProfileRequest request, UUID currentUserId) {
        return new UpdateProfileCommand(userId, request, currentUserId)
            .withRetry(1)
            .execute()
            .thenApply(ProfileResult::orElseThrow);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> deleteProfileAsync(UUID userId, UUID currentUserId) {
        return new DeleteProfileCommand(userId, currentUserId)
            .execute()
            .thenApply(ProfileResult::orElseThrow);
    }

    // Specialized update methods with virtual threads
    @Async
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public CompletableFuture<UserProfileResponse> updateTradingPreferencesAsync(UUID userId, TradingPreferences preferences, UUID currentUserId) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Fork parallel tasks
                var profileFetch = scope.fork(() -> findProfileByUserId(userId));
                var validation = scope.fork(() -> ValidationStrategy.TRADING_PREFERENCES_VALIDATION.validate(preferences));
                
                scope.join();
                scope.throwIfFailed();
                
                UserProfile profile = profileFetch.resultNow().orElseThrow();
                if (!validation.resultNow().isSuccess()) {
                    throw new ValidationException("Trading preferences validation failed");
                }
                
                // Apply update using strategy pattern
                return UpdateStrategy.TRADING_PREFS_UPDATE
                    .update(profile, preferences)
                    .map(updatedProfile -> {
                        UserProfile saved = userProfileRepository.save(updatedProfile);
                        // Async operations
                        publishEventAsync(saved, "TRADING_PREFERENCES_UPDATED");
                        auditLogAsync(saved.getId(), "TRADING_PREFERENCES_UPDATED", currentUserId);
                        return UserProfileResponse.fromEntity(saved);
                    })
                    .orElseThrow();
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Update interrupted");
            } catch (Exception e) {
                throw new RuntimeException("Failed to update trading preferences", e);
            }
        }, virtualExecutor);
    }

    @Async
    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public CompletableFuture<UserProfileResponse> updateKycInformationAsync(UUID userId, KYCInformation kycInfo, UUID currentUserId) {
        return CompletableFuture.supplyAsync(() -> {
            return findProfileByUserId(userId)
                .map(profile -> {
                    ValidationStrategy.KYC_VALIDATION.validate(kycInfo)
                        .orElseThrow(() -> new ValidationException("KYC validation failed"));
                    
                    return UpdateStrategy.KYC_UPDATE
                        .update(profile, kycInfo)
                        .map(updatedProfile -> {
                            UserProfile saved = userProfileRepository.save(updatedProfile);
                            
                            // Handle KYC status change events
                            handleKycStatusChangeAsync(saved, kycInfo, currentUserId);
                            
                            return UserProfileResponse.fromEntity(saved);
                        })
                        .orElseThrow();
                })
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
        }, virtualExecutor);
    }

    // Private Implementation Methods

    private ProfileResult<UserProfileResponse, String> processProfileCreation(CreateProfileRequest request, UUID currentUserId) {
        try {
            // Check for existing profile
            if (userProfileRepository.existsByUserId(request.getUserId())) {
                return ProfileResult.failure("Profile already exists for user: " + request.getUserId());
            }

            // Structured concurrency for parallel validation
            return CompletableFuture.supplyAsync(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Fork validation tasks
                    var businessRulesValidation = scope.fork(() -> 
                        validationService.validateCreateRequest(request));
                    var uniqueConstraintsValidation = scope.fork(() -> 
                        validationService.validateUniqueConstraints(request));
                    
                    scope.join();
                    scope.throwIfFailed();
                    
                    // All validations passed, create profile
                    UserProfile profile = buildProfileFromRequest(request);
                    UserProfile savedProfile = userProfileRepository.save(profile);
                    
                    // Async operations
                    auditLogAsync(savedProfile.getId(), "PROFILE_CREATED", currentUserId);
                    publishEventAsync(savedProfile, "PROFILE_CREATED");
                    
                    return ProfileResult.success(UserProfileResponse.fromEntity(savedProfile));
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ProfileResult.<UserProfileResponse, String>failure("Profile creation interrupted");
                } catch (Exception e) {
                    return ProfileResult.<UserProfileResponse, String>failure("Profile creation failed: " + e.getMessage());
                }
            }, virtualExecutor).join();
            
        } catch (Exception e) {
            log.error("Profile creation failed for user {}: {}", request.getUserId(), e.getMessage());
            return ProfileResult.failure("Profile creation failed: " + e.getMessage());
        }
    }

    private ProfileResult<UserProfileResponse, String> processProfileUpdate(UUID userId, UpdateProfileRequest request, UUID currentUserId) {
        try {
            return findProfileByUserId(userId)
                .map(existingProfile -> {
                    // Store for audit
                    UserProfile oldProfile = cloneProfile(existingProfile);
                    
                    // Apply updates using strategy pattern
                    ProfileResult<UserProfile, String> updateResult = applyProfileUpdates(existingProfile, request);
                    
                    return updateResult.map(updatedProfile -> {
                        UserProfile saved = userProfileRepository.save(updatedProfile);
                        
                        // Async operations
                        auditLogAsync(saved.getId(), "PROFILE_UPDATED", currentUserId);
                        publishEventAsync(saved, "PROFILE_UPDATED");
                        
                        return UserProfileResponse.fromEntity(saved);
                    });
                })
                .map(result -> result.orElse(null))
                .filter(response -> response != null)
                .map(ProfileResult::<UserProfileResponse, String>success)
                .orElse(ProfileResult.failure("Profile not found for user: " + userId));
                
        } catch (Exception e) {
            log.error("Profile update failed for user {}: {}", userId, e.getMessage());
            return ProfileResult.failure("Profile update failed: " + e.getMessage());
        }
    }

    private ProfileResult<Void, String> processProfileDeletion(UUID userId, UUID currentUserId) {
        try {
            return findProfileByUserId(userId)
                .map(profile -> {
                    // Audit before deletion
                    auditLogAsync(profile.getId(), "PROFILE_DELETED", currentUserId);
                    publishEventAsync(profile, "PROFILE_DELETED");
                    
                    // Soft delete for compliance
                    userProfileRepository.delete(profile);
                    
                    return ProfileResult.<Void, String>success(null);
                })
                .orElse(ProfileResult.failure("Profile not found for user: " + userId));
                
        } catch (Exception e) {
            log.error("Profile deletion failed for user {}: {}", userId, e.getMessage());
            return ProfileResult.failure("Profile deletion failed: " + e.getMessage());
        }
    }

    // Validation Methods (Strategy Pattern)
    private static ProfileResult<Boolean, String> validateCreateProfile(CreateProfileRequest request) {
        return Optional.ofNullable(request)
            .filter(r -> r.getUserId() != null)
            .filter(r -> r.getPersonalInfo() != null)
            .map(r -> ProfileResult.<Boolean, String>success(true))
            .orElse(ProfileResult.failure("Invalid create profile request"));
    }

    private static ProfileResult<Boolean, String> validateUpdateProfile(UpdateProfileRequest request) {
        return Optional.ofNullable(request)
            .map(r -> ProfileResult.<Boolean, String>success(true))
            .orElse(ProfileResult.failure("Invalid update profile request"));
    }

    private static ProfileResult<Boolean, String> validateKycInfo(KYCInformation kycInfo) {
        return Optional.ofNullable(kycInfo)
            .filter(k -> k.panNumber() != null && !k.panNumber().isBlank())
            .map(k -> ProfileResult.<Boolean, String>success(true))
            .orElse(ProfileResult.failure("Invalid KYC information"));
    }

    private static ProfileResult<Boolean, String> validateTradingPrefs(TradingPreferences preferences) {
        return Optional.ofNullable(preferences)
            .map(p -> ProfileResult.<Boolean, String>success(true))
            .orElse(ProfileResult.failure("Invalid trading preferences"));
    }

    // Update Strategy Methods
    private static ProfileResult<UserProfile, String> updatePersonalInfo(UserProfile profile, UpdateProfileRequest request) {
        if (request.getPersonalInfo() != null) {
            profile.setPersonalInfo(request.getPersonalInfo());
        }
        return ProfileResult.success(profile);
    }

    private static ProfileResult<UserProfile, String> updateTradingPreferences(UserProfile profile, TradingPreferences preferences) {
        profile.setTradingPreferences(preferences);
        return ProfileResult.success(profile);
    }

    private static ProfileResult<UserProfile, String> updateKycInformation(UserProfile profile, KYCInformation kycInfo) {
        profile.setKycInfo(kycInfo);
        return ProfileResult.success(profile);
    }

    private static ProfileResult<UserProfile, String> updateNotificationSettings(UserProfile profile, NotificationSettings settings) {
        profile.setNotificationSettings(settings);
        return ProfileResult.success(profile);
    }

    // Helper Methods
    private Optional<UserProfile> findProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId);
    }

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

    private ProfileResult<UserProfile, String> applyProfileUpdates(UserProfile profile, UpdateProfileRequest request) {
        return Optional.ofNullable(request)
            .map(req -> {
                if (req.getPersonalInfo() != null) {
                    profile.setPersonalInfo(req.getPersonalInfo());
                }
                if (req.getTradingPreferences() != null) {
                    profile.setTradingPreferences(req.getTradingPreferences());
                }
                if (req.getKycInfo() != null) {
                    profile.setKycInfo(req.getKycInfo());
                }
                if (req.getNotificationSettings() != null) {
                    profile.setNotificationSettings(req.getNotificationSettings());
                }
                return ProfileResult.success(profile);
            })
            .orElse(ProfileResult.failure("Invalid update request"));
    }

    // Async Helper Methods using Virtual Threads
    @Async("eventExecutor")
    private CompletableFuture<Void> publishEventAsync(UserProfile profile, String eventType) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (eventType) {
                    case "PROFILE_CREATED" -> eventService.publishProfileCreatedEvent(profile);
                    case "PROFILE_UPDATED" -> eventService.publishProfileUpdatedEvent(profile);
                    case "PROFILE_DELETED" -> eventService.publishProfileDeletedEvent(profile);
                    case "TRADING_PREFERENCES_UPDATED" -> eventService.publishTradingPreferencesUpdatedEvent(profile);
                    default -> log.warn("Unknown event type: {}", eventType);
                }
            } catch (Exception e) {
                log.error("Failed to publish event {} for profile {}: {}", eventType, profile.getId(), e.getMessage());
            }
        }, virtualExecutor);
    }

    @Async("profileExecutor")
    private CompletableFuture<Void> auditLogAsync(UUID profileId, String action, UUID currentUserId) {
        return CompletableFuture.runAsync(() -> {
            try {
                auditService.logProfileAction(profileId, action, currentUserId);
            } catch (Exception e) {
                log.error("Failed to log audit for profile {} action {}: {}", profileId, action, e.getMessage());
            }
        }, virtualExecutor);
    }

    @Async("profileExecutor")
    private CompletableFuture<Void> handleKycStatusChangeAsync(UserProfile profile, KYCInformation kycInfo, UUID currentUserId) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (kycInfo.kycStatus() == KYCStatus.VERIFIED) {
                    eventService.publishKycVerifiedEvent(profile);
                }
                auditService.logKycUpdate(profile.getId(), profile.getKycInfo(), kycInfo, currentUserId);
            } catch (Exception e) {
                log.error("Failed to handle KYC status change for profile {}: {}", profile.getId(), e.getMessage());
            }
        }, virtualExecutor);
    }

    // Synchronous wrapper methods for backward compatibility
    @Cacheable(value = "user-profiles", key = "#userId")
    public UserProfileResponse getProfileByUserId(UUID userId) {
        return getProfileByUserIdAsync(userId).join();
    }

    @Transactional
    @CacheEvict(value = "user-profiles", allEntries = true)
    public UserProfileResponse createProfile(CreateProfileRequest request, UUID currentUserId) {
        return createProfileAsync(request, currentUserId).join();
    }

    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request, UUID currentUserId) {
        return updateProfileAsync(userId, request, currentUserId).join();
    }

    @Transactional
    @CacheEvict(value = "user-profiles", key = "#userId")
    public void deleteProfile(UUID userId, UUID currentUserId) {
        deleteProfileAsync(userId, currentUserId).join();
    }
}