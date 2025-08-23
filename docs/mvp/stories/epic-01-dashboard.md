# Epic 1: Multi-Broker Dashboard
## Real-Time Unified View Across All Brokers

**Epic Goal**: Users can view their complete trading ecosystem in a single, real-time dashboard  
**Business Value**: Foundation for all platform features - users see immediate value  
**Timeline**: Weeks 1-2 (Sprint 1-2)  
**Story Points**: 21 points  
**Priority**: P0 (Critical)

---

## User Story Overview

| ID | Story | Points | Sprint | Priority | Value |
|----|-------|--------|--------|----------|-------|
| FE-001 | Real-time market data | 5 | 1 | P0 | High |
| FE-002 | WebSocket connection | 3 | 1 | P0 | High |
| FE-003 | Symbol search | 3 | 1 | P0 | High |
| FE-004 | Portfolio overview | 5 | 2 | P0 | High |
| FE-005 | Broker status display | 2 | 2 | P0 | Medium |
| FE-006 | Market overview cards | 1 | 2 | P1 | Medium |
| FE-007 | Responsive design | 1 | 2 | P0 | High |
| FE-008 | Error boundaries | 1 | 2 | P1 | Medium |

**Total**: 21 story points across 8 user stories

---

## FE-001: Real-Time Market Data Display

**As a** trader managing multiple broker accounts  
**I want to** see live price updates for all my tracked symbols  
**So that** I can make informed trading decisions without switching platforms

### Acceptance Criteria

#### AC1: Live Price Updates  
- **Given** I have symbols in my watchlist  
- **When** market prices change  
- **Then** I see price updates within 100ms  
- **And** price changes are visually highlighted (green for up, red for down)  
- **And** percentage changes are displayed with proper formatting

#### AC2: Multi-Exchange Support  
- **Given** I track symbols from NSE and BSE  
- **When** viewing the dashboard  
- **Then** I see prices from both exchanges clearly labeled  
- **And** exchange-specific data like volume and last traded time

#### AC3: Market Hours Handling  
- **Given** markets are closed  
- **When** viewing prices  
- **Then** I see "Market Closed" status  
- **And** previous close prices with "as of" timestamps  
- **And** next market opening time

### Technical Requirements

```typescript
interface MarketDataDisplay {
  symbol: string
  exchange: 'NSE' | 'BSE'
  currentPrice: number
  change: number
  changePercent: number
  volume: number
  lastTradeTime: Date
  marketStatus: 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_MARKET'
}
```

**Components Needed**:
- `PriceDisplayCard` with real-time updates
- `MarketStatusIndicator` for market hours
- `PriceChangeIndicator` with color coding

**WebSocket Integration**:
- Subscribe to price updates on component mount
- Handle connection failures with retry logic
- Batch updates for performance optimization

### Testing Scenarios

**Happy Path**:
1. User opens dashboard → sees current prices
2. Price changes → updates reflected immediately  
3. Market closes → status changes to closed

**Edge Cases**:
1. WebSocket disconnection → shows "connecting" status
2. Invalid symbol data → displays error state gracefully
3. High-frequency updates → throttling prevents UI lag

**Performance**:
- Price updates render in <50ms
- Dashboard handles 100+ symbols without lag
- Memory usage stays under 100MB

**Story Points**: 5 (Medium complexity, critical feature)  
**Dependencies**: WebSocket service, market data API  
**Risks**: WebSocket reliability, performance with many symbols

---

## FE-002: WebSocket Connection Management

**As a** user of the trading platform  
**I want** reliable real-time data connections  
**So that** my trading decisions are based on current market information

### Acceptance Criteria

#### AC1: Automatic Connection Management  
- **Given** I open the application  
- **When** the page loads  
- **Then** WebSocket connection is established automatically  
- **And** connection status is displayed to the user  
- **And** data subscription begins immediately

#### AC2: Reconnection on Failure  
- **Given** WebSocket connection is lost  
- **When** network issue is resolved  
- **Then** connection automatically re-establishes  
- **And** subscriptions are restored  
- **And** user sees "reconnected" notification

#### AC3: Connection Status Visibility  
- **Given** I'm using the application  
- **When** connection status changes  
- **Then** I see visual indicators:  
  - 🟢 Connected  
  - 🟡 Connecting  
  - 🔴 Disconnected  
- **And** appropriate user messaging

### Technical Requirements

