// Advanced Chart Component with TradingView-style Features
// FRONT-003: Real-time Market Data Enhancement

import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  BarChart3,
  LineChart,
  TrendingUp,
  TrendingDown,
  Maximize2,
  Minimize2,
  Settings,
  Plus,
  Minus,
  RotateCcw,
  Download,
  Share2,
  Palette,
  Grid3X3,
  Zap,
  Target
} from 'lucide-react'
import { CandlestickData, TechnicalIndicator } from '../../../services/enhancedWebSocketService'

export interface ChartData {
  timestamp: Date
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface DrawingTool {
  type: 'line' | 'rectangle' | 'circle' | 'arrow' | 'fibonacci'
  points: { x: number; y: number; price: number; time: Date }[]
  color: string
  id: string
}

export interface ChartIndicator extends TechnicalIndicator {
  visible: boolean
  settings: Record<string, any>
}

interface AdvancedChartProps {
  symbol: string
  data: ChartData[]
  chartType?: 'candlestick' | 'line' | 'area' | 'ohlc'
  timeframe?: '1m' | '5m' | '15m' | '30m' | '1h' | '4h' | '1d' | '1w'
  indicators?: ChartIndicator[]
  showVolume?: boolean
  showGrid?: boolean
  theme?: 'dark' | 'light'
  height?: number
  fullscreen?: boolean
  onFullscreenToggle?: () => void
  className?: string
}

const timeframeLabels = {
  '1m': '1 Minute',
  '5m': '5 Minutes',
  '15m': '15 Minutes',
  '30m': '30 Minutes',
  '1h': '1 Hour',
  '4h': '4 Hours',
  '1d': '1 Day',
  '1w': '1 Week'
}

const ToolbarButton: React.FC<{
  icon: React.ReactNode
  label: string
  active?: boolean
  onClick: () => void
  className?: string
}> = ({ icon, label, active, onClick, className = '' }) => (
  <motion.button
    whileHover={{ scale: 1.05 }}
    whileTap={{ scale: 0.95 }}
    onClick={onClick}
    className={`
      flex items-center justify-center p-2 rounded-lg transition-all duration-200
      ${active 
        ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50' 
        : 'bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600/50'
      }
      ${className}
    `}
    title={label}
  >
    {icon}
  </motion.button>
)

const TimeframeSelector: React.FC<{
  current: string
  onChange: (timeframe: string) => void
}> = ({ current, onChange }) => {
  const timeframes = ['1m', '5m', '15m', '30m', '1h', '4h', '1d', '1w']
  
  return (
    <div className="flex items-center space-x-1 bg-slate-800/50 rounded-lg p-1">
      {timeframes.map(tf => (
        <button
          key={tf}
          onClick={() => onChange(tf)}
          className={`
            px-3 py-1.5 text-xs font-medium rounded-md transition-all duration-200
            ${current === tf 
              ? 'bg-purple-500 text-white shadow-lg' 
              : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }
          `}
        >
          {tf.replace('m', 'M').replace('h', 'H').replace('d', 'D').replace('w', 'W')}
        </button>
      ))}
    </div>
  )
}

const ChartTypeSelector: React.FC<{
  current: string
  onChange: (type: string) => void
}> = ({ current, onChange }) => {
  const types = [
    { key: 'candlestick', label: 'Candlestick', icon: BarChart3 },
    { key: 'line', label: 'Line', icon: LineChart },
    { key: 'area', label: 'Area', icon: BarChart3 },
    { key: 'ohlc', label: 'OHLC', icon: BarChart3 }
  ]
  
  return (
    <div className="flex items-center space-x-1">
      {types.map(({ key, label, icon: Icon }) => (
        <ToolbarButton
          key={key}
          icon={<Icon className="w-4 h-4" />}
          label={label}
          active={current === key}
          onClick={() => onChange(key)}
        />
      ))}
    </div>
  )
}

const IndicatorPanel: React.FC<{
  indicators: ChartIndicator[]
  onToggleIndicator: (name: string) => void
  onAddIndicator: () => void
}> = ({ indicators, onToggleIndicator, onAddIndicator }) => {
  return (
    <div className="bg-slate-800/30 rounded-lg p-3">
      <div className="flex items-center justify-between mb-3">
        <h4 className="text-sm font-medium text-white">Technical Indicators</h4>
        <button
          onClick={onAddIndicator}
          className="p-1 rounded text-slate-400 hover:text-white transition-colors"
          title="Add Indicator"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>
      
      <div className="space-y-2">
        {indicators.map(indicator => (
          <div key={indicator.name} className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div 
                className="w-3 h-3 rounded-full" 
                style={{ backgroundColor: indicator.color }}
              />
              <span className="text-sm text-slate-300">{indicator.name}</span>
              <span className={`text-xs px-1.5 py-0.5 rounded ${
                indicator.signal === 'BUY' ? 'bg-green-500/20 text-green-400' :
                indicator.signal === 'SELL' ? 'bg-red-500/20 text-red-400' :
                'bg-slate-500/20 text-slate-400'
              }`}>
                {indicator.signal}
              </span>
            </div>
            
            <div className="flex items-center space-x-2">
              <span className="text-sm font-mono text-white">
                {indicator.value.toFixed(2)}
              </span>
              <button
                onClick={() => onToggleIndicator(indicator.name)}
                className={`w-4 h-4 rounded border-2 transition-colors ${
                  indicator.visible 
                    ? 'bg-purple-500 border-purple-500' 
                    : 'border-slate-500'
                }`}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

const PriceInfo: React.FC<{
  data: ChartData[]
  currentIndex: number
}> = ({ data, currentIndex }) => {
  const current = data[currentIndex] || data[data.length - 1]
  const previous = data[currentIndex - 1] || data[data.length - 2]
  
  if (!current) return null
  
  const change = previous ? current.close - previous.close : 0
  const changePercent = previous ? (change / previous.close) * 100 : 0
  const isPositive = change >= 0
  
  return (
    <div className="bg-slate-800/30 rounded-lg p-3">
      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <div className="text-slate-400">Open</div>
          <div className="font-mono text-white">₹{current.open.toFixed(2)}</div>
        </div>
        <div>
          <div className="text-slate-400">High</div>
          <div className="font-mono text-green-400">₹{current.high.toFixed(2)}</div>
        </div>
        <div>
          <div className="text-slate-400">Low</div>
          <div className="font-mono text-red-400">₹{current.low.toFixed(2)}</div>
        </div>
        <div>
          <div className="text-slate-400">Close</div>
          <div className={`font-mono font-semibold ${
            isPositive ? 'text-green-400' : 'text-red-400'
          }`}>
            ₹{current.close.toFixed(2)}
            <span className="text-xs ml-2">
              {isPositive ? '+' : ''}₹{change.toFixed(2)} ({changePercent.toFixed(2)}%)
            </span>
          </div>
        </div>
        <div className="col-span-2">
          <div className="text-slate-400">Volume</div>
          <div className="font-mono text-cyan-400">
            {(current.volume / 1000).toFixed(1)}K
          </div>
        </div>
      </div>
    </div>
  )
}

const ChartCanvas: React.FC<{
  data: ChartData[]
  chartType: string
  indicators: ChartIndicator[]
  showVolume: boolean
  showGrid: boolean
  width: number
  height: number
  onDataPointHover: (index: number) => void
}> = ({ data, chartType, indicators, showVolume, showGrid, width, height, onDataPointHover }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [hoveredIndex, setHoveredIndex] = useState<number>(-1)
  
  // Calculate price range and scaling
  const priceRange = useMemo(() => {
    if (!data.length) return { min: 0, max: 100 }
    
    const prices = data.flatMap(d => [d.high, d.low, d.open, d.close])
    const min = Math.min(...prices)
    const max = Math.max(...prices)
    const padding = (max - min) * 0.1
    
    return { 
      min: min - padding, 
      max: max + padding 
    }
  }, [data])
  
  // Convert price to Y coordinate
  const priceToY = useCallback((price: number) => {
    const chartHeight = showVolume ? height * 0.7 : height - 40
    return 20 + (priceRange.max - price) / (priceRange.max - priceRange.min) * chartHeight
  }, [priceRange, height, showVolume])
  
  // Draw candlestick
  const drawCandlestick = useCallback((ctx: CanvasRenderingContext2D, candle: ChartData, x: number, candleWidth: number) => {
    const openY = priceToY(candle.open)
    const closeY = priceToY(candle.close)
    const highY = priceToY(candle.high)
    const lowY = priceToY(candle.low)
    
    const isGreen = candle.close >= candle.open
    const bodyColor = isGreen ? '#10b981' : '#ef4444'
    const wickColor = isGreen ? '#059669' : '#dc2626'
    
    // Draw wick
    ctx.strokeStyle = wickColor
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(x + candleWidth / 2, highY)
    ctx.lineTo(x + candleWidth / 2, lowY)
    ctx.stroke()
    
    // Draw body
    ctx.fillStyle = bodyColor
    const bodyHeight = Math.abs(closeY - openY)
    const bodyY = Math.min(openY, closeY)
    
    if (bodyHeight < 1) {
      // Doji - draw line
      ctx.fillRect(x, bodyY, candleWidth, 1)
    } else {
      ctx.fillRect(x + 1, bodyY, candleWidth - 2, bodyHeight)
    }
  }, [priceToY])
  
  // Draw line chart
  const drawLineChart = useCallback((ctx: CanvasRenderingContext2D) => {
    if (data.length < 2) return
    
    const candleWidth = (width - 40) / data.length
    
    ctx.strokeStyle = '#8b5cf6'
    ctx.lineWidth = 2
    ctx.beginPath()
    
    data.forEach((candle, index) => {
      const x = 20 + index * candleWidth + candleWidth / 2
      const y = priceToY(candle.close)
      
      if (index === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    })
    
    ctx.stroke()
    
    // Draw area fill if area chart
    if (chartType === 'area') {
      ctx.lineTo(20 + (data.length - 1) * candleWidth + candleWidth / 2, priceToY(priceRange.min))
      ctx.lineTo(20 + candleWidth / 2, priceToY(priceRange.min))
      ctx.closePath()
      
      const gradient = ctx.createLinearGradient(0, 0, 0, height)
      gradient.addColorStop(0, 'rgba(139, 92, 246, 0.3)')
      gradient.addColorStop(1, 'rgba(139, 92, 246, 0.05)')
      
      ctx.fillStyle = gradient
      ctx.fill()
    }
  }, [data, width, priceToY, priceRange.min, chartType, height])
  
  // Draw grid
  const drawGrid = useCallback((ctx: CanvasRenderingContext2D) => {
    if (!showGrid) return
    
    ctx.strokeStyle = 'rgba(100, 116, 139, 0.2)'
    ctx.lineWidth = 1
    
    const chartHeight = showVolume ? height * 0.7 : height - 40
    
    // Horizontal grid lines (price levels)
    for (let i = 0; i <= 10; i++) {
      const y = 20 + (chartHeight / 10) * i
      ctx.beginPath()
      ctx.moveTo(20, y)
      ctx.lineTo(width - 20, y)
      ctx.stroke()
    }
    
    // Vertical grid lines (time)
    const candleWidth = (width - 40) / data.length
    const step = Math.max(1, Math.floor(data.length / 10))
    
    for (let i = 0; i < data.length; i += step) {
      const x = 20 + i * candleWidth
      ctx.beginPath()
      ctx.moveTo(x, 20)
      ctx.lineTo(x, height - 20)
      ctx.stroke()
    }
  }, [showGrid, width, height, data.length, showVolume])
  
  // Main draw function
  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas || !data.length) return
    
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    // Clear canvas
    ctx.clearRect(0, 0, width, height)
    
    // Set canvas size
    canvas.width = width
    canvas.height = height
    
    // Draw grid first
    drawGrid(ctx)
    
    const candleWidth = Math.max(1, (width - 40) / data.length - 1)
    
    if (chartType === 'candlestick' || chartType === 'ohlc') {
      data.forEach((candle, index) => {
        const x = 20 + index * (candleWidth + 1)
        drawCandlestick(ctx, candle, x, candleWidth)
      })
    } else {
      drawLineChart(ctx)
    }
    
    // Draw crosshair if hovering
    if (hoveredIndex >= 0) {
      const x = 20 + hoveredIndex * (candleWidth + 1) + candleWidth / 2
      const candle = data[hoveredIndex]
      const y = priceToY(candle.close)
      
      ctx.strokeStyle = 'rgba(139, 92, 246, 0.8)'
      ctx.lineWidth = 1
      ctx.setLineDash([5, 5])
      
      // Vertical line
      ctx.beginPath()
      ctx.moveTo(x, 20)
      ctx.lineTo(x, height - 20)
      ctx.stroke()
      
      // Horizontal line
      ctx.beginPath()
      ctx.moveTo(20, y)
      ctx.lineTo(width - 20, y)
      ctx.stroke()
      
      ctx.setLineDash([])
    }
  }, [data, width, height, chartType, drawGrid, drawCandlestick, drawLineChart, hoveredIndex, priceToY])
  
  // Handle mouse move for crosshair
  const handleMouseMove = useCallback((event: React.MouseEvent) => {
    const canvas = canvasRef.current
    if (!canvas || !data.length) return
    
    const rect = canvas.getBoundingClientRect()
    const x = event.clientX - rect.left
    const candleWidth = (width - 40) / data.length
    const index = Math.floor((x - 20) / candleWidth)
    
    if (index >= 0 && index < data.length) {
      setHoveredIndex(index)
      onDataPointHover(index)
    }
  }, [data.length, width, onDataPointHover])
  
  const handleMouseLeave = useCallback(() => {
    setHoveredIndex(-1)
    onDataPointHover(data.length - 1)
  }, [onDataPointHover, data.length])
  
  useEffect(() => {
    draw()
  }, [draw])
  
  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      className="cursor-crosshair"
      style={{ width, height }}
    />
  )
}

export const AdvancedChart: React.FC<AdvancedChartProps> = ({
  symbol,
  data,
  chartType = 'candlestick',
  timeframe = '15m',
  indicators = [],
  showVolume = true,
  showGrid = true,
  theme = 'dark',
  height = 500,
  fullscreen = false,
  onFullscreenToggle,
  className = ''
}) => {
  const [currentChartType, setCurrentChartType] = useState(chartType)
  const [currentTimeframe, setCurrentTimeframe] = useState(timeframe)
  const [currentIndicators, setCurrentIndicators] = useState<ChartIndicator[]>(
    indicators.map(ind => ({ ...ind, visible: true, settings: {} }))
  )
  const [showIndicatorPanel, setShowIndicatorPanel] = useState(false)
  const [hoveredDataIndex, setHoveredDataIndex] = useState(data.length - 1)
  const [chartSettings, setChartSettings] = useState({
    showVolume,
    showGrid,
    theme
  })
  
  const containerRef = useRef<HTMLDivElement>(null)
  const [containerSize, setContainerSize] = useState({ width: 800, height })
  
  // Update container size
  useEffect(() => {
    const updateSize = () => {
      if (containerRef.current) {
        const { offsetWidth } = containerRef.current
        setContainerSize({ width: offsetWidth - 32, height })
      }
    }
    
    updateSize()
    window.addEventListener('resize', updateSize)
    
    return () => window.removeEventListener('resize', updateSize)
  }, [height])
  
  // Handle indicator toggle
  const handleToggleIndicator = useCallback((name: string) => {
    setCurrentIndicators(prev => prev.map(ind => 
      ind.name === name ? { ...ind, visible: !ind.visible } : ind
    ))
  }, [])
  
  // Handle add indicator
  const handleAddIndicator = useCallback(() => {
    // In real implementation, this would open a modal to select indicators
    console.log('Add indicator modal would open here')
  }, [])
  
  const chartHeight = fullscreen ? containerSize.height - 120 : containerSize.height - 60
  
  return (
    <motion.div
      ref={containerRef}
      className={`glass-card rounded-2xl overflow-hidden ${className} ${
        fullscreen ? 'fixed inset-4 z-50' : ''
      }`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Toolbar */}
      <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-purple-400" />
            <h3 className="text-lg font-bold text-white">{symbol}</h3>
            <span className="text-sm text-slate-400">
              {timeframeLabels[currentTimeframe]}
            </span>
          </div>
          
          <TimeframeSelector
            current={currentTimeframe}
            onChange={setCurrentTimeframe}
          />
        </div>
        
        <div className="flex items-center space-x-2">
          <ChartTypeSelector
            current={currentChartType}
            onChange={setCurrentChartType}
          />
          
          <div className="flex items-center space-x-1">
            <ToolbarButton
              icon={<Grid3X3 className="w-4 h-4" />}
              label="Toggle Grid"
              active={chartSettings.showGrid}
              onClick={() => setChartSettings(prev => ({ ...prev, showGrid: !prev.showGrid }))}
            />
            
            <ToolbarButton
              icon={<BarChart3 className="w-4 h-4" />}
              label="Toggle Volume"
              active={chartSettings.showVolume}
              onClick={() => setChartSettings(prev => ({ ...prev, showVolume: !prev.showVolume }))}
            />
            
            <ToolbarButton
              icon={<Target className="w-4 h-4" />}
              label="Indicators"
              active={showIndicatorPanel}
              onClick={() => setShowIndicatorPanel(!showIndicatorPanel)}
            />
            
            <ToolbarButton
              icon={<Download className="w-4 h-4" />}
              label="Export Chart"
              onClick={() => console.log('Export chart')}
            />
            
            <ToolbarButton
              icon={<Share2 className="w-4 h-4" />}
              label="Share Chart"
              onClick={() => console.log('Share chart')}
            />
            
            {onFullscreenToggle && (
              <ToolbarButton
                icon={fullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
                label={fullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
                onClick={onFullscreenToggle}
              />
            )}
          </div>
        </div>
      </div>
      
      {/* Chart Content */}
      <div className="flex">
        {/* Main Chart Area */}
        <div className="flex-1 relative">
          {data.length > 0 ? (
            <ChartCanvas
              data={data}
              chartType={currentChartType}
              indicators={currentIndicators}
              showVolume={chartSettings.showVolume}
              showGrid={chartSettings.showGrid}
              width={containerSize.width - (showIndicatorPanel ? 300 : 0)}
              height={chartHeight}
              onDataPointHover={setHoveredDataIndex}
            />
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400">
              <div className="text-center">
                <BarChart3 className="w-16 h-16 mx-auto mb-4 opacity-50" />
                <p>No chart data available</p>
                <p className="text-sm">Waiting for market data...</p>
              </div>
            </div>
          )}
        </div>
        
        {/* Indicator Panel */}
        <AnimatePresence>
          {showIndicatorPanel && (
            <motion.div
              className="w-80 border-l border-slate-700/50 p-4 space-y-4"
              initial={{ opacity: 0, x: 300 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 300 }}
              transition={{ duration: 0.3 }}
            >
              <PriceInfo
                data={data}
                currentIndex={hoveredDataIndex}
              />
              
              <IndicatorPanel
                indicators={currentIndicators}
                onToggleIndicator={handleToggleIndicator}
                onAddIndicator={handleAddIndicator}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  )
}

export default AdvancedChart