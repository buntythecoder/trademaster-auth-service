package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.dto.*;
import com.trademaster.userprofile.entity.*;
import com.trademaster.userprofile.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile Management", description = "APIs for managing user profiles, KYC, and trading preferences")
@SecurityRequirement(name = "Bearer Authentication")
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    
    @Operation(
        summary = "Get current user's profile",
        description = "Retrieve the complete profile information for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Retrieving profile for user: {}", userId);
        
        UserProfileResponse response = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Create user profile",
        description = "Create a new user profile with personal information, trading preferences, and KYC details"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profile created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Profile already exists", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        log.info("Creating profile for user: {}", request.getUserId());
        
        // Ensure user can only create their own profile
        if (!request.getUserId().equals(currentUserId)) {
            throw new SecurityException("Cannot create profile for another user");
        }
        
        UserProfileResponse response = userProfileService.createProfile(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "Update user profile",
        description = "Update existing user profile information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Updating profile for user: {}", userId);
        
        UserProfileResponse response = userProfileService.updateProfile(userId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Update trading preferences",
        description = "Update user's trading preferences and risk profile"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trading preferences updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class)))
    })
    @PatchMapping("/me/trading-preferences")
    public ResponseEntity<UserProfileResponse> updateTradingPreferences(
            @Valid @RequestBody TradingPreferences preferences,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Updating trading preferences for user: {}", userId);
        
        UserProfileResponse response = userProfileService.updateTradingPreferences(userId, preferences, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Update KYC information",
        description = "Update user's KYC (Know Your Customer) information and verification status"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "KYC information updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class)))
    })
    @PatchMapping("/me/kyc")
    public ResponseEntity<UserProfileResponse> updateKycInformation(
            @Valid @RequestBody KYCInformation kycInfo,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Updating KYC information for user: {}", userId);
        
        UserProfileResponse response = userProfileService.updateKycInformation(userId, kycInfo, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Update notification settings",
        description = "Update user's notification preferences for various channels"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification settings updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class)))
    })
    @PatchMapping("/me/notifications")
    public ResponseEntity<UserProfileResponse> updateNotificationSettings(
            @Valid @RequestBody NotificationSettings settings,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Updating notification settings for user: {}", userId);
        
        UserProfileResponse response = userProfileService.updateNotificationSettings(userId, settings, userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Delete user profile",
        description = "Delete (archive) the user's profile - this action may be subject to regulatory retention requirements"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = com.trademaster.userprofile.exception.ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Deleting profile for user: {}", userId);
        
        userProfileService.deleteProfile(userId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
        summary = "Check if profile exists",
        description = "Check whether the current user has a profile created"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile existence checked"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content())
    })
    @GetMapping("/me/exists")
    public ResponseEntity<ProfileExistenceResponse> checkProfileExists(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        
        boolean exists = userProfileService.profileExists(userId);
        ProfileExistenceResponse response = new ProfileExistenceResponse(exists);
        
        return ResponseEntity.ok(response);
    }
    
    // Admin endpoints (would be secured with admin role)
    
    @Operation(
        summary = "Search profiles (Admin)",
        description = "Search user profiles with various criteria - Admin only"
    )
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> searchProfiles(
            @Parameter(description = "Name to search for") @RequestParam(required = false) String name,
            @Parameter(description = "Email to search for") @RequestParam(required = false) String email,
            @Parameter(description = "Mobile number to search for") @RequestParam(required = false) String mobile,
            @Parameter(description = "PAN number to search for") @RequestParam(required = false) String panNumber,
            @Parameter(description = "KYC status to filter by") @RequestParam(required = false) String kycStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching profiles with criteria: name={}, email={}, mobile={}, pan={}, kyc={}", 
                name, email, mobile, panNumber, kycStatus);
        
        Page<UserProfileResponse> profiles = userProfileService.searchProfiles(name, email, mobile, panNumber, kycStatus, pageable);
        return ResponseEntity.ok(profiles);
    }
    
    @Operation(
        summary = "Get profiles by KYC status (Admin)",
        description = "Retrieve profiles filtered by KYC verification status - Admin only"
    )
    @GetMapping("/by-kyc-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getProfilesByKycStatus(
            @Parameter(description = "KYC status", example = "VERIFIED") @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Retrieving profiles with KYC status: {}", status);
        
        Page<UserProfileResponse> profiles = userProfileService.getProfilesByKycStatus(status, pageable);
        return ResponseEntity.ok(profiles);
    }
    
    @Operation(
        summary = "Get profile statistics (Admin)",
        description = "Get overall profile statistics including completion rates - Admin only"
    )
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ProfileStatistics> getProfileStatistics() {
        log.info("Retrieving profile statistics");
        
        ProfileStatistics stats = userProfileService.getProfileStatistics();
        return ResponseEntity.ok(stats);
    }
    
    @Operation(
        summary = "Get profiles needing KYC renewal (Admin)",
        description = "Get list of profiles that need KYC renewal - Admin only"
    )
    @GetMapping("/kyc-renewal-needed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<UserProfileResponse>> getProfilesNeedingKycRenewal() {
        log.info("Retrieving profiles needing KYC renewal");
        
        List<UserProfileResponse> profiles = userProfileService.getProfilesNeedingKycRenewal();
        return ResponseEntity.ok(profiles);
    }
    
    @Operation(
        summary = "Get user profile by ID (Admin)",
        description = "Retrieve any user's profile by ID - Admin only"
    )
    @GetMapping("/{profileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT_AGENT')")
    public ResponseEntity<UserProfileResponse> getProfileById(
            @Parameter(description = "Profile UUID") @PathVariable UUID profileId) {
        
        log.info("Admin retrieving profile by ID: {}", profileId);
        
        UserProfileResponse response = userProfileService.getProfileById(profileId);
        return ResponseEntity.ok(response);
    }
}

// Helper DTOs
record ProfileExistenceResponse(boolean exists) {}