import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  TrendingUp, TrendingDown, Target, BarChart3, PieChart, Activity,
  Brain, Zap, Award, AlertCircle, ChevronUp, ChevronDown,
  Calendar, Settings, Download, RefreshCw, Eye, EyeOff,
  Layers, Filter, ArrowUpRight, ArrowDownRight, Star,
  DollarSign, Percent, Clock, Users, Briefcase, Globe
} from 'lucide-react'

// Types for Performance Attribution
interface AttributionFactor {
  id: string
  name: string
  category: 'ASSET_ALLOCATION' | 'SECURITY_SELECTION' | 'INTERACTION' | 'TIMING' | 'CURRENCY'
  contribution: number // basis points
  weight: number // percentage
  active: boolean
  explanation: string
  subFactors?: AttributionFactor[]
}

interface PerformanceBreakdown {
  totalReturn: number
  benchmarkReturn: number
  excessReturn: number
  activeReturn: number
  attribution: {
    assetAllocation: number
    securitySelection: number
    interaction: number
    timing: number
    currency: number
    other: number
  }
  riskMetrics: {
    trackingError: number
    informationRatio: number
    sharpeRatio: number
    beta: number
    alpha: number
    maxDrawdown: number
  }
  period: {
    start: Date
    end: Date
    label: string
  }
}

interface SectorAttribution {
  sector: string
  portfolioWeight: number
  benchmarkWeight: number
  overUnder: number
  portfolioReturn: number
  benchmarkReturn: number
  allocationEffect: number
  selectionEffect: number
  interactionEffect: number
  totalEffect: number
}

interface SecurityAttribution {
  symbol: string
  name: string
  sector: string
  portfolioWeight: number
  benchmarkWeight: number
  overUnder: number
  securityReturn: number
  contribution: number
  attribution: number
  rank: number
}

interface TimeSeriesAttribution {
  date: Date
  portfolioReturn: number
  benchmarkReturn: number
  excessReturn: number
  cumulativeExcess: number
  attribution: {
    allocation: number
    selection: number
    interaction: number
    timing: number
  }
}

// Mock data
const mockPerformanceBreakdown: PerformanceBreakdown = {
  totalReturn: 18.45,
  benchmarkReturn: 12.80,
  excessReturn: 5.65,
  activeReturn: 5.65,
  attribution: {
    assetAllocation: 120, // basis points
    securitySelection: 245,
    interaction: 35,
    timing: 85,
    currency: -15,
    other: 30
  },
  riskMetrics: {
    trackingError: 8.2,
    informationRatio: 0.69,
    sharpeRatio: 1.42,
    beta: 0.95,
    alpha: 4.2,
    maxDrawdown: -12.5
  },
  period: {
    start: new Date('2024-01-01'),
    end: new Date('2024-12-31'),
    label: 'YTD 2024'
  }
}

const mockSectorAttribution: SectorAttribution[] = [
  {
    sector: 'Technology',
    portfolioWeight: 28.5,
    benchmarkWeight: 22.1,
    overUnder: 6.4,
    portfolioReturn: 24.8,
    benchmarkReturn: 18.2,
    allocationEffect: 142,
    selectionEffect: 188,
    interactionEffect: 42,
    totalEffect: 372
  },
  {
    sector: 'Banking',
    portfolioWeight: 18.2,
    benchmarkWeight: 25.4,
    overUnder: -7.2,
    portfolioReturn: 15.3,
    benchmarkReturn: 12.1,
    allocationEffect: -87,
    selectionEffect: 58,
    interactionEffect: -23,
    totalEffect: -52
  },
  {
    sector: 'Healthcare',
    portfolioWeight: 12.8,
    benchmarkWeight: 8.9,
    overUnder: 3.9,
    portfolioReturn: 21.4,
    benchmarkReturn: 16.7,
    allocationEffect: 65,
    selectionEffect: 60,
    interactionEffect: 18,
    totalEffect: 143
  },
  {
    sector: 'Consumer Goods',
    portfolioWeight: 15.3,
    benchmarkWeight: 18.7,
    overUnder: -3.4,
    portfolioReturn: 8.9,
    benchmarkReturn: 11.2,
    allocationEffect: 38,
    selectionEffect: -35,
    interactionEffect: 8,
    totalEffect: 11
  },
  {
    sector: 'Energy',
    portfolioWeight: 8.7,
    benchmarkWeight: 12.3,
    overUnder: -3.6,
    portfolioReturn: 32.1,
    benchmarkReturn: 28.4,
    allocationEffect: -102,
    selectionEffect: 32,
    interactionEffect: -13,
    totalEffect: -83
  }
]

