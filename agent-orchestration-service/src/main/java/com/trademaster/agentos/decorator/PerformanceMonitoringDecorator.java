package com.trademaster.agentos.decorator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * ✅ FUNCTIONAL: Performance Monitoring Decorator
 * 
 * Decorates agent operations with performance monitoring and metrics collection.
 * Tracks execution time, success rates, and performance analytics.
 * 
 * Features:
 * - Non-invasive performance tracking
 * - Structured logging integration
 * - Automatic metrics collection
 * - Zero performance impact when disabled
 */
@Component
@RequiredArgsConstructor
public class PerformanceMonitoringDecorator implements AgentServiceDecorator {
    
    private final StructuredLoggingService structuredLogger;
    
    @Override
    public <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
            java.util.function.Function<Agent, Result<T, AgentError>> operation) {
        
        return agent -> {
            Instant startTime = Instant.now();
            String operationId = "op_" + System.nanoTime();
            
            structuredLogger.logDebug("performance_monitoring_started", 
                Map.of("agentId", agent.getAgentId(),
                       "operationId", operationId,
                       "startTime", startTime));
            
            return operation.apply(agent)
                .onSuccess(result -> logPerformanceSuccess(agent, operationId, startTime, result))
                .onFailure(error -> logPerformanceFailure(agent, operationId, startTime, error));
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Log successful operation performance
     */
    private <T> void logPerformanceSuccess(Agent agent, String operationId, Instant startTime, T result) {
        long executionTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        structuredLogger.logInfo("performance_monitoring_success", 
            Map.of("agentId", agent.getAgentId(),
                   "operationId", operationId,
                   "executionTimeMs", executionTimeMs,
                   "resultType", result.getClass().getSimpleName(),
                   "agentStatus", agent.getStatus().toString(),
                   "agentLoad", agent.getCurrentLoad()));
        
        // Update agent performance metrics
        updateAgentPerformanceMetrics(agent, executionTimeMs, true);
    }
    
    /**
     * ✅ FUNCTIONAL: Log failed operation performance
     */
    private void logPerformanceFailure(Agent agent, String operationId, Instant startTime, AgentError error) {
        long executionTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        
        structuredLogger.logWarning("performance_monitoring_failure", 
            Map.of("agentId", agent.getAgentId(),
                   "operationId", operationId,
                   "executionTimeMs", executionTimeMs,
                   "errorType", error.getClass().getSimpleName(),
                   "errorMessage", error.getMessage(),
                   "agentStatus", agent.getStatus().toString(),
                   "agentLoad", agent.getCurrentLoad()));
        
        // Update agent performance metrics
        updateAgentPerformanceMetrics(agent, executionTimeMs, false);
    }
    
    /**
     * ✅ FUNCTIONAL: Update agent performance metrics
     */
    private void updateAgentPerformanceMetrics(Agent agent, long executionTimeMs, boolean success) {
        // In a real implementation, this would update metrics in a time-series database
        // For now, we'll use structured logging to track performance trends
        structuredLogger.logDebug("agent_performance_metrics", 
            Map.of("agentId", agent.getAgentId(),
                   "executionTimeMs", executionTimeMs,
                   "success", success,
                   "currentSuccessRate", agent.getSuccessRate(),
                   "averageResponseTime", agent.getAverageResponseTime(),
                   "totalTasksCompleted", agent.getTotalTasksCompleted()));
    }
    
    /**
     * ✅ FUNCTIONAL: Create performance monitoring decorator instance
     */
    public static AgentServiceDecorator create(StructuredLoggingService logger) {
        return new PerformanceMonitoringDecorator(logger);
    }
}