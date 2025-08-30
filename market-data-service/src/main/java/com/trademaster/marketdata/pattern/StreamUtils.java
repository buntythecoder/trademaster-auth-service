package com.trademaster.marketdata.pattern;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Functional programming utilities for streams
 * Eliminates imperative loops and promotes functional transformation
 */
public final class StreamUtils {
    
    private StreamUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Functional grouping and partitioning
    public static <T, K> Map<K, List<T>> groupByFunction(Stream<T> stream, Function<T, K> classifier) {
        return stream.collect(Collectors.groupingBy(classifier));
    }
    
    public static <T> Map<Boolean, List<T>> partitionByPredicate(Stream<T> stream, Predicate<T> predicate) {
        return stream.collect(Collectors.partitioningBy(predicate));
    }
    
    // Safe operations
    public static <T> Optional<T> findFirstSafely(Stream<T> stream, Predicate<T> predicate) {
        return stream.filter(predicate).findFirst();
    }
    
    public static <T> List<T> filterAndCollect(Stream<T> stream, Predicate<T> predicate) {
        return stream.filter(predicate).collect(Collectors.toList());
    }
    
    // Transformation chains
    public static <T, R> List<R> mapAndCollect(Stream<T> stream, Function<T, R> mapper) {
        return stream.map(mapper).collect(Collectors.toList());
    }
    
    public static <T, R> List<R> flatMapAndCollect(Stream<T> stream, Function<T, Stream<R>> mapper) {
        return stream.flatMap(mapper).collect(Collectors.toList());
    }
    
    // Functional reduction
    public static <T> Optional<T> reduceOptional(Stream<T> stream, BinaryOperator<T> accumulator) {
        return stream.reduce(accumulator);
    }
    
    public static <T, U> U foldLeft(Stream<T> stream, U identity, BiFunction<U, T, U> accumulator) {
        return stream.reduce(identity, accumulator, (u1, u2) -> u2);
    }
    
    // Side effects in functional style
    public static <T> Stream<T> peekWithAction(Stream<T> stream, Consumer<T> action) {
        return stream.peek(action);
    }
    
    public static <T> List<T> processAndCollect(Stream<T> stream, Consumer<T> processor) {
        return stream.peek(processor).collect(Collectors.toList());
    }
    
    // Conditional operations
    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<T> predicate) {
        return stream.takeWhile(predicate);
    }
    
    public static <T> Stream<T> dropWhile(Stream<T> stream, Predicate<T> predicate) {
        return stream.dropWhile(predicate);
    }
    
    // Functional validation
    public static <T> boolean allMatch(Stream<T> stream, Predicate<T> predicate) {
        return stream.allMatch(predicate);
    }
    
    public static <T> boolean anyMatch(Stream<T> stream, Predicate<T> predicate) {
        return stream.anyMatch(predicate);
    }
    
    public static <T> boolean noneMatch(Stream<T> stream, Predicate<T> predicate) {
        return stream.noneMatch(predicate);
    }
    
    // Functional statistics
    public static <T> long countElements(Stream<T> stream) {
        return stream.count();
    }
    
    public static <T extends Comparable<T>> Optional<T> findMax(Stream<T> stream) {
        return stream.max(Comparator.naturalOrder());
    }
    
    public static <T extends Comparable<T>> Optional<T> findMin(Stream<T> stream) {
        return stream.min(Comparator.naturalOrder());
    }
    
    // Custom collectors
    public static <T> Collector<T, ?, Optional<T>> findFirst() {
        return Collector.of(
            () -> new ArrayList<T>(),
            (list, item) -> { if (list.isEmpty()) list.add(item); },
            (list1, list2) -> list1.isEmpty() ? list2 : list1,
            list -> list.isEmpty() ? Optional.<T>empty() : Optional.of(list.get(0))
        );
    }
    
    public static <T> Collector<T, ?, List<T>> toImmutableList() {
        return Collector.<T, ArrayList<T>, List<T>>of(
            ArrayList::new,
            ArrayList::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            Collections::unmodifiableList
        );
    }
    
    // Functional error handling
    public static <T, R> List<Result<R, String>> mapSafely(Stream<T> stream, Function<T, R> mapper) {
        return stream.map(item -> Result.safely(() -> mapper.apply(item)))
                    .collect(Collectors.toList());
    }
    
    public static <T, R> List<R> mapSafelyAndExtract(Stream<T> stream, Function<T, R> mapper) {
        return stream.map(item -> Result.safely(() -> mapper.apply(item)))
                    .filter(Result::isSuccess)
                    .map(result -> result.fold(error -> null, success -> success))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }
    
    // Functional filtering with validation
    public static <T> Stream<T> filterValid(Stream<T> stream, Function<T, Boolean> validator) {
        return stream.filter(validator::apply);
    }
    
    public static <T> List<T> validateAndCollect(Stream<T> stream, Predicate<T> validator) {
        return stream.filter(validator).collect(toImmutableList());
    }
    
    // Functional distinct and sorting
    public static <T> Stream<T> distinctBy(Stream<T> stream, Function<T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return stream.filter(item -> seen.add(keyExtractor.apply(item)));
    }
    
    public static <T, U extends Comparable<U>> Stream<T> sortedBy(Stream<T> stream, Function<T, U> keyExtractor) {
        return stream.sorted(Comparator.comparing(keyExtractor));
    }
    
    // Functional chunking
    public static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
        Iterator<T> iterator = stream.iterator();
        List<List<T>> chunks = new ArrayList<>();
        
        while (iterator.hasNext()) {
            List<T> chunk = new ArrayList<>();
            for (int i = 0; i < size && iterator.hasNext(); i++) {
                chunk.add(iterator.next());
            }
            chunks.add(chunk);
        }
        
        return chunks.stream();
    }
    
    // Functional zipping
    public static <T, U, R> Stream<R> zip(Stream<T> stream1, Stream<U> stream2, BiFunction<T, U, R> combiner) {
        Iterator<T> iter1 = stream1.iterator();
        Iterator<U> iter2 = stream2.iterator();
        List<R> results = new ArrayList<>();
        
        while (iter1.hasNext() && iter2.hasNext()) {
            results.add(combiner.apply(iter1.next(), iter2.next()));
        }
        
        return results.stream();
    }
    
    // Functional memoized operations
    public static <T, R> Function<Stream<T>, List<R>> memoizedMap(Function<T, R> mapper) {
        Function<T, R> memoizedMapper = Memoization.memoize(mapper);
        return stream -> stream.map(memoizedMapper).collect(Collectors.toList());
    }
}