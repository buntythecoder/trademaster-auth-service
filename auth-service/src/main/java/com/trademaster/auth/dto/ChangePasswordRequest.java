package com.trademaster.auth.dto;

/**
 * Change Password Request DTO
 *
 * @param email User email
 * @param currentPassword Current password for verification
 * @param newPassword New password to set
 */
public record ChangePasswordRequest(
    String email,
    String currentPassword,
    String newPassword
) {
    public ChangePasswordRequest {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password cannot be null or blank");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be null or blank");
        }
    }
}
