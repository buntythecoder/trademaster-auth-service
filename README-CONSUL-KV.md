# TradeMaster Consul KV Configuration Management

## Overview

TradeMaster uses **Consul KV store** for both service discovery and centralized configuration management. This approach eliminates the need for a separate Config Server, simplifying the architecture while providing all the benefits of centralized configuration.

## Why Consul KV Only? ✨

### **Advantages:**
- 🏗️ **Simplified Architecture**: One less service to maintain and monitor
- 🔄 **Real-time Updates**: Instant configuration changes without service restart
- 🌐 **Native Integration**: Tight coupling between service discovery and configuration
- 📊 **Built-in Versioning**: Consul automatically tracks configuration history
- 🏢 **Multi-datacenter**: Native support for distributed configurations
- 🚀 **Reduced Dependencies**: No Spring Cloud Config Server needed
- 💾 **Single Source of Truth**: Service discovery and configuration in one place

### **Configuration Hierarchy:**
```
config/
├── application/data          # Global configuration (all services)
├── trading-service/data      # Service-specific configuration
└── trading-service,prod/data # Environment-specific configuration
```

## Quick Start

### 1. Start Consul and Dependencies
```bash
# Start infrastructure
docker-compose -f docker-compose-consul.yml up -d consul postgres redis

# Wait for Consul to be ready
curl http://localhost:8500/v1/status/leader
```

### 2. Setup Configurations
```bash
# Run configuration setup script
./scripts/consul-config-setup.sh

# Or on Windows
scripts\consul-config-setup.bat
```

### 3. Start Trading Service
```bash
# Start the trading service
docker-compose -f docker-compose-consul.yml up -d trading-service

# Verify service registration
curl http://localhost:8500/v1/agent/services
```

### 4. Verify Configuration Loading
```bash
# Check if configurations are loaded
curl http://localhost:8083/actuator/env

# Check service health
curl http://localhost:8083/api/v2/health
```

## Configuration Management

### Using the Configuration Manager Script
```bash
# List all configurations
./scripts/consul-config-manager.sh list

# Get specific configuration
./scripts/consul-config-manager.sh get application/data

# Set configuration
./scripts/consul-config-manager.sh set trading-service/timeout "30s"

# Delete configuration
./scripts/consul-config-manager.sh delete old-config/data

# Backup configurations
./scripts/consul-config-manager.sh backup my-backup.json

# Restore from backup
./scripts/consul-config-manager.sh restore my-backup.json

# Watch for changes
./scripts/consul-config-manager.sh watch trading-service/

# Validate YAML
./scripts/consul-config-manager.sh validate trading-service/data
```

### Manual Configuration via Consul UI
1. Open Consul UI: http://localhost:8500/ui
2. Navigate to Key/Value tab
3. Browse to `config/` prefix
4. Edit configurations directly in the UI

### Manual Configuration via API
```bash
# Set configuration
curl -X PUT http://localhost:8500/v1/kv/config/trading-service/data \
  -d 'server:
  port: 8083
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trademaster'

# Get configuration
curl http://localhost:8500/v1/kv/config/trading-service/data

# Delete configuration  
curl -X DELETE http://localhost:8500/v1/kv/config/trading-service/data
```

## Configuration Structure

### Global Configuration (`config/application/data`)
```yaml
spring:
  threads:
    virtual:
      enabled: true
  jackson:
    time-zone: UTC
    date-format: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

logging:
  level:
    com.trademaster: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Service-Specific Configuration (`config/trading-service/data`)
```yaml
server:
  port: 8083

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/trademaster_trading}
    username: ${DATABASE_USERNAME:trading_user}
    password: ${DATABASE_PASSWORD:trading_pass}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

resilience4j:
  circuitbreaker:
    instances:
      brokerApi:
        sliding-window-size: 10
        failure-rate-threshold: 50

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: ${JWT_EXPIRATION:86400}

trading:
  order:
    max-quantity: ${MAX_ORDER_QUANTITY:10000}
    timeout-seconds: ${ORDER_TIMEOUT:30}
  risk:
    max-exposure-percent: ${MAX_EXPOSURE_PERCENT:80.0}
```

### Environment-Specific Configuration (`config/trading-service,prod/data`)
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 50

  jpa:
    show-sql: false

jwt:
  expiration: ${JWT_EXPIRATION:3600}  # Shorter for production

trading:
  order:
    max-quantity: ${MAX_ORDER_QUANTITY:100000}
  risk:
    max-exposure-percent: ${MAX_EXPOSURE_PERCENT:60.0}  # More conservative

logging:
  level:
    ROOT: WARN
    com.trademaster: INFO
```

