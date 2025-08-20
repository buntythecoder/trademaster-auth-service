# User Profile Development Specification
## Story 1.2: User Profile Service - CRUD Operations

**Phase**: 1 - Core Authentication & User Management  
**Priority**: High  
**Effort**: 3 weeks  
**Dependencies**: Authentication Service (Story 1.1) ✅ Completed

---

## Overview

Develop a comprehensive user profile management service that allows users to create, read, update, and delete their personal and trading profiles. This service extends beyond basic user information to include trading preferences, risk profiles, and personalization settings specific to the Indian retail trading market.

## Business Requirements

### Functional Requirements

#### FR-1: Profile Information Management
- **FR-1.1**: Users must be able to create and update basic profile information
- **FR-1.2**: Users must be able to manage PAN card and Aadhaar information for KYC compliance
- **FR-1.3**: Users must be able to set trading preferences and risk tolerance
- **FR-1.4**: Users must be able to configure notification preferences
- **FR-1.5**: Users must be able to upload and manage profile photo

#### FR-2: Trading Profile Configuration
- **FR-2.1**: Users must be able to set preferred trading segments (Equity, F&O, Commodity, Currency)
- **FR-2.2**: Users must be able to configure risk management settings
- **FR-2.3**: Users must be able to set default order quantities and types
- **FR-2.4**: Users must be able to manage broker account linkages
- **FR-2.5**: Users must be able to set investment goals and time horizons

#### FR-3: KYC & Compliance
- **FR-3.1**: System must validate PAN card format and checksum
- **FR-3.2**: System must store KYC verification status and documents
- **FR-3.3**: System must implement SEBI compliance checks
- **FR-3.4**: System must maintain audit trails for profile changes
- **FR-3.5**: System must support document upload with size and format validation

### Non-Functional Requirements

#### NFR-1: Performance
- Profile retrieval: < 100ms (95th percentile)
- Profile updates: < 200ms (95th percentile)
- Document uploads: < 5s for 5MB files
- Search operations: < 150ms (95th percentile)

#### NFR-2: Security
- All personal data encrypted at rest (AES-256)
- PII data masked in logs and monitoring
- Secure file storage with virus scanning
- GDPR-compliant data retention policies
- OWASP compliance for file uploads

#### NFR-3: Scalability
- Support 100,000 concurrent users
- Handle 10,000 profile updates/minute
- Horizontal scaling capability
- Database sharding support

#### NFR-4: Availability
- 99.9% uptime SLA
- Graceful degradation during high load
- Zero-downtime deployments
- Real-time monitoring and alerting

---

## Technical Architecture

### System Components

#### 1. User Profile Service (Spring Boot)
```yaml
Technology Stack:
  Framework: Spring Boot 3.2.0
  Language: Java 21
  Database: PostgreSQL 15
  Cache: Redis 7
  Message Queue: RabbitMQ 3.12
  File Storage: MinIO / AWS S3
  
Microservice Pattern:
  Architecture: Hexagonal Architecture
  API Style: RESTful with OpenAPI 3.0
  Event Driven: Domain events for profile changes
  Caching Strategy: Multi-level (L1: Caffeine, L2: Redis)
```

#### 2. Database Design
```sql
-- Core profile tables
user_profiles (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth_users(id),
  personal_info JSONB NOT NULL,
  trading_preferences JSONB NOT NULL,
  kyc_information JSONB NOT NULL,
  notification_settings JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  version INTEGER DEFAULT 1
);

-- Document management
user_documents (
  id UUID PRIMARY KEY,
  user_profile_id UUID NOT NULL REFERENCES user_profiles(id),
  document_type VARCHAR(50) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  verification_status VARCHAR(20) DEFAULT 'pending',
  uploaded_at TIMESTAMP DEFAULT NOW()
);

-- Trading preferences
trading_profiles (
  id UUID PRIMARY KEY,
  user_profile_id UUID NOT NULL REFERENCES user_profiles(id),
  segment_preferences JSONB NOT NULL,
  risk_profile JSONB NOT NULL,
  default_settings JSONB NOT NULL,
  broker_configurations JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Audit trail
profile_audit_logs (
  id UUID PRIMARY KEY,
  user_profile_id UUID NOT NULL REFERENCES user_profiles(id),
  changed_by UUID NOT NULL REFERENCES auth_users(id),
  change_type VARCHAR(50) NOT NULL,
  old_values JSONB,
  new_values JSONB,
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### 3. API Design

##### Core Profile Endpoints
```yaml
GET /api/v1/profiles/me:
  summary: Get current user's profile
  responses:
    200: UserProfileResponse
    404: Profile not found

