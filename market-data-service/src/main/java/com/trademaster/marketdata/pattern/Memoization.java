package com.trademaster.marketdata.pattern;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Advanced memoization patterns for caching expensive computations
 * Provides time-based expiration and multi-parameter memoization
 */
public final class Memoization {
    
    private Memoization() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Basic memoization
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
    
    // BiFunction memoization
    public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> function) {
        return new BiFunction<T, U, R>() {
            private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T t, U u) {
                return cache.computeIfAbsent(Pair.of(t, u), 
                    pair -> function.apply(pair.first(), pair.second()));
            }
        };
    }
    
    // Size-limited memoization (LRU)
    public static <T, R> Function<T, R> memoizeWithSizeLimit(
            Function<T, R> function, int maxSize) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                if (cache.size() >= maxSize && !cache.containsKey(input)) {
                    // Simple eviction - remove oldest (first) entry
                    cache.entrySet().iterator().remove();
                }
                return cache.computeIfAbsent(input, function);
            }
        };
    }
    
    // Weak reference memoization for memory efficiency
    public static <T, R> Function<T, R> memoizeWeak(Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, java.lang.ref.WeakReference<R>> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                java.lang.ref.WeakReference<R> ref = cache.get(input);
                R cached = ref != null ? ref.get() : null;
                
                if (cached != null) {
                    return cached;
                }
                
                R result = function.apply(input);
                cache.put(input, new java.lang.ref.WeakReference<>(result));
                return result;
            }
        };
    }
    
    // Conditional memoization
    public static <T, R> Function<T, R> memoizeIf(
            Function<T, R> function, 
            java.util.function.Predicate<T> shouldCache) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                if (!shouldCache.test(input)) {
                    return function.apply(input);
                }
                return cache.computeIfAbsent(input, function);
            }
        };
    }
    
    // Async memoization
    public static <T, R> Function<T, java.util.concurrent.CompletableFuture<R>> memoizeAsync(
            Function<T, java.util.concurrent.CompletableFuture<R>> function) {
        return new Function<T, java.util.concurrent.CompletableFuture<R>>() {
            private final Map<T, java.util.concurrent.CompletableFuture<R>> cache = new ConcurrentHashMap<>();
            
            @Override
            public java.util.concurrent.CompletableFuture<R> apply(T input) {
                return cache.computeIfAbsent(input, function);
            }
        };
    }
    
    // Supporting classes
    private record TimestampedValue<T>(T value, Instant timestamp) {
        boolean isValid(Duration expiration) {
            return timestamp.plus(expiration).isAfter(Instant.now());
        }
    }
    
    private record Pair<T, U>(T first, U second) {
        static <T, U> Pair<T, U> of(T first, U second) {
            return new Pair<>(first, second);
        }
    }
    
    // Triple for 3-parameter functions
    public static <T, U, V, R> Function<Triple<T, U, V>, R> memoizeTriple(
            Function<Triple<T, U, V>, R> function) {
        Map<Triple<T, U, V>, R> cache = new ConcurrentHashMap<>();
        return input -> cache.computeIfAbsent(input, function);
    }
    
    public record Triple<T, U, V>(T first, U second, V third) {
        public static <T, U, V> Triple<T, U, V> of(T first, U second, V third) {
            return new Triple<>(first, second, third);
        }
    }
    
    // Functional interface for tri-function
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
        
        default Function<Triple<T, U, V>, R> tupledAndMemoized() {
            return memoizeTriple(triple -> apply(triple.first(), triple.second(), triple.third()));
        }
    }
    
    // Utility to create memoized tri-function
    public static <T, U, V, R> TriFunction<T, U, V, R> memoize(TriFunction<T, U, V, R> function) {
        Function<Triple<T, U, V>, R> memoized = function.tupledAndMemoized();
        return (t, u, v) -> memoized.apply(Triple.of(t, u, v));
    }
}