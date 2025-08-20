package com.trademaster.userprofile.entity;

/**
 * User roles in the TradeMaster system with hierarchical permissions.
 * Roles are ordered by permission level (least to most privileged).
 */
public enum Role {
    
    /**
     * Basic authenticated user - can manage own profile and trading activities
     */
    USER("Basic user with standard trading permissions"),
    
    /**
     * Premium user with enhanced features and higher trading limits
     */
    PREMIUM_USER("Premium user with enhanced trading features"),
    
    /**
     * Customer support agent - can view user profiles for assistance
     */
    SUPPORT_AGENT("Customer support representative"),
    
    /**
     * Compliance officer - can access KYC data and compliance reports
     */
    COMPLIANCE_OFFICER("Compliance and regulatory oversight"),
    
    /**
     * System administrator - full access to all system functions
     */
    ADMIN("System administrator with full access");
    
    private final String description;
    
    Role(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this role has at least the same privilege level as the target role
     */
    public boolean hasPrivilegeLevel(Role targetRole) {
        return this.ordinal() >= targetRole.ordinal();
    }
    
    /**
     * Check if this role can access admin functions
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    /**
     * Check if this role can access compliance functions
     */
    public boolean canAccessCompliance() {
        return this == COMPLIANCE_OFFICER || this == ADMIN;
    }
    
    /**
     * Check if this role can access support functions
     */
    public boolean canProvideSupport() {
        return this == SUPPORT_AGENT || this == COMPLIANCE_OFFICER || this == ADMIN;
    }
    
    /**
     * Check if this role has premium features
     */
    public boolean isPremium() {
        return this.ordinal() >= PREMIUM_USER.ordinal();
    }
}