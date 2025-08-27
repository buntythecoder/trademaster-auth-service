package com.trademaster.payment.dto;

import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment Method Response DTO
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private UUID id;
    private PaymentMethod paymentMethodType;
    private PaymentGateway gatewayProvider;
    private String lastFourDigits;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardBrand;
    private String cardholderName;
    private Boolean isDefault;
    private Boolean isVerified;
    private Instant createdAt;
    private Instant lastUsedAt;
}