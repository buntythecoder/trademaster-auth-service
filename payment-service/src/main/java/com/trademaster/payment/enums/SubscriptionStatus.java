package com.trademaster.payment.enums;

import lombok.Getter;

/**
 * Subscription Status Enumeration
 * 
 * Represents the various states of a user subscription.
 * 
 * @author TradeMaster Development Team  
 * @version 1.0.0
 */
@Getter
public enum SubscriptionStatus {
    ACTIVE("Subscription is active and features are accessible"),
    INACTIVE("Subscription is inactive but not cancelled"),
    CANCELLED("Subscription has been cancelled by user"),
    EXPIRED("Subscription has expired due to non-payment"),
    SUSPENDED("Subscription is temporarily suspended");
    
    private final String description;
    
    SubscriptionStatus(String description) {
        this.description = description;
    }
    
    /**
     * Check if subscription allows feature access
     */
    public boolean allowsAccess() {
        return this == ACTIVE;
    }
    
    /**
     * Check if subscription can be reactivated
     */
    public boolean canReactivate() {
        return this == INACTIVE || this == EXPIRED || this == SUSPENDED;
    }
    
    /**
     * Check if subscription is terminated
     */
    public boolean isTerminated() {
        return this == CANCELLED;
    }
}