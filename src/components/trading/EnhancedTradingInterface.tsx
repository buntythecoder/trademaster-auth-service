// Enhanced Trading Interface with Mock Trading System
// FRONT-002: Trading Interface Implementation with comprehensive mock profile

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
  Search
} from 'lucide-react'
import { useMockTrading, OrderRequest } from '../../services/mockTradingService'
import { MockOrder, MockPosition, MockTrade } from '../../services/mockTradingEngine'
import { useAuthStore } from '../../stores/auth.store'
import { ConnectionStatus } from '../common/ConnectionStatus'

interface EnhancedTradingInterfaceProps {
  userId?: string
  defaultSymbol?: string
}

// Advanced Order Form Component
const AdvancedOrderForm: React.FC<{
  onSubmitOrder: (order: OrderRequest) => Promise<void>
  isLoading: boolean
  accounts: any[]
  selectedSymbol: string
  currentPrice: number
}> = ({ onSubmitOrder, isLoading, accounts, selectedSymbol, currentPrice }) => {
  const [orderForm, setOrderForm] = useState({
    brokerId: accounts[0]?.brokerId || '',
    symbol: selectedSymbol,
    side: 'BUY' as 'BUY' | 'SELL',
    orderType: 'MARKET' as 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET',
    quantity: 1,
    price: currentPrice,
    stopPrice: currentPrice * 0.95,
    targetPrice: currentPrice * 1.05
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [orderValue, setOrderValue] = useState(0)

  // Calculate order value
  useEffect(() => {
    const price = orderForm.orderType === 'MARKET' ? currentPrice : orderForm.price
    setOrderValue(orderForm.quantity * price)
  }, [orderForm.quantity, orderForm.price, orderForm.orderType, currentPrice])

  // Validate form
  const validateForm = useCallback(() => {
    const newErrors: Record<string, string> = {}

    if (!orderForm.brokerId) newErrors.brokerId = 'Please select a broker'
    if (!orderForm.symbol) newErrors.symbol = 'Please enter a symbol'
    if (orderForm.quantity <= 0) newErrors.quantity = 'Quantity must be greater than 0'
    if (orderForm.orderType !== 'MARKET' && orderForm.price <= 0) newErrors.price = 'Price must be greater than 0'
    if (orderForm.orderType === 'STOP_LOSS' && orderForm.stopPrice <= 0) newErrors.stopPrice = 'Stop price must be greater than 0'
    if (orderForm.orderType === 'BRACKET') {
      if (orderForm.stopPrice <= 0) newErrors.stopPrice = 'Stop price must be greater than 0'
      if (!orderForm.targetPrice || orderForm.targetPrice <= 0) newErrors.targetPrice = 'Target price must be greater than 0'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [orderForm])

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validateForm()) return

    try {
      await onSubmitOrder({
        ...orderForm,
        targetPrice: orderForm.targetPrice?.toString()
      })
      
      // Reset form after successful submission
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
          <div className={`w-2 h-2 rounded-full ${isLoading ? 'bg-orange-400 animate-pulse' : 'bg-green-400'}`} />
          <span className="text-sm text-slate-400">{isLoading ? 'Processing...' : 'Ready'}</span>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Broker Selection */}
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">Broker Account</label>
          <select
            value={orderForm.brokerId}
            onChange={(e) => setOrderForm(prev => ({ ...prev, brokerId: e.target.value }))}
            className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            <option value="">Select Broker</option>
            {accounts.map(account => (
              <option key={account.id} value={account.brokerId}>
                {account.displayName} - ₹{account.balance.toLocaleString()}
              </option>
            ))}
          </select>
          {errors.brokerId && <span className="text-red-400 text-xs mt-1">{errors.brokerId}</span>}
        </div>

        {/* Symbol */}
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
          <div className="relative">
            <input
              type="text"
              value={orderForm.symbol}
              onChange={(e) => setOrderForm(prev => ({ ...prev, symbol: e.target.value.toUpperCase() }))}
              placeholder="Enter symbol (e.g., RELIANCE)"
              className="w-full bg-slate-800/50 border border-slate-600 rounded-lg pl-3 pr-10 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <Search className="absolute right-3 top-2.5 w-4 h-4 text-slate-400" />
          </div>
          {errors.symbol && <span className="text-red-400 text-xs mt-1">{errors.symbol}</span>}
        </div>

        {/* Side and Order Type */}
        <div className="grid grid-cols-2 gap-4">
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

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Order Type</label>
            <select
              value={orderForm.orderType}
              onChange={(e) => setOrderForm(prev => ({ ...prev, orderType: e.target.value as any }))}
              className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              <option value="MARKET">Market</option>
              <option value="LIMIT">Limit</option>
              <option value="STOP_LOSS">Stop Loss</option>
              <option value="BRACKET">Bracket</option>
            </select>
          </div>
        </div>

        {/* Quantity and Price */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
            <div className="relative">
              <input
                type="number"
                min="1"
                value={orderForm.quantity}
                onChange={(e) => setOrderForm(prev => ({ ...prev, quantity: parseInt(e.target.value) || 0 }))}
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>
            {errors.quantity && <span className="text-red-400 text-xs mt-1">{errors.quantity}</span>}
          </div>

          {orderForm.orderType !== 'MARKET' && (
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Price</label>
              <input
                type="number"
                step="0.01"
                value={orderForm.price}
                onChange={(e) => setOrderForm(prev => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              {errors.price && <span className="text-red-400 text-xs mt-1">{errors.price}</span>}
            </div>
          )}
        </div>

        {/* Stop Loss and Target for advanced orders */}
        {(orderForm.orderType === 'STOP_LOSS' || orderForm.orderType === 'BRACKET') && (
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Stop Loss Price</label>
              <input
                type="number"
                step="0.01"
                value={orderForm.stopPrice}
                onChange={(e) => setOrderForm(prev => ({ ...prev, stopPrice: parseFloat(e.target.value) || 0 }))}
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              {errors.stopPrice && <span className="text-red-400 text-xs mt-1">{errors.stopPrice}</span>}
            </div>

            {orderForm.orderType === 'BRACKET' && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Target Price</label>
                <input
                  type="number"
                  step="0.01"
                  value={orderForm.targetPrice}
                  onChange={(e) => setOrderForm(prev => ({ ...prev, targetPrice: parseFloat(e.target.value) || 0 }))}
                  className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
                {errors.targetPrice && <span className="text-red-400 text-xs mt-1">{errors.targetPrice}</span>}
              </div>
            )}
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
          <div className="flex items-center justify-between text-sm">
            <span className="text-slate-400">Total Amount:</span>
            <span className="text-white font-bold">₹{(orderValue + orderValue * 0.0003).toLocaleString()}</span>
          </div>
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isLoading}
          className={`w-full py-3 px-4 rounded-lg font-semibold text-white transition-all duration-300 ${
            isLoading
              ? 'bg-slate-600 cursor-not-allowed'
              : orderForm.side === 'BUY'
              ? 'bg-green-600 hover:bg-green-700 hover:scale-105'
              : 'bg-red-600 hover:bg-red-700 hover:scale-105'
          } ${!isLoading ? 'shadow-lg hover:shadow-xl' : ''}`}
        >
          {isLoading ? (
            <div className="flex items-center justify-center">
              <RefreshCw className="w-4 h-4 animate-spin mr-2" />
              Processing...
            </div>
          ) : (
            `${orderForm.side} ${orderForm.quantity} ${orderForm.symbol}`
          )}
        </button>
      </form>
    </motion.div>
  )
}

// Order History Component
const OrderHistory: React.FC<{
  orders: MockOrder[]
  onCancelOrder: (orderId: string) => Promise<void>
}> = ({ orders, onCancelOrder }) => {
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'FILLED' | 'CANCELLED'>('ALL')

  const filteredOrders = useMemo(() => {
    if (filter === 'ALL') return orders
    return orders.filter(order => {
      switch (filter) {
        case 'PENDING': return ['PENDING', 'PLACED', 'PARTIAL'].includes(order.status)
        case 'FILLED': return order.status === 'FILLED'
        case 'CANCELLED': return ['CANCELLED', 'REJECTED'].includes(order.status)
        default: return true
      }
    })
  }, [orders, filter])

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'FILLED': return 'text-green-400 bg-green-400/20'
      case 'PENDING': case 'PLACED': return 'text-yellow-400 bg-yellow-400/20'
      case 'PARTIAL': return 'text-blue-400 bg-blue-400/20'
      case 'CANCELLED': case 'REJECTED': return 'text-red-400 bg-red-400/20'
      default: return 'text-slate-400 bg-slate-400/20'
    }
  }

  return (
    <motion.div 
      className="glass-card p-6 rounded-2xl"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 }}
    >
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-bold text-white flex items-center">
          <Clock className="w-5 h-5 mr-2 text-blue-400" />
          Order History ({orders.length})
        </h2>

        {/* Filter Buttons */}
        <div className="flex items-center space-x-2">
          {['ALL', 'PENDING', 'FILLED', 'CANCELLED'].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status as any)}
              className={`px-3 py-1 text-xs font-medium rounded-lg transition-colors ${
                filter === status
                  ? 'bg-purple-600 text-white'
                  : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
              }`}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      {filteredOrders.length === 0 ? (
        <div className="text-center py-8 text-slate-400">
          <Clock className="w-12 h-12 mx-auto mb-4 opacity-50" />
          <p>No orders found</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredOrders.map((order) => (
            <motion.div
              key={order.id}
              className="bg-slate-800/40 rounded-lg p-4 hover:bg-slate-800/60 transition-colors"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              whileHover={{ scale: 1.02 }}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3 mb-2">
                    <span className={`px-2 py-1 text-xs font-bold rounded-lg ${
                      order.side === 'BUY' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'
                    }`}>
                      {order.side}
                    </span>
                    <span className="font-semibold text-white">{order.symbol}</span>
                    <span className={`px-2 py-1 text-xs font-medium rounded-lg ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
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
                    <div>
                      <span className="text-slate-400">Filled:</span>
                      <span className="text-white ml-2">{order.filledQuantity}/{order.quantity}</span>
                    </div>
                    <div>
                      <span className="text-slate-400">Time:</span>
                      <span className="text-white ml-2">{order.timestamp.toLocaleTimeString()}</span>
                    </div>
                  </div>

                  {order.avgFillPrice && (
                    <div className="mt-2 text-sm">
                      <span className="text-slate-400">Avg Fill Price:</span>
                      <span className="text-green-400 ml-2 font-semibold">₹{order.avgFillPrice.toFixed(2)}</span>
                    </div>
                  )}
                </div>

                {/* Action Buttons */}
                <div className="ml-4">
                  {['PENDING', 'PLACED', 'PARTIAL'].includes(order.status) && (
                    <button
                      onClick={() => onCancelOrder(order.id)}
                      className="px-3 py-1 text-sm bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </motion.div>
  )
}

// Main Enhanced Trading Interface
export const EnhancedTradingInterface: React.FC<EnhancedTradingInterfaceProps> = ({
  userId = 'demo-user',
  defaultSymbol = 'RELIANCE'
}) => {
  const { user } = useAuthStore()
  const {
    orders,
    positions,
    trades,
    accounts,
    riskLimits,
    isLoading,
    error,
    lastUpdated,
    placeOrder,
    cancelOrder,
    getPortfolioMetrics,
    getRiskMetrics,
    refresh,
    clearError
  } = useMockTrading(userId)

  const [selectedSymbol, setSelectedSymbol] = useState(defaultSymbol)
  const [activeTab, setActiveTab] = useState<'order' | 'history' | 'positions' | 'analytics'>('order')

  // Mock current price for selected symbol
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
  const riskMetrics = getRiskMetrics()

  // Handle order submission
  const handleOrderSubmit = async (orderRequest: OrderRequest) => {
    await placeOrder(orderRequest)
    clearError()
  }

  // Handle order cancellation
  const handleOrderCancel = async (orderId: string) => {
    await cancelOrder(orderId)
    clearError()
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-800 p-4">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          className="glass-card p-6 rounded-2xl mb-6"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white mb-2">Enhanced Trading Interface</h1>
              <p className="text-slate-400">Complete trading functionality with mock profile system</p>
            </div>

            <div className="flex items-center space-x-4">
              <ConnectionStatus 
                status="connected"
                lastUpdate={lastUpdated}
                showDetails={false}
                className="px-3 py-2"
              />
              
              <button
                onClick={refresh}
                disabled={isLoading}
                className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors"
              >
                <RefreshCw className={`w-5 h-5 text-slate-300 ${isLoading ? 'animate-spin' : ''}`} />
              </button>
            </div>
          </div>
        </motion.div>

        {/* Error Message */}
        {error && (
          <motion.div
            className="bg-red-600/20 border border-red-500/30 rounded-lg p-4 mb-6"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <AlertTriangle className="w-5 h-5 text-red-400 mr-2" />
                <span className="text-red-400">{error}</span>
              </div>
              <button onClick={clearError} className="text-red-400 hover:text-red-300">
                <X className="w-5 h-5" />
              </button>
            </div>
          </motion.div>
        )}

        {/* Portfolio Summary */}
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
                <p className={`text-2xl font-bold ${portfolioMetrics.totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
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
                <p className={`text-2xl font-bold ${portfolioMetrics.totalDayPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {portfolioMetrics.totalDayPnL >= 0 ? '+' : ''}₹{portfolioMetrics.totalDayPnL.toLocaleString()}
                </p>
              </div>
              <Activity className={`w-8 h-8 ${portfolioMetrics.totalDayPnL >= 0 ? 'text-green-400' : 'text-red-400'}`} />
            </div>
          </div>

          <div className="glass-card p-4 rounded-xl">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-slate-400 text-sm">Risk Level</p>
                <p className={`text-2xl font-bold ${
                  riskMetrics.riskLevel === 'LOW' ? 'text-green-400' :
                  riskMetrics.riskLevel === 'MEDIUM' ? 'text-yellow-400' :
                  riskMetrics.riskLevel === 'HIGH' ? 'text-orange-400' : 'text-red-400'
                }`}>
                  {riskMetrics.riskLevel}
                </p>
              </div>
              <Shield className={`w-8 h-8 ${
                riskMetrics.riskLevel === 'LOW' ? 'text-green-400' :
                riskMetrics.riskLevel === 'MEDIUM' ? 'text-yellow-400' :
                riskMetrics.riskLevel === 'HIGH' ? 'text-orange-400' : 'text-red-400'
              }`} />
            </div>
          </div>
        </motion.div>

        {/* Tab Navigation */}
        <motion.div
          className="glass-card p-4 rounded-2xl mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <div className="flex items-center justify-center space-x-1">
            {[
              { id: 'order', label: 'Place Order', icon: Target },
              { id: 'history', label: 'Order History', icon: Clock },
              { id: 'positions', label: 'Positions', icon: BarChart3 },
              { id: 'analytics', label: 'Analytics', icon: PieChart }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center px-6 py-3 rounded-xl transition-all duration-300 ${
                  activeTab === tab.id
                    ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/30'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <tab.icon className="w-5 h-5 mr-2" />
                {tab.label}
              </button>
            ))}
          </div>
        </motion.div>

        {/* Tab Content */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
          >
            {activeTab === 'order' && (
              <AdvancedOrderForm
                onSubmitOrder={handleOrderSubmit}
                isLoading={isLoading}
                accounts={accounts}
                selectedSymbol={selectedSymbol}
                currentPrice={currentPrice}
              />
            )}

            {activeTab === 'history' && (
              <OrderHistory
                orders={orders}
                onCancelOrder={handleOrderCancel}
              />
            )}

            {activeTab === 'positions' && (
              <motion.div 
                className="glass-card p-6 rounded-2xl"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
              >
                <h2 className="text-xl font-bold text-white mb-6 flex items-center">
                  <BarChart3 className="w-5 h-5 mr-2 text-green-400" />
                  Positions ({positions.length})
                </h2>

                {positions.length === 0 ? (
                  <div className="text-center py-8 text-slate-400">
                    <BarChart3 className="w-12 h-12 mx-auto mb-4 opacity-50" />
                    <p>No positions found</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {positions.map((position) => (
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

                        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
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
                          <div>
                            <span className="text-slate-400">Day P&L:</span>
                            <span className={`ml-2 font-semibold ${position.dayPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                              {position.dayPnL >= 0 ? '+' : ''}₹{position.dayPnL.toFixed(2)}
                            </span>
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

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Risk Metrics */}
                  <div className="bg-slate-800/40 rounded-lg p-4">
                    <h3 className="text-lg font-semibold text-white mb-4">Risk Metrics</h3>
                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-slate-400">Portfolio Exposure:</span>
                        <span className="text-white">{riskMetrics.portfolioExposure.toFixed(1)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Concentration Risk:</span>
                        <span className="text-white">{riskMetrics.concentration.toFixed(1)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Volatility:</span>
                        <span className="text-white">{riskMetrics.volatility.toFixed(2)}</span>
                      </div>
                    </div>
                  </div>

                  {/* Sector Allocation */}
                  <div className="bg-slate-800/40 rounded-lg p-4">
                    <h3 className="text-lg font-semibold text-white mb-4">Sector Allocation</h3>
                    <div className="space-y-3">
                      {Object.entries(riskMetrics.sectorAllocation).map(([sector, value]) => (
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
      </div>
    </div>
  )
}

export default EnhancedTradingInterface