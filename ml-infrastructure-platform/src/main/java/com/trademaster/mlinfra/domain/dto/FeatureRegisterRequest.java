package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Feature Register Request
 * 
 * Request DTO for registering features following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record FeatureRegisterRequest(
    @NotBlank(message = "Feature name cannot be blank")
    @Size(max = 255, message = "Feature name cannot exceed 255 characters")
    String featureName,
    
    @NotBlank(message = "Feature version cannot be blank")
    @Size(max = 50, message = "Feature version cannot exceed 50 characters")
    String featureVersion,
    
    @Size(max = 1000, message = "Definition cannot exceed 1000 characters")
    String definition,
    
    @NotBlank(message = "Data type cannot be blank")
    String dataType,
    
    @NotBlank(message = "Computation type cannot be blank")
    String computationType,
    
    Map<String, Object> validationRules,
    
    Map<String, String> tags,
    
    @NotBlank(message = "Owner cannot be blank")
    String owner
) {
    
    /**
     * Factory method with minimal parameters
     */
    public static FeatureRegisterRequest minimal(String featureName, String dataType, String owner) {
        return new FeatureRegisterRequest(
            featureName,
            "1.0.0",
            null,
            dataType,
            "BATCH",
            Map.of(),
            Map.of(),
            owner
        );
    }
    
    /**
     * Factory method with definition
     */
    public static FeatureRegisterRequest withDefinition(
            String featureName, 
            String definition, 
            String dataType, 
            String owner) {
        return new FeatureRegisterRequest(
            featureName,
            "1.0.0",
            definition,
            dataType,
            "BATCH",
            Map.of(),
            Map.of(),
            owner
        );
    }
    
    /**
     * Add validation rules to request
     */
    public FeatureRegisterRequest withValidationRules(Map<String, Object> rules) {
        Map<String, Object> allRules = new java.util.HashMap<>(this.validationRules != null ? this.validationRules : Map.of());
        allRules.putAll(rules);
        
        return new FeatureRegisterRequest(
            featureName,
            featureVersion,
            definition,
            dataType,
            computationType,
            Map.copyOf(allRules),
            tags,
            owner
        );
    }
    
    /**
     * Set computation type
     */
    public FeatureRegisterRequest withComputationType(String computationType) {
        return new FeatureRegisterRequest(
            featureName,
            featureVersion,
            definition,
            dataType,
            computationType,
            validationRules,
            tags,
            owner
        );
    }
}