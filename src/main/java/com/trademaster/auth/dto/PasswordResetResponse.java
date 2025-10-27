package com.trademaster.auth.dto;

/**
 * Password Reset Response DTO
 *
 * @param message Success or error message
 * @param sessionId Session ID for the password reset operation
 */
public record PasswordResetResponse(
    String message,
    String sessionId
) {
    public PasswordResetResponse(String message) {
        this(message, null);
    }
}
