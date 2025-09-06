package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.domain.entity.TaskType;
import com.trademaster.agentos.domain.entity.AgentCapability;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.AgentError;
import com.trademaster.agentos.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Task Service
 * 
 * Business logic layer for Task management, execution tracking, queue management,
 * and performance analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    // Task Creation & Lifecycle Management

    /**
     * Create a new task
     */
    public Task createTask(Task task) {
        log.info("Creating new task: {} of type {}", task.getTaskName(), task.getTaskType());
        
        // Set initial status and timestamps
        task.setStatus(TaskStatus.PENDING);
        task.setProgressPercentage(0);
        task.setRetryCount(0);
        
        // Set default values if not provided
        if (task.getMaxRetries() == null) {
            task.setMaxRetries(3);
        }
        if (task.getTimeoutSeconds() == null) {
            task.setTimeoutSeconds(300); // 5 minutes default
        }
        if (task.getPriority() == null) {
            task.setPriority(TaskPriority.NORMAL);
        }
        
        Task savedTask = taskRepository.save(task);
        log.info("Successfully created task: {} with ID: {}", savedTask.getTaskName(), savedTask.getTaskId());
        
        return savedTask;
    }

    /**
     * Update task status
     */
    public void updateTaskStatus(Long taskId, TaskStatus status) {
        log.debug("Updating task {} status to {}", taskId, status);
        taskRepository.updateTaskStatus(taskId, status);
        
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            
            // Set timestamps based on status
            switch (status) {
                case IN_PROGRESS:
                    if (task.getStartedAt() == null) {
                        task.setStartedAt(Instant.now());
                    }
                    break;
                case COMPLETED:
                    task.markAsCompleted(null);
                    break;
                case FAILED:
                case ERROR:
                    task.setCompletedAt(Instant.now());
                    break;
            }
            
            taskRepository.save(task);
        }
    }

    /**
     * Update task progress
     */
    public void updateTaskProgress(Long taskId, Integer progress) {
        log.debug("Updating task {} progress to {}%", taskId, progress);
        taskRepository.updateTaskProgress(taskId, progress);
    }

    /**
     * Complete task with result
     */
    public void completeTask(Long taskId, String result) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.markAsCompleted(result);
            taskRepository.save(task);
            
            log.info("Task {} completed successfully", task.getTaskName());
        } else {
            log.warn("Attempted to complete unknown task ID: {}", taskId);
        }
    }

    /**
     * Fail task with error message
     */
    public void failTask(Long taskId, String errorMessage) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.markAsFailed(errorMessage);
            taskRepository.save(task);
            
            log.warn("Task {} failed: {}", task.getTaskName(), errorMessage);
        } else {
            log.warn("Attempted to fail unknown task ID: {}", taskId);
        }
    }

    /**
     * Cancel task
     */
    public void cancelTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setStatus(TaskStatus.CANCELLED);
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);
            
            log.info("Task {} cancelled", task.getTaskName());
        } else {
            log.warn("Attempted to cancel unknown task ID: {}", taskId);
        }
    }

    // Task Assignment & Queue Management

    /**
     * Assign task to agent
     */
    public Result<Task, AgentError> assignTaskToAgent(Long taskId, Long agentId) {
        log.debug("Assigning task {} to agent {}", taskId, agentId);
        return Result.<Task, AgentError>fromOptional(
                taskRepository.findById(taskId),
                new AgentError.NotFound(taskId, "assignTaskToAgent"))
            .flatMap(task -> {
                taskRepository.assignTaskToAgent(taskId, agentId);
                updateTaskStatus(taskId, TaskStatus.QUEUED);
                return Result.success(task);
            });
    }

    /**
     * Execute task asynchronously
     */
    public CompletableFuture<Result<Task, AgentError>> executeTaskAsync(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            // Simple task execution simulation
            updateTaskStatus(task.getTaskId(), TaskStatus.IN_PROGRESS);
            // Simulate processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateTaskStatus(task.getTaskId(), TaskStatus.COMPLETED);
            return Result.<Task, AgentError>success(task);
        });
    }

    /**
     * Get next task from queue for execution
     */
    @Transactional(readOnly = true)
    public Optional<Task> getNextTaskFromQueue() {
        List<Task> queuedTasks = taskRepository.findQueuedTasks();
        return queuedTasks.isEmpty() ? Optional.empty() : Optional.of(queuedTasks.get(0));
    }

    /**
     * Get high priority tasks for immediate execution
     */
    @Transactional(readOnly = true)
    public List<Task> getHighPriorityTasks() {
        return taskRepository.findHighPriorityTasks();
    }

    /**
     * Find tasks that can be executed by agent with given capabilities
     */
    @Transactional(readOnly = true)
    public List<Task> findExecutableTasksForCapabilities(List<AgentCapability> agentCapabilities) {
        return taskRepository.findExecutableTasksForCapabilities(agentCapabilities);
    }

    /**
     * Find optimal tasks for specific agent
     */
    @Transactional(readOnly = true)
    public List<Task> findOptimalTasksForAgent(List<AgentCapability> agentCapabilities) {
        return taskRepository.findOptimalTasksForAgent(agentCapabilities);
    }

    // Task Monitoring & Maintenance

    /**
     * Check for timed out tasks and handle them
     */
    public void handleTimedOutTasks() {
        log.debug("Checking for timed out tasks");
        
        Instant timeoutThreshold = Instant.now().minus(10, ChronoUnit.MINUTES); // Default timeout
        List<Task> timedOutTasks = taskRepository.findTimedOutTasks(timeoutThreshold);
        
        for (Task task : timedOutTasks) {
            log.warn("Task {} has timed out", task.getTaskName());
            
            if (task.getRetryCount() < task.getMaxRetries()) {
                // Retry the task
                task.setStatus(TaskStatus.PENDING);
                task.setRetryCount(task.getRetryCount() + 1);
                task.setStartedAt(null);
                taskRepository.save(task);
                
                log.info("Retrying timed out task: {} (attempt {}/{})", 
                        task.getTaskName(), task.getRetryCount(), task.getMaxRetries());
            } else {
                // Mark as failed
                task.markAsFailed("Task timed out after maximum retries");
                taskRepository.save(task);
                
                log.error("Task {} failed after maximum timeout retries", task.getTaskName());
            }
        }
        
        log.debug("Processed {} timed out tasks", timedOutTasks.size());
    }

    /**
     * ✅ COGNITIVE COMPLEXITY: Handle failed tasks that can be retried (Complexity: 3)
     */
    public void handleRetriableTasks() {
        log.debug("Processing retriable tasks");
        
        List<Task> processedTasks = taskRepository.findRetriableTasks()
            .stream()
            .map(this::resetTaskForRetry)
            .map(taskRepository::save)
            .peek(this::logTaskRetry)
            .toList();
            
        log.debug("Processed {} retriable tasks", processedTasks.size());
    }
    
    /**
     * ✅ COGNITIVE COMPLEXITY: Reset a task for retry (Complexity: 1)
     */
    private Task resetTaskForRetry(Task task) {
        task.setStatus(TaskStatus.PENDING);
        task.setAgentId(null);
        task.setStartedAt(null);
        task.setErrorMessage(null);
        taskRepository.incrementRetryCount(task.getTaskId());
        return task;
    }
    
    /**
     * ✅ COGNITIVE COMPLEXITY: Log task retry information (Complexity: 1)
     */
    private void logTaskRetry(Task task) {
        log.info("Retrying failed task: {} (attempt {}/{})", 
                task.getTaskName(), task.getRetryCount() + 1, task.getMaxRetries());
    }

    /**
     * Find and handle overdue tasks
     */
    public void handleOverdueTasks() {
        log.debug("Checking for overdue tasks");
        
        List<Task> overdueTasks = taskRepository.findOverdueTasks();
        
        for (Task task : overdueTasks) {
            log.warn("Task {} is overdue (deadline: {})", task.getTaskName(), task.getDeadline());
            
            // Could implement escalation logic here
            // For now, just log the overdue tasks
        }
        
        if (!overdueTasks.isEmpty()) {
            log.warn("Found {} overdue tasks", overdueTasks.size());
        }
    }

    /**
     * Clean up old completed tasks
     */
    public void cleanupOldTasks() {
        log.debug("Cleaning up old completed tasks");
        
        // Find tasks completed more than 30 days ago
        Instant cleanupThreshold = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Task> oldTasks = taskRepository.findTasksCompletedBetween(
                Instant.EPOCH, cleanupThreshold);
        
        // In a real system, you might archive these to a separate table
        // For now, we'll keep them for historical data
        
        log.debug("Found {} old completed tasks (not deleted, keeping for history)", oldTasks.size());
    }

    // Query Methods

    /**
     * Find task by ID
     */
    @Transactional(readOnly = true)
    public Optional<Task> findById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * Find tasks by status
     */
    @Transactional(readOnly = true)
    public List<Task> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    /**
     * Find tasks by type
     */
    @Transactional(readOnly = true)
    public List<Task> findByType(TaskType taskType) {
        return taskRepository.findByTaskType(taskType);
    }

    /**
     * Find tasks by user
     */
    @Transactional(readOnly = true)
    public List<Task> findByUserId(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    /**
     * Find tasks by agent
     */
    @Transactional(readOnly = true)
    public List<Task> findByAgentId(Long agentId) {
        return taskRepository.findByAgentId(agentId);
    }

    /**
     * Get all tasks with pagination
     */
    @Transactional(readOnly = true)
    public Page<Task> findAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }

    /**
     * Get task statistics
     */
    @Transactional(readOnly = true)
    public TaskStatisticsSummary getTaskStatistics() {
        Object[] stats = taskRepository.getSystemTaskStatistics();
        
        return TaskStatisticsSummary.builder()
                .totalTasks(((Number) stats[0]).longValue())
                .pendingTasks(((Number) stats[1]).longValue())
                .queuedTasks(((Number) stats[2]).longValue())
                .inProgressTasks(((Number) stats[3]).longValue())
                .completedTasks(((Number) stats[4]).longValue())
                .failedTasks(((Number) stats[5]).longValue())
                .averageDuration(stats[6] != null ? ((Number) stats[6]).doubleValue() : 0.0)
                .build();
    }

    /**
     * Get task statistics by type
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTaskStatisticsByType() {
        return taskRepository.getTaskStatisticsByType();
    }
    
    /**
     * ✅ FUNCTIONAL: Find tasks that can be rebalanced from overloaded agent
     * Cognitive Complexity: 2
     */
    @Transactional(readOnly = true)
    public List<Task> findRebalanceableTasks(Long agentId) {
        return taskRepository.findAll().stream()
            .filter(task -> task.getAgent() != null && task.getAgent().getAgentId().equals(agentId))
            .filter(task -> task.getStatus() == com.trademaster.agentos.domain.entity.TaskStatus.QUEUED || 
                           task.getStatus() == com.trademaster.agentos.domain.entity.TaskStatus.PENDING)
            .limit(3) // Only rebalance a few tasks at a time
            .collect(java.util.stream.Collectors.toList());
    }

    // Helper Classes

    @lombok.Data
    @lombok.Builder
    public static class TaskStatisticsSummary {
        private Long totalTasks;
        private Long pendingTasks;
        private Long queuedTasks;
        private Long inProgressTasks;
        private Long completedTasks;
        private Long failedTasks;
        private Double averageDuration;
    }
}