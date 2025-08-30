import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence, PanInfo } from 'framer-motion';
import { 
  TrendingUp, 
  TrendingDown, 
  Menu, 
  X, 
  Search, 
  Bell,
  Settings,
  MoreHorizontal,
  ArrowUpDown,
  Zap,
  Target,
  DollarSign,
  Percent,
  Clock,
  Shield,
  Eye,
  EyeOff,
  ChevronUp,
  ChevronDown,
  RefreshCw,
  Maximize2,
  Minimize2,
  Activity,
  BarChart3,
  PieChart
} from 'lucide-react';

interface TouchGesture {
  startX: number;
  startY: number;
  currentX: number;
  currentY: number;
  deltaX: number;
  deltaY: number;
  distance: number;
  direction: 'up' | 'down' | 'left' | 'right' | 'none';
}

interface MobileQuickOrder {
  symbol: string;
  side: 'buy' | 'sell';
  quantity: number;
  orderType: 'market' | 'limit' | 'stop';
  price?: number;
  stopPrice?: number;
  timeInForce: 'GTC' | 'IOC' | 'FOK' | 'DAY';
}

interface Position {
  symbol: string;
  quantity: number;
  avgPrice: number;
  marketPrice: number;
  unrealizedPnL: number;
  unrealizedPnLPercent: number;
  side: 'long' | 'short';
}

interface Order {
  id: string;
  symbol: string;
  side: 'buy' | 'sell';
  quantity: number;
  filledQuantity: number;
  price: number;
  orderType: 'market' | 'limit' | 'stop';
  status: 'pending' | 'filled' | 'cancelled' | 'partial';
  timestamp: Date;
}

