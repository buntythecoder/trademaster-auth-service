package com.trademaster.agentos.decorator;

import com.trademaster.agentos.domain.entity.Agent;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;

/**
 * ✅ FUNCTIONAL: Decorator Pattern Interface for Agent Service Enhancement
 * 
 * Allows dynamic composition of service behaviors using functional programming.
 * Decorators can be chained together to create complex service enhancement pipelines.
 * 
 * Features:
 * - Functional composition with Result monads
 * - Non-invasive service enhancement
 * - Chainable decorator pattern
 * - Runtime behavior modification
 */
@FunctionalInterface
public interface AgentServiceDecorator {
    
    /**
     * ✅ FUNCTIONAL: Enhance service operation with additional behavior
     * 
     * @param operation The base service operation to enhance
     * @return Enhanced operation with additional behavior
     */
    <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
        java.util.function.Function<Agent, Result<T, AgentError>> operation
    );
    
    /**
     * ✅ FUNCTIONAL: Compose multiple decorators into a single decorator
     * 
     * @param other The decorator to compose with this one
     * @return Composed decorator
     */
    default AgentServiceDecorator compose(AgentServiceDecorator other) {
        return new AgentServiceDecorator() {
            @Override
            public <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
                    java.util.function.Function<Agent, Result<T, AgentError>> operation) {
                return AgentServiceDecorator.this.decorate(other.decorate(operation));
            }
        };
    }
    
    /**
     * ✅ FUNCTIONAL: Create a decorator chain from multiple decorators
     * 
     * @param decorators The decorators to chain
     * @return Combined decorator
     */
    static AgentServiceDecorator chain(AgentServiceDecorator... decorators) {
        return java.util.Arrays.stream(decorators)
            .reduce(AgentServiceDecorator::compose)
            .orElse(AgentServiceDecorator.identity()); // Identity decorator if no decorators
    }
    
    /**
     * ✅ FUNCTIONAL: Identity decorator (no-op)
     * 
     * @return Decorator that passes through without modification
     */
    static AgentServiceDecorator identity() {
        return new AgentServiceDecorator() {
            @Override
            public <T> java.util.function.Function<Agent, Result<T, AgentError>> decorate(
                    java.util.function.Function<Agent, Result<T, AgentError>> operation) {
                return operation;
            }
        };
    }
}