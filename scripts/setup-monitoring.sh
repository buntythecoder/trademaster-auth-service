#!/bin/bash

# TradeMaster Monitoring & Observability Setup Script
# This script sets up comprehensive monitoring, logging, and alerting

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TRADEMASTER_DIR=$(pwd)
MONITORING_DIR="$TRADEMASTER_DIR/monitoring"
GRAFANA_DIR="$MONITORING_DIR/grafana"
LOGSTASH_DIR="$MONITORING_DIR/logstash"

echo -e "${BLUE}🚀 Setting up TradeMaster Monitoring & Observability...${NC}"

# Create directory structure
echo -e "${YELLOW}📁 Creating monitoring directory structure...${NC}"
mkdir -p $MONITORING_DIR/{grafana/{dashboards,datasources},logstash/{config,pipeline}}

# Create Grafana datasource configuration
echo -e "${YELLOW}📊 Setting up Grafana datasources...${NC}"
cat > $GRAFANA_DIR/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true

  - name: Elasticsearch
    type: elasticsearch
    access: proxy
    url: http://elasticsearch:9200
    database: "logstash-*"
    timeField: "@timestamp"
    editable: true

  - name: Jaeger
    type: jaeger
    access: proxy
    url: http://jaeger:16686
    editable: true
EOF

# Create Grafana dashboard provisioning
cat > $GRAFANA_DIR/dashboards/dashboards.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'TradeMaster Dashboards'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF

# Create comprehensive TradeMaster overview dashboard
echo -e "${YELLOW}📊 Creating TradeMaster overview dashboard...${NC}"
cat > $GRAFANA_DIR/dashboards/trademaster-overview.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "TradeMaster - System Overview",
    "tags": ["trademaster", "overview"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "System Health Overview",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\"auth-service|user-profile-service|trading-engine|notification-service\"}",
            "legendFormat": "{{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
        "options": {
          "colorMode": "background",
          "graphMode": "none",
          "justifyMode": "auto"
        }
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (job)",
            "legendFormat": "{{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, job))",
            "legendFormat": "{{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) by (job) / sum(rate(http_requests_total[5m])) by (job)",
            "legendFormat": "{{job}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      }
    ],
    "time": {"from": "now-1h", "to": "now"},
    "refresh": "5s"
  }
}
EOF

# Create Logstash configuration
echo -e "${YELLOW}📝 Setting up Logstash pipeline...${NC}"
cat > $LOGSTASH_DIR/config/logstash.yml << 'EOF'
http.host: "0.0.0.0"
xpack.monitoring.elasticsearch.hosts: ["http://elasticsearch:9200"]
path.config: /usr/share/logstash/pipeline
EOF

# Create Logstash pipeline for application logs
cat > $LOGSTASH_DIR/pipeline/logstash.conf << 'EOF'
input {
  beats {
    port => 5044
  }
  
  tcp {
    port => 5000
    codec => json_lines
  }

  http {
    port => 8080
  }
}

