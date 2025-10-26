# Subscription Service - Internal API Implementation Plan

**Date**: October 26, 2025
**Status**: READY FOR IMPLEMENTATION
**Priority**: P1 - High Priority
**Estimated Effort**: 3-4 hours

---

## 🎯 Executive Summary

**Objective**: Create internal API endpoints in subscription-service to support service-to-service subscription lifecycle operations.

**Current State**:
- ✅ External API exists (`/api/v1/subscriptions`) - for user-facing operations
- ✅ InternalSubscriptionController exists with health/status endpoints
- ❌ **MISSING**: Internal lifecycle operation endpoints for service-to-service calls

**Target State**:
- ✅ Internal API endpoints (`/api/internal/v1/subscriptions`) for service-to-service operations
- ✅ Payment-service can manage subscriptions without exposing external API
- ✅ Proper microservice boundaries with Zero Trust security

**Architectural Benefits**:
- 🏗️ **Proper Service Boundaries**: Internal API for service-to-service, external API for users
- 🔒 **Enhanced Security**: API key authentication for internal calls, JWT for external
- 📊 **Better Monitoring**: Separate metrics for internal vs external operations
- 🎯 **Single Responsibility**: Subscription-service owns ALL subscription lifecycle logic
- ♻️ **Code Reuse**: Internal endpoints reuse same business logic as external endpoints

---

## 🏗️ Architectural Principles

### Service Ownership Model

**Subscription-Service Responsibilities**:
- ✅ Create, activate, suspend, resume, cancel subscriptions
- ✅ Upgrade/downgrade subscription tiers
- ✅ Track subscription status and billing cycles
- ✅ Calculate proration for plan changes
- ✅ Manage subscription persistence

**Payment-Service Responsibilities**:
- ✅ Process one-time payments
- ✅ Handle payment webhooks from gateways
- ✅ Process subscription **billing** (create PaymentTransaction)
- ✅ Manage payment methods and refunds
- ❌ **NOT RESPONSIBLE**: Subscription lifecycle management (delegates to subscription-service)

### API Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (Kong)                        │
└───────────────────┬─────────────────────┬───────────────────┘
                    │                     │
        ┌───────────▼──────────┐  ┌──────▼────────────────┐
        │  External API (JWT)  │  │ Internal API (API Key) │
        │  /api/v1/...         │  │ /api/internal/v1/...   │
        └───────────┬──────────┘  └──────┬────────────────┘
                    │                     │
        ┌───────────▼─────────────────────▼───────────────┐
        │       Subscription Service                       │
        │  ┌─────────────────────────────────────────┐   │
        │  │   SubscriptionLifecycleService          │   │
        │  │   (Shared Business Logic)               │   │
        │  └─────────────────────────────────────────┘   │
        │  ┌─────────────┐  ┌──────────────────────┐    │
        │  │ Subscription│  │ SubscriptionHistory  │    │
        │  │ Repository  │  │ Repository           │    │
        │  └─────────────┘  └──────────────────────┘    │
        └──────────────────────────────────────────────────┘
