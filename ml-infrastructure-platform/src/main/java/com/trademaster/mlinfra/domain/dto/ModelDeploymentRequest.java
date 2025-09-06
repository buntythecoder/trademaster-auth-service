package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Model Deployment Request
 * 
 * Request DTO for deploying ML models following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record ModelDeploymentRequest(
    @NotBlank(message = "Model name cannot be blank")
    String modelName,
    
    @NotBlank(message = "Model version cannot be blank")
    String modelVersion,
    
    @NotBlank(message = "Deployment name cannot be blank")
    String deploymentName,
    
    @NotNull(message = "Environment cannot be null")
    Environment environment,
    
    @Positive(message = "Replicas must be positive")
    Integer replicas,
    
    Map<String, String> resourceRequirements,
    
    Map<String, String> environmentVariables,
    
    Map<String, String> labels,
    
    String endpoint,
    
    Boolean autoScale
) {
    
    /**
     * Deployment environment enumeration
     */
    public enum Environment {
        DEVELOPMENT, STAGING, PRODUCTION
    }
    
    /**
     * Factory method for development deployment
     */
    public static ModelDeploymentRequest development(String modelName, String modelVersion, String deploymentName) {
        return new ModelDeploymentRequest(
            modelName,
            modelVersion,
            deploymentName,
            Environment.DEVELOPMENT,
            1,
            Map.of(
                "memory", "512Mi",
                "cpu", "250m"
            ),
            Map.of(),
            Map.of("env", "development"),
            null,
            false
        );
    }
    
    /**
     * Factory method for production deployment
     */
    public static ModelDeploymentRequest production(String modelName, String modelVersion, String deploymentName) {
        return new ModelDeploymentRequest(
            modelName,
            modelVersion,
            deploymentName,
            Environment.PRODUCTION,
            3,
            Map.of(
                "memory", "2Gi",
                "cpu", "1000m"
            ),
            Map.of(),
            Map.of(
                "env", "production",
                "monitoring", "enabled"
            ),
            null,
            true
        );
    }
    
    /**
     * Add resource requirements
     */
    public ModelDeploymentRequest withResources(String cpu, String memory) {
        Map<String, String> resources = new java.util.HashMap<>(this.resourceRequirements != null ? this.resourceRequirements : Map.of());
        resources.put("cpu", cpu);
        resources.put("memory", memory);
        
        return new ModelDeploymentRequest(
            modelName,
            modelVersion,
            deploymentName,
            environment,
            replicas,
            Map.copyOf(resources),
            environmentVariables,
            labels,
            endpoint,
            autoScale
        );
    }
    
    /**
     * Enable auto-scaling
     */
    public ModelDeploymentRequest withAutoScale() {
        return new ModelDeploymentRequest(
            modelName,
            modelVersion,
            deploymentName,
            environment,
            replicas,
            resourceRequirements,
            environmentVariables,
            labels,
            endpoint,
            true
        );
    }
}