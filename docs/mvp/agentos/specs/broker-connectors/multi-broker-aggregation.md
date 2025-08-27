# Broker Connector Spec: Multi-Broker Aggregation Framework

## 🎯 Overview

Enhance the existing broker integration framework to support real-time multi-broker portfolio aggregation, intelligent order routing, and unified risk management. This spec builds on Epic 5 (Broker Management) and integrates with the current Spring Boot microservices architecture.

## 🏗️ Current Implementation Analysis

### Existing Infrastructure (Implemented)
- ✅ **Authentication Service**: JWT tokens, MFA, device trust
- ✅ **Trading Service**: Basic order management framework
- ✅ **Portfolio Service**: Position tracking and analytics
- ✅ **Market Data Service**: Real-time price streaming via WebSocket
- ✅ **Spring Boot 3.5.3**: Java 24 with Virtual Threads
- ✅ **PostgreSQL + Redis**: Data persistence and caching

### Integration Gaps (From Epic 5)
- ❌ **OAuth Broker Integration**: Secure broker API connection management
- ❌ **Real-time Data Aggregation**: Multi-broker portfolio consolidation
- ❌ **Intelligent Routing**: Cross-broker order optimization
- ❌ **Connection Health Monitoring**: Broker status and failover management
- ❌ **Unified Risk Management**: Cross-broker position and risk tracking

## 📋 Service Architecture Requirements

### Epic 5 Implementation: Broker Connection Framework

#### 5.1 Broker Integration Service
**Service**: `BrokerIntegrationService`
**Package**: `com.trademaster.integration.service`
**Purpose**: Centralized broker connection and data aggregation

```java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BrokerIntegrationService {
    
    private final BrokerConnectionManager connectionManager;
    private final BrokerOAuthService oauthService;
    private final DataAggregationService aggregationService;
    private final BrokerHealthMonitor healthMonitor;
    
    /**
     * Establish secure connection to broker via OAuth 2.0
     */
    public BrokerConnection connectBroker(String userId, BrokerType brokerType, 
                                        String authCode) {
        
        // 1. Exchange auth code for tokens
        BrokerTokens tokens = oauthService.exchangeCodeForTokens(brokerType, authCode);
        
        // 2. Validate broker account access
        BrokerAccount account = validateBrokerAccount(brokerType, tokens);
        
        // 3. Encrypt and store tokens
        EncryptedTokens encryptedTokens = encryptionService.encryptTokens(
            userId, brokerType, tokens
        );
        
        // 4. Create broker connection
        BrokerConnection connection = BrokerConnection.builder()
                .userId(userId)
                .brokerType(brokerType)
                .accountId(account.getAccountId())
                .tokens(encryptedTokens)
                .status(ConnectionStatus.CONNECTED)
                .capabilities(getBrokerCapabilities(brokerType))
                .lastHealthCheck(Instant.now())
                .build();
        
        // 5. Register with connection manager
        connectionManager.registerConnection(connection);
        
        // 6. Start health monitoring
        healthMonitor.startMonitoring(connection.getId());
        
        log.info("Successfully connected to {} for user {}", brokerType, userId);
        return connection;
    }
    
    /**
     * Aggregate portfolio data across all connected brokers
     */
    @Cacheable(value = "consolidated-portfolio", key = "#userId")
    public ConsolidatedPortfolio getConsolidatedPortfolio(String userId) {
        
        // 1. Get all active broker connections
        List<BrokerConnection> connections = connectionManager.getActiveConnections(userId);
        
        if (connections.isEmpty()) {
            throw new NoBrokerConnectionsException(userId);
        }
        
        // 2. Fetch portfolio data from each broker in parallel
        List<CompletableFuture<BrokerPortfolio>> portfolioFutures = connections.stream()
                .map(connection -> CompletableFuture.supplyAsync(() -> 
                    fetchPortfolioFromBroker(connection), virtualThreadExecutor))
                .collect(Collectors.toList());
        
        // 3. Wait for all portfolio data to arrive
        List<BrokerPortfolio> brokerPortfolios = portfolioFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        // 4. Aggregate portfolio data
        return aggregationService.aggregatePortfolios(userId, brokerPortfolios);
    }
    
    private BrokerPortfolio fetchPortfolioFromBroker(BrokerConnection connection) {
        try {
            BrokerApiClient client = brokerApiClientFactory.createClient(connection);
            return client.getPortfolio();
        } catch (Exception e) {
            log.error("Failed to fetch portfolio from broker {}: {}", 
                     connection.getBrokerType(), e.getMessage());
            
            // Mark connection as unhealthy
            healthMonitor.markUnhealthy(connection.getId(), e);
            
            // Return empty portfolio to continue aggregation
            return BrokerPortfolio.empty(connection.getBrokerType());
        }
    }
}
```

