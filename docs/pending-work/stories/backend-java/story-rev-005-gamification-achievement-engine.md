# Story REV-005: Gamification Achievement Engine

## Epic
Epic 5: Revenue Systems & Gamification

## Story Overview
**As a** TradeMaster user  
**I want** to earn achievements and rewards for trading activities  
**So that** I stay engaged and motivated to improve my trading skills

## Business Value
- **User Engagement**: 40% increase in daily active users through gamification
- **Retention**: 60% improvement in 30-day retention rates
- **Learning Incentives**: Encourage best practices and skill development
- **Community Building**: Foster competitive and collaborative trading environment

## Technical Requirements

### Achievement System Architecture
```java
@Service
@Transactional
public class AchievementService {
    
    @Autowired
    private AchievementRepository achievementRepository;
    
    @Autowired
    private UserAchievementRepository userAchievementRepository;
    
    @Autowired
    private TradingActivityService tradingActivityService;
    
    @Autowired
    private NotificationService notificationService;
    
    @EventListener
    @Async
    public void handleTradingEvent(TradingEvent event) {
        try {
            processAchievements(event.getUserId(), event);
        } catch (Exception e) {
            log.error("Failed to process achievements for user: " + event.getUserId(), e);
        }
    }
    
    public void processAchievements(String userId, TradingEvent event) {
        
        // Get all active achievements
        List<Achievement> activeAchievements = achievementRepository.findByStatus(AchievementStatus.ACTIVE);
        
        // Get user's current achievements
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        Set<String> completedAchievements = userAchievements.stream()
            .filter(ua -> ua.getStatus() == UserAchievementStatus.COMPLETED)
            .map(ua -> ua.getAchievementId())
            .collect(Collectors.toSet());
        
        for (Achievement achievement : activeAchievements) {
            // Skip if already completed
            if (completedAchievements.contains(achievement.getId())) {
                continue;
            }
            
            // Check if event is relevant for this achievement
            if (!isEventRelevant(achievement, event)) {
                continue;
            }
            
            // Process achievement progress
            AchievementProgress progress = calculateProgress(userId, achievement, event);
            
            if (progress.isCompleted()) {
                unlockAchievement(userId, achievement, progress);
            } else {
                updateProgress(userId, achievement, progress);
            }
        }
    }
    
    private void unlockAchievement(String userId, Achievement achievement, AchievementProgress progress) {
        
        // Create user achievement record
        UserAchievement userAchievement = UserAchievement.builder()
            .userId(userId)
            .achievementId(achievement.getId())
            .status(UserAchievementStatus.COMPLETED)
            .completedAt(LocalDateTime.now())
            .progress(progress.getCurrentValue())
            .targetValue(progress.getTargetValue())
            .build();
        
        userAchievementRepository.save(userAchievement);
        
        // Award points and rewards
        awardPoints(userId, achievement.getPointsReward());
        processRewards(userId, achievement);
        
        // Send notification
        notificationService.sendAchievementUnlocked(userId, achievement);
        
        // Check for badge upgrades
        checkBadgeUpgrades(userId, achievement);
        
        log.info("Achievement unlocked: {} for user: {}", achievement.getName(), userId);
    }
    
    private AchievementProgress calculateProgress(String userId, Achievement achievement, TradingEvent event) {
        
        return switch (achievement.getType()) {
            case TRADE_COUNT -> calculateTradeCountProgress(userId, achievement);
            case PROFIT_AMOUNT -> calculateProfitProgress(userId, achievement);
            case STREAK_DAYS -> calculateStreakProgress(userId, achievement);
            case PORTFOLIO_VALUE -> calculatePortfolioProgress(userId, achievement);
            case LEARNING_MODULES -> calculateLearningProgress(userId, achievement);
            case REFERRAL_COUNT -> calculateReferralProgress(userId, achievement);
            case CONSISTENCY -> calculateConsistencyProgress(userId, achievement);
            default -> throw new UnsupportedAchievementTypeException(achievement.getType());
        };
    }
    
    private AchievementProgress calculateTradeCountProgress(String userId, Achievement achievement) {
        
        LocalDateTime startDate = getAchievementStartDate(achievement);
        int tradeCount = tradingActivityService.getTradeCount(userId, startDate, LocalDateTime.now());
        int targetCount = achievement.getTargetValue();
        
        return AchievementProgress.builder()
            .currentValue(tradeCount)
            .targetValue(targetCount)
            .percentage((tradeCount * 100) / targetCount)
            .completed(tradeCount >= targetCount)
            .build();
    }
    
    private AchievementProgress calculateProfitProgress(String userId, Achievement achievement) {
        
        LocalDateTime startDate = getAchievementStartDate(achievement);
        BigDecimal totalProfit = tradingActivityService.getTotalProfit(userId, startDate, LocalDateTime.now());
        BigDecimal targetProfit = new BigDecimal(achievement.getTargetValue());
        
        return AchievementProgress.builder()
            .currentValue(totalProfit.intValue())
            .targetValue(targetProfit.intValue())
            .percentage((totalProfit.multiply(new BigDecimal(100)).divide(targetProfit, 0, RoundingMode.HALF_UP)).intValue())
            .completed(totalProfit.compareTo(targetProfit) >= 0)
            .build();
    }
    
    public LeaderboardResponse getLeaderboard(LeaderboardType type, String period) {
        
        LocalDateTime startDate = calculatePeriodStart(period);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<LeaderboardEntry> entries = switch (type) {
            case POINTS -> getPointsLeaderboard(startDate, endDate);
            case PROFITS -> getProfitsLeaderboard(startDate, endDate);
            case TRADES -> getTradesLeaderboard(startDate, endDate);
            case ACHIEVEMENTS -> getAchievementsLeaderboard(startDate, endDate);
        };
        
        return LeaderboardResponse.builder()
            .type(type)
            .period(period)
            .entries(entries)
            .lastUpdated(LocalDateTime.now())
            .build();
    }
}

@Service
public class BadgeService {
    
    public void checkBadgeUpgrades(String userId) {
        
        List<UserAchievement> userAchievements = userAchievementRepository.findCompletedByUserId(userId);
        List<Badge> availableBadges = badgeRepository.findAll();
        
        for (Badge badge : availableBadges) {
            if (isEligibleForBadge(userAchievements, badge)) {
                awardBadge(userId, badge);
            }
        }
    }
    
    private boolean isEligibleForBadge(List<UserAchievement> userAchievements, Badge badge) {
        
        return switch (badge.getType()) {
            case CATEGORY_MASTER -> hasCompletedCategoryAchievements(userAchievements, badge.getRequiredCategory());
            case MILESTONE_REACHED -> hasReachedMilestone(userAchievements, badge.getRequiredMilestone());
            case STREAK_CHAMPION -> hasStreakAchievements(userAchievements, badge.getRequiredStreak());
            case ALL_ROUNDER -> hasAllRounderAchievements(userAchievements);
        };
    }
}
```

