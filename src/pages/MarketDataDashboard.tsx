import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { 
  Activity, 
  BarChart3, 
  TrendingUp, 
  Maximize2,
  Grid3X3,
  Smartphone,
  Monitor
} from 'lucide-react'
import {
  MarketDataTicker,
  PriceChart,
  OrderBookWidget,
  WatchlistManager,
  MarketStatus,
  SymbolSearch,
  type MarketSymbol,
  type CandlestickData,
  type WatchlistSymbol
} from '@/components/market-data'
import { useMarketData, useConnectionStatus } from '@/hooks/useWebSocket'
import { useEnhancedMarketData } from '@/hooks/useEnhancedMarketData'
import { MultiBrokerPortfolioWidget } from '@/components/widgets/MultiBrokerPortfolioWidget'

interface MarketDataDashboardProps {
  initialSymbols?: string[]
  layout?: 'desktop' | 'tablet' | 'mobile'
}

const ViewModeToggle: React.FC<{
  mode: 'desktop' | 'tablet' | 'mobile'
  onChange: (mode: 'desktop' | 'tablet' | 'mobile') => void
}> = ({ mode, onChange }) => {
  return (
    <div className="flex items-center space-x-1 bg-slate-800/50 rounded-lg p-1">
      <button
        onClick={() => onChange('desktop')}
        className={`p-2 rounded-lg transition-all ${
          mode === 'desktop' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
        }`}
        title="Desktop View"
      >
        <Monitor className="h-4 w-4" />
      </button>
      <button
        onClick={() => onChange('tablet')}
        className={`p-2 rounded-lg transition-all ${
          mode === 'tablet' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
        }`}
        title="Tablet View"
      >
        <Grid3X3 className="h-4 w-4" />
      </button>
      <button
        onClick={() => onChange('mobile')}
        className={`p-2 rounded-lg transition-all ${
          mode === 'mobile' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
        }`}
        title="Mobile View"
      >
        <Smartphone className="h-4 w-4" />
      </button>
    </div>
  )
}

