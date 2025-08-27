package com.trademaster.payment.enums;

import lombok.Getter;

/**
 * Payment Status Enumeration
 * 
 * Represents the various states of a payment transaction throughout its lifecycle.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum PaymentStatus {
    PENDING("Payment initiated but not processed"),
    PROCESSING("Payment being processed by gateway"),
    COMPLETED("Payment successfully completed"),
    FAILED("Payment failed"),
    CANCELLED("Payment cancelled by user"),
    REFUNDED("Payment fully refunded"),
    PARTIALLY_REFUNDED("Payment partially refunded");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    /**
     * Check if the payment status indicates a successful completion
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Check if the payment status indicates a failure
     */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED;
    }
    
    /**
     * Check if the payment status indicates it's still processing
     */
    public boolean isProcessing() {
        return this == PENDING || this == PROCESSING;
    }
    
    /**
     * Check if the payment has been refunded (fully or partially)
     */
    public boolean isRefunded() {
        return this == REFUNDED || this == PARTIALLY_REFUNDED;
    }
}