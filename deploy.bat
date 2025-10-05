@echo off
REM TradeMaster Auth Service Docker Deployment Script (Windows)
REM Handles build and deployment with error handling

echo 🚀 TradeMaster Auth Service Deployment
echo ========================================

echo 📋 Step 1: Checking prerequisites...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker not found. Please install Docker Desktop.
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose not found. Please install Docker Desktop.
    pause
    exit /b 1
)

echo ✅ Docker and Docker Compose found

echo 📋 Step 2: Cleaning previous builds...
docker-compose down --remove-orphans 2>nul
docker system prune -f >nul 2>&1

echo 📋 Step 3: Building services (this may take a few minutes)...
echo Building PostgreSQL and Redis containers...
docker-compose up -d postgres redis
if errorlevel 1 (
    echo ❌ Failed to start database services
    pause
    exit /b 1
)

echo ⏳ Waiting for database to be ready...
timeout /t 10 /nobreak >nul

echo 🔍 Checking database health...
docker-compose exec postgres pg_isready -U trademaster_user -d trademaster_auth >nul 2>&1
if errorlevel 1 (
    echo ❌ Database not ready, but continuing...
)

echo 📋 Step 4: Attempting to build auth-service...
docker-compose build auth-service
if errorlevel 1 (
    echo ❌ Auth service build failed due to compilation errors
    echo.
    echo 🐳 Deploying minimal infrastructure only...
    echo ===========================================
    echo.
    echo 📋 Database services are running:
    echo 🐘 PostgreSQL: localhost:5432
    echo    Database: trademaster_auth
    echo    Username: trademaster_user
    echo    Password: trademaster_password
    echo.
    echo 🔴 Redis: localhost:6379
    echo.
    echo 💡 Next Steps:
    echo 1. Fix compilation errors in the auth service
    echo 2. Run: docker-compose build auth-service
    echo 3. Run: docker-compose up -d auth-service
    echo.
    echo 📜 View logs: docker-compose logs [service-name]
    echo 🛑 Stop all: docker-compose down
    goto :infrastructure_only
)

echo ✅ Auth service built successfully

echo 📋 Step 5: Starting all services...
docker-compose up -d
if errorlevel 1 (
    echo ❌ Failed to start services
    pause
    exit /b 1
)

echo ⏳ Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo 🏥 Health Check Results:
echo ========================

REM Check Auth Service
curl -s http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Auth Service: Starting (may need more time)
    echo.
    echo 🔍 Checking logs...
    docker-compose logs auth-service
) else (
    echo ✅ Auth Service: Healthy
    echo.
    echo 🎉 DEPLOYMENT SUCCESSFUL!
    echo ========================
    echo 🌐 Auth Service: http://localhost:8080
    echo 🔧 Management: http://localhost:9080/actuator
)

:infrastructure_only
echo 🐘 PostgreSQL: localhost:5432
echo 🔴 Redis: localhost:6379

echo.
echo 🎯 Deployment script completed!
echo.
echo Press any key to continue...
pause >nul