const mockSecurityAttribution: SecurityAttribution[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries',
    sector: 'Energy',
    portfolioWeight: 4.8,
    benchmarkWeight: 6.2,
    overUnder: -1.4,
    securityReturn: 28.5,
    contribution: 137,
    attribution: 89,
    rank: 1
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    sector: 'Technology',
    portfolioWeight: 5.2,
    benchmarkWeight: 4.1,
    overUnder: 1.1,
    securityReturn: 22.3,
    contribution: 116,
    attribution: 65,
    rank: 2
  },
  {
    symbol: 'INFY',
    name: 'Infosys Limited',
    sector: 'Technology',
    portfolioWeight: 3.8,
    benchmarkWeight: 2.9,
    overUnder: 0.9,
    securityReturn: 26.8,
    contribution: 102,
    attribution: 58,
    rank: 3
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank',
    sector: 'Banking',
    portfolioWeight: 3.2,
    benchmarkWeight: 4.8,
    overUnder: -1.6,
    securityReturn: 11.4,
    contribution: 36,
    attribution: -38,
    rank: 25
  }
]

const mockTimeSeriesAttribution: TimeSeriesAttribution[] = [
  {
    date: new Date('2024-01-31'),
    portfolioReturn: 2.1,
    benchmarkReturn: 1.8,
    excessReturn: 0.3,
    cumulativeExcess: 0.3,
    attribution: { allocation: 8, selection: 15, interaction: 5, timing: 2 }
  },
  {
    date: new Date('2024-02-29'),
    portfolioReturn: 1.8,
    benchmarkReturn: 2.2,
    excessReturn: -0.4,
    cumulativeExcess: -0.1,
    attribution: { allocation: -12, selection: -18, interaction: -2, timing: 8 }
  },
  {
    date: new Date('2024-03-31'),
    portfolioReturn: 3.2,
    benchmarkReturn: 2.1,
    excessReturn: 1.1,
    cumulativeExcess: 1.0,
    attribution: { allocation: 25, selection: 32, interaction: 8, timing: 15 }
  },
  {
    date: new Date('2024-04-30'),
    portfolioReturn: 1.5,
    benchmarkReturn: 1.9,
    excessReturn: -0.4,
    cumulativeExcess: 0.6,
    attribution: { allocation: -8, selection: -15, interaction: -5, timing: 4 }
  },
  {
    date: new Date('2024-05-31'),
    portfolioReturn: 2.8,
    benchmarkReturn: 1.2,
    excessReturn: 1.6,
    cumulativeExcess: 2.2,
    attribution: { allocation: 35, selection: 42, interaction: 12, timing: 18 }
  },
  {
    date: new Date('2024-06-30'),
    portfolioReturn: 1.9,
    benchmarkReturn: 2.5,
    excessReturn: -0.6,
    cumulativeExcess: 1.6,
    attribution: { allocation: -18, selection: -22, interaction: -8, timing: 2 }
  }
]

interface PerformanceAttributionEngineProps {
  className?: string
}

