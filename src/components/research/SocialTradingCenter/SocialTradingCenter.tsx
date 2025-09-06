import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Users, MessageSquare, TrendingUp, TrendingDown, Star, Trophy,
  ThumbsUp, ThumbsDown, Share2, Eye, Clock, Award, Target,
  Activity, BarChart3, Zap, Fire, Heart, Filter, Search,
  ChevronDown, ChevronUp, ArrowUpRight, ArrowDownRight,
  Globe, MapPin, Calendar, CheckCircle, AlertCircle
} from 'lucide-react'

// Types for Social Trading
interface TradingSignal {
  id: string
  trader: TraderProfile
  type: 'BUY' | 'SELL' | 'HOLD'
  symbol: string
  price: number
  targetPrice?: number
  stopLoss?: number
  confidence: number // 0-100
  reasoning: string
  timestamp: Date
  performance?: {
    currentReturn: number
    maxDrawdown: number
    winRate: number
    followers: number
  }
  tags: string[]
  likes: number
  comments: number
  views: number
  isFollowing: boolean
}

interface TraderProfile {
  id: string
  username: string
  displayName: string
  avatar: string
  verified: boolean
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND'
  stats: {
    totalReturn: number
    winRate: number
    followers: number
    following: number
    trades: number
    avgHoldTime: number
    maxDrawdown: number
    sharpeRatio: number
    monthlyReturn: number
  }
  specialties: string[]
  location?: string
  joinDate: Date
  bio?: string
  socialProof: {
    certifications: string[]
    achievements: string[]
    rankings: string[]
  }
}

interface Discussion {
  id: string
  author: TraderProfile
  title: string
  content: string
  symbol?: string
  category: 'ANALYSIS' | 'SIGNAL' | 'NEWS' | 'QUESTION' | 'TUTORIAL'
  timestamp: Date
  replies: number
  likes: number
  views: number
  trending: boolean
  tags: string[]
}

interface CommunityStats {
  totalTraders: number
  activeSignals: number
  avgReturn: number
  totalVolume: number
  topPerformers: TraderProfile[]
  trendingSymbols: {
    symbol: string
    mentions: number
    sentiment: number
    priceChange: number
  }[]
}

// Mock data
const mockTraders: TraderProfile[] = [
  {
    id: '1',
    username: 'alpha_trader',
    displayName: 'Alpha Trader Pro',
    avatar: '/avatars/trader1.jpg',
    verified: true,
    tier: 'DIAMOND',
    stats: {
      totalReturn: 145.8,
      winRate: 78.5,
      followers: 15420,
      following: 89,
      trades: 342,
      avgHoldTime: 12.5,
      maxDrawdown: -8.2,
      sharpeRatio: 2.34,
      monthlyReturn: 12.4
    },
    specialties: ['Large Cap', 'Technical Analysis', 'Options'],
    location: 'Mumbai, India',
    joinDate: new Date('2022-03-15'),
    bio: 'Technical analysis expert with 8+ years experience. Focus on large cap momentum plays.',
    socialProof: {
      certifications: ['CFA Level II', 'FRM'],
      achievements: ['Top 1% Performer 2023', '6-Month Win Streak'],
      rankings: ['#1 Large Cap Trader', '#3 Technical Analysis']
    }
  },
  {
    id: '2',
    username: 'quant_master',
    displayName: 'Quantitative Master',
    avatar: '/avatars/trader2.jpg',
    verified: true,
    tier: 'PLATINUM',
    stats: {
      totalReturn: 98.3,
      winRate: 71.2,
      followers: 8930,
      following: 34,
      trades: 567,
      avgHoldTime: 8.3,
      maxDrawdown: -12.1,
      sharpeRatio: 1.87,
      monthlyReturn: 8.9
    },
    specialties: ['Quantitative Analysis', 'Algorithms', 'Risk Management'],
    location: 'Bangalore, India',
    joinDate: new Date('2021-11-20'),
    bio: 'Algorithmic trading specialist. Building systematic strategies for consistent returns.',
    socialProof: {
      certifications: ['CQF', 'Python for Finance'],
      achievements: ['Lowest Drawdown 2023', 'Consistent Returns Award'],
      rankings: ['#1 Algo Trader', '#2 Risk Management']
    }
  },
  {
    id: '3',
    username: 'swing_king',
    displayName: 'Swing Trading King',
    avatar: '/avatars/trader3.jpg',
    verified: false,
    tier: 'GOLD',
    stats: {
      totalReturn: 67.4,
      winRate: 65.8,
      followers: 3240,
      following: 156,
      trades: 189,
      avgHoldTime: 24.7,
      maxDrawdown: -15.3,
      sharpeRatio: 1.42,
      monthlyReturn: 5.6
    },
    specialties: ['Swing Trading', 'Chart Patterns', 'Sector Rotation'],
    location: 'Delhi, India',
    joinDate: new Date('2023-01-08'),
    bio: 'Swing trader focusing on chart patterns and sector momentum.',
    socialProof: {
      certifications: ['Technical Analysis Certified'],
      achievements: ['Rising Star 2023'],
      rankings: ['#5 Swing Trader']
    }
  }
]

