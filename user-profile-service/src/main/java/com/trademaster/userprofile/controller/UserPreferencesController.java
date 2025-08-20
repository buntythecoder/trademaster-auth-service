package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.dto.UserPreferencesDto;
import com.trademaster.userprofile.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for User Preferences management
 */
@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User Preferences", description = "User preferences and settings management")
@SecurityRequirement(name = "bearerAuth")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @Operation(summary = "Get user preferences", description = "Retrieve user preferences by user profile ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User profile not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userProfileId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreferencesDto> getUserPreferences(
            @Parameter(description = "User profile ID", required = true)
            @PathVariable UUID userProfileId) {
        log.debug("GET request to retrieve preferences for user profile ID: {}", userProfileId);
        
        UserPreferencesDto preferences = userPreferencesService.getUserPreferences(userProfileId);
        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Get user preferences by email", description = "Retrieve user preferences by email address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/email/{email}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreferencesDto> getUserPreferencesByEmail(
            @Parameter(description = "User email address", required = true)
            @PathVariable String email) {
        log.debug("GET request to retrieve preferences for user email: {}", email);
        
        UserPreferencesDto preferences = userPreferencesService.getUserPreferencesByEmail(email);
        return ResponseEntity.ok(preferences);
    }

    @Operation(summary = "Create or update user preferences", description = "Create new or update existing user preferences")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "201", description = "Preferences created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User profile not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/user/{userProfileId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreferencesDto> saveUserPreferences(
            @Parameter(description = "User profile ID", required = true)
            @PathVariable UUID userProfileId,
            @Parameter(description = "User preferences data", required = true)
            @Valid @RequestBody UserPreferencesDto preferencesDto) {
        log.debug("PUT request to save preferences for user profile ID: {}", userProfileId);
        
        UserPreferencesDto savedPreferences = userPreferencesService.saveUserPreferences(userProfileId, preferencesDto);
        return ResponseEntity.ok(savedPreferences);
    }

    @Operation(summary = "Update theme preference", description = "Quick endpoint to update only the theme preference")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Theme preference updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid theme value"),
        @ApiResponse(responseCode = "404", description = "User profile not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PatchMapping("/user/{userProfileId}/theme/{theme}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreferencesDto> updateThemePreference(
            @Parameter(description = "User profile ID", required = true)
            @PathVariable UUID userProfileId,
            @Parameter(description = "Theme preference (light, dark, auto)", required = true)
            @PathVariable String theme) {
        log.debug("PATCH request to update theme to '{}' for user profile ID: {}", theme, userProfileId);
        
        // Validate theme value
        if (!theme.matches("^(light|dark|auto)$")) {
            throw new IllegalArgumentException("Theme must be one of: light, dark, auto");
        }
        
        UserPreferencesDto updatedPreferences = userPreferencesService.updateThemePreferences(userProfileId, theme);
        return ResponseEntity.ok(updatedPreferences);
    }
}