package com.trademaster.payment.repository;

import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Transaction Repository
 * 
 * Data access layer for payment transactions with custom queries
 * for analytics and reporting.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Find all transactions for a specific user
     */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Find transactions by status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);
    
    /**
     * Find transaction by gateway payment ID
     */
    Optional<PaymentTransaction> findByGatewayPaymentId(String gatewayPaymentId);
    
    /**
     * Find transaction by gateway order ID
     */
    Optional<PaymentTransaction> findByGatewayOrderId(String gatewayOrderId);
    
    /**
     * Find transactions for a subscription
     */
    List<PaymentTransaction> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
    
    /**
     * Find pending transactions older than specified time
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status IN :statuses AND p.createdAt < :cutoffTime")
    List<PaymentTransaction> findStaleTransactions(
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("cutoffTime") Instant cutoffTime
    );
    
    /**
     * Get payment statistics for a user
     */
    @Query("SELECT " +
           "COUNT(p) as totalTransactions, " +
           "SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END) as totalPaid, " +
           "COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE NULL END) as successfulTransactions, " +
           "COUNT(CASE WHEN p.status = 'FAILED' THEN 1 ELSE NULL END) as failedTransactions " +
           "FROM PaymentTransaction p WHERE p.userId = :userId")
    Object[] getPaymentStatistics(@Param("userId") UUID userId);
    
    /**
     * Find recent transactions for dashboard
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<PaymentTransaction> findRecentTransactions(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Count transactions by status for monitoring
     */
    @Query("SELECT p.status, COUNT(p) FROM PaymentTransaction p GROUP BY p.status")
    List<Object[]> countTransactionsByStatus();
    
    /**
     * Find transactions requiring reconciliation
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE " +
           "p.status = 'PROCESSING' AND p.createdAt < :cutoffTime OR " +
           "p.status = 'PENDING' AND p.createdAt < :pendingCutoffTime")
    List<PaymentTransaction> findTransactionsRequiringReconciliation(
            @Param("cutoffTime") Instant cutoffTime,
            @Param("pendingCutoffTime") Instant pendingCutoffTime
    );
    
    /**
     * Find transactions by user and date range
     */
    List<PaymentTransaction> findByUserIdAndCreatedAtBetween(
            UUID userId, Instant fromDate, Instant toDate);
}