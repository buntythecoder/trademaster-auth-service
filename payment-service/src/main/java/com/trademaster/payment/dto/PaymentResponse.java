package com.trademaster.payment.dto;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Response DTO
 * 
 * Response object containing payment processing results.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID transactionId;
    private PaymentStatus status;
    private String message;
    
    // Payment details
    private BigDecimal amount;
    private String currency;
    private PaymentGateway gateway;
    
    // Gateway-specific response data
    private GatewayResponse gatewayResponse;
    
    // Additional information
    private Instant createdAt;
    private Instant processedAt;
    private String receiptNumber;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GatewayResponse {
        private String orderId;
        private String paymentId;
        private String signature;
        
        // For frontend integration
        private String clientSecret; // Stripe
        private String razorpayOrderId; // Razorpay
        
        // Gateway-specific data
        private Map<String, Object> additionalData;
        
        // Payment URL for redirect-based payments
        private String paymentUrl;
        
        // QR code data for UPI payments
        private String qrCodeData;
    }
    
    // Factory methods for different response types
    public static PaymentResponse success(UUID transactionId, BigDecimal amount, String currency) {
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.COMPLETED)
                .message("Payment completed successfully")
                .amount(amount)
                .currency(currency)
                .processedAt(Instant.now())
                .build();
    }
    
    public static PaymentResponse pending(UUID transactionId, BigDecimal amount, String currency, 
                                        GatewayResponse gatewayResponse) {
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.PENDING)
                .message("Payment initiated successfully")
                .amount(amount)
                .currency(currency)
                .gatewayResponse(gatewayResponse)
                .createdAt(Instant.now())
                .build();
    }
    
    public static PaymentResponse failed(String message) {
        return PaymentResponse.builder()
                .status(PaymentStatus.FAILED)
                .message(message)
                .createdAt(Instant.now())
                .build();
    }
    
    // Helper methods
    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED;
    }
}