const mockSignals: TradingSignal[] = [
  {
    id: '1',
    trader: mockTraders[0],
    type: 'BUY',
    symbol: 'RELIANCE',
    price: 2456.75,
    targetPrice: 2650.00,
    stopLoss: 2300.00,
    confidence: 85,
    reasoning: 'Strong breakout above 200-day MA with high volume. Oil prices supportive.',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
    performance: {
      currentReturn: 3.2,
      maxDrawdown: -1.1,
      winRate: 78.5,
      followers: 342
    },
    tags: ['Breakout', 'Energy', 'Large Cap'],
    likes: 156,
    comments: 23,
    views: 1240,
    isFollowing: true
  },
  {
    id: '2',
    trader: mockTraders[1],
    type: 'SELL',
    symbol: 'TCS',
    price: 3245.80,
    targetPrice: 3050.00,
    stopLoss: 3350.00,
    confidence: 72,
    reasoning: 'Overbought conditions with RSI above 75. Expect correction to support.',
    timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
    performance: {
      currentReturn: -1.8,
      maxDrawdown: -2.3,
      winRate: 71.2,
      followers: 89
    },
    tags: ['Overbought', 'IT', 'Technical'],
    likes: 89,
    comments: 45,
    views: 890,
    isFollowing: false
  }
]

const mockDiscussions: Discussion[] = [
  {
    id: '1',
    author: mockTraders[0],
    title: 'Banking Sector Outlook: Time for Rotation?',
    content: 'Seeing interesting patterns in banking stocks. HDFC and ICICI showing relative strength...',
    symbol: 'HDFCBANK',
    category: 'ANALYSIS',
    timestamp: new Date(Date.now() - 1 * 60 * 60 * 1000),
    replies: 34,
    likes: 127,
    views: 2340,
    trending: true,
    tags: ['Banking', 'Sector Analysis', 'Rotation']
  },
  {
    id: '2',
    author: mockTraders[1],
    title: 'Options Strategy: Covered Calls in Current Market',
    content: 'Given the high volatility, covered calls might be attractive for income generation...',
    category: 'TUTORIAL',
    timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000),
    replies: 18,
    likes: 89,
    views: 1560,
    trending: false,
    tags: ['Options', 'Strategy', 'Income']
  }
]

