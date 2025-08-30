import { useState, useEffect, useCallback, useMemo } from 'react'
import { useWebSocket } from './useWebSocketHooks'

// Enhanced Portfolio Interfaces (extending the existing ones)
export interface Position {
  id: string
  symbol: string
  companyName: string
  quantity: number
  avgPrice: number
  currentPrice: number
  marketValue: number
  dayChange: number
  dayChangePercent: number
  totalReturn: number
  totalReturnPercent: number
  allocation: number
  sector: string
  assetType: 'EQUITY' | 'ETF' | 'MUTUAL_FUND' | 'BOND'
  lastUpdated: Date
  unrealizedPnL: number
  realizedPnL: number
  pnlPercent: number
}

export interface PortfolioSummary {
  userId: string
  totalValue: number
  totalInvested: number
  dayPnL: number
  dayPnLPercent: number
  totalPnL: number
  totalPnLPercent: number
  realizedPnL: number
  unrealizedPnL: number
  availableCash: number
  marginUsed: number
  marginAvailable: number
  lastUpdated: Date
  positionCount: number
  sectors: Record<string, number>
  assetTypes: Record<string, number>
}

export interface PortfolioUpdate {
  type: 'FULL' | 'POSITION' | 'SUMMARY' | 'PRICE_UPDATE'
  summary?: PortfolioSummary
  positions?: Position[]
  position?: Position
  priceUpdates?: { [symbol: string]: number }
  timestamp: Date
}

export interface TradingOrder {
  id: string
  symbol: string
  side: 'BUY' | 'SELL'
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'STOP_LIMIT'
  quantity: number
  price?: number
  stopPrice?: number
  status: 'PENDING' | 'PLACED' | 'FILLED' | 'PARTIAL' | 'CANCELLED' | 'REJECTED'
  filledQuantity: number
  remainingQuantity: number
  avgFillPrice?: number
  timestamp: Date
  lastUpdated: Date
}

export interface OrderUpdate {
  order: TradingOrder
  type: 'NEW' | 'UPDATED' | 'FILLED' | 'CANCELLED'
  timestamp: Date
}

