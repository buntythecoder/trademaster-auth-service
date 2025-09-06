package com.trademaster.mlinfra.controller;

import com.trademaster.mlinfra.domain.dto.*;
import com.trademaster.mlinfra.functional.Result;
import com.trademaster.mlinfra.service.FeatureStoreService;
import com.trademaster.mlinfra.service.MLflowService;
import com.trademaster.mlinfra.service.ModelServingService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ML Infrastructure Platform REST API Controller
 * 
 * Provides comprehensive REST API for ML platform operations:
 * - Experiment tracking and management
 * - Model registry and deployment
 * - Feature store operations
 * - Model serving and inference
 * - Health checks and monitoring
 * 
 * Built with Java 24 Virtual Threads for optimal performance.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ml")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class MLInfrastructureController {

    private final MLflowService mlflowService;
    private final FeatureStoreService featureStoreService;
    private final ModelServingService modelServingService;

    // ==================== EXPERIMENT MANAGEMENT ====================

    /**
     * Create new ML experiment
     */
    @PostMapping("/experiments")
    @Timed(value = "ml_experiments_create_duration", description = "Time to create ML experiment")
    @Counted(value = "ml_experiments_create_total", description = "Total ML experiments created")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createExperiment(
            @RequestBody @Valid ExperimentCreateRequest request) {
        
        log.info("Creating experiment: {}", request.name());
        
        return mlflowService.createExperiment(request.name(), request.tags())
            .thenApply(result -> result.fold(
                experiment -> {
                    log.info("Successfully created experiment: {}", experiment.experimentName());
                    return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                            "success", true,
                            "message", "Experiment created successfully",
                            "experiment", experiment
                        ));
                },
                error -> {
                    log.error("Failed to create experiment: {}", error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Failed to create experiment"
                        ));
                }
            ));
    }

    /**
     * Get all experiments with pagination
     */
    @GetMapping("/experiments")
    @Timed(value = "ml_experiments_list_duration", description = "Time to list ML experiments")
    public ResponseEntity<Map<String, Object>> getExperiments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        
        var result = mlflowService.getExperiments(page, size);
        
        return result.fold(
            experiments -> ResponseEntity.ok(Map.of(
                "success", true,
                "experiments", experiments,
                "page", page,
                "size", size,
                "total", experiments.size()
            )),
            error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", error,
                    "message", "Failed to retrieve experiments"
                ))
        );
    }

    /**
     * Start new experiment run
     */
    @PostMapping("/experiments/{experimentId}/runs")
    @Timed(value = "ml_runs_start_duration", description = "Time to start ML run")
    @Counted(value = "ml_runs_start_total", description = "Total ML runs started")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> startRun(
            @PathVariable @NotBlank String experimentId,
            @RequestBody @Valid RunStartRequest request) {
        
        log.info("Starting run for experiment: {}", experimentId);
        
        return mlflowService.startRun(experimentId, request.params(), request.tags())
            .thenApply(result -> result.fold(
                run -> {
                    log.info("Successfully started run: {} for experiment: {}", run.runId(), experimentId);
                    return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                            "success", true,
                            "message", "Run started successfully",
                            "run", run
                        ));
                },
                error -> {
                    log.error("Failed to start run for experiment: {}, error: {}", experimentId, error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Failed to start run"
                        ));
                }
            ));
    }

    /**
     * Log metrics to MLflow run
     */
    @PostMapping("/runs/{runId}/metrics")
    @Timed(value = "ml_metrics_log_duration", description = "Time to log ML metrics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> logMetrics(
            @PathVariable @NotBlank String runId,
            @RequestBody @Valid MetricsLogRequest request) {
        
        log.debug("Logging {} metrics for run: {}", request.metrics().size(), runId);
        
        return mlflowService.logMetrics(runId, request.metrics(), request.timestamp())
            .thenApply(result -> result.fold(
                success -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Metrics logged successfully",
                    "metricsCount", request.metrics().size()
                )),
                error -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", error,
                        "message", "Failed to log metrics"
                    ))
            ));
    }

    // ==================== MODEL REGISTRY ====================

    /**
     * Register model in MLflow model registry
     */
    @PostMapping("/models/register")
    @Timed(value = "ml_models_register_duration", description = "Time to register ML model")
    @Counted(value = "ml_models_register_total", description = "Total ML models registered")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> registerModel(
            @RequestBody @Valid ModelRegisterRequest request) {
        
        log.info("Registering model: {} from run: {}", request.name(), request.runId());
        
        return mlflowService.registerModel(
            request.name(), 
            request.runId(), 
            request.artifactPath(), 
            Map.<String, Object>copyOf(request.tags() != null ? request.tags() : Map.of())
        ).thenApply(result -> result.fold(
            model -> {
                log.info("Successfully registered model: {} version: {}", 
                    model.modelName(), model.modelVersion());
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true,
                        "message", "Model registered successfully",
                        "model", model
                    ));
            },
            error -> {
                log.error("Failed to register model: {}, error: {}", request.name(), error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", error,
                        "message", "Failed to register model"
                    ));
            }
        ));
    }

    /**
     * Get production models
     */
    @GetMapping("/models/production")
    @Timed(value = "ml_models_production_list_duration", description = "Time to list production models")
    public ResponseEntity<Map<String, Object>> getProductionModels() {
        var result = mlflowService.getProductionModels();
        
        return result.fold(
            models -> ResponseEntity.ok(Map.of(
                "success", true,
                "models", models,
                "count", models.size()
            )),
            error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", error,
                    "message", "Failed to retrieve production models"
                ))
        );
    }

    /**
     * Promote model to production
     */
    @PostMapping("/models/{modelName}/versions/{version}/promote")
    @Timed(value = "ml_models_promote_duration", description = "Time to promote model to production")
    @Counted(value = "ml_models_promote_total", description = "Total model promotions")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> promoteToProduction(
            @PathVariable @NotBlank String modelName,
            @PathVariable @NotBlank String version,
            @RequestBody(required = false) ModelPromoteRequest request) {
        
        var description = request != null ? request.description() : "Promoted to production";
        
        log.info("Promoting model to production: {} version: {}", modelName, version);
        
        return mlflowService.promoteToProduction(modelName, version, description)
            .thenApply(result -> result.fold(
                model -> {
                    log.info("Successfully promoted model: {} version: {} to production", 
                        modelName, version);
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Model promoted to production successfully",
                        "model", model
                    ));
                },
                error -> {
                    log.error("Failed to promote model: {} version: {}, error: {}", 
                        modelName, version, error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Failed to promote model to production"
                        ));
                }
            ));
    }

    // ==================== MODEL SERVING ====================

    /**
     * Deploy model for serving
     */
    @PostMapping("/models/deploy")
    @Timed(value = "ml_models_deploy_duration", description = "Time to deploy ML model")
    @Counted(value = "ml_models_deploy_total", description = "Total ML model deployments")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deployModel(
            @RequestBody @Valid ModelDeploymentRequest request) {
        
        log.info("Deploying model: {} version: {}", request.modelName(), request.modelVersion());
        
        return modelServingService.deployModel(request)
            .thenApply(result -> result.fold(
                deploymentId -> {
                    log.info("Successfully deployed model: {} version: {} with deployment ID: {}", 
                        request.modelName(), request.modelVersion(), deploymentId);
                    return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                            "success", true,
                            "message", "Model deployed successfully",
                            "deploymentId", deploymentId
                        ));
                },
                error -> {
                    log.error("Failed to deploy model: {} version: {}, error: {}", 
                        request.modelName(), request.modelVersion(), error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Failed to deploy model"
                        ));
                }
            ));
    }

    /**
     * Model inference endpoint
     */
    @PostMapping("/predict/{modelName}")
    @Timed(value = "ml_predict_duration", description = "Model inference time")
    @Counted(value = "ml_predict_total", description = "Total predictions made")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> predict(
            @PathVariable @NotBlank String modelName,
            @RequestBody @Valid PredictionRequest request) {
        
        var enrichedRequest = new PredictionRequest(
            modelName,
            request.modelVersion(),
            request.features(),
            request.getRequestIdOrGenerate(),
            request.userId(),
            request.sessionId(),
            request.explainPrediction(),
            request.requestMetadata()
        );
        
        log.debug("Processing prediction request: {} for model: {}", 
            enrichedRequest.requestId(), modelName);
        
        return modelServingService.predict(enrichedRequest)
            .thenApply(result -> result.fold(
                response -> {
                    log.debug("Successfully processed prediction: {} in {}ms", 
                        response.predictionId(), response.latencyMs());
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "prediction", response
                    ));
                },
                error -> {
                    log.error("Prediction failed for request: {}, error: {}", 
                        enrichedRequest.requestId(), error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Prediction failed"
                        ));
                }
            ));
    }

    /**
     * Get model deployment status
     */
    @GetMapping("/deployments/{deploymentId}")
    @Timed(value = "ml_deployments_status_duration", description = "Time to get deployment status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus(
            @PathVariable @NotBlank String deploymentId) {
        
        var result = modelServingService.getDeploymentStatus(deploymentId);
        
        return result.fold(
            status -> ResponseEntity.ok(Map.of(
                "success", true,
                "deployment", status
            )),
            error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "success", false,
                    "error", error,
                    "message", "Deployment not found"
                ))
        );
    }

    /**
     * List active deployments
     */
    @GetMapping("/deployments")
    @Timed(value = "ml_deployments_list_duration", description = "Time to list deployments")
    public ResponseEntity<Map<String, Object>> listActiveDeployments() {
        var result = modelServingService.listActiveDeployments();
        
        return result.fold(
            deployments -> ResponseEntity.ok(Map.of(
                "success", true,
                "deployments", deployments,
                "count", deployments.size()
            )),
            error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", error,
                    "message", "Failed to list deployments"
                ))
        );
    }

    // ==================== FEATURE STORE ====================

    /**
     * Register feature definition
     */
    @PostMapping("/features")
    @Timed(value = "ml_features_register_duration", description = "Time to register feature")
    @Counted(value = "ml_features_register_total", description = "Total features registered")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> registerFeature(
            @RequestBody @Valid FeatureRegisterRequest request) {
        
        log.info("Registering feature: {} version: {}", 
            request.featureName(), request.featureVersion());
        
        return featureStoreService.registerFeature(
            request.featureName(),
            request.featureVersion(), 
            request.definition(),
            request.dataType(),
            request.computationType(),
            request.validationRules()
        ).thenApply(result -> result.fold(
            feature -> {
                log.info("Successfully registered feature: {} version: {}", 
                    feature.name(), feature.version());
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true,
                        "message", "Feature registered successfully",
                        "feature", feature
                    ));
            },
            error -> {
                log.error("Failed to register feature: {} version: {}, error: {}", 
                    request.featureName(), request.featureVersion(), error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", error,
                        "message", "Failed to register feature"
                    ));
            }
        ));
    }

    /**
     * Compute features for inference
     */
    @PostMapping("/features/compute")
    @Timed(value = "ml_features_compute_duration", description = "Feature computation time")
    @Counted(value = "ml_features_compute_total", description = "Total feature computations")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> computeFeatures(
            @RequestBody @Valid FeatureComputeRequest request) {
        
        log.debug("Computing {} features for entity: {}", 
            request.featureIds().size(), request.entityIds());
        
        return featureStoreService.computeFeatures(request)
            .thenApply(result -> result.fold(
                response -> {
                    log.debug("Successfully computed {} features for entity: {} in {}ms", 
                        response.features().size(), response.entityIds(), response.computationTimeMs());
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "result", response
                    ));
                },
                error -> {
                    log.error("Failed to compute features for entity: {}, error: {}", 
                        request.entityIds(), error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "error", error,
                            "message", "Failed to compute features"
                        ));
                }
            ));
    }

    /**
     * Get cached features
     */
    @GetMapping("/features/{entityId}")
    @Timed(value = "ml_features_get_duration", description = "Time to get cached features")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getCachedFeatures(
            @PathVariable @NotBlank String entityId,
            @RequestParam List<String> features) {
        
        log.debug("Getting {} cached features for entity: {}", features.size(), entityId);
        
        return featureStoreService.getCachedFeatures(entityId, features)
            .thenApply(result -> result.fold(
                cachedFeatures -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "entityId", entityId,
                    "features", cachedFeatures,
                    "count", cachedFeatures.size()
                )),
                error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", error,
                        "message", "Failed to get cached features"
                    ))
            ));
    }

    /**
     * List active features
     */
    @GetMapping("/features")
    @Timed(value = "ml_features_list_duration", description = "Time to list features")
    public ResponseEntity<Map<String, Object>> listActiveFeatures() {
        var result = featureStoreService.listActiveFeatures();
        
        return result.fold(
            features -> ResponseEntity.ok(Map.of(
                "success", true,
                "features", features,
                "count", features.size()
            )),
            error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", error,
                    "message", "Failed to list features"
                ))
        );
    }

    // ==================== HEALTH CHECKS ====================

    /**
     * MLflow health check
     */
    @GetMapping("/health/mlflow")
    public ResponseEntity<Map<String, Object>> mlflowHealthCheck() {
        var result = mlflowService.healthCheck();
        
        return result.fold(
            health -> ResponseEntity.ok(health),
            error -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "status", "DOWN",
                    "error", error
                ))
        );
    }

    /**
     * Feature store health check
     */
    @GetMapping("/health/features")
    public ResponseEntity<Map<String, Object>> featureStoreHealthCheck() {
        var result = featureStoreService.healthCheck();
        
        return result.fold(
            health -> ResponseEntity.ok(health),
            error -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "status", "DOWN",
                    "error", error
                ))
        );
    }

    /**
     * Model serving health check
     */
    @GetMapping("/health/serving")
    public ResponseEntity<Map<String, Object>> modelServingHealthCheck() {
        var result = modelServingService.healthCheck();
        
        return result.fold(
            health -> ResponseEntity.ok(health),
            error -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "status", "DOWN",
                    "error", error
                ))
        );
    }

    /**
     * Overall platform health check
     */
    @GetMapping("/health")
    @Timed(value = "ml_health_check_duration", description = "Platform health check time")
    public ResponseEntity<Map<String, Object>> platformHealthCheck() {
        var mlflowHealth = mlflowService.healthCheck();
        var featureStoreHealth = featureStoreService.healthCheck();
        var modelServingHealth = modelServingService.healthCheck();
        
        var overallStatus = mlflowHealth.isSuccess() && 
                          featureStoreHealth.isSuccess() && 
                          modelServingHealth.isSuccess() ? "UP" : "DOWN";
        
        var health = Map.of(
            "status", overallStatus,
            "timestamp", java.time.LocalDateTime.now().toString(),
            "components", Map.of(
                "mlflow", mlflowHealth.fold(
                    result -> Map.of("status", result.get("status")),
                    error -> Map.of("status", "DOWN", "error", error)
                ),
                "featureStore", featureStoreHealth.fold(
                    result -> Map.of("status", result.get("status")),
                    error -> Map.of("status", "DOWN", "error", error)
                ),
                "modelServing", modelServingHealth.fold(
                    result -> Map.of("status", result.get("status")),
                    error -> Map.of("status", "DOWN", "error", error)
                )
            )
        );
        
        return "UP".equals(overallStatus) ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }
}