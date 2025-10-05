# TradeMaster Authentication Service

**Version:** 1.0.0  
**Java:** 21  
**Spring Boot:** 3.2.0  
**Build Tool:** Gradle  

A comprehensive authentication and authorization service for the TradeMaster trading platform, providing enterprise-grade security features with financial industry compliance standards.

## 🔐 Security Features

### Core Authentication
- **JWT-based authentication** with 15-minute access tokens and 24-hour refresh tokens
- **Multi-factor authentication (MFA)** supporting SMS, Email, TOTP, and Biometric
- **Device fingerprinting** for fraud detection and trusted device management
- **Password policies** with complexity requirements and history tracking
- **Account lockout** mechanisms with automatic unlock after timeout
- **Session management** with Redis backend and concurrent session limits

### Data Protection
- **AES-256-GCM encryption** for sensitive data at rest
- **AWS KMS integration** for enterprise key management and rotation
- **TLS 1.3** for data in transit encryption
- **PostgreSQL Transparent Data Encryption (TDE)** ready
- **Field-level encryption** for PII and sensitive profile data

### Authorization & Access Control
- **Role-Based Access Control (RBAC)** with fine-grained permissions
- **Subscription tier management** (Free, Premium, Professional, Enterprise)
- **KYC status tracking** for regulatory compliance
- **API rate limiting** based on user tiers and authentication status

### Compliance & Auditing
- **Comprehensive audit logging** with blockchain-style integrity verification
- **SEBI compliance** features for Indian financial regulations
- **Immutable audit trail** with cryptographic signatures
- **Data retention policies** (7 years for financial compliance)
- **Security event monitoring** and threat detection

## 🏗️ Architecture

### Database Schema
- **PostgreSQL primary database** with optimized indexes
- **Flyway migrations** for version-controlled schema management
- **Comprehensive entity relationships** (Users, Profiles, Roles, MFA, Devices, Audit)
- **JSON columns** for flexible metadata and behavioral settings

### Caching & Session Management
- **Redis session store** with 24-hour TTL
- **Concurrent session management** with configurable limits
- **Device-based session tracking** for security monitoring
- **Automatic session cleanup** and expiration handling

### Security Layers
1. **Kong API Gateway** with rate limiting and request validation
2. **Spring Security** with JWT authentication and RBAC
3. **Custom security filters** for device fingerprinting and threat detection
4. **Database-level security** with encrypted sensitive fields
5. **AWS KMS** for enterprise key management

## 🚀 Quick Start

### Prerequisites
- Java 21+
- PostgreSQL 14+
- Redis 6+
- AWS Account (for KMS)
- Kong API Gateway (optional)

### Environment Setup
```bash
# Clone and build
git clone <repository-url>
cd auth-service
./gradlew build

# Database setup
createdb trademaster_auth
./gradlew flywayMigrate

# Redis setup (using Docker)
docker run -d -p 6379:6379 redis:6-alpine

# AWS KMS setup
aws kms create-key --description "TradeMaster encryption key"
```

### Configuration
Update `application.yml` with your environment-specific values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/trademaster_auth
    username: your_db_user
    password: your_db_password
  
  redis:
    host: localhost
    port: 6379
    
trademaster:
  jwt:
    secret: your-256-bit-secret-key-here-change-in-production
    
  aws:
    region: us-east-1
    kms:
      key-id: alias/trademaster-encryption-key
```

### Run the Service
```bash
./gradlew bootRun
```

The service will be available at `http://localhost:8080`

## 📡 API Endpoints

### Public Endpoints
```http
POST /api/v1/auth/register      # User registration
POST /api/v1/auth/login         # User login
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password  
POST /api/v1/auth/verify-email
GET  /api/v1/auth/health        # Health check
```

### Protected Endpoints (Require Authentication)
```http
POST /api/v1/auth/refresh       # Token refresh
POST /api/v1/auth/logout        # User logout
POST /api/v1/auth/mfa/verify    # MFA verification
GET  /api/v1/profile            # User profile
PUT  /api/v1/profile            # Update profile
```

### Rate Limits (via Kong Gateway)
- **Free Tier:** 100/minute, 1,000/hour, 5,000/day
- **Authenticated:** 1,000/minute, 10,000/hour, 50,000/day  
- **Premium:** 5,000/minute, 50,000/hour, 500,000/day

