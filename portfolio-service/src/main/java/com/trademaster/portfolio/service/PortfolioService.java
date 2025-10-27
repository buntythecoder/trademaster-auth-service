package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.CreatePortfolioRequest;
import com.trademaster.portfolio.dto.PortfolioSummary;
import com.trademaster.portfolio.dto.RebalancingResult;
import com.trademaster.portfolio.dto.UpdatePortfolioRequest;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.PortfolioStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Service Interface
 * 
 * Core portfolio management operations using Java 24 Virtual Threads.
 * Provides high-performance portfolio operations with comprehensive monitoring.
 * 
 * Key Features:
 * - Real-time portfolio valuation (<50ms response time)
 * - Concurrent position updates with Virtual Threads
 * - Comprehensive P&L tracking and analytics
 * - Risk management integration
 * - Structured logging and Prometheus metrics
 * 
 * Performance Targets:
 * - Portfolio creation: <100ms
 * - Valuation update: <50ms
 * - Position retrieval: <25ms
 * - Bulk operations: <200ms
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PortfolioService {
    
    /**
     * Create a new portfolio for user
     * 
     * @param userId The user ID
     * @param request Portfolio creation request
     * @return Created portfolio
     */
    Portfolio createPortfolio(Long userId, CreatePortfolioRequest request);
    
    /**
     * Get portfolio by user ID
     * 
     * @param userId The user ID
     * @return Portfolio if found
     */
    Portfolio getPortfolioByUserId(Long userId);
    
    /**
     * Get portfolio by portfolio ID
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio if found
     */
    Portfolio getPortfolioById(Long portfolioId);
    
    /**
     * Update portfolio details
     * 
     * @param portfolioId The portfolio ID
     * @param request Update request
     * @return Updated portfolio
     */
    Portfolio updatePortfolio(Long portfolioId, UpdatePortfolioRequest request);
    
    /**
     * Update portfolio status
     * 
     * @param portfolioId The portfolio ID
     * @param newStatus New status
     * @param reason Reason for status change
     * @return Updated portfolio
     */
    Portfolio updatePortfolioStatus(Long portfolioId, PortfolioStatus newStatus, String reason);
    
    /**
     * Get all positions for portfolio
     * 
     * @param portfolioId The portfolio ID
     * @return List of positions
     */
    List<Position> getPortfolioPositions(Long portfolioId);
    
    /**
     * Get open positions only
     * 
     * @param portfolioId The portfolio ID
     * @return List of open positions
     */
    List<Position> getOpenPositions(Long portfolioId);
    
    /**
     * Get specific position by symbol
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @return Position if found
     */
    Position getPosition(Long portfolioId, String symbol);
    
    /**
     * Update portfolio valuation with current market prices
     * 
     * @param portfolioId The portfolio ID
     * @return Updated portfolio with new valuation
     */
    Portfolio updatePortfolioValuation(Long portfolioId);
    
    /**
     * Update portfolio valuation asynchronously
     * 
     * @param portfolioId The portfolio ID
     * @return CompletableFuture with updated portfolio
     */
    CompletableFuture<Portfolio> updatePortfolioValuationAsync(Long portfolioId);
    
    /**
     * Bulk update valuations for multiple portfolios
     * 
     * @param portfolioIds List of portfolio IDs
     * @return CompletableFuture with update count
     */
    CompletableFuture<Integer> bulkUpdateValuations(List<Long> portfolioIds);
    
    /**
     * Update portfolio from cash transaction
     * 
     * @param portfolioId The portfolio ID
     * @param amount Transaction amount (positive for deposits, negative for withdrawals)
     * @param description Transaction description
     * @return Updated portfolio
     */
    Portfolio updateCashBalance(Long portfolioId, BigDecimal amount, String description);
    
    /**
     * Add realized P&L to portfolio
     * 
     * @param portfolioId The portfolio ID
     * @param realizedPnl Realized P&L amount
     * @return Updated portfolio
     */
    Portfolio addRealizedPnl(Long portfolioId, BigDecimal realizedPnl);
    
    /**
     * Get portfolio summary with key metrics
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio summary
     */
    PortfolioSummary getPortfolioSummary(Long portfolioId);
    
    /**
     * Get portfolios requiring valuation update
     * 
     * @param cutoffTime Cutoff time for last valuation
     * @return List of portfolios needing update
     */
    List<Portfolio> getPortfoliosRequiringValuation(Instant cutoffTime);
    
    /**
     * Get portfolios with specific symbol
     * 
     * @param symbol The symbol
     * @return List of portfolios holding the symbol
     */
    List<Portfolio> getPortfoliosWithSymbol(String symbol);
    
    /**
     * Calculate portfolio metrics
     * 
     * @param portfolioId The portfolio ID
     * @return CompletableFuture with portfolio metrics
     */
    CompletableFuture<PortfolioMetrics> calculatePortfolioMetrics(Long portfolioId);
    
    /**
     * Get portfolio performance over time period
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Performance metrics
     */
    PortfolioPerformance getPortfolioPerformance(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Get top performing portfolios
     * 
     * @param limit Number of portfolios to return
     * @return List of top performing portfolios
     */
    List<Portfolio> getTopPerformingPortfolios(int limit);
    
    /**
     * Get underperforming portfolios
     * 
     * @param limit Number of portfolios to return
     * @return List of underperforming portfolios
     */
    List<Portfolio> getUnderperformingPortfolios(int limit);
    
    /**
     * Reset day trades count for all portfolios (daily scheduled task)
     * 
     * @return Number of portfolios updated
     */
    int resetDayTradesCount();
    
    /**
     * Get total Assets Under Management
     * 
     * @return Total AUM across all active portfolios
     */
    BigDecimal getTotalAssetsUnderManagement();
    
    /**
     * Get portfolio statistics
     * 
     * @return Portfolio statistics summary
     */
    PortfolioStatistics getPortfolioStatistics();
    
    /**
     * Validate portfolio operation permissions
     * 
     * @param portfolioId The portfolio ID
     * @param operation The operation being performed
     * @return true if operation is allowed
     */
    boolean validatePortfolioOperation(Long portfolioId, String operation);
    
    /**
     * Archive closed portfolio
     * 
     * @param portfolioId The portfolio ID
     * @return Archived portfolio
     */
    Portfolio archivePortfolio(Long portfolioId);
    
    /**
     * Restore archived portfolio
     * 
     * @param portfolioId The portfolio ID
     * @return Restored portfolio
     */
    Portfolio restorePortfolio(Long portfolioId);
    
    /**
     * Delete portfolio (admin operation)
     * 
     * @param portfolioId The portfolio ID
     * @param adminUserId Admin user performing deletion
     * @param reason Reason for deletion
     */
    void deletePortfolio(Long portfolioId, Long adminUserId, String reason);
    
    /**
     * Get portfolios for user with optional filtering
     * 
     * @param userId The user ID
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return Page of portfolio summaries
     */
    Page<PortfolioSummary> getPortfoliosForUser(Long userId, String status, Pageable pageable);
    
    /**
     * Initiate portfolio rebalancing
     *
     * @param portfolioId The portfolio ID
     * @param strategy Rebalancing strategy
     * @return Rebalancing result
     */
    CompletableFuture<RebalancingResult> initiateRebalancing(Long portfolioId, String strategy);
    
    /**
     * Delete portfolio (simple version for controller)
     *
     * @param portfolioId The portfolio ID
     */
    void deletePortfolio(Long portfolioId);

    /**
     * Get count of active portfolios
     *
     * Used for health checks and monitoring to track the number of
     * portfolios in ACTIVE status across the system.
     *
     * @return Count of active portfolios
     */
    long getActivePortfoliosCount();

    /**
     * Update portfolio valuation with explicit values
     *
     * @param portfolioId The portfolio ID
     * @param newValue New total portfolio value
     * @param newUnrealizedPnl New unrealized P&L
     * @return Updated portfolio
     */
    Portfolio updateValuation(Long portfolioId, BigDecimal newValue, BigDecimal newUnrealizedPnl);

    /**
     * Update portfolio cash balance (simple version)
     *
     * @param portfolioId The portfolio ID
     * @param amount Transaction amount
     * @return Updated portfolio
     */
    Portfolio updateCashBalance(Long portfolioId, BigDecimal amount);

    /**
     * Increment day trades count for portfolio
     *
     * @param portfolioId The portfolio ID
     * @return New day trades count
     */
    int incrementDayTradesCount(Long portfolioId);

    /**
     * Check if portfolio is approaching day trade limit
     *
     * @param portfolioId The portfolio ID
     * @return true if approaching limit (3 or more day trades)
     */
    boolean isApproachingDayTradeLimit(Long portfolioId);

    /**
     * Activate a suspended or closed portfolio
     *
     * @param portfolioId The portfolio ID
     * @return Activated portfolio
     */
    Portfolio activatePortfolio(Long portfolioId);

    /**
     * Close a portfolio
     *
     * @param portfolioId The portfolio ID
     * @return Closed portfolio
     */
    Portfolio closePortfolio(Long portfolioId);

    /**
     * Suspend a portfolio
     *
     * @param portfolioId The portfolio ID
     * @return Suspended portfolio
     */
    Portfolio suspendPortfolio(Long portfolioId);

    /**
     * Check if portfolio has minimum cash balance
     *
     * @param portfolio The portfolio
     * @return true if has minimum balance
     */
    boolean hasMinimumCashBalance(Portfolio portfolio);

    /**
     * Check if portfolio can trade
     *
     * @param portfolio The portfolio
     * @return true if can trade
     */
    boolean canTrade(Portfolio portfolio);
}