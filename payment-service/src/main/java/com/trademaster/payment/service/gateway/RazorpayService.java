package com.trademaster.payment.service.gateway;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Razorpay Payment Gateway Service Interface
 * Functional interface for Indian market payment processing
 *
 * Compliance:
 * - Rule 2: Interface Segregation - focused interface
 * - Rule 3: Functional Programming - no if-else, Result types
 * - Rule 11: Railway programming with Result
 * - Rule 24: Circuit breaker for ALL external calls
 */
public interface RazorpayService {

    /**
     * Create payment order in Razorpay
     * Returns Result for functional error handling
     *
     * @param request Payment request details
     * @return Result containing order ID or error
     */
    Result<String, String> createOrder(PaymentRequest request);

    /**
     * Verify payment signature for security
     * Razorpay webhook signature validation
     *
     * @param orderId Razorpay order ID
     * @param paymentId Razorpay payment ID
     * @param signature Razorpay signature
     * @return Result containing validation status
     */
    Result<Boolean, String> verifyPaymentSignature(
        String orderId,
        String paymentId,
        String signature
    );

    /**
     * Capture authorized payment
     * Two-step payment flow: authorize then capture
     *
     * @param paymentId Razorpay payment ID
     * @param amount Amount to capture
     * @param currency Currency code
     * @return Result containing capture confirmation
     */
    Result<PaymentResponse, String> capturePayment(
        String paymentId,
        Long amount,
        String currency
    );

    /**
     * Process refund asynchronously with virtual threads
     * CompletableFuture for async operations
     *
     * @param gatewayPaymentId Razorpay payment ID (pay_xxx)
     * @param request Refund request with amount and reason
     * @return CompletableFuture containing Result
     */
    CompletableFuture<Result<RefundResponse, String>> processRefund(
        String gatewayPaymentId,
        RefundRequest request
    );

    /**
     * Fetch payment details from Razorpay
     * Idempotent operation for reconciliation
     *
     * @param paymentId Razorpay payment ID
     * @return Result containing payment details
     */
    Result<PaymentResponse, String> fetchPaymentDetails(String paymentId);

    /**
     * Create subscription in Razorpay
     * Recurring payment setup
     *
     * @param planId Razorpay plan ID
     * @param customerId Customer ID
     * @param totalCount Billing cycles
     * @return Result containing subscription ID
     */
    Result<String, String> createSubscription(
        String planId,
        String customerId,
        Integer totalCount
    );

    /**
     * Cancel subscription in Razorpay
     * Stop recurring payments
     *
     * @param subscriptionId Razorpay subscription ID
     * @param cancelAtCycleEnd Cancel at period end or immediately
     * @return Result containing cancellation confirmation
     */
    Result<Boolean, String> cancelSubscription(
        String subscriptionId,
        Boolean cancelAtCycleEnd
    );
}
