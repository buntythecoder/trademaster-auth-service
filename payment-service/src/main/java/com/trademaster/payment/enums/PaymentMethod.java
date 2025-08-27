package com.trademaster.payment.enums;

import lombok.Getter;

/**
 * Payment Method Enumeration
 * 
 * Represents different payment methods supported by the platform.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum PaymentMethod {
    CARD("Credit/Debit Card", "Traditional card payments"),
    UPI("UPI", "Unified Payments Interface"),
    NETBANKING("Net Banking", "Direct bank transfer"),
    WALLET("Digital Wallet", "Paytm, PhonePe, etc."),
    BNPL("Buy Now Pay Later", "EMI and BNPL options");
    
    private final String displayName;
    private final String description;
    
    PaymentMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Check if the payment method is instant
     */
    public boolean isInstant() {
        return this == CARD || this == UPI || this == WALLET;
    }
    
    /**
     * Check if the payment method requires additional verification
     */
    public boolean requiresVerification() {
        return this == NETBANKING || this == BNPL;
    }
}