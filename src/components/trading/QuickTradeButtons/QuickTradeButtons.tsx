import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence, PanInfo } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  Zap,
  AlertTriangle,
  CheckCircle,
  Smartphone
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface QuickTradeProps {
  symbol: string
  currentPrice: number
  presetAmounts: number[]
  defaultAmount: number
  availableBalance: number
  currentPosition?: {
    quantity: number
    avgPrice: number
  }
  quickOrderType: 'MARKET' | 'LIMIT_BEST'
  riskWarnings: boolean
  onQuickTrade: (side: 'BUY' | 'SELL', amount: number) => Promise<void>
  className?: string
}

interface GestureConfig {
  swipeUpBuy: boolean
  swipeDownSell: boolean
  longPressAdvanced: boolean
  doubleTapConfirm: boolean
  hapticFeedback: boolean
}

const GestureIndicator: React.FC<{
  direction: 'up' | 'down'
  isActive: boolean
  label: string
}> = ({ direction, isActive, label }) => {
  return (
    <motion.div
      animate={{
        opacity: isActive ? 1 : 0.6,
        scale: isActive ? 1.05 : 1,
      }}
      className="flex flex-col items-center space-y-2"
    >
      <motion.div
        animate={{
          y: direction === 'up' ? (isActive ? -5 : 0) : (isActive ? 5 : 0)
        }}
        className={`text-2xl ${
          direction === 'up' ? 'text-green-400' : 'text-red-400'
        }`}
      >
        {direction === 'up' ? '↑' : '↓'}
      </motion.div>
      <span className="text-xs text-slate-400">{label}</span>
    </motion.div>
  )
}

const LastTradeInfo: React.FC<{
  lastTrade?: {
    symbol: string
    side: 'BUY' | 'SELL'
    quantity: number
    price: number
    timestamp: Date
  }
}> = ({ lastTrade }) => {
  if (!lastTrade) return null

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-slate-800/30 rounded-lg p-3 border border-slate-700/50"
    >
      <div className="text-xs text-slate-400 mb-1">Last Trade</div>
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          {lastTrade.side === 'BUY' ? (
            <TrendingUp className="h-3 w-3 text-green-400" />
          ) : (
            <TrendingDown className="h-3 w-3 text-red-400" />
          )}
          <span className="text-sm font-semibold text-white">
            {lastTrade.symbol} {lastTrade.side}
          </span>
        </div>
        <div className="text-right">
          <div className="text-sm font-semibold text-white">
            {lastTrade.quantity} @ ₹{lastTrade.price.toFixed(2)}
          </div>
          <div className="text-xs text-slate-400">
            {lastTrade.timestamp.toLocaleTimeString('en-IN', {
              hour: '2-digit',
              minute: '2-digit'
            })}
          </div>
        </div>
      </div>
    </motion.div>
  )
}

