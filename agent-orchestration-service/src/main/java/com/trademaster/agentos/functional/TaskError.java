package com.trademaster.agentos.functional;

/**
 * ✅ FUNCTIONAL ERROR HANDLING: Task-specific error types
 * 
 * Sealed interface hierarchy for task queue operations following Rule #11
 * - No exceptions in business logic
 * - Railway Oriented Programming  
 * - Type-safe error handling
 */
public sealed interface TaskError permits 
    TaskError.SerializationError,
    TaskError.DeserializationError,
    TaskError.QueueFullError,
    TaskError.TaskNotFoundError,
    TaskError.RedisOperationError,
    TaskError.ValidationError,
    TaskError.TimeoutError,
    TaskError.CommandExecutionError,
    TaskError.NotFound,
    TaskError.AssignmentError {
    
    /**
     * Get error message for logging/debugging
     */
    String getMessage();
    
    /**
     * Get error code for classification
     */
    String getErrorCode();
    
    /**
     * JSON serialization failed
     */
    record SerializationError(String taskId, String details, Exception cause) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Failed to serialize task %s: %s", taskId, details);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_SERIALIZATION_ERROR";
        }
    }
    
    /**
     * JSON deserialization failed
     */
    record DeserializationError(String taskId, String details, Exception cause) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Failed to deserialize task %s: %s", taskId, details);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_DESERIALIZATION_ERROR";
        }
    }
    
    /**
     * Task queue at capacity
     */
    record QueueFullError(int currentSize, int maxSize) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Task queue is full: %d/%d tasks", currentSize, maxSize);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_QUEUE_FULL";
        }
    }
    
    /**
     * Task not found in storage
     */
    record TaskNotFoundError(String taskId, String operation) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Task %s not found for operation: %s", taskId, operation);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_NOT_FOUND";
        }
    }
    
    /**
     * Redis operation failed
     */
    record RedisOperationError(String operation, String key, String details, Exception cause) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Redis %s operation failed for key %s: %s", operation, key, details);
        }
        
        @Override
        public String getErrorCode() {
            return "REDIS_OPERATION_ERROR";
        }
    }
    
    /**
     * Task validation failed
     */
    record ValidationError(String taskId, String field, String reason) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Task %s validation failed for %s: %s", taskId, field, reason);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_VALIDATION_ERROR";
        }
    }
    
    /**
     * Operation timed out
     */
    record TimeoutError(String operation, long timeoutMs, long actualMs) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Operation %s timed out: %dms (limit: %dms)", operation, actualMs, timeoutMs);
        }
        
        @Override
        public String getErrorCode() {
            return "OPERATION_TIMEOUT";
        }
    }
    
    /**
     * Command execution failed
     */
    record CommandExecutionError(String commandType, String commandId, String details, Exception cause) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Command execution failed for %s (id: %s): %s", commandType, commandId, details);
        }
        
        @Override
        public String getErrorCode() {
            return "COMMAND_EXECUTION_ERROR";
        }
    }
    
    /**
     * Factory method to create serialization error from exception
     */
    static TaskError serializationError(String taskId, Exception cause) {
        return new SerializationError(taskId, cause.getMessage(), cause);
    }
    
    /**
     * Factory method to create deserialization error from exception  
     */
    static TaskError deserializationError(String taskId, Exception cause) {
        return new DeserializationError(taskId, cause.getMessage(), cause);
    }
    
    /**
     * Factory method to create Redis error from exception
     */
    static TaskError redisError(String operation, String key, Exception cause) {
        return new RedisOperationError(operation, key, cause.getMessage(), cause);
    }
    
    /**
     * Factory method for queue full error
     */
    static TaskError queueFull(int currentSize, int maxSize) {
        return new QueueFullError(currentSize, maxSize);
    }
    
    /**
     * Factory method for task not found error
     */
    static TaskError notFound(String taskId, String operation) {
        return new TaskNotFoundError(taskId, operation);
    }
    
    /**
     * Factory method for validation error
     */
    static TaskError validation(String taskId, String field, String reason) {
        return new ValidationError(taskId, field, reason);
    }
    
    /**
     * Factory method for command execution error
     */
    static TaskError commandExecutionError(String commandType, String commandId, String details, Exception cause) {
        return new CommandExecutionError(commandType, commandId, details, cause);
    }
    
    /**
     * Agent not found for task delegation
     */
    record NotFound(String taskId, String details) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("No agent found for task %s: %s", taskId, details);
        }
        
        @Override
        public String getErrorCode() {
            return "AGENT_NOT_FOUND";
        }
    }
    
    /**
     * Task assignment to agent failed
     */
    record AssignmentError(String taskId, String agentId, String details) implements TaskError {
        @Override
        public String getMessage() {
            return String.format("Failed to assign task %s to agent %s: %s", taskId, agentId, details);
        }
        
        @Override
        public String getErrorCode() {
            return "TASK_ASSIGNMENT_ERROR";
        }
    }
    
    /**
     * Factory method for agent not found error
     */
    static TaskError agentNotFound(String taskId, String details) {
        return new NotFound(taskId, details);
    }
    
    /**
     * Factory method for assignment error
     */
    static TaskError assignmentError(String taskId, String agentId, String details) {
        return new AssignmentError(taskId, agentId, details);
    }
}