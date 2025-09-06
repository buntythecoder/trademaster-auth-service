package com.trademaster.agentos.controller;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskType;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.service.TaskService;
import com.trademaster.agentos.service.AgentOrchestrationService;
import com.trademaster.agentos.functional.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Task Controller
 * 
 * REST API endpoints for task management including task submission,
 * status updates, progress tracking, and task monitoring.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {

    private final TaskService taskService;
    private final AgentOrchestrationService orchestrationService;

    // Task Management

    /**
     * Submit a new task for execution
     */
    @PostMapping
    public ResponseEntity<Task> submitTask(@Valid @RequestBody Task task) {
        log.info("REST: Submitting new task: {} (type: {}, priority: {})", 
                task.getTaskName(), task.getTaskType(), task.getPriority());
        
        return Result.tryExecute(() -> orchestrationService.submitTask(task))
            .fold(
                submittedTask -> ResponseEntity.status(HttpStatus.CREATED).body(submittedTask),
                error -> error instanceof IllegalArgumentException
                    ? ResponseEntity.badRequest().<Task>build()
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Task>build()
            );
    }

    /**
     * Get task by ID
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTask(@PathVariable Long taskId) {
        log.debug("REST: Getting task: {}", taskId);
        
        Optional<Task> task = taskService.findById(taskId);
        return task.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all tasks with pagination
     */
    @GetMapping
    public ResponseEntity<Page<Task>> getAllTasks(Pageable pageable) {
        log.debug("REST: Getting all tasks (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Task> tasks = taskService.findAllTasks(pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable TaskStatus status) {
        log.debug("REST: Getting tasks by status: {}", status);
        
        List<Task> tasks = taskService.findByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by type
     */
    @GetMapping("/type/{taskType}")
    public ResponseEntity<List<Task>> getTasksByType(@PathVariable TaskType taskType) {
        log.debug("REST: Getting tasks by type: {}", taskType);
        
        List<Task> tasks = taskService.findByType(taskType);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getTasksByUser(@PathVariable Long userId) {
        log.debug("REST: Getting tasks for user: {}", userId);
        
        List<Task> tasks = taskService.findByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get tasks by agent
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<Task>> getTasksByAgent(@PathVariable Long agentId) {
        log.debug("REST: Getting tasks for agent: {}", agentId);
        
        List<Task> tasks = taskService.findByAgentId(agentId);
        return ResponseEntity.ok(tasks);
    }

    // Task Status Management

    /**
     * Update task status
     */
    @PutMapping("/{taskId}/status")
    public ResponseEntity<Void> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam TaskStatus status) {
        log.info("REST: Updating task {} status to {}", taskId, status);
        
        return Result.tryRun(() -> taskService.updateTaskStatus(taskId, status))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    /**
     * Update task progress
     */
    @PutMapping("/{taskId}/progress")
    public ResponseEntity<Void> updateTaskProgress(
            @PathVariable Long taskId,
            @RequestParam Integer progress) {
        log.debug("REST: Updating task {} progress to {}%", taskId, progress);
        
        return Result.tryRun(() -> taskService.updateTaskProgress(taskId, progress))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    /**
     * Complete a task
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) String result) {
        log.info("REST: Completing task: {}", taskId);
        
        return Result.tryRun(() -> taskService.completeTask(taskId, result))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    /**
     * Fail a task
     */
    @PostMapping("/{taskId}/fail")
    public ResponseEntity<Void> failTask(
            @PathVariable Long taskId,
            @RequestBody String errorMessage) {
        log.warn("REST: Failing task: {} with error: {}", taskId, errorMessage);
        
        return Result.tryRun(() -> taskService.failTask(taskId, errorMessage))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    /**
     * Cancel a task
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Void> cancelTask(@PathVariable Long taskId) {
        log.info("REST: Cancelling task: {}", taskId);
        
        return Result.tryRun(() -> taskService.cancelTask(taskId))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    // Task Assignment

    /**
     * Assign task to agent
     */
    @PostMapping("/{taskId}/assign/{agentId}")
    public ResponseEntity<Void> assignTaskToAgent(
            @PathVariable Long taskId,
            @PathVariable Long agentId) {
        log.info("REST: Assigning task {} to agent {}", taskId, agentId);
        
        return Result.tryRun(() -> orchestrationService.assignTaskToAgent(taskId, agentId))
            .fold(
                success -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    /**
     * Notify task completion (called by agents)
     */
    @PostMapping("/{taskId}/notify-completion")
    public ResponseEntity<Void> notifyTaskCompletion(
            @PathVariable Long taskId,
            @RequestParam boolean success,
            @RequestBody(required = false) String result,
            @RequestParam(defaultValue = "0") Long responseTimeMs) {
        log.info("REST: Task completion notification for task: {} (success: {})", taskId, success);
        
        return Result.tryRun(() -> orchestrationService.notifyTaskCompletion(taskId, success, result, responseTimeMs))
            .fold(
                successResult -> ResponseEntity.ok().<Void>build(),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()
            );
    }

    // Task Queue Management

    /**
     * Get next task from queue
     */
    @GetMapping("/queue/next")
    public ResponseEntity<Task> getNextTaskFromQueue() {
        log.debug("REST: Getting next task from queue");
        
        Optional<Task> nextTask = taskService.getNextTaskFromQueue();
        return nextTask.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Get high priority tasks
     */
    @GetMapping("/queue/high-priority")
    public ResponseEntity<List<Task>> getHighPriorityTasks() {
        log.debug("REST: Getting high priority tasks");
        
        List<Task> highPriorityTasks = taskService.getHighPriorityTasks();
        return ResponseEntity.ok(highPriorityTasks);
    }

    // Task Statistics and Monitoring

    /**
     * Get task statistics summary
     */
    @GetMapping("/statistics/summary")
    public ResponseEntity<TaskService.TaskStatisticsSummary> getTaskStatistics() {
        log.debug("REST: Getting task statistics summary");
        
        TaskService.TaskStatisticsSummary statistics = taskService.getTaskStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get task statistics by type
     */
    @GetMapping("/statistics/by-type")
    public ResponseEntity<List<Object[]>> getTaskStatisticsByType() {
        log.debug("REST: Getting task statistics by type");
        
        List<Object[]> statistics = taskService.getTaskStatisticsByType();
        return ResponseEntity.ok(statistics);
    }
}