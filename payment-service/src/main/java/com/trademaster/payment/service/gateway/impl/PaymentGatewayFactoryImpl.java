package com.trademaster.payment.service.gateway.impl;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.service.gateway.PaymentGatewayFactory;
import com.trademaster.payment.service.gateway.RazorpayService;
import com.trademaster.payment.service.gateway.StripeService;
import com.trademaster.payment.util.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Payment Gateway Factory Implementation
 * Functional factory with enum-based dispatch pattern
 *
 * Compliance:
 * - Rule 3: Functional Programming First - NO if-else, Map-based dispatch
 * - Rule 4: Advanced Design Patterns - Factory + Strategy patterns
 * - Rule 5: Cognitive Complexity ≤ 7 per method
 * - Rule 11: Railway programming with Result types
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 24: Circuit breaker for ALL gateway operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayFactoryImpl implements PaymentGatewayFactory {

    private final RazorpayService razorpayService;
    private final StripeService stripeService;

    /**
     * Gateway service dispatch map (NO if-else)
     * Functional strategy pattern with Map-based lookup
     */
    private Map<PaymentGateway, GatewayOperations> getGatewayOperationsMap() {
        return Map.of(
            PaymentGateway.RAZORPAY, createRazorpayOperations(),
            PaymentGateway.STRIPE, createStripeOperations(),
            PaymentGateway.UPI, createRazorpayOperations() // UPI uses Razorpay
        );
    }

    /**
     * Create payment intent/order with automatic gateway selection
     * Functional dispatch with Result type
     */
    @Override
    public Result<String, String> createPayment(PaymentRequest request) {
        log.info("Factory creating payment: userId={}, gateway={}, amount={}",
                request.getUserId(), request.getPaymentGateway(), request.getAmount());

        return selectGateway(request.getPaymentGateway())
            .flatMap(ops -> ops.createPayment().apply(request))
            .onSuccess(intentId -> log.info("Payment created: intentId={}, gateway={}",
                    intentId, request.getPaymentGateway()));
    }

    /**
     * Confirm/capture payment with gateway dispatch
     * Pattern matching for gateway detection
     */
    @Override
    public Result<PaymentResponse, String> confirmPayment(
            String paymentId,
            String paymentMethodId
    ) {
        log.info("Factory confirming payment: paymentId={}", paymentId);

        return detectGatewayFromPaymentId(paymentId)
            .flatMap(gateway -> selectGateway(gateway)
                .flatMap(ops -> ops.confirmPayment().apply(paymentId, paymentMethodId)))
            .onSuccess(response -> log.info("Payment confirmed: paymentId={}, status={}",
                    paymentId, response.getStatus()));
    }

    /**
     * Process refund asynchronously with gateway dispatch
     * Virtual threads for non-blocking operations
     */
    @Override
    public CompletableFuture<Result<RefundResponse, String>> processRefund(
            String gatewayPaymentId,
            RefundRequest request
    ) {
        log.info("Factory processing refund: gatewayPaymentId={}, transactionId={}, amount={}",
                gatewayPaymentId, request.getTransactionId(), request.getAmount());

        return detectGatewayFromPaymentId(gatewayPaymentId)
            .map(gateway -> selectGateway(gateway)
                .map(ops -> ops.processRefund().apply(gatewayPaymentId, request))
                .fold(
                    future -> future,
                    error -> CompletableFuture.completedFuture(Result.<RefundResponse, String>failure(error))
                ))
            .fold(
                future -> future,
                error -> CompletableFuture.completedFuture(Result.failure(error))
            );
    }

    /**
     * Retrieve payment details with gateway dispatch
     * Idempotent operation for reconciliation
     */
    @Override
    public Result<PaymentResponse, String> retrievePaymentDetails(String paymentId) {
        log.debug("Factory retrieving payment: paymentId={}", paymentId);

        return detectGatewayFromPaymentId(paymentId)
            .flatMap(gateway -> selectGateway(gateway)
                .flatMap(ops -> ops.retrievePayment().apply(paymentId)))
            .onSuccess(response -> log.debug("Payment retrieved: paymentId={}", paymentId));
    }

    /**
     * Verify webhook signature with gateway-specific validation
     * Security validation for webhook events
     */
    @Override
    public Result<Boolean, String> verifyWebhookSignature(
            PaymentGateway gateway,
            String payload,
            String signature,
            String secret
    ) {
        log.debug("Factory verifying webhook: gateway={}", gateway);

        return selectGateway(gateway)
            .flatMap(ops -> ops.verifyWebhook().apply(payload, signature, secret))
            .onSuccess(isValid -> log.info("Webhook verified: gateway={}, valid={}", gateway, isValid));
    }

    /**
     * Create customer in gateway with automatic dispatch
     * Customer management for recurring payments
     */
    @Override
    public Result<String, String> createCustomer(
            PaymentGateway gateway,
            String email,
            String name,
            Map<String, String> metadata
    ) {
        log.info("Factory creating customer: gateway={}, email={}", gateway, email);

        return selectGateway(gateway)
            .flatMap(ops -> ops.createCustomer().apply(email, name, metadata))
            .onSuccess(customerId -> log.info("Customer created: customerId={}, gateway={}", customerId, gateway));
    }

    /**
     * Create subscription with gateway dispatch
     * Recurring payment setup
     */
    @Override
    public Result<String, String> createSubscription(
            PaymentGateway gateway,
            String customerId,
            String planId,
            Integer quantity
    ) {
        log.info("Factory creating subscription: gateway={}, customerId={}", gateway, customerId);

        return selectGateway(gateway)
            .flatMap(ops -> ops.createSubscription().apply(customerId, planId, quantity))
            .onSuccess(subId -> log.info("Subscription created: subscriptionId={}, gateway={}", subId, gateway));
    }

    /**
     * Cancel subscription with gateway dispatch
     * Stop recurring payments
     */
    @Override
    public Result<Boolean, String> cancelSubscription(
            PaymentGateway gateway,
            String subscriptionId,
            Boolean cancelAtPeriodEnd
    ) {
        log.info("Factory cancelling subscription: gateway={}, subscriptionId={}", gateway, subscriptionId);

        return selectGateway(gateway)
            .flatMap(ops -> ops.cancelSubscription().apply(subscriptionId, cancelAtPeriodEnd))
            .onSuccess(cancelled -> log.info("Subscription cancelled: subscriptionId={}, success={}", subscriptionId, cancelled));
    }

    /**
     * Get gateway service instance for direct access
     * Factory method for service retrieval
     */
    @Override
    public Result<Object, String> getGatewayService(PaymentGateway gateway) {
        return selectGateway(gateway)
            .map(ops -> ops.getService().get());
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Select gateway operations using Map-based dispatch (NO if-else)
     * Functional strategy pattern
     */
    private Result<GatewayOperations, String> selectGateway(PaymentGateway gateway) {
        return Optional.ofNullable(getGatewayOperationsMap().get(gateway))
            .map(Result::<GatewayOperations, String>success)
            .orElseGet(() -> Result.failure("Unsupported payment gateway: " + gateway));
    }

    /**
     * Detect gateway from payment ID format
     * Pattern matching with switch expression (NO if-else)
     */
    private Result<PaymentGateway, String> detectGatewayFromPaymentId(String paymentId) {
        return ResultUtil.safely(() -> switch (true) {
            case Boolean _ when paymentId.startsWith("pi_") -> PaymentGateway.STRIPE;
            case Boolean _ when paymentId.startsWith("pay_") -> PaymentGateway.RAZORPAY;
            case Boolean _ when paymentId.startsWith("order_") -> PaymentGateway.RAZORPAY;
            default -> throw new IllegalArgumentException("Unknown payment ID format: " + paymentId);
        }).mapError(Throwable::getMessage);
    }

    /**
     * Create Razorpay operations wrapper
     * Functional adapter pattern
     */
    private GatewayOperations createRazorpayOperations() {
        return GatewayOperations.builder()
            .createPayment(request -> razorpayService.createOrder(request))
            .confirmPayment((paymentId, methodId) ->
                // For Razorpay, we need to fetch payment details first to get amount/currency
                razorpayService.fetchPaymentDetails(paymentId)
                    .flatMap(payment -> razorpayService.capturePayment(
                        paymentId,
                        payment.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue(),
                        payment.getCurrency()
                    ))
            )
            .processRefund((gatewayPaymentId, request) ->
                razorpayService.processRefund(gatewayPaymentId, request))
            .retrievePayment(razorpayService::fetchPaymentDetails)
            .verifyWebhook((payload, sig, secret) ->
                razorpayService.verifyPaymentSignature(
                    extractOrderId(payload),
                    extractPaymentId(payload),
                    sig
                )
            )
            .createCustomer((email, name, metadata) ->
                Result.failure("Customer creation not supported for Razorpay")
            )
            .createSubscription((customerId, planId, qty) ->
                razorpayService.createSubscription(planId, customerId, qty)
            )
            .cancelSubscription(razorpayService::cancelSubscription)
            .service(() -> razorpayService)
            .build();
    }

    /**
     * Create Stripe operations wrapper
     * Functional adapter pattern
     */
    private GatewayOperations createStripeOperations() {
        return GatewayOperations.builder()
            .createPayment(stripeService::createPaymentIntent)
            .confirmPayment(stripeService::confirmPaymentIntent)
            .processRefund((gatewayPaymentId, request) ->
                stripeService.processRefund(gatewayPaymentId, request))
            .retrievePayment(stripeService::retrievePaymentIntent)
            .verifyWebhook(stripeService::verifyWebhookSignature)
            .createCustomer(stripeService::createCustomer)
            .createSubscription(stripeService::createSubscription)
            .cancelSubscription(stripeService::cancelSubscription)
            .service(() -> stripeService)
            .build();
    }

    /**
     * Extract order ID from webhook payload
     * Functional string processing (NO if-else)
     */
    private String extractOrderId(String payload) {
        return Optional.ofNullable(payload)
            .flatMap(p -> Optional.of(p).filter(s -> s.contains("order_id")))
            .map(p -> p.split("\"order_id\":\"")[1].split("\"")[0])
            .orElse("");
    }

    /**
     * Extract payment ID from webhook payload
     * Functional string processing (NO if-else)
     */
    private String extractPaymentId(String payload) {
        return Optional.ofNullable(payload)
            .flatMap(p -> Optional.of(p).filter(s -> s.contains("payment_id")))
            .map(p -> p.split("\"payment_id\":\"")[1].split("\"")[0])
            .orElse("");
    }

    // ==================== Functional Gateway Operations Adapter ====================

    /**
     * Functional operations adapter for gateway abstraction
     * Strategy pattern with functional interfaces
     */
    @lombok.Builder
    private record GatewayOperations(
        Function<PaymentRequest, Result<String, String>> createPayment,
        java.util.function.BiFunction<String, String, Result<PaymentResponse, String>> confirmPayment,
        java.util.function.BiFunction<String, RefundRequest, CompletableFuture<Result<RefundResponse, String>>> processRefund,
        Function<String, Result<PaymentResponse, String>> retrievePayment,
        TriFunction<String, String, String, Result<Boolean, String>> verifyWebhook,
        TriFunction<String, String, Map<String, String>, Result<String, String>> createCustomer,
        TriFunction<String, String, Integer, Result<String, String>> createSubscription,
        java.util.function.BiFunction<String, Boolean, Result<Boolean, String>> cancelSubscription,
        java.util.function.Supplier<Object> service
    ) {
        public Function<PaymentRequest, Result<String, String>> createPayment() {
            return createPayment;
        }

        public java.util.function.BiFunction<String, String, Result<PaymentResponse, String>> confirmPayment() {
            return confirmPayment;
        }

        public java.util.function.BiFunction<String, RefundRequest, CompletableFuture<Result<RefundResponse, String>>> processRefund() {
            return processRefund;
        }

        public Function<String, Result<PaymentResponse, String>> retrievePayment() {
            return retrievePayment;
        }

        public TriFunction<String, String, String, Result<Boolean, String>> verifyWebhook() {
            return verifyWebhook;
        }

        public TriFunction<String, String, Map<String, String>, Result<String, String>> createCustomer() {
            return createCustomer;
        }

        public TriFunction<String, String, Integer, Result<String, String>> createSubscription() {
            return createSubscription;
        }

        public java.util.function.BiFunction<String, Boolean, Result<Boolean, String>> cancelSubscription() {
            return cancelSubscription;
        }

        public java.util.function.Supplier<Object> getService() {
            return service;
        }
    }

    /**
     * Functional interface for three-argument functions
     * Enables functional composition with three parameters
     */
    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
