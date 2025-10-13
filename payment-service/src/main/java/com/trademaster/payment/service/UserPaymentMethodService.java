package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.entity.UserPaymentMethod;

import java.util.List;
import java.util.UUID;

/**
 * User Payment Method Service - Interface
 * Manages user payment methods with validation and security
 *
 * Compliance:
 * - Rule 3: Functional interface with Result types
 * - Rule 6: Zero trust security (SecurityFacade for external access)
 * - Rule 11: Railway programming with Result types
 *
 * Security Requirements:
 * - PCI DSS compliance for card data
 * - Encryption at rest and in transit
 * - No plain-text card numbers in logs
 * - Token-based card references
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface UserPaymentMethodService {

    /**
     * Add new payment method for user
     * Validates card data, checks for duplicates, encrypts sensitive fields
     *
     * @param paymentMethod Payment method to add
     * @return Result with saved payment method or validation error
     */
    Result<UserPaymentMethod, String> addPaymentMethod(UserPaymentMethod paymentMethod);

    /**
     * Get all payment methods for user
     *
     * @param userId User ID (UUID)
     * @return Result with list of payment methods
     */
    Result<List<UserPaymentMethod>, String> getUserPaymentMethods(UUID userId);

    /**
     * Get default payment method for user
     *
     * @param userId User ID (UUID)
     * @return Result with default payment method or error if none exists
     */
    Result<UserPaymentMethod, String> getDefaultPaymentMethod(UUID userId);

    /**
     * Set payment method as default for user
     * Unsets other default payment methods automatically
     *
     * @param userId User ID (UUID)
     * @param paymentMethodId Payment method ID (UUID)
     * @return Result with updated payment method
     */
    Result<UserPaymentMethod, String> setDefaultPaymentMethod(UUID userId, UUID paymentMethodId);

    /**
     * Remove payment method
     * Validates that payment method belongs to user
     * Prevents deletion if referenced in active subscriptions
     *
     * @param userId User ID (UUID)
     * @param paymentMethodId Payment method ID (UUID)
     * @return Result indicating success or failure
     */
    Result<Boolean, String> removePaymentMethod(UUID userId, UUID paymentMethodId);

    /**
     * Validate payment method data
     * Checks card number format, expiry date, CVV format
     *
     * @param paymentMethod Payment method to validate
     * @return Result indicating validation success or specific error
     */
    Result<Boolean, String> validatePaymentMethod(UserPaymentMethod paymentMethod);
}
