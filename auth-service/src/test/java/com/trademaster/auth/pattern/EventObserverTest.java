package com.trademaster.auth.pattern;

import com.trademaster.auth.event.AuthEvent;
import com.trademaster.auth.event.LoginEvent;
import com.trademaster.auth.event.LogoutEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for EventObserver functional interface
 *
 * Tests cover:
 * - Observer creation with factory method
 * - Event filtering
 * - Observer composition (andThen)
 * - Error handling
 * - Virtual thread execution
 */
@DisplayName("EventObserver Functional Interface Tests")
class EventObserverTest {

    @Test
    @DisplayName("Should create observer with factory method")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testObserverFactoryMethod() {
        // Given
        AtomicReference<AuthEvent> capturedEvent = new AtomicReference<>();
        EventObserver<AuthEvent> observer = EventObserver.of(capturedEvent::set);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        Result<Void, String> result = observer.onEvent(event).join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(capturedEvent.get()).isEqualTo(event);
    }

    @Test
    @DisplayName("Should handle observer execution failure")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testObserverExecutionFailure() {
        // Given
        EventObserver<AuthEvent> observer = EventObserver.of(event -> {
            throw new RuntimeException("Observer failed");
        });

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        Result<Void, String> result = observer.onEvent(event).join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Observer execution failed");
    }

    @Test
    @DisplayName("Should filter events using predicate")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testEventFiltering() {
        // Given
        AtomicInteger processedCount = new AtomicInteger(0);
        EventObserver<LoginEvent> observer = EventObserver.<LoginEvent>of(event ->
            processedCount.incrementAndGet()
        ).filter(event -> event.mfaUsed());

        LoginEvent mfaEvent = LoginEvent.withMfa(1L, "session-1", "fp", "ip", true);
        LoginEvent noMfaEvent = LoginEvent.withMfa(2L, "session-2", "fp", "ip", false);

        // When
        observer.onEvent(mfaEvent).join();
        observer.onEvent(noMfaEvent).join();

        // Then
        assertThat(processedCount.get()).isEqualTo(1); // Only MFA event processed
    }

