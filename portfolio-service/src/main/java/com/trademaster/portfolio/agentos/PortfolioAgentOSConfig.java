package com.trademaster.portfolio.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Agent OS Configuration
 * 
 * Configures and initializes the Portfolio Agent within the TradeMaster AgentOS
 * ecosystem. Handles agent registration, capability initialization, health
 * monitoring, and integration with the Agent Orchestration Service.
 * 
 * Configuration Features:
 * - Automatic agent registration on application startup
 * - Capability registry initialization and health monitoring
 * - Scheduled health checks and performance reporting
 * - MCP protocol integration and endpoint configuration
 * - Error handling and recovery mechanisms
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PortfolioAgentOSConfig {
    
    private final PortfolioAgent portfolioAgent;
    private final PortfolioCapabilityRegistry capabilityRegistry;
    
    /**
     * Initializes AgentOS integration on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeAgentOSIntegration() {
        log.info("Initializing Portfolio Agent OS integration...");
        
        try {
            // Initialize capability registry
            capabilityRegistry.initializeCapabilities();
            
            // Register agent with orchestration service
            registerAgentWithOrchestrationService();
            
            // Perform initial health check
            portfolioAgent.performHealthCheck();
            
            log.info("Portfolio Agent OS integration completed successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Portfolio Agent OS integration", e);
            scheduleRetryInitialization();
        }
    }
    
    /**
     * Registers the portfolio agent with the Agent Orchestration Service
     */
    private void registerAgentWithOrchestrationService() {
        log.info("Registering Portfolio Agent with Orchestration Service...");
        
        try {
            // Trigger registration callback
            portfolioAgent.onRegistration();
            
            log.info("Portfolio Agent registration completed - ID: {}, Type: {}, Capabilities: {}", 
                    portfolioAgent.getAgentId(), 
                    portfolioAgent.getAgentType(), 
                    portfolioAgent.getCapabilities());
                    
        } catch (Exception e) {
            log.error("Failed to register Portfolio Agent with Orchestration Service", e);
            throw new RuntimeException("Agent registration failed", e);
        }
    }
    
    /**
     * Schedules periodic health checks and performance reporting
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Async
    public void performScheduledHealthCheck() {
        try {
            // Perform agent health check
            portfolioAgent.performHealthCheck();
            
            // Log health metrics
            Double healthScore = portfolioAgent.getHealthScore();
            log.debug("Portfolio Agent health check completed - Score: {}", healthScore);
            
            // Alert if health score is low
            if (healthScore < 0.7) {
                log.warn("Portfolio Agent health score is low: {} - investigating capabilities", healthScore);
                logCapabilityHealth();
            }
            
        } catch (Exception e) {
            log.error("Scheduled health check failed", e);
        }
    }
    
    /**
     * Reports performance metrics to orchestration service
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Async
    public void reportPerformanceMetrics() {
        try {
            var performanceSummary = capabilityRegistry.getPerformanceSummary();
            
            log.info("Portfolio Agent Performance Report:");
            performanceSummary.forEach((capability, metrics) -> 
                log.info("  {}: {}", capability, metrics));
                
        } catch (Exception e) {
            log.error("Failed to report performance metrics", e);
        }
    }
    
    /**
     * Performs capability health monitoring and optimization
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    @Async
    public void monitorCapabilityHealth() {
        try {
            portfolioAgent.getCapabilities().forEach(capability -> {
                Double healthScore = capabilityRegistry.getCapabilityHealthScore(capability);
                Double successRate = capabilityRegistry.getCapabilitySuccessRate(capability);
                Double avgTime = capabilityRegistry.getCapabilityAverageExecutionTime(capability);
                
                if (healthScore < 0.5) {
                    log.warn("Capability {} health is critical - Health: {}, Success Rate: {}, Avg Time: {}ms", 
                            capability, healthScore, successRate, avgTime);
                    
                    // Consider resetting metrics if needed
                    if (successRate < 0.3) {
                        log.info("Resetting metrics for underperforming capability: {}", capability);
                        capabilityRegistry.resetCapabilityMetrics(capability);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Capability health monitoring failed", e);
        }
    }
    
    /**
     * Handles graceful agent deregistration on shutdown
     */
    @EventListener
    public void handleShutdown() {
        log.info("Portfolio Agent shutdown initiated...");
        
        try {
            // Deregister from orchestration service
            portfolioAgent.onDeregistration();
            log.info("Portfolio Agent deregistration completed");
            
        } catch (Exception e) {
            log.error("Failed to deregister Portfolio Agent", e);
        }
    }
    
    /**
     * Schedules retry initialization if initial setup fails
     */
    @Async
    private void scheduleRetryInitialization() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30000); // Wait 30 seconds
                initializeAgentOSIntegration();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry initialization interrupted", e);
            }
        });
    }
    
    /**
     * Logs detailed capability health information
     */
    private void logCapabilityHealth() {
        portfolioAgent.getCapabilities().forEach(capability -> {
            Double healthScore = capabilityRegistry.getCapabilityHealthScore(capability);
            Double successRate = capabilityRegistry.getCapabilitySuccessRate(capability);
            Double avgTime = capabilityRegistry.getCapabilityAverageExecutionTime(capability);
            
            log.warn("Capability {}: Health={}, Success Rate={}, Avg Time={}ms", 
                    capability, healthScore, successRate, avgTime);
        });
    }
    
    /**
     * Bean for async task execution
     */
    @Bean
    public java.util.concurrent.Executor agentOSTaskExecutor() {
        java.util.concurrent.ThreadPoolExecutor executor = new java.util.concurrent.ThreadPoolExecutor(
            2,  // Core pool size
            10, // Maximum pool size
            60L, java.util.concurrent.TimeUnit.SECONDS, // Keep alive time
            new java.util.concurrent.LinkedBlockingQueue<>(100), // Work queue
            r -> {
                Thread t = new Thread(r, "AgentOS-Portfolio-Task-");
                t.setDaemon(true);
                return t;
            }
        );
        
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}