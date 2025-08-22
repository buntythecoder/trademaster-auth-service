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
 * Stress Test Result DTO
 * 
 * Comprehensive stress testing results with:
 * - Multiple scenario analysis
 * - Portfolio impact assessment
 * - Risk factor sensitivities
 * - Recovery strategies
 * - Historical scenario analysis
 * - Monte Carlo simulations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StressTestResult {
    
    /**
     * Test Metadata
     */
    private String testId;
    private Long userId;
    private String scenarioName;
    private String scenarioDescription;
    private Instant testExecutedAt;
    private LocalDate testDate;
    private String testType; // HISTORICAL, HYPOTHETICAL, MONTE_CARLO, REGULATORY
    private String scenarioCategory; // MARKET_CRASH, VOLATILITY_SPIKE, INTEREST_RATE_SHOCK, etc.
    private Integer simulationRuns; // Number of Monte Carlo runs
    
    /**
     * Scenario Configuration
     */
    private ScenarioConfiguration scenarioConfig;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioConfiguration {
        private Map<String, BigDecimal> marketShocks; // Asset price shocks
        private Map<String, BigDecimal> volatilityShocks; // Volatility shocks
        private BigDecimal interestRateShock; // Interest rate change
        private BigDecimal currencyShock; // Currency movement
        private BigDecimal liquidityShock; // Liquidity impact
        private BigDecimal correlationShift; // Correlation changes
        private Integer timeHorizonDays; // Scenario time horizon
        private BigDecimal probability; // Scenario probability
        private String severity; // MILD, MODERATE, SEVERE, EXTREME
    }
    
    /**
     * Portfolio Impact
     */
    private PortfolioImpact portfolioImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioImpact {
        private BigDecimal totalPnL; // Total P&L impact
        private BigDecimal portfolioReturn; // Portfolio return in scenario
        private BigDecimal portfolioValue; // Portfolio value post-scenario
        private BigDecimal maxLoss; // Maximum potential loss
        private BigDecimal averageLoss; // Average loss in simulations
        private BigDecimal probabilityOfLoss; // Probability of loss
        private BigDecimal valueAtRisk; // VaR in scenario
        private BigDecimal expectedShortfall; // Expected shortfall
        private BigDecimal recoveryTime; // Expected recovery time
        private BigDecimal capitalDepletion; // Capital depletion amount
    }
    
    /**
     * Position-Level Impact
     */
    private List<PositionImpact> positionImpacts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionImpact {
        private String symbol;
        private String exchange;
        private BigDecimal currentValue;
        private BigDecimal stressedValue;
        private BigDecimal pnlImpact;
        private BigDecimal returnImpact;
        private BigDecimal priceShock; // Applied price shock
        private BigDecimal volatilityImpact; // Volatility impact
        private String riskContribution; // HIGH, MEDIUM, LOW
        private BigDecimal betaToScenario; // Position beta to scenario
    }
    
    /**
     * Sector-Level Impact
     */
    private List<SectorImpact> sectorImpacts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorImpact {
        private String sectorName;
        private BigDecimal currentExposure;
        private BigDecimal pnlImpact;
        private BigDecimal returnImpact;
        private Integer positionsAffected;
        private BigDecimal concentrationRisk;
        private String impactSeverity; // MINIMAL, LOW, MODERATE, HIGH, SEVERE
        private List<String> topAffectedSymbols;
    }
    
    /**
     * Risk Factor Analysis
     */
    private RiskFactorAnalysis riskFactorAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactorAnalysis {
        private Map<String, BigDecimal> factorSensitivities; // Sensitivity to risk factors
        private BigDecimal marketRiskContribution; // Market risk contribution
        private BigDecimal specificRiskContribution; // Specific risk contribution
        private BigDecimal correlationEffect; // Correlation effect
        private BigDecimal liquidityEffect; // Liquidity effect
        private BigDecimal leverageEffect; // Leverage effect
        private Map<String, BigDecimal> greeksSensitivities; // Greeks sensitivities
        private BigDecimal nonLinearityEffect; // Non-linearity effects
    }
    
    /**
     * Tail Risk Analysis
     */
    private TailRiskAnalysis tailRiskAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TailRiskAnalysis {
        private BigDecimal percentile99Loss; // 99th percentile loss
        private BigDecimal percentile95Loss; // 95th percentile loss
        private BigDecimal percentile90Loss; // 90th percentile loss
        private BigDecimal worstCaseLoss; // Worst case scenario loss
        private BigDecimal tailExpectation; // Tail expectation
        private BigDecimal extremeVaR; // Extreme VaR
        private BigDecimal blackSwanProbability; // Black swan probability
        private List<String> tailRiskFactors; // Main tail risk contributors
    }
    
    /**
     * Liquidity Impact
     */
    private LiquidityImpact liquidityImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityImpact {
        private BigDecimal liquidationCost; // Cost of liquidation
        private Integer liquidationTimeDays; // Time to liquidate
        private BigDecimal bidAskSpreadIncrease; // Bid-ask spread widening
        private BigDecimal marketImpactCost; // Market impact cost
        private BigDecimal liquidityPremium; // Liquidity premium required
        private Map<String, Integer> liquidationSchedule; // Liquidation by asset
        private BigDecimal cashGenerationAbility; // Cash generation capability
        private String liquidityStress; // MILD, MODERATE, SEVERE
    }
    
    /**
     * Correlation Breakdown
     */
    private CorrelationBreakdown correlationBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrelationBreakdown {
        private BigDecimal averageCorrelation; // Average correlation in scenario
        private BigDecimal correlationIncrease; // Correlation increase
        private BigDecimal diversificationBenefit; // Remaining diversification
        private BigDecimal correlationRisk; // Risk from correlation increase
        private Map<String, BigDecimal> pairwiseCorrelations; // Pairwise correlations
        private BigDecimal systemicRisk; // Systemic risk component
        private String correlationRegime; // HIGH, MODERATE, LOW
    }
    
    /**
     * Recovery Analysis
     */
    private RecoveryAnalysis recoveryAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecoveryAnalysis {
        private Integer expectedRecoveryDays; // Expected recovery time
        private BigDecimal recoveryProbability; // Probability of full recovery
        private List<RecoveryPath> recoveryPaths; // Possible recovery paths
        private BigDecimal partialRecoveryLevel; // Partial recovery level
        private List<String> recoveryStrategies; // Recovery strategies
        private BigDecimal hedgingCost; // Cost of hedging
        private BigDecimal opportunityCost; // Opportunity cost during recovery
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecoveryPath {
        private String pathName;
        private Integer timeHorizonDays;
        private BigDecimal recoveryLevel; // % of losses recovered
        private BigDecimal probability;
        private List<String> requiredActions;
        private BigDecimal estimatedCost;
    }
    
    /**
     * Regulatory Impact
     */
    private RegulatoryImpact regulatoryImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryImpact {
        private Boolean capitalAdequacyBreach; // Capital adequacy breach
        private BigDecimal capitalShortfall; // Capital shortfall amount
        private List<String> regulatoryViolations; // Regulatory violations
        private BigDecimal additionalCapitalRequired; // Additional capital needed
        private String regulatoryAction; // NONE, WARNING, CORRECTIVE, PUNITIVE
        private Map<String, BigDecimal> complianceMetrics; // Compliance metrics
        private Boolean systemicRiskFlag; // Systemic risk indicator
    }
    
    /**
     * Mitigation Strategies
     */
    private List<MitigationStrategy> mitigationStrategies;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MitigationStrategy {
        private String strategyName;
        private String strategyType; // HEDGING, DIVERSIFICATION, POSITION_REDUCTION
        private String description;
        private BigDecimal riskReduction; // Risk reduction %
        private BigDecimal implementationCost; // Cost to implement
        private Integer implementationTime; // Time to implement (days)
        private BigDecimal effectivenessScore; // Effectiveness score (0-100)
        private List<String> requiredActions; // Required actions
        private String priority; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Model Validation
     */
    private ModelValidation modelValidation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelValidation {
        private BigDecimal modelConfidence; // Model confidence score
        private BigDecimal backtestAccuracy; // Backtest accuracy
        private String modelLimitations; // Model limitations
        private List<String> validationTests; // Validation tests performed
        private BigDecimal residualRisk; // Unexplained residual risk
        private String dataQuality; // EXCELLENT, GOOD, FAIR, POOR
        private Integer lookBackPeriod; // Data look-back period
        private List<String> modelAssumptions; // Key model assumptions
    }
    
    /**
     * Performance Metrics
     */
    private PerformanceMetrics performanceMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Long executionTimeMs; // Test execution time
        private Integer scenariosGenerated; // Number of scenarios generated
        private Integer convergenceIterations; // Monte Carlo convergence
        private BigDecimal computationalComplexity; // Computational complexity
        private String resourceUtilization; // HIGH, MEDIUM, LOW
        private List<String> performanceBottlenecks; // Performance bottlenecks
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if result indicates severe stress
     */
    public boolean isSevereStress() {
        return portfolioImpact != null && 
               portfolioImpact.getMaxLoss() != null &&
               portfolioImpact.getMaxLoss().compareTo(new BigDecimal("-20.0")) <= 0; // >20% loss
    }
    
    /**
     * Get worst affected positions
     */
    public List<PositionImpact> getWorstAffectedPositions(int limit) {
        if (positionImpacts == null) return List.of();
        
        return positionImpacts.stream()
            .sorted((a, b) -> a.getPnlImpact().compareTo(b.getPnlImpact())) // Ascending (most negative first)
            .limit(limit)
            .toList();
    }
    
    /**
     * Get high priority mitigation strategies
     */
    public List<MitigationStrategy> getHighPriorityMitigations() {
        if (mitigationStrategies == null) return List.of();
        
        return mitigationStrategies.stream()
            .filter(strategy -> "HIGH".equals(strategy.getPriority()))
            .toList();
    }
    
    /**
     * Calculate total recovery cost
     */
    public BigDecimal getTotalRecoveryCost() {
        if (mitigationStrategies == null) return BigDecimal.ZERO;
        
        return mitigationStrategies.stream()
            .filter(strategy -> strategy.getImplementationCost() != null)
            .map(MitigationStrategy::getImplementationCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if regulatory breaches exist
     */
    public boolean hasRegulatoryBreaches() {
        return regulatoryImpact != null && 
               regulatoryImpact.getCapitalAdequacyBreach() != null &&
               regulatoryImpact.getCapitalAdequacyBreach();
    }
    
    /**
     * Get stress test summary
     */
    public Map<String, Object> getStressTestSummary() {
        return Map.of(
            "scenarioName", scenarioName != null ? scenarioName : "Unknown",
            "testType", testType != null ? testType : "UNKNOWN",
            "totalPnL", portfolioImpact != null && portfolioImpact.getTotalPnL() != null ? 
                       portfolioImpact.getTotalPnL() : BigDecimal.ZERO,
            "maxLoss", portfolioImpact != null && portfolioImpact.getMaxLoss() != null ?
                      portfolioImpact.getMaxLoss() : BigDecimal.ZERO,
            "isSevereStress", isSevereStress(),
            "hasRegulatoryBreaches", hasRegulatoryBreaches(),
            "executionTime", performanceMetrics != null && performanceMetrics.getExecutionTimeMs() != null ?
                           performanceMetrics.getExecutionTimeMs() : 0L,
            "mitigationStrategiesCount", mitigationStrategies != null ? mitigationStrategies.size() : 0
        );
    }
    
    /**
     * Static factory methods
     */
    public static StressTestResult marketCrash(Long userId, BigDecimal crashMagnitude) {
        return StressTestResult.builder()
            .userId(userId)
            .scenarioName("Market Crash " + crashMagnitude + "%")
            .testType("HYPOTHETICAL")
            .scenarioCategory("MARKET_CRASH")
            .testExecutedAt(Instant.now())
            .scenarioConfig(ScenarioConfiguration.builder()
                .severity("SEVERE")
                .probability(new BigDecimal("0.05")) // 5% probability
                .build())
            .build();
    }
    
    public static StressTestResult volatilitySpike(Long userId) {
        return StressTestResult.builder()
            .userId(userId)
            .scenarioName("Volatility Spike")
            .testType("HYPOTHETICAL")
            .scenarioCategory("VOLATILITY_SPIKE")
            .testExecutedAt(Instant.now())
            .scenarioConfig(ScenarioConfiguration.builder()
                .severity("MODERATE")
                .probability(new BigDecimal("0.15")) // 15% probability
                .build())
            .build();
    }
}