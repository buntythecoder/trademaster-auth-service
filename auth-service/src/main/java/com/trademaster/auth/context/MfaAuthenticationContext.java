package com.trademaster.auth.context;

import com.trademaster.auth.dto.AuthenticationRequest;

/**
 * MFA Authentication Context
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record MfaAuthenticationContext(
    AuthenticationRequest request,
    String mfaToken
) {}