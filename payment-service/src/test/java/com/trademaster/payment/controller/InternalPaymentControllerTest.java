package com.trademaster.payment.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Comprehensive Unit Tests for InternalPaymentController
 *
 * MANDATORY: Rule #20 - Integration Testing with Business Scenarios
 * MANDATORY: Test coverage >80% for all endpoints
 *
 * Tests:
 * - All 4 internal API endpoints
 * - Success and error scenarios
 * - Correlation ID tracking
 * - Result pattern handling
 * - Response mapping
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InternalPaymentController Tests")
class InternalPaymentControllerTest {

    @Mock
    private PaymentProcessingService paymentProcessingService;

    @InjectMocks
    private InternalPaymentController controller;

    private UUID testPaymentId;
    private UUID testUserId;
    private String testCorrelationId;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testPaymentId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCorrelationId = "test-correlation-id";

        testTransaction = PaymentTransaction.builder()
            .id(testPaymentId)
            .userId(testUserId)
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .createdAt(Instant.now())
            .build();
    }

    // ==================== verifyPaymentStatus Tests ====================

    @Test
    @DisplayName("Should verify payment status successfully")
    void verifyPaymentStatus_withValidPaymentId_shouldReturnSuccess() {
        // Given
        when(paymentProcessingService.getTransaction(testPaymentId))
            .thenReturn(Result.success(testTransaction));

        // When
        ResponseEntity<Object> response = controller.verifyPaymentStatus(
            testPaymentId,
            testCorrelationId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when payment not found")
    void verifyPaymentStatus_withInvalidPaymentId_shouldReturn404() {
        // Given
        when(paymentProcessingService.getTransaction(testPaymentId))
            .thenReturn(Result.failure("Payment not found"));

        // When
        ResponseEntity<Object> response = controller.verifyPaymentStatus(
            testPaymentId,
            testCorrelationId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
    }

    @Test
    @DisplayName("Should generate correlation ID when not provided")
    void verifyPaymentStatus_withoutCorrelationId_shouldGenerateOne() {
        // Given
        when(paymentProcessingService.getTransaction(testPaymentId))
            .thenReturn(Result.success(testTransaction));

        // When
        ResponseEntity<Object> response = controller.verifyPaymentStatus(
            testPaymentId,
            null
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Correlation-Id")).isNotNull();
        assertThat(response.getHeaders().get("X-Correlation-Id").get(0)).startsWith("internal-");
    }

    // ==================== getUserPaymentDetails Tests ====================

    @Test
    @DisplayName("Should get user payment details successfully")
    void getUserPaymentDetails_withValidUserId_shouldReturnSuccess() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentTransaction> page = new PageImpl<>(List.of(testTransaction), pageable, 1);

        when(paymentProcessingService.getPaymentHistory(eq(testUserId), any(Pageable.class)))
            .thenReturn(Result.success(page));

        // When
        ResponseEntity<Object> response = controller.getUserPaymentDetails(
            testUserId,
            testCorrelationId,
            pageable
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when user has no payment history")
    void getUserPaymentDetails_withInvalidUserId_shouldReturn404() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(paymentProcessingService.getPaymentHistory(eq(testUserId), any(Pageable.class)))
            .thenReturn(Result.failure("User not found"));

        // When
        ResponseEntity<Object> response = controller.getUserPaymentDetails(
            testUserId,
            testCorrelationId,
            pageable
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
    }

    // ==================== initiateRefund Tests ====================

    @Test
    @DisplayName("Should initiate refund successfully")
    void initiateRefund_withValidRequest_shouldReturnSuccess() throws Exception {
        // Given
        RefundRequest request = RefundRequest.builder()
            .transactionId(testPaymentId)
            .amount(new BigDecimal("99.99"))
            .reason("Customer request")
            .build();

        RefundResponse refundResponse = RefundResponse.builder()
            .refundId(UUID.randomUUID().toString())
            .status("PENDING")
            .amount(new BigDecimal("99.99"))
            .currency("USD")
            .build();

        when(paymentProcessingService.processRefund(any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(refundResponse)));

        // When
        CompletableFuture<ResponseEntity<Object>> responseFuture = controller.initiateRefund(
            request,
            testCorrelationId
        );

        ResponseEntity<Object> response = responseFuture.get();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 when refund request is invalid")
    void initiateRefund_withInvalidRequest_shouldReturn400() throws Exception {
        // Given
        RefundRequest request = RefundRequest.builder()
            .transactionId(testPaymentId)
            .amount(new BigDecimal("99.99"))
            .reason("Customer request")
            .build();

        when(paymentProcessingService.processRefund(any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.failure("Invalid refund amount")));

        // When
        CompletableFuture<ResponseEntity<Object>> responseFuture = controller.initiateRefund(
            request,
            testCorrelationId
        );

        ResponseEntity<Object> response = responseFuture.get();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
    }

    // ==================== getTransactionByGatewayPaymentId Tests ====================

    @Test
    @DisplayName("Should get transaction by gateway payment ID successfully")
    void getTransactionByGatewayPaymentId_withValidId_shouldReturnSuccess() {
        // Given
        String gatewayPaymentId = "pay_123abc";
        when(paymentProcessingService.getTransactionByGatewayPaymentId(gatewayPaymentId))
            .thenReturn(Result.success(testTransaction));

        // When
        ResponseEntity<Object> response = controller.getTransactionByGatewayPaymentId(
            gatewayPaymentId,
            testCorrelationId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when gateway payment ID not found")
    void getTransactionByGatewayPaymentId_withInvalidId_shouldReturn404() {
        // Given
        String gatewayPaymentId = "pay_invalid";
        when(paymentProcessingService.getTransactionByGatewayPaymentId(gatewayPaymentId))
            .thenReturn(Result.failure("Transaction not found"));

        // When
        ResponseEntity<Object> response = controller.getTransactionByGatewayPaymentId(
            gatewayPaymentId,
            testCorrelationId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().get("X-Correlation-Id")).contains(testCorrelationId);
    }
}
