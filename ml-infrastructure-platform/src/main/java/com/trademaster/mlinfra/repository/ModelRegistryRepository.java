package com.trademaster.mlinfra.repository;

import com.trademaster.mlinfra.domain.entity.ModelRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Model Registry Repository
 * 
 * Data access layer for model registry with advanced queries:
 * - Model lifecycle and stage management
 * - Version control and deployment tracking
 * - Performance monitoring and analytics
 * - Production model management
 */
@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {

    /**
     * Find model by name and version
     */
    Optional<ModelRegistry> findByModelNameAndModelVersion(String modelName, String modelVersion);

    /**
     * Find all versions of a model
     */
    List<ModelRegistry> findByModelNameOrderByCreatedAtDesc(String modelName);

    /**
     * Find models by stage
     */
    List<ModelRegistry> findByModelStage(String modelStage);

    /**
     * Find production models
     */
    List<ModelRegistry> findByModelStageOrderByDeployedAtDesc(String modelStage);

    /**
     * Find latest version of a model
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelName = :modelName ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ModelRegistry> findLatestVersion(@Param("modelName") String modelName);

    /**
     * Find latest production model
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelName = :modelName AND m.modelStage = 'Production' ORDER BY m.deployedAt DESC LIMIT 1")
    Optional<ModelRegistry> findLatestProductionModel(@Param("modelName") String modelName);

    /**
     * Find models by run ID
     */
    List<ModelRegistry> findByRunId(String runId);

    /**
     * Find models by training experiment
     */
    List<ModelRegistry> findByTrainingExperimentId(Long trainingExperimentId);

    /**
     * Find models created within date range
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<ModelRegistry> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find models deployed within date range
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.deployedAt BETWEEN :startDate AND :endDate ORDER BY m.deployedAt DESC")
    List<ModelRegistry> findByDeployedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count models by stage
     */
    long countByModelStage(String modelStage);

    /**
     * Find models by type
     */
    List<ModelRegistry> findByModelTypeOrderByCreatedAtDesc(String modelType);

    /**
     * Find models needing refresh (old production models)
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelStage = 'Production' AND m.deployedAt < :cutoffDate ORDER BY m.deployedAt ASC")
    List<ModelRegistry> findModelsNeedingRefresh(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find top performing models by metric
     */
    @Query(value = "SELECT m.* FROM model_registry m WHERE JSON_EXTRACT(m.performance_metrics, :metricPath) IS NOT NULL ORDER BY CAST(JSON_EXTRACT(m.performance_metrics, :metricPath) AS DECIMAL(10,6)) DESC LIMIT :limit", 
           nativeQuery = true)
    List<ModelRegistry> findTopPerformingByMetric(@Param("metricPath") String metricPath, @Param("limit") int limit);

    /**
     * Find models with specific performance threshold
     */
    @Query("SELECT m FROM ModelRegistry m WHERE JSON_EXTRACT(m.performanceMetrics, :metricPath) >= :threshold ORDER BY CAST(JSON_EXTRACT(m.performanceMetrics, :metricPath) AS DECIMAL(10,6)) DESC")
    List<ModelRegistry> findModelsByPerformanceThreshold(
        @Param("metricPath") String metricPath, 
        @Param("threshold") double threshold
    );

    /**
     * Find models for A/B testing (multiple production models of same name)
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelName IN (SELECT m2.modelName FROM ModelRegistry m2 WHERE m2.modelStage = 'Production' GROUP BY m2.modelName HAVING COUNT(m2) > 1) AND m.modelStage = 'Production' ORDER BY m.modelName, m.deployedAt DESC")
    List<ModelRegistry> findModelsForABTesting();

    /**
     * Get model statistics by stage
     */
    @Query("SELECT m.modelStage, COUNT(m) FROM ModelRegistry m GROUP BY m.modelStage")
    List<Object[]> getModelStatsByStage();

    /**
     * Get model statistics by type
     */
    @Query("SELECT m.modelType, COUNT(m), COUNT(CASE WHEN m.modelStage = 'Production' THEN 1 END) FROM ModelRegistry m GROUP BY m.modelType")
    List<Object[]> getModelStatsByType();

    /**
     * Find models by metadata attribute
     */
    @Query("SELECT m FROM ModelRegistry m WHERE JSON_EXTRACT(m.metadata, :attributePath) = :attributeValue")
    List<ModelRegistry> findByMetadataAttribute(@Param("attributePath") String attributePath, @Param("attributeValue") String attributeValue);

    /**
     * Find recently deployed production models
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelStage = 'Production' AND m.deployedAt >= :sinceDate ORDER BY m.deployedAt DESC")
    List<ModelRegistry> findRecentProductionModels(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find models pending promotion (staging models)
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelStage = 'Staging' ORDER BY m.createdAt ASC")
    List<ModelRegistry> findModelsPendingPromotion();

    /**
     * Find archived models for cleanup
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelStage = 'Archived' OR m.retiredAt < :cutoffDate ORDER BY m.retiredAt ASC")
    List<ModelRegistry> findArchivedModelsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Search models by name pattern
     */
    List<ModelRegistry> findByModelNameContainingIgnoreCaseOrderByCreatedAtDesc(String namePattern);

    /**
     * Find models with validation results
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.validationResults IS NOT NULL AND JSON_LENGTH(m.validationResults) > 0")
    List<ModelRegistry> findModelsWithValidationResults();

    /**
     * Get average model performance by type
     */
    @Query("SELECT m.modelType, AVG(CAST(JSON_EXTRACT(m.performanceMetrics, :metricPath) AS DECIMAL(10,6))) FROM ModelRegistry m WHERE JSON_EXTRACT(m.performanceMetrics, :metricPath) IS NOT NULL GROUP BY m.modelType")
    List<Object[]> getAveragePerformanceByType(@Param("metricPath") String metricPath);

    /**
     * Find models ready for promotion to production
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelStage = 'Staging' AND JSON_EXTRACT(m.validationResults, '$.approved') = true ORDER BY m.createdAt ASC")
    List<ModelRegistry> findModelsReadyForProduction();

    /**
     * Count models by name (version count)
     */
    @Query("SELECT m.modelName, COUNT(m) FROM ModelRegistry m GROUP BY m.modelName ORDER BY COUNT(m) DESC")
    List<Object[]> getModelVersionCounts();

    /**
     * Find models with deployment configuration
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.deploymentConfig IS NOT NULL AND JSON_LENGTH(m.deploymentConfig) > 0")
    List<ModelRegistry> findModelsWithDeploymentConfig();

    /**
     * Delete archived models by IDs
     */
    void deleteByIdInAndModelStage(List<Long> ids, String modelStage);

    /**
     * Find models for performance comparison
     */
    @Query("SELECT m FROM ModelRegistry m WHERE m.modelName = :modelName AND JSON_EXTRACT(m.performanceMetrics, :metricPath) IS NOT NULL ORDER BY m.createdAt DESC LIMIT :limit")
    List<ModelRegistry> findModelsForComparison(
        @Param("modelName") String modelName, 
        @Param("metricPath") String metricPath, 
        @Param("limit") int limit
    );
}