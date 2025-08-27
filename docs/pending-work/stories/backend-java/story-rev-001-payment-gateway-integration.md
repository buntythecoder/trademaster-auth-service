# Story REV-001: Payment Gateway Integration

## Epic
Epic 5: Revenue Systems & Gamification

## Story Overview
**As a** TradeMaster user  
**I want** to securely pay for subscription services using multiple payment methods  
**So that** I can access premium features and continue using the platform seamlessly

## Business Value
- **Revenue Generation**: Direct monetization through subscription payments
- **User Trust**: Secure payment processing builds confidence
- **Market Coverage**: Multiple gateways ensure 98%+ payment success rate
- **Compliance**: PCI DSS compliance for financial data security

## Technical Requirements

### Core Integration
- **Payment Gateways**: Razorpay (primary), Stripe (international), UPI (domestic)
- **Payment Methods**: Credit/Debit cards, UPI, Net Banking, Wallets, BNPL
- **Currency Support**: INR (primary), USD (international expansion)
- **Security**: PCI DSS Level 1 compliance, tokenization, encryption

### API Integration Points
```typescript
// Backend Service Integration
interface PaymentGatewayService {
  // Razorpay Integration
  createRazorpayOrder(amount: number, currency: string, metadata: object): Promise<OrderResponse>
  verifyRazorpayPayment(razorpayPaymentId: string, razorpayOrderId: string, razorpaySignature: string): Promise<boolean>
  
  // Stripe Integration  
  createStripePaymentIntent(amount: number, currency: string, customerId: string): Promise<PaymentIntent>
  confirmStripePayment(paymentIntentId: string): Promise<PaymentResult>
  
  // Unified Payment Processing
  processPayment(request: PaymentRequest): Promise<PaymentResult>
  handleWebhook(provider: string, payload: any, signature: string): Promise<WebhookResult>
}

interface PaymentRequest {
  userId: string
  subscriptionPlanId: string
  amount: number
  currency: string
  paymentMethod: 'card' | 'upi' | 'netbanking' | 'wallet' | 'bnpl'
  provider: 'razorpay' | 'stripe'
  metadata: {
    planName: string
    billingCycle: 'monthly' | 'quarterly' | 'annual'
    discountApplied?: number
  }
}
```

### Frontend Integration
```tsx
// Payment Component Architecture
const PaymentComponent: React.FC<PaymentProps> = ({
  subscriptionPlan,
  onSuccess,
  onError
}) => {
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('card')
  const [processing, setProcessing] = useState(false)
  
  // Razorpay Integration
  const initializeRazorpay = useCallback(() => {
    const options = {
      key: process.env.REACT_APP_RAZORPAY_KEY,
      amount: subscriptionPlan.amount * 100,
      currency: 'INR',
      name: 'TradeMaster',
      description: `${subscriptionPlan.name} Subscription`,
      handler: handlePaymentSuccess,
      prefill: {
        name: user.name,
        email: user.email,
        contact: user.phone
      },
      theme: {
        color: '#10B981'
      }
    }
    
    const rzp = new window.Razorpay(options)
    rzp.open()
  }, [subscriptionPlan])
  
  // Stripe Elements Integration
  const StripePaymentForm = () => (
    <Elements stripe={stripePromise}>
      <CardElement 
        options={{
          style: {
            base: {
              fontSize: '16px',
              color: '#424770',
              '::placeholder': {
                color: '#aab7c4',
              },
            },
          },
        }}
      />
    </Elements>
  )
}
```

