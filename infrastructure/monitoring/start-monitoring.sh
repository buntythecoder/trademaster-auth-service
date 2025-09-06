#!/bin/bash

# TradeMaster Monitoring Stack Startup Script
# Deploys Prometheus, Grafana, AlertManager, and related monitoring services

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 Starting TradeMaster Monitoring Stack..."

# Create necessary directories for persistent data
echo "📁 Creating data directories..."
mkdir -p data/prometheus data/grafana data/alertmanager
mkdir -p grafana/dashboards

# Set proper permissions (skip on Windows/MSYS)
echo "🔒 Setting permissions..."
if command -v sudo >/dev/null 2>&1; then
    sudo chown -R 472:472 data/grafana 2>/dev/null || echo "Note: Could not set grafana permissions"
    sudo chown -R 65534:65534 data/prometheus 2>/dev/null || echo "Note: Could not set prometheus permissions"
    sudo chown -R 65534:65534 data/alertmanager 2>/dev/null || echo "Note: Could not set alertmanager permissions"
else
    echo "Note: Running on Windows - skipping permission setup"
fi

# Ensure main network exists
echo "🌐 Checking network configuration..."
if ! docker network ls | grep -q "trademaster-network"; then
    echo "Creating trademaster-network..."
    docker network create trademaster-network
else
    echo "Network trademaster-network already exists"
fi

# Validate configuration files
echo "✅ Validating configuration files..."
if [ ! -f "prometheus.yml" ]; then
    echo "❌ prometheus.yml not found"
    exit 1
fi

if [ ! -f "alertmanager.yml" ]; then
    echo "❌ alertmanager.yml not found"
    exit 1
fi

echo "Configuration files validated successfully"

# Check if monitoring stack is already running
echo "🔍 Checking existing services..."
RUNNING_SERVICES=$(docker-compose -f docker-compose-monitoring.yml ps -q)
if [ ! -z "$RUNNING_SERVICES" ]; then
    echo "⚠️ Monitoring services are already running. Stopping them first..."
    docker-compose -f docker-compose-monitoring.yml down
fi

# Start monitoring stack
echo "🚀 Starting monitoring services..."
docker-compose -f docker-compose-monitoring.yml up -d

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Health check function
check_service_health() {
    local service_name=$1
    local url=$2
    local timeout=60
    local count=0
    
    echo "Checking $service_name health..."
    while [ $count -lt $timeout ]; do
        if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|302"; then
            echo "✅ $service_name is healthy"
            return 0
        fi
        echo "   Waiting for $service_name... ($((count + 1))/$timeout)"
        sleep 2
        count=$((count + 1))
    done
    echo "❌ $service_name failed to start within ${timeout} attempts"
    return 1
}

# Check service health
echo "🏥 Performing health checks..."
check_service_health "Prometheus" "http://localhost:9090/-/healthy"
check_service_health "Grafana" "http://localhost:3000/api/health"
check_service_health "AlertManager" "http://localhost:9093/-/healthy"
check_service_health "Node Exporter" "http://localhost:9100/metrics"

# Display service URLs and access information
echo ""
echo "🎉 TradeMaster Monitoring Stack Started Successfully!"
echo ""
echo "📊 Service URLs:"
echo "   • Grafana:         http://localhost:3000"
echo "     Login: admin / trademaster2024"
echo ""
echo "   • Prometheus:      http://localhost:9090"
echo "     Targets: http://localhost:9090/targets"
echo "     Rules: http://localhost:9090/rules"
echo ""
echo "   • AlertManager:    http://localhost:9093"
echo "     Status: http://localhost:9093/#/status"
echo ""
echo "   • Node Exporter:   http://localhost:9100/metrics"
echo "   • cAdvisor:        http://localhost:8080"
echo "   • Redis Exporter:  http://localhost:9121/metrics"
echo "   • Postgres Exporter: http://localhost:9187/metrics"
echo ""
echo "🔧 Next Steps:"
echo "1. Import Grafana dashboards from grafana/dashboards/ directory"
echo "2. Configure AlertManager SMTP settings in alertmanager.yml"
echo "3. Set up Slack webhooks for critical alerts"
echo "4. Review and customize alert rules in alert_rules.yml and trading_rules.yml"
echo ""
echo "📈 Default Dashboards Available:"
echo "   • TradeMaster Overview"
echo "   • Trading System Metrics"
echo "   • Portfolio Analytics"
echo "   • Infrastructure Monitoring"
echo "   • Security Dashboard"
echo ""
echo "🚨 Alert Categories Configured:"
echo "   • Critical (immediate): Trading, Financial, Security"
echo "   • Warning (grouped): Performance, Infrastructure, Database"
echo ""
echo "For troubleshooting, check logs with:"
echo "docker-compose -f docker-compose-monitoring.yml logs [service-name]"
echo ""

# Optional: Start main TradeMaster services if they're not running
if [ "$1" = "--start-main" ]; then
    echo "🔄 Also starting main TradeMaster services..."
    cd ..
    if [ -f "docker-compose.yml" ]; then
        docker-compose up -d
        echo "✅ Main services started"
    else
        echo "⚠️ Main docker-compose.yml not found in parent directory"
    fi
fi

echo "✨ Monitoring stack deployment complete!"