```typescript
interface WebSocketService {
  connect(): Promise<void>
  disconnect(): void
  subscribe(channel: string, symbols: string[]): void
  unsubscribe(channel: string): void
  onMessage(callback: (data: any) => void): void
  onStatusChange(callback: (status: ConnectionStatus) => void): void
}

enum ConnectionStatus {
  CONNECTING = 'connecting',
  CONNECTED = 'connected', 
  DISCONNECTED = 'disconnected',
  RECONNECTING = 'reconnecting'
}
```

**Implementation Details**:
- Exponential backoff for reconnection (1s, 2s, 4s, 8s, max 30s)
- Heartbeat ping every 30 seconds
- Automatic cleanup on component unmount
- Queue messages during disconnection for replay

### Testing Scenarios

**Connection Tests**:
1. Normal connection → establishes in <3 seconds
2. Network failure → shows disconnected state
3. Network recovery → reconnects automatically  
4. Multiple reconnects → doesn't create memory leaks

**Subscription Management**:
1. Subscribe to channels → receives expected messages
2. Unsubscribe → stops receiving messages
3. Connection drop → subscriptions restored on reconnect

**Error Handling**:
1. Invalid WebSocket URL → shows meaningful error
2. Server rejection → displays user-friendly message
3. Timeout scenarios → appropriate fallback behavior

**Story Points**: 3 (Medium complexity, infrastructure)  
**Dependencies**: Backend WebSocket endpoint  
**Risks**: WebSocket server reliability, browser compatibility

---

## FE-003: Symbol Search and Watchlist

**As a** trader  
**I want to** easily search and add symbols to my watchlist  
**So that** I can track the instruments I'm interested in trading

### Acceptance Criteria

#### AC1: Fast Symbol Search  
- **Given** I want to find a trading symbol  
- **When** I type in the search box  
- **Then** I see auto-complete suggestions within 200ms  
- **And** suggestions include company name and exchange  
- **And** search works for both symbol codes and company names

#### AC2: Watchlist Management  
- **Given** I find a symbol I want to track  
- **When** I select it from search results  
- **Then** it's added to my watchlist immediately  
- **And** I can remove symbols from watchlist  
- **And** watchlist persists across sessions

#### AC3: Search Result Quality  
- **Given** I search for "RELI"  
- **When** viewing results  
- **Then** I see "RELIANCE - NSE" as top result  
- **And** results are ranked by relevance  
- **And** maximum 10 results shown

### Technical Requirements

```typescript
interface SymbolSearchResult {
  symbol: string
  companyName: string
  exchange: string
  instrumentType: 'EQUITY' | 'FUTURES' | 'OPTIONS'
  sector?: string
  isActive: boolean
}

interface Watchlist {
  userId: string
  symbols: string[]
  createdAt: Date
  updatedAt: Date
}
```

**Components Needed**:
- `SymbolSearchInput` with debounced search
- `SearchResultsList` with keyboard navigation
- `WatchlistDisplay` with drag-and-drop reordering
- `WatchlistItem` with price and remove button

**API Integration**:
- GET `/api/v1/symbols/search?q={query}&limit=10`
- POST `/api/v1/watchlist` to save watchlist
- Local caching for recent searches

### Testing Scenarios

**Search Functionality**:
1. Type "TCS" → see "TCS - NSE" in results
2. Type "Tata Consultancy" → see TCS in results  
3. Empty search → show popular symbols
4. Invalid search → show "No results found"

**Watchlist Operations**:
1. Add symbol → appears in watchlist immediately
2. Remove symbol → removed from UI and backend  
3. Reorder symbols → new order persists
4. Large watchlist → performance remains good

**Performance Tests**:
- Search responds in <200ms
- Auto-complete doesn't trigger on every keystroke
- Watchlist updates don't block UI

**Story Points**: 3 (Medium complexity, user interaction)  
**Dependencies**: Symbol search API, user preferences storage  
**Risks**: Search performance, large result sets

---

## FE-004: Portfolio Overview Dashboard

**As a** multi-broker trader  
**I want to** see my complete portfolio value and performance  
**So that** I understand my overall financial position at a glance

### Acceptance Criteria

#### AC1: Consolidated Portfolio Value  
- **Given** I have positions across multiple brokers  
- **When** I view the portfolio overview  
- **Then** I see total portfolio value across all brokers  
- **And** current day P&L with clear positive/negative indication  
- **And** overall P&L since inception

