# Story REV-002: Subscription Management Service

## Epic
Epic 5: Revenue Systems & Gamification

## Story Overview
**As a** TradeMaster platform administrator  
**I want** a comprehensive subscription management system  
**So that** users can manage their subscriptions and I can track recurring revenue effectively

## Business Value
- **Revenue Tracking**: Accurate MRR/ARR calculation and forecasting
- **Customer Retention**: Seamless subscription experience reduces churn
- **Business Intelligence**: Subscription analytics for growth optimization
- **Operational Efficiency**: Automated billing and subscription lifecycle management

## Technical Requirements

### Subscription Tiers Architecture
```typescript
interface SubscriptionPlan {
  id: string
  name: string
  displayName: string
  description: string
  features: Feature[]
  pricing: PricingTier[]
  billingCycles: BillingCycle[]
  limits: UsageLimits
  status: 'active' | 'deprecated' | 'coming_soon'
  metadata: {
    popularPlan?: boolean
    recommendedFor?: string[]
    upgradeIncentives?: string[]
  }
}

interface PricingTier {
  currency: 'INR' | 'USD'
  monthly: number
  quarterly: number
  annual: number
  discounts: {
    quarterly: number // percentage
    annual: number    // percentage
  }
}

interface Feature {
  key: string
  name: string
  description: string
  category: 'core' | 'advanced' | 'premium' | 'enterprise'
  enabled: boolean
  limits?: {
    daily?: number
    monthly?: number
    total?: number
  }
}
```

### Subscription Plans Definition
```yaml
# Free Tier
free_tier:
  name: "Free"
  price: 0
  features:
    - Basic market data (15-minute delay)
    - 5 trades per day
    - Basic portfolio tracking
    - Educational content access
  limits:
    daily_trades: 5
    portfolio_assets: 10
    api_calls_daily: 100

# Pro Tier
pro_tier:
  name: "Pro"
  price_monthly: 999 # INR
  price_annual: 9999 # INR (2 months free)
  features:
    - Real-time market data
    - Unlimited trades
    - Advanced charting
    - Portfolio analytics
    - Email notifications
    - Priority support
  limits:
    portfolio_assets: 100
    api_calls_daily: 10000
    alerts: 50

# Premium Tier
premium_tier:
  name: "Premium"
  price_monthly: 2999 # INR
  price_annual: 29999 # INR (2 months free)
  features:
    - All Pro features
    - AI trading insights
    - Behavioral pattern analysis
    - Advanced risk management
    - SMS + WhatsApp notifications
    - Institutional data access
    - Custom indicators
  limits:
    portfolio_assets: 500
    api_calls_daily: 50000
    alerts: 200
    ai_insights_daily: 50

# Enterprise Tier
enterprise_tier:
  name: "Enterprise"
  price_monthly: 25000 # INR
  custom_pricing: true
  features:
    - All Premium features
    - Custom integrations
    - Dedicated account manager
    - White-label options
    - API access
    - Custom reporting
    - SLA guarantees
  limits:
    unlimited: true
```

