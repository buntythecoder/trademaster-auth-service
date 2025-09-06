package com.trademaster.behavioralai.repository;

import com.trademaster.behavioralai.domain.entity.TradingPsychologyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Trading Psychology Profile Repository
 * 
 * Repository interface for psychology profile data access with advanced analytics queries.
 */
@Repository
public interface TradingPsychologyProfileRepository extends JpaRepository<TradingPsychologyProfile, Long> {

    /**
     * Find profile by user ID
     */
    Optional<TradingPsychologyProfile> findByUserId(String userId);

    /**
     * Find profiles by trader type
     */
    List<TradingPsychologyProfile> findByTraderType(TradingPsychologyProfile.TraderType traderType);

    /**
     * Find profiles requiring intensive coaching
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.impulsivityScore > 0.8 " +
           "OR (p.overconfidenceScore > 0.8 AND p.emotionalStabilityScore < 0.4) " +
           "OR (p.lossAversionScore > 0.8 AND p.riskToleranceScore < 0.3)")
    List<TradingPsychologyProfile> findProfilesRequiringIntensiveCoaching();

    /**
     * Find high-risk traders
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.riskToleranceScore > 0.7 AND p.impulsivityScore > 0.6")
    List<TradingPsychologyProfile> findHighRiskTraders();

    /**
     * Find emotionally stable traders
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.emotionalStabilityScore > 0.7 AND p.impulsivityScore < 0.4")
    List<TradingPsychologyProfile> findEmotionallyStableTraders();

    /**
     * Find profiles updated since timestamp
     */
    List<TradingPsychologyProfile> findByLastUpdatedAfterOrderByLastUpdatedDesc(Instant since);

    /**
     * Find profiles with high confidence levels
     */
    List<TradingPsychologyProfile> findByConfidenceLevelGreaterThanOrderByConfidenceLevelDesc(Double minConfidence);

    /**
     * Get average scores by trader type
     */
    @Query("SELECT p.traderType, " +
           "AVG(p.riskToleranceScore), " +
           "AVG(p.emotionalStabilityScore), " +
           "AVG(p.impulsivityScore), " +
           "AVG(p.overconfidenceScore), " +
           "AVG(p.lossAversionScore) " +
           "FROM TradingPsychologyProfile p " +
           "GROUP BY p.traderType")
    List<Object[]> getAverageScoresByTraderType();

    /**
     * Find similar profiles based on score ranges
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.userId <> :userId " +
           "AND ABS(p.riskToleranceScore - :riskTolerance) <= :tolerance " +
           "AND ABS(p.emotionalStabilityScore - :emotionalStability) <= :tolerance " +
           "AND ABS(p.impulsivityScore - :impulsivity) <= :tolerance " +
           "ORDER BY " +
           "(ABS(p.riskToleranceScore - :riskTolerance) + " +
           "ABS(p.emotionalStabilityScore - :emotionalStability) + " +
           "ABS(p.impulsivityScore - :impulsivity)) ASC")
    List<TradingPsychologyProfile> findSimilarProfiles(
        @Param("userId") String userId,
        @Param("riskTolerance") Double riskTolerance,
        @Param("emotionalStability") Double emotionalStability,
        @Param("impulsivity") Double impulsivity,
        @Param("tolerance") Double tolerance);

    /**
     * Calculate profile maturity (time since creation and updates)
     */
    @Query("SELECT p.userId, " +
           "EXTRACT(EPOCH FROM (p.lastUpdated - p.createdAt)) / 86400 as ageInDays, " +
           "p.confidenceLevel " +
           "FROM TradingPsychologyProfile p " +
           "WHERE p.lastUpdated >= :since " +
           "ORDER BY ageInDays DESC")
    List<Object[]> calculateProfileMaturity(@Param("since") Instant since);

    /**
     * Find profiles with specific dominant patterns
     */
    List<TradingPsychologyProfile> findByDominantPatternOrderByConfidenceLevelDesc(
        com.trademaster.behavioralai.dto.BehavioralPatternData.PatternType dominantPattern);

    /**
     * Get trader type distribution
     */
    @Query("SELECT p.traderType, COUNT(p) * 100.0 / (SELECT COUNT(pp) FROM TradingPsychologyProfile pp) " +
           "FROM TradingPsychologyProfile p " +
           "GROUP BY p.traderType " +
           "ORDER BY COUNT(p) DESC")
    List<Object[]> getTraderTypeDistribution();

    /**
     * Find profiles with extreme scores (outliers)
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.riskToleranceScore > 0.9 " +
           "OR p.emotionalStabilityScore < 0.1 " +
           "OR p.impulsivityScore > 0.9 " +
           "OR p.overconfidenceScore > 0.9 " +
           "OR p.lossAversionScore > 0.9")
    List<TradingPsychologyProfile> findExtremeProfiles();

    /**
     * Check if user has profile
     */
    boolean existsByUserId(String userId);

    /**
     * Delete profiles older than retention period
     */
    void deleteByLastUpdatedBefore(Instant cutoffDate);

    /**
     * Find profiles for ML model training
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.confidenceLevel >= :minConfidence " +
           "AND p.lastUpdated >= :since " +
           "ORDER BY p.confidenceLevel DESC, p.lastUpdated DESC")
    List<TradingPsychologyProfile> findProfilesForTraining(
        @Param("minConfidence") Double minConfidence,
        @Param("since") Instant since);

    /**
     * Get score distribution statistics
     */
    @Query("SELECT " +
           "MIN(p.riskToleranceScore), MAX(p.riskToleranceScore), AVG(p.riskToleranceScore), " +
           "MIN(p.emotionalStabilityScore), MAX(p.emotionalStabilityScore), AVG(p.emotionalStabilityScore), " +
           "MIN(p.impulsivityScore), MAX(p.impulsivityScore), AVG(p.impulsivityScore), " +
           "MIN(p.overconfidenceScore), MAX(p.overconfidenceScore), AVG(p.overconfidenceScore), " +
           "MIN(p.lossAversionScore), MAX(p.lossAversionScore), AVG(p.lossAversionScore) " +
           "FROM TradingPsychologyProfile p")
    Object[] getScoreStatistics();

    /**
     * Find profiles needing recalibration (old or low confidence)
     */
    @Query("SELECT p FROM TradingPsychologyProfile p " +
           "WHERE p.lastUpdated < :staleThreshold " +
           "OR p.confidenceLevel < :minConfidence " +
           "ORDER BY p.lastUpdated ASC")
    List<TradingPsychologyProfile> findProfilesNeedingRecalibration(
        @Param("staleThreshold") Instant staleThreshold,
        @Param("minConfidence") Double minConfidence);
}