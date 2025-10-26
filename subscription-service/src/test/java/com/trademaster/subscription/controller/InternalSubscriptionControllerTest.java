package com.trademaster.subscription.controller;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.dto.internal.InternalResumeRequest;
import com.trademaster.subscription.dto.internal.InternalSubscriptionResponse;
import com.trademaster.subscription.dto.internal.InternalSuspendRequest;
import com.trademaster.subscription.dto.internal.InternalTierChangeRequest;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.security.SecurityContext;
import com.trademaster.subscription.security.SecurityError;
import com.trademaster.subscription.security.SecurityError.SecurityErrorType;
import com.trademaster.subscription.security.SecurityFacade;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InternalSubscriptionController Unit Tests
 * MANDATORY: Rule #20 - >80% test coverage
 *
 * Tests all internal API endpoints with comprehensive scenarios:
 * - Success scenarios
 * - Security failures
 * - Not found scenarios
 * - Correlation ID propagation
 *
 * Compliance:
 * - Rule #20: Unit tests with functional test builders
 * - Rule #3: Functional programming patterns in tests
 * - Rule #11: Railway programming validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InternalSubscriptionController Tests")
class InternalSubscriptionControllerTest {

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private SubscriptionLifecycleService lifecycleService;

    @Mock
    private SubscriptionUpgradeService upgradeService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpSession httpSession;

    private InternalSubscriptionController controller;

    private UUID testSubscriptionId;
    private UUID testUserId;
    private UUID testCorrelationId;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        controller = new InternalSubscriptionController(securityFacade, lifecycleService, upgradeService);

        testSubscriptionId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCorrelationId = UUID.randomUUID();

        testSubscription = buildTestSubscription();

