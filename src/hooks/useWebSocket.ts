import { useState, useEffect, useCallback, useRef } from 'react'
import { 
  WebSocketService, 
  MarketData, 
  PortfolioUpdate, 
  OrderUpdate, 
  PriceAlert,
  webSocketService,
  MockDataGenerator
} from '@/services/websocket.service'

export interface WebSocketState {
  connectionStatus: 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING'
  lastUpdate: Date | null
  isLoading: boolean
  error: string | null
}

export interface UseWebSocketOptions {
  autoConnect?: boolean
  enableMockData?: boolean
  reconnectAttempts?: number
}

export function useWebSocket(options: UseWebSocketOptions = {}) {
  const {
    autoConnect = true,
    enableMockData = true,
    reconnectAttempts = 5
  } = options

  const [state, setState] = useState<WebSocketState>({
    connectionStatus: 'DISCONNECTED',
    lastUpdate: null,
    isLoading: false,
    error: null
  })

  const wsRef = useRef<WebSocketService>(webSocketService)
  const mockDataStarted = useRef(false)

  // Connection management
  const connect = useCallback(async () => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }))
      
      if (enableMockData) {
        // Simulate connection for demo
        setTimeout(() => {
          setState(prev => ({
            ...prev,
            connectionStatus: 'CONNECTED',
            isLoading: false,
            lastUpdate: new Date()
          }))
          
          // Start mock data stream
          if (!mockDataStarted.current) {
            MockDataGenerator.startMockStream(wsRef.current)
            mockDataStarted.current = true
          }
        }, 1000)
      } else {
        await wsRef.current.connect()
      }
    } catch (error) {
      setState(prev => ({
        ...prev,
        connectionStatus: 'DISCONNECTED',
        isLoading: false,
        error: error instanceof Error ? error.message : 'Connection failed'
      }))
    }
  }, [enableMockData])

  const disconnect = useCallback(() => {
    wsRef.current.disconnect()
    setState(prev => ({
      ...prev,
      connectionStatus: 'DISCONNECTED',
      isLoading: false
    }))
  }, [])

  // Subscription management
  const subscribeToSymbol = useCallback((symbol: string) => {
    wsRef.current.subscribeToSymbol(symbol)
  }, [])

  const unsubscribeFromSymbol = useCallback((symbol: string) => {
    wsRef.current.unsubscribeFromSymbol(symbol)
  }, [])

  const subscribeToPortfolio = useCallback(() => {
    wsRef.current.subscribeToPortfolio()
  }, [])

  const subscribeToOrders = useCallback(() => {
    wsRef.current.subscribeToOrders()
  }, [])

  const subscribeToAlerts = useCallback(() => {
    wsRef.current.subscribeToAlerts()
  }, [])

  // Event listeners setup
  useEffect(() => {
    const ws = wsRef.current

    const handleConnection = (data: { status: string; timestamp: Date }) => {
      setState(prev => ({
        ...prev,
        connectionStatus: data.status as any,
        lastUpdate: data.timestamp,
        isLoading: false
      }))
    }

    const handleError = (error: any) => {
      setState(prev => ({
        ...prev,
        error: error.message || 'WebSocket error',
        isLoading: false
      }))
    }

    ws.on('connection', handleConnection)
    ws.on('error', handleError)

    return () => {
      ws.off('connection', handleConnection)
      ws.off('error', handleError)
    }
  }, [])

  // Auto-connect
  useEffect(() => {
    if (autoConnect) {
      connect()
    }

    return () => {
      if (autoConnect) {
        disconnect()
      }
    }
  }, [autoConnect, connect, disconnect])

  return {
    ...state,
    connect,
    disconnect,
    subscribeToSymbol,
    unsubscribeFromSymbol,
    subscribeToPortfolio,
    subscribeToOrders,
    subscribeToAlerts,
    service: wsRef.current
  }
}

