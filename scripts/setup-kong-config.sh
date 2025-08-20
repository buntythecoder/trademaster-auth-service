#!/bin/bash

# Kong Configuration Setup Script for TradeMaster
# This script configures Kong API Gateway with services and routes

set -e

KONG_ADMIN_URL=${KONG_ADMIN_URL:-"http://localhost:8001"}
CONFIG_FILE=${CONFIG_FILE:-"infrastructure/kong/kong-master-config.yml"}

echo "🔧 Setting up Kong API Gateway configuration..."

# Wait for Kong Admin API to be available
echo "⏳ Waiting for Kong Admin API to be ready..."
until curl -f ${KONG_ADMIN_URL}/status; do
    echo "Kong Admin API not ready yet, waiting..."
    sleep 5
done

echo "✅ Kong Admin API is ready!"

# Check if deck is available for declarative configuration
if command -v deck &> /dev/null; then
    echo "📦 Using Kong Deck for declarative configuration..."
    deck sync --kong-addr ${KONG_ADMIN_URL}
elif [ -f "${CONFIG_FILE}" ]; then
    echo "📝 Applying Kong configuration from ${CONFIG_FILE}..."
    deck sync --kong-addr ${KONG_ADMIN_URL} --state ${CONFIG_FILE}
else
    echo "⚠️  No deck command found, applying configuration manually via REST API..."
    
    # Apply configuration manually via REST API
    
    # 1. Create Auth Service
    echo "🔐 Creating Auth Service..."
    curl -i -X POST ${KONG_ADMIN_URL}/services/ \
        --data "name=trademaster-auth-service" \
        --data "url=http://auth-service:8080/api/v1"
    
    # 2. Create User Profile Service
    echo "👤 Creating User Profile Service..."
    curl -i -X POST ${KONG_ADMIN_URL}/services/ \
        --data "name=trademaster-user-profile-service" \
        --data "url=http://user-profile-service:8082/api/v1"
    
    # 3. Create Routes for Auth Service
    echo "🛣️  Creating routes for Auth Service..."
    
    # Public auth routes
    curl -i -X POST ${KONG_ADMIN_URL}/services/trademaster-auth-service/routes \
        --data "name=auth-public" \
        --data "paths[]=/api/v1/auth/register" \
        --data "paths[]=/api/v1/auth/login" \
        --data "paths[]=/api/v1/auth/forgot-password" \
        --data "paths[]=/api/v1/auth/reset-password" \
        --data "paths[]=/api/v1/auth/verify-email" \
        --data "paths[]=/api/v1/auth/health" \
        --data "methods[]=GET" \
        --data "methods[]=POST" \
        --data "methods[]=OPTIONS"
    
    # Protected auth routes
    curl -i -X POST ${KONG_ADMIN_URL}/services/trademaster-auth-service/routes \
        --data "name=auth-protected" \
        --data "paths[]=/api/v1/auth/refresh" \
        --data "paths[]=/api/v1/auth/logout" \
        --data "paths[]=/api/v1/auth/profile" \
        --data "methods[]=GET" \
        --data "methods[]=POST" \
        --data "methods[]=PUT" \
        --data "methods[]=DELETE" \
        --data "methods[]=OPTIONS"
    
    # 4. Create Routes for User Profile Service
    echo "👤 Creating routes for User Profile Service..."
    
    # Health check routes
    curl -i -X POST ${KONG_ADMIN_URL}/services/trademaster-user-profile-service/routes \
        --data "name=user-profile-health" \
        --data "paths[]=/api/v1/profiles/actuator/health" \
        --data "paths[]=/api/v1/profiles/actuator/info" \
        --data "methods[]=GET" \
        --data "methods[]=OPTIONS"
    
    # Authenticated user profile routes
    curl -i -X POST ${KONG_ADMIN_URL}/services/trademaster-user-profile-service/routes \
        --data "name=user-profile-authenticated" \
        --data "paths[]=/api/v1/profiles/me" \
        --data "paths[]=/api/v1/profiles" \
        --data "methods[]=GET" \
        --data "methods[]=POST" \
        --data "methods[]=PUT" \
        --data "methods[]=PATCH" \
        --data "methods[]=DELETE" \
        --data "methods[]=OPTIONS"
    
    # 5. Add Plugins
    echo "🔌 Adding plugins..."
    
    # Rate limiting for public routes
    curl -i -X POST ${KONG_ADMIN_URL}/routes/auth-public/plugins \
        --data "name=rate-limiting" \
        --data "config.minute=100" \
        --data "config.hour=1000" \
        --data "config.day=5000" \
        --data "config.policy=local"
    
    # JWT authentication for protected routes
    curl -i -X POST ${KONG_ADMIN_URL}/routes/auth-protected/plugins \
        --data "name=jwt"
    
    curl -i -X POST ${KONG_ADMIN_URL}/routes/user-profile-authenticated/plugins \
        --data "name=jwt"
    
    # CORS for all routes
    curl -i -X POST ${KONG_ADMIN_URL}/routes/auth-public/plugins \
        --data "name=cors" \
        --data "config.origins=http://localhost:3000,https://trademaster.com" \
        --data "config.credentials=true"
    
    # 6. Create Consumers
    echo "👥 Creating consumers..."
    
    curl -i -X POST ${KONG_ADMIN_URL}/consumers/ \
        --data "username=trademaster-frontend"
    
    curl -i -X POST ${KONG_ADMIN_URL}/consumers/ \
        --data "username=trademaster-mobile"
    
    # Add JWT secrets to consumers
    curl -i -X POST ${KONG_ADMIN_URL}/consumers/trademaster-frontend/jwt \
        --data "key=trademaster-auth-service" \
        --data "secret=your-256-bit-secret-key-here-change-in-production"
    
    curl -i -X POST ${KONG_ADMIN_URL}/consumers/trademaster-mobile/jwt \
        --data "key=trademaster-auth-service" \
        --data "secret=your-256-bit-secret-key-here-change-in-production"
fi

echo "✅ Kong configuration completed successfully!"
echo "🌐 Kong Proxy: http://localhost:8000"
echo "⚙️  Kong Admin: http://localhost:8001"
echo ""
echo "Available routes:"
echo "  - POST /api/v1/auth/register (Public)"
echo "  - POST /api/v1/auth/login (Public)"
echo "  - POST /api/v1/auth/refresh (Protected)"
echo "  - GET  /api/v1/profiles/me (Protected)"
echo "  - POST /api/v1/profiles (Protected)"