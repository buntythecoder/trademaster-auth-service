package com.trademaster.portfolio.concurrent;

import com.trademaster.portfolio.concurrent.PortfolioTaskScope.CoordinatedResult;
import com.trademaster.portfolio.concurrent.PortfolioTaskScope.PortfolioConcurrencyError;
import com.trademaster.portfolio.functional.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structured Concurrency Testing for Portfolio Operations
 * 
 * Tests Java 24 Virtual Threads integration with StructuredTaskScope.
 * Validates coordinated task execution, timeout handling, and error propagation.
 * 
 * Testing Patterns:
 * - Virtual Thread validation
 * - Structured concurrency coordination
 * - Timeout behavior verification
 * - Error propagation testing
 * - Performance characteristics
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - Testing Suite)
 */
class PortfolioTaskScopeTest {
    
    @Test
    @DisplayName("Should execute coordinated operation successfully")
    @Timeout(5)
    void shouldExecuteCoordinatedOperationSuccessfully() {
        // Given
        String operationId = "test-coordinated-success";
        String expectedResult = "Operation completed successfully";
        
        // When
        Result<String, PortfolioConcurrencyError> result = 
            PortfolioTaskScope.executeCoordinated(operationId, () -> {
                // Simulate portfolio operation
                return expectedResult;
            });
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuccess().orElseThrow()).isEqualTo(expectedResult);
    }
    
    @Test
    @DisplayName("Should handle operation timeout gracefully")
    @Timeout(10)
    void shouldHandleOperationTimeoutGracefully() {
        // Given
        String operationId = "test-timeout";
        Duration shortTimeout = Duration.ofMillis(100);
        
        // When
        Result<String, PortfolioConcurrencyError> result = 
            PortfolioTaskScope.executeCoordinated(operationId, shortTimeout, () -> {
                // Simulate slow operation
                try {
                    Thread.sleep(500); // Longer than timeout
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Should not reach here";
            });
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure().orElseThrow()).isInstanceOf(PortfolioConcurrencyError.TaskTimeout.class);

        PortfolioConcurrencyError.TaskTimeout timeout =
            (PortfolioConcurrencyError.TaskTimeout) result.getFailure().orElseThrow();
        assertThat(timeout.operationId()).isEqualTo(operationId);
        assertThat(timeout.timeout()).isEqualTo(shortTimeout);
    }
    
    @Test
    @DisplayName("Should propagate operation failure properly")
    @Timeout(5)
    void shouldPropagateOperationFailureProperly() {
        // Given
        String operationId = "test-failure";
        RuntimeException expectedException = new RuntimeException("Portfolio calculation failed");

        // When
        Result<String, PortfolioConcurrencyError> result =
            PortfolioTaskScope.executeCoordinated(operationId, () -> {
                throw expectedException;
            });

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure().orElseThrow()).isInstanceOf(PortfolioConcurrencyError.TaskFailed.class);

        PortfolioConcurrencyError.TaskFailed failure =
            (PortfolioConcurrencyError.TaskFailed) result.getFailure().orElseThrow();
        assertThat(failure.operationId()).isEqualTo(operationId);
        assertThat(failure.cause()).isEqualTo(expectedException);
    }
    
    @Test
    @DisplayName("Should execute parallel operations successfully")
    @Timeout(5)
    void shouldExecuteParallelOperationsSuccessfully() {
        // Given
        String operationId = "test-parallel-success";
        String expectedResult1 = "Portfolio data loaded";
        Integer expectedResult2 = 42;
        
        // When
        Result<CoordinatedResult<String, Integer>, PortfolioConcurrencyError> result = 
            PortfolioTaskScope.executeParallel(
                operationId,
                () -> {
                    // Simulate portfolio data loading
                    simulateWork(50);
                    return expectedResult1;
                },
                () -> {
                    // Simulate risk calculation
                    simulateWork(100);
                    return expectedResult2;
                }
            );
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        CoordinatedResult<String, Integer> coordinated = result.getSuccess().orElseThrow();
        assertThat(coordinated.result1()).isEqualTo(expectedResult1);
        assertThat(coordinated.result2()).isEqualTo(expectedResult2);
    }
    
    @Test
    @DisplayName("Should handle parallel operation failure")
    @Timeout(5)
    void shouldHandleParallelOperationFailure() {
        // Given
        String operationId = "test-parallel-failure";
        RuntimeException expectedException = new RuntimeException("Risk calculation failed");

        // When
        Result<CoordinatedResult<String, Integer>, PortfolioConcurrencyError> result =
            PortfolioTaskScope.executeParallel(
                operationId,
                () -> {
                    // First operation succeeds
                    return "Success";
                },
                () -> {
                    // Second operation fails
                    throw expectedException;
                }
            );

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure().orElseThrow()).isInstanceOf(PortfolioConcurrencyError.TaskFailed.class);

        PortfolioConcurrencyError.TaskFailed failure =
            (PortfolioConcurrencyError.TaskFailed) result.getFailure().orElseThrow();
        assertThat(failure.operationId()).contains("task2");
        assertThat(failure.cause()).isEqualTo(expectedException);
    }
    
    @Test
    @DisplayName("Should handle parallel operation timeout")
    @Timeout(10)
    void shouldHandleParallelOperationTimeout() {
        // Given
        String operationId = "test-parallel-timeout";
        Duration shortTimeout = Duration.ofMillis(200);
        
        // When
        Result<CoordinatedResult<String, String>, PortfolioConcurrencyError> result = 
            PortfolioTaskScope.executeParallel(
                operationId,
                shortTimeout,
                () -> {
                    // First operation completes quickly
                    return "Fast operation";
                },
                () -> {
                    // Second operation is slow
                    simulateWork(500);
                    return "Slow operation";
                }
            );
        
        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure().orElseThrow()).isInstanceOf(PortfolioConcurrencyError.TaskTimeout.class);
    }
    
    @Test
    @DisplayName("Should execute operations on virtual threads")
    @Timeout(5)
    void shouldExecuteOperationsOnVirtualThreads() {
        // Given
        String operationId = "test-virtual-threads";
        
        // When
        Result<String, PortfolioConcurrencyError> result = 
            PortfolioTaskScope.executeCoordinated(operationId, () -> {
                // Verify we're running on a virtual thread
                Thread currentThread = Thread.currentThread();
                return currentThread.isVirtual() ? "Virtual thread confirmed" : "Platform thread detected";
            });
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSuccess().orElseThrow()).isEqualTo("Virtual thread confirmed");
    }
    
    @Test
    @DisplayName("Should handle concurrent portfolio operations efficiently")
    @Timeout(10)
    void shouldHandleConcurrentPortfolioOperationsEfficiently() {
        // Given
        int numberOfOperations = 100;
        
        // When - Execute multiple portfolio operations concurrently using functional approach
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture<?>[] futures = java.util.stream.IntStream.range(0, numberOfOperations)
            .mapToObj(operationIndex ->
                CompletableFuture.supplyAsync(() ->
                    PortfolioTaskScope.executeCoordinated(
                        "concurrent-test-" + operationIndex,
                        () -> {
                            // Simulate portfolio calculation
                            simulateWork(ThreadLocalRandom.current().nextInt(10, 50));
                            return "Operation " + operationIndex + " completed";
                        }
                    ), executor
                )
            )
            .toArray(CompletableFuture[]::new);
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures).join();
        
        // Then - Verify all operations completed successfully using functional approach
        java.util.Arrays.stream(futures)
            .forEach(future -> {
                @SuppressWarnings("unchecked")
                Result<String, PortfolioConcurrencyError> result = 
                    (Result<String, PortfolioConcurrencyError>) future.join();
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getSuccess().orElseThrow()).contains("completed");
            });
    }
    
    @Test
    @DisplayName("Should handle interruption gracefully")
    @Timeout(5)
    void shouldHandleInterruptionGracefully() {
        // Given
        String operationId = "test-interruption";

        // Start operation in separate thread and interrupt it
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture<Result<String, PortfolioConcurrencyError>> futureResult =
            CompletableFuture.supplyAsync(() ->
                PortfolioTaskScope.executeCoordinated(operationId, () -> {
                    try {
                        // Simulate long-running operation
                        Thread.sleep(2000);
                        return "Should not complete";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted", e);
                    }
                }), executor
            );

        // When - Cancel the future after short delay
        CompletableFuture<Void> canceller = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                futureResult.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, executor);

        // Wait for canceller to complete
        canceller.join();

        // Then - Operation should be cancelled or completed with failure
        try {
            Thread.sleep(500); // Give time for cancellation to propagate
            assertThat(futureResult).isCompletedExceptionally()
                .withFailMessage("Future should be cancelled or completed exceptionally");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Simulate work by sleeping for specified milliseconds
     */
    private void simulateWork(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }
}