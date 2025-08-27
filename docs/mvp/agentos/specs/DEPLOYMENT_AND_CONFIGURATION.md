# Agent OS Deployment and Configuration Guide

## Overview
Comprehensive deployment and configuration guide for TradeMaster Agent OS MVP. This document covers container deployment, environment configuration, service integration, monitoring setup, and production best practices.

## Deployment Architecture

### Multi-Service Deployment Model
```yaml
version: '3.8'
services:
  # Agent OS Core Services
  agent-orchestrator:
    image: trademaster/agent-orchestrator:1.0.0
    ports:
      - "8090:8090"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BROKERS=kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G

  agent-registry:
    image: trademaster/agent-registry:1.0.0
    ports:
      - "8091:8091"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.0'
          memory: 1G

  mcp-server:
    image: trademaster/mcp-server:1.0.0
    ports:
      - "8092:8092"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=postgres
      - TRADING_SERVICE_URL=http://trading-service:8081
      - MARKET_DATA_SERVICE_URL=http://market-data-service:8082
      - PORTFOLIO_SERVICE_URL=http://portfolio-service:8083
    depends_on:
      - postgres
      - trading-service
      - market-data-service
      - portfolio-service
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.5'
          memory: 1.5G

  # Agent Instances
  market-analysis-agent:
    image: trademaster/market-analysis-agent:1.0.0
    environment:
      - AGENT_TYPE=MARKET_ANALYSIS
      - ORCHESTRATOR_URL=http://agent-orchestrator:8090
      - MCP_SERVER_URL=http://mcp-server:8092
      - REDIS_HOST=redis
    depends_on:
      - agent-orchestrator
      - mcp-server
      - redis
    deploy:
      replicas: 5
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

  portfolio-management-agent:
    image: trademaster/portfolio-management-agent:1.0.0
    environment:
      - AGENT_TYPE=PORTFOLIO_MANAGEMENT
      - ORCHESTRATOR_URL=http://agent-orchestrator:8090
      - MCP_SERVER_URL=http://mcp-server:8092
      - REDIS_HOST=redis
    depends_on:
      - agent-orchestrator
      - mcp-server
      - redis
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '0.8'
          memory: 768M

  trading-execution-agent:
    image: trademaster/trading-execution-agent:1.0.0
    environment:
      - AGENT_TYPE=TRADING_EXECUTION
      - ORCHESTRATOR_URL=http://agent-orchestrator:8090
      - MCP_SERVER_URL=http://mcp-server:8092
      - REDIS_HOST=redis
    depends_on:
      - agent-orchestrator
      - mcp-server
      - redis
    deploy:
      replicas: 4
      resources:
        limits:
          cpus: '1.0'
          memory: 1G

  risk-management-agent:
    image: trademaster/risk-management-agent:1.0.0
    environment:
      - AGENT_TYPE=RISK_MANAGEMENT
      - ORCHESTRATOR_URL=http://agent-orchestrator:8090
      - MCP_SERVER_URL=http://mcp-server:8092
      - REDIS_HOST=redis
    depends_on:
      - agent-orchestrator
      - mcp-server
      - redis
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '0.8'
          memory: 768M

  # Infrastructure Services
  postgres:
    image: postgres:14-alpine
    environment:
      - POSTGRES_DB=trademaster_agentos
      - POSTGRES_USER=${DB_USERNAME}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts/agentos-schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M

volumes:
  postgres_data:
  redis_data:

networks:
  default:
    name: trademaster-agentos
```

## Container Configurations

### Agent Orchestrator Dockerfile
```dockerfile
FROM eclipse-temurin:24-jre-alpine

LABEL maintainer="TradeMaster DevOps <devops@trademaster.com>"
LABEL description="Agent OS Orchestration Engine"
LABEL version="1.0.0"

# Create app user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Install dependencies
RUN apk add --no-cache curl jq

# Copy application
COPY target/agent-orchestrator-*.jar /app/app.jar
COPY docker/orchestrator/entrypoint.sh /app/entrypoint.sh

# Set permissions
RUN chown -R app:app /app && chmod +x /app/entrypoint.sh

# Switch to app user
USER app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

WORKDIR /app

EXPOSE 8090

ENTRYPOINT ["./entrypoint.sh"]
CMD ["java", "--enable-preview", "-jar", "app.jar"]
```

