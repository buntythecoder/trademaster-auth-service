package com.trademaster.agentos.observer;

import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;

/**
 * ✅ FUNCTIONAL: Observer Interface for Agent Events
 * 
 * Functional interface for observing agent events using functional programming principles.
 * Provides type-safe event handling with Result monad integration.
 * 
 * Features:
 * - Functional programming approach
 * - Type-safe event handling
 * - Result monad integration for error handling
 * - Composable observer pattern
 */
@FunctionalInterface
public interface AgentEventObserver {
    
    /**
     * ✅ FUNCTIONAL: Handle agent event with Result monad
     * 
     * @param event The agent event to handle
     * @return Result indicating success or failure of event handling
     */
    Result<Void, AgentError> handleEvent(AgentEvent event);
    
    /**
     * ✅ FUNCTIONAL: Compose observers for sequential execution
     */
    default AgentEventObserver andThen(AgentEventObserver other) {
        return event -> this.handleEvent(event)
            .flatMap(ignored -> other.handleEvent(event));
    }
    
    /**
     * ✅ FUNCTIONAL: Compose observers with fallback handling
     */
    default AgentEventObserver orElse(AgentEventObserver fallback) {
        return event -> this.handleEvent(event)
            .onFailure(error -> fallback.handleEvent(event));
    }
    
    /**
     * ✅ FUNCTIONAL: Filter events before handling
     */
    default AgentEventObserver filter(java.util.function.Predicate<AgentEvent> condition) {
        return event -> condition.test(event) ? 
            this.handleEvent(event) : 
            Result.success(null);
    }
    
    /**
     * ✅ FUNCTIONAL: Transform events before handling
     */
    default <T> AgentEventObserver map(java.util.function.Function<AgentEvent, AgentEvent> transformer) {
        return event -> this.handleEvent(transformer.apply(event));
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer that handles only critical events
     */
    static AgentEventObserver criticalOnly(AgentEventObserver observer) {
        return observer.filter(event -> event.getEventType().isCritical());
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer that handles only health-related events
     */
    static AgentEventObserver healthOnly(AgentEventObserver observer) {
        return observer.filter(event -> event.getEventType().isHealthRelated());
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer that handles only task-related events
     */
    static AgentEventObserver taskOnly(AgentEventObserver observer) {
        return observer.filter(event -> event.getEventType().isTaskRelated());
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer for specific agent
     */
    static AgentEventObserver forAgent(Long agentId, AgentEventObserver observer) {
        return observer.filter(event -> event.getAgentId().equals(agentId));
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer for specific event types
     */
    static AgentEventObserver forEventTypes(java.util.Set<AgentEvent.AgentEventType> eventTypes, 
                                           AgentEventObserver observer) {
        return observer.filter(event -> eventTypes.contains(event.getEventType()));
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer with error recovery
     */
    static AgentEventObserver withErrorRecovery(AgentEventObserver observer, 
                                               java.util.function.Function<AgentError, AgentEventObserver> errorHandler) {
        return event -> observer.handleEvent(event)
            .onFailure(error -> errorHandler.apply(error).handleEvent(event));
    }
    
    /**
     * ✅ FUNCTIONAL: Create no-op observer
     */
    static AgentEventObserver noOp() {
        return event -> Result.success(null);
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer that logs events
     */
    static AgentEventObserver logging(java.util.function.Consumer<AgentEvent> logger) {
        return event -> {
            logger.accept(event);
            return Result.success(null);
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Combine multiple observers
     */
    static AgentEventObserver combine(AgentEventObserver... observers) {
        return java.util.Arrays.stream(observers)
            .reduce(AgentEventObserver::andThen)
            .orElse(noOp());
    }
    
    /**
     * ✅ FUNCTIONAL: Create observer with retry logic
     */
    static AgentEventObserver withRetry(AgentEventObserver observer, int maxRetries) {
        return event -> {
            Result<Void, AgentError> result = observer.handleEvent(event);
            int attempts = 1;
            
            while (result.isFailure() && attempts < maxRetries) {
                attempts++;
                result = observer.handleEvent(event);
            }
            
            return result;
        };
    }
}