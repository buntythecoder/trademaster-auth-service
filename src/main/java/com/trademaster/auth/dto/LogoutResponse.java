package com.trademaster.auth.dto;

/**
 * Logout Response DTO
 *
 * @param message Success or error message
 * @param sessionId Session ID that was logged out
 */
public record LogoutResponse(
    String message,
    String sessionId
) {
    public LogoutResponse(String message) {
        this(message, null);
    }
}
