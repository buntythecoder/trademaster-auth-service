package com.trademaster.userprofile.entity;

public enum TradingSegment {
    EQUITY("Equity", "Cash segment for buying and selling stocks"),
    FUTURES("Futures", "Futures contracts for hedging and speculation"),
    OPTIONS("Options", "Options contracts for risk management"),
    COMMODITY("Commodity", "Commodity trading including gold, silver, crude oil"),
    CURRENCY("Currency", "Currency derivatives trading"),
    MUTUAL_FUNDS("Mutual Funds", "Mutual fund investments"),
    IPO("IPO", "Initial Public Offerings"),
    BONDS("Bonds", "Government and corporate bonds"),
    ETF("ETF", "Exchange Traded Funds");
    
    private final String displayName;
    private final String description;
    
    TradingSegment(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isDerivative() {
        return this == FUTURES || this == OPTIONS || this == CURRENCY;
    }
    
    public boolean requiresHighRiskProfile() {
        return this == FUTURES || this == OPTIONS || this == COMMODITY || this == CURRENCY;
    }
}