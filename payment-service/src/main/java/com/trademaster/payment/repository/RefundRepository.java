package com.trademaster.payment.repository;

import com.trademaster.payment.entity.Refund;
import com.trademaster.payment.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refund Repository - Data Access for Refund Operations
 * Provides queries for refund tracking, reconciliation, and analytics
 *
 * Compliance:
 * - Rule 3: Functional data access with Stream API integration
 * - Rule 20: Comprehensive test coverage required
 * - Financial Domain: Audit trail queries for regulatory compliance
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * Find all refunds for a specific transaction
     * Critical for refund reconciliation and displaying refund history
     *
     * @param transactionId Original payment transaction ID (UUID)
     * @return List of refunds (may be empty, never null)
     */
    List<Refund> findByTransactionId(UUID transactionId);

    /**
     * Find refund by gateway refund identifier
     * Used for webhook processing and reconciliation
     *
     * @param refundId Gateway-specific refund ID (rfnd_xxx, re_xxx)
     * @return Optional refund
     */
    Optional<Refund> findByRefundId(String refundId);

    /**
     * Find all refunds by status
     * Useful for processing pending/failed refunds
     *
     * @param status Refund status
     * @return List of refunds in specified status
     */
    List<Refund> findByStatus(RefundStatus status);

    /**
     * Find refunds by transaction ID and status
     * Specific query for refund state validation
     *
     * @param transactionId Transaction ID (UUID)
     * @param status Refund status
     * @return List of matching refunds
     */
    List<Refund> findByTransactionIdAndStatus(UUID transactionId, RefundStatus status);

    /**
     * Calculate total refunded amount for a transaction
     * Critical for validating refund limits (can't refund more than transaction amount)
     *
     * @param transactionId Transaction ID (UUID)
     * @return Total refunded amount (BigDecimal for precision)
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r " +
           "WHERE r.transactionId = :transactionId AND r.status = 'SUCCESS'")
    BigDecimal calculateTotalRefundedAmount(@Param("transactionId") UUID transactionId);

    /**
     * Find refunds created within date range
     * Required for financial reporting and analytics
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of refunds in date range
     */
    @Query("SELECT r FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.createdAt DESC")
    List<Refund> findRefundsByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count refunds by status within date range
     * Analytics query for refund metrics
     *
     * @param status Refund status
     * @param startDate Start date
     * @param endDate End date
     * @return Count of refunds
     */
    @Query("SELECT COUNT(r) FROM Refund r " +
           "WHERE r.status = :status AND r.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(
        @Param("status") RefundStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find recent refunds for monitoring and support
     *
     * @param limit Maximum number of refunds to return
     * @return List of most recent refunds
     */
    @Query("SELECT r FROM Refund r ORDER BY r.createdAt DESC LIMIT :limit")
    List<Refund> findRecentRefunds(@Param("limit") int limit);
}
