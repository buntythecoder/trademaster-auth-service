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
 * Model Deployment Entity
 * 
 * Represents deployed models in Kubernetes:
 * - Deployment configuration and status
 * - Resource allocation and scaling
 * - Service endpoints and routing
 * - Performance metrics and monitoring
 */
@Entity
@Table(name = "model_deployments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@With
public class ModelDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", unique = true, nullable = false)
    private String deploymentId;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "deployment_name", nullable = false, length = 255)
    private String deploymentName;

    @Column(name = "environment", nullable = false, length = 50)
    private String environment;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "replicas")
    @Builder.Default
    private Integer replicas = 1;

    @Column(name = "min_replicas")
    @Builder.Default
    private Integer minReplicas = 1;

    @Column(name = "max_replicas")
    @Builder.Default
    private Integer maxReplicas = 10;

    @Column(name = "target_cpu_utilization")
    @Builder.Default
    private Integer targetCpuUtilization = 70;

    @Column(name = "endpoint", columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "namespace", length = 100)
    @Builder.Default
    private String namespace = "ml-platform";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_config", columnDefinition = "jsonb")
    private Map<String, Object> resourceConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environment_variables", columnDefinition = "jsonb")
    private Map<String, String> environmentVariables;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deployment_metadata", columnDefinition = "jsonb")
    private Map<String, Object> deploymentMetadata;

    @Column(name = "deployed_at")
    @Builder.Default
    private LocalDateTime deployedAt = LocalDateTime.now();

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Column(name = "health_status", length = 20)
    private String healthStatus;

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
     * Check if deployment is active
     */
    @JsonIgnore
    public boolean isActive() {
        return "DEPLOYED".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
    }

    /**
     * Check if deployment is healthy
     */
    @JsonIgnore
    public boolean isHealthy() {
        return "HEALTHY".equalsIgnoreCase(healthStatus) || "UP".equalsIgnoreCase(healthStatus);
    }

    /**
     * Check if deployment is scaling
     */
    @JsonIgnore
    public boolean isScaling() {
        return "SCALING".equalsIgnoreCase(status);
    }

    /**
     * Get model identifier
     */
    @JsonIgnore
    public String getModelIdentifier() {
        return modelName + ":" + modelVersion;
    }

    /**
     * Get deployment age in hours
     */
    @JsonIgnore
    public long getAgeInHours() {
        return java.time.Duration.between(deployedAt, LocalDateTime.now()).toHours();
    }

    /**
     * Get time since last health check in minutes
     */
    @JsonIgnore
    public Long getMinutesSinceLastHealthCheck() {
        if (lastHealthCheck == null) {
            return null;
        }
        return java.time.Duration.between(lastHealthCheck, LocalDateTime.now()).toMinutes();
    }

    /**
     * Check if health check is stale
     */
    @JsonIgnore
    public boolean isHealthCheckStale(int maxMinutesSinceHealthCheck) {
        var minutesSinceHealthCheck = getMinutesSinceLastHealthCheck();
        return minutesSinceHealthCheck != null && minutesSinceHealthCheck > maxMinutesSinceHealthCheck;
    }

    /**
     * Update deployment status
     */
    public ModelDeployment updateStatus(String newStatus) {
        this.status = newStatus;
        return this;
    }

    /**
     * Update health status
     */
    public ModelDeployment updateHealthStatus(String newHealthStatus) {
        this.healthStatus = newHealthStatus;
        this.lastHealthCheck = LocalDateTime.now();
        return this;
    }

    /**
     * Update replica count
     */
    public ModelDeployment updateReplicas(int newReplicas) {
        this.replicas = newReplicas;
        return this;
    }

    /**
     * Add environment variable
     */
    public ModelDeployment withEnvironmentVariable(String key, String value) {
        if (this.environmentVariables == null) {
            this.environmentVariables = new java.util.HashMap<>();
        }
        this.environmentVariables.put(key, value);
        return this;
    }

    /**
     * Add metadata
     */
    public ModelDeployment withMetadata(String key, Object value) {
        if (this.deploymentMetadata == null) {
            this.deploymentMetadata = new java.util.HashMap<>();
        }
        this.deploymentMetadata.put(key, value);
        return this;
    }

    /**
     * Set resource configuration
     */
    public ModelDeployment withResourceConfig(Map<String, Object> config) {
        this.resourceConfig = config;
        return this;
    }

    /**
     * Mark as deployed
     */
    public ModelDeployment markDeployed() {
        this.status = "DEPLOYED";
        this.deployedAt = LocalDateTime.now();
        return this;
    }

    /**
     * Mark as failed
     */
    public ModelDeployment markFailed() {
        this.status = "FAILED";
        return this;
    }

    /**
     * Mark as terminated
     */
    public ModelDeployment markTerminated() {
        this.status = "TERMINATED";
        return this;
    }

    /**
     * Factory method for creating deployment with defaults
     */
    public static ModelDeployment newDeployment(
            String modelName,
            String modelVersion,
            String deploymentName,
            String environment) {
        var entity = new ModelDeployment();
        entity.deploymentId = java.util.UUID.randomUUID().toString();
        entity.modelName = modelName;
        entity.modelVersion = modelVersion;
        entity.deploymentName = deploymentName;
        entity.environment = environment;
        entity.status = "PENDING";
        entity.replicas = 1;
        entity.minReplicas = 1;
        entity.maxReplicas = 10;
        entity.targetCpuUtilization = 70;
        entity.namespace = "ml-platform";
        entity.deployedAt = LocalDateTime.now();
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}