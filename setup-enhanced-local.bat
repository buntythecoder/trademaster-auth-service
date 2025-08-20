@echo off
REM TradeMaster Enhanced Local Development Setup (Windows)
REM Includes Kong API Gateway and Monitoring Stack

echo 🚀 Setting up TradeMaster Enhanced Local Development Environment

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running. Please start Docker Desktop and try again.
    exit /b 1
)

echo [SUCCESS] Docker is running ✓

REM Check if docker-compose is available
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker-compose could not be found. Please install docker-compose.
    exit /b 1
)

echo [SUCCESS] docker-compose is available ✓

REM Setup Kong database
echo [INFO] Setting up Kong database...
docker-compose -f docker-compose.enhanced.yml --profile setup up kong-migration --remove-orphans

REM Start core services
echo [INFO] Starting core services (PostgreSQL, Redis, Kong)...
docker-compose -f docker-compose.enhanced.yml up -d postgres redis kong-database kong

REM Wait for services to be healthy
echo [INFO] Waiting for services to be healthy...
timeout /t 30 /nobreak >nul

REM Start auth service
echo [INFO] Starting TradeMaster Authentication Service...
docker-compose -f docker-compose.enhanced.yml up -d auth-service

REM Start monitoring stack if requested
if "%1"=="--monitoring" goto monitoring
if "%1"=="-m" goto monitoring
goto configure_kong

:monitoring
echo [INFO] Starting monitoring stack (Prometheus + Grafana)...
docker-compose -f docker-compose.enhanced.yml --profile monitoring up -d prometheus grafana

:configure_kong
REM Configure Kong API Gateway
echo [INFO] Configuring Kong API Gateway...

REM Wait for Kong to be ready
timeout /t 15 /nobreak >nul

REM Create Kong service for auth service
curl -i -X POST http://localhost:8001/services/ --data "name=trademaster-auth" --data "url=http://auth-service:8080"

REM Create Kong route
curl -i -X POST http://localhost:8001/services/trademaster-auth/routes --data "hosts[]=localhost" --data "paths[]=/api/v1"

REM Add JWT plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins --data "name=jwt"

REM Add rate limiting plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins --data "name=rate-limiting" --data "config.minute=1000" --data "config.hour=10000"

REM Add prometheus plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins --data "name=prometheus"

echo [SUCCESS] Kong API Gateway configured ✓

REM Show service status
echo.
echo [INFO] Service Status:
docker-compose -f docker-compose.enhanced.yml ps

echo.
echo [SUCCESS] 🎉 TradeMaster Enhanced Local Development Environment is ready!

echo.
echo 📋 Available Services:
echo   • Authentication API:     http://localhost:8000/api/v1/auth
echo   • Kong Admin API:         http://localhost:8001
echo   • PostgreSQL:             localhost:5432
echo   • Redis:                  localhost:6379

if "%1"=="--monitoring" (
    echo   • Prometheus:             http://localhost:9090
    echo   • Grafana:                http://localhost:3001 ^(admin/admin123^)
)

echo.
echo 🧪 Test the setup:
echo   curl http://localhost:8000/api/v1/auth/health

echo.
echo 🛑 To stop all services:
echo   docker-compose -f docker-compose.enhanced.yml down

echo.
echo 📊 To start with monitoring:
echo   setup-enhanced-local.bat --monitoring