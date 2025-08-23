import React, { useState, useMemo } from 'react'
import { motion } from 'framer-motion'
import {
  TrendingUp,
  TrendingDown,
  PieChart,
  BarChart3,
  Activity,
  Target,
  Calendar,
  Filter,
  ArrowUpCircle,
  ArrowDownCircle,
  DollarSign,
  Percent,
  Clock
} from 'lucide-react'
import { usePortfolio, useMarketData } from '@/hooks/useWebSocket'
import { PerformanceChart } from '../charts/PerformanceChart'
import { PositionBreakdown } from './PositionBreakdown'
import { cn } from '@/lib/utils'

interface TimeRange {
  label: string
  value: '1D' | '1W' | '1M' | '3M' | '1Y' | 'ALL'
  days: number
}

interface PortfolioInsight {
  id: string
  title: string
  value: string
  change: number
  changeType: 'positive' | 'negative' | 'neutral'
  description: string
  icon: React.ReactNode
}

interface AllocationData {
  sector: string
  value: number
  percentage: number
  color: string
}

export const PortfolioAnalytics: React.FC = () => {
  const { portfolio, totalValue, dayPnL, dayPnLPercent, positions } = usePortfolio()
  const [selectedTimeRange, setSelectedTimeRange] = useState<TimeRange['value']>('1M')
  const [selectedView, setSelectedView] = useState<'overview' | 'allocation' | 'performance' | 'insights'>('overview')

  const timeRanges: TimeRange[] = [
    { label: '1D', value: '1D', days: 1 },
    { label: '1W', value: '1W', days: 7 },
    { label: '1M', value: '1M', days: 30 },
    { label: '3M', value: '3M', days: 90 },
    { label: '1Y', value: '1Y', days: 365 },
    { label: 'ALL', value: 'ALL', days: 1000 }
  ]

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const formatNumber = (num: number, decimals: number = 2) => {
    return num.toLocaleString('en-IN', { 
      minimumFractionDigits: decimals, 
      maximumFractionDigits: decimals 
    })
  }

  // Mock sector allocation data
  const sectorAllocation: AllocationData[] = useMemo(() => [
    { sector: 'Technology', value: 75000, percentage: 35, color: '#3B82F6' },
    { sector: 'Banking & Finance', value: 50000, percentage: 25, color: '#10B981' },
    { sector: 'Healthcare', value: 35000, percentage: 18, color: '#8B5CF6' },
    { sector: 'Consumer Goods', value: 25000, percentage: 12, color: '#F59E0B' },
    { sector: 'Energy', value: 15000, percentage: 7, color: '#EF4444' },
    { sector: 'Others', value: 10000, percentage: 3, color: '#6B7280' }
  ], [])

  // Calculate portfolio insights
  const portfolioInsights: PortfolioInsight[] = useMemo(() => [
    {
      id: 'totalReturn',
      title: 'Total Return',
      value: totalValue > 0 ? `${dayPnL >= 0 ? '+' : ''}${formatNumber(dayPnLPercent)}%` : '+12.5%',
      change: dayPnL || 12.5,
      changeType: (dayPnL || 12.5) >= 0 ? 'positive' : 'negative',
      description: 'Since inception',
      icon: <TrendingUp className="w-5 h-5" />
    },
    {
      id: 'bestPerformer',
      title: 'Best Performer',
      value: positions.length > 0 
        ? positions.reduce((best, pos) => pos.pnlPercent > best.pnlPercent ? pos : best, positions[0])?.symbol || 'RELIANCE'
        : 'RELIANCE',
      change: positions.length > 0 
        ? positions.reduce((best, pos) => pos.pnlPercent > best.pnlPercent ? pos : best, positions[0])?.pnlPercent || 24.5
        : 24.5,
      changeType: 'positive',
      description: 'Top gaining stock',
      icon: <ArrowUpCircle className="w-5 h-5" />
    },
    {
      id: 'worstPerformer',
      title: 'Worst Performer',
      value: positions.length > 0 
        ? positions.reduce((worst, pos) => pos.pnlPercent < worst.pnlPercent ? pos : worst, positions[0])?.symbol || 'HDFC'
        : 'HDFC',
      change: positions.length > 0 
        ? positions.reduce((worst, pos) => pos.pnlPercent < worst.pnlPercent ? pos : worst, positions[0])?.pnlPercent || -8.2
        : -8.2,
      changeType: 'negative',
      description: 'Underperforming stock',
      icon: <ArrowDownCircle className="w-5 h-5" />
    },
    {
      id: 'volatility',
      title: 'Portfolio Beta',
      value: '1.24',
      change: 24,
      changeType: 'neutral',
      description: 'Market correlation',
      icon: <Activity className="w-5 h-5" />
    }
  ], [positions, totalValue, dayPnL, dayPnLPercent])

  const viewTabs = [
    { id: 'overview', label: 'Overview', icon: <BarChart3 className="w-4 h-4" /> },
    { id: 'allocation', label: 'Allocation', icon: <PieChart className="w-4 h-4" /> },
    { id: 'performance', label: 'Performance', icon: <TrendingUp className="w-4 h-4" /> },
    { id: 'insights', label: 'Insights', icon: <Target className="w-4 h-4" /> }
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold gradient-text mb-2">Portfolio Analytics</h2>
          <p className="text-slate-400">Comprehensive insights and performance analysis</p>
        </div>
        
        {/* Time Range Selector */}
        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-slate-400" />
          <div className="flex rounded-xl glass-card p-1">
            {timeRanges.map((range) => (
              <button
                key={range.value}
                onClick={() => setSelectedTimeRange(range.value)}
                className={cn(
                  "px-3 py-1.5 text-sm font-medium rounded-lg transition-all",
                  selectedTimeRange === range.value
                    ? "bg-purple-500/20 text-purple-300 border border-purple-500/30"
                    : "text-slate-400 hover:text-white hover:bg-slate-700/50"
                )}
              >
                {range.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* View Tabs */}
      <div className="flex gap-1 glass-card p-1 rounded-xl w-fit">
        {viewTabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setSelectedView(tab.id as any)}
            className={cn(
              "flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-all",
              selectedView === tab.id
                ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                : "text-slate-400 hover:text-white hover:bg-slate-700/50"
            )}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content based on selected view */}
      <motion.div
        key={selectedView}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {selectedView === 'overview' && (
          <div className="grid gap-6">
            {/* Key Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {portfolioInsights.map((insight) => (
                <div key={insight.id} className="glass-card p-6 rounded-xl">
                  <div className="flex items-center justify-between mb-4">
                    <div className={cn(
                      "p-2 rounded-lg",
                      insight.changeType === 'positive' && "bg-green-500/20 text-green-400",
                      insight.changeType === 'negative' && "bg-red-500/20 text-red-400",
                      insight.changeType === 'neutral' && "bg-blue-500/20 text-blue-400"
                    )}>
                      {insight.icon}
                    </div>
                    <div className={cn(
                      "text-sm font-medium",
                      insight.changeType === 'positive' && "text-green-400",
                      insight.changeType === 'negative' && "text-red-400",
                      insight.changeType === 'neutral' && "text-blue-400"
                    )}>
                      {insight.changeType === 'positive' && '+'}
                      {insight.changeType !== 'neutral' && `${insight.change}%`}
                    </div>
                  </div>
                  <div>
                    <div className="text-xl font-bold text-white mb-1">{insight.value}</div>
                    <div className="text-sm text-slate-400">{insight.title}</div>
                    <div className="text-xs text-slate-500 mt-1">{insight.description}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Quick Portfolio Summary */}
            <div className="glass-card p-6 rounded-xl">
              <h3 className="text-lg font-semibold text-white mb-4">Portfolio Summary</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-400">
                    {totalValue > 0 ? formatCurrency(totalValue) : formatCurrency(245847)}
                  </div>
                  <div className="text-sm text-slate-400">Total Value</div>
                </div>
                <div className="text-center">
                  <div className={cn(
                    "text-2xl font-bold",
                    (dayPnL || 3247) >= 0 ? "text-green-400" : "text-red-400"
                  )}>
                    {(dayPnL || 3247) >= 0 ? '+' : ''}{formatCurrency(dayPnL || 3247)}
                  </div>
                  <div className="text-sm text-slate-400">Today's P&L</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-white">
                    {positions.length || 12}
                  </div>
                  <div className="text-sm text-slate-400">Active Positions</div>
                </div>
              </div>
            </div>
          </div>
        )}

        {selectedView === 'allocation' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Sector Allocation Chart Placeholder */}
            <div className="glass-card p-6 rounded-xl">
              <h3 className="text-lg font-semibold text-white mb-4">Sector Allocation</h3>
              <div className="space-y-3">
                {sectorAllocation.map((sector) => (
                  <div key={sector.sector} className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div 
                        className="w-4 h-4 rounded-full"
                        style={{ backgroundColor: sector.color }}
                      />
                      <span className="text-white">{sector.sector}</span>
                    </div>
                    <div className="text-right">
                      <div className="text-white font-medium">{formatCurrency(sector.value)}</div>
                      <div className="text-slate-400 text-sm">{sector.percentage}%</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Top Holdings */}
            <div className="glass-card p-6 rounded-xl">
              <h3 className="text-lg font-semibold text-white mb-4">Top Holdings</h3>
              <div className="space-y-3">
                {positions.slice(0, 6).map((position, index) => (
                  <div key={position.symbol} className="flex items-center justify-between py-2">
                    <div className="flex items-center gap-3">
                      <div className="text-slate-400 text-sm w-6">{index + 1}</div>
                      <div>
                        <div className="text-white font-medium">{position.symbol}</div>
                        <div className="text-slate-400 text-sm">{position.quantity} shares</div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-white">{formatCurrency(position.currentPrice * position.quantity)}</div>
                      <div className={cn(
                        "text-sm",
                        position.pnlPercent >= 0 ? "text-green-400" : "text-red-400"
                      )}>
                        {position.pnlPercent >= 0 ? '+' : ''}{formatNumber(position.pnlPercent)}%
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {selectedView === 'performance' && (
          <div className="grid gap-6">
            {/* Performance Chart */}
            <PerformanceChart
              timeRange={selectedTimeRange}
              onTimeRangeChange={setSelectedTimeRange}
              height={400}
            />

            {/* Performance Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="glass-card p-4 rounded-xl">
                <div className="text-slate-400 text-sm mb-1">Annualized Return</div>
                <div className="text-xl font-bold text-green-400">+18.5%</div>
              </div>
              <div className="glass-card p-4 rounded-xl">
                <div className="text-slate-400 text-sm mb-1">Volatility</div>
                <div className="text-xl font-bold text-white">12.3%</div>
              </div>
              <div className="glass-card p-4 rounded-xl">
                <div className="text-slate-400 text-sm mb-1">Sharpe Ratio</div>
                <div className="text-xl font-bold text-cyan-400">1.42</div>
              </div>
              <div className="glass-card p-4 rounded-xl">
                <div className="text-slate-400 text-sm mb-1">Max Drawdown</div>
                <div className="text-xl font-bold text-red-400">-8.7%</div>
              </div>
            </div>
          </div>
        )}

        {selectedView === 'insights' && (
          <div className="grid gap-6">
            {/* AI Insights */}
            <div className="glass-card p-6 rounded-xl">
              <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                <Target className="w-5 h-5 text-cyan-400" />
                AI-Powered Insights
              </h3>
              <div className="grid gap-4">
                <div className="p-4 bg-green-500/10 border border-green-500/20 rounded-lg">
                  <div className="flex items-start gap-3">
                    <TrendingUp className="w-5 h-5 text-green-400 mt-0.5" />
                    <div>
                      <div className="text-green-400 font-medium mb-1">Portfolio Optimization</div>
                      <div className="text-slate-300 text-sm">
                        Consider rebalancing your technology allocation. Current 35% exposure may be overweight.
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="p-4 bg-blue-500/10 border border-blue-500/20 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Target className="w-5 h-5 text-blue-400 mt-0.5" />
                    <div>
                      <div className="text-blue-400 font-medium mb-1">Diversification Score</div>
                      <div className="text-slate-300 text-sm">
                        Your portfolio shows good sector diversification (8.2/10). Consider adding international exposure.
                      </div>
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-yellow-500/10 border border-yellow-500/20 rounded-lg">
                  <div className="flex items-start gap-3">
                    <Clock className="w-5 h-5 text-yellow-400 mt-0.5" />
                    <div>
                      <div className="text-yellow-400 font-medium mb-1">Rebalancing Alert</div>
                      <div className="text-slate-300 text-sm">
                        HDFC position is down 8.2%. Consider reviewing fundamentals or dollar-cost averaging.
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Risk Metrics */}
            <div className="glass-card p-6 rounded-xl">
              <h3 className="text-lg font-semibold text-white mb-4">Risk Analysis</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <div className="text-slate-400 text-sm mb-2">Risk Level</div>
                  <div className="flex items-center gap-3">
                    <div className="flex-1 bg-slate-700 rounded-full h-2">
                      <div className="bg-gradient-to-r from-green-400 to-yellow-400 h-2 rounded-full w-3/5"></div>
                    </div>
                    <span className="text-white text-sm">Moderate</span>
                  </div>
                </div>
                <div>
                  <div className="text-slate-400 text-sm mb-2">Portfolio Beta</div>
                  <div className="text-2xl font-bold text-white">1.24</div>
                </div>
              </div>
            </div>

            {/* Position Breakdown */}
            <PositionBreakdown showDetailed={true} />
          </div>
        )}
      </motion.div>
    </div>
  )
}

export default PortfolioAnalytics