        // Mock HttpServletRequest
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("test-session-id");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("payment-service");
        when(httpRequest.getRequestURI()).thenReturn("/api/internal/v1/subscription/subscriptions/" + testSubscriptionId);
    }

    // ==================== Test Data Builders ====================

    private Subscription buildTestSubscription() {
        return Subscription.builder()
            .id(testSubscriptionId)
            .userId(testUserId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .monthlyPrice(new BigDecimal("99.00"))
            .billingAmount(new BigDecimal("89.10"))
            .currency("USD")
            .startDate(LocalDateTime.now().minusMonths(1))
            .endDate(null)
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .autoRenewal(true)
            .createdAt(LocalDateTime.now().minusMonths(1))
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private Subscription buildSuspendedSubscription() {
        return Subscription.builder()
            .id(testSubscriptionId)
            .userId(testUserId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.SUSPENDED)
            .monthlyPrice(new BigDecimal("99.00"))
            .billingAmount(new BigDecimal("89.10"))
            .currency("USD")
            .startDate(LocalDateTime.now().minusMonths(1))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .createdAt(LocalDateTime.now().minusMonths(1))
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ==================== Suspend Endpoint Tests ====================

    @Test
    @DisplayName("suspendSubscription - Success scenario with valid request")
    void suspendSubscription_Success() throws Exception {
        // Arrange
        InternalSuspendRequest request = InternalSuspendRequest.fromPaymentService(
            "Payment failed", testCorrelationId
        );

        Subscription suspendedSubscription = buildSuspendedSubscription();

        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(suspendedSubscription)
        ));

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.suspendSubscription(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSubscriptionId, response.getBody().id());
        assertEquals(SubscriptionStatus.SUSPENDED, response.getBody().status());
        assertEquals(testCorrelationId.toString(), response.getHeaders().getFirst("X-Correlation-ID"));

        // Verify interactions
        verify(securityFacade, times(1)).secureAccess(any(SecurityContext.class), any());
        verify(lifecycleService, times(1)).suspendSubscription(testSubscriptionId, "Payment failed");
    }

    @Test
    @DisplayName("suspendSubscription - Security failure scenario")
    void suspendSubscription_SecurityFailure() throws Exception {
        // Arrange
        InternalSuspendRequest request = InternalSuspendRequest.fromPaymentService(
            "Payment failed", testCorrelationId
        );

        mockSecurityFacadeFailure();

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.suspendSubscription(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(testCorrelationId.toString(), response.getHeaders().getFirst("X-Correlation-ID"));

        // Verify security facade was called but lifecycle service was not
        verify(securityFacade, times(1)).secureAccess(any(SecurityContext.class), any());
        verify(lifecycleService, never()).suspendSubscription(any(), any());
    }

    // ==================== Resume Endpoint Tests ====================

    @Test
    @DisplayName("resumeSubscription - Success scenario")
    void resumeSubscription_Success() throws Exception {
        // Arrange
        InternalResumeRequest request = InternalResumeRequest.fromPaymentService(testCorrelationId);

        Subscription resumedSubscription = buildTestSubscription();

        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(resumedSubscription)
        ));

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.resumeSubscription(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSubscriptionId, response.getBody().id());
        assertEquals(SubscriptionStatus.ACTIVE, response.getBody().status());
        assertEquals(testCorrelationId.toString(), response.getHeaders().getFirst("X-Correlation-ID"));

        // Verify interactions
        verify(securityFacade, times(1)).secureAccess(any(SecurityContext.class), any());
        verify(lifecycleService, times(1)).resumeSubscription(testSubscriptionId);
    }

    @Test
    @DisplayName("resumeSubscription - Security failure")
    void resumeSubscription_SecurityFailure() throws Exception {
        // Arrange
        InternalResumeRequest request = InternalResumeRequest.fromPaymentService(testCorrelationId);

        mockSecurityFacadeFailure();

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.resumeSubscription(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(securityFacade, times(1)).secureAccess(any(SecurityContext.class), any());
        verify(lifecycleService, never()).resumeSubscription(any());
    }

    // ==================== Change Tier Endpoint Tests ====================

    @Test
    @DisplayName("changeTier - Upgrade success")
    void changeTier_UpgradeSuccess() throws Exception {
        // Arrange
        InternalTierChangeRequest request = InternalTierChangeRequest.fromPaymentService(
            SubscriptionTier.AI_PREMIUM, testCorrelationId
        );

        Subscription upgradedSubscription = buildTestSubscription();
        upgradedSubscription = Subscription.builder()
            .id(upgradedSubscription.getId())
            .userId(upgradedSubscription.getUserId())
            .tier(SubscriptionTier.AI_PREMIUM)
            .status(upgradedSubscription.getStatus())
            .monthlyPrice(new BigDecimal("299.00"))
            .billingAmount(new BigDecimal("269.10"))
            .currency(upgradedSubscription.getCurrency())
            .startDate(upgradedSubscription.getStartDate())
            .nextBillingDate(upgradedSubscription.getNextBillingDate())
            .autoRenewal(upgradedSubscription.getAutoRenewal())
            .createdAt(upgradedSubscription.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .upgradedDate(LocalDateTime.now())
            .build();

        Subscription finalUpgradedSubscription = upgradedSubscription;
        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(finalUpgradedSubscription)
        ));

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.changeTier(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSubscriptionId, response.getBody().id());
        assertEquals(SubscriptionTier.AI_PREMIUM, response.getBody().tier());
        assertNotNull(response.getBody().upgradedDate());

        verify(securityFacade, times(1)).secureAccess(any(SecurityContext.class), any());
        verify(upgradeService, times(1)).upgradeSubscription(testSubscriptionId, SubscriptionTier.AI_PREMIUM);
    }

    @Test
    @DisplayName("changeTier - Security failure")
    void changeTier_SecurityFailure() throws Exception {
        // Arrange
        InternalTierChangeRequest request = InternalTierChangeRequest.fromPaymentService(
            SubscriptionTier.AI_PREMIUM, testCorrelationId
        );

        mockSecurityFacadeFailure();

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.changeTier(testSubscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(upgradeService, never()).upgradeSubscription(any(), any());
    }

    // ==================== Get Subscription Endpoint Tests ====================

    @Test
    @DisplayName("getSubscription - Found scenario")
    void getSubscription_Found() throws Exception {
        // Arrange
        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(testSubscription)
        ));

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.getSubscription(testSubscriptionId, testCorrelationId, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSubscriptionId, response.getBody().id());
        assertEquals(SubscriptionTier.PRO, response.getBody().tier());
        assertEquals(testCorrelationId.toString(), response.getHeaders().getFirst("X-Correlation-ID"));

        verify(lifecycleService, times(1)).getSubscription(testSubscriptionId);
    }

    @Test
    @DisplayName("getSubscription - Not found scenario")
    void getSubscription_NotFound() throws Exception {
        // Arrange
        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.failure("Subscription not found")
        ));

        // Act
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.getSubscription(testSubscriptionId, testCorrelationId, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(testCorrelationId.toString(), response.getHeaders().getFirst("X-Correlation-ID"));
    }

    // ==================== Get Active Subscriptions Endpoint Tests ====================

    @Test
    @DisplayName("getActiveSubscriptions - Multiple active subscriptions")
    void getActiveSubscriptions_MultipleActive() throws Exception {
        // Arrange
        Subscription sub1 = buildTestSubscription();
        Subscription sub2 = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .tier(SubscriptionTier.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .monthlyPrice(BigDecimal.ZERO)
            .billingAmount(BigDecimal.ZERO)
            .currency("USD")
            .startDate(LocalDateTime.now().minusMonths(2))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .autoRenewal(true)
            .createdAt(LocalDateTime.now().minusMonths(2))
            .updatedAt(LocalDateTime.now())
            .build();

        List<Subscription> activeSubscriptions = List.of(sub1, sub2);

        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(activeSubscriptions)
        ));

        // Act
        CompletableFuture<ResponseEntity<List<InternalSubscriptionResponse>>> future =
            controller.getActiveSubscriptions(testUserId, testCorrelationId, httpRequest);

        ResponseEntity<List<InternalSubscriptionResponse>> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().stream().allMatch(InternalSubscriptionResponse::isActive));

        verify(lifecycleService, times(1)).getActiveSubscriptions(testUserId);
    }

    @Test
    @DisplayName("getActiveSubscriptions - Empty list scenario")
    void getActiveSubscriptions_EmptyList() throws Exception {
        // Arrange
        mockSecurityFacadeSuccess(secCtx -> CompletableFuture.completedFuture(
            Result.success(List.of())
        ));

        // Act
        CompletableFuture<ResponseEntity<List<InternalSubscriptionResponse>>> future =
            controller.getActiveSubscriptions(testUserId, testCorrelationId, httpRequest);

        ResponseEntity<List<InternalSubscriptionResponse>> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("getActiveSubscriptions - Security failure")
    void getActiveSubscriptions_SecurityFailure() throws Exception {
        // Arrange
        mockSecurityFacadeFailure();

        // Act
        CompletableFuture<ResponseEntity<List<InternalSubscriptionResponse>>> future =
            controller.getActiveSubscriptions(testUserId, testCorrelationId, httpRequest);

        ResponseEntity<List<InternalSubscriptionResponse>> response = future.join();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(lifecycleService, never()).getActiveSubscriptions(any());
    }

    // ==================== Helper Methods ====================

    @SuppressWarnings("unchecked")
    private <T> void mockSecurityFacadeSuccess(Function<SecurityContext, CompletableFuture<Result<T, String>>> operation) {
        when(securityFacade.secureAccess(any(SecurityContext.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Function<SecurityContext, CompletableFuture<Result<T, String>>> op = invocation.getArgument(1);
                SecurityContext ctx = invocation.getArgument(0);
                return op.apply(ctx);
            });
    }

    @SuppressWarnings("unchecked")
    private void mockSecurityFacadeFailure() {
        when(securityFacade.secureAccess(any(SecurityContext.class), any(Function.class)))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure(new SecurityError("Invalid API key", SecurityErrorType.AUTHENTICATION_FAILED))
            ));
    }
}
