package com.trademaster.auth.pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for RetryCommandDecorator
 *
 * Tests cover:
 * - Retry logic with exponential backoff
 * - Maximum retry attempts enforcement
 * - Delay calculation verification
 * - Success after retries
 * - Immediate success (no retry)
 */
@DisplayName("RetryCommandDecorator Tests")
class RetryCommandDecoratorTest {

    @Test
    @DisplayName("Should succeed on first attempt without retry")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testSuccessOnFirstAttempt() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(Result.success("first-attempt"));
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 3);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("first-attempt");
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should retry and eventually succeed")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRetryAndSucceed() {
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
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 5);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should fail after max retry attempts")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFailAfterMaxRetries() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                Result.failure("always-fail")
            );
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 3);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("always-fail");
        assertThat(attempts.get()).isEqualTo(3); // Max attempts reached
    }

    @Test
    @DisplayName("Should apply exponential backoff delays")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testExponentialBackoff() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicLong firstRetryTime = new AtomicLong(0);
        AtomicLong secondRetryTime = new AtomicLong(0);

        Command<String> command = () -> {
            int attempt = attempts.incrementAndGet();
            long currentTime = System.currentTimeMillis();

            if (attempt == 1) {
                firstRetryTime.set(currentTime);
            } else if (attempt == 2) {
                secondRetryTime.set(currentTime);
            }

            return CompletableFuture.completedFuture(
                attempt < 3
                    ? Result.failure("attempt-" + attempt)
                    : Result.success("success")
            );
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 5);
        long startTime = System.currentTimeMillis();
        Result<String, String> result = retryCommand.execute().join();
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(3);

        // Verify exponential backoff occurred
        // First retry should wait ~1000ms, second retry ~2000ms
        long totalTime = endTime - startTime;
        assertThat(totalTime).isGreaterThanOrEqualTo(2500); // At least 1s + 2s delays minus execution time
    }

    @Test
    @DisplayName("Should respect maximum delay cap (10 seconds)")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testMaximumDelayCap() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(Result.success("success"));
        };

        // When
        RetryCommandDecorator<String> retryCommand = new RetryCommandDecorator<>(command, 15);

        // Then - verify max delay is capped at 10 seconds
        long delay10 = retryCommand.calculateExponentialDelay(10); // 2^10 = 1024 seconds
        long delay15 = retryCommand.calculateExponentialDelay(15); // 2^15 = 32768 seconds

        assertThat(delay10).isEqualTo(10000); // Capped at 10 seconds
        assertThat(delay15).isEqualTo(10000); // Capped at 10 seconds
    }

    @Test
    @DisplayName("Should calculate correct exponential delays")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testExponentialDelayCalculation() {
        // Given
        Command<String> command = () -> CompletableFuture.completedFuture(Result.success("test"));
        RetryCommandDecorator<String> retryCommand = new RetryCommandDecorator<>(command, 10);

        // When & Then
        assertThat(retryCommand.calculateExponentialDelay(0)).isEqualTo(1000);  // 2^0 * 1000 = 1000ms
        assertThat(retryCommand.calculateExponentialDelay(1)).isEqualTo(2000);  // 2^1 * 1000 = 2000ms
        assertThat(retryCommand.calculateExponentialDelay(2)).isEqualTo(4000);  // 2^2 * 1000 = 4000ms
        assertThat(retryCommand.calculateExponentialDelay(3)).isEqualTo(8000);  // 2^3 * 1000 = 8000ms
        assertThat(retryCommand.calculateExponentialDelay(4)).isEqualTo(10000); // Capped at 10000ms
    }

    @Test
    @DisplayName("Should retry exactly N times for N max attempts")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testExactRetryCount() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(Result.failure("fail"));
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 5);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(attempts.get()).isEqualTo(5); // Exactly 5 attempts
    }

    @Test
    @DisplayName("Should handle single retry attempt")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testSingleRetryAttempt() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            attempts.incrementAndGet();
            return CompletableFuture.completedFuture(Result.failure("fail"));
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 1);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(attempts.get()).isEqualTo(1); // Only one attempt, no retries
    }

    @Test
    @DisplayName("Should preserve error message after retries")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPreserveErrorMessage() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<String> command = () -> {
            int attempt = attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                Result.failure("error-attempt-" + attempt)
            );
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 3);
        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("error-attempt-3"); // Last error preserved
    }

    @Test
    @DisplayName("Should work with map() composition")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRetryWithMapComposition() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<Integer> command = () -> {
            int attempt = attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 2
                    ? Result.failure("attempt-" + attempt)
                    : Result.success(42)
            );
        };

        // When
        Command<String> retryCommand = new RetryCommandDecorator<>(command, 3)
            .map(value -> "Result: " + value);

        Result<String, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Result: 42");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should work with flatMap() composition")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRetryWithFlatMapComposition() {
        // Given
        AtomicInteger attempts = new AtomicInteger(0);
        Command<Integer> command = () -> {
            int attempt = attempts.incrementAndGet();
            return CompletableFuture.completedFuture(
                attempt < 2
                    ? Result.failure("attempt-" + attempt)
                    : Result.success(10)
            );
        };

        // When
        Command<Integer> retryCommand = new RetryCommandDecorator<>(command, 3)
            .flatMap(value -> () -> CompletableFuture.completedFuture(
                Result.success(value * 2)
            ));

        Result<Integer, String> result = retryCommand.execute().join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(20);
        assertThat(attempts.get()).isEqualTo(2);
    }
}
