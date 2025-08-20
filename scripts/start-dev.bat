@echo off
REM TradeMaster Development Environment Startup Script for Windows

echo 🚀 Starting TradeMaster Authentication Service - Development Environment

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not running. Please start Docker first.
    pause
    exit /b 1
)

REM Check if docker-compose is available
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ docker-compose is not installed or not in PATH.
    pause
    exit /b 1
)

REM Clean up any existing containers (optional)
set /p cleanup="🧹 Do you want to clean up existing containers? (y/N): "
if /i "%cleanup%"=="y" (
    echo 🧹 Cleaning up existing containers...
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml down -v
    docker system prune -f
)

REM Build and start services
echo 🏗️  Building and starting services...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d

REM Wait for services to be healthy
echo ⏳ Waiting for services to be ready...
timeout /t 10 /nobreak >nul

REM Check service health
echo 🔍 Checking service health...

REM Check PostgreSQL
docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T postgres pg_isready -U trademaster_user -d trademaster_auth >nul 2>&1
if errorlevel 1 (
    echo ❌ PostgreSQL is not ready
) else (
    echo ✅ PostgreSQL is ready
)

REM Check Redis
docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T redis redis-cli --raw incr ping >nul 2>&1
if errorlevel 1 (
    echo ❌ Redis is not ready
) else (
    echo ✅ Redis is ready
)

REM Check Auth Service
curl -f http://localhost:8080/api/v1/auth/health >nul 2>&1
if errorlevel 1 (
    echo ⏳ Auth Service is starting up...
    timeout /t 20 /nobreak >nul
    curl -f http://localhost:8080/api/v1/auth/health >nul 2>&1
    if errorlevel 1 (
        echo ❌ Auth Service failed to start
        echo 📋 Checking logs...
        docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs auth-service
    ) else (
        echo ✅ Auth Service is ready
    )
) else (
    echo ✅ Auth Service is ready
)

echo.
echo 🎉 TradeMaster Development Environment is running!
echo.
echo 📋 Service URLs:
echo    🔐 Auth Service API: http://localhost:8080/api/v1/auth
echo    📖 API Documentation: http://localhost:8080/swagger-ui.html
echo    💊 Health Check: http://localhost:8080/api/v1/auth/health
echo    📊 Actuator: http://localhost:8080/actuator
echo.
echo 🔧 Development Tools:
echo    🐘 PgAdmin: http://localhost:5050 ^(admin@trademaster.com / admin123^)
echo    📦 Redis Commander: http://localhost:8081
echo.
echo 🎯 Database:
echo    Host: localhost:5432
echo    Database: trademaster_auth
echo    Username: trademaster_user
echo    Password: trademaster_password
echo.
echo 📝 To view logs: docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f [service-name]
echo 🛑 To stop: docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

pause