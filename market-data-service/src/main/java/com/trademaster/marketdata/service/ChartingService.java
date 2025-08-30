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
import java.util.function.*;
import java.util.stream.*;

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
     * Get technical indicators data - Functional approach
     */
    @Cacheable(value = "chart-indicators", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public Map<String, List<IndicatorPoint>> getTechnicalIndicators(String symbol, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Getting technical indicators for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var indicatorData = chartDataRepository.findTechnicalIndicators(
                symbol, timeframe, startTime, endTime);
            
            return processIndicatorData(indicatorData);
            
        } catch (Exception e) {
            log.error("Error getting technical indicators for symbol: " + symbol, e);
            return Collections.emptyMap();
        }
    }
    
    private Map<String, List<IndicatorPoint>> processIndicatorData(List<Object[]> indicatorData) {
        String[] indicatorNames = {"SMA20", "SMA50", "EMA12", "EMA26", "RSI", "MACD", "MACD_SIGNAL", 
                                  "BOLLINGER_UPPER", "BOLLINGER_MIDDLE", "BOLLINGER_LOWER"};
        
        Map<String, List<IndicatorPoint>> indicators = Arrays.stream(indicatorNames)
            .collect(Collectors.toMap(Function.identity(), name -> new ArrayList<>()));
        
        indicatorData.forEach(row -> {
            var timestamp = (Instant) row[0];
            IntStream.range(2, Math.min(row.length, 12))
                .filter(i -> row[i] != null)
                .forEach(i -> 
                    indicators.get(indicatorNames[i - 2]).add(
                        IndicatorPoint.builder()
                            .timestamp(timestamp)
                            .value((BigDecimal) row[i])
                            .build()
                    )
                );
        });
        
        return indicators;
    }
    
    /**
     * Get volume analysis data - Functional approach
     */
    @Cacheable(value = "chart-volume", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public VolumeAnalysis getVolumeAnalysis(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Getting volume analysis for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var volumeData = chartDataRepository.findVolumeData(symbol, timeframe, startTime, endTime);
            
            return processVolumeAnalysis(volumeData, symbol, timeframe, startTime, endTime);
            
        } catch (Exception e) {
            log.error("Error getting volume analysis for symbol: " + symbol, e);
            return VolumeAnalysis.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .volumePoints(Collections.emptyList())
                .build();
        }
    }
    
    private VolumeAnalysis processVolumeAnalysis(List<Object[]> volumeData, String symbol, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        List<VolumePoint> volumePoints = volumeData.stream()
            .map(row -> VolumePoint.builder()
                .timestamp((Instant) row[0])
                .price((BigDecimal) row[1])
                .volume((Long) row[2])
                .vwap((BigDecimal) row[3])
                .obv((BigDecimal) row[4])
                .build())
            .toList();
        
        record VolumeStats(long totalVolume, BigDecimal weightedPrice, long maxVolume) {}
        
        // Calculate volume statistics functionally
        long totalVolume = volumeData.stream()
            .mapToLong(row -> (Long) row[2])
            .sum();
            
        BigDecimal weightedPrice = volumeData.stream()
            .map(row -> ((BigDecimal) row[1]).multiply(BigDecimal.valueOf((Long) row[2])))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        long maxVolume = volumeData.stream()
            .mapToLong(row -> (Long) row[2])
            .max()
            .orElse(0L);
            
        VolumeStats stats = new VolumeStats(totalVolume, weightedPrice, maxVolume);
        
        long averageVolume = volumePoints.isEmpty() ? 0L : stats.totalVolume / volumePoints.size();
        BigDecimal vwapOverall = stats.totalVolume > 0 ? 
            stats.weightedPrice.divide(BigDecimal.valueOf(stats.totalVolume), 6, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        return VolumeAnalysis.builder()
            .symbol(symbol)
            .timeframe(timeframe)
            .startTime(startTime)
            .endTime(endTime)
            .volumePoints(volumePoints)
            .totalVolume(stats.totalVolume)
            .averageVolume(averageVolume)
            .maxVolume(stats.maxVolume)
            .volumeWeightedAveragePrice(vwapOverall)
            .build();
    }
    
    /**
     * Get candlestick patterns - Functional approach
     */
    @Cacheable(value = "chart-patterns", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public List<CandlestickPattern> getCandlestickPatterns(String symbol, ChartData.Timeframe timeframe,
            Instant startTime, Instant endTime) {
        
        log.debug("Analyzing candlestick patterns for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var chartData = chartDataRepository.findCandlestickData(symbol, timeframe, startTime, endTime);
            
            return IntStream.range(0, chartData.size())
                .mapToObj(i -> analyzeCandlePatterns(chartData, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            
        } catch (Exception e) {
            log.error("Error analyzing candlestick patterns for symbol: " + symbol, e);
            return Collections.emptyList();
        }
    }
    
    private Optional<CandlestickPattern> analyzeCandlePatterns(List<ChartData> chartData, int index) {
        ChartData candle = chartData.get(index);
        
        List<String> detectedPatterns = Stream.<Stream<String>>of(
                detectSingleCandlePatterns(candle),
                index > 0 ? detectTwoCandlePatterns(chartData.get(index - 1), candle) : Stream.<String>empty(),
                index > 1 ? detectThreeCandlePatterns(
                    chartData.get(index - 2), chartData.get(index - 1), candle) : Stream.<String>empty()
            )
            .flatMap(Function.identity())
            .toList();
        
        return detectedPatterns.isEmpty() ? Optional.empty() :
            Optional.of(CandlestickPattern.builder()
                .timestamp(candle.getTimestamp())
                .patterns(detectedPatterns)
                .confidence(calculatePatternConfidence(detectedPatterns, candle))
                .price(candle.getClose())
                .build());
    }
    
    private Stream<String> detectSingleCandlePatterns(ChartData candle) {
        List<String> patterns = new ArrayList<>();
        if (candle.isDoji()) patterns.add("DOJI");
        if (candle.isHammer()) patterns.add("HAMMER");
        if (candle.isShootingStar()) patterns.add("SHOOTING_STAR");
        return patterns.stream();
    }
    
    private Stream<String> detectTwoCandlePatterns(ChartData prev, ChartData current) {
        List<String> patterns = new ArrayList<>();
        if (isBullishEngulfing(prev, current)) patterns.add("BULLISH_ENGULFING");
        if (isBearishEngulfing(prev, current)) patterns.add("BEARISH_ENGULFING");
        return patterns.stream();
    }
    
    private Stream<String> detectThreeCandlePatterns(ChartData c1, ChartData c2, ChartData c3) {
        List<String> patterns = new ArrayList<>();
        if (isMorningStar(c1, c2, c3)) patterns.add("MORNING_STAR");
        if (isEveningStar(c1, c2, c3)) patterns.add("EVENING_STAR");
        return patterns.stream();
    }
    
    /**
     * Get support and resistance levels - Functional approach
     */
    @Cacheable(value = "chart-levels", key = "#symbol + '_' + #timeframe + '_' + #startTime + '_' + #endTime")
    public SupportResistanceLevels getSupportResistanceLevels(String symbol, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Calculating support/resistance levels for symbol: {} timeframe: {}", symbol, timeframe);
        
        try {
            var levels = chartDataRepository.findSupportResistanceLevels(
                symbol, timeframe, startTime, endTime, 3); // Minimum 3 touches
            
            var chartData = chartDataRepository.findChartData(symbol, timeframe, startTime, endTime);
            BigDecimal currentPrice = chartData.isEmpty() ? BigDecimal.ZERO : 
                chartData.get(chartData.size() - 1).getClose();
            
            return processSupportResistanceLevels(levels, currentPrice, symbol, timeframe);
            
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
    
    private SupportResistanceLevels processSupportResistanceLevels(List<Object[]> levels, 
            BigDecimal currentPrice, String symbol, ChartData.Timeframe timeframe) {
        
        Map<Boolean, List<PriceLevel>> levelsByType = levels.stream()
            .map(level -> {
                BigDecimal price = (BigDecimal) level[0];
                int touchCount = ((Number) level[1]).intValue();
                
                return PriceLevel.builder()
                    .price(price)
                    .touchCount(touchCount)
                    .strength(calculateLevelStrength(touchCount, price, currentPrice))
                    .build();
            })
            .collect(Collectors.partitioningBy(priceLevel -> 
                priceLevel.price().compareTo(currentPrice) < 0));
        
        List<PriceLevel> supportLevels = levelsByType.get(true).stream()
            .sorted((a, b) -> b.strength().compareTo(a.strength()))
            .limit(5)
            .toList();
        
        List<PriceLevel> resistanceLevels = levelsByType.get(false).stream()
            .sorted((a, b) -> b.strength().compareTo(a.strength()))
            .limit(5)
            .toList();
        
        return SupportResistanceLevels.builder()
            .symbol(symbol)
            .timeframe(timeframe)
            .currentPrice(currentPrice)
            .supportLevels(supportLevels)
            .resistanceLevels(resistanceLevels)
            .calculatedAt(Instant.now())
            .build();
    }
    
    /**
     * Get chart data for multiple symbols (for correlation analysis) - Functional approach
     */
    public Map<String, List<OHLCVData>> getMultiSymbolData(List<String> symbols, 
            ChartData.Timeframe timeframe, Instant startTime, Instant endTime) {
        
        log.debug("Getting multi-symbol data for {} symbols, timeframe: {}", symbols.size(), timeframe);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            Map<String, CompletableFuture<List<OHLCVData>>> futures = symbols.stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    symbol -> CompletableFuture.supplyAsync(() -> 
                        getOHLCVData(symbol, timeframe, startTime, endTime))
                ));
            
            return futures.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        try {
                            return entry.getValue().get();
                        } catch (Exception e) {
                            log.error("Error getting data for symbol: " + entry.getKey(), e);
                            return Collections.<OHLCVData>emptyList();
                        }
                    }
                ));
            
        } catch (Exception e) {
            log.error("Error getting multi-symbol data", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get data quality report - Functional approach
     */
    public DataQualityReport getDataQualityReport(String symbol) {
        log.debug("Generating data quality report for symbol: {}", symbol);
        
        try {
            var qualityMetrics = chartDataRepository.getDataQualityMetrics(symbol);
            
            Map<ChartData.Timeframe, DataQualityMetric> timeframeMetrics = qualityMetrics.stream()
                .collect(Collectors.toMap(
                    metric -> (ChartData.Timeframe) metric[0],
                    metric -> {
                        var totalCount = ((Number) metric[1]).longValue();
                        var completeCount = ((Number) metric[2]).longValue();
                        var gapCount = ((Number) metric[3]).longValue();
                        var earliestData = (Instant) metric[4];
                        var latestData = (Instant) metric[5];
                        
                        var completeness = totalCount > 0 ? 
                            BigDecimal.valueOf(completeCount * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP) : 
                            BigDecimal.ZERO;
                        
                        return DataQualityMetric.builder()
                            .timeframe((ChartData.Timeframe) metric[0])
                            .totalDataPoints(totalCount)
                            .completeDataPoints(completeCount)
                            .gapCount(gapCount)
                            .completenessPercent(completeness)
                            .earliestData(earliestData)
                            .latestData(latestData)
                            .build();
                    }
                ));
            
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
            .map(DataQualityMetric::completenessPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalScore.divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateVolatility(List<ChartData> data) {
        return data.size() < 2 ? BigDecimal.ZERO :
            IntStream.range(1, data.size())
                .mapToObj(i -> calculateReturn(data.get(i - 1).getClose(), data.get(i).getClose()))
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    returns -> returns.isEmpty() ? BigDecimal.ZERO : computeVolatility(returns)
                ));
    }
    
    private BigDecimal calculateReturn(BigDecimal prevPrice, BigDecimal currentPrice) {
        return prevPrice.compareTo(BigDecimal.ZERO) == 0 ? null :
            currentPrice.divide(prevPrice, 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
    }
    
    private BigDecimal computeVolatility(List<BigDecimal> returns) {
        BigDecimal mean = returns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        
        BigDecimal variance = returns.stream()
            .map(ret -> ret.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
        
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