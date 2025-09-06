import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Activity, TrendingUp, TrendingDown, Clock, Target, Zap, BarChart3,
  ArrowUpRight, ArrowDownRight, DollarSign, Percent, Timer, Award,
  AlertTriangle, CheckCircle, XCircle, Eye, Filter, Search, Calendar,
  Settings, Download, RefreshCw, PlayCircle, PauseCircle, StopCircle,
  Users, Building, Globe, Briefcase, Star, Layers, PieChart, LineChart
} from 'lucide-react'

// Types for Trade Analytics
interface Trade {
  id: string
  timestamp: Date
  symbol: string
  side: 'BUY' | 'SELL'
  quantity: number
  executedPrice: number
  averagePrice: number
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT' | 'TWAP' | 'VWAP' | 'ICEBERG'
  timeInForce: 'DAY' | 'GTC' | 'IOC' | 'FOK'
  venue: string
  broker: string
  strategy?: string
  pnl: number
  pnlPercent: number
  commission: number
  marketImpact: number
  slippage: number
  fillRate: number
  executionTime: number // in milliseconds
  parentOrderId?: string
  tags: string[]
  metadata: {
    preTradeSpread: number
    postTradeSpread: number
    adv20: number // Average Daily Volume 20 days
    marketCapAtTrade: number
    volatilityAtTrade: number
    benchmarkPrice: number
  }
}

interface ExecutionMetrics {
  period: string
  totalTrades: number
  totalVolume: number
  totalPnL: number
  winRate: number
  avgWin: number
  avgLoss: number
  profitFactor: number
  largestWin: number
  largestLoss: number
  avgHoldingTime: number
  avgExecutionTime: number
  avgSlippage: number
  avgMarketImpact: number
  avgCommission: number
  shortfallRatio: number
  implementationShortfall: number
  vwapPerformance: number
  arrivalPricePerformance: number
  fillRateAverage: number
  cancellationRate: number
}

interface VenueAnalysis {
  venue: string
  broker: string
  tradesCount: number
  volumePercent: number
  avgFillRate: number
  avgExecutionTime: number
  avgSlippage: number
  avgMarketImpact: number
  avgCommission: number
  costScore: number
  speedScore: number
  qualityScore: number
  overallRating: 'EXCELLENT' | 'GOOD' | 'AVERAGE' | 'POOR'
}

interface AlgorithmicAnalysis {
  algorithm: string
  tradesCount: number
  avgParticipationRate: number
  completionRate: number
  avgSlippage: number
  marketImpactBps: number
  timingRisk: number
  opportunityCost: number
  implementationShortfall: number
  efficiency: number
  rating: 'A+' | 'A' | 'A-' | 'B+' | 'B' | 'B-' | 'C+' | 'C' | 'C-'
}

interface TradePattern {
  pattern: string
  frequency: number
  avgReturn: number
  successRate: number
  avgDuration: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  recommendation: string
  examples: string[]
}

interface MarketImpactAnalysis {
  orderSize: string
  avgImpact: number
  medianImpact: number
  impactVolatility: number
  permanentImpact: number
  temporaryImpact: number
  recoveryTime: number
  costBasisPoints: number
  sampleSize: number
}

// Mock data
const mockTrades: Trade[] = [
  {
    id: '1',
    timestamp: new Date('2024-01-15T09:30:00'),
    symbol: 'RELIANCE',
    side: 'BUY',
    quantity: 1000,
    executedPrice: 2456.75,
    averagePrice: 2456.75,
    orderType: 'TWAP',
    timeInForce: 'DAY',
    venue: 'NSE',
    broker: 'Zerodha',
    strategy: 'Momentum Breakout',
    pnl: 15420.50,
    pnlPercent: 0.63,
    commission: 23.45,
    marketImpact: 12.5,
    slippage: 8.2,
    fillRate: 100,
    executionTime: 1250,
    tags: ['Large Cap', 'Energy', 'Algo'],
    metadata: {
      preTradeSpread: 0.05,
      postTradeSpread: 0.07,
      adv20: 2500000,
      marketCapAtTrade: 1850000000000,
      volatilityAtTrade: 18.5,
      benchmarkPrice: 2448.30
    }
  },
  {
    id: '2',
    timestamp: new Date('2024-01-15T10:45:00'),
    symbol: 'TCS',
    side: 'SELL',
    quantity: 500,
    executedPrice: 3245.80,
    averagePrice: 3248.20,
    orderType: 'VWAP',
    timeInForce: 'DAY',
    venue: 'NSE',
    broker: 'ICICI Direct',
    strategy: 'Profit Taking',
    pnl: -2340.80,
    pnlPercent: -0.14,
    commission: 18.67,
    marketImpact: 15.8,
    slippage: 12.4,
    fillRate: 95.5,
    executionTime: 2800,
    tags: ['Large Cap', 'IT', 'Manual'],
    metadata: {
      preTradeSpread: 0.08,
      postTradeSpread: 0.12,
      adv20: 1800000,
      marketCapAtTrade: 1320000000000,
      volatilityAtTrade: 16.2,
      benchmarkPrice: 3250.15
    }
  }
]

