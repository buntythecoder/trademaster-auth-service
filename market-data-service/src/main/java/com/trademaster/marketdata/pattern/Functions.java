package com.trademaster.marketdata.pattern;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * Higher-order function utilities for functional composition and enhancement
 * Provides retry, memoization, circuit breaker, and composition patterns
 */
public final class Functions {
    
    private Functions() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Function composition
    @SafeVarargs
    public static <T> Function<T, T> compose(Function<T, T>... functions) {
        return input -> {
            T result = input;
            for (Function<T, T> function : functions) {
                result = function.apply(result);
            }
            return result;
        };
    }
    
    public static <T, U, R> Function<T, R> compose(Function<T, U> first, Function<U, R> second) {
        return input -> second.apply(first.apply(input));
    }
    
    // Predicate composition
    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<T>... predicates) {
        return input -> {
            for (Predicate<T> predicate : predicates) {
                if (!predicate.test(input)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    @SafeVarargs
    public static <T> Predicate<T> or(Predicate<T>... predicates) {
        return input -> {
            for (Predicate<T> predicate : predicates) {
                if (predicate.test(input)) {
                    return true;
                }
            }
            return false;
        };
    }
    
    // Retry pattern
    public static <T, R> Function<T, R> withRetry(
            Function<T, R> function, 
            int maxAttempts,
            Duration delay) {
        return input -> {
            Exception lastException = null;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                try {
                    return function.apply(input);
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxAttempts - 1) {
                        try {
                            Thread.sleep(delay.toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry", ie);
                        }
                    }
                }
            }
            throw new RuntimeException("Failed after " + maxAttempts + " attempts", lastException);
        };
    }
    
    // Memoization
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                return cache.computeIfAbsent(input, function);
            }
        };
    }
    
    // Time-based memoization with expiration
    public static <T, R> Function<T, R> memoizeWithExpiration(
            Function<T, R> function, Duration expiration) {
        return new Function<T, R>() {
            private final Map<T, TimestampedValue<R>> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                TimestampedValue<R> cached = cache.get(input);
                if (cached != null && cached.isValid(expiration)) {
                    return cached.value();
                }
                
                R result = function.apply(input);
                cache.put(input, new TimestampedValue<>(result, Instant.now()));
                return result;
            }
        };
    }
    
    // Circuit breaker pattern
    public static <T, R> Function<T, Result<R, String>> withCircuitBreaker(
            Function<T, R> function,
            int failureThreshold,
            Duration timeout) {
        return new CircuitBreakerFunction<>(function, failureThreshold, timeout);
    }
    
    // Safe function execution
    public static <T, R> Function<T, Result<R, String>> safely(Function<T, R> function) {
        return input -> {
            try {
                return Result.success(function.apply(input));
            } catch (Exception e) {
                return Result.failure(e.getMessage());
            }
        };
    }
    
    // Conditional execution
    public static <T> Function<T, T> when(Predicate<T> condition, Function<T, T> action) {
        return input -> condition.test(input) ? action.apply(input) : input;
    }
    
    public static <T> Function<T, T> unless(Predicate<T> condition, Function<T, T> action) {
        return when(condition.negate(), action);
    }
    
    // Function lifting to Optional
    public static <T, R> Function<T, java.util.Optional<R>> lift(Function<T, R> function) {
        return input -> {
            try {
                return java.util.Optional.ofNullable(function.apply(input));
            } catch (Exception e) {
                return java.util.Optional.empty();
            }
        };
    }
    
    // Currying support
    public static <T, U, R> Function<T, Function<U, R>> curry(BiFunction<T, U, R> biFunction) {
        return t -> u -> biFunction.apply(t, u);
    }
    
    public static <T, U, R> BiFunction<T, U, R> uncurry(Function<T, Function<U, R>> curriedFunction) {
        return (t, u) -> curriedFunction.apply(t).apply(u);
    }
    
    // Supporting classes
    private record TimestampedValue<T>(T value, Instant timestamp) {
        boolean isValid(Duration expiration) {
            return timestamp.plus(expiration).isAfter(Instant.now());
        }
    }
    
    // Circuit breaker implementation
    private static class CircuitBreakerFunction<T, R> implements Function<T, Result<R, String>> {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private final Function<T, R> function;
        private final int failureThreshold;
        private final Duration timeout;
        
        private volatile State state = State.CLOSED;
        private volatile int failureCount = 0;
        private volatile Instant lastFailureTime = Instant.MIN;
        
        CircuitBreakerFunction(Function<T, R> function, int failureThreshold, Duration timeout) {
            this.function = function;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }
        
        @Override
        public Result<R, String> apply(T input) {
            if (state == State.OPEN) {
                if (Instant.now().isAfter(lastFailureTime.plus(timeout))) {
                    state = State.HALF_OPEN;
                } else {
                    return Result.failure("Circuit breaker is open");
                }
            }
            
            try {
                R result = function.apply(input);
                onSuccess();
                return Result.success(result);
            } catch (Exception e) {
                onFailure();
                return Result.failure(e.getMessage());
            }
        }
        
        private void onSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }
        
        private void onFailure() {
            failureCount++;
            lastFailureTime = Instant.now();
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
            }
        }
    }
}