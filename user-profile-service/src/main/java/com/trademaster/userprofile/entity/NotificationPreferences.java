package com.trademaster.userprofile.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Notification preferences for trading activities
 * 
 * MANDATORY: Functional Programming First - Rule #3
 * MANDATORY: Immutability & Records Usage - Rule #9
 * MANDATORY: TradeMaster Compliance
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Builder
public record NotificationPreferences(
    @NotNull(message = "Email notifications flag is required")
    Boolean emailEnabled,
    
    @NotNull(message = "SMS notifications flag is required") 
    Boolean smsEnabled,
    
    @NotNull(message = "Push notifications flag is required")
    Boolean pushEnabled,
    
    @NotNull(message = "Trade confirmation notifications flag is required")
    Boolean tradeConfirmationsEnabled,
    
    @NotNull(message = "Order status notifications flag is required")
    Boolean orderStatusEnabled,
    
    @NotNull(message = "Price alerts flag is required")
    Boolean priceAlertsEnabled,
    
    @NotNull(message = "Market news notifications flag is required")
    Boolean marketNewsEnabled,
    
    @NotNull(message = "Account alerts flag is required")
    Boolean accountAlertsEnabled,
    
    String emailAddress,
    String phoneNumber,
    String preferredTimeZone
) {
    
    /**
     * Check if any notification type is enabled
     */
    public boolean hasAnyNotificationsEnabled() {
        return Boolean.TRUE.equals(emailEnabled) || 
               Boolean.TRUE.equals(smsEnabled) || 
               Boolean.TRUE.equals(pushEnabled);
    }
    
    /**
     * Check if trading notifications are enabled
     */
    public boolean hasTradingNotificationsEnabled() {
        return Boolean.TRUE.equals(tradeConfirmationsEnabled) ||
               Boolean.TRUE.equals(orderStatusEnabled) ||
               Boolean.TRUE.equals(priceAlertsEnabled);
    }
    
    /**
     * Check if contact information is provided for enabled channels
     */
    public boolean hasValidContactInfo() {
        boolean valid = true;
        
        if (Boolean.TRUE.equals(emailEnabled) && (emailAddress == null || emailAddress.trim().isEmpty())) {
            valid = false;
        }
        
        if (Boolean.TRUE.equals(smsEnabled) && (phoneNumber == null || phoneNumber.trim().isEmpty())) {
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Get active notification channels
     */
    public java.util.Set<String> getActiveChannels() {
        java.util.Set<String> channels = new java.util.HashSet<>();
        
        if (Boolean.TRUE.equals(emailEnabled)) {
            channels.add("EMAIL");
        }
        
        if (Boolean.TRUE.equals(smsEnabled)) {
            channels.add("SMS");
        }
        
        if (Boolean.TRUE.equals(pushEnabled)) {
            channels.add("PUSH");
        }
        
        return channels;
    }
}