package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Performance Metrics Value Object (FE-016 Implementation)
 * 
 * Comprehensive portfolio performance analytics with industry-standard metrics.
 * Implements Epic 3 requirement for performance analytics dashboard.
 * 
 * Features:
 * - Total return (absolute and percentage)
 * - Annualized return calculation
 * - Risk-adjusted metrics (Sharpe ratio, Alpha, Beta)
 * - Maximum drawdown analysis
 * - Win rate and average win/loss statistics
 * - Benchmark comparison (NIFTY, SENSEX)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-016)
 */
public record PerformanceMetrics(
    BigDecimal totalReturn,
    BigDecimal totalReturnPercent,
    BigDecimal annualizedReturn,
    BigDecimal sharpeRatio,
    BigDecimal maxDrawdown,
    BigDecimal maxDrawdownPercent,
    BigDecimal winRate,
    BigDecimal avgWin,
    BigDecimal avgLoss,
    BigDecimal volatility,
    
    List<BenchmarkComparison> benchmarkComparison,
    List<PeriodReturn> periodReturns,
    AttributionAnalysis attribution
) {
    
    /**
     * Compact constructor with validation
     */
    public PerformanceMetrics {
        // Set defaults for null values
        totalReturn = defaultIfNull(totalReturn, BigDecimal.ZERO);
        totalReturnPercent = defaultIfNull(totalReturnPercent, BigDecimal.ZERO);
        annualizedReturn = defaultIfNull(annualizedReturn, BigDecimal.ZERO);
        sharpeRatio = defaultIfNull(sharpeRatio, BigDecimal.ZERO);
        maxDrawdown = defaultIfNull(maxDrawdown, BigDecimal.ZERO);
        maxDrawdownPercent = defaultIfNull(maxDrawdownPercent, BigDecimal.ZERO);
        winRate = defaultIfNull(winRate, BigDecimal.ZERO);
        avgWin = defaultIfNull(avgWin, BigDecimal.ZERO);
        avgLoss = defaultIfNull(avgLoss, BigDecimal.ZERO);
        volatility = defaultIfNull(volatility, BigDecimal.ZERO);
        
        // Validate collections
        benchmarkComparison = defaultIfNull(benchmarkComparison, List.of());
        periodReturns = defaultIfNull(periodReturns, List.of());
        
        // Validate attribution
        if (attribution == null) {
            throw new IllegalArgumentException("Attribution analysis cannot be null");
        }
    }
    
    /**
     * Builder for PerformanceMetrics construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Get performance rating based on Sharpe ratio
     */
    public String getPerformanceRating() {
        if (sharpeRatio.compareTo(new BigDecimal("2.0")) > 0) return "EXCELLENT";
        if (sharpeRatio.compareTo(new BigDecimal("1.0")) > 0) return "GOOD";
        if (sharpeRatio.compareTo(new BigDecimal("0.5")) > 0) return "FAIR";
        if (sharpeRatio.compareTo(BigDecimal.ZERO) > 0) return "POOR";
        return "VERY_POOR";
    }
    
    /**
     * Get risk level based on volatility
     */
    public String getRiskLevel() {
        if (volatility.compareTo(new BigDecimal("30.0")) > 0) return "VERY_HIGH";
        if (volatility.compareTo(new BigDecimal("20.0")) > 0) return "HIGH";
        if (volatility.compareTo(new BigDecimal("15.0")) > 0) return "MODERATE";
        if (volatility.compareTo(new BigDecimal("10.0")) > 0) return "LOW";
        return "VERY_LOW";
    }
    
    /**
     * Check if portfolio is outperforming primary benchmark
     */
    public boolean isOutperformingBenchmark() {
        return benchmarkComparison.stream()
            .filter(bc -> "NIFTY_50".equals(bc.benchmarkSymbol()))
            .findFirst()
            .map(bc -> bc.alpha().compareTo(BigDecimal.ZERO) > 0)
            .orElse(false);
    }
    
    /**
     * Get best performing period
     */
    public String getBestPerformingPeriod() {
        return periodReturns.stream()
            .max((a, b) -> a.returnPercentage().compareTo(b.returnPercentage()))
            .map(PeriodReturn::period)
            .orElse("N/A");
    }
    
    /**
     * Get worst performing period
     */
    public String getWorstPerformingPeriod() {
        return periodReturns.stream()
            .min((a, b) -> a.returnPercentage().compareTo(b.returnPercentage()))
            .map(PeriodReturn::period)
            .orElse("N/A");
    }
    
    /**
     * Calculate risk-adjusted return (return per unit of risk)
     */
    public BigDecimal getRiskAdjustedReturn() {
        if (volatility.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return annualizedReturn.divide(volatility, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if portfolio has consistent performance (low drawdown)
     */
    public boolean hasConsistentPerformance() {
        return maxDrawdownPercent.compareTo(new BigDecimal("10.0")) <= 0;
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Builder for PerformanceMetrics
     */
    public static class Builder {
        private BigDecimal totalReturn = BigDecimal.ZERO;
        private BigDecimal totalReturnPercent = BigDecimal.ZERO;
        private BigDecimal annualizedReturn = BigDecimal.ZERO;
        private BigDecimal sharpeRatio = BigDecimal.ZERO;
        private BigDecimal maxDrawdown = BigDecimal.ZERO;
        private BigDecimal maxDrawdownPercent = BigDecimal.ZERO;
        private BigDecimal winRate = BigDecimal.ZERO;
        private BigDecimal avgWin = BigDecimal.ZERO;
        private BigDecimal avgLoss = BigDecimal.ZERO;
        private BigDecimal volatility = BigDecimal.ZERO;
        private List<BenchmarkComparison> benchmarkComparison = List.of();
        private List<PeriodReturn> periodReturns = List.of();
        private AttributionAnalysis attribution;
        
        public Builder totalReturn(BigDecimal totalReturn) {
            this.totalReturn = totalReturn;
            return this;
        }
        
        public Builder totalReturnPercent(BigDecimal totalReturnPercent) {
            this.totalReturnPercent = totalReturnPercent;
            return this;
        }
        
        public Builder annualizedReturn(BigDecimal annualizedReturn) {
            this.annualizedReturn = annualizedReturn;
            return this;
        }
        
        public Builder sharpeRatio(BigDecimal sharpeRatio) {
            this.sharpeRatio = sharpeRatio;
            return this;
        }
        
        public Builder maxDrawdown(BigDecimal maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
            return this;
        }
        
        public Builder maxDrawdownPercent(BigDecimal maxDrawdownPercent) {
            this.maxDrawdownPercent = maxDrawdownPercent;
            return this;
        }
        
        public Builder winRate(BigDecimal winRate) {
            this.winRate = winRate;
            return this;
        }
        
        public Builder avgWin(BigDecimal avgWin) {
            this.avgWin = avgWin;
            return this;
        }
        
        public Builder avgLoss(BigDecimal avgLoss) {
            this.avgLoss = avgLoss;
            return this;
        }
        
        public Builder volatility(BigDecimal volatility) {
            this.volatility = volatility;
            return this;
        }
        
        public Builder benchmarkComparison(List<BenchmarkComparison> benchmarkComparison) {
            this.benchmarkComparison = benchmarkComparison;
            return this;
        }
        
        public Builder periodReturns(List<PeriodReturn> periodReturns) {
            this.periodReturns = periodReturns;
            return this;
        }
        
        public Builder attribution(AttributionAnalysis attribution) {
            this.attribution = attribution;
            return this;
        }
        
        public PerformanceMetrics build() {
            // Create default attribution if null
            if (attribution == null) {
                attribution = AttributionAnalysis.builder()
                    .sectorAttribution(List.of())
                    .holdingAttribution(List.of())
                    .timeAttribution(List.of())
                    .brokerAttribution(List.of())
                    .build();
            }
            
            return new PerformanceMetrics(
                totalReturn, totalReturnPercent, annualizedReturn, sharpeRatio,
                maxDrawdown, maxDrawdownPercent, winRate, avgWin, avgLoss,
                volatility, benchmarkComparison, periodReturns, attribution
            );
        }
    }
}