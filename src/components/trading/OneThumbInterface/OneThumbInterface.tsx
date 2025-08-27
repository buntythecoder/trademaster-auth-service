import React, { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence, PanInfo } from 'framer-motion'
import { 
  TrendingUp, TrendingDown, Mic, MicOff, ShoppingCart, X,
  ArrowUp, ArrowDown, DollarSign, Target, Shield, Zap,
  MoreHorizontal, Volume2, VolumeX, Eye, EyeOff,
  ChevronUp, ChevronDown, Clock, Activity, Layers
} from 'lucide-react'

// Types
export interface QuickOrderData {
  symbol: string
  side: 'BUY' | 'SELL'
  quantity: number
  orderType: 'MARKET' | 'LIMIT'
  price?: number
  triggerPrice?: number
  stopLoss?: number
  target?: number
}

export interface OneThumbSettings {
  enableVoice: boolean
  enableHaptics: boolean
  quickQuantities: number[]
  defaultOrderType: 'MARKET' | 'LIMIT'
  enableGestures: boolean
  soundEnabled: boolean
  autoHideUI: boolean
}

export interface VoiceCommand {
  command: string
  confidence: number
  action: 'BUY' | 'SELL' | 'CANCEL' | 'MODIFY' | 'PORTFOLIO'
  symbol?: string
  quantity?: number
}

interface OneThumbInterfaceProps {
  className?: string
  onQuickTrade?: (order: QuickOrderData) => Promise<void>
  onVoiceCommand?: (command: VoiceCommand) => void
  currentPrice?: number
  symbol?: string
  balance?: number
}

