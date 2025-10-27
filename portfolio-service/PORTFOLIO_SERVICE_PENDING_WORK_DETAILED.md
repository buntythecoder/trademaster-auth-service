# Portfolio Service - Detailed Pending Work & Task List

**Date Created**: 2025-10-27
**Service**: portfolio-service
**Current Status**: 85% Complete (23/27 rules passing)
**Goal**: 100% Compliance - Production Ready

---

## Executive Summary

**Overall Progress**: 85% → Target: 100%
**Blocking Issues**: 2 (Rules #7, #24)
**High Priority Issues**: 2 (Rules #3, #5)
**Estimated Total Time**: 15-20 hours
**Target Completion**: Week 3

---

## CRITICAL PRIORITY 1 - BLOCKING ISSUES (Must Complete First)

### Task 1.1: Remove All TODO/Placeholder Comments (Rule #7 Violation)

**Status**: ❌ BLOCKING - Zero Tolerance Policy
**Priority**: CRITICAL
**Estimated Time**: 2 hours
**Service**: portfolio-service
**Assigned To**: Backend Team

**Files to Modify**:
1. `portfolio-service/src/main/java/com/trademaster/portfolio/controller/PortfolioController.java`

**Specific Violations**:

**Location 1**: PortfolioController.java:244
```java
// LINE 244: Remove this comment
// Note: Benchmark metrics require market data integration (Phase 3 enhancement)
```

**Location 2**: PortfolioController.java:263-276 - Placeholder benchmark values
```java
// LINES 263-276: Remove placeholder values or implement feature
return new PerformanceComparison(
    performance.portfolioId(),
    benchmark,
    benchmark + " Index",
    start,
    end,
    performance.totalReturn(),
    BigDecimal.ZERO, // Benchmark metrics require market data service
    performance.totalReturn(),
    performance.volatility(),
    BigDecimal.ZERO, // ← ALL THESE ZEROS ARE PLACEHOLDERS
    // ... more BigDecimal.ZERO placeholders
);
```

**Decision Required**:
- **Option A**: Remove entire benchmark comparison feature (2 hours)
- **Option B**: Implement market data integration for benchmarks (8-10 hours)

**Recommended Solution**: **Option A - Remove Feature**
- Remove `getPortfolioPerformance()` method (lines 213-234)
- Remove `createPerformanceComparison()` method (lines 252-284)
- Remove `convertToInstant()` method (lines 295-299)
- Update API documentation
- No breaking changes (feature not used in production)

**Verification Steps**:
```bash
cd portfolio-service
./gradlew clean build
grep -r "Phase 3" src/
grep -r "TODO" src/
grep -r "FIXME" src/
# Should return no results
```

**Success Criteria**:
- ✅ Zero TODO/FIXME/placeholder comments
- ✅ Zero "Phase X" references
- ✅ Clean compilation with no warnings
- ✅ All existing tests still pass

---

### Task 1.2: Fix Test Compilation Errors (Rule #24 Violation)

**Status**: ❌ BLOCKING - Tests Cannot Run
**Priority**: CRITICAL
**Estimated Time**: 3-4 hours
**Service**: portfolio-service
**Assigned To**: QA + Backend Team

**Current State**: 85 compilation errors in 3 test files

---

#### Task 1.2.1: Fix MCPPortfolioServerTest.java

**Estimated Time**: 30 minutes
**Error Type**: Import/Class Not Found

**Error Details**:
```
Error: Cannot resolve symbol 'RiskMetrics'
Location: MCPPortfolioServerTest.java
Root Cause: RiskMetrics class location/naming mismatch
```

**Investigation Steps**:
```bash
cd portfolio-service
find src -name "*Risk*.java" | grep -i metrics
grep -r "class RiskMetrics" src/
```

**Possible Solutions**:
1. RiskMetrics renamed → Update import statement
2. RiskMetrics moved → Update package import
3. RiskMetrics deleted → Remove test or mock class

**Verification**:
```bash
cd portfolio-service
./gradlew test --tests "MCPPortfolioServerTest" --console=plain
# Should show: BUILD SUCCESSFUL
```

---

#### Task 1.2.2: Fix PortfolioServiceIntegrationTest.java

**Estimated Time**: 2 hours
**Error Type**: Interface Mismatch

**Error Details**:
```
Error: Wrong service interface used
Location: PortfolioServiceIntegrationTest.java
Root Cause: Test uses FunctionalPortfolioService instead of PortfolioService
Expected: PortfolioService interface
Actual: FunctionalPortfolioService (non-existent or renamed)
```

**Investigation Steps**:
```bash
cd portfolio-service
grep -r "FunctionalPortfolioService" src/
grep -r "class.*PortfolioService" src/main/java/
```

**Solution**:
1. Replace all `FunctionalPortfolioService` references with `PortfolioService`
2. Update @Autowired field type
3. Update method signatures if needed
4. Fix import statements

**Changes Required**:
```java
// BEFORE (wrong interface)
@Autowired
private FunctionalPortfolioService functionalPortfolioService;

// AFTER (correct interface)
@Autowired
private PortfolioService portfolioService;
```

**Verification**:
```bash
cd portfolio-service
./gradlew test --tests "PortfolioServiceIntegrationTest" --console=plain
# Should show: BUILD SUCCESSFUL
```

---

#### Task 1.2.3: Fix PortfolioTaskScopeTest.java

**Estimated Time**: 15 minutes
**Error Type**: Minor Issues

**Error Details**:
```
Error: Method signature mismatch or missing imports
Location: PortfolioTaskScopeTest.java
Root Cause: API changes not reflected in tests
```

**Investigation Steps**:
```bash
cd portfolio-service
./gradlew test --tests "PortfolioTaskScopeTest" --console=plain 2>&1 | head -50
# Read actual compilation errors
```

**Likely Fixes**:
1. Update method signatures
2. Fix import statements
3. Update mock configurations

**Verification**:
```bash
cd portfolio-service
./gradlew test --tests "PortfolioTaskScopeTest" --console=plain
# Should show: BUILD SUCCESSFUL
```

---

#### Task 1.2.4: Run Full Test Suite

**Estimated Time**: 30 minutes
**Prerequisites**: Tasks 1.2.1, 1.2.2, 1.2.3 completed

**Commands**:
```bash
cd portfolio-service
./gradlew clean test --console=plain 2>&1 | tee test-results.log
grep "BUILD SUCCESSFUL" test-results.log
grep "tests completed" test-results.log
```

**Success Criteria**:
- ✅ All tests compile successfully
- ✅ Zero compilation errors
- ✅ Test suite runs to completion
- ✅ >80% unit test coverage
- ✅ >70% integration test coverage

**Generate Coverage Report**:
```bash
cd portfolio-service
./gradlew test jacocoTestReport
cat build/reports/jacoco/test/html/index.html
# Review coverage percentages
```

---

## HIGH PRIORITY 2 - CODE QUALITY IMPROVEMENTS

### Task 2.1: Convert to Functional Error Handling (Rule #3 Improvement)

**Status**: ⚠️ HIGH PRIORITY
**Priority**: HIGH
**Estimated Time**: 6 hours
**Service**: portfolio-service
**Assigned To**: Backend Team

**Current State**: 25% of code uses try-catch blocks (should be 0%)

---

#### Task 2.1.1: Refactor createPortfolio() Method

**File**: PortfolioServiceImpl.java
**Lines**: 90-107
**Estimated Time**: 2 hours

**Current Code (Lines 90-107)**:
```java
public Portfolio createPortfolio(Long userId, CreatePortfolioRequest request) {
    long startTime = System.currentTimeMillis();
    Timer.Sample metricsTimer = portfolioMetrics.startCreationTimer();

    try {
        portfolioLogger.setCorrelationId();
        portfolioLogger.setUserContext(userId);

        Portfolio savedPortfolio = executePortfolioCreation(userId, request, startTime, metricsTimer);

        return savedPortfolio;
    } catch (Exception e) {
        handlePortfolioCreationError(userId, startTime, e);
        throw new RuntimeException("Failed to create portfolio for user: " + userId, e);
    } finally {
        portfolioLogger.clearContext();
    }
}
```

**Target Code (Functional Pattern)**:
```java
public Result<Portfolio, PortfolioError> createPortfolio(Long userId, CreatePortfolioRequest request) {
    long startTime = System.currentTimeMillis();
    Timer.Sample metricsTimer = portfolioMetrics.startCreationTimer();

    return Result.ofTry(() -> {
        portfolioLogger.setCorrelationId();
        portfolioLogger.setUserContext(userId);
        return executePortfolioCreation(userId, request, startTime, metricsTimer);
    })
    .mapError(e -> {
        handlePortfolioCreationError(userId, startTime, e);
        return PortfolioError.CREATION_FAILED;
    })
    .onComplete(() -> portfolioLogger.clearContext());
}
```

**Changes Required**:
1. Change return type from `Portfolio` to `Result<Portfolio, PortfolioError>`
2. Replace try-catch with `Result.ofTry()`
3. Use `mapError()` for error handling
4. Use `onComplete()` for cleanup (finally block replacement)
5. Update all callers to handle Result type
6. Update PortfolioController to unwrap Result

**Impact Analysis**:
- PortfolioController.java: Update createPortfolio() endpoint (line 103)
- All tests using createPortfolio(): Update assertions

**Verification**:
```bash
cd portfolio-service
./gradlew clean build
./gradlew test --tests "*PortfolioService*" --console=plain
```

---

#### Task 2.1.2: Refactor updatePortfolio() Method

**File**: PortfolioServiceImpl.java
**Lines**: 235-265
**Estimated Time**: 2 hours

**Current Code Issues**:
- try-finally block (lines 239-263)
- Manual context management
- Exception throwing instead of Result types

**Target Pattern**: Same as Task 2.1.1 - use Result types

**Verification**:
```bash
./gradlew test --tests "*updatePortfolio*" --console=plain
```

---

#### Task 2.1.3: Refactor updateCashBalance() Method

**File**: PortfolioServiceImpl.java
**Lines**: 499-547
**Estimated Time**: 2 hours

**Current Code Issues**:
- try-finally block
- Manual exception handling
- Requires method extraction (see Task 2.2.1)

**Combined Effort**: Do this with Task 2.2.1 (method extraction)

---

#### Task 2.1.4: Remove if-else in PortfolioController

**File**: PortfolioController.java
**Line**: 344-346
**Estimated Time**: 30 minutes

**Current Code**:
```java
var result = riskService.assessTradeRisk(portfolioId, request);
return result.isSuccess()
    ? ResponseEntity.ok(convertAssessmentToAlerts(portfolioId, result.getOrThrow()))
    : ResponseEntity.badRequest().body(List.of());
```

**Target Code (Pattern Matching)**:
```java
return riskService.assessTradeRisk(portfolioId, request)
    .fold(
        error -> ResponseEntity.badRequest().body(List.of()),
        success -> ResponseEntity.ok(convertAssessmentToAlerts(portfolioId, success))
    );
```

**Verification**:
```bash
./gradlew test --tests "PortfolioControllerTest" --console=plain
```

---

### Task 2.2: Extract Long Methods (Rule #5 Improvement)

**Status**: ⚠️ MEDIUM PRIORITY
**Priority**: MEDIUM
**Estimated Time**: 3 hours
**Service**: portfolio-service

---

#### Task 2.2.1: Refactor updateCashBalance() Method

**File**: PortfolioServiceImpl.java
**Current Lines**: 499-547 (48 lines)
**Target**: 15 lines orchestration + extracted helpers
**Estimated Time**: 2 hours

**Current Method Structure**:
```java
public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount, String description) {
    // 48 lines of mixed concerns:
    // - Timer setup (3 lines)
    // - Context setup (3 lines)
    // - try block opening (1 line)
    // - Portfolio retrieval (2 lines)
    // - Balance calculation (2 lines)
    // - Validation logic (4 lines)
    // - Update logic (4 lines)
    // - Save (1 line)
    // - Metrics recording (3 lines)
    // - Transaction logging (9 lines)
    // - Event publishing (2 lines)
    // - Success logging (3 lines)
    // - Return (1 line)
    // - finally block (3 lines)
}
```

**Target Method Structure** (Apply Phase 6D Patterns):
```java
public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount, String description) {
    long startTime = System.currentTimeMillis();
    Timer.Sample cashUpdateTimer = portfolioMetrics.startCashUpdateTimer();
    portfolioLogger.setPortfolioContext(portfolioId);

    try {
        Portfolio updatedPortfolio = executeCashBalanceUpdate(portfolioId, amount);
        recordCashUpdateMetrics(portfolioId, amount, description, startTime, cashUpdateTimer, updatedPortfolio);
        return updatedPortfolio;
    } finally {
        portfolioLogger.clearContext();
    }
}

// Helper 1: Execute cash balance update with validation
private Portfolio executeCashBalanceUpdate(Long portfolioId, BigDecimal amount) {
    Portfolio portfolio = getPortfolioById(portfolioId);
    BigDecimal newBalance = calculateNewBalance(portfolio.getCashBalance(), amount);
    validateSufficientFunds(amount, newBalance);
    return applyCashBalanceUpdate(portfolio, amount, newBalance);
}

// Helper 2: Calculate new balance
private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount) {
    return currentBalance.add(amount);
}

// Helper 3: Validate sufficient funds
private void validateSufficientFunds(BigDecimal amount, BigDecimal newBalance) {
    Optional.of(amount.compareTo(BigDecimal.ZERO) < 0 && newBalance.compareTo(BigDecimal.ZERO) < 0)
        .filter(insufficientFunds -> !insufficientFunds)
        .orElseThrow(() -> new IllegalArgumentException("Insufficient cash balance for withdrawal"));
}

// Helper 4: Apply cash balance update
private Portfolio applyCashBalanceUpdate(Portfolio portfolio, BigDecimal amount, BigDecimal newBalance) {
    portfolio.setCashBalance(newBalance);
    portfolio.setTotalValue(portfolio.getTotalValue().add(amount));
    portfolio.setUpdatedAt(Instant.now());
    return portfolioRepository.save(portfolio);
}

// Helper 5: Record metrics, logging, and events
private void recordCashUpdateMetrics(Long portfolioId, BigDecimal amount, String description,
                                    long startTime, Timer.Sample timer, Portfolio updatedPortfolio) {
    long duration = System.currentTimeMillis() - startTime;
    portfolioMetrics.recordCashUpdateTime(timer);

    logCashTransaction(portfolioId, amount, duration);
    publishCashBalanceEvent(updatedPortfolio, amount, description);

    log.info("Cash balance updated for portfolio {}: new balance: {}",
        portfolioId, updatedPortfolio.getCashBalance());
}

// Helper 6: Log cash transaction
private void logCashTransaction(Long portfolioId, BigDecimal amount, long duration) {
    portfolioLogger.logTransactionCreated(
        portfolioId,
        amount.compareTo(BigDecimal.ZERO) > 0 ? "DEPOSIT" : "WITHDRAWAL",
        null, null, amount, null, duration
    );
}

// Helper 7: Publish cash balance event
private void publishCashBalanceEvent(Portfolio portfolio, BigDecimal amount, String description) {
    BigDecimal oldBalance = portfolio.getCashBalance().subtract(amount);
    eventPublisher.publishCashBalanceUpdatedEvent(
        portfolio, oldBalance, portfolio.getCashBalance(), description
    );
}
```

**Complexity Reduction**:
- **Before**: 48 lines, complexity ~12
- **After**: 15 lines orchestration, 7 helpers (each 3-8 lines, complexity 1-3)
- **Total Reduction**: 60% line reduction, 70% complexity reduction

**Verification**:
```bash
./gradlew test --tests "*cashBalance*" --console=plain
```

---

#### Task 2.2.2: Refactor getPortfoliosForUser() Method

**File**: PortfolioServiceImpl.java
**Current Lines**: 832-855 (23 lines)
**Target**: 12 lines orchestration + extracted helpers
**Estimated Time**: 1 hour

**Current Method**:
```java
public Page<PortfolioSummary> getPortfoliosForUser(Long userId, String status, Pageable pageable) {
    log.info("Getting portfolios for user: {} with status: {} (page: {}, size: {})",
        userId, status, pageable.getPageNumber(), pageable.getPageSize());

    // Use functional pattern matching for status-based repository selection
    Page<Portfolio> portfolioPage = Optional.ofNullable(status)
        .flatMap(s -> parsePortfolioStatus(s)
            .map(portfolioStatus -> portfolioRepository.findByUserIdAndStatusPageable(userId, portfolioStatus, pageable)))
        .orElseGet(() -> portfolioRepository.findAllByUserIdPageable(userId, pageable));

    // Transform portfolios to summaries using functional stream processing
    List<PortfolioSummary> summaries = portfolioPage.getContent().stream()
        .map(p -> getPortfolioSummary(p.getPortfolioId()))
        .toList();

    log.debug("Retrieved {} portfolios for user: {} with status: {}",
        summaries.size(), userId, status);

    return new org.springframework.data.domain.PageImpl<>(
        summaries,
        pageable,
        portfolioPage.getTotalElements()
    );
}
```

**Target Method** (Extracted Helpers):
```java
public Page<PortfolioSummary> getPortfoliosForUser(Long userId, String status, Pageable pageable) {
    log.info("Getting portfolios for user: {} with status: {}", userId, status);

    Page<Portfolio> portfolioPage = fetchPortfolioPage(userId, status, pageable);
    List<PortfolioSummary> summaries = convertToSummaries(portfolioPage);

    log.debug("Retrieved {} portfolios for user: {}", summaries.size(), userId);
    return createSummaryPage(summaries, pageable, portfolioPage.getTotalElements());
}

// Helper 1: Fetch portfolio page with optional status filter
private Page<Portfolio> fetchPortfolioPage(Long userId, String status, Pageable pageable) {
    return Optional.ofNullable(status)
        .flatMap(this::parsePortfolioStatus)
        .map(portfolioStatus -> portfolioRepository.findByUserIdAndStatusPageable(userId, portfolioStatus, pageable))
        .orElseGet(() -> portfolioRepository.findAllByUserIdPageable(userId, pageable));
}

// Helper 2: Convert portfolio page to summaries
private List<PortfolioSummary> convertToSummaries(Page<Portfolio> portfolioPage) {
    return portfolioPage.getContent().stream()
        .map(p -> getPortfolioSummary(p.getPortfolioId()))
        .toList();
}

// Helper 3: Create summary page
private Page<PortfolioSummary> createSummaryPage(List<PortfolioSummary> summaries,
                                                  Pageable pageable, long totalElements) {
    return new org.springframework.data.domain.PageImpl<>(summaries, pageable, totalElements);
}
```

**Complexity Reduction**:
- **Before**: 23 lines, complexity ~8
- **After**: 12 lines orchestration, 3 helpers (each 4-6 lines, complexity 2-3)
- **Total Reduction**: 48% line reduction, 65% complexity reduction

**Verification**:
```bash
./gradlew test --tests "*getPortfoliosForUser*" --console=plain
```

---

## MEDIUM PRIORITY 3 - VERIFICATION & DOCUMENTATION

### Task 3.1: Verify Test Coverage

**Status**: ⏳ PENDING
**Priority**: MEDIUM
**Estimated Time**: 2 hours
**Prerequisites**: Task 1.2 (Fix test compilation errors)

**Commands**:
```bash
cd portfolio-service

# Run tests with coverage
./gradlew clean test jacocoTestReport

# Generate HTML report
open build/reports/jacoco/test/html/index.html

# Check coverage thresholds
./gradlew jacocoTestCoverageVerification
```

**Coverage Targets**:
- ✅ Unit Tests: >80% line coverage
- ✅ Integration Tests: >70% line coverage
- ✅ Branch Coverage: >75%
- ✅ Critical Paths: 100% coverage

**Files to Review**:
1. `PortfolioServiceImpl.java` - Core service logic
2. `PortfolioController.java` - API endpoints
3. `InternalPortfolioController.java` - Internal APIs
4. `PortfolioRepository.java` - Data access

**If Coverage is Low**:
- Identify untested methods
- Add unit tests for each method
- Add integration tests for critical paths
- Target: 100% coverage on new code

---

### Task 3.2: Performance & Load Testing

**Status**: ⏳ PENDING
**Priority**: MEDIUM
**Estimated Time**: 4 hours
**Prerequisites**: All code changes completed

**Performance Targets** (from PortfolioServiceImpl.java:50-55):
```
Portfolio creation: <100ms
Valuation update: <50ms
Position retrieval: <25ms
Bulk operations: <200ms
Concurrent users: 10,000+
```

**Test Scenarios**:

**Scenario 1: Portfolio Creation Load Test**
```bash
# Create load test script
cat > load-test-create-portfolio.sh << 'EOF'
#!/bin/bash
for i in {1..1000}; do
  curl -X POST http://localhost:8083/api/v1/portfolios \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "userId": '$i',
      "portfolioName": "Test Portfolio '$i'",
      "portfolioType": "STANDARD",
      "initialCashBalance": 100000.00,
      "currency": "USD"
    }' &
done
wait
EOF

chmod +x load-test-create-portfolio.sh
./load-test-create-portfolio.sh
```

**Scenario 2: Valuation Update Performance**
```bash
# Test valuation update speed
time curl -X PUT http://localhost:8083/api/v1/portfolios/1/valuation \
  -H "Authorization: Bearer $JWT_TOKEN"
# Should complete in <50ms
```

**Scenario 3: Concurrent User Simulation**
```bash
# Use Apache Bench for concurrent requests
ab -n 10000 -c 1000 \
  -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1/summary
```

**Scenario 4: Bulk Operations Performance**
```bash
# Test bulk valuation updates
time curl -X POST http://localhost:8083/api/v1/portfolios/bulk-valuation \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"portfolioIds": [1,2,3,4,5,6,7,8,9,10]}'
# Should complete in <200ms
```

**Monitoring During Tests**:
```bash
# Monitor Prometheus metrics
curl http://localhost:8083/actuator/prometheus | grep portfolio

# Check circuit breaker status
curl http://localhost:8083/actuator/circuitbreakers

# Monitor thread usage
jcmd <PID> Thread.print | grep "Virtual Thread"
```

**Success Criteria**:
- ✅ All performance targets met
- ✅ No circuit breaker trips
- ✅ Memory usage stable under load
- ✅ Virtual threads scaling properly
- ✅ No database connection pool exhaustion

---

### Task 3.3: Update Documentation

**Status**: ⏳ PENDING
**Priority**: MEDIUM
**Estimated Time**: 2 hours

**Files to Update**:

**1. README.md**
```bash
# Update compliance status
sed -i 's/100% implementation compliance/100% implementation and test compliance/' README.md
sed -i 's/27 Mandatory Rules: Fully compliant/27 Mandatory Rules: 100% compliant - All tests passing/' README.md
```

**2. PORTFOLIO_SERVICE_PENDING_WORK.md**
```bash
# Update task completion status
# Mark all Category 6 (Testing) tasks as complete
# Update overall completion to 100%
```

**3. IMPLEMENTATION_STATUS.md**
```bash
# Update rule compliance
# Change failing rules to passing
# Update overall compliance to 100%
```

**4. CATEGORY_6_7_COMPLETION_STATUS.md**
```bash
# Update test status
# Mark all test files as functional
# Update coverage percentages
```

**5. API_USAGE_EXAMPLES.md**
```bash
# Remove any benchmark-related examples if feature removed
# Update examples to match final API
```

---

## DEPENDENCY TASKS - OTHER SERVICES

### Task 4.1: Market Data Service Integration (If Keeping Benchmark Feature)

**Status**: ⏳ OPTIONAL
**Priority**: LOW (only if not removing benchmark feature)
**Estimated Time**: 8-10 hours
**Service**: market-data-service + portfolio-service

**Only Required If**: Choosing Option B in Task 1.1 (implementing benchmarks)

**Subtasks**:

#### Task 4.1.1: Add Benchmark Data Endpoints to Market Data Service

**File**: `market-data-service/src/main/java/com/trademaster/marketdata/controller/InternalMarketDataController.java`

**New Endpoint**:
```java
@GetMapping("/api/internal/v1/market-data/benchmark/{symbol}/performance")
public ResponseEntity<BenchmarkPerformance> getBenchmarkPerformance(
    @PathVariable String symbol,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
) {
    // Implementation
}
```

**Steps**:
1. Create BenchmarkPerformance DTO
2. Add benchmark data retrieval logic
3. Add circuit breaker for external benchmark API
4. Add unit tests
5. Add integration tests

**Verification**:
```bash
cd market-data-service
./gradlew clean build
curl -H "X-API-Key: portfolio-service-secret-key" \
  "http://localhost:8081/api/internal/v1/market-data/benchmark/SPY/performance?startDate=2024-01-01&endDate=2024-12-31"
```

---

#### Task 4.1.2: Integrate Market Data Service in Portfolio Service

**File**: `portfolio-service/src/main/java/com/trademaster/portfolio/service/impl/PortfolioBenchmarkService.java`

**Create New Service**:
```java
@Service
@RequiredArgsConstructor
public class PortfolioBenchmarkService {
    private final InternalServiceClient internalServiceClient;

    @CircuitBreaker(name = "marketData")
    public CompletableFuture<BenchmarkPerformance> getBenchmarkPerformance(
        String symbol, Instant start, Instant end
    ) {
        // Call market-data-service internal API
    }
}
```

**Steps**:
1. Create PortfolioBenchmarkService
2. Update PortfolioController to use real benchmark data
3. Remove placeholder BigDecimal.ZERO values
4. Add error handling for market data unavailability
5. Add unit tests with mocks
6. Add integration tests with TestContainers

**Verification**:
```bash
cd portfolio-service
./gradlew clean build test
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8083/api/v1/portfolios/1/performance?startDate=2024-01-01&endDate=2024-12-31&benchmark=SPY"
```

---

### Task 4.2: No Additional Service Dependencies

**Status**: ✅ VERIFIED
**All other integrations are already implemented**:
- ✅ auth-service: JWT validation working
- ✅ trading-service: Internal APIs defined
- ✅ broker-auth-service: Internal APIs defined
- ✅ event-bus-service: Event publishing working

---

## TASK EXECUTION WORKFLOW

### Phase 1: Critical Fixes (Week 1 - Days 1-2)

**Day 1 Morning**: Task 1.1 - Remove TODOs
```bash
git checkout -b fix/remove-todos-placeholders
# Complete Task 1.1
./gradlew clean build
git add -A
git commit -m "fix(portfolio): Remove all TODO/placeholder comments - Rule #7 compliance

- Remove benchmark feature placeholders from PortfolioController
- Delete getPortfolioPerformance(), createPerformanceComparison(), convertToInstant()
- Update API documentation
- Achieve 100% Rule #7 compliance

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin fix/remove-todos-placeholders
```

**Day 1 Afternoon**: Task 1.2.1 - Fix MCPPortfolioServerTest
```bash
git checkout -b fix/mcp-portfolio-server-test
# Complete Task 1.2.1
./gradlew test --tests "MCPPortfolioServerTest" --console=plain
git add -A
git commit -m "test(portfolio): Fix MCPPortfolioServerTest compilation errors

- Fix RiskMetrics import/class resolution
- Update test assertions and mocks
- Verify all tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin fix/mcp-portfolio-server-test
```

**Day 2 Morning**: Task 1.2.2 - Fix PortfolioServiceIntegrationTest
```bash
git checkout -b fix/portfolio-service-integration-test
# Complete Task 1.2.2
./gradlew test --tests "PortfolioServiceIntegrationTest" --console=plain
git add -A
git commit -m "test(portfolio): Fix PortfolioServiceIntegrationTest compilation errors

- Replace FunctionalPortfolioService with PortfolioService
- Update @Autowired field types
- Fix method signatures and import statements
- Verify integration tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin fix/portfolio-service-integration-test
```

**Day 2 Afternoon**: Task 1.2.3 & 1.2.4 - Fix PortfolioTaskScopeTest + Full Suite
```bash
git checkout -b fix/portfolio-task-scope-test
# Complete Task 1.2.3
./gradlew clean test --console=plain
# Complete Task 1.2.4 - Full test suite
./gradlew test jacocoTestReport
git add -A
git commit -m "test(portfolio): Fix PortfolioTaskScopeTest and verify full test suite

- Fix minor test compilation issues
- Run full test suite - all tests passing
- Generate coverage report
- Achieve Rule #24 compliance

Coverage:
- Unit tests: XX%
- Integration tests: XX%
- Overall: XX%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin fix/portfolio-task-scope-test
```

---

### Phase 2: Code Quality (Week 2 - Days 3-5)

**Day 3**: Task 2.1.1 - Refactor createPortfolio()
```bash
git checkout -b refactor/functional-error-handling-create
# Complete Task 2.1.1
./gradlew clean build test
git add -A
git commit -m "refactor(portfolio): Convert createPortfolio to functional error handling

- Replace try-catch with Result types
- Use Result.ofTry() for error handling
- Update PortfolioController to handle Result type
- Improve Rule #3 compliance

Pattern: Railway programming with Result monad
Complexity reduction: 40%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin refactor/functional-error-handling-create
```

**Day 4**: Task 2.1.2 + 2.1.4 - Refactor updatePortfolio() + Remove if-else
```bash
git checkout -b refactor/functional-error-handling-update
# Complete Task 2.1.2 and 2.1.4
./gradlew clean build test
git add -A
git commit -m "refactor(portfolio): Convert updatePortfolio to functional patterns

- Replace try-catch with Result types in updatePortfolio()
- Remove if-else in PortfolioController (use fold pattern)
- Improve Rule #3 compliance to 85%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin refactor/functional-error-handling-update
```

**Day 5**: Task 2.2.1 + 2.1.3 - Refactor updateCashBalance()
```bash
git checkout -b refactor/extract-update-cash-balance
# Complete Task 2.2.1 and 2.1.3 together
./gradlew clean build test
git add -A
git commit -m "refactor(portfolio): Extract updateCashBalance method with functional patterns

- Reduce method from 48 lines to 15 lines orchestration
- Extract 7 helper methods (each 3-8 lines)
- Apply Phase 6D refactoring patterns
- Replace try-catch with Result types
- Achieve 100% Rule #5 compliance for this method

Complexity reduction:
- Line reduction: 60%
- Complexity reduction: 70%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin refactor/extract-update-cash-balance
```

---

### Phase 3: Final Verification (Week 3 - Days 6-7)

**Day 6**: Task 2.2.2 - Refactor getPortfoliosForUser()
```bash
git checkout -b refactor/extract-get-portfolios-for-user
# Complete Task 2.2.2
./gradlew clean build test
git add -A
git commit -m "refactor(portfolio): Extract getPortfoliosForUser method

- Reduce method from 23 lines to 12 lines orchestration
- Extract 3 helper methods
- Achieve 100% Rule #5 compliance

Complexity reduction:
- Line reduction: 48%
- Complexity reduction: 65%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin refactor/extract-get-portfolios-for-user
```

**Day 7 Morning**: Task 3.1 - Verify Test Coverage
```bash
git checkout -b verify/test-coverage
./gradlew clean test jacocoTestReport
./gradlew jacocoTestCoverageVerification
# Review coverage report
git add -A
git commit -m "test(portfolio): Verify and document test coverage

Coverage Results:
- Unit Tests: XX% (Target: >80%)
- Integration Tests: XX% (Target: >70%)
- Branch Coverage: XX% (Target: >75%)
- Overall: XX%

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin verify/test-coverage
```

**Day 7 Afternoon**: Task 3.2 - Performance Testing
```bash
# Run all load tests
./load-test-create-portfolio.sh
# Document results in performance-test-results.md
```

**Day 7 Evening**: Task 3.3 - Update Documentation
```bash
git checkout -b docs/update-final-status
# Complete Task 3.3
git add -A
git commit -m "docs(portfolio): Update all documentation for 100% compliance

- Update README.md with final compliance status
- Update IMPLEMENTATION_STATUS.md to 100%
- Update CATEGORY_6_7_COMPLETION_STATUS.md
- Update PORTFOLIO_SERVICE_PENDING_WORK.md

Final Status:
- 27/27 Rules Passing: 100%
- Golden Specification: 95%
- Functional Requirements: 100%
- Test Coverage: >80%
- Production Ready: ✅

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin docs/update-final-status
```

---

## FINAL VERIFICATION CHECKLIST

### Pre-Production Deployment Checklist

**Code Quality** ✅:
- [ ] All 27 mandatory rules passing
- [ ] Zero TODO/FIXME/placeholder comments
- [ ] Zero compilation errors (production + tests)
- [ ] Zero compilation warnings
- [ ] All methods ≤15 lines
- [ ] All method complexity ≤7
- [ ] 100% functional programming patterns

**Testing** ✅:
- [ ] All tests compile successfully
- [ ] All tests pass
- [ ] Unit test coverage >80%
- [ ] Integration test coverage >70%
- [ ] Load testing completed
- [ ] Performance targets met

**Documentation** ✅:
- [ ] README.md updated
- [ ] API documentation complete
- [ ] Deployment guide current
- [ ] Architecture diagrams updated
- [ ] All MD files reflect final state

**Golden Specification** ✅:
- [ ] Consul integration verified
- [ ] Kong API Gateway configured
- [ ] All 3 health checks working
- [ ] OpenAPI documentation complete
- [ ] JWT + API key authentication working
- [ ] Prometheus metrics collecting
- [ ] Circuit breakers functioning

**Final Build** ✅:
```bash
cd portfolio-service
./gradlew clean build -x test
./gradlew test
./gradlew jacocoTestReport
./gradlew bootRun
# Verify service starts successfully
# Test all API endpoints
# Check actuator health
curl http://localhost:8083/actuator/health
```

**Git Status** ✅:
```bash
git status
# Should show: nothing to commit, working tree clean
git log --oneline -10
# Review recent commits
git branch -a
# Ensure all feature branches merged
```

---

## SUCCESS CRITERIA

**Portfolio Service is 100% Complete When**:

✅ **All 27 mandatory rules passing** (currently 23/27)
✅ **Zero blocking violations** (Rule #7, #24)
✅ **Golden Specification 95%+** (currently 95%)
✅ **Functional requirements 100%** (currently 100%)
✅ **Test compilation 100%** (currently failing)
✅ **Test coverage >80% unit, >70% integration**
✅ **Performance targets met** (<200ms, <50ms, <25ms)
✅ **Documentation complete and accurate**
✅ **Production deployment ready**

**Final Compliance Score Target**: **100%** (27/27 rules)

---

## ESTIMATED TIMELINE

| Phase | Duration | Tasks | Completion |
|-------|----------|-------|------------|
| **Phase 1: Critical Fixes** | 2 days | 1.1, 1.2 | 0% |
| **Phase 2: Code Quality** | 3 days | 2.1, 2.2 | 0% |
| **Phase 3: Verification** | 2 days | 3.1, 3.2, 3.3 | 0% |
| **Total** | **7 days** | **15 tasks** | **0%** |

**Start Date**: TBD
**Target Completion**: Week 3
**Current Status**: Ready to Begin

---

## NOTES

1. **Task Dependencies**: Follow the order strictly - Phase 1 tasks are blocking
2. **Git Workflow**: Create feature branch for each task, commit after verification
3. **Testing**: Run full test suite after each task
4. **Documentation**: Update docs as you complete tasks
5. **Communication**: Update this file with progress daily
6. **Rollback Plan**: Keep feature branches until merged to main

---

**Document Version**: 1.0
**Last Updated**: 2025-10-27
**Status**: READY FOR EXECUTION
**Next Action**: Begin Task 1.1 - Remove TODO/Placeholder Comments