#### AC2: Broker-wise Breakdown  
- **Given** I have multiple broker connections  
- **When** viewing portfolio overview  
- **Then** I see value distribution by broker  
- **And** percentage allocation per broker  
- **And** performance comparison between brokers

#### AC3: Real-time Updates  
- **Given** market prices are changing  
- **When** viewing portfolio  
- **Then** portfolio value updates in real-time  
- **And** P&L changes reflect immediately  
- **And** update frequency is smooth (not jarring)

### Technical Requirements

```typescript
interface PortfolioOverview {
  totalValue: number
  totalCost: number
  unrealizedPnL: number
  realizedPnL: number
  dayPnL: number
  dayPnLPercent: number
  lastUpdated: Date
  
  brokerBreakdown: {
    brokerId: string
    value: number
    dayPnL: number
    percentage: number
  }[]
}
```

**Components Needed**:
- `PortfolioSummaryCard` with key metrics
- `BrokerBreakdownChart` (pie/donut chart)
- `PnLIndicator` with color-coded display
- `PortfolioMetrics` with animated counters

**Calculation Logic**:
- Aggregate positions from all connected brokers
- Calculate unrealized P&L using current market prices
- Handle currency formatting (₹ symbol, Indian number format)
- Update calculations on price changes

### Testing Scenarios

**Portfolio Calculations**:
1. Multiple positions → correct total value
2. Mix of profit/loss → accurate P&L calculation  
3. Price updates → portfolio value updates correctly
4. Broker connection issue → graceful degradation

**Visual Display**:
1. Large portfolio values → formatted properly (₹12,45,000)
2. Positive P&L → green indication with ↗ arrow
3. Negative P&L → red indication with ↘ arrow  
4. Zero P&L → neutral indication

**Real-time Performance**:
- Portfolio updates smoothly without flicker
- Calculations complete in <100ms
- UI remains responsive during updates

**Story Points**: 5 (High complexity, core feature)  
**Dependencies**: Portfolio aggregation API, real-time price data  
**Risks**: Calculation accuracy, performance with large portfolios

---

## FE-005: Broker Status and Connection Display

**As a** user with multiple broker accounts  
**I want to** see the connection status of all my brokers  
**So that** I know which accounts are available for trading

### Acceptance Criteria

#### AC1: Broker Connection Status  
- **Given** I have connected multiple brokers  
- **When** I view the dashboard  
- **Then** I see status for each broker:  
  - 🟢 Connected and fully functional  
  - 🟡 Connected but limited (rate limited, etc.)  
  - 🔴 Connection error or unavailable  
  - ⚫ Not connected

#### AC2: Connection Details  
- **Given** I want more information about broker status  
- **When** I hover over or click a broker status  
- **Then** I see detailed information:  
  - Last successful connection time  
  - API rate limit status  
  - Available features (trading, portfolio, etc.)  
  - Error message if applicable

#### AC3: Quick Actions  
- **Given** I see a broker with connection issues  
- **When** I want to fix the connection  
- **Then** I can click to:  
  - Retry connection  
  - Refresh authentication  
  - View detailed settings  
  - Get help/support

### Technical Requirements

```typescript
interface BrokerStatus {
  brokerId: string
  name: string
  status: 'connected' | 'limited' | 'error' | 'disconnected'
  lastConnected?: Date
  lastError?: string
  features: {
    trading: boolean
    portfolio: boolean
    marketData: boolean
  }
  rateLimit?: {
    current: number
    limit: number
    resetTime: Date
  }
}
```

**Components Needed**:
- `BrokerStatusCard` with status indicators
- `BrokerStatusBadge` for compact display
- `BrokerActionMenu` with quick actions
- `BrokerDetailsModal` for detailed information

**Status Monitoring**:
- Real-time status updates via WebSocket
- Periodic health checks (every 5 minutes)
- Error detection and user notification
- Auto-retry logic for temporary failures

### Testing Scenarios

**Status Display**:
1. All brokers connected → all show green status
2. One broker fails → shows red with error message
3. Rate limit hit → shows yellow with limit info
4. Network issue → shows appropriate error state

**User Actions**:
1. Retry connection → attempts reconnection
2. View details → shows modal with full information
3. Refresh auth → redirects to broker OAuth flow
4. Get help → shows relevant support information

**Real-time Updates**:
- Status changes reflect immediately in UI
- No polling - uses WebSocket for efficiency
- Graceful handling of temporary connection issues

**Story Points**: 2 (Low complexity, important UX)  
**Dependencies**: Broker health check API, WebSocket events  
**Risks**: False positive errors, broker API reliability

