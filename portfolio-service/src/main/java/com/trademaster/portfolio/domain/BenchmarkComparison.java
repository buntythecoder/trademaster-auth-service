package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.util.Optional;

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
     * Compact constructor with functional validation - eliminates all if-statements with Optional
     */
    public BenchmarkComparison {
        // Validate string fields - eliminates if-statements with Optional.filter().orElseThrow()
        Optional.ofNullable(benchmarkName)
            .filter(name -> !name.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Benchmark name cannot be null or blank"));

        Optional.ofNullable(benchmarkSymbol)
            .filter(symbol -> !symbol.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Benchmark symbol cannot be null or blank"));

        // Set defaults - eliminates if-statements with Optional.ofNullable().orElse()
        portfolioReturn = Optional.ofNullable(portfolioReturn).orElse(BigDecimal.ZERO);
        benchmarkReturn = Optional.ofNullable(benchmarkReturn).orElse(BigDecimal.ZERO);
        alpha = Optional.ofNullable(alpha).orElse(BigDecimal.ZERO);
        beta = Optional.ofNullable(beta).orElse(BigDecimal.ONE);

        // Capture original correlation value for error message (must be effectively final for lambda)
        final var originalCorrelation = correlation;
        correlation = Optional.ofNullable(correlation).orElse(BigDecimal.ZERO);

        // Validate correlation range - eliminates if-statement with Optional.filter().orElseThrow()
        Optional.of(correlation)
            .filter(corr -> corr.compareTo(new BigDecimal("-1.0")) >= 0 &&
                           corr.compareTo(new BigDecimal("1.0")) <= 0)
            .orElseThrow(() -> new IllegalArgumentException("Correlation must be between -1.0 and 1.0: " + originalCorrelation));
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
     * Get relative performance description - eliminates if-statements with Optional chaining
     */
    public String getRelativePerformance() {
        BigDecimal alphaBps = alpha.multiply(new BigDecimal("100")); // Convert to basis points

        return Optional.of(alphaBps)
            .filter(bps -> bps.compareTo(new BigDecimal("500")) > 0)
            .map(bps -> "SIGNIFICANTLY_OUTPERFORMING")
            .or(() -> Optional.of(alphaBps)
                .filter(bps -> bps.compareTo(new BigDecimal("100")) > 0)
                .map(bps -> "OUTPERFORMING"))
            .or(() -> Optional.of(alphaBps)
                .filter(bps -> bps.compareTo(new BigDecimal("-100")) > 0)
                .map(bps -> "INLINE"))
            .or(() -> Optional.of(alphaBps)
                .filter(bps -> bps.compareTo(new BigDecimal("-500")) > 0)
                .map(bps -> "UNDERPERFORMING"))
            .orElse("SIGNIFICANTLY_UNDERPERFORMING");
    }
    
    /**
     * Get beta classification - eliminates if-statements with Optional chaining
     */
    public String getBetaClassification() {
        return Optional.of(beta)
            .filter(b -> b.compareTo(new BigDecimal("1.5")) > 0)
            .map(b -> "VERY_HIGH")
            .or(() -> Optional.of(beta)
                .filter(b -> b.compareTo(new BigDecimal("1.2")) > 0)
                .map(b -> "HIGH"))
            .or(() -> Optional.of(beta)
                .filter(b -> b.compareTo(new BigDecimal("0.8")) > 0)
                .map(b -> "MODERATE"))
            .or(() -> Optional.of(beta)
                .filter(b -> b.compareTo(new BigDecimal("0.5")) > 0)
                .map(b -> "LOW"))
            .orElse("VERY_LOW");
    }
    
    /**
     * Get correlation strength description - eliminates if-statements with Optional chaining
     */
    public String getCorrelationStrength() {
        BigDecimal absCorr = correlation.abs();

        return Optional.of(absCorr)
            .filter(corr -> corr.compareTo(new BigDecimal("0.9")) > 0)
            .map(corr -> "VERY_STRONG")
            .or(() -> Optional.of(absCorr)
                .filter(corr -> corr.compareTo(new BigDecimal("0.7")) > 0)
                .map(corr -> "STRONG"))
            .or(() -> Optional.of(absCorr)
                .filter(corr -> corr.compareTo(new BigDecimal("0.5")) > 0)
                .map(corr -> "MODERATE"))
            .or(() -> Optional.of(absCorr)
                .filter(corr -> corr.compareTo(new BigDecimal("0.3")) > 0)
                .map(corr -> "WEAK"))
            .orElse("VERY_WEAK");
    }
    
    /**
     * Calculate information ratio (alpha/tracking error) - eliminates if-statement with Optional
     */
    public BigDecimal calculateInformationRatio() {
        // Simplified calculation - in practice would use tracking error
        BigDecimal trackingError = new BigDecimal("2.0"); // Assumed tracking error
        return Optional.of(trackingError)
            .filter(error -> error.compareTo(BigDecimal.ZERO) > 0)
            .map(error -> alpha.divide(error, 4, BigDecimal.ROUND_HALF_UP))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Helper method to provide default value for null - eliminates ternary with Optional
     */
    private static <T> T defaultIfNull(T value, T defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
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