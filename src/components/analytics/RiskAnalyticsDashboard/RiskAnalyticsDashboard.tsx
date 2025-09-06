import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Shield, AlertTriangle, TrendingDown, BarChart3, PieChart, Activity,
  Target, Zap, Clock, Calendar, Settings, Download, RefreshCw,
  Filter, Search, ChevronUp, ChevronDown, Eye, EyeOff, Star,
  DollarSign, Percent, Gauge, Award, LineChart, Layers, Users,
  Building, Globe, Briefcase, ArrowUpRight, ArrowDownRight, Brain,
  AlertCircle, CheckCircle, XCircle, Timer, TrendingUp, Volume2
} from 'lucide-react'

// Types for Risk Analytics
interface RiskMetric {
  id: string
  name: string
  value: number
  limit?: number
  unit: string
  status: 'NORMAL' | 'WARNING' | 'DANGER' | 'CRITICAL'
  trend: 'UP' | 'DOWN' | 'STABLE'
  change: number
  description: string
  lastUpdated: Date
}

interface VaRAnalysis {
  confidence: number
  timeHorizon: string
  portfolioVaR: number
  componentVaR: {
    symbol: string
    name: string
    value: number
    contribution: number
    marginalVaR: number
    componentPercent: number
  }[]
  historicalVaR: number
  parametricVaR: number
  monteCarloVaR: number
  expectedShortfall: number
  worstCase: number
  bestCase: number
}

interface StressTesting {
  scenario: string
  category: 'MARKET' | 'CREDIT' | 'OPERATIONAL' | 'LIQUIDITY' | 'REGULATORY'
  probability: number
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME'
  portfolioImpact: number
  maxDrawdown: number
  recoveryTime: number // in days
  hedgingCost: number
  description: string
  keyDrivers: string[]
  mitigationStrategies: string[]
}

interface ConcentrationRisk {
  type: 'SINGLE_STOCK' | 'SECTOR' | 'GEOGRAPHY' | 'CURRENCY' | 'MARKET_CAP'
  name: string
  currentWeight: number
  riskLimit: number
  utilizationPercent: number
  riskContribution: number
  diversificationRatio: number
  herfindahlIndex?: number
  status: 'COMPLIANT' | 'NEAR_LIMIT' | 'BREACH'
  recommendation: string
}

interface CorrelationAnalysis {
  period: string
  averageCorrelation: number
  maxCorrelation: number
  minCorrelation: number
  correlationMatrix: {
    asset1: string
    asset2: string
    correlation: number
    significance: number
    isSignificant: boolean
  }[]
  clusterAnalysis: {
    cluster: string
    assets: string[]
    intraClusterCorr: number
    riskContribution: number
  }[]
  conditionalCorrelations: {
    condition: string
    avgCorrelation: number
    increase: number
  }[]
}

interface LiquidityRisk {
  asset: string
  sector: string
  liquidityScore: number
  avgDailyVolume: number
  marketCapRatio: number
  bidAskSpread: number
  liquidityRank: number
  liquidationTime: number // days to liquidate position
  liquidationCost: number // basis points
  status: 'HIGHLY_LIQUID' | 'LIQUID' | 'MODERATE' | 'ILLIQUID'
  recommendation: string
}

interface RiskAttribution {
  factor: string
  category: 'SYSTEMATIC' | 'IDIOSYNCRATIC' | 'SECTOR' | 'STYLE' | 'MACRO'
  riskContribution: number
  riskPercent: number
  activeRisk: number
  exposureSize: number
  volatility: number
  correlationToPortfolio: number
  marginalRisk: number
  explanation: string
}

interface DrawdownAnalysis {
  period: string
  currentDrawdown: number
  maxDrawdown: number
  averageDrawdown: number
  drawdownDuration: number
  recoveryTime: number
  drawdownFrequency: number
  calmarRatio: number
  sterlingRatio: number
  burkeRatio: number
  ulcerIndex: number
  painIndex: number
  drawdownSeries: {
    date: Date
    drawdown: number
    isActive: boolean
  }[]
}

// Mock data
const mockRiskMetrics: RiskMetric[] = [
  {
    id: '1',
    name: 'Portfolio VaR (95%)',
    value: -2.45,
    limit: -3.00,
    unit: '%',
    status: 'NORMAL',
    trend: 'DOWN',
    change: -0.15,
    description: '95% confidence level Value at Risk over 1-day horizon',
    lastUpdated: new Date()
  },
  {
    id: '2',
    name: 'Expected Shortfall',
    value: -3.78,
    limit: -4.50,
    unit: '%',
    status: 'WARNING',
    trend: 'UP',
    change: 0.23,
    description: 'Conditional VaR - expected loss beyond VaR threshold',
    lastUpdated: new Date()
  },
  {
    id: '3',
    name: 'Portfolio Beta',
    value: 0.95,
    limit: 1.20,
    unit: '',
    status: 'NORMAL',
    trend: 'STABLE',
    change: -0.02,
    description: 'Systematic risk relative to market benchmark',
    lastUpdated: new Date()
  },
  {
    id: '4',
    name: 'Tracking Error',
    value: 4.85,
    limit: 8.00,
    unit: '%',
    status: 'NORMAL',
    trend: 'DOWN',
    change: -0.35,
    description: 'Standard deviation of excess returns vs benchmark',
    lastUpdated: new Date()
  },
  {
    id: '5',
    name: 'Maximum Single Stock',
    value: 8.5,
    limit: 10.0,
    unit: '%',
    status: 'WARNING',
    trend: 'UP',
    change: 0.8,
    description: 'Largest individual stock concentration',
    lastUpdated: new Date()
  },
  {
    id: '6',
    name: 'Sector Concentration',
    value: 28.7,
    limit: 35.0,
    unit: '%',
    status: 'NORMAL',
    trend: 'UP',
    change: 1.2,
    description: 'Maximum sector exposure concentration',
    lastUpdated: new Date()
  }
]

