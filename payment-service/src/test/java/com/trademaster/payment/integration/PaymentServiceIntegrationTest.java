package com.trademaster.payment.integration;

import com.trademaster.payment.PaymentServiceApplication;
import com.trademaster.payment.entity.*;
import com.trademaster.payment.enums.*;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import com.trademaster.payment.repository.UserPaymentMethodRepository;
import com.trademaster.payment.repository.RefundRepository;
import com.trademaster.payment.service.SubscriptionService;
import com.trademaster.payment.service.UserPaymentMethodService;
import com.trademaster.payment.service.PaymentProcessingService;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TradeMaster Payment Service
 * 
 * Tests complete payment processing workflows:
 * - PostgreSQL for payment persistence and transactions
 * - WireMock for payment gateway integration (Razorpay/Stripe)
 * - Redis for session and cache management
 * - Payment lifecycle: authorization → capture → settlement → refund
 * - Subscription billing and recurring payments
 * - Payment method validation and security
 * 
 * MANDATORY: TestContainers for enterprise-grade testing
 * MANDATORY: 80%+ coverage with realistic payment workflows
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(classes = PaymentServiceApplication.class)
@Testcontainers
@ActiveProfiles("integration-test")
@Transactional
public class PaymentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("trademaster_payment_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(1));

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        
        // Payment Gateway Mock URLs (WireMock)
        registry.add("payment.razorpay.base-url", () -> "http://localhost:8089/razorpay");
        registry.add("payment.stripe.base-url", () -> "http://localhost:8089/stripe");
        registry.add("payment.razorpay.key-id", () -> "rzp_test_12345");
        registry.add("payment.razorpay.key-secret", () -> "test_secret_12345");
        registry.add("payment.stripe.public-key", () -> "pk_test_12345");
        registry.add("payment.stripe.secret-key", () -> "sk_test_12345");
        
        // JPA configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        
        // Virtual Threads configuration
        registry.add("spring.threads.virtual.enabled", () -> "true");
        
        // Test mode configuration
        registry.add("payment.test-mode", () -> "true");
        registry.add("payment.webhook.validation.enabled", () -> "false");
    }

    @Autowired
    private PaymentProcessingService paymentService;

    @Autowired
    private UserPaymentMethodService paymentMethodService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private RefundRepository refundRepository;

    private Long testUserId;
    private String testEmail;
    private BigDecimal testAmount;

    @BeforeEach
    void setUp() {
        testUserId = 12345L;
        testEmail = "test@trademaster.com";
        testAmount = new BigDecimal("999.00");
        
        // Clear any existing test data
        refundRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        
        // Setup WireMock stubs
        setupPaymentGatewayMocks();
    }

    @Test
    void processPayment_WithValidRazorpayCard_ShouldSucceed() {
        // Arrange
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .userId(testUserId)
                .paymentMethodId(paymentMethod.getId())
                .amount(testAmount)
                .currency("INR")
                .description("TradeMaster Premium Subscription")
                .build();

        // Act
        PaymentTransaction transaction = paymentService.processPayment(paymentRequest);

        // Assert
        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(testUserId, transaction.getUserId());
        assertEquals(testAmount, transaction.getAmount());
        assertEquals("INR", transaction.getCurrency());
        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertEquals(PaymentProvider.RAZORPAY, transaction.getProvider());
        assertNotNull(transaction.getPaymentId());
        assertNotNull(transaction.getCreatedAt());
        
        // Verify persistence in database
        Optional<PaymentTransaction> persistedTransaction = 
                transactionRepository.findById(transaction.getId());
        assertTrue(persistedTransaction.isPresent());
        assertEquals(transaction.getPaymentId(), persistedTransaction.get().getPaymentId());
        assertEquals(PaymentStatus.SUCCESS, persistedTransaction.get().getStatus());
    }

    @Test
    void processPayment_WithValidStripeCard_ShouldSucceed() {
        // Arrange
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.STRIPE);
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .userId(testUserId)
                .paymentMethodId(paymentMethod.getId())
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .description("TradeMaster Pro Plan")
                .build();

        // Act
        PaymentTransaction transaction = paymentService.processPayment(paymentRequest);

        // Assert
        assertEquals(new BigDecimal("49.99"), transaction.getAmount());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertEquals(PaymentProvider.STRIPE, transaction.getProvider());
    }

    @Test
    void paymentLifecycle_AuthorizeCaptureRefund_ShouldWorkEndToEnd() {
        // Step 1: Create payment method
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        // Step 2: Authorize payment (hold funds)
        PaymentRequest authRequest = PaymentRequest.builder()
                .userId(testUserId)
                .paymentMethodId(paymentMethod.getId())
                .amount(testAmount)
                .currency("INR")
                .description("Pre-authorization for trading")
                .authorizeOnly(true)
                .build();

        PaymentTransaction authorizedTransaction = paymentService.processPayment(authRequest);
        assertEquals(PaymentStatus.AUTHORIZED, authorizedTransaction.getStatus());
        
        // Step 3: Capture authorized payment
        PaymentTransaction capturedTransaction = paymentService.capturePayment(
                authorizedTransaction.getId(), testAmount);
        assertEquals(PaymentStatus.SUCCESS, capturedTransaction.getStatus());
        assertEquals(testAmount, capturedTransaction.getCapturedAmount());
        assertNotNull(capturedTransaction.getCapturedAt());
        
        // Step 4: Process partial refund
        BigDecimal refundAmount = new BigDecimal("299.00");
        RefundRequest refundRequest = RefundRequest.builder()
                .transactionId(capturedTransaction.getId())
                .amount(refundAmount)
                .reason("Partial service cancellation")
                .build();
        
        Refund refund = paymentService.processRefund(refundRequest);
        
        // Assert refund
        assertEquals(refundAmount, refund.getAmount());
        assertEquals(RefundStatus.SUCCESS, refund.getStatus());
        assertEquals(capturedTransaction.getId(), refund.getTransactionId());
        assertNotNull(refund.getRefundId());
        assertNotNull(refund.getProcessedAt());
        
        // Verify refund persistence
        List<Refund> refunds = refundRepository.findByTransactionId(capturedTransaction.getId());
        assertEquals(1, refunds.size());
        assertEquals(refundAmount, refunds.get(0).getAmount());
        
        // Verify transaction updated with refund info
        PaymentTransaction updatedTransaction = transactionRepository.findById(capturedTransaction.getId())
                .orElseThrow(() -> new AssertionError("Transaction not found"));
        assertEquals(refundAmount, updatedTransaction.getRefundedAmount());
    }

    @Test
    void subscriptionBilling_WithRecurringPayment_ShouldProcessSuccessfully() {
        // Arrange
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        // Create subscription
        Subscription subscription = Subscription.builder()
                .userId(testUserId)
                .planId("PREMIUM_MONTHLY")
                .paymentMethodId(paymentMethod.getId())
                .amount(new BigDecimal("1999.00"))
                .currency("INR")
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle("MONTHLY")
                .nextBillingDate(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
        
        // Act - Process first billing cycle
        PaymentTransaction transaction1 = subscriptionService.processBilling(subscription);
        
        // Simulate monthly billing
        subscription.setNextBillingDate(LocalDateTime.now().plusDays(60));
        PaymentTransaction transaction2 = subscriptionService.processBilling(subscription);
        
        // Assert both transactions succeeded
        assertEquals(PaymentStatus.SUCCESS, transaction1.getStatus());
        assertEquals(PaymentStatus.SUCCESS, transaction2.getStatus());
        assertEquals(PaymentType.SUBSCRIPTION, transaction1.getType());
        assertEquals(PaymentType.SUBSCRIPTION, transaction2.getType());
        
        // Verify both transactions linked to same subscription
        assertEquals(subscription.getId(), transaction1.getSubscriptionId());
        assertEquals(subscription.getId(), transaction2.getSubscriptionId());
        
        // Verify billing amounts consistent
        assertEquals(subscription.getAmount(), transaction1.getAmount());
        assertEquals(subscription.getAmount(), transaction2.getAmount());
    }

    @Test
    void paymentMethodValidation_WithInvalidData_ShouldReturnErrors() {
        // Test 1: Invalid card number
        UserPaymentMethod invalidCard = UserPaymentMethod.builder()
                .userId(testUserId)
                .provider(PaymentProvider.RAZORPAY)
                .type(PaymentMethodType.CREDIT_CARD)
                .cardNumber("1234567890123456") // Invalid test card
                .expiryMonth(12)
                .expiryYear(2025)
                .holderName("Test User")
                .isDefault(false)
                .isActive(true)
                .build();
        
        assertThrows(Exception.class, () -> paymentMethodService.addPaymentMethod(invalidCard));
        
        // Test 2: Expired card
        UserPaymentMethod expiredCard = UserPaymentMethod.builder()
                .userId(testUserId)
                .provider(PaymentProvider.STRIPE)
                .type(PaymentMethodType.CREDIT_CARD)
                .cardNumber("4000000000000002")
                .expiryMonth(1)
                .expiryYear(2020) // Expired
                .holderName("Test User")
                .isDefault(false)
                .isActive(true)
                .build();
        
        assertThrows(Exception.class, () -> paymentMethodService.addPaymentMethod(expiredCard));
        
        // Test 3: Empty holder name
        UserPaymentMethod noHolderName = UserPaymentMethod.builder()
                .userId(testUserId)
                .provider(PaymentProvider.RAZORPAY)
                .type(PaymentMethodType.CREDIT_CARD)
                .cardNumber("4111111111111111")
                .expiryMonth(12)
                .expiryYear(2025)
                .holderName("") // Empty name
                .isDefault(false)
                .isActive(true)
                .build();
        
        assertThrows(Exception.class, () -> paymentMethodService.addPaymentMethod(noHolderName));
        
        // Verify no invalid payment methods persisted
        List<UserPaymentMethod> paymentMethods = paymentMethodRepository.findByUserId(testUserId);
        assertEquals(0, paymentMethods.size(), "No invalid payment methods should be persisted");
    }

    @Test
    void concurrentPaymentProcessing_With25Payments_ShouldHandleAllSuccessfully() throws InterruptedException {
        // Arrange
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        int numberOfPayments = 25;
        CountDownLatch latch = new CountDownLatch(numberOfPayments);
        
        // Act - Process payments concurrently using Virtual Threads
        List<CompletableFuture<PaymentTransaction>> futures = IntStream.range(0, numberOfPayments)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        PaymentRequest request = PaymentRequest.builder()
                                .userId(testUserId + i) // Different users
                                .paymentMethodId(paymentMethod.getId())
                                .amount(new BigDecimal("100.00").add(new BigDecimal(i)))
                                .currency("INR")
                                .description("Concurrent payment test " + i)
                                .build();
                        
                        return paymentService.processPayment(request);
                    } finally {
                        latch.countDown();
                    }
                }))
                .toList();

        // Wait for all payments to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All payments should complete within 30 seconds");

        // Assert all payments succeeded
        List<PaymentTransaction> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        assertEquals(numberOfPayments, results.size());
        
        long successCount = results.stream()
                .mapToLong(tx -> PaymentStatus.SUCCESS.equals(tx.getStatus()) ? 1 : 0)
                .sum();
        
        assertEquals(numberOfPayments, successCount, "All concurrent payments should succeed");

        // Verify all payments persisted
        List<PaymentTransaction> persistedTransactions = transactionRepository.findAll();
        assertEquals(numberOfPayments, persistedTransactions.size(), "All payments should be persisted");
    }

    @Test
    void paymentRetry_WithFailedPayment_ShouldRetryAndSucceed() {
        // Arrange - Setup WireMock to fail first attempt, succeed on retry
        wireMock.stubFor(post(urlPathMatching("/razorpay/payments"))
                .inScenario("Payment Retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":\"temporary_failure\"}"))
                .willSetStateTo("Retry"));
                
        wireMock.stubFor(post(urlPathMatching("/razorpay/payments"))
                .inScenario("Payment Retry")
                .whenScenarioStateIs("Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"pay_retry_success\",\"status\":\"captured\",\"amount\":99900}")));

        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .userId(testUserId)
                .paymentMethodId(paymentMethod.getId())
                .amount(testAmount)
                .currency("INR")
                .description("Retry test payment")
                .build();

        // Act - Process payment with retry logic
        PaymentTransaction transaction = paymentService.processPaymentWithRetry(paymentRequest, 2);

        // Assert - Payment should succeed after retry
        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertEquals("pay_retry_success", transaction.getPaymentId());
        assertTrue(transaction.getRetryCount() > 0, "Retry count should be greater than 0");
    }

    @Test
    void webhookProcessing_FromPaymentGateway_ShouldUpdateTransactionStatus() {
        // Arrange - Create pending transaction
        PaymentTransaction pendingTransaction = createPendingTransaction();
        
        // Simulate webhook payload from Razorpay
        String webhookPayload = """
                {
                    "event": "payment.captured",
                    "payload": {
                        "payment": {
                            "id": "%s",
                            "status": "captured",
                            "amount": %d,
                            "currency": "INR"
                        }
                    }
                }
                """.formatted(pendingTransaction.getPaymentId(), testAmount.multiply(new BigDecimal("100")).intValue());

        // Act - Process webhook
        paymentService.processWebhook(webhookPayload, PaymentProvider.RAZORPAY);

        // Assert - Transaction status should be updated
        PaymentTransaction updatedTransaction = transactionRepository.findById(pendingTransaction.getId())
                .orElseThrow(() -> new AssertionError("Transaction not found"));
        
        assertEquals(PaymentStatus.SUCCESS, updatedTransaction.getStatus());
        assertNotNull(updatedTransaction.getUpdatedAt());
    }

    @Test
    void paymentAnalytics_ShouldProvideAccurateMetrics() {
        // Arrange - Create various payment transactions
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        // Successful payments
        createTestTransaction(testUserId, paymentMethod.getId(), PaymentStatus.SUCCESS, new BigDecimal("1000.00"));
        createTestTransaction(testUserId + 1, paymentMethod.getId(), PaymentStatus.SUCCESS, new BigDecimal("2000.00"));
        createTestTransaction(testUserId + 2, paymentMethod.getId(), PaymentStatus.SUCCESS, new BigDecimal("1500.00"));
        
        // Failed payments
        createTestTransaction(testUserId + 3, paymentMethod.getId(), PaymentStatus.FAILED, new BigDecimal("500.00"));
        createTestTransaction(testUserId + 4, paymentMethod.getId(), PaymentStatus.FAILED, new BigDecimal("750.00"));

        // Act - Get payment analytics
        PaymentAnalytics analytics = paymentService.getPaymentAnalytics(
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        // Assert
        assertEquals(5, analytics.getTotalTransactions());
        assertEquals(3, analytics.getSuccessfulTransactions());
        assertEquals(2, analytics.getFailedTransactions());
        assertEquals(new BigDecimal("4500.00"), analytics.getTotalAmount());
        assertEquals(new BigDecimal("1500.00"), analytics.getAverageTransactionAmount());
        assertEquals(60.0, analytics.getSuccessRate()); // 3/5 = 60%
    }

    @Test
    void refundProcessing_WithMultipleRefunds_ShouldMaintainAccuracy() {
        // Arrange - Create successful payment
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.STRIPE);
        
        PaymentTransaction transaction = createTestTransaction(
                testUserId, paymentMethod.getId(), PaymentStatus.SUCCESS, new BigDecimal("1000.00"));

        // Act - Process multiple partial refunds
        RefundRequest refund1 = RefundRequest.builder()
                .transactionId(transaction.getId())
                .amount(new BigDecimal("300.00"))
                .reason("Partial cancellation")
                .build();
        
        RefundRequest refund2 = RefundRequest.builder()
                .transactionId(transaction.getId())
                .amount(new BigDecimal("200.00"))
                .reason("Service adjustment")
                .build();
        
        Refund processedRefund1 = paymentService.processRefund(refund1);
        Refund processedRefund2 = paymentService.processRefund(refund2);

        // Assert both refunds
        assertEquals(RefundStatus.SUCCESS, processedRefund1.getStatus());
        assertEquals(RefundStatus.SUCCESS, processedRefund2.getStatus());
        assertEquals(new BigDecimal("300.00"), processedRefund1.getAmount());
        assertEquals(new BigDecimal("200.00"), processedRefund2.getAmount());
        
        // Verify transaction updated with total refunded amount
        PaymentTransaction updatedTransaction = transactionRepository.findById(transaction.getId())
                .orElseThrow(() -> new AssertionError("Transaction not found"));
        assertEquals(new BigDecimal("500.00"), updatedTransaction.getRefundedAmount()); // 300 + 200
        
        // Verify refund history
        List<Refund> refunds = refundRepository.findByTransactionId(transaction.getId());
        assertEquals(2, refunds.size());
        
        BigDecimal totalRefunded = refunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("500.00"), totalRefunded);
    }

    /**
     * Helper methods for test data creation and WireMock setup
     */
    private void setupPaymentGatewayMocks() {
        // Razorpay success response
        wireMock.stubFor(post(urlPathMatching("/razorpay/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"pay_test_12345\",\"status\":\"captured\",\"amount\":99900}")));
        
        // Stripe success response
        wireMock.stubFor(post(urlPathMatching("/stripe/payment_intents"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"pi_test_12345\",\"status\":\"succeeded\",\"amount\":4999}")));
        
        // Refund endpoints
        wireMock.stubFor(post(urlPathMatching("/razorpay/payments/.*/refund"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"rfnd_test_12345\",\"status\":\"processed\"}")));
        
        wireMock.stubFor(post(urlPathMatching("/stripe/refunds"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"re_test_12345\",\"status\":\"succeeded\"}")));
    }
    
    private UserPaymentMethod createTestPaymentMethod(Long userId, PaymentProvider provider) {
        UserPaymentMethod paymentMethod = UserPaymentMethod.builder()
                .userId(userId)
                .provider(provider)
                .type(PaymentMethodType.CREDIT_CARD)
                .cardNumber(provider == PaymentProvider.RAZORPAY ? "4111111111111111" : "4000000000000002")
                .expiryMonth(12)
                .expiryYear(2025)
                .holderName("Test User")
                .isDefault(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return paymentMethodRepository.save(paymentMethod);
    }
    
    private PaymentTransaction createTestTransaction(Long userId, Long paymentMethodId, 
            PaymentStatus status, BigDecimal amount) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .paymentMethodId(paymentMethodId)
                .amount(amount)
                .currency("INR")
                .status(status)
                .provider(PaymentProvider.RAZORPAY)
                .type(PaymentType.ONE_TIME)
                .paymentId("pay_test_" + System.currentTimeMillis())
                .description("Test transaction")
                .createdAt(LocalDateTime.now())
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    private PaymentTransaction createPendingTransaction() {
        UserPaymentMethod paymentMethod = createTestPaymentMethod(testUserId, PaymentProvider.RAZORPAY);
        
        return createTestTransaction(testUserId, paymentMethod.getId(), PaymentStatus.PENDING, testAmount);
    }
}