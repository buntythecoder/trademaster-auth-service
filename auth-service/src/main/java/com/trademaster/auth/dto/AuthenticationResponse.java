package com.trademaster.auth.dto;

import com.trademaster.auth.service.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response DTO containing JWT tokens and user info
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private Long requestId; // Request tracking ID
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    private AuthenticationService.UserDto user;
    
    private boolean requiresEmailVerification;
    private boolean requiresMfa;
    private String mfaChallenge;
    
    private String message;
    
    // Additional security information
    private String deviceFingerprint;
    private Long sessionTimeout;
    
    // Agent orchestration fields
    private String status;
    private java.util.List<String> processingResults;
    private Long processingTimeMs;
    private String errorMessage;
}