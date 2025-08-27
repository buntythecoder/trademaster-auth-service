import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Trophy, Medal, Crown, Star, TrendingUp, Users, 
  Filter, Calendar, BarChart3, Award, Zap
} from 'lucide-react'

export interface LeaderboardEntry {
  id: string
  username: string
  displayName: string
  avatar?: string
  rank: number
  points: number
  level: number
  badge?: string
  streak: number
  weeklyPoints: number
  monthlyPoints: number
  totalTrades: number
  winRate: number
  achievements: number
  isCurrentUser?: boolean
}

interface LeaderboardProps {
  entries: LeaderboardEntry[]
  timeframe?: 'weekly' | 'monthly' | 'all-time'
  category?: 'points' | 'trades' | 'winrate' | 'achievements'
  showTop?: number
  className?: string
}

const timeframeLabels = {
  weekly: 'This Week',
  monthly: 'This Month',
  'all-time': 'All Time'
}

const categoryConfig = {
  points: {
    label: 'Points',
    icon: Star,
    getValue: (entry: LeaderboardEntry, timeframe: string) => {
      switch (timeframe) {
        case 'weekly': return entry.weeklyPoints
        case 'monthly': return entry.monthlyPoints
        default: return entry.points
      }
    },
    format: (value: number) => value.toLocaleString()
  },
  trades: {
    label: 'Trades',
    icon: BarChart3,
    getValue: (entry: LeaderboardEntry) => entry.totalTrades,
    format: (value: number) => value.toLocaleString()
  },
  winrate: {
    label: 'Win Rate',
    icon: TrendingUp,
    getValue: (entry: LeaderboardEntry) => entry.winRate,
    format: (value: number) => `${value.toFixed(1)}%`
  },
  achievements: {
    label: 'Achievements',
    icon: Award,
    getValue: (entry: LeaderboardEntry) => entry.achievements,
    format: (value: number) => value.toString()
  }
}

