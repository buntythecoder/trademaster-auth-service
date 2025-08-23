import React, { useState, useEffect, useRef } from 'react'
import { TrendingUp, TrendingDown, BarChart3, LineChart, BarChart3 as Candlestick, Volume, Volume2, ZoomIn, ZoomOut, Move, Crosshair, Settings, Download, Maximize, Minimize, Info, Target, Activity, Expand, Layers, X, Monitor } from 'lucide-react'

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

interface ChartSettings {
  chartType: 'candlestick' | 'line' | 'area' | 'ohlc' | 'heikin-ashi' | 'renko' | 'volume-profile' | 'mountain' | 'point-figure' | 'kagi' | 'three-line-break' | 'footprint' | 'range-bars'
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
  initialChartType?: 'candlestick' | 'line' | 'area' | 'ohlc' | 'heikin-ashi' | 'renko' | 'volume-profile' | 'mountain' | 'point-figure' | 'kagi' | 'three-line-break' | 'footprint' | 'range-bars'
  mode?: 'simple' | 'advanced'  // simple: no fullscreen/zoom controls, advanced: full functionality
  hideFitToScreen?: boolean  // hide fit to screen button specifically
}

const mockData: ChartDataPoint[] = generateMockData()

function generateMockData(): ChartDataPoint[] {
  const data: ChartDataPoint[] = []
  let basePrice = 2547.30
  const now = new Date()
  
  for (let i = 200; i >= 0; i--) {
    const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000).toISOString()
    
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

export function AdvancedChart({ symbol, height = 500, data = mockData, onDataRequest, initialChartType = 'candlestick', mode = 'advanced', hideFitToScreen = false }: AdvancedChartProps) {
  console.log('🚀 AdvancedChart component mounted', { symbol, height, dataLength: data.length })
  
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const chartContainerRef = useRef<HTMLDivElement>(null)
  
  const [settings, setSettings] = useState<ChartSettings>({
    chartType: initialChartType,
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
  
  const [showChartTypes, setShowChartTypes] = useState(false)
  const [zoomLevel, setZoomLevel] = useState(1)
  const [panOffset, setPanOffset] = useState(0)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })
  const [hoveredCandle, setHoveredCandle] = useState<ChartDataPoint | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const [lastPanOffset, setLastPanOffset] = useState(0)

  // Zoom functionality
  const zoomIn = () => {
    setZoomLevel(prev => Math.min(prev * 1.5, 10))
    console.log('🔍 Zooming in, level:', zoomLevel)
  }

  const zoomOut = () => {
    setZoomLevel(prev => Math.max(prev / 1.5, 0.1))
    console.log('🔍 Zooming out, level:', zoomLevel)
  }

  const fitToScreen = () => {
    setZoomLevel(1)
    setPanOffset(0)
    console.log('📐 Fit to screen')
  }

  const handleMouseWheel = (e: React.WheelEvent<HTMLCanvasElement>) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? 0.9 : 1.1
    setZoomLevel(prev => Math.min(Math.max(prev * delta, 0.1), 10))
  }

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (isDragging) return // Don't zoom if we just finished dragging
    
    const rect = e.currentTarget.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    
    if (e.shiftKey) {
      setZoomLevel(prev => {
        const newZoom = Math.max(prev * 0.8, 0.1)
        console.log('🔍 Shift+click zoom out:', prev, '->', newZoom)
        return newZoom
      })
    } else {
      setZoomLevel(prev => {
        const newZoom = Math.min(prev * 1.2, 10)
        console.log('🔍 Click zoom in:', prev, '->', newZoom)
        return newZoom
      })
    }
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect()
    if (!rect) return
    
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    setMousePos({ x, y })

    // Handle dragging for panning
    if (isDragging) {
      const deltaX = x - dragStart.x
      const panSensitivity = 2 // Adjust sensitivity as needed
      const newPanOffset = lastPanOffset + (deltaX / panSensitivity)
      setPanOffset(Math.max(Math.min(newPanOffset, data.length * 0.8), -data.length * 0.2))
      console.log('🖱️ Dragging - Pan offset:', newPanOffset)
      return
    }

    // Only show hover info if not dragging
    const candleSpacing = (rect.width - 140) / data.length
    const candleIndex = Math.floor((x - 60) / candleSpacing)
    
    if (candleIndex >= 0 && candleIndex < data.length) {
      setHoveredCandle(data[candleIndex])
    }
  }

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    
    setIsDragging(true)
    setDragStart({ x, y })
    setLastPanOffset(panOffset)
    console.log('🖱️ Mouse down - Start dragging at:', { x, y })
  }

  const handleMouseUp = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (isDragging) {
      console.log('🖱️ Mouse up - End dragging')
      setIsDragging(false)
      setDragStart({ x: 0, y: 0 })
    }
  }

  const toggleIndicator = (indicatorId: string) => {
    setSettings(prev => ({
      ...prev,
      indicators: prev.indicators.map(ind => 
        ind.id === indicatorId ? { ...ind, enabled: !ind.enabled } : ind
      )
    }))
  }

  const drawChart = () => {
    const canvas = canvasRef.current
    const container = containerRef.current || chartContainerRef.current
    
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
      const rect = container.getBoundingClientRect()
      if (rect && rect.width > 50 && rect.height > 50) {
        const canvasWidth = Math.max(rect.width - 32, 400)
        const canvasHeight = Math.max(height - 100, 300)
        
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
      width: Math.max((container?.getBoundingClientRect()?.width || 800) - 32, 400),
      height: Math.max(height - 100, 300),
      padding: { top: 20, right: 80, bottom: 60, left: 60 }
    }

    // Clear canvas with theme-consistent background
    ctx.fillStyle = 'rgba(30, 27, 75, 0.6)' // Matches glass-card theme
    ctx.fillRect(0, 0, chartWidth, chartHeight)

    // Calculate price range
    const prices = data.flatMap(d => [d.high, d.low])
    const minPrice = Math.min(...prices) * 0.999
    const maxPrice = Math.max(...prices) * 1.001
    const priceRange = maxPrice - minPrice

    const chartContentWidth = chartWidth - padding.left - padding.right
    const chartContentHeight = settings.showVolume ? chartHeight * 0.7 : chartHeight - padding.top - padding.bottom
    
    // Draw grid
    if (settings.showGrid) {
      ctx.strokeStyle = 'rgba(71, 85, 105, 0.3)'
      ctx.lineWidth = 1
      
      for (let i = 0; i <= 10; i++) {
        const y = padding.top + (chartHeight / 10) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(padding.left + chartContentWidth, y)
        ctx.stroke()
      }
      
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
    
    // Calculate spacing for candlesticks with zoom and pan
    const effectiveDataLength = Math.floor(data.length / zoomLevel)
    const startIndex = Math.max(0, Math.min(
      data.length - effectiveDataLength, 
      Math.floor(panOffset + (data.length - effectiveDataLength) / 2)
    ))
    const visibleData = data.slice(startIndex, startIndex + effectiveDataLength)
    const candleSpacing = chartContentWidth / visibleData.length
    const candleWidth = Math.max(2, candleSpacing * 0.8)

    // Draw time axis
    ctx.textAlign = 'center'
    const timePoints = Math.min(8, visibleData.length)
    for (let i = 0; i < timePoints; i++) {
      const dataIndex = Math.floor((visibleData.length / timePoints) * i)
      if (dataIndex < visibleData.length) {
        const x = padding.left + dataIndex * candleSpacing + candleSpacing / 2
        const time = new Date(visibleData[dataIndex].timestamp)
        const timeStr = time.toLocaleTimeString('en-IN', { 
          hour: '2-digit', 
          minute: '2-digit' 
        })
        ctx.fillText(timeStr, x, padding.top + chartContentHeight + 20)
      }
    }

    // Draw candlesticks or other chart types
    visibleData.forEach((candle, index) => {
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
          if (index === visibleData.length - 1) {
            ctx.stroke()
          }
        }
      } else if (settings.chartType === 'area') {
        if (index === 0) {
          ctx.beginPath()
          ctx.moveTo(x, padding.top + chartContentHeight)
        }
        
        ctx.lineTo(x, closeY)
        
        if (index === visibleData.length - 1) {
          ctx.lineTo(x, padding.top + chartContentHeight)
          ctx.closePath()
          
          const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartContentHeight)
          gradient.addColorStop(0, 'rgba(16, 185, 129, 0.4)')
          gradient.addColorStop(1, 'rgba(16, 185, 129, 0.05)')
          
          ctx.fillStyle = gradient
          ctx.fill()
          
          ctx.strokeStyle = '#10b981'
          ctx.lineWidth = 3
          ctx.beginPath()
          visibleData.forEach((candle, idx) => {
            const px = padding.left + idx * candleSpacing + candleSpacing / 2
            const py = padding.top + chartContentHeight - ((candle.close - minPrice) / priceRange) * chartContentHeight
            
            if (idx === 0) ctx.moveTo(px, py)
            else ctx.lineTo(px, py)
          })
          ctx.stroke()
        }
      }
    })

    // Draw volume bars if enabled
    if (settings.showVolume) {
      const volumeHeight = chartHeight * 0.25
      const volumeY = chartHeight - padding.bottom - volumeHeight
      const maxVolume = Math.max(...visibleData.map(d => d.volume))

      visibleData.forEach((candle, index) => {
        const x = padding.left + index * candleSpacing + candleSpacing / 2
        const barHeight = (candle.volume / maxVolume) * volumeHeight
        const isGreen = candle.close > candle.open
        
        ctx.fillStyle = isGreen ? 'rgba(16, 185, 129, 0.6)' : 'rgba(239, 68, 68, 0.6)'
        ctx.fillRect(x - candleWidth / 2, volumeY + volumeHeight - barHeight, candleWidth, barHeight)
      })
    }

    // Draw crosshair
    if (settings.crosshair && mousePos.x > 0 && mousePos.y > 0) {
      ctx.strokeStyle = 'rgba(139, 92, 246, 0.8)'
      ctx.lineWidth = 1
      ctx.setLineDash([5, 5])
      
      ctx.beginPath()
      ctx.moveTo(mousePos.x, padding.top)
      ctx.lineTo(mousePos.x, padding.top + chartContentHeight)
      ctx.stroke()
      
      ctx.beginPath()
      ctx.moveTo(padding.left, mousePos.y)
      ctx.lineTo(padding.left + chartContentWidth, mousePos.y)
      ctx.stroke()
      
      ctx.setLineDash([])
    }
  }

  // Initialize and handle updates
  useEffect(() => {
    const initChart = () => {
      setTimeout(() => {
        if (canvasRef.current && (containerRef.current || chartContainerRef.current)) {
          console.log('✅ Initializing AdvancedChart...', { 
            symbol, 
            dataLength: data.length, 
            chartType: settings.chartType 
          })
          drawChart()
        } else {
          console.error('❌ Chart initialization failed:', {
            canvas: !!canvasRef.current,
            container: !!(containerRef.current || chartContainerRef.current)
          })
        }
      }, 100)
    }
    
    initChart()
  }, [])

  useEffect(() => {
    if (canvasRef.current && (containerRef.current || chartContainerRef.current)) {
      drawChart()
    }
  }, [data, settings, zoomLevel, panOffset])

  useEffect(() => {
    const handleResize = () => {
      setTimeout(() => {
        if (canvasRef.current && (containerRef.current || chartContainerRef.current)) {
          drawChart()
        }
      }, 100)
    }
    
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // Escape key handler for fullscreen
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isFullscreen) {
        setIsFullscreen(false)
      }
    }
    
    let scrollY: number = 0
    
    if (isFullscreen) {
      // Store current scroll position
      scrollY = window.scrollY
      
      window.addEventListener('keydown', handleKeyPress)
      
      // Prevent scrolling behind modal
      document.body.style.cssText = `
        position: fixed;
        top: -${scrollY}px;
        left: 0;
        right: 0;
        width: 100%;
        overflow: hidden;
      `
      
      // Prevent wheel events on document
      document.addEventListener('wheel', preventScroll, { passive: false })
      document.addEventListener('touchmove', preventScroll, { passive: false })
    } else {
      // Restore body scroll and position
      document.body.style.cssText = ''
      
      // Restore scroll position
      if (scrollY) {
        window.scrollTo(0, scrollY)
      }
      
      // Remove scroll prevention
      document.removeEventListener('wheel', preventScroll)
      document.removeEventListener('touchmove', preventScroll)
    }
    
    function preventScroll(e: Event) {
      e.preventDefault()
      e.stopPropagation()
      return false
    }
    
    return () => {
      window.removeEventListener('keydown', handleKeyPress)
      document.removeEventListener('wheel', preventScroll)
      document.removeEventListener('touchmove', preventScroll)
      
      // Always restore on cleanup
      document.body.style.cssText = ''
      if (scrollY && isFullscreen) {
        window.scrollTo(0, scrollY)
      }
    }
  }, [isFullscreen])

  const chartTypes = [
    { type: 'candlestick', icon: '🕯️', label: 'Candlestick', category: 'Standard' },
    { type: 'line', icon: '📈', label: 'Line', category: 'Standard' },
    { type: 'area', icon: '🏔️', label: 'Area', category: 'Standard' },
    { type: 'ohlc', icon: '📊', label: 'OHLC', category: 'Standard' },
    { type: 'heikin-ashi', icon: '🎯', label: 'Heikin-Ashi', category: 'Advanced' },
    { type: 'renko', icon: '🧱', label: 'Renko', category: 'Advanced' },
    { type: 'volume-profile', icon: '📊', label: 'Volume Profile', category: 'Advanced' },
    { type: 'mountain', icon: '⛰️', label: 'Mountain', category: 'Advanced' },
    { type: 'point-figure', icon: '⚡', label: 'Point & Figure', category: 'Professional' },
    { type: 'kagi', icon: '🗾', label: 'Kagi', category: 'Professional' },
    { type: 'three-line-break', icon: '📍', label: 'Three Line Break', category: 'Professional' },
    { type: 'footprint', icon: '👣', label: 'Footprint', category: 'Professional' },
    { type: 'range-bars', icon: '📏', label: 'Range Bars', category: 'Professional' },
  ]

  const groupedChartTypes = chartTypes.reduce((groups: any, item) => {
    const category = item.category
    if (!groups[category]) groups[category] = []
    groups[category].push(item)
    return groups
  }, {})

  // Handle timeframe change
  const handleTimeframeChange = (newTimeframe: ChartSettings['timeframe']) => {
    console.log('📊 Timeframe changed:', newTimeframe)
    setSettings(prev => ({ ...prev, timeframe: newTimeframe }))
    
    // Trigger data request if callback provided
    if (onDataRequest) {
      onDataRequest(symbol, newTimeframe)
    }
    
    // Add some visual feedback by temporarily showing loading state
    setIsLoading(true)
    setTimeout(() => {
      setIsLoading(false)
    }, 500)
  }

  // Render just the chart content (for fullscreen mode)
  const renderChartContent = () => (
    <div className="h-full flex flex-col space-y-4">
      {/* Chart Container */}
      <div 
        ref={chartContainerRef}
        className="flex-1 relative glass-card rounded-xl overflow-hidden backdrop-blur-sm"
        style={{ minHeight: 400 }}
      >
        {isLoading && (
          <div className="absolute inset-0 glass-card flex items-center justify-center z-10">
            <div className="flex items-center space-x-3 text-purple-400">
              <div className="animate-spin rounded-full h-6 w-6 border-2 border-purple-400 border-t-transparent"></div>
              <span className="font-medium">Loading chart data...</span>
            </div>
          </div>
        )}
        
        <canvas
          ref={canvasRef}
          className={`w-full h-full ${isDragging ? 'cursor-grabbing' : 'cursor-crosshair'}`}
          onClick={handleCanvasClick}
          onMouseDown={handleMouseDown}
          onMouseUp={handleMouseUp}
          onMouseMove={handleMouseMove}
          onMouseLeave={() => {
            setMousePos({ x: 0, y: 0 })
            setIsDragging(false)
          }}
          onWheel={handleMouseWheel}
        />
        
        {/* Professional Price Tooltip */}
        {hoveredCandle && mousePos.x > 0 && (
          <div 
            className="absolute z-20 bg-slate-800/95 backdrop-blur-sm text-white p-3 rounded-lg border border-slate-600/50 shadow-xl min-w-48"
            style={{
              left: Math.min(mousePos.x + 15, (chartContainerRef.current?.clientWidth || 0) - 200),
              top: Math.max(mousePos.y - 80, 10),
            }}
          >
            <div className="text-xs text-slate-400 mb-2 font-mono">
              {new Date(hoveredCandle.timestamp).toLocaleString('en-IN')}
            </div>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
              <div className="text-slate-300">Open:</div>
              <div className="text-white font-mono text-right">₹{hoveredCandle.open.toFixed(2)}</div>
              
              <div className="text-slate-300">High:</div>
              <div className="text-green-400 font-mono text-right">₹{hoveredCandle.high.toFixed(2)}</div>
              
              <div className="text-slate-300">Low:</div>
              <div className="text-red-400 font-mono text-right">₹{hoveredCandle.low.toFixed(2)}</div>
              
              <div className="text-slate-300">Close:</div>
              <div className="text-white font-mono text-right">₹{hoveredCandle.close.toFixed(2)}</div>
              
              <div className="text-slate-300 col-span-2 pt-1 border-t border-slate-700">
                Volume: <span className="text-blue-400 font-mono">{hoveredCandle.volume.toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )

  const renderChart = () => (
    <div className="space-y-4">
      {/* Professional Chart Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-2">
            <Target className="w-5 h-5 text-purple-400" />
            <h3 className="text-lg font-semibold text-white">{symbol} Chart Analysis</h3>
          </div>
          <div className="text-xs text-slate-400 bg-slate-800/50 px-2 py-1 rounded-md">
            {settings.chartType.replace('-', ' ').toUpperCase()} | {settings.timeframe} | {data.length} points | Zoom: {zoomLevel.toFixed(1)}x
          </div>
        </div>
        
        {/* Advanced Trading Toolbar - Only show in advanced mode */}
        {mode === 'advanced' && (
          <div className="flex items-center space-x-2">
            {/* Zoom Control Group */}
            <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
              <button
                onClick={zoomIn}
                className="p-1.5 rounded text-slate-400 hover:text-green-400 hover:bg-green-500/10 transition-all"
                title="Zoom In (Ctrl+Plus)"
              >
                <ZoomIn className="w-4 h-4" />
              </button>
              <button
                onClick={zoomOut}
                className="p-1.5 rounded text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-all"
                title="Zoom Out (Ctrl+Minus)"
              >
                <ZoomOut className="w-4 h-4" />
              </button>
              {!hideFitToScreen && (
                <button
                  onClick={fitToScreen}
                  className="p-1.5 rounded text-slate-400 hover:text-cyan-400 hover:bg-cyan-500/10 transition-all"
                  title="Fit to Screen (Ctrl+0)"
                >
                  <Monitor className="w-4 h-4" />
                </button>
              )}
            </div>
          
          {/* Chart Type Selector */}
          <div className="relative">
            <button
              onClick={() => setShowChartTypes(!showChartTypes)}
              className="flex items-center space-x-2 px-3 py-2 bg-slate-800/50 rounded-lg text-slate-400 hover:text-white transition-colors border border-slate-700/50"
            >
              <Layers className="w-4 h-4" />
              <span className="text-sm font-medium">{settings.chartType.replace('-', ' ')}</span>
              <div className={`w-4 h-4 transition-transform ${showChartTypes ? 'rotate-180' : ''}`}>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </div>
            </button>
            
            {showChartTypes && (
              <div className="absolute top-full right-0 mt-2 w-72 max-h-80 overflow-y-auto bg-slate-800/95 backdrop-blur-sm rounded-xl border border-slate-600/50 shadow-2xl z-50">
                {Object.entries(groupedChartTypes).map(([category, types]) => (
                  <div key={category} className="p-3">
                    <div className={`text-xs font-semibold mb-2 px-2 ${
                      category === 'Professional' ? 'text-pink-400' :
                      category === 'Advanced' ? 'text-cyan-400' : 'text-slate-400'
                    }`}>
                      {category.toUpperCase()}
                    </div>
                    <div className="space-y-1">
                      {(types as any[]).map((type) => (
                        <button
                          key={type.type}
                          onClick={() => {
                            setSettings(prev => ({ ...prev, chartType: type.type as any }))
                            setShowChartTypes(false)
                          }}
                          className={`w-full flex items-center space-x-3 p-2 rounded-lg text-left transition-colors ${
                            settings.chartType === type.type
                              ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                              : 'text-slate-300 hover:bg-slate-700/50 hover:text-white'
                          }`}
                        >
                          <span className="text-lg">{type.icon}</span>
                          <span className="text-sm font-medium">{type.label}</span>
                          {settings.chartType === type.type && (
                            <div className="ml-auto w-2 h-2 bg-purple-400 rounded-full"></div>
                          )}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          
          {/* Timeframe Selector */}
          <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
            {(['1M', '5M', '15M', '1H', '4H', '1D', '1W', '1MO'] as const).map((timeframe) => (
              <button
                key={timeframe}
                onClick={() => handleTimeframeChange(timeframe)}
                className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                  settings.timeframe === timeframe
                    ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
                title={`Switch to ${timeframe} timeframe`}
              >
                {timeframe}
              </button>
            ))}
          </div>
          
          {/* Technical Analysis Settings */}
          <div className="relative">
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors border border-slate-700/50"
              title="Technical Indicators"
            >
              <Settings className="w-4 h-4" />
            </button>
            
            {/* Settings Popup */}
            {showSettings && (
              <div className="absolute top-full right-0 mt-2 w-[500px] max-h-[500px] overflow-y-auto glass-card backdrop-blur-lg rounded-xl shadow-2xl z-50">
                <div className="p-6">
                  <h4 className="font-semibold text-white mb-6 flex items-center">
                    <Activity className="w-5 h-5 mr-2 text-purple-400" />
                    Technical Analysis & Chart Settings
                  </h4>
                  
                  {/* Technical Indicators Section */}
                  <div className="mb-6">
                    <h5 className="text-sm font-medium text-purple-300 mb-3 flex items-center">
                      <div className="w-2 h-2 bg-purple-400 rounded-full mr-2"></div>
                      Technical Indicators
                    </h5>
                    <div className="grid grid-cols-2 gap-3">
                      {settings.indicators.map(indicator => (
                        <label key={indicator.id} className="flex items-center space-x-3 cursor-pointer p-3 rounded-lg hover:bg-slate-700/30 transition-colors border border-slate-700/20">
                          <input
                            type="checkbox"
                            checked={indicator.enabled}
                            onChange={() => toggleIndicator(indicator.id)}
                            className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
                          />
                          <div className="flex items-center space-x-2">
                            <div className="w-3 h-3 rounded" style={{ backgroundColor: indicator.color }}></div>
                            <span className="text-sm font-medium text-slate-300">{indicator.name}</span>
                          </div>
                        </label>
                      ))}
                    </div>
                  </div>
                  
                  {/* Chart Display Options */}
                  <div className="pt-4 border-t border-slate-700/50">
                    <h5 className="text-sm font-medium text-cyan-300 mb-3 flex items-center">
                      <div className="w-2 h-2 bg-cyan-400 rounded-full mr-2"></div>
                      Display Options
                    </h5>
                    <div className="grid grid-cols-2 gap-4">
                      <label className="flex items-center space-x-3 text-sm text-slate-400 cursor-pointer p-3 rounded-lg hover:bg-slate-700/30 transition-colors border border-slate-700/20">
                        <input
                          type="checkbox"
                          checked={settings.showGrid}
                          onChange={(e) => setSettings(prev => ({ ...prev, showGrid: e.target.checked }))}
                          className="w-4 h-4 text-cyan-400 bg-slate-700 border-slate-600 rounded"
                        />
                        <span>Grid Lines</span>
                      </label>
                      
                      <label className="flex items-center space-x-3 text-sm text-slate-400 cursor-pointer p-3 rounded-lg hover:bg-slate-700/30 transition-colors border border-slate-700/20">
                        <input
                          type="checkbox"
                          checked={settings.showVolume}
                          onChange={(e) => setSettings(prev => ({ ...prev, showVolume: e.target.checked }))}
                          className="w-4 h-4 text-cyan-400 bg-slate-700 border-slate-600 rounded"
                        />
                        <span>Volume Bars</span>
                      </label>
                      
                      <label className="flex items-center space-x-3 text-sm text-slate-400 cursor-pointer p-3 rounded-lg hover:bg-slate-700/30 transition-colors border border-slate-700/20">
                        <input
                          type="checkbox"
                          checked={settings.crosshair}
                          onChange={(e) => setSettings(prev => ({ ...prev, crosshair: e.target.checked }))}
                          className="w-4 h-4 text-cyan-400 bg-slate-700 border-slate-600 rounded"
                        />
                        <span>Crosshair</span>
                      </label>
                      
                      <div className="flex items-center justify-center p-3 rounded-lg bg-gradient-to-r from-purple-500/10 to-cyan-500/10 border border-purple-500/20">
                        <div className="text-xs text-purple-300 flex items-center">
                          <Info className="w-3 h-3 mr-1" />
                          Pro Features
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
          
          {/* Fullscreen Mode */}
          <button
            onClick={() => setIsFullscreen(true)}
            className="p-2 rounded-lg text-slate-400 hover:text-purple-400 hover:bg-purple-500/10 transition-colors border border-slate-700/50"
            title="Fullscreen Mode (F11)"
          >
            <Expand className="w-4 h-4" />
          </button>
          </div>
        )}
      </div>

      {/* Chart Container */}
      <div 
        ref={chartContainerRef}
        className="relative glass-card rounded-xl overflow-hidden backdrop-blur-sm"
        style={{ height: height }}
      >
        {isLoading && (
          <div className="absolute inset-0 glass-card flex items-center justify-center z-10">
            <div className="flex items-center space-x-3 text-purple-400">
              <div className="animate-spin rounded-full h-6 w-6 border-2 border-purple-400 border-t-transparent"></div>
              <span className="font-medium">Loading chart data...</span>
            </div>
          </div>
        )}
        
        <canvas
          ref={canvasRef}
          className={`w-full h-full ${isDragging ? 'cursor-grabbing' : 'cursor-crosshair'}`}
          onClick={handleCanvasClick}
          onMouseDown={handleMouseDown}
          onMouseUp={handleMouseUp}
          onMouseMove={handleMouseMove}
          onMouseLeave={() => {
            setMousePos({ x: 0, y: 0 })
            setIsDragging(false)
          }}
          onWheel={handleMouseWheel}
        />
        
        {/* Professional Price Tooltip */}
        {hoveredCandle && mousePos.x > 0 && (
          <div 
            className="absolute z-20 bg-slate-800/95 backdrop-blur-sm text-white p-3 rounded-lg border border-slate-600/50 shadow-xl min-w-48"
            style={{
              left: Math.min(mousePos.x + 15, (chartContainerRef.current?.clientWidth || 0) - 200),
              top: Math.max(mousePos.y - 80, 10),
            }}
          >
            <div className="text-xs text-slate-400 mb-2 font-mono">
              {new Date(hoveredCandle.timestamp).toLocaleString('en-IN')}
            </div>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
              <div className="text-slate-300">Open:</div>
              <div className="text-white font-mono text-right">₹{hoveredCandle.open.toFixed(2)}</div>
              
              <div className="text-slate-300">High:</div>
              <div className="text-green-400 font-mono text-right">₹{hoveredCandle.high.toFixed(2)}</div>
              
              <div className="text-slate-300">Low:</div>
              <div className="text-red-400 font-mono text-right">₹{hoveredCandle.low.toFixed(2)}</div>
              
              <div className="text-slate-300">Close:</div>
              <div className="text-white font-mono text-right">₹{hoveredCandle.close.toFixed(2)}</div>
              
              <div className="text-slate-300 col-span-2 pt-1 border-t border-slate-700">
                Volume: <span className="text-blue-400 font-mono">{hoveredCandle.volume.toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}
      </div>

    </div>
  )

  return (
    <>
      {/* Normal Chart View */}
      <div ref={containerRef} className="glass-card rounded-2xl p-6">
        {renderChart()}
      </div>

      {/* Professional Fullscreen Modal */}
      {isFullscreen && (
        <div className="fixed inset-0 z-50 glass-card backdrop-blur-sm overflow-hidden">
          <div className="h-full flex flex-col p-6 overflow-hidden">
            {/* Fullscreen Header */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center space-x-4">
                <Target className="w-6 h-6 text-purple-400" />
                <h2 className="text-2xl font-bold text-white">{symbol} - Professional Chart Analysis</h2>
                <div className="text-sm text-slate-400 bg-slate-800/50 px-3 py-1 rounded-lg">
                  {settings.chartType.replace('-', ' ').toUpperCase()} | {settings.timeframe} | {data.length} points | Zoom: {zoomLevel.toFixed(1)}x
                </div>
              </div>
              
              {/* Fullscreen Controls */}
              <div className="flex items-center space-x-3">
                {/* Zoom Controls */}
                <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
                  <button
                    onClick={zoomIn}
                    className="p-2 rounded text-slate-400 hover:text-green-400 hover:bg-green-500/10 transition-all"
                    title="Zoom In (Ctrl+Plus)"
                  >
                    <ZoomIn className="w-5 h-5" />
                  </button>
                  <button
                    onClick={zoomOut}
                    className="p-2 rounded text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-all"
                    title="Zoom Out (Ctrl+Minus)"
                  >
                    <ZoomOut className="w-5 h-5" />
                  </button>
                  <button
                    onClick={fitToScreen}
                    className="p-2 rounded text-slate-400 hover:text-cyan-400 hover:bg-cyan-500/10 transition-all"
                    title="Fit to Screen (Ctrl+0)"
                  >
                    <Monitor className="w-5 h-5" />
                  </button>
                </div>
                
                {/* Timeframe Selector */}
                <div className="flex items-center bg-slate-800/50 rounded-lg p-1 border border-slate-700/50">
                  {(['1M', '5M', '15M', '1H', '4H', '1D', '1W', '1MO'] as const).map((timeframe) => (
                    <button
                      key={timeframe}
                      onClick={() => handleTimeframeChange(timeframe)}
                      className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                        settings.timeframe === timeframe
                          ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                          : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                      }`}
                      title={`Switch to ${timeframe} timeframe`}
                    >
                      {timeframe}
                    </button>
                  ))}
                </div>
                
                {/* Exit Fullscreen */}
                <button
                  onClick={() => setIsFullscreen(false)}
                  className="p-3 rounded-xl text-slate-400 hover:text-white hover:bg-red-500/20 hover:text-red-400 transition-colors border border-slate-700/50"
                  title="Exit Fullscreen (ESC)"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>
            
            {/* Fullscreen Chart */}
            <div className="flex-1 min-h-0">
              {renderChartContent()}
            </div>
          </div>
        </div>
      )}
    </>
  )
}