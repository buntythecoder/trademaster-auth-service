package com.trademaster.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Rebalancing Result DTO
 *
 * Comprehensive result of portfolio rebalancing operation with execution details.
 * Used to track rebalancing progress, costs, and outcomes.
 *
 * Rule #9: Immutable Records for Data Transfer Objects
 * Rule #3: Functional Programming - Immutable data structures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public record RebalancingResult(
    String rebalancingId,
    Long portfolioId,
    String strategy,
    RebalancingStatus status,
    BigDecimal totalValue,
    Integer ordersCreated,
    BigDecimal estimatedCosts,
    BigDecimal actualCosts,
    List<String> orderIds,
    String statusMessage,
    Instant initiatedAt,
    Instant completedAt
) {

    /**
     * Rebalancing status enumeration
     *
     * Rule #3: Functional enum with pattern matching support
     */
    public enum RebalancingStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Factory method for initiated rebalancing
     *
     * Rule #4: Factory Pattern with functional construction
     */
    public static RebalancingResult initiated(
            String rebalancingId,
            Long portfolioId,
            String strategy,
            BigDecimal totalValue) {
        return new RebalancingResult(
            rebalancingId,
            portfolioId,
            strategy,
            RebalancingStatus.INITIATED,
            totalValue,
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            "Rebalancing initiated successfully",
            Instant.now(),
            null
        );
    }

    /**
     * Factory method for completed rebalancing
     *
     * Rule #4: Factory Pattern with functional construction
     */
    public static RebalancingResult completed(
            String rebalancingId,
            Long portfolioId,
            String strategy,
            BigDecimal totalValue,
            Integer ordersCreated,
            BigDecimal actualCosts,
            List<String> orderIds) {
        return new RebalancingResult(
            rebalancingId,
            portfolioId,
            strategy,
            RebalancingStatus.COMPLETED,
            totalValue,
            ordersCreated,
            actualCosts,
            actualCosts,
            orderIds,
            "Rebalancing completed successfully",
            Instant.now().minusSeconds(60), // Approximate start time
            Instant.now()
        );
    }

    /**
     * Factory method for failed rebalancing
     *
     * Rule #4: Factory Pattern with functional construction
     */
    public static RebalancingResult failed(
            String rebalancingId,
            Long portfolioId,
            String strategy,
            String errorMessage) {
        return new RebalancingResult(
            rebalancingId,
            portfolioId,
            strategy,
            RebalancingStatus.FAILED,
            BigDecimal.ZERO,
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            errorMessage,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Check if rebalancing is complete
     *
     * Rule #3: Functional predicate method
     */
    public boolean isComplete() {
        return status == RebalancingStatus.COMPLETED ||
               status == RebalancingStatus.FAILED ||
               status == RebalancingStatus.CANCELLED;
    }

    /**
     * Check if rebalancing was successful
     *
     * Rule #3: Functional predicate method
     */
    public boolean isSuccessful() {
        return status == RebalancingStatus.COMPLETED;
    }

    /**
     * Get execution duration in seconds
     *
     * Rule #3: Functional calculation method
     */
    public Long getExecutionDurationSeconds() {
        return (completedAt != null && initiatedAt != null)
            ? completedAt.getEpochSecond() - initiatedAt.getEpochSecond()
            : 0L;
    }
}
