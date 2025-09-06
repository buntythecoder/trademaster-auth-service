# TradeMaster Deployment Guide

## Quick Start (One-Click Setup)

### Windows
```bash
# Run the startup script
.\scripts\start-trademaster.bat
```

### Linux/macOS
```bash
# Make script executable and run
chmod +x scripts/start-trademaster.sh
./scripts/start-trademaster.sh
```

## What's Included

### Infrastructure Services
- **PostgreSQL 15**: Primary database with multiple service databases
- **Redis 7**: Caching and session storage
- **Apache Kafka**: Event streaming platform
- **Elasticsearch**: Search and analytics engine  
- **MinIO**: S3-compatible object storage
- **MLflow**: ML model tracking and registry

### TradeMaster Services
- **Agent Orchestration Service** (Port 8090): AgentOS coordination
- **Broker Authentication Service** (Port 8087): Multi-broker authentication
- **Behavioral AI Service** (Port 8085): AI-powered trading insights
- **ML Infrastructure Platform** (Port 8088): Machine learning platform
- **Market Data Service** (Port 8082): Real-time market data
- **Notification Service** (Port 8084): Multi-channel notifications
- **Risk Management Service** (Port 8086): Risk assessment and limits
- **Payment Gateway Service** (Port 8089): Payment processing

### Additional Services
- **Nginx Load Balancer** (Port 80): API gateway and load balancer
- **pgAdmin** (Port 5050): Database management (development mode)
- **Redis Commander** (Port 8081): Redis management (development mode)

## Configuration

### Environment Variables

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` file with your configuration:

#### Required Configuration
```env
# Database (Change in production)
POSTGRES_USER=trademaster
POSTGRES_PASSWORD=your_secure_password

# Redis (Change in production)  
REDIS_PASSWORD=your_redis_password

# JWT Secret (MUST be at least 32 characters)
JWT_SECRET=your-super-secret-jwt-key-for-production

# Broker Encryption (MUST be exactly 32 bytes)
BROKER_ENCRYPTION_KEY=your-32-byte-encryption-key
```

#### Broker API Keys (Fill as needed)
```env
# Zerodha
ZERODHA_APP_ID=your_zerodha_app_id
ZERODHA_API_SECRET=your_zerodha_api_secret

# Upstox
UPSTOX_CLIENT_ID=your_upstox_client_id
UPSTOX_CLIENT_SECRET=your_upstox_client_secret

# Angel One
ANGEL_CLIENT_CODE=your_angel_client_code
ANGEL_API_KEY=your_angel_api_key
```

#### Payment Gateway Configuration
```env
# Razorpay
RAZORPAY_KEY_ID=your_razorpay_key
RAZORPAY_KEY_SECRET=your_razorpay_secret

# Stripe  
STRIPE_PUBLISHABLE_KEY=your_stripe_key
STRIPE_SECRET_KEY=your_stripe_secret
```

## Service URLs

After startup, services will be available at:

| Service | URL | Description |
|---------|-----|-------------|
| Load Balancer | http://localhost | Main API gateway |
| Agent Orchestration | http://localhost:8090/agent-os | AgentOS management |
| Broker Auth | http://localhost:8087/api/v1 | Broker authentication |
| Behavioral AI | http://localhost:8085/behavioral-ai | AI insights |
| ML Platform | http://localhost:8088 | ML infrastructure |
| Market Data | http://localhost:8082 | Market data APIs |
| Notifications | http://localhost:8084 | Notification APIs |
| Risk Management | http://localhost:8086 | Risk APIs |
| Payment Gateway | http://localhost:8089 | Payment APIs |
| MLflow | http://localhost:5000 | ML model registry |
| pgAdmin | http://localhost:5050 | Database admin |
| Redis Commander | http://localhost:8081 | Redis management |

## Health Checks

All services include health check endpoints:
```bash
# Check specific service health
curl http://localhost:8090/agent-os/actuator/health

# Check load balancer health
curl http://localhost/health
```

## Logs and Monitoring

### View Service Logs
```bash
# View all logs
docker compose logs -f

# View specific service logs
docker compose logs -f agent-orchestration-service
docker compose logs -f broker-auth-service
docker compose logs -f behavioral-ai-service

