package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.RiskLevel;
import com.trademaster.userprofile.entity.UserProfile;
import com.trademaster.userprofile.service.UserProfileService;
import com.trademaster.userprofile.service.ProfileAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Functional User Profile REST Controller
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Error Handling Patterns - Rule #11
 * MANDATORY: Structured Logging & Monitoring - Rule #15
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "${trademaster.cors.allowed-origins:http://localhost:3000}")
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    private final ProfileAuditService profileAuditService;
    
    // ========== QUERY OPERATIONS ==========
    
    /**
     * Get user profile with functional error handling
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> getProfile(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting profile for user: {} [correlation: {}]", userId, correlationId);
        
        return userProfileService.findByUserId(userId)
            .map(this::mapToProfileResponse)
            .fold(
                profile -> {
                    logAuditEvent("PROFILE_VIEW", userId, authentication, request, correlationId);
                    return ResponseEntity.ok(profile);
                },
                error -> handleProfileError(error)
            );
    }
    
    /**
     * Check if profile exists
     */
    @GetMapping("/{userId}/exists")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> checkProfileExists(@PathVariable UUID userId) {
        
        return userProfileService.existsByUserId(userId)
            .map(exists -> Map.of("exists", exists, "userId", userId))
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get profiles by KYC status (admin only)
     */
    @GetMapping("/kyc-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProfilesByKycStatus(@PathVariable String status) {
        
        return userProfileService.findByKycStatus(status)
            .map(profiles -> profiles.stream().map(this::mapToProfileResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get profiles by risk level (admin only)
     */
    @GetMapping("/risk-level/{riskLevel}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProfilesByRiskLevel(@PathVariable RiskLevel riskLevel) {
        
        return userProfileService.findByRiskLevel(riskLevel)
            .map(profiles -> profiles.stream().map(this::mapToProfileResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Search profiles (admin only)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchProfiles(@RequestParam String term) {
        
        return userProfileService.searchProfiles(term)
            .map(profiles -> profiles.stream().map(this::mapToProfileResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    // ========== COMMAND OPERATIONS ==========
    
    /**
     * Create new profile with functional validation
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createProfile(
            @Valid @RequestBody CreateProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Creating profile for user: {} [correlation: {}]", request.userId(), correlationId);
        
        return userProfileService.createProfile(
                request.userId(),
                request.personalInfo().toMap(),
                request.tradingPreferences().toMap(),
                request.kycInformation().toMap(),
                request.notificationSettings().toMap()
            )
            .map(this::mapToProfileResponse)
            .fold(
                profile -> {
                    logAuditEvent("PROFILE_CREATE", request.userId(), authentication, httpRequest, correlationId);
                    return ResponseEntity.status(201).body(profile);
                },
                error -> handleProfileError(error)
            );
    }
    
    /**
     * Update profile with functional composition
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('USER') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating profile for user: {} [correlation: {}]", userId, correlationId);
        
        return userProfileService.updateProfile(userId, profile -> 
                applyProfileUpdates(profile, request)
            )
            .map(this::mapToProfileResponse)
            .fold(
                profile -> {
                    logAuditEvent("PROFILE_UPDATE", userId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(profile);
                },
                error -> handleProfileError(error)
            );
    }
    
    /**
     * Update KYC information
     */
    @PutMapping("/{userId}/kyc")
    @PreAuthorize("hasRole('USER') and (#userId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updateKycInformation(
            @PathVariable UUID userId,
            @Valid @RequestBody KycUpdateRequest kycInformation,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating KYC for user: {} [correlation: {}]", userId, correlationId);
        
        return userProfileService.updateKycInformation(userId, kycInformation.toMap())
            .map(this::mapToProfileResponse)
            .fold(
                profile -> {
                    logAuditEvent("KYC_UPDATE", userId, authentication, request, correlationId);
                    return ResponseEntity.ok(profile);
                },
                error -> handleProfileError(error)
            );
    }
    
    /**
     * Activate profile
     */
    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateProfile(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Activating profile for user: {} [correlation: {}]", userId, correlationId);
        
        return userProfileService.activateProfile(userId)
            .map(this::mapToProfileResponse)
            .fold(
                profile -> {
                    logAuditEvent("PROFILE_ACTIVATE", userId, authentication, request, correlationId);
                    return ResponseEntity.ok(profile);
                },
                error -> handleProfileError(error)
            );
    }
    
    // ========== ASYNCHRONOUS OPERATIONS ==========
    
    /**
     * Async profile retrieval
     */
    @GetMapping("/{userId}/async")
    @PreAuthorize("hasRole('USER') and (#userId == authentication.name or hasRole('ADMIN'))")
    public CompletableFuture<ResponseEntity<?>> getProfileAsync(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting profile async for user: {} [correlation: {}]", userId, correlationId);
        
        return userProfileService.findByUserIdAsync(userId)
            .thenApply(result -> result
                .map(this::mapToProfileResponse)
                .fold(
                    profile -> {
                        logAuditEvent("PROFILE_VIEW_ASYNC", userId, authentication, request, correlationId);
                        return ResponseEntity.ok(profile);
                    },
                    error -> handleProfileError(error)
                ));
    }
    
    /**
     * Batch update profiles (admin only)
     */
    @PostMapping("/batch-update")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> batchUpdateProfiles(
            @Valid @RequestBody BatchUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Batch updating {} profiles [correlation: {}]", request.userIds().size(), correlationId);
        
        return userProfileService.batchUpdateProfiles(request.userIds(), profile -> 
                applyBatchUpdates(profile, request)
            )
            .thenApply(result -> result
                .map(profiles -> Map.of(
                    "updated", profiles.size(),
                    "requested", request.userIds().size(),
                    "correlationId", correlationId
                ))
                .fold(
                    ResponseEntity::ok,
                    error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
                ));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Map UserProfile to response DTO
     */
    private ProfileResponse mapToProfileResponse(UserProfile profile) {
        return new ProfileResponse(
            profile.getId(),
            profile.getUserId(),
            profile.getPersonalInfo(),
            profile.getTradingPreferences(),
            maskSensitiveKycInfo(profile.getKycInformation()),
            profile.getNotificationSettings(),
            profile.getRiskLevel(),
            profile.isKycCompleted(),
            profile.getVersion(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
    
    /**
     * Apply profile updates functionally
     */
    private UserProfile applyProfileUpdates(UserProfile profile, UpdateProfileRequest request) {
        UserProfile updatedProfile = profile;
        
        if (request.personalInfo() != null) {
            updatedProfile = updatedProfile.withPersonalInfo(request.personalInfo().toMap());
        }
        if (request.tradingPreferences() != null) {
            updatedProfile = updatedProfile.withTradingPreferences(request.tradingPreferences().toMap());
        }
        if (request.notificationSettings() != null) {
            updatedProfile = updatedProfile.withNotificationSettings(request.notificationSettings().toMap());
        }
        
        return updatedProfile;
    }
    
    /**
     * Apply batch updates functionally
     */
    private UserProfile applyBatchUpdates(UserProfile profile, BatchUpdateRequest request) {
        UserProfile updatedProfile = profile;
        
        if (request.tradingPreferences() != null) {
            updatedProfile = updatedProfile.withTradingPreferences(request.tradingPreferences().toMap());
        }
        
        if (request.notificationSettings() != null) {
            updatedProfile = updatedProfile.withNotificationSettings(request.notificationSettings().toMap());
        }
        
        return updatedProfile;
    }
    
    /**
     * Mask sensitive KYC information for response
     */
    private Map<String, Object> maskSensitiveKycInfo(Map<String, Object> kycInfo) {
        if (kycInfo == null) return null;
        
        Map<String, Object> masked = new HashMap<>(kycInfo);
        
        // Mask sensitive fields
        if (masked.containsKey("panNumber")) {
            String pan = (String) masked.get("panNumber");
            masked.put("panNumber", maskString(pan, 3, 2));
        }
        
        if (masked.containsKey("aadhaarNumber")) {
            String aadhaar = (String) masked.get("aadhaarNumber");
            masked.put("aadhaarNumber", maskString(aadhaar, 4, 4));
        }
        
        return masked;
    }
    
    /**
     * Mask string with functional approach
     */
    private String maskString(String input, int prefixLength, int suffixLength) {
        if (input == null || input.length() <= prefixLength + suffixLength) {
            return input;
        }
        
        String prefix = input.substring(0, prefixLength);
        String suffix = input.substring(input.length() - suffixLength);
        String middle = "*".repeat(input.length() - prefixLength - suffixLength);
        
        return prefix + middle + suffix;
    }
    
    /**
     * Handle profile errors with functional pattern matching
     */
    private ResponseEntity<?> handleProfileError(UserProfileService.ProfileError error) {
        return switch (error) {
            case UserProfileService.ProfileNotFoundError notFound -> 
                ResponseEntity.notFound().build();
            case UserProfileService.ValidationError validation -> 
                ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", validation.message(),
                    "details", validation.details()
                ));
            case UserProfileService.ConcurrencyError concurrency -> 
                ResponseEntity.status(409).body(Map.of(
                    "error", "Concurrency conflict",
                    "message", concurrency.message()
                ));
            case UserProfileService.SystemError system -> 
                ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", "Please contact support"
                ));
        };
    }
    
    /**
     * Log audit event with security context
     */
    private void logAuditEvent(
            String eventType,
            UUID userId,
            Authentication authentication,
            HttpServletRequest request,
            String correlationId) {
        
        try {
            log.info("Audit event: {} for user: {} by: {} [correlation: {}]", 
                eventType, userId, authentication.getName(), correlationId);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }
    
    // ========== REQUEST/RESPONSE DTOs ==========
    
    /**
     * Create profile request record
     */
    public record CreateProfileRequest(
        @NotNull UUID userId,
        @NotNull PersonalInfo personalInfo,
        @NotNull TradingPreferences tradingPreferences,
        @NotNull KycInformation kycInformation,
        @NotNull NotificationSettings notificationSettings
    ) {}
    
    /**
     * Update profile request record
     */
    public record UpdateProfileRequest(
        PersonalInfo personalInfo,
        TradingPreferences tradingPreferences,
        NotificationSettings notificationSettings
    ) {}
    
    /**
     * Batch update request record
     */
    public record BatchUpdateRequest(
        @NotNull List<UUID> userIds,
        TradingPreferences tradingPreferences,
        NotificationSettings notificationSettings
    ) {}
    
    /**
     * KYC update request record
     */
    public record KycUpdateRequest(
        String panNumber,
        String aadhaarNumber,
        String addressProof,
        String incomeProof,
        Boolean kycCompleted
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (panNumber != null) map.put("panNumber", panNumber);
            if (aadhaarNumber != null) map.put("aadhaarNumber", aadhaarNumber);
            if (addressProof != null) map.put("addressProof", addressProof);
            if (incomeProof != null) map.put("incomeProof", incomeProof);
            if (kycCompleted != null) map.put("kycCompleted", kycCompleted);
            return Map.copyOf(map);
        }
    }
    
    /**
     * Personal information record
     */
    public record PersonalInfo(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String dateOfBirth,
        String address
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (firstName != null) map.put("firstName", firstName);
            if (lastName != null) map.put("lastName", lastName);
            if (email != null) map.put("email", email);
            if (phoneNumber != null) map.put("phoneNumber", phoneNumber);
            if (dateOfBirth != null) map.put("dateOfBirth", dateOfBirth);
            if (address != null) map.put("address", address);
            return Map.copyOf(map);
        }
    }
    
    /**
     * Trading preferences record
     */
    public record TradingPreferences(
        String riskTolerance,
        List<String> preferredSectors,
        BigDecimal maxPositionSize,
        Boolean autoInvest,
        String tradingStrategy
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (riskTolerance != null) map.put("riskTolerance", riskTolerance);
            if (preferredSectors != null) map.put("preferredSectors", preferredSectors);
            if (maxPositionSize != null) map.put("maxPositionSize", maxPositionSize);
            if (autoInvest != null) map.put("autoInvest", autoInvest);
            if (tradingStrategy != null) map.put("tradingStrategy", tradingStrategy);
            return Map.copyOf(map);
        }
    }
    
    /**
     * KYC information record
     */
    public record KycInformation(
        String panNumber,
        String aadhaarNumber,
        String addressProof,
        String incomeProof,
        String bankAccountNumber,
        String ifscCode
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (panNumber != null) map.put("panNumber", panNumber);
            if (aadhaarNumber != null) map.put("aadhaarNumber", aadhaarNumber);
            if (addressProof != null) map.put("addressProof", addressProof);
            if (incomeProof != null) map.put("incomeProof", incomeProof);
            if (bankAccountNumber != null) map.put("bankAccountNumber", bankAccountNumber);
            if (ifscCode != null) map.put("ifscCode", ifscCode);
            return Map.copyOf(map);
        }
    }
    
    /**
     * Notification settings record
     */
    public record NotificationSettings(
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean pushNotifications,
        List<String> notificationTypes,
        String preferredTime
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (emailNotifications != null) map.put("emailNotifications", emailNotifications);
            if (smsNotifications != null) map.put("smsNotifications", smsNotifications);
            if (pushNotifications != null) map.put("pushNotifications", pushNotifications);
            if (notificationTypes != null) map.put("notificationTypes", notificationTypes);
            if (preferredTime != null) map.put("preferredTime", preferredTime);
            return Map.copyOf(map);
        }
    }
    
    /**
     * Profile response record
     */
    public record ProfileResponse(
        UUID id,
        UUID userId,
        Map<String, Object> personalInfo,
        Map<String, Object> tradingPreferences,
        Map<String, Object> kycInformation,
        Map<String, Object> notificationSettings,
        RiskLevel riskLevel,
        boolean kycCompleted,
        Integer version,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}
}