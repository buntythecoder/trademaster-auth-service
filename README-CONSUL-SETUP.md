# TradeMaster Consul & Config Server Setup

## Overview

TradeMaster has been successfully migrated from Eureka to **Consul** for service discovery and now includes a **Spring Cloud Config Server** for centralized configuration management.

## Architecture

### Service Discovery: Consul
- **Consul Server**: `consul:8500`
- **Service Registration**: Automatic registration with health checks
- **Load Balancing**: Client-side load balancing with Spring Cloud LoadBalancer
- **Health Monitoring**: Continuous health monitoring with `/api/v2/health`

### Configuration Management: Spring Cloud Config Server
- **Config Server**: `config-server:8888`
- **Centralized Configuration**: All service configurations managed centrally
- **Environment Profiles**: Support for dev, test, prod environments
- **Dynamic Configuration**: Real-time configuration updates via Consul
- **Security**: Basic authentication for config endpoints

## Components

### 1. Consul Service Discovery
```yaml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        register: true
        health-check-path: /api/v2/health
        health-check-interval: 10s
        tags:
          - trading-service
          - microservice
          - java24
          - virtual-threads
```

### 2. Config Server (Java 21)
- **Port**: 8888 (API), 8889 (Management)
- **Authentication**: admin/config123
- **Configuration Sources**:
  - Git repository (production)
  - Local filesystem (development)
  - Consul KV store
- **Features**:
  - Encrypted configuration support
  - Service discovery integration
  - Health checks and monitoring

### 3. Trading Service (Java 24)
- **Port**: 8083
- **Service Name**: `trading-service`
- **Health Check**: `/api/v2/health`
- **Configuration**: Loaded from Config Server
- **Features**:
  - Virtual Threads enabled
  - Consul service registration
  - Circuit breakers with Resilience4j
  - Kong Gateway compatible

## Quick Start

### 1. Start Infrastructure
```bash
# Start Consul, Config Server, and dependencies
docker-compose -f docker-compose-consul.yml up -d consul postgres redis config-server
```

### 2. Verify Services
```bash
# Check Consul UI
http://localhost:8500/ui

# Check Config Server
curl -u admin:config123 http://localhost:8888/actuator/health

# Check service configurations
curl -u admin:config123 http://localhost:8888/trading-service/consul
```

### 3. Start Trading Service
```bash
# Start trading service
docker-compose -f docker-compose-consul.yml up -d trading-service

# Verify registration in Consul
curl http://localhost:8500/v1/agent/services
```

### 4. API Gateway (Optional)
```bash
# Start Kong Gateway
docker-compose -f docker-compose-consul.yml up -d kong

# Configure Kong with trading service
curl -X POST http://localhost:8001/services \
  --data "name=trading-service" \
  --data "url=http://trading-service:8083"
```

## Configuration Structure

### Shared Configuration (`shared-config/application.yml`)
```yaml
spring:
  threads:
    virtual:
      enabled: true
logging:
  level:
    com.trademaster: INFO
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

### Service-Specific Configuration (`services/trading-service/trading-service.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/trademaster_trading
    username: trading_user
    password: trading_pass
  
trading:
  order:
    max-quantity: 10000
    timeout-seconds: 30
  risk:
    max-exposure-percent: 80.0
```

## Health Checks

### Consul Health Check
- **Endpoint**: `/api/v2/health`
- **Interval**: 10 seconds
- **Timeout**: 5 seconds
- **Critical Timeout**: 30 seconds

### Kong Gateway Integration
- **Service Health**: `/api/v2/health`
- **Gateway Health**: `/gateway/health`
- **Load Balancer**: Automatic failover

## Monitoring

### Prometheus Metrics
- **Config Server**: `:8889/actuator/prometheus`
- **Trading Service**: `:8083/actuator/prometheus`
- **Consul**: `:8500/v1/agent/metrics?format=prometheus`

### Service Discovery
```bash
# List all services
curl http://localhost:8500/v1/agent/services

# Check service health
curl http://localhost:8500/v1/health/service/trading-service

# View service catalog
curl http://localhost:8500/v1/catalog/services
```

## Configuration Management

### Adding New Configuration
1. **Local Development**: Add to `config/services/{service-name}/`
2. **Production**: Commit to Git repository
3. **Dynamic Updates**: Use Consul KV or Git webhook

### Environment-Specific Configuration
```yaml
---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

## Security

### Config Server Authentication
- **Username**: `admin`
- **Password**: `config123` (configurable via environment)
- **Endpoints**: Basic auth for all configuration endpoints
- **Health Checks**: Public access for monitoring

### Consul ACLs (Production)
```bash
# Enable ACL system
consul acl bootstrap

# Create service token
consul acl token create \
  -description "Trading Service Token" \
  -service-identity "trading-service"
```

## Troubleshooting

### Common Issues

1. **Service not registering with Consul**
   ```bash
   # Check Consul connectivity
   curl http://localhost:8500/v1/status/leader
   
   # Verify service logs
   docker logs trading-service
   ```

2. **Configuration not loading**
   ```bash
   # Test config server
   curl -u admin:config123 http://localhost:8888/trading-service/consul
   
   # Check bootstrap configuration
   # Ensure bootstrap.yml has correct config server URI
   ```

3. **Health checks failing**
   ```bash
   # Test health endpoint directly
   curl http://localhost:8083/api/v2/health
   
   # Check Consul health status
   curl http://localhost:8500/v1/health/service/trading-service
   ```

### Logs and Debugging
```bash
# Enable debug logging
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_CONSUL=DEBUG

# View service logs
docker-compose -f docker-compose-consul.yml logs -f trading-service
```

## Migration Notes

### Changes from Eureka
1. **Dependency Changes**: 
   - Removed: `spring-cloud-starter-netflix-eureka-client`
   - Added: `spring-cloud-starter-consul-discovery`, `spring-cloud-starter-consul-config`

2. **Configuration Changes**:
   - Replaced Eureka configuration with Consul
   - Added Config Server integration
   - Updated health check endpoints

3. **Service Registration**:
   - Services now register with Consul automatically
   - Health checks use `/api/v2/health`
   - Service metadata includes tags and custom attributes

## Production Recommendations

1. **Consul Cluster**: Deploy 3-5 node Consul cluster for HA
2. **Config Server HA**: Deploy multiple Config Server instances
3. **Security**: Enable Consul ACLs and encrypt communication
4. **Monitoring**: Integrate with Prometheus/Grafana
5. **Backup**: Regular backup of Consul data and Git repository

## API Documentation

With Kong Gateway and OpenAPI integration:
- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8083/api-docs`
- **Kong Admin**: `http://localhost:8001`
- **Kong Proxy**: `http://localhost:8000`

The setup is now production-ready with enterprise-grade service discovery, centralized configuration, and comprehensive monitoring.