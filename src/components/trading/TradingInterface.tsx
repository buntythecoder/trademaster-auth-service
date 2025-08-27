import React, { useState, useEffect, useMemo } from 'react'
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
  Plus
} from 'lucide-react'
import { TradingServiceFactory, Order, Position, RiskMetrics, TradingProfile } from '@/services/tradingProfiles'
import { OrderForm } from './OrderForm'
import { PositionManager } from './PositionManager'
import { QuickTradeButtons } from './QuickTradeButtons'
import { RiskMeter } from './RiskMeter'
import { useAuthStore } from '../../stores/auth.store'
import { useEnhancedPortfolioWebSocket } from '../../hooks/useEnhancedPortfolioWebSocket'
import { ConnectionStatus } from '../common/ConnectionStatus'
import { WebSocketErrorBoundary } from '../common/WebSocketErrorBoundary'

interface OrderFormData {
  symbol: string
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'BRACKET'
  side: 'BUY' | 'SELL'
  quantity: number
  price?: number
  stopPrice?: number
  target?: number
  timeInForce: 'day' | 'gtc' | 'ioc'
  brokerId?: string
  brokerName?: string
}

// This will be replaced with TradingServiceFactory data

// This will be replaced with TradingServiceFactory data

export function TradingInterface() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<'order' | 'positions' | 'history'>('order')
  const [orders, setOrders] = useState<Order[]>([])
  const [positions, setPositions] = useState<Position[]>([])
  const [riskMetrics, setRiskMetrics] = useState<RiskMetrics | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [currentProfile, setCurrentProfile] = useState<TradingProfile>(TradingServiceFactory.getCurrentProfile())
  const [showProfileSwitch, setShowProfileSwitch] = useState(false)
  
  // Real-time portfolio and trading data with fallback
  const {
    portfolio,
    positions: portfolioPositions,
    orders: portfolioOrders,
    isConnected,
    connectionStatus,
    lastUpdate
  } = useEnhancedPortfolioWebSocket(user?.id || 'demo-user')

  // Load initial data
  useEffect(() => {
    loadTradingData()
  }, [currentProfile])

  const loadTradingData = async () => {
    setIsLoading(true)
    try {
      const [ordersData, positionsData, riskData] = await Promise.all([
        TradingServiceFactory.getOrders(),
        TradingServiceFactory.getPositions(),
        TradingServiceFactory.getRiskMetrics()
      ])
      
      setOrders(ordersData)
      setPositions(positionsData)
      setRiskMetrics(riskData)
    } catch (error) {
      console.error('Failed to load trading data:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleProfileSwitch = (profile: TradingProfile) => {
    TradingServiceFactory.setProfile(profile)
    setCurrentProfile(profile)
    setShowProfileSwitch(false)
    loadTradingData()
  }

  const handleOrderPlaced = async (orderData: Partial<Order>) => {
    try {
      const newOrder = await TradingServiceFactory.placeOrder(orderData)
      setOrders(prev => [newOrder, ...prev])
      await loadTradingData() // Refresh all data
    } catch (error) {
      console.error('Failed to place order:', error)
    }
  }

  const handleOrderCancel = async (orderId: string) => {
    try {
      await TradingServiceFactory.cancelOrder(orderId)
      await loadTradingData() // Refresh data
    } catch (error) {
      console.error('Failed to cancel order:', error)
    }
  }

  const pendingOrders = useMemo(() => 
    orders.filter(order => order.status === 'PENDING'), [orders])
  const executedOrders = useMemo(() => 
    orders.filter(order => order.status === 'EXECUTED'), [orders])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const formatNumber = (num: number, decimals: number = 2) => {
    return num.toLocaleString('en-IN', { 
      minimumFractionDigits: decimals, 
      maximumFractionDigits: decimals 
    })
  }

  const handleBrokerChange = (broker: any) => {
    setAvailableBalance(broker.balance)
  }

  const handleOrderSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setShowOrderConfirmation(true)
    setTimeout(() => setShowOrderConfirmation(false), 3000)
  }

  const calculateOrderValue = () => {
    const price = orderForm.price || 2547.30 // Current market price for RELIANCE
    return orderForm.quantity * price
  }

  const totalPnL = positions.reduce((sum, position) => sum + position.pnl, 0)

  return (
    <WebSocketErrorBoundary>
      <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Trading Interface</h1>
          <p className="text-slate-400">Execute trades and manage your positions</p>
        </div>
        <div className="flex items-center space-x-4">
          {/* Connection Status */}
          <ConnectionStatus
            status={connectionStatus}
            lastUpdate={lastUpdate}
            showDetails={false}
            className="px-3 py-2"
          />
          
          {/* Trading Status */}
          <div className={`glass-card px-4 py-2 rounded-xl ${
            isConnected ? 'border-green-500/30' : 'border-orange-500/30'
          }`}>
            <div className="flex items-center space-x-2">
              <div className={`w-2 h-2 rounded-full ${
                isConnected ? 'bg-green-400 animate-pulse' : 'bg-orange-400'
              }`} />
              <span className={`text-sm font-medium ${
                isConnected ? 'text-green-400' : 'text-orange-400'
              }`}>
                {isConnected ? 'Live Trading' : 'Offline Mode'}
              </span>
            </div>
          </div>
          
          {/* Available Balance */}
          <div className="glass-card px-4 py-2 rounded-xl">
            <div className="text-sm text-slate-400">
              Available: <span className="text-white font-semibold">₹{availableBalance.toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Broker Selection */}
      {user?.role === 'ADMIN' ? (
        <BrokerSelector />
      ) : (
        <CompactBrokerSelector onBrokerChange={handleBrokerChange} />
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Order Form */}
        <div className="lg:col-span-1">
          <div className="glass-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-white">Place Order</h2>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setOrderForm(prev => ({ ...prev, side: 'buy' }))}
                  className={`px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                    orderForm.side === 'buy'
                      ? 'bg-green-500/20 text-green-400 border border-green-500/50'
                      : 'text-slate-400 hover:text-green-400'
                  }`}
                >
                  BUY
                </button>
                <button
                  onClick={() => setOrderForm(prev => ({ ...prev, side: 'sell' }))}
                  className={`px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                    orderForm.side === 'sell'
                      ? 'bg-red-500/20 text-red-400 border border-red-500/50'
                      : 'text-slate-400 hover:text-red-400'
                  }`}
                >
                  SELL
                </button>
              </div>
            </div>

            <form onSubmit={handleOrderSubmit} className="space-y-4">
              {/* Symbol Selection */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
                <select
                  value={orderForm.symbol}
                  onChange={(e) => setOrderForm(prev => ({ ...prev, symbol: e.target.value }))}
                  className="cyber-input w-full py-3 rounded-xl text-white"
                >
                  <option value="RELIANCE">RELIANCE</option>
                  <option value="TCS">TCS</option>
                  <option value="HDFC">HDFC</option>
                  <option value="INFY">INFY</option>
                </select>
              </div>

              {/* Order Type */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Order Type</label>
                <div className="grid grid-cols-3 gap-2">
                  {['market', 'limit', 'stop'].map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setOrderForm(prev => ({ ...prev, orderType: type as any }))}
                      className={`py-2 px-3 rounded-xl text-sm font-medium transition-all capitalize ${
                        orderForm.orderType === type
                          ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50'
                          : 'bg-slate-700/50 text-slate-400 hover:text-white'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>

              {/* Quantity */}
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setOrderForm(prev => ({ ...prev, quantity: Math.max(1, prev.quantity - 1) }))}
                    className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
                  >
                    <Minus className="w-4 h-4" />
                  </button>
                  <input
                    type="number"
                    min="1"
                    value={orderForm.quantity}
                    onChange={(e) => setOrderForm(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
                    className="cyber-input flex-1 text-center py-3 rounded-xl text-white"
                  />
                  <button
                    type="button"
                    onClick={() => setOrderForm(prev => ({ ...prev, quantity: prev.quantity + 1 }))}
                    className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Price (for limit/stop orders) */}
              {orderForm.orderType !== 'market' && (
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">
                    {orderForm.orderType === 'limit' ? 'Limit Price' : 'Stop Price'}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={orderForm.price || ''}
                    onChange={(e) => setOrderForm(prev => ({ ...prev, price: parseFloat(e.target.value) }))}
                    className="cyber-input w-full px-4 py-3 rounded-xl text-white"
                    placeholder="Enter price"
                  />
                </div>
              )}

              {/* Order Value */}
              <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-400">Order Value:</span>
                  <span className="text-white font-semibold">
                    ₹{calculateOrderValue().toLocaleString()}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm mt-2">
                  <span className="text-slate-400">Available:</span>
                  <span className="text-cyan-400 font-semibold">₹{availableBalance.toLocaleString()}</span>
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                className={`w-full py-4 rounded-2xl font-semibold text-lg transition-all ${
                  orderForm.side === 'buy'
                    ? 'bg-gradient-to-r from-green-500 to-green-600 hover:from-green-400 hover:to-green-500 text-white'
                    : 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-400 hover:to-red-500 text-white'
                }`}
              >
                {orderForm.side === 'buy' ? 'Buy' : 'Sell'} {orderForm.symbol}
              </button>
            </form>

            {/* Order Confirmation */}
            {showOrderConfirmation && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                <div className="glass-card rounded-2xl p-6 max-w-sm mx-4">
                  <div className="text-center">
                    <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-4" />
                    <h3 className="text-lg font-bold text-white mb-2">Order Placed!</h3>
                    <p className="text-slate-400 text-sm">
                      Your {orderForm.side} order for {orderForm.quantity} {orderForm.symbol} has been submitted
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Panel - Positions & Orders */}
        <div className="lg:col-span-2">
          <div className="glass-card rounded-2xl p-6">
            {/* Tab Navigation */}
            <div className="flex items-center space-x-4 mb-6 overflow-x-auto">
              {[
                { key: 'positions', label: 'Positions', icon: Target },
                { key: 'orders', label: 'Orders', icon: Clock },
                { key: 'orderbook', label: 'Order Book', icon: ArrowUpDown },
                { key: 'advanced', label: 'Advanced Orders', icon: Settings },
                { key: 'algorithmic', label: 'Algo Trading', icon: Brain },
              ].map(({ key, label, icon: Icon }) => (
                <button
                  key={key}
                  onClick={() => setActiveTab(key as any)}
                  className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
                    activeTab === key
                      ? 'bg-purple-500/20 text-purple-400'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{label}</span>
                </button>
              ))}
            </div>

            {/* Positions Tab */}
            {activeTab === 'positions' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-bold text-white">Open Positions</h3>
                  <div className={`px-3 py-1.5 rounded-xl text-sm font-semibold ${
                    totalPnL >= 0 
                      ? 'bg-green-500/20 text-green-400' 
                      : 'bg-red-500/20 text-red-400'
                  }`}>
                    Total P&L: {totalPnL >= 0 ? '+' : ''}₹{totalPnL.toLocaleString()}
                  </div>
                </div>
                
                <div className="space-y-3">
                  {positions.map((position) => (
                    <div key={position.symbol} className="bg-slate-800/30 rounded-xl p-4 hover:bg-slate-700/30 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                          <div>
                            <div className="font-semibold text-white">{position.symbol}</div>
                            <div className="text-sm text-slate-400">
                              {position.quantity} shares @ ₹{position.avgPrice}
                            </div>
                          </div>
                          <div className="text-sm">
                            <div className="text-slate-300">Current: ₹{position.currentPrice}</div>
                            <div className="text-xs text-slate-500">
                              Value: ₹{(position.quantity * position.currentPrice).toLocaleString()}
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className={`font-semibold ${
                            position.pnl >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {position.pnl >= 0 ? '+' : ''}₹{position.pnl.toLocaleString()}
                          </div>
                          <div className={`text-sm ${
                            position.pnlPercent >= 0 ? 'text-green-400' : 'text-red-400'
                          }`}>
                            {position.pnlPercent >= 0 ? '+' : ''}{position.pnlPercent.toFixed(2)}%
                          </div>
                        </div>
                        <div className="flex items-center space-x-2 ml-4">
                          <button 
                            onClick={() => {
                              setOrderForm(prev => ({
                                ...prev,
                                symbol: position.symbol,
                                side: 'buy',
                                quantity: 1,
                                orderType: 'market'
                              }));
                              setActiveOrderPanel('buy');
                              setActiveTab('order');
                            }}
                            className="p-2 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors"
                            title="Buy more"
                          >
                            <TrendingUp className="w-4 h-4" />
                          </button>
                          <button 
                            onClick={() => {
                              setOrderForm(prev => ({
                                ...prev,
                                symbol: position.symbol,
                                side: 'sell',
                                quantity: Math.min(position.quantity, 1),
                                orderType: 'market'
                              }));
                              setActiveOrderPanel('sell');
                              setActiveTab('order');
                            }}
                            className="p-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                            title="Sell position"
                          >
                            <TrendingDown className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Orders Tab */}
            {activeTab === 'orders' && (
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-white mb-4">Recent Orders</h3>
                <div className="space-y-3">
                  {orders.map((order) => (
                    <div key={order.id} className="bg-slate-800/30 rounded-xl p-4 hover:bg-slate-700/30 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                          <div className={`p-2 rounded-xl ${
                            order.side === 'buy' 
                              ? 'bg-green-500/20 text-green-400' 
                              : 'bg-red-500/20 text-red-400'
                          }`}>
                            {order.side === 'buy' ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
                          </div>
                          <div>
                            <div className="font-semibold text-white">
                              {order.side.toUpperCase()} {order.symbol}
                            </div>
                            <div className="text-sm text-slate-400">
                              {order.quantity} shares @ ₹{order.price} ({order.orderType})
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className={`px-3 py-1 rounded-xl text-xs font-semibold ${
                            order.status === 'filled' 
                              ? 'bg-green-500/20 text-green-400'
                              : order.status === 'pending'
                              ? 'bg-yellow-500/20 text-yellow-400'
                              : order.status === 'cancelled'
                              ? 'bg-gray-500/20 text-gray-400'
                              : 'bg-red-500/20 text-red-400'
                          }`}>
                            {order.status}
                          </div>
                          <div className="text-xs text-slate-500 mt-1">
                            {order.timestamp.toLocaleTimeString()}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Order Book Tab */}
            {activeTab === 'orderbook' && (
              <div>
                <OrderBook />
              </div>
            )}

            {/* Advanced Orders Tab */}
            {activeTab === 'advanced' && (
              <div>
                <AdvancedOrderPanel />
              </div>
            )}

            {/* Algorithmic Trading Tab */}
            {activeTab === 'algorithmic' && (
              <div>
                <AlgorithmicTradingPanel />
              </div>
            )}
          </div>
        </div>
      </div>
      </div>
    </WebSocketErrorBoundary>
  )
}