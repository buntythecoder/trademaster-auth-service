package com.trademaster.auth.agentos;

import lombok.Getter;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Authentication AgentOS Configuration
 * 
 * Manages the lifecycle, registration, and health monitoring of the Authentication Agent
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
@Getter
public class AuthAgentOSConfig implements ApplicationRunner {
    
    private final AuthenticationAgent authenticationAgent;
    private final AuthCapabilityRegistry capabilityRegistry;

    /**
     * -- GETTER --
     *  Get agent registration status
     */
    private volatile boolean agentRegistered = false;
    /**
     * -- GETTER --
     *  Get time of last health check
     */
    private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();
    /**
     * -- GETTER --
     *  Get agent registration time
     */
    private volatile LocalDateTime registrationTime;
    
    /**
     * Initialize Authentication Agent on application startup
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing Authentication Agent for AgentOS integration...");
        
        try {
            // Initialize capability registry with authentication capabilities
            capabilityRegistry.initializeCapabilities();
            
            // Register agent with AgentOS orchestrator
            registerWithAgentOS();
            
            // Perform initial health check
            performInitialHealthCheck();
            
            log.info("Authentication Agent successfully initialized and registered with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to initialize Authentication Agent", e);
            throw new RuntimeException("Authentication Agent initialization failed", e);
        }
    }
    
    /**
     * Register agent with the AgentOS orchestration service
     */
    private void registerWithAgentOS() {
        try {
            log.info("Registering Authentication Agent with AgentOS orchestrator...");
            
            // Set registration callback
            authenticationAgent.onRegistration();
            
            var agentId = authenticationAgent.getAgentId();
            var agentType = authenticationAgent.getAgentType();
            var capabilities = authenticationAgent.getCapabilities();
            var healthScore = authenticationAgent.getHealthScore();
            
            log.info("Agent Registration Details:");
            log.info("  Agent ID: {}", agentId);
            log.info("  Agent Type: {}", agentType);
            log.info("  Capabilities: {}", capabilities);
            log.info("  Initial Health Score: {}", healthScore);
            
            agentRegistered = true;
            registrationTime = LocalDateTime.now();
            
            log.info("Authentication Agent registered successfully with AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to register Authentication Agent with AgentOS", e);
            agentRegistered = false;
            throw new RuntimeException("Agent registration failed", e);
        }
    }
    
    /**
     * Perform initial health check after registration
     */
    private void performInitialHealthCheck() {
        try {
            log.info("Performing initial health check for Authentication Agent...");
            
            authenticationAgent.performHealthCheck();
            
            var healthScore = authenticationAgent.getHealthScore();
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            
            log.info("Initial Health Check Results:");
            log.info("  Overall Health Score: {}", healthScore);
            log.info("  Capability Performance Summary: {}", performanceSummary);
            
            lastHealthCheck = LocalDateTime.now();
            
            log.info("Initial health check completed successfully");
            
        } catch (Exception e) {
            log.error("Initial health check failed for Authentication Agent", e);
            // Don't throw exception - allow agent to start but log the issue
        }
    }
    
    /**
     * Periodic health check and metrics reporting
     * Runs every 30 seconds to monitor agent health
     */
    @Scheduled(fixedRate = 30000, initialDelay = 30000)
    public void performPeriodicHealthCheck() {
        Optional.of(agentRegistered)
            .filter(registered -> registered)
            .ifPresentOrElse(
                registered -> performHealthCheckInternal(),
                () -> log.warn("Skipping health check - agent not registered with AgentOS")
            );
    }

    private void performHealthCheckInternal() {
        Optional.of(authenticationAgent)
            .ifPresent(agent -> {
                try {
                    log.debug("Performing periodic health check for Authentication Agent...");
                    agent.performHealthCheck();

                    double healthScore = agent.getHealthScore();
                    Duration timeSinceRegistration = Duration.between(registrationTime, LocalDateTime.now());

                    // Log health status using functional threshold mapping
                    getHealthStatus(healthScore).accept(healthScore);

                    // Log uptime information
                    log.debug("Authentication Agent uptime: {} minutes", timeSinceRegistration.toMinutes());
                    lastHealthCheck = LocalDateTime.now();

                } catch (Exception e) {
                    log.error("Periodic health check failed for Authentication Agent", e);
                    // Continue operation despite health check failure
                }
            });
    }

    private java.util.function.Consumer<Double> getHealthStatus(double healthScore) {
        return java.util.stream.Stream.of(
                Map.entry(0.8, (Consumer<Double>) score ->
                    log.debug("Authentication Agent health check: HEALTHY (Score: {})", score)),
                Map.entry(0.5, (Consumer<Double>) score ->
                    log.warn("Authentication Agent health check: DEGRADED (Score: {})", score)),
                Map.entry(0.0, (Consumer<Double>) score ->
                    log.error("Authentication Agent health check: UNHEALTHY (Score: {})", score))
            )
            .filter(entry -> healthScore >= entry.getKey())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(score -> log.error("Authentication Agent health check: UNHEALTHY (Score: {})", score));
    }
    
    /**
     * Capability performance monitoring
     * Runs every 2 minutes to track capability metrics
     */
    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    public void monitorCapabilityPerformance() {
        Optional.of(agentRegistered)
            .filter(registered -> registered)
            .ifPresent(registered -> {
                try {
                    log.debug("Monitoring Authentication Agent capability performance...");

                    var performanceSummary = capabilityRegistry.getPerformanceSummary();
                    var overallHealth = capabilityRegistry.calculateOverallHealthScore();

                    // Log performance metrics
                    performanceSummary.forEach((capability, metrics) ->
                        log.debug("Capability [{}]: {}", capability, metrics));

                    log.debug("Overall capability health score: {}", overallHealth);

                    // Alert on performance degradation
                    Optional.of(overallHealth)
                        .filter(health -> health < 0.7)
                        .ifPresent(health ->
                            log.warn("Authentication Agent capability performance degraded - Health Score: {}", health));

                } catch (Exception e) {
                    log.error("Capability performance monitoring failed", e);
                }
            });
    }
    
    /**
     * Clean shutdown and deregistration
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("Authentication Agent shutting down - deregistering from AgentOS...");

        try {
            Optional.of(agentRegistered)
                .filter(registered -> registered)
                .ifPresent(registered -> {
                    // Notify orchestrator of shutdown
                    authenticationAgent.onDeregistration();

                    Duration uptime = Duration.between(registrationTime, LocalDateTime.now());
                    log.info("Authentication Agent deregistered successfully - Uptime: {} minutes",
                            uptime.toMinutes());
                });
            
        } catch (Exception e) {
            log.error("Error during Authentication Agent deregistration", e);
        }
    }

    /**
     * Force health check execution
     */
    public void forceHealthCheck() {
        log.info("Force executing health check for Authentication Agent...");
        performPeriodicHealthCheck();
    }
    
    /**
     * Reset capability metrics
     */
    public void resetCapabilityMetrics() {
        log.info("Resetting all capability metrics for Authentication Agent...");
        
        try {
            authenticationAgent.getCapabilities().forEach(capability -> {
                capabilityRegistry.resetCapabilityMetrics(capability);
                log.info("Reset metrics for capability: {}", capability);
            });
            
            log.info("All capability metrics reset successfully");
            
        } catch (Exception e) {
            log.error("Failed to reset capability metrics", e);
        }
    }
}