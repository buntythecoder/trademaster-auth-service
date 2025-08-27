import React, { useState, useRef, useEffect } from 'react'
import { motion, PanInfo, useMotionValue, useTransform } from 'framer-motion'
import { 
  TrendingUp, TrendingDown, RotateCcw, Move, ZapOff, Zap,
  ArrowUp, ArrowDown, ArrowLeft, ArrowRight, Target,
  Volume2, VolumeX, Vibrate, VolumeOff, Settings,
  Eye, EyeOff, Layers, Activity, Clock, AlertTriangle
} from 'lucide-react'

// Types
export interface GestureConfig {
  sensitivity: number // 0.5 - 2.0
  hapticIntensity: 'light' | 'medium' | 'heavy'
  soundEnabled: boolean
  visualFeedback: boolean
  confirmationRequired: boolean
  doubleTapEnabled: boolean
  longPressEnabled: boolean
  swipeThreshold: number // pixels
}

export interface GestureAction {
  type: 'BUY' | 'SELL' | 'CANCEL' | 'MODIFY' | 'PORTFOLIO'
  gesture: 'swipe-right' | 'swipe-left' | 'swipe-up' | 'swipe-down' | 'pinch' | 'double-tap' | 'long-press'
  symbol: string
  quantity?: number
  confidence: number
  timestamp: Date
}

export interface TouchPoint {
  id: number
  x: number
  y: number
  startTime: number
}

interface GestureTradingProps {
  className?: string
  onGestureAction?: (action: GestureAction) => void
  symbol?: string
  currentPrice?: number
  isEnabled?: boolean
}