filter {
  # Parse Spring Boot logs
  if [fields][service] =~ /(auth|user-profile|trading|notification)/ {
    grok {
      match => { 
        "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:thread} %{DATA:logger} - %{GREEDYDATA:log_message}" 
      }
    }
    
    date {
      match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
    }
    
    mutate {
      add_field => { "service" => "%{[fields][service]}" }
    }
  }
  
  # Parse trading-specific logs
  if [fields][service] == "trading-engine" {
    if [log_message] =~ /Trade executed/ {
      grok {
        match => { 
          "log_message" => "Trade executed: %{DATA:trade_action} %{NUMBER:quantity} %{DATA:symbol} at %{NUMBER:price}" 
        }
      }
      mutate {
        add_field => { "event_type" => "trade_execution" }
      }
    }
  }
  
  # Parse authentication logs
  if [fields][service] == "auth-service" {
    if [log_message] =~ /(Login|Authentication)/ {
      mutate {
        add_field => { "event_type" => "authentication" }
      }
    }
  }
  
  # Add common fields
  mutate {
    add_field => { "environment" => "local" }
    add_field => { "application" => "trademaster" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "trademaster-%{+YYYY.MM.dd}"
    template_name => "trademaster"
    template_pattern => "trademaster-*"
    template => {
      "index_patterns" => ["trademaster-*"],
      "settings" => {
        "number_of_shards" => 1,
        "number_of_replicas" => 0
      },
      "mappings" => {
        "properties" => {
          "@timestamp" => { "type" => "date" },
          "level" => { "type" => "keyword" },
          "service" => { "type" => "keyword" },
          "event_type" => { "type" => "keyword" },
          "message" => { "type" => "text" },
          "log_message" => { "type" => "text" }
        }
      }
    }
  }
  
  stdout {
    codec => rubydebug
  }
}
EOF

# Create monitoring startup script
echo -e "${YELLOW}🔧 Creating monitoring startup script...${NC}"
cat > $TRADEMASTER_DIR/start-monitoring.sh << 'EOF'
#!/bin/bash

echo "🚀 Starting TradeMaster Monitoring Stack..."

# Start monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

echo "⏳ Waiting for services to start..."
sleep 30

# Check service health
echo "🏥 Checking service health..."
services=("prometheus:9090" "grafana:3001" "elasticsearch:9200" "kibana:5601" "jaeger:16686" "alertmanager:9093")

for service in "${services[@]}"; do
    name=$(echo $service | cut -d: -f1)
    port=$(echo $service | cut -d: -f2)
    if curl -f -s "http://localhost:$port" > /dev/null 2>&1; then
        echo "✅ $name is healthy"
    else
        echo "❌ $name is not responding"
    fi
done

echo ""
echo "📊 Monitoring Dashboard URLs:"
echo "   Grafana: http://localhost:3001 (admin/trademaster123)"
echo "   Prometheus: http://localhost:9090"
echo "   Kibana: http://localhost:5601"
echo "   Jaeger: http://localhost:16686"
echo "   AlertManager: http://localhost:9093"
echo ""
echo "🎉 TradeMaster Monitoring Stack is ready!"
EOF

chmod +x $TRADEMASTER_DIR/start-monitoring.sh

# Create monitoring teardown script
cat > $TRADEMASTER_DIR/stop-monitoring.sh << 'EOF'
#!/bin/bash

echo "🛑 Stopping TradeMaster Monitoring Stack..."
docker-compose -f docker-compose.monitoring.yml down

echo "🗑️ Cleaning up monitoring volumes..."
docker-compose -f docker-compose.monitoring.yml down -v

echo "✅ TradeMaster Monitoring Stack stopped"
EOF

chmod +x $TRADEMASTER_DIR/stop-monitoring.sh

# Create health check script
echo -e "${YELLOW}🏥 Creating health check script...${NC}"
cat > $TRADEMASTER_DIR/health-check.sh << 'EOF'
#!/bin/bash

# TradeMaster Health Check Script
echo "🏥 TradeMaster Health Check"
echo "=========================="

# Function to check service health
check_service() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    if curl -f -s -o /dev/null -w "%{http_code}" "$url" | grep -q "$expected_code"; then
        echo "✅ $name: Healthy"
        return 0
    else
        echo "❌ $name: Unhealthy"
        return 1
    fi
}

# Check application services
echo "📱 Application Services:"
check_service "Auth Service" "http://localhost:8081/actuator/health"
check_service "User Profile Service" "http://localhost:8082/actuator/health"  
check_service "Trading Engine" "http://localhost:8083/actuator/health"
check_service "Notification Service" "http://localhost:8084/actuator/health"
check_service "Frontend" "http://localhost:5173"

echo ""
echo "🔧 Infrastructure Services:"
check_service "PostgreSQL" "http://localhost:5432" "000"
check_service "Redis" "http://localhost:6379" "000"
check_service "MinIO" "http://localhost:9000/minio/health/live"

echo ""
echo "📊 Monitoring Services:"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3001/api/health"
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"
check_service "Kibana" "http://localhost:5601/api/status"
check_service "Jaeger" "http://localhost:16686"
check_service "AlertManager" "http://localhost:9093/-/healthy"

echo ""
echo "📈 System Metrics:"
echo "   CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F% '{print $1}')%"
echo "   Memory: $(free | grep Mem | awk '{printf("%.1f%%\n", $3/$2 * 100.0)}')"
echo "   Disk: $(df -h / | awk 'NR==2{printf "%s", $5}')"

echo ""
echo "Health check completed! 🎉"
EOF

chmod +x $TRADEMASTER_DIR/health-check.sh

# Create comprehensive README for monitoring
echo -e "${YELLOW}📚 Creating monitoring documentation...${NC}"
cat > $MONITORING_DIR/README.md << 'EOF'
# TradeMaster Monitoring & Observability

This directory contains the complete monitoring and observability setup for TradeMaster.

## 🏗️ Architecture

### Metrics Collection (Prometheus)
- **Application Metrics**: Spring Boot Actuator endpoints
- **Infrastructure Metrics**: Node Exporter, cAdvisor, Postgres/Redis exporters
- **Business Metrics**: Custom trading, user, and KYC metrics
- **JVM Metrics**: Memory, GC, thread pools

