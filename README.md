# TradeMaster Authentication Service

Enterprise authentication service with Java 24 Virtual Threads and Spring Boot 3.5.3

## Features
- JWT authentication
- MFA support
- Zero Trust Architecture
- High-performance role caching
- Kong Gateway integration
- Broker OAuth 2.0 integration (Zerodha, Upstox, Angel One, ICICI Direct)

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

## Broker Integration
For detailed OAuth 2.0 integration with Indian stock brokers, see [BROKER_OAUTH_GUIDE.md](./BROKER_OAUTH_GUIDE.md)

Supported brokers:
- **Zerodha Kite Connect** - OAuth 2.0 with custom flow
- **Upstox** - Standard OAuth 2.0
- **Angel One** - OAuth 2.0 with PKCE
- **ICICI Direct** - OAuth 2.0

Features:
- PKCE flow for enhanced security
- Token encryption (AES-256)
- Automatic token refresh
- Vault integration for secure storage

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