    @Test
    @DisplayName("Should compose observers using andThen")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testObserverComposition() {
        // Given
        AtomicInteger firstCount = new AtomicInteger(0);
        AtomicInteger secondCount = new AtomicInteger(0);

        EventObserver<AuthEvent> first = EventObserver.of(event -> firstCount.incrementAndGet());
        EventObserver<AuthEvent> second = EventObserver.of(event -> secondCount.incrementAndGet());

        EventObserver<AuthEvent> composed = first.andThen(second);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        Result<Void, String> result = composed.onEvent(event).join();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(firstCount.get()).isEqualTo(1);
        assertThat(secondCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute composed observers in order")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testComposedObserverExecutionOrder() {
        // Given
        AtomicReference<String> executionOrder = new AtomicReference<>("");

        EventObserver<AuthEvent> first = EventObserver.of(event ->
            executionOrder.updateAndGet(order -> order + "first-")
        );
        EventObserver<AuthEvent> second = EventObserver.of(event ->
            executionOrder.updateAndGet(order -> order + "second-")
        );
        EventObserver<AuthEvent> third = EventObserver.of(event ->
            executionOrder.updateAndGet(order -> order + "third")
        );

        EventObserver<AuthEvent> composed = first.andThen(second).andThen(third);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        composed.onEvent(event).join();

        // Then
        assertThat(executionOrder.get()).isEqualTo("first-second-third");
    }

    @Test
    @DisplayName("Should handle failure in composed observer chain")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testComposedObserverFailure() {
        // Given
        AtomicInteger firstCount = new AtomicInteger(0);
        AtomicInteger secondCount = new AtomicInteger(0);

        EventObserver<AuthEvent> first = EventObserver.of(event -> firstCount.incrementAndGet());
        EventObserver<AuthEvent> failing = EventObserver.of(event -> {
            throw new RuntimeException("Second observer failed");
        });
        EventObserver<AuthEvent> third = EventObserver.of(event -> secondCount.incrementAndGet());

        EventObserver<AuthEvent> composed = first.andThen(failing).andThen(third);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        Result<Void, String> result = composed.onEvent(event).join();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(firstCount.get()).isEqualTo(1); // First observer executed
        assertThat(secondCount.get()).isZero(); // Third observer not executed due to failure
    }

    @Test
    @DisplayName("Should combine filter and andThen composition")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testFilterAndComposition() {
        // Given
        AtomicInteger count = new AtomicInteger(0);

        EventObserver<LoginEvent> observer = EventObserver.<LoginEvent>of(event ->
            count.incrementAndGet()
        )
        .filter(event -> event.mfaUsed())
        .andThen(EventObserver.of(event -> count.incrementAndGet()));

        LoginEvent mfaEvent = LoginEvent.withMfa(1L, "session-1", "fp", "ip", true);
        LoginEvent noMfaEvent = LoginEvent.withMfa(2L, "session-2", "fp", "ip", false);

        // When
        observer.onEvent(mfaEvent).join();
        observer.onEvent(noMfaEvent).join();

        // Then
        assertThat(count.get()).isEqualTo(2); // Only MFA event processed twice
    }

    @Test
    @DisplayName("Should execute observer asynchronously with virtual threads")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testAsynchronousExecution() {
        // Given
        AtomicReference<Long> executionThreadId = new AtomicReference<>();

        EventObserver<AuthEvent> observer = event ->
            CompletableFuture.supplyAsync(() -> {
                executionThreadId.set(Thread.currentThread().threadId());
                return Result.success(null);
            });

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        observer.onEvent(event).join();

        // Then
        assertThat(executionThreadId.get()).isNotNull();
        assertThat(executionThreadId.get()).isNotEqualTo(Thread.currentThread().threadId());
    }

    @Test
    @DisplayName("Should filter out all events when predicate never matches")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testFilteringAllEvents() {
        // Given
        AtomicInteger count = new AtomicInteger(0);

        EventObserver<LoginEvent> observer = EventObserver.<LoginEvent>of(event ->
            count.incrementAndGet()
        ).filter(event -> false); // Never matches

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "fp", "ip", true);

        // When
        observer.onEvent(event).join();

        // Then
        assertThat(count.get()).isZero(); // No events processed
    }

    @Test
    @DisplayName("Should handle different event types in observer")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testDifferentEventTypes() {
        // Given
        AtomicInteger loginCount = new AtomicInteger(0);
        AtomicInteger logoutCount = new AtomicInteger(0);

        EventObserver<AuthEvent> observer = EventObserver.of(event -> {
            switch (event) {
                case LoginEvent login -> loginCount.incrementAndGet();
                case LogoutEvent logout -> logoutCount.incrementAndGet();
                default -> {}
            }
        });

        LoginEvent loginEvent = LoginEvent.withMfa(1L, "session-1", "fp", "ip", true);
        LogoutEvent logoutEvent = LogoutEvent.sessionExpired(1L, "session-1", "ip");

        // When
        observer.onEvent(loginEvent).join();
        observer.onEvent(logoutEvent).join();

        // Then
        assertThat(loginCount.get()).isEqualTo(1);
        assertThat(logoutCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create multiple independent observers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMultipleIndependentObservers() {
        // Given
        AtomicInteger observer1Count = new AtomicInteger(0);
        AtomicInteger observer2Count = new AtomicInteger(0);

        EventObserver<AuthEvent> observer1 = EventObserver.of(event -> observer1Count.incrementAndGet());
        EventObserver<AuthEvent> observer2 = EventObserver.of(event -> observer2Count.incrementAndGet());

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "fp", "ip", true);

        // When
        observer1.onEvent(event).join();
        observer2.onEvent(event).join();

        // Then
        assertThat(observer1Count.get()).isEqualTo(1);
        assertThat(observer2Count.get()).isEqualTo(1);
    }
}