### Achievement Definitions
```yaml
# Achievement Categories and Definitions
trading_achievements:
  first_trade:
    name: "First Steps"
    description: "Execute your first trade"
    type: "TRADE_COUNT"
    target_value: 1
    points_reward: 100
    category: "BEGINNER"
    
  trade_milestone_10:
    name: "Getting Started"
    description: "Complete 10 trades"
    type: "TRADE_COUNT"
    target_value: 10
    points_reward: 250
    category: "BEGINNER"
    
  trade_milestone_100:
    name: "Active Trader"
    description: "Complete 100 trades"
    type: "TRADE_COUNT"
    target_value: 100
    points_reward: 1000
    category: "INTERMEDIATE"
    
  profitable_week:
    name: "Weekly Winner"
    description: "Make profit for 7 consecutive days"
    type: "STREAK_DAYS"
    target_value: 7
    points_reward: 500
    category: "PERFORMANCE"

profit_achievements:
  first_profit:
    name: "First Gains"
    description: "Earn your first ₹100 in profit"
    type: "PROFIT_AMOUNT"
    target_value: 100
    points_reward: 200
    category: "PERFORMANCE"
    
  profit_milestone_10k:
    name: "Rising Star"
    description: "Earn ₹10,000 in total profit"
    type: "PROFIT_AMOUNT"
    target_value: 10000
    points_reward: 2000
    category: "PERFORMANCE"
    
  profit_milestone_100k:
    name: "Profit Master"
    description: "Earn ₹1,00,000 in total profit"
    type: "PROFIT_AMOUNT"
    target_value: 100000
    points_reward: 10000
    category: "ADVANCED"

learning_achievements:
  course_completion:
    name: "Knowledge Seeker"
    description: "Complete 5 learning modules"
    type: "LEARNING_MODULES"
    target_value: 5
    points_reward: 300
    category: "EDUCATION"
    
  risk_management:
    name: "Risk Aware"
    description: "Set stop-loss on 10 consecutive trades"
    type: "RISK_MANAGEMENT"
    target_value: 10
    points_reward: 400
    category: "EDUCATION"

social_achievements:
  first_referral:
    name: "Community Builder"
    description: "Refer your first friend"
    type: "REFERRAL_COUNT"
    target_value: 1
    points_reward: 500
    category: "SOCIAL"
    
  referral_master:
    name: "Influencer"
    description: "Refer 10 successful traders"
    type: "REFERRAL_COUNT"
    target_value: 10
    points_reward: 5000
    category: "SOCIAL"

consistency_achievements:
  daily_trader:
    name: "Daily Trader"
    description: "Trade for 30 consecutive days"
    type: "CONSISTENCY"
    target_value: 30
    points_reward: 1500
    category: "CONSISTENCY"
    
  portfolio_builder:
    name: "Portfolio Builder"
    description: "Maintain portfolio value above ₹1L for 30 days"
    type: "PORTFOLIO_VALUE"
    target_value: 100000
    points_reward: 2500
    category: "ADVANCED"
```

