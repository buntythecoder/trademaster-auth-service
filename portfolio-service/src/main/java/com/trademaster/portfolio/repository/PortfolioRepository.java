package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.model.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Portfolio Repository
 * 
 * High-performance JPA repository for portfolio management operations.
 * Optimized for Java 24 Virtual Threads with structured logging and Prometheus metrics.
 * 
 * Performance Features:
 * - Optimized queries with proper indexing
 * - Batch operations for bulk updates
 * - Native queries for complex calculations
 * - Structured logging for all operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    
    /**
     * Find portfolio by user ID
     * Primary lookup method with structured logging
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.status != 'CLOSED'")
    Optional<Portfolio> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find portfolio by user ID and specific status
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.status = :status")
    Optional<Portfolio> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PortfolioStatus status);
    
    /**
     * Check if portfolio exists for user with specific status
     */
    @Query("SELECT COUNT(p) > 0 FROM Portfolio p WHERE p.userId = :userId AND p.status = :status")
    boolean existsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PortfolioStatus status);
    
    /**
     * Find all portfolios for a user (including closed)
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<Portfolio> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find portfolios for user with pagination support
     * Rule #3: Functional query with pagination
     * Rule #22: Performance optimized with indexed userId
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId ORDER BY p.updatedAt DESC")
    org.springframework.data.domain.Page<Portfolio> findAllByUserIdPageable(
        @Param("userId") Long userId,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find portfolios for user by status with pagination support
     * Rule #3: Functional query with filtering and pagination
     * Rule #22: Performance optimized with composite index
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.status = :status ORDER BY p.updatedAt DESC")
    org.springframework.data.domain.Page<Portfolio> findByUserIdAndStatusPageable(
        @Param("userId") Long userId,
        @Param("status") PortfolioStatus status,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Find portfolios by status with pagination support
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = :status ORDER BY p.updatedAt DESC")
    List<Portfolio> findByStatus(@Param("status") PortfolioStatus status);
    
    /**
     * Find portfolios requiring valuation update
     * Used by scheduled tasks for batch processing
     */
    @Query("SELECT p FROM Portfolio p WHERE p.lastValuationAt IS NULL OR p.lastValuationAt < :cutoffTime")
    List<Portfolio> findPortfoliosRequiringValuation(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find portfolios with specific symbol positions
     * Used for market data updates
     */
    @Query("""
        SELECT DISTINCT p FROM Portfolio p 
        JOIN p.positions pos 
        WHERE pos.symbol = :symbol AND pos.quantity != 0
        """)
    List<Portfolio> findPortfoliosWithSymbol(@Param("symbol") String symbol);
    
    /**
     * Get portfolio summary statistics
     */
    @Query("""
        SELECT NEW map(
            COUNT(p) as totalPortfolios,
            SUM(p.totalValue) as totalValue,
            AVG(p.totalValue) as averageValue,
            SUM(p.realizedPnl) as totalRealizedPnl,
            SUM(p.unrealizedPnl) as totalUnrealizedPnl
        )
        FROM Portfolio p 
        WHERE p.status = 'ACTIVE'
        """)
    List<Object> getPortfolioSummaryStatistics();
    
    /**
     * Update portfolio valuation - optimized for high frequency updates
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Portfolio p 
        SET p.totalValue = :totalValue, 
            p.unrealizedPnl = :unrealizedPnl, 
            p.lastValuationAt = :timestamp
        WHERE p.portfolioId = :portfolioId
        """)
    int updatePortfolioValuation(
        @Param("portfolioId") Long portfolioId,
        @Param("totalValue") BigDecimal totalValue,
        @Param("unrealizedPnl") BigDecimal unrealizedPnl,
        @Param("timestamp") Instant timestamp
    );
    
    /**
     * Update cash balance - atomic operation
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Portfolio p 
        SET p.cashBalance = p.cashBalance + :amount 
        WHERE p.portfolioId = :portfolioId
        """)
    int updateCashBalance(
        @Param("portfolioId") Long portfolioId,
        @Param("amount") BigDecimal amount
    );
    
    /**
     * Add realized P&L - atomic operation
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Portfolio p 
        SET p.realizedPnl = p.realizedPnl + :pnl,
            p.lastPnlCalculationAt = :timestamp
        WHERE p.portfolioId = :portfolioId
        """)
    int addRealizedPnl(
        @Param("portfolioId") Long portfolioId,
        @Param("pnl") BigDecimal pnl,
        @Param("timestamp") Instant timestamp
    );
    
    /**
     * Update day trades count - for PDT compliance
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Portfolio p 
        SET p.dayTradesCount = p.dayTradesCount + 1 
        WHERE p.portfolioId = :portfolioId
        """)
    int incrementDayTradesCount(@Param("portfolioId") Long portfolioId);
    
    /**
     * Reset day trades count for active portfolios - scheduled daily
     * Rule #3: Functional query with proper filtering
     * Rule #16: Configurable reset based on status
     *
     * @return Number of portfolios updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Portfolio p SET p.dayTradesCount = 0 WHERE p.status = 'ACTIVE'")
    int resetDayTradesCount();

    /**
     * Reset all day trades count - administrative operation
     * Rule #3: Functional query without conditionals
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Portfolio p SET p.dayTradesCount = 0")
    int resetAllDayTradesCount();

    /**
     * Find portfolios exceeding day trade count threshold
     * Rule #3: Functional query with stream-friendly result
     * Rule #13: Optimized for Stream API processing
     *
     * @param count Minimum day trade count threshold
     * @return List of portfolios exceeding the threshold
     */
    @Query("SELECT p FROM Portfolio p WHERE p.dayTradesCount > :count AND p.status = 'ACTIVE' ORDER BY p.dayTradesCount DESC")
    List<Portfolio> findByDayTradesCountGreaterThan(@Param("count") int count);
    
    /**
     * Find portfolios approaching risk limits
     * Used for risk monitoring and alerts
     */
    @Query("""
        SELECT p FROM Portfolio p 
        WHERE (p.unrealizedPnl / NULLIF(p.totalCost, 0) * 100) < :lossThreshold
        OR p.dayTradesCount >= :dayTradeLimit
        """)
    List<Portfolio> findPortfoliosApproachingRiskLimits(
        @Param("lossThreshold") BigDecimal lossThreshold,
        @Param("dayTradeLimit") Integer dayTradeLimit
    );
    
    /**
     * Get portfolios for performance analytics
     */
    @Query("""
        SELECT p FROM Portfolio p 
        WHERE p.status = 'ACTIVE' 
        AND p.totalCost > 0 
        AND p.createdAt <= :endDate
        ORDER BY p.totalValue DESC
        """)
    List<Portfolio> findPortfoliosForAnalytics(@Param("endDate") Instant endDate);
    
    /**
     * Get portfolio performance metrics in native query for speed
     */
    @Query(value = """
        SELECT 
            portfolio_id,
            total_value,
            total_cost,
            realized_pnl,
            unrealized_pnl,
            (realized_pnl + unrealized_pnl) as total_pnl,
            CASE 
                WHEN total_cost > 0 THEN ((realized_pnl + unrealized_pnl) / total_cost * 100)
                ELSE 0 
            END as return_percent,
            day_pnl,
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at)) / 86400 as days_since_creation
        FROM portfolios 
        WHERE portfolio_id = :portfolioId
        """, nativeQuery = true)
    List<Object[]> getPortfolioPerformanceMetrics(@Param("portfolioId") Long portfolioId);
    
    /**
     * Bulk update portfolio statuses - administrative operation
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Portfolio p SET p.status = :newStatus WHERE p.status = :currentStatus")
    int bulkUpdateStatus(
        @Param("currentStatus") PortfolioStatus currentStatus,
        @Param("newStatus") PortfolioStatus newStatus
    );
    
    /**
     * Find portfolios by total value range
     * Used for portfolio categorization and analytics
     */
    @Query("""
        SELECT p FROM Portfolio p 
        WHERE p.totalValue BETWEEN :minValue AND :maxValue 
        AND p.status = 'ACTIVE'
        ORDER BY p.totalValue DESC
        """)
    List<Portfolio> findByTotalValueRange(
        @Param("minValue") BigDecimal minValue,
        @Param("maxValue") BigDecimal maxValue
    );
    
    /**
     * Get top performing portfolios
     */
    @Query("""
        SELECT p FROM Portfolio p 
        WHERE p.status = 'ACTIVE' 
        AND p.totalCost > 0
        ORDER BY ((p.realizedPnl + p.unrealizedPnl) / p.totalCost) DESC
        """)
    List<Portfolio> findTopPerformingPortfolios();
    
    /**
     * Get portfolios with negative performance
     */
    @Query("""
        SELECT p FROM Portfolio p 
        WHERE p.status = 'ACTIVE' 
        AND (p.realizedPnl + p.unrealizedPnl) < 0
        ORDER BY ((p.realizedPnl + p.unrealizedPnl) / p.totalCost) ASC
        """)
    List<Portfolio> findUnderperformingPortfolios();
    
    /**
     * Count active portfolios - health check metric
     */
    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.status = 'ACTIVE'")
    Long countActivePortfolios();
    
    /**
     * Calculate total AUM (Assets Under Management) - Rule #3 Functional
     * Used by PortfolioServiceImpl for metrics tracking
     *
     * @return Total assets under management across all active portfolios
     */
    @Query("SELECT COALESCE(SUM(p.cashBalance + p.totalValue), 0) FROM Portfolio p WHERE p.status = 'ACTIVE'")
    BigDecimal calculateTotalAUM();

    /**
     * Get total AUM (Assets Under Management) - Alternative method
     */
    @Query("SELECT COALESCE(SUM(p.totalValue), 0) FROM Portfolio p WHERE p.status = 'ACTIVE'")
    BigDecimal getTotalAssetsUnderManagement();

    /**
     * Calculate AUM for specific user - Rule #3 Functional
     * Rule #13: Stream-friendly single result
     *
     * @param userId User identifier
     * @return Total AUM for user's active portfolios
     */
    @Query("SELECT COALESCE(SUM(p.cashBalance + p.totalValue), 0) FROM Portfolio p WHERE p.userId = :userId AND p.status = 'ACTIVE'")
    BigDecimal calculateAUMByUserId(@Param("userId") Long userId);

    /**
     * Find top portfolios by AUM - Rule #3 Functional, Rule #22 Performance
     * Optimized query with LIMIT for performance
     *
     * @param pageable Pagination parameters with limit
     * @return Top N portfolios sorted by total value
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE' ORDER BY (p.cashBalance + p.totalValue) DESC")
    List<Portfolio> findTopPortfoliosByAUM(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find portfolios by currency
     */
    @Query("SELECT p FROM Portfolio p WHERE p.currency = :currency AND p.status = 'ACTIVE'")
    List<Portfolio> findByCurrency(@Param("currency") String currency);
    
    /**
     * Get portfolio creation statistics
     */
    @Query(value = """
        SELECT
            DATE(created_at) as creation_date,
            COUNT(*) as portfolios_created,
            AVG(total_value) as avg_initial_value
        FROM portfolios
        WHERE created_at >= :fromDate
        GROUP BY DATE(created_at)
        ORDER BY creation_date DESC
        """, nativeQuery = true)
    List<Object[]> getPortfolioCreationStatistics(@Param("fromDate") Instant fromDate);

    // ==================== TASK 1.3: PERFORMANCE QUERY METHODS ====================
    // Rule #3: Functional Programming - No if-else, pure queries
    // Rule #13: Stream API Mastery - Optimized for stream processing
    // Rule #22: Performance Standards - Indexed queries <50ms

    /**
     * Find portfolios with return greater than threshold
     * Rule #3: Functional query for high-performance filtering
     * Rule #22: Optimized with ORDER BY for top performers
     *
     * @param minReturn Minimum total return percentage threshold
     * @return Portfolios exceeding return threshold, sorted by performance
     */
    @Query("""
        SELECT p FROM Portfolio p
        WHERE ((p.realizedPnl + p.unrealizedPnl) / NULLIF(p.totalCost, 0) * 100) > :minReturn
        AND p.status = 'ACTIVE'
        AND p.totalCost > 0
        ORDER BY ((p.realizedPnl + p.unrealizedPnl) / p.totalCost) DESC
        """)
    List<Portfolio> findByReturnGreaterThan(@Param("minReturn") BigDecimal minReturn);

    /**
     * Calculate average return across all active portfolios
     * Rule #3: Functional aggregation with null safety
     * Rule #9: Returns immutable BigDecimal result
     *
     * @return Average portfolio return percentage
     */
    @Query("""
        SELECT COALESCE(AVG((p.realizedPnl + p.unrealizedPnl) / NULLIF(p.totalCost, 0) * 100), 0)
        FROM Portfolio p
        WHERE p.status = 'ACTIVE' AND p.totalCost > 0
        """)
    BigDecimal calculateAverageReturn();

    /**
     * Find portfolios with negative returns (losses)
     * Rule #3: Functional query with natural filtering
     * Rule #13: Stream-optimized for further processing
     *
     * @return Portfolios with losses, sorted by severity
     */
    @Query("""
        SELECT p FROM Portfolio p
        WHERE (p.realizedPnl + p.unrealizedPnl) < 0
        AND p.status = 'ACTIVE'
        ORDER BY (p.realizedPnl + p.unrealizedPnl) ASC
        """)
    List<Portfolio> findPortfoliosWithLoss();

    /**
     * Find portfolios by total return range
     * Rule #3: Functional range query
     * Rule #22: Performance optimized with calculated field
     *
     * @param minReturn Minimum return percentage
     * @param maxReturn Maximum return percentage
     * @return Portfolios within return range
     */
    @Query("""
        SELECT p FROM Portfolio p
        WHERE ((p.realizedPnl + p.unrealizedPnl) / NULLIF(p.totalCost, 0) * 100) BETWEEN :minReturn AND :maxReturn
        AND p.status = 'ACTIVE'
        AND p.totalCost > 0
        ORDER BY ((p.realizedPnl + p.unrealizedPnl) / p.totalCost) DESC
        """)
    List<Portfolio> findByReturnRange(
        @Param("minReturn") BigDecimal minReturn,
        @Param("maxReturn") BigDecimal maxReturn
    );
}