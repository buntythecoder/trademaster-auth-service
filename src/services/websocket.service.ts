import { notificationService } from './notification.service'

// Browser-compatible EventEmitter
class EventEmitter {
  private events: { [key: string]: Function[] } = {}

  on(event: string, listener: Function) {
    if (!this.events[event]) {
      this.events[event] = []
    }
    this.events[event].push(listener)
  }

  off(event: string, listener: Function) {
    if (!this.events[event]) return
    this.events[event] = this.events[event].filter(l => l !== listener)
  }

  emit(event: string, ...args: any[]) {
    if (!this.events[event]) return
    this.events[event].forEach(listener => {
      try {
        listener(...args)
      } catch (error) {
        console.error('Error in event listener:', error)
      }
    })
  }

  removeAllListeners(event?: string) {
    if (event) {
      delete this.events[event]
    } else {
      this.events = {}
    }
  }
}

export interface MarketData {
  symbol: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  open: number
  timestamp: Date
  bid?: number
  ask?: number
  lastTradeTime?: Date
}

export interface PortfolioUpdate {
  totalValue: number
  dayPnL: number
  dayPnLPercent: number
  positions: {
    symbol: string
    quantity: number
    avgPrice: number
    currentPrice: number
    pnl: number
    pnlPercent: number
  }[]
  timestamp: Date
}

export interface OrderUpdate {
  orderId: string
  symbol: string
  side: 'BUY' | 'SELL'
  quantity: number
  price: number
  status: 'PENDING' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED'
  timestamp: Date
  message?: string
}

export interface PriceAlert {
  id: string
  symbol: string
  type: 'ABOVE' | 'BELOW' | 'PERCENT_CHANGE'
  targetPrice: number
  currentPrice: number
  triggered: boolean
  timestamp: Date
  message: string
}

