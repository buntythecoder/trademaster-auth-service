package com.trademaster.payment.enums;

import lombok.Getter;

/**
 * Payment Gateway Enumeration
 * 
 * Supported payment gateways for processing transactions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum PaymentGateway {
    RAZORPAY("Razorpay", "Primary Indian payment gateway", "INR"),
    STRIPE("Stripe", "International payment gateway", "USD"),
    UPI("UPI", "Unified Payments Interface", "INR");
    
    private final String displayName;
    private final String description;
    private final String defaultCurrency;
    
    PaymentGateway(String displayName, String description, String defaultCurrency) {
        this.displayName = displayName;
        this.description = description;
        this.defaultCurrency = defaultCurrency;
    }
    
    /**
     * Check if the gateway supports international payments
     */
    public boolean isInternational() {
        return this == STRIPE;
    }
    
    /**
     * Check if the gateway is primarily for Indian market
     */
    public boolean isDomestic() {
        return this == RAZORPAY || this == UPI;
    }
}