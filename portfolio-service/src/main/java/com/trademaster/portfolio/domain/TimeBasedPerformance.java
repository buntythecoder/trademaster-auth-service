package com.trademaster.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Time Based Performance Data
 * 
 * Represents performance metrics calculated over time periods.
 */
public record TimeBasedPerformance(
    Long portfolioId,
    Instant fromDate,
    Instant toDate,
    BigDecimal totalReturn,
    BigDecimal volatility,
    BenchmarkComparison benchmarkComparison,
    List<RollingReturn> rollingReturns,
    DrawdownAnalysis drawdownAnalysis
) {
    
    /**
     * Create builder for TimeBasedPerformance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for TimeBasedPerformance
     */
    public static class Builder {
        private Long portfolioId;
        private Instant fromDate;
        private Instant toDate;
        private BigDecimal totalReturn = BigDecimal.ZERO;
        private BigDecimal volatility = BigDecimal.ZERO;
        private BenchmarkComparison benchmarkComparison;
        private List<RollingReturn> rollingReturns = List.of();
        private DrawdownAnalysis drawdownAnalysis;
        
        public Builder portfolioId(Long portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }
        
        public Builder fromDate(Instant fromDate) {
            this.fromDate = fromDate;
            return this;
        }
        
        public Builder toDate(Instant toDate) {
            this.toDate = toDate;
            return this;
        }
        
        public Builder totalReturn(BigDecimal totalReturn) {
            this.totalReturn = totalReturn;
            return this;
        }
        
        public Builder volatility(BigDecimal volatility) {
            this.volatility = volatility;
            return this;
        }
        
        public Builder benchmarkComparison(BenchmarkComparison benchmarkComparison) {
            this.benchmarkComparison = benchmarkComparison;
            return this;
        }
        
        public Builder rollingReturns(List<RollingReturn> rollingReturns) {
            this.rollingReturns = rollingReturns;
            return this;
        }
        
        public Builder drawdownAnalysis(DrawdownAnalysis drawdownAnalysis) {
            this.drawdownAnalysis = drawdownAnalysis;
            return this;
        }
        
        public TimeBasedPerformance build() {
            return new TimeBasedPerformance(
                portfolioId, fromDate, toDate, totalReturn, volatility,
                benchmarkComparison, rollingReturns, drawdownAnalysis
            );
        }
    }
}