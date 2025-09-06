@echo off
REM TradeMaster Startup Script for Windows
REM This script starts all TradeMaster services using Docker Compose

echo ================================
echo TradeMaster Startup Script
echo ================================

REM Check if Docker is running
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not installed or not running
    echo Please install Docker Desktop and make sure it's running
    pause
    exit /b 1
)

REM Check if Docker Compose is available
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Compose is not available
    echo Please update Docker Desktop to latest version
    pause
    exit /b 1
)

REM Check if .env file exists
if not exist ".env" (
    echo WARNING: .env file not found!
    echo Copying .env.example to .env...
    copy .env.example .env
    echo.
    echo IMPORTANT: Please edit .env file and set your API keys and secrets
    echo Press any key to continue with default values, or Ctrl+C to exit and configure first
    pause
)

echo Starting TradeMaster infrastructure...
echo.

REM Create required directories
if not exist "logs" mkdir logs
if not exist "logs\agent-orchestration" mkdir logs\agent-orchestration
if not exist "logs\broker-auth" mkdir logs\broker-auth
if not exist "logs\behavioral-ai" mkdir logs\behavioral-ai
if not exist "logs\ml-platform" mkdir logs\ml-platform
if not exist "logs\market-data" mkdir logs\market-data
if not exist "logs\notification" mkdir logs\notification
if not exist "logs\risk-management" mkdir logs\risk-management
if not exist "logs\payment-gateway" mkdir logs\payment-gateway
if not exist "ml-models" mkdir ml-models

echo Step 1: Starting infrastructure services (PostgreSQL, Redis, Kafka, etc.)...
docker compose up -d postgres redis zookeeper kafka elasticsearch minio

echo Waiting for infrastructure services to be healthy...
timeout /t 30 /nobreak >nul

echo Step 2: Starting TradeMaster services...
docker compose up -d agent-orchestration-service broker-auth-service behavioral-ai-service ml-infrastructure-platform market-data-service notification-service risk-management-service payment-gateway-service

echo Step 3: Starting MLflow and Load Balancer...
docker compose up -d mlflow nginx

echo.
echo ================================
echo TradeMaster Services Started!
echo ================================
echo.
echo Service URLs:
echo - Load Balancer: http://localhost
echo - Agent Orchestration: http://localhost:8090/agent-os
echo - Broker Authentication: http://localhost:8087/api/v1
echo - Behavioral AI: http://localhost:8085/behavioral-ai
echo - ML Infrastructure: http://localhost:8088
echo - Market Data: http://localhost:8082
echo - Notifications: http://localhost:8084
echo - Risk Management: http://localhost:8086
echo - Payment Gateway: http://localhost:8089
echo - MLflow: http://localhost:5000
echo - PostgreSQL Admin: http://localhost:5050 ^(development profile^)
echo - Redis Commander: http://localhost:8081 ^(development profile^)
echo.
echo To view logs: docker compose logs -f [service-name]
echo To stop services: docker compose down
echo To restart services: docker compose restart
echo.

REM Optional: Start development tools if in development mode
set /p start_dev_tools="Start development tools (pgAdmin, Redis Commander)? (y/n): "
if /i "%start_dev_tools%"=="y" (
    echo Starting development tools...
    docker compose --profile development up -d
    echo Development tools started!
    echo - pgAdmin: http://localhost:5050 ^(admin@trademaster.com / admin123^)
    echo - Redis Commander: http://localhost:8081
)

echo.
echo TradeMaster is ready for trading!
pause