package com.trademaster.auth.controller;

import com.trademaster.auth.dto.SecurityAuditResponse;
import com.trademaster.auth.entity.SecurityAuditLog;
import com.trademaster.auth.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;

import com.trademaster.auth.constants.AuthConstants;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Security Audit", description = "Security audit logs and monitoring endpoints")
public class SecurityAuditController {

    private final SecurityAuditService securityAuditService;

    @GetMapping("/security-events")
    @Operation(summary = "Get User Security Events", description = "Get security audit logs for the authenticated user")
    public ResponseEntity<Page<SecurityAuditResponse>> getUserSecurityEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "" + AuthConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "" + AuthConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = userDetails.getUsername();
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        List<SecurityAuditLog> auditLogsList = securityAuditService.getUserAuditLogs(userId, pageable);
        
        // Convert List to Page manually (simplified for compilation fix)
        Page<SecurityAuditLog> auditLogs = new PageImpl<>(auditLogsList, pageable, auditLogsList.size());
        
        Page<SecurityAuditResponse> response = auditLogs.map(SecurityAuditResponse::fromEntity);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/security-events/high-risk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Get High Risk Events", description = "Get recent high-risk security events (Admin/Compliance only)")
    public ResponseEntity<List<SecurityAuditResponse>> getHighRiskEvents(
            @RequestParam(defaultValue = "" + AuthConstants.DEFAULT_AUDIT_HOURS) int hours) {
        
        List<SecurityAuditLog> highRiskEvents = securityAuditService.getRecentHighRiskEvents(hours);
        
        List<SecurityAuditResponse> response = highRiskEvents.stream()
                .map(SecurityAuditResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/security-metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Get Security Metrics", description = "Get security metrics summary (Admin/Compliance only)")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics(
            @RequestParam(defaultValue = "" + AuthConstants.DEFAULT_METRICS_DAYS) int days) {
        
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(days);
        Map<String, Object> metrics = securityAuditService.getSecurityMetrics("ALL", from, to);
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/security-events/export")
    @Operation(summary = "Export Security Events", description = "Export security audit logs for compliance")
    public ResponseEntity<Map<String, Object>> exportSecurityEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String eventType) {
        
        String userIdString = userDetails.getUsername();
        Long userId = Long.valueOf(userIdString);
        
        try {
            // Parse date strings to LocalDateTime
            LocalDateTime startDateTime = startDate != null ? LocalDateTime.parse(startDate + AuthConstants.TIME_START_SUFFIX) : LocalDateTime.now().minusDays(AuthConstants.DEFAULT_METRICS_DAYS);
            LocalDateTime endDateTime = endDate != null ? LocalDateTime.parse(endDate + AuthConstants.TIME_END_SUFFIX) : LocalDateTime.now();
            
            // Service method returns CompletableFuture, so we need to handle it
            var futureResult = securityAuditService.exportSecurityEvents(
                startDateTime, endDateTime, eventType, userId);
            
            var result = futureResult.join(); // Block for simplicity (should use async handling in production)
            
            if (result.isSuccess()) {
                String exportData = result.getValue();
                
                return ResponseEntity.ok()
                        .header("Content-Type", "application/csv")
                        .header("Content-Disposition", "attachment; filename=security-audit-" + 
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".csv")
                        .body(Map.of(
                            "data", exportData,
                            "exportedBy", userId,
                            "exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            "parameters", Map.of(
                                    "startDate", startDate != null ? startDate : "all-time",
                                    "endDate", endDate != null ? endDate : "current",
                                    "eventType", eventType != null ? eventType : "all"
                            )
                        ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to export security events", "details", result.getError()));
            }
        } catch (Exception e) {
            log.error("Failed to export security events", 
                StructuredArguments.kv("userId", userId),
                StructuredArguments.kv("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Export failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/security-dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Get Security Dashboard Data", description = "Get comprehensive security dashboard data")
    public ResponseEntity<Map<String, Object>> getSecurityDashboard() {
        
        // Get recent metrics and high-risk events
        LocalDateTime metricsTo = LocalDateTime.now();
        LocalDateTime metricsFrom = metricsTo.minusDays(AuthConstants.DEFAULT_DASHBOARD_DAYS);
        Map<String, Object> metrics = securityAuditService.getSecurityMetrics("ALL", metricsFrom, metricsTo);
        List<SecurityAuditLog> recentHighRisk = securityAuditService.getRecentHighRiskEvents(AuthConstants.DEFAULT_HIGH_RISK_HOURS);
        
        return ResponseEntity.ok(Map.of(
                "weeklyMetrics", metrics,
                "recentHighRiskEvents", recentHighRisk.stream()
                        .map(SecurityAuditResponse::fromEntity)
                        .toList(),
                "dashboardGeneratedAt", java.time.LocalDateTime.now(),
                "summary", Map.of(
                        "totalHighRiskEvents24h", recentHighRisk.size(),
                        "weeklyEventCount", metrics.get("total_events")
                )
        ));
    }
}