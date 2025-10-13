package com.trademaster.payment.enums;

/**
 * Payment Method Type Enum
 * Defines the type of payment instrument used for transactions
 *
 * Compliance:
 * - Rule 14: Pattern matching with sealed types
 * - PCI DSS: Different security requirements per payment method type
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum PaymentMethodType {

    /**
     * Credit card payment (Visa, Mastercard, Amex, etc.)
     * PCI DSS Level 1 compliance required
     */
    CREDIT_CARD("Credit Card", "Credit card payment", true),

    /**
     * Debit card payment
     * PCI DSS Level 1 compliance required
     */
    DEBIT_CARD("Debit Card", "Debit card payment", true),

    /**
     * UPI (Unified Payments Interface) - India
     * Real-time bank transfer
     */
    UPI("UPI", "Unified Payments Interface", false),

    /**
     * Net banking direct bank transfer
     */
    NET_BANKING("Net Banking", "Direct bank transfer", false),

    /**
     * Digital wallet (Paytm, PhonePe, Google Pay, etc.)
     */
    WALLET("Wallet", "Digital wallet payment", false),

    /**
     * Bank account direct debit/ACH
     */
    BANK_ACCOUNT("Bank Account", "Direct bank account debit", false),

    /**
     * Cash payment (for offline transactions)
     */
    CASH("Cash", "Cash payment", false),

    /**
     * Cryptocurrency payment
     */
    CRYPTO("Cryptocurrency", "Cryptocurrency payment", false);

    private final String displayName;
    private final String description;
    private final boolean requiresCardData;

    PaymentMethodType(String displayName, String description, boolean requiresCardData) {
        this.displayName = displayName;
        this.description = description;
        this.requiresCardData = requiresCardData;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this payment method requires PCI DSS compliant card data storage
     */
    public boolean requiresCardData() {
        return requiresCardData;
    }

    /**
     * Check if this is a card-based payment method
     */
    public boolean isCard() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }

    /**
     * Check if this payment method supports recurring payments
     */
    public boolean supportsRecurring() {
        return this == CREDIT_CARD || this == DEBIT_CARD || this == BANK_ACCOUNT || this == UPI;
    }
}
