import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  BarChart, 
  LineChart, 
  TrendingUp, 
  TrendingDown, 
  Maximize2, 
  Minimize2,
  Settings,
  RefreshCw
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface CandlestickData {
  timestamp: Date
  open: number
  high: number
  low: number
  close: number
  volume: number
}

interface TechnicalIndicator {
  name: string
  value: number
  signal: 'BUY' | 'SELL' | 'HOLD'
  color: string
}

interface PriceChartProps {
  symbol: string
  data: CandlestickData[]
  chartType?: 'candlestick' | 'line' | 'area'
  timeframe?: '1m' | '5m' | '15m' | '1h' | '4h' | '1d'
  indicators?: TechnicalIndicator[]
  showVolume?: boolean
  showGrid?: boolean
  fullscreen?: boolean
  onFullscreenToggle?: () => void
  className?: string
}

const ChartControls: React.FC<{
  chartType: 'candlestick' | 'line' | 'area'
  timeframe: string
  showVolume: boolean
  showGrid: boolean
  onChartTypeChange: (type: 'candlestick' | 'line' | 'area') => void
  onTimeframeChange: (timeframe: string) => void
  onToggleVolume: () => void
  onToggleGrid: () => void
  onRefresh: () => void
}> = ({
  chartType,
  timeframe,
  showVolume,
  showGrid,
  onChartTypeChange,
  onTimeframeChange,
  onToggleVolume,
  onToggleGrid,
  onRefresh
}) => {
  const timeframes = ['1m', '5m', '15m', '1h', '4h', '1d']

  return (
    <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
      {/* Chart Type Controls */}
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onChartTypeChange('candlestick')}
          className={`p-2 rounded-lg transition-all duration-300 ${
            chartType === 'candlestick' 
              ? 'bg-purple-600 text-white' 
              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
          }`}
        >
          <BarChart className="h-4 w-4" />
        </button>
        <button
          onClick={() => onChartTypeChange('line')}
          className={`p-2 rounded-lg transition-all duration-300 ${
            chartType === 'line'
              ? 'bg-purple-600 text-white'
              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
          }`}
        >
          <LineChart className="h-4 w-4" />
        </button>
        <button
          onClick={() => onChartTypeChange('area')}
          className={`p-2 rounded-lg transition-all duration-300 ${
            chartType === 'area'
              ? 'bg-purple-600 text-white'
              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
          }`}
        >
          <TrendingUp className="h-4 w-4" />
        </button>
      </div>

      {/* Timeframe Selector */}
      <div className="flex items-center space-x-1 bg-slate-800 rounded-lg p-1">
        {timeframes.map((tf) => (
          <button
            key={tf}
            onClick={() => onTimeframeChange(tf)}
            className={`px-3 py-1 rounded-md text-xs font-medium transition-all duration-300 ${
              timeframe === tf
                ? 'bg-purple-600 text-white'
                : 'text-slate-300 hover:bg-slate-700'
            }`}
          >
            {tf}
          </button>
        ))}
      </div>

      {/* Settings Controls */}
      <div className="flex items-center space-x-2">
        <button
          onClick={onToggleVolume}
          className={`p-2 rounded-lg transition-all duration-300 ${
            showVolume
              ? 'bg-green-600 text-white'
              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
          }`}
          title="Toggle Volume"
        >
          <BarChart className="h-4 w-4" />
        </button>
        <button
          onClick={onToggleGrid}
          className={`p-2 rounded-lg transition-all duration-300 ${
            showGrid
              ? 'bg-blue-600 text-white'
              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
          }`}
          title="Toggle Grid"
        >
          <Settings className="h-4 w-4" />
        </button>
        <button
          onClick={onRefresh}
          className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 transition-all duration-300"
          title="Refresh Data"
        >
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}

