import React, { useState, useEffect, useMemo } from 'react'
import { motion } from 'framer-motion'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts'
import { Calendar, TrendingUp, TrendingDown, BarChart3, Maximize2, Settings } from 'lucide-react'

interface PerformanceDataPoint {
  timestamp: string
  date: Date
  totalValue: number
  dayChange: number
  benchmark: number
  brokerBreakdown?: {
    [brokerId: string]: number
  }
}

interface InteractivePerformanceChartProps {
  portfolioHistory: PerformanceDataPoint[]
  benchmarkData?: PerformanceDataPoint[]
  timeRange?: '1D' | '1W' | '1M' | '3M' | '1Y'
  brokerComparison?: boolean
  height?: number
  touchOptimized?: boolean
  compactControls?: boolean
}

type TimeRange = '1D' | '1W' | '1M' | '3M' | '1Y'

const timeRangeOptions: { label: string; value: TimeRange }[] = [
  { label: '1D', value: '1D' },
  { label: '1W', value: '1W' },
  { label: '1M', value: '1M' },
  { label: '3M', value: '3M' },
  { label: '1Y', value: '1Y' },
]

const generateMockData = (timeRange: TimeRange): PerformanceDataPoint[] => {
  const now = new Date()
  const dataPoints: PerformanceDataPoint[] = []
  
  let intervalMinutes: number
  let totalPoints: number
  
  switch (timeRange) {
    case '1D':
      intervalMinutes = 15
      totalPoints = 24 // 6 hours of data
      break
    case '1W':
      intervalMinutes = 60 * 4 // 4 hours
      totalPoints = 42
      break
    case '1M':
      intervalMinutes = 60 * 24 // 1 day
      totalPoints = 30
      break
    case '3M':
      intervalMinutes = 60 * 24 * 3 // 3 days
      totalPoints = 30
      break
    case '1Y':
      intervalMinutes = 60 * 24 * 12 // 12 days
      totalPoints = 30
      break
  }
  
  let baseValue = 800000
  let benchmarkBase = 100
  
  for (let i = totalPoints; i >= 0; i--) {
    const date = new Date(now.getTime() - (i * intervalMinutes * 60 * 1000))
    const randomChange = (Math.random() - 0.5) * 0.02 // ±1% random walk
    const benchmarkChange = (Math.random() - 0.5) * 0.015 // ±0.75% benchmark
    
    baseValue *= (1 + randomChange)
    benchmarkBase *= (1 + benchmarkChange)
    
    dataPoints.push({
      timestamp: timeRange === '1D' 
        ? date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }),
      date,
      totalValue: Math.round(baseValue),
      dayChange: Math.round(baseValue * randomChange),
      benchmark: Math.round(benchmarkBase * 100) / 100,
      brokerBreakdown: {
        zerodha: Math.round(baseValue * 0.5),
        groww: Math.round(baseValue * 0.3),
        angel: Math.round(baseValue * 0.2)
      }
    })
  }
  
  return dataPoints
}

const CustomTooltip: React.FC<any> = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const portfolioData = payload.find(p => p.dataKey === 'totalValue')
    const benchmarkData = payload.find(p => p.dataKey === 'benchmark')
    
    return (
      <div className="glass-card p-4 rounded-xl border border-purple-500/30">
        <p className="text-sm text-white font-semibold mb-2">{label}</p>
        {portfolioData && (
          <div className="flex items-center space-x-2 mb-1">
            <div className="w-3 h-3 rounded-full bg-purple-400"></div>
            <p className="text-sm text-white">
              Portfolio: ₹{portfolioData.value?.toLocaleString('en-IN')}
            </p>
          </div>
        )}
        {benchmarkData && (
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-cyan-400"></div>
            <p className="text-sm text-white">
              NIFTY 50: {benchmarkData.value?.toFixed(2)}
            </p>
          </div>
        )}
      </div>
    )
  }
  return null
}

