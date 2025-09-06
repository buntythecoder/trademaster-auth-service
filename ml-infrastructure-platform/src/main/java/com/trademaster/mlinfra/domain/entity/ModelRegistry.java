package com.trademaster.mlinfra.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model Registry Entity
 * 
 * Represents registered ML models in the model registry:
 * - Model versioning and lifecycle management
 * - Stage transitions (staging, production, archived)
 * - Performance metrics and validation results
 * - Deployment configuration and metadata
 */
@Entity
@Table(name = "model_registry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"model_name", "model_version"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@With
public class ModelRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "model_stage", length = 30)
    @Builder.Default
    private String modelStage = "None";

    @Column(name = "model_uri", nullable = false, columnDefinition = "TEXT")
    private String modelUri;

    @Column(name = "model_type", nullable = false, length = 100)
    private String modelType;

    @Column(name = "run_id", length = 255)
    private String runId;

    @Column(name = "training_experiment_id")
    private Long trainingExperimentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "performance_metrics", columnDefinition = "jsonb")
    private Map<String, Double> performanceMetrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_results", columnDefinition = "jsonb")
    private Map<String, Object> validationResults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deployment_config", columnDefinition = "jsonb")
    private Map<String, Object> deploymentConfig;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_experiment_id", insertable = false, updatable = false)
    @JsonIgnore
    private MLExperiment trainingExperiment;

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
     * Check if model is in production stage
     */
    @JsonIgnore
    public boolean isProduction() {
        return "Production".equalsIgnoreCase(modelStage);
    }

    /**
     * Check if model is in staging
     */
    @JsonIgnore
    public boolean isStaging() {
        return "Staging".equalsIgnoreCase(modelStage);
    }

    /**
     * Check if model is archived
     */
    @JsonIgnore
    public boolean isArchived() {
        return "Archived".equalsIgnoreCase(modelStage) || retiredAt != null;
    }

    /**
     * Get model identifier
     */
    @JsonIgnore
    public String getModelIdentifier() {
        return modelName + ":" + modelVersion;
    }

    /**
     * Add or update performance metric
     */
    public ModelRegistry withPerformanceMetric(String name, Double value) {
        if (this.performanceMetrics == null) {
            this.performanceMetrics = new java.util.HashMap<>();
        }
        this.performanceMetrics.put(name, value);
        return this;
    }

    /**
     * Add or update metadata
     */
    public ModelRegistry withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add validation result
     */
    public ModelRegistry withValidationResult(String key, Object value) {
        if (this.validationResults == null) {
            this.validationResults = new java.util.HashMap<>();
        }
        this.validationResults.put(key, value);
        return this;
    }

    /**
     * Transition to staging
     */
    public ModelRegistry promoteToStaging() {
        this.modelStage = "Staging";
        return this;
    }

    /**
     * Transition to production
     */
    public ModelRegistry promoteToProduction() {
        this.modelStage = "Production";
        this.deployedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Archive model
     */
    public ModelRegistry archive() {
        this.modelStage = "Archived";
        this.retiredAt = LocalDateTime.now();
        return this;
    }

    /**
     * Get days since deployment
     */
    @JsonIgnore
    public Long getDaysSinceDeployment() {
        if (deployedAt == null) {
            return null;
        }
        return java.time.Duration.between(deployedAt, LocalDateTime.now()).toDays();
    }

    /**
     * Check if model needs refresh based on age
     */
    @JsonIgnore
    public boolean needsRefresh(int maxDaysInProduction) {
        var daysSinceDeployment = getDaysSinceDeployment();
        return daysSinceDeployment != null && daysSinceDeployment > maxDaysInProduction;
    }

    /**
     * Create builder with defaults
     */
}