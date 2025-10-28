package com.trademaster.auth.service;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.pattern.SafeOperations;
import com.trademaster.auth.pattern.VirtualThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Authentication Event Publisher
 *
 * Central service for publishing and managing authentication events.
 * Implements Observer Pattern with functional programming and virtual threads.
 *
 * Features:
 * - Thread-safe observer management with ConcurrentHashMap
 * - Virtual threads for scalable async event notification
 * - Type-safe event subscriptions using sealed types
 * - Functional composition for observer chains
 * - Comprehensive audit logging and metrics
 *
 * Design Patterns:
 * - Observer Pattern (functional)
 * - Publish-Subscribe Pattern
 * - Strategy Pattern (for event routing)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class AuthEventPublisher {

    private final Map<Class<? extends AuthEvent>, Set<EventObserver<? extends AuthEvent>>> observers =
        new ConcurrentHashMap<>();

    /**
     * Subscribe to specific event type
     *
     * @param eventType Event class to observe
     * @param observer Event observer to register
     */
    public <T extends AuthEvent> void subscribe(
            Class<T> eventType,
            EventObserver<T> observer) {

        SafeOperations.safelyToResult(() -> {
            observers.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet())
                .add(observer);
            log.debug("Observer registered for event type: {}", eventType.getSimpleName());
            return null; // Void operation
        }).fold(
            error -> {
                log.error("Failed to register observer for event type: {}", eventType.getSimpleName(), error);
                return null;
            },
            success -> success
        );
    }

    /**
     * Unsubscribe from specific event type
     *
     * @param eventType Event class to stop observing
     * @param observer Event observer to unregister
     */
    public <T extends AuthEvent> void unsubscribe(
            Class<T> eventType,
            EventObserver<T> observer) {

        SafeOperations.safelyToResult(() -> {
            observers.getOrDefault(eventType, ConcurrentHashMap.newKeySet())
                .remove(observer);
            log.debug("Observer unregistered for event type: {}", eventType.getSimpleName());
            return null; // Void operation
        }).fold(
            error -> {
                log.error("Failed to unregister observer for event type: {}", eventType.getSimpleName(), error);
                return null;
            },
            success -> success
        );
    }

    /**
     * Publish authentication event to all registered observers
     *
     * @param event Authentication event to publish
     * @return CompletableFuture with results from all observers
     */
    public <T extends AuthEvent> CompletableFuture<List<Result<Void, String>>> publish(T event) {
        return VirtualThreadFactory.INSTANCE.supplyAsync(() ->
            SafeOperations.safelyToResult(() -> {
                log.debug("Publishing event: type={}, eventId={}, userId={}, correlation={}",
                    event.getClass().getSimpleName(),
                    event.eventId(),
                    event.userId(),
                    event.correlationId());

                Set<EventObserver<? extends AuthEvent>> eventObservers =
                    observers.getOrDefault(event.getClass(), ConcurrentHashMap.newKeySet());

                return notifyObservers(event, eventObservers);
            }).fold(
                error -> {
                    log.error("Event publication failed: eventId={}, error={}",
                        event.eventId(), error);
                    return List.of(Result.failure("Event publication failed: " + error));
                },
                results -> results
            )
        );
    }

    /**
     * Notify all observers of an event in parallel using virtual threads
     */
    @SuppressWarnings("unchecked")
    private <T extends AuthEvent> List<Result<Void, String>> notifyObservers(
            T event,
            Set<EventObserver<? extends AuthEvent>> eventObservers) {

        List<CompletableFuture<Result<Void, String>>> futures = eventObservers.stream()
            .map(observer -> ((EventObserver<T>) observer).onEvent(event))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .join();
    }

    /**
     * Publish login event
     */
    public CompletableFuture<List<Result<Void, String>>> publishLogin(LoginEvent event) {
        return publish(event);
    }

    /**
     * Publish logout event
     */
    public CompletableFuture<List<Result<Void, String>>> publishLogout(LogoutEvent event) {
        return publish(event);
    }

    /**
     * Publish password change event
     */
    public CompletableFuture<List<Result<Void, String>>> publishPasswordChange(PasswordChangeEvent event) {
        return publish(event);
    }

    /**
     * Publish MFA event
     */
    public CompletableFuture<List<Result<Void, String>>> publishMfa(MfaEvent event) {
        return publish(event);
    }

    /**
     * Publish verification event
     */
    public CompletableFuture<List<Result<Void, String>>> publishVerification(VerificationEvent event) {
        return publish(event);
    }

    /**
     * Get count of observers for specific event type
     */
    public int getObserverCount(Class<? extends AuthEvent> eventType) {
        return observers.getOrDefault(eventType, ConcurrentHashMap.newKeySet()).size();
    }

    /**
     * Get total number of registered observers
     */
    public int getTotalObserverCount() {
        return observers.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Clear all observers (for testing purposes)
     */
    public void clearAllObservers() {
        SafeOperations.safelyToResult(() -> {
            observers.clear();
            log.info("All observers cleared");
            return null; // Void operation
        }).fold(
            error -> {
                log.error("Failed to clear observers", error);
                return null;
            },
            success -> success
        );
    }
}
