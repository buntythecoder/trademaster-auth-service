package com.trademaster.payment.service;

import com.trademaster.payment.common.Result;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Functional Payment Service
 * 
 * MANDATORY: Functional Programming - Rule #3
 * MANDATORY: Error Handling Patterns - Rule #11  
 * MANDATORY: Pattern Matching - Rule #14
 * MANDATORY: Resilience Patterns - Rule #24
 * MANDATORY: Virtual Threads - Rule #12
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FunctionalPaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final FunctionalRazorpayService functionalRazorpayService;
    private final FunctionalStripeService functionalStripeService;
    private final SubscriptionService subscriptionService;
    private final PaymentMetricsService metricsService;
    private final StructuredLoggingService loggingService;
    private final ApplicationEventPublisher eventPublisher;
    
    private static final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(4);
    
    /**
     * Process a payment request with functional programming patterns
     * 
     * MANDATORY: Functional Programming - Rule #3
     * MANDATORY: Virtual Threads - Rule #12
     * MANDATORY: Result<T,E> Pattern - Rule #11
     */
    @Transactional
    public CompletableFuture<Result<PaymentResponse, String>> processPayment(PaymentRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        return initializePaymentContext(request, correlationId, startTime)
            .thenCompose(contextResult -> contextResult.match(
                context -> processPaymentWithResilience(request, context),
                error -> CompletableFuture.completedFuture(Result.<PaymentResponse, String>failure(error))
            ));
    }
    
    /**
     * Initialize payment processing context
     */
    private CompletableFuture<Result<PaymentContext, String>> initializePaymentContext(
            PaymentRequest request, 
            String correlationId, 
            Instant startTime) {
        
        return CompletableFuture.supplyAsync(() -> 
            Result.tryExecute(() -> {
                // Set logging context
                loggingService.setCorrelationId(correlationId);
                loggingService.setBusinessContext(
                    null, 
                    Optional.ofNullable(request.getSubscriptionPlanId())
                        .map(UUID::toString)
                        .orElse(null)
                );
                
                // Record metrics - payment initiated
                metricsService.recordPaymentInitiated(
                    request.getPaymentGateway().name(),
                    request.getPaymentMethod().name()
                );
                
                log.info("Processing payment request for user: {}, amount: {} {}", 
                        request.getUserId(), request.getAmount(), request.getCurrency());
                
                return PaymentContext.builder()
                    .correlationId(correlationId)
                    .startTime(startTime)
                    .request(request)
                    .build();
            }).mapError(exception -> "Failed to initialize payment context: " + exception.getMessage()),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Process payment with circuit breaker protection
     * 
     * MANDATORY: Resilience Patterns - Rule #24
     */
    private CompletableFuture<Result<PaymentResponse, String>> processPaymentWithResilience(
            PaymentRequest request, 
            PaymentContext context) {
        
        return createPaymentTransaction(request)
            .thenCompose(transactionResult -> transactionResult.match(
                transaction -> executePaymentWithCircuitBreaker(request, transaction, context),
                error -> CompletableFuture.completedFuture(Result.<PaymentResponse, String>failure(error))
            ));
    }
    
    /**
     * Execute payment with functional gateway services (circuit breaker protection included)
     */
    private CompletableFuture<Result<PaymentResponse, String>> executePaymentWithCircuitBreaker(
            PaymentRequest request, 
            PaymentTransaction transaction, 
            PaymentContext context) {
        
        // Functional gateway services already provide circuit breaker protection
        return routeToGateway(request, transaction)
            .thenCompose(result -> result.match(
                response -> handlePaymentResult(response, transaction, request, context),
                error -> handlePaymentFailure(error, transaction, request, context)
            ));
    }
    
    
    /**
     * Route payment to appropriate gateway using pattern matching
     * 
     * MANDATORY: Pattern Matching - Rule #14
     * MANDATORY: Functional Programming - Rule #3
     */
    private CompletableFuture<Result<PaymentResponse, String>> routeToGateway(PaymentRequest request, PaymentTransaction transaction) {
        return switch (request.getPaymentGateway()) {
            case RAZORPAY -> processRazorpayPayment(request, transaction);
            case STRIPE -> processStripePayment(request, transaction);
            case UPI -> processUpiPayment(request, transaction);
            default -> CompletableFuture.completedFuture(Result.failure("Unsupported payment gateway: " + request.getPaymentGateway()));
        };
    }
    
    /**
     * Process Razorpay payment with functional patterns
     */
    private CompletableFuture<Result<PaymentResponse, String>> processRazorpayPayment(PaymentRequest request, PaymentTransaction transaction) {
        return functionalRazorpayService.processPayment(request, transaction);
    }
    
    /**
     * Process Stripe payment with functional patterns
     */
    private CompletableFuture<Result<PaymentResponse, String>> processStripePayment(PaymentRequest request, PaymentTransaction transaction) {
        return functionalStripeService.processPayment(request, transaction);
    }
    
    /**
     * Process UPI payment through Razorpay
     */
    private CompletableFuture<Result<PaymentResponse, String>> processUpiPayment(PaymentRequest request, PaymentTransaction transaction) {
        return functionalRazorpayService.processUpiPayment(request, transaction);
    }
    
    /**
     * Create payment transaction with functional error handling
     */
    private CompletableFuture<Result<PaymentTransaction, String>> createPaymentTransaction(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            Result.tryExecute(() -> {
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
            }).mapError(exception -> "Failed to create payment transaction: " + exception.getMessage()),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Handle successful payment result
     */
    private CompletableFuture<Result<PaymentResponse, String>> handlePaymentResult(
            PaymentResponse response,
            PaymentTransaction transaction,
            PaymentRequest request,
            PaymentContext context) {
        
        return updateTransactionFromResponse(transaction, response)
            .thenCompose(updateResult -> updateResult.match(
                updatedTransaction -> {
                    if (response.isSuccessful()) {
                        return handleSuccessfulPayment(response, updatedTransaction, request, context);
                    } else {
                        return handlePaymentFailure("Payment failed: " + response.getMessage(), updatedTransaction, request, context);
                    }
                },
                error -> CompletableFuture.completedFuture(Result.<PaymentResponse, String>failure(error))
            ));
    }
    
    /**
     * Handle payment failure with functional patterns
     */
    private CompletableFuture<Result<PaymentResponse, String>> handlePaymentFailure(
            String error,
            PaymentTransaction transaction,
            PaymentRequest request,
            PaymentContext context) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.error("Payment processing failed for user: {}, error: {}", request.getUserId(), error);
            
            // Record metrics - payment failed
            long processingTimeMs = Duration.between(context.getStartTime(), Instant.now()).toMillis();
            metricsService.recordPaymentFailed(
                request.getPaymentGateway().name(),
                request.getPaymentMethod().name(),
                "ProcessingError",
                processingTimeMs
            );
            
            // Log business audit for payment failure
            logPaymentFailure(request, context, error, processingTimeMs);
            
            // Publish payment failed event
            eventPublisher.publishEvent(new PaymentFailedEvent(
                request.getUserId(),
                request.getAmount(),
                request.getCurrency(),
                error
            ));
            
            PaymentResponse failedResponse = PaymentResponse.failed("Payment processing failed: " + error);
            return Result.<PaymentResponse, String>success(failedResponse);
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Handle successful payment with functional patterns
     */
    private CompletableFuture<Result<PaymentResponse, String>> handleSuccessfulPayment(
            PaymentResponse response,
            PaymentTransaction transaction,
            PaymentRequest request,
            PaymentContext context) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Record metrics - payment completed successfully
            long processingTimeMs = Duration.between(context.getStartTime(), Instant.now()).toMillis();
            metricsService.recordPaymentCompleted(
                request.getPaymentGateway().name(),
                request.getPaymentMethod().name(),
                request.getCurrency(),
                processingTimeMs
            );
            
            // Log business audit for successful payment
            logSuccessfulPayment(transaction, request, context, response, processingTimeMs);
            
            // Activate subscription asynchronously
            activateSubscriptionAsync(transaction, request);
            
            log.info("Payment processing completed for transaction: {}, status: {}", 
                    transaction.getId(), response.getStatus());
            
            return Result.<PaymentResponse, String>success(response);
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Update transaction with gateway response data
     */
    private CompletableFuture<Result<PaymentTransaction, String>> updateTransactionFromResponse(
            PaymentTransaction transaction, 
            PaymentResponse response) {
        
        return CompletableFuture.supplyAsync(() -> 
            Result.tryExecute(() -> {
                transaction.setStatus(response.getStatus());
                
                Optional.ofNullable(response.getGatewayResponse()).ifPresent(gatewayResponse -> {
                    transaction.setGatewayOrderId(gatewayResponse.getOrderId());
                    transaction.setGatewayPaymentId(gatewayResponse.getPaymentId());
                });
                
                Optional.ofNullable(response.getReceiptNumber())
                    .ifPresent(transaction::setReceiptNumber);
                
                return paymentRepository.save(transaction);
            }).mapError(exception -> "Failed to update transaction: " + exception.getMessage()),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Activate subscription asynchronously with virtual threads
     */
    private void activateSubscriptionAsync(PaymentTransaction transaction, PaymentRequest request) {
        CompletableFuture
            .supplyAsync(() -> 
                Optional.ofNullable(request.getSubscriptionPlanId())
                    .map(planId -> 
                        Result.tryExecute(() -> {
                            subscriptionService.activateSubscription(
                                request.getUserId(),
                                planId,
                                transaction.getId()
                            );
                            
                            // Log subscription activation audit
                            logSubscriptionActivation(request, transaction);
                            
                            // Publish payment completed event
                            eventPublisher.publishEvent(new PaymentCompletedEvent(
                                transaction.getId(),
                                transaction.getUserId(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                planId
                            ));
                            
                            return "Subscription activated successfully";
                        }).mapError(exception -> "Subscription activation failed: " + exception.getMessage())
                    ).orElse(Result.success("No subscription to activate")),
                Executors.newVirtualThreadPerTaskExecutor()
            )
            .thenAccept(result -> result.match(
                success -> {
                    log.info("Subscription activation completed: {}", success);
                    return null;
                },
                error -> {
                    log.error("Subscription activation failed: {}", error);
                    logSubscriptionActivationError(request, transaction, error);
                    return null;
                }
            ));
    }
    
    /**
     * Get payment transaction by ID with functional patterns
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<PaymentTransaction, String>> getTransaction(UUID transactionId) {
        return CompletableFuture.supplyAsync(() -> 
            paymentRepository.findById(transactionId)
                .map(transaction -> Result.<PaymentTransaction, String>success(transaction))
                .orElse(Result.failure("Transaction not found: " + transactionId)),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get payment status by transaction ID
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<PaymentStatus, String>> getPaymentStatus(UUID transactionId) {
        return getTransaction(transactionId)
            .thenApply(result -> result.map(PaymentTransaction::getStatus));
    }
    
    /**
     * Get payment history for a user with functional patterns
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<PaymentTransaction>, String>> getPaymentHistory(
            UUID userId, 
            Pageable pageable) {
        
        return CompletableFuture.supplyAsync(() -> 
            Result.tryExecute(() -> paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .mapError(exception -> "Failed to retrieve payment history: " + exception.getMessage()),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Logging helper methods
    
    private void logSuccessfulPayment(PaymentTransaction transaction, PaymentRequest request, 
                                    PaymentContext context, PaymentResponse response, long processingTimeMs) {
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
                "gateway_order_id", Optional.ofNullable(response.getGatewayResponse())
                    .map(gw -> gw.getOrderId()).orElse(""),
                "subscription_plan_id", Optional.ofNullable(request.getSubscriptionPlanId())
                    .map(UUID::toString).orElse("")
            )
        );
    }
    
    private void logPaymentFailure(PaymentRequest request, PaymentContext context, 
                                 String error, long processingTimeMs) {
        loggingService.logPaymentTransaction(
            "payment_failed",
            context.getCorrelationId(),
            request.getUserId().toString(),
            request.getPaymentGateway().name(),
            request.getAmount().toString(),
            request.getCurrency(),
            "FAILED",
            Map.of(
                "error_type", "ProcessingError",
                "error_message", error,
                "payment_method", request.getPaymentMethod().name(),
                "processing_time_ms", processingTimeMs
            )
        );
    }
    
    private void logSubscriptionActivation(PaymentRequest request, PaymentTransaction transaction) {
        Optional.ofNullable(request.getSubscriptionPlanId()).ifPresent(planId -> 
            loggingService.logSubscriptionEvent(
                "subscription_activated",
                "GENERATED_ID",
                request.getUserId().toString(),
                planId.toString(),
                Optional.ofNullable(request.getMetadata())
                    .map(metadata -> metadata.getPlanName())
                    .orElse(""),
                request.getAmount().toString(),
                Optional.ofNullable(request.getMetadata())
                    .map(metadata -> metadata.getBillingCycle().getDisplayName())
                    .orElse(""),
                "ACTIVE"
            )
        );
    }
    
    private void logSubscriptionActivationError(PaymentRequest request, PaymentTransaction transaction, String error) {
        loggingService.logError(
            "subscription_activation",
            "Failed to activate subscription after successful payment",
            "SUBSCRIPTION_ACTIVATION_FAILED",
            new RuntimeException(error),
            Map.of(
                "transaction_id", transaction.getId().toString(),
                "user_id", request.getUserId().toString(),
                "subscription_plan_id", Optional.ofNullable(request.getSubscriptionPlanId())
                    .map(UUID::toString).orElse("")
            )
        );
    }
    
    /**
     * Build transaction description from request with functional patterns
     */
    private String buildTransactionDescription(PaymentRequest request) {
        return Optional.ofNullable(request.getMetadata())
            .map(metadata -> {
                StringBuilder description = new StringBuilder("TradeMaster Subscription");
                
                Optional.ofNullable(metadata.getPlanName())
                    .ifPresent(planName -> description.append(" - ").append(planName));
                
                Optional.ofNullable(metadata.getBillingCycle())
                    .ifPresent(cycle -> description.append(" (").append(cycle.getDisplayName()).append(")"));
                
                return description.toString();
            })
            .orElse("TradeMaster Subscription");
    }
    
    /**
     * Payment processing context record
     */
    private static record PaymentContext(
        String correlationId,
        Instant startTime,
        PaymentRequest request
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String correlationId;
            private Instant startTime;
            private PaymentRequest request;
            
            public Builder correlationId(String correlationId) {
                this.correlationId = correlationId;
                return this;
            }
            
            public Builder startTime(Instant startTime) {
                this.startTime = startTime;
                return this;
            }
            
            public Builder request(PaymentRequest request) {
                this.request = request;
                return this;
            }
            
            public PaymentContext build() {
                return new PaymentContext(correlationId, startTime, request);
            }
        }
        
        public String getCorrelationId() { return correlationId; }
        public Instant getStartTime() { return startTime; }
        public PaymentRequest getRequest() { return request; }
    }
}