export type WebSocketMessage = 
  | { type: 'MARKET_DATA'; data: MarketData }
  | { type: 'PORTFOLIO_UPDATE'; data: PortfolioUpdate }
  | { type: 'ORDER_UPDATE'; data: OrderUpdate }
  | { type: 'PRICE_ALERT'; data: PriceAlert }
  | { type: 'CONNECTION_STATUS'; data: { status: 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING'; timestamp: Date } }
  | { type: 'HEARTBEAT'; data: { timestamp: Date } }

export class WebSocketService extends EventEmitter {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectDelay = 1000
  private heartbeatInterval: NodeJS.Timeout | null = null
  private connectionTimeout: NodeJS.Timeout | null = null
  private isIntentionalClose = false
  private subscriptions = new Set<string>()
  
  constructor(url: string = 'ws://localhost:8080/ws') {
    super()
    this.url = url
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)
        
        this.ws.onopen = () => {
          console.log('WebSocket connected')
          this.reconnectAttempts = 0
          this.startHeartbeat()
          
          // Welcome notification
          notificationService.addSystemNotification('Connected to real-time trading data', 'low')
          
          this.emit('connection', { status: 'CONNECTED', timestamp: new Date() })
          
          // Re-subscribe to previous subscriptions
          this.subscriptions.forEach(subscription => {
            this.send({ type: 'SUBSCRIBE', symbol: subscription })
          })
          
          resolve()
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
          this.emit('connection', { status: 'DISCONNECTED', timestamp: new Date() })
          
          if (!this.isIntentionalClose && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnect()
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error)
          this.emit('error', error)
          reject(error)
        }

        // Connection timeout
        this.connectionTimeout = setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            this.ws?.close()
            reject(new Error('Connection timeout'))
          }
        }, 10000)

      } catch (error) {
        reject(error)
      }
    })
  }

  disconnect(): void {
    this.isIntentionalClose = true
    this.stopHeartbeat()
    this.clearConnectionTimeout()
    
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }
  }

  private reconnect(): void {
    if (this.isIntentionalClose) return
    
    this.reconnectAttempts++
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1)
    
    console.log(`Reconnecting... Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
    this.emit('connection', { status: 'RECONNECTING', timestamp: new Date() })
    
    setTimeout(() => {
      this.connect().catch(error => {
        console.error('Reconnection failed:', error)
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
          this.emit('error', new Error('Max reconnection attempts reached'))
        }
      })
    }, delay)
  }

  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'PING', timestamp: Date.now() })
      }
    }, 30000) // 30 seconds
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  private clearConnectionTimeout(): void {
    if (this.connectionTimeout) {
      clearTimeout(this.connectionTimeout)
      this.connectionTimeout = null
    }
  }

  private handleMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'MARKET_DATA':
        this.emit('marketData', message.data)
        break
      case 'PORTFOLIO_UPDATE':
        this.emit('portfolioUpdate', message.data)
        notificationService.onPortfolioUpdate(message.data)
        break
      case 'ORDER_UPDATE':
        this.emit('orderUpdate', message.data)
        notificationService.onOrderUpdate(message.data)
        break
      case 'PRICE_ALERT':
        this.emit('priceAlert', message.data)
        break
      case 'HEARTBEAT':
        // Handle heartbeat response
        break
      default:
        console.warn('Unknown message type:', message)
    }
  }

  private send(data: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  // Public methods for subscriptions
  subscribeToSymbol(symbol: string): void {
    this.subscriptions.add(symbol)
    this.send({ type: 'SUBSCRIBE', symbol })
  }

  unsubscribeFromSymbol(symbol: string): void {
    this.subscriptions.delete(symbol)
    this.send({ type: 'UNSUBSCRIBE', symbol })
  }

  subscribeToPortfolio(): void {
    this.send({ type: 'SUBSCRIBE_PORTFOLIO' })
  }

  subscribeToOrders(): void {
    this.send({ type: 'SUBSCRIBE_ORDERS' })
  }

  subscribeToAlerts(): void {
    this.send({ type: 'SUBSCRIBE_ALERTS' })
  }

  getConnectionStatus(): 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING' {
    if (!this.ws) return 'DISCONNECTED'
    
    switch (this.ws.readyState) {
      case WebSocket.OPEN:
        return 'CONNECTED'
      case WebSocket.CONNECTING:
        return 'RECONNECTING'
      default:
        return 'DISCONNECTED'
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  getSubscriptions(): string[] {
    return Array.from(this.subscriptions)
  }
}

// Singleton instance
export const webSocketService = new WebSocketService()

// Mock data generator for development/demo
export class MockDataGenerator {
  private static symbols = ['RELIANCE', 'TCS', 'INFY', 'HDFC', 'ICICIBANK', 'KOTAKBANK', 'SBIN', 'BHARTIARTL', 'ITC', 'LT']
  private static prices: Record<string, number> = {
    'RELIANCE': 2500.00,
    'TCS': 3200.00,
    'INFY': 1450.00,
    'HDFC': 2800.00,
    'ICICIBANK': 950.00,
    'KOTAKBANK': 1750.00,
    'SBIN': 620.00,
    'BHARTIARTL': 900.00,
    'ITC': 480.00,
    'LT': 3100.00
  }

  static generateMarketData(symbol: string): MarketData {
    const basePrice = this.prices[symbol] || 1000
    const changePercent = (Math.random() - 0.5) * 4 // -2% to +2%
    const change = (basePrice * changePercent) / 100
    const price = basePrice + change
    
    return {
      symbol,
      price: Number(price.toFixed(2)),
      change: Number(change.toFixed(2)),
      changePercent: Number(changePercent.toFixed(2)),
      volume: Math.floor(Math.random() * 1000000) + 10000,
      high: Number((price * 1.02).toFixed(2)),
      low: Number((price * 0.98).toFixed(2)),
      open: Number((basePrice * (1 + (Math.random() - 0.5) * 0.01)).toFixed(2)),
      bid: Number((price - 0.05).toFixed(2)),
      ask: Number((price + 0.05).toFixed(2)),
      timestamp: new Date(),
      lastTradeTime: new Date()
    }
  }

  static generatePortfolioUpdate(): PortfolioUpdate {
    const positions = this.symbols.slice(0, 5).map(symbol => {
      const marketData = this.generateMarketData(symbol)
      const quantity = Math.floor(Math.random() * 100) + 10
      const avgPrice = marketData.price * (0.95 + Math.random() * 0.1)
      const pnl = (marketData.price - avgPrice) * quantity
      const pnlPercent = ((marketData.price - avgPrice) / avgPrice) * 100
      
      return {
        symbol,
        quantity,
        avgPrice: Number(avgPrice.toFixed(2)),
        currentPrice: marketData.price,
        pnl: Number(pnl.toFixed(2)),
        pnlPercent: Number(pnlPercent.toFixed(2))
      }
    })

    const totalValue = positions.reduce((sum, pos) => sum + (pos.currentPrice * pos.quantity), 0)
    const dayPnL = positions.reduce((sum, pos) => sum + pos.pnl, 0)
    const dayPnLPercent = (dayPnL / (totalValue - dayPnL)) * 100

    return {
      totalValue: Number(totalValue.toFixed(2)),
      dayPnL: Number(dayPnL.toFixed(2)),
      dayPnLPercent: Number(dayPnLPercent.toFixed(2)),
      positions,
      timestamp: new Date()
    }
  }

  static startMockStream(wsService: WebSocketService): void {
    // Simulate market data updates every 1-3 seconds
    setInterval(() => {
      if (wsService.isConnected()) {
        const symbols = wsService.getSubscriptions()
        if (symbols.length > 0) {
          const randomSymbol = symbols[Math.floor(Math.random() * symbols.length)]
          const marketData = this.generateMarketData(randomSymbol)
          wsService.emit('marketData', marketData)
        }
      }
    }, 1000 + Math.random() * 2000)

    // Simulate portfolio updates every 10-30 seconds
    setInterval(() => {
      if (wsService.isConnected()) {
        const portfolioUpdate = this.generatePortfolioUpdate()
        wsService.emit('portfolioUpdate', portfolioUpdate)
      }
    }, 10000 + Math.random() * 20000)

    // Simulate occasional order updates
    setInterval(() => {
      if (wsService.isConnected() && Math.random() > 0.8) {
        const orderStatuses = ['filled', 'cancelled', 'partially_filled', 'pending']
        const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ITC']
        const sides = ['BUY', 'SELL']
        
        const orderUpdate = {
          orderId: `ORD_${Date.now()}`,
          symbol: symbols[Math.floor(Math.random() * symbols.length)],
          side: sides[Math.floor(Math.random() * sides.length)],
          quantity: Math.floor(Math.random() * 50) + 10,
          price: 1000 + Math.random() * 2000,
          status: orderStatuses[Math.floor(Math.random() * orderStatuses.length)],
          timestamp: new Date(),
          type: 'LIMIT'
        }
        
        wsService.emit('orderUpdate', orderUpdate)
        notificationService.onOrderUpdate(orderUpdate)
      }
    }, 20000 + Math.random() * 40000)

    // Simulate occasional price alerts
    setInterval(() => {
      if (wsService.isConnected() && Math.random() > 0.7) {
        const symbols = wsService.getSubscriptions()
        if (symbols.length > 0) {
          const randomSymbol = symbols[Math.floor(Math.random() * symbols.length)]
          const marketData = this.generateMarketData(randomSymbol)
          
          const alert: PriceAlert = {
            id: `alert_${Date.now()}`,
            symbol: randomSymbol,
            type: 'ABOVE',
            targetPrice: marketData.price - 10,
            currentPrice: marketData.price,
            triggered: true,
            timestamp: new Date(),
            message: `${randomSymbol} crossed above ₹${marketData.price - 10}`
          }
          
          wsService.emit('priceAlert', alert)
          
          // Trigger notification for price alert
          notificationService.addNotification({
            type: 'price_alert',
            title: 'Price Alert Triggered',
            message: `${alert.symbol} price alert: ₹${alert.price.toFixed(2)} (${alert.change >= 0 ? '+' : ''}${alert.changePercent.toFixed(2)}%)`,
            read: false,
            priority: Math.abs(alert.changePercent) > 3 ? 'high' : 'medium',
            category: 'Alerts',
            data: alert
          })
        }
      }
    }, 30000 + Math.random() * 60000)
  }
}