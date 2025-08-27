import React, { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { 
  TrendingUp, 
  TrendingDown,
  Activity,
  Eye,
  Star,
  Plus,
  Minus,
  BarChart3,
  LineChart,
  BarChart3 as Candlestick,
  Volume2,
  Clock,
  Globe,
  Zap
} from 'lucide-react'
import { MarketScanner } from './MarketScanner'
import { EconomicCalendar } from './EconomicCalendar'
import { MarketNewsTicker } from './MarketNewsTicker'
import { AdvancedChart } from '../charts/AdvancedChart'
import { SymbolLookup } from '../common/SymbolLookup'
import { useMarketData, useMarketStatus } from '../../hooks/useMarketDataWebSocket'
import { ConnectionStatus } from '../common/ConnectionStatus'
import { WebSocketErrorBoundary } from '../common/WebSocketErrorBoundary'

interface MarketStock {
  symbol: string
  name: string
  price: number
  change: number
  changePercent: number
  volume: string
  marketCap: string
  high: number
  low: number
  open: number
  isWatched: boolean
}

// Define fallback data for when WebSocket is disconnected
const fallbackMarketData: MarketStock[] = [
  {
    symbol: 'RELIANCE',
    name: 'Reliance Industries Limited',
    price: 2547.30,
    change: 23.45,
    changePercent: 0.93,
    volume: '1.2M',
    marketCap: '17.2L Cr',
    high: 2568.90,
    low: 2531.20,
    open: 2540.15,
    isWatched: true
  },
  {
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    price: 3642.80,
    change: -18.90,
    changePercent: -0.52,
    volume: '890K',
    marketCap: '13.4L Cr',
    high: 3678.45,
    low: 3635.20,
    open: 3661.70,
    isWatched: true
  },
  {
    symbol: 'HDFCBANK',
    name: 'HDFC Bank Limited',
    price: 1567.25,
    change: 12.80,
    changePercent: 0.82,
    volume: '2.1M',
    marketCap: '11.8L Cr',
    high: 1579.30,
    low: 1554.45,
    open: 1559.90,
    isWatched: false
  },
  {
    symbol: 'INFY',
    name: 'Infosys Limited',
    price: 1423.60,
    change: 8.25,
    changePercent: 0.58,
    volume: '1.8M',
    marketCap: '5.9L Cr',
    high: 1435.80,
    low: 1418.90,
    open: 1420.35,
    isWatched: true
  },
  {
    symbol: 'ICICIBANK',
    name: 'ICICI Bank Limited',
    price: 945.70,
    change: -7.45,
    changePercent: -0.78,
    volume: '3.2M',
    marketCap: '6.6L Cr',
    high: 956.30,
    low: 941.85,
    open: 953.15,
    isWatched: false
  }
]

export function MarketDataDashboard() {
  const navigate = useNavigate()
  const location = useLocation()
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE')
  const [chartType, setChartType] = useState<'line' | 'candlestick' | 'area' | 'ohlc' | 'heikin-ashi' | 'renko' | 'volume-profile' | 'mountain' | 'point-figure' | 'kagi' | 'three-line-break' | 'footprint' | 'range-bars'>('candlestick')
  const [timeframe, setTimeframe] = useState('1D')
  const [watchlistSymbols, setWatchlistSymbols] = useState(['RELIANCE', 'TCS', 'INFY'])
  const [activeMarketTab, setActiveMarketTab] = useState('overview')
  
  // Real-time market data from WebSocket
  const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK']
  const { data: marketDataMap, connectionStatus, isConnected, lastUpdate } = useMarketData(symbols)
  const { marketStatus } = useMarketStatus()
  
  // Helper function to format volume
  const formatVolume = (volume: number): string => {
    if (volume >= 1000000) {
      return `${(volume / 1000000).toFixed(1)}M`
    } else if (volume >= 1000) {
      return `${(volume / 1000).toFixed(0)}K`
    }
    return volume.toString()
  }
  
  // Convert WebSocket data to component format with fallback
  const marketData: MarketStock[] = symbols.map(symbol => {
    const wsData = marketDataMap.get(symbol)
    const fallbackData = fallbackMarketData.find(stock => stock.symbol === symbol)
    
    if (wsData) {
      return {
        symbol: wsData.symbol,
        name: fallbackData?.name || wsData.symbol,
        price: wsData.price,
        change: wsData.change,
        changePercent: wsData.changePercent,
        volume: formatVolume(wsData.volume),
        marketCap: fallbackData?.marketCap || 'N/A',
        high: wsData.high,
        low: wsData.low,
        open: wsData.open,
        isWatched: watchlistSymbols.includes(symbol)
      }
    }
    
    // Return fallback data when WebSocket is disconnected
    return fallbackData ? {
      ...fallbackData,
      isWatched: watchlistSymbols.includes(symbol)
    } : {
      symbol,
      name: symbol,
      price: 0,
      change: 0,
      changePercent: 0,
      volume: '0',
      marketCap: 'N/A',
      high: 0,
      low: 0,
      open: 0,
      isWatched: watchlistSymbols.includes(symbol)
    }
  })
  
  const isMarketOpen = marketStatus?.isOpen || false

  useEffect(() => {
    // Handle symbol selection from global search
    if (location.state?.selectedSymbol) {
      setSelectedSymbol(location.state.selectedSymbol)
      // Clear the state after using it
      window.history.replaceState({}, document.title)
    }
  }, [location.state])

  const selectedStock = marketData.find(stock => stock.symbol === selectedSymbol) || marketData[0]

  const toggleWatchlist = (symbol: string) => {
    setWatchlistSymbols(prev => {
      if (prev.includes(symbol)) {
        return prev.filter(s => s !== symbol)
      } else {
        return [...prev, symbol]
      }
    })
  }

  return (
    <WebSocketErrorBoundary>
      <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Market Data</h1>
          <p className="text-slate-400">Real-time market data and analytics</p>
        </div>
        <div className="flex items-center space-x-4">
          {/* Connection Status */}
          <ConnectionStatus
            status={connectionStatus}
            lastUpdate={lastUpdate}
            showDetails={false}
            className="px-3 py-2"
          />
          
          {/* Market Status */}
          <div className={`flex items-center space-x-2 px-4 py-2 rounded-xl glass-card ${
            isMarketOpen ? 'border-green-500/50' : 'border-orange-500/50'
          }`}>
            <div className={`w-2 h-2 rounded-full ${
              isMarketOpen ? 'bg-green-400 animate-pulse' : 'bg-orange-400'
            }`} />
            <span className={`text-sm font-medium ${
              isMarketOpen ? 'text-green-400' : 'text-orange-400'
            }`}>
              {isMarketOpen ? 'Market Open' : 'Market Closed'}
            </span>
          </div>
          
          {/* Current Time */}
          <div className="glass-card px-4 py-2 rounded-xl">
            <div className="flex items-center space-x-2 text-slate-300">
              <Clock className="w-4 h-4" />
              <span className="text-sm font-mono">{new Date().toLocaleTimeString()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Market Ticker */}
      <div className="glass-card rounded-2xl p-4 overflow-hidden">
        <div className="flex space-x-8 animate-scroll">
          {marketData.map((stock) => (
            <div key={stock.symbol} className="flex items-center space-x-3 whitespace-nowrap">
              <span className="text-white font-semibold">{stock.symbol}</span>
              <span className="text-slate-300">₹{stock.price.toLocaleString()}</span>
              <span className={`flex items-center space-x-1 text-sm ${
                stock.change >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {stock.change >= 0 ? 
                  <TrendingUp className="w-3 h-3" /> : 
                  <TrendingDown className="w-3 h-3" />
                }
                <span>{stock.changePercent.toFixed(2)}%</span>
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Chart Area */}
        <div className="lg:col-span-2 space-y-6">
          {/* Chart Controls */}
          <div className="glass-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white mb-1">{selectedStock.name}</h2>
                <div className="flex items-center space-x-4">
                  <span className="text-2xl font-bold text-white">
                    ₹{selectedStock.price.toLocaleString()}
                  </span>
                  <span className={`flex items-center space-x-1 text-lg ${
                    selectedStock.change >= 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {selectedStock.change >= 0 ? 
                      <TrendingUp className="w-4 h-4" /> : 
                      <TrendingDown className="w-4 h-4" />
                    }
                    <span>{selectedStock.change >= 0 ? '+' : ''}{selectedStock.change}</span>
                    <span>({selectedStock.changePercent.toFixed(2)}%)</span>
                  </span>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => toggleWatchlist(selectedStock.symbol)}
                  className={`p-2 rounded-xl transition-colors ${
                    selectedStock.isWatched 
                      ? 'bg-yellow-500/20 text-yellow-400' 
                      : 'bg-slate-700/50 text-slate-400 hover:text-yellow-400'
                  }`}
                >
                  <Star className={`w-5 h-5 ${selectedStock.isWatched ? 'fill-current' : ''}`} />
                </button>
                <button 
                  onClick={() => navigate('/trading')}
                  className="cyber-button px-4 py-2 text-sm rounded-xl"
                >
                  Trade
                </button>
              </div>
            </div>


            {/* Timeframe Controls */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center space-x-2">
                {['1D', '1W', '1M', '3M', '1Y'].map((tf) => (
                  <button
                    key={tf}
                    onClick={() => setTimeframe(tf)}
                    className={`px-3 py-1.5 text-sm rounded-xl transition-colors ${
                      timeframe === tf
                        ? 'bg-cyan-500/20 text-cyan-400'
                        : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                    }`}
                  >
                    {tf}
                  </button>
                ))}
              </div>
            </div>

            {/* Real Chart Area */}
            <div className="h-80 mb-6 overflow-hidden">
              <AdvancedChart 
                key={`${selectedStock.symbol}-${chartType}`}
                symbol={selectedStock.symbol} 
                height={300}
                initialChartType={chartType as any}
                mode="simple"
              />
            </div>
          </div>

          {/* Market Stats */}
          <div className="grid gap-4 md:grid-cols-4">
            <div className="glass-card p-4 rounded-xl">
              <div className="text-sm text-slate-400 mb-1">High</div>
              <div className="text-lg font-bold text-white">₹{selectedStock.high.toLocaleString()}</div>
            </div>
            <div className="glass-card p-4 rounded-xl">
              <div className="text-sm text-slate-400 mb-1">Low</div>
              <div className="text-lg font-bold text-white">₹{selectedStock.low.toLocaleString()}</div>
            </div>
            <div className="glass-card p-4 rounded-xl">
              <div className="text-sm text-slate-400 mb-1">Volume</div>
              <div className="text-lg font-bold text-white">{selectedStock.volume}</div>
            </div>
            <div className="glass-card p-4 rounded-xl">
              <div className="text-sm text-slate-400 mb-1">Market Cap</div>
              <div className="text-lg font-bold text-white">{selectedStock.marketCap}</div>
            </div>
          </div>
        </div>

        {/* Sidebar - Watchlist & Market Movers */}
        <div className="space-y-6">
          {/* Watchlist */}
          <div className="glass-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-bold text-white flex items-center">
                <Star className="w-5 h-5 mr-2 text-yellow-400" />
                Watchlist
              </h3>
              <span className="text-sm text-slate-400">
                {marketData.filter(stock => stock.isWatched).length} stocks
              </span>
            </div>
            <div className="space-y-3">
              {marketData.filter(stock => stock.isWatched).map((stock) => (
                <div 
                  key={stock.symbol}
                  onClick={() => setSelectedSymbol(stock.symbol)}
                  className={`p-3 rounded-xl transition-all cursor-pointer ${
                    selectedSymbol === stock.symbol
                      ? 'bg-purple-500/20 border border-purple-500/50'
                      : 'bg-slate-800/30 hover:bg-slate-700/50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-semibold text-white">{stock.symbol}</div>
                      <div className="text-sm text-slate-400">₹{stock.price.toLocaleString()}</div>
                    </div>
                    <div className="text-right">
                      <div className={`text-sm font-medium ${
                        stock.change >= 0 ? 'text-green-400' : 'text-red-400'
                      }`}>
                        {stock.change >= 0 ? '+' : ''}{stock.changePercent.toFixed(2)}%
                      </div>
                      <div className="text-xs text-slate-400">{stock.volume}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Top Gainers */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-lg font-bold text-white mb-6 flex items-center">
              <TrendingUp className="w-5 h-5 mr-2 text-green-400" />
              Top Gainers
            </h3>
            <div className="space-y-3">
              {marketData
                .filter(stock => stock.change > 0)
                .sort((a, b) => b.changePercent - a.changePercent)
                .slice(0, 3)
                .map((stock) => (
                <div key={stock.symbol} className="flex items-center justify-between p-2">
                  <div>
                    <div className="text-white font-medium">{stock.symbol}</div>
                    <div className="text-sm text-slate-400">₹{stock.price.toLocaleString()}</div>
                  </div>
                  <div className="text-green-400 font-medium">
                    +{stock.changePercent.toFixed(2)}%
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-lg font-bold text-white mb-6 flex items-center">
              <Zap className="w-5 h-5 mr-2 text-cyan-400" />
              Quick Actions
            </h3>
            <div className="grid gap-3">
              <button 
                onClick={() => alert('Price alert functionality coming soon!')}
                className="cyber-button w-full py-3 rounded-xl text-sm"
              >
                Create Alert
              </button>
              <button 
                onClick={() => {
                  // Toggle watchlist status for selected symbol
                  setMarketData(prev => 
                    prev.map(stock => 
                      stock.symbol === selectedSymbol 
                        ? { ...stock, isWatched: !stock.isWatched }
                        : stock
                    )
                  );
                  alert(`${selectedSymbol} ${marketData.find(s => s.symbol === selectedSymbol)?.isWatched ? 'removed from' : 'added to'} watchlist!`);
                }}
                className="glass-card border border-green-500/50 hover:border-green-400/70 text-green-400 w-full py-3 rounded-xl text-sm transition-colors"
              >
                Add to Watchlist
              </button>
              <button 
                onClick={() => alert('Advanced market analysis coming soon!')}
                className="glass-card border border-orange-500/50 hover:border-orange-400/70 text-orange-400 w-full py-3 rounded-xl text-sm transition-colors"
              >
                Market Analysis
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Market Analysis Tabs */}
      <div className="glass-card rounded-2xl p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-white">Market Analysis Tools</h2>
          <div className="flex space-x-1">
            {[
              { key: 'overview', label: 'Overview', icon: BarChart3 },
              { key: 'charts', label: 'Advanced Charts', icon: LineChart },
              { key: 'news', label: 'Market News', icon: Globe },
              { key: 'calendar', label: 'Economic Calendar', icon: Clock },
              { key: 'scanner', label: 'Market Scanner', icon: Activity }
            ].map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                onClick={() => setActiveMarketTab(key)}
                className={`flex items-center space-x-2 px-4 py-2 rounded-xl transition-all ${
                  activeMarketTab === key
                    ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="text-sm font-medium">{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        {activeMarketTab === 'overview' && (
          <div className="space-y-6">
            {/* Selected Stock Overview */}
            <div className="glass-card p-6 rounded-xl">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <div className="p-2 rounded-xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20">
                    {selectedStock.isWatched ? <Star className="w-6 h-6 text-yellow-400" /> : <Activity className="w-6 h-6 text-purple-400" />}
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-white">{selectedStock.symbol}</h2>
                    <p className="text-slate-400">{selectedStock.name}</p>
                  </div>
                </div>
                <button
                  onClick={() => {
                    const updatedData = marketData.map(stock =>
                      stock.symbol === selectedStock.symbol
                        ? { ...stock, isWatched: !stock.isWatched }
                        : stock
                    );
                    setMarketData(updatedData);
                  }}
                  className={`p-2 rounded-xl transition-colors ${
                    selectedStock.isWatched
                      ? 'text-yellow-400 hover:bg-yellow-500/20'
                      : 'text-slate-400 hover:text-yellow-400 hover:bg-slate-600/50'
                  }`}
                  title={selectedStock.isWatched ? "Remove from watchlist" : "Add to watchlist"}
                >
                  <Star className="w-5 h-5" />
                </button>
              </div>
              
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="text-center">
                  <div className="text-3xl font-bold text-white">₹{selectedStock.price.toFixed(2)}</div>
                  <div className={`text-sm font-semibold ${
                    selectedStock.change >= 0 ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {selectedStock.change >= 0 ? '+' : ''}₹{selectedStock.change.toFixed(2)} ({selectedStock.changePercent.toFixed(2)}%)
                  </div>
                  <div className="text-xs text-slate-400 mt-1">Current Price</div>
                </div>
                
                <div className="text-center">
                  <div className="text-xl font-bold text-green-400">₹{selectedStock.high.toFixed(2)}</div>
                  <div className="text-xs text-slate-400">Day High</div>
                </div>
                
                <div className="text-center">
                  <div className="text-xl font-bold text-red-400">₹{selectedStock.low.toFixed(2)}</div>
                  <div className="text-xs text-slate-400">Day Low</div>
                </div>
                
                <div className="text-center">
                  <div className="text-xl font-bold text-blue-400">{selectedStock.volume}</div>
                  <div className="text-xs text-slate-400">Volume</div>
                </div>
              </div>
            </div>

            {/* Stock Details */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="glass-card p-4 rounded-xl">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-medium text-slate-400">Market Cap</h4>
                  <Globe className="w-4 h-4 text-blue-400" />
                </div>
                <div className="text-xl font-bold text-white">{selectedStock.marketCap}</div>
              </div>
              
              <div className="glass-card p-4 rounded-xl">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-medium text-slate-400">Open Price</h4>
                  <Clock className="w-4 h-4 text-purple-400" />
                </div>
                <div className="text-xl font-bold text-white">₹{selectedStock.open.toFixed(2)}</div>
              </div>
            </div>

            {/* Top Gainers & Losers */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="glass-card p-6 rounded-xl">
                <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
                  <TrendingUp className="w-5 h-5 text-green-400 mr-2" />
                  Top Gainers
                </h4>
                <div className="space-y-3">
                  {marketData.filter(stock => stock.change > 0).slice(0, 3).map((stock) => (
                    <div key={stock.symbol} className="flex items-center justify-between py-2 border-b border-slate-700/50">
                      <div>
                        <div className="font-medium text-white">{stock.symbol}</div>
                        <div className="text-sm text-slate-400">{stock.name}</div>
                      </div>
                      <div className="text-right">
                        <div className="font-semibold text-white">₹{stock.price.toFixed(2)}</div>
                        <div className="text-green-400 text-sm">+{stock.changePercent.toFixed(2)}%</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="glass-card p-6 rounded-xl">
                <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
                  <TrendingDown className="w-5 h-5 text-red-400 mr-2" />
                  Top Losers
                </h4>
                <div className="space-y-3">
                  {marketData.filter(stock => stock.change < 0).slice(0, 3).map((stock) => (
                    <div key={stock.symbol} className="flex items-center justify-between py-2 border-b border-slate-700/50">
                      <div>
                        <div className="font-medium text-white">{stock.symbol}</div>
                        <div className="text-sm text-slate-400">{stock.name}</div>
                      </div>
                      <div className="text-right">
                        <div className="font-semibold text-white">₹{stock.price.toFixed(2)}</div>
                        <div className="text-red-400 text-sm">{stock.changePercent.toFixed(2)}%</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Market Activity */}
            <div className="glass-card p-6 rounded-xl">
              <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
                <Activity className="w-5 h-5 text-purple-400 mr-2" />
                Market Activity
              </h4>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-400">1,247</div>
                  <div className="text-sm text-slate-400">Advances</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-red-400">892</div>
                  <div className="text-sm text-slate-400">Declines</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-yellow-400">156</div>
                  <div className="text-sm text-slate-400">Unchanged</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-400">₹45.2K Cr</div>
                  <div className="text-sm text-slate-400">Turnover</div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeMarketTab === 'charts' && (
          <div className="space-y-6">
            <div className="h-96">
              <AdvancedChart symbol={selectedSymbol} hideFitToScreen={true} height={380} />
            </div>
          </div>
        )}

        {activeMarketTab === 'news' && (
          <div>
            <MarketNewsTicker />
          </div>
        )}

        {activeMarketTab === 'calendar' && (
          <div>
            <EconomicCalendar />
          </div>
        )}

        {activeMarketTab === 'scanner' && (
          <div>
            <MarketScanner />
          </div>
        )}
        </div>
      </div>
    </WebSocketErrorBoundary>
  )
}