const mockCommunityStats: CommunityStats = {
  totalTraders: 45230,
  activeSignals: 1240,
  avgReturn: 24.6,
  totalVolume: 15680000000, // 15.68B
  topPerformers: mockTraders.slice(0, 3),
  trendingSymbols: [
    { symbol: 'RELIANCE', mentions: 234, sentiment: 0.72, priceChange: 2.3 },
    { symbol: 'TCS', mentions: 189, sentiment: -0.15, priceChange: -1.8 },
    { symbol: 'INFY', mentions: 156, sentiment: 0.45, priceChange: 1.2 },
    { symbol: 'HDFC', mentions: 134, sentiment: 0.38, priceChange: 0.8 }
  ]
}

interface SocialTradingCenterProps {
  className?: string
}

export const SocialTradingCenter: React.FC<SocialTradingCenterProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'signals' | 'leaderboard' | 'discussions' | 'my-feed'>('signals')
  const [selectedFilter, setSelectedFilter] = useState<'all' | 'following' | 'trending'>('all')
  const [signalType, setSignalType] = useState<'all' | 'BUY' | 'SELL' | 'HOLD'>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [showFilters, setShowFilters] = useState(false)
  
  // Data state
  const [signals, setSignals] = useState<TradingSignal[]>(mockSignals)
  const [traders, setTraders] = useState<TraderProfile[]>(mockTraders)
  const [discussions, setDiscussions] = useState<Discussion[]>(mockDiscussions)
  const [communityStats] = useState<CommunityStats>(mockCommunityStats)

  // Real-time updates simulation
  useEffect(() => {
    const interval = setInterval(() => {
      setSignals(prev => prev.map(signal => ({
        ...signal,
        views: signal.views + Math.floor(Math.random() * 5),
        likes: signal.likes + (Math.random() > 0.8 ? 1 : 0)
      })))
    }, 10000)

    return () => clearInterval(interval)
  }, [])

  // Filter signals
  const filteredSignals = signals.filter(signal => {
    if (signalType !== 'all' && signal.type !== signalType) return false
    if (selectedFilter === 'following' && !signal.isFollowing) return false
    if (searchQuery && !signal.symbol.toLowerCase().includes(searchQuery.toLowerCase())) return false
    return true
  })

  const handleFollowTrader = (traderId: string) => {
    setSignals(prev => prev.map(signal => 
      signal.trader.id === traderId 
        ? { ...signal, isFollowing: !signal.isFollowing }
        : signal
    ))
  }

  const handleLikeSignal = (signalId: string) => {
    setSignals(prev => prev.map(signal =>
      signal.id === signalId
        ? { ...signal, likes: signal.likes + 1 }
        : signal
    ))
  }

  const getTierColor = (tier: TraderProfile['tier']) => {
    const colors = {
      BRONZE: 'text-amber-600',
      SILVER: 'text-slate-400',
      GOLD: 'text-yellow-500',
      PLATINUM: 'text-purple-400',
      DIAMOND: 'text-cyan-400'
    }
    return colors[tier]
  }

  const getTierIcon = (tier: TraderProfile['tier']) => {
    return <Award className={`w-4 h-4 ${getTierColor(tier)}`} />
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Community Stats Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white mb-2">
              Social Trading Center
            </h1>
            <p className="text-slate-400">
              Connect with top traders, share insights, and follow winning strategies
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <Globe className="w-5 h-5 text-blue-400" />
            <span className="text-sm text-slate-400">Live Community</span>
          </div>
        </div>

        <div className="grid grid-cols-4 gap-6">
          <div className="text-center">
            <div className="flex items-center justify-center mb-2">
              <Users className="w-5 h-5 text-blue-400 mr-2" />
              <span className="text-2xl font-bold text-white">
                {(communityStats.totalTraders / 1000).toFixed(1)}K
              </span>
            </div>
            <p className="text-sm text-slate-400">Active Traders</p>
          </div>
          
          <div className="text-center">
            <div className="flex items-center justify-center mb-2">
              <Activity className="w-5 h-5 text-green-400 mr-2" />
              <span className="text-2xl font-bold text-white">
                {communityStats.activeSignals}
              </span>
            </div>
            <p className="text-sm text-slate-400">Active Signals</p>
          </div>

          <div className="text-center">
            <div className="flex items-center justify-center mb-2">
              <TrendingUp className="w-5 h-5 text-green-400 mr-2" />
              <span className="text-2xl font-bold text-green-400">
                +{communityStats.avgReturn}%
              </span>
            </div>
            <p className="text-sm text-slate-400">Avg Return</p>
          </div>

          <div className="text-center">
            <div className="flex items-center justify-center mb-2">
              <BarChart3 className="w-5 h-5 text-purple-400 mr-2" />
              <span className="text-2xl font-bold text-white">
                ₹{(communityStats.totalVolume / 1000000000).toFixed(1)}B
              </span>
            </div>
            <p className="text-sm text-slate-400">Volume Today</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'signals', label: 'Trading Signals', icon: Zap },
              { id: 'leaderboard', label: 'Leaderboard', icon: Trophy },
              { id: 'discussions', label: 'Discussions', icon: MessageSquare },
              { id: 'my-feed', label: 'My Feed', icon: Heart }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>

          {/* Search and Filters */}
          <div className="flex items-center space-x-4">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search symbols, traders..."
                className="pl-10 pr-4 py-2 bg-slate-800/50 border border-slate-700/50 rounded-lg text-white placeholder-slate-400 text-sm w-64 focus:border-blue-500 focus:outline-none"
              />
            </div>
            
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`p-2 rounded-lg transition-colors ${
                showFilters ? 'bg-blue-600' : 'bg-slate-800/50 hover:bg-slate-700/50'
              }`}
            >
              <Filter className="w-4 h-4 text-white" />
            </button>
          </div>
        </div>

        {/* Filter Options */}
        <AnimatePresence>
          {showFilters && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-700/50 pt-4"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-2">
                    <span className="text-sm text-slate-400">Filter:</span>
                    {['all', 'following', 'trending'].map((filter) => (
                      <button
                        key={filter}
                        onClick={() => setSelectedFilter(filter as typeof selectedFilter)}
                        className={`px-3 py-1 rounded-lg text-xs font-medium capitalize transition-all ${
                          selectedFilter === filter
                            ? 'bg-blue-600 text-white'
                            : 'bg-slate-800/50 text-slate-400 hover:text-white'
                        }`}
                      >
                        {filter}
                      </button>
                    ))}
                  </div>

                  {activeTab === 'signals' && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-slate-400">Type:</span>
                      {['all', 'BUY', 'SELL', 'HOLD'].map((type) => (
                        <button
                          key={type}
                          onClick={() => setSignalType(type as typeof signalType)}
                          className={`px-3 py-1 rounded-lg text-xs font-medium capitalize transition-all ${
                            signalType === type
                              ? type === 'BUY' ? 'bg-green-600 text-white'
                                : type === 'SELL' ? 'bg-red-600 text-white'
                                : type === 'HOLD' ? 'bg-yellow-600 text-white'
                                : 'bg-blue-600 text-white'
                              : 'bg-slate-800/50 text-slate-400 hover:text-white'
                          }`}
                        >
                          {type}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Tab Content */}
        <div className="mt-6">
          <AnimatePresence mode="wait">
            {/* Trading Signals Tab */}
            {activeTab === 'signals' && (
              <motion.div
                key="signals"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {filteredSignals.map((signal) => (
                  <div
                    key={signal.id}
                    className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all"
                  >
                    {/* Signal Header */}
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center text-white font-semibold">
                          {signal.trader.displayName.charAt(0)}
                        </div>
                        
                        <div>
                          <div className="flex items-center space-x-2">
                            <h3 className="font-semibold text-white">{signal.trader.displayName}</h3>
                            {signal.trader.verified && (
                              <CheckCircle className="w-4 h-4 text-blue-400" />
                            )}
                            {getTierIcon(signal.trader.tier)}
                          </div>
                          <div className="flex items-center space-x-2 text-sm text-slate-400">
                            <span>@{signal.trader.username}</span>
                            <span>•</span>
                            <Clock className="w-3 h-3" />
                            <span>{new Date(signal.timestamp).toLocaleTimeString()}</span>
                          </div>
                        </div>
                      </div>

                      <button
                        onClick={() => handleFollowTrader(signal.trader.id)}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                          signal.isFollowing
                            ? 'bg-blue-600 text-white hover:bg-blue-700'
                            : 'bg-slate-700 text-white hover:bg-slate-600'
                        }`}
                      >
                        {signal.isFollowing ? 'Following' : 'Follow'}
                      </button>
                    </div>

                    {/* Signal Details */}
                    <div className="grid grid-cols-3 gap-6 mb-4">
                      <div>
                        <div className="flex items-center space-x-2 mb-2">
                          <div className={`px-3 py-1 rounded-lg text-sm font-bold ${
                            signal.type === 'BUY' ? 'bg-green-600 text-white'
                              : signal.type === 'SELL' ? 'bg-red-600 text-white'
                              : 'bg-yellow-600 text-white'
                          }`}>
                            {signal.type}
                          </div>
                          <span className="text-xl font-bold text-white">{signal.symbol}</span>
                        </div>
                        <p className="text-lg text-slate-300">₹{signal.price.toLocaleString()}</p>
                      </div>

                      <div className="space-y-2">
                        {signal.targetPrice && (
                          <div className="flex justify-between">
                            <span className="text-sm text-slate-400">Target:</span>
                            <span className="text-sm text-green-400">
                              ₹{signal.targetPrice.toLocaleString()}
                            </span>
                          </div>
                        )}
                        {signal.stopLoss && (
                          <div className="flex justify-between">
                            <span className="text-sm text-slate-400">Stop Loss:</span>
                            <span className="text-sm text-red-400">
                              ₹{signal.stopLoss.toLocaleString()}
                            </span>
                          </div>
                        )}
                        <div className="flex justify-between">
                          <span className="text-sm text-slate-400">Confidence:</span>
                          <span className="text-sm text-white">{signal.confidence}%</span>
                        </div>
                      </div>

                      {signal.performance && (
                        <div className="text-right">
                          <div className={`text-2xl font-bold mb-1 ${
                            signal.performance.currentReturn >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {signal.performance.currentReturn >= 0 ? '+' : ''}{signal.performance.currentReturn.toFixed(1)}%
                          </div>
                          <p className="text-xs text-slate-400">Current Return</p>
                          <div className="text-xs text-slate-400 mt-1">
                            {signal.performance.followers} followers
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Reasoning */}
                    <div className="bg-slate-900/50 rounded-lg p-4 mb-4">
                      <p className="text-sm text-slate-300">{signal.reasoning}</p>
                    </div>

                    {/* Tags */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        {signal.tags.map((tag) => (
                          <span
                            key={tag}
                            className="px-2 py-1 bg-slate-700/50 rounded-lg text-xs text-slate-400"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>

                      {/* Engagement */}
                      <div className="flex items-center space-x-4">
                        <button
                          onClick={() => handleLikeSignal(signal.id)}
                          className="flex items-center space-x-1 text-slate-400 hover:text-red-400 transition-colors"
                        >
                          <ThumbsUp className="w-4 h-4" />
                          <span className="text-sm">{signal.likes}</span>
                        </button>
                        <div className="flex items-center space-x-1 text-slate-400">
                          <MessageSquare className="w-4 h-4" />
                          <span className="text-sm">{signal.comments}</span>
                        </div>
                        <div className="flex items-center space-x-1 text-slate-400">
                          <Eye className="w-4 h-4" />
                          <span className="text-sm">{signal.views}</span>
                        </div>
                        <button className="text-slate-400 hover:text-white transition-colors">
                          <Share2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}

            {/* Leaderboard Tab */}
            {activeTab === 'leaderboard' && (
              <motion.div
                key="leaderboard"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                <div className="grid grid-cols-1 gap-4">
                  {traders.map((trader, index) => (
                    <div
                      key={trader.id}
                      className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                          <div className="relative">
                            <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center text-white font-bold text-lg">
                              {trader.displayName.charAt(0)}
                            </div>
                            <div className="absolute -top-1 -right-1 bg-yellow-500 text-black text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center">
                              #{index + 1}
                            </div>
                          </div>

                          <div className="flex-1">
                            <div className="flex items-center space-x-2 mb-1">
                              <h3 className="font-bold text-white text-lg">{trader.displayName}</h3>
                              {trader.verified && <CheckCircle className="w-5 h-5 text-blue-400" />}
                              {getTierIcon(trader.tier)}
                            </div>
                            
                            <p className="text-slate-400 text-sm mb-2">@{trader.username}</p>
                            
                            <div className="flex items-center space-x-4 text-sm">
                              {trader.location && (
                                <div className="flex items-center space-x-1 text-slate-400">
                                  <MapPin className="w-3 h-3" />
                                  <span>{trader.location}</span>
                                </div>
                              )}
                              <div className="flex items-center space-x-1 text-slate-400">
                                <Calendar className="w-3 h-3" />
                                <span>Joined {trader.joinDate.getFullYear()}</span>
                              </div>
                            </div>
                            
                            {trader.specialties.length > 0 && (
                              <div className="flex items-center space-x-2 mt-2">
                                {trader.specialties.slice(0, 3).map((specialty) => (
                                  <span
                                    key={specialty}
                                    className="px-2 py-1 bg-blue-600/20 text-blue-400 rounded-lg text-xs"
                                  >
                                    {specialty}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>

                          <div className="grid grid-cols-3 gap-8 text-center">
                            <div>
                              <div className="text-2xl font-bold text-green-400 mb-1">
                                +{trader.stats.totalReturn.toFixed(1)}%
                              </div>
                              <p className="text-xs text-slate-400">Total Return</p>
                            </div>
                            
                            <div>
                              <div className="text-2xl font-bold text-white mb-1">
                                {trader.stats.winRate.toFixed(1)}%
                              </div>
                              <p className="text-xs text-slate-400">Win Rate</p>
                            </div>
                            
                            <div>
                              <div className="text-2xl font-bold text-blue-400 mb-1">
                                {(trader.stats.followers / 1000).toFixed(1)}K
                              </div>
                              <p className="text-xs text-slate-400">Followers</p>
                            </div>
                          </div>

                          <button className="px-6 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg font-medium hover:from-blue-700 hover:to-purple-700 transition-all">
                            Follow
                          </button>
                        </div>
                      </div>

                      {/* Additional Stats Row */}
                      <div className="mt-4 pt-4 border-t border-slate-700/30">
                        <div className="grid grid-cols-4 gap-4 text-center">
                          <div>
                            <div className="text-lg font-semibold text-white">
                              {trader.stats.trades}
                            </div>
                            <p className="text-xs text-slate-400">Trades</p>
                          </div>
                          <div>
                            <div className="text-lg font-semibold text-white">
                              {trader.stats.avgHoldTime.toFixed(1)}d
                            </div>
                            <p className="text-xs text-slate-400">Avg Hold</p>
                          </div>
                          <div>
                            <div className="text-lg font-semibold text-red-400">
                              {trader.stats.maxDrawdown.toFixed(1)}%
                            </div>
                            <p className="text-xs text-slate-400">Max DD</p>
                          </div>
                          <div>
                            <div className="text-lg font-semibold text-purple-400">
                              {trader.stats.sharpeRatio.toFixed(2)}
                            </div>
                            <p className="text-xs text-slate-400">Sharpe</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Discussions Tab */}
            {activeTab === 'discussions' && (
              <motion.div
                key="discussions"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {discussions.map((discussion) => (
                  <div
                    key={discussion.id}
                    className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all"
                  >
                    <div className="flex items-start space-x-4">
                      <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center text-white font-semibold">
                        {discussion.author.displayName.charAt(0)}
                      </div>

                      <div className="flex-1">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center space-x-2">
                            <h3 className="font-semibold text-white">{discussion.author.displayName}</h3>
                            {discussion.author.verified && (
                              <CheckCircle className="w-4 h-4 text-blue-400" />
                            )}
                            <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                              discussion.category === 'ANALYSIS' ? 'bg-blue-600/20 text-blue-400'
                                : discussion.category === 'SIGNAL' ? 'bg-green-600/20 text-green-400'
                                : discussion.category === 'NEWS' ? 'bg-purple-600/20 text-purple-400'
                                : discussion.category === 'QUESTION' ? 'bg-yellow-600/20 text-yellow-400'
                                : 'bg-cyan-600/20 text-cyan-400'
                            }`}>
                              {discussion.category}
                            </span>
                            {discussion.trending && (
                              <div className="flex items-center space-x-1 text-orange-400">
                                <Fire className="w-3 h-3" />
                                <span className="text-xs">Trending</span>
                              </div>
                            )}
                          </div>
                          
                          <div className="text-sm text-slate-400">
                            {new Date(discussion.timestamp).toLocaleTimeString()}
                          </div>
                        </div>

                        <h2 className="font-bold text-white text-lg mb-2">{discussion.title}</h2>
                        
                        {discussion.symbol && (
                          <div className="flex items-center space-x-2 mb-2">
                            <Target className="w-4 h-4 text-blue-400" />
                            <span className="text-blue-400 font-medium">{discussion.symbol}</span>
                          </div>
                        )}

                        <p className="text-slate-300 mb-4">{discussion.content}</p>

                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            {discussion.tags.map((tag) => (
                              <span
                                key={tag}
                                className="px-2 py-1 bg-slate-700/50 rounded-lg text-xs text-slate-400"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>

                          <div className="flex items-center space-x-4">
                            <div className="flex items-center space-x-1 text-slate-400">
                              <MessageSquare className="w-4 h-4" />
                              <span className="text-sm">{discussion.replies}</span>
                            </div>
                            <div className="flex items-center space-x-1 text-slate-400">
                              <ThumbsUp className="w-4 h-4" />
                              <span className="text-sm">{discussion.likes}</span>
                            </div>
                            <div className="flex items-center space-x-1 text-slate-400">
                              <Eye className="w-4 h-4" />
                              <span className="text-sm">{discussion.views}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}

            {/* My Feed Tab - Placeholder */}
            {activeTab === 'my-feed' && (
              <motion.div
                key="my-feed"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Heart className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Your Personalized Feed
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Follow traders and join discussions to see personalized content from your network here.
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Trending Symbols Sidebar */}
      <div className="glass-card rounded-2xl p-6">
        <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
          <Fire className="w-5 h-5 text-orange-400" />
          <span>Trending Symbols</span>
        </h3>
        
        <div className="space-y-3">
          {communityStats.trendingSymbols.map((item) => (
            <div
              key={item.symbol}
              className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg hover:bg-slate-700/30 transition-colors"
            >
              <div>
                <div className="font-semibold text-white">{item.symbol}</div>
                <div className="text-sm text-slate-400">
                  {item.mentions} mentions
                </div>
              </div>
              
              <div className="text-right">
                <div className={`font-semibold ${
                  item.priceChange >= 0 ? 'text-green-400' : 'text-red-400'
                }`}>
                  {item.priceChange >= 0 ? '+' : ''}{item.priceChange.toFixed(1)}%
                </div>
                <div className={`text-sm ${
                  item.sentiment > 0 ? 'text-green-400' : 'text-red-400'
                }`}>
                  {item.sentiment > 0 ? 'Bullish' : 'Bearish'}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default SocialTradingCenter