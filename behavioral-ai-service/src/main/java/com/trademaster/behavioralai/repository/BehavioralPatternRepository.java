package com.trademaster.behavioralai.repository;

import com.trademaster.behavioralai.domain.entity.BehavioralPattern;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.EmotionAnalysisResult;
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
 * Behavioral Pattern Repository
 * 
 * Repository interface for behavioral pattern data access following Spring Data JPA patterns.
 * Provides functional query methods for behavioral analysis.
 */
@Repository
public interface BehavioralPatternRepository extends JpaRepository<BehavioralPattern, Long> {

    /**
     * Find patterns by user ID ordered by detection time
     */
    List<BehavioralPattern> findByUserIdOrderByDetectedAtDesc(String userId);

    /**
     * Find patterns by user ID with pagination
     */
    Page<BehavioralPattern> findByUserId(String userId, Pageable pageable);

    /**
     * Find patterns by user ID and pattern type
     */
    List<BehavioralPattern> findByUserIdAndPatternType(String userId, 
                                                      BehavioralPatternData.PatternType patternType);

    /**
     * Find patterns by user ID within time range
     */
    List<BehavioralPattern> findByUserIdAndDetectedAtBetween(String userId, 
                                                           Instant startTime, 
                                                           Instant endTime);

    /**
     * Find patterns by user ID after specified time, ordered by detection time descending
     */
    List<BehavioralPattern> findByUserIdAndDetectedAtAfterOrderByDetectedAtDesc(String userId, Instant since);

    /**
     * Find patterns by trading session
     */
    List<BehavioralPattern> findByTradingSessionIdOrderByDetectedAtDesc(String sessionId);

    /**
     * Find high-confidence patterns by user
     */
    List<BehavioralPattern> findByUserIdAndConfidenceScoreGreaterThanOrderByDetectedAtDesc(
        String userId, Double minConfidence);

    /**
     * Find patterns requiring intervention
     */
    @Query("SELECT p FROM BehavioralPattern p WHERE p.userId = :userId " +
           "AND p.interventionTriggered = false " +
           "AND p.confidenceScore > :minConfidence " +
           "AND p.patternType IN :highRiskPatterns " +
           "ORDER BY p.detectedAt DESC")
    List<BehavioralPattern> findPatternsRequiringIntervention(
        @Param("userId") String userId,
        @Param("minConfidence") Double minConfidence,
        @Param("highRiskPatterns") List<BehavioralPatternData.PatternType> highRiskPatterns);

    /**
     * Find recent patterns by emotion type
     */
    List<BehavioralPattern> findByUserIdAndEmotionalStateAndDetectedAtAfterOrderByDetectedAtDesc(
        String userId, EmotionAnalysisResult.EmotionType emotionalState, Instant since);

    /**
     * Count patterns by user and type in time period
     */
    @Query("SELECT COUNT(p) FROM BehavioralPattern p WHERE p.userId = :userId " +
           "AND p.patternType = :patternType " +
           "AND p.detectedAt BETWEEN :startTime AND :endTime")
    Long countPatternsByUserAndTypeInPeriod(
        @Param("userId") String userId,
        @Param("patternType") BehavioralPatternData.PatternType patternType,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);

    /**
     * Find most frequent pattern types for user
     */
    @Query("SELECT p.patternType, COUNT(p) as frequency " +
           "FROM BehavioralPattern p " +
           "WHERE p.userId = :userId " +
           "AND p.detectedAt >= :since " +
           "GROUP BY p.patternType " +
           "ORDER BY frequency DESC")
    List<Object[]> findMostFrequentPatternTypes(@Param("userId") String userId,
                                              @Param("since") Instant since);

    /**
     * Calculate average confidence by pattern type for user
     */
    @Query("SELECT p.patternType, AVG(p.confidenceScore) " +
           "FROM BehavioralPattern p " +
           "WHERE p.userId = :userId " +
           "AND p.detectedAt >= :since " +
           "GROUP BY p.patternType")
    List<Object[]> findAverageConfidenceByPatternType(@Param("userId") String userId,
                                                     @Param("since") Instant since);

    /**
     * Find patterns with high risk scores
     */
    List<BehavioralPattern> findByUserIdAndRiskScoreGreaterThanOrderByRiskScoreDesc(
        String userId, Double minRiskScore);

    /**
     * Find patterns by multiple criteria
     */
    @Query("SELECT p FROM BehavioralPattern p WHERE p.userId = :userId " +
           "AND (:patternType IS NULL OR p.patternType = :patternType) " +
           "AND (:minConfidence IS NULL OR p.confidenceScore >= :minConfidence) " +
           "AND (:emotionalState IS NULL OR p.emotionalState = :emotionalState) " +
           "AND (:sessionId IS NULL OR p.tradingSessionId = :sessionId) " +
           "AND p.detectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY p.detectedAt DESC")
    Page<BehavioralPattern> findPatternsByCriteria(
        @Param("userId") String userId,
        @Param("patternType") BehavioralPatternData.PatternType patternType,
        @Param("minConfidence") Double minConfidence,
        @Param("emotionalState") EmotionAnalysisResult.EmotionType emotionalState,
        @Param("sessionId") String sessionId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);

    /**
     * Find latest pattern by user and type
     */
    Optional<BehavioralPattern> findFirstByUserIdAndPatternTypeOrderByDetectedAtDesc(
        String userId, BehavioralPatternData.PatternType patternType);

    /**
     * Check if user has pattern in recent time
     */
    boolean existsByUserIdAndPatternTypeAndDetectedAtAfter(
        String userId, BehavioralPatternData.PatternType patternType, Instant since);

    /**
     * Get pattern distribution for user in time period
     */
    @Query("SELECT p.patternType, COUNT(p) * 100.0 / " +
           "(SELECT COUNT(pp) FROM BehavioralPattern pp WHERE pp.userId = :userId " +
           "AND pp.detectedAt BETWEEN :startTime AND :endTime) " +
           "FROM BehavioralPattern p " +
           "WHERE p.userId = :userId " +
           "AND p.detectedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY p.patternType " +
           "ORDER BY COUNT(p) DESC")
    List<Object[]> getPatternDistribution(@Param("userId") String userId,
                                        @Param("startTime") Instant startTime,
                                        @Param("endTime") Instant endTime);

    /**
     * Find patterns trending upward (increasing frequency)
     */
    @Query("SELECT p.patternType FROM BehavioralPattern p " +
           "WHERE p.userId = :userId " +
           "AND p.detectedAt >= :recentStart " +
           "GROUP BY p.patternType " +
           "HAVING COUNT(p) > " +
           "(SELECT COUNT(pp) FROM BehavioralPattern pp " +
           "WHERE pp.userId = :userId " +
           "AND pp.patternType = p.patternType " +
           "AND pp.detectedAt BETWEEN :olderStart AND :recentStart)")
    List<BehavioralPatternData.PatternType> findTrendingPatterns(
        @Param("userId") String userId,
        @Param("recentStart") Instant recentStart,
        @Param("olderStart") Instant olderStart);

    /**
     * Delete old patterns beyond retention period
     */
    void deleteByDetectedAtBefore(Instant cutoffDate);

    /**
     * Find patterns for ML training data
     */
    @Query("SELECT p FROM BehavioralPattern p " +
           "WHERE p.confidenceScore >= :minConfidence " +
           "AND p.detectedAt >= :since " +
           "ORDER BY p.detectedAt DESC")
    Page<BehavioralPattern> findTrainingData(@Param("minConfidence") Double minConfidence,
                                           @Param("since") Instant since,
                                           Pageable pageable);
}