export function useMarketData(symbols: string[] = []) {
  const [marketData, setMarketData] = useState<Record<string, MarketData>>({})
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const { subscribeToSymbol, unsubscribeFromSymbol, service } = useWebSocket()

  // Handle market data updates
  useEffect(() => {
    const handleMarketData = (data: MarketData) => {
      setMarketData(prev => ({
        ...prev,
        [data.symbol]: data
      }))
      setLastUpdate(new Date())
    }

    service.on('marketData', handleMarketData)

    return () => {
      service.off('marketData', handleMarketData)
    }
  }, [service])

  // Subscribe/unsubscribe to symbols
  useEffect(() => {
    symbols.forEach(symbol => {
      subscribeToSymbol(symbol)
    })

    return () => {
      symbols.forEach(symbol => {
        unsubscribeFromSymbol(symbol)
      })
    }
  }, [symbols, subscribeToSymbol, unsubscribeFromSymbol])

  const getPrice = useCallback((symbol: string): number | null => {
    return marketData[symbol]?.price || null
  }, [marketData])

  const getChange = useCallback((symbol: string): { change: number; changePercent: number } | null => {
    const data = marketData[symbol]
    return data ? { change: data.change, changePercent: data.changePercent } : null
  }, [marketData])

  return {
    marketData,
    lastUpdate,
    getPrice,
    getChange,
    symbols: Object.keys(marketData)
  }
}

export function usePortfolio() {
  const [portfolio, setPortfolio] = useState<PortfolioUpdate | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const { subscribeToPortfolio, service } = useWebSocket()

  useEffect(() => {
    const handlePortfolioUpdate = (data: PortfolioUpdate) => {
      setPortfolio(data)
      setLastUpdate(data.timestamp)
    }

    service.on('portfolioUpdate', handlePortfolioUpdate)
    subscribeToPortfolio()

    return () => {
      service.off('portfolioUpdate', handlePortfolioUpdate)
    }
  }, [service, subscribeToPortfolio])

  return {
    portfolio,
    lastUpdate,
    totalValue: portfolio?.totalValue || 0,
    dayPnL: portfolio?.dayPnL || 0,
    dayPnLPercent: portfolio?.dayPnLPercent || 0,
    positions: portfolio?.positions || []
  }
}

export function useOrders() {
  const [orders, setOrders] = useState<OrderUpdate[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const { subscribeToOrders, service } = useWebSocket()

  useEffect(() => {
    const handleOrderUpdate = (data: OrderUpdate) => {
      setOrders(prev => {
        const existingIndex = prev.findIndex(order => order.orderId === data.orderId)
        if (existingIndex >= 0) {
          const updated = [...prev]
          updated[existingIndex] = data
          return updated
        } else {
          return [data, ...prev]
        }
      })
      setLastUpdate(data.timestamp)
    }

    service.on('orderUpdate', handleOrderUpdate)
    subscribeToOrders()

    return () => {
      service.off('orderUpdate', handleOrderUpdate)
    }
  }, [service, subscribeToOrders])

  return {
    orders,
    lastUpdate,
    pendingOrders: orders.filter(o => o.status === 'PENDING'),
    filledOrders: orders.filter(o => o.status === 'FILLED'),
    cancelledOrders: orders.filter(o => o.status === 'CANCELLED')
  }
}

export function usePriceAlerts() {
  const [alerts, setAlerts] = useState<PriceAlert[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const { subscribeToAlerts, service } = useWebSocket()

  useEffect(() => {
    const handlePriceAlert = (data: PriceAlert) => {
      setAlerts(prev => [data, ...prev.slice(0, 49)]) // Keep last 50 alerts
      setLastUpdate(data.timestamp)
    }

    service.on('priceAlert', handlePriceAlert)
    subscribeToAlerts()

    return () => {
      service.off('priceAlert', handlePriceAlert)
    }
  }, [service, subscribeToAlerts])

  return {
    alerts,
    lastUpdate,
    activeAlerts: alerts.filter(a => a.triggered),
    recentAlerts: alerts.slice(0, 10)
  }
}

// Connection status indicator hook
export function useConnectionStatus() {
  const { connectionStatus, lastUpdate, error } = useWebSocket()

  const getStatusColor = useCallback(() => {
    switch (connectionStatus) {
      case 'CONNECTED': return 'text-green-400'
      case 'RECONNECTING': return 'text-yellow-400'
      case 'DISCONNECTED': return 'text-red-400'
      default: return 'text-gray-400'
    }
  }, [connectionStatus])

  const getStatusIcon = useCallback(() => {
    switch (connectionStatus) {
      case 'CONNECTED': return '🟢'
      case 'RECONNECTING': return '🟡'
      case 'DISCONNECTED': return '🔴'
      default: return '⚪'
    }
  }, [connectionStatus])

  return {
    connectionStatus,
    lastUpdate,
    error,
    getStatusColor,
    getStatusIcon,
    isConnected: connectionStatus === 'CONNECTED',
    isReconnecting: connectionStatus === 'RECONNECTING'
  }
}