const mockVaRAnalysis: VaRAnalysis = {
  confidence: 95,
  timeHorizon: '1 Day',
  portfolioVaR: -2.45,
  componentVaR: [
    {
      symbol: 'RELIANCE',
      name: 'Reliance Industries',
      value: -0.68,
      contribution: 27.8,
      marginalVaR: -0.089,
      componentPercent: 8.5
    },
    {
      symbol: 'TCS',
      name: 'Tata Consultancy Services',
      value: -0.42,
      contribution: 17.1,
      marginalVaR: -0.067,
      componentPercent: 6.2
    },
    {
      symbol: 'INFY',
      name: 'Infosys Limited',
      value: -0.35,
      contribution: 14.3,
      marginalVaR: -0.058,
      componentPercent: 5.8
    },
    {
      symbol: 'HDFCBANK',
      name: 'HDFC Bank',
      value: -0.31,
      contribution: 12.7,
      marginalVaR: -0.052,
      componentPercent: 5.4
    }
  ],
  historicalVaR: -2.38,
  parametricVaR: -2.45,
  monteCarloVaR: -2.52,
  expectedShortfall: -3.78,
  worstCase: -8.95,
  bestCase: 4.25
}

const mockStressTests: StressTesting[] = [
  {
    scenario: 'Market Crash (2008-style)',
    category: 'MARKET',
    probability: 2.5,
    severity: 'EXTREME',
    portfolioImpact: -35.8,
    maxDrawdown: -42.3,
    recoveryTime: 485,
    hedgingCost: 180,
    description: 'Global financial crisis scenario with severe market dislocation',
    keyDrivers: ['Credit Crisis', 'Liquidity Crunch', 'Risk-off Sentiment', 'Banking Sector Collapse'],
    mitigationStrategies: ['Increase Cash Position', 'Hedge with Put Options', 'Reduce Beta Exposure', 'Diversify Geographically']
  },
  {
    scenario: 'Geopolitical Crisis',
    category: 'MARKET',
    probability: 8.5,
    severity: 'HIGH',
    portfolioImpact: -18.5,
    maxDrawdown: -23.7,
    recoveryTime: 125,
    hedgingCost: 95,
    description: 'Major geopolitical event affecting regional markets',
    keyDrivers: ['Political Instability', 'Trade Tensions', 'Currency Volatility', 'Commodity Disruption'],
    mitigationStrategies: ['Currency Hedging', 'Reduce Commodity Exposure', 'Defensive Positioning', 'Increase Gold Allocation']
  },
  {
    scenario: 'Interest Rate Shock',
    category: 'MARKET',
    probability: 15.2,
    severity: 'MEDIUM',
    portfolioImpact: -12.8,
    maxDrawdown: -16.4,
    recoveryTime: 85,
    hedgingCost: 65,
    description: 'Sudden unexpected central bank policy change',
    keyDrivers: ['Inflation Surprise', 'Central Bank Hawkishness', 'Bond Market Volatility', 'Credit Spread Widening'],
    mitigationStrategies: ['Duration Hedging', 'Float-to-Fixed Swaps', 'Reduce Financial Sector', 'Increase REIT Exposure']
  },
  {
    scenario: 'Technology Sector Correction',
    category: 'MARKET',
    probability: 25.8,
    severity: 'MEDIUM',
    portfolioImpact: -8.9,
    maxDrawdown: -11.2,
    recoveryTime: 45,
    hedgingCost: 35,
    description: 'Major correction in technology and growth stocks',
    keyDrivers: ['Valuation Reset', 'Growth Concerns', 'Regulatory Pressure', 'Competition Intensification'],
    mitigationStrategies: ['Reduce Growth Exposure', 'Value Tilt', 'Sector Rotation', 'Quality Focus']
  }
]

const mockConcentrationRisks: ConcentrationRisk[] = [
  {
    type: 'SINGLE_STOCK',
    name: 'RELIANCE',
    currentWeight: 8.5,
    riskLimit: 10.0,
    utilizationPercent: 85.0,
    riskContribution: 12.8,
    diversificationRatio: 0.82,
    status: 'NEAR_LIMIT',
    recommendation: 'Monitor closely and consider reducing position if it approaches limit'
  },
  {
    type: 'SECTOR',
    name: 'Information Technology',
    currentWeight: 28.7,
    riskLimit: 35.0,
    utilizationPercent: 82.0,
    riskContribution: 22.4,
    diversificationRatio: 0.75,
    herfindahlIndex: 0.18,
    status: 'NEAR_LIMIT',
    recommendation: 'Consider diversifying into other sectors to maintain balance'
  },
  {
    type: 'MARKET_CAP',
    name: 'Large Cap',
    currentWeight: 72.8,
    riskLimit: 80.0,
    utilizationPercent: 91.0,
    riskContribution: 45.2,
    diversificationRatio: 0.68,
    status: 'COMPLIANT',
    recommendation: 'Well within limits, consider small/mid-cap opportunities'
  }
]

