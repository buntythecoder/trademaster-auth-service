package com.trademaster.auth.dto;

/**
 * Password Change Response DTO
 *
 * @param message Success or error message
 * @param sessionId Session ID for the password change operation
 */
public record PasswordChangeResponse(
    String message,
    String sessionId
) {
    public PasswordChangeResponse(String message) {
        this(message, null);
    }
}
