import React, { useState, useEffect, useRef } from 'react'
import { motion } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  Calendar,
  BarChart3,
  LineChart,
  Target,
  Zap,
  Activity
} from 'lucide-react'

interface PerformanceDataPoint {
  date: string
  portfolioValue: number
  dailyPnL: number
  dailyReturn: number
  cumulativeReturn: number
  benchmark: number
  volume: number
}

interface PerformanceChartProps {
  data?: PerformanceDataPoint[]
  timeRange: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL'
  onTimeRangeChange: (range: '1D' | '1W' | '1M' | '3M' | '6M' | '1Y' | 'ALL') => void
  height?: number
}

const generateMockPerformanceData = (timeRange: string): PerformanceDataPoint[] => {
  const data: PerformanceDataPoint[] = []
  const now = new Date()
  let startDate = new Date()
  let dataPoints = 30
  
  switch (timeRange) {
    case '1D':
      startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000)
      dataPoints = 24
      break
    case '1W':
      startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      dataPoints = 7
      break
    case '1M':
      startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
      dataPoints = 30
      break
    case '3M':
      startDate = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000)
      dataPoints = 90
      break
    case '6M':
      startDate = new Date(now.getTime() - 180 * 24 * 60 * 60 * 1000)
      dataPoints = 180
      break
    case '1Y':
      startDate = new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000)
      dataPoints = 365
      break
    case 'ALL':
      startDate = new Date(now.getTime() - 2 * 365 * 24 * 60 * 60 * 1000)
      dataPoints = 730
      break
  }
  
  let baseValue = 250000
  let cumulativeReturn = 0
  
  for (let i = 0; i < dataPoints; i++) {
    const date = new Date(startDate.getTime() + i * (24 * 60 * 60 * 1000))
    
    // Generate realistic market movements
    const dailyReturn = (Math.random() - 0.48) * 0.04 // Slight upward bias
    const benchmarkReturn = (Math.random() - 0.5) * 0.025 // Market index
    
    baseValue = baseValue * (1 + dailyReturn)
    cumulativeReturn += dailyReturn * 100
    
    const dailyPnL = baseValue * dailyReturn
    const volume = Math.floor(Math.random() * 50000) + 10000
    
    data.push({
      date: date.toISOString(),
      portfolioValue: parseFloat(baseValue.toFixed(2)),
      dailyPnL: parseFloat(dailyPnL.toFixed(2)),
      dailyReturn: parseFloat((dailyReturn * 100).toFixed(2)),
      cumulativeReturn: parseFloat(cumulativeReturn.toFixed(2)),
      benchmark: parseFloat((benchmarkReturn * 100).toFixed(2)),
      volume
    })
  }
  
  return data
}

