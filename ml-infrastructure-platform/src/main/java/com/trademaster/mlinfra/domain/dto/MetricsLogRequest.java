package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Metrics Log Request
 * 
 * Request DTO for logging ML metrics following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record MetricsLogRequest(
    @NotBlank(message = "Run ID cannot be blank")
    String runId,
    
    @NotNull(message = "Metrics cannot be null")
    @Size(min = 1, message = "At least one metric must be provided")
    Map<String, Double> metrics,
    
    Long timestamp,
    
    Integer step
) {
    
    /**
     * Factory method with current timestamp
     */
    public static MetricsLogRequest now(String runId, Map<String, Double> metrics) {
        return new MetricsLogRequest(
            runId,
            Map.copyOf(metrics),
            System.currentTimeMillis(),
            null
        );
    }
    
    /**
     * Factory method with step
     */
    public static MetricsLogRequest withStep(String runId, Map<String, Double> metrics, Integer step) {
        return new MetricsLogRequest(
            runId,
            Map.copyOf(metrics),
            System.currentTimeMillis(),
            step
        );
    }
    
    /**
     * Factory method for single metric
     */
    public static MetricsLogRequest single(String runId, String metricName, Double value) {
        return new MetricsLogRequest(
            runId,
            Map.of(metricName, value),
            System.currentTimeMillis(),
            null
        );
    }
    
    /**
     * Add additional metrics
     */
    public MetricsLogRequest withAdditionalMetrics(Map<String, Double> additionalMetrics) {
        Map<String, Double> allMetrics = new java.util.HashMap<>(this.metrics);
        allMetrics.putAll(additionalMetrics);
        
        return new MetricsLogRequest(
            runId,
            Map.copyOf(allMetrics),
            timestamp,
            step
        );
    }
}