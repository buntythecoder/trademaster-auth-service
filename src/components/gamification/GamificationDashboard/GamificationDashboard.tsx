import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Tabs,
  Tab,
  LinearProgress,
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  ListItemSecondaryAction,
  Badge,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Tooltip,
  Divider,
  Paper,
  Stack,
  CircularProgress,
  Alert,
  AlertTitle
} from '@mui/material';
import {
  EmojiEvents as TrophyIcon,
  Star as StarIcon,
  Whatshot as FireIcon,
  TrendingUp as TrendingUpIcon,
  People as PeopleIcon,
  Timeline as TimelineIcon,
  Grade as GradeIcon,
  Military_tech as MedalIcon,
  Groups as GroupsIcon,
  Speed as SpeedIcon,
  Target as TargetIcon,
  Celebration as CelebrationIcon,
  MonetizationOn as CoinIcon,
  LocalFireDepartment as StreakIcon,
  WorkspacePremium as PremiumIcon,
  Add as AddIcon,
  Close as CloseIcon,
  Share as ShareIcon,
  Download as DownloadIcon
} from '@mui/icons-material';
import { motion, AnimatePresence } from 'framer-motion';

// Enhanced Interfaces for Gamification
interface Achievement {
  id: string;
  title: string;
  description: string;
  icon: string;
  category: 'trading' | 'learning' | 'social' | 'milestone' | 'streak' | 'performance';
  tier: 'bronze' | 'silver' | 'gold' | 'platinum' | 'diamond';
  points: number;
  unlockedAt?: Date;
  progress: number;
  maxProgress: number;
  rarity: 'common' | 'rare' | 'epic' | 'legendary' | 'mythic';
  prerequisites?: string[];
  reward?: AchievementReward;
}

interface AchievementReward {
  type: 'points' | 'badge' | 'feature_unlock' | 'discount' | 'premium_time';
  value: number | string;
  description: string;
}

interface UserLevel {
  currentLevel: number;
  currentXP: number;
  nextLevelXP: number;
  totalXP: number;
  levelTitle: string;
  benefits: string[];
  prestigeLevel?: number;
}

interface LeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  avatar: string;
  points: number;
  level: number;
  achievements: number;
  winRate: number;
  streak: number;
  change: number; // rank change from previous period
  isCurrentUser: boolean;
  badges: Badge[];
  performance: LeaderboardPerformance;
}

interface LeaderboardPerformance {
  totalTrades: number;
  profitableTradesPercent: number;
  averageReturn: number;
  consistency: number;
  riskAdjustedReturn: number;
}

interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string;
  color: string;
  earnedAt: Date;
  category: 'skill' | 'achievement' | 'special' | 'seasonal' | 'exclusive';
  displayOrder: number;
}

interface Challenge {
  id: string;
  title: string;
  description: string;
  type: 'daily' | 'weekly' | 'monthly' | 'special' | 'community';
  difficulty: 'easy' | 'medium' | 'hard' | 'expert' | 'legendary';
  reward: ChallengeReward;
  progress: number;
  maxProgress: number;
  deadline: Date;
  participants: number;
  completedBy: number;
  status: 'active' | 'completed' | 'failed' | 'expired';
  requirements: string[];
}

interface ChallengeReward {
  points: number;
  badges?: string[];
  specialReward?: string;
  multiplier?: number;
}

interface PointsBreakdown {
  category: string;
  points: number;
  percentage: number;
  trend: number;
  activities: PointActivity[];
}

interface PointActivity {
  id: string;
  description: string;
  points: number;
  timestamp: Date;
  category: string;
  multiplier?: number;
}

interface SocialFeature {
  type: 'friend_challenge' | 'group_competition' | 'community_goal' | 'leaderboard_battle';
  title: string;
  description: string;
  participants: string[];
  status: 'pending' | 'active' | 'completed';
  reward: string;
  progress: number;
  maxProgress: number;
  deadline?: Date;
}

interface GamificationStats {
  totalPoints: number;
  rank: number;
  level: UserLevel;
  achievements: Achievement[];
  badges: Badge[];
  streaks: StreakInfo[];
  challenges: Challenge[];
  social: SocialFeature[];
  pointsHistory: PointsBreakdown[];
}

