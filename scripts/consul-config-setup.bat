@echo off
REM TradeMaster Consul Configuration Setup Script for Windows
REM This script populates Consul KV store with application configurations

setlocal EnableDelayedExpansion

set CONSUL_HOST=%CONSUL_HOST%
if "%CONSUL_HOST%"=="" set CONSUL_HOST=localhost

set CONSUL_PORT=%CONSUL_PORT%
if "%CONSUL_PORT%"=="" set CONSUL_PORT=8500

set CONSUL_URL=http://%CONSUL_HOST%:%CONSUL_PORT%

echo Setting up TradeMaster configurations in Consul KV...
echo Consul URL: %CONSUL_URL%

REM Wait for Consul to be ready
echo Waiting for Consul to be ready...
:wait_loop
curl -s "%CONSUL_URL%/v1/status/leader" >nul 2>&1
if errorlevel 1 (
    echo Waiting for Consul...
    timeout /t 2 >nul
    goto wait_loop
)
echo Consul is ready

REM 1. Application-wide configuration
echo Setting up application-wide configurations...
curl -s -X PUT "%CONSUL_URL%/v1/kv/config/application/data" -d "# TradeMaster Shared Configuration
spring:
  threads:
    virtual:
      enabled: true
  jackson:
    time-zone: UTC
    date-format: 'yyyy-MM-dd''T''HH:mm:ss.SSS''Z'''
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

logging:
  level:
    com.trademaster: INFO
    org.springframework.security: INFO
  pattern:
    console: '%%d{yyyy-MM-dd HH:mm:ss.SSS} [%%thread] %%-5level [%%X{correlationId}] %%logger{36} - %%msg%%n'

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true" >nul

REM 2. Trading Service configuration
echo Setting up trading-service configurations...
curl -s -X PUT "%CONSUL_URL%/v1/kv/config/trading-service/data" -d "server:
  port: 8083

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/trademaster_trading}
    username: ${DATABASE_USERNAME:trading_user}
    password: ${DATABASE_PASSWORD:trading_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect

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
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration: ${JWT_EXPIRATION:86400}

trading:
  order:
    max-quantity: ${MAX_ORDER_QUANTITY:10000}
    timeout-seconds: ${ORDER_TIMEOUT:30}
  risk:
    max-exposure-percent: ${MAX_EXPOSURE_PERCENT:80.0}" >nul

echo.
echo Configuration setup complete!
echo.
echo To verify configurations:
echo   curl %CONSUL_URL%/v1/kv/config/application/data
echo   curl %CONSUL_URL%/v1/kv/config/trading-service/data
echo.
echo Consul UI: %CONSUL_URL%/ui/dc1/kv/config/

pause