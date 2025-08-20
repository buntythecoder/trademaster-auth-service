package com.trademaster.userprofile.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DefaultOrderSettings(
    @NotNull(message = "Default order type is required")
    OrderType defaultOrderType,
    
    @NotNull(message = "Default order validity is required")
    OrderValidity defaultOrderValidity,
    
    @Min(value = 1, message = "Default quantity must be at least 1")
    Integer defaultQuantity,
    
    @DecimalMin(value = "0.01", message = "Default amount must be greater than 0")
    BigDecimal defaultAmount,
    
    ProductType defaultProductType,
    
    Boolean autoSquareOff,
    
    Integer squareOffTime, // in minutes before market close
    
    Boolean stopLossEnabled,
    
    @DecimalMin(value = "0.01", message = "Stop loss percentage must be positive")
    BigDecimal stopLossPercentage,
    
    Boolean takeProfitEnabled,
    
    @DecimalMin(value = "0.01", message = "Take profit percentage must be positive")
    BigDecimal takeProfitPercentage,
    
    Boolean trailingStopLossEnabled,
    
    @DecimalMin(value = "0.01", message = "Trailing stop loss percentage must be positive")
    BigDecimal trailingStopLossPercentage
) {
    
    public boolean hasRiskManagement() {
        return Boolean.TRUE.equals(stopLossEnabled) || Boolean.TRUE.equals(takeProfitEnabled);
    }
    
    public boolean isAdvancedTrader() {
        return Boolean.TRUE.equals(trailingStopLossEnabled) || 
               Boolean.TRUE.equals(autoSquareOff);
    }
    
    public BigDecimal calculateStopLossPrice(BigDecimal entryPrice, boolean isLong) {
        if (!Boolean.TRUE.equals(stopLossEnabled) || stopLossPercentage == null || entryPrice == null) {
            return null;
        }
        
        BigDecimal factor = stopLossPercentage.divide(BigDecimal.valueOf(100));
        
        if (isLong) {
            return entryPrice.multiply(BigDecimal.ONE.subtract(factor));
        } else {
            return entryPrice.multiply(BigDecimal.ONE.add(factor));
        }
    }
    
    public BigDecimal calculateTakeProfitPrice(BigDecimal entryPrice, boolean isLong) {
        if (!Boolean.TRUE.equals(takeProfitEnabled) || takeProfitPercentage == null || entryPrice == null) {
            return null;
        }
        
        BigDecimal factor = takeProfitPercentage.divide(BigDecimal.valueOf(100));
        
        if (isLong) {
            return entryPrice.multiply(BigDecimal.ONE.add(factor));
        } else {
            return entryPrice.multiply(BigDecimal.ONE.subtract(factor));
        }
    }
}