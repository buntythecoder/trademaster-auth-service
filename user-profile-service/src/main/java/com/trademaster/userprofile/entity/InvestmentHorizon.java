package com.trademaster.userprofile.entity;

public enum InvestmentHorizon {
    SHORT_TERM("Short Term", "Less than 1 year", 0, 12),
    MEDIUM_TERM("Medium Term", "1 to 5 years", 12, 60),
    LONG_TERM("Long Term", "More than 5 years", 60, Integer.MAX_VALUE);
    
    private final String displayName;
    private final String description;
    private final int minMonths;
    private final int maxMonths;
    
    InvestmentHorizon(String displayName, String description, int minMonths, int maxMonths) {
        this.displayName = displayName;
        this.description = description;
        this.minMonths = minMonths;
        this.maxMonths = maxMonths;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getMinMonths() {
        return minMonths;
    }
    
    public int getMaxMonths() {
        return maxMonths;
    }
    
    public boolean isValidForMonths(int months) {
        return months >= minMonths && months <= maxMonths;
    }
    
    public static InvestmentHorizon fromMonths(int months) {
        for (InvestmentHorizon horizon : values()) {
            if (horizon.isValidForMonths(months)) {
                return horizon;
            }
        }
        throw new IllegalArgumentException("Invalid investment horizon months: " + months);
    }
}