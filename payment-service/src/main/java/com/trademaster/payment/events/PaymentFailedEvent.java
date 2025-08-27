package com.trademaster.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Failed Event
 * 
 * Published when a payment fails.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private Instant timestamp = Instant.now();
    
    public PaymentFailedEvent(UUID userId, BigDecimal amount, String currency, String failureReason) {
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.failureReason = failureReason;
        this.timestamp = Instant.now();
    }
}