export const InteractivePerformanceChart: React.FC<InteractivePerformanceChartProps> = ({
  portfolioHistory = [],
  benchmarkData = [],
  timeRange = '1D',
  brokerComparison = false,
  height = 400,
  touchOptimized = false,
  compactControls = false
}) => {
  const [selectedTimeRange, setSelectedTimeRange] = useState<TimeRange>(timeRange)
  const [showBrokerBreakdown, setShowBrokerBreakdown] = useState(brokerComparison)
  const [showBenchmark, setShowBenchmark] = useState(true)
  const [isFullscreen, setIsFullscreen] = useState(false)
  
  // Generate mock data if no real data provided
  const chartData = useMemo(() => {
    return portfolioHistory.length > 0 
      ? portfolioHistory 
      : generateMockData(selectedTimeRange)
  }, [portfolioHistory, selectedTimeRange])
  
  const performanceMetrics = useMemo(() => {
    if (chartData.length < 2) return { totalReturn: 0, totalReturnPercent: 0, isPositive: true }
    
    const firstValue = chartData[0].totalValue
    const lastValue = chartData[chartData.length - 1].totalValue
    const totalReturn = lastValue - firstValue
    const totalReturnPercent = (totalReturn / firstValue) * 100
    
    return {
      totalReturn,
      totalReturnPercent,
      isPositive: totalReturn >= 0
    }
  }, [chartData])

  return (
    <motion.div 
      className={`glass-card rounded-2xl p-6 ${isFullscreen ? 'fixed inset-4 z-50' : ''}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.1 }}
    >
      {/* Chart Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-xl font-bold text-white flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-purple-400" />
            <span>Performance Analysis</span>
          </h3>
          <div className="flex items-center space-x-4 mt-2">
            <div className={`text-sm font-medium ${
              performanceMetrics.isPositive ? 'text-green-400' : 'text-red-400'
            }`}>
              {performanceMetrics.isPositive ? '+' : ''}
              {performanceMetrics.totalReturnPercent.toFixed(2)}% 
              ({performanceMetrics.isPositive ? '+' : ''}₹
              {Math.abs(performanceMetrics.totalReturn).toLocaleString('en-IN')})
            </div>
            <div className="w-1 h-1 bg-slate-600 rounded-full" />
            <div className="text-xs text-slate-400">
              {selectedTimeRange} performance
            </div>
          </div>
        </div>
        
        {!compactControls && (
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className="cyber-button-sm p-2 rounded-lg"
            >
              <Maximize2 className="w-4 h-4" />
            </button>
            <button className="cyber-button-sm p-2 rounded-lg">
              <Settings className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>

      {/* Time Range Controls */}
      <div className={`flex items-center justify-between mb-6 ${
        compactControls ? 'flex-col space-y-4' : 'flex-row'
      }`}>
        <div className={`flex items-center space-x-2 ${
          compactControls ? 'flex-wrap justify-center' : ''
        }`}>
          {timeRangeOptions.map((option) => (
            <button
              key={option.value}
              onClick={() => setSelectedTimeRange(option.value)}
              className={`px-4 py-2 rounded-xl font-medium transition-all duration-300 ${
                selectedTimeRange === option.value
                  ? 'cyber-button text-white'
                  : 'glass-card text-slate-400 hover:text-white border border-slate-700 hover:border-purple-500/50'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
        
        <div className={`flex items-center space-x-4 ${
          compactControls ? 'flex-wrap justify-center' : ''
        }`}>
          <label className="flex items-center space-x-2 cursor-pointer">
            <input
              type="checkbox"
              checked={showBenchmark}
              onChange={(e) => setShowBenchmark(e.target.checked)}
              className="sr-only"
            />
            <div className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
              showBenchmark 
                ? 'bg-cyan-400 border-cyan-400' 
                : 'border-slate-600'
            }`}>
              {showBenchmark && <div className="w-2 h-2 bg-white rounded-sm" />}
            </div>
            <span className="text-sm text-slate-300">NIFTY 50</span>
          </label>
          
          <label className="flex items-center space-x-2 cursor-pointer">
            <input
              type="checkbox"
              checked={showBrokerBreakdown}
              onChange={(e) => setShowBrokerBreakdown(e.target.checked)}
              className="sr-only"
            />
            <div className={`w-4 h-4 rounded border-2 flex items-center justify-center ${
              showBrokerBreakdown 
                ? 'bg-green-400 border-green-400' 
                : 'border-slate-600'
            }`}>
              {showBrokerBreakdown && <div className="w-2 h-2 bg-white rounded-sm" />}
            </div>
            <span className="text-sm text-slate-300">Broker Split</span>
          </label>
        </div>
      </div>

      {/* Chart Container */}
      <div className="relative" style={{ height: `${height}px` }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart 
            data={chartData} 
            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid 
              strokeDasharray="3 3" 
              stroke="rgb(51, 65, 85)" 
              opacity={0.3}
            />
            <XAxis 
              dataKey="timestamp"
              stroke="rgb(148, 163, 184)"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="rgb(148, 163, 184)"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `₹${(value / 1000).toFixed(0)}K`}
            />
            <Tooltip content={<CustomTooltip />} />
            
            {/* Portfolio Line */}
            <Line
              type="monotone"
              dataKey="totalValue"
              stroke="url(#portfolioGradient)"
              strokeWidth={3}
              dot={false}
              activeDot={{ r: 6, fill: '#a855f7', stroke: '#1e1b3c', strokeWidth: 2 }}
            />
            
            {/* Benchmark Line */}
            {showBenchmark && (
              <Line
                type="monotone"
                dataKey="benchmark"
                stroke="#06b6d4"
                strokeWidth={2}
                dot={false}
                strokeDasharray="5 5"
                opacity={0.7}
              />
            )}
            
            {/* Broker Breakdown Lines */}
            {showBrokerBreakdown && chartData[0]?.brokerBreakdown && (
              <>
                <Line
                  type="monotone"
                  dataKey="brokerBreakdown.zerodha"
                  stroke="#10b981"
                  strokeWidth={2}
                  dot={false}
                  opacity={0.6}
                />
                <Line
                  type="monotone"
                  dataKey="brokerBreakdown.groww"
                  stroke="#f59e0b"
                  strokeWidth={2}
                  dot={false}
                  opacity={0.6}
                />
                <Line
                  type="monotone"
                  dataKey="brokerBreakdown.angel"
                  stroke="#ef4444"
                  strokeWidth={2}
                  dot={false}
                  opacity={0.6}
                />
              </>
            )}
            
            <defs>
              <linearGradient id="portfolioGradient" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stopColor="#a855f7" />
                <stop offset="100%" stopColor="#06b6d4" />
              </linearGradient>
            </defs>
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Performance Summary */}
      <div className="mt-6 pt-4 border-t border-slate-700">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="text-center">
            <div className={`text-lg font-bold ${
              performanceMetrics.isPositive ? 'text-green-400' : 'text-red-400'
            }`}>
              {performanceMetrics.isPositive ? '+' : ''}
              {performanceMetrics.totalReturnPercent.toFixed(2)}%
            </div>
            <div className="text-xs text-slate-400">Total Return</div>
          </div>
          
          <div className="text-center">
            <div className="text-lg font-bold text-white">
              ₹{Math.abs(performanceMetrics.totalReturn).toLocaleString('en-IN')}
            </div>
            <div className="text-xs text-slate-400">Absolute Change</div>
          </div>
          
          <div className="text-center">
            <div className="text-lg font-bold text-cyan-400">
              {chartData.length}
            </div>
            <div className="text-xs text-slate-400">Data Points</div>
          </div>
          
          <div className="text-center">
            <div className="text-lg font-bold text-purple-400">
              {selectedTimeRange}
            </div>
            <div className="text-xs text-slate-400">Time Range</div>
          </div>
        </div>
      </div>
    </motion.div>
  )
}