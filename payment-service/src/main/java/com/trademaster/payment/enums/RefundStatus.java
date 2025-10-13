package com.trademaster.payment.enums;

/**
 * Refund Status Enum
 * Represents the lifecycle status of a refund operation
 *
 * Compliance:
 * - Rule 14: Pattern matching with sealed types
 * - Financial Domain: Clear state transitions for refund lifecycle
 *
 * Status Flow:
 * PENDING → PROCESSING → SUCCESS
 * PENDING → PROCESSING → FAILED
 * SUCCESS → REVERSED (rare case)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum RefundStatus {

    /**
     * Refund request initiated, awaiting processing
     */
    PENDING("Pending", "Refund request has been initiated and is awaiting processing"),

    /**
     * Refund is being processed by payment gateway
     */
    PROCESSING("Processing", "Refund is currently being processed"),

    /**
     * Refund completed successfully, funds returned to customer
     */
    SUCCESS("Success", "Refund completed successfully"),

    /**
     * Refund processing failed
     */
    FAILED("Failed", "Refund processing failed"),

    /**
     * Refund was cancelled before completion
     */
    CANCELLED("Cancelled", "Refund was cancelled"),

    /**
     * Refund was reversed (rare case for chargebacks/disputes)
     */
    REVERSED("Reversed", "Refund was reversed");

    private final String displayName;
    private final String description;

    RefundStatus(String displayName, String description) {
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
     * Check if refund is in a terminal state (cannot be changed)
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == REVERSED;
    }

    /**
     * Check if refund is in a processing state
     */
    public boolean isProcessing() {
        return this == PENDING || this == PROCESSING;
    }
}
