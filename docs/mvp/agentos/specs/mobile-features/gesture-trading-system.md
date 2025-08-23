# Mobile Features Spec: Advanced Gesture Trading System

## 🎯 Overview

Implement revolutionary gesture-based trading controls for the TradeMaster PWA, making it the first gesture-controlled trading platform in India. This spec builds on Epic 4 (Mobile PWA Experience) and leverages the existing responsive design system.

## 🏗️ Current Implementation Analysis

### Existing Mobile Foundation (Implemented)
- ✅ **PWA Infrastructure**: Service worker, manifest, installable
- ✅ **Responsive Design**: Mobile-first TailwindCSS with breakpoints
- ✅ **Touch Optimization**: Touch-action optimized components
- ✅ **Glassmorphism Theme**: Mobile-optimized glass cards and neon effects
- ✅ **React Components**: 40+ mobile-ready components

### Mobile Feature Gaps (From Epic 4)
- ❌ **Gesture Recognition Engine**: Touch/swipe gesture processing
- ❌ **Haptic Feedback System**: Tactile response for actions
- ❌ **Gesture-Based Trading**: Swipe-to-trade, pinch controls
- ❌ **Mobile-Specific UI**: Gesture hints, floating action buttons
- ❌ **Voice Commands**: Audio trading controls

## 📋 Feature Requirements

### Epic 4 Implementation: Revolutionary Gesture Controls

#### 4.1 Gesture Recognition Engine
**Service**: `GestureRecognitionService`
**Package**: `src/services/gestures`
**Integration**: Custom React hooks with haptic feedback

```typescript
interface GestureRecognitionService {
  // Gesture detection and processing
  recognizeGesture(touchEvent: TouchEvent): GestureResult
  registerGestureHandler(type: GestureType, handler: GestureHandler): void
  calibrateGestureSettings(sensitivity: GestureSensitivity): void
  
  // Haptic feedback integration
  triggerHaptic(pattern: HapticPattern): Promise<void>
  isHapticSupported(): boolean
}

interface GestureResult {
  type: GestureType
  confidence: number
  velocity: number
  distance: number
  direction: GestureDirection
  targetElement: HTMLElement
  touchPoints: TouchPoint[]
  timestamp: number
}

enum GestureType {
  SWIPE_RIGHT_BUY = 'swipe_right_buy',
  SWIPE_LEFT_SELL = 'swipe_left_sell', 
  LONG_PRESS_MENU = 'long_press_menu',
  PINCH_ZOOM = 'pinch_zoom',
  DOUBLE_TAP_SWITCH = 'double_tap_switch',
  THREE_FINGER_REFRESH = 'three_finger_refresh',
  DRAG_UP_INCREASE = 'drag_up_increase',
  DRAG_DOWN_DECREASE = 'drag_down_decrease'
}

enum HapticPattern {
  LIGHT = 'light',          // Gesture recognition
  MEDIUM = 'medium',        // Action preview  
  HEAVY = 'heavy',          // Action confirmation
  SUCCESS = 'success',      // Trade execution
  WARNING = 'warning',      // Risk warning
  ERROR = 'error'           // Action failed
}
```

#### 4.2 Swipe-to-Trade Implementation
**Component**: `SwipeToTradeCard`
**Extends**: Existing market/portfolio components
**Design**: Interactive swipe overlay with progress indicators

