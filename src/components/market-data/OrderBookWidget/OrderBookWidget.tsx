import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  ArrowUpDown, 
  Volume2,
  DollarSign,
  MoreHorizontal
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface OrderBookEntry {
  price: number
  quantity: number
  total: number
  orders?: number
}

interface OrderBookData {
  symbol: string
  bids: OrderBookEntry[]
  asks: OrderBookEntry[]
  spread: number
  spreadPercent: number
  lastUpdated: Date
}

interface OrderBookWidgetProps {
  symbol: string
  data?: OrderBookData
  maxLevels?: number
  showSpread?: boolean
  showOrders?: boolean
  compactMode?: boolean
  className?: string
}

const OrderBookRow: React.FC<{
  entry: OrderBookEntry
  type: 'bid' | 'ask'
  maxTotal: number
  index: number
  showOrders: boolean
}> = ({ entry, type, maxTotal, index, showOrders }) => {
  const fillPercent = (entry.total / maxTotal) * 100
  const isBid = type === 'bid'

  return (
    <motion.div
      className={`relative flex items-center justify-between px-3 py-1 text-sm hover:bg-slate-800/40 transition-all duration-200 ${
        isBid ? 'text-green-200' : 'text-red-200'
      }`}
      initial={{ opacity: 0, x: isBid ? -20 : 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.02, duration: 0.3 }}
      whileHover={{ scale: 1.01 }}
    >
      {/* Background fill */}
      <div 
        className={`absolute inset-0 ${
          isBid 
            ? 'bg-gradient-to-r from-green-600/20 to-transparent' 
            : 'bg-gradient-to-l from-red-600/20 to-transparent'
        }`}
        style={{ 
          width: `${fillPercent}%`,
          [isBid ? 'left' : 'right']: 0
        }}
      />

      {/* Content */}
      <div className="relative z-10 flex items-center justify-between w-full">
        {isBid ? (
          <>
            <span className="font-mono font-semibold text-green-400">
              ₹{entry.price.toFixed(2)}
            </span>
            <span className="text-slate-300 font-mono">
              {entry.quantity.toLocaleString()}
            </span>
            <span className="text-slate-400 font-mono text-xs">
              {entry.total.toLocaleString()}
            </span>
            {showOrders && entry.orders && (
              <span className="text-slate-500 text-xs">
                {entry.orders}
              </span>
            )}
          </>
        ) : (
          <>
            {showOrders && entry.orders && (
              <span className="text-slate-500 text-xs">
                {entry.orders}
              </span>
            )}
            <span className="text-slate-400 font-mono text-xs">
              {entry.total.toLocaleString()}
            </span>
            <span className="text-slate-300 font-mono">
              {entry.quantity.toLocaleString()}
            </span>
            <span className="font-mono font-semibold text-red-400">
              ₹{entry.price.toFixed(2)}
            </span>
          </>
        )}
      </div>
    </motion.div>
  )
}

const SpreadDisplay: React.FC<{
  spread: number
  spreadPercent: number
  bestBid: number
  bestAsk: number
}> = ({ spread, spreadPercent, bestBid, bestAsk }) => {
  return (
    <motion.div 
      className="flex items-center justify-center py-3 bg-slate-800/50 border-y border-slate-700/50"
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4 }}
    >
      <div className="text-center">
        <div className="flex items-center justify-center space-x-2 text-yellow-400 font-semibold">
          <ArrowUpDown className="h-4 w-4" />
          <span className="font-mono">₹{spread.toFixed(2)}</span>
          <span className="text-xs">({spreadPercent.toFixed(3)}%)</span>
        </div>
        <div className="text-xs text-slate-400 mt-1">
          Spread: {bestAsk.toFixed(2)} - {bestBid.toFixed(2)}
        </div>
      </div>
    </motion.div>
  )
}

