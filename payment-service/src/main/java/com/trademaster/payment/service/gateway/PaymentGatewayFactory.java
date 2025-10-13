package com.trademaster.payment.service.gateway;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.enums.PaymentGateway;

import java.util.concurrent.CompletableFuture;

/**
 * Payment Gateway Factory Interface
 * Functional factory pattern for gateway selection and operations
 *
 * Compliance:
 * - Rule 2: Interface Segregation - focused factory interface
 * - Rule 4: Advanced Design Patterns - Factory pattern
 * - Rule 11: Railway programming with Result types
 * - Rule 24: Circuit breaker for ALL operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface PaymentGatewayFactory {

    /**
     * Create payment intent/order with automatic gateway selection
     * Functional factory dispatch based on request parameters
     *
     * @param request Payment request with gateway selection criteria
     * @return Result containing payment intent ID or error
     */
    Result<String, String> createPayment(PaymentRequest request);

    /**
     * Confirm/capture payment with gateway dispatch
     * Routes to appropriate gateway based on payment ID format
     *
     * @param paymentId Payment intent/order ID
     * @param paymentMethodId Payment method identifier
     * @return Result containing payment confirmation or error
     */
    Result<PaymentResponse, String> confirmPayment(
        String paymentId,
        String paymentMethodId
    );

    /**
     * Process refund asynchronously with gateway dispatch
     * Virtual threads for non-blocking operations
     *
     * @param gatewayPaymentId Gateway payment ID (pay_xxx, pi_xxx)
     * @param request Refund request with amount and reason
     * @return CompletableFuture containing Result
     */
    CompletableFuture<Result<RefundResponse, String>> processRefund(
        String gatewayPaymentId,
        RefundRequest request
    );

    /**
     * Retrieve payment details with gateway dispatch
     * Idempotent operation for reconciliation
     *
     * @param paymentId Payment intent/order ID
     * @return Result containing payment details or error
     */
    Result<PaymentResponse, String> retrievePaymentDetails(String paymentId);

    /**
     * Verify webhook signature with gateway-specific validation
     * Security validation for webhook events
     *
     * @param gateway Payment gateway enum
     * @param payload Webhook payload
     * @param signature Webhook signature header
     * @param secret Webhook secret key
     * @return Result containing validation status
     */
    Result<Boolean, String> verifyWebhookSignature(
        PaymentGateway gateway,
        String payload,
        String signature,
        String secret
    );

    /**
     * Create customer in gateway with automatic dispatch
     * Customer management for recurring payments
     *
     * @param gateway Payment gateway enum
     * @param email Customer email
     * @param name Customer name
     * @param metadata Additional customer data
     * @return Result containing customer ID or error
     */
    Result<String, String> createCustomer(
        PaymentGateway gateway,
        String email,
        String name,
        java.util.Map<String, String> metadata
    );

    /**
     * Create subscription with gateway dispatch
     * Recurring payment setup
     *
     * @param gateway Payment gateway enum
     * @param customerId Gateway customer ID
     * @param planId Gateway plan ID
     * @param quantity Subscription quantity
     * @return Result containing subscription ID or error
     */
    Result<String, String> createSubscription(
        PaymentGateway gateway,
        String customerId,
        String planId,
        Integer quantity
    );

    /**
     * Cancel subscription with gateway dispatch
     * Stop recurring payments
     *
     * @param gateway Payment gateway enum
     * @param subscriptionId Gateway subscription ID
     * @param cancelAtPeriodEnd Cancel at period end or immediately
     * @return Result containing cancellation confirmation
     */
    Result<Boolean, String> cancelSubscription(
        PaymentGateway gateway,
        String subscriptionId,
        Boolean cancelAtPeriodEnd
    );

    /**
     * Get gateway service instance for direct access
     * Factory method for service retrieval
     *
     * @param gateway Payment gateway enum
     * @return Gateway service instance wrapped in Result
     */
    Result<Object, String> getGatewayService(PaymentGateway gateway);
}