### Backend Service Implementation
```java
@Service
@Transactional
public class SubscriptionService {
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NotificationService notificationService;
    
    public Subscription createSubscription(CreateSubscriptionRequest request) {
        // Validate subscription plan
        SubscriptionPlan plan = planRepository
            .findByIdAndStatus(request.getPlanId(), PlanStatus.ACTIVE)
            .orElseThrow(() -> new InvalidSubscriptionPlanException());
        
        // Calculate billing dates
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(startDate, request.getBillingCycle());
        LocalDateTime nextBillingDate = calculateNextBilling(endDate);
        
        // Create subscription
        Subscription subscription = Subscription.builder()
            .userId(request.getUserId())
            .planId(request.getPlanId())
            .status(SubscriptionStatus.PENDING)
            .billingCycle(request.getBillingCycle())
            .startDate(startDate)
            .endDate(endDate)
            .nextBillingDate(nextBillingDate)
            .amount(plan.getPrice(request.getBillingCycle()))
            .currency("INR")
            .build();
        
        subscription = subscriptionRepository.save(subscription);
        
        // Process initial payment
        if (subscription.getAmount() > 0) {
            PaymentResult paymentResult = paymentService.processPayment(
                PaymentRequest.builder()
                    .userId(request.getUserId())
                    .subscriptionId(subscription.getId())
                    .amount(subscription.getAmount())
                    .description("Subscription: " + plan.getDisplayName())
                    .build()
            );
            
            if (paymentResult.isSuccessful()) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setActivatedAt(LocalDateTime.now());
            } else {
                subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);
            }
        } else {
            // Free tier activation
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setActivatedAt(LocalDateTime.now());
        }
        
        return subscriptionRepository.save(subscription);
    }
    
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void processBillingCycle() {
        List<Subscription> subscriptionsToRenew = subscriptionRepository
            .findByNextBillingDateBeforeAndStatus(
                LocalDateTime.now(), 
                SubscriptionStatus.ACTIVE
            );
        
        for (Subscription subscription : subscriptionsToRenew) {
            try {
                renewSubscription(subscription);
            } catch (Exception e) {
                log.error("Failed to renew subscription: " + subscription.getId(), e);
                handleRenewalFailure(subscription);
            }
        }
    }
    
    private void renewSubscription(Subscription subscription) {
        // Process renewal payment
        PaymentResult paymentResult = paymentService.processPayment(
            PaymentRequest.builder()
                .userId(subscription.getUserId())
                .subscriptionId(subscription.getId())
                .amount(subscription.getAmount())
                .description("Subscription Renewal")
                .isRenewal(true)
                .build()
        );
        
        if (paymentResult.isSuccessful()) {
            // Extend subscription period
            LocalDateTime newEndDate = calculateEndDate(
                subscription.getEndDate(), 
                subscription.getBillingCycle()
            );
            LocalDateTime newNextBillingDate = calculateNextBilling(newEndDate);
            
            subscription.setEndDate(newEndDate);
            subscription.setNextBillingDate(newNextBillingDate);
            subscription.setLastRenewalDate(LocalDateTime.now());
            
            subscriptionRepository.save(subscription);
            
            // Send renewal confirmation
            notificationService.sendSubscriptionRenewalConfirmation(subscription);
            
        } else {
            // Handle payment failure
            subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);
            subscription.setPaymentFailureCount(subscription.getPaymentFailureCount() + 1);
            
            // Grace period logic
            if (subscription.getPaymentFailureCount() >= 3) {
                subscription.setStatus(SubscriptionStatus.SUSPENDED);
                notificationService.sendSubscriptionSuspensionNotice(subscription);
            } else {
                // Schedule retry
                schedulePaymentRetry(subscription);
                notificationService.sendPaymentFailureNotification(subscription);
            }
            
            subscriptionRepository.save(subscription);
        }
    }
    
    public void upgradeSubscription(String subscriptionId, String newPlanId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException());
        
        SubscriptionPlan newPlan = planRepository.findById(newPlanId)
            .orElseThrow(() -> new InvalidSubscriptionPlanException());
        
        // Calculate prorated amount
        BigDecimal proratedAmount = calculateProratedUpgrade(subscription, newPlan);
        
        if (proratedAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Process upgrade payment
            PaymentResult paymentResult = paymentService.processPayment(
                PaymentRequest.builder()
                    .userId(subscription.getUserId())
                    .subscriptionId(subscription.getId())
                    .amount(proratedAmount)
                    .description("Subscription Upgrade")
                    .build()
            );
            
            if (!paymentResult.isSuccessful()) {
                throw new SubscriptionUpgradeFailedException("Payment failed for upgrade");
            }
        }
        
        // Update subscription
        subscription.setPlanId(newPlanId);
        subscription.setAmount(newPlan.getPrice(subscription.getBillingCycle()));
        subscription.setUpgradedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
        
        // Update user permissions
        updateUserFeatureAccess(subscription.getUserId(), newPlan);
        
        // Send upgrade confirmation
        notificationService.sendSubscriptionUpgradeConfirmation(subscription, newPlan);
    }
}
```

