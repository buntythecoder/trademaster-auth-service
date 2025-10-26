# Subscription Lifecycle Implementation Plan
## Payment Service - Pause/Resume/ChangePlan Features

**Created**: October 26, 2025
**Status**: Architectural Decision Required
**Priority**: P1 - High Priority

---

## 🎯 Problem Statement

Three subscription lifecycle methods in `SubscriptionServiceImpl` return explicit failures:

```java
// Current Implementation (Lines 122-152)
pauseSubscription(Long subscriptionId)      → Result.failure("Subscription pause feature not available")
resumeSubscription(Long subscriptionId)     → Result.failure("Subscription resume feature not available")
changeSubscriptionPlan(Long, String)        → Result.failure("Subscription plan change feature not available")
```

**Root Cause**: Payment-service has **NO UserSubscriptionRepository** - cannot persist subscription state changes.

---

## 🏗️ Architectural Analysis

### Current Repository Inventory

**Payment Service Repositories**:
- ✅ PaymentTransactionRepository
- ✅ RefundRepository
- ✅ SubscriptionPlanRepository (subscription plans only, not user subscriptions)
- ✅ UserPaymentMethodRepository
- ✅ WebhookLogRepository
- ✅ PaymentEventRepository
- ❌ **MISSING**: UserSubscriptionRepository

**Subscription Service** (separate service):
- ✅ SubscriptionRepository (found at subscription-service/src/main/java/...)

### Service Responsibilities

**Payment Service** (current scope):
- Process one-time payments
- Process refunds
- Handle payment webhooks from gateways
- Process subscription **billing** (PaymentTransaction creation)
- Manage payment methods

**Subscription Service** (separate microservice):
- Manage subscription lifecycle (create, pause, resume, cancel)
- Track subscription status and billing cycles
- Handle subscription plan changes
- Calculate proration for plan changes

---

## 🚦 Implementation Options

### Option 1: Call Subscription Service API (RECOMMENDED) ✅

**Approach**: Payment service delegates to subscription-service via internal API

**Pros**:
- ✅ Maintains microservice boundaries
- ✅ Single source of truth for subscription data
- ✅ Reuses InternalServiceClient infrastructure
- ✅ No database schema changes
- ✅ Follows Zero Trust security model

**Cons**:
- ⚠️ Network call overhead
- ⚠️ Requires subscription-service endpoints to exist
- ⚠️ Cross-service dependency

**Implementation Steps**:

#### 1.1 Add Subscription Service Configuration
```java
// application.yml
trademaster:
  services:
    subscription-service-url: ${SUBSCRIPTION_SERVICE_URL:http://localhost:8087}
```

#### 1.2 Extend InternalServiceClient
```java
// Add to InternalServiceClient.java
@Value("${trademaster.services.subscription-service-url:http://localhost:8087}")
private String subscriptionServiceUrl;

public <T, R> Optional<R> callSubscriptionService(
        String endpoint,
        T requestBody,
        Class<R> responseType) {
    String correlationId = UUID.randomUUID().toString();
    String url = subscriptionServiceUrl + "/api/internal/v1" + endpoint;

    log.debug("Calling Subscription Service: {} | correlation_id={}", url, correlationId);

    try {
        R response = restClient.post()
            .uri(url)
            .header("X-API-Key", apiKey)
            .header("X-Correlation-ID", correlationId)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
            .retrieve()
            .body(responseType);

        return Optional.ofNullable(response);
    } catch (Exception e) {
        log.error("Subscription Service call failed: {} | correlation_id={}",
                 e.getMessage(), correlationId, e);
        return Optional.empty();
    }
}
```

#### 1.3 Implement Subscription Methods
```java
// SubscriptionServiceImpl.java updates

@Override
@Transactional
public Result<UserSubscription, String> pauseSubscription(Long subscriptionId) {
    log.info("Pausing subscription via subscription-service: subscriptionId={}", subscriptionId);

    return callSubscriptionServiceInternal("/subscriptions/" + subscriptionId + "/pause", null)
        .map(response -> (UserSubscription) response)
        .map(Result::<UserSubscription, String>success)
        .orElse(Result.failure("Failed to pause subscription - subscription-service unavailable"));
}

@Override
@Transactional
public Result<UserSubscription, String> resumeSubscription(Long subscriptionId) {
    log.info("Resuming subscription via subscription-service: subscriptionId={}", subscriptionId);

    return callSubscriptionServiceInternal("/subscriptions/" + subscriptionId + "/resume", null)
        .map(response -> (UserSubscription) response)
        .map(Result::<UserSubscription, String>success)
        .orElse(Result.failure("Failed to resume subscription - subscription-service unavailable"));
}

@Override
@Transactional
public Result<UserSubscription, String> changeSubscriptionPlan(Long subscriptionId, String newPlanId) {
    log.info("Changing subscription plan via subscription-service: subscriptionId={}, newPlanId={}",
            subscriptionId, newPlanId);

    ChangePlanRequest request = new ChangePlanRequest(newPlanId);

    return callSubscriptionServiceInternal("/subscriptions/" + subscriptionId + "/change-plan", request)
        .map(response -> (UserSubscription) response)
        .map(Result::<UserSubscription, String>success)
        .orElse(Result.failure("Failed to change subscription plan - subscription-service unavailable"));
}

private Optional<Object> callSubscriptionServiceInternal(String endpoint, Object request) {
    return internalServiceClient.callSubscriptionService(endpoint, request, Object.class);
}
```

**Estimated Effort**: 4-6 hours
**Dependencies**: Subscription-service internal API endpoints must exist

---

### Option 2: Create UserSubscriptionRepository (NOT RECOMMENDED) ❌

**Approach**: Add subscription management directly to payment-service

