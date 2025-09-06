package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.common.Result;
import com.trademaster.userprofile.entity.UserPreferences;
import com.trademaster.userprofile.service.UserPreferencesService;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Functional User Preferences REST Controller
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
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@CrossOrigin(origins = "${trademaster.cors.allowed-origins:http://localhost:3000}")
public class UserPreferencesController {
    
    private final UserPreferencesService userPreferencesService;
    private final ProfileAuditService profileAuditService;
    
    // ========== QUERY OPERATIONS ==========
    
    /**
     * Get user preferences with functional error handling
     */
    @GetMapping("/user/{userProfileId}")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> getUserPreferences(
            @PathVariable UUID userProfileId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting preferences for user profile: {} [correlation: {}]", userProfileId, correlationId);
        
        return userPreferencesService.getPreferencesWithDefaults(userProfileId)
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("PREFERENCES_VIEW", userProfileId, authentication, request, correlationId);
                    return ResponseEntity.ok(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    /**
     * Get users by theme preference (admin only)
     */
    @GetMapping("/theme/{theme}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByTheme(@PathVariable String theme) {
        
        return userPreferencesService.findByTheme(theme)
            .map(preferences -> preferences.stream().map(this::mapToPreferencesResponse).toList())
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get users with notifications enabled (admin only)
     */
    @GetMapping("/notifications/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersWithNotifications() {
        
        return userPreferencesService.findUsersWithNotifications()
            .map(preferences -> Map.of(
                "users", preferences.stream().map(this::mapToPreferencesResponse).toList(),
                "count", preferences.size()
            ))
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get users with trading alerts enabled (admin only)
     */
    @GetMapping("/alerts/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersWithTradingAlerts() {
        
        return userPreferencesService.findUsersWithTradingAlerts()
            .map(preferences -> Map.of(
                "users", preferences.stream().map(this::mapToPreferencesResponse).toList(),
                "count", preferences.size()
            ))
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    /**
     * Get high security users (admin only)
     */
    @GetMapping("/security/high")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getHighSecurityUsers() {
        
        return userPreferencesService.findHighSecurityUsers()
            .map(preferences -> Map.of(
                "users", preferences.stream().map(this::mapToPreferencesResponse).toList(),
                "count", preferences.size()
            ))
            .fold(
                ResponseEntity::ok,
                error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
            );
    }
    
    // ========== COMMAND OPERATIONS ==========
    
    /**
     * Create preferences with defaults
     */
    @PostMapping("/user/{userProfileId}")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> createPreferences(
            @PathVariable UUID userProfileId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Creating preferences for user profile: {} [correlation: {}]", userProfileId, correlationId);
        
        return userPreferencesService.createPreferences(userProfileId)
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("PREFERENCES_CREATE", userProfileId, authentication, request, correlationId);
                    return ResponseEntity.status(201).body(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    /**
     * Update theme preference
     */
    @PutMapping("/user/{userProfileId}/theme")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updateTheme(
            @PathVariable UUID userProfileId,
            @Valid @RequestBody ThemeUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating theme for user profile: {} to: {} [correlation: {}]", 
            userProfileId, request.theme(), correlationId);
        
        return userPreferencesService.updateTheme(userProfileId, request.theme())
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("THEME_UPDATE", userProfileId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    /**
     * Update notification settings
     */
    @PutMapping("/user/{userProfileId}/notifications")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updateNotificationSettings(
            @PathVariable UUID userProfileId,
            @Valid @RequestBody NotificationUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating notifications for user profile: {} [correlation: {}]", 
            userProfileId, correlationId);
        
        return userPreferencesService.updateNotificationSettings(
                userProfileId,
                request.emailNotifications(),
                request.smsNotifications(),
                request.pushNotifications()
            )
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("NOTIFICATIONS_UPDATE", userProfileId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    /**
     * Update security settings
     */
    @PutMapping("/user/{userProfileId}/security")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updateSecuritySettings(
            @PathVariable UUID userProfileId,
            @Valid @RequestBody SecurityUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating security settings for user profile: {} [correlation: {}]", 
            userProfileId, correlationId);
        
        return userPreferencesService.updateSecuritySettings(
                userProfileId,
                request.twoFactorEnabled(),
                request.sessionTimeout()
            )
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("SECURITY_UPDATE", userProfileId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    /**
     * Update complete preferences
     */
    @PutMapping("/user/{userProfileId}")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public ResponseEntity<?> updatePreferences(
            @PathVariable UUID userProfileId,
            @Valid @RequestBody CompletePreferencesUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Updating complete preferences for user profile: {} [correlation: {}]", 
            userProfileId, correlationId);
        
        return userPreferencesService.updatePreferences(userProfileId, preferences -> 
                applyCompleteUpdate(preferences, request)
            )
            .map(this::mapToPreferencesResponse)
            .fold(
                preferences -> {
                    logAuditEvent("PREFERENCES_UPDATE", userProfileId, authentication, httpRequest, correlationId);
                    return ResponseEntity.ok(preferences);
                },
                error -> handlePreferencesError(error)
            );
    }
    
    // ========== ASYNCHRONOUS OPERATIONS ==========
    
    /**
     * Async preferences retrieval
     */
    @GetMapping("/user/{userProfileId}/async")
    @PreAuthorize("hasRole('USER') and (#userProfileId == authentication.name or hasRole('ADMIN'))")
    public CompletableFuture<ResponseEntity<?>> getUserPreferencesAsync(
            @PathVariable UUID userProfileId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting preferences async for user profile: {} [correlation: {}]", 
            userProfileId, correlationId);
        
        return userPreferencesService.findByUserProfileIdAsync(userProfileId)
            .thenApply(result -> result
                .map(this::mapToPreferencesResponse)
                .fold(
                    preferences -> {
                        logAuditEvent("PREFERENCES_VIEW_ASYNC", userProfileId, authentication, request, correlationId);
                        return ResponseEntity.ok(preferences);
                    },
                    error -> handlePreferencesError(error)
                ));
    }
    
    /**
     * Bulk preferences update (admin only)
     */
    @PostMapping("/bulk-update")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> bulkUpdatePreferences(
            @Valid @RequestBody BulkUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Bulk updating {} user preferences [correlation: {}]", 
            request.userProfileIds().size(), correlationId);
        
        var result = userPreferencesService.bulkUpdatePreferences(
                request.userProfileIds(),
                preferences -> applyBulkUpdates(preferences, request.updates())
            );
            
        return CompletableFuture.completedFuture(
            result
                .map(preferences -> Map.of(
                    "updated", preferences.size(),
                    "requested", request.userProfileIds().size(),
                    "correlationId", correlationId
                ))
                .fold(
                    ResponseEntity::ok,
                    error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
                )
        );
    }
    
    /**
     * Process notification preferences async
     */
    @PostMapping("/process-notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> processNotificationPreferences() {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Processing notification preferences [correlation: {}]", correlationId);
        
        return userPreferencesService.processNotificationPreferences()
            .thenApply(result -> result
                .map(preferences -> Map.of(
                    "processed", preferences.size(),
                    "correlationId", correlationId
                ))
                .fold(
                    ResponseEntity::ok,
                    error -> ResponseEntity.badRequest().body(Map.of("error", error.message()))
                ));
    }
    
    // ========== FUNCTIONAL HELPER METHODS ==========
    
    /**
     * Map UserPreferences to response DTO
     */
    private PreferencesResponse mapToPreferencesResponse(UserPreferences preferences) {
        return new PreferencesResponse(
            preferences.getId(),
            preferences.getUserProfile().getUserId(),
            preferences.getTheme(),
            preferences.getLanguage(),
            preferences.getTimezone(),
            preferences.getCurrency(),
            preferences.getEmailNotifications(),
            preferences.getSmsNotifications(),
            preferences.getPushNotifications(),
            preferences.getTradingAlerts(),
            preferences.getPriceAlerts(),
            preferences.getTwoFactorEnabled(),
            preferences.getSessionTimeout(),
            preferences.getVersion(),
            preferences.getCreatedAt(),
            preferences.getUpdatedAt()
        );
    }
    
    /**
     * Apply complete preferences update functionally
     */
    private UserPreferences applyCompleteUpdate(
            UserPreferences preferences, 
            CompletePreferencesUpdateRequest request) {
        
        UserPreferences updated = preferences;
        
        if (request.theme() != null) {
            updated = updated.withTheme(request.theme());
        }
        if (request.language() != null) {
            updated = updated.withLanguage(request.language());
        }
        if (request.timezone() != null) {
            updated = updated.withTimezone(request.timezone());
        }
        if (request.currency() != null) {
            updated = updated.withCurrency(request.currency());
        }
        if (request.emailNotifications() != null) {
            updated = updated.withEmailNotifications(request.emailNotifications());
        }
        if (request.smsNotifications() != null) {
            updated = updated.withSmsNotifications(request.smsNotifications());
        }
        if (request.pushNotifications() != null) {
            updated = updated.withPushNotifications(request.pushNotifications());
        }
        if (request.tradingAlerts() != null) {
            updated = updated.withTradingAlerts(request.tradingAlerts());
        }
        if (request.priceAlerts() != null) {
            updated = updated.withPriceAlerts(request.priceAlerts());
        }
        if (request.twoFactorEnabled() != null) {
            updated = updated.withTwoFactorEnabled(request.twoFactorEnabled());
        }
        if (request.sessionTimeout() != null) {
            updated = updated.withSessionTimeout(request.sessionTimeout());
        }
        
        return updated;
    }
    
    /**
     * Apply bulk updates functionally
     */
    private UserPreferences applyBulkUpdates(
            UserPreferences preferences,
            Map<String, Object> updates) {
        
        UserPreferences updated = preferences;
        
        if (updates.containsKey("theme")) {
            updated = updated.withTheme((String) updates.get("theme"));
        }
        if (updates.containsKey("language")) {
            updated = updated.withLanguage((String) updates.get("language"));
        }
        if (updates.containsKey("emailNotifications")) {
            updated = updated.withEmailNotifications((Boolean) updates.get("emailNotifications"));
        }
        if (updates.containsKey("tradingAlerts")) {
            updated = updated.withTradingAlerts((Boolean) updates.get("tradingAlerts"));
        }
        
        return updated;
    }
    
    /**
     * Handle preferences errors with functional pattern matching
     */
    private ResponseEntity<?> handlePreferencesError(UserPreferencesService.PreferencesError error) {
        return switch (error) {
            case UserPreferencesService.PreferencesNotFoundError notFound -> 
                ResponseEntity.notFound().build();
            case UserPreferencesService.ValidationError validation -> 
                ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation failed",
                    "message", validation.message(),
                    "details", validation.details()
                ));
            case UserPreferencesService.ConcurrencyError concurrency -> 
                ResponseEntity.status(409).body(Map.of(
                    "error", "Concurrency conflict",
                    "message", concurrency.message()
                ));
            case UserPreferencesService.SystemError system -> 
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
            UUID userProfileId,
            Authentication authentication,
            HttpServletRequest request,
            String correlationId) {
        
        try {
            log.info("Audit event: {} for user profile: {} by: {} [correlation: {}]", 
                eventType, userProfileId, authentication.getName(), correlationId);
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }
    
    // ========== REQUEST/RESPONSE DTOs ==========
    
    /**
     * Theme update request record
     */
    public record ThemeUpdateRequest(
        @NotNull String theme
    ) {}
    
    /**
     * Notification update request record
     */
    public record NotificationUpdateRequest(
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean pushNotifications
    ) {}
    
    /**
     * Security update request record
     */
    public record SecurityUpdateRequest(
        Boolean twoFactorEnabled,
        Integer sessionTimeout
    ) {}
    
    /**
     * Complete preferences update request record
     */
    public record CompletePreferencesUpdateRequest(
        String theme,
        String language,
        String timezone,
        String currency,
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean pushNotifications,
        Boolean tradingAlerts,
        Boolean priceAlerts,
        Boolean twoFactorEnabled,
        Integer sessionTimeout
    ) {}
    
    /**
     * Bulk update request record
     */
    public record BulkUpdateRequest(
        @NotNull List<UUID> userProfileIds,
        @NotNull Map<String, Object> updates
    ) {}
    
    /**
     * Preferences response record
     */
    public record PreferencesResponse(
        UUID id,
        UUID userId,
        String theme,
        String language,
        String timezone,
        String currency,
        Boolean emailNotifications,
        Boolean smsNotifications,
        Boolean pushNotifications,
        Boolean tradingAlerts,
        Boolean priceAlerts,
        Boolean twoFactorEnabled,
        Integer sessionTimeout,
        Integer version,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {}
}