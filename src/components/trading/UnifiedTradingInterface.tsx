// Unified Trading Interface - Combines all trading functionality
// Merges features from TradingInterface and EnhancedTradingInterface

import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown,
  Activity, 
  AlertTriangle,
  CheckCircle,
  Clock,
  X,
  Settings,
  BarChart3,
  Shield,
  DollarSign,
  Target,
  StopCircle,
  Play,
  Minus,
  Plus,
  RefreshCw,
  Zap,
  Building2,
  PieChart,
  Filter,
  Download,
  Search,
  ArrowUpDown,
  Brain,
  Monitor,
  Smartphone,
  Maximize2
} from 'lucide-react'

// Mock Trading System Integration
import { useMockTrading, OrderRequest } from '../../services/mockTradingService'
import { MockOrder, MockPosition, MockTrade } from '../../services/mockTradingEngine'

// Real-time Data Integration
import { useAuthStore } from '../../stores/auth.store'
import { useEnhancedPortfolioWebSocket } from '../../hooks/useEnhancedPortfolioWebSocket'
import { ConnectionStatus } from '../common/ConnectionStatus'
import { WebSocketErrorBoundary } from '../common/WebSocketErrorBoundary'

// Broker Services
import { MultiBrokerService, BrokerConnection } from '../../services/brokerService'
import { TradingServiceFactory, Order, Position, RiskMetrics, TradingProfile } from '../../services/tradingProfiles'

interface UnifiedTradingInterfaceProps {
  userId?: string
  defaultSymbol?: string
  layout?: 'desktop' | 'tablet' | 'mobile'
}

interface OrderFormData {
  symbol: string
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
  side: 'BUY' | 'SELL'
  quantity: number
  price?: number
  stopPrice?: number
  targetPrice?: number
  timeInForce: 'day' | 'gtc' | 'ioc'
  brokerId?: string
  brokerName?: string
}

