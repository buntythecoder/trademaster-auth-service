# Story FE-005: Mobile-Responsive Trading Interface

## Epic
Epic 2: Market Data & Trading Foundation

## Story Overview
**As a** TradeMaster user on mobile device  
**I want** a fully responsive trading interface optimized for mobile usage  
**So that** I can trade effectively on-the-go with the same functionality as desktop

## Business Value
- **Market Coverage**: 85% of Indian users primarily use mobile devices for trading
- **User Accessibility**: Enable trading anywhere, anytime for maximum market participation
- **Competitive Advantage**: Superior mobile experience vs existing trading platforms
- **Revenue Impact**: Mobile-first approach increases user engagement by 65%

## Technical Requirements

### Responsive Design System
```tsx
// Responsive Breakpoints
const breakpoints = {
  xs: '320px',
  sm: '640px', 
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px'
} as const

// Mobile-First Media Queries
const mediaQueries = {
  mobile: `(max-width: ${breakpoints.sm})`,
  tablet: `(min-width: ${breakpoints.sm}) and (max-width: ${breakpoints.lg})`,
  desktop: `(min-width: ${breakpoints.lg})`,
  touch: '(pointer: coarse)',
  hover: '(hover: hover)'
}

// Responsive Hook
export const useResponsive = () => {
  const [breakpoint, setBreakpoint] = useState<keyof typeof breakpoints>('md')
  const [isMobile, setIsMobile] = useState(false)
  const [isTouch, setIsTouch] = useState(false)
  
  useEffect(() => {
    const checkBreakpoint = () => {
      const width = window.innerWidth
      
      if (width < parseInt(breakpoints.sm)) {
        setBreakpoint('xs')
        setIsMobile(true)
      } else if (width < parseInt(breakpoints.md)) {
        setBreakpoint('sm')
        setIsMobile(true)
      } else if (width < parseInt(breakpoints.lg)) {
        setBreakpoint('md')
        setIsMobile(false)
      } else if (width < parseInt(breakpoints.xl)) {
        setBreakpoint('lg')
        setIsMobile(false)
      } else {
        setBreakpoint('xl')
        setIsMobile(false)
      }
      
      // Detect touch capability
      setIsTouch('ontouchstart' in window || navigator.maxTouchPoints > 0)
    }
    
    checkBreakpoint()
    window.addEventListener('resize', checkBreakpoint)
    
    return () => window.removeEventListener('resize', checkBreakpoint)
  }, [])
  
  return { breakpoint, isMobile, isTouch }
}
```

### Mobile Trading Interface Components
```tsx
// Mobile Trading Dashboard
export const MobileTradingDashboard: React.FC = () => {
  const { isMobile } = useResponsive()
  const [activeTab, setActiveTab] = useState<'watchlist' | 'positions' | 'orders'>('watchlist')
  
  return (
    <div className="mobile-trading-dashboard">
      {/* Compact Header */}
      <MobileHeader />
      
      {/* Market Summary Banner */}
      <MarketSummaryBanner />
      
      {/* Main Content Tabs */}
      <div className="trading-tabs">
        <TabNavigation 
          tabs={[
            { key: 'watchlist', label: 'Watchlist', icon: <Eye /> },
            { key: 'positions', label: 'Positions', icon: <TrendingUp /> },
            { key: 'orders', label: 'Orders', icon: <List /> }
          ]}
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />
        
        <div className="tab-content">
          {activeTab === 'watchlist' && <MobileWatchlist />}
          {activeTab === 'positions' && <MobilePositions />}
          {activeTab === 'orders' && <MobileOrders />}
        </div>
      </div>
      
      {/* Quick Action Button */}
      <FloatingActionButton
        onClick={() => setActiveTab('orders')}
        icon={<Plus />}
        label="Quick Trade"
      />
    </div>
  )
}

// Mobile Watchlist Component
export const MobileWatchlist: React.FC = () => {
  const { data: watchlistData } = useMarketData(['NIFTY', 'BANKNIFTY', 'RELIANCE', 'TCS'])
  const [sortBy, setSortBy] = useState<'name' | 'price' | 'change'>('name')
  
  return (
    <div className="mobile-watchlist">
      {/* Sort Controls */}
      <div className="sort-controls">
        <Select value={sortBy} onValueChange={setSortBy}>
          <SelectTrigger className="w-32">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="name">Name</SelectItem>
            <SelectItem value="price">Price</SelectItem>
            <SelectItem value="change">Change</SelectItem>
          </SelectContent>
        </Select>
      </div>
      
      {/* Watchlist Items */}
      <div className="watchlist-items">
        {watchlistData.map((item, index) => (
          <MobileWatchlistItem
            key={item.symbol}
            data={item}
            onTrade={() => openTradeModal(item.symbol)}
          />
        ))}
      </div>
    </div>
  )
}

// Optimized Watchlist Item for Mobile
export const MobileWatchlistItem: React.FC<{
  data: MarketData
  onTrade: () => void
}> = ({ data, onTrade }) => {
  const priceChange = data.change || 0
  const priceChangePercent = data.changePercent || 0
  const isPositive = priceChange >= 0
  
  return (
    <div className="mobile-watchlist-item">
      <div className="item-content">
        {/* Symbol and Name */}
        <div className="symbol-info">
          <span className="symbol">{data.symbol}</span>
          <span className="company-name">{data.companyName}</span>
        </div>
        
        {/* Price Information */}
        <div className="price-info">
          <span className="current-price">₹{data.price.toFixed(2)}</span>
          <div className={`price-change ${isPositive ? 'positive' : 'negative'}`}>
            <span className="change-amount">
              {isPositive ? '+' : ''}₹{priceChange.toFixed(2)}
            </span>
            <span className="change-percent">
              ({isPositive ? '+' : ''}{priceChangePercent.toFixed(2)}%)
            </span>
          </div>
        </div>
        
        {/* Quick Actions */}
        <div className="quick-actions">
          <Button size="sm" variant="outline" onClick={onTrade}>
            Trade
          </Button>
        </div>
      </div>
      
      {/* Mini Chart */}
      <div className="mini-chart">
        <MiniSparklineChart data={data.priceHistory} />
      </div>
    </div>
  )
}
```