## Real-time Configuration Updates

### Automatic Refresh
The trading service automatically watches for configuration changes in Consul and applies them without restart:

```yaml
spring:
  cloud:
    consul:
      config:
        watch:
          enabled: true
          delay: 1000  # Check every second
```

### Manual Refresh
```bash
# Trigger configuration refresh
curl -X POST http://localhost:8083/actuator/refresh
```

### Configuration Change Events
Monitor configuration changes in application logs:
```
2024-01-15 10:30:45.123 [pool-1-thread-1] INFO  o.s.c.c.c.ConfigWatch - Updating configuration for trading-service
2024-01-15 10:30:45.124 [pool-1-thread-1] INFO  o.s.c.e.RefreshEventListener - Refresh keys changed: [trading.order.max-quantity]
```

## Monitoring and Observability

### Health Checks
```bash
# Service health
curl http://localhost:8083/api/v2/health

# Consul health
curl http://localhost:8500/v1/health/service/trading-service

# Configuration endpoint health
curl http://localhost:8083/actuator/health/consul-config
```

### Metrics
```bash
# Consul KV metrics
curl http://localhost:8500/v1/agent/metrics?format=prometheus | grep consul_kv

# Service configuration metrics
curl http://localhost:8083/actuator/prometheus | grep config
```

### Consul UI Monitoring
- **Service Health**: http://localhost:8500/ui/dc1/services/trading-service
- **Configuration**: http://localhost:8500/ui/dc1/kv/config/
- **Metrics**: http://localhost:8500/ui/dc1/nodes

## Security Best Practices

### Consul ACLs (Production)
```bash
# Enable ACL system
consul acl bootstrap

# Create service-specific token
consul acl token create \
  -description "Trading Service Token" \
  -service-identity "trading-service"

# Create configuration management token
consul acl token create \
  -description "Config Management Token" \
  -policy-name "config-management"
```

### Environment Variables
Always use environment variables for sensitive configuration:
```yaml
jwt:
  secret: ${JWT_SECRET}  # Never hardcode secrets
spring:
  datasource:
    password: ${DATABASE_PASSWORD}
```

### Configuration Encryption
For highly sensitive data, use Consul's encryption features or external secret management.

## Troubleshooting

### Common Issues

1. **Configuration Not Loading**
   ```bash
   # Check Consul connectivity
   curl http://localhost:8500/v1/status/leader
   
   # Verify configuration exists
   ./scripts/consul-config-manager.sh get application/data
   
   # Check service logs
   docker logs trading-service
   ```

2. **Service Not Updating Configuration**
   ```bash
   # Check if watch is enabled
   curl http://localhost:8083/actuator/env | grep consul.config.watch
   
   # Manual refresh trigger
   curl -X POST http://localhost:8083/actuator/refresh
   
   # Check for configuration change events in logs
   docker logs trading-service | grep "configuration"
   ```

3. **Consul KV Access Issues**
   ```bash
   # Test direct access
   curl -v http://localhost:8500/v1/kv/config/application/data
   
   # Check ACL permissions (if enabled)
   curl -H "X-Consul-Token: <token>" http://localhost:8500/v1/kv/config/
   ```

## Backup and Disaster Recovery

### Automated Backup
```bash
# Daily backup cron job
0 2 * * * /path/to/consul-config-manager.sh backup /backups/consul-config-$(date +\%Y\%m\%d).json

# Weekly backup retention
find /backups -name "consul-config-*.json" -mtime +7 -delete
```

### Disaster Recovery
```bash
# Restore from backup
./scripts/consul-config-manager.sh restore /backups/consul-config-20240115.json

# Verify restoration
./scripts/consul-config-manager.sh list
```

## Migration from Config Server

If migrating from Spring Cloud Config Server:
1. Export existing configurations to YAML files
2. Use `consul-config-setup.sh` to populate Consul KV
3. Update `bootstrap.yml` to remove config server references  
4. Remove config server dependencies from `build.gradle`
5. Test service startup and configuration loading

## Performance Optimization

- **Consul Agent**: Run Consul agent on each node for better performance
- **Configuration Caching**: Consul client caches configurations locally
- **Watch Optimization**: Tune watch delay based on change frequency
- **Network Optimization**: Use Consul's built-in compression for large configurations

This simplified architecture provides enterprise-grade configuration management with the reliability and features of Consul KV store.