package com.trademaster.notification.model;

/**
 * Enum representing the type of notification
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum NotificationType {
    EMAIL("Email"),
    SMS("SMS"), 
    PUSH("Push Notification"),
    IN_APP("In-App Notification"),
    WEBHOOK("Webhook");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}