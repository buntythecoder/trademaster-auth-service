// Market Depth & Order Book Visualization
// FRONT-003: Real-time Market Data Enhancement

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  TrendingUp,
  TrendingDown,
  BarChart3,
  Activity,
  Layers,
  Zap,
  Eye,
  Settings,
  Maximize2,
  Volume2,
  Target,
  ArrowUpDown,
  RefreshCw
} from 'lucide-react'
import { OrderBookUpdate } from '../../../services/enhancedWebSocketService'

export interface OrderBookEntry {
  price: number
  quantity: number
  orders: number
  total?: number
  percentage?: number
}

export interface MarketDepthData {
  symbol: string
  bids: OrderBookEntry[]
  asks: OrderBookEntry[]
  lastPrice: number
  spread: number
  spreadPercent: number
  lastUpdated: Date
}

interface OrderBookVisualizationProps {
  symbol: string
  data?: MarketDepthData
  maxLevels?: number
  showSpread?: boolean
  showVolume?: boolean
  precision?: number
  className?: string
}

// Mock data for demonstration
const generateMockOrderBook = (symbol: string, lastPrice: number): MarketDepthData => {
  const spread = lastPrice * 0.001 // 0.1% spread
  const bidPrice = lastPrice - spread / 2
  const askPrice = lastPrice + spread / 2
  
  const bids: OrderBookEntry[] = []
  const asks: OrderBookEntry[] = []
  
  // Generate 10 levels each side
  for (let i = 0; i < 10; i++) {
    // Bids (decreasing price)
    const bidPriceLevel = bidPrice - (i * lastPrice * 0.0005)
    const bidQuantity = Math.floor(Math.random() * 1000) + 100
    bids.push({
      price: bidPriceLevel,
      quantity: bidQuantity,
      orders: Math.floor(Math.random() * 20) + 1,
      total: bids.reduce((acc, b) => acc + b.quantity, 0) + bidQuantity
    })
    
    // Asks (increasing price)
    const askPriceLevel = askPrice + (i * lastPrice * 0.0005)
    const askQuantity = Math.floor(Math.random() * 1000) + 100
    asks.push({
      price: askPriceLevel,
      quantity: askQuantity,
      orders: Math.floor(Math.random() * 20) + 1,
      total: asks.reduce((acc, a) => acc + a.quantity, 0) + askQuantity
    })
  }
  
  // Calculate percentages for visualization
  const maxBidQuantity = Math.max(...bids.map(b => b.quantity))
  const maxAskQuantity = Math.max(...asks.map(a => a.quantity))
  const maxQuantity = Math.max(maxBidQuantity, maxAskQuantity)
  
  bids.forEach(bid => {
    bid.percentage = (bid.quantity / maxQuantity) * 100
  })
  
  asks.forEach(ask => {
    ask.percentage = (ask.quantity / maxQuantity) * 100
  })
  
  return {
    symbol,
    bids,
    asks,
    lastPrice,
    spread: askPrice - bidPrice,
    spreadPercent: ((askPrice - bidPrice) / lastPrice) * 100,
    lastUpdated: new Date()
  }
}

const OrderBookRow: React.FC<{
  entry: OrderBookEntry
  side: 'bid' | 'ask'
  precision: number
  isHighlighted?: boolean
  onClick?: () => void
}> = ({ entry, side, precision, isHighlighted, onClick }) => {
  const isBid = side === 'bid'
  
  return (
    <motion.tr
      className={`group cursor-pointer transition-all duration-200 ${
        isHighlighted 
          ? 'bg-purple-500/10 ring-1 ring-purple-500/30' 
          : 'hover:bg-slate-700/30'
      }`}
      onClick={onClick}
      whileHover={{ scale: 1.01 }}
      transition={{ duration: 0.1 }}
    >
      {/* Price */}
      <td className={`px-3 py-2 text-right font-mono font-medium ${
        isBid ? 'text-green-400' : 'text-red-400'
      }`}>
        ₹{entry.price.toFixed(precision)}
      </td>
      
      {/* Quantity with background bar */}
      <td className="px-3 py-2 text-right relative">
        <div 
          className={`absolute inset-y-0 right-0 opacity-20 ${
            isBid ? 'bg-green-500' : 'bg-red-500'
          }`}
          style={{ width: `${entry.percentage || 0}%` }}
        />
        <span className="relative z-10 font-mono text-white">
          {entry.quantity.toLocaleString()}
        </span>
      </td>
      
      {/* Orders */}
      <td className="px-3 py-2 text-right font-mono text-slate-400 text-sm">
        {entry.orders}
      </td>
      
      {/* Total (cumulative) */}
      <td className="px-3 py-2 text-right font-mono text-slate-300 text-sm">
        {entry.total?.toLocaleString() || '-'}
      </td>
    </motion.tr>
  )
}

