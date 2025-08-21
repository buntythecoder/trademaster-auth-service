# UI Story 2.1: Market Data Dashboard

**Epic**: 2 - Market Data & Trading Foundation  
**Story**: Real-time Market Data Dashboard Interface  
**Priority**: High - Revenue Critical  
**Complexity**: High  
**Duration**: 2 weeks  

## 📋 Story Overview

**As a** retail trader using TradeMaster  
**I want** a comprehensive real-time market data dashboard with intuitive mobile interface  
**So that** I can make informed trading decisions with professional-grade market insights

## 🎯 Business Value

- **Revenue Impact**: Essential for trading fee generation
- **User Engagement**: Keeps users active on platform with real-time data
- **Competitive Advantage**: Sub-100ms latency market data
- **Mobile-First**: 80% of users access via mobile devices

## 🖼️ UI Requirements

### Design System Consistency
- **Theme**: Continue existing dark fintech theme with glassmorphism
- **Colors**: Purple primary (#8B5CF6), green gains (#22C55E), red losses (#EF4444)
- **Components**: Extend existing `glass-card`, `cyber-button`, `cyber-input` patterns
- **Typography**: Inter font, 16px+ base for mobile readability
- **Animations**: Smooth transitions with particle effects for real-time updates

### Component Architecture
```typescript
// Core Components Required
- MarketDataTicker: Horizontal scrolling price ticker
- PriceChart: Interactive candlestick/line charts
- OrderBookWidget: Real-time bid/ask visualization
- WatchlistManager: Customizable symbol tracking
- MarketStatusIndicator: Exchange status display
- QuickTradeActions: Buy/sell quick access buttons
```

## 🏗️ Component Specifications

### 1. Market Data Ticker Component

#### Visual Design
```typescript
interface MarketTickerProps {
  symbols: MarketSymbol[];
  speed: 'slow' | 'normal' | 'fast';
  showChange: boolean;
  showVolume: boolean;
  pauseOnHover: boolean;
}

interface MarketSymbol {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  volume?: number;
  exchange: 'NSE' | 'BSE';
}
```

#### Layout Specifications
**Mobile (375px+)**:
```
┌─────────────────────────┐
│ RELIANCE ₹2,345.60 +2.4%│ ← Scrolling horizontally
│ INFY ₹1,234.50 -1.2%   │   Auto-scrolling with
│ TCS ₹3,456.78 +0.8%    │   pause on touch
└─────────────────────────┘ 
Height: 60px, Glass card background
```

**Desktop (768px+)**:
```
┌────────────────────────────────────────────────────────────┐
│ RELIANCE ₹2,345.60 +2.4% | INFY ₹1,234.50 -1.2% | TCS... │
└────────────────────────────────────────────────────────────┘
Height: 48px, Full width ticker
```

#### Technical Requirements
- **Update Frequency**: Real-time WebSocket updates (<100ms latency)
- **Scroll Performance**: 60fps smooth scrolling
- **Touch Interaction**: Tap to add to watchlist, long press for details
- **Accessibility**: ARIA labels for screen readers

### 2. Price Chart Component

#### Visual Design
```typescript
interface PriceChartProps {
  symbol: string;
  timeframe: '1m' | '5m' | '15m' | '1h' | '4h' | '1d' | '1w';
  chartType: 'candlestick' | 'line' | 'area';
  indicators?: TechnicalIndicator[];
  height: number;
  showVolume: boolean;
  realtimeUpdates: boolean;
}

interface TechnicalIndicator {
  type: 'SMA' | 'EMA' | 'RSI' | 'MACD' | 'BB';
  period: number;
  color: string;
}
```

#### Layout Specifications
**Mobile Chart (375px+)**:
```
┌─────────────────────────┐
│ RELIANCE • 1D • Line    │ 40px header
├─────────────────────────┤
│                         │
│     Price Chart         │ 280px chart
│   (Touch/pinch zoom)    │ area
│                         │
├─────────────────────────┤
│ Vol: ██████████████     │ 40px volume
├─────────────────────────┤
│ [1m][5m][1h][1d][1w]   │ 44px timeframe
└─────────────────────────┘
```

#### Interaction Patterns
- **Mobile**: Pinch-to-zoom, drag to pan, tap for crosshair
- **Desktop**: Mouse wheel zoom, click and drag, hover crosshair
- **Real-time**: Smooth animations for new candles/data points
- **Loading**: Skeleton charts while data loads

### 3. Order Book Widget

#### Visual Design
```typescript
interface OrderBookProps {
  symbol: string;
  depth: number; // Number of price levels to show
  compact: boolean; // Mobile-friendly compact view
  showSpread: boolean;
  updateFrequency: number; // milliseconds
}

interface OrderBookLevel {
  price: number;
  quantity: number;
  orderCount: number;
}
```

#### Layout Specifications
**Mobile Order Book**:
```
┌─────────────────────────┐
│ RELIANCE Order Book     │ Header
├─────────────────────────┤
│ ASKS (Sell Orders)      │ Red section
│ 2,346.50  500  ████     │ Price, Qty, Bar
│ 2,346.00  750  ██████   │ Visual quantity
│ 2,345.75  300  ██       │
├─────────────────────────┤
│ Spread: ₹0.25 (0.01%)   │ Highlight
├─────────────────────────┤
│ BIDS (Buy Orders)       │ Green section
│ 2,345.50  800  ██████   │
│ 2,345.25  600  ████     │
│ 2,345.00  1000 ████████ │
└─────────────────────────┘
```

### 4. Watchlist Manager

#### Visual Design
```typescript
interface WatchlistProps {
  symbols: WatchlistSymbol[];
  allowDragReorder: boolean;
  showQuickActions: boolean;
  maxSymbols: number;
  onSymbolClick: (symbol: string) => void;
  onRemoveSymbol: (symbol: string) => void;
}

interface WatchlistSymbol {
  symbol: string;
  companyName: string;
  price: number;
  change: number;
  changePercent: number;
  alert?: PriceAlert;
}
```

#### Layout Specifications
**Mobile Watchlist**:
```
┌─────────────────────────┐
│ My Watchlist (5/10)     │ Header with count
├─────────────────────────┤
│ RELIANCE               │
│ Reliance Industries    │ Company name
│ ₹2,345.60  +2.4% ↑    │ Price & change
├─────────────────────────┤
│ INFY                   │
│ Infosys Limited        │ Swipe left to
│ ₹1,234.50  -1.2% ↓    │ remove/trade
├─────────────────────────┤
│ + Add Symbol           │ Add button
└─────────────────────────┘
```

### 5. Market Status Indicator

#### Visual Design
```typescript
interface MarketStatusProps {
  exchange: 'NSE' | 'BSE';
  status: 'pre_open' | 'open' | 'closed' | 'holiday';
  nextSession?: Date;
  showCountdown: boolean;
}
```

#### Status Indicators
```
Market Open:   🟢 NSE Open • 3:15 PM remaining
Pre-Market:    🟡 Pre-Market • Opens in 15 min
Market Closed: 🔴 NSE Closed • Opens Mon 9:15 AM
Holiday:       🟠 Holiday • Resumes Tuesday
```

## 📱 Mobile-First Design Patterns

### Touch Interaction Standards
- **Minimum Touch Target**: 44px × 44px
- **Comfortable Touch**: 48px × 48px for primary actions
- **Gesture Support**: Swipe, pinch, long press, double tap
- **Haptic Feedback**: Optional vibration for interactions

### Performance Optimizations
- **Virtual Scrolling**: For large watchlists (>50 symbols)
- **Lazy Loading**: Charts load only when visible
- **Progressive Enhancement**: Core data first, then charts
- **Offline Support**: Cache last known prices

### Responsive Breakpoints
```css
/* Mobile First */
.dashboard-container {
  /* 320px+ : Single column layout */
  grid-template-columns: 1fr;
  gap: 1rem;
  padding: 1rem;
}

@media (min-width: 768px) {
  /* Tablet: Two column layout */
  .dashboard-container {
    grid-template-columns: 2fr 1fr;
    gap: 1.5rem;
    padding: 1.5rem;
  }
}

@media (min-width: 1024px) {
  /* Desktop: Three column layout */
  .dashboard-container {
    grid-template-columns: 1fr 2fr 1fr;
    gap: 2rem;
    padding: 2rem;
  }
}
```

## 🎨 Visual Design Specifications

### Color System for Market Data
```css
/* Price Movement Colors */
:root {
  --bull-green: #22C55E;      /* Gains/Bullish */
  --bear-red: #EF4444;        /* Losses/Bearish */
  --neutral-gray: #94A3B8;    /* No change */
  
  /* Market Status Colors */
  --market-open: #22C55E;     /* Trading active */
  --market-closed: #6B7280;   /* After hours */
  --market-pre: #F59E0B;      /* Pre-market */
  --market-holiday: #F97316;  /* Holiday */
  
  /* Chart Colors */
  --chart-bg: rgba(30, 27, 75, 0.4);
  --chart-grid: rgba(139, 92, 246, 0.1);
  --chart-border: rgba(139, 92, 246, 0.2);
}
```

### Animation Standards
```css
/* Real-time update animations */
.price-flash-up {
  animation: flashGreen 0.3s ease-out;
}

.price-flash-down {
  animation: flashRed 0.3s ease-out;
}

@keyframes flashGreen {
  0% { background-color: transparent; }
  50% { background-color: rgba(34, 197, 94, 0.2); }
  100% { background-color: transparent; }
}

@keyframes flashRed {
  0% { background-color: transparent; }
  50% { background-color: rgba(239, 68, 68, 0.2); }
  100% { background-color: transparent; }
}

/* Chart animations */
.chart-enter {
  animation: chartSlideIn 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes chartSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

## 🔧 Technical Implementation

### Component File Structure
```
src/components/market-data/
├── MarketDataTicker/
│   ├── MarketDataTicker.tsx
│   ├── MarketDataTicker.test.tsx
│   ├── MarketDataTicker.stories.tsx
│   └── index.ts
├── PriceChart/
│   ├── PriceChart.tsx
│   ├── ChartControls.tsx
│   ├── TechnicalIndicators.tsx
│   └── index.ts
├── OrderBook/
│   ├── OrderBook.tsx
│   ├── OrderBookLevel.tsx
│   └── index.ts
├── Watchlist/
│   ├── WatchlistManager.tsx
│   ├── WatchlistItem.tsx
│   ├── AddSymbolDialog.tsx
│   └── index.ts
└── MarketStatus/
    ├── MarketStatusIndicator.tsx
    └── index.ts
```

### WebSocket Integration
```typescript
// Real-time data connection
interface MarketDataWebSocket {
  connect(): void;
  disconnect(): void;
  subscribe(symbols: string[]): void;
  unsubscribe(symbols: string[]): void;
  onPriceUpdate: (callback: (data: PriceUpdate) => void) => void;
  onOrderBookUpdate: (callback: (data: OrderBookUpdate) => void) => void;
}

// Usage in components
const useMarketData = (symbols: string[]) => {
  const [prices, setPrices] = useState<Map<string, PriceData>>();
  
  useEffect(() => {
    const ws = new MarketDataWebSocket();
    ws.connect();
    ws.subscribe(symbols);
    
    ws.onPriceUpdate((update) => {
      setPrices(prev => new Map(prev.set(update.symbol, update.data)));
    });
    
    return () => {
      ws.unsubscribe(symbols);
      ws.disconnect();
    };
  }, [symbols]);
  
  return prices;
};
```

## ✅ Acceptance Criteria

### Functional Requirements
- [ ] **Real-time Updates**: Market data updates within 100ms of source
- [ ] **Symbol Support**: Support for 500+ NSE/BSE symbols
- [ ] **Chart Interaction**: Pinch zoom, pan, and tap interactions work smoothly
- [ ] **Watchlist Management**: Add/remove symbols, reorder via drag/drop
- [ ] **Market Status**: Accurate display of exchange status and trading hours
- [ ] **Offline Handling**: Graceful degradation when connection lost

### Performance Requirements
- [ ] **Load Time**: Dashboard loads within 2 seconds on 4G
- [ ] **Chart Rendering**: Charts render within 1 second of data receipt
- [ ] **Scroll Performance**: 60fps scrolling on ticker and watchlist
- [ ] **Memory Usage**: <100MB RAM usage on mobile devices
- [ ] **Battery Impact**: <5% battery usage per hour of active use

### Visual Requirements
- [ ] **Design Consistency**: Matches existing TradeMaster design system
- [ ] **Color Coding**: Consistent green/red for gains/losses
- [ ] **Typography**: Readable on mobile devices (16px+ base)
- [ ] **Touch Targets**: All interactive elements meet 44px minimum
- [ ] **Loading States**: Skeleton loaders for all components

### Accessibility Requirements
- [ ] **Screen Reader**: All components work with VoiceOver/TalkBack
- [ ] **Keyboard Navigation**: Tab order is logical and complete
- [ ] **Color Contrast**: 4.5:1 ratio for all text elements
- [ ] **Focus Indicators**: Visible focus states for all interactive elements
- [ ] **Motion Sensitivity**: Respects prefers-reduced-motion setting

### Technical Requirements
- [ ] **TypeScript**: Full type safety with no 'any' types
- [ ] **Testing**: 90%+ test coverage with unit and integration tests
- [ ] **Error Handling**: Graceful error states and recovery
- [ ] **Internationalization**: Support for English, Hindi locales
- [ ] **Analytics**: Track user interactions for UX optimization

## 🧪 Testing Strategy

### Unit Testing
```typescript
// Component testing examples
describe('MarketDataTicker', () => {
  it('renders symbol prices correctly', () => {
    render(<MarketDataTicker symbols={mockSymbols} />);
    expect(screen.getByText('RELIANCE')).toBeInTheDocument();
    expect(screen.getByText('₹2,345.60')).toBeInTheDocument();
  });
  
  it('shows green color for positive changes', () => {
    const symbol = { ...mockSymbol, change: 10.5 };
    render(<MarketDataTicker symbols={[symbol]} />);
    const changeElement = screen.getByText('+10.5');
    expect(changeElement).toHaveClass('text-bull-green');
  });
});
```

### Integration Testing
```typescript
// WebSocket integration testing
describe('Real-time Data Integration', () => {
  it('updates prices when WebSocket receives data', async () => {
    const mockWebSocket = createMockWebSocket();
    render(<MarketDataDashboard />);
    
    mockWebSocket.sendPriceUpdate({
      symbol: 'RELIANCE',
      price: 2350.00,
      change: 15.50
    });
    
    await waitFor(() => {
      expect(screen.getByText('₹2,350.00')).toBeInTheDocument();
    });
  });
});
```

### Visual Testing
```typescript
// Storybook stories for visual testing
export default {
  title: 'Market Data/Dashboard',
  component: MarketDataDashboard,
} as ComponentMeta<typeof MarketDataDashboard>;

export const Default = () => (
  <MarketDataDashboard symbols={mockSymbols} />
);

export const LoadingState = () => (
  <MarketDataDashboard symbols={[]} isLoading={true} />
);

export const ErrorState = () => (
  <MarketDataDashboard symbols={[]} error="Connection failed" />
);
```

### Mobile Testing
- **Device Testing**: Test on actual iOS/Android devices
- **Performance**: Lighthouse mobile performance > 90
- **Touch Testing**: All gestures work correctly
- **Orientation**: Portrait and landscape modes
- **Network**: Test on 3G/4G/WiFi conditions

## 🚀 Implementation Plan

### Week 1: Foundation Components
- **Day 1-2**: Set up component structure and TypeScript interfaces
- **Day 3-4**: Implement MarketDataTicker with mock data
- **Day 5**: Implement MarketStatusIndicator
- **Weekend**: Code review and testing

### Week 2: Interactive Components
- **Day 1-2**: Implement PriceChart with basic candlestick view
- **Day 3-4**: Implement OrderBook widget and Watchlist manager
- **Day 5**: WebSocket integration and real-time updates
- **Weekend**: Integration testing and performance optimization

### Week 3: Polish and Testing
- **Day 1-2**: Mobile responsiveness and touch interactions
- **Day 3-4**: Accessibility improvements and error handling
- **Day 5**: Final testing and documentation
- **Weekend**: Deployment preparation

## 📊 Success Metrics

### User Engagement
- **Dashboard Load Time**: <2 seconds average
- **Time on Dashboard**: >5 minutes average session
- **Interaction Rate**: >80% of users interact with charts
- **Return Rate**: >70% daily active users return next day

### Technical Performance
- **Uptime**: 99.9% availability during trading hours
- **Latency**: <100ms for real-time data updates
- **Error Rate**: <0.1% for critical operations
- **Mobile Performance**: Lighthouse score >90

### Business Impact
- **User Retention**: 5% improvement in 30-day retention
- **Trading Activity**: 15% increase in trades placed
- **Feature Adoption**: 60% of users use watchlist feature
- **Support Tickets**: <2 tickets/day related to market data

---

**Dependencies**: Epic 2.1 Market Data Service backend completion  
**Blockers**: None identified  
**Risk Level**: Medium - Complex real-time UI components  
**Review Required**: UI/UX team approval before development start