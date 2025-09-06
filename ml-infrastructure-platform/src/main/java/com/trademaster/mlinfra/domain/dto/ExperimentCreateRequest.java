package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Experiment Create Request
 * 
 * Request DTO for creating ML experiments following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record ExperimentCreateRequest(
    @NotBlank(message = "Experiment name cannot be blank")
    @Size(max = 255, message = "Experiment name cannot exceed 255 characters")
    String name,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,
    
    Map<String, String> tags,
    
    @NotBlank(message = "User ID cannot be blank")
    String userId,
    
    String artifactLocation
) {
    
    /**
     * Factory method with minimal parameters
     */
    public static ExperimentCreateRequest minimal(String name, String userId) {
        return new ExperimentCreateRequest(
            name,
            null,
            Map.of(),
            userId,
            null
        );
    }
    
    /**
     * Factory method with description
     */
    public static ExperimentCreateRequest withDescription(String name, String description, String userId) {
        return new ExperimentCreateRequest(
            name,
            description,
            Map.of(),
            userId,
            null
        );
    }
    
    /**
     * Add tags to request
     */
    public ExperimentCreateRequest withTags(Map<String, String> additionalTags) {
        Map<String, String> allTags = new java.util.HashMap<>(this.tags != null ? this.tags : Map.of());
        allTags.putAll(additionalTags);
        
        return new ExperimentCreateRequest(
            name,
            description,
            Map.copyOf(allTags),
            userId,
            artifactLocation
        );
    }
}