const MockCandlestick: React.FC<{
  data: CandlestickData
  index: number
  maxHigh: number
  minLow: number
  width: number
  height: number
}> = ({ data, index, maxHigh, minLow, width, height }) => {
  const isGreen = data.close >= data.open
  const priceRange = maxHigh - minLow
  
  const yScale = (price: number) => height - ((price - minLow) / priceRange) * height
  
  const openY = yScale(data.open)
  const closeY = yScale(data.close)
  const highY = yScale(data.high)
  const lowY = yScale(data.low)
  
  const bodyHeight = Math.abs(closeY - openY)
  const bodyY = Math.min(openY, closeY)
  
  return (
    <motion.g
      initial={{ opacity: 0, scaleY: 0 }}
      animate={{ opacity: 1, scaleY: 1 }}
      transition={{ delay: index * 0.02, duration: 0.3 }}
    >
      {/* Wick */}
      <line
        x1={width / 2}
        y1={highY}
        x2={width / 2}
        y2={lowY}
        stroke={isGreen ? '#10b981' : '#ef4444'}
        strokeWidth={1}
      />
      
      {/* Body */}
      <rect
        x={width * 0.2}
        y={bodyY}
        width={width * 0.6}
        height={Math.max(bodyHeight, 1)}
        fill={isGreen ? '#10b981' : '#ef4444'}
        opacity={0.8}
        rx={1}
      />
    </motion.g>
  )
}

