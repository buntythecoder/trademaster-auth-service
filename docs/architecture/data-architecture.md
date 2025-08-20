# Data Architecture

## Database Design

### PostgreSQL (Primary Transactional Database)

**User Management Schema:**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    kyc_status VARCHAR(50) DEFAULT 'pending',
    subscription_tier VARCHAR(50) DEFAULT 'free',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_profiles (
    user_id INTEGER REFERENCES users(id),
    risk_tolerance VARCHAR(20),
    trading_experience VARCHAR(20),
    behavioral_settings JSONB,
    preferences JSONB
);
```

**Trading Schema:**
```sql
CREATE TABLE trades (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    symbol VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    trade_type VARCHAR(10) CHECK (trade_type IN ('BUY', 'SELL')),
    status VARCHAR(20) DEFAULT 'pending',
    executed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE portfolios (
    user_id INTEGER REFERENCES users(id),
    symbol VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    avg_price DECIMAL(10,2) NOT NULL,
    unrealized_pnl DECIMAL(15,2),
    updated_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, symbol)
);
```

### InfluxDB (Time-Series Market Data)

**Market Data Schema:**
```sql
-- Price data measurement
price,symbol=RELIANCE,exchange=NSE value=2450.50,volume=1000000 1609459200000000000

-- Behavioral events measurement  
behavior,user_id=123,event_type=emotional_trade confidence=0.85,risk_score=7.2 1609459200000000000

-- Institutional activity measurement
institutional,symbol=TCS,activity_type=accumulation strength=8.5,volume_ratio=3.2 1609459200000000000
```

### Redis (Caching & Session Management)

**Caching Strategy:**
```yaml
cache_layers:
  user_sessions: 24h_ttl
  market_data: 5s_ttl
  portfolio_data: 30s_ttl
  behavioral_models: 1h_ttl
  compliance_rules: 4h_ttl
```

## Data Processing Pipeline

### Real-time Stream Processing

**Apache Kafka Topics:**
- `market-data-raw`: Raw market data from exchanges
- `market-data-processed`: Cleaned and normalized data
- `user-behavior-events`: Trading actions and UI interactions
- `institutional-alerts`: Detected institutional activity
- `compliance-events`: Regulatory compliance checks

**Stream Processing Architecture:**
```java
@Component
public class MarketDataProcessor {
    
    @KafkaListener(topics = "market-data-raw")
    public void processRawData(MarketDataEvent event) {
        // Data validation and normalization
        // Real-time analysis and alerting
        // Store to InfluxDB
        // Broadcast to WebSocket clients
    }
    
    @KafkaListener(topics = "user-behavior-events")
    public void processBehaviorEvent(BehaviorEvent event) {
        // Behavioral pattern analysis
        // Risk assessment
        // AI model inference
        // Generate interventions if needed
    }
}
```
