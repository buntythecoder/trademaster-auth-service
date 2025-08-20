package com.trademaster.userprofile.entity;

import lombok.Builder;

@Builder
public record NotificationSettings(
    // Email notifications
    boolean emailEnabled,
    boolean tradeExecutionEmails,
    boolean priceAlertEmails,
    boolean newsAndUpdatesEmails,
    boolean portfolioSummaryEmails,
    
    // SMS notifications
    boolean smsEnabled,
    boolean tradeExecutionSms,
    boolean priceAlertSms,
    boolean marginCallSms,
    
    // Push notifications
    boolean pushEnabled,
    boolean tradeExecutionPush,
    boolean priceAlertPush,
    boolean newsAndUpdatesPush,
    boolean marketHoursPush,
    
    // Frequency settings
    String emailFrequency, // IMMEDIATE, DAILY, WEEKLY
    String smsFrequency,
    String pushFrequency,
    
    // Quiet hours
    boolean quietHoursEnabled,
    String quietHoursStart, // HH:mm format
    String quietHoursEnd
) {
    
    public boolean hasAnyNotificationEnabled() {
        return emailEnabled || smsEnabled || pushEnabled;
    }
    
    public boolean isQuietTime(java.time.LocalTime currentTime) {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        
        try {
            java.time.LocalTime start = java.time.LocalTime.parse(quietHoursStart);
            java.time.LocalTime end = java.time.LocalTime.parse(quietHoursEnd);
            
            if (start.isBefore(end)) {
                return !currentTime.isBefore(start) && !currentTime.isAfter(end);
            } else {
                // Handles overnight quiet hours (e.g., 22:00 to 06:00)
                return !currentTime.isAfter(end) || !currentTime.isBefore(start);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean shouldSendTradeNotification(String channel) {
        return switch (channel.toLowerCase()) {
            case "email" -> emailEnabled && tradeExecutionEmails;
            case "sms" -> smsEnabled && tradeExecutionSms;
            case "push" -> pushEnabled && tradeExecutionPush;
            default -> false;
        };
    }
}