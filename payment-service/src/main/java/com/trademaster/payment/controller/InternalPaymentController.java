package com.trademaster.payment.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.service.PaymentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Internal Payment API Controller
 * Service-to-service communication endpoints with API key authentication
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else, Railway programming with Result types
 * - Rule 6: Zero Trust Security - @PreAuthorize for internal service-to-service calls
 * - Rule 11: Railway programming with Result.fold() for response mapping
 * - Rule 12: Virtual Threads with CompletableFuture for async operations
 * - Rule 15: Structured logging with correlation IDs
 * - Rule 18: Descriptive naming (verifyPaymentStatus)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/internal/v1/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Internal Payment API",
    description = "Internal service-to-service payment operations with API key authentication"
)
@SecurityRequirement(name = "API_KEY")
public class InternalPaymentController {

    private final PaymentProcessingService paymentProcessingService;

    /**
     * Verify payment status for internal service calls
     * Used by Portfolio Service to confirm payment completion
     * Complexity: 4 (within ≤7 limit)
     */
    @Operation(
        summary = "Verify Payment Status",
        description = "Verifies payment completion status for internal service consumption. " +
                     "Used by Portfolio Service to confirm payment before updating positions."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment verification successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentVerificationResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/verify/{paymentId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Object> verifyPaymentStatus(
            @Parameter(description = "Payment transaction ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID paymentId,

            @Parameter(description = "Request correlation ID for tracing")
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = Optional.ofNullable(correlationId).orElse(generateCorrelationId());

        log.info("Internal payment verification requested: paymentId={} | correlation_id={}",
                paymentId, corrId);

        return paymentProcessingService.getTransaction(paymentId)
            .map(transaction -> new PaymentVerificationResponse(
                paymentId.toString(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrency(),
                Instant.now(),
                corrId
            ))
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, corrId),
                error -> createErrorResponse(error, HttpStatus.NOT_FOUND, corrId)
            );
    }

    /**
     * Get payment details for internal service calls
     * Used by Subscription Service to check payment history
     * Complexity: 5 (within ≤7 limit)
     */
    @Operation(
        summary = "Get User Payment Details",
        description = "Retrieves payment history and details for a specific user. " +
                     "Used by Subscription Service to verify payment eligibility."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User payment details retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Object> getUserPaymentDetails(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,

            @Parameter(description = "Request correlation ID for tracing")
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,

            Pageable pageable
    ) {
        String corrId = Optional.ofNullable(correlationId).orElse(generateCorrelationId());

        log.info("Internal user payment details requested: userId={} | correlation_id={}",
                userId, corrId);

        return paymentProcessingService.getPaymentHistory(userId, pageable)
            .map(page -> createUserPaymentSummary(userId, page, corrId))
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, corrId),
                error -> createErrorResponse(error, HttpStatus.NOT_FOUND, corrId)
            );
    }

    /**
     * Initiate refund for internal service calls
     * Used by Trading Service to refund failed trades
     * Complexity: 5 (within ≤7 limit)
     */
    @Operation(
        summary = "Initiate Refund",
        description = "Initiates refund process for failed trades or cancellations. " +
                     "Used by Trading Service for automated refund workflows."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund initiated successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing API key"),
        @ApiResponse(responseCode = "400", description = "Invalid refund request")
    })
    @PostMapping("/refund")
    @PreAuthorize("hasRole('SERVICE')")
    public CompletableFuture<ResponseEntity<Object>> initiateRefund(
            @Parameter(description = "Refund request details")
            @RequestBody RefundRequest refundRequest,

            @Parameter(description = "Request correlation ID for tracing")
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = Optional.ofNullable(correlationId).orElse(generateCorrelationId());

        log.info("Internal refund requested: transactionId={}, amount={} | correlation_id={}",
                refundRequest.getTransactionId(), refundRequest.getAmount(), corrId);

        return paymentProcessingService.processRefund(refundRequest)
            .thenApply(result -> result.fold(
                refundResponse -> createSuccessResponse(
                    new RefundInitiationResponse(
                        refundResponse.getRefundId(),
                        refundResponse.getStatus(),
                        refundResponse.getAmount(),
                        refundResponse.getCurrency(),
                        Instant.now(),
                        corrId
                    ),
                    HttpStatus.OK,
                    corrId
                ),
                error -> createErrorResponse(error, HttpStatus.BAD_REQUEST, corrId)
            ));
    }

    /**
     * Get transaction by gateway payment ID (internal use)
     * Used for webhook processing and reconciliation
     * Complexity: 4 (within ≤7 limit)
     */
    @Operation(
        summary = "Get Transaction by Gateway Payment ID",
        description = "Retrieve transaction details using gateway-specific payment ID. " +
                     "Used for webhook processing and payment reconciliation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/gateway-payment/{gatewayPaymentId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Object> getTransactionByGatewayPaymentId(
            @Parameter(description = "Gateway Payment ID", example = "pay_123abc or pi_456def")
            @PathVariable String gatewayPaymentId,

            @Parameter(description = "Request correlation ID for tracing")
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = Optional.ofNullable(correlationId).orElse(generateCorrelationId());

        log.debug("Getting transaction by gateway payment ID: gatewayPaymentId={} | correlation_id={}",
                gatewayPaymentId, corrId);

        return paymentProcessingService.getTransactionByGatewayPaymentId(gatewayPaymentId)
            .fold(
                success -> createSuccessResponse(success, HttpStatus.OK, corrId),
                error -> createErrorResponse(error, HttpStatus.NOT_FOUND, corrId)
            );
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Create user payment summary from page
     * Functional data transformation (NO loops)
     * Complexity: 3 (within ≤7 limit)
     */
    private UserPaymentSummary createUserPaymentSummary(
            UUID userId,
            Page<PaymentTransaction> page,
            String correlationId
    ) {
        return new UserPaymentSummary(
            userId.toString(),
            (int) page.getTotalElements(),
            page.getContent().stream()
                .findFirst()
                .map(transaction -> new LastPayment(
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getCreatedAt()
                ))
                .orElse(null),
            Instant.now(),
            correlationId
        );
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
        InternalErrorResponse errorResponse = new InternalErrorResponse(
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
        return "internal-" + UUID.randomUUID().toString();
    }

    // ==================== Response DTOs (Immutable Records) ====================

    /**
     * Payment verification response (Rule 9: Records for immutability)
     */
    public record PaymentVerificationResponse(
        String paymentId,
        String status,
        BigDecimal amount,
        String currency,
        Instant timestamp,
        String correlationId
    ) {}

    /**
     * User payment summary (Rule 9: Records for immutability)
     */
    public record UserPaymentSummary(
        String userId,
        int totalPayments,
        LastPayment lastPayment,
        Instant timestamp,
        String correlationId
    ) {}

    /**
     * Last payment details (Rule 9: Records for immutability)
     */
    public record LastPayment(
        BigDecimal amount,
        String currency,
        Instant date
    ) {}

    /**
     * Refund initiation response (Rule 9: Records for immutability)
     */
    public record RefundInitiationResponse(
        String refundId,
        String status,
        BigDecimal amount,
        String currency,
        Instant timestamp,
        String correlationId
    ) {}

    /**
     * Internal error response (Rule 9: Records for immutability)
     */
    public record InternalErrorResponse(
        int statusCode,
        String message,
        String correlationId,
        Instant timestamp
    ) {}
}
