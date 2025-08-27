package com.trademaster.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Refund Response DTO
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private String refundId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String reason;
    private Instant createdAt;
    private Instant processedAt;
    private String gatewayResponse;
    
    public boolean isSuccessful() {
        return "succeeded".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
    }
}