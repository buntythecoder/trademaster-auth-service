import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Clock, 
  CheckCircle, 
  AlertCircle, 
  XCircle,
  Filter,
  Calendar,
  TrendingUp,
  TrendingDown,
  MoreVertical,
  Download,
  Search,
  RefreshCw,
  Zap
} from 'lucide-react'

export type OrderStatus = 'pending' | 'executed' | 'cancelled' | 'rejected' | 'partial'
export type OrderType = 'market' | 'limit' | 'stop_loss' | 'stop_limit' | 'bracket' | 'cover'
export type OrderSide = 'buy' | 'sell'

export interface Order {
  id: string
  symbol: string
  side: OrderSide
  orderType: OrderType
  quantity: number
  price?: number
  stopPrice?: number
  targetPrice?: number
  executedQuantity: number
  executedPrice?: number
  status: OrderStatus
  placedAt: Date
  executedAt?: Date
  validUntil?: Date
  brokerage: number
  taxes: number
  pnl?: number
  broker: string
  rejection_reason?: string
  parentOrderId?: string
  childOrders?: string[]
  tags?: string[]
}

export interface OrderHistoryProps {
  orders?: Order[]
  onOrderCancel?: (orderId: string) => void
  onOrderModify?: (orderId: string, updates: Partial<Order>) => void
  onRefresh?: () => void
  showBrokerFilter?: boolean
  showPnLSummary?: boolean
  compactMode?: boolean
  className?: string
}

