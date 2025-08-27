package com.trademaster.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication request DTO for user login
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    private Long requestId; // Request tracking ID
    
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    private String username;  // Can be email or username

    @NotBlank(message = "Password is required")
    private String password;

    private String mfaCode;  // 6-digit MFA code
    
    private String biometricData;  // Biometric authentication data
    
    private String socialProvider;  // GOOGLE, FACEBOOK, etc.
    private String socialToken;     // OAuth token from social provider

    @Builder.Default
    private boolean rememberMe = false;

    @Builder.Default
    private boolean mfaBypass = false;

    private String deviceInfo;
}