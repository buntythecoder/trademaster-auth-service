package com.trademaster.auth.context;

import com.trademaster.auth.dto.SocialAuthRequest;

/**
 * Social Authentication Context
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SocialAuthenticationContext(
    SocialAuthRequest request,
    String socialToken
) {}

