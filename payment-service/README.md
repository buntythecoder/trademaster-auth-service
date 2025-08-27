# Payment Service

The Payment Service handles all payment processing, subscription management, and financial transactions for the TradeMaster platform.

## Features

- **Multi-Gateway Support**: Razorpay (India), Stripe (International), UPI
- **Secure Payment Processing**: PCI DSS compliant with tokenized payment storage
- **Real-time Webhooks**: Instant payment status updates from gateways
- **Subscription Management**: Automated subscription lifecycle handling
- **Event Publishing**: Kafka integration for payment events
- **Comprehensive Auditing**: Full audit trail for financial compliance

## Technology Stack

- **Framework**: Spring Boot 3.5.3
- **Language**: Java 24 with Virtual Threads
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for session management and rate limiting
- **Message Queue**: Kafka for event publishing
- **Payment Gateways**: Razorpay Java SDK, Stripe Java SDK
- **Documentation**: OpenAPI 3 (Swagger)

## API Endpoints

### Payment Processing
- `POST /api/v1/payments/process` - Process a payment
- `GET /api/v1/payments/transaction/{id}` - Get transaction details
- `GET /api/v1/payments/transaction/{id}/status` - Get payment status

### User Management
- `GET /api/v1/payments/user/{userId}/history` - Get payment history

### Webhooks
- `POST /api/v1/webhooks/razorpay` - Razorpay webhook notifications
- `POST /api/v1/webhooks/stripe` - Stripe webhook notifications
- `POST /api/v1/webhooks/upi` - UPI webhook notifications

## Configuration

### Required Environment Variables

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/trademaster_payment
SPRING_DATASOURCE_USERNAME=trademaster_user
SPRING_DATASOURCE_PASSWORD=trademaster_password

# Razorpay Configuration
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_WEBHOOK_SECRET=your_razorpay_webhook_secret

# Stripe Configuration
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret

# Redis Configuration
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Kafka Configuration
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE trademaster_payment;
CREATE USER trademaster_user WITH PASSWORD 'trademaster_password';
GRANT ALL PRIVILEGES ON DATABASE trademaster_payment TO trademaster_user;
```

2. Run Flyway migrations:
```bash
./gradlew flywayMigrate
```

## Running the Service

### Development
```bash
./gradlew bootRun
```

### Production with Docker
```bash
# Build the JAR
./gradlew build

# Build Docker image
docker build -t trademaster/payment-service:latest .

# Run container
docker run -d \
  --name payment-service \
  -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file .env \
  trademaster/payment-service:latest
```

## Payment Flow

### Standard Payment Flow
1. User initiates payment via frontend
2. Frontend calls `/payments/process` with payment details
3. Service creates transaction record and routes to appropriate gateway
4. Gateway returns payment intent/order details
5. Frontend handles gateway-specific payment UI
6. Gateway sends webhook on payment completion
7. Service processes webhook and updates transaction status
8. Subscription is activated automatically on successful payment

### Supported Payment Methods

#### Razorpay (India)
- Credit/Debit Cards
- UPI
- Net Banking
- Digital Wallets (Paytm, PhonePe, etc.)
- Buy Now Pay Later (BNPL)

#### Stripe (International)
- Credit/Debit Cards
- Digital Wallets (Apple Pay, Google Pay)
- SEPA Direct Debit
- BECS Direct Debit

## Security Features

- **PCI DSS Compliance**: No sensitive payment data stored
- **Tokenization**: Payment methods stored as secure tokens
- **Webhook Verification**: All webhook signatures verified
- **Rate Limiting**: API rate limiting with Redis
- **Encryption**: All sensitive data encrypted at rest and in transit
- **Audit Logging**: Comprehensive audit trail for compliance

## Monitoring & Observability

- **Health Checks**: `/actuator/health` endpoint
- **Metrics**: Prometheus metrics exposed at `/actuator/prometheus`
- **Distributed Tracing**: Micrometer integration
- **Structured Logging**: JSON logging with correlation IDs

## Testing

### Run Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

## Troubleshooting

### Common Issues

1. **Payment Gateway Connection Errors**
   - Verify API credentials are correct
   - Check network connectivity to gateway endpoints
   - Ensure webhook URLs are accessible from the internet

2. **Database Migration Failures**
   - Check database user permissions
   - Verify PostgreSQL version compatibility
   - Review Flyway migration logs

3. **Webhook Signature Verification Failures**
   - Confirm webhook secret matches gateway configuration
   - Check webhook endpoint URL configuration
   - Verify request headers are being passed correctly

### Logs
Service logs are structured JSON format. Key log entries to monitor:
- Payment processing attempts
- Webhook receipt and processing
- Database transaction errors
- Gateway API errors

## Contributing

1. Follow existing code patterns and conventions
2. Ensure all tests pass before submitting PR
3. Add integration tests for new payment methods
4. Update documentation for API changes
5. Follow security best practices for financial data handling