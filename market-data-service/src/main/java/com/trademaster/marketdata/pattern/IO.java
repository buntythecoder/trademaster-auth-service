package com.trademaster.marketdata.pattern;

import com.trademaster.common.functional.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * IO monad for managing side effects in functional programming
 * Represents a computation that may perform side effects
 */
public final class IO<T> {
    
    private final Supplier<T> computation;
    
    private IO(Supplier<T> computation) {
        this.computation = computation;
    }
    
    // Factory methods
    public static <T> IO<T> of(Supplier<T> computation) {
        return new IO<>(computation);
    }
    
    public static <T> IO<T> pure(T value) {
        return new IO<>(() -> value);
    }
    
    public static <T> IO<T> delay(Supplier<T> computation) {
        return new IO<>(computation);
    }
    
    // Execute the computation
    public T unsafeRun() {
        return computation.get();
    }
    
    public CompletableFuture<T> runAsync() {
        return CompletableFuture.supplyAsync(computation);
    }
    
    public CompletableFuture<T> runAsync(Executor executor) {
        return CompletableFuture.supplyAsync(computation, executor);
    }
    
    // Safe execution with Result
    public Result<T, String> runSafely() {
        return Result.safely(() -> computation.get(), Exception::getMessage);
    }
    
    // Monadic operations
    public <U> IO<U> map(Function<T, U> mapper) {
        return new IO<>(() -> mapper.apply(computation.get()));
    }
    
    public <U> IO<U> flatMap(Function<T, IO<U>> mapper) {
        return new IO<>(() -> mapper.apply(computation.get()).unsafeRun());
    }
    
    // Combining IOs
    public <U, V> IO<V> zipWith(IO<U> other, java.util.function.BiFunction<T, U, V> combiner) {
        return new IO<>(() -> combiner.apply(
            this.computation.get(),
            other.computation.get()
        ));
    }
    
    public <U> IO<java.util.List<Object>> zip(IO<U> other) {
        return zipWith(other, java.util.List::of);
    }
    
    // Error handling
    public IO<T> handleError(Function<Exception, T> handler) {
        return new IO<>(() -> {
            try {
                return computation.get();
            } catch (Exception e) {
                return handler.apply(e);
            }
        });
    }
    
    public IO<T> orElse(IO<T> fallback) {
        return new IO<>(() -> {
            try {
                return computation.get();
            } catch (Exception e) {
                return fallback.unsafeRun();
            }
        });
    }
    
    // Side effects
    public IO<T> peek(java.util.function.Consumer<T> action) {
        return new IO<>(() -> {
            T result = computation.get();
            action.accept(result);
            return result;
        });
    }
    
    // Conditional execution
    public IO<T> when(java.util.function.Predicate<T> condition, 
                      java.util.function.Consumer<T> action) {
        return new IO<>(() -> {
            T result = computation.get();
            if (condition.test(result)) {
                action.accept(result);
            }
            return result;
        });
    }
    
    // Retry logic
    public IO<T> retry(int maxAttempts) {
        return new IO<>(() -> {
            Exception lastException = null;
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    return computation.get();
                } catch (Exception e) {
                    lastException = e;
                    if (i == maxAttempts - 1) {
                        throw new RuntimeException("Retry failed after " + maxAttempts + " attempts", e);
                    }
                }
            }
            throw new RuntimeException("Unexpected retry failure", lastException);
        });
    }
    
    // Memoization
    public IO<T> memoize() {
        java.util.function.Function<Object, T> memoizedFunction = 
            Memoization.memoize(ignored -> computation.get());
        return new IO<>(() -> memoizedFunction.apply(null));
    }
    
    // Timeout
    public IO<T> timeout(java.time.Duration duration) {
        return new IO<>(() -> {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(computation);
            try {
                return future.get(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Operation timed out after " + duration, e);
            }
        });
    }
    
    // Parallel execution
    public static <T, U> IO<java.util.List<Object>> parallel(IO<T> io1, IO<U> io2) {
        return new IO<>(() -> {
            CompletableFuture<T> future1 = io1.runAsync();
            CompletableFuture<U> future2 = io2.runAsync();
            
            try {
                return java.util.List.of(future1.get(), future2.get());
            } catch (Exception e) {
                throw new RuntimeException("Parallel execution failed", e);
            }
        });
    }
    
    @SafeVarargs
    public static <T> IO<java.util.List<T>> sequence(IO<T>... ios) {
        return new IO<>(() -> {
            java.util.List<T> results = new java.util.ArrayList<>();
            for (IO<T> io : ios) {
                results.add(io.unsafeRun());
            }
            return results;
        });
    }
    
    @Override
    public String toString() {
        return "IO{computation}";
    }
}