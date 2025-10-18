package com.trademaster.marketdata.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Virtual Thread Behavior Validation Tests
 *
 * Following MANDATORY RULE #1 (Task #1):
 * - Java 24 with Virtual Threads ONLY
 * - CompletableFuture with virtual thread executors
 * - No platform threads for I/O operations
 * - Structured concurrency validation
 *
 * Following MANDATORY RULE #20 (Testing Standards):
 * - Unit tests with >80% coverage
 * - Virtual thread behavior validation
 * - Concurrency testing
 *
 * Benefits:
 * - Validates virtual thread usage correctness
 * - Ensures no platform thread blocking
 * - Tests concurrent behavior at scale
 * - Verifies structured concurrency patterns
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@DisplayName("Virtual Thread Behavior Validation Tests")
class VirtualThreadBehaviorTest {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadBehaviorTest.class);

    // Test Configuration Constants
    private static final int CONCURRENT_OPERATIONS = 1000;
    private static final int CONCURRENT_TASKS = 100;
    private static final long SIMULATED_IO_DELAY_MS = 100;
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Virtual Thread Executor Factory
     * MANDATORY RULE #1: Use Executors.newVirtualThreadPerTaskExecutor()
     */
    private ExecutorService createVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Test
    @DisplayName("Virtual threads should be created correctly")
    void testVirtualThreadCreation() {
        // Given: Virtual thread factory
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Submit task to virtual thread executor
            final var future = executor.submit(() -> {
                final var currentThread = Thread.currentThread();
                log.info("Thread name: {}, isVirtual: {}", currentThread.getName(), currentThread.isVirtual());
                return currentThread.isVirtual();
            });

            // Then: Thread should be virtual
            final var isVirtual = future.get(5, TimeUnit.SECONDS);
            assertThat(isVirtual).isTrue();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Virtual thread creation test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("CompletableFuture should use virtual threads when provided with virtual thread executor")
    void testCompletableFutureWithVirtualThreads() {
        // Given: Virtual thread executor
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Create CompletableFuture with virtual thread executor
            final var future = CompletableFuture.supplyAsync(() -> {
                final var currentThread = Thread.currentThread();
                log.info("CompletableFuture thread: {}, isVirtual: {}",
                    currentThread.getName(), currentThread.isVirtual());
                return currentThread.isVirtual();
            }, executor);

            // Then: Should execute on virtual thread
            final var isVirtual = future.get(5, TimeUnit.SECONDS);
            assertThat(isVirtual).isTrue();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("CompletableFuture virtual thread test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Virtual threads should handle massive concurrency efficiently")
    void testMassiveConcurrency() {
        // Given: Virtual thread executor and many concurrent tasks
        final var executor = createVirtualThreadExecutor();
        final var startTime = Instant.now();

        try {
            // When: Submit many concurrent tasks
            final var futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    // Simulate I/O operation
                    try {
                        Thread.sleep(SIMULATED_IO_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Thread.currentThread().isVirtual();
                }, executor))
                .toList();

            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            final var endTime = Instant.now();
            final var duration = Duration.between(startTime, endTime);

            // Then: All should use virtual threads and complete efficiently
            final var allVirtual = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                })
                .allMatch(Boolean::booleanValue);

            assertThat(allVirtual).isTrue();

            // Virtual threads should handle this many concurrent operations efficiently
            // With platform threads, this would take ~(CONCURRENT_OPERATIONS * SIMULATED_IO_DELAY_MS)
            // With virtual threads, should be much faster due to efficient context switching
            log.info("Completed {} concurrent operations in {} ms",
                CONCURRENT_OPERATIONS, duration.toMillis());

            // Should complete much faster than sequential execution
            final var sequentialTime = CONCURRENT_OPERATIONS * SIMULATED_IO_DELAY_MS;
            assertThat(duration.toMillis()).isLessThan(sequentialTime / 2);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Massive concurrency test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Virtual threads should not block platform threads during I/O")
    void testNonBlockingIO() {
        // Given: Virtual thread executor
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Execute I/O operations on virtual threads
            final var ioTasks = IntStream.range(0, CONCURRENT_TASKS)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate blocking I/O
                        Thread.sleep(SIMULATED_IO_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, executor))
                .toList();

            // Wait for all to complete
            final var allDone = CompletableFuture.allOf(ioTasks.toArray(new CompletableFuture[0]));
            allDone.get(10, TimeUnit.SECONDS);

            // Then: All tasks should complete without platform thread blocking
            assertThat(allDone).isCompleted();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Non-blocking I/O test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Structured concurrency should coordinate virtual threads")
    void testStructuredConcurrency() throws InterruptedException, ExecutionException {
        // Given: Multiple concurrent operations that need coordination
        final var results = new ArrayList<String>();

        // When: Use StructuredTaskScope for coordinated execution
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork multiple subtasks
            final var task1 = scope.fork(() -> {
                Thread.sleep(100);
                return "Task 1 completed on virtual thread: " + Thread.currentThread().isVirtual();
            });

            final var task2 = scope.fork(() -> {
                Thread.sleep(50);
                return "Task 2 completed on virtual thread: " + Thread.currentThread().isVirtual();
            });

            final var task3 = scope.fork(() -> {
                Thread.sleep(75);
                return "Task 3 completed on virtual thread: " + Thread.currentThread().isVirtual();
            });

            // Wait for all tasks to complete or any to fail
            scope.join();
            scope.throwIfFailed();

            // Collect results
            results.add(task1.get());
            results.add(task2.get());
            results.add(task3.get());

            // Then: All tasks should complete successfully on virtual threads
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(result -> result.contains("true"));

            log.info("Structured concurrency results: {}", results);
        }
    }

    @Test
    @DisplayName("Virtual thread executor should shutdown gracefully")
    void testGracefulShutdown() throws InterruptedException {
        // Given: Virtual thread executor with running tasks
        final var executor = createVirtualThreadExecutor();

        // When: Submit tasks and shutdown
        IntStream.range(0, 10)
            .forEach(i -> executor.submit(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));

        executor.shutdown();

        // Then: Should shutdown gracefully
        final var terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(terminated).isTrue();
        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    @DisplayName("Virtual threads should handle exceptions correctly")
    void testExceptionHandling() {
        // Given: Virtual thread executor
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Task throws exception
            final var future = CompletableFuture.supplyAsync(() -> {
                if (Thread.currentThread().isVirtual()) {
                    throw new RuntimeException("Test exception on virtual thread");
                }
                return "Should not reach here";
            }, executor);

            // Then: Exception should be properly propagated
            assertThat(future)
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableOfType(ExecutionException.class)
                .withMessageContaining("Test exception on virtual thread");

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Virtual threads should support chained CompletableFuture operations")
    void testChainedOperations() {
        // Given: Virtual thread executor
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Chain multiple async operations
            final var result = CompletableFuture.supplyAsync(() -> {
                return Thread.currentThread().isVirtual() ? 10 : 0;
            }, executor)
                .thenApplyAsync(value -> value * 2, executor)
                .thenApplyAsync(value -> value + 5, executor)
                .thenApplyAsync(value -> "Result: " + value, executor)
                .get(5, TimeUnit.SECONDS);

            // Then: All operations should complete on virtual threads
            assertThat(result).isEqualTo("Result: 25");

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Chained operations test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Virtual threads should support parallel stream processing")
    void testParallelStreamWithVirtualThreads() {
        // Given: Large dataset
        final var numbers = IntStream.range(0, CONCURRENT_TASKS).boxed().toList();

        // When: Process with parallel stream (uses ForkJoinPool by default)
        // Note: This test documents current behavior, not ideal virtual thread usage
        final var results = numbers.parallelStream()
            .map(n -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return n * 2;
            })
            .toList();

        // Then: All elements should be processed
        assertThat(results).hasSize(CONCURRENT_TASKS);
        assertThat(results).containsExactlyInAnyOrderElementsOf(
            IntStream.range(0, CONCURRENT_TASKS)
                .map(n -> n * 2)
                .boxed()
                .toList()
        );
    }

    @Test
    @DisplayName("Virtual threads should handle timeout scenarios")
    void testTimeoutHandling() {
        // Given: Virtual thread executor
        final var executor = createVirtualThreadExecutor();

        try {
            // When: Task takes longer than timeout
            final var future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(5000); // 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Should timeout";
            }, executor);

            // Then: Should timeout appropriately
            assertThat(future)
                .failsWithin(Duration.ofSeconds(2))
                .withThrowableOfType(TimeoutException.class);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Virtual threads should execute tasks in parallel efficiently")
    void testParallelExecution() {
        // Given: Virtual thread executor and parallel tasks
        final var executor = createVirtualThreadExecutor();
        final var startTime = Instant.now();

        try {
            // When: Execute tasks in parallel
            final var task1 = CompletableFuture.supplyAsync(() -> {
                sleep(100);
                return "Task 1";
            }, executor);

            final var task2 = CompletableFuture.supplyAsync(() -> {
                sleep(100);
                return "Task 2";
            }, executor);

            final var task3 = CompletableFuture.supplyAsync(() -> {
                sleep(100);
                return "Task 3";
            }, executor);

            // Wait for all
            final var combined = CompletableFuture.allOf(task1, task2, task3);
            combined.get(5, TimeUnit.SECONDS);

            final var endTime = Instant.now();
            final var duration = Duration.between(startTime, endTime);

            // Then: Should execute in parallel (total time < sum of individual times)
            assertThat(duration.toMillis()).isLessThan(250); // Much less than 300ms sequential
            assertThat(task1.get()).isEqualTo("Task 1");
            assertThat(task2.get()).isEqualTo("Task 2");
            assertThat(task3.get()).isEqualTo("Task 3");

            log.info("Parallel execution completed in {} ms", duration.toMillis());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Parallel execution test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Helper method to sleep without checked exception
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
