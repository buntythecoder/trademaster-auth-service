# Story FE-004: Real-time WebSocket Architecture

## Epic
Epic 2: Market Data & Trading Foundation

## Story Overview
**As a** TradeMaster user  
**I want** real-time market data updates without page refreshes  
**So that** I can make informed trading decisions based on live market information

## Business Value
- **Competitive Advantage**: Sub-100ms market data delivery for faster decision making
- **User Experience**: Seamless real-time updates without manual refresh
- **Data Accuracy**: Always current market data for reliable trading
- **Reduced Load**: Efficient data streaming reduces server load vs polling

## Technical Requirements

### WebSocket Connection Management
```typescript
// WebSocket Service Implementation
class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private subscribers = new Map<string, Set<SubscriptionCallback>>()
  private heartbeatInterval: NodeJS.Timeout | null = null
  private messageQueue: QueuedMessage[] = []
  private isConnecting = false
  
  constructor(private config: WebSocketConfig) {
    this.connect()
  }
  
  private async connect(): Promise<void> {
    if (this.isConnecting || this.ws?.readyState === WebSocket.OPEN) {
      return
    }
    
    this.isConnecting = true
    
    try {
      const token = await this.getAuthToken()
      const wsUrl = `${this.config.url}?token=${encodeURIComponent(token)}`
      
      this.ws = new WebSocket(wsUrl)
      this.setupEventHandlers()
      
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      this.handleReconnect()
    } finally {
      this.isConnecting = false
    }
  }
  
  private setupEventHandlers(): void {
    if (!this.ws) return
    
    this.ws.onopen = () => {
      console.log('WebSocket connected')
      this.reconnectAttempts = 0
      this.isConnecting = false
      
      // Send queued messages
      this.processMessageQueue()
      
      // Start heartbeat
      this.startHeartbeat()
      
      // Resubscribe to channels
      this.resubscribeAll()
    }
    
    this.ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        this.handleMessage(message)
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }
    
    this.ws.onclose = (event) => {
      console.log('WebSocket disconnected:', event.code, event.reason)
      this.stopHeartbeat()
      
      if (!event.wasClean) {
        this.handleReconnect()
      }
    }
    
    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error)
    }
  }
  
  private handleMessage(message: WebSocketMessage): void {
    const { type, channel, data } = message
    
    switch (type) {
      case 'data':
        this.notifySubscribers(channel, data)
        break
        
      case 'heartbeat':
        // Heartbeat received, connection is alive
        break
        
      case 'error':
        console.error('WebSocket server error:', data)
        break
        
      case 'subscription_confirmed':
        console.log('Subscription confirmed for channel:', channel)
        break
        
      default:
        console.warn('Unknown message type:', type)
    }
  }
  
  private notifySubscribers(channel: string, data: any): void {
    const channelSubscribers = this.subscribers.get(channel)
    if (channelSubscribers) {
      channelSubscribers.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error('Subscriber callback error:', error)
        }
      })
    }
  }
  
  public subscribe(channel: string, callback: SubscriptionCallback): () => void {
    // Add subscriber
    if (!this.subscribers.has(channel)) {
      this.subscribers.set(channel, new Set())
    }
    this.subscribers.get(channel)!.add(callback)
    
    // Send subscription message
    this.send({
      type: 'subscribe',
      channel: channel
    })
    
    // Return unsubscribe function
    return () => {
      const channelSubscribers = this.subscribers.get(channel)
      if (channelSubscribers) {
        channelSubscribers.delete(callback)
        
        // If no more subscribers, unsubscribe from channel
        if (channelSubscribers.size === 0) {
          this.subscribers.delete(channel)
          this.send({
            type: 'unsubscribe',
            channel: channel
          })
        }
      }
    }
  }
  
  private send(message: OutgoingMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      // Queue message for later
      this.messageQueue.push({
        message,
        timestamp: Date.now()
      })
    }
  }
  
  private handleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached')
      return
    }
    
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts)
    this.reconnectAttempts++
    
    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`)
    
    setTimeout(() => {
      this.connect()
    }, delay)
  }
  
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.send({ type: 'ping' })
    }, 30000) // 30 seconds
  }
  
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }
  
  public disconnect(): void {
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }
  }
}

// WebSocket Hook for React Components
export const useWebSocket = () => {
  const wsService = useMemo(() => {
    return new WebSocketService({
      url: process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws',
      reconnectDelay: 1000,
      maxReconnectAttempts: 5
    })
  }, [])
  
  useEffect(() => {
    return () => {
      wsService.disconnect()
    }
  }, [wsService])
  
  return wsService
}
```

### Real-time Data Hooks
```typescript
// Market Data Hook
export const useMarketData = (symbols: string[]) => {
  const wsService = useWebSocket()
  const [data, setData] = useState<Map<string, MarketData>>(new Map())
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting')
  
  useEffect(() => {
    if (symbols.length === 0) return
    
    const unsubscribers: (() => void)[] = []
    
    // Subscribe to each symbol
    symbols.forEach(symbol => {
      const channel = `market.${symbol}`
      const unsubscribe = wsService.subscribe(channel, (marketData: MarketData) => {
        setData(prev => new Map(prev.set(symbol, marketData)))
      })
      unsubscribers.push(unsubscribe)
    })
    
    // Connection status tracking
    const statusUnsubscribe = wsService.subscribe('connection.status', (status) => {
      setConnectionStatus(status)
    })
    unsubscribers.push(statusUnsubscribe)
    
    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe())
    }
  }, [symbols, wsService])
  
  return { data, connectionStatus }
}