**Pros**:
- ✅ No cross-service calls
- ✅ Lower latency

**Cons**:
- ❌ Violates microservice boundaries
- ❌ Duplicates subscription data (data inconsistency risk)
- ❌ Requires database schema changes
- ❌ Creates tight coupling between services
- ❌ Conflicts with existing subscription-service architecture

**Why Not Recommended**: TradeMaster already has a dedicated subscription-service. Adding subscription management to payment-service creates:
- Data duplication
- Inconsistency risk
- Unclear service boundaries
- Maintenance complexity

---

### Option 3: Remove These Methods (ALTERNATIVE) ⚠️

**Approach**: Delete pause/resume/changePlan from SubscriptionService interface

**Rationale**: If payment-service only processes payments (not subscription lifecycle), these methods don't belong here.

**Pros**:
- ✅ Clear service boundaries
- ✅ No implementation complexity

**Cons**:
- ❌ Breaks existing interface contract
- ❌ May be needed for gateway integration (Stripe/Razorpay subscriptions)

**When to Use**: If controllers/clients don't actually call these methods

---

## 🎯 RECOMMENDED APPROACH: Option 1

**Implementation Plan**:

### Phase 1: Verify Subscription Service Endpoints (1 hour)
1. Check if subscription-service has internal API endpoints:
   - `POST /api/internal/v1/subscriptions/{id}/pause`
   - `POST /api/internal/v1/subscriptions/{id}/resume`
   - `POST /api/internal/v1/subscriptions/{id}/change-plan`

2. Verify endpoint contracts (request/response DTOs)

3. Test endpoints with API key authentication

### Phase 2: Extend InternalServiceClient (1 hour)
1. Add `subscription-service-url` configuration
2. Add `callSubscriptionService()` method
3. Add circuit breaker for resilience (Rule 24)
4. Add correlation ID logging

### Phase 3: Implement Subscription Methods (2 hours)
1. Inject InternalServiceClient into SubscriptionServiceImpl
2. Implement pauseSubscription with service call
3. Implement resumeSubscription with service call
4. Implement changeSubscriptionPlan with service call
5. Add structured logging with correlation IDs
6. Handle failures gracefully with Result types

### Phase 4: Testing (1-2 hours)
1. Create unit tests with mocked InternalServiceClient
2. Create integration tests (Docker required)
3. Test circuit breaker behavior
4. Verify correlation ID propagation

### Phase 5: Documentation (30 min)
1. Update API documentation
2. Document cross-service communication
3. Add troubleshooting guide

**Total Estimated Effort**: 5-6 hours

---

## 🔐 Security Considerations

**API Key Authentication** (Rule 6):
- ✅ Internal service calls use X-API-Key header
- ✅ Correlation IDs for distributed tracing
- ✅ InternalServiceClient already implements Zero Trust

**Circuit Breaker** (Rule 24):
- ⚠️ TODO: Add @CircuitBreaker to InternalServiceClient methods
- ⚠️ TODO: Configure Resilience4j for subscription-service calls

---

## 📋 Acceptance Criteria

### Functional Requirements
- [  ] pauseSubscription calls subscription-service API
- [  ] resumeSubscription calls subscription-service API
- [  ] changeSubscriptionPlan calls subscription-service API
- [  ] All methods return Result<UserSubscription, String>
- [  ] Failures handled gracefully with meaningful error messages
- [  ] Network failures don't crash the service

### Non-Functional Requirements
- [  ] Correlation IDs propagated across services
- [  ] Structured logging with all required fields
- [  ] Circuit breaker protection (Resilience4j)
- [  ] Response time <200ms (excluding network latency)
- [  ] API key authentication on all calls

### Testing Requirements
- [  ] Unit tests with mocked InternalServiceClient (>80% coverage)
- [  ] Integration tests with TestContainers
- [  ] Circuit breaker behavior verified
- [  ] Error scenarios tested (service unavailable, timeout, invalid response)

---

## 🚨 Blockers & Dependencies

### Blockers
1. **Subscription Service Endpoints** - Must exist before implementation
2. **Endpoint Contracts** - Must match UserSubscription DTO
3. **API Key Configuration** - Must be configured in both services

### Dependencies
- Subscription-service internal API availability
- InternalServiceClient (already exists)
- API key authentication configured
- Kong API Gateway routing (for internal calls)

---

## 📊 Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Subscription-service unavailable | Medium | High | Circuit breaker + fallback response |
| Network latency | Low | Medium | Async processing + timeout configuration |
| Data inconsistency | Low | High | Use subscription-service as single source of truth |
| API contract changes | Medium | Medium | Versioned API endpoints |

---

## 🔄 Alternative: Gateway-Level Subscription Management

**If using Stripe/Razorpay subscriptions**:

Some payment gateways manage subscriptions directly. In this case:
- Call gateway API to pause/resume
- Store minimal subscription state locally
- Sync status from gateway webhooks

**Pros**:
- ✅ Gateway handles complexity
- ✅ No cross-service calls

**Cons**:
- ❌ Gateway lock-in
- ❌ Limited customization
- ❌ Multiple gateways = multiple implementations

---

## 📝 Next Steps

**Immediate** (Choose One):
1. ✅ **RECOMMENDED**: Verify subscription-service endpoints exist → Implement Option 1
2. ⚠️ **ALTERNATIVE**: Consult architect about service boundaries → Consider Option 3
3. ❌ **NOT RECOMMENDED**: Do NOT implement Option 2 (violates architecture)

**After Decision**:
1. Update this document with chosen approach
2. Create implementation tasks
3. Assign ownership
4. Set timeline

---

**Status**: Awaiting architectural decision
**Recommendation**: Option 1 (Call Subscription Service API)
**Created By**: Claude Code SuperClaude
**Date**: October 26, 2025
