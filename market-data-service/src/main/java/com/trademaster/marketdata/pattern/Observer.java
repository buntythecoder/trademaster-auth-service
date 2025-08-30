package com.trademaster.marketdata.pattern;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Functional Observer Pattern Implementation
 * Provides type-safe event handling with functional composition
 */
public final class Observer {
    
    private Observer() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Observer interface for event handling
     */
    @FunctionalInterface
    public interface EventObserver<T> {
        void onEvent(T event);
        
        // Functional composition
        default EventObserver<T> andThen(EventObserver<T> after) {
            return event -> {
                this.onEvent(event);
                after.onEvent(event);
            };
        }
        
        default EventObserver<T> compose(EventObserver<T> before) {
            return event -> {
                before.onEvent(event);
                this.onEvent(event);
            };
        }
    }
    
    /**
     * Event subject that notifies observers
     */
    public static class EventSubject<T> {
        private final CopyOnWriteArrayList<EventObserver<T>> observers = new CopyOnWriteArrayList<>();
        
        public void subscribe(EventObserver<T> observer) {
            observers.add(observer);
        }
        
        public void unsubscribe(EventObserver<T> observer) {
            observers.remove(observer);
        }
        
        public void notify(T event) {
            observers.forEach(observer -> observer.onEvent(event));
        }
        
        public void notifyAsync(T event) {
            observers.parallelStream().forEach(observer -> observer.onEvent(event));
        }
        
        public int getObserverCount() {
            return observers.size();
        }
        
        public void clear() {
            observers.clear();
        }
    }
    
    /**
     * Typed event system for different event types
     */
    public static class TypedEventBus {
        private final Map<Class<?>, EventSubject<Object>> subjects = new ConcurrentHashMap<>();
        
        @SuppressWarnings("unchecked")
        public <T> void subscribe(Class<T> eventType, EventObserver<T> observer) {
            subjects.computeIfAbsent(eventType, k -> new EventSubject<>())
                   .subscribe((EventObserver<Object>) observer);
        }
        
        @SuppressWarnings("unchecked")
        public <T> void unsubscribe(Class<T> eventType, EventObserver<T> observer) {
            EventSubject<Object> subject = subjects.get(eventType);
            if (subject != null) {
                subject.unsubscribe((EventObserver<Object>) observer);
            }
        }
        
        @SuppressWarnings("unchecked")
        public <T> void publish(T event) {
            EventSubject<Object> subject = subjects.get(event.getClass());
            if (subject != null) {
                subject.notify(event);
            }
        }
        
        @SuppressWarnings("unchecked")
        public <T> void publishAsync(T event) {
            EventSubject<Object> subject = subjects.get(event.getClass());
            if (subject != null) {
                subject.notifyAsync(event);
            }
        }
    }
    
    /**
     * Alert events for the notification system
     */
    public sealed interface AlertEvent permits AlertEvent.AlertTriggered, AlertEvent.AlertCreated, AlertEvent.AlertDeleted {
        
        record AlertTriggered(
            String alertId,
            String symbol,
            String condition,
            String userId,
            Instant triggeredAt,
            Map<String, Object> context
        ) implements AlertEvent {}
        
        record AlertCreated(
            String alertId,
            String symbol,
            String userId,
            Instant createdAt
        ) implements AlertEvent {}
        
        record AlertDeleted(
            String alertId,
            String userId,
            Instant deletedAt
        ) implements AlertEvent {}
    }
    
    /**
     * Factory methods for creating observers with error handling
     */
    public static <T> EventObserver<T> safeObserver(Consumer<T> handler) {
        return event -> {
            try {
                handler.accept(event);
            } catch (Exception e) {
                // Log error but don't propagate to prevent other observers from failing
                System.err.println("Observer error: " + e.getMessage());
            }
        };
    }
    
    public static <T> EventObserver<T> loggingObserver(Consumer<T> handler, String name) {
        return event -> {
            System.out.println("Observer [" + name + "] processing: " + event);
            handler.accept(event);
        };
    }
    
    public static <T> EventObserver<T> conditionalObserver(
            java.util.function.Predicate<T> condition, 
            Consumer<T> handler) {
        return event -> {
            if (condition.test(event)) {
                handler.accept(event);
            }
        };
    }
    
    /**
     * Functional event filtering and transformation
     */
    public static <T, R> EventObserver<T> transformingObserver(
            java.util.function.Function<T, R> transformer,
            Consumer<R> handler) {
        return event -> handler.accept(transformer.apply(event));
    }
    
    public static <T> EventObserver<T> batchingObserver(
            Consumer<java.util.List<T>> batchHandler, 
            int batchSize, 
            java.time.Duration timeout) {
        // Implementation would require more complex state management
        // This is a simplified version for demonstration
        return event -> batchHandler.accept(java.util.List.of(event));
    }
}