export const OneThumbInterface: React.FC<OneThumbInterfaceProps> = ({
  className = '',
  onQuickTrade,
  onVoiceCommand,
  currentPrice = 1250.50,
  symbol = 'RELIANCE',
  balance = 50000
}) => {
  // Core state
  const [isExpanded, setIsExpanded] = useState(false)
  const [activePanel, setActivePanel] = useState<'trade' | 'portfolio' | 'voice' | null>(null)
  const [quickQuantity, setQuickQuantity] = useState(1)
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT'>('MARKET')
  const [limitPrice, setLimitPrice] = useState(currentPrice)
  
  // Voice & interaction state
  const [isListening, setIsListening] = useState(false)
  const [voiceText, setVoiceText] = useState('')
  const [hapticEnabled, setHapticEnabled] = useState(true)
  const [soundEnabled, setSoundEnabled] = useState(true)
  
  // Gesture state
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  const [gestureActive, setGestureActive] = useState(false)
  const [swipeDirection, setSwipeDirection] = useState<'up' | 'down' | 'left' | 'right' | null>(null)
  
  // UI state
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [isProcessing, setIsProcessing] = useState(false)
  const [lastAction, setLastAction] = useState<string>('')
  
  // Refs
  const voiceRecognition = useRef<SpeechRecognition | null>(null)
  const interfaceRef = useRef<HTMLDivElement>(null)
  
  // Settings
  const [settings, setSettings] = useState<OneThumbSettings>({
    enableVoice: false,
    enableHaptics: true,
    quickQuantities: [1, 5, 10, 25, 50],
    defaultOrderType: 'MARKET',
    enableGestures: true,
    soundEnabled: true,
    autoHideUI: false
  })

  // Voice recognition setup
  useEffect(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.webkitSpeechRecognition || window.SpeechRecognition
      voiceRecognition.current = new SpeechRecognition()
      
      if (voiceRecognition.current) {
        voiceRecognition.current.continuous = true
        voiceRecognition.current.interimResults = true
        voiceRecognition.current.lang = 'en-US'
        
        voiceRecognition.current.onresult = (event) => {
          const results = Array.from(event.results)
          const transcript = results
            .map(result => result[0])
            .map(result => result.transcript)
            .join('')
          
          setVoiceText(transcript)
          
          // Process voice commands
          if (event.results[event.results.length - 1].isFinal) {
            processVoiceCommand(transcript)
          }
        }
        
        voiceRecognition.current.onerror = (event) => {
          console.error('Voice recognition error:', event.error)
          setIsListening(false)
        }
        
        voiceRecognition.current.onend = () => {
          setIsListening(false)
        }
      }
    }
  }, [])

  // Process voice commands
  const processVoiceCommand = (transcript: string) => {
    const command = transcript.toLowerCase().trim()
    
    // Extract trading commands
    if (command.includes('buy') || command.includes('purchase')) {
      const quantityMatch = command.match(/(\d+)/g)
      const quantity = quantityMatch ? parseInt(quantityMatch[0]) : 1
      
      const voiceCommand: VoiceCommand = {
        command: transcript,
        confidence: 0.9,
        action: 'BUY',
        symbol,
        quantity
      }
      
      onVoiceCommand?.(voiceCommand)
      triggerHapticFeedback()
    } else if (command.includes('sell')) {
      const quantityMatch = command.match(/(\d+)/g)
      const quantity = quantityMatch ? parseInt(quantityMatch[0]) : 1
      
      const voiceCommand: VoiceCommand = {
        command: transcript,
        confidence: 0.9,
        action: 'SELL',
        symbol,
        quantity
      }
      
      onVoiceCommand?.(voiceCommand)
      triggerHapticFeedback()
    }
    
    setVoiceText('')
  }

  // Haptic feedback
  const triggerHapticFeedback = (type: 'light' | 'medium' | 'heavy' = 'medium') => {
    if (hapticEnabled && 'vibrate' in navigator) {
      const patterns = {
        light: [10],
        medium: [15, 5, 15],
        heavy: [25, 10, 25, 10, 25]
      }
      navigator.vibrate(patterns[type])
    }
  }

  // Sound feedback
  const playSound = (type: 'success' | 'error' | 'tap' = 'tap') => {
    if (!soundEnabled) return
    
    const audioContext = new AudioContext()
    const frequencies = { success: 800, error: 300, tap: 600 }
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.value = frequencies[type]
    oscillator.type = 'sine'
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1)
    
    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + 0.1)
  }

  // Handle quick trade
  const handleQuickTrade = async (side: 'BUY' | 'SELL') => {
    setIsProcessing(true)
    
    const orderData: QuickOrderData = {
      symbol,
      side,
      quantity: quickQuantity,
      orderType,
      ...(orderType === 'LIMIT' && { price: limitPrice })
    }
    
    try {
      await onQuickTrade?.(orderData)
      setLastAction(`${side} ${quickQuantity} ${symbol}`)
      playSound('success')
      triggerHapticFeedback('heavy')
    } catch (error) {
      playSound('error')
      triggerHapticFeedback('light')
    } finally {
      setIsProcessing(false)
    }
  }

  // Handle voice toggle
  const toggleVoiceRecognition = () => {
    if (isListening) {
      voiceRecognition.current?.stop()
      setIsListening(false)
    } else {
      voiceRecognition.current?.start()
      setIsListening(true)
    }
    triggerHapticFeedback()
  }

  // Handle gesture swipe
  const handleDragEnd = (event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    const { offset, velocity } = info
    const threshold = 50
    const velocityThreshold = 500
    
    if (Math.abs(offset.x) > threshold || Math.abs(velocity.x) > velocityThreshold) {
      if (offset.x > 0) {
        setSwipeDirection('right')
        handleQuickTrade('BUY')
      } else {
        setSwipeDirection('left')
        handleQuickTrade('SELL')
      }
    } else if (Math.abs(offset.y) > threshold || Math.abs(velocity.y) > velocityThreshold) {
      if (offset.y < 0) {
        setSwipeDirection('up')
        setIsExpanded(true)
      } else {
        setSwipeDirection('down')
        setIsExpanded(false)
      }
    }
    
    setTimeout(() => setSwipeDirection(null), 300)
    setDragOffset({ x: 0, y: 0 })
  }

  return (
    <div className={`fixed bottom-0 left-0 right-0 z-50 ${className}`} ref={interfaceRef}>
      {/* Gesture feedback overlay */}
      <AnimatePresence>
        {swipeDirection && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 pointer-events-none z-40"
          >
            <div className={`absolute inset-0 flex items-center justify-center text-6xl ${
              swipeDirection === 'right' ? 'text-green-400' :
              swipeDirection === 'left' ? 'text-red-400' :
              swipeDirection === 'up' ? 'text-blue-400' : 'text-purple-400'
            }`}>
              {swipeDirection === 'right' && <TrendingUp />}
              {swipeDirection === 'left' && <TrendingDown />}
              {swipeDirection === 'up' && <ChevronUp />}
              {swipeDirection === 'down' && <ChevronDown />}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Voice feedback overlay */}
      <AnimatePresence>
        {isListening && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            className="fixed top-20 left-4 right-4 z-40"
          >
            <div className="glass-card rounded-2xl p-4 border border-blue-400/30">
              <div className="flex items-center space-x-3 mb-2">
                <motion.div
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                  className="w-4 h-4 rounded-full bg-red-500"
                />
                <span className="text-white font-medium">Listening...</span>
              </div>
              {voiceText && (
                <p className="text-slate-300 text-sm">{voiceText}</p>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main interface */}
      <motion.div
        drag={settings.enableGestures}
        dragConstraints={{ left: 0, right: 0, top: -200, bottom: 0 }}
        dragElastic={0.1}
        onDragEnd={handleDragEnd}
        className="relative"
      >
        {/* Expanded panels */}
        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ opacity: 0, y: 100 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 100 }}
              className="mb-2"
            >
              {/* Advanced controls */}
              {showAdvanced && (
                <div className="mx-4 mb-2 glass-card rounded-2xl p-4 space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold text-white">Advanced Settings</h3>
                    <button
                      onClick={() => setShowAdvanced(false)}
                      className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  
                  {/* Order type selector */}
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-300">Order Type</label>
                    <div className="flex space-x-2">
                      {['MARKET', 'LIMIT'].map((type) => (
                        <button
                          key={type}
                          onClick={() => setOrderType(type as 'MARKET' | 'LIMIT')}
                          className={`flex-1 p-3 rounded-xl font-medium transition-all ${
                            orderType === type
                              ? 'bg-gradient-to-r from-blue-600 to-cyan-600 text-white'
                              : 'bg-slate-800/50 text-slate-300 hover:bg-slate-700/50'
                          }`}
                        >
                          {type}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Limit price (if limit order) */}
                  {orderType === 'LIMIT' && (
                    <div className="space-y-2">
                      <label className="text-sm font-medium text-slate-300">Limit Price</label>
                      <input
                        type="number"
                        value={limitPrice}
                        onChange={(e) => setLimitPrice(parseFloat(e.target.value))}
                        className="w-full p-3 bg-slate-800/50 border border-slate-600/30 rounded-xl text-white focus:outline-none focus:border-blue-400/50"
                        placeholder="Enter limit price"
                      />
                    </div>
                  )}

                  {/* Settings toggles */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-slate-300">Voice Commands</span>
                      <button
                        onClick={() => setSettings(prev => ({ ...prev, enableVoice: !prev.enableVoice }))}
                        className={`w-12 h-6 rounded-full transition-colors ${
                          settings.enableVoice ? 'bg-green-500' : 'bg-slate-600'
                        }`}
                      >
                        <div className={`w-5 h-5 rounded-full bg-white transition-transform ${
                          settings.enableVoice ? 'translate-x-6' : 'translate-x-0.5'
                        }`} />
                      </button>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-slate-300">Haptic Feedback</span>
                      <button
                        onClick={() => setHapticEnabled(!hapticEnabled)}
                        className={`w-12 h-6 rounded-full transition-colors ${
                          hapticEnabled ? 'bg-green-500' : 'bg-slate-600'
                        }`}
                      >
                        <div className={`w-5 h-5 rounded-full bg-white transition-transform ${
                          hapticEnabled ? 'translate-x-6' : 'translate-x-0.5'
                        }`} />
                      </button>
                    </div>
                    
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-slate-300">Sound Effects</span>
                      <button
                        onClick={() => setSoundEnabled(!soundEnabled)}
                        className={`flex items-center space-x-2 p-2 rounded-lg transition-colors ${
                          soundEnabled ? 'bg-green-500/20 text-green-400' : 'bg-slate-700/50 text-slate-400'
                        }`}
                      >
                        {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* Quick quantity selector */}
              <div className="mx-4 mb-2 glass-card rounded-2xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-white">Quick Quantity</h3>
                  <span className="text-sm text-slate-400">{symbol} @ ₹{currentPrice}</span>
                </div>
                
                <div className="grid grid-cols-5 gap-2 mb-3">
                  {settings.quickQuantities.map((qty) => (
                    <button
                      key={qty}
                      onClick={() => {
                        setQuickQuantity(qty)
                        playSound('tap')
                        triggerHapticFeedback('light')
                      }}
                      className={`p-3 rounded-xl font-medium transition-all ${
                        quickQuantity === qty
                          ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white'
                          : 'bg-slate-800/50 text-slate-300 hover:bg-slate-700/50'
                      }`}
                    >
                      {qty}
                    </button>
                  ))}
                </div>
                
                <div className="flex items-center justify-between text-xs text-slate-400">
                  <span>Total: ₹{(quickQuantity * currentPrice).toLocaleString()}</span>
                  <span>Balance: ₹{balance.toLocaleString()}</span>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Main trading bar */}
        <div className="mx-4 mb-4 glass-card rounded-2xl p-4">
          {/* Status bar */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              <span className="text-sm font-medium text-white">{symbol}</span>
              <span className="text-sm text-slate-400">₹{currentPrice}</span>
            </div>
            
            <div className="flex items-center space-x-2">
              {/* Voice toggle */}
              {settings.enableVoice && (
                <button
                  onClick={toggleVoiceRecognition}
                  className={`p-2 rounded-lg transition-all ${
                    isListening 
                      ? 'bg-red-500/20 text-red-400' 
                      : 'bg-slate-700/50 text-slate-400 hover:text-white'
                  }`}
                >
                  {isListening ? <Mic className="w-4 h-4" /> : <MicOff className="w-4 h-4" />}
                </button>
              )}
              
              {/* Expand toggle */}
              <button
                onClick={() => {
                  setIsExpanded(!isExpanded)
                  triggerHapticFeedback('light')
                }}
                className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
              >
                {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
              </button>
              
              {/* Settings */}
              <button
                onClick={() => setShowAdvanced(!showAdvanced)}
                className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
              >
                <MoreHorizontal className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Last action display */}
          {lastAction && (
            <div className="mb-3 p-2 bg-green-500/10 border border-green-500/20 rounded-lg">
              <div className="flex items-center space-x-2">
                <Activity className="w-4 h-4 text-green-400" />
                <span className="text-sm text-green-400">Last: {lastAction}</span>
              </div>
            </div>
          )}

          {/* Main action buttons */}
          <div className="grid grid-cols-2 gap-3">
            {/* Buy button */}
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => handleQuickTrade('BUY')}
              disabled={isProcessing}
              className="relative flex items-center justify-center space-x-3 p-4 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 rounded-2xl font-bold text-white transition-all disabled:opacity-50 min-h-[60px]"
            >
              {isProcessing ? (
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                >
                  <Activity className="w-5 h-5" />
                </motion.div>
              ) : (
                <>
                  <TrendingUp className="w-5 h-5" />
                  <div className="text-center">
                    <div>BUY</div>
                    <div className="text-xs opacity-80">{quickQuantity} shares</div>
                  </div>
                </>
              )}
            </motion.button>

            {/* Sell button */}
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => handleQuickTrade('SELL')}
              disabled={isProcessing}
              className="relative flex items-center justify-center space-x-3 p-4 bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-700 hover:to-rose-700 rounded-2xl font-bold text-white transition-all disabled:opacity-50 min-h-[60px]"
            >
              {isProcessing ? (
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                >
                  <Activity className="w-5 h-5" />
                </motion.div>
              ) : (
                <>
                  <TrendingDown className="w-5 h-5" />
                  <div className="text-center">
                    <div>SELL</div>
                    <div className="text-xs opacity-80">{quickQuantity} shares</div>
                  </div>
                </>
              )}
            </motion.button>
          </div>

          {/* Gesture hint */}
          {settings.enableGestures && !isExpanded && (
            <div className="mt-3 text-center">
              <div className="flex items-center justify-center space-x-4 text-xs text-slate-500">
                <span>← Swipe for SELL</span>
                <span>Swipe for BUY →</span>
              </div>
              <div className="flex items-center justify-center space-x-2 text-xs text-slate-500 mt-1">
                <span>↑ Swipe up to expand</span>
              </div>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  )
}

export default OneThumbInterface