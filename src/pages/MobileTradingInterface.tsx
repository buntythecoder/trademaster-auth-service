import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Smartphone, Layers, Zap, Activity, Target, Settings,
  TrendingUp, TrendingDown, BarChart3, PieChart, Clock,
  Shield, Volume2, Vibrate, Eye, Mic, Move, RotateCcw
} from 'lucide-react'
import { OneThumbInterface, GestureTrading } from '../components/trading'
import type { QuickOrderData, VoiceCommand, GestureAction } from '../components/trading'

// Mock data
const mockWatchlist = [
  { symbol: 'RELIANCE', price: 2456.75, change: 12.45, changePercent: 0.51 },
  { symbol: 'TCS', price: 3245.80, change: -23.20, changePercent: -0.71 },
  { symbol: 'INFY', price: 1456.90, change: 8.75, changePercent: 0.60 },
  { symbol: 'HDFC', price: 1678.45, change: -5.30, changePercent: -0.32 },
  { symbol: 'ICICIBANK', price: 987.60, change: 15.80, changePercent: 1.63 }
]

interface MobileTradingInterfaceProps {
  className?: string
}

export const MobileTradingInterface: React.FC<MobileTradingInterfaceProps> = ({
  className = ''
}) => {
  // Core state
  const [activeMode, setActiveMode] = useState<'one-thumb' | 'gesture' | 'traditional'>('one-thumb')
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE')
  const [currentPrice, setCurrentPrice] = useState(2456.75)
  
  // Interface state
  const [showModeSelector, setShowModeSelector] = useState(false)
  const [tradeHistory, setTradeHistory] = useState<any[]>([])
  const [gestureEnabled, setGestureEnabled] = useState(true)
  const [voiceEnabled, setVoiceEnabled] = useState(false)
  
  // Performance tracking
  const [sessionStats, setSessionStats] = useState({
    totalTrades: 0,
    successRate: 95,
    avgExecutionTime: 1.2,
    voiceCommands: 0,
    gesturesUsed: 0
  })

  // Update current price based on selected symbol
  useEffect(() => {
    const selected = mockWatchlist.find(item => item.symbol === selectedSymbol)
    if (selected) {
      setCurrentPrice(selected.price)
    }
  }, [selectedSymbol])

  // Handle quick trade execution
  const handleQuickTrade = async (orderData: QuickOrderData) => {
    console.log('Executing quick trade:', orderData)
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // Update session stats
    setSessionStats(prev => ({
      ...prev,
      totalTrades: prev.totalTrades + 1
    }))
    
    // Add to trade history
    setTradeHistory(prev => [{
      id: Date.now(),
      ...orderData,
      timestamp: new Date(),
      status: 'executed',
      executionPrice: currentPrice
    }, ...prev.slice(0, 9)])
  }

  // Handle voice command
  const handleVoiceCommand = (command: VoiceCommand) => {
    console.log('Voice command received:', command)
    
    setSessionStats(prev => ({
      ...prev,
      voiceCommands: prev.voiceCommands + 1
    }))
    
    // Process voice command based on action
    if (command.action === 'BUY' || command.action === 'SELL') {
      const orderData: QuickOrderData = {
        symbol: command.symbol || selectedSymbol,
        side: command.action,
        quantity: command.quantity || 1,
        orderType: 'MARKET'
      }
      handleQuickTrade(orderData)
    }
  }

  // Handle gesture action
  const handleGestureAction = (action: GestureAction) => {
    console.log('Gesture action received:', action)
    
    setSessionStats(prev => ({
      ...prev,
      gesturesUsed: prev.gesturesUsed + 1
    }))
    
    // Process gesture action
    if (action.type === 'BUY' || action.type === 'SELL') {
      const orderData: QuickOrderData = {
        symbol: action.symbol,
        side: action.type,
        quantity: action.quantity || 1,
        orderType: 'MARKET'
      }
      handleQuickTrade(orderData)
    }
  }

  return (
    <div className={`min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 ${className}`}>
      {/* Header */}
      <div className="sticky top-0 z-30 glass-card border-b border-slate-700/50">
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center space-x-3">
            <Smartphone className="w-6 h-6 text-blue-400" />
            <div>
              <h1 className="font-bold text-white">Mobile Trading</h1>
              <p className="text-sm text-slate-400">
                {activeMode === 'one-thumb' ? 'One-Thumb Mode' : 
                 activeMode === 'gesture' ? 'Gesture Mode' : 'Traditional Mode'}
              </p>
            </div>
          </div>
          
          <button
            onClick={() => setShowModeSelector(!showModeSelector)}
            className="p-2 glass-card rounded-lg text-slate-400 hover:text-white transition-colors"
          >
            <Settings className="w-5 h-5" />
          </button>
        </div>

        {/* Mode selector */}
        <AnimatePresence>
          {showModeSelector && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="border-t border-slate-700/50"
            >
              <div className="p-4 grid grid-cols-3 gap-3">
                {[
                  { 
                    id: 'one-thumb', 
                    label: 'One-Thumb', 
                    icon: Zap, 
                    description: 'Single-hand optimized' 
                  },
                  { 
                    id: 'gesture', 
                    label: 'Gesture', 
                    icon: Move, 
                    description: 'Swipe to trade' 
                  },
                  { 
                    id: 'traditional', 
                    label: 'Traditional', 
                    icon: Layers, 
                    description: 'Full interface' 
                  }
                ].map(({ id, label, icon: Icon, description }) => (
                  <button
                    key={id}
                    onClick={() => {
                      setActiveMode(id as typeof activeMode)
                      setShowModeSelector(false)
                    }}
                    className={`p-3 rounded-xl text-center transition-all ${
                      activeMode === id
                        ? 'bg-gradient-to-r from-blue-600 to-cyan-600 text-white'
                        : 'bg-slate-800/50 text-slate-300 hover:bg-slate-700/50'
                    }`}
                  >
                    <Icon className="w-5 h-5 mx-auto mb-1" />
                    <div className="text-xs font-medium">{label}</div>
                    <div className="text-xs opacity-70 mt-1">{description}</div>
                  </button>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Watchlist bar */}
      <div className="sticky top-16 z-20 bg-slate-800/80 backdrop-blur-sm border-b border-slate-700/50">
        <div className="flex overflow-x-auto scrollbar-hide p-2 space-x-2">
          {mockWatchlist.map((stock) => (
            <button
              key={stock.symbol}
              onClick={() => setSelectedSymbol(stock.symbol)}
              className={`flex-shrink-0 p-3 rounded-xl transition-all min-w-[120px] ${
                selectedSymbol === stock.symbol
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50'
              }`}
            >
              <div className="text-sm font-medium">{stock.symbol}</div>
              <div className="text-xs">₹{stock.price}</div>
              <div className={`text-xs ${
                stock.change >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {stock.change >= 0 ? '+' : ''}{stock.changePercent}%
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Main content */}
      <div className="p-4 pb-32">
        {/* Session stats */}
        <div className="mb-6 glass-card rounded-2xl p-4">
          <h3 className="font-semibold text-white mb-3 flex items-center space-x-2">
            <Activity className="w-4 h-4" />
            <span>Session Performance</span>
          </h3>
          
          <div className="grid grid-cols-2 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-400">{sessionStats.totalTrades}</div>
              <div className="text-xs text-slate-400">Total Trades</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-400">{sessionStats.successRate}%</div>
              <div className="text-xs text-slate-400">Success Rate</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-400">{sessionStats.avgExecutionTime}s</div>
              <div className="text-xs text-slate-400">Avg Execution</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-yellow-400">
                {sessionStats.voiceCommands + sessionStats.gesturesUsed}
              </div>
              <div className="text-xs text-slate-400">Smart Actions</div>
            </div>
          </div>
        </div>

        {/* Interface mode content */}
        <AnimatePresence mode="wait">
          {activeMode === 'gesture' && (
            <motion.div
              key="gesture"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3 }}
            >
              <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold text-white flex items-center space-x-2">
                    <Move className="w-4 h-4" />
                    <span>Gesture Trading</span>
                  </h3>
                  <div className="text-sm text-slate-400">
                    {selectedSymbol} @ ₹{currentPrice}
                  </div>
                </div>
                
                <GestureTrading
                  symbol={selectedSymbol}
                  currentPrice={currentPrice}
                  onGestureAction={handleGestureAction}
                  isEnabled={gestureEnabled}
                />
              </div>
            </motion.div>
          )}

          {activeMode === 'traditional' && (
            <motion.div
              key="traditional"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3 }}
              className="space-y-6"
            >
              {/* Traditional interface placeholder */}
              <div className="glass-card rounded-2xl p-6 text-center">
                <Layers className="w-12 h-12 mx-auto mb-4 text-slate-400" />
                <h3 className="font-semibold text-white mb-2">Traditional Interface</h3>
                <p className="text-slate-400 text-sm">
                  Full trading interface with all features and controls
                </p>
                <div className="mt-4 p-4 bg-slate-800/50 rounded-xl">
                  <p className="text-slate-500 text-xs">
                    This would contain the full trading interface with order forms, 
                    charts, position management, and advanced features.
                  </p>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Recent trades */}
        {tradeHistory.length > 0 && (
          <div className="glass-card rounded-2xl p-4">
            <h3 className="font-semibold text-white mb-3 flex items-center space-x-2">
              <Clock className="w-4 h-4" />
              <span>Recent Trades</span>
            </h3>
            
            <div className="space-y-2">
              {tradeHistory.slice(0, 5).map((trade) => (
                <div
                  key={trade.id}
                  className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg"
                >
                  <div className="flex items-center space-x-3">
                    {trade.side === 'BUY' ? (
                      <TrendingUp className="w-4 h-4 text-green-400" />
                    ) : (
                      <TrendingDown className="w-4 h-4 text-red-400" />
                    )}
                    <div>
                      <div className="text-sm font-medium text-white">
                        {trade.side} {trade.quantity} {trade.symbol}
                      </div>
                      <div className="text-xs text-slate-400">
                        @ ₹{trade.executionPrice}
                      </div>
                    </div>
                  </div>
                  
                  <div className="text-right">
                    <div className="text-sm text-white">
                      ₹{(trade.quantity * trade.executionPrice).toLocaleString()}
                    </div>
                    <div className="text-xs text-green-400">
                      {trade.status}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* One-thumb interface (always visible when active) */}
      {activeMode === 'one-thumb' && (
        <OneThumbInterface
          symbol={selectedSymbol}
          currentPrice={currentPrice}
          onQuickTrade={handleQuickTrade}
          onVoiceCommand={handleVoiceCommand}
          balance={50000}
        />
      )}
    </div>
  )
}

export default MobileTradingInterface