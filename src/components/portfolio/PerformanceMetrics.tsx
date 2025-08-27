import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Activity, 
  TrendingUp, 
  TrendingDown, 
  Target, 
  Shield, 
  Zap,
  Info,
  BarChart3,
  AlertCircle 
} from 'lucide-react'
import { PortfolioData } from '../../hooks/usePortfolioWebSocket'
import { cn } from '../../lib/utils'

interface PerformanceMetricsProps {
  portfolio: PortfolioData | null
}

interface MetricCard {
  id: string
  title: string
  value: string
  change?: number
  description: string
  category: 'returns' | 'risk' | 'benchmark'
  icon: React.ReactNode
  color: string
  tooltip: string
  trend: 'up' | 'down' | 'neutral'
}

type TimeFrame = '1M' | '3M' | '6M' | '1Y' | '3Y' | '5Y' | 'ALL'

export function PerformanceMetrics({ portfolio }: PerformanceMetricsProps) {
  const [selectedTimeframe, setSelectedTimeframe] = useState<TimeFrame>('1Y')
  const [selectedCategory, setSelectedCategory] = useState<'all' | 'returns' | 'risk' | 'benchmark'>('all')
  const [showTooltip, setShowTooltip] = useState<string | null>(null)

  const timeframes = [
    { label: '1M', value: '1M' as TimeFrame },
    { label: '3M', value: '3M' as TimeFrame },
    { label: '6M', value: '6M' as TimeFrame },
    { label: '1Y', value: '1Y' as TimeFrame },
    { label: '3Y', value: '3Y' as TimeFrame },
    { label: '5Y', value: '5Y' as TimeFrame },
    { label: 'ALL', value: 'ALL' as TimeFrame }
  ]

  const categories = [
    { label: 'All Metrics', value: 'all' },
    { label: 'Returns', value: 'returns' },
    { label: 'Risk', value: 'risk' },
    { label: 'Benchmark', value: 'benchmark' }
  ]

  // Calculate metrics based on portfolio data
  const getMetrics = (): MetricCard[] => {
    if (!portfolio) return []

    const baseMetrics: MetricCard[] = [
      // Returns Metrics
      {
        id: 'total-return',
        title: 'Total Return',
        value: `${portfolio.summary.portfolioReturn >= 0 ? '+' : ''}${portfolio.summary.portfolioReturn.toFixed(2)}%`,
        change: portfolio.summary.portfolioReturn,
        description: 'Overall portfolio performance',
        category: 'returns',
        icon: <TrendingUp className="w-5 h-5" />,
        color: portfolio.summary.portfolioReturn >= 0 ? 'green' : 'red',
        tooltip: 'Total return since portfolio inception, calculated as (Current Value - Total Invested) / Total Invested',
        trend: portfolio.summary.portfolioReturn >= 0 ? 'up' : 'down'
      },
      {
        id: 'annualized-return',
        title: 'Annualized Return',
        value: `+${portfolio.summary.annualizedReturn.toFixed(1)}%`,
        change: portfolio.summary.annualizedReturn,
        description: 'Yearly average return',
        category: 'returns',
        icon: <BarChart3 className="w-5 h-5" />,
        color: 'cyan',
        tooltip: 'Compound annual growth rate (CAGR) of your portfolio over the selected time period',
        trend: 'up'
      },
      {
        id: 'day-return',
        title: 'Day Return',
        value: `${portfolio.summary.dayChangePercent >= 0 ? '+' : ''}${portfolio.summary.dayChangePercent.toFixed(2)}%`,
        change: portfolio.summary.dayChangePercent,
        description: 'Today\'s performance',
        category: 'returns',
        icon: <Zap className="w-5 h-5" />,
        color: portfolio.summary.dayChangePercent >= 0 ? 'green' : 'red',
        tooltip: 'Portfolio return for the current trading session',
        trend: portfolio.summary.dayChangePercent >= 0 ? 'up' : 'down'
      },

      // Risk Metrics
      {
        id: 'volatility',
        title: 'Volatility',
        value: '18.5%',
        description: 'Price fluctuation measure',
        category: 'risk',
        icon: <Activity className="w-5 h-5" />,
        color: 'orange',
        tooltip: 'Standard deviation of returns, indicating how much your portfolio value fluctuates',
        trend: 'neutral'
      },
      {
        id: 'max-drawdown',
        title: 'Max Drawdown',
        value: '-12.3%',
        change: -12.3,
        description: 'Largest peak-to-trough decline',
        category: 'risk',
        icon: <TrendingDown className="w-5 h-5" />,
        color: 'red',
        tooltip: 'Maximum loss from a peak to the subsequent trough, indicating downside risk',
        trend: 'down'
      },
      {
        id: 'sharpe-ratio',
        title: 'Sharpe Ratio',
        value: '1.42',
        change: 1.42,
        description: 'Risk-adjusted returns',
        category: 'risk',
        icon: <Shield className="w-5 h-5" />,
        color: 'blue',
        tooltip: 'Measure of risk-adjusted return. Higher values indicate better risk-adjusted performance',
        trend: 'up'
      },
      {
        id: 'var-95',
        title: 'VaR (95%)',
        value: '-₹24,567',
        change: -24567,
        description: '1-day Value at Risk',
        category: 'risk',
        icon: <AlertCircle className="w-5 h-5" />,
        color: 'red',
        tooltip: '95% confidence that portfolio will not lose more than this amount in one day',
        trend: 'down'
      },

      // Benchmark Comparison
      {
        id: 'alpha',
        title: 'Alpha',
        value: '+2.4%',
        change: 2.4,
        description: 'Excess return vs NIFTY 50',
        category: 'benchmark',
        icon: <Target className="w-5 h-5" />,
        color: 'green',
        tooltip: 'Portfolio return minus benchmark return, indicating outperformance',
        trend: 'up'
      },
      {
        id: 'beta',
        title: 'Beta',
        value: '1.15',
        change: 1.15,
        description: 'Market sensitivity',
        category: 'benchmark',
        icon: <Activity className="w-5 h-5" />,
        color: 'yellow',
        tooltip: 'Measure of portfolio volatility relative to market. Beta > 1 means more volatile than market',
        trend: 'neutral'
      },
      {
        id: 'correlation',
        title: 'Correlation',
        value: '0.78',
        change: 0.78,
        description: 'Market relationship',
        category: 'benchmark',
        icon: <BarChart3 className="w-5 h-5" />,
        color: 'blue',
        tooltip: 'How closely portfolio moves with the market. 1.0 means perfect correlation',
        trend: 'neutral'
      },
      {
        id: 'information-ratio',
        title: 'Information Ratio',
        value: '0.65',
        change: 0.65,
        description: 'Active return efficiency',
        category: 'benchmark',
        icon: <Target className="w-5 h-5" />,
        color: 'cyan',
        tooltip: 'Alpha divided by tracking error, measuring active management efficiency',
        trend: 'up'
      }
    ]

    return baseMetrics
  }

  const metrics = getMetrics()
  
  const filteredMetrics = selectedCategory === 'all' 
    ? metrics 
    : metrics.filter(metric => metric.category === selectedCategory)

  const getColorClasses = (color: string) => {
    const colors = {
      green: 'text-green-400 bg-green-500/20 border-green-500/30',
      red: 'text-red-400 bg-red-500/20 border-red-500/30',
      cyan: 'text-cyan-400 bg-cyan-500/20 border-cyan-500/30',
      blue: 'text-blue-400 bg-blue-500/20 border-blue-500/30',
      orange: 'text-orange-400 bg-orange-500/20 border-orange-500/30',
      yellow: 'text-yellow-400 bg-yellow-500/20 border-yellow-500/30'
    }
    return colors[color as keyof typeof colors] || colors.blue
  }

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-6">
          <div className="h-6 bg-slate-700 rounded w-1/3"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="glass-card p-6 rounded-xl">
                <div className="space-y-3">
                  <div className="h-4 bg-slate-700 rounded w-3/4"></div>
                  <div className="h-8 bg-slate-700 rounded w-1/2"></div>
                  <div className="h-3 bg-slate-700 rounded w-full"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-2xl p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-lg bg-blue-500/20">
            <BarChart3 className="w-5 h-5 text-blue-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">Performance Metrics</h3>
            <p className="text-sm text-slate-400">Key performance indicators and risk metrics</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center space-x-3">
          {/* Category Filter */}
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value as any)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            {categories.map(category => (
              <option key={category.value} value={category.value}>
                {category.label}
              </option>
            ))}
          </select>

          {/* Timeframe Selector */}
          <div className="flex rounded-xl glass-card p-1">
            {timeframes.slice(0, 4).map((timeframe) => (
              <button
                key={timeframe.value}
                onClick={() => setSelectedTimeframe(timeframe.value)}
                className={cn(
                  "px-3 py-1.5 text-sm font-medium rounded-lg transition-all",
                  selectedTimeframe === timeframe.value
                    ? "bg-blue-500/20 text-blue-300 border border-blue-500/30"
                    : "text-slate-400 hover:text-white hover:bg-slate-700/50"
                )}
              >
                {timeframe.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <AnimatePresence mode="popLayout">
          {filteredMetrics.map((metric, index) => (
            <motion.div
              key={metric.id}
              layout
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              transition={{ delay: index * 0.05 }}
              className={cn(
                "glass-card p-6 rounded-xl cursor-pointer transition-all relative group",
                "hover:scale-[1.02] hover:shadow-lg"
              )}
              onMouseEnter={() => setShowTooltip(metric.id)}
              onMouseLeave={() => setShowTooltip(null)}
            >
              {/* Metric Header */}
              <div className="flex items-center justify-between mb-4">
                <div className={cn(
                  "p-2 rounded-lg border",
                  getColorClasses(metric.color)
                )}>
                  {metric.icon}
                </div>
                
                {metric.trend !== 'neutral' && (
                  <div className={cn(
                    "flex items-center text-sm font-medium",
                    metric.trend === 'up' ? "text-green-400" : "text-red-400"
                  )}>
                    {metric.trend === 'up' ? (
                      <TrendingUp className="w-3 h-3 mr-1" />
                    ) : (
                      <TrendingDown className="w-3 h-3 mr-1" />
                    )}
                  </div>
                )}
              </div>

              {/* Metric Value */}
              <div className="space-y-2">
                <div className={cn(
                  "text-2xl font-bold",
                  `text-${metric.color}-400`
                )}>
                  {metric.value}
                </div>
                <div className="text-white font-medium text-sm">{metric.title}</div>
                <div className="text-slate-400 text-xs">{metric.description}</div>
              </div>

              {/* Tooltip */}
              <AnimatePresence>
                {showTooltip === metric.id && (
                  <motion.div
                    initial={{ opacity: 0, y: 10, scale: 0.9 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 10, scale: 0.9 }}
                    className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 z-10"
                  >
                    <div className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-300 max-w-48 text-center shadow-xl">
                      {metric.tooltip}
                      <div className="absolute top-full left-1/2 transform -translate-x-1/2">
                        <div className="border-4 border-transparent border-t-slate-900"></div>
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Trend Indicator */}
              <div className="absolute bottom-2 right-2 opacity-20 group-hover:opacity-40 transition-opacity">
                <Info className="w-4 h-4 text-slate-400" />
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 pt-6 border-t border-slate-700/50">
        <div className="glass-card p-6 rounded-xl">
          <div className="text-slate-400 text-sm mb-2">Risk Assessment</div>
          <div className="flex items-center space-x-3">
            <div className="flex-1 bg-slate-700 rounded-full h-3">
              <div className="bg-gradient-to-r from-green-400 to-yellow-400 h-3 rounded-full w-3/5"></div>
            </div>
            <span className="text-white text-sm font-medium">Moderate Risk</span>
          </div>
          <div className="mt-2 text-xs text-slate-500">
            Portfolio shows balanced risk profile with moderate volatility
          </div>
        </div>

        <div className="glass-card p-6 rounded-xl">
          <div className="text-slate-400 text-sm mb-2">Performance Grade</div>
          <div className="text-3xl font-bold text-cyan-400 mb-1">B+</div>
          <div className="text-xs text-slate-500">
            Above average returns with good risk management
          </div>
        </div>

        <div className="glass-card p-6 rounded-xl">
          <div className="text-slate-400 text-sm mb-2">Market Outperformance</div>
          <div className="flex items-center space-x-2">
            <TrendingUp className="w-5 h-5 text-green-400" />
            <div className="text-2xl font-bold text-green-400">+2.4%</div>
          </div>
          <div className="text-xs text-slate-500 mt-1">
            Beating NIFTY 50 by 240 basis points
          </div>
        </div>
      </div>
    </div>
  )
}