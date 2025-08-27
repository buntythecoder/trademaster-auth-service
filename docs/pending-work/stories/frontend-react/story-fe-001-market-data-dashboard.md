# Story FE-001: Market Data Dashboard Integration

## 📋 Story Overview

**Story ID**: FE-001  
**Epic**: PW-001 (Frontend Core Implementation)  
**Title**: Market Data Dashboard Real-time Integration  
**Priority**: 🔥 **CRITICAL**  
**Effort**: 8 Story Points  
**Owner**: Frontend Lead  
**Sprint**: 1-2  

## 🎯 User Story

**As a** trader using TradeMaster  
**I want** to see real-time market data including live prices, charts, and market information  
**So that** I can make informed trading decisions based on current market conditions  

## 📝 Detailed Description

Replace the current mock data implementation in the market data dashboard with real-time WebSocket connections to the market data service. This includes live price updates, interactive charts, real-time order book data, and market news integration.

**Current State**: Market data dashboard shows mock data and placeholder charts  
**Desired State**: Fully functional real-time market data display with live updates  

## ✅ Acceptance Criteria

### AC-1: Real-time Price Updates
- [ ] **GIVEN** I am viewing the market data dashboard
- [ ] **WHEN** a stock price changes in the market
- [ ] **THEN** I see the updated price within 100ms on my screen
- [ ] **AND** the price change is highlighted (green for up, red for down)
- [ ] **AND** the percentage change and absolute change are displayed correctly

### AC-2: WebSocket Connection Management
- [ ] **GIVEN** I am using the market data dashboard
- [ ] **WHEN** I navigate to the page
- [ ] **THEN** a WebSocket connection is established automatically
- [ ] **AND** if the connection drops, it automatically reconnects within 5 seconds
- [ ] **AND** I see a connection status indicator showing online/offline status
- [ ] **AND** reconnection attempts are logged and monitored

### AC-3: Interactive TradingView Charts
- [ ] **GIVEN** I select a stock symbol
- [ ] **WHEN** I view the chart
- [ ] **THEN** I see a professional TradingView chart with real historical data
- [ ] **AND** the chart updates in real-time with current price movements
- [ ] **AND** I can change timeframes (1m, 5m, 15m, 1h, 1d, 1w, 1m)
- [ ] **AND** I can add technical indicators and drawing tools
- [ ] **AND** chart loads within 2 seconds

### AC-4: Order Book Display
- [ ] **GIVEN** I am viewing a stock's market data
- [ ] **WHEN** I look at the order book section
- [ ] **THEN** I see real-time bid and ask prices with quantities
- [ ] **AND** the order book updates in real-time as orders change
- [ ] **AND** I can see market depth (top 10 bid/ask levels)
- [ ] **AND** I can click on a price level to quickly set my order price

### AC-5: Watchlist Management
- [ ] **GIVEN** I have created a watchlist
- [ ] **WHEN** I view my watchlist
- [ ] **THEN** I see real-time prices for all symbols in my watchlist
- [ ] **AND** I can add/remove symbols from the watchlist
- [ ] **AND** watchlist data persists across sessions
- [ ] **AND** I can reorder symbols by dragging and dropping
- [ ] **AND** watchlist updates automatically every second

### AC-6: Symbol Search
- [ ] **GIVEN** I want to search for a stock
- [ ] **WHEN** I type in the search box
- [ ] **THEN** I see autocomplete suggestions from real market data
- [ ] **AND** search results include symbol, company name, and current price
- [ ] **AND** search is responsive and shows results within 200ms
- [ ] **AND** I can search by symbol, company name, or sector

### AC-7: Market Status & Trading Hours
- [ ] **GIVEN** I am viewing the market data dashboard
- [ ] **WHEN** I check market status
- [ ] **THEN** I see current market status (Open, Closed, Pre-market, After-hours)
- [ ] **AND** I see remaining time until market open/close
- [ ] **AND** market status updates automatically
- [ ] **AND** I see different indicators for BSE, NSE, and MCX markets

