import React, { useState } from 'react';
import { TrendingUp, TrendingDown, Target, Zap, Clock, BookOpen, AlertTriangle, Settings, DollarSign, Activity } from 'lucide-react';

interface AdvancedOrderRequest {
  orderType: 'BRACKET' | 'OCO' | 'TRAILING_STOP' | 'ICEBERG' | 'TWAP';
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: number;
  
  // Bracket Order
  entryPrice?: number;
  takeProfitPrice?: number;
  stopLossPrice?: number;
  
  // OCO Order
  ocoOrders?: [OrderRequest, OrderRequest];
  
  // Trailing Stop
  trailAmount?: number;
  trailPercent?: number;
  
  // Iceberg Order
  displayQuantity?: number;
  
  // TWAP Order
  twapDuration?: number; // minutes
  twapSlices?: number;
  
  // Common fields
  timeInForce?: 'DAY' | 'GTC' | 'IOC' | 'FOK';
  notes?: string;
}

interface OrderRequest {
  orderType: 'MARKET' | 'LIMIT' | 'STOP' | 'STOP_LIMIT';
  price?: number;
  stopPrice?: number;
  quantity: number;
  timeInForce?: string;
}

interface SymbolSelectorProps {
  value: string;
  onChange: (symbol: string) => void;
  disabled?: boolean;
}

