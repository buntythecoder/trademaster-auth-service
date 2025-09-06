package com.trademaster.agentos.chain;

import com.trademaster.agentos.chain.impl.ResourceAllocationHandler;
import com.trademaster.agentos.chain.impl.SecurityValidationHandler;
import com.trademaster.agentos.chain.impl.TaskValidationHandler;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ✅ MANDATORY: Task Processing Chain Orchestrator
 * 
 * Configures and manages the Chain of Responsibility for task processing.
 * Provides a unified interface for processing tasks through the complete pipeline.
 * 
 * Processing Pipeline:
 * 1. TaskValidationHandler - Basic task validation
 * 2. SecurityValidationHandler - Security and authorization checks
 * 3. ResourceAllocationHandler - Resource availability and allocation
 * 
 * Features:
 * - Configurable handler chain
 * - Comprehensive error handling
 * - Performance metrics collection
 * - Structured logging integration
 */
@Service
@RequiredArgsConstructor
public class TaskProcessingChain {
    
    private final TaskValidationHandler taskValidationHandler;
    private final SecurityValidationHandler securityValidationHandler;
    private final ResourceAllocationHandler resourceAllocationHandler;
    private final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ FUNCTIONAL: Process task through the complete chain
     */
    public Result<Task, AgentError> processTask(Task task) {
        var timer = System.currentTimeMillis();
        
        structuredLogger.logDebug("task_processing_chain_started", 
            Map.of("taskId", task.getTaskId(), "taskType", task.getTaskType(),
                   "priority", task.getPriority()));
        
        return buildProcessingChain()
            .handle(task)
            .onSuccess(processedTask -> {
                long duration = System.currentTimeMillis() - timer;
                structuredLogger.logInfo("task_processing_completed", 
                    Map.of("taskId", task.getTaskId(), 
                           "durationMs", duration,
                           "handlersExecuted", getChainLength()));
            })
            .onFailure(error -> {
                long duration = System.currentTimeMillis() - timer;
                structuredLogger.logWarning("task_processing_failed", 
                    Map.of("taskId", task.getTaskId(),
                           "durationMs", duration,
                           "error", error.getMessage(),
                           "errorType", error.getClass().getSimpleName()));
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Build the processing chain with proper order
     */
    private TaskProcessingHandler buildProcessingChain() {
        // Build chain: Validation -> Security -> Resource Allocation
        taskValidationHandler
            .setNext(securityValidationHandler)
            .setNext(resourceAllocationHandler);
        
        return taskValidationHandler;
    }
    
    /**
     * ✅ FUNCTIONAL: Process task with custom chain (for testing/debugging)
     */
    public Result<Task, AgentError> processTaskWithCustomChain(Task task, TaskProcessingHandler customChain) {
        structuredLogger.logDebug("custom_chain_processing_started", 
            Map.of("taskId", task.getTaskId(), "customChainHandler", customChain.getHandlerName()));
        
        return customChain.handle(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Validate task without full processing (quick check)
     */
    public Result<Task, AgentError> validateTaskOnly(Task task) {
        structuredLogger.logDebug("task_validation_only", 
            Map.of("taskId", task.getTaskId()));
        
        return taskValidationHandler.handle(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Check security without other processing
     */
    public Result<Task, AgentError> validateSecurityOnly(Task task) {
        structuredLogger.logDebug("security_validation_only", 
            Map.of("taskId", task.getTaskId()));
        
        return securityValidationHandler.handle(task);
    }
    
    /**
     * ✅ FUNCTIONAL: Check resource availability without allocation
     */
    public Result<Task, AgentError> checkResourcesOnly(Task task) {
        structuredLogger.logDebug("resource_check_only", 
            Map.of("taskId", task.getTaskId()));
        
        return resourceAllocationHandler.handle(task);
    }
    
    /**
     * Get chain configuration for monitoring
     */
    public String getChainConfiguration() {
        return String.format("TaskValidation -> SecurityValidation -> ResourceAllocation");
    }
    
    /**
     * Get chain length for metrics
     */
    public int getChainLength() {
        return 3; // TaskValidation + Security + ResourceAllocation
    }
    
    /**
     * Get handler names in execution order
     */
    public java.util.List<String> getHandlerNames() {
        return java.util.List.of(
            taskValidationHandler.getHandlerName(),
            securityValidationHandler.getHandlerName(),
            resourceAllocationHandler.getHandlerName()
        );
    }
}