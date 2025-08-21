# Story 2.3: Portfolio Management & P&L Tracking

## Overview
Comprehensive portfolio management service providing real-time position tracking, P&L calculations, portfolio analytics, and risk management for the TradeMaster trading platform.

## Technical Architecture

### Core Technology Stack
- **Framework**: Spring Boot 3.5.3 + Spring MVC  
- **Language**: Java 24 with Virtual Threads (preview features enabled)
- **Concurrency**: Virtual Threads for unlimited scalability (~8KB per thread)
- **Database**: PostgreSQL with JPA/Hibernate
- **HTTP Client**: OkHttp for external API calls
- **Async Operations**: CompletableFuture with Virtual Thread executors
- **Testing**: JUnit 5 + Testcontainers
- **API Documentation**: SpringDoc OpenAPI 3

### Performance Targets
- **Portfolio Valuation**: <50ms response time
- **Position Updates**: <10ms per position
- **P&L Calculations**: <25ms for full portfolio
- **Risk Analytics**: <100ms for comprehensive metrics
- **Concurrent Users**: 10,000+ simultaneous portfolio views
- **Memory Usage**: ~8KB per Virtual Thread operation

## Business Requirements

### BR-2.3.1: Real-Time Portfolio Valuation
- Calculate current portfolio value using live market prices
- Support multiple asset classes (Equity, Derivatives, Commodities)
- Update valuations automatically on price changes
- Handle currency conversions for multi-currency portfolios
- Provide breakdown by asset class, sector, and individual holdings

### BR-2.3.2: Position Tracking & Management
- Track long and short positions across all instruments
- Maintain position history with complete audit trail
- Calculate average cost basis using FIFO/LIFO/Weighted Average methods
- Support partial position closures and adjustments
- Real-time position updates from order executions

### BR-2.3.3: P&L Calculations
- **Realized P&L**: Profit/loss from closed positions
- **Unrealized P&L**: Mark-to-market gains/losses on open positions
- **Daily P&L**: Day-over-day portfolio performance
- **Period P&L**: Configurable time period performance analysis
- **Tax Lot Tracking**: For capital gains tax calculations

### BR-2.3.4: Portfolio Analytics
- **Performance Metrics**: Total return, annualized return, Sharpe ratio
- **Risk Metrics**: Portfolio volatility, Value at Risk (VaR), maximum drawdown
- **Asset Allocation**: Current vs target allocation analysis
- **Sector Exposure**: Industry and geographic diversification analysis
- **Benchmark Comparison**: Performance vs market indices

### BR-2.3.5: Risk Management
- **Concentration Risk**: Single position/sector exposure limits
- **Correlation Analysis**: Portfolio correlation matrix
- **Stress Testing**: Portfolio performance under market scenarios
- **Risk Budget**: Risk allocation across strategies and positions
- **Margin Requirements**: Real-time margin utilization tracking

## Functional Requirements

### FR-2.3.1: Portfolio Management API
```http
GET    /api/v1/portfolios/{userId}                    # Get user portfolio
GET    /api/v1/portfolios/{userId}/positions          # Get all positions
GET    /api/v1/portfolios/{userId}/positions/{symbol} # Get specific position
GET    /api/v1/portfolios/{userId}/pnl                # Get P&L summary
GET    /api/v1/portfolios/{userId}/analytics          # Get portfolio analytics
GET    /api/v1/portfolios/{userId}/risk-metrics       # Get risk analysis
POST   /api/v1/portfolios/{userId}/rebalance          # Trigger rebalancing
```

### FR-2.3.2: Position Updates Integration
- Listen to order execution events from Trading Service
- Update positions in real-time when orders are filled
- Handle partial fills and order modifications
- Maintain position integrity across system restarts

### FR-2.3.3: Market Data Integration
- Subscribe to real-time price feeds from Market Data Service
- Update portfolio valuations on price changes
- Cache market prices for performance optimization
- Handle market data feed failures gracefully

### FR-2.3.4: Reporting & Export
- Generate portfolio statements (PDF/Excel)
- Export position data in multiple formats
- Scheduled reporting (daily/weekly/monthly)
- Custom report generation with filters

## Data Models

### Portfolio Entity
```java
@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String portfolioName;
    
    @Column(nullable = false)
    private String currency;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalValue;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal cashBalance;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalCost;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal realizedPnl;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal unrealizedPnl;
    
    @Enumerated(EnumType.STRING)
    private PortfolioStatus status;
    
    @Enumerated(EnumType.STRING)
    private CostBasisMethod costBasisMethod;
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Position Entity
```java
@Entity
@Table(name = "positions")
public class Position {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;
    
