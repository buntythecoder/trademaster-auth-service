package com.trademaster.agentos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.TaskError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ✅ MANDATORY: Task Queue System with Redis Backend
 * 
 * Phase 1 Requirement from AGENT_OS_MVP_SPEC.md:
 * - Simple task queue system with Redis backend
 * - Priority-based task scheduling
 * - Task lifecycle management with Virtual Threads
 * - Distributed task processing across agents
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskQueueService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    @Value("${agentos.tasks.queue-size:1000}")
    private int maxQueueSize;
    
    @Value("${agentos.tasks.retry-max-attempts:3}")
    private int maxRetryAttempts;
    
    // ✅ REDIS KEYS: Task queue structure
    private static final String TASK_QUEUE_PREFIX = "agentos:task:queue:";
    private static final String TASK_DATA_PREFIX = "agentos:task:data:";
    private static final String TASK_PROCESSING_PREFIX = "agentos:task:processing:";
    private static final String TASK_FAILED_PREFIX = "agentos:task:failed:";
    
    // ✅ PRIORITY QUEUES: Separate queues by priority
    private static final String HIGH_PRIORITY_QUEUE = TASK_QUEUE_PREFIX + "high";
    private static final String NORMAL_PRIORITY_QUEUE = TASK_QUEUE_PREFIX + "normal";
    private static final String LOW_PRIORITY_QUEUE = TASK_QUEUE_PREFIX + "low";
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Add task to priority queue using Railway Programming
     */
    @Async
    public CompletableFuture<Boolean> enqueueTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("task_enqueue");
            
            return enqueueTaskFunctional(task)
                .onSuccess(result -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logBusinessTransaction(
                        "task_enqueue",
                        task.getTaskId().toString(),
                        "enqueue",
                        "system",
                        Map.of(
                            "priority", task.getPriority(),
                            "taskType", task.getTaskType(),
                            "queuePosition", result
                        )
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("task_enqueue", error.getErrorCode());
                    structuredLogger.logError("task_enqueue", error.getMessage(), null,
                        Map.of("taskId", task.getTaskId(), "priority", task.getPriority(), "errorCode", error.getErrorCode()));
                })
                .map(queuePosition -> true)
                .getOrElse(false);
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional task enqueue pipeline
     */
    private Result<Long, TaskError> enqueueTaskFunctional(Task task) {
        return validateQueueCapacity()
            .flatMap(ignored -> serializeTask(task))
            .flatMap(taskJson -> storeTaskData(task, taskJson))
            .flatMap(ignored -> addToQueue(task));
    }
    
    /**
     * ✅ FUNCTIONAL VALIDATION: Check queue capacity without throwing
     */
    private Result<Result.Unit, TaskError> validateQueueCapacity() {
        return Result.tryExecute(() -> getTotalQueueSize(), 
            e -> TaskError.redisError("queue_size_check", "queue_stats", e))
            .flatMap(currentSize -> 
                currentSize >= maxQueueSize 
                    ? Result.<Result.Unit, TaskError>failure(TaskError.queueFull(Math.toIntExact(currentSize), maxQueueSize))
                    : Result.<Result.Unit, TaskError>success(Result.Unit.INSTANCE)
            );
    }
    
    /**
     * ✅ FUNCTIONAL SERIALIZATION: Serialize task without throwing
     */
    private Result<String, TaskError> serializeTask(Task task) {
        try {
            String serializedTask = objectMapper.writeValueAsString(task);
            return Result.success(serializedTask);
        } catch (Exception e) {
            return Result.failure(TaskError.serializationError(task.getTaskId().toString(), e));
        }
    }
    
    /**
     * ✅ FUNCTIONAL STORAGE: Store task data in Redis
     */
    private Result<Void, TaskError> storeTaskData(Task task, String taskJson) {
        return Result.tryExecute(() -> {
            String taskKey = TASK_DATA_PREFIX + task.getTaskId();
            redisTemplate.opsForValue().set(taskKey, taskJson, Duration.ofHours(24));
            return null;
        }, e -> TaskError.redisError("store_task_data", TASK_DATA_PREFIX + task.getTaskId(), e));
    }
    
    /**
     * ✅ FUNCTIONAL QUEUE: Add task to priority queue
     */
    private Result<Long, TaskError> addToQueue(Task task) {
        return Result.tryExecute(() -> {
            String queueKey = getQueueKeyByPriority(task.getPriority());
            return redisTemplate.opsForList().rightPush(queueKey, task.getTaskId().toString());
        }, e -> TaskError.redisError("add_to_queue", getQueueKeyByPriority(task.getPriority()), e));
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Get next highest priority task using Railway Programming
     */
    @Async
    public CompletableFuture<Optional<Task>> dequeueTask(String agentId) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("task_dequeue");
            
            return dequeueTaskFunctional(agentId)
                .onSuccess(taskOpt -> {
                    timer.stop(metrics.getApiResponseTime());
                    taskOpt.ifPresentOrElse(
                        task -> structuredLogger.logBusinessTransaction(
                            "task_dequeue",
                            task.getTaskId().toString(),
                            "dequeue",
                            agentId,
                            Map.of("priority", task.getPriority(), "taskType", task.getTaskType())
                        ),
                        () -> { /* No task available - normal case */ }
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("task_dequeue", error.getErrorCode());
                    structuredLogger.logError("task_dequeue", error.getMessage(), null,
                        Map.of("agentId", agentId, "errorCode", error.getErrorCode()));
                })
                .getOrElse(Optional.empty());
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional task dequeue pipeline
     */
    private Result<Optional<Task>, TaskError> dequeueTaskFunctional(String agentId) {
        return dequeueFromQueueFunctional(HIGH_PRIORITY_QUEUE, agentId)
            .flatMap(taskOpt -> taskOpt.isPresent() 
                ? Result.<Optional<Task>, TaskError>success(taskOpt)
                : dequeueFromQueueFunctional(NORMAL_PRIORITY_QUEUE, agentId))
            .flatMap(taskOpt -> taskOpt.isPresent()
                ? Result.<Optional<Task>, TaskError>success(taskOpt)
                : dequeueFromQueueFunctional(LOW_PRIORITY_QUEUE, agentId))
            .flatMap(taskOpt -> taskOpt.map(task -> 
                markTaskAsProcessingFunctional(task, agentId)
                    .map(ignored -> Optional.of(task))
            ).orElse(Result.<Optional<Task>, TaskError>success(Optional.empty())));
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Mark task as completed using Railway Programming
     */
    @Async
    public CompletableFuture<Boolean> completeTask(Long taskId, String agentId, String result) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("task_complete");
            
            return completeTaskFunctional(taskId, agentId, result)
                .onSuccess(task -> {
                    timer.stop(metrics.getApiResponseTime());
                    structuredLogger.logTaskExecution(
                        taskId.toString(),
                        task.getTaskType().toString(),
                        agentId,
                        task.getPriority().toString(),
                        System.currentTimeMillis() - task.getCreatedAt().toEpochMilli()
                    );
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("task_complete", error.getErrorCode());
                    structuredLogger.logError("task_complete", error.getMessage(), null,
                        Map.of("taskId", taskId, "agentId", agentId, "errorCode", error.getErrorCode()));
                })
                .map(task -> true)
                .getOrElse(false);
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional task completion pipeline
     */
    private Result<Task, TaskError> completeTaskFunctional(Long taskId, String agentId, String result) {
        return removeFromProcessingQueue(taskId, agentId)
            .flatMap(ignored -> getTaskByIdFunctional(taskId))
            .map(this::markTaskCompleted)
            .flatMap(task -> persistCompletedTask(task, taskId));
    }
    
    /**
     * ✅ FUNCTIONAL CLEANUP: Remove task from processing queue
     */
    private Result<Void, TaskError> removeFromProcessingQueue(Long taskId, String agentId) {
        return Result.tryExecute(() -> {
            String processingKey = TASK_PROCESSING_PREFIX + agentId;
            redisTemplate.opsForSet().remove(processingKey, taskId.toString());
            return null;
        }, e -> TaskError.redisError("remove_from_processing", TASK_PROCESSING_PREFIX + agentId, e));
    }
    
    /**
     * ✅ FUNCTIONAL UPDATE: Mark task as completed
     */
    private Task markTaskCompleted(Task task) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setProgressPercentage(100);
        return task;
    }
    
    /**
     * ✅ FUNCTIONAL PERSISTENCE: Save completed task
     */
    private Result<Task, TaskError> persistCompletedTask(Task task, Long taskId) {
        try {
            String taskJson = objectMapper.writeValueAsString(task);
            return Result.tryExecute(() -> {
                String taskKey = TASK_DATA_PREFIX + taskId;
                redisTemplate.opsForValue().set(taskKey, taskJson, Duration.ofDays(7));
                return task;
            }, e -> TaskError.redisError("persist_completed_task", TASK_DATA_PREFIX + taskId, e));
        } catch (Exception e) {
            return Result.failure(TaskError.serializationError(taskId.toString(), e));
        }
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Mark task as failed with retry logic using Railway Programming
     */
    @Async
    public CompletableFuture<Boolean> failTask(Long taskId, String agentId, String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("task_fail");
            
            return processTaskFailureFunctional(taskId, agentId, errorMessage)
                .onSuccess(task -> {
                    timer.stop(metrics.getApiResponseTime());
                })
                .onFailure(error -> {
                    timer.stop(metrics.getApiResponseTime());
                    metrics.recordError("task_fail", error.getErrorCode());
                    structuredLogger.logError("task_fail", error.getMessage(), null,
                        Map.of("taskId", taskId, "agentId", agentId, "errorMessage", errorMessage, "errorCode", error.getErrorCode()));
                })
                .map(task -> true)
                .getOrElse(false);
        });
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Functional task failure processing pipeline
     */
    private Result<Task, TaskError> processTaskFailureFunctional(Long taskId, String agentId, String errorMessage) {
        return getTaskByIdFunctional(taskId)
            .flatMap(task -> handleTaskFailureRetryFunctional(task, agentId, errorMessage));
    }
    
    /**
     * ✅ RAILWAY PROGRAMMING: Handle retry logic for failed task
     */
    private Result<Task, TaskError> handleTaskFailureRetryFunctional(Task task, String agentId, String errorMessage) {
        return task.getRetryCount() < maxRetryAttempts
            ? retryFailedTaskFunctional(task, agentId, errorMessage)
            : markTaskAsPermanentlyFailedFunctional(task, agentId, errorMessage);
    }
    
    /**
     * ✅ FUNCTIONAL RETRY: Retry a failed task using functional pipeline
     */
    private Result<Task, TaskError> retryFailedTaskFunctional(Task task, String agentId, String errorMessage) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setStatus(TaskStatus.PENDING);
        
        // Re-enqueue task asynchronously (fire and forget)
        enqueueTask(task);
        
        structuredLogger.logBusinessTransaction(
            "task_retry",
            task.getTaskId().toString(),
            "retry",
            agentId,
            Map.of("retryCount", task.getRetryCount(), "error", errorMessage)
        );
        
        return cleanupAndPersistTaskFunctional(task, agentId);
    }
    
    /**
     * ✅ FUNCTIONAL FAILURE: Mark task as permanently failed using functional pipeline
     */
    private Result<Task, TaskError> markTaskAsPermanentlyFailedFunctional(Task task, String agentId, String errorMessage) {
        task.setStatus(TaskStatus.FAILED);
        task.setCompletedAt(Instant.now());
        
        return recordFailedTaskFunctional(task, agentId, errorMessage)
            .flatMap(ignored -> {
                logTaskFailure(task, agentId, errorMessage);
                return cleanupAndPersistTaskFunctional(task, agentId);
            });
    }
    
    /**
     * ✅ FUNCTIONAL RECORDING: Record failed task for analysis using Result monad
     */
    private Result<Void, TaskError> recordFailedTaskFunctional(Task task, String agentId, String errorMessage) {
        String failedKey = TASK_FAILED_PREFIX + Instant.now().toEpochMilli();
        Map<String, Object> failureInfo = Map.of(
            "taskId", task.getTaskId(),
            "agentId", agentId,
            "error", errorMessage,
            "retryCount", task.getRetryCount(),
            "failedAt", Instant.now()
        );
        
        try {
            String failureJson = objectMapper.writeValueAsString(failureInfo);
            return Result.tryExecute(() -> {
                redisTemplate.opsForValue().set(failedKey, failureJson, Duration.ofDays(30));
                return null;
            }, e -> TaskError.redisError("record_failed_task", failedKey, e));
        } catch (Exception e) {
            return Result.failure(TaskError.serializationError("failure_info_" + task.getTaskId(), e));
        }
    }
    
    /**
     * ✅ COGNITIVE COMPLEXITY: Log task failure (Complexity: 1)
     */
    private void logTaskFailure(Task task, String agentId, String errorMessage) {
        structuredLogger.logTaskFailure(
            task.getTaskId().toString(),
            task.getTaskType().toString(),
            agentId,
            errorMessage,
            System.currentTimeMillis() - task.getCreatedAt().toEpochMilli()
        );
    }
    
    /**
     * ✅ FUNCTIONAL CLEANUP: Cleanup and persist task changes using Result monad
     */
    private Result<Task, TaskError> cleanupAndPersistTaskFunctional(Task task, String agentId) {
        return removeFromProcessingQueue(task.getTaskId(), agentId)
            .flatMap(ignored -> {
                try {
                    String taskJson = objectMapper.writeValueAsString(task);
                    return Result.tryExecute(() -> {
                        String taskKey = TASK_DATA_PREFIX + task.getTaskId();
                        redisTemplate.opsForValue().set(taskKey, taskJson, Duration.ofDays(30));
                        return task;
                    }, e -> TaskError.redisError("persist_task", TASK_DATA_PREFIX + task.getTaskId(), e));
                } catch (Exception e) {
                    return Result.failure(TaskError.serializationError(task.getTaskId().toString(), e));
                }
            });
    }
    
    /**
     * ✅ LEGACY COMPATIBILITY: Keep original method for backward compatibility
     */
    private Boolean cleanupAndPersistTask(Task task, String agentId) {
        return cleanupAndPersistTaskFunctional(task, agentId)
            .map(persistedTask -> true)
            .getOrElse(false);
    }
    
    /**
     * ✅ FUNCTIONAL QUERY: Get queue statistics using Result monad
     */
    public TaskQueueStats getQueueStats() {
        return getQueueStatsFunctional()
            .onFailure(error -> structuredLogger.logError("get_queue_stats", error.getMessage(), null, 
                Map.of("errorCode", error.getErrorCode())))
            .getOrElse(TaskQueueStats.builder().build());
    }
    
    /**
     * ✅ FUNCTIONAL STATS: Get queue statistics pipeline
     */
    private Result<TaskQueueStats, TaskError> getQueueStatsFunctional() {
        return Result.tryExecute(() -> {
            long highPriorityCount = redisTemplate.opsForList().size(HIGH_PRIORITY_QUEUE);
            long normalPriorityCount = redisTemplate.opsForList().size(NORMAL_PRIORITY_QUEUE);
            long lowPriorityCount = redisTemplate.opsForList().size(LOW_PRIORITY_QUEUE);
            long totalTasks = highPriorityCount + normalPriorityCount + lowPriorityCount;
            
            return TaskQueueStats.builder()
                .highPriorityTasks(highPriorityCount)
                .normalPriorityTasks(normalPriorityCount)
                .lowPriorityTasks(lowPriorityCount)
                .totalTasks(totalTasks)
                .maxQueueSize(maxQueueSize)
                .utilizationPercent((double) totalTasks / maxQueueSize * 100)
                .build();
        }, e -> TaskError.redisError("get_queue_stats", "queue_statistics", e));
    }
    
    /**
     * ✅ FUNCTIONAL CLEANUP: Remove expired tasks using Result monad
     */
    @Async
    public CompletableFuture<Void> cleanupExpiredTasks() {
        return CompletableFuture.runAsync(() -> {
            structuredLogger.setOperationContext("task_cleanup");
            
            cleanupExpiredTasksFunctional()
                .onSuccess(ignored -> structuredLogger.logBusinessTransaction(
                    "task_cleanup",
                    "system",
                    "cleanup",
                    "system",
                    Map.of("cleanupTime", Instant.now())
                ))
                .onFailure(error -> structuredLogger.logError("task_cleanup", error.getMessage(), null, 
                    Map.of("errorCode", error.getErrorCode())));
        });
    }
    
    /**
     * ✅ FUNCTIONAL CLEANUP: Expired tasks cleanup pipeline
     */
    private Result<Void, TaskError> cleanupExpiredTasksFunctional() {
        return Result.tryExecute(() -> {
            Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(30));
            
            // ✅ FUNCTIONAL: Scan processing queues and re-enqueue stale tasks
            String processingPattern = TASK_PROCESSING_PREFIX + "*";
            
            return redisTemplate.keys(processingPattern).stream()
                .map(this::processStaleTaskKey)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum) > 0 ? null : null;
        }, e -> TaskError.redisError("cleanup_expired_tasks", "task_cleanup", e));
    }
    
    /**
     * ✅ FUNCTIONAL: Process individual stale task key
     */
    private Integer processStaleTaskKey(String processingKey) {
        try {
            String taskData = redisTemplate.opsForValue().get(processingKey);
            if (taskData == null) return 0;
            
            Task task = objectMapper.readValue(taskData, Task.class);
            Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(30));
            
            // ✅ FUNCTIONAL: Check if task is stale using ternary operator
            boolean isStale = task.getUpdatedAt() != null && task.getUpdatedAt().isBefore(staleThreshold);
            
            if (isStale) {
                // ✅ FUNCTIONAL: Re-enqueue stale task and remove from processing
                String queueKey = getQueueKeyByPriority(task.getPriority());
                redisTemplate.opsForList().rightPush(queueKey, task.getTaskId().toString());
                redisTemplate.delete(processingKey);
                
                log.info("Re-enqueued stale task: {} from processing queue", task.getTaskId());
                return 1;
            }
            return 0;
        } catch (Exception e) {
            log.error("Error processing stale task key: {}", processingKey, e);
            return 0;
        }
    }
    
    // ✅ PRIVATE METHODS
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Dequeue from specific queue using Result monad
     */
    private Result<Optional<Task>, TaskError> dequeueFromQueueFunctional(String queueKey, String agentId) {
        return Result.tryExecute(() -> redisTemplate.opsForList().leftPop(queueKey))
            .mapError(e -> TaskError.redisError("dequeue", queueKey, e))
            .flatMap(taskIdStr -> {
                if (taskIdStr == null) {
                    return Result.<Optional<Task>, TaskError>success(Optional.empty());
                }
                return Result.tryExecute(() -> Long.parseLong(taskIdStr))
                    .mapError(e -> TaskError.deserializationError(taskIdStr, e))
                    .flatMap(this::getTaskByIdFunctional)
                    .map(Optional::of);
            });
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Get task by ID using Result monad
     */
    private Result<Task, TaskError> getTaskByIdFunctional(Long taskId) {
        String taskKey = TASK_DATA_PREFIX + taskId;
        return Result.tryExecute(() -> redisTemplate.opsForValue().get(taskKey))
            .mapError(e -> TaskError.redisError("get_task", taskKey, e))
            .flatMap(taskJson -> {
                if (taskJson == null) {
                    return Result.<Task, TaskError>failure(TaskError.notFound(taskId.toString(), "get_task_by_id"));
                }
                try {
                    Task task = objectMapper.readValue(taskJson, Task.class);
                    return Result.<Task, TaskError>success(task);
                } catch (Exception e) {
                    return Result.<Task, TaskError>failure(TaskError.deserializationError(taskId.toString(), e));
                }
            });
    }
    
    /**
     * ✅ FUNCTIONAL ERROR HANDLING: Mark task as processing using Result monad
     */
    private Result<Void, TaskError> markTaskAsProcessingFunctional(Task task, String agentId) {
        return Result.tryExecute(() -> {
            String processingKey = TASK_PROCESSING_PREFIX + agentId;
            redisTemplate.opsForSet().add(processingKey, task.getTaskId().toString());
            redisTemplate.expire(processingKey, Duration.ofHours(2));
            return null;
        }, e -> TaskError.redisError("mark_processing", TASK_PROCESSING_PREFIX + agentId, e));
    }
    
    /**
     * ✅ LEGACY COMPATIBILITY: Keep original method for backward compatibility
     */
    private Optional<Task> getTaskById(Long taskId) {
        return getTaskByIdFunctional(taskId).toOptional();
    }
    
    /**
     * ✅ LEGACY COMPATIBILITY: Keep original method for backward compatibility 
     */
    private void markTaskAsProcessing(Task task, String agentId) {
        markTaskAsProcessingFunctional(task, agentId)
            .onFailure(error -> structuredLogger.logError("mark_task_processing", error.getMessage(), null,
                Map.of("taskId", task.getTaskId(), "agentId", agentId, "errorCode", error.getErrorCode())));
    }
    
    private String getQueueKeyByPriority(TaskPriority priority) {
        return switch (priority) {
            case CRITICAL, HIGH -> HIGH_PRIORITY_QUEUE;
            case NORMAL -> NORMAL_PRIORITY_QUEUE;
            case LOW, DEFERRED -> LOW_PRIORITY_QUEUE;
        };
    }
    
    private long getTotalQueueSize() {
        return redisTemplate.opsForList().size(HIGH_PRIORITY_QUEUE) +
               redisTemplate.opsForList().size(NORMAL_PRIORITY_QUEUE) +
               redisTemplate.opsForList().size(LOW_PRIORITY_QUEUE);
    }
    
    // ✅ DATA MODEL
    
    @lombok.Data
    @lombok.Builder
    public static class TaskQueueStats {
        private long highPriorityTasks;
        private long normalPriorityTasks;
        private long lowPriorityTasks;
        private long totalTasks;
        private int maxQueueSize;
        private double utilizationPercent;
    }
}