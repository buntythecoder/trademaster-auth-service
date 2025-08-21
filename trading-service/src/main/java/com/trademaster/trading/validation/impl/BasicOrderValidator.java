package com.trademaster.trading.validation.impl;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import com.trademaster.trading.validation.OrderValidator;
import com.trademaster.trading.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Basic Order Validator
 * 
 * Performs fundamental validation of order requests including:
 * - Required field validation
 * - Business rule compliance
 * - Price and quantity constraints
 * - Order type specific validation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class BasicOrderValidator implements OrderValidator {
    
    @Override
    public ValidationResult validate(OrderRequest orderRequest, Long userId) {
        long startTime = System.currentTimeMillis();
        
        ValidationResult result = ValidationResult.builder()
            .valid(true)
            .validatorName(getValidatorName())
            .build();
        
        try {
            // Validate basic fields
            validateBasicFields(orderRequest, result);
            
            // Validate order type specific requirements
            validateOrderTypeRequirements(orderRequest, result);
            
            // Validate price relationships
            validatePriceRelationships(orderRequest, result);
            
            // Validate time in force requirements
            validateTimeInForceRequirements(orderRequest, result);
            
            // Validate quantity constraints
            validateQuantityConstraints(orderRequest, result);
            
            // Validate symbol format
            validateSymbolFormat(orderRequest, result);
            
        } catch (Exception e) {
            log.error("Error during basic validation for user {}: {}", userId, e.getMessage());
            result.addError("Validation error: " + e.getMessage());
        }
        
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    @Override
    public ValidationResult validateModification(Order existingOrder, OrderRequest modificationRequest, Long userId) {
        ValidationResult result = ValidationResult.builder()
            .valid(true)
            .validatorName(getValidatorName())
            .build();
        
        // Check if order can be modified
        if (!existingOrder.getStatus().isModifiable()) {
            result.addError("Order cannot be modified in status: " + existingOrder.getStatus());
            return result;
        }
        
        // Validate that core fields match
        if (!existingOrder.getSymbol().equals(modificationRequest.getSymbol())) {
            result.addError("Symbol cannot be changed during modification");
        }
        
        if (!existingOrder.getSide().equals(modificationRequest.getSide())) {
            result.addError("Order side cannot be changed during modification");
        }
        
        if (!existingOrder.getOrderType().equals(modificationRequest.getOrderType())) {
            result.addError("Order type cannot be changed during modification");
        }
        
        // Validate new quantity is not less than filled quantity
        if (modificationRequest.getQuantity() < existingOrder.getFilledQuantity()) {
            result.addError("New quantity cannot be less than already filled quantity: " + 
                          existingOrder.getFilledQuantity());
        }
        
        // Perform standard validation on the modification
        ValidationResult basicValidation = validate(modificationRequest, userId);
        if (basicValidation != null && !basicValidation.isValid()) {
            result = result.merge(basicValidation);
        }
        
        return result;
    }
    
    private void validateBasicFields(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getSymbol() == null || orderRequest.getSymbol().trim().isEmpty()) {
            result.addError("Symbol is required");
        }
        
        if (orderRequest.getExchange() == null || orderRequest.getExchange().trim().isEmpty()) {
            result.addError("Exchange is required");
        }
        
        if (orderRequest.getOrderType() == null) {
            result.addError("Order type is required");
        }
        
        if (orderRequest.getSide() == null) {
            result.addError("Order side is required");
        }
        
        if (orderRequest.getQuantity() == null || orderRequest.getQuantity() <= 0) {
            result.addError("Quantity must be a positive number");
        }
        
        if (orderRequest.getTimeInForce() == null) {
            result.addError("Time in force is required");
        }
    }
    
    private void validateOrderTypeRequirements(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getOrderType() == null) {
            return; // Already handled in basic validation
        }
        
        OrderType orderType = orderRequest.getOrderType();
        
        // Validate limit price requirements
        if (orderType.requiresLimitPrice() && orderRequest.getLimitPrice() == null) {
            result.addError("Limit price is required for " + orderType + " orders");
        }
        
        if (!orderType.requiresLimitPrice() && orderRequest.getLimitPrice() != null) {
            result.addWarning("Limit price is not used for " + orderType + " orders");
        }
        
        // Validate stop price requirements
        if (orderType.requiresStopPrice() && orderRequest.getStopPrice() == null) {
            result.addError("Stop price is required for " + orderType + " orders");
        }
        
        if (!orderType.requiresStopPrice() && orderRequest.getStopPrice() != null) {
            result.addWarning("Stop price is not used for " + orderType + " orders");
        }
        
        // Validate positive prices
        if (orderRequest.getLimitPrice() != null && orderRequest.getLimitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Limit price must be positive");
        }
        
        if (orderRequest.getStopPrice() != null && orderRequest.getStopPrice().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Stop price must be positive");
        }
    }
    
    private void validatePriceRelationships(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getOrderType() != OrderType.STOP_LIMIT || 
            orderRequest.getLimitPrice() == null || 
            orderRequest.getStopPrice() == null) {
            return;
        }
        
        // For stop-limit orders, validate price relationships
        switch (orderRequest.getSide()) {
            case BUY:
                // For buy stop-limit: stop price >= limit price (buying on upward momentum)
                if (orderRequest.getStopPrice().compareTo(orderRequest.getLimitPrice()) < 0) {
                    result.addError("For buy stop-limit orders, stop price must be >= limit price");
                }
                break;
            case SELL:
                // For sell stop-limit: stop price <= limit price (selling on downward momentum)
                if (orderRequest.getStopPrice().compareTo(orderRequest.getLimitPrice()) > 0) {
                    result.addError("For sell stop-limit orders, stop price must be <= limit price");
                }
                break;
        }
    }
    
    private void validateTimeInForceRequirements(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getTimeInForce() == null) {
            return; // Already handled in basic validation
        }
        
        TimeInForce timeInForce = orderRequest.getTimeInForce();
        
        // Validate GTD expiry date requirement
        if (timeInForce == TimeInForce.GTD) {
            if (orderRequest.getExpiryDate() == null) {
                result.addError("Expiry date is required for Good Till Date orders");
            } else if (!orderRequest.getExpiryDate().isAfter(LocalDate.now())) {
                result.addError("Expiry date must be in the future");
            } else if (orderRequest.getExpiryDate().isAfter(LocalDate.now().plusDays(365))) {
                result.addWarning("Expiry date is more than 1 year in the future");
            }
        } else if (orderRequest.getExpiryDate() != null) {
            result.addWarning("Expiry date is only used for Good Till Date orders");
        }
        
        // Validate IOC/FOK with order types
        if ((timeInForce == TimeInForce.IOC || timeInForce == TimeInForce.FOK) && 
            orderRequest.getOrderType() != OrderType.MARKET && 
            orderRequest.getOrderType() != OrderType.LIMIT) {
            result.addWarning("IOC/FOK orders are typically used with Market or Limit order types");
        }
    }
    
    private void validateQuantityConstraints(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getQuantity() == null) {
            return; // Already handled in basic validation
        }
        
        int quantity = orderRequest.getQuantity();
        
        // Maximum quantity constraints
        if (quantity > 1_000_000) {
            result.addError("Order quantity cannot exceed 1,000,000 shares");
        }
        
        // Minimum quantity constraints
        if (quantity < 1) {
            result.addError("Order quantity must be at least 1 share");
        }
        
        // Large order warning
        if (quantity > 100_000) {
            result.addWarning("Large order detected (" + quantity + " shares). Consider breaking into smaller orders.");
        }
    }
    
    private void validateSymbolFormat(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.getSymbol() == null) {
            return; // Already handled in basic validation
        }
        
        String symbol = orderRequest.getSymbol().trim().toUpperCase();
        
        // Validate symbol format
        if (!symbol.matches("^[A-Z0-9_]{1,20}$")) {
            result.addError("Symbol must contain only uppercase letters, numbers, and underscores (max 20 characters)");
        }
        
        // Validate exchange-specific symbol formats
        if ("NSE".equals(orderRequest.getExchange()) || "BSE".equals(orderRequest.getExchange())) {
            if (symbol.length() > 20) {
                result.addError("Symbol too long for Indian exchanges (max 20 characters)");
            }
        }
        
        // Common symbol validation
        if (symbol.startsWith("_") || symbol.endsWith("_")) {
            result.addWarning("Symbol should not start or end with underscore");
        }
    }
    
    @Override
    public int getPriority() {
        return 1; // Highest priority - basic validation should run first
    }
    
    @Override
    public String getValidatorName() {
        return "BasicOrderValidator";
    }
}