---

## FE-006: Market Overview Cards

**As a** trader  
**I want to** see key market indices and their performance  
**So that** I understand overall market sentiment

### Acceptance Criteria

#### AC1: Index Display  
- **Given** I want to understand market direction  
- **When** I view the dashboard  
- **Then** I see key indices:  
  - NIFTY 50  
  - SENSEX  
  - BANK NIFTY  
- **And** current values with day change and percentage

#### AC2: Market Status  
- **Given** I need to know trading hours  
- **When** viewing market overview  
- **Then** I see current market status:  
  - 🟢 Market Open  
  - 🔴 Market Closed  
  - 🟡 Pre-market/After-market  
- **And** next market session timing

#### AC3: Visual Indicators  
- **Given** market indices are changing  
- **When** viewing the overview  
- **Then** I see color-coded performance:  
  - Green for positive movement  
  - Red for negative movement  
  - Neutral for flat/unchanged

### Technical Requirements

```typescript
interface MarketIndex {
  name: string
  symbol: string
  currentValue: number
  change: number
  changePercent: number
  volume?: number
  lastUpdated: Date
}

interface MarketStatus {
  status: 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_MARKET'
  nextSessionTime?: Date
  currentTime: Date
}
```

**Components Needed**:
- `MarketIndexCard` with index data
- `MarketStatusBadge` for session info
- `IndexChangeIndicator` for performance display

**Data Requirements**:
- Real-time index data updates
- Market timing information
- Historical comparison data

### Testing Scenarios

**Data Display**:
1. Market open → shows live index values
2. Market closed → shows last close with timestamp
3. Index up → green color with ↗ arrow
4. Index down → red color with ↘ arrow

**Market Status**:
1. During market hours → shows "Market Open"
2. After hours → shows "Market Closed" with next open time
3. Weekend → shows next Monday opening
4. Holiday → shows next trading day

**Story Points**: 1 (Low complexity, nice-to-have)  
**Dependencies**: Market data API, market timing service  
**Risks**: Data provider reliability

---

## FE-007: Mobile Responsive Design

**As a** trader who primarily uses mobile devices  
**I want** the dashboard to work perfectly on my phone  
**So that** I can trade effectively on the go

### Acceptance Criteria

#### AC1: Mobile Layout Optimization  
- **Given** I access the dashboard on mobile  
- **When** viewing in portrait mode  
- **Then** all content fits without horizontal scrolling  
- **And** text is readable without zooming  
- **And** touch targets are at least 44px

#### AC2: Touch Interactions  
- **Given** I'm using touch interface  
- **When** interacting with elements  
- **Then** buttons respond immediately to touch  
- **And** scrolling is smooth and natural  
- **And** pinch-to-zoom works on charts

#### AC3: Performance on Mobile  
- **Given** I'm on a slower mobile connection  
- **When** loading the dashboard  
- **Then** critical content loads within 3 seconds  
- **And** subsequent interactions are responsive  
- **And** data usage is optimized

### Technical Requirements

**Responsive Breakpoints**:
- Mobile: 320px - 767px
- Tablet: 768px - 1023px  
- Desktop: 1024px+

**Touch Optimizations**:
- Minimum touch target: 44px × 44px
- Touch-friendly spacing: 8px minimum
- Thumb-zone optimization for key actions
- Gesture support for common actions

**Performance Requirements**:
- First Contentful Paint: <2s on 3G
- Time to Interactive: <3s on 3G
- Bundle size: <500KB initial load

```css
/* Example responsive utilities */
.mobile-container {
  width: 100%;
  max-width: 28rem; /* 448px */
  margin: 0 auto;
  padding: 1rem;
}

@media (min-width: 768px) {
  .desktop-container {
    max-width: 32rem;
    padding: 2rem;
  }
}
```

### Testing Scenarios

**Device Testing**:
1. iPhone SE (375px) → all content accessible
2. Android (360px) → no horizontal scroll
3. iPad (768px) → optimal tablet layout
4. Large phone (414px) → comfortable use

**Interaction Testing**:
1. Tap buttons → immediate visual feedback
2. Scroll lists → smooth scrolling performance
3. Swipe gestures → work where implemented
4. Form inputs → keyboard doesn't obscure content

**Performance Testing**:
- Test on slow 3G connection
- Monitor bundle size and loading time
- Check memory usage on low-end devices

