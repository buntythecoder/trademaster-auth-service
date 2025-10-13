package com.trademaster.payment.enums;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Payment Gateway Enumeration with Functional Strategy Pattern
 * Functional implementation with NO if-else statements
 *
 * Compliance:
 * - Rule 3: Functional Programming First - NO if-else, strategy-based selection
 * - Rule 4: Advanced Design Patterns - Strategy pattern with enum
 * - Rule 14: Pattern matching with functional predicates
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Getter
public enum PaymentGateway {
    RAZORPAY(
        "Razorpay",
        "Primary Indian payment gateway",
        "INR",
        List.of("INR"),
        List.of("IN"),
        gateway -> false,  // Not international
        gateway -> true    // Domestic
    ),
    STRIPE(
        "Stripe",
        "International payment gateway",
        "USD",
        List.of("USD", "EUR", "GBP", "AUD", "CAD", "SGD"),
        List.of("US", "GB", "EU", "AU", "CA", "SG"),
        gateway -> true,   // International
        gateway -> false   // Not domestic
    ),
    UPI(
        "UPI",
        "Unified Payments Interface",
        "INR",
        List.of("INR"),
        List.of("IN"),
        gateway -> false,  // Not international
        gateway -> true    // Domestic
    );

    private final String displayName;
    private final String description;
    private final String defaultCurrency;
    private final List<String> supportedCurrencies;
    private final List<String> supportedCountries;
    private final Predicate<PaymentGateway> internationalPredicate;
    private final Predicate<PaymentGateway> domesticPredicate;

    PaymentGateway(
            String displayName,
            String description,
            String defaultCurrency,
            List<String> supportedCurrencies,
            List<String> supportedCountries,
            Predicate<PaymentGateway> internationalPredicate,
            Predicate<PaymentGateway> domesticPredicate
    ) {
        this.displayName = displayName;
        this.description = description;
        this.defaultCurrency = defaultCurrency;
        this.supportedCurrencies = List.copyOf(supportedCurrencies);
        this.supportedCountries = List.copyOf(supportedCountries);
        this.internationalPredicate = internationalPredicate;
        this.domesticPredicate = domesticPredicate;
    }

    /**
     * Check if the gateway supports international payments
     * Functional predicate-based implementation (NO if-else)
     */
    public boolean isInternational() {
        return internationalPredicate.test(this);
    }

    /**
     * Check if the gateway is primarily for Indian market
     * Functional predicate-based implementation (NO if-else)
     */
    public boolean isDomestic() {
        return domesticPredicate.test(this);
    }

    /**
     * Check if gateway supports given currency
     * Functional collection operation (NO loops)
     */
    public boolean supportsCurrency(String currency) {
        return supportedCurrencies.stream()
            .anyMatch(c -> c.equalsIgnoreCase(currency));
    }

    /**
     * Check if gateway supports given country
     * Functional collection operation (NO loops)
     */
    public boolean supportsCountry(String countryCode) {
        return supportedCountries.stream()
            .anyMatch(c -> c.equalsIgnoreCase(countryCode));
    }

    /**
     * Functional gateway selection based on currency
     * Pattern matching with switch expression (NO if-else)
     */
    public static PaymentGateway selectByCurrency(String currency) {
        return switch (currency.toUpperCase()) {
            case "INR" -> RAZORPAY;
            case "USD", "EUR", "GBP", "AUD", "CAD", "SGD" -> STRIPE;
            default -> RAZORPAY; // Default fallback
        };
    }

    /**
     * Functional gateway selection based on country
     * Pattern matching with switch expression (NO if-else)
     */
    public static PaymentGateway selectByCountry(String countryCode) {
        return switch (countryCode.toUpperCase()) {
            case "IN" -> RAZORPAY;
            case "US", "GB", "EU", "AU", "CA", "SG" -> STRIPE;
            default -> RAZORPAY; // Default fallback
        };
    }

    /**
     * Functional gateway selection with priority strategy
     * Uses Map-based lookup (NO if-else)
     */
    private static final Map<String, PaymentGateway> CURRENCY_PRIORITY_MAP = Map.ofEntries(
        Map.entry("INR", RAZORPAY),
        Map.entry("USD", STRIPE),
        Map.entry("EUR", STRIPE),
        Map.entry("GBP", STRIPE)
    );

    public static PaymentGateway selectWithPriority(String currency, String countryCode) {
        return CURRENCY_PRIORITY_MAP.getOrDefault(
            currency.toUpperCase(),
            selectByCountry(countryCode)
        );
    }
}