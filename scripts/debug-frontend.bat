@echo off
echo Debugging TradeMaster Frontend Setup...
echo.

echo === Docker Status ===
docker --version
echo.

echo === Container Status ===
docker ps -a | findstr trademaster-frontend
echo.

echo === Port Usage ===
netstat -ano | findstr :6006
echo.

echo === Docker Logs (if container exists) ===
docker logs trademaster-frontend-simple 2>nul || echo No container logs found
echo.

echo === Network Status ===
docker network ls | findstr frontend
echo.

pause