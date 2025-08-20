package com.trademaster.userprofile.entity;

import lombok.Builder;

import java.time.Instant;

@Builder
public record KYCInformation(
    KYCStatus kycStatus,
    Instant kycSubmittedAt,
    Instant kycVerifiedAt,
    String kycVerificationId,
    
    // PAN verification
    boolean panVerified,
    Instant panVerifiedAt,
    String panVerificationId,
    String panName, // Name as per PAN card
    
    // Aadhaar verification
    boolean aadhaarVerified,
    Instant aadhaarVerifiedAt,
    String aadhaarVerificationId,
    String aadhaarName, // Name as per Aadhaar
    
    // Bank account verification
    boolean bankAccountVerified,
    String bankAccountNumber,
    String bankName,
    String ifscCode,
    String bankAccountType,
    
    // Document verification
    boolean documentsUploaded,
    boolean documentsVerified,
    Instant documentsVerifiedAt,
    
    // Video KYC (future)
    boolean videoKycCompleted,
    Instant videoKycCompletedAt,
    String videoKycId,
    
    // Additional information
    String incomeRange,
    String netWorthRange,
    String politicalExposure,
    String sourceOfFunds,
    
    // Compliance flags
    boolean cddCompleted, // Customer Due Diligence
    boolean fatcaCompleted, // Foreign Account Tax Compliance Act
    boolean uboDeclarationCompleted, // Ultimate Beneficial Owner
    
    String remarks,
    String rejectionReason
) {
    
    public boolean isKYCComplete() {
        return kycStatus == KYCStatus.VERIFIED && 
               panVerified && 
               aadhaarVerified && 
               documentsVerified &&
               bankAccountVerified;
    }
    
    public boolean isPendingVerification() {
        return kycStatus == KYCStatus.PENDING || kycStatus == KYCStatus.UNDER_REVIEW;
    }
    
    public boolean canTrade() {
        return isKYCComplete() && cddCompleted;
    }
    
    public int getKYCCompletionPercentage() {
        int totalSteps = 6; // PAN, Aadhaar, Documents, Bank, CDD, FATCA
        int completedSteps = 0;
        
        if (panVerified) completedSteps++;
        if (aadhaarVerified) completedSteps++;
        if (documentsVerified) completedSteps++;
        if (bankAccountVerified) completedSteps++;
        if (cddCompleted) completedSteps++;
        if (fatcaCompleted) completedSteps++;
        
        return (completedSteps * 100) / totalSteps;
    }
}

enum KYCStatus {
    NOT_STARTED("Not Started", "KYC process not initiated"),
    IN_PROGRESS("In Progress", "KYC documents being collected"),
    PENDING("Pending", "KYC submitted, waiting for verification"),
    UNDER_REVIEW("Under Review", "KYC being reviewed by compliance team"),
    VERIFIED("Verified", "KYC successfully completed"),
    REJECTED("Rejected", "KYC rejected due to insufficient/invalid documents"),
    EXPIRED("Expired", "KYC verification expired, re-verification required");
    
    private final String displayName;
    private final String description;
    
    KYCStatus(String displayName, String description) {
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
        return this == NOT_STARTED || this == REJECTED || this == EXPIRED;
    }
}