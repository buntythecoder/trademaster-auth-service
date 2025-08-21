import { useState, useEffect } from 'react'
import { useSelector } from 'react-redux'
import { RootState } from '../store'
import { tradingApi, positionApi, portfolioApi } from '../lib/api'
import type { Order, Position, Portfolio, PlaceOrderRequest } from '../types'

// Development mode - provide mock data when backend is not available
const isDevelopmentMode = import.meta.env.DEV

// Mock data for development
const mockPortfolios: Portfolio[] = [
  {
    id: '1',
    name: 'Demo Portfolio',
    description: 'Demo portfolio for development',
    userId: 'dev-user',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    totalValue: 500000,
    availableCash: 100000,
    totalInvested: 400000,
    totalPnl: 25000,
    dayPnl: 2500
  }
]

const mockPositions: Position[] = [
  {
    id: '1',
    portfolioId: '1',
    symbol: 'RELIANCE',
    quantity: 100,
    averagePrice: 2450.00,
    currentPrice: 2465.50,
    unrealizedPnl: 1550.00,
    marketValue: 246550.00,
    positionType: 'LONG',
    createdAt: new Date().toISOString()
  },
  {
    id: '2',
    portfolioId: '1',
    symbol: 'TCS',
    quantity: 50,
    averagePrice: 3520.00,
    currentPrice: 3498.25,
    unrealizedPnl: -1087.50,
    marketValue: 174912.50,
    positionType: 'LONG',
    createdAt: new Date().toISOString()
  }
]

const mockOrders: Order[] = [
  {
    id: '1',
    portfolioId: '1',
    symbol: 'HDFC',
    quantity: 25,
    price: 1650.00,
    side: 'BUY',
    status: 'PENDING',
    orderType: 'LIMIT',
    timeInForce: 'DAY',
    createdAt: new Date().toISOString()
  },
  {
    id: '2',
    portfolioId: '1',
    symbol: 'INFY',
    quantity: 75,
    price: 1420.50,
    side: 'SELL',
    status: 'FILLED',
    orderType: 'MARKET',
    timeInForce: 'DAY',
    createdAt: new Date().toISOString()
  }
]

export interface TradingAccount {
  accountValue: number
  buyingPower: number
  dayPnl: number
  totalPnl: number
  cash: number
  equity: number
}