### Agent Instance Dockerfile
```dockerfile
FROM node:18-alpine

LABEL maintainer="TradeMaster DevOps <devops@trademaster.com>"
LABEL description="TradeMaster AI Agent Runtime"
LABEL version="1.0.0"

# Create app user
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app

# Install dependencies
RUN apk add --no-cache curl python3 make g++

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# Copy application
COPY src/ ./src/
COPY config/ ./config/
COPY docker/agent/entrypoint.sh ./entrypoint.sh

# Set permissions
RUN chown -R app:app /app && chmod +x ./entrypoint.sh

# Switch to app user
USER app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${AGENT_PORT:-3000}/health || exit 1

EXPOSE 3000

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "src/index.js"]
```

## Environment Configuration

### Production Environment Variables
```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=production
NODE_ENV=production
LOG_LEVEL=info

# Database Configuration
DB_HOST=postgres-cluster.internal
DB_PORT=5432
DB_NAME=trademaster_agentos
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
DB_MAX_CONNECTIONS=50
DB_CONNECTION_TIMEOUT=30000

# Redis Configuration
REDIS_HOST=redis-cluster.internal
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}
REDIS_MAX_CONNECTIONS=20
REDIS_TIMEOUT=5000

# Kafka Configuration
KAFKA_BROKERS=kafka-cluster-1.internal:9092,kafka-cluster-2.internal:9092,kafka-cluster-3.internal:9092
KAFKA_GROUP_ID=trademaster-agentos
KAFKA_AUTO_OFFSET_RESET=earliest
KAFKA_SESSION_TIMEOUT=30000

# Service URLs
TRADING_SERVICE_URL=http://trading-service.internal:8081
MARKET_DATA_SERVICE_URL=http://market-data-service.internal:8082
PORTFOLIO_SERVICE_URL=http://portfolio-service.internal:8083
AUTH_SERVICE_URL=http://auth-service.internal:8080
NOTIFICATION_SERVICE_URL=http://notification-service.internal:8084

# Security Configuration
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=3600
ENCRYPTION_KEY=${ENCRYPTION_KEY}
SALT_ROUNDS=12

# Agent Configuration
MAX_AGENTS_PER_TYPE=10
AGENT_HEARTBEAT_INTERVAL=30000
AGENT_TIMEOUT=300000
MAX_CONCURRENT_TASKS=100
TASK_QUEUE_SIZE=1000

# MCP Configuration
MCP_SERVER_URL=http://mcp-server.internal:8092
MCP_CONNECTION_TIMEOUT=10000
MCP_MAX_RETRIES=3
MCP_BATCH_SIZE=10

# Monitoring Configuration
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090
GRAFANA_ENABLED=true
JAEGER_ENABLED=true
JAEGER_ENDPOINT=http://jaeger-collector.monitoring:14268/api/traces

# Performance Configuration
THREAD_POOL_SIZE=50
CONNECTION_POOL_SIZE=20
CACHE_TTL=300
BATCH_SIZE=100
MAX_MEMORY_USAGE=1536m
GC_ALGORITHM=G1
```

### Development Environment Variables
```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=development
NODE_ENV=development
LOG_LEVEL=debug

# Database Configuration (Local)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=trademaster_agentos_dev
DB_USERNAME=trademaster_dev
DB_PASSWORD=dev_password
DB_MAX_CONNECTIONS=10

# Redis Configuration (Local)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_MAX_CONNECTIONS=5

# Kafka Configuration (Local)
KAFKA_BROKERS=localhost:9092
KAFKA_GROUP_ID=trademaster-agentos-dev
KAFKA_AUTO_OFFSET_RESET=earliest

# Service URLs (Local)
TRADING_SERVICE_URL=http://localhost:8081
MARKET_DATA_SERVICE_URL=http://localhost:8082
PORTFOLIO_SERVICE_URL=http://localhost:8083
AUTH_SERVICE_URL=http://localhost:8080
NOTIFICATION_SERVICE_URL=http://localhost:8084

# Development Features
HOT_RELOAD=true
DEBUG_MODE=true
MOCK_EXTERNAL_SERVICES=true
ENABLE_AGENT_SIMULATOR=true

# Reduced Resource Limits
MAX_AGENTS_PER_TYPE=3
AGENT_HEARTBEAT_INTERVAL=10000
MAX_CONCURRENT_TASKS=20
TASK_QUEUE_SIZE=100
```

