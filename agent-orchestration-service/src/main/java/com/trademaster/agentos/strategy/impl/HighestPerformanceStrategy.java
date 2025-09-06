package com.trademaster.agentos.strategy.impl;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.strategy.AgentSelectionStrategy;
import com.trademaster.agentos.strategy.AgentSelectionStrategy.SelectionContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ FUNCTIONAL: Highest Performance Agent Selection Strategy
 * 
 * Selects the agent with the best performance metrics for critical tasks.
 * Prioritizes agents with high success rates and low response times.
 * 
 * Selection Criteria:
 * 1. Minimum tasks completed threshold (10) for statistical significance
 * 2. Highest success rate
 * 3. Lowest average response time
 * 4. Available capacity (not at max load)
 */
@Component
public class HighestPerformanceStrategy implements AgentSelectionStrategy {
    
    private static final long MIN_TASKS_FOR_PERFORMANCE_EVAL = 10L;
    private static final double MIN_SUCCESS_RATE_THRESHOLD = 0.8; // 80%
    
    @Override
    public Result<Agent, AgentError> selectAgent(
            List<Agent> candidates, 
            AgentType agentType, 
            List<AgentCapability> requiredCapabilities,
            SelectionContext context) {
        
        return candidates.stream()
            .filter(this::hasAvailableCapacity)
            .filter(this::hasEnoughTaskHistory)
            .filter(this::meetsMinimumSuccessRate)
            .max((agent1, agent2) -> {
                // Primary: Compare success rate (higher is better)
                int successRateComparison = Double.compare(agent1.getSuccessRate(), agent2.getSuccessRate());
                if (successRateComparison != 0) {
                    return successRateComparison;
                }
                
                // Secondary: Compare average response time (lower is better)
                int responseTimeComparison = Long.compare(agent2.getAverageResponseTime(), agent1.getAverageResponseTime());
                if (responseTimeComparison != 0) {
                    return responseTimeComparison;
                }
                
                // Tertiary: Compare total tasks completed (higher is better - more experience)
                return Long.compare(agent1.getTotalTasksCompleted(), agent2.getTotalTasksCompleted());
            })
            .map(Result::<Agent, AgentError>success)
            .orElseGet(() -> Result.failure(
                new AgentError.NoAvailableAgent(
                    agentType.toString(), 
                    requiredCapabilities.stream()
                        .map(AgentCapability::toString)
                        .collect(Collectors.toList())
                )
            ));
    }
    
    /**
     * ✅ FUNCTIONAL: Helper methods using pure functions
     */
    private boolean hasAvailableCapacity(Agent agent) {
        return agent.getCurrentLoad() < agent.getMaxConcurrentTasks();
    }
    
    private boolean hasEnoughTaskHistory(Agent agent) {
        return agent.getTotalTasksCompleted() >= MIN_TASKS_FOR_PERFORMANCE_EVAL;
    }
    
    private boolean meetsMinimumSuccessRate(Agent agent) {
        return agent.getSuccessRate() >= MIN_SUCCESS_RATE_THRESHOLD;
    }
    
    @Override
    public String getStrategyName() {
        return "HighestPerformance";
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for performance-critical tasks
    }
    
    @Override
    public boolean isApplicable(AgentType agentType, List<AgentCapability> requiredCapabilities) {
        // Best for critical or high-priority tasks
        return requiredCapabilities.stream()
            .anyMatch(capability -> 
                capability.toString().toLowerCase().contains("critical") ||
                capability.toString().toLowerCase().contains("priority") ||
                capability.toString().toLowerCase().contains("performance")
            );
    }
}