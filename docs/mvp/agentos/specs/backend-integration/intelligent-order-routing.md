# Backend Integration Spec: Intelligent Order Routing Service

## 🎯 Overview

Enhance the existing Trading Service with intelligent order routing capabilities that optimize execution across multiple brokers. This spec builds on the current Spring Boot infrastructure and integrates with existing broker connection framework.

## 🏗️ Current Implementation Analysis

### Existing Backend Services (Implemented)
- ✅ `TradingServiceApplication` - Basic trading service structure
- ✅ `TradingController` - REST endpoints for order management  
- ✅ Order management database schema (V1 migration)
- ✅ Spring Boot 3.5.3 with Java 24 Virtual Threads
- ✅ JWT security integration
- ✅ PostgreSQL + Redis caching infrastructure

### Current Service Gaps
- ❌ Multi-broker routing algorithm
- ❌ Real-time execution optimization
- ❌ Order splitting and smart routing
- ❌ Execution quality analytics
- ❌ Intelligent broker selection

## 📋 Service Requirements

### Epic 2 Enhancement: Intelligent Order Router

#### 2.1 Order Routing Engine
**Service**: `IntelligentOrderRoutingService`
**Package**: `com.trademaster.trading.service.routing`
**Integration**: Extends existing trading service

```java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class IntelligentOrderRoutingService {
    
    private final BrokerCapabilityService brokerCapabilityService;
    private final ExecutionAnalyticsService executionAnalyticsService;
    private final RiskManagementService riskManagementService;
    private final OrderOptimizationAlgorithm optimizationAlgorithm;
    
    /**
     * Route order to optimal broker(s) based on multiple factors
     */
    public OrderRoutingResult routeOrder(OrderRequest orderRequest, 
                                       List<BrokerConnection> availableBrokers) {
        
        // 1. Pre-routing validation
        validateOrderRequest(orderRequest);
        
        // 2. Get broker capabilities and current status
        List<BrokerExecutionProfile> brokerProfiles = 
            getBrokerExecutionProfiles(availableBrokers, orderRequest.getSymbol());
        
        // 3. Calculate optimal routing strategy
        RoutingStrategy strategy = optimizationAlgorithm.calculateOptimalRouting(
            orderRequest, brokerProfiles
        );
        
        // 4. Execute routing decision
        return executeRoutingStrategy(strategy, orderRequest);
    }
}
```

#### 2.2 Broker Execution Profiling
**Entity**: `BrokerExecutionProfile`
**Package**: `com.trademaster.trading.entity.routing`

```java
@Entity
@Table(name = "broker_execution_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerExecutionProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String brokerId;
    
    @Column(nullable = false)
    private String symbol;
    
    // Execution Quality Metrics
    @Column(name = "avg_execution_time_ms")
    private Integer averageExecutionTimeMs;
    
    @Column(name = "fill_rate_percentage", precision = 5, scale = 2)
    private BigDecimal fillRatePercentage;
    
    @Column(name = "price_improvement_bps", precision = 8, scale = 4)
    private BigDecimal priceImprovementBps;
    
    @Column(name = "slippage_bps", precision = 8, scale = 4)
    private BigDecimal averageSlippageBps;
    
    // Current Market Conditions
    @Column(name = "current_spread_bps", precision = 8, scale = 4)
    private BigDecimal currentSpreadBps;
    
    @Column(name = "order_book_depth")
    private Long orderBookDepth;
    
    @Column(name = "recent_volume")
    private Long recentVolume;
    
    // Broker Capabilities
    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = OrderType.class)
    @CollectionTable(name = "broker_supported_order_types")
    private Set<OrderType> supportedOrderTypes;
    
    @Column(name = "max_order_size")
    private Long maxOrderSize;
    
    @Column(name = "min_order_size")  
    private Long minOrderSize;
    
    // Real-time Status
    @Enumerated(EnumType.STRING)
    private BrokerStatus status;
    
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    
    @Column(name = "error_rate_percentage", precision = 5, scale = 2)
    private BigDecimal errorRatePercentage;
    
    // Time tracking
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

enum BrokerStatus {
    AVAILABLE, BUSY, MAINTENANCE, OFFLINE, ERROR
}
```

#### 2.3 Routing Optimization Algorithm
**Service**: `OrderOptimizationAlgorithm`
**Pattern**: Strategy Pattern for different routing algorithms

