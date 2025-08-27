import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Star, Zap, Trophy, Target, Gift, TrendingUp,
  Plus, Minus, Clock, Activity, Award, Crown
} from 'lucide-react'

export interface PointsTransaction {
  id: string
  type: 'earned' | 'spent' | 'bonus'
  points: number
  description: string
  category: 'trading' | 'achievement' | 'daily' | 'social' | 'bonus' | 'purchase'
  timestamp: Date
  reference?: string
}

export interface Level {
  level: number
  title: string
  minPoints: number
  maxPoints: number
  rewards: string[]
  color: string
}

export interface PointsSystemProps {
  currentPoints: number
  currentLevel: Level
  nextLevel?: Level
  recentTransactions: PointsTransaction[]
  dailyStreak: number
  weeklyTarget: number
  weeklyProgress: number
  className?: string
  onPointsSpend?: (points: number, item: string) => void
}

const categoryIcons = {
  trading: TrendingUp,
  achievement: Trophy,
  daily: Clock,
  social: Activity,
  bonus: Gift,
  purchase: Star
}

const categoryColors = {
  trading: 'text-green-400 bg-green-500/20',
  achievement: 'text-yellow-400 bg-yellow-500/20',
  daily: 'text-blue-400 bg-blue-500/20',
  social: 'text-purple-400 bg-purple-500/20',
  bonus: 'text-pink-400 bg-pink-500/20',
  purchase: 'text-red-400 bg-red-500/20'
}

