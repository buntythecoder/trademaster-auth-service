#!/bin/bash

# TradeMaster Consul Configuration Setup Script
# This script populates Consul KV store with application configurations

CONSUL_HOST=${CONSUL_HOST:-localhost}
CONSUL_PORT=${CONSUL_PORT:-8500}
CONSUL_URL="http://$CONSUL_HOST:$CONSUL_PORT"

echo "Setting up TradeMaster configurations in Consul KV..."
echo "Consul URL: $CONSUL_URL"

# Function to put configuration to Consul KV
put_config() {
    local key=$1
    local value=$2
    echo "Setting key: $key"
    curl -s -X PUT "$CONSUL_URL/v1/kv/$key" -d "$value" > /dev/null
    if [ $? -eq 0 ]; then
        echo "✓ Successfully set $key"
    else
        echo "✗ Failed to set $key"
    fi
}

# Function to put YAML configuration from file
put_yaml_config() {
    local key=$1
    local file=$2
    if [ -f "$file" ]; then
        echo "Setting YAML key: $key from file: $file"
        curl -s -X PUT "$CONSUL_URL/v1/kv/$key" --data-binary "@$file" > /dev/null
        if [ $? -eq 0 ]; then
            echo "✓ Successfully set $key from $file"
        else
            echo "✗ Failed to set $key from $file"
        fi
    else
        echo "✗ File not found: $file"
    fi
}

# Wait for Consul to be ready
echo "Waiting for Consul to be ready..."
until curl -s "$CONSUL_URL/v1/status/leader" > /dev/null; do
    echo "Waiting for Consul..."
    sleep 2
done
echo "✓ Consul is ready"

# 1. Application-wide configuration (shared across all services)
echo "Setting up application-wide configurations..."

put_config "config/application/data" "# TradeMaster Shared Configuration
spring:
  threads:
    virtual:
      enabled: true
  jackson:
    time-zone: UTC
    date-format: \"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\"
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false

# Logging Configuration (Shared)
logging:
  level:
    com.trademaster: INFO
    org.springframework.security: INFO
    org.springframework.cloud.consul: INFO
  pattern:
    console: \"%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n\"
    file: \"%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n\"

# Actuator Configuration (Shared)
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
        step: 60s"

# 2. Trading Service specific configuration
echo "Setting up trading-service configurations..."

put_config "config/trading-service/data" "# Trading Service Configuration
server:
  port: 8083

# Database Configuration
spring:
  datasource:
    url: \${DATABASE_URL:jdbc:postgresql://localhost:5432/trademaster_trading}
    username: \${DATABASE_USERNAME:trading_user}
    password: \${DATABASE_PASSWORD:trading_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
    database-platform: org.hibernate.dialect.PostgreSQLDialect

  # Redis Configuration
  redis:
    host: \${REDIS_HOST:localhost}
    port: \${REDIS_PORT:6379}
    password: \${REDIS_PASSWORD:}
    database: 0
    jedis:
      pool:
        max-active: 20
        max-idle: 8
        min-idle: 2

# Resilience4j Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    instances:
      brokerApi:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30000
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
      database:
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 60
        wait-duration-in-open-state: 60000

# JWT Configuration
jwt:
  secret: \${JWT_SECRET:your-secret-key-change-in-production}
  expiration: \${JWT_EXPIRATION:86400}
  refresh-expiration: \${JWT_REFRESH_EXPIRATION:604800}

# Trading Service Properties
trading:
  order:
    max-quantity: \${MAX_ORDER_QUANTITY:10000}
    max-value: \${MAX_ORDER_VALUE:1000000.00}
    timeout-seconds: \${ORDER_TIMEOUT:30}
  risk:
    max-exposure-percent: \${MAX_EXPOSURE_PERCENT:80.0}
    max-daily-loss: \${MAX_DAILY_LOSS:50000.00}

# OpenAPI Configuration
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method"

# 3. Trading Service production profile configuration
echo "Setting up trading-service production configurations..."

put_config "config/trading-service,prod/data" "# Trading Service Production Configuration
spring:
  datasource:
    url: \${DATABASE_URL}
    username: \${DATABASE_USERNAME}
    password: \${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  redis:
    host: \${REDIS_HOST}
    port: \${REDIS_PORT}
    password: \${REDIS_PASSWORD}

# Production JWT settings
jwt:
  secret: \${JWT_SECRET}
  expiration: \${JWT_EXPIRATION:3600}

# Production trading limits
trading:
  order:
    max-quantity: \${MAX_ORDER_QUANTITY:100000}
    max-value: \${MAX_ORDER_VALUE:10000000.00}
  risk:
    max-exposure-percent: \${MAX_EXPOSURE_PERCENT:60.0}
    max-daily-loss: \${MAX_DAILY_LOSS:100000.00}

# Production logging
logging:
  level:
    ROOT: WARN
    com.trademaster: INFO
  file:
    name: /app/logs/trading-service.log"

# 4. Development specific configuration
echo "Setting up development configurations..."

put_config "config/trading-service,dev/data" "# Trading Service Development Configuration
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

# Development JWT settings (longer expiration for testing)
jwt:
  expiration: 86400

# Development trading limits (lower for testing)
trading:
  order:
    max-quantity: 1000
    max-value: 100000.00

# Development logging
logging:
  level:
    ROOT: INFO
    com.trademaster: DEBUG
    org.springframework.cloud.consul: DEBUG
    org.hibernate.SQL: DEBUG"

echo ""
echo "🎉 Configuration setup complete!"
echo ""
echo "To verify configurations:"
echo "  curl $CONSUL_URL/v1/kv/config/application/data"
echo "  curl $CONSUL_URL/v1/kv/config/trading-service/data"
echo "  curl $CONSUL_URL/v1/kv/config/trading-service,prod/data"
echo ""
echo "Consul UI: $CONSUL_URL/ui/dc1/kv/config/"