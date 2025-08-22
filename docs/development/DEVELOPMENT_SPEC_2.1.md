# Development Specification: Epic 2.1 Implementation

## Overview

This document provides detailed development specifications for implementing Epic 2.1 - Backend Implementation Gap Analysis. The implementation will bridge the gap between sophisticated frontend components and backend APIs to create a fully integrated trading platform.

## Development Approach

### 1. Implementation Strategy
- **Iterative Development**: Implement stories incrementally for faster feedback
- **API-First Design**: Complete backend APIs before frontend integration
- **Test-Driven Development**: Write tests before implementation
- **Progressive Enhancement**: Enhance existing components rather than rebuilding

### 2. Technology Stack Alignment
- **Backend**: Java 24 + Spring Boot 3.5.x + Virtual Threads
- **Frontend**: React 18 + TypeScript + Zustand  
- **Database**: PostgreSQL 17+ with optimized indexes
- **Real-time**: WebSocket connections
- **Caching**: Redis for session and real-time data

### 3. Development Priorities
1. **Story 2.1.1**: Authentication Service Integration (Critical - Foundation)
2. **Story 2.1.2**: Market Data Service Integration (High - User Engagement)
3. **Story 2.1.3**: Portfolio Analytics Frontend (High - Business Value)
4. **Story 2.1.6**: Risk Management Integration (High - Compliance)
5. **Story 2.1.4**: Trading Service Frontend (Medium - Enhancement)
6. **Story 2.1.5**: Document & Profile Management (Medium - UX)

## Implementation Plan

### Phase 1: Foundation (Weeks 1-2)
**Story 2.1.1 - Authentication Service Integration**

#### Backend Implementation

**1. Database Schema Setup**
```sql
-- Create MFA tables
CREATE TABLE mfa_configuration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    mfa_type VARCHAR(20) NOT NULL,
    secret_key VARCHAR(255),
    enabled BOOLEAN DEFAULT false,
    backup_codes TEXT[],
    last_used TIMESTAMP,
    failed_attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, mfa_type)
);

-- Create device trust tables
CREATE TABLE user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50) NOT NULL,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    user_agent TEXT,
    ip_address INET,
    location VARCHAR(255),
    trusted BOOLEAN DEFAULT false,
    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP,
    trust_expiry TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, device_fingerprint)
);

-- Create security audit logs
CREATE TABLE security_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(50),
    session_id VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    ip_address INET,
    user_agent TEXT,
    location VARCHAR(255),
    metadata JSONB,
    risk_level VARCHAR(20),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user sessions
CREATE TABLE user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    device_fingerprint VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true,
    attributes JSONB
);

-- Create indexes for performance
CREATE INDEX idx_mfa_config_user_id ON mfa_configuration(user_id);
CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_fingerprint ON user_devices(device_fingerprint);
CREATE INDEX idx_audit_logs_user_id ON security_audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON security_audit_logs(timestamp);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(active);
```

**2. Service Implementation Structure**
```
auth-service/src/main/java/com/trademaster/auth/
├── service/
│   ├── MfaService.java               # MFA operations
│   ├── DeviceTrustService.java       # Device management
│   ├── SecurityAuditService.java     # Security logging
│   ├── SessionManagementService.java # Session handling
│   └── EncryptionService.java        # Crypto operations
├── entity/
│   ├── MfaConfiguration.java
│   ├── UserDevice.java
│   ├── SecurityAuditLog.java
│   └── UserSession.java
├── repository/
│   ├── MfaConfigurationRepository.java
│   ├── UserDeviceRepository.java
│   ├── SecurityAuditLogRepository.java
│   └── UserSessionRepository.java
├── dto/
│   ├── MfaSetupResponse.java
│   ├── DeviceTrustRequest.java
│   └── SecurityEventDto.java
└── controller/
    ├── MfaController.java
    ├── DeviceController.java
    └── SecurityController.java
```

**3. Key Backend Components**

