package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.PerformanceMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Performance Metrics Repository
 *
 * High-performance JPA repository for time-series performance analytics.
 * Optimized for Java 24 Virtual Threads with comprehensive historical tracking.
 *
 * Performance Features:
 * - Time-series optimized queries with strategic indexing
 * - Period-based aggregations (daily, weekly, monthly)
 * - Efficient historical data retrieval
 * - Benchmark performance tracking
 * - Structured logging for all operations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetricSnapshot, Long> {

    // ==================== TASK 1.7: TIME-SERIES QUERY METHODS ====================
    // Rule #3: Functional Programming - No if-else, pure queries
    // Rule #13: Stream API Mastery - Optimized for stream processing
    // Rule #22: Performance Standards - Indexed queries <50ms

    /**
     * Find latest performance snapshot for portfolio
     * Rule #3: Functional query with natural ordering
     * Rule #22: Performance optimized with indexed snapshot_date
     *
     * @param portfolioId Portfolio identifier
     * @return Latest performance snapshot
     */
    @Query("SELECT p FROM PerformanceMetricSnapshot p WHERE p.portfolioId = :portfolioId ORDER BY p.snapshotDate DESC")
    Optional<PerformanceMetricSnapshot> findLatestByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find performance snapshots within date range
     * Rule #3: Functional range query with temporal filtering
     * Rule #22: Performance optimized with composite index (portfolio_id, snapshot_date)
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of performance snapshots within date range
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.snapshotDate BETWEEN :startDate AND :endDate
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findByPortfolioIdAndDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find daily performance snapshots for portfolio
     * Rule #3: Functional period type filtering
     * Rule #13: Stream-friendly for daily analytics
     *
     * @param portfolioId Portfolio identifier
     * @return List of daily performance snapshots
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'DAILY'
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findDailySnapshotsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find weekly performance snapshots for portfolio
     * Rule #3: Functional period filtering
     * Rule #13: Stream-optimized for weekly trends
     *
     * @param portfolioId Portfolio identifier
     * @return List of weekly performance snapshots
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'WEEKLY'
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findWeeklySnapshotsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find monthly performance snapshots for portfolio
     * Rule #3: Functional period filtering
     * Rule #13: Stream-optimized for monthly analysis
     *
     * @param portfolioId Portfolio identifier
     * @return List of monthly performance snapshots
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'MONTHLY'
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findMonthlySnapshotsByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find performance snapshots by period type and date range
     * Rule #3: Functional multi-criteria filtering
     * Rule #22: Performance optimized with unique index
     *
     * @param portfolioId Portfolio identifier
     * @param periodType Period type (DAILY, WEEKLY, MONTHLY, etc.)
     * @param startDate Start date
     * @param endDate End date
     * @return List of performance snapshots matching criteria
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = :periodType
        AND p.snapshotDate BETWEEN :startDate AND :endDate
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findByPortfolioIdAndPeriodTypeAndDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("periodType") String periodType,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get performance trend statistics for time period
     * Rule #3: Functional aggregation for trend analysis
     * Rule #22: Native query for complex calculations
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date for analysis
     * @return Performance trend statistics
     */
    @Query(value = """
        SELECT
            MIN(total_return_percent) as min_return,
            MAX(total_return_percent) as max_return,
            AVG(total_return_percent) as avg_return,
            STDDEV(total_return_percent) as return_volatility,
            MIN(portfolio_value) as min_portfolio_value,
            MAX(portfolio_value) as max_portfolio_value,
            AVG(portfolio_value) as avg_portfolio_value,
            COUNT(*) as snapshot_count
        FROM performance_metrics
        WHERE portfolio_id = :portfolioId
        AND snapshot_date >= :startDate
        """, nativeQuery = true)
    List<Object[]> getPerformanceTrendStatistics(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate
    );

    /**
     * Find top performing periods by return
     * Rule #3: Functional query for performance ranking
     * Rule #13: Stream-optimized with Pageable
     *
     * @param portfolioId Portfolio identifier
     * @param pageable Pagination parameters
     * @return Top N performing periods
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'DAILY'
        ORDER BY p.totalReturnPercent DESC
        """)
    List<PerformanceMetricSnapshot> findTopPerformingPeriods(
        @Param("portfolioId") Long portfolioId,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find worst performing periods by return
     * Rule #3: Functional query for loss identification
     * Rule #13: Stream-optimized for risk analysis
     *
     * @param portfolioId Portfolio identifier
     * @param pageable Pagination parameters
     * @return Worst N performing periods
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'DAILY'
        ORDER BY p.totalReturnPercent ASC
        """)
    List<PerformanceMetricSnapshot> findWorstPerformingPeriods(
        @Param("portfolioId") Long portfolioId,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find snapshots with Sharpe ratio above threshold
     * Rule #3: Functional threshold filtering
     * Rule #13: Stream-friendly for quality analysis
     *
     * @param portfolioId Portfolio identifier
     * @param sharpeThreshold Minimum Sharpe ratio threshold
     * @return List of high-quality performance snapshots
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.sharpeRatio IS NOT NULL
        AND p.sharpeRatio > :sharpeThreshold
        ORDER BY p.sharpeRatio DESC
        """)
    List<PerformanceMetricSnapshot> findByPortfolioIdAndSharpeRatioGreaterThan(
        @Param("portfolioId") Long portfolioId,
        @Param("sharpeThreshold") BigDecimal sharpeThreshold
    );

    /**
     * Calculate average monthly return over period
     * Rule #3: Functional aggregation for return calculation
     * Rule #22: Performance optimized for reporting
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date for calculation
     * @return Average monthly return percentage
     */
    @Query("""
        SELECT COALESCE(AVG(p.totalReturnPercent), 0)
        FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.periodType = 'MONTHLY'
        AND p.snapshotDate >= :startDate
        """)
    BigDecimal calculateAverageMonthlyReturn(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate
    );

    /**
     * Find snapshots exceeding maximum drawdown threshold
     * Rule #3: Functional risk threshold query
     * Rule #13: Stream-optimized for risk monitoring
     *
     * @param portfolioId Portfolio identifier
     * @param drawdownThreshold Maximum drawdown percentage threshold
     * @return List of snapshots with excessive drawdown
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.maxDrawdownPercent > :drawdownThreshold
        ORDER BY p.maxDrawdownPercent DESC
        """)
    List<PerformanceMetricSnapshot> findByPortfolioIdAndDrawdownExceedsThreshold(
        @Param("portfolioId") Long portfolioId,
        @Param("drawdownThreshold") BigDecimal drawdownThreshold
    );

    /**
     * Get benchmark comparison data for time period
     * Rule #3: Functional benchmark filtering
     * Rule #13: Stream-friendly for comparison analysis
     *
     * @param portfolioId Portfolio identifier
     * @param benchmarkSymbol Benchmark symbol (e.g., NIFTY_50)
     * @param startDate Start date for comparison
     * @return List of snapshots with benchmark data
     */
    @Query("""
        SELECT p FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.benchmarkSymbol = :benchmarkSymbol
        AND p.snapshotDate >= :startDate
        ORDER BY p.snapshotDate DESC
        """)
    List<PerformanceMetricSnapshot> findBenchmarkComparisonData(
        @Param("portfolioId") Long portfolioId,
        @Param("benchmarkSymbol") String benchmarkSymbol,
        @Param("startDate") Instant startDate
    );

    /**
     * Count outperforming periods vs. benchmark
     * Rule #3: Functional count for outperformance metric
     * Rule #22: Fast counting for scorecarding
     *
     * @param portfolioId Portfolio identifier
     * @param benchmarkSymbol Benchmark symbol
     * @param startDate Start date for analysis
     * @return Number of periods outperforming benchmark
     */
    @Query("""
        SELECT COUNT(p)
        FROM PerformanceMetricSnapshot p
        WHERE p.portfolioId = :portfolioId
        AND p.benchmarkSymbol = :benchmarkSymbol
        AND p.relativeReturn > 0
        AND p.snapshotDate >= :startDate
        """)
    Long countOutperformingPeriods(
        @Param("portfolioId") Long portfolioId,
        @Param("benchmarkSymbol") String benchmarkSymbol,
        @Param("startDate") Instant startDate
    );

    /**
     * Find latest snapshots for all portfolios
     * Rule #3: Functional aggregation with window function pattern
     * Rule #22: Native query for performance
     *
     * @return Latest snapshot for each portfolio
     */
    @Query(value = """
        SELECT DISTINCT ON (portfolio_id) *
        FROM performance_metrics
        ORDER BY portfolio_id, snapshot_date DESC
        """, nativeQuery = true)
    List<PerformanceMetricSnapshot> findLatestSnapshotsForAllPortfolios();

    /**
     * Delete old snapshots before cutoff date
     * Rule #3: Functional cleanup query
     * Rule #22: Efficient deletion with temporal filter
     *
     * @param cutoffDate Date before which to delete snapshots
     * @return Number of deleted records
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PerformanceMetricSnapshot p WHERE p.snapshotDate < :cutoffDate AND p.periodType = 'INTRADAY'")
    int deleteIntradaySnapshotsOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count total snapshots for portfolio
     * Rule #3: Functional count for metrics
     * Rule #22: Indexed for fast counting
     *
     * @param portfolioId Portfolio identifier
     * @return Total number of snapshots
     */
    @Query("SELECT COUNT(p) FROM PerformanceMetricSnapshot p WHERE p.portfolioId = :portfolioId")
    Long countSnapshotsByPortfolioId(@Param("portfolioId") Long portfolioId);
}
