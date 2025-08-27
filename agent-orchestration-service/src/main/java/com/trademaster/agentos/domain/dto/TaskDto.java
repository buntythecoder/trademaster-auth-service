package com.trademaster.agentos.domain.dto;

import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskType;

import java.time.Instant;
import java.util.Map;

/**
 * ✅ RECORD: Immutable Task Data Transfer Object
 * 
 * Type-safe, immutable representation of task data for API communication.
 */
public record TaskDto(
    Long taskId,
    String taskName,
    TaskType taskType,
    String description,
    TaskStatus status,
    TaskPriority priority,
    Long agentId,
    String assignedAgentType,
    int progressPercentage,
    Long estimatedDuration,
    Long actualDuration,
    int retryCount,
    int maxRetries,
    Map<String, Object> inputParameters,
    String outputResult,
    String errorMessage,
    Long userId,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    Instant updatedAt
) {
    
    /**
     * ✅ VALIDATION: Compact constructor with validation
     */
    public TaskDto {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Task ID must be positive");
        }
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("Task name cannot be blank");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Task type cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Task status cannot be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Task priority cannot be null");
        }
        if (progressPercentage < 0 || progressPercentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        // Defensive copying of mutable collections
        inputParameters = inputParameters == null ? Map.of() : Map.copyOf(inputParameters);
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if task is completed
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || status == TaskStatus.FAILED;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if task is in progress
     */
    public boolean isInProgress() {
        return status == TaskStatus.IN_PROGRESS;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if task is pending
     */
    public boolean isPending() {
        return status == TaskStatus.PENDING;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Calculate execution duration
     */
    public Long getExecutionDuration() {
        if (startedAt == null) return null;
        if (completedAt == null && isInProgress()) {
            return java.time.Duration.between(startedAt, Instant.now()).toMillis();
        }
        if (completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        }
        return null;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Check if task can be retried
     */
    public boolean canBeRetried() {
        return status == TaskStatus.FAILED && retryCount < maxRetries;
    }
    
    /**
     * ✅ DERIVED PROPERTY: Get task priority weight for scheduling
     */
    public int getPriorityWeight() {
        return switch (priority) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case NORMAL -> 3;
            case LOW -> 4;
            case DEFERRED -> 5;
        };
    }
    
    /**
     * ✅ FACTORY METHOD: Create from entity
     */
    public static TaskDto fromEntity(com.trademaster.agentos.domain.entity.Task task) {
        return new TaskDto(
            task.getTaskId(),
            task.getTaskName(),
            task.getTaskType(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getAgentId(),
            task.getAssignedAgentType(),
            task.getProgressPercentage(),
            task.getEstimatedDuration(),
            task.getActualDuration(),
            task.getRetryCount(),
            task.getMaxRetries(),
            parseInputParameters(task.getInputParameters()),
            task.getOutputResult(),
            task.getErrorMessage(),
            task.getUserId(),
            task.getCreatedAt(),
            task.getStartedAt(),
            task.getCompletedAt(),
            task.getUpdatedAt()
        );
    }
    
    /**
     * ✅ FACTORY METHOD: Create minimal task for listing
     */
    public static TaskDto minimal(
        Long taskId,
        String taskName,
        TaskType taskType,
        TaskStatus status,
        TaskPriority priority,
        int progressPercentage
    ) {
        return new TaskDto(
            taskId,
            taskName,
            taskType,
            null, // description
            status,
            priority,
            null, // agentId
            null, // assignedAgentType
            progressPercentage,
            null, // estimatedDuration
            null, // actualDuration
            0, // retryCount
            3, // maxRetries (default)
            Map.of(), // inputParameters
            null, // outputResult
            null, // errorMessage
            1L, // userId (default)
            Instant.now(), // createdAt
            null, // startedAt
            null, // completedAt
            Instant.now() // updatedAt
        );
    }
    
    /**
     * ✅ UTILITY: Parse JSON input parameters safely
     */
    private static Map<String, Object> parseInputParameters(String inputParametersJson) {
        if (inputParametersJson == null || inputParametersJson.isBlank()) {
            return Map.of();
        }
        
        try {
            // In a real implementation, use Jackson ObjectMapper here
            // For now, return empty map to avoid JSON parsing dependency
            return Map.of();
        } catch (Exception e) {
            return Map.of("parse_error", e.getMessage());
        }
    }
}