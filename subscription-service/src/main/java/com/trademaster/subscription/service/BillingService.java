package com.trademaster.subscription.service;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.event.SubscriptionEvent;
import com.trademaster.subscription.event.SubscriptionEventPublisher;
import com.trademaster.subscription.exception.SubscriptionNotFoundException;
import com.trademaster.subscription.repository.SubscriptionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Billing Service
 * 
 * Handles subscription billing operations and payment integration.
 * Integrates with payment gateway service for processing transactions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventPublisher eventPublisher;
    private final StructuredLoggingService loggingService;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.services.payment-gateway.url}")
    private String paymentGatewayUrl;

    @Value("${app.services.payment-gateway.api-key}")
    private String paymentGatewayApiKey;

    @Value("${app.billing.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.billing.grace-period-days:7}")
    private int gracePeriodDays;

    // Metrics
    private final Counter billingSuccessCounter;
    private final Counter billingFailureCounter;
    private final Counter retryAttemptCounter;
    private final Timer billingTimer;

    public BillingService(SubscriptionRepository subscriptionRepository,
                         SubscriptionEventPublisher eventPublisher,
                         StructuredLoggingService loggingService,
                         RestTemplate restTemplate,
                         MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.loggingService = loggingService;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.billingSuccessCounter = Counter.builder("subscription.billing.success")
            .description("Number of successful billing operations")
            .register(meterRegistry);
            
        this.billingFailureCounter = Counter.builder("subscription.billing.failure")
            .description("Number of failed billing operations")
            .register(meterRegistry);
            
        this.retryAttemptCounter = Counter.builder("subscription.billing.retry")
            .description("Number of billing retry attempts")
            .register(meterRegistry);
            
        this.billingTimer = Timer.builder("subscription.billing.duration")
            .description("Time taken for billing operations")
            .register(meterRegistry);
    }

    /**
     * Process initial subscription payment
     */
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public CompletableFuture<UUID> processInitialPayment(UUID subscriptionId, UUID paymentMethodId, 
                                                        String promoCode) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

                BigDecimal amount = calculateBillingAmount(subscription.getTier(), subscription.getBillingCycle());
                if (promoCode != null) {
                    amount = applyPromoCode(amount, promoCode);
                }

                PaymentRequest paymentRequest = PaymentRequest.builder()
                    .amount(amount)
                    .currency("USD")
                    .paymentMethodId(paymentMethodId)
                    .customerId(subscription.getUserId().toString())
                    .description("TradeMaster " + subscription.getTier() + " Subscription")
                    .metadata(Map.of(
                        "subscriptionId", subscriptionId.toString(),
                        "tier", subscription.getTier().toString(),
                        "billingCycle", subscription.getBillingCycle().toString()
                    ))
                    .build();

                UUID transactionId = processPayment(paymentRequest);
                
                // Update subscription with successful payment
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(LocalDateTime.now());
                subscription.setNextBillingDate(calculateNextBillingDate(subscription.getBillingCycle()));
                subscription.setFailedBillingAttempts(0);
                subscription.setLastBillingDate(LocalDateTime.now());
                subscriptionRepository.save(subscription);

                billingSuccessCounter.increment();
                
                loggingService.logBusinessEvent(
                    "initial_payment_processed",
                    subscription.getUserId().toString(),
                    subscriptionId.toString(),
                    "billing_success",
                    Map.of("transactionId", transactionId.toString(), "amount", amount.toString())
                );

                // Publish subscription activated event
                eventPublisher.publishSubscriptionActivated(
                    SubscriptionEvent.subscriptionActivated(
                        subscriptionId,
                        subscription.getUserId(),
                        subscription.getTier(),
                        transactionId,
                        UUID.randomUUID().toString()
                    )
                );

                return transactionId;

            } catch (Exception e) {
                billingFailureCounter.increment();
                log.error("Failed to process initial payment for subscription: {}", subscriptionId, e);
                throw e;
            } finally {
                sample.stop(billingTimer);
            }
        });
    }

    /**
     * Process recurring billing
     */
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public CompletableFuture<Boolean> processRecurringBilling(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

                if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                    log.warn("Cannot process billing for non-active subscription: {}", subscriptionId);
                    return false;
                }

                BigDecimal amount = calculateBillingAmount(subscription.getTier(), subscription.getBillingCycle());

                PaymentRequest paymentRequest = PaymentRequest.builder()
                    .amount(amount)
                    .currency("USD")
                    .paymentMethodId(subscription.getPaymentMethodId())
                    .customerId(subscription.getUserId().toString())
                    .description("TradeMaster " + subscription.getTier() + " Subscription Renewal")
                    .metadata(Map.of(
                        "subscriptionId", subscriptionId.toString(),
                        "tier", subscription.getTier().toString(),
                        "billingCycle", subscription.getBillingCycle().toString(),
                        "recurringPayment", "true"
                    ))
                    .build();

                try {
                    UUID transactionId = processPayment(paymentRequest);
                    
                    // Update subscription with successful payment
                    subscription.setNextBillingDate(calculateNextBillingDate(subscription.getBillingCycle()));
                    subscription.setFailedBillingAttempts(0);
                    subscription.setLastBillingDate(LocalDateTime.now());
                    subscriptionRepository.save(subscription);

                    billingSuccessCounter.increment();
                    
                    loggingService.logBusinessEvent(
                        "recurring_payment_processed",
                        subscription.getUserId().toString(),
                        subscriptionId.toString(),
                        "billing_success",
                        Map.of("transactionId", transactionId.toString(), "amount", amount.toString())
                    );

                    // Publish subscription renewed event
                    eventPublisher.publishSubscriptionEvent(
                        SubscriptionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType(SubscriptionEvent.SUBSCRIPTION_RENEWED)
                            .subscriptionId(subscriptionId)
                            .userId(subscription.getUserId())
                            .tier(subscription.getTier())
                            .status(SubscriptionStatus.ACTIVE)
                            .timestamp(LocalDateTime.now())
                            .source("subscription-service")
                            .version("1.0")
                            .payload(Map.of("transactionId", transactionId.toString()))
                            .build()
                    );

                    return true;

                } catch (PaymentException e) {
                    return handlePaymentFailure(subscription, e);
                }

            } catch (Exception e) {
                billingFailureCounter.increment();
                log.error("Failed to process recurring billing for subscription: {}", subscriptionId, e);
                return false;
            } finally {
                sample.stop(billingTimer);
            }
        });
    }

    /**
     * Handle payment upgrade billing
     */
    @Async("subscriptionProcessingExecutor")
    @Transactional
    public CompletableFuture<UUID> processUpgradePayment(UUID subscriptionId, SubscriptionTier newTier,
                                                        BillingCycle newBillingCycle, String promoCode) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found: " + subscriptionId));

                // Calculate prorated amount
                BigDecimal upgradeAmount = calculateUpgradeAmount(
                    subscription.getTier(), newTier, 
                    subscription.getBillingCycle(), newBillingCycle,
                    subscription.getNextBillingDate()
                );

                if (promoCode != null) {
                    upgradeAmount = applyPromoCode(upgradeAmount, promoCode);
                }

                if (upgradeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    PaymentRequest paymentRequest = PaymentRequest.builder()
                        .amount(upgradeAmount)
                        .currency("USD")
                        .paymentMethodId(subscription.getPaymentMethodId())
                        .customerId(subscription.getUserId().toString())
                        .description("TradeMaster Subscription Upgrade")
                        .metadata(Map.of(
                            "subscriptionId", subscriptionId.toString(),
                            "oldTier", subscription.getTier().toString(),
                            "newTier", newTier.toString(),
                            "upgrade", "true"
                        ))
                        .build();

                    UUID transactionId = processPayment(paymentRequest);
                    
                    billingSuccessCounter.increment();
                    
                    loggingService.logBusinessEvent(
                        "upgrade_payment_processed",
                        subscription.getUserId().toString(),
                        subscriptionId.toString(),
                        "billing_success",
                        Map.of("transactionId", transactionId.toString(), "amount", upgradeAmount.toString())
                    );

                    return transactionId;
                }

                // No payment needed for downgrade or same tier
                return null;

            } catch (Exception e) {
                billingFailureCounter.increment();
                log.error("Failed to process upgrade payment for subscription: {}", subscriptionId, e);
                throw e;
            } finally {
                sample.stop(billingTimer);
            }
        });
    }

    /**
     * Process payment via payment gateway
     */
    private UUID processPayment(PaymentRequest paymentRequest) throws PaymentException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + paymentGatewayApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<PaymentRequest> request = new HttpEntity<>(paymentRequest, headers);
            
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                paymentGatewayUrl + "/api/v1/payments",
                HttpMethod.POST,
                request,
                PaymentResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PaymentResponse paymentResponse = response.getBody();
                if ("succeeded".equals(paymentResponse.getStatus())) {
                    return paymentResponse.getTransactionId();
                } else {
                    throw new PaymentException("Payment failed: " + paymentResponse.getErrorMessage());
                }
            } else {
                throw new PaymentException("Payment gateway returned error response");
            }

        } catch (Exception e) {
            log.error("Error processing payment", e);
            throw new PaymentException("Failed to process payment: " + e.getMessage(), e);
        }
    }

    /**
     * Handle payment failure
     */
    private boolean handlePaymentFailure(Subscription subscription, PaymentException e) {
        subscription.setFailedBillingAttempts(subscription.getFailedBillingAttempts() + 1);
        
        if (subscription.getFailedBillingAttempts() >= maxRetryAttempts) {
            // Suspend subscription after max retries
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            subscription.setSuspensionReason("Payment failed after " + maxRetryAttempts + " attempts");
            
            loggingService.logBusinessEvent(
                "subscription_suspended_payment_failure",
                subscription.getUserId().toString(),
                subscription.getId().toString(),
                "billing_failure",
                Map.of("failedAttempts", subscription.getFailedBillingAttempts())
            );

            // Publish suspension event
            eventPublisher.publishSubscriptionEvent(
                SubscriptionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(SubscriptionEvent.SUBSCRIPTION_SUSPENDED)
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUserId())
                    .tier(subscription.getTier())
                    .status(SubscriptionStatus.SUSPENDED)
                    .timestamp(LocalDateTime.now())
                    .source("subscription-service")
                    .version("1.0")
                    .payload(Map.of("reason", "payment_failure"))
                    .build()
            );
            
        } else {
            // Mark for retry
            subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);
            retryAttemptCounter.increment();
            
            // Publish payment failed event
            eventPublisher.publishPaymentFailed(
                SubscriptionEvent.paymentFailed(
                    subscription.getId(),
                    subscription.getUserId(),
                    subscription.getTier(),
                    subscription.getFailedBillingAttempts(),
                    e.getMessage(),
                    UUID.randomUUID().toString()
                )
            );
        }
        
        subscriptionRepository.save(subscription);
        billingFailureCounter.increment();
        return false;
    }

    /**
     * Calculate billing amount based on tier and cycle
     */
    private BigDecimal calculateBillingAmount(SubscriptionTier tier, BillingCycle cycle) {
        return switch (cycle) {
            case MONTHLY -> tier.getMonthlyPrice();
            case QUARTERLY -> tier.getQuarterlyPrice();
            case ANNUALLY -> tier.getAnnualPrice();
        };
    }

    /**
     * Calculate upgrade amount with proration
     */
    private BigDecimal calculateUpgradeAmount(SubscriptionTier oldTier, SubscriptionTier newTier,
                                            BillingCycle oldCycle, BillingCycle newCycle,
                                            LocalDateTime nextBillingDate) {
        // Simplified proration calculation
        BigDecimal oldAmount = calculateBillingAmount(oldTier, oldCycle);
        BigDecimal newAmount = calculateBillingAmount(newTier, newCycle);
        
        // Calculate remaining days in current billing period
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), nextBillingDate);
        long totalDaysInCycle = switch (oldCycle) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case ANNUALLY -> 365;
        };
        
        // Calculate prorated credit from old subscription
        BigDecimal proratedCredit = oldAmount.multiply(BigDecimal.valueOf(daysRemaining))
                                           .divide(BigDecimal.valueOf(totalDaysInCycle), 2, BigDecimal.ROUND_HALF_UP);
        
        // Calculate prorated charge for new subscription
        BigDecimal proratedCharge = newAmount.multiply(BigDecimal.valueOf(daysRemaining))
                                           .divide(BigDecimal.valueOf(totalDaysInCycle), 2, BigDecimal.ROUND_HALF_UP);
        
        return proratedCharge.subtract(proratedCredit).max(BigDecimal.ZERO);
    }

    /**
     * Apply promo code discount
     */
    private BigDecimal applyPromoCode(BigDecimal amount, String promoCode) {
        // Simplified promo code logic - in real implementation, this would query promo code service
        Map<String, BigDecimal> promoCodes = Map.of(
            "WELCOME20", new BigDecimal("0.20"),
            "SUMMER15", new BigDecimal("0.15"),
            "NEWUSER10", new BigDecimal("0.10")
        );
        
        BigDecimal discount = promoCodes.getOrDefault(promoCode.toUpperCase(), BigDecimal.ZERO);
        return amount.multiply(BigDecimal.ONE.subtract(discount));
    }

    /**
     * Calculate next billing date
     */
    private LocalDateTime calculateNextBillingDate(BillingCycle cycle) {
        LocalDateTime now = LocalDateTime.now();
        return switch (cycle) {
            case MONTHLY -> now.plusMonths(1);
            case QUARTERLY -> now.plusMonths(3);
            case ANNUALLY -> now.plusYears(1);
        };
    }

    /**
     * Get subscriptions due for billing
     */
    @Transactional(readOnly = true)
    public CompletableFuture<List<Subscription>> getSubscriptionsDueForBilling() {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime now = LocalDateTime.now();
            return subscriptionRepository.findByNextBillingDateBeforeAndStatus(now, SubscriptionStatus.ACTIVE);
        });
    }

    // Inner classes for payment processing
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PaymentRequest {
        private BigDecimal amount;
        private String currency;
        private UUID paymentMethodId;
        private String customerId;
        private String description;
        private Map<String, String> metadata;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PaymentResponse {
        private UUID transactionId;
        private String status;
        private String errorMessage;
        private Map<String, Object> details;
    }

    private static class PaymentException extends Exception {
        public PaymentException(String message) {
            super(message);
        }
        
        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}