## Configuration Files

### Agent Orchestrator Configuration
```yaml
# application-production.yml
server:
  port: 8090
  shutdown: graceful
  
spring:
  application:
    name: agent-orchestrator
    
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 2
        
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    consumer:
      group-id: ${KAFKA_GROUP_ID}
      auto-offset-reset: ${KAFKA_AUTO_OFFSET_RESET}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.trademaster.agentos.events
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    db:
      enabled: true
    redis:
      enabled: true
    kafka:
      enabled: true

logging:
  level:
    com.trademaster.agentos: info
    org.springframework.kafka: warn
    org.hibernate: warn
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/agent-orchestrator.log
    max-size: 100MB
    max-history: 10

# Agent OS Specific Configuration
agentos:
  orchestration:
    max-concurrent-workflows: 100
    workflow-timeout: 300000
    checkpoint-interval: 30000
    
  agents:
    max-instances-per-type: 10
    heartbeat-interval: 30000
    registration-timeout: 60000
    health-check-interval: 45000
    
  resources:
    cpu-limit: 2.0
    memory-limit: 2048
    network-limit: 1000
    storage-limit: 10240
    
  security:
    jwt-secret: ${JWT_SECRET}
    encryption-key: ${ENCRYPTION_KEY}
    rate-limit: 1000
    
  mcp:
    server-url: ${MCP_SERVER_URL}
    connection-timeout: 10000
    max-retries: 3
    batch-size: 10
```

### Agent Instance Configuration
```json
{
  "agent": {
    "type": "MARKET_ANALYSIS",
    "name": "Market Analysis Agent",
    "version": "1.0.0",
    "capabilities": [
      "technical-analysis",
      "fundamental-analysis",
      "sentiment-analysis",
      "market-screening"
    ],
    "resources": {
      "cpu": 0.5,
      "memory": 512,
      "network": 100
    },
    "communication": {
      "orchestratorUrl": "${ORCHESTRATOR_URL}",
      "mcpServerUrl": "${MCP_SERVER_URL}",
      "heartbeatInterval": 30000,
      "messageTimeout": 10000,
      "maxRetries": 3
    },
    "performance": {
      "maxConcurrentTasks": 5,
      "taskTimeout": 60000,
      "cacheSize": 1000,
      "cacheTtl": 300000
    },
    "logging": {
      "level": "info",
      "format": "json",
      "maxFileSize": "50MB",
      "maxFiles": 5
    }
  },
  "features": {
    "marketAnalysis": {
      "indicators": ["RSI", "MACD", "SMA", "EMA", "BOLLINGER_BANDS"],
      "timeframes": ["1m", "5m", "15m", "1h", "4h", "1d"],
      "maxSymbols": 100,
      "updateInterval": 5000
    },
    "riskManagement": {
      "maxPositionSize": 0.1,
      "stopLossThreshold": 0.02,
      "profitTargetThreshold": 0.05
    }
  }
}
```

## Deployment Scripts

