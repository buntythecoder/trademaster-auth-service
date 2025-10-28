package com.trademaster.auth.pattern;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Functional Command Pattern - Base Interface
 *
 * Encapsulates business operations as executable commands with:
 * - Async execution using CompletableFuture
 * - Functional composition via map/flatMap
 * - Result type for railway-oriented programming
 * - Decorator pattern support for cross-cutting concerns
 *
 * Design Principles:
 * - Single Responsibility: Each command encapsulates ONE operation
 * - Open/Closed: Extend via decorators without modifying base commands
 * - Dependency Inversion: Commands depend on abstractions (interfaces)
 *
 * @param <T> The result type of command execution
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface Command<T> {

    /**
     * Execute the command asynchronously
     *
     * @return CompletableFuture containing Result of execution
     */
    CompletableFuture<Result<T, String>> execute();

    /**
     * Transform command result using mapper function
     *
     * @param mapper Function to transform result
     * @param <U> Target result type
     * @return New command with mapped result
     */
    default <U> Command<U> map(Function<T, U> mapper) {
        return () -> this.execute()
            .thenApply(result -> result.map(mapper));
    }

    /**
     * FlatMap for monadic composition
     *
     * @param mapper Function to transform result to new command
     * @param <U> Target result type
     * @return New command with flatMapped result
     */
    default <U> Command<U> flatMap(Function<T, Command<U>> mapper) {
        return () -> this.execute()
            .thenCompose(result ->
                result.fold(
                    error -> CompletableFuture.completedFuture(Result.failure(error)),
                    value -> mapper.apply(value).execute()
                )
            );
    }

    /**
     * Add retry logic with exponential backoff
     *
     * @param attempts Maximum retry attempts
     * @return Command wrapped with retry decorator
     */
    default Command<T> withRetry(int attempts) {
        return new RetryCommandDecorator<>(this, attempts);
    }

    /**
     * Add validation before execution
     *
     * @param validator Validation function
     * @return Command wrapped with validation decorator
     */
    default Command<T> withValidation(Function<T, Result<Boolean, String>> validator) {
        return new ValidationCommandDecorator<>(this, validator);
    }

    /**
     * Add metrics collection
     *
     * @param commandName Name for metrics tracking
     * @return Command wrapped with metrics decorator
     */
    default Command<T> withMetrics(String commandName) {
        return new MetricsCommandDecorator<>(this, commandName);
    }

    /**
     * Add audit logging
     *
     * @param commandName Name for audit trail
     * @return Command wrapped with audit decorator
     */
    default Command<T> withAudit(String commandName) {
        return new AuditCommandDecorator<>(this, commandName);
    }

    /**
     * Combine multiple decorators in one call
     *
     * @param commandName Name for tracking
     * @param retryAttempts Number of retry attempts
     * @return Fully decorated command
     */
    default Command<T> withFullDecorators(String commandName, int retryAttempts) {
        return this
            .withRetry(retryAttempts)
            .withValidation(value -> Result.success(true))
            .withMetrics(commandName)
            .withAudit(commandName);
    }
}
