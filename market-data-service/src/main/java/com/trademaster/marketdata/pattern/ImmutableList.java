package com.trademaster.marketdata.pattern;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Immutable List implementation for functional programming
 * Ensures data immutability and promotes functional transformations
 */
public final class ImmutableList<T> implements Iterable<T> {
    
    private final List<T> items;
    
    private ImmutableList(List<T> items) {
        this.items = List.copyOf(items);
    }
    
    // Factory methods
    public static <T> ImmutableList<T> empty() {
        return new ImmutableList<>(List.of());
    }
    
    public static <T> ImmutableList<T> of(T... items) {
        return new ImmutableList<>(List.of(items));
    }
    
    public static <T> ImmutableList<T> from(Collection<T> collection) {
        return new ImmutableList<>(new ArrayList<>(collection));
    }
    
    public static <T> ImmutableList<T> from(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return new ImmutableList<>(list);
    }
    
    // Basic operations
    public int size() {
        return items.size();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public T get(int index) {
        return items.get(index);
    }
    
    public T head() {
        if (items.isEmpty()) {
            throw new NoSuchElementException("Empty list has no head");
        }
        return items.get(0);
    }
    
    public ImmutableList<T> tail() {
        if (items.isEmpty()) {
            return empty();
        }
        return new ImmutableList<>(items.subList(1, items.size()));
    }
    
    public Optional<T> headOption() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }
    
    // Functional operations
    public <R> ImmutableList<R> map(Function<T, R> mapper) {
        return from(items.stream().map(mapper).collect(Collectors.toList()));
    }
    
    public <R> ImmutableList<R> flatMap(Function<T, ImmutableList<R>> mapper) {
        return from(items.stream()
            .flatMap(item -> mapper.apply(item).stream())
            .collect(Collectors.toList()));
    }
    
    public ImmutableList<T> filter(Predicate<T> predicate) {
        return from(items.stream()
            .filter(predicate)
            .collect(Collectors.toList()));
    }
    
    public ImmutableList<T> filterNot(Predicate<T> predicate) {
        return filter(predicate.negate());
    }
    
    // Reduction operations
    public Optional<T> reduce(java.util.function.BinaryOperator<T> accumulator) {
        return items.stream().reduce(accumulator);
    }
    
    public <R> R fold(R identity, java.util.function.BiFunction<R, T, R> accumulator) {
        return items.stream().reduce(identity, accumulator, (r1, r2) -> r2);
    }
    
    public Optional<T> find(Predicate<T> predicate) {
        return items.stream().filter(predicate).findFirst();
    }
    
    public boolean exists(Predicate<T> predicate) {
        return items.stream().anyMatch(predicate);
    }
    
    public boolean forAll(Predicate<T> predicate) {
        return items.stream().allMatch(predicate);
    }
    
    // List operations
    public ImmutableList<T> append(T item) {
        List<T> newList = new ArrayList<>(items);
        newList.add(item);
        return new ImmutableList<>(newList);
    }
    
    public ImmutableList<T> prepend(T item) {
        List<T> newList = new ArrayList<>();
        newList.add(item);
        newList.addAll(items);
        return new ImmutableList<>(newList);
    }
    
    public ImmutableList<T> concat(ImmutableList<T> other) {
        List<T> newList = new ArrayList<>(items);
        newList.addAll(other.items);
        return new ImmutableList<>(newList);
    }
    
    public ImmutableList<T> take(int n) {
        if (n <= 0) return empty();
        if (n >= items.size()) return this;
        return new ImmutableList<>(items.subList(0, n));
    }
    
    public ImmutableList<T> drop(int n) {
        if (n <= 0) return this;
        if (n >= items.size()) return empty();
        return new ImmutableList<>(items.subList(n, items.size()));
    }
    
    public ImmutableList<T> takeWhile(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (predicate.test(item)) {
                result.add(item);
            } else {
                break;
            }
        }
        return new ImmutableList<>(result);
    }
    
    public ImmutableList<T> dropWhile(Predicate<T> predicate) {
        List<T> result = new ArrayList<>(items);
        while (!result.isEmpty() && predicate.test(result.get(0))) {
            result.remove(0);
        }
        return new ImmutableList<>(result);
    }
    
    // Partitioning
    public ImmutableList<ImmutableList<T>> partition(Predicate<T> predicate) {
        Map<Boolean, List<T>> partitioned = items.stream()
            .collect(Collectors.partitioningBy(predicate));
        return ImmutableList.of(
            from(partitioned.get(true)),
            from(partitioned.get(false))
        );
    }
    
    public ImmutableList<ImmutableList<T>> groupBy(Function<T, ?> keyMapper) {
        return from(items.stream()
            .collect(Collectors.groupingBy(keyMapper))
            .values()
            .stream()
            .map(ImmutableList::from)
            .collect(Collectors.toList()));
    }
    
    // Sorting
    public ImmutableList<T> sorted() {
        return from(items.stream()
            .sorted()
            .collect(Collectors.toList()));
    }
    
    public ImmutableList<T> sortedBy(Comparator<T> comparator) {
        return from(items.stream()
            .sorted(comparator)
            .collect(Collectors.toList()));
    }
    
    public <R extends Comparable<R>> ImmutableList<T> sortedBy(Function<T, R> keyExtractor) {
        return sortedBy(Comparator.comparing(keyExtractor));
    }
    
    // Conversion
    public List<T> toList() {
        return List.copyOf(items);
    }
    
    public Set<T> toSet() {
        return Set.copyOf(items);
    }
    
    public Stream<T> stream() {
        return items.stream();
    }
    
    public Stream<T> parallelStream() {
        return items.parallelStream();
    }
    
    // Iterator
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
    
    // Safe indexing
    public Optional<T> safeGet(int index) {
        return index >= 0 && index < items.size() 
            ? Optional.of(items.get(index))
            : Optional.empty();
    }
    
    // Statistical operations (for numeric types)
    public Optional<T> max() {
        return items.stream().max((Comparator<? super T>) Comparator.naturalOrder());
    }
    
    public Optional<T> min() {
        return items.stream().min((Comparator<? super T>) Comparator.naturalOrder());
    }
    
    // Equality and hashing
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImmutableList<?> that = (ImmutableList<?>) obj;
        return Objects.equals(items, that.items);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
    
    @Override
    public String toString() {
        return "ImmutableList" + items.toString();
    }
}