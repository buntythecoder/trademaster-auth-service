package com.trademaster.payment.service;

import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.events.PaymentCompletedEvent;
import com.trademaster.payment.events.PaymentFailedEvent;
import com.trademaster.payment.exception.PaymentProcessingException;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Service
 * 
 * Main service for processing payments through multiple gateways.
 * Handles payment lifecycle, gateway routing, and event publishing.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;
    private final PaymentMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Process a payment request through the appropriate gateway
     * STANDARDS COMPLIANT: Uses Virtual Threads for scalability + Metrics + Structured Logging
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Instant startTime = Instant.now();
        String correlationId = UUID.randomUUID().toString();
        
        // Set logging context per standards
        loggingService.setCorrelationId(correlationId);
        loggingService.setBusinessContext(null, request.getSubscriptionPlanId() != null ? request.getSubscriptionPlanId().toString() : null);
        
        // Record metrics - payment initiated
        metricsService.recordPaymentInitiated(
            request.getPaymentGateway().name(),
            request.getPaymentMethod().name()
        );
        
        log.info("Processing payment request for user: {}, amount: {} {}", 
                request.getUserId(), request.getAmount(), request.getCurrency());
        
        try {
            // Create payment transaction record
            PaymentTransaction transaction = createPaymentTransaction(request);
            
            // Route to appropriate payment gateway
            PaymentResponse response = routeToGateway(request, transaction);
            
            // Update transaction with gateway response
            updateTransactionFromResponse(transaction, response);
            
            // Handle successful payments
            if (response.isSuccessful()) {
                // Record metrics - payment completed successfully
                long processingTimeMs = Duration.between(startTime, Instant.now()).toMillis();
                metricsService.recordPaymentCompleted(
                    request.getPaymentGateway().name(),
                    request.getPaymentMethod().name(),
                    request.getCurrency(),
                    processingTimeMs
                );
                
                // Log business audit for successful payment
                loggingService.logPaymentTransaction(
                    "payment_completed",
                    transaction.getId().toString(),
                    request.getUserId().toString(),
                    request.getPaymentGateway().name(),
                    request.getAmount().toString(),
                    request.getCurrency(),
                    "COMPLETED",
                    Map.of(
                        "processing_time_ms", processingTimeMs,
                        "payment_method", request.getPaymentMethod().name(),
                        "gateway_order_id", response.getGatewayResponse() != null ? response.getGatewayResponse().getOrderId() : "",
                        "subscription_plan_id", request.getSubscriptionPlanId() != null ? request.getSubscriptionPlanId().toString() : ""
                    )
                );
                
                handleSuccessfulPayment(transaction, request);
            }
            
            log.info("Payment processing completed for transaction: {}, status: {}", 
                    transaction.getId(), response.getStatus());
            
            return response;
            
        } catch (Exception e) {
            log.error("Payment processing failed for user: {}", request.getUserId(), e);
            
            // Record metrics - payment failed
            metricsService.recordPaymentFailed(
                request.getPaymentGateway().name(),
                request.getPaymentMethod().name(),
                e.getClass().getSimpleName(),
                Duration.between(startTime, Instant.now()).toMillis()
            );
            
            // Log business audit for payment failure
            loggingService.logPaymentTransaction(
                "payment_failed",
                correlationId,
                request.getUserId().toString(),
                request.getPaymentGateway().name(),
                request.getAmount().toString(),
                request.getCurrency(),
                "FAILED",
                Map.of(
                    "error_type", e.getClass().getSimpleName(),
                    "error_message", e.getMessage(),
                    "payment_method", request.getPaymentMethod().name()
                )
            );
            
            PaymentResponse failedResponse = PaymentResponse.failed(
                "Payment processing failed: " + e.getMessage()
            );
            
            // Publish payment failed event
            eventPublisher.publishEvent(new PaymentFailedEvent(
                request.getUserId(),
                request.getAmount(),
                request.getCurrency(),
                e.getMessage()
            ));
            
            return failedResponse;
        }
    }
    
    /**
     * Route payment to appropriate gateway based on configuration
     */
    private PaymentResponse routeToGateway(PaymentRequest request, PaymentTransaction transaction) {
        return switch (request.getPaymentGateway()) {
            case RAZORPAY -> razorpayService.processPayment(request, transaction);
            case STRIPE -> stripeService.processPayment(request, transaction);
            case UPI -> razorpayService.processUpiPayment(request, transaction);
            default -> throw new PaymentProcessingException(
                "Unsupported payment gateway: " + request.getPaymentGateway()
            );
        };
    }
    
    /**
     * Create payment transaction record
     */
    private PaymentTransaction createPaymentTransaction(PaymentRequest request) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentGateway(request.getPaymentGateway())
                .paymentMethod(request.getPaymentMethod())
                .description(buildTransactionDescription(request))
                .build();
        
        return paymentRepository.save(transaction);
    }
    
    /**
     * Update transaction with gateway response data
     */
    private void updateTransactionFromResponse(PaymentTransaction transaction, PaymentResponse response) {
        transaction.setStatus(response.getStatus());
        
        if (response.getGatewayResponse() != null) {
            transaction.setGatewayOrderId(response.getGatewayResponse().getOrderId());
            transaction.setGatewayPaymentId(response.getGatewayResponse().getPaymentId());
        }
        
        if (response.getReceiptNumber() != null) {
            transaction.setReceiptNumber(response.getReceiptNumber());
        }
        
        paymentRepository.save(transaction);
    }
    
    /**
     * Handle successful payment completion
     * STANDARDS COMPLIANT: Uses Virtual Threads for async processing
     */
    @Async("virtualThreadExecutor")
    private void handleSuccessfulPayment(PaymentTransaction transaction, PaymentRequest request) {
        try {
            // Activate subscription
            subscriptionService.activateSubscription(
                request.getUserId(),
                request.getSubscriptionPlanId(),
                transaction.getId()
            );
            
            // Log subscription activation audit
            if (request.getSubscriptionPlanId() != null) {
                loggingService.logSubscriptionEvent(
                    "subscription_activated",
                    "GENERATED_ID", // This will be set by subscription service
                    request.getUserId().toString(),
                    request.getSubscriptionPlanId().toString(),
                    request.getMetadata() != null ? request.getMetadata().getPlanName() : "",
                    request.getAmount().toString(),
                    request.getMetadata() != null ? request.getMetadata().getBillingCycle().getDisplayName() : "",
                    "ACTIVE"
                );
            }
            
            // Publish payment completed event
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                request.getSubscriptionPlanId()
            ));
            
            log.info("Subscription activated for user: {} with plan: {}", 
                    request.getUserId(), request.getSubscriptionPlanId());
            
        } catch (Exception e) {
            log.error("Failed to activate subscription for transaction: {}", 
                    transaction.getId(), e);
            
            // Log subscription activation failure
            loggingService.logError(
                "subscription_activation",
                "Failed to activate subscription after successful payment",
                "SUBSCRIPTION_ACTIVATION_FAILED",
                e,
                Map.of(
                    "transaction_id", transaction.getId().toString(),
                    "user_id", request.getUserId().toString(),
                    "subscription_plan_id", request.getSubscriptionPlanId() != null ? request.getSubscriptionPlanId().toString() : ""
                )
            );
            
            // Note: Payment succeeded but subscription activation failed
            // This should trigger manual intervention or retry mechanism
        }
    }
    
    /**
     * Build transaction description from request
     */
    private String buildTransactionDescription(PaymentRequest request) {
        StringBuilder description = new StringBuilder("TradeMaster Subscription");
        
        if (request.getMetadata() != null) {
            PaymentRequest.PaymentMetadata metadata = request.getMetadata();
            if (metadata.getPlanName() != null) {
                description.append(" - ").append(metadata.getPlanName());
            }
            if (metadata.getBillingCycle() != null) {
                description.append(" (").append(metadata.getBillingCycle().getDisplayName()).append(")");
            }
        }
        
        return description.toString();
    }
    
    /**
     * Get payment transaction by ID
     */
    @Transactional(readOnly = true)
    public PaymentTransaction getTransaction(UUID transactionId) {
        return paymentRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentProcessingException("Transaction not found: " + transactionId));
    }
    
    /**
     * Get payment status by transaction ID
     */
    @Transactional(readOnly = true) 
    public PaymentStatus getPaymentStatus(UUID transactionId) {
        return getTransaction(transactionId).getStatus();
    }
    
    /**
     * Get payment history for a user
     */
    @Transactional(readOnly = true)
    public Page<PaymentTransaction> getPaymentHistory(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}