// Advanced Order Form Component with all features
const UnifiedOrderForm: React.FC<{
  onSubmitOrder: (order: OrderFormData) => Promise<void>
  isLoading: boolean
  availableBrokers: BrokerConnection[]
  selectedSymbol: string
  currentPrice: number
  availableBalance: number
  compactMode?: boolean
}> = ({ 
  onSubmitOrder, 
  isLoading, 
  availableBrokers, 
  selectedSymbol, 
  currentPrice, 
  availableBalance,
  compactMode = false 
}) => {
  const [orderForm, setOrderForm] = useState<OrderFormData>({
    symbol: selectedSymbol,
    side: 'BUY',
    orderType: 'MARKET',
    quantity: 1,
    price: currentPrice,
    stopPrice: currentPrice * 0.95,
    targetPrice: currentPrice * 1.05,
    timeInForce: 'day',
    brokerId: availableBrokers[0]?.id || '',
    brokerName: availableBrokers[0]?.displayName || ''
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [showOrderConfirmation, setShowOrderConfirmation] = useState(false)

  // Calculate order value
  const orderValue = useMemo(() => {
    const price = orderForm.orderType === 'MARKET' ? currentPrice : (orderForm.price || currentPrice)
    return orderForm.quantity * price
  }, [orderForm.quantity, orderForm.price, orderForm.orderType, currentPrice])

  // Validate form
  const validateForm = useCallback(() => {
    const newErrors: Record<string, string> = {}

    if (!orderForm.brokerId) newErrors.brokerId = 'Please select a broker'
    if (!orderForm.symbol) newErrors.symbol = 'Please enter a symbol'
    if (orderForm.quantity <= 0) newErrors.quantity = 'Quantity must be greater than 0'
    if (orderForm.orderType !== 'MARKET' && (!orderForm.price || orderForm.price <= 0)) {
      newErrors.price = 'Price must be greater than 0'
    }
    if ((orderForm.orderType === 'STOP_LOSS' || orderForm.orderType === 'BRACKET') && (!orderForm.stopPrice || orderForm.stopPrice <= 0)) {
      newErrors.stopPrice = 'Stop price must be greater than 0'
    }
    if (orderForm.orderType === 'BRACKET' && (!orderForm.targetPrice || orderForm.targetPrice <= 0)) {
      newErrors.targetPrice = 'Target price must be greater than 0'
    }
    if (orderValue > availableBalance) {
      newErrors.balance = 'Insufficient balance'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [orderForm, orderValue, availableBalance])

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validateForm()) return

    try {
      await onSubmitOrder(orderForm)
      
      // Show success confirmation
      setShowOrderConfirmation(true)
      setTimeout(() => setShowOrderConfirmation(false), 3000)
      
      // Reset form
      setOrderForm(prev => ({
        ...prev,
        quantity: 1,
        price: currentPrice,
        stopPrice: currentPrice * 0.95,
        targetPrice: currentPrice * 1.05
      }))
    } catch (error) {
      console.error('Order submission failed:', error)
    }
  }

  // Quick quantity buttons
  const quickQuantities = [1, 5, 10, 25, 50, 100]

  return (
    <motion.div 
      className="glass-card p-6 rounded-2xl"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-white flex items-center">
          <Target className="w-5 h-5 mr-2 text-purple-400" />
          Place Order
        </h2>
        <div className="flex items-center space-x-2">
          <div className={`w-2 h-2 rounded-full ${
            isLoading ? 'bg-orange-400 animate-pulse' : 'bg-green-400'
          }`} />
          <span className="text-sm text-slate-400">
            {isLoading ? 'Processing...' : 'Ready'}
          </span>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Broker Selection */}
        {!compactMode && availableBrokers.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">
              Broker Account
            </label>
            <select
              value={orderForm.brokerId}
              onChange={(e) => {
                const broker = availableBrokers.find(b => b.id === e.target.value)
                setOrderForm(prev => ({
                  ...prev,
                  brokerId: e.target.value,
                  brokerName: broker?.displayName || ''
                }))
              }}
              className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="">Select Broker</option>
              {availableBrokers.map(broker => (
                <option key={broker.id} value={broker.id}>
                  {broker.displayName} - ₹{(broker.balance?.availableMargin || 0).toLocaleString()}
                </option>
              ))}
            </select>
            {errors.brokerId && <span className="text-red-400 text-xs mt-1">{errors.brokerId}</span>}
          </div>
        )}

        {/* Symbol and Side */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
            <div className="relative">
              <input
                type="text"
                value={orderForm.symbol}
                onChange={(e) => setOrderForm(prev => ({ ...prev, symbol: e.target.value.toUpperCase() }))}
                placeholder="Enter symbol"
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg pl-3 pr-10 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <Search className="absolute right-3 top-2.5 w-4 h-4 text-slate-400" />
            </div>
            {errors.symbol && <span className="text-red-400 text-xs mt-1">{errors.symbol}</span>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Side</label>
            <div className="flex rounded-lg overflow-hidden">
              <button
                type="button"
                onClick={() => setOrderForm(prev => ({ ...prev, side: 'BUY' }))}
                className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                  orderForm.side === 'BUY' ? 'bg-green-600 text-white' : 'bg-slate-700 text-slate-300'
                }`}
              >
                BUY
              </button>
              <button
                type="button"
                onClick={() => setOrderForm(prev => ({ ...prev, side: 'SELL' }))}
                className={`flex-1 py-2 px-3 text-sm font-medium transition-colors ${
                  orderForm.side === 'SELL' ? 'bg-red-600 text-white' : 'bg-slate-700 text-slate-300'
                }`}
              >
                SELL
              </button>
            </div>
          </div>
        </div>

        {/* Order Type */}
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">Order Type</label>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2">
            {['MARKET', 'LIMIT', 'STOP_LOSS', 'BRACKET'].map((type) => (
              <button
                key={type}
                type="button"
                onClick={() => setOrderForm(prev => ({ ...prev, orderType: type as any }))}
                className={`py-2 px-3 rounded-lg text-sm font-medium transition-all ${
                  orderForm.orderType === type
                    ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50'
                    : 'bg-slate-700/50 text-slate-400 hover:text-white'
                }`}
              >
                {type.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        {/* Quantity with Quick Buttons */}
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <button
                type="button"
                onClick={() => setOrderForm(prev => ({ ...prev, quantity: Math.max(1, prev.quantity - 1) }))}
                className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
              >
                <Minus className="w-4 h-4" />
              </button>
              <input
                type="number"
                min="1"
                value={orderForm.quantity}
                onChange={(e) => setOrderForm(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
                className="flex-1 bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm text-center focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <button
                type="button"
                onClick={() => setOrderForm(prev => ({ ...prev, quantity: prev.quantity + 1 }))}
                className="p-2 rounded-lg bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
            
            {/* Quick quantity buttons */}
            <div className="flex flex-wrap gap-1">
              {quickQuantities.map(qty => (
                <button
                  key={qty}
                  type="button"
                  onClick={() => setOrderForm(prev => ({ ...prev, quantity: qty }))}
                  className="px-3 py-1 text-xs bg-slate-700/50 text-slate-400 hover:text-white rounded-lg transition-colors"
                >
                  {qty}
                </button>
              ))}
            </div>
            {errors.quantity && <span className="text-red-400 text-xs">{errors.quantity}</span>}
          </div>
        </div>

        {/* Price Fields */}
        {orderForm.orderType !== 'MARKET' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                {orderForm.orderType === 'LIMIT' ? 'Limit Price' : 'Price'}
              </label>
              <input
                type="number"
                step="0.01"
                value={orderForm.price || ''}
                onChange={(e) => setOrderForm(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                placeholder="Enter price"
              />
              {errors.price && <span className="text-red-400 text-xs mt-1">{errors.price}</span>}
            </div>

            {(orderForm.orderType === 'STOP_LOSS' || orderForm.orderType === 'BRACKET') && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Stop Loss Price</label>
                <input
                  type="number"
                  step="0.01"
                  value={orderForm.stopPrice || ''}
                  onChange={(e) => setOrderForm(prev => ({ ...prev, stopPrice: parseFloat(e.target.value) || 0 }))}
                  className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="Enter stop price"
                />
                {errors.stopPrice && <span className="text-red-400 text-xs mt-1">{errors.stopPrice}</span>}
              </div>
            )}
          </div>
        )}

        {/* Target Price for Bracket Orders */}
        {orderForm.orderType === 'BRACKET' && (
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Target Price</label>
            <input
              type="number"
              step="0.01"
              value={orderForm.targetPrice || ''}
              onChange={(e) => setOrderForm(prev => ({ ...prev, targetPrice: parseFloat(e.target.value) || 0 }))}
              className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              placeholder="Enter target price"
            />
            {errors.targetPrice && <span className="text-red-400 text-xs mt-1">{errors.targetPrice}</span>}
          </div>
        )}

        {/* Time in Force */}
        {!compactMode && (
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Time in Force</label>
            <div className="flex space-x-2">
              {[{ value: 'day', label: 'Day' }, { value: 'gtc', label: 'GTC' }, { value: 'ioc', label: 'IOC' }].map(({ value, label }) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setOrderForm(prev => ({ ...prev, timeInForce: value as any }))}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    orderForm.timeInForce === value
                      ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50'
                      : 'bg-slate-700/50 text-slate-400 hover:text-white'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Order Summary */}
        <div className="bg-slate-800/30 rounded-lg p-4">
          <div className="flex items-center justify-between text-sm mb-2">
            <span className="text-slate-400">Order Value:</span>
            <span className="text-white font-semibold">₹{orderValue.toLocaleString()}</span>
          </div>
          <div className="flex items-center justify-between text-sm mb-2">
            <span className="text-slate-400">Brokerage (est.):</span>
            <span className="text-slate-300">₹{(orderValue * 0.0003).toFixed(2)}</span>
          </div>
          <div className="flex items-center justify-between text-sm mb-2">
            <span className="text-slate-400">Available:</span>
            <span className={`font-semibold ${
              availableBalance >= orderValue ? 'text-cyan-400' : 'text-red-400'
            }`}>
              ₹{availableBalance.toLocaleString()}
            </span>
          </div>
          <div className="flex items-center justify-between text-sm border-t border-slate-600 pt-2">
            <span className="text-slate-400">Total Amount:</span>
            <span className="text-white font-bold">
              ₹{(orderValue + orderValue * 0.0003).toLocaleString()}
            </span>
          </div>
          {errors.balance && <div className="text-red-400 text-xs mt-2">{errors.balance}</div>}
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isLoading || Object.keys(errors).length > 0}
          className={`w-full py-4 rounded-2xl font-semibold text-lg transition-all duration-300 ${
            isLoading || Object.keys(errors).length > 0
              ? 'bg-slate-600 cursor-not-allowed text-slate-400'
              : orderForm.side === 'BUY'
              ? 'bg-gradient-to-r from-green-500 to-green-600 hover:from-green-400 hover:to-green-500 text-white hover:scale-105 shadow-lg hover:shadow-xl'
              : 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-400 hover:to-red-500 text-white hover:scale-105 shadow-lg hover:shadow-xl'
          }`}
        >
          {isLoading ? (
            <div className="flex items-center justify-center">
              <RefreshCw className="w-5 h-5 animate-spin mr-2" />
              Processing...
            </div>
          ) : (
            `${orderForm.side} ${orderForm.quantity} ${orderForm.symbol}`
          )}
        </button>
      </form>

      {/* Order Confirmation Modal */}
      <AnimatePresence>
        {showOrderConfirmation && (
          <motion.div
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              className="glass-card rounded-2xl p-6 max-w-sm mx-4"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
            >
              <div className="text-center">
                <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-4" />
                <h3 className="text-lg font-bold text-white mb-2">Order Placed!</h3>
                <p className="text-slate-400 text-sm">
                  Your {orderForm.side} order for {orderForm.quantity} {orderForm.symbol} has been submitted
                </p>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}

// Main Unified Trading Interface
export const UnifiedTradingInterface: React.FC<UnifiedTradingInterfaceProps> = ({
  userId = 'demo-user',
  defaultSymbol = 'RELIANCE',
  layout = 'desktop'
}) => {
  const { user } = useAuthStore()
  const [viewMode, setViewMode] = useState<'desktop' | 'tablet' | 'mobile'>(layout)
  const [selectedSymbol, setSelectedSymbol] = useState(defaultSymbol)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [activeTab, setActiveTab] = useState<'order' | 'positions' | 'history' | 'analytics'>('order')
  const [availableBrokers, setAvailableBrokers] = useState<BrokerConnection[]>([])
  const [selectedBroker, setSelectedBroker] = useState<BrokerConnection | null>(null)
  const [availableBalance, setAvailableBalance] = useState(500000)

  // Mock Trading System Integration
  const {
    orders: mockOrders,
    positions: mockPositions,
    trades,
    accounts,
    riskLimits,
    isLoading: mockLoading,
    error: mockError,
    lastUpdated,
    placeOrder: placeMockOrder,
    cancelOrder: cancelMockOrder,
    getPortfolioMetrics,
    getRiskMetrics,
    refresh: refreshMock,
    clearError
  } = useMockTrading(userId)

  // Real-time WebSocket Data
  const portfolioData = useEnhancedPortfolioWebSocket(user?.id || 'demo-user')
  const {
    portfolio,
    positions: wsPositions,
    orders: wsOrders,
    isConnected,
    connectionStatus,
    lastUpdate
  } = portfolioData || {}

  // Trading Profile Integration
  const [currentProfile, setCurrentProfile] = useState<TradingProfile>(TradingServiceFactory.getCurrentProfile())
  const [orders, setOrders] = useState<Order[]>([])
  const [positions, setPositions] = useState<Position[]>([])
  const [riskMetrics, setRiskMetrics] = useState<RiskMetrics | null>(null)

  // Broker connections setup
  useEffect(() => {
    loadBrokerConnections()
  }, [])

  const loadBrokerConnections = async () => {
    try {
      // Initialize mock brokers
      MultiBrokerService.initializeMockBrokers()
      
      // Get connected brokers
      const brokers = MultiBrokerService.getConnectedBrokers().filter(b => b.status === 'connected')
      setAvailableBrokers(brokers)
      
      // Set default broker
      const defaultBroker = MultiBrokerService.getDefaultBroker()
      if (defaultBroker) {
        setSelectedBroker(defaultBroker)
        setAvailableBalance(defaultBroker.balance?.availableMargin || 500000)
      }
    } catch (error) {
      console.error('Failed to load broker connections:', error)
    }
  }

  // Mock current prices
  const getCurrentPrice = useCallback((symbol: string): number => {
    const basePrices: Record<string, number> = {
      'RELIANCE': 2456.75,
      'TCS': 3789.40,
      'HDFCBANK': 1678.90,
      'INFY': 1456.30,
      'ICICIBANK': 1123.45
    }
    return basePrices[symbol] || 1000
  }, [])

  const currentPrice = getCurrentPrice(selectedSymbol)
  const portfolioMetrics = getPortfolioMetrics()
  const riskMetricsData = getRiskMetrics()

  // Handle order submission - combines both systems
  const handleOrderSubmit = async (orderData: OrderFormData) => {
    try {
      // Submit to mock trading system
      await placeMockOrder({
        ...orderData,
        targetPrice: orderData.targetPrice
      })

      // Submit to multi-broker service if broker selected
      if (selectedBroker) {
        await MultiBrokerService.placeOrder({
          ...orderData,
          brokerId: selectedBroker.id
        })
      }

      // Submit to trading service factory
      const newOrder = await TradingServiceFactory.placeOrder(orderData)
      setOrders(prev => [newOrder, ...prev])

      clearError()
    } catch (error) {
      console.error('Failed to place order:', error)
    }
  }

  // Handle order cancellation
  const handleOrderCancel = async (orderId: string) => {
    try {
      await cancelMockOrder(orderId)
      await TradingServiceFactory.cancelOrder(orderId)
    } catch (error) {
      console.error('Failed to cancel order:', error)
    }
  }

  // View mode controls
  const ViewModeToggle = () => (
    <div className="flex items-center space-x-1 bg-slate-800/50 rounded-lg p-1">
      {[
        { mode: 'desktop', icon: Monitor, title: 'Desktop View' },
        { mode: 'tablet', icon: BarChart3, title: 'Tablet View' },
        { mode: 'mobile', icon: Smartphone, title: 'Mobile View' }
      ].map(({ mode, icon: Icon, title }) => (
        <button
          key={mode}
          onClick={() => setViewMode(mode as any)}
          className={`p-2 rounded-lg transition-all ${
            viewMode === mode ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
          }`}
          title={title}
        >
          <Icon className="h-4 w-4" />
        </button>
      ))}
    </div>
  )

  // Layout configurations
  const getLayoutClasses = () => {
    switch (viewMode) {
      case 'mobile':
        return {
          container: 'flex flex-col space-y-4',
          mainSection: 'w-full',
          sidePanel: 'w-full'
        }
      case 'tablet':
        return {
          container: 'flex flex-col space-y-6',
          mainSection: 'w-full',
          sidePanel: 'w-full grid grid-cols-2 gap-6'
        }
      default: // desktop
        return {
          container: 'flex space-x-6',
          mainSection: 'flex-1 space-y-6',
          sidePanel: 'w-96 space-y-6'
        }
    }
  }

  const layoutClasses = getLayoutClasses()

  return (
    <WebSocketErrorBoundary>
      <div className={`min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-800 p-4 ${
        isFullscreen ? 'fixed inset-0 z-50' : ''
      }`}>
        <div className="max-w-7xl mx-auto">
          {/* Header */}
          <motion.div
            className="glass-card p-6 rounded-2xl mb-6"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div>
                  <h1 className="text-3xl font-bold gradient-text mb-2">Unified Trading Interface</h1>
                  <p className="text-slate-400">Complete trading functionality with real-time data and mock profiles</p>
                </div>
              </div>

              <div className="flex items-center space-x-4">
                <ConnectionStatus
                  status={connectionStatus || 'connected'}
                  lastUpdate={lastUpdate || lastUpdated}
                  showDetails={false}
                  className="px-3 py-2"
                />
                
                <ViewModeToggle />
                
                <button
                  onClick={() => setIsFullscreen(!isFullscreen)}
                  className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors"
                  title="Toggle Fullscreen"
                >
                  <Maximize2 className="w-5 h-5 text-slate-300" />
                </button>

                <button
                  onClick={refreshMock}
                  disabled={mockLoading}
                  className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors"
                  title="Refresh Data"
                >
                  <RefreshCw className={`w-5 h-5 text-slate-300 ${mockLoading ? 'animate-spin' : ''}`} />
                </button>
                
                <div className="glass-card px-4 py-2 rounded-xl">
                  <div className="text-sm text-slate-400">
                    Available: <span className="text-white font-semibold">₹{availableBalance.toLocaleString()}</span>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>

          {/* Error Message */}
          {mockError && (
            <motion.div
              className="bg-red-600/20 border border-red-500/30 rounded-lg p-4 mb-6"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <AlertTriangle className="w-5 h-5 text-red-400 mr-2" />
                  <span className="text-red-400">{mockError}</span>
                </div>
                <button onClick={clearError} className="text-red-400 hover:text-red-300">
                  <X className="w-5 h-5" />
                </button>
              </div>
            </motion.div>
          )}

          {/* Portfolio Summary Cards */}
          <motion.div
            className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className="glass-card p-4 rounded-xl">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-400 text-sm">Total Value</p>
                  <p className="text-white text-2xl font-bold">₹{portfolioMetrics.totalValue.toLocaleString()}</p>
                </div>
                <DollarSign className="w-8 h-8 text-green-400" />
              </div>
            </div>

            <div className="glass-card p-4 rounded-xl">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-400 text-sm">Total P&L</p>
                  <p className={`text-2xl font-bold ${
                    portfolioMetrics.totalPnL >= 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {portfolioMetrics.totalPnL >= 0 ? '+' : ''}₹{portfolioMetrics.totalPnL.toLocaleString()}
                  </p>
                </div>
                {portfolioMetrics.totalPnL >= 0 ? (
                  <TrendingUp className="w-8 h-8 text-green-400" />
                ) : (
                  <TrendingDown className="w-8 h-8 text-red-400" />
                )}
              </div>
            </div>

            <div className="glass-card p-4 rounded-xl">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-400 text-sm">Day P&L</p>
                  <p className={`text-2xl font-bold ${
                    portfolioMetrics.totalDayPnL >= 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {portfolioMetrics.totalDayPnL >= 0 ? '+' : ''}₹{portfolioMetrics.totalDayPnL.toLocaleString()}
                  </p>
                </div>
                <Activity className={`w-8 h-8 ${
                  portfolioMetrics.totalDayPnL >= 0 ? 'text-green-400' : 'text-red-400'
                }`} />
              </div>
            </div>

            <div className="glass-card p-4 rounded-xl">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-slate-400 text-sm">Risk Level</p>
                  <p className={`text-2xl font-bold ${
                    riskMetricsData.riskLevel === 'LOW' ? 'text-green-400' :
                    riskMetricsData.riskLevel === 'MEDIUM' ? 'text-yellow-400' :
                    riskMetricsData.riskLevel === 'HIGH' ? 'text-orange-400' : 'text-red-400'
                  }`}>
                    {riskMetricsData.riskLevel}
                  </p>
                </div>
                <Shield className={`w-8 h-8 ${
                  riskMetricsData.riskLevel === 'LOW' ? 'text-green-400' :
                  riskMetricsData.riskLevel === 'MEDIUM' ? 'text-yellow-400' :
                  riskMetricsData.riskLevel === 'HIGH' ? 'text-orange-400' : 'text-red-400'
                }`} />
              </div>
            </div>
          </motion.div>

          {/* Tab Navigation - Mobile */}
          {viewMode === 'mobile' && (
            <motion.div
              className="glass-card p-4 rounded-2xl mb-6"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <div className="flex items-center justify-center space-x-1">
                {[
                  { id: 'order', label: 'Place Order', icon: Target },
                  { id: 'history', label: 'History', icon: Clock },
                  { id: 'positions', label: 'Positions', icon: BarChart3 },
                  { id: 'analytics', label: 'Analytics', icon: PieChart }
                ].map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`flex items-center px-4 py-3 rounded-xl transition-all duration-300 ${
                      activeTab === tab.id
                        ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/30'
                        : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                    }`}
                  >
                    <tab.icon className="w-4 h-4 mr-2" />
                    <span className="text-sm">{tab.label}</span>
                  </button>
                ))}
              </div>
            </motion.div>
          )}

          {/* Main Content */}
          <div className={layoutClasses.container}>
            {/* Main Section */}
            <div className={layoutClasses.mainSection}>
              {/* Desktop/Tablet Layout */}
              {viewMode !== 'mobile' && (
                <>
                  {/* Order Form */}
                  <UnifiedOrderForm
                    onSubmitOrder={handleOrderSubmit}
                    isLoading={mockLoading}
                    availableBrokers={availableBrokers}
                    selectedSymbol={selectedSymbol}
                    currentPrice={currentPrice}
                    availableBalance={availableBalance}
                    compactMode={viewMode === 'tablet'}
                  />
                </>
              )}

              {/* Mobile Layout - Tab Content */}
              {viewMode === 'mobile' && (
                <AnimatePresence mode="wait">
                  <motion.div
                    key={activeTab}
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -20 }}
                    transition={{ duration: 0.3 }}
                  >
                    {activeTab === 'order' && (
                      <UnifiedOrderForm
                        onSubmitOrder={handleOrderSubmit}
                        isLoading={mockLoading}
                        availableBrokers={availableBrokers}
                        selectedSymbol={selectedSymbol}
                        currentPrice={currentPrice}
                        availableBalance={availableBalance}
                        compactMode={true}
                      />
                    )}

                    {activeTab === 'history' && (
                      <motion.div 
                        className="glass-card p-6 rounded-2xl"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                      >
                        <h2 className="text-xl font-bold text-white mb-6 flex items-center">
                          <Clock className="w-5 h-5 mr-2 text-blue-400" />
                          Order History ({mockOrders.length})
                        </h2>

                        {mockOrders.length === 0 ? (
                          <div className="text-center py-8 text-slate-400">
                            <Clock className="w-12 h-12 mx-auto mb-4 opacity-50" />
                            <p>No orders found</p>
                          </div>
                        ) : (
                          <div className="space-y-4">
                            {mockOrders.slice(0, 10).map((order) => (
                              <div
                                key={order.id}
                                className="bg-slate-800/40 rounded-lg p-4 hover:bg-slate-800/60 transition-colors"
                              >
                                <div className="flex items-center justify-between">
                                  <div className="flex items-center space-x-3">
                                    <span className={`px-2 py-1 text-xs font-bold rounded-lg ${
                                      order.side === 'BUY' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'
                                    }`}>
                                      {order.side}
                                    </span>
                                    <span className="font-semibold text-white">{order.symbol}</span>
                                    <span className={`px-2 py-1 text-xs font-medium rounded-lg ${
                                      order.status === 'FILLED' ? 'text-green-400 bg-green-400/20' :
                                      order.status === 'PENDING' ? 'text-yellow-400 bg-yellow-400/20' :
                                      order.status === 'CANCELLED' ? 'text-red-400 bg-red-400/20' :
                                      'text-slate-400 bg-slate-400/20'
                                    }`}>
                                      {order.status}
                                    </span>
                                  </div>
                                  {['PENDING', 'PLACED', 'PARTIAL'].includes(order.status) && (
                                    <button
                                      onClick={() => handleOrderCancel(order.id)}
                                      className="px-3 py-1 text-sm bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                                    >
                                      Cancel
                                    </button>
                                  )}
                                </div>
                                <div className="grid grid-cols-2 gap-4 mt-3 text-sm">
                                  <div>
                                    <span className="text-slate-400">Quantity:</span>
                                    <span className="text-white ml-2">{order.quantity}</span>
                                  </div>
                                  <div>
                                    <span className="text-slate-400">Price:</span>
                                    <span className="text-white ml-2">
                                      {order.orderType === 'MARKET' ? 'Market' : `₹${order.price?.toFixed(2)}`}
                                    </span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </motion.div>
                    )}

                    {activeTab === 'positions' && (
                      <motion.div 
                        className="glass-card p-6 rounded-2xl"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                      >
                        <h2 className="text-xl font-bold text-white mb-6 flex items-center">
                          <BarChart3 className="w-5 h-5 mr-2 text-green-400" />
                          Positions ({mockPositions.length})
                        </h2>

                        {mockPositions.length === 0 ? (
                          <div className="text-center py-8 text-slate-400">
                            <BarChart3 className="w-12 h-12 mx-auto mb-4 opacity-50" />
                            <p>No positions found</p>
                          </div>
                        ) : (
                          <div className="space-y-4">
                            {mockPositions.map((position) => (
                              <div
                                key={position.id}
                                className="bg-slate-800/40 rounded-lg p-4 hover:bg-slate-800/60 transition-colors"
                              >
                                <div className="flex items-center justify-between mb-3">
                                  <div className="flex items-center space-x-3">
                                    <span className="font-bold text-white text-lg">{position.symbol}</span>
                                    <span className="text-slate-400 text-sm">{position.companyName}</span>
                                  </div>
                                  <span className={`px-3 py-1 text-sm font-medium rounded-lg ${
                                    position.totalPnL >= 0 ? 'bg-green-600/20 text-green-400' : 'bg-red-600/20 text-red-400'
                                  }`}>
                                    {position.totalPnL >= 0 ? '+' : ''}₹{position.totalPnL.toLocaleString()} 
                                    ({position.pnlPercent.toFixed(2)}%)
                                  </span>
                                </div>

                                <div className="grid grid-cols-2 gap-4 text-sm">
                                  <div>
                                    <span className="text-slate-400">Quantity:</span>
                                    <span className="text-white ml-2 font-semibold">{position.quantity}</span>
                                  </div>
                                  <div>
                                    <span className="text-slate-400">Avg Price:</span>
                                    <span className="text-white ml-2">₹{position.avgPrice.toFixed(2)}</span>
                                  </div>
                                  <div>
                                    <span className="text-slate-400">Current:</span>
                                    <span className="text-white ml-2">₹{position.currentPrice.toFixed(2)}</span>
                                  </div>
                                  <div>
                                    <span className="text-slate-400">Value:</span>
                                    <span className="text-white ml-2">₹{position.marketValue.toLocaleString()}</span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </motion.div>
                    )}

                    {activeTab === 'analytics' && (
                      <motion.div 
                        className="glass-card p-6 rounded-2xl"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                      >
                        <h2 className="text-xl font-bold text-white mb-6 flex items-center">
                          <PieChart className="w-5 h-5 mr-2 text-purple-400" />
                          Portfolio Analytics
                        </h2>

                        <div className="space-y-6">
                          <div className="bg-slate-800/40 rounded-lg p-4">
                            <h3 className="text-lg font-semibold text-white mb-4">Risk Metrics</h3>
                            <div className="space-y-3">
                              <div className="flex justify-between">
                                <span className="text-slate-400">Portfolio Exposure:</span>
                                <span className="text-white">{riskMetricsData.portfolioExposure.toFixed(1)}%</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-400">Concentration Risk:</span>
                                <span className="text-white">{riskMetricsData.concentration.toFixed(1)}%</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-slate-400">Volatility:</span>
                                <span className="text-white">{riskMetricsData.volatility.toFixed(2)}</span>
                              </div>
                            </div>
                          </div>

                          <div className="bg-slate-800/40 rounded-lg p-4">
                            <h3 className="text-lg font-semibold text-white mb-4">Sector Allocation</h3>
                            <div className="space-y-3">
                              {Object.entries(riskMetricsData.sectorAllocation).map(([sector, value]) => (
                                <div key={sector} className="flex justify-between">
                                  <span className="text-slate-400">{sector}:</span>
                                  <span className="text-white">₹{value.toLocaleString()}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </motion.div>
                </AnimatePresence>
              )}
            </div>
          </div>
        </div>
      </div>
    </WebSocketErrorBoundary>
  )
}

export default UnifiedTradingInterface