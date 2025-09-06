package com.trademaster.multibroker.dto;

import com.trademaster.multibroker.entity.BrokerType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Broker Health Summary DTO
 * 
 * MANDATORY: Immutable Record + Functional Composition + Zero Placeholders
 * 
 * Comprehensive health monitoring summary for broker connections. Provides
 * aggregated health metrics, performance indicators, and status information
 * for system monitoring and alerting purposes.
 * 
 * Health Categories:
 * - Overall system health status
 * - Individual broker health metrics
 * - Performance and latency indicators
 * - Error rates and failure patterns
 * - Capacity and throughput metrics
 * 
 * Monitoring Features:
 * - Real-time health scoring
 * - Trend analysis and predictions
 * - Alert threshold management
 * - Recovery recommendations
 * - Capacity planning insights
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Health Monitoring & Analytics)
 */
@Builder
public record BrokerHealthSummary(
    String userId,
    Instant generatedAt,
    Long totalConnections,
    Long healthyConnections,
    Long degradedConnections,
    Long failedConnections,
    Double overallHealthPercentage,
    String healthStatus,
    Long totalTradingVolume,
    Long totalErrorCount,
    Double averageResponseTime,
    Double maxResponseTime,
    Long uptimeSeconds,
    Map<BrokerType, BrokerHealthMetric> brokerMetrics,
    List<String> activeAlerts,
    List<String> recommendations,
    Instant lastFullHealthCheck,
    Boolean requiresAttention,
    String primaryIssue,
    Integer confidenceScore
) {
    
    /**
     * Individual Broker Health Metric
     */
    @Builder
    public record BrokerHealthMetric(
        BrokerType brokerType,
        Boolean isHealthy,
        Double healthScore,
        Long responseTimeMs,
        Long errorCount,
        Instant lastHealthCheck,
        String status,
        String issue
    ) {
        
        /**
         * Check if broker is performing well
         * 
         * @return true if performance is good
         */
        public boolean isPerformingWell() {
            return Boolean.TRUE.equals(isHealthy) &&
                   healthScore != null && healthScore > 80.0 &&
                   responseTimeMs != null && responseTimeMs < 1000;
        }
        
        /**
         * Get performance tier
         * 
         * @return performance level (EXCELLENT, GOOD, FAIR, POOR)
         */
        public String getPerformanceTier() {
            if (responseTimeMs == null || !Boolean.TRUE.equals(isHealthy)) {
                return "POOR";
            }
            
            return switch (responseTimeMs.intValue()) {
                case int ms when ms < 200 -> "EXCELLENT";
                case int ms when ms < 1000 -> "GOOD";
                case int ms when ms < 3000 -> "FAIR";
                default -> "POOR";
            };
        }
    }
    
    /**
     * Check if system is in good health
     * 
     * @return true if overall health is good
     */
    public boolean isSystemHealthy() {
        return overallHealthPercentage != null &&
               overallHealthPercentage >= 90.0 &&
               "HEALTHY".equals(healthStatus);
    }
    
    /**
     * Check if system is in degraded state
     * 
     * @return true if system is degraded but functional
     */
    public boolean isSystemDegraded() {
        return overallHealthPercentage != null &&
               overallHealthPercentage >= 70.0 &&
               overallHealthPercentage < 90.0;
    }
    
    /**
     * Check if system is in critical state
     * 
     * @return true if system requires immediate attention
     */
    public boolean isSystemCritical() {
        return overallHealthPercentage != null &&
               overallHealthPercentage < 70.0 ||
               Boolean.TRUE.equals(requiresAttention);
    }
    
    /**
     * Get system status color code for UI
     * 
     * @return color code (GREEN, YELLOW, RED)
     */
    public String getStatusColor() {
        if (isSystemHealthy()) {
            return "GREEN";
        } else if (isSystemDegraded()) {
            return "YELLOW";
        } else {
            return "RED";
        }
    }
    
    /**
     * Calculate error rate percentage
     * 
     * @return error rate as percentage
     */
    public double getErrorRate() {
        if (totalTradingVolume == null || totalTradingVolume == 0L) {
            return 0.0;
        }
        
        long errors = totalErrorCount != null ? totalErrorCount : 0L;
        return (errors * 100.0) / totalTradingVolume;
    }
    
    /**
     * Get availability percentage
     * 
     * @return availability as percentage (0-100)
     */
    public double getAvailabilityPercentage() {
        if (totalConnections == null || totalConnections == 0L) {
            return 0.0;
        }
        
        long healthy = healthyConnections != null ? healthyConnections : 0L;
        long degraded = degradedConnections != null ? degradedConnections : 0L;
        long available = healthy + degraded;
        
        return (available * 100.0) / totalConnections;
    }
    
    /**
     * Get performance rating based on response times
     * 
     * @return performance rating (A, B, C, D, F)
     */
    public String getPerformanceRating() {
        if (averageResponseTime == null) {
            return "F";
        }
        
        return switch (averageResponseTime.intValue()) {
            case int ms when ms < 200 -> "A";
            case int ms when ms < 500 -> "B";
            case int ms when ms < 1000 -> "C";
            case int ms when ms < 3000 -> "D";
            default -> "F";
        };
    }
    
    /**
     * Get count of healthy brokers by type
     * 
     * @return count of healthy brokers
     */
    public long getHealthyBrokerCount() {
        if (brokerMetrics == null) {
            return 0L;
        }
        
        return brokerMetrics.values().stream()
            .mapToLong(metric -> Boolean.TRUE.equals(metric.isHealthy()) ? 1L : 0L)
            .sum();
    }
    
    /**
     * Get the worst performing broker
     * 
     * @return broker type with worst performance, or null if none
     */
    public BrokerType getWorstPerformingBroker() {
        if (brokerMetrics == null || brokerMetrics.isEmpty()) {
            return null;
        }
        
        return brokerMetrics.entrySet().stream()
            .min((e1, e2) -> {
                Double score1 = e1.getValue().healthScore();
                Double score2 = e2.getValue().healthScore();
                
                if (score1 == null && score2 == null) return 0;
                if (score1 == null) return 1;
                if (score2 == null) return -1;
                
                return score1.compareTo(score2);
            })
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get the best performing broker
     * 
     * @return broker type with best performance, or null if none
     */
    public BrokerType getBestPerformingBroker() {
        if (brokerMetrics == null || brokerMetrics.isEmpty()) {
            return null;
        }
        
        return brokerMetrics.entrySet().stream()
            .max((e1, e2) -> {
                Double score1 = e1.getValue().healthScore();
                Double score2 = e2.getValue().healthScore();
                
                if (score1 == null && score2 == null) return 0;
                if (score1 == null) return -1;
                if (score2 == null) return 1;
                
                return score1.compareTo(score2);
            })
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Check if health check data is stale
     * 
     * @return true if last health check is older than 15 minutes
     */
    public boolean isHealthDataStale() {
        if (lastFullHealthCheck == null) {
            return true;
        }
        
        Instant fifteenMinutesAgo = Instant.now().minusSeconds(900);
        return lastFullHealthCheck.isBefore(fifteenMinutesAgo);
    }
    
    /**
     * Get summary message for system status
     * 
     * @return human-readable status summary
     */
    public String getStatusSummary() {
        if (isSystemHealthy()) {
            return String.format("All systems operational (%d/%d brokers healthy)", 
                               healthyConnections, totalConnections);
        } else if (isSystemDegraded()) {
            return String.format("Degraded performance (%d/%d brokers healthy)", 
                               healthyConnections, totalConnections);
        } else {
            return String.format("System issues detected (%d/%d brokers failed)", 
                               failedConnections, totalConnections);
        }
    }
    
    /**
     * Get priority level for alerts
     * 
     * @return alert priority (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public String getAlertPriority() {
        if (isSystemCritical()) {
            return "CRITICAL";
        } else if (isSystemDegraded()) {
            return "HIGH";
        } else if (activeAlerts != null && !activeAlerts.isEmpty()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Create summary for dashboard display
     * 
     * @return dashboard-optimized summary
     */
    public BrokerHealthSummary forDashboard() {
        return BrokerHealthSummary.builder()
            .userId(userId)
            .generatedAt(generatedAt)
            .totalConnections(totalConnections)
            .healthyConnections(healthyConnections)
            .degradedConnections(degradedConnections)
            .failedConnections(failedConnections)
            .overallHealthPercentage(overallHealthPercentage)
            .healthStatus(healthStatus)
            .averageResponseTime(averageResponseTime)
            .activeAlerts(activeAlerts)
            .recommendations(recommendations)
            .requiresAttention(requiresAttention)
            .primaryIssue(primaryIssue)
            .confidenceScore(confidenceScore)
            .build();
    }
}