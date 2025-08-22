import React, { useState, useEffect } from 'react'
import { TrendingUp, TrendingDown, Activity, BarChart3 } from 'lucide-react'

interface OrderBookEntry {
  price: number
  quantity: number
  total: number
}

interface OrderBookData {
  symbol: string
  bids: OrderBookEntry[]
  asks: OrderBookEntry[]
  lastPrice: number
  change: number
  changePercent: number
  volume: string
}

const mockOrderBookData: OrderBookData = {
  symbol: 'RELIANCE',
  bids: [
    { price: 2547.25, quantity: 150, total: 150 },
    { price: 2547.20, quantity: 200, total: 350 },
    { price: 2547.15, quantity: 100, total: 450 },
    { price: 2547.10, quantity: 300, total: 750 },
    { price: 2547.05, quantity: 180, total: 930 },
    { price: 2547.00, quantity: 250, total: 1180 },
    { price: 2546.95, quantity: 120, total: 1300 },
    { price: 2546.90, quantity: 400, total: 1700 },
  ],
  asks: [
    { price: 2547.30, quantity: 180, total: 180 },
    { price: 2547.35, quantity: 220, total: 400 },
    { price: 2547.40, quantity: 150, total: 550 },
    { price: 2547.45, quantity: 280, total: 830 },
    { price: 2547.50, quantity: 200, total: 1030 },
    { price: 2547.55, quantity: 160, total: 1190 },
    { price: 2547.60, quantity: 300, total: 1490 },
    { price: 2547.65, quantity: 190, total: 1680 },
  ],
  lastPrice: 2547.30,
  change: 23.45,
  changePercent: 0.93,
  volume: '1.2M'
}

interface OrderBookProps {
  symbol?: string
  height?: number
}

