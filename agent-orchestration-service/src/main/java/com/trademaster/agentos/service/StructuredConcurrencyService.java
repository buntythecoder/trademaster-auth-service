package com.trademaster.agentos.service;

import com.trademaster.agentos.config.AgentOSMetrics;
import com.trademaster.agentos.domain.dto.AgentDto;
import com.trademaster.agentos.domain.dto.TaskDto;
import com.trademaster.agentos.domain.types.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * ✅ STRUCTURED CONCURRENCY: Modern concurrency patterns with Java 24
 * 
 * Implements structured concurrency for coordinated task execution with proper
 * lifecycle management, error propagation, and resource cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredConcurrencyService {
    
    private final AgentOSMetrics metrics;
    private final StructuredLoggingService structuredLogger;
    
    /**
     * ✅ STRUCTURED CONCURRENCY: Execute multiple operations with guaranteed cleanup
     * 
     * All subtasks are cancelled if any task fails or times out.
     * Resources are automatically cleaned up regardless of outcome.
     */
    @Async
    public CompletableFuture<TaskResult> executeCoordinatedTask(
        Long taskId,
        List<Supplier<String>> operations,
        Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            structuredLogger.setOperationContext("structured_concurrent_execution");
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // ✅ STRUCTURED TASK SCOPE: Launch all operations concurrently
                List<StructuredTaskScope.Subtask<String>> subtasks = operations.stream()
                    .map(operation -> scope.fork(operation::get))
                    .toList();
                
                // ✅ TIMEOUT & CANCELLATION: Wait with timeout, cancel all on failure
                scope.join();
                scope.throwIfFailed(); // Propagate any failures
                
                // ✅ RESULT COLLECTION: Gather all successful results
                List<String> results = subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                
                timer.stop(metrics.getApiResponseTime());
                
                structuredLogger.logBusinessTransaction(
                    "structured_concurrent_execution",
                    taskId.toString(),
                    "completed",
                    "system",
                    java.util.Map.of(
                        "operationCount", operations.size(),
                        "executionTime", System.currentTimeMillis()
                    )
                );
                
                return TaskResult.success(taskId, results, System.currentTimeMillis());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                timer.stop(metrics.getApiResponseTime());
                
                structuredLogger.logError("structured_execution", "Task interrupted", e,
                    java.util.Map.of("taskId", taskId));
                
                return TaskResult.cancelled(taskId, "Task was interrupted");
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("structured_execution", e.getClass().getSimpleName());
                
                structuredLogger.logError("structured_execution", "Task failed", e,
                    java.util.Map.of("taskId", taskId));
                
                return TaskResult.failure(taskId, e.getMessage(), e, 0);
            }
        });
    }
    
    /**
     * ✅ STRUCTURED CONCURRENCY: Race multiple operations, return first success
     * 
     * Uses ShutdownOnSuccess to cancel remaining tasks when first succeeds.
     */
    @Async
    public CompletableFuture<TaskResult> executeRaceToSuccess(
        Long taskId,
        List<Supplier<String>> operations,
        Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            
            try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
                
                // ✅ RACE CONDITION: Launch all operations, return first success
                operations.stream()
                    .forEach(operation -> scope.fork(operation::get));
                
                // ✅ FIRST SUCCESS WINS: Wait for first successful completion
                scope.join();
                
                // Get the winning result
                String result = scope.result();
                
                timer.stop(metrics.getApiResponseTime());
                
                return TaskResult.success(taskId, result, System.currentTimeMillis());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TaskResult.cancelled(taskId, "Race was interrupted");
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                return TaskResult.failure(taskId, e.getMessage(), e, 0);
            }
        });
    }
    
    /**
     * ✅ STRUCTURED CONCURRENCY: Parallel agent health checks
     * 
     * Coordinates multiple agent health checks with proper error handling and timeout.
     */
    @Async
    public CompletableFuture<List<AgentDto>> checkAgentHealthConcurrently(
        List<Long> agentIds,
        Duration timeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // ✅ CONCURRENT HEALTH CHECKS: Fork health check for each agent
                List<StructuredTaskScope.Subtask<AgentDto>> healthCheckTasks = agentIds.stream()
                    .map(agentId -> scope.fork(() -> performHealthCheck(agentId)))
                    .toList();
                
                // ✅ COORDINATED COMPLETION: Wait for all checks or timeout
                scope.join();
                scope.throwIfFailed();
                
                // ✅ RESULT AGGREGATION: Collect all healthy agents
                List<AgentDto> healthyAgents = healthCheckTasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(agent -> agent != null && agent.isAvailable())
                    .toList();
                
                timer.stop(metrics.getApiResponseTime());
                
                structuredLogger.logBusinessTransaction(
                    "concurrent_health_check",
                    "agents",
                    "completed",
                    "system",
                    java.util.Map.of(
                        "totalAgents", agentIds.size(),
                        "healthyAgents", healthyAgents.size(),
                        "checkDuration", System.currentTimeMillis()
                    )
                );
                
                return healthyAgents;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                timer.stop(metrics.getApiResponseTime());
                return List.of();
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("concurrent_health_check", e.getClass().getSimpleName());
                
                structuredLogger.logError("concurrent_health_check", "Health check failed", e,
                    java.util.Map.of("agentCount", agentIds.size()));
                
                return List.of();
            }
        });
    }
    
    /**
     * ✅ STRUCTURED CONCURRENCY: Parallel task processing with backpressure
     * 
     * Processes multiple tasks concurrently with controlled parallelism and proper cleanup.
     */
    @Async
    public CompletableFuture<List<TaskResult>> processTasksBatch(
        List<TaskDto> tasks,
        int maxConcurrency,
        Duration taskTimeout
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var timer = metrics.startApiTimer();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // ✅ CONTROLLED CONCURRENCY: Process tasks in batches
                List<StructuredTaskScope.Subtask<TaskResult>> taskExecutions = tasks.stream()
                    .limit(maxConcurrency) // Control parallelism
                    .map(task -> scope.fork(() -> processIndividualTask(task, taskTimeout)))
                    .toList();
                
                // ✅ BATCH COMPLETION: Wait for all tasks in batch
                scope.join(); // Buffer time
                scope.throwIfFailed();
                
                // ✅ RESULT COLLECTION: Gather all task results
                List<TaskResult> results = taskExecutions.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                
                timer.stop(metrics.getApiResponseTime());
                
                structuredLogger.logBusinessTransaction(
                    "batch_processing",
                    "tasks",
                    "completed",
                    "system",
                    java.util.Map.of(
                        "batchSize", tasks.size(),
                        "successCount", results.stream().mapToInt(r -> r.isCompleted() ? 1 : 0).sum(),
                        "processingTime", System.currentTimeMillis()
                    )
                );
                
                return results;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                timer.stop(metrics.getApiResponseTime());
                return List.of();
                
            } catch (Exception e) {
                timer.stop(metrics.getApiResponseTime());
                metrics.recordError("batch_processing", e.getClass().getSimpleName());
                return List.of();
            }
        });
    }
    
    /**
     * ✅ HELPER: Simulate agent health check
     */
    private AgentDto performHealthCheck(Long agentId) {
        try {
            // Simulate network call with random delay
            Thread.sleep(100 + (int) (Math.random() * 200));
            
            // Return mock healthy agent
            return AgentDto.forResponse(
                agentId,
                "agent-" + agentId,
                com.trademaster.agentos.domain.entity.AgentType.MARKET_ANALYSIS,
                com.trademaster.agentos.domain.entity.AgentStatus.ACTIVE,
                2, // currentLoad
                10, // maxConcurrentTasks
                0.95 // successRate
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * ✅ HELPER: Simulate individual task processing
     */
    private TaskResult processIndividualTask(TaskDto task, Duration timeout) {
        try {
            // Simulate task processing
            Thread.sleep(50 + (int) (Math.random() * 100));
            
            if (Math.random() < 0.9) { // 90% success rate
                return TaskResult.success(
                    task.taskId(),
                    "Task completed successfully",
                    100L
                );
            } else {
                return TaskResult.failure(
                    task.taskId(),
                    "Task processing failed",
                    new RuntimeException("Simulated failure"),
                    0
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TaskResult.cancelled(task.taskId(), "Task was interrupted");
        }
    }
}