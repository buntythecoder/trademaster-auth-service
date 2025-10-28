package com.trademaster.auth.pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Audit Command Decorator
 *
 * Provides audit trail for command execution.
 *
 * Features:
 * - Structured logging for audit compliance
 * - Before/after execution logging
 * - Success/failure tracking
 *
 * @param <T> Command result type
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AuditCommandDecorator<T> implements Command<T> {

    private final Command<T> decorated;
    private final String commandName;

    @Override
    public CompletableFuture<Result<T, String>> execute() {
        logCommandStart();

        return decorated.execute()
            .thenApply(result -> {
                logCommandCompletion(result);
                return result;
            });
    }

    private void logCommandStart() {
        log.info("AUDIT: Command execution started",
            StructuredArguments.kv("command", commandName),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("status", "STARTED"));
    }

    private void logCommandCompletion(Result<T, String> result) {
        String status = result.isSuccess() ? "SUCCESS" : "FAILURE";
        String message = result.isSuccess()
            ? "Command execution completed successfully"
            : "Command execution failed: " + result.getError().orElse("Unknown error");

        log.info("AUDIT: Command execution completed",
            StructuredArguments.kv("command", commandName),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("message", message));
    }
}