#### 5.2 Broker Connection Entity Framework
**Entity**: `BrokerConnection`
**Package**: `com.trademaster.integration.entity`

```java
@Entity
@Table(name = "broker_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;
    
    @Column(name = "account_id", nullable = false)
    private String accountId;
    
    @Column(name = "display_name")
    private String displayName; // User-friendly name
    
    // OAuth token storage (encrypted)
    @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
    private String encryptedAccessToken;
    
    @Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
    private String encryptedRefreshToken;
    
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    
    // Connection status and health
    @Enumerated(EnumType.STRING)
    private ConnectionStatus status;
    
    @Column(name = "last_successful_call")
    private Instant lastSuccessfulCall;
    
    @Column(name = "last_health_check")
    private Instant lastHealthCheck;
    
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;
    
    @Column(name = "error_message")
    private String lastErrorMessage;
    
    // Broker capabilities
    @Type(JsonType.class)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private BrokerCapabilities capabilities;
    
    // Connection configuration
    @Type(JsonType.class)  
    @Column(name = "connection_config", columnDefinition = "jsonb")
    private BrokerConnectionConfig config;
    
    // Audit fields
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    @Version
    private Long version;
}

@Data
@Builder
public class BrokerCapabilities {
    private Set<OrderType> supportedOrderTypes;
    private Set<String> supportedExchanges;
    private BigDecimal maxOrderValue;
    private Long maxOrderQuantity;
    private boolean supportsMarginTrading;
    private boolean supportsOptionsTrading;
    private boolean supportsCommodityTrading;
    private boolean supportsInternationalTrading;
    private RateLimitConfig rateLimits;
    private Set<String> supportedFeatures;
}

enum BrokerType {
    ZERODHA("Zerodha", "https://api.kite.trade", "zerodha-oauth"),
    GROWW("Groww", "https://growwapi.com", "groww-oauth"),
    ANGEL_ONE("Angel One", "https://apiconnect.angelbroking.com", "angel-oauth"),
    UPSTOX("Upstox", "https://api.upstox.com", "upstox-oauth"),
    FYERS("Fyers", "https://api.fyers.in", "fyers-oauth"),
    IIFL("IIFL", "https://ttblaze.iifl.com", "iifl-oauth");
    
    private final String displayName;
    private final String apiBaseUrl;
    private final String oauthProvider;
}

enum ConnectionStatus {
    CONNECTED,
    DISCONNECTED, 
    TOKEN_EXPIRED,
    RATE_LIMITED,
    MAINTENANCE,
    ERROR,
    SUSPENDED
}
```

