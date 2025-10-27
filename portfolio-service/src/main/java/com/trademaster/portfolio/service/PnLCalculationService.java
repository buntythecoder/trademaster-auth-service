package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.*;
import com.trademaster.portfolio.entity.Portfolio;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.CostBasisMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * P&L Calculation Service Interface
 * 
 * Comprehensive profit and loss calculation engine using Java 24 Virtual Threads.
 * Provides real-time P&L tracking with multiple accounting methods and tax optimization.
 * 
 * Key Features:
 * - Real-time unrealized P&L calculations (<10ms)
 * - Realized P&L tracking with FIFO/LIFO/Weighted Average methods
 * - Tax lot management for compliance and optimization
 * - Day P&L and performance attribution
 * - Portfolio-level and position-level P&L aggregation
 * - Comprehensive audit trail for financial compliance
 * 
 * Performance Targets:
 * - Position P&L calculation: <5ms
 * - Portfolio valuation: <50ms
 * - Tax lot calculation: <25ms
 * - Bulk P&L updates: <100ms for 1000+ positions
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PnLCalculationService {
    
    /**
     * Calculate current portfolio valuation with P&L breakdown
     * 
     * @param portfolioId The portfolio ID
     * @return Portfolio valuation result
     */
    PortfolioValuationResult calculatePortfolioValuation(Long portfolioId);
    
    /**
     * Calculate portfolio valuation asynchronously
     * 
     * @param portfolioId The portfolio ID
     * @return CompletableFuture with valuation result
     */
    CompletableFuture<PortfolioValuationResult> calculatePortfolioValuationAsync(Long portfolioId);
    
    /**
     * Calculate unrealized P&L for specific position
     * 
     * @param position The position
     * @param currentPrice Current market price
     * @return Unrealized P&L amount
     */
    BigDecimal calculateUnrealizedPnL(Position position, BigDecimal currentPrice);
    
    /**
     * Calculate realized P&L from trade execution
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param tradeQuantity Trade quantity (negative for sells)
     * @param tradePrice Trade price
     * @param costBasisMethod Cost basis method (FIFO, LIFO, WAC)
     * @return Realized P&L calculation result
     */
    RealizedPnLResult calculateRealizedPnL(Long portfolioId, String symbol, 
                                          Integer tradeQuantity, BigDecimal tradePrice,
                                          CostBasisMethod costBasisMethod);
    
    /**
     * Calculate day P&L for position
     * 
     * @param position The position
     * @param currentPrice Current price
     * @param previousClosePrice Previous close price
     * @return Day P&L amount
     */
    BigDecimal calculateDayPnL(Position position, BigDecimal currentPrice, BigDecimal previousClosePrice);
    
    /**
     * Calculate weighted average cost for position
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @return Weighted average cost
     */
    BigDecimal calculateWeightedAverageCost(Long portfolioId, String symbol);
    
    /**
     * Get tax lots for symbol using specified method
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param costBasisMethod Cost basis method
     * @return List of tax lots
     */
    List<TaxLotInfo> getTaxLots(Long portfolioId, String symbol, CostBasisMethod costBasisMethod);
    
    /**
     * Calculate P&L breakdown by time period
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return P&L breakdown result
     */
    PnLBreakdown calculatePnLBreakdown(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Calculate position-level P&L metrics
     * 
     * @param portfolioId The portfolio ID
     * @return List of position P&L metrics
     */
    List<PositionPnLMetrics> calculatePositionPnLMetrics(Long portfolioId);
    
    /**
     * Calculate portfolio performance attribution
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Performance attribution result
     */
    PerformanceAttribution calculatePerformanceAttribution(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Update position cost basis from trade
     * 
     * @param position The position
     * @param tradeQuantity Trade quantity
     * @param tradePrice Trade price
     * @param costBasisMethod Cost basis method
     * @return Updated cost basis result
     */
    CostBasisUpdateResult updateCostBasis(Position position, Integer tradeQuantity, 
                                         BigDecimal tradePrice, CostBasisMethod costBasisMethod);
    
    /**
     * Calculate total return for position
     * 
     * @param position The position
     * @param currentPrice Current market price
     * @return Total return percentage
     */
    BigDecimal calculateTotalReturn(Position position, BigDecimal currentPrice);
    
    /**
     * Calculate annualized return for position
     * 
     * @param position The position
     * @param currentPrice Current market price
     * @return Annualized return percentage
     */
    BigDecimal calculateAnnualizedReturn(Position position, BigDecimal currentPrice);
    
    /**
     * Bulk calculate unrealized P&L for all positions
     * 
     * @param portfolioId The portfolio ID
     * @return CompletableFuture with total unrealized P&L
     */
    CompletableFuture<BigDecimal> bulkCalculateUnrealizedPnL(Long portfolioId);
    
    /**
     * Calculate P&L impact from hypothetical trade
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param quantity Trade quantity
     * @param price Trade price
     * @return P&L impact analysis
     */
    PnLImpactAnalysis calculateTradeImpact(Long portfolioId, String symbol, Integer quantity, BigDecimal price);
    
    /**
     * Get monthly P&L summary
     * 
     * @param portfolioId The portfolio ID
     * @param year The year
     * @return Monthly P&L summary
     */
    List<MonthlyPnLSummary> getMonthlyPnLSummary(Long portfolioId, Integer year);
    
    /**
     * Calculate dividend and interest income
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Income calculation result
     */
    IncomeCalculationResult calculateIncome(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Calculate fees and commissions impact
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return Fees calculation result
     */
    FeesCalculationResult calculateFees(Long portfolioId, Instant fromDate, Instant toDate);
    
    /**
     * Validate P&L calculation accuracy
     * 
     * @param portfolioId The portfolio ID
     * @return P&L validation result
     */
    PnLValidationResult validatePnLCalculation(Long portfolioId);
    
    /**
     * Get P&L trend analysis
     * 
     * @param portfolioId The portfolio ID
     * @param periodDays Number of days for analysis
     * @return P&L trend data
     */
    PnLTrendAnalysis getPnLTrend(Long portfolioId, Integer periodDays);
    
    /**
     * Calculate sharpe ratio for portfolio
     * 
     * @param portfolioId The portfolio ID
     * @param riskFreeRate Risk-free rate for calculation
     * @param periodDays Analysis period in days
     * @return Sharpe ratio
     */
    BigDecimal calculateSharpeRatio(Long portfolioId, BigDecimal riskFreeRate, Integer periodDays);
    
    /**
     * Calculate maximum drawdown
     * 
     * @param portfolioId The portfolio ID
     * @param periodDays Analysis period in days
     * @return Maximum drawdown percentage
     */
    BigDecimal calculateMaxDrawdown(Long portfolioId, Integer periodDays);
    
    /**
     * Generate P&L attribution report
     * 
     * @param portfolioId The portfolio ID
     * @param fromDate Start date
     * @param toDate End date
     * @return P&L attribution report
     */
    CompletableFuture<PnLAttributionReport> generatePnLReport(Long portfolioId, Instant fromDate, Instant toDate);
}