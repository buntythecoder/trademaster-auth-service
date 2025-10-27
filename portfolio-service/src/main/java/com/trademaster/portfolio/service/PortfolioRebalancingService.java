package com.trademaster.portfolio.service;

import com.trademaster.portfolio.dto.RebalancingPlan;
import com.trademaster.portfolio.dto.TargetAllocation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Portfolio Rebalancing Service Interface
 *
 * Provides portfolio rebalancing with multiple strategies:
 * - Target allocation rebalancing
 * - Tax-aware rebalancing (minimize tax impact)
 * - Cost-minimizing rebalancing (minimize trading costs)
 * - Threshold-based rebalancing (rebalance only if deviation exceeds threshold)
 *
 * Performance Targets:
 * - Plan generation: <100ms
 * - Order execution: <500ms
 *
 * Rule #2: Interface Segregation - Focused interface for rebalancing
 * Rule #12: Virtual Threads - All async operations use CompletableFuture
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface PortfolioRebalancingService {

    /**
     * Generate rebalancing plan based on target allocations
     *
     * @param portfolioId Portfolio identifier
     * @param targetAllocations Target allocation for each symbol
     * @param strategy Rebalancing strategy (TAX_AWARE, COST_MINIMIZING, AGGRESSIVE)
     * @return Rebalancing plan with trade recommendations
     */
    CompletableFuture<RebalancingPlan> generateRebalancingPlan(
        Long portfolioId,
        List<TargetAllocation> targetAllocations,
        String strategy
    );

    /**
     * Execute rebalancing based on approved plan
     *
     * @param portfolioId Portfolio identifier
     * @param plan Rebalancing plan to execute
     * @return Execution result with order IDs
     */
    CompletableFuture<RebalancingResult> executeRebalancing(
        Long portfolioId,
        RebalancingPlan plan
    );

    /**
     * Calculate current portfolio allocation
     *
     * @param portfolioId Portfolio identifier
     * @return Map of symbol to current allocation percentage
     */
    CompletableFuture<java.util.Map<String, java.math.BigDecimal>> getCurrentAllocation(
        Long portfolioId
    );

    /**
     * Validate target allocations sum to 100%
     *
     * @param targetAllocations Target allocations to validate
     * @return true if valid
     */
    boolean validateTargetAllocations(List<TargetAllocation> targetAllocations);

    /**
     * Rebalancing execution result
     */
    record RebalancingResult(
        String rebalancingId,
        Long portfolioId,
        String status,
        List<String> orderIds,
        java.math.BigDecimal actualCost,
        String message
    ) {}
}
