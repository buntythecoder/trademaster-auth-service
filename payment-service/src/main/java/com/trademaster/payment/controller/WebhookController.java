package com.trademaster.payment.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.domain.WebhookEvent;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.service.WebhookProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Webhook Controller - External Gateway Webhooks
 * Public endpoints for payment gateway webhook notifications
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else, NO try-catch, Railway programming
 * - Rule 11: Railway programming with Result.fold() for response mapping
 * - Rule 12: Virtual Threads with CompletableFuture for async processing
 * - Rule 14: Pattern matching for gateway routing
 * - Rule 15: Structured logging with correlation IDs
 * - Rule 24: Circuit breaker through WebhookProcessingService
 *
 * Security: Signature verification instead of @PreAuthorize (public webhooks)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Webhook endpoints for payment gateway real-time notifications")
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;

    /**
     * Handle Razorpay webhook with Railway programming
     * Complexity: 5 (within ≤7 limit)
     */
    @PostMapping("/razorpay")
    @Operation(
        summary = "Razorpay Webhook",
        description = "Handle Razorpay payment notifications with signature verification. " +
                     "Supports payment.captured, payment.failed, payment.authorized, refund events.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or payload"),
            @ApiResponse(responseCode = "500", description = "Webhook processing failed")
        }
    )
    public CompletableFuture<ResponseEntity<Object>> handleRazorpayWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @Parameter(description = "Razorpay signature header")
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("Received Razorpay webhook: signature={}...",
                maskSignature(signature));

        Map<String, String> headers = extractHeaders(request);

        return webhookProcessingService.processWebhook(
                PaymentGateway.RAZORPAY,
                payload,
                signature,
                headers
            )
            .thenApply(result -> result.fold(
                event -> createWebhookSuccessResponse(event, HttpStatus.OK),
                error -> createWebhookErrorResponse(error, HttpStatus.BAD_REQUEST)
            ));
    }

    /**
     * Handle Stripe webhook with Railway programming
     * Complexity: 5 (within ≤7 limit)
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Stripe Webhook",
        description = "Handle Stripe payment notifications with signature verification. " +
                     "Supports payment_intent.succeeded, payment_intent.payment_failed, charge events.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or payload"),
            @ApiResponse(responseCode = "500", description = "Webhook processing failed")
        }
    )
    public CompletableFuture<ResponseEntity<Object>> handleStripeWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @Parameter(description = "Stripe signature header")
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook: signature={}...",
                maskSignature(signature));

        Map<String, String> headers = extractHeaders(request);

        return webhookProcessingService.processWebhook(
                PaymentGateway.STRIPE,
                payload,
                signature,
                headers
            )
            .thenApply(result -> result.fold(
                event -> createWebhookSuccessResponse(event, HttpStatus.OK),
                error -> createWebhookErrorResponse(error, HttpStatus.BAD_REQUEST)
            ));
    }

    /**
     * Handle UPI webhook (through Razorpay)
     * Pattern matching routes to Razorpay handler
     * Complexity: 5 (within ≤7 limit)
     */
    @PostMapping("/upi")
    @Operation(
        summary = "UPI Webhook",
        description = "Handle UPI payment notifications (routed through Razorpay)",
        responses = {
            @ApiResponse(responseCode = "200", description = "UPI webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or payload")
        }
    )
    public CompletableFuture<ResponseEntity<Object>> handleUpiWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("Received UPI webhook (via Razorpay)");

        Map<String, String> headers = extractHeaders(request);

        return webhookProcessingService.processWebhook(
                PaymentGateway.UPI,
                payload,
                signature,
                headers
            )
            .thenApply(result -> result.fold(
                event -> createWebhookSuccessResponse(event, HttpStatus.OK),
                error -> createWebhookErrorResponse(error, HttpStatus.BAD_REQUEST)
            ));
    }

    /**
     * Generic webhook endpoint with gateway routing
     * Pattern matching for gateway selection (NO if-else)
     * Complexity: 6 (within ≤7 limit)
     */
    @PostMapping("/{gateway}")
    @Operation(
        summary = "Generic Webhook Endpoint",
        description = "Handle webhooks for any supported gateway with automatic routing",
        responses = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid gateway or signature")
        }
    )
    public CompletableFuture<ResponseEntity<Object>> handleGenericWebhook(
            @Parameter(description = "Payment Gateway (razorpay, stripe, upi)")
            @PathVariable String gateway,
            HttpServletRequest request,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {

        log.info("Received generic webhook: gateway={}", gateway);

        return parseGateway(gateway)
            .map(g -> webhookProcessingService.processWebhook(
                g, payload, extractSignature(g, headers), headers
            ))
            .fold(
                future -> future.thenApply(result -> result.fold(
                    event -> createWebhookSuccessResponse(event, HttpStatus.OK),
                    error -> createWebhookErrorResponse(error, HttpStatus.BAD_REQUEST)
                )),
                error -> CompletableFuture.completedFuture(
                    createWebhookErrorResponse(error, HttpStatus.BAD_REQUEST)
                )
            );
    }

    /**
     * Webhook health check endpoint
     * Complexity: 1 (within ≤7 limit)
     */
    @GetMapping("/health")
    @Operation(
        summary = "Webhook Health Check",
        description = "Check webhook endpoint availability and status"
    )
    public ResponseEntity<HealthResponse> webhookHealthCheck() {
        HealthResponse response = new HealthResponse(
            "UP",
            "Webhook endpoints are operational",
            Instant.now()
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Parse gateway from string with pattern matching (NO if-else)
     * Complexity: 2 (within ≤7 limit)
     */
    private Result<PaymentGateway, String> parseGateway(String gateway) {
        return switch (gateway.toLowerCase()) {
            case "razorpay" -> Result.success(PaymentGateway.RAZORPAY);
            case "stripe" -> Result.success(PaymentGateway.STRIPE);
            case "upi" -> Result.success(PaymentGateway.UPI);
            default -> Result.failure("Unsupported gateway: " + gateway);
        };
    }

    /**
     * Extract signature from headers with pattern matching (NO if-else)
     * Complexity: 2 (within ≤7 limit)
     */
    private String extractSignature(PaymentGateway gateway, Map<String, String> headers) {
        return switch (gateway) {
            case RAZORPAY, UPI -> headers.getOrDefault("X-Razorpay-Signature", "");
            case STRIPE -> headers.getOrDefault("Stripe-Signature", "");
        };
    }

    /**
     * Extract headers from request
     * Functional header extraction with Stream API (NO loops)
     * Complexity: 2 (within ≤7 limit)
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        return java.util.Collections.list(request.getHeaderNames()).stream()
            .collect(Collectors.toMap(
                name -> name,
                request::getHeader
            ));
    }

    /**
     * Mask signature for logging security
     * Complexity: 1 (within ≤7 limit)
     */
    private String maskSignature(String signature) {
        return signature.substring(0, Math.min(signature.length(), 10));
    }

    /**
     * Create webhook success response
     * Complexity: 1 (within ≤7 limit)
     */
    private ResponseEntity<Object> createWebhookSuccessResponse(
            WebhookEvent event,
            HttpStatus status
    ) {
        WebhookResponse response = new WebhookResponse(
            "success",
            "Webhook processed successfully",
            event.gateway().name(),
            event.eventType(),
            Instant.now()
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }

    /**
     * Create webhook error response
     * Complexity: 1 (within ≤7 limit)
     */
    private ResponseEntity<Object> createWebhookErrorResponse(
            String error,
            HttpStatus status
    ) {
        WebhookErrorResponse response = new WebhookErrorResponse(
            status.value(),
            error,
            Instant.now()
        );

        return ResponseEntity
            .status(status)
            .body(response);
    }

    // ==================== Response DTOs (Immutable Records) ====================

    /**
     * Webhook success response (Rule 9: Records for immutability)
     */
    public record WebhookResponse(
        String status,
        String message,
        String gateway,
        String eventType,
        Instant timestamp
    ) {}

    /**
     * Webhook error response (Rule 9: Records for immutability)
     */
    public record WebhookErrorResponse(
        int statusCode,
        String message,
        Instant timestamp
    ) {}

    /**
     * Health check response (Rule 9: Records for immutability)
     */
    public record HealthResponse(
        String status,
        String message,
        Instant timestamp
    ) {}
}
