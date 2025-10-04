#!/bin/bash

# TradeMaster Auth Service Docker Deployment Script
# Handles build and deployment with error handling

set -e

echo "🚀 TradeMaster Auth Service Deployment"
echo "========================================"

# Function to handle errors
handle_error() {
    echo "❌ Deployment failed at step: $1"
    echo "Check logs above for details"
    exit 1
}

# Function to cleanup on exit
cleanup() {
    echo "🧹 Cleaning up..."
    docker-compose down --remove-orphans 2>/dev/null || true
}

trap cleanup EXIT

echo "📋 Step 1: Checking prerequisites..."
if ! command -v docker &> /dev/null; then
    handle_error "Docker not found. Please install Docker."
fi

if ! command -v docker-compose &> /dev/null; then
    handle_error "Docker Compose not found. Please install Docker Compose."
fi

echo "✅ Docker and Docker Compose found"

echo "📋 Step 2: Cleaning previous builds..."
docker-compose down --remove-orphans || true
docker system prune -f || true

echo "📋 Step 3: Building services (this may take a few minutes)..."
echo "Building PostgreSQL and Redis containers..."
docker-compose up -d postgres redis || handle_error "Failed to start database services"

echo "⏳ Waiting for database to be ready..."
sleep 10

echo "🔍 Checking database health..."
docker-compose exec postgres pg_isready -U trademaster_user -d trademaster_auth || handle_error "Database not ready"

echo "📋 Step 4: Attempting to build auth-service..."
if docker-compose build auth-service; then
    echo "✅ Auth service built successfully"

    echo "📋 Step 5: Starting all services..."
    docker-compose up -d || handle_error "Failed to start services"

    echo "⏳ Waiting for services to be ready..."
    sleep 30

    echo "🏥 Health Check Results:"
    echo "========================"

    # Check PostgreSQL
    if docker-compose exec postgres pg_isready -U trademaster_user -d trademaster_auth > /dev/null 2>&1; then
        echo "✅ PostgreSQL: Healthy"
    else
        echo "❌ PostgreSQL: Unhealthy"
    fi

    # Check Redis
    if docker-compose exec redis redis-cli ping > /dev/null 2>&1; then
        echo "✅ Redis: Healthy"
    else
        echo "❌ Redis: Unhealthy"
    fi

    # Check Auth Service
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Auth Service: Healthy"
        echo ""
        echo "🎉 DEPLOYMENT SUCCESSFUL!"
        echo "========================"
        echo "🌐 Auth Service: http://localhost:8080"
        echo "🔧 Management: http://localhost:9080/actuator"
        echo "🐘 PostgreSQL: localhost:5432"
        echo "🔴 Redis: localhost:6379"
        echo "🦍 Kong Gateway: http://localhost:8000 (if enabled)"
        echo "🛡️  Kong Admin: http://localhost:8001 (if enabled)"
        echo ""
        echo "📊 Service Status:"
        curl -s http://localhost:8080/actuator/health | jq '.' || echo "Health endpoint not responding"

    else
        echo "⚠️  Auth Service: Starting (may need more time)"
        echo ""
        echo "🔍 Checking logs..."
        docker-compose logs auth-service | tail -20
        echo ""
        echo "💡 Services are running. Check logs with: docker-compose logs auth-service"
    fi

else
    echo "❌ Auth service build failed due to compilation errors"
    echo ""
    echo "🐳 Deploying minimal infrastructure only..."
    echo "=========================================="

    echo "📋 Starting database services only..."
    docker-compose up -d postgres redis || handle_error "Failed to start database services"

    echo "⏳ Waiting for database services..."
    sleep 10

    echo "🏥 Infrastructure Health Check:"
    echo "==============================="

    if docker-compose exec postgres pg_isready -U trademaster_user -d trademaster_auth > /dev/null 2>&1; then
        echo "✅ PostgreSQL: Ready for connections"
    else
        echo "❌ PostgreSQL: Not ready"
    fi

    if docker-compose exec redis redis-cli ping > /dev/null 2>&1; then
        echo "✅ Redis: Ready for connections"
    else
        echo "❌ Redis: Not ready"
    fi

    echo ""
    echo "📋 INFRASTRUCTURE DEPLOYED"
    echo "=========================="
    echo "🐘 PostgreSQL: localhost:5432"
    echo "   Database: trademaster_auth"
    echo "   Username: trademaster_user"
    echo "   Password: trademaster_password"
    echo ""
    echo "🔴 Redis: localhost:6379"
    echo ""
    echo "💡 Next Steps:"
    echo "1. Fix compilation errors in the auth service"
    echo "2. Run: docker-compose build auth-service"
    echo "3. Run: docker-compose up -d auth-service"
    echo ""
    echo "📜 View logs: docker-compose logs [service-name]"
    echo "🛑 Stop all: docker-compose down"
fi

echo ""
echo "🎯 Deployment script completed!"