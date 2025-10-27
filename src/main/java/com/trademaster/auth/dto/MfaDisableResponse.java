package com.trademaster.auth.dto;

/**
 * MFA Disable Response DTO
 *
 * @param message Success or error message
 * @param sessionId Session ID for the MFA disable operation
 */
public record MfaDisableResponse(
    String message,
    String sessionId
) {
    public MfaDisableResponse(String message) {
        this(message, null);
    }
}
