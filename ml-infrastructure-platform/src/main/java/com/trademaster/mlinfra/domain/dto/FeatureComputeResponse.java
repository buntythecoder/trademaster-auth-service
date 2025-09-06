package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * Feature Compute Response
 * 
 * Response DTO for computed features following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record FeatureComputeResponse(
    String requestId,
    
    Map<String, Object> features,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant computedAt,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant validUntil,
    
    Map<String, Object> metadata,
    
    Boolean fromCache,
    
    Long computationTimeMs
) {
    
    /**
     * Factory method for successful computation
     */
    public static FeatureComputeResponse success(
            String requestId, 
            Map<String, Object> features, 
            boolean fromCache,
            long computationTimeMs) {
        Instant now = Instant.now();
        return new FeatureComputeResponse(
            requestId,
            Map.copyOf(features),
            now,
            now.plusSeconds(3600), // 1 hour validity
            Map.of(
                "feature_count", features.size(),
                "computation_source", fromCache ? "cache" : "compute"
            ),
            fromCache,
            computationTimeMs
        );
    }
    
    /**
     * Factory method with custom validity
     */
    public static FeatureComputeResponse withValidity(
            String requestId, 
            Map<String, Object> features,
            Instant validUntil,
            boolean fromCache,
            long computationTimeMs) {
        return new FeatureComputeResponse(
            requestId,
            Map.copyOf(features),
            Instant.now(),
            validUntil,
            Map.of(
                "feature_count", features.size(),
                "computation_source", fromCache ? "cache" : "compute"
            ),
            fromCache,
            computationTimeMs
        );
    }
    
    /**
     * Check if response is still valid
     */
    public boolean isValid() {
        return validUntil != null && Instant.now().isBefore(validUntil);
    }
    
    /**
     * Get feature value by name
     */
    public <T> T getFeatureValue(String featureName, Class<T> type) {
        Object value = features.get(featureName);
        return type.cast(value);
    }

    /**
     * Get entity IDs from metadata (for backwards compatibility)
     */
    public Object entityIds() {
        return metadata != null ? metadata.get("entity_ids") : "unknown";
    }
}