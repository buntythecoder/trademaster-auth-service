package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.MarketDataUpdate;
import com.trademaster.portfolio.dto.PositionUpdateRequest;
import com.trademaster.portfolio.dto.TradeExecutionRequest;
import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.PositionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Position Service Interface
 * 
 * Core position management operations using Java 24 Virtual Threads.
 * Provides high-performance position tracking with sub-10ms updates.
 * 
 * Key Features:
 * - Real-time position updates from trade executions
 * - Bulk market data price updates with Virtual Threads
 * - Position risk analysis and concentration monitoring
 * - Comprehensive P&L calculations (realized and unrealized)
 * - Structured logging and Prometheus metrics
 * 
 * Performance Targets:
 * - Position update: <10ms
 * - Price update: <5ms per position
 * - Bulk price updates: <50ms for 1000+ positions
 * - Position retrieval: <5ms
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PositionService {
    
    /**
     * Update position from trade execution
     * 
     * @param portfolioId The portfolio ID
     * @param request Trade execution details
     * @return Updated position
     */
    Position updatePositionFromTrade(Long portfolioId, TradeExecutionRequest request);
    
    /**
     * Update position from trade execution asynchronously
     * 
     * @param portfolioId The portfolio ID
     * @param request Trade execution details
     * @return CompletableFuture with updated position
     */
    CompletableFuture<Position> updatePositionFromTradeAsync(Long portfolioId, TradeExecutionRequest request);
    
    /**
     * Update position price from market data
     * 
     * @param symbol The symbol to update
     * @param newPrice New market price
     * @param timestamp Price update timestamp
     * @return Number of positions updated
     */
    int updatePositionPrice(String symbol, BigDecimal newPrice, Instant timestamp);
    
    /**
     * Bulk update position prices from market data feed
     * 
     * @param marketDataUpdates List of price updates
     * @return CompletableFuture with total positions updated
     */
    CompletableFuture<Integer> bulkUpdatePositionPrices(List<MarketDataUpdate> marketDataUpdates);
    
    /**
     * Create new position from first trade
     * 
     * @param portfolioId The portfolio ID
     * @param request Trade execution details
     * @return New position
     */
    Position createPosition(Long portfolioId, TradeExecutionRequest request);
    
    /**
     * Close position (set quantity to zero)
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param closePrice Final price
     * @param closeReason Reason for closure
     * @return Closed position
     */
    Position closePosition(Long portfolioId, String symbol, BigDecimal closePrice, String closeReason);
    
    /**
     * Get position by portfolio and symbol
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @return Position if found
     */
    Position getPosition(Long portfolioId, String symbol);
    
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
     * Get positions by type
     * 
     * @param portfolioId The portfolio ID
     * @param positionType The position type
     * @return List of positions
     */
    List<Position> getPositionsByType(Long portfolioId, PositionType positionType);
    
    /**
     * Get largest positions by market value
     * 
     * @param portfolioId The portfolio ID
     * @param limit Number of positions to return
     * @return List of largest positions
     */
    List<Position> getLargestPositions(Long portfolioId, int limit);
    
    /**
     * Get top gaining positions
     * 
     * @param portfolioId The portfolio ID
     * @param limit Number of positions to return
     * @return List of top gainers
     */
    List<Position> getTopGainers(Long portfolioId, int limit);
    
    /**
     * Get top losing positions
     * 
     * @param portfolioId The portfolio ID
     * @param limit Number of positions to return
     * @return List of top losers
     */
    List<Position> getTopLosers(Long portfolioId, int limit);
    
    /**
     * Calculate position concentration risk
     * 
     * @param portfolioId The portfolio ID
     * @return Concentration analysis data
     */
    List<PositionConcentration> calculateConcentrationRisk(Long portfolioId);
    
    /**
     * Get positions requiring margin calls
     * 
     * @param portfolioId The portfolio ID
     * @param marginCallThreshold Threshold for margin call
     * @return List of positions requiring margin call
     */
    List<Position> getPositionsRequiringMarginCall(Long portfolioId, BigDecimal marginCallThreshold);
    
    /**
     * Update position price manually (admin operation)
     * 
     * @param positionId The position ID
     * @param newPrice New price
     * @param adminUserId Admin user ID
     * @param reason Reason for manual update
     * @return Updated position
     */
    Position updatePositionPriceManually(Long positionId, BigDecimal newPrice, Long adminUserId, String reason);
    
    /**
     * Get positions requiring price updates
     * 
     * @param cutoffTime Cutoff time for last update
     * @return List of positions needing price updates
     */
    List<Position> getPositionsRequiringPriceUpdate(Instant cutoffTime);
    
    /**
     * Calculate position metrics for analytics
     * 
     * @param portfolioId The portfolio ID
     * @return CompletableFuture with position metrics
     */
    CompletableFuture<PositionMetrics> calculatePositionMetrics(Long portfolioId);
    
    /**
     * Get asset allocation breakdown
     * 
     * @param portfolioId The portfolio ID
     * @return Asset allocation data
     */
    List<AssetAllocation> getAssetAllocation(Long portfolioId);
    
    /**
     * Get exchange distribution
     * 
     * @param portfolioId The portfolio ID
     * @return Exchange distribution data
     */
    List<ExchangeDistribution> getExchangeDistribution(Long portfolioId);
    
    /**
     * Calculate average holding period
     * 
     * @param portfolioId The portfolio ID
     * @return Average holding period by symbol
     */
    List<HoldingPeriod> getAverageHoldingPeriod(Long portfolioId);
    
    /**
     * Get position performance statistics
     * 
     * @param portfolioId The portfolio ID
     * @return Position performance summary
     */
    PositionStatistics getPositionStatistics(Long portfolioId);
    
    /**
     * Update day P&L for end-of-day processing
     * 
     * @param symbol The symbol
     * @param currentPrice Current price
     * @param previousClosePrice Previous close price
     * @return Number of positions updated
     */
    int updateDayPnlForSymbol(String symbol, BigDecimal currentPrice, BigDecimal previousClosePrice);
    
    /**
     * Clean up closed positions older than specified date
     * 
     * @param cutoffDate Cutoff date for cleanup
     * @return Number of positions deleted
     */
    int cleanupClosedPositions(Instant cutoffDate);
    
    /**
     * Validate position operation
     * 
     * @param portfolioId The portfolio ID
     * @param symbol The symbol
     * @param operation The operation
     * @return true if operation is valid
     */
    boolean validatePositionOperation(Long portfolioId, String symbol, String operation);
    
    /**
     * Get positions by value range
     * 
     * @param portfolioId The portfolio ID
     * @param minValue Minimum market value
     * @param maxValue Maximum market value
     * @return List of positions in range
     */
    List<Position> getPositionsByValueRange(Long portfolioId, BigDecimal minValue, BigDecimal maxValue);
    
    /**
     * Count positions by symbol across all portfolios
     * 
     * @param symbol The symbol
     * @return Number of portfolios holding the symbol
     */
    Long countPositionsBySymbol(String symbol);
}

