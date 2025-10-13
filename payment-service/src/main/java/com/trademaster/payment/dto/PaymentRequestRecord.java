package com.trademaster.payment.dto;

import com.trademaster.payment.enums.BillingCycle;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable Payment Request DTO (Record-based)
 * Demonstrates Rule 9 compliance with immutable records
 *
 * Compliance:
 * - Rule 9: Immutable records for all data holders
 * - Rule 11: Validation in compact constructor
 * - Rule 18: Descriptive naming
 *
 * @param userId User ID initiating payment
 * @param subscriptionPlanId Subscription plan to pay for
 * @param amount Payment amount
 * @param currency Currency code (3 letters)
 * @param paymentMethod Payment method type
 * @param paymentGateway Gateway to process payment
 * @param savedPaymentMethodId Optional saved payment method
 * @param metadata Payment metadata
 * @param options Payment options
 */
public record PaymentRequestRecord(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotNull(message = "Subscription plan ID is required")
    UUID subscriptionPlanId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 8, fraction = 2)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    @NotNull(message = "Payment gateway is required")
    PaymentGateway paymentGateway,

    UUID savedPaymentMethodId,

    PaymentMetadata metadata,

    PaymentOptions options
) {
    /**
     * Compact constructor with validation
     * Rule 11: Functional validation chains
     */
    public PaymentRequestRecord {
        // Null-safe defaults using functional patterns
        currency = (currency != null && !currency.isBlank()) ? currency : "INR";

        // Defensive copies for mutable types (if any)
        // Records are inherently immutable for immutable types
    }

    /**
     * Builder pattern for convenient record construction
     * Rule 4: Builder pattern with fluent API
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID userId;
        private UUID subscriptionPlanId;
        private BigDecimal amount;
        private String currency = "INR";
        private PaymentMethod paymentMethod;
        private PaymentGateway paymentGateway;
        private UUID savedPaymentMethodId;
        private PaymentMetadata metadata;
        private PaymentOptions options;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder subscriptionPlanId(UUID subscriptionPlanId) {
            this.subscriptionPlanId = subscriptionPlanId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder paymentGateway(PaymentGateway paymentGateway) {
            this.paymentGateway = paymentGateway;
            return this;
        }

        public Builder savedPaymentMethodId(UUID savedPaymentMethodId) {
            this.savedPaymentMethodId = savedPaymentMethodId;
            return this;
        }

        public Builder metadata(PaymentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder options(PaymentOptions options) {
            this.options = options;
            return this;
        }

        public PaymentRequestRecord build() {
            return new PaymentRequestRecord(
                userId,
                subscriptionPlanId,
                amount,
                currency,
                paymentMethod,
                paymentGateway,
                savedPaymentMethodId,
                metadata,
                options
            );
        }
    }

    /**
     * Immutable Payment Metadata
     */
    public record PaymentMetadata(
        String planName,
        BillingCycle billingCycle,
        BigDecimal discountApplied,
        String description,
        Map<String, Object> additionalInfo
    ) {
        public PaymentMetadata {
            // Defensive copy for mutable Map
            additionalInfo = additionalInfo != null
                ? Map.copyOf(additionalInfo)
                : Map.of();
        }
    }

    /**
     * Immutable Payment Options
     */
    public record PaymentOptions(
        Boolean savePaymentMethod,
        Boolean setAsDefault,
        String returnUrl,
        String cancelUrl,
        String upiId,
        String cardToken,
        Map<String, Object> gatewayOptions
    ) {
        public PaymentOptions {
            savePaymentMethod = savePaymentMethod != null ? savePaymentMethod : false;
            setAsDefault = setAsDefault != null ? setAsDefault : false;

            // Defensive copy for mutable Map
            gatewayOptions = gatewayOptions != null
                ? Map.copyOf(gatewayOptions)
                : Map.of();
        }
    }
}
