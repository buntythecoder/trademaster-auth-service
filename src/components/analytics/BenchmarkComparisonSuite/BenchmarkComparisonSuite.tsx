import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Target, TrendingUp, TrendingDown, BarChart3, PieChart, Activity,
  Award, Zap, AlertTriangle, Clock, Calendar, Settings, Download,
  RefreshCw, Filter, Search, ChevronUp, ChevronDown, Eye, EyeOff,
  Star, Globe, Building, Users, Briefcase, ArrowUpRight, ArrowDownRight,
  Shield, DollarSign, Percent, Gauge, Trophy, LineChart, Layers
} from 'lucide-react'

// Types for Benchmark Comparison
interface Benchmark {
  id: string
  name: string
  symbol: string
  description: string
  type: 'INDEX' | 'PEER_GROUP' | 'CUSTOM' | 'COMPOSITE'
  category: 'BROAD_MARKET' | 'SECTOR' | 'STYLE' | 'REGIONAL' | 'THEMATIC'
  region: string
  currency: string
  inception: Date
  aum?: number
  provider: string
  active: boolean
}

interface PerformanceComparison {
  period: '1D' | '1W' | '1M' | '3M' | '6M' | 'YTD' | '1Y' | '3Y' | '5Y' | '10Y' | 'ITD'
  portfolio: {
    return: number
    volatility: number
    sharpe: number
    maxDrawdown: number
    calmarRatio: number
    sortinoRatio: number
    beta: number
    alpha: number
    trackingError: number
    informationRatio: number
    upCapture: number
    downCapture: number
  }
  benchmark: {
    return: number
    volatility: number
    sharpe: number
    maxDrawdown: number
    calmarRatio: number
    sortinoRatio: number
  }
  relative: {
    excessReturn: number
    relativeVolatility: number
    relativeSharpe: number
    relativeMaxDD: number
    outperformanceProbability: number
    winRate: number
    avgWin: number
    avgLoss: number
    winLossRatio: number
  }
}

interface RiskAdjustedMetrics {
  sharpeRatio: number
  treynorRatio: number
  jensenAlpha: number
  informationRatio: number
  calmarRatio: number
  sortinoRatio: number
  omegaRatio: number
  sterlingRatio: number
  burkeRatio: number
  martinRatio: number
  painIndex: number
  ulcerIndex: number
  valueatRisk95: number
  valueatRisk99: number
  conditionalVaR95: number
  conditionalVaR99: number
  maximumDrawdown: number
  averageDrawdown: number
  drawdownDuration: number
  recoveryTime: number
}

interface StyleAnalysis {
  styleFactor: string
  portfolioExposure: number
  benchmarkExposure: number
  activeExposure: number
  contribution: number
  tStat: number
  significance: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE'
}

interface PeerComparison {
  metric: string
  portfolioValue: number
  peerPercentile: number
  peerAverage: number
  peerMedian: number
  ranking: number
  totalPeers: number
  quartile: 1 | 2 | 3 | 4
}

interface RollingMetrics {
  date: Date
  portfolioReturn: number
  benchmarkReturn: number
  excessReturn: number
  trackingError: number
  informationRatio: number
  beta: number
  alpha: number
  correlation: number
  upCapture: number
  downCapture: number
}

