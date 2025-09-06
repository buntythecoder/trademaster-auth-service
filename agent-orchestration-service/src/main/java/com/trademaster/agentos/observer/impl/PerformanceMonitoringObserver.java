package com.trademaster.agentos.observer.impl;

import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.observer.AgentEvent;
import com.trademaster.agentos.observer.AgentEventObserver;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ FUNCTIONAL: Performance Monitoring Observer
 * 
 * Observes agent events to track and analyze performance metrics.
 * Implements intelligent performance trend analysis and alerting.
 * 
 * Features:
 * - Real-time performance metric tracking
 * - Performance trend analysis
 * - Automatic performance alert generation
 * - Performance degradation detection
 */
@Component
@RequiredArgsConstructor
public class PerformanceMonitoringObserver implements AgentEventObserver {
    
    private final StructuredLoggingService structuredLogger;
    private final Map<Long, PerformanceMetrics> agentMetrics = new ConcurrentHashMap<>();
    
    // Performance thresholds
    private static final long SLOW_RESPONSE_THRESHOLD_MS = 5000;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 0.8;
    private static final int PERFORMANCE_WINDOW_MINUTES = 10;
    
    @Override
    public Result<Void, AgentError> handleEvent(AgentEvent event) {
        // ✅ FUNCTIONAL: Replace if-else with functional patterns, keep minimal try-catch
        try {
            return java.util.Optional.of(event)
                .filter(this::isPerformanceRelevant)
                .map(this::processPerformanceEvent)
                .orElse(Result.success(null));
        } catch (Exception e) {
            return Result.failure(new AgentError.CommunicationFailed(
                event.getAgentId(), "performance_monitoring", 
                "Failed to process performance event: " + e.getMessage()));
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Process performance-relevant event
     */
    private Result<Void, AgentError> processPerformanceEvent(AgentEvent event) {
        Long agentId = event.getAgentId();
        PerformanceMetrics metrics = agentMetrics.computeIfAbsent(agentId, 
            k -> new PerformanceMetrics(agentId, event.getAgentName()));
        
        // Update metrics based on event type
        updateMetricsFromEvent(metrics, event);
        
        // Analyze performance and generate alerts if needed
        analyzePerformanceAndAlert(metrics, event);
        
        structuredLogger.logDebug("performance_event_processed", 
            Map.of("agentId", agentId,
                   "eventType", event.getEventType(),
                   "currentAvgResponseTime", metrics.getAverageResponseTime(),
                   "currentSuccessRate", metrics.getSuccessRate()));
        
        return Result.success(null);
    }
    
    /**
     * ✅ FUNCTIONAL: Check if event is relevant for performance monitoring
     */
    private boolean isPerformanceRelevant(AgentEvent event) {
        return switch (event.getEventType()) {
            case TASK_ASSIGNED, TASK_COMPLETED, TASK_FAILED,
                 PERFORMANCE_DEGRADATION, PERFORMANCE_IMPROVED,
                 STATUS_CHANGED, HEARTBEAT_RECEIVED, HEARTBEAT_MISSED -> true;
            default -> false;
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Update performance metrics from event
     */
    private void updateMetricsFromEvent(PerformanceMetrics metrics, AgentEvent event) {
        switch (event.getEventType()) {
            case TASK_COMPLETED -> {
                metrics.recordTaskCompletion(getExecutionTimeFromEvent(event), true);
            }
            case TASK_FAILED -> {
                metrics.recordTaskCompletion(getExecutionTimeFromEvent(event), false);
            }
            case PERFORMANCE_DEGRADATION -> {
                metrics.recordPerformanceDegradation();
            }
            case PERFORMANCE_IMPROVED -> {
                metrics.recordPerformanceImprovement();
            }
            case HEARTBEAT_RECEIVED -> {
                metrics.recordHeartbeat();
            }
            case HEARTBEAT_MISSED -> {
                metrics.recordMissedHeartbeat();
            }
            case STATUS_CHANGED -> {
                metrics.recordStatusChange(event.getPreviousStatus(), event.getCurrentStatus());
            }
        }
    }
    
    /**
     * ✅ FUNCTIONAL: Analyze performance and generate alerts using Optional chains
     */
    private void analyzePerformanceAndAlert(PerformanceMetrics metrics, AgentEvent event) {
        // Check for performance degradation
        java.util.Optional.of(metrics.getAverageResponseTime())
            .filter(responseTime -> responseTime > SLOW_RESPONSE_THRESHOLD_MS)
            .ifPresent(responseTime -> generatePerformanceAlert(metrics, "slow_response_time", 
                Map.of("averageResponseTime", responseTime, "threshold", SLOW_RESPONSE_THRESHOLD_MS)));
        
        // Check for low success rate
        java.util.Optional.of(metrics.getSuccessRate())
            .filter(successRate -> successRate < LOW_SUCCESS_RATE_THRESHOLD)
            .ifPresent(successRate -> generatePerformanceAlert(metrics, "low_success_rate", 
                Map.of("successRate", successRate, "threshold", LOW_SUCCESS_RATE_THRESHOLD)));
        
        // Check for consecutive failures
        java.util.Optional.of(metrics.getConsecutiveFailures())
            .filter(failures -> failures >= 3)
            .ifPresent(failures -> generatePerformanceAlert(metrics, "consecutive_failures", 
                Map.of("consecutiveFailures", failures)));
        
        // Check for missed heartbeats
        java.util.Optional.of(metrics.getMissedHeartbeats())
            .filter(missed -> missed >= 2)
            .ifPresent(missed -> generatePerformanceAlert(metrics, "missed_heartbeats", 
                Map.of("missedHeartbeats", missed)));
    }
    
    /**
     * ✅ FUNCTIONAL: Generate performance alert
     */
    private void generatePerformanceAlert(PerformanceMetrics metrics, String alertType, 
                                        Map<String, Object> alertData) {
        
        Map<String, Object> alertContext = new java.util.HashMap<>(alertData);
        alertContext.put("agentId", metrics.getAgentId());
        alertContext.put("agentName", metrics.getAgentName());
        alertContext.put("alertType", alertType);
        alertContext.put("timestamp", Instant.now());
        alertContext.put("totalTasks", metrics.getTotalTasks());
        alertContext.put("recentPerformanceTrend", metrics.getPerformanceTrend());
        
        structuredLogger.logWarning("performance_alert", alertContext);
    }
    
    /**
     * ✅ FUNCTIONAL: Extract execution time from event metadata
     */
    private long getExecutionTimeFromEvent(AgentEvent event) {
        return event.getMetadata().containsKey("executionTime") ?
            ((Number) event.getMetadata().get("executionTime")).longValue() : 0L;
    }
    
    /**
     * ✅ FUNCTIONAL: Get performance metrics for agent using Optional
     */
    public Result<PerformanceMetrics, AgentError> getPerformanceMetrics(Long agentId) {
        return java.util.Optional.ofNullable(agentMetrics.get(agentId))
            .<Result<PerformanceMetrics, AgentError>>map(Result::success)
            .orElse(Result.failure(new AgentError.NotFound(agentId, "performance_metrics")));
    }
    
    /**
     * ✅ FUNCTIONAL: Get all performance metrics
     */
    public Result<Map<Long, PerformanceMetrics>, AgentError> getAllPerformanceMetrics() {
        return Result.success(new java.util.HashMap<>(agentMetrics));
    }
    
    /**
     * ✅ FUNCTIONAL: Clear performance metrics for agent
     */
    public Result<Void, AgentError> clearMetrics(Long agentId) {
        agentMetrics.remove(agentId);
        structuredLogger.logInfo("performance_metrics_cleared", Map.of("agentId", agentId));
        return Result.success(null);
    }
    
    /**
     * ✅ FUNCTIONAL: Performance Metrics Data Structure
     */
    public static class PerformanceMetrics {
        private final Long agentId;
        private final String agentName;
        private final Instant createdAt;
        
        private long totalTasks = 0;
        private long completedTasks = 0;
        private long failedTasks = 0;
        private long totalExecutionTime = 0;
        private int consecutiveFailures = 0;
        private int missedHeartbeats = 0;
        private Instant lastHeartbeat;
        private Instant lastTaskCompletion;
        
        public PerformanceMetrics(Long agentId, String agentName) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.createdAt = Instant.now();
            this.lastHeartbeat = Instant.now();
        }
        
        public synchronized void recordTaskCompletion(long executionTime, boolean success) {
            totalTasks++;
            totalExecutionTime += executionTime;
            lastTaskCompletion = Instant.now();
            
            // ✅ FUNCTIONAL: Replace if-else with functional approach
            java.util.Optional.of(success)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                    s -> {
                        completedTasks++;
                        consecutiveFailures = 0;
                    },
                    () -> {
                        failedTasks++;
                        consecutiveFailures++;
                    }
                );
        }
        
        public synchronized void recordHeartbeat() {
            lastHeartbeat = Instant.now();
            missedHeartbeats = 0;
        }
        
        public synchronized void recordMissedHeartbeat() {
            missedHeartbeats++;
        }
        
        public synchronized void recordPerformanceDegradation() {
            // Implementation for performance degradation tracking
        }
        
        public synchronized void recordPerformanceImprovement() {
            // Implementation for performance improvement tracking
        }
        
        public synchronized void recordStatusChange(com.trademaster.agentos.domain.entity.AgentStatus from,
                                                   com.trademaster.agentos.domain.entity.AgentStatus to) {
            // Implementation for status change tracking
        }
        
        // Getters
        public Long getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public Instant getCreatedAt() { return createdAt; }
        public long getTotalTasks() { return totalTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public long getFailedTasks() { return failedTasks; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public int getMissedHeartbeats() { return missedHeartbeats; }
        public Instant getLastHeartbeat() { return lastHeartbeat; }
        public Instant getLastTaskCompletion() { return lastTaskCompletion; }
        
        public double getSuccessRate() {
            return totalTasks > 0 ? (double) completedTasks / totalTasks : 1.0;
        }
        
        public long getAverageResponseTime() {
            return completedTasks > 0 ? totalExecutionTime / completedTasks : 0;
        }
        
        public String getPerformanceTrend() {
            // ✅ FUNCTIONAL: Replace if-else chain with pattern matching approach
            return java.util.Optional.of(consecutiveFailures)
                .filter(failures -> failures > 2)
                .map(failures -> "DECLINING")
                .orElse(
                    java.util.Optional.of(getSuccessRate())
                        .filter(rate -> rate > 0.95)
                        .map(rate -> "EXCELLENT")
                        .orElse(
                            java.util.Optional.of(getSuccessRate())
                                .filter(rate -> rate > 0.85)
                                .map(rate -> "GOOD")
                                .orElse("NEEDS_ATTENTION")
                        )
                );
        }
        
        public long getUptimeMinutes() {
            return ChronoUnit.MINUTES.between(createdAt, Instant.now());
        }
    }
}