### AC-8: Economic Calendar Integration
- [ ] **GIVEN** I want to see upcoming market events
- [ ] **WHEN** I view the economic calendar section
- [ ] **THEN** I see today's economic events that might affect markets
- [ ] **AND** events are categorized by importance (High, Medium, Low)
- [ ] **AND** I can see event details including expected vs actual values
- [ ] **AND** calendar updates automatically as new events are published

### AC-9: Market News Feed
- [ ] **GIVEN** I want to stay updated on market news
- [ ] **WHEN** I view the news section
- [ ] **THEN** I see latest market-relevant news articles
- [ ] **AND** news articles are updated in real-time
- [ ] **AND** I can filter news by categories (market, company, sector)
- [ ] **AND** clicking on news opens detailed article view
- [ ] **AND** news source and timestamp are clearly displayed

### AC-10: Mobile Responsive Design
- [ ] **GIVEN** I am using TradeMaster on a mobile device
- [ ] **WHEN** I access the market data dashboard
- [ ] **THEN** all features work seamlessly on mobile
- [ ] **AND** charts are touch-optimized with pinch/zoom functionality
- [ ] **AND** watchlist items are easy to tap and manage
- [ ] **AND** data loads quickly even on slower mobile connections

## 🔧 Technical Implementation Details

### Frontend Architecture
```typescript
// WebSocket Service for Market Data
export class MarketDataWebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectInterval = 5000
  
  connect(symbols: string[]): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(MARKET_DATA_WS_URL)
      
      this.ws.onopen = () => {
        this.subscribe(symbols)
        this.reconnectAttempts = 0
        resolve()
      }
      
      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        this.handleMarketDataUpdate(data)
      }
      
      this.ws.onclose = () => {
        this.handleReconnection()
      }
      
      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        reject(error)
      }
    })
  }
  
  private handleReconnection(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      setTimeout(() => {
        this.reconnectAttempts++
        this.connect(this.subscribedSymbols)
      }, this.reconnectInterval)
    }
  }
}

// Market Data Store (Redux Toolkit)
export const marketDataSlice = createSlice({
  name: 'marketData',
  initialState: {
    prices: {},
    orderBooks: {},
    connectionStatus: 'disconnected',
    watchlist: [],
    selectedSymbol: null,
    chartData: {},
    news: [],
    economicEvents: []
  },
  reducers: {
    updatePrice: (state, action) => {
      const { symbol, price, change, changePercent } = action.payload
      state.prices[symbol] = {
        current: price,
        change,
        changePercent,
        timestamp: Date.now()
      }
    },
    updateOrderBook: (state, action) => {
      const { symbol, bids, asks } = action.payload
      state.orderBooks[symbol] = { bids, asks }
    },
    setConnectionStatus: (state, action) => {
      state.connectionStatus = action.payload
    }
  }
})
```

### API Integration Points
```typescript
// Market Data API Service
export class MarketDataAPIService {
  async getHistoricalData(symbol: string, timeframe: string): Promise<OHLCData[]> {
    const response = await api.get(`/api/v1/market-data/historical/${symbol}`, {
      params: { timeframe }
    })
    return response.data
  }
  
  async searchSymbols(query: string): Promise<SymbolSearchResult[]> {
    const response = await api.get('/api/v1/market-data/search', {
      params: { q: query, limit: 10 }
    })
    return response.data
  }
  
  async getEconomicCalendar(): Promise<EconomicEvent[]> {
    const response = await api.get('/api/v1/market-data/economic-calendar')
    return response.data
  }
  
  async getMarketNews(): Promise<NewsArticle[]> {
    const response = await api.get('/api/v1/market-data/news/latest')
    return response.data
  }
}

// Data Models
interface MarketPrice {
  symbol: string
  current: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  open: number
  previousClose: number
  timestamp: number
}

interface OrderBookLevel {
  price: number
  quantity: number
  orders: number
}

interface OrderBook {
  symbol: string
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
  timestamp: number
}
```

