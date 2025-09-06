package com.trademaster.portfolio.domain;

import java.math.BigDecimal;

/**
 * Benchmark Comparison Value Object
 * 
 * Represents portfolio performance comparison against market benchmarks.
 * Part of FE-016 Performance Analytics Dashboard feature.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - FE-016)
 */
public record BenchmarkComparison(
    String benchmarkName,
    String benchmarkSymbol,
    BigDecimal portfolioReturn,
    BigDecimal benchmarkReturn,
    BigDecimal alpha,         // Excess return vs benchmark
    BigDecimal beta,          // Portfolio volatility relative to benchmark
    BigDecimal correlation    // Correlation with benchmark
) {
    
    /**
     * Compact constructor with validation
     */
    public BenchmarkComparison {
        if (benchmarkName == null || benchmarkName.isBlank()) {
            throw new IllegalArgumentException("Benchmark name cannot be null or blank");
        }
        if (benchmarkSymbol == null || benchmarkSymbol.isBlank()) {
            throw new IllegalArgumentException("Benchmark symbol cannot be null or blank");
        }
        
        // Set defaults
        portfolioReturn = defaultIfNull(portfolioReturn, BigDecimal.ZERO);
        benchmarkReturn = defaultIfNull(benchmarkReturn, BigDecimal.ZERO);
        alpha = defaultIfNull(alpha, BigDecimal.ZERO);
        beta = defaultIfNull(beta, BigDecimal.ONE);
        correlation = defaultIfNull(correlation, BigDecimal.ZERO);
        
        // Validate ranges
        if (correlation.compareTo(new BigDecimal("-1.0")) < 0 || 
            correlation.compareTo(new BigDecimal("1.0")) > 0) {
            throw new IllegalArgumentException("Correlation must be between -1.0 and 1.0: " + correlation);
        }
    }
    
    /**
     * Check if portfolio is outperforming benchmark
     */
    public boolean isOutperforming() {
        return alpha.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if portfolio is more volatile than benchmark (beta > 1)
     */
    public boolean isMoreVolatile() {
        return beta.compareTo(BigDecimal.ONE) > 0;
    }
    
    /**
     * Check if portfolio is highly correlated with benchmark (>0.8)
     */
    public boolean isHighlyCorrelated() {
        return correlation.abs().compareTo(new BigDecimal("0.8")) > 0;
    }
    
    /**
     * Get relative performance description
     */
    public String getRelativePerformance() {
        BigDecimal alphaBps = alpha.multiply(new BigDecimal("100")); // Convert to basis points
        
        if (alphaBps.compareTo(new BigDecimal("500")) > 0) return "SIGNIFICANTLY_OUTPERFORMING";
        if (alphaBps.compareTo(new BigDecimal("100")) > 0) return "OUTPERFORMING";
        if (alphaBps.compareTo(new BigDecimal("-100")) > 0) return "INLINE";
        if (alphaBps.compareTo(new BigDecimal("-500")) > 0) return "UNDERPERFORMING";
        return "SIGNIFICANTLY_UNDERPERFORMING";
    }
    
    /**
     * Get beta classification
     */
    public String getBetaClassification() {
        if (beta.compareTo(new BigDecimal("1.5")) > 0) return "VERY_HIGH";
        if (beta.compareTo(new BigDecimal("1.2")) > 0) return "HIGH";
        if (beta.compareTo(new BigDecimal("0.8")) > 0) return "MODERATE";
        if (beta.compareTo(new BigDecimal("0.5")) > 0) return "LOW";
        return "VERY_LOW";
    }
    
    /**
     * Get correlation strength description
     */
    public String getCorrelationStrength() {
        BigDecimal absCorr = correlation.abs();
        
        if (absCorr.compareTo(new BigDecimal("0.9")) > 0) return "VERY_STRONG";
        if (absCorr.compareTo(new BigDecimal("0.7")) > 0) return "STRONG";
        if (absCorr.compareTo(new BigDecimal("0.5")) > 0) return "MODERATE";
        if (absCorr.compareTo(new BigDecimal("0.3")) > 0) return "WEAK";
        return "VERY_WEAK";
    }
    
    /**
     * Calculate information ratio (alpha/tracking error)
     */
    public BigDecimal calculateInformationRatio() {
        // Simplified calculation - in practice would use tracking error
        BigDecimal trackingError = new BigDecimal("2.0"); // Assumed tracking error
        if (trackingError.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return alpha.divide(trackingError, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Create builder for BenchmarkComparison
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for BenchmarkComparison
     */
    public static class Builder {
        private String benchmarkName;
        private String benchmarkSymbol;
        private BigDecimal portfolioReturn = BigDecimal.ZERO;
        private BigDecimal benchmarkReturn = BigDecimal.ZERO;
        private BigDecimal alpha = BigDecimal.ZERO;
        private BigDecimal beta = BigDecimal.ONE;
        private BigDecimal correlation = BigDecimal.ZERO;
        
        public Builder benchmarkName(String benchmarkName) {
            this.benchmarkName = benchmarkName;
            return this;
        }
        
        public Builder benchmarkSymbol(String benchmarkSymbol) {
            this.benchmarkSymbol = benchmarkSymbol;
            return this;
        }
        
        public Builder portfolioReturn(BigDecimal portfolioReturn) {
            this.portfolioReturn = portfolioReturn;
            return this;
        }
        
        public Builder benchmarkReturn(BigDecimal benchmarkReturn) {
            this.benchmarkReturn = benchmarkReturn;
            return this;
        }
        
        public Builder alpha(BigDecimal alpha) {
            this.alpha = alpha;
            return this;
        }
        
        public Builder beta(BigDecimal beta) {
            this.beta = beta;
            return this;
        }
        
        public Builder correlation(BigDecimal correlation) {
            this.correlation = correlation;
            return this;
        }
        
        public BenchmarkComparison build() {
            return new BenchmarkComparison(
                benchmarkName, benchmarkSymbol, portfolioReturn, benchmarkReturn,
                alpha, beta, correlation
            );
        }
    }
}