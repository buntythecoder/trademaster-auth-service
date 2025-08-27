# TradeMaster Subscription Service

Comprehensive subscription management service for the TradeMaster platform, built with Java 24 + Virtual Threads for optimal performance.

## 🚀 Features

- **Multi-tier Subscription System**: FREE, PRO, AI PREMIUM, and INSTITUTIONAL tiers
- **Real-time Usage Tracking**: Feature-based usage monitoring with automatic limit enforcement
- **Automated Billing**: Recurring billing with payment gateway integration and retry logic
- **Event-driven Architecture**: Kafka-based events for system integration
- **Trial Management**: Flexible trial periods with expiration handling
- **Virtual Threads**: Java 24 Virtual Threads for high-performance concurrent operations
- **Comprehensive Monitoring**: Prometheus metrics and structured logging
- **API Documentation**: OpenAPI/Swagger documentation with interactive UI

## 🏗️ Architecture

### Technology Stack
- **Java 24** with Virtual Threads (Preview Features)
- **Spring Boot 3.5.3** with Spring MVC
- **PostgreSQL** with Flyway migrations
- **Apache Kafka** for event streaming
- **Redis** for caching
- **Prometheus** for metrics
- **Docker** for containerization

### Service Architecture
```
┌─────────────────────────────────────────────────────┐
│                 Subscription Service                 │
├─────────────────┬───────────────┬───────────────────┤
│   Controllers   │   Services    │     Repositories  │
│                 │               │                   │
│ • Subscription  │ • Subscription│ • Subscription    │
│ • Usage         │ • Usage       │ • Usage Tracking  │
│ • Billing       │ • Billing     │ • History         │
└─────────────────┴───────────────┴───────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Kafka     │    │ PostgreSQL  │    │  Payment    │
│   Events    │    │  Database   │    │  Gateway    │
└─────────────┘    └─────────────┘    └─────────────┘
```

## 📊 Subscription Tiers

### FREE Tier - $0/month
- ✅ 1,000 API calls/month
- ✅ 3 portfolios
- ✅ 5 watchlists
- ✅ 10 alerts
- ✅ Basic analytics

### PRO Tier - $29.99/month
- ✅ 10,000 API calls/month
- ✅ 10 portfolios
- ✅ 25 watchlists
- ✅ 100 alerts
- ✅ Advanced analytics
- ✅ Data export

### AI PREMIUM Tier - $99.99/month
- ✅ 50,000 API calls/month
- ✅ 50 portfolios
- ✅ 100 watchlists
- ✅ 500 alerts
- ✅ 1,000 AI insights/month
- ✅ Priority support

### INSTITUTIONAL Tier - Custom
- ✅ Unlimited usage
- ✅ Dedicated support
- ✅ SLA guarantees
- ✅ Custom integrations

## 🚀 Quick Start

### Prerequisites
- Java 24 (with preview features enabled)
- PostgreSQL 15+
- Apache Kafka 2.8+
- Redis 6+
- Docker & Docker Compose

### Environment Setup
1. **Clone the repository**
   ```bash
   git clone https://github.com/trademaster/subscription-service.git
   cd subscription-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d postgres kafka redis
   ```

3. **Configure environment variables**
   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/trademaster_subscription
   export DATABASE_USERNAME=trademaster_user
   export DATABASE_PASSWORD=trademaster_password
   export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   export REDIS_URL=redis://localhost:6379
   ```

4. **Run database migrations**
   ```bash
   ./gradlew flywayMigrate
   ```

5. **Start the service**
   ```bash
   ./gradlew bootRun
   ```

### Verification
- **Health Check**: http://localhost:8086/actuator/health
- **API Documentation**: http://localhost:8086/swagger-ui.html
- **Metrics**: http://localhost:8086/actuator/prometheus

## 🔧 Configuration

### Application Configuration
Key configuration properties in `application.yml`:

```yaml
server:
  port: 8086

spring:
  threads:
    virtual:
      enabled: true  # MANDATORY: Virtual Threads

