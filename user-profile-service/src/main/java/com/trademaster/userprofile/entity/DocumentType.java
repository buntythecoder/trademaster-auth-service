package com.trademaster.userprofile.entity;

public enum DocumentType {
    // Identity Documents
    PAN_CARD("PAN Card", "Permanent Account Number Card", true, 10485760), // 10MB
    AADHAAR_CARD("Aadhaar Card", "Unique Identification Document", true, 10485760),
    PASSPORT("Passport", "International Identity Document", false, 10485760),
    DRIVING_LICENSE("Driving License", "Motor Vehicle License", false, 10485760),
    VOTER_ID("Voter ID", "Election Commission Identity Card", false, 10485760),
    
    // Address Proof Documents
    UTILITY_BILL("Utility Bill", "Electricity/Water/Gas Bill", false, 5242880), // 5MB
    BANK_STATEMENT("Bank Statement", "Bank Account Statement", false, 10485760),
    RENT_AGREEMENT("Rent Agreement", "House Rent Agreement", false, 10485760),
    PROPERTY_DOCUMENT("Property Document", "Property Ownership Document", false, 20971520), // 20MB
    
    // Income Proof Documents
    SALARY_SLIP("Salary Slip", "Monthly Salary Certificate", false, 5242880),
    ITR("Income Tax Return", "Annual Income Tax Return", false, 10485760),
    FORM_16("Form 16", "Tax Deduction Certificate", false, 5242880),
    BUSINESS_PROOF("Business Proof", "Business Registration/License", false, 10485760),
    
    // Financial Documents
    BANK_ACCOUNT_PROOF("Bank Account Proof", "Bank Account Verification", false, 5242880),
    CANCELLED_CHEQUE("Cancelled Cheque", "Bank Account Verification Cheque", false, 5242880),
    
    // Additional Documents
    PROFILE_PHOTO("Profile Photo", "User Profile Photograph", false, 2097152), // 2MB
    SIGNATURE("Signature", "Digital Signature Sample", false, 1048576), // 1MB
    OTHER("Other", "Other Supporting Documents", false, 15728640); // 15MB
    
    private final String displayName;
    private final String description;
    private final boolean mandatory;
    private final long maxSizeBytes;
    
    DocumentType(String displayName, String description, boolean mandatory, long maxSizeBytes) {
        this.displayName = displayName;
        this.description = description;
        this.mandatory = mandatory;
        this.maxSizeBytes = maxSizeBytes;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isMandatory() {
        return mandatory;
    }
    
    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }
    
    public String getMaxSizeFormatted() {
        final String[] units = {"B", "KB", "MB", "GB"};
        double size = maxSizeBytes;
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.0f %s", size, units[unitIndex]);
    }
    
    public boolean isIdentityDocument() {
        return this == PAN_CARD || this == AADHAAR_CARD || this == PASSPORT || 
               this == DRIVING_LICENSE || this == VOTER_ID;
    }
    
    public boolean isAddressProof() {
        return this == UTILITY_BILL || this == BANK_STATEMENT || 
               this == RENT_AGREEMENT || this == PROPERTY_DOCUMENT;
    }
    
    public boolean isIncomeProof() {
        return this == SALARY_SLIP || this == ITR || this == FORM_16 || this == BUSINESS_PROOF;
    }
    
    public boolean isFinancialDocument() {
        return this == BANK_ACCOUNT_PROOF || this == CANCELLED_CHEQUE;
    }
    
    public String[] getAllowedMimeTypes() {
        return switch (this) {
            case PROFILE_PHOTO, SIGNATURE -> new String[]{"image/jpeg", "image/jpg", "image/png"};
            case PAN_CARD, AADHAAR_CARD -> new String[]{"image/jpeg", "image/jpg", "image/png", "application/pdf"};
            default -> new String[]{"image/jpeg", "image/jpg", "image/png", "application/pdf", 
                                   "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        };
    }
    
    public boolean isValidMimeType(String mimeType) {
        if (mimeType == null) return false;
        
        String[] allowedTypes = getAllowedMimeTypes();
        for (String allowedType : allowedTypes) {
            if (allowedType.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }
}