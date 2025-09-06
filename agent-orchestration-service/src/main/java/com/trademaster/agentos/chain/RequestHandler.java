package com.trademaster.agentos.chain;

import com.trademaster.agentos.functional.Result;

import java.util.function.Function;

/**
 * Abstract base class for Chain of Responsibility pattern in TradeMaster Agent OS
 * 
 * Provides a functional approach to request processing chains with Result types
 * for proper error handling and composition.
 * 
 * @param <T> The type of request being processed
 * @param <R> The type of response returned
 * @param <E> The type of error that can occur
 */
public abstract class RequestHandler<T, R, E> {
    
    protected RequestHandler<T, R, E> nextHandler;
    
    /**
     * Sets the next handler in the chain
     */
    public RequestHandler<T, R, E> setNext(RequestHandler<T, R, E> handler) {
        this.nextHandler = handler;
        return handler; // Return next handler for fluent chaining
    }
    
    /**
     * Handles the request. If this handler can't process it, passes to next handler.
     * Uses Result type for functional error handling.
     */
    public final Result<R, E> handle(T request) {
        Result<R, E> result = doHandle(request);
        
        // If this handler couldn't process the request, try the next one
        if (result.isFailure() && canPassToNext() && nextHandler != null) {
            return nextHandler.handle(request);
        }
        
        return result;
    }
    
    /**
     * Abstract method for handling the request
     * Subclasses implement their specific handling logic here
     */
    protected abstract Result<R, E> doHandle(T request);
    
    /**
     * Determines whether the request should be passed to the next handler
     * Default implementation passes on failure, but can be overridden
     */
    protected boolean canPassToNext() {
        return true;
    }
    
    /**
     * Checks if this handler can handle the given request
     * Subclasses can override for more specific logic
     */
    protected boolean canHandle(T request) {
        return true;
    }
    
    /**
     * Functional composition method for creating handler chains
     */
    public RequestHandler<T, R, E> then(RequestHandler<T, R, E> nextHandler) {
        setNext(nextHandler);
        return this;
    }
    
    /**
     * Creates a functional handler from a simple function
     */
    public static <T, R, E> RequestHandler<T, R, E> of(Function<T, Result<R, E>> handlerFunction) {
        return new FunctionalRequestHandler<>(handlerFunction);
    }
    
    /**
     * Creates a conditional handler that only processes requests matching a predicate
     */
    public static <T, R, E> RequestHandler<T, R, E> when(
            java.util.function.Predicate<T> condition,
            Function<T, Result<R, E>> handlerFunction,
            E fallbackError
    ) {
        return new ConditionalRequestHandler<>(condition, handlerFunction, fallbackError);
    }
    
    /**
     * Functional implementation of RequestHandler
     */
    private static class FunctionalRequestHandler<T, R, E> extends RequestHandler<T, R, E> {
        private final Function<T, Result<R, E>> handlerFunction;
        
        public FunctionalRequestHandler(Function<T, Result<R, E>> handlerFunction) {
            this.handlerFunction = handlerFunction;
        }
        
        @Override
        protected Result<R, E> doHandle(T request) {
            return handlerFunction.apply(request);
        }
    }
    
    /**
     * Conditional implementation of RequestHandler
     */
    private static class ConditionalRequestHandler<T, R, E> extends RequestHandler<T, R, E> {
        private final java.util.function.Predicate<T> condition;
        private final Function<T, Result<R, E>> handlerFunction;
        private final E fallbackError;
        
        public ConditionalRequestHandler(
                java.util.function.Predicate<T> condition,
                Function<T, Result<R, E>> handlerFunction,
                E fallbackError
        ) {
            this.condition = condition;
            this.handlerFunction = handlerFunction;
            this.fallbackError = fallbackError;
        }
        
        @Override
        protected Result<R, E> doHandle(T request) {
            return condition.test(request) 
                ? handlerFunction.apply(request)
                : Result.failure(fallbackError);
        }
        
        @Override
        protected boolean canHandle(T request) {
            return condition.test(request);
        }
    }
}