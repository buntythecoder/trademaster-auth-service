import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Trophy, Star, Medal, Award, Target, Users, 
  TrendingUp, Zap, Clock, Gift, Crown, BarChart3,
  ChevronRight, Filter, Calendar, Activity
} from 'lucide-react'
import { AchievementCard } from './AchievementCard'
import { Leaderboard } from './Leaderboard'
import { PointsSystem } from './PointsSystem'
import type { Achievement, LeaderboardEntry, PointsTransaction, Level } from './AchievementCard'

// Mock data for demonstration
const mockAchievements: Achievement[] = [
  {
    id: 'first-trade',
    title: 'First Steps',
    description: 'Execute your first trade',
    category: 'trading',
    rarity: 'common',
    points: 100,
    icon: 'zap',
    unlocked: true,
    unlockedAt: new Date('2025-08-25'),
    progress: 1,
    maxProgress: 1,
    reward: { type: 'points', value: '100 points' }
  },
  {
    id: 'profit-maker',
    title: 'Profit Maker',
    description: 'Make ₹10,000 in profits',
    category: 'trading',
    rarity: 'rare',
    points: 500,
    icon: 'trending',
    unlocked: true,
    unlockedAt: new Date('2025-08-26'),
    progress: 15000,
    maxProgress: 10000,
    reward: { type: 'badge', value: 'Profit Maker Badge' }
  },
  {
    id: 'streak-master',
    title: 'Streak Master',
    description: 'Maintain a 30-day trading streak',
    category: 'streak',
    rarity: 'epic',
    points: 1000,
    icon: 'clock',
    unlocked: false,
    progress: 18,
    maxProgress: 30,
    requirements: ['Trade for 30 consecutive days', 'Maintain positive P&L ratio']
  },
  {
    id: 'legend-trader',
    title: 'Trading Legend',
    description: 'Reach ₹1 million in total volume',
    category: 'milestone',
    rarity: 'legendary',
    points: 5000,
    icon: 'crown',
    unlocked: false,
    progress: 450000,
    maxProgress: 1000000,
    requirements: ['Trade ₹1 million total volume', 'Maintain 70% win rate', 'Complete 1000 trades']
  }
]

const mockLeaderboard: LeaderboardEntry[] = [
  {
    id: '1',
    username: 'tradePro',
    displayName: 'Trade Pro',
    rank: 1,
    points: 15420,
    level: 12,
    badge: 'Legend',
    streak: 45,
    weeklyPoints: 2340,
    monthlyPoints: 8900,
    totalTrades: 1250,
    winRate: 78.5,
    achievements: 24
  },
  {
    id: '2',
    username: 'marketMaster',
    displayName: 'Market Master',
    rank: 2,
    points: 12890,
    level: 11,
    badge: 'Expert',
    streak: 23,
    weeklyPoints: 1890,
    monthlyPoints: 7456,
    totalTrades: 987,
    winRate: 73.2,
    achievements: 19
  },
  {
    id: 'current',
    username: 'currentUser',
    displayName: 'You',
    rank: 3,
    points: 8750,
    level: 8,
    streak: 12,
    weeklyPoints: 1250,
    monthlyPoints: 4320,
    totalTrades: 645,
    winRate: 68.9,
    achievements: 15,
    isCurrentUser: true
  }
]

const mockTransactions: PointsTransaction[] = [
  {
    id: '1',
    type: 'earned',
    points: 150,
    description: 'Completed profitable trade',
    category: 'trading',
    timestamp: new Date(),
    reference: 'TRADE-001'
  },
  {
    id: '2',
    type: 'earned',
    points: 500,
    description: 'Achievement unlocked: Profit Maker',
    category: 'achievement',
    timestamp: new Date(Date.now() - 3600000),
    reference: 'ACH-profit-maker'
  },
  {
    id: '3',
    type: 'earned',
    points: 50,
    description: 'Daily streak bonus',
    category: 'daily',
    timestamp: new Date(Date.now() - 86400000)
  }
]

