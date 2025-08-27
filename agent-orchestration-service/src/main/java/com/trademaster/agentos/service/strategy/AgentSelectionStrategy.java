package com.trademaster.agentos.service.strategy;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentType;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * ✅ STRATEGY PATTERN: Agent Selection Strategy Interface
 * 
 * Implements Strategy pattern with functional programming for agent selection algorithms.
 * Each strategy represents a different optimization approach for task assignment.
 */
@FunctionalInterface
public interface AgentSelectionStrategy extends Function<List<Agent>, Optional<Agent>> {
    
    /**
     * Select optimal agent from candidate list
     */
    Optional<Agent> selectAgent(List<Agent> candidates);
    
    @Override
    default Optional<Agent> apply(List<Agent> candidates) {
        return selectAgent(candidates);
    }
    
    // ✅ FUNCTIONAL COMPOSITION: Predefined strategy implementations
    
    /**
     * Least Loaded Strategy - Select agent with lowest current load
     */
    static AgentSelectionStrategy leastLoaded() {
        return candidates -> candidates.stream()
            .min((a1, a2) -> Double.compare(
                (double) a1.getCurrentLoad() / a1.getMaxConcurrentTasks(),
                (double) a2.getCurrentLoad() / a2.getMaxConcurrentTasks()
            ));
    }
    
    /**
     * Best Performance Strategy - Select agent with highest success rate and lowest response time
     */
    static AgentSelectionStrategy bestPerformance() {
        return candidates -> candidates.stream()
            .max((a1, a2) -> {
                // Weighted score: 70% success rate, 30% response time (inverted)
                double score1 = a1.getSuccessRate() * 0.7 - (a1.getAverageResponseTime() / 1000.0) * 0.3;
                double score2 = a2.getSuccessRate() * 0.7 - (a2.getAverageResponseTime() / 1000.0) * 0.3;
                return Double.compare(score1, score2);
            });
    }
    
    /**
     * Round Robin Strategy - Cycle through agents evenly
     */
    static AgentSelectionStrategy roundRobin(int currentIndex) {
        return candidates -> candidates.isEmpty() 
            ? Optional.empty() 
            : Optional.of(candidates.get(currentIndex % candidates.size()));
    }
    
    /**
     * Most Experienced Strategy - Select agent with most completed tasks
     */
    static AgentSelectionStrategy mostExperienced() {
        return candidates -> candidates.stream()
            .max((a1, a2) -> Long.compare(a1.getTotalTasksCompleted(), a2.getTotalTasksCompleted()));
    }
    
    /**
     * Composite Strategy - Combine multiple strategies with weights
     */
    static AgentSelectionStrategy composite(List<WeightedStrategy> strategies) {
        return candidates -> {
            if (candidates.isEmpty()) return Optional.empty();
            
            return candidates.stream()
                .max((a1, a2) -> {
                    double score1 = strategies.stream()
                        .mapToDouble(ws -> ws.calculateScore(a1))
                        .sum();
                    double score2 = strategies.stream()
                        .mapToDouble(ws -> ws.calculateScore(a2))
                        .sum();
                    return Double.compare(score1, score2);
                });
        };
    }
    
    /**
     * ✅ RECORD PATTERN: Immutable weighted strategy configuration
     */
    record WeightedStrategy(
        Function<Agent, Double> scoreFunction,
        double weight
    ) {
        public double calculateScore(Agent agent) {
            return scoreFunction.apply(agent) * weight;
        }
        
        // ✅ FACTORY METHODS: Create common weighted strategies
        public static WeightedStrategy loadWeight(double weight) {
            return new WeightedStrategy(
                agent -> 1.0 - ((double) agent.getCurrentLoad() / agent.getMaxConcurrentTasks()),
                weight
            );
        }
        
        public static WeightedStrategy performanceWeight(double weight) {
            return new WeightedStrategy(
                Agent::getSuccessRate,
                weight
            );
        }
        
        public static WeightedStrategy responseTimeWeight(double weight) {
            return new WeightedStrategy(
                agent -> Math.max(0, 1000.0 - agent.getAverageResponseTime()) / 1000.0,
                weight
            );
        }
        
        public static WeightedStrategy experienceWeight(double weight) {
            return new WeightedStrategy(
                agent -> Math.log10(Math.max(1, agent.getTotalTasksCompleted())),
                weight
            );
        }
    }
    
    /**
     * ✅ BUILDER PATTERN: Fluent strategy builder
     */
    class Builder {
        private final List<WeightedStrategy> strategies = new java.util.ArrayList<>();
        
        public Builder withLoadBalance(double weight) {
            strategies.add(WeightedStrategy.loadWeight(weight));
            return this;
        }
        
        public Builder withPerformance(double weight) {
            strategies.add(WeightedStrategy.performanceWeight(weight));
            return this;
        }
        
        public Builder withResponseTime(double weight) {
            strategies.add(WeightedStrategy.responseTimeWeight(weight));
            return this;
        }
        
        public Builder withExperience(double weight) {
            strategies.add(WeightedStrategy.experienceWeight(weight));
            return this;
        }
        
        public Builder withCustom(Function<Agent, Double> scoreFunction, double weight) {
            strategies.add(new WeightedStrategy(scoreFunction, weight));
            return this;
        }
        
        public AgentSelectionStrategy build() {
            return composite(List.copyOf(strategies));
        }
    }
    
    /**
     * ✅ FACTORY METHOD: Create builder instance
     */
    static Builder builder() {
        return new Builder();
    }
}