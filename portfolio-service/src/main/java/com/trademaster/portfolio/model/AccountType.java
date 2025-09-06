package com.trademaster.portfolio.model;

/**
 * Account Type Enumeration
 * 
 * Defines different types of trading accounts.
 */
public enum AccountType {
    INDIVIDUAL("Individual Account"),
    JOINT("Joint Account"),
    CORPORATE("Corporate Account"),
    TRUST("Trust Account"),
    RETIREMENT("Retirement Account"),
    MARGIN("Margin Account"),
    OPTIONS("Options Account");
    
    private final String displayName;
    
    AccountType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}