package com.trademaster.agentos.strategy;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.domain.entity.AgentType;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

import java.util.List;
import java.util.function.Function;

/**
 * ✅ STRATEGY PATTERN: Functional Agent Selection Strategy Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Agent selection algorithm contract only
 * - Interface Segregation: Focused selection interface
 * - Open/Closed: Extensible via strategy implementations
 * - Dependency Inversion: Abstractions for selection algorithms
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - Function-based strategy interface
 * - Result monad for error handling
 * - Immutable strategy implementations
 * - Composable selection algorithms
 */
public interface AgentSelectionStrategy {
    
    /**
     * ✅ STRATEGY PATTERN: Core selection algorithm
     * Functional interface for agent selection
     */
    Result<Agent, AgentError> selectAgent(
        List<Agent> candidateAgents,
        AgentType requiredType,
        List<AgentCapability> requiredCapabilities,
        SelectionContext context
    );
    
    /**
     * ✅ STRATEGY PATTERN: Strategy identification
     */
    String getStrategyName();
    
    /**
     * ✅ STRATEGY PATTERN: Strategy priority for conflict resolution
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * ✅ STRATEGY PATTERN: Strategy applicability check
     */
    default boolean isApplicable(AgentType agentType, List<AgentCapability> requiredCapabilities) {
        return true;
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Strategy composition
     */
    default AgentSelectionStrategy andThen(AgentSelectionStrategy fallbackStrategy) {
        return new CompositeSelectionStrategy(this, fallbackStrategy);
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Strategy filtering
     */
    default AgentSelectionStrategy withFilter(
            Function<List<Agent>, List<Agent>> agentFilter) {
        return new FilteredSelectionStrategy(this, agentFilter);
    }
    
    /**
     * Selection context for strategy decision-making
     */
    record SelectionContext(
        String taskId,
        String priority,
        Long requestingUserId,
        java.time.Instant deadline,
        java.util.Map<String, Object> customAttributes
    ) {
        public static SelectionContext defaultContext() {
            return new SelectionContext(
                null,
                "NORMAL",
                null,
                null,
                java.util.Map.of()
            );
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String taskId;
            private String priority = "NORMAL";
            private Long requestingUserId;
            private java.time.Instant deadline;
            private java.util.Map<String, Object> customAttributes = java.util.Map.of();
            
            public Builder taskId(String taskId) {
                this.taskId = taskId;
                return this;
            }
            
            public Builder priority(String priority) {
                this.priority = priority;
                return this;
            }
            
            public Builder requestingUserId(Long userId) {
                this.requestingUserId = userId;
                return this;
            }
            
            public Builder deadline(java.time.Instant deadline) {
                this.deadline = deadline;
                return this;
            }
            
            public Builder customAttributes(java.util.Map<String, Object> attributes) {
                this.customAttributes = attributes;
                return this;
            }
            
            public SelectionContext build() {
                return new SelectionContext(taskId, priority, requestingUserId, deadline, customAttributes);
            }
        }
    }
    
    /**
     * ✅ COMPOSITE PATTERN: Composite selection strategy
     */
    record CompositeSelectionStrategy(
        AgentSelectionStrategy primaryStrategy,
        AgentSelectionStrategy fallbackStrategy
    ) implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            return primaryStrategy.selectAgent(candidateAgents, requiredType, requiredCapabilities, context)
                .recover(primaryError -> fallbackStrategy.selectAgent(
                    candidateAgents, requiredType, requiredCapabilities, context));
        }
        
        @Override
        public String getStrategyName() {
            return primaryStrategy.getStrategyName() + "+" + fallbackStrategy.getStrategyName();
        }
        
        @Override
        public int getPriority() {
            return Math.max(primaryStrategy.getPriority(), fallbackStrategy.getPriority());
        }
    }
    
    /**
     * ✅ DECORATOR PATTERN: Filtered selection strategy
     */
    record FilteredSelectionStrategy(
        AgentSelectionStrategy delegateStrategy,
        Function<List<Agent>, List<Agent>> agentFilter
    ) implements AgentSelectionStrategy {
        
        @Override
        public Result<Agent, AgentError> selectAgent(
                List<Agent> candidateAgents,
                AgentType requiredType,
                List<AgentCapability> requiredCapabilities,
                SelectionContext context) {
            
            List<Agent> filteredCandidates = agentFilter.apply(candidateAgents);
            return delegateStrategy.selectAgent(filteredCandidates, requiredType, requiredCapabilities, context);
        }
        
        @Override
        public String getStrategyName() {
            return "Filtered(" + delegateStrategy.getStrategyName() + ")";
        }
        
        @Override
        public int getPriority() {
            return delegateStrategy.getPriority();
        }
    }
}