// Mock data
const mockBenchmarks: Benchmark[] = [
  {
    id: '1',
    name: 'Nifty 50',
    symbol: 'NIFTY50',
    description: 'NSE Nifty 50 Index - Blue-chip Indian equities',
    type: 'INDEX',
    category: 'BROAD_MARKET',
    region: 'India',
    currency: 'INR',
    inception: new Date('1996-04-22'),
    provider: 'NSE',
    active: true
  },
  {
    id: '2',
    name: 'BSE Sensex',
    symbol: 'SENSEX',
    description: 'Bombay Stock Exchange Sensitive Index',
    type: 'INDEX',
    category: 'BROAD_MARKET',
    region: 'India',
    currency: 'INR',
    inception: new Date('1986-01-01'),
    provider: 'BSE',
    active: true
  },
  {
    id: '3',
    name: 'Nifty IT',
    symbol: 'CNXIT',
    description: 'Nifty Information Technology Index',
    type: 'INDEX',
    category: 'SECTOR',
    region: 'India',
    currency: 'INR',
    inception: new Date('2003-01-01'),
    provider: 'NSE',
    active: false
  },
  {
    id: '4',
    name: 'Large Cap Mutual Funds',
    symbol: 'LARGECAP_MF',
    description: 'Peer group of large-cap mutual funds in India',
    type: 'PEER_GROUP',
    category: 'STYLE',
    region: 'India',
    currency: 'INR',
    inception: new Date('2010-01-01'),
    aum: 2500000000000, // 2.5 trillion
    provider: 'AMFI',
    active: true
  },
  {
    id: '5',
    name: 'MSCI India',
    symbol: 'MSCI_INDIA',
    description: 'MSCI India Index - International perspective',
    type: 'INDEX',
    category: 'REGIONAL',
    region: 'India',
    currency: 'USD',
    inception: new Date('1993-05-31'),
    provider: 'MSCI',
    active: false
  }
]

const mockPerformanceData: Record<string, PerformanceComparison> = {
  'YTD': {
    period: 'YTD',
    portfolio: {
      return: 18.45,
      volatility: 16.8,
      sharpe: 1.09,
      maxDrawdown: -8.2,
      calmarRatio: 2.25,
      sortinoRatio: 1.54,
      beta: 0.95,
      alpha: 5.2,
      trackingError: 4.8,
      informationRatio: 1.08,
      upCapture: 110.2,
      downCapture: 88.5
    },
    benchmark: {
      return: 12.80,
      volatility: 15.2,
      sharpe: 0.84,
      maxDrawdown: -12.1,
      calmarRatio: 1.06,
      sortinoRatio: 1.18
    },
    relative: {
      excessReturn: 5.65,
      relativeVolatility: 1.6,
      relativeSharpe: 0.25,
      relativeMaxDD: 3.9,
      outperformanceProbability: 68.5,
      winRate: 62.8,
      avgWin: 1.8,
      avgLoss: -1.2,
      winLossRatio: 1.5
    }
  },
  '1Y': {
    period: '1Y',
    portfolio: {
      return: 24.8,
      volatility: 18.2,
      sharpe: 1.36,
      maxDrawdown: -15.3,
      calmarRatio: 1.62,
      sortinoRatio: 1.89,
      beta: 0.92,
      alpha: 8.4,
      trackingError: 6.2,
      informationRatio: 1.35,
      upCapture: 108.5,
      downCapture: 85.2
    },
    benchmark: {
      return: 16.2,
      volatility: 16.8,
      sharpe: 0.96,
      maxDrawdown: -18.7,
      calmarRatio: 0.87,
      sortinoRatio: 1.28
    },
    relative: {
      excessReturn: 8.6,
      relativeVolatility: 1.4,
      relativeSharpe: 0.4,
      relativeMaxDD: 3.4,
      outperformanceProbability: 72.3,
      winRate: 65.2,
      avgWin: 2.1,
      avgLoss: -1.4,
      winLossRatio: 1.5
    }
  },
  '3Y': {
    period: '3Y',
    portfolio: {
      return: 15.8,
      volatility: 17.5,
      sharpe: 0.90,
      maxDrawdown: -22.4,
      calmarRatio: 0.71,
      sortinoRatio: 1.24,
      beta: 0.88,
      alpha: 4.2,
      trackingError: 7.1,
      informationRatio: 0.59,
      upCapture: 105.8,
      downCapture: 92.3
    },
    benchmark: {
      return: 11.5,
      volatility: 18.9,
      sharpe: 0.61,
      maxDrawdown: -28.2,
      calmarRatio: 0.41,
      sortinoRatio: 0.84
    },
    relative: {
      excessReturn: 4.3,
      relativeVolatility: -1.4,
      relativeSharpe: 0.29,
      relativeMaxDD: 5.8,
      outperformanceProbability: 58.7,
      winRate: 56.4,
      avgWin: 1.9,
      avgLoss: -1.5,
      winLossRatio: 1.27
    }
  }
}

