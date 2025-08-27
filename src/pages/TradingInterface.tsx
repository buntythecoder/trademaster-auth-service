import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Activity, 
  Zap, 
  Shield,
  BarChart3,
  TrendingUp,
  Settings,
  Smartphone,
  Monitor,
  Maximize2,
  Clock
} from 'lucide-react'
import {
  OrderForm,
  QuickTradeButtons,
  PositionManager,
  RiskMeter,
  OrderHistory,
  LivePositionTracker,
  AdvancedOrderForm,
  type OrderRequest,
  type RiskLimits,
  type BrokerAccount,
  type Position,
  type RiskLevel,
  type SectorExposure,
  type Order,
  type LivePosition,
  type AdvancedOrderRequest,
  type RiskCalculation
} from '@/components/trading'
import { useMarketData, useConnectionStatus, usePortfolio } from '@/hooks/useWebSocket'
import { MarketDataTicker, type MarketSymbol } from '@/components/market-data'

interface TradingInterfaceProps {
  initialSymbol?: string
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
        <BarChart3 className="h-4 w-4" />
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

const TradingHeader: React.FC<{
  viewMode: 'desktop' | 'tablet' | 'mobile'
  onViewModeChange: (mode: 'desktop' | 'tablet' | 'mobile') => void
  selectedSymbol: string
  onSymbolChange: (symbol: string) => void
  isFullscreen: boolean
  onToggleFullscreen: () => void
}> = ({ viewMode, onViewModeChange, selectedSymbol, onSymbolChange, isFullscreen, onToggleFullscreen }) => {
  const { isConnected, connectionStatus, lastUpdate } = useConnectionStatus()

  const popularSymbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK']

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
              <h1 className="text-2xl font-bold text-white">Trading Interface</h1>
              <div className="flex items-center space-x-4 text-sm text-slate-400">
                <span>Status: {connectionStatus}</span>
                {lastUpdate && (
                  <span>Last Update: {lastUpdate.toLocaleTimeString()}</span>
                )}
                <span>•</span>
                <span>Live Trading</span>
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Activity className={`h-5 w-5 ${isConnected ? 'text-green-400' : 'text-red-400'}`} />
            <Zap className="h-5 w-5 text-yellow-400" />
            <Shield className="h-5 w-5 text-blue-400" />
          </div>
        </div>

        <div className="flex items-center space-x-4">
          {/* Symbol Selector */}
          <div className="flex items-center space-x-2">
            <span className="text-sm text-slate-400">Symbol:</span>
            <select
              value={selectedSymbol}
              onChange={(e) => onSymbolChange(e.target.value)}
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              {popularSymbols.map(symbol => (
                <option key={symbol} value={symbol}>{symbol}</option>
              ))}
            </select>
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
            <div className="text-sm font-semibold text-white">Trading Hours</div>
            <div className="text-xs text-green-400">9:15 AM - 3:30 PM IST</div>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

export const TradingInterface: React.FC<TradingInterfaceProps> = ({
  initialSymbol = 'RELIANCE',
  layout = 'desktop'
}) => {
  const [viewMode, setViewMode] = useState<'desktop' | 'tablet' | 'mobile'>(layout)
  const [selectedSymbol, setSelectedSymbol] = useState(initialSymbol)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [activeTab, setActiveTab] = useState<'order' | 'quick' | 'positions' | 'risk'>('order')

  // WebSocket integration
  const symbols = ['RELIANCE', 'TCS', 'HDFCBANK', 'INFY', 'ICICIBANK']
  const { marketData } = useMarketData(symbols)
  const { portfolio } = usePortfolio()

  // Mock data for components
  const mockBrokerAccounts: BrokerAccount[] = [
    {
      id: 'zerodha-001',
      brokerName: 'ZERODHA',
      displayName: 'Zerodha Account (****1234)',
      balance: 500000,
      marginAvailable: 750000,
      status: 'ACTIVE'
    },
    {
      id: 'upstox-001',
      brokerName: 'UPSTOX',
      displayName: 'Upstox Account (****5678)',
      balance: 250000,
      marginAvailable: 375000,
      status: 'ACTIVE'
    }
  ]

  const mockRiskLimits: RiskLimits = {
    maxPositionSize: 1000000,
    maxPortfolioExposure: 80,
    minStopLossPercent: 5,
    maxOrderValue: 500000
  }

  const mockPositions: Position[] = [
    {
      symbol: 'RELIANCE',
      companyName: 'Reliance Industries Limited',
      quantity: 100,
      avgPrice: 2320.50,
      currentPrice: marketData['RELIANCE']?.price || 2456.75,
      pnl: 13625,
      pnlPercent: 5.87,
      marketValue: 245675,
      dayChange: marketData['RELIANCE']?.change || 34.50,
      dayChangePercent: marketData['RELIANCE']?.changePercent || 1.42,
      sector: 'Oil & Gas',
      lastUpdate: new Date()
    },
    {
      symbol: 'TCS',
      companyName: 'Tata Consultancy Services Limited',
      quantity: 50,
      avgPrice: 3850.25,
      currentPrice: marketData['TCS']?.price || 3789.40,
      pnl: -3042.5,
      pnlPercent: -1.58,
      marketValue: 189470,
      dayChange: marketData['TCS']?.change || -42.15,
      dayChangePercent: marketData['TCS']?.changePercent || -1.10,
      sector: 'Information Technology',
      lastUpdate: new Date()
    }
  ]

  const mockSectorExposure: SectorExposure[] = [
    { sector: 'Information Technology', exposure: 35.2, limit: 30 },
    { sector: 'Banking', exposure: 22.8, limit: 25 },
    { sector: 'Oil & Gas', exposure: 28.5, limit: 20 },
    { sector: 'Pharmaceuticals', exposure: 8.3, limit: 15 },
    { sector: 'Automobiles', exposure: 5.2, limit: 10 }
  ]

  // Get current symbol data
  const currentSymbolData = marketData[selectedSymbol]
  const currentPrice = currentSymbolData?.price || 2456.75
  const currentPosition = mockPositions.find(p => p.symbol === selectedSymbol)

  // Convert market data to ticker format
  const tickerSymbols: MarketSymbol[] = Object.entries(marketData).map(([symbol, data]) => ({
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

  function getSymbolName(symbol: string): string {
    const names: Record<string, string> = {
      'RELIANCE': 'Reliance Industries',
      'TCS': 'Tata Consultancy Services',
      'HDFCBANK': 'HDFC Bank',
      'INFY': 'Infosys Limited',
      'ICICIBANK': 'ICICI Bank',
      'WIPRO': 'Wipro Limited'
    }
    return names[symbol] || symbol
  }

  // Calculate portfolio exposure
  const totalPositionValue = mockPositions.reduce((sum, pos) => sum + pos.marketValue, 0)
  const totalBalance = mockBrokerAccounts.reduce((sum, acc) => sum + acc.balance, 0)
  const portfolioExposure = totalBalance > 0 ? (totalPositionValue / totalBalance) * 100 : 0
  
  const currentRisk: RiskLevel = 
    portfolioExposure < 20 ? 'LOW' :
    portfolioExposure < 50 ? 'MEDIUM' :
    portfolioExposure < 80 ? 'HIGH' : 'CRITICAL'

  // Layout configurations
  const getLayoutClasses = () => {
    switch (viewMode) {
      case 'mobile':
        return {
          container: 'flex flex-col space-y-4',
          mainSection: 'w-full',
          sidePanel: 'w-full',
          tabContainer: 'block',
          tickerContainer: 'w-full'
        }
      case 'tablet':
        return {
          container: 'flex flex-col space-y-6',
          mainSection: 'w-full',
          sidePanel: 'w-full grid grid-cols-2 gap-6',
          tabContainer: 'hidden',
          tickerContainer: 'w-full'
        }
      default: // desktop
        return {
          container: 'flex space-x-6',
          mainSection: 'flex-1 space-y-6',
          sidePanel: 'w-96 space-y-6',
          tabContainer: 'hidden',
          tickerContainer: 'w-full'
        }
    }
  }

  const layout_classes = getLayoutClasses()

  const handleOrderSubmit = async (order: OrderRequest) => {
    console.log('Submitting order:', order)
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000))
    console.log('Order submitted successfully!')
  }

  const handleQuickTrade = async (side: 'BUY' | 'SELL', amount: number) => {
    console.log(`Quick ${side} order for ₹${amount}`)
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1500))
    console.log('Quick trade executed!')
  }

  const handleClosePosition = (symbol: string, quantity?: number) => {
    console.log(`Closing position for ${symbol}`, quantity ? `(${quantity} shares)` : '(all)')
  }

  const handleSetStopLoss = (symbol: string, stopPrice: number) => {
    console.log(`Setting stop loss for ${symbol} at ₹${stopPrice}`)
  }

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
        {/* Trading Header */}
        <TradingHeader
          viewMode={viewMode}
          onViewModeChange={setViewMode}
          selectedSymbol={selectedSymbol}
          onSymbolChange={setSelectedSymbol}
          isFullscreen={isFullscreen}
          onToggleFullscreen={handleToggleFullscreen}
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

        {/* Mobile Tab Navigation */}
        <AnimatePresence>
          {viewMode === 'mobile' && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="mb-6 glass-widget-card rounded-2xl p-4"
            >
              <div className="grid grid-cols-5 gap-1">
                {[
                  { id: 'order', label: 'Order', icon: BarChart3 },
                  { id: 'positions', label: 'Live', icon: Activity },
                  { id: 'history', label: 'History', icon: Clock },
                  { id: 'quick', label: 'Quick', icon: Zap },
                  { id: 'risk', label: 'Risk', icon: Shield }
                ].map((tab) => (
                  <motion.button
                    key={tab.id}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`h-16 rounded-xl flex flex-col items-center justify-center space-y-1 transition-all duration-300 ${
                      activeTab === tab.id
                        ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/30'
                        : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white'
                    }`}
                  >
                    <tab.icon className="h-5 w-5" />
                    <span className="text-xs font-medium">{tab.label}</span>
                  </motion.button>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Main Content */}
        <div className={layout_classes.container}>
          {/* Main Section */}
          <div className={layout_classes.mainSection}>
            {/* Desktop/Tablet Layout */}
            {viewMode !== 'mobile' && (
              <>
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                  {/* Enhanced Order Form */}
                  <AdvancedOrderForm
                    symbol={selectedSymbol}
                    currentPrice={currentPrice}
                    availableBalance={mockBrokerAccounts[0]?.balance || 500000}
                    brokers={mockBrokerAccounts.map(acc => acc.broker)}
                    onSubmitOrder={(order) => {
                      console.log('Advanced order submitted:', order)
                      // Handle order submission
                    }}
                    onCalculateRisk={(params) => {
                      // Mock risk calculation
                      return {
                        maxLoss: params.quantity * params.price * 0.1,
                        maxGain: params.quantity * params.price * 0.15,
                        riskRewardRatio: 1.5,
                        marginRequired: params.quantity * params.price,
                        leverage: 1,
                        positionSize: params.quantity * params.price
                      }
                    }}
                    marketHours={isConnected}
                  />

                  <QuickTradeButtons
                    symbol={selectedSymbol}
                    currentPrice={currentPrice}
                    presetAmounts={[10000, 25000, 50000]}
                    defaultAmount={25000}
                    availableBalance={mockBrokerAccounts[0]?.balance || 500000}
                    currentPosition={currentPosition}
                    quickOrderType="MARKET"
                    riskWarnings={true}
                    onQuickTrade={handleQuickTrade}
                  />
                </div>

                {/* Live Position Tracker */}
                <div className="mb-6">
                  <LivePositionTracker
                    onRefresh={() => console.log('Refreshing positions')}
                    onClosePosition={(positionId) => console.log('Close position:', positionId)}
                    onSetStopLoss={(positionId, price) => console.log('Set stop loss:', positionId, price)}
                    onSetTarget={(positionId, price) => console.log('Set target:', positionId, price)}
                    showRiskAnalysis={true}
                    compactMode={viewMode === 'tablet'}
                  />
                </div>

                {/* Order History */}
                <div>
                  <OrderHistory
                    onRefresh={() => console.log('Refreshing order history')}
                    showBrokerFilter={true}
                    showPnLSummary={true}
                    compactMode={viewMode === 'tablet'}
                  />
                </div>
              </>
            )}

            {/* Mobile Layout - Tab Content */}
            {viewMode === 'mobile' && (
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
                      symbol={selectedSymbol}
                      currentPrice={currentPrice}
                      availableBalance={mockBrokerAccounts[0]?.balance || 500000}
                      brokers={mockBrokerAccounts.map(acc => acc.broker)}
                      onSubmitOrder={(order) => {
                        console.log('Mobile order submitted:', order)
                      }}
                      onCalculateRisk={(params) => ({
                        maxLoss: params.quantity * params.price * 0.1,
                        maxGain: params.quantity * params.price * 0.15,
                        riskRewardRatio: 1.5,
                        marginRequired: params.quantity * params.price,
                        leverage: 1,
                        positionSize: params.quantity * params.price
                      })}
                      marketHours={isConnected}
                      compactMode={true}
                    />
                  )}

                  {activeTab === 'positions' && (
                    <LivePositionTracker
                      onRefresh={() => console.log('Mobile: Refreshing positions')}
                      onClosePosition={(positionId) => console.log('Mobile: Close position:', positionId)}
                      onSetStopLoss={(positionId, price) => console.log('Mobile: Set stop loss:', positionId, price)}
                      onSetTarget={(positionId, price) => console.log('Mobile: Set target:', positionId, price)}
                      showRiskAnalysis={true}
                      compactMode={true}
                    />
                  )}

                  {activeTab === 'history' && (
                    <OrderHistory
                      onRefresh={() => console.log('Mobile: Refreshing order history')}
                      showBrokerFilter={true}
                      showPnLSummary={false}
                      compactMode={true}
                    />
                  )}

                  {activeTab === 'quick' && (
                    <QuickTradeButtons
                      symbol={selectedSymbol}
                      currentPrice={currentPrice}
                      presetAmounts={[10000, 25000, 50000]}
                      defaultAmount={25000}
                      availableBalance={mockBrokerAccounts[0]?.balance || 500000}
                      currentPosition={currentPosition}
                      quickOrderType="MARKET"
                      riskWarnings={true}
                      onQuickTrade={handleQuickTrade}
                    />
                  )}

                  {activeTab === 'risk' && (
                    <RiskMeter
                      currentRisk={currentRisk}
                      portfolioExposure={portfolioExposure}
                      sectorConcentration={mockSectorExposure}
                      dayTradingLimit={100000}
                      marginUtilization={45}
                      maxDrawdown={8.2}
                      totalPositions={mockPositions.length}
                      availableBalance={totalBalance}
                      usedMargin={150000}
                    />
                  )}
                </motion.div>
              </AnimatePresence>
            )}
          </div>

          {/* Side Panel (Desktop & Tablet only) */}
          {viewMode !== 'mobile' && (
            <div className={layout_classes.sidePanel}>
              <PositionManager
                positions={mockPositions}
                onClosePosition={handleClosePosition}
                onModifyPosition={(symbol) => console.log('Modify:', symbol)}
                onSetAlert={(symbol) => console.log('Alert:', symbol)}
                onSetStopLoss={handleSetStopLoss}
                compactMode={viewMode === 'tablet'}
              />

              <RiskMeter
                currentRisk={currentRisk}
                portfolioExposure={portfolioExposure}
                sectorConcentration={mockSectorExposure}
                dayTradingLimit={100000}
                marginUtilization={45}
                maxDrawdown={8.2}
                totalPositions={mockPositions.length}
                availableBalance={totalBalance}
                usedMargin={150000}
              />
            </div>
          )}
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
                {mockPositions.filter(p => p.pnl > 0).length}
              </div>
              <div className="text-slate-400">Profitable</div>
            </div>
            <div>
              <div className="text-red-400 font-bold text-lg">
                {mockPositions.filter(p => p.pnl < 0).length}
              </div>
              <div className="text-slate-400">In Loss</div>
            </div>
            <div>
              <div className="text-cyan-400 font-bold text-lg">
                ₹{totalPositionValue.toLocaleString('en-IN')}
              </div>
              <div className="text-slate-400">Total Value</div>
            </div>
            <div>
              <div className="text-purple-400 font-bold text-lg">
                {currentRisk}
              </div>
              <div className="text-slate-400">Risk Level</div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}