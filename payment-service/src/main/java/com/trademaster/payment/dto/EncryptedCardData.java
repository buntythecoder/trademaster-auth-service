package com.trademaster.payment.dto;

/**
 * Encrypted Card Data Record
 * Immutable container for encrypted payment card information
 *
 * Compliance:
 * - Rule 9: Immutable Records for data transfer
 * - PCI DSS: No plain-text card data storage
 *
 * @param encryptedCardNumber AES-256-GCM encrypted card number (Base64 encoded)
 * @param encryptedCvv AES-256-GCM encrypted CVV (Base64 encoded)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record EncryptedCardData(
    String encryptedCardNumber,
    String encryptedCvv
) {}