### Production Deployment Script
```bash
#!/bin/bash
set -e

echo "🚀 Starting TradeMaster Agent OS Production Deployment"

# Configuration
ENVIRONMENT=${ENVIRONMENT:-production}
REGISTRY=${DOCKER_REGISTRY:-registry.trademaster.com}
VERSION=${VERSION:-latest}
NAMESPACE=${NAMESPACE:-trademaster-agentos}

# Pre-deployment checks
echo "📋 Running pre-deployment checks..."

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed"
    exit 1
fi

# Check environment variables
required_vars=(
    "DB_USERNAME"
    "DB_PASSWORD"
    "REDIS_PASSWORD"
    "JWT_SECRET"
    "ENCRYPTION_KEY"
)

for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        echo "❌ Required environment variable $var is not set"
        exit 1
    fi
done

# Pull latest images
echo "📦 Pulling latest Docker images..."
docker-compose -f docker-compose.prod.yml pull

# Create network if it doesn't exist
docker network create trademaster-agentos 2>/dev/null || true

# Database migration
echo "🗄️  Running database migrations..."
docker-compose -f docker-compose.prod.yml run --rm agent-orchestrator \
    java -jar app.jar --spring.profiles.active=migration --spring.jpa.hibernate.ddl-auto=update

# Start infrastructure services
echo "🏗️  Starting infrastructure services..."
docker-compose -f docker-compose.prod.yml up -d postgres redis kafka zookeeper

# Wait for infrastructure to be ready
echo "⏳ Waiting for infrastructure services..."
./scripts/wait-for-services.sh

# Start Agent OS services
echo "🤖 Starting Agent OS services..."
docker-compose -f docker-compose.prod.yml up -d \
    agent-orchestrator \
    agent-registry \
    mcp-server

# Wait for Agent OS core services
echo "⏳ Waiting for Agent OS core services..."
./scripts/wait-for-agentos-services.sh

# Start agent instances
echo "🎯 Starting agent instances..."
docker-compose -f docker-compose.prod.yml up -d \
    market-analysis-agent \
    portfolio-management-agent \
    trading-execution-agent \
    risk-management-agent

# Health check
echo "🏥 Running health checks..."
./scripts/health-check.sh

# Start monitoring
if [[ "${ENABLE_MONITORING}" == "true" ]]; then
    echo "📊 Starting monitoring services..."
    docker-compose -f docker-compose.monitoring.yml up -d
fi

echo "✅ TradeMaster Agent OS deployed successfully!"
echo "🌐 Agent Orchestrator: http://localhost:8090"
echo "📊 Metrics: http://localhost:9090 (if monitoring enabled)"
```

### Health Check Script
```bash
#!/bin/bash

# Health check script for Agent OS services
echo "🏥 Running Agent OS health checks..."

# Services to check
services=(
    "http://localhost:8090/actuator/health:Agent Orchestrator"
    "http://localhost:8091/actuator/health:Agent Registry"
    "http://localhost:8092/actuator/health:MCP Server"
)

all_healthy=true

for service in "${services[@]}"; do
    url=$(echo $service | cut -d: -f1-2)
    name=$(echo $service | cut -d: -f3)
    
    echo "Checking $name..."
    
    if curl -f -s "$url" > /dev/null; then
        echo "✅ $name is healthy"
    else
        echo "❌ $name is not responding"
        all_healthy=false
    fi
done

# Check agent registrations
echo "🤖 Checking agent registrations..."
agent_count=$(curl -s http://localhost:8091/api/v1/agents | jq '.length // 0')

if [[ $agent_count -gt 0 ]]; then
    echo "✅ $agent_count agents registered"
else
    echo "⚠️  No agents registered yet"
fi

if $all_healthy; then
    echo "✅ All health checks passed!"
    exit 0
else
    echo "❌ Some health checks failed!"
    exit 1
fi
```

## Kubernetes Deployment (Optional)

