# Portfolio Service - Deployment Guide

**Version**: 2.0.0
**Last Updated**: 2025-10-07

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Environment Configuration](#environment-configuration)
6. [Database Migration](#database-migration)
7. [Health Checks](#health-checks)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Java 24** with `--enable-preview` flag
- **Gradle 8.5+**
- **PostgreSQL 16+**
- **Docker 24+** (for containerized deployment)
- **kubectl 1.28+** (for Kubernetes deployment)
- **Consul 1.17+** (for service discovery)

### Required Services
- **auth-service** (authentication)
- **trading-service** (order management)
- **broker-auth-service** (broker integration)
- **event-bus-service** (async messaging)
- **PostgreSQL** (primary database)
- **Redis** (optional caching)
- **Consul** (service discovery)
- **Kong Gateway** (API gateway)

---

## Local Development

### 1. Clone Repository
```bash
git clone https://github.com/trademaster/portfolio-service.git
cd portfolio-service
```

### 2. Configure Database
```bash
# Start PostgreSQL with Docker
docker run -d \
  --name portfolio-postgres \
  -e POSTGRES_DB=portfolio \
  -e POSTGRES_USER=portfolio_user \
  -e POSTGRES_PASSWORD=portfolio_pass \
  -p 5432:5432 \
  postgres:16-alpine
```

### 3. Set Environment Variables
```bash
# Create .env file
cat > .env <<EOF
SPRING_PROFILES_ACTIVE=dev
POSTGRES_URL=jdbc:postgresql://localhost:5432/portfolio
POSTGRES_USERNAME=portfolio_user
POSTGRES_PASSWORD=portfolio_pass
JWT_SECRET=your_jwt_secret_here
PORTFOLIO_SERVICE_API_KEY=portfolio-service-secret-key
CONSUL_HOST=localhost
CONSUL_PORT=8500
REDIS_HOST=localhost
REDIS_PORT=6379
EOF
```

### 4. Build Project
```bash
# Build with Gradle
./gradlew clean build

# Skip tests for faster build
./gradlew clean build -x test
```

### 5. Run Application
```bash
# Run with preview features enabled
./gradlew bootRun --args='--enable-preview'

# Or run JAR directly
java --enable-preview -jar build/libs/portfolio-service-2.0.0.jar
```

### 6. Verify Deployment
```bash
# Health check
curl http://localhost:8083/actuator/health

# OpenAPI docs
curl http://localhost:8083/v3/api-docs

# Swagger UI
open http://localhost:8083/swagger-ui.html
```

---

## Docker Deployment

### 1. Build Docker Image
```bash
# Build with Dockerfile
docker build -t trademaster/portfolio-service:2.0.0 .

# Or use docker-compose
docker-compose build portfolio-service
```

### 2. Docker Compose Configuration
```yaml
# docker-compose.yml
version: '3.8'

services:
  portfolio-service:
    image: trademaster/portfolio-service:2.0.0
    container_name: portfolio-service
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      POSTGRES_URL: jdbc:postgresql://postgres:5432/portfolio
      POSTGRES_USERNAME: portfolio_user
      POSTGRES_PASSWORD: portfolio_pass
      JWT_SECRET: ${JWT_SECRET}
      PORTFOLIO_SERVICE_API_KEY: ${PORTFOLIO_SERVICE_API_KEY}
      CONSUL_HOST: consul
      CONSUL_PORT: 8500
    depends_on:
      - postgres
      - consul
      - redis
    networks:
      - trademaster-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:16-alpine
    container_name: portfolio-postgres
    environment:
      POSTGRES_DB: portfolio
      POSTGRES_USER: portfolio_user
      POSTGRES_PASSWORD: portfolio_pass
    ports:
      - "5432:5432"
    volumes:
      - portfolio-postgres-data:/var/lib/postgresql/data
    networks:
      - trademaster-network

  consul:
    image: consul:1.17
    container_name: consul
    ports:
      - "8500:8500"
    networks:
      - trademaster-network

  redis:
    image: redis:7-alpine
    container_name: portfolio-redis
    ports:
      - "6379:6379"
    networks:
      - trademaster-network

networks:
  trademaster-network:
    driver: bridge

volumes:
  portfolio-postgres-data:
```

### 3. Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f portfolio-service

# Stop services
docker-compose down
```

### 4. Verify Container Health
```bash
# Check container status
docker ps | grep portfolio-service

# Check logs
docker logs portfolio-service

# Execute health check
docker exec portfolio-service curl http://localhost:8083/actuator/health
```

---

## Kubernetes Deployment

### 1. Create Namespace
```bash
kubectl create namespace trademaster
```

### 2. Create ConfigMap
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: portfolio-service-config
  namespace: trademaster
data:
  SPRING_PROFILES_ACTIVE: "production"
  CONSUL_HOST: "consul.trademaster.svc.cluster.local"
  CONSUL_PORT: "8500"
  REDIS_HOST: "redis.trademaster.svc.cluster.local"
  REDIS_PORT: "6379"
```

### 3. Create Secrets
```yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: portfolio-service-secrets
  namespace: trademaster
type: Opaque
data:
  postgres-username: <base64-encoded-username>
  postgres-password: <base64-encoded-password>
  jwt-secret: <base64-encoded-jwt-secret>
  api-key: <base64-encoded-api-key>
```

```bash
# Create secrets
kubectl apply -f k8s/secrets.yaml
```

### 4. Create Deployment
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: portfolio-service
  namespace: trademaster
  labels:
    app: portfolio-service
    version: "2.0.0"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: portfolio-service
  template:
    metadata:
      labels:
        app: portfolio-service
        version: "2.0.0"
    spec:
      containers:
      - name: portfolio-service
        image: trademaster/portfolio-service:2.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8083
          name: http
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: portfolio-service-config
              key: SPRING_PROFILES_ACTIVE
        - name: POSTGRES_USERNAME
          valueFrom:
            secretKeyRef:
              name: portfolio-service-secrets
              key: postgres-username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: portfolio-service-secrets
              key: postgres-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: portfolio-service-secrets
              key: jwt-secret
        - name: PORTFOLIO_SERVICE_API_KEY
          valueFrom:
            secretKeyRef:
              name: portfolio-service-secrets
              key: api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8083
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8083
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

### 5. Create Service
```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: portfolio-service
  namespace: trademaster
  labels:
    app: portfolio-service
spec:
  type: ClusterIP
  ports:
  - port: 8083
    targetPort: 8083
    protocol: TCP
    name: http
  selector:
    app: portfolio-service
```

### 6. Deploy to Kubernetes
```bash
# Apply all configurations
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Verify deployment
kubectl get pods -n trademaster -l app=portfolio-service
kubectl get svc -n trademaster -l app=portfolio-service

# Check logs
kubectl logs -n trademaster -l app=portfolio-service -f
```

### 7. Horizontal Pod Autoscaling
```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: portfolio-service-hpa
  namespace: trademaster
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: portfolio-service
  minReplicas: 3
  maxReplicas: 10
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
```

```bash
kubectl apply -f k8s/hpa.yaml
```

---

## Environment Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes | dev | Active Spring profile (dev/docker/production) |
| `POSTGRES_URL` | Yes | - | PostgreSQL JDBC URL |
| `POSTGRES_USERNAME` | Yes | - | Database username |
| `POSTGRES_PASSWORD` | Yes | - | Database password |
| `JWT_SECRET` | Yes | - | JWT signing secret |
| `PORTFOLIO_SERVICE_API_KEY` | Yes | - | Service API key |
| `CONSUL_HOST` | No | localhost | Consul hostname |
| `CONSUL_PORT` | No | 8500 | Consul port |
| `REDIS_HOST` | No | localhost | Redis hostname |
| `REDIS_PORT` | No | 6379 | Redis port |
| `AUTH_SERVICE_URL` | No | http://localhost:8080 | Auth service URL |
| `TRADING_SERVICE_URL` | No | http://localhost:8082 | Trading service URL |
| `BROKER_AUTH_SERVICE_URL` | No | http://localhost:8084 | Broker auth service URL |
| `EVENT_BUS_SERVICE_URL` | No | http://localhost:8085 | Event bus service URL |

### Spring Profiles

#### `dev` Profile
- Local development
- H2 in-memory database option
- Debug logging enabled
- Security relaxed

#### `docker` Profile
- Docker compose deployment
- PostgreSQL required
- Service discovery via Docker network
- INFO logging

#### `production` Profile
- Kubernetes deployment
- High availability configuration
- Strict security
- WARN logging
- Metrics enabled

---

## Database Migration

### Flyway Migration
Database migrations are managed by Flyway and run automatically on startup.

### Migration Files Location
```
src/main/resources/db/migration/
├── V001__Create_portfolio_schema.sql
├── V002__Create_position_table.sql
├── V003__Create_transaction_table.sql
├── V004__Create_risk_limit_table.sql
└── V005__Add_indexes.sql
```

### Manual Migration
```bash
# Run migrations manually
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Clean database (CAUTION: destroys data)
./gradlew flywayClean
```

### Rollback Strategy
```bash
# Create rollback script
./gradlew flywayUndo

# Repair migration history
./gradlew flywayRepair
```

---

## Health Checks

### Endpoints

#### Standard Health Check
```bash
curl http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

#### Kong-Compatible Health Check
```bash
curl http://localhost:8083/api/v2/health
```

#### Internal Health Check (API key required)
```bash
curl -H "X-API-Key: portfolio-service-secret-key" \
  http://localhost:8083/api/internal/v1/portfolio/health
```

### Liveness vs Readiness

#### Liveness Probe
```
GET /actuator/health/liveness
```
- Determines if application should be restarted
- Checks: JVM health, thread deadlocks

#### Readiness Probe
```
GET /actuator/health/readiness
```
- Determines if application can accept traffic
- Checks: Database connection, dependent services

---

## Troubleshooting

### Common Issues

#### 1. Application Won't Start
```bash
# Check Java version
java -version  # Must be Java 24

# Check preview features
java --enable-preview -version

# Check database connection
psql -h localhost -U portfolio_user -d portfolio
```

#### 2. Database Connection Issues
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check database logs
docker logs portfolio-postgres

# Test connection
psql "postgresql://portfolio_user:portfolio_pass@localhost:5432/portfolio"
```

#### 3. Service Discovery Issues
```bash
# Check Consul registration
curl http://localhost:8500/v1/agent/services | jq '.portfolio-service'

# Check Consul health
curl http://localhost:8500/v1/health/service/portfolio-service
```

#### 4. Circuit Breaker Tripped
```bash
# Check circuit breaker status
curl http://localhost:8083/actuator/circuitbreakers

# Check circuit breaker metrics
curl http://localhost:8083/actuator/prometheus | grep circuit
```

#### 5. High Memory Usage
```bash
# Check JVM metrics
curl http://localhost:8083/actuator/metrics/jvm.memory.used

# Generate heap dump
jmap -dump:live,format=b,file=heap.bin <pid>

# Analyze heap dump
jhat heap.bin
```

### Logs

#### View Application Logs
```bash
# Docker
docker logs -f portfolio-service

# Kubernetes
kubectl logs -n trademaster -l app=portfolio-service -f

# Local
tail -f logs/portfolio-service.log
```

#### Log Levels
```bash
# Change log level at runtime
curl -X POST http://localhost:8083/actuator/loggers/com.trademaster.portfolio \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Performance Tuning

### JVM Options
```bash
java --enable-preview \
  -Xms512m \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heapdump.hprof \
  -jar portfolio-service-2.0.0.jar
```

### HikariCP Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Virtual Threads Configuration
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

---

## Security Checklist

- [ ] JWT secret configured (not default)
- [ ] API keys rotated
- [ ] Database credentials encrypted
- [ ] TLS/SSL certificates valid
- [ ] Network policies configured
- [ ] Service-to-service authentication enabled
- [ ] Audit logging enabled
- [ ] Security headers configured

---

## Support & Contact

- **Documentation**: See `docs/` directory
- **Issues**: GitHub Issues
- **Email**: dev@trademaster.com
- **Slack**: #trademaster-support

---

**Last Updated**: 2025-10-07
**Version**: 2.0.0
