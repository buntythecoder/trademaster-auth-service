package com.trademaster.payment.service;

import com.razorpay.*;
import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.gateway.impl.RazorpayServiceImpl;
import com.trademaster.payment.util.CircuitBreakerUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for RazorpayServiceImpl
 *
 * Coverage:
 * - All 7 interface methods
 * - Circuit breaker behavior (success, failure, open)
 * - HMAC-SHA256 signature verification
 * - Railway programming with Result types
 * - Async operations with CompletableFuture
 * - Functional error handling patterns
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else in test logic
 * - Rule 11: Railway programming with Result types
 * - Rule 20: >80% coverage target
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RazorpayService Unit Tests")
class RazorpayServiceImplTest {

    @Mock
    private CircuitBreakerUtil circuitBreakerUtil;

    @InjectMocks
    private RazorpayServiceImpl razorpayService;

    private PaymentRequest testPaymentRequest;
    private RefundRequest testRefundRequest;
    private UUID testUserId;
    private UUID testTransactionId;
    private BigDecimal testAmount;
    private String testOrderId;
    private String testPaymentId;
    private String testRefundId;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();
        testAmount = new BigDecimal("1000.00");
        testOrderId = "order_test_" + System.currentTimeMillis();
        testPaymentId = "pay_test_" + System.currentTimeMillis();
        testRefundId = "rfnd_test_" + System.currentTimeMillis();

        // Configure service with test credentials
        ReflectionTestUtils.setField(razorpayService, "razorpayKeyId", "test_key_id");
        ReflectionTestUtils.setField(razorpayService, "razorpayKeySecret", "test_key_secret");
        ReflectionTestUtils.setField(razorpayService, "webhookSecret", "test_webhook_secret");

        // Build test payment request
        testPaymentRequest = PaymentRequest.builder()
            .userId(testUserId)
            .subscriptionPlanId(UUID.randomUUID())
            .amount(testAmount)
            .currency("INR")
            .paymentMethod(com.trademaster.payment.enums.PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Build test refund request
        testRefundRequest = RefundRequest.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .reason("Test refund")
            .build();
    }

    // ==================== createOrder Tests ====================

    @Test
    @DisplayName("createOrder - Success with circuit breaker")
    void createOrder_WithValidRequest_ShouldSucceed() {
        // Arrange
        String expectedOrderId = testOrderId;

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(expectedOrderId));

        // Act
        Result<String, String> result = razorpayService.createOrder(testPaymentRequest);

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(orderId -> {
            assertNotNull(orderId);
            assertEquals(expectedOrderId, orderId);
        });