## 🔧 Configuration

### Security Configuration
```yaml
trademaster:
  security:
    rate-limit:
      authenticated: 1000    # requests per minute
      free-tier: 100        # requests per minute
      premium: 5000         # requests per minute
    session:
      max-concurrent: 5     # max sessions per user
      timeout: 1440         # 24 hours in minutes
    
  jwt:
    expiration: 900000      # 15 minutes
    refresh-expiration: 86400000  # 24 hours
```

### Encryption Configuration
```yaml
trademaster:
  encryption:
    algorithm: AES/GCM/NoPadding
    key-length: 256
    
  aws:
    region: us-east-1
    kms:
      key-id: alias/trademaster-encryption-key
```

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./gradlew test

# Integration tests  
./gradlew integrationTest

# Security tests
./gradlew securityTest

# All tests with coverage
./gradlew clean test jacocoTestReport
```

### Test Coverage Targets
- **Unit Tests:** >80% line coverage
- **Integration Tests:** All critical authentication flows
- **Security Tests:** Authentication, authorization, and encryption validation

## 📊 Monitoring & Observability

### Metrics (via Actuator + Prometheus)
- Authentication success/failure rates
- Session creation and expiration metrics  
- MFA verification statistics
- Rate limiting violations
- Encryption/decryption performance
- Database connection pool metrics

### Health Checks (/actuator/health)
- Database connectivity
- Redis connectivity  
- AWS KMS accessibility
- Encryption service functionality

### Logging
- **Audit logs:** All authentication and authorization events
- **Security logs:** Suspicious activities and violations
- **Application logs:** Service operations and errors
- **Performance logs:** Response times and resource usage

## 🔒 Security Best Practices

### Production Deployment
1. **Environment Variables:** Never commit secrets to version control
2. **TLS Everywhere:** Use HTTPS/TLS for all communications
3. **Network Security:** Implement proper firewall rules and VPC isolation
4. **Secret Management:** Use AWS Secrets Manager or similar for production secrets
5. **Database Security:** Enable connection encryption and use dedicated DB users
6. **Container Security:** Use minimal base images and regular security scans

### Key Rotation
```bash
# Rotate JWT signing key
# Update JWT_SECRET environment variable and restart service

# Rotate AWS KMS key  
aws kms schedule-key-deletion --key-id old-key-id --pending-window-in-days 30
# Update KMS_KEY_ID and restart service

# Rotate database passwords
# Update DB credentials and restart service
```

## 📝 Development Notes

### Code Structure
```
src/main/java/com/trademaster/auth/
├── config/          # Configuration classes
├── controller/      # REST controllers  
├── dto/            # Data transfer objects
├── entity/         # JPA entities
├── repository/     # Data access layer
├── security/       # Security components
└── service/        # Business logic
```

### Key Components
- **AuthenticationService:** Core authentication logic
- **JwtTokenProvider:** JWT token generation and validation
- **EncryptionService:** AES-256 encryption with AWS KMS
- **SessionManagementService:** Redis-based session management
- **DeviceFingerprintService:** Device identification and tracking
- **AuditService:** Comprehensive event logging

### Database Migrations
Located in `src/main/resources/db/migration/`:
- `V1__Create_users_table.sql` - Initial schema with users, profiles, roles
- `V2__Create_audit_tables.sql` - Audit and compliance tables

## 🔄 Story Implementation Status

✅ **Completed Features:**
- User registration with email validation
- Secure login with JWT tokens  
- Password policy enforcement
- Device fingerprinting
- Role-based access control (RBAC)
- Redis session management
- AES-256 encryption with AWS KMS
- Kong API Gateway configuration
- Comprehensive database schema
- Audit logging foundation

🔄 **In Progress:**
- Full audit logging implementation
- MFA service integration
- Email verification system

⏳ **Planned:**
- Comprehensive test suite
- Performance optimization
- Advanced threat detection
- Biometric authentication support

## 📞 Support & Maintenance

### Development Team
- **Architecture:** Systems design and security implementation
- **Backend:** API development and database management  
- **DevOps:** Infrastructure and deployment automation
- **QA:** Testing and quality assurance

### Issue Reporting
Report security issues confidentially to: security@trademaster.com  
General issues: Create GitHub issue with detailed reproduction steps

---

**© 2024 TradeMaster Development Team. Financial-grade security for modern trading platforms.**