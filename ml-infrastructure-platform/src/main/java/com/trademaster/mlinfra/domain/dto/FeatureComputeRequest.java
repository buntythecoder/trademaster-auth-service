package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Feature Compute Request
 * 
 * Request DTO for computing features following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record FeatureComputeRequest(
    @NotNull(message = "Feature IDs cannot be null")
    List<String> featureIds,
    
    @NotNull(message = "Entity IDs cannot be null")
    Map<String, Object> entityIds,
    
    Instant timestamp,
    
    Map<String, Object> context,
    
    @NotBlank(message = "User ID cannot be blank")
    String userId,
    
    Boolean useCache
) {
    
    /**
     * Factory method for single feature
     */
    public static FeatureComputeRequest single(String featureId, Map<String, Object> entityIds, String userId) {
        return new FeatureComputeRequest(
            List.of(featureId),
            Map.copyOf(entityIds),
            Instant.now(),
            Map.of(),
            userId,
            true
        );
    }
    
    /**
     * Factory method for multiple features
     */
    public static FeatureComputeRequest multiple(List<String> featureIds, Map<String, Object> entityIds, String userId) {
        return new FeatureComputeRequest(
            List.copyOf(featureIds),
            Map.copyOf(entityIds),
            Instant.now(),
            Map.of(),
            userId,
            true
        );
    }
    
    /**
     * Add context to request
     */
    public FeatureComputeRequest withContext(Map<String, Object> additionalContext) {
        Map<String, Object> allContext = new java.util.HashMap<>(this.context != null ? this.context : Map.of());
        allContext.putAll(additionalContext);
        
        return new FeatureComputeRequest(
            featureIds,
            entityIds,
            timestamp,
            Map.copyOf(allContext),
            userId,
            useCache
        );
    }
    
    /**
     * Disable caching
     */
    public FeatureComputeRequest withoutCache() {
        return new FeatureComputeRequest(
            featureIds,
            entityIds,
            timestamp,
            context,
            userId,
            false
        );
    }
}