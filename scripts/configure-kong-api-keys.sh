#!/bin/bash

# Kong Dynamic API Key Configuration Script
# Sets up Kong consumers, routes, and plugins for TradeMaster services
# This ensures the backend starts in a working state with proper authentication

echo "🔧 Configuring Kong API Gateway for TradeMaster services..."

# Wait for Kong to be ready
echo "⏳ Waiting for Kong to be ready..."
until curl -s http://localhost:8001/status >/dev/null 2>&1; do
    echo "   Waiting for Kong Admin API..."
    sleep 2
done
echo "✅ Kong is ready"

# Function to create consumer if it doesn't exist
create_consumer() {
    local username=$1
    local custom_id=$2
    local tags=$3
    
    if ! curl -s http://localhost:8001/consumers/$username >/dev/null 2>&1; then
        echo "🔧 Creating consumer: $username"
        curl -s -X POST http://localhost:8001/consumers \
            -d "username=$username" \
            -d "custom_id=$custom_id" \
            -d "tags=$tags" >/dev/null
        echo "✅ Consumer $username created"
    else
        echo "ℹ️  Consumer $username already exists"
    fi
}

# Function to create API key for consumer if it doesn't exist
create_api_key() {
    local consumer=$1
    local existing_key=$(curl -s http://localhost:8001/consumers/$consumer/key-auth | jq -r '.data[0].key // empty')
    
    if [ -z "$existing_key" ]; then
        echo "🔧 Creating API key for consumer: $consumer"
        local api_key=$(curl -s -X POST http://localhost:8001/consumers/$consumer/key-auth | jq -r '.key')
        echo "✅ API key created for $consumer: $api_key"
        echo "   Store this key securely!"
    else
        echo "ℹ️  API key already exists for $consumer: $existing_key"
    fi
}

# Function to create service if it doesn't exist
create_service() {
    local name=$1
    local host=$2
    local port=$3
    
    if ! curl -s http://localhost:8001/services/$name >/dev/null 2>&1; then
        echo "🔧 Creating service: $name"
        curl -s -X POST http://localhost:8001/services \
            -d "name=$name" \
            -d "host=$host" \
            -d "port=$port" >/dev/null
        echo "✅ Service $name created"
    else
        echo "ℹ️  Service $name already exists"
    fi
}

# Function to create route if it doesn't exist
create_route() {
    local name=$1
    local service_name=$2
    local path=$3
    local strip_path=$4
    
    if ! curl -s http://localhost:8001/routes | jq -e ".data[] | select(.name == \"$name\")" >/dev/null 2>&1; then
        echo "🔧 Creating route: $name"
        local service_id=$(curl -s http://localhost:8001/services/$service_name | jq -r '.id')
        curl -s -X POST http://localhost:8001/routes \
            -d "name=$name" \
            -d "service.id=$service_id" \
            -d "paths[]=$path" \
            -d "strip_path=$strip_path" >/dev/null
        echo "✅ Route $name created"
    else
        echo "ℹ️  Route $name already exists"
    fi
}

# Function to add plugin to route if it doesn't exist
add_route_plugin() {
    local route_name=$1
    local plugin_name=$2
    local config=$3
    
    local route_id=$(curl -s http://localhost:8001/routes | jq -r ".data[] | select(.name == \"$route_name\") | .id")
    local existing_plugin=$(curl -s http://localhost:8001/routes/$route_id/plugins | jq -e ".data[] | select(.name == \"$plugin_name\")" 2>/dev/null)
    
    if [ "$existing_plugin" = "null" ] || [ -z "$existing_plugin" ]; then
        echo "🔧 Adding $plugin_name plugin to route: $route_name"
        curl -s -X POST http://localhost:8001/routes/$route_id/plugins \
            -H "Content-Type: application/json" \
            -d "$config" >/dev/null
        echo "✅ Plugin $plugin_name added to route $route_name"
    else
        echo "ℹ️  Plugin $plugin_name already exists on route $route_name"
    fi
}

echo ""
echo "📋 Setting up Kong consumers and API keys..."

# Create consumers
create_consumer "trading-service-internal" "svc-$(date +%s)-trading-$(openssl rand -hex 4)" "internal,trading,service-to-service,high-priority"
create_consumer "event-bus-service-internal" "svc-$(date +%s)-events-$(openssl rand -hex 4)" "internal,events,service-to-service"

# Create API keys
create_api_key "trading-service-internal" 
create_api_key "event-bus-service-internal"

echo ""
echo "🌐 Setting up Kong services..."

# Create services
create_service "trading-service" "trading-service" "8083"
create_service "event-bus-service" "event-bus-service" "8081"

echo ""
echo "🛣️  Setting up Kong routes..."

# Create routes for internal APIs
create_route "trading-internal-api" "trading-service" "/api/internal/trading/greeting" "false"
create_route "event-bus-internal" "event-bus-service" "/api/internal/event-bus/greeting" "false" 

echo ""
echo "🔌 Configuring route plugins..."

# Add key-auth plugin to routes
add_route_plugin "trading-internal-api" "key-auth" '{"name": "key-auth", "config": {"key_names": ["X-API-Key"], "hide_credentials": false}}'
add_route_plugin "event-bus-internal" "key-auth" '{"name": "key-auth", "config": {"key_names": ["X-API-Key"], "hide_credentials": false}}'

# Add request-transformer plugins to rewrite URIs
add_route_plugin "trading-internal-api" "request-transformer" '{"name": "request-transformer", "config": {"replace": {"uri": "/api/internal/greeting"}}}'
add_route_plugin "event-bus-internal" "request-transformer" '{"name": "request-transformer", "config": {"replace": {"uri": "/api/internal/greeting"}}}'

echo ""
echo "🧪 Testing Kong configuration..."

# Get API keys for testing
TRADING_API_KEY=$(curl -s http://localhost:8001/consumers/trading-service-internal/key-auth | jq -r '.data[0].key')
EVENTBUS_API_KEY=$(curl -s http://localhost:8001/consumers/event-bus-service-internal/key-auth | jq -r '.data[0].key')

echo "📝 API Keys for testing:"
echo "   Trading Service: $TRADING_API_KEY" 
echo "   Event Bus Service: $EVENTBUS_API_KEY"

# Test endpoints
echo ""
echo "🧪 Running authentication tests..."

echo -n "   Testing trading service: "
if curl -s -H "X-API-Key: $TRADING_API_KEY" http://localhost:8000/api/internal/trading/greeting | grep -q "authenticated.*true"; then
    echo "✅ SUCCESS"
else 
    echo "❌ FAILED"
fi

echo -n "   Testing event-bus service: "
if curl -s -H "X-API-Key: $EVENTBUS_API_KEY" http://localhost:8000/api/internal/event-bus/greeting | grep -q "authenticated.*true"; then
    echo "✅ SUCCESS"  
else
    echo "❌ FAILED"
fi

echo -n "   Testing security (no API key): "
if curl -s http://localhost:8000/api/internal/trading/greeting | grep -q "No API key found"; then
    echo "✅ SUCCESS"
else
    echo "❌ FAILED"
fi

echo ""
echo "🎉 Kong API Gateway configuration completed!"
echo ""
echo "📚 Usage:"
echo "   curl -H \"X-API-Key: $TRADING_API_KEY\" http://localhost:8000/api/internal/trading/greeting"
echo "   curl -H \"X-API-Key: $EVENTBUS_API_KEY\" http://localhost:8000/api/internal/event-bus/greeting"
echo ""
echo "🔐 Security:"
echo "   • Internal API endpoints require valid Kong API keys"
echo "   • Public health endpoints remain accessible without authentication"
echo "   • Direct service calls (bypassing Kong) are rejected"
echo "   • Invalid/missing API keys return appropriate error responses"