function SymbolSelector({ value, onChange, disabled }: SymbolSelectorProps) {
  const [searchTerm, setSearchTerm] = useState(value);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Mock symbols - in real app, this would come from API
  const mockSymbols = [
    'RELIANCE', 'TCS', 'INFY', 'HDFC', 'ITC', 'HINDUNILVR', 'SBIN', 'BHARTIARTL',
    'ASIANPAINT', 'MARUTI', 'KOTAKBANK', 'LT', 'HCLTECH', 'WIPRO', 'ULTRACEMCO'
  ];

  const handleInputChange = (input: string) => {
    setSearchTerm(input);
    if (input.length > 0) {
      const filtered = mockSymbols.filter(symbol => 
        symbol.toLowerCase().includes(input.toLowerCase())
      );
      setSuggestions(filtered.slice(0, 5));
      setShowSuggestions(true);
    } else {
      setShowSuggestions(false);
    }
  };

  const handleSymbolSelect = (symbol: string) => {
    setSearchTerm(symbol);
    onChange(symbol);
    setShowSuggestions(false);
  };

  return (
    <div className="relative">
      <input
        type="text"
        value={searchTerm}
        onChange={(e) => handleInputChange(e.target.value)}
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        disabled={disabled}
        className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50 disabled:opacity-50"
        placeholder="Search symbol..."
      />
      
      {showSuggestions && suggestions.length > 0 && (
        <div className="absolute z-10 w-full mt-1 bg-slate-800 border border-slate-700 rounded-lg shadow-lg">
          {suggestions.map(symbol => (
            <button
              key={symbol}
              onClick={() => handleSymbolSelect(symbol)}
              className="w-full px-4 py-2 text-left text-white hover:bg-slate-700 first:rounded-t-lg last:rounded-b-lg"
            >
              {symbol}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

interface BracketOrderConfigurationProps {
  config: AdvancedOrderRequest;
  onChange: (config: AdvancedOrderRequest) => void;
}

function BracketOrderConfiguration({ config, onChange }: BracketOrderConfigurationProps) {
  const handleChange = (field: keyof AdvancedOrderRequest, value: any) => {
    onChange({ ...config, [field]: value });
  };

  const calculateProfitLoss = () => {
    if (!config.entryPrice || !config.quantity) return null;
    
    const profit = config.takeProfitPrice 
      ? (config.takeProfitPrice - config.entryPrice) * config.quantity
      : 0;
      
    const loss = config.stopLossPrice 
      ? (config.entryPrice - config.stopLossPrice) * config.quantity
      : 0;
      
    return { profit, loss };
  };

  const pl = calculateProfitLoss();

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Entry Price (₹)
          </label>
          <input
            type="number"
            step="0.01"
            value={config.entryPrice || ''}
            onChange={(e) => handleChange('entryPrice', parseFloat(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Entry price"
          />
        </div>
        
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Take Profit (₹)
          </label>
          <input
            type="number"
            step="0.01"
            value={config.takeProfitPrice || ''}
            onChange={(e) => handleChange('takeProfitPrice', parseFloat(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Profit target"
          />
        </div>
        
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Stop Loss (₹)
          </label>
          <input
            type="number"
            step="0.01"
            value={config.stopLossPrice || ''}
            onChange={(e) => handleChange('stopLossPrice', parseFloat(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Stop loss"
          />
        </div>
      </div>

      {pl && (
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-4 h-4 text-green-400" />
              <span className="text-slate-300">Potential Profit:</span>
              <span className="text-green-400 font-medium">₹{pl.profit.toFixed(2)}</span>
            </div>
            <div className="flex items-center space-x-2">
              <TrendingDown className="w-4 h-4 text-red-400" />
              <span className="text-slate-300">Potential Loss:</span>
              <span className="text-red-400 font-medium">₹{pl.loss.toFixed(2)}</span>
            </div>
          </div>
          {pl.profit > 0 && pl.loss > 0 && (
            <div className="mt-2 text-xs text-slate-400">
              Risk/Reward Ratio: 1:{(pl.profit / pl.loss).toFixed(2)}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function OCOOrderConfiguration({ config, onChange }: BracketOrderConfigurationProps) {
  const [order1, setOrder1] = useState<OrderRequest>({
    orderType: 'LIMIT',
    price: 0,
    quantity: config.quantity || 0,
    timeInForce: 'DAY'
  });
  
  const [order2, setOrder2] = useState<OrderRequest>({
    orderType: 'STOP',
    stopPrice: 0,
    quantity: config.quantity || 0,
    timeInForce: 'DAY'
  });

  const handleConfigChange = () => {
    onChange({
      ...config,
      ocoOrders: [order1, order2]
    });
  };

  React.useEffect(() => {
    handleConfigChange();
  }, [order1, order2]);

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Order 1 */}
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <h5 className="text-white font-medium mb-3 flex items-center">
            <Target className="w-4 h-4 mr-2" />
            Order 1 (Primary)
          </h5>
          
          <div className="space-y-3">
            <div>
              <label className="block text-sm text-slate-300 mb-1">Order Type</label>
              <select
                value={order1.orderType}
                onChange={(e) => setOrder1(prev => ({ 
                  ...prev, 
                  orderType: e.target.value as any 
                }))}
                className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
              >
                <option value="LIMIT">Limit</option>
                <option value="MARKET">Market</option>
                <option value="STOP">Stop</option>
                <option value="STOP_LIMIT">Stop Limit</option>
              </select>
            </div>
            
            {(order1.orderType === 'LIMIT' || order1.orderType === 'STOP_LIMIT') && (
              <div>
                <label className="block text-sm text-slate-300 mb-1">Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={order1.price || ''}
                  onChange={(e) => setOrder1(prev => ({ 
                    ...prev, 
                    price: parseFloat(e.target.value) || 0 
                  }))}
                  className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
                />
              </div>
            )}
            
            {(order1.orderType === 'STOP' || order1.orderType === 'STOP_LIMIT') && (
              <div>
                <label className="block text-sm text-slate-300 mb-1">Stop Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={order1.stopPrice || ''}
                  onChange={(e) => setOrder1(prev => ({ 
                    ...prev, 
                    stopPrice: parseFloat(e.target.value) || 0 
                  }))}
                  className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
                />
              </div>
            )}
          </div>
        </div>

        {/* Order 2 */}
        <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
          <h5 className="text-white font-medium mb-3 flex items-center">
            <AlertTriangle className="w-4 h-4 mr-2" />
            Order 2 (Secondary)
          </h5>
          
          <div className="space-y-3">
            <div>
              <label className="block text-sm text-slate-300 mb-1">Order Type</label>
              <select
                value={order2.orderType}
                onChange={(e) => setOrder2(prev => ({ 
                  ...prev, 
                  orderType: e.target.value as any 
                }))}
                className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
              >
                <option value="STOP">Stop</option>
                <option value="LIMIT">Limit</option>
                <option value="MARKET">Market</option>
                <option value="STOP_LIMIT">Stop Limit</option>
              </select>
            </div>
            
            {(order2.orderType === 'LIMIT' || order2.orderType === 'STOP_LIMIT') && (
              <div>
                <label className="block text-sm text-slate-300 mb-1">Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={order2.price || ''}
                  onChange={(e) => setOrder2(prev => ({ 
                    ...prev, 
                    price: parseFloat(e.target.value) || 0 
                  }))}
                  className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
                />
              </div>
            )}
            
            {(order2.orderType === 'STOP' || order2.orderType === 'STOP_LIMIT') && (
              <div>
                <label className="block text-sm text-slate-300 mb-1">Stop Price (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  value={order2.stopPrice || ''}
                  onChange={(e) => setOrder2(prev => ({ 
                    ...prev, 
                    stopPrice: parseFloat(e.target.value) || 0 
                  }))}
                  className="w-full px-3 py-2 bg-slate-700/50 border border-slate-600/50 rounded text-white text-sm"
                />
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-3">
        <div className="flex items-start space-x-2">
          <AlertTriangle className="w-5 h-5 text-blue-400 mt-0.5" />
          <div className="text-sm">
            <div className="text-blue-400 font-medium">OCO Order Behavior</div>
            <div className="text-blue-300 mt-1">
              When one order fills, the other will be automatically cancelled. 
              This helps manage risk by setting both profit targets and stop losses.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TrailingStopConfiguration({ config, onChange }: BracketOrderConfigurationProps) {
  const handleChange = (field: keyof AdvancedOrderRequest, value: any) => {
    onChange({ ...config, [field]: value });
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Trail Amount (₹)
          </label>
          <input
            type="number"
            step="0.01"
            value={config.trailAmount || ''}
            onChange={(e) => handleChange('trailAmount', parseFloat(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Fixed amount to trail"
          />
          <div className="text-xs text-slate-400 mt-1">
            Stop will trail by this fixed amount
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Trail Percent (%)
          </label>
          <input
            type="number"
            step="0.1"
            value={config.trailPercent || ''}
            onChange={(e) => handleChange('trailPercent', parseFloat(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Percentage to trail"
          />
          <div className="text-xs text-slate-400 mt-1">
            Stop will trail by this percentage
          </div>
        </div>
      </div>

      <div className="bg-purple-500/10 border border-purple-500/20 rounded-lg p-4">
        <h5 className="text-white font-medium mb-2 flex items-center">
          <Activity className="w-4 h-4 mr-2" />
          Trailing Stop Example
        </h5>
        <div className="text-sm text-slate-300 space-y-1">
          <div>• Stock at ₹100, set 5% trailing stop</div>
          <div>• Initial stop: ₹95</div>
          <div>• Stock moves to ₹110, stop trails to ₹104.50</div>
          <div>• Stock moves to ₹105, stop remains at ₹104.50</div>
          <div>• If stock hits ₹104.50, position is sold</div>
        </div>
      </div>
    </div>
  );
}

function IcebergOrderConfiguration({ config, onChange }: BracketOrderConfigurationProps) {
  const handleChange = (field: keyof AdvancedOrderRequest, value: any) => {
    onChange({ ...config, [field]: value });
  };

  const hiddenQuantity = (config.quantity || 0) - (config.displayQuantity || 0);

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Display Quantity
          </label>
          <input
            type="number"
            min="1"
            max={config.quantity || 1}
            value={config.displayQuantity || ''}
            onChange={(e) => handleChange('displayQuantity', parseInt(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Visible quantity"
          />
          <div className="text-xs text-slate-400 mt-1">
            Amount visible in order book
          </div>
        </div>
        
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <div className="text-sm text-slate-300 space-y-2">
            <div className="flex justify-between">
              <span>Total Quantity:</span>
              <span className="text-white font-medium">{config.quantity || 0}</span>
            </div>
            <div className="flex justify-between">
              <span>Display Quantity:</span>
              <span className="text-white font-medium">{config.displayQuantity || 0}</span>
            </div>
            <div className="flex justify-between border-t border-slate-700 pt-2">
              <span>Hidden Quantity:</span>
              <span className="text-purple-400 font-medium">{hiddenQuantity > 0 ? hiddenQuantity : 0}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-4">
        <div className="flex items-start space-x-2">
          <Zap className="w-5 h-5 text-yellow-400 mt-0.5" />
          <div className="text-sm">
            <div className="text-yellow-400 font-medium">Iceberg Order Benefits</div>
            <div className="text-yellow-300 mt-1">
              Only shows small portions of large orders to avoid market impact. 
              Helps maintain anonymity and reduces slippage for institutional-size trades.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TWAPOrderConfiguration({ config, onChange }: BracketOrderConfigurationProps) {
  const handleChange = (field: keyof AdvancedOrderRequest, value: any) => {
    onChange({ ...config, [field]: value });
  };

  const avgSliceSize = config.twapSlices ? Math.floor((config.quantity || 0) / config.twapSlices) : 0;
  const sliceInterval = config.twapDuration && config.twapSlices ? 
    (config.twapDuration / config.twapSlices).toFixed(1) : 0;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Duration (minutes)
          </label>
          <input
            type="number"
            min="1"
            max="480"
            value={config.twapDuration || ''}
            onChange={(e) => handleChange('twapDuration', parseInt(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Total execution time"
          />
          <div className="text-xs text-slate-400 mt-1">
            Total time to execute order
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-slate-300 mb-2">
            Number of Slices
          </label>
          <input
            type="number"
            min="2"
            max="100"
            value={config.twapSlices || ''}
            onChange={(e) => handleChange('twapSlices', parseInt(e.target.value) || 0)}
            className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white"
            placeholder="Order slices"
          />
          <div className="text-xs text-slate-400 mt-1">
            Number of smaller orders
          </div>
        </div>
      </div>

      {avgSliceSize > 0 && sliceInterval > 0 && (
        <div className="bg-slate-800/50 rounded-lg p-4 border border-slate-700/50">
          <h5 className="text-white font-medium mb-3 flex items-center">
            <Clock className="w-4 h-4 mr-2" />
            TWAP Execution Plan
          </h5>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <div className="text-slate-400">Avg Slice Size</div>
              <div className="text-white font-medium">{avgSliceSize} shares</div>
            </div>
            <div>
              <div className="text-slate-400">Slice Interval</div>
              <div className="text-white font-medium">{sliceInterval} minutes</div>
            </div>
            <div>
              <div className="text-slate-400">Total Slices</div>
              <div className="text-white font-medium">{config.twapSlices}</div>
            </div>
          </div>
        </div>
      )}

      <div className="bg-green-500/10 border border-green-500/20 rounded-lg p-4">
        <div className="flex items-start space-x-2">
          <Clock className="w-5 h-5 text-green-400 mt-0.5" />
          <div className="text-sm">
            <div className="text-green-400 font-medium">TWAP Strategy</div>
            <div className="text-green-300 mt-1">
              Time-Weighted Average Price execution spreads your order over time to minimize market impact 
              and achieve an average price closer to the market benchmark.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function AdvancedOrderPreview({ orderRequest }: { orderRequest: AdvancedOrderRequest }) {
  const getOrderTypeDescription = () => {
    switch (orderRequest.orderType) {
      case 'BRACKET':
        return 'Entry order with profit target and stop loss';
      case 'OCO':
        return 'Two orders where filling one cancels the other';
      case 'TRAILING_STOP':
        return 'Stop loss that trails favorable price movement';
      case 'ICEBERG':
        return 'Large order hidden in smaller visible pieces';
      case 'TWAP':
        return 'Time-weighted execution across multiple slices';
      default:
        return '';
    }
  };

  const estimatedValue = () => {
    if (orderRequest.orderType === 'BRACKET' && orderRequest.entryPrice) {
      return orderRequest.entryPrice * orderRequest.quantity;
    }
    return 0;
  };

  return (
    <div className="glass-card rounded-xl p-6">
      <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
        <Settings className="w-5 h-5 mr-2" />
        Order Preview
      </h3>
      
      <div className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <div className="text-sm text-slate-400">Order Type</div>
            <div className="text-white font-medium">{orderRequest.orderType.replace('_', ' ')}</div>
            <div className="text-xs text-slate-500 mt-1">{getOrderTypeDescription()}</div>
          </div>
          
          <div>
            <div className="text-sm text-slate-400">Symbol & Side</div>
            <div className="text-white font-medium">
              {orderRequest.symbol} • {orderRequest.side}
            </div>
          </div>
          
          <div>
            <div className="text-sm text-slate-400">Quantity</div>
            <div className="text-white font-medium">{orderRequest.quantity.toLocaleString()}</div>
          </div>
          
          {estimatedValue() > 0 && (
            <div>
              <div className="text-sm text-slate-400">Estimated Value</div>
              <div className="text-white font-medium">₹{estimatedValue().toLocaleString()}</div>
            </div>
          )}
        </div>

        {/* Order Type Specific Details */}
        {orderRequest.orderType === 'BRACKET' && (
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <div className="text-slate-400">Entry Price</div>
                <div className="text-white font-medium">₹{orderRequest.entryPrice}</div>
              </div>
              <div>
                <div className="text-slate-400">Take Profit</div>
                <div className="text-green-400 font-medium">₹{orderRequest.takeProfitPrice}</div>
              </div>
              <div>
                <div className="text-slate-400">Stop Loss</div>
                <div className="text-red-400 font-medium">₹{orderRequest.stopLossPrice}</div>
              </div>
            </div>
          </div>
        )}

        {orderRequest.orderType === 'TRAILING_STOP' && (
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <div className="grid grid-cols-2 gap-4 text-sm">
              {orderRequest.trailAmount && (
                <div>
                  <div className="text-slate-400">Trail Amount</div>
                  <div className="text-white font-medium">₹{orderRequest.trailAmount}</div>
                </div>
              )}
              {orderRequest.trailPercent && (
                <div>
                  <div className="text-slate-400">Trail Percent</div>
                  <div className="text-white font-medium">{orderRequest.trailPercent}%</div>
                </div>
              )}
            </div>
          </div>
        )}

        {orderRequest.orderType === 'ICEBERG' && (
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div className="text-slate-400">Display Quantity</div>
                <div className="text-white font-medium">{orderRequest.displayQuantity}</div>
              </div>
              <div>
                <div className="text-slate-400">Hidden Quantity</div>
                <div className="text-purple-400 font-medium">
                  {orderRequest.quantity - (orderRequest.displayQuantity || 0)}
                </div>
              </div>
            </div>
          </div>
        )}

        {orderRequest.orderType === 'TWAP' && (
          <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <div className="text-slate-400">Duration</div>
                <div className="text-white font-medium">{orderRequest.twapDuration} minutes</div>
              </div>
              <div>
                <div className="text-slate-400">Slices</div>
                <div className="text-white font-medium">{orderRequest.twapSlices}</div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function OrderTypeDocumentation({ orderType }: { orderType: AdvancedOrderRequest['orderType'] }) {
  const getDocumentation = () => {
    switch (orderType) {
      case 'BRACKET':
        return {
          title: 'Bracket Orders',
          description: 'A bracket order combines an entry order with both profit-taking and stop-loss orders.',
          features: [
            'Automatically places profit and stop orders when entry fills',
            'Risk management built into the order structure',
            'Ideal for swing trading and position management',
            'Cancels remaining orders when one leg executes'
          ],
          risks: [
            'All three price levels must be realistic and achievable',
            'Market gaps can bypass stop loss levels',
            'Requires more margin than simple orders'
          ]
        };
        
      case 'OCO':
        return {
          title: 'One-Cancels-Other (OCO)',
          description: 'Two orders placed simultaneously where execution of one automatically cancels the other.',
          features: [
            'Allows setting both profit targets and stop losses',
            'Automatically cancels the unfilled order when one executes',
            'Useful for breakout and breakdown scenarios',
            'No need to manually cancel orders'
          ],
          risks: [
            'Both orders must be realistic for current market conditions',
            'Fast markets may execute both orders simultaneously',
            'Requires clear understanding of market direction bias'
          ]
        };
        
      case 'TRAILING_STOP':
        return {
          title: 'Trailing Stop Orders',
          description: 'Stop loss orders that automatically adjust in your favor as the stock price moves.',
          features: [
            'Automatically trails favorable price movements',
            'Locks in profits while limiting losses',
            'Can be set as fixed amount or percentage',
            'Ideal for trending markets'
          ],
          risks: [
            'May get stopped out in volatile markets',
            'Trail amount affects sensitivity to market noise',
            'Not suitable for choppy, sideways markets'
          ]
        };
        
      case 'ICEBERG':
        return {
          title: 'Iceberg Orders',
          description: 'Large orders that show only small portions to the market to minimize impact.',
          features: [
            'Hides large order size from other market participants',
            'Reduces market impact and slippage',
            'Automatically reveals new portions as previous ones fill',
            'Essential for institutional-size trading'
          ],
          risks: [
            'May take longer to execute completely',
            'Visible portion should be optimal size',
            'Market conditions may change during execution'
          ]
        };
        
      case 'TWAP':
        return {
          title: 'Time-Weighted Average Price (TWAP)',
          description: 'Executes large orders by breaking them into smaller pieces over a specified time period.',
          features: [
            'Spreads execution over time to reduce market impact',
            'Aims to achieve average price close to time-weighted benchmark',
            'Automatically manages timing and sizing of child orders',
            'Ideal for large institutional orders'
          ],
          risks: [
            'May miss favorable price movements waiting for next slice',
            'Execution time increases with more slices',
            'Market conditions may deteriorate during execution period'
          ]
        };
        
      default:
        return null;
    }
  };

  const doc = getDocumentation();
  if (!doc) return null;

  return (
    <div className="glass-card rounded-xl p-6">
      <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
        <BookOpen className="w-5 h-5 mr-2" />
        {doc.title}
      </h3>
      
      <div className="space-y-4">
        <p className="text-slate-300">{doc.description}</p>
        
        <div>
          <h4 className="text-white font-medium mb-2">Key Features</h4>
          <ul className="space-y-1">
            {doc.features.map((feature, index) => (
              <li key={index} className="text-sm text-slate-300 flex items-start">
                <span className="text-green-400 mr-2">•</span>
                {feature}
              </li>
            ))}
          </ul>
        </div>
        
        <div>
          <h4 className="text-white font-medium mb-2">Risk Considerations</h4>
          <ul className="space-y-1">
            {doc.risks.map((risk, index) => (
              <li key={index} className="text-sm text-yellow-300 flex items-start">
                <span className="text-yellow-400 mr-2">⚠</span>
                {risk}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

export function AdvancedOrderTypes() {
  const [orderRequest, setOrderRequest] = useState<AdvancedOrderRequest>({
    orderType: 'BRACKET',
    symbol: '',
    side: 'BUY',
    quantity: 0,
    timeInForce: 'DAY'
  });
  const [placing, setPlacing] = useState(false);

  const placeAdvancedOrder = async () => {
    if (!orderRequest.symbol || orderRequest.quantity <= 0) {
      return;
    }

    setPlacing(true);
    try {
      // Simulate API call - replace with actual service call
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      console.log('Advanced order placed:', orderRequest);
      // Show success message and reset form
      setOrderRequest({
        orderType: 'BRACKET',
        symbol: '',
        side: 'BUY',
        quantity: 0,
        timeInForce: 'DAY'
      });
    } catch (error) {
      console.error('Failed to place advanced order:', error);
    } finally {
      setPlacing(false);
    }
  };

  const isValid = () => {
    if (!orderRequest.symbol || orderRequest.quantity <= 0) return false;
    
    switch (orderRequest.orderType) {
      case 'BRACKET':
        return orderRequest.entryPrice && orderRequest.takeProfitPrice && orderRequest.stopLossPrice;
      case 'TRAILING_STOP':
        return orderRequest.trailAmount || orderRequest.trailPercent;
      case 'ICEBERG':
        return orderRequest.displayQuantity && orderRequest.displayQuantity < orderRequest.quantity;
      case 'TWAP':
        return orderRequest.twapDuration && orderRequest.twapSlices;
      case 'OCO':
        return orderRequest.ocoOrders && orderRequest.ocoOrders.length === 2;
      default:
        return true;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white">Advanced Order Types</h1>
        <p className="text-slate-400 mt-1">Professional trading with sophisticated order types</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Order Configuration */}
        <div className="lg:col-span-2 space-y-6">
          {/* Order Type Selector */}
          <div className="glass-card rounded-xl p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Select Order Type</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-6">
              {(['BRACKET', 'OCO', 'TRAILING_STOP', 'ICEBERG', 'TWAP'] as const).map((type) => (
                <button
                  key={type}
                  onClick={() => setOrderRequest(prev => ({ ...prev, orderType: type }))}
                  className={`p-4 rounded-xl text-sm font-medium transition-all border ${
                    orderRequest.orderType === type
                      ? 'bg-purple-500/20 text-purple-400 border-purple-500/50'
                      : 'bg-slate-700/50 text-slate-400 hover:text-white border-slate-600/50 hover:border-slate-500/50'
                  }`}
                >
                  <div className="mb-1">{type.replace('_', ' ')}</div>
                  <div className="text-xs opacity-75">
                    {type === 'BRACKET' && 'Entry + P&L'}
                    {type === 'OCO' && 'One Cancels Other'}
                    {type === 'TRAILING_STOP' && 'Trailing Stop'}
                    {type === 'ICEBERG' && 'Hidden Size'}
                    {type === 'TWAP' && 'Time Average'}
                  </div>
                </button>
              ))}
            </div>

            {/* Basic Order Fields */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
                <SymbolSelector
                  value={orderRequest.symbol}
                  onChange={(symbol) => setOrderRequest(prev => ({ ...prev, symbol }))}
                  disabled={placing}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Side</label>
                <select
                  value={orderRequest.side}
                  onChange={(e) => setOrderRequest(prev => ({ ...prev, side: e.target.value as 'BUY' | 'SELL' }))}
                  disabled={placing}
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50"
                >
                  <option value="BUY">Buy</option>
                  <option value="SELL">Sell</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
                <input
                  type="number"
                  min="1"
                  value={orderRequest.quantity || ''}
                  onChange={(e) => setOrderRequest(prev => ({ ...prev, quantity: parseInt(e.target.value) || 0 }))}
                  disabled={placing}
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/50"
                  placeholder="Order quantity"
                />
              </div>
            </div>

            {/* Advanced Order Configuration */}
            {orderRequest.orderType === 'BRACKET' && (
              <BracketOrderConfiguration
                config={orderRequest}
                onChange={setOrderRequest}
              />
            )}
            
            {orderRequest.orderType === 'OCO' && (
              <OCOOrderConfiguration
                config={orderRequest}
                onChange={setOrderRequest}
              />
            )}
            
            {orderRequest.orderType === 'TRAILING_STOP' && (
              <TrailingStopConfiguration
                config={orderRequest}
                onChange={setOrderRequest}
              />
            )}
            
            {orderRequest.orderType === 'ICEBERG' && (
              <IcebergOrderConfiguration
                config={orderRequest}
                onChange={setOrderRequest}
              />
            )}
            
            {orderRequest.orderType === 'TWAP' && (
              <TWAPOrderConfiguration
                config={orderRequest}
                onChange={setOrderRequest}
              />
            )}

            {/* Place Order Button */}
            <button
              onClick={placeAdvancedOrder}
              disabled={!isValid() || placing}
              className="w-full mt-6 px-6 py-4 bg-purple-500 hover:bg-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center space-x-2"
            >
              {placing ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Placing Order...</span>
                </>
              ) : (
                <>
                  <DollarSign className="w-5 h-5" />
                  <span>Place {orderRequest.orderType.replace('_', ' ')} Order</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Order Preview & Documentation */}
        <div className="space-y-6">
          {/* Order Preview */}
          <AdvancedOrderPreview orderRequest={orderRequest} />
          
          {/* Order Type Documentation */}
          <OrderTypeDocumentation orderType={orderRequest.orderType} />
        </div>
      </div>
    </div>
  );
}

export default AdvancedOrderTypes;