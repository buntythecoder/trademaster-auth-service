package com.trademaster.portfolio.model;

/**
 * Cost Basis Calculation Method
 * 
 * Defines the method used to calculate the average cost of securities
 * for P&L calculations and tax reporting purposes.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public enum CostBasisMethod {
    
    /**
     * First In, First Out (FIFO)
     * - Uses the cost of the earliest acquired shares first
     * - Common for tax calculations in most jurisdictions
     * - Provides predictable tax treatment
     */
    FIFO("fifo"),
    
    /**
     * Last In, First Out (LIFO)
     * - Uses the cost of the most recently acquired shares first
     * - May provide tax advantages in certain markets
     * - Less common but useful for specific strategies
     */
    LIFO("lifo"),
    
    /**
     * Weighted Average Cost
     * - Calculates average cost across all purchases
     * - Simplifies P&L calculations
     * - Commonly used for mutual funds and ETFs
     */
    WEIGHTED_AVERAGE("weighted_average"),
    
    /**
     * Specific Identification
     * - Allows manual selection of specific lots to sell
     * - Provides maximum tax optimization flexibility
     * - Requires detailed lot tracking
     */
    SPECIFIC_ID("specific_id");
    
    private final String value;
    
    CostBasisMethod(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Check if method requires lot-level tracking
     */
    public boolean requiresLotTracking() {
        return this == FIFO || this == LIFO || this == SPECIFIC_ID;
    }
    
    /**
     * Check if method supports tax optimization
     */
    public boolean supportsTaxOptimization() {
        return this == SPECIFIC_ID || this == FIFO || this == LIFO;
    }
}