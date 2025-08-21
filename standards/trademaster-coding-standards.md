# TradeMaster Coding Standards v2.0

## Core Technology Stack

### **MANDATORY: Java 24 + Virtual Threads Architecture**

**Primary Principle**: Use Java 24 Virtual Threads for unlimited scalability instead of reactive programming.

#### Technology Requirements

1. **Java Version**: Java 24 with `--enable-preview` flag
2. **Concurrency Model**: Virtual Threads (millions of threads without performance penalty)
3. **Spring Framework**: Spring Boot 3.5.3 + Spring MVC (NO WebFlux/Reactive)
4. **Database Access**: JPA/Hibernate with HikariCP (NO R2DBC)
5. **HTTP Clients**: OkHttp or Apache HttpClient5 (NO WebClient)
6. **Monitoring**: Prometheus metrics with comprehensive business and system metrics
7. **Logging**: Structured JSON logging with correlation IDs and context preservation

#### Performance Benefits

- **Virtual Threads**: Handle millions of concurrent requests without blocking
- **Simpler Code**: Standard blocking I/O with thread-per-request model
- **Better Debugging**: Standard stack traces and debugging tools
- **Easier Testing**: No complex reactive testing frameworks needed
- **Maintainability**: Familiar synchronous programming model

---

## Architecture Standards

### 1. Service Architecture

```java
// ✅ CORRECT: Virtual Thread Service
@Service
public class TradingService {
    
    @Async  // Runs on Virtual Thread
    public CompletableFuture<Order> placeOrder(OrderRequest request) {
        // Standard blocking calls - no performance penalty with Virtual Threads
        validateOrder(request);           // Blocking validation
        checkRiskLimits(request);        // Blocking database call
        Order order = orderRepository.save(order);  // Blocking JPA save
        notifyBroker(order);             // Blocking HTTP call
        return CompletableFuture.completedFuture(order);
    }
}
```

```java
// ❌ WRONG: Reactive Approach (Deprecated)
@Service
public class TradingService {
    public Mono<Order> placeOrder(OrderRequest request) {
        return validateOrder(request)
            .flatMap(this::checkRiskLimits)
            .flatMap(orderRepository::save)
            .flatMap(this::notifyBroker);
    }
}
```

### 2. Controller Standards

```java
// ✅ CORRECT: Spring MVC with Virtual Threads
@RestController
public class TradingController {
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        // Blocking call - Virtual Thread handles concurrency
        OrderResponse response = tradingService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/orders/{orderId}")
    public CompletableFuture<ResponseEntity<OrderResponse>> getOrder(@PathVariable String orderId) {
        // Async with Virtual Threads for high concurrency
        return CompletableFuture.supplyAsync(() -> {
            OrderResponse order = tradingService.getOrder(orderId);
            return ResponseEntity.ok(order);
        });
    }
}
```

### 3. Repository Standards

```java
// ✅ CORRECT: JPA Repository with Virtual Threads
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderId(String orderId);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);
    
    // Async methods using Virtual Threads
    default CompletableFuture<Optional<Order>> findByOrderIdAsync(String orderId) {
        return CompletableFuture.supplyAsync(() -> findByOrderId(orderId));
    }
}
```

### 4. Configuration Standards

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true  # MANDATORY
  
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
    
  jpa:
    properties:
      hibernate:
        connection:
          handling_mode: delayed_acquisition_and_release_after_transaction
```

```gradle
// build.gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)  // MANDATORY
    }
}

tasks.named('compileJava') {
    options.compilerArgs += ['--enable-preview']  // MANDATORY
}
```

---

## Code Quality Standards

### 1. Entity Design

```java
// ✅ CORRECT: JPA Entity with proper annotations
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_symbol", columnList = "symbol")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", unique = true, nullable = false, length = 50)
    private String orderId;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

### 2. Async Processing with Virtual Threads