    @Column(nullable = false)
    private Long portfolioId;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(nullable = false)
    private String exchange;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal averageCost;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalCost;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal currentPrice;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal marketValue;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal unrealizedPnl;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal realizedPnl;
    
    @Enumerated(EnumType.STRING)
    private PositionType positionType;
    
    private Instant openedAt;
    private Instant updatedAt;
}
```

### Transaction Entity
```java
@Entity
@Table(name = "portfolio_transactions")
public class PortfolioTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    
    @Column(nullable = false)
    private Long portfolioId;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private String symbol;
    
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal commission;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal tax;
    
    private Instant executedAt;
    private Instant createdAt;
}
```

## Service Architecture

### Core Services

#### 1. PortfolioService
```java
public interface PortfolioService {
    Portfolio getPortfolio(Long userId);
    Portfolio createPortfolio(Long userId, CreatePortfolioRequest request);
    Portfolio updatePortfolio(Long portfolioId, UpdatePortfolioRequest request);
    List<Position> getPositions(Long portfolioId);
    Position getPosition(Long portfolioId, String symbol);
    PortfolioPnLSummary getPnLSummary(Long portfolioId);
    PortfolioAnalytics getAnalytics(Long portfolioId);
    void updatePortfolioValuation(Long portfolioId);
}
```

#### 2. PositionService
```java
public interface PositionService {
    void updatePositionFromTrade(Long portfolioId, TradeExecutionEvent event);
    Position openPosition(Long portfolioId, String symbol, Integer quantity, BigDecimal price);
    Position closePosition(Long portfolioId, String symbol, Integer quantity, BigDecimal price);
    List<Position> getPositionsByPortfolio(Long portfolioId);
    void recalculatePosition(Long positionId);
    BigDecimal calculateAverageCost(Long positionId);
}
```

#### 3. PnLCalculationService
```java
public interface PnLCalculationService {
    PnLResult calculateRealizedPnL(Long portfolioId, LocalDate fromDate, LocalDate toDate);
    PnLResult calculateUnrealizedPnL(Long portfolioId);
    DailyPnL calculateDailyPnL(Long portfolioId, LocalDate date);
    PeriodPnL calculatePeriodPnL(Long portfolioId, LocalDate fromDate, LocalDate toDate);
    void updateUnrealizedPnL(Long portfolioId, Map<String, BigDecimal> currentPrices);
}
```

#### 4. PortfolioAnalyticsService
```java
public interface PortfolioAnalyticsService {
    PortfolioAnalytics calculateAnalytics(Long portfolioId);
    RiskMetrics calculateRiskMetrics(Long portfolioId);
    AssetAllocation getAssetAllocation(Long portfolioId);
    PerformanceMetrics getPerformanceMetrics(Long portfolioId, Period period);
    CompletableFuture<BenchmarkComparison> compareToBenchmark(Long portfolioId, String benchmarkSymbol);
}
```

#### 5. PortfolioRiskService
```java
public interface PortfolioRiskService {
    RiskAssessment assessPortfolioRisk(Long portfolioId);
    ConcentrationRisk analyzeConcentrationRisk(Long portfolioId);
    CorrelationMatrix calculateCorrelationMatrix(Long portfolioId);
    VaRResult calculateValueAtRisk(Long portfolioId, double confidenceLevel, int timeHorizon);
    StressTestResult performStressTest(Long portfolioId, StressTestScenario scenario);
}
```

## Integration Architecture

### 1. Trading Service Integration
```java
@EventListener
@Async("portfolioExecutor")
public void handleTradeExecution(TradeExecutionEvent event) {
    // Update positions when trades are executed
    positionService.updatePositionFromTrade(event.getPortfolioId(), event);
    
    // Recalculate portfolio value
    portfolioService.updatePortfolioValuation(event.getPortfolioId());
    
    // Update P&L calculations
    pnlCalculationService.calculateUnrealizedPnL(event.getPortfolioId());
}
```

### 2. Market Data Integration
```java
@EventListener
@Async("marketDataExecutor")
public void handlePriceUpdate(PriceUpdateEvent event) {
    // Update position market values
    List<Portfolio> affectedPortfolios = portfolioService.getPortfoliosWithSymbol(event.getSymbol());
    
    CompletableFuture.allOf(
        affectedPortfolios.stream()
            .map(portfolio -> CompletableFuture.runAsync(() -> 
                portfolioService.updatePortfolioValuation(portfolio.getPortfolioId())))
            .toArray(CompletableFuture[]::new)
    ).join();
}
```

## Performance Optimizations

### 1. Virtual Thread Optimization
- Dedicated Virtual Thread executors for different workloads
- Non-blocking I/O operations for external service calls
- Parallel processing of portfolio calculations

### 2. Caching Strategy
```java
@Configuration
public class CachingConfig {
    @Bean
    @Qualifier("portfolioCache")
    public CacheManager portfolioCacheManager() {
        // Redis-based caching for portfolio data
        // TTL: 5 minutes for portfolio valuations
        // TTL: 1 minute for position data
        // TTL: 15 minutes for analytics
    }
}
```

### 3. Database Optimization
- Optimized JPA queries with fetch joins
- Database indexes on frequently queried columns
- Connection pooling with HikariCP
- Read replicas for analytics queries

## Security & Compliance

### 1. Authentication & Authorization
- JWT token validation for all endpoints
- Role-based access control (RBAC)
- User data isolation and multi-tenancy support

### 2. Audit Trail
- Complete audit log for all portfolio transactions
- Immutable transaction records
- Compliance with financial regulations

### 3. Data Privacy
- Encryption at rest and in transit
- PII data protection
- GDPR compliance for EU users

## Monitoring & Observability

### 1. Metrics Collection
```java
@Component
public class PortfolioMetrics {
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordPortfolioUpdate(PortfolioUpdateEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("portfolio.update.duration")
            .tag("portfolio_id", event.getPortfolioId().toString())
            .register(meterRegistry));
    }
}
```

### 2. Health Checks
- Portfolio service health endpoint
- Database connectivity checks
- External service dependency checks

### 3. Alerting
- Portfolio calculation failures
- Performance degradation alerts
- Data consistency issues

## Testing Strategy

### 1. Unit Tests
- Service layer business logic validation
- P&L calculation accuracy tests
- Risk metric calculation tests

### 2. Integration Tests
- Trading Service integration scenarios
- Market Data Service integration tests
- Database integration with Testcontainers

### 3. Performance Tests
- Load testing with 10,000+ concurrent users
- Portfolio calculation performance benchmarks
- Memory usage validation for Virtual Threads

## Deployment Architecture

### 1. Microservice Configuration
```yaml
server:
  port: 8083
  
