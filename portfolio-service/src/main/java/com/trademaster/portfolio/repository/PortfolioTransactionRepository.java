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
 * Portfolio Transaction Repository
 * 
 * High-performance repository for transaction audit trail and P&L calculations.
 * Provides comprehensive transaction history with optimized queries for reporting.
 * 
 * Performance Features:
 * - Date-range queries optimized with proper indexing
 * - Aggregate functions for P&L calculations
 * - Bulk transaction processing capabilities
 * - Structured logging for all financial operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {
    
    /**
     * Find all transactions for a portfolio
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.portfolioId = :portfolioId ORDER BY t.executedAt DESC")
    List<PortfolioTransaction> findByPortfolioId(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find transactions by portfolio and date range
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.executedAt BETWEEN :fromDate AND :toDate
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Find transactions by symbol and portfolio
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.symbol = :symbol
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndSymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Find transactions by type
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
     * Find trade execution transactions
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.transactionType IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER')
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findTradeExecutions(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find cash transactions (deposits/withdrawals)
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.transactionType IN ('DEPOSIT', 'WITHDRAWAL')
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findCashTransactions(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find dividend and interest transactions
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.transactionType IN ('DIVIDEND', 'INTEREST')
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findIncomeTransactions(@Param("portfolioId") Long portfolioId);
    
    /**
     * Calculate realized P&L for date range
     */
    @Query("""
        SELECT COALESCE(SUM(t.realizedPnl), 0) 
        FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.realizedPnl IS NOT NULL
        AND t.executedAt BETWEEN :fromDate AND :toDate
        """)
    BigDecimal calculateRealizedPnlForDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Calculate total fees and commissions
     */
    @Query("""
        SELECT COALESCE(SUM(t.commission + t.tax + t.otherFees), 0) 
        FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.executedAt BETWEEN :fromDate AND :toDate
        """)
    BigDecimal calculateTotalFeesForDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Calculate net cash flow
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN t.transactionType IN ('BUY', 'BUY_TO_COVER', 'FEE', 'WITHDRAWAL') THEN -t.netAmount
                WHEN t.transactionType IN ('SELL', 'SHORT_SELL', 'DIVIDEND', 'INTEREST', 'DEPOSIT') THEN t.netAmount
                ELSE 0
            END
        ), 0)
        FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.executedAt BETWEEN :fromDate AND :toDate
        """)
    BigDecimal calculateNetCashFlowForDateRange(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Get trading volume statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_trades,
            COUNT(CASE WHEN transaction_type IN ('BUY', 'SELL') THEN 1 END) as equity_trades,
            COUNT(CASE WHEN transaction_type IN ('SHORT_SELL', 'BUY_TO_COVER') THEN 1 END) as short_trades,
            SUM(CASE WHEN transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER') THEN amount ELSE 0 END) as total_volume,
            AVG(CASE WHEN transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER') THEN amount ELSE NULL END) as avg_trade_size
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND executed_at BETWEEN :fromDate AND :toDate
        """, nativeQuery = true)
    List<Object[]> getTradingVolumeStatistics(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Get daily trading summary
     */
    @Query(value = """
        SELECT 
            DATE(executed_at) as trade_date,
            COUNT(*) as trades_count,
            SUM(CASE WHEN transaction_type IN ('BUY', 'SHORT_SELL') THEN amount ELSE 0 END) as buy_volume,
            SUM(CASE WHEN transaction_type IN ('SELL', 'BUY_TO_COVER') THEN amount ELSE 0 END) as sell_volume,
            SUM(commission + tax + other_fees) as total_fees,
            COUNT(DISTINCT symbol) as symbols_traded
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND transaction_type IN ('BUY', 'SELL', 'SHORT_SELL', 'BUY_TO_COVER')
        AND executed_at BETWEEN :fromDate AND :toDate
        GROUP BY DATE(executed_at)
        ORDER BY trade_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyTradingSummary(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Get symbol-wise trading statistics
     */
    @Query(value = """
        SELECT 
            symbol,
            COUNT(*) as trades_count,
            SUM(CASE WHEN transaction_type IN ('BUY', 'SHORT_SELL') THEN quantity ELSE 0 END) as shares_bought,
            SUM(CASE WHEN transaction_type IN ('SELL', 'BUY_TO_COVER') THEN quantity ELSE 0 END) as shares_sold,
            SUM(CASE WHEN transaction_type IN ('BUY', 'SHORT_SELL') THEN amount ELSE 0 END) as buy_value,
            SUM(CASE WHEN transaction_type IN ('SELL', 'BUY_TO_COVER') THEN amount ELSE 0 END) as sell_value,
            SUM(COALESCE(realized_pnl, 0)) as total_realized_pnl,
            AVG(price) as avg_price
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND symbol IS NOT NULL
        AND executed_at BETWEEN :fromDate AND :toDate
        GROUP BY symbol
        ORDER BY total_realized_pnl DESC
        """, nativeQuery = true)
    List<Object[]> getSymbolTradingStatistics(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Get dividend income summary
     */
    @Query(value = """
        SELECT 
            symbol,
            COUNT(*) as dividend_payments,
            SUM(amount) as total_dividends,
            AVG(price) as avg_dividend_per_share,
            MIN(executed_at) as first_payment,
            MAX(executed_at) as last_payment
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND transaction_type = 'DIVIDEND'
        AND executed_at BETWEEN :fromDate AND :toDate
        GROUP BY symbol
        ORDER BY total_dividends DESC
        """, nativeQuery = true)
    List<Object[]> getDividendIncomeSummary(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Find transactions by order ID
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.orderId = :orderId ORDER BY t.executedAt")
    List<PortfolioTransaction> findByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Find transactions by trade ID
     */
    @Query("SELECT t FROM PortfolioTransaction t WHERE t.tradeId = :tradeId")
    List<PortfolioTransaction> findByTradeId(@Param("tradeId") String tradeId);
    
    /**
     * Get recent transactions for activity feed
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        ORDER BY t.executedAt DESC 
        LIMIT :limit
        """)
    List<PortfolioTransaction> findRecentTransactions(
        @Param("portfolioId") Long portfolioId,
        @Param("limit") Integer limit
    );
    
    /**
     * Get cost basis for symbol using FIFO method
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.symbol = :symbol
        AND t.transactionType IN ('BUY', 'SHORT_SELL')
        ORDER BY t.executedAt ASC
        """)
    List<PortfolioTransaction> getFIFOTransactionsForSymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Get cost basis for symbol using LIFO method
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.symbol = :symbol
        AND t.transactionType IN ('BUY', 'SHORT_SELL')
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> getLIFOTransactionsForSymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Calculate weighted average cost for symbol
     */
    @Query("""
        SELECT 
            SUM(t.quantity * t.price) / SUM(t.quantity) as weightedAverageCost
        FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.symbol = :symbol
        AND t.transactionType IN ('BUY', 'SHORT_SELL')
        """)
    BigDecimal getWeightedAverageCostForSymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Get tax lot information for symbol
     */
    @Query(value = """
        SELECT 
            executed_at as purchase_date,
            quantity,
            price as cost_basis,
            quantity * price as total_cost,
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - executed_at)) / 86400 as holding_days
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND symbol = :symbol
        AND transaction_type = 'BUY'
        ORDER BY executed_at ASC
        """, nativeQuery = true)
    List<Object[]> getTaxLotsForSymbol(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Get monthly P&L summary
     */
    @Query(value = """
        SELECT 
            EXTRACT(YEAR FROM executed_at) as year,
            EXTRACT(MONTH FROM executed_at) as month,
            SUM(COALESCE(realized_pnl, 0)) as monthly_realized_pnl,
            COUNT(*) as transactions_count,
            SUM(commission + tax + other_fees) as monthly_fees
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND realized_pnl IS NOT NULL
        GROUP BY EXTRACT(YEAR FROM executed_at), EXTRACT(MONTH FROM executed_at)
        ORDER BY year DESC, month DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyPnLSummary(@Param("portfolioId") Long portfolioId);
    
    /**
     * Count transactions by type for analytics
     */
    @Query(value = """
        SELECT 
            transaction_type,
            COUNT(*) as transaction_count,
            SUM(amount) as total_amount
        FROM portfolio_transactions 
        WHERE portfolio_id = :portfolioId 
        AND executed_at BETWEEN :fromDate AND :toDate
        GROUP BY transaction_type
        ORDER BY transaction_count DESC
        """, nativeQuery = true)
    List<Object[]> getTransactionTypeStatistics(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Find transactions by portfolio and symbol ordered by execution time
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.symbol = :symbol
        ORDER BY t.executedAt ASC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndSymbolOrderByExecutionTime(
        @Param("portfolioId") Long portfolioId,
        @Param("symbol") String symbol
    );
    
    /**
     * Find transactions by portfolio and execution time between dates
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.executedAt BETWEEN :fromDate AND :toDate
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndExecutionTimeBetween(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
    
    /**
     * Find transactions by portfolio, date range and transaction types
     */
    @Query("""
        SELECT t FROM PortfolioTransaction t 
        WHERE t.portfolioId = :portfolioId 
        AND t.executedAt BETWEEN :fromDate AND :toDate
        AND t.transactionType IN :transactionTypes
        ORDER BY t.executedAt DESC
        """)
    List<PortfolioTransaction> findByPortfolioIdAndExecutionTimeBetweenAndTransactionTypeIn(
        @Param("portfolioId") Long portfolioId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate,
        @Param("transactionTypes") List<TransactionType> transactionTypes
    );
}