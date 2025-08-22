package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * P&L Report DTO
 * 
 * Comprehensive profit and loss reporting with:
 * - Realized and unrealized P&L breakdown
 * - Period-over-period analysis
 * - Attribution analysis by symbol, sector, strategy
 * - Tax implications and wash sale tracking
 * - Performance benchmarking
 * - Risk-adjusted returns
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PnLReport {
    
    /**
     * Report Metadata
     */
    private Long userId;
    private String reportId;
    private Instant generatedAt;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String reportType; // REALTIME, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
    private String currency;
    
    /**
     * Summary P&L
     */
    private BigDecimal totalPnL; // Total P&L for period
    private BigDecimal realizedPnL; // Realized P&L from closed positions
    private BigDecimal unrealizedPnL; // Unrealized P&L from open positions
    private BigDecimal previousPeriodPnL; // Previous period P&L for comparison
    private BigDecimal periodChange; // Change from previous period
    private BigDecimal periodChangePercent; // Percentage change from previous period
    
    /**
     * Detailed P&L Breakdown
     */
    private BigDecimal tradingPnL; // P&L from trading activities
    private BigDecimal dividendIncome; // Dividend income received
    private BigDecimal interestIncome; // Interest income
    private BigDecimal interestExpense; // Interest paid on margin
    private BigDecimal borrowingCosts; // Security borrowing costs
    private BigDecimal commissions; // Commission costs
    private BigDecimal fees; // Other fees and charges
    private BigDecimal taxes; // Tax implications
    private BigDecimal netPnL; // Net P&L after all costs
    
    /**
     * P&L by Time Period
     */
    private List<PeriodPnL> dailyPnL; // Daily P&L breakdown
    private List<PeriodPnL> weeklyPnL; // Weekly P&L summary
    private List<PeriodPnL> monthlyPnL; // Monthly P&L summary
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodPnL {
        private LocalDate date;
        private BigDecimal realizedPnL;
        private BigDecimal unrealizedPnL;
        private BigDecimal totalPnL;
        private BigDecimal cumulativePnL;
        private Integer tradesCount;
        private BigDecimal volume;
        private BigDecimal highWaterMark;
        private BigDecimal drawdown;
    }
    
    /**
     * P&L by Symbol
     */
    private List<SymbolPnL> symbolBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolPnL {
        private String symbol;
        private String exchange;
        private BigDecimal realizedPnL;
        private BigDecimal unrealizedPnL;
        private BigDecimal totalPnL;
        private BigDecimal totalReturn;
        private BigDecimal totalReturnPercent;
        private Integer tradesCount;
        private BigDecimal volume;
        private BigDecimal averageHoldingPeriod;
        private BigDecimal winRate;
        private BigDecimal profitFactor;
        private String sector;
        private String industry;
    }
    
    /**
     * P&L by Sector
     */\n    private List<SectorPnL> sectorBreakdown;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class SectorPnL {\n        private String sectorName;\n        private BigDecimal realizedPnL;\n        private BigDecimal unrealizedPnL;\n        private BigDecimal totalPnL;\n        private BigDecimal sectorWeight; // Percentage of portfolio\n        private BigDecimal sectorContribution; // Contribution to total P&L\n        private Integer positionsCount;\n        private List<String> topContributors; // Top contributing symbols\n        private List<String> topDetractors; // Top detracting symbols\n    }\n    \n    /**\n     * P&L by Strategy/Tag\n     */\n    private List<StrategyPnL> strategyBreakdown;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class StrategyPnL {\n        private String strategyName;\n        private String description;\n        private BigDecimal realizedPnL;\n        private BigDecimal unrealizedPnL;\n        private BigDecimal totalPnL;\n        private BigDecimal sharpeRatio;\n        private BigDecimal maxDrawdown;\n        private BigDecimal winRate;\n        private Integer tradesCount;\n        private BigDecimal averageTradeSize;\n        private BigDecimal totalVolume;\n    }\n    \n    /**\n     * Trading Statistics\n     */\n    private TradingStats tradingStats;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class TradingStats {\n        private Integer totalTrades;\n        private Integer winningTrades;\n        private Integer losingTrades;\n        private BigDecimal winRate; // Percentage\n        private BigDecimal averageWin;\n        private BigDecimal averageLoss;\n        private BigDecimal largestWin;\n        private BigDecimal largestLoss;\n        private BigDecimal profitFactor; // Gross profit / gross loss\n        private BigDecimal averageTradeSize;\n        private BigDecimal averageHoldingPeriod; // In days\n        private BigDecimal tradingFrequency; // Trades per day\n        private BigDecimal turnoverRate; // Portfolio turnover\n    }\n    \n    /**\n     * Risk Metrics\n     */\n    private RiskMetrics riskMetrics;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class RiskMetrics {\n        private BigDecimal volatility; // Annualized volatility\n        private BigDecimal sharpeRatio; // Risk-adjusted return\n        private BigDecimal sortinoRatio; // Downside risk-adjusted return\n        private BigDecimal calmarRatio; // Return / max drawdown\n        private BigDecimal maxDrawdown;\n        private BigDecimal currentDrawdown;\n        private Integer drawdownDays;\n        private BigDecimal valueAtRisk; // 95% VaR\n        private BigDecimal expectedShortfall; // CVaR\n        private BigDecimal beta; // Market beta\n        private BigDecimal alpha; // Market alpha\n        private BigDecimal trackingError;\n        private BigDecimal informationRatio;\n    }\n    \n    /**\n     * Performance Attribution\n     */\n    private PerformanceAttribution attribution;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class PerformanceAttribution {\n        private Map<String, BigDecimal> assetAllocationEffect;\n        private Map<String, BigDecimal> stockSelectionEffect;\n        private Map<String, BigDecimal> interactionEffect;\n        private Map<String, BigDecimal> timingEffect;\n        private Map<String, BigDecimal> currencyEffect;\n        private BigDecimal totalActiveReturn;\n        private String primaryDriver; // Main source of returns\n    }\n    \n    /**\n     * Tax Analysis\n     */\n    private TaxAnalysis taxAnalysis;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class TaxAnalysis {\n        private BigDecimal shortTermCapitalGains;\n        private BigDecimal longTermCapitalGains;\n        private BigDecimal dividendIncome;\n        private BigDecimal interestIncome;\n        private BigDecimal taxableIncome;\n        private BigDecimal estimatedTaxLiability;\n        private BigDecimal afterTaxReturn;\n        private List<WashSaleAdjustment> washSales;\n        private Map<String, BigDecimal> taxLotBreakdown;\n    }\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class WashSaleAdjustment {\n        private String symbol;\n        private LocalDate sellDate;\n        private LocalDate repurchaseDate;\n        private BigDecimal adjustedLoss;\n        private BigDecimal washSaleAmount;\n        private String description;\n    }\n    \n    /**\n     * Benchmark Comparison\n     */\n    private BenchmarkComparison benchmarkComparison;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class BenchmarkComparison {\n        private String benchmarkName; // e.g., NIFTY50, SENSEX\n        private BigDecimal benchmarkReturn;\n        private BigDecimal portfolioReturn;\n        private BigDecimal activeReturn; // Portfolio - benchmark\n        private BigDecimal outperformance; // Positive = outperformed\n        private BigDecimal trackingError;\n        private BigDecimal informationRatio;\n        private BigDecimal upCaptureRatio;\n        private BigDecimal downCaptureRatio;\n        private Integer outperformanceDays;\n        private Integer underperformanceDays;\n    }\n    \n    /**\n     * Cost Analysis\n     */\n    private CostAnalysis costAnalysis;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class CostAnalysis {\n        private BigDecimal totalTradingCosts;\n        private BigDecimal commissionCosts;\n        private BigDecimal spreadCosts;\n        private BigDecimal marketImpactCosts;\n        private BigDecimal opportunityCosts;\n        private BigDecimal borrowingCosts;\n        private BigDecimal managementFees;\n        private BigDecimal costAsPercentOfPnL;\n        private BigDecimal costDragOnReturns;\n        private Map<String, BigDecimal> costBreakdown;\n    }\n    \n    /**\n     * Monthly Summary (for longer period reports)\n     */\n    private List<MonthlySummary> monthlySummaries;\n    \n    @Data\n    @Builder\n    @NoArgsConstructor\n    @AllArgsConstructor\n    public static class MonthlySummary {\n        private String month; // YYYY-MM format\n        private BigDecimal monthlyReturn;\n        private BigDecimal cumulativeReturn;\n        private BigDecimal volatility;\n        private BigDecimal maxDrawdown;\n        private Integer tradesCount;\n        private BigDecimal turnover;\n        private BigDecimal sharpeRatio;\n        private String performanceRank; // TOP_QUARTILE, etc.\n    }\n    \n    /**\n     * Helper Methods\n     */\n    \n    /**\n     * Get overall return percentage\n     */\n    public BigDecimal getOverallReturnPercent() {\n        // This would typically be calculated based on initial portfolio value\n        // For now, return a placeholder calculation\n        return totalPnL != null ? totalPnL : BigDecimal.ZERO;\n    }\n    \n    /**\n     * Get profit factor (gross profit / gross loss)\n     */\n    public BigDecimal getProfitFactor() {\n        if (tradingStats != null && tradingStats.getProfitFactor() != null) {\n            return tradingStats.getProfitFactor();\n        }\n        return BigDecimal.ZERO;\n    }\n    \n    /**\n     * Get win rate percentage\n     */\n    public BigDecimal getWinRate() {\n        if (tradingStats != null && tradingStats.getWinRate() != null) {\n            return tradingStats.getWinRate();\n        }\n        return BigDecimal.ZERO;\n    }\n    \n    /**\n     * Check if portfolio outperformed benchmark\n     */\n    public boolean outperformedBenchmark() {\n        return benchmarkComparison != null && \n               benchmarkComparison.getActiveReturn() != null &&\n               benchmarkComparison.getActiveReturn().compareTo(BigDecimal.ZERO) > 0;\n    }\n    \n    /**\n     * Get performance category based on Sharpe ratio\n     */\n    public String getPerformanceCategory() {\n        if (riskMetrics == null || riskMetrics.getSharpeRatio() == null) {\n            return \"UNKNOWN\";\n        }\n        \n        BigDecimal sharpe = riskMetrics.getSharpeRatio();\n        if (sharpe.compareTo(new BigDecimal(\"2.0\")) > 0) {\n            return \"EXCELLENT\";\n        } else if (sharpe.compareTo(new BigDecimal(\"1.0\")) > 0) {\n            return \"VERY_GOOD\";\n        } else if (sharpe.compareTo(new BigDecimal(\"0.5\")) > 0) {\n            return \"GOOD\";\n        } else if (sharpe.compareTo(BigDecimal.ZERO) > 0) {\n            return \"FAIR\";\n        } else {\n            return \"POOR\";\n        }\n    }\n    \n    /**\n     * Get risk category based on volatility and drawdown\n     */\n    public String getRiskCategory() {\n        if (riskMetrics == null || riskMetrics.getMaxDrawdown() == null) {\n            return \"UNKNOWN\";\n        }\n        \n        BigDecimal maxDD = riskMetrics.getMaxDrawdown().abs();\n        if (maxDD.compareTo(new BigDecimal(\"5\")) <= 0) {\n            return \"LOW\";\n        } else if (maxDD.compareTo(new BigDecimal(\"15\")) <= 0) {\n            return \"MODERATE\";\n        } else if (maxDD.compareTo(new BigDecimal(\"25\")) <= 0) {\n            return \"HIGH\";\n        } else {\n            return \"VERY_HIGH\";\n        }\n    }\n    \n    /**\n     * Get top performing symbols\n     */\n    public List<SymbolPnL> getTopPerformers(int count) {\n        if (symbolBreakdown == null) {\n            return List.of();\n        }\n        \n        return symbolBreakdown.stream()\n            .sorted((a, b) -> b.getTotalPnL().compareTo(a.getTotalPnL()))\n            .limit(count)\n            .toList();\n    }\n    \n    /**\n     * Get worst performing symbols\n     */\n    public List<SymbolPnL> getWorstPerformers(int count) {\n        if (symbolBreakdown == null) {\n            return List.of();\n        }\n        \n        return symbolBreakdown.stream()\n            .sorted((a, b) -> a.getTotalPnL().compareTo(b.getTotalPnL()))\n            .limit(count)\n            .toList();\n    }\n    \n    /**\n     * Calculate after-tax return\n     */\n    public BigDecimal getAfterTaxReturn() {\n        if (taxAnalysis != null && taxAnalysis.getAfterTaxReturn() != null) {\n            return taxAnalysis.getAfterTaxReturn();\n        }\n        return totalPnL; // Fallback to gross return\n    }\n    \n    /**\n     * Get summary statistics\n     */\n    public Map<String, Object> getSummaryStats() {\n        return Map.of(\n            \"totalPnL\", totalPnL != null ? totalPnL : BigDecimal.ZERO,\n            \"realizedPnL\", realizedPnL != null ? realizedPnL : BigDecimal.ZERO,\n            \"unrealizedPnL\", unrealizedPnL != null ? unrealizedPnL : BigDecimal.ZERO,\n            \"winRate\", getWinRate(),\n            \"profitFactor\", getProfitFactor(),\n            \"sharpeRatio\", riskMetrics != null && riskMetrics.getSharpeRatio() != null ? \n                          riskMetrics.getSharpeRatio() : BigDecimal.ZERO,\n            \"maxDrawdown\", riskMetrics != null && riskMetrics.getMaxDrawdown() != null ?\n                          riskMetrics.getMaxDrawdown() : BigDecimal.ZERO,\n            \"performanceCategory\", getPerformanceCategory(),\n            \"riskCategory\", getRiskCategory(),\n            \"outperformedBenchmark\", outperformedBenchmark()\n        );\n    }\n    \n    /**\n     * Static factory methods\n     */\n    public static PnLReport empty(Long userId, LocalDate periodStart, LocalDate periodEnd) {\n        return PnLReport.builder()\n            .userId(userId)\n            .generatedAt(Instant.now())\n            .periodStart(periodStart)\n            .periodEnd(periodEnd)\n            .totalPnL(BigDecimal.ZERO)\n            .realizedPnL(BigDecimal.ZERO)\n            .unrealizedPnL(BigDecimal.ZERO)\n            .netPnL(BigDecimal.ZERO)\n            .build();\n    }\n    \n    public static PnLReport error(Long userId, String errorMessage) {\n        return PnLReport.builder()\n            .userId(userId)\n            .generatedAt(Instant.now())\n            .totalPnL(BigDecimal.ZERO)\n            .realizedPnL(BigDecimal.ZERO)\n            .unrealizedPnL(BigDecimal.ZERO)\n            .build();\n    }\n}"