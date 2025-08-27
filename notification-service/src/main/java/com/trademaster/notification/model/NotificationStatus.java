package com.trademaster.notification.model;

/**
 * Enum representing the status of a notification
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum NotificationStatus {
    PENDING("Pending"),
    SENT("Sent"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    PROCESSING("Processing");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}