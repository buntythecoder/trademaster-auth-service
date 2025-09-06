package com.trademaster.agentos.command;

import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.TaskError;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.time.Instant;

/**
 * ✅ COMMAND PATTERN: Result Types for Task Operations
 */
record TaskCreationResult(
    Long taskId,
    String taskTitle,
    String status,
    Instant createdAt
) {}

record TaskUpdateResult(
    Long taskId,
    String previousStatus,
    String newStatus,
    Instant updatedAt
) {}

record TaskDeletionResult(
    Long taskId,
    String taskTitle,
    Instant deletedAt
) {}

record TaskAssignmentResult(
    Long taskId,
    Long previousAgentId,
    Long newAgentId,
    Instant assignedAt
) {}

record TaskCompletionResult(
    Long taskId,
    String finalStatus,
    String completionNotes,
    Instant completedAt
) {}

record TaskCancellationResult(
    Long taskId,
    String cancellationReason,
    Instant cancelledAt
) {}

/**
 * ✅ COMMAND PATTERN: Functional Command Interface
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Command execution contract only
 * - Interface Segregation: Focused command interface
 * - Open/Closed: Extensible via implementations
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - CompletableFuture for async execution
 * - Result monad for error handling
 * - Immutable command state
 */
public sealed interface TaskCommand<T> permits
    TaskCommand.CreateTaskCommand,
    TaskCommand.UpdateTaskCommand,
    TaskCommand.DeleteTaskCommand,
    TaskCommand.AssignTaskCommand,
    TaskCommand.CompleteTaskCommand,
    TaskCommand.CancelTaskCommand,
    TaskCommand.MappedCommand {
    
    /**
     * ✅ COMMAND PATTERN: Execute command with functional result
     */
    CompletableFuture<Result<T, TaskError>> execute();
    
    /**
     * ✅ COMMAND PATTERN: Command metadata for tracking
     */
    String getCommandId();
    Instant getCreatedAt();
    String getCommandType();
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Command transformation
     */
    default <R> TaskCommand<R> map(Function<T, R> mapper) {
        return new MappedCommand<>(this, mapper);
    }
    
    /**
     * ✅ COMMAND PATTERN: Create Task Command
     */
    record CreateTaskCommand(
        String commandId,
        Instant createdAt,
        String taskTitle,
        String taskDescription,
        Long agentId,
        String priority,
        Function<Void, CompletableFuture<Result<TaskCreationResult, TaskError>>> executor
    ) implements TaskCommand<TaskCreationResult> {
        
        @Override
        public CompletableFuture<Result<TaskCreationResult, TaskError>> execute() {
            return executor.apply(null);
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "CREATE_TASK";
        }
        
    }
    
    /**
     * ✅ COMMAND PATTERN: Update Task Command
     */
    record UpdateTaskCommand(
        String commandId,
        Instant createdAt,
        Long taskId,
        String newStatus,
        String updateDescription,
        Function<Long, CompletableFuture<Result<TaskUpdateResult, TaskError>>> executor
    ) implements TaskCommand<TaskUpdateResult> {
        
        @Override
        public CompletableFuture<Result<TaskUpdateResult, TaskError>> execute() {
            return executor.apply(taskId);
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "UPDATE_TASK";
        }
    }
    
    /**
     * ✅ COMMAND PATTERN: Delete Task Command
     */
    record DeleteTaskCommand(
        String commandId,
        Instant createdAt,
        Long taskId,
        String reason,
        Function<Long, CompletableFuture<Result<TaskDeletionResult, TaskError>>> executor
    ) implements TaskCommand<TaskDeletionResult> {
        
        @Override
        public CompletableFuture<Result<TaskDeletionResult, TaskError>> execute() {
            return executor.apply(taskId);
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "DELETE_TASK";
        }
    }
    
    /**
     * ✅ COMMAND PATTERN: Assign Task Command
     */
    record AssignTaskCommand(
        String commandId,
        Instant createdAt,
        Long taskId,
        Long targetAgentId,
        String assignmentReason,
        Function<AssignmentRequest, CompletableFuture<Result<TaskAssignmentResult, TaskError>>> executor
    ) implements TaskCommand<TaskAssignmentResult> {
        
        @Override
        public CompletableFuture<Result<TaskAssignmentResult, TaskError>> execute() {
            return executor.apply(new AssignmentRequest(taskId, targetAgentId, assignmentReason));
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "ASSIGN_TASK";
        }
        
        public record AssignmentRequest(
            Long taskId,
            Long targetAgentId,
            String reason
        ) {}
    }
    
    /**
     * ✅ COMMAND PATTERN: Complete Task Command
     */
    record CompleteTaskCommand(
        String commandId,
        Instant createdAt,
        Long taskId,
        String completionNotes,
        Boolean success,
        Function<CompletionRequest, CompletableFuture<Result<TaskCompletionResult, TaskError>>> executor
    ) implements TaskCommand<TaskCompletionResult> {
        
        @Override
        public CompletableFuture<Result<TaskCompletionResult, TaskError>> execute() {
            return executor.apply(new CompletionRequest(taskId, completionNotes, success));
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "COMPLETE_TASK";
        }
        
        public record CompletionRequest(
            Long taskId,
            String completionNotes,
            Boolean success
        ) {}
    }
    
    /**
     * ✅ COMMAND PATTERN: Cancel Task Command
     */
    record CancelTaskCommand(
        String commandId,
        Instant createdAt,
        Long taskId,
        String cancellationReason,
        Function<CancellationRequest, CompletableFuture<Result<TaskCancellationResult, TaskError>>> executor
    ) implements TaskCommand<TaskCancellationResult> {
        
        @Override
        public CompletableFuture<Result<TaskCancellationResult, TaskError>> execute() {
            return executor.apply(new CancellationRequest(taskId, cancellationReason));
        }
        
        @Override
        public String getCommandId() {
            return commandId;
        }
        
        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        @Override
        public String getCommandType() {
            return "CANCEL_TASK";
        }
        
        public record CancellationRequest(
            Long taskId,
            String reason
        ) {}
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Mapped command wrapper
     */
    record MappedCommand<T, R>(
        TaskCommand<T> originalCommand,
        Function<T, R> mapper
    ) implements TaskCommand<R> {
        
        @Override
        public CompletableFuture<Result<R, TaskError>> execute() {
            return originalCommand.execute()
                .thenApply(result -> result.map(mapper));
        }
        
        @Override
        public String getCommandId() {
            return originalCommand.getCommandId();
        }
        
        @Override
        public Instant getCreatedAt() {
            return originalCommand.getCreatedAt();
        }
        
        @Override
        public String getCommandType() {
            return "MAPPED_" + originalCommand.getCommandType();
        }
    }
}
