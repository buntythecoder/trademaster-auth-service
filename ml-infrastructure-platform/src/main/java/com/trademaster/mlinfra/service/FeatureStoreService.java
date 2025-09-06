package com.trademaster.mlinfra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.mlinfra.config.FeatureStoreProperties;
import com.trademaster.mlinfra.domain.dto.FeatureDto;
import com.trademaster.mlinfra.domain.dto.FeatureComputeRequest;
import com.trademaster.mlinfra.domain.dto.FeatureComputeResponse;
import com.trademaster.mlinfra.domain.entity.FeatureDefinition;
import com.trademaster.mlinfra.functional.Result;
import com.trademaster.mlinfra.repository.FeatureDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Feature Store Service
 * 
 * Provides high-performance feature store operations:
 * - Real-time feature computation and serving
 * - Batch feature processing
 * - Feature versioning and lineage
 * - Data quality monitoring
 * 
 * Uses Redis for real-time features and PostgreSQL for batch features.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeatureStoreService {

    private final FeatureStoreProperties featureStoreProperties;
    private final FeatureDefinitionRepository featureDefinitionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String FEATURE_KEY_PREFIX = "feature:";
    private static final String FEATURE_METADATA_PREFIX = "feature_meta:";
    private static final String FEATURE_VERSION_PREFIX = "feature_version:";
    
    /**
     * Register new feature definition
     */
    @Async("featureExecutor")
    public CompletableFuture<Result<FeatureDto, String>> registerFeature(
            String featureName,
            String featureVersion,
            String definition,
            String dataType,
            String computationType,
            Map<String, Object> validationRules) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Registering feature: {} version: {}", featureName, featureVersion);
                
                // Validate feature definition
                var validationResult = validateFeatureDefinition(definition, dataType, validationRules);
                if (validationResult.isFailure()) {
                    return Result.failure("Feature validation failed: " + validationResult.getError());
                }
                
                // Check if feature already exists
                var existingFeature = featureDefinitionRepository
                    .findByFeatureNameAndFeatureVersion(featureName, featureVersion);
                
                if (existingFeature.isPresent()) {
                    log.warn("Feature already exists: {} version: {}", featureName, featureVersion);
                    return Result.failure("Feature already exists: " + featureName + " version: " + featureVersion);
                }
                
                // Save feature definition
                var feature = FeatureDefinition.builder()
                    .featureName(featureName)
                    .featureVersion(featureVersion)
                    .definition(definition)
                    .dataType(dataType)
                    .computationType(computationType)
                    .validationRules(validationRules)
                    .createdAt(LocalDateTime.now())
                    .isActive(true)
                    .build();
                
                var savedFeature = featureDefinitionRepository.save(feature);
                
                // Cache feature metadata in Redis
                cacheFeatureMetadata(savedFeature);
                
                var featureDto = FeatureDto.fromEntity(savedFeature);
                
                log.info("Registered feature: {} version: {}", featureName, featureVersion);
                return Result.success(featureDto);
                
            } catch (Exception e) {
                log.error("Failed to register feature: {} version: {}", featureName, featureVersion, e);
                return Result.failure("Failed to register feature: " + e.getMessage());
            }
        });
    }
    
    /**
     * Compute features for real-time serving
     */
    @Async("featureExecutor")
    public CompletableFuture<Result<FeatureComputeResponse, String>> computeFeatures(
            FeatureComputeRequest request) {
        
        return CompletableFuture.supplyAsync(() -> {
            var startTime = System.currentTimeMillis();
            
            try {
                log.debug("Computing features for entities: {}", request.entityIds());
                
                // Get feature definitions
                var featureDefinitions = getFeatureDefinitions(request.featureIds());
                if (featureDefinitions.isEmpty()) {
                    return Result.failure("No valid feature definitions found");
                }
                
                var computedFeatures = new HashMap<String, Object>();
                
                // Compute each feature
                for (var featureDefinition : featureDefinitions) {
                    var computeResult = computeSingleFeature(
                        featureDefinition, 
                        request.entityIds().toString(), 
                        request.timestamp() != null ? request.timestamp().toString() : Instant.now().toString()
                    );
                    
                    if (computeResult.isSuccess()) {
                        computedFeatures.put(
                            featureDefinition.getFeatureName(), 
                            computeResult.getValue()
                        );
                    } else {
                        log.warn("Failed to compute feature: {}, error: {}", 
                            featureDefinition.getFeatureName(), computeResult.getError());
                        
                        // Use default value or skip based on configuration
                        computedFeatures.put(featureDefinition.getFeatureName(), null);
                    }
                }
                
                // Cache computed features for future use
                // Real-time features (simplified for compatibility)
                if (request.useCache() != null && !request.useCache()) {
                    cacheComputedFeatures(request.entityIds().toString(), computedFeatures);
                }
                
                var latency = System.currentTimeMillis() - startTime;
                
                var response = FeatureComputeResponse.success(
                    request.entityIds().toString(),
                    computedFeatures,
                    false, // Not from cache
                    latency
                );
                
                log.debug("Computed {} features for entity {} in {}ms", 
                    computedFeatures.size(), request.entityIds().toString(), latency);
                
                return Result.success(response);
                
            } catch (Exception e) {
                var latency = System.currentTimeMillis() - startTime;
                log.error("Failed to compute features for entity: {} after {}ms", 
                    request.entityIds().toString(), latency, e);
                return Result.failure("Failed to compute features: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get cached features from Redis
     */
    @Async("featureExecutor")
    public CompletableFuture<Result<Map<String, Object>, String>> getCachedFeatures(
            String entityId,
            List<String> featureNames) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                var cachedFeatures = new HashMap<String, Object>();
                
                for (var featureName : featureNames) {
                    var key = buildFeatureKey(entityId, featureName);
                    var value = redisTemplate.opsForValue().get(key);
                    
                    if (value != null) {
                        cachedFeatures.put(featureName, value);
                    }
                }
                
                log.debug("Retrieved {} cached features for entity: {}", 
                    cachedFeatures.size(), entityId);
                
                return Result.success(cachedFeatures);
                
            } catch (Exception e) {
                log.error("Failed to get cached features for entity: {}", entityId, e);
                return Result.failure("Failed to get cached features: " + e.getMessage());
            }
        });
    }
    
    /**
     * Store features in batch mode
     */
    @Async("featureExecutor")
    public CompletableFuture<Result<Integer, String>> storeBatchFeatures(
            List<Map<String, Object>> featureBatch) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Storing batch of {} feature records", featureBatch.size());
                
                var storedCount = 0;
                
                for (var featureRecord : featureBatch) {
                    var entityId = (String) featureRecord.get("entity_id");
                    if (entityId == null) {
                        log.warn("Skipping feature record without entity_id");
                        continue;
                    }
                    
                    // Store in Redis with TTL
                    var ttl = featureStoreProperties.features().defaultTtl();
                    
                    featureRecord.forEach((key, value) -> {
                        if (!"entity_id".equals(key) && value != null) {
                            var redisKey = buildFeatureKey(entityId, key);
                            redisTemplate.opsForValue().set(redisKey, value, ttl);
                        }
                    });
                    
                    storedCount++;
                }
                
                log.info("Stored {} feature records in batch", storedCount);
                return Result.success(storedCount);
                
            } catch (Exception e) {
                log.error("Failed to store batch features", e);
                return Result.failure("Failed to store batch features: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get feature definition by name and version
     */
    @Transactional(readOnly = true)
    public Result<FeatureDto, String> getFeatureDefinition(String featureName, String featureVersion) {
        try {
            return featureDefinitionRepository.findByFeatureNameAndFeatureVersion(featureName, featureVersion)
                .map(FeatureDto::fromEntity)
                .map(Result::<FeatureDto, String>success)
                .orElse(Result.failure("Feature not found: " + featureName + " version: " + featureVersion));
                
        } catch (Exception e) {
            log.error("Failed to get feature definition: {} version: {}", featureName, featureVersion, e);
            return Result.failure("Failed to get feature definition: " + e.getMessage());
        }
    }
    
    /**
     * List all active features
     */
    @Transactional(readOnly = true)
    public Result<List<FeatureDto>, String> listActiveFeatures() {
        try {
            var features = featureDefinitionRepository.findByIsActiveOrderByCreatedAtDesc(true);
            
            var featureDtos = features.stream()
                .map(FeatureDto::fromEntity)
                .toList();
                
            return Result.success(featureDtos);
            
        } catch (Exception e) {
            log.error("Failed to list active features", e);
            return Result.failure("Failed to list active features: " + e.getMessage());
        }
    }
    
    // Private helper methods
    
    private Result<Void, String> validateFeatureDefinition(
            String definition, 
            String dataType, 
            Map<String, Object> validationRules) {
        
        try {
            // Basic validation
            if (definition == null || definition.trim().isEmpty()) {
                return Result.failure("Feature definition cannot be empty");
            }
            
            // Validate data type
            var supportedTypes = featureStoreProperties.features().supportedDataTypes();
            if (!supportedTypes.contains(dataType)) {
                return Result.failure("Unsupported data type: " + dataType);
            }
            
            // Validate definition syntax (simplified)
            if (!definition.contains("SELECT") && !definition.contains("COMPUTE")) {
                return Result.failure("Invalid feature definition syntax");
            }
            
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure("Validation error: " + e.getMessage());
        }
    }
    
    private List<FeatureDefinition> getFeatureDefinitions(List<String> featureNames) {
        return featureNames.stream()
            .map(name -> featureDefinitionRepository.findByFeatureNameAndIsActive(name, true))
            .filter(opt -> opt.isPresent())
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    private Result<Object, String> computeSingleFeature(
            FeatureDefinition featureDefinition, 
            String entityId, 
            String timestamp) {
        
        try {
            // This is a simplified feature computation
            // In a real implementation, this would execute the feature definition
            // against the appropriate data sources (database, streaming data, etc.)
            
            var definition = featureDefinition.getDefinition();
            var dataType = featureDefinition.getDataType();
            
            // Mock feature computation based on data type
            Object computedValue = switch (dataType) {
                case "integer" -> new Random().nextInt(100);
                case "float" -> new Random().nextDouble() * 100.0;
                case "string" -> "computed_value_" + entityId;
                case "boolean" -> new Random().nextBoolean();
                case "timestamp" -> LocalDateTime.now().toString();
                default -> null;
            };
            
            return Result.success(computedValue);
            
        } catch (Exception e) {
            return Result.failure("Feature computation failed: " + e.getMessage());
        }
    }
    
    private void cacheFeatureMetadata(FeatureDefinition feature) {
        try {
            var key = FEATURE_METADATA_PREFIX + feature.getFeatureName() + ":" + feature.getFeatureVersion();
            var metadata = Map.of(
                "dataType", feature.getDataType(),
                "computationType", feature.getComputationType(),
                "isActive", feature.getIsActive(),
                "createdAt", feature.getCreatedAt().toString()
            );
            
            var ttl = featureStoreProperties.features().defaultTtl();
            redisTemplate.opsForValue().set(key, metadata, ttl);
            
        } catch (Exception e) {
            log.warn("Failed to cache feature metadata for: {}", feature.getFeatureName(), e);
        }
    }
    
    private void cacheComputedFeatures(String entityId, Map<String, Object> features) {
        try {
            var ttl = featureStoreProperties.features().defaultTtl();
            
            features.forEach((featureName, value) -> {
                var key = buildFeatureKey(entityId, featureName);
                redisTemplate.opsForValue().set(key, value, ttl);
            });
            
        } catch (Exception e) {
            log.warn("Failed to cache computed features for entity: {}", entityId, e);
        }
    }
    
    private String buildFeatureKey(String entityId, String featureName) {
        return FEATURE_KEY_PREFIX + entityId + ":" + featureName;
    }
    
    /**
     * Health check for feature store connectivity
     */
    public Result<Map<String, Object>, String> healthCheck() {
        try {
            // Test Redis connectivity
            redisTemplate.opsForValue().set("health_check", "test", Duration.ofSeconds(10));
            var redisTest = redisTemplate.opsForValue().get("health_check");
            
            // Test PostgreSQL connectivity
            var featureCount = featureDefinitionRepository.count();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "redis_connection", redisTest != null ? "UP" : "DOWN",
                "postgres_connection", "UP",
                "active_features", (Object) featureCount, // Cast to Object
                "timestamp", LocalDateTime.now().toString()
            );
            
            return Result.<Map<String, Object>, String>success(health);
            
        } catch (Exception e) {
            log.error("Feature store health check failed", e);
            
            var health = Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            );
            
            return Result.failure("Feature store health check failed: " + e.getMessage());
        }
    }
}