# TradeMaster Authentication Service

Enterprise authentication service with Java 24 Virtual Threads and Spring Boot 3.5.3

## Features
- JWT authentication
- MFA support
- Zero Trust Architecture
- High-performance role caching
- Kong Gateway integration

## API Endpoints
### Authentication
- POST /api/v1/auth/register
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/auth/logout

### MFA
- POST /api/v1/mfa/enable
- POST /api/v1/mfa/verify

### Device Management
- GET /api/v1/devices
- POST /api/v1/devices/trust

### Internal (Kong headers required)
- GET /internal/v1/auth/validate-token
- GET /internal/v1/auth/user/{userId}

## Setup
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=dev'

## Testing
./gradlew test

### Load Test Results
Test: testConcurrentUserRegistration
- Success Rate: 100% (50/50)
- Throughput: 8+ reg/sec
- Status: PASSED

## Performance
- Login: < 50ms (without MFA)
- Registration: < 150ms
- Concurrent Users: 10,000+

## Security
- JWT with RS256 signing
- Bcrypt password hashing
- Rate limiting
- Circuit breaker

---
Built by TradeMaster Engineering Team
