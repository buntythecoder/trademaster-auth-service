package com.trademaster.userprofile.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * User Profile AgentOS Configuration
 * 
 * Manages the lifecycle, registration, and health monitoring of the User Profile Agent
 * within the TradeMaster AgentOS ecosystem. Handles automatic registration with the
 * Agent Orchestration Service and periodic health reporting.
 * 
 * Lifecycle Management:
 * - Automatic agent registration on startup
 * - Periodic health checks and reporting
 * - Graceful deregistration on shutdown
 * - Capability registry initialization
 * - Performance metrics collection
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class UserProfileAgentOSConfig implements ApplicationRunner {
    
    private final UserProfileCapabilityRegistry capabilityRegistry;
    
    private volatile boolean agentRegistered = false;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private volatile LocalDateTime registrationTime;
    
    /**
     * Initialize User Profile Agent on application startup
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing User Profile Service for AgentOS integration...");
        
        try {
            // Initialize capability registry with user profile capabilities
            capabilityRegistry.initializeCapabilities();
            
            agentRegistered = true;
            registrationTime = LocalDateTime.now();
            
            log.info("User Profile Service successfully initialized");
            
        } catch (Exception e) {
            log.error("Failed to initialize User Profile Service", e);
            throw new RuntimeException("User Profile Service initialization failed", e);
        }
    }
    
    
    /**
     * Periodic health check and metrics reporting
     * Runs every 30 seconds to monitor service health
     */
    @Scheduled(fixedRate = 30000, initialDelay = 30000)
    public void performPeriodicHealthCheck() {
        
        if (!agentRegistered) {
            return;
        }
        
        try {
            var overallHealth = capabilityRegistry.calculateOverallHealthScore();
            var timeSinceRegistration = Duration.between(registrationTime, LocalDateTime.now());
            
            // Log health status
            if (overallHealth >= 0.8) {
                log.debug("User Profile Service health check: HEALTHY (Score: {})", overallHealth);
            } else if (overallHealth >= 0.5) {
                log.warn("User Profile Service health check: DEGRADED (Score: {})", overallHealth);
            } else {
                log.error("User Profile Service health check: UNHEALTHY (Score: {})", overallHealth);
            }
            
            lastHealthCheck = LocalDateTime.now();
            
        } catch (Exception e) {
            log.error("Periodic health check failed for User Profile Service", e);
        }
    }
    
    /**
     * Capability performance monitoring
     * Runs every 2 minutes to track capability metrics
     */
    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    public void monitorCapabilityPerformance() {
        
        if (!agentRegistered) {
            return;
        }
        
        try {
            log.debug("Monitoring User Profile Agent capability performance...");
            
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            var overallHealth = capabilityRegistry.calculateOverallHealthScore();
            
            // Log performance metrics
            performanceSummary.forEach((capability, metrics) -> {
                log.debug("Capability [{}]: {}", capability, metrics);
            });
            
            log.debug("Overall capability health score: {}", overallHealth);
            
            // Alert on performance degradation
            if (overallHealth < 0.7) {
                log.warn("User Profile Agent capability performance degraded - Health Score: {}", overallHealth);
            }
            
        } catch (Exception e) {
            log.error("Capability performance monitoring failed", e);
        }
    }
    
    /**
     * User profile specific monitoring
     * Runs every 5 minutes to check user management operations
     */
    @Scheduled(fixedRate = 300000, initialDelay = 180000)
    public void monitorUserProfileOperations() {
        
        if (!agentRegistered) {
            return;
        }
        
        try {
            log.debug("Monitoring user profile operations...");
            
            // Check user management capability health
            var userManagementHealth = capabilityRegistry.getCapabilityHealthScore("USER_MANAGEMENT");
            var kycComplianceHealth = capabilityRegistry.getCapabilityHealthScore("KYC_COMPLIANCE");
            var documentManagementHealth = capabilityRegistry.getCapabilityHealthScore("DOCUMENT_MANAGEMENT");
            
            // Monitor critical operations performance
            if (userManagementHealth < 0.7) {
                log.warn("User Management capability performance degraded - Health Score: {}", userManagementHealth);
            }
            
            if (kycComplianceHealth < 0.7) {
                log.warn("KYC Compliance capability performance degraded - Health Score: {}", kycComplianceHealth);
            }
            
            if (documentManagementHealth < 0.7) {
                log.warn("Document Management capability performance degraded - Health Score: {}", documentManagementHealth);
            }
            
            log.debug("User profile operations monitoring completed - User Mgmt: {}, KYC: {}, Docs: {}", 
                    userManagementHealth, kycComplianceHealth, documentManagementHealth);
            
        } catch (Exception e) {
            log.error("User profile operations monitoring failed", e);
        }
    }
    
    /**
     * KYC compliance monitoring
     * Runs every 10 minutes to check compliance status and alert on issues
     */
    @Scheduled(fixedRate = 600000, initialDelay = 300000)
    public void monitorKYCCompliance() {
        
        if (!agentRegistered) {
            return;
        }
        
        try {
            log.debug("Monitoring KYC compliance status...");
            
            var kycHealth = capabilityRegistry.getCapabilityHealthScore("KYC_COMPLIANCE");
            var kycSuccessRate = capabilityRegistry.getCapabilitySuccessRate("KYC_COMPLIANCE");
            var kycAvgTime = capabilityRegistry.getCapabilityAverageExecutionTime("KYC_COMPLIANCE");
            
            // Alert on KYC performance issues
            if (kycHealth < 0.8) {
                log.warn("KYC compliance monitoring alert - Health: {}, Success Rate: {}, Avg Time: {}ms", 
                        kycHealth, kycSuccessRate, kycAvgTime);
            }
            
            // Monitor compliance processing time
            if (kycAvgTime > 2000) { // Alert if KYC processing takes more than 2 seconds
                log.warn("KYC processing time exceeds threshold - Average time: {}ms", kycAvgTime);
            }
            
            log.debug("KYC compliance monitoring completed - Health: {}, Time: {}ms", kycHealth, kycAvgTime);
            
        } catch (Exception e) {
            log.error("KYC compliance monitoring failed", e);
        }
    }
    
    /**
     * Clean shutdown
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("User Profile Service shutting down...");
        
        try {
            if (agentRegistered) {
                var uptime = Duration.between(registrationTime, LocalDateTime.now());
                log.info("User Profile Service shutdown - Uptime: {} minutes", uptime.toMinutes());
            }
            
        } catch (Exception e) {
            log.error("Error during User Profile Service shutdown", e);
        }
    }
    
    /**
     * Get agent registration status
     */
    public boolean isAgentRegistered() {
        return agentRegistered;
    }
    
    /**
     * Get time of last health check
     */
    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }
    
    /**
     * Get agent registration time
     */
    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }
    
    /**
     * Force health check execution
     */
    public void forceHealthCheck() {
        log.info("Force executing health check for User Profile Agent...");
        performPeriodicHealthCheck();
    }
    
    /**
     * Reset capability metrics
     */
    public void resetCapabilityMetrics() {
        log.info("Resetting all capability metrics for User Profile Service...");
        
        try {
            capabilityRegistry.getCapabilities().forEach(capability -> {
                capabilityRegistry.resetCapabilityMetrics(capability);
                log.info("Reset metrics for capability: {}", capability);
            });
            
            log.info("All capability metrics reset successfully");
            
        } catch (Exception e) {
            log.error("Failed to reset capability metrics", e);
        }
    }
}