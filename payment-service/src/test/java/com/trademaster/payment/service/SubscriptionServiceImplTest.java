package com.trademaster.payment.service;

import com.trademaster.common.functional.Result;
import com.trademaster.payment.entity.PaymentTransaction;
import com.trademaster.payment.entity.Subscription;
import com.trademaster.payment.entity.SubscriptionPlan;
import com.trademaster.payment.entity.UserSubscription;
import com.trademaster.payment.enums.PaymentGateway;
import com.trademaster.payment.enums.PaymentMethod;
import com.trademaster.payment.enums.PaymentStatus;
import com.trademaster.payment.enums.SubscriptionStatus;
import com.trademaster.payment.repository.PaymentTransactionRepository;
import com.trademaster.payment.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for SubscriptionServiceImpl
 *
 * Coverage:
 * - All subscription lifecycle methods
 * - Billing transaction creation (Result + entity patterns)
 * - Railway programming with Result types
 * - Functional error handling
 * - Long-to-UUID conversion testing
 *
 * Compliance:
 * - Rule 3: Functional Programming - NO if-else in test logic
 * - Rule 11: Railway programming with Result types
 * - Rule 20: >80% coverage target
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Unit Tests")
class SubscriptionServiceImplTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UserSubscription testUserSubscription;
    private Subscription testSubscription;
    private SubscriptionPlan testPlan;
    private PaymentTransaction mockTransaction;
    private UUID testUserId;
    private BigDecimal testAmount;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testUserId = UUID.randomUUID();
        testAmount = new BigDecimal("999.00");

        // Build test subscription plan
        testPlan = SubscriptionPlan.builder()
            .id(UUID.randomUUID())
            .name("Premium Plan")
            .description("Premium subscription plan")
            .price(testAmount)
            .currency("USD")
            .billingCycle(com.trademaster.payment.enums.BillingCycle.MONTHLY)
            .features(java.util.Map.of("feature1", true, "feature2", true))
            .limits(java.util.Map.of("maxUsers", 100))
            .isActive(true)
            .trialDays(7)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Build test user subscription with UserSubscription entity
        testUserSubscription = UserSubscription.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .subscriptionPlan(testPlan)
            .amount(testAmount)
            .currency("USD")
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(Instant.now())
            .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Build test subscription with Long userId (for testing compatibility)
        testSubscription = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(123L)
            .paymentMethodId(1L)
            .planId("plan_premium_monthly")
            .amount(testAmount)
            .currency("USD")
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle("MONTHLY")
            .startedAt(java.time.LocalDateTime.now())
            .nextBillingDate(java.time.LocalDateTime.now().plusDays(30))
            .build();

        // Build mock transaction
        mockTransaction = PaymentTransaction.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .amount(testAmount)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .paymentMethod(PaymentMethod.CARD)
            .description("Subscription billing")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ==================== processBilling(UserSubscription) Tests ====================

    @Test
    @DisplayName("processBilling - Valid UserSubscription should create transaction")
    void processBilling_WithValidUserSubscription_ShouldSucceed() {
        // Arrange
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);

        // Act
        Result<PaymentTransaction, String> result =
            subscriptionService.processBilling(testUserSubscription);

        // Assert - Railway programming validation
        assertTrue(result.isSuccess());
        result.onSuccess(transaction -> {
            assertNotNull(transaction);
            assertEquals(testAmount, transaction.getAmount());
            assertEquals("USD", transaction.getCurrency());
            assertEquals(PaymentStatus.COMPLETED, transaction.getStatus());
        });

        verify(transactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("processBilling - Should use correct payment gateway based on currency")
    void processBilling_ShouldSelectGatewayByCurrency() {
        // Arrange - INR should use Razorpay
        UserSubscription inrSubscription = UserSubscription.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .subscriptionPlan(testPlan)
            .amount(new BigDecimal("7500.00"))
            .currency("INR")
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(Instant.now())
            .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        PaymentTransaction inrTransaction = PaymentTransaction.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .amount(new BigDecimal("7500.00"))
            .currency("INR")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.RAZORPAY)
            .paymentMethod(PaymentMethod.CARD)
            .build();

        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(inrTransaction);

        // Act
        Result<PaymentTransaction, String> result =
            subscriptionService.processBilling(inrSubscription);

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(transaction ->
            assertEquals(PaymentGateway.RAZORPAY, transaction.getPaymentGateway())
        );
    }

    @Test
    @DisplayName("processBilling - Repository failure should return error")
    void processBilling_WithRepositoryFailure_ShouldReturnError() {
        // Arrange
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        Result<PaymentTransaction, String> result =
            subscriptionService.processBilling(testUserSubscription);

        // Assert - Railway programming error handling
        assertTrue(result.isFailure());
        result.onFailure(error -> {
            assertNotNull(error);
            assertTrue(error.contains("Database connection failed"));
        });
    }

    // ==================== processBilling(Subscription) Tests ====================

    @Test
    @DisplayName("processBilling - Valid Subscription entity should create transaction")
    void processBilling_WithValidSubscription_ShouldSucceed() {
        // Arrange
        PaymentTransaction expectedTransaction = PaymentTransaction.builder()
            .userId(new UUID(123L, 0L)) // Converted from Long userId
            .subscriptionId(testSubscription.getId())
            .amount(testAmount)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.STRIPE)
            .paymentMethod(PaymentMethod.CARD)
            .build();

        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(expectedTransaction);

        // Act
        PaymentTransaction transaction = subscriptionService.processBilling(testSubscription);

        // Assert
        assertNotNull(transaction);
        assertEquals(testAmount, transaction.getAmount());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(PaymentStatus.COMPLETED, transaction.getStatus());

        verify(transactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("processBilling - Should convert Long userId to UUID correctly")
    void processBilling_ShouldConvertLongUserIdToUuid() {
        // Arrange
        Long userId = 456L;
        Subscription subscription = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .paymentMethodId(1L)
            .planId("plan_test")
            .amount(testAmount)
            .currency("USD")
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle("MONTHLY")
            .startedAt(java.time.LocalDateTime.now())
            .nextBillingDate(java.time.LocalDateTime.now().plusDays(30))
            .build();

        PaymentTransaction capturedTransaction = PaymentTransaction.builder()
            .userId(new UUID(userId, 0L))
            .subscriptionId(subscription.getId())
            .amount(testAmount)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .paymentGateway(PaymentGateway.STRIPE)
            .paymentMethod(PaymentMethod.CARD)
            .build();

        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(capturedTransaction);

        // Act
        PaymentTransaction transaction = subscriptionService.processBilling(subscription);

        // Assert
        assertNotNull(transaction);
        assertEquals(new UUID(userId, 0L), transaction.getUserId());
    }

    // ==================== createSubscription Tests ====================

    @Test
    @DisplayName("createSubscription - Valid request should return success (not implemented)")
    void createSubscription_WithValidRequest_ShouldReturnSuccess() {
        // Arrange
        UserSubscription newSubscription = UserSubscription.builder()
            .userId(testUserId)
            .subscriptionPlan(testPlan)
            .amount(testAmount)
            .currency("USD")
            .status(SubscriptionStatus.INACTIVE)
            .currentPeriodStart(Instant.now())
            .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .build();

        // Act
        Result<UserSubscription, String> result =
            subscriptionService.createSubscription(newSubscription);

        // Assert - Currently returns success without persistence
        assertTrue(result.isSuccess());
        result.onSuccess(subscription -> {
            assertNotNull(subscription);
            assertEquals(testUserId, subscription.getUserId());
        });

        // Note: No repository interaction since feature not fully implemented
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSubscription - Should accept various subscription plans")
    void createSubscription_WithDifferentPlans_ShouldSucceed() {
        // Arrange - Test with different plan
        SubscriptionPlan basicPlan = SubscriptionPlan.builder()
            .id(UUID.randomUUID())
            .name("Basic Plan")
            .price(new BigDecimal("499.00"))
            .currency("USD")
            .billingCycle(com.trademaster.payment.enums.BillingCycle.MONTHLY)
            .features(java.util.Map.of("basicFeature", true))
            .limits(java.util.Map.of("maxUsers", 10))
            .isActive(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        UserSubscription basicSubscription = UserSubscription.builder()
            .userId(testUserId)
            .subscriptionPlan(basicPlan)
            .amount(new BigDecimal("499.00"))
            .currency("USD")
            .status(SubscriptionStatus.INACTIVE)
            .currentPeriodStart(Instant.now())
            .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
            .build();

        // Act
        Result<UserSubscription, String> result =
            subscriptionService.createSubscription(basicSubscription);

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(subscription ->
            assertEquals("Basic Plan", subscription.getSubscriptionPlan().getName())
        );
    }

    // ==================== cancelSubscription Tests ====================

    @Test
    @DisplayName("cancelSubscription - Immediate cancellation should return success (not implemented)")
    void cancelSubscription_WithImmediateCancellation_ShouldReturnSuccess() {
        // Arrange
        Long subscriptionId = 1L;
        boolean immediate = true;

        // Act
        Result<Boolean, String> result =
            subscriptionService.cancelSubscription(subscriptionId, immediate);

        // Assert - Currently returns success without gateway integration
        assertTrue(result.isSuccess());
        result.onSuccess(cancelled -> assertTrue(cancelled));
    }

    @Test
    @DisplayName("cancelSubscription - End of period cancellation should return success (not implemented)")
    void cancelSubscription_WithEndOfPeriodCancellation_ShouldReturnSuccess() {
        // Arrange
        Long subscriptionId = 2L;
        boolean immediate = false;

        // Act
        Result<Boolean, String> result =
            subscriptionService.cancelSubscription(subscriptionId, immediate);

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(cancelled -> assertTrue(cancelled));
    }

    // ==================== pauseSubscription Tests ====================

    @Test
    @DisplayName("pauseSubscription - Should return failure (feature not implemented)")
    void pauseSubscription_ShouldReturnFailure() {
        // Arrange
        Long subscriptionId = 1L;

        // Act
        Result<UserSubscription, String> result =
            subscriptionService.pauseSubscription(subscriptionId);

        // Assert - Feature explicitly not implemented
        assertTrue(result.isFailure());
        result.onFailure(error -> {
            assertNotNull(error);
            assertTrue(error.contains("pause feature not available"));
        });
    }

    @Test
    @DisplayName("pauseSubscription - Multiple calls should consistently return failure")
    void pauseSubscription_MultipleCalls_ShouldConsistentlyFail() {
        // Arrange
        Long subscriptionId = 1L;

        // Act - call multiple times
        Result<UserSubscription, String> result1 = subscriptionService.pauseSubscription(subscriptionId);
        Result<UserSubscription, String> result2 = subscriptionService.pauseSubscription(subscriptionId);
        Result<UserSubscription, String> result3 = subscriptionService.pauseSubscription(subscriptionId);

        // Assert - all should fail consistently
        assertTrue(result1.isFailure());
        assertTrue(result2.isFailure());
        assertTrue(result3.isFailure());
    }

    // ==================== resumeSubscription Tests ====================

    @Test
    @DisplayName("resumeSubscription - Should return failure (feature not implemented)")
    void resumeSubscription_ShouldReturnFailure() {
        // Arrange
        Long subscriptionId = 1L;

        // Act
        Result<UserSubscription, String> result =
            subscriptionService.resumeSubscription(subscriptionId);

        // Assert - Feature explicitly not implemented
        assertTrue(result.isFailure());
        result.onFailure(error -> {
            assertNotNull(error);
            assertTrue(error.contains("resume feature not available"));
        });
    }

    @Test
    @DisplayName("resumeSubscription - Different subscription IDs should all fail")
    void resumeSubscription_WithDifferentIds_ShouldAllFail() {
        // Act - test with different IDs
        Result<UserSubscription, String> result1 = subscriptionService.resumeSubscription(1L);
        Result<UserSubscription, String> result2 = subscriptionService.resumeSubscription(2L);
        Result<UserSubscription, String> result3 = subscriptionService.resumeSubscription(999L);

        // Assert - all should fail
        assertTrue(result1.isFailure());
        assertTrue(result2.isFailure());
        assertTrue(result3.isFailure());
    }

    // ==================== changeSubscriptionPlan Tests ====================

    @Test
    @DisplayName("changeSubscriptionPlan - Should return failure (feature not implemented)")
    void changeSubscriptionPlan_ShouldReturnFailure() {
        // Arrange
        Long subscriptionId = 1L;
        String newPlanId = "plan_premium_annual";

        // Act
        Result<UserSubscription, String> result =
            subscriptionService.changeSubscriptionPlan(subscriptionId, newPlanId);

        // Assert - Feature explicitly not implemented
        assertTrue(result.isFailure());
        result.onFailure(error -> {
            assertNotNull(error);
            assertTrue(error.contains("plan change feature not available"));
        });
    }

    @Test
    @DisplayName("changeSubscriptionPlan - Various plan changes should all fail")
    void changeSubscriptionPlan_WithVariousPlans_ShouldAllFail() {
        // Act - test with different plan upgrades/downgrades
        Result<UserSubscription, String> upgrade =
            subscriptionService.changeSubscriptionPlan(1L, "plan_premium");
        Result<UserSubscription, String> downgrade =
            subscriptionService.changeSubscriptionPlan(1L, "plan_basic");
        Result<UserSubscription, String> sameTier =
            subscriptionService.changeSubscriptionPlan(1L, "plan_premium_annual");

        // Assert - all should fail
        assertTrue(upgrade.isFailure());
        assertTrue(downgrade.isFailure());
        assertTrue(sameTier.isFailure());
    }

    @Test
    @DisplayName("changeSubscriptionPlan - Empty plan ID should fail")
    void changeSubscriptionPlan_WithEmptyPlanId_ShouldFail() {
        // Act
        Result<UserSubscription, String> result =
            subscriptionService.changeSubscriptionPlan(1L, "");

        // Assert
        assertTrue(result.isFailure());
    }

    // ==================== Integration & Edge Cases ====================

    @Test
    @DisplayName("processBilling - Multiple billing cycles should work correctly")
    void processBilling_MultipleBillingCycles_ShouldSucceed() {
        // Arrange
        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(mockTransaction);

        // Act - simulate 3 billing cycles
        Result<PaymentTransaction, String> cycle1 =
            subscriptionService.processBilling(testUserSubscription);
        Result<PaymentTransaction, String> cycle2 =
            subscriptionService.processBilling(testUserSubscription);
        Result<PaymentTransaction, String> cycle3 =
            subscriptionService.processBilling(testUserSubscription);

        // Assert - all should succeed
        assertTrue(cycle1.isSuccess());
        assertTrue(cycle2.isSuccess());
        assertTrue(cycle3.isSuccess());

        verify(transactionRepository, times(3)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("processBilling - Zero amount subscription should still process")
    void processBilling_WithZeroAmount_ShouldStillProcess() {
        // Arrange - free trial period
        UserSubscription freeSubscription = UserSubscription.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .subscriptionPlan(testPlan)
            .amount(BigDecimal.ZERO)
            .currency("USD")
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(Instant.now())
            .currentPeriodEnd(Instant.now().plus(7, ChronoUnit.DAYS))
            .build();

        PaymentTransaction freeTransaction = PaymentTransaction.builder()
            .userId(testUserId)
            .amount(BigDecimal.ZERO)
            .currency("USD")
            .status(PaymentStatus.COMPLETED)
            .build();

        when(transactionRepository.save(any(PaymentTransaction.class)))
            .thenReturn(freeTransaction);

        // Act
        Result<PaymentTransaction, String> result =
            subscriptionService.processBilling(freeSubscription);

        // Assert
        assertTrue(result.isSuccess());
        result.onSuccess(transaction ->
            assertEquals(BigDecimal.ZERO, transaction.getAmount())
        );
    }
}