### Database Schema
```sql
-- Achievements Master
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Basic Info
    key VARCHAR(50) NOT NULL UNIQUE, -- e.g., 'first_trade', 'profit_10k'
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    
    -- Achievement Logic
    type achievement_type NOT NULL,
    category achievement_category NOT NULL,
    target_value INTEGER NOT NULL,
    
    -- Rewards
    points_reward INTEGER NOT NULL DEFAULT 0,
    badge_reward VARCHAR(50),
    feature_unlock VARCHAR(50),
    
    -- Configuration
    is_repeatable BOOLEAN DEFAULT false,
    difficulty_level difficulty_level DEFAULT 'beginner',
    time_period_days INTEGER, -- null for lifetime achievements
    
    -- Display
    icon_url VARCHAR(200),
    color_scheme VARCHAR(20),
    sort_order INTEGER DEFAULT 0,
    
    -- Status
    status achievement_status DEFAULT 'active',
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User Achievements
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    user_id UUID NOT NULL REFERENCES users(id),
    achievement_id UUID NOT NULL REFERENCES achievements(id),
    
    -- Progress
    status user_achievement_status DEFAULT 'in_progress',
    progress INTEGER DEFAULT 0,
    target_value INTEGER NOT NULL,
    
    -- Completion Details
    completed_at TIMESTAMP,
    points_earned INTEGER DEFAULT 0,
    
    -- Context
    completion_context JSONB DEFAULT '{}',
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, achievement_id)
);

-- User Points and Badges
CREATE TABLE user_gamification_profile (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    
    -- Points
    total_points INTEGER DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    points_to_next_level INTEGER DEFAULT 1000,
    
    -- Badges
    total_badges INTEGER DEFAULT 0,
    featured_badge VARCHAR(50),
    
    -- Statistics
    achievements_completed INTEGER DEFAULT 0,
    current_streak_days INTEGER DEFAULT 0,
    longest_streak_days INTEGER DEFAULT 0,
    
    -- Leaderboard Position
    points_rank INTEGER,
    profits_rank INTEGER,
    
    -- Settings
    notifications_enabled BOOLEAN DEFAULT true,
    public_profile BOOLEAN DEFAULT false,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Badges
CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Basic Info
    key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    
    -- Badge Properties
    type badge_type NOT NULL,
    rarity badge_rarity DEFAULT 'common',
    
    -- Requirements
    required_achievements TEXT[], -- Array of achievement keys
    required_points INTEGER DEFAULT 0,
    required_level INTEGER DEFAULT 1,
    
    -- Display
    icon_url VARCHAR(200),
    color_scheme VARCHAR(20),
    
    -- Status
    status badge_status DEFAULT 'active',
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User Badges
CREATE TABLE user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    user_id UUID NOT NULL REFERENCES users(id),
    badge_id UUID NOT NULL REFERENCES badges(id),
    
    -- Award Details
    awarded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    award_reason TEXT,
    
    -- Display
    is_featured BOOLEAN DEFAULT false,
    display_order INTEGER DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, badge_id)
);

-- Achievement Events Log
CREATE TABLE achievement_events_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event Details
    user_id UUID NOT NULL REFERENCES users(id),
    achievement_id UUID NOT NULL REFERENCES achievements(id),
    event_type achievement_event_type NOT NULL,
    
    -- Progress Details
    previous_progress INTEGER DEFAULT 0,
    new_progress INTEGER NOT NULL,
    points_awarded INTEGER DEFAULT 0,
    
    -- Context
    triggering_event_type VARCHAR(50), -- 'trade_executed', 'profit_earned', etc.
    triggering_event_data JSONB DEFAULT '{}',
    
    -- Timestamp
    event_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Custom Types
CREATE TYPE achievement_type AS ENUM (
    'trade_count', 'profit_amount', 'streak_days', 'portfolio_value',
    'learning_modules', 'referral_count', 'consistency', 'risk_management'
);

CREATE TYPE achievement_category AS ENUM (
    'beginner', 'intermediate', 'advanced', 'performance', 
    'education', 'social', 'consistency', 'special'
);

CREATE TYPE user_achievement_status AS ENUM (
    'not_started', 'in_progress', 'completed', 'expired'
);

CREATE TYPE achievement_status AS ENUM ('active', 'deprecated', 'seasonal');

CREATE TYPE difficulty_level AS ENUM ('beginner', 'intermediate', 'advanced', 'expert');

CREATE TYPE badge_type AS ENUM ('milestone', 'category_master', 'streak_champion', 'special_event');

CREATE TYPE badge_rarity AS ENUM ('common', 'rare', 'epic', 'legendary');

CREATE TYPE badge_status AS ENUM ('active', 'deprecated', 'seasonal');

CREATE TYPE achievement_event_type AS ENUM (
    'progress_updated', 'completed', 'points_awarded', 'badge_earned'
);

-- Indexes for Performance
CREATE INDEX idx_user_achievements_user_status ON user_achievements(user_id, status);
CREATE INDEX idx_user_achievements_completed ON user_achievements(completed_at DESC) WHERE status = 'completed';
CREATE INDEX idx_achievements_category_status ON achievements(category, status);
CREATE INDEX idx_gamification_profile_points ON user_gamification_profile(total_points DESC);
CREATE INDEX idx_achievement_events_user_timestamp ON achievement_events_log(user_id, event_timestamp DESC);
```

