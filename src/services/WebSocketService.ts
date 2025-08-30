import { EventEmitter } from 'events'

// TypeScript interfaces for WebSocket communication
export interface WebSocketConfig {
  url: string
  reconnectDelay: number
  maxReconnectAttempts: number
  heartbeatInterval: number
}

export interface WebSocketMessage {
  type: 'data' | 'heartbeat' | 'error' | 'subscription_confirmed' | 'pong'
  channel?: string
  data?: any
  error?: string
  timestamp?: number
}

export interface OutgoingMessage {
  type: 'subscribe' | 'unsubscribe' | 'ping'
  channel?: string
  data?: any
}

export interface QueuedMessage {
  message: OutgoingMessage
  timestamp: number
  retries: number
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting'
export type SubscriptionCallback = (data: any) => void

class WebSocketService extends EventEmitter {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private reconnectDelay: number
  private maxReconnectAttempts: number
  private heartbeatInterval: number
  private subscribers = new Map<string, Set<SubscriptionCallback>>()
  private heartbeatTimer: NodeJS.Timeout | null = null
  private messageQueue: QueuedMessage[] = []
  private isConnecting = false
  private connectionStatus: ConnectionStatus = 'disconnected'
  private config: WebSocketConfig
  private mockMode = true // Use mock mode by default in development

  constructor(config: WebSocketConfig) {
    super()
    this.config = config
    this.reconnectDelay = config.reconnectDelay
    this.maxReconnectAttempts = config.maxReconnectAttempts
    this.heartbeatInterval = config.heartbeatInterval
    
    // Auto-connect (or simulate connection in mock mode)
    this.connect()
  }

  private async getAuthToken(): Promise<string> {
    // Get auth token from localStorage or auth store
    const token = localStorage.getItem('authToken')
    if (!token) {
      throw new Error('No authentication token available')
    }
    return token
  }

  public getConnectionStatus(): ConnectionStatus {
    return this.connectionStatus
  }

  private setConnectionStatus(status: ConnectionStatus) {
    if (this.connectionStatus !== status) {
      this.connectionStatus = status
      this.emit('connectionStatusChanged', status)
      
      // Emit specific events
      if (status === 'connected') {
        this.emit('connected')
      } else if (status === 'disconnected') {
        this.emit('disconnected')
      } else if (status === 'error') {
        this.emit('error', new Error('Connection error'))
      }
    }
  }

  private async connect(): Promise<void> {
    if (this.mockMode) {
      console.log('WebSocket connected (mock mode)')
      this.setConnectionStatus('connected')
      this.simulateMockData()
      return
    }
    
    if (this.isConnecting || this.ws?.readyState === WebSocket.OPEN) {
      return
    }

    this.isConnecting = true
    this.setConnectionStatus('connecting')

    try {
      const token = await this.getAuthToken()
      const wsUrl = `${this.config.url}?token=${encodeURIComponent(token)}`
      
      this.ws = new WebSocket(wsUrl)
      this.setupEventHandlers()
      
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      this.setConnectionStatus('error')
      this.handleReconnect()
    } finally {
      this.isConnecting = false
    }
  }