### Database Schema
```sql
-- Subscription Plans
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Pricing
    monthly_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    quarterly_price DECIMAL(10,2),
    annual_price DECIMAL(10,2),
    
    -- Discounts
    quarterly_discount_percent INTEGER DEFAULT 0,
    annual_discount_percent INTEGER DEFAULT 0,
    
    -- Features and Limits
    features JSONB NOT NULL DEFAULT '[]',
    usage_limits JSONB NOT NULL DEFAULT '{}',
    
    -- Metadata
    is_popular BOOLEAN DEFAULT false,
    recommended_for TEXT[],
    status plan_status DEFAULT 'active',
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User Subscriptions
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    
    -- Subscription Details
    status subscription_status NOT NULL DEFAULT 'pending',
    billing_cycle billing_cycle_enum NOT NULL DEFAULT 'monthly',
    
    -- Pricing
    amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Dates
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    next_billing_date TIMESTAMP,
    activated_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    suspended_at TIMESTAMP,
    
    -- Billing History
    last_renewal_date TIMESTAMP,
    payment_failure_count INTEGER DEFAULT 0,
    total_amount_paid DECIMAL(12,2) DEFAULT 0,
    
    -- Metadata
    upgraded_from UUID REFERENCES subscription_plans(id),
    upgraded_at TIMESTAMP,
    cancellation_reason TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Subscription Usage Tracking
CREATE TABLE subscription_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- Usage Metrics
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    api_calls INTEGER DEFAULT 0,
    trades_executed INTEGER DEFAULT 0,
    alerts_sent INTEGER DEFAULT 0,
    ai_insights_requested INTEGER DEFAULT 0,
    
    -- Feature Usage
    features_used JSONB DEFAULT '{}',
    
    -- Limits Exceeded
    limits_exceeded JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(subscription_id, usage_date)
);

-- Custom Types
CREATE TYPE subscription_status AS ENUM (
    'pending', 'active', 'cancelled', 'suspended', 
    'payment_failed', 'expired', 'trial'
);

CREATE TYPE billing_cycle_enum AS ENUM ('monthly', 'quarterly', 'annual');
CREATE TYPE plan_status AS ENUM ('active', 'deprecated', 'coming_soon');
```

### Frontend Subscription Management
```tsx
const SubscriptionManagement: React.FC = () => {
  const { data: currentSubscription } = useQuery(['subscription'], 
    subscriptionApi.getCurrentSubscription
  )
  const { data: availablePlans } = useQuery(['plans'], 
    subscriptionApi.getAvailablePlans
  )
  
  const upgradeMutation = useMutation(subscriptionApi.upgradeSubscription)
  const cancelMutation = useMutation(subscriptionApi.cancelSubscription)
  
  const handleUpgrade = async (planId: string) => {
    try {
      const result = await upgradeMutation.mutateAsync({ planId })
      toast.success('Subscription upgraded successfully!')
      queryClient.invalidateQueries(['subscription'])
    } catch (error) {
      toast.error('Failed to upgrade subscription. Please try again.')
    }
  }
  
  const handleCancel = async () => {
    const confirmed = await confirmDialog({
      title: 'Cancel Subscription',
      message: 'Are you sure you want to cancel your subscription? You will lose access to premium features at the end of your billing cycle.',
      confirmText: 'Cancel Subscription',
      cancelText: 'Keep Subscription'
    })
    
    if (confirmed) {
      try {
        await cancelMutation.mutateAsync({ 
          subscriptionId: currentSubscription.id,
          reason: 'user_requested'
        })
        toast.success('Subscription cancelled successfully')
        queryClient.invalidateQueries(['subscription'])
      } catch (error) {
        toast.error('Failed to cancel subscription. Please contact support.')
      }
    }
  }
  
  return (
    <div className="subscription-management">
      {/* Current Subscription Status */}
      <Card className="current-subscription">
        <CardHeader>
          <CardTitle>Current Plan</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="plan-info">
            <h3>{currentSubscription?.plan?.displayName}</h3>
            <p className="price">
              ₹{currentSubscription?.amount}/
              {currentSubscription?.billingCycle}
            </p>
            <p className="next-billing">
              Next billing: {formatDate(currentSubscription?.nextBillingDate)}
            </p>
          </div>
          
          <div className="plan-features">
            {currentSubscription?.plan?.features?.map(feature => (
              <div key={feature.key} className="feature">
                <CheckCircle className="text-green-500" />
                <span>{feature.name}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
      
      {/* Available Plans */}
      <div className="available-plans">
        <h2>Available Plans</h2>
        <div className="plans-grid">
          {availablePlans?.map(plan => (
            <PlanCard
              key={plan.id}
              plan={plan}
              currentPlan={currentSubscription?.planId === plan.id}
              onUpgrade={() => handleUpgrade(plan.id)}
              disabled={upgradeMutation.isLoading}
            />
          ))}
        </div>
      </div>
      
      {/* Usage Statistics */}
      <UsageStatistics subscriptionId={currentSubscription?.id} />
      
      {/* Billing History */}
      <BillingHistory userId={currentSubscription?.userId} />
      
      {/* Subscription Actions */}
      <div className="subscription-actions">
        <Button
          variant="outline"
          onClick={handleCancel}
          disabled={cancelMutation.isLoading}
        >
          Cancel Subscription
        </Button>
      </div>
    </div>
  )
}
```