// Order Book Hook
export const useOrderBook = (symbol: string) => {
  const wsService = useWebSocket()
  const [orderBook, setOrderBook] = useState<OrderBook | null>(null)
  const [lastUpdate, setLastUpdate] = useState<number>(0)
  
  useEffect(() => {
    if (!symbol) return
    
    const channel = `orderbook.${symbol}`
    const unsubscribe = wsService.subscribe(channel, (data: OrderBookUpdate) => {
      setOrderBook(prev => {
        if (!prev) return data.orderBook
        
        // Merge incremental updates
        return mergeOrderBookUpdate(prev, data)
      })
      setLastUpdate(Date.now())
    })
    
    return unsubscribe
  }, [symbol, wsService])
  
  return { orderBook, lastUpdate }
}

// Trade Stream Hook
export const useTradeStream = (symbol: string) => {
  const wsService = useWebSocket()
  const [trades, setTrades] = useState<Trade[]>([])
  const maxTrades = 100
  
  useEffect(() => {
    if (!symbol) return
    
    const channel = `trades.${symbol}`
    const unsubscribe = wsService.subscribe(channel, (trade: Trade) => {
      setTrades(prev => {
        const newTrades = [trade, ...prev].slice(0, maxTrades)
        return newTrades
      })
    })
    
    return unsubscribe
  }, [symbol, wsService])
  
  return trades
}

// Portfolio Updates Hook
export const usePortfolioUpdates = (userId: string) => {
  const wsService = useWebSocket()
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null)
  const [positions, setPositions] = useState<Position[]>([])
  
  useEffect(() => {
    const portfolioChannel = `portfolio.${userId}`
    const positionsChannel = `positions.${userId}`
    
    const portfolioUnsubscribe = wsService.subscribe(portfolioChannel, (data: PortfolioUpdate) => {
      setPortfolio(data.portfolio)
    })
    
    const positionsUnsubscribe = wsService.subscribe(positionsChannel, (data: PositionUpdate) => {
      setPositions(prev => {
        const existing = prev.find(p => p.symbol === data.position.symbol)
        if (existing) {
          return prev.map(p => p.symbol === data.position.symbol ? data.position : p)
        } else {
          return [...prev, data.position]
        }
      })
    })
    
    return () => {
      portfolioUnsubscribe()
      positionsUnsubscribe()
    }
  }, [userId, wsService])
  
  return { portfolio, positions }
}
```

### Performance Optimizations
```typescript
// Message Batching and Throttling
class MessageBatcher {
  private batches = new Map<string, any[]>()
  private timeouts = new Map<string, NodeJS.Timeout>()
  private readonly batchDelay = 50 // 50ms batching window
  
  public addMessage(channel: string, data: any, callback: (batch: any[]) => void): void {
    if (!this.batches.has(channel)) {
      this.batches.set(channel, [])
    }
    
    this.batches.get(channel)!.push(data)
    
    // Clear existing timeout
    const existingTimeout = this.timeouts.get(channel)
    if (existingTimeout) {
      clearTimeout(existingTimeout)
    }
    
    // Set new timeout
    const timeout = setTimeout(() => {
      const batch = this.batches.get(channel) || []
      if (batch.length > 0) {
        callback(batch)
        this.batches.delete(channel)
        this.timeouts.delete(channel)
      }
    }, this.batchDelay)
    
    this.timeouts.set(channel, timeout)
  }
}

// Optimized Market Data Component
export const MarketDataGrid: React.FC<{ symbols: string[] }> = ({ symbols }) => {
  const { data, connectionStatus } = useMarketData(symbols)
  const [displayData, setDisplayData] = useState<Map<string, MarketData>>(new Map())
  
  // Use message batching to reduce render frequency
  const batcher = useMemo(() => new MessageBatcher(), [])
  
  useEffect(() => {
    data.forEach((marketData, symbol) => {
      batcher.addMessage(symbol, marketData, () => {
        setDisplayData(prev => new Map(prev.set(symbol, marketData)))
      })
    })
  }, [data, batcher])
  
  // Virtualization for large symbol lists
  const virtualizer = useVirtualizer({
    count: symbols.length,
    getScrollElement: () => document.getElementById('market-data-container'),
    estimateSize: () => 60,
    overscan: 10
  })
  
  return (
    <div id="market-data-container" className="market-data-grid">
      <ConnectionStatus status={connectionStatus} />
      
      <div
        style={{
          height: virtualizer.getTotalSize(),
          width: '100%',
          position: 'relative'
        }}
      >
        {virtualizer.getVirtualItems().map(virtualItem => {
          const symbol = symbols[virtualItem.index]
          const marketData = displayData.get(symbol)
          
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
              <MarketDataRow symbol={symbol} data={marketData} />
            </div>
          )
        })}
      </div>
    </div>
  )
}
```

### Error Handling and Fallback
```typescript
// Error Boundary for WebSocket Components
export class WebSocketErrorBoundary extends React.Component<
  { children: ReactNode; fallback: ReactNode },
  { hasError: boolean }