const mockExecutionMetrics: ExecutionMetrics = {
  period: 'YTD 2024',
  totalTrades: 1247,
  totalVolume: 125000000,
  totalPnL: 2450000,
  winRate: 58.4,
  avgWin: 2850.75,
  avgLoss: -1960.40,
  profitFactor: 1.45,
  largestWin: 45680.90,
  largestLoss: -23450.60,
  avgHoldingTime: 2.8, // days
  avgExecutionTime: 1850, // milliseconds
  avgSlippage: 9.2, // basis points
  avgMarketImpact: 12.8, // basis points
  avgCommission: 25.40, // rupees
  shortfallRatio: 0.15,
  implementationShortfall: 18.5, // basis points
  vwapPerformance: -5.2, // basis points vs VWAP
  arrivalPricePerformance: 12.8, // basis points vs arrival price
  fillRateAverage: 94.2,
  cancellationRate: 3.8
}

const mockVenueAnalysis: VenueAnalysis[] = [
  {
    venue: 'NSE',
    broker: 'Zerodha',
    tradesCount: 456,
    volumePercent: 35.2,
    avgFillRate: 96.8,
    avgExecutionTime: 1200,
    avgSlippage: 8.5,
    avgMarketImpact: 11.2,
    avgCommission: 18.50,
    costScore: 85,
    speedScore: 92,
    qualityScore: 88,
    overallRating: 'EXCELLENT'
  },
  {
    venue: 'NSE',
    broker: 'ICICI Direct',
    tradesCount: 298,
    volumePercent: 28.7,
    avgFillRate: 94.2,
    avgExecutionTime: 1850,
    avgSlippage: 12.8,
    avgMarketImpact: 15.4,
    avgCommission: 24.80,
    costScore: 78,
    speedScore: 75,
    qualityScore: 82,
    overallRating: 'GOOD'
  },
  {
    venue: 'BSE',
    broker: 'HDFC Securities',
    tradesCount: 192,
    volumePercent: 18.9,
    avgFillRate: 91.5,
    avgExecutionTime: 2200,
    avgSlippage: 15.2,
    avgMarketImpact: 18.7,
    avgCommission: 32.40,
    costScore: 72,
    speedScore: 68,
    qualityScore: 75,
    overallRating: 'AVERAGE'
  }
]

const mockAlgorithmicAnalysis: AlgorithmicAnalysis[] = [
  {
    algorithm: 'TWAP',
    tradesCount: 145,
    avgParticipationRate: 12.5,
    completionRate: 98.2,
    avgSlippage: 6.8,
    marketImpactBps: 8.9,
    timingRisk: 15.2,
    opportunityCost: 12.5,
    implementationShortfall: 14.8,
    efficiency: 87.5,
    rating: 'A'
  },
  {
    algorithm: 'VWAP',
    tradesCount: 89,
    avgParticipationRate: 18.7,
    completionRate: 96.1,
    avgSlippage: 9.2,
    marketImpactBps: 12.4,
    timingRisk: 22.8,
    opportunityCost: 18.9,
    implementationShortfall: 19.6,
    efficiency: 82.1,
    rating: 'A-'
  },
  {
    algorithm: 'Implementation Shortfall',
    tradesCount: 67,
    avgParticipationRate: 25.4,
    completionRate: 94.8,
    avgSlippage: 11.8,
    marketImpactBps: 15.7,
    timingRisk: 28.5,
    opportunityCost: 22.4,
    implementationShortfall: 23.2,
    efficiency: 78.9,
    rating: 'B+'
  }
]