#### 5.3 OAuth Integration Service
**Service**: `BrokerOAuthService`
**Purpose**: Handle OAuth flows for different brokers

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class BrokerOAuthService {
    
    private final Map<BrokerType, BrokerOAuthProvider> oauthProviders;
    private final EncryptionService encryptionService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Initiate OAuth flow for broker connection
     */
    public String initiateOAuthFlow(String userId, BrokerType brokerType, 
                                  String redirectUri) {
        
        BrokerOAuthProvider provider = getProvider(brokerType);
        
        // Generate state parameter for security
        String state = generateSecureState(userId, brokerType);
        
        // Store state in Redis with 10-minute expiry
        String stateKey = "oauth:state:" + state;
        OAuthState oauthState = OAuthState.builder()
                .userId(userId)
                .brokerType(brokerType)
                .redirectUri(redirectUri)
                .createdAt(Instant.now())
                .build();
                
        redisTemplate.opsForValue().set(stateKey, oauthState, Duration.ofMinutes(10));
        
        // Build authorization URL
        String authUrl = provider.buildAuthorizationUrl(redirectUri, state);
        
        log.info("Initiated OAuth flow for {} - user: {}", brokerType, userId);
        return authUrl;
    }
    
    /**
     * Exchange authorization code for access tokens
     */
    public BrokerTokens exchangeCodeForTokens(String code, String state) {
        
        // Validate state parameter
        OAuthState oauthState = validateState(state);
        
        BrokerOAuthProvider provider = getProvider(oauthState.getBrokerType());
        
        // Exchange code for tokens
        BrokerTokens tokens = provider.exchangeCodeForTokens(
            code, oauthState.getRedirectUri()
        );
        
        // Validate tokens by making a test API call
        validateTokens(oauthState.getBrokerType(), tokens);
        
        log.info("Successfully exchanged OAuth code for tokens - broker: {}, user: {}", 
                oauthState.getBrokerType(), oauthState.getUserId());
        
        return tokens;
    }
    
    /**
     * Refresh expired access tokens
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<BrokerTokens> refreshTokens(BrokerConnection connection) {
        
        try {
            BrokerOAuthProvider provider = getProvider(connection.getBrokerType());
            
            String refreshToken = encryptionService.decryptRefreshToken(
                connection.getEncryptedRefreshToken()
            );
            
            BrokerTokens newTokens = provider.refreshAccessToken(refreshToken);
            
            // Update connection with new tokens
            updateConnectionTokens(connection, newTokens);
            
            log.info("Successfully refreshed tokens for broker: {}, user: {}", 
                    connection.getBrokerType(), connection.getUserId());
            
            return CompletableFuture.completedFuture(newTokens);
            
        } catch (Exception e) {
            log.error("Failed to refresh tokens for connection: {}", connection.getId(), e);
            
            // Mark connection as token expired
            connectionManager.updateStatus(connection.getId(), ConnectionStatus.TOKEN_EXPIRED);
            
            throw new TokenRefreshFailedException(connection.getId(), e);
        }
    }
    
    private BrokerOAuthProvider getProvider(BrokerType brokerType) {
        BrokerOAuthProvider provider = oauthProviders.get(brokerType);
        if (provider == null) {
            throw new UnsupportedBrokerException(brokerType);
        }
        return provider;
    }
}
```

#### 5.4 Real-time Data Aggregation Service
**Service**: `DataAggregationService`
**Purpose**: Aggregate and normalize data from multiple brokers

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class DataAggregationService {
    
    private final PositionNormalizationService normalizationService;
    private final PriceService priceService;
    private final CurrencyConversionService currencyService;
    
    /**
     * Aggregate portfolios from multiple brokers
     */
    public ConsolidatedPortfolio aggregatePortfolios(String userId, 
                                                   List<BrokerPortfolio> brokerPortfolios) {
        
        // 1. Normalize positions across brokers
        List<ConsolidatedPosition> positions = brokerPortfolios.stream()
                .flatMap(portfolio -> portfolio.getPositions().stream())
                .collect(Collectors.groupingBy(Position::getSymbol))
                .entrySet().stream()
                .map(entry -> consolidatePositionsForSymbol(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        
        // 2. Calculate aggregated metrics
        BigDecimal totalValue = positions.stream()
                .map(ConsolidatedPosition::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = positions.stream()
                .map(ConsolidatedPosition::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal unrealizedPnL = totalValue.subtract(totalCost);
        BigDecimal unrealizedPnLPercent = totalCost.equals(BigDecimal.ZERO) ? 
                BigDecimal.ZERO : unrealizedPnL.divide(totalCost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // 3. Calculate day change across all positions
        BigDecimal dayChange = calculateDayChange(positions);
        
        // 4. Generate broker breakdown
        List<BrokerPortfolioBreakdown> brokerBreakdown = generateBrokerBreakdown(
            brokerPortfolios, totalValue
        );
        
        // 5. Calculate asset allocation
        List<AssetAllocation> assetAllocation = calculateAssetAllocation(positions);
        
        return ConsolidatedPortfolio.builder()
                .userId(userId)
                .totalValue(totalValue)
                .totalCost(totalCost)
                .unrealizedPnL(unrealizedPnL)
                .unrealizedPnLPercent(unrealizedPnLPercent)
                .dayChange(dayChange)
                .positions(positions)
                .brokerBreakdown(brokerBreakdown)
                .assetAllocation(assetAllocation)
                .lastUpdated(Instant.now())
                .build();
    }
    
    /**
     * Consolidate positions for the same symbol across brokers
     */
    private ConsolidatedPosition consolidatePositionsForSymbol(String symbol, 
                                                             List<Position> positions) {
        
        // Normalize positions (handle different symbol formats, lot sizes, etc.)
        List<NormalizedPosition> normalizedPositions = positions.stream()
                .map(pos -> normalizationService.normalize(pos))
                .collect(Collectors.toList());
        
        // Calculate consolidated metrics
        long totalQuantity = normalizedPositions.stream()
                .mapToLong(NormalizedPosition::getQuantity)
                .sum();
        
        BigDecimal weightedAvgPrice = calculateWeightedAveragePrice(normalizedPositions);
        
        BigDecimal currentPrice = priceService.getCurrentPrice(symbol);
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(totalQuantity));
        
        // Create broker position breakdown
        List<BrokerPositionBreakdown> brokerBreakdown = normalizedPositions.stream()
                .map(pos -> BrokerPositionBreakdown.builder()
                        .brokerId(pos.getBrokerId())
                        .brokerName(pos.getBrokerName())
                        .quantity(pos.getQuantity())
                        .avgPrice(pos.getAvgPrice())
                        .currentValue(pos.getCurrentValue())
                        .build())
                .collect(Collectors.toList());
        
        return ConsolidatedPosition.builder()
                .symbol(symbol)
                .companyName(getCompanyName(symbol))
                .totalQuantity(totalQuantity)
                .avgPrice(weightedAvgPrice)
                .currentPrice(currentPrice)
                .totalCost(weightedAvgPrice.multiply(BigDecimal.valueOf(totalQuantity)))
                .currentValue(currentValue)
                .unrealizedPnL(currentValue.subtract(
                    weightedAvgPrice.multiply(BigDecimal.valueOf(totalQuantity))))
                .brokerPositions(brokerBreakdown)
                .build();
    }
}
```

## 🔗 API Integration Points

### REST Controllers for Broker Management
**Controller**: `BrokerConnectionController`

```java
@RestController
@RequestMapping("/api/v1/brokers")
@RequiredArgsConstructor
@Validated
public class BrokerConnectionController {
    
    private final BrokerIntegrationService integrationService;
    private final BrokerOAuthService oauthService;
    
    /**
     * Initiate broker OAuth connection
     */
    @PostMapping("/connect/initiate")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<OAuthInitiateResponse> initiateConnection(
            @Valid @RequestBody BrokerConnectRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String authUrl = oauthService.initiateOAuthFlow(
            userId, request.getBrokerType(), request.getRedirectUri()
        );
        
        return ResponseEntity.ok(OAuthInitiateResponse.builder()
                .authorizationUrl(authUrl)
                .brokerType(request.getBrokerType())
                .build());
    }
    
    /**
     * Complete broker OAuth connection
     */
    @PostMapping("/connect/complete")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<BrokerConnectionResponse> completeConnection(
            @Valid @RequestBody OAuthCompleteRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Exchange code for tokens
        BrokerTokens tokens = oauthService.exchangeCodeForTokens(
            request.getCode(), request.getState()
        );
        
        // Create broker connection
        BrokerConnection connection = integrationService.connectBroker(
            userId, request.getBrokerType(), tokens
        );
        
        return ResponseEntity.ok(BrokerConnectionResponse.from(connection));
    }
    
    /**
     * Get all broker connections for user
     */
    @GetMapping("/connections")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<List<BrokerConnectionResponse>> getConnections(
            Authentication authentication) {
        
        String userId = authentication.getName();
        List<BrokerConnection> connections = integrationService.getUserConnections(userId);
        
        List<BrokerConnectionResponse> responses = connections.stream()
                .map(BrokerConnectionResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get consolidated portfolio across all brokers
     */
    @GetMapping("/portfolio/consolidated")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<ConsolidatedPortfolio> getConsolidatedPortfolio(
            Authentication authentication) {
        
        String userId = authentication.getName();
        ConsolidatedPortfolio portfolio = integrationService.getConsolidatedPortfolio(userId);
        
        return ResponseEntity.ok(portfolio);
    }
    
    /**
     * Disconnect broker
     */
    @DeleteMapping("/connections/{connectionId}")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<Void> disconnectBroker(
            @PathVariable UUID connectionId,
            Authentication authentication) {
        
        String userId = authentication.getName();
        integrationService.disconnectBroker(userId, connectionId);
        
        return ResponseEntity.noContent().build();
    }
}
```

### WebSocket Integration for Real-time Updates
**Handler**: `BrokerDataWebSocketHandler`

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class BrokerDataWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionManager sessionManager;
    private final BrokerIntegrationService integrationService;
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessionManager.addSession(userId, session);
        
        // Start streaming consolidated portfolio updates
        startPortfolioStream(userId);
        
        log.info("Broker data WebSocket connected for user: {}", userId);
    }
    
    /**
     * Send real-time portfolio updates to client
     */
    @EventListener
    public void handlePortfolioUpdate(PortfolioUpdateEvent event) {
        sessionManager.getSessionsForUser(event.getUserId()).forEach(session -> {
            try {
                String message = objectMapper.writeValueAsString(
                    WebSocketMessage.builder()
                        .type("PORTFOLIO_UPDATE")
                        .data(event.getPortfolioData())
                        .timestamp(Instant.now())
                        .build()
                );
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send portfolio update", e);
            }
        });
    }
    
    /**
     * Send broker connection status updates
     */
    @EventListener
    public void handleConnectionStatusUpdate(BrokerConnectionStatusEvent event) {
        sessionManager.getSessionsForUser(event.getUserId()).forEach(session -> {
            try {
                String message = objectMapper.writeValueAsString(
                    WebSocketMessage.builder()
                        .type("BROKER_STATUS_UPDATE")
                        .data(event.getConnectionStatus())
                        .timestamp(Instant.now())
                        .build()
                );
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send broker status update", e);
            }
        });
    }
}
```

## 🎯 Database Schema Integration

### Migration: V3__Add_broker_integration_tables.sql
```sql
-- Broker connections table
CREATE TABLE broker_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    broker_type VARCHAR(20) NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,
    token_expires_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONNECTED',
    last_successful_call TIMESTAMP,
    last_health_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    consecutive_failures INTEGER DEFAULT 0,
    error_message TEXT,
    capabilities JSONB,
    connection_config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(user_id, broker_type, account_id)
);

-- Consolidated positions table  
CREATE TABLE consolidated_positions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(200),
    total_quantity BIGINT NOT NULL,
    avg_price DECIMAL(12,4) NOT NULL,
    current_price DECIMAL(12,4),
    total_cost DECIMAL(15,2),
    current_value DECIMAL(15,2),
    unrealized_pnl DECIMAL(15,2),
    day_change DECIMAL(15,2),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, symbol)
);

-- Broker position breakdown
CREATE TABLE broker_position_breakdown (
    id BIGSERIAL PRIMARY KEY,
    consolidated_position_id BIGINT NOT NULL REFERENCES consolidated_positions(id),
    broker_connection_id UUID NOT NULL REFERENCES broker_connections(id),
    quantity BIGINT NOT NULL,
    avg_price DECIMAL(12,4) NOT NULL,
    current_value DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- OAuth states for security
CREATE TABLE oauth_states (
    state VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    redirect_uri TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Broker health monitoring
CREATE TABLE broker_health_logs (
    id BIGSERIAL PRIMARY KEY,
    broker_connection_id UUID NOT NULL REFERENCES broker_connections(id),
    check_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_time_ms INTEGER,
    error_message TEXT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_broker_connections_user_id ON broker_connections(user_id);
CREATE INDEX idx_broker_connections_status ON broker_connections(status);
CREATE INDEX idx_consolidated_positions_user_id ON consolidated_positions(user_id);
CREATE INDEX idx_broker_position_breakdown_consolidated_id ON broker_position_breakdown(consolidated_position_id);
CREATE INDEX idx_oauth_states_expires_at ON oauth_states(expires_at);
CREATE INDEX idx_broker_health_logs_connection_id ON broker_health_logs(broker_connection_id);
CREATE INDEX idx_broker_health_logs_checked_at ON broker_health_logs(checked_at);
```

## 🧪 Testing Strategy

### Integration Testing with Test Brokers
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
class BrokerIntegrationServiceIntegrationTest {
    
    @Test
    @Order(1)
    void shouldConnectToBrokerViaOAuth() {
        // Given
        String userId = "test-user-123";
        BrokerType brokerType = BrokerType.ZERODHA;
        String mockAuthCode = "test-auth-code";
        
        // Mock OAuth response
        mockBrokerOAuthServer.stub(WireMock.post("/oauth/token")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(createMockTokenResponse())));
        
        // When
        BrokerConnection connection = brokerIntegrationService.connectBroker(
            userId, brokerType, mockAuthCode
        );
        
        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.CONNECTED);
        assertThat(connection.getBrokerType()).isEqualTo(brokerType);
    }
    
    @Test
    @Order(2)
    void shouldAggregatePortfolioFromMultipleBrokers() {
        // Given
        String userId = "test-user-123";
        setupMockBrokerPortfolios();
        
        // When
        ConsolidatedPortfolio portfolio = brokerIntegrationService
                .getConsolidatedPortfolio(userId);
        
        // Then
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getTotalValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(portfolio.getPositions()).isNotEmpty();
        assertThat(portfolio.getBrokerBreakdown()).hasSize(2);
    }
    
    @Test
    @Order(3)
    void shouldHandleBrokerConnectionFailure() {
        // Given
        String userId = "test-user-123";
        mockBrokerApiFailure();
        
        // When
        ConsolidatedPortfolio portfolio = brokerIntegrationService
                .getConsolidatedPortfolio(userId);
        
        // Then - Should still return portfolio from healthy brokers
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getBrokerBreakdown()).hasSize(1); // Only healthy broker
    }
}
```

### Performance Testing
```java
@Component
public class BrokerIntegrationPerformanceTest {
    
    @Test
    void shouldHandleMultipleConcurrentPortfolioRequests() {
        int concurrentRequests = 100;
        String userId = "test-user-123";
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<ConsolidatedPortfolio>> futures = new ArrayList<>();
        
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                brokerIntegrationService.getConsolidatedPortfolio(userId), executor
            ));
        }
        
        // All requests should complete within 5 seconds
        assertTimeout(Duration.ofSeconds(5), () -> {
            List<ConsolidatedPortfolio> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            
            assertThat(results).hasSize(concurrentRequests);
            results.forEach(portfolio -> assertThat(portfolio).isNotNull());
        });
    }
}
```

## 🚀 Implementation Timeline

### Phase 1: Core Integration Framework (Week 1-2)
- Implement `BrokerIntegrationService` with OAuth flows
- Create broker connection entities and repositories
- Build basic health monitoring system

### Phase 2: Data Aggregation (Week 3-4)
- Implement `DataAggregationService` for portfolio consolidation
- Build position normalization and consolidation logic
- Add real-time WebSocket updates

### Phase 3: Production Readiness (Week 5-6)
- Comprehensive testing with real broker sandbox APIs
- Performance optimization and caching strategies
- Security auditing and compliance verification

**Ready to unify the multi-broker trading ecosystem! 🔗⚡**