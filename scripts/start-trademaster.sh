#!/bin/bash

# TradeMaster Complete Stack Startup Script
# This script starts the entire TradeMaster platform in one command

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

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

# Function to check if Docker is running
check_docker() {
    print_info "Checking Docker availability..."
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running or not accessible"
        print_error "Please start Docker Desktop or Docker service and try again"
        exit 1
    fi
    print_success "Docker is available"
}

# Function to check if Docker Compose is available
check_docker_compose() {
    print_info "Checking Docker Compose availability..."
    if ! docker-compose --version > /dev/null 2>&1; then
        print_error "Docker Compose is not available"
        print_error "Please install Docker Compose and try again"
        exit 1
    fi
    print_success "Docker Compose is available"
}

# Function to cleanup previous containers
cleanup_containers() {
    print_info "Cleaning up previous containers..."
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose-consul.yml down -v --remove-orphans > /dev/null 2>&1 || true
    print_success "Previous containers cleaned up"
}

# Function to start the stack
start_stack() {
    print_info "Starting TradeMaster complete stack..."
    print_info "This may take 3-5 minutes for initial setup..."
    cd "$PROJECT_ROOT"
    
    # Start with build to ensure latest images
    docker-compose -f docker-compose-consul.yml up -d --build
    
    print_success "Stack startup initiated"
}

# Function to wait for services to be healthy
wait_for_services() {
    print_info "Waiting for services to be healthy..."
    
    services=("consul" "postgres" "redis" "consul-config-setup" "trading-service" "kong")
    
    for service in "${services[@]}"; do
        print_info "Waiting for $service to be ready..."
        
        max_attempts=60
        attempt=1
        
        while [ $attempt -le $max_attempts ]; do
            if docker-compose -f docker-compose-consul.yml ps $service | grep -q "healthy\|Up"; then
                print_success "$service is ready"
                break
            elif [ $attempt -eq $max_attempts ]; then
                print_warning "$service taking longer than expected, continuing..."
                break
            else
                sleep 5
                attempt=$((attempt + 1))
            fi
        done
    done
}

# Function to verify services
verify_services() {
    print_info "Verifying services are accessible..."
    
    # Wait a bit more for services to fully initialize
    sleep 10
    
    # Check Consul UI
    if curl -s "http://localhost:8500/ui" > /dev/null 2>&1; then
        print_success "Consul UI is accessible at http://localhost:8500/ui"
    else
        print_warning "Consul UI may not be ready yet at http://localhost:8500/ui"
    fi
    
    # Check Trading Service Health
    if curl -s "http://localhost:8083/api/v2/health" > /dev/null 2>&1; then
        print_success "Trading Service is healthy at http://localhost:8083/api/v2/health"
    else
        print_warning "Trading Service may not be ready yet at http://localhost:8083/api/v2/health"
    fi
    
    # Check Trading Service API Documentation
    if curl -s "http://localhost:8083/swagger-ui.html" > /dev/null 2>&1; then
        print_success "Trading API Documentation is available at http://localhost:8083/swagger-ui.html"
    else
        print_warning "Trading API Documentation may not be ready yet"
    fi
    
    # Check Kong Admin API
    if curl -s "http://localhost:8001" > /dev/null 2>&1; then
        print_success "Kong Admin API is accessible at http://localhost:8001"
    else
        print_warning "Kong Admin API may not be ready yet at http://localhost:8001"
    fi
    
    # Check Prometheus
    if curl -s "http://localhost:9090" > /dev/null 2>&1; then
        print_success "Prometheus is accessible at http://localhost:9090"
    else
        print_warning "Prometheus may not be ready yet at http://localhost:9090"
    fi
}

# Function to show service URLs
show_service_urls() {
    echo
    print_success "🎉 TradeMaster Platform is starting up!"
    echo
    print_info "📋 Service URLs:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}📊 Trading API Documentation:${NC} http://localhost:8083/swagger-ui.html"
    echo -e "${BLUE}🚀 Kong API Gateway:${NC}        http://localhost:8000/api/v2"
    echo -e "${BLUE}⚙️  Kong Admin API:${NC}          http://localhost:8001"
    echo -e "${BLUE}🔍 Consul Service Discovery:${NC} http://localhost:8500/ui"
    echo -e "${BLUE}📈 Prometheus Metrics:${NC}       http://localhost:9090"
    echo -e "${BLUE}❤️  Trading Service Health:${NC}  http://localhost:8083/api/v2/health"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
    print_info "💡 Tip: Services may take a few more minutes to be fully ready"
    print_info "💡 Use 'docker-compose -f docker-compose-consul.yml ps' to check status"
    print_info "💡 Use 'docker-compose -f docker-compose-consul.yml logs -f' to view logs"
    echo
}

# Function to show next steps
show_next_steps() {
    print_info "🚀 Next Steps:"
    echo "  1. Wait 2-3 minutes for all services to be fully ready"
    echo "  2. Visit http://localhost:8083/swagger-ui.html for API documentation"
    echo "  3. Check service registration at http://localhost:8500/ui"
    echo "  4. Monitor metrics at http://localhost:9090"
    echo
    print_info "🛠️  Useful Commands:"
    echo "  • Check service status: docker-compose -f docker-compose-consul.yml ps"
    echo "  • View all logs:       docker-compose -f docker-compose-consul.yml logs -f"
    echo "  • Stop all services:   docker-compose -f docker-compose-consul.yml down"
    echo "  • Full cleanup:        docker-compose -f docker-compose-consul.yml down -v"
    echo
}

# Main execution
main() {
    echo -e "${GREEN}🚀 TradeMaster Platform Startup${NC}"
    echo "=================================="
    echo
    
    check_docker
    check_docker_compose
    cleanup_containers
    start_stack
    wait_for_services
    verify_services
    show_service_urls
    show_next_steps
    
    print_success "TradeMaster platform startup completed!"
}

# Run main function
main "$@"