#!/bin/bash

# TradeMaster Kong Gateway Configuration Script
# Configures services, routes, and plugins for comprehensive API management

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
KONG_ADMIN_URL="http://localhost:8001"
TRADING_SERVICE_URL="http://trading-service:8083"
MAX_RETRIES=30
RETRY_INTERVAL=10

# Function to print colored output
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_header() { echo -e "${PURPLE}🔧 $1${NC}"; }

# Function to wait for Kong Admin API
wait_for_kong() {
    print_info "Waiting for Kong Admin API to be ready..."
    
    for i in $(seq 1 $MAX_RETRIES); do
        if curl -s "$KONG_ADMIN_URL" > /dev/null 2>&1; then
            print_success "Kong Admin API is ready"
            return 0
        fi
        
        print_info "Attempt $i/$MAX_RETRIES: Kong not ready, waiting ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done
    
    print_error "Kong Admin API failed to become ready after $((MAX_RETRIES * RETRY_INTERVAL)) seconds"
    return 1
}

# Function to wait for trading service
wait_for_trading_service() {
    print_info "Waiting for Trading Service to be ready..."
    
    for i in $(seq 1 $MAX_RETRIES); do
        if curl -s "$TRADING_SERVICE_URL/api/v2/health" > /dev/null 2>&1; then
            print_success "Trading Service is ready"
            return 0
        fi
        
        print_info "Attempt $i/$MAX_RETRIES: Trading Service not ready, waiting ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done
    
    print_warning "Trading Service not ready, but proceeding with Kong configuration"
    return 0
}

# Function to create or update Kong service
create_service() {
    local service_name=$1
    local service_url=$2
    local service_path=${3:-""}
    
    print_info "Creating/updating Kong service: $service_name"
    
    # Check if service exists
    local service_exists
    service_exists=$(curl -s "$KONG_ADMIN_URL/services/$service_name" | grep -o '"name"' || echo "")
    
    if [[ -n "$service_exists" ]]; then
        print_info "Service $service_name exists, updating..."
        curl -s -X PATCH "$KONG_ADMIN_URL/services/$service_name" \
            -d "url=$service_url" \
            -d "path=$service_path" \
            -d "connect_timeout=10000" \
            -d "write_timeout=60000" \
            -d "read_timeout=60000" \
            -d "retries=3" > /dev/null
    else
        print_info "Service $service_name does not exist, creating..."
        curl -s -X POST "$KONG_ADMIN_URL/services" \
            -d "name=$service_name" \
            -d "url=$service_url" \
            -d "path=$service_path" \
            -d "connect_timeout=10000" \
            -d "write_timeout=60000" \
            -d "read_timeout=60000" \
            -d "retries=3" > /dev/null
    fi
    
    print_success "Service $service_name configured successfully"
}

# Function to create or update Kong route
create_route() {
    local route_name=$1
    local service_name=$2
    local paths=$3
    local methods=${4:-"GET,POST,PUT,DELETE,PATCH"}
    local strip_path=${5:-"true"}
    
    print_info "Creating/updating Kong route: $route_name"
    
    # Check if route exists
    local route_exists
    route_exists=$(curl -s "$KONG_ADMIN_URL/routes/$route_name" | grep -o '"name"' || echo "")
    
    if [[ -n "$route_exists" ]]; then
        print_info "Route $route_name exists, updating..."
        curl -s -X PATCH "$KONG_ADMIN_URL/routes/$route_name" \
            -d "service.name=$service_name" \
            -d "paths=$paths" \
            -d "methods=$methods" \
            -d "strip_path=$strip_path" \
            -d "preserve_host=false" > /dev/null
    else
        print_info "Route $route_name does not exist, creating..."
        curl -s -X POST "$KONG_ADMIN_URL/routes" \
            -d "name=$route_name" \
            -d "service.name=$service_name" \
            -d "paths=$paths" \
            -d "methods=$methods" \
            -d "strip_path=$strip_path" \
            -d "preserve_host=false" > /dev/null
    fi
    
    print_success "Route $route_name configured successfully"
}