/**
 * Position Concentration DTO
 */
record PositionConcentration(
    String symbol,
    BigDecimal marketValue,
    BigDecimal concentrationPercent,
    String riskLevel,
    boolean exceedsThreshold
) {}

/**
 * Position Metrics DTO
 */
record PositionMetrics(
    Long portfolioId,
    Integer totalPositions,
    Integer profitablePositions,
    Integer losingPositions,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedPnl,
    BigDecimal averageUnrealizedPnl,
    BigDecimal bestPerformer,
    BigDecimal worstPerformer,
    BigDecimal concentrationRisk,
    Instant calculatedAt
) {}

/**
 * Asset Allocation DTO
 */
record AssetAllocation(
    String instrumentType,
    Integer positionCount,
    BigDecimal totalValue,
    BigDecimal totalUnrealizedPnl,
    BigDecimal averageUnrealizedPnl,
    BigDecimal allocationPercent
) {}

/**
 * Exchange Distribution DTO
 */
record ExchangeDistribution(
    String exchange,
    Integer positionCount,
    BigDecimal totalValue,
    BigDecimal averageReturnPercent,
    BigDecimal allocationPercent
) {}

/**
 * Holding Period DTO
 */
record HoldingPeriod(
    String symbol,
    Integer averageHoldingDays,
    BigDecimal totalReturn,
    String holdingCategory
) {}

/**
 * Position Statistics DTO
 */
record PositionStatistics(
    Integer totalPositions,
    Integer profitablePositions,
    Integer losingPositions,
    BigDecimal totalMarketValue,
    BigDecimal totalUnrealizedPnl,
    BigDecimal averagePositionSize,
    BigDecimal largestPosition,
    BigDecimal smallestPosition,
    Double winRate,
    Instant calculatedAt
) {}