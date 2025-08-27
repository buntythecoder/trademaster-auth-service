import { useState, useEffect, useRef, useCallback } from 'react'

export interface RealTimeMarketData {
  symbol: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  open: number
  marketCap?: number
  bid?: number
  ask?: number
  lastTrade: Date
  isConnected: boolean
}

export interface MarketDataSubscription {
  symbol: string
  subscribed: Date
  dataPoints: number
  lastUpdate: Date
}

interface UseEnhancedMarketDataProps {
  symbols: string[]
  updateInterval?: number
  enableRealtimeUpdates?: boolean
}

interface UseEnhancedMarketDataReturn {
  marketData: Record<string, RealTimeMarketData>
  subscriptions: MarketDataSubscription[]
  isConnected: boolean
  connectionStatus: 'connecting' | 'connected' | 'disconnected' | 'error'
  lastUpdate: Date | null
  subscribe: (symbol: string) => void
  unsubscribe: (symbol: string) => void
  refreshData: () => void
  getSymbolData: (symbol: string) => RealTimeMarketData | null
}

export const useEnhancedMarketData = ({
  symbols,
  updateInterval = 2000,
  enableRealtimeUpdates = true
}: UseEnhancedMarketDataProps): UseEnhancedMarketDataReturn => {
  const [marketData, setMarketData] = useState<Record<string, RealTimeMarketData>>({})
  const [subscriptions, setSubscriptions] = useState<MarketDataSubscription[]>([])
  const [isConnected, setIsConnected] = useState(false)
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected' | 'error'>('disconnected')
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  
  const intervalRef = useRef<NodeJS.Timeout>()
  const wsRef = useRef<WebSocket | null>(null)
  const subscribedSymbolsRef = useRef<Set<string>>(new Set())

  // Mock market data generator for symbols
  const generateMarketData = useCallback((symbol: string): RealTimeMarketData => {
    const baseData = {
      'RELIANCE': { basePrice: 2456.75, sector: 'Oil & Gas', marketCap: 1664000 },
      'TCS': { basePrice: 3789.40, sector: 'IT', marketCap: 1379000 },
      'HDFCBANK': { basePrice: 1687.25, sector: 'Banking', marketCap: 932000 },
      'INFY': { basePrice: 1456.80, sector: 'IT', marketCap: 608000 },
      'ICICIBANK': { basePrice: 987.35, sector: 'Banking', marketCap: 691000 },
      'WIPRO': { basePrice: 445.60, sector: 'IT', marketCap: 243000 },
      'TATASTEEL': { basePrice: 134.25, sector: 'Steel', marketCap: 167000 },
      'BAJFINANCE': { basePrice: 6789.50, sector: 'Financial Services', marketCap: 419000 },
      'AAPL': { basePrice: 195.89, sector: 'Technology', marketCap: 3042000 },
      'MSFT': { basePrice: 378.85, sector: 'Technology', marketCap: 2814000 }
    }

    const base = baseData[symbol] || { basePrice: 1000 + Math.random() * 500, marketCap: 50000 }
    const volatility = 0.02 // 2% volatility
    const changePercent = (Math.random() - 0.5) * volatility * 2
    const price = base.basePrice * (1 + changePercent)
    const change = price - base.basePrice
    
    const open = base.basePrice * (0.98 + Math.random() * 0.04)
    const high = Math.max(open, price) * (1 + Math.random() * 0.01)
    const low = Math.min(open, price) * (1 - Math.random() * 0.01)
    const volume = Math.floor(Math.random() * 5000000) + 100000
    
    return {
      symbol,
      price,
      change,
      changePercent: changePercent * 100,
      volume,
      high,
      low,
      open,
      marketCap: base.marketCap,
      bid: price * (0.999 - Math.random() * 0.001),
      ask: price * (1.001 + Math.random() * 0.001),
      lastTrade: new Date(),
      isConnected: true
    }
  }, [])

  // Initialize WebSocket connection (mock)
  const initializeConnection = useCallback(() => {
    setConnectionStatus('connecting')
    
    // Simulate WebSocket connection
    setTimeout(() => {
      setIsConnected(true)
      setConnectionStatus('connected')
      console.log('Market data WebSocket connected (simulated)')
    }, 1000)
  }, [])

  // Subscribe to a symbol
  const subscribe = useCallback((symbol: string) => {
    if (subscribedSymbolsRef.current.has(symbol)) return
    
    subscribedSymbolsRef.current.add(symbol)
    
    const newSubscription: MarketDataSubscription = {
      symbol,
      subscribed: new Date(),
      dataPoints: 0,
      lastUpdate: new Date()
    }
    
    setSubscriptions(prev => [...prev.filter(s => s.symbol !== symbol), newSubscription])
    
    // Generate initial data
    const initialData = generateMarketData(symbol)
    setMarketData(prev => ({
      ...prev,
      [symbol]: initialData
    }))
    
    console.log(`Subscribed to ${symbol}`)
  }, [generateMarketData])

  // Unsubscribe from a symbol
  const unsubscribe = useCallback((symbol: string) => {
    subscribedSymbolsRef.current.delete(symbol)
    setSubscriptions(prev => prev.filter(s => s.symbol !== symbol))
    setMarketData(prev => {
      const { [symbol]: removed, ...rest } = prev
      return rest
    })
    console.log(`Unsubscribed from ${symbol}`)
  }, [])

  // Refresh all data
  const refreshData = useCallback(() => {
    const symbolsToRefresh = Array.from(subscribedSymbolsRef.current)
    const updatedData: Record<string, RealTimeMarketData> = {}
    
    symbolsToRefresh.forEach(symbol => {
      updatedData[symbol] = generateMarketData(symbol)
    })
    
    setMarketData(updatedData)
    setLastUpdate(new Date())
    
    // Update subscriptions data point counts
    setSubscriptions(prev => prev.map(sub => ({
      ...sub,
      dataPoints: sub.dataPoints + 1,
      lastUpdate: new Date()
    })))
  }, [generateMarketData])

  // Get specific symbol data
  const getSymbolData = useCallback((symbol: string): RealTimeMarketData | null => {
    return marketData[symbol] || null
  }, [marketData])

  // Initialize connection on mount
  useEffect(() => {
    initializeConnection()
    
    return () => {
      if (wsRef.current) {
        wsRef.current.close()
      }
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [initializeConnection])

  // Subscribe to initial symbols
  useEffect(() => {
    symbols.forEach(symbol => {
      subscribe(symbol)
    })
  }, [symbols, subscribe])

  // Set up real-time updates
  useEffect(() => {
    if (!enableRealtimeUpdates || !isConnected) return

    intervalRef.current = setInterval(() => {
      refreshData()
    }, updateInterval)

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [enableRealtimeUpdates, isConnected, updateInterval, refreshData])

  // Simulate connection status changes
  useEffect(() => {
    const statusInterval = setInterval(() => {
      // Randomly simulate brief disconnections (5% chance)
      if (Math.random() < 0.05 && connectionStatus === 'connected') {
        setConnectionStatus('connecting')
        setIsConnected(false)
        
        setTimeout(() => {
          setConnectionStatus('connected')
          setIsConnected(true)
        }, 1000 + Math.random() * 2000)
      }
    }, 10000) // Check every 10 seconds

    return () => clearInterval(statusInterval)
  }, [connectionStatus])

  return {
    marketData,
    subscriptions,
    isConnected,
    connectionStatus,
    lastUpdate,
    subscribe,
    unsubscribe,
    refreshData,
    getSymbolData
  }
}

export default useEnhancedMarketData