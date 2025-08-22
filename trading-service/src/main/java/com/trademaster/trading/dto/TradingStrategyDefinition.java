package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Trading Strategy Definition DTO
 * 
 * Complete strategy definition for creation and configuration:
 * - Strategy parameters and rules
 * - Signal generation logic
 * - Risk management settings
 * - Execution preferences
 * - Backtesting configuration
 * - Machine learning settings
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategyDefinition {
    
    /**
     * Basic Strategy Information
     */
    private String name;
    private String description;
    private String strategyType; // TREND_FOLLOWING, MEAN_REVERSION, MOMENTUM, ARBITRAGE
    private String category; // SYSTEMATIC, DISCRETIONARY, HYBRID
    private List<String> symbols;
    private List<String> assetClasses;
    private String timeframe;
    private String timezone;
    
    /**
     * Signal Generation Configuration
     */
    private SignalGenerationConfig signalConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalGenerationConfig {
        private List<TechnicalIndicator> indicators;
        private List<SignalFilter> filters;
        private String combinationMethod; // AND, OR, WEIGHTED_AVERAGE
        private BigDecimal minSignalStrength;
        private Integer signalTimeoutMinutes;
        private Boolean enableMultiTimeframe;
        private List<String> confirmationTimeframes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalIndicator {
        private String name; // RSI, MACD, SMA, EMA, BOLLINGER_BANDS
        private Map<String, Object> parameters;
        private BigDecimal weight;
        private Boolean enabled;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalFilter {
        private String type; // TIME, VOLUME, VOLATILITY, TREND
        private Map<String, Object> parameters;
        private Boolean enabled;
    }
    
    /**
     * Risk Management Configuration
     */
    private RiskManagementConfig riskConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskManagementConfig {
        private BigDecimal maxPositionSize;
        private BigDecimal maxPortfolioWeight;
        private BigDecimal stopLossPercent;
        private BigDecimal takeProfitPercent;
        private BigDecimal maxDailyLoss;
        private BigDecimal maxDrawdown;
        private Boolean enableTrailingStop;
        private BigDecimal trailingStopPercent;
    }
    
    /**
     * Execution Configuration
     */
    private ExecutionConfig executionConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionConfig {
        private String executionAlgorithm; // MARKET, LIMIT, TWAP, VWAP
        private BigDecimal slippageTolerance;
        private Integer maxExecutionTimeMinutes;
        private Boolean enableSmartRouting;
        private List<String> preferredVenues;
        private String orderType;
    }
    
    /**
     * Position Sizing Configuration
     */
    private PositionSizingConfig positionSizing;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionSizingConfig {
        private String method; // FIXED, PERCENT_RISK, KELLY, VOLATILITY_TARGET
        private BigDecimal baseSize;
        private BigDecimal riskPerTrade;
        private Boolean enableDynamicSizing;
        private BigDecimal targetVolatility;
    }
    
    /**
     * Machine Learning Configuration
     */
    private MLConfiguration mlConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MLConfiguration {
        private Boolean enableML;
        private String modelType;
        private List<String> features;
        private Integer trainingWindowDays;
        private Integer retrainingFrequency;
        private Map<String, Object> hyperparameters;
    }
    
    /**
     * Backtesting Configuration
     */
    private BacktestConfiguration backtestConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestConfiguration {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal initialCapital;
        private BigDecimal commissionRate;
        private String benchmarkIndex;
        private Boolean enableMonteCarlo;
        private Integer monteCarloRuns;
    }
    
    /**
     * Strategy Validation
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               strategyType != null &&
               symbols != null && !symbols.isEmpty() &&
               signalConfig != null &&
               riskConfig != null;
    }
    
    /**
     * Get strategy complexity score
     */
    public BigDecimal getComplexityScore() {
        BigDecimal score = BigDecimal.ZERO;
        
        // Add complexity based on indicators
        if (signalConfig != null && signalConfig.getIndicators() != null) {
            score = score.add(BigDecimal.valueOf(signalConfig.getIndicators().size() * 0.2));
        }
        
        // Add complexity based on filters
        if (signalConfig != null && signalConfig.getFilters() != null) {
            score = score.add(BigDecimal.valueOf(signalConfig.getFilters().size() * 0.1));
        }
        
        // Add complexity based on ML
        if (mlConfig != null && Boolean.TRUE.equals(mlConfig.getEnableML())) {
            score = score.add(BigDecimal.valueOf(0.5));
        }
        
        return score;
    }
    
    /**
     * Static factory methods
     */
    public static TradingStrategyDefinition simpleTrendFollowing(String name, List<String> symbols) {
        return TradingStrategyDefinition.builder()
            .name(name)
            .description("Simple trend following strategy")
            .strategyType("TREND_FOLLOWING")
            .category("SYSTEMATIC")
            .symbols(symbols)
            .assetClasses(List.of("EQUITY"))
            .timeframe("1h")
            .timezone("America/New_York")
            .signalConfig(SignalGenerationConfig.builder()
                .indicators(List.of(
                    TechnicalIndicator.builder()
                        .name("SMA")
                        .parameters(Map.of("period", 20))
                        .weight(BigDecimal.valueOf(0.6))
                        .enabled(true)
                        .build(),
                    TechnicalIndicator.builder()
                        .name("RSI")
                        .parameters(Map.of("period", 14))
                        .weight(BigDecimal.valueOf(0.4))
                        .enabled(true)
                        .build()
                ))
                .combinationMethod("WEIGHTED_AVERAGE")
                .minSignalStrength(BigDecimal.valueOf(0.7))
                .signalTimeoutMinutes(60)
                .build())
            .riskConfig(RiskManagementConfig.builder()
                .maxPositionSize(BigDecimal.valueOf(10000))
                .stopLossPercent(BigDecimal.valueOf(2.0))
                .takeProfitPercent(BigDecimal.valueOf(6.0))
                .maxDailyLoss(BigDecimal.valueOf(1000))
                .build())
            .executionConfig(ExecutionConfig.builder()
                .executionAlgorithm("MARKET")
                .slippageTolerance(BigDecimal.valueOf(0.1))
                .maxExecutionTimeMinutes(5)
                .orderType("MARKET")
                .build())
            .build();
    }
}