export const GestureTrading: React.FC<GestureTradingProps> = ({
  className = '',
  onGestureAction,
  symbol = 'RELIANCE',
  currentPrice = 1250.50,
  isEnabled = true
}) => {
  // Core state
  const [gestureConfig, setGestureConfig] = useState<GestureConfig>({
    sensitivity: 1.0,
    hapticIntensity: 'medium',
    soundEnabled: true,
    visualFeedback: true,
    confirmationRequired: false,
    doubleTapEnabled: true,
    longPressEnabled: true,
    swipeThreshold: 50
  })
  
  // Gesture detection state
  const [touchPoints, setTouchPoints] = useState<TouchPoint[]>([])
  const [activeGesture, setActiveGesture] = useState<string>('')
  const [gestureConfidence, setGestureConfidence] = useState(0)
  const [lastGestureTime, setLastGestureTime] = useState(0)
  
  // UI state
  const [showFeedback, setShowFeedback] = useState('')
  const [isCalibrating, setIsCalibrating] = useState(false)
  const [gestureHistory, setGestureHistory] = useState<GestureAction[]>([])
  const [showSettings, setShowSettings] = useState(false)
  
  // Motion values for gesture feedback
  const x = useMotionValue(0)
  const y = useMotionValue(0)
  const scale = useMotionValue(1)
  const rotate = useMotionValue(0)
  
  // Transform motion values for visual feedback
  const backgroundColor = useTransform(
    x,
    [-200, -50, 0, 50, 200],
    ['#dc2626', '#ef4444', '#1f2937', '#10b981', '#059669']
  )
  
  const opacity = useTransform(
    scale,
    [0.8, 1, 1.2],
    [0.6, 1, 0.8]
  )
  
  // Refs
  const gestureAreaRef = useRef<HTMLDivElement>(null)
  const longPressTimer = useRef<NodeJS.Timeout>()
  const doubleTapTimer = useRef<NodeJS.Timeout>()
  const lastTapTime = useRef(0)

  // Haptic feedback
  const triggerHaptic = (intensity: 'light' | 'medium' | 'heavy' = gestureConfig.hapticIntensity) => {
    if ('vibrate' in navigator) {
      const patterns = {
        light: [10],
        medium: [15, 5, 15],
        heavy: [25, 10, 25, 10, 25]
      }
      navigator.vibrate(patterns[intensity])
    }
  }

  // Audio feedback
  const playGestureSound = (type: 'swipe' | 'tap' | 'success' | 'error') => {
    if (!gestureConfig.soundEnabled) return
    
    const audioContext = new AudioContext()
    const frequencies = { swipe: 400, tap: 800, success: 1000, error: 200 }
    
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.frequency.value = frequencies[type]
    oscillator.type = type === 'error' ? 'sawtooth' : 'sine'
    
    gainNode.gain.setValueAtTime(0.2, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2)
    
    oscillator.start()
    oscillator.stop(audioContext.currentTime + 0.2)
  }

  // Visual feedback
  const showVisualFeedback = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
    if (!gestureConfig.visualFeedback) return
    
    setShowFeedback(message)
    setTimeout(() => setShowFeedback(''), 2000)
  }

  // Detect gesture from swipe
  const detectSwipeGesture = (info: PanInfo): GestureAction | null => {
    const { offset, velocity, direction } = info
    const distance = Math.sqrt(offset.x * offset.x + offset.y * offset.y)
    const speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
    
    if (distance < gestureConfig.swipeThreshold && speed < 500) return null
    
    // Calculate confidence based on distance, speed, and direction consistency
    const confidence = Math.min(
      (distance / 200) * 0.4 + 
      (speed / 2000) * 0.4 + 
      (Math.abs(direction.x) > Math.abs(direction.y) ? 0.2 : 0),
      1.0
    ) * gestureConfig.sensitivity
    
    let gestureType: GestureAction['gesture']
    let actionType: GestureAction['type']
    
    if (Math.abs(offset.x) > Math.abs(offset.y)) {
      // Horizontal swipe
      if (offset.x > 0) {
        gestureType = 'swipe-right'
        actionType = 'BUY'
      } else {
        gestureType = 'swipe-left'
        actionType = 'SELL'
      }
    } else {
      // Vertical swipe
      if (offset.y < 0) {
        gestureType = 'swipe-up'
        actionType = 'PORTFOLIO'
      } else {
        gestureType = 'swipe-down'
        actionType = 'CANCEL'
      }
    }
    
    return {
      type: actionType,
      gesture: gestureType,
      symbol,
      quantity: 1,
      confidence,
      timestamp: new Date()
    }
  }

  // Handle drag end
  const handleDragEnd = (event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    const action = detectSwipeGesture(info)
    
    if (action && action.confidence > 0.3) {
      setActiveGesture(action.gesture)
      setGestureConfidence(action.confidence)
      
      // Add to history
      setGestureHistory(prev => [action, ...prev.slice(0, 9)])
      
      // Trigger feedback
      triggerHaptic()
      playGestureSound('swipe')
      showVisualFeedback(`${action.type} ${symbol}`, action.type === 'BUY' ? 'success' : 'info')
      
      // Execute if confidence is high enough or confirmation not required
      if (action.confidence > 0.7 || !gestureConfig.confirmationRequired) {
        onGestureAction?.(action)
        playGestureSound('success')
        triggerHaptic('heavy')
      }
    }
    
    // Reset motion values
    x.set(0)
    y.set(0)
    scale.set(1)
    rotate.set(0)
    
    setTimeout(() => setActiveGesture(''), 1000)
  }

  // Handle touch events for multi-touch gestures
  const handleTouchStart = (event: React.TouchEvent) => {
    const newTouchPoints: TouchPoint[] = Array.from(event.touches).map((touch, index) => ({
      id: touch.identifier,
      x: touch.clientX,
      y: touch.clientY,
      startTime: Date.now()
    }))
    
    setTouchPoints(newTouchPoints)
    
    // Long press detection
    if (gestureConfig.longPressEnabled && newTouchPoints.length === 1) {
      longPressTimer.current = setTimeout(() => {
        const action: GestureAction = {
          type: 'MODIFY',
          gesture: 'long-press',
          symbol,
          confidence: 1.0,
          timestamp: new Date()
        }
        
        onGestureAction?.(action)
        triggerHaptic('heavy')
        playGestureSound('tap')
        showVisualFeedback('Quick Settings', 'info')
        setShowSettings(true)
      }, 800)
    }
  }

  const handleTouchEnd = (event: React.TouchEvent) => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current)
    }
    
    // Double tap detection
    if (gestureConfig.doubleTapEnabled && touchPoints.length === 1) {
      const currentTime = Date.now()
      if (currentTime - lastTapTime.current < 300) {
        const action: GestureAction = {
          type: 'PORTFOLIO',
          gesture: 'double-tap',
          symbol,
          confidence: 1.0,
          timestamp: new Date()
        }
        
        onGestureAction?.(action)
        triggerHaptic('medium')
        playGestureSound('tap')
        showVisualFeedback('Portfolio View', 'info')
        
        // Clear double tap timer
        if (doubleTapTimer.current) {
          clearTimeout(doubleTapTimer.current)
        }
      } else {
        // Single tap - start double tap timer
        doubleTapTimer.current = setTimeout(() => {
          // Handle single tap if needed
        }, 300)
      }
      lastTapTime.current = currentTime
    }
    
    setTouchPoints([])
  }

  // Pinch gesture detection
  useEffect(() => {
    if (touchPoints.length === 2) {
      // Calculate distance between two touch points
      const distance = Math.sqrt(
        Math.pow(touchPoints[1].x - touchPoints[0].x, 2) +
        Math.pow(touchPoints[1].y - touchPoints[0].y, 2)
      )
      
      // Store initial distance for pinch detection
      // Implementation would continue based on distance changes
    }
  }, [touchPoints])

  // Gesture calibration
  const calibrateGestures = async () => {
    setIsCalibrating(true)
    showVisualFeedback('Calibrating gestures...', 'info')
    
    // Simulate calibration process
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    setIsCalibrating(false)
    showVisualFeedback('Calibration complete!', 'success')
    triggerHaptic('heavy')
  }

  if (!isEnabled) {
    return (
      <div className={`${className} flex items-center justify-center p-8`}>
        <div className="text-center text-slate-400">
          <ZapOff className="w-12 h-12 mx-auto mb-2 opacity-50" />
          <p>Gesture trading disabled</p>
        </div>
      </div>
    )
  }

  return (
    <div className={`relative ${className}`}>
      {/* Gesture feedback overlay */}
      {showFeedback && (
        <motion.div
          initial={{ opacity: 0, scale: 0.8, y: -50 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.8, y: -50 }}
          className="absolute top-4 left-4 right-4 z-50 text-center"
        >
          <div className="glass-card rounded-xl p-3 border border-blue-400/30">
            <p className="text-white font-medium">{showFeedback}</p>
          </div>
        </motion.div>
      )}

      {/* Settings panel */}
      {showSettings && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          className="absolute top-0 right-0 bottom-0 w-80 z-40 glass-card rounded-l-2xl p-4"
        >
          <div className="flex items-center justify-between mb-6">
            <h3 className="font-semibold text-white">Gesture Settings</h3>
            <button
              onClick={() => setShowSettings(false)}
              className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400"
            >
              <Eye className="w-4 h-4" />
            </button>
          </div>

          <div className="space-y-4">
            {/* Sensitivity */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Sensitivity: {gestureConfig.sensitivity}x
              </label>
              <input
                type="range"
                min="0.5"
                max="2.0"
                step="0.1"
                value={gestureConfig.sensitivity}
                onChange={(e) => setGestureConfig(prev => ({ 
                  ...prev, 
                  sensitivity: parseFloat(e.target.value) 
                }))}
                className="w-full"
              />
            </div>

            {/* Haptic intensity */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Haptic Feedback</label>
              <div className="grid grid-cols-3 gap-2">
                {(['light', 'medium', 'heavy'] as const).map((intensity) => (
                  <button
                    key={intensity}
                    onClick={() => {
                      setGestureConfig(prev => ({ ...prev, hapticIntensity: intensity }))
                      triggerHaptic(intensity)
                    }}
                    className={`p-2 rounded-lg text-sm capitalize transition-colors ${
                      gestureConfig.hapticIntensity === intensity
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50'
                    }`}
                  >
                    {intensity}
                  </button>
                ))}
              </div>
            </div>

            {/* Toggles */}
            <div className="space-y-3">
              {[
                { key: 'soundEnabled', label: 'Sound Effects', icon: gestureConfig.soundEnabled ? Volume2 : VolumeX },
                { key: 'visualFeedback', label: 'Visual Feedback', icon: gestureConfig.visualFeedback ? Eye : EyeOff },
                { key: 'confirmationRequired', label: 'Confirmation Required', icon: AlertTriangle },
                { key: 'doubleTapEnabled', label: 'Double Tap', icon: Target },
                { key: 'longPressEnabled', label: 'Long Press', icon: Clock }
              ].map(({ key, label, icon: Icon }) => (
                <div key={key} className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Icon className="w-4 h-4 text-slate-400" />
                    <span className="text-sm text-slate-300">{label}</span>
                  </div>
                  <button
                    onClick={() => setGestureConfig(prev => ({ 
                      ...prev, 
                      [key]: !prev[key as keyof GestureConfig] 
                    }))}
                    className={`w-10 h-6 rounded-full transition-colors ${
                      gestureConfig[key as keyof GestureConfig] ? 'bg-green-500' : 'bg-slate-600'
                    }`}
                  >
                    <div className={`w-4 h-4 rounded-full bg-white transition-transform ${
                      gestureConfig[key as keyof GestureConfig] ? 'translate-x-5' : 'translate-x-0.5'
                    }`} />
                  </button>
                </div>
              ))}
            </div>

            {/* Calibration */}
            <button
              onClick={calibrateGestures}
              disabled={isCalibrating}
              className="w-full p-3 bg-gradient-to-r from-purple-600 to-pink-600 rounded-xl font-medium text-white hover:from-purple-700 hover:to-pink-700 transition-all disabled:opacity-50"
            >
              {isCalibrating ? (
                <div className="flex items-center justify-center space-x-2">
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                  >
                    <RotateCcw className="w-4 h-4" />
                  </motion.div>
                  <span>Calibrating...</span>
                </div>
              ) : (
                'Calibrate Gestures'
              )}
            </button>
          </div>
        </motion.div>
      )}

      {/* Main gesture area */}
      <motion.div
        ref={gestureAreaRef}
        drag
        dragConstraints={{ left: 0, right: 0, top: 0, bottom: 0 }}
        dragElastic={0.2}
        onDragEnd={handleDragEnd}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        style={{
          x,
          y,
          scale,
          rotate,
          backgroundColor: gestureConfig.visualFeedback ? backgroundColor : '#1f2937'
        }}
        className="relative min-h-96 rounded-2xl border-2 border-dashed border-slate-600/50 flex items-center justify-center cursor-move select-none"
      >
        {/* Center content */}
        <div className="text-center">
          <motion.div
            animate={activeGesture ? { scale: [1, 1.2, 1] } : {}}
            transition={{ duration: 0.5 }}
            className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-r from-blue-600 to-purple-600 flex items-center justify-center"
          >
            {activeGesture === 'swipe-right' && <ArrowRight className="w-8 h-8 text-white" />}
            {activeGesture === 'swipe-left' && <ArrowLeft className="w-8 h-8 text-white" />}
            {activeGesture === 'swipe-up' && <ArrowUp className="w-8 h-8 text-white" />}
            {activeGesture === 'swipe-down' && <ArrowDown className="w-8 h-8 text-white" />}
            {!activeGesture && <Move className="w-8 h-8 text-white" />}
          </motion.div>
          
          <h3 className="text-xl font-bold text-white mb-2">
            {symbol} @ ₹{currentPrice}
          </h3>
          
          <p className="text-slate-400 text-sm mb-4">
            {activeGesture ? (
              <span className="text-blue-400">
                {activeGesture.replace('-', ' ').toUpperCase()} detected
                {gestureConfidence > 0 && ` (${Math.round(gestureConfidence * 100)}%)`}
              </span>
            ) : (
              'Swipe to trade • Double tap for portfolio • Long press for settings'
            )}
          </p>

          {/* Gesture indicators */}
          <div className="grid grid-cols-2 gap-2 text-xs text-slate-500">
            <div className="flex items-center space-x-1">
              <ArrowRight className="w-3 h-3 text-green-400" />
              <span>Swipe right: BUY</span>
            </div>
            <div className="flex items-center space-x-1">
              <ArrowLeft className="w-3 h-3 text-red-400" />
              <span>Swipe left: SELL</span>
            </div>
            <div className="flex items-center space-x-1">
              <ArrowUp className="w-3 h-3 text-blue-400" />
              <span>Swipe up: Portfolio</span>
            </div>
            <div className="flex items-center space-x-1">
              <ArrowDown className="w-3 h-3 text-purple-400" />
              <span>Swipe down: Cancel</span>
            </div>
          </div>
        </div>

        {/* Touch points visualization */}
        {gestureConfig.visualFeedback && touchPoints.map((point, index) => (
          <motion.div
            key={point.id}
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 0.7 }}
            exit={{ scale: 0, opacity: 0 }}
            className="absolute w-8 h-8 rounded-full bg-blue-400/50 border-2 border-blue-400 pointer-events-none"
            style={{
              left: point.x - 16,
              top: point.y - 16,
            }}
          >
            <div className="w-2 h-2 rounded-full bg-blue-400 absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2" />
          </motion.div>
        ))}
      </motion.div>

      {/* Gesture history */}
      {gestureHistory.length > 0 && (
        <div className="mt-4 p-4 glass-card rounded-xl">
          <h4 className="font-medium text-white mb-2 flex items-center space-x-2">
            <Activity className="w-4 h-4" />
            <span>Recent Gestures</span>
          </h4>
          <div className="space-y-1">
            {gestureHistory.slice(0, 3).map((action, index) => (
              <div key={index} className="flex items-center justify-between text-sm">
                <span className={`font-medium ${
                  action.type === 'BUY' ? 'text-green-400' :
                  action.type === 'SELL' ? 'text-red-400' :
                  'text-blue-400'
                }`}>
                  {action.type}
                </span>
                <span className="text-slate-400">{action.gesture}</span>
                <span className="text-slate-500">
                  {Math.round(action.confidence * 100)}%
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Settings trigger */}
      <button
        onClick={() => setShowSettings(true)}
        className="absolute top-4 right-4 p-2 glass-card rounded-lg text-slate-400 hover:text-white transition-colors"
      >
        <Settings className="w-4 h-4" />
      </button>
    </div>
  )
}

export default GestureTrading