export const OrderBookWidget: React.FC<OrderBookWidgetProps> = ({
  symbol,
  data,
  maxLevels = 10,
  showSpread = true,
  showOrders = false,
  compactMode = false,
  className = ''
}) => {
  const [selectedLevel, setSelectedLevel] = useState<number | null>(null)
  const { isConnected } = useConnectionStatus()

  // Mock data generator
  const generateMockData = useMemo((): OrderBookData => {
    const basePrice = 2456.75
    const bids: OrderBookEntry[] = []
    const asks: OrderBookEntry[] = []

    // Generate bids (decreasing prices)
    let total = 0
    for (let i = 0; i < maxLevels; i++) {
      const price = basePrice - (i + 1) * 0.25
      const quantity = Math.floor(Math.random() * 1000) + 100
      total += quantity
      const orders = Math.floor(Math.random() * 10) + 1

      bids.push({
        price,
        quantity,
        total,
        orders
      })
    }

    // Generate asks (increasing prices)
    total = 0
    for (let i = 0; i < maxLevels; i++) {
      const price = basePrice + (i + 1) * 0.25
      const quantity = Math.floor(Math.random() * 1000) + 100
      total += quantity
      const orders = Math.floor(Math.random() * 10) + 1

      asks.push({
        price,
        quantity,
        total,
        orders
      })
    }

    const bestBid = bids[0]?.price || 0
    const bestAsk = asks[0]?.price || 0
    const spread = bestAsk - bestBid
    const spreadPercent = (spread / bestBid) * 100

    return {
      symbol,
      bids,
      asks: asks.reverse(), // Show lowest asks first
      spread,
      spreadPercent,
      lastUpdated: new Date()
    }
  }, [symbol, maxLevels])

  const orderBookData = data || generateMockData

  // Calculate max total for visualization
  const maxBidTotal = Math.max(...orderBookData.bids.map(b => b.total))
  const maxAskTotal = Math.max(...orderBookData.asks.map(a => a.total))
  const maxTotal = Math.max(maxBidTotal, maxAskTotal)

  // Calculate summary metrics
  const totalBidVolume = orderBookData.bids.reduce((sum, bid) => sum + bid.quantity, 0)
  const totalAskVolume = orderBookData.asks.reduce((sum, ask) => sum + ask.quantity, 0)
  const totalOrders = orderBookData.bids.reduce((sum, bid) => sum + (bid.orders || 0), 0) +
                     orderBookData.asks.reduce((sum, ask) => sum + (ask.orders || 0), 0)

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-slate-700/50">
        <div className="flex items-center space-x-3">
          <div className={`w-3 h-3 rounded-full ${
            isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
          }`} />
          <div>
            <h2 className="text-lg font-bold text-white">Order Book</h2>
            <span className="text-sm text-slate-400">{symbol}</span>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {!compactMode && (
            <>
              <div className="text-center">
                <div className="text-xs text-slate-400">Volume</div>
                <div className="text-sm font-semibold text-white">
                  {(totalBidVolume + totalAskVolume).toLocaleString()}
                </div>
              </div>
              {showOrders && (
                <div className="text-center">
                  <div className="text-xs text-slate-400">Orders</div>
                  <div className="text-sm font-semibold text-white">
                    {totalOrders}
                  </div>
                </div>
              )}
            </>
          )}
          <button className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300">
            <MoreHorizontal className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Column Headers */}
      {!compactMode && (
        <div className="px-4 py-2 bg-slate-800/30 border-b border-slate-700/50">
          <div className="flex items-center justify-between text-xs font-medium text-slate-400">
            <div className="flex items-center space-x-8">
              <span>Price</span>
              <span>Size</span>
              <span>Total</span>
              {showOrders && <span>Orders</span>}
            </div>
            <div className="flex items-center space-x-8">
              {showOrders && <span>Orders</span>}
              <span>Total</span>
              <span>Size</span>
              <span>Price</span>
            </div>
          </div>
        </div>
      )}

      {/* Asks (Sell Orders) */}
      <div className="divide-y divide-slate-700/30">
        <AnimatePresence mode="popLayout">
          {orderBookData.asks.map((ask, index) => (
            <OrderBookRow
              key={`ask-${ask.price}-${index}`}
              entry={ask}
              type="ask"
              maxTotal={maxTotal}
              index={index}
              showOrders={showOrders}
            />
          ))}
        </AnimatePresence>
      </div>

      {/* Spread Display */}
      {showSpread && orderBookData.bids[0] && orderBookData.asks[0] && (
        <SpreadDisplay
          spread={orderBookData.spread}
          spreadPercent={orderBookData.spreadPercent}
          bestBid={orderBookData.bids[0].price}
          bestAsk={orderBookData.asks[0].price}
        />
      )}

      {/* Bids (Buy Orders) */}
      <div className="divide-y divide-slate-700/30">
        <AnimatePresence mode="popLayout">
          {orderBookData.bids.map((bid, index) => (
            <OrderBookRow
              key={`bid-${bid.price}-${index}`}
              entry={bid}
              type="bid"
              maxTotal={maxTotal}
              index={index}
              showOrders={showOrders}
            />
          ))}
        </AnimatePresence>
      </div>

      {/* Summary Stats */}
      {!compactMode && (
        <div className="p-4 border-t border-slate-700/50">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="space-y-2">
              <div className="flex items-center space-x-2 text-green-400">
                <TrendingUp className="h-4 w-4" />
                <span className="font-semibold">Bid Side</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">Total Volume:</span>
                <span className="text-white font-mono">{totalBidVolume.toLocaleString()}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">Best Bid:</span>
                <span className="text-green-400 font-mono">₹{orderBookData.bids[0]?.price.toFixed(2)}</span>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center space-x-2 text-red-400">
                <TrendingDown className="h-4 w-4" />
                <span className="font-semibold">Ask Side</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">Total Volume:</span>
                <span className="text-white font-mono">{totalAskVolume.toLocaleString()}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">Best Ask:</span>
                <span className="text-red-400 font-mono">₹{orderBookData.asks[0]?.price.toFixed(2)}</span>
              </div>
            </div>
          </div>

          {/* Market Depth Indicator */}
          <div className="mt-4 pt-4 border-t border-slate-700/50">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Market Depth</span>
              <span>Updated: {orderBookData.lastUpdated.toLocaleTimeString()}</span>
            </div>
            <div className="mt-2 bg-slate-800 rounded-full h-2 overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-green-500 to-red-500"
                style={{ 
                  background: `linear-gradient(to right, #10b981 0%, #10b981 ${
                    (totalBidVolume / (totalBidVolume + totalAskVolume)) * 100
                  }%, #ef4444 ${
                    (totalBidVolume / (totalBidVolume + totalAskVolume)) * 100
                  }%, #ef4444 100%)`
                }}
              />
            </div>
            <div className="flex justify-between text-xs text-slate-400 mt-1">
              <span>Buy: {((totalBidVolume / (totalBidVolume + totalAskVolume)) * 100).toFixed(1)}%</span>
              <span>Sell: {((totalAskVolume / (totalBidVolume + totalAskVolume)) * 100).toFixed(1)}%</span>
            </div>
          </div>
        </div>
      )}
    </motion.div>
  )
}