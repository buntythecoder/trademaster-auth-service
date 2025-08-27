package com.trademaster.payment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refund Request DTO
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @Size(max = 500, message = "Refund reason must be at most 500 characters")
    private String reason;
    
    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;
    
    private String customerNotification;
    
    @Builder.Default
    private Boolean notifyCustomer = true;
}