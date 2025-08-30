package com.trademaster.marketdata.config;

import com.trademaster.marketdata.agentos.MarketDataAgent;
import com.trademaster.marketdata.agentos.MarketDataCapabilityRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * AgentOS Configuration for Market Data Service
 * 
 * Configures the integration between the Market Data Service and the
 * TradeMaster AgentOS framework. Handles agent registration, capability
 * management, and performance monitoring.
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Getter
public class AgentOSConfig implements ApplicationRunner {
    
    private final MarketDataAgent marketDataAgent;
    private final MarketDataCapabilityRegistry capabilityRegistry;
    
    // Agent registration state tracking
    private volatile boolean agentRegistered = false;
    private LocalDateTime registrationTime;
    private LocalDateTime lastHealthCheck;
    
    /**
     * Task executor for AgentOS operations with virtual threads
     */
    @Bean(name = "agentOSTaskExecutor")
    public Executor agentOSTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AgentOS-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("AgentOS Task Executor initialized with virtual threads support");
        return executor;
    }
    
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
            log.error("Initial health check failed", e);
            throw new RuntimeException("Initial health check failed", e);
        }
    }
    
    /**
     * Periodic health check and metrics reporting
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performPeriodicHealthCheck() {
        try {
            if (!agentRegistered) {
                log.warn("Skipping health check - Agent not registered");
                return;
            }
            
            // Perform agent health check
            marketDataAgent.performHealthCheck();
            
            // Log health metrics
            double healthScore = marketDataAgent.getHealthScore();
            log.debug("Market Data Agent health check - Score: {}", healthScore);
            
            lastHealthCheck = LocalDateTime.now();
            
            // Report to orchestration service if health degrades
            if (healthScore < 0.7) {
                log.warn("Market Data Agent health degraded: {}", healthScore);
                reportHealthDegradation(healthScore);
            }
            
        } catch (Exception e) {
            log.error("Health check failed for Market Data Agent", e);
        }
    }
    
    /**
     * Capability metrics reporting
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void reportCapabilityMetrics() {
        try {
            var allMetrics = capabilityRegistry.getAllMetrics();
            
            allMetrics.forEach((capabilityName, metrics) -> {
                log.info("Capability {} - Success: {}, Failures: {}, Avg Execution: {}ms, Health: {}",
                        capabilityName,
                        metrics.getSuccessCount(),
                        metrics.getFailureCount(),
                        metrics.getAverageExecutionTime(),
                        capabilityRegistry.getCapabilityHealthScore(capabilityName));
            });
            
        } catch (Exception e) {
            log.error("Failed to report capability metrics", e);
        }
    }
    
    /**
     * Report health degradation to the orchestration service
     */
    private void reportHealthDegradation(double healthScore) {
        log.warn("Reporting health degradation to Orchestration Service - Score: {}", healthScore);
        
        // Analyze individual capability health
        capabilityRegistry.getAllMetrics().forEach((capability, metrics) -> {
            double capabilityHealth = metrics.calculateHealthScore();
            if (capabilityHealth < 0.7) {
                log.warn("Degraded capability: {} - Health Score: {:.2f}, Success: {}, Failures: {}", 
                        capability, capabilityHealth, metrics.getSuccessCount(), metrics.getFailureCount());
            }
        });
        
        // Update overall agent health status
        if (healthScore < 0.5) {
            log.error("Critical health degradation detected - Score: {}", healthScore);
        }
    }
    
    /**
     * Graceful shutdown - deregister agent
     */
    @org.springframework.context.event.EventListener
    public void handleShutdown(org.springframework.context.event.ContextClosedEvent event) {
        try {
            if (!agentRegistered) {
                log.info("Agent was not registered, skipping deregistration");
                return;
            }
            
            log.info("Deregistering Market Data Agent from AgentOS...");
            
            // Trigger agent deregistration callback
            marketDataAgent.onDeregistration();
            
            // Update registration state
            agentRegistered = false;
            
            log.info("Market Data Agent successfully deregistered from AgentOS");
            
        } catch (Exception e) {
            log.error("Failed to deregister Market Data Agent", e);
        }
    }
}