export const PriceChart: React.FC<PriceChartProps> = ({
  symbol,
  data,
  chartType = 'candlestick',
  timeframe = '15m',
  indicators = [],
  showVolume = true,
  showGrid = true,
  fullscreen = false,
  onFullscreenToggle,
  className = ''
}) => {
  const [currentChartType, setCurrentChartType] = useState(chartType)
  const [currentTimeframe, setCurrentTimeframe] = useState(timeframe)
  const [showVolumeState, setShowVolumeState] = useState(showVolume)
  const [showGridState, setShowGridState] = useState(showGrid)
  const [hoveredCandle, setHoveredCandle] = useState<number | null>(null)
  const { isConnected } = useConnectionStatus()

  // Mock data generator
  const generateMockData = useMemo(() => {
    const mockData: CandlestickData[] = []
    const basePrice = 2456.75
    let currentPrice = basePrice
    const now = new Date()

    for (let i = 99; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * 15 * 60 * 1000) // 15-minute intervals
      
      const volatility = 0.02
      const change = (Math.random() - 0.5) * volatility
      const open = currentPrice
      const close = currentPrice * (1 + change)
      const high = Math.max(open, close) * (1 + Math.random() * 0.01)
      const low = Math.min(open, close) * (1 - Math.random() * 0.01)
      const volume = Math.floor(Math.random() * 1000000) + 500000

      mockData.push({
        timestamp,
        open,
        high,
        low,
        close,
        volume
      })

      currentPrice = close
    }

    return mockData
  }, [])

  const chartData = data.length > 0 ? data : generateMockData
  const currentPrice = chartData[chartData.length - 1]?.close || 0
  const previousPrice = chartData[chartData.length - 2]?.close || currentPrice
  const priceChange = currentPrice - previousPrice
  const priceChangePercent = (priceChange / previousPrice) * 100

  // Chart calculations
  const maxHigh = Math.max(...chartData.map(d => d.high))
  const minLow = Math.min(...chartData.map(d => d.low))
  const maxVolume = Math.max(...chartData.map(d => d.volume))

  const chartHeight = fullscreen ? 500 : 300
  const volumeHeight = showVolumeState ? 80 : 0
  const mainChartHeight = chartHeight - volumeHeight

  const handleRefresh = () => {
    // In real implementation, this would trigger data refresh
    console.log('Refreshing chart data for', symbol)
  }

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      layout={fullscreen}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
        <div className="flex items-center space-x-4">
          <div className={`w-3 h-3 rounded-full ${
            isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
          }`} />
          <div>
            <h2 className="text-lg font-bold text-white">{symbol}</h2>
            <div className="flex items-center space-x-2">
              <span className="text-2xl font-bold text-white">
                ₹{currentPrice.toFixed(2)}
              </span>
              <span className={`flex items-center text-sm ${
                priceChange >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {priceChange >= 0 ? <TrendingUp className="h-3 w-3 mr-1" /> : <TrendingDown className="h-3 w-3 mr-1" />}
                {priceChange >= 0 ? '+' : ''}₹{priceChange.toFixed(2)} ({priceChangePercent.toFixed(2)}%)
              </span>
            </div>
          </div>
        </div>

        {onFullscreenToggle && (
          <button
            onClick={onFullscreenToggle}
            className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300"
          >
            {fullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>
        )}
      </div>

      {/* Chart Controls */}
      <ChartControls
        chartType={currentChartType}
        timeframe={currentTimeframe}
        showVolume={showVolumeState}
        showGrid={showGridState}
        onChartTypeChange={setCurrentChartType}
        onTimeframeChange={setCurrentTimeframe}
        onToggleVolume={() => setShowVolumeState(!showVolumeState)}
        onToggleGrid={() => setShowGridState(!showGridState)}
        onRefresh={handleRefresh}
      />

      {/* Technical Indicators */}
      {indicators.length > 0 && (
        <div className="px-4 py-2 border-b border-slate-700/50">
          <div className="flex flex-wrap gap-3">
            {indicators.map((indicator, index) => (
              <div
                key={index}
                className="flex items-center space-x-2 text-xs bg-slate-800/50 px-3 py-1 rounded-full"
              >
                <div 
                  className="w-2 h-2 rounded-full"
                  style={{ backgroundColor: indicator.color }}
                />
                <span className="text-slate-300">{indicator.name}</span>
                <span className="font-semibold text-white">{indicator.value.toFixed(2)}</span>
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                  indicator.signal === 'BUY' ? 'bg-green-600 text-white' :
                  indicator.signal === 'SELL' ? 'bg-red-600 text-white' :
                  'bg-yellow-600 text-white'
                }`}>
                  {indicator.signal}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Chart Area */}
      <div className="p-4">
        <div className="relative" style={{ height: chartHeight }}>
          <svg
            width="100%"
            height={chartHeight}
            className="rounded-lg bg-slate-900/50"
            onMouseLeave={() => setHoveredCandle(null)}
          >
            {/* Grid */}
            {showGridState && (
              <g opacity={0.2}>
                {Array.from({ length: 5 }).map((_, i) => {
                  const y = (i * mainChartHeight) / 4
                  return (
                    <line
                      key={i}
                      x1={0}
                      y1={y}
                      x2="100%"
                      y2={y}
                      stroke="#64748b"
                      strokeWidth={0.5}
                      strokeDasharray="2,2"
                    />
                  )
                })}
              </g>
            )}

            {/* Candlesticks */}
            <g>
              {chartData.map((candle, index) => {
                const x = (index * 100) / chartData.length + '%'
                const width = 100 / chartData.length

                return (
                  <g key={index} transform={`translate(${x}, 0)`}>
                    <MockCandlestick
                      data={candle}
                      index={index}
                      maxHigh={maxHigh}
                      minLow={minLow}
                      width={width}
                      height={mainChartHeight}
                    />
                  </g>
                )
              })}
            </g>

            {/* Volume Bars */}
            {showVolumeState && (
              <g transform={`translate(0, ${mainChartHeight})`}>
                {chartData.map((candle, index) => {
                  const x = (index * 100) / chartData.length
                  const width = 100 / chartData.length
                  const height = (candle.volume / maxVolume) * volumeHeight
                  const isGreen = candle.close >= candle.open

                  return (
                    <rect
                      key={index}
                      x={`${x}%`}
                      y={volumeHeight - height}
                      width={`${width * 0.8}%`}
                      height={height}
                      fill={isGreen ? '#10b98150' : '#ef444450'}
                      opacity={0.7}
                    />
                  )
                })}
              </g>
            )}
          </svg>

          {/* Hover Tooltip */}
          <AnimatePresence>
            {hoveredCandle !== null && chartData[hoveredCandle] && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 10 }}
                className="absolute top-4 left-4 bg-slate-800 border border-slate-600 rounded-lg p-3 text-sm"
              >
                <div className="space-y-1">
                  <div className="font-semibold text-white">
                    {chartData[hoveredCandle].timestamp.toLocaleString()}
                  </div>
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div>
                      <span className="text-slate-400">Open: </span>
                      <span className="text-white">₹{chartData[hoveredCandle].open.toFixed(2)}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">High: </span>
                      <span className="text-green-400">₹{chartData[hoveredCandle].high.toFixed(2)}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Low: </span>
                      <span className="text-red-400">₹{chartData[hoveredCandle].low.toFixed(2)}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Close: </span>
                      <span className="text-white">₹{chartData[hoveredCandle].close.toFixed(2)}</span>
                    </div>
                  </div>
                  <div className="pt-1 border-t border-slate-600">
                    <span className="text-slate-400">Volume: </span>
                    <span className="text-cyan-400">{chartData[hoveredCandle].volume.toLocaleString()}</span>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Footer */}
      <div className="px-4 pb-4">
        <div className="text-center text-xs text-slate-400">
          Last updated: {new Date().toLocaleTimeString()} • {chartData.length} data points
        </div>
      </div>
    </motion.div>
  )
}