package com.trademaster.behavioralai.repository;

import com.trademaster.behavioralai.domain.entity.CoachingInterventionEntity;
import com.trademaster.behavioralai.dto.BehavioralPatternData;
import com.trademaster.behavioralai.dto.CoachingIntervention;
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
 * Coaching Intervention Repository
 * 
 * Repository interface for coaching intervention data access with analytics queries.
 */
@Repository
public interface CoachingInterventionRepository extends JpaRepository<CoachingInterventionEntity, Long> {

    /**
     * Find interventions by user ID ordered by trigger time
     */
    List<CoachingInterventionEntity> findByUserIdOrderByTriggeredAtDesc(String userId);

    /**
     * Find interventions by user ID with pagination
     */
    Page<CoachingInterventionEntity> findByUserId(String userId, Pageable pageable);

    /**
     * Find intervention by intervention ID
     */
    Optional<CoachingInterventionEntity> findByInterventionId(String interventionId);

    /**
     * Find interventions by type and user
     */
    List<CoachingInterventionEntity> findByUserIdAndInterventionTypeOrderByTriggeredAtDesc(
        String userId, CoachingIntervention.InterventionType interventionType);

    /**
     * Find urgent interventions without response
     */
    @Query("SELECT i FROM CoachingInterventionEntity i " +
           "WHERE i.priority = 'HIGH' " +
           "AND i.userResponse IS NULL " +
           "AND i.triggeredAt >= :since " +
           "ORDER BY i.triggeredAt ASC")
    List<CoachingInterventionEntity> findUrgentInterventionsWithoutResponse(@Param("since") Instant since);

    /**
     * Find interventions by trigger pattern
     */
    List<CoachingInterventionEntity> findByTriggerPatternOrderByTriggeredAtDesc(
        BehavioralPatternData.PatternType triggerPattern);

    /**
     * Find interventions for trading session
     */
    List<CoachingInterventionEntity> findByTradingSessionIdOrderByTriggeredAtDesc(String sessionId);

    /**
     * Count interventions by user in time period
     */
    @Query("SELECT COUNT(i) FROM CoachingInterventionEntity i " +
           "WHERE i.userId = :userId " +
           "AND i.triggeredAt BETWEEN :startTime AND :endTime")
    Long countInterventionsByUserInPeriod(
        @Param("userId") String userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);

    /**
     * Find effective interventions (high actual effectiveness)
     */
    List<CoachingInterventionEntity> findByActualEffectivenessGreaterThanOrderByActualEffectivenessDesc(
        Double minEffectiveness);

    /**
     * Calculate average effectiveness by intervention type
     */
    @Query("SELECT i.interventionType, AVG(i.actualEffectiveness), COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.actualEffectiveness IS NOT NULL " +
           "AND i.triggeredAt >= :since " +
           "GROUP BY i.interventionType " +
           "ORDER BY AVG(i.actualEffectiveness) DESC")
    List<Object[]> calculateAverageEffectivenessByType(@Param("since") Instant since);

    /**
     * Find interventions with user feedback
     */
    List<CoachingInterventionEntity> findByUserFeedbackIsNotNullOrderByTriggeredAtDesc();

    /**
     * Find interventions by priority level
     */
    List<CoachingInterventionEntity> findByPriorityOrderByTriggeredAtDesc(CoachingIntervention.Priority priority);

    /**
     * Get intervention response rate by user
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN i.userResponse IS NOT NULL THEN 1 END) * 100.0 / COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.userId = :userId " +
           "AND i.triggeredAt >= :since")
    Double getResponseRateByUser(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Find fastest responding users
     */
    @Query("SELECT i.userId, AVG(EXTRACT(EPOCH FROM (i.respondedAt - i.triggeredAt))) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.respondedAt IS NOT NULL " +
           "AND i.triggeredAt >= :since " +
           "GROUP BY i.userId " +
           "ORDER BY AVG(EXTRACT(EPOCH FROM (i.respondedAt - i.triggeredAt))) ASC")
    List<Object[]> findFastestRespondingUsers(@Param("since") Instant since);

