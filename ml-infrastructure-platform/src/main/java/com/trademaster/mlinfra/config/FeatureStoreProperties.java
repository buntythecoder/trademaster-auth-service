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
 * Feature Store Configuration Properties
 * 
 * Configuration for feature store implementation:
 * - Redis for real-time features
 * - PostgreSQL for batch features
 * - Feature versioning and lineage
 * - Data quality monitoring
 */
@Validated
@ConfigurationProperties(prefix = "ml.feature-store")
public record FeatureStoreProperties(
    
    @Valid @NotNull
    RedisConfig redis,
    
    @Valid @NotNull
    PostgresConfig postgres,
    
    @Valid @NotNull
    FeatureConfig features,
    
    @Valid @NotNull
    MonitoringConfig monitoring
) {

    /**
     * Redis Configuration for Real-time Features
     */
    public record RedisConfig(
        @NotBlank String host,
        @Positive int port,
        @NotBlank String password,
        @Positive int database,
        Duration timeout,
        @Positive int maxConnections,
        boolean ssl
    ) {}

    /**
     * PostgreSQL Configuration for Batch Features
     */
    public record PostgresConfig(
        @NotBlank String url,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String schema,
        @Positive int maxPoolSize,
        Duration connectionTimeout,
        boolean ssl
    ) {}

    /**
     * Feature Configuration
     */
    public record FeatureConfig(
        Duration defaultTtl,
        @Positive int maxFeatureGroups,
        @Positive int maxFeaturesPerGroup,
        boolean enableVersioning,
        boolean enableLineage,
        @Valid @NotNull
        ValidationConfig validation,
        List<String> supportedDataTypes
    ) {
        
        public record ValidationConfig(
            boolean enabled,
            Duration validationTimeout,
            List<String> validationRules,
            @Positive int maxValidationErrors
        ) {}
    }

    /**
     * Feature Store Monitoring Configuration
     */
    public record MonitoringConfig(
        boolean enableDataDrift,
        boolean enableDataQuality,
        Duration monitoringInterval,
        @Valid @NotNull
        DriftConfig drift,
        @Valid @NotNull
        QualityConfig quality
    ) {
        
        public record DriftConfig(
            double threshold,
            @NotBlank String method,
            Duration referenceWindow,
            Duration comparisonWindow,
            List<String> monitoredFeatures
        ) {}
        
        public record QualityConfig(
            @Positive int minSamples,
            double missingValueThreshold,
            double outlierThreshold,
            List<String> qualityMetrics
        ) {}
    }

    /**
     * Default feature store configuration values
     */
    public static FeatureStoreProperties defaults() {
        return new FeatureStoreProperties(
            new RedisConfig(
                "localhost",
                6379,
                "",
                0,
                Duration.ofSeconds(5),
                100,
                false
            ),
            new PostgresConfig(
                "jdbc:postgresql://localhost:5432/ml_platform",
                "postgres",
                "postgres",
                "feature_store",
                20,
                Duration.ofSeconds(10),
                false
            ),
            new FeatureConfig(
                Duration.ofHours(24),
                100,
                1000,
                true,
                true,
                new FeatureConfig.ValidationConfig(
                    true,
                    Duration.ofSeconds(10),
                    List.of(
                        "not_null",
                        "range_check",
                        "type_check",
                        "pattern_match"
                    ),
                    10
                ),
                List.of(
                    "string",
                    "integer",
                    "float",
                    "boolean",
                    "timestamp",
                    "array",
                    "json"
                )
            ),
            new MonitoringConfig(
                true,
                true,
                Duration.ofMinutes(15),
                new MonitoringConfig.DriftConfig(
                    0.05,
                    "ks_test",
                    Duration.ofDays(7),
                    Duration.ofDays(1),
                    List.of()
                ),
                new MonitoringConfig.QualityConfig(
                    1000,
                    0.1,
                    0.05,
                    List.of(
                        "completeness",
                        "uniqueness",
                        "validity",
                        "consistency",
                        "timeliness"
                    )
                )
            )
        );
    }
}