```typescript
interface SwipeToTradeCardProps {
  symbol: string
  currentPrice: number
  position?: Position
  swipeThreshold: number
  hapticEnabled: boolean
  onSwipeProgress: (progress: number, direction: 'buy' | 'sell') => void
  onTradeExecute: (order: SwipeTradeOrder) => void
  onSwipeCancel: () => void
}

interface SwipeTradeOrder {
  symbol: string
  side: 'buy' | 'sell'
  quantity: number
  orderType: 'market' | 'limit'
  price?: number
  swipeVelocity: number
}

const SwipeToTradeCard: React.FC<SwipeToTradeCardProps> = ({
  symbol,
  currentPrice,
  position,
  swipeThreshold = 120,
  hapticEnabled = true,
  onSwipeProgress,
  onTradeExecute,
  onSwipeCancel
}) => {
  const [swipeState, setSwipeState] = useState<SwipeState>('idle')
  const [swipeProgress, setSwipeProgress] = useState(0)
  const cardRef = useRef<HTMLDivElement>(null)
  
  // Gesture recognition hooks
  const {
    gestures,
    isGestureActive,
    gestureProgress
  } = useGestureRecognition({
    element: cardRef.current,
    thresholds: {
      swipe: swipeThreshold,
      velocity: 0.5,
      timeout: 2000
    },
    hapticEnabled
  })
  
  const handleSwipeProgress = (progress: number, direction: SwipeDirection) => {
    setSwipeProgress(progress)
    
    // Haptic feedback at progress milestones
    if (progress > 0.3 && progress < 0.35) {
      triggerHaptic(HapticPattern.LIGHT)
    } else if (progress > 0.7 && progress < 0.75) {
      triggerHaptic(HapticPattern.MEDIUM)
    }
    
    onSwipeProgress(progress, direction === 'right' ? 'buy' : 'sell')
  }
  
  const handleSwipeComplete = (gesture: SwipeGesture) => {
    const order: SwipeTradeOrder = {
      symbol,
      side: gesture.direction === 'right' ? 'buy' : 'sell',
      quantity: calculateSwipeQuantity(gesture.velocity, position),
      orderType: gesture.velocity > 1.5 ? 'market' : 'limit',
      price: gesture.orderType === 'limit' ? currentPrice : undefined,
      swipeVelocity: gesture.velocity
    }
    
    // Success haptic feedback
    triggerHaptic(HapticPattern.SUCCESS)
    
    onTradeExecute(order)
  }
  
  return (
    <div 
      ref={cardRef}
      className="relative glass-card rounded-2xl overflow-hidden touch-none"
    >
      {/* Swipe Progress Indicators */}
      <SwipeProgressIndicators 
        leftProgress={swipeProgress}
        rightProgress={swipeProgress}
        activeDirection={gestureDirection}
      />
      
      {/* Main Card Content */}
      <div className="relative z-10 p-4">
        <StockInfo 
          symbol={symbol}
          price={currentPrice}
          position={position}
        />
      </div>
      
      {/* Swipe Action Overlays */}
      <SwipeActionOverlay
        side="left" 
        action="sell"
        progress={swipeProgress}
        active={gestureDirection === 'left'}
      />
      
      <SwipeActionOverlay
        side="right"
        action="buy" 
        progress={swipeProgress}
        active={gestureDirection === 'right'}
      />
      
      {/* Gesture Hints */}
      {!isGestureActive && (
        <GestureHints 
          showBuyHint={!position || position.quantity > 0}
          showSellHint={position && position.quantity > 0}
        />
      )}
    </div>
  )
}
```

#### 4.3 Advanced Gesture Controls
**Hook**: `useAdvancedGestures`
**Features**: Multi-touch gestures, complex patterns

```typescript
interface AdvancedGestureConfig {
  longPressThreshold: number
  pinchThreshold: number
  multiTouchEnabled: boolean
  voiceCommandsEnabled: boolean
  accessibilityMode: boolean
}

const useAdvancedGestures = (config: AdvancedGestureConfig) => {
  const [activeGestures, setActiveGestures] = useState<Set<GestureType>>(new Set())
  const [gestureHistory, setGestureHistory] = useState<GestureEvent[]>([])
  
  const registerGesture = useCallback((
    element: HTMLElement,
    gestureType: GestureType,
    handler: GestureHandler,
    options?: GestureOptions
  ) => {
    const gestureRecognizer = new GestureRecognizer(element, {
      type: gestureType,
      ...options,
      onGestureStart: (event) => {
        setActiveGestures(prev => new Set(prev).add(gestureType))
        if (config.hapticEnabled) {
          triggerHaptic(HapticPattern.LIGHT)
        }
      },
      onGestureEnd: (event) => {
        setActiveGestures(prev => {
          const updated = new Set(prev)
          updated.delete(gestureType)
          return updated
        })
        handler(event)
      }
    })
    
    return () => gestureRecognizer.destroy()
  }, [config])
  
  // Gesture combinations
  const recognizeGestureCombination = useCallback((
    gestures: GestureType[],
    timeWindow: number = 1000
  ) => {
    const recentGestures = gestureHistory.filter(
      g => Date.now() - g.timestamp < timeWindow
    )
    
    return gestures.every(gesture => 
      recentGestures.some(g => g.type === gesture)
    )
  }, [gestureHistory])
  
  return {
    registerGesture,
    activeGestures,
    gestureHistory,
    recognizeGestureCombination
  }
}
```

