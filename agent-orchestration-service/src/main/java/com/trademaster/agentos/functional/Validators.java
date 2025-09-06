package com.trademaster.agentos.functional;

import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for common validation predicates
 */
public final class Validators {
    
    private Validators() {}
    
    // String validations
    public static final Predicate<String> NOT_BLANK = s -> s != null && !s.trim().isEmpty();
    public static final Predicate<String> NOT_EMPTY = s -> s != null && !s.isEmpty();
    
    // Numeric validations
    public static final Predicate<Integer> POSITIVE = i -> i != null && i > 0;
    public static final Predicate<Integer> NON_NEGATIVE = i -> i != null && i >= 0;
    public static final Predicate<Long> POSITIVE_LONG = l -> l != null && l > 0;
    public static final Predicate<Double> POSITIVE_DOUBLE = d -> d != null && d > 0.0;
    
    // Collection validations
    public static final Predicate<List<?>> NOT_EMPTY_LIST = l -> l != null && !l.isEmpty();
    
    // Custom validation factories
    public static <T extends Comparable<T>> Predicate<T> greaterThan(T value) {
        return t -> t != null && t.compareTo(value) > 0;
    }
    
    public static <T extends Comparable<T>> Predicate<T> lessThan(T value) {
        return t -> t != null && t.compareTo(value) < 0;
    }
    
    public static <T extends Comparable<T>> Predicate<T> between(T min, T max) {
        return t -> t != null && t.compareTo(min) >= 0 && t.compareTo(max) <= 0;
    }
    
    public static Predicate<String> minLength(int length) {
        return s -> s != null && s.length() >= length;
    }
    
    public static Predicate<String> maxLength(int length) {
        return s -> s != null && s.length() <= length;
    }
    
    public static Predicate<String> exactLength(int length) {
        return s -> s != null && s.length() == length;
    }
    
    public static Predicate<String> matches(String regex) {
        return s -> s != null && s.matches(regex);
    }
}