**MfaService.java** - Core MFA functionality
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {
    private final MfaConfigurationRepository mfaRepository;
    private final GoogleAuthenticator googleAuthenticator;
    private final SecurityAuditService auditService;
    
    public MfaSetupResponse initiateMfaSetup(String userId, MfaType type);
    public boolean verifyMfaSetup(String userId, String code);
    public boolean verifyMfaLogin(String userId, String code);
    public List<String> regenerateBackupCodes(String userId);
    public void disableMfa(String userId, MfaType type);
}
```

**DeviceTrustService.java** - Device fingerprinting and trust
```java
@Service
@RequiredArgsConstructor
public class DeviceTrustService {
    private final UserDeviceRepository deviceRepository;
    
    public String generateDeviceFingerprint(HttpServletRequest request);
    public boolean isDeviceTrusted(String userId, String fingerprint);
    public void trustDevice(String userId, String fingerprint, int days);
    public List<UserDevice> getUserDevices(String userId);
    public void revokeDevice(String userId, String deviceId);
}
```

#### Frontend Integration

**4. Service Integration**
```typescript
// src/services/authService.ts
export class AuthService {
    async setupMfa(type: 'TOTP' | 'SMS' | 'EMAIL'): Promise<MfaSetupResponse> {
        const response = await apiClient.post('/api/v1/auth/mfa/setup', { type });
        return response.data;
    }
    
    async verifyMfaSetup(code: string): Promise<boolean> {
        const response = await apiClient.post('/api/v1/auth/mfa/verify-setup', { code });
        return response.data.success;
    }
    
    async getTrustedDevices(): Promise<UserDevice[]> {
        const response = await apiClient.get('/api/v1/auth/devices');
        return response.data;
    }
    
    async getSecurityLogs(filters: SecurityLogFilters): Promise<SecurityEvent[]> {
        const response = await apiClient.get('/api/v1/auth/security/logs', { params: filters });
        return response.data;
    }
}
```

**5. Component Enhancement**
```typescript
// src/components/auth/MFASetup.tsx
export function MFASetup() {
    const [mfaType, setMfaType] = useState<'TOTP' | 'SMS' | 'EMAIL'>('TOTP');
    const [setupData, setSetupData] = useState<MfaSetupResponse>();
    const [verificationCode, setVerificationCode] = useState('');
    
    const handleSetupMfa = async () => {
        try {
            const response = await authService.setupMfa(mfaType);
            setSetupData(response);
        } catch (error) {
            handleError(error);
        }
    };
    
    const handleVerifySetup = async () => {
        try {
            const success = await authService.verifyMfaSetup(verificationCode);
            if (success) {
                showSuccess('MFA setup completed successfully');
                onSetupComplete();
            } else {
                showError('Invalid verification code');
            }
        } catch (error) {
            handleError(error);
        }
    };
    
    // Component JSX implementation...
}
```

### Phase 2: Market Data (Weeks 3-4)
**Story 2.1.2 - Market Data Service Integration**

#### Implementation Approach

**1. WebSocket Infrastructure**
```java
// Enhanced WebSocket configuration
@Configuration
@EnableWebSocket
public class MarketDataWebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MarketDataWebSocketHandler(), "/ws/market-data")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

@Component
public class MarketDataWebSocketHandler extends TextWebSocketHandler {
    private final MarketDataService marketDataService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle subscription requests
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.get("action").asText();
        
        if ("subscribe".equals(action)) {
            String symbol = json.get("symbol").asText();
            subscribeToSymbol(session, symbol);
        }
    }
}
```

**2. Market Scanner Implementation**
```java
@Service
public class MarketScannerService {
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void runActiveScans() {
        List<MarketScanner> activeScans = scannerRepository.findByActiveTrue();
        
        activeScans.parallelStream().forEach(scanner -> {
            try {
                List<ScanResult> results = executeScan(scanner);
                notifySubscribers(scanner.getUserId(), results);
            } catch (Exception e) {
                log.error("Failed to execute scan: {}", scanner.getId(), e);
            }
        });
    }
    
    public List<ScanResult> executeScan(MarketScanner scanner) {
        // Apply filtering criteria to market data
        return marketDataRepository.findByCustomCriteria(scanner.getCriteria());
    }
}
```

**3. Frontend WebSocket Integration**
```typescript
// src/services/marketDataWebSocket.ts
export class MarketDataWebSocketService {
    private ws: WebSocket | null = null;
    private subscriptions: Map<string, Set<(data: MarketDataUpdate) => void>> = new Map();
    
    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.ws = new WebSocket(`${WS_BASE_URL}/ws/market-data`);
            
