#!/bin/bash

# TradeMaster Enhanced Local Development Setup
# Includes Kong API Gateway and Monitoring Stack

set -e

echo "🚀 Setting up TradeMaster Enhanced Local Development Environment"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

print_success "Docker is running ✓"

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose could not be found. Please install docker-compose."
    exit 1
fi

print_success "docker-compose is available ✓"

# Setup Kong database
print_status "Setting up Kong database..."
docker-compose -f docker-compose.enhanced.yml --profile setup up kong-migration --remove-orphans

# Start core services
print_status "Starting core services (PostgreSQL, Redis, Kong)..."
docker-compose -f docker-compose.enhanced.yml up -d postgres redis kong-database kong

# Wait for services to be healthy
print_status "Waiting for services to be healthy..."
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker-compose -f docker-compose.enhanced.yml ps | grep -q "healthy"; then
        print_success "Core services are healthy ✓"
        break
    fi
    sleep 5
    elapsed=$((elapsed + 5))
    echo -n "."
done

if [ $elapsed -ge $timeout ]; then
    print_error "Services did not become healthy within $timeout seconds"
    docker-compose -f docker-compose.enhanced.yml logs
    exit 1
fi

# Start auth service
print_status "Starting TradeMaster Authentication Service..."
docker-compose -f docker-compose.enhanced.yml up -d auth-service

# Start monitoring stack if requested
if [[ "$1" == "--monitoring" || "$1" == "-m" ]]; then
    print_status "Starting monitoring stack (Prometheus + Grafana)..."
    docker-compose -f docker-compose.enhanced.yml --profile monitoring up -d prometheus grafana
fi

# Start development tools if requested
if [[ "$1" == "--dev" || "$1" == "-d" ]]; then
    print_status "Starting development tools (pgAdmin, Redis Commander)..."
    docker-compose -f docker-compose.enhanced.yml --profile development up -d pgadmin redis-commander
fi

# Configure Kong API Gateway
print_status "Configuring Kong API Gateway..."

# Wait for Kong to be ready
sleep 10

# Create Kong service for auth service
curl -i -X POST http://localhost:8001/services/ \
    --data "name=trademaster-auth" \
    --data "url=http://auth-service:8080"

# Create Kong route
curl -i -X POST http://localhost:8001/services/trademaster-auth/routes \
    --data "hosts[]=localhost" \
    --data "paths[]=/api/v1"

# Add JWT plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins \
    --data "name=jwt"

# Add rate limiting plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins \
    --data "name=rate-limiting" \
    --data "config.minute=1000" \
    --data "config.hour=10000"

# Add prometheus plugin
curl -i -X POST http://localhost:8001/services/trademaster-auth/plugins \
    --data "name=prometheus"

print_success "Kong API Gateway configured ✓"

# Show service status
echo
print_status "Service Status:"
docker-compose -f docker-compose.enhanced.yml ps

echo
print_success "🎉 TradeMaster Enhanced Local Development Environment is ready!"

echo
echo "📋 Available Services:"
echo "  • Authentication API:     http://localhost:8000/api/v1/auth"
echo "  • Kong Admin API:         http://localhost:8001"
echo "  • PostgreSQL:             localhost:5432"
echo "  • Redis:                  localhost:6379"

if [[ "$1" == "--monitoring" || "$1" == "-m" ]]; then
    echo "  • Prometheus:             http://localhost:9090"
    echo "  • Grafana:                http://localhost:3001 (admin/admin123)"
fi

if [[ "$1" == "--dev" || "$1" == "-d" ]]; then
    echo "  • pgAdmin:                http://localhost:5050 (admin@trademaster.com/admin123)"
    echo "  • Redis Commander:        http://localhost:8081"
fi

echo
echo "🧪 Test the setup:"
echo "  curl http://localhost:8000/api/v1/auth/health"

echo
echo "🛑 To stop all services:"
echo "  docker-compose -f docker-compose.enhanced.yml down"

echo
echo "📊 To start with monitoring:"
echo "  ./setup-enhanced-local.sh --monitoring"

echo
echo "🔧 To start with development tools:"
echo "  ./setup-enhanced-local.sh --dev"