package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Model Promote Request
 * 
 * Request DTO for promoting ML models following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record ModelPromoteRequest(
    @NotBlank(message = "Model name cannot be blank")
    String modelName,
    
    @NotNull(message = "Stage cannot be null")
    ModelStage stage,
    
    String version,
    
    Map<String, String> tags,
    
    String description,
    
    Boolean archiveExistingVersions
) {
    
    /**
     * Model stage enumeration
     */
    public enum ModelStage {
        STAGING, PRODUCTION, ARCHIVED
    }
    
    /**
     * Factory method for staging
     */
    public static ModelPromoteRequest toStaging(String modelName, String version) {
        return new ModelPromoteRequest(
            modelName,
            ModelStage.STAGING,
            version,
            Map.of(),
            null,
            false
        );
    }
    
    /**
     * Factory method for production
     */
    public static ModelPromoteRequest toProduction(String modelName, String version) {
        return new ModelPromoteRequest(
            modelName,
            ModelStage.PRODUCTION,
            version,
            Map.of("promoted_at", java.time.Instant.now().toString()),
            "Promoted to production",
            true
        );
    }
    
    /**
     * Factory method for archiving
     */
    public static ModelPromoteRequest toArchived(String modelName, String version, String reason) {
        return new ModelPromoteRequest(
            modelName,
            ModelStage.ARCHIVED,
            version,
            Map.of("archived_reason", reason),
            reason,
            false
        );
    }
    
    /**
     * Add tags to request
     */
    public ModelPromoteRequest withTags(Map<String, String> additionalTags) {
        Map<String, String> allTags = new java.util.HashMap<>(this.tags != null ? this.tags : Map.of());
        allTags.putAll(additionalTags);
        
        return new ModelPromoteRequest(
            modelName,
            stage,
            version,
            Map.copyOf(allTags),
            description,
            archiveExistingVersions
        );
    }
}