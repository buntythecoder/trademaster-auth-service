package com.trademaster.trading.service;

import com.trademaster.trading.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Trading Strategy Service Interface
 * 
 * Comprehensive algorithmic trading strategy framework with:
 * - Multi-asset class strategy execution and management
 * - Real-time signal generation and processing
 * - Advanced backtesting and performance analytics
 * - Risk-adjusted strategy optimization
 * - Machine learning integration and adaptive strategies
 * - Multi-timeframe analysis and execution
 * - Portfolio-aware strategy coordination
 * - Institutional-grade execution algorithms
 * 
 * Performance Targets:
 * - Signal generation: <10ms latency
 * - Strategy execution: <50ms order-to-market
 * - Backtest processing: >10,000 trades/second
 * - Strategy optimization: <5 minutes full optimization
 * - Real-time monitoring: <1ms strategy health checks
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface TradingStrategyService {
    
    // ==================== STRATEGY LIFECYCLE MANAGEMENT ====================
    
    /**
     * Create and register new trading strategy
     * @param strategyDefinition Complete strategy configuration
     * @return Created strategy with assigned ID and validation results
     */
    Mono<TradingStrategy> createStrategy(TradingStrategyDefinition strategyDefinition);
    
    /**
     * Update existing strategy configuration
     * @param strategyId Strategy identifier
     * @param updates Strategy configuration updates
     * @return Updated strategy configuration
     */
    Mono<TradingStrategy> updateStrategy(String strategyId, TradingStrategyDefinition updates);
    
    /**
     * Deploy strategy for live trading
     * @param strategyId Strategy to deploy
     * @param deploymentConfig Deployment parameters and risk limits
     * @return Deployment result and real-time monitoring stream
     */
    Mono<StrategyDeployment> deployStrategy(String strategyId, StrategyDeploymentConfig deploymentConfig);
    
    /**
     * Stop strategy execution gracefully
     * @param strategyId Strategy to stop
     * @param stopReason Reason for stopping
     * @return Stop confirmation and final performance metrics
     */
    Mono<StrategyStopResult> stopStrategy(String strategyId, String stopReason);
    
    /**
     * Pause/resume strategy execution
     * @param strategyId Strategy to pause/resume
     * @param action PAUSE or RESUME
     * @return Action result and current strategy state
     */
    Mono<StrategyControlResult> controlStrategy(String strategyId, String action);
    
    /**
     * Get strategy by ID with full configuration
     * @param strategyId Strategy identifier
     * @return Complete strategy configuration and current state
     */
    Mono<TradingStrategy> getStrategy(String strategyId);
    
    /**
     * Get all strategies for user with filtering
     * @param userId User identifier
     * @param filter Optional filtering criteria
     * @return List of user's strategies
     */
    Flux<TradingStrategy> getUserStrategies(Long userId, StrategyFilter filter);
    
    /**
     * Delete strategy permanently
     * @param strategyId Strategy to delete
     * @return Deletion confirmation
     */
    Mono<Void> deleteStrategy(String strategyId);
    
    // ==================== SIGNAL GENERATION AND PROCESSING ====================
    
    /**
     * Generate real-time trading signals
     * @param strategyId Strategy generating signals
     * @param marketData Current market data
     * @return Trading signals with confidence scores
     */
    Mono<List<TradingSignal>> generateSignals(String strategyId, MarketDataSnapshot marketData);
    
    /**
     * Process trading signal into actionable orders
     * @param signal Trading signal to process
     * @param portfolioContext Current portfolio state
     * @return Order recommendations with sizing and timing
     */
    Mono<List<OrderRecommendation>> processSignal(TradingSignal signal, PortfolioContext portfolioContext);
    
    /**
     * Execute signal-generated orders
     * @param recommendations Order recommendations from signal processing
     * @param executionParams Execution parameters and constraints
     * @return Execution results and performance metrics
     */
    Mono<List<SignalExecutionResult>> executeSignalOrders(List<OrderRecommendation> recommendations, 
                                                          SignalExecutionParams executionParams);
    
    /**
     * Stream real-time signals for strategy
     * @param strategyId Strategy to monitor
     * @return Real-time signal stream
     */
    Flux<TradingSignal> streamSignals(String strategyId);
    
    /**
     * Get historical signals for analysis
     * @param strategyId Strategy identifier
     * @param startDate Start date for signal history
     * @param endDate End date for signal history
     * @return Historical signal data with performance outcomes
     */
    Flux<TradingSignal> getHistoricalSignals(String strategyId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Validate signal quality and consistency
     * @param strategyId Strategy generating signals
     * @param timeframe Validation time frame
     * @return Signal quality metrics and validation results
     */
    Mono<SignalQualityReport> validateSignalQuality(String strategyId, String timeframe);
    
    // ==================== BACKTESTING AND SIMULATION ====================
    
    /**
     * Run comprehensive strategy backtest
     * @param strategyDefinition Strategy to backtest
     * @param backtestParams Backtest parameters and data sources
     * @return Complete backtest results with performance analytics
     */
    Mono<BacktestResult> runBacktest(TradingStrategyDefinition strategyDefinition, 
                                   BacktestParameters backtestParams);
    
    /**
     * Run forward-looking strategy simulation
     * @param strategyId Strategy to simulate
     * @param simulationParams Simulation parameters and scenarios
     * @return Simulation results and scenario analysis
     */
    Mono<SimulationResult> runSimulation(String strategyId, SimulationParameters simulationParams);
    
    /**
     * Compare multiple strategies performance
     * @param strategyIds List of strategies to compare
     * @param comparisonParams Comparison parameters and benchmarks
     * @return Comprehensive strategy comparison analysis
     */
    Mono<StrategyComparison> compareStrategies(List<String> strategyIds, 
                                             ComparisonParameters comparisonParams);
    
    /**
     * Perform walk-forward analysis
     * @param strategyDefinition Strategy for analysis
     * @param walkForwardParams Walk-forward parameters
     * @return Walk-forward analysis results with optimization periods
     */
    Mono<WalkForwardResult> performWalkForwardAnalysis(TradingStrategyDefinition strategyDefinition,
                                                      WalkForwardParameters walkForwardParams);
    
    /**
     * Run Monte Carlo simulation
     * @param strategyId Strategy for simulation
     * @param monteCarloParams Simulation parameters
     * @return Monte Carlo results with confidence intervals
     */
    Mono<MonteCarloResult> runMonteCarloSimulation(String strategyId, 
                                                  MonteCarloParameters monteCarloParams);
    
    // ==================== STRATEGY OPTIMIZATION ====================
    
    /**
     * Optimize strategy parameters using machine learning
     * @param strategyId Strategy to optimize
     * @param optimizationConfig Optimization parameters and constraints
     * @return Optimized parameters and performance improvements
     */
    Mono<StrategyOptimizationResult> optimizeStrategy(String strategyId, 
                                                    OptimizationConfiguration optimizationConfig);
    
    /**
     * Perform genetic algorithm optimization
     * @param strategyDefinition Base strategy for optimization
     * @param geneticParams Genetic algorithm parameters
     * @return Best evolved strategy parameters
     */
    Mono<GeneticOptimizationResult> optimizeWithGeneticAlgorithm(TradingStrategyDefinition strategyDefinition,
                                                               GeneticOptimizationParams geneticParams);
    
    /**
     * Run multi-objective optimization
     * @param strategyId Strategy to optimize
     * @param objectives List of optimization objectives (return, risk, drawdown)
     * @return Pareto-optimal solution set
     */
    Mono<ParetoOptimizationResult> performMultiObjectiveOptimization(String strategyId,
                                                                    List<OptimizationObjective> objectives);
    
    /**
     * Optimize strategy for regime-specific performance
     * @param strategyId Strategy to optimize
     * @param regimeParams Market regime parameters
     * @return Regime-specific optimized parameters
     */
    Mono<RegimeOptimizationResult> optimizeForMarketRegimes(String strategyId,
                                                           RegimeOptimizationParams regimeParams);
    
    // ==================== PERFORMANCE ANALYTICS ====================
    
    /**
     * Generate comprehensive strategy performance report
     * @param strategyId Strategy to analyze
     * @param reportParams Report parameters and benchmarks
     * @return Detailed performance analytics report
     */
    Mono<StrategyPerformanceReport> generatePerformanceReport(String strategyId,
                                                            PerformanceReportParams reportParams);
    
    /**
     * Calculate real-time strategy metrics
     * @param strategyId Strategy to analyze
     * @return Current performance metrics and risk indicators
     */
    Mono<StrategyMetrics> calculateRealTimeMetrics(String strategyId);
    
    /**
     * Perform attribution analysis
     * @param strategyId Strategy to analyze
     * @param attributionParams Attribution analysis parameters
     * @return Performance attribution breakdown by factors
     */
    Mono<PerformanceAttribution> performAttributionAnalysis(String strategyId,
                                                           AttributionParameters attributionParams);
    
    /**
     * Calculate strategy risk metrics
     * @param strategyId Strategy to analyze
     * @param riskParams Risk calculation parameters
     * @return Comprehensive risk metrics and VaR analysis
     */
    Mono<StrategyRiskMetrics> calculateRiskMetrics(String strategyId, RiskAnalysisParams riskParams);
    
    /**
     * Generate benchmark comparison
     * @param strategyId Strategy to compare
     * @param benchmarks List of benchmark indices/strategies
     * @return Benchmark comparison analysis
     */
    Mono<BenchmarkComparison> compareToBenchmarks(String strategyId, List<String> benchmarks);
    
    /**
     * Stream real-time performance metrics
     * @param strategyId Strategy to monitor
     * @return Real-time performance metric updates
     */
    Flux<StrategyMetrics> streamPerformanceMetrics(String strategyId);
    
    // ==================== RISK MANAGEMENT INTEGRATION ====================
    
    /**
     * Set strategy risk limits
     * @param strategyId Strategy identifier
     * @param riskLimits Risk limits configuration
     * @return Risk limit validation and activation result
     */
    Mono<RiskLimitResult> setStrategyRiskLimits(String strategyId, StrategyRiskLimits riskLimits);
    
    /**
     * Monitor strategy risk in real-time
     * @param strategyId Strategy to monitor
     * @return Real-time risk monitoring alerts and metrics
     */
    Flux<StrategyRiskAlert> monitorStrategyRisk(String strategyId);
    
    /**
     * Calculate portfolio impact of strategy
     * @param strategyId Strategy to analyze
     * @param portfolioContext Current portfolio state
     * @return Portfolio impact analysis and correlation effects
     */
    Mono<PortfolioImpactAnalysis> calculatePortfolioImpact(String strategyId, 
                                                          PortfolioContext portfolioContext);
    
    /**
     * Perform strategy stress testing
     * @param strategyId Strategy to stress test
     * @param stressScenarios List of stress test scenarios
     * @return Stress test results and scenario analysis
     */
    Mono<StrategyStressTestResult> performStressTest(String strategyId, 
                                                   List<StressTestScenario> stressScenarios);
    
    // ==================== MACHINE LEARNING INTEGRATION ====================
    
    /**
     * Train machine learning models for strategy
     * @param strategyId Strategy identifier
     * @param trainingConfig ML training configuration
     * @return Model training results and performance metrics
     */
    Mono<MLModelTrainingResult> trainStrategyModel(String strategyId, MLTrainingConfig trainingConfig);
    
    /**
     * Deploy trained ML model for strategy
     * @param strategyId Strategy identifier
     * @param modelId Trained model to deploy
     * @return Model deployment result and performance validation
     */
    Mono<MLModelDeploymentResult> deployStrategyModel(String strategyId, String modelId);
    
    /**
     * Get ML model predictions
     * @param strategyId Strategy using ML model
     * @param inputFeatures Feature vector for prediction
     * @return Model predictions with confidence intervals
     */
    Mono<MLPredictionResult> getModelPredictions(String strategyId, Map<String, Object> inputFeatures);
    
    /**
     * Update strategy model with new data
     * @param strategyId Strategy identifier
     * @param trainingData New training data
     * @return Model update results and performance changes
     */
    Mono<MLModelUpdateResult> updateStrategyModel(String strategyId, List<Map<String, Object>> trainingData);
    
    // ==================== STRATEGY COORDINATION ====================
    
    /**
     * Coordinate multiple strategies in portfolio
     * @param strategyIds List of strategies to coordinate
     * @param coordinationConfig Coordination parameters and constraints
     * @return Strategy coordination results and allocation adjustments
     */
    Mono<StrategyCoordinationResult> coordinateStrategies(List<String> strategyIds,
                                                         CoordinationConfiguration coordinationConfig);
    
    /**
     * Create strategy ensemble
     * @param strategyIds Component strategies for ensemble
     * @param ensembleConfig Ensemble configuration and weighting
     * @return Ensemble strategy configuration and performance projection
     */
    Mono<EnsembleStrategy> createStrategyEnsemble(List<String> strategyIds,
                                                EnsembleConfiguration ensembleConfig);
    
    /**
     * Manage strategy lifecycle events
     * @param strategyId Strategy identifier
     * @param event Lifecycle event (START, STOP, PAUSE, ERROR)
     * @return Event processing result and strategy state update
     */
    Mono<LifecycleEventResult> handleStrategyLifecycleEvent(String strategyId, StrategyLifecycleEvent event);
    
    // ==================== MARKET DATA INTEGRATION ====================
    
    /**
     * Subscribe strategy to market data feeds
     * @param strategyId Strategy identifier
     * @param dataSubscriptions List of market data subscriptions
     * @return Subscription confirmation and data feed quality metrics
     */
    Mono<DataSubscriptionResult> subscribeToMarketData(String strategyId, 
                                                      List<MarketDataSubscription> dataSubscriptions);
    
    /**
     * Process real-time market data for strategy
     * @param strategyId Strategy receiving data
     * @param marketUpdate Market data update
     * @return Processing result and potential signal generation
     */
    Mono<MarketDataProcessingResult> processMarketDataUpdate(String strategyId, MarketDataUpdate marketUpdate);
    
    /**
     * Get strategy market data requirements
     * @param strategyId Strategy identifier
     * @return List of required market data feeds and frequencies
     */
    Mono<List<MarketDataRequirement>> getStrategyDataRequirements(String strategyId);
    
    // ==================== COMPLIANCE AND REPORTING ====================
    
    /**
     * Generate regulatory compliance report for strategy
     * @param strategyId Strategy identifier
     * @param reportingPeriod Reporting period
     * @return Compliance report with regulatory metrics
     */
    Mono<StrategyComplianceReport> generateComplianceReport(String strategyId, ReportingPeriod reportingPeriod);
    
    /**
     * Audit strategy trading activity
     * @param strategyId Strategy to audit
     * @param auditParams Audit parameters and scope
     * @return Comprehensive trading activity audit
     */
    Mono<StrategyAuditReport> auditStrategyActivity(String strategyId, AuditParameters auditParams);
    
    /**
     * Export strategy configuration and results
     * @param strategyId Strategy to export
     * @param exportFormat Export format (JSON, XML, CSV)
     * @return Exported strategy data
     */
    Mono<StrategyExportResult> exportStrategy(String strategyId, String exportFormat);
    
    // ==================== SYSTEM HEALTH AND MONITORING ====================
    
    /**
     * Get service health metrics
     * @return Current service health and performance statistics
     */
    Mono<Map<String, Object>> getServiceHealth();
    
    /**
     * Get strategy execution statistics
     * @return Strategy execution performance and resource utilization
     */
    Mono<Map<String, Object>> getExecutionStatistics();
    
    /**
     * Monitor system resource usage
     * @return Real-time system resource monitoring
     */
    Flux<SystemResourceMetrics> monitorSystemResources();
}