export const Leaderboard: React.FC<LeaderboardProps> = ({
  entries,
  timeframe = 'all-time',
  category = 'points',
  showTop = 10,
  className = ''
}) => {
  const [selectedTimeframe, setSelectedTimeframe] = useState(timeframe)
  const [selectedCategory, setSelectedCategory] = useState(category)
  const [showFilters, setShowFilters] = useState(false)

  const config = categoryConfig[selectedCategory]
  const sortedEntries = [...entries]
    .sort((a, b) => config.getValue(b, selectedTimeframe) - config.getValue(a, selectedTimeframe))
    .slice(0, showTop)
    .map((entry, index) => ({ ...entry, rank: index + 1 }))

  const getRankIcon = (rank: number) => {
    switch (rank) {
      case 1: return <Crown className="w-5 h-5 text-yellow-400" />
      case 2: return <Medal className="w-5 h-5 text-slate-300" />
      case 3: return <Medal className="w-5 h-5 text-amber-600" />
      default: return <span className="text-slate-400 font-bold">#{rank}</span>
    }
  }

  const getRankStyle = (rank: number, isCurrentUser?: boolean) => {
    if (isCurrentUser) {
      return 'bg-blue-500/10 border-blue-400/30 ring-1 ring-blue-400/20'
    }
    
    switch (rank) {
      case 1: return 'bg-gradient-to-r from-yellow-500/10 to-yellow-600/10 border-yellow-400/30'
      case 2: return 'bg-gradient-to-r from-slate-400/10 to-slate-500/10 border-slate-400/30'
      case 3: return 'bg-gradient-to-r from-amber-500/10 to-amber-600/10 border-amber-500/30'
      default: return 'bg-slate-800/30 border-slate-600/30'
    }
  }

  const currentUser = entries.find(entry => entry.isCurrentUser)
  const currentUserRank = sortedEntries.findIndex(entry => entry.isCurrentUser) + 1

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Header with filters */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Trophy className="w-6 h-6 text-yellow-400" />
          <div>
            <h2 className="text-xl font-bold text-white">Leaderboard</h2>
            <p className="text-sm text-slate-400">
              {timeframeLabels[selectedTimeframe]} • {config.label}
            </p>
          </div>
        </div>
        
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="p-2 glass-card rounded-lg text-slate-400 hover:text-white transition-colors"
        >
          <Filter className="w-4 h-4" />
        </button>
      </div>

      {/* Filters */}
      <AnimatePresence>
        {showFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="glass-card rounded-xl p-4 space-y-3"
          >
            {/* Timeframe filter */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Timeframe</label>
              <div className="grid grid-cols-3 gap-2">
                {Object.entries(timeframeLabels).map(([key, label]) => (
                  <button
                    key={key}
                    onClick={() => setSelectedTimeframe(key as typeof selectedTimeframe)}
                    className={`p-2 rounded-lg text-sm font-medium transition-colors ${
                      selectedTimeframe === key
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {/* Category filter */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Category</label>
              <div className="grid grid-cols-2 gap-2">
                {Object.entries(categoryConfig).map(([key, config]) => {
                  const IconComponent = config.icon
                  return (
                    <button
                      key={key}
                      onClick={() => setSelectedCategory(key as typeof selectedCategory)}
                      className={`p-2 rounded-lg text-sm font-medium transition-colors flex items-center space-x-2 ${
                        selectedCategory === key
                          ? 'bg-blue-600 text-white'
                          : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50'
                      }`}
                    >
                      <IconComponent className="w-4 h-4" />
                      <span>{config.label}</span>
                    </button>
                  )
                })}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Current user highlight (if not in top) */}
      {currentUser && currentUserRank === 0 && (
        <div className="glass-card rounded-xl p-4 bg-blue-500/10 border border-blue-400/30">
          <div className="flex items-center space-x-3">
            <Users className="w-5 h-5 text-blue-400" />
            <div>
              <p className="text-blue-400 font-medium">Your Position</p>
              <p className="text-slate-400 text-sm">
                Not in top {showTop} • {config.format(config.getValue(currentUser, selectedTimeframe))} {config.label.toLowerCase()}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Leaderboard list */}
      <div className="space-y-2">
        <AnimatePresence>
          {sortedEntries.map((entry, index) => {
            const IconComponent = config.icon
            const value = config.getValue(entry, selectedTimeframe)
            
            return (
              <motion.div
                key={entry.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ delay: index * 0.05 }}
                className={`glass-card rounded-xl p-4 border transition-all duration-300 hover:shadow-lg ${getRankStyle(entry.rank, entry.isCurrentUser)}`}
              >
                <div className="flex items-center space-x-4">
                  {/* Rank */}
                  <div className="w-12 h-12 rounded-xl bg-slate-700/50 flex items-center justify-center flex-shrink-0">
                    {getRankIcon(entry.rank)}
                  </div>

                  {/* User info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-2">
                      <h3 className="font-semibold text-white truncate">
                        {entry.displayName}
                        {entry.isCurrentUser && (
                          <span className="ml-2 text-xs px-2 py-1 bg-blue-500/20 text-blue-400 rounded-lg">
                            You
                          </span>
                        )}
                      </h3>
                      {entry.badge && (
                        <span className="text-xs px-2 py-1 bg-purple-500/20 text-purple-400 rounded-lg">
                          {entry.badge}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center space-x-4 mt-1">
                      <span className="text-sm text-slate-400">
                        Level {entry.level}
                      </span>
                      {entry.streak > 0 && (
                        <div className="flex items-center space-x-1 text-sm text-orange-400">
                          <Zap className="w-3 h-3" />
                          <span>{entry.streak} day streak</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Stats */}
                  <div className="text-right flex-shrink-0">
                    <div className="flex items-center space-x-2">
                      <IconComponent className="w-4 h-4 text-slate-400" />
                      <span className="font-semibold text-white">
                        {config.format(value)}
                      </span>
                    </div>
                    {selectedCategory === 'points' && selectedTimeframe !== 'all-time' && (
                      <div className="text-xs text-slate-400 mt-1">
                        {entry.points.toLocaleString()} total
                      </div>
                    )}
                  </div>
                </div>

                {/* Additional stats for top 3 */}
                {entry.rank <= 3 && (
                  <div className="mt-3 pt-3 border-t border-slate-700/50">
                    <div className="grid grid-cols-3 gap-4 text-center">
                      <div>
                        <div className="text-sm font-medium text-white">
                          {entry.totalTrades.toLocaleString()}
                        </div>
                        <div className="text-xs text-slate-400">Trades</div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-white">
                          {entry.winRate.toFixed(1)}%
                        </div>
                        <div className="text-xs text-slate-400">Win Rate</div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-white">
                          {entry.achievements}
                        </div>
                        <div className="text-xs text-slate-400">Achievements</div>
                      </div>
                    </div>
                  </div>
                )}
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>

      {/* Load more button */}
      {entries.length > showTop && (
        <div className="text-center">
          <button className="px-6 py-2 bg-slate-700/50 hover:bg-slate-600/50 rounded-xl text-slate-300 hover:text-white transition-colors">
            View More
          </button>
        </div>
      )}
    </div>
  )
}

export default Leaderboard