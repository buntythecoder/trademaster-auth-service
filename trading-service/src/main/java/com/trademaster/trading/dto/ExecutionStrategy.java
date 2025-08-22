package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Execution Strategy DTO
 * 
 * Comprehensive execution strategy configuration with:
 * - Algorithm-specific parameters
 * - Venue selection criteria
 * - Risk controls and limits
 * - Performance targets
 * - Market condition adaptations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStrategy {
    
    /**
     * Strategy Identification
     */
    private String strategyId;
    private String strategyName;
    private String strategyType; // TWAP, VWAP, IS, ARRIVAL_PRICE, ICEBERG, POV, etc.
    private String description;
    private String version;
    private Long userId;
    private Boolean active;
    
    /**
     * Algorithm Parameters
     */
    private AlgorithmConfig algorithmConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmConfig {
        private String algorithmType; // TWAP, VWAP, IMPLEMENTATION_SHORTFALL, etc.
        private Integer timeHorizonMinutes; // Total execution time
        private Integer sliceCount; // Number of child orders
        private BigDecimal participationRate; // Market participation rate (0.0-1.0)
        private BigDecimal aggressiveness; // Execution aggressiveness (0.0-1.0)
        private BigDecimal riskAversion; // Risk aversion parameter (0.0-1.0)
        private Boolean adaptiveSchedule; // Adapt to market conditions
        private String scheduleType; // LINEAR, FRONTLOADED, BACKLOADED, ADAPTIVE
        private Map<String, Object> customParameters; // Algorithm-specific parameters
    }
    
    /**
     * TWAP Strategy Configuration
     */
    private TWAPConfig twapConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TWAPConfig {
        private Integer executionPeriodMinutes; // Total execution period
        private Integer sliceIntervalMinutes; // Time between slices
        private Integer minSliceSize; // Minimum slice size
        private Integer maxSliceSize; // Maximum slice size
        private Boolean randomizeSlices; // Add randomization to slices
        private BigDecimal maxDeviation; // Max deviation from TWAP schedule
        private String pricingModel; // MIDPOINT, BEST_BID_OFFER, LAST
        private Boolean respectLimits; // Respect limit prices
    }
    
    /**
     * VWAP Strategy Configuration
     */
    private VWAPConfig vwapConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VWAPConfig {
        private BigDecimal targetParticipationRate; // Target participation rate
        private BigDecimal maxParticipationRate; // Maximum participation rate
        private Integer lookbackPeriodDays; // Historical volume lookback
        private String volumeProfile; // INTRADAY, HISTORICAL, FORECAST
        private Boolean adaptToVolume; // Adapt to real-time volume
        private BigDecimal volumeForecastAccuracy; // Volume forecast confidence
        private String aggressionSchedule; // PASSIVE, NEUTRAL, AGGRESSIVE
        private Boolean respectVolumeConstraints; // Respect market volume limits
    }
    
    /**
     * Implementation Shortfall Configuration
     */
    private ImplementationShortfallConfig isConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImplementationShortfallConfig {
        private BigDecimal riskAversion; // Risk aversion coefficient
        private BigDecimal temporaryImpactCoeff; // Temporary impact coefficient
        private BigDecimal permanentImpactCoeff; // Permanent impact coefficient
        private BigDecimal volatilityEstimate; // Volatility estimate
        private String optimizationObjective; // MINIMIZE_COST, MINIMIZE_RISK, BALANCED
        private Boolean dynamicOptimization; // Real-time optimization
        private Integer rebalanceIntervalMinutes; // Rebalance frequency
        private BigDecimal urgencyFactor; // Execution urgency
    }
    
    /**
     * Venue Selection Criteria
     */
    private VenueSelection venueSelection;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueSelection {
        private List<String> preferredVenues; // Preferred execution venues
        private List<String> excludedVenues; // Excluded venues
        private String selectionCriteria; // PRICE, LIQUIDITY, SPEED, COST, BALANCED
        private BigDecimal minLiquidityThreshold; // Minimum liquidity requirement
        private Long maxLatencyMicros; // Maximum acceptable latency
        private Boolean enableDarkPools; // Allow dark pool routing
        private Boolean enableCrossingNetworks; // Allow crossing networks
        private String routingStrategy; // SMART, SEQUENTIAL, PARALLEL, WEIGHTED
        private Map<String, BigDecimal> venueWeights; // Venue allocation weights
    }
    
    /**
     * Risk Controls
     */
    private RiskControls riskControls;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskControls {
        private BigDecimal maxOrderValue; // Maximum single order value
        private BigDecimal maxParticipationRate; // Maximum participation rate
        private Integer maxSliceSize; // Maximum slice size
        private BigDecimal priceCollar; // Price collar percentage
        private BigDecimal maxSpreadWidth; // Maximum spread width
        private Boolean enableCircuitBreaker; // Enable circuit breaker
        private BigDecimal maxMarketImpact; // Maximum acceptable market impact
        private Integer maxRetries; // Maximum retry attempts
        private List<String> riskChecks; // Risk checks to apply
        private Boolean emergencyCancel; // Enable emergency cancel
    }
    
    /**
     * Performance Targets
     */
    private PerformanceTargets performanceTargets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceTargets {
        private Long maxLatencyMicros; // Target latency in microseconds
        private BigDecimal minFillRate; // Minimum fill rate percentage
        private BigDecimal maxImplementationShortfall; // Maximum implementation shortfall
        private BigDecimal targetVWAPDeviation; // Target VWAP deviation
        private String benchmarkType; // ARRIVAL_PRICE, VWAP, TWAP, CLOSE
        private BigDecimal maxSlippage; // Maximum slippage tolerance
        private Integer maxExecutionTime; // Maximum execution time minutes
        private BigDecimal minExecutionQuality; // Minimum quality score
    }
    
    /**
     * Market Condition Adaptations
     */
    private MarketAdaptations marketAdaptations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketAdaptations {
        private Boolean adaptToVolatility; // Adapt to market volatility
        private Boolean adaptToLiquidity; // Adapt to market liquidity
        private Boolean adaptToSpread; // Adapt to bid-ask spread
        private String volatilityResponse; // SLOW_DOWN, SPEED_UP, PAUSE, CONTINUE
        private String lowLiquidityResponse; // WAIT, AGGRESSIVE, CANCEL, ICEBERG
        private String wideSpreadResponse; // LIMIT, MARKET, MIDPOINT, PASSIVE
        private Map<String, String> conditionOverrides; // Market condition overrides
        private Boolean enableCircuitBreakers; // Enable market circuit breakers
    }
    
    /**
     * Timing Configuration
     */
    private TimingConfig timingConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimingConfig {
        private LocalTime startTime; // Strategy start time
        private LocalTime endTime; // Strategy end time
        private List<String> excludedDays; // Excluded days of week
        private Boolean respectMarketHours; // Only execute during market hours
        private String timezone; // Timezone for timing
        private Integer maxExecutionDays; // Maximum execution days
        private Boolean pauseOnNews; // Pause execution on news events
        private List<String> newsCategories; // News categories to monitor
    }
    
    /**
     * Order Management
     */
    private OrderManagement orderManagement;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderManagement {
        private String orderType; // MARKET, LIMIT, STOP, STOP_LIMIT
        private String timeInForce; // DAY, GTC, IOC, FOK
        private Boolean hiddenOrders; // Use hidden/iceberg orders
        private Integer visibleQuantity; // Visible quantity for iceberg
        private String priceStrategy; // AGGRESSIVE, PASSIVE, MIDPOINT, SMART
        private Boolean enableOrderReplace; // Allow order replacement
        private Integer replaceThresholdBps; // Price movement threshold for replace
        private String cancellationPolicy; // AGGRESSIVE, NORMAL, CONSERVATIVE
    }
    
    /**
     * Monitoring and Alerts
     */
    private MonitoringConfig monitoringConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringConfig {
        private Boolean enableRealTimeMonitoring; // Real-time monitoring
        private Integer reportingIntervalSeconds; // Reporting frequency
        private List<String> alertConditions; // Alert conditions
        private String alertChannel; // EMAIL, SMS, PUSH, SYSTEM
        private Boolean enablePerformanceTracking; // Performance tracking
        private String dashboardLevel; // BASIC, DETAILED, ADVANCED
        private Boolean logExecutionDetails; // Log detailed execution info
        private Integer dataRetentionDays; // Data retention period
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if strategy is time-based
     */
    public boolean isTimeBased() {
        return "TWAP".equals(strategyType) || "TIME_SLICED".equals(strategyType);
    }
    
    /**
     * Check if strategy is volume-based
     */
    public boolean isVolumeBased() {
        return "VWAP".equals(strategyType) || "POV".equals(strategyType);
    }
    
    /**
     * Check if strategy supports market adaptation
     */
    public boolean supportsMarketAdaptation() {
        return marketAdaptations != null && 
               (marketAdaptations.getAdaptToVolatility() || 
                marketAdaptations.getAdaptToLiquidity() || 
                marketAdaptations.getAdaptToSpread());
    }
    
    /**
     * Get execution urgency level
     */
    public String getUrgencyLevel() {
        if (algorithmConfig == null || algorithmConfig.getAggressiveness() == null) {
            return "NORMAL";
        }
        
        BigDecimal aggressiveness = algorithmConfig.getAggressiveness();
        if (aggressiveness.compareTo(new BigDecimal("0.8")) > 0) {
            return "HIGH";
        } else if (aggressiveness.compareTo(new BigDecimal("0.3")) < 0) {
            return "LOW";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * Calculate estimated execution time
     */
    public Integer getEstimatedExecutionTimeMinutes() {
        if (algorithmConfig != null && algorithmConfig.getTimeHorizonMinutes() != null) {
            return algorithmConfig.getTimeHorizonMinutes();
        }
        
        if (twapConfig != null && twapConfig.getExecutionPeriodMinutes() != null) {
            return twapConfig.getExecutionPeriodMinutes();
        }
        
        // Default estimation based on strategy type
        return switch (strategyType != null ? strategyType : "MARKET") {
            case "MARKET" -> 1;
            case "TWAP" -> 60;
            case "VWAP" -> 240; // 4 hours
            case "IMPLEMENTATION_SHORTFALL" -> 120;
            case "ICEBERG" -> 180;
            default -> 30;
        };
    }
    
    /**
     * Validate strategy configuration
     */
    public List<String> validateConfiguration() {
        List<String> errors = new java.util.ArrayList<>();
        
        if (strategyType == null) {
            errors.add("Strategy type is required");
        }
        
        if ("TWAP".equals(strategyType) && twapConfig == null) {
            errors.add("TWAP configuration is required for TWAP strategy");
        }
        
        if ("VWAP".equals(strategyType) && vwapConfig == null) {
            errors.add("VWAP configuration is required for VWAP strategy");
        }
        
        if (algorithmConfig != null && algorithmConfig.getParticipationRate() != null) {
            BigDecimal participationRate = algorithmConfig.getParticipationRate();
            if (participationRate.compareTo(BigDecimal.ZERO) <= 0 || 
                participationRate.compareTo(BigDecimal.ONE) > 0) {
                errors.add("Participation rate must be between 0.0 and 1.0");
            }
        }
        
        return errors;
    }
    
    /**
     * Get strategy summary
     */
    public Map<String, Object> getStrategySummary() {
        return Map.of(
            "strategyId", strategyId != null ? strategyId : "N/A",
            "strategyName", strategyName != null ? strategyName : "N/A",
            "strategyType", strategyType != null ? strategyType : "UNKNOWN",
            "urgencyLevel", getUrgencyLevel(),
            "estimatedTimeMinutes", getEstimatedExecutionTimeMinutes(),
            "supportsAdaptation", supportsMarketAdaptation(),
            "isTimeBased", isTimeBased(),
            "isVolumeBased", isVolumeBased(),
            "active", active != null ? active : false
        );
    }
    
    /**
     * Static factory methods
     */
    public static ExecutionStrategy marketStrategy() {
        return ExecutionStrategy.builder()
            .strategyType("MARKET")
            .strategyName("Market Order")
            .description("Immediate market execution")
            .algorithmConfig(AlgorithmConfig.builder()
                .algorithmType("MARKET")
                .aggressiveness(BigDecimal.ONE)
                .build())
            .active(true)
            .build();
    }
    
    public static ExecutionStrategy twapStrategy(Integer periodMinutes) {
        return ExecutionStrategy.builder()
            .strategyType("TWAP")
            .strategyName("Time-Weighted Average Price")
            .description("Execute over time to achieve TWAP")
            .algorithmConfig(AlgorithmConfig.builder()
                .algorithmType("TWAP")
                .timeHorizonMinutes(periodMinutes)
                .aggressiveness(new BigDecimal("0.5"))
                .build())
            .twapConfig(TWAPConfig.builder()
                .executionPeriodMinutes(periodMinutes)
                .sliceIntervalMinutes(Math.max(1, periodMinutes / 20))
                .pricingModel("MIDPOINT")
                .respectLimits(true)
                .build())
            .active(true)
            .build();
    }
    
    public static ExecutionStrategy vwapStrategy(BigDecimal participationRate) {
        return ExecutionStrategy.builder()
            .strategyType("VWAP")
            .strategyName("Volume-Weighted Average Price")
            .description("Execute following volume profile to achieve VWAP")
            .algorithmConfig(AlgorithmConfig.builder()
                .algorithmType("VWAP")
                .participationRate(participationRate)
                .aggressiveness(new BigDecimal("0.6"))
                .build())
            .vwapConfig(VWAPConfig.builder()
                .targetParticipationRate(participationRate)
                .maxParticipationRate(participationRate.multiply(new BigDecimal("1.5")))
                .lookbackPeriodDays(20)
                .adaptToVolume(true)
                .build())
            .active(true)
            .build();
    }
}