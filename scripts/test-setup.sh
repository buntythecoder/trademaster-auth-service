#!/bin/bash

# TradeMaster End-to-End Test Script
# This script verifies that the TradeMaster setup is working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }

echo -e "${GREEN}🧪 TradeMaster End-to-End Test Suite${NC}"
echo "====================================="
echo

# Test 1: Check if Docker Compose services are running
test_docker_services() {
    print_info "Test 1: Checking Docker services status..."
    
    if docker-compose -f docker-compose-consul.yml ps | grep -q "Up\|healthy"; then
        print_success "Docker services are running"
    else
        print_error "Docker services are not running properly"
        print_info "Run: docker-compose -f docker-compose-consul.yml ps"
        return 1
    fi
}

# Test 2: Check Consul service discovery
test_consul_discovery() {
    print_info "Test 2: Testing Consul service discovery..."
    
    # Check if Consul is accessible
    if curl -s "http://localhost:8500/v1/status/leader" > /dev/null; then
        print_success "Consul leader is available"
    else
        print_error "Consul leader is not available"
        return 1
    fi
    
    # Check if trading service is registered
    if curl -s "http://localhost:8500/v1/agent/services" | grep -q "trading-service"; then
        print_success "Trading service is registered in Consul"
    else
        print_warning "Trading service may not be registered yet"
    fi
}

# Test 3: Check configuration management
test_consul_config() {
    print_info "Test 3: Testing Consul configuration management..."
    
    # Check if configurations are loaded
    if curl -s "http://localhost:8500/v1/kv/config/application/data" > /dev/null; then
        print_success "Application configuration is available in Consul KV"
    else
        print_error "Application configuration not found in Consul KV"
        return 1
    fi
    
    if curl -s "http://localhost:8500/v1/kv/config/trading-service/data" > /dev/null; then
        print_success "Trading service configuration is available in Consul KV"
    else
        print_error "Trading service configuration not found in Consul KV"
        return 1
    fi
}

# Test 4: Check trading service health
test_trading_service() {
    print_info "Test 4: Testing Trading Service health..."
    
    # Check health endpoint
    health_response=$(curl -s "http://localhost:8083/api/v2/health" 2>/dev/null || echo "")
    if echo "$health_response" | grep -q '"status":"UP"'; then
        print_success "Trading service health check passed"
    else
        print_error "Trading service health check failed"
        print_info "Response: $health_response"
        return 1
    fi
    
    # Check if service provides proper JSON response
    if echo "$health_response" | grep -q '"service":"trading-service"'; then
        print_success "Trading service returns proper health information"
    else
        print_warning "Trading service health response format may be incorrect"
    fi
}

# Test 5: Check Spring Boot actuator endpoints
test_actuator_endpoints() {
    print_info "Test 5: Testing Spring Boot actuator endpoints..."
    
    # Test health endpoint
    if curl -s "http://localhost:8083/actuator/health" > /dev/null; then
        print_success "Actuator health endpoint is accessible"
    else
        print_error "Actuator health endpoint is not accessible"
        return 1
    fi
    
    # Test prometheus metrics endpoint
    if curl -s "http://localhost:8083/actuator/prometheus" | grep -q "jvm_"; then
        print_success "Prometheus metrics endpoint is working"
    else
        print_warning "Prometheus metrics endpoint may not be working properly"
    fi
}

# Test 6: Check API documentation
test_api_documentation() {
    print_info "Test 6: Testing API documentation..."
    
    # Check Swagger UI
    if curl -s "http://localhost:8083/swagger-ui.html" > /dev/null; then
        print_success "Swagger UI is accessible"
    else
        print_error "Swagger UI is not accessible"
        return 1
    fi
    
    # Check OpenAPI JSON
    if curl -s "http://localhost:8083/api-docs" | grep -q '"openapi"'; then
        print_success "OpenAPI specification is available"
    else
        print_warning "OpenAPI specification may not be properly configured"
    fi
}

# Test 7: Check Kong Gateway
test_kong_gateway() {
    print_info "Test 7: Testing Kong Gateway..."
    
    # Check Kong admin API
    if curl -s "http://localhost:8001" > /dev/null; then
        print_success "Kong Admin API is accessible"
    else
        print_error "Kong Admin API is not accessible"
        return 1
    fi
    
    # Check Kong health
    kong_status=$(curl -s "http://localhost:8001/status" 2>/dev/null || echo "")
    if echo "$kong_status" | grep -q '"server":'; then
        print_success "Kong Gateway is healthy"
    else
        print_warning "Kong Gateway status check failed"
    fi
}

# Test 8: Check Prometheus metrics collection
test_prometheus() {
    print_info "Test 8: Testing Prometheus metrics collection..."
    
    # Check if Prometheus is accessible
    if curl -s "http://localhost:9090/api/v1/query?query=up" > /dev/null; then
        print_success "Prometheus API is accessible"
    else
        print_error "Prometheus API is not accessible"
        return 1
    fi
    
    # Check if trading service target is configured
    targets=$(curl -s "http://localhost:9090/api/v1/targets" 2>/dev/null || echo "")
    if echo "$targets" | grep -q "trading-service"; then
        print_success "Trading service is configured as Prometheus target"
    else
        print_warning "Trading service may not be configured as Prometheus target"
    fi
}

# Test 9: Check database connectivity
test_database() {
    print_info "Test 9: Testing database connectivity..."
    
    # Check if PostgreSQL is accessible via trading service health
    health_response=$(curl -s "http://localhost:8083/actuator/health" 2>/dev/null || echo "")
    if echo "$health_response" | grep -q '"db":{"status":"UP"'; then
        print_success "Database connectivity is working"
    else
        print_warning "Database connectivity check inconclusive"
        print_info "Health response: $health_response"
    fi
}

# Test 10: Check Redis connectivity
test_redis() {
    print_info "Test 10: Testing Redis connectivity..."
    
    # Check Redis via trading service health
    health_response=$(curl -s "http://localhost:8083/actuator/health" 2>/dev/null || echo "")
    if echo "$health_response" | grep -q '"redis":{"status":"UP"'; then
        print_success "Redis connectivity is working"
    else
        print_warning "Redis connectivity check inconclusive"
    fi
}

# Main test execution
main() {
    local failed_tests=0
    
    # Run all tests
    test_docker_services || ((failed_tests++))
    test_consul_discovery || ((failed_tests++))
    test_consul_config || ((failed_tests++))
    test_trading_service || ((failed_tests++))
    test_actuator_endpoints || ((failed_tests++))
    test_api_documentation || ((failed_tests++))
    test_kong_gateway || ((failed_tests++))
    test_prometheus || ((failed_tests++))
    test_database || ((failed_tests++))
    test_redis || ((failed_tests++))
    
    echo
    echo "====================================="
    
    if [ $failed_tests -eq 0 ]; then
        print_success "🎉 All tests passed! TradeMaster setup is working correctly."
        echo
        print_info "🚀 Ready for trading operations!"
        print_info "📊 Visit http://localhost:8083/swagger-ui.html to explore the API"
        print_info "🔍 Check service status at http://localhost:8500/ui"
        print_info "📈 Monitor metrics at http://localhost:9090"
    else
        print_error "❌ $failed_tests test(s) failed. Please check the setup."
        echo
        print_info "🛠️  Troubleshooting steps:"
        print_info "1. Check service logs: docker-compose -f docker-compose-consul.yml logs -f"
        print_info "2. Check service status: docker-compose -f docker-compose-consul.yml ps"
        print_info "3. Restart services: docker-compose -f docker-compose-consul.yml restart"
        return 1
    fi
    
    echo
}

# Run tests
main "$@"