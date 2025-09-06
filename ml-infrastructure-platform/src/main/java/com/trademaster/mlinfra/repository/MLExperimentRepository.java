package com.trademaster.mlinfra.repository;

import com.trademaster.mlinfra.domain.entity.MLExperiment;
import com.trademaster.mlinfra.domain.types.ExperimentStatus;
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
 * ML Experiment Repository
 * 
 * Data access layer for ML experiments with optimized queries:
 * - Experiment lifecycle management
 * - Performance tracking and analytics
 * - MLflow integration support
 * - Efficient pagination and filtering
 */
@Repository
public interface MLExperimentRepository extends JpaRepository<MLExperiment, Long> {

    /**
     * Find experiment by name
     */
    Optional<MLExperiment> findByExperimentName(String experimentName);

    /**
     * Find experiment by MLflow run ID
     */
    Optional<MLExperiment> findByMlflowRunId(String mlflowRunId);

    /**
     * Find experiment by MLflow experiment ID
     */
    List<MLExperiment> findByMlflowExperimentId(String mlflowExperimentId);

    /**
     * Find experiments by status
     */
    List<MLExperiment> findByExperimentStatus(ExperimentStatus status);

    /**
     * Find running experiments
     */
    List<MLExperiment> findByExperimentStatusOrderByStartedAtDesc(ExperimentStatus status);

    /**
     * Find experiments by model type
     */
    List<MLExperiment> findByModelTypeOrderByCreatedAtDesc(String modelType);

    /**
     * Find experiments created by user
     */
    List<MLExperiment> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find experiments within date range
     */
    @Query("SELECT e FROM MLExperiment e WHERE e.startedAt BETWEEN :startDate AND :endDate ORDER BY e.startedAt DESC")
    List<MLExperiment> findByStartedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all experiments ordered by creation date
     */
    Page<MLExperiment> findAllOrderByCreatedAt(Pageable pageable);

    /**
     * Find experiments by status with pagination
     */
    Page<MLExperiment> findByExperimentStatusOrderByStartedAtDesc(ExperimentStatus status, Pageable pageable);

    /**
     * Count experiments by status
     */
    long countByExperimentStatus(ExperimentStatus status);

    /**
     * Count total experiments
     */
    @Query("SELECT COUNT(e) FROM MLExperiment e")
    long countTotal();

    /**
     * Find long-running experiments (running for more than specified hours)
     */
    @Query("SELECT e FROM MLExperiment e WHERE e.experimentStatus = 'RUNNING' AND e.startedAt < :cutoffTime")
    List<MLExperiment> findLongRunningExperiments(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find experiments with specific tag
     */
    @Query("SELECT e FROM MLExperiment e WHERE JSON_EXTRACT(e.tags, :tagPath) = :tagValue")
    List<MLExperiment> findByTag(@Param("tagPath") String tagPath, @Param("tagValue") String tagValue);

    /**
     * Find top performing experiments by metric
     */
    @Query(value = "SELECT e.* FROM ml_experiments e WHERE JSON_EXTRACT(e.metrics, :metricPath) IS NOT NULL ORDER BY CAST(JSON_EXTRACT(e.metrics, :metricPath) AS DECIMAL(10,6)) DESC LIMIT :limit", 
           nativeQuery = true)
    List<MLExperiment> findTopPerformingByMetric(@Param("metricPath") String metricPath, @Param("limit") int limit);

    /**
     * Find recent experiments by model type
     */
    @Query("SELECT e FROM MLExperiment e WHERE e.modelType = :modelType AND e.startedAt >= :sinceDate ORDER BY e.startedAt DESC")
    List<MLExperiment> findRecentByModelType(
        @Param("modelType") String modelType, 
        @Param("sinceDate") LocalDateTime sinceDate
    );

    /**
     * Get experiment statistics by status
     */
    @Query("SELECT e.experimentStatus, COUNT(e) FROM MLExperiment e GROUP BY e.experimentStatus")
    List<Object[]> getExperimentStatsByStatus();

    /**
     * Get experiment statistics by model type
     */
    @Query("SELECT e.modelType, COUNT(e), AVG(CASE WHEN e.completedAt IS NOT NULL AND e.startedAt IS NOT NULL THEN EXTRACT(EPOCH FROM (e.completedAt - e.startedAt))/3600.0 END) FROM MLExperiment e GROUP BY e.modelType")
    List<Object[]> getExperimentStatsByModelType();

    /**
     * Find experiments needing cleanup (completed/failed for more than specified days)
     */
    @Query("SELECT e FROM MLExperiment e WHERE e.experimentStatus IN ('COMPLETED', 'FAILED', 'CANCELLED') AND e.completedAt < :cutoffDate")
    List<MLExperiment> findExperimentsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old experiments by IDs
     */
    void deleteByIdIn(List<Long> ids);

    /**
     * Search experiments by name pattern
     */
    List<MLExperiment> findByExperimentNameContainingIgnoreCaseOrderByCreatedAtDesc(String namePattern);

    /**
     * Find experiments with specific hyperparameter
     */
    @Query("SELECT e FROM MLExperiment e WHERE JSON_EXTRACT(e.hyperparameters, :paramPath) = :paramValue")
    List<MLExperiment> findByHyperparameter(@Param("paramPath") String paramPath, @Param("paramValue") String paramValue);

    /**
     * Get average experiment duration by model type
     */
    @Query("SELECT e.modelType, AVG(CASE WHEN e.completedAt IS NOT NULL AND e.startedAt IS NOT NULL THEN EXTRACT(EPOCH FROM (e.completedAt - e.startedAt))/3600.0 END) as avgDurationHours FROM MLExperiment e WHERE e.experimentStatus = 'COMPLETED' GROUP BY e.modelType")
    List<Object[]> getAverageExperimentDuration();

    /**
     * Find successful experiments for model recommendations
     */
    @Query("SELECT e FROM MLExperiment e WHERE e.experimentStatus = 'COMPLETED' AND JSON_EXTRACT(e.metrics, :metricPath) >= :threshold ORDER BY CAST(JSON_EXTRACT(e.metrics, :metricPath) AS DECIMAL(10,6)) DESC")
    List<MLExperiment> findSuccessfulExperiments(
        @Param("metricPath") String metricPath, 
        @Param("threshold") double threshold
    );
}