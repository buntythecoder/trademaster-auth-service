#!/bin/bash
# Build All TradeMaster Services Script for Linux/macOS

set -e

echo "================================"
echo "Building All TradeMaster Services"
echo "================================"

# Check if Gradle wrapper is available
if [[ ! -f "./gradlew" ]]; then
    echo "ERROR: Gradle wrapper not found"
    echo "Please make sure you're in the TradeMaster root directory"
    exit 1
fi

echo "Building Agent Orchestration Service..."
cd agent-orchestration-service
../gradlew build -x test
cd ..

echo "Building Broker Authentication Service..."
cd broker-auth-service
../gradlew build -x test
cd ..

echo "Building Behavioral AI Service..."
cd behavioral-ai-service
../gradlew build -x test
cd ..

echo "Building ML Infrastructure Platform..."
cd ml-infrastructure-platform
../gradlew build -x test
cd ..

echo "Building Market Data Service..."
cd market-data-service
../gradlew build -x test
cd ..

echo "Building Notification Service..."
cd notification-service
../gradlew build -x test
cd ..

echo "Building Risk Management Service..."
cd risk-management-service
../gradlew build -x test
cd ..

echo "Building Payment Gateway Service..."
cd payment-gateway-service
../gradlew build -x test
cd ..

echo ""
echo "================================"
echo "All Services Built Successfully!"
echo "================================"
echo ""
echo "Next steps:"
echo "1. Run: docker compose build"
echo "2. Run: ./scripts/start-trademaster.sh"
echo ""