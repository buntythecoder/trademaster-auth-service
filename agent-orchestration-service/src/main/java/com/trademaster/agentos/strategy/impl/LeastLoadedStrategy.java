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
 * ✅ FUNCTIONAL: Least Loaded Agent Selection Strategy
 * 
 * Selects the agent with the lowest current load to ensure optimal distribution.
 * Uses functional programming patterns and stream operations.
 * 
 * Selection Criteria:
 * 1. Agent with minimum current load
 * 2. Secondary sort by success rate (higher is better)
 * 3. Tertiary sort by average response time (lower is better)
 */
@Component
public class LeastLoadedStrategy implements AgentSelectionStrategy {
    
    @Override
    public Result<Agent, AgentError> selectAgent(
            List<Agent> candidates, 
            AgentType agentType, 
            List<AgentCapability> requiredCapabilities,
            SelectionContext context) {
        
        return candidates.stream()
            .filter(agent -> agent.getCurrentLoad() < agent.getMaxConcurrentTasks())
            .min((agent1, agent2) -> {
                // Primary: Compare current load (lower is better)
                int loadComparison = Integer.compare(agent1.getCurrentLoad(), agent2.getCurrentLoad());
                if (loadComparison != 0) {
                    return loadComparison;
                }
                
                // Secondary: Compare success rate (higher is better)
                int successRateComparison = Double.compare(agent2.getSuccessRate(), agent1.getSuccessRate());
                if (successRateComparison != 0) {
                    return successRateComparison;
                }
                
                // Tertiary: Compare average response time (lower is better)
                return Long.compare(agent1.getAverageResponseTime(), agent2.getAverageResponseTime());
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
    
    @Override
    public String getStrategyName() {
        return "LeastLoaded";
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for load balancing
    }
    
}