const mockStyleAnalysis: StyleAnalysis[] = [
  {
    styleFactor: 'Market Beta',
    portfolioExposure: 0.95,
    benchmarkExposure: 1.00,
    activeExposure: -0.05,
    contribution: -12,
    tStat: -1.8,
    significance: 'MEDIUM'
  },
  {
    styleFactor: 'Size Factor',
    portfolioExposure: 0.15,
    benchmarkExposure: 0.08,
    activeExposure: 0.07,
    contribution: 45,
    tStat: 2.4,
    significance: 'HIGH'
  },
  {
    styleFactor: 'Value Factor',
    portfolioExposure: -0.08,
    benchmarkExposure: 0.02,
    activeExposure: -0.10,
    contribution: -28,
    tStat: -1.9,
    significance: 'MEDIUM'
  },
  {
    styleFactor: 'Quality Factor',
    portfolioExposure: 0.22,
    benchmarkExposure: 0.12,
    activeExposure: 0.10,
    contribution: 67,
    tStat: 3.1,
    significance: 'HIGH'
  },
  {
    styleFactor: 'Momentum Factor',
    portfolioExposure: 0.18,
    benchmarkExposure: 0.05,
    activeExposure: 0.13,
    contribution: 82,
    tStat: 3.8,
    significance: 'HIGH'
  },
  {
    styleFactor: 'Low Volatility',
    portfolioExposure: -0.12,
    benchmarkExposure: -0.03,
    activeExposure: -0.09,
    contribution: -23,
    tStat: -1.2,
    significance: 'LOW'
  }
]

const mockPeerComparison: PeerComparison[] = [
  {
    metric: 'Total Return (YTD)',
    portfolioValue: 18.45,
    peerPercentile: 78,
    peerAverage: 14.2,
    peerMedian: 13.8,
    ranking: 23,
    totalPeers: 105,
    quartile: 1
  },
  {
    metric: 'Sharpe Ratio',
    portfolioValue: 1.09,
    peerPercentile: 72,
    peerAverage: 0.89,
    peerMedian: 0.91,
    ranking: 29,
    totalPeers: 105,
    quartile: 1
  },
  {
    metric: 'Maximum Drawdown',
    portfolioValue: -8.2,
    peerPercentile: 85,
    peerAverage: -11.8,
    peerMedian: -10.5,
    ranking: 16,
    totalPeers: 105,
    quartile: 1
  },
  {
    metric: 'Information Ratio',
    portfolioValue: 1.08,
    peerPercentile: 81,
    peerAverage: 0.65,
    peerMedian: 0.72,
    ranking: 20,
    totalPeers: 105,
    quartile: 1
  },
  {
    metric: 'Tracking Error',
    portfolioValue: 4.8,
    peerPercentile: 42,
    peerAverage: 5.2,
    peerMedian: 4.9,
    ranking: 61,
    totalPeers: 105,
    quartile: 2
  }
]

interface BenchmarkComparisonSuiteProps {
  className?: string
}

