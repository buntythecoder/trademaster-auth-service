package com.trademaster.marketdata.service;

import com.trademaster.marketdata.dto.OHLCVData;
import com.trademaster.marketdata.entity.ChartData;
import com.trademaster.marketdata.repository.ChartDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

/**
 * Charting Service
 * 
 * Comprehensive service for charting data with advanced analytics,
 * technical indicators, and performance optimization.
 * 
 * Features:
 * - Multi-timeframe OHLCV data retrieval
 * - Technical indicator calculations and caching
 * - Candlestick pattern recognition
 * - Volume analysis and market microstructure
 * - Support/resistance level detection
 * - Data quality monitoring and gap detection
 * - High-performance queries with caching
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartingService {
    
    private final ChartDataRepository chartDataRepository;
    
    /**
     * Get OHLCV data for charting
     */
    @Cacheable(value = "chart-ohlcv", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public List<OHLCVData> getOHLCVData(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Getting OHLCV data for symbol: {} timeframe: {} range: {} - {}", 
            symbol, timeframe, startTime, endTime);
        
        try {
            var chartDataList = chartDataRepository.findChartData(symbol, timeframe, startTime, endTime);
            
            return chartDataList.stream()
                .map(data -> OHLCVData.builder()
                    .timestamp(data.getTimestamp())
                    .open(data.getOpen())
                    .high(data.getHigh())
                    .low(data.getLow())
                    .close(data.getClose())
                    .volume(data.getVolume())
                    .build())
                .toList();
                
        } catch (Exception e) {
            log.error("Error getting OHLCV data for symbol: " + symbol, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get complete chart data with technical indicators
     */
    @Cacheable(value = "chart-complete", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public List<ChartData> getCompleteChartData(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Getting complete chart data for symbol: {} timeframe: {} range: {} - {}", 
            symbol, timeframe, startTime, endTime);
        
        return chartDataRepository.findChartData(symbol, timeframe, startTime, endTime);
    }
    
    /**
     * Get chart data with pagination
     */
    public Page<ChartData> getChartDataPaged(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime, Pageable pageable) {
        
        return chartDataRepository.findChartData(symbol, timeframe, startTime, endTime, pageable);
    }
    
    /**
     * Get technical indicators data
     */
    @Cacheable(value = "chart-indicators", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public Map<String, List<IndicatorPoint>> getTechnicalIndicators(String symbol, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Getting technical indicators for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var indicatorData = chartDataRepository.findTechnicalIndicators(
                symbol, timeframe, startTime, endTime);
            
            var indicators = new HashMap<String, List<IndicatorPoint>>();
            
            // Initialize lists
            indicators.put("SMA20", new ArrayList<>());
            indicators.put("SMA50", new ArrayList<>());
            indicators.put("EMA12", new ArrayList<>());
            indicators.put("EMA26", new ArrayList<>());
            indicators.put("RSI", new ArrayList<>());
            indicators.put("MACD", new ArrayList<>());
            indicators.put("MACD_SIGNAL", new ArrayList<>());
            indicators.put("BOLLINGER_UPPER", new ArrayList<>());
            indicators.put("BOLLINGER_MIDDLE", new ArrayList<>());
            indicators.put("BOLLINGER_LOWER", new ArrayList<>());
            
            // Process data
            for (var row : indicatorData) {
                var timestamp = (Instant) row[0];
                var close = (BigDecimal) row[1];
                
                addIndicatorPoint(indicators, "SMA20", timestamp, (BigDecimal) row[2]);
                addIndicatorPoint(indicators, "SMA50", timestamp, (BigDecimal) row[3]);
                addIndicatorPoint(indicators, "EMA12", timestamp, (BigDecimal) row[4]);
                addIndicatorPoint(indicators, "EMA26", timestamp, (BigDecimal) row[5]);
                addIndicatorPoint(indicators, "RSI", timestamp, (BigDecimal) row[6]);
                addIndicatorPoint(indicators, "MACD", timestamp, (BigDecimal) row[7]);
                addIndicatorPoint(indicators, "MACD_SIGNAL", timestamp, (BigDecimal) row[8]);
                addIndicatorPoint(indicators, "BOLLINGER_UPPER", timestamp, (BigDecimal) row[9]);
                addIndicatorPoint(indicators, "BOLLINGER_MIDDLE", timestamp, (BigDecimal) row[10]);
                addIndicatorPoint(indicators, "BOLLINGER_LOWER", timestamp, (BigDecimal) row[11]);
            }
            
            return indicators;
            
        } catch (Exception e) {
            log.error("Error getting technical indicators for symbol: " + symbol, e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get volume analysis data
     */
    @Cacheable(value = "chart-volume", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public VolumeAnalysis getVolumeAnalysis(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Getting volume analysis for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var volumeData = chartDataRepository.findVolumeData(symbol, timeframe, startTime, endTime);
            
            var volumePoints = new ArrayList<VolumePoint>();
            var totalVolume = 0L;
            var volumeWeightedPrice = BigDecimal.ZERO;
            var maxVolume = 0L;
            var avgVolumeSum = 0L;
            
            for (var row : volumeData) {
                var timestamp = (Instant) row[0];
                var close = (BigDecimal) row[1];
                var volume = (Long) row[2];
                var vwap = (BigDecimal) row[3];
                var obv = (BigDecimal) row[4];
                
                volumePoints.add(VolumePoint.builder()
                    .timestamp(timestamp)
                    .price(close)
                    .volume(volume)
                    .vwap(vwap)
                    .obv(obv)
                    .build());
                
                totalVolume += volume;
                volumeWeightedPrice = volumeWeightedPrice.add(close.multiply(BigDecimal.valueOf(volume)));
                maxVolume = Math.max(maxVolume, volume);
                avgVolumeSum += volume;
            }
            
            var averageVolume = volumePoints.isEmpty() ? 0L : avgVolumeSum / volumePoints.size();
            var vwapOverall = totalVolume > 0 ? 
                volumeWeightedPrice.divide(BigDecimal.valueOf(totalVolume), 6, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;
            
            return VolumeAnalysis.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .startTime(startTime)
                .endTime(endTime)
                .volumePoints(volumePoints)
                .totalVolume(totalVolume)
                .averageVolume(averageVolume)
                .maxVolume(maxVolume)
                .volumeWeightedAveragePrice(vwapOverall)
                .build();
            
        } catch (Exception e) {
            log.error("Error getting volume analysis for symbol: " + symbol, e);
            return VolumeAnalysis.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .volumePoints(Collections.emptyList())
                .build();
        }
    }
    
    /**
     * Get candlestick patterns
     */
    @Cacheable(value = "chart-patterns", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public List<CandlestickPattern> getCandlestickPatterns(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Analyzing candlestick patterns for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var chartData = chartDataRepository.findCandlestickData(symbol, timeframe, startTime, endTime);
            var patterns = new ArrayList<CandlestickPattern>();
            
            for (int i = 0; i < chartData.size(); i++) {
                var candle = chartData.get(i);
                var detectedPatterns = new ArrayList<String>();
                
                // Single candle patterns
                if (candle.isDoji()) {
                    detectedPatterns.add("DOJI");
                }
                if (candle.isHammer()) {
                    detectedPatterns.add("HAMMER");
                }
                if (candle.isShootingStar()) {
                    detectedPatterns.add("SHOOTING_STAR");
                }
                
                // Multi-candle patterns (require previous candles)
                if (i > 0) {
                    var prevCandle = chartData.get(i - 1);
                    
                    // Bullish engulfing
                    if (isBullishEngulfing(prevCandle, candle)) {
                        detectedPatterns.add("BULLISH_ENGULFING");
                    }
                    
                    // Bearish engulfing
                    if (isBearishEngulfing(prevCandle, candle)) {
                        detectedPatterns.add("BEARISH_ENGULFING");
                    }
                }
                
                // Three-candle patterns
                if (i > 1) {
                    var candle1 = chartData.get(i - 2);
                    var candle2 = chartData.get(i - 1);
                    var candle3 = candle;
                    
                    // Morning star
                    if (isMorningStar(candle1, candle2, candle3)) {
                        detectedPatterns.add("MORNING_STAR");
                    }
                    
                    // Evening star
                    if (isEveningStar(candle1, candle2, candle3)) {
                        detectedPatterns.add("EVENING_STAR");
                    }
                }
                
                if (!detectedPatterns.isEmpty()) {
                    patterns.add(CandlestickPattern.builder()
                        .timestamp(candle.getTimestamp())
                        .patterns(detectedPatterns)
                        .confidence(calculatePatternConfidence(detectedPatterns, candle))
                        .price(candle.getClose())
                        .build());
                }
            }
            
            return patterns;
            
        } catch (Exception e) {
            log.error("Error analyzing candlestick patterns for symbol: " + symbol, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get support and resistance levels
     */
    @Cacheable(value = "chart-levels", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public SupportResistanceLevels getSupportResistanceLevels(String symbol, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Calculating support/resistance levels for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var levels = chartDataRepository.findSupportResistanceLevels(
                symbol, timeframe, startTime, endTime, 3); // Minimum 3 touches
            
            var supportLevels = new ArrayList<PriceLevel>();
            var resistanceLevels = new ArrayList<PriceLevel>();
            
            var chartData = chartDataRepository.findChartData(symbol, timeframe, startTime, endTime);
            var currentPrice = chartData.isEmpty() ? BigDecimal.ZERO : chartData.get(chartData.size() - 1).getClose();
            
            for (var level : levels) {
                var price = (BigDecimal) level[0];
                var touchCount = ((Number) level[1]).intValue();
                
                var priceLevel = PriceLevel.builder()
                    .price(price)
                    .touchCount(touchCount)
                    .strength(calculateLevelStrength(touchCount, price, currentPrice))
                    .build();
                
                if (price.compareTo(currentPrice) < 0) {
                    supportLevels.add(priceLevel);
                } else {
                    resistanceLevels.add(priceLevel);
                }
            }
            
            // Sort by strength (descending)
            supportLevels.sort((a, b) -> b.getStrength().compareTo(a.getStrength()));
            resistanceLevels.sort((a, b) -> b.getStrength().compareTo(a.getStrength()));
            
            return SupportResistanceLevels.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .currentPrice(currentPrice)
                .supportLevels(supportLevels.stream().limit(5).toList()) // Top 5
                .resistanceLevels(resistanceLevels.stream().limit(5).toList()) // Top 5
                .calculatedAt(Instant.now())
                .build();
            
        } catch (Exception e) {
            log.error("Error calculating support/resistance levels for symbol: " + symbol, e);
            return SupportResistanceLevels.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .supportLevels(Collections.emptyList())
                .resistanceLevels(Collections.emptyList())
                .build();
        }
    }
    
    /**
     * Get chart data for multiple symbols (for correlation analysis)
     */
    public Map<String, List<OHLCVData>> getMultiSymbolData(List<String> symbols, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Getting multi-symbol data for {} symbols, timeframe: {}", symbols.size(), timeframe);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            var futures = new HashMap<String, CompletableFuture<List<OHLCVData>>>();
            
            // Launch parallel requests for each symbol
            for (var symbol : symbols) {
                futures.put(symbol, 
                    CompletableFuture.supplyAsync(() -> getOHLCVData(symbol, timeframe, startTime, endTime)));
            }
            
            // Collect results
            var results = new HashMap<String, List<OHLCVData>>();
            for (var entry : futures.entrySet()) {
                try {
                    results.put(entry.getKey(), entry.getValue().get());
                } catch (Exception e) {
                    log.error("Error getting data for symbol: " + entry.getKey(), e);
                    results.put(entry.getKey(), Collections.emptyList());
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("Error getting multi-symbol data", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get data quality report
     */
    public DataQualityReport getDataQualityReport(String symbol) {
        log.debug("Generating data quality report for symbol: {}", symbol);
        
        try {
            var qualityMetrics = chartDataRepository.getDataQualityMetrics(symbol);
            var timeframeMetrics = new HashMap<ChartData.Timeframe, DataQualityMetric>();
            
            for (var metric : qualityMetrics) {
                var timeframe = (ChartData.Timeframe) metric[0];
                var totalCount = ((Number) metric[1]).longValue();
                var completeCount = ((Number) metric[2]).longValue();
                var gapCount = ((Number) metric[3]).longValue();
                var earliestData = (Instant) metric[4];
                var latestData = (Instant) metric[5];
                
                var completeness = totalCount > 0 ? 
                    BigDecimal.valueOf(completeCount * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
                
                timeframeMetrics.put(timeframe, DataQualityMetric.builder()
                    .timeframe(timeframe)
                    .totalDataPoints(totalCount)
                    .completeDataPoints(completeCount)
                    .gapCount(gapCount)
                    .completenessPercent(completeness)
                    .earliestData(earliestData)
                    .latestData(latestData)
                    .build());
            }
            
            return DataQualityReport.builder()
                .symbol(symbol)
                .generatedAt(Instant.now())
                .timeframeMetrics(timeframeMetrics)
                .overallScore(calculateOverallQualityScore(timeframeMetrics))
                .build();
            
        } catch (Exception e) {
            log.error("Error generating data quality report for symbol: " + symbol, e);
            return DataQualityReport.builder()
                .symbol(symbol)
                .generatedAt(Instant.now())
                .timeframeMetrics(Collections.emptyMap())
                .overallScore(BigDecimal.ZERO)
                .build();
        }
    }
    
    /**
     * Get aggregated statistics for a period
     */
    public PeriodStatistics getPeriodStatistics(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Calculating period statistics for symbol: {} timeframe: {} period: {} - {}", 
            symbol, timeframe, startTime, endTime);
        
        try {
            var stats = chartDataRepository.getAggregateStatistics(symbol, timeframe, startTime, endTime);
            
            if (stats.isEmpty()) {
                return PeriodStatistics.builder()
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .build();
            }
            
            var row = stats.get(0);
            var periodLow = (BigDecimal) row[0];
            var periodHigh = (BigDecimal) row[1];
            var totalVolume = ((Number) row[2]).longValue();
            var avgPrice = (BigDecimal) row[3];
            var dataPoints = ((Number) row[4]).longValue();
            
            // Get first and last prices for return calculation
            var chartData = chartDataRepository.findChartData(symbol, timeframe, startTime, endTime);
            var openPrice = chartData.isEmpty() ? BigDecimal.ZERO : chartData.get(0).getOpen();
            var closePrice = chartData.isEmpty() ? BigDecimal.ZERO : chartData.get(chartData.size() - 1).getClose();
            
            var totalReturn = openPrice.compareTo(BigDecimal.ZERO) != 0 ?
                closePrice.subtract(openPrice).divide(openPrice, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;
            
            var volatility = calculateVolatility(chartData);
            
            return PeriodStatistics.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .startTime(startTime)
                .endTime(endTime)
                .openPrice(openPrice)
                .closePrice(closePrice)
                .highPrice(periodHigh)
                .lowPrice(periodLow)
                .averagePrice(avgPrice)
                .totalVolume(totalVolume)
                .averageVolume(dataPoints > 0 ? totalVolume / dataPoints : 0L)
                .totalReturn(totalReturn)
                .volatility(volatility)
                .dataPoints(dataPoints)
                .build();
            
        } catch (Exception e) {
            log.error("Error calculating period statistics for symbol: " + symbol, e);
            return PeriodStatistics.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .build();
        }
    }
    
    // Private helper methods
    
    private void addIndicatorPoint(Map<String, List<IndicatorPoint>> indicators, 
            String name, Instant timestamp, BigDecimal value) {
        if (value != null) {
            indicators.get(name).add(IndicatorPoint.builder()
                .timestamp(timestamp)
                .value(value)
                .build());
        }
    }
    
    private boolean isBullishEngulfing(ChartData prev, ChartData current) {
        return prev.isBearish() && current.isBullish() &&
               current.getOpen().compareTo(prev.getClose()) < 0 &&
               current.getClose().compareTo(prev.getOpen()) > 0;
    }
    
    private boolean isBearishEngulfing(ChartData prev, ChartData current) {
        return prev.isBullish() && current.isBearish() &&
               current.getOpen().compareTo(prev.getClose()) > 0 &&
               current.getClose().compareTo(prev.getOpen()) < 0;
    }
    
    private boolean isMorningStar(ChartData c1, ChartData c2, ChartData c3) {
        return c1.isBearish() && c2.isDoji() && c3.isBullish() &&
               c2.getHigh().compareTo(c1.getLow().min(c3.getLow())) < 0;
    }
    
    private boolean isEveningStar(ChartData c1, ChartData c2, ChartData c3) {
        return c1.isBullish() && c2.isDoji() && c3.isBearish() &&
               c2.getLow().compareTo(c1.getHigh().max(c3.getHigh())) > 0;
    }
    
    private BigDecimal calculatePatternConfidence(List<String> patterns, ChartData candle) {
        // Simple confidence calculation based on volume and pattern type
        var baseConfidence = patterns.contains("DOJI") ? 60 : 
                           patterns.contains("HAMMER") ? 75 :
                           patterns.contains("ENGULFING") ? 85 : 70;
        
        // Adjust for volume (high volume increases confidence)
        // This would require average volume calculation
        return BigDecimal.valueOf(baseConfidence);
    }
    
    private BigDecimal calculateLevelStrength(int touchCount, BigDecimal price, BigDecimal currentPrice) {
        var distanceWeight = currentPrice.compareTo(BigDecimal.ZERO) != 0 ?
            BigDecimal.valueOf(100).subtract(
                price.subtract(currentPrice).abs()
                    .divide(currentPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            ).max(BigDecimal.ZERO) :
            BigDecimal.valueOf(50);
        
        var touchWeight = BigDecimal.valueOf(touchCount * 20);
        
        return distanceWeight.multiply(BigDecimal.valueOf(0.7))
            .add(touchWeight.multiply(BigDecimal.valueOf(0.3)))
            .min(BigDecimal.valueOf(100));
    }
    
    private BigDecimal calculateOverallQualityScore(Map<ChartData.Timeframe, DataQualityMetric> metrics) {
        if (metrics.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        var totalScore = metrics.values().stream()
            .map(DataQualityMetric::getCompletenessPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalScore.divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateVolatility(List<ChartData> data) {
        if (data.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        var returns = new ArrayList<BigDecimal>();
        for (int i = 1; i < data.size(); i++) {
            var prevPrice = data.get(i - 1).getClose();
            var currentPrice = data.get(i).getClose();
            
            if (prevPrice.compareTo(BigDecimal.ZERO) != 0) {
                var return_ = currentPrice.divide(prevPrice, 6, RoundingMode.HALF_UP)
                    .subtract(BigDecimal.ONE);
                returns.add(return_);
            }
        }
        
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calculate standard deviation of returns
        var mean = returns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        
        var variance = returns.stream()
            .map(ret -> ret.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        
        // Approximate square root for volatility
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()) * 100).setScale(4, RoundingMode.HALF_UP);
    }
    
    // Data classes for responses
    
    @lombok.Builder
    public record IndicatorPoint(
        Instant timestamp,
        BigDecimal value
    ) {}
    
    @lombok.Builder
    public record VolumePoint(
        Instant timestamp,
        BigDecimal price,
        Long volume,
        BigDecimal vwap,
        BigDecimal obv
    ) {}
    
    @lombok.Builder
    public record VolumeAnalysis(
        String symbol,
        ChartData.Timeframe timeframe,
        Instant startTime,
        Instant endTime,
        List<VolumePoint> volumePoints,
        Long totalVolume,
        Long averageVolume,
        Long maxVolume,
        BigDecimal volumeWeightedAveragePrice
    ) {}
    
    @lombok.Builder
    public record CandlestickPattern(
        Instant timestamp,
        List<String> patterns,
        BigDecimal confidence,
        BigDecimal price
    ) {}
    
    @lombok.Builder
    public record PriceLevel(
        BigDecimal price,
        Integer touchCount,
        BigDecimal strength
    ) {}
    
    @lombok.Builder
    public record SupportResistanceLevels(
        String symbol,
        ChartData.Timeframe timeframe,
        BigDecimal currentPrice,
        List<PriceLevel> supportLevels,
        List<PriceLevel> resistanceLevels,
        Instant calculatedAt
    ) {}
    
    @lombok.Builder
    public record DataQualityMetric(
        ChartData.Timeframe timeframe,
        Long totalDataPoints,
        Long completeDataPoints,
        Long gapCount,
        BigDecimal completenessPercent,
        Instant earliestData,
        Instant latestData
    ) {}
    
    @lombok.Builder
    public record DataQualityReport(
        String symbol,
        Instant generatedAt,
        Map<ChartData.Timeframe, DataQualityMetric> timeframeMetrics,
        BigDecimal overallScore
    ) {}
    
    @lombok.Builder
    public record PeriodStatistics(
        String symbol,
        ChartData.Timeframe timeframe,
        Instant startTime,
        Instant endTime,
        BigDecimal openPrice,
        BigDecimal closePrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal averagePrice,
        Long totalVolume,
        Long averageVolume,
        BigDecimal totalReturn,
        BigDecimal volatility,
        Long dataPoints
    ) {}
}