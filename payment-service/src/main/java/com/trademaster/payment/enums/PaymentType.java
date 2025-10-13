package com.trademaster.payment.enums;

/**
 * Payment Type Enum
 * Classifies payment transactions by purpose and billing model
 *
 * Compliance:
 * - Rule 14: Pattern matching with sealed types
 * - Financial Domain: Clear categorization for reporting and analytics
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum PaymentType {

    /**
     * One-time payment for immediate services/products
     */
    ONE_TIME("One-Time", "Single payment transaction"),

    /**
     * Recurring payment for subscription service
     */
    SUBSCRIPTION("Subscription", "Recurring payment for subscription billing"),

    /**
     * Top-up payment for adding funds to wallet/account
     */
    TOP_UP("Top-Up", "Adding funds to account balance"),

    /**
     * Pre-authorization to hold funds without immediate capture
     */
    AUTHORIZATION("Authorization", "Pre-authorization to hold funds"),

    /**
     * Deposit payment requiring later settlement
     */
    DEPOSIT("Deposit", "Deposit payment for future service"),

    /**
     * Refund payment (reverse transaction)
     */
    REFUND("Refund", "Refund of previous payment"),

    /**
     * Fee payment for service charges
     */
    FEE("Fee", "Service or transaction fee payment");

    private final String displayName;
    private final String description;

    PaymentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a recurring payment type
     */
    public boolean isRecurring() {
        return this == SUBSCRIPTION;
    }

    /**
     * Check if this type requires immediate capture
     */
    public boolean requiresImmediateCapture() {
        return this == ONE_TIME || this == TOP_UP || this == FEE;
    }
}