export const PerformanceChart: React.FC<PerformanceChartProps> = ({
  data,
  timeRange,
  onTimeRangeChange,
  height = 400
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const [chartData, setChartData] = useState<PerformanceDataPoint[]>([])
  const [chartType, setChartType] = useState<'line' | 'area' | 'candlestick'>('area')
  const [showBenchmark, setShowBenchmark] = useState(true)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })
  const [hoveredPoint, setHoveredPoint] = useState<PerformanceDataPoint | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsLoading(true)
    // Simulate API call delay
    const timer = setTimeout(() => {
      const newData = data || generateMockPerformanceData(timeRange)
      setChartData(newData)
      setIsLoading(false)
    }, 300)
    
    return () => clearTimeout(timer)
  }, [data, timeRange])

  useEffect(() => {
    drawChart()
  }, [chartData, chartType, showBenchmark, height])

  const drawChart = () => {
    const canvas = canvasRef.current
    const container = containerRef.current
    
    if (!canvas || !container || !chartData.length) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const rect = container.getBoundingClientRect()
    const canvasWidth = rect.width
    const canvasHeight = height
    
    canvas.width = canvasWidth * window.devicePixelRatio
    canvas.height = canvasHeight * window.devicePixelRatio
    canvas.style.width = canvasWidth + 'px'
    canvas.style.height = canvasHeight + 'px'
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio)

    // Clear canvas
    ctx.fillStyle = 'rgba(30, 27, 75, 0.4)'
    ctx.fillRect(0, 0, canvasWidth, canvasHeight)

    const padding = { top: 20, right: 80, bottom: 60, left: 60 }
    const chartWidth = canvasWidth - padding.left - padding.right
    const chartHeight = canvasHeight - padding.top - padding.bottom

    // Calculate data ranges
    const values = chartData.map(d => d.portfolioValue)
    const minValue = Math.min(...values) * 0.995
    const maxValue = Math.max(...values) * 1.005
    const valueRange = maxValue - minValue

    // Draw grid
    ctx.strokeStyle = 'rgba(71, 85, 105, 0.3)'
    ctx.lineWidth = 1
    
    for (let i = 0; i <= 5; i++) {
      const y = padding.top + (chartHeight / 5) * i
      ctx.beginPath()
      ctx.moveTo(padding.left, y)
      ctx.lineTo(padding.left + chartWidth, y)
      ctx.stroke()
    }

    // Draw price labels
    ctx.fillStyle = '#94a3b8'
    ctx.font = '12px Inter, system-ui'
    ctx.textAlign = 'left'
    
    for (let i = 0; i <= 5; i++) {
      const value = minValue + (valueRange / 5) * (5 - i)
      const y = padding.top + (chartHeight / 5) * i
      ctx.fillText(`₹${value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`, padding.left + chartWidth + 8, y + 4)
    }

    // Draw time labels
    ctx.textAlign = 'center'
    const labelCount = Math.min(6, chartData.length)
    for (let i = 0; i < labelCount; i++) {
      const dataIndex = Math.floor((chartData.length / labelCount) * i)
      if (dataIndex < chartData.length) {
        const x = padding.left + (chartWidth / (chartData.length - 1)) * dataIndex
        const date = new Date(chartData[dataIndex].date)
        const label = timeRange === '1D' 
          ? date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
          : date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })
        
        ctx.fillStyle = '#94a3b8'
        ctx.fillText(label, x, padding.top + chartHeight + 20)
      }
    }

    // Draw benchmark line if enabled
    if (showBenchmark) {
      ctx.strokeStyle = 'rgba(251, 146, 60, 0.8)' // Orange for benchmark
      ctx.lineWidth = 2
      ctx.setLineDash([5, 5])
      ctx.beginPath()
      
      chartData.forEach((point, index) => {
        const x = padding.left + (chartWidth / (chartData.length - 1)) * index
        // Create a benchmark based on initial value + benchmark returns
        const benchmarkValue = chartData[0].portfolioValue * (1 + (point.cumulativeReturn * 0.7) / 100) // 70% correlation
        const y = padding.top + chartHeight - ((benchmarkValue - minValue) / valueRange) * chartHeight
        
        if (index === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      })
      
      ctx.stroke()
      ctx.setLineDash([])
    }

    // Draw main chart based on type
    if (chartType === 'area') {
      // Create area path
      ctx.beginPath()
      ctx.moveTo(padding.left, padding.top + chartHeight)
      
      chartData.forEach((point, index) => {
        const x = padding.left + (chartWidth / (chartData.length - 1)) * index
        const y = padding.top + chartHeight - ((point.portfolioValue - minValue) / valueRange) * chartHeight
        ctx.lineTo(x, y)
      })
      
      ctx.lineTo(padding.left + chartWidth, padding.top + chartHeight)
      ctx.closePath()
      
      // Create gradient fill
      const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartHeight)
      const lastValue = chartData[chartData.length - 1]
      const firstValue = chartData[0]
      const isPositive = lastValue.portfolioValue >= firstValue.portfolioValue
      
      if (isPositive) {
        gradient.addColorStop(0, 'rgba(16, 185, 129, 0.4)')
        gradient.addColorStop(1, 'rgba(16, 185, 129, 0.05)')
      } else {
        gradient.addColorStop(0, 'rgba(239, 68, 68, 0.4)')
        gradient.addColorStop(1, 'rgba(239, 68, 68, 0.05)')
      }
      
      ctx.fillStyle = gradient
      ctx.fill()
      
      // Draw line on top
      ctx.strokeStyle = isPositive ? '#10b981' : '#ef4444'
      ctx.lineWidth = 3
      ctx.beginPath()
      
      chartData.forEach((point, index) => {
        const x = padding.left + (chartWidth / (chartData.length - 1)) * index
        const y = padding.top + chartHeight - ((point.portfolioValue - minValue) / valueRange) * chartHeight
        
        if (index === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      })
      
      ctx.stroke()
    } else if (chartType === 'line') {
      const lastValue = chartData[chartData.length - 1]
      const firstValue = chartData[0]
      const isPositive = lastValue.portfolioValue >= firstValue.portfolioValue
      
      ctx.strokeStyle = isPositive ? '#10b981' : '#ef4444'
      ctx.lineWidth = 3
      ctx.beginPath()
      
      chartData.forEach((point, index) => {
        const x = padding.left + (chartWidth / (chartData.length - 1)) * index
        const y = padding.top + chartHeight - ((point.portfolioValue - minValue) / valueRange) * chartHeight
        
        if (index === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      })
      
      ctx.stroke()
    }
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    const canvas = canvasRef.current
    if (!canvas || !chartData.length) return

    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    setMousePos({ x, y })

    // Find nearest data point
    const padding = { top: 20, right: 80, bottom: 60, left: 60 }
    const chartWidth = rect.width - padding.left - padding.right
    
    if (x >= padding.left && x <= padding.left + chartWidth) {
      const dataIndex = Math.round(((x - padding.left) / chartWidth) * (chartData.length - 1))
      if (dataIndex >= 0 && dataIndex < chartData.length) {
        setHoveredPoint(chartData[dataIndex])
      }
    } else {
      setHoveredPoint(null)
    }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const formatPercentage = (value: number) => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
  }

  const timeRanges = [
    { label: '1D', value: '1D' as const },
    { label: '1W', value: '1W' as const },
    { label: '1M', value: '1M' as const },
    { label: '3M', value: '3M' as const },
    { label: '6M', value: '6M' as const },
    { label: '1Y', value: '1Y' as const },
    { label: 'ALL', value: 'ALL' as const }
  ]

  // Calculate performance metrics
  const currentValue = chartData[chartData.length - 1]
  const totalReturn = currentValue ? ((currentValue.portfolioValue - chartData[0].portfolioValue) / chartData[0].portfolioValue) * 100 : 0
  const totalPnL = currentValue ? currentValue.portfolioValue - chartData[0].portfolioValue : 0

  return (
    <div className="glass-card rounded-2xl p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-purple-500/20 rounded-xl">
            <Activity className="w-5 h-5 text-purple-400" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Portfolio Performance</h3>
            <p className="text-sm text-slate-400">Track your returns over time</p>
          </div>
        </div>

        {/* Performance Summary */}
        <div className="flex items-center space-x-6">
          <div className="text-right">
            <div className="text-sm text-slate-400">Total Return</div>
            <div className={`text-xl font-bold ${totalReturn >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {formatPercentage(totalReturn)}
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-400">P&L</div>
            <div className={`text-xl font-bold ${totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {totalPnL >= 0 ? '+' : ''}{formatCurrency(totalPnL)}
            </div>
          </div>
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center justify-between mb-4">
        {/* Time Range Selector */}
        <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
          {timeRanges.map((range) => (
            <button
              key={range.value}
              onClick={() => onTimeRangeChange(range.value)}
              className={`px-3 py-1 text-sm font-medium rounded transition-colors ${
                timeRange === range.value
                  ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
              }`}
            >
              {range.label}
            </button>
          ))}
        </div>

        {/* Chart Controls */}
        <div className="flex items-center space-x-2">
          {/* Chart Type */}
          <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
            <button
              onClick={() => setChartType('area')}
              className={`p-2 rounded transition-colors ${
                chartType === 'area' ? 'bg-purple-500/20 text-purple-400' : 'text-slate-400 hover:text-white'
              }`}
              title="Area Chart"
            >
              <BarChart3 className="w-4 h-4" />
            </button>
            <button
              onClick={() => setChartType('line')}
              className={`p-2 rounded transition-colors ${
                chartType === 'line' ? 'bg-purple-500/20 text-purple-400' : 'text-slate-400 hover:text-white'
              }`}
              title="Line Chart"
            >
              <LineChart className="w-4 h-4" />
            </button>
          </div>

          {/* Benchmark Toggle */}
          <button
            onClick={() => setShowBenchmark(!showBenchmark)}
            className={`px-3 py-2 text-sm rounded-lg border transition-colors ${
              showBenchmark
                ? 'bg-orange-500/20 text-orange-400 border-orange-500/50'
                : 'text-slate-400 hover:text-white border-slate-700/50'
            }`}
          >
            Benchmark
          </button>
        </div>
      </div>

      {/* Chart Container */}
      <div 
        ref={containerRef} 
        className="relative bg-slate-900/50 rounded-xl overflow-hidden"
        style={{ height }}
      >
        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/80 z-10">
            <div className="flex items-center space-x-3 text-purple-400">
              <div className="animate-spin rounded-full h-6 w-6 border-2 border-purple-400 border-t-transparent"></div>
              <span className="font-medium">Loading chart data...</span>
            </div>
          </div>
        )}

        <canvas
          ref={canvasRef}
          className="w-full h-full cursor-crosshair"
          onMouseMove={handleMouseMove}
          onMouseLeave={() => {
            setMousePos({ x: 0, y: 0 })
            setHoveredPoint(null)
          }}
        />

        {/* Hover Tooltip */}
        {hoveredPoint && mousePos.x > 0 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="absolute z-20 bg-slate-800/95 backdrop-blur-sm text-white p-4 rounded-lg border border-slate-600/50 shadow-xl min-w-64"
            style={{
              left: Math.min(mousePos.x + 15, containerRef.current?.clientWidth || 0 - 280),
              top: Math.max(mousePos.y - 100, 10),
            }}
          >
            <div className="text-xs text-slate-400 mb-3 font-mono">
              {new Date(hoveredPoint.date).toLocaleDateString('en-IN', {
                weekday: 'short',
                year: 'numeric',
                month: 'short',
                day: 'numeric'
              })}
            </div>
            
            <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <div className="text-slate-300">Portfolio Value:</div>
              <div className="text-white font-semibold text-right">
                {formatCurrency(hoveredPoint.portfolioValue)}
              </div>
              
              <div className="text-slate-300">Daily P&L:</div>
              <div className={`font-semibold text-right ${
                hoveredPoint.dailyPnL >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {hoveredPoint.dailyPnL >= 0 ? '+' : ''}{formatCurrency(hoveredPoint.dailyPnL)}
              </div>
              
              <div className="text-slate-300">Daily Return:</div>
              <div className={`font-semibold text-right ${
                hoveredPoint.dailyReturn >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {formatPercentage(hoveredPoint.dailyReturn)}
              </div>
              
              <div className="text-slate-300">Cumulative Return:</div>
              <div className={`font-semibold text-right ${
                hoveredPoint.cumulativeReturn >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {formatPercentage(hoveredPoint.cumulativeReturn)}
              </div>
            </div>
          </motion.div>
        )}
      </div>

      {/* Legend */}
      <div className="flex items-center justify-center space-x-6 mt-4 text-sm">
        <div className="flex items-center space-x-2">
          <div className="w-3 h-3 bg-gradient-to-r from-green-400 to-purple-500 rounded-full"></div>
          <span className="text-slate-400">Portfolio</span>
        </div>
        {showBenchmark && (
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 border-2 border-dashed border-orange-400 rounded-full"></div>
            <span className="text-slate-400">Benchmark (NIFTY 50)</span>
          </div>
        )}
      </div>
    </div>
  )
}

export default PerformanceChart