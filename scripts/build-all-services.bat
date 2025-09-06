@echo off
REM Build All TradeMaster Services Script for Windows

echo ================================
echo Building All TradeMaster Services
echo ================================

REM Check if Gradle is available
gradlew.bat --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Gradle wrapper not found
    echo Please make sure you're in the TradeMaster root directory
    pause
    exit /b 1
)

echo Building Agent Orchestration Service...
cd agent-orchestration-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Agent Orchestration Service
    pause
    exit /b 1
)
cd ..

echo Building Broker Authentication Service...
cd broker-auth-service  
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Broker Authentication Service
    pause
    exit /b 1
)
cd ..

echo Building Behavioral AI Service...
cd behavioral-ai-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Behavioral AI Service
    pause
    exit /b 1
)
cd ..

echo Building ML Infrastructure Platform...
cd ml-infrastructure-platform
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build ML Infrastructure Platform
    pause
    exit /b 1
)
cd ..

echo Building Market Data Service...
cd market-data-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Market Data Service
    pause
    exit /b 1
)
cd ..

echo Building Notification Service...
cd notification-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Notification Service
    pause
    exit /b 1
)
cd ..

echo Building Risk Management Service...
cd risk-management-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Risk Management Service
    pause
    exit /b 1
)
cd ..

echo Building Payment Gateway Service...
cd payment-gateway-service
call ..\gradlew.bat build -x test
if errorlevel 1 (
    echo ERROR: Failed to build Payment Gateway Service
    pause
    exit /b 1
)
cd ..

echo.
echo ================================
echo All Services Built Successfully!
echo ================================
echo.
echo Next steps:
echo 1. Run: docker compose build
echo 2. Run: .\scripts\start-trademaster.bat
echo.
pause