const SpreadIndicator: React.FC<{
  spread: number
  spreadPercent: number
  lastPrice: number
}> = ({ spread, spreadPercent, lastPrice }) => {
  return (
    <div className="bg-slate-800/50 rounded-lg p-4 mx-3 my-2 border border-slate-700/50">
      <div className="flex items-center justify-between">
        <div className="text-center flex-1">
          <div className="text-xs text-slate-400 mb-1">Best Bid</div>
          <div className="font-mono font-bold text-green-400">
            ₹{(lastPrice - spread/2).toFixed(2)}
          </div>
        </div>
        
        <div className="text-center flex-1 px-4">
          <div className="text-xs text-slate-400 mb-1">Spread</div>
          <div className="font-mono font-bold text-white">
            ₹{spread.toFixed(2)}
          </div>
          <div className="text-xs text-slate-500">
            ({spreadPercent.toFixed(3)}%)
          </div>
        </div>
        
        <div className="text-center flex-1">
          <div className="text-xs text-slate-400 mb-1">Best Ask</div>
          <div className="font-mono font-bold text-red-400">
            ₹{(lastPrice + spread/2).toFixed(2)}
          </div>
        </div>
      </div>
    </div>
  )
}

const MarketDepthChart: React.FC<{
  bids: OrderBookEntry[]
  asks: OrderBookEntry[]
  height: number
}> = ({ bids, asks, height }) => {
  const maxQuantity = Math.max(
    ...bids.map(b => b.quantity),
    ...asks.map(a => a.quantity)
  )
  
  return (
    <div className="bg-slate-800/30 rounded-lg p-4">
      <div className="flex items-center justify-between mb-4">
        <h4 className="text-sm font-medium text-white">Market Depth</h4>
        <div className="flex items-center space-x-2 text-xs text-slate-400">
          <div className="flex items-center space-x-1">
            <div className="w-3 h-2 bg-green-500/60 rounded-sm" />
            <span>Bids</span>
          </div>
          <div className="flex items-center space-x-1">
            <div className="w-3 h-2 bg-red-500/60 rounded-sm" />
            <span>Asks</span>
          </div>
        </div>
      </div>
      
      <div className="relative" style={{ height }}>
        <svg width="100%" height={height} className="overflow-visible">
          {/* Grid lines */}
          <defs>
            <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
              <path
                d="M 20 0 L 0 0 0 20"
                fill="none"
                stroke="rgba(100, 116, 139, 0.1)"
                strokeWidth="1"
              />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
          
          {/* Bid area (left side) */}
          <path
            d={`M 0,${height} ${bids.map((bid, i) => {
              const x = (i / bids.length) * 50 // Left 50% of width
              const y = height - (bid.quantity / maxQuantity) * height
              return `L ${x},${y}`
            }).join(' ')} L 50,${height} Z`}
            fill="rgba(34, 197, 94, 0.2)"
            stroke="rgba(34, 197, 94, 0.6)"
            strokeWidth={2}
          />
          
          {/* Ask area (right side) */}
          <path
            d={`M 50,${height} ${asks.map((ask, i) => {
              const x = 50 + (i / asks.length) * 50 // Right 50% of width
              const y = height - (ask.quantity / maxQuantity) * height
              return `L ${x},${y}`
            }).join(' ')} L 100,${height} Z`}
            fill="rgba(239, 68, 68, 0.2)"
            stroke="rgba(239, 68, 68, 0.6)"
            strokeWidth={2}
          />
          
          {/* Center line */}
          <line
            x1="50%"
            y1="0"
            x2="50%"
            y2={height}
            stroke="rgba(139, 92, 246, 0.5)"
            strokeWidth={2}
            strokeDasharray="4,4"
          />
        </svg>
      </div>
    </div>
  )
}

