# Market Data Service - Implementation Tasks

## Document Information

- **Service**: Market Data Service
- **Task Plan Version**: 1.0.0
- **Created**: 2025-01-18
- **Priority**: P0 - PRODUCTION BLOCKER
- **Estimated Total Effort**: 144-220 hours (18-27 working days)

---

## Table of Contents

1. [Phase 1: Immediate Blockers](#phase-1-immediate-blockers)
2. [Phase 2: Critical Issues](#phase-2-critical-issues)
3. [Phase 3: Quality Improvements](#phase-3-quality-improvements)
4. [Phase 4: Production Completeness](#phase-4-production-completeness)
5. [Task Dependencies](#task-dependencies)
6. [Resource Allocation](#resource-allocation)

---

## Phase 1: Immediate Blockers (6-14 hours)

**Priority**: P0 - IMMEDIATE
**Timeline**: Sprint 1, Week 1
**Goal**: Fix build and remove code duplication

### Task 1.1: Fix Common Library Dependency

**Priority**: P0
**Estimated Effort**: 1-2 hours
**Dependencies**: None
**Assignee**: Senior Java Developer

**Description**:
Fix the build system by properly configuring the common library dependency that is currently causing compilation failures.

**Acceptance Criteria**:
- [ ] `./gradlew :market-data-service:build` succeeds without errors
- [ ] All common library classes are accessible
- [ ] Project compiles successfully
- [ ] Zero compilation errors

**Implementation Steps**:
1. Add `implementation project(':trademaster-common-service-lib')` to build.gradle
2. Verify common library classes are accessible
3. Run `./gradlew :market-data-service:compileJava` from project root
4. Verify no compilation errors
5. Run `./gradlew :market-data-service:build` to confirm full build

**Files to Modify**:
- `market-data-service/build.gradle`

**Verification Command**:
```bash
cd /path/to/trademaster
./gradlew :market-data-service:build --warning-mode all
```

**Expected Output**:
```
BUILD SUCCESSFUL in 8s
0 errors
100 warnings  # Will fix in later tasks
```

---

### Task 1.2: Delete Duplicate Common Library Code

**Priority**: P0
**Estimated Effort**: 2-4 hours
**Dependencies**: Task 1.1 (Common library must be accessible)
**Assignee**: Senior Java Developer

**Description**:
Remove all locally duplicated code that exists in the common library and replace with proper imports.

**Acceptance Criteria**:
- [ ] All duplicate Result/Try/Validation code deleted
- [ ] Common library types imported and used
- [ ] No custom error handling code
- [ ] Project compiles successfully
- [ ] All tests pass

**Files to Delete/Modify**:

**DELETE These Files** (if they exist):
```
market-data-service/src/main/java/com/trademaster/marketdata/common/Result.java
market-data-service/src/main/java/com/trademaster/marketdata/common/Try.java
market-data-service/src/main/java/com/trademaster/marketdata/common/Validation.java
market-data-service/src/main/java/com/trademaster/marketdata/common/Either.java
```

**REPLACE With Imports**:
```java
import com.trademaster.common.functional.Result;
import com.trademaster.common.functional.Try;
import com.trademaster.common.functional.Validation;
import com.trademaster.common.functional.Either;
```

**Implementation Steps**:
1. Search for duplicate classes: `Result`, `Try`, `Validation`, `Either`
2. Verify common library has these classes
3. Delete local duplicates
4. Add imports from common library
5. Update all usages
6. Run tests to verify functionality
7. Run `./gradlew build` to confirm

**Verification Commands**:
```bash
# Search for duplicate classes
find . -name "Result.java" -o -name "Try.java" -o -name "Validation.java"

# Should only find in trademaster-common-service-lib, not in market-data-service

# Verify build
./gradlew :market-data-service:build
```

---

### Task 1.3: Split MarketDataService God Class

**Priority**: P0
**Estimated Effort**: 4-8 hours
**Dependencies**: Task 1.1, Task 1.2
**Assignee**: Senior Java Developer

**Description**:
Split the 771-line MarketDataService into 5 focused service classes following Single Responsibility Principle, each max 200 lines.

**Acceptance Criteria**:
- [ ] MarketDataService.java ≤ 200 lines
- [ ] 5 new service classes created
- [ ] Each service has single responsibility
- [ ] Max 10 methods per class
- [ ] Max 15 lines per method
- [ ] All tests pass
- [ ] Zero compilation errors

**New Service Classes to Create**:

1. **MarketDataQueryService.java** (150-180 lines)
   - `getCurrentPrice(symbol, exchange)`
   - `getHistoricalData(symbol, exchange, from, to, interval)`
   - `getBulkPriceData(symbols, exchange)`
   - `getActiveSymbols(exchange, minutes)`
   - **Responsibility**: Read-only market data queries

2. **MarketDataWriteService.java** (120-150 lines)
   - `writeMarketData(dataPoint)`
   - `batchWriteMarketData(dataPoints)`
   - **Responsibility**: Writing and persisting market data

3. **MarketDataSubscriptionService.java** (Already exists, verify <200 lines)
   - `subscribeToRealTimeUpdates(request)`
   - `unsubscribe(sessionId, symbols)`
   - `removeAllSubscriptions(sessionId)`
   - **Responsibility**: WebSocket subscription management

4. **PriceAlertService.java** (140-170 lines)
   - `createPriceAlert(request)`
   - `updatePriceAlert(request)`
   - `deletePriceAlert(request)`
   - `listPriceAlerts(criteria)`
   - **Responsibility**: Price alert management

5. **DataQualityService.java** (100-130 lines)
   - `generateQualityReport(symbol, exchange, hours)`
   - `validateDataPoint(dataPoint)`
   - **Responsibility**: Data quality and validation

**Refactored MarketDataService.java** (180-200 lines)
   - Orchestrates the above services
   - Provides facade for complex operations
   - Maintains backward compatibility

**Implementation Steps**:
1. Create 5 new service files with proper package structure
2. Extract methods from MarketDataService to appropriate new services
3. Update dependency injection
4. Update MarketDataService to use new services
5. Update MarketDataController to use new services
6. Run all tests
7. Verify line counts: `wc -l *.java`

**Files to Create**:
```
market-data-service/src/main/java/com/trademaster/marketdata/service/
├── MarketDataQueryService.java        (NEW)
├── MarketDataWriteService.java        (NEW)
├── PriceAlertService.java             (NEW)
├── DataQualityService.java            (NEW)
└── MarketDataService.java             (REFACTOR)
```

**Files to Modify**:
- `MarketDataService.java` - Reduce from 771 to <200 lines
- `MarketDataController.java` - Update dependency injection
- All test files - Update service references

**Verification Commands**:
```bash
# Check line counts
wc -l src/main/java/com/trademaster/marketdata/service/*.java

# Expected output (each file):
# 180 MarketDataQueryService.java
# 150 MarketDataWriteService.java
# 170 PriceAlertService.java
# 130 DataQualityService.java
# 200 MarketDataService.java

# Run tests
./gradlew :market-data-service:test

# Build
./gradlew :market-data-service:build
```

---

## Phase 2: Critical Issues (80-112 hours)

**Priority**: P1 - CRITICAL
**Timeline**: Sprint 1-2
**Goal**: Achieve functional programming compliance and test coverage

### Task 2.1: Eliminate If-Else Statements (Rule #3)

**Priority**: P1
**Estimated Effort**: 16-24 hours
**Dependencies**: Phase 1 complete
**Assignee**: Senior Java Developer

**Description**:
Convert 577 if-else statements to functional patterns using switch expressions, Optional, Strategy pattern, and Map lookups.

**Acceptance Criteria**:
- [ ] ZERO if-else statements in codebase
- [ ] All conditionals use switch expressions with pattern matching
- [ ] Optional used for null handling
- [ ] Strategy pattern for runtime polymorphism
- [ ] Map lookups for dispatch logic
- [ ] All tests pass
- [ ] Code complexity ≤ 7 per method

**Pattern Conversions Required**:

**Pattern 1: Simple If-Else → Switch Expression**

❌ BEFORE:
```java
if (interval.equalsIgnoreCase("1m")) {
    return parseOneMinuteData(jsonData);
} else if (interval.equalsIgnoreCase("5m")) {
    return parseFiveMinuteData(jsonData);
} else if (interval.equalsIgnoreCase("1h")) {
    return parseHourlyData(jsonData);
} else {
    throw new IllegalArgumentException("Unsupported interval");
}
```

✅ AFTER:
```java
return switch (interval.toLowerCase()) {
    case "1m" -> parseOneMinuteData(jsonData);
    case "5m" -> parseFiveMinuteData(jsonData);
    case "1h" -> parseHourlyData(jsonData);
    default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
};
```

**Pattern 2: Null Checks → Optional**

❌ BEFORE:
```java
if (cachedData == null) {
    return fetchFromDatabase(symbol);
} else {
    return cachedData;
}
```

✅ AFTER:
```java
return Optional.ofNullable(cachedData)
    .orElseGet(() -> fetchFromDatabase(symbol));
```

**Pattern 3: Complex Conditionals → Strategy Pattern**

❌ BEFORE:
```java
if (dataType.equals("TICK")) {
    return processTickData(data);
} else if (dataType.equals("OHLC")) {
    return processOHLCData(data);
} else if (dataType.equals("ORDER_BOOK")) {
    return processOrderBookData(data);
}
```

✅ AFTER:
```java
@FunctionalInterface
interface DataProcessor {
    MarketDataPoint process(RawData data);
}

private static final Map<String, DataProcessor> PROCESSORS = Map.of(
    "TICK", MarketDataService::processTickData,
    "OHLC", MarketDataService::processOHLCData,
    "ORDER_BOOK", MarketDataService::processOrderBookData
);

return Optional.ofNullable(PROCESSORS.get(dataType))
    .map(processor -> processor.process(data))
    .orElseThrow(() -> new IllegalArgumentException("Unknown data type: " + dataType));
```

**Pattern 4: Guard Clauses → Filter Chains**

❌ BEFORE:
```java
if (symbols != null && !symbols.isEmpty() && symbols.size() <= 100) {
    processSymbols(symbols);
} else {
    throw new ValidationException("Invalid symbols");
}
```

✅ AFTER:
```java
Optional.ofNullable(symbols)
    .filter(s -> !s.isEmpty())
    .filter(s -> s.size() <= 100)
    .ifPresentOrElse(
        this::processSymbols,
        () -> { throw new ValidationException("Invalid symbols"); }
    );
```

**Files to Modify** (577 if-else statements across):
```
market-data-service/src/main/java/com/trademaster/marketdata/provider/impl/BSEDataProvider.java
market-data-service/src/main/java/com/trademaster/marketdata/provider/impl/NSEDataProvider.java
market-data-service/src/main/java/com/trademaster/marketdata/provider/impl/AlphaVantageProvider.java
market-data-service/src/main/java/com/trademaster/marketdata/service/MarketDataProviderService.java
market-data-service/src/main/java/com/trademaster/marketdata/controller/MarketDataController.java
... and 15 more files
```

**Implementation Steps**:
1. Run grep to find all if-else: `grep -r "if (" --include="*.java"`
2. Categorize by pattern (simple, null check, strategy, guard)
3. Convert batch by pattern (start with simple)
4. Run tests after each batch
5. Verify complexity: `./gradlew sonarqube` (if configured)

**Verification Commands**:
```bash
# Find all if-else statements
grep -r "if (" --include="*.java" src/main/java | wc -l
# Expected: 0

# Verify all code compiles
./gradlew :market-data-service:build

# Run tests
./gradlew :market-data-service:test --tests "*"
```

---

### Task 2.2: Eliminate Loops (Rule #3)

**Priority**: P1
**Estimated Effort**: 8-16 hours
**Dependencies**: Task 2.1
**Assignee**: Senior Java Developer

**Description**:
Convert 48 for/while loops to functional Stream API operations.

**Acceptance Criteria**:
- [ ] ZERO for/while loops in codebase
- [ ] All collection processing uses Stream API
- [ ] Parallel streams used where appropriate
- [ ] Custom collectors for complex transformations
- [ ] All tests pass
- [ ] Performance benchmarks show no regression

**Pattern Conversions Required**:

**Pattern 1: Simple For Loop → Stream.map()**

❌ BEFORE:
```java
List<MarketDataPoint> results = new ArrayList<>();
for (String symbol : symbols) {
    MarketDataPoint dataPoint = getCurrentPrice(symbol);
    results.add(dataPoint);
}
return results;
```

✅ AFTER:
```java
return symbols.stream()
    .map(this::getCurrentPrice)
    .collect(Collectors.toList());
```

**Pattern 2: For Loop with Condition → Stream.filter().map()**

❌ BEFORE:
```java
List<MarketDataPoint> validPoints = new ArrayList<>();
for (MarketDataPoint point : dataPoints) {
    if (point.price() > 0 && point.volume() > 1000) {
        validPoints.add(point);
    }
}
return validPoints;
```

✅ AFTER:
```java
return dataPoints.stream()
    .filter(point -> point.price() > 0)
    .filter(point -> point.volume() > 1000)
    .collect(Collectors.toList());
```

**Pattern 3: For Loop with Aggregation → Stream.reduce()**

❌ BEFORE:
```java
double totalVolume = 0.0;
for (MarketDataPoint point : dataPoints) {
    totalVolume += point.volume();
}
return totalVolume;
```

✅ AFTER:
```java
return dataPoints.stream()
    .mapToDouble(MarketDataPoint::volume)
    .sum();
```

**Pattern 4: Nested For Loop → Stream.flatMap()**

❌ BEFORE:
```java
List<Trade> allTrades = new ArrayList<>();
for (String symbol : symbols) {
    List<Trade> symbolTrades = getTrades(symbol);
    for (Trade trade : symbolTrades) {
        if (trade.quantity() > 100) {
            allTrades.add(trade);
        }
    }
}
return allTrades;
```

✅ AFTER:
```java
return symbols.stream()
    .flatMap(symbol -> getTrades(symbol).stream())
    .filter(trade -> trade.quantity() > 100)
    .collect(Collectors.toList());
```

**Pattern 5: For Loop with Map Building → Stream.collect(Collectors.toMap())**

❌ BEFORE:
```java
Map<String, MarketDataPoint> priceMap = new HashMap<>();
for (MarketDataPoint point : dataPoints) {
    priceMap.put(point.symbol(), point);
}
return priceMap;
```

✅ AFTER:
```java
return dataPoints.stream()
    .collect(Collectors.toMap(
        MarketDataPoint::symbol,
        Function.identity(),
        (existing, replacement) -> replacement  // Handle duplicates
    ));
```

**Files to Modify** (48 loops across):
```
market-data-service/src/main/java/com/trademaster/marketdata/service/*.java
market-data-service/src/main/java/com/trademaster/marketdata/provider/impl/*.java
market-data-service/src/main/java/com/trademaster/marketdata/websocket/*.java
```

**Implementation Steps**:
1. Find all loops: `grep -r "for (" --include="*.java"`
2. Categorize by pattern
3. Convert batch by pattern
4. Add parallel streams where beneficial
5. Run performance tests
6. Verify tests pass

**Verification Commands**:
```bash
# Find all loops
grep -r "for (" --include="*.java" src/main/java | wc -l
# Expected: 0

grep -r "while (" --include="*.java" src/main/java | wc -l
# Expected: 0

# Build
./gradlew :market-data-service:build

# Run tests
./gradlew :market-data-service:test
```

---

### Task 2.3: Fix 100 Compiler Warnings (Rule #8)

**Priority**: P1
**Estimated Effort**: 4-8 hours
**Dependencies**: Task 2.1, Task 2.2
**Assignee**: Senior Java Developer

**Description**:
Fix all 100 compiler warnings to achieve zero-warning policy.

**Acceptance Criteria**:
- [ ] ZERO compiler warnings
- [ ] All lambdas converted from anonymous classes
- [ ] All method references used where applicable
- [ ] All unused code removed
- [ ] All deprecated APIs replaced
- [ ] Build succeeds with 0 warnings

**Warning Categories to Fix**:

**Category 1: Convert to Lambda (45 warnings)**

❌ BEFORE:
```java
CompletableFuture.supplyAsync(new Supplier<MarketDataPoint>() {
    @Override
    public MarketDataPoint get() {
        return fetchData(symbol);
    }
}, executor);
```

✅ AFTER:
```java
CompletableFuture.supplyAsync(() -> fetchData(symbol), executor);
```

**Category 2: Use Method Reference (28 warnings)**

❌ BEFORE:
```java
symbols.stream()
    .map(s -> String.valueOf(s))
    .collect(Collectors.toList());
```

✅ AFTER:
```java
symbols.stream()
    .map(String::valueOf)
    .toList();
```

**Category 3: Remove Unused (15 warnings)**

❌ BEFORE:
```java
private void unusedHelperMethod() {
    // This method is never called
}

import java.util.Collections;  // Never used
```

✅ AFTER:
```java
// Delete unused method

// Remove unused import
```

**Category 4: Replace Deprecated (12 warnings)**

❌ BEFORE:
```java
@Deprecated
Date legacyDate = new Date();
```

✅ AFTER:
```java
Instant modernInstant = Instant.now();
LocalDateTime localDateTime = LocalDateTime.now();
```

**Implementation Steps**:
1. Run build with warnings: `./gradlew build --warning-mode all > warnings.txt`
2. Categorize warnings by type
3. Fix batch by category (lambdas, method refs, unused, deprecated)
4. Verify each batch: `./gradlew build --warning-mode all`
5. Repeat until 0 warnings

**Files to Modify**:
```
# All Java files with warnings (run build to identify)
./gradlew :market-data-service:build --warning-mode all 2>&1 | grep "warning:"
```

**Verification Commands**:
```bash
# Build with all warnings
./gradlew :market-data-service:build --warning-mode all

# Expected output:
# BUILD SUCCESSFUL in 8s
# 0 errors
# 0 warnings ✅
```

---

### Task 2.4: Implement Unit Tests (Rule #20)

**Priority**: P1
**Estimated Effort**: 40-56 hours
**Dependencies**: Phase 2 Tasks 2.1-2.3
**Assignee**: Senior Java Developer + QA Engineer

**Description**:
Implement comprehensive unit tests to achieve >80% code coverage using functional test builders.

**Acceptance Criteria**:
- [ ] Unit test coverage >80%
- [ ] All service classes have test coverage
- [ ] All controller methods tested
- [ ] Functional test builders implemented
- [ ] Property-based tests for complex logic
- [ ] Virtual thread tests for concurrency
- [ ] All tests pass
- [ ] Test execution <2 minutes

**Test Structure**:

```
market-data-service/src/test/java/com/trademaster/marketdata/
├── service/
│   ├── MarketDataQueryServiceTest.java      (80%+ coverage)
│   ├── MarketDataWriteServiceTest.java      (80%+ coverage)
│   ├── PriceAlertServiceTest.java           (80%+ coverage)
│   ├── DataQualityServiceTest.java          (80%+ coverage)
│   └── MarketDataSubscriptionServiceTest.java (80%+ coverage)
├── controller/
│   ├── MarketDataControllerTest.java        (80%+ coverage)
│   └── MarketDataWebSocketHandlerTest.java  (80%+ coverage)
├── provider/
│   ├── BSEDataProviderTest.java             (80%+ coverage)
│   ├── NSEDataProviderTest.java             (80%+ coverage)
│   └── AlphaVantageProviderTest.java        (80%+ coverage)
├── builder/
│   ├── MarketDataPointBuilder.java          (Test builder)
│   ├── SubscriptionRequestBuilder.java      (Test builder)
│   └── PriceAlertRequestBuilder.java        (Test builder)
└── property/
    └── MarketDataPropertyTests.java         (Property-based tests)
```

**Functional Test Builder Pattern**:

```java
// MarketDataPointBuilder.java
public class MarketDataPointBuilder {

    // Fluent builder with sensible defaults
    public static MarketDataPoint.MarketDataPointBuilder defaultMarketDataPoint() {
        return MarketDataPoint.builder()
            .symbol("RELIANCE")
            .exchange("NSE")
            .price(BigDecimal.valueOf(2500.00))
            .volume(1000000L)
            .timestamp(Instant.now())
            .dataType("TICK");
    }

    // Named constructors for common scenarios
    public static MarketDataPoint tickData(String symbol) {
        return defaultMarketDataPoint()
            .symbol(symbol)
            .dataType("TICK")
            .build();
    }

    public static MarketDataPoint ohlcData(String symbol, String interval) {
        return defaultMarketDataPoint()
            .symbol(symbol)
            .dataType("OHLC")
            .open(BigDecimal.valueOf(2450.00))
            .high(BigDecimal.valueOf(2550.00))
            .low(BigDecimal.valueOf(2440.00))
            .price(BigDecimal.valueOf(2500.00))
            .build();
    }
}
```

**Test Example Using Builder**:

```java
@Test
void shouldRetrieveCurrentPrice() {
    // Given
    String symbol = "RELIANCE";
    MarketDataPoint expectedData = MarketDataPointBuilder.tickData(symbol);
    when(marketDataRepository.getLatestPrice(symbol, "NSE"))
        .thenReturn(Optional.of(expectedData));

    // When
    CompletableFuture<Optional<MarketDataPoint>> result =
        marketDataQueryService.getCurrentPrice(symbol, "NSE");

    // Then
    assertThat(result.join())
        .isPresent()
        .get()
        .satisfies(data -> {
            assertThat(data.symbol()).isEqualTo(symbol);
            assertThat(data.price()).isGreaterThan(BigDecimal.ZERO);
        });
}
```

**Virtual Thread Concurrency Test**:

```java
@Test
void shouldHandleConcurrentRequestsWithVirtualThreads() throws Exception {
    // Given
    int concurrentRequests = 10000;
    List<String> symbols = List.of("RELIANCE", "TCS", "INFY", "HDFC", "ICICI");

    // When
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        List<StructuredTaskScope.Subtask<Optional<MarketDataPoint>>> tasks =
            IntStream.range(0, concurrentRequests)
                .mapToObj(i -> symbols.get(i % symbols.size()))
                .map(symbol -> scope.fork(() ->
                    marketDataQueryService.getCurrentPrice(symbol, "NSE").join()))
                .toList();

        scope.join();
        scope.throwIfFailed();

        // Then
        long successfulRequests = tasks.stream()
            .map(StructuredTaskScope.Subtask::get)
            .filter(Optional::isPresent)
            .count();

        assertThat(successfulRequests).isGreaterThan(concurrentRequests * 0.95);
    }
}
```

**Implementation Steps**:
1. Create test builders (MarketDataPointBuilder, etc.)
2. Write service tests (80%+ coverage each)
3. Write controller tests
4. Write provider tests
5. Add property-based tests for complex logic
6. Add concurrency tests with virtual threads
7. Verify coverage: `./gradlew jacocoTestReport`
8. Review coverage report in `build/reports/jacoco/test/html/index.html`

**Files to Create**:
```
src/test/java/com/trademaster/marketdata/
├── builder/
│   ├── MarketDataPointBuilder.java
│   ├── SubscriptionRequestBuilder.java
│   └── PriceAlertRequestBuilder.java
├── service/
│   ├── MarketDataQueryServiceTest.java
│   ├── MarketDataWriteServiceTest.java
│   ├── PriceAlertServiceTest.java
│   └── DataQualityServiceTest.java
└── property/
    └── MarketDataPropertyTests.java
```

**Verification Commands**:
```bash
# Run all tests
./gradlew :market-data-service:test

# Generate coverage report
./gradlew :market-data-service:jacocoTestReport

# View coverage
open build/reports/jacoco/test/html/index.html

# Expected: >80% line coverage, >75% branch coverage
```

---

### Task 2.5: Apply Zero Trust Security Pattern (Rule #6)

**Priority**: P1
**Estimated Effort**: 8-12 hours
**Dependencies**: Phase 1 complete
**Assignee**: Senior Java Developer

**Description**:
Implement tiered security pattern with SecurityFacade + SecurityMediator for external access and simple injection for internal services.

**Acceptance Criteria**:
- [ ] SecurityFacade implemented for external API access
- [ ] SecurityMediator coordinates authentication, authorization, audit
- [ ] Internal service-to-service uses simple injection
- [ ] Clear security boundary separation
- [ ] Audit logging for all external access
- [ ] All tests pass

**Security Pattern Implementation**:

**External Access (Full Security Stack)**:

```java
// 1. SecurityFacade.java
@Component
@RequiredArgsConstructor
public class SecurityFacade {
    private final SecurityMediator mediator;

    public <T> Result<T, SecurityError> secureAccess(
            SecurityContext context,
            Function<Void, T> operation) {
        return mediator.mediateAccess(context, operation);
    }
}

// 2. SecurityMediator.java
@Component
@RequiredArgsConstructor
public class SecurityMediator {
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final AuditService auditService;
    private final RiskAssessmentService riskService;

    public <T> Result<T, SecurityError> mediateAccess(
            SecurityContext context,
            Function<Void, T> operation) {

        return authService.authenticate(context)
            .flatMap(authzService::authorize)
            .flatMap(riskService::assessRisk)
            .flatMap(this::executeOperation)
            .map(result -> auditService.log(context, result));
    }
}

// 3. MarketDataController.java (External API)
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {
    private final SecurityFacade securityFacade;
    private final MarketDataQueryService queryService;

    @GetMapping("/{symbol}/price")
    public ResponseEntity<Result<MarketDataPoint, SecurityError>> getCurrentPrice(
            @PathVariable String symbol,
            @RequestHeader("Authorization") String token) {

        SecurityContext context = SecurityContext.fromJWT(token);

        Result<MarketDataPoint, SecurityError> result = securityFacade.secureAccess(
            context,
            () -> queryService.getCurrentPrice(symbol, "NSE").join().orElseThrow()
        );

        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(403).body(result);
    }
}
```

**Internal Access (Lightweight Direct)**:

```java
// InternalMarketDataController.java
@RestController
@RequestMapping("/api/internal/v1/market-data")
@RequiredArgsConstructor  // Simple constructor injection
public class InternalMarketDataController {
    private final MarketDataQueryService queryService;  // Direct injection
    private final MarketDataWriteService writeService;

    @GetMapping("/{symbol}/price")
    @PreAuthorize("hasRole('SERVICE')")  // Role-based only
    public ResponseEntity<MarketDataPoint> getCurrentPrice(@PathVariable String symbol) {
        // Direct service call - already inside security boundary
        return queryService.getCurrentPrice(symbol, "NSE")
            .thenApply(dataPoint -> dataPoint
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()))
            .join();
    }
}
```

**Implementation Steps**:
1. Create SecurityFacade.java
2. Create SecurityMediator.java
3. Create AuthenticationService, AuthorizationService, AuditService
4. Update MarketDataController to use SecurityFacade
5. Keep InternalMarketDataController with simple injection
6. Add security audit logging
7. Write security tests

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/security/
├── SecurityFacade.java
├── SecurityMediator.java
├── AuthenticationService.java
├── AuthorizationService.java
├── AuditService.java
└── RiskAssessmentService.java
```

**Files to Modify**:
```
src/main/java/com/trademaster/marketdata/controller/
├── MarketDataController.java           (Add SecurityFacade)
└── InternalMarketDataController.java   (Keep simple injection)
```

**Verification Commands**:
```bash
# Build
./gradlew :market-data-service:build

# Run security tests
./gradlew :market-data-service:test --tests "*Security*"

# Test external API with JWT
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:8084/api/v1/market-data/RELIANCE/price

# Test internal API with service key
curl -H "X-API-Key: <service-key>" \
     http://localhost:8084/api/internal/v1/market-data/RELIANCE/price
```

---

### Task 2.6: Extract Constants and Magic Numbers (Rule #17)

**Priority**: P2
**Estimated Effort**: 4-6 hours
**Dependencies**: Phase 2 Tasks 2.1-2.3
**Assignee**: Mid-level Java Developer

**Description**:
Extract all magic numbers and magic strings into properly named constants.

**Acceptance Criteria**:
- [ ] ZERO magic numbers in code
- [ ] ZERO magic strings in code
- [ ] All constants in dedicated classes
- [ ] Constants properly documented
- [ ] Meaningful constant names
- [ ] All tests pass

**Constant Classes to Create**:

```java
// MarketDataConstants.java
public final class MarketDataConstants {

    // Prevent instantiation
    private MarketDataConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // Timeouts
    public static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final int CIRCUIT_BREAKER_TIMEOUT_MS = 30000;
    public static final int WEBSOCKET_TIMEOUT_MS = 60000;

    // Rate Limits
    public static final int MAX_SYMBOLS_PER_SUBSCRIPTION = 100;
    public static final int MAX_CONCURRENT_REQUESTS = 10000;
    public static final int MIN_UPDATE_FREQUENCY_MS = 100;

    // Cache TTLs
    public static final int CURRENT_PRICE_CACHE_TTL_SECONDS = 5;
    public static final int OHLC_DATA_CACHE_TTL_SECONDS = 60;
    public static final int SYMBOL_LIST_CACHE_TTL_SECONDS = 300;

    // Data Quality Thresholds
    public static final double HIGH_QUALITY_SCORE = 0.95;
    public static final double MEDIUM_QUALITY_SCORE = 0.80;
    public static final double LOW_QUALITY_THRESHOLD = 0.60;

    // Exchanges
    public static final String EXCHANGE_NSE = "NSE";
    public static final String EXCHANGE_BSE = "BSE";
    public static final String EXCHANGE_MCX = "MCX";

    // Data Types
    public static final String DATA_TYPE_TICK = "TICK";
    public static final String DATA_TYPE_OHLC = "OHLC";
    public static final String DATA_TYPE_ORDER_BOOK = "ORDER_BOOK";
}
```

**Before/After Example**:

❌ BEFORE:
```java
if (symbols.size() > 100) {
    throw new IllegalArgumentException("Too many symbols");
}

if (updateFrequency < 100) {
    throw new IllegalArgumentException("Frequency too low");
}

cache.set(key, value, 5);  // What does 5 mean?
```

✅ AFTER:
```java
if (symbols.size() > MarketDataConstants.MAX_SYMBOLS_PER_SUBSCRIPTION) {
    throw new IllegalArgumentException("Exceeded max symbols: " +
        MarketDataConstants.MAX_SYMBOLS_PER_SUBSCRIPTION);
}

if (updateFrequency < MarketDataConstants.MIN_UPDATE_FREQUENCY_MS) {
    throw new IllegalArgumentException("Update frequency must be at least " +
        MarketDataConstants.MIN_UPDATE_FREQUENCY_MS + "ms");
}

cache.set(key, value, MarketDataConstants.CURRENT_PRICE_CACHE_TTL_SECONDS);
```

**Implementation Steps**:
1. Search for magic numbers: `grep -E "[^a-zA-Z_][0-9]{2,}" --include="*.java" -r src/main/java`
2. Search for magic strings: `grep -E "\"[A-Z]{3,}\"" --include="*.java" -r src/main/java`
3. Categorize constants (timeouts, limits, thresholds, etc.)
4. Create constant classes
5. Replace all usages
6. Run tests
7. Verify build

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/constants/
├── MarketDataConstants.java
├── CacheConstants.java
├── ValidationConstants.java
└── ExchangeConstants.java
```

**Verification Commands**:
```bash
# Search for remaining magic numbers (should find few/none)
grep -E "[^a-zA-Z_][0-9]{3,}" --include="*.java" -r src/main/java

# Build
./gradlew :market-data-service:build

# Run tests
./gradlew :market-data-service:test
```

---

## Phase 3: Quality Improvements (24-42 hours)

**Priority**: P2 - HIGH
**Timeline**: Sprint 2-3
**Goal**: Enhance code quality and error handling

### Task 3.1: Implement Result/Try Error Handling (Rule #11)

**Priority**: P2
**Estimated Effort**: 8-12 hours
**Dependencies**: Phase 2 complete, Common library integrated
**Assignee**: Senior Java Developer

**Description**:
Replace exception-based error handling with Result/Try functional types from common library.

**Acceptance Criteria**:
- [ ] All public methods return Result<T, E>
- [ ] Railway programming pattern for error handling
- [ ] No try-catch in business logic
- [ ] Functional error composition with flatMap/map
- [ ] All tests pass
- [ ] Error handling consistency

**Error Handling Pattern**:

```java
// Using Result<T, E> from common library
import com.trademaster.common.functional.Result;

// Before: Exception-based
public MarketDataPoint getCurrentPrice(String symbol) {
    try {
        MarketDataPoint data = repository.findLatest(symbol);
        if (data == null) {
            throw new NotFoundException("Symbol not found");
        }
        return data;
    } catch (DatabaseException e) {
        throw new ServiceException("Database error", e);
    }
}

// After: Result-based
public Result<MarketDataPoint, MarketDataError> getCurrentPrice(String symbol) {
    return repository.findLatest(symbol)
        .toResult(() -> MarketDataError.symbolNotFound(symbol))
        .onFailure(error -> log.error("Failed to get price: {}", error));
}
```

**Railway Programming Pattern**:

```java
// Chaining operations with automatic error propagation
public Result<OrderConfirmation, TradingError> placeOrder(OrderRequest request) {
    return validateOrder(request)
        .flatMap(this::checkRiskLimits)
        .flatMap(this::checkMarketData)
        .flatMap(this::executeOrder)
        .map(this::auditOrder)
        .onSuccess(order -> log.info("Order placed: {}", order.id()))
        .onFailure(error -> log.error("Order failed: {}", error.message()));
}
```

**Implementation Steps**:
1. Import Result, Try from common library
2. Create MarketDataError sealed interface
3. Convert service methods to return Result<T, E>
4. Update controllers to handle Result types
5. Write tests for error scenarios
6. Remove try-catch blocks from business logic

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/error/
├── MarketDataError.java          (Sealed interface)
├── SymbolNotFoundError.java
├── DataQualityError.java
├── ProviderUnavailableError.java
└── ValidationError.java
```

**Files to Modify**:
```
src/main/java/com/trademaster/marketdata/service/*.java
src/main/java/com/trademaster/marketdata/controller/*.java
```

**Verification Commands**:
```bash
# Build
./gradlew :market-data-service:build

# Run tests
./gradlew :market-data-service:test

# Verify no try-catch in business logic
grep -r "try {" --include="*.java" src/main/java/com/trademaster/marketdata/service
# Should find ZERO matches
```

---

### Task 3.2: Add Validation Framework (Common Library)

**Priority**: P2
**Estimated Effort**: 4-8 hours
**Dependencies**: Task 3.1
**Assignee**: Mid-level Java Developer

**Description**:
Replace manual validation with common library Validation framework.

**Acceptance Criteria**:
- [ ] All DTOs use Validation framework
- [ ] Custom validators for business rules
- [ ] Functional validation chains
- [ ] Clear validation error messages
- [ ] All tests pass

**Validation Pattern**:

```java
// Using Validation from common library
import com.trademaster.common.functional.Validation;
import com.trademaster.common.functional.Validators;

// Before: Manual validation
public void validateSubscriptionRequest(SubscriptionRequest request) {
    if (request.symbols() == null || request.symbols().isEmpty()) {
        throw new IllegalArgumentException("Symbols cannot be empty");
    }
    if (request.symbols().size() > 100) {
        throw new IllegalArgumentException("Max 100 symbols");
    }
    if (request.updateFrequency() != null && request.updateFrequency() < 100) {
        throw new IllegalArgumentException("Min frequency 100ms");
    }
}

// After: Validation framework
public Validation<SubscriptionRequest> validateSubscriptionRequest(SubscriptionRequest request) {
    return Validation.validate(request)
        .field(SubscriptionRequest::symbols,
            Validators.notEmpty("Symbols cannot be empty"))
        .field(SubscriptionRequest::symbols,
            Validators.maxSize(100, "Maximum 100 symbols allowed"))
        .field(SubscriptionRequest::updateFrequency,
            Validators.min(100, "Update frequency must be at least 100ms"));
}
```

**Implementation Steps**:
1. Import Validation, Validators from common library
2. Create custom validators for business rules
3. Replace manual validation in controllers
4. Replace manual validation in services
5. Write validation tests

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/validation/
├── SymbolValidator.java
├── PriceAlertValidator.java
└── SubscriptionValidator.java
```

**Verification Commands**:
```bash
# Build
./gradlew :market-data-service:build

# Run validation tests
./gradlew :market-data-service:test --tests "*Validation*"
```

---

### Task 3.3: Performance Benchmarks and SLA Monitoring

**Priority**: P2
**Estimated Effort**: 6-10 hours
**Dependencies**: Phase 2 complete
**Assignee**: Senior Java Developer

**Description**:
Implement performance benchmarks and SLA monitoring to verify <25ms critical, <50ms high, <100ms standard operations.

**Acceptance Criteria**:
- [ ] JMH benchmarks for critical operations
- [ ] SLA monitoring with Prometheus metrics
- [ ] Performance regression tests
- [ ] Benchmark results documented
- [ ] All operations meet SLA targets

**JMH Benchmark Setup**:

```java
// MarketDataBenchmark.java
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MarketDataBenchmark {

    private MarketDataQueryService queryService;

    @Setup
    public void setup() {
        // Initialize service with test data
    }

    @Benchmark
    public void benchmarkGetCurrentPrice() {
        queryService.getCurrentPrice("RELIANCE", "NSE").join();
    }

    @Benchmark
    public void benchmarkBulkPriceData() {
        List<String> symbols = List.of("RELIANCE", "TCS", "INFY", "HDFC", "ICICI");
        queryService.getBulkPriceData(symbols, "NSE").join();
    }
}
```

**SLA Monitoring**:

```java
// SLAMetrics.java
@Component
@RequiredArgsConstructor
public class SLAMetrics {
    private final MeterRegistry meterRegistry;

    public void recordOperation(String operation, Duration duration, String slaLevel) {
        Timer.builder("market_data.operation.duration")
            .tag("operation", operation)
            .tag("sla_level", slaLevel)
            .register(meterRegistry)
            .record(duration);

        // Record SLA violation if threshold exceeded
        if (isSLAViolation(duration, slaLevel)) {
            Counter.builder("market_data.sla.violations")
                .tag("operation", operation)
                .tag("sla_level", slaLevel)
                .register(meterRegistry)
                .increment();
        }
    }

    private boolean isSLAViolation(Duration duration, String slaLevel) {
        return switch (slaLevel) {
            case "CRITICAL" -> duration.toMillis() > 25;
            case "HIGH" -> duration.toMillis() > 50;
            case "STANDARD" -> duration.toMillis() > 100;
            default -> false;
        };
    }
}
```

**Implementation Steps**:
1. Add JMH dependency to build.gradle
2. Create benchmark classes
3. Run benchmarks: `./gradlew jmh`
4. Add SLA metrics to services
5. Configure Grafana dashboards
6. Document benchmark results

**Files to Create**:
```
src/jmh/java/com/trademaster/marketdata/benchmark/
├── MarketDataBenchmark.java
├── CircuitBreakerBenchmark.java
└── CacheBenchmark.java
```

**Verification Commands**:
```bash
# Run JMH benchmarks
./gradlew jmh

# View results
cat build/reports/jmh/results.txt

# Expected SLA compliance:
# getCurrentPrice: 15-20ms (CRITICAL: ✅ <25ms)
# getBulkPriceData: 40-45ms (HIGH: ✅ <50ms)
# getHistoricalData: 80-90ms (STANDARD: ✅ <100ms)
```

---

### Task 3.4: Integration Tests with TestContainers (Rule #20)

**Priority**: P2
**Estimated Effort**: 6-12 hours
**Dependencies**: Phase 2 Task 2.4
**Assignee**: QA Engineer + Senior Java Developer

**Description**:
Implement integration tests using TestContainers for PostgreSQL, Redis, Consul, and Kafka.

**Acceptance Criteria**:
- [ ] Integration test coverage >70%
- [ ] TestContainers for all dependencies
- [ ] Database integration tests
- [ ] Cache integration tests
- [ ] Consul integration tests
- [ ] Kafka integration tests
- [ ] All tests pass
- [ ] Test execution <5 minutes

**TestContainers Setup**:

```java
// AbstractIntegrationTest.java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_market_data")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static ConsulContainer consul = new ConsulContainer("hashicorp/consul:1.17")
        .withConsulCommand("agent -dev -client 0.0.0.0");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cloud.consul.host", consul::getHost);
        registry.add("spring.cloud.consul.port", consul::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

**Integration Test Example**:

```java
// MarketDataServiceIntegrationTest.java
@ExtendWith(SpringExtension.class)
class MarketDataServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MarketDataQueryService queryService;

    @Autowired
    private MarketDataWriteService writeService;

    @Autowired
    private MarketDataRepository repository;

    @Test
    void shouldWriteAndRetrieveMarketData() {
        // Given
        MarketDataPoint dataPoint = MarketDataPointBuilder.tickData("RELIANCE");

        // When
        Result<Boolean, MarketDataError> writeResult =
            writeService.writeMarketData(dataPoint).join();

        // Then
        assertThat(writeResult.isSuccess()).isTrue();

        Result<MarketDataPoint, MarketDataError> queryResult =
            queryService.getCurrentPrice("RELIANCE", "NSE").join();

        assertThat(queryResult.isSuccess()).isTrue();
        assertThat(queryResult.get().symbol()).isEqualTo("RELIANCE");
    }

    @Test
    void shouldCachePriceDataInRedis() {
        // Given
        MarketDataPoint dataPoint = MarketDataPointBuilder.tickData("TCS");
        writeService.writeMarketData(dataPoint).join();

        // When - First call hits database
        queryService.getCurrentPrice("TCS", "NSE").join();

        // Then - Second call should hit cache
        long startTime = System.currentTimeMillis();
        queryService.getCurrentPrice("TCS", "NSE").join();
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(10); // Cache hit should be <10ms
    }
}
```

**Implementation Steps**:
1. Add TestContainers dependencies
2. Create AbstractIntegrationTest
3. Write database integration tests
4. Write cache integration tests
5. Write Consul integration tests
6. Write Kafka integration tests
7. Run integration tests: `./gradlew integrationTest`

**Files to Create**:
```
src/test/java/com/trademaster/marketdata/integration/
├── AbstractIntegrationTest.java
├── MarketDataServiceIntegrationTest.java
├── MarketDataCacheIntegrationTest.java
├── ConsulIntegrationTest.java
└── KafkaIntegrationTest.java
```

**Verification Commands**:
```bash
# Run integration tests
./gradlew :market-data-service:integrationTest

# Generate coverage report
./gradlew :market-data-service:jacocoTestReport

# Expected: >70% integration coverage
```

---

## Phase 4: Production Completeness (32-52 hours)

**Priority**: P3 - MEDIUM
**Timeline**: Sprint 3-4
**Goal**: Complete missing features and production readiness

### Task 4.1: Implement MCX Data Provider (MVP Requirement)

**Priority**: P3
**Estimated Effort**: 8-12 hours
**Dependencies**: Phase 2 complete
**Assignee**: Mid-level Java Developer

**Description**:
Implement MCX (Multi Commodity Exchange) data provider to complete MVP requirements for market data integration.

**Acceptance Criteria**:
- [ ] MCXDataProvider implemented
- [ ] Circuit breaker configured for MCX
- [ ] Support for commodity futures data
- [ ] Integration with MarketDataProviderService
- [ ] Unit tests >80% coverage
- [ ] Integration tests with TestContainers
- [ ] Documentation updated

**MCX Data Provider Pattern**:

```java
// MCXDataProvider.java
@Service
@RequiredArgsConstructor
@Slf4j
public class MCXDataProvider implements MarketDataProvider {

    private final CircuitBreakerService circuitBreakerService;
    private final RestTemplate restTemplate;

    @Value("${trademaster.mcx.api.url}")
    private String mcxApiUrl;

    @Value("${trademaster.mcx.api.key}")
    private String apiKey;

    @Override
    public CompletableFuture<Optional<MarketDataPoint>> getCurrentPrice(String symbol) {
        return circuitBreakerService.executeMCXProviderOperationWithFallback(
            () -> fetchFromMCX(symbol),
            () -> Optional.empty()
        );
    }

    private Optional<MarketDataPoint> fetchFromMCX(String symbol) {
        try {
            String url = mcxApiUrl + "/commodity/" + symbol;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);

            ResponseEntity<MCXResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), MCXResponse.class);

            return Optional.ofNullable(response.getBody())
                .map(this::convertToMarketDataPoint);

        } catch (Exception e) {
            log.error("MCX API call failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private MarketDataPoint convertToMarketDataPoint(MCXResponse response) {
        return MarketDataPoint.builder()
            .symbol(response.symbol())
            .exchange("MCX")
            .price(response.ltp())
            .volume(response.volume())
            .dataType("COMMODITY")
            .timestamp(Instant.now())
            .build();
    }
}
```

**Implementation Steps**:
1. Create MCXDataProvider.java
2. Add MCX circuit breaker configuration
3. Create MCXResponse DTO
4. Integrate with MarketDataProviderService
5. Write unit tests
6. Write integration tests
7. Update documentation

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/provider/impl/
├── MCXDataProvider.java

src/main/java/com/trademaster/marketdata/dto/
├── MCXResponse.java
├── MCXQuote.java

src/test/java/com/trademaster/marketdata/provider/impl/
├── MCXDataProviderTest.java

src/test/java/com/trademaster/marketdata/integration/
├── MCXIntegrationTest.java
```

**Configuration**:
```yaml
# application.yml
trademaster:
  mcx:
    api:
      url: ${MCX_API_URL:https://api.mcxindia.com}
      key: ${MCX_API_KEY}
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 120000
      sliding-window-size: 10
```

**Verification Commands**:
```bash
# Run tests
./gradlew :market-data-service:test --tests "*MCX*"

# Build
./gradlew :market-data-service:build

# Test MCX endpoint
curl -H "X-API-Key: <service-key>" \
     http://localhost:8084/api/internal/v1/market-data/GOLD/price?exchange=MCX
```

---

### Task 4.2: Implement Security Audit Logging

**Priority**: P3
**Estimated Effort**: 4-6 hours
**Dependencies**: Task 2.5 (Security Pattern)
**Assignee**: Security Engineer

**Description**:
Implement comprehensive security audit logging for all external access attempts and security events.

**Acceptance Criteria**:
- [ ] All external API access logged
- [ ] All authentication attempts logged
- [ ] All authorization failures logged
- [ ] Structured logging with correlation IDs
- [ ] Security events sent to SIEM
- [ ] PII/sensitive data not logged
- [ ] Audit log retention policy

**Security Audit Pattern**:

```java
// SecurityAuditService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final KafkaTemplate<String, SecurityAuditEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private static final String AUDIT_TOPIC = "security-audit-events";

    public void logAuthenticationAttempt(AuthenticationEvent event) {
        SecurityAuditEvent auditEvent = SecurityAuditEvent.builder()
            .eventType("AUTHENTICATION_ATTEMPT")
            .userId(maskPII(event.userId()))
            .ipAddress(event.ipAddress())
            .userAgent(event.userAgent())
            .success(event.success())
            .failureReason(event.failureReason())
            .timestamp(Instant.now())
            .correlationId(event.correlationId())
            .build();

        log.info("Security Audit: {}", auditEvent);
        kafkaTemplate.send(AUDIT_TOPIC, auditEvent.correlationId(), auditEvent);

        // Record metrics
        Counter.builder("security.authentication.attempts")
            .tag("success", String.valueOf(event.success()))
            .register(meterRegistry)
            .increment();
    }

    public void logAuthorizationFailure(AuthorizationEvent event) {
        SecurityAuditEvent auditEvent = SecurityAuditEvent.builder()
            .eventType("AUTHORIZATION_FAILURE")
            .userId(maskPII(event.userId()))
            .resource(event.resource())
            .action(event.action())
            .requiredRole(event.requiredRole())
            .userRoles(event.userRoles())
            .timestamp(Instant.now())
            .correlationId(event.correlationId())
            .build();

        log.warn("Security Audit: Authorization Failed - {}", auditEvent);
        kafkaTemplate.send(AUDIT_TOPIC, auditEvent.correlationId(), auditEvent);

        // Record metrics
        Counter.builder("security.authorization.failures")
            .tag("resource", event.resource())
            .tag("action", event.action())
            .register(meterRegistry)
            .increment();
    }

    private String maskPII(String value) {
        // Mask PII (email, phone, etc.) for compliance
        return value.replaceAll("(.{3}).*(.{3}@.*)", "$1***$2");
    }
}
```

**Implementation Steps**:
1. Create SecurityAuditService.java
2. Create SecurityAuditEvent record
3. Integrate with SecurityMediator
4. Add audit logging to authentication/authorization
5. Configure Kafka topic for audit events
6. Add security metrics
7. Write audit tests

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/security/
├── SecurityAuditService.java
├── SecurityAuditEvent.java
├── AuthenticationEvent.java
└── AuthorizationEvent.java
```

**Verification Commands**:
```bash
# Run security tests
./gradlew :market-data-service:test --tests "*Security*"

# Check Kafka for audit events
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic security-audit-events --from-beginning

# Check Prometheus metrics
curl http://localhost:8084/actuator/prometheus | grep security
```

---

### Task 4.3: Implement SLA Monitoring and Alerting

**Priority**: P3
**Estimated Effort**: 6-10 hours
**Dependencies**: Task 3.3 (Performance Benchmarks)
**Assignee**: DevOps Engineer + Senior Developer

**Description**:
Implement SLA monitoring with Prometheus metrics and Grafana dashboards with alerting for SLA violations.

**Acceptance Criteria**:
- [ ] Prometheus metrics for all operations
- [ ] SLA compliance tracking
- [ ] Grafana dashboards created
- [ ] AlertManager rules configured
- [ ] PagerDuty integration (optional)
- [ ] SLA violation alerts
- [ ] SLA reports generated

**Prometheus Metrics**:

```java
// SLAMonitoringService.java
@Service
@RequiredArgsConstructor
public class SLAMonitoringService {

    private final MeterRegistry meterRegistry;

    // SLA Thresholds (from Golden Spec)
    private static final long CRITICAL_SLA_MS = 25;
    private static final long HIGH_SLA_MS = 50;
    private static final long STANDARD_SLA_MS = 100;

    public void recordOperationDuration(String operation, String slaLevel, Duration duration) {
        // Record operation duration
        Timer.builder("market_data.operation.duration")
            .tag("operation", operation)
            .tag("sla_level", slaLevel)
            .description("Operation execution time")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(meterRegistry)
            .record(duration);

        // Check SLA compliance
        long slaThreshold = getSLAThreshold(slaLevel);
        boolean violated = duration.toMillis() > slaThreshold;

        if (violated) {
            recordSLAViolation(operation, slaLevel, duration.toMillis(), slaThreshold);
        }

        // Record SLA compliance percentage
        Gauge.builder("market_data.sla.compliance", this, s -> calculateCompliancePercentage(operation))
            .tag("operation", operation)
            .tag("sla_level", slaLevel)
            .description("SLA compliance percentage")
            .register(meterRegistry);
    }

    private void recordSLAViolation(String operation, String slaLevel, long actualMs, long thresholdMs) {
        Counter.builder("market_data.sla.violations")
            .tag("operation", operation)
            .tag("sla_level", slaLevel)
            .description("SLA violation count")
            .register(meterRegistry)
            .increment();

        Timer.builder("market_data.sla.violation.duration")
            .tag("operation", operation)
            .tag("sla_level", slaLevel)
            .description("Duration of SLA violations")
            .register(meterRegistry)
            .record(Duration.ofMillis(actualMs - thresholdMs));

        log.warn("SLA VIOLATION: operation={}, slaLevel={}, actual={}ms, threshold={}ms",
            operation, slaLevel, actualMs, thresholdMs);
    }

    private long getSLAThreshold(String slaLevel) {
        return switch (slaLevel) {
            case "CRITICAL" -> CRITICAL_SLA_MS;
            case "HIGH" -> HIGH_SLA_MS;
            case "STANDARD" -> STANDARD_SLA_MS;
            default -> Long.MAX_VALUE;
        };
    }

    private double calculateCompliancePercentage(String operation) {
        // Calculate from metrics registry
        // (total_requests - violations) / total_requests * 100
        return 99.5; // Placeholder
    }
}
```

**Grafana Dashboard JSON**:

```json
{
  "dashboard": {
    "title": "Market Data Service - SLA Monitoring",
    "panels": [
      {
        "title": "Operation Duration (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, market_data_operation_duration_bucket{sla_level=\"CRITICAL\"})",
            "legendFormat": "{{operation}} (CRITICAL <25ms)"
          }
        ],
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [25],
                "type": "gt"
              }
            }
          ]
        }
      },
      {
        "title": "SLA Compliance %",
        "targets": [
          {
            "expr": "market_data_sla_compliance",
            "legendFormat": "{{operation}} - {{sla_level}}"
          }
        ]
      },
      {
        "title": "SLA Violations",
        "targets": [
          {
            "expr": "rate(market_data_sla_violations_total[5m])",
            "legendFormat": "{{operation}} violations/sec"
          }
        ]
      }
    ]
  }
}
```

**AlertManager Rules**:

```yaml
# prometheus/alert-rules.yml
groups:
  - name: market_data_sla
    interval: 30s
    rules:
      - alert: CriticalSLAViolation
        expr: histogram_quantile(0.95, market_data_operation_duration_bucket{sla_level="CRITICAL"}) > 0.025
        for: 2m
        labels:
          severity: critical
          service: market-data-service
        annotations:
          summary: "Critical SLA violation detected"
          description: "{{ $labels.operation }} p95 latency is {{ $value }}s (threshold: 25ms)"

      - alert: SLAComplianceBelow95Percent
        expr: market_data_sla_compliance < 95
        for: 5m
        labels:
          severity: warning
          service: market-data-service
        annotations:
          summary: "SLA compliance below 95%"
          description: "{{ $labels.operation }} SLA compliance is {{ $value }}%"
```

**Implementation Steps**:
1. Create SLAMonitoringService.java
2. Integrate with all service methods
3. Create Grafana dashboard JSON
4. Configure Prometheus scraping
5. Configure AlertManager rules
6. Test SLA alerts
7. Document SLA targets

**Files to Create**:
```
src/main/java/com/trademaster/marketdata/monitoring/
├── SLAMonitoringService.java
├── SLAMetrics.java

monitoring/grafana/dashboards/
├── market-data-sla-dashboard.json

monitoring/prometheus/
├── alert-rules.yml
```

**Verification Commands**:
```bash
# Check Prometheus metrics
curl http://localhost:8084/actuator/prometheus | grep market_data

# Query Prometheus
curl 'http://localhost:9090/api/v1/query?query=market_data_operation_duration_bucket'

# View Grafana dashboard
open http://localhost:3000/d/market-data-sla

# Trigger SLA violation test
ab -n 1000 -c 100 http://localhost:8084/api/v1/market-data/RELIANCE/price
```

---

### Task 4.4: E2E Tests with Playwright (Rule #20)

**Priority**: P3
**Estimated Effort**: 10-16 hours
**Dependencies**: Phase 3 complete
**Assignee**: QA Engineer

**Description**:
Implement end-to-end tests for critical user journeys using Playwright or similar framework.

**Acceptance Criteria**:
- [ ] E2E test coverage >50%
- [ ] WebSocket subscription tests
- [ ] Real-time data flow tests
- [ ] API endpoint tests
- [ ] Performance tests
- [ ] All tests pass
- [ ] Test execution <10 minutes

**E2E Test Scenarios**:

1. **Real-Time Data Subscription**:
   ```javascript
   test('should subscribe and receive real-time price updates', async () => {
     // Connect to WebSocket
     const ws = await connectToWebSocket('ws://localhost:8084/ws/market-data');

     // Subscribe to symbols
     ws.send(JSON.stringify({
       type: 'subscribe',
       data: {
         symbols: ['RELIANCE', 'TCS', 'INFY'],
         dataTypes: ['TICK'],
         updateFrequency: 1000
       }
     }));

     // Wait for subscription confirmation
     const confirmation = await waitForMessage(ws, 'subscribed');
     expect(confirmation.status).toBe('success');

     // Receive real-time updates
     const updates = await collectMessages(ws, 10, 30000); // 10 messages, 30s timeout
     expect(updates.length).toBe(10);
     expect(updates[0].type).toBe('marketData');
   });
   ```

2. **Price Alert Lifecycle**:
   ```javascript
   test('should create, trigger, and delete price alert', async () => {
     // Create alert
     const createResponse = await api.post('/api/v1/market-data/alerts', {
       symbol: 'RELIANCE',
       targetPrice: 2500.00,
       triggerCondition: 'ABOVE',
       priority: 'HIGH'
     });
     expect(createResponse.status).toBe(201);

     const alertId = createResponse.data.alertId;

     // Simulate price update that triggers alert
     // ... trigger logic ...

     // Verify alert triggered
     const alertStatus = await api.get(`/api/v1/market-data/alerts/${alertId}`);
     expect(alertStatus.data.status).toBe('TRIGGERED');

     // Delete alert
     const deleteResponse = await api.delete(`/api/v1/market-data/alerts/${alertId}`);
     expect(deleteResponse.status).toBe(204);
   });
   ```

3. **Historical Data Retrieval**:
   ```javascript
   test('should retrieve historical OHLC data', async () => {
     const response = await api.post('/api/v1/market-data/historical', {
       symbols: ['RELIANCE'],
       timeframe: '1h',
       from: '2024-01-01T00:00:00Z',
       to: '2024-01-31T23:59:59Z'
     });

     expect(response.status).toBe(200);
     expect(response.data.symbols).toContain('RELIANCE');
     expect(response.data.data.RELIANCE.length).toBeGreaterThan(0);

     // Verify OHLC structure
     const ohlc = response.data.data.RELIANCE[0];
     expect(ohlc).toHaveProperty('open');
     expect(ohlc).toHaveProperty('high');
     expect(ohlc).toHaveProperty('low');
     expect(ohlc).toHaveProperty('close');
     expect(ohlc).toHaveProperty('volume');
   });
   ```

**Implementation Steps**:
1. Set up Playwright test framework
2. Write WebSocket subscription tests
3. Write price alert E2E tests
4. Write historical data tests
5. Write performance tests
6. Run E2E test suite
7. Generate test reports

**Files to Create**:
```
src/e2e/
├── tests/
│   ├── websocket.spec.js
│   ├── price-alerts.spec.js
│   ├── historical-data.spec.js
│   └── performance.spec.js
├── fixtures/
│   ├── test-data.json
│   └── mock-responses.json
└── playwright.config.js
```

**Verification Commands**:
```bash
# Run E2E tests
npm run test:e2e

# Run specific test suite
npm run test:e2e -- --grep "websocket"

# Generate HTML report
npm run test:e2e -- --reporter=html
```

---

### Task 4.5: Documentation Updates

**Priority**: P3
**Estimated Effort**: 4-8 hours
**Dependencies**: All Phase 4 tasks
**Assignee**: Technical Writer + Senior Developer

**Description**:
Update all documentation to reflect implemented changes and production readiness.

**Acceptance Criteria**:
- [ ] README updated
- [ ] API documentation updated
- [ ] Architecture diagrams updated
- [ ] Deployment guide created
- [ ] Troubleshooting guide created
- [ ] Performance benchmarks documented
- [ ] SLA targets documented

**Documentation to Update**:

1. **README.md**:
   ```markdown
   # Market Data Service

   ## Overview
   Production-ready market data service with real-time data feeds, circuit breaker protection, and <25ms SLA for critical operations.

   ## Features
   - ✅ Real-time data from BSE, NSE, MCX
   - ✅ WebSocket subscriptions
   - ✅ Price alerts with notifications
   - ✅ Historical OHLC data
   - ✅ Circuit breakers on all external calls
   - ✅ >80% test coverage
   - ✅ SLA monitoring and alerting

   ## Performance SLAs
   - Critical Operations: <25ms (p95)
   - High Priority: <50ms (p95)
   - Standard Operations: <100ms (p95)

   ## Getting Started
   ...
   ```

2. **API_DOCUMENTATION.md**:
   - Update all endpoints
   - Add request/response examples
   - Document error codes
   - Add authentication examples

3. **DEPLOYMENT_GUIDE.md**:
   - Docker deployment
   - Kubernetes deployment
   - Environment variables
   - Health check configuration
   - Monitoring setup

4. **TROUBLESHOOTING.md**:
   - Common issues and solutions
   - Circuit breaker states
   - Performance debugging
   - Log analysis

**Implementation Steps**:
1. Update README.md
2. Update API_DOCUMENTATION.md
3. Create DEPLOYMENT_GUIDE.md
4. Create TROUBLESHOOTING.md
5. Update architecture diagrams
6. Document performance benchmarks
7. Review and publish

**Files to Create/Update**:
```
market-data-service/
├── README.md                     (UPDATE)
├── API_DOCUMENTATION.md          (UPDATE)
├── DEPLOYMENT_GUIDE.md           (CREATE)
├── TROUBLESHOOTING.md            (CREATE)
├── PERFORMANCE_BENCHMARKS.md     (CREATE)
└── docs/
    ├── architecture/
    │   ├── overview.md           (UPDATE)
    │   └── diagrams/             (UPDATE)
    └── api/
        └── openapi.yaml          (UPDATE)
```

---

## Task Dependencies

### Dependency Graph

```
Phase 1 (Immediate Blockers)
├── Task 1.1: Fix Common Library Dependency
│   └── Task 1.2: Delete Duplicate Code
│       └── Task 1.3: Split God Classes
│
Phase 2 (Critical Issues)
├── Task 2.1: Eliminate If-Else  ──┐
├── Task 2.2: Eliminate Loops     ──┼──> Task 2.3: Fix Warnings
│                                   │
├── Phase 1 Complete ───────────────┼──> Task 2.4: Unit Tests
│                                   │
└── Phase 1 Complete ───────────────┴──> Task 2.5: Security Pattern
    │
    └──> Task 2.6: Extract Constants

Phase 3 (Quality Improvements)
├── Phase 2 Complete + Common Lib ──> Task 3.1: Result/Try Error Handling
│   └──> Task 3.2: Validation Framework
│
├── Phase 2 Complete ──> Task 3.3: Performance Benchmarks
│
└── Task 2.4 Complete ──> Task 3.4: Integration Tests

Phase 4 (Production Completeness)
├── Phase 2 Complete ──> Task 4.1: MCX Provider
│
├── Task 2.5 Complete ──> Task 4.2: Security Audit Logging
│
├── Task 3.3 Complete ──> Task 4.3: SLA Monitoring
│
├── Phase 3 Complete ──> Task 4.4: E2E Tests
│
└── All Tasks Complete ──> Task 4.5: Documentation
```

### Critical Path

```
Task 1.1 (2h)
  → Task 1.2 (4h)
  → Task 1.3 (8h)
  → Task 2.1 (24h)
  → Task 2.2 (16h)
  → Task 2.4 (56h)
  → Task 3.4 (12h)
  → Task 4.4 (16h)

Total Critical Path: 138 hours (17.25 working days)
```

### Parallel Execution Opportunities

**After Phase 1 Complete**:
- Task 2.1, 2.2, 2.3 can run in parallel (developer time permitting)
- Task 2.5, 2.6 can run in parallel
- Task 2.4 starts after 2.1, 2.2, 2.3 complete

**After Phase 2 Complete**:
- Task 3.1, 3.3 can run in parallel
- Task 4.1 can start independently
- Task 4.2 depends on Task 2.5

**After Phase 3 Complete**:
- Task 4.3 can start
- Task 4.4 can start
- Both can run in parallel

**Final Phase**:
- Task 4.5 requires all other tasks complete

---

## Resource Allocation

### Team Structure

**Senior Java Developer** (Full-time, 6-8 weeks):
- Task 1.1, 1.2, 1.3 (Phase 1)
- Task 2.1, 2.2, 2.5 (Phase 2)
- Task 3.1, 3.3 (Phase 3)
- Technical oversight for all tasks

**Mid-level Java Developer** (Part-time, 4-6 weeks):
- Task 2.6 (Phase 2)
- Task 3.2 (Phase 3)
- Task 4.1 (Phase 4)
- Support for test implementation

**QA Engineer** (Part-time, 3-4 weeks):
- Task 2.4 (Phase 2 - Unit Tests)
- Task 3.4 (Phase 3 - Integration Tests)
- Task 4.4 (Phase 4 - E2E Tests)
- Test coverage analysis

**DevOps Engineer** (Part-time, 2-3 weeks):
- Task 4.3 (SLA Monitoring)
- Deployment automation
- CI/CD pipeline updates

**Security Engineer** (Part-time, 1-2 weeks):
- Task 4.2 (Security Audit Logging)
- Security review and validation

**Technical Writer** (Part-time, 1 week):
- Task 4.5 (Documentation)
- API documentation
- User guides

### Sprint Planning

**Sprint 1 (2 weeks)**:
- Phase 1: Immediate Blockers (6-14 hours)
- Phase 2: Start critical issues (30-40 hours)
- Focus: Fix build, remove duplicates, split services, start functional patterns

**Sprint 2 (2 weeks)**:
- Phase 2: Continue critical issues (50-72 hours)
- Phase 3: Start quality improvements (10-15 hours)
- Focus: Complete functional patterns, achieve >80% test coverage

**Sprint 3 (2 weeks)**:
- Phase 3: Complete quality improvements (14-27 hours)
- Phase 4: Start production features (15-25 hours)
- Focus: Integration tests, MCX provider, security audit

**Sprint 4 (2 weeks)**:
- Phase 4: Complete production features (17-27 hours)
- Final validation and documentation
- Focus: SLA monitoring, E2E tests, documentation

### Estimated Timeline

```
Week 1-2:  Phase 1 + Phase 2 (partial)
Week 3-4:  Phase 2 (complete) + Phase 3 (partial)
Week 5-6:  Phase 3 (complete) + Phase 4 (partial)
Week 7-8:  Phase 4 (complete) + Final validation

Total: 6-8 weeks to production-ready state
```

---

## Success Criteria

### Phase 1 Success Criteria
- [ ] Build succeeds: `./gradlew :market-data-service:build`
- [ ] Zero compilation errors
- [ ] Common library properly integrated
- [ ] All services <200 lines
- [ ] Max 10 methods per class

### Phase 2 Success Criteria
- [ ] Zero if-else statements
- [ ] Zero for/while loops
- [ ] Zero compiler warnings
- [ ] >80% unit test coverage
- [ ] Security pattern implemented
- [ ] All constants extracted

### Phase 3 Success Criteria
- [ ] Result/Try error handling throughout
- [ ] Validation framework integrated
- [ ] Performance benchmarks meet SLA
- [ ] >70% integration test coverage

### Phase 4 Success Criteria
- [ ] MCX data provider operational
- [ ] Security audit logging active
- [ ] SLA monitoring and alerting configured
- [ ] >50% E2E test coverage
- [ ] Documentation complete

### Production Readiness Checklist
- [ ] All 27 mandatory rules compliant
- [ ] Golden specification compliance
- [ ] Common library fully integrated
- [ ] Test coverage targets met
- [ ] Performance SLAs achieved
- [ ] Security standards met
- [ ] Documentation complete
- [ ] Monitoring and alerting configured
- [ ] Deployment guide available
- [ ] Team training complete

---

## Validation and Sign-Off

### Code Review Checklist
- [ ] All tasks completed
- [ ] All tests passing
- [ ] Code coverage >80% unit, >70% integration
- [ ] Zero compiler warnings
- [ ] Performance benchmarks meet SLA
- [ ] Security review passed
- [ ] Architecture review approved
- [ ] Documentation reviewed

### Stakeholder Sign-Off
- [ ] Engineering Lead: Code quality approved
- [ ] Architect: Design patterns approved
- [ ] Security: Security audit passed
- [ ] QA: Test coverage approved
- [ ] DevOps: Deployment ready
- [ ] Product: Features complete

---

**Document Version**: 1.0.0
**Last Updated**: 2025-01-18
**Next Review**: After Phase 1 completion
**Owner**: Engineering Team Lead