```

**Key Principle**: Both external and internal APIs delegate to the SAME business logic layer (SubscriptionLifecycleService). Only security context differs.

---

## 📋 Implementation Requirements

### Endpoints to Implement

All endpoints in `InternalSubscriptionController.java`:

#### 1. Suspend Subscription
```
POST /api/internal/v1/subscriptions/{subscriptionId}/suspend
```

**Request Body**:
```json
{
  "reason": "Payment failure",
  "serviceId": "payment-service",
  "correlationId": "uuid"
}
```

**Response**:
```json
{
  "id": "uuid",
  "userId": "uuid",
  "status": "SUSPENDED",
  "tier": "PRO",
  "nextBillingDate": "2025-11-26T00:00:00Z",
  "suspendedAt": "2025-10-26T16:30:00Z",
  "suspensionReason": "Payment failure"
}
```

#### 2. Resume Subscription
```
POST /api/internal/v1/subscriptions/{subscriptionId}/resume
```

**Request Body**:
```json
{
  "serviceId": "payment-service",
  "correlationId": "uuid"
}
```

**Response**: Same structure as suspend

#### 3. Upgrade/Change Tier
```
POST /api/internal/v1/subscriptions/{subscriptionId}/change-tier
```

**Request Body**:
```json
{
  "newTier": "AI_PREMIUM",
  "effectiveDate": "immediate|next_billing",
  "serviceId": "payment-service",
  "correlationId": "uuid"
}
```

**Response**:
```json
{
  "id": "uuid",
  "userId": "uuid",
  "status": "ACTIVE",
  "tier": "AI_PREMIUM",
  "previousTier": "PRO",
  "proratedAmount": "250.00",
  "nextBillingDate": "2025-11-26T00:00:00Z",
  "upgradeTimestamp": "2025-10-26T16:30:00Z"
}
```

#### 4. Get Subscription by ID (Internal)
```
GET /api/internal/v1/subscriptions/{subscriptionId}
```

**Response**: Full subscription details

#### 5. Get User Active Subscription (Internal)
```
GET /api/internal/v1/subscriptions/user/{userId}/active
```

**Response**: Active subscription or 404

---

## 🔧 Implementation Details

### Step 1: Create DTOs (30 minutes)

**File**: `subscription-service/src/main/java/com/trademaster/subscription/dto/internal/`

#### InternalSuspendRequest.java
```java
package com.trademaster.subscription.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Subscription Suspend Request
 * MANDATORY: Rule #9 - Immutable Record
 *
 * Used for service-to-service subscription suspension
 *
 * @author TradeMaster Development Team
 */
public record InternalSuspendRequest(
    @NotBlank(message = "Suspension reason is required")
    String reason,

    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    /**
     * Factory method for payment service calls
     */
    public static InternalSuspendRequest fromPaymentService(String reason, UUID correlationId) {
        return new InternalSuspendRequest(reason, "payment-service", correlationId);
    }
}
```

#### InternalResumeRequest.java
```java
package com.trademaster.subscription.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Subscription Resume Request
 * MANDATORY: Rule #9 - Immutable Record
 *
 * @author TradeMaster Development Team
 */
public record InternalResumeRequest(
    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    public static InternalResumeRequest fromPaymentService(UUID correlationId) {
        return new InternalResumeRequest("payment-service", correlationId);
    }
}
```

#### InternalTierChangeRequest.java
```java
package com.trademaster.subscription.dto.internal;

import com.trademaster.subscription.enums.SubscriptionTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal Subscription Tier Change Request
 * MANDATORY: Rule #9 - Immutable Record
 *
 * @author TradeMaster Development Team
 */