```java
// ✅ CORRECT: Virtual Thread Async Processing
@Service
public class MarketDataService {
    
    @Async
    @EventListener
    public void handlePriceUpdate(PriceUpdateEvent event) {
        // Process price update - runs on Virtual Thread
        updatePortfolioValues(event.getSymbol(), event.getPrice());
        broadcastToWebSocketClients(event);
        updateCache(event);
    }
    
    public CompletableFuture<List<MarketData>> getBulkPrices(List<String> symbols) {
        return CompletableFuture.supplyAsync(() -> {
            // Parallel processing with Virtual Threads
            return symbols.parallelStream()
                .map(this::getCurrentPrice)
                .collect(Collectors.toList());
        });
    }
}
```

### 3. HTTP Client Standards

```java
// ✅ CORRECT: OkHttp with Virtual Threads
@Component
public class BrokerApiClient {
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .build();
    
    public BrokerResponse placeOrder(BrokerOrderRequest request) {
        // Blocking HTTP call - Virtual Thread makes it non-blocking at scale
        Request httpRequest = new Request.Builder()
            .url(brokerApiUrl)
            .post(RequestBody.create(json, MediaType.JSON))
            .build();
            
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return parseResponse(response);
        }
    }
}
```

---

## Performance Standards

### 1. Response Time Targets

| Operation | Target | Virtual Thread Benefit |
|-----------|---------|------------------------|
| Order Placement | <50ms | Concurrent risk checks |
| Order Lookup | <25ms | Parallel cache/DB queries |
| Market Data Query | <100ms | Concurrent provider calls |
| Portfolio Update | <10ms | Parallel position calculations |

### 2. Concurrency Targets

| Metric | Target | Virtual Thread Advantage |
|--------|---------|--------------------------|
| Concurrent Users | 10,000+ | Unlimited threads |
| Orders/Second | 1,000+ | No thread pool limits |
| WebSocket Connections | 50,000+ | Thread-per-connection |
| Database Connections | 50 | Efficient connection reuse |

### 3. Memory Efficiency

- **Virtual Thread Stack**: ~8KB per thread (vs 2MB for platform threads)
- **Heap Usage**: Minimal increase with Virtual Threads
- **GC Pressure**: Reduced due to fewer object allocations

---

## Testing Standards

### 1. Unit Testing

```java
// ✅ CORRECT: Standard JUnit with Virtual Threads
@ExtendWith(MockitoExtension.class)
class TradingServiceTest {
    
    @Test
    void shouldPlaceOrderSuccessfully() {
        // Standard testing - no reactive complexity
        OrderRequest request = OrderRequest.builder()
            .symbol("AAPL")
            .quantity(100)
            .build();
            
        OrderResponse response = tradingService.placeOrder(request);
        
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    
    @Test
    void shouldHandleConcurrentOrders() {
        // Test concurrency with Virtual Threads
        List<CompletableFuture<OrderResponse>> futures = IntStream.range(0, 1000)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
                tradingService.placeOrder(createOrderRequest())))
            .toList();
            
        List<OrderResponse> responses = futures.stream()
            .map(CompletableFuture::join)
            .toList();
            
        assertThat(responses).hasSize(1000);
    }
}
```

### 2. Integration Testing

```java
// ✅ CORRECT: TestContainers with Virtual Threads
@SpringBootTest
@Testcontainers
class TradingIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("trademaster_test")
        .withUsername("test")
        .withPassword("test");
    
    @Test
    void shouldProcessOrdersUnderLoad() {
        // Load test with Virtual Threads
        int orderCount = 10000;
        
        List<CompletableFuture<Void>> tasks = IntStream.range(0, orderCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                OrderRequest request = createOrderRequest();
                tradingController.placeOrder(request);
            }))
            .toList();
            
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        
        long totalOrders = orderRepository.count();
        assertThat(totalOrders).isEqualTo(orderCount);
    }
}
```

---

## Migration Guidelines

### From Reactive to Virtual Threads