# Follow logs in real-time
docker compose logs -f --tail=100 ml-infrastructure-platform
```

### Log Files
Logs are also written to files in the `./logs/` directory:
- `./logs/agent-orchestration/`
- `./logs/broker-auth/`
- `./logs/behavioral-ai/`
- `./logs/ml-platform/`
- `./logs/market-data/`
- `./logs/notification/`
- `./logs/risk-management/`
- `./logs/payment-gateway/`

## Management Commands

### Start Services
```bash
# Windows
.\scripts\start-trademaster.bat

# Linux/macOS  
./scripts/start-trademaster.sh
```

### Stop Services
```bash
# Windows (preserves data)
.\scripts\stop-trademaster.bat

# Linux/macOS (preserves data)
./scripts/stop-trademaster.sh

# Stop and remove all data
docker compose down -v
```

### Restart Individual Services
```bash
# Restart specific service
docker compose restart broker-auth-service

# Update and restart service
docker compose up -d --force-recreate agent-orchestration-service
```

### Scale Services
```bash
# Scale market data service to 3 instances
docker compose up -d --scale market-data-service=3

# Scale notification service to 2 instances  
docker compose up -d --scale notification-service=2
```

## Development Mode

Enable development tools:
```bash
# Start with development profile
docker compose --profile development up -d

# Or set environment variable
export COMPOSE_PROFILES=development
docker compose up -d
```

Development mode includes:
- pgAdmin for database management
- Redis Commander for Redis management
- Additional logging and debugging

## Production Considerations

### Security
1. **Change all default passwords** in `.env`
2. **Use strong JWT secrets** (minimum 32 characters)
3. **Configure proper CORS origins**
4. **Set up SSL/TLS** certificates in `docker/nginx/ssl/`
5. **Use environment-specific secrets management**

### Performance
1. **Adjust memory limits** in docker-compose.yml
2. **Configure connection pooling** settings
3. **Set up monitoring** with Prometheus/Grafana
4. **Configure log rotation** for production

### High Availability
1. **Use external databases** for production
2. **Set up Redis clustering**
3. **Configure Kafka clusters**
4. **Use container orchestration** (Kubernetes)

## Troubleshooting

### Common Issues

#### Services not starting
```bash
# Check Docker daemon
docker info

# Check service status
docker compose ps

# View startup logs
docker compose logs service-name
```

#### Database connection issues
```bash
# Check PostgreSQL logs
docker compose logs postgres

# Verify database creation
docker compose exec postgres psql -U trademaster -l
```

#### Port conflicts
```bash
# Check what's using the port
netstat -tulpn | grep :8080

# Change ports in docker-compose.yml if needed
```

#### Memory issues
```bash
# Check container resource usage
docker stats

# Increase memory limits in docker-compose.yml
```

### Service Dependencies

Services start in this order:
1. Infrastructure (PostgreSQL, Redis, Kafka, etc.)
2. Core services (Agent Orchestration, Broker Auth)
3. AI services (Behavioral AI, ML Platform)
4. Data services (Market Data, Notifications)
5. Business services (Risk Management, Payments)
6. Support services (MLflow, Load Balancer)

## Backup and Restore

### Backup Data
```bash
# Backup PostgreSQL databases
docker compose exec postgres pg_dumpall -U trademaster > backup.sql

# Backup Redis data
docker compose exec redis redis-cli --rdb /data/dump.rdb

# Backup volumes
docker run --rm -v trademaster_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz -C /data .
```

### Restore Data
```bash
# Restore PostgreSQL
cat backup.sql | docker compose exec -T postgres psql -U trademaster

# Restore volumes
docker run --rm -v trademaster_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres_backup.tar.gz -C /data
```

## Support

For issues and questions:
1. Check the logs: `docker compose logs -f`
2. Verify configuration: check `.env` file
3. Check service health: `curl http://localhost/health`
4. Review this documentation
5. Check Docker and Docker Compose versions

## Next Steps

After successful deployment:
1. Configure broker API keys in `.env`
2. Set up payment gateways
3. Configure notification channels
4. Load ML models into the platform
5. Set up monitoring and alerting
6. Configure backup procedures
7. Plan for production deployment