interface StreakInfo {
  type: 'daily_login' | 'trading_days' | 'profitable_days' | 'learning_streak';
  current: number;
  longest: number;
  multiplier: number;
  nextReward: string;
}

const GamificationDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [gamificationData, setGamificationData] = useState<GamificationStats | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [selectedAchievement, setSelectedAchievement] = useState<Achievement | null>(null);
  const [selectedChallenge, setSelectedChallenge] = useState<Challenge | null>(null);
  const [achievementDialogOpen, setAchievementDialogOpen] = useState(false);
  const [challengeDialogOpen, setChallengeDialogOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [notifications, setNotifications] = useState<string[]>([]);

  // Mock data generation
  const generateMockData = useCallback((): GamificationStats => {
    const mockLevel: UserLevel = {
      currentLevel: 12,
      currentXP: 2840,
      nextLevelXP: 3200,
      totalXP: 18640,
      levelTitle: "Expert Trader",
      benefits: [
        "Advanced Analytics Access",
        "Priority Support",
        "Exclusive Webinars",
        "Custom Indicators"
      ],
      prestigeLevel: 1
    };

    const mockAchievements: Achievement[] = [
      {
        id: 'first_profit',
        title: 'First Profit',
        description: 'Make your first profitable trade',
        icon: 'trending_up',
        category: 'milestone',
        tier: 'bronze',
        points: 100,
        unlockedAt: new Date('2024-01-15'),
        progress: 1,
        maxProgress: 1,
        rarity: 'common',
        reward: { type: 'points', value: 100, description: '+100 XP bonus' }
      },
      {
        id: 'week_warrior',
        title: 'Week Warrior',
        description: 'Trade every day for a week',
        icon: 'calendar_today',
        category: 'streak',
        tier: 'silver',
        points: 250,
        unlockedAt: new Date('2024-01-22'),
        progress: 7,
        maxProgress: 7,
        rarity: 'rare',
        reward: { type: 'feature_unlock', value: 'advanced_charts', description: 'Unlock Advanced Charts' }
      },
      {
        id: 'profit_master',
        title: 'Profit Master',
        description: 'Achieve 10 consecutive profitable trades',
        icon: 'star',
        category: 'performance',
        tier: 'gold',
        points: 500,
        unlockedAt: new Date('2024-02-01'),
        progress: 10,
        maxProgress: 10,
        rarity: 'epic',
        reward: { type: 'discount', value: '20%', description: '20% off premium subscription' }
      },
      {
        id: 'risk_manager',
        title: 'Risk Manager',
        description: 'Maintain stop-loss on 50 trades',
        icon: 'shield',
        category: 'trading',
        tier: 'gold',
        points: 400,
        progress: 35,
        maxProgress: 50,
        rarity: 'epic',
        reward: { type: 'badge', value: 'risk_expert', description: 'Risk Expert badge' }
      },
      {
        id: 'community_helper',
        title: 'Community Helper',
        description: 'Help 25 community members',
        icon: 'people',
        category: 'social',
        tier: 'platinum',
        points: 750,
        progress: 18,
        maxProgress: 25,
        rarity: 'legendary',
        reward: { type: 'premium_time', value: '30', description: '30 days premium access' }
      }
    ];

    const mockBadges: Badge[] = [
      {
        id: 'early_adopter',
        name: 'Early Adopter',
        description: 'One of the first 1000 users',
        icon: 'star',
        color: '#FFD700',
        earnedAt: new Date('2024-01-10'),
        category: 'special',
        displayOrder: 1
      },
      {
        id: 'consistent_trader',
        name: 'Consistent Trader',
        description: 'Maintain 70%+ win rate for 30 days',
        icon: 'trending_up',
        color: '#4CAF50',
        earnedAt: new Date('2024-02-05'),
        category: 'skill',
        displayOrder: 2
      },
      {
        id: 'mentor',
        name: 'Mentor',
        description: 'Help 10 new traders',
        icon: 'school',
        color: '#2196F3',
        earnedAt: new Date('2024-02-10'),
        category: 'achievement',
        displayOrder: 3
      }
    ];

    const mockStreaks: StreakInfo[] = [
      {
        type: 'daily_login',
        current: 15,
        longest: 28,
        multiplier: 1.5,
        nextReward: '500 bonus points at 20 days'
      },
      {
        type: 'trading_days',
        current: 8,
        longest: 12,
        multiplier: 1.2,
        nextReward: 'Premium feature unlock at 10 days'
      },
      {
        type: 'profitable_days',
        current: 5,
        longest: 9,
        multiplier: 2.0,
        nextReward: 'Expert badge at 7 days'
      }
    ];

    const mockChallenges: Challenge[] = [
      {
        id: 'daily_trader',
        title: 'Daily Trader',
        description: 'Complete 5 trades today',
        type: 'daily',
        difficulty: 'easy',
        reward: { points: 100, badges: ['daily_champion'] },
        progress: 3,
        maxProgress: 5,
        deadline: new Date(Date.now() + 8 * 60 * 60 * 1000), // 8 hours
        participants: 156,
        completedBy: 89,
        status: 'active',
        requirements: ['Complete 5 trades', 'Maintain positive P&L', 'Use stop-loss']
      },
      {
        id: 'weekly_winner',
        title: 'Weekly Winner',
        description: 'Finish in top 10% this week',
        type: 'weekly',
        difficulty: 'hard',
        reward: { points: 500, specialReward: 'Premium feature access' },
        progress: 78,
        maxProgress: 90,
        deadline: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000), // 3 days
        participants: 892,
        completedBy: 45,
        status: 'active',
        requirements: ['Top 10% performance', 'Min 20 trades', 'Positive returns']
      },
      {
        id: 'risk_master',
        title: 'Risk Master',
        description: 'Complete 30 days without loss >5%',
        type: 'monthly',
        difficulty: 'expert',
        reward: { points: 1000, multiplier: 2, badges: ['risk_master'] },
        progress: 22,
        maxProgress: 30,
        deadline: new Date(Date.now() + 8 * 24 * 60 * 60 * 1000), // 8 days
        participants: 234,
        completedBy: 12,
        status: 'active',
        requirements: ['Max 5% daily loss', 'Consistent trading', 'Risk management']
      }
    ];

    const mockPointsHistory: PointsBreakdown[] = [
      { category: 'Trading Performance', points: 1250, percentage: 45, trend: 12, activities: [] },
      { category: 'Daily Challenges', points: 800, percentage: 29, trend: 8, activities: [] },
      { category: 'Achievements', points: 450, percentage: 16, trend: -2, activities: [] },
      { category: 'Social Interaction', points: 280, percentage: 10, trend: 15, activities: [] }
    ];

    return {
      totalPoints: 15420,
      rank: 23,
      level: mockLevel,
      achievements: mockAchievements,
      badges: mockBadges,
      streaks: mockStreaks,
      challenges: mockChallenges,
      social: [],
      pointsHistory: mockPointsHistory
    };
  }, []);

  const generateLeaderboard = useCallback((): LeaderboardEntry[] => {
    const entries: LeaderboardEntry[] = [];
    for (let i = 0; i < 20; i++) {
      entries.push({
        rank: i + 1,
        userId: `user_${i + 1}`,
        username: `Trader${String(i + 1).padStart(3, '0')}`,
        avatar: `https://api.dicebear.com/7.x/adventurer/svg?seed=user${i + 1}`,
        points: 20000 - (i * 500) + Math.floor(Math.random() * 400),
        level: Math.floor((20000 - (i * 500)) / 1000) + 1,
        achievements: Math.floor(Math.random() * 15) + 5,
        winRate: Math.round((0.6 + Math.random() * 0.3) * 100),
        streak: Math.floor(Math.random() * 20),
        change: Math.floor(Math.random() * 10) - 5,
        isCurrentUser: i === 22,
        badges: [],
        performance: {
          totalTrades: Math.floor(Math.random() * 500) + 100,
          profitableTradesPercent: Math.round((0.5 + Math.random() * 0.4) * 100),
          averageReturn: Math.round((Math.random() * 20 - 5) * 100) / 100,
          consistency: Math.round(Math.random() * 100),
          riskAdjustedReturn: Math.round((Math.random() * 15 - 2) * 100) / 100
        }
      });
    }
    return entries;
  }, []);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const data = generateMockData();
      const leaderboardData = generateLeaderboard();
      
      setGamificationData(data);
      setLeaderboard(leaderboardData);
      setLoading(false);

      // Simulate achievement notifications
      setTimeout(() => {
        setNotifications(['New achievement unlocked: Daily Champion!']);
      }, 2000);
    };

    loadData();
  }, [generateMockData, generateLeaderboard]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const handleAchievementClick = (achievement: Achievement) => {
    setSelectedAchievement(achievement);
    setAchievementDialogOpen(true);
  };

  const handleChallengeClick = (challenge: Challenge) => {
    setSelectedChallenge(challenge);
    setChallengeDialogOpen(true);
  };

  const getTierColor = (tier: string) => {
    switch (tier) {
      case 'bronze': return '#CD7F32';
      case 'silver': return '#C0C0C0';
      case 'gold': return '#FFD700';
      case 'platinum': return '#E5E4E2';
      case 'diamond': return '#B9F2FF';
      default: return '#666';
    }
  };

  const getRarityColor = (rarity: string) => {
    switch (rarity) {
      case 'common': return '#757575';
      case 'rare': return '#2196F3';
      case 'epic': return '#9C27B0';
      case 'legendary': return '#FF9800';
      case 'mythic': return '#E91E63';
      default: return '#666';
    }
  };

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'easy': return '#4CAF50';
      case 'medium': return '#FF9800';
      case 'hard': return '#F44336';
      case 'expert': return '#9C27B0';
      case 'legendary': return '#E91E63';
      default: return '#666';
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (!gamificationData) {
    return (
      <Alert severity="error">
        <AlertTitle>Error</AlertTitle>
        Failed to load gamification data. Please try again later.
      </Alert>
    );
  }

  const { level, achievements, badges, streaks, challenges, pointsHistory } = gamificationData;

  return (
    <Box sx={{ p: 3 }}>
      {/* Notifications */}
      <AnimatePresence>
        {notifications.map((notification, index) => (
          <motion.div
            key={index}
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            style={{ position: 'fixed', top: 20, right: 20, zIndex: 9999 }}
          >
            <Alert 
              severity="success" 
              onClose={() => setNotifications(prev => prev.filter((_, i) => i !== index))}
              sx={{ mb: 1 }}
            >
              {notification}
            </Alert>
          </motion.div>
        ))}
      </AnimatePresence>

      {/* Header Section */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={8}>
          <Card sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
            <CardContent>
              <Box display="flex" alignItems="center" gap={2}>
                <Avatar sx={{ width: 80, height: 80, fontSize: '2rem' }}>
                  {level.levelTitle.charAt(0)}
                </Avatar>
                <Box flex={1}>
                  <Typography variant="h4" fontWeight="bold">
                    Level {level.currentLevel} {level.prestigeLevel && `★${level.prestigeLevel}`}
                  </Typography>
                  <Typography variant="h6" sx={{ opacity: 0.9 }}>
                    {level.levelTitle}
                  </Typography>
                  <Box display="flex" alignItems="center" gap={1} mt={1}>
                    <Typography variant="body2">
                      {level.currentXP.toLocaleString()} / {level.nextLevelXP.toLocaleString()} XP
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={(level.currentXP / level.nextLevelXP) * 100}
                      sx={{ 
                        flex: 1, 
                        height: 8, 
                        borderRadius: 4,
                        backgroundColor: 'rgba(255,255,255,0.3)',
                        '& .MuiLinearProgress-bar': { backgroundColor: '#FFD700' }
                      }}
                    />
                  </Box>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Card sx={{ textAlign: 'center', background: '#FFD700', color: '#333' }}>
                <CardContent sx={{ pb: '16px !important' }}>
                  <TrophyIcon sx={{ fontSize: 40, mb: 1 }} />
                  <Typography variant="h6" fontWeight="bold">
                    #{gamificationData.rank}
                  </Typography>
                  <Typography variant="body2">Global Rank</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={6}>
              <Card sx={{ textAlign: 'center', background: '#4CAF50', color: 'white' }}>
                <CardContent sx={{ pb: '16px !important' }}>
                  <CoinIcon sx={{ fontSize: 40, mb: 1 }} />
                  <Typography variant="h6" fontWeight="bold">
                    {gamificationData.totalPoints.toLocaleString()}
                  </Typography>
                  <Typography variant="body2">Total Points</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Grid>
      </Grid>

      {/* Navigation Tabs */}
      <Card sx={{ mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab icon={<StarIcon />} label="Overview" />
          <Tab icon={<TrophyIcon />} label="Achievements" />
          <Tab icon={<GradeIcon />} label="Badges" />
          <Tab icon={<PeopleIcon />} label="Leaderboard" />
          <Tab icon={<TargetIcon />} label="Challenges" />
          <Tab icon={<FireIcon />} label="Streaks" />
        </Tabs>
      </Card>

      {/* Tab Content */}
      <Box>
        {/* Overview Tab */}
        {activeTab === 0 && (
          <Grid container spacing={3}>
            {/* Quick Stats */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
                    <TimelineIcon color="primary" />
                    Progress Overview
                  </Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" color="primary" fontWeight="bold">
                          {achievements.filter(a => a.unlockedAt).length}
                        </Typography>
                        <Typography variant="body2">Achievements</Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={6}>
                      <Box textAlign="center" p={2} sx={{ backgroundColor: '#f5f5f5', borderRadius: 2 }}>
                        <Typography variant="h4" color="secondary" fontWeight="bold">
                          {badges.length}
                        </Typography>
                        <Typography variant="body2">Badges Earned</Typography>
                      </Box>
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>
            </Grid>

            {/* Active Streaks */}
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
                    <StreakIcon color="error" />
                    Active Streaks
                  </Typography>
                  <List dense>
                    {streaks.map((streak, index) => (
                      <ListItem key={index}>
                        <ListItemAvatar>
                          <Avatar sx={{ bgcolor: 'orange' }}>
                            <FireIcon />
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={streak.type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                          secondary={`Current: ${streak.current} days | Best: ${streak.longest} days`}
                        />
                        <Chip 
                          label={`${streak.multiplier}x`} 
                          size="small" 
                          color="primary" 
                        />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>
              </Card>
            </Grid>

            {/* Points Breakdown */}
            <Grid item xs={12}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
                    <CoinIcon color="warning" />
                    Points Breakdown
                  </Typography>
                  <Grid container spacing={2}>
                    {pointsHistory.map((category, index) => (
                      <Grid item xs={12} sm={6} md={3} key={index}>
                        <Paper sx={{ p: 2, textAlign: 'center' }}>
                          <Typography variant="h5" fontWeight="bold" color="primary">
                            {category.points.toLocaleString()}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {category.category}
                          </Typography>
                          <Typography variant="caption" color={category.trend >= 0 ? 'success.main' : 'error.main'}>
                            {category.trend >= 0 ? '+' : ''}{category.trend}% this month
                          </Typography>
                          <LinearProgress
                            variant="determinate"
                            value={category.percentage}
                            sx={{ mt: 1 }}
                          />
                        </Paper>
                      </Grid>
                    ))}
                  </Grid>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )}

        {/* Achievements Tab */}
        {activeTab === 1 && (
          <Grid container spacing={3}>
            {achievements.map((achievement) => (
              <Grid item xs={12} sm={6} md={4} key={achievement.id}>
                <motion.div
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <Card 
                    sx={{ 
                      cursor: 'pointer',
                      border: achievement.unlockedAt ? `2px solid ${getTierColor(achievement.tier)}` : '2px solid #e0e0e0',
                      opacity: achievement.unlockedAt ? 1 : 0.6
                    }}
                    onClick={() => handleAchievementClick(achievement)}
                  >
                    <CardContent>
                      <Box display="flex" alignItems="center" gap={2} mb={2}>
                        <Badge badgeContent={achievement.tier} color="primary">
                          <Avatar sx={{ bgcolor: getTierColor(achievement.tier) }}>
                            <TrophyIcon />
                          </Avatar>
                        </Badge>
                        <Box flex={1}>
                          <Typography variant="h6" fontWeight="bold">
                            {achievement.title}
                          </Typography>
                          <Chip 
                            label={achievement.rarity} 
                            size="small" 
                            sx={{ bgcolor: getRarityColor(achievement.rarity), color: 'white' }}
                          />
                        </Box>
                      </Box>
                      <Typography variant="body2" color="text.secondary" mb={2}>
                        {achievement.description}
                      </Typography>
                      <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="body2" fontWeight="bold" color="primary">
                          {achievement.points} XP
                        </Typography>
                        {achievement.unlockedAt ? (
                          <Chip label="Completed" color="success" size="small" />
                        ) : (
                          <LinearProgress
                            variant="determinate"
                            value={(achievement.progress / achievement.maxProgress) * 100}
                            sx={{ flex: 1, ml: 2 }}
                          />
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </motion.div>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Badges Tab */}
        {activeTab === 2 && (
          <Grid container spacing={3}>
            {badges.map((badge) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={badge.id}>
                <Card sx={{ textAlign: 'center' }}>
                  <CardContent>
                    <Avatar
                      sx={{
                        width: 80,
                        height: 80,
                        bgcolor: badge.color,
                        mx: 'auto',
                        mb: 2,
                        fontSize: '2rem'
                      }}
                    >
                      <StarIcon />
                    </Avatar>
                    <Typography variant="h6" fontWeight="bold" gutterBottom>
                      {badge.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" mb={2}>
                      {badge.description}
                    </Typography>
                    <Chip 
                      label={badge.category} 
                      size="small" 
                      variant="outlined"
                    />
                    <Typography variant="caption" display="block" mt={1}>
                      Earned: {badge.earnedAt.toLocaleDateString()}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Leaderboard Tab */}
        {activeTab === 3 && (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
                <TrophyIcon color="warning" />
                Global Leaderboard
              </Typography>
              <List>
                {leaderboard.slice(0, 10).map((entry) => (
                  <ListItem 
                    key={entry.userId}
                    sx={{
                      bgcolor: entry.isCurrentUser ? 'primary.light' : 'transparent',
                      borderRadius: 1,
                      mb: 1
                    }}
                  >
                    <ListItemAvatar>
                      <Badge
                        badgeContent={
                          entry.rank <= 3 ? (
                            <MedalIcon sx={{ color: entry.rank === 1 ? '#FFD700' : entry.rank === 2 ? '#C0C0C0' : '#CD7F32' }} />
                          ) : entry.rank
                        }
                        color="primary"
                      >
                        <Avatar src={entry.avatar} />
                      </Badge>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Box display="flex" alignItems="center" gap={1}>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {entry.username}
                          </Typography>
                          <Chip 
                            label={`Level ${entry.level}`} 
                            size="small" 
                            color="primary" 
                            variant="outlined"
                          />
                        </Box>
                      }
                      secondary={
                        <Stack direction="row" spacing={2}>
                          <Typography variant="caption">
                            Points: {entry.points.toLocaleString()}
                          </Typography>
                          <Typography variant="caption">
                            Win Rate: {entry.winRate}%
                          </Typography>
                          <Typography variant="caption">
                            Streak: {entry.streak}
                          </Typography>
                        </Stack>
                      }
                    />
                    <ListItemSecondaryAction>
                      <Box display="flex" alignItems="center" gap={1}>
                        <Typography 
                          variant="caption" 
                          color={entry.change >= 0 ? 'success.main' : 'error.main'}
                        >
                          {entry.change >= 0 ? '↑' : '↓'} {Math.abs(entry.change)}
                        </Typography>
                      </Box>
                    </ListItemSecondaryAction>
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        )}

        {/* Challenges Tab */}
        {activeTab === 4 && (
          <Grid container spacing={3}>
            {challenges.map((challenge) => (
              <Grid item xs={12} md={6} key={challenge.id}>
                <Card 
                  sx={{ 
                    cursor: 'pointer',
                    border: `2px solid ${getDifficultyColor(challenge.difficulty)}`,
                  }}
                  onClick={() => handleChallengeClick(challenge)}
                >
                  <CardContent>
                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {challenge.title}
                        </Typography>
                        <Stack direction="row" spacing={1} mt={1}>
                          <Chip 
                            label={challenge.type} 
                            size="small" 
                            color="primary"
                          />
                          <Chip 
                            label={challenge.difficulty}
                            size="small"
                            sx={{ 
                              bgcolor: getDifficultyColor(challenge.difficulty),
                              color: 'white'
                            }}
                          />
                        </Stack>
                      </Box>
                      <Typography variant="h6" color="primary" fontWeight="bold">
                        {challenge.reward.points} XP
                      </Typography>
                    </Box>
                    
                    <Typography variant="body2" color="text.secondary" mb={2}>
                      {challenge.description}
                    </Typography>
                    
                    <Box mb={2}>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">
                          Progress: {challenge.progress}/{challenge.maxProgress}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {Math.round((challenge.progress / challenge.maxProgress) * 100)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={(challenge.progress / challenge.maxProgress) * 100}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                    
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Typography variant="caption" color="text.secondary">
                        {challenge.participants} participants • {challenge.completedBy} completed
                      </Typography>
                      <Typography variant="caption" color="error">
                        Ends: {challenge.deadline.toLocaleDateString()}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Streaks Tab */}
        {activeTab === 5 && (
          <Grid container spacing={3}>
            {streaks.map((streak, index) => (
              <Grid item xs={12} md={6} key={index}>
                <Card>
                  <CardContent>
                    <Box display="flex" alignItems="center" gap={2} mb={3}>
                      <Avatar sx={{ bgcolor: 'orange', width: 60, height: 60 }}>
                        <FireIcon sx={{ fontSize: '2rem' }} />
                      </Avatar>
                      <Box>
                        <Typography variant="h5" fontWeight="bold">
                          {streak.type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Current streak multiplier: {streak.multiplier}x
                        </Typography>
                      </Box>
                    </Box>
                    
                    <Grid container spacing={2} mb={3}>
                      <Grid item xs={6}>
                        <Box textAlign="center" p={2} sx={{ bgcolor: 'primary.light', borderRadius: 2 }}>
                          <Typography variant="h3" fontWeight="bold" color="primary">
                            {streak.current}
                          </Typography>
                          <Typography variant="body2">Current Days</Typography>
                        </Box>
                      </Grid>
                      <Grid item xs={6}>
                        <Box textAlign="center" p={2} sx={{ bgcolor: 'success.light', borderRadius: 2 }}>
                          <Typography variant="h3" fontWeight="bold" color="success.main">
                            {streak.longest}
                          </Typography>
                          <Typography variant="body2">Best Streak</Typography>
                        </Box>
                      </Grid>
                    </Grid>
                    
                    <Paper sx={{ p: 2, bgcolor: 'info.light' }}>
                      <Typography variant="body2" fontWeight="bold" color="info.main">
                        Next Reward:
                      </Typography>
                      <Typography variant="body2">
                        {streak.nextReward}
                      </Typography>
                    </Paper>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Box>

      {/* Achievement Detail Dialog */}
      <Dialog
        open={achievementDialogOpen}
        onClose={() => setAchievementDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        {selectedAchievement && (
          <>
            <DialogTitle sx={{ textAlign: 'center', pb: 1 }}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  bgcolor: getTierColor(selectedAchievement.tier),
                  mx: 'auto',
                  mb: 2
                }}
              >
                <TrophyIcon sx={{ fontSize: '3rem' }} />
              </Avatar>
              <Typography variant="h5" fontWeight="bold">
                {selectedAchievement.title}
              </Typography>
              <Stack direction="row" spacing={1} justifyContent="center" mt={1}>
                <Chip label={selectedAchievement.tier} sx={{ bgcolor: getTierColor(selectedAchievement.tier), color: 'white' }} />
                <Chip label={selectedAchievement.rarity} sx={{ bgcolor: getRarityColor(selectedAchievement.rarity), color: 'white' }} />
              </Stack>
            </DialogTitle>
            <DialogContent>
              <Typography variant="body1" gutterBottom>
                {selectedAchievement.description}
              </Typography>
              
              <Box mt={3} mb={2}>
                <Typography variant="h6" gutterBottom>Progress</Typography>
                <Box display="flex" alignItems="center" gap={2}>
                  <LinearProgress
                    variant="determinate"
                    value={(selectedAchievement.progress / selectedAchievement.maxProgress) * 100}
                    sx={{ flex: 1, height: 8, borderRadius: 4 }}
                  />
                  <Typography variant="body2">
                    {selectedAchievement.progress} / {selectedAchievement.maxProgress}
                  </Typography>
                </Box>
              </Box>

              {selectedAchievement.reward && (
                <Paper sx={{ p: 2, bgcolor: 'success.light', mt: 2 }}>
                  <Typography variant="subtitle2" fontWeight="bold" color="success.main">
                    Reward:
                  </Typography>
                  <Typography variant="body2">
                    {selectedAchievement.reward.description}
                  </Typography>
                </Paper>
              )}

              {selectedAchievement.unlockedAt && (
                <Box mt={2} textAlign="center">
                  <Chip 
                    icon={<CelebrationIcon />}
                    label={`Unlocked on ${selectedAchievement.unlockedAt.toLocaleDateString()}`}
                    color="success"
                  />
                </Box>
              )}
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setAchievementDialogOpen(false)}>Close</Button>
              <Button startIcon={<ShareIcon />}>Share Achievement</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Challenge Detail Dialog */}
      <Dialog
        open={challengeDialogOpen}
        onClose={() => setChallengeDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        {selectedChallenge && (
          <>
            <DialogTitle>
              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="h5" fontWeight="bold">
                  {selectedChallenge.title}
                </Typography>
                <IconButton onClick={() => setChallengeDialogOpen(false)}>
                  <CloseIcon />
                </IconButton>
              </Box>
              <Stack direction="row" spacing={1} mt={1}>
                <Chip label={selectedChallenge.type} color="primary" />
                <Chip 
                  label={selectedChallenge.difficulty}
                  sx={{ bgcolor: getDifficultyColor(selectedChallenge.difficulty), color: 'white' }}
                />
              </Stack>
            </DialogTitle>
            <DialogContent>
              <Typography variant="body1" gutterBottom>
                {selectedChallenge.description}
              </Typography>
              
              <Box mt={3} mb={2}>
                <Typography variant="h6" gutterBottom>Progress</Typography>
                <Box display="flex" alignItems="center" gap={2} mb={1}>
                  <LinearProgress
                    variant="determinate"
                    value={(selectedChallenge.progress / selectedChallenge.maxProgress) * 100}
                    sx={{ flex: 1, height: 12, borderRadius: 6 }}
                  />
                  <Typography variant="body2" fontWeight="bold">
                    {Math.round((selectedChallenge.progress / selectedChallenge.maxProgress) * 100)}%
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  {selectedChallenge.progress} of {selectedChallenge.maxProgress} completed
                </Typography>
              </Box>

              <Paper sx={{ p: 2, bgcolor: 'primary.light', mb: 2 }}>
                <Typography variant="subtitle2" fontWeight="bold" color="primary">
                  Reward: {selectedChallenge.reward.points} XP
                </Typography>
                {selectedChallenge.reward.specialReward && (
                  <Typography variant="body2">
                    Special: {selectedChallenge.reward.specialReward}
                  </Typography>
                )}
              </Paper>

              <Box mb={2}>
                <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                  Requirements:
                </Typography>
                <List dense>
                  {selectedChallenge.requirements.map((req, index) => (
                    <ListItem key={index} sx={{ py: 0.5 }}>
                      <ListItemText primary={`• ${req}`} />
                    </ListItem>
                  ))}
                </List>
              </Box>

              <Box display="flex" justifyContent="space-between" alignItems="center">
                <Typography variant="body2" color="text.secondary">
                  {selectedChallenge.participants} participants
                </Typography>
                <Typography variant="body2" color="error">
                  Deadline: {selectedChallenge.deadline.toLocaleDateString()}
                </Typography>
              </Box>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setChallengeDialogOpen(false)}>Close</Button>
              <Button variant="contained" startIcon={<AddIcon />}>
                Join Challenge
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </Box>
  );
};

export default GamificationDashboard;