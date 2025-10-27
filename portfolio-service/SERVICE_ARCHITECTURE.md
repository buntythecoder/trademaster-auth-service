# Portfolio Service - Service Architecture Documentation

**Version**: 2.0.0
**Last Updated**: 2025-10-07
**Status**: Production Ready

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture Layers](#architecture-layers)
3. [Component Diagram](#component-diagram)
4. [Data Flow](#data-flow)
5. [Integration Points](#integration-points)
6. [Design Patterns](#design-patterns)
7. [Technology Stack](#technology-stack)
8. [Performance Characteristics](#performance-characteristics)

---

## Overview

### Service Purpose
Portfolio Management Service provides comprehensive portfolio tracking, P&L calculation, risk management, and performance analytics for the TradeMaster trading platform.

### Key Responsibilities
- **Portfolio Lifecycle Management**: Create, update, close portfolios
- **Position Tracking**: Real-time position monitoring and valuation
- **P&L Calculations**: Realized and unrealized profit/loss calculation
- **Risk Analytics**: Portfolio risk assessment and monitoring
- **Performance Reporting**: Time-weighted returns and performance metrics
- **Internal APIs**: Service-to-service communication for trading operations

### Service Boundaries
- **Owns**: Portfolio data, positions, transactions, risk limits
- **Does NOT Own**: User authentication, order execution, market data
- **Depends On**: auth-service, trading-service, broker-auth-service, event-bus-service

---

## Architecture Layers

### 1. **Presentation Layer** (Controllers)
```
controller/
├── PortfolioController.java          # External API endpoints
├── PositionController.java           # Position management API
├── InternalPortfolioController.java  # Service-to-service APIs
├── GreetingsController.java          # Service discovery
└── ApiV2HealthController.java        # Health checks
```

**Responsibilities**:
- HTTP request/response handling
- Input validation (Jakarta Validation)
- JWT authentication (external APIs)
- API key authentication (internal APIs)
- OpenAPI documentation
- CORS handling

### 2. **Application Layer** (Services)
```
service/
├── PortfolioService.java             # Portfolio business logic
├── PositionService.java              # Position management
├── PnLCalculationService.java        # P&L calculation engine
├── PortfolioAnalyticsService.java    # Performance analytics
├── PortfolioRiskService.java         # Risk assessment
└── impl/                             # Implementation classes
```

**Responsibilities**:
- Business logic orchestration
- Transaction management (@Transactional)
- Async processing (Virtual Threads)
- Circuit breaker patterns
- Event publishing
- Metrics collection

### 3. **Domain Layer** (Entities & DTOs)
```
entity/
├── Portfolio.java                    # Core portfolio entity
├── Position.java                     # Position entity
├── Transaction.java                  # Transaction history
└── RiskLimit.java                    # Risk limit configuration

dto/
├── CreatePortfolioRequest.java       # Request DTOs
├── PortfolioSummary.java             # Response DTOs
├── PnLBreakdown.java                 # Calculation results
└── RiskAssessmentResult.java         # Risk analytics
```

**Responsibilities**:
- Domain model definition
- Business rules enforcement
- Data validation
- Immutability (Records, @Builder)

### 4. **Infrastructure Layer** (Repositories & Config)
```
repository/
├── PortfolioRepository.java          # Portfolio data access
├── PositionRepository.java           # Position data access
├── TransactionRepository.java        # Transaction history
└── RiskLimitRepository.java          # Risk configuration

config/
├── PortfolioCircuitBreakerConfig.java  # Resilience4j configuration
├── ConsulConfig.java                   # Service discovery
├── KongConfiguration.java              # API gateway integration
└── OpenApiConfiguration.java           # API documentation
```

**Responsibilities**:
- Database access (JPA/Hibernate)
- Connection pool management (HikariCP)
- Circuit breaker configuration
- Service registration (Consul)
- API gateway integration (Kong)

---

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Portfolio Service                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐ │
│  │  Portfolio   │  │  Position    │  │  Internal APIs  │ │
│  │  Controller  │  │  Controller  │  │   Controller    │ │
│  └──────┬───────┘  └──────┬───────┘  └────────┬────────┘ │
│         │                 │                   │           │
│         ├─────────────────┴───────────────────┤           │
│         │                                     │           │
│  ┌──────▼─────────────────────────────────────▼────────┐ │
│  │           Portfolio Service Layer                   │ │
│  │  (Business Logic, Validation, Orchestration)        │ │
│  └──────┬──────────────────────────────────────────────┘ │
│         │                                                 │
│  ┌──────▼─────────────────────────────────────────────┐ │
│  │         Repository Layer (JPA/Hibernate)           │ │
│  └──────┬─────────────────────────────────────────────┘ │
│         │                                                │
└─────────┼────────────────────────────────────────────────┘
          │
          ▼
    ┌─────────────┐
    │ PostgreSQL  │
    │  Database   │
    └─────────────┘
```

---

## Data Flow

### 1. **Portfolio Creation Flow**
```
User Request
    │
    ▼
PortfolioController.createPortfolio()
    │
    ├── JWT validation (Spring Security)
    ├── Input validation (Jakarta Validation)
    │
    ▼
PortfolioService.createPortfolio()
    │
    ├── Check existing portfolios
    ├── Apply business rules
    ├── Create portfolio entity
    │
    ▼
PortfolioRepository.save()
    │
    ├── JPA entity persistence
    ├── Database transaction
    │
    ▼
Event Bus Publish
    │
    ├── portfolio.created event
    │
    ▼
Return Portfolio Response
```

### 2. **Position Update Flow (Async)**
```
Trade Execution Event
    │
    ▼
PositionService.updatePositionFromTrade()
    │
    ├── Execute in Virtual Thread
    ├── Circuit breaker protection
    │
    ▼
Position Calculation
    │
    ├── Calculate new quantity
    ├── Update average price
    ├── Calculate P&L
    │
    ▼
PositionRepository.save()
    │
    ├── Atomic update
    │
    ▼
Portfolio Valuation Update
    │
    ├── Recalculate portfolio value
    ├── Update unrealized P&L
    │
    ▼
PortfolioRepository.updatePortfolioValuation()
```

### 3. **Internal API Call Flow**
```
Service-to-Service Call
    │
    ▼
ServiceApiKeyFilter
    │
    ├── API key validation
    ├── Service identity verification
    │
    ▼
InternalPortfolioController
    │
    ├── /api/internal/greetings
    ├── /api/internal/v1/portfolio/users/{userId}/summary
    ├── /api/internal/v1/portfolio/users/{userId}/validate-buying-power
    │
    ▼
PortfolioService (Direct call - no security facade)
    │
    ├── Lightweight processing
    ├── No additional validation
    │
    ▼
Return Internal Response
```

---

## Integration Points

### Upstream Dependencies

#### 1. **Auth Service** (`auth-service`)
- **Purpose**: User authentication and authorization
- **Protocol**: HTTP/REST
- **Endpoints Used**:
  - `GET /api/internal/users/{userId}/validate`
  - `POST /api/internal/auth/verify-token`
- **Circuit Breaker**: Enabled (5s timeout)

#### 2. **Trading Service** (`trading-service`)
- **Purpose**: Order execution coordination
- **Protocol**: HTTP/REST + Events
- **Endpoints Used**:
  - `POST /api/internal/orders/validate`
  - `GET /api/internal/orders/{orderId}/status`
- **Circuit Breaker**: Enabled (10s timeout)

#### 3. **Broker Auth Service** (`broker-auth-service`)
- **Purpose**: Broker integration and session management
- **Protocol**: HTTP/REST
- **Endpoints Used**:
  - `GET /api/internal/brokers/{brokerId}/session-status`
- **Circuit Breaker**: Enabled (5s timeout)

#### 4. **Event Bus Service** (`event-bus-service`)
- **Purpose**: Async event distribution
- **Protocol**: Kafka/RabbitMQ
- **Events Published**:
  - `portfolio.created`
  - `portfolio.updated`
  - `position.changed`
  - `pnl.calculated`
- **Events Consumed**:
  - `trade.executed`
  - `market.data.updated`

### Downstream Consumers

#### Internal API Clients
- **Trading Service**: Buying power validation
- **Risk Service**: Portfolio risk assessment
- **Notification Service**: Alert triggers
- **Event Bus Service**: Service health check

---

## Design Patterns

### 1. **Repository Pattern**
- **Purpose**: Data access abstraction
- **Implementation**: Spring Data JPA repositories
- **Example**: `PortfolioRepository`, `PositionRepository`

### 2. **Builder Pattern**
- **Purpose**: Complex object construction
- **Implementation**: Lombok `@Builder`
- **Example**: Portfolio entity creation

### 3. **Factory Pattern**
- **Purpose**: Object creation with business logic
- **Implementation**: Functional factories with enum-based implementations
- **Example**: P&L calculation factory

### 4. **Strategy Pattern**
- **Purpose**: Algorithm selection
- **Implementation**: Function-based strategies
- **Example**: Cost basis calculation (FIFO, LIFO, Average)

### 5. **Circuit Breaker Pattern**
- **Purpose**: Fault tolerance
- **Implementation**: Resilience4j
- **Example**: External service calls with fallbacks

### 6. **Observer Pattern**
- **Purpose**: Event notification
- **Implementation**: Spring Events + Kafka
- **Example**: Portfolio change notifications

### 7. **Facade Pattern**
- **Purpose**: Simplified interface for external access
- **Implementation**: SecurityFacade + SecurityMediator
- **Usage**: External API security (not used for internal service-to-service)

---

## Technology Stack

### Core Technologies
- **Java 24** with Virtual Threads (`--enable-preview`)
- **Spring Boot 3.5.3** (Spring MVC, NOT WebFlux)
- **JPA/Hibernate** for persistence
- **PostgreSQL 16** database
- **HikariCP** connection pooling

### Libraries & Frameworks
- **Lombok**: Boilerplate reduction
- **MapStruct**: DTO mapping
- **Resilience4j**: Circuit breakers
- **Micrometer**: Metrics collection
- **TestContainers**: Integration testing

### Infrastructure
- **Consul**: Service discovery
- **Kong**: API gateway
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards

---

## Performance Characteristics

### Response Time Targets
- **Portfolio Creation**: <200ms
- **Portfolio Retrieval**: <50ms
- **Position Update**: <100ms
- **P&L Calculation**: <150ms
- **Risk Assessment**: <200ms

### Throughput Targets
- **Concurrent Users**: 10,000+
- **Requests per Second**: 5,000+
- **Database Connections**: 50 (HikariCP pool)

### Scalability
- **Horizontal Scaling**: Stateless design, supports multiple instances
- **Database Scaling**: Read replicas for analytics queries
- **Caching**: Redis for frequently accessed portfolios
- **Virtual Threads**: High concurrency with low resource usage

---

## Security Architecture

### External Access (Full Security)
```
External Request → Kong Gateway → JWT Validation → SecurityFacade → Business Logic
```

### Internal Access (Lightweight Security)
```
Internal Request → ServiceApiKeyFilter → Direct Service Call → Business Logic
```

### Security Features
- **JWT Authentication**: External APIs
- **API Key Authentication**: Internal APIs
- **Role-Based Access Control**: Method-level security
- **Input Sanitization**: Functional validation chains
- **Audit Logging**: All security events logged
- **Zero Trust**: All requests validated

---

## Monitoring & Observability

### Health Checks
- **Standard**: `/actuator/health`
- **Kong-compatible**: `/api/v2/health`
- **Internal**: `/api/internal/v1/portfolio/health`

### Metrics
- **Prometheus Endpoints**: `/actuator/prometheus`
- **Custom Metrics**:
  - Portfolio creation rate
  - P&L calculation duration
  - Position update latency
  - Circuit breaker state

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Log Levels**: DEBUG (dev), INFO (prod)
- **Audit Trail**: All financial operations logged

---

## Deployment Architecture

```
                    ┌─────────────┐
                    │  Kong API   │
                    │   Gateway   │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │Portfolio│      │Portfolio│      │Portfolio│
    │Service 1│      │Service 2│      │Service 3│
    └────┬────┘      └────┬────┘      └────┬────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                    ┌──────▼──────┐
                    │ PostgreSQL  │
                    │   Primary   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ PostgreSQL  │
                    │  Read Replica│
                    └─────────────┘
```

---

## API Documentation

- **OpenAPI Spec**: `http://localhost:8083/v3/api-docs`
- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **Redoc**: `http://localhost:8083/redoc`

---

## Contact & Support

- **Development Team**: TradeMaster Development Team
- **Email**: dev@trademaster.com
- **Documentation**: See `docs/` directory
- **Issue Tracker**: GitHub Issues

---

**Document Version**: 1.0.0
**Last Review**: 2025-10-07
**Next Review**: 2025-11-07
