package com.trademaster.mlinfra.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Model Prediction Request
 * 
 * Represents a request for model inference:
 * - Model identification and version
 * - Input features for prediction
 * - Request metadata and configuration
 * - Explanation and debugging options
 */
public record PredictionRequest(
    @NotBlank(message = "Model name is required")
    String modelName,
    
    String modelVersion, // Optional, defaults to latest production version
    
    @NotNull(message = "Features are required")
    Map<String, Object> features,
    
    String requestId, // Optional, auto-generated if not provided
    
    String userId, // Optional, for tracking and personalization
    
    String sessionId, // Optional, for session-based tracking
    
    Boolean explainPrediction, // Optional, defaults to false
    
    Map<String, Object> requestMetadata // Optional, additional context
) {

    /**
     * Create builder for prediction request
     */
    public static PredictionRequestBuilder builder() {
        return new PredictionRequestBuilder();
    }

    /**
     * Get model version or default to "latest"
     */
    public String getModelVersionOrDefault() {
        return modelVersion != null ? modelVersion : "latest";
    }

    /**
     * Check if explanation is requested
     */
    public boolean shouldExplain() {
        return Boolean.TRUE.equals(explainPrediction);
    }

    /**
     * Get request ID or generate one if null
     */
    public String getRequestIdOrGenerate() {
        return requestId != null ? requestId : java.util.UUID.randomUUID().toString();
    }

    /**
     * Check if request has metadata
     */
    public boolean hasMetadata() {
        return requestMetadata != null && !requestMetadata.isEmpty();
    }

    /**
     * Get feature count
     */
    public int getFeatureCount() {
        return features != null ? features.size() : 0;
    }

    /**
     * Get name (alias for modelName)
     */
    public String name() {
        return modelName;
    }

    /**
     * Get feature IDs from features map keys
     */
    public java.util.Set<String> featureIds() {
        return features != null ? features.keySet() : java.util.Set.of();
    }

    public static class PredictionRequestBuilder {
        private String modelName;
        private String modelVersion;
        private Map<String, Object> features;
        private String requestId;
        private String userId;
        private String sessionId;
        private Boolean explainPrediction = false;
        private Map<String, Object> requestMetadata;

        public PredictionRequestBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public PredictionRequestBuilder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public PredictionRequestBuilder features(Map<String, Object> features) {
            this.features = features;
            return this;
        }

        public PredictionRequestBuilder feature(String key, Object value) {
            if (this.features == null) {
                this.features = new java.util.HashMap<>();
            }
            this.features.put(key, value);
            return this;
        }

        public PredictionRequestBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public PredictionRequestBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public PredictionRequestBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public PredictionRequestBuilder explainPrediction(Boolean explainPrediction) {
            this.explainPrediction = explainPrediction;
            return this;
        }

        public PredictionRequestBuilder requestMetadata(Map<String, Object> requestMetadata) {
            this.requestMetadata = requestMetadata;
            return this;
        }

        public PredictionRequestBuilder addMetadata(String key, Object value) {
            if (this.requestMetadata == null) {
                this.requestMetadata = new java.util.HashMap<>();
            }
            this.requestMetadata.put(key, value);
            return this;
        }

        public PredictionRequest build() {
            // Generate request ID if not provided
            if (requestId == null) {
                requestId = java.util.UUID.randomUUID().toString();
            }

            return new PredictionRequest(
                modelName, modelVersion, features, requestId,
                userId, sessionId, explainPrediction, requestMetadata
            );
        }
    }
}