# Story REV-003: Feature Access Control & Usage Tracking

## Epic
Epic 5: Revenue Systems & Gamification

## Story Overview
**As a** TradeMaster platform  
**I want** to control feature access based on subscription tiers and track usage  
**So that** I can enforce subscription limits and provide usage analytics

## Business Value
- **Revenue Protection**: Prevent unauthorized access to premium features
- **Usage Analytics**: Data-driven insights for pricing optimization
- **Fair Usage**: Ensure equitable resource allocation across users
- **Upsell Opportunities**: Identify users approaching limits for upgrade prompts

## Technical Requirements

### Feature Access Control System
```java
@Service
@Transactional(readOnly = true)
public class FeatureAccessService {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UsageTrackingService usageTrackingService;
    
    @Autowired
    private FeatureRepository featureRepository;
    
    @Cacheable(value = "feature-access", key = "#userId + '_' + #featureKey")
    public FeatureAccessResult checkFeatureAccess(String userId, String featureKey) {
        
        // Get user's active subscription
        Subscription subscription = subscriptionService.getActiveSubscription(userId);
        if (subscription == null) {
            return FeatureAccessResult.builder()
                .hasAccess(false)
                .reason("No active subscription")
                .requiredPlan("pro")
                .build();
        }
        
        // Get feature definition
        Feature feature = featureRepository.findByKey(featureKey)
            .orElseThrow(() -> new FeatureNotFoundException(featureKey));
        
        // Check if feature is included in plan
        SubscriptionPlan plan = subscription.getPlan();
        if (!plan.getFeatures().contains(feature)) {
            return FeatureAccessResult.builder()
                .hasAccess(false)
                .reason("Feature not included in current plan")
                .requiredPlan(feature.getMinimumPlan())
                .build();
        }
        
        // Check usage limits if applicable
        if (feature.hasUsageLimits()) {
            UsageCheckResult usageCheck = usageTrackingService.checkUsageLimit(
                userId, featureKey, feature.getLimits()
            );
            
            if (!usageCheck.isWithinLimit()) {
                return FeatureAccessResult.builder()
                    .hasAccess(false)
                    .reason("Usage limit exceeded")
                    .currentUsage(usageCheck.getCurrentUsage())
                    .limit(usageCheck.getLimit())
                    .resetDate(usageCheck.getResetDate())
                    .build();
            }
        }
        
        return FeatureAccessResult.builder()
            .hasAccess(true)
            .build();
    }
    
    @CacheEvict(value = "feature-access", key = "#userId + '_*'")
    public void recordFeatureUsage(String userId, String featureKey, FeatureUsageContext context) {
        
        // Verify access before recording usage
        FeatureAccessResult accessResult = checkFeatureAccess(userId, featureKey);
        if (!accessResult.isHasAccess()) {
            throw new FeatureAccessDeniedException(
                "Access denied for feature: " + featureKey + 
                ". Reason: " + accessResult.getReason()
            );
        }
        
        // Record usage
        FeatureUsage usage = FeatureUsage.builder()
            .userId(userId)
            .featureKey(featureKey)
            .subscriptionId(subscriptionService.getActiveSubscription(userId).getId())
            .usageDate(LocalDate.now())
            .usageContext(context)
            .ipAddress(context.getIpAddress())
            .userAgent(context.getUserAgent())
            .build();
        
        usageTrackingService.recordUsage(usage);
        
        // Check if user is approaching limits
        checkAndNotifyApproachingLimits(userId, featureKey);
    }
    
    private void checkAndNotifyApproachingLimits(String userId, String featureKey) {
        Feature feature = featureRepository.findByKey(featureKey).orElse(null);
        if (feature == null || !feature.hasUsageLimits()) {
            return;
        }
        
        UsageCheckResult currentUsage = usageTrackingService.checkUsageLimit(
            userId, featureKey, feature.getLimits()
        );
        
        // Notify when 80% of limit is reached
        double usagePercentage = (double) currentUsage.getCurrentUsage() / currentUsage.getLimit();
        if (usagePercentage >= 0.8 && usagePercentage < 1.0) {
            notificationService.sendUsageLimitWarning(userId, featureKey, currentUsage);
        }
    }
}

@Service
@Transactional
public class UsageTrackingService {
    
    @Autowired
    private FeatureUsageRepository usageRepository;
    
    public void recordUsage(FeatureUsage usage) {
        // Aggregate daily usage
        Optional<DailyUsageAggregate> existing = usageRepository
            .findByUserIdAndFeatureKeyAndDate(
                usage.getUserId(), 
                usage.getFeatureKey(), 
                usage.getUsageDate()
            );
        
        if (existing.isPresent()) {
            existing.get().incrementCount();
            usageRepository.save(existing.get());
        } else {
            DailyUsageAggregate aggregate = DailyUsageAggregate.builder()
                .userId(usage.getUserId())
                .featureKey(usage.getFeatureKey())
                .subscriptionId(usage.getSubscriptionId())
                .usageDate(usage.getUsageDate())
                .usageCount(1)
                .build();
            usageRepository.save(aggregate);
        }
        
        // Store detailed usage for analytics
        usageRepository.save(usage);
    }
    
    public UsageCheckResult checkUsageLimit(String userId, String featureKey, FeatureLimits limits) {
        LocalDate startDate = calculatePeriodStart(limits.getPeriod());
        LocalDate endDate = LocalDate.now();
        
        Integer currentUsage = usageRepository.countUsageInPeriod(
            userId, featureKey, startDate, endDate
        );
        
        Integer limit = getLimitForPeriod(limits, limits.getPeriod());
        
        return UsageCheckResult.builder()
            .currentUsage(currentUsage)
            .limit(limit)
            .isWithinLimit(currentUsage < limit)
            .resetDate(calculateNextReset(limits.getPeriod()))
            .build();
    }
}
```

