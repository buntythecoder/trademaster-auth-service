package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Strategy Deployment DTO
 * 
 * Comprehensive strategy deployment configuration and status:
 * - Deployment environment and settings
 * - Resource allocation and scaling configuration
 * - Risk limits and safety controls
 * - Monitoring and alerting setup
 * - Performance tracking and health metrics
 * - Rollback and recovery procedures
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDeployment {
    
    /**
     * Deployment Identification
     */
    private String deploymentId;
    private String strategyId;
    private String strategyName;
    private String version;
    private Long userId;
    private Instant deployedAt;
    private String deployedBy;
    private Instant lastUpdatedAt;
    
    /**
     * Deployment Configuration
     */
    private DeploymentConfig config;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentConfig {
        // Environment Settings
        private String environment; // PAPER, LIVE, SANDBOX
        private String region; // Deployment region
        private String datacenter; // Target datacenter
        private String computeSize; // SMALL, MEDIUM, LARGE, XLARGE
        private Boolean highAvailability; // Enable HA deployment
        private Integer replicas; // Number of replicas
        
        // Capital Allocation
        private BigDecimal allocatedCapital; // Capital allocated to strategy
        private BigDecimal maxDailyRisk; // Maximum daily risk amount
        private BigDecimal emergencyStopLoss; // Emergency stop loss level
        private Boolean enableCapitalScaling; // Scale capital based on performance
        private BigDecimal scalingFactor; // Capital scaling factor
        
        // Execution Settings
        private List<String> enabledMarkets; // Enabled trading markets
        private List<String> enabledAssetClasses; // Enabled asset classes
        private String executionMode; // AGGRESSIVE, BALANCED, CONSERVATIVE
        private Boolean enableAfterHoursTrading; // After hours trading
        private String riskProfile; // LOW, MEDIUM, HIGH
        
        // Market Data Configuration
        private List<String> dataProviders; // Market data providers
        private String dataLatencyTier; // REAL_TIME, DELAYED, END_OF_DAY
        private Boolean enableLevel2Data; // Level 2 market data
        private Boolean enableNewsData; // News and sentiment data
        
        // Integration Settings
        private List<String> brokerConnections; // Connected brokers
        private Map<String, Object> brokerSettings; // Broker-specific settings
        private Boolean enableOrderRouting; // Smart order routing
        private List<String> preferredVenues; // Preferred execution venues
    }
    
    /**
     * Risk Configuration
     */
    private RiskConfiguration riskConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskConfiguration {
        // Position Limits
        private BigDecimal maxPositionSize; // Maximum position size
        private BigDecimal maxPortfolioWeight; // Maximum portfolio weight
        private Integer maxConcurrentPositions; // Maximum open positions
        private BigDecimal sectorConcentrationLimit; // Sector concentration limit
        
        // Loss Limits
        private BigDecimal dailyLossLimit; // Daily loss limit
        private BigDecimal weeklyLossLimit; // Weekly loss limit
        private BigDecimal monthlyLossLimit; // Monthly loss limit
        private BigDecimal maxDrawdownLimit; // Maximum drawdown limit
        private BigDecimal stopLossPercent; // Global stop loss percentage
        
        // Risk Monitoring
        private Integer riskCheckIntervalSeconds; // Risk check frequency
        private Boolean enableRealTimeRisk; // Real-time risk monitoring
        private Boolean autoLiquidateOnViolation; // Auto liquidate on risk violation
        private List<String> riskAlertMethods; // Alert methods
        
        // Circuit Breakers
        private Boolean enableCircuitBreakers; // Enable circuit breakers
        private BigDecimal volatilityThreshold; // Volatility circuit breaker
        private BigDecimal drawdownThreshold; // Drawdown circuit breaker
        private Integer consecutiveLossThreshold; // Consecutive loss breaker
        
        // Emergency Controls
        private Boolean enableEmergencyStop; // Emergency stop capability
        private List<String> emergencyTriggers; // Emergency stop triggers
        private String emergencyContactMethod; // Emergency contact method
        private Boolean requireManagerApproval; // Manager approval for restart
    }
    
    /**
     * Monitoring Configuration
     */
    private MonitoringConfig monitoringConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringConfig {
        // Performance Monitoring
        private Boolean enablePerformanceTracking; // Track performance metrics
        private Integer performanceUpdateIntervalSeconds; // Update frequency
        private List<String> performanceMetrics; // Metrics to track
        private Boolean enableBenchmarkComparison; // Compare to benchmarks
        
        // Health Monitoring
        private Boolean enableHealthChecks; // Enable health monitoring
        private Integer healthCheckIntervalSeconds; // Health check frequency
        private List<String> healthMetrics; // Health metrics to monitor
        private BigDecimal unhealthyThreshold; // Unhealthy threshold
        
        // Alert Configuration
        private List<AlertRule> alertRules; // Alert rules
        private Map<String, String> alertChannels; // Alert channels (email, slack, etc.)
        private String alertSeverityLevel; // Alert severity level
        private Boolean enableAlertEscalation; // Escalate critical alerts
        
        // Logging Configuration
        private String logLevel; // DEBUG, INFO, WARN, ERROR
        private Boolean enableTradeLogging; // Log all trades
        private Boolean enableSignalLogging; // Log all signals
        private Boolean enablePerformanceLogging; // Log performance data
        private Integer logRetentionDays; // Log retention period
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertRule {
        private String ruleName;
        private String metric; // Metric to monitor
        private String condition; // GT, LT, EQ
        private BigDecimal threshold; // Alert threshold
        private String severity; // INFO, WARNING, CRITICAL
        private Boolean enabled;
    }
    
    /**
     * Deployment Status
     */
    private DeploymentStatus status;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentStatus {
        private String currentState; // DEPLOYING, RUNNING, PAUSED, STOPPED, ERROR, ROLLBACK
        private String healthStatus; // HEALTHY, WARNING, CRITICAL, UNKNOWN
        private LocalDateTime lastStateChange; // Last state change time
        private String stateChangeReason; // Reason for state change
        
        // Operational Status
        private Boolean isActive; // Currently processing signals
        private LocalDateTime lastSignalTime; // Last signal generation
        private LocalDateTime lastTradeTime; // Last trade execution
        private Integer signalsGeneratedToday; // Signals generated today
        private Integer tradesExecutedToday; // Trades executed today
        
        // Error Tracking
        private String lastError; // Last error message
        private LocalDateTime lastErrorTime; // Last error time
        private Integer errorCount24h; // Errors in last 24 hours
        private List<String> recentErrors; // Recent error messages
        private Boolean requiresAttention; // Requires manual attention
        
        // Performance Indicators
        private BigDecimal currentPnL; // Current P&L
        private BigDecimal todayPnL; // Today's P&L
        private BigDecimal currentDrawdown; // Current drawdown
        private BigDecimal utilizationPercent; // Capital utilization
        private String performanceRating; // Current performance rating
    }
    
    /**
     * Resource Usage
     */
    private ResourceUsage resourceUsage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsage {
        // Compute Resources
        private BigDecimal cpuUsagePercent; // CPU usage percentage
        private Long memoryUsageBytes; // Memory usage in bytes
        private Long memoryLimitBytes; // Memory limit
        private BigDecimal diskUsagePercent; // Disk usage percentage
        private Long networkBytesTransferred; // Network data transferred
        
        // Database Resources
        private Integer databaseConnections; // Active DB connections
        private Long databaseQueriesExecuted; // DB queries executed
        private BigDecimal averageQueryTimeMs; // Average query time
        
        // Market Data Resources
        private Integer dataSubscriptions; // Active data subscriptions
        private Long messagesProcessed; // Data messages processed
        private BigDecimal messageProcessingLatency; // Processing latency
        
        // Cost Tracking
        private BigDecimal hourlyComputeCost; // Hourly compute cost
        private BigDecimal dailyDataCost; // Daily data cost
        private BigDecimal monthlyEstimatedCost; // Monthly cost estimate
        private String costOptimizationLevel; // Cost optimization level
    }
    
    /**
     * Deployment History
     */
    private List<DeploymentEvent> deploymentHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentEvent {
        private Instant timestamp;
        private String eventType; // DEPLOY, START, STOP, PAUSE, ERROR, ROLLBACK
        private String description;
        private String initiatedBy;
        private Map<String, Object> eventData;
        private String severity; // INFO, WARNING, ERROR
    }
    
    /**
     * Rollback Configuration
     */
    private RollbackConfig rollbackConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackConfig {
        private Boolean enableAutoRollback; // Enable automatic rollback
        private List<String> rollbackTriggers; // Conditions that trigger rollback
        private String previousVersion; // Previous stable version
        private BigDecimal rollbackThreshold; // Performance threshold for rollback
        private Integer rollbackTimeoutMinutes; // Rollback timeout
        private Boolean preservePositions; // Preserve open positions on rollback
        private String rollbackStrategy; // IMMEDIATE, GRACEFUL, SCHEDULED
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if deployment is running
     */
    public boolean isRunning() {
        return status != null && "RUNNING".equals(status.getCurrentState());
    }
    
    /**
     * Check if deployment is healthy
     */
    public boolean isHealthy() {
        return status != null && "HEALTHY".equals(status.getHealthStatus());
    }
    
    /**
     * Check if deployment is in production
     */
    public boolean isProduction() {
        return config != null && "LIVE".equals(config.getEnvironment());
    }
    
    /**
     * Check if deployment requires attention
     */
    public boolean requiresAttention() {
        if (status == null) return true;
        
        return Boolean.TRUE.equals(status.getRequiresAttention()) ||
               "CRITICAL".equals(status.getHealthStatus()) ||
               "ERROR".equals(status.getCurrentState());
    }
    
    /**
     * Get resource efficiency score
     */
    public BigDecimal getResourceEfficiency() {
        if (resourceUsage == null) return BigDecimal.ZERO;
        
        // Simple efficiency calculation based on CPU and memory usage
        BigDecimal cpuEfficiency = resourceUsage.getCpuUsagePercent() != null ?
            new BigDecimal("100").subtract(resourceUsage.getCpuUsagePercent()) : BigDecimal.ZERO;
        
        BigDecimal memoryEfficiency = BigDecimal.ZERO;
        if (resourceUsage.getMemoryUsageBytes() != null && resourceUsage.getMemoryLimitBytes() != null) {
            BigDecimal memoryPercent = new BigDecimal(resourceUsage.getMemoryUsageBytes())
                .divide(new BigDecimal(resourceUsage.getMemoryLimitBytes()), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            memoryEfficiency = new BigDecimal("100").subtract(memoryPercent);
        }
        
        return cpuEfficiency.add(memoryEfficiency).divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get deployment uptime in hours
     */
    public Long getUptimeHours() {
        if (deployedAt == null) return 0L;
        return java.time.Duration.between(deployedAt, Instant.now()).toHours();
    }
    
    /**
     * Get deployment summary
     */
    public Map<String, Object> getDeploymentSummary() {
        return Map.of(
            "deploymentId", deploymentId != null ? deploymentId : "N/A",
            "strategyName", strategyName != null ? strategyName : "Unknown",
            "environment", config != null && config.getEnvironment() != null ? 
                         config.getEnvironment() : "UNKNOWN",
            "state", status != null && status.getCurrentState() != null ? 
                    status.getCurrentState() : "UNKNOWN",
            "health", status != null && status.getHealthStatus() != null ? 
                     status.getHealthStatus() : "UNKNOWN",
            "isRunning", isRunning(),
            "isHealthy", isHealthy(),
            "requiresAttention", requiresAttention(),
            "uptimeHours", getUptimeHours(),
            "currentPnL", status != null && status.getCurrentPnL() != null ? 
                         status.getCurrentPnL() : BigDecimal.ZERO,
            "deployedAt", deployedAt != null ? deployedAt : Instant.EPOCH
        );
    }
    
    /**
     * Static factory methods
     */
    public static StrategyDeployment paperTradingDeployment(String strategyId, String strategyName, Long userId) {
        return StrategyDeployment.builder()
            .deploymentId("PAPER_" + System.currentTimeMillis())
            .strategyId(strategyId)
            .strategyName(strategyName)
            .userId(userId)
            .deployedAt(Instant.now())
            .config(DeploymentConfig.builder()
                .environment("PAPER")
                .region("us-east-1")
                .computeSize("MEDIUM")
                .highAvailability(false)
                .replicas(1)
                .allocatedCapital(new BigDecimal("100000"))
                .maxDailyRisk(new BigDecimal("1000"))
                .executionMode("BALANCED")
                .dataLatencyTier("REAL_TIME")
                .build())
            .riskConfig(RiskConfiguration.builder()
                .maxPositionSize(new BigDecimal("10000"))
                .dailyLossLimit(new BigDecimal("1000"))
                .maxDrawdownLimit(new BigDecimal("5000"))
                .enableCircuitBreakers(true)
                .enableEmergencyStop(true)
                .build())
            .status(DeploymentStatus.builder()
                .currentState("RUNNING")
                .healthStatus("HEALTHY")
                .lastStateChange(LocalDateTime.now())
                .isActive(true)
                .currentPnL(BigDecimal.ZERO)
                .todayPnL(BigDecimal.ZERO)
                .performanceRating("GOOD")
                .build())
            .build();
    }
    
    public static StrategyDeployment liveDeployment(String strategyId, String strategyName, Long userId,
                                                  BigDecimal allocatedCapital) {
        return StrategyDeployment.builder()
            .deploymentId("LIVE_" + System.currentTimeMillis())
            .strategyId(strategyId)
            .strategyName(strategyName)
            .userId(userId)
            .deployedAt(Instant.now())
            .config(DeploymentConfig.builder()
                .environment("LIVE")
                .region("us-east-1")
                .computeSize("LARGE")
                .highAvailability(true)
                .replicas(2)
                .allocatedCapital(allocatedCapital)
                .maxDailyRisk(allocatedCapital.multiply(new BigDecimal("0.02"))) // 2% daily risk
                .executionMode("BALANCED")
                .dataLatencyTier("REAL_TIME")
                .enableLevel2Data(true)
                .build())
            .riskConfig(RiskConfiguration.builder()
                .maxPositionSize(allocatedCapital.multiply(new BigDecimal("0.1"))) // 10% max position
                .dailyLossLimit(allocatedCapital.multiply(new BigDecimal("0.02"))) // 2% daily loss
                .maxDrawdownLimit(allocatedCapital.multiply(new BigDecimal("0.1"))) // 10% max drawdown
                .enableCircuitBreakers(true)
                .enableEmergencyStop(true)
                .enableRealTimeRisk(true)
                .requireManagerApproval(true)
                .build())
            .status(DeploymentStatus.builder()
                .currentState("DEPLOYING")
                .healthStatus("UNKNOWN")
                .lastStateChange(LocalDateTime.now())
                .isActive(false)
                .performanceRating("PENDING")
                .build())
            .build();
    }
}