  private simulateMockData(): void {
    // Simulate periodic mock data updates for development
    setTimeout(() => {
      this.subscribers.forEach((callbacks, channel) => {
        callbacks.forEach(callback => {
          // Generate mock data based on channel
          let mockData;
          if (channel.includes('portfolio')) {
            mockData = {
              type: 'FULL',
              summary: {
                userId: 'demo-user',
                totalValue: 125000 + Math.random() * 10000,
                totalInvested: 100000,
                dayPnL: (Math.random() - 0.5) * 5000,
                dayPnLPercent: (Math.random() - 0.5) * 5,
                totalPnL: 25000 + Math.random() * 10000,
                totalPnLPercent: 25 + Math.random() * 5,
                realizedPnL: 5000,
                unrealizedPnL: 20000,
                availableCash: 50000,
                marginUsed: 30000,
                marginAvailable: 45000,
                lastUpdated: new Date(),
                positionCount: 5,
                sectors: { 'Technology': 40, 'Healthcare': 30, 'Finance': 30 },
                assetTypes: { 'EQUITY': 80, 'ETF': 20 }
              },
              positions: [
                {
                  id: '1',
                  symbol: 'RELIANCE',
                  companyName: 'Reliance Industries Ltd',
                  quantity: 10,
                  avgPrice: 2500,
                  currentPrice: 2547 + Math.random() * 100,
                  marketValue: 25470,
                  dayChange: Math.random() * 100 - 50,
                  dayChangePercent: (Math.random() - 0.5) * 5,
                  totalReturn: 470,
                  totalReturnPercent: 1.88,
                  allocation: 20,
                  sector: 'Energy',
                  assetType: 'EQUITY',
                  lastUpdated: new Date(),
                  unrealizedPnL: 470,
                  realizedPnL: 0,
                  pnlPercent: 1.88
                }
              ]
            }
          } else if (channel.includes('trades')) {
            mockData = {
              type: 'NEW',
              order: {
                id: `order_${Date.now()}`,
                symbol: 'RELIANCE',
                side: Math.random() > 0.5 ? 'BUY' : 'SELL',
                quantity: 10,
                price: 2547,
                type: 'MARKET',
                status: 'EXECUTED',
                timestamp: new Date(),
                executedQuantity: 10,
                executedPrice: 2547
              }
            }
          }
          
          if (mockData) {
            callback(mockData)
          }
        })
      })
    }, 1000) // Initial data
    
    // Set up periodic updates
    setInterval(() => {
      this.subscribers.forEach((callbacks, channel) => {
        if (channel.includes('portfolio')) {
          callbacks.forEach(callback => {
            callback({
              type: 'PRICE_UPDATE',
              priceUpdates: {
                'RELIANCE': 2547 + (Math.random() - 0.5) * 20,
                'TCS': 3456 + (Math.random() - 0.5) * 30,
                'HDFC': 1678 + (Math.random() - 0.5) * 15
              }
            })
          })
        }
      })
    }, 5000) // Update every 5 seconds
  }

  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('WebSocket connected successfully')
      this.reconnectAttempts = 0
      this.isConnecting = false
      this.setConnectionStatus('connected')
      
      // Process queued messages
      this.processMessageQueue()
      
      // Start heartbeat
      this.startHeartbeat()
      
      // Resubscribe to all channels
      this.resubscribeAll()
    }