### Database Schema Extensions
```sql
-- Payment Transactions Table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES subscriptions(id),
    
    -- Payment Details
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status payment_status NOT NULL DEFAULT 'pending',
    
    -- Gateway Integration
    payment_gateway payment_gateway_enum NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_order_id VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    
    -- Payment Method
    payment_method payment_method_enum NOT NULL,
    payment_method_details JSONB,
    
    -- Metadata
    metadata JSONB,
    failure_reason TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- Payment Method Storage
CREATE TABLE user_payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- Tokenized Payment Details
    payment_method_token VARCHAR(255) NOT NULL,
    payment_method_type payment_method_enum NOT NULL,
    gateway_provider payment_gateway_enum NOT NULL,
    
    -- Display Information (No sensitive data)
    display_name VARCHAR(100),
    last_four_digits CHAR(4),
    expiry_month INTEGER,
    expiry_year INTEGER,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Custom Types
CREATE TYPE payment_status AS ENUM ('pending', 'processing', 'completed', 'failed', 'cancelled', 'refunded');
CREATE TYPE payment_gateway_enum AS ENUM ('razorpay', 'stripe', 'upi');
CREATE TYPE payment_method_enum AS ENUM ('card', 'upi', 'netbanking', 'wallet', 'bnpl');
```

## Acceptance Criteria

### Core Payment Processing
- [ ] **Multiple Gateway Support**: Razorpay (domestic) and Stripe (international) integration
- [ ] **Payment Methods**: Support cards, UPI, net banking, wallets, BNPL options
- [ ] **Success Rate**: Achieve 98%+ payment success rate across methods
- [ ] **Response Time**: Payment processing completion within 10 seconds

### Security & Compliance
- [ ] **PCI Compliance**: No sensitive payment data stored in application
- [ ] **Tokenization**: All payment methods stored as secure tokens
- [ ] **Encryption**: All payment communications encrypted (TLS 1.3)
- [ ] **Fraud Detection**: Integration with gateway fraud detection systems

### User Experience
- [ ] **Seamless Flow**: Single-page checkout with real-time validation
- [ ] **Method Management**: Save and manage payment methods securely
- [ ] **Instant Feedback**: Real-time payment status with success/failure notifications
- [ ] **Mobile Optimized**: Responsive payment forms for all devices

### Business Logic
- [ ] **Subscription Activation**: Automatic feature activation upon successful payment
- [ ] **Failure Handling**: Graceful handling of payment failures with retry options
- [ ] **Webhook Processing**: Real-time payment status updates via webhooks
- [ ] **Reconciliation**: Daily payment reconciliation with gateway reports

## Technical Implementation Details

### Backend Service Architecture
```java
@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private RazorpayClient razorpayClient;
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private PaymentTransactionRepository transactionRepository;
    
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            // Create transaction record
            PaymentTransaction transaction = createTransaction(request);
            
            // Route to appropriate gateway
            PaymentResult result = switch (request.getProvider()) {
                case RAZORPAY -> processRazorpayPayment(request, transaction);
                case STRIPE -> processStripePayment(request, transaction);
                default -> throw new UnsupportedPaymentProviderException();
            };
            
            // Update transaction status
            updateTransactionStatus(transaction, result);
            
            // Activate subscription if successful
            if (result.isSuccessful()) {
                subscriptionService.activateSubscription(
                    request.getUserId(), 
                    request.getSubscriptionPlanId()
                );
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw new PaymentProcessingException("Payment failed: " + e.getMessage());
        }
    }
    
    @EventListener
    public void handlePaymentWebhook(PaymentWebhookEvent event) {
        // Verify webhook signature
        if (!verifyWebhookSignature(event)) {
            log.warn("Invalid webhook signature received");
            return;
        }
        
        // Process payment status update
        PaymentTransaction transaction = transactionRepository
            .findByGatewayTransactionId(event.getTransactionId())
            .orElseThrow(() -> new TransactionNotFoundException());
            
        transaction.setStatus(event.getStatus());
        transaction.setProcessedAt(event.getTimestamp());
        
        if (event.getStatus() == PaymentStatus.COMPLETED) {
            subscriptionService.activateSubscription(
                transaction.getUserId(),
                transaction.getSubscriptionId()
            );
        }
        
        transactionRepository.save(transaction);
    }
}
```

