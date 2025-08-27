package com.trademaster.agentos.controller;

import com.trademaster.agentos.service.AgentOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Orchestration Controller
 * 
 * REST API endpoints for system-wide orchestration metrics,
 * monitoring, and management operations.
 */
@RestController
@RequestMapping("/api/v1/orchestration")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrchestrationController {

    private final AgentOrchestrationService orchestrationService;

    /**
     * Get comprehensive orchestration metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<AgentOrchestrationService.OrchestrationMetrics> getOrchestrationMetrics() {
        log.debug("REST: Getting orchestration metrics");
        
        try {
            AgentOrchestrationService.OrchestrationMetrics metrics = orchestrationService.getOrchestrationMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting orchestration metrics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthStatus> getSystemHealth() {
        log.debug("REST: Getting system health status");
        
        try {
            AgentOrchestrationService.OrchestrationMetrics metrics = orchestrationService.getOrchestrationMetrics();
            
            // Determine system health based on metrics
            SystemHealthStatus health = SystemHealthStatus.builder()
                    .status(determineSystemStatus(metrics))
                    .totalAgents(metrics.getTotalAgents())
                    .activeAgents(metrics.getActiveAgents())
                    .errorAgents(metrics.getErrorAgents())
                    .systemUtilization(metrics.getSystemUtilization())
                    .pendingTasks(metrics.getPendingTasks())
                    .failedTasks(metrics.getFailedTasks())
                    .averageSuccessRate(metrics.getAverageSuccessRate())
                    .build();
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting system health", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Trigger manual health check
     */
    @PostMapping("/health-check")
    public ResponseEntity<Void> triggerHealthCheck() {
        log.info("REST: Triggering manual health check");
        
        try {
            // Health check is performed by scheduled tasks
            // This endpoint could trigger immediate health check if needed
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error triggering health check", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get system configuration info
     */
    @GetMapping("/info")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        log.debug("REST: Getting system info");
        
        try {
            SystemInfo info = SystemInfo.builder()
                    .serviceName("TradeMaster Agent Orchestration Service")
                    .version("1.0.0")
                    .javaVersion(System.getProperty("java.version"))
                    .springBootVersion(getClass().getPackage().getImplementationVersion())
                    .uptime(getSystemUptime())
                    .build();
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error getting system info", e);
            return ResponseEntity.status(500).build();
        }
    }

    // Helper Methods

    /**
     * Determine overall system status based on metrics
     */
    private String determineSystemStatus(AgentOrchestrationService.OrchestrationMetrics metrics) {
        if (metrics.getErrorAgents() > 0 || metrics.getFailedTasks() > metrics.getCompletedTasks()) {
            return "DEGRADED";
        } else if (metrics.getActiveAgents() == 0) {
            return "DOWN";
        } else if (metrics.getSystemUtilization() > 90) {
            return "HIGH_LOAD";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * Get system uptime in milliseconds
     */
    private Long getSystemUptime() {
        return System.currentTimeMillis() - 
               java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    // Helper Classes

    @lombok.Data
    @lombok.Builder
    public static class SystemHealthStatus {
        private String status;
        private Long totalAgents;
        private Long activeAgents;
        private Long errorAgents;
        private Double systemUtilization;
        private Long pendingTasks;
        private Long failedTasks;
        private Double averageSuccessRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class SystemInfo {
        private String serviceName;
        private String version;
        private String javaVersion;
        private String springBootVersion;
        private Long uptime;
    }
}