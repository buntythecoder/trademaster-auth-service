package com.trademaster.mlinfra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ML Infrastructure Configuration Properties
 * 
 * Centralized configuration for all ML platform components:
 * - Object storage (MinIO)
 * - Monitoring and metrics
 * - Performance tuning
 * - Security settings
 */
@Validated
@ConfigurationProperties(prefix = "ml.infrastructure")
public record MLInfrastructureProperties(
    
    @Valid @NotNull
    MinioConfig minio,
    
    @Valid @NotNull
    MonitoringConfig monitoring,
    
    @Valid @NotNull
    PerformanceConfig performance,
    
    @Valid @NotNull
    SecurityConfig security
) {

    /**
     * MinIO Object Storage Configuration
     * For model artifacts, datasets, and experiment logs
     */
    public record MinioConfig(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotBlank String region,
        boolean pathStyle
    ) {}

    /**
     * Monitoring and Observability Configuration
     */
    public record MonitoringConfig(
        @Valid @NotNull
        PrometheusConfig prometheus,
        
        @Valid @NotNull
        LoggingConfig logging,
        
        @Valid @NotNull
        TracingConfig tracing,
        
        boolean enabled
    ) {
        
        public record PrometheusConfig(
            @NotBlank String endpoint,
            @Positive int scrapeInterval,
            Map<String, String> labels
        ) {}
        
        public record LoggingConfig(
            @NotBlank String level,
            @NotBlank String pattern,
            boolean structured,
            List<String> excludeClasses
        ) {}
        
        public record TracingConfig(
            boolean enabled,
            @NotBlank String endpoint,
            double samplingRate
        ) {}
    }

    /**
     * Performance and Resource Configuration
     */
    public record PerformanceConfig(
        @Valid @NotNull
        InferenceConfig inference,
        
        @Valid @NotNull
        TrainingConfig training,
        
        @Valid @NotNull
        CachingConfig caching,
        
        @Valid @NotNull
        ResourceConfig resources
    ) {
        
        public record InferenceConfig(
            Duration timeout,
            @Positive int maxConcurrentRequests,
            @Positive int batchSize,
            Duration latencyTarget
        ) {}
        
        public record TrainingConfig(
            @Positive int maxParallelJobs,
            Duration jobTimeout,
            @NotBlank String defaultFramework,
            Map<String, Object> resourceLimits
        ) {}
        
        public record CachingConfig(
            Duration featureTtl,
            Duration modelTtl,
            Duration experimentTtl,
            @Positive int maxCacheSize
        ) {}
        
        public record ResourceConfig(
            @NotBlank String cpuLimit,
            @NotBlank String memoryLimit,
            @NotBlank String gpuLimit,
            @NotBlank String storageLimit
        ) {}
    }

    /**
     * Security and Authentication Configuration
     */
    public record SecurityConfig(
        @Valid @NotNull
        AuthenticationConfig authentication,
        
        @Valid @NotNull
        AuthorizationConfig authorization,
        
        @Valid @NotNull
        EncryptionConfig encryption,
        
        @Valid @NotNull
        AuditConfig audit
    ) {
        
        public record AuthenticationConfig(
            @NotBlank String jwtSecret,
            Duration jwtExpiration,
            @NotBlank String issuer,
            boolean requireAuthentication
        ) {}
        
        public record AuthorizationConfig(
            boolean enableRbac,
            List<String> adminRoles,
            List<String> userRoles,
            Map<String, List<String>> resourcePermissions
        ) {}
        
        public record EncryptionConfig(
            @NotBlank String algorithm,
            @NotBlank String keySize,
            boolean encryptAtRest,
            boolean encryptInTransit
        ) {}
        
        public record AuditConfig(
            boolean enabled,
            List<String> auditedOperations,
            Duration retentionPeriod,
            @NotBlank String logLevel
        ) {}
    }

    /**
     * Default configuration values
     */
    public static MLInfrastructureProperties defaults() {
        return new MLInfrastructureProperties(
            new MinioConfig(
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "ml-platform",
                "us-east-1",
                true
            ),
            new MonitoringConfig(
                new MonitoringConfig.PrometheusConfig(
                    "http://localhost:9090",
                    15,
                    Map.of("service", "ml-infrastructure")
                ),
                new MonitoringConfig.LoggingConfig(
                    "INFO",
                    "%d{HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n",
                    true,
                    List.of("org.apache.kafka", "org.springframework.kafka")
                ),
                new MonitoringConfig.TracingConfig(
                    true,
                    "http://localhost:14268/api/traces",
                    0.1
                ),
                true
            ),
            new PerformanceConfig(
                new PerformanceConfig.InferenceConfig(
                    Duration.ofSeconds(5),
                    1000,
                    32,
                    Duration.ofMillis(50)
                ),
                new PerformanceConfig.TrainingConfig(
                    5,
                    Duration.ofHours(24),
                    "pytorch",
                    Map.of(
                        "cpu", "4",
                        "memory", "8Gi",
                        "gpu", "1"
                    )
                ),
                new PerformanceConfig.CachingConfig(
                    Duration.ofHours(1),
                    Duration.ofDays(7),
                    Duration.ofDays(30),
                    10000
                ),
                new PerformanceConfig.ResourceConfig(
                    "2",
                    "4Gi",
                    "1",
                    "10Gi"
                )
            ),
            new SecurityConfig(
                new SecurityConfig.AuthenticationConfig(
                    "ml-platform-secret-key-change-in-production",
                    Duration.ofHours(24),
                    "ml-infrastructure-platform",
                    true
                ),
                new SecurityConfig.AuthorizationConfig(
                    true,
                    List.of("ADMIN", "ML_ENGINEER"),
                    List.of("USER", "VIEWER"),
                    Map.of(
                        "experiments", List.of("READ", "WRITE"),
                        "models", List.of("READ", "DEPLOY"),
                        "features", List.of("READ", "WRITE")
                    )
                ),
                new SecurityConfig.EncryptionConfig(
                    "AES-256-GCM",
                    "256",
                    true,
                    true
                ),
                new SecurityConfig.AuditConfig(
                    true,
                    List.of(
                        "MODEL_DEPLOY",
                        "EXPERIMENT_CREATE",
                        "FEATURE_CREATE",
                        "USER_LOGIN"
                    ),
                    Duration.ofDays(365),
                    "INFO"
                )
            )
        );
    }
}