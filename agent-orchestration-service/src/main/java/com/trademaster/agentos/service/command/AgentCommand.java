package com.trademaster.agentos.service.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * ✅ COMMAND PATTERN: Agent Operation Commands with Functional Programming
 * 
 * Implements Command pattern using functional interfaces for agent operations.
 * Supports async execution, rollback, and composition with Virtual Threads.
 */
@FunctionalInterface
public interface AgentCommand<T> extends Supplier<CompletableFuture<T>> {
    
    /**
     * Execute the command asynchronously
     */
    CompletableFuture<T> execute();
    
    @Override
    default CompletableFuture<T> get() {
        return execute();
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Chain commands together
     */
    default <U> AgentCommand<U> thenCompose(java.util.function.Function<T, AgentCommand<U>> mapper) {
        return () -> execute().thenCompose(result -> mapper.apply(result).execute());
    }
    
    /**
     * ✅ FUNCTIONAL COMPOSITION: Transform command result
     */
    default <U> AgentCommand<U> thenApply(java.util.function.Function<T, U> mapper) {
        return () -> execute().thenApply(mapper);
    }
    
    /**
     * ✅ ERROR HANDLING: Handle command execution failures
     */
    default AgentCommand<T> exceptionally(java.util.function.Function<Throwable, T> fallback) {
        return () -> execute().exceptionally(fallback);
    }
    
    /**
     * ✅ TIMEOUT HANDLING: Add timeout to command execution
     */
    default AgentCommand<T> withTimeout(java.time.Duration timeout) {
        return () -> execute().orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * ✅ RETRY PATTERN: Retry command execution on failure
     */
    default AgentCommand<T> withRetry(int maxAttempts, java.time.Duration delay) {
        return () -> {
            CompletableFuture<T> result = new CompletableFuture<>();
            executeWithRetry(result, maxAttempts, delay);
            return result;
        };
    }
    
    private void executeWithRetry(CompletableFuture<T> result, int attemptsLeft, java.time.Duration delay) {
        execute().whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.complete(value);
            } else if (attemptsLeft > 1) {
                CompletableFuture.delayedExecutor(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> executeWithRetry(result, attemptsLeft - 1, delay));
            } else {
                result.completeExceptionally(throwable);
            }
        });
    }
    
    /**
     * ✅ PARALLEL EXECUTION: Execute multiple commands in parallel
     */
    static <T> CompletableFuture<java.util.List<T>> executeAll(java.util.List<AgentCommand<T>> commands) {
        CompletableFuture<T>[] futures = commands.stream()
            .map(AgentCommand::execute)
            .toArray(CompletableFuture[]::new);
            
        return CompletableFuture.allOf(futures)
            .thenApply(v -> java.util.Arrays.stream(futures)
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }
    
    /**
     * ✅ BATCH PROCESSING: Execute commands in batches with backpressure
     */
    static <T> CompletableFuture<java.util.List<T>> executeBatch(
        java.util.List<AgentCommand<T>> commands, 
        int batchSize
    ) {
        if (commands.isEmpty()) {
            return CompletableFuture.completedFuture(java.util.List.of());
        }
        
        java.util.List<java.util.List<AgentCommand<T>>> batches = new java.util.ArrayList<>();
        for (int i = 0; i < commands.size(); i += batchSize) {
            batches.add(commands.subList(i, Math.min(i + batchSize, commands.size())));
        }
        
        return batches.stream()
            .map(AgentCommand::executeAll)
            .reduce((f1, f2) -> f1.thenCompose(list1 -> 
                f2.thenApply(list2 -> {
                    java.util.List<T> combined = new java.util.ArrayList<>(list1);
                    combined.addAll(list2);
                    return combined;
                })))
            .orElse(CompletableFuture.completedFuture(java.util.List.of()));
    }
}