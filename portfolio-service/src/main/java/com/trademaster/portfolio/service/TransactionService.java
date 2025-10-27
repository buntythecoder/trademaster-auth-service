package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.*;
import com.trademaster.portfolio.entity.PortfolioTransaction;
import com.trademaster.portfolio.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Transaction Service Interface
 *
 * Comprehensive transaction processing service using Java 24 Virtual Threads.
 * Provides complete audit trail for all portfolio transactions with sub-50ms processing.
 *
 * Key Features:
 * - Trade execution transaction recording
 * - Cash movement processing (deposits/withdrawals)
 * - Dividend and corporate action handling
 * - Transaction history and reporting
 * - P&L impact calculation
 * - Comprehensive audit trail
 *
 * Performance Targets:
 * - Transaction recording: <25ms
 * - History retrieval: <50ms
 * - Bulk transaction processing: <100ms for 1000+ transactions
 * - Report generation: <200ms
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface TransactionService {

    // ==================== TRANSACTION RECORDING ====================

    /**
     * Record trade execution transaction
     *
     * @param portfolioId Portfolio identifier
     * @param orderId Order identifier
     * @param tradeId Trade identifier
     * @param symbol Stock symbol
     * @param exchange Exchange code
     * @param transactionType Transaction type (BUY/SELL/SHORT_SELL/BUY_TO_COVER)
     * @param quantity Number of shares
     * @param price Execution price
     * @param commission Commission charged
     * @param executedAt Execution timestamp
     * @return Recorded transaction
     */
    PortfolioTransaction recordTradeExecution(
        Long portfolioId,
        Long orderId,
        String tradeId,
        String symbol,
        String exchange,
        TransactionType transactionType,
        Integer quantity,
        BigDecimal price,
        BigDecimal commission,
        Instant executedAt
    );

    /**
     * Record trade execution asynchronously
     *
     * @param portfolioId Portfolio identifier
     * @param orderId Order identifier
     * @param tradeId Trade identifier
     * @param symbol Stock symbol
     * @param exchange Exchange code
     * @param transactionType Transaction type
     * @param quantity Number of shares
     * @param price Execution price
     * @param commission Commission charged
     * @param executedAt Execution timestamp
     * @return CompletableFuture with recorded transaction
     */
    CompletableFuture<PortfolioTransaction> recordTradeExecutionAsync(
        Long portfolioId,
        Long orderId,
        String tradeId,
        String symbol,
        String exchange,
        TransactionType transactionType,
        Integer quantity,
        BigDecimal price,
        BigDecimal commission,
        Instant executedAt
    );

    /**
     * Record cash transaction (deposit or withdrawal)
     *
     * @param portfolioId Portfolio identifier
     * @param transactionType Transaction type (DEPOSIT/WITHDRAWAL)
     * @param amount Transaction amount
     * @param description Transaction description
     * @return Recorded transaction
     */
    PortfolioTransaction recordCashTransaction(
        Long portfolioId,
        TransactionType transactionType,
        BigDecimal amount,
        String description
    );

    /**
     * Record dividend payment
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Stock symbol
     * @param dividendPerShare Dividend amount per share
     * @param shares Number of shares
     * @param paymentDate Payment date
     * @return Recorded transaction
     */
    PortfolioTransaction recordDividend(
        Long portfolioId,
        String symbol,
        BigDecimal dividendPerShare,
        Integer shares,
        Instant paymentDate
    );

    /**
     * Record interest payment on cash balance
     *
     * @param portfolioId Portfolio identifier
     * @param interestAmount Interest amount
     * @param paymentDate Payment date
     * @return Recorded transaction
     */
    PortfolioTransaction recordInterest(
        Long portfolioId,
        BigDecimal interestAmount,
        Instant paymentDate
    );

    /**
     * Record stock split adjustment
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Stock symbol
     * @param splitRatio Split ratio (e.g., 2:1 = 2.0)
     * @param effectiveDate Split effective date
     * @return Recorded transaction
     */
    PortfolioTransaction recordStockSplit(
        Long portfolioId,
        String symbol,
        BigDecimal splitRatio,
        Instant effectiveDate
    );

    /**
     * Record stock dividend (additional shares)
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Stock symbol
     * @param sharesReceived Number of shares received
     * @param paymentDate Payment date
     * @return Recorded transaction
     */
    PortfolioTransaction recordStockDividend(
        Long portfolioId,
        String symbol,
        Integer sharesReceived,
        Instant paymentDate
    );

    /**
     * Bulk record transactions asynchronously
     *
     * @param transactions List of transactions to record
     * @return CompletableFuture with list of recorded transactions
     */
    CompletableFuture<List<PortfolioTransaction>> bulkRecordTransactions(
        List<PortfolioTransaction> transactions
    );

    // ==================== TRANSACTION RETRIEVAL ====================

    /**
     * Get all transactions for portfolio
     *
     * @param portfolioId Portfolio identifier
     * @return List of all transactions
     */
    List<PortfolioTransaction> getAllTransactions(Long portfolioId);

    /**
     * Get transactions by date range
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of transactions in date range
     */
    List<PortfolioTransaction> getTransactionsByDateRange(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Get transactions by symbol
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Stock symbol
     * @return List of transactions for symbol
     */
    List<PortfolioTransaction> getTransactionsBySymbol(Long portfolioId, String symbol);

    /**
     * Get transactions by type
     *
     * @param portfolioId Portfolio identifier
     * @param transactionType Transaction type
     * @return List of transactions of specified type
     */
    List<PortfolioTransaction> getTransactionsByType(
        Long portfolioId,
        TransactionType transactionType
    );

    /**
     * Get trade execution transactions only
     *
     * @param portfolioId Portfolio identifier
     * @return List of trade executions
     */
    List<PortfolioTransaction> getTradeExecutions(Long portfolioId);

    /**
     * Get cash transactions (deposits and withdrawals)
     *
     * @param portfolioId Portfolio identifier
     * @return List of cash transactions
     */
    List<PortfolioTransaction> getCashTransactions(Long portfolioId);

    /**
     * Get dividend and interest transactions
     *
     * @param portfolioId Portfolio identifier
     * @return List of income transactions
     */
    List<PortfolioTransaction> getIncomeTransactions(Long portfolioId);

    /**
     * Get recent transactions for activity feed
     *
     * @param portfolioId Portfolio identifier
     * @param limit Maximum number of transactions
     * @return List of recent transactions
     */
    List<PortfolioTransaction> getRecentTransactions(Long portfolioId, Integer limit);

    /**
     * Get transactions by order ID
     *
     * @param orderId Order identifier
     * @return List of transactions for order
     */
    List<PortfolioTransaction> getTransactionsByOrderId(Long orderId);

    /**
     * Get transactions by trade ID
     *
     * @param tradeId Trade identifier
     * @return List of transactions for trade
     */
    List<PortfolioTransaction> getTransactionsByTradeId(String tradeId);

    // ==================== TRANSACTION ANALYTICS ====================

    /**
     * Calculate realized P&L for date range
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Total realized P&L
     */
    BigDecimal calculateRealizedPnL(Long portfolioId, Instant fromDate, Instant toDate);

    /**
     * Calculate total fees and commissions
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Total fees
     */
    BigDecimal calculateTotalFees(Long portfolioId, Instant fromDate, Instant toDate);

    /**
     * Calculate net cash flow
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Net cash flow
     */
    BigDecimal calculateNetCashFlow(Long portfolioId, Instant fromDate, Instant toDate);

    /**
     * Generate transaction summary report
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Transaction summary
     */
    TransactionSummary generateTransactionSummary(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Generate transaction summary asynchronously
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return CompletableFuture with transaction summary
     */
    CompletableFuture<TransactionSummary> generateTransactionSummaryAsync(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Get daily transaction counts
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Daily transaction counts
     */
    List<DailyTransactionCount> getDailyTransactionCounts(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Get symbol trading statistics
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Symbol trading statistics
     */
    List<SymbolTradingStats> getSymbolTradingStatistics(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Get dividend income summary
     *
     * @param portfolioId Portfolio identifier
     * @param fromDate Start date
     * @param toDate End date
     * @return Dividend income summary
     */
    List<DividendIncomeSummary> getDividendIncomeSummary(
        Long portfolioId,
        Instant fromDate,
        Instant toDate
    );

    /**
     * Get monthly P&L summary
     *
     * @param portfolioId Portfolio identifier
     * @return Monthly P&L data
     */
    List<MonthlyPnLSummary> getMonthlyPnLSummary(Long portfolioId);

    // ==================== TRANSACTION VALIDATION ====================

    /**
     * Validate transaction before recording
     *
     * @param portfolioId Portfolio identifier
     * @param transactionType Transaction type
     * @param amount Transaction amount
     * @param quantity Transaction quantity (if applicable)
     * @return true if valid
     */
    boolean validateTransaction(
        Long portfolioId,
        TransactionType transactionType,
        BigDecimal amount,
        Integer quantity
    );

    /**
     * Check if transaction can be recorded
     *
     * @param portfolioId Portfolio identifier
     * @param symbol Stock symbol (if applicable)
     * @param transactionType Transaction type
     * @param quantity Quantity (if applicable)
     * @return true if transaction can be recorded
     */
    boolean canRecordTransaction(
        Long portfolioId,
        String symbol,
        TransactionType transactionType,
        Integer quantity
    );
}