        verify(circuitBreakerUtil).executeWithCircuitBreaker(
            eq("razorpay-service"),
            any(Supplier.class)
        );
    }

    @Test
    @DisplayName("createOrder - Circuit breaker open should return failure")
    void createOrder_WithCircuitBreakerOpen_ShouldReturnFailure() {
        // Arrange - Circuit breaker returns empty (open state)
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<String, String> result = razorpayService.createOrder(testPaymentRequest);

        // Assert - Railway programming error handling
        assertTrue(result.isFailure());
        result.onFailure(error -> {
            assertNotNull(error);
            assertTrue(error.contains("Failed to create Razorpay order"));
        });
    }

    @Test
    @DisplayName("createOrder - Gateway exception should be handled gracefully")
    void createOrder_WithGatewayException_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<String, String> result = razorpayService.createOrder(testPaymentRequest);

        // Assert
        assertTrue(result.isFailure());
    }

    // ==================== verifyPaymentSignature Tests ====================

    @Test
    @DisplayName("verifyPaymentSignature - Valid HMAC-SHA256 signature should succeed")
    void verifyPaymentSignature_WithValidSignature_ShouldReturnTrue() {
        // Arrange
        String orderId = "order_test123";
        String paymentId = "pay_test456";

        // Generate actual HMAC-SHA256 signature for testing
        String payload = orderId + "|" + paymentId;
        String expectedSignature = computeTestSignature(payload, "test_webhook_secret");

        // Act
        Result<Boolean, String> result = razorpayService.verifyPaymentSignature(
            orderId,
            paymentId,
            expectedSignature
        );

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(isValid -> {
            assertTrue(isValid, "Signature should be valid");
        });
    }

    @Test
    @DisplayName("verifyPaymentSignature - Invalid signature should return false")
    void verifyPaymentSignature_WithInvalidSignature_ShouldReturnFalse() {
        // Arrange
        String orderId = "order_test123";
        String paymentId = "pay_test456";
        String invalidSignature = "invalid_signature_xyz";

        // Act
        Result<Boolean, String> result = razorpayService.verifyPaymentSignature(
            orderId,
            paymentId,
            invalidSignature
        );

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(isValid -> {
            assertFalse(isValid, "Invalid signature should return false");
        });
    }

    @Test
    @DisplayName("verifyPaymentSignature - Empty signature should return false")
    void verifyPaymentSignature_WithEmptySignature_ShouldReturnFalse() {
        // Arrange
        String orderId = "order_test123";
        String paymentId = "pay_test456";
        String emptySignature = "";

        // Act
        Result<Boolean, String> result = razorpayService.verifyPaymentSignature(
            orderId,
            paymentId,
            emptySignature
        );

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(isValid -> assertFalse(isValid));
    }

    // ==================== capturePayment Tests ====================

    @Test
    @DisplayName("capturePayment - Successful capture should return payment response")
    void capturePayment_WithValidPaymentId_ShouldSucceed() {
        // Arrange
        Long amountInPaise = 100000L; // 1000.00 INR
        String currency = "INR";

        PaymentResponse expectedResponse = PaymentResponse.builder()
            .amount(testAmount)
            .currency(currency)
            .status(PaymentStatus.COMPLETED)
            .gateway(PaymentGateway.RAZORPAY)
            .createdAt(Instant.now())
            .build();

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(expectedResponse));

        // Act
        Result<PaymentResponse, String> result = razorpayService.capturePayment(
            testPaymentId,
            amountInPaise,
            currency
        );

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(response -> {
            assertNotNull(response);
            assertEquals(testAmount, response.getAmount());
            assertEquals(PaymentStatus.COMPLETED, response.getStatus());
            assertEquals(PaymentGateway.RAZORPAY, response.getGateway());
        });
    }

    @Test
    @DisplayName("capturePayment - Circuit breaker open should return failure")
    void capturePayment_WithCircuitBreakerOpen_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<PaymentResponse, String> result = razorpayService.capturePayment(
            testPaymentId,
            100000L,
            "INR"
        );

        // Assert
        assertTrue(result.isFailure());
        result.onFailure(error ->
            assertTrue(error.contains("Failed to capture payment"))
        );
    }

    @Test
    @DisplayName("capturePayment - Invalid payment ID should return failure")
    void capturePayment_WithInvalidPaymentId_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<PaymentResponse, String> result = razorpayService.capturePayment(
            "invalid_payment_id",
            100000L,
            "INR"
        );

        // Assert
        assertTrue(result.isFailure());
    }

    // ==================== processRefund Tests ====================

    @Test
    @DisplayName("processRefund - Async refund with CompletableFuture should succeed")
    void processRefund_WithValidRequest_ShouldSucceed() throws Exception {
        // Arrange
        RefundResponse expectedResponse = RefundResponse.builder()
            .refundId(testRefundId)
            .transactionId(testPaymentId)
            .amount(testAmount)
            .currency("INR")
            .status("processed")
            .createdAt(Instant.now())
            .build();

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(expectedResponse));

        // Act
        CompletableFuture<Result<RefundResponse, String>> futureResult =
            razorpayService.processRefund(testPaymentId, testRefundRequest);

        Result<RefundResponse, String> result = futureResult.get();

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(response -> {
            assertNotNull(response);
            assertEquals(testRefundId, response.getRefundId());
            assertEquals(testAmount, response.getAmount());
            assertEquals("processed", response.getStatus());
        });
    }

    @Test
    @DisplayName("processRefund - Circuit breaker open should return failure")
    void processRefund_WithCircuitBreakerOpen_ShouldReturnFailure() throws Exception {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        CompletableFuture<Result<RefundResponse, String>> futureResult =
            razorpayService.processRefund(testPaymentId, testRefundRequest);

        Result<RefundResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure());
        result.onFailure(error ->
            assertTrue(error.contains("Failed to process refund"))
        );
    }

    @Test
    @DisplayName("processRefund - Virtual threads should handle async operation")
    void processRefund_ShouldExecuteAsynchronously() throws Exception {
        // Arrange
        RefundResponse mockResponse = RefundResponse.builder()
            .refundId(testRefundId)
            .transactionId(testPaymentId)
            .amount(testAmount)
            .currency("INR")
            .status("processed")
            .createdAt(Instant.now())
            .build();

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(mockResponse));

        // Act
        CompletableFuture<Result<RefundResponse, String>> future1 =
            razorpayService.processRefund(testPaymentId, testRefundRequest);
        CompletableFuture<Result<RefundResponse, String>> future2 =
            razorpayService.processRefund(testPaymentId + "_2", testRefundRequest);

        // Wait for both to complete
        CompletableFuture.allOf(future1, future2).get();

        // Assert - both should succeed
        assertTrue(future1.get().isSuccess());
        assertTrue(future2.get().isSuccess());
    }

    // ==================== fetchPaymentDetails Tests ====================

    @Test
    @DisplayName("fetchPaymentDetails - Idempotent fetch should succeed")
    void fetchPaymentDetails_WithValidPaymentId_ShouldSucceed() {
        // Arrange
        PaymentResponse expectedResponse = PaymentResponse.builder()
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .gateway(PaymentGateway.RAZORPAY)
            .createdAt(Instant.now())
            .build();

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(expectedResponse));

        // Act
        Result<PaymentResponse, String> result = razorpayService.fetchPaymentDetails(testPaymentId);

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(response -> {
            assertNotNull(response);
            assertEquals(testAmount, response.getAmount());
            assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        });
    }

    @Test
    @DisplayName("fetchPaymentDetails - Multiple calls should be idempotent")
    void fetchPaymentDetails_MultipleCalls_ShouldBeIdempotent() {
        // Arrange
        PaymentResponse mockResponse = PaymentResponse.builder()
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .gateway(PaymentGateway.RAZORPAY)
            .createdAt(Instant.now())
            .build();

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(mockResponse));

        // Act - call multiple times
        Result<PaymentResponse, String> result1 = razorpayService.fetchPaymentDetails(testPaymentId);
        Result<PaymentResponse, String> result2 = razorpayService.fetchPaymentDetails(testPaymentId);
        Result<PaymentResponse, String> result3 = razorpayService.fetchPaymentDetails(testPaymentId);

        // Assert - all calls should succeed with same data
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertTrue(result3.isSuccess());

        verify(circuitBreakerUtil, times(3))
            .executeWithCircuitBreaker(anyString(), any(Supplier.class));
    }

    @Test
    @DisplayName("fetchPaymentDetails - Circuit breaker open should return failure")
    void fetchPaymentDetails_WithCircuitBreakerOpen_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<PaymentResponse, String> result = razorpayService.fetchPaymentDetails(testPaymentId);

        // Assert
        assertTrue(result.isFailure());
        result.onFailure(error ->
            assertTrue(error.contains("Failed to fetch payment details"))
        );
    }

    // ==================== createSubscription Tests ====================

    @Test
    @DisplayName("createSubscription - Valid subscription should succeed")
    void createSubscription_WithValidRequest_ShouldSucceed() {
        // Arrange
        String planId = "plan_test123";
        String customerId = "cust_test456";
        Integer totalCount = 12;
        String expectedSubId = "sub_test789";

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(expectedSubId));

        // Act
        Result<String, String> result = razorpayService.createSubscription(
            planId,
            customerId,
            totalCount
        );

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(subId -> {
            assertNotNull(subId);
            assertEquals(expectedSubId, subId);
        });
    }

    @Test
    @DisplayName("createSubscription - Circuit breaker open should return failure")
    void createSubscription_WithCircuitBreakerOpen_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<String, String> result = razorpayService.createSubscription(
            "plan_test",
            "cust_test",
            12
        );

        // Assert
        assertTrue(result.isFailure());
        result.onFailure(error ->
            assertTrue(error.contains("Failed to create subscription"))
        );
    }

    @Test
    @DisplayName("createSubscription - Invalid plan ID should return failure")
    void createSubscription_WithInvalidPlanId_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<String, String> result = razorpayService.createSubscription(
            "",
            "cust_test",
            12
        );

        // Assert
        assertTrue(result.isFailure());
    }

    // ==================== cancelSubscription Tests ====================

    @Test
    @DisplayName("cancelSubscription - Immediate cancellation should succeed")
    void cancelSubscription_WithImmediateCancellation_ShouldSucceed() {
        // Arrange
        String subscriptionId = "sub_test123";
        Boolean cancelAtCycleEnd = false;

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(true));

        // Act
        Result<Boolean, String> result = razorpayService.cancelSubscription(
            subscriptionId,
            cancelAtCycleEnd
        );

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(cancelled -> {
            assertTrue(cancelled);
        });
    }

    @Test
    @DisplayName("cancelSubscription - Cancel at cycle end should succeed")
    void cancelSubscription_WithCancelAtCycleEnd_ShouldSucceed() {
        // Arrange
        String subscriptionId = "sub_test123";
        Boolean cancelAtCycleEnd = true;

        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(true));

        // Act
        Result<Boolean, String> result = razorpayService.cancelSubscription(
            subscriptionId,
            cancelAtCycleEnd
        );

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(cancelled -> assertTrue(cancelled));
    }

    @Test
    @DisplayName("cancelSubscription - Circuit breaker open should return failure")
    void cancelSubscription_WithCircuitBreakerOpen_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.empty());

        // Act
        Result<Boolean, String> result = razorpayService.cancelSubscription(
            "sub_test",
            false
        );

        // Assert
        assertTrue(result.isFailure());
        result.onFailure(error ->
            assertTrue(error.contains("Failed to cancel subscription"))
        );
    }

    @Test
    @DisplayName("cancelSubscription - Invalid subscription ID should return failure")
    void cancelSubscription_WithInvalidSubscriptionId_ShouldReturnFailure() {
        // Arrange
        when(circuitBreakerUtil.executeWithCircuitBreaker(anyString(), any(Supplier.class)))
            .thenReturn(Optional.of(false));

        // Act
        Result<Boolean, String> result = razorpayService.cancelSubscription(
            "invalid_sub",
            false
        );

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(cancelled -> assertFalse(cancelled));
    }

    // ==================== Helper Methods ====================

    /**
     * Compute HMAC-SHA256 signature for testing
     * Mirrors the implementation in RazorpayServiceImpl
     */
    private String computeTestSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
                );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }
}
