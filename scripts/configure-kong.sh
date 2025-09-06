#!/bin/bash

# TradeMaster Kong Gateway Configuration Script
# Configures all microservices routing through Kong API Gateway

set -e

KONG_ADMIN_URL=${KONG_ADMIN_URL:-http://localhost:8001}
KONG_PROXY_URL=${KONG_PROXY_URL:-http://localhost:8000}

echo "🚀 Configuring TradeMaster services in Kong Gateway..."
echo "Kong Admin URL: $KONG_ADMIN_URL"
echo "Kong Proxy URL: $KONG_PROXY_URL"

# Wait for Kong to be ready
echo "⏳ Waiting for Kong to be ready..."
for i in {1..30}; do
    if curl -s "$KONG_ADMIN_URL/status" > /dev/null; then
        echo "✅ Kong is ready"
        break
    fi
    echo "   Waiting... ($i/30)"
    sleep 2
done

# Function to create service and route
create_service_and_route() {
    local service_name=$1
    local service_url=$2
    local route_path=$3
    local port=$4
    
    echo "📋 Configuring $service_name..."
    
    # Delete existing service if it exists
    curl -s -X DELETE "$KONG_ADMIN_URL/services/$service_name" || true
    
    # Create service
    curl -s -X POST "$KONG_ADMIN_URL/services" \
        --data "name=$service_name" \
        --data "url=$service_url" \
        --data "connect_timeout=30000" \
        --data "write_timeout=30000" \
        --data "read_timeout=30000" || {
        echo "❌ Failed to create service $service_name"
        return 1
    }
    
    # Create route
    curl -s -X POST "$KONG_ADMIN_URL/services/$service_name/routes" \
        --data "paths[]=$route_path" \
        --data "strip_path=false" \
        --data "preserve_host=false" || {
        echo "❌ Failed to create route for $service_name"
        return 1
    }
    
    echo "✅ Configured $service_name -> $route_path"
}

# Configure TradeMaster Core Services
echo "🔧 Setting up TradeMaster microservices..."

create_service_and_route "agent-orchestration" "http://agent-orchestration-service:8090" "/api/v1/agents" 8090
create_service_and_route "trading-service" "http://trading-service:8081" "/api/v1/trading" 8081  
create_service_and_route "portfolio-service" "http://portfolio-service:8083" "/api/v1/portfolio" 8083
create_service_and_route "market-data-service" "http://market-data-service:8082" "/api/v1/market-data" 8082
create_service_and_route "notification-service" "http://notification-service:8084" "/api/v1/notifications" 8084
create_service_and_route "user-profile-service" "http://user-profile-service:8085" "/api/v1/users" 8085
create_service_and_route "payment-service" "http://payment-service:8086" "/api/v1/payments" 8086
create_service_and_route "subscription-service" "http://subscription-service:8087" "/api/v1/subscriptions" 8087
create_service_and_route "broker-auth-service" "http://broker-auth-service:8088" "/api/v1/broker-auth" 8088

# Configure Infrastructure Services
echo "🏗️ Setting up infrastructure services..."
create_service_and_route "eureka-server" "http://eureka-server:8761" "/eureka" 8761
create_service_and_route "zipkin-tracing" "http://zipkin:9411" "/zipkin" 9411

# Add Kong plugins for enhanced functionality
echo "🔌 Setting up Kong plugins..."

# Rate limiting plugin (global)
curl -s -X POST "$KONG_ADMIN_URL/plugins" \
    --data "name=rate-limiting" \
    --data "config.minute=1000" \
    --data "config.hour=10000" \
    --data "config.policy=local" || echo "⚠️ Rate limiting plugin already exists"

# CORS plugin (global) 
curl -s -X POST "$KONG_ADMIN_URL/plugins" \
    --data "name=cors" \
    --data "config.origins=http://localhost:3000,http://localhost:5173,https://trademaster.app" \
    --data "config.methods=GET,POST,PUT,DELETE,OPTIONS" \
    --data "config.headers=Content-Type,Authorization,X-Requested-With" \
    --data "config.credentials=true" || echo "⚠️ CORS plugin already exists"

# Request/Response logging for monitoring
curl -s -X POST "$KONG_ADMIN_URL/plugins" \
    --data "name=http-log" \
    --data "config.http_endpoint=http://elasticsearch:9200/kong-logs/_doc" \
    --data "config.method=POST" || echo "⚠️ Logging plugin already exists"

# Prometheus metrics
curl -s -X POST "$KONG_ADMIN_URL/plugins" \
    --data "name=prometheus" || echo "⚠️ Prometheus plugin already exists"

# JWT Authentication for protected routes
echo "🔐 Setting up JWT authentication..."
curl -s -X POST "$KONG_ADMIN_URL/plugins" \
    --data "name=jwt" \
    --data "config.secret_is_base64=false" || echo "⚠️ JWT plugin already exists"

# Health check endpoints (no auth required)
echo "🏥 Setting up health check routes..."
curl -s -X POST "$KONG_ADMIN_URL/services" \
    --data "name=health-checks" \
    --data "url=http://agent-orchestration-service:8090/actuator/health"

curl -s -X POST "$KONG_ADMIN_URL/services/health-checks/routes" \
    --data "paths[]=/health" \
    --data "strip_path=true"

# Display configuration summary
echo ""
echo "🎉 Kong Gateway Configuration Complete!"
echo ""
echo "📊 Service Summary:"
echo "   • Agent Orchestration: $KONG_PROXY_URL/api/v1/agents"
echo "   • Trading Service:     $KONG_PROXY_URL/api/v1/trading"  
echo "   • Portfolio Service:   $KONG_PROXY_URL/api/v1/portfolio"
echo "   • Market Data:         $KONG_PROXY_URL/api/v1/market-data"
echo "   • Notifications:       $KONG_PROXY_URL/api/v1/notifications"
echo "   • User Profiles:       $KONG_PROXY_URL/api/v1/users"
echo "   • Payments:            $KONG_PROXY_URL/api/v1/payments"
echo "   • Subscriptions:       $KONG_PROXY_URL/api/v1/subscriptions"
echo "   • Broker Auth:         $KONG_PROXY_URL/api/v1/broker-auth"
echo ""
echo "🏗️ Infrastructure:"
echo "   • Service Discovery:   $KONG_PROXY_URL/eureka"
echo "   • Distributed Tracing: $KONG_PROXY_URL/zipkin"
echo "   • Health Checks:       $KONG_PROXY_URL/health"
echo ""
echo "🔌 Enabled Plugins:"
echo "   • Rate Limiting: 1000/min, 10000/hour"
echo "   • CORS: Configured for frontend origins"  
echo "   • JWT Authentication: Ready for protected routes"
echo "   • Prometheus Metrics: /metrics endpoint"
echo "   • HTTP Logging: ELK stack integration"
echo ""
echo "📋 Kong Admin API: $KONG_ADMIN_URL"
echo "🚀 Kong Proxy: $KONG_PROXY_URL"
echo ""

# Test connectivity
echo "🧪 Testing Kong connectivity..."
if curl -s "$KONG_PROXY_URL/health" > /dev/null; then
    echo "✅ Kong proxy is responding"
else  
    echo "⚠️ Kong proxy not responding - check service status"
fi