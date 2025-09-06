#!/bin/bash
# TradeMaster Stop Script for Linux/macOS

echo "================================"
echo "TradeMaster Stop Script"
echo "================================"

echo "Stopping all TradeMaster services..."

# Stop all services
docker compose down

echo "Checking if you want to remove volumes (this will delete all data!)"
read -p "Remove all data volumes? This will delete all databases and stored data! (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "WARNING: Removing all volumes and data..."
    docker compose down -v
    echo "All data has been removed!"
else
    echo "Data volumes preserved"
fi

echo ""
echo "TradeMaster services stopped!"
echo ""
echo "To restart: run ./scripts/start-trademaster.sh"
echo "To view remaining containers: docker ps"
echo "To remove everything: docker compose down -v --remove-orphans"