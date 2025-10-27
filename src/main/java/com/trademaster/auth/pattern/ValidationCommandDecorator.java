package com.trademaster.auth.pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Validation Command Decorator
 *
 * Validates command execution results before returning.
 *
 * Features:
 * - Pre/post execution validation
 * - Functional validation chains
 * - Railway-oriented error handling
 *
 * @param <T> Command result type
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ValidationCommandDecorator<T> implements Command<T> {

    private final Command<T> decorated;
    private final Function<T, Result<Boolean, String>> validator;

    @Override
    public CompletableFuture<Result<T, String>> execute() {
        return decorated.execute()
            .thenApply(result -> result.flatMap(value ->
                validator.apply(value).fold(
                    error -> Result.failure("Validation failed: " + error),
                    isValid -> isValid
                        ? Result.success(value)
                        : Result.failure("Validation failed")
                )
            ));
    }
}