### Feature Definition System
```java
@Entity
@Table(name = "features")
public class Feature {
    
    @Id
    private String key; // e.g., "real_time_data", "ai_insights"
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private FeatureCategory category;
    
    @Column(name = "minimum_plan")
    private String minimumPlan; // free, pro, premium, enterprise
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private FeatureLimits limits;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Usage tracking settings
    @Column(name = "track_usage")
    private Boolean trackUsage = true;
    
    @Column(name = "notify_on_limit")
    private Boolean notifyOnLimit = true;
}

@Embeddable
public class FeatureLimits {
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private Integer totalLimit;
    
    @Enumerated(EnumType.STRING)
    private LimitPeriod period = LimitPeriod.DAILY;
    
    // Soft limits for warnings
    private Integer softLimitPercentage = 80; // 80% warning threshold
}

public enum FeatureCategory {
    CORE,           // Basic platform features
    MARKET_DATA,    // Real-time data, advanced charts
    TRADING,        // Order execution, advanced orders
    ANALYTICS,      // Portfolio analytics, performance reports  
    AI_ML,          // AI insights, pattern recognition
    NOTIFICATIONS,  // Email, SMS, push notifications
    API,            // API access, webhooks
    SUPPORT         // Priority support, dedicated manager
}
```

### Database Schema
```sql
-- Features Definition
CREATE TABLE features (
    key VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category feature_category NOT NULL,
    minimum_plan plan_tier NOT NULL DEFAULT 'free',
    
    -- Usage Limits
    limits JSONB DEFAULT '{}',
    
    -- Configuration
    is_active BOOLEAN DEFAULT true,
    track_usage BOOLEAN DEFAULT true,
    notify_on_limit BOOLEAN DEFAULT true,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Daily Usage Aggregates
CREATE TABLE daily_usage_aggregates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES subscriptions(id),
    feature_key VARCHAR(50) NOT NULL REFERENCES features(key),
    
    -- Usage Data
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    usage_count INTEGER NOT NULL DEFAULT 1,
    
    -- Context
    subscription_plan VARCHAR(50),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, feature_key, usage_date)
);

-- Detailed Feature Usage (for analytics)
CREATE TABLE feature_usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES subscriptions(id),
    feature_key VARCHAR(50) NOT NULL REFERENCES features(key),
    
    -- Usage Context
    usage_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT,
    
    -- Request Context
    request_details JSONB DEFAULT '{}',
    response_details JSONB DEFAULT '{}',
    
    -- Performance Metrics
    processing_time_ms INTEGER,
    success BOOLEAN DEFAULT true,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Feature Access Audit
CREATE TABLE feature_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    feature_key VARCHAR(50) NOT NULL REFERENCES features(key),
    
    -- Access Details
    access_granted BOOLEAN NOT NULL,
    denial_reason TEXT,
    subscription_plan VARCHAR(50),
    
    -- Context
    access_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Custom Types
CREATE TYPE feature_category AS ENUM (
    'core', 'market_data', 'trading', 'analytics', 
    'ai_ml', 'notifications', 'api', 'support'
);

CREATE TYPE plan_tier AS ENUM ('free', 'pro', 'premium', 'enterprise');

-- Indexes for Performance
CREATE INDEX idx_daily_usage_user_feature_date ON daily_usage_aggregates(user_id, feature_key, usage_date);
CREATE INDEX idx_feature_usage_user_timestamp ON feature_usage_logs(user_id, usage_timestamp);
CREATE INDEX idx_feature_access_user_timestamp ON feature_access_logs(user_id, access_timestamp);
```

