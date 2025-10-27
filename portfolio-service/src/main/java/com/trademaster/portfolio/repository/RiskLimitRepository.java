package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.RiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Risk Limit Repository
 *
 * High-performance JPA repository for portfolio risk limit management.
 * Optimized for Java 24 Virtual Threads with comprehensive risk monitoring.
 *
 * Performance Features:
 * - Optimized queries with strategic indexing
 * - Efficient risk limit lookups for real-time checks
 * - Historical risk limit tracking
 * - Structured logging for all operations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {

    // ==================== TASK 1.6: RISK LIMIT QUERY METHODS ====================
    // Rule #3: Functional Programming - No if-else, pure queries
    // Rule #13: Stream API Mastery - Optimized for stream processing
    // Rule #22: Performance Standards - Indexed queries <50ms

    /**
     * Find active risk limit for portfolio
     * Rule #3: Functional query with natural ordering
     * Rule #22: Performance optimized with indexed portfolio_id
     *
     * @param portfolioId Portfolio identifier
     * @return Current active risk limit configuration
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.portfolioId = :portfolioId ORDER BY r.effectiveDate DESC")
    Optional<RiskLimit> findActiveRiskLimitByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Convenience alias for findActiveRiskLimitByPortfolioId
     * Rule #18: Provide alternative method names for common usage patterns
     *
     * @param portfolioId Portfolio identifier
     * @return Current active risk limit configuration
     */
    default Optional<RiskLimit> findByPortfolioId(Long portfolioId) {
        return findActiveRiskLimitByPortfolioId(portfolioId);
    }

    /**
     * Find risk limit effective at specific date
     * Rule #3: Functional temporal query with pattern matching
     * Rule #22: Performance optimized with composite index
     *
     * @param portfolioId Portfolio identifier
     * @param effectiveDate Date to query risk limits
     * @return Risk limit effective at the specified date
     */
    @Query("""
        SELECT r FROM RiskLimit r
        WHERE r.portfolioId = :portfolioId
        AND r.effectiveDate <= :effectiveDate
        ORDER BY r.effectiveDate DESC
        """)
    Optional<RiskLimit> findRiskLimitAtDate(
        @Param("portfolioId") Long portfolioId,
        @Param("effectiveDate") Instant effectiveDate
    );

    /**
     * Find all risk limits for portfolio ordered by effective date
     * Rule #3: Functional query for historical tracking
     * Rule #13: Stream-friendly for analysis
     *
     * @param portfolioId Portfolio identifier
     * @return List of all risk limits ordered by effective date
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.portfolioId = :portfolioId ORDER BY r.effectiveDate DESC")
    List<RiskLimit> findRiskLimitHistory(@Param("portfolioId") Long portfolioId);

    /**
     * Find risk limits by framework type
     * Rule #3: Functional type filtering
     * Rule #13: Stream-optimized for framework analysis
     *
     * @param riskFramework Risk framework type (STANDARD, AGGRESSIVE, CONSERVATIVE, CUSTOM)
     * @return List of portfolios using the specified risk framework
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.riskFramework = :riskFramework AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)")
    List<RiskLimit> findByRiskFramework(@Param("riskFramework") String riskFramework);

    /**
     * Find portfolios with auto-liquidation enabled
     * Rule #3: Functional boolean filtering
     * Rule #22: Indexed query for risk monitoring
     *
     * @return List of risk limits with auto-liquidation enabled
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.autoLiquidationEnabled = true AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)")
    List<RiskLimit> findWithAutoLiquidationEnabled();

    /**
     * Find portfolios exceeding specific leverage threshold
     * Rule #3: Functional threshold query
     * Rule #13: Stream-optimized for risk alerts
     *
     * @param leverageThreshold Maximum leverage threshold to check
     * @return List of portfolios with leverage limits below threshold
     */
    @Query("""
        SELECT r FROM RiskLimit r
        WHERE r.maxLeverageRatio < :leverageThreshold
        AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)
        ORDER BY r.maxLeverageRatio ASC
        """)
    List<RiskLimit> findByMaxLeverageRatioLessThan(@Param("leverageThreshold") java.math.BigDecimal leverageThreshold);

    /**
     * Find risk limits modified by specific user
     * Rule #3: Functional user filtering
     * Rule #13: Stream-friendly for audit trail
     *
     * @param userId User identifier
     * @return List of risk limits modified by the user
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.modifiedBy = :userId ORDER BY r.lastModified DESC")
    List<RiskLimit> findByModifiedBy(@Param("userId") Long userId);

    /**
     * Count active risk limit configurations
     * Rule #3: Functional count for metrics
     * Rule #22: Fast counting for monitoring
     *
     * @return Total number of active risk limit configurations
     */
    @Query("SELECT COUNT(DISTINCT r.portfolioId) FROM RiskLimit r")
    Long countActiveRiskLimits();

    /**
     * Find risk limits requiring review (older than threshold)
     * Rule #3: Functional temporal filtering
     * Rule #13: Stream-optimized for compliance reviews
     *
     * @param reviewThreshold Date threshold for review requirement
     * @return List of risk limits requiring review
     */
    @Query("""
        SELECT r FROM RiskLimit r
        WHERE r.lastModified < :reviewThreshold
        AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)
        ORDER BY r.lastModified ASC
        """)
    List<RiskLimit> findRiskLimitsRequiringReview(@Param("reviewThreshold") Instant reviewThreshold);

    /**
     * Find conservative risk frameworks
     * Rule #3: Functional framework filtering
     * Rule #13: Stream-friendly for analysis
     *
     * @return List of portfolios using conservative risk frameworks
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.riskFramework = 'CONSERVATIVE' AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)")
    List<RiskLimit> findConservativeRiskLimits();

    /**
     * Find aggressive risk frameworks
     * Rule #3: Functional framework filtering
     * Rule #13: Stream-optimized for risk monitoring
     *
     * @return List of portfolios using aggressive risk frameworks
     */
    @Query("SELECT r FROM RiskLimit r WHERE r.riskFramework = 'AGGRESSIVE' AND r.effectiveDate = (SELECT MAX(r2.effectiveDate) FROM RiskLimit r2 WHERE r2.portfolioId = r.portfolioId)")
    List<RiskLimit> findAggressiveRiskLimits();

    /**
     * Delete old risk limit configurations
     * Rule #3: Functional cleanup query
     * Rule #22: Efficient deletion with temporal filter
     *
     * @param cutoffDate Date before which to delete configurations
     * @return Number of deleted records
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM RiskLimit r WHERE r.effectiveDate < :cutoffDate AND r.portfolioId IN (SELECT r2.portfolioId FROM RiskLimit r2 GROUP BY r2.portfolioId HAVING COUNT(r2) > 1)")
    int deleteOldRiskLimits(@Param("cutoffDate") Instant cutoffDate);
}