### Visualization (Grafana)
- **System Overview Dashboard**: Health, performance, and error rates
- **Application Dashboards**: Service-specific metrics
- **Infrastructure Dashboards**: System resources and database performance
- **Business Dashboards**: Trading volume, user activity, KYC status

### Logging (ELK Stack)
- **Elasticsearch**: Log storage and indexing
- **Logstash**: Log processing and parsing
- **Kibana**: Log analysis and visualization
- **Filebeat**: Log shipping from applications

### Tracing (Jaeger)
- **Distributed Tracing**: End-to-end request tracking
- **Performance Analysis**: Latency and bottleneck identification
- **Service Dependencies**: Service interaction mapping

### Alerting (AlertManager)
- **Multi-channel Alerts**: Email, SMS, webhook notifications
- **Team-based Routing**: Security, trading, compliance teams
- **Severity-based Handling**: Critical, warning, info alerts
- **Alert Deduplication**: Intelligent grouping and inhibition

## 🚀 Quick Start

### Start Monitoring Stack
```bash
./start-monitoring.sh
```

### Access Dashboards
- **Grafana**: http://localhost:3001 (admin/trademaster123)
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601
- **Jaeger**: http://localhost:16686
- **AlertManager**: http://localhost:9093

### Health Check
```bash
./health-check.sh
```

### Stop Monitoring Stack
```bash
./stop-monitoring.sh
```

## 📊 Key Metrics

### Application Metrics
- `http_requests_total`: Total HTTP requests
- `http_request_duration_seconds`: Request latency
- `jvm_memory_used_bytes`: JVM memory usage
- `hikaricp_connections_active`: Database connections

### Business Metrics
- `user_registrations_total`: User registrations
- `trading_volume_total`: Trading volume
- `kyc_pending_verifications`: KYC backlog
- `notification_delivery_failed_total`: Failed notifications

### Infrastructure Metrics
- `node_cpu_seconds_total`: CPU usage
- `node_memory_MemTotal_bytes`: Memory usage
- `pg_stat_activity_count`: Database connections
- `redis_memory_used_bytes`: Cache usage

## 🚨 Alerts

### Critical Alerts
- Application down (1 minute)
- Database connection pool exhaustion
- High rate of failed trades
- Security incidents

### Warning Alerts
- High response time (>2s for 5 minutes)
- High error rate (>5% for 2 minutes)
- High resource usage (CPU >80%, Memory >85%)
- KYC processing backlog

### Info Alerts
- Low user registrations
- Unusual trading patterns
- System maintenance notifications

## 🔧 Configuration

### Adding New Services
1. Update `prometheus.yml` with new scrape targets
2. Create service-specific dashboards in Grafana
3. Add relevant alerts in `trademaster-alerts.yml`
4. Update logstash pipeline for new log formats

### Custom Metrics
- Add Micrometer meters in Spring Boot applications
- Export metrics via `/actuator/prometheus` endpoint
- Create custom dashboards and alerts

### Log Parsing
- Update Logstash pipeline configuration
- Add grok patterns for new log formats
- Create Kibana index patterns and visualizations

## 📚 Best Practices

### Metrics
- Use appropriate metric types (counter, gauge, histogram, summary)
- Include relevant labels for filtering and grouping
- Avoid high cardinality labels
- Monitor both RED (Rate, Errors, Duration) and USE (Utilization, Saturation, Errors) metrics

### Alerting
- Create runbooks for alert responses
- Set appropriate thresholds to avoid alert fatigue
- Use alert inhibition to prevent noise
- Test alert routes and escalation procedures

### Dashboards
- Create role-specific dashboards
- Use templating for dynamic filtering
- Include SLA/SLO tracking
- Add annotations for deployments and incidents

### Logging
- Use structured logging (JSON format)
- Include correlation IDs for distributed tracing
- Log at appropriate levels
- Include relevant context in log messages

## 🔒 Security Considerations

- Enable authentication for all monitoring services
- Use HTTPS for external access
- Implement RBAC for different teams
- Regularly rotate credentials
- Monitor access to sensitive metrics
EOF

echo -e "${GREEN}✅ TradeMaster Monitoring & Observability setup complete!${NC}"
echo ""
echo -e "${BLUE}📊 Next Steps:${NC}"
echo "1. Run: ${YELLOW}./start-monitoring.sh${NC} to start the monitoring stack"
echo "2. Access Grafana at: ${YELLOW}http://localhost:3001${NC} (admin/trademaster123)"
echo "3. Run: ${YELLOW}./health-check.sh${NC} to verify all services"
echo "4. Configure your applications to export metrics to Prometheus"
echo ""
echo -e "${GREEN}🎉 Happy Monitoring!${NC}"
EOF

chmod +x $TRADEMASTER_DIR/scripts/setup-monitoring.sh