### Component Structure
```typescript
// Main Market Data Dashboard Component
export const MarketDataDashboard: React.FC = () => {
  const dispatch = useAppDispatch()
  const marketData = useAppSelector(selectMarketData)
  const { connect, disconnect } = useMarketDataWebSocket()
  
  useEffect(() => {
    // Connect to WebSocket when component mounts
    connect(marketData.watchlist)
    
    return () => {
      disconnect()
    }
  }, [])
  
  return (
    <div className="market-data-dashboard">
      <MarketStatusBar />
      <div className="dashboard-grid">
        <WatchlistPanel />
        <ChartingPanel />
        <OrderBookPanel />
        <NewsPanel />
        <EconomicCalendarPanel />
      </div>
    </div>
  )
}

// Individual Panel Components
export const WatchlistPanel: React.FC = () => {
  const watchlist = useAppSelector(selectWatchlist)
  const prices = useAppSelector(selectPrices)
  
  return (
    <Panel title="Watchlist">
      {watchlist.map(symbol => (
        <WatchlistItem 
          key={symbol}
          symbol={symbol}
          price={prices[symbol]}
        />
      ))}
    </Panel>
  )
}
```

## 🧪 Testing Strategy

### Unit Tests
```typescript
// WebSocket Service Tests
describe('MarketDataWebSocketService', () => {
  let service: MarketDataWebSocketService
  let mockWebSocket: jest.Mocked<WebSocket>
  
  beforeEach(() => {
    service = new MarketDataWebSocketService()
    mockWebSocket = new MockWebSocket() as jest.Mocked<WebSocket>
    global.WebSocket = jest.fn(() => mockWebSocket)
  })
  
  test('should connect and subscribe to symbols', async () => {
    const symbols = ['RELIANCE', 'TCS']
    const connectPromise = service.connect(symbols)
    
    // Simulate WebSocket connection
    mockWebSocket.onopen()
    
    await connectPromise
    expect(mockWebSocket.send).toHaveBeenCalledWith(
      JSON.stringify({ action: 'subscribe', symbols })
    )
  })
  
  test('should handle reconnection on connection loss', () => {
    jest.useFakeTimers()
    service.connect(['RELIANCE'])
    
    // Simulate connection loss
    mockWebSocket.onclose()
    
    // Fast-forward time to trigger reconnection
    jest.advanceTimersByTime(5000)
    
    expect(global.WebSocket).toHaveBeenCalledTimes(2)
    jest.useRealTimers()
  })
})

// Redux Store Tests
describe('marketDataSlice', () => {
  test('should update price correctly', () => {
    const initialState = { prices: {}, /* other state */ }
    
    const newState = marketDataSlice.reducer(initialState, {
      type: 'marketData/updatePrice',
      payload: {
        symbol: 'RELIANCE',
        price: 2450.50,
        change: 25.30,
        changePercent: 1.04
      }
    })
    
    expect(newState.prices['RELIANCE']).toEqual({
      current: 2450.50,
      change: 25.30,
      changePercent: 1.04,
      timestamp: expect.any(Number)
    })
  })
})
```

### Integration Tests
```typescript
// Component Integration Tests
describe('MarketDataDashboard Integration', () => {
  test('should display real-time price updates', async () => {
    render(<MarketDataDashboard />)
    
    // Mock WebSocket message
    const mockPriceUpdate = {
      type: 'price_update',
      symbol: 'RELIANCE',
      price: 2450.50,
      change: 25.30
    }
    
    // Simulate WebSocket message
    act(() => {
      mockWebSocketService.simulateMessage(mockPriceUpdate)
    })
    
    await waitFor(() => {
      expect(screen.getByText('₹2,450.50')).toBeInTheDocument()
      expect(screen.getByText('+25.30')).toBeInTheDocument()
    })
  })
})
```

