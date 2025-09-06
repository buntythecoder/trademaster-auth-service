package com.trademaster.agentos.chain.impl;

import com.trademaster.agentos.chain.TaskProcessingHandler;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.ValidationResult;
import com.trademaster.agentos.service.StructuredLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ✅ FUNCTIONAL: Task Validation Handler using Functional Validation
 * 
 * First handler in the chain that validates task properties.
 * Uses ValidationResult for comprehensive error accumulation.
 * 
 * Validation Rules:
 * - Task ID must be present
 * - Task type must be specified
 * - Required capabilities must be present
 * - Priority must be valid
 * - Payload must not be empty for certain task types
 */
@Component
@RequiredArgsConstructor
public class TaskValidationHandler extends TaskProcessingHandler {
    
    private final StructuredLoggingService structuredLogger;
    
    @Override
    public Result<Task, AgentError> handle(Task task) {
        structuredLogger.logDebug("task_validation_started", 
            Map.of("taskId", task.getTaskId(), "taskType", task.getTaskType()));
        
        return AgentError.fromValidationResult(validateTask(task))
            .onSuccess(validatedTask -> 
                structuredLogger.logDebug("task_validation_passed", 
                    Map.of("taskId", task.getTaskId())))
            .onFailure(error -> 
                structuredLogger.logWarning("task_validation_failed", 
                    Map.of("taskId", task.getTaskId(), "error", error.getMessage())))
            .flatMap(this::processNext);
    }
    
    /**
     * ✅ FUNCTIONAL: Comprehensive task validation using ValidationResult
     */
    private ValidationResult<Task> validateTask(Task task) {
        return ValidationResult.valid(task)
            .validate(t -> t.getTaskId() != null, "Task ID cannot be null")
            .validate(t -> t.getTaskType() != null, "Task type cannot be null")
            .validate(t -> t.getRequiredCapabilities() != null && !t.getRequiredCapabilities().isEmpty(),
                     "Required capabilities cannot be empty")
            .validate(t -> t.getPriority() != null, "Priority cannot be null")
            .validate(t -> isValidPriority(t.getPriority()), "Priority must be valid")
            .validate(t -> !requiresPayload(t) || hasValidInputParameters(t), "Input parameters required for this task type")
            .validate(t -> t.getCreatedAt() != null, "Created timestamp cannot be null")
            .validate(t -> t.getUpdatedAt() != null, "Updated timestamp cannot be null");
    }
    
    /**
     * ✅ FUNCTIONAL: Validation helper methods using pure functions
     */
    private boolean isValidPriority(TaskPriority priority) {
        return priority != null;
    }
    
    private boolean requiresPayload(Task task) {
        return task.getTaskType().toString().toLowerCase().contains("data") ||
               task.getTaskType().toString().toLowerCase().contains("process") ||
               task.getTaskType().toString().toLowerCase().contains("transform");
    }
    
    private boolean hasValidInputParameters(Task task) {
        return task.getInputParameters() != null && !task.getInputParameters().trim().isEmpty();
    }
    
    @Override
    public String getHandlerName() {
        return "TaskValidation";
    }
}