const OrderBookStats: React.FC<{
  data: MarketDepthData
}> = ({ data }) => {
  const totalBidQuantity = data.bids.reduce((sum, bid) => sum + bid.quantity, 0)
  const totalAskQuantity = data.asks.reduce((sum, ask) => sum + ask.quantity, 0)
  const totalBidOrders = data.bids.reduce((sum, bid) => sum + bid.orders, 0)
  const totalAskOrders = data.asks.reduce((sum, ask) => sum + ask.orders, 0)
  
  const bidAskRatio = totalBidQuantity / totalAskQuantity
  const imbalance = ((totalBidQuantity - totalAskQuantity) / (totalBidQuantity + totalAskQuantity)) * 100
  
  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="bg-green-500/10 rounded-lg p-4 border border-green-500/20">
        <div className="flex items-center justify-between mb-2">
          <h4 className="text-sm font-medium text-green-400">Total Bids</h4>
          <TrendingUp className="w-4 h-4 text-green-400" />
        </div>
        <div className="space-y-1">
          <div className="text-lg font-bold text-white font-mono">
            {totalBidQuantity.toLocaleString()}
          </div>
          <div className="text-xs text-green-300">
            {totalBidOrders} orders
          </div>
        </div>
      </div>
      
      <div className="bg-red-500/10 rounded-lg p-4 border border-red-500/20">
        <div className="flex items-center justify-between mb-2">
          <h4 className="text-sm font-medium text-red-400">Total Asks</h4>
          <TrendingDown className="w-4 h-4 text-red-400" />
        </div>
        <div className="space-y-1">
          <div className="text-lg font-bold text-white font-mono">
            {totalAskQuantity.toLocaleString()}
          </div>
          <div className="text-xs text-red-300">
            {totalAskOrders} orders
          </div>
        </div>
      </div>
      
      <div className="col-span-2 bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <div className="text-slate-400 mb-1">Bid/Ask Ratio</div>
            <div className={`font-mono font-bold ${
              bidAskRatio > 1 ? 'text-green-400' : bidAskRatio < 1 ? 'text-red-400' : 'text-slate-300'
            }`}>
              {bidAskRatio.toFixed(2)}
            </div>
          </div>
          <div>
            <div className="text-slate-400 mb-1">Imbalance</div>
            <div className={`font-mono font-bold ${
              imbalance > 10 ? 'text-green-400' : imbalance < -10 ? 'text-red-400' : 'text-slate-300'
            }`}>
              {imbalance > 0 ? '+' : ''}{imbalance.toFixed(1)}%
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export const OrderBookVisualization: React.FC<OrderBookVisualizationProps> = ({
  symbol,
  data,
  maxLevels = 10,
  showSpread = true,
  showVolume = true,
  precision = 2,
  className = ''
}) => {
  const [orderBookData, setOrderBookData] = useState<MarketDepthData>(
    data || generateMockOrderBook(symbol, 2456.75)
  )
  const [selectedLevel, setSelectedLevel] = useState<{ side: 'bid' | 'ask', index: number } | null>(null)
  const [viewMode, setViewMode] = useState<'table' | 'depth' | 'both'>('both')
  const [isAutoRefresh, setIsAutoRefresh] = useState(true)

  // Simulate real-time updates
  useEffect(() => {
    if (!isAutoRefresh) return
    
    const interval = setInterval(() => {
      setOrderBookData(prev => {
        // Simulate price movements
        const priceChange = (Math.random() - 0.5) * prev.lastPrice * 0.001
        const newLastPrice = Math.max(prev.lastPrice + priceChange, prev.lastPrice * 0.95)
        
        return generateMockOrderBook(symbol, newLastPrice)
      })
    }, 1000) // Update every second

    return () => clearInterval(interval)
  }, [symbol, isAutoRefresh])

  const displayedBids = useMemo(() => 
    orderBookData.bids.slice(0, maxLevels).reverse(), // Reverse to show highest bid first
    [orderBookData.bids, maxLevels]
  )
  
  const displayedAsks = useMemo(() => 
    orderBookData.asks.slice(0, maxLevels),
    [orderBookData.asks, maxLevels]
  )

  const handleLevelClick = useCallback((side: 'bid' | 'ask', index: number) => {
    setSelectedLevel(prev => 
      prev?.side === side && prev?.index === index ? null : { side, index }
    )
  }, [])

  return (
    <motion.div
      className={`glass-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
        <div className="flex items-center space-x-3">
          <Layers className="w-5 h-5 text-purple-400" />
          <h3 className="text-lg font-bold text-white">{symbol} Order Book</h3>
          <div className={`w-2 h-2 rounded-full ${
            isAutoRefresh ? 'bg-green-400 animate-pulse' : 'bg-slate-500'
          }`} />
        </div>
        
        <div className="flex items-center space-x-2">
          <div className="flex items-center space-x-1 bg-slate-800/50 rounded-lg p-1">
            {(['table', 'depth', 'both'] as const).map(mode => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                className={`px-3 py-1 text-xs rounded-md capitalize transition-colors ${
                  viewMode === mode
                    ? 'bg-purple-500 text-white'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>
          
          <button
            onClick={() => setIsAutoRefresh(!isAutoRefresh)}
            className={`p-2 rounded-lg transition-colors ${
              isAutoRefresh
                ? 'bg-green-500/20 text-green-400'
                : 'bg-slate-700/50 text-slate-400 hover:text-white'
            }`}
            title="Toggle Auto Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${isAutoRefresh ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      <div className="p-4 space-y-4">
        {/* Statistics */}
        <OrderBookStats data={orderBookData} />
        
        {/* Spread Indicator */}
        {showSpread && (
          <SpreadIndicator
            spread={orderBookData.spread}
            spreadPercent={orderBookData.spreadPercent}
            lastPrice={orderBookData.lastPrice}
          />
        )}

        {(viewMode === 'table' || viewMode === 'both') && (
          <div className="bg-slate-800/30 rounded-lg overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-800/50">
                  <tr>
                    <th className="px-3 py-3 text-right text-slate-400 font-medium">Price</th>
                    <th className="px-3 py-3 text-right text-slate-400 font-medium">Quantity</th>
                    <th className="px-3 py-3 text-right text-slate-400 font-medium">Orders</th>
                    <th className="px-3 py-3 text-right text-slate-400 font-medium">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {/* Asks (sell orders) */}
                  {displayedAsks.map((ask, index) => (
                    <OrderBookRow
                      key={`ask-${index}`}
                      entry={ask}
                      side="ask"
                      precision={precision}
                      isHighlighted={selectedLevel?.side === 'ask' && selectedLevel?.index === index}
                      onClick={() => handleLevelClick('ask', index)}
                    />
                  )).reverse()}
                  
                  {/* Bids (buy orders) */}
                  {displayedBids.map((bid, index) => (
                    <OrderBookRow
                      key={`bid-${index}`}
                      entry={bid}
                      side="bid"
                      precision={precision}
                      isHighlighted={selectedLevel?.side === 'bid' && selectedLevel?.index === index}
                      onClick={() => handleLevelClick('bid', index)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {(viewMode === 'depth' || viewMode === 'both') && (
          <MarketDepthChart
            bids={displayedBids}
            asks={displayedAsks}
            height={200}
          />
        )}

        {/* Footer */}
        <div className="flex items-center justify-between text-xs text-slate-500">
          <span>
            Last updated: {orderBookData.lastUpdated.toLocaleTimeString()}
          </span>
          <span>
            Showing {maxLevels} levels each side
          </span>
        </div>
      </div>
    </motion.div>
  )
}

export default OrderBookVisualization