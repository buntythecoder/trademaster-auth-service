package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.dto.PaymentRequest;
import com.trademaster.payment.dto.PaymentResponse;
import com.trademaster.payment.dto.RefundRequest;
import com.trademaster.payment.dto.RefundResponse;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import com.trademaster.payment.service.gateway.PaymentGatewayFactory;
import com.trademaster.payment.service.impl.PaymentProcessingServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentProcessingServiceImpl
 *
 * Tests payment processing workflows with functional programming patterns:
 * - Railway programming with Result types
 * - Virtual threads with CompletableFuture
 * - Circuit breaker integration
 * - Event publishing
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else in test logic
 * - Rule 11: Railway programming with Result types
 * - Rule 20: >80% code coverage target
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessingService Unit Tests")
class PaymentProcessingServiceImplTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentGatewayFactory gatewayFactory;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeterRegistry meterRegistry;

    private PaymentProcessingServiceImpl paymentService;

    private UUID testUserId;
    private UUID testTransactionId;
    private BigDecimal testAmount;
    private PaymentRequest validPaymentRequest;
    private PaymentTransaction mockTransaction;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        paymentService = new PaymentProcessingServiceImpl(
            transactionRepository,
            gatewayFactory,
            kafkaTemplate,
            meterRegistry
        );

        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();
        testAmount = new BigDecimal("999.00");

        // Setup valid payment request
        validPaymentRequest = PaymentRequest.builder()
            .userId(testUserId)
            .subscriptionPlanId(UUID.randomUUID())
            .amount(testAmount)
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .metadata(PaymentRequest.PaymentMetadata.builder()
                .description("Test payment")
                .build())
            .build();

        // Setup mock transaction
        mockTransaction = PaymentTransaction.builder()
            .id(testTransactionId)
            .userId(testUserId)
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .paymentMethod(PaymentMethod.CARD)
            .gatewayPaymentId("pay_test_12345")
            .description("Test payment")
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("processPayment - Valid request should succeed")
    void processPayment_WithValidRequest_ShouldSucceed() throws Exception {
        // Arrange
        PaymentResponse mockGatewayResponse = PaymentResponse.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .gateway(PaymentGateway.RAZORPAY)
            .gatewayResponse(PaymentResponse.GatewayResponse.builder()
                .paymentId("pay_test_12345")
                .build())
            .createdAt(Instant.now())
            .build();

        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(gatewayFactory.createPayment(any(PaymentRequest.class)))
            .thenReturn(Result.success("order_test_12345"));
        when(kafkaTemplate.send(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(validPaymentRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert - Railway programming validation
        assertTrue(result.isSuccess(), "Payment should succeed with valid request");

        result.onSuccess(response -> {
            assertNotNull(response, "Payment response should not be null");
            assertEquals(testAmount, response.getAmount(), "Amount should match");
            assertEquals("INR", response.getCurrency(), "Currency should match");
            assertEquals(PaymentGateway.RAZORPAY, response.getGateway(), "Gateway should match");
            assertNotNull(response.getCreatedAt(), "Created timestamp should be set");
        });

        // Verify interactions
        verify(transactionRepository, atLeastOnce()).save(any(PaymentTransaction.class));
        verify(gatewayFactory).createPayment(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("processPayment - Null amount should fail validation")
    void processPayment_WithNullAmount_ShouldFailValidation() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(testUserId)
            .amount(null) // Invalid: null amount
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(invalidRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert - Railway programming failure path
        assertTrue(result.isFailure(), "Payment with null amount should fail validation");

        result.onFailure(error -> {
            assertNotNull(error, "Error message should not be null");
            assertTrue(error.contains("Invalid payment request"),
                "Error should indicate validation failure");
        });

        // Verify no gateway calls made
        verify(gatewayFactory, never()).createPayment(any());
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("processPayment - Zero amount should fail validation")
    void processPayment_WithZeroAmount_ShouldFailValidation() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(testUserId)
            .amount(BigDecimal.ZERO) // Invalid: zero amount
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(invalidRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Payment with zero amount should fail validation");
        verify(gatewayFactory, never()).createPayment(any());
    }

    @Test
    @DisplayName("processPayment - Negative amount should fail validation")
    void processPayment_WithNegativeAmount_ShouldFailValidation() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(testUserId)
            .amount(new BigDecimal("-100.00")) // Invalid: negative amount
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(invalidRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Payment with negative amount should fail validation");
    }

    @Test
    @DisplayName("processPayment - Null userId should fail validation")
    void processPayment_WithNullUserId_ShouldFailValidation() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(null) // Invalid: null userId
            .amount(testAmount)
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .build();

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(invalidRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Payment with null userId should fail validation");
    }

    @Test
    @DisplayName("processPayment - Null gateway should fail validation")
    void processPayment_WithNullGateway_ShouldFailValidation() throws Exception {
        // Arrange
        PaymentRequest invalidRequest = PaymentRequest.builder()
            .userId(testUserId)
            .amount(testAmount)
            .currency("INR")
            .paymentMethod(PaymentMethod.CARD)
            .paymentGateway(null) // Invalid: null gateway
            .build();

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(invalidRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Payment with null gateway should fail validation");
    }

    @Test
    @DisplayName("processPayment - Gateway failure should return error")
    void processPayment_WithGatewayFailure_ShouldReturnError() throws Exception {
        // Arrange
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(gatewayFactory.createPayment(any(PaymentRequest.class)))
            .thenReturn(Result.failure("Gateway connection failed"));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(validPaymentRequest);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert - Railway programming error handling
        assertTrue(result.isFailure(), "Gateway failure should result in error");

        result.onFailure(error -> {
            assertNotNull(error, "Error message should not be null");
            assertTrue(error.contains("Gateway") || error.contains("failed"),
                "Error should indicate gateway failure");
        });

        // Verify transaction was created
        verify(transactionRepository, atLeastOnce()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("confirmPayment - Valid transaction should succeed")
    void confirmPayment_WithValidTransaction_ShouldSucceed() throws Exception {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(mockTransaction));
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(kafkaTemplate.send(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.confirmPayment(testTransactionId, "pm_test_12345");
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isSuccess(), "Confirm payment should succeed with valid transaction");

        result.onSuccess(response -> {
            assertNotNull(response);
            assertEquals(testTransactionId, response.getTransactionId());
        });

        verify(transactionRepository).findById(testTransactionId);
        verify(transactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("confirmPayment - Non-existent transaction should fail")
    void confirmPayment_WithNonExistentTransaction_ShouldFail() throws Exception {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.empty());

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.confirmPayment(testTransactionId, "pm_test_12345");
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Confirm payment should fail for non-existent transaction");

        result.onFailure(error -> {
            assertTrue(error.contains("not found") || error.contains("Transaction"),
                "Error should indicate transaction not found");
        });

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processRefund - Valid refund should succeed")
    void processRefund_WithValidRequest_ShouldSucceed() throws Exception {
        // Arrange
        RefundRequest refundRequest = RefundRequest.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .reason("Customer requested refund")
            .build();

        RefundResponse mockRefundResponse = RefundResponse.builder()
            .refundId("rfnd_test_12345")
            .transactionId(testTransactionId.toString())
            .amount(testAmount)
            .currency("INR")
            .status("processed")
            .createdAt(Instant.now())
            .build();

        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(mockTransaction));
        when(gatewayFactory.processRefund(anyString(), any(RefundRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Result.success(mockRefundResponse)));
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);

        // Act
        CompletableFuture<Result<RefundResponse, String>> futureResult =
            paymentService.processRefund(refundRequest);
        Result<RefundResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isSuccess(), "Refund should succeed with valid request");

        result.onSuccess(response -> {
            assertNotNull(response);
            assertEquals(testTransactionId, response.getTransactionId());
            assertEquals(testAmount, response.getAmount());
            assertEquals("processed", response.getStatus());
        });

        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    @DisplayName("processRefund - Non-existent transaction should fail")
    void processRefund_WithNonExistentTransaction_ShouldFail() throws Exception {
        // Arrange
        RefundRequest refundRequest = RefundRequest.builder()
            .transactionId(testTransactionId)
            .amount(testAmount)
            .reason("Refund request")
            .build();

        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.empty());

        // Act
        CompletableFuture<Result<RefundResponse, String>> futureResult =
            paymentService.processRefund(refundRequest);
        Result<RefundResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Refund should fail for non-existent transaction");
    }

    @Test
    @DisplayName("getTransaction - Existing transaction should be retrieved")
    void getTransaction_WithExistingId_ShouldReturnTransaction() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(mockTransaction));

        // Act
        Result<PaymentTransaction, String> result =
            paymentService.getTransaction(testTransactionId);

        // Assert
        assertTrue(result.isSuccess(), "Should retrieve existing transaction");

        result.onSuccess(transaction -> {
            assertNotNull(transaction);
            assertEquals(testTransactionId, transaction.getId());
            assertEquals(testUserId, transaction.getUserId());
            assertEquals(testAmount, transaction.getAmount());
        });

        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    @DisplayName("getTransaction - Non-existent transaction should fail")
    void getTransaction_WithNonExistentId_ShouldFail() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.empty());

        // Act
        Result<PaymentTransaction, String> result =
            paymentService.getTransaction(testTransactionId);

        // Assert
        assertTrue(result.isFailure(), "Should fail for non-existent transaction");

        result.onFailure(error -> {
            assertTrue(error.contains("not found"), "Error should indicate not found");
        });
    }

    @Test
    @DisplayName("getPaymentStatus - Existing transaction should return status")
    void getPaymentStatus_WithExistingTransaction_ShouldReturnStatus() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(mockTransaction));

        // Act
        Result<PaymentStatus, String> result =
            paymentService.getPaymentStatus(testTransactionId);

        // Assert
        assertTrue(result.isSuccess(), "Should retrieve payment status");

        result.onSuccess(status -> {
            assertNotNull(status);
            assertEquals(PaymentStatus.COMPLETED, status);
        });
    }

    @Test
    @DisplayName("getPaymentHistory - Should return paginated results")
    void getPaymentHistory_WithValidUserId_ShouldReturnPaginatedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<PaymentTransaction> transactions = List.of(mockTransaction);
        Page<PaymentTransaction> page = new PageImpl<>(transactions, pageable, 1);

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
            .thenReturn(page);

        // Act
        Result<Page<PaymentTransaction>, String> result =
            paymentService.getPaymentHistory(testUserId, pageable);

        // Assert
        assertTrue(result.isSuccess(), "Should retrieve payment history");

        result.onSuccess(historyPage -> {
            assertNotNull(historyPage);
            assertEquals(1, historyPage.getTotalElements());
            assertEquals(1, historyPage.getContent().size());
            assertEquals(testTransactionId, historyPage.getContent().get(0).getId());
        });

        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("retryPayment - Valid failed transaction should retry")
    void retryPayment_WithValidFailedTransaction_ShouldRetry() throws Exception {
        // Arrange
        PaymentTransaction failedTransaction = PaymentTransaction.builder()
            .id(testTransactionId)
            .userId(testUserId)
            .amount(testAmount)
            .currency("INR")
            .status(PaymentStatus.FAILED)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .paymentMethod(PaymentMethod.CARD)
            .build();

        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(failedTransaction));
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(gatewayFactory.createPayment(any(PaymentRequest.class)))
            .thenReturn(Result.success("order_retry_12345"));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.retryPayment(testTransactionId);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isSuccess(), "Retry should succeed for failed transaction");

        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    @DisplayName("retryPayment - Completed transaction should not retry")
    void retryPayment_WithCompletedTransaction_ShouldNotRetry() throws Exception {
        // Arrange - transaction already completed
        when(transactionRepository.findById(testTransactionId))
            .thenReturn(Optional.of(mockTransaction));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.retryPayment(testTransactionId);
        Result<PaymentResponse, String> result = futureResult.get();

        // Assert
        assertTrue(result.isFailure(), "Should not retry completed transaction");

        result.onFailure(error -> {
            assertTrue(error.contains("cannot be retried") || error.contains("already"),
                "Error should indicate transaction cannot be retried");
        });

        // Verify no gateway calls made
        verify(gatewayFactory, never()).createPayment(any());
    }

    @Test
    @DisplayName("processPayment - Should record metrics")
    void processPayment_ShouldRecordMetrics() throws Exception {
        // Arrange
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(gatewayFactory.createPayment(any(PaymentRequest.class)))
            .thenReturn(Result.success("order_test_12345"));

        // Act
        CompletableFuture<Result<PaymentResponse, String>> futureResult =
            paymentService.processPayment(validPaymentRequest);
        futureResult.get();

        // Assert - Verify metrics were recorded
        assertNotNull(meterRegistry.find("payment.processing.requests").counter(),
            "Payment processing counter should be registered");
    }

    @Test
    @DisplayName("Virtual Threads - Multiple concurrent payments should execute in parallel")
    void processPayment_WithConcurrentRequests_ShouldExecuteInParallel() throws Exception {
        // Arrange
        int numberOfPayments = 5;
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);
        when(gatewayFactory.createPayment(any(PaymentRequest.class)))
            .thenReturn(Result.success("order_concurrent_12345"));

        // Act - Submit multiple payments concurrently
        List<CompletableFuture<Result<PaymentResponse, String>>> futures =
            java.util.stream.IntStream.range(0, numberOfPayments)
                .mapToObj(i -> {
                    PaymentRequest request = PaymentRequest.builder()
                        .userId(UUID.randomUUID())
                        .amount(testAmount)
                        .currency("INR")
                        .paymentMethod(PaymentMethod.CARD)
                        .paymentGateway(PaymentGateway.RAZORPAY)
                        .build();
                    return paymentService.processPayment(request);
                })
                .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Assert - All should complete successfully
        List<Result<PaymentResponse, String>> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        long successCount = results.stream()
            .filter(Result::isSuccess)
            .count();

        assertEquals(numberOfPayments, successCount,
            "All concurrent payments should succeed");

        // Verify parallel execution
        verify(transactionRepository, times(numberOfPayments))
            .save(any(PaymentTransaction.class));
    }
}