export const BenchmarkComparisonSuite: React.FC<BenchmarkComparisonSuiteProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'overview' | 'risk-adjusted' | 'style' | 'peer' | 'rolling'>('overview')
  const [selectedPeriod, setSelectedPeriod] = useState<'YTD' | '1Y' | '3Y' | '5Y' | 'ITD'>('YTD')
  const [selectedBenchmarks, setSelectedBenchmarks] = useState<string[]>(['1', '2'])
  const [showBenchmarkSelector, setShowBenchmarkSelector] = useState(false)
  
  // Data state
  const [benchmarks] = useState<Benchmark[]>(mockBenchmarks)
  const [performanceData] = useState(mockPerformanceData)
  const [styleAnalysis] = useState<StyleAnalysis[]>(mockStyleAnalysis)
  const [peerData] = useState<PeerComparison[]>(mockPeerComparison)
  
  // UI state
  const [isLoading, setIsLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  const currentPerformance = performanceData[selectedPeriod] || performanceData['YTD']
  const activeBenchmarks = benchmarks.filter(b => selectedBenchmarks.includes(b.id))

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => setIsLoading(false), 2000)
  }

  const toggleBenchmark = (benchmarkId: string) => {
    setSelectedBenchmarks(prev => 
      prev.includes(benchmarkId)
        ? prev.filter(id => id !== benchmarkId)
        : [...prev, benchmarkId]
    )
  }

  const getPercentileColor = (percentile: number) => {
    if (percentile >= 90) return 'text-green-400'
    if (percentile >= 75) return 'text-green-300'
    if (percentile >= 50) return 'text-yellow-400'
    if (percentile >= 25) return 'text-orange-400'
    return 'text-red-400'
  }

  const getQuartileLabel = (quartile: number) => {
    const labels = { 1: 'Top Quartile', 2: '2nd Quartile', 3: '3rd Quartile', 4: 'Bottom Quartile' }
    return labels[quartile as keyof typeof labels]
  }

  const getSignificanceColor = (significance: string) => {
    const colors = {
      HIGH: 'text-green-400',
      MEDIUM: 'text-yellow-400',
      LOW: 'text-orange-400',
      NONE: 'text-slate-400'
    }
    return colors[significance as keyof typeof colors]
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-green-600 to-blue-600 rounded-xl flex items-center justify-center">
              <Target className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Benchmark Comparison Suite
              </h1>
              <p className="text-slate-400">
                Advanced benchmark analysis with risk-adjusted performance metrics
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Period Selector */}
            <div className="flex items-center space-x-1">
              {['YTD', '1Y', '3Y', '5Y', 'ITD'].map((period) => (
                <button
                  key={period}
                  onClick={() => setSelectedPeriod(period as typeof selectedPeriod)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                    selectedPeriod === period
                      ? 'bg-green-600 text-white'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`}
                >
                  {period}
                </button>
              ))}
            </div>
            
            <button
              onClick={() => setShowBenchmarkSelector(!showBenchmarkSelector)}
              className="px-4 py-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors text-white text-sm"
            >
              Benchmarks ({selectedBenchmarks.length})
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

        {/* Benchmark Selector */}
        <AnimatePresence>
          {showBenchmarkSelector && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-700/50 pt-4 mb-4"
            >
              <div className="grid grid-cols-2 gap-4">
                {benchmarks.map((benchmark) => (
                  <div
                    key={benchmark.id}
                    className={`p-4 rounded-xl border cursor-pointer transition-all ${
                      selectedBenchmarks.includes(benchmark.id)
                        ? 'bg-green-600/20 border-green-600/50'
                        : 'bg-slate-800/30 border-slate-700/30 hover:border-slate-600/50'
                    }`}
                    onClick={() => toggleBenchmark(benchmark.id)}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-semibold text-white">{benchmark.name}</h4>
                      <div className="flex items-center space-x-2">
                        <span className={`px-2 py-1 bg-${
                          benchmark.type === 'INDEX' ? 'blue' : 
                          benchmark.type === 'PEER_GROUP' ? 'purple' : 'cyan'
                        }-600/20 text-${
                          benchmark.type === 'INDEX' ? 'blue' : 
                          benchmark.type === 'PEER_GROUP' ? 'purple' : 'cyan'
                        }-400 rounded text-xs`}>
                          {benchmark.type}
                        </span>
                        {selectedBenchmarks.includes(benchmark.id) && (
                          <Eye className="w-4 h-4 text-green-400" />
                        )}
                      </div>
                    </div>
                    <p className="text-sm text-slate-400 mb-2">{benchmark.description}</p>
                    <div className="flex items-center space-x-4 text-xs text-slate-500">
                      <span>{benchmark.region}</span>
                      <span>{benchmark.provider}</span>
                      <span>Since {benchmark.inception.getFullYear()}</span>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Performance Summary */}
        <div className="grid grid-cols-5 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {currentPerformance.portfolio.return.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Portfolio Return</p>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {currentPerformance.benchmark.return.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Benchmark Return</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              +{currentPerformance.relative.excessReturn.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Excess Return</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {currentPerformance.portfolio.informationRatio.toFixed(2)}
            </div>
            <p className="text-sm text-slate-400">Information Ratio</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-400 mb-1">
              {currentPerformance.relative.outperformanceProbability.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Outperformance Prob.</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'overview', label: 'Performance Overview', icon: BarChart3 },
              { id: 'risk-adjusted', label: 'Risk-Adjusted Metrics', icon: Shield },
              { id: 'style', label: 'Style Analysis', icon: Layers },
              { id: 'peer', label: 'Peer Comparison', icon: Users },
              { id: 'rolling', label: 'Rolling Analysis', icon: Activity }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-green-600 to-blue-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        <div className="mt-6">
          <AnimatePresence mode="wait">
            {/* Performance Overview Tab */}
            {activeTab === 'overview' && (
              <motion.div
                key="overview"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* Return Comparison */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <TrendingUp className="w-5 h-5 text-green-400" />
                    <span>Return Analysis</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Portfolio Return</span>
                      <span className="font-bold text-green-400">
                        {currentPerformance.portfolio.return.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Benchmark Return</span>
                      <span className="font-bold text-blue-400">
                        {currentPerformance.benchmark.return.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Excess Return</span>
                      <span className="font-bold text-green-400">
                        +{currentPerformance.relative.excessReturn.toFixed(2)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Win Rate</span>
                      <span className="font-bold text-white">
                        {currentPerformance.relative.winRate.toFixed(1)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Win/Loss Ratio</span>
                      <span className="font-bold text-purple-400">
                        {currentPerformance.relative.winLossRatio.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Risk Comparison */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Shield className="w-5 h-5 text-yellow-400" />
                    <span>Risk Analysis</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Portfolio Volatility</span>
                      <span className="font-bold text-white">
                        {currentPerformance.portfolio.volatility.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Benchmark Volatility</span>
                      <span className="font-bold text-slate-400">
                        {currentPerformance.benchmark.volatility.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Tracking Error</span>
                      <span className="font-bold text-yellow-400">
                        {currentPerformance.portfolio.trackingError.toFixed(2)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Beta</span>
                      <span className="font-bold text-blue-400">
                        {currentPerformance.portfolio.beta.toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Alpha</span>
                      <span className="font-bold text-green-400">
                        +{currentPerformance.portfolio.alpha.toFixed(2)}%
                      </span>
                    </div>
                  </div>
                </div>

                {/* Capture Ratios */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <ArrowUpRight className="w-5 h-5 text-green-400" />
                    <span>Market Capture Analysis</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-slate-400">Upside Capture</span>
                        <span className="font-bold text-green-400">
                          {currentPerformance.portfolio.upCapture.toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-slate-700 rounded-full h-2">
                        <div 
                          className="bg-green-400 h-2 rounded-full"
                          style={{ width: `${Math.min(currentPerformance.portfolio.upCapture, 150) / 150 * 100}%` }}
                        />
                      </div>
                      <div className="text-xs text-slate-500 mt-1">
                        {currentPerformance.portfolio.upCapture > 100 ? 'Outperforming in up markets' : 'Underperforming in up markets'}
                      </div>
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-slate-400">Downside Capture</span>
                        <span className="font-bold text-red-400">
                          {currentPerformance.portfolio.downCapture.toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full bg-slate-700 rounded-full h-2">
                        <div 
                          className="bg-red-400 h-2 rounded-full"
                          style={{ width: `${Math.min(currentPerformance.portfolio.downCapture, 150) / 150 * 100}%` }}
                        />
                      </div>
                      <div className="text-xs text-slate-500 mt-1">
                        {currentPerformance.portfolio.downCapture < 100 ? 'Less downside than benchmark' : 'More downside than benchmark'}
                      </div>
                    </div>

                    <div className="pt-3 border-t border-slate-700/50">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">Capture Ratio</span>
                        <span className="font-bold text-purple-400">
                          {(currentPerformance.portfolio.upCapture / currentPerformance.portfolio.downCapture).toFixed(2)}
                        </span>
                      </div>
                      <div className="text-xs text-slate-500 mt-1">
                        {(currentPerformance.portfolio.upCapture / currentPerformance.portfolio.downCapture) > 1.0 
                          ? 'Favorable asymmetric performance' 
                          : 'Unfavorable asymmetric performance'
                        }
                      </div>
                    </div>
                  </div>
                </div>

                {/* Sharpe Ratio Comparison */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Award className="w-5 h-5 text-purple-400" />
                    <span>Risk-Adjusted Returns</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Portfolio Sharpe</span>
                      <span className="font-bold text-green-400">
                        {currentPerformance.portfolio.sharpe.toFixed(2)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Benchmark Sharpe</span>
                      <span className="font-bold text-blue-400">
                        {currentPerformance.benchmark.sharpe.toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Information Ratio</span>
                      <span className="font-bold text-purple-400">
                        {currentPerformance.portfolio.informationRatio.toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Sortino Ratio</span>
                      <span className="font-bold text-cyan-400">
                        {currentPerformance.portfolio.sortinoRatio.toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Calmar Ratio</span>
                      <span className="font-bold text-yellow-400">
                        {currentPerformance.portfolio.calmarRatio.toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Risk-Adjusted Metrics Tab */}
            {activeTab === 'risk-adjusted' && (
              <motion.div
                key="risk-adjusted"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Shield className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Advanced Risk-Adjusted Metrics
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Comprehensive risk metrics including VaR, CVaR, Ulcer Index, and advanced drawdown analytics.
                </p>
              </motion.div>
            )}

            {/* Style Analysis Tab */}
            {activeTab === 'style' && (
              <motion.div
                key="style"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Style Factor Exposures</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Factor</th>
                          <th className="text-right p-4 text-sm text-slate-400">Portfolio</th>
                          <th className="text-right p-4 text-sm text-slate-400">Benchmark</th>
                          <th className="text-right p-4 text-sm text-slate-400">Active</th>
                          <th className="text-right p-4 text-sm text-slate-400">Contribution</th>
                          <th className="text-right p-4 text-sm text-slate-400">t-Stat</th>
                          <th className="text-center p-4 text-sm text-slate-400">Significance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {styleAnalysis.map((factor) => (
                          <tr
                            key={factor.styleFactor}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {factor.styleFactor}
                            </td>
                            <td className="p-4 text-right text-white">
                              {factor.portfolioExposure.toFixed(3)}
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {factor.benchmarkExposure.toFixed(3)}
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              factor.activeExposure >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {factor.activeExposure >= 0 ? '+' : ''}{factor.activeExposure.toFixed(3)}
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              factor.contribution >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {factor.contribution >= 0 ? '+' : ''}{factor.contribution} bps
                            </td>
                            <td className={`p-4 text-right ${
                              Math.abs(factor.tStat) >= 2.0 ? 'text-white font-bold' : 'text-slate-400'
                            }`}>
                              {factor.tStat.toFixed(1)}
                            </td>
                            <td className="p-4 text-center">
                              <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                                factor.significance === 'HIGH' ? 'bg-green-600/20 text-green-400' :
                                factor.significance === 'MEDIUM' ? 'bg-yellow-600/20 text-yellow-400' :
                                factor.significance === 'LOW' ? 'bg-orange-600/20 text-orange-400' :
                                'bg-slate-600/20 text-slate-400'
                              }`}>
                                {factor.significance}
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

            {/* Peer Comparison Tab */}
            {activeTab === 'peer' && (
              <motion.div
                key="peer"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <div className="flex items-center justify-between">
                      <h3 className="font-bold text-white">Peer Group Comparison</h3>
                      <span className="text-sm text-slate-400">
                        Universe: Large Cap Equity Funds (105 funds)
                      </span>
                    </div>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Metric</th>
                          <th className="text-right p-4 text-sm text-slate-400">Portfolio</th>
                          <th className="text-right p-4 text-sm text-slate-400">Percentile</th>
                          <th className="text-right p-4 text-sm text-slate-400">Peer Average</th>
                          <th className="text-right p-4 text-sm text-slate-400">Peer Median</th>
                          <th className="text-right p-4 text-sm text-slate-400">Ranking</th>
                          <th className="text-center p-4 text-sm text-slate-400">Quartile</th>
                        </tr>
                      </thead>
                      <tbody>
                        {peerData.map((peer) => (
                          <tr
                            key={peer.metric}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {peer.metric}
                            </td>
                            <td className="p-4 text-right font-bold text-green-400">
                              {peer.metric.includes('Return') || peer.metric.includes('Ratio') 
                                ? peer.portfolioValue.toFixed(2) 
                                : peer.portfolioValue.toFixed(1)
                              }
                              {peer.metric.includes('Return') || peer.metric.includes('Drawdown') ? '%' : ''}
                            </td>
                            <td className={`p-4 text-right font-bold ${getPercentileColor(peer.peerPercentile)}`}>
                              {peer.peerPercentile}th
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {peer.metric.includes('Return') || peer.metric.includes('Ratio') 
                                ? peer.peerAverage.toFixed(2) 
                                : peer.peerAverage.toFixed(1)
                              }
                              {peer.metric.includes('Return') || peer.metric.includes('Drawdown') ? '%' : ''}
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {peer.metric.includes('Return') || peer.metric.includes('Ratio') 
                                ? peer.peerMedian.toFixed(2) 
                                : peer.peerMedian.toFixed(1)
                              }
                              {peer.metric.includes('Return') || peer.metric.includes('Drawdown') ? '%' : ''}
                            </td>
                            <td className="p-4 text-right text-white">
                              #{peer.ranking} / {peer.totalPeers}
                            </td>
                            <td className="p-4 text-center">
                              <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                                peer.quartile === 1 ? 'bg-green-600/20 text-green-400' :
                                peer.quartile === 2 ? 'bg-blue-600/20 text-blue-400' :
                                peer.quartile === 3 ? 'bg-yellow-600/20 text-yellow-400' :
                                'bg-red-600/20 text-red-400'
                              }`}>
                                Q{peer.quartile}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Peer Distribution Charts */}
                <div className="grid grid-cols-2 gap-6">
                  <div className="bg-slate-800/30 rounded-xl p-6">
                    <h3 className="font-bold text-white mb-4">Return Distribution</h3>
                    <div className="h-48 flex items-center justify-center">
                      <div className="text-center">
                        <PieChart className="w-12 h-12 text-slate-400 mx-auto mb-2" />
                        <p className="text-sm text-slate-400">Distribution chart placeholder</p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-slate-800/30 rounded-xl p-6">
                    <h3 className="font-bold text-white mb-4">Risk-Return Scatter</h3>
                    <div className="h-48 flex items-center justify-center">
                      <div className="text-center">
                        <BarChart3 className="w-12 h-12 text-slate-400 mx-auto mb-2" />
                        <p className="text-sm text-slate-400">Scatter plot placeholder</p>
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Rolling Analysis Tab */}
            {activeTab === 'rolling' && (
              <motion.div
                key="rolling"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Activity className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Rolling Performance Analysis
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Interactive rolling performance charts with customizable time windows and correlation analysis.
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  )
}

export default BenchmarkComparisonSuite