### Kubernetes Manifests
```yaml
# kubernetes/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: trademaster-agentos
  labels:
    name: trademaster-agentos
---
# kubernetes/agent-orchestrator-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agent-orchestrator
  namespace: trademaster-agentos
spec:
  replicas: 3
  selector:
    matchLabels:
      app: agent-orchestrator
  template:
    metadata:
      labels:
        app: agent-orchestrator
    spec:
      containers:
      - name: agent-orchestrator
        image: trademaster/agent-orchestrator:1.0.0
        ports:
        - containerPort: 8090
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          value: "postgres-service"
        - name: REDIS_HOST
          value: "redis-service"
        - name: KAFKA_BROKERS
          value: "kafka-service:9092"
        resources:
          limits:
            cpu: 2000m
            memory: 2Gi
          requests:
            cpu: 1000m
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8090
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8090
          initialDelaySeconds: 30
          periodSeconds: 10
---
# kubernetes/agent-orchestrator-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: agent-orchestrator-service
  namespace: trademaster-agentos
spec:
  selector:
    app: agent-orchestrator
  ports:
  - protocol: TCP
    port: 8090
    targetPort: 8090
  type: ClusterIP
```

## Monitoring and Observability

### Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "agent-os-rules.yml"

scrape_configs:
  - job_name: 'agent-orchestrator'
    static_configs:
      - targets: ['agent-orchestrator:8090']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'agent-registry'
    static_configs:
      - targets: ['agent-registry:8091']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'mcp-server'
    static_configs:
      - targets: ['mcp-server:8092']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'agent-instances'
    static_configs:
      - targets: 
        - 'market-analysis-agent:3000'
        - 'portfolio-management-agent:3000'
        - 'trading-execution-agent:3000'
        - 'risk-management-agent:3000'
    metrics_path: '/metrics'
    scrape_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### Grafana Dashboard Configuration
```json
{
  "dashboard": {
    "title": "TradeMaster Agent OS Overview",
    "panels": [
      {
        "title": "Active Agents",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(agent_registry_active_agents)"
          }
        ]
      },
      {
        "title": "Workflow Execution Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(workflow_executions_total[5m])"
          }
        ]
      },
      {
        "title": "Agent CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "avg by (agent_type) (agent_cpu_usage_ratio)"
          }
        ]
      },
      {
        "title": "Task Queue Length",
        "type": "graph",
        "targets": [
          {
            "expr": "task_queue_length"
          }
        ]
      }
    ]
  }
}
```

## Backup and Recovery

### Database Backup Script
```bash
#!/bin/bash

# Agent OS Database backup script
BACKUP_DIR=${BACKUP_DIR:-/backups}
DB_HOST=${DB_HOST:-localhost}
DB_NAME=${DB_NAME:-trademaster_agentos}
DB_USER=${DB_USERNAME}
RETENTION_DAYS=${RETENTION_DAYS:-30}

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/agentos_backup_$TIMESTAMP.sql"

echo "🗄️  Starting Agent OS database backup..."

# Create backup directory
mkdir -p $BACKUP_DIR

# Create backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME > $BACKUP_FILE

if [[ $? -eq 0 ]]; then
    echo "✅ Database backup completed: $BACKUP_FILE"
    
    # Compress backup
    gzip $BACKUP_FILE
    echo "📦 Backup compressed: ${BACKUP_FILE}.gz"
    
    # Clean old backups
    find $BACKUP_DIR -name "agentos_backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
    echo "🧹 Old backups cleaned up"
else
    echo "❌ Database backup failed"
    exit 1
fi
```

## Troubleshooting Guide

### Common Issues and Solutions

#### 1. Agent Registration Failures
```bash
# Check agent registry logs
docker logs agent-registry

# Common causes:
# - Network connectivity issues
# - Redis connection problems
# - Invalid agent configuration

# Solution: Verify network and Redis connectivity
docker exec agent-registry ping redis
```

#### 2. Workflow Execution Timeouts
```bash
# Check orchestrator logs
docker logs agent-orchestrator

# Common causes:
# - Agent overload
# - Database connection issues
# - Resource constraints

# Solution: Scale agents or increase resources
docker-compose up --scale market-analysis-agent=10
```

#### 3. MCP Communication Errors
```bash
# Check MCP server logs
docker logs mcp-server

# Common causes:
# - Service unavailability
# - Authentication issues
# - Message format errors

# Solution: Verify service health and credentials
curl -f http://mcp-server:8092/actuator/health
```

This comprehensive deployment guide provides everything needed to successfully deploy and manage TradeMaster Agent OS in production environments.