export function OrderBook({ symbol = 'RELIANCE', height = 400 }: OrderBookProps) {
  const [orderBookData, setOrderBookData] = useState(mockOrderBookData)
  const [selectedPrice, setSelectedPrice] = useState<number | null>(null)

  useEffect(() => {
    // Simulate real-time order book updates
    const interval = setInterval(() => {
      setOrderBookData(prevData => {
        const newBids = prevData.bids.map(bid => ({
          ...bid,
          quantity: Math.max(50, bid.quantity + Math.floor((Math.random() - 0.5) * 100)),
          total: bid.total + Math.floor((Math.random() - 0.5) * 50)
        }))
        
        const newAsks = prevData.asks.map(ask => ({
          ...ask,
          quantity: Math.max(50, ask.quantity + Math.floor((Math.random() - 0.5) * 100)),
          total: ask.total + Math.floor((Math.random() - 0.5) * 50)
        }))

        return {
          ...prevData,
          bids: newBids,
          asks: newAsks,
          lastPrice: prevData.lastPrice + (Math.random() - 0.5) * 2,
        }
      })
    }, 2000)

    return () => clearInterval(interval)
  }, [])

  const maxTotal = Math.max(
    Math.max(...orderBookData.bids.map(b => b.total)),
    Math.max(...orderBookData.asks.map(a => a.total))
  )

  const spread = orderBookData.asks[0]?.price - orderBookData.bids[0]?.price
  const spreadPercent = (spread / orderBookData.lastPrice) * 100

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Activity className="w-5 h-5 mr-2 text-cyan-400" />
            Order Book
          </h3>
          <div className="flex items-center space-x-4 mt-1">
            <span className="text-sm text-slate-400">{orderBookData.symbol}</span>
            <div className={`text-sm font-semibold ${
              orderBookData.change >= 0 ? 'text-green-400' : 'text-red-400'
            }`}>
              ₹{orderBookData.lastPrice.toFixed(2)}
            </div>
            <div className={`text-xs ${
              orderBookData.change >= 0 ? 'text-green-400' : 'text-red-400'
            }`}>
              {orderBookData.change >= 0 ? '+' : ''}{orderBookData.changePercent.toFixed(2)}%
            </div>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <div className="text-right">
            <div className="text-xs text-slate-400">Volume</div>
            <div className="text-sm font-semibold text-white">{orderBookData.volume}</div>
          </div>
          <BarChart3 className="w-4 h-4 text-slate-400" />
        </div>
      </div>

      {/* Spread Info */}
      <div className="flex items-center justify-center mb-4 p-3 rounded-xl bg-slate-800/30">
        <div className="text-center">
          <div className="text-xs text-slate-400">Spread</div>
          <div className="text-sm font-semibold text-orange-400">
            ₹{spread.toFixed(2)} ({spreadPercent.toFixed(3)}%)
          </div>
        </div>
      </div>

      {/* Order Book Table */}
      <div className="grid grid-cols-3 text-xs text-slate-400 mb-2">
        <div className="text-left">Price</div>
        <div className="text-right">Quantity</div>
        <div className="text-right">Total</div>
      </div>

      <div className="space-y-1 max-h-64 overflow-y-auto custom-scrollbar">
        {/* Asks (Sell Orders) - Reverse order to show highest prices first */}
        {[...orderBookData.asks].reverse().map((ask, index) => (
          <div 
            key={`ask-${index}`}
            onClick={() => setSelectedPrice(ask.price)}
            className={`relative grid grid-cols-3 text-xs py-1.5 px-2 rounded cursor-pointer transition-all hover:bg-red-500/10 ${
              selectedPrice === ask.price ? 'bg-red-500/20 ring-1 ring-red-500/50' : ''
            }`}
          >
            {/* Background bar for asks */}
            <div 
              className="absolute inset-0 bg-red-500/10 rounded"
              style={{ 
                width: `${(ask.total / maxTotal) * 100}%`,
                right: 0 
              }}
            />
            <div className="relative text-red-400 font-mono">₹{ask.price.toFixed(2)}</div>
            <div className="relative text-right text-white">{ask.quantity}</div>
            <div className="relative text-right text-slate-300">{ask.total}</div>
          </div>
        ))}

        {/* Current Price Divider */}
        <div className="flex items-center py-2 my-2">
          <div className="flex-1 h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent" />
          <div className={`mx-4 text-sm font-bold ${
            orderBookData.change >= 0 ? 'text-green-400' : 'text-red-400'
          }`}>
            ₹{orderBookData.lastPrice.toFixed(2)}
          </div>
          <div className="flex-1 h-px bg-gradient-to-r from-purple-500/50 via-transparent to-transparent" />
        </div>

        {/* Bids (Buy Orders) */}
        {orderBookData.bids.map((bid, index) => (
          <div 
            key={`bid-${index}`}
            onClick={() => setSelectedPrice(bid.price)}
            className={`relative grid grid-cols-3 text-xs py-1.5 px-2 rounded cursor-pointer transition-all hover:bg-green-500/10 ${
              selectedPrice === bid.price ? 'bg-green-500/20 ring-1 ring-green-500/50' : ''
            }`}
          >
            {/* Background bar for bids */}
            <div 
              className="absolute inset-0 bg-green-500/10 rounded"
              style={{ 
                width: `${(bid.total / maxTotal) * 100}%`,
                left: 0 
              }}
            />
            <div className="relative text-green-400 font-mono">₹{bid.price.toFixed(2)}</div>
            <div className="relative text-right text-white">{bid.quantity}</div>
            <div className="relative text-right text-slate-300">{bid.total}</div>
          </div>
        ))}
      </div>

      {/* Selected Price Info */}
      {selectedPrice && (
        <div className="mt-4 p-3 rounded-xl bg-slate-800/30 border border-purple-500/30">
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-400">Selected Price</span>
            <span className="text-sm font-bold text-white">₹{selectedPrice.toFixed(2)}</span>
          </div>
          <div className="flex items-center space-x-2 mt-2">
            <button 
              className="flex-1 py-2 px-3 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors text-xs font-semibold"
              onClick={() => console.log('Buy at', selectedPrice)}
            >
              Buy at ₹{selectedPrice.toFixed(2)}
            </button>
            <button 
              className="flex-1 py-2 px-3 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors text-xs font-semibold"
              onClick={() => console.log('Sell at', selectedPrice)}
            >
              Sell at ₹{selectedPrice.toFixed(2)}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}