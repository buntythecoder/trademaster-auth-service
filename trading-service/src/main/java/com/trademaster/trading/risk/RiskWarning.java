package com.trademaster.trading.risk;

import lombok.Builder;
import lombok.Data;

/**
 * Risk Warning
 * 
 * Represents a risk concern that doesn't block execution
 * but should be brought to the user's attention.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class RiskWarning {
    
    /**
     * Type of risk warning
     */
    private RiskWarningType type;
    
    /**
     * Human-readable warning message
     */
    private String message;
    
    /**
     * Current risk level (0.0 to 1.0)
     */
    private double riskLevel;
    
    /**
     * Recommended action
     */
    private String recommendation;
}

/**
 * Risk Warning Types
 */
enum RiskWarningType {
    HIGH_VOLATILITY,
    LARGE_ORDER_SIZE,
    CONCENTRATION_BUILDING,
    UNUSUAL_ACTIVITY,
    MARKET_CONDITIONS,
    APPROACHING_LIMITS
}