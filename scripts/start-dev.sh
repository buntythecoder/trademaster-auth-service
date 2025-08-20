#!/bin/bash

# TradeMaster Development Environment Startup Script

echo "🚀 Starting TradeMaster Authentication Service - Development Environment"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    echo "❌ docker-compose is not installed or not in PATH."
    exit 1
fi

# Clean up any existing containers (optional)
read -p "🧹 Do you want to clean up existing containers? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 Cleaning up existing containers..."
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml down -v
    docker system prune -f
fi

# Build and start services
echo "🏗️  Building and starting services..."
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🔍 Checking service health..."

# Check PostgreSQL
if docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T postgres pg_isready -U trademaster_user -d trademaster_auth; then
    echo "✅ PostgreSQL is ready"
else
    echo "❌ PostgreSQL is not ready"
fi

# Check Redis
if docker-compose -f docker-compose.yml -f docker-compose.dev.yml exec -T redis redis-cli --raw incr ping; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
fi

# Check Auth Service
if curl -f http://localhost:8080/api/v1/auth/health > /dev/null 2>&1; then
    echo "✅ Auth Service is ready"
else
    echo "⏳ Auth Service is starting up..."
    sleep 20
    if curl -f http://localhost:8080/api/v1/auth/health > /dev/null 2>&1; then
        echo "✅ Auth Service is ready"
    else
        echo "❌ Auth Service failed to start"
        echo "📋 Checking logs..."
        docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs auth-service
    fi
fi

echo ""
echo "🎉 TradeMaster Development Environment is running!"
echo ""
echo "📋 Service URLs:"
echo "   🔐 Auth Service API: http://localhost:8080/api/v1/auth"
echo "   📖 API Documentation: http://localhost:8080/swagger-ui.html"
echo "   💊 Health Check: http://localhost:8080/api/v1/auth/health"
echo "   📊 Actuator: http://localhost:8080/actuator"
echo ""
echo "🔧 Development Tools:"
echo "   🐘 PgAdmin: http://localhost:5050 (admin@trademaster.com / admin123)"
echo "   📦 Redis Commander: http://localhost:8081"
echo ""
echo "🎯 Database:"
echo "   Host: localhost:5432"
echo "   Database: trademaster_auth"
echo "   Username: trademaster_user"
echo "   Password: trademaster_password"
echo ""
echo "📝 To view logs: docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f [service-name]"
echo "🛑 To stop: docker-compose -f docker-compose.yml -f docker-compose.dev.yml down"