export const QuickTradeButtons: React.FC<QuickTradeProps> = ({
  symbol,
  currentPrice,
  presetAmounts,
  defaultAmount,
  availableBalance,
  currentPosition,
  quickOrderType,
  riskWarnings,
  onQuickTrade,
  className = ''
}) => {
  const [selectedAmount, setSelectedAmount] = useState(defaultAmount)
  const [isTrading, setIsTrading] = useState(false)
  const [dragDirection, setDragDirection] = useState<'up' | 'down' | null>(null)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [pendingTrade, setPendingTrade] = useState<{ side: 'BUY' | 'SELL' } | null>(null)
  const [lastTrade, setLastTrade] = useState<{
    symbol: string
    side: 'BUY' | 'SELL'
    quantity: number
    price: number
    timestamp: Date
  } | null>(null)

  const { isConnected } = useConnectionStatus()

  // Mock gesture config - in real app, this would come from user preferences
  const gestureConfig: GestureConfig = {
    swipeUpBuy: true,
    swipeDownSell: true,
    longPressAdvanced: true,
    doubleTapConfirm: false,
    hapticFeedback: true
  }

  // Calculate order quantities
  const buyQuantity = Math.floor(selectedAmount / currentPrice)
  const sellQuantity = currentPosition?.quantity || 0
  const canBuy = selectedAmount <= availableBalance && buyQuantity > 0
  const canSell = sellQuantity > 0

  // Haptic feedback simulation
  const triggerHapticFeedback = (type: 'light' | 'medium' | 'heavy' = 'medium') => {
    if (gestureConfig.hapticFeedback && 'vibrate' in navigator) {
      const patterns = {
        light: [10],
        medium: [20],
        heavy: [30, 10, 30]
      }
      navigator.vibrate(patterns[type])
    }
  }

  const handleQuickTrade = async (side: 'BUY' | 'SELL') => {
    if (isTrading || !isConnected) return
    
    const canProceed = side === 'BUY' ? canBuy : canSell
    if (!canProceed) return

    if (riskWarnings && selectedAmount > availableBalance * 0.2) {
      setPendingTrade({ side })
      setShowConfirmation(true)
      return
    }

    await executeTrade(side)
  }

  const executeTrade = async (side: 'BUY' | 'SELL') => {
    try {
      setIsTrading(true)
      triggerHapticFeedback('heavy')

      await onQuickTrade(side, selectedAmount)

      // Update last trade info
      setLastTrade({
        symbol,
        side,
        quantity: side === 'BUY' ? buyQuantity : sellQuantity,
        price: currentPrice,
        timestamp: new Date()
      })

      triggerHapticFeedback('light')
    } catch (error) {
      console.error('Quick trade failed:', error)
      triggerHapticFeedback('heavy')
    } finally {
      setIsTrading(false)
      setShowConfirmation(false)
      setPendingTrade(null)
    }
  }

  const handleDragStart = () => {
    triggerHapticFeedback('light')
  }

  const handleDrag = (_: any, info: PanInfo) => {
    const threshold = 50
    if (Math.abs(info.offset.y) > threshold) {
      const direction = info.offset.y < 0 ? 'up' : 'down'
      if (direction !== dragDirection) {
        setDragDirection(direction)
        triggerHapticFeedback('light')
      }
    } else {
      setDragDirection(null)
    }
  }

  const handleDragEnd = (_: any, info: PanInfo) => {
    const threshold = 100
    
    if (Math.abs(info.offset.y) > threshold) {
      const side = info.offset.y < 0 ? 'BUY' : 'SELL'
      handleQuickTrade(side)
    }
    
    setDragDirection(null)
  }

  return (
    <>
      <motion.div 
        className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        {/* Header */}
        <div className="p-4 border-b border-slate-700/50">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Zap className="h-5 w-5 text-yellow-400" />
              <div>
                <h3 className="text-lg font-bold text-white">Quick Trade</h3>
                <div className="flex items-center space-x-2 text-sm text-slate-400">
                  <Smartphone className="h-3 w-3" />
                  <span>Gesture Mode</span>
                </div>
              </div>
            </div>
            
            <div className={`w-3 h-3 rounded-full ${
              isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
            }`} />
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* Quick Buy Button */}
          <motion.div
            drag="y"
            dragConstraints={{ top: -200, bottom: 200 }}
            dragElastic={0.3}
            onDragStart={handleDragStart}
            onDrag={handleDrag}
            onDragEnd={handleDragEnd}
            whileTap={{ scale: 0.98 }}
            className="relative"
          >
            <motion.button
              onClick={() => handleQuickTrade('BUY')}
              disabled={!canBuy || isTrading || !isConnected}
              animate={{
                backgroundColor: dragDirection === 'up' 
                  ? '#16A34A' 
                  : canBuy ? '#22C55E' : '#64748B',
                scale: dragDirection === 'up' ? 1.05 : 1,
              }}
              className="w-full h-32 rounded-2xl font-bold text-xl text-white shadow-lg transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div className="flex flex-col items-center justify-center space-y-2">
                <div className="flex items-center space-x-3">
                  <TrendingUp className="h-8 w-8" />
                  <span>QUICK BUY</span>
                </div>
                <div className="text-lg opacity-90">
                  {buyQuantity} shares • ₹{selectedAmount.toLocaleString('en-IN')}
                </div>
                <div className="text-sm opacity-70">
                  {quickOrderType === 'MARKET' ? 'Market Price' : 'Best Limit'}
                </div>
              </div>
              
              {gestureConfig.swipeUpBuy && (
                <div className="absolute top-3 right-3">
                  <GestureIndicator 
                    direction="up" 
                    isActive={dragDirection === 'up'}
                    label="Swipe Up" 
                  />
                </div>
              )}
            </motion.button>
          </motion.div>

          {/* Quick Sell Button */}
          <motion.div
            drag="y"
            dragConstraints={{ top: -200, bottom: 200 }}
            dragElastic={0.3}
            onDragStart={handleDragStart}
            onDrag={handleDrag}
            onDragEnd={handleDragEnd}
            whileTap={{ scale: 0.98 }}
            className="relative"
          >
            <motion.button
              onClick={() => handleQuickTrade('SELL')}
              disabled={!canSell || isTrading || !isConnected}
              animate={{
                backgroundColor: dragDirection === 'down' 
                  ? '#DC2626' 
                  : canSell ? '#EF4444' : '#64748B',
                scale: dragDirection === 'down' ? 1.05 : 1,
              }}
              className="w-full h-32 rounded-2xl font-bold text-xl text-white shadow-lg transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div className="flex flex-col items-center justify-center space-y-2">
                <div className="flex items-center space-x-3">
                  <TrendingDown className="h-8 w-8" />
                  <span>QUICK SELL</span>
                </div>
                <div className="text-lg opacity-90">
                  {currentPosition ? (
                    <>
                      {sellQuantity} shares • ₹{(sellQuantity * currentPrice).toLocaleString('en-IN')}
                    </>
                  ) : (
                    'No Position'
                  )}
                </div>
                <div className="text-sm opacity-70">
                  {currentPosition ? 'Current Holding' : 'Buy first to sell'}
                </div>
              </div>

              {gestureConfig.swipeDownSell && (
                <div className="absolute bottom-3 right-3">
                  <GestureIndicator 
                    direction="down" 
                    isActive={dragDirection === 'down'}
                    label="Swipe Down" 
                  />
                </div>
              )}
            </motion.button>
          </motion.div>

          {/* Amount Selection */}
          <div className="space-y-3">
            <label className="block text-sm font-medium text-slate-300">
              Trade Amount
            </label>
            <div className="grid grid-cols-3 gap-3">
              {presetAmounts.map((amount) => (
                <motion.button
                  key={amount}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setSelectedAmount(amount)}
                  className={`h-12 rounded-lg font-semibold transition-all duration-200 ${
                    selectedAmount === amount
                      ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/30'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-white'
                  }`}
                >
                  ₹{(amount / 1000).toFixed(0)}K
                </motion.button>
              ))}
            </div>
          </div>

          {/* Custom Amount */}
          <div className="space-y-3">
            <label className="block text-sm font-medium text-slate-300">
              Custom Amount
            </label>
            <input
              type="number"
              value={selectedAmount}
              onChange={(e) => setSelectedAmount(Number(e.target.value) || 0)}
              className="w-full h-12 bg-slate-800 border border-slate-600 rounded-lg text-white px-4 text-lg font-semibold focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="₹0"
              step={1000}
              min={1000}
              max={availableBalance}
            />
          </div>

          {/* Last Trade Info */}
          <LastTradeInfo lastTrade={lastTrade} />

          {/* Trading Status */}
          <AnimatePresence>
            {isTrading && (
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className="bg-blue-900/20 border border-blue-700/50 rounded-lg p-4"
              >
                <div className="flex items-center space-x-3">
                  <div className="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
                  <div>
                    <div className="text-blue-300 font-semibold">Processing Trade...</div>
                    <div className="text-blue-400 text-sm">Please wait while we execute your order</div>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>

      {/* Risk Warning Confirmation Modal */}
      <AnimatePresence>
        {showConfirmation && pendingTrade && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowConfirmation(false)}
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
                  <AlertTriangle className="h-6 w-6 text-yellow-400" />
                  <h3 className="text-lg font-bold text-white">High Value Trade</h3>
                </div>
                
                <div className="space-y-3 text-sm text-slate-300 mb-6">
                  <p>
                    You're about to {pendingTrade.side.toLowerCase()} ₹{selectedAmount.toLocaleString('en-IN')} 
                    worth of {symbol}, which is {((selectedAmount / availableBalance) * 100).toFixed(1)}% 
                    of your available balance.
                  </p>
                  <p className="text-yellow-300">
                    Consider diversifying your investments across different assets to manage risk.
                  </p>
                </div>

                <div className="flex space-x-3">
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setShowConfirmation(false)}
                    className="flex-1 h-12 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-semibold transition-all duration-200"
                  >
                    Cancel
                  </motion.button>
                  
                  <motion.button
                    whileTap={{ scale: 0.98 }}
                    onClick={() => executeTrade(pendingTrade.side)}
                    className={`flex-1 h-12 rounded-lg font-semibold text-white transition-all duration-200 ${
                      pendingTrade.side === 'BUY'
                        ? 'bg-green-600 hover:bg-green-700'
                        : 'bg-red-600 hover:bg-red-700'
                    }`}
                  >
                    Proceed
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