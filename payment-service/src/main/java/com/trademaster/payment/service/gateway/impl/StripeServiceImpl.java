package com.trademaster.payment.service.gateway.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.gateway.StripeService;
import com.trademaster.payment.util.CircuitBreakerUtil;
import com.trademaster.payment.util.ResultUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Stripe Payment Gateway Implementation
 * Functional implementation with circuit breakers and Railway programming
 *
 * Compliance:
 * - Rule 3: Functional Programming First - NO if-else, NO loops, NO try-catch
 * - Rule 11: Railway programming with Result types
 * - Rule 24: Circuit breaker for ALL external calls
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 10: @Slf4j for structured logging
 * - Rule 14: Pattern matching with switch expressions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements StripeService {

    private static final String CIRCUIT_BREAKER_NAME = "stripe-service";

    private final CircuitBreakerUtil circuitBreakerUtil;

    @Value("${payment.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void initializeStripe() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API initialized successfully");
    }

    /**
     * Create payment intent with circuit breaker protection
     * Functional approach: Result type, no try-catch
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createPaymentIntent(PaymentRequest request) {
        log.info("Creating Stripe payment intent: userId={}, amount={}",
                request.getUserId(), request.getAmount());

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> buildPaymentIntentParams(request)
                .flatMap(this::executePaymentIntentCreation)
                .map(PaymentIntent::getId)
                .onSuccess(intentId -> log.info("Stripe payment intent created: intentId={}", intentId))
                .getOrElse(null)
        ).map(value -> Result.<String, String>success(value))
         .orElseGet(() -> Result.failure("Failed to create Stripe payment intent"));
    }

    /**
     * Confirm payment intent with circuit breaker
     * Railway programming with flatMap chains
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<PaymentResponse, String> confirmPaymentIntent(
            String paymentIntentId,
            String paymentMethodId
    ) {
        log.info("Confirming Stripe payment intent: intentId={}", paymentIntentId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> buildConfirmParams(paymentMethodId)
                .flatMap(params -> retrieveAndConfirmIntent(paymentIntentId, params))
                .map(this::mapToPaymentResponse)
                .onSuccess(response -> log.info("Payment intent confirmed: intentId={}, status={}",
                        paymentIntentId, response.getStatus()))
                .getOrElse(null)
        ).map(value -> Result.<PaymentResponse, String>success(value))
         .orElseGet(() -> Result.failure("Failed to confirm payment intent"));
    }

    /**
     * Capture authorized payment intent
     * Functional pattern for two-step payment flows
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<PaymentResponse, String> capturePaymentIntent(String paymentIntentId) {
        log.info("Capturing Stripe payment intent: intentId={}", paymentIntentId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> retrievePaymentIntentById(paymentIntentId)
                .flatMap(this::executeCaptureIntent)
                .map(this::mapToPaymentResponse)
                .onSuccess(response -> log.info("Payment intent captured: intentId={}", paymentIntentId))
                .getOrElse(null)
        ).map(value -> Result.<PaymentResponse, String>success(value))
         .orElseGet(() -> Result.failure("Failed to capture payment intent"));
    }

    /**
     * Process refund asynchronously with virtual threads
     * CompletableFuture for non-blocking operations
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Result<RefundResponse, String>> processRefund(
            String gatewayPaymentId,
            RefundRequest request
    ) {
        log.info("Processing Stripe refund: gatewayPaymentId={}, transactionId={}, amount={}",
                gatewayPaymentId, request.getTransactionId(), request.getAmount());

        return CompletableFuture.supplyAsync(() ->
            circuitBreakerUtil.executeWithCircuitBreaker(
                CIRCUIT_BREAKER_NAME,
                () -> buildRefundParams(gatewayPaymentId, request)
                    .flatMap(this::executeRefundCreation)
                    .map(this::mapToRefundResponse)
                    .onSuccess(response -> log.info("Refund processed: refundId={}, status={}",
                            response.getRefundId(), response.getStatus()))
                    .getOrElse(null)
            ).map(value -> Result.<RefundResponse, String>success(value))
             .orElseGet(() -> Result.failure("Failed to process refund"))
        );
    }

    /**
     * Retrieve payment intent details with circuit breaker
     * Idempotent operation for reconciliation
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<PaymentResponse, String> retrievePaymentIntent(String paymentIntentId) {
        log.debug("Retrieving Stripe payment intent: intentId={}", paymentIntentId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> retrievePaymentIntentById(paymentIntentId)
                .map(this::mapToPaymentResponse)
                .onSuccess(response -> log.debug("Payment intent retrieved: intentId={}", paymentIntentId))
                .getOrElse(null)
        ).map(value -> Result.<PaymentResponse, String>success(value))
         .orElseGet(() -> Result.failure("Failed to retrieve payment intent"));
    }

    /**
     * Create customer with circuit breaker
     * Customer management for recurring payments
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createCustomer(
            String email,
            String name,
            Map<String, String> metadata
    ) {
        log.info("Creating Stripe customer: email={}", email);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> buildCustomerParams(email, name, metadata)
                .flatMap(this::executeCustomerCreation)
                .map(Customer::getId)
                .onSuccess(customerId -> log.info("Stripe customer created: customerId={}", customerId))
                .getOrElse(null)
        ).map(value -> Result.<String, String>success(value))
         .orElseGet(() -> Result.failure("Failed to create Stripe customer"));
    }

    /**
     * Create subscription with circuit breaker
     * Recurring payment setup
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createSubscription(
            String customerId,
            String priceId,
            Integer quantity
    ) {
        log.info("Creating Stripe subscription: customerId={}, priceId={}", customerId, priceId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> buildSubscriptionParams(customerId, priceId, quantity)
                .flatMap(this::executeSubscriptionCreation)
                .map(Subscription::getId)
                .onSuccess(subId -> log.info("Subscription created: subscriptionId={}", subId))
                .getOrElse(null)
        ).map(value -> Result.<String, String>success(value))
         .orElseGet(() -> Result.failure("Failed to create subscription"));
    }

    /**
     * Cancel subscription with circuit breaker
     * Stop recurring payments
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<Boolean, String> cancelSubscription(
            String subscriptionId,
            Boolean cancelAtPeriodEnd
    ) {
        log.info("Cancelling Stripe subscription: subscriptionId={}, cancelAtEnd={}",
                subscriptionId, cancelAtPeriodEnd);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> retrieveSubscription(subscriptionId)
                .flatMap(sub -> executeCancellation(sub, cancelAtPeriodEnd))
                .map(sub -> "canceled".equals(sub.getStatus()))
                .onSuccess(cancelled -> log.info("Subscription cancelled: subscriptionId={}, success={}",
                        subscriptionId, cancelled))
                .getOrElse(false)
        ).map(value -> Result.<Boolean, String>success(value))
         .orElseGet(() -> Result.failure("Failed to cancel subscription"));
    }

    /**
     * Verify webhook signature using Stripe SDK
     * Functional security validation
     */
    @Override
    public Result<Boolean, String> verifyWebhookSignature(
            String payload,
            String signature,
            String secret
    ) {
        log.debug("Verifying Stripe webhook signature");

        return ResultUtil.tryExecute(() ->
            Webhook.constructEvent(payload, signature, secret) != null
        ).map(Result::<Boolean, String>success)
         .getOrElse(Result.failure("Webhook signature verification failed"));
    }

    /**
     * Create setup intent for saving payment methods
     * For future payments without immediate charge
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createSetupIntent(String customerId) {
        log.info("Creating Stripe setup intent: customerId={}", customerId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> buildSetupIntentParams(customerId)
                .flatMap(this::executeSetupIntentCreation)
                .map(SetupIntent::getClientSecret)
                .onSuccess(secret -> log.info("Setup intent created for customer: {}", customerId))
                .getOrElse(null)
        ).map(value -> Result.<String, String>success(value))
         .orElseGet(() -> Result.failure("Failed to create setup intent"));
    }

    // ==================== Private Helper Methods (Functional) ====================

    private Result<PaymentIntentCreateParams, String> buildPaymentIntentParams(
            PaymentRequest request
    ) {
        return ResultUtil.safely(() ->
            PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency(request.getCurrency().toLowerCase())
                .setDescription("Payment for subscription")
                .putMetadata("user_id", request.getUserId().toString())
                .putMetadata("subscription_plan_id", request.getSubscriptionPlanId().toString())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build()
        ).mapError(Throwable::getMessage);
    }

    private Result<PaymentIntent, String> executePaymentIntentCreation(
            PaymentIntentCreateParams params
    ) {
        return ResultUtil.tryExecute(() ->
            PaymentIntent.create(params)
        ).mapError(Throwable::getMessage);
    }

    private Result<PaymentIntentConfirmParams, String> buildConfirmParams(
            String paymentMethodId
    ) {
        return ResultUtil.safely(() ->
            PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .setReturnUrl("https://trademaster.app/payment/success")
                .build()
        ).mapError(Throwable::getMessage);
    }

    private Result<PaymentIntent, String> retrievePaymentIntentById(String paymentIntentId) {
        return ResultUtil.tryExecute(() ->
            PaymentIntent.retrieve(paymentIntentId)
        ).mapError(Throwable::getMessage);
    }

    private Result<PaymentIntent, String> retrieveAndConfirmIntent(
            String paymentIntentId,
            PaymentIntentConfirmParams params
    ) {
        return retrievePaymentIntentById(paymentIntentId)
            .flatMap(intent -> ResultUtil.tryExecute(() ->
                intent.confirm(params)
            ).mapError(Throwable::getMessage));
    }

    private Result<PaymentIntent, String> executeCaptureIntent(PaymentIntent intent) {
        return ResultUtil.tryExecute(() ->
            intent.capture()
        ).mapError(Throwable::getMessage);
    }

    private Result<RefundCreateParams, String> buildRefundParams(
            String gatewayPaymentId,
            RefundRequest request
    ) {
        return ResultUtil.safely(() ->
            RefundCreateParams.builder()
                .setPaymentIntent(gatewayPaymentId)
                .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build()
        ).mapError(Throwable::getMessage);
    }

    private Result<Refund, String> executeRefundCreation(RefundCreateParams params) {
        return ResultUtil.tryExecute(() ->
            Refund.create(params)
        ).mapError(Throwable::getMessage);
    }

    private Result<CustomerCreateParams, String> buildCustomerParams(
            String email,
            String name,
            Map<String, String> metadata
    ) {
        return ResultUtil.safely(() -> {
            CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name);

            metadata.forEach(builder::putMetadata);

            return builder.build();
        }).mapError(Throwable::getMessage);
    }

    private Result<Customer, String> executeCustomerCreation(CustomerCreateParams params) {
        return ResultUtil.tryExecute(() ->
            Customer.create(params)
        ).mapError(Throwable::getMessage);
    }

    private Result<SubscriptionCreateParams, String> buildSubscriptionParams(
            String customerId,
            String priceId,
            Integer quantity
    ) {
        return ResultUtil.safely(() ->
            SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .setQuantity(quantity.longValue())
                        .build()
                )
                .build()
        ).mapError(Throwable::getMessage);
    }

    private Result<Subscription, String> executeSubscriptionCreation(
            SubscriptionCreateParams params
    ) {
        return ResultUtil.tryExecute(() ->
            Subscription.create(params)
        ).mapError(Throwable::getMessage);
    }

    private Result<Subscription, String> retrieveSubscription(String subscriptionId) {
        return ResultUtil.tryExecute(() ->
            Subscription.retrieve(subscriptionId)
        ).mapError(Throwable::getMessage);
    }

    private Result<Subscription, String> executeCancellation(
            Subscription subscription,
            Boolean cancelAtPeriodEnd
    ) {
        return ResultUtil.tryExecute(() ->
            cancelAtPeriodEnd
                ? subscription.update(
                    SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build()
                )
                : subscription.cancel()
        ).mapError(Throwable::getMessage);
    }

    private Result<SetupIntentCreateParams, String> buildSetupIntentParams(String customerId) {
        return ResultUtil.safely(() ->
            SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build()
        ).mapError(Throwable::getMessage);
    }

    private Result<SetupIntent, String> executeSetupIntentCreation(
            SetupIntentCreateParams params
    ) {
        return ResultUtil.tryExecute(() ->
            SetupIntent.create(params)
        ).mapError(Throwable::getMessage);
    }

    private PaymentResponse mapToPaymentResponse(PaymentIntent intent) {
        PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
            .paymentId(intent.getId())
            .clientSecret(intent.getClientSecret())
            .build();

        return PaymentResponse.builder()
            .amount(BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100)))
            .currency(intent.getCurrency().toUpperCase())
            .status(mapPaymentStatus(intent.getStatus()))
            .gateway(com.trademaster.payment.enums.PaymentGateway.STRIPE)
            .gatewayResponse(gatewayResponse)
            .createdAt(Instant.ofEpochSecond(intent.getCreated()))
            .build();
    }

    private RefundResponse mapToRefundResponse(Refund refund) {
        return RefundResponse.builder()
            .refundId(refund.getId())
            .transactionId(refund.getPaymentIntent())
            .amount(BigDecimal.valueOf(refund.getAmount()).divide(BigDecimal.valueOf(100)))
            .currency(refund.getCurrency().toUpperCase())
            .status(refund.getStatus())
            .createdAt(Instant.ofEpochSecond(refund.getCreated()))
            .build();
    }

    private PaymentStatus mapPaymentStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "requires_payment_method" -> PaymentStatus.PENDING;
            case "requires_confirmation" -> PaymentStatus.PENDING;
            case "requires_action" -> PaymentStatus.PENDING;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_capture" -> PaymentStatus.AUTHORIZED;
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }
}
