package com.trademaster.agentos.domain.types;

import java.time.Instant;
import java.util.Optional;

/**
 * ✅ SEALED CLASS: Task Result Type Hierarchy
 * 
 * Type-safe representation of task execution outcomes with pattern matching support.
 * Eliminates null checks and provides exhaustive case handling.
 */
public sealed interface TaskResult 
    permits TaskResult.Success, 
            TaskResult.Failure, 
            TaskResult.Pending,
            TaskResult.Cancelled {
    
    Long taskId();
    Instant timestamp();
    
    /**
     * ✅ RECORD: Successful task completion
     */
    record Success(
        Long taskId,
        Object result,
        long executionTimeMs,
        Instant timestamp
    ) implements TaskResult {}
    
    /**
     * ✅ RECORD: Failed task execution
     */
    record Failure(
        Long taskId,
        String errorMessage,
        Optional<Throwable> cause,
        int retryCount,
        Instant timestamp
    ) implements TaskResult {}
    
    /**
     * ✅ RECORD: Task still in progress
     */
    record Pending(
        Long taskId,
        int progressPercentage,
        Optional<String> status,
        Instant timestamp
    ) implements TaskResult {}
    
    /**
     * ✅ RECORD: Task was cancelled
     */
    record Cancelled(
        Long taskId,
        String reason,
        Instant timestamp
    ) implements TaskResult {}
    
    /**
     * ✅ PATTERN MATCHING: Type-safe result processing
     */
    default boolean isCompleted() {
        return switch (this) {
            case Success(var taskId, var result, var time, var timestamp) -> true;
            case Failure(var taskId, var error, var cause, var retry, var timestamp) -> true;
            case Pending(var taskId, var progress, var status, var timestamp) -> false;
            case Cancelled(var taskId, var reason, var timestamp) -> true;
        };
    }
    
    /**
     * ✅ PATTERN MATCHING: Extract result value safely
     */
    default Optional<Object> getResult() {
        return switch (this) {
            case Success(var taskId, var result, var time, var timestamp) -> Optional.ofNullable(result);
            case Failure(var taskId, var error, var cause, var retry, var timestamp) -> Optional.empty();
            case Pending(var taskId, var progress, var status, var timestamp) -> Optional.empty();
            case Cancelled(var taskId, var reason, var timestamp) -> Optional.empty();
        };
    }
    
    /**
     * ✅ PATTERN MATCHING: Extract error message safely
     */
    default Optional<String> getErrorMessage() {
        return switch (this) {
            case Success(var taskId, var result, var time, var timestamp) -> Optional.empty();
            case Failure(var taskId, var error, var cause, var retry, var timestamp) -> Optional.of(error);
            case Pending(var taskId, var progress, var status, var timestamp) -> Optional.empty();
            case Cancelled(var taskId, var reason, var timestamp) -> Optional.of("Task cancelled: " + reason);
        };
    }
    
    /**
     * ✅ PATTERN MATCHING: Transform result with type safety
     */
    default <T> Optional<T> mapResult(java.util.function.Function<Object, T> mapper) {
        return switch (this) {
            case Success(var taskId, var result, var time, var timestamp) -> 
                Optional.ofNullable(result).map(mapper);
            case Failure(var taskId, var error, var cause, var retry, var timestamp) -> Optional.empty();
            case Pending(var taskId, var progress, var status, var timestamp) -> Optional.empty();
            case Cancelled(var taskId, var reason, var timestamp) -> Optional.empty();
        };
    }
    
    /**
     * ✅ PATTERN MATCHING: Handle different result types with callbacks
     */
    default void handle(
        java.util.function.Consumer<Success> onSuccess,
        java.util.function.Consumer<Failure> onFailure,
        java.util.function.Consumer<Pending> onPending,
        java.util.function.Consumer<Cancelled> onCancelled
    ) {
        switch (this) {
            case Success success -> onSuccess.accept(success);
            case Failure failure -> onFailure.accept(failure);
            case Pending pending -> onPending.accept(pending);
            case Cancelled cancelled -> onCancelled.accept(cancelled);
        }
    }
    
    /**
     * ✅ FACTORY METHODS: Type-safe result creation
     */
    static TaskResult success(Long taskId, Object result, long executionTimeMs) {
        return new Success(taskId, result, executionTimeMs, Instant.now());
    }
    
    static TaskResult failure(Long taskId, String errorMessage, Throwable cause, int retryCount) {
        return new Failure(taskId, errorMessage, Optional.ofNullable(cause), retryCount, Instant.now());
    }
    
    static TaskResult pending(Long taskId, int progressPercentage, String status) {
        return new Pending(taskId, progressPercentage, Optional.ofNullable(status), Instant.now());
    }
    
    static TaskResult cancelled(Long taskId, String reason) {
        return new Cancelled(taskId, reason, Instant.now());
    }
}