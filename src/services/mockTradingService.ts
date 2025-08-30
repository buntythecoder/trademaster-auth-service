// Mock Trading Service - Provides React hooks for trading functionality
// Integrates with mockTradingEngine and WebSocket service

import { useState, useEffect, useCallback } from 'react'
import { mockTradingEngine, MockOrder, MockPosition, MockTrade, MockBrokerAccount, RiskLimits } from './mockTradingEngine'
import { webSocketService } from './websocket.service'

export interface OrderRequest {
  brokerId: string
  symbol: string
  side: 'BUY' | 'SELL'
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
  quantity: number
  price?: number
  stopPrice?: number
  targetPrice?: string | number
}

export interface TradingState {
  orders: MockOrder[]
  positions: MockPosition[]
  trades: MockTrade[]
  accounts: MockBrokerAccount[]
  riskLimits: RiskLimits
  isLoading: boolean
  error: string | null
  lastUpdated: Date | null
}

// Custom hook for trading functionality
export function useMockTrading(userId: string) {
  const [state, setState] = useState<TradingState>({
    orders: [],
    positions: [],
    trades: [],
    accounts: [],
    riskLimits: {
      maxOrderValue: 0,
      maxPositionSize: 0,
      maxDayTradingLimit: 0,
      maxPortfolioExposure: 0,
      minStopLossPercent: 0
    },
    isLoading: true,
    error: null,
    lastUpdated: null
  })

  // Load initial data
  const loadTradingData = useCallback(() => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }))

      const profile = mockTradingEngine.getProfile(userId)
      if (!profile) {
        throw new Error('User profile not found')
      }

      setState({
        orders: mockTradingEngine.getOrders(userId),
        positions: mockTradingEngine.getPositions(userId),
        trades: mockTradingEngine.getTrades(userId),
        accounts: mockTradingEngine.getBrokerAccounts(userId),
        riskLimits: profile.riskLimits,
        isLoading: false,
        error: null,
        lastUpdated: new Date()
      })
    } catch (error) {
      console.error('Failed to load trading data:', error)
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Failed to load trading data'
      }))
    }
  }, [userId])

  // Place order
  const placeOrder = useCallback(async (orderRequest: OrderRequest): Promise<MockOrder | null> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }))

      const order = await mockTradingEngine.placeOrder(userId, orderRequest)
      
      // Refresh data
      loadTradingData()
      
      // Emit order update event
      webSocketService.emit('orderUpdate', {
        orderId: order.id,
        symbol: order.symbol,
        side: order.side,
        quantity: order.quantity,
        price: order.price,
        status: order.status,
        timestamp: order.timestamp,
        type: order.orderType
      })

      console.log(`Order placed successfully: ${order.id}`)
      return order
    } catch (error) {
      console.error('Failed to place order:', error)
      setState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Failed to place order'
      }))
      return null
    }
  }, [userId, loadTradingData])

  // Cancel order
  const cancelOrder = useCallback(async (orderId: string): Promise<boolean> => {
    try {
      const success = await mockTradingEngine.cancelOrder(userId, orderId)
      
      if (success) {
        // Refresh data
        loadTradingData()
        
        // Emit order update event
        const order = mockTradingEngine.getOrders(userId).find(o => o.id === orderId)
        if (order) {
          webSocketService.emit('orderUpdate', {
            orderId: order.id,
            symbol: order.symbol,
            side: order.side,
            quantity: order.quantity,
            price: order.price,
            status: order.status,
            timestamp: order.timestamp,
            type: order.orderType
          })
        }

        console.log(`Order cancelled successfully: ${orderId}`)
      }
      
      return success
    } catch (error) {
      console.error('Failed to cancel order:', error)
      setState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to cancel order'
      }))
      return false
    }
  }, [userId, loadTradingData])

  // Get position by symbol
  const getPosition = useCallback((symbol: string, brokerId?: string): MockPosition | null => {
    return state.positions.find(pos => 
      pos.symbol === symbol && (!brokerId || pos.brokerId === brokerId)
    ) || null
  }, [state.positions])

  // Get orders by symbol
  const getOrdersBySymbol = useCallback((symbol: string, brokerId?: string): MockOrder[] => {
    return state.orders.filter(order => 
      order.symbol === symbol && (!brokerId || order.brokerId === brokerId)
    )
  }, [state.orders])

  // Get pending orders
  const getPendingOrders = useCallback((): MockOrder[] => {
    return state.orders.filter(order => ['PENDING', 'PLACED', 'PARTIAL'].includes(order.status))
  }, [state.orders])

  // Calculate portfolio metrics
  const getPortfolioMetrics = useCallback(() => {
    const totalValue = state.positions.reduce((sum, pos) => sum + pos.marketValue, 0)
    const totalInvested = state.positions.reduce((sum, pos) => sum + pos.totalInvested, 0)
    const totalPnL = state.positions.reduce((sum, pos) => sum + pos.totalPnL, 0)
    const totalDayPnL = state.positions.reduce((sum, pos) => sum + pos.dayPnL, 0)
    const totalBalance = state.accounts.reduce((sum, acc) => sum + acc.balance, 0)
    const totalMarginUsed = state.accounts.reduce((sum, acc) => sum + acc.marginUsed, 0)
    const totalMarginAvailable = state.accounts.reduce((sum, acc) => sum + acc.marginAvailable, 0)

    return {
      totalValue,
      totalInvested,
      totalPnL,
      totalPnLPercent: totalInvested > 0 ? (totalPnL / totalInvested) * 100 : 0,
      totalDayPnL,
      totalDayPnLPercent: totalValue > 0 ? (totalDayPnL / totalValue) * 100 : 0,
      totalBalance,
      totalMarginUsed,
      totalMarginAvailable,
      positionCount: state.positions.length,
      orderCount: state.orders.length,
      pendingOrderCount: getPendingOrders().length
    }
  }, [state.positions, state.accounts, state.orders, getPendingOrders])

  // Calculate risk metrics
  const getRiskMetrics = useCallback(() => {
    const metrics = getPortfolioMetrics()
    const portfolioExposure = metrics.totalBalance > 0 ? (metrics.totalValue / metrics.totalBalance) * 100 : 0
    
    // Calculate sector allocation
    const sectorAllocation: Record<string, number> = {}
    state.positions.forEach(pos => {
      sectorAllocation[pos.sector] = (sectorAllocation[pos.sector] || 0) + pos.marketValue
    })

    // Calculate concentration risk (largest position %)
    const largestPosition = Math.max(...state.positions.map(pos => pos.marketValue))
    const concentration = metrics.totalValue > 0 ? (largestPosition / metrics.totalValue) * 100 : 0

    // Simple volatility calculation (standard deviation of position returns)
    const returns = state.positions.map(pos => pos.pnlPercent)
    const avgReturn = returns.reduce((sum, ret) => sum + ret, 0) / returns.length || 0
    const variance = returns.reduce((sum, ret) => sum + Math.pow(ret - avgReturn, 2), 0) / returns.length || 0
    const volatility = Math.sqrt(variance)

    return {
      portfolioExposure,
      concentration,
      volatility,
      sectorAllocation,
      riskLevel: portfolioExposure < 20 ? 'LOW' as const :
                portfolioExposure < 50 ? 'MEDIUM' as const :
                portfolioExposure < 80 ? 'HIGH' as const : 'CRITICAL' as const
    }
  }, [state.positions, getPortfolioMetrics])

  // Listen to price updates and update positions
  useEffect(() => {
    const handleMarketData = (data: any) => {
      if (data.symbol) {
        // Update position prices in mock engine
        mockTradingEngine.updatePrices(userId, { [data.symbol]: data.price })
        
        // Refresh positions
        setState(prev => ({
          ...prev,
          positions: mockTradingEngine.getPositions(userId),
          lastUpdated: new Date()
        }))
      }
    }

    webSocketService.on('marketData', handleMarketData)

    return () => {
      webSocketService.off('marketData', handleMarketData)
    }
  }, [userId])

  // Listen to order updates from mock engine
  useEffect(() => {
    const handleOrderUpdate = () => {
      setState(prev => ({
        ...prev,
        orders: mockTradingEngine.getOrders(userId),
        positions: mockTradingEngine.getPositions(userId),
        trades: mockTradingEngine.getTrades(userId),
        lastUpdated: new Date()
      }))
    }

    // Check for order updates every 2 seconds
    const interval = setInterval(handleOrderUpdate, 2000)

    return () => clearInterval(interval)
  }, [userId])

  // Initial data load
  useEffect(() => {
    loadTradingData()
  }, [loadTradingData])

  return {
    // State
    ...state,
    
    // Actions
    placeOrder,
    cancelOrder,
    loadTradingData,
    
    // Getters
    getPosition,
    getOrdersBySymbol,
    getPendingOrders,
    getPortfolioMetrics,
    getRiskMetrics,
    
    // Utilities
    refresh: loadTradingData,
    clearError: () => setState(prev => ({ ...prev, error: null }))
  }
}

// Export trading service for direct use
export const mockTradingService = {
  getProfile: mockTradingEngine.getProfile.bind(mockTradingEngine),
  getBrokerAccounts: mockTradingEngine.getBrokerAccounts.bind(mockTradingEngine),
  getPositions: mockTradingEngine.getPositions.bind(mockTradingEngine),
  getOrders: mockTradingEngine.getOrders.bind(mockTradingEngine),
  getTrades: mockTradingEngine.getTrades.bind(mockTradingEngine),
  placeOrder: mockTradingEngine.placeOrder.bind(mockTradingEngine),
  cancelOrder: mockTradingEngine.cancelOrder.bind(mockTradingEngine),
}

export default mockTradingService