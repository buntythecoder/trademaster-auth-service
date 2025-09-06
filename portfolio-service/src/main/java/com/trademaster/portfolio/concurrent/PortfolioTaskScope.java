package com.trademaster.portfolio.concurrent;

import com.trademaster.portfolio.functional.Result;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Structured Concurrency for Portfolio Operations (Rule #28 Implementation)
 * 
 * Provides coordinated task execution with Virtual Threads and structured concurrency.
 * Implements Java 24 preview feature for enterprise-grade portfolio operations.
 * 
 * Features:
 * - Virtual Thread coordination with StructuredTaskScope
 * - Timeout-aware operations with graceful degradation
 * - Error propagation with functional Result types
 * - Resource cleanup with AutoCloseable pattern
 * - Performance monitoring with metrics collection
 * 
 * Performance Targets:
 * - Task coordination overhead: <5ms
 * - Concurrent operation limit: 50 simultaneous tasks
 * - Memory usage: <10MB per scope
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Epic 3 - AgentOS Compliance)
 */
@Slf4j
public class PortfolioTaskScope<T> extends StructuredTaskScope<T> {
    
    private final String operationId;
    private final Instant startTime;
    private final Duration timeout;
    
    /**
     * Create a new portfolio task scope with timeout
     */
    private PortfolioTaskScope(String operationId, Duration timeout) {
        super();
        this.operationId = operationId;
        this.startTime = Instant.now();
        this.timeout = timeout;
        
        log.debug("Started portfolio task scope: {} with timeout: {}", operationId, timeout);
    }
    
    /**
     * Create portfolio task scope with default timeout
     */
    public static <T> PortfolioTaskScope<T> create(String operationId) {
        return new PortfolioTaskScope<>(operationId, Duration.ofSeconds(30));
    }
    
    /**
     * Create portfolio task scope with custom timeout
     */
    public static <T> PortfolioTaskScope<T> create(String operationId, Duration timeout) {
        return new PortfolioTaskScope<>(operationId, timeout);
    }
    
    /**
     * Execute coordinated portfolio operation with structured concurrency
     */
    public static <T> Result<T, PortfolioConcurrencyError> executeCoordinated(
            String operationId,
            Supplier<T> operation) {
        
        return executeCoordinated(operationId, Duration.ofSeconds(30), operation);
    }
    
    /**
     * Execute coordinated portfolio operation with custom timeout
     */
    public static <T> Result<T, PortfolioConcurrencyError> executeCoordinated(
            String operationId,
            Duration timeout,
            Supplier<T> operation) {
        
        try (var scope = PortfolioTaskScope.<T>create(operationId, timeout)) {
            
            // Fork the operation in virtual thread
            Subtask<T> task = scope.fork(operation::get);
            
            // Wait for completion with timeout
            scope.joinUntil(Instant.now().plus(timeout));
            
            // Get result or handle failure
            return switch (task.state()) {
                case SUCCESS -> {
                    T result = task.get();
                    scope.logCompletion(true, null);
                    yield Result.success(result);
                }
                case FAILED -> {
                    Throwable exception = task.exception();
                    scope.logCompletion(false, exception);
                    yield Result.failure(PortfolioConcurrencyError.taskFailed(operationId, exception));
                }
                case UNAVAILABLE -> {
                    scope.logCompletion(false, null);
                    yield Result.failure(PortfolioConcurrencyError.taskTimeout(operationId, timeout));
                }
            };
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Portfolio operation interrupted: {}", operationId, e);
            return Result.failure(PortfolioConcurrencyError.interrupted(operationId, e));
            
        } catch (TimeoutException e) {
            log.error("Portfolio operation timeout: {} after {}", operationId, timeout, e);
            return Result.failure(PortfolioConcurrencyError.taskTimeout(operationId, timeout));
            
        } catch (Exception e) {
            log.error("Portfolio operation failed: {}", operationId, e);
            return Result.failure(PortfolioConcurrencyError.taskFailed(operationId, e));
        }
    }
    
    /**
     * Execute multiple coordinated portfolio operations in parallel
     */
    public static <T1, T2> Result<CoordinatedResult<T1, T2>, PortfolioConcurrencyError> executeParallel(
            String operationId,
            Supplier<T1> operation1,
            Supplier<T2> operation2) {
        
        return executeParallel(operationId, Duration.ofSeconds(30), operation1, operation2);
    }
    
