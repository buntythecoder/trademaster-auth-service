package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.PortfolioTransaction;
import com.trademaster.portfolio.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Transaction Repository
 *
 * High-performance JPA repository for portfolio transaction management.
 * Optimized for Java 24 Virtual Threads with comprehensive audit trail support.
 *
 * Performance Features:
 * - Optimized queries with strategic indexing
 * - Time-series analysis for P&L calculations
 * - Efficient transaction history retrieval
 * - Structured logging for all operations
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface TransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    // ==================== TASK 1.5: TRANSACTION QUERY METHODS ====================
    // Rule #3: Functional Programming - No if-else, pure queries
    // Rule #13: Stream API Mastery - Optimized for stream processing
    // Rule #22: Performance Standards - Indexed queries <50ms

    /**
     * Find all transactions for a portfolio ordered by execution time
     * Rule #3: Functional query with natural ordering
     * Rule #22: Performance optimized with indexed portfolio_id
     *
     * @param portfolioId Portfolio identifier
     * @return List of transactions ordered by execution time (descending)
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.portfolioId = :portfolioId ORDER BY t.executedAt DESC")
    List<PortfolioTransaction> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions for portfolio within date range
     * Rule #3: Functional range query with timestamp filtering
     * Rule #22: Performance optimized with composite index (portfolio_id, executed_at)
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions within date range
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.executedAt BETWEEN :startDate AND :endDate
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find transactions by portfolio and transaction type
     * Rule #3: Functional type filtering with pattern matching support
     * Rule #13: Stream-friendly for further processing
     *
     * @param portfolioId Portfolio identifier
     * @param transactionType Type of transaction to filter by
     * @return List of transactions matching the type
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.transactionType = :transactionType
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndTransactionType(
        @Param("portfolioId") Long portfolioId,
        @Param("transactionType") TransactionType transactionType
    );

    /**
     * Calculate total commissions for a portfolio
     * Rule #3: Functional aggregation with null safety
     * Rule #22: Performance optimized for metrics tracking
     *
     * @param portfolioId Portfolio identifier
     * @return Total commissions paid (commission + tax + other fees)
     */
    @Query("""
        SELECT COALESCE(SUM(t.commission + t.tax + t.otherFees), 0)
        FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        """)
    BigDecimal calculateTotalCommissions(@Param("portfolioId") Long portfolioId);

    /**
     * Find all trade executions for a symbol across portfolio
     * Rule #3: Functional query filtering trade execution types
     * Rule #13: Stream-optimized for position cost basis calculations
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Security symbol
     * @return List of trade execution transactions for the symbol
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.symbol = :symbol
        AND t.transactionType IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER')
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findTradeExecutionsBySymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );

    /**
     * Calculate total realized P&L for portfolio
     * Rule #3: Functional aggregation with null safety
     * Rule #22: Performance optimized for P&L reporting
     *
     * @param portfolioId Portfolio identifier
     * @return Total realized P&L from all transactions
     */
    @Query("""
        SELECT COALESCE(SUM(t.realizedPnl), 0)
        FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.realizedPnl IS NOT NULL
        """)
    BigDecimal calculateTotalRealizedPnl(@Param("portfolioId") Long portfolioId);

    /**
     * Find transactions by order ID
     * Rule #3: Functional lookup for order tracking
     * Rule #22: Indexed query for trade reconciliation
     *
     * @param orderId Order identifier from trading service
     * @return List of transactions associated with the order
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.orderId = :orderId ORDER BY t.executedAt DESC")
    List<PortfolioTransaction> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Get transaction statistics for portfolio
     * Rule #3: Functional aggregation query
     * Rule #22: Native query for complex calculations
     *
     * @param portfolioId Portfolio identifier
     * @return Transaction statistics (count, total volume, fees)
     */
    @Query(value = """
        SELECT
            COUNT(*) as total_transactions,
            COUNT(DISTINCT symbol) as unique_symbols,
            SUM(CASE WHEN transaction_type IN ('BUY', 'BUY_TO_COVER') THEN quantity ELSE 0 END) as total_bought,
            SUM(CASE WHEN transaction_type IN ('SELL', 'SHORT_SELL') THEN quantity ELSE 0 END) as total_sold,
            SUM(commission + tax + other_fees) as total_costs,
            SUM(CASE WHEN realized_pnl IS NOT NULL THEN realized_pnl ELSE 0 END) as total_realized_pnl
        FROM portfolio_transactions
        WHERE portfolio_id = :portfolioId
        """, nativeQuery = true)
    List<Object[]> getTransactionStatistics(@Param("portfolioId") Long portfolioId);

    /**
     * Find recent transactions with limit
     * Rule #3: Functional query with pagination support
     * Rule #13: Stream-friendly for activity feeds
     *
     * @param portfolioId Portfolio identifier
     * @param pageable Pagination parameters
     * @return Recent transactions limited by page size
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.portfolioId = :portfolioId ORDER BY t.executedAt DESC")
    List<PortfolioTransaction> findRecentTransactions(
        @Param("portfolioId") Long portfolioId,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Get daily transaction summary
     * Rule #3: Functional time-series aggregation
     * Rule #22: Native query for performance
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date for summary
     * @return Daily transaction summary statistics
     */
    @Query(value = """
        SELECT
            DATE(executed_at) as transaction_date,
            COUNT(*) as transaction_count,
            SUM(amount) as total_volume,
            SUM(commission + tax + other_fees) as total_fees,
            SUM(CASE WHEN realized_pnl IS NOT NULL THEN realized_pnl ELSE 0 END) as daily_pnl
        FROM portfolio_transactions
        WHERE portfolio_id = :portfolioId
        AND executed_at >= :startDate
        GROUP BY DATE(executed_at)
        ORDER BY transaction_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyTransactionSummary(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate
    );

    /**
     * Find dividend transactions for portfolio
     * Rule #3: Functional type filtering
     * Rule #13: Stream-optimized for income tracking
     *
     * @param portfolioId Portfolio identifier
     * @return List of dividend transactions
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.transactionType = 'DIVIDEND'
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findDividendTransactions(@Param("portfolioId") Long portfolioId);

    /**
     * Calculate total dividends received
     * Rule #3: Functional aggregation for income calculation
     * Rule #22: Performance optimized for reporting
     *
     * @param portfolioId Portfolio identifier
     * @return Total dividend income received
     */
    @Query("""
        SELECT COALESCE(SUM(t.netAmount), 0)
        FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.transactionType = 'DIVIDEND'
        """)
    BigDecimal calculateTotalDividends(@Param("portfolioId") Long portfolioId);

    /**
     * Count transactions for portfolio
     * Rule #3: Functional count for metrics
     * Rule #22: Indexed for fast counting
     *
     * @param portfolioId Portfolio identifier
     * @return Total number of transactions
     */
    @Query("SELECT COUNT(t) FROM PortfolioTransaction t WHERE t.portfolioId = :portfolioId")
    Long countTransactionsByPortfolioId(@Param("portfolioId") Long portfolioId);

    // ==================== ADDITIONAL TASK 1.5 METHODS ====================

    /**
     * Calculate total buy amount for date range
     * Rule #3: Functional aggregation with type filtering
     * Rule #22: Performance optimized with composite index
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total amount spent on buy transactions
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.transactionType IN ('BUY', 'BUY_TO_COVER')
        AND t.executedAt BETWEEN :startDate AND :endDate
        """)
    BigDecimal calculateTotalBuyAmount(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Calculate total sell amount for date range
     * Rule #3: Functional aggregation with type filtering
     * Rule #22: Performance optimized with composite index
     *
     * @param portfolioId Portfolio identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total amount received from sell transactions
     */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM PortfolioTransaction t
        WHERE t.portfolioId = :portfolioId
        AND t.transactionType IN ('SELL', 'SHORT_SELL')
        AND t.executedAt BETWEEN :startDate AND :endDate
        """)
    BigDecimal calculateTotalSellAmount(
        @Param("portfolioId") Long portfolioId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find transactions by symbol and transaction type
     * Rule #3: Functional filtering with multiple criteria
     * Rule #13: Stream-optimized for analysis workflows
     *
     * @param symbol Security symbol
     * @param transactionType Type of transaction
     * @return List of matching transactions ordered by execution time
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t
        WHERE t.symbol = :symbol
        AND t.transactionType = :transactionType
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findBySymbolAndType(
        @Param("symbol") String symbol,
        @Param("transactionType") TransactionType transactionType
    );
}
