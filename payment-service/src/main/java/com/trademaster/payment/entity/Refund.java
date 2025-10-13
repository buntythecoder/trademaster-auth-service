package com.trademaster.payment.entity;

import com.trademaster.payment.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refund Entity - Financial Refund Processing
 * Tracks all refund operations with audit trail and status management
 *
 * Compliance:
 * - Rule 9: Entity with proper Lombok annotations
 * - Rule 16: No hardcoded values
 * - Rule 20: Immutable creation patterns with Builder
 *
 * Financial Domain Requirements:
 * - Audit trail: processedAt, createdAt for regulatory compliance
 * - Status tracking: RefundStatus enum for state machine
 * - Amount precision: BigDecimal for financial calculations
 * - Gateway integration: gatewayRefundId for reconciliation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_refund_status", columnList = "status"),
    @Index(name = "idx_refund_gateway_id", columnList = "gateway_refund_id"),
    @Index(name = "idx_refund_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to original payment transaction (UUID)
     * Required for refund reconciliation and audit trail
     */
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    /**
     * Gateway-specific refund identifier (Razorpay: rfnd_xxx, Stripe: re_xxx)
     * Used for reconciliation with payment gateway
     */
    @Column(name = "gateway_refund_id", length = 100)
    private String refundId;

    /**
     * Refund amount in original transaction currency
     * Precision: 19 digits with 4 decimal places for sub-currency units
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217: USD, INR, EUR)
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Current refund status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefundStatus status;

    /**
     * Reason for refund request
     * Important for compliance and customer service
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Timestamp when refund was successfully processed
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Audit trail: Record creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit trail: Record last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Initiated by user ID for audit trail
     */
    @Column(name = "initiated_by")
    private Long initiatedBy;

    /**
     * Internal notes for operations team
     */
    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /**
     * JPA lifecycle callbacks for audit timestamps
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
