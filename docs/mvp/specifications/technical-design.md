# Technical Design Specification
## TradeMaster Orchestrator - Multi-Broker Trading Platform

**Document Version:** 1.0  
**Last Updated:** August 23, 2025  
**Document Owner:** Engineering Team  
**Status:** Draft  

---

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Design](#architecture-design)
3. [Database Design](#database-design)
4. [API Design](#api-design)
5. [Real-Time Data Architecture](#real-time-data-architecture)
6. [Security Architecture](#security-architecture)
7. [Integration Architecture](#integration-architecture)
8. [Scaling & Performance](#scaling--performance)
9. [Deployment Architecture](#deployment-architecture)
10. [Monitoring & Observability](#monitoring--observability)

---

## System Overview

### High-Level Architecture

TradeMaster Orchestrator follows a microservices architecture pattern with event-driven communication and real-time data streaming capabilities.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │  Mobile Client  │    │  Admin Panel    │
│   (React PWA)   │    │   (React PWA)   │    │    (React)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (Kong)                       │
│              Load Balancing | Rate Limiting | Security         │
└─────────────────────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Auth Service   │    │ Market Service  │    │ Trading Service │
│   (Spring)      │    │   (Spring)      │    │   (Spring)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                     Message Bus (Apache Kafka)                 │
│                Real-time Events | Order Routing                │
└─────────────────────────────────────────────────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│Portfolio Service│    │Analytics Service│    │Notification Svc │
│   (Spring)      │    │   (Python)      │    │   (Spring)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                               │
│  PostgreSQL | Redis | InfluxDB | MongoDB | Elasticsearch      │
└─────────────────────────────────────────────────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────────┐
│                    External Integrations                       │
│        Broker APIs | Market Data | Payment Gateways           │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

#### Backend Services
- **Framework:** Spring Boot 3.x with Java 21
- **Build Tool:** Maven 3.9+ with multi-module structure
- **API Gateway:** Kong Gateway for routing, security, rate limiting
- **Message Queue:** Apache Kafka for event streaming
- **Cache:** Redis for session management and real-time data caching
- **Search:** Elasticsearch for logs and analytics

#### Frontend Application
- **Framework:** React 18+ with TypeScript 5+
- **Build Tool:** Vite with ESBuild for fast compilation
- **State Management:** Redux Toolkit + RTK Query
- **UI Components:** shadcn/ui + Tailwind CSS (existing system)
- **Charts:** TradingView Charting Library + Chart.js
- **PWA:** Service Workers + Web App Manifest

#### Data Storage
- **Primary Database:** PostgreSQL 15+ with read replicas
- **Time-Series Data:** InfluxDB for market data and metrics
- **Document Store:** MongoDB for flexible schemas
- **Cache:** Redis Cluster for high availability
- **Search:** Elasticsearch for full-text search and analytics

---

## Architecture Design

### Microservices Architecture

#### Service Decomposition Strategy

**1. Authentication Service (`auth-service`)**
```yaml
responsibilities:
  - User authentication and authorization
  - OAuth broker integration management
  - Session and token management
  - Multi-factor authentication
  - Security audit logging

technology_stack:
  framework: Spring Boot 3.x + Spring Security 6
  database: PostgreSQL + Redis
  external_integrations:
    - Broker OAuth APIs
    - SMS/Email providers
    - Identity verification services

api_endpoints:
  - POST /api/v1/auth/login
  - POST /api/v1/auth/logout
  - POST /api/v1/auth/refresh
  - GET /api/v1/auth/brokers/status
  - POST /api/v1/auth/brokers/{broker_id}/connect
```

**2. Market Data Service (`market-service`)**
```yaml
responsibilities:
  - Real-time market data aggregation
  - Historical data management
  - Market alerts and notifications
  - Data normalization across brokers
  - Market analysis and insights

technology_stack:
  framework: Spring Boot 3.x + WebFlux (reactive)
  database: InfluxDB + Redis + MongoDB
  message_queue: Kafka for real-time streaming
  external_integrations:
    - NSE/BSE APIs
    - Broker market data feeds
    - Third-party data providers

data_flow:
  - Broker APIs → Kafka → Market Service → InfluxDB/Redis
  - WebSocket clients ← Market Service ← Kafka
```

**3. Trading Service (`trading-service`)**
```yaml
responsibilities:
  - Order placement and management
  - Intelligent order routing
  - Risk management and validation
  - Trade execution monitoring
  - Order analytics and reporting

technology_stack:
  framework: Spring Boot 3.x + Virtual Threads (Java 21)
  database: PostgreSQL + Redis
  message_queue: Kafka for order events
  external_integrations:
    - Multiple broker trading APIs
    - Risk management systems
    - Compliance reporting

order_flow:
  - Client → API Gateway → Trading Service
  - Trading Service → Broker APIs (parallel)
  - Order Status → Kafka → WebSocket clients
```

**4. Portfolio Service (`portfolio-service`)**
```yaml
responsibilities:
  - Portfolio aggregation across brokers
  - Position tracking and reconciliation
  - P&L calculations
  - Performance analytics
  - Risk metrics computation

technology_stack:
  framework: Spring Boot 3.x + Spring Data
  database: PostgreSQL + InfluxDB + Redis
  message_queue: Kafka for position updates
  external_integrations:
    - Broker portfolio APIs
    - Market data feeds
    - Tax calculation services

data_processing:
  - Real-time position updates via Kafka
  - Batch reconciliation processes
  - Performance metric calculations
  - Risk assessment algorithms
```

**5. Analytics Service (`analytics-service`)**
```yaml
responsibilities:
  - Advanced portfolio analytics
  - AI-powered trading insights
  - Behavioral pattern analysis
  - Performance benchmarking
  - Custom reporting

technology_stack:
  framework: FastAPI (Python 3.11+) + Celery
  database: PostgreSQL + MongoDB + Elasticsearch
  machine_learning: scikit-learn + TensorFlow
  message_queue: Kafka + Redis for task queue

ml_pipeline:
  - Data ingestion from multiple sources
  - Feature engineering and preprocessing
  - Model training and validation
  - Real-time inference serving
  - Model performance monitoring
```

**6. Notification Service (`notification-service`)**
```yaml
responsibilities:
  - Real-time alerts and notifications
  - Multi-channel communication
  - Notification preferences management
  - Alert rule engine
  - Communication audit trail

technology_stack:
  framework: Spring Boot 3.x + WebSocket
  database: PostgreSQL + Redis
  message_queue: Kafka for event processing
  external_integrations:
    - Push notification services
    - SMS/Email gateways
    - WebSocket connections

notification_channels:
  - Web push notifications
  - Mobile push notifications
  - Email notifications
  - SMS alerts
  - In-app notifications
```

### Service Communication Patterns

#### Synchronous Communication
- **REST APIs:** Service-to-service communication for CRUD operations
- **Circuit Breaker Pattern:** Resilience4j for fault tolerance
- **Retry Logic:** Exponential backoff with jitter
- **Timeout Management:** Request-specific timeout configurations

#### Asynchronous Communication
- **Event-Driven Architecture:** Kafka for domain events
- **CQRS Pattern:** Command-Query separation for read/write optimization
- **Saga Pattern:** Distributed transaction management
- **Dead Letter Queues:** Failed message handling and recovery

---

## Database Design

### PostgreSQL Schema Design

#### Core Entity Relationships

```sql
-- Users and Authentication
CREATE SCHEMA auth;

CREATE TABLE auth.users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth.user_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(user_id),
    device_fingerprint VARCHAR(255),
    ip_address INET,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth.broker_connections (
    connection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(user_id),
    broker_id VARCHAR(50) NOT NULL,
    broker_user_id VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trading and Portfolio
CREATE SCHEMA trading;

CREATE TABLE trading.orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(user_id),
    parent_order_id UUID REFERENCES trading.orders(order_id), -- For split orders
    broker_id VARCHAR(50) NOT NULL,
    broker_order_id VARCHAR(255),
    symbol VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL, -- MARKET, LIMIT, SL, etc.
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    quantity DECIMAL(15,4) NOT NULL,
    price DECIMAL(15,4),
    trigger_price DECIMAL(15,4),
    filled_quantity DECIMAL(15,4) DEFAULT 0,
    avg_price DECIMAL(15,4),
    status VARCHAR(50) DEFAULT 'PENDING',
    validity VARCHAR(20) DEFAULT 'DAY',
    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trading.executions (
    execution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES trading.orders(order_id),
    trade_id VARCHAR(255),
    quantity DECIMAL(15,4) NOT NULL,
    price DECIMAL(15,4) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trading.positions (
    position_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(user_id),
    broker_id VARCHAR(50) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    quantity DECIMAL(15,4) NOT NULL,
    avg_price DECIMAL(15,4) NOT NULL,
    current_price DECIMAL(15,4),
    unrealized_pnl DECIMAL(15,4),
    realized_pnl DECIMAL(15,4) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Market Data (Reference data in PostgreSQL, time-series in InfluxDB)
CREATE SCHEMA market;

CREATE TABLE market.instruments (
    instrument_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sector VARCHAR(100),
    lot_size INTEGER DEFAULT 1,
    tick_size DECIMAL(10,8),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE market.price_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(user_id),
    symbol VARCHAR(50) NOT NULL,
    condition_type VARCHAR(20) NOT NULL, -- ABOVE, BELOW, CHANGE_PERCENT
    threshold_value DECIMAL(15,4) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    triggered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Database Indexing Strategy

```sql
-- Performance-critical indexes
CREATE INDEX CONCURRENTLY idx_users_email ON auth.users(email);
CREATE INDEX CONCURRENTLY idx_users_phone ON auth.users(phone);
CREATE INDEX CONCURRENTLY idx_sessions_user_expires ON auth.user_sessions(user_id, expires_at);
CREATE INDEX CONCURRENTLY idx_broker_connections_user_broker ON auth.broker_connections(user_id, broker_id);

CREATE INDEX CONCURRENTLY idx_orders_user_status ON trading.orders(user_id, status);
CREATE INDEX CONCURRENTLY idx_orders_placed_at ON trading.orders(placed_at);
CREATE INDEX CONCURRENTLY idx_positions_user_symbol ON trading.positions(user_id, symbol);
CREATE INDEX CONCURRENTLY idx_executions_order_time ON trading.executions(order_id, executed_at);

CREATE INDEX CONCURRENTLY idx_instruments_symbol_exchange ON market.instruments(symbol, exchange);
CREATE INDEX CONCURRENTLY idx_price_alerts_user_active ON market.price_alerts(user_id, is_active);

-- Composite indexes for complex queries
CREATE INDEX CONCURRENTLY idx_orders_composite ON trading.orders(user_id, status, placed_at);
CREATE INDEX CONCURRENTLY idx_positions_composite ON trading.positions(user_id, broker_id, symbol);
```

### InfluxDB Schema Design

#### Time-Series Data for Market Data and Metrics

```sql
-- Market data measurement
market_data,symbol=RELIANCE,exchange=NSE price=2450.50,volume=1000000,open=2440.00,high=2460.00,low=2430.00 1629788400000000000

-- Order metrics measurement  
order_metrics,user_id=user123,broker=zerodha,symbol=INFY execution_time=150,slippage=0.05,fill_rate=1.0 1629788400000000000

-- Portfolio performance measurement
portfolio_metrics,user_id=user123,broker=zerodha total_value=1000000,day_pnl=5000,total_pnl=50000 1629788400000000000

-- System performance measurement
system_metrics,service=trading-service,instance=pod1 response_time=45,cpu_usage=65.5,memory_usage=512 1629788400000000000
```

### Redis Data Structures

#### Session Management and Caching

```python
# User session data
user_session:{session_id} = {
    "user_id": "uuid",
    "broker_tokens": {
        "zerodha": {"access_token": "...", "expires": 1629788400},
        "groww": {"access_token": "...", "expires": 1629788500}
    },
    "last_activity": 1629788400,
    "device_info": {...}
}

# Real-time price cache
price:{symbol}:{exchange} = {
    "price": 2450.50,
    "change": 10.50,
    "change_percent": 0.43,
    "volume": 1000000,
    "timestamp": 1629788400
}

# Order book cache
orderbook:{symbol}:{exchange} = {
    "bids": [[2450.00, 1000], [2449.50, 500]],
    "asks": [[2450.50, 800], [2451.00, 1200]],
    "timestamp": 1629788400
}

# User portfolio cache
portfolio:{user_id} = {
    "total_value": 1000000,
    "day_pnl": 5000,
    "positions": [...],
    "last_updated": 1629788400
}
```

---

## API Design

### RESTful API Specification

#### Authentication Endpoints

```yaml
/api/v1/auth:
  /login:
    POST:
      summary: User login with email/phone and password
      request_body:
        email: string
        password: string
        device_fingerprint: string
      responses:
        200:
          access_token: string (JWT, 15 min)
          refresh_token: string (7 days)
          user_profile: object
        401: Invalid credentials
        429: Rate limit exceeded
  
  /brokers/{broker_id}/connect:
    POST:
      summary: Initialize OAuth flow with broker
      parameters:
        broker_id: string (zerodha, groww, angelone)
      responses:
        200:
          auth_url: string
          state: string
        400: Invalid broker ID
  
  /brokers/{broker_id}/callback:
    POST:
      summary: Handle OAuth callback from broker
      request_body:
        code: string
        state: string
      responses:
        200:
          connection_status: string
          permissions: array
```

#### Trading Endpoints

```yaml
/api/v1/trading:
  /orders:
    GET:
      summary: Get user's order history
      parameters:
        status: string (optional)
        broker_id: string (optional)
        from_date: string (optional)
        to_date: string (optional)
        limit: integer (default: 50)
      responses:
        200:
          orders: array
          pagination: object
    
    POST:
      summary: Place new order
      request_body:
        symbol: string
        exchange: string
        side: string (BUY/SELL)
        quantity: number
        order_type: string
        price: number (optional)
        trigger_price: number (optional)
        broker_preference: string (optional)
      responses:
        201:
          order_id: string
          routing_plan: object
          estimated_execution_time: number
        400: Invalid order parameters
        429: Rate limit exceeded
  
  /orders/{order_id}:
    GET:
      summary: Get order details and status
      responses:
        200:
          order: object
          executions: array
          current_status: string
    
    PUT:
      summary: Modify pending order
      request_body:
        quantity: number (optional)
        price: number (optional)
      responses:
        200:
          updated_order: object
        400: Order cannot be modified
```

#### Portfolio Endpoints

```yaml
/api/v1/portfolio:
  /:
    GET:
      summary: Get consolidated portfolio
      parameters:
        include_brokers: array (optional)
        as_of_date: string (optional)
      responses:
        200:
          total_value: number
          day_pnl: number
          total_pnl: number
          positions: array
          allocation: object
  
  /positions:
    GET:
      summary: Get detailed position breakdown
      parameters:
        broker_id: string (optional)
        symbol: string (optional)
      responses:
        200:
          positions: array
          summary: object
  
  /analytics:
    GET:
      summary: Get portfolio analytics
      parameters:
        period: string (1D, 1W, 1M, 3M, 1Y)
        benchmark: string (optional)
      responses:
        200:
          performance_metrics: object
          risk_metrics: object
          attribution_analysis: object
```

### WebSocket API Specification

#### Real-Time Data Streams

```javascript
// Connection establishment
const ws = new WebSocket('wss://api.trademaster.com/ws/v1');

// Authentication
ws.send(JSON.stringify({
  type: 'auth',
  token: 'jwt_access_token'
}));

// Subscribe to market data
ws.send(JSON.stringify({
  type: 'subscribe',
  channel: 'market_data',
  symbols: ['RELIANCE.NSE', 'INFY.NSE', 'TCS.NSE']
}));

// Subscribe to portfolio updates
ws.send(JSON.stringify({
  type: 'subscribe',
  channel: 'portfolio',
  user_id: 'user123'
}));

// Message formats
{
  "type": "market_data",
  "symbol": "RELIANCE.NSE",
  "data": {
    "price": 2450.50,
    "change": 10.50,
    "volume": 1000000,
    "timestamp": 1629788400000
  }
}

{
  "type": "portfolio_update",
  "data": {
    "total_value": 1000000,
    "day_pnl": 5000,
    "positions_changed": ["RELIANCE.NSE", "INFY.NSE"]
  }
}

{
  "type": "order_update",
  "order_id": "order123",
  "data": {
    "status": "FILLED",
    "filled_quantity": 100,
    "avg_price": 2450.50,
    "timestamp": 1629788400000
  }
}
```

---

## Real-Time Data Architecture

### Event Streaming Architecture

#### Kafka Topic Design

```yaml
topics:
  market-data:
    partitions: 20
    replication: 3
    retention: 7 days
    key: symbol
    schema:
      symbol: string
      exchange: string
      price: decimal
      volume: long
      timestamp: long
  
  order-events:
    partitions: 10
    replication: 3
    retention: 30 days
    key: user_id
    schema:
      order_id: string
      user_id: string
      event_type: string
      data: object
      timestamp: long
  
  portfolio-updates:
    partitions: 5
    replication: 3
    retention: 30 days
    key: user_id
    schema:
      user_id: string
      broker_id: string
      positions: array
      metrics: object
      timestamp: long
```

#### Stream Processing

```java
// Market data stream processor
@Component
public class MarketDataProcessor {
    
    @KafkaListener(topics = "market-data")
    public void processMarketData(@Payload MarketDataEvent event) {
        // Update Redis cache for real-time access
        redisTemplate.opsForValue().set(
            "price:" + event.getSymbol(), 
            event, 
            Duration.ofMinutes(1)
        );
        
        // Store in InfluxDB for historical data
        influxDBService.writeMarketData(event);
        
        // Trigger price alerts
        alertService.checkPriceAlerts(event);
        
        // Broadcast to WebSocket clients
        websocketService.broadcastMarketData(event);
    }
}

// Portfolio stream processor with aggregation
@Component
public class PortfolioProcessor {
    
    @KafkaListener(topics = "order-events")
    public void processOrderEvent(@Payload OrderEvent event) {
        if (event.getEventType().equals("FILLED")) {
            // Update position in database
            positionService.updatePosition(event);
            
            // Recalculate portfolio metrics
            PortfolioMetrics metrics = portfolioService.calculateMetrics(
                event.getUserId()
            );
            
            // Cache updated portfolio
            redisTemplate.opsForValue().set(
                "portfolio:" + event.getUserId(),
                metrics,
                Duration.ofMinutes(5)
            );
            
            // Broadcast update to user's WebSocket connection
            websocketService.sendToUser(
                event.getUserId(),
                new PortfolioUpdateMessage(metrics)
            );
        }
    }
}
```

### WebSocket Connection Management

```java
@Component
public class WebSocketConnectionManager {
    
    private final Map<String, Set<WebSocketSession>> userSessions = 
        new ConcurrentHashMap<>();
    
    private final Map<String, Set<String>> symbolSubscriptions = 
        new ConcurrentHashMap<>();
    
    @EventListener
    public void handleWebSocketConnectEvent(SessionConnectedEvent event) {
        String userId = extractUserId(event);
        String sessionId = event.getMessage().getHeaders()
            .get("simpSessionId", String.class);
            
        userSessions.computeIfAbsent(userId, k -> new HashSet<>())
            .add(sessionId);
        
        log.info("User {} connected with session {}", userId, sessionId);
    }
    
    @EventListener  
    public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        // Remove session from all user mappings
        userSessions.values().forEach(sessions -> sessions.remove(sessionId));
        
        // Clean up symbol subscriptions
        symbolSubscriptions.values().forEach(sessions -> 
            sessions.remove(sessionId));
    }
    
    public void subscribeToSymbol(String sessionId, String symbol) {
        symbolSubscriptions.computeIfAbsent(symbol, k -> new HashSet<>())
            .add(sessionId);
    }
    
    public void broadcastMarketData(String symbol, MarketDataEvent data) {
        Set<String> sessions = symbolSubscriptions.get(symbol);
        if (sessions != null) {
            sessions.forEach(sessionId -> {
                try {
                    messagingTemplate.convertAndSendToUser(
                        sessionId, 
                        "/topic/market-data", 
                        data
                    );
                } catch (Exception e) {
                    log.error("Failed to send market data to session {}", 
                        sessionId, e);
                }
            });
        }
    }
}
```

---

## Security Architecture

### Authentication & Authorization

#### JWT Token Strategy

```java
@Service
public class JwtTokenService {
    
    public static class TokenPair {
        private String accessToken;  // 15 minutes
        private String refreshToken; // 7 days
    }
    
    public TokenPair generateTokens(User user) {
        Instant now = Instant.now();
        
        // Access token with limited scope
        String accessToken = Jwts.builder()
            .setSubject(user.getUserId().toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(900))) // 15 mins
            .claim("type", "access")
            .claim("roles", user.getRoles())
            .claim("brokers", user.getConnectedBrokers())
            .signWith(accessTokenKey, SignatureAlgorithm.RS256)
            .compact();
        
        // Refresh token with limited scope
        String refreshToken = Jwts.builder()
            .setSubject(user.getUserId().toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(604800))) // 7 days
            .claim("type", "refresh")
            .claim("jti", UUID.randomUUID().toString()) // Unique ID
            .signWith(refreshTokenKey, SignatureAlgorithm.RS256)
            .compact();
        
        // Store refresh token in Redis for revocation capability
        redisTemplate.opsForValue().set(
            "refresh_token:" + refreshToken,
            user.getUserId().toString(),
            Duration.ofDays(7)
        );
        
        return new TokenPair(accessToken, refreshToken);
    }
}
```

#### OAuth Broker Integration Security

```java
@Service
public class BrokerOAuthService {
    
    public String initiateBrokerConnection(String userId, String brokerId) {
        // Generate state parameter for CSRF protection
        String state = generateSecureState(userId, brokerId);
        
        // Store state in Redis with expiration
        redisTemplate.opsForValue().set(
            "oauth_state:" + state,
            new OAuthState(userId, brokerId),
            Duration.ofMinutes(10)
        );
        
        // Build authorization URL
        BrokerConfig config = brokerConfigService.getConfig(brokerId);
        return config.getAuthUrl() + "?" +
            "client_id=" + config.getClientId() + "&" +
            "response_type=code&" +
            "redirect_uri=" + config.getRedirectUri() + "&" +
            "state=" + state + "&" +
            "scope=" + config.getScope();
    }
    
    public BrokerConnection handleCallback(String code, String state) {
        // Validate state parameter
        OAuthState oauthState = redisTemplate.opsForValue()
            .get("oauth_state:" + state);
        if (oauthState == null) {
            throw new SecurityException("Invalid or expired state");
        }
        
        // Exchange code for tokens
        BrokerTokenResponse tokens = exchangeCodeForTokens(
            oauthState.getBrokerId(), code
        );
        
        // Encrypt and store tokens
        BrokerConnection connection = new BrokerConnection();
        connection.setUserId(oauthState.getUserId());
        connection.setBrokerId(oauthState.getBrokerId());
        connection.setAccessToken(encryptToken(tokens.getAccessToken()));
        connection.setRefreshToken(encryptToken(tokens.getRefreshToken()));
        connection.setTokenExpiresAt(tokens.getExpiresAt());
        
        return brokerConnectionRepository.save(connection);
    }
}
```

### Data Encryption

```java
@Service
public class EncryptionService {
    
    private final AESUtil aesUtil;
    private final RSAUtil rsaUtil;
    
    // AES-256 for bulk data encryption
    public String encryptSensitiveData(String plainText) {
        return aesUtil.encrypt(plainText, getDataEncryptionKey());
    }
    
    public String decryptSensitiveData(String encryptedText) {
        return aesUtil.decrypt(encryptedText, getDataEncryptionKey());
    }
    
    // RSA for key exchange and small data
    public String encryptWithPublicKey(String plainText) {
        return rsaUtil.encryptWithPublicKey(plainText, getPublicKey());
    }
    
    // Database-level encryption for PII
    @PrePersist
    @PreUpdate
    public void encryptEntity(Object entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Encrypted.class)) {
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(entity);
                    if (value != null) {
                        field.set(entity, encryptSensitiveData(value));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Encryption failed", e);
                }
            }
        }
    }
}
```

---

## Integration Architecture

### Broker API Integration

#### Unified Broker Interface

```java
public interface BrokerService {
    
    // Portfolio operations
    Portfolio getPortfolio(String userId, String brokerId);
    List<Position> getPositions(String userId, String brokerId);
    
    // Order operations
    OrderResponse placeOrder(String userId, String brokerId, OrderRequest order);
    OrderResponse modifyOrder(String userId, String brokerId, String orderId, 
                            ModifyOrderRequest request);
    void cancelOrder(String userId, String brokerId, String orderId);
    List<Order> getOrders(String userId, String brokerId, OrderFilter filter);
    
    // Market data
    Quote getQuote(String symbol, String exchange);
    OrderBook getOrderBook(String symbol, String exchange);
    List<Trade> getTrades(String symbol, String exchange, int limit);
    
    // Connection management
    boolean testConnection(String userId, String brokerId);
    void refreshTokens(String userId, String brokerId);
}

@Service
public class ZerodhaService implements BrokerService {
    
    private final RestTemplate restTemplate;
    private final BrokerConnectionRepository connectionRepository;
    
    @Override
    public Portfolio getPortfolio(String userId, String brokerId) {
        BrokerConnection connection = getValidConnection(userId, brokerId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(decryptToken(connection.getAccessToken()));
        
        try {
            ResponseEntity<ZerodhaPortfolioResponse> response = restTemplate.exchange(
                "https://api.kite.trade/portfolio/holdings",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ZerodhaPortfolioResponse.class
            );
            
            return mapToStandardPortfolio(response.getBody());
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                refreshTokens(userId, brokerId);
                return getPortfolio(userId, brokerId); // Retry once
            }
            throw new BrokerIntegrationException("Portfolio fetch failed", e);
        }
    }
    
    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3)
    public OrderResponse placeOrder(String userId, String brokerId, 
                                  OrderRequest order) {
        BrokerConnection connection = getValidConnection(userId, brokerId);
        
        // Map standard order to Zerodha format
        ZerodhaOrderRequest zerodhaOrder = mapToZerodhaOrder(order);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(decryptToken(connection.getAccessToken()));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            ResponseEntity<ZerodhaOrderResponse> response = restTemplate.exchange(
                "https://api.kite.trade/orders/regular",
                HttpMethod.POST,
                new HttpEntity<>(zerodhaOrder, headers),
                ZerodhaOrderResponse.class
            );
            
            return mapToStandardOrderResponse(response.getBody());
            
        } catch (HttpClientErrorException e) {
            handleBrokerError(e, userId, brokerId);
            throw new BrokerIntegrationException("Order placement failed", e);
        }
    }
}
```

#### Circuit Breaker Pattern

```java
@Component
public class BrokerCircuitBreaker {
    
    private final Map<String, CircuitBreaker> circuitBreakers = 
        new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeCircuitBreakers() {
        List<String> brokers = Arrays.asList("zerodha", "groww", "angelone");
        
        brokers.forEach(brokerId -> {
            CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults(brokerId);
            
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Broker {} circuit breaker state changed: {} -> {}",
                            brokerId, event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()));
            
            circuitBreakers.put(brokerId, circuitBreaker);
        });
    }
    
    public <T> T executeBrokerOperation(String brokerId, 
                                       Supplier<T> operation) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(brokerId);
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                return operation.get();
            } catch (BrokerTimeoutException e) {
                log.warn("Broker {} operation timed out", brokerId);
                throw e;
            } catch (BrokerUnavailableException e) {
                log.error("Broker {} is unavailable", brokerId, e);
                throw e;
            }
        });
    }
}
```

### Order Routing Algorithm

```java
@Service
public class IntelligentOrderRouter {
    
