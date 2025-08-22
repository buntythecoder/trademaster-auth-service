package com.trademaster.marketdata.repository;

import com.trademaster.marketdata.entity.ChartData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Chart Data Repository
 * 
 * Provides optimized data access methods for charting applications
 * with high-performance queries for time-series data.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface ChartDataRepository extends JpaRepository<ChartData, Long> {
    
    /**
     * Get chart data for a symbol and timeframe within date range
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findChartData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get chart data with pagination
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    Page<ChartData> findChartData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable);
    
    /**
     * Get latest chart data point for a symbol and timeframe
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "ORDER BY c.timestamp DESC LIMIT 1")
    Optional<ChartData> findLatestChartData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe);
    
    /**
     * Get chart data for multiple symbols
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol IN :symbols " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.symbol ASC, c.timestamp ASC")
    List<ChartData> findChartDataForSymbols(
        @Param("symbols") List<String> symbols,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get OHLCV data only (lightweight query)
     */
    @Query("SELECT NEW com.trademaster.marketdata.dto.OHLCVData(" +
           "c.timestamp, c.open, c.high, c.low, c.close, c.volume) " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<Object[]> findOHLCVData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get technical indicators data
     */
    @Query("SELECT c.timestamp, c.close, c.sma20, c.sma50, c.ema12, c.ema26, " +
           "c.rsi, c.macd, c.macdSignal, c.bollingerUpper, c.bollingerMiddle, c.bollingerLower " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<Object[]> findTechnicalIndicators(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get volume analysis data
     */
    @Query("SELECT c.timestamp, c.close, c.volume, c.vwap, c.obv " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<Object[]> findVolumeData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get volatility data
     */
    @Query("SELECT c.timestamp, c.close, c.atr, c.volatility, " +
           "c.bollingerUpper, c.bollingerLower " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<Object[]> findVolatilityData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get candlestick pattern data
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findCandlestickData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get data for backtesting (includes all indicators)
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "AND c.isComplete = true " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findBacktestingData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get high/low data for range analysis
     */
    @Query("SELECT c.timestamp, c.high, c.low, c.close, c.volume " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<Object[]> findHighLowData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Find gaps in data
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.hasGaps = true " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findDataGaps(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get aggregate statistics for a period
     */
    @Query("SELECT " +
           "MIN(c.low) as periodLow, " +
           "MAX(c.high) as periodHigh, " +
           "SUM(c.volume) as totalVolume, " +
           "AVG(c.close) as avgPrice, " +
           "COUNT(c) as dataPoints " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime")
    List<Object[]> getAggregateStatistics(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Find data points with specific patterns
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "AND (" +
           "   (c.rsi <= 30) OR " + // Oversold
           "   (c.rsi >= 70) OR " + // Overbought
           "   (c.volume > :volumeThreshold) OR " + // High volume
           "   (ABS(c.close - c.open) / c.open * 100 > :priceChangeThreshold)" + // Large price movement
           ") " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findSignificantDataPoints(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("volumeThreshold") Long volumeThreshold,
        @Param("priceChangeThreshold") BigDecimal priceChangeThreshold);
    
    /**
     * Get support and resistance levels
     */
    @Query("SELECT c.close, COUNT(*) as touchCount " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY ROUND(c.close, 2) " +
           "HAVING COUNT(*) > :minTouches " +
           "ORDER BY COUNT(*) DESC")
    List<Object[]> findSupportResistanceLevels(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("minTouches") Integer minTouches);
    
    /**
     * Get correlation data for multiple symbols
     */
    @Query("SELECT c1.timestamp, c1.close as price1, c2.close as price2 " +
           "FROM ChartData c1, ChartData c2 " +
           "WHERE c1.symbol = :symbol1 AND c2.symbol = :symbol2 " +
           "AND c1.timeframe = :timeframe AND c2.timeframe = :timeframe " +
           "AND c1.timestamp = c2.timestamp " +
           "AND c1.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c1.timestamp ASC")
    List<Object[]> findCorrelationData(
        @Param("symbol1") String symbol1,
        @Param("symbol2") String symbol2,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get data count for a symbol and timeframe
     */
    @Query("SELECT COUNT(c) FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime")
    Long countDataPoints(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get available date range for a symbol
     */
    @Query("SELECT MIN(c.timestamp) as earliest, MAX(c.timestamp) as latest " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe")
    List<Object[]> getDataRange(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe);
    
    /**
     * Check if data exists for specific timestamp
     */
    @Query("SELECT COUNT(c) > 0 FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.timestamp = :timestamp")
    Boolean dataExistsForTimestamp(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("timestamp") Instant timestamp);
    
    /**
     * Get symbols with data in date range
     */
    @Query("SELECT DISTINCT c.symbol FROM ChartData c " +
           "WHERE c.timeframe = :timeframe " +
           "AND c.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY c.symbol")
    List<String> findSymbolsWithData(
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);
    
    /**
     * Get incomplete data points (for data quality checks)
     */
    @Query("SELECT c FROM ChartData c WHERE c.symbol = :symbol " +
           "AND c.timeframe = :timeframe " +
           "AND c.isComplete = false " +
           "ORDER BY c.timestamp ASC")
    List<ChartData> findIncompleteData(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe);
    
    /**
     * Update indicators for existing data point
     */
    @Modifying
    @Query("UPDATE ChartData c SET " +
           "c.sma20 = :sma20, c.sma50 = :sma50, c.ema12 = :ema12, c.ema26 = :ema26, " +
           "c.rsi = :rsi, c.macd = :macd, c.macdSignal = :macdSignal, " +
           "c.bollingerUpper = :bollingerUpper, c.bollingerMiddle = :bollingerMiddle, c.bollingerLower = :bollingerLower, " +
           "c.atr = :atr, c.obv = :obv " +
           "WHERE c.id = :id")
    int updateTechnicalIndicators(
        @Param("id") Long id,
        @Param("sma20") BigDecimal sma20,
        @Param("sma50") BigDecimal sma50,
        @Param("ema12") BigDecimal ema12,
        @Param("ema26") BigDecimal ema26,
        @Param("rsi") BigDecimal rsi,
        @Param("macd") BigDecimal macd,
        @Param("macdSignal") BigDecimal macdSignal,
        @Param("bollingerUpper") BigDecimal bollingerUpper,
        @Param("bollingerMiddle") BigDecimal bollingerMiddle,
        @Param("bollingerLower") BigDecimal bollingerLower,
        @Param("atr") BigDecimal atr,
        @Param("obv") BigDecimal obv);
    
    /**
     * Mark data as complete
     */
    @Modifying
    @Query("UPDATE ChartData c SET c.isComplete = true " +
           "WHERE c.symbol = :symbol AND c.timeframe = :timeframe " +
           "AND c.timestamp = :timestamp")
    int markDataComplete(
        @Param("symbol") String symbol,
        @Param("timeframe") ChartData.Timeframe timeframe,
        @Param("timestamp") Instant timestamp);
    
    /**
     * Delete old data beyond retention period
     */
    @Modifying
    @Query("DELETE FROM ChartData c WHERE c.timestamp < :cutoffTime " +
           "AND c.timeframe IN :timeframes")
    int deleteOldData(
        @Param("cutoffTime") Instant cutoffTime,
        @Param("timeframes") List<ChartData.Timeframe> timeframes);
    
    /**
     * Get data quality metrics
     */
    @Query("SELECT " +
           "c.timeframe, " +
           "COUNT(c) as totalCount, " +
           "COUNT(CASE WHEN c.isComplete THEN 1 END) as completeCount, " +
           "COUNT(CASE WHEN c.hasGaps THEN 1 END) as gapCount, " +
           "MIN(c.timestamp) as earliestData, " +
           "MAX(c.timestamp) as latestData " +
           "FROM ChartData c WHERE c.symbol = :symbol " +
           "GROUP BY c.timeframe " +
           "ORDER BY c.timeframe")
    List<Object[]> getDataQualityMetrics(@Param("symbol") String symbol);
}