const mockTradePatterns: TradePattern[] = [
  {
    pattern: 'Opening Range Breakout',
    frequency: 23,
    avgReturn: 1.45,
    successRate: 68.2,
    avgDuration: 2.3,
    riskLevel: 'MEDIUM',
    recommendation: 'Maintain current position sizing',
    examples: ['RELIANCE 15-Jan', 'TCS 18-Jan', 'INFY 22-Jan']
  },
  {
    pattern: 'Mean Reversion after Gap Down',
    frequency: 18,
    avgReturn: 0.85,
    successRate: 72.8,
    avgDuration: 1.8,
    riskLevel: 'LOW',
    recommendation: 'Consider increasing allocation',
    examples: ['HDFC 12-Jan', 'ICICIBANK 20-Jan']
  },
  {
    pattern: 'Momentum Continuation',
    frequency: 31,
    avgReturn: 2.15,
    successRate: 62.1,
    avgDuration: 3.8,
    riskLevel: 'HIGH',
    recommendation: 'Optimize entry timing',
    examples: ['TATAMOTORS 16-Jan', 'BHARTIARTL 19-Jan']
  }
]

const mockMarketImpactAnalysis: MarketImpactAnalysis[] = [
  {
    orderSize: '< 0.1% ADV',
    avgImpact: 2.5,
    medianImpact: 1.8,
    impactVolatility: 1.2,
    permanentImpact: 0.8,
    temporaryImpact: 1.7,
    recoveryTime: 3.2,
    costBasisPoints: 2.5,
    sampleSize: 245
  },
  {
    orderSize: '0.1% - 0.5% ADV',
    avgImpact: 8.7,
    medianImpact: 6.4,
    impactVolatility: 4.2,
    permanentImpact: 2.8,
    temporaryImpact: 5.9,
    recoveryTime: 8.5,
    costBasisPoints: 8.7,
    sampleSize: 189
  },
  {
    orderSize: '0.5% - 1.0% ADV',
    avgImpact: 18.2,
    medianImpact: 14.8,
    impactVolatility: 8.9,
    permanentImpact: 6.4,
    temporaryImpact: 11.8,
    recoveryTime: 15.7,
    costBasisPoints: 18.2,
    sampleSize: 98
  },
  {
    orderSize: '> 1.0% ADV',
    avgImpact: 35.8,
    medianImpact: 28.4,
    impactVolatility: 18.5,
    permanentImpact: 12.8,
    temporaryImpact: 23.0,
    recoveryTime: 32.4,
    costBasisPoints: 35.8,
    sampleSize: 42
  }
]

interface TradeAnalyticsCenterProps {
  className?: string
}