    /**
     * Execute multiple coordinated portfolio operations in parallel with timeout
     */
    public static <T1, T2> Result<CoordinatedResult<T1, T2>, PortfolioConcurrencyError> executeParallel(
            String operationId,
            Duration timeout,
            Supplier<T1> operation1,
            Supplier<T2> operation2) {
        
        try (var scope = PortfolioTaskScope.<Object>create(operationId, timeout)) {
            
            // Fork both operations in parallel
            Subtask<T1> task1 = scope.fork(operation1::get);
            Subtask<T2> task2 = scope.fork(operation2::get);
            
            // Wait for both to complete
            scope.joinUntil(Instant.now().plus(timeout));
            
            // Check both results
            return switch (task1.state()) {
                case SUCCESS -> switch (task2.state()) {
                    case SUCCESS -> {
                        var result = new CoordinatedResult<>(task1.get(), task2.get());
                        scope.logCompletion(true, null);
                        yield Result.success(result);
                    }
                    case FAILED -> {
                        scope.logCompletion(false, task2.exception());
                        yield Result.failure(PortfolioConcurrencyError.taskFailed(operationId + "-task2", task2.exception()));
                    }
                    case UNAVAILABLE -> {
                        scope.logCompletion(false, null);
                        yield Result.failure(PortfolioConcurrencyError.taskTimeout(operationId + "-task2", timeout));
                    }
                };
                case FAILED -> {
                    scope.logCompletion(false, task1.exception());
                    yield Result.failure(PortfolioConcurrencyError.taskFailed(operationId + "-task1", task1.exception()));
                }
                case UNAVAILABLE -> {
                    scope.logCompletion(false, null);
                    yield Result.failure(PortfolioConcurrencyError.taskTimeout(operationId + "-task1", timeout));
                }
            };
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Parallel portfolio operations interrupted: {}", operationId, e);
            return Result.failure(PortfolioConcurrencyError.interrupted(operationId, e));
            
        } catch (TimeoutException e) {
            log.error("Parallel portfolio operations timeout: {} after {}", operationId, timeout, e);
            return Result.failure(PortfolioConcurrencyError.taskTimeout(operationId, timeout));
            
        } catch (Exception e) {
            log.error("Parallel portfolio operations failed: {}", operationId, e);
            return Result.failure(PortfolioConcurrencyError.taskFailed(operationId, e));
        }
    }
    
    /**
     * Log operation completion with metrics
     */
    private void logCompletion(boolean success, Throwable error) {
        Duration executionTime = Duration.between(startTime, Instant.now());
        
        if (success) {
            log.info("Portfolio operation completed successfully: {} in {}ms", 
                operationId, executionTime.toMillis());
        } else {
            log.error("Portfolio operation failed: {} after {}ms", 
                operationId, executionTime.toMillis(), error);
        }
        
        // Record operation metrics for monitoring
        recordOperationMetrics(success, executionTime, error);
    }
    
    /**
     * Record operation metrics for Prometheus monitoring
     */
    private void recordOperationMetrics(boolean success, Duration executionTime, Throwable error) {
        // Implement metrics recording using Micrometer
        // Counter for success/failure rates
        String status = success ? "success" : "failure";
        
        // In production, this would use Micrometer Counter and Timer
        log.debug("Operation metrics: operation={}, status={}, duration={}ms, error={}", 
            operationId, status, executionTime.toMillis(), error != null ? error.getClass().getSimpleName() : "none");
        
        // Performance monitoring thresholds
        if (executionTime.toMillis() > 1000) {
            log.warn("Slow portfolio operation detected: {} took {}ms", operationId, executionTime.toMillis());
        }
        
        if (!success && error != null) {
            log.error("Portfolio operation failure pattern: operation={}, error_type={}", 
                operationId, error.getClass().getSimpleName());
        }
    }
    
    /**
     * Result container for coordinated parallel operations
     */
    public record CoordinatedResult<T1, T2>(T1 result1, T2 result2) {}
    
    /**
     * Portfolio concurrency error types
     */
    public sealed interface PortfolioConcurrencyError {
        
        record TaskFailed(String operationId, String message, Throwable cause) 
            implements PortfolioConcurrencyError {}
            
        record TaskTimeout(String operationId, Duration timeout) 
            implements PortfolioConcurrencyError {}
            
        record TaskInterrupted(String operationId, String message) 
            implements PortfolioConcurrencyError {}
        
        static PortfolioConcurrencyError taskFailed(String operationId, Throwable cause) {
            return new TaskFailed(operationId, 
                "Portfolio operation failed: " + operationId, cause);
        }
        
        static PortfolioConcurrencyError taskTimeout(String operationId, Duration timeout) {
            return new TaskTimeout(operationId, timeout);
        }
        
        static PortfolioConcurrencyError interrupted(String operationId, InterruptedException cause) {
            return new TaskInterrupted(operationId, 
                "Portfolio operation interrupted: " + operationId);
        }
    }
}