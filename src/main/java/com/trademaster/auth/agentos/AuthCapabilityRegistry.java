package com.trademaster.auth.agentos;

import com.trademaster.auth.constants.AuthConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authentication Capability Registry
 * 
 * Tracks performance, health, and usage statistics for all authentication capabilities
 * in the TradeMaster Agent ecosystem. Provides real-time metrics for the
 * Agent Orchestration Service to make intelligent routing decisions.
 * 
 * Capabilities Managed:
 * - USER_AUTHENTICATION: JWT-based authentication and session management
 * - MULTI_FACTOR_AUTH: MFA setup, verification, and recovery
 * - SECURITY_AUDIT: Security event logging and compliance monitoring
 * - SESSION_MANAGEMENT: User session lifecycle and security controls
 * - DEVICE_TRUST: Device fingerprinting and trusted device management
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCapabilityRegistry {
    
    private final Map<String, CapabilityMetrics> capabilityMetrics = new ConcurrentHashMap<>();
    
    /**
     * Initialize capability metrics for authentication agent
     */
    public void initializeCapabilities() {
        initializeCapability(AuthConstants.CAPABILITY_USER_AUTHENTICATION);
        initializeCapability(AuthConstants.CAPABILITY_MULTI_FACTOR_AUTH);
        initializeCapability(AuthConstants.CAPABILITY_SECURITY_AUDIT);
        initializeCapability(AuthConstants.CAPABILITY_SESSION_MANAGEMENT);
        initializeCapability(AuthConstants.CAPABILITY_DEVICE_TRUST);
        
        log.info("Authentication capability registry initialized with {} capabilities", 
                capabilityMetrics.size());
    }
    
    /**
     * Records successful execution of a capability
     */
    public void recordSuccessfulExecution(String capability) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordSuccess();
                log.debug("Recorded successful execution for capability: {}", capability);
            });
    }
    
    /**
     * Records failed execution of a capability
     */
    public void recordFailedExecution(String capability, Exception error) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordFailure(error);
                log.warn("Recorded failed execution for capability: {} - Error: {}",
                        capability, error.getMessage());
            });
    }
    
    /**
     * Records execution time for performance tracking
     */
    public void recordExecutionTime(String capability, Duration executionTime) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordExecutionTime(executionTime);
                log.debug("Recorded execution time for capability: {} - Duration: {}ms",
                        capability, executionTime.toMillis());
            });
    }
    
    /**
     * Gets current health score for a specific capability
     */
    public Double getCapabilityHealthScore(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getHealthScore)
            .orElse(0.0);
    }
    
    /**
     * Gets success rate for a specific capability
     */
    public Double getCapabilitySuccessRate(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getSuccessRate)
            .orElse(0.0);
    }

    /**
     * Gets average execution time for a specific capability
     */
    public Double getCapabilityAverageExecutionTime(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getAverageExecutionTime)
            .orElse(0.0);
    }

    /**
     * Gets the last error message for a specific capability
     */
    public String getCapabilityLastError(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getLastError)
            .orElse(null);
    }

    /**
     * Gets the last execution time for a specific capability
     */
    public LocalDateTime getCapabilityLastExecution(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getLastExecution)
            .orElse(null);
    }
    
    /**
     * Calculates overall agent health score across all capabilities
     */
    public Double calculateOverallHealthScore() {
        return Optional.of(capabilityMetrics)
            .filter(metrics -> !metrics.isEmpty())
            .map(metrics -> {
                double totalHealth = metrics.values().stream()
                    .mapToDouble(CapabilityMetrics::getHealthScore)
                    .sum();
                double overallHealth = totalHealth / metrics.size();
                log.debug("Calculated overall health score: {}", overallHealth);
                return overallHealth;
            })
            .orElse(0.0);
    }
    
    /**
     * Gets performance summary for all capabilities
     */
    public Map<String, String> getPerformanceSummary() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        
        capabilityMetrics.forEach((capability, metrics) -> {
            summary.put(capability, String.format(
                "Success Rate: %.2f%%, Avg Time: %.2fms, Health: %.2f",
                metrics.getSuccessRate() * 100,
                metrics.getAverageExecutionTime(),
                metrics.getHealthScore()
            ));
        });
        
        return summary;
    }
    
    /**
     * Resets metrics for a specific capability
     */
    public void resetCapabilityMetrics(String capability) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.reset();
                log.info("Reset metrics for capability: {}", capability);
            });
    }
    
    /**
     * Initializes a new capability with default metrics
     */
    private void initializeCapability(String capability) {
        capabilityMetrics.put(capability, new CapabilityMetrics(capability));
        log.debug("Initialized capability: {}", capability);
    }
    
    /**
     * Internal class to track metrics for each capability
     */
    @Getter
    private static class CapabilityMetrics {
        private final String capabilityName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private volatile LocalDateTime lastExecution = LocalDateTime.now();
        private volatile String lastError = null;
        
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
        
        public void recordExecutionTime(Duration executionTime) {
            totalExecutionTime.addAndGet(executionTime.toMillis());
            executionCount.incrementAndGet();
        }
        
        public Double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return total > 0 ? (double) successCount.get() / total : 1.0;
        }
        
        public Double getAverageExecutionTime() {
            int count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
        
        public Double getHealthScore() {
            double successRate = getSuccessRate();
            double avgTime = getAverageExecutionTime();
            double recency = getRecencyScore();
            
            // Health score based on success rate (60%), performance (25%), recency (15%)
            return (successRate * 0.60) + 
                   (getPerformanceScore(avgTime) * 0.25) + 
                   (recency * 0.15);
        }
        
        private Double getRecencyScore() {
            Duration timeSinceLastExecution = Duration.between(lastExecution, LocalDateTime.now());
            long minutesSinceExecution = timeSinceLastExecution.toMinutes();

            // Score decreases over time, 1.0 if executed within 5 minutes
            return java.util.stream.Stream.of(
                    Map.entry(5L, 1.0),
                    Map.entry(30L, 0.8),
                    Map.entry(120L, 0.6),
                    Map.entry(360L, 0.4)
                )
                .filter(entry -> minutesSinceExecution <= entry.getKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0.2);
        }
        
        private Double getPerformanceScore(double avgTimeMs) {
            // Authentication-specific performance thresholds using constants
            return java.util.stream.Stream.of(
                    Map.entry((double) AuthConstants.PERFORMANCE_EXCELLENT_MS, 1.0),  // Excellent for authentication
                    Map.entry((double) AuthConstants.PERFORMANCE_GOOD_MS, 0.9),       // Good for security operations
                    Map.entry((double) AuthConstants.PERFORMANCE_AVERAGE_MS, 0.7),    // Average for MFA operations
                    Map.entry((double) AuthConstants.PERFORMANCE_POOR_MS, 0.5)        // Poor for session management
                )
                .filter(entry -> avgTimeMs <= entry.getKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0.2);  // Very poor
        }
        

        public void reset() {
            successCount.set(0);
            failureCount.set(0);
            totalExecutionTime.set(0);
            executionCount.set(0);
            lastExecution = LocalDateTime.now();
            lastError = null;
        }
    }
}