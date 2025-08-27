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
    
    private final UserProfileAgent userProfileAgent;
    private final UserProfileCapabilityRegistry capabilityRegistry;
    
    private volatile boolean agentRegistered = false;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private volatile LocalDateTime registrationTime;
    
    /**
     * Initialize User Profile Agent on application startup
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing User Profile Agent for AgentOS integration...");
        
        try {
            // Initialize capability registry with user profile capabilities
            capabilityRegistry.initializeCapabilities();
            
            // Register agent with AgentOS orchestrator
            registerWithAgentOS();
            
            // Perform initial health check
            performInitialHealthCheck();
            
            log.info("User Profile Agent successfully initialized and registered with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to initialize User Profile Agent", e);
            throw new RuntimeException("User Profile Agent initialization failed", e);
        }
    }
    
    /**
     * Register agent with the AgentOS orchestration service
     */
    private void registerWithAgentOS() {
        try {
            log.info("Registering User Profile Agent with AgentOS orchestrator...");
            
            // Set registration callback
            userProfileAgent.onRegistration();
            
            var agentId = userProfileAgent.getAgentId();
            var agentType = userProfileAgent.getAgentType();
            var capabilities = userProfileAgent.getCapabilities();
            var healthScore = userProfileAgent.getHealthScore();
            
            log.info("Agent Registration Details:");
            log.info("  Agent ID: {}", agentId);
            log.info("  Agent Type: {}", agentType);
            log.info("  Capabilities: {}", capabilities);
            log.info("  Initial Health Score: {}", healthScore);
            
            agentRegistered = true;
            registrationTime = LocalDateTime.now();
            
            log.info("User Profile Agent registered successfully with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to register User Profile Agent with AgentOS", e);
            agentRegistered = false;
            throw new RuntimeException("Agent registration failed", e);
        }
    }
    
    /**
     * Perform initial health check after registration
     */
    private void performInitialHealthCheck() {
        try {
            log.info("Performing initial health check for User Profile Agent...");
            
            userProfileAgent.performHealthCheck();
            
            var healthScore = userProfileAgent.getHealthScore();
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            
            log.info("Initial Health Check Results:");
            log.info("  Overall Health Score: {}", healthScore);
            log.info("  Capability Performance Summary: {}", performanceSummary);
            
            lastHealthCheck = LocalDateTime.now();
            
            log.info("Initial health check completed successfully");
            
        } catch (Exception e) {
            log.error("Initial health check failed for User Profile Agent", e);
            // Don't throw exception - allow agent to start but log the issue
        }
    }
    
    /**
     * Periodic health check and metrics reporting
     * Runs every 30 seconds to monitor agent health
     */
    @Scheduled(fixedRate = 30000, initialDelay = 30000)
    public void performPeriodicHealthCheck() {
        
        if (!agentRegistered) {
            log.warn("Skipping health check - agent not registered with AgentOS");
            return;
        }
        
        try {
            log.debug("Performing periodic health check for User Profile Agent...");
            
            // Perform agent health check
            userProfileAgent.performHealthCheck();
            
            var healthScore = userProfileAgent.getHealthScore();
            var timeSinceRegistration = Duration.between(registrationTime, LocalDateTime.now());
            
            // Log health status
            if (healthScore >= 0.8) {
                log.debug("User Profile Agent health check: HEALTHY (Score: {})", healthScore);
            } else if (healthScore >= 0.5) {
                log.warn("User Profile Agent health check: DEGRADED (Score: {})", healthScore);
            } else {
                log.error("User Profile Agent health check: UNHEALTHY (Score: {})", healthScore);
            }
            
            // Log uptime information
            log.debug("User Profile Agent uptime: {} minutes", timeSinceRegistration.toMinutes());
            
            lastHealthCheck = LocalDateTime.now();
            
        } catch (Exception e) {
            log.error("Periodic health check failed for User Profile Agent", e);
            // Continue operation despite health check failure
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
     * Clean shutdown and deregistration
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("User Profile Agent shutting down - deregistering from AgentOS...");
        
        try {
            if (agentRegistered) {
                // Notify orchestrator of shutdown
                userProfileAgent.onDeregistration();
                
                var uptime = Duration.between(registrationTime, LocalDateTime.now());
                log.info("User Profile Agent deregistered successfully - Uptime: {} minutes", 
                        uptime.toMinutes());
            }
            
        } catch (Exception e) {
            log.error("Error during User Profile Agent deregistration", e);
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
        log.info("Resetting all capability metrics for User Profile Agent...");
        
        try {
            userProfileAgent.getCapabilities().forEach(capability -> {
                capabilityRegistry.resetCapabilityMetrics(capability);
                log.info("Reset metrics for capability: {}", capability);
            });
            
            log.info("All capability metrics reset successfully");
            
        } catch (Exception e) {
            log.error("Failed to reset capability metrics", e);
        }
    }
}