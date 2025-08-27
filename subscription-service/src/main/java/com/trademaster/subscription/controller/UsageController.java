package com.trademaster.subscription.controller;

import com.trademaster.subscription.dto.UsageCheckRequest;
import com.trademaster.subscription.dto.UsageCheckResponse;
import com.trademaster.subscription.dto.UsageIncrementRequest;
import com.trademaster.subscription.dto.UsageStatsResponse;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.service.UsageTrackingService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Usage Tracking Controller
 * 
 * Provides REST endpoints for usage tracking and limit enforcement.
 * All operations use Virtual Threads for optimal performance.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usage Tracking", description = "Usage tracking and limit enforcement operations")
public class UsageController {

    private final UsageTrackingService usageTrackingService;

    /**
     * Check if user has access to a feature
     */
    @PostMapping("/check")
    @Timed(value = "usage.check", description = "Time taken to check usage access")
    @Operation(
        summary = "Check feature access",
        description = "Checks if a user has access to a specific feature based on their subscription limits"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Access check completed",
            content = @Content(schema = @Schema(implementation = UsageCheckResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<UsageCheckResponse>> checkFeatureAccess(
            @Valid @RequestBody UsageCheckRequest request) {
        
        log.info("Checking feature access for user: {}, feature: {}", 
                request.getUserId(), request.getFeatureName());
        
        return usageTrackingService.checkFeatureAccess(request.getUserId(), request.getFeatureName())
            .thenApply(hasAccess -> {
                UsageCheckResponse response = UsageCheckResponse.builder()
                    .userId(request.getUserId())
                    .featureName(request.getFeatureName())
                    .hasAccess(hasAccess)
                    .checkTime(java.time.LocalDateTime.now())
                    .build();
                    
                return ResponseEntity.ok(response);
            });
    }

    /**
     * Increment usage for a feature
     */
    @PostMapping("/increment")
    @Timed(value = "usage.increment", description = "Time taken to increment usage")
    @Operation(
        summary = "Increment feature usage",
        description = "Increments usage count for a specific feature"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usage incremented successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Usage limit exceeded"),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Void>> incrementUsage(
            @Valid @RequestBody UsageIncrementRequest request) {
        
        log.info("Incrementing usage for user: {}, feature: {}, amount: {}", 
                request.getUserId(), request.getFeatureName(), request.getIncrementBy());
        
        return usageTrackingService.incrementUsage(
            request.getUserId(), 
            request.getFeatureName(), 
            request.getIncrementBy()
        ).thenApply(result -> ResponseEntity.ok().build());
    }

    /**
     * Validate and increment usage atomically
     */
    @PostMapping("/validate-increment")
    @Timed(value = "usage.validate.increment", description = "Time taken to validate and increment usage")
    @Operation(
        summary = "Validate and increment usage",
        description = "Atomically validates access and increments usage for a feature"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usage validated and incremented"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Usage limit would be exceeded"),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Void>> validateAndIncrementUsage(
            @Valid @RequestBody UsageCheckRequest request) {
        
        log.info("Validating and incrementing usage for user: {}, feature: {}", 
                request.getUserId(), request.getFeatureName());
        
        return usageTrackingService.validateAndIncrementUsage(
            request.getUserId(), 
            request.getFeatureName()
        ).thenApply(result -> ResponseEntity.ok().build());
    }

    /**
     * Get current usage for a feature
     */
    @GetMapping("/users/{userId}/features/{featureName}/current")
    @Timed(value = "usage.get.current", description = "Time taken to get current usage")
    @Operation(
        summary = "Get current feature usage",
        description = "Retrieves current usage count for a specific feature"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Current usage retrieved",
            content = @Content(schema = @Schema(implementation = Long.class))
        ),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Long>> getCurrentUsage(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Feature name") @PathVariable String featureName) {
        
        log.info("Getting current usage for user: {}, feature: {}", userId, featureName);
        
        return usageTrackingService.getCurrentUsage(userId, featureName)
            .thenApply(usage -> ResponseEntity.ok(usage));
    }

    /**
     * Get usage statistics for a user
     */
    @GetMapping("/users/{userId}/stats")
    @Timed(value = "usage.get.stats", description = "Time taken to get usage statistics")
    @Operation(
        summary = "Get user usage statistics",
        description = "Retrieves usage statistics for a user for a specific month"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usage statistics retrieved",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<UsageStatsResponse>>> getUserUsageStats(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Month (YYYY-MM-DD)") 
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        
        LocalDate targetMonth = month != null ? month : LocalDate.now();
        log.info("Getting usage stats for user: {}, month: {}", userId, targetMonth);
        
        return usageTrackingService.getUserUsageStats(userId, targetMonth)
            .thenApply(usageList -> {
                List<UsageStatsResponse> responses = usageList.stream()
                    .map(usage -> UsageStatsResponse.builder()
                        .userId(usage.getUserId())
                        .featureName(usage.getFeatureName())
                        .usageCount(usage.getUsageCount())
                        .usagePeriod(usage.getUsagePeriod())
                        .lastUpdated(usage.getLastUpdated())
                        .build())
                    .toList();
                    
                return ResponseEntity.ok(responses);
            });
    }

    /**
     * Get comprehensive usage report for a user
     */
    @GetMapping("/users/{userId}/report")
    @Timed(value = "usage.get.report", description = "Time taken to get usage report")
    @Operation(
        summary = "Get comprehensive usage report",
        description = "Retrieves comprehensive usage report for a user across all periods"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usage report retrieved",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(responseCode = "404", description = "User subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<UsageStatsResponse>>> getComprehensiveUsageReport(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        log.info("Getting comprehensive usage report for user: {}", userId);
        
        return usageTrackingService.getComprehensiveUsageReport(userId)
            .thenApply(usageList -> {
                List<UsageStatsResponse> responses = usageList.stream()
                    .map(usage -> UsageStatsResponse.builder()
                        .userId(usage.getUserId())
                        .featureName(usage.getFeatureName())
                        .usageCount(usage.getUsageCount())
                        .usagePeriod(usage.getUsagePeriod())
                        .lastUpdated(usage.getLastUpdated())
                        .build())
                    .toList();
                    
                return ResponseEntity.ok(responses);
            });
    }

    /**
     * Reset monthly usage (Admin endpoint)
     */
    @PostMapping("/admin/reset-monthly")
    @Timed(value = "usage.reset.monthly", description = "Time taken to reset monthly usage")
    @Operation(
        summary = "Reset monthly usage",
        description = "Resets usage for all users for a specific month (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Monthly usage reset completed"),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Void>> resetMonthlyUsage(
            @Parameter(description = "Period to reset (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period) {
        
        log.info("Admin requesting monthly usage reset for period: {}", period);
        
        return usageTrackingService.resetMonthlyUsage(period)
            .thenApply(result -> ResponseEntity.ok().build());
    }

    /**
     * Health check for usage tracking service
     */
    @GetMapping("/health")
    @Timed(value = "usage.health", description = "Time taken for usage service health check")
    @Operation(
        summary = "Usage service health check",
        description = "Performs health check on usage tracking service"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy"),
        @ApiResponse(responseCode = "503", description = "Service is unavailable")
    })
    public ResponseEntity<Void> healthCheck() {
        // Simple health check - can be expanded with actual service checks
        return ResponseEntity.ok().build();
    }
}