### REST API Implementation
```java
@RestController
@RequestMapping("/api/v1/features")
@Validated
public class FeatureAccessController {
    
    @Autowired
    private FeatureAccessService featureAccessService;
    
    @GetMapping("/{featureKey}/access")
    public ResponseEntity<FeatureAccessResponse> checkFeatureAccess(
            @PathVariable String featureKey,
            @RequestHeader("Authorization") String token) {
        
        String userId = jwtService.extractUserId(token);
        
        FeatureAccessResult result = featureAccessService.checkFeatureAccess(userId, featureKey);
        
        return ResponseEntity.ok(FeatureAccessResponse.builder()
            .hasAccess(result.isHasAccess())
            .reason(result.getReason())
            .currentUsage(result.getCurrentUsage())
            .limit(result.getLimit())
            .resetDate(result.getResetDate())
            .requiredPlan(result.getRequiredPlan())
            .build());
    }
    
    @PostMapping("/{featureKey}/usage")
    public ResponseEntity<Void> recordUsage(
            @PathVariable String featureKey,
            @RequestBody @Valid FeatureUsageRequest request,
            @RequestHeader("Authorization") String token,
            HttpServletRequest httpRequest) {
        
        String userId = jwtService.extractUserId(token);
        
        FeatureUsageContext context = FeatureUsageContext.builder()
            .requestDetails(request.getRequestDetails())
            .ipAddress(getClientIpAddress(httpRequest))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .build();
        
        featureAccessService.recordFeatureUsage(userId, featureKey, context);
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/usage/summary")
    public ResponseEntity<UsageSummaryResponse> getUsageSummary(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "30") int days) {
        
        String userId = jwtService.extractUserId(token);
        
        UsageSummary summary = featureAccessService.getUserUsageSummary(userId, days);
        
        return ResponseEntity.ok(UsageSummaryResponse.from(summary));
    }
    
    @GetMapping("/limits")
    public ResponseEntity<List<FeatureLimitResponse>> getFeatureLimits(
            @RequestHeader("Authorization") String token) {
        
        String userId = jwtService.extractUserId(token);
        
        List<FeatureLimitInfo> limits = featureAccessService.getUserFeatureLimits(userId);
        
        return ResponseEntity.ok(
            limits.stream()
                .map(FeatureLimitResponse::from)
                .collect(Collectors.toList())
        );
    }
}
```