const OrderHistory: React.FC<OrderHistoryProps> = ({
  orders = [],
  onOrderCancel,
  onOrderModify,
  onRefresh,
  showBrokerFilter = true,
  showPnLSummary = true,
  compactMode = false,
  className = ''
}) => {
  const [filteredOrders, setFilteredOrders] = useState<Order[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'all'>('all')
  const [sideFilter, setSideFilter] = useState<OrderSide | 'all'>('all')
  const [brokerFilter, setBrokerFilter] = useState<string>('all')
  const [dateFilter, setDateFilter] = useState<'today' | 'week' | 'month' | 'all'>('today')
  const [isRefreshing, setIsRefreshing] = useState(false)

  // Mock data if no orders provided
  const mockOrders: Order[] = useMemo(() => [
    {
      id: 'ORD-001',
      symbol: 'RELIANCE',
      side: 'buy',
      orderType: 'limit',
      quantity: 100,
      price: 2456.50,
      executedQuantity: 100,
      executedPrice: 2456.75,
      status: 'executed',
      placedAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
      executedAt: new Date(Date.now() - 2 * 60 * 60 * 1000 + 30000),
      brokerage: 12.28,
      taxes: 49.13,
      pnl: 25.00,
      broker: 'Zerodha'
    },
    {
      id: 'ORD-002',
      symbol: 'TCS',
      side: 'sell',
      orderType: 'market',
      quantity: 50,
      executedQuantity: 50,
      executedPrice: 3789.20,
      status: 'executed',
      placedAt: new Date(Date.now() - 4 * 60 * 60 * 1000),
      executedAt: new Date(Date.now() - 4 * 60 * 60 * 1000 + 15000),
      brokerage: 9.47,
      taxes: 37.89,
      pnl: -105.50,
      broker: 'Upstox'
    },
    {
      id: 'ORD-003',
      symbol: 'HDFCBANK',
      side: 'buy',
      orderType: 'bracket',
      quantity: 200,
      price: 1687.00,
      stopPrice: 1670.00,
      targetPrice: 1720.00,
      executedQuantity: 0,
      status: 'pending',
      placedAt: new Date(Date.now() - 30 * 60 * 1000),
      validUntil: new Date(Date.now() + 24 * 60 * 60 * 1000),
      brokerage: 0,
      taxes: 0,
      broker: 'Angel One'
    },
    {
      id: 'ORD-004',
      symbol: 'INFY',
      side: 'buy',
      orderType: 'stop_loss',
      quantity: 150,
      price: 1456.00,
      stopPrice: 1450.00,
      executedQuantity: 0,
      status: 'cancelled',
      placedAt: new Date(Date.now() - 6 * 60 * 60 * 1000),
      brokerage: 0,
      taxes: 0,
      broker: 'ICICI Direct'
    },
    {
      id: 'ORD-005',
      symbol: 'ICICIBANK',
      side: 'sell',
      orderType: 'limit',
      quantity: 300,
      price: 987.50,
      executedQuantity: 150,
      executedPrice: 987.35,
      status: 'partial',
      placedAt: new Date(Date.now() - 1 * 60 * 60 * 1000),
      brokerage: 7.41,
      taxes: 14.81,
      pnl: 67.50,
      broker: 'Zerodha'
    }
  ], [])

  const displayOrders = orders.length > 0 ? orders : mockOrders

  // Filter logic
  useEffect(() => {
    let filtered = displayOrders

    // Search filter
    if (searchQuery.trim()) {
      filtered = filtered.filter(order => 
        order.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.broker.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }

    // Status filter
    if (statusFilter !== 'all') {
      filtered = filtered.filter(order => order.status === statusFilter)
    }

    // Side filter
    if (sideFilter !== 'all') {
      filtered = filtered.filter(order => order.side === sideFilter)
    }

    // Broker filter
    if (brokerFilter !== 'all') {
      filtered = filtered.filter(order => order.broker === brokerFilter)
    }

    // Date filter
    if (dateFilter !== 'all') {
      const now = new Date()
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      
      filtered = filtered.filter(order => {
        switch (dateFilter) {
          case 'today':
            return order.placedAt >= startOfToday
          case 'week':
            return order.placedAt >= new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
          case 'month':
            return order.placedAt >= new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
          default:
            return true
        }
      })
    }

    setFilteredOrders(filtered.sort((a, b) => b.placedAt.getTime() - a.placedAt.getTime()))
  }, [displayOrders, searchQuery, statusFilter, sideFilter, brokerFilter, dateFilter])

  // Summary calculations
  const summary = useMemo(() => {
    const executed = filteredOrders.filter(o => o.status === 'executed')
    const totalBrokerage = executed.reduce((sum, o) => sum + o.brokerage, 0)
    const totalTaxes = executed.reduce((sum, o) => sum + o.taxes, 0)
    const totalPnL = executed.reduce((sum, o) => sum + (o.pnl || 0), 0)
    const totalValue = executed.reduce((sum, o) => sum + (o.executedQuantity * (o.executedPrice || 0)), 0)
    
    return {
      totalOrders: filteredOrders.length,
      executedOrders: executed.length,
      pendingOrders: filteredOrders.filter(o => o.status === 'pending').length,
      totalValue,
      totalBrokerage,
      totalTaxes,
      totalPnL,
      winRate: executed.length > 0 ? executed.filter(o => (o.pnl || 0) > 0).length / executed.length * 100 : 0
    }
  }, [filteredOrders])

  // Unique brokers for filter
  const uniqueBrokers = useMemo(() => {
    return [...new Set(displayOrders.map(o => o.broker))].sort()
  }, [displayOrders])

  const handleRefresh = async () => {
    setIsRefreshing(true)
    await new Promise(resolve => setTimeout(resolve, 1500)) // Simulate refresh
    onRefresh?.()
    setIsRefreshing(false)
  }

  const getStatusIcon = (status: OrderStatus) => {
    switch (status) {
      case 'executed':
        return <CheckCircle className="w-4 h-4 text-green-400" />
      case 'pending':
        return <Clock className="w-4 h-4 text-yellow-400" />
      case 'partial':
        return <Zap className="w-4 h-4 text-blue-400" />
      case 'cancelled':
        return <XCircle className="w-4 h-4 text-gray-400" />
      case 'rejected':
        return <AlertCircle className="w-4 h-4 text-red-400" />
      default:
        return <Clock className="w-4 h-4 text-slate-400" />
    }
  }

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case 'executed':
        return 'text-green-400 bg-green-400/10'
      case 'pending':
        return 'text-yellow-400 bg-yellow-400/10'
      case 'partial':
        return 'text-blue-400 bg-blue-400/10'
      case 'cancelled':
        return 'text-gray-400 bg-gray-400/10'
      case 'rejected':
        return 'text-red-400 bg-red-400/10'
      default:
        return 'text-slate-400 bg-slate-400/10'
    }
  }

  const formatCurrency = (value: number) => {
    return `₹${value.toLocaleString('en-IN', { 
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`
  }

  const OrderCard: React.FC<{ order: Order }> = ({ order }) => {
    return (
      <motion.div
        className="glass-card p-4 rounded-xl border border-slate-700/50 hover:border-purple-400/30 transition-all duration-300"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        layout
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              {getStatusIcon(order.status)}
              <span className="font-semibold text-white">{order.symbol}</span>
              <span className={`px-2 py-0.5 rounded text-xs font-medium ${getStatusColor(order.status)}`}>
                {order.status.toUpperCase()}
              </span>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-400">{order.broker}</span>
            <button className="p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors">
              <MoreVertical className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Order Details */}
        <div className="grid grid-cols-2 gap-4 mb-3">
          <div>
            <div className="text-xs text-slate-400 mb-1">Order</div>
            <div className="text-sm">
              <span className={`font-medium ${order.side === 'buy' ? 'text-green-400' : 'text-red-400'}`}>
                {order.side.toUpperCase()}
              </span>
              <span className="text-slate-300 ml-2">{order.quantity}</span>
              {order.executedQuantity > 0 && order.executedQuantity < order.quantity && (
                <span className="text-blue-400 ml-1">({order.executedQuantity})</span>
              )}
            </div>
            <div className="text-xs text-slate-400 mt-1">
              {order.orderType.toUpperCase()}
            </div>
          </div>

          <div>
            <div className="text-xs text-slate-400 mb-1">Price</div>
            <div className="text-sm">
              {order.executedPrice ? (
                <span className="text-white font-medium">
                  {formatCurrency(order.executedPrice)}
                </span>
              ) : order.price ? (
                <span className="text-slate-300">
                  {formatCurrency(order.price)}
                </span>
              ) : (
                <span className="text-slate-400">Market</span>
              )}
            </div>
            {order.stopPrice && (
              <div className="text-xs text-red-400 mt-1">
                Stop: {formatCurrency(order.stopPrice)}
              </div>
            )}
            {order.targetPrice && (
              <div className="text-xs text-green-400 mt-1">
                Target: {formatCurrency(order.targetPrice)}
              </div>
            )}
          </div>
        </div>

        {/* Value & PnL */}
        <div className="flex items-center justify-between text-xs mb-3">
          <div className="text-slate-400">
            Value: <span className="text-white">
              {order.executedQuantity > 0 && order.executedPrice 
                ? formatCurrency(order.executedQuantity * order.executedPrice)
                : order.price 
                  ? formatCurrency(order.quantity * order.price)
                  : 'N/A'
              }
            </span>
          </div>
          
          {order.pnl && (
            <div className={`flex items-center ${order.pnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {order.pnl >= 0 ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
              {formatCurrency(Math.abs(order.pnl))}
            </div>
          )}
        </div>

        {/* Timestamps */}
        <div className="flex items-center justify-between text-xs text-slate-400">
          <div>
            Placed: {order.placedAt.toLocaleString('en-IN', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            })}
          </div>
          {order.executedAt && (
            <div>
              Executed: {order.executedAt.toLocaleString('en-IN', {
                hour: '2-digit',
                minute: '2-digit'
              })}
            </div>
          )}
        </div>

        {/* Charges */}
        {(order.brokerage > 0 || order.taxes > 0) && (
          <div className="mt-2 pt-2 border-t border-slate-700/50 flex justify-between text-xs text-slate-400">
            <div>Brokerage: {formatCurrency(order.brokerage)}</div>
            <div>Taxes: {formatCurrency(order.taxes)}</div>
          </div>
        )}
      </motion.div>
    )
  }

  return (
    <div className={`glass-widget-card rounded-2xl ${className}`}>
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-white">Order History</h2>
          <div className="flex items-center space-x-2">
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="cyber-button-sm p-2 rounded-xl hover:scale-110 transition-all duration-300 disabled:opacity-50"
              title="Refresh"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
            
            <button
              className="cyber-button-sm p-2 rounded-xl hover:scale-110 transition-all duration-300"
              title="Download CSV"
            >
              <Download className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Summary Stats */}
        {showPnLSummary && !compactMode && (
          <div className="grid grid-cols-4 gap-4 mb-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-white">{summary.totalOrders}</div>
              <div className="text-xs text-slate-400">Total Orders</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-400">{summary.executedOrders}</div>
              <div className="text-xs text-slate-400">Executed</div>
            </div>
            <div className="text-center">
              <div className={`text-2xl font-bold ${summary.totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatCurrency(summary.totalPnL)}
              </div>
              <div className="text-xs text-slate-400">Net P&L</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-400">
                {summary.winRate.toFixed(1)}%
              </div>
              <div className="text-xs text-slate-400">Win Rate</div>
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="space-y-3">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by symbol, order ID, or broker..."
              className="w-full pl-10 pr-4 py-2 glass-input rounded-xl text-white placeholder-slate-400 text-sm"
            />
          </div>

          {/* Filter Row */}
          <div className="flex items-center space-x-3 overflow-x-auto">
            {/* Date Filter */}
            <select
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value as any)}
              className="glass-input rounded-lg px-3 py-2 text-sm text-white"
            >
              <option value="today">Today</option>
              <option value="week">This Week</option>
              <option value="month">This Month</option>
              <option value="all">All Time</option>
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              className="glass-input rounded-lg px-3 py-2 text-sm text-white"
            >
              <option value="all">All Status</option>
              <option value="executed">Executed</option>
              <option value="pending">Pending</option>
              <option value="partial">Partial</option>
              <option value="cancelled">Cancelled</option>
              <option value="rejected">Rejected</option>
            </select>

            {/* Side Filter */}
            <select
              value={sideFilter}
              onChange={(e) => setSideFilter(e.target.value as any)}
              className="glass-input rounded-lg px-3 py-2 text-sm text-white"
            >
              <option value="all">Buy & Sell</option>
              <option value="buy">Buy Only</option>
              <option value="sell">Sell Only</option>
            </select>

            {/* Broker Filter */}
            {showBrokerFilter && uniqueBrokers.length > 1 && (
              <select
                value={brokerFilter}
                onChange={(e) => setBrokerFilter(e.target.value)}
                className="glass-input rounded-lg px-3 py-2 text-sm text-white"
              >
                <option value="all">All Brokers</option>
                {uniqueBrokers.map(broker => (
                  <option key={broker} value={broker}>{broker}</option>
                ))}
              </select>
            )}
          </div>
        </div>
      </div>

      {/* Order List */}
      <div className="p-6">
        {filteredOrders.length === 0 ? (
          <div className="text-center py-12">
            <Clock className="w-12 h-12 text-slate-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-white mb-2">No Orders Found</h3>
            <p className="text-slate-400">
              {searchQuery || statusFilter !== 'all' || sideFilter !== 'all' || brokerFilter !== 'all'
                ? 'Try adjusting your filters to see more results.'
                : 'Your order history will appear here once you start trading.'
              }
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <AnimatePresence mode="popLayout">
              {filteredOrders.map((order) => (
                <OrderCard key={order.id} order={order} />
              ))}
            </AnimatePresence>
          </div>
        )}
      </div>
    </div>
  )
}

export { OrderHistory }
export default OrderHistory