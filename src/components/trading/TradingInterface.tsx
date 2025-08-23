import React, { useState } from 'react'
import { 
  TrendingUp, 
  TrendingDown,
  Zap,
  Clock,
  AlertTriangle,
  CheckCircle,
  DollarSign,
  Target,
  Shield,
  Activity,
  ArrowUpDown,
  Minus,
  Plus,
  Brain,
  Settings,
  BarChart3
} from 'lucide-react'
import { BrokerSelector } from './BrokerSelector'
import { CompactBrokerSelector } from './CompactBrokerSelector'
import { OrderBook } from './OrderBook'
import { AlgorithmicTradingPanel } from './AlgorithmicTradingPanel'
import { AdvancedOrderPanel } from './AdvancedOrderPanel'
import { useAuthStore } from '../../stores/auth.store'

interface OrderFormData {
  symbol: string
  orderType: 'market' | 'limit' | 'stop'
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  stopPrice?: number
  timeInForce: 'day' | 'gtc' | 'ioc'
}

interface Position {
  symbol: string
  quantity: number
  avgPrice: number
  currentPrice: number
  pnl: number
  pnlPercent: number
  side: 'long' | 'short'
}

interface Order {
  id: string
  symbol: string
  side: 'buy' | 'sell'
  quantity: number
  price: number
  status: 'pending' | 'filled' | 'cancelled' | 'rejected'
  timestamp: Date
  orderType: 'market' | 'limit' | 'stop'
}

const mockPositions: Position[] = [
  {
    symbol: 'RELIANCE',
    quantity: 10,
    avgPrice: 2520.50,
    currentPrice: 2547.30,
    pnl: 268.00,
    pnlPercent: 1.06,
    side: 'long'
  },
  {
    symbol: 'TCS',
    quantity: 5,
    avgPrice: 3680.20,
    currentPrice: 3642.80,
    pnl: -187.00,
    pnlPercent: -1.02,
    side: 'long'
  },
  {
    symbol: 'HDFC',
    quantity: 15,
    avgPrice: 1545.30,
    currentPrice: 1567.25,
    pnl: 329.25,
    pnlPercent: 1.42,
    side: 'long'
  }
]

const mockOrders: Order[] = [
  {
    id: '1',
    symbol: 'INFY',
    side: 'buy',
    quantity: 8,
    price: 1420.00,
    status: 'filled',
    timestamp: new Date(Date.now() - 30000),
    orderType: 'market'
  },
  {
    id: '2',
    symbol: 'ICICIBANK',
    side: 'sell',
    quantity: 12,
    price: 950.00,
    status: 'pending',
    timestamp: new Date(Date.now() - 120000),
    orderType: 'limit'
  },
  {
    id: '3',
    symbol: 'WIPRO',
    side: 'buy',
    quantity: 20,
    price: 445.50,
    status: 'cancelled',
    timestamp: new Date(Date.now() - 300000),
    orderType: 'limit'
  }
]

export function TradingInterface() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<'positions' | 'orders' | 'orderbook' | 'advanced' | 'algorithmic'>('positions')
  const [activeOrderPanel, setActiveOrderPanel] = useState<'buy' | 'sell'>('buy')
  const [availableBalance, setAvailableBalance] = useState(45230) // Default balance
  const [orderForm, setOrderForm] = useState<OrderFormData>({
    symbol: 'RELIANCE',
    orderType: 'market',
    side: 'buy',
    quantity: 1,
    timeInForce: 'day'
  })
  const [positions] = useState(mockPositions)
  const [orders] = useState(mockOrders)
  const [showOrderConfirmation, setShowOrderConfirmation] = useState(false)

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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Trading Interface</h1>
          <p className="text-slate-400">Execute trades and manage your positions</p>
        </div>
        <div className="flex items-center space-x-4">
          <div className="glass-card px-4 py-2 rounded-xl">
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              <span className="text-sm font-medium text-green-400">Live Trading</span>
            </div>
          </div>
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
  )
}