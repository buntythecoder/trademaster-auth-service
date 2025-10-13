package com.trademaster.payment.service.gateway;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Stripe Payment Gateway Service Interface
 * Functional interface for international market payment processing
 *
 * Compliance:
 * - Rule 2: Interface Segregation - focused interface
 * - Rule 3: Functional Programming - no if-else, Result types
 * - Rule 11: Railway programming with Result
 * - Rule 24: Circuit breaker for ALL external calls
 */
public interface StripeService {

    /**
     * Create payment intent in Stripe
     * Returns Result for functional error handling
     *
     * @param request Payment request details
     * @return Result containing payment intent ID or error
     */
    Result<String, String> createPaymentIntent(PaymentRequest request);

    /**
     * Confirm payment intent
     * Two-step payment flow: create then confirm
     *
     * @param paymentIntentId Stripe payment intent ID
     * @param paymentMethodId Stripe payment method ID
     * @return Result containing confirmation status
     */
    Result<PaymentResponse, String> confirmPaymentIntent(
        String paymentIntentId,
        String paymentMethodId
    );

    /**
     * Capture authorized payment intent
     * For separate authorization and capture flows
     *
     * @param paymentIntentId Stripe payment intent ID
     * @return Result containing capture confirmation
     */
    Result<PaymentResponse, String> capturePaymentIntent(String paymentIntentId);

    /**
     * Process refund asynchronously with virtual threads
     * CompletableFuture for async operations
     *
     * @param gatewayPaymentId Stripe payment intent ID (pi_xxx)
     * @param request Refund request with amount and reason
     * @return CompletableFuture containing Result
     */
    CompletableFuture<Result<RefundResponse, String>> processRefund(
        String gatewayPaymentId,
        RefundRequest request
    );

    /**
     * Retrieve payment intent details from Stripe
     * Idempotent operation for reconciliation
     *
     * @param paymentIntentId Stripe payment intent ID
     * @return Result containing payment details
     */
    Result<PaymentResponse, String> retrievePaymentIntent(String paymentIntentId);

    /**
     * Create customer in Stripe
     * Customer management for recurring payments
     *
     * @param email Customer email
     * @param name Customer name
     * @param metadata Additional customer data
     * @return Result containing customer ID
     */
    Result<String, String> createCustomer(
        String email,
        String name,
        java.util.Map<String, String> metadata
    );

    /**
     * Create subscription in Stripe
     * Recurring payment setup with pricing
     *
     * @param customerId Stripe customer ID
     * @param priceId Stripe price ID
     * @param quantity Subscription quantity
     * @return Result containing subscription ID
     */
    Result<String, String> createSubscription(
        String customerId,
        String priceId,
        Integer quantity
    );

    /**
     * Cancel subscription in Stripe
     * Stop recurring payments
     *
     * @param subscriptionId Stripe subscription ID
     * @param cancelAtPeriodEnd Cancel at period end or immediately
     * @return Result containing cancellation confirmation
     */
    Result<Boolean, String> cancelSubscription(
        String subscriptionId,
        Boolean cancelAtPeriodEnd
    );

    /**
     * Verify webhook signature for security
     * Stripe webhook signature validation
     *
     * @param payload Webhook payload
     * @param signature Stripe signature header
     * @param secret Webhook secret
     * @return Result containing validation status
     */
    Result<Boolean, String> verifyWebhookSignature(
        String payload,
        String signature,
        String secret
    );

    /**
     * Create setup intent for saving payment methods
     * For future payments without immediate charge
     *
     * @param customerId Stripe customer ID
     * @return Result containing setup intent client secret
     */
    Result<String, String> createSetupIntent(String customerId);
}
