package com.trademaster.payment.entity;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.enums.PaymentStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Transaction Entity
 * 
 * Represents a payment transaction with gateway integration details.
 * Maintains audit trail and supports multiple payment gateways.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "subscription_id")
    private UUID subscriptionId;
    
    // Payment Details
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    // Gateway Integration
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false)
    private PaymentGateway paymentGateway;
    
    @Size(max = 255)
    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;
    
    @Size(max = 255)
    @Column(name = "gateway_order_id") 
    private String gatewayOrderId;
    
    @Size(max = 255)
    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;
    
    // Payment Method Details
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Type(JsonType.class)
    @Column(name = "payment_method_details", columnDefinition = "jsonb")
    private Map<String, Object> paymentMethodDetails;
    
    // Transaction Information
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Size(max = 100)
    @Column(name = "receipt_number")
    private String receiptNumber;
    
    // Failure Information
    @Size(max = 1000)
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Size(max = 50)
    @Column(name = "failure_code")
    private String failureCode;
    
    // Refund Information
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 8, fraction = 2)
    @Builder.Default
    @Column(name = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    @Size(max = 500)
    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;
    
    // Metadata and Response Data
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Type(JsonType.class)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        
        if (status == PaymentStatus.COMPLETED && processedAt == null) {
            processedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        
        if (status == PaymentStatus.COMPLETED && processedAt == null) {
            processedAt = updatedAt;
        }
    }
    
    // Helper methods
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
    
    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED;
    }
    
    public boolean canBeRefunded() {
        return isCompleted() && refundedAmount.compareTo(amount) < 0;
    }
    
    public BigDecimal getRemainingRefundAmount() {
        if (!canBeRefunded()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount);
    }
    
    public boolean isPartiallyRefunded() {
        return refundedAmount.compareTo(BigDecimal.ZERO) > 0 && 
               refundedAmount.compareTo(amount) < 0;
    }
    
    public boolean isFullyRefunded() {
        return refundedAmount.compareTo(amount) >= 0;
    }
    
    public String getDisplayAmount() {
        return String.format("%s %.2f", currency, amount);
    }
    
    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }
    
    public void markAsFailed(String reason, String code) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.failureCode = code;
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }
    
    public void addRefund(BigDecimal refundAmount, String reason) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        BigDecimal totalRefund = this.refundedAmount.add(refundAmount);
        if (totalRefund.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException("Total refund amount cannot exceed transaction amount");
        }
        
        this.refundedAmount = totalRefund;
        this.refundReason = reason;
        
        if (totalRefund.compareTo(this.amount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }
}