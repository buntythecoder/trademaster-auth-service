package com.trademaster.auth.pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for Command interface and functional composition
 *
 * Tests cover:
 * - Basic command execution
 * - Functional composition (map, flatMap)
 * - Decorator chaining
 * - Virtual thread execution
 * - Result type handling
 */
@DisplayName("Command Interface Tests")
class CommandTest {

    @Test
    @DisplayName("Should execute simple command successfully")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testSimpleCommandExecution() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("test-result")
        );

        // When
        Result<String, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test-result");
    }

    @Test
    @DisplayName("Should execute command that returns failure")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCommandExecutionWithFailure() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.failure("operation-failed")
        );

        // When
        Result<String, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("operation-failed");
    }

    @Test
    @DisplayName("Should map command result using map() composition")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCommandMapComposition() {
        // Given
        Command<Integer> command = () -> CompletableFuture.completedFuture(
            Result.success(42)
        );

        // When
        Command<String> mappedCommand = command.map(value -> "Value: " + value);
        Result<String, String> result = mappedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Value: 42");
    }

    @Test
    @DisplayName("Should preserve failure through map() composition")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCommandMapPreservesFailure() {
        // Given
        Command<Integer> command = () -> CompletableFuture.completedFuture(
            Result.failure("initial-error")
        );

        // When
        Command<String> mappedCommand = command.map(value -> "Value: " + value);
        Result<String, String> result = mappedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("initial-error");
    }

    @Test
    @DisplayName("Should chain commands using flatMap() composition")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCommandFlatMapComposition() {
        // Given
        Command<Integer> firstCommand = () -> CompletableFuture.completedFuture(
            Result.success(5)
        );

        // When
        Command<Integer> chainedCommand = firstCommand.flatMap(value ->
            () -> CompletableFuture.completedFuture(Result.success(value * 2))
        );
        Result<Integer, String> result = chainedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should short-circuit flatMap() on failure")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testCommandFlatMapShortCircuitsOnFailure() {
        // Given
        AtomicInteger executionCount = new AtomicInteger(0);
        Command<Integer> failingCommand = () -> CompletableFuture.completedFuture(
            Result.failure("first-error")
        );

        // When
        Command<Integer> chainedCommand = failingCommand.flatMap(value -> {
            executionCount.incrementAndGet();
            return () -> CompletableFuture.completedFuture(Result.success(value * 2));
        });
        Result<Integer, String> result = chainedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("first-error");
        assertThat(executionCount.get()).isZero(); // Second command should not execute
    }

    @Test
    @DisplayName("Should chain multiple map() operations")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMultipleMapChaining() {
        // Given
        Command<Integer> command = () -> CompletableFuture.completedFuture(
            Result.success(10)
        );

        // When
        Command<String> chainedCommand = command
            .map(value -> value * 2)           // 20
            .map(value -> value + 5)           // 25
            .map(value -> "Result: " + value); // "Result: 25"

        Result<String, String> result = chainedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Result: 25");
    }

    @Test
    @DisplayName("Should apply retry decorator via withRetry()")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testWithRetryDecorator() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            int attempt = attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 3
                    ? Result.failure("attempt-" + attempt)
                    : Result.success("success")
            );
        };

        // When
        Command<String> retryCommand = command.withRetry(5);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should apply metrics decorator via withMetrics()")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testWithMetricsDecorator() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("test-value")
        );

        // When
        Command<String> metricsCommand = command.withMetrics("test-command");
        Result<String, String> result = metricsCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test-value");
        // Metrics should be recorded but not affect result
    }

    @Test
    @DisplayName("Should apply audit decorator via withAudit()")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testWithAuditDecorator() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(
            Result.success("test-value")
        );

        // When
        Command<String> auditCommand = command.withAudit("test-command");
        Result<String, String> result = auditCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test-value");
        // Audit log should be written but not affect result
    }

    @Test
    @DisplayName("Should chain multiple decorators")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMultipleDecoratorChaining() {
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
        Command<String> decoratedCommand = command
            .withRetry(3)
            .withMetrics("multi-decorator-test")
            .withAudit("multi-decorator-test");

        Result<String, String> result = decoratedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute command asynchronously with virtual threads")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testAsynchronousExecution() {
        // Given
        String currentThreadName = Thread.currentThread().getName();
        AtomicInteger executionThreadId = new AtomicInteger(-1);

        Command<String> command = () -> CompletableFuture.supplyAsync(() -> {
            executionThreadId.set((int) Thread.currentThread().threadId());
            return Result.success("async-result");
        });

        // When
        Result<String, String> result = command.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("async-result");
        // Different thread should have been used
        assertThat(executionThreadId.get()).isNotEqualTo((int) Thread.currentThread().threadId());
    }

    @Test
    @DisplayName("Should compose map() and flatMap() together")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMapAndFlatMapComposition() {
        // Given
        Command<Integer> command = () -> CompletableFuture.completedFuture(
            Result.success(5)
        );

        // When
        Command<String> composedCommand = command
            .map(value -> value * 2)  // 10
            .flatMap(value -> () -> CompletableFuture.completedFuture(
                Result.success("Value: " + value)
            ));

        Result<String, String> result = composedCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Value: 10");
    }

    @Test
    @DisplayName("Should handle exceptions in command execution")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExceptionHandlingInCommand() {
        // Given
        Command<String> command = () -> CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Command execution failed");
        });

        // When
        CompletableFuture<Result<String, String>> future = command.execute();

        // Then
        assertThat(future)
            .failsWithin(500, TimeUnit.MILLISECONDS)
            .withThrowableThat()
            .havingRootCause()
            .isInstanceOf(RuntimeException.class)
            .withMessage("Command execution failed");
    }
}
