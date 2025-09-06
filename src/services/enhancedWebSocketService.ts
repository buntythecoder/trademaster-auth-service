import { BrowserEventEmitter } from '../utils/BrowserEventEmitter'

// Types for market data
export interface MarketDataUpdate {
  symbol: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  open: number
  bid: number
  ask: number
  marketCap: number
  timestamp: Date
}

export interface OrderBookLevel {
  price: number
  quantity: number
  orders: number
}

export interface OrderBookUpdate {
  symbol: string
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
  spread: number
  timestamp: Date
}

export interface CandlestickData {
  timestamp: Date
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface TechnicalIndicator {
  name: string
  value: number
  signal: 'BUY' | 'SELL' | 'HOLD'
  color: string
  timestamp: Date
}

interface StreamSubscription {
  symbol: string
  type: 'market_data' | 'order_book' | 'candlestick' | 'indicators'
  callback: (data: any) => void
  isActive: boolean
  subscribed: Date
  dataPoints: number
}

class EnhancedWebSocketService extends BrowserEventEmitter {
  private ws: WebSocket | null = null
  private connectionStatus: 'disconnected' | 'connecting' | 'connected' | 'error' = 'disconnected'
  private isConnecting = false
  private reconnectAttempts = 0
  private heartbeatInterval: NodeJS.Timeout | null = null
  private dataGenerationInterval: NodeJS.Timeout | null = null
  private subscriptions = new Map<string, StreamSubscription>()

  // Mock market data for simulation
  private baseData: { [symbol: string]: { basePrice: number; volatility: number; sector: string; marketCap?: number } } = {
    'RELIANCE': { basePrice: 2456.75, volatility: 0.02, sector: 'Energy' },
    'TCS': { basePrice: 3542.80, volatility: 0.018, sector: 'IT Services' },
    'HDFCBANK': { basePrice: 1698.45, volatility: 0.019, sector: 'Banking' },
    'INFY': { basePrice: 1456.30, volatility: 0.021, sector: 'IT Services' },
    'HINDUNILVR': { basePrice: 2378.90, volatility: 0.015, sector: 'FMCG' },
    'ITC': { basePrice: 456.25, volatility: 0.017, sector: 'FMCG' },
    'SBIN': { basePrice: 578.40, volatility: 0.024, sector: 'Banking' },
    'BHARTIARTL': { basePrice: 967.85, volatility: 0.020, sector: 'Telecom' },
    'ASIANPAINT': { basePrice: 3245.60, volatility: 0.016, sector: 'Paints' },
    'MARUTI': { basePrice: 10456.75, volatility: 0.019, sector: 'Automotive' },
    'TATAMOTORS': { basePrice: 789.25, volatility: 0.028, sector: 'Automotive' },
    'TATASTEEL': { basePrice: 134.25, volatility: 0.025, sector: 'Steel' },
    'BAJFINANCE': { basePrice: 6789.50, volatility: 0.022, sector: 'Financial Services' },
    'AAPL': { basePrice: 195.89, volatility: 0.014, sector: 'Technology' },
    'MSFT': { basePrice: 378.85, volatility: 0.013, sector: 'Technology' },
    'GOOGL': { basePrice: 2789.34, volatility: 0.016, sector: 'Technology' },
    'AMZN': { basePrice: 3378.23, volatility: 0.018, sector: 'E-commerce' }
  }

  private priceHistory = new Map<string, number[]>()
  private candlestickHistory = new Map<string, CandlestickData[]>()

  constructor(private wsUrl: string = 'wss://mock-market-data.trademaster.com') {
    super()
    this.setMaxListeners(100)
  }

  // Connect to WebSocket server (simulated)
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        resolve()
        return
      }

      if (this.isConnecting) {
        this.once('connected', resolve)
        this.once('error', reject)
        return
      }

      this.isConnecting = true
      this.connectionStatus = 'connecting'
      this.emit('status', this.connectionStatus)

