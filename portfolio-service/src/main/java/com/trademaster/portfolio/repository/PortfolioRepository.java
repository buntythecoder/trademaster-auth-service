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
    @Modifying
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
    @Modifying
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
    @Modifying
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
    @Modifying
    @Query("""
        UPDATE Portfolio p 
        SET p.dayTradesCount = p.dayTradesCount + 1 
        WHERE p.portfolioId = :portfolioId
        """)
    int incrementDayTradesCount(@Param("portfolioId") Long portfolioId);
    
    /**
     * Reset day trades count - scheduled daily
     */
    @Modifying
    @Query("UPDATE Portfolio p SET p.dayTradesCount = 0")
    int resetAllDayTradesCount();
    
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
    @Modifying
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
     * Get total AUM (Assets Under Management)
     */
    @Query("SELECT COALESCE(SUM(p.totalValue), 0) FROM Portfolio p WHERE p.status = 'ACTIVE'")
    BigDecimal getTotalAssetsUnderManagement();
    
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
}