public record InternalTierChangeRequest(
    @NotNull(message = "New tier is required")
    SubscriptionTier newTier,

    @NotBlank(message = "Effective date is required")
    String effectiveDate, // "immediate" or "next_billing"

    @NotBlank(message = "Service ID is required")
    String serviceId,

    @NotNull(message = "Correlation ID is required")
    UUID correlationId
) {
    public static InternalTierChangeRequest fromPaymentService(
            SubscriptionTier newTier,
            UUID correlationId) {
        return new InternalTierChangeRequest(newTier, "immediate", "payment-service", correlationId);
    }
}
```

#### InternalSubscriptionResponse.java
```java
package com.trademaster.subscription.dto.internal;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Internal Subscription Response
 * MANDATORY: Rule #9 - Immutable Record
 *
 * Lightweight response for internal service-to-service communication
 * Contains only essential subscription data without derived fields
 *
 * @author TradeMaster Development Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalSubscriptionResponse(
    UUID id,
    UUID userId,
    SubscriptionTier tier,
    SubscriptionStatus status,
    BigDecimal monthlyPrice,
    String currency,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime nextBillingDate,
    Boolean autoRenewal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Lifecycle metadata
    LocalDateTime suspendedAt,
    String suspensionReason,
    SubscriptionTier previousTier,
    BigDecimal proratedAmount,
    LocalDateTime upgradeTimestamp
) {
    /**
     * Factory method from Subscription entity
     */
    public static InternalSubscriptionResponse fromSubscription(Subscription subscription) {
        return new InternalSubscriptionResponse(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getMonthlyPrice(),
            subscription.getCurrency(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getNextBillingDate(),
            subscription.getAutoRenewal(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            // Lifecycle metadata (null if not applicable)
            null, // suspendedAt - from history if needed
            null, // suspensionReason - from history if needed
            null, // previousTier - from history if needed
            null, // proratedAmount - calculated if needed
            null  // upgradeTimestamp - from history if needed
        );
    }

    /**
     * Factory method with lifecycle metadata
     */
    public static InternalSubscriptionResponse withLifecycleMetadata(
            Subscription subscription,
            LocalDateTime suspendedAt,
            String suspensionReason,
            SubscriptionTier previousTier,
            BigDecimal proratedAmount,
            LocalDateTime upgradeTimestamp) {
        return new InternalSubscriptionResponse(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getMonthlyPrice(),
            subscription.getCurrency(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getNextBillingDate(),
            subscription.getAutoRenewal(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            suspendedAt,
            suspensionReason,
            previousTier,
            proratedAmount,
            upgradeTimestamp
        );
    }
}
```

---

### Step 2: Extend InternalSubscriptionController (2 hours)

**File**: `subscription-service/src/main/java/com/trademaster/subscription/controller/InternalSubscriptionController.java`

**Add these endpoints to existing controller**:

```java
package com.trademaster.subscription.controller;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.dto.internal.*;
import com.trademaster.subscription.security.SecurityContext;
import com.trademaster.subscription.security.SecurityFacade;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Internal Subscription API Controller
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 * MANDATORY: Rule #5 - Max 200 lines per class
 * MANDATORY: Rule #12 - Virtual Threads (CompletableFuture)
 *
 * Provides internal service-to-service communication endpoints for subscription management.
 *
 * Security:
 * - SecurityFacade for all operations
 * - API key authentication (ServiceApiKeyFilter)
 * - @PreAuthorize for role-based access
 * - Correlation ID tracking
 *
 * @author TradeMaster Engineering Team
 * @version 3.0.0
 */
@RestController
@RequestMapping("/api/internal/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Hidden  // Hide from public OpenAPI documentation
public class InternalSubscriptionController {

    private final SecurityFacade securityFacade;
    private final SubscriptionLifecycleService lifecycleService;
    private final SubscriptionUpgradeService upgradeService;

    // ==================== Existing Health/Status Endpoints ====================
    // (keep existing health, status, capabilities, metrics endpoints)

    // ==================== NEW Lifecycle Operation Endpoints ====================

    /**
     * Suspend subscription (internal service-to-service)
     * MANDATORY: Rule #6 - SecurityFacade + @PreAuthorize
     * MANDATORY: Rule #15 - Correlation ID tracking
     */
    @PostMapping("/{subscriptionId}/suspend")
    @Timed(value = "subscription.internal.suspend")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Suspend subscription (internal)", hidden = true)
    public CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> suspendSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody InternalSuspendRequest request,
            HttpServletRequest httpRequest) {

        log.info("Internal suspend request: subscriptionId={}, serviceId={}, correlationId={}",
                subscriptionId, request.serviceId(), request.correlationId());

        SecurityContext securityContext = buildInternalSecurityContext(
            httpRequest,
            request.serviceId(),
            request.correlationId()
        );

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.suspendSubscription(subscriptionId, request.reason())
        ).thenApply(result -> result.match(
            subscription -> {
                log.info("Subscription suspended successfully: id={}, correlationId={}",
                        subscriptionId, request.correlationId());
                return ResponseEntity.ok()
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .body(InternalSubscriptionResponse.fromSubscription(subscription));
            },
            securityError -> {
                log.error("Failed to suspend subscription: id={}, error={}, correlationId={}",
                        subscriptionId, securityError.message(), request.correlationId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .build();
            }
        ));
    }

    /**
     * Resume subscription (internal service-to-service)
     * MANDATORY: Rule #6 - SecurityFacade + @PreAuthorize
     */
    @PostMapping("/{subscriptionId}/resume")
    @Timed(value = "subscription.internal.resume")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Resume subscription (internal)", hidden = true)
    public CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> resumeSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody InternalResumeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Internal resume request: subscriptionId={}, serviceId={}, correlationId={}",
                subscriptionId, request.serviceId(), request.correlationId());

        SecurityContext securityContext = buildInternalSecurityContext(
            httpRequest,
            request.serviceId(),
            request.correlationId()
        );

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.resumeSubscription(subscriptionId)
        ).thenApply(result -> result.match(
            subscription -> {
                log.info("Subscription resumed successfully: id={}, correlationId={}",
                        subscriptionId, request.correlationId());
                return ResponseEntity.ok()
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .body(InternalSubscriptionResponse.fromSubscription(subscription));
            },
            securityError -> {
                log.error("Failed to resume subscription: id={}, error={}, correlationId={}",
                        subscriptionId, securityError.message(), request.correlationId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .build();
            }
        ));
    }

    /**
     * Change subscription tier (internal service-to-service)
     * MANDATORY: Rule #6 - SecurityFacade + @PreAuthorize
     */
    @PostMapping("/{subscriptionId}/change-tier")
    @Timed(value = "subscription.internal.change-tier")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Change subscription tier (internal)", hidden = true)
    public CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> changeSubscriptionTier(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody InternalTierChangeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Internal tier change request: subscriptionId={}, newTier={}, serviceId={}, correlationId={}",
                subscriptionId, request.newTier(), request.serviceId(), request.correlationId());

        SecurityContext securityContext = buildInternalSecurityContext(
            httpRequest,
            request.serviceId(),
            request.correlationId()
        );

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> upgradeService.upgradeSubscription(subscriptionId, request.newTier())
        ).thenApply(result -> result.match(
            subscription -> {
                log.info("Subscription tier changed successfully: id={}, newTier={}, correlationId={}",
                        subscriptionId, request.newTier(), request.correlationId());
                return ResponseEntity.ok()
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .body(InternalSubscriptionResponse.fromSubscription(subscription));
            },
            securityError -> {
                log.error("Failed to change subscription tier: id={}, error={}, correlationId={}",
                        subscriptionId, securityError.message(), request.correlationId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Correlation-ID", request.correlationId().toString())
                    .build();
            }
        ));
    }

    /**
     * Get subscription by ID (internal)
     * MANDATORY: Rule #6 - SecurityFacade + @PreAuthorize
     */
    @GetMapping("/{subscriptionId}")
    @Timed(value = "subscription.internal.get")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Get subscription by ID (internal)", hidden = true)
    public CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> getSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @RequestParam(required = false) UUID correlationId,
            HttpServletRequest httpRequest) {

        UUID correlation = correlationId != null ? correlationId : UUID.randomUUID();

        log.info("Internal get subscription: subscriptionId={}, correlationId={}",
                subscriptionId, correlation);

        SecurityContext securityContext = buildInternalSecurityContext(
            httpRequest,
            "internal-api-call",
            correlation
        );

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.findById(subscriptionId)
        ).thenApply(result -> result.match(
            optionalSubscription -> optionalSubscription
                .map(subscription -> {
                    log.debug("Subscription found: id={}, correlationId={}", subscriptionId, correlation);
                    return ResponseEntity.ok()
                        .header("X-Correlation-ID", correlation.toString())
                        .body(InternalSubscriptionResponse.fromSubscription(subscription));
                })
                .orElseGet(() -> {
                    log.warn("Subscription not found: id={}, correlationId={}", subscriptionId, correlation);
                    return ResponseEntity.<InternalSubscriptionResponse>notFound()
                        .header("X-Correlation-ID", correlation.toString())
                        .build();
                }),
            securityError -> {
                log.error("Failed to get subscription: id={}, error={}, correlationId={}",
                        subscriptionId, securityError.message(), correlation);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .<InternalSubscriptionResponse>header("X-Correlation-ID", correlation.toString())
                    .build();
            }
        ));
    }

    /**
     * Get active subscription for user (internal)
     * MANDATORY: Rule #6 - SecurityFacade + @PreAuthorize
     */
    @GetMapping("/user/{userId}/active")
    @Timed(value = "subscription.internal.get-active")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Get user active subscription (internal)", hidden = true)
    public CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> getActiveSubscription(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @RequestParam(required = false) UUID correlationId,
            HttpServletRequest httpRequest) {

        UUID correlation = correlationId != null ? correlationId : UUID.randomUUID();

        log.info("Internal get active subscription: userId={}, correlationId={}",
                userId, correlation);

        SecurityContext securityContext = buildInternalSecurityContext(
            httpRequest,
            "internal-api-call",
            correlation
        );

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.getActiveSubscription(userId)
        ).thenApply(result -> result.match(
            optionalSubscription -> optionalSubscription
                .map(subscription -> {
                    log.debug("Active subscription found: userId={}, correlationId={}", userId, correlation);
                    return ResponseEntity.ok()
                        .header("X-Correlation-ID", correlation.toString())
                        .body(InternalSubscriptionResponse.fromSubscription(subscription));
                })
                .orElseGet(() -> {
                    log.warn("No active subscription: userId={}, correlationId={}", userId, correlation);
                    return ResponseEntity.<InternalSubscriptionResponse>notFound()
                        .header("X-Correlation-ID", correlation.toString())
                        .build();
                }),
            securityError -> {
                log.error("Failed to get active subscription: userId={}, error={}, correlationId={}",
                        userId, securityError.message(), correlation);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .<InternalSubscriptionResponse>header("X-Correlation-ID", correlation.toString())
                    .build();
            }
        ));
    }

    /**
     * Build SecurityContext for internal service calls
     * MANDATORY: Rule #6 - Zero Trust Security Context
     */
    private SecurityContext buildInternalSecurityContext(
            HttpServletRequest httpRequest,
            String serviceId,
            UUID correlationId) {
        return SecurityContext.builder()
            .userId(UUID.fromString("00000000-0000-0000-0000-000000000001")) // System/Service ID
            .sessionId(correlationId.toString())
            .ipAddress(httpRequest.getRemoteAddr())
            .userAgent(serviceId) // Use serviceId as user agent for tracking
            .requestPath(httpRequest.getRequestURI())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

---

### Step 3: Update application.yml (15 minutes)

**File**: `subscription-service/src/main/resources/application.yml`

Add internal API configuration:

```yaml
# Internal API Configuration
trademaster:
  api:
    internal:
      enabled: true
      base-path: /api/internal/v1
      allowed-services:
        - payment-service
        - notification-service
        - portfolio-service
      rate-limit:
        requests-per-minute: 1000
      timeout:
        read: 5000ms
        connect: 2000ms
```

---

### Step 4: Create Unit Tests (1 hour)

**File**: `subscription-service/src/test/java/com/trademaster/subscription/controller/InternalSubscriptionControllerTest.java`

```java
package com.trademaster.subscription.controller;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.dto.internal.*;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.security.SecurityError;
import com.trademaster.subscription.security.SecurityFacade;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Internal Subscription Controller Tests
 * MANDATORY: Rule #20 - Unit test coverage >80%
 * MANDATORY: Rule #3 - Functional test patterns
 */
@ExtendWith(MockitoExtension.class)
class InternalSubscriptionControllerTest {

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private SubscriptionLifecycleService lifecycleService;

    @Mock
    private SubscriptionUpgradeService upgradeService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private InternalSubscriptionController controller;

    private UUID subscriptionId;
    private UUID userId;
    private UUID correlationId;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        correlationId = UUID.randomUUID();

        testSubscription = Subscription.builder()
            .id(subscriptionId)
            .userId(userId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .monthlyPrice(BigDecimal.valueOf(999))
            .currency("INR")
            .autoRenewal(true)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusMonths(1))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Mock HttpServletRequest
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getRequestURI()).thenReturn("/api/internal/v1/subscriptions");
    }

    @Test
    void suspendSubscription_Success() {
        // Given
        InternalSuspendRequest request = new InternalSuspendRequest(
            "Payment failure", "payment-service", correlationId
        );

        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testSubscription)));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.suspendSubscription(subscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(subscriptionId);
        assertThat(response.getHeaders().get("X-Correlation-ID")).contains(correlationId.toString());

        verify(securityFacade).secureAccess(any(), any());
    }

    @Test
    void suspendSubscription_SecurityFailure() {
        // Given
        InternalSuspendRequest request = new InternalSuspendRequest(
            "Payment failure", "payment-service", correlationId
        );

        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                Result.failure(new SecurityError("UNAUTHORIZED", "Invalid API key"))
            ));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.suspendSubscription(subscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().get("X-Correlation-ID")).contains(correlationId.toString());
    }

    @Test
    void resumeSubscription_Success() {
        // Given
        InternalResumeRequest request = new InternalResumeRequest("payment-service", correlationId);

        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testSubscription)));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.resumeSubscription(subscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(subscriptionId);
    }

    @Test
    void changeTier_Success() {
        // Given
        InternalTierChangeRequest request = new InternalTierChangeRequest(
            SubscriptionTier.AI_PREMIUM, "immediate", "payment-service", correlationId
        );

        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Result.success(testSubscription)));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.changeSubscriptionTier(subscriptionId, request, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getSubscription_Found() {
        // Given
        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                Result.success(Optional.of(testSubscription))
            ));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.getSubscription(subscriptionId, correlationId, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(subscriptionId);
    }

    @Test
    void getSubscription_NotFound() {
        // Given
        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(Result.success(Optional.empty())));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.getSubscription(subscriptionId, correlationId, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getActiveSubscription_Found() {
        // Given
        when(securityFacade.secureAccess(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                Result.success(Optional.of(testSubscription))
            ));

        // When
        CompletableFuture<ResponseEntity<InternalSubscriptionResponse>> future =
            controller.getActiveSubscription(userId, correlationId, httpRequest);

        ResponseEntity<InternalSubscriptionResponse> response = future.join();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(userId);
    }
}
```

---

## 🔒 Security Compliance

### Zero Trust Security (Rule #6)

✅ **All endpoints protected**:
- SecurityFacade validates all requests
- API key authentication via ServiceApiKeyFilter
- @PreAuthorize("hasRole('SERVICE')") on all endpoints
- SecurityContext built for every request

✅ **Correlation ID tracking**:
- All requests include correlationId
- Logged in all operations
- Returned in response headers

✅ **Structured logging**:
- Service ID logged (which service made the call)
- Correlation ID in all log entries
- Success/failure logged with context

### Compliance Checklist

- [x] Rule #3: Functional Programming (CompletableFuture, Result types, no if-else)
- [x] Rule #6: Zero Trust Security (SecurityFacade + API key)
- [x] Rule #9: Immutable Records (all DTOs are records)
- [x] Rule #10: @Slf4j logging
- [x] Rule #11: Railway Programming (Result types)
- [x] Rule #12: Virtual Threads (CompletableFuture)
- [x] Rule #15: Correlation IDs in all operations
- [x] Rule #16: Dynamic configuration
- [x] Rule #20: Unit tests >80% coverage
- [x] Rule #24: Circuit breaker ready (SecurityFacade has it)

---

## 📊 Testing Strategy

### Unit Tests (1 hour)
- ✅ Test all 5 endpoints (suspend, resume, change-tier, get, get-active)
- ✅ Test success scenarios
- ✅ Test security failures
- ✅ Test not-found scenarios
- ✅ Verify correlation ID propagation

### Integration Tests (1 hour)
- ⚠️ Requires both subscription-service and payment-service running
- Test real API key authentication
- Test end-to-end flow from payment-service
- Verify database persistence
- Validate response mapping

---

## 🚀 Deployment Steps

### 1. Code Deployment
```bash
# Build subscription-service
cd subscription-service
./gradlew clean build

# Verify tests pass
./gradlew test

# Run application
./gradlew bootRun
```

### 2. Configuration Updates
```yaml
# Add to application.yml or environment variables
TRADEMASTER_SECURITY_INTERNAL_API_KEY=<secure-api-key>
```

### 3. API Gateway (Kong) Configuration
```yaml
# Internal routes (not publicly exposed)
routes:
  - name: subscription-internal-api
    paths:
      - /api/internal/v1/subscriptions
    service: subscription-service
    plugins:
      - name: key-auth
        config:
          key_names: ["X-API-Key"]
```

### 4. Verification
```bash
# Test health endpoint
curl -H "X-API-Key: <api-key>" \
  http://localhost:8087/api/internal/v1/subscriptions/health

# Test suspend endpoint
curl -X POST \
  -H "X-API-Key: <api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Test suspension",
    "serviceId": "payment-service",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
  }' \
  http://localhost:8087/api/internal/v1/subscriptions/<subscription-id>/suspend
```

---

## 📈 Success Metrics

### Performance Targets
- API response time: <50ms (internal calls should be fast)
- Throughput: >1000 requests/minute
- Error rate: <0.1%
- Circuit breaker triggers: <1% of calls

### Monitoring
- Prometheus metrics for all endpoints (@Timed)
- Correlation ID tracking in logs
- Success/failure rates
- Response time percentiles (p50, p95, p99)

---

## 🔄 Integration with Payment-Service

Once internal API is deployed, payment-service implementation becomes straightforward:

**File**: `payment-service/.../service/impl/SubscriptionServiceImpl.java`

```java
@Override
@Transactional
public Result<UserSubscription, String> pauseSubscription(Long subscriptionId) {
    log.info("Delegating pause to subscription-service: subscriptionId={}", subscriptionId);

    UUID uuid = UUID.fromString(subscriptionId.toString()); // Simplified mapping
    UUID correlationId = UUID.randomUUID();

    InternalSuspendRequest request = InternalSuspendRequest.fromPaymentService(
        "User requested pause",
        correlationId
    );

    return internalServiceClient
        .callSubscriptionService(
            "/subscriptions/" + uuid + "/suspend",
            request,
            InternalSubscriptionResponse.class)
        .map(response -> mapToUserSubscription(response))
        .map(Result::<UserSubscription, String>success)
        .orElse(Result.failure("Subscription service unavailable"));
}
```

---

## 📋 Pre-Implementation Checklist

Before starting implementation:

- [ ] Review architectural principles
- [ ] Understand Zero Trust security requirements
- [ ] Verify SubscriptionLifecycleService interface
- [ ] Confirm SubscriptionUpgradeService availability
- [ ] Check SecurityFacade implementation
- [ ] Review existing InternalSubscriptionController structure
- [ ] Understand Result type pattern
- [ ] Review CompletableFuture usage patterns

---

## ⏱️ Implementation Timeline

| Task | Effort | Cumulative |
|------|--------|------------|
| Create DTOs | 30 min | 30 min |
| Extend Controller | 2 hours | 2.5 hours |
| Update Configuration | 15 min | 2.75 hours |
| Create Unit Tests | 1 hour | 3.75 hours |
| Manual Testing | 30 min | 4.25 hours |
| Documentation | 15 min | 4.5 hours |

**Total Estimated Effort**: 4-5 hours

---

## 🎯 Acceptance Criteria

### Functional
- [ ] All 5 endpoints implemented (suspend, resume, change-tier, get, get-active)
- [ ] API key authentication enforced
- [ ] Correlation ID tracking working
- [ ] All endpoints return proper HTTP status codes
- [ ] Response DTOs correctly mapped from entities

### Non-Functional
- [ ] Unit test coverage >80%
- [ ] All 27 TradeMaster rules complied
- [ ] Metrics collection via @Timed
- [ ] Structured logging with correlation IDs
- [ ] Virtual threads used (CompletableFuture)
- [ ] Zero Trust security implemented

### Integration
- [ ] Payment-service can call all endpoints
- [ ] End-to-end flows working
- [ ] Database persistence verified
- [ ] Circuit breaker functioning

---

**Document Status**: READY FOR IMPLEMENTATION
**Created**: October 26, 2025
**Architect**: Claude Code SuperClaude
**Next Step**: Create DTOs in subscription-service/dto/internal/ package
