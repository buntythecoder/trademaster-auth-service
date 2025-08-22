package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Trading Strategy DTO
 * 
 * Comprehensive trading strategy definition with:
 * - Multi-asset class support and execution parameters
 * - Advanced signal generation and filtering logic
 * - Risk management and position sizing rules
 * - Machine learning integration and adaptive features
 * - Performance tracking and optimization capabilities
 * - Real-time monitoring and alert systems
 * - Backtesting and simulation configurations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategy {
    
    /**
     * Strategy Identification and Metadata
     */
    private String strategyId;
    private String strategyName;
    private String description;
    private Long userId; // Strategy owner
    private String version; // Strategy version for tracking changes
    private String strategyType; // TREND_FOLLOWING, MEAN_REVERSION, MOMENTUM, ARBITRAGE, ML_BASED
    private String strategyCategory; // SYSTEMATIC, DISCRETIONARY, HYBRID
    private Instant createdAt;
    private Instant lastModifiedAt;
    private String modifiedBy;
    
    /**
     * Strategy Configuration
     */
    private StrategyConfig configuration;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyConfig {
        // Basic Strategy Parameters
        private String timeframe; // 1m, 5m, 15m, 1h, 4h, 1d
        private List<String> symbols; // Tradeable instruments
        private List<String> assetClasses; // EQUITY, FOREX, CRYPTO, COMMODITY
        private String tradingHours; // Market hours for execution
        private String timezone; // Strategy timezone
        
        // Signal Generation Parameters
        private SignalConfig signalConfig;
        private List<IndicatorConfig> indicators; // Technical indicators
        private FilterConfig filterConfig; // Signal filters
        
        // Execution Parameters
        private ExecutionConfig executionConfig;
        private RiskConfig riskConfig;
        private PositionSizing positionSizing;
        
        // Advanced Features
        private MLConfig mlConfig; // Machine learning configuration
        private OptimizationConfig optimizationConfig;
        private BacktestConfig backtestConfig;
    }
    
    /**
     * Signal Generation Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalConfig {
        private String signalType; // BUY, SELL, BUY_SELL, MULTI_LEVEL
        private BigDecimal minSignalStrength; // Minimum signal confidence
        private Integer signalTimeoutMinutes; // Signal expiration time
        private Boolean enableSignalFiltering; // Apply signal filters
        private String signalCombinationMethod; // AND, OR, WEIGHTED_AVERAGE
        private List<String> signalSources; // Technical, fundamental, sentiment
        private Map<String, BigDecimal> signalWeights; // Signal source weights
        private Boolean enableSignalSmoothing; // Smooth signal noise
        private Integer lookbackPeriods; // Historical periods for signals
        private BigDecimal signalThreshold; // Entry/exit thresholds
    }
    
    /**
     * Technical Indicator Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorConfig {
        private String indicatorName; // RSI, MACD, SMA, EMA, BOLLINGER_BANDS
        private String indicatorType; // TREND, MOMENTUM, VOLATILITY, VOLUME
        private Map<String, Object> parameters; // Indicator-specific parameters
        private String dataSource; // OHLCV, TICK, ORDER_BOOK
        private Integer period; // Calculation period
        private Boolean enabled; // Indicator active status
        private BigDecimal weight; // Indicator weight in composite signals
        private String normalizeMethod; // NONE, Z_SCORE, MIN_MAX
        private Boolean enableDynamicPeriod; // Adaptive period calculation
    }
    
    /**
     * Signal Filter Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterConfig {
        private Boolean enableTimeFilter; // Filter by time of day
        private List<String> allowedTimeRanges; // Allowed trading hours
        private Boolean enableVolumeFilter; // Filter by volume conditions
        private BigDecimal minVolumeRatio; // Minimum volume vs average
        private Boolean enableVolatilityFilter; // Filter by volatility
        private BigDecimal maxVolatility; // Maximum allowed volatility
        private Boolean enableTrendFilter; // Filter by trend direction
        private String trendDirection; // BULL, BEAR, SIDEWAYS, ANY
        private Boolean enableNewsFilter; // Filter by news events
        private List<String> newsCategories; // Relevant news categories
        private Boolean enableSeasonalityFilter; // Seasonal patterns
        private Map<String, BigDecimal> seasonalAdjustments; // Monthly/quarterly adjustments
    }
    
    /**
     * Execution Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionConfig {
        private String executionAlgorithm; // MARKET, LIMIT, TWAP, VWAP, IS
        private BigDecimal slippage; // Expected slippage tolerance
        private Integer maxExecutionTimeMinutes; // Maximum execution time
        private Boolean enableSmartRouting; // Use smart order routing
        private List<String> preferredVenues; // Preferred execution venues
        private String orderType; // MARKET, LIMIT, STOP, STOP_LIMIT
        private BigDecimal limitOffset; // Limit price offset from market
        private Boolean enablePartialFills; // Accept partial fills
        private Integer maxOrderSlices; // Maximum order slicing
        private BigDecimal minFillSize; // Minimum fill size
        private String timeInForce; // GTC, DAY, IOC, FOK
        private Boolean enableDarkPool; // Use dark pool liquidity
        private BigDecimal participationRate; // VWAP participation rate
    }
    
    /**
     * Risk Management Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskConfig {
        // Position Limits
        private BigDecimal maxPositionSize; // Maximum position size
        private BigDecimal maxPortfolioWeight; // Maximum portfolio allocation
        private Integer maxConcurrentPositions; // Maximum open positions
        private BigDecimal maxSectorExposure; // Maximum sector concentration
        
        // Stop Loss and Take Profit
        private Boolean enableStopLoss; // Enable stop loss orders
        private BigDecimal stopLossPercent; // Stop loss percentage
        private Boolean enableTrailingStop; // Enable trailing stop
        private BigDecimal trailingStopPercent; // Trailing stop percentage
        private Boolean enableTakeProfit; // Enable take profit orders
        private BigDecimal takeProfitPercent; // Take profit percentage
        
        // Risk Metrics
        private BigDecimal maxDailyLoss; // Maximum daily loss limit
        private BigDecimal maxDrawdown; // Maximum drawdown limit
        private BigDecimal maxVaR; // Maximum Value at Risk
        private BigDecimal maxBeta; // Maximum beta exposure
        private BigDecimal maxCorrelation; // Maximum position correlation
        
        // Risk Monitoring
        private Boolean enableRealTimeRisk; // Real-time risk monitoring
        private Integer riskCheckIntervalSeconds; // Risk check frequency
        private Boolean enableRiskAlerts; // Risk alert notifications
        private List<String> alertMethods; // EMAIL, SMS, PUSH, API
        
        // Emergency Controls
        private Boolean enableEmergencyStop; // Emergency stop capability
        private List<String> emergencyTriggers; // Emergency stop conditions
        private Boolean enableAutoLiquidation; // Automatic position liquidation
        private BigDecimal liquidationThreshold; // Auto-liquidation threshold
    }
    
    /**
     * Position Sizing Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionSizing {
        private String sizingMethod; // FIXED, PERCENT_RISK, KELLY, VOLATILITY_TARGET
        private BigDecimal basePositionSize; // Base position size
        private BigDecimal riskPerTrade; // Risk per trade percentage
        private Boolean enableDynamicSizing; // Dynamic position sizing
        private String volatilityMeasure; // ATR, HISTORICAL, IMPLIED
        private Integer volatilityPeriod; // Volatility calculation period
        private BigDecimal targetVolatility; // Target portfolio volatility
        private Boolean enableKellyCriterion; // Kelly criterion sizing
        private BigDecimal kellyFraction; // Fraction of Kelly to use
        private BigDecimal maxKellySize; // Maximum Kelly position size
        private Boolean enableCorrelationAdjustment; // Adjust for correlations
        private Boolean enableConcentrationLimit; // Apply concentration limits
        private Map<String, BigDecimal> sectorLimits; // Sector allocation limits
    }
    
    /**
     * Machine Learning Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MLConfig {
        private Boolean enableML; // Enable ML features
        private String modelType; // REGRESSION, CLASSIFICATION, ENSEMBLE, NEURAL_NETWORK
        private List<String> featureSet; // Feature engineering configuration
        private String targetVariable; // Target variable for prediction
        private Integer trainingWindowDays; // Training data window
        private Integer retrainingFrequencyDays; // Model retraining frequency
        private BigDecimal predictionConfidenceThreshold; // Minimum prediction confidence
        private Boolean enableOnlineLearning; // Online learning capability
        private String validationMethod; // WALK_FORWARD, CROSS_VALIDATION, BOOTSTRAP
        private Map<String, Object> hyperparameters; // Model hyperparameters
        private Boolean enableFeatureSelection; // Automatic feature selection
        private Boolean enableEnsembleMethods; // Use ensemble models
        private String ensembleMethod; // BAGGING, BOOSTING, STACKING
        private Integer ensembleSize; // Number of models in ensemble
    }
    
    /**
     * Optimization Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationConfig {
        private Boolean enableOptimization; // Enable parameter optimization
        private String optimizationMethod; // GENETIC, GRID_SEARCH, BAYESIAN, PARTICLE_SWARM
        private String objectiveFunction; // RETURN, SHARPE, CALMAR, SORTINO, MULTI_OBJECTIVE
        private List<String> optimizationParameters; // Parameters to optimize
        private Map<String, Object> parameterRanges; // Parameter optimization ranges
        private Integer optimizationIterations; // Maximum optimization iterations
        private BigDecimal convergenceThreshold; // Optimization convergence threshold
        private Boolean enableWalkForward; // Walk-forward optimization
        private Integer walkForwardSteps; // Number of walk-forward steps
        private Integer outOfSamplePeriod; // Out-of-sample testing period
        private Boolean enableRobustnessTest; // Parameter robustness testing
        private String robustnessMetric; // Robustness measurement metric
    }
    
    /**
     * Backtesting Configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestConfig {
        private LocalDate startDate; // Backtest start date
        private LocalDate endDate; // Backtest end date
        private BigDecimal initialCapital; // Starting capital
        private BigDecimal commissionRate; // Commission per trade
        private BigDecimal slippageModel; // Slippage model parameters
        private String dataFrequency; // Data frequency (1m, 5m, 1h, 1d)
        private Boolean enableRebalancing; // Portfolio rebalancing
        private String rebalancingFrequency; // Daily, weekly, monthly
        private Boolean adjustForDividends; // Dividend adjustments
        private Boolean adjustForSplits; // Stock split adjustments
        private String benchmarkIndex; // Benchmark for comparison
        private List<String> performanceMetrics; // Metrics to calculate
        private Boolean enableMonteCarlo; // Monte Carlo simulation
        private Integer monteCarloRuns; // Number of MC simulations
        private Boolean generateReport; // Generate detailed report
    }
    
    /**
     * Strategy State and Status
     */
    private StrategyStatus status;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyStatus {
        private String currentState; // CREATED, TESTING, DEPLOYED, RUNNING, PAUSED, STOPPED, ERROR
        private String deploymentEnvironment; // PAPER, LIVE
        private LocalDateTime lastExecutionTime; // Last signal generation time
        private LocalDateTime nextScheduledRun; // Next scheduled execution
        private Integer totalSignalsGenerated; // Total signals generated
        private Integer successfulTrades; // Successful trade count
        private Integer failedTrades; // Failed trade count
        private BigDecimal totalPnL; // Total P&L from strategy
        private BigDecimal currentDrawdown; // Current drawdown
        private BigDecimal maxDrawdown; // Maximum drawdown
        private String healthStatus; // HEALTHY, WARNING, CRITICAL, ERROR
        private List<String> activeAlerts; // Current active alerts
        private Map<String, Object> performanceMetrics; // Key performance metrics
        private String lastErrorMessage; // Last error encountered
        private Instant lastErrorTime; // Time of last error
        private Integer errorCount24h; // Error count in last 24 hours
    }
    
    /**
     * Strategy Performance Metrics
     */
    private PerformanceMetrics performanceMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        // Return Metrics
        private BigDecimal totalReturn; // Total strategy return
        private BigDecimal annualizedReturn; // Annualized return
        private BigDecimal monthToDateReturn; // MTD return
        private BigDecimal yearToDateReturn; // YTD return
        private BigDecimal weeklyReturn; // Weekly return
        private BigDecimal dailyReturn; // Daily return
        
        // Risk Metrics
        private BigDecimal volatility; // Return volatility
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal sortinoRatio; // Downside risk-adjusted return
        private BigDecimal calmarRatio; // Return/max drawdown
        private BigDecimal informationRatio; // Excess return/tracking error
        private BigDecimal beta; // Market beta
        private BigDecimal alpha; // Market alpha
        private BigDecimal trackingError; // Tracking error vs benchmark
        
        // Drawdown Metrics
        private BigDecimal maxDrawdown; // Maximum drawdown
        private BigDecimal currentDrawdown; // Current drawdown
        private Integer maxDrawdownDuration; // Max drawdown duration (days)
        private Integer currentDrawdownDuration; // Current drawdown duration
        private BigDecimal averageDrawdown; // Average drawdown
        private Integer drawdownRecoveryDays; // Average recovery time
        
        // Trade Metrics
        private Integer totalTrades; // Total number of trades
        private Integer winningTrades; // Number of winning trades
        private Integer losingTrades; // Number of losing trades
        private BigDecimal winRate; // Win rate percentage
        private BigDecimal averageWin; // Average winning trade
        private BigDecimal averageLoss; // Average losing trade
        private BigDecimal profitFactor; // Gross profit/gross loss
        private BigDecimal averageHoldingPeriod; // Average trade duration
        private BigDecimal maxConsecutiveWins; // Maximum consecutive wins
        private BigDecimal maxConsecutiveLosses; // Maximum consecutive losses
        
        // Efficiency Metrics
        private Integer signalAccuracy; // Signal prediction accuracy
        private BigDecimal executionQuality; // Execution quality score
        private BigDecimal transactionCosts; // Total transaction costs
        private BigDecimal capacity; // Strategy capacity estimate
        private BigDecimal scalability; // Scalability score
        private String performanceConsistency; // Consistency rating
    }
    
    /**
     * Resource Usage and System Metrics
     */
    private ResourceUsage resourceUsage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsage {
        private Long cpuTimeMillis; // CPU time used
        private Long memoryUsageBytes; // Memory usage
        private Long networkBytesTransferred; // Network data transferred
        private Integer databaseQueries; // Database queries executed
        private Long storageUsageBytes; // Storage space used
        private BigDecimal operationalCost; // Estimated operational cost
        private String resourceEfficiency; // Resource efficiency rating
        private Map<String, Object> detailedMetrics; // Detailed resource metrics
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if strategy is currently running
     */
    public boolean isRunning() {
        return status != null && "RUNNING".equals(status.getCurrentState());
    }
    
    /**
     * Check if strategy is in error state
     */
    public boolean hasErrors() {
        return status != null && ("ERROR".equals(status.getCurrentState()) || 
                                "CRITICAL".equals(status.getHealthStatus()));
    }
    
    /**
     * Check if strategy is profitable
     */
    public boolean isProfitable() {
        return performanceMetrics != null && 
               performanceMetrics.getTotalReturn() != null &&
               performanceMetrics.getTotalReturn().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get strategy Sharpe ratio
     */
    public BigDecimal getSharpeRatio() {
        return performanceMetrics != null && performanceMetrics.getSharpeRatio() != null
            ? performanceMetrics.getSharpeRatio()
            : BigDecimal.ZERO;
    }
    
    /**
     * Check if strategy requires attention
     */
    public boolean requiresAttention() {
        if (status == null) return true;
        
        return "WARNING".equals(status.getHealthStatus()) || 
               "CRITICAL".equals(status.getHealthStatus()) ||
               "ERROR".equals(status.getHealthStatus()) ||
               (status.getActiveAlerts() != null && !status.getActiveAlerts().isEmpty());
    }
    
    /**
     * Get strategy age in days
     */
    public Long getAgeInDays() {
        if (createdAt == null) return 0L;
        return java.time.Duration.between(createdAt, Instant.now()).toDays();
    }
    
    /**
     * Get strategy summary for dashboard
     */
    public Map<String, Object> getStrategySummary() {
        return Map.of(
            "strategyId", strategyId != null ? strategyId : "N/A",
            "name", strategyName != null ? strategyName : "Unnamed Strategy",
            "type", strategyType != null ? strategyType : "UNKNOWN",
            "status", status != null && status.getCurrentState() != null ? 
                     status.getCurrentState() : "UNKNOWN",
            "performance", performanceMetrics != null && performanceMetrics.getTotalReturn() != null ?
                         performanceMetrics.getTotalReturn() : BigDecimal.ZERO,
            "sharpe", getSharpeRatio(),
            "maxDrawdown", performanceMetrics != null && performanceMetrics.getMaxDrawdown() != null ?
                         performanceMetrics.getMaxDrawdown() : BigDecimal.ZERO,
            "totalTrades", performanceMetrics != null && performanceMetrics.getTotalTrades() != null ?
                         performanceMetrics.getTotalTrades() : 0,
            "isRunning", isRunning(),
            "requiresAttention", requiresAttention(),
            "lastUpdate", lastModifiedAt != null ? lastModifiedAt : Instant.EPOCH
        );
    }
    
    /**
     * Static factory methods
     */
    
    public static TradingStrategy trendFollowingStrategy(String name, Long userId, List<String> symbols) {
        return TradingStrategy.builder()
            .strategyId("TREND_" + System.currentTimeMillis())
            .strategyName(name)
            .userId(userId)
            .strategyType("TREND_FOLLOWING")
            .strategyCategory("SYSTEMATIC")
            .version("1.0.0")
            .createdAt(Instant.now())
            .configuration(StrategyConfig.builder()
                .timeframe("1h")
                .symbols(symbols)
                .assetClasses(List.of("EQUITY"))
                .signalConfig(SignalConfig.builder()
                    .signalType("BUY_SELL")
                    .minSignalStrength(new BigDecimal("0.7"))
                    .signalTimeoutMinutes(60)
                    .build())
                .build())
            .status(StrategyStatus.builder()
                .currentState("CREATED")
                .deploymentEnvironment("PAPER")
                .healthStatus("HEALTHY")
                .build())
            .build();
    }
    
    public static TradingStrategy meanReversionStrategy(String name, Long userId, List<String> symbols) {
        return TradingStrategy.builder()
            .strategyId("MEAN_REV_" + System.currentTimeMillis())
            .strategyName(name)
            .userId(userId)
            .strategyType("MEAN_REVERSION")
            .strategyCategory("SYSTEMATIC")
            .version("1.0.0")
            .createdAt(Instant.now())
            .configuration(StrategyConfig.builder()
                .timeframe("15m")
                .symbols(symbols)
                .assetClasses(List.of("EQUITY"))
                .signalConfig(SignalConfig.builder()
                    .signalType("BUY_SELL")
                    .minSignalStrength(new BigDecimal("0.8"))
                    .signalTimeoutMinutes(30)
                    .build())
                .build())
            .status(StrategyStatus.builder()
                .currentState("CREATED")
                .deploymentEnvironment("PAPER")
                .healthStatus("HEALTHY")
                .build())
            .build();
    }
}