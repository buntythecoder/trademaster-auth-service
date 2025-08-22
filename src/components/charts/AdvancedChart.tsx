import React, { useState, useEffect, useRef } from 'react'
import { TrendingUp, TrendingDown, BarChart3, LineChart, BarChart3 as Candlestick, Volume, ZoomIn, ZoomOut, Move, Crosshair, Settings, Download, Maximize, Minimize, Info, Target } from 'lucide-react'

interface ChartDataPoint {
  timestamp: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

interface TechnicalIndicator {
  id: string
  name: string
  enabled: boolean
  color: string
  values?: number[]
}

interface MarketInfo {
  currentPrice: number
  change: number
  changePercent: number
  dayHigh: number
  dayLow: number
  volume: number
  marketCap: string
  pe: number
  yield: number
}

interface ChartSettings {
  chartType: 'candlestick' | 'line' | 'area' | 'ohlc'
  timeframe: '1M' | '5M' | '15M' | '1H' | '4H' | '1D' | '1W' | '1MO'
  indicators: TechnicalIndicator[]
  showVolume: boolean
  showGrid: boolean
  crosshair: boolean
}

interface AdvancedChartProps {
  symbol: string
  height?: number
  data?: ChartDataPoint[]
  onDataRequest?: (symbol: string, timeframe: string) => void
}

const mockData: ChartDataPoint[] = generateMockData()

function generateMockData(): ChartDataPoint[] {
  const data: ChartDataPoint[] = []
  let basePrice = 2547.30
  const now = new Date()
  
  for (let i = 200; i >= 0; i--) {
    const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000).toISOString()
    
    // Generate realistic OHLC data with better patterns
    const volatility = 0.015 + Math.random() * 0.01
    const trend = Math.sin(i * 0.05) * 0.002 + Math.cos(i * 0.02) * 0.001
    const momentum = Math.sin(i * 0.3) * 0.005
    
    const open = basePrice
    const randomChange = (Math.random() - 0.5) * volatility * basePrice
    const trendChange = trend * basePrice
    const momentumChange = momentum * basePrice
    
    const close = Math.max(open + randomChange + trendChange + momentumChange, 100)
    
    const spreadPercent = 0.003 + Math.random() * 0.007
    const high = Math.max(open, close) + Math.random() * spreadPercent * basePrice
    const low = Math.min(open, close) - Math.random() * spreadPercent * basePrice
    
    const volume = Math.floor(Math.random() * 2000000 + 500000)
    
    data.push({
      timestamp,
      open: parseFloat(open.toFixed(2)),
      high: parseFloat(high.toFixed(2)),
      low: parseFloat(low.toFixed(2)),
      close: parseFloat(close.toFixed(2)),
      volume
    })
    
    basePrice = close * (1 + (Math.random() - 0.5) * 0.001)
  }
  
  return data
}

const mockMarketInfo: MarketInfo = {
  currentPrice: 2547.30,
  change: 23.45,
  changePercent: 0.93,
  dayHigh: 2565.80,
  dayLow: 2521.15,
  volume: 24567890,
  marketCap: "₹16.2L Cr",
  pe: 28.4,
  yield: 0.35
}

