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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * ✅ FUNCTIONAL: Round Robin Agent Selection Strategy
 * 
 * Distributes tasks evenly across available agents using round-robin scheduling.
 * Maintains fairness by ensuring all agents get equal opportunity.
 * 
 * Selection Criteria:
 * 1. Available capacity (not at max load)
 * 2. Round-robin selection from available agents
 * 3. Thread-safe counter for concurrent access
 */
@Component
public class RoundRobinStrategy implements AgentSelectionStrategy {
    
    // Thread-safe counter for round-robin selection
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    @Override
    public Result<Agent, AgentError> selectAgent(
            List<Agent> candidates, 
            AgentType agentType, 
            List<AgentCapability> requiredCapabilities,
            SelectionContext context) {
        
        // Filter available agents
        List<Agent> availableAgents = candidates.stream()
            .filter(agent -> agent.getCurrentLoad() < agent.getMaxConcurrentTasks())
            .collect(Collectors.toList());
        
        return availableAgents.isEmpty() 
            ? Result.failure(new AgentError.NoAvailableAgent(
                agentType.toString(), 
                requiredCapabilities.stream()
                    .map(AgentCapability::toString)
                    .collect(Collectors.toList())
            ))
            : Result.success(selectRoundRobin(availableAgents));
    }
    
    /**
     * ✅ FUNCTIONAL: Thread-safe round-robin selection
     */
    private Agent selectRoundRobin(List<Agent> availableAgents) {
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % availableAgents.size();
        return availableAgents.get(index);
    }
    
    @Override
    public String getStrategyName() {
        return "RoundRobin";
    }
    
    @Override
    public int getPriority() {
        return 70; // Medium priority - good for fairness
    }
    
    @Override
    public boolean isApplicable(AgentType agentType, List<AgentCapability> requiredCapabilities) {
        // Good for scenarios where fairness is important
        return requiredCapabilities.stream()
            .anyMatch(capability -> 
                capability.toString().toLowerCase().contains("fair") ||
                capability.toString().toLowerCase().contains("distribute") ||
                capability.toString().toLowerCase().contains("balance")
            );
    }
    
    /**
     * Reset counter for testing purposes
     */
    public void resetCounter() {
        roundRobinCounter.set(0);
    }
}