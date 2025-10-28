package com.trademaster.auth.dto;

/**
 * MFA Configuration DTO
 *
 * @param mfaType Type of MFA (TOTP, SMS, EMAIL)
 * @param secretKey Secret key for TOTP generation
 * @param enabled Whether MFA is enabled
 */
public record MfaConfig(
    MfaType mfaType,
    String secretKey,
    boolean enabled
) {
    /**
     * MFA Type Enumeration
     */
    public enum MfaType {
        TOTP,
        SMS,
        EMAIL
    }
}
