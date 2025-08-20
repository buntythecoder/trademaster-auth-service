package com.trademaster.userprofile.entity;

public enum VerificationStatus {
    PENDING("Pending", "Document uploaded, awaiting verification"),
    IN_PROGRESS("In Progress", "Document is being verified"),
    VERIFIED("Verified", "Document successfully verified"),
    REJECTED("Rejected", "Document rejected due to quality or authenticity issues"),
    EXPIRED("Expired", "Document verification expired, re-upload required");
    
    private final String displayName;
    private final String description;
    
    VerificationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == VERIFIED;
    }
    
    public boolean requiresAction() {
        return this == REJECTED || this == EXPIRED;
    }
    
    public boolean isProcessing() {
        return this == PENDING || this == IN_PROGRESS;
    }
}