export function AdvancedChart({ symbol, height = 500, data = mockData, onDataRequest }: AdvancedChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  
  const [settings, setSettings] = useState<ChartSettings>({
    chartType: 'candlestick',
    timeframe: '1H',
    indicators: [
      { id: 'sma20', name: 'SMA (20)', enabled: false, color: '#10b981' },
      { id: 'sma50', name: 'SMA (50)', enabled: false, color: '#f59e0b' },
      { id: 'rsi', name: 'RSI (14)', enabled: false, color: '#8b5cf6' },
      { id: 'macd', name: 'MACD', enabled: false, color: '#06b6d4' },
      { id: 'bb', name: 'Bollinger Bands', enabled: false, color: '#ec4899' }
    ],
    showVolume: true,
    showGrid: true,
    crosshair: true
  })
  
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [showMarketInfo, setShowMarketInfo] = useState(true)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })
  const [hoveredCandle, setHoveredCandle] = useState<ChartDataPoint | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [marketInfo, setMarketInfo] = useState<MarketInfo>(mockMarketInfo)

  // Chart dimensions and scaling
  const [chartDimensions, setChartDimensions] = useState({
    width: 800,
    height: height - 100, // Account for controls
    padding: { top: 20, right: 80, bottom: 60, left: 60 }
  })

  const drawChart = () => {
    const canvas = canvasRef.current
    const container = containerRef.current
    
    if (!canvas || !container || !data.length) {
      console.log('Chart render skipped:', { canvas: !!canvas, container: !!container, dataLength: data.length })
      return
    }

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      console.error('Failed to get canvas context')
      return
    }

    try {

      // Set canvas size
      const rect = container.getBoundingClientRect()
      if (rect && rect.width > 50 && rect.height > 50) {
        const canvasWidth = Math.max(rect.width - 32, 400) // Account for padding, minimum width
        const canvasHeight = Math.max(height - 100, 300) // Account for controls, minimum height
        
        canvas.width = canvasWidth * window.devicePixelRatio
        canvas.height = canvasHeight * window.devicePixelRatio
        canvas.style.width = canvasWidth + 'px'
        canvas.style.height = canvasHeight + 'px'
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio)
        
        console.log('Chart rendering:', { canvasWidth, canvasHeight, dataPoints: data.length })
      } else {
        console.log('Container not ready for rendering:', { rect })
        return
      }
    } catch (error) {
      console.error('Error in drawChart setup:', error)
      return
    }

    const { width: chartWidth, height: chartHeight, padding } = {
      width: Math.max((containerRef.current?.getBoundingClientRect()?.width || 800) - 32, 400),
      height: Math.max(height - 100, 300),
      padding: { top: 20, right: 80, bottom: 60, left: 60 }
    }

    // Clear canvas
    ctx.fillStyle = 'rgba(15, 23, 42, 0.95)'
    ctx.fillRect(0, 0, chartWidth, chartHeight)

    // Calculate price range
    const prices = data.flatMap(d => [d.high, d.low])
    const minPrice = Math.min(...prices) * 0.999
    const maxPrice = Math.max(...prices) * 1.001
    const priceRange = maxPrice - minPrice

    // Chart area
    const chartContentWidth = chartWidth - padding.left - padding.right
    const chartContentHeight = settings.showVolume ? chartHeight * 0.7 : chartHeight - padding.top - padding.bottom
    
    // Draw grid
    if (settings.showGrid) {
      ctx.strokeStyle = 'rgba(71, 85, 105, 0.3)'
      ctx.lineWidth = 1
      
      // Horizontal lines
      for (let i = 0; i <= 10; i++) {
        const y = padding.top + (chartHeight / 10) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(padding.left + chartContentWidth, y)
        ctx.stroke()
      }
      
      // Vertical lines
      for (let i = 0; i <= 10; i++) {
        const x = padding.left + (chartContentWidth / 10) * i
        ctx.beginPath()
        ctx.moveTo(x, padding.top)
        ctx.lineTo(x, padding.top + chartContentHeight)
        ctx.stroke()
      }
    }

    // Draw price axis
    ctx.fillStyle = '#94a3b8'
    ctx.font = '11px Inter, system-ui'
    ctx.textAlign = 'left'
    
    for (let i = 0; i <= 8; i++) {
      const price = minPrice + (priceRange / 8) * i
      const y = padding.top + chartContentHeight - (chartContentHeight / 8) * i
      ctx.fillText(`₹${price.toFixed(1)}`, padding.left + chartContentWidth + 8, y + 4)
    }
    
    // Draw time axis
    ctx.textAlign = 'center'
    const timePoints = Math.min(8, data.length)
    for (let i = 0; i < timePoints; i++) {
      const dataIndex = Math.floor((data.length / timePoints) * i)
      if (dataIndex < data.length) {
        const x = padding.left + dataIndex * candleSpacing + candleSpacing / 2
        const time = new Date(data[dataIndex].timestamp)
        const timeStr = time.toLocaleTimeString('en-IN', { 
          hour: '2-digit', 
          minute: '2-digit' 
        })
        ctx.fillText(timeStr, x, padding.top + chartContentHeight + 20)
      }
    }

    // Draw candlesticks or line
    const candleWidth = Math.max(2, chartContentWidth / data.length * 0.8)
    const candleSpacing = chartContentWidth / data.length

    data.forEach((candle, index) => {
      const x = padding.left + index * candleSpacing + candleSpacing / 2
      const openY = padding.top + chartContentHeight - ((candle.open - minPrice) / priceRange) * chartContentHeight
      const closeY = padding.top + chartContentHeight - ((candle.close - minPrice) / priceRange) * chartContentHeight
      const highY = padding.top + chartContentHeight - ((candle.high - minPrice) / priceRange) * chartContentHeight
      const lowY = padding.top + chartContentHeight - ((candle.low - minPrice) / priceRange) * chartContentHeight

      if (settings.chartType === 'candlestick') {
        const isGreen = candle.close > candle.open
        
        // High-Low line
        ctx.strokeStyle = isGreen ? '#10b981' : '#ef4444'
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(x, highY)
        ctx.lineTo(x, lowY)
        ctx.stroke()

        // Candle body
        ctx.fillStyle = isGreen ? '#10b981' : '#ef4444'
        const bodyHeight = Math.abs(closeY - openY)
        const bodyY = Math.min(openY, closeY)
        
        if (bodyHeight < 1) {
          // Doji - draw a line
          ctx.strokeStyle = isGreen ? '#10b981' : '#ef4444'
          ctx.lineWidth = candleWidth
          ctx.beginPath()
          ctx.moveTo(x - candleWidth / 2, bodyY)
          ctx.lineTo(x + candleWidth / 2, bodyY)
          ctx.stroke()
        } else {
          ctx.fillRect(x - candleWidth / 2, bodyY, candleWidth, bodyHeight)
        }
      } else if (settings.chartType === 'line') {
        ctx.strokeStyle = '#8b5cf6'
        ctx.lineWidth = 2
        
        if (index === 0) {
          ctx.beginPath()
          ctx.moveTo(x, closeY)
        } else {
          ctx.lineTo(x, closeY)
          if (index === data.length - 1) {
            ctx.stroke()
          }
        }
      }
    })

    // Draw volume bars if enabled
    if (settings.showVolume) {
      const volumeHeight = chartHeight * 0.25
      const volumeY = chartHeight - padding.bottom - volumeHeight
      const maxVolume = Math.max(...data.map(d => d.volume))

      data.forEach((candle, index) => {
        const x = padding.left + index * candleSpacing + candleSpacing / 2
        const barHeight = (candle.volume / maxVolume) * volumeHeight
        const isGreen = candle.close > candle.open
        
        ctx.fillStyle = isGreen ? 'rgba(16, 185, 129, 0.6)' : 'rgba(239, 68, 68, 0.6)'
        ctx.fillRect(x - candleWidth / 2, volumeY + volumeHeight - barHeight, candleWidth, barHeight)
      })
    }

    // Draw technical indicators
    const activeIndicators: string[] = []
    settings.indicators.forEach(indicator => {
      if (!indicator.enabled) return
      
      activeIndicators.push(indicator.name)
      ctx.strokeStyle = indicator.color
      ctx.lineWidth = 2
      
      if (indicator.id === 'sma20' || indicator.id === 'sma50') {
        const period = indicator.id === 'sma20' ? 20 : 50
        drawSMA(ctx, data, period, minPrice, maxPrice, chartContentHeight, padding, candleSpacing, indicator.color)
      } else if (indicator.id === 'bb') {
        drawBollingerBands(ctx, data, 20, 2, minPrice, maxPrice, chartContentHeight, padding, candleSpacing, indicator.color)
      }
    })
    
    // Draw indicator legend
    if (activeIndicators.length > 0) {
      ctx.font = '10px Inter, system-ui'
      ctx.textAlign = 'left'
      let legendX = padding.left + 10
      let legendY = padding.top + 15
      
      activeIndicators.forEach((name, index) => {
        const indicator = settings.indicators.find(ind => ind.name === name)
        if (indicator) {
          ctx.fillStyle = indicator.color
          ctx.fillRect(legendX, legendY - 8, 12, 2)
          ctx.fillStyle = '#e2e8f0'
          ctx.fillText(name, legendX + 16, legendY)
          legendX += ctx.measureText(name).width + 30
          if (legendX > chartWidth - 100) {
            legendX = padding.left + 10
            legendY += 20
          }
        }
      })
    }

    // Draw crosshair
    if (settings.crosshair && mousePos.x > 0 && mousePos.y > 0) {
      ctx.strokeStyle = 'rgba(139, 92, 246, 0.8)'
      ctx.lineWidth = 1
      ctx.setLineDash([5, 5])
      
      // Vertical line
      ctx.beginPath()
      ctx.moveTo(mousePos.x, padding.top)
      ctx.lineTo(mousePos.x, padding.top + chartContentHeight)
      ctx.stroke()
      
      // Horizontal line
      ctx.beginPath()
      ctx.moveTo(padding.left, mousePos.y)
      ctx.lineTo(padding.left + chartContentWidth, mousePos.y)
      ctx.stroke()
      
      ctx.setLineDash([])
    }
  }

  const drawSMA = (
    ctx: CanvasRenderingContext2D,
    data: ChartDataPoint[],
    period: number,
    minPrice: number,
    maxPrice: number,
    chartContentHeight: number,
    padding: any,
    candleSpacing: number,
    color: string
  ) => {
    if (data.length < period) return

    ctx.strokeStyle = color
    ctx.lineWidth = 2
    ctx.beginPath()

    for (let i = period - 1; i < data.length; i++) {
      const sum = data.slice(i - period + 1, i + 1).reduce((acc, d) => acc + d.close, 0)
      const sma = sum / period
      
      const x = padding.left + i * candleSpacing + candleSpacing / 2
      const y = padding.top + chartContentHeight - ((sma - minPrice) / (maxPrice - minPrice)) * chartContentHeight
      
      if (i === period - 1) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    }
    
    ctx.stroke()
  }

  const drawBollingerBands = (
    ctx: CanvasRenderingContext2D,
    data: ChartDataPoint[],
    period: number,
    multiplier: number,
    minPrice: number,
    maxPrice: number,
    chartContentHeight: number,
    padding: any,
    candleSpacing: number,
    color: string
  ) => {
    if (data.length < period) return

    const upperBand: number[] = []
    const lowerBand: number[] = []
    const smaValues: number[] = []

    for (let i = period - 1; i < data.length; i++) {
      const slice = data.slice(i - period + 1, i + 1)
      const sum = slice.reduce((acc, d) => acc + d.close, 0)
      const sma = sum / period
      
      const variance = slice.reduce((acc, d) => acc + Math.pow(d.close - sma, 2), 0) / period
      const stdDev = Math.sqrt(variance)
      
      smaValues.push(sma)
      upperBand.push(sma + multiplier * stdDev)
      lowerBand.push(sma - multiplier * stdDev)
    }

    // Draw upper band
    ctx.strokeStyle = color + '80'
    ctx.lineWidth = 1
    ctx.beginPath()
    upperBand.forEach((value, index) => {
      const x = padding.left + (index + period - 1) * candleSpacing + candleSpacing / 2
      const y = padding.top + chartContentHeight - ((value - minPrice) / (maxPrice - minPrice)) * chartContentHeight
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.stroke()

    // Draw lower band
    ctx.beginPath()
    lowerBand.forEach((value, index) => {
      const x = padding.left + (index + period - 1) * candleSpacing + candleSpacing / 2
      const y = padding.top + chartContentHeight - ((value - minPrice) / (maxPrice - minPrice)) * chartContentHeight
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.stroke()

    // Fill between bands
    ctx.fillStyle = color + '10'
    ctx.beginPath()
    upperBand.forEach((value, index) => {
      const x = padding.left + (index + period - 1) * candleSpacing + candleSpacing / 2
      const y = padding.top + chartContentHeight - ((value - minPrice) / (maxPrice - minPrice)) * chartContentHeight
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    for (let i = lowerBand.length - 1; i >= 0; i--) {
      const value = lowerBand[i]
      const x = padding.left + (i + period - 1) * candleSpacing + candleSpacing / 2
      const y = padding.top + chartContentHeight - ((value - minPrice) / (maxPrice - minPrice)) * chartContentHeight
      ctx.lineTo(x, y)
    }
    ctx.closePath()
    ctx.fill()
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect()
    if (!rect) return
    
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    setMousePos({ x, y })

    // Find hovered candle
    const candleSpacing = (rect.width - 140) / data.length
    const candleIndex = Math.floor((x - 60) / candleSpacing)
    
    if (candleIndex >= 0 && candleIndex < data.length) {
      setHoveredCandle(data[candleIndex])
    }
  }

  const handleTimeframeChange = (newTimeframe: string) => {
    setSettings(prev => ({ ...prev, timeframe: newTimeframe as any }))
    setIsLoading(true)
    onDataRequest?.(symbol, newTimeframe)
    // Simulate loading
    setTimeout(() => setIsLoading(false), 1000)
  }

  const toggleIndicator = (indicatorId: string) => {
    setSettings(prev => ({
      ...prev,
      indicators: prev.indicators.map(ind => 
        ind.id === indicatorId ? { ...ind, enabled: !ind.enabled } : ind
      )
    }))
  }

  const exportChart = () => {
    const canvas = canvasRef.current
    if (!canvas) return
    
    const link = document.createElement('a')
    link.download = `${symbol}-chart-${settings.timeframe}.png`
    link.href = canvas.toDataURL()
    link.click()
  }

  // Initialize chart after mount
  useEffect(() => {
    const initChart = () => {
      setTimeout(() => {
        if (canvasRef.current && containerRef.current) {
          console.log('Initializing chart...')
          drawChart()
        }
      }, 100)
    }
    
    initChart()
    
    // Update market info with latest data
    if (data.length > 0) {
      const latestCandle = data[data.length - 1]
      const previousCandle = data[data.length - 2]
      if (latestCandle && previousCandle) {
        const change = latestCandle.close - previousCandle.close
        const changePercent = (change / previousCandle.close) * 100
        setMarketInfo(prev => ({
          ...prev,
          currentPrice: latestCandle.close,
          change,
          changePercent
        }))
      }
    }
  }, [])

  // Redraw on data/settings changes
  useEffect(() => {
    if (canvasRef.current && containerRef.current) {
      drawChart()
    }
  }, [data, settings])

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      setTimeout(() => {
        if (canvasRef.current && containerRef.current) {
          drawChart()
        }
      }, 100)
    }
    
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return (
    <div ref={containerRef} className={`glass-card rounded-2xl p-4 ${isFullscreen ? 'fixed inset-0 z-50' : ''}`}>
      {/* Market Info Panel */}
      {showMarketInfo && (
        <div className="mb-6 p-4 rounded-xl bg-gradient-to-r from-slate-800/50 to-slate-700/30 border border-slate-600/50">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20">
                <Target className="w-6 h-6 text-purple-400" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-white">{symbol}</h3>
                <div className="flex items-center space-x-2">
                  <span className="text-2xl font-bold text-white">₹{marketInfo.currentPrice.toFixed(2)}</span>
                  <div className={`flex items-center space-x-1 px-2 py-1 rounded-lg ${
                    marketInfo.change >= 0 
                      ? 'bg-green-500/20 text-green-400' 
                      : 'bg-red-500/20 text-red-400'
                  }`}>
                    {marketInfo.change >= 0 ? (
                      <TrendingUp className="w-4 h-4" />
                    ) : (
                      <TrendingDown className="w-4 h-4" />
                    )}
                    <span className="text-sm font-semibold">
                      {marketInfo.change >= 0 ? '+' : ''}₹{marketInfo.change.toFixed(2)} ({marketInfo.changePercent.toFixed(2)}%)
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <button
              onClick={() => setShowMarketInfo(false)}
              className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
              title="Hide market info"
            >
              <Minimize className="w-4 h-4" />
            </button>
          </div>
          
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
            <div className="text-center">
              <div className="text-xs text-slate-400">Day High</div>
              <div className="text-sm font-semibold text-green-400">₹{marketInfo.dayHigh.toFixed(2)}</div>
            </div>
            <div className="text-center">
              <div className="text-xs text-slate-400">Day Low</div>
              <div className="text-sm font-semibold text-red-400">₹{marketInfo.dayLow.toFixed(2)}</div>
            </div>
            <div className="text-center">
              <div className="text-xs text-slate-400">Volume</div>
              <div className="text-sm font-semibold text-cyan-400">{(marketInfo.volume / 1000000).toFixed(1)}M</div>
            </div>
            <div className="text-center">
              <div className="text-xs text-slate-400">Market Cap</div>
              <div className="text-sm font-semibold text-purple-400">{marketInfo.marketCap}</div>
            </div>
            <div className="text-center">
              <div className="text-xs text-slate-400">P/E Ratio</div>
              <div className="text-sm font-semibold text-orange-400">{marketInfo.pe.toFixed(1)}</div>
            </div>
            <div className="text-center">
              <div className="text-xs text-slate-400">Div Yield</div>
              <div className="text-sm font-semibold text-yellow-400">{marketInfo.yield.toFixed(2)}%</div>
            </div>
          </div>
        </div>
      )}
      
      {!showMarketInfo && (
        <div className="mb-4 flex justify-end">
          <button
            onClick={() => setShowMarketInfo(true)}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
            title="Show market info"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Chart Controls */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-4">
          {/* Chart Type */}
          <div className="flex items-center space-x-2">
            {[
              { type: 'candlestick', icon: BarChart3, label: 'Candlestick' },
              { type: 'line', icon: LineChart, label: 'Line' },
              { type: 'area', icon: TrendingUp, label: 'Area' }
            ].map(({ type, icon: Icon, label }) => (
              <button
                key={type}
                onClick={() => setSettings(prev => ({ ...prev, chartType: type as any }))}
                className={`p-2 rounded-xl transition-colors ${
                  settings.chartType === type
                    ? 'bg-purple-500/20 text-purple-400'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
                title={label}
              >
                <Icon className="w-4 h-4" />
              </button>
            ))}
          </div>

          {/* Timeframe */}
          <div className="flex items-center space-x-1">
            {['1M', '5M', '15M', '1H', '4H', '1D', '1W', '1MO'].map(tf => (
              <button
                key={tf}
                onClick={() => handleTimeframeChange(tf)}
                className={`px-2 py-1 text-sm rounded-lg transition-colors ${
                  settings.timeframe === tf
                    ? 'bg-cyan-500/20 text-cyan-400'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                {tf}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setSettings(prev => ({ ...prev, showVolume: !prev.showVolume }))}
            className={`p-2 rounded-xl transition-colors ${
              settings.showVolume
                ? 'bg-green-500/20 text-green-400'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
            title="Toggle Volume"
          >
            <Volume className="w-4 h-4" />
          </button>
          
          <button
            onClick={() => setShowSettings(!showSettings)}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors"
            title="Chart Settings"
          >
            <Settings className="w-4 h-4" />
          </button>
          
          <button
            onClick={exportChart}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors"
            title="Export Chart"
          >
            <Download className="w-4 h-4" />
          </button>
          
          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors"
            title="Toggle Fullscreen"
          >
            {isFullscreen ? <Minimize className="w-4 h-4" /> : <Maximize className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Settings Panel */}
      {showSettings && (
        <div className="mb-4 p-4 rounded-xl bg-slate-800/30 border border-purple-500/30">
          <h4 className="font-semibold text-white mb-3">Technical Indicators</h4>
          <div className="grid gap-2 md:grid-cols-3">
            {settings.indicators.map(indicator => (
              <label key={indicator.id} className="flex items-center space-x-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={indicator.enabled}
                  onChange={() => toggleIndicator(indicator.id)}
                  className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
                />
                <span className="text-sm text-slate-300 flex items-center">
                  <div 
                    className="w-3 h-3 rounded mr-2" 
                    style={{ backgroundColor: indicator.color }}
                  />
                  {indicator.name}
                </span>
              </label>
            ))}
          </div>
          
          <div className="flex items-center space-x-4 mt-4 pt-3 border-t border-slate-700/50">
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                checked={settings.showGrid}
                onChange={(e) => setSettings(prev => ({ ...prev, showGrid: e.target.checked }))}
                className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
              />
              <span className="text-sm text-slate-300">Show Grid</span>
            </label>
            
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                checked={settings.crosshair}
                onChange={(e) => setSettings(prev => ({ ...prev, crosshair: e.target.checked }))}
                className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
              />
              <span className="text-sm text-slate-300">Crosshair</span>
            </label>
          </div>
        </div>
      )}

      {/* Chart Container */}
      <div 
        ref={containerRef}
        className="relative bg-slate-900/50 rounded-xl overflow-hidden"
        style={{ height: isFullscreen ? 'calc(100vh - 120px)' : height }}
      >
        {isLoading && (
          <div className="absolute inset-0 bg-slate-900/80 flex items-center justify-center z-10">
            <div className="flex items-center space-x-3 text-purple-400">
              <div className="loading-dots">
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
                <div className="loading-dot"></div>
              </div>
              <span>Loading chart data...</span>
            </div>
          </div>
        )}
        
        <canvas
          ref={canvasRef}
          className="w-full h-full cursor-crosshair"
          onMouseMove={handleMouseMove}
          onMouseLeave={() => setMousePos({ x: 0, y: 0 })}
        />
        
        {/* Price Info Tooltip */}
        {hoveredCandle && mousePos.x > 0 && (
          <div 
            className="absolute z-20 glass-card p-3 rounded-xl border border-purple-500/30 pointer-events-none"
            style={{ 
              left: Math.min(mousePos.x + 10, (containerRef.current?.clientWidth || 0) - 200),
              top: Math.max(mousePos.y - 10, 10)
            }}
          >
            <div className="text-xs font-mono space-y-1">
              <div className="text-slate-400">
                {new Date(hoveredCandle.timestamp).toLocaleString('en-IN', {
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit'
                })}
              </div>
              <div className="grid grid-cols-2 gap-x-3 text-white">
                <div>O: ₹{hoveredCandle.open.toFixed(2)}</div>
                <div>H: ₹{hoveredCandle.high.toFixed(2)}</div>
                <div>L: ₹{hoveredCandle.low.toFixed(2)}</div>
                <div>C: ₹{hoveredCandle.close.toFixed(2)}</div>
              </div>
              <div className="text-slate-400 pt-1 border-t border-slate-700">
                Vol: {hoveredCandle.volume.toLocaleString()}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}