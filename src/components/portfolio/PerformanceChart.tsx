import React, { useState, useMemo } from 'react'
import { motion } from 'framer-motion'
import { BarChart3, TrendingUp, Activity, Target } from 'lucide-react'
import { PortfolioData } from '../../hooks/usePortfolioWebSocket'
import { cn } from '../../lib/utils'

interface PerformanceChartProps {
  portfolio: PortfolioData | null
}

type TimeFrame = '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL'

interface TimeFrameOption {
  label: string
  value: TimeFrame
  days: number
}

export function PerformanceChart({ portfolio }: PerformanceChartProps) {
  const [selectedTimeframe, setSelectedTimeframe] = useState<TimeFrame>('1M')
  const [showBenchmark, setShowBenchmark] = useState(true)
  const [chartType, setChartType] = useState<'line' | 'area'>('area')

  const timeframes: TimeFrameOption[] = [
    { label: '1D', value: '1D', days: 1 },
    { label: '1W', value: '1W', days: 7 },
    { label: '1M', value: '1M', days: 30 },
    { label: '3M', value: '3M', days: 90 },
    { label: '6M', value: '6M', days: 180 },
    { label: '1Y', value: '1Y', days: 365 },
    { label: 'ALL', value: 'ALL', days: 1000 }
  ]

  // Generate mock performance data based on timeframe
  const performanceData = useMemo(() => {
    if (!portfolio) return []

    const currentTime = new Date()
    const selectedFrame = timeframes.find(tf => tf.value === selectedTimeframe)
    const days = selectedFrame?.days || 30

    const data = []
    const startValue = portfolio.summary.totalInvested
    const endValue = portfolio.summary.currentValue
    const totalReturn = (endValue - startValue) / startValue

    // Generate data points
    for (let i = 0; i <= Math.min(days, 100); i++) {
      const date = new Date(currentTime.getTime() - (days - i) * 24 * 60 * 60 * 1000)
      const progress = i / days
      
      // Add some volatility to make it realistic
      const volatility = 0.02 * Math.sin(i * 0.1) * Math.random()
      const baseReturn = totalReturn * progress
      const adjustedReturn = baseReturn + volatility
      
      const portfolioValue = startValue * (1 + adjustedReturn)
      const benchmarkReturn = 0.085 * progress + (0.01 * Math.sin(i * 0.05)) // NIFTY mock return
      const benchmarkValue = startValue * (1 + benchmarkReturn)

      data.push({
        timestamp: date,
        portfolioValue,
        benchmarkValue,
        returns: adjustedReturn * 100
      })
    }

    return data
  }, [portfolio, selectedTimeframe])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      notation: amount > 1000000 ? 'compact' : 'standard',
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(amount)
  }

  const calculateStats = () => {
    if (!performanceData.length) return { portfolioReturn: 0, benchmarkReturn: 0, alpha: 0 }
    
    const firstPoint = performanceData[0]
    const lastPoint = performanceData[performanceData.length - 1]
    
    const portfolioReturn = ((lastPoint.portfolioValue - firstPoint.portfolioValue) / firstPoint.portfolioValue) * 100
    const benchmarkReturn = ((lastPoint.benchmarkValue - firstPoint.benchmarkValue) / firstPoint.benchmarkValue) * 100
    const alpha = portfolioReturn - benchmarkReturn

    return { portfolioReturn, benchmarkReturn, alpha }
  }

  const stats = calculateStats()

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-slate-700 rounded w-1/3"></div>
          <div className="h-64 bg-slate-700 rounded"></div>
          <div className="flex space-x-2">
            {Array.from({ length: 7 }).map((_, i) => (
              <div key={i} className="h-8 w-12 bg-slate-700 rounded"></div>
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
          <div className="p-2 rounded-lg bg-cyan-500/20">
            <BarChart3 className="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">Performance vs NIFTY 50</h3>
            <p className="text-sm text-slate-400">Historical portfolio performance tracking</p>
          </div>
        </div>

        {/* Chart Controls */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowBenchmark(!showBenchmark)}
            className={cn(
              "px-3 py-1.5 text-xs font-medium rounded-lg transition-all",
              showBenchmark
                ? "bg-purple-500/20 text-purple-300 border border-purple-500/30"
                : "text-slate-400 hover:text-white hover:bg-slate-700/50"
            )}
          >
            Benchmark
          </button>
          <button
            onClick={() => setChartType(chartType === 'line' ? 'area' : 'line')}
            className="p-1.5 rounded-lg glass-card text-slate-400 hover:text-white transition-colors"
          >
            <Activity className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Chart Area - Mock visualization */}
      <div className="relative">
        <div className="h-80 bg-slate-900/50 rounded-xl p-4 relative overflow-hidden">
          {/* Grid lines */}
          <div className="absolute inset-0 opacity-10">
            {Array.from({ length: 5 }).map((_, i) => (
              <div
                key={i}
                className="absolute w-full border-t border-slate-400"
                style={{ top: `${(i + 1) * 20}%` }}
              />
            ))}
            {Array.from({ length: 4 }).map((_, i) => (
              <div
                key={i}
                className="absolute h-full border-l border-slate-400"
                style={{ left: `${(i + 1) * 25}%` }}
              />
            ))}
          </div>

          {/* Mock Chart Lines */}
          <div className="absolute inset-4">
            <svg width="100%" height="100%" className="overflow-visible">
              {/* Portfolio Performance Line */}
              <defs>
                <linearGradient id="portfolioGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stopColor="rgb(34, 197, 94)" stopOpacity="0.3" />
                  <stop offset="100%" stopColor="rgb(34, 197, 94)" stopOpacity="0.05" />
                </linearGradient>
                <linearGradient id="benchmarkGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stopColor="rgb(139, 92, 246)" stopOpacity="0.2" />
                  <stop offset="100%" stopColor="rgb(139, 92, 246)" stopOpacity="0.05" />
                </linearGradient>
              </defs>

              {/* Portfolio area/line */}
              {chartType === 'area' ? (
                <path
                  d="M0,60 Q25,45 50,40 T100,35 L100,100 L0,100 Z"
                  fill="url(#portfolioGradient)"
                  className="animate-pulse"
                />
              ) : null}
              <path
                d="M0,60 Q25,45 50,40 T100,35"
                stroke="rgb(34, 197, 94)"
                strokeWidth="3"
                fill="none"
                className="drop-shadow-lg"
              />

              {/* Benchmark line */}
              {showBenchmark && (
                <>
                  {chartType === 'area' ? (
                    <path
                      d="M0,65 Q25,55 50,50 T100,45 L100,100 L0,100 Z"
                      fill="url(#benchmarkGradient)"
                      className="animate-pulse"
                    />
                  ) : null}
                  <path
                    d="M0,65 Q25,55 50,50 T100,45"
                    stroke="rgb(139, 92, 246)"
                    strokeWidth="2"
                    strokeDasharray="5,5"
                    fill="none"
                  />
                </>
              )}
            </svg>

            {/* Interactive elements */}
            <div className="absolute top-4 left-4 space-y-1">
              <div className="flex items-center space-x-2 text-sm">
                <div className="w-3 h-0.5 bg-green-400 rounded"></div>
                <span className="text-green-400">Portfolio</span>
              </div>
              {showBenchmark && (
                <div className="flex items-center space-x-2 text-sm">
                  <div className="w-3 h-0.5 bg-purple-400 rounded border-dashed border-t"></div>
                  <span className="text-purple-400">NIFTY 50</span>
                </div>
              )}
            </div>

            {/* Value indicators */}
            <div className="absolute top-4 right-4 text-right space-y-1">
              <div className="text-2xl font-bold text-green-400">
                {formatCurrency(portfolio.summary.currentValue)}
              </div>
              <div className="text-sm text-slate-400">Current Value</div>
            </div>
          </div>

          {/* Touch interaction overlay */}
          <div className="absolute inset-0 cursor-crosshair" title="Interactive chart - tap and drag to explore" />
        </div>
      </div>

      {/* Time Frame Selector */}
      <div className="flex items-center justify-center">
        <div className="flex rounded-xl glass-card p-1">
          {timeframes.map((timeframe) => (
            <button
              key={timeframe.value}
              onClick={() => setSelectedTimeframe(timeframe.value)}
              className={cn(
                "px-4 py-2 text-sm font-medium rounded-lg transition-all",
                selectedTimeframe === timeframe.value
                  ? "bg-cyan-500/20 text-cyan-300 border border-cyan-500/30"
                  : "text-slate-400 hover:text-white hover:bg-slate-700/50"
              )}
            >
              {timeframe.label}
            </button>
          ))}
        </div>
      </div>

      {/* Performance Stats */}
      <div className="grid grid-cols-3 gap-4 pt-4 border-t border-slate-700/50">
        <div className="text-center">
          <div className="text-sm text-slate-400 mb-1">Portfolio Return</div>
          <div className={cn(
            "text-lg font-semibold",
            stats.portfolioReturn >= 0 ? "text-green-400" : "text-red-400"
          )}>
            {stats.portfolioReturn >= 0 ? '+' : ''}{stats.portfolioReturn.toFixed(2)}%
          </div>
        </div>

        <div className="text-center">
          <div className="text-sm text-slate-400 mb-1">NIFTY 50 Return</div>
          <div className={cn(
            "text-lg font-semibold",
            stats.benchmarkReturn >= 0 ? "text-green-400" : "text-red-400"
          )}>
            {stats.benchmarkReturn >= 0 ? '+' : ''}{stats.benchmarkReturn.toFixed(2)}%
          </div>
        </div>

        <div className="text-center">
          <div className="text-sm text-slate-400 mb-1">Alpha (Outperformance)</div>
          <div className={cn(
            "text-lg font-semibold flex items-center justify-center space-x-1",
            stats.alpha >= 0 ? "text-cyan-400" : "text-orange-400"
          )}>
            {stats.alpha >= 0 ? (
              <TrendingUp className="w-4 h-4" />
            ) : (
              <Target className="w-4 h-4" />
            )}
            <span>
              {stats.alpha >= 0 ? '+' : ''}{stats.alpha.toFixed(2)}%
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}