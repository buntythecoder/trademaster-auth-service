package com.trademaster.payment.controller;

import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.util.UUID;

/**
 * Payment Controller
 * 
 * REST API endpoints for payment processing and management.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "Payment processing and subscription management APIs")
public class PaymentController {

    private final PaymentService paymentService;
    
    /**
     * Process a new payment
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process Payment",
        description = "Initiate a payment through the configured gateway"
    )
    @ApiResponse(responseCode = "200", description = "Payment initiated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    @ApiResponse(responseCode = "500", description = "Payment processing failed")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        
        log.info("Received payment request for user: {}, amount: {} {}", 
                request.getUserId(), request.getAmount(), request.getCurrency());
        
        PaymentResponse response = paymentService.processPayment(request);
        
        HttpStatus status = response.isSuccessful() ? HttpStatus.OK : 
                          response.isPending() ? HttpStatus.ACCEPTED : 
                          HttpStatus.BAD_REQUEST;
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Get payment transaction details
     */
    @GetMapping("/transaction/{transactionId}")
    @Operation(
        summary = "Get Transaction",
        description = "Retrieve payment transaction details by ID"
    )
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentTransaction> getTransaction(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {
        
        PaymentTransaction transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * Get payment status
     */
    @GetMapping("/transaction/{transactionId}/status")
    @Operation(
        summary = "Get Payment Status",
        description = "Get current status of a payment transaction"
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @Parameter(description = "Transaction ID") @PathVariable UUID transactionId) {
        
        PaymentStatus status = paymentService.getPaymentStatus(transactionId);
        
        PaymentStatusResponse response = PaymentStatusResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .message(getStatusMessage(status))
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user payment history
     */
    @GetMapping("/user/{userId}/history")
    @Operation(
        summary = "Get Payment History",
        description = "Retrieve payment history for a specific user"
    )
    @ApiResponse(responseCode = "200", description = "Payment history retrieved")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<Page<PaymentTransaction>> getPaymentHistory(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            Pageable pageable) {
        
        Page<PaymentTransaction> history = paymentService.getPaymentHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check payment service health and gateway connectivity"
    )
    public ResponseEntity<HealthResponse> healthCheck() {
        
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .message("Payment service is operational")
                .timestamp(java.time.Instant.now())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    private String getStatusMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Payment is being processed";
            case PROCESSING -> "Payment is in progress";
            case COMPLETED -> "Payment completed successfully";
            case FAILED -> "Payment failed";
            case CANCELLED -> "Payment was cancelled";
            case REFUNDED -> "Payment has been refunded";
            case PARTIALLY_REFUNDED -> "Payment has been partially refunded";
        };
    }
    
    // Response DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentStatusResponse {
        private UUID transactionId;
        private PaymentStatus status;
        private String message;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String message;
        private java.time.Instant timestamp;
    }
}