**Story Points**: 1 (Low complexity, essential UX)  
**Dependencies**: Existing responsive utilities, mobile testing devices  
**Risks**: Device fragmentation, performance on low-end devices

---

## FE-008: Error Boundaries and Error Handling

**As a** user  
**I want** graceful error handling when things go wrong  
**So that** I can continue using the platform even when some features fail

### Acceptance Criteria

#### AC1: Graceful Error Recovery  
- **Given** a component encounters an error  
- **When** the error occurs  
- **Then** only that component shows an error state  
- **And** the rest of the application continues working  
- **And** I see a user-friendly error message

#### AC2: Error Reporting  
- **Given** an error occurs  
- **When** it happens  
- **Then** error details are logged for debugging  
- **And** I have option to report the issue  
- **And** I can retry the failed operation

#### AC3: Fallback States  
- **Given** real-time data fails to load  
- **When** viewing affected components  
- **Then** I see cached data with "last updated" timestamp  
- **And** clear indication that data may be stale  
- **And** option to refresh manually

### Technical Requirements

```typescript
interface ErrorBoundary {
  componentDidCatch(error: Error, errorInfo: ErrorInfo): void
  getDerivedStateFromError(error: Error): ErrorState
  render(): ReactNode
}

interface ErrorState {
  hasError: boolean
  error?: Error
  errorId: string
  retryCount: number
}
```

**Error Categories**:
- Network errors (API failures, WebSocket disconnection)
- Data errors (invalid response format, calculation errors)
- UI errors (component render failures, state corruption)
- Permission errors (authentication, authorization)

**Error Recovery Strategies**:
- Automatic retry with exponential backoff
- Fallback to cached data
- Partial feature degradation
- Clear user communication and action options

### Testing Scenarios

**Error Simulation**:
1. Network failure → shows offline state with cached data
2. Invalid API response → shows error with retry option
3. Component crash → error boundary prevents app crash
4. WebSocket disconnection → shows connecting state

**User Experience**:
1. Error message → clear, non-technical language
2. Retry action → works and clears error state
3. Report issue → collects relevant debug information
4. Fallback data → clearly marked as potentially stale

**Error Recovery**:
- Automatic retry succeeds → normal state restored
- Manual retry → user feedback and state management
- Permanent error → graceful degradation with alternatives

**Story Points**: 1 (Low complexity, reliability feature)  
**Dependencies**: Error logging service, monitoring system  
**Risks**: Over-engineering error handling, poor error UX

---

## Sprint Allocation

### Sprint 1 (Week 1): Real-time Foundation
**Goal**: Establish real-time data flow and search capability  
**Stories**: FE-001, FE-002, FE-003  
**Story Points**: 11 points  

**Sprint Success Criteria**:
- Real-time price updates working
- WebSocket connection stable with reconnection
- Symbol search functional with watchlist
- Mobile responsive layout

### Sprint 2 (Week 2): Dashboard Complete
**Goal**: Complete unified dashboard with portfolio overview  
**Stories**: FE-004, FE-005, FE-006, FE-007, FE-008  
**Story Points**: 10 points  

**Sprint Success Criteria**:
- Portfolio overview showing consolidated data
- Broker status display with health monitoring
- Market overview for context
- Fully responsive mobile experience
- Robust error handling throughout

---

## Definition of Done for Epic 1

### Technical Requirements
- [ ] All WebSocket connections stable and tested
- [ ] Real-time data updates with <100ms latency
- [ ] Portfolio calculations accurate to 4 decimal places
- [ ] Mobile responsive on devices 320px+
- [ ] Error boundaries prevent app crashes

### User Experience Requirements
- [ ] Dashboard loads in <2 seconds on 3G
- [ ] Smooth animations and transitions
- [ ] Clear visual feedback for all user actions
- [ ] Accessible keyboard navigation
- [ ] WCAG 2.1 AA color contrast compliance

### Quality Assurance
- [ ] 90%+ unit test coverage for all components
- [ ] Integration tests for WebSocket functionality
- [ ] Cross-browser testing (Chrome, Safari, Firefox)
- [ ] Mobile device testing on iOS and Android
- [ ] Performance testing with 100+ symbols in watchlist

### Business Value Delivered
- [ ] Users can see consolidated portfolio in real-time
- [ ] Multi-broker data aggregation working
- [ ] Foundation established for trading features
- [ ] Mobile-first experience ready for 85% mobile users
- [ ] Platform stability and error recovery proven

**Epic 1 Success = Foundation Complete for Revenue-Generating Features**