#### 4.4 Haptic Feedback Integration
**Service**: `HapticFeedbackService`
**Platform**: iOS/Android haptic APIs via PWA

```typescript
class HapticFeedbackService {
  private isSupported: boolean = false
  private hapticQueue: HapticEvent[] = []
  
  constructor() {
    this.detectHapticSupport()
  }
  
  private detectHapticSupport(): void {
    // iOS Safari haptic support
    if ('vibrate' in navigator || 'hapticEngine' in window) {
      this.isSupported = true
    }
    
    // Android Chrome haptic support
    if ('serviceWorker' in navigator && 'vibrate' in navigator) {
      this.isSupported = true
    }
  }
  
  async triggerHaptic(pattern: HapticPattern): Promise<void> {
    if (!this.isSupported) return
    
    const hapticEvent: HapticEvent = {
      pattern,
      timestamp: Date.now(),
      intensity: this.getPatternIntensity(pattern)
    }
    
    try {
      switch (pattern) {
        case HapticPattern.LIGHT:
          await this.vibrate([10])
          break
        case HapticPattern.MEDIUM:
          await this.vibrate([50])
          break  
        case HapticPattern.HEAVY:
          await this.vibrate([100])
          break
        case HapticPattern.SUCCESS:
          await this.vibrate([30, 30, 60])
          break
        case HapticPattern.WARNING:
          await this.vibrate([100, 50, 100])
          break
        case HapticPattern.ERROR:
          await this.vibrate([200, 100, 200, 100, 200])
          break
      }
      
      this.logHapticEvent(hapticEvent)
    } catch (error) {
      console.warn('Haptic feedback failed:', error)
    }
  }
  
  private async vibrate(pattern: number[]): Promise<void> {
    if ('vibrate' in navigator) {
      navigator.vibrate(pattern)
    }
  }
  
  // iOS-specific haptic feedback
  private async triggerIOSHaptic(type: 'selection' | 'impact' | 'notification'): Promise<void> {
    if ('hapticEngine' in window) {
      try {
        // @ts-ignore - iOS PWA haptic API
        await window.hapticEngine.trigger(type)
      } catch (error) {
        console.warn('iOS haptic failed:', error)
      }
    }
  }
}
```

#### 4.5 Voice Command Integration  
**Service**: `VoiceCommandService`
**Features**: Speech recognition for trading actions

