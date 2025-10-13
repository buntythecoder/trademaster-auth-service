package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Processing Service Interface
 * Functional interface for payment orchestration and processing
 *
 * Compliance:
 * - Rule 2: Interface Segregation - focused interface for payment operations
 * - Rule 3: Functional Programming - Result types, CompletableFuture
 * - Rule 11: Railway programming with Result types
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 24: Circuit breaker protection through gateway factory
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface PaymentProcessingService {

    /**
     * Process payment request asynchronously with virtual threads
     * Railway programming with Result type for error handling
     *
     * @param request Payment request with gateway and amount details
     * @return CompletableFuture containing Result with payment response or error
     */
    CompletableFuture<Result<PaymentResponse, String>> processPayment(PaymentRequest request);

    /**
     * Confirm payment after initial authorization
     * Used for two-step payment flows
     *
     * @param transactionId Payment transaction ID
     * @param paymentMethodId Gateway payment method ID
     * @return CompletableFuture containing Result with confirmation or error
     */
    CompletableFuture<Result<PaymentResponse, String>> confirmPayment(
        UUID transactionId,
        String paymentMethodId
    );

    /**
     * Process refund asynchronously with virtual threads
     * Functional refund processing with validation
     *
     * @param request Refund request details
     * @return CompletableFuture containing Result with refund response or error
     */
    CompletableFuture<Result<RefundResponse, String>> processRefund(RefundRequest request);

    /**
     * Get transaction details by ID
     * Idempotent read operation
     *
     * @param transactionId Transaction ID
     * @return Result containing transaction or error
     */
    Result<PaymentTransaction, String> getTransaction(UUID transactionId);

    /**
     * Get payment status by transaction ID
     * Lightweight status check
     *
     * @param transactionId Transaction ID
     * @return Result containing payment status or error
     */
    Result<PaymentStatus, String> getPaymentStatus(UUID transactionId);

    /**
     * Get payment history for user with pagination
     * Functional pagination support
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Result containing paginated transactions or error
     */
    Result<Page<PaymentTransaction>, String> getPaymentHistory(
        UUID userId,
        Pageable pageable
    );

    /**
     * Get transaction by gateway payment ID
     * Used for webhook processing and reconciliation
     *
     * @param gatewayPaymentId Gateway-specific payment ID
     * @return Result containing transaction or error
     */
    Result<PaymentTransaction, String> getTransactionByGatewayPaymentId(
        String gatewayPaymentId
    );

    /**
     * Update transaction status from webhook
     * Functional status update with audit trail
     *
     * @param transactionId Transaction ID
     * @param newStatus New payment status
     * @param gatewayResponse Gateway response data
     * @return Result containing updated transaction or error
     */
    Result<PaymentTransaction, String> updateTransactionStatus(
        UUID transactionId,
        PaymentStatus newStatus,
        java.util.Map<String, Object> gatewayResponse
    );

    /**
     * Retry failed payment
     * Functional retry with exponential backoff
     *
     * @param transactionId Failed transaction ID
     * @return CompletableFuture containing Result with retry response or error
     */
    CompletableFuture<Result<PaymentResponse, String>> retryPayment(UUID transactionId);
}