### Frontend Integration
```typescript
// Achievement Hook
export const useAchievements = (userId: string) => {
  const { data: achievements } = useQuery(
    ['achievements', userId],
    () => achievementApi.getUserAchievements(userId),
    { staleTime: 5 * 60 * 1000 } // 5 minutes
  )
  
  const { data: progress } = useQuery(
    ['achievement-progress', userId],
    () => achievementApi.getAchievementProgress(userId),
    { refetchInterval: 30000 } // 30 seconds
  )
  
  const { data: leaderboard } = useQuery(
    ['leaderboard', 'points', 'weekly'],
    () => achievementApi.getLeaderboard('points', 'weekly')
  )
  
  return { achievements, progress, leaderboard }
}

// Achievement Notification Component
export const AchievementNotification: React.FC = () => {
  const [notification, setNotification] = useState<AchievementNotification | null>(null)
  
  useEffect(() => {
    const eventSource = new EventSource('/api/v1/achievements/events')
    
    eventSource.addEventListener('achievement-unlocked', (event) => {
      const achievement = JSON.parse(event.data)
      setNotification({
        type: 'achievement-unlocked',
        achievement,
        show: true
      })
      
      // Show for 5 seconds
      setTimeout(() => setNotification(null), 5000)
    })
    
    return () => eventSource.close()
  }, [])
  
  if (!notification?.show) return null
  
  return (
    <motion.div
      initial={{ opacity: 0, y: -100 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -100 }}
      className="achievement-notification"
    >
      <div className="achievement-content">
        <Trophy className="achievement-icon" />
        <div>
          <h3>Achievement Unlocked!</h3>
          <p>{notification.achievement.name}</p>
          <span>+{notification.achievement.pointsReward} points</span>
        </div>
      </div>
    </motion.div>
  )
}
```