const DashboardHeader: React.FC<{
  viewMode: 'desktop' | 'tablet' | 'mobile'
  onViewModeChange: (mode: 'desktop' | 'tablet' | 'mobile') => void
  onToggleFullscreen: () => void
  isConnected: boolean
  connectionStatus: string
  lastUpdate: Date | null
}> = ({ viewMode, onViewModeChange, onToggleFullscreen, isConnected, connectionStatus, lastUpdate }) => {

  return (
    <motion.div
      className="glass-widget-card rounded-2xl p-6 mb-6"
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-3">
            <div className={`w-4 h-4 rounded-full ${
              isConnected ? 'bg-green-400 animate-pulse shadow-lg shadow-green-400/50' : 'bg-red-400'
            }`} />
            <div>
              <h1 className="text-2xl font-bold text-white">Market Data Dashboard</h1>
              <div className="flex items-center space-x-4 text-sm text-slate-400">
                <span>Status: {connectionStatus}</span>
                {lastUpdate && (
                  <span>Last Update: {lastUpdate.toLocaleTimeString()}</span>
                )}
                <span>•</span>
                <span>Real-time Market Data</span>
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Activity className={`h-5 w-5 ${isConnected ? 'text-green-400' : 'text-red-400'}`} />
            <BarChart3 className="h-5 w-5 text-purple-400" />
            <TrendingUp className="h-5 w-5 text-cyan-400" />
          </div>
        </div>

        <div className="flex items-center space-x-4">
          {/* Global Symbol Search */}
          <div className="w-80 max-w-sm">
            <SymbolSearch
              onSymbolSelect={(symbol) => {
                console.log('Selected symbol:', symbol)
                setSelectedSymbol(symbol)
                // Auto-subscribe to new symbol for real-time data
                subscribe(symbol)
              }}
              onAddToWatchlist={(symbol) => {
                console.log('Adding to watchlist:', symbol)
                // Add to watchlist if not already present
                const isAlreadyInWatchlist = watchlistSymbols.some(w => w.symbol === symbol.symbol)
                if (!isAlreadyInWatchlist) {
                  const newWatchlistSymbol: WatchlistSymbol = {
                    id: symbol.symbol,
                    symbol: symbol.symbol,
                    name: symbol.name,
                    price: symbol.price || 0,
                    change: symbol.change || 0,
                    changePercent: symbol.changePercent || 0,
                    volume: symbol.volume || 0,
                    sector: symbol.sector,
                    isFavorite: false,
                    isHidden: false
                  }
                  setWatchlistSymbols(prev => [...prev, newWatchlistSymbol])
                  subscribe(symbol.symbol)
                }
              }}
              placeholder="Search symbols globally..."
              maxResults={6}
              className="w-full"
            />
          </div>
          
          <ViewModeToggle mode={viewMode} onChange={onViewModeChange} />
          
          <button
            onClick={onToggleFullscreen}
            className="cyber-button-sm p-3 rounded-xl hover:scale-110 transition-all duration-300"
            title="Toggle Fullscreen"
          >
            <Maximize2 className="h-4 w-4" />
          </button>

          <div className="text-right">
            <div className="text-sm font-semibold text-white">Market Hours</div>
            <div className="text-xs text-green-400">9:15 AM - 3:30 PM IST</div>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

export const MarketDataDashboard: React.FC<MarketDataDashboardProps> = ({
  initialSymbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK'],
  layout = 'desktop'
}) => {
  const [viewMode, setViewMode] = useState<'desktop' | 'tablet' | 'mobile'>(layout)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE')
  const [watchlistSymbols, setWatchlistSymbols] = useState<WatchlistSymbol[]>([])
  const [expandedChart, setExpandedChart] = useState(false)

  // Enhanced WebSocket integration for real-time data
  const { 
    marketData: enhancedMarketData, 
    isConnected: enhancedConnected,
    connectionStatus: enhancedStatus,
    lastUpdate: enhancedLastUpdate,
    subscribe,
    getSymbolData
  } = useEnhancedMarketData({
    symbols: initialSymbols,
    updateInterval: 2000,
    enableRealtimeUpdates: true
  })

  // Fallback to original WebSocket hook if needed
  const { marketData: fallbackData, lastUpdate } = useMarketData(initialSymbols)

  // Use enhanced market data, fallback to original if needed
  const activeMarketData = enhancedConnected ? enhancedMarketData : fallbackData
  const activeLastUpdate = enhancedConnected ? enhancedLastUpdate : lastUpdate

  // Convert market data to ticker format
  const tickerSymbols: MarketSymbol[] = Object.entries(activeMarketData).map(([symbol, data]) => ({
    symbol,
    name: getSymbolName(symbol),
    price: data.price,
    change: data.change,
    changePercent: data.changePercent,
    volume: data.volume,
    high: data.high,
    low: data.low,
    lastUpdated: data.timestamp
  }))

  // Generate mock candlestick data for selected symbol
  const generateCandlestickData = (): CandlestickData[] => {
    const data: CandlestickData[] = []
    const currentSymbolData = activeMarketData[selectedSymbol]
    const basePrice = currentSymbolData?.price || 2456.75
    let currentPrice = basePrice * 0.98 // Start slightly lower
    const now = new Date()

    for (let i = 99; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * 15 * 60 * 1000) // 15-minute intervals
      
      const volatility = 0.015
      const change = (Math.random() - 0.5) * volatility
      const open = currentPrice
      const close = currentPrice * (1 + change)
      const high = Math.max(open, close) * (1 + Math.random() * 0.008)
      const low = Math.min(open, close) * (1 - Math.random() * 0.008)
      const volume = Math.floor(Math.random() * 500000) + 100000

      data.push({
        timestamp,
        open,
        high,
        low,
        close,
        volume
      })

      currentPrice = close
    }

    return data
  }

  // Helper function to get symbol names
  function getSymbolName(symbol: string): string {
    const names: Record<string, string> = {
      'RELIANCE': 'Reliance Industries',
      'TCS': 'Tata Consultancy Services',
      'HDFCBANK': 'HDFC Bank',
      'INFY': 'Infosys Limited',
      'ICICIBANK': 'ICICI Bank',
      'WIPRO': 'Wipro Limited',
      'TATASTEEL': 'Tata Steel',
      'BAJFINANCE': 'Bajaj Finance'
    }
    return names[symbol] || symbol
  }

  // Convert market data to watchlist format
  useEffect(() => {
    const converted = Object.entries(activeMarketData).map(([symbol, data]) => ({
      id: symbol,
      symbol,
      name: getSymbolName(symbol),
      price: data.price,
      change: data.change,
      changePercent: data.changePercent,
      volume: data.volume,
      sector: getSymbolSector(symbol),
      isFavorite: ['RELIANCE', 'TCS', 'HDFCBANK'].includes(symbol),
      isHidden: false
    }))
    setWatchlistSymbols(converted)
  }, [activeMarketData])

  function getSymbolSector(symbol: string): string {
    const sectors: Record<string, string> = {
      'RELIANCE': 'Oil & Gas',
      'TCS': 'Information Technology',
      'HDFCBANK': 'Banking',
      'INFY': 'Information Technology',
      'ICICIBANK': 'Banking',
      'WIPRO': 'Information Technology',
      'TATASTEEL': 'Steel',
      'BAJFINANCE': 'Financial Services'
    }
    return sectors[symbol] || 'Others'
  }

  // Layout configurations
  const getLayoutClasses = () => {
    switch (viewMode) {
      case 'mobile':
        return {
          container: 'flex flex-col space-y-4',
          chartSection: 'w-full',
          sidePanel: 'w-full grid grid-cols-1 gap-4',
          tickerContainer: 'w-full',
          chartContainer: 'w-full'
        }
      case 'tablet':
        return {
          container: 'flex flex-col space-y-6',
          chartSection: 'w-full grid grid-cols-2 gap-6',
          sidePanel: 'w-full grid grid-cols-2 gap-4',
          tickerContainer: 'w-full',
          chartContainer: 'col-span-2'
        }
      default: // desktop
        return {
          container: 'flex space-x-6',
          chartSection: 'flex-1',
          sidePanel: 'w-80 space-y-4',
          tickerContainer: 'w-full',
          chartContainer: 'w-full'
        }
    }
  }

  const layout_classes = getLayoutClasses()

  const handleToggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  return (
    <div className={`min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-800 p-4 ${
      isFullscreen ? 'fixed inset-0 z-50' : ''
    }`}>
      <div className="max-w-7xl mx-auto">
        {/* Dashboard Header */}
        <DashboardHeader
          viewMode={viewMode}
          onViewModeChange={setViewMode}
          onToggleFullscreen={handleToggleFullscreen}
          isConnected={enhancedConnected}
          connectionStatus={enhancedStatus}
          lastUpdate={activeLastUpdate}
        />

        {/* Market Ticker */}
        <div className="mb-6">
          <MarketDataTicker
            symbols={tickerSymbols}
            speed="normal"
            showChange={true}
            showVolume={viewMode === 'desktop'}
            pauseOnHover={true}
            className={layout_classes.tickerContainer}
          />
        </div>

        {/* Main Content */}
        <div className={layout_classes.container}>
          {/* Chart Section */}
          <div className={layout_classes.chartSection}>
            <motion.div
              className="space-y-6"
              layout
              transition={{ duration: 0.3 }}
            >
              {/* Price Chart */}
              <PriceChart
                symbol={selectedSymbol}
                data={generateCandlestickData()}
                chartType="candlestick"
                timeframe="15m"
                indicators={[
                  {
                    name: 'RSI',
                    value: 67.8,
                    signal: 'BUY',
                    color: '#10b981'
                  },
                  {
                    name: 'MACD',
                    value: 12.5,
                    signal: 'HOLD',
                    color: '#f59e0b'
                  }
                ]}
                showVolume={true}
                showGrid={true}
                fullscreen={expandedChart}
                onFullscreenToggle={() => setExpandedChart(!expandedChart)}
                className={layout_classes.chartContainer}
              />

              {/* Order Book (Desktop & Tablet only) */}
              {viewMode !== 'mobile' && (
                <OrderBookWidget
                  symbol={selectedSymbol}
                  maxLevels={10}
                  showSpread={true}
                  showOrders={viewMode === 'desktop'}
                  compactMode={viewMode === 'tablet'}
                />
              )}

              {/* Portfolio Widget (Mobile view) */}
              {viewMode === 'mobile' && (
                <MultiBrokerPortfolioWidget
                  portfolioData={null}
                  brokerConnections={[]}
                  compactMode={true}
                />
              )}
            </motion.div>
          </div>

          {/* Side Panel */}
          <div className={layout_classes.sidePanel}>
            {/* Market Status */}
            <MarketStatus
              compact={viewMode !== 'desktop'}
              className="mb-4"
            />
            
            {/* Watchlist */}
            <WatchlistManager
              symbols={watchlistSymbols}
              onAddSymbol={(symbol) => {
                console.log('Add symbol:', symbol)
                // In real implementation, this would add to watchlist
              }}
              onRemoveSymbol={(symbolId) => {
                console.log('Remove symbol:', symbolId)
                setWatchlistSymbols(prev => prev.filter(s => s.id !== symbolId))
              }}
              onToggleFavorite={(symbolId) => {
                setWatchlistSymbols(prev =>
                  prev.map(s => s.id === symbolId ? { ...s, isFavorite: !s.isFavorite } : s)
                )
              }}
              onToggleVisibility={(symbolId) => {
                setWatchlistSymbols(prev =>
                  prev.map(s => s.id === symbolId ? { ...s, isHidden: !s.isHidden } : s)
                )
              }}
              compactMode={viewMode === 'tablet'}
            />

            {/* Portfolio Widget (Desktop & Tablet) */}
            {viewMode !== 'mobile' && (
              <MultiBrokerPortfolioWidget
                portfolioData={null}
                brokerConnections={[]}
                compactMode={viewMode === 'tablet'}
              />
            )}

            {/* Order Book (Mobile only) */}
            {viewMode === 'mobile' && (
              <OrderBookWidget
                symbol={selectedSymbol}
                maxLevels={8}
                showSpread={true}
                showOrders={false}
                compactMode={true}
              />
            )}
          </div>
        </div>

        {/* Footer Stats */}
        <motion.div
          className="mt-8 glass-widget-card rounded-2xl p-4"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8, duration: 0.6 }}
        >
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center text-sm">
            <div>
              <div className="text-green-400 font-bold text-lg">
                {tickerSymbols.filter(s => s.change > 0).length}
              </div>
              <div className="text-slate-400">Gainers</div>
            </div>
            <div>
              <div className="text-red-400 font-bold text-lg">
                {tickerSymbols.filter(s => s.change < 0).length}
              </div>
              <div className="text-slate-400">Losers</div>
            </div>
            <div>
              <div className="text-cyan-400 font-bold text-lg">
                {tickerSymbols.length}
              </div>
              <div className="text-slate-400">Symbols</div>
            </div>
            <div>
              <div className="text-purple-400 font-bold text-lg">
                {activeLastUpdate ? activeLastUpdate.toLocaleTimeString() : 'N/A'}
              </div>
              <div className="text-slate-400">Last Update</div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}