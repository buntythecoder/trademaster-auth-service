package com.trademaster.marketdata.agentos;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.trademaster.marketdata.agentos.AgentConstants.*;

/**
 * Market Data Agent Capability Registry
 * 
 * Tracks the performance, health, and availability of market data agent capabilities.
 * Provides metrics and health scoring for the AgentOS orchestration framework.
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
public class MarketDataCapabilityRegistry {
    
    private final Map<String, CapabilityMetrics> capabilityMetrics = new ConcurrentHashMap<>();
    
    /**
     * Records a successful execution of a capability
     */
    public void recordSuccessfulExecution(String capabilityName) {
        getOrCreateMetrics(capabilityName).recordSuccess();
        log.debug("Recorded successful execution for capability: {}", capabilityName);
    }
    
    /**
     * Records a failed execution of a capability
     */
    public void recordFailedExecution(String capabilityName, Exception error) {
        getOrCreateMetrics(capabilityName).recordFailure(error);
        log.warn("Recorded failed execution for capability: {} - Error: {}", 
                capabilityName, error.getMessage());
    }
    
    /**
     * Records the execution time for a capability
     */
    public void recordExecutionTime(String capabilityName, long durationMs) {
        getOrCreateMetrics(capabilityName).recordExecutionTime(durationMs);
    }
    
    /**
     * Gets the health score for a specific capability (0.0 to 1.0)
     */
    public Double getCapabilityHealthScore(String capabilityName) {
        var metrics = capabilityMetrics.get(capabilityName);
        return metrics != null ? metrics.calculateHealthScore() : 1.0;
    }
    
    /**
     * Calculates the overall health score across all capabilities
     */
    public Double calculateOverallHealthScore() {
        if (capabilityMetrics.isEmpty()) {
            return 1.0;
        }
        
        double totalScore = capabilityMetrics.values().stream()
            .mapToDouble(CapabilityMetrics::calculateHealthScore)
            .sum();
            
        return totalScore / capabilityMetrics.size();
    }
    
    /**
     * Gets or creates metrics for a capability
     */
    private CapabilityMetrics getOrCreateMetrics(String capabilityName) {
        return capabilityMetrics.computeIfAbsent(capabilityName, 
            name -> new CapabilityMetrics(name));
    }
    
    /**
     * Gets all capability metrics
     */
    public Map<String, CapabilityMetrics> getAllMetrics() {
        return Map.copyOf(capabilityMetrics);
    }
    
    /**
     * Initialize capabilities for the market data agent
     */
    public void initializeCapabilities() {
        log.info("Initializing market data agent capabilities...");
        
        // Initialize core capabilities
        String[] coreCapabilities = {
            CAPABILITY_REAL_TIME_DATA,
            CAPABILITY_HISTORICAL_DATA, 
            CAPABILITY_TECHNICAL_ANALYSIS,
            CAPABILITY_MARKET_SCANNING,
            CAPABILITY_PRICE_ALERTS,
            CAPABILITY_DATA_CACHING,
            CAPABILITY_SYMBOL_LOOKUP,
            CAPABILITY_EXCHANGE_INTEGRATION
        };
        
        for (String capability : coreCapabilities) {
            getOrCreateMetrics(capability);
            log.debug("Initialized capability: {}", capability);
        }
        
        log.info("Market data agent capabilities initialized successfully");
    }
    
    /**
     * Get performance summary for all capabilities
     */
    public Map<String, String> getPerformanceSummary() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        
        capabilityMetrics.forEach((name, metrics) -> {
            double healthScore = metrics.calculateHealthScore();
            long avgExecutionTime = metrics.getAverageExecutionTime();
            int successCount = metrics.getSuccessCount();
            int failureCount = metrics.getFailureCount();
            
            String performanceInfo = String.format(
                "Health: %.2f, AvgTime: %dms, Success: %d, Failures: %d",
                healthScore, avgExecutionTime, successCount, failureCount
            );
            
            summary.put(name, performanceInfo);
        });
        
        return summary;
    }
    
    /**
     * Reset capability metrics for a specific capability
     */
    public void resetCapabilityMetrics(String capabilityName) {
        capabilityMetrics.remove(capabilityName);
        log.info("Reset metrics for capability: {}", capabilityName);
    }
    
    /**
     * Capability performance metrics
     */
    @Getter
    public static class CapabilityMetrics {
        private final String capabilityName;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private volatile LocalDateTime lastExecution;
        private volatile String lastError;
        
        public CapabilityMetrics(String capabilityName) {
            this.capabilityName = capabilityName;
        }
        
        public void recordSuccess() {
            successCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
        }
        
        public void recordFailure(Exception error) {
            failureCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
            lastError = error.getMessage();
        }
        
        public void recordExecutionTime(long durationMs) {
            totalExecutionTime.addAndGet(durationMs);
            executionCount.incrementAndGet();
        }
        
        public double calculateHealthScore() {
            int total = getSuccessCount() + getFailureCount();
            if (total == 0) {
                return 1.0; // No executions yet, assume healthy
            }
            
            double successRate = (double) getSuccessCount() / total;
            
            // Factor in recent activity (penalize if no recent executions)
            double activityFactor = 1.0;
            if (lastExecution != null) {
                long minutesSinceLastExecution = java.time.Duration.between(
                    lastExecution, LocalDateTime.now()).toMinutes();
                if (minutesSinceLastExecution > 60) {
                    activityFactor = Math.max(0.5, 1.0 - (minutesSinceLastExecution / 1440.0));
                }
            }
            
            return successRate * activityFactor;
        }
        
        public long getAverageExecutionTime() {
            int count = executionCount.get();
            return count > 0 ? totalExecutionTime.get() / count : 0;
        }
        
        // Custom getters for atomic fields (Lombok @Getter doesn't handle these correctly)
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
    }
}