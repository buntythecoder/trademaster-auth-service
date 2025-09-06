import React, { useState, useEffect, useCallback } from 'react'
import { motion } from 'framer-motion'
import {
  TrendingUp, TrendingDown, PieChart, BarChart3, Target, Activity, DollarSign, 
  Percent, Calendar, Filter, Download, RefreshCw, Eye, EyeOff, ArrowUp, ArrowDown,
  Zap, Trophy, AlertTriangle, CheckCircle, Clock, Bookmark, Star, TrendingRight
} from 'lucide-react'

interface Asset {
  symbol: string
  name: string
  quantity: number
  avgPrice: number
  currentPrice: number
  value: number
  pnl: number
  pnlPercent: number
  allocation: number
  sector: string
  assetType: 'stock' | 'etf' | 'mutual_fund' | 'bond' | 'commodity'
  dayChange: number
  dayChangePercent: number
  beta: number
  pe?: number
  dividend?: number
}

interface PortfolioMetrics {
  totalValue: number
  totalInvestment: number
  totalPnL: number
  totalPnLPercent: number
  dayChange: number
  dayChangePercent: number
  sharpeRatio: number
  volatility: number
  beta: number
  var95: number
  maxDrawdown: number
  cagr: number
  dividendYield: number
  expenseRatio: number
}

interface BenchmarkComparison {
  name: string
  returns1M: number
  returns3M: number
  returns6M: number
  returns1Y: number
  returns3Y: number
  alpha: number
  beta: number
  correlation: number
}

interface PerformanceData {
  date: string
  portfolioValue: number
  benchmarkValue: number
  cashFlow?: number
  dividends?: number
}

interface PortfolioAnalyticsDashboardProps {
  initialTab?: 'overview' | 'holdings' | 'performance' | 'risk' | 'comparison' | 'allocation'
}