const MobileTradingInterface: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'trade' | 'positions' | 'orders' | 'watchlist'>('trade');
  const [showSidebar, setShowSidebar] = useState(false);
  const [selectedSymbol, setSelectedSymbol] = useState('AAPL');
  const [quickOrder, setQuickOrder] = useState<MobileQuickOrder>({
    symbol: 'AAPL',
    side: 'buy',
    quantity: 100,
    orderType: 'market',
    timeInForce: 'GTC'
  });
  const [showAdvancedOrder, setShowAdvancedOrder] = useState(false);
  const [touchPosition, setTouchPosition] = useState<{ x: number; y: number } | null>(null);
  const [isPortrait, setIsPortrait] = useState(window.innerHeight > window.innerWidth);
  const [showQuickActions, setShowQuickActions] = useState(false);
  const [swipeThreshold] = useState(50);
  const [chartTimeframe, setChartTimeframe] = useState('1D');
  const [hideBalances, setHideBalances] = useState(false);
  
  const containerRef = useRef<HTMLDivElement>(null);
  const lastTapTime = useRef<number>(0);

  // Mock data
  const positions: Position[] = [
    {
      symbol: 'AAPL',
      quantity: 200,
      avgPrice: 150.25,
      marketPrice: 152.80,
      unrealizedPnL: 510.00,
      unrealizedPnLPercent: 1.70,
      side: 'long'
    },
    {
      symbol: 'GOOGL',
      quantity: -50,
      avgPrice: 2750.00,
      marketPrice: 2720.30,
      unrealizedPnL: 1485.00,
      unrealizedPnLPercent: 1.08,
      side: 'short'
    }
  ];

  const orders: Order[] = [
    {
      id: '1',
      symbol: 'TSLA',
      side: 'buy',
      quantity: 100,
      filledQuantity: 0,
      price: 245.00,
      orderType: 'limit',
      status: 'pending',
      timestamp: new Date()
    }
  ];

  const watchlist = [
    { symbol: 'AAPL', price: 152.80, change: 2.55, changePercent: 1.70 },
    { symbol: 'GOOGL', price: 2720.30, change: -29.70, changePercent: -1.08 },
    { symbol: 'TSLA', price: 248.50, change: 5.20, changePercent: 2.13 },
    { symbol: 'MSFT', price: 378.90, change: -1.45, changePercent: -0.38 }
  ];

  // Handle orientation change
  useEffect(() => {
    const handleOrientationChange = () => {
      setIsPortrait(window.innerHeight > window.innerWidth);
    };
    
    window.addEventListener('resize', handleOrientationChange);
    return () => window.removeEventListener('resize', handleOrientationChange);
  }, []);

  // Handle touch gestures
  const handleTouchStart = useCallback((event: React.TouchEvent) => {
    const touch = event.touches[0];
    setTouchPosition({ x: touch.clientX, y: touch.clientY });
  }, []);

  const handleTouchEnd = useCallback((event: React.TouchEvent) => {
    setTouchPosition(null);
  }, []);

  const handleSwipe = useCallback((event: any, info: PanInfo) => {
    const { offset, velocity } = info;
    
    // Horizontal swipe for tab switching
    if (Math.abs(offset.x) > Math.abs(offset.y) && Math.abs(offset.x) > swipeThreshold) {
      const tabs = ['trade', 'positions', 'orders', 'watchlist'];
      const currentIndex = tabs.indexOf(activeTab);
      
      if (offset.x > 0 && currentIndex > 0) {
        setActiveTab(tabs[currentIndex - 1] as any);
      } else if (offset.x < 0 && currentIndex < tabs.length - 1) {
        setActiveTab(tabs[currentIndex + 1] as any);
      }
    }
    
    // Vertical swipe for quick actions
    if (Math.abs(offset.y) > Math.abs(offset.x) && offset.y < -swipeThreshold) {
      setShowQuickActions(true);
    }
  }, [activeTab, swipeThreshold]);

  const handleDoubleTap = useCallback((callback: () => void) => {
    const now = Date.now();
    if (now - lastTapTime.current < 300) {
      callback();
    }
    lastTapTime.current = now;
  }, []);

  const executeQuickOrder = () => {
    console.log('Executing mobile quick order:', quickOrder);
    // Mock order execution with haptic feedback
    if ('vibrate' in navigator) {
      navigator.vibrate(100);
    }
  };

  const formatCurrency = (value: number, hideValue = false) => {
    if (hideValue) return '••••••';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(value);
  };

  const formatPercent = (value: number, hideValue = false) => {
    if (hideValue) return '•••%';
    return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
  };

  const TradeTab = () => (
    <div className="p-4 space-y-4">
      {/* Symbol Search */}
      <div className="relative">
        <Search className="absolute left-3 top-3 text-gray-400" size={20} />
        <input
          type="text"
          placeholder="Search symbols..."
          value={selectedSymbol}
          onChange={(e) => setSelectedSymbol(e.target.value)}
          className="w-full pl-11 pr-4 py-3 bg-gray-800 border border-gray-600 rounded-xl text-lg"
        />
      </div>

      {/* Price Display */}
      <motion.div 
        className="bg-gradient-to-r from-blue-900/30 to-purple-900/30 rounded-xl p-4 border border-blue-500/30"
        whileHover={{ scale: 1.01 }}
        onDoubleClick={() => handleDoubleTap(() => console.log('Double tap on price'))}
      >
        <div className="flex items-center justify-between mb-2">
          <span className="text-2xl font-bold">{selectedSymbol}</span>
          <div className="flex items-center space-x-2">
            <button onClick={() => setHideBalances(!hideBalances)} className="p-1">
              {hideBalances ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            <RefreshCw size={16} className="text-gray-400" />
          </div>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-3xl font-bold text-green-400">
            {formatCurrency(152.80, hideBalances)}
          </span>
          <div className="text-right">
            <div className="text-green-400 font-medium">+$2.55</div>
            <div className="text-green-400 text-sm">+1.70%</div>
          </div>
        </div>
      </motion.div>

      {/* Chart Timeframe Selector */}
      <div className="flex space-x-2 overflow-x-auto pb-2">
        {['1m', '5m', '15m', '1H', '4H', '1D', '1W'].map((tf) => (
          <button
            key={tf}
            onClick={() => setChartTimeframe(tf)}
            className={`px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap ${
              chartTimeframe === tf
                ? 'bg-blue-600 text-white'
                : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            {tf}
          </button>
        ))}
      </div>

      {/* Mini Chart Placeholder */}
      <div className="bg-gray-800 rounded-xl p-4 h-48">
        <div className="flex items-center justify-center h-full text-gray-400">
          <BarChart3 size={48} />
          <span className="ml-2 text-lg">Chart ({chartTimeframe})</span>
        </div>
      </div>

      {/* Quick Order Panel */}
      <motion.div 
        className="bg-gray-800 rounded-xl p-4 border border-gray-600"
        drag="y"
        dragConstraints={{ top: -100, bottom: 0 }}
        onDragEnd={handleSwipe}
      >
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-lg">Quick Order</h3>
          <button
            onClick={() => setShowAdvancedOrder(!showAdvancedOrder)}
            className="text-blue-400 text-sm"
          >
            {showAdvancedOrder ? 'Simple' : 'Advanced'}
          </button>
        </div>

        {/* Order Type & Side Selector */}
        <div className="grid grid-cols-2 gap-3 mb-4">
          <div className="space-y-2">
            <label className="text-sm text-gray-300">Side</label>
            <div className="flex rounded-lg overflow-hidden">
              <button
                onClick={() => setQuickOrder(prev => ({ ...prev, side: 'buy' }))}
                className={`flex-1 py-2 text-sm font-medium ${
                  quickOrder.side === 'buy'
                    ? 'bg-green-600 text-white'
                    : 'bg-gray-700 text-gray-300'
                }`}
              >
                BUY
              </button>
              <button
                onClick={() => setQuickOrder(prev => ({ ...prev, side: 'sell' }))}
                className={`flex-1 py-2 text-sm font-medium ${
                  quickOrder.side === 'sell'
                    ? 'bg-red-600 text-white'
                    : 'bg-gray-700 text-gray-300'
                }`}
              >
                SELL
              </button>
            </div>
          </div>
          
          <div className="space-y-2">
            <label className="text-sm text-gray-300">Type</label>
            <select
              value={quickOrder.orderType}
              onChange={(e) => setQuickOrder(prev => ({ ...prev, orderType: e.target.value as any }))}
              className="w-full py-2 px-3 bg-gray-700 border border-gray-600 rounded text-sm"
            >
              <option value="market">Market</option>
              <option value="limit">Limit</option>
              <option value="stop">Stop</option>
            </select>
          </div>
        </div>

        {/* Quantity Input */}
        <div className="space-y-2 mb-4">
          <label className="text-sm text-gray-300">Quantity</label>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setQuickOrder(prev => ({ ...prev, quantity: Math.max(1, prev.quantity - 10) }))}
              className="w-10 h-10 bg-gray-700 rounded-lg flex items-center justify-center"
            >
              -
            </button>
            <input
              type="number"
              value={quickOrder.quantity}
              onChange={(e) => setQuickOrder(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
              className="flex-1 py-2 px-3 bg-gray-700 border border-gray-600 rounded text-center"
            />
            <button
              onClick={() => setQuickOrder(prev => ({ ...prev, quantity: prev.quantity + 10 }))}
              className="w-10 h-10 bg-gray-700 rounded-lg flex items-center justify-center"
            >
              +
            </button>
          </div>
        </div>

        {/* Price Input (for limit/stop orders) */}
        {quickOrder.orderType !== 'market' && (
          <div className="space-y-2 mb-4">
            <label className="text-sm text-gray-300">
              {quickOrder.orderType === 'limit' ? 'Limit Price' : 'Stop Price'}
            </label>
            <input
              type="number"
              step="0.01"
              value={quickOrder.price || ''}
              onChange={(e) => setQuickOrder(prev => ({ ...prev, price: parseFloat(e.target.value) || undefined }))}
              className="w-full py-2 px-3 bg-gray-700 border border-gray-600 rounded"
            />
          </div>
        )}

        {/* Advanced Options */}
        <AnimatePresence>
          {showAdvancedOrder && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="space-y-3 mb-4"
            >
              <div>
                <label className="text-sm text-gray-300 block mb-1">Time In Force</label>
                <select
                  value={quickOrder.timeInForce}
                  onChange={(e) => setQuickOrder(prev => ({ ...prev, timeInForce: e.target.value as any }))}
                  className="w-full py-2 px-3 bg-gray-700 border border-gray-600 rounded text-sm"
                >
                  <option value="GTC">Good Till Cancelled</option>
                  <option value="IOC">Immediate or Cancel</option>
                  <option value="FOK">Fill or Kill</option>
                  <option value="DAY">Day Order</option>
                </select>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Order Preview */}
        <div className="bg-gray-700 rounded-lg p-3 mb-4">
          <div className="flex justify-between text-sm mb-1">
            <span>Estimated Cost:</span>
            <span className="font-medium">
              {formatCurrency((quickOrder.price || 152.80) * quickOrder.quantity, hideBalances)}
            </span>
          </div>
          <div className="flex justify-between text-sm text-gray-400">
            <span>+ Commission:</span>
            <span>$0.00</span>
          </div>
        </div>

        {/* Execute Button */}
        <motion.button
          onClick={executeQuickOrder}
          className={`w-full py-4 rounded-xl font-bold text-lg ${
            quickOrder.side === 'buy'
              ? 'bg-green-600 hover:bg-green-700 text-white'
              : 'bg-red-600 hover:bg-red-700 text-white'
          }`}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          {quickOrder.side === 'buy' ? 'BUY' : 'SELL'} {quickOrder.symbol}
        </motion.button>
      </motion.div>
    </div>
  );

  const PositionsTab = () => (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold">Positions</h2>
        <button onClick={() => setHideBalances(!hideBalances)} className="p-2">
          {hideBalances ? <EyeOff size={20} /> : <Eye size={20} />}
        </button>
      </div>
      
      {positions.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <PieChart size={48} className="mx-auto mb-4 opacity-50" />
          <p>No open positions</p>
        </div>
      ) : (
        <div className="space-y-3">
          {positions.map((position, index) => (
            <motion.div
              key={position.symbol}
              className="bg-gray-800 rounded-xl p-4 border border-gray-600"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-lg">{position.symbol}</span>
                  <span className={`px-2 py-1 text-xs rounded ${
                    position.side === 'long' ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'
                  }`}>
                    {position.side.toUpperCase()}
                  </span>
                </div>
                <button className="p-1">
                  <MoreHorizontal size={16} className="text-gray-400" />
                </button>
              </div>
              
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <div className="text-gray-400">Quantity</div>
                  <div className="font-medium">{Math.abs(position.quantity)}</div>
                </div>
                <div>
                  <div className="text-gray-400">Avg Price</div>
                  <div className="font-medium">{formatCurrency(position.avgPrice, hideBalances)}</div>
                </div>
                <div>
                  <div className="text-gray-400">Market Price</div>
                  <div className="font-medium">{formatCurrency(position.marketPrice, hideBalances)}</div>
                </div>
                <div>
                  <div className="text-gray-400">P&L</div>
                  <div className={`font-bold ${position.unrealizedPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                    {formatCurrency(position.unrealizedPnL, hideBalances)}
                    <div className="text-xs">
                      {formatPercent(position.unrealizedPnLPercent, hideBalances)}
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Quick Actions */}
              <div className="flex space-x-2 mt-3">
                <button className="flex-1 py-2 bg-red-600 hover:bg-red-700 rounded-lg text-sm font-medium">
                  Close
                </button>
                <button className="flex-1 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg text-sm font-medium">
                  Add
                </button>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );

  const OrdersTab = () => (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold">Orders</h2>
        <button className="text-red-400 text-sm">Cancel All</button>
      </div>
      
      {orders.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <Activity size={48} className="mx-auto mb-4 opacity-50" />
          <p>No pending orders</p>
        </div>
      ) : (
        <div className="space-y-3">
          {orders.map((order, index) => (
            <motion.div
              key={order.id}
              className="bg-gray-800 rounded-xl p-4 border border-gray-600"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-2">
                  <span className="font-bold">{order.symbol}</span>
                  <span className={`px-2 py-1 text-xs rounded ${
                    order.side === 'buy' ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'
                  }`}>
                    {order.side.toUpperCase()}
                  </span>
                  <span className="px-2 py-1 text-xs rounded bg-blue-900 text-blue-300">
                    {order.orderType.toUpperCase()}
                  </span>
                </div>
                <button className="text-red-400 text-sm">Cancel</button>
              </div>
              
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <div className="text-gray-400">Quantity</div>
                  <div>{order.quantity}</div>
                </div>
                <div>
                  <div className="text-gray-400">Price</div>
                  <div>{formatCurrency(order.price, hideBalances)}</div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );

  const WatchlistTab = () => (
    <div className="p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-bold">Watchlist</h2>
        <button className="p-2">
          <Settings size={20} className="text-gray-400" />
        </button>
      </div>
      
      <div className="space-y-2">
        {watchlist.map((item) => (
          <motion.div
            key={item.symbol}
            className="bg-gray-800 rounded-lg p-4 border border-gray-600"
            whileHover={{ backgroundColor: 'rgba(55, 65, 81, 0.8)' }}
            onTap={() => setSelectedSymbol(item.symbol)}
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="font-bold">{item.symbol}</div>
                <div className="text-sm text-gray-400">{formatCurrency(item.price, hideBalances)}</div>
              </div>
              <div className="text-right">
                <div className={`font-bold ${item.change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {item.change >= 0 ? '+' : ''}{formatCurrency(Math.abs(item.change), hideBalances)}
                </div>
                <div className={`text-sm ${item.changePercent >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {formatPercent(item.changePercent, hideBalances)}
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );

  return (
    <div 
      ref={containerRef}
      className="h-full bg-gradient-to-br from-gray-900 to-black text-white overflow-hidden"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {/* Status Bar */}
      <div className="bg-black/50 p-3 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <button onClick={() => setShowSidebar(true)}>
            <Menu size={20} />
          </button>
          <span className="font-bold text-lg">TradeMaster</span>
        </div>
        <div className="flex items-center space-x-3">
          <Bell size={20} className="text-gray-400" />
          <div className="w-2 h-2 bg-green-400 rounded-full"></div>
        </div>
      </div>

      {/* Main Content */}
      <motion.div 
        className="flex-1 overflow-auto"
        drag="x"
        dragConstraints={{ left: 0, right: 0 }}
        onDragEnd={handleSwipe}
      >
        {activeTab === 'trade' && <TradeTab />}
        {activeTab === 'positions' && <PositionsTab />}
        {activeTab === 'orders' && <OrdersTab />}
        {activeTab === 'watchlist' && <WatchlistTab />}
      </motion.div>

      {/* Bottom Navigation */}
      <div className="bg-gray-900/95 backdrop-blur-sm border-t border-gray-700">
        <div className="grid grid-cols-4 py-2">
          {[
            { key: 'trade', icon: TrendingUp, label: 'Trade' },
            { key: 'positions', icon: PieChart, label: 'Positions' },
            { key: 'orders', icon: Activity, label: 'Orders' },
            { key: 'watchlist', icon: BarChart3, label: 'Watch' }
          ].map(({ key, icon: Icon, label }) => (
            <motion.button
              key={key}
              onClick={() => setActiveTab(key as any)}
              className={`p-3 flex flex-col items-center space-y-1 ${
                activeTab === key ? 'text-blue-400' : 'text-gray-400'
              }`}
              whileTap={{ scale: 0.95 }}
            >
              <Icon size={24} />
              <span className="text-xs">{label}</span>
            </motion.button>
          ))}
        </div>
      </div>

      {/* Quick Actions Overlay */}
      <AnimatePresence>
        {showQuickActions && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50 flex items-end"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onTap={() => setShowQuickActions(false)}
          >
            <motion.div
              className="w-full bg-gray-900 rounded-t-2xl p-6 border-t border-gray-700"
              initial={{ y: 300 }}
              animate={{ y: 0 }}
              exit={{ y: 300 }}
              drag="y"
              dragConstraints={{ top: 0, bottom: 300 }}
              onDragEnd={(e, info) => {
                if (info.offset.y > 100) {
                  setShowQuickActions(false);
                }
              }}
            >
              <div className="w-12 h-1 bg-gray-600 rounded-full mx-auto mb-6"></div>
              
              <h3 className="text-xl font-bold mb-4">Quick Actions</h3>
              
              <div className="grid grid-cols-2 gap-4">
                <button className="p-4 bg-green-600 rounded-xl flex flex-col items-center space-y-2">
                  <TrendingUp size={24} />
                  <span className="font-medium">Quick Buy</span>
                </button>
                <button className="p-4 bg-red-600 rounded-xl flex flex-col items-center space-y-2">
                  <TrendingDown size={24} />
                  <span className="font-medium">Quick Sell</span>
                </button>
                <button className="p-4 bg-gray-700 rounded-xl flex flex-col items-center space-y-2">
                  <Shield size={24} />
                  <span className="font-medium">Stop Loss</span>
                </button>
                <button className="p-4 bg-gray-700 rounded-xl flex flex-col items-center space-y-2">
                  <Target size={24} />
                  <span className="font-medium">Take Profit</span>
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <AnimatePresence>
        {showSidebar && (
          <motion.div
            className="fixed inset-0 z-50 bg-black/50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onTap={() => setShowSidebar(false)}
          >
            <motion.div
              className="absolute left-0 top-0 bottom-0 w-80 bg-gray-900 border-r border-gray-700"
              initial={{ x: -320 }}
              animate={{ x: 0 }}
              exit={{ x: -320 }}
            >
              <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-xl font-bold">Menu</h2>
                  <button onClick={() => setShowSidebar(false)}>
                    <X size={24} />
                  </button>
                </div>
                
                {/* Account Balance */}
                <div className="bg-gray-800 rounded-xl p-4 mb-6">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-400">Account Balance</span>
                    <button onClick={() => setHideBalances(!hideBalances)}>
                      {hideBalances ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  <div className="text-2xl font-bold text-green-400">
                    {formatCurrency(125650.75, hideBalances)}
                  </div>
                  <div className="text-sm text-gray-400">
                    Day P&L: {formatCurrency(2340.25, hideBalances)}
                  </div>
                </div>

                {/* Navigation Links */}
                <div className="space-y-2">
                  {[
                    { label: 'Portfolio', icon: PieChart },
                    { label: 'Analytics', icon: BarChart3 },
                    { label: 'Settings', icon: Settings },
                    { label: 'Help', icon: Bell }
                  ].map((item) => (
                    <button
                      key={item.label}
                      className="w-full flex items-center space-x-3 p-3 text-left hover:bg-gray-800 rounded-lg"
                    >
                      <item.icon size={20} />
                      <span>{item.label}</span>
                    </button>
                  ))}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Touch Feedback */}
      {touchPosition && (
        <div
          className="fixed pointer-events-none z-50 w-4 h-4 bg-blue-400 rounded-full transform -translate-x-2 -translate-y-2 opacity-50"
          style={{
            left: touchPosition.x,
            top: touchPosition.y
          }}
        />
      )}
    </div>
  );
};

export default MobileTradingInterface;