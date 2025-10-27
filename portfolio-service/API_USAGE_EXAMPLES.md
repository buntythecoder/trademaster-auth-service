# Portfolio Service - API Usage Examples

**Version**: 2.0.0
**Last Updated**: 2025-10-07

---

## 📋 Table of Contents

1. [Authentication](#authentication)
2. [Portfolio Management](#portfolio-management)
3. [Position Tracking](#position-tracking)
4. [P&L Calculations](#pnl-calculations)
5. [Risk Analytics](#risk-analytics)
6. [Performance Metrics](#performance-metrics)
7. [Internal APIs](#internal-apis)
8. [Error Handling](#error-handling)

---

## Authentication

### External API Authentication (JWT)

All external APIs require JWT bearer token authentication.

```bash
# Obtain JWT token from auth-service
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader1",
    "password": "securePassword123"
  }'

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1001,
  "expiresIn": 3600
}

# Use token in subsequent requests
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios
```

### Internal API Authentication (API Key)

Internal service-to-service APIs require API key authentication.

```bash
# Use API key in header
curl -H "X-Service-API-Key: portfolio-service-secret-key" \
  http://localhost:8083/api/internal/greetings
```

---

## Portfolio Management

### 1. Create Portfolio

Create a new portfolio for a user.

**Endpoint**: `POST /api/v1/portfolios`

```bash
curl -X POST http://localhost:8083/api/v1/portfolios \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "portfolioName": "My Trading Portfolio",
    "portfolioType": "STANDARD",
    "initialCash": 100000.00,
    "currency": "USD"
  }'
```

**Response** (201 Created):
```json
{
  "portfolioId": 1,
  "userId": 1001,
  "portfolioName": "My Trading Portfolio",
  "status": "ACTIVE",
  "totalValue": 100000.00,
  "cashBalance": 100000.00,
  "totalCost": 0.00,
  "realizedPnl": 0.00,
  "unrealizedPnl": 0.00,
  "dayPnl": 0.00,
  "currency": "USD",
  "dayTradesCount": 0,
  "createdAt": "2025-10-07T10:30:00Z",
  "lastValuationAt": "2025-10-07T10:30:00Z"
}
```

### 2. Get Portfolio by ID

Retrieve portfolio details.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1
```

**Response** (200 OK):
```json
{
  "portfolioId": 1,
  "portfolioName": "My Trading Portfolio",
  "status": "ACTIVE",
  "totalValue": 125000.00,
  "cashBalance": 75000.00,
  "totalCost": 45000.00,
  "marketValue": 50000.00,
  "realizedPnl": 2500.00,
  "unrealizedPnl": 5000.00,
  "dayPnl": 1500.00,
  "returnPercentage": 11.11,
  "positionCount": 5,
  "dayTradesCount": 2,
  "lastValuationAt": "2025-10-07T15:45:30Z"
}
```

### 3. Update Portfolio

Update portfolio details.

**Endpoint**: `PUT /api/v1/portfolios/{portfolioId}`

```bash
curl -X PUT http://localhost:8083/api/v1/portfolios/1 \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioName": "Updated Portfolio Name",
    "status": "ACTIVE"
  }'
```

### 4. Get Portfolio Summary

Get comprehensive portfolio summary.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/summary`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1/summary
```

**Response** (200 OK):
```json
{
  "portfolioId": 1,
  "portfolioName": "My Trading Portfolio",
  "status": "ACTIVE",
  "totalValue": 125000.00,
  "cashBalance": 75000.00,
  "realizedPnl": 2500.00,
  "unrealizedPnl": 5000.00,
  "dayPnl": 1500.00,
  "positionCount": 5,
  "winningPositions": 3,
  "losingPositions": 2,
  "largestPosition": 15000.00,
  "lastValuationAt": "2025-10-07T15:45:30Z",
  "summaryTimestamp": "2025-10-07T16:00:00Z"
}
```

---

## Position Tracking

### 1. Get All Positions

List all positions in a portfolio.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/positions`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1/positions
```

**Response** (200 OK):
```json
[
  {
    "positionId": 101,
    "symbol": "AAPL",
    "positionType": "LONG",
    "quantity": 100,
    "averagePrice": 150.00,
    "currentPrice": 160.00,
    "marketValue": 16000.00,
    "totalCost": 15000.00,
    "unrealizedPnl": 1000.00,
    "realizedPnl": 0.00,
    "returnPercentage": 6.67,
    "dayChange": 250.00,
    "dayChangePercentage": 1.59
  },
  {
    "positionId": 102,
    "symbol": "GOOGL",
    "positionType": "LONG",
    "quantity": 50,
    "averagePrice": 120.00,
    "currentPrice": 125.00,
    "marketValue": 6250.00,
    "totalCost": 6000.00,
    "unrealizedPnl": 250.00,
    "realizedPnl": 0.00,
    "returnPercentage": 4.17,
    "dayChange": 100.00,
    "dayChangePercentage": 1.63
  }
]
```

### 2. Get Position by Symbol

Retrieve specific position details.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/positions/symbol/{symbol}`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1/positions/symbol/AAPL
```

### 3. Execute Trade

Update position based on trade execution.

**Endpoint**: `POST /api/v1/portfolios/{portfolioId}/positions/trades`

```bash
curl -X POST http://localhost:8083/api/v1/portfolios/1/positions/trades \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "tradeType": "BUY",
    "quantity": 50,
    "price": 155.00,
    "commission": 5.00,
    "executionTime": "2025-10-07T14:30:00Z"
  }'
```

**Response** (201 Created):
```json
{
  "positionId": 101,
  "symbol": "AAPL",
  "positionType": "LONG",
  "quantity": 150,
  "averagePrice": 151.67,
  "currentPrice": 160.00,
  "marketValue": 24000.00,
  "totalCost": 22750.00,
  "unrealizedPnl": 1250.00,
  "lastTradeAt": "2025-10-07T14:30:00Z"
}
```

### 4. Get Tax Lots

Retrieve tax lot information for cost basis calculations.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/positions/symbol/{symbol}/tax-lots`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8083/api/v1/portfolios/1/positions/symbol/AAPL/tax-lots?costBasisMethod=FIFO"
```

**Response** (200 OK):
```json
[
  {
    "taxLotId": 1001,
    "symbol": "AAPL",
    "quantity": 100,
    "purchasePrice": 150.00,
    "purchaseDate": "2025-09-01",
    "costBasis": 15000.00,
    "currentValue": 16000.00,
    "unrealizedGain": 1000.00,
    "holdingPeriod": 36,
    "qualifiesForLongTermGains": false
  },
  {
    "taxLotId": 1002,
    "symbol": "AAPL",
    "quantity": 50,
    "purchasePrice": 155.00,
    "purchaseDate": "2025-10-07",
    "costBasis": 7750.00,
    "currentValue": 8000.00,
    "unrealizedGain": 250.00,
    "holdingPeriod": 0,
    "qualifiesForLongTermGains": false
  }
]
```

---

## P&L Calculations

### 1. Get P&L Breakdown

Get detailed profit/loss breakdown.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/pnl`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8083/api/v1/portfolios/1/pnl?startDate=2025-01-01&endDate=2025-10-07"
```

**Response** (200 OK):
```json
{
  "portfolioId": 1,
  "startDate": "2025-01-01",
  "endDate": "2025-10-07",
  "realizedPnl": 2500.00,
  "unrealizedPnl": 5000.00,
  "totalPnl": 7500.00,
  "dayPnl": 1500.00,
  "weekPnl": 3200.00,
  "monthPnl": 7500.00,
  "yearPnl": 7500.00,
  "realizedGains": 3500.00,
  "realizedLosses": -1000.00,
  "unrealizedGains": 6200.00,
  "unrealizedLosses": -1200.00,
  "totalFees": 125.00,
  "netPnl": 7375.00,
  "returnPercentage": 7.375
}
```

### 2. Calculate Real-Time P&L

Get real-time P&L calculation with current market prices.

**Endpoint**: `POST /api/v1/portfolios/{portfolioId}/pnl/calculate`

```bash
curl -X POST http://localhost:8083/api/v1/portfolios/1/pnl/calculate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "includeUnrealized": true,
    "includeFees": true,
    "costBasisMethod": "FIFO"
  }'
```

---

## Risk Analytics

### 1. Get Portfolio Risk Assessment

Get comprehensive risk metrics.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/risk`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/1/risk
```

**Response** (200 OK):
```json
{
  "portfolioId": 1,
  "overallRiskLevel": "MODERATE",
  "riskScore": 6.5,
  "concentrationRisk": {
    "largestPositionPercentage": 32.5,
    "top5PositionsPercentage": 75.2,
    "diversificationScore": 7.2
  },
  "volatilityRisk": {
    "portfolioBeta": 1.15,
    "standardDeviation": 0.025,
    "sharpeRatio": 1.85,
    "valueAtRisk95": -2500.00
  },
  "liquidityRisk": {
    "liquidPositionsPercentage": 95.5,
    "averageDailyVolume": 1500000,
    "liquidityScore": 8.5
  },
  "dayTradingRisk": {
    "currentDayTrades": 2,
    "dayTradeLimit": 3,
    "approachingLimit": false
  },
  "riskAlerts": [
    {
      "severity": "MEDIUM",
      "category": "CONCENTRATION",
      "message": "AAPL position exceeds 30% of portfolio"
    }
  ],
  "calculatedAt": "2025-10-07T16:00:00Z"
}
```

### 2. Assess Trade Risk

Evaluate risk before executing a trade.

**Endpoint**: `POST /api/v1/portfolios/{portfolioId}/risk/assess`

```bash
curl -X POST http://localhost:8083/api/v1/portfolios/1/risk/assess \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "TSLA",
    "tradeType": "BUY",
    "quantity": 100,
    "price": 250.00
  }'
```

**Response** (200 OK):
```json
{
  "approved": true,
  "riskLevel": "MODERATE",
  "riskFactors": [
    {
      "factor": "CONCENTRATION",
      "impact": "MEDIUM",
      "message": "This trade will increase TSLA position to 25% of portfolio"
    },
    {
      "factor": "BUYING_POWER",
      "impact": "LOW",
      "message": "Sufficient cash available: $75,000"
    }
  ],
  "recommendations": [
    "Consider reducing AAPL position before increasing TSLA exposure"
  ],
  "estimatedImpact": {
    "newConcentration": 25.0,
    "remainingCash": 50000.00,
    "newRiskScore": 6.8
  }
}
```

---

## Performance Metrics

### 1. Get Portfolio Analytics

Get comprehensive performance analytics.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/analytics`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8083/api/v1/portfolios/1/analytics?period=3M"
```

**Response** (200 OK):
```json
{
  "portfolioId": 1,
  "period": "3M",
  "returns": {
    "totalReturn": 7.5,
    "annualizedReturn": 30.0,
    "dayReturn": 1.2,
    "weekReturn": 2.56,
    "monthReturn": 6.25,
    "quarterReturn": 7.5
  },
  "performance": {
    "sharpeRatio": 1.85,
    "sortinoRatio": 2.15,
    "calmarRatio": 3.25,
    "informationRatio": 1.45,
    "alpha": 0.025,
    "beta": 1.15
  },
  "drawdown": {
    "currentDrawdown": -2.5,
    "maxDrawdown": -8.5,
    "maxDrawdownDate": "2025-08-15",
    "recoveryDays": 12
  },
  "winRate": {
    "winningTrades": 15,
    "losingTrades": 5,
    "winRate": 75.0,
    "profitFactor": 2.8
  }
}
```

### 2. Compare Performance

Compare portfolio performance against benchmarks.

**Endpoint**: `GET /api/v1/portfolios/{portfolioId}/analytics/compare`

```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:8083/api/v1/portfolios/1/analytics/compare?benchmarks=SPY,QQQ&period=1Y"
```

---

## Internal APIs

### 1. Service Greetings

Internal endpoint for service discovery.

**Endpoint**: `GET /api/internal/greetings`

```bash
curl -H "X-Service-API-Key: portfolio-service-secret-key" \
  http://localhost:8083/api/internal/greetings
```

**Response** (200 OK):
```json
{
  "service": "portfolio-service",
  "version": "2.0.0",
  "message": "Portfolio Service is running and ready",
  "timestamp": "2025-10-07T16:00:00Z",
  "status": "OPERATIONAL",
  "capabilities": {
    "portfolioTracking": "ACTIVE",
    "pnlCalculation": "ACTIVE",
    "riskAnalytics": "ACTIVE",
    "performanceReporting": "ACTIVE"
  }
}
```

### 2. Get Portfolio Summary (Internal)

Internal API for service-to-service portfolio data access.

**Endpoint**: `GET /api/internal/v1/portfolio/users/{userId}/summary`

```bash
curl -H "X-Service-API-Key: portfolio-service-secret-key" \
  http://localhost:8083/api/internal/v1/portfolio/users/1001/summary
```

### 3. Validate Buying Power

Check if user has sufficient buying power for a trade.

**Endpoint**: `GET /api/internal/v1/portfolio/users/{userId}/validate-buying-power`

```bash
curl -H "X-Service-API-Key: portfolio-service-secret-key" \
  "http://localhost:8083/api/internal/v1/portfolio/users/1001/validate-buying-power?requiredAmount=25000.00"
```

**Response** (200 OK):
```json
{
  "valid": true,
  "availableAmount": 75000.00,
  "requiredAmount": 25000.00,
  "excessAmount": 50000.00,
  "timestamp": "2025-10-07T16:00:00Z"
}
```

---

## Error Handling

### Standard Error Response Format

All errors follow a consistent format:

```json
{
  "timestamp": "2025-10-07T16:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid portfolio data",
  "path": "/api/v1/portfolios",
  "correlationId": "abc123-def456-ghi789",
  "validationErrors": [
    {
      "field": "initialCash",
      "message": "Initial cash must be greater than 1000"
    }
  ]
}
```

### Common HTTP Status Codes

| Status Code | Description | Example |
|-------------|-------------|---------|
| 200 OK | Successful request | GET portfolio |
| 201 Created | Resource created | POST new portfolio |
| 400 Bad Request | Invalid input | Missing required fields |
| 401 Unauthorized | Missing/invalid authentication | No JWT token |
| 403 Forbidden | Insufficient permissions | Accessing another user's portfolio |
| 404 Not Found | Resource doesn't exist | Portfolio ID not found |
| 409 Conflict | Resource conflict | User already has active portfolio |
| 429 Too Many Requests | Rate limit exceeded | Exceeded API rate limit |
| 500 Internal Server Error | Server error | Database connection failure |
| 503 Service Unavailable | Service temporarily unavailable | Maintenance mode |

### Error Examples

#### 400 Bad Request
```bash
curl -X POST http://localhost:8083/api/v1/portfolios \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
```

Response:
```json
{
  "timestamp": "2025-10-07T16:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": [
    {
      "field": "userId",
      "message": "User ID is required"
    },
    {
      "field": "portfolioName",
      "message": "Portfolio name is required"
    }
  ]
}
```

#### 404 Not Found
```bash
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:8083/api/v1/portfolios/99999
```

Response:
```json
{
  "timestamp": "2025-10-07T16:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Portfolio with ID 99999 not found",
  "path": "/api/v1/portfolios/99999"
}
```

---

## Rate Limiting

### Rate Limit Headers

All responses include rate limit headers:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1696693200
```

### Rate Limits by Endpoint Category

| Category | Limit | Window |
|----------|-------|--------|
| Portfolio Operations | 100/min | per user |
| P&L Calculations | 200/min | per user |
| Risk Assessment | 50/min | per user |
| Internal APIs | 1000/min | per service |

---

## Additional Resources

- **OpenAPI Spec**: `http://localhost:8083/v3/api-docs`
- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **Service Architecture**: See `SERVICE_ARCHITECTURE.md`
- **Deployment Guide**: See `DEPLOYMENT_GUIDE.md`

---

**Last Updated**: 2025-10-07
**Version**: 2.0.0
