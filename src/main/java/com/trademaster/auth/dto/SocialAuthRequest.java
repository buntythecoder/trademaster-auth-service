package com.trademaster.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Social Authentication Request
 */
@Setter
@Getter
public class SocialAuthRequest {
    // Getters and setters
    private String provider;
    private String accessToken;
    private String email;

}