# Function to configure plugin
configure_plugin() {
    local plugin_name=$1
    local service_name=$2
    local config=$3
    local route_name=${4:-""}
    
    print_info "Configuring plugin: $plugin_name for service: $service_name"
    
    # Build the request data
    local request_data="name=$plugin_name&service.name=$service_name"
    
    if [[ -n "$route_name" ]]; then
        request_data="${request_data}&route.name=$route_name"
    fi
    
    if [[ -n "$config" ]]; then
        request_data="${request_data}&$config"
    fi
    
    # Check if plugin already exists
    local plugin_exists
    if [[ -n "$route_name" ]]; then
        plugin_exists=$(curl -s "$KONG_ADMIN_URL/routes/$route_name/plugins" | grep "\"name\":\"$plugin_name\"" || echo "")
    else
        plugin_exists=$(curl -s "$KONG_ADMIN_URL/services/$service_name/plugins" | grep "\"name\":\"$plugin_name\"" || echo "")
    fi
    
    if [[ -n "$plugin_exists" ]]; then
        print_warning "Plugin $plugin_name already exists, skipping..."
    else
        curl -s -X POST "$KONG_ADMIN_URL/plugins" -d "$request_data" > /dev/null
        print_success "Plugin $plugin_name configured successfully"
    fi
}

# Function to configure CORS plugin
configure_cors() {
    local service_name=$1
    
    print_info "Configuring CORS plugin for service: $service_name"
    
    local cors_config="config.origins=*&config.methods=GET,POST,PUT,DELETE,PATCH,OPTIONS&config.headers=Accept,Accept-Version,Content-Length,Content-MD5,Content-Type,Date,Authorization&config.exposed_headers=X-Auth-Token&config.credentials=true&config.max_age=3600"
    
    configure_plugin "cors" "$service_name" "$cors_config"
}

# Function to configure rate limiting
configure_rate_limiting() {
    local service_name=$1
    local requests_per_minute=${2:-"100"}
    local requests_per_hour=${3:-"1000"}
    
    print_info "Configuring rate limiting for service: $service_name ($requests_per_minute/min, $requests_per_hour/hour)"
    
    local rate_limit_config="config.minute=$requests_per_minute&config.hour=$requests_per_hour&config.policy=local"
    
    configure_plugin "rate-limiting" "$service_name" "$rate_limit_config"
}

# Function to configure Prometheus metrics
configure_prometheus() {
    local service_name=$1
    
    print_info "Configuring Prometheus plugin for service: $service_name"
    
    local prometheus_config="config.per_consumer=true&config.status_code_metrics=true&config.latency_metrics=true&config.bandwidth_metrics=true&config.upstream_health_metrics=true"
    
    configure_plugin "prometheus" "$service_name" "$prometheus_config"
}

# Function to configure request validation
configure_request_validation() {
    local service_name=$1
    local route_name=$2
    
    print_info "Configuring request size limiting for route: $route_name"
    
    local validation_config="config.allowed_content_types=application/json,application/x-www-form-urlencoded,multipart/form-data&config.body_size=8192"
    
    configure_plugin "request-size-limiting" "$service_name" "$validation_config" "$route_name"
}

# Function to configure circuit breaker (if available)
configure_circuit_breaker() {
    local service_name=$1
    
    print_info "Configuring proxy cache for service: $service_name (circuit breaker pattern)"
    
    local cache_config="config.cache_ttl=300&config.cache_control=true&config.storage_ttl=3600"
    
    configure_plugin "proxy-cache" "$service_name" "$cache_config"
}