export const PointsSystem: React.FC<PointsSystemProps> = ({
  currentPoints,
  currentLevel,
  nextLevel,
  recentTransactions,
  dailyStreak,
  weeklyTarget,
  weeklyProgress,
  className = '',
  onPointsSpend
}) => {
  const [showTransactions, setShowTransactions] = useState(false)
  const [animatingPoints, setAnimatingPoints] = useState(0)
  const [pointsChange, setPointsChange] = useState(0)

  // Animate points changes
  useEffect(() => {
    if (pointsChange !== 0) {
      setAnimatingPoints(pointsChange)
      const timer = setTimeout(() => {
        setAnimatingPoints(0)
        setPointsChange(0)
      }, 2000)
      return () => clearTimeout(timer)
    }
  }, [pointsChange])

  // Calculate level progress
  const levelProgress = nextLevel 
    ? ((currentPoints - currentLevel.minPoints) / (nextLevel.minPoints - currentLevel.minPoints)) * 100
    : 100

  const pointsToNext = nextLevel ? nextLevel.minPoints - currentPoints : 0

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Points header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-r from-yellow-500 to-orange-500 flex items-center justify-center">
              <Star className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-white flex items-center space-x-2">
                <span>{currentPoints.toLocaleString()}</span>
                <AnimatePresence>
                  {animatingPoints !== 0 && (
                    <motion.span
                      initial={{ opacity: 0, y: -20, scale: 0.8 }}
                      animate={{ opacity: 1, y: -10, scale: 1 }}
                      exit={{ opacity: 0, y: -30, scale: 0.6 }}
                      className={`text-lg ${
                        animatingPoints > 0 ? 'text-green-400' : 'text-red-400'
                      }`}
                    >
                      {animatingPoints > 0 ? '+' : ''}{animatingPoints}
                    </motion.span>
                  )}
                </AnimatePresence>
              </h2>
              <p className="text-slate-400">Total Points</p>
            </div>
          </div>
          
          <div className="text-right">
            <div className="flex items-center space-x-2 mb-1">
              <Crown className={`w-4 h-4 ${currentLevel.color}`} />
              <span className="font-medium text-white">{currentLevel.title}</span>
            </div>
            <p className="text-sm text-slate-400">Level {currentLevel.level}</p>
          </div>
        </div>

        {/* Level progress */}
        {nextLevel && (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-slate-400">Progress to {nextLevel.title}</span>
              <span className="text-slate-300">
                {pointsToNext.toLocaleString()} points to go
              </span>
            </div>
            <div className="w-full bg-slate-700/50 rounded-full h-3">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(levelProgress, 100)}%` }}
                transition={{ duration: 1, ease: 'easeOut' }}
                className="h-3 rounded-full bg-gradient-to-r from-blue-500 to-purple-500"
              />
            </div>
            <div className="flex justify-between text-xs text-slate-500">
              <span>Level {currentLevel.level}</span>
              <span>{Math.round(levelProgress)}%</span>
              <span>Level {nextLevel.level}</span>
            </div>
          </div>
        )}
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 gap-4">
        {/* Daily streak */}
        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-lg bg-orange-500/20 flex items-center justify-center">
              <Zap className="w-5 h-5 text-orange-400" />
            </div>
            <div>
              <div className="text-xl font-bold text-white">{dailyStreak}</div>
              <div className="text-sm text-slate-400">Day Streak</div>
            </div>
          </div>
        </div>

        {/* Weekly progress */}
        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-lg bg-green-500/20 flex items-center justify-center">
              <Target className="w-5 h-5 text-green-400" />
            </div>
            <div className="flex-1">
              <div className="text-xl font-bold text-white">
                {Math.round((weeklyProgress / weeklyTarget) * 100)}%
              </div>
              <div className="text-sm text-slate-400">Weekly Goal</div>
              <div className="w-full bg-slate-700/50 rounded-full h-1.5 mt-1">
                <div 
                  className="h-1.5 rounded-full bg-green-400 transition-all duration-500"
                  style={{ width: `${Math.min((weeklyProgress / weeklyTarget) * 100, 100)}%` }}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent transactions */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white flex items-center space-x-2">
            <Activity className="w-5 h-5" />
            <span>Recent Activity</span>
          </h3>
          <button
            onClick={() => setShowTransactions(!showTransactions)}
            className="text-sm text-blue-400 hover:text-blue-300 transition-colors"
          >
            {showTransactions ? 'Show Less' : 'Show All'}
          </button>
        </div>

        <div className="space-y-3">
          {(showTransactions ? recentTransactions : recentTransactions.slice(0, 3)).map((transaction) => {
            const CategoryIcon = categoryIcons[transaction.category]
            const categoryStyle = categoryColors[transaction.category]
            
            return (
              <motion.div
                key={transaction.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex items-center space-x-4 p-3 bg-slate-800/30 rounded-lg"
              >
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${categoryStyle}`}>
                  <CategoryIcon className="w-4 h-4" />
                </div>
                
                <div className="flex-1">
                  <div className="font-medium text-white">{transaction.description}</div>
                  <div className="text-sm text-slate-400">
                    {transaction.timestamp.toLocaleDateString()} at {transaction.timestamp.toLocaleTimeString()}
                  </div>
                </div>
                
                <div className={`font-semibold flex items-center space-x-1 ${
                  transaction.type === 'earned' || transaction.type === 'bonus' 
                    ? 'text-green-400' 
                    : 'text-red-400'
                }`}>
                  {transaction.type === 'earned' || transaction.type === 'bonus' ? (
                    <Plus className="w-4 h-4" />
                  ) : (
                    <Minus className="w-4 h-4" />
                  )}
                  <span>{transaction.points.toLocaleString()}</span>
                </div>
              </motion.div>
            )
          })}
        </div>

        {recentTransactions.length === 0 && (
          <div className="text-center py-8 text-slate-400">
            <Activity className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>No recent activity</p>
            <p className="text-sm">Start trading to earn points!</p>
          </div>
        )}
      </div>

      {/* Points shop/rewards */}
      <div className="glass-card rounded-2xl p-6">
        <h3 className="font-semibold text-white mb-4 flex items-center space-x-2">
          <Gift className="w-5 h-5" />
          <span>Rewards</span>
        </h3>

        <div className="grid grid-cols-2 gap-3">
          {[
            { 
              name: 'Premium Features (1 day)', 
              cost: 500, 
              icon: Star,
              color: 'from-purple-500 to-purple-600'
            },
            { 
              name: 'Trading Boost (24h)', 
              cost: 250, 
              icon: Zap,
              color: 'from-yellow-500 to-orange-500'
            },
            { 
              name: 'Advanced Analytics', 
              cost: 1000, 
              icon: Trophy,
              color: 'from-blue-500 to-cyan-500'
            },
            { 
              name: 'Personal Trader Badge', 
              cost: 750, 
              icon: Award,
              color: 'from-green-500 to-emerald-500'
            }
          ].map((reward) => {
            const canAfford = currentPoints >= reward.cost
            const RewardIcon = reward.icon
            
            return (
              <motion.button
                key={reward.name}
                whileHover={canAfford ? { scale: 1.02 } : {}}
                whileTap={canAfford ? { scale: 0.98 } : {}}
                onClick={() => canAfford && onPointsSpend?.(reward.cost, reward.name)}
                disabled={!canAfford}
                className={`p-4 rounded-xl text-left transition-all ${
                  canAfford 
                    ? 'bg-slate-700/50 hover:bg-slate-600/50 cursor-pointer' 
                    : 'bg-slate-800/30 opacity-50 cursor-not-allowed'
                }`}
              >
                <div className={`w-8 h-8 rounded-lg bg-gradient-to-r ${reward.color} flex items-center justify-center mb-2`}>
                  <RewardIcon className="w-4 h-4 text-white" />
                </div>
                <div className="font-medium text-white text-sm mb-1">{reward.name}</div>
                <div className={`text-xs flex items-center space-x-1 ${
                  canAfford ? 'text-yellow-400' : 'text-slate-500'
                }`}>
                  <Star className="w-3 h-3" />
                  <span>{reward.cost.toLocaleString()}</span>
                </div>
              </motion.button>
            )
          })}
        </div>
      </div>
    </div>
  )
}

export default PointsSystem