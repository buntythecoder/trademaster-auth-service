package com.trademaster.marketdata.config;

import com.trademaster.marketdata.agentos.MarketDataAgent;
import com.trademaster.marketdata.agentos.MarketDataCapabilityRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
public class AgentOSConfig {
    
    private final MarketDataAgent marketDataAgent;
    private final MarketDataCapabilityRegistry capabilityRegistry;
    
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
     * Register the market data agent with the orchestration service on startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerMarketDataAgent() {
        try {
            log.info("Registering Market Data Agent with AgentOS...");
            
            // Trigger agent registration callback
            marketDataAgent.onRegistration();
            
            // Log agent capabilities
            log.info("Market Data Agent registered with capabilities: {}", 
                    marketDataAgent.getCapabilities());
            log.info("Initial health score: {}", marketDataAgent.getHealthScore());
            
            // Notify orchestration service (would typically be done via REST call)
            notifyAgentOrchestrationService();
            
        } catch (Exception e) {
            log.error("Failed to register Market Data Agent with AgentOS", e);
            throw new RuntimeException("AgentOS registration failed", e);
        }
    }
    
    /**
     * Periodic health check and metrics reporting
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performPeriodicHealthCheck() {
        try {
            // Perform agent health check
            marketDataAgent.performHealthCheck();
            
            // Log health metrics
            double healthScore = marketDataAgent.getHealthScore();
            log.debug("Market Data Agent health check - Score: {}", healthScore);
            
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
                log.info("Capability {} - Success: {}, Failures: {}, Avg Execution: {}ms, Health: {:.2f}",
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
     * Notify the Agent Orchestration Service of agent registration
     */
    private void notifyAgentOrchestrationService() {
        // In a real implementation, this would make an HTTP call to the 
        // Agent Orchestration Service to register this agent
        
        log.info("Market Data Agent notification sent to Orchestration Service");
        log.info("Agent Details:");
        log.info("  - Agent ID: {}", marketDataAgent.getAgentId());
        log.info("  - Agent Type: {}", marketDataAgent.getAgentType());
        log.info("  - Capabilities: {}", marketDataAgent.getCapabilities());
        log.info("  - Health Score: {}", marketDataAgent.getHealthScore());
        
        // TODO: Implement actual HTTP client call to orchestration service
        // Example:
        // restTemplate.postForEntity(
        //     "http://agent-orchestration-service:8090/api/agents/register",
        //     createAgentRegistrationRequest(),
        //     AgentRegistrationResponse.class
        // );
    }
    
    /**
     * Report health degradation to the orchestration service
     */
    private void reportHealthDegradation(double healthScore) {
        log.warn("Reporting health degradation to Orchestration Service - Score: {}", healthScore);
        
        // TODO: Implement actual health reporting
        // Example:
        // restTemplate.postForEntity(
        //     "http://agent-orchestration-service:8090/api/agents/{agentId}/health",
        //     createHealthReport(healthScore),
        //     Void.class,
        //     marketDataAgent.getAgentId()
        // );
    }
    
    /**
     * Graceful shutdown - deregister agent
     */
    @EventListener
    public void handleShutdown() {
        try {
            log.info("Deregistering Market Data Agent from AgentOS...");
            
            // Trigger agent deregistration callback
            marketDataAgent.onDeregistration();
            
            // Notify orchestration service
            // TODO: Implement deregistration HTTP call
            
            log.info("Market Data Agent successfully deregistered");
            
        } catch (Exception e) {
            log.error("Failed to deregister Market Data Agent", e);
        }
    }
}