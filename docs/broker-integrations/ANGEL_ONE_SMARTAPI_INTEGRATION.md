# Angel One SmartAPI Integration Guide

## Overview

This document provides comprehensive implementation details for Angel One SmartAPI integration in the TradeMaster platform, following enterprise-grade security patterns and functional programming principles.

## API Endpoints and Base URLs

### Production Environment
- **Base URL**: `https://apiconnect.angelbroking.com`
- **WebSocket**: `wss://smartapisocket.angelone.in/smart-stream`

### Sandbox Environment  
- **Base URL**: `https://apiconnect.angelone.in/rest` (Testing)

### Key API Endpoints

| Endpoint | Method | Purpose | Rate Limit |
|----------|--------|---------|------------|
| `/rest/auth/angelbroking/user/v1/loginByPassword` | POST | Password-based login | 10/min |
| `/rest/auth/angelbroking/jwt/v1/generateTokens` | POST | TOTP verification & JWT generation | 5/min |
| `/rest/secure/angelbroking/user/v1/logout` | POST | Session logout | 10/min |
| `/rest/secure/angelbroking/portfolio/v1/getAllHolding` | GET | Portfolio holdings | 100/min |
| `/rest/secure/angelbroking/order/v1/placeOrder` | POST | Place orders | 50/min |

## Authentication Flow

### 1. Password-Based Login (Step 1)

```http
POST /rest/auth/angelbroking/user/v1/loginByPassword
Content-Type: application/json
X-ApiKey: your-api-key
X-ClientLocalIP: 192.168.1.1
X-ClientLocalMACAddress: 00:00:00:00:00:00
X-ClientPublicIP: 106.51.74.76
X-SourceID: WEB
X-UserType: USER

{
    "clientcode": "A123456",
    "password": "your-password",
    "totp": ""
}
```

**Response:**
```json
{
    "status": true,
    "message": "Login Successful",
    "errorcode": "",
    "data": {
        "jwtToken": "",
        "refreshToken": "",
        "feedToken": ""
    }
}
```

### 2. TOTP Verification (Step 2)

```http
POST /rest/auth/angelbroking/jwt/v1/generateTokens
Content-Type: application/json
X-ApiKey: your-api-key
X-ClientLocalIP: 192.168.1.1
X-ClientLocalMACAddress: 00:00:00:00:00:00
X-ClientPublicIP: 106.51.74.76
X-SourceID: WEB
X-UserType: USER

{
    "clientcode": "A123456",
    "totp": "123456"
}
```

**Response:**
```json
{
    "status": true,
    "message": "SUCCESS",
    "errorcode": "",
    "data": {
        "jwtToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
        "feedToken": "feedtoken123456"
    }
}
```

### 3. JWT Token Refresh

```http
POST /rest/auth/angelbroking/jwt/v1/generateTokens
Content-Type: application/json
X-ApiKey: your-api-key
Authorization: Bearer refresh-token

{
    "refreshToken": "your-refresh-token"
}
```

## Headers and Authentication Requirements

### Required Headers

| Header | Value | Description | Required |
|--------|-------|-------------|----------|
| `Content-Type` | `application/json` | Request content type | ✅ |
| `X-ApiKey` | Your API Key | Application identifier | ✅ |
| `X-ClientLocalIP` | Client IP (e.g., 192.168.1.1) | Local IP address | ✅ |
| `X-ClientLocalMACAddress` | MAC Address | Client MAC address | ✅ |
| `X-ClientPublicIP` | Public IP | Internet-facing IP | ✅ |
| `X-SourceID` | WEB/MOBILE/API | Request source | ✅ |
| `X-UserType` | USER | User type | ✅ |
| `Authorization` | Bearer JWT | JWT token for secure endpoints | For secured APIs |

### Authentication Token Usage

After successful authentication, use the JWT token in the Authorization header:
```http
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

## Java Implementation Details

### 1. Service Configuration

```yaml
# application.yml
broker:
  angel-one:
    name: "Angel One SmartAPI"
    api-url: "https://apiconnect.angelbroking.com"
    client-code: ${ANGEL_CLIENT_CODE:}
    password: ${ANGEL_PASSWORD:}
    api-key: ${ANGEL_API_KEY:}
    totp-secret: ${ANGEL_TOTP_SECRET:}
    rate-limits:
      per-second: 25
      per-minute: 200
      per-day: 100000
    session-validity: 43200  # 12 hours in seconds

# Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    instances:
      angel-one-api:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 3000ms
        minimum-number-of-calls: 10
        sliding-window-size: 20
        wait-duration-in-open-state: 45s
        
  ratelimiter:
    instances:
      angel-one-api:
        limit-for-period: 20
        limit-refresh-period: 1s
        timeout-duration: 5s
```

### 2. Service Implementation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AngelOneApiService implements BrokerApiService {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${broker.angel-one.api-url}")
    private String apiUrl;
    
    @Value("${broker.angel-one.client-code}")
    private String clientCode;
    
    @Value("${broker.angel-one.api-key}")
    private String apiKey;
    
    @Value("${broker.angel-one.password}")
    private String password;
    
    @Override
    public CompletableFuture<AuthResponse> authenticate(AuthRequest request) {
        return CompletableFuture
            .supplyAsync(() -> performAuthentication(request),
                        Executors.newVirtualThreadPerTaskExecutor())
            .handle(this::handleAuthenticationResult);
    }
    
    private AuthResponse performAuthentication(AuthRequest request) {
        return switch (determineAuthMethod(request)) {
            case PASSWORD_LOGIN -> authenticateWithPassword(request);
            case TOTP_VERIFICATION -> authenticateWithTotp(request);
            case JWT_REFRESH -> authenticateWithJwtRefresh(request);
            case INVALID -> createFailureResponse("Invalid authentication parameters");
        };
    }
}
```

### 3. Request/Response Formats

#### Login Payload
```java
private record LoginPayload(
    String clientcode,
    String password,
    String totp
) {}
```

#### TOTP Payload
```java
private record TotpPayload(
    String clientcode,
    String totp
) {}
```

#### Token Refresh Payload
```java
private record RefreshPayload(
    String refreshToken
) {}
```

## Error Handling Patterns

### Common Error Codes

| Error Code | Message | Description | Action |
|------------|---------|-------------|---------|
| `AG8001` | Invalid Login Credentials | Wrong username/password | Retry with correct credentials |
| `AG8002` | Invalid TOTP | Wrong TOTP code | Generate new TOTP |
| `AG8003` | Session Expired | JWT token expired | Refresh token or re-authenticate |
| `AG8004` | Rate Limit Exceeded | Too many requests | Implement exponential backoff |
| `AG8005` | IP Not Whitelisted | IP address not allowed | Contact support |

### Error Response Format
```json
{
    "status": false,
    "message": "Invalid Login Credentials",
    "errorcode": "AG8001",
    "data": null
}
```

### Functional Error Handling
```java
private AuthResponse processApiResponse(Response response, String operation) throws IOException {
    String responseBody = response.body().string();
    
    if (!response.isSuccessful()) {
        log.error("Angel One {} failed: {}", operation, responseBody);
        return createFailureResponse(String.format("%s failed: %s", operation, responseBody));
    }
    
    return parseSuccessResponse(responseBody)
        .map(this::createSuccessResponse)
        .orElse(createFailureResponse("Invalid response format"));
}

private Optional<JsonNode> parseSuccessResponse(String responseBody) {
    try {
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        return jsonResponse.get("status").asBoolean() 
            ? Optional.of(jsonResponse.get("data"))
            : Optional.empty();
    } catch (Exception e) {
        log.error("Failed to parse Angel One API response", e);
        return Optional.empty();
    }
}
```

## Token Management

### JWT Token Structure
- **Access Token**: Used for API calls, expires in 12 hours
- **Refresh Token**: Used to refresh access token, longer validity
- **Feed Token**: Used for WebSocket streaming data

### Token Refresh Strategy
```java
private CompletableFuture<AuthResponse> refreshTokenIfNeeded(String currentToken) {
    return CompletableFuture
        .supplyAsync(() -> {
            if (isTokenNearExpiry(currentToken)) {
                return refreshToken(extractRefreshToken(currentToken));
            }
            return CompletableFuture.completedFuture(currentResponse);
        }, Executors.newVirtualThreadPerTaskExecutor())
        .thenCompose(Function.identity());
}
```

## Rate Limiting Implementation

### Rate Limits
- **Per Second**: 25 requests
- **Per Minute**: 200 requests  
- **Per Day**: 100,000 requests

