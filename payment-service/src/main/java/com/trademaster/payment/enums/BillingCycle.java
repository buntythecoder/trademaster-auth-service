package com.trademaster.payment.enums;

import lombok.Getter;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Billing Cycle Enumeration
 * 
 * Represents different billing frequencies for subscriptions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Getter
public enum BillingCycle {
    MONTHLY("Monthly", Period.ofMonths(1), 1),
    QUARTERLY("Quarterly", Period.ofMonths(3), 3), 
    ANNUAL("Annual", Period.ofYears(1), 12);
    
    private final String displayName;
    private final Period period;
    private final int months;
    
    BillingCycle(String displayName, Period period, int months) {
        this.displayName = displayName;
        this.period = period;
        this.months = months;
    }
    
    /**
     * Get the number of days in this billing cycle (approximate)
     */
    public int getDays() {
        return months * 30; // Approximate for calculations
    }
    
    /**
     * Check if this is the most frequent billing cycle
     */
    public boolean isFrequent() {
        return this == MONTHLY;
    }
    
    /**
     * Get discount percentage typically offered for this billing cycle
     */
    public double getDiscountPercentage() {
        return switch (this) {
            case MONTHLY -> 0.0;      // No discount
            case QUARTERLY -> 5.0;    // 5% discount
            case ANNUAL -> 15.0;      // 15% discount
        };
    }
}