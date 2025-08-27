import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  AlertCircle,
  Target,
  Shield,
  MoreVertical,
  Eye,
  Bell,
  X,
  Percent
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface Position {
  symbol: string
  companyName: string
  quantity: number
  avgPrice: number
  currentPrice: number
  pnl: number
  pnlPercent: number
  marketValue: number
  lastUpdate: Date
  dayChange: number
  dayChangePercent: number
  sector?: string
}

interface PositionCardProps {
  position: Position
  onClose: (symbol: string, quantity?: number) => void
  onModify: (symbol: string) => void
  onSetAlert: (symbol: string) => void
  onSetStopLoss: (symbol: string, stopPrice: number) => void
  showActions?: boolean
}

interface PositionManagerProps {
  positions: Position[]
  onClosePosition: (symbol: string, quantity?: number) => void
  onModifyPosition: (symbol: string) => void
  onSetAlert: (symbol: string) => void
  onSetStopLoss: (symbol: string, stopPrice: number) => void
  sortBy?: 'pnl' | 'value' | 'alphabetical'
  filterBy?: 'all' | 'profit' | 'loss'
  compactMode?: boolean
  className?: string
}

const SwipeActions: React.FC<{
  onLeftSwipe: () => void
  onRightSwipe: () => void
  leftLabel: string
  rightLabel: string
  leftColor: string
  rightColor: string
  children: React.ReactNode
}> = ({ onLeftSwipe, onRightSwipe, leftLabel, rightLabel, leftColor, rightColor, children }) => {
  const [dragOffset, setDragOffset] = useState(0)
  const [isDragging, setIsDragging] = useState(false)

  const handleDrag = (_: any, info: any) => {
    setDragOffset(info.offset.x)
    setIsDragging(true)
  }

  const handleDragEnd = (_: any, info: any) => {
    const threshold = 100
    
    if (info.offset.x > threshold) {
      onRightSwipe()
    } else if (info.offset.x < -threshold) {
      onLeftSwipe()
    }
    
    setDragOffset(0)
    setIsDragging(false)
  }

  return (
    <div className="relative overflow-hidden rounded-xl">
      {/* Left Swipe Action */}
      <motion.div
        animate={{
          x: Math.max(0, dragOffset),
          opacity: dragOffset > 50 ? 1 : 0.5
        }}
        className={`absolute inset-y-0 left-0 right-1/2 flex items-center justify-center ${leftColor} z-0`}
      >
        <span className="text-white font-semibold text-sm">{leftLabel}</span>
      </motion.div>

      {/* Right Swipe Action */}
      <motion.div
        animate={{
          x: Math.min(0, dragOffset),
          opacity: dragOffset < -50 ? 1 : 0.5
        }}
        className={`absolute inset-y-0 right-0 left-1/2 flex items-center justify-center ${rightColor} z-0`}
      >
        <span className="text-white font-semibold text-sm">{rightLabel}</span>
      </motion.div>

      {/* Main Content */}
      <motion.div
        drag="x"
        dragConstraints={{ left: -200, right: 200 }}
        dragElastic={0.2}
        onDrag={handleDrag}
        onDragEnd={handleDragEnd}
        animate={{ x: dragOffset }}
        className="relative z-10 bg-slate-800/90 backdrop-blur-sm"
      >
        {children}
      </motion.div>
    </div>
  )
}