const mockCorrelationAnalysis: CorrelationAnalysis = {
  period: '60 Days',
  averageCorrelation: 0.42,
  maxCorrelation: 0.89,
  minCorrelation: -0.15,
  correlationMatrix: [
    { asset1: 'TCS', asset2: 'INFY', correlation: 0.89, significance: 3.8, isSignificant: true },
    { asset1: 'HDFCBANK', asset2: 'ICICIBANK', correlation: 0.78, significance: 3.2, isSignificant: true },
    { asset1: 'RELIANCE', asset2: 'ONGC', correlation: 0.65, significance: 2.9, isSignificant: true },
    { asset1: 'TCS', asset2: 'HDFCBANK', correlation: 0.34, significance: 1.8, isSignificant: false }
  ],
  clusterAnalysis: [
    {
      cluster: 'IT Services',
      assets: ['TCS', 'INFY', 'WIPRO', 'HCLTECH'],
      intraClusterCorr: 0.82,
      riskContribution: 28.5
    },
    {
      cluster: 'Banking',
      assets: ['HDFCBANK', 'ICICIBANK', 'KOTAKBANK', 'AXISBANK'],
      intraClusterCorr: 0.71,
      riskContribution: 22.8
    },
    {
      cluster: 'Energy',
      assets: ['RELIANCE', 'ONGC', 'BPCL', 'IOCL'],
      intraClusterCorr: 0.58,
      riskContribution: 18.2
    }
  ],
  conditionalCorrelations: [
    { condition: 'Market Stress (VIX > 30)', avgCorrelation: 0.75, increase: 0.33 },
    { condition: 'Bull Market (Returns > 15%)', avgCorrelation: 0.38, increase: -0.04 },
    { condition: 'High Volatility Period', avgCorrelation: 0.68, increase: 0.26 }
  ]
}

const mockLiquidityRisks: LiquidityRisk[] = [
  {
    asset: 'RELIANCE',
    sector: 'Energy',
    liquidityScore: 95,
    avgDailyVolume: 25000000,
    marketCapRatio: 0.08,
    bidAskSpread: 0.05,
    liquidityRank: 1,
    liquidationTime: 0.5,
    liquidationCost: 8.5,
    status: 'HIGHLY_LIQUID',
    recommendation: 'Excellent liquidity, no concerns'
  },
  {
    asset: 'TCS',
    sector: 'Information Technology',
    liquidityScore: 92,
    avgDailyVolume: 18000000,
    marketCapRatio: 0.12,
    bidAskSpread: 0.08,
    liquidityRank: 2,
    liquidationTime: 0.8,
    liquidationCost: 12.3,
    status: 'HIGHLY_LIQUID',
    recommendation: 'Very good liquidity profile'
  },
  {
    asset: 'SMALLCAP_STOCK',
    sector: 'Consumer Goods',
    liquidityScore: 68,
    avgDailyVolume: 850000,
    marketCapRatio: 0.45,
    bidAskSpread: 0.25,
    liquidityRank: 85,
    liquidationTime: 5.2,
    liquidationCost: 85.4,
    status: 'MODERATE',
    recommendation: 'Monitor position size relative to daily volume'
  }
]

const mockRiskAttribution: RiskAttribution[] = [
  {
    factor: 'Market Beta',
    category: 'SYSTEMATIC',
    riskContribution: 35.8,
    riskPercent: 62.2,
    activeRisk: 2.8,
    exposureSize: 0.95,
    volatility: 16.8,
    correlationToPortfolio: 0.92,
    marginalRisk: 1.45,
    explanation: 'Systematic market risk exposure through beta coefficient'
  },
  {
    factor: 'Technology Sector',
    category: 'SECTOR',
    riskContribution: 12.4,
    riskPercent: 21.5,
    activeRisk: 4.2,
    exposureSize: 28.7,
    volatility: 22.3,
    correlationToPortfolio: 0.78,
    marginalRisk: 0.89,
    explanation: 'Concentration risk from overweight technology sector exposure'
  },
  {
    factor: 'Quality Factor',
    category: 'STYLE',
    riskContribution: 5.8,
    riskPercent: 10.1,
    activeRisk: 1.8,
    exposureSize: 0.22,
    volatility: 12.5,
    correlationToPortfolio: 0.65,
    marginalRisk: 0.42,
    explanation: 'Style risk from quality factor exposure'
  },
  {
    factor: 'Individual Stock Selection',
    category: 'IDIOSYNCRATIC',
    riskContribution: 3.6,
    riskPercent: 6.2,
    activeRisk: 2.1,
    exposureSize: 1.0,
    volatility: 8.9,
    correlationToPortfolio: 0.28,
    marginalRisk: 0.25,
    explanation: 'Stock-specific risk from individual security selection'
  }
]

