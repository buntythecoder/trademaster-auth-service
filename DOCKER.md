# TradeMaster Authentication Service - Docker Deployment Guide

This guide provides comprehensive instructions for running the TradeMaster Authentication Service using Docker and Docker Compose.

## 🏗️ Architecture Overview

The TradeMaster Authentication Service consists of the following components:

- **Auth Service**: Spring Boot 3.x application with JWT authentication
- **PostgreSQL**: Primary database for user data and audit logs
- **Redis**: Session storage and caching
- **Nginx**: Reverse proxy and load balancer (production)
- **PgAdmin**: Database administration interface (development)
- **Redis Commander**: Redis management interface (development)

## 📋 Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Git**: For cloning the repository
- **Curl**: For testing API endpoints (optional)

### System Requirements

**Minimum:**
- RAM: 4 GB
- CPU: 2 cores
- Disk: 10 GB free space

**Recommended:**
- RAM: 8 GB
- CPU: 4 cores
- Disk: 20 GB free space

## 🚀 Quick Start

### Development Environment

1. **Clone the repository and navigate to the project directory:**
   ```bash
   git clone <repository-url>
   cd trademaster
   ```

2. **Start the development environment:**

   **Linux/macOS:**
   ```bash
   chmod +x scripts/start-dev.sh
   ./scripts/start-dev.sh
   ```

   **Windows:**
   ```cmd
   scripts\start-dev.bat
   ```

   **Manual Docker Compose:**
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d
   ```

3. **Verify the services are running:**
   ```bash
   curl http://localhost:8080/api/v1/auth/health
   ```

### Production Environment

1. **Set up environment variables:**
   ```bash
   export JWT_SECRET_PROD="your-super-secure-256-bit-jwt-secret-key-here"
   export DB_PASSWORD_PROD="your-secure-database-password"
   export REDIS_PASSWORD_PROD="your-secure-redis-password"
   ```

2. **Start the production environment:**
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
   ```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_SECRET` | JWT signing secret (256-bit) | Development key | Yes |
| `DB_USERNAME` | Database username | `trademaster_user` | No |
| `DB_PASSWORD` | Database password | `trademaster_password` | No |
| `REDIS_HOST` | Redis hostname | `redis` | No |
| `REDIS_PORT` | Redis port | `6379` | No |
| `REDIS_PASSWORD` | Redis password | `trademaster_redis_pass` | No |
| `AWS_REGION` | AWS region for KMS | `us-east-1` | No |
| `AWS_KMS_KEY_ID` | KMS key ID for encryption | `alias/trademaster-encryption-key` | No |

### Rate Limiting Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `LOGIN_RATE_LIMIT` | Login attempts per window | `5` |
| `LOGIN_RATE_WINDOW` | Login rate limit window (minutes) | `1` |
| `REGISTRATION_RATE_LIMIT` | Registration attempts per window | `3` |
| `REGISTRATION_RATE_WINDOW` | Registration rate limit window (minutes) | `60` |
| `PASSWORD_RESET_RATE_LIMIT` | Password reset attempts per window | `2` |
| `PASSWORD_RESET_RATE_WINDOW` | Password reset rate limit window (minutes) | `60` |

## 📊 Service URLs

### Development Environment

| Service | URL | Description |
|---------|-----|-------------|
| Auth Service API | http://localhost:8080/api/v1/auth | Main API endpoints |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| Health Check | http://localhost:8080/api/v1/auth/health | Service health status |
| Actuator | http://localhost:8080/actuator | Spring Boot monitoring |
| PgAdmin | http://localhost:5050 | PostgreSQL admin interface |
| Redis Commander | http://localhost:8081 | Redis management interface |

### Production Environment

| Service | URL | Description |
|---------|-----|-------------|
| Auth Service API | http://localhost/api/v1/auth | Main API endpoints (via Nginx) |
| Swagger UI | http://localhost/api/v1/swagger-ui.html | API documentation |
| Health Check | http://localhost/api/v1/auth/health | Service health status |

## 🗃️ Database Access

### Development Database Connection

- **Host**: `localhost:5432`
- **Database**: `trademaster_auth`
- **Username**: `trademaster_user`
- **Password**: `trademaster_password`

### PgAdmin Access

- **URL**: http://localhost:5050
- **Email**: `admin@trademaster.com`
- **Password**: `admin123`

### Redis Access

- **Host**: `localhost:6379`
- **Password**: `trademaster_redis_pass`
- **Web Interface**: http://localhost:8081

## 🛠️ Docker Commands

### Service Management

```bash
# Start all services
docker-compose up -d

# Start with rebuild
docker-compose up --build -d

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f auth-service