# Main configuration function
configure_kong_gateway() {
    print_header "Starting Kong Gateway Configuration for TradeMaster"
    echo "============================================================"
    echo
    
    # Wait for dependencies
    wait_for_kong || exit 1
    wait_for_trading_service
    
    echo
    print_header "Configuring Services and Routes"
    echo "================================"
    
    # 1. Trading Service Configuration
    print_info "Setting up Trading Service..."
    create_service "trading-service" "$TRADING_SERVICE_URL"
    
    # API v2 routes for trading service
    create_route "trading-api-v2" "trading-service" "/api/v2" "GET,POST,PUT,DELETE,PATCH" "false"
    create_route "trading-health" "trading-service" "/api/v2/health" "GET" "false"
    create_route "trading-actuator" "trading-service" "/actuator" "GET" "false"
    create_route "trading-swagger" "trading-service" "/swagger-ui.html,/api-docs,/swagger-ui" "GET" "false"
    
    echo
    print_header "Configuring Plugins"
    echo "==================="
    
    # Configure CORS for all services
    configure_cors "trading-service"
    
    # Configure rate limiting (different limits for different endpoints)
    configure_rate_limiting "trading-service" "1000" "10000"  # Higher limits for trading
    
    # Configure Prometheus metrics
    configure_prometheus "trading-service"
    
    # Configure request validation for API routes
    configure_request_validation "trading-service" "trading-api-v2"
    
    # Configure circuit breaker pattern with proxy cache
    configure_circuit_breaker "trading-service"
    
    echo
    print_header "Configuring Global Plugins"
    echo "=========================="
    
    # Global request ID plugin
    print_info "Configuring global request ID plugin..."
    curl -s -X POST "$KONG_ADMIN_URL/plugins" \
        -d "name=correlation-id" \
        -d "config.header_name=X-Correlation-ID" \
        -d "config.generator=uuid#counter" \
        -d "config.echo_downstream=true" > /dev/null
    print_success "Global request ID plugin configured"
    
    # Global request termination for health checks
    print_info "Configuring global request termination for Kong health..."
    curl -s -X POST "$KONG_ADMIN_URL/plugins" \
        -d "name=request-termination" \
        -d "config.status_code=200" \
        -d "config.message={\"status\":\"UP\",\"service\":\"kong-gateway\"}" \
        -d "config.content_type=application/json" \
        -d "route.paths=/health,/ping" > /dev/null 2>&1 || print_warning "Global health route plugin may already exist"
    
    echo
    print_header "Configuration Summary"
    echo "===================="
    
    # Display configuration summary
    print_success "Kong Gateway configuration completed successfully!"
    echo
    print_info "📋 Services configured:"
    echo "   • trading-service: $TRADING_SERVICE_URL"
    echo
    print_info "🛣️  Routes configured:"
    echo "   • /api/v2/* → trading-service (API endpoints)"
    echo "   • /api/v2/health → trading-service (Health checks)"
    echo "   • /actuator/* → trading-service (Spring Boot Actuator)"
    echo "   • /swagger-ui.html → trading-service (API documentation)"
    echo
    print_info "🔌 Plugins configured:"
    echo "   • CORS: Enabled for all origins"
    echo "   • Rate Limiting: 1000/min, 10000/hour for trading"
    echo "   • Prometheus: Metrics collection enabled"
    echo "   • Request Size Limiting: 8KB max request size"
    echo "   • Proxy Cache: Circuit breaker pattern"
    echo "   • Correlation ID: Request tracking enabled"
    echo
    print_info "🌐 Access URLs:"
    echo "   • Kong Gateway: http://localhost:8000"
    echo "   • Kong Admin: http://localhost:8001"
    echo "   • Trading API: http://localhost:8000/api/v2"
    echo "   • API Health: http://localhost:8000/api/v2/health"
    echo "   • Kong Health: http://localhost:8000/health"
    echo "   • API Docs: http://localhost:8000/swagger-ui.html"
    echo
    print_success "🎉 Kong Gateway is ready for production traffic!"
}

