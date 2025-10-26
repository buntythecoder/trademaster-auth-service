package com.trademaster.payment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.common.functional.Result;
import com.trademaster.payment.domain.WebhookEvent;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.service.WebhookProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive MockMvc Tests for WebhookController
 *
 * Coverage:
 * - All 5 webhook endpoints
 * - Signature verification scenarios
 * - Railway programming response mapping
 * - Pattern matching for gateway routing
 * - Async CompletableFuture handling
 * - Public endpoints (no authentication)
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else in test logic
 * - Rule 11: Railway programming with Result.fold()
 * - Rule 14: Pattern matching for gateway selection
 * - Rule 20: >80% coverage target
 */
@WebMvcTest(WebhookController.class)
@DisplayName("WebhookController MockMvc Tests")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookProcessingService webhookProcessingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String testPayload;
    private String testSignature;
    private WebhookEvent testEvent;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize test data
        testPayload = """
            {
                "event": "payment.captured",
                "payload": {
                    "payment": {
                        "entity": {
                            "id": "pay_test123",
                            "amount": 100000,
                            "currency": "INR",
                            "status": "captured"
                        }
                    }
                }
            }
            """;

        testSignature = "test_signature_abc123xyz789";

        // Build test webhook event using Unknown sealed record
        Map<String, Object> payloadMap = objectMapper.readValue(
            testPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        testEvent = new WebhookEvent.Unknown(
            PaymentGateway.RAZORPAY,
            "payment.captured",
            payloadMap
        );
    }

    // ==================== POST /api/v1/webhooks/razorpay Tests ====================

    @Test
    @DisplayName("handleRazorpayWebhook - Valid webhook should return 200")
    void handleRazorpayWebhook_WithValidSignature_ShouldReturn200() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(testEvent)));

        // Act & Assert - Railway programming success path
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.message").value("Webhook processed successfully"))
            .andExpect(jsonPath("$.gateway").value("RAZORPAY"))
            .andExpect(jsonPath("$.eventType").value("payment.captured"));
    }

    @Test
    @DisplayName("handleRazorpayWebhook - Invalid signature should return 400")
    void handleRazorpayWebhook_WithInvalidSignature_ShouldReturn400() throws Exception {
        // Arrange - Railway programming error path
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(
            Result.failure("Invalid signature")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", "invalid_signature")
                .content(testPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Invalid signature"));
    }

    @Test
    @DisplayName("handleRazorpayWebhook - Missing signature header should return 400")
    void handleRazorpayWebhook_WithMissingSignature_ShouldReturn400() throws Exception {
        // Act & Assert - Missing required header
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("handleRazorpayWebhook - Various event types should be processed")
    void handleRazorpayWebhook_WithVariousEventTypes_ShouldProcess() throws Exception {
        // Test payment.failed event
        Map<String, Object> payloadMap = objectMapper.readValue(
            testPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent failedEvent = new WebhookEvent.Unknown(
            PaymentGateway.RAZORPAY,
            "payment.failed",
            payloadMap
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(failedEvent)));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventType").value("payment.failed"));
    }

    // ==================== POST /api/v1/webhooks/stripe Tests ====================

    @Test
    @DisplayName("handleStripeWebhook - Valid webhook should return 200")
    void handleStripeWebhook_WithValidSignature_ShouldReturn200() throws Exception {
        // Arrange
        String stripePayload = """
            {
                "type": "payment_intent.succeeded",
                "data": {
                    "object": {
                        "id": "pi_test123",
                        "amount": 100000,
                        "currency": "usd",
                        "status": "succeeded"
                    }
                }
            }
            """;

        Map<String, Object> stripePayloadMap = objectMapper.readValue(
            stripePayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent stripeEvent = new WebhookEvent.Unknown(
            PaymentGateway.STRIPE,
            "payment_intent.succeeded",
            stripePayloadMap
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.STRIPE),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(stripeEvent)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", testSignature)
                .content(stripePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.gateway").value("STRIPE"))
            .andExpect(jsonPath("$.eventType").value("payment_intent.succeeded"));
    }

    @Test
    @DisplayName("handleStripeWebhook - Invalid signature should return 400")
    void handleStripeWebhook_WithInvalidSignature_ShouldReturn400() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.STRIPE),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(
            Result.failure("Invalid Stripe signature")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "invalid_stripe_sig")
                .content(testPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid Stripe signature"));
    }

    @Test
    @DisplayName("handleStripeWebhook - Missing signature header should return 400")
    void handleStripeWebhook_WithMissingSignature_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("handleStripeWebhook - Charge events should be processed")
    void handleStripeWebhook_WithChargeEvents_ShouldProcess() throws Exception {
        // Test charge.succeeded event
        Map<String, Object> chargePayloadMap = objectMapper.readValue(
            testPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent chargeEvent = new WebhookEvent.Unknown(
            PaymentGateway.STRIPE,
            "charge.succeeded",
            chargePayloadMap
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.STRIPE),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(chargeEvent)));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventType").value("charge.succeeded"));
    }

    // ==================== POST /api/v1/webhooks/upi Tests ====================

    @Test
    @DisplayName("handleUpiWebhook - Valid UPI webhook should return 200")
    void handleUpiWebhook_WithValidSignature_ShouldReturn200() throws Exception {
        // Arrange
        String upiPayload = """
            {
                "event": "upi.payment.received",
                "payload": {
                    "upi_transaction": {
                        "id": "upi_test123",
                        "amount": 50000,
                        "currency": "INR",
                        "status": "completed"
                    }
                }
            }
            """;

        Map<String, Object> upiPayloadMap = objectMapper.readValue(
            upiPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent upiEvent = new WebhookEvent.Unknown(
            PaymentGateway.UPI,
            "upi.payment.received",
            upiPayloadMap
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.UPI),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(upiEvent)));

        // Act & Assert - UPI routes through Razorpay
        mockMvc.perform(post("/api/v1/webhooks/upi")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(upiPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.gateway").value("UPI"))
            .andExpect(jsonPath("$.eventType").value("upi.payment.received"));
    }

    @Test
    @DisplayName("handleUpiWebhook - Invalid signature should return 400")
    void handleUpiWebhook_WithInvalidSignature_ShouldReturn400() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.UPI),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(
            Result.failure("Invalid UPI signature")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/upi")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", "invalid_upi_sig")
                .content(testPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid UPI signature"));
    }

    // ==================== POST /api/v1/webhooks/{gateway} Tests ====================

    @Test
    @DisplayName("handleGenericWebhook - Razorpay gateway should route correctly")
    void handleGenericWebhook_WithRazorpayGateway_ShouldRoute() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(testEvent)));

        // Act & Assert - Pattern matching gateway routing
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gateway").value("RAZORPAY"));
    }

    @Test
    @DisplayName("handleGenericWebhook - Stripe gateway should route correctly")
    void handleGenericWebhook_WithStripeGateway_ShouldRoute() throws Exception {
        // Arrange
        Map<String, Object> stripePayloadMap2 = objectMapper.readValue(
            testPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent stripeEvent = new WebhookEvent.Unknown(
            PaymentGateway.STRIPE,
            "payment.succeeded",
            stripePayloadMap2
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.STRIPE),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(stripeEvent)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gateway").value("STRIPE"));
    }

    @Test
    @DisplayName("handleGenericWebhook - UPI gateway should route correctly")
    void handleGenericWebhook_WithUpiGateway_ShouldRoute() throws Exception {
        // Arrange
        Map<String, Object> upiPayloadMap2 = objectMapper.readValue(
            testPayload,
            new TypeReference<Map<String, Object>>() {}
        );
        WebhookEvent upiEvent = new WebhookEvent.Unknown(
            PaymentGateway.UPI,
            "upi.payment.received",
            upiPayloadMap2
        );

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.UPI),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(upiEvent)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/upi")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gateway").value("UPI"));
    }

    @Test
    @DisplayName("handleGenericWebhook - Unsupported gateway should return 400")
    void handleGenericWebhook_WithUnsupportedGateway_ShouldReturn400() throws Exception {
        // Act & Assert - Pattern matching returns error for unsupported gateway
        mockMvc.perform(post("/api/v1/webhooks/{gateway}", "unsupported_gateway")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Test-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Unsupported gateway")));
    }

    @Test
    @DisplayName("handleGenericWebhook - Case insensitive gateway names should work")
    void handleGenericWebhook_WithCaseInsensitiveNames_ShouldWork() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(testEvent)));

        // Act & Assert - Pattern matching handles case conversion
        mockMvc.perform(post("/api/v1/webhooks/{gateway}", "RAZORPAY")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/webhooks/{gateway}", "RaZoRpAy")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk());
    }

    // ==================== GET /api/v1/webhooks/health Tests ====================

    @Test
    @DisplayName("webhookHealthCheck - Should return UP status")
    void webhookHealthCheck_ShouldReturnUpStatus() throws Exception {
        // Act & Assert - Public health check endpoint
        mockMvc.perform(get("/api/v1/webhooks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").value("Webhook endpoints are operational"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("webhookHealthCheck - Multiple calls should always return UP")
    void webhookHealthCheck_MultipleCalls_ShouldAlwaysReturnUp() throws Exception {
        // Act & Assert - Idempotent health check
        mockMvc.perform(get("/api/v1/webhooks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/v1/webhooks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/v1/webhooks/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ==================== Security & Edge Cases ====================

    @Test
    @DisplayName("All webhook endpoints - Public access without authentication")
    void allWebhookEndpoints_ShouldBePublic() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            any(PaymentGateway.class),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(testEvent)));

        // Act & Assert - All webhook endpoints should work without authentication
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/webhooks/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Webhook endpoints - Empty payload should be processed")
    void webhookEndpoints_WithEmptyPayload_ShouldProcess() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(
            Result.failure("Empty payload not allowed")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Webhook endpoints - Malformed JSON should return 400")
    void webhookEndpoints_WithMalformedJson_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content("invalid json {]"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Webhook responses - All should include timestamp")
    void webhookResponses_ShouldIncludeTimestamp() throws Exception {
        // Arrange
        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(CompletableFuture.completedFuture(Result.success(testEvent)));

        // Act & Assert - All webhook responses have timestamps
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Async webhook processing - CompletableFuture should complete")
    void asyncWebhookProcessing_ShouldComplete() throws Exception {
        // Arrange - Simulate async processing with delay
        CompletableFuture<Result<WebhookEvent, String>> delayedFuture =
            CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100); // Simulate processing delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Result.success(testEvent);
            });

        when(webhookProcessingService.processWebhook(
            eq(PaymentGateway.RAZORPAY),
            anyString(),
            anyString(),
            anyMap()
        )).thenReturn(delayedFuture);

        // Act & Assert - Virtual threads handle async processing
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", testSignature)
                .content(testPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"));
    }
}