### Touch-Optimized Trading Forms
```tsx
// Mobile Trading Form
export const MobileTradeForm: React.FC<{
  symbol: string
  onClose: () => void
}> = ({ symbol, onClose }) => {
  const [orderType, setOrderType] = useState<'buy' | 'sell'>('buy')
  const [quantity, setQuantity] = useState<string>('')
  const [price, setPrice] = useState<string>('')
  const [orderMode, setOrderMode] = useState<'market' | 'limit'>('market')
  
  const { data: marketData } = useMarketData([symbol])
  const currentPrice = marketData.get(symbol)?.price || 0
  
  return (
    <div className="mobile-trade-form">
      {/* Header */}
      <div className="form-header">
        <h3>{symbol}</h3>
        <Button variant="ghost" size="sm" onClick={onClose}>
          <X />
        </Button>
      </div>
      
      {/* Current Price Display */}
      <div className="current-price-banner">
        <span className="label">Current Price</span>
        <span className="price">₹{currentPrice.toFixed(2)}</span>
      </div>
      
      {/* Buy/Sell Toggle */}
      <div className="order-type-toggle">
        <Button
          variant={orderType === 'buy' ? 'default' : 'outline'}
          onClick={() => setOrderType('buy')}
          className="flex-1"
        >
          Buy
        </Button>
        <Button
          variant={orderType === 'sell' ? 'default' : 'outline'}
          onClick={() => setOrderType('sell')}
          className="flex-1"
        >
          Sell
        </Button>
      </div>
      
      {/* Quantity Input with Touch-Friendly Controls */}
      <div className="quantity-section">
        <Label>Quantity</Label>
        <div className="quantity-input-group">
          <Button
            variant="outline"
            size="sm"
            onClick={() => adjustQuantity(-1)}
            disabled={parseInt(quantity) <= 1}
          >
            <Minus />
          </Button>
          
          <Input
            type="number"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0"
            className="text-center text-lg"
            inputMode="numeric"
            pattern="[0-9]*"
          />
          
          <Button
            variant="outline"
            size="sm"
            onClick={() => adjustQuantity(1)}
          >
            <Plus />
          </Button>
        </div>
        
        {/* Quick Quantity Buttons */}
        <div className="quick-quantity-buttons">
          {[1, 5, 10, 25, 50].map(qty => (
            <Button
              key={qty}
              variant="outline"
              size="sm"
              onClick={() => setQuantity(qty.toString())}
            >
              {qty}
            </Button>
          ))}
        </div>
      </div>
      
      {/* Order Mode Toggle */}
      <div className="order-mode-toggle">
        <Button
          variant={orderMode === 'market' ? 'default' : 'outline'}
          onClick={() => setOrderMode('market')}
          className="flex-1"
        >
          Market
        </Button>
        <Button
          variant={orderMode === 'limit' ? 'default' : 'outline'}
          onClick={() => setOrderMode('limit')}
          className="flex-1"
        >
          Limit
        </Button>
      </div>
      
      {/* Price Input (for limit orders) */}
      {orderMode === 'limit' && (
        <div className="price-section">
          <Label>Limit Price</Label>
          <Input
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            placeholder={currentPrice.toFixed(2)}
            className="text-lg"
            inputMode="decimal"
          />
        </div>
      )}
      
      {/* Order Summary */}
      <div className="order-summary">
        <div className="summary-row">
          <span>Quantity:</span>
          <span>{quantity || 0} shares</span>
        </div>
        <div className="summary-row">
          <span>Price:</span>
          <span>₹{orderMode === 'market' ? currentPrice.toFixed(2) : price || '0.00'}</span>
        </div>
        <div className="summary-row total">
          <span>Total Amount:</span>
          <span>₹{calculateTotal().toFixed(2)}</span>
        </div>
      </div>
      
      {/* Submit Button */}
      <Button
        className={`submit-button ${orderType}`}
        onClick={handleSubmit}
        disabled={!quantity || (orderMode === 'limit' && !price)}
        size="lg"
      >
        {orderType === 'buy' ? 'Buy' : 'Sell'} {quantity || 0} shares
      </Button>
    </div>
  )
}
```