POST /api/v1/profiles:
  summary: Create user profile
  requestBody: CreateProfileRequest
  responses:
    201: UserProfileResponse
    400: Validation errors

PUT /api/v1/profiles/me:
  summary: Update user profile
  requestBody: UpdateProfileRequest
  responses:
    200: UserProfileResponse
    400: Validation errors
    409: Version conflict

PATCH /api/v1/profiles/me/trading-preferences:
  summary: Update trading preferences
  requestBody: TradingPreferencesRequest
  responses:
    200: TradingPreferencesResponse

GET /api/v1/profiles/me/documents:
  summary: List user documents
  responses:
    200: DocumentListResponse

POST /api/v1/profiles/me/documents:
  summary: Upload document
  requestBody: multipart/form-data
  responses:
    201: DocumentResponse
    400: File validation errors

DELETE /api/v1/profiles/me/documents/{documentId}:
  summary: Delete document
  responses:
    204: No content
    404: Document not found
```

##### KYC & Verification Endpoints
```yaml
GET /api/v1/profiles/me/kyc-status:
  summary: Get KYC verification status
  responses:
    200: KYCStatusResponse

POST /api/v1/profiles/me/kyc/verify-pan:
  summary: Verify PAN card
  requestBody: PANVerificationRequest
  responses:
    200: VerificationResponse

POST /api/v1/profiles/me/kyc/verify-aadhaar:
  summary: Verify Aadhaar (via UIDAI APIs)
  requestBody: AadhaarVerificationRequest
  responses:
    200: VerificationResponse
```

#### 4. Data Models

##### UserProfile Entity
```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Type(JsonType.class)
    @Column(name = "personal_info", columnDefinition = "jsonb")
    private PersonalInformation personalInfo;
    
    @Type(JsonType.class)
    @Column(name = "trading_preferences", columnDefinition = "jsonb")
    private TradingPreferences tradingPreferences;
    
    @Type(JsonType.class)
    @Column(name = "kyc_information", columnDefinition = "jsonb")
    private KYCInformation kycInfo;
    
    @Version
    private Integer version;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}
```

##### PersonalInformation Value Object
```java
public record PersonalInformation(
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String mobileNumber,
    String emailAddress,
    Address address,
    String panNumber,
    String aadhaarNumber,
    String profilePhotoUrl,
    String occupation,
    String annualIncome,
    String educationLevel,
    String tradingExperience
) {
    // Validation methods and business logic
}
```

##### TradingPreferences Value Object
```java
public record TradingPreferences(
    Set<TradingSegment> preferredSegments,
    RiskProfile riskProfile,
    DefaultOrderSettings defaultOrderSettings,
    List<BrokerConfiguration> brokerConfigs,
    NotificationPreferences notifications,
    Map<String, Object> customSettings
) {}

public enum TradingSegment {
    EQUITY, FUTURES_OPTIONS, COMMODITY, CURRENCY, MUTUAL_FUNDS
}
```

---

## Implementation Plan

### Phase 1: Core Profile Service (Week 1-2)

#### Sprint 1.1: Foundation & Basic CRUD (5 days)
**Day 1-2: Project Setup**
- Create Spring Boot microservice project
- Set up database schema with Flyway migrations
- Configure Redis caching and RabbitMQ messaging
- Implement base entities and repositories

**Day 3-5: Basic CRUD Operations**
- Implement UserProfile service layer
- Create REST controllers for basic operations
- Add request/response DTOs and validation
- Implement unit and integration tests

#### Sprint 1.2: Advanced Features (5 days)
**Day 6-8: Document Management**
- Implement file upload with validation
- Set up MinIO/S3 integration for file storage
- Add virus scanning with ClamAV integration
- Implement document CRUD operations

**Day 9-10: Caching & Performance**
- Implement multi-level caching strategy
- Add database query optimization
- Set up monitoring with Prometheus metrics
- Performance testing and optimization

### Phase 2: KYC & Compliance (Week 2-3)

#### Sprint 2.1: KYC Integration (5 days)
**Day 11-13: PAN & Aadhaar Verification**
- Integrate PAN verification APIs
- Implement Aadhaar verification (mock/sandbox)
- Add KYC status management
- Implement compliance audit logging

**Day 14-15: Security & Compliance**
- Implement data encryption for PII
- Add GDPR compliance features
- Set up comprehensive audit trails
- Security testing and vulnerability scanning

### Phase 3: Integration & Testing (Week 3)

#### Sprint 3.1: Integration & Deployment (5 days)
**Day 16-18: Service Integration**
- Integrate with authentication service
- Set up Kong API Gateway routing
- Implement event publishing for profile changes
- Add health checks and monitoring

**Day 19-20: Testing & Documentation**
- Comprehensive integration testing
- Performance testing with load scenarios
- Complete API documentation with examples
- User acceptance testing scenarios

---

## Testing Strategy

### Unit Testing
- Service layer testing with 90%+ coverage
- Repository layer testing with test containers
- Validation logic testing for all DTOs
- Domain model testing for business rules

### Integration Testing
- API endpoint testing with @SpringBootTest
- Database integration with test containers
- Redis caching behavior testing
- File upload/download integration testing

### Performance Testing
- Load testing with 10,000 concurrent users
- Stress testing for profile update scenarios
- Memory usage profiling for large datasets
- Database connection pool optimization

### Security Testing
- Authentication and authorization testing
- Input validation and sanitization testing
- File upload security testing
- SQL injection and XSS prevention testing

---

## Monitoring & Observability

### Metrics Collection
```yaml
Business Metrics:
  - profile_creation_rate
  - profile_update_frequency
  - kyc_completion_rate
  - document_upload_success_rate
  - document_verification_time