## Acceptance Criteria

### Achievement System
- [ ] **Dynamic Processing**: Real-time achievement progress tracking
- [ ] **Multiple Categories**: Support for trading, learning, social, and consistency achievements
- [ ] **Progress Tracking**: Detailed progress indicators for all achievements
- [ ] **Point System**: Comprehensive point-based reward system

### User Engagement
- [ ] **Real-time Notifications**: Instant notifications when achievements are unlocked
- [ ] **Progress Visualization**: Visual progress bars and completion indicators
- [ ] **Leaderboards**: Weekly, monthly, and all-time leaderboards
- [ ] **Badge System**: Collectible badges for milestone achievements

### Gamification Features
- [ ] **Level System**: User levels based on total points earned
- [ ] **Streak Tracking**: Daily trading streaks with rewards
- [ ] **Social Features**: Friend challenges and community achievements
- [ ] **Seasonal Events**: Special achievements for events and competitions

### Performance
- [ ] **Real-time Processing**: Sub-500ms achievement processing
- [ ] **Scalable Design**: Support for 100K+ active users
- [ ] **Cache Optimization**: Efficient caching for leaderboards and progress
- [ ] **Event-driven Architecture**: Asynchronous processing for better performance

## Testing Strategy

### Unit Tests
- Achievement calculation logic accuracy
- Progress tracking algorithms
- Point and badge award systems
- Leaderboard ranking calculations

### Integration Tests
- Trading event integration
- Real-time notification delivery
- Database consistency validation
- Cache synchronization testing

### Performance Tests
- Achievement processing latency
- Leaderboard query performance
- Real-time event handling throughput
- Database optimization validation

### User Experience Tests
- Achievement notification timing
- Progress visualization accuracy
- Mobile responsiveness
- Accessibility compliance

## Definition of Done
- [ ] Achievement system processing all trading events in real-time
- [ ] User dashboard showing achievements and progress completed
- [ ] Real-time notifications for achievement unlocks working
- [ ] Leaderboard system with rankings operational
- [ ] Badge collection and display system implemented
- [ ] Point-based leveling system functional
- [ ] Admin dashboard for achievement management completed
- [ ] Performance testing passed (sub-500ms processing)
- [ ] Mobile-responsive achievement interfaces deployed
- [ ] Analytics tracking for engagement metrics implemented

## Story Points: 15

## Dependencies
- Trading activity tracking system
- Real-time notification infrastructure
- User profile system integration
- Analytics system for engagement tracking

## Notes
- Integration with social sharing for achievement announcements
- Consideration for seasonal and special event achievements
- Support for achievement categories based on user skill levels
- Integration with referral system for social achievements