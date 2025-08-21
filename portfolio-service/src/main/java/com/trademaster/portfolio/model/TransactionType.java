package com.trademaster.portfolio.model;

/**
 * Portfolio Transaction Type
 * 
 * Represents different types of transactions that affect portfolio positions.
 * Used for comprehensive transaction tracking and audit trails.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public enum TransactionType {
    
    /**
     * Buy transaction - acquiring a long position
     * - Increases long position quantity
     * - Debits cash balance
     * - Affects average cost calculation
     */
    BUY("buy"),
    
    /**
     * Sell transaction - reducing a long position
     * - Decreases long position quantity
     * - Credits cash balance
     * - Realizes P&L
     */
    SELL("sell"),
    
    /**
     * Short sell transaction - opening a short position
     * - Creates or increases short position
     * - Credits cash balance (proceeds from short sale)
     * - Requires margin maintenance
     */
    SHORT_SELL("short_sell"),
    
    /**
     * Buy to cover transaction - closing a short position
     * - Decreases short position quantity
     * - Debits cash balance
     * - Realizes P&L on short position
     */
    BUY_TO_COVER("buy_to_cover"),
    
    /**
     * Dividend received
     * - Credits cash balance
     * - No position quantity change
     * - Affects total return calculation
     */
    DIVIDEND("dividend"),
    
    /**
     * Interest earned on cash balance
     * - Credits cash balance
     * - No position quantity change
     * - Part of portfolio income
     */
    INTEREST("interest"),
    
    /**
     * Commission/fee charged
     * - Debits cash balance
     * - Affects net P&L calculation
     * - Transaction cost tracking
     */
    FEE("fee"),
    
    /**
     * Cash deposit to portfolio
     * - Credits cash balance
     * - No position changes
     * - Affects portfolio value
     */
    DEPOSIT("deposit"),
    
    /**
     * Cash withdrawal from portfolio
     * - Debits cash balance
     * - No position changes
     * - Affects portfolio value
     */
    WITHDRAWAL("withdrawal"),
    
    /**
     * Stock split adjustment
     * - Adjusts position quantity and price
     * - No cash impact
     * - Maintains total position value
     */
    SPLIT("split"),
    
    /**
     * Stock dividend (additional shares)
     * - Increases position quantity
     * - No cash impact
     * - Adjusts average cost basis
     */
    STOCK_DIVIDEND("stock_dividend"),
    
    /**
     * Spin-off transaction
     * - May create new position
     * - Adjusts existing position
     * - Complex corporate action
     */
    SPINOFF("spinoff");
    
    private final String value;
    
    TransactionType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Check if transaction affects position quantity
     */
    public boolean affectsQuantity() {
        return switch (this) {
            case BUY, SELL, SHORT_SELL, BUY_TO_COVER, SPLIT, STOCK_DIVIDEND, SPINOFF -> true;
            case DIVIDEND, INTEREST, FEE, DEPOSIT, WITHDRAWAL -> false;
        };
    }
    
    /**
     * Check if transaction affects cash balance
     */
    public boolean affectsCash() {
        return switch (this) {
            case SPLIT, STOCK_DIVIDEND -> false;
            default -> true;
        };
    }
    
    /**
     * Check if transaction realizes P&L
     */
    public boolean realizesGains() {
        return this == SELL || this == BUY_TO_COVER;
    }
    
    /**
     * Check if transaction is a trade execution
     */
    public boolean isTradeExecution() {
        return this == BUY || this == SELL || this == SHORT_SELL || this == BUY_TO_COVER;
    }
    
    /**
     * Check if transaction is a corporate action
     */
    public boolean isCorporateAction() {
        return this == SPLIT || this == STOCK_DIVIDEND || this == SPINOFF;
    }
}