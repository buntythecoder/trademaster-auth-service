package com.trademaster.auth.pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Retry Command Decorator - Exponential Backoff Pattern
 *
 * Provides automatic retry with exponential backoff for failed commands.
 *
 * Features:
 * - Configurable retry attempts
 * - Exponential backoff with max delay
 * - Functional composition
 * - No if-else statements (uses Optional filter chains)
 *
 * @param <T> Command result type
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RetryCommandDecorator<T> implements Command<T> {

    private final Command<T> decorated;
    private final int maxAttempts;
    private static final long MAX_DELAY_MS = 10000L; // 10 seconds max

    @Override
    public CompletableFuture<Result<T, String>> execute() {
        return executeWithBackoff(0);
    }

    /**
     * Execute with exponential backoff using functional approach
     */
    private CompletableFuture<Result<T, String>> executeWithBackoff(int currentAttempt) {
        return decorated.execute()
            .thenCompose(result ->
                Optional.of(result)
                    .filter(r -> r.isSuccess() || currentAttempt >= maxAttempts - 1)
                    .map(CompletableFuture::completedFuture)
                    .orElseGet(() -> retryAfterDelay(currentAttempt, result))
            );
    }

    /**
     * Calculate delay and retry using functional pattern
     */
    private CompletableFuture<Result<T, String>> retryAfterDelay(int currentAttempt, Result<T, String> previousResult) {
        long delayMs = calculateExponentialDelay(currentAttempt);

        log.warn("Command failed, retrying after {}ms (attempt {}/{})",
            delayMs, currentAttempt + 1, maxAttempts);

        return CompletableFuture
            .runAsync(() -> SafeOperations.safelyToResult(() -> {
                try {
                    Thread.sleep(delayMs);
                    return null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Sleep interrupted: " + e.getMessage(), e);
                }
            }).fold(
                error -> {
                    if (error.contains("InterruptedException")) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                },
                success -> null
            ))
            .thenCompose(ignored -> executeWithBackoff(currentAttempt + 1));
    }

    /**
     * Calculate exponential backoff delay (package-private for testing)
     */
    long calculateExponentialDelay(int attempt) {
        return Math.min(1000L * (long) Math.pow(2, attempt), MAX_DELAY_MS);
    }
}