    public OrderRoutingPlan createRoutingPlan(OrderRequest order, 
                                            String userId) {
        // Get user's connected brokers
        List<BrokerConnection> connections = getActiveBrokerConnections(userId);
        
        // Analyze order characteristics
        OrderAnalysis analysis = analyzeOrder(order);
        
        // Get market conditions
        MarketConditions market = getMarketConditions(order.getSymbol());
        
        // Calculate optimal routing
        List<RouteAllocation> allocations = new ArrayList<>();
        
        if (analysis.getOrderSize() > market.getAverageTradeSize() * 10) {
            // Large order - split across multiple brokers
            allocations = splitLargeOrder(order, connections, market);
        } else {
            // Small order - route to best broker
            String bestBroker = selectBestBroker(order, connections, market);
            allocations.add(new RouteAllocation(bestBroker, order.getQuantity()));
        }
        
        return new OrderRoutingPlan(allocations, analysis.getEstimatedTime());
    }
    
    private String selectBestBroker(OrderRequest order, 
                                  List<BrokerConnection> connections,
                                  MarketConditions market) {
        return connections.stream()
            .collect(Collectors.toMap(
                BrokerConnection::getBrokerId,
                conn -> calculateBrokerScore(conn, order, market)
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(connections.get(0).getBrokerId());
    }
    
    private double calculateBrokerScore(BrokerConnection connection, 
                                      OrderRequest order,
                                      MarketConditions market) {
        double score = 0.0;
        
        // Execution speed (40% weight)
        score += getBrokerExecutionSpeed(connection.getBrokerId()) * 0.4;
        
        // Brokerage cost (30% weight)  
        score += (1.0 - getBrokerageCost(connection.getBrokerId(), order)) * 0.3;
        
        // Liquidity access (20% weight)
        score += getBrokerLiquidity(connection.getBrokerId(), order.getSymbol()) * 0.2;
        
        // Reliability (10% weight)
        score += getBrokerReliability(connection.getBrokerId()) * 0.1;
        
        return score;
    }
}
```

---

## Scaling & Performance

### Horizontal Scaling Strategy

#### Auto-Scaling Configuration

```yaml
# Kubernetes HPA configuration
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: trading-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trading-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: market-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: market-service
  minReplicas: 2
  maxReplicas: 15
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
  - type: External
    external:
      metric:
        name: kafka_consumer_lag
      target:
        type: Value
        value: "1000"
```

#### Database Scaling

```sql
-- Read replica configuration
CREATE SUBSCRIPTION trading_replica 
CONNECTION 'host=primary-db port=5432 dbname=trademaster' 
PUBLICATION trading_publication;

-- Partitioning strategy for orders table
CREATE TABLE trading.orders_2024_q1 PARTITION OF trading.orders
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE trading.orders_2024_q2 PARTITION OF trading.orders  
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

-- Index on partitioned table
CREATE INDEX CONCURRENTLY idx_orders_2024_q1_user_status 
ON trading.orders_2024_q1(user_id, status);
```

### Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        
        // Configure cache-specific settings
        Map<String, RedisCacheConfiguration> caches = new HashMap<>();
        
        // Market data cache - 30 seconds TTL
        caches.put("market-data", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(30))
            .prefixCacheNameWith("market:"));
        
        // User portfolio cache - 5 minutes TTL
        caches.put("portfolio", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .prefixCacheNameWith("portfolio:"));
        
        // Order book cache - 1 second TTL
        caches.put("orderbook", RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(1))
            .prefixCacheNameWith("orderbook:"));
        
        return builder.withInitialCacheConfigurations(caches).build();
    }
}

@Service
public class CachedMarketDataService {
    
    @Cacheable(value = "market-data", key = "#symbol")
    public Quote getQuote(String symbol) {
        return brokerService.getQuote(symbol);
    }
    
    @CacheEvict(value = "portfolio", key = "#userId")
    public void invalidatePortfolioCache(String userId) {
        // Cache will be refreshed on next access
    }
    
    @Cacheable(value = "orderbook", key = "#symbol")
    public OrderBook getOrderBook(String symbol) {
        return brokerService.getOrderBook(symbol);
    }
}
```

### Performance Optimization

#### Connection Pooling

```java
@Configuration
public class DatabaseConfiguration {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setIdleTimeout(300000); // 5 minutes
        config.setConnectionTimeout(20000); // 20 seconds
        config.setLeakDetectionThreshold(300000); // 5 minutes
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(5);
        config.setReadOnly(true);
        
        return new HikariDataSource(config);
    }
}
```

---

## Deployment Architecture

### Kubernetes Deployment

```yaml
# Trading Service Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trading-service
  labels:
    app: trading-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: trading-service
  template:
    metadata:
      labels:
        app: trading-service
        version: v1
    spec:
      containers:
      - name: trading-service
        image: trademaster/trading-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30

---
# Market Service Deployment  
apiVersion: apps/v1
kind: Deployment
metadata:
  name: market-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: market-service
  template:
    metadata:
      labels:
        app: market-service
    spec:
      containers:
      - name: market-service
        image: trademaster/market-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: KAFKA_BROKERS
          value: "kafka-cluster:9092"
        - name: INFLUXDB_URL
          valueFrom:
            configMapKeyRef:
              name: influxdb-config
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi" 
            cpu: "500m"

---
# Service definitions
apiVersion: v1
kind: Service
metadata:
  name: trading-service
spec:
  selector:
    app: trading-service
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: market-service
spec:
  selector:
    app: market-service
  ports:
  - port: 80
    targetPort: 8081
  type: ClusterIP
```

### Infrastructure as Code

```terraform
# AWS EKS Cluster
module "eks" {
  source = "terraform-aws-modules/eks/aws"
  
  cluster_name    = "trademaster-prod"
  cluster_version = "1.28"
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets
  
  node_groups = {
    main = {
      desired_capacity = 5
      max_capacity     = 20
      min_capacity     = 3
      
      instance_types = ["t3.large", "t3.xlarge"]
      
      k8s_labels = {
        Environment = "production"
        Application = "trademaster"
      }
    }
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "trademaster-postgres"
  
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.large"
  
  allocated_storage     = 100
  max_allocated_storage = 1000
  storage_encrypted     = true
  
  db_name  = "trademaster"
  username = "postgres"
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  performance_insights_enabled = true
  monitoring_interval         = 60
  monitoring_role_arn         = aws_iam_role.rds_monitoring.arn
}

# ElastiCache Redis
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "trademaster-redis"
  description                = "TradeMaster Redis cluster"
  
  port                = 6379
  parameter_group_name = "default.redis7"
  node_type           = "cache.t3.micro"
  
  num_cache_clusters = 2
  
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "trademaster-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets
  
  enable_deletion_protection = true
  
  access_logs {
    bucket  = aws_s3_bucket.alb_logs.bucket
    enabled = true
  }
}
```

---

## Monitoring & Observability

### Application Performance Monitoring

```java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class OrderService {
    
    private final Counter orderPlacedCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeOrdersGauge;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.orderPlacedCounter = Counter.builder("orders.placed")
            .description("Number of orders placed")
            .tag("service", "trading")
            .register(meterRegistry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(meterRegistry);
            
        this.activeOrdersGauge = Gauge.builder("orders.active")
            .description("Number of active orders")
            .register(meterRegistry, this, OrderService::getActiveOrderCount);
    }
    
    @Timed(value = "orders.place", description = "Time taken to place order")
    public OrderResponse placeOrder(OrderRequest order) {
        return Timer.Sample.start(orderProcessingTimer)
            .stop(meterRegistry -> {
                orderPlacedCounter.increment();
                return processOrder(order);
            });
    }
    
    private int getActiveOrderCount() {
        return orderRepository.countByStatus(OrderStatus.ACTIVE);
    }
}
```

### Logging Configuration

```yaml
# logback-spring.xml
<configuration>
    <springProfile name="production">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
        
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/trademaster/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/trademaster/application.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Health Check Implementation

```java
@Component
public class BrokerHealthIndicator implements HealthIndicator {
    
    private final Map<String, BrokerService> brokerServices;
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        
        Map<String, Object> brokerStatuses = new HashMap<>();
        boolean allHealthy = true;
        
        for (Map.Entry<String, BrokerService> entry : brokerServices.entrySet()) {
            String brokerId = entry.getKey();
            BrokerService service = entry.getValue();
            
            try {
                boolean isHealthy = service.healthCheck();
                brokerStatuses.put(brokerId, isHealthy ? "UP" : "DOWN");
                if (!isHealthy) allHealthy = false;
                
            } catch (Exception e) {
                brokerStatuses.put(brokerId, "ERROR: " + e.getMessage());
                allHealthy = false;
            }
        }
        
        healthBuilder.withDetails(brokerStatuses);
        
        if (allHealthy) {
            healthBuilder.up();
        } else {
            healthBuilder.down();
        }
        
        return healthBuilder.build();
    }
}

@RestController
@RequestMapping("/actuator")
public class CustomHealthEndpoint {
    
