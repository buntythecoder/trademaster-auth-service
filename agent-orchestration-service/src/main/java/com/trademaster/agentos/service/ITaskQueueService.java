package com.trademaster.agentos.service;

import com.trademaster.agentos.domain.entity.Task;
import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.TaskError;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ✅ INTERFACE SEGREGATION: Task Queue Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Only task queue operations
 * - Interface Segregation: Focused contract for queue management
 * - Dependency Inversion: Abstractions for queue implementations
 */
public interface ITaskQueueService {
    
    /**
     * ✅ SRP: Enqueue task - single responsibility
     */
    CompletableFuture<Boolean> enqueueTask(Task task);
    
    /**
     * ✅ SRP: Dequeue task - single responsibility
     */
    CompletableFuture<Optional<Task>> dequeueTask(String agentId);
    
    /**
     * ✅ SRP: Complete task - single responsibility
     */
    CompletableFuture<Boolean> completeTask(Long taskId, String agentId, String result);
    
    /**
     * ✅ SRP: Fail task - single responsibility
     */
    CompletableFuture<Boolean> failTask(Long taskId, String agentId, String errorMessage);
    
    /**
     * ✅ SRP: Cleanup expired tasks - single responsibility
     */
    CompletableFuture<Void> cleanupExpiredTasks();
}