## Acceptance Criteria

### Subscription Lifecycle Management
- [ ] **Plan Creation**: Support for multiple subscription tiers with different features
- [ ] **Activation**: Automatic activation upon successful payment
- [ ] **Renewal**: Automated billing cycle processing with 99% success rate
- [ ] **Upgrades/Downgrades**: Seamless plan changes with prorated billing

### Billing & Payment Integration
- [ ] **Automated Billing**: Monthly, quarterly, and annual billing cycles
- [ ] **Payment Processing**: Integration with payment gateway for recurring charges
- [ ] **Failed Payment Handling**: 3-attempt retry with grace period management
- [ ] **Proration**: Accurate prorated billing for plan changes

### User Management
- [ ] **Feature Access Control**: Real-time feature access based on subscription
- [ ] **Usage Tracking**: Monitor and enforce usage limits per plan
- [ ] **Notifications**: Email/SMS notifications for billing events
- [ ] **Self-Service**: User dashboard for subscription management

### Analytics & Reporting
- [ ] **MRR/ARR Tracking**: Accurate revenue calculation and forecasting
- [ ] **Churn Analysis**: Subscription cancellation tracking and analysis
- [ ] **Usage Analytics**: Feature usage patterns by subscription tier
- [ ] **Business Intelligence**: Dashboard for subscription metrics

## Testing Strategy

### Unit Tests
- Subscription lifecycle state management
- Billing calculation and proration logic
- Feature access control validation
- Usage limit enforcement

### Integration Tests
- Payment gateway integration for recurring billing
- Database transaction integrity
- Email/SMS notification delivery
- Feature flag system integration

### Performance Tests
- High-volume subscription processing
- Billing cycle batch processing
- Usage tracking performance
- Dashboard loading optimization

## Definition of Done
- [ ] All subscription tiers implemented with proper feature access
- [ ] Automated billing system processing recurring payments
- [ ] User dashboard for subscription management completed
- [ ] Admin dashboard for subscription analytics implemented
- [ ] Payment failure handling with retry logic working
- [ ] Usage tracking and limit enforcement operational
- [ ] Email/SMS notifications for all subscription events
- [ ] Proration calculations accurate for plan changes
- [ ] Performance testing completed (10K+ active subscriptions)
- [ ] Security audit passed for subscription data handling

## Story Points: 21

## Dependencies
- REV-001: Payment Gateway Integration (for processing recurring payments)
- User management system integration
- Notification service implementation
- Feature flag system for access control

## Notes
- Integration with existing user roles and permissions system
- Consideration for freemium model with usage-based limits
- Support for promotional pricing and discount codes
- Integration with analytics system for business intelligence