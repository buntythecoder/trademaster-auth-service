package com.trademaster.mlinfra.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

/**
 * MLflow Configuration Properties
 * 
 * Configuration for MLflow experiment tracking and model registry:
 * - Tracking server configuration
 * - Model registry settings
 * - Artifact storage configuration
 * - Authentication and security
 */
@Validated
@ConfigurationProperties(prefix = "ml.mlflow")
public record MLflowProperties(
    
    @NotBlank String trackingUri,
    @NotBlank String registryUri,
    @NotBlank String artifactUri,
    
    @NotNull ExperimentConfig experiment,
    @NotNull ModelRegistryConfig modelRegistry,
    @NotNull AuthConfig auth,
    @NotNull StorageConfig storage
) {

    /**
     * MLflow Experiment Tracking Configuration
     */
    public record ExperimentConfig(
        @NotBlank String defaultExperimentName,
        @Positive int maxRunsPerExperiment,
        Duration runTimeout,
        boolean autoLogMetrics,
        boolean autoLogParams,
        boolean autoLogArtifacts,
        Map<String, String> defaultTags
    ) {}

    /**
     * MLflow Model Registry Configuration
     */
    public record ModelRegistryConfig(
        @NotBlank String defaultModelName,
        @NotBlank String stagingStage,
        @NotBlank String productionStage,
        @NotBlank String archivedStage,
        boolean requireApproval,
        Duration approvalTimeout,
        @Positive int maxModelVersions
    ) {}

    /**
     * MLflow Authentication Configuration
     */
    public record AuthConfig(
        boolean enabled,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String tokenUri,
        Duration tokenExpiration
    ) {}

    /**
     * MLflow Storage Configuration
     */
    public record StorageConfig(
        @NotBlank String backend,
        @NotBlank String backendUri,
        @NotBlank String artifactRoot,
        @Positive long maxArtifactSizeMb,
        Duration artifactTtl,
        Map<String, String> s3Config
    ) {}

    /**
     * Default MLflow configuration values
     */
    public static MLflowProperties defaults() {
        return new MLflowProperties(
            "http://localhost:5000",
            "http://localhost:5000",
            "s3://ml-platform/mlflow-artifacts",
            new ExperimentConfig(
                "default-experiment",
                1000,
                Duration.ofHours(24),
                true,
                true,
                true,
                Map.of(
                    "environment", "development",
                    "team", "ml-team",
                    "project", "trademaster"
                )
            ),
            new ModelRegistryConfig(
                "default-model",
                "Staging",
                "Production",
                "Archived",
                true,
                Duration.ofDays(7),
                10
            ),
            new AuthConfig(
                false,
                "mlflow",
                "mlflow",
                "http://localhost:5000/api/2.0/mlflow/users/get-token",
                Duration.ofHours(24)
            ),
            new StorageConfig(
                "s3",
                "s3://ml-platform/mlflow",
                "s3://ml-platform/mlflow-artifacts",
                500L,
                Duration.ofDays(30),
                Map.of(
                    "AWS_ACCESS_KEY_ID", "minioadmin",
                    "AWS_SECRET_ACCESS_KEY", "minioadmin",
                    "MLFLOW_S3_ENDPOINT_URL", "http://localhost:9000"
                )
            )
        );
    }
}