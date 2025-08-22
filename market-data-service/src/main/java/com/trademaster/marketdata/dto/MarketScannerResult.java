package com.trademaster.marketdata.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Market Scanner Result DTO
 * 
 * Contains scan results with comprehensive market data,
 * technical indicators, and metadata for each symbol.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record MarketScannerResult(
    
    // Scan Metadata
    String scanId,
    Instant scanTime,
    MarketScannerRequest originalRequest,
    ScanStatistics statistics,
    
    // Results
    List<ScanResultItem> results,
    
    // Pagination
    PaginationInfo pagination
    
) {
    
    /**
     * Individual scan result item
     */
    @Builder
    public record ScanResultItem(
        
        // Basic Information
        String symbol,
        String exchange,
        String companyName,
        String sector,
        String industry,
        String marketCap,
        
        // Current Price Data
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayChange,
        BigDecimal dayChangePercent,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        BigDecimal dayOpen,
        
        // Volume Data
        Long currentVolume,
        Long avgVolume,
        BigDecimal volumeRatio, // Current vs average
        BigDecimal volumeSpikeRatio, // vs previous day
        
        // Bid/Ask Data
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal spread,
        BigDecimal spreadPercent,
        Long bidSize,
        Long askSize,
        
        // Performance Metrics
        BigDecimal weekChange,
        BigDecimal weekChangePercent,
        BigDecimal monthChange,
        BigDecimal monthChangePercent,
        BigDecimal threeMonthChange,
        BigDecimal threeMonthChangePercent,
        BigDecimal yearChange,
        BigDecimal yearChangePercent,
        
        // Technical Indicators
        Map<String, BigDecimal> technicalIndicators,
        
        // Volatility Metrics
        BigDecimal volatility,
        BigDecimal beta,
        BigDecimal atr, // Average True Range
        
        // Fundamental Data
        BigDecimal peRatio,
        BigDecimal pegRatio,
        BigDecimal priceToBook,
        BigDecimal priceToSales,
        BigDecimal divYield,
        BigDecimal epsGrowth,
        BigDecimal revenueGrowth,
        BigDecimal profitMargin,
        BigDecimal debtToEquity,
        BigDecimal roe, // Return on Equity
        
        // Pattern Recognition
        List<String> detectedPatterns,
        List<String> candlestickPatterns,
        
        // Support/Resistance Levels
        List<SupportResistanceLevel> supportLevels,
        List<SupportResistanceLevel> resistanceLevels,
        
        // Breakout Analysis
        BreakoutAnalysis breakoutAnalysis,
        
        // Data Quality
        Integer dataQuality, // 0-100 score
        Instant lastUpdated,
        String marketStatus,
        
        // Ranking Score
        BigDecimal scanScore, // Composite score based on filters
        Integer rank
        
    ) {
        
        /**
         * Support/Resistance Level
         */
        @Builder
        public record SupportResistanceLevel(
            BigDecimal level,
            Integer strength, // 1-10 strength score
            Integer touches, // Number of times price touched this level
            Instant lastTouch,
            String timeframe
        ) {}
        
        /**
         * Breakout Analysis
         */
        @Builder
        public record BreakoutAnalysis(
            Boolean volumeBreakout,
            Boolean priceBreakout,
            Boolean resistanceBreakout,
            Boolean supportBreakdown,
            BigDecimal breakoutStrength, // 0-100 score
            BigDecimal breakoutVolume,
            Instant breakoutTime,
            String breakoutType // BULLISH, BEARISH, NEUTRAL
        ) {}
        
        /**
         * Get technical indicator value
         */
        public BigDecimal getTechnicalIndicator(String indicator) {
            return technicalIndicators != null ? technicalIndicators.get(indicator) : null;
        }
        
        /**
         * Check if symbol has strong momentum
         */
        public boolean hasStrongMomentum() {
            return dayChangePercent != null && dayChangePercent.compareTo(new BigDecimal("3.0")) > 0 &&
                   volumeRatio != null && volumeRatio.compareTo(new BigDecimal("1.5")) > 0;
        }
        
        /**
         * Check if symbol is near breakout
         */
        public boolean isNearBreakout() {
            return breakoutAnalysis != null && (
                Boolean.TRUE.equals(breakoutAnalysis.priceBreakout()) ||
                Boolean.TRUE.equals(breakoutAnalysis.volumeBreakout()) ||
                Boolean.TRUE.equals(breakoutAnalysis.resistanceBreakout())
            );
        }
        
        /**
         * Get overall quality score
         */
        public String getQualityGrade() {
            if (dataQuality == null) return "UNKNOWN";
            return switch (dataQuality / 10) {
                case 10, 9 -> "A";
                case 8 -> "B";
                case 7 -> "C";
                case 6 -> "D";
                default -> "F";
            };
        }
        
        /**
         * Check if symbol meets quality threshold
         */
        public boolean meetsQualityThreshold(int threshold) {
            return dataQuality != null && dataQuality >= threshold;
        }
        
        /**
         * Get market cap category
         */
        public String getMarketCapCategory() {
            if (marketCap == null) return "UNKNOWN";
            return switch (marketCap.toUpperCase()) {
                case "LARGE_CAP", "LARGE" -> "Large Cap";
                case "MID_CAP", "MID" -> "Mid Cap";
                case "SMALL_CAP", "SMALL" -> "Small Cap";
                case "MICRO_CAP", "MICRO" -> "Micro Cap";
                default -> marketCap;
            };
        }
        
        /**
         * Calculate momentum score
         */
        public BigDecimal getMomentumScore() {
            BigDecimal score = BigDecimal.ZERO;
            
            // Day change contribution (40%)
            if (dayChangePercent != null) {
                score = score.add(dayChangePercent.multiply(new BigDecimal("0.4")));
            }
            
            // Volume ratio contribution (30%)
            if (volumeRatio != null) {
                score = score.add(volumeRatio.subtract(BigDecimal.ONE).multiply(new BigDecimal("30")));
            }
            
            // Technical momentum (30%)
            BigDecimal rsi = getTechnicalIndicator("RSI");
            if (rsi != null) {
                // RSI between 50-70 is positive momentum
                if (rsi.compareTo(new BigDecimal("50")) >= 0 && rsi.compareTo(new BigDecimal("70")) <= 0) {
                    score = score.add(new BigDecimal("15"));
                }
            }
            
            return score;
        }
    }
    
    /**
     * Scan Statistics
     */
    @Builder
    public record ScanStatistics(
        Integer totalSymbolsScanned,
        Integer symbolsMatched,
        Integer filtersApplied,
        Long executionTimeMs,
        Map<String, Integer> exchangeBreakdown,
        Map<String, Integer> sectorBreakdown,
        BigDecimal avgQualityScore,
        String mostActiveExchange,
        String mostActiveSector
    ) {}
    
    /**
     * Pagination Information
     */
    @Builder
    public record PaginationInfo(
        Integer currentPage,
        Integer pageSize,
        Integer totalPages,
        Long totalResults,
        Boolean hasNext,
        Boolean hasPrevious
    ) {}
    
    /**
     * Get top performers by change percent
     */
    public List<ScanResultItem> getTopPerformers(int count) {
        return results.stream()
            .filter(item -> item.dayChangePercent() != null)
            .sorted((a, b) -> b.dayChangePercent().compareTo(a.dayChangePercent()))
            .limit(count)
            .toList();
    }
    
    /**
     * Get most active by volume
     */
    public List<ScanResultItem> getMostActive(int count) {
        return results.stream()
            .filter(item -> item.currentVolume() != null)
            .sorted((a, b) -> b.currentVolume().compareTo(a.currentVolume()))
            .limit(count)
            .toList();
    }
    
    /**
     * Get symbols with technical breakouts
     */
    public List<ScanResultItem> getBreakouts() {
        return results.stream()
            .filter(ScanResultItem::isNearBreakout)
            .toList();
    }
    
    /**
     * Get symbols by sector
     */
    public Map<String, List<ScanResultItem>> groupBySector() {
        return results.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                item -> item.sector() != null ? item.sector() : "UNKNOWN"
            ));
    }
    
    /**
     * Get symbols by exchange
     */
    public Map<String, List<ScanResultItem>> groupByExchange() {
        return results.stream()
            .collect(java.util.stream.Collectors.groupingBy(ScanResultItem::exchange));
    }
    
    /**
     * Get quality distribution
     */
    public Map<String, Long> getQualityDistribution() {
        return results.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ScanResultItem::getQualityGrade,
                java.util.stream.Collectors.counting()
            ));
    }
    
    /**
     * Check if scan was successful
     */
    public boolean isSuccessful() {
        return results != null && !results.isEmpty() && 
               statistics != null && statistics.totalSymbolsScanned() > 0;
    }
    
    /**
     * Get scan efficiency (matched/scanned ratio)
     */
    public BigDecimal getScanEfficiency() {
        if (statistics == null || statistics.totalSymbolsScanned() == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(statistics.symbolsMatched())
            .divide(new BigDecimal(statistics.totalSymbolsScanned()), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        if (statistics == null) return "No statistics available";
        
        return String.format(
            "Scanned %d symbols in %dms, found %d matches (%.2f%% efficiency)",
            statistics.totalSymbolsScanned(),
            statistics.executionTimeMs(),
            statistics.symbolsMatched(),
            getScanEfficiency()
        );
    }
}