export const PerformanceAttributionEngine: React.FC<PerformanceAttributionEngineProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'overview' | 'sectors' | 'securities' | 'timeseries' | 'factors'>('overview')
  const [selectedPeriod, setSelectedPeriod] = useState<'1M' | '3M' | '6M' | 'YTD' | '1Y'>('YTD')
  const [showDetails, setShowDetails] = useState(false)
  const [selectedFactor, setSelectedFactor] = useState<string | null>(null)
  
  // Data state
  const [performanceData] = useState<PerformanceBreakdown>(mockPerformanceBreakdown)
  const [sectorData] = useState<SectorAttribution[]>(mockSectorAttribution)
  const [securityData] = useState<SecurityAttribution[]>(mockSecurityAttribution)
  const [timeSeriesData] = useState<TimeSeriesAttribution[]>(mockTimeSeriesAttribution)
  
  // UI state
  const [isLoading, setIsLoading] = useState(false)
  const [sortBy, setSortBy] = useState<'contribution' | 'weight' | 'return'>('contribution')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')

  // Performance attribution factors
  const attributionFactors: AttributionFactor[] = [
    {
      id: 'asset_allocation',
      name: 'Asset Allocation',
      category: 'ASSET_ALLOCATION',
      contribution: performanceData.attribution.assetAllocation,
      weight: 35.2,
      active: true,
      explanation: 'Effect of over/under weighting asset classes relative to benchmark'
    },
    {
      id: 'security_selection',
      name: 'Security Selection',
      category: 'SECURITY_SELECTION',
      contribution: performanceData.attribution.securitySelection,
      weight: 42.8,
      active: true,
      explanation: 'Effect of selecting outperforming securities within each asset class'
    },
    {
      id: 'interaction',
      name: 'Allocation-Selection Interaction',
      category: 'INTERACTION',
      contribution: performanceData.attribution.interaction,
      weight: 8.5,
      active: true,
      explanation: 'Cross-effect between allocation and selection decisions'
    },
    {
      id: 'timing',
      name: 'Market Timing',
      category: 'TIMING',
      contribution: performanceData.attribution.timing,
      weight: 12.1,
      active: true,
      explanation: 'Effect of changing allocations over time based on market conditions'
    },
    {
      id: 'currency',
      name: 'Currency Effect',
      category: 'CURRENCY',
      contribution: performanceData.attribution.currency,
      weight: 1.4,
      active: false,
      explanation: 'Impact of currency movements on foreign holdings'
    }
  ]

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => setIsLoading(false), 2000)
  }

  const getContributionColor = (value: number) => {
    if (value >= 100) return 'text-green-400'
    if (value >= 50) return 'text-green-300'
    if (value >= -50) return 'text-slate-400'
    if (value >= -100) return 'text-red-300'
    return 'text-red-400'
  }

  const getContributionBg = (value: number) => {
    if (value >= 100) return 'bg-green-600/20 border-green-600/30'
    if (value >= 50) return 'bg-green-600/10 border-green-600/20'
    if (value >= -50) return 'bg-slate-600/20 border-slate-600/30'
    if (value >= -100) return 'bg-red-600/10 border-red-600/20'
    return 'bg-red-600/20 border-red-600/30'
  }

  // Sort sector data
  const sortedSectorData = [...sectorData].sort((a, b) => {
    const multiplier = sortOrder === 'asc' ? 1 : -1
    switch (sortBy) {
      case 'contribution': return (b.totalEffect - a.totalEffect) * multiplier
      case 'weight': return (b.portfolioWeight - a.portfolioWeight) * multiplier
      case 'return': return (b.portfolioReturn - a.portfolioReturn) * multiplier
      default: return 0
    }
  })

  // Sort security data
  const sortedSecurityData = [...securityData].sort((a, b) => {
    const multiplier = sortOrder === 'asc' ? 1 : -1
    switch (sortBy) {
      case 'contribution': return (b.attribution - a.attribution) * multiplier
      case 'weight': return (b.portfolioWeight - a.portfolioWeight) * multiplier
      case 'return': return (b.securityReturn - a.securityReturn) * multiplier
      default: return 0
    }
  })

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
              <BarChart3 className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Performance Attribution Engine
              </h1>
              <p className="text-slate-400">
                Multi-factor performance analysis with attribution breakdown
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Period Selector */}
            <div className="flex items-center space-x-1">
              {['1M', '3M', '6M', 'YTD', '1Y'].map((period) => (
                <button
                  key={period}
                  onClick={() => setSelectedPeriod(period as typeof selectedPeriod)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                    selectedPeriod === period
                      ? 'bg-blue-600 text-white'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`}
                >
                  {period}
                </button>
              ))}
            </div>
            
            <button
              onClick={handleRefresh}
              disabled={isLoading}
              className="p-2 bg-slate-800/50 hover:bg-slate-700/50 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-5 h-5 text-white ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Performance Summary */}
        <div className="grid grid-cols-6 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {performanceData.totalReturn.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Total Return</p>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {performanceData.benchmarkReturn.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Benchmark</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              +{performanceData.excessReturn.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Excess Return</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {performanceData.riskMetrics.informationRatio.toFixed(2)}
            </div>
            <p className="text-sm text-slate-400">Info Ratio</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-400 mb-1">
              {performanceData.riskMetrics.trackingError.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Tracking Error</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-cyan-400 mb-1">
              {performanceData.riskMetrics.sharpeRatio.toFixed(2)}
            </div>
            <p className="text-sm text-slate-400">Sharpe Ratio</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'overview', label: 'Attribution Overview', icon: PieChart },
              { id: 'sectors', label: 'Sector Analysis', icon: Layers },
              { id: 'securities', label: 'Security Analysis', icon: Target },
              { id: 'timeseries', label: 'Time Series', icon: Activity },
              { id: 'factors', label: 'Factor Analysis', icon: Brain }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white'
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
            {/* Attribution Overview Tab */}
            {activeTab === 'overview' && (
              <motion.div
                key="overview"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* Attribution Waterfall */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <BarChart3 className="w-5 h-5 text-blue-400" />
                    <span>Attribution Waterfall</span>
                  </h3>
                  
                  <div className="space-y-4">
                    {attributionFactors.map((factor) => (
                      <div
                        key={factor.id}
                        className={`p-4 rounded-lg border transition-all cursor-pointer ${getContributionBg(factor.contribution)}`}
                        onClick={() => setSelectedFactor(selectedFactor === factor.id ? null : factor.id)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center space-x-2">
                            <span className="font-medium text-white">{factor.name}</span>
                            {factor.active ? (
                              <Eye className="w-4 h-4 text-green-400" />
                            ) : (
                              <EyeOff className="w-4 h-4 text-slate-400" />
                            )}
                          </div>
                          
                          <div className="text-right">
                            <div className={`font-bold ${getContributionColor(factor.contribution)}`}>
                              {factor.contribution >= 0 ? '+' : ''}{factor.contribution} bps
                            </div>
                            <div className="text-xs text-slate-400">
                              {factor.weight.toFixed(1)}% weight
                            </div>
                          </div>
                        </div>
                        
                        <AnimatePresence>
                          {selectedFactor === factor.id && (
                            <motion.div
                              initial={{ opacity: 0, height: 0 }}
                              animate={{ opacity: 1, height: 'auto' }}
                              exit={{ opacity: 0, height: 0 }}
                              className="border-t border-slate-700/50 pt-2 mt-2"
                            >
                              <p className="text-sm text-slate-300">{factor.explanation}</p>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Risk Metrics */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Activity className="w-5 h-5 text-purple-400" />
                    <span>Risk-Adjusted Performance</span>
                  </h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Alpha</span>
                      <span className="font-semibold text-green-400">
                        +{performanceData.riskMetrics.alpha.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Beta</span>
                      <span className="font-semibold text-white">
                        {performanceData.riskMetrics.beta.toFixed(2)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Information Ratio</span>
                      <span className="font-semibold text-blue-400">
                        {performanceData.riskMetrics.informationRatio.toFixed(2)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Tracking Error</span>
                      <span className="font-semibold text-yellow-400">
                        {performanceData.riskMetrics.trackingError.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Max Drawdown</span>
                      <span className="font-semibold text-red-400">
                        {performanceData.riskMetrics.maxDrawdown.toFixed(2)}%
                      </span>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Sector Analysis Tab */}
            {activeTab === 'sectors' && (
              <motion.div
                key="sectors"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {/* Controls */}
                <div className="flex items-center justify-between bg-slate-800/30 rounded-xl p-4">
                  <div className="flex items-center space-x-4">
                    <span className="text-sm text-slate-400">Sort by:</span>
                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
                      className="bg-slate-700 text-white rounded-lg px-3 py-1 text-sm border border-slate-600"
                    >
                      <option value="contribution">Total Effect</option>
                      <option value="weight">Portfolio Weight</option>
                      <option value="return">Return</option>
                    </select>
                    
                    <button
                      onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                      className="p-1 text-slate-400 hover:text-white transition-colors"
                    >
                      {sortOrder === 'asc' ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Sector Attribution Table */}
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Sector Attribution Analysis</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Sector</th>
                          <th className="text-right p-4 text-sm text-slate-400">Portfolio %</th>
                          <th className="text-right p-4 text-sm text-slate-400">Benchmark %</th>
                          <th className="text-right p-4 text-sm text-slate-400">Over/Under</th>
                          <th className="text-right p-4 text-sm text-slate-400">Return</th>
                          <th className="text-right p-4 text-sm text-slate-400">Allocation</th>
                          <th className="text-right p-4 text-sm text-slate-400">Selection</th>
                          <th className="text-right p-4 text-sm text-slate-400">Interaction</th>
                          <th className="text-right p-4 text-sm text-slate-400">Total Effect</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sortedSectorData.map((sector) => (
                          <tr
                            key={sector.sector}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4">
                              <div className="font-medium text-white">{sector.sector}</div>
                            </td>
                            <td className="p-4 text-right text-white">
                              {sector.portfolioWeight.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {sector.benchmarkWeight.toFixed(1)}%
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              sector.overUnder >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {sector.overUnder >= 0 ? '+' : ''}{sector.overUnder.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-white">
                              {sector.portfolioReturn.toFixed(1)}%
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              sector.allocationEffect >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {sector.allocationEffect >= 0 ? '+' : ''}{sector.allocationEffect}
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              sector.selectionEffect >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {sector.selectionEffect >= 0 ? '+' : ''}{sector.selectionEffect}
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              sector.interactionEffect >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {sector.interactionEffect >= 0 ? '+' : ''}{sector.interactionEffect}
                            </td>
                            <td className={`p-4 text-right font-bold ${
                              sector.totalEffect >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {sector.totalEffect >= 0 ? '+' : ''}{sector.totalEffect} bps
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Security Analysis Tab */}
            {activeTab === 'securities' && (
              <motion.div
                key="securities"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {/* Top Contributors/Detractors */}
                <div className="grid grid-cols-2 gap-6">
                  <div className="bg-slate-800/30 rounded-xl p-6">
                    <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                      <TrendingUp className="w-5 h-5 text-green-400" />
                      <span>Top Contributors</span>
                    </h3>
                    
                    <div className="space-y-3">
                      {sortedSecurityData.filter(s => s.attribution > 0).slice(0, 5).map((security) => (
                        <div key={security.symbol} className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                          <div>
                            <div className="font-medium text-white">{security.symbol}</div>
                            <div className="text-xs text-slate-400">{security.sector}</div>
                          </div>
                          <div className="text-right">
                            <div className="font-bold text-green-400">
                              +{security.attribution} bps
                            </div>
                            <div className="text-xs text-slate-400">
                              {security.portfolioWeight.toFixed(1)}%
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="bg-slate-800/30 rounded-xl p-6">
                    <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                      <TrendingDown className="w-5 h-5 text-red-400" />
                      <span>Top Detractors</span>
                    </h3>
                    
                    <div className="space-y-3">
                      {sortedSecurityData.filter(s => s.attribution < 0).slice(0, 5).map((security) => (
                        <div key={security.symbol} className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                          <div>
                            <div className="font-medium text-white">{security.symbol}</div>
                            <div className="text-xs text-slate-400">{security.sector}</div>
                          </div>
                          <div className="text-right">
                            <div className="font-bold text-red-400">
                              {security.attribution} bps
                            </div>
                            <div className="text-xs text-slate-400">
                              {security.portfolioWeight.toFixed(1)}%
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Security Attribution Table */}
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Security Attribution Breakdown</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Rank</th>
                          <th className="text-left p-4 text-sm text-slate-400">Security</th>
                          <th className="text-left p-4 text-sm text-slate-400">Sector</th>
                          <th className="text-right p-4 text-sm text-slate-400">Weight</th>
                          <th className="text-right p-4 text-sm text-slate-400">Return</th>
                          <th className="text-right p-4 text-sm text-slate-400">Contribution</th>
                          <th className="text-right p-4 text-sm text-slate-400">Attribution</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sortedSecurityData.map((security) => (
                          <tr
                            key={security.symbol}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4">
                              <div className="text-white">#{security.rank}</div>
                            </td>
                            <td className="p-4">
                              <div>
                                <div className="font-medium text-white">{security.symbol}</div>
                                <div className="text-xs text-slate-400">{security.name}</div>
                              </div>
                            </td>
                            <td className="p-4 text-slate-400">
                              {security.sector}
                            </td>
                            <td className="p-4 text-right text-white">
                              {security.portfolioWeight.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-white">
                              {security.securityReturn.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-white">
                              {security.contribution} bps
                            </td>
                            <td className={`p-4 text-right font-bold ${
                              security.attribution >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {security.attribution >= 0 ? '+' : ''}{security.attribution} bps
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Time Series Tab */}
            {activeTab === 'timeseries' && (
              <motion.div
                key="timeseries"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-4"
              >
                {/* Time Series Chart Placeholder */}
                <div className="bg-slate-800/30 rounded-xl p-6 h-96 flex items-center justify-center">
                  <div className="text-center">
                    <Activity className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                    <h3 className="text-xl font-semibold text-white mb-2">
                      Time Series Attribution Analysis
                    </h3>
                    <p className="text-slate-400">
                      Interactive charts showing attribution evolution over time
                    </p>
                  </div>
                </div>

                {/* Monthly Attribution Data */}
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Monthly Attribution Breakdown</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Month</th>
                          <th className="text-right p-4 text-sm text-slate-400">Portfolio</th>
                          <th className="text-right p-4 text-sm text-slate-400">Benchmark</th>
                          <th className="text-right p-4 text-sm text-slate-400">Excess</th>
                          <th className="text-right p-4 text-sm text-slate-400">Cumulative</th>
                          <th className="text-right p-4 text-sm text-slate-400">Allocation</th>
                          <th className="text-right p-4 text-sm text-slate-400">Selection</th>
                          <th className="text-right p-4 text-sm text-slate-400">Timing</th>
                        </tr>
                      </thead>
                      <tbody>
                        {timeSeriesData.map((data) => (
                          <tr
                            key={data.date.toISOString()}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 text-white">
                              {data.date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}
                            </td>
                            <td className="p-4 text-right text-white">
                              {data.portfolioReturn.toFixed(2)}%
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {data.benchmarkReturn.toFixed(2)}%
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              data.excessReturn >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {data.excessReturn >= 0 ? '+' : ''}{data.excessReturn.toFixed(2)}%
                            </td>
                            <td className={`p-4 text-right font-medium ${
                              data.cumulativeExcess >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {data.cumulativeExcess >= 0 ? '+' : ''}{data.cumulativeExcess.toFixed(2)}%
                            </td>
                            <td className={`p-4 text-right ${
                              data.attribution.allocation >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {data.attribution.allocation >= 0 ? '+' : ''}{data.attribution.allocation}
                            </td>
                            <td className={`p-4 text-right ${
                              data.attribution.selection >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {data.attribution.selection >= 0 ? '+' : ''}{data.attribution.selection}
                            </td>
                            <td className={`p-4 text-right ${
                              data.attribution.timing >= 0 ? 'text-green-400' : 'text-red-400'
                            }`}>
                              {data.attribution.timing >= 0 ? '+' : ''}{data.attribution.timing}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Factor Analysis Tab */}
            {activeTab === 'factors' && (
              <motion.div
                key="factors"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center py-12"
              >
                <Brain className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-white mb-2">
                  Multi-Factor Analysis
                </h3>
                <p className="text-slate-400 max-w-md mx-auto">
                  Advanced factor attribution analysis including style factors, risk models, and custom factor exposures.
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  )
}

export default PerformanceAttributionEngine