### Mobile Performance Optimizations
```tsx
// Virtual Scrolling for Mobile Lists
export const VirtualizedWatchlist: React.FC = () => {
  const { data: watchlistData } = useWatchlist()
  const parentRef = useRef<HTMLDivElement>(null)
  
  const virtualizer = useVirtualizer({
    count: watchlistData.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 80, // Estimated item height
    overscan: 5 // Render 5 items outside viewport
  })
  
  return (
    <div
      ref={parentRef}
      className="virtualized-watchlist"
      style={{ height: 'calc(100vh - 200px)', overflow: 'auto' }}
    >
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative'
        }}
      >
        {virtualizer.getVirtualItems().map(virtualItem => {
          const item = watchlistData[virtualItem.index]
          
          return (
            <div
              key={virtualItem.key}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: `${virtualItem.size}px`,
                transform: `translateY(${virtualItem.start}px)`
              }}
            >
              <MobileWatchlistItem data={item} />
            </div>
          )
        })}
      </div>
    </div>
  )
}

// Touch Gesture Support
export const useTouchGestures = (elementRef: RefObject<HTMLElement>) => {
  const [touchStart, setTouchStart] = useState<{ x: number; y: number } | null>(null)
  const [touchEnd, setTouchEnd] = useState<{ x: number; y: number } | null>(null)
  
  const minSwipeDistance = 50
  
  const onTouchStart = useCallback((e: TouchEvent) => {
    setTouchEnd(null)
    setTouchStart({
      x: e.targetTouches[0].clientX,
      y: e.targetTouches[0].clientY
    })
  }, [])
  
  const onTouchMove = useCallback((e: TouchEvent) => {
    setTouchEnd({
      x: e.targetTouches[0].clientX,
      y: e.targetTouches[0].clientY
    })
  }, [])
  
  const onTouchEnd = useCallback(() => {
    if (!touchStart || !touchEnd) return
    
    const distanceX = touchStart.x - touchEnd.x
    const distanceY = touchStart.y - touchEnd.y
    const isLeftSwipe = distanceX > minSwipeDistance
    const isRightSwipe = distanceX < -minSwipeDistance
    const isUpSwipe = distanceY > minSwipeDistance
    const isDownSwipe = distanceY < -minSwipeDistance
    
    return {
      isLeftSwipe,
      isRightSwipe,
      isUpSwipe,
      isDownSwipe
    }
  }, [touchStart, touchEnd])
  
  useEffect(() => {
    const element = elementRef.current
    if (!element) return
    
    element.addEventListener('touchstart', onTouchStart)
    element.addEventListener('touchmove', onTouchMove)
    element.addEventListener('touchend', onTouchEnd)
    
    return () => {
      element.removeEventListener('touchstart', onTouchStart)
      element.removeEventListener('touchmove', onTouchMove)
      element.removeEventListener('touchend', onTouchEnd)
    }
  }, [elementRef, onTouchStart, onTouchMove, onTouchEnd])
}
```

