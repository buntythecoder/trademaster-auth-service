package com.trademaster.agentos.command;

import com.trademaster.agentos.functional.Result;
import com.trademaster.agentos.functional.TaskError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * ✅ COMMAND PATTERN: Functional Task Command Processor
 * 
 * MANDATORY SOLID Compliance:
 * - Single Responsibility: Command processing and execution only
 * - Open/Closed: Extensible via command implementations
 * - Interface Segregation: Focused command processing interface
 * - Dependency Inversion: Depends on command abstractions
 * 
 * MANDATORY FUNCTIONAL PROGRAMMING:
 * - No if-else statements (uses Map-based routing)
 * - Stream API for command processing
 * - CompletableFuture for async operations
 * - Result monad for error handling
 * - Immutable command queue
 * 
 * Cognitive Complexity: ≤7 per method, ≤15 total per class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskCommandProcessor {
    
    private final AtomicLong commandCounter = new AtomicLong(0);
    private final Queue<CommandExecution> commandHistory = new ConcurrentLinkedQueue<>();
    
    /**
     * ✅ COMMAND PATTERN: Command execution routing map (no if-else)
     * Maps command types to execution strategies
     */
    private final Map<String, Function<TaskCommand<?>, CompletableFuture<Result<?, TaskError>>>> commandExecutors = Map.of(
        "CREATE_TASK", this::executeCreateCommand,
        "UPDATE_TASK", this::executeUpdateCommand,
        "DELETE_TASK", this::executeDeleteCommand,
        "ASSIGN_TASK", this::executeAssignCommand,
        "COMPLETE_TASK", this::executeCompleteCommand,
        "CANCEL_TASK", this::executeCancelCommand
    );
    
    /**
     * Command execution history record
     */
    public record CommandExecution(
        String commandId,
        String commandType,
        Instant startedAt,
        Instant completedAt,
        Boolean success,
        String errorMessage
    ) {}
    
    /**
     * ✅ COMMAND PATTERN: Process single command with functional routing
     * Cognitive Complexity: 3 (map lookup + async composition)
     */
    public <T> CompletableFuture<Result<T, TaskError>> processCommand(TaskCommand<T> command) {
        log.info("Processing command", Map.of(
            "commandId", command.getCommandId(),
            "commandType", command.getCommandType()
        ));
        
        Instant startTime = Instant.now();
        
        return getCommandExecutor(command.getCommandType())
            .apply(command)
            .thenApply(result -> {
                recordCommandExecution(command, startTime, result);
                return (Result<T, TaskError>) result;
            })
            .exceptionally(throwable -> {
                TaskError error = new TaskError.CommandExecutionError(
                    command.getCommandType(), 
                    command.getCommandId(),
                    "Command execution failed", 
                    throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable));
                recordCommandExecution(command, startTime, Result.failure(error));
                return Result.failure(error);
            });
    }
    
    /**
     * ✅ FUNCTIONAL: Helper method to handle wildcard type conversion
     * Cognitive Complexity: 1
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<Result<?, TaskError>> processCommandWithWildcard(TaskCommand<?> command) {
        // Cast is safe because Result<T,TaskError> is compatible with Result<?,TaskError>
        return (CompletableFuture<Result<?, TaskError>>) processCommand((TaskCommand) command);
    }
    
    /**
     * ✅ FUNCTIONAL STRATEGY SELECTION: Get executor without if-else
     * Cognitive Complexity: 1
     */
    private Function<TaskCommand<?>, CompletableFuture<Result<?, TaskError>>> getCommandExecutor(String commandType) {
        return commandExecutors.getOrDefault(commandType, this::executeUnknownCommand);
    }
    
    /**
     * ✅ COMMAND PATTERN: Batch command processing with functional composition
     * Cognitive Complexity: 2 (stream operations + CompletableFuture composition)
     */
    public CompletableFuture<Result<List<CommandExecution>, TaskError>> processBatch(
            List<TaskCommand<?>> commands) {
        
        log.info("Processing command batch", Map.of("batchSize", commands.size()));
        
        List<CompletableFuture<Result<?, TaskError>>> futures = commands.stream()
            .map(command -> this.processCommandWithWildcard(command))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<Result<?, TaskError>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                
                long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                long errorCount = results.size() - successCount;
                
                log.info("Batch processing completed", Map.of(
                    "successCount", successCount,
                    "errorCount", errorCount
                ));
                
                return Result.success(List.copyOf(commandHistory));
            });
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Command pipeline processing
     * Cognitive Complexity: 2
     */
    public <T, R> CompletableFuture<Result<R, TaskError>> processCommandPipeline(
            TaskCommand<T> firstCommand,
            Function<T, TaskCommand<R>> nextCommandGenerator) {
        
        return processCommand(firstCommand)
            .thenCompose(firstResult -> firstResult.fold(
                successValue -> processCommand(nextCommandGenerator.apply(successValue)),
                error -> CompletableFuture.completedFuture(Result.<R, TaskError>failure(error))
            ));
    }
    
    /**
     * ✅ COMMAND EXECUTION STRATEGIES: Individual command type processors
     */
    
    private CompletableFuture<Result<?, TaskError>> executeCreateCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Create command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeUpdateCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Update command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeDeleteCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Delete command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeAssignCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Assign command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeCompleteCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Complete command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeCancelCommand(TaskCommand<?> command) {
        return command.execute()
            .thenApply(result -> {
                log.debug("Cancel command executed", Map.of("commandId", command.getCommandId()));
                return result;
            });
    }
    
    private CompletableFuture<Result<?, TaskError>> executeUnknownCommand(TaskCommand<?> command) {
        log.warn("Unknown command type", Map.of(
            "commandId", command.getCommandId(),
            "commandType", command.getCommandType()
        ));
        return CompletableFuture.completedFuture(
            Result.failure(new TaskError.CommandExecutionError(
                command.getCommandType(),
                command.getCommandId(),
                "Unknown command type: " + command.getCommandType(), 
                null))
        );
    }
    
    /**
     * ✅ FUNCTIONAL SIDE EFFECT: Record command execution history
     * Cognitive Complexity: 1
     */
    private void recordCommandExecution(TaskCommand<?> command, Instant startTime, Result<?, TaskError> result) {
        CommandExecution execution = new CommandExecution(
            command.getCommandId(),
            command.getCommandType(),
            startTime,
            Instant.now(),
            result.isSuccess(),
            result.getError().map(Object::toString).orElse(null)
        );
        
        commandHistory.offer(execution);
        
        // Keep only last 1000 executions to prevent memory leaks
        while (commandHistory.size() > 1000) {
            commandHistory.poll();
        }
    }
    
    /**
     * ✅ QUERY METHODS: Command history and metrics
     */
    
    public List<CommandExecution> getCommandHistory() {
        return List.copyOf(commandHistory);
    }
    
    public CompletableFuture<CommandStatistics> getCommandStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> commandTypeCounts = commandHistory.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    CommandExecution::commandType,
                    java.util.stream.Collectors.counting()
                ));
            
            long totalCommands = commandHistory.size();
            long successfulCommands = commandHistory.stream()
                .mapToLong(e -> Boolean.TRUE.equals(e.success()) ? 1 : 0)
                .sum();
            
            double successRate = (totalCommands > 0) ? 
                (double) successfulCommands / totalCommands : 0.0;
            
            return new CommandStatistics(
                totalCommands,
                successfulCommands,
                successRate,
                commandTypeCounts
            );
        });
    }
    
    public record CommandStatistics(
        long totalCommands,
        long successfulCommands,
        double successRate,
        Map<String, Long> commandTypeCounts
    ) {}
    
    /**
     * ✅ COMMAND FACTORY METHODS: Functional command builders
     */
    
    public String generateCommandId() {
        return "CMD-" + commandCounter.incrementAndGet() + "-" + Instant.now().toEpochMilli();
    }
    
    /**
     * ✅ FUNCTIONAL COMMAND COMPOSITION: Async command chaining
     * Cognitive Complexity: 2
     */
    public <T, R, S> CompletableFuture<Result<S, TaskError>> chainCommands(
            TaskCommand<T> firstCommand,
            Function<T, TaskCommand<R>> secondCommandGenerator,
            Function<R, TaskCommand<S>> thirdCommandGenerator) {
        
        return processCommand(firstCommand)
            .thenCompose(firstResult -> firstResult.fold(
                firstValue -> processCommand(secondCommandGenerator.apply(firstValue))
                    .thenCompose(secondResult -> secondResult.fold(
                        secondValue -> processCommand(thirdCommandGenerator.apply(secondValue)),
                        error -> CompletableFuture.completedFuture(Result.<S, TaskError>failure(error))
                    )),
                error -> CompletableFuture.completedFuture(Result.<S, TaskError>failure(error))
            ));
    }
}