const PositionCard: React.FC<PositionCardProps> = ({
  position,
  onClose,
  onModify,
  onSetAlert,
  onSetStopLoss,
  showActions = true
}) => {
  const [showQuickActions, setShowQuickActions] = useState(false)
  const [showStopLossModal, setShowStopLossModal] = useState(false)
  const [stopLossPrice, setStopLossPrice] = useState<number>(position.avgPrice * 0.95)

  const isProfit = position.pnl >= 0
  const hasAlert = Math.random() > 0.7 // Mock alert status

  const handleClosePercent = (percent: number) => {
    const quantity = Math.floor(position.quantity * (percent / 100))
    onClose(position.symbol, quantity)
    setShowQuickActions(false)
  }

  const handleSetStopLoss = () => {
    onSetStopLoss(position.symbol, stopLossPrice)
    setShowStopLossModal(false)
  }

  return (
    <>
      <SwipeActions
        onLeftSwipe={() => onClose(position.symbol)}
        onRightSwipe={() => onSetAlert(position.symbol)}
        leftLabel="Close Position"
        rightLabel="Set Alert"
        leftColor="bg-red-600"
        rightColor="bg-blue-600"
      >
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 rounded-xl bg-slate-800/50 hover:bg-slate-700/50 transition-all duration-300 border border-slate-700/50"
        >
          {/* Header */}
          <div className="flex items-start justify-between mb-3">
            <div className="flex-1">
              <div className="flex items-center space-x-2">
                <h3 className="text-lg font-bold text-white">{position.symbol}</h3>
                {hasAlert && (
                  <Bell className="h-4 w-4 text-yellow-400" />
                )}
              </div>
              <p className="text-sm text-slate-400 truncate">{position.companyName}</p>
              {position.sector && (
                <span className="inline-block px-2 py-1 mt-1 text-xs bg-slate-700 text-slate-300 rounded-full">
                  {position.sector}
                </span>
              )}
            </div>

            {showActions && (
              <button
                onClick={() => setShowQuickActions(!showQuickActions)}
                className="p-2 hover:bg-slate-600 rounded-lg transition-colors"
              >
                <MoreVertical className="h-4 w-4 text-slate-400" />
              </button>
            )}
          </div>

          {/* Position Details */}
          <div className="grid grid-cols-2 gap-4 mb-3">
            <div>
              <div className="text-xs text-slate-400 mb-1">Quantity & Avg Price</div>
              <div className="text-sm text-white">
                <span className="font-semibold">{position.quantity}</span> @ ₹{position.avgPrice.toFixed(2)}
              </div>
            </div>
            <div>
              <div className="text-xs text-slate-400 mb-1">Current Price</div>
              <div className="flex items-center space-x-2">
                <span className="text-sm font-semibold text-white">
                  ₹{position.currentPrice.toFixed(2)}
                </span>
                <span className={`text-xs flex items-center ${
                  position.dayChange >= 0 ? 'text-green-400' : 'text-red-400'
                }`}>
                  {position.dayChange >= 0 ? (
                    <TrendingUp className="h-3 w-3 mr-1" />
                  ) : (
                    <TrendingDown className="h-3 w-3 mr-1" />
                  )}
                  {position.dayChangePercent.toFixed(2)}%
                </span>
              </div>
            </div>
          </div>

          {/* P&L and Market Value */}
          <div className="grid grid-cols-2 gap-4 mb-3">
            <div>
              <div className="text-xs text-slate-400 mb-1">P&L</div>
              <div className={`text-lg font-bold ${isProfit ? 'text-green-400' : 'text-red-400'}`}>
                {isProfit ? '+' : ''}₹{Math.abs(position.pnl).toLocaleString('en-IN')}
              </div>
              <div className={`text-sm ${isProfit ? 'text-green-400' : 'text-red-400'}`}>
                ({isProfit ? '+' : ''}{position.pnlPercent.toFixed(2)}%)
              </div>
            </div>
            <div>
              <div className="text-xs text-slate-400 mb-1">Market Value</div>
              <div className="text-lg font-bold text-white">
                ₹{position.marketValue.toLocaleString('en-IN')}
              </div>
              <div className="text-sm text-slate-400">
                Investment: ₹{(position.quantity * position.avgPrice).toLocaleString('en-IN')}
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <AnimatePresence>
            {showQuickActions && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="pt-3 border-t border-slate-700"
              >
                <div className="grid grid-cols-2 gap-2 mb-3">
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => handleClosePercent(25)}
                    className="h-10 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Sell 25%
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => handleClosePercent(50)}
                    className="h-10 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Sell 50%
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => handleClosePercent(100)}
                    className="h-10 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Sell All
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setShowStopLossModal(true)}
                    className="h-10 bg-yellow-600/20 hover:bg-yellow-600/30 text-yellow-400 rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Stop Loss
                  </motion.button>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => onSetAlert(position.symbol)}
                    className="h-10 bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Set Alert
                  </motion.button>
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => onModify(position.symbol)}
                    className="h-10 bg-slate-600 hover:bg-slate-500 text-white rounded-lg font-medium text-sm transition-all duration-200"
                  >
                    Modify
                  </motion.button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Last Update */}
          <div className="mt-3 pt-2 border-t border-slate-700/50 text-xs text-slate-500 text-center">
            Updated {position.lastUpdate.toLocaleTimeString('en-IN', { 
              hour: '2-digit', 
              minute: '2-digit' 
            })}
          </div>
        </motion.div>
      </SwipeActions>

      {/* Stop Loss Modal */}
      <AnimatePresence>
        {showStopLossModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowStopLossModal(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-slate-800 rounded-2xl border border-slate-700 w-full max-w-sm"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6">
                <div className="flex items-center space-x-3 mb-4">
                  <Shield className="h-6 w-6 text-yellow-400" />
                  <h3 className="text-lg font-bold text-white">Set Stop Loss</h3>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">
                      Stop Loss Price for {position.symbol}
                    </label>
                    <input
                      type="number"
                      value={stopLossPrice}
                      onChange={(e) => setStopLossPrice(Number(e.target.value))}
                      className="w-full h-12 bg-slate-700 border border-slate-600 rounded-lg text-white px-4 text-lg focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                      step={0.05}
                    />
                  </div>

                  <div className="bg-slate-700/50 rounded-lg p-3 space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Current Price:</span>
                      <span className="text-white">₹{position.currentPrice.toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Your Avg Price:</span>
                      <span className="text-white">₹{position.avgPrice.toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Potential Loss:</span>
                      <span className={`font-semibold ${
                        (position.avgPrice - stopLossPrice) >= 0 ? 'text-red-400' : 'text-green-400'
                      }`}>
                        ₹{Math.abs((position.avgPrice - stopLossPrice) * position.quantity).toLocaleString('en-IN')}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex space-x-3 mt-6">
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setShowStopLossModal(false)}
                    className="flex-1 h-12 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-semibold transition-all duration-200"
                  >
                    Cancel
                  </motion.button>
                  
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={handleSetStopLoss}
                    className="flex-1 h-12 bg-yellow-600 hover:bg-yellow-700 text-white rounded-lg font-semibold transition-all duration-200"
                  >
                    Set Stop Loss
                  </motion.button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}

export const PositionManager: React.FC<PositionManagerProps> = ({
  positions,
  onClosePosition,
  onModifyPosition,
  onSetAlert,
  onSetStopLoss,
  sortBy = 'pnl',
  filterBy = 'all',
  compactMode = false,
  className = ''
}) => {
  const [activeSort, setActiveSort] = useState(sortBy)
  const [activeFilter, setActiveFilter] = useState(filterBy)

  const { isConnected } = useConnectionStatus()

  // Mock positions if none provided
  const mockPositions: Position[] = positions.length > 0 ? positions : [
    {
      symbol: 'RELIANCE',
      companyName: 'Reliance Industries Limited',
      quantity: 100,
      avgPrice: 2320.50,
      currentPrice: 2456.75,
      pnl: 13625,
      pnlPercent: 5.87,
      marketValue: 245675,
      dayChange: 34.50,
      dayChangePercent: 1.42,
      sector: 'Oil & Gas',
      lastUpdate: new Date()
    },
    {
      symbol: 'TCS',
      companyName: 'Tata Consultancy Services Limited',
      quantity: 50,
      avgPrice: 3850.25,
      currentPrice: 3789.40,
      pnl: -3042.5,
      pnlPercent: -1.58,
      marketValue: 189470,
      dayChange: -42.15,
      dayChangePercent: -1.10,
      sector: 'Information Technology',
      lastUpdate: new Date()
    },
    {
      symbol: 'HDFCBANK',
      companyName: 'HDFC Bank Limited',
      quantity: 200,
      avgPrice: 1650.80,
      currentPrice: 1687.25,
      pnl: 7290,
      pnlPercent: 2.21,
      marketValue: 337450,
      dayChange: 18.90,
      dayChangePercent: 1.13,
      sector: 'Banking',
      lastUpdate: new Date()
    }
  ]

  // Sort and filter positions
  const processedPositions = mockPositions
    .filter(position => {
      if (activeFilter === 'profit') return position.pnl > 0
      if (activeFilter === 'loss') return position.pnl < 0
      return true
    })
    .sort((a, b) => {
      switch (activeSort) {
        case 'pnl':
          return b.pnl - a.pnl
        case 'value':
          return b.marketValue - a.marketValue
        case 'alphabetical':
          return a.symbol.localeCompare(b.symbol)
        default:
          return 0
      }
    })

  // Portfolio summary
  const totalValue = mockPositions.reduce((sum, pos) => sum + pos.marketValue, 0)
  const totalPnL = mockPositions.reduce((sum, pos) => sum + pos.pnl, 0)
  const totalInvested = mockPositions.reduce((sum, pos) => sum + (pos.quantity * pos.avgPrice), 0)
  const totalPnLPercent = totalInvested > 0 ? (totalPnL / totalInvested) * 100 : 0

  const profitPositions = mockPositions.filter(p => p.pnl > 0).length
  const lossPositions = mockPositions.filter(p => p.pnl < 0).length

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`w-3 h-3 rounded-full ${
              isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
            }`} />
            <div>
              <h2 className="text-xl font-bold text-white">Your Positions</h2>
              <span className="text-sm text-slate-400">
                {processedPositions.length} positions
              </span>
            </div>
          </div>

          <div className="text-right">
            <div className={`text-lg font-bold ${totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {totalPnL >= 0 ? '+' : ''}₹{Math.abs(totalPnL).toLocaleString('en-IN')}
            </div>
            <div className={`text-sm ${totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              ({totalPnLPercent >= 0 ? '+' : ''}{totalPnLPercent.toFixed(2)}%)
            </div>
          </div>
        </div>

        {/* Portfolio Summary */}
        {!compactMode && (
          <div className="grid grid-cols-3 gap-4 text-center text-sm">
            <div>
              <div className="text-white font-bold text-lg">
                ₹{totalValue.toLocaleString('en-IN')}
              </div>
              <div className="text-slate-400">Total Value</div>
            </div>
            <div>
              <div className="text-green-400 font-bold text-lg">
                {profitPositions}
              </div>
              <div className="text-slate-400">Profitable</div>
            </div>
            <div>
              <div className="text-red-400 font-bold text-lg">
                {lossPositions}
              </div>
              <div className="text-slate-400">In Loss</div>
            </div>
          </div>
        )}
      </div>

      {/* Filters and Sort */}
      <div className="px-6 py-4 border-b border-slate-700/50">
        <div className="flex items-center justify-between">
          {/* Filter Buttons */}
          <div className="flex space-x-2">
            {(['all', 'profit', 'loss'] as const).map((filter) => (
              <motion.button
                key={filter}
                whileTap={{ scale: 0.95 }}
                onClick={() => setActiveFilter(filter)}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${
                  activeFilter === filter
                    ? 'bg-purple-600 text-white'
                    : 'bg-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                {filter === 'all' ? 'All' : filter === 'profit' ? 'Profit' : 'Loss'}
              </motion.button>
            ))}
          </div>

          {/* Sort Buttons */}
          <div className="flex space-x-2">
            {(['pnl', 'value', 'alphabetical'] as const).map((sort) => (
              <motion.button
                key={sort}
                whileTap={{ scale: 0.95 }}
                onClick={() => setActiveSort(sort)}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${
                  activeSort === sort
                    ? 'bg-cyan-600 text-white'
                    : 'bg-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                {sort === 'pnl' ? 'P&L' : sort === 'value' ? 'Value' : 'A-Z'}
              </motion.button>
            ))}
          </div>
        </div>
      </div>

      {/* Positions List */}
      <div className="p-6 space-y-4 max-h-96 overflow-y-auto">
        <AnimatePresence mode="popLayout">
          {processedPositions.map((position) => (
            <PositionCard
              key={position.symbol}
              position={position}
              onClose={onClosePosition}
              onModify={onModifyPosition}
              onSetAlert={onSetAlert}
              onSetStopLoss={onSetStopLoss}
              showActions={!compactMode}
            />
          ))}
        </AnimatePresence>

        {processedPositions.length === 0 && (
          <div className="text-center py-8 text-slate-400">
            <Eye className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No positions found</p>
            <p className="text-sm mt-1">
              {activeFilter === 'profit' ? 'No profitable positions' : 
               activeFilter === 'loss' ? 'No positions in loss' : 
               'Start trading to see positions here'}
            </p>
          </div>
        )}
      </div>

      {/* Swipe Instructions */}
      {!compactMode && processedPositions.length > 0 && (
        <div className="px-6 pb-4">
          <div className="text-center text-xs text-slate-500">
            💡 Swipe left to close • Swipe right to set alerts
          </div>
        </div>
      )}
    </motion.div>
  )
}