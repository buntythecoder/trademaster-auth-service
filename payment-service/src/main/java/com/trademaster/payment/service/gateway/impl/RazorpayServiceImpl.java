package com.trademaster.payment.service.gateway.impl;

import com.razorpay.*;
import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.gateway.RazorpayService;
import com.trademaster.payment.util.CircuitBreakerUtil;
import com.trademaster.payment.util.ResultUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

/**
 * Razorpay Payment Gateway Implementation
 * Functional implementation with circuit breakers and Railway programming
 *
 * Compliance:
 * - Rule 3: Functional Programming First - NO if-else, NO loops, NO try-catch
 * - Rule 11: Railway programming with Result types
 * - Rule 24: Circuit breaker for ALL external calls
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 10: @Slf4j for structured logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayServiceImpl implements RazorpayService {

    private static final String CIRCUIT_BREAKER_NAME = "razorpay-service";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final CircuitBreakerUtil circuitBreakerUtil;

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Create payment order with circuit breaker protection
     * Functional approach: Result type, no try-catch
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createOrder(PaymentRequest request) {
        log.info("Creating Razorpay order: userId={}, amount={}", request.getUserId(), request.getAmount());

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> createRazorpayClient()
                .flatMap(client -> createOrderRequest(request)
                    .flatMap(orderRequest -> executeOrderCreation(client, orderRequest)))
                .map(order -> order.get("id").toString())
                .onSuccess(orderId -> log.info("Razorpay order created: orderId={}", orderId))
                .getOrElse(null)
        ).map(value -> Result.<String, String>success(value))
         .orElseGet(() -> Result.failure("Failed to create Razorpay order"));
    }

    /**
     * Verify payment signature using HMAC-SHA256
     * Functional crypto validation without if-else
     */
    @Override
    public Result<Boolean, String> verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature
    ) {
        log.debug("Verifying Razorpay signature: orderId={}, paymentId={}", orderId, paymentId);

        return createSignaturePayload(orderId, paymentId)
            .flatMap(payload -> computeHmacSignature(payload, webhookSecret))
            .map(computed -> computed.equals(signature))
            .onSuccess(isValid -> log.info("Signature verification: orderId={}, valid={}", orderId, isValid))
            .map(Result::<Boolean, String>success)
            .getOrElse(Result.failure("Signature verification failed"));
    }

    /**
     * Capture authorized payment with circuit breaker
     * Railway programming with flatMap chains
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<PaymentResponse, String> capturePayment(
            String paymentId,
            Long amount,
            String currency
    ) {
        log.info("Capturing Razorpay payment: paymentId={}, amount={}", paymentId, amount);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> createRazorpayClient()
                .flatMap(client -> capturePaymentRequest(client, paymentId, amount, currency))
                .map(payment -> mapToPaymentResponse(payment.toJson()))
                .onSuccess(response -> log.info("Payment captured: paymentId={}, status={}",
                        paymentId, response.getStatus()))
                .getOrElse(null)
        ).map(value -> Result.<PaymentResponse, String>success(value))
         .orElseGet(() -> Result.failure("Failed to capture payment"));
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
        log.info("Processing Razorpay refund: gatewayPaymentId={}, transactionId={}, amount={}",
                gatewayPaymentId, request.getTransactionId(), request.getAmount());

        return CompletableFuture.supplyAsync(() ->
            circuitBreakerUtil.executeWithCircuitBreaker(
                CIRCUIT_BREAKER_NAME,
                () -> createRazorpayClient()
                    .flatMap(client -> createRefundRequest(request)
                        .flatMap(refundReq -> executeRefundCreation(client, gatewayPaymentId, refundReq)))
                    .map(refund -> mapToRefundResponse(refund.toJson()))
                    .onSuccess(response -> log.info("Refund processed: refundId={}, status={}",
                            response.getRefundId(), response.getStatus()))
                    .getOrElse(null)
            ).map(value -> Result.<RefundResponse, String>success(value))
             .orElseGet(() -> Result.failure("Failed to process refund"))
        );
    }

    /**
     * Fetch payment details with circuit breaker
     * Idempotent operation for reconciliation
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<PaymentResponse, String> fetchPaymentDetails(String paymentId) {
        log.debug("Fetching Razorpay payment details: paymentId={}", paymentId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> createRazorpayClient()
                .flatMap(client -> fetchPayment(client, paymentId))
                .map(payment -> mapToPaymentResponse(payment.toJson()))
                .onSuccess(response -> log.debug("Payment details fetched: paymentId={}", paymentId))
                .getOrElse(null)
        ).map(value -> Result.<PaymentResponse, String>success(value))
         .orElseGet(() -> Result.failure("Failed to fetch payment details"));
    }

    /**
     * Create subscription with circuit breaker
     * Recurring payment setup
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Result<String, String> createSubscription(
            String planId,
            String customerId,
            Integer totalCount
    ) {
        log.info("Creating Razorpay subscription: planId={}, customerId={}", planId, customerId);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> createRazorpayClient()
                .flatMap(client -> createSubscriptionRequest(planId, customerId, totalCount)
                    .flatMap(subRequest -> executeSubscriptionCreation(client, subRequest)))
                .map(subscription -> subscription.toJson().getString("id"))
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
            Boolean cancelAtCycleEnd
    ) {
        log.info("Cancelling Razorpay subscription: subscriptionId={}, cancelAtEnd={}",
                subscriptionId, cancelAtCycleEnd);

        return circuitBreakerUtil.executeWithCircuitBreaker(
            CIRCUIT_BREAKER_NAME,
            () -> createRazorpayClient()
                .flatMap(client -> cancelSubscriptionRequest(client, subscriptionId, cancelAtCycleEnd))
                .map(subscription -> "cancelled".equals(subscription.toJson().getString("status")))
                .onSuccess(cancelled -> log.info("Subscription cancelled: subscriptionId={}, success={}",
                        subscriptionId, cancelled))
                .getOrElse(false)
        ).map(value -> Result.<Boolean, String>success(value))
         .orElseGet(() -> Result.failure("Failed to cancel subscription"));
    }

    // ==================== Private Helper Methods (Functional) ====================

    private Result<RazorpayClient, String> createRazorpayClient() {
        return ResultUtil.tryExecute(() ->
            new RazorpayClient(razorpayKeyId, razorpayKeySecret)
        ).mapError(Throwable::getMessage);
    }

    private Result<JSONObject, String> createOrderRequest(PaymentRequest request) {
        return ResultUtil.safely(() -> {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());
            return orderRequest;
        }).mapError(Throwable::getMessage);
    }

    private Result<Order, String> executeOrderCreation(
            RazorpayClient client,
            JSONObject orderRequest
    ) {
        return ResultUtil.tryExecute(() -> client.orders.create(orderRequest))
            .mapError(Throwable::getMessage);
    }

    private Result<String, String> createSignaturePayload(String orderId, String paymentId) {
        return Result.success(orderId + "|" + paymentId);
    }

    private Result<String, String> computeHmacSignature(String payload, String secret) {
        return ResultUtil.tryExecute(() -> {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }).mapError(Throwable::getMessage);
    }

    private Result<Payment, String> capturePaymentRequest(
            RazorpayClient client,
            String paymentId,
            Long amount,
            String currency
    ) {
        return ResultUtil.tryExecute(() -> {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amount);
            captureRequest.put("currency", currency);
            return client.payments.capture(paymentId, captureRequest);
        }).mapError(Throwable::getMessage);
    }

    private Result<JSONObject, String> createRefundRequest(RefundRequest request) {
        return ResultUtil.safely(() -> {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
            return refundRequest;
        }).mapError(Throwable::getMessage);
    }

    private Result<Refund, String> executeRefundCreation(
            RazorpayClient client,
            String paymentId,
            JSONObject refundRequest
    ) {
        return ResultUtil.tryExecute(() ->
            client.payments.refund(paymentId, refundRequest)
        ).mapError(Throwable::getMessage);
    }

    private Result<Payment, String> fetchPayment(RazorpayClient client, String paymentId) {
        return ResultUtil.tryExecute(() ->
            client.payments.fetch(paymentId)
        ).mapError(Throwable::getMessage);
    }

    private Result<JSONObject, String> createSubscriptionRequest(
            String planId,
            String customerId,
            Integer totalCount
    ) {
        return ResultUtil.safely(() -> {
            JSONObject subRequest = new JSONObject();
            subRequest.put("plan_id", planId);
            subRequest.put("customer_id", customerId);
            subRequest.put("total_count", totalCount);
            return subRequest;
        }).mapError(Throwable::getMessage);
    }

    private Result<Subscription, String> executeSubscriptionCreation(
            RazorpayClient client,
            JSONObject subRequest
    ) {
        return ResultUtil.tryExecute(() ->
            client.subscriptions.create(subRequest)
        ).mapError(Throwable::getMessage);
    }

    private Result<Subscription, String> cancelSubscriptionRequest(
            RazorpayClient client,
            String subscriptionId,
            Boolean cancelAtCycleEnd
    ) {
        return ResultUtil.tryExecute(() -> {
            JSONObject cancelRequest = new JSONObject();
            cancelRequest.put("cancel_at_cycle_end", cancelAtCycleEnd);
            return client.subscriptions.cancel(subscriptionId, cancelRequest);
        }).mapError(Throwable::getMessage);
    }

    private PaymentResponse mapToPaymentResponse(JSONObject payment) {
        PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
            .paymentId(payment.getString("id"))
            .orderId(payment.optString("order_id", null))
            .razorpayOrderId(payment.optString("order_id", null))
            .build();

        return PaymentResponse.builder()
            .amount(BigDecimal.valueOf(payment.getLong("amount")).divide(BigDecimal.valueOf(100)))
            .currency(payment.getString("currency"))
            .status(mapPaymentStatus(payment.getString("status")))
            .gateway(com.trademaster.payment.enums.PaymentGateway.RAZORPAY)
            .gatewayResponse(gatewayResponse)
            .createdAt(Instant.ofEpochSecond(payment.getLong("created_at")))
            .build();
    }

    private RefundResponse mapToRefundResponse(JSONObject refund) {
        return RefundResponse.builder()
            .refundId(refund.getString("id"))
            .transactionId(refund.getString("payment_id"))
            .amount(BigDecimal.valueOf(refund.getLong("amount")).divide(BigDecimal.valueOf(100)))
            .currency(refund.getString("currency"))
            .status(refund.getString("status"))
            .createdAt(Instant.ofEpochSecond(refund.getLong("created_at")))
            .build();
    }

    private PaymentStatus mapPaymentStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "authorized" -> PaymentStatus.AUTHORIZED;
            case "captured" -> PaymentStatus.COMPLETED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "failed" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}