    this.ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        this.handleMessage(message)
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error, event.data)
        this.emit('parseError', error)
      }
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket disconnected:', event.code, event.reason)
      this.stopHeartbeat()
      this.setConnectionStatus('disconnected')
      
      if (!event.wasClean && event.code !== 1000) {
        this.handleReconnect()
      }
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error)
      this.setConnectionStatus('error')
    }
  }

  private handleMessage(message: WebSocketMessage): void {
    const { type, channel, data, error } = message

    switch (type) {
      case 'data':
        if (channel) {
          this.notifySubscribers(channel, data)
        }
        break

      case 'heartbeat':
        // Heartbeat received, connection is alive
        this.send({ type: 'ping' })
        break

      case 'pong':
        // Pong received in response to ping
        break

      case 'error':
        console.error('WebSocket server error:', error)
        this.emit('serverError', error)
        break

      case 'subscription_confirmed':
        console.log('Subscription confirmed for channel:', channel)
        this.emit('subscriptionConfirmed', channel)
        break

      default:
        console.warn('Unknown message type:', type, message)
    }
  }

  private notifySubscribers(channel: string, data: any): void {
    const channelSubscribers = this.subscribers.get(channel)
    if (channelSubscribers) {
      channelSubscribers.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`Subscriber callback error for channel ${channel}:`, error)
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

    // Send subscription message if connected
    if (this.connectionStatus === 'connected') {
      this.send({
        type: 'subscribe',
        channel: channel
      })
    }

    // Return unsubscribe function
    return () => {
      const channelSubscribers = this.subscribers.get(channel)
      if (channelSubscribers) {
        channelSubscribers.delete(callback)
        
        // If no more subscribers, unsubscribe from channel
        if (channelSubscribers.size === 0) {
          this.subscribers.delete(channel)
          if (this.connectionStatus === 'connected') {
            this.send({
              type: 'unsubscribe',
              channel: channel
            })
          }
        }
      }
    }
  }

  private send(message: OutgoingMessage): void {
    if (this.mockMode) {
      console.log('WebSocket message sent (mock mode):', message)
      return
    }
    
    if (this.ws?.readyState === WebSocket.OPEN) {
      try {
        this.ws.send(JSON.stringify(message))
      } catch (error) {
        console.error('Failed to send WebSocket message:', error)
        this.queueMessage(message)
      }
    } else {
      this.queueMessage(message)
    }
  }

  private queueMessage(message: OutgoingMessage): void {
    // Don't queue heartbeat messages
    if (message.type === 'ping') return

    this.messageQueue.push({
      message,
      timestamp: Date.now(),
      retries: 0
    })

    // Limit queue size
    if (this.messageQueue.length > 1000) {
      this.messageQueue = this.messageQueue.slice(-500) // Keep only the 500 most recent
    }
  }

  private processMessageQueue(): void {
    const now = Date.now()
    const validMessages = this.messageQueue.filter(
      item => (now - item.timestamp) < 30000 // Only process messages less than 30 seconds old
    )

    validMessages.forEach(({ message }) => {
      this.send(message)
    })

    this.messageQueue = []
  }

  private resubscribeAll(): void {
    this.subscribers.forEach((subscribers, channel) => {
      if (subscribers.size > 0) {
        this.send({
          type: 'subscribe',
          channel: channel
        })
      }
    })
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`Max reconnection attempts (${this.maxReconnectAttempts}) reached`)
      this.setConnectionStatus('error')
      this.emit('maxReconnectAttemptsReached')
      return
    }

    this.setConnectionStatus('reconnecting')
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      30000 // Max 30 second delay
    )
    this.reconnectAttempts++

    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    setTimeout(() => {
      this.connect()
    }, delay)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping' })
      }
    }, this.heartbeatInterval)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  public reconnect(): void {
    this.disconnect()
    setTimeout(() => {
      this.reconnectAttempts = 0
      this.connect()
    }, 1000)
  }

  public disconnect(): void {
    if (this.mockMode) {
      console.log('WebSocket disconnected (mock mode)')
      this.setConnectionStatus('disconnected')
      return
    }
    
    this.stopHeartbeat()
    
    if (this.ws) {
      // Clean close
      this.ws.close(1000, 'Client disconnect')
      this.ws = null
    }
    
    this.setConnectionStatus('disconnected')
    this.messageQueue = []
  }

  public getSubscriptionCount(): number {
    let count = 0
    this.subscribers.forEach(subscribers => {
      count += subscribers.size
    })
    return count
  }

  public getChannels(): string[] {
    return Array.from(this.subscribers.keys())
  }

  public isHealthy(): boolean {
    return this.connectionStatus === 'connected' && 
           this.ws?.readyState === WebSocket.OPEN
  }
}

// Singleton instance
let wsServiceInstance: WebSocketService | null = null

export const createWebSocketService = (config?: Partial<WebSocketConfig>): WebSocketService => {
  if (wsServiceInstance) {
    return wsServiceInstance
  }

  const defaultConfig: WebSocketConfig = {
    url: import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws',
    reconnectDelay: 1000,
    maxReconnectAttempts: 5,
    heartbeatInterval: 30000
  }

  wsServiceInstance = new WebSocketService({ ...defaultConfig, ...config })
  return wsServiceInstance
}

export const getWebSocketService = (): WebSocketService => {
  if (!wsServiceInstance) {
    return createWebSocketService()
  }
  return wsServiceInstance
}

export const disconnectWebSocketService = (): void => {
  if (wsServiceInstance) {
    wsServiceInstance.disconnect()
    wsServiceInstance = null
  }
}

export default WebSocketService