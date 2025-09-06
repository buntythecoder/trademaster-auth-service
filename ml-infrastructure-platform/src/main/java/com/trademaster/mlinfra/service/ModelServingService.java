package com.trademaster.mlinfra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.mlinfra.config.MLInfrastructureProperties;
import com.trademaster.mlinfra.domain.dto.ModelDeploymentRequest;
import com.trademaster.mlinfra.domain.dto.PredictionRequest;
import com.trademaster.mlinfra.domain.dto.PredictionResponse;
import com.trademaster.mlinfra.domain.entity.ModelDeployment;
import com.trademaster.mlinfra.domain.entity.ModelPrediction;
import com.trademaster.mlinfra.functional.Result;
import com.trademaster.mlinfra.repository.ModelDeploymentRepository;
import com.trademaster.mlinfra.repository.ModelPredictionRepository;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Model Serving Service
 * 
 * High-performance model serving infrastructure:
 * - <50ms inference latency
 * - Auto-scaling Kubernetes deployments
 * - Load balancing and traffic splitting
 * - A/B testing and canary deployments
 * - Real-time performance monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ModelServingService {

    private final MLInfrastructureProperties properties;
    private final ModelDeploymentRepository deploymentRepository;
    private final ModelPredictionRepository predictionRepository;
    private final KubernetesClient kubernetesClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // In-memory cache for model endpoints
    private final Map<String, String> modelEndpoints = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    /**
     * Deploy model to Kubernetes cluster with auto-scaling
     */
    @Async("inferenceExecutor")
    public CompletableFuture<Result<String, String>> deployModel(
            ModelDeploymentRequest request) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                var deploymentId = UUID.randomUUID().toString();
                log.info("Deploying model: {} version: {} with deployment ID: {}", 
                    request.modelName(), request.modelVersion(), deploymentId);
                
                // Create Kubernetes deployment
                var deployment = createKubernetesDeployment(request, deploymentId);
                var createdDeployment = kubernetesClient.apps().deployments()
                    .inNamespace("ml-platform")
                    .resource(deployment)
                    .create();
                
                // Create Kubernetes service
                var service = createKubernetesService(request, deploymentId);
                var createdService = kubernetesClient.services()
                    .inNamespace("ml-platform")
                    .resource(service)
                    .create();
                
                // Create HPA (Horizontal Pod Autoscaler)
                var hpa = createHorizontalPodAutoscaler(request, deploymentId);
                kubernetesClient.autoscaling().v1().horizontalPodAutoscalers()
                    .inNamespace("ml-platform")
                    .resource(hpa)
                    .create();
                
                // Wait for deployment to be ready
                var isReady = waitForDeploymentReady(deploymentId, Duration.ofMinutes(10));
                if (!isReady) {
                    return Result.failure("Deployment failed to become ready within timeout");
                }
                
                // Save deployment record
                var modelDeployment = ModelDeployment.builder()
                    .deploymentId(deploymentId)
                    .modelName(request.modelName())
                    .modelVersion(request.modelVersion())
                    .replicas(request.replicas())
                    .status("DEPLOYED")
                    .endpoint(buildServiceEndpoint(deploymentId))
                    .deployedAt(LocalDateTime.now())
                    .build();
                
                deploymentRepository.save(modelDeployment);
                
                // Cache endpoint for fast lookup
                var modelKey = request.modelName() + ":" + request.modelVersion();
                modelEndpoints.put(modelKey, modelDeployment.getEndpoint());
                
                log.info("Successfully deployed model: {} version: {} at endpoint: {}", 
                    request.modelName(), request.modelVersion(), modelDeployment.getEndpoint());
                
                return Result.success(deploymentId);
                
            } catch (Exception e) {
                log.error("Failed to deploy model: {} version: {}", 
                    request.modelName(), request.modelVersion(), e);
                return Result.failure("Model deployment failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * High-performance model inference with <50ms latency
     */
    @Async("inferenceExecutor")
    public CompletableFuture<Result<PredictionResponse, String>> predict(
            PredictionRequest request) {
        
        return CompletableFuture.supplyAsync(() -> {
            var startTime = System.currentTimeMillis();
            var requestId = UUID.randomUUID().toString();
            
            try {
                log.debug("Processing prediction request: {} for model: {}", 
                    requestId, request.modelName());
                
                // Get model endpoint
                var endpoint = getModelEndpoint(request.modelName(), request.modelVersion());
                if (endpoint.isFailure()) {
                    return Result.failure("Model endpoint not found: " + endpoint.getError());
                }
                
                // Prepare HTTP request
                var jsonBody = objectMapper.writeValueAsString(Map.of(
                    "instances", List.of(request.features()),
                    "signature_name", "serving_default"
                ));
                
                var httpRequest = new Request.Builder()
                    .url(endpoint.getValue() + "/v1/models/" + request.modelName() + ":predict")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .header("Content-Type", "application/json")
                    .header("X-Request-ID", requestId)
                    .build();
                
                // Execute inference with timeout
                var call = httpClient.newCall(httpRequest);
                var response = call.execute();
                
                if (!response.isSuccessful()) {
                    var error = "Inference failed with status: " + response.code();
                    log.error("Prediction failed for request: {} - {}", requestId, error);
                    return Result.failure(error);
                }
                
                // Parse response
                var responseBody = response.body().string();
                var predictionResult = objectMapper.readTree(responseBody);
                
                var latency = System.currentTimeMillis() - startTime;
                
                // Extract prediction and confidence
                var predictions = predictionResult.get("predictions");
                var prediction = predictions.isArray() && predictions.size() > 0 ? 
                    predictions.get(0) : predictions;
                
                var confidence = extractConfidence(prediction);
                
                var predictionResponse = new PredictionResponse(
                    requestId,
                    prediction,
                    confidence,
                    request.modelName(),
                    request.modelVersion() != null ? request.modelVersion() : "latest",
                    (int) latency,
                    null // explanation not implemented in this version
                );
                
                // Log prediction for monitoring (async)
                logPrediction(request, predictionResponse, latency);
                
                log.debug("Completed prediction request: {} in {}ms", requestId, latency);
                
                // Check latency target
                var latencyTarget = properties.performance().inference().latencyTarget().toMillis();
                if (latency > latencyTarget) {
                    log.warn("Prediction latency {}ms exceeds target {}ms for model: {}", 
                        latency, latencyTarget, request.modelName());
                }
                
                return Result.success(predictionResponse);
                
            } catch (Exception e) {
                var latency = System.currentTimeMillis() - startTime;
                log.error("Prediction failed for request: {} after {}ms", requestId, latency, e);
                return Result.failure("Prediction failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Scale model deployment based on load
     */
    @Async("inferenceExecutor")
    public CompletableFuture<Result<Void, String>> scaleModel(
            String modelName, 
            String modelVersion, 
            int targetReplicas) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Scaling model: {} version: {} to {} replicas", 
                    modelName, modelVersion, targetReplicas);
                
                var deployment = deploymentRepository
                    .findByModelNameAndModelVersionAndStatus(modelName, modelVersion, "DEPLOYED")
                    .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));
                
                // Update Kubernetes deployment
                kubernetesClient.apps().deployments()
                    .inNamespace("ml-platform")
                    .withName("model-" + deployment.getDeploymentId())
                    .scale(targetReplicas);
                
                // Update database record
                var updatedDeployment = deployment.withReplicas(targetReplicas);
                deploymentRepository.save(updatedDeployment);
                
                log.info("Successfully scaled model: {} version: {} to {} replicas", 
                    modelName, modelVersion, targetReplicas);
                
                return Result.success(null);
                
            } catch (Exception e) {
                log.error("Failed to scale model: {} version: {}", modelName, modelVersion, e);
                return Result.failure("Scaling failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get deployment status and metrics
     */
    @Transactional(readOnly = true)
    public Result<Map<String, Object>, String> getDeploymentStatus(String deploymentId) {
        try {
            var deployment = deploymentRepository.findByDeploymentId(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));
            
            // Get Kubernetes deployment status
            var k8sDeployment = kubernetesClient.apps().deployments()
                .inNamespace("ml-platform")
                .withName("model-" + deploymentId)
                .get();
            
            Map<String, Object> status = Map.of(
                "deploymentId", deploymentId,
                "modelName", deployment.getModelName(),
                "modelVersion", deployment.getModelVersion(),
                "status", deployment.getStatus(),
                "replicas", (Object) deployment.getReplicas(), // Cast to Object
                "readyReplicas", (Object) (k8sDeployment != null ? 
                    k8sDeployment.getStatus().getReadyReplicas() : 0),
                "endpoint", deployment.getEndpoint(),
                "deployedAt", deployment.getDeployedAt().toString()
            );
            
            return Result.<Map<String, Object>, String>success(status);
            
        } catch (Exception e) {
            log.error("Failed to get deployment status: {}", deploymentId, e);
            return Result.failure("Failed to get deployment status: " + e.getMessage());
        }
    }
    
    /**
     * List all active deployments
     */
    @Transactional(readOnly = true)
    public Result<List<Map<String, Object>>, String> listActiveDeployments() {
        try {
            var deployments = deploymentRepository.findByStatusOrderByDeployedAtDesc("DEPLOYED");
            
            var deploymentList = deployments.stream()
                .map(deployment -> Map.of(
                    "deploymentId", deployment.getDeploymentId(),
                    "modelName", deployment.getModelName(),
                    "modelVersion", deployment.getModelVersion(),
                    "replicas", deployment.getReplicas(),
                    "endpoint", deployment.getEndpoint(),
                    "deployedAt", deployment.getDeployedAt().toString()
                ))
                .map(map -> Map.<String, Object>copyOf(map))
                .toList();
            
            return Result.<List<Map<String, Object>>, String>success(deploymentList);
            
        } catch (Exception e) {
            log.error("Failed to list active deployments", e);
            return Result.failure("Failed to list active deployments: " + e.getMessage());
        }
    }
    
    // Private helper methods
    
    private Deployment createKubernetesDeployment(ModelDeploymentRequest request, String deploymentId) {
        return new io.fabric8.kubernetes.api.model.apps.DeploymentBuilder()
            .withNewMetadata()
                .withName("model-" + deploymentId)
                .withNamespace("ml-platform")
                .addToLabels("app", "ml-model")
                .addToLabels("model", request.modelName())
                .addToLabels("version", request.modelVersion())
            .endMetadata()
            .withNewSpec()
                .withReplicas(request.replicas() != null ? request.replicas() : 1)
                .withNewSelector()
                    .addToMatchLabels("app", "ml-model-" + deploymentId)
                .endSelector()
                .withNewTemplate()
                    .withNewMetadata()
                        .addToLabels("app", "ml-model-" + deploymentId)
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("model-server")
                            .withImage("tensorflow/serving:latest")
                            .addNewPort()
                                .withContainerPort(8501)
                                .withName("http")
                            .endPort()
                            .withNewResources()
                                .addToRequests("cpu", new io.fabric8.kubernetes.api.model.Quantity("500m"))
                                .addToRequests("memory", new io.fabric8.kubernetes.api.model.Quantity("512Mi"))
                                .addToLimits("cpu", new io.fabric8.kubernetes.api.model.Quantity("1000m"))
                                .addToLimits("memory", new io.fabric8.kubernetes.api.model.Quantity("1Gi"))
                            .endResources()
                            .addNewEnv()
                                .withName("MODEL_NAME")
                                .withValue(request.modelName())
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }
    
    private io.fabric8.kubernetes.api.model.Service createKubernetesService(
            ModelDeploymentRequest request, String deploymentId) {
        
        return new io.fabric8.kubernetes.api.model.ServiceBuilder()
            .withNewMetadata()
                .withName("model-service-" + deploymentId)
                .withNamespace("ml-platform")
            .endMetadata()
            .withNewSpec()
                .addToSelector("app", "ml-model-" + deploymentId)
                .addNewPort()
                    .withPort(8501)
                    .withTargetPort(new io.fabric8.kubernetes.api.model.IntOrString(8501))
                    .withName("http")
                .endPort()
                .withType("ClusterIP")
            .endSpec()
            .build();
    }
    
    private io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscaler createHorizontalPodAutoscaler(
            ModelDeploymentRequest request, String deploymentId) {
        
        return new io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscalerBuilder()
            .withNewMetadata()
                .withName("model-hpa-" + deploymentId)
                .withNamespace("ml-platform")
            .endMetadata()
            .withNewSpec()
                .withNewScaleTargetRef()
                    .withApiVersion("apps/v1")
                    .withKind("Deployment")
                    .withName("model-" + deploymentId)
                .endScaleTargetRef()
                .withMinReplicas(1)
                .withMaxReplicas(request.replicas() != null ? request.replicas() * 3 : 3)
                .withTargetCPUUtilizationPercentage(70)
            .endSpec()
            .build();
    }
    
    private boolean waitForDeploymentReady(String deploymentId, Duration timeout) {
        var endTime = System.currentTimeMillis() + timeout.toMillis();
        
        while (System.currentTimeMillis() < endTime) {
            try {
                var deployment = kubernetesClient.apps().deployments()
                    .inNamespace("ml-platform")
                    .withName("model-" + deploymentId)
                    .get();
                
                if (deployment != null && deployment.getStatus() != null) {
                    var readyReplicas = deployment.getStatus().getReadyReplicas();
                    var replicas = deployment.getSpec().getReplicas();
                    
                    if (readyReplicas != null && readyReplicas.equals(replicas)) {
                        return true;
                    }
                }
                
                Thread.sleep(5000); // Wait 5 seconds
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                log.warn("Error checking deployment status: {}", deploymentId, e);
            }
        }
        
        return false;
    }
    
    private Result<String, String> getModelEndpoint(String modelName, String modelVersion) {
        var modelKey = modelName + ":" + (modelVersion != null ? modelVersion : "latest");
        
        // Check cache first
        var cachedEndpoint = modelEndpoints.get(modelKey);
        if (cachedEndpoint != null) {
            return Result.success(cachedEndpoint);
        }
        
        // Look up in database
        var deployment = deploymentRepository.findByModelNameAndModelVersionAndStatus(
            modelName, 
            modelVersion != null ? modelVersion : "latest", 
            "DEPLOYED"
        );
        
        if (deployment.isPresent()) {
            var endpoint = deployment.get().getEndpoint();
            modelEndpoints.put(modelKey, endpoint); // Cache for future use
            return Result.success(endpoint);
        }
        
        return Result.failure("Model endpoint not found for: " + modelKey);
    }
    
    private String buildServiceEndpoint(String deploymentId) {
        return "http://model-service-" + deploymentId + ".ml-platform.svc.cluster.local:8501";
    }
    
    private double extractConfidence(com.fasterxml.jackson.databind.JsonNode prediction) {
        // This is a simplified confidence extraction
        // In real scenarios, this would depend on the model output format
        if (prediction.isArray() && prediction.size() > 0) {
            var maxValue = 0.0;
            for (var value : prediction) {
                if (value.isNumber() && value.doubleValue() > maxValue) {
                    maxValue = value.doubleValue();
                }
            }
            return maxValue;
        } else if (prediction.isNumber()) {
            return Math.abs(prediction.doubleValue());
        }
        return 0.5; // Default confidence
    }
    
    private void logPrediction(PredictionRequest request, PredictionResponse response, long latency) {
        try {
            var modelPrediction = ModelPrediction.builder()
                .predictionId(response.predictionId())
                .modelName(request.modelName())
                .modelVersion(response.modelVersion())
                .inputFeatures(request.features())
                .predictionResult(response.prediction())
                .confidenceScore(response.confidence())
                .inferenceLatencyMs((int) latency)
                .predictionTimestamp(LocalDateTime.now())
                .build();
            
            predictionRepository.save(modelPrediction);
            requestCounter.incrementAndGet();
            
        } catch (Exception e) {
            log.warn("Failed to log prediction: {}", response.predictionId(), e);
        }
    }
    
    /**
     * Health check for model serving infrastructure
     */
    public Result<Map<String, Object>, String> healthCheck() {
        try {
            // Check Kubernetes connectivity
            var namespaces = kubernetesClient.namespaces().list();
            var k8sStatus = namespaces != null ? "UP" : "DOWN";
            
            // Get active deployments count
            var activeDeployments = deploymentRepository.countByStatus("DEPLOYED");
            
            // Get request metrics
            var totalRequests = requestCounter.get();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "kubernetes_connection", k8sStatus,
                "active_deployments", (Object) activeDeployments,
                "total_requests", (Object) totalRequests,
                "cached_endpoints", (Object) modelEndpoints.size(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            return Result.<Map<String, Object>, String>success(health);
            
        } catch (Exception e) {
            log.error("Model serving health check failed", e);
            
            var health = Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            return Result.failure("Model serving health check failed: " + e.getMessage());
        }
    }
}