export const useTrading = () => {
  const { token } = useSelector((state: RootState) => state.auth)
  
  const [positions, setPositions] = useState<Position[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<string | null>(null)
  const [account, setAccount] = useState<TradingAccount | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Fetch portfolios
  const fetchPortfolios = async () => {
    setLoading(true)
    setError(null)
    
    try {
      if (isDevelopmentMode) {
        // Use mock data in development
        setPortfolios(mockPortfolios)
        if (!selectedPortfolioId && mockPortfolios.length > 0) {
          setSelectedPortfolioId(mockPortfolios[0].id)
        }
      } else {
        if (!token) return
        const response = await portfolioApi.getPortfolios()
        setPortfolios(response.content || [])
        // Auto-select first portfolio if none selected
        if (!selectedPortfolioId && response.content.length > 0) {
          setSelectedPortfolioId(response.content[0].id)
        }
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch portfolios')
      console.error('Error fetching portfolios:', err)
    } finally {
      setLoading(false)
    }
  }

  // Fetch positions
  const fetchPositions = async () => {
    if (!selectedPortfolioId) return
    
    setLoading(true)
    setError(null)
    
    try {
      if (isDevelopmentMode) {
        // Use mock data in development
        setPositions(mockPositions.filter(p => p.portfolioId === selectedPortfolioId))
      } else {
        if (!token) return
        const response = await positionApi.getPositions(selectedPortfolioId, { activeOnly: true })
        setPositions(response.content || [])
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch positions')
      console.error('Error fetching positions:', err)
    } finally {
      setLoading(false)
    }
  }

  // Fetch orders
  const fetchOrders = async () => {
    setLoading(true)
    setError(null)
    
    try {
      if (isDevelopmentMode) {
        // Use mock data in development
        setOrders(mockOrders.filter(o => !selectedPortfolioId || o.portfolioId === selectedPortfolioId))
      } else {
        if (!token) return
        const response = await tradingApi.getOrders({ portfolioId: selectedPortfolioId || undefined })
        setOrders(response.content || [])
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch orders')
      console.error('Error fetching orders:', err)
    } finally {
      setLoading(false)
    }
  }

  // Fetch account info (using portfolio summary as account info)
  const fetchAccount = async () => {
    if (!selectedPortfolioId) return
    
    setLoading(true)
    setError(null)
    
    try {
      if (isDevelopmentMode) {
        // Use mock data in development
        const portfolio = mockPortfolios.find(p => p.id === selectedPortfolioId)
        if (portfolio) {
          setAccount({
            accountValue: portfolio.totalValue || 500000,
            buyingPower: portfolio.availableCash || 100000,
            dayPnl: portfolio.dayPnl || 2500,
            totalPnl: portfolio.totalPnl || 25000,
            cash: portfolio.availableCash || 100000,
            equity: (portfolio.totalValue || 500000) - (portfolio.availableCash || 100000)
          })
        }
      } else {
        if (!token) return
        const summary = await portfolioApi.getPortfolioSummary(selectedPortfolioId)
        // Map portfolio summary to account structure
        setAccount({
          accountValue: summary.totalValue || 0,
          buyingPower: summary.availableCash || 0,
          dayPnl: summary.dayPnl || 0,
          totalPnl: summary.totalPnl || 0,
          cash: summary.cash || 0,
          equity: summary.equity || 0
        })
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch account info')
      console.error('Error fetching account:', err)
    } finally {
      setLoading(false)
    }
  }

  // Place order
  const placeOrder = async (orderRequest: PlaceOrderRequest) => {
    if (!token || !selectedPortfolioId) throw new Error('Not authenticated or no portfolio selected')
    
    setLoading(true)
    setError(null)
    
    try {
      // Add portfolio ID to order request
      const orderWithPortfolio = {
        ...orderRequest,
        portfolioId: selectedPortfolioId
      }
      
      const response = await tradingApi.placeOrder(orderWithPortfolio)
      
      // Refresh orders and positions after successful order
      await Promise.all([fetchOrders(), fetchPositions(), fetchAccount()])
      
      return response
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to place order'
      setError(errorMessage)
      console.error('Error placing order:', err)
      throw new Error(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  // Cancel order
  const cancelOrder = async (orderId: string) => {
    if (!token) throw new Error('Not authenticated')
    
    setLoading(true)
    setError(null)
    
    try {
      await tradingApi.cancelOrder(orderId)
      
      // Refresh orders after cancellation
      await fetchOrders()
      
      return true
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to cancel order'
      setError(errorMessage)
      console.error('Error canceling order:', err)
      throw new Error(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  // Close position
  const closePosition = async (positionId: string) => {
    if (!token || !selectedPortfolioId) throw new Error('Not authenticated or no portfolio selected')
    
    setLoading(true)
    setError(null)
    
    try {
      await positionApi.closePosition(selectedPortfolioId, positionId)
      
      // Refresh positions and orders after closing
      await Promise.all([fetchPositions(), fetchOrders(), fetchAccount()])
      
      return true
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to close position'
      setError(errorMessage)
      console.error('Error closing position:', err)
      throw new Error(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  // Initialize data on mount
  useEffect(() => {
    if (isDevelopmentMode || token) {
      fetchPortfolios()
    }
  }, [token])

  // Fetch trading data when portfolio is selected
  useEffect(() => {
    if (selectedPortfolioId && (isDevelopmentMode || token)) {
      Promise.all([
        fetchPositions(),
        fetchOrders(),
        fetchAccount()
      ])
    }
  }, [token, selectedPortfolioId])

  // Auto-refresh data every 30 seconds
  useEffect(() => {
    if (!selectedPortfolioId || (!isDevelopmentMode && !token)) return

    const interval = setInterval(() => {
      Promise.all([
        fetchPositions(),
        fetchOrders(),
        fetchAccount()
      ])
    }, 30000) // 30 seconds

    return () => clearInterval(interval)
  }, [token, selectedPortfolioId])

  return {
    positions,
    orders,
    portfolios,
    selectedPortfolioId,
    setSelectedPortfolioId,
    account,
    loading,
    error,
    placeOrder,
    cancelOrder,
    closePosition,
    fetchPositions,
    fetchOrders,
    fetchAccount,
    fetchPortfolios,
    setError
  }
}