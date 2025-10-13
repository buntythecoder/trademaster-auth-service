package com.trademaster.payment.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.PaymentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Controller - External API
 * REST API with functional patterns and Zero Trust security
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else, Railway programming with Result types
 * - Rule 6: Zero Trust Security - @PreAuthorize for all external endpoints
 * - Rule 8: OpenAPI documentation with comprehensive examples
 * - Rule 11: Railway programming with Result.fold() for response mapping
 * - Rule 12: Virtual Threads with CompletableFuture for async operations
 * - Rule 15: Structured logging with correlation IDs
 * - Rule 18: Meaningful method names with action verbs
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "Payment processing and transaction management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentProcessingService paymentProcessingService;

    /**
     * Process payment with Railway programming
     * Complexity: 4 (within ≤7 limit)
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process Payment",
        description = "Initiate a payment transaction through the configured gateway. " +
                     "Supports Razorpay, Stripe, and UPI payment methods.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Payment initiated successfully",
                content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "500", description = "Payment processing failed")
        }
    )
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Object>> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        String correlationId = generateCorrelationId();
        log.info("Processing payment request: correlationId={}, userId={}, amount={} {}",
                correlationId, request.getUserId(), request.getAmount(), request.getCurrency());

        return paymentProcessingService.processPayment(request)
            .thenApply(result -> result.fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, correlationId)
            ));
    }

    /**
     * Confirm payment with functional response mapping
     * Complexity: 4 (within ≤7 limit)
     */
    @PostMapping("/confirm/{transactionId}")
    @Operation(
        summary = "Confirm Payment",
        description = "Confirm a pending payment with payment method details",
        responses = {
            @ApiResponse(responseCode = "200", description = "Payment confirmed successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "400", description = "Payment confirmation failed")
        }
    )
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Object>> confirmPayment(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId,
            @Parameter(description = "Payment Method ID") @RequestParam String paymentMethodId) {

        String correlationId = generateCorrelationId();
        log.info("Confirming payment: correlationId={}, transactionId={}", correlationId, transactionId);

        return paymentProcessingService.confirmPayment(transactionId, paymentMethodId)
            .thenApply(result -> result.fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, correlationId)
            ));
    }

    /**
     * Process refund with Railway programming
     * Complexity: 4 (within ≤7 limit)
     */
    @PostMapping("/refund")
    @Operation(
        summary = "Process Refund",
        description = "Initiate a refund for a completed payment transaction",
        responses = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
        }
    )
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<Object>> processRefund(
            @Valid @RequestBody RefundRequest request) {

        String correlationId = generateCorrelationId();
        log.info("Processing refund: correlationId={}, transactionId={}, amount={}",
                correlationId, request.getTransactionId(), request.getAmount());

        return paymentProcessingService.processRefund(request)
            .thenApply(result -> result.fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, correlationId)
            ));
    }

    /**
     * Get transaction with functional error handling
     * Complexity: 3 (within ≤7 limit)
     */
    @GetMapping("/transaction/{transactionId}")
    @Operation(
        summary = "Get Transaction",
        description = "Retrieve payment transaction details by ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
        }
    )
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Object> getTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {

        String correlationId = generateCorrelationId();
        log.debug("Getting transaction: correlationId={}, transactionId={}", correlationId, transactionId);

        return paymentProcessingService.getTransaction(transactionId)
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.NOT_FOUND, correlationId)
            );
    }

    /**
     * Get payment status with pattern matching
     * Complexity: 3 (within ≤7 limit)
     */
    @GetMapping("/transaction/{transactionId}/status")
    @Operation(
        summary = "Get Payment Status",
        description = "Get current status of a payment transaction",
        responses = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
        }
    )
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Object> getPaymentStatus(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {

        String correlationId = generateCorrelationId();

        return paymentProcessingService.getPaymentStatus(transactionId)
            .map(status -> new PaymentStatusResponse(transactionId, status, getStatusMessage(status)))
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.NOT_FOUND, correlationId)
            );
    }

    /**
     * Get payment history with pagination
     * Complexity: 3 (within ≤7 limit)
     */
    @GetMapping("/user/{userId}/history")
    @Operation(
        summary = "Get Payment History",
        description = "Retrieve paginated payment history for a specific user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Payment history retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied - User can only view own history")
        }
    )
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<Object> getPaymentHistory(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Pageable pageable) {

        String correlationId = generateCorrelationId();
        log.debug("Getting payment history: correlationId={}, userId={}", correlationId, userId);

        return paymentProcessingService.getPaymentHistory(userId, pageable)
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, correlationId)
            );
    }

    /**
     * Retry failed payment
     * Complexity: 4 (within ≤7 limit)
     */
    @PostMapping("/retry/{transactionId}")
    @Operation(
        summary = "Retry Payment",
        description = "Retry a failed payment transaction",
        responses = {
            @ApiResponse(responseCode = "200", description = "Payment retry initiated"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "400", description = "Transaction cannot be retried")
        }
    )
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Object>> retryPayment(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {

        String correlationId = generateCorrelationId();
        log.info("Retrying payment: correlationId={}, transactionId={}", correlationId, transactionId);

        return paymentProcessingService.retryPayment(transactionId)
            .thenApply(result -> result.fold(
                success -> createSuccessResponse(success, HttpStatus.OK, correlationId),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, correlationId)
            ));
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Get status message with pattern matching (NO if-else)
     * Complexity: 1 (within ≤7 limit)
     */
    private String getStatusMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Payment is being processed";
            case PROCESSING -> "Payment is in progress";
            case AUTHORIZED -> "Payment has been authorized";
            case COMPLETED -> "Payment completed successfully";
            case FAILED -> "Payment failed";
            case CANCELLED -> "Payment was cancelled";
            case REFUNDED -> "Payment has been refunded";
            case PARTIALLY_REFUNDED -> "Payment has been partially refunded";
        };
    }

    /**
     * Create success response with correlation ID
     * Complexity: 1 (within ≤7 limit)
     */
    private ResponseEntity<Object> createSuccessResponse(
            Object data,
            HttpStatus status,
            String correlationId
    ) {
        return ResponseEntity
            .status(status)
            .header("X-Correlation-Id", correlationId)
            .body(data);
    }

    /**
     * Create error response with correlation ID
     * Complexity: 1 (within ≤7 limit)
     */
    private ResponseEntity<Object> createErrorResponse(
            String error,
            HttpStatus status,
            String correlationId
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
            status.value(),
            error,
            correlationId,
            Instant.now()
        );

        return ResponseEntity
            .status(status)
            .header("X-Correlation-Id", correlationId)
            .body(errorResponse);
    }

    /**
     * Generate correlation ID for tracing
     * Complexity: 1 (within ≤7 limit)
     */
    private String generateCorrelationId() {
        return "payment-" + UUID.randomUUID().toString();
    }

    // ==================== Response DTOs (Immutable Records) ====================

    /**
     * Payment status response (Rule 9: Records for immutability)
     */
    public record PaymentStatusResponse(
        UUID transactionId,
        PaymentStatus status,
        String message
    ) {}

    /**
     * Error response (Rule 9: Records for immutability)
     */
    public record ErrorResponse(
        int statusCode,
        String message,
        String correlationId,
        Instant timestamp
    ) {}
}
