package com.trademaster.payment.service.impl;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import com.trademaster.payment.service.PaymentProcessingService;
import com.trademaster.payment.service.gateway.PaymentGatewayFactory;
import com.trademaster.payment.util.ResultUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Payment Processing Service Implementation
 * Functional implementation with Railway programming, Virtual Threads, and Circuit Breakers
 *
 * Compliance:
 * - Rule 1: Java 24 + Virtual Threads with CompletableFuture
 * - Rule 2: SOLID Principles - Single Responsibility, DI
 * - Rule 3: Functional Programming - NO if-else, NO loops, NO try-catch
 * - Rule 4: Advanced Design Patterns - Strategy, Command, Observer
 * - Rule 5: Cognitive Complexity ≤ 7, max 15 lines per method
 * - Rule 6: Zero Trust - Use gateway factory, audit all operations
 * - Rule 9: Immutability - Result types, functional composition
 * - Rule 10: @Slf4j for structured logging
 * - Rule 11: Railway programming with Result types
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 13: Stream API (no loops)
 * - Rule 14: Pattern matching with switch expressions
 * - Rule 15: Structured logging with correlation IDs
 * - Rule 16: @Value for configuration
 * - Rule 19: Private by default, explicit access
 * - Rule 24: Circuit breaker protection via gateway factory
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private static final String PAYMENT_TOPIC = "payment.events";
    private static final String CORRELATION_ID_PREFIX = "payment-";

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${trademaster.payment.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${trademaster.payment.receipt.prefix:TM}")
    private String receiptPrefix;

    // Virtual Thread Executor for async operations (Rule 12)
    private static final java.util.concurrent.Executor VIRTUAL_EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Process payment request asynchronously with virtual threads
     * Railway programming pattern with functional composition
     * Cognitive Complexity: 5 (compliant with Rule 5)
     */
    @Override
    @Transactional
    public CompletableFuture<Result<PaymentResponse, String>> processPayment(
            PaymentRequest request
    ) {
        String correlationId = generateCorrelationId();
        Timer.Sample timer = Timer.start(meterRegistry);

        log.info("Processing payment: correlationId={}, userId={}, gateway={}, amount={}",
                correlationId, request.getUserId(), request.getPaymentGateway(), request.getAmount());

        return CompletableFuture.supplyAsync(
            () -> validatePaymentRequest(request)
                .flatMap(validRequest -> createPaymentTransaction(validRequest, correlationId))
                .flatMap(transaction -> processWithGateway(request, transaction, correlationId))
                .onSuccess(response -> recordSuccessMetrics(timer, request.getPaymentGateway()))
                .onSuccess(response -> publishPaymentEvent(response, correlationId))
                .onFailure(error -> recordFailureMetrics(timer, request.getPaymentGateway(), error)),
            VIRTUAL_EXECUTOR
        );
    }

    /**
     * Confirm payment after authorization
     * Two-step payment flow support
     * Cognitive Complexity: 4
     */
    @Override
    @Transactional
    public CompletableFuture<Result<PaymentResponse, String>> confirmPayment(
            UUID transactionId,
            String paymentMethodId
    ) {
        String correlationId = generateCorrelationId();
        log.info("Confirming payment: correlationId={}, transactionId={}", correlationId, transactionId);

        return CompletableFuture.supplyAsync(
            () -> findTransaction(transactionId)
                .flatMap(transaction -> confirmWithGateway(transaction, paymentMethodId, correlationId))
                .flatMap(response -> updateTransactionAfterConfirmation(response, transactionId))
                .onSuccess(response -> publishPaymentEvent(response, correlationId)),
            VIRTUAL_EXECUTOR
        );
    }

    /**
     * Process refund asynchronously
     * Functional refund processing with validation
     * Cognitive Complexity: 5
     */
    @Override
    @Transactional
    public CompletableFuture<Result<RefundResponse, String>> processRefund(RefundRequest request) {
        String correlationId = generateCorrelationId();
        log.info("Processing refund: correlationId={}, transactionId={}, amount={}",
                correlationId, request.getTransactionId(), request.getAmount());

        return CompletableFuture.supplyAsync(
            () -> validateRefundRequest(request)
                .flatMap(validRequest -> findTransaction(validRequest.getTransactionId()))
                .flatMap(transaction -> validateRefundable(transaction, request.getAmount()))
                .flatMap(transaction -> processRefundWithGateway(transaction, request, correlationId))
                .flatMap(response -> updateTransactionAfterRefund(response, request))
                .onSuccess(response -> publishRefundEvent(response, correlationId)),
            VIRTUAL_EXECUTOR
        );
    }

    /**
     * Get transaction details by ID
     * Simple read operation
     * Cognitive Complexity: 2
     */
    @Override
    @Transactional(readOnly = true)
    public Result<PaymentTransaction, String> getTransaction(UUID transactionId) {
        log.debug("Getting transaction: transactionId={}", transactionId);
        return findTransaction(transactionId);
    }

    /**
     * Get payment status
     * Lightweight status check
     * Cognitive Complexity: 3
     */
    @Override
    @Transactional(readOnly = true)
    public Result<PaymentStatus, String> getPaymentStatus(UUID transactionId) {
        log.debug("Getting payment status: transactionId={}", transactionId);
        return findTransaction(transactionId)
            .map(PaymentTransaction::getStatus);
    }

    /**
     * Get payment history with pagination
     * Functional pagination support
     * Cognitive Complexity: 2
     */
    @Override
    @Transactional(readOnly = true)
    public Result<Page<PaymentTransaction>, String> getPaymentHistory(
            UUID userId,
            Pageable pageable
    ) {
        log.debug("Getting payment history: userId={}, page={}", userId, pageable.getPageNumber());
        return ResultUtil.safely(() ->
            transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        ).mapError(Throwable::getMessage);
    }

    /**
     * Get transaction by gateway payment ID
     * Used for webhook processing
     * Cognitive Complexity: 2
     */
    @Override
    @Transactional(readOnly = true)
    public Result<PaymentTransaction, String> getTransactionByGatewayPaymentId(
            String gatewayPaymentId
    ) {
        log.debug("Getting transaction by gateway payment ID: {}", gatewayPaymentId);
        return Optional.ofNullable(gatewayPaymentId)
            .flatMap(transactionRepository::findByGatewayPaymentId)
            .map(Result::<PaymentTransaction, String>success)
            .orElseGet(() -> Result.failure("Transaction not found: " + gatewayPaymentId));
    }

    /**
     * Update transaction status from webhook
     * Functional status update with audit
     * Cognitive Complexity: 4
     */
    @Override
    @Transactional
    public Result<PaymentTransaction, String> updateTransactionStatus(
            UUID transactionId,
            PaymentStatus newStatus,
            Map<String, Object> gatewayResponse
    ) {
        log.info("Updating transaction status: transactionId={}, newStatus={}", transactionId, newStatus);

        return findTransaction(transactionId)
            .flatMap(transaction -> updateStatus(transaction, newStatus, gatewayResponse))
            .flatMap(this::saveTransaction)
            .onSuccess(transaction -> log.info("Transaction status updated: transactionId={}, status={}",
                    transactionId, transaction.getStatus()));
    }

    /**
     * Retry failed payment
     * Functional retry with validation
     * Cognitive Complexity: 5
     */
    @Override
    @Transactional
    public CompletableFuture<Result<PaymentResponse, String>> retryPayment(UUID transactionId) {
        String correlationId = generateCorrelationId();
        log.info("Retrying payment: correlationId={}, transactionId={}", correlationId, transactionId);

        return CompletableFuture.supplyAsync(
            () -> findTransaction(transactionId)
                .flatMap(this::validateRetryable)
                .flatMap(transaction -> recreatePaymentRequest(transaction))
                .flatMap(request -> processPayment(request).join()),
            VIRTUAL_EXECUTOR
        );
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Validate payment request
     * Functional validation chain (NO if-else)
     * Cognitive Complexity: 2
     */
    private Result<PaymentRequest, String> validatePaymentRequest(PaymentRequest request) {
        return Optional.ofNullable(request)
            .filter(req -> req.getAmount() != null && req.getAmount().doubleValue() > 0)
            .filter(req -> req.getUserId() != null)
            .filter(req -> req.getPaymentGateway() != null)
            .map(Result::<PaymentRequest, String>success)
            .orElseGet(() -> Result.failure("Invalid payment request: missing required fields"));
    }

    /**
     * Create payment transaction entity
     * Functional entity creation with builder
     * Cognitive Complexity: 3
     */
    private Result<PaymentTransaction, String> createPaymentTransaction(
            PaymentRequest request,
            String correlationId
    ) {
        return ResultUtil.safely(() -> {
            PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(request.getUserId())
                .subscriptionId(request.getSubscriptionPlanId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentGateway(request.getPaymentGateway())
                .paymentMethod(request.getPaymentMethod())
                .description(buildDescription(request))
                .receiptNumber(generateReceiptNumber())
                .metadata(Map.of("correlationId", correlationId))
                .build();

            return transactionRepository.save(transaction);
        }).mapError(Throwable::getMessage);
    }

    /**
     * Process payment with gateway using factory
     * Pattern matching for gateway routing (NO if-else)
     * Cognitive Complexity: 4
     */
    private Result<PaymentResponse, String> processWithGateway(
            PaymentRequest request,
            PaymentTransaction transaction,
            String correlationId
    ) {
        log.info("Routing to gateway: correlationId={}, gateway={}", correlationId, request.getPaymentGateway());

        return gatewayFactory.createPayment(request)
            .flatMap(intentId -> updateTransactionWithGatewayId(transaction, intentId)
                .flatMap(updatedTransaction -> buildPaymentResponse(updatedTransaction, intentId)));
    }

    /**
     * Confirm payment with gateway
     * Two-step confirmation flow
     * Cognitive Complexity: 3
     */
    private Result<PaymentResponse, String> confirmWithGateway(
            PaymentTransaction transaction,
            String paymentMethodId,
            String correlationId
    ) {
        log.info("Confirming with gateway: correlationId={}, gateway={}",
                correlationId, transaction.getPaymentGateway());

        return Optional.ofNullable(transaction.getGatewayOrderId())
            .map(orderId -> gatewayFactory.confirmPayment(orderId, paymentMethodId))
            .orElseGet(() -> Result.failure("No gateway order ID found for transaction"));
    }

    /**
     * Process refund with gateway
     * Functional refund processing
     * Cognitive Complexity: 4
     */
    private Result<RefundResponse, String> processRefundWithGateway(
            PaymentTransaction transaction,
            RefundRequest request,
            String correlationId
    ) {
        log.info("Processing refund with gateway: correlationId={}, gateway={}, gatewayPaymentId={}",
                correlationId, transaction.getPaymentGateway(), transaction.getGatewayPaymentId());

        return Optional.ofNullable(transaction.getGatewayPaymentId())
            .map(gatewayPaymentId -> gatewayFactory.processRefund(gatewayPaymentId, request)
                .thenApply(result -> result.onSuccess(response ->
                    log.info("Refund processed: correlationId={}, refundId={}", correlationId, response.getRefundId())
                ))
                .join())
            .orElse(Result.failure("No gateway payment ID found for transaction"));
    }

    /**
     * Validate refund request
     * Functional validation (NO if-else)
     * Cognitive Complexity: 2
     */
    private Result<RefundRequest, String> validateRefundRequest(RefundRequest request) {
        return Optional.ofNullable(request)
            .filter(req -> req.getTransactionId() != null)
            .filter(req -> req.getAmount() != null && req.getAmount().doubleValue() > 0)
            .map(Result::<RefundRequest, String>success)
            .orElseGet(() -> Result.failure("Invalid refund request"));
    }

    /**
     * Validate transaction is refundable
     * Business rule validation (NO if-else)
     * Cognitive Complexity: 3
     */
    private Result<PaymentTransaction, String> validateRefundable(
            PaymentTransaction transaction,
            java.math.BigDecimal requestedAmount
    ) {
        return Optional.of(transaction)
            .filter(t -> t.getStatus() == PaymentStatus.COMPLETED)
            .filter(t -> t.getRemainingRefundAmount().compareTo(requestedAmount) >= 0)
            .map(Result::<PaymentTransaction, String>success)
            .orElseGet(() -> Result.failure("Transaction cannot be refunded"));
    }

    /**
     * Validate transaction is retryable
     * Retry validation (NO if-else)
     * Cognitive Complexity: 2
     */
    private Result<PaymentTransaction, String> validateRetryable(PaymentTransaction transaction) {
        return Optional.of(transaction)
            .filter(t -> t.getStatus() == PaymentStatus.FAILED || t.getStatus() == PaymentStatus.CANCELLED)
            .map(Result::<PaymentTransaction, String>success)
            .orElseGet(() -> Result.failure("Transaction cannot be retried"));
    }

    /**
     * Find transaction by ID
     * Repository lookup with Result wrapper
     * Cognitive Complexity: 2
     */
    private Result<PaymentTransaction, String> findTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
            .map(Result::<PaymentTransaction, String>success)
            .orElseGet(() -> Result.failure("Transaction not found: " + transactionId));
    }

    /**
     * Save transaction
     * Repository save with Result wrapper
     * Cognitive Complexity: 1
     */
    private Result<PaymentTransaction, String> saveTransaction(PaymentTransaction transaction) {
        return ResultUtil.safely(() -> transactionRepository.save(transaction))
            .mapError(Throwable::getMessage);
    }

    /**
     * Update transaction with gateway ID
     * Functional entity update
     * Cognitive Complexity: 2
     */
    private Result<PaymentTransaction, String> updateTransactionWithGatewayId(
            PaymentTransaction transaction,
            String gatewayId
    ) {
        transaction.setGatewayOrderId(gatewayId);
        transaction.setStatus(PaymentStatus.PROCESSING);
        return saveTransaction(transaction);
    }

    /**
     * Update transaction status
     * Functional status transition
     * Cognitive Complexity: 2
     */
    private Result<PaymentTransaction, String> updateStatus(
            PaymentTransaction transaction,
            PaymentStatus newStatus,
            Map<String, Object> gatewayResponse
    ) {
        transaction.setStatus(newStatus);
        transaction.setGatewayResponse(gatewayResponse);
        return Result.success(transaction);
    }

    /**
     * Update transaction after confirmation
     * Post-confirmation update
     * Cognitive Complexity: 3
     */
    private Result<PaymentResponse, String> updateTransactionAfterConfirmation(
            PaymentResponse response,
            UUID transactionId
    ) {
        return findTransaction(transactionId)
            .flatMap(transaction -> {
                transaction.setStatus(response.getStatus());
                Optional.ofNullable(response.getGatewayResponse())
                    .map(PaymentResponse.GatewayResponse::getPaymentId)
                    .ifPresent(transaction::setGatewayPaymentId);
                return saveTransaction(transaction);
            })
            .map(transaction -> response);
    }

    /**
     * Update transaction after refund
     * Post-refund update
     * Cognitive Complexity: 3
     */
    private Result<RefundResponse, String> updateTransactionAfterRefund(
            RefundResponse response,
            RefundRequest request
    ) {
        return findTransaction(request.getTransactionId())
            .flatMap(transaction -> {
                transaction.addRefund(request.getAmount(), request.getReason());
                return saveTransaction(transaction);
            })
            .map(transaction -> response);
    }

    /**
     * Build payment response from transaction
     * DTO mapping with functional composition
     * Cognitive Complexity: 2
     */
    private Result<PaymentResponse, String> buildPaymentResponse(
            PaymentTransaction transaction,
            String intentId
    ) {
        PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
            .orderId(intentId)
            .build();

        return Result.success(PaymentResponse.builder()
            .transactionId(transaction.getId())
            .status(transaction.getStatus())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .gateway(transaction.getPaymentGateway())
            .gatewayResponse(gatewayResponse)
            .receiptNumber(transaction.getReceiptNumber())
            .createdAt(transaction.getCreatedAt())
            .message("Payment processed successfully")
            .build());
    }

    /**
     * Recreate payment request from transaction
     * Request reconstruction for retry
     * Cognitive Complexity: 2
     */
    private Result<PaymentRequest, String> recreatePaymentRequest(PaymentTransaction transaction) {
        return Result.success(PaymentRequest.builder()
            .userId(transaction.getUserId())
            .subscriptionPlanId(transaction.getSubscriptionId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .paymentGateway(transaction.getPaymentGateway())
            .paymentMethod(transaction.getPaymentMethod())
            .build());
    }

    /**
     * Build description from request
     * String construction helper
     * Cognitive Complexity: 1
     */
    private String buildDescription(PaymentRequest request) {
        return Optional.ofNullable(request.getMetadata())
            .map(meta -> meta.getPlanName())
            .map(planName -> "Payment for " + planName + " subscription")
            .orElse("Subscription payment");
    }

    /**
     * Generate receipt number
     * Unique receipt generation
     * Cognitive Complexity: 1
     */
    private String generateReceiptNumber() {
        return String.format("%s_%s_%d",
            receiptPrefix,
            java.time.LocalDate.now().toString().replace("-", ""),
            System.nanoTime() % 1000000);
    }

    /**
     * Generate correlation ID
     * Unique correlation ID for tracing
     * Cognitive Complexity: 1
     */
    private String generateCorrelationId() {
        return CORRELATION_ID_PREFIX + UUID.randomUUID().toString();
    }

    /**
     * Record success metrics
     * Prometheus metrics recording
     * Cognitive Complexity: 1
     */
    private void recordSuccessMetrics(Timer.Sample timer, PaymentGateway gateway) {
        timer.stop(meterRegistry.timer("payment.processing.time",
            "gateway", gateway.name(), "status", "success"));
        meterRegistry.counter("payment.processed",
            "gateway", gateway.name(), "status", "success").increment();
    }

    /**
     * Record failure metrics
     * Prometheus metrics recording
     * Cognitive Complexity: 1
     */
    private void recordFailureMetrics(Timer.Sample timer, PaymentGateway gateway, String error) {
        timer.stop(meterRegistry.timer("payment.processing.time",
            "gateway", gateway.name(), "status", "failure"));
        meterRegistry.counter("payment.processed",
            "gateway", gateway.name(), "status", "failure").increment();
        log.error("Payment processing failed: gateway={}, error={}", gateway, error);
    }

    /**
     * Publish payment event to Kafka
     * Event publishing with functional composition
     * Cognitive Complexity: 2
     */
    private void publishPaymentEvent(PaymentResponse response, String correlationId) {
        CompletableFuture.runAsync(() -> {
            Map<String, Object> event = Map.of(
                "eventType", "PAYMENT_PROCESSED",
                "transactionId", response.getTransactionId().toString(),
                "status", response.getStatus().name(),
                "amount", response.getAmount(),
                "currency", response.getCurrency(),
                "correlationId", correlationId,
                "timestamp", Instant.now().toString()
            );

            kafkaTemplate.send(PAYMENT_TOPIC, response.getTransactionId().toString(), event);
            log.info("Payment event published: correlationId={}, transactionId={}",
                    correlationId, response.getTransactionId());
        }, VIRTUAL_EXECUTOR);
    }

    /**
     * Publish refund event to Kafka
     * Event publishing for refunds
     * Cognitive Complexity: 2
     */
    private void publishRefundEvent(RefundResponse response, String correlationId) {
        CompletableFuture.runAsync(() -> {
            Map<String, Object> event = Map.of(
                "eventType", "REFUND_PROCESSED",
                "refundId", response.getRefundId(),
                "transactionId", response.getTransactionId(),
                "amount", response.getAmount(),
                "correlationId", correlationId,
                "timestamp", Instant.now().toString()
            );

            kafkaTemplate.send(PAYMENT_TOPIC, response.getRefundId(), event);
            log.info("Refund event published: correlationId={}, refundId={}",
                    correlationId, response.getRefundId());
        }, VIRTUAL_EXECUTOR);
    }
}
