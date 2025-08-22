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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        String userId = userDetails.getUsername();
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SecurityAuditLog> auditLogs = securityAuditService.getUserAuditLogs(userId, pageable);
        
        Page<SecurityAuditResponse> response = auditLogs.map(SecurityAuditResponse::fromEntity);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/security-events/high-risk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Get High Risk Events", description = "Get recent high-risk security events (Admin/Compliance only)")
    public ResponseEntity<List<SecurityAuditResponse>> getHighRiskEvents(
            @RequestParam(defaultValue = "24") int hours) {
        
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
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> metrics = securityAuditService.getSecurityMetrics(days);
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/security-events/export")
    @Operation(summary = "Export Security Events", description = "Export security audit logs for compliance")
    public ResponseEntity<Map<String, Object>> exportSecurityEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String eventType) {
        
        String userId = userDetails.getUsername();
        
        // In a real implementation, you would generate and return a file download
        // For now, return a summary
        
        return ResponseEntity.ok(Map.of(
                "message", "Export functionality would generate downloadable file",
                "parameters", Map.of(
                        "userId", userId,
                        "startDate", startDate != null ? startDate : "not specified",
                        "endDate", endDate != null ? endDate : "not specified",
                        "eventType", eventType != null ? eventType : "all"
                ),
                "note", "In production, this would return a CSV/PDF file download"
        ));
    }

    @GetMapping("/security-dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    @Operation(summary = "Get Security Dashboard Data", description = "Get comprehensive security dashboard data")
    public ResponseEntity<Map<String, Object>> getSecurityDashboard() {
        
        // Get recent metrics and high-risk events
        Map<String, Object> metrics = securityAuditService.getSecurityMetrics(7);
        List<SecurityAuditLog> recentHighRisk = securityAuditService.getRecentHighRiskEvents(24);
        
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