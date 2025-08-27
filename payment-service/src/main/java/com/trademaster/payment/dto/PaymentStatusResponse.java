package com.trademaster.payment.dto;

import com.trademaster.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Status Response DTO
 * 
 * Response object for payment status queries.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {
    
    private UUID transactionId;
    private PaymentStatus status;
    private String message;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private Instant processedAt;
    private String receiptNumber;
    private String gatewayOrderId;
    private String gatewayPaymentId;
}