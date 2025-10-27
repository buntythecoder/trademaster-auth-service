package com.trademaster.auth.dto;

/**
 * Email Verification Response DTO
 *
 * @param message Verification result message
 * @param email Verified email address
 */
public record EmailVerificationResponse(
    String message,
    String email
) {
}
