package com.trademaster.payment.dto;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Method Request DTO
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {

    @NotNull(message = "Payment method type is required")
    private PaymentMethod paymentMethodType;
    
    @NotNull(message = "Gateway provider is required")
    private PaymentGateway gatewayProvider;
    
    @NotNull(message = "Payment method token is required")
    @Size(min = 1, max = 255, message = "Payment method token must be between 1 and 255 characters")
    private String paymentMethodToken;
    
    @Size(max = 4, message = "Last four digits must be at most 4 characters")
    private String lastFourDigits;
    
    private Integer expiryMonth;
    
    private Integer expiryYear;
    
    @Size(max = 50, message = "Card brand must be at most 50 characters")
    private String cardBrand;
    
    @Size(max = 100, message = "Cardholder name must be at most 100 characters")
    private String cardholderName;
    
    @Size(max = 500, message = "Billing address must be at most 500 characters")
    private String billingAddress;
}