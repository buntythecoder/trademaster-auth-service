import React, { useState } from 'react'
import { 
  Target,
  Clock,
  Shield,
  Zap,
  TrendingUp,
  TrendingDown,
  Settings,
  AlertTriangle,
  CheckCircle,
  Activity,
  BarChart3,
  Calendar,
  Plus,
  Minus
} from 'lucide-react'

interface AdvancedOrderData {
  symbol: string
  orderType: 'market' | 'limit' | 'stop_loss' | 'take_profit' | 'trailing_stop' | 'bracket' | 'iceberg' | 'twap' | 'vwap'
  side: 'buy' | 'sell'
  quantity: number
  price?: number
  stopPrice?: number
  takeProfitPrice?: number
  trailingAmount?: number
  trailingPercent?: number
  timeInForce: 'day' | 'gtc' | 'ioc' | 'fok'
  executionAlgorithm?: 'twap' | 'vwap' | 'iceberg' | 'pov'
  algorithmParameters?: {
    duration?: number  // minutes
    participation_rate?: number  // % for POV
    slice_size?: number  // for iceberg
    price_improvement?: boolean
  }
}

interface ConditionalOrder {
  id: string
  condition: 'price' | 'time' | 'volume' | 'technical'
  conditionValue: string
  triggerOrder: AdvancedOrderData
  status: 'active' | 'triggered' | 'cancelled'
}

const orderTypeDescriptions = {
  market: 'Execute immediately at current market price',
  limit: 'Execute at specific price or better',
  stop_loss: 'Sell when price falls below stop price',
  take_profit: 'Sell when price reaches target price',
  trailing_stop: 'Stop loss that follows price movement',
  bracket: 'Buy/Sell with automatic stop loss and take profit',
  iceberg: 'Large order broken into smaller visible portions',
  twap: 'Time-Weighted Average Price execution',
  vwap: 'Volume-Weighted Average Price execution'
}

