package com.trademaster.auth.pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command Executor Service - Centralized Command Execution
 *
 * Provides centralized execution infrastructure for all commands:
 * - Single point of command execution
 * - Automatic decorator application
 * - Batch command execution
 * - Transaction coordination
 *
 * Design Patterns:
 * - Command Pattern: Encapsulates operations as commands
 * - Decorator Pattern: Adds cross-cutting concerns
 * - Fa çade Pattern: Simplifies command execution interface
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class CommandExecutor {

    /**
     * Execute command with default decorators
     *
     * @param command Command to execute
     * @param <T> Result type
     * @return CompletableFuture with command result
     */
    public <T> CompletableFuture<Result<T, String>> execute(Command<T> command) {
        return command.execute();
    }

    /**
     * Execute command with full decorator stack
     *
     * @param command Command to execute
     * @param commandName Name for tracking
     * @param retryAttempts Number of retry attempts
     * @param <T> Result type
     * @return CompletableFuture with command result
     */
    public <T> CompletableFuture<Result<T, String>> executeWithDecorators(
            Command<T> command,
            String commandName,
            int retryAttempts) {

        return command
            .withRetry(retryAttempts)
            .withMetrics(commandName)
            .withAudit(commandName)
            .execute();
    }

    /**
     * Execute multiple commands in parallel
     *
     * @param commands List of commands to execute
     * @param <T> Result type
     * @return CompletableFuture with list of results
     */
    public <T> CompletableFuture<List<Result<T, String>>> executeParallel(List<Command<T>> commands) {
        return CompletableFuture.allOf(
            commands.stream()
                .map(Command::execute)
                .toArray(CompletableFuture[]::new)
        ).thenApply(ignored ->
            commands.stream()
                .map(Command::execute)
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        );
    }

    /**
     * Execute commands sequentially
     *
     * @param commands List of commands to execute in order
     * @param <T> Result type
     * @return CompletableFuture with list of results
     */
    public <T> CompletableFuture<List<Result<T, String>>> executeSequential(List<Command<T>> commands) {
        return commands.stream()
            .reduce(
                CompletableFuture.completedFuture(List.<Result<T, String>>of()),
                (futureResults, command) ->
                    futureResults.thenCompose(results ->
                        command.execute().thenApply(result -> {
                            List<Result<T, String>> newResults = new java.util.ArrayList<>(results);
                            newResults.add(result);
                            return newResults;
                        })
                    ),
                (f1, f2) -> f1
            );
    }

    /**
     * Execute command and unwrap result or throw exception
     *
     * @param command Command to execute
     * @param <T> Result type
     * @return CompletableFuture with unwrapped result
     */
    public <T> CompletableFuture<T> executeAndUnwrap(Command<T> command) {
        return command.execute()
            .thenApply(result -> result.orElseThrow(error ->
                new RuntimeException("Command execution failed: " + error)));
    }

    /**
     * Execute command and unwrap result or return default value
     *
     * @param command Command to execute
     * @param defaultValue Default value to return on failure
     * @param <T> Result type
     * @return CompletableFuture with unwrapped result or default value
     */
    public <T> CompletableFuture<T> executeAndUnwrap(Command<T> command, T defaultValue) {
        return command.execute()
            .thenApply(result -> result.getValue().orElse(defaultValue));
    }
}
