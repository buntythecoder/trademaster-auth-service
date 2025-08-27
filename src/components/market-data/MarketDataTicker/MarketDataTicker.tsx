import React, { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { TrendingUp, TrendingDown, Pause, Play } from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface MarketSymbol {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  lastUpdated: Date
}

interface MarketDataTickerProps {
  symbols: MarketSymbol[]
  speed?: 'slow' | 'normal' | 'fast'
  showChange?: boolean
  showVolume?: boolean
  pauseOnHover?: boolean
  className?: string
}

const speedSettings = {
  slow: 40,
  normal: 30,
  fast: 20
}

const TickerItem: React.FC<{ 
  symbol: MarketSymbol
  showChange: boolean
  showVolume: boolean
}> = ({ symbol, showChange, showVolume }) => {
  const isPositive = symbol.change >= 0

  return (
    <motion.div
      className="flex items-center space-x-4 px-6 py-2 bg-slate-800/30 rounded-xl backdrop-blur-sm border border-slate-700/50 hover:bg-slate-700/40 transition-all duration-300 min-w-max"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      whileHover={{ scale: 1.02 }}
      transition={{ duration: 0.3 }}
    >
      {/* Symbol Info */}
      <div className="text-left">
        <div className="font-semibold text-white text-sm">{symbol.symbol}</div>
        <div className="text-xs text-slate-400 truncate max-w-20">{symbol.name}</div>
      </div>

      {/* Price */}
      <div className="text-right">
        <div className="font-bold text-white">
          ₹{symbol.price.toLocaleString('en-IN', { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
          })}
        </div>
        
        {showChange && (
          <div className={`flex items-center text-xs ${
            isPositive ? 'text-green-400' : 'text-red-400'
          }`}>
            {isPositive ? (
              <TrendingUp className="h-3 w-3 mr-1" />
            ) : (
              <TrendingDown className="h-3 w-3 mr-1" />
            )}
            {isPositive ? '+' : ''}₹{Math.abs(symbol.change).toFixed(2)}
            <span className="ml-1">
              ({isPositive ? '+' : ''}{symbol.changePercent.toFixed(2)}%)
            </span>
          </div>
        )}
      </div>

      {/* Volume (optional) */}
      {showVolume && (
        <div className="text-right">
          <div className="text-xs text-slate-400">Vol</div>
          <div className="text-xs text-slate-300 font-medium">
            {(symbol.volume / 1000).toFixed(1)}K
          </div>
        </div>
      )}

      {/* Market Status Indicator */}
      <div className="flex items-center">
        <div className={`w-2 h-2 rounded-full ${
          Date.now() - symbol.lastUpdated.getTime() < 5000 
            ? 'bg-green-400 animate-pulse' 
            : 'bg-yellow-400'
        }`} />
      </div>
    </motion.div>
  )
}

export const MarketDataTicker: React.FC<MarketDataTickerProps> = ({
  symbols,
  speed = 'normal',
  showChange = true,
  showVolume = false,
  pauseOnHover = true,
  className = ''
}) => {
  const [isPaused, setIsPaused] = useState(false)
  const [userPaused, setUserPaused] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)
  const { isConnected } = useConnectionStatus()

  // Mock data when no symbols provided
  const mockSymbols: MarketSymbol[] = [
    {
      symbol: 'RELIANCE',
      name: 'Reliance Industries',
      price: 2456.75,
      change: 34.50,
      changePercent: 1.42,
      volume: 2847293,
      high: 2467.80,
      low: 2398.20,
      lastUpdated: new Date()
    },
    {
      symbol: 'TCS',
      name: 'Tata Consultancy',
      price: 3789.40,
      change: -42.15,
      changePercent: -1.10,
      volume: 1583647,
      high: 3834.90,
      low: 3776.25,
      lastUpdated: new Date()
    },
    {
      symbol: 'HDFCBANK',
      name: 'HDFC Bank',
      price: 1687.25,
      change: 18.90,
      changePercent: 1.13,
      volume: 3847392,
      high: 1695.80,
      low: 1674.30,
      lastUpdated: new Date()
    },
    {
      symbol: 'INFY',
      name: 'Infosys',
      price: 1456.80,
      change: -8.45,
      changePercent: -0.58,
      volume: 2947583,
      high: 1467.90,
      low: 1445.20,
      lastUpdated: new Date()
    },
    {
      symbol: 'ICICIBANK',
      name: 'ICICI Bank',
      price: 987.35,
      change: 12.75,
      changePercent: 1.31,
      volume: 4738291,
      high: 994.80,
      low: 979.45,
      lastUpdated: new Date()
    },
    {
      symbol: 'WIPRO',
      name: 'Wipro Limited',
      price: 445.60,
      change: -3.20,
      changePercent: -0.71,
      volume: 1847392,
      high: 452.30,
      low: 441.80,
      lastUpdated: new Date()
    }
  ]

  const displaySymbols = symbols.length > 0 ? symbols : mockSymbols

  // Auto-scroll animation
  useEffect(() => {
    if (!scrollRef.current || isPaused || userPaused) return

    const scrollContainer = scrollRef.current
    const scrollWidth = scrollContainer.scrollWidth
    const clientWidth = scrollContainer.clientWidth
    
    if (scrollWidth <= clientWidth) return

    const animationDuration = speedSettings[speed] * 1000 // Convert to milliseconds
    let startTime: number
    let animationId: number

    const animate = (timestamp: number) => {
      if (!startTime) startTime = timestamp
      
      const elapsed = timestamp - startTime
      const progress = (elapsed / animationDuration) % 1
      
      scrollContainer.scrollLeft = progress * (scrollWidth - clientWidth)
      
      if (!isPaused && !userPaused) {
        animationId = requestAnimationFrame(animate)
      }
    }

    animationId = requestAnimationFrame(animate)

    return () => {
      if (animationId) {
        cancelAnimationFrame(animationId)
      }
    }
  }, [speed, isPaused, userPaused, displaySymbols])

  const handleMouseEnter = () => {
    if (pauseOnHover) {
      setIsPaused(true)
    }
  }

  const handleMouseLeave = () => {
    if (pauseOnHover) {
      setIsPaused(false)
    }
  }

  const toggleUserPause = () => {
    setUserPaused(!userPaused)
  }

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
        <div className="flex items-center space-x-3">
          <div className={`w-3 h-3 rounded-full ${
            isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
          }`} />
          <h2 className="text-lg font-bold text-white">Market Ticker</h2>
          <span className="text-sm text-slate-400">
            {displaySymbols.length} symbols
          </span>
        </div>
        
        <button
          onClick={toggleUserPause}
          className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300"
          aria-label={userPaused ? 'Resume ticker' : 'Pause ticker'}
        >
          {userPaused ? (
            <Play className="h-4 w-4" />
          ) : (
            <Pause className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Ticker Content */}
      <div 
        ref={scrollRef}
        className="overflow-x-hidden p-4"
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        style={{ 
          scrollbarWidth: 'none',
          msOverflowStyle: 'none' 
        }}
      >
        <div className="flex space-x-4 w-max">
          <AnimatePresence mode="popLayout">
            {displaySymbols.map((symbol) => (
              <TickerItem
                key={`${symbol.symbol}-${symbol.lastUpdated.getTime()}`}
                symbol={symbol}
                showChange={showChange}
                showVolume={showVolume}
              />
            ))}
          </AnimatePresence>
        </div>
      </div>

      {/* Speed Indicator */}
      <div className="px-4 pb-2">
        <div className="flex items-center justify-center space-x-2 text-xs text-slate-400">
          <span>Speed:</span>
          <div className={`w-2 h-2 rounded-full ${
            speed === 'slow' ? 'bg-yellow-400' : 
            speed === 'normal' ? 'bg-green-400' : 'bg-red-400'
          }`} />
          <span className="capitalize">{speed}</span>
          {(isPaused || userPaused) && (
            <>
              <span>•</span>
              <span>Paused</span>
            </>
          )}
        </div>
      </div>
    </motion.div>
  )
}