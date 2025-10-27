package com.trademaster.auth.context;

import com.trademaster.auth.dto.AuthenticationRequest;

/**
 * MFA Authentication Context with builder support
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record MfaAuthenticationContext(
    AuthenticationRequest request,
    String mfaToken
) {

    /**
     * Create builder instance
     */
    public static MfaAuthenticationContextBuilder builder() {
        return new MfaAuthenticationContextBuilder();
    }

    /**
     * Builder for MfaAuthenticationContext
     * Provides fluent API for constructing MFA authentication contexts
     */
    public static class MfaAuthenticationContextBuilder {
        private AuthenticationRequest request;
        private String mfaToken;

        public MfaAuthenticationContextBuilder request(AuthenticationRequest request) {
            this.request = request;
            return this;
        }

        public MfaAuthenticationContextBuilder mfaToken(String mfaToken) {
            this.mfaToken = mfaToken;
            return this;
        }

        public MfaAuthenticationContext build() {
            return new MfaAuthenticationContext(request, mfaToken);
        }
    }
}