      // Simulate WebSocket connection
      setTimeout(() => {
        try {
          this.connectionStatus = 'connected'
          this.isConnecting = false
          this.reconnectAttempts = 0
          
          // Start heartbeat and data generation
          this.startHeartbeat()
          this.startDataGeneration()
          
          this.emit('connected')
          this.emit('status', this.connectionStatus)
          
          console.log('Enhanced WebSocket connected (simulated)')
          resolve()
        } catch (error) {
          this.connectionStatus = 'error'
          this.isConnecting = false
          this.emit('error', error)
          this.emit('status', this.connectionStatus)
          reject(error)
        }
      }, 500 + Math.random() * 1000)
    })
  }

  // Disconnect from WebSocket
  disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }

    if (this.dataGenerationInterval) {
      clearInterval(this.dataGenerationInterval)
      this.dataGenerationInterval = null
    }

    this.connectionStatus = 'disconnected'
    this.emit('status', this.connectionStatus)
    this.emit('disconnected')
  }

  // Subscribe to market data stream
  subscribeMarketData(symbol: string, callback: (data: MarketDataUpdate) => void): string {
    const subscriptionId = `market_${symbol}_${Date.now()}`
    
    const subscription: StreamSubscription = {
      symbol,
      type: 'market_data',
      callback,
      isActive: true,
      subscribed: new Date(),
      dataPoints: 0
    }

    this.subscriptions.set(subscriptionId, subscription)
    
    if (!this.priceHistory.has(symbol)) {
      this.priceHistory.set(symbol, [])
    }

    console.log(`Subscribed to market data for ${symbol}`)
    this.emit('subscription', { type: 'market_data', symbol, subscriptionId })
    
    return subscriptionId
  }

  // Subscribe to order book stream
  subscribeOrderBook(symbol: string, callback: (data: OrderBookUpdate) => void): string {
    const subscriptionId = `orderbook_${symbol}_${Date.now()}`
    
    const subscription: StreamSubscription = {
      symbol,
      type: 'order_book',
      callback,
      isActive: true,
      subscribed: new Date(),
      dataPoints: 0
    }

    this.subscriptions.set(subscriptionId, subscription)
    console.log(`Subscribed to order book for ${symbol}`)
    
    return subscriptionId
  }

  // Subscribe to candlestick data stream
  subscribeCandlestick(symbol: string, timeframe: string, callback: (data: CandlestickData) => void): string {
    const subscriptionId = `candlestick_${symbol}_${timeframe}_${Date.now()}`
    
    const subscription: StreamSubscription = {
      symbol,
      type: 'candlestick',
      callback,
      isActive: true,
      subscribed: new Date(),
      dataPoints: 0
    }

    this.subscriptions.set(subscriptionId, subscription)
    
    if (!this.candlestickHistory.has(symbol)) {
      this.candlestickHistory.set(symbol, [])
    }

    console.log(`Subscribed to ${timeframe} candlesticks for ${symbol}`)
    
    return subscriptionId
  }

  // Subscribe to technical indicators
  subscribeIndicators(symbol: string, indicators: string[], callback: (data: TechnicalIndicator[]) => void): string {
    const subscriptionId = `indicators_${symbol}_${Date.now()}`
    
    const subscription: StreamSubscription = {
      symbol,
      type: 'indicators',
      callback,
      isActive: true,
      subscribed: new Date(),
      dataPoints: 0
    }

    this.subscriptions.set(subscriptionId, subscription)
    console.log(`Subscribed to indicators [${indicators.join(', ')}] for ${symbol}`)
    
    return subscriptionId
  }

  // Unsubscribe from a stream
  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId)
    if (subscription) {
      subscription.isActive = false
      this.subscriptions.delete(subscriptionId)
      console.log(`Unsubscribed from ${subscription.type} for ${subscription.symbol}`)
      this.emit('unsubscription', { subscriptionId, ...subscription })
    }
  }

  // Get connection status
  getConnectionStatus() {
    return {
      status: this.connectionStatus,
      isConnected: this.connectionStatus === 'connected',
      subscriptions: this.subscriptions.size,
      reconnectAttempts: this.reconnectAttempts
    }
  }

  // Get active subscriptions
  getActiveSubscriptions() {
    return Array.from(this.subscriptions.entries()).map(([id, sub]) => ({
      id,
      ...sub
    }))
  }

  // Generate realistic market data
  private generateMarketData(symbol: string): MarketDataUpdate {
    const base = this.baseData[symbol] || { 
      basePrice: 1000 + Math.random() * 500, 
      volatility: 0.02, 
      sector: 'Unknown' 
    }
    
    const priceHistory = this.priceHistory.get(symbol) || []
    const lastPrice = priceHistory.length > 0 ? priceHistory[priceHistory.length - 1] : base.basePrice
    
    // Random walk with mean reversion
    const randomWalk = (Math.random() - 0.5) * base.volatility * 2
    const meanReversion = (base.basePrice - lastPrice) * 0.001
    const priceChange = randomWalk + meanReversion
    
    const newPrice = Math.max(0.01, lastPrice * (1 + priceChange))
    const change = newPrice - base.basePrice
    const changePercent = (change / base.basePrice) * 100
    
    // Update price history (keep last 1000 points)
    priceHistory.push(newPrice)
    if (priceHistory.length > 1000) {
      priceHistory.shift()
    }
    this.priceHistory.set(symbol, priceHistory)
    
    // Calculate high/low from recent history
    const recentPrices = priceHistory.slice(-100)
    const high = Math.max(...recentPrices, newPrice)
    const low = Math.min(...recentPrices, newPrice)
    const open = priceHistory.length > 50 ? priceHistory[priceHistory.length - 50] : newPrice
    
    return {
      symbol,
      price: newPrice,
      change,
      changePercent,
      volume: Math.floor(Math.random() * 5000000) + 100000,
      high,
      low,
      open,
      bid: newPrice * (0.999 - Math.random() * 0.002),
      ask: newPrice * (1.001 + Math.random() * 0.002),
      marketCap: base.marketCap || Math.floor(newPrice * 1000000000),
      timestamp: new Date()
    }
  }

  // Start heartbeat to maintain connection
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.connectionStatus === 'connected') {
        // Simulate occasional connection issues
        if (Math.random() < 0.02) {
          this.simulateConnectionIssue()
        }
      }
    }, 30000)
  }

  // Start data generation for active subscriptions
  private startDataGeneration(): void {
    this.dataGenerationInterval = setInterval(() => {
      if (this.connectionStatus !== 'connected') return
      
      // Process all active subscriptions
      for (const [subscriptionId, subscription] of this.subscriptions) {
        if (!subscription.isActive) continue
        
        try {
          switch (subscription.type) {
            case 'market_data':
              const marketData = this.generateMarketData(subscription.symbol)
              subscription.callback(marketData)
              break
              
            case 'order_book':
              // Generate mock order book data
              const orderBook = this.generateOrderBook(subscription.symbol)
              subscription.callback(orderBook)
              break
              
            case 'candlestick':
              const candlestick = this.generateCandlestick(subscription.symbol)
              if (candlestick) {
                subscription.callback(candlestick)
              }
              break
              
            case 'indicators':
              const indicators = this.generateTechnicalIndicators(subscription.symbol)
              if (indicators.length > 0) {
                subscription.callback(indicators)
              }
              break
          }
          
          subscription.dataPoints++
        } catch (error) {
          console.error(`Error generating data for ${subscription.symbol}:`, error)
          this.emit('error', error)
        }
      }
    }, 1000)
  }

  // Generate order book data
  private generateOrderBook(symbol: string): OrderBookUpdate {
    const marketData = this.generateMarketData(symbol)
    const price = marketData.price
    
    const bids: OrderBookLevel[] = []
    const asks: OrderBookLevel[] = []
    
    // Generate 10 bid levels
    for (let i = 0; i < 10; i++) {
      const levelPrice = price * (1 - (i + 1) * 0.001)
      bids.push({
        price: levelPrice,
        quantity: Math.floor(Math.random() * 10000) + 100,
        orders: Math.floor(Math.random() * 50) + 1
      })
    }
    
    // Generate 10 ask levels
    for (let i = 0; i < 10; i++) {
      const levelPrice = price * (1 + (i + 1) * 0.001)
      asks.push({
        price: levelPrice,
        quantity: Math.floor(Math.random() * 10000) + 100,
        orders: Math.floor(Math.random() * 50) + 1
      })
    }
    
    const spread = asks[0].price - bids[0].price
    
    return {
      symbol,
      bids,
      asks,
      spread,
      timestamp: new Date()
    }
  }

  // Generate candlestick data
  private generateCandlestick(symbol: string): CandlestickData | null {
    const priceHistory = this.priceHistory.get(symbol) || []
    if (priceHistory.length < 2) {
      this.generateMarketData(symbol)
    }
    
    const recent = priceHistory.slice(-15)
    if (recent.length === 0) return null
    
    const open = recent[0]
    const close = recent[recent.length - 1]
    const high = Math.max(...recent)
    const low = Math.min(...recent)
    const volume = Math.floor(Math.random() * 500000) + 50000
    
    return {
      timestamp: new Date(),
      open,
      high,
      low,
      close,
      volume
    }
  }

  // Generate technical indicators
  private generateTechnicalIndicators(symbol: string): TechnicalIndicator[] {
    const priceHistory = this.priceHistory.get(symbol) || []
    if (priceHistory.length < 20) return []
    
    const recent = priceHistory.slice(-20)
    const currentPrice = recent[recent.length - 1]
    
    // Simple RSI calculation
    const gains = []
    const losses = []
    
    for (let i = 1; i < recent.length; i++) {
      const change = recent[i] - recent[i - 1]
      if (change > 0) {
        gains.push(change)
        losses.push(0)
      } else {
        gains.push(0)
        losses.push(Math.abs(change))
      }
    }
    
    const avgGain = gains.reduce((a, b) => a + b, 0) / gains.length
    const avgLoss = losses.reduce((a, b) => a + b, 0) / losses.length
    const rs = avgGain / (avgLoss || 0.01)
    const rsi = 100 - (100 / (1 + rs))
    
    // Simple Moving Average
    const sma20 = recent.reduce((a, b) => a + b, 0) / recent.length
    
    return [
      {
        name: 'RSI',
        value: rsi,
        signal: rsi > 70 ? 'SELL' : rsi < 30 ? 'BUY' : 'HOLD',
        color: rsi > 70 ? '#ef4444' : rsi < 30 ? '#10b981' : '#f59e0b',
        timestamp: new Date()
      },
      {
        name: 'SMA20',
        value: sma20,
        signal: currentPrice > sma20 ? 'BUY' : 'SELL',
        color: currentPrice > sma20 ? '#10b981' : '#ef4444',
        timestamp: new Date()
      }
    ]
  }

  // Simulate connection issues for testing
  private simulateConnectionIssue(): void {
    console.log('Simulating connection issue...')
    this.connectionStatus = 'connecting'
    this.emit('status', this.connectionStatus)
    
    setTimeout(() => {
      this.connectionStatus = 'connected'
      this.emit('status', this.connectionStatus)
      console.log('Connection restored')
    }, 2000 + Math.random() * 3000)
  }

  // Cleanup on destroy
  destroy(): void {
    this.disconnect()
    this.subscriptions.clear()
    this.priceHistory.clear()
    this.candlestickHistory.clear()
    this.removeAllListeners()
  }
}

// Singleton instance
export const enhancedWebSocketService = new EnhancedWebSocketService()
export default enhancedWebSocketService