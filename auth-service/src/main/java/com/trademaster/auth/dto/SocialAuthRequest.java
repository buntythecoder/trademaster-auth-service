package com.trademaster.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Social Authentication Request
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialAuthRequest {
    // Getters and setters
    private String provider;
    private String accessToken;
    private String email;

}