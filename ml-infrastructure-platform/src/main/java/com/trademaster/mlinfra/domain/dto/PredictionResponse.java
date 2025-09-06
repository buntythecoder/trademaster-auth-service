package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model Prediction Response
 * 
 * Represents the result of a model inference request:
 * - Prediction result and confidence score
 * - Model information and performance metrics
 * - Request tracking and timing data
 * - Optional explanation and debugging info
 */
public record PredictionResponse(
    String predictionId,
    Object prediction,
    Double confidence,
    String modelName,
    String modelVersion,
    Integer latencyMs,
    Map<String, Object> explanation,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    String requestId,
    Map<String, Object> metadata
) {

    /**
     * Create response with current timestamp
     */
    public PredictionResponse(
            String predictionId,
            Object prediction,
            Double confidence,
            String modelName,
            String modelVersion,
            Integer latencyMs,
            Map<String, Object> explanation) {
        this(
            predictionId, prediction, confidence, modelName, modelVersion, 
            latencyMs, explanation, LocalDateTime.now(), null, null
        );
    }

    /**
     * Create builder for prediction response
     */
    public static PredictionResponseBuilder builder() {
        return new PredictionResponseBuilder();
    }

    /**
     * Check if prediction has high confidence
     */
    public boolean isHighConfidence(double threshold) {
        return confidence != null && confidence >= threshold;
    }

    /**
     * Check if prediction is low latency
     */
    public boolean isLowLatency(int maxLatencyMs) {
        return latencyMs != null && latencyMs <= maxLatencyMs;
    }

    /**
     * Check if response has explanation
     */
    public boolean hasExplanation() {
        return explanation != null && !explanation.isEmpty();
    }

    /**
     * Get confidence category
     */
    public String getConfidenceCategory() {
        if (confidence == null) {
            return "UNKNOWN";
        } else if (confidence >= 0.9) {
            return "VERY_HIGH";
        } else if (confidence >= 0.7) {
            return "HIGH";
        } else if (confidence >= 0.5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Get latency category
     */
    public String getLatencyCategory() {
        if (latencyMs == null) {
            return "UNKNOWN";
        } else if (latencyMs <= 50) {
            return "EXCELLENT";
        } else if (latencyMs <= 100) {
            return "GOOD";
        } else if (latencyMs <= 200) {
            return "ACCEPTABLE";
        } else {
            return "POOR";
        }
    }

    public static class PredictionResponseBuilder {
        private String predictionId;
        private Object prediction;
        private Double confidence;
        private String modelName;
        private String modelVersion;
        private Integer latencyMs;
        private Map<String, Object> explanation;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String requestId;
        private Map<String, Object> metadata;

        public PredictionResponseBuilder predictionId(String predictionId) {
            this.predictionId = predictionId;
            return this;
        }

        public PredictionResponseBuilder prediction(Object prediction) {
            this.prediction = prediction;
            return this;
        }

        public PredictionResponseBuilder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public PredictionResponseBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public PredictionResponseBuilder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public PredictionResponseBuilder latencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public PredictionResponseBuilder explanation(Map<String, Object> explanation) {
            this.explanation = explanation;
            return this;
        }

        public PredictionResponseBuilder addExplanation(String key, Object value) {
            if (this.explanation == null) {
                this.explanation = new java.util.HashMap<>();
            }
            this.explanation.put(key, value);
            return this;
        }

        public PredictionResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public PredictionResponseBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public PredictionResponseBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PredictionResponseBuilder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new java.util.HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public PredictionResponse build() {
            // Generate prediction ID if not provided
            if (predictionId == null) {
                predictionId = java.util.UUID.randomUUID().toString();
            }

            return new PredictionResponse(
                predictionId, prediction, confidence, modelName, modelVersion,
                latencyMs, explanation, timestamp, requestId, metadata
            );
        }
    }
}