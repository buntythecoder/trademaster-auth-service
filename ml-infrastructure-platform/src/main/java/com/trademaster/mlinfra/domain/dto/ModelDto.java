package com.trademaster.mlinfra.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trademaster.mlinfra.domain.entity.ModelRegistry;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model Registry Data Transfer Object
 * 
 * Represents model registry data for API responses:
 * - Model metadata and versioning
 * - Stage transitions and deployment info
 * - Performance metrics and validation
 * - MLflow integration details
 */
public record ModelDto(
    Long id,
    String modelName,
    String modelVersion,
    String modelStage,
    String modelUri,
    String modelType,
    String runId,
    Long trainingExperimentId,
    Map<String, Double> performanceMetrics,
    Map<String, Object> validationResults,
    Map<String, Object> metadata,
    Map<String, Object> deploymentConfig,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime deployedAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime retiredAt,
    
    String modelIdentifier,
    boolean isProduction,
    boolean isStaging,
    boolean isArchived,
    Long daysSinceDeployment
) {

    /**
     * Create DTO from entity
     */
    public static ModelDto fromEntity(ModelRegistry model) {
        return new ModelDto(
            model.getId(),
            model.getModelName(),
            model.getModelVersion(),
            model.getModelStage(),
            model.getModelUri(),
            model.getModelType(),
            model.getRunId(),
            model.getTrainingExperimentId(),
            model.getPerformanceMetrics(),
            model.getValidationResults(),
            model.getMetadata(),
            model.getDeploymentConfig(),
            model.getCreatedAt(),
            model.getDeployedAt(),
            model.getRetiredAt(),
            model.getModelIdentifier(),
            model.isProduction(),
            model.isStaging(),
            model.isArchived(),
            model.getDaysSinceDeployment()
        );
    }

    /**
     * Create builder for new model
     */
    public static ModelDtoBuilder builder() {
        return new ModelDtoBuilder();
    }

    public static class ModelDtoBuilder {
        private Long id;
        private String modelName;
        private String modelVersion;
        private String modelStage = "None";
        private String modelUri;
        private String modelType;
        private String runId;
        private Long trainingExperimentId;
        private Map<String, Double> performanceMetrics;
        private Map<String, Object> validationResults;
        private Map<String, Object> metadata;
        private Map<String, Object> deploymentConfig;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime deployedAt;
        private LocalDateTime retiredAt;

        public ModelDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ModelDtoBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public ModelDtoBuilder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public ModelDtoBuilder modelStage(String modelStage) {
            this.modelStage = modelStage;
            return this;
        }

        public ModelDtoBuilder modelUri(String modelUri) {
            this.modelUri = modelUri;
            return this;
        }

        public ModelDtoBuilder modelType(String modelType) {
            this.modelType = modelType;
            return this;
        }

        public ModelDtoBuilder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public ModelDtoBuilder trainingExperimentId(Long trainingExperimentId) {
            this.trainingExperimentId = trainingExperimentId;
            return this;
        }

        public ModelDtoBuilder performanceMetrics(Map<String, Double> performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }

        public ModelDtoBuilder validationResults(Map<String, Object> validationResults) {
            this.validationResults = validationResults;
            return this;
        }

        public ModelDtoBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ModelDtoBuilder deploymentConfig(Map<String, Object> deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
            return this;
        }

        public ModelDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ModelDtoBuilder deployedAt(LocalDateTime deployedAt) {
            this.deployedAt = deployedAt;
            return this;
        }

        public ModelDtoBuilder retiredAt(LocalDateTime retiredAt) {
            this.retiredAt = retiredAt;
            return this;
        }

        public ModelDto build() {
            var modelIdentifier = modelName + ":" + modelVersion;
            var isProduction = "Production".equalsIgnoreCase(modelStage);
            var isStaging = "Staging".equalsIgnoreCase(modelStage);
            var isArchived = "Archived".equalsIgnoreCase(modelStage) || retiredAt != null;
            
            Long daysSinceDeployment = null;
            if (deployedAt != null) {
                daysSinceDeployment = java.time.Duration.between(deployedAt, LocalDateTime.now()).toDays();
            }

            return new ModelDto(
                id, modelName, modelVersion, modelStage, modelUri, modelType, runId,
                trainingExperimentId, performanceMetrics, validationResults, metadata,
                deploymentConfig, createdAt, deployedAt, retiredAt, modelIdentifier,
                isProduction, isStaging, isArchived, daysSinceDeployment
            );
        }
    }
}