### E2E Tests
```typescript
// Cypress E2E Tests
describe('Market Data Dashboard E2E', () => {
  it('should show real-time market data', () => {
    cy.visit('/market-data')
    
    // Check that WebSocket connection is established
    cy.get('[data-testid=connection-status]').should('contain', 'Connected')
    
    // Check that watchlist loads
    cy.get('[data-testid=watchlist]').should('be.visible')
    cy.get('[data-testid=watchlist-item]').should('have.length.greaterThan', 0)
    
    // Check that prices update
    cy.get('[data-testid=price-RELIANCE]').should('not.be.empty')
    
    // Check that chart loads
    cy.get('[data-testid=trading-chart]').should('be.visible')
    
    // Test symbol search
    cy.get('[data-testid=symbol-search]').type('TCS')
    cy.get('[data-testid=search-results]').should('contain', 'TCS')
  })
  
  it('should handle connection loss gracefully', () => {
    cy.visit('/market-data')
    
    // Simulate connection loss
    cy.window().then(win => {
      win.dispatchEvent(new Event('offline'))
    })
    
    cy.get('[data-testid=connection-status]').should('contain', 'Reconnecting')
    
    // Simulate connection restoration
    cy.window().then(win => {
      win.dispatchEvent(new Event('online'))
    })
    
    cy.get('[data-testid=connection-status]').should('contain', 'Connected')
  })
})
```

## 📊 Performance Requirements

### Response Time Targets
- **WebSocket Connection**: <2s initial connection
- **Price Updates**: <100ms from market to UI display
- **Chart Loading**: <3s for initial chart load
- **Symbol Search**: <200ms for search results
- **Component Render**: <16ms per frame (60fps)

### Scalability Requirements
- **Concurrent Connections**: Support 1000+ concurrent WebSocket connections
- **Data Throughput**: Handle 100+ price updates per second
- **Memory Usage**: <50MB for market data cache
- **Network Bandwidth**: <1MB/minute per active user

## 🔒 Security Considerations

- **WebSocket Authentication**: JWT token validation for WebSocket connections
- **Data Validation**: Validate all incoming market data to prevent injection
- **Rate Limiting**: Limit WebSocket message frequency per user
- **CORS Configuration**: Proper CORS headers for market data APIs
- **Input Sanitization**: Sanitize all user inputs for symbol search

## 📈 Analytics & Monitoring

### Key Metrics to Track
- WebSocket connection success rate
- Average price update latency
- Chart load performance
- User engagement with different features
- Error rates for market data operations
- Mobile vs desktop usage patterns

### Error Monitoring
- WebSocket connection failures
- Market data API errors
- Chart rendering failures
- Symbol search timeouts

## 🔗 Dependencies

### Internal Dependencies
- ✅ Market Data Service APIs must be operational
- ✅ Authentication service for WebSocket connections
- ⚠️ TradingView charting library license
- ⚠️ WebSocket infrastructure setup

### External Dependencies
- ⚠️ BSE/NSE market data access permissions
- ⚠️ News feed API integration
- ⚠️ Economic calendar data provider

## 🚀 Definition of Done

- [ ] All acceptance criteria met and tested
- [ ] Unit test coverage >80%
- [ ] Integration tests pass
- [ ] E2E tests pass
- [ ] Performance requirements met
- [ ] Security review passed
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Deployed to staging and tested
- [ ] Product owner acceptance

---

**Business Impact**: This story enables real trading functionality and transforms TradeMaster from a prototype to a functional trading platform. Success directly impacts user satisfaction and revenue potential.

**Technical Risk**: Medium - requires complex WebSocket management and real-time data synchronization, but builds on existing backend infrastructure.

**User Value**: High - provides essential market data functionality that all trading decisions depend on.