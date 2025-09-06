@echo off
REM TradeMaster Kong Gateway Configuration Script for Windows
REM Configures all microservices routing through Kong API Gateway

setlocal enabledelayedexpansion

set KONG_ADMIN_URL=http://localhost:8001
set KONG_PROXY_URL=http://localhost:8000

echo 🚀 Configuring TradeMaster services in Kong Gateway...
echo Kong Admin URL: %KONG_ADMIN_URL%
echo Kong Proxy URL: %KONG_PROXY_URL%

REM Wait for Kong to be ready
echo ⏳ Waiting for Kong to be ready...
for /L %%i in (1,1,30) do (
    curl -s "%KONG_ADMIN_URL%/status" >nul 2>&1
    if !errorlevel! equ 0 (
        echo ✅ Kong is ready
        goto :configure
    )
    echo    Waiting... (%%i/30)
    timeout /t 2 >nul
)

echo ❌ Kong did not become ready within 60 seconds
exit /b 1

:configure
echo 📋 Configuring TradeMaster services...

REM Agent Orchestration Service
curl -s -X POST "%KONG_ADMIN_URL%/services" --data "name=agent-orchestration" --data "url=http://agent-orchestration-service:8090"
curl -s -X POST "%KONG_ADMIN_URL%/services/agent-orchestration/routes" --data "paths[]=/api/v1/agents" --data "strip_path=false"

REM Trading Service
curl -s -X POST "%KONG_ADMIN_URL%/services" --data "name=trading-service" --data "url=http://trading-service:8081"
curl -s -X POST "%KONG_ADMIN_URL%/services/trading-service/routes" --data "paths[]=/api/v1/trading" --data "strip_path=false"

REM Portfolio Service
curl -s -X POST "%KONG_ADMIN_URL%/services" --data "name=portfolio-service" --data "url=http://portfolio-service:8083"
curl -s -X POST "%KONG_ADMIN_URL%/services/portfolio-service/routes" --data "paths[]=/api/v1/portfolio" --data "strip_path=false"

REM Market Data Service
curl -s -X POST "%KONG_ADMIN_URL%/services" --data "name=market-data-service" --data "url=http://market-data-service:8082"
curl -s -X POST "%KONG_ADMIN_URL%/services/market-data-service/routes" --data "paths[]=/api/v1/market-data" --data "strip_path=false"

REM Notification Service
curl -s -X POST "%KONG_ADMIN_URL%/services" --data "name=notification-service" --data "url=http://notification-service:8084"
curl -s -X POST "%KONG_ADMIN_URL%/services/notification-service/routes" --data "paths[]=/api/v1/notifications" --data "strip_path=false"

REM Add Kong plugins
echo 🔌 Setting up Kong plugins...

REM Rate limiting
curl -s -X POST "%KONG_ADMIN_URL%/plugins" --data "name=rate-limiting" --data "config.minute=1000" --data "config.hour=10000"

REM CORS
curl -s -X POST "%KONG_ADMIN_URL%/plugins" --data "name=cors" --data "config.origins=http://localhost:3000,http://localhost:5173" --data "config.methods=GET,POST,PUT,DELETE,OPTIONS"

REM Prometheus metrics
curl -s -X POST "%KONG_ADMIN_URL%/plugins" --data "name=prometheus"

echo.
echo 🎉 Kong Gateway Configuration Complete!
echo.
echo 📊 Service Routes:
echo    • Agent Orchestration: %KONG_PROXY_URL%/api/v1/agents
echo    • Trading Service:     %KONG_PROXY_URL%/api/v1/trading
echo    • Portfolio Service:   %KONG_PROXY_URL%/api/v1/portfolio
echo    • Market Data:         %KONG_PROXY_URL%/api/v1/market-data
echo    • Notifications:       %KONG_PROXY_URL%/api/v1/notifications
echo.
echo 📋 Kong Admin: %KONG_ADMIN_URL%
echo 🚀 Kong Proxy: %KONG_PROXY_URL%

pause