@echo off
REM TradeMaster Stop Script for Windows

echo ================================
echo TradeMaster Stop Script
echo ================================

echo Stopping all TradeMaster services...

REM Stop all services
docker compose down

echo Checking if you want to remove volumes (this will delete all data!)
set /p remove_volumes="Remove all data volumes? This will delete all databases and stored data! (y/n): "
if /i "%remove_volumes%"=="y" (
    echo WARNING: Removing all volumes and data...
    docker compose down -v
    echo All data has been removed!
) else (
    echo Data volumes preserved
)

echo.
echo TradeMaster services stopped!
echo.
echo To restart: run start-trademaster.bat
echo To view remaining containers: docker ps
echo To remove everything: docker compose down -v --remove-orphans

pause