package com.trademaster.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.RefundCollection;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.RefundListParams;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.exception.PaymentProcessingException;
import com.trademaster.payment.exception.RefundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stripe Payment Service
 * 
 * Handles Stripe payment gateway integration for international payments.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;
    
    /**
     * Process payment through Stripe
     */
    public PaymentResponse processPayment(PaymentRequest request, PaymentTransaction transaction) {
        try {
            log.info("Processing Stripe payment for transaction: {}", transaction.getId());
            
            // Create payment intent
            PaymentIntent paymentIntent = createPaymentIntent(request, transaction);
            
            // Build gateway response
            PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
                    .paymentId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .build();
            
            // Update transaction
            transaction.setGatewayPaymentId(paymentIntent.getId());
            transaction.setStatus(PaymentStatus.PENDING);
            
            return PaymentResponse.pending(
                transaction.getId(),
                request.getAmount(),
                request.getCurrency(),
                gatewayResponse
            );
            
        } catch (Exception e) {
            log.error("Stripe payment creation failed for transaction: {}", transaction.getId(), e);
            throw new PaymentProcessingException("Failed to create Stripe payment: " + e.getMessage());
        }
    }
    
    /**
     * Verify Stripe webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Webhook.constructEvent(payload, signature, webhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Create Stripe payment intent
     */
    private PaymentIntent createPaymentIntent(PaymentRequest request, PaymentTransaction transaction) {
        try {
            // Convert amount to cents
            long amountInCents = request.getAmount().multiply(new BigDecimal("100")).longValue();
            
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("transaction_id", transaction.getId().toString());
            metadata.put("user_id", request.getUserId().toString());
            metadata.put("subscription_plan_id", request.getSubscriptionPlanId().toString());
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .putAllMetadata(metadata)
                    .setDescription("TradeMaster Subscription")
                    .build();
            
            return PaymentIntent.create(params);
            
        } catch (Exception e) {
            throw new PaymentProcessingException("Failed to create Stripe payment intent", e);
        }
    }
    
    /**
     * Process refund
     */
    public RefundResponse processRefund(String paymentIntentId, BigDecimal refundAmount, RefundRequest refundRequest) {
        try {
            long amountInCents = refundAmount.multiply(new BigDecimal("100")).longValue();
            
            Map<String, String> metadata = new HashMap<>();
            if (refundRequest.getReason() != null) {
                metadata.put("reason", refundRequest.getReason());
            }
            
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amountInCents)
                    .putAllMetadata(metadata)
                    .build();
            
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(params);
            
            return RefundResponse.builder()
                    .refundId(refund.getId())
                    .transactionId(paymentIntentId)
                    .amount(refundAmount)
                    .currency(refund.getCurrency().toUpperCase())
                    .status(refund.getStatus())
                    .createdAt(Instant.ofEpochSecond(refund.getCreated()))
                    .gatewayResponse(refund.toJson())
                    .build();
            
        } catch (Exception e) {
            log.error("Stripe refund failed for payment: {}", paymentIntentId, e);
            throw new RefundException("Failed to process refund with Stripe: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get refund status
     */
    public RefundResponse getRefundStatus(String refundId) {
        try {
            com.stripe.model.Refund refund = com.stripe.model.Refund.retrieve(refundId);
            
            return RefundResponse.builder()
                    .refundId(refund.getId())
                    .transactionId(refund.getPaymentIntent())
                    .amount(new BigDecimal(refund.getAmount()).divide(new BigDecimal("100")))
                    .currency(refund.getCurrency().toUpperCase())
                    .status(refund.getStatus())
                    .createdAt(Instant.ofEpochSecond(refund.getCreated()))
                    .gatewayResponse(refund.toJson())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to get refund status from Stripe for refund: {}", refundId, e);
            throw new RefundException("Failed to get refund status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all refunds for a transaction
     */
    public List<RefundResponse> getTransactionRefunds(String paymentIntentId) {
        try {
            RefundListParams params = RefundListParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();
            
            RefundCollection refunds = com.stripe.model.Refund.list(params);
            
            return refunds.getData().stream()
                    .map(refund -> RefundResponse.builder()
                            .refundId(refund.getId())
                            .transactionId(refund.getPaymentIntent())
                            .amount(new BigDecimal(refund.getAmount()).divide(new BigDecimal("100")))
                            .currency(refund.getCurrency().toUpperCase())
                            .status(refund.getStatus())
                            .createdAt(Instant.ofEpochSecond(refund.getCreated()))
                            .gatewayResponse(refund.toJson())
                            .build())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to get refunds from Stripe for payment: {}", paymentIntentId, e);
            throw new RefundException("Failed to get refunds: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel refund (if supported and refund is pending)
     */
    public boolean cancelRefund(String refundId) {
        try {
            com.stripe.model.Refund refund = com.stripe.model.Refund.retrieve(refundId);
            
            if ("pending".equals(refund.getStatus())) {
                refund.cancel();
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Failed to cancel refund with Stripe: {}", refundId, e);
            return false;
        }
    }
}