package com.trademaster.payment.dto;

import com.trademaster.payment.enums.BillingCycle;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Request DTO
 * 
 * Request object for initiating payment processing.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Subscription plan ID is required")
    private UUID subscriptionPlanId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Builder.Default
    private String currency = "INR";
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @NotNull(message = "Payment gateway is required")
    private PaymentGateway paymentGateway;
    
    // Optional payment method ID for saved payment methods
    private UUID savedPaymentMethodId;
    
    // Metadata for the payment
    private PaymentMetadata metadata;
    
    // Additional payment options
    private PaymentOptions options;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMetadata {
        private String planName;
        private BillingCycle billingCycle;
        private BigDecimal discountApplied;
        private String description;
        private Map<String, Object> additionalInfo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor  
    @Builder
    public static class PaymentOptions {
        @Builder.Default
        private Boolean savePaymentMethod = false;
        
        @Builder.Default
        private Boolean setAsDefault = false;
        
        private String returnUrl;
        private String cancelUrl;
        
        // For UPI payments
        private String upiId;
        
        // For card payments
        private String cardToken;
        
        // Additional gateway-specific options
        private Map<String, Object> gatewayOptions;
    }
}