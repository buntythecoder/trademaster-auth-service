package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.mlinfra.domain.entity.MLExperiment;
import com.trademaster.mlinfra.domain.types.ExperimentStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ML Experiment Data Transfer Object
 * 
 * Represents experiment data for API responses:
 * - Experiment metadata and configuration
 * - Training parameters and results
 * - MLflow integration details
 * - Status and timing information
 */
public record ExperimentDto(
    Long id,
    String experimentName,
    String modelType,
    String trainingDatasetId,
    String featureSetVersion,
    Map<String, Object> hyperparameters,
    Map<String, Double> metrics,
    Map<String, String> artifacts,
    ExperimentStatus experimentStatus,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime startedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime completedAt,
    
    String createdBy,
    String mlflowRunId,
    String mlflowExperimentId,
    Map<String, String> tags,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    Long durationSeconds,
    boolean isRunning,
    boolean isCompleted,
    boolean isFailed
) {

    /**
     * Create DTO from entity
     */
    public static ExperimentDto fromEntity(MLExperiment experiment) {
        return new ExperimentDto(
            experiment.getId(),
            experiment.getExperimentName(),
            experiment.getModelType(),
            experiment.getTrainingDatasetId(),
            experiment.getFeatureSetVersion(),
            experiment.getHyperparameters(),
            experiment.getMetrics(),
            experiment.getArtifacts(),
            experiment.getExperimentStatus(),
            experiment.getStartedAt(),
            experiment.getCompletedAt(),
            experiment.getCreatedBy(),
            experiment.getMlflowRunId(),
            experiment.getMlflowExperimentId(),
            experiment.getTags(),
            experiment.getCreatedAt(),
            experiment.getDurationSeconds(),
            experiment.isRunning(),
            experiment.isCompleted(),
            experiment.isFailed()
        );
    }

    /**
     * Create builder for new experiment
     */
    public static ExperimentDtoBuilder builder() {
        return new ExperimentDtoBuilder();
    }

    public static class ExperimentDtoBuilder {
        private Long id;
        private String experimentName;
        private String modelType;
        private String trainingDatasetId;
        private String featureSetVersion;
        private Map<String, Object> hyperparameters;
        private Map<String, Double> metrics;
        private Map<String, String> artifacts;
        private ExperimentStatus experimentStatus = ExperimentStatus.RUNNING;
        private LocalDateTime startedAt = LocalDateTime.now();
        private LocalDateTime completedAt;
        private String createdBy;
        private String mlflowRunId;
        private String mlflowExperimentId;
        private Map<String, String> tags;
        private LocalDateTime createdAt = LocalDateTime.now();
        private Long durationSeconds;
        private boolean isRunning = true;
        private boolean isCompleted = false;
        private boolean isFailed = false;

        public ExperimentDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ExperimentDtoBuilder experimentName(String experimentName) {
            this.experimentName = experimentName;
            return this;
        }

        public ExperimentDtoBuilder modelType(String modelType) {
            this.modelType = modelType;
            return this;
        }

        public ExperimentDtoBuilder trainingDatasetId(String trainingDatasetId) {
            this.trainingDatasetId = trainingDatasetId;
            return this;
        }

        public ExperimentDtoBuilder featureSetVersion(String featureSetVersion) {
            this.featureSetVersion = featureSetVersion;
            return this;
        }

        public ExperimentDtoBuilder hyperparameters(Map<String, Object> hyperparameters) {
            this.hyperparameters = hyperparameters;
            return this;
        }

        public ExperimentDtoBuilder metrics(Map<String, Double> metrics) {
            this.metrics = metrics;
            return this;
        }

        public ExperimentDtoBuilder artifacts(Map<String, String> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public ExperimentDtoBuilder experimentStatus(ExperimentStatus experimentStatus) {
            this.experimentStatus = experimentStatus;
            this.isRunning = experimentStatus == ExperimentStatus.RUNNING;
            this.isCompleted = experimentStatus == ExperimentStatus.COMPLETED;
            this.isFailed = experimentStatus == ExperimentStatus.FAILED;
            return this;
        }

        public ExperimentDtoBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ExperimentDtoBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ExperimentDtoBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public ExperimentDtoBuilder mlflowRunId(String mlflowRunId) {
            this.mlflowRunId = mlflowRunId;
            return this;
        }

        public ExperimentDtoBuilder mlflowExperimentId(String mlflowExperimentId) {
            this.mlflowExperimentId = mlflowExperimentId;
            return this;
        }

        public ExperimentDtoBuilder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public ExperimentDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ExperimentDto build() {
            // Calculate duration if both start and end times are available
            if (startedAt != null && completedAt != null) {
                this.durationSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
            } else if (startedAt != null) {
                this.durationSeconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
            }

            return new ExperimentDto(
                id, experimentName, modelType, trainingDatasetId, featureSetVersion,
                hyperparameters, metrics, artifacts, experimentStatus,
                startedAt, completedAt, createdBy, mlflowRunId, mlflowExperimentId,
                tags, createdAt, durationSeconds, isRunning, isCompleted, isFailed
            );
        }
    }
}