// Enhanced Portfolio Hook
export const useEnhancedPortfolioWebSocket = (userId: string) => {
  const webSocketData = useWebSocket()
  const { 
    subscribe, 
    connectionStatus = 'disconnected', 
    isConnected = false 
  } = webSocketData || {}
  
  // Portfolio State
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null)
  const [positions, setPositions] = useState<Position[]>([])
  const [orders, setOrders] = useState<TradingOrder[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [errors, setErrors] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)

  // Performance tracking
  const [updateCount, setUpdateCount] = useState(0)
  const [latency, setLatency] = useState<number>(0)

  // Process portfolio updates
  const processPortfolioUpdate = useCallback((update: PortfolioUpdate) => {
    const updateTime = new Date()
    
    try {
      switch (update.type) {
        case 'FULL':
          if (update.summary) {
            setPortfolio({
              ...update.summary,
              lastUpdated: new Date(update.summary.lastUpdated)
            })
          }
          if (update.positions) {
            setPositions(update.positions.map(pos => ({
              ...pos,
              lastUpdated: new Date(pos.lastUpdated)
            })))
          }
          break

        case 'SUMMARY':
          if (update.summary) {
            setPortfolio({
              ...update.summary,
              lastUpdated: new Date(update.summary.lastUpdated)
            })
          }
          break

        case 'POSITION':
          if (update.position) {
            const updatedPosition = {
              ...update.position,
              lastUpdated: new Date(update.position.lastUpdated)
            }
            
            setPositions(prev => {
              const existing = prev.find(p => p.symbol === updatedPosition.symbol)
              if (existing) {
                return prev.map(p => p.symbol === updatedPosition.symbol ? updatedPosition : p)
              } else {
                return [...prev, updatedPosition]
              }
            })
          }
          break

        case 'PRICE_UPDATE':
          if (update.priceUpdates) {
            setPositions(prev => prev.map(position => {
              const newPrice = update.priceUpdates![position.symbol]
              if (newPrice && newPrice !== position.currentPrice) {
                const marketValue = newPrice * position.quantity
                const totalReturn = marketValue - (position.avgPrice * position.quantity)
                const totalReturnPercent = (totalReturn / (position.avgPrice * position.quantity)) * 100
                const dayChange = (newPrice - position.currentPrice) * position.quantity
                const dayChangePercent = ((newPrice - position.currentPrice) / position.currentPrice) * 100

                return {
                  ...position,
                  currentPrice: newPrice,
                  marketValue,
                  totalReturn,
                  totalReturnPercent,
                  dayChange,
                  dayChangePercent,
                  unrealizedPnL: totalReturn,
                  pnlPercent: totalReturnPercent,
                  lastUpdated: updateTime
                }
              }
              return position
            }))

            // Update portfolio summary with new totals
            setPortfolio(prev => {
              if (!prev) return prev
              
              const totalValue = positions.reduce((sum, pos) => {
                const price = update.priceUpdates![pos.symbol] || pos.currentPrice
                return sum + (price * pos.quantity)
              }, 0)
              
              const totalPnL = totalValue - prev.totalInvested
              const totalPnLPercent = (totalPnL / prev.totalInvested) * 100

              return {
                ...prev,
                totalValue,
                totalPnL,
                totalPnLPercent,
                unrealizedPnL: totalPnL,
                lastUpdated: updateTime
              }
            })
          }
          break
      }

      setLastUpdate(updateTime)
      setUpdateCount(prev => prev + 1)
      setIsLoading(false)
      
      // Calculate latency
      if (update.timestamp) {
        const serverTime = new Date(update.timestamp)
        setLatency(updateTime.getTime() - serverTime.getTime())
      }

    } catch (error) {
      console.error('Error processing portfolio update:', error)
      setErrors(prev => [...prev, `Failed to process ${update.type} update`])
    }
  }, [positions])

  // Process order updates
  const processOrderUpdate = useCallback((update: OrderUpdate) => {
    try {
      const updatedOrder = {
        ...update.order,
        timestamp: new Date(update.order.timestamp),
        lastUpdated: new Date(update.order.lastUpdated)
      }

      setOrders(prev => {
        const existing = prev.find(o => o.id === updatedOrder.id)
        if (existing) {
          return prev.map(o => o.id === updatedOrder.id ? updatedOrder : o)
        } else if (update.type === 'NEW') {
          return [updatedOrder, ...prev]
        }
        return prev
      })

      setLastUpdate(new Date())
    } catch (error) {
      console.error('Error processing order update:', error)
      setErrors(prev => [...prev, `Failed to process order update for ${update.order.id}`])
    }
  }, [])

  // Subscribe to portfolio updates
  useEffect(() => {
    if (!userId || !isConnected || !subscribe) {
      setPortfolio(null)
      setPositions([])
      setOrders([])
      setIsLoading(true)
      return
    }

    setErrors([])
    const unsubscribers: (() => void)[] = []

    // Portfolio updates
    const portfolioChannel = `portfolio.${userId}`
    const portfolioUnsubscribe = subscribe(portfolioChannel, processPortfolioUpdate)
    unsubscribers.push(portfolioUnsubscribe)

    // Order updates
    const ordersChannel = `orders.${userId}`
    const ordersUnsubscribe = subscribe(ordersChannel, processOrderUpdate)
    unsubscribers.push(ordersUnsubscribe)

    // Request initial data
    const requestChannel = `portfolio.request.${userId}`
    const requestUnsubscribe = subscribe(requestChannel, () => {
      // Handle initial data response
    })
    unsubscribers.push(requestUnsubscribe)

    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe())
    }
  }, [userId, subscribe, isConnected, processPortfolioUpdate, processOrderUpdate])

  // Computed values
  const metrics = useMemo(() => {
    if (!portfolio || !positions.length) {
      return {
        bestPerformer: null,
        worstPerformer: null,
        sectorAllocation: {},
        assetAllocation: {},
        riskMetrics: null
      }
    }

    const bestPerformer = positions.reduce((best, pos) => 
      pos.totalReturnPercent > best.totalReturnPercent ? pos : best, positions[0]
    )

    const worstPerformer = positions.reduce((worst, pos) => 
      pos.totalReturnPercent < worst.totalReturnPercent ? pos : worst, positions[0]
    )

    const sectorAllocation = positions.reduce((acc, pos) => {
      acc[pos.sector] = (acc[pos.sector] || 0) + pos.allocation
      return acc
    }, {} as Record<string, number>)

    const assetAllocation = positions.reduce((acc, pos) => {
      acc[pos.assetType] = (acc[pos.assetType] || 0) + pos.allocation
      return acc
    }, {} as Record<string, number>)

    // Simple risk metrics
    const returns = positions.map(p => p.totalReturnPercent)
    const avgReturn = returns.reduce((sum, ret) => sum + ret, 0) / returns.length
    const variance = returns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / returns.length
    const volatility = Math.sqrt(variance)

    const riskMetrics = {
      volatility,
      sharpeRatio: avgReturn / volatility || 0,
      maxDrawdown: Math.min(...returns),
      beta: 1.0 // Simplified
    }

    return {
      bestPerformer,
      worstPerformer,
      sectorAllocation,
      assetAllocation,
      riskMetrics
    }
  }, [portfolio, positions])

  // Utility functions
  const getPosition = useCallback((symbol: string) => {
    return positions.find(p => p.symbol === symbol) || null
  }, [positions])

  const getPositionsBySymbols = useCallback((symbols: string[]) => {
    return positions.filter(p => symbols.includes(p.symbol))
  }, [positions])

  const getOrdersBySymbol = useCallback((symbol: string) => {
    return orders.filter(o => o.symbol === symbol)
  }, [orders])

  const getPendingOrders = useCallback(() => {
    return orders.filter(o => ['PENDING', 'PLACED', 'PARTIAL'].includes(o.status))
  }, [orders])

  const getTotalInvested = useCallback((symbols?: string[]) => {
    const relevantPositions = symbols ? getPositionsBySymbols(symbols) : positions
    return relevantPositions.reduce((sum, pos) => sum + (pos.avgPrice * pos.quantity), 0)
  }, [positions, getPositionsBySymbols])

  const getTotalValue = useCallback((symbols?: string[]) => {
    const relevantPositions = symbols ? getPositionsBySymbols(symbols) : positions
    return relevantPositions.reduce((sum, pos) => sum + pos.marketValue, 0)
  }, [positions, getPositionsBySymbols])

  return {
    // Core data
    portfolio,
    positions,
    orders,
    
    // Status
    isConnected,
    connectionStatus,
    isLoading,
    errors,
    lastUpdate,
    
    // Performance
    updateCount,
    latency,
    
    // Computed metrics
    metrics,
    
    // Utility functions
    getPosition,
    getPositionsBySymbols,
    getOrdersBySymbol,
    getPendingOrders,
    getTotalInvested,
    getTotalValue,
    
    // Counts
    positionCount: positions.length,
    orderCount: orders.length,
    pendingOrderCount: getPendingOrders().length
  }
}