```java
@Component
@Slf4j
public class OrderOptimizationAlgorithm {
    
    private final Map<RoutingObjective, RoutingStrategy> strategies;
    
    public OrderOptimizationAlgorithm() {
        strategies = Map.of(
            RoutingObjective.BEST_PRICE, new BestPriceRoutingStrategy(),
            RoutingObjective.FASTEST_EXECUTION, new FastestExecutionStrategy(),
            RoutingObjective.LOWEST_COST, new LowestCostStrategy(),
            RoutingObjective.SMART_BALANCE, new SmartBalanceStrategy()
        );
    }
    
    /**
     * Multi-factor optimization algorithm
     */
    public RoutingDecision calculateOptimalRouting(
            OrderRequest order, 
            List<BrokerExecutionProfile> brokerProfiles) {
        
        // 1. Score each broker across multiple dimensions
        List<BrokerScore> brokerScores = scoreBrokers(order, brokerProfiles);
        
        // 2. Apply order-specific routing strategy
        RoutingStrategy strategy = determineRoutingStrategy(order);
        
        // 3. Calculate optimal allocation
        List<OrderAllocation> allocations = strategy.calculateAllocations(
            order, brokerScores
        );
        
        // 4. Validate routing decision
        validateRoutingDecision(allocations, order);
        
        return RoutingDecision.builder()
                .orderId(order.getId())
                .strategy(strategy.getType())
                .allocations(allocations)
                .expectedExecution(calculateExpectedExecution(allocations))
                .confidenceScore(calculateConfidenceScore(allocations))
                .build();
    }
    
    private List<BrokerScore> scoreBrokers(OrderRequest order, 
                                         List<BrokerExecutionProfile> profiles) {
        return profiles.stream()
                .filter(profile -> canExecuteOrder(profile, order))
                .map(profile -> calculateBrokerScore(profile, order))
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .collect(Collectors.toList());
    }
    
    private BrokerScore calculateBrokerScore(BrokerExecutionProfile profile, 
                                           OrderRequest order) {
        
        double executionSpeedScore = calculateExecutionSpeedScore(profile);
        double priceQualityScore = calculatePriceQualityScore(profile);
        double reliabilityScore = calculateReliabilityScore(profile);  
        double liquidityScore = calculateLiquidityScore(profile, order);
        double costScore = calculateCostScore(profile, order);
        
        // Weighted composite score based on order characteristics
        double totalScore = 
            (executionSpeedScore * getSpeedWeight(order)) +
            (priceQualityScore * getPriceWeight(order)) +
            (reliabilityScore * getReliabilityWeight(order)) +
            (liquidityScore * getLiquidityWeight(order)) +
            (costScore * getCostWeight(order));
            
        return BrokerScore.builder()
                .brokerId(profile.getBrokerId())
                .executionSpeedScore(executionSpeedScore)
                .priceQualityScore(priceQualityScore)
                .reliabilityScore(reliabilityScore)
                .liquidityScore(liquidityScore)
                .costScore(costScore)
                .totalScore(totalScore)
                .build();
    }
}
```

#### 2.4 Real-time Execution Analytics
**Service**: `ExecutionAnalyticsService`
**Purpose**: Track and analyze execution quality

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionAnalyticsService {
    
    private final ExecutionMetricsRepository executionMetricsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Track order execution and update broker profiles
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<Void> trackExecution(OrderExecution execution) {
        
        // 1. Calculate execution metrics
        ExecutionMetrics metrics = calculateExecutionMetrics(execution);
        
        // 2. Update broker performance profile
        updateBrokerProfile(execution.getBrokerId(), metrics);
        
        // 3. Store execution data for analysis
        persistExecutionMetrics(metrics);
        
        // 4. Publish real-time updates
        publishExecutionUpdate(execution, metrics);
        
        // 5. Trigger routing optimization updates
        triggerRoutingOptimization(execution.getBrokerId());
        
        return CompletableFuture.completedFuture(null);
    }
    
    private ExecutionMetrics calculateExecutionMetrics(OrderExecution execution) {
        
        long executionLatency = Duration.between(
            execution.getOrderTime(), execution.getExecutionTime()
        ).toMillis();
        
        BigDecimal slippage = calculateSlippage(
            execution.getExpectedPrice(), 
            execution.getExecutedPrice(),
            execution.getSide()
        );
        
        BigDecimal priceImprovement = calculatePriceImprovement(
            execution.getMarketPrice(),
            execution.getExecutedPrice(),
            execution.getSide()
        );
        
        return ExecutionMetrics.builder()
                .orderId(execution.getOrderId())
                .brokerId(execution.getBrokerId())
                .symbol(execution.getSymbol())
                .executionLatencyMs(executionLatency)
                .slippageBps(slippage)
                .priceImprovementBps(priceImprovement)
                .fillRate(execution.getFillRate())
                .executionQuality(calculateExecutionQuality(slippage, priceImprovement))
                .build();
    }
}
```

## 🔗 Integration Points

### REST API Extensions
**Controller**: `IntelligentTradingController`
**Extends**: Existing `TradingController`

```java
@RestController
@RequestMapping("/api/v1/trading/intelligent")
@RequiredArgsConstructor
@Validated
public class IntelligentTradingController {
    
