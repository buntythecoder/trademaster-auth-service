// Enhanced Market Data Dashboard - Integration Component
// FRONT-003: Real-time Market Data Enhancement

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  BarChart3,
  Search,
  Layers,
  TrendingUp,
  Activity,
  Maximize2,
  Settings,
  RefreshCw,
  Zap,
  Eye,
  Target,
  Filter,
  Star,
  Grid3X3,
  Monitor,
  Wifi,
  WifiOff,
  AlertCircle
} from 'lucide-react'

// Import our new components
import AdvancedChart from './AdvancedChart/AdvancedChart'
import MarketScanner from './MarketScanner/MarketScanner'
import EnhancedSymbolSearch from './EnhancedSymbolSearch/EnhancedSymbolSearch'
import OrderBookVisualization from './OrderBook/OrderBookVisualization'
import { MarketDataTicker } from './MarketDataTicker/MarketDataTicker'

// Import our enhanced hooks
import { useRealTimeMarketData } from '../../hooks/useRealTimeMarketData'
import { useEnhancedMarketData, RealTimeMarketData } from '../../hooks/useEnhancedMarketData'

interface EnhancedMarketDataDashboardProps {
  className?: string
}

interface DashboardLayout {
  id: string
  name: string
  components: Array<{
    id: string
    component: string
    props: Record<string, any>
    span: { cols: number; rows: number }
  }>
}

const defaultLayouts: DashboardLayout[] = [
  {
    id: 'trader',
    name: 'Trader View',
    components: [
      {
        id: 'search',
        component: 'SymbolSearch',
        props: {},
        span: { cols: 12, rows: 1 }
      },
      {
        id: 'chart',
        component: 'AdvancedChart',
        props: { symbol: 'RELIANCE' },
        span: { cols: 8, rows: 6 }
      },
      {
        id: 'orderbook',
        component: 'OrderBook',
        props: { symbol: 'RELIANCE' },
        span: { cols: 4, rows: 6 }
      },
      {
        id: 'ticker',
        component: 'MarketTicker',
        props: {},
        span: { cols: 12, rows: 1 }
      }
    ]
  },
  {
    id: 'scanner',
    name: 'Scanner View',
    components: [
      {
        id: 'search',
        component: 'SymbolSearch',
        props: {},
        span: { cols: 12, rows: 1 }
      },
      {
        id: 'scanner',
        component: 'MarketScanner',
        props: {},
        span: { cols: 12, rows: 7 }
      }
    ]
  },
  {
    id: 'analysis',
    name: 'Analysis View',
    components: [
      {
        id: 'search',
        component: 'SymbolSearch',
        props: {},
        span: { cols: 6, rows: 1 }
      },
      {
        id: 'chart1',
        component: 'AdvancedChart',
        props: { symbol: 'RELIANCE' },
        span: { cols: 6, rows: 4 }
      },
      {
        id: 'chart2',
        component: 'AdvancedChart',
        props: { symbol: 'TCS' },
        span: { cols: 6, rows: 4 }
      },
      {
        id: 'orderbook1',
        component: 'OrderBook',
        props: { symbol: 'RELIANCE' },
        span: { cols: 3, rows: 4 }
      },
      {
        id: 'orderbook2',
        component: 'OrderBook',
        props: { symbol: 'TCS' },
        span: { cols: 3, rows: 4 }
      }
    ]
  }
]

