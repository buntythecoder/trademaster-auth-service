package com.trademaster.agentos.strategy;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * ✅ REGISTRY PATTERN + STRATEGY PATTERN: Agent Selection Strategy Registry
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Strategy registration and selection only
 * - Open/Closed: Extensible via new strategy implementations
 * - Interface Segregation: Focused strategy registry interface
 * - Dependency Inversion: Uses strategy abstractions
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - No if-else statements (uses Map-based strategy lookup)
 * - Stream API for strategy selection and ranking
 * - Function composition for strategy chaining
 * - Result monad for error handling
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentSelectionStrategyRegistry {
    
    private final Map<String, AgentSelectionStrategy> strategies = new ConcurrentHashMap<>();
    private final Map<StrategyType, List<String>> strategyTypeMapping = new ConcurrentHashMap<>();
    
    /**
     * ✅ ENUM STRATEGY PATTERN: Strategy type classification
     */
    public enum StrategyType {
        LOAD_BALANCED,
        PERFORMANCE_OPTIMIZED,
        ROUND_ROBIN,
        PRIORITY_BASED,
        CAPABILITY_MATCHED,
        CUSTOM
    }
    
    /**
     * ✅ FUNCTIONAL INITIALIZATION: Register default strategies
     */
    public void initializeDefaultStrategies() {
        registerStrategy(new LoadBalancedSelectionStrategy());
        registerStrategy(new PerformanceOptimizedSelectionStrategy());
        registerStrategy(new RoundRobinSelectionStrategy());
        registerStrategy(new PriorityBasedSelectionStrategy());
        registerStrategy(new CapabilityMatchedSelectionStrategy());
        
        log.info("Initialized {} default agent selection strategies", strategies.size());
    }
    
    /**
     * ✅ REGISTRY PATTERN: Register strategy with functional mapping
     * Cognitive Complexity: 1
     */
    public void registerStrategy(AgentSelectionStrategy strategy) {
        strategies.put(strategy.getStrategyName(), strategy);
        
        StrategyType type = determineStrategyType(strategy);
        strategyTypeMapping.computeIfAbsent(type, k -> List.of()).add(strategy.getStrategyName());
        
        log.debug("Registered strategy: {} with type: {}", strategy.getStrategyName(), type);
    }
    
    /**
     * ✅ STRATEGY PATTERN: Get strategy by name (functional lookup)
     * Cognitive Complexity: 1
     */
    public Optional<AgentSelectionStrategy> getStrategy(String strategyName) {
        return Optional.ofNullable(strategies.get(strategyName));
    }
    
    /**
     * ✅ STRATEGY PATTERN: Get best strategy for context (functional selection)
     * Cognitive Complexity: 3 (stream operations + optional mapping)
     */
    public Result<AgentSelectionStrategy, AgentError> getBestStrategy(
            AgentType agentType,
            List<AgentCapability> requiredCapabilities,
            AgentSelectionStrategy.SelectionContext context) {
        
        return strategies.values().stream()
            .filter(strategy -> isStrategyApplicable(strategy, agentType, requiredCapabilities))
            .max(Comparator.comparingInt(AgentSelectionStrategy::getPriority))
            .map(Result::<AgentSelectionStrategy, AgentError>success)
            .orElse(Result.failure(new AgentError.StrategyNotFound(
                "No applicable strategy found for agentType: " + agentType)));
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Create composite strategy chain
     * Cognitive Complexity: 2
     */
    public AgentSelectionStrategy createCompositeStrategy(List<String> strategyNames) {
        List<AgentSelectionStrategy> strategyList = strategyNames.stream()
            .map(strategies::get)
            .filter(java.util.Objects::nonNull)
            .toList();
        
        return strategyList.stream()
            .reduce(AgentSelectionStrategy::andThen)
            .orElse(getDefaultStrategy());
    }
    
    /**
     * ✅ FACTORY PATTERN: Create filtered strategy
     */
    public AgentSelectionStrategy createFilteredStrategy(
            String baseStrategyName,
            Function<List<Agent>, List<Agent>> filter) {
        
        return getStrategy(baseStrategyName)
            .map(strategy -> strategy.withFilter(filter))
            .orElse(getDefaultStrategy().withFilter(filter));
    }
    
    /**
     * ✅ DEFAULT STRATEGY: Fallback strategy selection
     * Cognitive Complexity: 1
     */
    private AgentSelectionStrategy getDefaultStrategy() {
        return strategies.getOrDefault("LoadBalanced", new LoadBalancedSelectionStrategy());
    }
    
    /**
     * ✅ FUNCTIONAL CLASSIFICATION: Determine strategy type without if-else
     */
    private StrategyType determineStrategyType(AgentSelectionStrategy strategy) {
        Map<String, StrategyType> typeMapping = Map.of(
            "LoadBalanced", StrategyType.LOAD_BALANCED,
            "PerformanceOptimized", StrategyType.PERFORMANCE_OPTIMIZED,
            "RoundRobin", StrategyType.ROUND_ROBIN,
            "PriorityBased", StrategyType.PRIORITY_BASED,
            "CapabilityMatched", StrategyType.CAPABILITY_MATCHED
        );
        
        return typeMapping.getOrDefault(strategy.getStrategyName(), StrategyType.CUSTOM);
    }
    
    /**
     * ✅ FUNCTIONAL PREDICATE: Strategy applicability check
     */
    private boolean isStrategyApplicable(
            AgentSelectionStrategy strategy,
            AgentType agentType,
            List<AgentCapability> requiredCapabilities) {
        
        return strategy.getStrategyName() != null && !strategy.getStrategyName().isBlank();
    }
    
    /**
     * ✅ CONCRETE STRATEGY IMPLEMENTATIONS
     */
    
    /**
     * Load-balanced selection strategy
     */
    private static class LoadBalancedSelectionStrategy implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            return candidateAgents.stream()
                .min(Comparator.comparingInt(Agent::getCurrentLoad))
                .map(Result::<Agent, AgentError>success)
                .orElse(Result.failure(new AgentError.SelectionError("No agents available for load balancing")));
        }
        
        @Override
        public String getStrategyName() {
            return "LoadBalanced";
        }
        
        @Override
        public int getPriority() {
            return 5;
        }
    }
    
    /**
     * Performance-optimized selection strategy
     */
    private static class PerformanceOptimizedSelectionStrategy implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            return candidateAgents.stream()
                .filter(agent -> agent.getSuccessRate() > 0.8)
                .min(Comparator.comparingLong(Agent::getAverageResponseTime))
                .map(Result::<Agent, AgentError>success)
                .orElse(candidateAgents.stream()
                    .max(Comparator.comparingDouble(Agent::getSuccessRate))
                    .map(Result::<Agent, AgentError>success)
                    .orElse(Result.failure(new AgentError.SelectionError("No performant agents available"))));
        }
        
        @Override
        public String getStrategyName() {
            return "PerformanceOptimized";
        }
        
        @Override
        public int getPriority() {
            return 8;
        }
    }
    
    /**
     * Round-robin selection strategy
     */
    private static class RoundRobinSelectionStrategy implements AgentSelectionStrategy {
        private static int currentIndex = 0;
        
        @Override
        public synchronized Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            return (candidateAgents.isEmpty()) ?
                Result.failure(new AgentError.SelectionError("No agents available for round-robin selection")) :
                Result.success(candidateAgents.get(currentIndex++ % candidateAgents.size()));
        }
        
        @Override
        public String getStrategyName() {
            return "RoundRobin";
        }
        
        @Override
        public int getPriority() {
            return 3;
        }
    }
    
    /**
     * Priority-based selection strategy
     */
    private static class PriorityBasedSelectionStrategy implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            Map<String, Integer> priorityWeights = Map.of(
                "HIGH", 3,
                "NORMAL", 2,
                "LOW", 1
            );
            
            int contextPriority = priorityWeights.getOrDefault(context.priority(), 2);
            
            return candidateAgents.stream()
                .max(Comparator.comparingDouble(agent -> 
                    calculatePriorityScore(agent, contextPriority)))
                .map(Result::<Agent, AgentError>success)
                .orElse(Result.failure(new AgentError.SelectionError("No agents available for priority-based selection")));
        }
        
        private double calculatePriorityScore(Agent agent, int contextPriority) {
            return agent.getSuccessRate() * 0.4 + 
                   (1.0 / (agent.getCurrentLoad() + 1)) * 0.3 +
                   contextPriority * 0.3;
        }
        
        @Override
        public String getStrategyName() {
            return "PriorityBased";
        }
        
        @Override
        public int getPriority() {
            return 6;
        }
    }
    
    /**
     * Capability-matched selection strategy
     */
    private static class CapabilityMatchedSelectionStrategy implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            return candidateAgents.stream()
                .max(Comparator.comparingInt(agent -> 
                    calculateCapabilityMatch(agent, requiredCapabilities)))
                .map(Result::<Agent, AgentError>success)
                .orElse(Result.failure(new AgentError.SelectionError("No capability-matched agents available")));
        }
        
        private int calculateCapabilityMatch(Agent agent, List<AgentCapability> requiredCapabilities) {
            return (int) requiredCapabilities.stream()
                .mapToLong(capability -> agent.getCapabilities().contains(capability) ? 1L : 0L)
                .sum();
        }
        
        @Override
        public String getStrategyName() {
            return "CapabilityMatched";
        }
        
        @Override
        public int getPriority() {
            return 7;
        }
    }
    
    /**
     * ✅ QUERY METHODS: Strategy information and metrics
     */
    
    public List<String> getAvailableStrategies() {
        return List.copyOf(strategies.keySet());
    }
    
    public Map<StrategyType, List<String>> getStrategiesByType() {
        return Map.copyOf(strategyTypeMapping);
    }
    
    public int getRegisteredStrategyCount() {
        return strategies.size();
    }
}