# Restart a specific service
docker-compose restart auth-service

# Scale auth service (for load testing)
docker-compose up -d --scale auth-service=3
```

### Development vs Production

```bash
# Development environment
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Production environment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Start with specific profile
docker-compose --profile development up -d
docker-compose --profile production up -d
```

### Container Management

```bash
# List running containers
docker-compose ps

# Execute command in container
docker-compose exec auth-service bash
docker-compose exec postgres psql -U trademaster_user -d trademaster_auth

# View container resources
docker stats

# View container details
docker inspect trademaster-auth-service
```

## 🧪 Testing and Validation

### Health Checks

```bash
# Check auth service health
curl http://localhost:8080/api/v1/auth/health

# Check all services health
docker-compose ps

# Detailed health check
curl http://localhost:8080/actuator/health
```

### API Testing

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "StrongPassword123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "StrongPassword123!"
  }'
```

### Database Testing

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U trademaster_user -d trademaster_auth

# Run SQL queries
docker-compose exec postgres psql -U trademaster_user -d trademaster_auth -c "SELECT * FROM users;"
```

### Load Testing

```bash
# Start multiple auth service instances
docker-compose up -d --scale auth-service=3

# Use Apache Bench for load testing
ab -n 1000 -c 10 http://localhost:8080/api/v1/auth/health

# Use wrk for advanced load testing
wrk -t12 -c400 -d30s http://localhost:8080/api/v1/auth/health
```

## 🔍 Monitoring and Logging

### Log Management

```bash
# View real-time logs
docker-compose logs -f

# View logs for last 100 lines
docker-compose logs --tail=100

# View logs with timestamps
docker-compose logs -t

# Save logs to file
docker-compose logs > trademaster-logs.txt
```

### Monitoring

```bash
# View resource usage
docker stats

# View system events
docker system events

# View disk usage
docker system df

# Prometheus metrics (if enabled)
curl http://localhost:8080/actuator/prometheus
```

## 🛡️ Security Considerations

### Production Security Checklist

- [ ] Change all default passwords
- [ ] Use strong JWT secret (256-bit)
- [ ] Enable SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Enable audit logging
- [ ] Regular security updates
- [ ] Monitor for suspicious activities
- [ ] Backup encryption keys

### Network Security

```bash
# View network configuration
docker network ls
docker network inspect trademaster_trademaster-network

# Isolate containers (production)
docker network create --driver bridge --internal trademaster-internal
```

## 🚨 Troubleshooting

### Common Issues

**Service won't start:**
```bash
# Check logs
docker-compose logs auth-service

# Check container status
docker-compose ps

# Restart service
docker-compose restart auth-service
```

**Database connection issues:**
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Test database connection
docker-compose exec postgres pg_isready -U trademaster_user -d trademaster_auth

# Connect to database manually
docker-compose exec postgres psql -U trademaster_user -d trademaster_auth
```

**Redis connection issues:**
```bash
# Check Redis logs
docker-compose logs redis

# Test Redis connection
docker-compose exec redis redis-cli ping

# Check Redis configuration
docker-compose exec redis redis-cli info
```

**Performance issues:**
```bash
# Check resource usage
docker stats

# Check system resources
docker system df

# Clean up unused resources
docker system prune -a
```

### Debug Mode

Enable debug logging for troubleshooting:

```bash
# Add to docker-compose override
services:
  auth-service:
    environment:
      LOGGING_LEVEL_COM_TRADEMASTER: DEBUG
      LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY: DEBUG
```

## 🔄 Backup and Recovery

### Database Backup

```bash
# Create database backup
docker-compose exec postgres pg_dump -U trademaster_user -d trademaster_auth > backup.sql

# Restore database backup
docker-compose exec -T postgres psql -U trademaster_user -d trademaster_auth < backup.sql
```

### Redis Backup

```bash
# Create Redis backup
docker-compose exec redis redis-cli SAVE
docker cp trademaster-redis:/data/dump.rdb ./redis-backup.rdb

# Restore Redis backup
docker cp ./redis-backup.rdb trademaster-redis:/data/dump.rdb
docker-compose restart redis
```

## 📚 Additional Resources

- [Spring Boot Docker Documentation](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Documentation](https://hub.docker.com/_/postgres)
- [Redis Docker Documentation](https://hub.docker.com/_/redis)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [TradeMaster API Documentation](http://localhost:8080/swagger-ui.html)

## 🆘 Support

For issues and support:

1. Check the troubleshooting section above
2. Review application logs: `docker-compose logs -f auth-service`
3. Check service health: `curl http://localhost:8080/api/v1/auth/health`
4. Open an issue in the project repository

---

**Happy containerizing! 🐳**