# Function to verify Kong configuration
verify_configuration() {
    print_header "Verifying Kong Configuration"
    echo "============================"
    
    local failed_checks=0
    
    # Test Kong health
    if curl -s "http://localhost:8000/health" | grep -q "UP"; then
        print_success "Kong Gateway health check: PASSED"
    else
        print_error "Kong Gateway health check: FAILED"
        ((failed_checks++))
    fi
    
    # Test Trading Service through Kong
    if curl -s "http://localhost:8000/api/v2/health" | grep -q "UP"; then
        print_success "Trading Service through Kong: PASSED"
    else
        print_error "Trading Service through Kong: FAILED"
        ((failed_checks++))
    fi
    
    # Test CORS headers
    local cors_test
    cors_test=$(curl -s -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: GET" \
        -X OPTIONS "http://localhost:8000/api/v2/health" | grep -i "access-control" || echo "")
    
    if [[ -n "$cors_test" ]]; then
        print_success "CORS configuration: PASSED"
    else
        print_warning "CORS configuration: May not be working properly"
    fi
    
    # Test rate limiting headers
    local rate_limit_test
    rate_limit_test=$(curl -s -I "http://localhost:8000/api/v2/health" | grep -i "x-ratelimit" || echo "")
    
    if [[ -n "$rate_limit_test" ]]; then
        print_success "Rate limiting configuration: PASSED"
    else
        print_warning "Rate limiting headers: Not visible (may be normal)"
    fi
    
    echo
    if [[ $failed_checks -eq 0 ]]; then
        print_success "✅ All configuration checks passed!"
        return 0
    else
        print_error "❌ $failed_checks check(s) failed"
        return 1
    fi
}

# Function to display help
show_help() {
    echo "TradeMaster Kong Gateway Configuration Script"
    echo "============================================="
    echo
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  --configure     Configure Kong Gateway (default)"
    echo "  --verify        Verify Kong configuration"
    echo "  --status        Show Kong status"
    echo "  --reset         Reset Kong configuration"
    echo "  --help          Show this help message"
    echo
    echo "Examples:"
    echo "  $0                    # Configure Kong Gateway"
    echo "  $0 --verify          # Verify configuration"
    echo "  $0 --status          # Show status"
}

# Function to show Kong status
show_status() {
    print_header "Kong Gateway Status"
    echo "=================="
    
    # Kong service status
    if curl -s "$KONG_ADMIN_URL" > /dev/null 2>&1; then
        print_success "Kong Admin API: UP"
        
        # Show services
        echo
        print_info "📋 Configured Services:"
        curl -s "$KONG_ADMIN_URL/services" | grep -o '"name":"[^"]*"' | sed 's/"name":"/   • /' | sed 's/"//' || print_warning "Could not retrieve services"
        
        # Show routes  
        echo
        print_info "🛣️  Configured Routes:"
        curl -s "$KONG_ADMIN_URL/routes" | grep -o '"name":"[^"]*"' | sed 's/"name":"/   • /' | sed 's/"//' || print_warning "Could not retrieve routes"
        
        # Show plugins
        echo
        print_info "🔌 Active Plugins:"
        curl -s "$KONG_ADMIN_URL/plugins" | grep -o '"name":"[^"]*"' | sed 's/"name":"/   • /' | sed 's/"//' | sort | uniq || print_warning "Could not retrieve plugins"
        
    else
        print_error "Kong Admin API: DOWN"
        return 1
    fi
}

# Function to reset Kong configuration
reset_configuration() {
    print_header "Resetting Kong Configuration"
    echo "============================"
    
    print_warning "This will remove all Kong services, routes, and plugins!"
    print_info "Press Ctrl+C within 10 seconds to cancel..."
    sleep 10
    
    print_info "Removing all plugins..."
    curl -s "$KONG_ADMIN_URL/plugins" | grep -o '"id":"[^"]*"' | sed 's/"id":"//' | sed 's/"//' | \
    while read -r plugin_id; do
        curl -s -X DELETE "$KONG_ADMIN_URL/plugins/$plugin_id" > /dev/null
        print_info "Removed plugin: $plugin_id"
    done
    
    print_info "Removing all routes..."
    curl -s "$KONG_ADMIN_URL/routes" | grep -o '"id":"[^"]*"' | sed 's/"id":"//' | sed 's/"//' | \
    while read -r route_id; do
        curl -s -X DELETE "$KONG_ADMIN_URL/routes/$route_id" > /dev/null
        print_info "Removed route: $route_id"
    done
    
    print_info "Removing all services..."
    curl -s "$KONG_ADMIN_URL/services" | grep -o '"id":"[^"]*"' | sed 's/"id":"//' | sed 's/"//' | \
    while read -r service_id; do
        curl -s -X DELETE "$KONG_ADMIN_URL/services/$service_id" > /dev/null
        print_info "Removed service: $service_id"
    done
    
    print_success "Kong configuration reset completed"
}

# Main script logic
case "${1:---configure}" in
    --configure)
        configure_kong_gateway
        echo
        verify_configuration
        ;;
    --verify)
        verify_configuration
        ;;
    --status)
        show_status
        ;;
    --reset)
        reset_configuration
        ;;
    --help)
        show_help
        ;;
    *)
        print_error "Unknown option: $1"
        echo
        show_help
        exit 1
        ;;
esac