### Frontend Payment Flow
```tsx
const usePaymentFlow = () => {
  const [paymentState, setPaymentState] = useState<PaymentState>('idle')
  const { mutate: processPayment } = useMutation(paymentApi.processPayment)
  
  const initiatePayment = useCallback(async (paymentRequest: PaymentRequest) => {
    setPaymentState('processing')
    
    try {
      if (paymentRequest.provider === 'razorpay') {
        // Razorpay integration
        const order = await paymentApi.createRazorpayOrder(paymentRequest)
        
        const razorpayOptions = {
          key: process.env.REACT_APP_RAZORPAY_KEY_ID,
          amount: order.amount,
          currency: order.currency,
          name: 'TradeMaster',
          description: paymentRequest.metadata.planName,
          order_id: order.id,
          handler: async (response: RazorpayResponse) => {
            const verification = await paymentApi.verifyRazorpayPayment({
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_order_id: response.razorpay_order_id,
              razorpay_signature: response.razorpay_signature
            })
            
            if (verification.success) {
              setPaymentState('completed')
              toast.success('Payment successful! Your subscription is now active.')
              onPaymentSuccess(verification.transaction)
            } else {
              setPaymentState('failed')
              toast.error('Payment verification failed. Please contact support.')
            }
          },
          prefill: {
            name: user.name,
            email: user.email,
            contact: user.phone
          }
        }
        
        const rzp = new window.Razorpay(razorpayOptions)
        rzp.open()
        
      } else if (paymentRequest.provider === 'stripe') {
        // Stripe integration
        const { client_secret } = await paymentApi.createStripePaymentIntent(paymentRequest)
        
        const result = await stripe.confirmCardPayment(client_secret, {
          payment_method: paymentRequest.paymentMethodId
        })
        
        if (result.error) {
          setPaymentState('failed')
          toast.error(`Payment failed: ${result.error.message}`)
        } else {
          setPaymentState('completed')
          toast.success('Payment successful! Your subscription is now active.')
          onPaymentSuccess(result.paymentIntent)
        }
      }
      
    } catch (error) {
      setPaymentState('failed')
      toast.error('Payment processing failed. Please try again.')
      console.error('Payment error:', error)
    }
  }, [user, onPaymentSuccess])
  
  return {
    paymentState,
    initiatePayment
  }
}
```

## Testing Strategy

### Unit Tests
- Payment service logic validation
- Gateway integration error handling
- Webhook signature verification
- Transaction state management

### Integration Tests
- End-to-end payment flow testing
- Gateway sandbox integration
- Webhook delivery validation
- Database transaction integrity

### Security Tests
- PCI compliance validation
- Payment data encryption verification
- Tokenization security testing
- Fraud detection system integration

### Performance Tests
- Payment processing latency (<10s)
- High-volume transaction handling
- Gateway failover testing
- Database query optimization

## Definition of Done
- [ ] Both Razorpay and Stripe integrations completed and tested
- [ ] All major payment methods supported (cards, UPI, wallets)
- [ ] PCI DSS compliance validated by security audit
- [ ] 98%+ payment success rate achieved in testing
- [ ] Webhook processing handling all payment status updates
- [ ] Mobile-responsive payment forms implemented
- [ ] Error handling and retry mechanisms working
- [ ] Payment reconciliation process automated
- [ ] Security penetration testing passed
- [ ] Load testing completed (1000+ concurrent payments)

## Story Points: 13

## Dependencies
- Razorpay merchant account setup and API keys
- Stripe merchant account and integration keys
- PCI compliance certification process
- SSL certificate installation for secure communications
- Bank account setup for payment settlements

## Notes
- Integration with existing user management and subscription systems
- Consideration for international expansion with additional payment methods
- Compliance with RBI guidelines for digital payments in India
- Integration with existing notification system for payment confirmations