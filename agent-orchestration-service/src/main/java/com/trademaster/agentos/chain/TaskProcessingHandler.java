package com.trademaster.agentos.chain;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;

/**
 * ✅ MANDATORY: Chain of Responsibility Pattern for Task Processing
 * 
 * Defines a chain of handlers for processing tasks through various validation
 * and processing stages. Each handler can process the request or pass it to
 * the next handler in the chain.
 * 
 * Features:
 * - Functional composition with Result monads
 * - Type-safe error handling
 * - Flexible processing pipeline
 * - Easy to extend and modify
 */
public abstract class TaskProcessingHandler {
    
    protected TaskProcessingHandler nextHandler;
    
    /**
     * Set the next handler in the chain
     */
    public TaskProcessingHandler setNext(TaskProcessingHandler handler) {
        this.nextHandler = handler;
        return handler;
    }
    
    /**
     * Handle the task processing request
     * Each handler can either process the task or pass it to the next handler
     * 
     * @param task The task to process
     * @return Result containing processed task or error
     */
    public abstract Result<Task, AgentError> handle(Task task);
    
    /**
     * ✅ FUNCTIONAL: Process next handler in chain if present
     */
    protected Result<Task, AgentError> processNext(Task task) {
        return nextHandler != null 
            ? nextHandler.handle(task)
            : Result.success(task);
    }
    
    /**
     * Get handler name for logging and debugging
     */
    public abstract String getHandlerName();
}