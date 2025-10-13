package com.trademaster.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Payment Analytics DTO - Financial Metrics and Reporting
 * Aggregated payment data for business intelligence and monitoring
 *
 * Compliance:
 * - Rule 9: Immutable DTO with Builder pattern
 * - Rule 16: No hardcoded values, all calculated from data
 * - Financial Domain: BigDecimal for precise financial calculations
 *
 * Usage:
 * - Real-time dashboards
 * - Financial reporting
 * - Performance monitoring
 * - Business intelligence
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAnalytics {

    /**
     * Total number of payment transactions in the period
     */
    private Integer totalTransactions;

    /**
     * Number of successfully completed payments
     */
    private Integer successfulTransactions;

    /**
     * Number of failed payment attempts
     */
    private Integer failedTransactions;

    /**
     * Number of pending/processing payments
     */
    private Integer pendingTransactions;

    /**
     * Total payment amount processed (successful only)
     * Precision: 19 digits with 4 decimal places
     */
    private BigDecimal totalAmount;

    /**
     * Average transaction amount
     * Calculated: totalAmount / successfulTransactions
     */
    private BigDecimal averageTransactionAmount;

    /**
     * Success rate percentage (0-100)
     * Calculated: (successfulTransactions / totalTransactions) * 100
     */
    private Double successRate;

    /**
     * Failure rate percentage (0-100)
     * Calculated: (failedTransactions / totalTransactions) * 100
     */
    private Double failureRate;

    /**
     * Total refunded amount in the period
     */
    private BigDecimal totalRefunded;

    /**
     * Net revenue (totalAmount - totalRefunded)
     */
    private BigDecimal netRevenue;

    /**
     * Analysis start date
     */
    private LocalDateTime periodStart;

    /**
     * Analysis end date
     */
    private LocalDateTime periodEnd;

    /**
     * Currency code for amounts (ISO 4217)
     */
    private String currency;

    /**
     * Calculate success rate if not already set
     * Formula: (successful / total) * 100
     *
     * @return Success rate percentage
     */
    public Double getSuccessRate() {
        if (successRate != null) {
            return successRate;
        }

        if (totalTransactions == null || totalTransactions == 0) {
            return 0.0;
        }

        if (successfulTransactions == null) {
            return 0.0;
        }

        return (successfulTransactions.doubleValue() / totalTransactions.doubleValue()) * 100.0;
    }

    /**
     * Calculate failure rate if not already set
     * Formula: (failed / total) * 100
     *
     * @return Failure rate percentage
     */
    public Double getFailureRate() {
        if (failureRate != null) {
            return failureRate;
        }

        if (totalTransactions == null || totalTransactions == 0) {
            return 0.0;
        }

        if (failedTransactions == null) {
            return 0.0;
        }

        return (failedTransactions.doubleValue() / totalTransactions.doubleValue()) * 100.0;
    }

    /**
     * Calculate average transaction amount if not already set
     * Formula: totalAmount / successfulTransactions
     *
     * @return Average transaction amount
     */
    public BigDecimal getAverageTransactionAmount() {
        if (averageTransactionAmount != null) {
            return averageTransactionAmount;
        }

        if (totalAmount == null || successfulTransactions == null || successfulTransactions == 0) {
            return BigDecimal.ZERO;
        }

        return totalAmount.divide(
            BigDecimal.valueOf(successfulTransactions),
            2,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Calculate net revenue if not already set
     * Formula: totalAmount - totalRefunded
     *
     * @return Net revenue
     */
    public BigDecimal getNetRevenue() {
        if (netRevenue != null) {
            return netRevenue;
        }

        BigDecimal amount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal refunded = totalRefunded != null ? totalRefunded : BigDecimal.ZERO;

        return amount.subtract(refunded);
    }
}
