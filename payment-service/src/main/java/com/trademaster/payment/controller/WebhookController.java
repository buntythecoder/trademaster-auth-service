package com.trademaster.payment.controller;

import com.trademaster.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Webhook Controller
 * 
 * Handles payment gateway webhooks for real-time payment status updates.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Webhook endpoints for payment gateway notifications")
public class WebhookController {

    private final WebhookService webhookService;
    
    /**
     * Handle Razorpay webhooks
     */
    @PostMapping("/razorpay")
    @Operation(
        summary = "Razorpay Webhook",
        description = "Handle Razorpay payment notifications"
    )
    public ResponseEntity<String> handleRazorpayWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @Parameter(description = "Razorpay signature header") 
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        log.info("Received Razorpay webhook with signature: {}", 
                signature.substring(0, Math.min(signature.length(), 10)) + "...");
        
        try {
            webhookService.processRazorpayWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook", e);
            return ResponseEntity.badRequest().body("Webhook processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle Stripe webhooks
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Stripe Webhook",
        description = "Handle Stripe payment notifications"
    )
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @Parameter(description = "Stripe signature header")
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Received Stripe webhook with signature: {}", 
                signature.substring(0, Math.min(signature.length(), 10)) + "...");
        
        try {
            webhookService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook", e);
            return ResponseEntity.badRequest().body("Webhook processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle UPI webhooks (through Razorpay)
     */
    @PostMapping("/upi")
    @Operation(
        summary = "UPI Webhook", 
        description = "Handle UPI payment notifications"
    )
    public ResponseEntity<String> handleUpiWebhook(
            HttpServletRequest request,
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        log.info("Received UPI webhook");
        
        try {
            webhookService.processUpiWebhook(payload, signature);
            return ResponseEntity.ok("UPI webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Failed to process UPI webhook", e);
            return ResponseEntity.badRequest().body("UPI webhook processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Generic webhook health check
     */
    @GetMapping("/health")
    @Operation(
        summary = "Webhook Health Check",
        description = "Check webhook endpoint health"
    )
    public ResponseEntity<String> webhookHealthCheck() {
        return ResponseEntity.ok("Webhook endpoints are operational");
    }
}