// Trading Updates Hook
export const useTradingUpdates = (userId: string) => {
  const { subscribe, connectionStatus, isConnected } = useWebSocket()
  const [orders, setOrders] = useState<TradingOrder[]>([])
  const [trades, setTrades] = useState<any[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)

  useEffect(() => {
    if (!userId || !isConnected) return

    const unsubscribers: (() => void)[] = []

    // Order status updates
    const ordersChannel = `trading.orders.${userId}`
    const ordersUnsubscribe = subscribe(ordersChannel, (update: OrderUpdate) => {
      const updatedOrder = {
        ...update.order,
        timestamp: new Date(update.order.timestamp),
        lastUpdated: new Date(update.order.lastUpdated)
      }

      setOrders(prev => {
        const existing = prev.find(o => o.id === updatedOrder.id)
        if (existing) {
          return prev.map(o => o.id === updatedOrder.id ? updatedOrder : o)
        } else if (update.type === 'NEW') {
          return [updatedOrder, ...prev.slice(0, 99)] // Keep last 100 orders
        }
        return prev
      })
      
      setLastUpdate(new Date())
    })
    unsubscribers.push(ordersUnsubscribe)

    // Trade confirmations
    const tradesChannel = `trading.trades.${userId}`
    const tradesUnsubscribe = subscribe(tradesChannel, (trade: any) => {
      setTrades(prev => [trade, ...prev.slice(0, 49)]) // Keep last 50 trades
      setLastUpdate(new Date())
    })
    unsubscribers.push(tradesUnsubscribe)

    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe())
    }
  }, [userId, subscribe, isConnected])

  return {
    orders,
    trades,
    lastUpdate,
    connectionStatus,
    isConnected
  }
}