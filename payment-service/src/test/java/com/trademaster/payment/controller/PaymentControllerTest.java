package com.trademaster.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive MockMvc Tests for PaymentController
 *
 * Coverage:
 * - All 7 REST endpoints
 * - Success and error scenarios
 * - Railway programming response mapping
 * - Security (@PreAuthorize) validation
 * - Input validation (Jakarta Validation)
 * - Async CompletableFuture handling
 * - Correlation ID headers
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else in test logic
 * - Rule 6: Zero Trust Security - @WithMockUser for auth testing
 * - Rule 11: Railway programming with Result.fold()
 * - Rule 20: >80% coverage target
 */
@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController MockMvc Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentProcessingService paymentProcessingService;

    private PaymentRequest testPaymentRequest;
    private PaymentResponse testPaymentResponse;
    private RefundRequest testRefundRequest;
    private RefundResponse testRefundResponse;
    private PaymentTransaction testTransaction;
    private UUID testUserId;
    private UUID testTransactionId;
    private BigDecimal testAmount;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();
        testAmount = new BigDecimal("1000.00");

        // Build test payment request
        testPaymentRequest = PaymentRequest.builder()
            .userId(testUserId)
            .subscriptionPlanId(UUID.randomUUID())
            .amount(testAmount)
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Build test payment response
        testPaymentResponse = PaymentResponse.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .gateway(PaymentGateway.RAZORPAY)
            .createdAt(Instant.now())
            .build();

        // Build test refund request
        testRefundRequest = RefundRequest.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .reason("Customer request")
            .build();

        // Build test refund response
        testRefundResponse = RefundResponse.builder()
            .refundId("rfnd_test123")
            .transactionId(testTransactionId.toString())
            .amount(testAmount)
            .currency("INR")
            .status("processed")
            .createdAt(Instant.now())
            .build();

        // Build test transaction
        testTransaction = PaymentTransaction.builder()
            .id(testTransactionId)
            .userId(testUserId)
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .paymentMethod(PaymentMethod.CARD)
            .description("Test transaction")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ==================== POST /api/v1/payments/process Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("processPayment - Valid request should return 200 OK")
    void processPayment_WithValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentProcessingService.processPayment(any(PaymentRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testPaymentResponse)));

        // Act & Assert - Railway programming success path
        mockMvc.perform(post("/api/v1/payments/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentRequest)))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.transactionId").value(testTransactionId.toString()))
            .andExpect(jsonPath("$.amount").value(testAmount.doubleValue()))
            .andExpect(jsonPath("$.currency").value("INR"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("processPayment - Service failure should return 400 Bad Request")
    void processPayment_WithServiceFailure_ShouldReturn400() throws Exception {
        // Arrange - Railway programming error path
        when(paymentProcessingService.processPayment(any(PaymentRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure("Payment gateway unavailable")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.message").value("Payment gateway unavailable"))
            .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("processPayment - Invalid amount should return 400 validation error")
    void processPayment_WithInvalidAmount_ShouldReturn400() throws Exception {
        // Arrange - negative amount (validation failure)
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(testUserId)
            .amount(new BigDecimal("-100.00"))
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Act & Assert - validation should fail before service call
        mockMvc.perform(post("/api/v1/payments/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("processPayment - Without authentication should return 401")
    void processPayment_WithoutAuthentication_ShouldReturn401() throws Exception {
        // Act & Assert - Zero Trust security validation
        mockMvc.perform(post("/api/v1/payments/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentRequest)))
            .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/v1/payments/confirm/{transactionId} Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("confirmPayment - Valid request should return 200 OK")
    void confirmPayment_WithValidRequest_ShouldReturn200() throws Exception {
        // Arrange
        String paymentMethodId = "pm_test123";
        when(paymentProcessingService.confirmPayment(any(UUID.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testPaymentResponse)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/confirm/{transactionId}", testTransactionId)
                .with(csrf())
                .param("paymentMethodId", paymentMethodId))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("confirmPayment - Transaction not found should return 400")
    void confirmPayment_WithNotFoundTransaction_ShouldReturn400() throws Exception {
        // Arrange - Railway programming error path
        String paymentMethodId = "pm_test123";
        when(paymentProcessingService.confirmPayment(any(UUID.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure("Transaction not found")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/confirm/{transactionId}", testTransactionId)
                .with(csrf())
                .param("paymentMethodId", paymentMethodId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    @DisplayName("confirmPayment - Without authentication should return 401")
    void confirmPayment_WithoutAuthentication_ShouldReturn401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/confirm/{transactionId}", testTransactionId)
                .with(csrf())
                .param("paymentMethodId", "pm_test"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== POST /api/v1/payments/refund Tests ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("processRefund - Valid request with ADMIN role should return 200")
    void processRefund_WithAdminRole_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentProcessingService.processRefund(any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testRefundResponse)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refund")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRefundRequest)))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.refundId").value("rfnd_test123"))
            .andExpect(jsonPath("$.status").value("processed"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("processRefund - Valid request with USER role should return 200")
    void processRefund_WithUserRole_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentProcessingService.processRefund(any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testRefundResponse)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refund")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRefundRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("processRefund - Service failure should return 400")
    void processRefund_WithServiceFailure_ShouldReturn400() throws Exception {
        // Arrange - Railway programming error path
        when(paymentProcessingService.processRefund(any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure("Refund not allowed for this transaction")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/refund")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRefundRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Refund not allowed")));
    }

    // ==================== GET /api/v1/payments/transaction/{transactionId} Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getTransaction - Existing transaction should return 200")
    void getTransaction_WithExistingTransaction_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentProcessingService.getTransaction(any(UUID.class)))
            .thenReturn(Result.success(testTransaction));

        // Act & Assert - Railway programming success path
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}", testTransactionId))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.amount").value(testAmount.doubleValue()))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getTransaction - Non-existing transaction should return 404")
    void getTransaction_WithNonExistingTransaction_ShouldReturn404() throws Exception {
        // Arrange - Railway programming error path
        when(paymentProcessingService.getTransaction(any(UUID.class)))
            .thenReturn(Result.failure("Transaction not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}", testTransactionId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Transaction not found"))
            .andExpect(jsonPath("$.statusCode").value(404));
    }

    // ==================== GET /api/v1/payments/transaction/{transactionId}/status Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getPaymentStatus - Existing transaction should return status with message")
    void getPaymentStatus_WithExistingTransaction_ShouldReturnStatus() throws Exception {
        // Arrange
        when(paymentProcessingService.getPaymentStatus(any(UUID.class)))
            .thenReturn(Result.success(PaymentStatus.COMPLETED));

        // Act & Assert - Pattern matching in getStatusMessage()
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}/status", testTransactionId))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.transactionId").value(testTransactionId.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.message").value("Payment completed successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getPaymentStatus - Various statuses should have correct messages")
    void getPaymentStatus_WithVariousStatuses_ShouldReturnCorrectMessages() throws Exception {
        // Test PENDING status
        when(paymentProcessingService.getPaymentStatus(any(UUID.class)))
            .thenReturn(Result.success(PaymentStatus.PENDING));

        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}/status", testTransactionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Payment is being processed"));

        // Test FAILED status
        when(paymentProcessingService.getPaymentStatus(any(UUID.class)))
            .thenReturn(Result.success(PaymentStatus.FAILED));

        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}/status", testTransactionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Payment failed"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getPaymentStatus - Non-existing transaction should return 404")
    void getPaymentStatus_WithNonExistingTransaction_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentProcessingService.getPaymentStatus(any(UUID.class)))
            .thenReturn(Result.failure("Transaction not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}/status", testTransactionId))
            .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/payments/user/{userId}/history Tests ====================

    @Test
    @WithMockUser(roles = "USER", username = "test-user")
    @DisplayName("getPaymentHistory - Valid request should return paginated results")
    void getPaymentHistory_WithValidRequest_ShouldReturnPaginatedResults() throws Exception {
        // Arrange
        List<PaymentTransaction> transactions = List.of(testTransaction);
        Page<PaymentTransaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 1);

        when(paymentProcessingService.getPaymentHistory(any(UUID.class), any()))
            .thenReturn(Result.success(page));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/user/{userId}/history", testUserId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].amount").value(testAmount.doubleValue()))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getPaymentHistory - Empty history should return empty page")
    void getPaymentHistory_WithEmptyHistory_ShouldReturnEmptyPage() throws Exception {
        // Arrange
        Page<PaymentTransaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(paymentProcessingService.getPaymentHistory(any(UUID.class), any()))
            .thenReturn(Result.success(emptyPage));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/user/{userId}/history", testUserId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("getPaymentHistory - Service failure should return 400")
    void getPaymentHistory_WithServiceFailure_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentProcessingService.getPaymentHistory(any(UUID.class), any()))
            .thenReturn(Result.failure("Failed to retrieve payment history"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/user/{userId}/history", testUserId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Failed to retrieve payment history"));
    }

    // ==================== POST /api/v1/payments/retry/{transactionId} Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("retryPayment - Valid retry should return 200")
    void retryPayment_WithValidRetry_ShouldReturn200() throws Exception {
        // Arrange
        when(paymentProcessingService.retryPayment(any(UUID.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testPaymentResponse)));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/retry/{transactionId}", testTransactionId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("retryPayment - Cannot retry completed transaction should return 400")
    void retryPayment_WithCompletedTransaction_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentProcessingService.retryPayment(any(UUID.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure("Cannot retry completed transaction")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/retry/{transactionId}", testTransactionId)
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Cannot retry completed")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("retryPayment - Non-existing transaction should return 400")
    void retryPayment_WithNonExistingTransaction_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentProcessingService.retryPayment(any(UUID.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure("Transaction not found")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/retry/{transactionId}", testTransactionId)
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("retryPayment - Without authentication should return 401")
    void retryPayment_WithoutAuthentication_ShouldReturn401() throws Exception {
        // Act & Assert - Zero Trust security
        mockMvc.perform(post("/api/v1/payments/retry/{transactionId}", testTransactionId)
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Security & Validation Tests ====================

    @Test
    @DisplayName("All endpoints - Without CSRF token should return 403")
    void allEndpoints_WithoutCsrfToken_ShouldReturn403() throws Exception {
        // POST endpoints require CSRF token
        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPaymentRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    @DisplayName("Protected endpoints - Insufficient role should return 403")
    void protectedEndpoints_WithInsufficientRole_ShouldReturn403() throws Exception {
        // GUEST role should not have access to USER endpoints
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}", testTransactionId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Response headers - All responses should include correlation ID")
    void allResponses_ShouldIncludeCorrelationId() throws Exception {
        // Arrange
        when(paymentProcessingService.getTransaction(any(UUID.class)))
            .thenReturn(Result.success(testTransaction));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/transaction/{transactionId}", testTransactionId))
            .andExpect(header().exists("X-Correlation-Id"))
            .andExpect(header().string("X-Correlation-Id", startsWith("payment-")));
    }
}
