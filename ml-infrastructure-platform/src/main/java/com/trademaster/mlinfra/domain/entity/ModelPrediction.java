package com.trademaster.mlinfra.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model Prediction Entity
 * 
 * Stores prediction requests and responses for monitoring and analysis:
 * - Input features and prediction results
 * - Performance metrics and latency tracking
 * - User context and request metadata
 * - Model performance monitoring data
 */
@Entity
@Table(name = "model_predictions",
    indexes = {
        @Index(name = "idx_model_predictions_timestamp", 
               columnList = "model_name, model_version, prediction_timestamp"),
        @Index(name = "idx_model_predictions_user", 
               columnList = "user_id, prediction_timestamp")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "prediction_id", unique = true, nullable = false, length = 255)
    private String predictionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_features", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> inputFeatures;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prediction_result", nullable = false, columnDefinition = "jsonb")
    private Object predictionResult;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private Double confidenceScore;

    @Column(name = "inference_latency_ms")
    private Integer inferenceLatencyMs;

    @Column(name = "prediction_timestamp")
    @Builder.Default
    private LocalDateTime predictionTimestamp = LocalDateTime.now();

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "request_id", length = 255)
    private String requestId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_metadata", columnDefinition = "jsonb")
    private Map<String, Object> requestMetadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_metadata", columnDefinition = "jsonb")
    private Map<String, Object> responseMetadata;

    @Column(name = "feedback_score")
    private Double feedbackScore;

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    @Column(name = "feedback_timestamp")
    private LocalDateTime feedbackTimestamp;

    @Column(name = "is_training_data")
    @Builder.Default
    private Boolean isTrainingData = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (predictionTimestamp == null) {
            predictionTimestamp = LocalDateTime.now();
        }
    }

    /**
     * Get model identifier
     */
    @JsonIgnore
    public String getModelIdentifier() {
        return modelName + ":" + modelVersion;
    }

    /**
     * Check if prediction is high confidence
     */
    @JsonIgnore
    public boolean isHighConfidence(double threshold) {
        return confidenceScore != null && confidenceScore >= threshold;
    }

    /**
     * Check if prediction is low latency
     */
    @JsonIgnore
    public boolean isLowLatency(int maxLatencyMs) {
        return inferenceLatencyMs != null && inferenceLatencyMs <= maxLatencyMs;
    }

    /**
     * Check if user provided feedback
     */
    @JsonIgnore
    public boolean hasFeedback() {
        return feedbackScore != null || 
               (feedbackComment != null && !feedbackComment.trim().isEmpty());
    }

    /**
     * Check if feedback is positive
     */
    @JsonIgnore
    public boolean hasPositiveFeedback() {
        return feedbackScore != null && feedbackScore > 0.5;
    }

    /**
     * Get age of prediction in hours
     */
    @JsonIgnore
    public long getAgeInHours() {
        return java.time.Duration.between(predictionTimestamp, LocalDateTime.now()).toHours();
    }

    /**
     * Check if prediction is recent
     */
    @JsonIgnore
    public boolean isRecent(int maxAgeHours) {
        return getAgeInHours() <= maxAgeHours;
    }

    /**
     * Add user feedback
     */
    public ModelPrediction withFeedback(Double score, String comment) {
        this.feedbackScore = score;
        this.feedbackComment = comment;
        this.feedbackTimestamp = LocalDateTime.now();
        return this;
    }

    /**
     * Add request metadata
     */
    public ModelPrediction withRequestMetadata(String key, Object value) {
        if (this.requestMetadata == null) {
            this.requestMetadata = new java.util.HashMap<>();
        }
        this.requestMetadata.put(key, value);
        return this;
    }

    /**
     * Add response metadata
     */
    public ModelPrediction withResponseMetadata(String key, Object value) {
        if (this.responseMetadata == null) {
            this.responseMetadata = new java.util.HashMap<>();
        }
        this.responseMetadata.put(key, value);
        return this;
    }

    /**
     * Mark as training data
     */
    public ModelPrediction markAsTrainingData() {
        this.isTrainingData = true;
        return this;
    }

    /**
     * Get latency performance category
     */
    @JsonIgnore
    public String getLatencyCategory() {
        if (inferenceLatencyMs == null) {
            return "UNKNOWN";
        } else if (inferenceLatencyMs <= 50) {
            return "EXCELLENT";
        } else if (inferenceLatencyMs <= 100) {
            return "GOOD";
        } else if (inferenceLatencyMs <= 200) {
            return "ACCEPTABLE";
        } else {
            return "POOR";
        }
    }

    /**
     * Get confidence performance category
     */
    @JsonIgnore
    public String getConfidenceCategory() {
        if (confidenceScore == null) {
            return "UNKNOWN";
        } else if (confidenceScore >= 0.9) {
            return "VERY_HIGH";
        } else if (confidenceScore >= 0.7) {
            return "HIGH";
        } else if (confidenceScore >= 0.5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Factory method for creating prediction with defaults
     */
    public static ModelPrediction newPrediction(
            String modelName,
            String modelVersion,
            String userId,
            Map<String, Object> inputFeatures,
            Object predictionResult) {
        var entity = new ModelPrediction();
        entity.predictionId = java.util.UUID.randomUUID().toString();
        entity.modelName = modelName;
        entity.modelVersion = modelVersion;
        entity.userId = userId;
        entity.inputFeatures = inputFeatures;
        entity.predictionResult = predictionResult;
        entity.predictionTimestamp = LocalDateTime.now();
        entity.isTrainingData = false;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}