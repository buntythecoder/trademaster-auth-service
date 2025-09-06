package com.trademaster.agentos.visitor;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentStatus;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ VISITOR PATTERN: Agent Analytics Visitor Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Define analytics operations contract
 * - Open/Closed: New operations via new visitors without modifying agents
 * - Liskov Substitution: All visitors interchangeable
 * - Interface Segregation: Focused on analytics operations only
 * - Dependency Inversion: Abstract visitor interface
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - Pure functions for analytics calculations
 * - Result monad for error handling
 * - Immutable analytics data structures
 * - CompletableFuture for async operations
 * 
 * The Visitor pattern allows adding new analytics operations to agents
 * without modifying the agent classes themselves, maintaining OCP compliance.
 */
public interface AgentAnalyticsVisitor<T> {
    
    /**
     * ✅ VISITOR METHOD: Visit market analysis agents
     */
    CompletableFuture<Result<T, AgentError>> visitMarketAnalysisAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Visit risk assessment agents
     */
    CompletableFuture<Result<T, AgentError>> visitRiskAssessmentAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Visit compliance check agents
     */
    CompletableFuture<Result<T, AgentError>> visitComplianceCheckAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Visit trading execution agents
     */
    CompletableFuture<Result<T, AgentError>> visitTradingExecutionAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Visit portfolio management agents
     */
    CompletableFuture<Result<T, AgentError>> visitPortfolioManagementAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Visit data processing agents
     */
    CompletableFuture<Result<T, AgentError>> visitDataProcessingAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    );
    
    /**
     * ✅ VISITOR METHOD: Default visitor for unknown agent types
     */
    default CompletableFuture<Result<T, AgentError>> visitGenericAgent(
        Agent agent, 
        Map<String, Object> analyticsContext
    ) {
        return CompletableFuture.completedFuture(
            Result.failure(AgentError.unsupportedOperation("UNSUPPORTED_AGENT_TYPE", 
                "No analytics visitor for agent type: " + agent.getAgentType())));
    }
    
    /**
     * ✅ ANALYTICS REPORT: Generate comprehensive analytics report
     */
    record AgentAnalyticsReport(
        String agentId,
        String agentName,
        AgentType agentType,
        AgentStatus currentStatus,
        Set<AgentCapability> capabilities,
        PerformanceMetrics performance,
        ResourceUtilization resources,
        HealthMetrics health,
        SecurityMetrics security,
        BusinessMetrics business,
        Instant reportGeneratedAt
    ) {}
    
    /**
     * ✅ PERFORMANCE METRICS: Agent performance analytics
     */
    record PerformanceMetrics(
        BigDecimal successRate,
        Duration averageResponseTime,
        Long totalTasksProcessed,
        Long successfulTasks,
        Long failedTasks,
        BigDecimal throughputPerHour,
        Duration uptimePercentage,
        Map<String, BigDecimal> taskTypePerformance
    ) {}
    
    /**
     * ✅ RESOURCE UTILIZATION: Resource usage analytics
     */
    record ResourceUtilization(
        Integer currentLoad,
        Integer maxConcurrentTasks,
        BigDecimal cpuUtilization,
        BigDecimal memoryUtilization,
        BigDecimal networkUtilization,
        Duration idleTime,
        Duration busyTime,
        Map<String, Object> resourceConstraints
    ) {}
    
    /**
     * ✅ HEALTH METRICS: Agent health analytics
     */
    record HealthMetrics(
        String overallHealth,
        Instant lastHeartbeat,
        Duration heartbeatInterval,
        Long missedHeartbeats,
        String recoveryStatus,
        Map<String, String> healthChecks,
        Set<String> alerts,
        BigDecimal healthScore
    ) {}
    
    /**
     * ✅ SECURITY METRICS: Security-related analytics
     */
    record SecurityMetrics(
        String securityClearance,
        Set<String> accessPermissions,
        Long authenticationAttempts,
        Long authenticationFailures,
        Set<String> securityViolations,
        Instant lastSecurityAudit,
        String complianceStatus,
        BigDecimal securityScore
    ) {}
    
    /**
     * ✅ BUSINESS METRICS: Business value analytics
     */
    record BusinessMetrics(
        BigDecimal revenueGenerated,
        BigDecimal costSavings,
        BigDecimal roi,
        String businessValue,
        Map<String, BigDecimal> kpis,
        Set<String> businessGoalsAchieved,
        BigDecimal customerSatisfactionScore,
        String slaCompliance
    ) {}
    
    /**
     * ✅ TREND ANALYSIS: Historical trend data
     */
    record TrendAnalysis(
        Map<String, java.util.List<BigDecimal>> performanceTrends,
        Map<String, java.util.List<BigDecimal>> utilizationTrends,
        Map<String, java.util.List<String>> healthTrends,
        Map<String, java.util.List<BigDecimal>> businessTrends,
        String trendDirection,
        Set<String> anomalies,
        Map<String, String> recommendations
    ) {}
    
    /**
     * ✅ COMPARATIVE ANALYSIS: Agent comparison data
     */
    record ComparativeAnalysis(
        String agentId,
        BigDecimal relativePerformance,
        String performanceRank,
        Map<String, BigDecimal> peerComparison,
        Set<String> strengthAreas,
        Set<String> improvementAreas,
        String benchmarkCategory
    ) {}
    
    /**
     * ✅ PREDICTIVE INSIGHTS: Future performance predictions
     */
    record PredictiveInsights(
        BigDecimal predictedPerformance,
        Duration estimatedMaintenanceWindow,
        Set<String> riskFactors,
        Map<String, BigDecimal> futureCapacityNeeds,
        String scalingRecommendations,
        BigDecimal confidenceScore
    ) {}
    
    /**
     * ✅ COMPREHENSIVE ANALYTICS: Complete analytics data structure
     */
    record ComprehensiveAgentAnalytics(
        AgentAnalyticsReport basicReport,
        TrendAnalysis trends,
        ComparativeAnalysis comparison,
        PredictiveInsights predictions,
        Map<String, Object> customMetrics,
        String analyticsVersion
    ) {}
}