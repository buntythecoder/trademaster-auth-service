package com.trademaster.mlinfra.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trademaster.mlinfra.domain.types.ExperimentStatus;
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
 * ML Experiment Entity
 * 
 * Represents a machine learning experiment with MLflow integration:
 * - Experiment metadata and configuration
 * - Training parameters and hyperparameters
 * - Results metrics and artifacts
 * - Integration with MLflow tracking
 */
@Entity
@Table(name = "ml_experiments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLExperiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "experiment_name", nullable = false, length = 255)
    private String experimentName;

    @Column(name = "model_type", nullable = false, length = 100)
    private String modelType;

    @Column(name = "training_dataset_id", length = 255)
    private String trainingDatasetId;

    @Column(name = "feature_set_version", length = 50)
    private String featureSetVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hyperparameters", columnDefinition = "jsonb")
    private Map<String, Object> hyperparameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Double> metrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "artifacts", columnDefinition = "jsonb")
    private Map<String, String> artifacts;

    @Enumerated(EnumType.STRING)
    @Column(name = "experiment_status", length = 30)
    @Builder.Default
    private ExperimentStatus experimentStatus = ExperimentStatus.RUNNING;

    @Column(name = "started_at")
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "mlflow_run_id", unique = true, length = 255)
    private String mlflowRunId;

    @Column(name = "mlflow_experiment_id", length = 255)
    private String mlflowExperimentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Mark experiment as completed
     */
    public MLExperiment complete() {
        this.experimentStatus = ExperimentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Mark experiment as failed
     */
    public MLExperiment fail() {
        this.experimentStatus = ExperimentStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Add or update metric
     */
    public MLExperiment withMetric(String name, Double value) {
        if (this.metrics == null) {
            this.metrics = new java.util.HashMap<>();
        }
        this.metrics.put(name, value);
        return this;
    }

    /**
     * Add or update hyperparameter
     */
    public MLExperiment withHyperparameter(String name, Object value) {
        if (this.hyperparameters == null) {
            this.hyperparameters = new java.util.HashMap<>();
        }
        this.hyperparameters.put(name, value);
        return this;
    }

    /**
     * Add artifact reference
     */
    public MLExperiment withArtifact(String name, String uri) {
        if (this.artifacts == null) {
            this.artifacts = new java.util.HashMap<>();
        }
        this.artifacts.put(name, uri);
        return this;
    }

    /**
     * Add or update tag
     */
    public MLExperiment withTag(String key, String value) {
        if (this.tags == null) {
            this.tags = new java.util.HashMap<>();
        }
        this.tags.put(key, value);
        return this;
    }

    /**
     * Check if experiment is running
     */
    @JsonIgnore
    public boolean isRunning() {
        return ExperimentStatus.RUNNING.equals(this.experimentStatus);
    }

    /**
     * Check if experiment is completed
     */
    @JsonIgnore
    public boolean isCompleted() {
        return ExperimentStatus.COMPLETED.equals(this.experimentStatus);
    }

    /**
     * Check if experiment failed
     */
    @JsonIgnore
    public boolean isFailed() {
        return ExperimentStatus.FAILED.equals(this.experimentStatus);
    }

    /**
     * Get experiment duration in seconds
     */
    @JsonIgnore
    public Long getDurationSeconds() {
        if (startedAt == null) {
            return null;
        }
        
        var endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).getSeconds();
    }

    /**
     * Create builder with default values
     */
}