            this.ws.onopen = () => resolve();
            this.ws.onerror = (error) => reject(error);
            this.ws.onmessage = (event) => {
                const data = JSON.parse(event.data) as MarketDataUpdate;
                this.handleUpdate(data);
            };
        });
    }
    
    subscribe(symbol: string, callback: (data: MarketDataUpdate) => void): void {
        if (!this.subscriptions.has(symbol)) {
            this.subscriptions.set(symbol, new Set());
            this.ws?.send(JSON.stringify({ action: 'subscribe', symbol }));
        }
        this.subscriptions.get(symbol)?.add(callback);
    }
    
    private handleUpdate(data: MarketDataUpdate): void {
        const callbacks = this.subscriptions.get(data.symbol);
        callbacks?.forEach(callback => callback(data));
    }
}
```

### Phase 3: Portfolio & Risk (Weeks 5-6)
**Stories 2.1.3 & 2.1.6 - Portfolio Analytics & Risk Management**

#### Implementation Structure

**1. Enhanced Portfolio Service**
```java
@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
    
    @GetMapping("/{portfolioId}/performance")
    public ResponseEntity<PerformanceComparison> getPerformanceComparison(
            @PathVariable UUID portfolioId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "SPY") String benchmark) {
        
        PerformanceComparison performance = analyticsService.comparePerformance(
            portfolioId, startDate, endDate, benchmark);
        return ResponseEntity.ok(performance);
    }
    
    @PostMapping("/{portfolioId}/risk/assess")
    public ResponseEntity<List<RiskAlert>> assessRisk(
            @PathVariable UUID portfolioId,
            @RequestBody RiskAssessmentRequest request) {
        
        List<RiskAlert> alerts = riskService.assessRisk(portfolioId, request);
        return ResponseEntity.ok(alerts);
    }
}
```

**2. Real-time Risk Monitoring**
```java
@Service
public class RiskMonitoringService {
    
    @EventListener
    public void handleMarketDataUpdate(MarketDataUpdate update) {
        // Get portfolios with positions in updated symbol
        List<Portfolio> affectedPortfolios = portfolioService.getPortfoliosWithSymbol(update.getSymbol());
        
        affectedPortfolios.parallelStream().forEach(portfolio -> {
            RiskMetrics newRisk = calculateRiskMetrics(portfolio, update);
            checkRiskLimits(portfolio, newRisk);
        });
    }
    
    @Async
    public void checkRiskLimits(Portfolio portfolio, RiskMetrics risk) {
        RiskLimitConfiguration limits = riskService.getRiskLimits(portfolio.getId());
        
        List<RiskAlert> alerts = new ArrayList<>();
        
        if (risk.getValueAtRisk() > limits.getMaxVaR()) {
            alerts.add(createRiskAlert(portfolio, "VAR_BREACH", risk.getValueAtRisk(), limits.getMaxVaR()));
        }
        
        if (!alerts.isEmpty()) {
            riskAlertService.sendAlerts(alerts);
        }
    }
}
```

### Phase 4: Trading & Profile (Weeks 7-8)
**Stories 2.1.4 & 2.1.5 - Trading Service & Profile Management**

#### Implementation Approach

**1. Real-time Order Updates**
```java
@Component
public class OrderEventListener {
    
    @EventListener
    public void handleOrderUpdate(OrderUpdateEvent event) {
        // Send real-time update to WebSocket clients
        webSocketService.sendOrderUpdate(event.getUserId(), event.getOrderUpdate());
        
        // Update order analytics
        orderAnalyticsService.updateMetrics(event.getOrder());
        
        // Check for alerts
        if (event.getOrderUpdate().getStatus() == OrderStatus.FILLED) {
            alertService.sendOrderFillNotification(event.getUserId(), event.getOrder());
        }
    }
}
```

**2. Document Upload Service**
```java
@Service
public class DocumentService {
    
    @Value("${minio.bucket.documents}")
    private String documentsBucket;
    
    private final MinioClient minioClient;
    
