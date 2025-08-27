package com.trademaster.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Completed Event
 * 
 * Published when a payment is successfully completed.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    
    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private UUID subscriptionPlanId;
    private Instant timestamp = Instant.now();
    
    public PaymentCompletedEvent(UUID transactionId, UUID userId, BigDecimal amount, 
                                String currency, UUID subscriptionPlanId) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.subscriptionPlanId = subscriptionPlanId;
        this.timestamp = Instant.now();
    }
}