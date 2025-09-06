package com.trademaster.userprofile.entity;

/**
 * Entity Type Enumeration for Audit and System References
 * 
 * MANDATORY: Pattern Matching Excellence - Rule #14
 * MANDATORY: Functional Programming First - Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public enum EntityType {
    USER_PROFILE("User Profile"),
    USER_PREFERENCES("User Preferences"), 
    USER_DOCUMENT("User Document"),
    KYC_INFORMATION("KYC Information"),
    NOTIFICATION_SETTINGS("Notification Settings"),
    TRADING_PREFERENCES("Trading Preferences"),
    SUBSCRIPTION("Subscription"),
    AUDIT_LOG("Audit Log");
    
    private final String displayName;
    
    EntityType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Parse entity type from string using pattern matching
     */
    public static EntityType fromString(String value) {
        return switch (value.toUpperCase().replace(" ", "_")) {
            case "USER_PROFILE", "PROFILE" -> USER_PROFILE;
            case "USER_PREFERENCES", "PREFERENCES" -> USER_PREFERENCES;
            case "USER_DOCUMENT", "DOCUMENT" -> USER_DOCUMENT;
            case "KYC_INFORMATION", "KYC" -> KYC_INFORMATION;
            case "NOTIFICATION_SETTINGS", "NOTIFICATIONS" -> NOTIFICATION_SETTINGS;
            case "TRADING_PREFERENCES", "TRADING" -> TRADING_PREFERENCES;
            case "SUBSCRIPTION", "SUB" -> SUBSCRIPTION;
            case "AUDIT_LOG", "AUDIT" -> AUDIT_LOG;
            default -> USER_PROFILE; // Safe default
        };
    }
    
    /**
     * Get the entity class name for this type
     */
    public String getEntityClassName() {
        return switch (this) {
            case USER_PROFILE -> "UserProfile";
            case USER_PREFERENCES -> "UserPreferences";
            case USER_DOCUMENT -> "UserDocument";
            case KYC_INFORMATION -> "KYCInformation";
            case NOTIFICATION_SETTINGS -> "NotificationSettings";
            case TRADING_PREFERENCES -> "TradingPreferences";
            case SUBSCRIPTION -> "Subscription";
            case AUDIT_LOG -> "ProfileAuditLog";
        };
    }
    
    /**
     * Check if this entity type contains sensitive data
     */
    public boolean containsSensitiveData() {
        return switch (this) {
            case USER_PROFILE, KYC_INFORMATION, USER_DOCUMENT -> true;
            case USER_PREFERENCES, NOTIFICATION_SETTINGS, TRADING_PREFERENCES, 
                 SUBSCRIPTION, AUDIT_LOG -> false;
        };
    }
    
    /**
     * Check if this entity type requires audit logging
     */
    public boolean requiresAuditLogging() {
        return switch (this) {
            case USER_PROFILE, KYC_INFORMATION, USER_DOCUMENT, SUBSCRIPTION -> true;
            case USER_PREFERENCES, NOTIFICATION_SETTINGS, TRADING_PREFERENCES, 
                 AUDIT_LOG -> false;
        };
    }
}