### Mobile-Specific UI Patterns
```scss
// Mobile-First CSS
.mobile-trading-dashboard {
  // Mobile viewport optimization
  min-height: 100vh;
  min-height: 100dvh; // Dynamic viewport height
  
  // Safe area support for iOS
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
  padding-left: env(safe-area-inset-left);
  padding-right: env(safe-area-inset-right);
  
  // Prevent zoom on form inputs
  input, select, textarea {
    font-size: 16px; // Prevents iOS zoom
  }
  
  // Touch-friendly tap targets
  .tap-target {
    min-height: 44px;
    min-width: 44px;
    
    // Increase tap area with pseudo-element
    &::before {
      content: '';
      position: absolute;
      top: -10px;
      left: -10px;
      right: -10px;
      bottom: -10px;
    }
  }
  
  // Optimized animations for mobile
  .animate-slide-in {
    animation: slideIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }
  
  // Gesture feedback
  .swipeable {
    touch-action: pan-y;
    
    &.swiping {
      transform: translateX(var(--swipe-offset));
      transition: none;
    }
  }
}

// Responsive grid system
.mobile-grid {
  display: grid;
  gap: 1rem;
  
  // Single column on mobile
  grid-template-columns: 1fr;
  
  @media (min-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (min-width: 1024px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

// Performance optimizations
.gpu-accelerated {
  transform: translateZ(0);
  will-change: transform;
}

.smooth-scroll {
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}
```

## Acceptance Criteria

### Responsive Design
- [ ] **Breakpoint Coverage**: Optimized layouts for xs (320px), sm (640px), md (768px), lg (1024px)
- [ ] **Touch Targets**: Minimum 44px tap targets for all interactive elements
- [ ] **Safe Area Support**: Proper handling of iPhone notch and Android navigation gestures
- [ ] **Orientation Support**: Landscape and portrait mode optimization

### Mobile Performance
- [ ] **Load Time**: <3 seconds on 3G networks
- [ ] **Frame Rate**: 60fps scrolling and animations on mid-range devices
- [ ] **Memory Usage**: <100MB for trading interface
- [ ] **Virtual Scrolling**: Efficient rendering for lists >100 items

### Touch Interactions
- [ ] **Gesture Support**: Swipe gestures for navigation and actions
- [ ] **Touch Feedback**: Visual feedback for all touch interactions
- [ ] **Scroll Behavior**: Smooth momentum scrolling with proper boundaries
- [ ] **Form Optimization**: Appropriate input types and keyboards

### User Experience
- [ ] **Thumb Navigation**: All primary actions accessible with one thumb
- [ ] **Quick Actions**: Floating action buttons for common tasks
- [ ] **Modal Optimization**: Full-screen modals for complex forms
- [ ] **Progressive Disclosure**: Layered information architecture

## Testing Strategy

### Device Testing
- Physical testing on iOS devices (iPhone 12, 13, 14, SE)
- Physical testing on Android devices (Samsung Galaxy, OnePlus, Pixel)
- Tablet testing (iPad, Android tablets)
- Browser compatibility (Safari, Chrome Mobile, Samsung Internet)

### Performance Testing
- Network throttling (3G, slow 3G, offline scenarios)
- CPU throttling simulation for budget devices
- Memory pressure testing
- Battery usage optimization validation

### Accessibility Testing
- Screen reader compatibility (VoiceOver, TalkBack)
- High contrast mode support
- Font size scaling validation
- Color contrast ratio verification

### User Experience Testing
- One-handed usage scenarios
- Portrait/landscape orientation changes
- Keyboard integration with form flows
- Touch gesture accuracy and responsiveness

## Definition of Done
- [ ] Mobile-responsive trading interface implemented across all screen sizes
- [ ] Touch-optimized forms with proper input types and validation
- [ ] Virtual scrolling for performance on large data sets
- [ ] Gesture support for common trading actions
- [ ] Safe area and notch handling for modern devices
- [ ] Performance testing passed on 3G networks (<3s load time)
- [ ] Cross-device testing completed on iOS and Android
- [ ] Accessibility standards met (WCAG 2.1 AA)
- [ ] PWA features integrated (offline support, add to home screen)
- [ ] Battery usage optimized for extended trading sessions

## Story Points: 16

## Dependencies
- Backend API optimization for mobile data payloads
- CDN configuration for mobile asset delivery
- WebSocket optimizations for mobile networks
- Push notification service for mobile alerts

## Notes
- Consider progressive web app (PWA) features for app-like experience
- Integration with device features (camera for KYC, biometric authentication)
- Offline queue management for order placement when connection is lost
- Dark mode optimization for mobile trading in various lighting conditions