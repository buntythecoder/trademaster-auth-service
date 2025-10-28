package com.trademaster.auth.pattern;

import com.trademaster.auth.event.AuthEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Event Observer - Functional Observer Pattern
 *
 * Functional interface for observing authentication events.
 * Uses virtual threads for non-blocking async event processing.
 *
 * Features:
 * - Async event handling with CompletableFuture
 * - Functional composition via filter() and map()
 * - Virtual thread execution for scalability
 * - Result types for error handling
 *
 * Design Patterns:
 * - Observer Pattern (functional)
 * - Strategy Pattern (for event handling)
 * - Decorator Pattern (for observers with filters)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface EventObserver<T extends AuthEvent> {

    /**
     * Handle an authentication event asynchronously
     *
     * @param event The authentication event to handle
     * @return CompletableFuture with Result indicating success or failure
     */
    CompletableFuture<Result<Void, String>> onEvent(T event);

    /**
     * Create an observer from a simple consumer
     *
     * @param consumer Event consumer function
     * @return EventObserver that wraps the consumer
     */
    static <T extends AuthEvent> EventObserver<T> of(Consumer<T> consumer) {
        return event -> VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                consumer.accept(event);
                return null;
            }).fold(
                error -> Result.failure("Observer execution failed: " + error),
                ignored -> Result.success(null)
            )
        );
    }

    /**
     * Create a filtered observer that only processes events matching the predicate
     *
     * @param predicate Filter condition
     * @return Filtered observer
     */
    default EventObserver<T> filter(Predicate<T> predicate) {
        return event -> predicate.test(event)
            ? this.onEvent(event)
            : CompletableFuture.completedFuture(Result.success(null));
    }

    /**
     * Compose this observer with another observer
     * Both observers will be notified of events
     *
     * @param other Another observer to compose with
     * @return Composed observer
     */
    default EventObserver<T> andThen(EventObserver<T> other) {
        return event -> this.onEvent(event)
            .thenCompose(result -> other.onEvent(event));
    }

    /**
     * Create an observer with retry capability
     *
     * @param maxAttempts Maximum retry attempts
     * @return Observer with retry logic
     */
    default EventObserver<T> withRetry(int maxAttempts) {
        return event -> executeWithRetry(event, maxAttempts, 0);
    }

    /**
     * Execute observer with exponential backoff retry
     */
    private CompletableFuture<Result<Void, String>> executeWithRetry(
            T event,
            int maxAttempts,
            int currentAttempt) {

        return this.onEvent(event)
            .thenCompose(result ->
                result.isSuccess() || currentAttempt >= maxAttempts - 1
                    ? CompletableFuture.completedFuture(result)
                    : retryAfterDelay(event, maxAttempts, currentAttempt)
            );
    }

    /**
     * Retry after exponential backoff delay
     */
    private CompletableFuture<Result<Void, String>> retryAfterDelay(
            T event,
            int maxAttempts,
            int currentAttempt) {

        long delayMs = Math.min(1000L * (long) Math.pow(2, currentAttempt), 10000L);

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
            .thenCompose(ignored -> executeWithRetry(event, maxAttempts, currentAttempt + 1));
    }
}
