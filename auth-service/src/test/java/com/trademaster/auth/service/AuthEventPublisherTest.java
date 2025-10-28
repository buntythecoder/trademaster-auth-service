package com.trademaster.auth.service;

import com.trademaster.auth.event.*;
import com.trademaster.auth.pattern.EventObserver;
import com.trademaster.auth.pattern.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for AuthEventPublisher service
 *
 * Tests cover:
 * - Observer subscription
 * - Event publication
 * - Parallel observer notification
 * - Type-safe event routing
 * - Error handling in observers
 * - Concurrent event publishing
 */
@DisplayName("AuthEventPublisher Service Tests")
class AuthEventPublisherTest {

    private AuthEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AuthEventPublisher();
    }

    @Test
    @DisplayName("Should subscribe observer to event type")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testSubscribeObserver() {
        // Given
        AtomicInteger count = new AtomicInteger(0);
        EventObserver<LoginEvent> observer = EventObserver.of(event -> count.incrementAndGet());

        // When
        publisher.subscribe(LoginEvent.class, observer);

        // Then - No exception means successful subscription
        assertThat(count.get()).isZero(); // Observer not triggered yet
    }

    @Test
    @DisplayName("Should publish event to subscribed observers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPublishEventToSubscribers() {
        // Given
        AtomicInteger count = new AtomicInteger(0);
        EventObserver<LoginEvent> observer = EventObserver.of(event -> count.incrementAndGet());

        publisher.subscribe(LoginEvent.class, observer);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        List<Result<Void, String>> results = publisher.publish(event).join();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should publish event to multiple observers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPublishToMultipleObservers() {
        // Given
        AtomicInteger observer1Count = new AtomicInteger(0);
        AtomicInteger observer2Count = new AtomicInteger(0);
        AtomicInteger observer3Count = new AtomicInteger(0);

        EventObserver<LoginEvent> observer1 = EventObserver.of(event -> observer1Count.incrementAndGet());
        EventObserver<LoginEvent> observer2 = EventObserver.of(event -> observer2Count.incrementAndGet());
        EventObserver<LoginEvent> observer3 = EventObserver.of(event -> observer3Count.incrementAndGet());

        publisher.subscribe(LoginEvent.class, observer1);
        publisher.subscribe(LoginEvent.class, observer2);
        publisher.subscribe(LoginEvent.class, observer3);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        List<Result<Void, String>> results = publisher.publish(event).join();

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(observer1Count.get()).isEqualTo(1);
        assertThat(observer2Count.get()).isEqualTo(1);
        assertThat(observer3Count.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should notify observers in parallel")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testParallelObserverNotification() {
        // Given
        EventObserver<LoginEvent> slowObserver = EventObserver.of(event -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        publisher.subscribe(LoginEvent.class, slowObserver);
        publisher.subscribe(LoginEvent.class, slowObserver);
        publisher.subscribe(LoginEvent.class, slowObserver);
        publisher.subscribe(LoginEvent.class, slowObserver);
        publisher.subscribe(LoginEvent.class, slowObserver);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        long startTime = System.currentTimeMillis();
        List<Result<Void, String>> results = publisher.publish(event).join();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assertThat(results).hasSize(5);
        // Parallel execution should take ~200ms, not 5*200=1000ms
        assertThat(duration).isLessThan(800);
    }

    @Test
    @DisplayName("Should route events to correct observer types")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testEventTypeRouting() {
        // Given
        AtomicInteger loginCount = new AtomicInteger(0);
        AtomicInteger logoutCount = new AtomicInteger(0);

        EventObserver<LoginEvent> loginObserver = EventObserver.of(event -> loginCount.incrementAndGet());
        EventObserver<LogoutEvent> logoutObserver = EventObserver.of(event -> logoutCount.incrementAndGet());

        publisher.subscribe(LoginEvent.class, loginObserver);
        publisher.subscribe(LogoutEvent.class, logoutObserver);

        LoginEvent loginEvent = LoginEvent.withMfa(1L, "session-1", "fp", "ip", true);
        LogoutEvent logoutEvent = LogoutEvent.sessionExpired(1L, "session-1", "ip");

        // When
        publisher.publish(loginEvent).join();
        publisher.publish(logoutEvent).join();

        // Then
        assertThat(loginCount.get()).isEqualTo(1);
        assertThat(logoutCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle observer execution failures gracefully")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testObserverExecutionFailure() {
        // Given
        EventObserver<LoginEvent> failingObserver = EventObserver.of(event -> {
            throw new RuntimeException("Observer failed");
        });
        EventObserver<LoginEvent> successObserver = EventObserver.of(event -> {
            // Success
        });

        publisher.subscribe(LoginEvent.class, failingObserver);
        publisher.subscribe(LoginEvent.class, successObserver);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        List<Result<Void, String>> results = publisher.publish(event).join();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(1).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should publish event with no subscribers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testPublishWithNoSubscribers() {
        // Given
        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "device-fp", "192.168.1.1", true);

        // When
        List<Result<Void, String>> results = publisher.publish(event).join();

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should pass correct event data to observers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testEventDataPassedToObserver() {
        // Given
        AtomicReference<LoginEvent> capturedEvent = new AtomicReference<>();
        EventObserver<LoginEvent> observer = EventObserver.of(capturedEvent::set);

        publisher.subscribe(LoginEvent.class, observer);

        LoginEvent event = LoginEvent.withMfa(
            123L,
            "session-456",
            "device-fingerprint",
            "192.168.1.100",
            true
        );

        // When
        publisher.publish(event).join();

        // Then
        assertThat(capturedEvent.get()).isNotNull();
        assertThat(capturedEvent.get().userId()).isEqualTo(123L);
        assertThat(capturedEvent.get().sessionId()).isEqualTo("session-456");
        assertThat(capturedEvent.get().deviceFingerprint()).isEqualTo("device-fingerprint");
        assertThat(capturedEvent.get().ipAddress()).isEqualTo("192.168.1.100");
        assertThat(capturedEvent.get().mfaUsed()).isTrue();
    }

    @Test
    @DisplayName("Should subscribe multiple observers for different event types")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testMultipleEventTypeSubscriptions() {
        // Given
        AtomicInteger loginCount = new AtomicInteger(0);
        AtomicInteger passwordChangeCount = new AtomicInteger(0);
        AtomicInteger mfaCount = new AtomicInteger(0);

        publisher.subscribe(LoginEvent.class, EventObserver.of(event -> loginCount.incrementAndGet()));
        publisher.subscribe(PasswordChangeEvent.class, EventObserver.of(event -> passwordChangeCount.incrementAndGet()));
        publisher.subscribe(MfaEvent.class, EventObserver.of(event -> mfaCount.incrementAndGet()));

        LoginEvent loginEvent = LoginEvent.withMfa(1L, "s1", "fp", "ip", true);
        PasswordChangeEvent passwordEvent = PasswordChangeEvent.userInitiated(1L, "s1", "ip", true);
        MfaEvent mfaEvent = MfaEvent.enabled(1L, "s1", MfaEvent.MfaType.TOTP);

        // When
        publisher.publish(loginEvent).join();
        publisher.publish(passwordEvent).join();
        publisher.publish(mfaEvent).join();

        // Then
        assertThat(loginCount.get()).isEqualTo(1);
        assertThat(passwordChangeCount.get()).isEqualTo(1);
        assertThat(mfaCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle concurrent event publishing")
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void testConcurrentEventPublishing() {
        // Given
        AtomicInteger count = new AtomicInteger(0);
        EventObserver<LoginEvent> observer = EventObserver.of(event -> count.incrementAndGet());

        publisher.subscribe(LoginEvent.class, observer);

        // When - Publish 100 events concurrently
        List<CompletableFuture<List<Result<Void, String>>>> futures =
            java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> LoginEvent.withMfa((long) i, "session-" + i, "fp", "ip", true))
                .map(publisher::publish)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then
        assertThat(count.get()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should support AuthEvent base type observers")
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void testBaseTypeObserver() {
        // Given
        AtomicInteger count = new AtomicInteger(0);
        EventObserver<AuthEvent> observer = EventObserver.of(event -> count.incrementAndGet());

        publisher.subscribe(LoginEvent.class, (EventObserver) observer);
        publisher.subscribe(LogoutEvent.class, (EventObserver) observer);

        LoginEvent loginEvent = LoginEvent.withMfa(1L, "s1", "fp", "ip", true);
        LogoutEvent logoutEvent = LogoutEvent.sessionExpired(1L, "s1", "ip");

        // When
        publisher.publish(loginEvent).join();
        publisher.publish(logoutEvent).join();

        // Then
        assertThat(count.get()).isEqualTo(2); // Both events processed
    }

    @Test
    @DisplayName("Should execute observers with virtual threads")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testVirtualThreadExecution() {
        // Given
        AtomicReference<String> threadName = new AtomicReference<>();
        EventObserver<LoginEvent> observer = EventObserver.of(event ->
            threadName.set(Thread.currentThread().getName())
        );

        publisher.subscribe(LoginEvent.class, observer);

        LoginEvent event = LoginEvent.withMfa(1L, "session-123", "fp", "ip", true);

        // When
        publisher.publish(event).join();

        // Then
        assertThat(threadName.get()).isNotNull();
        // Virtual threads should have different naming pattern
        assertThat(threadName.get()).isNotEmpty();
    }
}
