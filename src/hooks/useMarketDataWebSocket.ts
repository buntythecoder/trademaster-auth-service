import { useState, useEffect, useMemo, useCallback } from 'react'
import { useWebSocket } from './useWebSocketHooks'

// Market Data Interfaces
export interface MarketData {
  symbol: string
  price: number
  change: number
  changePercent: number
  volume: number
  high: number
  low: number
  open: number
  previousClose: number
  lastUpdated: Date
  marketStatus: 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_HOURS'
}

export interface OrderBookLevel {
  price: number
  quantity: number
  orders: number
}

export interface OrderBook {
  symbol: string
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
  lastUpdated: Date
  spread: number
  midPrice: number
}

export interface Trade {
  id: string
  symbol: string
  price: number
  quantity: number
  timestamp: Date
  side: 'BUY' | 'SELL'
  isOddLot: boolean
}

export interface OrderBookUpdate {
  symbol: string
  type: 'snapshot' | 'delta'
  bids?: OrderBookLevel[]
  asks?: OrderBookLevel[]
  timestamp: Date
}

// Market Data Hook
export const useMarketData = (symbols: string[]) => {
  const { subscribe, connectionStatus, isConnected } = useWebSocket()
  const [data, setData] = useState<Map<string, MarketData>>(new Map())
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [errors, setErrors] = useState<string[]>([])

  // Create stable symbols array to prevent unnecessary re-subscriptions
  const symbolsKey = useMemo(() => symbols.sort().join(','), [symbols])
  const stableSymbols = useMemo(() => symbols.sort(), [symbolsKey])

  useEffect(() => {
    if (!isConnected || stableSymbols.length === 0) return

    const unsubscribers: (() => void)[] = []
    setErrors([])

    // Subscribe to each symbol's market data
    stableSymbols.forEach(symbol => {
      const channel = `market.${symbol}`
      
      const unsubscribe = subscribe(channel, (marketData: MarketData) => {
        try {
          setData(prev => new Map(prev.set(symbol, {
            ...marketData,
            lastUpdated: new Date(marketData.lastUpdated)
          })))
          setLastUpdate(new Date())
        } catch (error) {
          console.error(`Error processing market data for ${symbol}:`, error)
          setErrors(prev => [...prev, `Failed to process data for ${symbol}`])
        }
      })
      
      unsubscribers.push(unsubscribe)
    })

    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe())
    }
  }, [stableSymbols, subscribe, isConnected])

  // Clear data when disconnected
  useEffect(() => {
    if (!isConnected) {
      setData(new Map())
      setLastUpdate(null)
    }
  }, [isConnected])

  const getSymbolData = useCallback((symbol: string) => {
    return data.get(symbol) || null
  }, [data])

  const isSymbolSubscribed = useCallback((symbol: string) => {
    return stableSymbols.includes(symbol)
  }, [stableSymbols])

  return {
    data,
    lastUpdate,
    connectionStatus,
    isConnected,
    errors,
    getSymbolData,
    isSymbolSubscribed,
    symbolCount: data.size
  }
}

// Order Book Hook
export const useOrderBook = (symbol: string) => {
  const { subscribe, connectionStatus, isConnected } = useWebSocket()
  const [orderBook, setOrderBook] = useState<OrderBook | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [error, setError] = useState<string | null>(null)

  const mergeOrderBookUpdate = useCallback((
    current: OrderBook | null, 
    update: OrderBookUpdate
  ): OrderBook => {
    if (!current || update.type === 'snapshot') {
      return {
        symbol: update.symbol,
        bids: update.bids || [],
        asks: update.asks || [],
        lastUpdated: new Date(update.timestamp),
        spread: 0,
        midPrice: 0
      }
    }

    // Merge delta updates
    const newBids = update.bids ? [...update.bids] : [...current.bids]
    const newAsks = update.asks ? [...update.asks] : [...current.asks]

    // Sort bids descending (highest price first)
    newBids.sort((a, b) => b.price - a.price)
    // Sort asks ascending (lowest price first)
    newAsks.sort((a, b) => a.price - b.price)

    const bestBid = newBids[0]?.price || 0
    const bestAsk = newAsks[0]?.price || 0
    const spread = bestAsk - bestBid
    const midPrice = (bestBid + bestAsk) / 2

    return {
      symbol: update.symbol,
      bids: newBids.slice(0, 20), // Keep top 20 levels
      asks: newAsks.slice(0, 20), // Keep top 20 levels
      lastUpdated: new Date(update.timestamp),
      spread,
      midPrice
    }
  }, [])

  useEffect(() => {
    if (!symbol || !isConnected) {
      setOrderBook(null)
      setLastUpdate(null)
      return
    }

    setError(null)
    const channel = `orderbook.${symbol}`
    
    const unsubscribe = subscribe(channel, (update: OrderBookUpdate) => {
      try {
        setOrderBook(prev => mergeOrderBookUpdate(prev, update))
        setLastUpdate(new Date())
      } catch (error) {
        console.error(`Error processing order book update for ${symbol}:`, error)
        setError(`Failed to process order book data for ${symbol}`)
      }
    })

    return unsubscribe
  }, [symbol, subscribe, isConnected, mergeOrderBookUpdate])

  const getBestBid = useCallback(() => {
    return orderBook?.bids[0] || null
  }, [orderBook])

  const getBestAsk = useCallback(() => {
    return orderBook?.asks[0] || null
  }, [orderBook])

  const getSpread = useCallback(() => {
    return orderBook?.spread || 0
  }, [orderBook])

  const getMidPrice = useCallback(() => {
    return orderBook?.midPrice || 0
  }, [orderBook])

  return {
    orderBook,
    lastUpdate,
    connectionStatus,
    isConnected,
    error,
    getBestBid,
    getBestAsk,
    getSpread,
    getMidPrice
  }
}

