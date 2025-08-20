#!/bin/bash
echo "Debugging TradeMaster Frontend Setup..."
echo

echo "=== Docker Status ==="
docker --version
echo

echo "=== Container Status ==="
docker ps -a | grep trademaster-frontend
echo

echo "=== Port Usage ==="
lsof -i :6006 2>/dev/null || netstat -tuln | grep :6006
echo

echo "=== Docker Logs (if container exists) ==="
docker logs trademaster-frontend-simple 2>/dev/null || echo "No container logs found"
echo

echo "=== Network Status ==="
docker network ls | grep frontend
echo