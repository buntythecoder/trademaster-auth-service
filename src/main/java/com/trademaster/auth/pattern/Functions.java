package com.trademaster.auth.pattern;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Function Composition Utilities
 * 
 * Provides functional composition patterns to replace imperative code
 * following TradeMaster Advanced Design Patterns standards.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public final class Functions {

    private Functions() {}
    
    /**
     * Compose multiple functions left to right
     */
    @SafeVarargs
    public static <T> Function<T, T> compose(Function<T, T>... functions) {
        return Arrays.stream(functions)
            .reduce(Function.identity(), Function::andThen);
    }
    
    /**
     * Compose multiple functions right to left
     */
    @SafeVarargs
    public static <T> Function<T, T> pipe(Function<T, T>... functions) {
        return Arrays.stream(functions)
            .reduce(Function.identity(), Function::compose);
    }
    
    /**
     * AND composition for predicates
     */
    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(Predicate::and)
            .orElse(t -> true);
    }
    
    /**
     * OR composition for predicates
     */
    @SafeVarargs
    public static <T> Predicate<T> or(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(Predicate::or)
            .orElse(t -> false);
    }
    
    /**
     * Conditional function application
     */
    public static <T> Function<T, T> when(Predicate<T> condition, Function<T, T> transformer) {
        return input -> condition.test(input) ? transformer.apply(input) : input;
    }
    
    /**
     * Unless condition (opposite of when)
     */
    public static <T> Function<T, T> unless(Predicate<T> condition, Function<T, T> transformer) {
        return when(condition.negate(), transformer);
    }
    
    /**
     * Constant function
     */
    public static <T, R> Function<T, R> constant(R value) {
        return input -> value;
    }
    
    /**
     * Identity function with type cast
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, R> cast() {
        return input -> (R) input;
    }
    
    /**
     * Function that throws exception
     */
    public static <T, R> Function<T, R> throwing(String message) {
        return input -> {
            throw new RuntimeException(message);
        };
    }
    
    /**
     * Try function with fallback
     */
    public static <T, R> Function<T, R> tryWith(Function<T, R> function, Function<T, R> fallback) {
        return input -> {
            try {
                return function.apply(input);
            } catch (Exception e) {
                return fallback.apply(input);
            }
        };
    }
    
    /**
     * Memoization wrapper
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        return new Function<T, R>() {
            private final java.util.Map<T, R> cache = new java.util.concurrent.ConcurrentHashMap<>();
            
            @Override
            public R apply(T input) {
                return cache.computeIfAbsent(input, function);
            }
        };
    }
}