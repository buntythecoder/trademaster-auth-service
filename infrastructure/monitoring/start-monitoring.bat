@echo off
REM TradeMaster Monitoring Stack Startup Script for Windows
REM Deploys Prometheus, Grafana, AlertManager, and related monitoring services

setlocal enabledelayedexpansion

echo 🚀 Starting TradeMaster Monitoring Stack...

REM Create necessary directories for persistent data
echo 📁 Creating data directories...
if not exist "data\prometheus" mkdir data\prometheus
if not exist "data\grafana" mkdir data\grafana
if not exist "data\alertmanager" mkdir data\alertmanager
if not exist "grafana\dashboards" mkdir grafana\dashboards

REM Ensure main network exists
echo 🌐 Checking network configuration...
docker network ls | find "trademaster-network" >nul
if errorlevel 1 (
    echo Creating trademaster-network...
    docker network create trademaster-network
) else (
    echo Network trademaster-network already exists
)

REM Validate configuration files
echo ✅ Validating configuration files...
if not exist "prometheus.yml" (
    echo ❌ prometheus.yml not found
    exit /b 1
)

if not exist "alertmanager.yml" (
    echo ❌ alertmanager.yml not found
    exit /b 1
)

echo Configuration files validated successfully

REM Check if monitoring stack is already running
echo 🔍 Checking existing services...
for /f %%i in ('docker-compose -f docker-compose-monitoring.yml ps -q') do (
    echo ⚠️ Monitoring services are already running. Stopping them first...
    docker-compose -f docker-compose-monitoring.yml down
    goto :continue
)
:continue

REM Start monitoring stack
echo 🚀 Starting monitoring services...
docker-compose -f docker-compose-monitoring.yml up -d

REM Wait for services to be ready
echo ⏳ Waiting for services to start...
timeout /t 30 >nul

REM Health check function (basic version for Windows)
echo 🏥 Performing health checks...
echo Checking Prometheus health...
curl -s http://localhost:9090/-/healthy >nul && echo ✅ Prometheus is healthy || echo ❌ Prometheus health check failed

echo Checking Grafana health...
curl -s http://localhost:3000/api/health >nul && echo ✅ Grafana is healthy || echo ❌ Grafana health check failed

echo Checking AlertManager health...
curl -s http://localhost:9093/-/healthy >nul && echo ✅ AlertManager is healthy || echo ❌ AlertManager health check failed

echo Checking Node Exporter health...
curl -s http://localhost:9100/metrics >nul && echo ✅ Node Exporter is healthy || echo ❌ Node Exporter health check failed

REM Display service URLs and access information
echo.
echo 🎉 TradeMaster Monitoring Stack Started Successfully!
echo.
echo 📊 Service URLs:
echo    • Grafana:         http://localhost:3000
echo      Login: admin / trademaster2024
echo.
echo    • Prometheus:      http://localhost:9090
echo      Targets: http://localhost:9090/targets
echo      Rules: http://localhost:9090/rules
echo.
echo    • AlertManager:    http://localhost:9093
echo      Status: http://localhost:9093/#/status
echo.
echo    • Node Exporter:   http://localhost:9100/metrics
echo    • cAdvisor:        http://localhost:8080
echo    • Redis Exporter:  http://localhost:9121/metrics
echo    • Postgres Exporter: http://localhost:9187/metrics
echo.
echo 🔧 Next Steps:
echo 1. Import Grafana dashboards from grafana/dashboards/ directory
echo 2. Configure AlertManager SMTP settings in alertmanager.yml
echo 3. Set up Slack webhooks for critical alerts
echo 4. Review and customize alert rules in alert_rules.yml and trading_rules.yml
echo.
echo 📈 Default Dashboards Available:
echo    • TradeMaster Overview
echo    • Trading System Metrics
echo    • Portfolio Analytics
echo    • Infrastructure Monitoring
echo    • Security Dashboard
echo.
echo 🚨 Alert Categories Configured:
echo    • Critical (immediate): Trading, Financial, Security
echo    • Warning (grouped): Performance, Infrastructure, Database
echo.
echo For troubleshooting, check logs with:
echo docker-compose -f docker-compose-monitoring.yml logs [service-name]
echo.

REM Optional: Start main TradeMaster services if they're not running
if "%1"=="--start-main" (
    echo 🔄 Also starting main TradeMaster services...
    cd ..
    if exist "docker-compose.yml" (
        docker-compose up -d
        echo ✅ Main services started
    ) else (
        echo ⚠️ Main docker-compose.yml not found in parent directory
    )
)

echo ✨ Monitoring stack deployment complete!
pause