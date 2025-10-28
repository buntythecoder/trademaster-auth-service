package com.trademaster.auth.pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Metrics Command Decorator
 *
 * Collects performance metrics for command execution.
 *
 * Metrics Collected:
 * - Execution duration
 * - Success/failure rates
 * - Command name tracking
 *
 * @param <T> Command result type
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MetricsCommandDecorator<T> implements Command<T> {

    private final Command<T> decorated;
    private final String commandName;

    @Override
    public CompletableFuture<Result<T, String>> execute() {
        Instant startTime = Instant.now();

        return decorated.execute()
            .thenApply(result -> {
                Duration executionTime = Duration.between(startTime, Instant.now());
                logMetrics(executionTime, result);
                return result;
            });
    }

    private void logMetrics(Duration executionTime, Result<T, String> result) {
        String status = result.isSuccess() ? "SUCCESS" : "FAILURE";

        log.info("METRICS: command={}, status={}, duration_ms={}, timestamp={}",
            commandName,
            status,
            executionTime.toMillis(),
            Instant.now());
    }
}
