package com.trademaster.mlinfra.repository;

import com.trademaster.mlinfra.domain.entity.ModelPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Model Prediction Repository
 * 
 * Repository interface for model predictions following Spring Data JPA patterns.
 * Provides functional query methods for prediction tracking.
 */
@Repository
public interface ModelPredictionRepository extends JpaRepository<ModelPrediction, Long> {

    /**
     * Find prediction by prediction ID
     */
    Optional<ModelPrediction> findByPredictionId(String predictionId);

    /**
     * Find predictions by model name
     */
    List<ModelPrediction> findByModelNameOrderByPredictedAtDesc(String modelName);

    /**
     * Find predictions by model name and version
     */
    List<ModelPrediction> findByModelNameAndModelVersionOrderByPredictedAtDesc(String modelName, String modelVersion);

    /**
     * Find predictions by deployment
     */
    List<ModelPrediction> findByDeploymentNameOrderByPredictedAtDesc(String deploymentName);

    /**
     * Find predictions by user
     */
    List<ModelPrediction> findByUserIdOrderByPredictedAtDesc(String userId);

    /**
     * Find predictions after timestamp
     */
    List<ModelPrediction> findByPredictedAtAfterOrderByPredictedAtDesc(Instant since);

    /**
     * Find predictions in time range
     */
    List<ModelPrediction> findByPredictedAtBetweenOrderByPredictedAtDesc(Instant start, Instant end);

    /**
     * Find predictions with high confidence
     */
    @Query("SELECT p FROM ModelPrediction p WHERE p.confidence >= :minConfidence ORDER BY p.predictedAt DESC")
    List<ModelPrediction> findHighConfidencePredictions(@Param("minConfidence") Double minConfidence);

    /**
     * Find predictions by status
     */
    List<ModelPrediction> findByStatusOrderByPredictedAtDesc(String status);

    /**
     * Count predictions by model
     */
    @Query("SELECT COUNT(p) FROM ModelPrediction p WHERE p.modelName = :modelName")
    Long countByModelName(@Param("modelName") String modelName);

    /**
     * Count predictions in time range
     */
    @Query("SELECT COUNT(p) FROM ModelPrediction p WHERE p.predictedAt BETWEEN :start AND :end")
    Long countPredictionsInRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find predictions with pagination
     */
    Page<ModelPrediction> findByModelName(String modelName, Pageable pageable);

    /**
     * Find predictions by multiple criteria
     */
    @Query("SELECT p FROM ModelPrediction p WHERE " +
           "(:modelName IS NULL OR p.modelName = :modelName) AND " +
           "(:deploymentName IS NULL OR p.deploymentName = :deploymentName) AND " +
           "(:userId IS NULL OR p.userId = :userId) AND " +
           "(:minConfidence IS NULL OR p.confidence >= :minConfidence) AND " +
           "p.predictedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY p.predictedAt DESC")
    Page<ModelPrediction> findPredictionsByCriteria(
        @Param("modelName") String modelName,
        @Param("deploymentName") String deploymentName,
        @Param("userId") String userId,
        @Param("minConfidence") Double minConfidence,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);

    /**
     * Get average confidence by model
     */
    @Query("SELECT AVG(p.confidence) FROM ModelPrediction p WHERE p.modelName = :modelName")
    Double getAverageConfidenceByModel(@Param("modelName") String modelName);

    /**
     * Get prediction distribution by model
     */
    @Query("SELECT p.modelName, COUNT(p) FROM ModelPrediction p GROUP BY p.modelName ORDER BY COUNT(p) DESC")
    List<Object[]> getPredictionDistributionByModel();

    /**
     * Find recent predictions requiring feedback
     */
    @Query("SELECT p FROM ModelPrediction p WHERE p.feedbackReceived = false AND p.predictedAt >= :since ORDER BY p.predictedAt DESC")
    List<ModelPrediction> findPredictionsRequiringFeedback(@Param("since") Instant since);

    /**
     * Delete old predictions
     */
    void deleteByPredictedAtBefore(Instant cutoffDate);

    /**
     * Find predictions for A/B testing
     */
    @Query("SELECT p FROM ModelPrediction p WHERE p.experimentId IS NOT NULL AND p.predictedAt >= :since")
    List<ModelPrediction> findExperimentPredictions(@Param("since") Instant since);
}