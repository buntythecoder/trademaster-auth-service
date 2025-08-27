package com.trademaster.marketdata.agentos;

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
 * Market Data AgentOS Configuration
 * 
 * Manages the lifecycle, registration, and health monitoring of the Market Data Agent
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
public class MarketDataAgentOSConfig implements ApplicationRunner {
    
    private final MarketDataAgent marketDataAgent;
    private final MarketDataCapabilityRegistry capabilityRegistry;
    
    private volatile boolean agentRegistered = false;
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    private volatile LocalDateTime registrationTime;
    
    /**
     * Initialize Market Data Agent on application startup
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing Market Data Agent for AgentOS integration...");
        
        try {
            // Initialize capability registry with market data capabilities
            capabilityRegistry.initializeCapabilities();
            
            // Register agent with AgentOS orchestrator
            registerWithAgentOS();
            
            // Perform initial health check
            performInitialHealthCheck();
            
            log.info("Market Data Agent successfully initialized and registered with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to initialize Market Data Agent", e);
            throw new RuntimeException("Market Data Agent initialization failed", e);
        }
    }
    
    /**
     * Register agent with the AgentOS orchestration service
     */
    private void registerWithAgentOS() {
        try {
            log.info("Registering Market Data Agent with AgentOS orchestrator...");
            
            // Set registration callback
            marketDataAgent.onRegistration();
            
            var agentId = marketDataAgent.getAgentId();
            var agentType = marketDataAgent.getAgentType();
            var capabilities = marketDataAgent.getCapabilities();
            var healthScore = marketDataAgent.getHealthScore();
            
            log.info("Agent Registration Details:");
            log.info("  Agent ID: {}", agentId);
            log.info("  Agent Type: {}", agentType);
            log.info("  Capabilities: {}", capabilities);
            log.info("  Initial Health Score: {}", healthScore);
            
            agentRegistered = true;
            registrationTime = LocalDateTime.now();
            
            log.info("Market Data Agent registered successfully with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to register Market Data Agent with AgentOS", e);
            agentRegistered = false;
            throw new RuntimeException("Agent registration failed", e);
        }
    }
    
    /**
     * Perform initial health check after registration
     */
    private void performInitialHealthCheck() {
        try {
            log.info("Performing initial health check for Market Data Agent...");
            
            marketDataAgent.performHealthCheck();
            
            var healthScore = marketDataAgent.getHealthScore();
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            
            log.info("Initial Health Check Results:");
            log.info("  Overall Health Score: {}", healthScore);
            log.info("  Capability Performance Summary: {}", performanceSummary);
            
            lastHealthCheck = LocalDateTime.now();
            
            log.info("Initial health check completed successfully");
            
        } catch (Exception e) {
            log.error("Initial health check failed for Market Data Agent", e);
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
            log.debug("Performing periodic health check for Market Data Agent...");
            
            // Perform agent health check
            marketDataAgent.performHealthCheck();
            
            var healthScore = marketDataAgent.getHealthScore();
            var timeSinceRegistration = Duration.between(registrationTime, LocalDateTime.now());
            
            // Log health status
            if (healthScore >= 0.8) {
                log.debug("Market Data Agent health check: HEALTHY (Score: {})", healthScore);
            } else if (healthScore >= 0.5) {
                log.warn("Market Data Agent health check: DEGRADED (Score: {})", healthScore);
            } else {
                log.error("Market Data Agent health check: UNHEALTHY (Score: {})", healthScore);
            }
            
            // Log uptime information
            log.debug("Market Data Agent uptime: {} minutes", timeSinceRegistration.toMinutes());
            
            lastHealthCheck = LocalDateTime.now();
            
        } catch (Exception e) {
            log.error("Periodic health check failed for Market Data Agent", e);
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
            log.debug("Monitoring Market Data Agent capability performance...");
            
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            var overallHealth = capabilityRegistry.calculateOverallHealthScore();
            
            // Log performance metrics
            performanceSummary.forEach((capability, metrics) -> {
                log.debug("Capability [{}]: {}", capability, metrics);
            });
            
            log.debug("Overall capability health score: {}", overallHealth);
            
            // Alert on performance degradation
            if (overallHealth < 0.7) {
                log.warn("Market Data Agent capability performance degraded - Health Score: {}", overallHealth);
            }
            
        } catch (Exception e) {
            log.error("Capability performance monitoring failed", e);
        }
    }
    
    /**
     * Market data connectivity monitoring
     * Runs every 5 minutes to check data feed connectivity
     */
    @Scheduled(fixedRate = 300000, initialDelay = 120000)
    public void monitorDataConnectivity() {
        
        if (!agentRegistered) {
            return;
        }
        
        try {
            log.debug("Monitoring market data feed connectivity...");
            
            // Check real-time data capability health
            var realTimeHealth = capabilityRegistry.getCapabilityHealthScore("REAL_TIME_DATA");
            var historicalHealth = capabilityRegistry.getCapabilityHealthScore("HISTORICAL_DATA");
            
            // Monitor data feed performance
            if (realTimeHealth < 0.7) {
                log.warn("Real-time data feed performance degraded - Health Score: {}", realTimeHealth);
            }
            
            if (historicalHealth < 0.7) {
                log.warn("Historical data feed performance degraded - Health Score: {}", historicalHealth);
            }
            
            log.debug("Data connectivity check completed - Real-time: {}, Historical: {}", 
                    realTimeHealth, historicalHealth);
            
        } catch (Exception e) {
            log.error("Data connectivity monitoring failed", e);
        }
    }
    
    /**
     * Clean shutdown and deregistration
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("Market Data Agent shutting down - deregistering from AgentOS...");
        
        try {
            if (agentRegistered) {
                // Notify orchestrator of shutdown
                marketDataAgent.onDeregistration();
                
                var uptime = Duration.between(registrationTime, LocalDateTime.now());
                log.info("Market Data Agent deregistered successfully - Uptime: {} minutes", 
                        uptime.toMinutes());
            }
            
        } catch (Exception e) {
            log.error("Error during Market Data Agent deregistration", e);
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
        log.info("Force executing health check for Market Data Agent...");
        performPeriodicHealthCheck();
    }
    
    /**
     * Reset capability metrics
     */
    public void resetCapabilityMetrics() {
        log.info("Resetting all capability metrics for Market Data Agent...");
        
        try {
            marketDataAgent.getCapabilities().forEach(capability -> {
                capabilityRegistry.resetCapabilityMetrics(capability);
                log.info("Reset metrics for capability: {}", capability);
            });
            
            log.info("All capability metrics reset successfully");
            
        } catch (Exception e) {
            log.error("Failed to reset capability metrics", e);
        }
    }
}