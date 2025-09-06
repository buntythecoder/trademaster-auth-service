package com.trademaster.agentos.visitor;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ VISITOR CONTEXT: Agent Visitor Pattern Context
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Visitor pattern coordination only
 * - Open/Closed: New visitors added without modification
 * - Liskov Substitution: All visitors interchangeable
 * - Interface Segregation: Focused visitor coordination
 * - Dependency Inversion: Uses visitor abstractions
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - Pattern matching for agent type dispatch
 * - Result monad for error handling
 * - Immutable visitor context
 * - CompletableFuture for async operations
 * 
 * This class implements the "accept" side of the Visitor pattern,
 * dispatching to appropriate visitor methods based on agent type.
 */
@Service
@Slf4j
public class AgentVisitorContext {
    
    /**
     * ✅ VISITOR PATTERN: Accept visitor for agent analytics
     * Uses pattern matching instead of if-else for type dispatch
     * Cognitive Complexity: 2
     */
    public <T> CompletableFuture<Result<T, AgentError>> acceptVisitor(
            Agent agent, 
            AgentAnalyticsVisitor<T> visitor, 
            Map<String, Object> analyticsContext) {
        
        log.debug("Accepting visitor for agent", Map.of(
            "agentId", agent.getAgentId(),
            "agentType", agent.getAgentType(),
            "visitorType", visitor.getClass().getSimpleName()
        ));
        
        return switch (agent.getAgentType()) {
            case MARKET_ANALYSIS -> visitor.visitMarketAnalysisAgent(agent, analyticsContext);
            case RISK_ASSESSMENT -> visitor.visitRiskAssessmentAgent(agent, analyticsContext);
            case COMPLIANCE_CHECK -> visitor.visitComplianceCheckAgent(agent, analyticsContext);
            case TRADING_EXECUTION -> visitor.visitTradingExecutionAgent(agent, analyticsContext);
            case PORTFOLIO_MANAGEMENT -> visitor.visitPortfolioManagementAgent(agent, analyticsContext);
            case DATA_PROCESSING -> visitor.visitDataProcessingAgent(agent, analyticsContext);
            default -> visitor.visitGenericAgent(agent, analyticsContext);
        };
    }
    
    /**
     * ✅ BATCH VISITOR: Accept visitor for multiple agents
     * Cognitive Complexity: 3
     */
    public <T> CompletableFuture<Result<Map<String, T>, AgentError>> acceptVisitorBatch(
            java.util.List<Agent> agents,
            AgentAnalyticsVisitor<T> visitor,
            Map<String, Object> analyticsContext) {
        
        log.debug("Accepting batch visitor for agents", Map.of(
            "agentCount", agents.size(),
            "visitorType", visitor.getClass().getSimpleName()
        ));
        
        java.util.List<CompletableFuture<Result<java.util.Map.Entry<String, T>, AgentError>>> futures = 
            agents.stream()
                .map(agent -> acceptVisitor(agent, visitor, analyticsContext)
                    .thenApply(result -> result.map(value -> 
                        java.util.Map.entry(agent.getAgentId().toString(), value))))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<String, T> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue
                    ));
                
                return Result.<Map<String, T>, AgentError>success(results);
            });
    }
    
    /**
     * ✅ CONDITIONAL VISITOR: Accept visitor with conditions
     * Cognitive Complexity: 2
     */
    public <T> CompletableFuture<Result<T, AgentError>> acceptVisitorConditional(
            Agent agent,
            AgentAnalyticsVisitor<T> visitor,
            Map<String, Object> analyticsContext,
            java.util.function.Predicate<Agent> condition) {
        
        return condition.test(agent) ?
            acceptVisitor(agent, visitor, analyticsContext) :
            CompletableFuture.completedFuture(
                Result.failure(AgentError.validationError("CONDITION_NOT_MET", 
                    "Agent does not meet visitor conditions")));
    }
    
    /**
     * ✅ TYPED VISITOR: Accept visitor with type filtering
     * Cognitive Complexity: 2
     */
    public <T> CompletableFuture<Result<java.util.List<T>, AgentError>> acceptVisitorByType(
            java.util.List<Agent> agents,
            AgentType targetType,
            AgentAnalyticsVisitor<T> visitor,
            Map<String, Object> analyticsContext) {
        
        java.util.List<Agent> filteredAgents = agents.stream()
            .filter(agent -> agent.getAgentType() == targetType)
            .toList();
        
        log.debug("Accepting visitor by type", Map.of(
            "targetType", targetType,
            "totalAgents", agents.size(),
            "filteredCount", filteredAgents.size()
        ));
        
        java.util.List<CompletableFuture<Result<T, AgentError>>> futures = 
            filteredAgents.stream()
                .map(agent -> acceptVisitor(agent, visitor, analyticsContext))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                java.util.List<T> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
                
                return Result.<java.util.List<T>, AgentError>success(results);
            });
    }
    
    /**
     * ✅ CAPABILITY VISITOR: Accept visitor with capability filtering
     * Cognitive Complexity: 3
     */
    public <T> CompletableFuture<Result<java.util.List<T>, AgentError>> acceptVisitorByCapability(
            java.util.List<Agent> agents,
            java.util.Set<com.trademaster.agentos.domain.entity.AgentCapability> requiredCapabilities,
            AgentAnalyticsVisitor<T> visitor,
            Map<String, Object> analyticsContext) {
        
        java.util.List<Agent> filteredAgents = agents.stream()
            .filter(agent -> agent.getCapabilities().containsAll(requiredCapabilities))
            .toList();
        
        log.debug("Accepting visitor by capability", Map.of(
            "requiredCapabilities", requiredCapabilities,
            "totalAgents", agents.size(),
            "filteredCount", filteredAgents.size()
        ));
        
        java.util.List<CompletableFuture<Result<T, AgentError>>> futures = 
            filteredAgents.stream()
                .map(agent -> acceptVisitor(agent, visitor, analyticsContext))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                java.util.List<T> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Result::isSuccess)
                    .map(result -> result.getValue().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
                
                return Result.<java.util.List<T>, AgentError>success(results);
            });
    }
    
    /**
     * ✅ VISITOR FACTORY: Create analytics context with defaults
     */
    public static Map<String, Object> createAnalyticsContext(String requestId, String userId) {
        return Map.of(
            "requestId", requestId,
            "userId", userId,
            "timestamp", java.time.Instant.now(),
            "analyticsVersion", "1.0",
            "includeHistory", true,
            "includePredictions", true,
            "timeRange", "30d"
        );
    }
    
    /**
     * ✅ VISITOR FACTORY: Create enhanced analytics context
     */
    public static Map<String, Object> createEnhancedAnalyticsContext(
            String requestId, 
            String userId,
            java.time.Duration timeRange,
            boolean includeComparative,
            boolean includePredictive) {
        
        return Map.of(
            "requestId", requestId,
            "userId", userId,
            "timestamp", java.time.Instant.now(),
            "analyticsVersion", "2.0",
            "timeRange", timeRange.toString(),
            "includeHistory", true,
            "includeComparative", includeComparative,
            "includePredictive", includePredictive,
            "detailLevel", "comprehensive"
        );
    }
}