    public DocumentResponse uploadDocument(String userId, DocumentUploadRequest request) {
        try {
            // Validate file type and size
            validateDocument(request.getFile());
            
            // Generate unique filename
            String filename = generateDocumentFilename(userId, request.getDocumentType(), request.getFile());
            
            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(filename)
                    .stream(request.getFile().getInputStream(), request.getFile().getSize(), -1)
                    .contentType(request.getFile().getContentType())
                    .build()
            );
            
            // Save metadata to database
            UserDocument document = UserDocument.builder()
                .userId(userId)
                .documentType(request.getDocumentType())
                .filename(filename)
                .originalFilename(request.getFile().getOriginalFilename())
                .fileSize(request.getFile().getSize())
                .contentType(request.getFile().getContentType())
                .verificationStatus(VerificationStatus.PENDING)
                .uploadDate(LocalDateTime.now())
                .build();
            
            document = documentRepository.save(document);
            
            // Trigger verification process
            documentVerificationService.initiateVerification(document);
            
            return mapToDocumentResponse(document);
            
        } catch (Exception e) {
            log.error("Failed to upload document for user: {}", userId, e);
            throw new FileUploadException("Failed to upload document", e);
        }
    }
}
```

## Development Standards

### 1. Code Quality Standards
- **Test Coverage**: Minimum 80% line coverage
- **Code Review**: All code must be reviewed by at least 2 developers
- **Documentation**: All public APIs must be documented
- **Logging**: Structured logging with appropriate levels

### 2. Performance Requirements
- **API Response Time**: <200ms for 95th percentile
- **Real-time Updates**: <100ms latency
- **Database Queries**: <50ms for simple queries
- **File Upload**: Support up to 10MB files

### 3. Security Standards
- **Authentication**: JWT with refresh tokens
- **Authorization**: Role-based access control
- **Data Encryption**: At rest and in transit
- **Input Validation**: All inputs validated and sanitized

### 4. Testing Strategy
- **Unit Tests**: JUnit 5 for backend, Jest for frontend
- **Integration Tests**: TestContainers for database testing
- **E2E Tests**: Playwright for user workflows
- **Performance Tests**: JMeter for load testing

## Deployment Strategy

### 1. Database Migrations
```sql
-- Migration scripts will be versioned (V1__initial_schema.sql, V2__add_mfa.sql, etc.)
-- Use Flyway for automated migrations
-- All migrations must be backward compatible
```

### 2. Feature Flags
```java
@Component
public class FeatureFlags {
    
    @Value("${features.mfa.enabled:false}")
    private boolean mfaEnabled;
    
    @Value("${features.real-time-risk.enabled:false}")
    private boolean realTimeRiskEnabled;
    
    public boolean isMfaEnabled() { return mfaEnabled; }
    public boolean isRealTimeRiskEnabled() { return realTimeRiskEnabled; }
}
```

### 3. Environment Configuration
- **Development**: Full feature set enabled
- **Staging**: Production-like configuration
- **Production**: Gradual rollout with feature flags

## Monitoring & Observability

### 1. Metrics Collection
- **Application Metrics**: Micrometer + Prometheus
- **Business Metrics**: Custom metrics for trading activities
- **Performance Metrics**: Response times, error rates
- **Security Metrics**: Authentication failures, suspicious activities

### 2. Logging Strategy
- **Structured Logging**: JSON format with correlation IDs
- **Log Aggregation**: Centralized logging with ELK stack
- **Security Logging**: All security events logged
- **Performance Logging**: Slow query detection

### 3. Alerting Rules
- **Error Rate**: >1% error rate for 5 minutes
- **Response Time**: >500ms for 95th percentile
- **Security**: Failed authentication attempts
- **Business**: Trading system unavailability

## Success Criteria

### 1. Technical Metrics
- All integration tests passing
- Performance benchmarks met
- Security scan results clean
- Code coverage >80%

### 2. Business Metrics
- User engagement increase >40%
- Feature adoption rate >70%
- Support ticket reduction >30%
- User satisfaction >4.5/5

### 3. Operational Metrics
- Deployment success rate >99%
- System uptime >99.9%
- Mean time to recovery <15 minutes
- Zero security incidents

This development specification provides the foundation for implementing Epic 2.1. Each phase builds upon the previous one, ensuring a solid foundation while delivering incremental value to users.