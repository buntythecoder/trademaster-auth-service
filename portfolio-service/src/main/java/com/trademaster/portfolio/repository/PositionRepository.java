package com.trademaster.portfolio.repository;

import com.trademaster.portfolio.entity.Position;
import com.trademaster.portfolio.model.PositionType;
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
 * Position Repository
 * 
 * High-performance JPA repository for position management operations.
 * Optimized for sub-10ms position updates with comprehensive monitoring.
 * 
 * Performance Features:
 * - Atomic position updates for trade executions
 * - Bulk price updates for market data feeds
 * - Optimized queries with strategic indexing
 * - Structured logging for all position operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * Find position by portfolio and symbol
     * Primary lookup method for position updates
     */
    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.symbol = :symbol")
    Optional<Position> findByPortfolioIdAndSymbol(
        @Param("portfolioId") Long portfolioId, 
        @Param("symbol") String symbol
    );
    
    /**
     * Find all positions for a portfolio
     */
    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId ORDER BY p.marketValue DESC")
    List<Position> findByPortfolioId(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find all open positions for a portfolio
     */
    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.quantity != 0 ORDER BY p.marketValue DESC")
    List<Position> findOpenPositionsByPortfolioId(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find positions by symbol across all portfolios
     * Used for market data price updates
     */
    @Query("SELECT p FROM Position p WHERE p.symbol = :symbol AND p.quantity != 0")
    List<Position> findBySymbol(@Param("symbol") String symbol);
    
    /**
     * Find positions by multiple symbols - batch price updates
     */
    @Query("SELECT p FROM Position p WHERE p.symbol IN :symbols AND p.quantity != 0")
    List<Position> findBySymbolIn(@Param("symbols") List<String> symbols);
    
    /**
     * Find positions requiring price updates
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.quantity != 0 
        AND (p.lastPriceUpdateAt IS NULL OR p.lastPriceUpdateAt < :cutoffTime)
        """)
    List<Position> findPositionsRequiringPriceUpdate(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Get largest positions by market value
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.quantity != 0 
        ORDER BY p.marketValue DESC
        """)
    List<Position> findLargestPositions(@Param("portfolioId") Long portfolioId);
    
    /**
     * Get top gainers in portfolio
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.quantity != 0 
        AND p.unrealizedPnl > 0
        ORDER BY p.unrealizedPnl DESC
        """)
    List<Position> findTopGainers(@Param("portfolioId") Long portfolioId);
    
    /**
     * Get top losers in portfolio
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.quantity != 0 
        AND p.unrealizedPnl < 0
        ORDER BY p.unrealizedPnl ASC
        """)
    List<Position> findTopLosers(@Param("portfolioId") Long portfolioId);
    
    /**
     * Update position price and market value - atomic operation
     */
    @Modifying
    @Query("""
        UPDATE Position p 
        SET p.currentPrice = :price,
            p.marketValue = :price * ABS(p.quantity),
            p.unrealizedPnl = (:price * ABS(p.quantity)) - (p.averageCost * ABS(p.quantity)),
            p.lastPriceUpdateAt = :timestamp
        WHERE p.positionId = :positionId
        """)
    int updatePositionPrice(
        @Param("positionId") Long positionId,
        @Param("price") BigDecimal price,
        @Param("timestamp") Instant timestamp
    );
    
    /**
     * Batch update prices for multiple positions
     */
    @Modifying
    @Query("""
        UPDATE Position p 
        SET p.currentPrice = :price,
            p.marketValue = :price * ABS(p.quantity),
            p.unrealizedPnl = (:price * ABS(p.quantity)) - (p.averageCost * ABS(p.quantity)),
            p.lastPriceUpdateAt = :timestamp
        WHERE p.symbol = :symbol AND p.quantity != 0
        """)
    int updatePriceForSymbol(
        @Param("symbol") String symbol,
        @Param("price") BigDecimal price,
        @Param("timestamp") Instant timestamp
    );
    
    /**
     * Update position from trade execution - complex atomic operation
     */
    @Modifying
    @Query("""
        UPDATE Position p 
        SET p.quantity = :newQuantity,
            p.averageCost = :newAverageCost,
            p.totalCost = :newTotalCost,
            p.lastTradePrice = :tradePrice,
            p.lastTradeQuantity = :tradeQuantity,
            p.lastTradeAt = :tradeTime,
            p.realizedPnl = p.realizedPnl + :realizedPnl
        WHERE p.positionId = :positionId
        """)
    int updatePositionFromTrade(
        @Param("positionId") Long positionId,
        @Param("newQuantity") Integer newQuantity,
        @Param("newAverageCost") BigDecimal newAverageCost,
        @Param("newTotalCost") BigDecimal newTotalCost,
        @Param("tradePrice") BigDecimal tradePrice,
        @Param("tradeQuantity") Integer tradeQuantity,
        @Param("tradeTime") Instant tradeTime,
        @Param("realizedPnl") BigDecimal realizedPnl
    );
    
    /**
     * Update day P&L for end-of-day calculations
     */
    @Modifying
    @Query("""
        UPDATE Position p 
        SET p.dayPnl = (:currentPrice - p.previousClosePrice) * p.quantity,
            p.previousClosePrice = :previousClosePrice
        WHERE p.symbol = :symbol AND p.quantity != 0
        """)
    int updateDayPnlForSymbol(
        @Param("symbol") String symbol,
        @Param("currentPrice") BigDecimal currentPrice,
        @Param("previousClosePrice") BigDecimal previousClosePrice
    );
    
    /**
     * Get position concentration analysis
     */
    @Query("""
        SELECT NEW map(
            p.symbol as symbol,
            p.marketValue as marketValue,
            (p.marketValue / SUM(p.marketValue) OVER()) * 100 as concentrationPercent
        )
        FROM Position p 
        WHERE p.portfolioId = :portfolioId AND p.quantity != 0
        ORDER BY p.marketValue DESC
        """)
    List<Object> getPositionConcentration(@Param("portfolioId") Long portfolioId);
    
    /**
     * Get positions by position type
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.positionType = :positionType 
        AND p.quantity != 0
        """)
    List<Position> findByPortfolioIdAndPositionType(
        @Param("portfolioId") Long portfolioId,
        @Param("positionType") PositionType positionType
    );
    
    /**
     * Get portfolio asset allocation
     */
    @Query(value = """
        SELECT 
            instrument_type,
            COUNT(*) as position_count,
            SUM(market_value) as total_value,
            SUM(unrealized_pnl) as total_unrealized_pnl,
            AVG(unrealized_pnl) as avg_unrealized_pnl
        FROM positions 
        WHERE portfolio_id = :portfolioId AND quantity != 0
        GROUP BY instrument_type
        ORDER BY total_value DESC
        """, nativeQuery = true)
    List<Object[]> getAssetAllocation(@Param("portfolioId") Long portfolioId);
    
    /**
     * Get exchange-wise distribution
     */
    @Query(value = """
        SELECT 
            exchange,
            COUNT(*) as position_count,
            SUM(market_value) as total_value,
            AVG((current_price - average_cost) / average_cost * 100) as avg_return_percent
        FROM positions 
        WHERE portfolio_id = :portfolioId AND quantity != 0
        GROUP BY exchange
        ORDER BY total_value DESC
        """, nativeQuery = true)
    List<Object[]> getExchangeDistribution(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find positions with high concentration risk
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.quantity != 0
        AND p.marketValue > (
            SELECT SUM(pos.marketValue) * :concentrationThreshold / 100 
            FROM Position pos 
            WHERE pos.portfolioId = :portfolioId AND pos.quantity != 0
        )
        ORDER BY p.marketValue DESC
        """)
    List<Position> findHighConcentrationPositions(
        @Param("portfolioId") Long portfolioId,
        @Param("concentrationThreshold") BigDecimal concentrationThreshold
    );
    
    /**
     * Get positions performance statistics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_positions,
            COUNT(CASE WHEN unrealized_pnl > 0 THEN 1 END) as profitable_positions,
            COUNT(CASE WHEN unrealized_pnl < 0 THEN 1 END) as losing_positions,
            AVG(unrealized_pnl) as avg_unrealized_pnl,
            SUM(market_value) as total_market_value,
            SUM(unrealized_pnl) as total_unrealized_pnl,
            MAX(unrealized_pnl) as best_performer,
            MIN(unrealized_pnl) as worst_performer
        FROM positions 
        WHERE portfolio_id = :portfolioId AND quantity != 0
        """, nativeQuery = true)
    List<Object[]> getPositionStatistics(@Param("portfolioId") Long portfolioId);
    
    /**
     * Find positions by value range
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.quantity != 0
        AND p.marketValue BETWEEN :minValue AND :maxValue
        ORDER BY p.marketValue DESC
        """)
    List<Position> findByMarketValueRange(
        @Param("portfolioId") Long portfolioId,
        @Param("minValue") BigDecimal minValue,
        @Param("maxValue") BigDecimal maxValue
    );
    
    /**
     * Get positions requiring margin calls
     */
    @Query("""
        SELECT p FROM Position p 
        WHERE p.portfolioId = :portfolioId 
        AND p.positionType = 'SHORT'
        AND p.unrealizedPnl < :marginCallThreshold
        """)
    List<Position> findPositionsRequiringMarginCall(
        @Param("portfolioId") Long portfolioId,
        @Param("marginCallThreshold") BigDecimal marginCallThreshold
    );
    
    /**
     * Count positions by symbol - for risk analysis
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.symbol = :symbol AND p.quantity != 0")
    Long countPositionsBySymbol(@Param("symbol") String symbol);
    
    /**
     * Get average holding period
     */
    @Query(value = """
        SELECT 
            symbol,
            AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - opened_at)) / 86400) as avg_holding_days
        FROM positions 
        WHERE portfolio_id = :portfolioId AND quantity != 0
        GROUP BY symbol
        ORDER BY avg_holding_days DESC
        """, nativeQuery = true)
    List<Object[]> getAverageHoldingPeriod(@Param("portfolioId") Long portfolioId);
    
    /**
     * Delete closed positions older than specified date
     */
    @Modifying
    @Query("DELETE FROM Position p WHERE p.quantity = 0 AND p.updatedAt < :cutoffDate")
    int deleteClosedPositionsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}