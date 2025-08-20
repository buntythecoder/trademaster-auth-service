# API Specification

This is the API specification for the spec detailed in @.agent-os/specs/2025-08-20-infrastructure-deployment/spec.md

> Created: 2025-08-20
> Version: 1.0.0

## Infrastructure API Endpoints

### Health Check and Status Endpoints

### GET /health
**Purpose:** Service health check for load balancer and monitoring
**Parameters:** None
**Response:** JSON with service status, database connectivity, Redis connectivity
**Errors:** 503 Service Unavailable if any dependency is down

```json
{
  "status": "UP",
  "timestamp": "2025-08-20T10:30:00Z",
  "checks": {
    "database": "UP",
    "redis": "UP",
    "kong": "UP"
  }
}
```

### GET /health/ready
**Purpose:** Kubernetes/ECS readiness probe for zero-downtime deployments
**Parameters:** None  
**Response:** 200 OK when service is ready to accept traffic
**Errors:** 503 Service Unavailable during startup or shutdown

### GET /health/live
**Purpose:** Kubernetes/ECS liveness probe for automatic restart decisions
**Parameters:** None
**Response:** 200 OK when service is operational
**Errors:** 503 Service Unavailable when service needs restart

### GET /metrics
**Purpose:** Prometheus metrics endpoint for monitoring and alerting
**Parameters:** None
**Response:** Prometheus format metrics
**Errors:** 404 if metrics endpoint disabled

## Infrastructure Management Endpoints

### GET /actuator/info
**Purpose:** Service build information and version details
**Parameters:** None
**Response:** JSON with application name, version, build timestamp, Git commit
**Errors:** None

### GET /actuator/env
**Purpose:** Environment configuration inspection for debugging
**Parameters:** None
**Response:** JSON with active profiles and configuration properties
**Errors:** 403 Forbidden in production environment

### POST /actuator/shutdown
**Purpose:** Graceful shutdown endpoint for deployment automation
**Parameters:** None
**Response:** 200 OK with shutdown confirmation
**Errors:** 403 Forbidden if shutdown endpoint disabled

## Kong API Gateway Integration

### Rate Limiting Headers
All API responses include rate limiting information:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1629456000
```

### Authentication Token Validation
Kong validates JWT tokens and adds headers:
```
X-User-ID: user123
X-User-Roles: USER,TRADER
X-Kong-Request-ID: request-uuid
```

### CORS Configuration
Cross-Origin Resource Sharing headers for frontend integration:
```
Access-Control-Allow-Origin: https://trademaster.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
```

## Error Response Format

All infrastructure endpoints follow consistent error response format:

```json
{
  "timestamp": "2025-08-20T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Database connection failed",
  "path": "/health",
  "requestId": "req-uuid-123"
}
```

## Security Headers

All API responses include security headers:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000
```