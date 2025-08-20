package com.trademaster.userprofile.entity;

/**
 * Granular permissions for fine-grained access control.
 * Used in conjunction with roles for comprehensive RBAC.
 */
public enum Permission {
    
    // Profile Management Permissions
    PROFILE_READ_OWN("Read own profile information"),
    PROFILE_WRITE_OWN("Modify own profile information"),
    PROFILE_DELETE_OWN("Delete own profile"),
    
    PROFILE_READ_ANY("Read any user's profile information"),
    PROFILE_WRITE_ANY("Modify any user's profile information"),
    PROFILE_DELETE_ANY("Delete any user's profile"),
    
    // KYC Management Permissions
    KYC_READ_OWN("Read own KYC information"),
    KYC_WRITE_OWN("Modify own KYC information"),
    KYC_READ_ANY("Read any user's KYC information"),
    KYC_VERIFY("Verify KYC documents and status"),
    KYC_APPROVE("Approve KYC applications"),
    KYC_REJECT("Reject KYC applications"),
    
    // Document Management Permissions
    DOCUMENT_READ_OWN("Read own documents"),
    DOCUMENT_UPLOAD_OWN("Upload own documents"),
    DOCUMENT_DELETE_OWN("Delete own documents"),
    DOCUMENT_READ_ANY("Read any user's documents"),
    DOCUMENT_VERIFY("Verify document authenticity"),
    
    // Trading Permissions
    TRADING_BASIC("Basic trading operations"),
    TRADING_ADVANCED("Advanced trading features"),
    TRADING_MARGIN("Margin trading"),
    TRADING_DERIVATIVES("Derivatives trading"),
    
    // Administrative Permissions
    USER_SEARCH("Search and filter users"),
    USER_STATISTICS("View user statistics and reports"),
    USER_BULK_OPERATIONS("Perform bulk operations on users"),
    
    // System Permissions
    SYSTEM_CONFIG("Configure system settings"),
    SYSTEM_MONITORING("Monitor system health and performance"),
    SYSTEM_MAINTENANCE("Perform system maintenance operations"),
    
    // Compliance Permissions
    COMPLIANCE_REPORTS("Generate compliance reports"),
    COMPLIANCE_AUDIT("Access audit logs and trails"),
    COMPLIANCE_INVESTIGATION("Investigate compliance issues");
    
    private final String description;
    
    Permission(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this is a self-service permission (operates on own data only)
     */
    public boolean isSelfService() {
        return name().contains("_OWN");
    }
    
    /**
     * Check if this is an administrative permission
     */
    public boolean isAdministrative() {
        return name().startsWith("USER_") || name().startsWith("SYSTEM_") || name().contains("_ANY");
    }
    
    /**
     * Check if this permission relates to compliance
     */
    public boolean isComplianceRelated() {
        return name().startsWith("KYC_") || name().startsWith("COMPLIANCE_") || 
               name().contains("VERIFY") || name().contains("APPROVE");
    }
}