Technical Metrics:
  - api_request_duration
  - database_query_time
  - cache_hit_ratio
  - file_upload_throughput
  - error_rate_by_endpoint

Infrastructure Metrics:
  - jvm_memory_usage
  - database_connection_pool
  - redis_memory_usage
  - disk_storage_utilization
```

### Alerting Rules
```yaml
Critical Alerts:
  - API response time > 500ms (95th percentile)
  - Error rate > 1% for 5 minutes
  - Database connection pool exhaustion
  - File storage disk usage > 85%

Warning Alerts:
  - Cache hit ratio < 80%
  - Profile update failures > 0.5%
  - KYC verification API failures
  - Document upload size exceeded
```

---

## Security Considerations

### Data Protection
- **Encryption**: AES-256 for PII data at rest
- **Masking**: PII data masked in logs and monitoring
- **Access Control**: Role-based access with fine-grained permissions
- **Data Retention**: Automated data purging per GDPR requirements

### API Security
- **Rate Limiting**: 1000 requests/minute per user (basic tier)
- **Input Validation**: Comprehensive validation for all inputs
- **File Upload Security**: Virus scanning, size limits, type validation
- **SQL Injection Prevention**: Parameterized queries and ORM usage

### Compliance Requirements
- **SEBI Compliance**: KYC data retention for 7 years
- **GDPR Compliance**: Data portability and right to deletion
- **PCI DSS**: If handling payment card information
- **ISO 27001**: Information security management standards

---

## Success Metrics

### User Experience Metrics
- Profile completion rate: > 95%
- Profile update success rate: > 99.5%
- Average profile load time: < 100ms
- Document upload success rate: > 98%

### Technical Performance Metrics
- API availability: 99.9%
- Response time SLA compliance: > 95%
- Cache hit ratio: > 85%
- Zero data loss incidents

### Business Impact Metrics
- KYC completion time reduction: 60%
- User onboarding time reduction: 40%
- Support tickets reduction: 50%
- User profile engagement: > 80%

---

## Risk Assessment & Mitigation

### High-Risk Items
1. **PII Data Security**: Comprehensive encryption and access controls
2. **KYC API Reliability**: Circuit breakers and fallback mechanisms
3. **File Upload Vulnerabilities**: Multi-layer security scanning
4. **Database Performance**: Query optimization and connection pooling

### Mitigation Strategies
1. **Security**: Regular security audits and penetration testing
2. **Performance**: Continuous monitoring and automated scaling
3. **Compliance**: Regular compliance audits and legal review
4. **Data Loss**: Automated backups and disaster recovery procedures

---

## Next Steps After Completion

1. **Profile Analytics Service** - User behavior analysis and insights
2. **Preference Learning Engine** - AI-driven preference recommendations
3. **Social Trading Features** - Connect profiles with social trading
4. **Advanced KYC Features** - Video KYC and biometric verification
5. **Portfolio Integration** - Link profiles with trading portfolio data

This specification provides a comprehensive foundation for implementing a robust, secure, and scalable user profile management service that meets both technical requirements and regulatory compliance needs for the Indian retail trading market.