export const PortfolioAnalyticsDashboard: React.FC<PortfolioAnalyticsDashboardProps> = ({ 
  initialTab = 'overview' 
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'holdings' | 'performance' | 'risk' | 'comparison' | 'allocation'>(initialTab)
  const [portfolioMetrics, setPortfolioMetrics] = useState<PortfolioMetrics | null>(null)
  const [assets, setAssets] = useState<Asset[]>([])
  const [performanceData, setPerformanceData] = useState<PerformanceData[]>([])
  const [benchmarks, setBenchmarks] = useState<BenchmarkComparison[]>([])
  const [timeframe, setTimeframe] = useState<'1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL'>('1Y')
  const [showValues, setShowValues] = useState(true)
  
  useEffect(() => {
    loadPortfolioData()
    const interval = setInterval(loadPortfolioData, 30000) // Refresh every 30 seconds
    return () => clearInterval(interval)
  }, [])

  const loadPortfolioData = useCallback(() => {
    // Mock Portfolio Assets
    const mockAssets: Asset[] = [
      {
        symbol: 'RELIANCE',
        name: 'Reliance Industries Ltd',
        quantity: 50,
        avgPrice: 2450.00,
        currentPrice: 2678.50,
        value: 133925.00,
        pnl: 11425.00,
        pnlPercent: 9.33,
        allocation: 15.8,
        sector: 'Energy',
        assetType: 'stock',
        dayChange: 45.20,
        dayChangePercent: 1.72,
        beta: 1.15,
        pe: 24.5,
        dividend: 1.8
      },
      {
        symbol: 'TCS',
        name: 'Tata Consultancy Services',
        quantity: 25,
        avgPrice: 3250.00,
        currentPrice: 3542.80,
        value: 88570.00,
        pnl: 7320.00,
        pnlPercent: 9.01,
        allocation: 10.4,
        sector: 'IT',
        assetType: 'stock',
        dayChange: -12.40,
        dayChangePercent: -0.35,
        beta: 0.89,
        pe: 28.2,
        dividend: 2.1
      },
      {
        symbol: 'HDFCBANK',
        name: 'HDFC Bank Limited',
        quantity: 40,
        avgPrice: 1580.00,
        currentPrice: 1634.70,
        value: 65388.00,
        pnl: 2188.00,
        pnlPercent: 3.47,
        allocation: 7.7,
        sector: 'Banking',
        assetType: 'stock',
        dayChange: 8.30,
        dayChangePercent: 0.51,
        beta: 1.08,
        pe: 18.4,
        dividend: 1.5
      },
      {
        symbol: 'NIFTY50ETF',
        name: 'ICICI Prudential Nifty 50 ETF',
        quantity: 200,
        avgPrice: 182.50,
        currentPrice: 196.80,
        value: 39360.00,
        pnl: 2860.00,
        pnlPercent: 7.84,
        allocation: 4.6,
        sector: 'Index Fund',
        assetType: 'etf',
        dayChange: 1.20,
        dayChangePercent: 0.61,
        beta: 1.00,
        dividend: 1.2
      },
      {
        symbol: 'AXISBANK',
        name: 'Axis Bank Limited',
        quantity: 60,
        avgPrice: 980.00,
        currentPrice: 1045.25,
        value: 62715.00,
        pnl: 3915.00,
        pnlPercent: 6.66,
        allocation: 7.4,
        sector: 'Banking',
        assetType: 'stock',
        dayChange: -5.80,
        dayChangePercent: -0.55,
        beta: 1.32,
        pe: 12.8,
        dividend: 0.8
      },
      {
        symbol: 'ASIANPAINT',
        name: 'Asian Paints Limited',
        quantity: 15,
        avgPrice: 3180.00,
        currentPrice: 2945.60,
        value: 44184.00,
        pnl: -3516.00,
        pnlPercent: -7.38,
        allocation: 5.2,
        sector: 'Consumer Goods',
        assetType: 'stock',
        dayChange: -22.40,
        dayChangePercent: -0.75,
        beta: 0.75,
        pe: 45.2,
        dividend: 1.9
      },
      {
        symbol: 'GOLDBEES',
        name: 'Goldman Sachs Gold ETF',
        quantity: 100,
        avgPrice: 42.80,
        currentPrice: 44.95,
        value: 4495.00,
        pnl: 215.00,
        pnlPercent: 5.02,
        allocation: 0.5,
        sector: 'Commodity',
        assetType: 'etf',
        dayChange: 0.15,
        dayChangePercent: 0.33,
        beta: -0.12,
        dividend: 0.0
      }
    ]

    // Calculate portfolio metrics
    const totalValue = mockAssets.reduce((sum, asset) => sum + asset.value, 0)
    const totalInvestment = mockAssets.reduce((sum, asset) => sum + (asset.quantity * asset.avgPrice), 0)
    const totalPnL = totalValue - totalInvestment
    const totalPnLPercent = (totalPnL / totalInvestment) * 100
    const dayChange = mockAssets.reduce((sum, asset) => sum + (asset.quantity * asset.dayChange), 0)
    const dayChangePercent = (dayChange / (totalValue - dayChange)) * 100

    const mockMetrics: PortfolioMetrics = {
      totalValue,
      totalInvestment,
      totalPnL,
      totalPnLPercent,
      dayChange,
      dayChangePercent,
      sharpeRatio: 1.47,
      volatility: 15.8,
      beta: 1.08,
      var95: -2.8,
      maxDrawdown: -12.4,
      cagr: 14.2,
      dividendYield: 1.6,
      expenseRatio: 0.05
    }

    // Mock benchmark data
    const mockBenchmarks: BenchmarkComparison[] = [
      {
        name: 'NIFTY 50',
        returns1M: 2.8,
        returns3M: 8.4,
        returns6M: 12.6,
        returns1Y: 18.9,
        returns3Y: 42.7,
        alpha: 2.1,
        beta: 1.08,
        correlation: 0.89
      },
      {
        name: 'NIFTY 100',
        returns1M: 2.6,
        returns3M: 8.1,
        returns6M: 12.2,
        returns1Y: 18.2,
        returns3Y: 41.3,
        alpha: 1.8,
        beta: 1.05,
        correlation: 0.92
      },
      {
        name: 'BSE SENSEX',
        returns1M: 2.7,
        returns3M: 8.3,
        returns6M: 12.4,
        returns1Y: 18.6,
        returns3Y: 42.1,
        alpha: 1.9,
        beta: 1.06,
        correlation: 0.87
      }
    ]

    // Mock performance data
    const mockPerformanceData: PerformanceData[] = Array.from({ length: 365 }, (_, i) => {
      const date = new Date()
      date.setDate(date.getDate() - (365 - i))
      
      return {
        date: date.toISOString().split('T')[0],
        portfolioValue: totalValue * (0.85 + (i / 365) * 0.3 + Math.sin(i / 30) * 0.05),
        benchmarkValue: totalValue * (0.88 + (i / 365) * 0.25 + Math.sin(i / 35) * 0.03),
        cashFlow: i % 30 === 0 ? Math.random() * 10000 : undefined,
        dividends: i % 90 === 0 ? Math.random() * 500 : undefined
      }
    })

    setAssets(mockAssets)
    setPortfolioMetrics(mockMetrics)
    setBenchmarks(mockBenchmarks)
    setPerformanceData(mockPerformanceData)
  }, [])

  const formatCurrency = (amount: number) => {
    if (showValues) {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0
      }).format(amount)
    }
    return '••••••'
  }

  const formatPercent = (percent: number) => {
    return `${percent >= 0 ? '+' : ''}${percent.toFixed(2)}%`
  }

  if (!portfolioMetrics) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6 flex items-center justify-center">
        <div className="flex items-center space-x-3">
          <RefreshCw className="w-6 h-6 animate-spin text-purple-400" />
          <span className="text-white text-lg">Loading portfolio data...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6">
      <div className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
              Portfolio Analytics Dashboard
            </h1>
            <p className="text-slate-400 text-lg">
              Comprehensive portfolio performance and risk analysis
            </p>
          </div>
          
          <div className="flex items-center space-x-4">
            <button
              onClick={() => setShowValues(!showValues)}
              className="flex items-center space-x-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-all duration-200"
            >
              {showValues ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
              <span className="text-white">{showValues ? 'Hide' : 'Show'} Values</span>
            </button>
            
            <button
              onClick={loadPortfolioData}
              className="flex items-center space-x-2 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg transition-all duration-200"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="text-white">Refresh</span>
            </button>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {[
            { id: 'overview', label: 'Portfolio Overview', icon: BarChart3 },
            { id: 'holdings', label: 'Holdings Analysis', icon: PieChart },
            { id: 'performance', label: 'Performance', icon: TrendingUp },
            { id: 'risk', label: 'Risk Analysis', icon: AlertTriangle },
            { id: 'comparison', label: 'Benchmark Comparison', icon: Target },
            { id: 'allocation', label: 'Asset Allocation', icon: Activity }
          ].map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Content */}
      <div className="space-y-6">
        {activeTab === 'overview' && <OverviewTab metrics={portfolioMetrics} assets={assets} formatCurrency={formatCurrency} formatPercent={formatPercent} />}
        {activeTab === 'holdings' && <HoldingsTab assets={assets} formatCurrency={formatCurrency} formatPercent={formatPercent} />}
        {activeTab === 'performance' && <PerformanceTab performanceData={performanceData} timeframe={timeframe} setTimeframe={setTimeframe} formatCurrency={formatCurrency} />}
        {activeTab === 'risk' && <RiskAnalysisTab metrics={portfolioMetrics} assets={assets} formatCurrency={formatCurrency} formatPercent={formatPercent} />}
        {activeTab === 'comparison' && <BenchmarkComparisonTab benchmarks={benchmarks} metrics={portfolioMetrics} formatPercent={formatPercent} />}
        {activeTab === 'allocation' && <AllocationTab assets={assets} formatCurrency={formatCurrency} formatPercent={formatPercent} />}
      </div>
    </div>
  )
}

