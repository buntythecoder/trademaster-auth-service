package com.trademaster.userprofile.entity;

/**
 * Change Type Enumeration for Audit Logging
 * 
 * MANDATORY: Pattern Matching Excellence - Rule #14
 * MANDATORY: Functional Programming First - Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public enum ChangeType {
    // Basic operations
    CREATE("Entity creation"),
    UPDATE("Entity update"),
    DELETE("Entity deletion"),
    STATUS_CHANGE("Status modification"),
    SECURITY_UPDATE("Security-related update"),
    SYSTEM_UPDATE("System-generated update"),
    
    // Authentication and session management
    LOGIN("User login"),
    LOGOUT("User logout"),
    PASSWORD_CHANGE("Password change"),
    TWO_FACTOR_ENABLE("Two-factor authentication enabled"),
    TWO_FACTOR_DISABLE("Two-factor authentication disabled"),
    
    // Profile management
    PROFILE_CREATE("Profile creation"),
    PROFILE_UPDATE("Profile update"),
    PROFILE_ACTIVATE("Profile activation"),
    PROFILE_DEACTIVATE("Profile deactivation"),
    PROFILE_DELETE("Profile deletion"),
    
    // KYC operations
    KYC_SUBMIT("KYC submission"),
    KYC_VERIFY("KYC verification"),
    KYC_APPROVE("KYC approval"),
    KYC_REJECT("KYC rejection"),
    KYC_EXPIRE("KYC expiration"),
    
    // Document operations
    DOCUMENT_UPLOAD("Document upload"),
    DOCUMENT_UPDATE("Document update"),
    DOCUMENT_DELETE("Document deletion"),
    DOCUMENT_VERIFY("Document verification"),
    DOCUMENT_APPROVE("Document approval"),
    DOCUMENT_REJECT("Document rejection"),
    
    // Preferences and settings
    PREFERENCES_UPDATE("Preferences update"),
    NOTIFICATION_SETTINGS_UPDATE("Notification settings update"),
    PRIVACY_SETTINGS_UPDATE("Privacy settings update"),
    TRADING_SETTINGS_UPDATE("Trading settings update"),
    
    // Risk and compliance
    RISK_PROFILE_UPDATE("Risk profile update"),
    COMPLIANCE_CHECK("Compliance check"),
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
    
    // Data operations
    DATA_EXPORT("Data export"),
    DATA_IMPORT("Data import"),
    DATA_MIGRATION("Data migration"),
    DATA_BACKUP("Data backup"),
    
    // System events
    SYSTEM_MAINTENANCE("System maintenance"),
    SYSTEM_UPGRADE("System upgrade"),
    SYSTEM_ERROR("System error"),
    SYSTEM_RECOVERY("System recovery");
    
    private final String description;
    
    ChangeType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get display name for UI/logging - same as description for now
     */
    public String getDisplayName() {
        return description;
    }
    
    /**
     * Check if this change type is sensitive and requires special handling
     */
    public boolean isSensitive() {
        return switch (this) {
            case LOGIN, LOGOUT, PASSWORD_CHANGE, TWO_FACTOR_ENABLE, TWO_FACTOR_DISABLE,
                 SECURITY_UPDATE, KYC_SUBMIT, KYC_VERIFY, KYC_APPROVE, KYC_REJECT,
                 DOCUMENT_UPLOAD, DOCUMENT_VERIFY, DOCUMENT_APPROVE, DOCUMENT_REJECT,
                 SUSPICIOUS_ACTIVITY, DELETE, PROFILE_DEACTIVATE, PROFILE_DELETE -> true;
            default -> false;
        };
    }
    
    /**
     * Parse change type from string using pattern matching
     */
    public static ChangeType fromString(String value) {
        return switch (value.toUpperCase()) {
            case "CREATE", "CREATED", "INSERT" -> CREATE;
            case "UPDATE", "UPDATED", "MODIFY", "MODIFIED" -> UPDATE;
            case "DELETE", "DELETED", "REMOVE", "REMOVED" -> DELETE;
            case "STATUS_CHANGE", "STATUS", "STATE_CHANGE" -> STATUS_CHANGE;
            case "SECURITY_UPDATE", "SECURITY" -> SECURITY_UPDATE;
            case "SYSTEM_UPDATE", "SYSTEM", "AUTOMATED" -> SYSTEM_UPDATE;
            case "LOGIN", "SIGN_IN" -> LOGIN;
            case "LOGOUT", "SIGN_OUT" -> LOGOUT;
            case "PASSWORD_CHANGE", "PASSWORD_UPDATE" -> PASSWORD_CHANGE;
            case "KYC_SUBMIT", "KYC_SUBMISSION" -> KYC_SUBMIT;
            case "KYC_VERIFY", "KYC_VERIFICATION" -> KYC_VERIFY;
            case "KYC_APPROVE", "KYC_APPROVAL" -> KYC_APPROVE;
            case "KYC_REJECT", "KYC_REJECTION" -> KYC_REJECT;
            case "DOCUMENT_UPLOAD", "DOC_UPLOAD" -> DOCUMENT_UPLOAD;
            case "DOCUMENT_VERIFY", "DOC_VERIFY" -> DOCUMENT_VERIFY;
            case "DOCUMENT_APPROVE", "DOC_APPROVE" -> DOCUMENT_APPROVE;
            case "DOCUMENT_REJECT", "DOC_REJECT" -> DOCUMENT_REJECT;
            case "PROFILE_ACTIVATE", "ACTIVATE" -> PROFILE_ACTIVATE;
            case "PROFILE_DEACTIVATE", "DEACTIVATE" -> PROFILE_DEACTIVATE;
            default -> UPDATE; // Safe default
        };
    }
    
    /**
     * Check if this change type requires elevated permissions
     */
    public boolean requiresElevatedPermissions() {
        return switch (this) {
            case DELETE, SECURITY_UPDATE, PROFILE_DELETE, PROFILE_DEACTIVATE,
                 PASSWORD_CHANGE, TWO_FACTOR_ENABLE, TWO_FACTOR_DISABLE,
                 KYC_APPROVE, KYC_REJECT, DOCUMENT_APPROVE, DOCUMENT_REJECT,
                 SUSPICIOUS_ACTIVITY, DATA_EXPORT, SYSTEM_MAINTENANCE -> true;
            default -> false;
        };
    }
    
    /**
     * Get audit severity level
     */
    public AuditSeverity getAuditSeverity() {
        return switch (this) {
            // Critical security events
            case SUSPICIOUS_ACTIVITY, PROFILE_DELETE, DATA_EXPORT -> AuditSeverity.CRITICAL;
            
            // High severity events
            case DELETE, SECURITY_UPDATE, PASSWORD_CHANGE, TWO_FACTOR_DISABLE,
                 KYC_REJECT, DOCUMENT_REJECT, PROFILE_DEACTIVATE, SYSTEM_ERROR -> AuditSeverity.HIGH;
            
            // Medium severity events  
            case LOGIN, LOGOUT, KYC_SUBMIT, KYC_VERIFY, KYC_APPROVE,
                 DOCUMENT_UPLOAD, DOCUMENT_VERIFY, DOCUMENT_APPROVE,
                 TWO_FACTOR_ENABLE, PROFILE_ACTIVATE -> AuditSeverity.MEDIUM;
            
            // Low severity events
            case UPDATE, STATUS_CHANGE, SYSTEM_UPDATE, PREFERENCES_UPDATE,
                 NOTIFICATION_SETTINGS_UPDATE, PRIVACY_SETTINGS_UPDATE,
                 TRADING_SETTINGS_UPDATE, RISK_PROFILE_UPDATE,
                 COMPLIANCE_CHECK, DATA_MIGRATION, DATA_BACKUP,
                 SYSTEM_MAINTENANCE, SYSTEM_UPGRADE, SYSTEM_RECOVERY,
                 DOCUMENT_DELETE -> AuditSeverity.LOW;
            
            // Info level events
            case CREATE, PROFILE_CREATE, PROFILE_UPDATE, DOCUMENT_UPDATE,
                 KYC_EXPIRE, DATA_IMPORT -> AuditSeverity.INFO;
        };
    }
    
    public enum AuditSeverity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }
}
