package com.trademaster.userprofile.controller;

import com.trademaster.userprofile.entity.ChangeType;
import com.trademaster.userprofile.entity.EntityType;
import com.trademaster.userprofile.service.ProfileAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simplified Profile Audit Controller
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: Structured Logging & Monitoring - Rule #15
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class ProfileAuditController {
    
    private final ProfileAuditService profileAuditService;
    
    /**
     * Get audit history for a user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (#userId == authentication.name)")
    public ResponseEntity<?> getUserAuditHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting audit history for user: {} [correlation: {}]", userId, correlationId);
        
        return profileAuditService.findByUserProfile(userId, Optional.empty(), Optional.empty(), Optional.empty())
            .fold(
                auditLogs -> {
                    log.debug("Retrieved {} audit logs for user: {}", auditLogs.size(), userId);
                    return ResponseEntity.ok(Map.of(
                        "auditLogs", auditLogs,
                        "count", auditLogs.size(),
                        "correlationId", correlationId
                    ));
                },
                error -> {
                    log.error("Failed to retrieve audit history for user: {} - {}", userId, error);
                    return ResponseEntity.internalServerError().body(Map.of(
                        "error", "Failed to retrieve audit history",
                        "correlationId", correlationId
                    ));
                }
            );
    }
    
    /**
     * Get recent activity summary
     */
    @GetMapping("/user/{userId}/summary")
    @PreAuthorize("hasRole('ADMIN') or (#userId == authentication.name)")
    public ResponseEntity<?> getActivitySummary(
            @PathVariable UUID userId,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting activity summary for user: {} [correlation: {}]", userId, correlationId);
        
        return profileAuditService.getActivitySummary(userId)
            .fold(
                summary -> {
                    log.debug("Retrieved activity summary for user: {}", userId);
                    return ResponseEntity.ok(Map.of(
                        "summary", summary,
                        "correlationId", correlationId
                    ));
                },
                error -> {
                    log.error("Failed to retrieve activity summary for user: {} - {}", userId, error);
                    return ResponseEntity.internalServerError().body(Map.of(
                        "error", "Failed to retrieve activity summary",
                        "correlationId", correlationId
                    ));
                }
            );
    }
    
    /**
     * Get recent activity
     */
    @GetMapping("/user/{userId}/recent")
    @PreAuthorize("hasRole('ADMIN') or (#userId == authentication.name)")
    public ResponseEntity<?> getRecentActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "24") int hours,
            Authentication authentication,
            HttpServletRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Getting recent activity for user: {} (last {} hours) [correlation: {}]", 
                userId, hours, correlationId);
        
        return profileAuditService.findRecentActivity(userId, hours)
            .fold(
                activities -> {
                    log.debug("Retrieved {} recent activities for user: {}", activities.size(), userId);
                    return ResponseEntity.ok(Map.of(
                        "activities", activities,
                        "count", activities.size(),
                        "hours", hours,
                        "correlationId", correlationId
                    ));
                },
                error -> {
                    log.error("Failed to retrieve recent activity for user: {} - {}", userId, error);
                    return ResponseEntity.internalServerError().body(Map.of(
                        "error", "Failed to retrieve recent activity",
                        "correlationId", correlationId
                    ));
                }
            );
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ProfileAuditService",
            "timestamp", java.time.Instant.now()
        ));
    }
}