```typescript
interface VoiceCommandService {
  isSupported: boolean
  isListening: boolean
  startListening(): Promise<void>
  stopListening(): void
  registerCommand(phrase: string, handler: VoiceCommandHandler): void
  setLanguage(language: 'en-US' | 'hi-IN'): void
}

interface VoiceCommand {
  phrase: string
  confidence: number
  action: TradingAction
  parameters: Record<string, any>
}

class VoiceCommandServiceImpl implements VoiceCommandService {
  private recognition: SpeechRecognition | null = null
  private commands: Map<string, VoiceCommandHandler> = new Map()
  
  constructor() {
    this.initializeSpeechRecognition()
  }
  
  private initializeSpeechRecognition(): void {
    if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
      this.recognition = new SpeechRecognition()
      
      this.recognition.continuous = true
      this.recognition.interimResults = false
      this.recognition.lang = 'en-US'
      
      this.recognition.onresult = (event) => {
        const transcript = event.results[event.resultIndex][0].transcript.toLowerCase().trim()
        const confidence = event.results[event.resultIndex][0].confidence
        
        this.processVoiceCommand(transcript, confidence)
      }
    }
  }
  
  private processVoiceCommand(transcript: string, confidence: number): void {
    // Trading command patterns
    const commandPatterns = {
      buy: /(?:buy|purchase)\s+(\d+)?\s*(shares?\s+of\s+)?([a-z]+)/i,
      sell: /(?:sell|dispose)\s+(\d+)?\s*(shares?\s+of\s+)?([a-z]+)/i,
      check: /(?:check|show|what's)\s+(?:my\s+)?(?:portfolio|position|balance)/i,
      cancel: /(?:cancel|stop|abort)\s+(?:all\s+)?(?:orders?|trades?)/i,
      help: /(?:help|what\s+can\s+you\s+do)/i
    }
    
    for (const [action, pattern] of Object.entries(commandPatterns)) {
      const match = transcript.match(pattern)
      if (match && confidence > 0.7) {
        this.executeVoiceCommand(action as TradingAction, match, confidence)
        return
      }
    }
    
    // No command matched
    this.showVoiceCommandError('Command not recognized')
  }
  
  private executeVoiceCommand(action: TradingAction, match: RegExpMatchArray, confidence: number): void {
    const command: VoiceCommand = {
      phrase: match[0],
      confidence,
      action,
      parameters: this.extractParameters(action, match)
    }
    
    const handler = this.commands.get(action)
    if (handler) {
      handler(command)
    }
  }
}
```

## 🎨 Mobile-Specific UI Components

### Gesture Hint System
```typescript
const GestureHints: React.FC<GestureHintsProps> = ({ 
  showBuyHint, 
  showSellHint, 
  hintDuration = 3000 
}) => {
  const [showHints, setShowHints] = useState(true)
  
  useEffect(() => {
    const timer = setTimeout(() => {
      setShowHints(false)
    }, hintDuration)
    
    return () => clearTimeout(timer)
  }, [hintDuration])
  
  if (!showHints) return null
  
  return (
    <div className="absolute inset-0 pointer-events-none z-20">
      {showBuyHint && (
        <div className="absolute right-4 top-1/2 transform -translate-y-1/2 
                        flex items-center space-x-2 text-green-400 animate-pulse">
          <ArrowRight className="w-4 h-4" />
          <span className="text-sm font-medium">Swipe to Buy</span>
        </div>
      )}
      
      {showSellHint && (
        <div className="absolute left-4 top-1/2 transform -translate-y-1/2 
                        flex items-center space-x-2 text-red-400 animate-pulse">
          <ArrowLeft className="w-4 h-4" />
          <span className="text-sm font-medium">Swipe to Sell</span>
        </div>
      )}
      
      <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 
                      text-slate-400 text-xs animate-fade-in">
        Long press for more options
      </div>
    </div>
  )
}
```

### Floating Action Button (FAB)
```typescript
const FloatingActionButton: React.FC<FABProps> = ({
  actions,
  primaryAction,
  position = 'bottom-right',
  expandedByDefault = false
}) => {
  const [expanded, setExpanded] = useState(expandedByDefault)
  
  return (
    <div className={`fixed z-50 ${getPositionClasses(position)}`}>
      {/* Secondary Actions */}
      <AnimatePresence>
        {expanded && (
          <div className="absolute bottom-16 right-0 space-y-3">
            {actions.map((action, index) => (
              <motion.button
                key={action.id}
                initial={{ opacity: 0, y: 20, scale: 0.8 }}
                animate={{ 
                  opacity: 1, 
                  y: 0, 
                  scale: 1,
                  transition: { delay: index * 0.1 }
                }}
                exit={{ opacity: 0, y: 20, scale: 0.8 }}
                className="flex items-center space-x-3 glass-card px-4 py-2 
                           rounded-full text-white hover:bg-purple-500/20"
                onClick={() => {
                  action.onPress()
                  setExpanded(false)
                }}
              >
                <action.icon className="w-5 h-5" />
                <span className="text-sm">{action.label}</span>
              </motion.button>
            ))}
          </div>
        )}
      </AnimatePresence>
      
      {/* Primary FAB */}
      <motion.button
        className="w-14 h-14 cyber-button rounded-full flex items-center 
                   justify-center shadow-lg shadow-purple-500/25"
        whileTap={{ scale: 0.95 }}
        onClick={() => setExpanded(!expanded)}
      >
        <motion.div
          animate={{ rotate: expanded ? 45 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <primaryAction.icon className="w-6 h-6" />
        </motion.div>
      </motion.button>
    </div>
  )
}
```

