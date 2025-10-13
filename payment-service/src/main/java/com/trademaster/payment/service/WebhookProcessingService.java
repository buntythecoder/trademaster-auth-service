package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.domain.WebhookEvent;
import com.trademaster.payment.entity.WebhookLog;
import com.trademaster.payment.enums.PaymentGateway;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Webhook Processing Service Interface
 * Functional interface for webhook event processing and verification
 *
 * Compliance:
 * - Rule 2: Interface Segregation - focused webhook operations interface
 * - Rule 3: Functional Programming - Result types, CompletableFuture
 * - Rule 11: Railway programming with Result types
 * - Rule 12: Virtual Threads with CompletableFuture
 * - Rule 14: Pattern matching on sealed WebhookEvent types
 * - Rule 24: Circuit breaker protection through gateway factory
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface WebhookProcessingService {

    /**
     * Process incoming webhook asynchronously with virtual threads
     * Verifies signature, parses event, updates transaction, and logs
     *
     * Railway programming pattern:
     * 1. Verify webhook signature
     * 2. Parse webhook event (sealed type)
     * 3. Handle webhook event (pattern matching)
     * 4. Log webhook for audit trail
     * 5. Return result with error handling
     *
     * @param gateway Payment gateway that sent the webhook
     * @param payload Raw webhook payload as string
     * @param signature Webhook signature for verification
     * @param headers Webhook headers for additional context
     * @return CompletableFuture containing Result with processed event or error
     */
    CompletableFuture<Result<WebhookEvent, String>> processWebhook(
        PaymentGateway gateway,
        String payload,
        String signature,
        Map<String, String> headers
    );

    /**
     * Handle parsed webhook event with pattern matching
     * Uses sealed WebhookEvent interface for type-safe dispatch
     *
     * Pattern matching on sealed types (NO if-else):
     * - PaymentSucceeded → update transaction to COMPLETED
     * - PaymentFailed → update transaction to FAILED
     * - PaymentAuthorized → update transaction to AUTHORIZED
     * - PaymentCaptured → update transaction to COMPLETED
     * - RefundProcessed → update transaction to REFUNDED
     * - RefundFailed → log refund failure
     * - Unknown → log unknown event
     *
     * @param event Parsed webhook event (sealed type)
     * @param correlationId Correlation ID for tracking
     * @return Result containing success confirmation or error
     */
    Result<String, String> handleWebhookEvent(
        WebhookEvent event,
        String correlationId
    );

    /**
     * Verify webhook signature using gateway-specific validation
     * Delegates to PaymentGatewayFactory for gateway-specific verification
     *
     * @param gateway Payment gateway
     * @param payload Raw webhook payload
     * @param signature Webhook signature
     * @param secret Webhook secret key
     * @return Result containing verification status or error
     */
    Result<Boolean, String> verifyWebhookSignature(
        PaymentGateway gateway,
        String payload,
        String signature,
        String secret
    );

    /**
     * Parse webhook payload into typed event
     * Uses WebhookEvent sealed interface factory method
     *
     * @param gateway Payment gateway
     * @param payload Raw webhook payload
     * @return Result containing parsed WebhookEvent or error
     */
    Result<WebhookEvent, String> parseWebhookEvent(
        PaymentGateway gateway,
        Map<String, Object> payload
    );

    /**
     * Log webhook for audit trail and compliance
     * Persists webhook details for troubleshooting and compliance
     *
     * @param gateway Payment gateway
     * @param payload Raw webhook payload
     * @param event Parsed webhook event
     * @param status Processing status (SUCCESS, FAILED, INVALID_SIGNATURE)
     * @param correlationId Correlation ID for tracking
     * @return Result containing saved WebhookLog or error
     */
    Result<WebhookLog, String> logWebhook(
        PaymentGateway gateway,
        String payload,
        WebhookEvent event,
        String status,
        String correlationId
    );

    /**
     * Replay failed webhook processing
     * Reprocesses webhook from audit log for recovery
     *
     * @param webhookLogId Webhook log ID to replay
     * @return CompletableFuture containing Result with replay status or error
     */
    CompletableFuture<Result<String, String>> replayWebhook(Long webhookLogId);

    /**
     * Get webhook processing history with pagination
     * Returns paginated webhook logs for monitoring
     *
     * @param gateway Optional gateway filter
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return Result containing paginated webhook logs or error
     */
    Result<org.springframework.data.domain.Page<WebhookLog>, String> getWebhookHistory(
        java.util.Optional<PaymentGateway> gateway,
        java.util.Optional<String> status,
        org.springframework.data.domain.Pageable pageable
    );
}