> {
  constructor(props: any) {
    super(props)
    this.state = { hasError: false }
  }
  
  static getDerivedStateFromError(error: Error) {
    return { hasError: true }
  }
  
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('WebSocket error:', error, errorInfo)
    // Log to error reporting service
    errorReportingService.reportError(error, { context: 'WebSocket', ...errorInfo })
  }
  
  render() {
    if (this.state.hasError) {
      return this.props.fallback
    }
    
    return this.props.children
  }
}

// Fallback to REST API when WebSocket fails
export const useMarketDataWithFallback = (symbols: string[]) => {
  const { data: wsData, connectionStatus } = useMarketData(symbols)
  const [fallbackData, setFallbackData] = useState<Map<string, MarketData>>(new Map())
  const [isUsingFallback, setIsUsingFallback] = useState(false)
  
  // Fallback to polling when WebSocket is disconnected
  useEffect(() => {
    if (connectionStatus === 'disconnected' || connectionStatus === 'error') {
      setIsUsingFallback(true)
      
      const pollInterval = setInterval(async () => {
        try {
          const data = await marketDataApi.getMarketData(symbols)
          setFallbackData(new Map(data.map(item => [item.symbol, item])))
        } catch (error) {
          console.error('Fallback API call failed:', error)
        }
      }, 5000) // Poll every 5 seconds
      
      return () => clearInterval(pollInterval)
    } else {
      setIsUsingFallback(false)
    }
  }, [connectionStatus, symbols])
  
  return {
    data: isUsingFallback ? fallbackData : wsData,
    connectionStatus,
    isUsingFallback
  }
}
```

## Acceptance Criteria

### Connection Management
- [ ] **Automatic Reconnection**: Exponential backoff reconnection with max 5 attempts
- [ ] **Connection Status**: Real-time connection status indicator for users
- [ ] **Message Queuing**: Queue messages during disconnection and send on reconnect
- [ ] **Heartbeat**: 30-second heartbeat to maintain connection

### Real-time Data Streaming
- [ ] **Market Data**: Real-time price updates with sub-100ms latency
- [ ] **Order Book**: Live order book updates with depth visualization
- [ ] **Trade Stream**: Real-time trade execution updates
- [ ] **Portfolio Updates**: Live portfolio and position updates

### Performance Optimization
- [ ] **Message Batching**: Batch updates in 50ms windows to reduce render frequency
- [ ] **Virtualization**: Virtual scrolling for large data sets (>100 symbols)
- [ ] **Memory Management**: Efficient memory usage with data cleanup
- [ ] **CPU Optimization**: <5% CPU usage during active streaming

### Error Handling
- [ ] **Graceful Degradation**: Fallback to REST API when WebSocket fails
- [ ] **Error Boundaries**: Isolate WebSocket errors from other components
- [ ] **Retry Logic**: Intelligent retry mechanisms for failed operations
- [ ] **User Feedback**: Clear error messages and recovery instructions

## Testing Strategy

### Unit Tests
- WebSocket service connection management
- Message handling and parsing logic
- Subscription and unsubscription flows
- Reconnection and error handling logic

### Integration Tests
- End-to-end WebSocket communication
- Real-time data accuracy validation
- Performance under high message volume
- Connection failure and recovery scenarios

### Performance Tests
- Latency measurement (target: <100ms)
- Memory usage monitoring during streaming
- CPU usage optimization validation
- Network bandwidth efficiency testing

### User Experience Tests
- Connection status indicator accuracy
- Real-time update visual feedback
- Error message clarity and helpfulness
- Mobile device performance validation

## Definition of Done
- [ ] WebSocket service with automatic reconnection implemented
- [ ] Real-time market data streaming functional across all devices
- [ ] Connection status indicators working throughout application
- [ ] Message batching and virtualization optimizations active
- [ ] Error boundaries and fallback mechanisms operational
- [ ] Performance testing passed (sub-100ms latency, <5% CPU)
- [ ] Cross-browser compatibility validated (Chrome, Firefox, Safari, Edge)
- [ ] Mobile responsiveness and performance optimized
- [ ] Comprehensive error handling and user feedback implemented
- [ ] Integration testing with backend WebSocket services completed

## Story Points: 13

## Dependencies
- Backend WebSocket server implementation
- Market data provider API integration
- Authentication token management system
- Error reporting and monitoring service

## Notes
- Consider implementing WebSocket protocol compression for bandwidth optimization
- Integration with service worker for offline queue management
- Support for multiple WebSocket connections for different data types
- Implementation of circuit breaker pattern for external service reliability