    @GetMapping("/health/trading")
    public ResponseEntity<Map<String, Object>> tradingHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Check order processing capability
        boolean canProcessOrders = orderService.canProcessOrders();
        health.put("order_processing", canProcessOrders ? "UP" : "DOWN");
        
        // Check broker connectivity
        Map<String, String> brokerStatus = brokerHealthService.checkAllBrokers();
        health.put("brokers", brokerStatus);
        
        // Check market data feed
        boolean marketDataActive = marketDataService.isMarketDataActive();
        health.put("market_data", marketDataActive ? "UP" : "DOWN");
        
        boolean overallHealthy = canProcessOrders && marketDataActive && 
            brokerStatus.values().stream().anyMatch("UP"::equals);
        
        HttpStatus status = overallHealthy ? HttpStatus.OK : 
                           HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(status).body(health);
    }
}
```

---

**✅ Technical Design Document Complete**

This comprehensive technical design specification provides detailed architecture, database design, API specifications, security measures, and deployment strategies for TradeMaster Orchestrator. The document covers all aspects needed for development team implementation.

**Key Technical Highlights:**
- **Microservices Architecture** with Spring Boot 3.x + Java 21
- **Event-Driven Real-Time System** with Kafka + WebSocket
- **Multi-Database Strategy** (PostgreSQL + InfluxDB + Redis + MongoDB)
- **Intelligent Order Routing** with circuit breaker patterns
- **Enterprise Security** with OAuth + JWT + AES encryption
- **Auto-Scaling Infrastructure** on Kubernetes
- **Comprehensive Monitoring** with Prometheus + ELK stack

Ready to proceed with wireframes and development planning using the existing design system.