package com.trademaster.agentos.strategy;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ✅ MANDATORY: Strategy Pattern Context for Agent Selection
 * 
 * Manages agent selection strategies and provides intelligent strategy selection.
 * Implements the Context role in the Strategy pattern with functional composition.
 * 
 * Features:
 * - Runtime strategy selection based on criteria
 * - Fallback strategy chain for resilience
 * - Performance metrics and logging
 * - Functional error handling
 */
@Service
@RequiredArgsConstructor
public class AgentSelectionContext {
    
    private final List<AgentSelectionStrategy> availableStrategies;
    private final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ FUNCTIONAL: Select optimal agent using intelligent strategy selection
     */
    public Result<Agent, AgentError> selectOptimalAgent(
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities) {
        
        // Use default context for backward compatibility
        AgentSelectionStrategy.SelectionContext context = AgentSelectionStrategy.SelectionContext.defaultContext();
        return selectOptimalAgent(candidates, agentType, requiredCapabilities, context);
    }
    
    /**
     * ✅ FUNCTIONAL: Select optimal agent with context
     */
    public Result<Agent, AgentError> selectOptimalAgent(
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities,
            AgentSelectionStrategy.SelectionContext context) {
        
        structuredLogger.logDebug("agent_selection_started", 
            Map.of("candidateCount", candidates.size(), 
                   "agentType", agentType,
                   "requiredCapabilities", requiredCapabilities,
                   "priority", context.priority(),
                   "taskId", context.taskId()));
        
        return findBestStrategy(agentType, requiredCapabilities)
            .map(strategy -> executeSelectionWithFallback(strategy, candidates, agentType, requiredCapabilities, context))
            .orElseGet(() -> executeDefaultSelection(candidates, agentType, requiredCapabilities, context));
    }
    
    /**
     * ✅ FUNCTIONAL: Find best strategy based on applicability and priority
     */
    private Optional<AgentSelectionStrategy> findBestStrategy(
            AgentType agentType, 
            List<AgentCapability> requiredCapabilities) {
        
        return availableStrategies.stream()
            .filter(strategy -> strategy.isApplicable(agentType, requiredCapabilities))
            .max((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()));
    }
    
    /**
     * ✅ FUNCTIONAL: Execute selection with fallback strategies
     */
    private Result<Agent, AgentError> executeSelectionWithFallback(
            AgentSelectionStrategy primaryStrategy,
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities,
            AgentSelectionStrategy.SelectionContext context) {
        
        structuredLogger.logDebug("using_selection_strategy", 
            Map.of("strategyName", primaryStrategy.getStrategyName(),
                   "priority", primaryStrategy.getPriority()));
        
        return primaryStrategy.selectAgent(candidates, agentType, requiredCapabilities, context)
            .onSuccess(agent -> structuredLogger.logInfo("agent_selected_successfully",
                Map.of("agentId", agent.getAgentId(),
                       "agentName", agent.getAgentName(),
                       "strategyUsed", primaryStrategy.getStrategyName())))
            .onFailure(error -> structuredLogger.logWarning("primary_strategy_failed",
                Map.of("strategyName", primaryStrategy.getStrategyName(),
                       "error", error.getMessage())))
            .recover(error -> tryFallbackStrategies(primaryStrategy, candidates, agentType, requiredCapabilities, context));
    }
    
    /**
     * ✅ FUNCTIONAL: Try fallback strategies in priority order
     */
    private Result<Agent, AgentError> tryFallbackStrategies(
            AgentSelectionStrategy failedStrategy,
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities,
            AgentSelectionStrategy.SelectionContext context) {
        
        return availableStrategies.stream()
            .filter(strategy -> !strategy.equals(failedStrategy))
            .sorted((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()))
            .map(strategy -> {
                structuredLogger.logDebug("trying_fallback_strategy",
                    Map.of("strategyName", strategy.getStrategyName()));
                
                return strategy.selectAgent(candidates, agentType, requiredCapabilities, context);
            })
            .filter(Result::isSuccess)
            .findFirst()
            .orElseGet(() -> Result.failure(new AgentError.NoAvailableAgent(
                agentType.toString(),
                requiredCapabilities.stream()
                    .map(AgentCapability::toString)
                    .toList()
            )));
    }
    
    /**
     * ✅ FUNCTIONAL: Execute default selection when no strategy is applicable
     */
    private Result<Agent, AgentError> executeDefaultSelection(
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities,
            AgentSelectionStrategy.SelectionContext context) {
        
        structuredLogger.logWarning("using_default_selection",
            Map.of("reason", "no_applicable_strategy_found"));
        
        // Default to first available strategy or simple first-available selection
        return availableStrategies.stream()
            .findFirst()
            .map(strategy -> strategy.selectAgent(candidates, agentType, requiredCapabilities, context))
            .orElseGet(() -> selectFirstAvailable(candidates, agentType, requiredCapabilities));
    }
    
    /**
     * ✅ FUNCTIONAL: Simple first-available selection as final fallback
     */
    private Result<Agent, AgentError> selectFirstAvailable(
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities) {
        
        return candidates.stream()
            .filter(agent -> agent.getCurrentLoad() < agent.getMaxConcurrentTasks())
            .findFirst()
            .map(Result::<Agent, AgentError>success)
            .orElseGet(() -> Result.failure(new AgentError.NoAvailableAgent(
                agentType.toString(),
                requiredCapabilities.stream()
                    .map(AgentCapability::toString)
                    .toList()
            )));
    }
    
    /**
     * ✅ FUNCTIONAL: Execute specific strategy by name (for testing/debugging)
     */
    public Result<Agent, AgentError> selectWithStrategy(
            String strategyName,
            List<Agent> candidates,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities) {
        
        return availableStrategies.stream()
            .filter(strategy -> strategy.getStrategyName().equals(strategyName))
            .findFirst()
            .map(strategy -> {
                structuredLogger.logDebug("explicit_strategy_selection",
                    Map.of("strategyName", strategyName));
                
                return strategy.selectAgent(candidates, agentType, requiredCapabilities, AgentSelectionStrategy.SelectionContext.defaultContext());
            })
            .orElseGet(() -> {
                structuredLogger.logWarning("strategy_not_found",
                    Map.of("requestedStrategy", strategyName));
                
                return Result.failure(AgentError.simple("Strategy not found: " + strategyName));
            });
    }
    
    /**
     * Get available strategy names for API responses
     */
    public List<String> getAvailableStrategyNames() {
        return availableStrategies.stream()
            .map(AgentSelectionStrategy::getStrategyName)
            .toList();
    }
}