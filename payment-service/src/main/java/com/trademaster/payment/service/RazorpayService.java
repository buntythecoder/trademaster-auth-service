package com.trademaster.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Razorpay Payment Service
 * 
 * Handles Razorpay payment gateway integration for Indian market.
 * Supports cards, UPI, net banking, and wallets.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    
    @Value("${payment.razorpay.webhook-secret}")
    private String webhookSecret;
    
    /**
     * Process payment through Razorpay
     */
    public PaymentResponse processPayment(PaymentRequest request, PaymentTransaction transaction) {
        try {
            log.info("Processing Razorpay payment for transaction: {}", transaction.getId());
            
            // Create Razorpay order
            Order order = createRazorpayOrder(request, transaction);
            
            // Build gateway response
            PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
                    .orderId(order.get("id"))
                    .razorpayOrderId(order.get("id"))
                    .build();
            
            // Update transaction with order details
            transaction.setGatewayOrderId(order.get("id"));
            transaction.setStatus(PaymentStatus.PENDING);
            
            return PaymentResponse.pending(
                transaction.getId(),
                request.getAmount(),
                request.getCurrency(),
                gatewayResponse
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for transaction: {}", transaction.getId(), e);
            throw new PaymentProcessingException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    /**
     * Process UPI payment through Razorpay
     */
    public PaymentResponse processUpiPayment(PaymentRequest request, PaymentTransaction transaction) {
        try {
            log.info("Processing UPI payment through Razorpay for transaction: {}", transaction.getId());
            
            // Create UPI-specific order
            Order order = createUpiOrder(request, transaction);
            
            // Build gateway response with UPI-specific data
            PaymentResponse.GatewayResponse gatewayResponse = PaymentResponse.GatewayResponse.builder()
                    .orderId(order.get("id"))
                    .razorpayOrderId(order.get("id"))
                    .qrCodeData(generateUpiQrData(order, request))
                    .build();
            
            transaction.setGatewayOrderId(order.get("id"));
            transaction.setStatus(PaymentStatus.PENDING);
            
            return PaymentResponse.pending(
                transaction.getId(),
                request.getAmount(),
                request.getCurrency(),
                gatewayResponse
            );
            
        } catch (RazorpayException e) {
            log.error("UPI payment creation failed for transaction: {}", transaction.getId(), e);
            throw new PaymentProcessingException("Failed to create UPI payment: " + e.getMessage());
        }
    }
    
    /**
     * Verify Razorpay payment signature
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            
            return Utils.verifyPaymentSignature(attributes, webhookSecret);
            
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Create Razorpay order
     */
    private Order createRazorpayOrder(PaymentRequest request, PaymentTransaction transaction) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue()); // Convert to paise
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "TM_" + transaction.getId().toString().substring(0, 8));
        
        // Add payment method preferences
        JSONObject notes = new JSONObject();
        notes.put("user_id", request.getUserId().toString());
        notes.put("subscription_plan_id", request.getSubscriptionPlanId().toString());
        notes.put("payment_method", request.getPaymentMethod().name());
        orderRequest.put("notes", notes);
        
        // Add payment preferences
        if (request.getOptions() != null) {
            JSONObject paymentCapture = new JSONObject();
            paymentCapture.put("automatic", true);
            orderRequest.put("payment_capture", paymentCapture);
        }
        
        return razorpayClient.orders.create(orderRequest);
    }
    
    /**
     * Create UPI-specific order
     */
    private Order createUpiOrder(PaymentRequest request, PaymentTransaction transaction) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "UPI_" + transaction.getId().toString().substring(0, 8));
        orderRequest.put("method", "upi");
        
        // UPI-specific preferences
        JSONObject method = new JSONObject();
        method.put("upi", true);
        orderRequest.put("method", method);
        
        return razorpayClient.orders.create(orderRequest);
    }
    
    /**
     * Generate UPI QR code data
     */
    private String generateUpiQrData(Order order, PaymentRequest request) {
        // This would typically generate UPI QR code data
        // For now, return the order ID (in real implementation, generate proper UPI string)
        return String.format("upi://pay?pa=trademaster@razorpay&pn=TradeMaster&am=%s&cu=%s&tn=Order%%20%s",
                request.getAmount().toString(),
                request.getCurrency(),
                order.get("id")
        );
    }
    
    /**
     * Process refund
     */
    public RefundResponse processRefund(String paymentId, BigDecimal refundAmount, RefundRequest refundRequest) {
        try {
            JSONObject refundRequestJson = new JSONObject();
            refundRequestJson.put("amount", refundAmount.multiply(new BigDecimal("100")).intValue());
            
            if (refundRequest.getReason() != null) {
                refundRequestJson.put("notes", Map.of("reason", refundRequest.getReason()));
            }
            
            Refund refund = razorpayClient.payments.refund(paymentId, refundRequestJson);
            
            return RefundResponse.builder()
                    .refundId(refund.get("id"))
                    .transactionId(paymentId)
                    .amount(refundAmount)
                    .currency("INR")
                    .status(refund.get("status"))
                    .createdAt(Instant.now())
                    .gatewayResponse(refund.toString())
                    .build();
            
        } catch (Exception e) {
            log.error("Razorpay refund failed for payment: {}", paymentId, e);
            throw new RefundException("Failed to process refund with Razorpay: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get refund status
     */
    public RefundResponse getRefundStatus(String refundId) {
        try {
            Refund refund = razorpayClient.refunds.fetch(refundId);
            
            return RefundResponse.builder()
                    .refundId(refund.get("id"))
                    .transactionId(refund.get("payment_id"))
                    .amount(new BigDecimal(refund.get("amount").toString()).divide(new BigDecimal("100")))
                    .currency("INR")
                    .status(refund.get("status"))
                    .createdAt(Instant.ofEpochSecond((Integer) refund.get("created_at")))
                    .gatewayResponse(refund.toString())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to get refund status from Razorpay for refund: {}", refundId, e);
            throw new RefundException("Failed to get refund status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all refunds for a transaction
     */
    public List<RefundResponse> getTransactionRefunds(String paymentId) {
        try {
            List<Refund> refunds = razorpayClient.payments.fetchAllRefunds(paymentId);
            
            return refunds.stream()
                    .map(refund -> RefundResponse.builder()
                            .refundId(refund.get("id"))
                            .transactionId(refund.get("payment_id"))
                            .amount(new BigDecimal(refund.get("amount").toString()).divide(new BigDecimal("100")))
                            .currency("INR")
                            .status(refund.get("status"))
                            .createdAt(Instant.ofEpochSecond((Integer) refund.get("created_at")))
                            .gatewayResponse(refund.toString())
                            .build())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to get refunds from Razorpay for payment: {}", paymentId, e);
            throw new RefundException("Failed to get refunds: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel refund (if supported and refund is pending)
     */
    public boolean cancelRefund(String refundId) {
        // Razorpay doesn't support canceling refunds once initiated
        // This method exists for interface compliance
        log.warn("Refund cancellation not supported by Razorpay for refund: {}", refundId);
        return false;
    }
}