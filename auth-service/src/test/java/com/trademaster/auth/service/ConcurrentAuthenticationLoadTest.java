package com.trademaster.auth.service;

import com.trademaster.auth.entity.User;
import com.trademaster.auth.pattern.Result;
import com.trademaster.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent Authentication Load Test - Virtual Thread Performance Validation
 *
 * MANDATORY: Enhanced Test Coverage - Performance Improvement #2
 * MANDATORY: Virtual Thread load testing - Rule #12
 * MANDATORY: Concurrent authentication validation - Enterprise requirement
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.threads.virtual.enabled=true",
    "trademaster.rate-limit.login.requests=1000", // High limit for load testing
    "trademaster.rate-limit.login.window-minutes=1"
})
@Transactional
@DisplayName("Concurrent Authentication Load Tests")
class ConcurrentAuthenticationLoadTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private List<User> testUsers;
    private final String testPassword = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        // Create test users for load testing
        testUsers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                .username("loadtest" + i + "@example.com")
                .email("loadtest" + i + "@example.com")
                .firstName("Load")
                .lastName("Test" + i)
                .build();
            testUsers.add(userRepository.save(user));
        }
    }

    @Test
    @DisplayName("Should handle 1000 concurrent authentication requests with Virtual Threads")
    void testHighConcurrencyAuthentication() throws Exception {
        // Test parameters
        int totalRequests = 1000;
        int maxConcurrency = 100;
        Duration maxDuration = Duration.ofSeconds(30);

        // Performance tracking
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong maxResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Instant startTime = Instant.now();

        // Submit all authentication requests
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            final int requestIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Instant requestStart = Instant.now();

                    // Use round-robin user selection
                    User testUser = testUsers.get(requestIndex % testUsers.size());
                    String clientIp = "127.0.0." + (requestIndex % 255 + 1);
                    String userAgent = "LoadTest-Agent-" + requestIndex;

                    Result<AuthenticationResult, String> result = authenticationService
                        .authenticate(testUser.getEmail(), testPassword, clientIp, userAgent);

                    Instant requestEnd = Instant.now();
                    long responseTimeMs = Duration.between(requestStart, requestEnd).toMillis();

                    // Update metrics
                    totalResponseTime.addAndGet(responseTimeMs);
                    updateMaxResponseTime(maxResponseTime, responseTimeMs);
                    updateMinResponseTime(minResponseTime, responseTimeMs);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }, executor);

            futures.add(future);
        }

        // Wait for completion with timeout
        boolean completedInTime = latch.await(maxDuration.toSeconds(), TimeUnit.SECONDS);
        Instant endTime = Instant.now();

        // Shutdown executor
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        // Calculate metrics
        Duration totalDuration = Duration.between(startTime, endTime);
        double throughputPerSecond = totalRequests / (totalDuration.toMillis() / 1000.0);
        double avgResponseTimeMs = totalResponseTime.get() / (double) totalRequests;
        double successRate = (successCount.get() / (double) totalRequests) * 100;

        // Assertions - Performance targets
        assertThat(completedInTime)
            .withFailMessage("Load test did not complete within %s seconds", maxDuration.toSeconds())
            .isTrue();

        assertThat(successRate)
            .withFailMessage("Success rate %.2f%% is below minimum 95%%", successRate)
            .isGreaterThanOrEqualTo(95.0);

        assertThat(avgResponseTimeMs)
            .withFailMessage("Average response time %.2fms exceeds 200ms target", avgResponseTimeMs)
            .isLessThanOrEqualTo(200.0);

        assertThat(maxResponseTime.get())
            .withFailMessage("Maximum response time %dms exceeds 1000ms", maxResponseTime.get())
            .isLessThanOrEqualTo(1000);

        assertThat(throughputPerSecond)
            .withFailMessage("Throughput %.2f req/sec is below minimum 100 req/sec", throughputPerSecond)
            .isGreaterThanOrEqualTo(100.0);

        // Log performance metrics
        System.out.printf("""

            === CONCURRENT AUTHENTICATION LOAD TEST RESULTS ===
            Total Requests: %d
            Duration: %s
            Success Count: %d
            Failure Count: %d
            Success Rate: %.2f%%
            Throughput: %.2f requests/second
            Average Response Time: %.2f ms
            Min Response Time: %d ms
            Max Response Time: %d ms
            ================================================

            """, totalRequests, totalDuration, successCount.get(), failureCount.get(),
            successRate, throughputPerSecond, avgResponseTimeMs,
            minResponseTime.get(), maxResponseTime.get());
    }

    @Test
    @DisplayName("Should handle burst authentication requests efficiently")
    void testBurstAuthentication() throws Exception {
        // Simulate burst of 500 requests in quick succession
        int burstSize = 500;
        CountDownLatch burstLatch = new CountDownLatch(burstSize);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        AtomicInteger successCount = new AtomicInteger(0);
        Instant burstStart = Instant.now();

        // Release all requests simultaneously
        for (int i = 0; i < burstSize; i++) {
            final int requestIndex = i;
            executor.submit(() -> {
                try {
                    User testUser = testUsers.get(requestIndex % testUsers.size());
                    Result<AuthenticationResult, String> result = authenticationService
                        .authenticate(testUser.getEmail(), testPassword,
                                    "192.168.1." + (requestIndex % 255 + 1),
                                    "BurstTest-" + requestIndex);

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    burstLatch.countDown();
                }
            });
        }

        // Wait for burst completion
        boolean burstCompleted = burstLatch.await(15, TimeUnit.SECONDS);
        Duration burstDuration = Duration.between(burstStart, Instant.now());

        executor.shutdown();

        // Assertions
        assertThat(burstCompleted).isTrue();
        assertThat(successCount.get())
            .isGreaterThanOrEqualTo((int) (burstSize * 0.95)); // 95% success rate
        assertThat(burstDuration.toSeconds())
            .isLessThanOrEqualTo(10); // Complete within 10 seconds

        System.out.printf("Burst test: %d requests in %s, %d successes\n",
            burstSize, burstDuration, successCount.get());
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void testSustainedLoad() throws Exception {
        // Run sustained load for 2 minutes with 50 req/sec
        Duration testDuration = Duration.ofMinutes(2);
        int requestsPerSecond = 50;
        int totalRequests = (int) (testDuration.toSeconds() * requestsPerSecond);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger requestCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(testDuration);

        // Submit requests at controlled rate
        CompletableFuture<Void> loadDriver = CompletableFuture.runAsync(() -> {
            try {
                while (Instant.now().isBefore(endTime)) {
                    // Submit batch of requests
                    for (int i = 0; i < requestsPerSecond && Instant.now().isBefore(endTime); i++) {
                        final int requestIndex = requestCount.getAndIncrement();

                        executor.submit(() -> {
                            try {
                                User testUser = testUsers.get(requestIndex % testUsers.size());
                                Result<AuthenticationResult, String> result = authenticationService
                                    .authenticate(testUser.getEmail(), testPassword,
                                                "10.0.0." + (requestIndex % 255 + 1),
                                                "SustainedTest-" + requestIndex);

                                if (result.isSuccess()) {
                                    successCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // Log but continue
                            }
                        });
                    }

                    // Wait for next second
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        // Wait for test completion
        loadDriver.get(testDuration.toSeconds() + 30, TimeUnit.SECONDS);

        // Allow final requests to complete
        Thread.sleep(5000);

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Duration actualDuration = Duration.between(startTime, Instant.now());
        double successRate = (successCount.get() / (double) requestCount.get()) * 100;

        // Assertions for sustained load
        assertThat(successRate)
            .withFailMessage("Sustained load success rate %.2f%% is below 90%%", successRate)
            .isGreaterThanOrEqualTo(90.0);

        assertThat(requestCount.get())
            .withFailMessage("Only %d requests submitted, expected around %d",
                           requestCount.get(), totalRequests)
            .isGreaterThanOrEqualTo((int) (totalRequests * 0.9));

        System.out.printf("Sustained load: %d requests over %s, %.2f%% success rate\n",
            requestCount.get(), actualDuration, successRate);
    }

    @Test
    @DisplayName("Should handle concurrent user registrations efficiently")
    void testConcurrentUserRegistration() throws Exception {
        int concurrentRegistrations = 200;
        CountDownLatch registrationLatch = new CountDownLatch(concurrentRegistrations);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Instant startTime = Instant.now();

        // Submit concurrent registration requests
        for (int i = 0; i < concurrentRegistrations; i++) {
            final int userIndex = i;
            executor.submit(() -> {
                try {
                    String email = "concurrent" + userIndex + "@example.com";
                    Result<User, String> result = userService.registerUser(
                        email, "SecurePassword123!", "Concurrent", "User" + userIndex,
                        "127.0.0.1", "ConcurrentTest"
                    );

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    registrationLatch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = registrationLatch.await(30, TimeUnit.SECONDS);
        Duration registrationDuration = Duration.between(startTime, Instant.now());

        executor.shutdown();

        // Assertions
        assertThat(completed).isTrue();
        assertThat(successCount.get())
            .isGreaterThanOrEqualTo((int) (concurrentRegistrations * 0.95));

        double throughput = concurrentRegistrations / (registrationDuration.toMillis() / 1000.0);
        assertThat(throughput)
            .isGreaterThanOrEqualTo(20.0); // Minimum 20 registrations/second

        System.out.printf("Concurrent registrations: %d/%d successful in %s (%.2f reg/sec)\n",
            successCount.get(), concurrentRegistrations, registrationDuration, throughput);
    }

    // Helper methods
    private void updateMaxResponseTime(AtomicLong maxResponseTime, long responseTime) {
        maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));
    }

    private void updateMinResponseTime(AtomicLong minResponseTime, long responseTime) {
        minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
    }
}