const PerformanceMonitor: React.FC<{
  connectionStatus: string
  isConnected: boolean
  subscriptions: number
  lastUpdate: Date | null
  marketData: Record<string, RealTimeMarketData>
}> = ({ connectionStatus, isConnected, subscriptions, lastUpdate, marketData }) => {
  const [performanceMetrics, setPerformanceMetrics] = useState({
    avgLatency: 0,
    updateRate: 0,
    memoryUsage: 0,
    renderTime: 0
  })

  // Simulate performance monitoring
  useEffect(() => {
    const interval = setInterval(() => {
      setPerformanceMetrics({
        avgLatency: 15 + Math.random() * 10, // 15-25ms
        updateRate: subscriptions * (0.8 + Math.random() * 0.4), // Updates per second
        memoryUsage: 45 + Math.random() * 15, // 45-60MB
        renderTime: 2 + Math.random() * 3 // 2-5ms
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [subscriptions])

  return (
    <div className="bg-slate-800/30 rounded-lg p-3 border border-slate-700/50">
      <div className="flex items-center justify-between mb-3">
        <h4 className="text-sm font-medium text-white">Performance Monitor</h4>
        <div className={`flex items-center space-x-2 ${
          isConnected ? 'text-green-400' : 'text-red-400'
        }`}>
          {isConnected ? (
            <Wifi className="w-4 h-4" />
          ) : (
            <WifiOff className="w-4 h-4" />
          )}
          <span className="text-xs capitalize">{connectionStatus}</span>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 text-xs">
        <div className="bg-slate-700/30 rounded p-2">
          <div className="text-slate-400 mb-1">Latency</div>
          <div className="font-mono text-white">
            {performanceMetrics.avgLatency.toFixed(1)}ms
          </div>
        </div>

        <div className="bg-slate-700/30 rounded p-2">
          <div className="text-slate-400 mb-1">Updates/sec</div>
          <div className="font-mono text-white">
            {performanceMetrics.updateRate.toFixed(1)}
          </div>
        </div>

        <div className="bg-slate-700/30 rounded p-2">
          <div className="text-slate-400 mb-1">Memory</div>
          <div className="font-mono text-white">
            {performanceMetrics.memoryUsage.toFixed(1)}MB
          </div>
        </div>

        <div className="bg-slate-700/30 rounded p-2">
          <div className="text-slate-400 mb-1">Render</div>
          <div className="font-mono text-white">
            {performanceMetrics.renderTime.toFixed(1)}ms
          </div>
        </div>
      </div>

      <div className="mt-3 pt-3 border-t border-slate-700/50">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-400">
            {subscriptions} active subscriptions
          </span>
          <span className="text-slate-400">
            {Object.keys(marketData).length} symbols
          </span>
        </div>
        
        {lastUpdate && (
          <div className="text-xs text-slate-500 mt-1">
            Last update: {lastUpdate.toLocaleTimeString()}
          </div>
        )}
      </div>
    </div>
  )
}

const ComponentRenderer: React.FC<{
  componentConfig: DashboardLayout['components'][0]
  selectedSymbol: string
  onSymbolSelect: (symbol: any) => void
}> = ({ componentConfig, selectedSymbol, onSymbolSelect }) => {
  const { component, props } = componentConfig

  switch (component) {
    case 'SymbolSearch':
      return (
        <EnhancedSymbolSearch
          onSelect={onSymbolSelect}
          placeholder="Search stocks, ETFs, crypto..."
          maxResults={10}
          showFilters={true}
          {...props}
        />
      )

    case 'AdvancedChart':
      return (
        <AdvancedChart
          symbol={selectedSymbol}
          data={[]} // Will be populated by real data
          chartType="candlestick"
          timeframe="15m"
          showVolume={true}
          showGrid={true}
          height={400}
          {...props}
        />
      )

    case 'OrderBook':
      return (
        <OrderBookVisualization
          symbol={selectedSymbol}
          maxLevels={10}
          showSpread={true}
          showVolume={true}
          precision={2}
          {...props}
        />
      )

    case 'MarketScanner':
      return <MarketScanner {...props} />

    case 'MarketTicker':
      return (
        <MarketDataTicker
          symbols={[]} // Will be populated by real data
          speed="normal"
          showChange={true}
          showVolume={false}
          pauseOnHover={true}
          {...props}
        />
      )

    default:
      return (
        <div className="flex items-center justify-center h-full text-slate-400 bg-slate-800/20 rounded-lg">
          <div className="text-center">
            <AlertCircle className="w-8 h-8 mx-auto mb-2" />
            <p>Unknown component: {component}</p>
          </div>
        </div>
      )
  }
}

export const EnhancedMarketDataDashboard: React.FC<EnhancedMarketDataDashboardProps> = ({
  className = ''
}) => {
  const [currentLayout, setCurrentLayout] = useState<DashboardLayout>(defaultLayouts[0])
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE')
  const [showPerformanceMonitor, setShowPerformanceMonitor] = useState(true)
  const [isFullscreen, setIsFullscreen] = useState(false)

  // Use our enhanced market data hooks
  const {
    marketData,
    subscriptions,
    isConnected,
    connectionStatus,
    lastUpdate,
    subscribe,
    unsubscribe,
    refreshData
  } = useEnhancedMarketData({
    symbols: [selectedSymbol, 'TCS', 'HDFCBANK', 'INFY'],
    updateInterval: 1000,
    enableRealtimeUpdates: true
  })

  const handleSymbolSelect = useCallback((symbolResult: any) => {
    const newSymbol = symbolResult.symbol
    setSelectedSymbol(newSymbol)
    subscribe(newSymbol)
  }, [subscribe])

  const handleLayoutChange = useCallback((layout: DashboardLayout) => {
    setCurrentLayout(layout)
  }, [])

  // Performance optimization: Memoize ticker symbols
  const tickerSymbols = useMemo(() => {
    return Object.values(marketData).map(data => ({
      symbol: data.symbol,
      name: `${data.symbol} Stock`,
      price: data.price,
      change: data.change,
      changePercent: data.changePercent,
      volume: data.volume,
      high: data.high,
      low: data.low,
      lastUpdated: data.lastTrade
    }))
  }, [marketData])

  // Auto-refresh data periodically
  useEffect(() => {
    const interval = setInterval(() => {
      if (isConnected) {
        refreshData()
      }
    }, 30000) // Refresh every 30 seconds

    return () => clearInterval(interval)
  }, [isConnected, refreshData])

  return (
    <div className={`space-y-6 ${className} ${isFullscreen ? 'fixed inset-0 z-50 bg-slate-900 p-6' : ''}`}>
      {/* Dashboard Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <Monitor className="w-6 h-6 text-purple-400" />
            <h1 className="text-2xl font-bold text-white">Market Data Dashboard</h1>
            <div className={`w-2 h-2 rounded-full ${
              isConnected ? 'bg-green-400 animate-pulse' : 'bg-red-400'
            }`} />
          </div>

          <div className="flex items-center space-x-2">
            {defaultLayouts.map(layout => (
              <button
                key={layout.id}
                onClick={() => handleLayoutChange(layout)}
                className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                  currentLayout.id === layout.id
                    ? 'bg-purple-500 text-white'
                    : 'bg-slate-700/50 text-slate-300 hover:bg-slate-600/50 hover:text-white'
                }`}
              >
                {layout.name}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => setShowPerformanceMonitor(!showPerformanceMonitor)}
            className={`cyber-button-sm p-2 ${
              showPerformanceMonitor ? 'bg-purple-500/20 text-purple-400' : ''
            }`}
            title="Performance Monitor"
          >
            <Activity className="w-4 h-4" />
          </button>

          <button
            onClick={refreshData}
            className="cyber-button-sm p-2"
            title="Refresh Data"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="cyber-button-sm p-2"
            title="Toggle Fullscreen"
          >
            {isFullscreen ? <Monitor className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
          </button>
        </div>
      </div>

      <div className="flex gap-6">
        {/* Main Dashboard Area */}
        <div className="flex-1">
          <div className="grid grid-cols-12 gap-6 auto-rows-fr">
            <AnimatePresence mode="wait">
              {currentLayout.components.map(componentConfig => (
                <motion.div
                  key={`${currentLayout.id}-${componentConfig.id}`}
                  className={`col-span-${componentConfig.span.cols} row-span-${componentConfig.span.rows}`}
                  style={{
                    gridColumn: `span ${componentConfig.span.cols}`,
                    gridRow: `span ${componentConfig.span.rows}`
                  }}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  transition={{ duration: 0.3, delay: 0.1 }}
                >
                  <div className="h-full">
                    <ComponentRenderer
                      componentConfig={componentConfig}
                      selectedSymbol={selectedSymbol}
                      onSymbolSelect={handleSymbolSelect}
                    />
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        </div>

        {/* Side Panel */}
        <AnimatePresence>
          {showPerformanceMonitor && (
            <motion.div
              className="w-80 space-y-4"
              initial={{ opacity: 0, x: 100 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 100 }}
              transition={{ duration: 0.3 }}
            >
              <PerformanceMonitor
                connectionStatus={connectionStatus}
                isConnected={isConnected}
                subscriptions={subscriptions.length}
                lastUpdate={lastUpdate}
                marketData={marketData}
              />

              {/* Active Subscriptions */}
              <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
                <h4 className="text-sm font-medium text-white mb-3">Active Subscriptions</h4>
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {subscriptions.map(sub => (
                    <div
                      key={sub.symbol}
                      className="flex items-center justify-between bg-slate-700/30 rounded p-2"
                    >
                      <div>
                        <div className="text-sm font-medium text-white">{sub.symbol}</div>
                        <div className="text-xs text-slate-400">
                          {sub.dataPoints} updates
                        </div>
                      </div>
                      <button
                        onClick={() => unsubscribe(sub.symbol)}
                        className="text-slate-400 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quick Actions */}
              <div className="bg-slate-800/30 rounded-lg p-4 border border-slate-700/50">
                <h4 className="text-sm font-medium text-white mb-3">Quick Actions</h4>
                <div className="space-y-2">
                  <button
                    onClick={() => {
                      ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK'].forEach(subscribe)
                    }}
                    className="w-full cyber-button-sm py-2 text-sm"
                  >
                    <Star className="w-4 h-4 mr-2" />
                    Subscribe to Top 5
                  </button>
                  
                  <button
                    onClick={() => {
                      Object.keys(marketData).forEach(unsubscribe)
                    }}
                    className="w-full bg-red-500/20 text-red-400 hover:bg-red-500/30 rounded-lg py-2 px-3 text-sm transition-colors"
                  >
                    <WifiOff className="w-4 h-4 mr-2" />
                    Unsubscribe All
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default EnhancedMarketDataDashboard