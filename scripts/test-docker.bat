@echo off
REM TradeMaster Docker Validation Script for Windows

echo 🧪 TradeMaster Docker Validation Test
echo ======================================

set PASSED=0
set FAILED=0

echo.
echo 1. 🐳 Docker Environment Tests
echo ------------------------------

REM Test Docker installation
echo Testing Docker installation...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test Docker Compose installation
echo Testing Docker Compose installation...
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test Docker daemon
echo Testing Docker daemon running...
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

echo.
echo 2. 🏗️  Docker Build Tests
echo -------------------------

REM Check if auth service image exists
echo Testing Auth service Docker image exists...
docker images | findstr trademaster-auth >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

echo.
echo 3. 🚀 Docker Compose Validation
echo -------------------------------

echo Starting Docker Compose services...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d >nul 2>&1

echo Waiting for services to initialize...
timeout /t 30 /nobreak >nul

REM Test PostgreSQL container
echo Testing PostgreSQL container running...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps postgres | findstr "Up" >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test Redis container
echo Testing Redis container running...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps redis | findstr "Up" >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test Auth service container
echo Testing Auth service container running...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps auth-service | findstr "Up" >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

echo.
echo 4. 🔍 Service Health Tests
echo --------------------------

echo Waiting for health checks...
timeout /t 15 /nobreak >nul

REM Test PostgreSQL connectivity
echo Testing PostgreSQL database connectivity...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T postgres pg_isready -U trademaster_user -d trademaster_auth >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test Redis connectivity
echo Testing Redis connectivity...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T redis redis-cli --raw incr ping >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

echo.
echo 5. 🌐 HTTP API Tests
echo -------------------

echo Waiting for auth service to fully start...
timeout /t 10 /nobreak >nul

REM Test health endpoint
echo Testing Auth service health endpoint...
curl -f http://localhost:8080/api/v1/auth/health >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

REM Test actuator endpoint
echo Testing Auth service actuator health...
curl -f http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo ❌ FAILED
    set /a FAILED+=1
) else (
    echo ✅ PASSED
    set /a PASSED+=1
)

echo.
echo 6. 🧹 Cleanup
echo -------------

echo Stopping and removing containers...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down >nul 2>&1

echo Testing containers stopped successfully...
docker-compose -f docker-compose.yml -f docker-compose.dev.yml ps | findstr "Up" >nul 2>&1
if errorlevel 1 (
    echo ✅ PASSED
    set /a PASSED+=1
) else (
    echo ❌ FAILED
    set /a FAILED+=1
)

echo.
echo 📊 Test Results Summary
echo =======================
set /a TOTAL=%PASSED%+%FAILED%
echo Total Tests: %TOTAL%
echo ✅ Passed: %PASSED%
echo ❌ Failed: %FAILED%

if %FAILED% EQU 0 (
    echo.
    echo 🎉 All tests passed! TradeMaster Docker setup is working correctly.
    echo.
    echo ✅ You can now start the development environment with:
    echo    scripts\start-dev.bat
    echo.
    echo 📖 For more information, see DOCKER.md
) else (
    echo.
    echo ❌ Some tests failed. Please check the Docker setup.
    echo.
    echo 🔍 Troubleshooting:
    echo    - Check Docker daemon is running
    echo    - Ensure ports 5432, 6379, 8080 are available
    echo    - Review logs: docker-compose logs
    echo.
    echo 📖 For more information, see DOCKER.md
)

pause