export const TradeAnalyticsCenter: React.FC<TradeAnalyticsCenterProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'overview' | 'execution' | 'venues' | 'algos' | 'patterns' | 'impact'>('overview')
  const [selectedPeriod, setSelectedPeriod] = useState<'1D' | '1W' | '1M' | 'YTD' | '1Y'>('YTD')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedTrade, setSelectedTrade] = useState<Trade | null>(null)
  
  // Filter state
  const [filterSide, setFilterSide] = useState<'ALL' | 'BUY' | 'SELL'>('ALL')
  const [filterVenue, setFilterVenue] = useState<'ALL' | 'NSE' | 'BSE'>('ALL')
  const [filterStrategy, setFilterStrategy] = useState<string>('ALL')
  
  // Data state
  const [trades] = useState<Trade[]>(mockTrades)
  const [metrics] = useState<ExecutionMetrics>(mockExecutionMetrics)
  const [venueData] = useState<VenueAnalysis[]>(mockVenueAnalysis)
  const [algoData] = useState<AlgorithmicAnalysis[]>(mockAlgorithmicAnalysis)
  const [patterns] = useState<TradePattern[]>(mockTradePatterns)
  const [impactData] = useState<MarketImpactAnalysis[]>(mockMarketImpactAnalysis)
  
  // UI state
  const [isLoading, setIsLoading] = useState(false)
  const [showFilters, setShowFilters] = useState(false)

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => setIsLoading(false), 2000)
  }

  const getVenueRatingColor = (rating: string) => {
    const colors = {
      EXCELLENT: 'text-green-400',
      GOOD: 'text-blue-400',
      AVERAGE: 'text-yellow-400',
      POOR: 'text-red-400'
    }
    return colors[rating as keyof typeof colors]
  }

  const getRatingColor = (rating: string) => {
    if (rating.startsWith('A')) return 'text-green-400'
    if (rating.startsWith('B')) return 'text-yellow-400'
    return 'text-red-400'
  }

  const getRiskColor = (risk: string) => {
    const colors = {
      LOW: 'text-green-400',
      MEDIUM: 'text-yellow-400',
      HIGH: 'text-red-400'
    }
    return colors[risk as keyof typeof colors]
  }

  // Filter trades
  const filteredTrades = trades.filter(trade => {
    if (filterSide !== 'ALL' && trade.side !== filterSide) return false
    if (filterVenue !== 'ALL' && trade.venue !== filterVenue) return false
    if (filterStrategy !== 'ALL' && trade.strategy !== filterStrategy) return false
    if (searchQuery && !trade.symbol.toLowerCase().includes(searchQuery.toLowerCase())) return false
    return true
  })

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-purple-600 to-pink-600 rounded-xl flex items-center justify-center">
              <Activity className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Trade Analytics Center
              </h1>
              <p className="text-slate-400">
                Advanced trade execution analysis and performance optimization
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Period Selector */}
            <div className="flex items-center space-x-1">
              {['1D', '1W', '1M', 'YTD', '1Y'].map((period) => (
                <button
                  key={period}
                  onClick={() => setSelectedPeriod(period as typeof selectedPeriod)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                    selectedPeriod === period
                      ? 'bg-purple-600 text-white'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`}
                >
                  {period}
                </button>
              ))}
            </div>
            
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`p-2 rounded-lg transition-colors ${
                showFilters ? 'bg-purple-600' : 'bg-slate-800/50 hover:bg-slate-700/50'
              }`}
            >
              <Filter className="w-4 h-4 text-white" />
            </button>

            <button
              onClick={handleRefresh}
              disabled={isLoading}
              className="p-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-5 h-5 text-white ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Filters */}
        <AnimatePresence>
          {showFilters && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-700/50 pt-4 mb-4"
            >
              <div className="grid grid-cols-4 gap-4">
                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Side</label>
                  <select
                    value={filterSide}
                    onChange={(e) => setFilterSide(e.target.value as typeof filterSide)}
                    className="bg-slate-700 text-white rounded-lg px-3 py-2 text-sm border border-slate-600 w-full"
                  >
                    <option value="ALL">All Sides</option>
                    <option value="BUY">Buy Only</option>
                    <option value="SELL">Sell Only</option>
                  </select>
                </div>

                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Venue</label>
                  <select
                    value={filterVenue}
                    onChange={(e) => setFilterVenue(e.target.value as typeof filterVenue)}
                    className="bg-slate-700 text-white rounded-lg px-3 py-2 text-sm border border-slate-600 w-full"
                  >
                    <option value="ALL">All Venues</option>
                    <option value="NSE">NSE</option>
                    <option value="BSE">BSE</option>
                  </select>
                </div>

                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Strategy</label>
                  <select
                    value={filterStrategy}
                    onChange={(e) => setFilterStrategy(e.target.value)}
                    className="bg-slate-700 text-white rounded-lg px-3 py-2 text-sm border border-slate-600 w-full"
                  >
                    <option value="ALL">All Strategies</option>
                    <option value="Momentum Breakout">Momentum Breakout</option>
                    <option value="Profit Taking">Profit Taking</option>
                  </select>
                </div>

                <div>
                  <label className="text-sm text-slate-400 mb-2 block">Search</label>
                  <div className="relative">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Symbol..."
                      className="pl-10 pr-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 text-sm w-full focus:border-purple-500 focus:outline-none"
                    />
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Key Metrics */}
        <div className="grid grid-cols-6 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {metrics.totalTrades.toLocaleString()}
            </div>
            <p className="text-sm text-slate-400">Total Trades</p>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {metrics.winRate.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Win Rate</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {metrics.profitFactor.toFixed(2)}
            </div>
            <p className="text-sm text-slate-400">Profit Factor</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-400 mb-1">
              {metrics.avgSlippage.toFixed(1)}
            </div>
            <p className="text-sm text-slate-400">Avg Slippage (bps)</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-cyan-400 mb-1">
              {metrics.fillRateAverage.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Avg Fill Rate</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-orange-400 mb-1">
              {metrics.avgExecutionTime.toFixed(0)}ms
            </div>
            <p className="text-sm text-slate-400">Avg Exec Time</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'overview', label: 'Execution Overview', icon: BarChart3 },
              { id: 'execution', label: 'Execution Quality', icon: Target },
              { id: 'venues', label: 'Venue Analysis', icon: Building },
              { id: 'algos', label: 'Algorithm Analysis', icon: Zap },
              { id: 'patterns', label: 'Trade Patterns', icon: Layers },
              { id: 'impact', label: 'Market Impact', icon: TrendingUp }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>

          <div className="flex items-center space-x-2">
            <button className="p-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors">
              <Download className="w-4 h-4 text-white" />
            </button>
            <button className="p-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors">
              <Settings className="w-4 h-4 text-white" />
            </button>
          </div>
        </div>

        {/* Tab Content */}
        <div className="mt-6">
          <AnimatePresence mode="wait">
            {/* Execution Overview Tab */}
            {activeTab === 'overview' && (
              <motion.div
                key="overview"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* P&L Analysis */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <DollarSign className="w-5 h-5 text-green-400" />
                    <span>P&L Analysis</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Total P&L</span>
                      <span className="font-bold text-green-400">
                        ₹{metrics.totalPnL.toLocaleString()}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Win Rate</span>
                      <span className="font-bold text-blue-400">
                        {metrics.winRate.toFixed(1)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Avg Win</span>
                      <span className="font-bold text-green-400">
                        ₹{metrics.avgWin.toLocaleString()}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Avg Loss</span>
                      <span className="font-bold text-red-400">
                        ₹{metrics.avgLoss.toLocaleString()}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Profit Factor</span>
                      <span className="font-bold text-purple-400">
                        {metrics.profitFactor.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Execution Costs */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Percent className="w-5 h-5 text-yellow-400" />
                    <span>Execution Costs</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Avg Slippage</span>
                      <span className="font-bold text-yellow-400">
                        {metrics.avgSlippage.toFixed(1)} bps
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Market Impact</span>
                      <span className="font-bold text-orange-400">
                        {metrics.avgMarketImpact.toFixed(1)} bps
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Commission</span>
                      <span className="font-bold text-slate-300">
                        ₹{metrics.avgCommission.toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Implementation Shortfall</span>
                      <span className="font-bold text-red-400">
                        {metrics.implementationShortfall.toFixed(1)} bps
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">VWAP Performance</span>
                      <span className={`font-bold ${
                        metrics.vwapPerformance >= 0 ? 'text-green-400' : 'text-red-400'
                      }`}>
                        {metrics.vwapPerformance >= 0 ? '+' : ''}{metrics.vwapPerformance.toFixed(1)} bps
                      </span>
                    </div>
                  </div>
                </div>

                {/* Timing Analysis */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Clock className="w-5 h-5 text-blue-400" />
                    <span>Timing Analysis</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Avg Holding Time</span>
                      <span className="font-bold text-blue-400">
                        {metrics.avgHoldingTime.toFixed(1)} days
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Avg Execution Time</span>
                      <span className="font-bold text-cyan-400">
                        {metrics.avgExecutionTime.toFixed(0)} ms
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Fill Rate</span>
                      <span className="font-bold text-green-400">
                        {metrics.fillRateAverage.toFixed(1)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Cancellation Rate</span>
                      <span className="font-bold text-red-400">
                        {metrics.cancellationRate.toFixed(1)}%
                      </span>
                    </div>
                  </div>
                </div>

                {/* Recent Trades */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Activity className="w-5 h-5 text-purple-400" />
                    <span>Recent Trades</span>
                  </h3>
                  
                  <div className="space-y-3">
                    {filteredTrades.slice(0, 5).map((trade) => (
                      <div
                        key={trade.id}
                        className="p-3 bg-slate-700/30 rounded-lg hover:bg-slate-700/50 transition-colors cursor-pointer"
                        onClick={() => setSelectedTrade(trade)}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex items-center space-x-2">
                            {trade.side === 'BUY' ? (
                              <ArrowUpRight className="w-4 h-4 text-green-400" />
                            ) : (
                              <ArrowDownRight className="w-4 h-4 text-red-400" />
                            )}
                            <span className="font-medium text-white">{trade.symbol}</span>
                            <span className="text-xs text-slate-400">{trade.quantity}</span>
                          </div>
                          
                          <div className="text-right">
                            <div className={`text-sm font-bold ${
                              trade.pnl >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {trade.pnl >= 0 ? '+' : ''}₹{trade.pnl.toLocaleString()}
                            </div>
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between text-xs text-slate-400">
                          <span>{trade.orderType} @ {trade.venue}</span>
                          <span>Slippage: {trade.slippage.toFixed(1)}bps</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Execution Quality Tab */}
            {activeTab === 'execution' && (
              <motion.div
                key="execution"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Target className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Execution Quality Metrics
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Advanced execution quality analysis including arrival price performance, timing risk, and opportunity cost measurement.
                </p>
              </motion.div>
            )}

            {/* Venue Analysis Tab */}
            {activeTab === 'venues' && (
              <motion.div
                key="venues"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Venue Performance Comparison</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Venue</th>
                          <th className="text-left p-4 text-sm text-slate-400">Broker</th>
                          <th className="text-right p-4 text-sm text-slate-400">Trades</th>
                          <th className="text-right p-4 text-sm text-slate-400">Volume %</th>
                          <th className="text-right p-4 text-sm text-slate-400">Fill Rate</th>
                          <th className="text-right p-4 text-sm text-slate-400">Exec Time</th>
                          <th className="text-right p-4 text-sm text-slate-400">Slippage</th>
                          <th className="text-right p-4 text-sm text-slate-400">Commission</th>
                          <th className="text-center p-4 text-sm text-slate-400">Rating</th>
                        </tr>
                      </thead>
                      <tbody>
                        {venueData.map((venue) => (
                          <tr
                            key={`${venue.venue}-${venue.broker}`}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {venue.venue}
                            </td>
                            <td className="p-4 text-slate-300">
                              {venue.broker}
                            </td>
                            <td className="p-4 text-right text-white">
                              {venue.tradesCount}
                            </td>
                            <td className="p-4 text-right text-white">
                              {venue.volumePercent.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-green-400">
                              {venue.avgFillRate.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-blue-400">
                              {venue.avgExecutionTime.toFixed(0)}ms
                            </td>
                            <td className="p-4 text-right text-yellow-400">
                              {venue.avgSlippage.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-slate-300">
                              ₹{venue.avgCommission.toFixed(2)}
                            </td>
                            <td className="p-4 text-center">
                              <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                                venue.overallRating === 'EXCELLENT' ? 'bg-green-600/20 text-green-400' :
                                venue.overallRating === 'GOOD' ? 'bg-blue-600/20 text-blue-400' :
                                venue.overallRating === 'AVERAGE' ? 'bg-yellow-600/20 text-yellow-400' :
                                'bg-red-600/20 text-red-400'
                              }`}>
                                {venue.overallRating}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Venue Score Cards */}
                <div className="grid grid-cols-3 gap-6">
                  {venueData.map((venue) => (
                    <div key={`${venue.venue}-${venue.broker}`} className="bg-slate-800/30 rounded-xl p-6">
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <h4 className="font-bold text-white">{venue.venue}</h4>
                          <p className="text-sm text-slate-400">{venue.broker}</p>
                        </div>
                        <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                          venue.overallRating === 'EXCELLENT' ? 'bg-green-600/20 text-green-400' :
                          venue.overallRating === 'GOOD' ? 'bg-blue-600/20 text-blue-400' :
                          venue.overallRating === 'AVERAGE' ? 'bg-yellow-600/20 text-yellow-400' :
                          'bg-red-600/20 text-red-400'
                        }`}>
                          {venue.overallRating}
                        </span>
                      </div>

                      <div className="space-y-3">
                        <div className="flex justify-between">
                          <span className="text-sm text-slate-400">Cost Score</span>
                          <div className="flex items-center space-x-2">
                            <div className="w-16 bg-slate-700 rounded-full h-2">
                              <div 
                                className="bg-green-400 h-2 rounded-full"
                                style={{ width: `${venue.costScore}%` }}
                              />
                            </div>
                            <span className="text-sm text-white">{venue.costScore}</span>
                          </div>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-sm text-slate-400">Speed Score</span>
                          <div className="flex items-center space-x-2">
                            <div className="w-16 bg-slate-700 rounded-full h-2">
                              <div 
                                className="bg-blue-400 h-2 rounded-full"
                                style={{ width: `${venue.speedScore}%` }}
                              />
                            </div>
                            <span className="text-sm text-white">{venue.speedScore}</span>
                          </div>
                        </div>

                        <div className="flex justify-between">
                          <span className="text-sm text-slate-400">Quality Score</span>
                          <div className="flex items-center space-x-2">
                            <div className="w-16 bg-slate-700 rounded-full h-2">
                              <div 
                                className="bg-purple-400 h-2 rounded-full"
                                style={{ width: `${venue.qualityScore}%` }}
                              />
                            </div>
                            <span className="text-sm text-white">{venue.qualityScore}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Algorithm Analysis Tab */}
            {activeTab === 'algos' && (
              <motion.div
                key="algos"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Algorithmic Trading Performance</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Algorithm</th>
                          <th className="text-right p-4 text-sm text-slate-400">Trades</th>
                          <th className="text-right p-4 text-sm text-slate-400">Participation</th>
                          <th className="text-right p-4 text-sm text-slate-400">Completion</th>
                          <th className="text-right p-4 text-sm text-slate-400">Slippage</th>
                          <th className="text-right p-4 text-sm text-slate-400">Impact</th>
                          <th className="text-right p-4 text-sm text-slate-400">Efficiency</th>
                          <th className="text-center p-4 text-sm text-slate-400">Rating</th>
                        </tr>
                      </thead>
                      <tbody>
                        {algoData.map((algo) => (
                          <tr
                            key={algo.algorithm}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {algo.algorithm}
                            </td>
                            <td className="p-4 text-right text-white">
                              {algo.tradesCount}
                            </td>
                            <td className="p-4 text-right text-blue-400">
                              {algo.avgParticipationRate.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-green-400">
                              {algo.completionRate.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-yellow-400">
                              {algo.avgSlippage.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-orange-400">
                              {algo.marketImpactBps.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-purple-400">
                              {algo.efficiency.toFixed(1)}%
                            </td>
                            <td className="p-4 text-center">
                              <span className={`px-2 py-1 rounded-lg text-xs font-bold ${getRatingColor(algo.rating)}`}>
                                {algo.rating}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Trade Patterns Tab */}
            {activeTab === 'patterns' && (
              <motion.div
                key="patterns"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                {patterns.map((pattern) => (
                  <div key={pattern.pattern} className="bg-slate-800/30 rounded-xl p-6">
                    <div className="flex items-center justify-between mb-4">
                      <div>
                        <h3 className="font-bold text-white mb-1">{pattern.pattern}</h3>
                        <div className="flex items-center space-x-4 text-sm text-slate-400">
                          <span>Frequency: {pattern.frequency} trades</span>
                          <span>Avg Duration: {pattern.avgDuration} days</span>
                          <span className={getRiskColor(pattern.riskLevel)}>
                            {pattern.riskLevel} Risk
                          </span>
                        </div>
                      </div>

                      <div className="text-right">
                        <div className="text-2xl font-bold text-green-400 mb-1">
                          +{pattern.avgReturn.toFixed(2)}%
                        </div>
                        <div className="text-sm text-slate-400">Avg Return</div>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-6">
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-slate-400">Success Rate</span>
                          <span className="font-bold text-white">{pattern.successRate.toFixed(1)}%</span>
                        </div>
                        <div className="w-full bg-slate-700 rounded-full h-2">
                          <div 
                            className="bg-green-400 h-2 rounded-full"
                            style={{ width: `${pattern.successRate}%` }}
                          />
                        </div>
                      </div>

                      <div>
                        <p className="text-sm text-slate-400 mb-2">Recommendation:</p>
                        <p className="text-sm text-green-400 font-medium">{pattern.recommendation}</p>
                      </div>
                    </div>

                    <div className="mt-4 pt-4 border-t border-slate-700/50">
                      <p className="text-sm text-slate-400 mb-2">Recent Examples:</p>
                      <div className="flex items-center space-x-2">
                        {pattern.examples.map((example) => (
                          <span key={example} className="px-2 py-1 bg-slate-700/50 rounded text-xs text-slate-300">
                            {example}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}

            {/* Market Impact Tab */}
            {activeTab === 'impact' && (
              <motion.div
                key="impact"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Market Impact by Order Size</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Order Size</th>
                          <th className="text-right p-4 text-sm text-slate-400">Avg Impact</th>
                          <th className="text-right p-4 text-sm text-slate-400">Median</th>
                          <th className="text-right p-4 text-sm text-slate-400">Permanent</th>
                          <th className="text-right p-4 text-sm text-slate-400">Temporary</th>
                          <th className="text-right p-4 text-sm text-slate-400">Recovery</th>
                          <th className="text-right p-4 text-sm text-slate-400">Cost (bps)</th>
                          <th className="text-right p-4 text-sm text-slate-400">Sample</th>
                        </tr>
                      </thead>
                      <tbody>
                        {impactData.map((impact) => (
                          <tr
                            key={impact.orderSize}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {impact.orderSize}
                            </td>
                            <td className="p-4 text-right text-yellow-400">
                              {impact.avgImpact.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-white">
                              {impact.medianImpact.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-red-400">
                              {impact.permanentImpact.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-orange-400">
                              {impact.temporaryImpact.toFixed(1)}bps
                            </td>
                            <td className="p-4 text-right text-blue-400">
                              {impact.recoveryTime.toFixed(1)}min
                            </td>
                            <td className="p-4 text-right font-bold text-purple-400">
                              {impact.costBasisPoints.toFixed(1)}
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {impact.sampleSize}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Trade Detail Modal */}
      <AnimatePresence>
        {selectedTrade && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedTrade(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-slate-900 rounded-2xl p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">
                  Trade Details - {selectedTrade.symbol}
                </h2>
                <button
                  onClick={() => setSelectedTrade(null)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Side:</span>
                    <span className={selectedTrade.side === 'BUY' ? 'text-green-400' : 'text-red-400'}>
                      {selectedTrade.side}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Quantity:</span>
                    <span className="text-white">{selectedTrade.quantity.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Executed Price:</span>
                    <span className="text-white">₹{selectedTrade.executedPrice.toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Order Type:</span>
                    <span className="text-white">{selectedTrade.orderType}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Venue:</span>
                    <span className="text-white">{selectedTrade.venue}</span>
                  </div>
                </div>

                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">P&L:</span>
                    <span className={selectedTrade.pnl >= 0 ? 'text-green-400' : 'text-red-400'}>
                      {selectedTrade.pnl >= 0 ? '+' : ''}₹{selectedTrade.pnl.toLocaleString()}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Slippage:</span>
                    <span className="text-yellow-400">{selectedTrade.slippage.toFixed(1)} bps</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Market Impact:</span>
                    <span className="text-orange-400">{selectedTrade.marketImpact.toFixed(1)} bps</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Commission:</span>
                    <span className="text-white">₹{selectedTrade.commission.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Execution Time:</span>
                    <span className="text-blue-400">{selectedTrade.executionTime}ms</span>
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-slate-700">
                <div className="text-sm text-slate-400 mb-2">Tags:</div>
                <div className="flex items-center space-x-2">
                  {selectedTrade.tags.map((tag) => (
                    <span key={tag} className="px-2 py-1 bg-slate-700/50 rounded text-xs text-slate-300">
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default TradeAnalyticsCenter