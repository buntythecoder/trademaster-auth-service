package com.trademaster.agentos.controller;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.domain.entity.TaskStatus;
import com.trademaster.agentos.domain.entity.TaskType;
import com.trademaster.agentos.domain.entity.TaskPriority;
import com.trademaster.agentos.service.TaskService;
import com.trademaster.agentos.service.AgentOrchestrationService;
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
        
        try {
            Task submittedTask = orchestrationService.submitTask(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(submittedTask);
        } catch (IllegalArgumentException e) {
            log.warn("Task submission failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error submitting task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        
        try {
            taskService.updateTaskStatus(taskId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating task status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update task progress
     */
    @PutMapping("/{taskId}/progress")
    public ResponseEntity<Void> updateTaskProgress(
            @PathVariable Long taskId,
            @RequestParam Integer progress) {
        log.debug("REST: Updating task {} progress to {}%", taskId, progress);
        
        try {
            taskService.updateTaskProgress(taskId, progress);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating task progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete a task
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) String result) {
        log.info("REST: Completing task: {}", taskId);
        
        try {
            taskService.completeTask(taskId, result);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error completing task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Fail a task
     */
    @PostMapping("/{taskId}/fail")
    public ResponseEntity<Void> failTask(
            @PathVariable Long taskId,
            @RequestBody String errorMessage) {
        log.warn("REST: Failing task: {} with error: {}", taskId, errorMessage);
        
        try {
            taskService.failTask(taskId, errorMessage);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error failing task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel a task
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Void> cancelTask(@PathVariable Long taskId) {
        log.info("REST: Cancelling task: {}", taskId);
        
        try {
            taskService.cancelTask(taskId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cancelling task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        
        try {
            orchestrationService.assignTaskToAgent(taskId, agentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error assigning task to agent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
        
        try {
            orchestrationService.notifyTaskCompletion(taskId, success, result, responseTimeMs);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing task completion notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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