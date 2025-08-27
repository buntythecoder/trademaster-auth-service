package com.trademaster.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.events.PaymentCompletedEvent;
import com.trademaster.payment.events.PaymentFailedEvent;
import com.trademaster.payment.exception.PaymentProcessingException;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Webhook Service
 * 
 * Processes payment gateway webhooks for real-time payment status updates.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentTransactionRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    /**
     * Process Razorpay webhook notifications
     */
    public void processRazorpayWebhook(String payload, String signature) {
        log.info("Processing Razorpay webhook");
        
        // Verify webhook signature
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Razorpay webhook signature received");
            throw new PaymentProcessingException("Invalid webhook signature");
        }
        
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("event").asText();
            JsonNode paymentData = webhookData.get("payload").get("payment").get("entity");
            
            String paymentId = paymentData.get("id").asText();
            String orderId = paymentData.get("order_id").asText();
            String status = paymentData.get("status").asText();
            
            log.info("Razorpay webhook: event={}, paymentId={}, status={}", eventType, paymentId, status);
            
            // Find transaction by gateway payment ID or order ID
            PaymentTransaction transaction = paymentRepository.findByGatewayPaymentId(paymentId)
                    .orElse(paymentRepository.findByGatewayOrderId(orderId)
                            .orElseThrow(() -> new PaymentProcessingException("Transaction not found for payment: " + paymentId)));
            
            // Update transaction based on webhook event
            updateTransactionFromRazorpayWebhook(transaction, eventType, status, paymentData);
            
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook", e);
            throw new PaymentProcessingException("Webhook processing failed", e);
        }
    }
    
    /**
     * Process Stripe webhook notifications
     */
    public void processStripeWebhook(String payload, String signature) {
        log.info("Processing Stripe webhook");
        
        // Verify webhook signature
        if (!stripeService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Stripe webhook signature received");
            throw new PaymentProcessingException("Invalid webhook signature");
        }
        
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("type").asText();
            JsonNode paymentIntentData = webhookData.get("data").get("object");
            
            String paymentIntentId = paymentIntentData.get("id").asText();
            String status = paymentIntentData.get("status").asText();
            
            log.info("Stripe webhook: event={}, paymentIntentId={}, status={}", eventType, paymentIntentId, status);
            
            // Find transaction by gateway payment ID
            PaymentTransaction transaction = paymentRepository.findByGatewayPaymentId(paymentIntentId)
                    .orElseThrow(() -> new PaymentProcessingException("Transaction not found for payment intent: " + paymentIntentId));
            
            // Update transaction based on webhook event
            updateTransactionFromStripeWebhook(transaction, eventType, status, paymentIntentData);
            
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook", e);
            throw new PaymentProcessingException("Webhook processing failed", e);
        }
    }
    
    /**
     * Process UPI webhook notifications
     */
    public void processUpiWebhook(String payload, String signature) {
        log.info("Processing UPI webhook");
        
        // UPI webhooks come through Razorpay, so use Razorpay verification
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid UPI webhook signature received");
            throw new PaymentProcessingException("Invalid webhook signature");
        }
        
        try {
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("event").asText();
            JsonNode paymentData = webhookData.get("payload").get("payment").get("entity");
            
            String paymentId = paymentData.get("id").asText();
            String orderId = paymentData.get("order_id").asText();
            String status = paymentData.get("status").asText();
            String method = paymentData.get("method").asText();
            
            log.info("UPI webhook: event={}, paymentId={}, status={}, method={}", 
                    eventType, paymentId, status, method);
            
            // Find transaction
            PaymentTransaction transaction = paymentRepository.findByGatewayPaymentId(paymentId)
                    .orElse(paymentRepository.findByGatewayOrderId(orderId)
                            .orElseThrow(() -> new PaymentProcessingException("Transaction not found for UPI payment: " + paymentId)));
            
            // Update transaction
            updateTransactionFromRazorpayWebhook(transaction, eventType, status, paymentData);
            
        } catch (Exception e) {
            log.error("Failed to process UPI webhook", e);
            throw new PaymentProcessingException("UPI webhook processing failed", e);
        }
    }
    
    /**
     * Update transaction from Razorpay webhook data
     */
    private void updateTransactionFromRazorpayWebhook(PaymentTransaction transaction, 
                                                      String eventType, String status, JsonNode paymentData) {
        
        PaymentStatus oldStatus = transaction.getStatus();
        PaymentStatus newStatus = mapRazorpayStatusToPaymentStatus(status);
        
        transaction.setStatus(newStatus);
        transaction.setGatewayPaymentId(paymentData.get("id").asText());
        
        // Handle specific events
        switch (eventType) {
            case "payment.captured" -> {
                transaction.markAsCompleted();
                handlePaymentSuccess(transaction);
            }
            case "payment.failed" -> {
                String errorDescription = paymentData.has("error_description") ? 
                    paymentData.get("error_description").asText() : "Payment failed";
                String errorCode = paymentData.has("error_code") ? 
                    paymentData.get("error_code").asText() : null;
                    
                transaction.markAsFailed(errorDescription, errorCode);
                handlePaymentFailure(transaction, errorDescription);
            }
            case "payment.authorized" -> {
                transaction.setStatus(PaymentStatus.PROCESSING);
            }
        }
        
        paymentRepository.save(transaction);
        
        log.info("Updated transaction {} status from {} to {} via Razorpay webhook", 
                transaction.getId(), oldStatus, newStatus);
    }
    
    /**
     * Update transaction from Stripe webhook data
     */
    private void updateTransactionFromStripeWebhook(PaymentTransaction transaction, 
                                                   String eventType, String status, JsonNode paymentIntentData) {
        
        PaymentStatus oldStatus = transaction.getStatus();
        PaymentStatus newStatus = mapStripeStatusToPaymentStatus(status);
        
        transaction.setStatus(newStatus);
        
        // Handle specific events
        switch (eventType) {
            case "payment_intent.succeeded" -> {
                transaction.markAsCompleted();
                handlePaymentSuccess(transaction);
            }
            case "payment_intent.payment_failed" -> {
                String errorMessage = paymentIntentData.has("last_payment_error") ?
                    paymentIntentData.get("last_payment_error").get("message").asText() : "Payment failed";
                String errorCode = paymentIntentData.has("last_payment_error") ?
                    paymentIntentData.get("last_payment_error").get("code").asText() : null;
                    
                transaction.markAsFailed(errorMessage, errorCode);
                handlePaymentFailure(transaction, errorMessage);
            }
            case "payment_intent.processing" -> {
                transaction.setStatus(PaymentStatus.PROCESSING);
            }
        }
        
        paymentRepository.save(transaction);
        
        log.info("Updated transaction {} status from {} to {} via Stripe webhook", 
                transaction.getId(), oldStatus, newStatus);
    }
    
    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(PaymentTransaction transaction) {
        try {
            // Activate subscription if applicable
            if (transaction.getSubscriptionId() != null) {
                subscriptionService.activateSubscriptionFromPayment(transaction);
            }
            
            // Publish success event
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getSubscriptionId()
            ));
            
        } catch (Exception e) {
            log.error("Failed to handle payment success for transaction: {}", transaction.getId(), e);
        }
    }
    
    /**
     * Handle payment failure
     */
    private void handlePaymentFailure(PaymentTransaction transaction, String reason) {
        // Publish failure event
        eventPublisher.publishEvent(new PaymentFailedEvent(
            transaction.getUserId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            reason
        ));
    }
    
    /**
     * Map Razorpay status to internal payment status
     */
    private PaymentStatus mapRazorpayStatusToPaymentStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "created" -> PaymentStatus.PENDING;
            case "authorized" -> PaymentStatus.PROCESSING;
            case "captured" -> PaymentStatus.COMPLETED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "failed" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
    
    /**
     * Map Stripe status to internal payment status
     */
    private PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "requires_payment_method", "requires_confirmation" -> PaymentStatus.PENDING;
            case "processing" -> PaymentStatus.PROCESSING;
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "canceled" -> PaymentStatus.CANCELLED;
            case "requires_action" -> PaymentStatus.PENDING;
            default -> PaymentStatus.PENDING;
        };
    }
}