export function AdvancedOrderPanel() {
  const [orderData, setOrderData] = useState<AdvancedOrderData>({
    symbol: 'RELIANCE',
    orderType: 'limit',
    side: 'buy',
    quantity: 10,
    price: 2547.30,
    timeInForce: 'day'
  })
  
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false)
  const [conditionalOrders, setConditionalOrders] = useState<ConditionalOrder[]>([])
  const [activeTab, setActiveTab] = useState<'order' | 'conditional' | 'algorithms'>('order')

  const calculateOrderValue = () => {
    const price = orderData.price || 2547.30
    return orderData.quantity * price
  }

  const handleOrderSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    alert('Advanced order placed successfully! Order routing to optimal execution venue...')
  }

  const addConditionalOrder = () => {
    const newOrder: ConditionalOrder = {
      id: Date.now().toString(),
      condition: 'price',
      conditionValue: '2600',
      triggerOrder: { ...orderData },
      status: 'active'
    }
    setConditionalOrders(prev => [...prev, newOrder])
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold gradient-text">Advanced Order Management</h2>
            <p className="text-slate-400">Sophisticated order types and execution algorithms</p>
          </div>
          <button
            onClick={() => setShowAdvancedOptions(!showAdvancedOptions)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
              showAdvancedOptions 
                ? 'bg-purple-500/20 text-purple-400' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Settings className="w-4 h-4" />
            <span>Advanced</span>
          </button>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3">
              <Target className="h-5 w-5 text-purple-400" />
              <div>
                <div className="text-sm text-slate-400">Available Orders</div>
                <div className="text-lg font-bold text-white">9</div>
              </div>
            </div>
          </div>
          
          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3">
              <Clock className="h-5 w-5 text-cyan-400" />
              <div>
                <div className="text-sm text-slate-400">Pending Orders</div>
                <div className="text-lg font-bold text-white">3</div>
              </div>
            </div>
          </div>

          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3">
              <Activity className="h-5 w-5 text-green-400" />
              <div>
                <div className="text-sm text-slate-400">Conditional</div>
                <div className="text-lg font-bold text-white">{conditionalOrders.length}</div>
              </div>
            </div>
          </div>

          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="flex items-center space-x-3">
              <Zap className="h-5 w-5 text-orange-400" />
              <div>
                <div className="text-sm text-slate-400">Algo Orders</div>
                <div className="text-lg font-bold text-white">2</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex items-center space-x-6 mb-6">
        {[
          { key: 'order', label: 'Order Entry', icon: Target },
          { key: 'conditional', label: 'Conditional Orders', icon: Clock },
          { key: 'algorithms', label: 'Execution Algorithms', icon: Zap },
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

      {/* Order Entry Tab */}
      {activeTab === 'order' && (
        <div className="glass-card rounded-2xl p-6">
          <form onSubmit={handleOrderSubmit} className="space-y-6">
            <div className="grid gap-6 md:grid-cols-2">
              {/* Basic Order Details */}
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-white">Order Details</h3>
                
                <div className="grid gap-4 md:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
                    <select
                      value={orderData.symbol}
                      onChange={(e) => setOrderData(prev => ({ ...prev, symbol: e.target.value }))}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                    >
                      <option value="RELIANCE">RELIANCE</option>
                      <option value="TCS">TCS</option>
                      <option value="HDFC">HDFC</option>
                      <option value="INFY">INFY</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Side</label>
                    <div className="flex space-x-2">
                      <button
                        type="button"
                        onClick={() => setOrderData(prev => ({ ...prev, side: 'buy' }))}
                        className={`flex-1 py-3 rounded-xl font-semibold transition-all ${
                          orderData.side === 'buy'
                            ? 'bg-green-500/20 text-green-400 border border-green-500/50'
                            : 'bg-slate-700/50 text-slate-400'
                        }`}
                      >
                        BUY
                      </button>
                      <button
                        type="button"
                        onClick={() => setOrderData(prev => ({ ...prev, side: 'sell' }))}
                        className={`flex-1 py-3 rounded-xl font-semibold transition-all ${
                          orderData.side === 'sell'
                            ? 'bg-red-500/20 text-red-400 border border-red-500/50'
                            : 'bg-slate-700/50 text-slate-400'
                        }`}
                      >
                        SELL
                      </button>
                    </div>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Order Type</label>
                  <select
                    value={orderData.orderType}
                    onChange={(e) => setOrderData(prev => ({ ...prev, orderType: e.target.value as any }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  >
                    {Object.entries(orderTypeDescriptions).map(([type, description]) => (
                      <option key={type} value={type}>
                        {type.replace('_', ' ').toUpperCase()} - {description}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
                  <div className="flex items-center space-x-2">
                    <button
                      type="button"
                      onClick={() => setOrderData(prev => ({ ...prev, quantity: Math.max(1, prev.quantity - 10) }))}
                      className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <input
                      type="number"
                      min="1"
                      value={orderData.quantity}
                      onChange={(e) => setOrderData(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
                      className="cyber-input flex-1 text-center py-3 rounded-xl text-white"
                    />
                    <button
                      type="button"
                      onClick={() => setOrderData(prev => ({ ...prev, quantity: prev.quantity + 10 }))}
                      className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white transition-colors"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {orderData.orderType !== 'market' && (
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Price</label>
                    <input
                      type="number"
                      step="0.01"
                      value={orderData.price || ''}
                      onChange={(e) => setOrderData(prev => ({ ...prev, price: parseFloat(e.target.value) }))}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                      placeholder="Enter price"
                    />
                  </div>
                )}
              </div>

              {/* Advanced Order Parameters */}
              <div className="space-y-4">
                <h3 className="text-lg font-bold text-white">Advanced Parameters</h3>
                
                {(orderData.orderType === 'stop_loss' || orderData.orderType === 'bracket') && (
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Stop Price</label>
                    <input
                      type="number"
                      step="0.01"
                      value={orderData.stopPrice || ''}
                      onChange={(e) => setOrderData(prev => ({ ...prev, stopPrice: parseFloat(e.target.value) }))}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                      placeholder="Enter stop price"
                    />
                  </div>
                )}

                {(orderData.orderType === 'take_profit' || orderData.orderType === 'bracket') && (
                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Take Profit Price</label>
                    <input
                      type="number"
                      step="0.01"
                      value={orderData.takeProfitPrice || ''}
                      onChange={(e) => setOrderData(prev => ({ ...prev, takeProfitPrice: parseFloat(e.target.value) }))}
                      className="cyber-input w-full py-3 rounded-xl text-white"
                      placeholder="Enter take profit price"
                    />
                  </div>
                )}

                {orderData.orderType === 'trailing_stop' && (
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Trail Amount (₹)</label>
                      <input
                        type="number"
                        step="0.01"
                        value={orderData.trailingAmount || ''}
                        onChange={(e) => setOrderData(prev => ({ ...prev, trailingAmount: parseFloat(e.target.value) }))}
                        className="cyber-input w-full py-3 rounded-xl text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Trail Percent (%)</label>
                      <input
                        type="number"
                        step="0.1"
                        value={orderData.trailingPercent || ''}
                        onChange={(e) => setOrderData(prev => ({ ...prev, trailingPercent: parseFloat(e.target.value) }))}
                        className="cyber-input w-full py-3 rounded-xl text-white"
                      />
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Time in Force</label>
                  <select
                    value={orderData.timeInForce}
                    onChange={(e) => setOrderData(prev => ({ ...prev, timeInForce: e.target.value as any }))}
                    className="cyber-input w-full py-3 rounded-xl text-white"
                  >
                    <option value="day">Day (Good for Day)</option>
                    <option value="gtc">GTC (Good Till Cancelled)</option>
                    <option value="ioc">IOC (Immediate or Cancel)</option>
                    <option value="fok">FOK (Fill or Kill)</option>
                  </select>
                </div>

                {showAdvancedOptions && (
                  <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                    <h4 className="font-semibold text-white mb-3">Execution Algorithm</h4>
                    <select
                      value={orderData.executionAlgorithm || ''}
                      onChange={(e) => setOrderData(prev => ({ ...prev, executionAlgorithm: e.target.value as any }))}
                      className="cyber-input w-full py-2 rounded-xl text-white mb-3"
                    >
                      <option value="">Standard Execution</option>
                      <option value="twap">TWAP (Time-Weighted Average Price)</option>
                      <option value="vwap">VWAP (Volume-Weighted Average Price)</option>
                      <option value="iceberg">Iceberg (Hidden Quantity)</option>
                      <option value="pov">POV (Percent of Volume)</option>
                    </select>
                    
                    {orderData.executionAlgorithm && (
                      <div className="grid gap-3 md:grid-cols-2">
                        <div>
                          <label className="block text-xs text-slate-400 mb-1">Duration (min)</label>
                          <input
                            type="number"
                            placeholder="60"
                            className="cyber-input w-full py-2 text-sm rounded-xl text-white"
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-slate-400 mb-1">Participation %</label>
                          <input
                            type="number"
                            step="0.1"
                            placeholder="10.0"
                            className="cyber-input w-full py-2 text-sm rounded-xl text-white"
                          />
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Order Summary */}
            <div className="glass-card p-4 rounded-xl bg-slate-800/30">
              <h4 className="font-semibold text-white mb-3">Order Summary</h4>
              <div className="grid gap-3 md:grid-cols-4 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-400">Order Value:</span>
                  <span className="text-white font-semibold">₹{calculateOrderValue().toLocaleString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Estimated Fee:</span>
                  <span className="text-cyan-400">₹{(calculateOrderValue() * 0.001).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Available:</span>
                  <span className="text-green-400">₹45,230</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">After Order:</span>
                  <span className="text-purple-400">₹{(45230 - calculateOrderValue()).toLocaleString()}</span>
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className={`w-full py-4 rounded-2xl font-semibold text-lg transition-all ${
                orderData.side === 'buy'
                  ? 'bg-gradient-to-r from-green-500 to-green-600 hover:from-green-400 hover:to-green-500 text-white'
                  : 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-400 hover:to-red-500 text-white'
              }`}
            >
              Place {orderData.orderType.replace('_', ' ').toUpperCase()} Order
            </button>
          </form>
        </div>
      )}

      {/* Conditional Orders Tab */}
      {activeTab === 'conditional' && (
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-white">Conditional Orders</h3>
              <button
                onClick={addConditionalOrder}
                className="cyber-button px-4 py-2 rounded-xl"
              >
                <Plus className="w-4 h-4 mr-2" />
                Add Condition
              </button>
            </div>

            <div className="space-y-4">
              {conditionalOrders.length === 0 ? (
                <div className="text-center py-12">
                  <Clock className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                  <h4 className="text-lg font-semibold text-white mb-2">No Conditional Orders</h4>
                  <p className="text-slate-400">Create orders that trigger based on market conditions</p>
                </div>
              ) : (
                conditionalOrders.map((order) => (
                  <div key={order.id} className="glass-card p-4 rounded-xl bg-slate-800/30">
                    <div className="flex items-center justify-between mb-3">
                      <div>
                        <h4 className="font-semibold text-white">
                          {order.triggerOrder.side.toUpperCase()} {order.triggerOrder.symbol}
                        </h4>
                        <div className="text-sm text-slate-400">
                          When price {order.condition === 'price' ? 'reaches' : ''} ₹{order.conditionValue}
                        </div>
                      </div>
                      <div className={`px-3 py-1 rounded-xl text-xs font-semibold ${
                        order.status === 'active' ? 'bg-green-500/20 text-green-400' :
                        order.status === 'triggered' ? 'bg-purple-500/20 text-purple-400' :
                        'bg-gray-500/20 text-gray-400'
                      }`}>
                        {order.status}
                      </div>
                    </div>
                    <div className="text-sm text-slate-300">
                      Order: {order.triggerOrder.quantity} shares @ ₹{order.triggerOrder.price}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Add New Conditional Order */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-4">Create Conditional Order</h3>
            <div className="grid gap-4 md:grid-cols-3">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Condition Type</label>
                <select className="cyber-input w-full py-3 rounded-xl text-white">
                  <option>Price crosses above</option>
                  <option>Price crosses below</option>
                  <option>Volume exceeds</option>
                  <option>RSI above/below</option>
                  <option>Time-based trigger</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Trigger Value</label>
                <input
                  type="text"
                  placeholder="2600"
                  className="cyber-input w-full py-3 rounded-xl text-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Valid Until</label>
                <select className="cyber-input w-full py-3 rounded-xl text-white">
                  <option>End of day</option>
                  <option>End of week</option>
                  <option>End of month</option>
                  <option>Good till cancelled</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Execution Algorithms Tab */}
      {activeTab === 'algorithms' && (
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Execution Algorithms</h3>
            
            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-4">
                <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                  <div className="flex items-center space-x-3 mb-3">
                    <Clock className="h-6 w-6 text-purple-400" />
                    <div>
                      <h4 className="font-semibold text-white">TWAP</h4>
                      <div className="text-sm text-slate-400">Time-Weighted Average Price</div>
                    </div>
                  </div>
                  <p className="text-sm text-slate-300 mb-3">
                    Spreads order execution evenly over specified time period to minimize market impact
                  </p>
                  <div className="text-xs text-slate-400">
                    • Best for: Large orders, illiquid stocks<br/>
                    • Duration: 15 min - 4 hours<br/>
                    • Market Impact: Low
                  </div>
                </div>

                <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                  <div className="flex items-center space-x-3 mb-3">
                    <BarChart3 className="h-6 w-6 text-cyan-400" />
                    <div>
                      <h4 className="font-semibold text-white">VWAP</h4>
                      <div className="text-sm text-slate-400">Volume-Weighted Average Price</div>
                    </div>
                  </div>
                  <p className="text-sm text-slate-300 mb-3">
                    Matches execution to historical volume patterns for optimal price execution
                  </p>
                  <div className="text-xs text-slate-400">
                    • Best for: Following market rhythm<br/>
                    • Execution: Volume-based slicing<br/>
                    • Benchmark: VWAP tracking
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                  <div className="flex items-center space-x-3 mb-3">
                    <Shield className="h-6 w-6 text-green-400" />
                    <div>
                      <h4 className="font-semibold text-white">Iceberg</h4>
                      <div className="text-sm text-slate-400">Hidden Quantity</div>
                    </div>
                  </div>
                  <p className="text-sm text-slate-300 mb-3">
                    Shows small portions of large orders to minimize market impact and information leakage
                  </p>
                  <div className="text-xs text-slate-400">
                    • Best for: Large block orders<br/>
                    • Visible: 5-10% of total size<br/>
                    • Strategy: Stealth execution
                  </div>
                </div>

                <div className="glass-card p-4 rounded-xl bg-slate-800/30">
                  <div className="flex items-center space-x-3 mb-3">
                    <Activity className="h-6 w-6 text-orange-400" />
                    <div>
                      <h4 className="font-semibold text-white">POV</h4>
                      <div className="text-sm text-slate-400">Percent of Volume</div>
                    </div>
                  </div>
                  <p className="text-sm text-slate-300 mb-3">
                    Maintains consistent participation rate in market volume for controlled execution
                  </p>
                  <div className="text-xs text-slate-400">
                    • Best for: Liquidity following<br/>
                    • Rate: 5-30% of volume<br/>
                    • Flexibility: Volume adaptive
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-6 p-4 rounded-xl bg-gradient-to-r from-purple-500/10 to-cyan-500/10 border border-purple-500/20">
              <div className="flex items-center space-x-3 mb-2">
                <Zap className="h-5 w-5 text-purple-400" />
                <h4 className="font-semibold text-white">Smart Order Routing (SOR)</h4>
              </div>
              <p className="text-sm text-slate-300 mb-3">
                Automatically routes orders across multiple exchanges and dark pools for best execution
              </p>
              <div className="grid gap-3 md:grid-cols-3 text-xs">
                <div className="text-slate-400">
                  ✓ Real-time venue analysis<br/>
                  ✓ Price improvement seeking<br/>
                  ✓ Latency optimization
                </div>
                <div className="text-slate-400">
                  ✓ Dark pool access<br/>
                  ✓ Rebate capture<br/>
                  ✓ Market impact reduction
                </div>
                <div className="text-slate-400">
                  ✓ Regulatory compliance<br/>
                  ✓ Best execution reporting<br/>
                  ✓ Cost transparency
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}