### Aspect-Based Access Control
```java
@Aspect
@Component
public class FeatureAccessAspect {
    
    @Autowired
    private FeatureAccessService featureAccessService;
    
    @Around("@annotation(requiresFeature)")
    public Object checkFeatureAccess(ProceedingJoinPoint joinPoint, RequiresFeature requiresFeature) throws Throwable {
        
        // Extract user ID from security context
        String userId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        
        // Check feature access
        FeatureAccessResult accessResult = featureAccessService.checkFeatureAccess(
            userId, 
            requiresFeature.value()
        );
        
        if (!accessResult.isHasAccess()) {
            throw new FeatureAccessDeniedException(
                "Access denied for feature: " + requiresFeature.value() + 
                ". Reason: " + accessResult.getReason()
            );
        }
        
        // Record usage if tracking is enabled
        if (requiresFeature.trackUsage()) {
            FeatureUsageContext context = buildUsageContext(joinPoint);
            featureAccessService.recordFeatureUsage(userId, requiresFeature.value(), context);
        }
        
        return joinPoint.proceed();
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresFeature {
    String value(); // Feature key
    boolean trackUsage() default true;
}

// Usage Example
@RestController
public class TradingController {
    
    @RequiresFeature("real_time_data")
    @GetMapping("/api/v1/market-data/realtime")
    public ResponseEntity<MarketData> getRealTimeData() {
        // This method requires real-time data access
        // Access control and usage tracking handled by aspect
    }
    
    @RequiresFeature("ai_insights")
    @PostMapping("/api/v1/ai/insights")
    public ResponseEntity<AiInsight> getAiInsights(@RequestBody InsightRequest request) {
        // This method requires AI insights access
        // Usage will be tracked automatically
    }
}
```

## Acceptance Criteria

### Access Control
- [ ] **Feature-based Access**: Control access to specific features based on subscription plans
- [ ] **Real-time Validation**: Sub-100ms feature access validation
- [ ] **Usage Limits**: Enforce daily, monthly, and total usage limits
- [ ] **Graceful Denial**: Clear messaging when access is denied

### Usage Tracking
- [ ] **Comprehensive Logging**: Track all feature usage with context
- [ ] **Performance Metrics**: Monitor feature usage performance impact
- [ ] **Analytics Integration**: Provide usage data for business analytics
- [ ] **Audit Trail**: Complete audit trail for compliance

### User Experience
- [ ] **Limit Warnings**: Notify users when approaching limits (80% threshold)
- [ ] **Usage Dashboard**: Self-service usage monitoring for users
- [ ] **Upgrade Prompts**: Contextual upgrade suggestions when limits reached
- [ ] **Fair Usage**: Prevent abuse while supporting legitimate usage

### System Performance
- [ ] **Caching Strategy**: Redis caching for frequently accessed permissions
- [ ] **Database Optimization**: Optimized queries for usage tracking
- [ ] **Async Processing**: Non-blocking usage recording
- [ ] **Scalability**: Support 100K+ users with usage tracking

## Testing Strategy

### Unit Tests
- Feature access validation logic
- Usage limit calculation accuracy
- Cache invalidation strategies
- Audit logging completeness

### Integration Tests
- Database query performance
- Cache consistency validation
- API endpoint functionality
- Aspect-based access control

### Performance Tests
- Access validation latency (<100ms)
- Usage tracking throughput (10K+ requests/minute)
- Cache hit ratio optimization (>90%)
- Database query optimization

### Security Tests
- Authorization bypass prevention
- Usage data privacy validation
- Audit trail integrity
- Rate limiting effectiveness

## Definition of Done
- [ ] Feature-based access control implemented and tested
- [ ] Usage tracking system recording all feature usage
- [ ] API endpoints for access validation and usage monitoring
- [ ] Aspect-based access control for automatic enforcement
- [ ] User dashboard for usage monitoring implemented
- [ ] Admin dashboard for usage analytics completed
- [ ] Performance testing passed (sub-100ms access checks)
- [ ] Security audit completed for access control system
- [ ] Documentation for feature configuration and usage
- [ ] Monitoring and alerting for system health

## Story Points: 13

## Dependencies
- REV-002: Subscription Management Service (for subscription plan access)
- User authentication system
- Redis caching infrastructure
- Notification service for limit warnings

## Notes
- Integration with existing API Gateway for centralized access control
- Consider implementation of soft limits vs hard limits for different features
- Support for temporary feature access (promotions, trials)
- Integration with analytics system for usage pattern analysis