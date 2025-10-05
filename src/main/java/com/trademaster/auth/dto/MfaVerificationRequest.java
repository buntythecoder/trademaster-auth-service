package com.trademaster.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaVerificationRequest {
    
    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 8, message = "MFA code must be 6-8 digits")
    private String code;
    
    private String mfaType; // TOTP, SMS, EMAIL
}