    /**
     * Find interventions with low effectiveness
     */
    @Query("SELECT i FROM CoachingInterventionEntity i " +
           "WHERE i.actualEffectiveness IS NOT NULL " +
           "AND i.actualEffectiveness < :threshold " +
           "ORDER BY i.actualEffectiveness ASC")
    List<CoachingInterventionEntity> findLowEffectivenessInterventions(@Param("threshold") Double threshold);

    /**
     * Get intervention frequency by pattern type
     */
    @Query("SELECT i.triggerPattern, COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.triggeredAt >= :since " +
           "GROUP BY i.triggerPattern " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> getInterventionFrequencyByPattern(@Param("since") Instant since);

    /**
     * Find users exceeding intervention rate limit
     */
    @Query("SELECT i.userId, COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.triggeredAt >= :since " +
           "GROUP BY i.userId " +
           "HAVING COUNT(i) > :maxInterventions " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> findUsersExceedingRateLimit(
        @Param("since") Instant since,
        @Param("maxInterventions") Long maxInterventions);

    /**
     * Calculate intervention success metrics
     */
    @Query("SELECT " +
           "COUNT(i) as total, " +
           "COUNT(CASE WHEN i.userResponse = 'ACCEPTED' THEN 1 END) as accepted, " +
           "COUNT(CASE WHEN i.actionTaken = true THEN 1 END) as actionTaken, " +
           "AVG(i.actualEffectiveness) as avgEffectiveness, " +
           "AVG(CASE WHEN i.respondedAt IS NOT NULL THEN " +
           "EXTRACT(EPOCH FROM (i.respondedAt - i.triggeredAt)) END) as avgResponseTime " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.triggeredAt >= :since")
    Object[] calculateInterventionMetrics(@Param("since") Instant since);

    /**
     * Find most effective intervention messages
     */
    @Query("SELECT i.message, AVG(i.actualEffectiveness), COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.actualEffectiveness IS NOT NULL " +
           "AND i.triggeredAt >= :since " +
           "GROUP BY i.message " +
           "HAVING COUNT(i) >= :minCount " +
           "ORDER BY AVG(i.actualEffectiveness) DESC")
    List<Object[]> findMostEffectiveMessages(
        @Param("since") Instant since,
        @Param("minCount") Long minCount);

    /**
     * Find interventions needing follow-up
     */
    @Query("SELECT i FROM CoachingInterventionEntity i " +
           "WHERE i.priority = 'HIGH' " +
           "AND i.userResponse IN ('DISMISSED', 'IGNORED') " +
           "AND i.triggeredAt >= :since " +
           "ORDER BY i.triggeredAt DESC")
    List<CoachingInterventionEntity> findInterventionsNeedingFollowUp(@Param("since") Instant since);

    /**
     * Check recent intervention for same pattern
     */
    boolean existsByUserIdAndTriggerPatternAndTriggeredAtAfter(
        String userId, BehavioralPatternData.PatternType pattern, Instant since);

    /**
     * Delete old interventions beyond retention period
     */
    void deleteByTriggeredAtBefore(Instant cutoffDate);

    /**
     * Find interventions for ML training (with responses)
     */
    @Query("SELECT i FROM CoachingInterventionEntity i " +
           "WHERE i.userResponse IS NOT NULL " +
           "AND i.actualEffectiveness IS NOT NULL " +
           "AND i.triggeredAt >= :since " +
           "ORDER BY i.triggeredAt DESC")
    Page<CoachingInterventionEntity> findInterventionsForTraining(
        @Param("since") Instant since, Pageable pageable);

    /**
     * Get user intervention preferences (response patterns)
     */
    @Query("SELECT i.interventionType, " +
           "COUNT(CASE WHEN i.userResponse = 'ACCEPTED' THEN 1 END) * 100.0 / COUNT(i) " +
           "FROM CoachingInterventionEntity i " +
           "WHERE i.userId = :userId " +
           "AND i.userResponse IS NOT NULL " +
           "GROUP BY i.interventionType " +
           "ORDER BY COUNT(CASE WHEN i.userResponse = 'ACCEPTED' THEN 1 END) * 100.0 / COUNT(i) DESC")
    List<Object[]> getUserInterventionPreferences(@Param("userId") String userId);
}