const mockDrawdownAnalysis: DrawdownAnalysis = {
  period: 'Last 3 Years',
  currentDrawdown: -3.2,
  maxDrawdown: -18.7,
  averageDrawdown: -6.8,
  drawdownDuration: 15,
  recoveryTime: 28,
  drawdownFrequency: 8.5,
  calmarRatio: 1.32,
  sterlingRatio: 1.18,
  burkeRatio: 0.95,
  ulcerIndex: 4.8,
  painIndex: 2.2,
  drawdownSeries: [
    { date: new Date('2024-01-15'), drawdown: -2.1, isActive: false },
    { date: new Date('2024-02-15'), drawdown: -5.8, isActive: false },
    { date: new Date('2024-03-15'), drawdown: -8.9, isActive: false },
    { date: new Date('2024-04-15'), drawdown: -3.2, isActive: true }
  ]
}

interface RiskAnalyticsDashboardProps {
  className?: string
}

export const RiskAnalyticsDashboard: React.FC<RiskAnalyticsDashboardProps> = ({
  className = ''
}) => {
  // State management
  const [activeTab, setActiveTab] = useState<'overview' | 'var' | 'stress' | 'concentration' | 'correlation' | 'attribution'>('overview')
  const [selectedTimeframe, setSelectedTimeframe] = useState<'1D' | '1W' | '1M' | '3M' | '1Y'>('1M')
  const [confidenceLevel, setConfidenceLevel] = useState<95 | 99>(95)
  const [showRiskLimits, setShowRiskLimits] = useState(true)
  
  // Data state
  const [riskMetrics] = useState<RiskMetric[]>(mockRiskMetrics)
  const [varAnalysis] = useState<VaRAnalysis>(mockVaRAnalysis)
  const [stressTests] = useState<StressTesting[]>(mockStressTests)
  const [concentrationRisks] = useState<ConcentrationRisk[]>(mockConcentrationRisks)
  const [correlationData] = useState<CorrelationAnalysis>(mockCorrelationAnalysis)
  const [liquidityRisks] = useState<LiquidityRisk[]>(mockLiquidityRisks)
  const [riskAttribution] = useState<RiskAttribution[]>(mockRiskAttribution)
  const [drawdownData] = useState<DrawdownAnalysis>(mockDrawdownAnalysis)
  
  // UI state
  const [isLoading, setIsLoading] = useState(false)
  const [selectedScenario, setSelectedScenario] = useState<StressTesting | null>(null)

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => setIsLoading(false), 2000)
  }

  const getStatusColor = (status: string) => {
    const colors = {
      NORMAL: 'text-green-400',
      WARNING: 'text-yellow-400',
      DANGER: 'text-orange-400',
      CRITICAL: 'text-red-400',
      COMPLIANT: 'text-green-400',
      NEAR_LIMIT: 'text-yellow-400',
      BREACH: 'text-red-400'
    }
    return colors[status as keyof typeof colors] || 'text-slate-400'
  }

  const getStatusBg = (status: string) => {
    const colors = {
      NORMAL: 'bg-green-600/20 border-green-600/30',
      WARNING: 'bg-yellow-600/20 border-yellow-600/30',
      DANGER: 'bg-orange-600/20 border-orange-600/30',
      CRITICAL: 'bg-red-600/20 border-red-600/30',
      COMPLIANT: 'bg-green-600/20 border-green-600/30',
      NEAR_LIMIT: 'bg-yellow-600/20 border-yellow-600/30',
      BREACH: 'bg-red-600/20 border-red-600/30'
    }
    return colors[status as keyof typeof colors] || 'bg-slate-600/20 border-slate-600/30'
  }

  const getTrendIcon = (trend: string) => {
    if (trend === 'UP') return <TrendingUp className="w-4 h-4 text-green-400" />
    if (trend === 'DOWN') return <TrendingDown className="w-4 h-4 text-red-400" />
    return <Activity className="w-4 h-4 text-slate-400" />
  }

  const getSeverityColor = (severity: string) => {
    const colors = {
      LOW: 'text-blue-400',
      MEDIUM: 'text-yellow-400',
      HIGH: 'text-orange-400',
      EXTREME: 'text-red-400'
    }
    return colors[severity as keyof typeof colors]
  }

  const getLiquidityColor = (status: string) => {
    const colors = {
      HIGHLY_LIQUID: 'text-green-400',
      LIQUID: 'text-blue-400',
      MODERATE: 'text-yellow-400',
      ILLIQUID: 'text-red-400'
    }
    return colors[status as keyof typeof colors]
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-gradient-to-br from-red-600 to-orange-600 rounded-xl flex items-center justify-center">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Risk Analytics Dashboard
              </h1>
              <p className="text-slate-400">
                Advanced risk measurement and monitoring with stress testing
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            {/* Timeframe Selector */}
            <div className="flex items-center space-x-1">
              {['1D', '1W', '1M', '3M', '1Y'].map((period) => (
                <button
                  key={period}
                  onClick={() => setSelectedTimeframe(period as typeof selectedTimeframe)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium transition-all ${
                    selectedTimeframe === period
                      ? 'bg-red-600 text-white'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                  }`}
                >
                  {period}
                </button>
              ))}
            </div>

            {/* Confidence Level */}
            <div className="flex items-center space-x-2">
              <span className="text-sm text-slate-400">VaR:</span>
              <select
                value={confidenceLevel}
                onChange={(e) => setConfidenceLevel(Number(e.target.value) as typeof confidenceLevel)}
                className="bg-slate-700 text-white rounded-lg px-3 py-1 text-sm border border-slate-600"
              >
                <option value={95}>95%</option>
                <option value={99}>99%</option>
              </select>
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

        {/* Risk Summary */}
        <div className="grid grid-cols-6 gap-6">
          <div className="text-center">
            <div className="text-2xl font-bold text-red-400 mb-1">
              {varAnalysis.portfolioVaR.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Portfolio VaR</p>
          </div>
          
          <div className="text-center">
            <div className="text-2xl font-bold text-orange-400 mb-1">
              {varAnalysis.expectedShortfall.toFixed(2)}%
            </div>
            <p className="text-sm text-slate-400">Expected Shortfall</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">
              {riskMetrics.find(m => m.name.includes('Beta'))?.value.toFixed(2)}
            </div>
            <p className="text-sm text-slate-400">Portfolio Beta</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-400 mb-1">
              {riskMetrics.find(m => m.name.includes('Tracking'))?.value.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Tracking Error</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">
              {drawdownData.currentDrawdown.toFixed(1)}%
            </div>
            <p className="text-sm text-slate-400">Current Drawdown</p>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-green-400 mb-1">
              {stressTests.filter(s => s.severity === 'HIGH' || s.severity === 'EXTREME').length}
            </div>
            <p className="text-sm text-slate-400">High Risk Scenarios</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1">
            {[
              { id: 'overview', label: 'Risk Overview', icon: Shield },
              { id: 'var', label: 'VaR Analysis', icon: BarChart3 },
              { id: 'stress', label: 'Stress Testing', icon: AlertTriangle },
              { id: 'concentration', label: 'Concentration Risk', icon: Target },
              { id: 'correlation', label: 'Correlation Analysis', icon: Activity },
              { id: 'attribution', label: 'Risk Attribution', icon: PieChart }
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setActiveTab(id as typeof activeTab)}
                className={`px-4 py-2 rounded-xl flex items-center space-x-2 transition-all ${
                  activeTab === id
                    ? 'bg-gradient-to-r from-red-600 to-orange-600 text-white'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowRiskLimits(!showRiskLimits)}
              className={`px-3 py-1 rounded-lg text-sm transition-colors ${
                showRiskLimits ? 'bg-red-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Risk Limits
            </button>
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
            {/* Risk Overview Tab */}
            {activeTab === 'overview' && (
              <motion.div
                key="overview"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                {/* Risk Metrics Grid */}
                <div className="grid grid-cols-2 gap-6">
                  {riskMetrics.map((metric) => (
                    <div
                      key={metric.id}
                      className={`p-6 rounded-xl border transition-all ${getStatusBg(metric.status)}`}
                    >
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <h3 className="font-bold text-white text-lg">{metric.name}</h3>
                          <p className="text-sm text-slate-400">{metric.description}</p>
                        </div>
                        <div className="text-right">
                          <div className="flex items-center space-x-2">
                            {getTrendIcon(metric.trend)}
                            <span className={`text-xs ${getStatusColor(metric.status)}`}>
                              {metric.status}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-end justify-between">
                        <div>
                          <div className="text-3xl font-bold text-white mb-1">
                            {metric.value.toFixed(2)}{metric.unit}
                          </div>
                          {metric.limit && (
                            <div className="text-sm text-slate-400">
                              Limit: {metric.limit.toFixed(2)}{metric.unit}
                            </div>
                          )}
                        </div>

                        <div className="text-right">
                          <div className={`text-sm font-medium ${
                            metric.change >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {metric.change >= 0 ? '+' : ''}{metric.change.toFixed(2)}{metric.unit}
                          </div>
                          <div className="text-xs text-slate-500">vs yesterday</div>
                        </div>
                      </div>

                      {metric.limit && showRiskLimits && (
                        <div className="mt-4">
                          <div className="flex justify-between text-xs text-slate-400 mb-1">
                            <span>0{metric.unit}</span>
                            <span>{metric.limit.toFixed(1)}{metric.unit}</span>
                          </div>
                          <div className="w-full bg-slate-700 rounded-full h-2">
                            <div 
                              className={`h-2 rounded-full ${
                                Math.abs(metric.value) / Math.abs(metric.limit || 1) > 0.8 
                                  ? 'bg-red-400' 
                                  : Math.abs(metric.value) / Math.abs(metric.limit || 1) > 0.6 
                                    ? 'bg-yellow-400' 
                                    : 'bg-green-400'
                              }`}
                              style={{ width: `${Math.min((Math.abs(metric.value) / Math.abs(metric.limit || 1)) * 100, 100)}%` }}
                            />
                          </div>
                          <div className="text-xs text-slate-500 mt-1">
                            {((Math.abs(metric.value) / Math.abs(metric.limit || 1)) * 100).toFixed(1)}% of limit used
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {/* Liquidity Risk Overview */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4 flex items-center space-x-2">
                    <Volume2 className="w-5 h-5 text-blue-400" />
                    <span>Liquidity Risk Overview</span>
                  </h3>
                  
                  <div className="grid grid-cols-3 gap-6">
                    {liquidityRisks.slice(0, 3).map((risk) => (
                      <div key={risk.asset} className="bg-slate-700/30 rounded-lg p-4">
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <div className="font-medium text-white">{risk.asset}</div>
                            <div className="text-sm text-slate-400">{risk.sector}</div>
                          </div>
                          <div className="text-right">
                            <div className="text-lg font-bold text-white">
                              {risk.liquidityScore}
                            </div>
                            <div className="text-xs text-slate-400">Score</div>
                          </div>
                        </div>

                        <div className="space-y-2 text-sm">
                          <div className="flex justify-between">
                            <span className="text-slate-400">Status:</span>
                            <span className={getLiquidityColor(risk.status)}>
                              {risk.status.replace('_', ' ')}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Liquidation Time:</span>
                            <span className="text-white">{risk.liquidationTime} days</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Liquidation Cost:</span>
                            <span className="text-yellow-400">{risk.liquidationCost.toFixed(1)} bps</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* VaR Analysis Tab */}
            {activeTab === 'var' && (
              <motion.div
                key="var"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* VaR Methods Comparison */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">VaR Methodology Comparison</h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Historical VaR</span>
                      <span className="font-bold text-white">
                        {varAnalysis.historicalVaR.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Parametric VaR</span>
                      <span className="font-bold text-blue-400">
                        {varAnalysis.parametricVaR.toFixed(2)}%
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Monte Carlo VaR</span>
                      <span className="font-bold text-purple-400">
                        {varAnalysis.monteCarloVaR.toFixed(2)}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Expected Shortfall</span>
                      <span className="font-bold text-red-400">
                        {varAnalysis.expectedShortfall.toFixed(2)}%
                      </span>
                    </div>
                  </div>
                </div>

                {/* Component VaR */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Component VaR Analysis</h3>
                  
                  <div className="space-y-3">
                    {varAnalysis.componentVaR.map((component) => (
                      <div key={component.symbol} className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                        <div>
                          <div className="font-medium text-white">{component.symbol}</div>
                          <div className="text-xs text-slate-400">
                            {component.componentPercent.toFixed(1)}% of portfolio
                          </div>
                        </div>
                        
                        <div className="text-right">
                          <div className="font-bold text-red-400">
                            {component.value.toFixed(2)}%
                          </div>
                          <div className="text-xs text-slate-400">
                            {component.contribution.toFixed(1)}% of VaR
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* VaR Distribution */}
                <div className="col-span-2 bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Return Distribution Analysis</h3>
                  
                  <div className="grid grid-cols-3 gap-6">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-400 mb-1">
                        {varAnalysis.bestCase.toFixed(2)}%
                      </div>
                      <p className="text-sm text-slate-400">Best Case (95th percentile)</p>
                    </div>
                    
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-400 mb-1">
                        {varAnalysis.portfolioVaR.toFixed(2)}%
                      </div>
                      <p className="text-sm text-slate-400">VaR ({varAnalysis.confidence}% confidence)</p>
                    </div>
                    
                    <div className="text-center">
                      <div className="text-2xl font-bold text-red-400 mb-1">
                        {varAnalysis.worstCase.toFixed(2)}%
                      </div>
                      <p className="text-sm text-slate-400">Worst Case Scenario</p>
                    </div>
                  </div>

                  <div className="mt-6 h-48 flex items-center justify-center">
                    <div className="text-center">
                      <LineChart className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                      <p className="text-slate-400">VaR Distribution Chart</p>
                      <p className="text-xs text-slate-500">Interactive return distribution visualization</p>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}

            {/* Stress Testing Tab */}
            {activeTab === 'stress' && (
              <motion.div
                key="stress"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="grid grid-cols-2 gap-6">
                  {stressTests.map((test) => (
                    <div
                      key={test.scenario}
                      className="bg-slate-800/30 rounded-xl p-6 border border-slate-700/30 hover:border-slate-600/50 transition-all cursor-pointer"
                      onClick={() => setSelectedScenario(test)}
                    >
                      <div className="flex items-center justify-between mb-4">
                        <div>
                          <h3 className="font-bold text-white mb-1">{test.scenario}</h3>
                          <div className="flex items-center space-x-2">
                            <span className={`px-2 py-1 rounded text-xs font-medium ${
                              test.category === 'MARKET' ? 'bg-blue-600/20 text-blue-400' :
                              test.category === 'CREDIT' ? 'bg-red-600/20 text-red-400' :
                              'bg-purple-600/20 text-purple-400'
                            }`}>
                              {test.category}
                            </span>
                            <span className={`px-2 py-1 rounded text-xs font-medium ${
                              test.severity === 'EXTREME' ? 'bg-red-600/20 text-red-400' :
                              test.severity === 'HIGH' ? 'bg-orange-600/20 text-orange-400' :
                              test.severity === 'MEDIUM' ? 'bg-yellow-600/20 text-yellow-400' :
                              'bg-blue-600/20 text-blue-400'
                            }`}>
                              {test.severity}
                            </span>
                          </div>
                        </div>
                        
                        <div className="text-right">
                          <div className="text-2xl font-bold text-red-400 mb-1">
                            {test.portfolioImpact.toFixed(1)}%
                          </div>
                          <div className="text-xs text-slate-400">Portfolio Impact</div>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4 mb-4">
                        <div>
                          <div className="text-sm text-slate-400">Probability</div>
                          <div className="font-semibold text-white">{test.probability.toFixed(1)}%</div>
                        </div>
                        <div>
                          <div className="text-sm text-slate-400">Max Drawdown</div>
                          <div className="font-semibold text-red-400">{test.maxDrawdown.toFixed(1)}%</div>
                        </div>
                        <div>
                          <div className="text-sm text-slate-400">Recovery Time</div>
                          <div className="font-semibold text-blue-400">{test.recoveryTime} days</div>
                        </div>
                        <div>
                          <div className="text-sm text-slate-400">Hedging Cost</div>
                          <div className="font-semibold text-yellow-400">{test.hedgingCost} bps</div>
                        </div>
                      </div>

                      <p className="text-sm text-slate-300 mb-3">{test.description}</p>

                      <div className="flex items-center justify-between">
                        <div className="text-xs text-slate-500">
                          {test.keyDrivers.length} key drivers
                        </div>
                        <div className="text-xs text-blue-400">
                          Click for details →
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            {/* Concentration Risk Tab */}
            {activeTab === 'concentration' && (
              <motion.div
                key="concentration"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Concentration Risk Analysis</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Type</th>
                          <th className="text-left p-4 text-sm text-slate-400">Name</th>
                          <th className="text-right p-4 text-sm text-slate-400">Current</th>
                          <th className="text-right p-4 text-sm text-slate-400">Limit</th>
                          <th className="text-right p-4 text-sm text-slate-400">Utilization</th>
                          <th className="text-right p-4 text-sm text-slate-400">Risk Contrib</th>
                          <th className="text-center p-4 text-sm text-slate-400">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {concentrationRisks.map((risk) => (
                          <tr
                            key={`${risk.type}-${risk.name}`}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 text-slate-300">
                              {risk.type.replace('_', ' ')}
                            </td>
                            <td className="p-4 font-medium text-white">
                              {risk.name}
                            </td>
                            <td className="p-4 text-right text-white">
                              {risk.currentWeight.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-slate-400">
                              {risk.riskLimit.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right font-medium">
                              <div className="flex items-center justify-end space-x-2">
                                <div className="w-16 bg-slate-700 rounded-full h-2">
                                  <div 
                                    className={`h-2 rounded-full ${
                                      risk.utilizationPercent > 80 ? 'bg-red-400' : 
                                      risk.utilizationPercent > 60 ? 'bg-yellow-400' : 'bg-green-400'
                                    }`}
                                    style={{ width: `${Math.min(risk.utilizationPercent, 100)}%` }}
                                  />
                                </div>
                                <span className="text-white text-sm">
                                  {risk.utilizationPercent.toFixed(0)}%
                                </span>
                              </div>
                            </td>
                            <td className="p-4 text-right text-purple-400">
                              {risk.riskContribution.toFixed(1)}%
                            </td>
                            <td className="p-4 text-center">
                              <span className={`px-2 py-1 rounded-lg text-xs font-medium ${getStatusBg(risk.status)}`}>
                                <span className={getStatusColor(risk.status)}>
                                  {risk.status.replace('_', ' ')}
                                </span>
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

            {/* Correlation Analysis Tab */}
            {activeTab === 'correlation' && (
              <motion.div
                key="correlation"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* Correlation Statistics */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Correlation Statistics</h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Average Correlation</span>
                      <span className="font-bold text-white">
                        {correlationData.averageCorrelation.toFixed(3)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Maximum Correlation</span>
                      <span className="font-bold text-red-400">
                        {correlationData.maxCorrelation.toFixed(3)}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                      <span className="text-slate-400">Minimum Correlation</span>
                      <span className="font-bold text-green-400">
                        {correlationData.minCorrelation.toFixed(3)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* High Correlations */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">High Correlations</h3>
                  
                  <div className="space-y-3">
                    {correlationData.correlationMatrix
                      .filter(c => Math.abs(c.correlation) > 0.6)
                      .sort((a, b) => Math.abs(b.correlation) - Math.abs(a.correlation))
                      .map((corr) => (
                      <div key={`${corr.asset1}-${corr.asset2}`} className="flex items-center justify-between p-3 bg-slate-700/30 rounded-lg">
                        <div>
                          <div className="font-medium text-white">
                            {corr.asset1} - {corr.asset2}
                          </div>
                          <div className="text-xs text-slate-400">
                            {corr.isSignificant ? 'Statistically Significant' : 'Not Significant'}
                          </div>
                        </div>
                        
                        <div className="text-right">
                          <div className={`font-bold ${
                            Math.abs(corr.correlation) > 0.8 ? 'text-red-400' :
                            Math.abs(corr.correlation) > 0.6 ? 'text-yellow-400' : 'text-white'
                          }`}>
                            {corr.correlation.toFixed(3)}
                          </div>
                          <div className="text-xs text-slate-400">
                            t-stat: {corr.significance.toFixed(1)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Cluster Analysis */}
                <div className="col-span-2 bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Asset Cluster Analysis</h3>
                  
                  <div className="grid grid-cols-3 gap-4">
                    {correlationData.clusterAnalysis.map((cluster) => (
                      <div key={cluster.cluster} className="bg-slate-700/30 rounded-lg p-4">
                        <div className="flex items-center justify-between mb-3">
                          <h4 className="font-semibold text-white">{cluster.cluster}</h4>
                          <div className="text-lg font-bold text-purple-400">
                            {cluster.intraClusterCorr.toFixed(2)}
                          </div>
                        </div>
                        
                        <div className="space-y-2 mb-3">
                          {cluster.assets.map((asset) => (
                            <span key={asset} className="inline-block px-2 py-1 bg-slate-800/50 rounded text-xs text-slate-300 mr-1">
                              {asset}
                            </span>
                          ))}
                        </div>
                        
                        <div className="text-sm text-slate-400">
                          Risk Contribution: {cluster.riskContribution.toFixed(1)}%
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Risk Attribution Tab */}
            {activeTab === 'attribution' && (
              <motion.div
                key="attribution"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="space-y-6"
              >
                <div className="bg-slate-800/30 rounded-xl overflow-hidden">
                  <div className="p-4 border-b border-slate-700/50">
                    <h3 className="font-bold text-white">Risk Factor Attribution</h3>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-700/50">
                          <th className="text-left p-4 text-sm text-slate-400">Factor</th>
                          <th className="text-left p-4 text-sm text-slate-400">Category</th>
                          <th className="text-right p-4 text-sm text-slate-400">Risk Contrib.</th>
                          <th className="text-right p-4 text-sm text-slate-400">Risk %</th>
                          <th className="text-right p-4 text-sm text-slate-400">Active Risk</th>
                          <th className="text-right p-4 text-sm text-slate-400">Marginal Risk</th>
                          <th className="text-right p-4 text-sm text-slate-400">Correlation</th>
                        </tr>
                      </thead>
                      <tbody>
                        {riskAttribution.map((factor) => (
                          <tr
                            key={factor.factor}
                            className="border-b border-slate-700/30 hover:bg-slate-700/20 transition-colors"
                          >
                            <td className="p-4 font-medium text-white">
                              {factor.factor}
                            </td>
                            <td className="p-4">
                              <span className={`px-2 py-1 rounded text-xs font-medium ${
                                factor.category === 'SYSTEMATIC' ? 'bg-red-600/20 text-red-400' :
                                factor.category === 'SECTOR' ? 'bg-blue-600/20 text-blue-400' :
                                factor.category === 'STYLE' ? 'bg-purple-600/20 text-purple-400' :
                                'bg-slate-600/20 text-slate-400'
                              }`}>
                                {factor.category}
                              </span>
                            </td>
                            <td className="p-4 text-right font-bold text-white">
                              {factor.riskContribution.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-purple-400">
                              {factor.riskPercent.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-yellow-400">
                              {factor.activeRisk.toFixed(1)}%
                            </td>
                            <td className="p-4 text-right text-blue-400">
                              {factor.marginalRisk.toFixed(2)}
                            </td>
                            <td className="p-4 text-right text-white">
                              {factor.correlationToPortfolio.toFixed(3)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Risk Attribution Chart */}
                <div className="bg-slate-800/30 rounded-xl p-6">
                  <h3 className="font-bold text-white mb-4">Risk Decomposition</h3>
                  
                  <div className="h-48 flex items-center justify-center">
                    <div className="text-center">
                      <PieChart className="w-16 h-16 text-slate-400 mx-auto mb-4" />
                      <p className="text-slate-400">Risk Attribution Breakdown</p>
                      <p className="text-xs text-slate-500">Interactive risk factor decomposition chart</p>
                    </div>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Stress Test Detail Modal */}
      <AnimatePresence>
        {selectedScenario && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setSelectedScenario(null)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-slate-900 rounded-2xl p-6 max-w-4xl w-full max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-white">
                  {selectedScenario.scenario}
                </h2>
                <button
                  onClick={() => setSelectedScenario(null)}
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>

              <div className="grid grid-cols-2 gap-6 mb-6">
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-white mb-2">Key Drivers</h3>
                    <div className="space-y-1">
                      {selectedScenario.keyDrivers.map((driver) => (
                        <div key={driver} className="flex items-center space-x-2">
                          <AlertTriangle className="w-4 h-4 text-yellow-400" />
                          <span className="text-slate-300 text-sm">{driver}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-white mb-2">Mitigation Strategies</h3>
                    <div className="space-y-1">
                      {selectedScenario.mitigationStrategies.map((strategy) => (
                        <div key={strategy} className="flex items-center space-x-2">
                          <Shield className="w-4 h-4 text-green-400" />
                          <span className="text-slate-300 text-sm">{strategy}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-slate-800/30 rounded-xl p-4">
                <p className="text-slate-300">{selectedScenario.description}</p>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default RiskAnalyticsDashboard