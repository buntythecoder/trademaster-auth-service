package com.trademaster.payment.service.impl;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.entity.UserPaymentMethod;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.repository.UserPaymentMethodRepository;
import com.trademaster.payment.service.UserPaymentMethodService;
import com.trademaster.payment.util.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Payment Method Service Implementation
 * Manages tokenized payment methods with PCI DSS compliance
 *
 * Architecture:
 * - Token-based payment storage (NO raw card data)
 * - Gateway handles card validation and encryption
 * - Only stores display data (last 4 digits, expiry, cardholder name)
 * - Payment method tokens reference gateway-stored payment instruments
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else, Optional and pattern matching
 * - Rule 6: Zero Trust Security - Token-based, no sensitive data storage
 * - Rule 11: Railway programming with Result types
 * - Rule 12: Virtual Threads with CompletableFuture (for async operations)
 * - Rule 15: Structured logging with correlation IDs
 * - PCI DSS: Token-based storage, no card data at rest
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPaymentMethodServiceImpl implements UserPaymentMethodService {

    private final UserPaymentMethodRepository paymentMethodRepository;

    /**
     * Add new payment method with validation
     * Payment method token provided by gateway
     * Functional validation chain with Railway programming
     */
    @Override
    @Transactional
    public Result<UserPaymentMethod, String> addPaymentMethod(UserPaymentMethod paymentMethod) {
        log.info("Adding payment method: userId={}, paymentMethodType={}",
                paymentMethod.getUserId(), paymentMethod.getPaymentMethodType());

        return validatePaymentMethodData(paymentMethod)
            .flatMap(this::checkForDuplicates)
            .flatMap(this::initializeTimestamps)
            .flatMap(this::savePaymentMethod)
            .onSuccess(saved -> log.info("Payment method added: id={}, userId={}",
                    saved.getId(), saved.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<UserPaymentMethod>, String> getUserPaymentMethods(UUID userId) {
        return ResultUtil.safely(() ->
            paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)
        ).mapError(Throwable::getMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<UserPaymentMethod, String> getDefaultPaymentMethod(UUID userId) {
        return Optional.ofNullable(
            paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                .orElse(null)
        )
            .map(Result::<UserPaymentMethod, String>success)
            .orElseGet(() -> Result.failure("No default payment method found for user: " + userId));
    }

    @Override
    @Transactional
    public Result<UserPaymentMethod, String> setDefaultPaymentMethod(UUID userId, UUID paymentMethodId) {
        return findPaymentMethod(paymentMethodId)
            .flatMap(method -> validateOwnership(method, userId))
            .flatMap(this::unsetOtherDefaults)
            .flatMap(this::setAsDefault)
            .onSuccess(method -> log.info("Default payment method set: userId={}, methodId={}",
                    userId, paymentMethodId));
    }

    @Override
    @Transactional
    public Result<Boolean, String> removePaymentMethod(UUID userId, UUID paymentMethodId) {
        return findPaymentMethod(paymentMethodId)
            .flatMap(method -> validateOwnership(method, userId))
            .flatMap(this::checkSubscriptionUsage)
            .flatMap(this::deactivatePaymentMethod)
            .map(method -> true)
            .onSuccess(success -> log.info("Payment method removed: userId={}, methodId={}",
                    userId, paymentMethodId));
    }

    @Override
    public Result<Boolean, String> validatePaymentMethod(UserPaymentMethod paymentMethod) {
        return validatePaymentMethodData(paymentMethod)
            .map(validated -> true);
    }

    // ==================== Private Helper Methods (Functional) ====================

    /**
     * Validate payment method data with functional chains
     * NO if-else, using Optional and pattern matching
     *
     * Validates:
     * - Payment method token exists
     * - Last 4 digits format (4 digits)
     * - Expiry date is valid and not expired
     * - Cardholder name is present
     * - Payment method type is set
     */
    private Result<UserPaymentMethod, String> validatePaymentMethodData(UserPaymentMethod paymentMethod) {
        return validatePaymentToken(paymentMethod)
            .flatMap(this::validateLastFourDigits)
            .flatMap(this::validateExpiryDate)
            .flatMap(this::validateCardholderName)
            .flatMap(this::validatePaymentMethodType);
    }

    private Result<UserPaymentMethod, String> validatePaymentToken(UserPaymentMethod paymentMethod) {
        return Optional.ofNullable(paymentMethod.getPaymentMethodToken())
            .filter(token -> !token.trim().isEmpty())
            .map(token -> Result.<UserPaymentMethod, String>success(paymentMethod))
            .orElseGet(() -> Result.failure("Payment method token is required"));
    }

    private Result<UserPaymentMethod, String> validateLastFourDigits(UserPaymentMethod paymentMethod) {
        return Optional.ofNullable(paymentMethod.getLastFourDigits())
            .filter(digits -> digits.length() == 4)
            .filter(digits -> digits.chars().allMatch(Character::isDigit))
            .map(digits -> Result.<UserPaymentMethod, String>success(paymentMethod))
            .orElseGet(() -> Result.failure("Last four digits must be exactly 4 numeric digits"));
    }

    private Result<UserPaymentMethod, String> validateExpiryDate(UserPaymentMethod paymentMethod) {
        return Optional.of(paymentMethod)
            .filter(method -> method.getExpiryMonth() != null &&
                             method.getExpiryMonth() >= 1 &&
                             method.getExpiryMonth() <= 12)
            .filter(method -> method.getExpiryYear() != null &&
                             method.getExpiryYear() >= Year.now().getValue())
            .filter(method -> !isCardExpired(method))
            .map(Result::<UserPaymentMethod, String>success)
            .orElseGet(() -> Result.failure("Card is expired or has invalid expiry date"));
    }

    private Result<UserPaymentMethod, String> validateCardholderName(UserPaymentMethod paymentMethod) {
        return Optional.ofNullable(paymentMethod.getCardholderName())
            .filter(name -> !name.trim().isEmpty())
            .filter(name -> name.length() >= 3)
            .map(name -> Result.<UserPaymentMethod, String>success(paymentMethod))
            .orElseGet(() -> Result.failure("Cardholder name is required and must be at least 3 characters"));
    }

    private Result<UserPaymentMethod, String> validatePaymentMethodType(UserPaymentMethod paymentMethod) {
        return Optional.ofNullable(paymentMethod.getPaymentMethodType())
            .map(type -> Result.<UserPaymentMethod, String>success(paymentMethod))
            .orElseGet(() -> Result.failure("Payment method type is required"));
    }

    private Result<UserPaymentMethod, String> checkForDuplicates(UserPaymentMethod paymentMethod) {
        // Check if user already has an active payment method with same last 4 digits
        List<UserPaymentMethod> existingMethods =
            paymentMethodRepository.findByUserIdAndIsActiveTrue(paymentMethod.getUserId());

        return Optional.of(existingMethods)
            .filter(methods -> methods.stream()
                .noneMatch(method -> paymentMethod.getLastFourDigits().equals(method.getLastFourDigits())))
            .map(methods -> Result.<UserPaymentMethod, String>success(paymentMethod))
            .orElseGet(() -> Result.failure("Payment method with same last 4 digits already exists"));
    }

    private Result<UserPaymentMethod, String> initializeTimestamps(UserPaymentMethod paymentMethod) {
        return ResultUtil.safely(() -> {
            Instant now = Instant.now();
            paymentMethod.setCreatedAt(now);
            paymentMethod.setUpdatedAt(now);
            return paymentMethod;
        }).mapError(Throwable::getMessage);
    }

    private Result<UserPaymentMethod, String> savePaymentMethod(UserPaymentMethod paymentMethod) {
        return ResultUtil.tryExecute(() ->
            paymentMethodRepository.save(paymentMethod)
        ).mapError(Throwable::getMessage);
    }

    private Result<UserPaymentMethod, String> findPaymentMethod(UUID paymentMethodId) {
        return paymentMethodRepository.findById(paymentMethodId)
            .map(Result::<UserPaymentMethod, String>success)
            .orElseGet(() -> Result.failure("Payment method not found: " + paymentMethodId));
    }

    private Result<UserPaymentMethod, String> validateOwnership(UserPaymentMethod method, UUID userId) {
        return Optional.of(method)
            .filter(m -> m.getUserId().equals(userId))
            .map(Result::<UserPaymentMethod, String>success)
            .orElseGet(() -> Result.failure("Payment method does not belong to user"));
    }

    private Result<UserPaymentMethod, String> unsetOtherDefaults(UserPaymentMethod paymentMethod) {
        paymentMethodRepository.findByUserIdAndIsActiveTrue(paymentMethod.getUserId())
            .stream()
            .filter(UserPaymentMethod::getIsDefault)
            .forEach(method -> {
                method.setIsDefault(false);
                paymentMethodRepository.save(method);
            });

        return Result.success(paymentMethod);
    }

    private Result<UserPaymentMethod, String> setAsDefault(UserPaymentMethod paymentMethod) {
        paymentMethod.setIsDefault(true);
        return ResultUtil.safely(() ->
            paymentMethodRepository.save(paymentMethod)
        ).mapError(Throwable::getMessage);
    }

    private Result<UserPaymentMethod, String> checkSubscriptionUsage(UserPaymentMethod paymentMethod) {
        // Subscription usage check disabled - payment method deletion allowed unconditionally
        // Subscription service integration not implemented in current version
        // Payment method deactivation (soft delete) used instead of hard delete for audit trail
        return Result.success(paymentMethod);
    }

    private Result<UserPaymentMethod, String> deactivatePaymentMethod(UserPaymentMethod paymentMethod) {
        paymentMethod.setIsActive(false);
        paymentMethod.setUpdatedAt(Instant.now());
        return ResultUtil.safely(() ->
            paymentMethodRepository.save(paymentMethod)
        ).mapError(Throwable::getMessage);
    }

    /**
     * Check if card is expired based on expiry date
     * Functional implementation without if-else
     */
    private boolean isCardExpired(UserPaymentMethod paymentMethod) {
        int currentYear = Year.now().getValue();
        int currentMonth = LocalDateTime.now().getMonthValue();

        return paymentMethod.getExpiryYear() < currentYear ||
               (paymentMethod.getExpiryYear() == currentYear &&
                paymentMethod.getExpiryMonth() < currentMonth);
    }
}
