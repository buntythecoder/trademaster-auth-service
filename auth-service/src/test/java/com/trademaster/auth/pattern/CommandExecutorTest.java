package com.trademaster.auth.pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for CommandExecutor service
 *
 * Tests cover:
 * - Single command execution
 * - Parallel command execution
 * - Sequential command execution
 * - Command unwrapping
 * - Decorator integration
 */
@DisplayName("CommandExecutor Service Tests")
class CommandExecutorTest {

    private CommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new CommandExecutor();
    }

    @Test
    @DisplayName("Should execute single command successfully")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteSingleCommand() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("test-value")
        );

        // When
        Result<String, String> result = executor.execute(command).join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Should execute command with decorators")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testExecuteWithDecorators() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            int attempt = attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 2
                    ? Result.failure("attempt-" + attempt)
                    : Result.success("success")
            );
        };

        // When
        Result<String, String> result = executor.executeWithDecorators(
            command,
            "test-command",
            3
        ).join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute commands in parallel")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testExecuteParallel() {
        // Given
        Command<String> command1 = () -> CompletableFuture.completedFuture(
            Result.success("result-1")
        );
        Command<String> command2 = () -> CompletableFuture.completedFuture(
            Result.success("result-2")
        );
        Command<String> command3 = () -> CompletableFuture.completedFuture(
            Result.success("result-3")
        );

        List<Command<String>> commands = List.of(command1, command2, command3);

        // When
        List<Result<String, String>> results = executor.executeParallel(commands).join();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).getValue()).isEqualTo("result-1");
        assertThat(results.get(1).isSuccess()).isTrue();
        assertThat(results.get(1).getValue()).isEqualTo("result-2");
        assertThat(results.get(2).isSuccess()).isTrue();
        assertThat(results.get(2).getValue()).isEqualTo("result-3");
    }

    @Test
    @DisplayName("Should execute parallel commands faster than sequential")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testParallelExecutionPerformance() {
        // Given
        Command<String> slowCommand = () -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Result.success("slow-result");
        });

        List<Command<String>> commands = List.of(
            slowCommand, slowCommand, slowCommand, slowCommand, slowCommand
        );

        // When
        long startTime = System.currentTimeMillis();
        List<Result<String, String>> results = executor.executeParallel(commands).join();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertThat(results).hasSize(5);
        assertThat(results).allMatch(Result::isSuccess);
        // Parallel execution should take ~200ms, not 5*200=1000ms
        assertThat(duration).isLessThan(800);
    }

    @Test
    @DisplayName("Should handle mixed success and failure in parallel execution")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testParallelExecutionWithMixedResults() {
        // Given
        Command<String> successCommand = () -> CompletableFuture.completedFuture(
            Result.success("success")
        );
        Command<String> failureCommand = () -> CompletableFuture.completedFuture(
            Result.failure("failure")
        );

        List<Command<String>> commands = List.of(
            successCommand,
            failureCommand,
            successCommand
        );

        // When
        List<Result<String, String>> results = executor.executeParallel(commands).join();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(1).isSuccess()).isFalse();
        assertThat(results.get(2).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should execute commands sequentially")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testExecuteSequential() {
        // Given
        AtomicInteger executionOrder = new AtomicInteger(0);

        Command<Integer> command1 = () -> CompletableFuture.supplyAsync(() -> {
            int order = executionOrder.incrementAndGet();
            return Result.success(order);
        });

        Command<Integer> command2 = () -> CompletableFuture.supplyAsync(() -> {
            int order = executionOrder.incrementAndGet();
            return Result.success(order);
        });

        Command<Integer> command3 = () -> CompletableFuture.supplyAsync(() -> {
            int order = executionOrder.incrementAndGet();
            return Result.success(order);
        });

        List<Command<Integer>> commands = List.of(command1, command2, command3);

        // When
        List<Result<Integer, String>> results = executor.executeSequential(commands).join();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(1);
        assertThat(results.get(1).getValue()).isEqualTo(2);
        assertThat(results.get(2).getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should short-circuit sequential execution on failure")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSequentialExecutionShortCircuit() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);

        Command<String> command1 = () -> {
            executionCount.incrementAndGet();
            return CompletableFuture.completedFuture(Result.success("result-1"));
        };

        Command<String> command2 = () -> {
            executionCount.incrementAndGet();
            return CompletableFuture.completedFuture(Result.failure("error-2"));
        };

        Command<String> command3 = () -> {
            executionCount.incrementAndGet();
            return CompletableFuture.completedFuture(Result.success("result-3"));
        };

        List<Command<String>> commands = List.of(command1, command2, command3);

        // When
        List<Result<String, String>> results = executor.executeSequential(commands).join();

        // Then
        assertThat(results).hasSize(2); // Only first 2 commands executed
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(1).isSuccess()).isFalse();
        assertThat(executionCount.get()).isEqualTo(2); // Third command not executed
    }

    @Test
    @DisplayName("Should unwrap command result to value")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteAndUnwrap() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("unwrapped-value")
        );

        // When
        String value = executor.executeAndUnwrap(command, "default-value").join();

        // Then
        assertThat(value).isEqualTo("unwrapped-value");
    }

    @Test
    @DisplayName("Should return default value on failure when unwrapping")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteAndUnwrapWithFailure() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.failure("operation-failed")
        );

        // When
        String value = executor.executeAndUnwrap(command, "default-value").join();

        // Then
        assertThat(value).isEqualTo("default-value");
    }

    @Test
    @DisplayName("Should execute empty parallel command list")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteParallelWithEmptyList() {
        // Given
        List<Command<String>> commands = List.of();

        // When
        List<Result<String, String>> results = executor.executeParallel(commands).join();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should execute empty sequential command list")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteSequentialWithEmptyList() {
        // Given
        List<Command<String>> commands = List.of();

        // When
        List<Result<String, String>> results = executor.executeSequential(commands).join();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should execute single command in parallel list")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExecuteParallelWithSingleCommand() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("single-result")
        );
        List<Command<String>> commands = List.of(command);

        // When
        List<Result<String, String>> results = executor.executeParallel(commands).join();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).getValue()).isEqualTo("single-result");
    }

    @Test
    @DisplayName("Should integrate decorators with parallel execution")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testParallelExecutionWithDecorators() {
        // Given
        AtomicInteger attempts1 = new AtomicInteger(0);
        AtomicInteger attempts2 = new AtomicInteger(0);

        Command<String> command1 = () -> {
            int attempt = attempts1.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 2 ? Result.failure("fail-1") : Result.success("success-1")
            );
        };

        Command<String> command2 = () -> {
            int attempt = attempts2.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 2 ? Result.failure("fail-2") : Result.success("success-2")
            );
        };

        List<Command<String>> commands = List.of(
            command1.withRetry(3),
            command2.withRetry(3)
        );

        // When
        List<Result<String, String>> results = executor.executeParallel(commands).join();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).getValue()).isEqualTo("success-1");
        assertThat(results.get(1).isSuccess()).isTrue();
        assertThat(results.get(1).getValue()).isEqualTo("success-2");
        assertThat(attempts1.get()).isEqualTo(2);
        assertThat(attempts2.get()).isEqualTo(2);
    }
}