const currentLevel: Level = {
  level: 8,
  title: 'Advanced Trader',
  minPoints: 8000,
  maxPoints: 10000,
  rewards: ['Advanced Analytics', 'Priority Support'],
  color: 'text-purple-400'
}

const nextLevel: Level = {
  level: 9,
  title: 'Expert Trader',
  minPoints: 10000,
  maxPoints: 15000,
  rewards: ['Expert Badge', 'Premium Features', 'Custom Alerts'],
  color: 'text-blue-400'
}

interface GamificationDashboardProps {
  className?: string
}

export const GamificationDashboard: React.FC<GamificationDashboardProps> = ({
  className = ''
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'achievements' | 'leaderboard' | 'rewards'>('overview')
  const [achievementFilter, setAchievementFilter] = useState<'all' | 'unlocked' | 'locked'>('all')
  const [showClaimAnimation, setShowClaimAnimation] = useState(false)

  const currentUser = mockLeaderboard.find(entry => entry.isCurrentUser)!
  const filteredAchievements = mockAchievements.filter(achievement => {
    if (achievementFilter === 'unlocked') return achievement.unlocked
    if (achievementFilter === 'locked') return !achievement.unlocked
    return true
  })

  const handleAchievementClaim = (achievement: Achievement) => {
    setShowClaimAnimation(true)
    console.log('Claiming achievement:', achievement.title)
    setTimeout(() => setShowClaimAnimation(false), 2000)
  }

  const handlePointsSpend = (points: number, item: string) => {
    console.log(`Spending ${points} points on ${item}`)
  }

  const tabConfig = [
    { id: 'overview', label: 'Overview', icon: BarChart3 },
    { id: 'achievements', label: 'Achievements', icon: Trophy },
    { id: 'leaderboard', label: 'Leaderboard', icon: Medal },
    { id: 'rewards', label: 'Rewards', icon: Gift }
  ]

  return (
    <div className={`min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 ${className}`}>
      {/* Claim animation overlay */}
      <AnimatePresence>
        {showClaimAnimation && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          >
            <motion.div
              initial={{ scale: 0, rotate: -180 }}
              animate={{ scale: 1, rotate: 0 }}
              exit={{ scale: 0, rotate: 180 }}
              className="glass-card rounded-2xl p-8 text-center"
            >
              <motion.div
                animate={{ 
                  scale: [1, 1.2, 1],
                  rotate: [0, 10, -10, 0]
                }}
                transition={{ duration: 1, repeat: 1 }}
                className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-r from-yellow-500 to-orange-500 flex items-center justify-center"
              >
                <Trophy className="w-10 h-10 text-white" />
              </motion.div>
              <h3 className="text-2xl font-bold text-white mb-2">Achievement Unlocked!</h3>
              <p className="text-slate-400">Congratulations on your progress!</p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <div className="sticky top-0 z-30 glass-card border-b border-slate-700/50">
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center space-x-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-r from-purple-600 to-pink-600 flex items-center justify-center">
                <Trophy className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-white">Gamification</h1>
                <p className="text-slate-400">Track your achievements and compete with others</p>
              </div>
            </div>

            <div className="text-right">
              <div className="text-2xl font-bold text-white">{currentUser.points.toLocaleString()}</div>
              <div className="text-sm text-slate-400">Total Points</div>
            </div>
          </div>

          {/* Tab navigation */}
          <div className="flex space-x-1 bg-slate-800/50 rounded-xl p-1">
            {tabConfig.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all ${
                  activeTab === id
                    ? 'bg-blue-600 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        <AnimatePresence mode="wait">
          {/* Overview Tab */}
          {activeTab === 'overview' && (
            <motion.div
              key="overview"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-6"
            >
              {/* Quick stats */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="glass-card rounded-xl p-6 text-center">
                  <div className="w-12 h-12 mx-auto mb-3 rounded-xl bg-yellow-500/20 flex items-center justify-center">
                    <Star className="w-6 h-6 text-yellow-400" />
                  </div>
                  <div className="text-2xl font-bold text-white mb-1">{currentUser.points.toLocaleString()}</div>
                  <div className="text-sm text-slate-400">Total Points</div>
                </div>

                <div className="glass-card rounded-xl p-6 text-center">
                  <div className="w-12 h-12 mx-auto mb-3 rounded-xl bg-purple-500/20 flex items-center justify-center">
                    <Crown className="w-6 h-6 text-purple-400" />
                  </div>
                  <div className="text-2xl font-bold text-white mb-1">Level {currentUser.level}</div>
                  <div className="text-sm text-slate-400">{currentLevel.title}</div>
                </div>

                <div className="glass-card rounded-xl p-6 text-center">
                  <div className="w-12 h-12 mx-auto mb-3 rounded-xl bg-green-500/20 flex items-center justify-center">
                    <Trophy className="w-6 h-6 text-green-400" />
                  </div>
                  <div className="text-2xl font-bold text-white mb-1">{currentUser.achievements}</div>
                  <div className="text-sm text-slate-400">Achievements</div>
                </div>

                <div className="glass-card rounded-xl p-6 text-center">
                  <div className="w-12 h-12 mx-auto mb-3 rounded-xl bg-orange-500/20 flex items-center justify-center">
                    <Zap className="w-6 h-6 text-orange-400" />
                  </div>
                  <div className="text-2xl font-bold text-white mb-1">{currentUser.streak}</div>
                  <div className="text-sm text-slate-400">Day Streak</div>
                </div>
              </div>

              {/* Recent achievements */}
              <div className="glass-card rounded-xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-semibold text-white">Recent Achievements</h3>
                  <button
                    onClick={() => setActiveTab('achievements')}
                    className="flex items-center space-x-2 text-blue-400 hover:text-blue-300 transition-colors"
                  >
                    <span>View All</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {mockAchievements.filter(a => a.unlocked).slice(0, 2).map(achievement => (
                    <AchievementCard
                      key={achievement.id}
                      achievement={achievement}
                      showProgress={false}
                      interactive={false}
                    />
                  ))}
                </div>
              </div>

              {/* Leaderboard preview */}
              <div className="glass-card rounded-xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-semibold text-white">Top Traders</h3>
                  <button
                    onClick={() => setActiveTab('leaderboard')}
                    className="flex items-center space-x-2 text-blue-400 hover:text-blue-300 transition-colors"
                  >
                    <span>Full Leaderboard</span>
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
                
                <Leaderboard entries={mockLeaderboard} showTop={3} />
              </div>
            </motion.div>
          )}

          {/* Achievements Tab */}
          {activeTab === 'achievements' && (
            <motion.div
              key="achievements"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="space-y-6"
            >
              {/* Filter */}
              <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-white">Your Achievements</h2>
                <div className="flex items-center space-x-2">
                  <Filter className="w-4 h-4 text-slate-400" />
                  <select
                    value={achievementFilter}
                    onChange={(e) => setAchievementFilter(e.target.value as typeof achievementFilter)}
                    className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm"
                  >
                    <option value="all">All</option>
                    <option value="unlocked">Unlocked</option>
                    <option value="locked">Locked</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredAchievements.map(achievement => (
                  <AchievementCard
                    key={achievement.id}
                    achievement={achievement}
                    onClaim={handleAchievementClaim}
                  />
                ))}
              </div>
            </motion.div>
          )}

          {/* Leaderboard Tab */}
          {activeTab === 'leaderboard' && (
            <motion.div
              key="leaderboard"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <Leaderboard entries={mockLeaderboard} showTop={10} />
            </motion.div>
          )}

          {/* Rewards Tab */}
          {activeTab === 'rewards' && (
            <motion.div
              key="rewards"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              <PointsSystem
                currentPoints={currentUser.points}
                currentLevel={currentLevel}
                nextLevel={nextLevel}
                recentTransactions={mockTransactions}
                dailyStreak={currentUser.streak}
                weeklyTarget={2000}
                weeklyProgress={currentUser.weeklyPoints}
                onPointsSpend={handlePointsSpend}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default GamificationDashboard