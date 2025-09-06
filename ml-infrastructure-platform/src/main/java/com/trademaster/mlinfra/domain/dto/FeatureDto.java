package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Feature DTO
 * 
 * Data transfer object for feature store information following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record FeatureDto(
    @NotBlank(message = "Feature ID cannot be blank")
    String featureId,
    
    @NotBlank(message = "Feature name cannot be blank")
    String name,
    
    @NotNull(message = "Feature type cannot be null")
    FeatureType type,
    
    String description,
    
    @NotBlank(message = "Owner cannot be blank")
    String owner,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant createdAt,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant updatedAt,
    
    String version,
    
    Map<String, Object> schema,
    
    Map<String, String> tags,
    
    @NotNull(message = "Status cannot be null")
    FeatureStatus status,
    
    String dataSource,
    
    Map<String, Object> computeConfig
) {
    
    /**
     * Feature type enumeration
     */
    public enum FeatureType {
        CONTINUOUS, CATEGORICAL, BINARY, TEXT, TIMESTAMP
    }
    
    /**
     * Feature status enumeration
     */
    public enum FeatureStatus {
        ACTIVE, DEPRECATED, EXPERIMENTAL, ARCHIVED
    }
    
    /**
     * Factory method for new feature
     */
    public static FeatureDto newFeature(String name, FeatureType type, String owner, String description) {
        return new FeatureDto(
            java.util.UUID.randomUUID().toString(),
            name,
            type,
            description,
            owner,
            Instant.now(),
            Instant.now(),
            "1.0.0",
            Map.of(),
            Map.of(),
            FeatureStatus.ACTIVE,
            null,
            Map.of()
        );
    }
    
    /**
     * Check if feature is active
     */
    public boolean isActive() {
        return status == FeatureStatus.ACTIVE;
    }
    
    /**
     * Check if feature is deprecated
     */
    public boolean isDeprecated() {
        return status == FeatureStatus.DEPRECATED || status == FeatureStatus.ARCHIVED;
    }
    
    /**
     * Update feature with new version
     */
    public FeatureDto withNewVersion(String newVersion) {
        return new FeatureDto(
            featureId,
            name,
            type,
            description,
            owner,
            createdAt,
            Instant.now(),
            newVersion,
            schema,
            tags,
            status,
            dataSource,
            computeConfig
        );
    }

    /**
     * Create FeatureDto from FeatureDefinition entity
     */
    public static FeatureDto fromEntity(com.trademaster.mlinfra.domain.entity.FeatureDefinition entity) {
        return new FeatureDto(
            entity.getFeatureIdentifier(),
            entity.getFeatureName(),
            FeatureType.CONTINUOUS, // Default feature type
            entity.getDescription(),
            entity.getCreatedBy(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : java.time.Instant.now(),
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : java.time.Instant.now(),
            entity.getFeatureVersion(),
            entity.getMetadata() != null ? Map.copyOf(entity.getMetadata()) : Map.of(),
            Map.of(), // No tags field in entity
            entity.getIsActive() ? FeatureStatus.ACTIVE : FeatureStatus.ARCHIVED,
            entity.getDataType(),
            Map.of("computationType", entity.getComputationType()) // Map compute type to config
        );
    }
}