1. **Dependencies**: Remove WebFlux, add Spring MVC
2. **Repositories**: Replace R2DBC with JPA
3. **Controllers**: Replace Mono/Flux with standard return types
4. **Services**: Replace reactive chains with sequential calls
5. **HTTP Clients**: Replace WebClient with OkHttp
6. **Testing**: Replace reactor-test with standard testing

### Performance Verification

- **Load Testing**: Verify equivalent or better performance
- **Memory Usage**: Monitor heap and thread stack usage
- **Response Times**: Ensure targets are met
- **Concurrency**: Test with 10,000+ concurrent users

---

## Monitoring & Observability Standards

### **MANDATORY: Prometheus Metrics**

Every service MUST implement comprehensive Prometheus metrics for Grafana dashboards.

#### Required Metrics Categories

1. **Business Metrics**
   - Authentication success/failure rates
   - Order placement/execution rates
   - Portfolio valuation frequencies
   - User registration/activation counts
   - Trading volume and transaction counts

2. **Performance Metrics**
   - API response times (histogram)
   - Service throughput (counter)
   - Database query durations (timer)
   - Cache hit/miss ratios (gauge)
   - Virtual Thread utilization (gauge)

3. **System Health Metrics**
   - JVM memory usage and GC metrics
   - Database connection pool status
   - External service health checks
   - Error rates by endpoint (counter)
   - Circuit breaker states (gauge)

4. **Security Metrics**
   - Failed authentication attempts
   - Rate limit violations
   - Suspicious activity detections
   - MFA success/failure rates
   - Token validation metrics

#### Implementation Example

