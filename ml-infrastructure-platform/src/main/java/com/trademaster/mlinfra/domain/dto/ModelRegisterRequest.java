package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Model Register Request
 * 
 * Request DTO for registering ML models following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record ModelRegisterRequest(
    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 255, message = "Model name cannot exceed 255 characters")
    String name,
    
    @NotBlank(message = "Run ID cannot be blank")
    String runId,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,
    
    Map<String, String> tags,
    
    String artifactPath,
    
    String modelType,
    
    String framework,
    
    String version
) {
    
    /**
     * Factory method with minimal parameters
     */
    public static ModelRegisterRequest minimal(String name, String runId) {
        return new ModelRegisterRequest(
            name,
            runId,
            null,
            Map.of(),
            null,
            "sklearn",
            "scikit-learn",
            "1.0.0"
        );
    }
    
    /**
     * Factory method with description
     */
    public static ModelRegisterRequest withDescription(String name, String runId, String description) {
        return new ModelRegisterRequest(
            name,
            runId,
            description,
            Map.of(),
            null,
            "sklearn",
            "scikit-learn",
            "1.0.0"
        );
    }
    
    /**
     * Add tags to request
     */
    public ModelRegisterRequest withTags(Map<String, String> additionalTags) {
        Map<String, String> allTags = new java.util.HashMap<>(this.tags != null ? this.tags : Map.of());
        allTags.putAll(additionalTags);
        
        return new ModelRegisterRequest(
            name,
            runId,
            description,
            Map.copyOf(allTags),
            artifactPath,
            modelType,
            framework,
            version
        );
    }
    
    /**
     * Set framework and type
     */
    public ModelRegisterRequest withFramework(String framework, String modelType) {
        return new ModelRegisterRequest(
            name,
            runId,
            description,
            tags,
            artifactPath,
            modelType,
            framework,
            version
        );
    }
}