app:
  subscription:
    trial-period-days: 14
    grace-period-days: 7
    max-retry-attempts: 3
  
  services:
    payment-gateway:
      url: ${PAYMENT_GATEWAY_URL:http://localhost:8084}
      api-key: ${PAYMENT_GATEWAY_API_KEY}
```

### Virtual Threads Configuration
The service is optimized for Java 24 Virtual Threads:

```java
@Bean("subscriptionProcessingExecutor")
public Executor subscriptionProcessingExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

## 📡 API Documentation

### Core Endpoints

#### Subscription Management
- `POST /api/v1/subscriptions` - Create subscription
- `GET /api/v1/subscriptions/{id}` - Get subscription
- `GET /api/v1/subscriptions/users/{userId}/active` - Get active subscription
- `POST /api/v1/subscriptions/{id}/upgrade` - Upgrade subscription
- `POST /api/v1/subscriptions/{id}/cancel` - Cancel subscription

#### Usage Tracking
- `POST /api/v1/usage/check` - Check feature access
- `POST /api/v1/usage/increment` - Increment usage
- `POST /api/v1/usage/validate-increment` - Validate and increment atomically
- `GET /api/v1/usage/users/{userId}/stats` - Get usage statistics

### Authentication
All endpoints require JWT authentication:
```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
     -X GET http://localhost:8086/api/v1/subscriptions/users/123/active
```

### Error Responses
Structured error responses with HTTP status codes:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid subscription data",
  "errorCode": "VALIDATION_ERROR",
  "path": "/api/v1/subscriptions",
  "fieldErrors": {
    "tier": ["must not be null"],
    "billingCycle": ["must be MONTHLY, QUARTERLY, or ANNUALLY"]
  }
}
```

## 📊 Events & Integration

### Kafka Topics
The service publishes events to multiple Kafka topics:

- **subscription-events**: Lifecycle events (created, activated, cancelled)
- **usage-events**: Usage tracking and limit violations
- **billing-events**: Payment and billing events
- **notification-events**: User notifications

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "subscription.created",
  "timestamp": "2024-01-15T10:30:00Z",
  "subscriptionId": "uuid",
  "userId": "uuid",
  "tier": "PRO",
  "status": "ACTIVE",
  "payload": { ... },
  "correlationId": "uuid"
}
```

## 📈 Monitoring & Observability

### Metrics
Comprehensive Prometheus metrics:
- `subscription_created_total` - Total subscriptions created
- `subscription_billing_success_total` - Successful billing operations
- `subscription_usage_checks_total` - Usage access checks
- `subscription_limits_exceeded_total` - Usage limit violations

### Logging
Structured JSON logging with:
- Request/response correlation IDs
- User and subscription context
- Performance metrics
- Business events

### Health Checks
- Database connectivity
- Kafka producer/consumer health
- External service dependencies
- Usage tracking service status

## 🔄 Background Tasks

### Scheduled Operations
- **Recurring Billing**: Hourly processing of due subscriptions
- **Trial Expiration**: Daily checks for expiring trials
- **Usage Reset**: Monthly usage counter reset
- **Data Cleanup**: Daily cleanup of old records

### Cron Schedules
```yaml
Recurring Billing: "0 0 * * * *"     # Every hour
Trial Check:      "0 0 8,20 * * *"   # 8 AM and 8 PM daily
Usage Reset:      "0 0 2 1 * *"      # 2 AM on 1st of month
Cleanup:          "0 0 3 * * *"      # 3 AM daily
```

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Integration tests with TestContainers
./gradlew integrationTest

# All tests with coverage
./gradlew test jacocoTestReport
```

### Test Coverage
- Unit tests: Controllers, Services, Repositories
- Integration tests: Database, Kafka, Redis
- End-to-end tests: Complete subscription workflows

## 🚀 Deployment

### Docker Deployment
```bash
# Build image
docker build -t trademaster/subscription-service .

# Run container
docker run -p 8086:8086 \
  -e DATABASE_URL=jdbc:postgresql://db:5432/trademaster_subscription \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  trademaster/subscription-service
```

### Production Configuration
Key production settings:
```yaml
spring:
  profiles:
    active: production
  
logging:
  level:
    root: INFO
    com.trademaster: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## 🔐 Security

### Authentication & Authorization
- JWT-based authentication
- Role-based access control
- API rate limiting per subscription tier
- PCI DSS compliant payment handling

### Data Protection
- Encrypted sensitive data at rest
- TLS encryption in transit
- Audit logging for compliance
- GDPR compliance for user data

## 🛠️ Development

### Code Standards
- Java 24 with Virtual Threads
- Spring Boot 3.5.3 best practices
- CompletableFuture for async operations
- Structured logging and metrics
- Comprehensive error handling

### Project Structure
```
src/main/java/com/trademaster/subscription/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── enums/          # Enumeration classes
├── event/          # Event handling
├── exception/      # Custom exceptions
├── repository/     # Data repositories
├── scheduler/      # Background tasks
└── service/        # Business logic
```

## 🤝 Contributing

1. Follow TradeMaster coding standards
2. Ensure 90%+ test coverage
3. Update documentation for API changes
4. Include performance metrics for new features
5. Test with Virtual Threads compatibility

## 📝 License

Copyright © 2024 TradeMaster. All rights reserved.

---

**Service Status**: ✅ Production Ready  
**API Version**: v1  
**Last Updated**: January 2025  
**Maintainer**: TradeMaster Development Team