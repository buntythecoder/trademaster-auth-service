import React, { useState, useEffect, useCallback, useRef } from 'react'
import { motion, PanInfo, useDragControls } from 'framer-motion'
import {
  Plus, Minus, TrendingUp, TrendingDown, Activity, DollarSign, Percent,
  Volume2, VolumeX, Mic, MicOff, Eye, Target, Zap, ChevronUp, ChevronDown,
  ChevronLeft, ChevronRight, Play, Pause, RotateCcw, Settings, Menu,
  Smartphone, Vibrate, TouchIcon, Gesture, ArrowUp, ArrowDown, SwipeUp
} from 'lucide-react'

interface Stock {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  bid: number
  ask: number
}

interface Position {
  symbol: string
  quantity: number
  avgPrice: number
  currentPrice: number
  pnl: number
  pnlPercent: number
}

interface GestureConfig {
  swipeThreshold: number
  holdDuration: number
  doubleTapTimeout: number
  hapticFeedback: boolean
  voiceEnabled: boolean
  quickActionsEnabled: boolean
}

interface OneThumbTradingInterfaceProps {
  onTrade?: (action: 'buy' | 'sell', symbol: string, quantity: number) => void
  onVoiceCommand?: (command: string) => void
}

export const OneThumbTradingInterface: React.FC<OneThumbTradingInterfaceProps> = ({
  onTrade,
  onVoiceCommand
}) => {
  const [currentStock, setCurrentStock] = useState<Stock | null>(null)
  const [positions, setPositions] = useState<Position[]>([])
  const [watchlist, setWatchlist] = useState<Stock[]>([])
  const [isListening, setIsListening] = useState(false)
  const [gestureConfig, setGestureConfig] = useState<GestureConfig>({
    swipeThreshold: 100,
    holdDuration: 1000,
    doubleTapTimeout: 300,
    hapticFeedback: true,
    voiceEnabled: true,
    quickActionsEnabled: true
  })
  const [activeView, setActiveView] = useState<'trading' | 'positions' | 'watchlist' | 'settings'>('trading')
  const [quickActionVisible, setQuickActionVisible] = useState(false)
  const [lastTap, setLastTap] = useState<number>(0)
  const [holdTimer, setHoldTimer] = useState<NodeJS.Timeout | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [isTrading, setIsTrading] = useState(false)
  const [notifications, setNotifications] = useState<string[]>([])
  
  const dragControls = useDragControls()
  const recognitionRef = useRef<SpeechRecognition | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    loadMockData()
    initializeSpeechRecognition()
    setupTouchEvents()
    
    const interval = setInterval(updateMarketData, 2000)
    return () => {
      clearInterval(interval)
      cleanup()
    }
  }, [])

  const loadMockData = useCallback(() => {
    const mockWatchlist: Stock[] = [
      {
        symbol: 'RELIANCE',
        name: 'Reliance Industries',
        price: 2678.50,
        change: 45.20,
        changePercent: 1.72,
        volume: 2547893,
        high: 2695.00,
        low: 2642.30,
        bid: 2678.00,
        ask: 2679.00
      },
      {
        symbol: 'TCS',
        name: 'Tata Consultancy Services',
        price: 3542.80,
        change: -12.40,
        changePercent: -0.35,
        volume: 1876543,
        high: 3565.20,
        low: 3530.50,
        bid: 3542.50,
        ask: 3543.00
      },
      {
        symbol: 'HDFCBANK',
        name: 'HDFC Bank',
        price: 1634.70,
        change: 8.30,
        changePercent: 0.51,
        volume: 3456789,
        high: 1642.80,
        low: 1625.40,
        bid: 1634.50,
        ask: 1635.00
      },
      {
        symbol: 'INFY',
        name: 'Infosys Limited',
        price: 1523.45,
        change: 23.60,
        changePercent: 1.57,
        volume: 1234567,
        high: 1535.20,
        low: 1510.80,
        bid: 1523.20,
        ask: 1523.80
      }
    ]

    const mockPositions: Position[] = [
      {
        symbol: 'RELIANCE',
        quantity: 50,
        avgPrice: 2450.00,
        currentPrice: 2678.50,
        pnl: 11425.00,
        pnlPercent: 9.33
      },
      {
        symbol: 'TCS',
        quantity: 25,
        avgPrice: 3250.00,
        currentPrice: 3542.80,
        pnl: 7320.00,
        pnlPercent: 9.01
      }
    ]

    setWatchlist(mockWatchlist)
    setCurrentStock(mockWatchlist[0])
    setPositions(mockPositions)
  }, [])

  const updateMarketData = useCallback(() => {
    setWatchlist(prev => prev.map(stock => ({
      ...stock,
      price: stock.price + (Math.random() - 0.5) * 10,
      change: stock.change + (Math.random() - 0.5) * 2
    })))
  }, [])

  const initializeSpeechRecognition = useCallback(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = (window as any).webkitSpeechRecognition || (window as any).SpeechRecognition
      recognitionRef.current = new SpeechRecognition()
      recognitionRef.current.continuous = false
      recognitionRef.current.interimResults = false
      recognitionRef.current.lang = 'en-US'

      recognitionRef.current.onresult = (event: any) => {
        const command = event.results[0][0].transcript.toLowerCase()
        processVoiceCommand(command)
      }

      recognitionRef.current.onerror = () => {
        setIsListening(false)
        addNotification('Voice recognition error')
      }

      recognitionRef.current.onend = () => {
        setIsListening(false)
      }
    }
  }, [])

  const setupTouchEvents = useCallback(() => {
    const container = containerRef.current
    if (!container) return

    let touchStartY = 0
    let touchStartX = 0

    const handleTouchStart = (e: TouchEvent) => {
      touchStartY = e.touches[0].clientY
      touchStartX = e.touches[0].clientX
    }

    const handleTouchEnd = (e: TouchEvent) => {
      const touchEndY = e.changedTouches[0].clientY
      const touchEndX = e.changedTouches[0].clientX
      const deltaY = touchStartY - touchEndY
      const deltaX = touchStartX - touchEndX

      // Vertical swipes
      if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > gestureConfig.swipeThreshold) {
        if (deltaY > 0) {
          handleSwipeUp()
        } else {
          handleSwipeDown()
        }
      }

      // Horizontal swipes
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > gestureConfig.swipeThreshold) {
        if (deltaX > 0) {
          handleSwipeLeft()
        } else {
          handleSwipeRight()
        }
      }
    }

    container.addEventListener('touchstart', handleTouchStart, { passive: true })
    container.addEventListener('touchend', handleTouchEnd, { passive: true })

    return () => {
      container.removeEventListener('touchstart', handleTouchStart)
      container.removeEventListener('touchend', handleTouchEnd)
    }
  }, [gestureConfig.swipeThreshold])

  const cleanup = useCallback(() => {
    if (holdTimer) clearTimeout(holdTimer)
    if (recognitionRef.current) {
      recognitionRef.current.stop()
    }
  }, [holdTimer])

  const triggerHaptic = useCallback((intensity: 'light' | 'medium' | 'heavy' = 'light') => {
    if (gestureConfig.hapticFeedback && 'vibrate' in navigator) {
      const patterns = {
        light: [10],
        medium: [20],
        heavy: [30, 10, 30]
      }
      navigator.vibrate(patterns[intensity])
    }
  }, [gestureConfig.hapticFeedback])

  const addNotification = useCallback((message: string) => {
    setNotifications(prev => [...prev, message])
    setTimeout(() => {
      setNotifications(prev => prev.slice(1))
    }, 3000)
  }, [])

  const handleSwipeUp = useCallback(() => {
    triggerHaptic('light')
    if (activeView === 'trading') {
      setQuantity(prev => Math.min(prev + 1, 1000))
    } else {
      const currentIndex = watchlist.findIndex(stock => stock.symbol === currentStock?.symbol)
      if (currentIndex > 0) {
        setCurrentStock(watchlist[currentIndex - 1])
      }
    }
  }, [activeView, watchlist, currentStock, triggerHaptic])

  const handleSwipeDown = useCallback(() => {
    triggerHaptic('light')
    if (activeView === 'trading') {
      setQuantity(prev => Math.max(prev - 1, 1))
    } else {
      const currentIndex = watchlist.findIndex(stock => stock.symbol === currentStock?.symbol)
      if (currentIndex < watchlist.length - 1) {
        setCurrentStock(watchlist[currentIndex + 1])
      }
    }
  }, [activeView, watchlist, currentStock, triggerHaptic])

  const handleSwipeRight = useCallback(() => {
    if (!currentStock) return
    triggerHaptic('medium')
    executeTrade('buy', currentStock.symbol, quantity)
  }, [currentStock, quantity, triggerHaptic])

  const handleSwipeLeft = useCallback(() => {
    if (!currentStock) return
    triggerHaptic('medium')
    executeTrade('sell', currentStock.symbol, quantity)
  }, [currentStock, quantity, triggerHaptic])

  const executeTrade = useCallback(async (action: 'buy' | 'sell', symbol: string, qty: number) => {
    setIsTrading(true)
    triggerHaptic('heavy')
    
    try {
      // Simulate trade execution
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      addNotification(`${action.toUpperCase()} ${qty} ${symbol} - Order placed successfully`)
      
      if (onTrade) {
        onTrade(action, symbol, qty)
      }
      
      // Update positions
      if (action === 'buy') {
        setPositions(prev => {
          const existingPosition = prev.find(p => p.symbol === symbol)
          if (existingPosition) {
            return prev.map(p => 
              p.symbol === symbol 
                ? { ...p, quantity: p.quantity + qty }
                : p
            )
          } else {
            return [...prev, {
              symbol,
              quantity: qty,
              avgPrice: currentStock?.price || 0,
              currentPrice: currentStock?.price || 0,
              pnl: 0,
              pnlPercent: 0
            }]
          }
        })
      }
    } catch (error) {
      addNotification(`Failed to ${action} ${symbol}`)
      triggerHaptic('heavy')
    } finally {
      setIsTrading(false)
    }
  }, [currentStock, triggerHaptic, addNotification, onTrade])

  const processVoiceCommand = useCallback((command: string) => {
    const words = command.split(' ')
    
    if (words.includes('buy') || words.includes('purchase')) {
      const qty = parseInt(words.find(word => /^\d+$/.test(word)) || '1')
      const symbol = words.find(word => 
        watchlist.some(stock => stock.symbol.toLowerCase() === word.toLowerCase())
      )
      
      if (symbol && currentStock) {
        executeTrade('buy', symbol.toUpperCase(), qty)
      }
    } else if (words.includes('sell')) {
      const qty = parseInt(words.find(word => /^\d+$/.test(word)) || '1')
      const symbol = words.find(word => 
        watchlist.some(stock => stock.symbol.toLowerCase() === word.toLowerCase())
      )
      
      if (symbol && currentStock) {
        executeTrade('sell', symbol.toUpperCase(), qty)
      }
    } else if (words.includes('switch') || words.includes('show')) {
      const symbol = words.find(word => 
        watchlist.some(stock => stock.symbol.toLowerCase() === word.toLowerCase())
      )
      
      if (symbol) {
        const stock = watchlist.find(s => s.symbol.toLowerCase() === symbol.toLowerCase())
        if (stock) setCurrentStock(stock)
      }
    }

    if (onVoiceCommand) {
      onVoiceCommand(command)
    }
  }, [watchlist, currentStock, executeTrade, onVoiceCommand])

  const toggleVoiceRecognition = useCallback(() => {
    if (!gestureConfig.voiceEnabled) return

    if (isListening) {
      recognitionRef.current?.stop()
      setIsListening(false)
    } else {
      recognitionRef.current?.start()
      setIsListening(true)
      triggerHaptic('light')
    }
  }, [isListening, gestureConfig.voiceEnabled, triggerHaptic])

  const handleDoubleTap = useCallback(() => {
    const now = Date.now()
    if (now - lastTap < gestureConfig.doubleTapTimeout) {
      setQuickActionVisible(!quickActionVisible)
      triggerHaptic('medium')
    }
    setLastTap(now)
  }, [lastTap, gestureConfig.doubleTapTimeout, quickActionVisible, triggerHaptic])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(amount)
  }

  const formatPercent = (percent: number) => {
    return `${percent >= 0 ? '+' : ''}${percent.toFixed(2)}%`
  }

  if (!currentStock) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center">
        <motion.div 
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          className="w-8 h-8 border-2 border-purple-500 border-t-transparent rounded-full"
        />
      </div>
    )
  }

  return (
    <div 
      ref={containerRef}
      className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-4 select-none overflow-hidden"
      onDoubleClick={handleDoubleTap}
    >
      {/* Notifications */}
      <div className="fixed top-4 left-4 right-4 z-50 space-y-2">
        {notifications.map((notification, index) => (
          <motion.div
            key={index}
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className="bg-purple-600 text-white px-4 py-2 rounded-lg shadow-lg text-sm font-medium"
          >
            {notification}
          </motion.div>
        ))}
      </div>

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <Smartphone className="w-6 h-6 text-purple-400" />
          <h1 className="text-2xl font-bold text-white">One-Thumb Trading</h1>
        </div>
        
        <div className="flex items-center space-x-2">
          <button
            onClick={toggleVoiceRecognition}
            className={`p-3 rounded-full transition-all duration-200 ${
              isListening 
                ? 'bg-red-500 hover:bg-red-600' 
                : 'bg-purple-600 hover:bg-purple-700'
            }`}
            disabled={!gestureConfig.voiceEnabled}
          >
            {isListening ? <MicOff className="w-5 h-5 text-white" /> : <Mic className="w-5 h-5 text-white" />}
          </button>
        </div>
      </div>

      {/* Main Trading Area */}
      <div className="space-y-6">
        {/* Current Stock Display */}
        <motion.div 
          className="glass-card p-6 rounded-3xl text-center"
          whileTap={{ scale: 0.95 }}
        >
          <div className="mb-4">
            <h2 className="text-3xl font-bold text-white mb-1">{currentStock.symbol}</h2>
            <p className="text-slate-400 text-sm">{currentStock.name}</p>
          </div>
          
          <div className="mb-4">
            <div className="text-4xl font-bold text-white mb-2">
              {formatCurrency(currentStock.price)}
            </div>
            <div className={`text-xl font-semibold ${
              currentStock.change >= 0 ? 'text-green-400' : 'text-red-400'
            }`}>
              {formatCurrency(currentStock.change)} ({formatPercent(currentStock.changePercent)})
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-slate-400">High</p>
              <p className="text-white font-semibold">{formatCurrency(currentStock.high)}</p>
            </div>
            <div>
              <p className="text-slate-400">Low</p>
              <p className="text-white font-semibold">{formatCurrency(currentStock.low)}</p>
            </div>
          </div>
        </motion.div>

        {/* Quantity Selector */}
        <div className="glass-card p-4 rounded-2xl">
          <div className="flex items-center justify-between">
            <div className="text-slate-400 text-sm">Quantity</div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                className="p-2 bg-slate-700 hover:bg-slate-600 rounded-full transition-all duration-200"
              >
                <Minus className="w-4 h-4 text-white" />
              </button>
              
              <div className="text-2xl font-bold text-white w-16 text-center">
                {quantity}
              </div>
              
              <button
                onClick={() => setQuantity(Math.min(1000, quantity + 1))}
                className="p-2 bg-slate-700 hover:bg-slate-600 rounded-full transition-all duration-200"
              >
                <Plus className="w-4 h-4 text-white" />
              </button>
            </div>
          </div>
        </div>

        {/* Gesture Trading Buttons */}
        <div className="grid grid-cols-2 gap-4">
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onPanEnd={(event, info) => {
              if (info.offset.x > 50) {
                executeTrade('buy', currentStock.symbol, quantity)
              }
            }}
            className="glass-card p-8 rounded-2xl bg-gradient-to-br from-green-500/20 to-green-600/20 border-green-500/30"
            disabled={isTrading}
          >
            <div className="text-center">
              <ChevronRight className="w-8 h-8 text-green-400 mx-auto mb-3" />
              <div className="text-green-400 font-bold text-lg">BUY</div>
              <div className="text-slate-400 text-sm mt-2">Swipe Right →</div>
              <div className="text-white text-lg font-semibold mt-2">
                {formatCurrency(currentStock.price * quantity)}
              </div>
            </div>
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onPanEnd={(event, info) => {
              if (info.offset.x < -50) {
                executeTrade('sell', currentStock.symbol, quantity)
              }
            }}
            className="glass-card p-8 rounded-2xl bg-gradient-to-br from-red-500/20 to-red-600/20 border-red-500/30"
            disabled={isTrading}
          >
            <div className="text-center">
              <ChevronLeft className="w-8 h-8 text-red-400 mx-auto mb-3" />
              <div className="text-red-400 font-bold text-lg">SELL</div>
              <div className="text-slate-400 text-sm mt-2">← Swipe Left</div>
              <div className="text-white text-lg font-semibold mt-2">
                {formatCurrency(currentStock.price * quantity)}
              </div>
            </div>
          </motion.button>
        </div>

        {/* Gesture Instructions */}
        <div className="glass-card p-4 rounded-2xl">
          <h3 className="text-white font-semibold mb-3 flex items-center">
            <Gesture className="w-5 h-5 mr-2" />
            Gesture Commands
          </h3>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="flex items-center space-x-2">
              <SwipeUp className="w-4 h-4 text-purple-400" />
              <span className="text-slate-400">Swipe Up: +Qty</span>
            </div>
            <div className="flex items-center space-x-2">
              <ChevronDown className="w-4 h-4 text-purple-400" />
              <span className="text-slate-400">Swipe Down: -Qty</span>
            </div>
            <div className="flex items-center space-x-2">
              <ChevronRight className="w-4 h-4 text-green-400" />
              <span className="text-slate-400">Swipe Right: Buy</span>
            </div>
            <div className="flex items-center space-x-2">
              <ChevronLeft className="w-4 h-4 text-red-400" />
              <span className="text-slate-400">Swipe Left: Sell</span>
            </div>
          </div>
        </div>

        {/* Quick Navigation */}
        <div className="flex justify-center space-x-2">
          {watchlist.slice(0, 4).map((stock, index) => (
            <button
              key={stock.symbol}
              onClick={() => setCurrentStock(stock)}
              className={`p-3 rounded-full transition-all duration-200 ${
                currentStock.symbol === stock.symbol
                  ? 'bg-purple-600'
                  : 'bg-slate-700 hover:bg-slate-600'
              }`}
            >
              <span className="text-white text-xs font-medium">
                {stock.symbol.substring(0, 3)}
              </span>
            </button>
          ))}
        </div>

        {/* Voice Command Indicator */}
        {isListening && (
          <motion.div 
            className="glass-card p-4 rounded-2xl text-center"
            animate={{ scale: [1, 1.05, 1] }}
            transition={{ duration: 1, repeat: Infinity }}
          >
            <div className="flex items-center justify-center space-x-2 text-red-400">
              <Volume2 className="w-5 h-5" />
              <span className="font-medium">Listening for voice commands...</span>
            </div>
          </motion.div>
        )}

        {/* Trading Status */}
        {isTrading && (
          <motion.div 
            className="glass-card p-4 rounded-2xl text-center"
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 1, repeat: Infinity }}
          >
            <div className="flex items-center justify-center space-x-2 text-purple-400">
              <Activity className="w-5 h-5" />
              <span className="font-medium">Processing trade...</span>
            </div>
          </motion.div>
        )}
      </div>

      {/* Bottom Safe Area */}
      <div className="h-8"></div>
    </div>
  )
}

export default OneThumbTradingInterface