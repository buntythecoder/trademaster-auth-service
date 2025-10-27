package com.trademaster.auth.context;

import com.trademaster.auth.dto.SocialAuthRequest;

/**
 * Social Authentication Context with builder support
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SocialAuthenticationContext(
    SocialAuthRequest request,
    String socialToken
) {

    /**
     * Create builder instance
     */
    public static SocialAuthenticationContextBuilder builder() {
        return new SocialAuthenticationContextBuilder();
    }

    /**
     * Builder for SocialAuthenticationContext
     * Provides fluent API for constructing social authentication contexts
     */
    public static class SocialAuthenticationContextBuilder {
        private SocialAuthRequest request;
        private String socialToken;

        public SocialAuthenticationContextBuilder request(SocialAuthRequest request) {
            this.request = request;
            return this;
        }

        public SocialAuthenticationContextBuilder socialToken(String socialToken) {
            this.socialToken = socialToken;
            return this;
        }

        public SocialAuthenticationContext build() {
            return new SocialAuthenticationContext(request, socialToken);
        }
    }
}