// Trade Stream Hook
export const useTradeStream = (symbol: string, maxTrades: number = 100) => {
  const { subscribe, connectionStatus, isConnected } = useWebSocket()
  const [trades, setTrades] = useState<Trade[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!symbol || !isConnected) {
      setTrades([])
      setLastUpdate(null)
      return
    }

    setError(null)
    const channel = `trades.${symbol}`
    
    const unsubscribe = subscribe(channel, (trade: Trade) => {
      try {
        setTrades(prev => {
          const newTrades = [{
            ...trade,
            timestamp: new Date(trade.timestamp)
          }, ...prev].slice(0, maxTrades)
          return newTrades
        })
        setLastUpdate(new Date())
      } catch (error) {
        console.error(`Error processing trade update for ${symbol}:`, error)
        setError(`Failed to process trade data for ${symbol}`)
      }
    })

    return unsubscribe
  }, [symbol, subscribe, isConnected, maxTrades])

  const getLatestTrade = useCallback(() => {
    return trades[0] || null
  }, [trades])

  const getTradingVolume = useCallback((timeWindow: number = 3600000) => { // 1 hour default
    const cutoff = new Date(Date.now() - timeWindow)
    return trades
      .filter(trade => trade.timestamp >= cutoff)
      .reduce((total, trade) => total + trade.quantity, 0)
  }, [trades])

  const getAveragePrice = useCallback((timeWindow: number = 3600000) => {
    const cutoff = new Date(Date.now() - timeWindow)
    const recentTrades = trades.filter(trade => trade.timestamp >= cutoff)
    
    if (recentTrades.length === 0) return 0
    
    const totalValue = recentTrades.reduce((sum, trade) => sum + (trade.price * trade.quantity), 0)
    const totalQuantity = recentTrades.reduce((sum, trade) => sum + trade.quantity, 0)
    
    return totalQuantity > 0 ? totalValue / totalQuantity : 0
  }, [trades])

  return {
    trades,
    lastUpdate,
    connectionStatus,
    isConnected,
    error,
    tradeCount: trades.length,
    getLatestTrade,
    getTradingVolume,
    getAveragePrice
  }
}

// Market Status Hook
export const useMarketStatus = () => {
  const { subscribe, connectionStatus, isConnected } = useWebSocket()
  const [marketStatus, setMarketStatus] = useState({
    isOpen: false,
    nextOpen: null as Date | null,
    nextClose: null as Date | null,
    timezone: 'Asia/Kolkata',
    tradingSession: 'CLOSED' as 'PRE_MARKET' | 'REGULAR' | 'POST_MARKET' | 'CLOSED'
  })
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)

  useEffect(() => {
    if (!isConnected) return

    const channel = 'market.status'
    
    const unsubscribe = subscribe(channel, (status: any) => {
      try {
        setMarketStatus({
          ...status,
          nextOpen: status.nextOpen ? new Date(status.nextOpen) : null,
          nextClose: status.nextClose ? new Date(status.nextClose) : null
        })
        setLastUpdate(new Date())
      } catch (error) {
        console.error('Error processing market status update:', error)
      }
    })

    return unsubscribe
  }, [subscribe, isConnected])

  return {
    marketStatus,
    lastUpdate,
    connectionStatus,
    isConnected
  }
}