# Technical Stack

## Backend Framework
- **Application Framework:** Spring Boot 3.5.4 (Java 24)
- **Database System:** PostgreSQL 17+ with Redis 7+ caching
- **Security:** Spring Security 7 (if not then 6) with JWT authentication, AWS KMS encryption
- **API Gateway:** Kong API Gateway with rate limiting and request transformation
- **Testing:** JUnit 5, TestContainers, Mockito with 95% coverage target

## Frontend Framework
- **JavaScript Framework:** React 18.2.0 with TypeScript 5.2.2
- **Build Tool:** Vite 4.5.0 with ES modules
- **Import Strategy:** ES modules with TypeScript strict mode
- **CSS Framework:** Tailwind CSS 3.3.0 with PostCSS
- **UI Component Library:** Radix UI with shadcn/ui components

## Development Tools
- **Testing Framework:** Vitest for unit testing, Playwright for E2E testing
- **Documentation:** Storybook for component development
- **Code Quality:** ESLint, Prettier, SonarQube integration
- **Database Migrations:** Flyway with version-controlled schema evolution

## Infrastructure & Deployment
- **Application Hosting:** AWS ECS/EKS with Docker containerization
- **Database Hosting:** AWS RDS PostgreSQL with Multi-AZ deployment
- **Cache Hosting:** AWS ElastiCache Redis cluster
- **Asset Hosting:** AWS CloudFront CDN with S3 storage
- **Deployment Solution:** Docker Compose (development), Kubernetes (production)

## Security & Compliance
- **Encryption:** AES-256-GCM at rest, TLS 1.3 in transit
- **Key Management:** AWS KMS with automatic key rotation
- **Authentication:** JWT with 15-minute expiry, refresh token rotation
- **Session Management:** Redis-based with 24-hour TTL
- **Audit Logging:** Blockchain-style integrity with cryptographic hashes

## Monitoring & Observability
- **Metrics:** Prometheus with Grafana dashboards
- **Logging:** Structured JSON logs with correlation IDs
- **Health Monitoring:** Spring Boot Actuator endpoints
- **Performance:** APM with distributed tracing
- **Alerting:** PagerDuty integration for critical issues

## AI/ML Services
- **ML Framework:** Python 3.11 with TensorFlow 2.15/PyTorch 2.1
- **API Framework:** FastAPI for ML service endpoints
- **Model Serving:** TensorFlow Serving with A/B testing
- **Data Pipeline:** Apache Kafka for real-time data processing
- **Feature Store:** Redis with time-series data optimization

## Market Data Integration
- **Exchange Connectivity:** NSE, BSE, MCX WebSocket APIs
- **Data Processing:** Apache Kafka with real-time streaming
- **Storage:** TimescaleDB for time-series market data
- **Caching:** Redis with configurable TTL per data type
- **Backup:** S3 with cross-region replication

## Mobile Platform
- **Framework:** React Native 0.73+ with TypeScript
- **Navigation:** React Navigation 6.x
- **State Management:** Redux Toolkit with RTK Query
- **Notifications:** Firebase Cloud Messaging
- **Biometric Auth:** React Native Biometrics integration

## Code Repository
- **Repository URL:** https://github.com/trademaster/trademaster-platform
- **Branching Strategy:** GitFlow with feature branches
- **CI/CD:** GitHub Actions with automated testing and deployment
- **Code Review:** Required PR approvals with automated quality checks