### Implementation with Resilience4j
```java
@Component
public class AngelOneRateLimiter {
    
    @RateLimiter(name = "angel-one-api")
    public CompletableFuture<ApiResponse> callApi(ApiRequest request) {
        return makeApiCall(request);
    }
}
```

## Security Considerations

### 1. Credential Management
- Store API credentials in environment variables
- Use HashiCorp Vault for production secrets
- Implement credential rotation strategies

### 2. IP Whitelisting
- Register your server IP addresses with Angel One
- Handle IP change scenarios gracefully
- Implement fallback mechanisms

### 3. TOTP Management
- Securely store TOTP secrets
- Implement time synchronization
- Handle TOTP generation and validation

```java
@Component
public class TotpGenerator {
    
    public String generateTotp(String secret) {
        return TimeBasedOneTimePasswordGenerator
            .generateCurrentTimeBasedPassword(
                Base32.decode(secret),
                Duration.ofSeconds(30)
            );
    }
}
```

## Testing Strategy

### 1. Unit Tests
```java
@Test
void shouldAuthenticateWithTotpSuccessfully() throws Exception {
    // Given
    String mockTotpResponse = createMockSuccessResponse();
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(mockTotpResponse));
    
    AuthRequest request = createTotpAuthRequest();
    
    // When
    CompletableFuture<AuthResponse> future = angelOneApiService.authenticate(request);
    AuthResponse response = future.get(10, TimeUnit.SECONDS);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.success()).isTrue();
    assertThat(response.accessToken()).isNotNull();
}
```

### 2. Integration Tests
```java
@SpringBootTest
@Testcontainers
class AngelOneApiServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Test
    void shouldCompleteFullAuthenticationFlow() {
        // Test complete authentication flow with real-like scenarios
    }
}
```

## Performance Considerations

### 1. Virtual Threads Usage
- All I/O operations use Virtual Threads
- Optimal for high-concurrency scenarios
- Reduced memory footprint

### 2. Connection Pooling
```java
@Bean
public OkHttpClient angelOneHttpClient() {
    return new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build();
}
```

### 3. Caching Strategy
```java
@Cacheable(value = "angel-one-tokens", key = "#userId")
public CompletableFuture<AuthResponse> getOrRefreshToken(String userId) {
    // Implementation with Redis caching
}
```

## Monitoring and Observability

### 1. Metrics Collection
```java
@Component
public class AngelOneMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter authenticationCounter;
    private final Timer authenticationTimer;
    
    public AngelOneMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.authenticationCounter = Counter.builder("angel_one_auth_total")
            .description("Total Angel One authentication attempts")
            .register(meterRegistry);
        this.authenticationTimer = Timer.builder("angel_one_auth_duration")
            .description("Angel One authentication duration")
            .register(meterRegistry);
    }
}
```

### 2. Health Checks
```java
@Component
public class AngelOneHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        return isAngelOneApiHealthy() 
            ? Health.up().withDetail("status", "Connected").build()
            : Health.down().withDetail("status", "Disconnected").build();
    }
}
```

## Best Practices

### 1. Functional Programming Patterns
- Use `CompletableFuture` for async operations
- Implement Result/Either types for error handling
- Avoid mutable state and side effects

### 2. Security Patterns  
- Implement zero-trust security model
- Use SecurityFacade for external access
- Audit all authentication attempts

### 3. Resilience Patterns
- Circuit breaker for external API calls
- Exponential backoff for retries
- Graceful degradation strategies

### 4. Code Quality
- Follow SOLID principles
- Maintain cognitive complexity < 7
- Use records for immutable data structures

## Deployment Configuration

### Environment Variables
```bash
# Angel One Configuration
ANGEL_CLIENT_CODE=your-client-code
ANGEL_PASSWORD=your-password  
ANGEL_API_KEY=your-api-key
ANGEL_TOTP_SECRET=your-totp-secret

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/trademaster
DATABASE_USERNAME=trademaster_user
DATABASE_PASSWORD=secure_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Docker Configuration
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/broker-auth-service.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app.jar"]
```

This comprehensive Angel One SmartAPI integration follows TradeMaster's enterprise standards, implementing functional programming patterns, virtual threads, circuit breakers, and comprehensive security measures while maintaining high performance and reliability.