```java
// ✅ CORRECT: Comprehensive Metrics Configuration
@Component
public class ServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter authenticationAttempts;
    private final Counter authenticationSuccesses;
    private final Timer authenticationDuration;
    private final Counter securityIncidents;
    
    public ServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.authenticationAttempts = Counter.builder("auth.attempts")
            .description("Authentication attempts")
            .tag("service", "auth")
            .register(meterRegistry);
            
        this.authenticationDuration = Timer.builder("auth.duration")
            .description("Authentication processing time")
            .tag("service", "auth")
            .register(meterRegistry);
    }
    
    public void recordAuthenticationAttempt(String method, boolean success, long durationMs) {
        authenticationAttempts.increment(
            Tags.of("method", method, "status", success ? "success" : "failure")
        );
        authenticationDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### **MANDATORY: Structured Logging**

Every service MUST implement structured JSON logging for ELK stack integration.

#### Required Logging Standards

1. **Structured JSON Format**
   - Use Logstash encoder for JSON structure
   - Include correlation IDs for distributed tracing
   - Preserve context across Virtual Thread operations
   - Separate audit trails for compliance

2. **Context Preservation**
   - MDC for request correlation IDs
   - User context (user ID, session ID)
   - Security context (IP address, user agent)
   - Business context (portfolio ID, order ID)

3. **Log Categories**
   - **Application Logs**: General application events
   - **Security Logs**: Authentication, authorization, security incidents
   - **Audit Logs**: Business transactions for compliance
   - **Performance Logs**: Metrics and timing information

#### Implementation Example

```java
// ✅ CORRECT: Structured Logging Configuration
@Component
public class ServiceLogger {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceLogger.class);
    
    public void logAuthenticationSuccess(String userId, String sessionId, 
                                       String ipAddress, long durationMs) {
        log.info("Authentication successful",
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("sessionId", sessionId),
            StructuredArguments.kv("ipAddress", ipAddress),
            StructuredArguments.kv("durationMs", durationMs),
            StructuredArguments.kv("operation", "authentication"),
            StructuredArguments.kv("status", "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    public void logSecurityIncident(String incidentType, String severity, 
                                  String userId, Map<String, Object> details) {
        log.warn("Security incident detected",
            StructuredArguments.kv("incidentType", incidentType),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv("userId", userId),
            StructuredArguments.kv("operation", "security_incident"),
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }
}
```

#### Logback Configuration

```xml
<!-- ✅ CORRECT: Structured JSON Logging Configuration -->
<configuration>
    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp>
                        <timeZone>UTC</timeZone>
                        <fieldName>@timestamp</fieldName>
                    </timestamp>
                    <logLevel><fieldName>level</fieldName></logLevel>
                    <message><fieldName>message</fieldName></message>
                    <mdc><fieldName>mdc</fieldName></mdc>
                    <arguments><fieldName>arguments</fieldName></arguments>
                    <pattern>
                        <pattern>
                            {
                                "service": "service-name",
                                "environment": "${spring.profiles.active}",
                                "host": "${HOSTNAME}",
                                "thread": "%thread"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
        </appender>
    </springProfile>
</configuration>
```

#### Performance Requirements

- **Logging Overhead**: <0.1ms per log entry
- **No Blocking**: Async appenders for Virtual Thread compatibility
- **Correlation**: Maintain context across async operations
- **Retention**: 30 days application logs, 90 days security logs

---

## Error Handling Standards

```java
// ✅ CORRECT: Exception handling with Virtual Threads
@Service
public class TradingService {
    
    public OrderResponse placeOrder(OrderRequest request) {
        try {
            validateOrder(request);
            return processOrder(request);
        } catch (ValidationException e) {
            log.error("Order validation failed: {}", e.getMessage());
            throw new BadRequestException("Invalid order: " + e.getMessage());
        } catch (RiskCheckException e) {
            log.warn("Risk check failed for order: {}", e.getMessage());
            throw new ForbiddenException("Risk check failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing order: {}", e.getMessage(), e);
            throw new InternalServerException("Order processing failed");
        }
    }
}
```

---

## Security Standards

```java
// ✅ CORRECT: JWT Security with Virtual Threads
@Component
public class JwtAuthenticationManager {
    
    public Authentication authenticate(String token) {
        // Blocking JWT validation - Virtual Thread makes it scalable
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
            return createAuthentication(claims);
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid JWT token");
        }
    }
}
```

---

## Monitoring Standards

```java
// ✅ CORRECT: Metrics with Virtual Threads
@Component
public class TradingMetrics {
    
    private final Counter orderCounter = Counter.builder("orders.placed")
        .description("Number of orders placed")
        .register(meterRegistry);
        
    private final Timer orderProcessingTimer = Timer.builder("orders.processing.time")
        .description("Order processing time")
        .register(meterRegistry);
    
    @EventListener
    public void recordOrderPlaced(OrderPlacedEvent event) {
        // Metrics recording - runs on Virtual Thread
        orderCounter.increment(
            Tags.of(
                "symbol", event.getSymbol(),
                "side", event.getSide().toString()
            )
        );
    }
}
```

---

## Documentation Standards

1. **Code Comments**: Focus on business logic, not technical implementation
2. **API Documentation**: OpenAPI 3.0 with examples
3. **Performance Notes**: Document Virtual Thread benefits
4. **Migration Notes**: Explain changes from reactive approach

---

## Compliance Checklist

### ✅ Java 24 + Virtual Threads Compliance

- [ ] Java 24 with `--enable-preview` flag
- [ ] Spring Boot 3.5.3 + Spring MVC (NO WebFlux)
- [ ] JPA/Hibernate with HikariCP (NO R2DBC)
- [ ] `spring.threads.virtual.enabled=true`
- [ ] OkHttp or Apache HttpClient5 (NO WebClient)
- [ ] CompletableFuture for async operations
- [ ] Standard JUnit testing (NO reactor-test)
- [ ] Performance targets met
- [ ] Load testing with 10,000+ concurrent users
- [ ] Memory usage optimized

### ❌ Prohibited Technologies

- ❌ Spring WebFlux / Reactive programming
- ❌ R2DBC reactive database access
- ❌ WebClient reactive HTTP client
- ❌ Mono/Flux reactive types
- ❌ reactor-test testing framework
- ❌ Project Reactor dependencies

---

**Effective Date**: 2025-08-21  
**Version**: 2.0.0 (Java 24 + Virtual Threads)  
**Previous Version**: 1.0.0 (Reactive Programming - Deprecated)