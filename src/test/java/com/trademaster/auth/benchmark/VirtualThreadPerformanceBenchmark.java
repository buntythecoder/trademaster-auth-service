package com.trademaster.auth.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

/**
 * Virtual Thread Performance Benchmarks
 *
 * MANDATORY: Virtual Thread Performance Validation - Rule #12
 * MANDATORY: Concurrent Processing Performance - Enterprise requirement
 * MANDATORY: Platform Thread vs Virtual Thread Comparison
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class VirtualThreadPerformanceBenchmark {

    @Param({"10", "100", "1000", "5000"})
    private int taskCount;

    @Param({"10", "50", "100"})
    private int ioDelayMs;

    private ExecutorService virtualThreadExecutor;
    private ExecutorService platformThreadExecutor;

    @Setup(Level.Trial)
    public void setUp() {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        platformThreadExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        shutdownExecutor(virtualThreadExecutor, "Virtual Thread Executor");
        shutdownExecutor(platformThreadExecutor, "Platform Thread Executor");
    }

    /**
     * Benchmark Virtual Thread I/O intensive operations
     * Target: Superior performance for I/O bound tasks
     */
    @Benchmark
    public void benchmarkVirtualThreadIO(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong totalTime = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            virtualThreadExecutor.submit(() -> {
                try {
                    long taskStart = System.nanoTime();

                    // Simulate I/O operation (authentication, database call, etc.)
                    Thread.sleep(ioDelayMs);

                    // Simulate some CPU work
                    performAuthenticationWork();

                    long taskEnd = System.nanoTime();
                    totalTime.addAndGet(taskEnd - taskStart);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        bh.consume(totalTime.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark Platform Thread I/O intensive operations
     * Comparison baseline for Virtual Threads
     */
    @Benchmark
    public void benchmarkPlatformThreadIO(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong totalTime = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            platformThreadExecutor.submit(() -> {
                try {
                    long taskStart = System.nanoTime();

                    // Simulate I/O operation
                    Thread.sleep(ioDelayMs);

                    // Simulate some CPU work
                    performAuthenticationWork();

                    long taskEnd = System.nanoTime();
                    totalTime.addAndGet(taskEnd - taskStart);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        bh.consume(totalTime.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark Virtual Thread creation overhead
     * Target: Minimal overhead for thread creation
     */
    @Benchmark
    public void benchmarkVirtualThreadCreation(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            Thread.startVirtualThread(() -> {
                try {
                    // Minimal work to measure creation overhead
                    completedTasks.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        bh.consume(completedTasks.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark Platform Thread creation overhead
     * Comparison baseline for Virtual Thread creation
     */
    @Benchmark
    public void benchmarkPlatformThreadCreation(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>(taskCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    // Minimal work to measure creation overhead
                    completedTasks.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        latch.await(10, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        // Clean up threads
        for (Thread thread : threads) {
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        bh.consume(completedTasks.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark Virtual Thread concurrent authentication simulation
     * Target: Handle thousands of concurrent authentications efficiently
     */
    @Benchmark
    public void benchmarkConcurrentAuthentication(Blackhole bh) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            final int userId = i;
            virtualThreadExecutor.submit(() -> {
                try {
                    // Simulate authentication workflow
                    if (simulateAuthentication(userId)) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.nanoTime();

        bh.consume(successCount.get());
        bh.consume(failureCount.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark Structured Concurrency performance
     * Target: Efficient coordination of related tasks
     */
    @Benchmark
    public void benchmarkStructuredConcurrency(Blackhole bh) throws InterruptedException {
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.nanoTime();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Submit related authentication tasks
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                scope.fork(() -> {
                    try {
                        // Simulate related authentication operations
                        performAuthenticationWork();
                        Thread.sleep(ioDelayMs / 10); // Reduced delay for related tasks
                        completedTasks.incrementAndGet();
                        return taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });
            }

            // Wait for all tasks to complete
            scope.join();
            scope.throwIfFailed();

        } catch (ExecutionException e) {
            // Handle execution failure
            bh.consume(e);
        }

        long endTime = System.nanoTime();

        bh.consume(completedTasks.get());
        bh.consume(endTime - startTime);
    }

    /**
     * Benchmark memory usage comparison
     * Target: Lower memory footprint with Virtual Threads
     */
    @Benchmark
    public void benchmarkMemoryUsage(Blackhole bh) throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        // Measure initial memory
        runtime.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            virtualThreadExecutor.submit(() -> {
                try {
                    // Create some objects to simulate authentication work
                    String[] data = new String[10];
                    for (int j = 0; j < data.length; j++) {
                        data[j] = "user-" + Thread.currentThread().threadId() + "-" + j;
                    }
                    Thread.sleep(ioDelayMs / 5);
                    bh.consume(data);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);

        // Measure final memory
        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;

        bh.consume(memoryUsed);
    }

    // Helper methods

    private void performAuthenticationWork() {
        // Simulate CPU-intensive authentication operations
        int hash = 0;
        String data = "user-authentication-data-" + Thread.currentThread().threadId();

        for (int i = 0; i < 1000; i++) {
            hash = hash * 31 + data.hashCode();
        }

        // Prevent optimization
        if (hash == Integer.MAX_VALUE) {
            System.out.println("Unlikely hash collision");
        }
    }

    private boolean simulateAuthentication(int userId) {
        try {
            // Simulate database lookup
            Thread.sleep(ioDelayMs / 20);

            // Simulate password validation
            performAuthenticationWork();

            // Simulate token generation
            Thread.sleep(ioDelayMs / 50);

            // 95% success rate
            return (userId % 100) < 95;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void shutdownExecutor(ExecutorService executor, String name) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println(name + " did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.err.println(name + " did not terminate after forced shutdown");
            }
        }
    }
}