const OverviewTab: React.FC<{
  metrics: PortfolioMetrics
  assets: Asset[]
  formatCurrency: (amount: number) => string
  formatPercent: (percent: number) => string
}> = ({ metrics, assets, formatCurrency, formatPercent }) => (
  <div className="space-y-6">
    {/* Key Metrics */}
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-green-500/20 to-green-600/20">
            <DollarSign className="h-6 w-6 text-green-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{formatCurrency(metrics.totalValue)}</div>
          </div>
        </div>
        <h3 className="text-green-400 font-semibold mb-1">Portfolio Value</h3>
        <p className="text-slate-400 text-sm">current market value</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className={`p-3 rounded-xl bg-gradient-to-br ${
            metrics.totalPnL >= 0 ? 'from-green-500/20 to-green-600/20' : 'from-red-500/20 to-red-600/20'
          }`}>
            {metrics.totalPnL >= 0 ? 
              <TrendingUp className="h-6 w-6 text-green-400" /> :
              <TrendingDown className="h-6 w-6 text-red-400" />
            }
          </div>
          <div className="text-right">
            <div className={`text-2xl font-bold ${metrics.totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {formatCurrency(metrics.totalPnL)}
            </div>
          </div>
        </div>
        <h3 className={`font-semibold mb-1 ${metrics.totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
          Total P&L
        </h3>
        <p className="text-slate-400 text-sm">{formatPercent(metrics.totalPnLPercent)}</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className={`p-3 rounded-xl bg-gradient-to-br ${
            metrics.dayChange >= 0 ? 'from-green-500/20 to-green-600/20' : 'from-red-500/20 to-red-600/20'
          }`}>
            {metrics.dayChange >= 0 ? 
              <ArrowUp className="h-6 w-6 text-green-400" /> :
              <ArrowDown className="h-6 w-6 text-red-400" />
            }
          </div>
          <div className="text-right">
            <div className={`text-2xl font-bold ${metrics.dayChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {formatCurrency(metrics.dayChange)}
            </div>
          </div>
        </div>
        <h3 className={`font-semibold mb-1 ${metrics.dayChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
          Today's Change
        </h3>
        <p className="text-slate-400 text-sm">{formatPercent(metrics.dayChangePercent)}</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
            <Trophy className="h-6 w-6 text-blue-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{metrics.sharpeRatio}</div>
          </div>
        </div>
        <h3 className="text-blue-400 font-semibold mb-1">Sharpe Ratio</h3>
        <p className="text-slate-400 text-sm">risk-adjusted returns</p>
      </div>
    </div>

    {/* Top Holdings */}
    <div className="glass-card p-6 rounded-2xl">
      <h3 className="text-xl font-bold text-white mb-6">Top Holdings</h3>
      <div className="space-y-4">
        {assets.slice(0, 5).map((asset, index) => (
          <div key={asset.symbol} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div className="flex items-center space-x-4">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center">
                <span className="text-white font-bold">{index + 1}</span>
              </div>
              <div>
                <h4 className="text-white font-semibold">{asset.symbol}</h4>
                <p className="text-slate-400 text-sm">{asset.name}</p>
              </div>
            </div>
            
            <div className="text-right">
              <div className="text-white font-semibold">{formatCurrency(asset.value)}</div>
              <div className="text-slate-400 text-sm">{asset.allocation.toFixed(1)}% allocation</div>
            </div>
            
            <div className="text-right">
              <div className={`font-semibold ${asset.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatCurrency(asset.pnl)}
              </div>
              <div className={`text-sm ${asset.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatPercent(asset.pnlPercent)}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const HoldingsTab: React.FC<{
  assets: Asset[]
  formatCurrency: (amount: number) => string
  formatPercent: (percent: number) => string
}> = ({ assets, formatCurrency, formatPercent }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Complete Holdings Analysis</h3>
    <div className="space-y-4">
      {assets.map((asset) => (
        <div key={asset.symbol} className="p-4 bg-slate-800/30 rounded-xl">
          <div className="grid grid-cols-1 md:grid-cols-6 gap-4">
            <div>
              <h4 className="text-white font-semibold">{asset.symbol}</h4>
              <p className="text-slate-400 text-sm">{asset.sector}</p>
            </div>
            <div>
              <p className="text-slate-400 text-sm">Quantity</p>
              <p className="text-white font-semibold">{asset.quantity}</p>
            </div>
            <div>
              <p className="text-slate-400 text-sm">Avg Price</p>
              <p className="text-white font-semibold">{formatCurrency(asset.avgPrice)}</p>
            </div>
            <div>
              <p className="text-slate-400 text-sm">Current Price</p>
              <p className="text-white font-semibold">{formatCurrency(asset.currentPrice)}</p>
            </div>
            <div>
              <p className="text-slate-400 text-sm">Market Value</p>
              <p className="text-white font-semibold">{formatCurrency(asset.value)}</p>
            </div>
            <div>
              <p className="text-slate-400 text-sm">P&L</p>
              <p className={`font-semibold ${asset.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatCurrency(asset.pnl)} ({formatPercent(asset.pnlPercent)})
              </p>
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
)

const PerformanceTab: React.FC<{
  performanceData: PerformanceData[]
  timeframe: '1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL'
  setTimeframe: (timeframe: '1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL') => void
  formatCurrency: (amount: number) => string
}> = ({ performanceData, timeframe, setTimeframe, formatCurrency }) => (
  <div className="glass-card rounded-2xl p-6">
    <div className="flex items-center justify-between mb-6">
      <h3 className="text-xl font-bold text-white">Performance Analysis</h3>
      <div className="flex space-x-2">
        {(['1M', '3M', '6M', '1Y', '3Y', 'ALL'] as const).map(tf => (
          <button
            key={tf}
            onClick={() => setTimeframe(tf)}
            className={`px-3 py-1 rounded text-sm font-medium transition-all duration-200 ${
              timeframe === tf
                ? 'bg-purple-500 text-white'
                : 'text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            {tf}
          </button>
        ))}
      </div>
    </div>
    
    <div className="h-80 bg-slate-800/30 rounded-xl flex items-center justify-center">
      <div className="text-center">
        <TrendingUp className="w-12 h-12 text-purple-400 mx-auto mb-4" />
        <p className="text-slate-400">Performance chart visualization</p>
        <p className="text-slate-500 text-sm mt-2">
          Interactive portfolio vs benchmark comparison for {timeframe}
        </p>
      </div>
    </div>
  </div>
)

const RiskAnalysisTab: React.FC<{
  metrics: PortfolioMetrics
  assets: Asset[]
  formatCurrency: (amount: number) => string
  formatPercent: (percent: number) => string
}> = ({ metrics, assets, formatCurrency, formatPercent }) => (
  <div className="space-y-6">
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500/20 to-orange-600/20">
            <Activity className="h-6 w-6 text-orange-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{metrics.volatility}%</div>
          </div>
        </div>
        <h3 className="text-orange-400 font-semibold mb-1">Volatility</h3>
        <p className="text-slate-400 text-sm">annual volatility</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-red-500/20 to-red-600/20">
            <TrendingDown className="h-6 w-6 text-red-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{formatPercent(metrics.var95)}</div>
          </div>
        </div>
        <h3 className="text-red-400 font-semibold mb-1">VaR (95%)</h3>
        <p className="text-slate-400 text-sm">value at risk</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-yellow-500/20 to-yellow-600/20">
            <AlertTriangle className="h-6 w-6 text-yellow-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{formatPercent(metrics.maxDrawdown)}</div>
          </div>
        </div>
        <h3 className="text-yellow-400 font-semibold mb-1">Max Drawdown</h3>
        <p className="text-slate-400 text-sm">worst decline</p>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
            <Target className="h-6 w-6 text-purple-400" />
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-white">{metrics.beta}</div>
          </div>
        </div>
        <h3 className="text-purple-400 font-semibold mb-1">Portfolio Beta</h3>
        <p className="text-slate-400 text-sm">market correlation</p>
      </div>
    </div>

    <div className="glass-card p-6 rounded-2xl">
      <h3 className="text-xl font-bold text-white mb-6">Risk Contribution by Asset</h3>
      <div className="space-y-4">
        {assets.map((asset) => (
          <div key={asset.symbol} className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
            <div className="flex items-center space-x-4">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-red-500/20 to-orange-500/20 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-orange-400" />
              </div>
              <div>
                <h4 className="text-white font-semibold">{asset.symbol}</h4>
                <p className="text-slate-400 text-sm">{asset.allocation.toFixed(1)}% allocation</p>
              </div>
            </div>
            
            <div className="text-right">
              <div className="text-white font-semibold">Beta: {asset.beta}</div>
              <div className="text-slate-400 text-sm">
                Risk Score: {(asset.beta * asset.allocation / 100 * 10).toFixed(1)}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  </div>
)

const BenchmarkComparisonTab: React.FC<{
  benchmarks: BenchmarkComparison[]
  metrics: PortfolioMetrics
  formatPercent: (percent: number) => string
}> = ({ benchmarks, metrics, formatPercent }) => (
  <div className="glass-card rounded-2xl p-6">
    <h3 className="text-xl font-bold text-white mb-6">Benchmark Comparison</h3>
    <div className="overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="border-b border-slate-700">
            <th className="pb-4 text-slate-400 font-medium">Benchmark</th>
            <th className="pb-4 text-slate-400 font-medium">1M</th>
            <th className="pb-4 text-slate-400 font-medium">3M</th>
            <th className="pb-4 text-slate-400 font-medium">6M</th>
            <th className="pb-4 text-slate-400 font-medium">1Y</th>
            <th className="pb-4 text-slate-400 font-medium">Alpha</th>
            <th className="pb-4 text-slate-400 font-medium">Beta</th>
          </tr>
        </thead>
        <tbody>
          <tr className="border-b border-slate-700/50">
            <td className="py-4 text-white font-semibold">Your Portfolio</td>
            <td className="py-4 text-green-400 font-semibold">+3.2%</td>
            <td className="py-4 text-green-400 font-semibold">+9.1%</td>
            <td className="py-4 text-green-400 font-semibold">+13.8%</td>
            <td className="py-4 text-green-400 font-semibold">+{metrics.cagr.toFixed(1)}%</td>
            <td className="py-4 text-green-400 font-semibold">+2.3%</td>
            <td className="py-4 text-white">{metrics.beta}</td>
          </tr>
          {benchmarks.map((benchmark) => (
            <tr key={benchmark.name} className="border-b border-slate-700/50">
              <td className="py-4 text-white">{benchmark.name}</td>
              <td className="py-4 text-slate-300">{formatPercent(benchmark.returns1M)}</td>
              <td className="py-4 text-slate-300">{formatPercent(benchmark.returns3M)}</td>
              <td className="py-4 text-slate-300">{formatPercent(benchmark.returns6M)}</td>
              <td className="py-4 text-slate-300">{formatPercent(benchmark.returns1Y)}</td>
              <td className="py-4 text-slate-300">{formatPercent(benchmark.alpha)}</td>
              <td className="py-4 text-slate-300">{benchmark.beta}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </div>
)

const AllocationTab: React.FC<{
  assets: Asset[]
  formatCurrency: (amount: number) => string
  formatPercent: (percent: number) => string
}> = ({ assets, formatCurrency, formatPercent }) => {
  const sectorAllocation = assets.reduce((acc, asset) => {
    acc[asset.sector] = (acc[asset.sector] || 0) + asset.allocation
    return acc
  }, {} as Record<string, number>)

  return (
    <div className="space-y-6">
      <div className="grid gap-6 md:grid-cols-2">
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6">Sector Allocation</h3>
          <div className="space-y-4">
            {Object.entries(sectorAllocation).map(([sector, allocation]) => (
              <div key={sector} className="flex items-center justify-between">
                <span className="text-white">{sector}</span>
                <span className="text-purple-400 font-semibold">{allocation.toFixed(1)}%</span>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6">Asset Type Allocation</h3>
          <div className="space-y-4">
            {['stock', 'etf', 'mutual_fund'].map((type) => {
              const allocation = assets
                .filter(asset => asset.assetType === type)
                .reduce((sum, asset) => sum + asset.allocation, 0)
              
              if (allocation === 0) return null

              return (
                <div key={type} className="flex items-center justify-between">
                  <span className="text-white capitalize">{type.replace('_', ' ')}</span>
                  <span className="text-cyan-400 font-semibold">{allocation.toFixed(1)}%</span>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      <div className="glass-card p-6 rounded-2xl">
        <h3 className="text-xl font-bold text-white mb-6">Allocation Visualization</h3>
        <div className="h-80 bg-slate-800/30 rounded-xl flex items-center justify-center">
          <div className="text-center">
            <PieChart className="w-12 h-12 text-purple-400 mx-auto mb-4" />
            <p className="text-slate-400">Interactive allocation pie chart</p>
            <p className="text-slate-500 text-sm mt-2">
              Visual representation of portfolio allocation by sector and asset type
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default PortfolioAnalyticsDashboard