## 📱 PWA Integration Enhancements

### Service Worker Extensions
```typescript
// Enhanced service worker for gesture data caching
self.addEventListener('message', (event) => {
  if (event.data.type === 'CACHE_GESTURE_DATA') {
    caches.open('gesture-cache-v1').then(cache => {
      cache.put('gesture-settings', new Response(
        JSON.stringify(event.data.settings)
      ))
    })
  }
  
  if (event.data.type === 'TRIGGER_HAPTIC') {
    // Background haptic feedback
    self.registration.showNotification('', {
      tag: 'haptic',
      silent: true,
      vibrate: event.data.pattern
    })
  }
})
```

### Manifest Enhancements
```json
{
  "name": "TradeMaster Gesture Trading",
  "short_name": "TradeMaster",
  "description": "Revolutionary gesture-based trading platform",
  "categories": ["finance", "business", "productivity"],
  "orientation": "portrait-primary",
  "display_override": ["window-controls-overlay", "standalone"],
  "shortcuts": [
    {
      "name": "Quick Buy",
      "url": "/quick-buy",
      "icons": [{"src": "/icons/buy-96.png", "sizes": "96x96"}]
    },
    {
      "name": "Portfolio",  
      "url": "/portfolio",
      "icons": [{"src": "/icons/portfolio-96.png", "sizes": "96x96"}]
    }
  ],
  "scope": "/",
  "start_url": "/dashboard",
  "theme_color": "#8B5CF6",
  "background_color": "#0F0D23",
  "related_applications": [],
  "prefer_related_applications": false
}
```

## 🧪 Testing Strategy

### Gesture Testing Framework
```typescript
class GestureTestFramework {
  private testRunner: TestRunner
  private hapticMock: HapticMock
  
  async testSwipeGesture(
    element: HTMLElement,
    direction: 'left' | 'right',
    velocity: number
  ): Promise<GestureTestResult> {
    
    const startTouch = this.createTouchEvent('touchstart', 100, 200)
    const moveTouch = this.createTouchEvent(
      'touchmove', 
      direction === 'right' ? 220 : -20, 
      200
    )
    const endTouch = this.createTouchEvent('touchend', 240, 200)
    
    element.dispatchEvent(startTouch)
    await this.delay(50)
    element.dispatchEvent(moveTouch)
    await this.delay(100)  
    element.dispatchEvent(endTouch)
    
    return {
      gestureRecognized: true,
      direction,
      velocity,
      hapticTriggered: this.hapticMock.wasTriggered()
    }
  }
  
  async testHapticFeedback(pattern: HapticPattern): Promise<boolean> {
    const hapticService = new HapticFeedbackService()
    await hapticService.triggerHaptic(pattern)
    return this.hapticMock.verifyPattern(pattern)
  }
}
```

### Performance Testing
- **Gesture Recognition Latency**: < 16ms (60fps)
- **Haptic Response Time**: < 10ms
- **Touch Event Processing**: < 5ms
- **Battery Usage**: < 5% per hour of active trading

## 🚀 Implementation Phases

### Phase 1: Core Gesture Engine (Week 1-2)
- Build gesture recognition service
- Implement basic swipe-to-trade
- Add haptic feedback system

### Phase 2: Advanced Gestures (Week 3-4)  
- Multi-touch gesture support
- Voice command integration
- Floating action buttons

### Phase 3: Polish & Optimization (Week 5-6)
- Performance optimization
- Accessibility improvements
- Comprehensive testing

**Ready to revolutionize mobile trading! 📱⚡**