    private final IntelligentOrderRoutingService routingService;
    private final ExecutionAnalyticsService analyticsService;
    
    /**
     * Submit order with intelligent routing
     */
    @PostMapping("/orders/smart")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<OrderRoutingResult> submitSmartOrder(
            @Valid @RequestBody SmartOrderRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        OrderRoutingResult result = routingService.routeAndExecuteOrder(
            request.toOrderRequest(userId)
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get routing recommendations for an order
     */
    @PostMapping("/orders/analyze")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<RoutingRecommendation> analyzeOrder(
            @Valid @RequestBody OrderAnalysisRequest request) {
        
        RoutingRecommendation recommendation = routingService.getRoutingRecommendation(
            request.toOrderRequest()
        );
        
        return ResponseEntity.ok(recommendation);
    }
    
    /**
     * Get execution analytics for user's orders
     */
    @GetMapping("/analytics/execution")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<ExecutionAnalytics> getExecutionAnalytics(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        String userId = authentication.getName();
        ExecutionAnalytics analytics = analyticsService.getUserExecutionAnalytics(
            userId, days
        );
        
        return ResponseEntity.ok(analytics);
    }
}
```

### WebSocket Integration
**Handler**: `OrderRoutingWebSocketHandler`
**Purpose**: Real-time routing updates

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderRoutingWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessionManager.addSession(userId, session);
        
        // Send initial routing status
        sendRoutingStatus(session);
        
        log.info("Order routing WebSocket connected for user: {}", userId);
    }
    
    /**
     * Send real-time routing updates
     */
    public void sendRoutingUpdate(String userId, OrderRoutingUpdate update) {
        sessionManager.getSessionsForUser(userId).forEach(session -> {
            try {
                String message = objectMapper.writeValueAsString(
                    WebSocketMessage.builder()
                        .type("ROUTING_UPDATE")
                        .data(update)
                        .timestamp(Instant.now())
                        .build()
                );
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send routing update", e);
            }
        });
    }
}
```

### Kafka Event Integration
**Events**: Order routing and execution events

```java
// Order Routing Events
@KafkaListener(topics = "order-routing-decisions")
public void handleRoutingDecision(OrderRoutingEvent event) {
    log.info("Processing routing decision for order: {}", event.getOrderId());
    
    // Update routing metrics
    analyticsService.updateRoutingMetrics(event);
    
    // Send WebSocket updates
    webSocketHandler.sendRoutingUpdate(event.getUserId(), event.toUpdate());
}

@KafkaListener(topics = "order-executions")
public void handleOrderExecution(OrderExecutionEvent event) {
    log.info("Processing order execution: {}", event.getOrderId());
    
    // Track execution quality
    analyticsService.trackExecution(event.toExecution());
    
    // Update broker profiles
    brokerProfileService.updateFromExecution(event);
}
```

## 🎯 Database Schema Extensions

### Migration: V2__Add_intelligent_routing_tables.sql
```sql
-- Broker execution profiles
CREATE TABLE broker_execution_profiles (
    id BIGSERIAL PRIMARY KEY,
    broker_id VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    avg_execution_time_ms INTEGER,
    fill_rate_percentage DECIMAL(5,2),
    price_improvement_bps DECIMAL(8,4),
    slippage_bps DECIMAL(8,4),
    current_spread_bps DECIMAL(8,4),
    order_book_depth BIGINT,
    recent_volume BIGINT,
    max_order_size BIGINT,
    min_order_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    response_time_ms INTEGER,
    error_rate_percentage DECIMAL(5,2),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(broker_id, symbol)
);

-- Order routing decisions
CREATE TABLE order_routing_decisions (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    routing_strategy VARCHAR(30) NOT NULL,
    confidence_score DECIMAL(5,3),
    expected_execution_time_ms INTEGER,
    expected_slippage_bps DECIMAL(8,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order allocations (for split orders)
CREATE TABLE order_allocations (
    id BIGSERIAL PRIMARY KEY,
    routing_decision_id BIGINT NOT NULL REFERENCES order_routing_decisions(id),
    broker_id VARCHAR(50) NOT NULL,
    allocation_percentage DECIMAL(5,2) NOT NULL,
    expected_execution_price DECIMAL(12,4),
    priority INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

-- Execution metrics
CREATE TABLE execution_metrics (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    broker_id VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    execution_latency_ms BIGINT,
    slippage_bps DECIMAL(8,4),
    price_improvement_bps DECIMAL(8,4),
    fill_rate DECIMAL(5,2),
    execution_quality_score DECIMAL(5,3),
    market_conditions JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_broker_profiles_broker_symbol ON broker_execution_profiles(broker_id, symbol);
CREATE INDEX idx_routing_decisions_user_id ON order_routing_decisions(user_id);
CREATE INDEX idx_routing_decisions_created_at ON order_routing_decisions(created_at);
CREATE INDEX idx_execution_metrics_broker_symbol ON execution_metrics(broker_id, symbol);
CREATE INDEX idx_execution_metrics_created_at ON execution_metrics(created_at);
```

## 🧪 Testing Strategy

### Unit Testing
```java
@ExtendWith(MockitoExtension.class)
class IntelligentOrderRoutingServiceTest {
    
    @Mock
    private BrokerCapabilityService brokerCapabilityService;
    
    @Mock
    private ExecutionAnalyticsService executionAnalyticsService;
    
    @InjectMocks
    private IntelligentOrderRoutingService routingService;
    
    @Test
    void shouldRouteOrderToOptimalBroker() {
        // Given
        OrderRequest order = createTestOrder();
        List<BrokerConnection> brokers = createTestBrokers();
        
        // When
        OrderRoutingResult result = routingService.routeOrder(order, brokers);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAllocations()).hasSize(1);
        assertThat(result.getConfidenceScore()).isGreaterThan(0.7);
    }
    
    @Test
    void shouldSplitLargeOrderAcrossMultipleBrokers() {
        // Given
        OrderRequest largeOrder = createLargeOrder(1000000);
        List<BrokerConnection> brokers = createMultipleBrokers();
        
        // When
        OrderRoutingResult result = routingService.routeOrder(largeOrder, brokers);
        
        // Then
        assertThat(result.getAllocations()).hasSizeGreaterThan(1);
        assertThat(result.getAllocations().stream()
            .mapToDouble(OrderAllocation::getAllocationPercentage)
            .sum()).isEqualTo(100.0, offset(0.01));
    }
}
```

### Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IntelligentTradingControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("trademaster_test")
            .withUsername("test")
            .withPassword("test");
            
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Test
    void shouldSubmitSmartOrderSuccessfully() throws Exception {
        SmartOrderRequest request = SmartOrderRequest.builder()
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .quantity(100L)
                .orderType(OrderType.MARKET)
                .routingObjective(RoutingObjective.SMART_BALANCE)
                .build();
        
        mockMvc.perform(post("/api/v1/trading/intelligent/orders/smart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocations").isArray())
                .andExpect(jsonPath("$.confidenceScore").isNumber());
    }
}
```

## 📊 Performance Requirements

### Service Level Objectives (SLOs)
- **Routing Decision Time**: < 50ms for 95th percentile
- **Order Execution Time**: < 200ms for 90th percentile  
- **System Throughput**: 1000+ orders per second
- **Availability**: 99.95% uptime during market hours

### Caching Strategy
- **Broker Profiles**: Redis cache with 5-minute TTL
- **Market Data**: In-memory cache with 1-second TTL
- **Routing Decisions**: Database persistence for audit trail
- **Analytics**: Pre-computed metrics with hourly updates

### Monitoring & Observability
```java
// Metrics collection
@Component
@RequiredArgsConstructor
public class RoutingMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter routingDecisions = Counter.builder("routing.decisions")
            .description("Total routing decisions made")
            .register(meterRegistry);
            
    private final Timer routingLatency = Timer.builder("routing.latency")
            .description("Time to make routing decision")
            .register(meterRegistry);
            
    private final Gauge activeConnections = Gauge.builder("broker.connections.active")
            .description("Active broker connections")
            .register(meterRegistry, this, RoutingMetrics::getActiveBrokerConnections);
            
    public void recordRoutingDecision(String strategy, Duration latency) {
        routingDecisions.increment(Tags.of("strategy", strategy));
        routingLatency.record(latency);
    }
}
```

## 🚀 Implementation Timeline

### Phase 1: Core Routing Engine (Week 1-2)
- Implement `IntelligentOrderRoutingService`
- Create broker profiling system
- Basic routing algorithms (best price, fastest execution)

### Phase 2: Advanced Analytics (Week 3-4)
- Build execution analytics service
- Implement real-time performance tracking
- Add broker score optimization

### Phase 3: Integration & Testing (Week 5-6)
- WebSocket real-time updates
- Kafka event processing
- Performance testing and optimization

**Ready to revolutionize order execution! ⚡🎯**