spring:
  application:
    name: portfolio-service
  datasource:
    url: jdbc:postgresql://localhost:5432/trademaster_portfolio
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 2. Docker Configuration
```dockerfile
FROM eclipse-temurin:24-jre-alpine
COPY target/portfolio-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app.jar"]
```

## Success Criteria

### 1. Performance Metrics
- ✅ Portfolio valuation: <50ms response time
- ✅ Position updates: <10ms processing time
- ✅ P&L calculations: <25ms for full portfolio
- ✅ Support 10,000+ concurrent users

### 2. Functional Validation
- ✅ Accurate P&L calculations across all scenarios
- ✅ Real-time position tracking with order integration
- ✅ Comprehensive portfolio analytics and risk metrics
- ✅ Complete audit trail for all transactions

### 3. Integration Success
- ✅ Seamless integration with Trading Service
- ✅ Real-time market data consumption
- ✅ Authentication service integration
- ✅ Proper error handling and resilience

## Implementation Timeline

**Phase 1**: Core Portfolio & Position Management (Days 1-3)
- Portfolio and Position entities
- Basic CRUD operations
- Database schema and migrations

**Phase 2**: P&L Calculation Engine (Days 4-5)
- Realized/Unrealized P&L calculations
- Transaction processing
- Cost basis calculations

**Phase 3**: Analytics & Risk Management (Days 6-7)
- Portfolio analytics service
- Risk metrics calculations
- Performance analytics

**Phase 4**: Integration & API (Days 8-9)
- Trading Service integration
- Market Data Service integration
- REST API endpoints

**Phase 5**: Testing & Validation (Days 10-11)
- Comprehensive testing
- Performance validation
- Integration testing

This specification provides the complete blueprint for implementing Story 2.3 using Java 24 + Virtual Threads architecture while maintaining high performance, scalability, and compliance with financial industry standards.