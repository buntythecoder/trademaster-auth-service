package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Run Start Request
 * 
 * Request DTO for starting ML runs following TradeMaster standards.
 * Uses Records for immutability and functional programming principles.
 */
public record RunStartRequest(
    @NotBlank(message = "Experiment ID cannot be blank")
    String experimentId,
    
    @NotBlank(message = "User ID cannot be blank")
    String userId,
    
    @Size(max = 255, message = "Run name cannot exceed 255 characters")
    String runName,
    
    Map<String, String> tags,
    
    Map<String, Object> params,
    
    String sourceType,
    String sourceName,
    String sourceVersion,
    String entryPoint,
    
    Long startTime
) {
    
    /**
     * Factory method with minimal parameters
     */
    public static RunStartRequest minimal(String experimentId, String userId) {
        return new RunStartRequest(
            experimentId,
            userId,
            null,
            Map.of(),
            Map.of(),
            "LOCAL",
            null,
            null,
            null,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Factory method with run name
     */
    public static RunStartRequest withName(String experimentId, String userId, String runName) {
        return new RunStartRequest(
            experimentId,
            userId,
            runName,
            Map.of(),
            Map.of(),
            "LOCAL",
            null,
            null,
            null,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Add parameters to request
     */
    public RunStartRequest withParams(Map<String, Object> additionalParams) {
        Map<String, Object> allParams = new java.util.HashMap<>(this.params != null ? this.params : Map.of());
        allParams.putAll(additionalParams);
        
        return new RunStartRequest(
            experimentId,
            userId,
            runName,
            tags,
            Map.copyOf(allParams),
            sourceType,
            sourceName,
            sourceVersion,
            entryPoint,
            startTime
        );
    }
    
    /**
     * Add tags to request
     */
    public RunStartRequest withTags(Map<String, String> additionalTags) {
        Map<String, String> allTags = new java.util.HashMap<>(this.tags != null ? this.tags : Map.of());
        allTags.putAll(additionalTags);
        
        return new RunStartRequest(
            experimentId,
            userId,
            runName,
            Map.copyOf(allTags),
            params,
            sourceType,
            sourceName,
            sourceVersion,
            entryPoint,
            startTime
        );
    }
}