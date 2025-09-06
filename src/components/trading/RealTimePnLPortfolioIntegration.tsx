import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, TrendingDown, DollarSign, Calendar, BarChart3, PieChart, 
  Target, Clock, Activity, Building2, ArrowUpRight, ArrowDownRight,
  Percent, Filter, RefreshCw, Eye, EyeOff, ChevronRight, AlertTriangle,
  Shield, Calculator, Coins, Scale, Zap, Users, MapPin, Award,
  Bell, Settings, Download, Upload, Share, Bookmark, Hash,
  Gauge, Timer, TrendingDown as SlippageIcon, Database
} from 'lucide-react'
import { BrokerConnection, BrokerPosition, IndianBrokerType } from '../../services/brokerService'

// FRONT-018: Enhanced Types for Real-time P&L & Portfolio Integration
interface TaxCalculation {
  symbol: string
  brokerId: string
  brokerName: string
  transactionType: 'buy' | 'sell'
  quantity: number
  price: number
  value: number
  // Tax Components
  stt: number // Securities Transaction Tax
  exchangeCharges: number
  sebiCharges: number
  stampDuty: number
  brokerage: number
  gst: number // GST on brokerage
  totalTax: number
  netAmount: number
  // Tax Rates
  sttRate: number
  brokerageRate: number
  gstRate: number
}

interface MarginUtilization {
  brokerId: string
  brokerName: string
  totalMargin: number
  usedMargin: number
  availableMargin: number
  utilizationPercent: number
  marginType: 'cash' | 'collateral' | 'combined'
  pledgedSecurities: number
  exposureMultiple: number
  dayTradingPower: number
  marginCallLevel: number
  liquidationLevel: number
  riskStatus: 'safe' | 'warning' | 'danger' | 'margin_call'
}

interface PerformanceAttribution {
  brokerId: string
  brokerName: string
  securitySelection: number // Performance due to stock picking
  allocationEffect: number // Performance due to position sizing
  interactionEffect: number // Combined effect
  totalAttribution: number
  benchmarkReturn: number
  portfolioReturn: number
  excessReturn: number
  informationRatio: number
  sharpeRatio: number
  maxDrawdown: number
  winRate: number // % of profitable trades
  avgWinSize: number
  avgLossSize: number
  profitFactor: number // Gross profit / Gross loss
}

interface RiskMetrics {
  portfolioValue: number
  var95: number // Value at Risk 95%
  var99: number // Value at Risk 99%
  expectedShortfall: number
  portfolioBeta: number
  portfolioVolatility: number
  concentrationRisk: number // Largest position as % of portfolio
  sectorConcentration: Map<string, number>
  correlationRisk: number
  liquidityRisk: number
  marginRisk: number
  drawdownFromPeak: number
  riskScore: number // 0-100
  riskLevel: 'low' | 'moderate' | 'high' | 'extreme'
}

interface PortfolioRebalancing {
  currentAllocation: Map<string, number>
  targetAllocation: Map<string, number>
  rebalanceRequired: boolean
  rebalanceAmount: number
  suggestedTrades: {
    symbol: string
    broker: string
    action: 'buy' | 'sell'
    quantity: number
    reason: string
    priority: 'high' | 'medium' | 'low'
  }[]
  rebalanceScore: number // How far from target
  estimatedCost: number
  estimatedImpact: number
}

interface TaxOptimization {
  currentYear: number
  realizableGains: number
  realizableLosses: number
  netPosition: number
  taxImplication: number
  suggestions: {
    action: 'harvest_loss' | 'defer_gain' | 'convert_stcg_to_ltcg' | 'switch_broker'
    description: string
    potentialSaving: number
    risk: 'low' | 'medium' | 'high'
    deadline?: Date
    affectedPositions: string[]
  }[]
  stcgTax: number // Short term capital gains
  ltcgTax: number // Long term capital gains
  totalTaxLiability: number
  optimizationPotential: number
}

interface RealTimePnLPortfolioProps {
  positions: BrokerPosition[]
  brokers: BrokerConnection[]
  onRefresh?: () => void
  isLoading?: boolean
  className?: string
}

export const RealTimePnLPortfolioIntegration: React.FC<RealTimePnLPortfolioProps> = ({
  positions,
  brokers,
  onRefresh,
  isLoading = false,
  className = ''
}) => {
  // State Management
  const [activeTab, setActiveTab] = useState<'overview' | 'tax' | 'margin' | 'attribution' | 'risk' | 'rebalance' | 'optimization'>('overview')
  const [selectedTimeframe, setSelectedTimeframe] = useState<'1d' | '1w' | '1m' | '3m' | '1y' | 'ytd' | 'all'>('1d')
  const [selectedBroker, setSelectedBroker] = useState<string | 'all'>('all')
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [refreshInterval, setRefreshInterval] = useState(30) // seconds
  const [showAdvancedMetrics, setShowAdvancedMetrics] = useState(false)
  const [notification, setNotification] = useState<{type: 'success' | 'warning' | 'error' | 'info', message: string} | null>(null)

  // Real-time updates
  useEffect(() => {
    if (autoRefresh && onRefresh) {
      const interval = setInterval(onRefresh, refreshInterval * 1000)
      return () => clearInterval(interval)
    }
  }, [autoRefresh, refreshInterval, onRefresh])

  // Broker configurations for display
  const brokerConfigs = {
    zerodha: { name: 'Zerodha Kite', icon: '🚀', color: 'blue', marginMultiple: 4 },
    upstox: { name: 'Upstox Pro', icon: '📈', color: 'green', marginMultiple: 3.5 },
    angel_one: { name: 'Angel One', icon: '👼', color: 'orange', marginMultiple: 3 },
    icici_direct: { name: 'ICICI Direct', icon: '🏦', color: 'purple', marginMultiple: 2.5 },
    groww: { name: 'Groww', icon: '🌱', color: 'emerald', marginMultiple: 2 },
    iifl: { name: 'IIFL Securities', icon: '💼', color: 'indigo', marginMultiple: 3 }
  }

  // Calculate comprehensive tax breakdown for all positions
  const taxCalculations = useMemo(() => {
    const calculations: TaxCalculation[] = []
    
    positions.forEach(position => {
      const broker = brokers.find(b => b.id === position.brokerId)
      if (!broker) return

      const value = position.quantity * position.currentPrice
      const brokerageRate = 0.0003 // 0.03% (example)
      
      // Calculate brokerage
      const brokerage = Math.min(value * brokerageRate, 20) // Max ₹20 per order
      
      // Calculate STT (Securities Transaction Tax)
      const sttRate = 0.001 // 0.1% for delivery, 0.025% for intraday
      const stt = value * sttRate
      
      // Calculate other charges
      const exchangeCharges = value * 0.0000345 // NSE: 0.00345%
      const sebiCharges = value * 0.000001 // 0.0001%
      const stampDuty = value * 0.000015 // 0.0015%
      const gstRate = 0.18
      const gst = brokerage * gstRate
      
      const totalTax = stt + exchangeCharges + sebiCharges + stampDuty + brokerage + gst
      
      calculations.push({
        symbol: position.symbol,
        brokerId: position.brokerId,
        brokerName: position.brokerName,
        transactionType: 'sell', // Assuming sell for P&L calculation
        quantity: position.quantity,
        price: position.currentPrice,
        value,
        stt,
        exchangeCharges,
        sebiCharges,
        stampDuty,
        brokerage,
        gst,
        totalTax,
        netAmount: value - totalTax,
        sttRate,
        brokerageRate,
        gstRate
      })
    })
    
    return calculations
  }, [positions, brokers])

  // Calculate margin utilization across brokers
  const marginUtilizations = useMemo(() => {
    return brokers.map(broker => {
      const config = brokerConfigs[broker.brokerType as keyof typeof brokerConfigs]
      const brokerPositions = positions.filter(p => p.brokerId === broker.id)
      const totalValue = brokerPositions.reduce((sum, p) => sum + (p.quantity * p.currentPrice), 0)
      
      const totalMargin = broker.balance?.totalBalance || 100000
      const usedMargin = totalValue / (config?.marginMultiple || 3)
      const availableMargin = totalMargin - usedMargin
      const utilizationPercent = (usedMargin / totalMargin) * 100
      
      let riskStatus: MarginUtilization['riskStatus'] = 'safe'
      if (utilizationPercent > 90) riskStatus = 'margin_call'
      else if (utilizationPercent > 80) riskStatus = 'danger'
      else if (utilizationPercent > 60) riskStatus = 'warning'

      return {
        brokerId: broker.id,
        brokerName: broker.displayName,
        totalMargin,
        usedMargin,
        availableMargin,
        utilizationPercent,
        marginType: 'combined' as const,
        pledgedSecurities: totalValue * 0.3, // Assume 30% pledged
        exposureMultiple: config?.marginMultiple || 3,
        dayTradingPower: totalMargin * (config?.marginMultiple || 3),
        marginCallLevel: totalMargin * 0.8,
        liquidationLevel: totalMargin * 0.9,
        riskStatus
      } as MarginUtilization
    })
  }, [brokers, positions, brokerConfigs])

  // Calculate performance attribution
  const performanceAttributions = useMemo(() => {
    return brokers.map(broker => {
      const brokerPositions = positions.filter(p => p.brokerId === broker.id)
      const totalPnl = brokerPositions.reduce((sum, p) => sum + p.pnl, 0)
      const totalValue = brokerPositions.reduce((sum, p) => sum + (p.quantity * p.currentPrice), 0)
      
      const portfolioReturn = totalValue > 0 ? (totalPnl / totalValue) * 100 : 0
      const benchmarkReturn = 12 // Assume 12% benchmark return
      const excessReturn = portfolioReturn - benchmarkReturn
      
      const profitablePositions = brokerPositions.filter(p => p.pnl > 0)
      const losingPositions = brokerPositions.filter(p => p.pnl < 0)
      
      const winRate = brokerPositions.length > 0 ? (profitablePositions.length / brokerPositions.length) * 100 : 0
      const avgWinSize = profitablePositions.length > 0 ? 
        profitablePositions.reduce((sum, p) => sum + p.pnl, 0) / profitablePositions.length : 0
      const avgLossSize = losingPositions.length > 0 ? 
        Math.abs(losingPositions.reduce((sum, p) => sum + p.pnl, 0) / losingPositions.length) : 0

      return {
        brokerId: broker.id,
        brokerName: broker.displayName,
        securitySelection: portfolioReturn * 0.6, // 60% attributed to stock selection
        allocationEffect: portfolioReturn * 0.3, // 30% to allocation
        interactionEffect: portfolioReturn * 0.1, // 10% interaction
        totalAttribution: portfolioReturn,
        benchmarkReturn,
        portfolioReturn,
        excessReturn,
        informationRatio: excessReturn / 15, // Assuming 15% tracking error
        sharpeRatio: portfolioReturn / 20, // Assuming 20% volatility
        maxDrawdown: -8.5, // Example drawdown
        winRate,
        avgWinSize,
        avgLossSize,
        profitFactor: avgLossSize > 0 ? avgWinSize / avgLossSize : 0
      } as PerformanceAttribution
    })
  }, [brokers, positions])

  // Calculate comprehensive risk metrics
  const riskMetrics = useMemo(() => {
    const totalValue = positions.reduce((sum, p) => sum + (p.quantity * p.currentPrice), 0)
    const totalPnl = positions.reduce((sum, p) => sum + p.pnl, 0)
    const portfolioReturn = totalValue > 0 ? (totalPnl / totalValue) * 100 : 0
    
    // Calculate concentration risk
    const positionValues = positions.map(p => p.quantity * p.currentPrice)
    const maxPosition = Math.max(...positionValues)
    const concentrationRisk = totalValue > 0 ? (maxPosition / totalValue) * 100 : 0
    
    // Risk score calculation (0-100)
    let riskScore = 0
    riskScore += Math.min(concentrationRisk, 40) // Max 40 points for concentration
    riskScore += Math.min(Math.abs(portfolioReturn) / 2, 30) // Max 30 points for volatility
    riskScore += marginUtilizations.reduce((sum, m) => sum + m.utilizationPercent, 0) / marginUtilizations.length / 3 // Max 30 points for margin
    
    let riskLevel: RiskMetrics['riskLevel'] = 'low'
    if (riskScore > 75) riskLevel = 'extreme'
    else if (riskScore > 50) riskLevel = 'high'
    else if (riskScore > 25) riskLevel = 'moderate'

    return {
      portfolioValue: totalValue,
      var95: totalValue * 0.05, // 5% VaR
      var99: totalValue * 0.02, // 2% VaR
      expectedShortfall: totalValue * 0.07,
      portfolioBeta: 1.2,
      portfolioVolatility: 22,
      concentrationRisk,
      sectorConcentration: new Map([
        ['Technology', 35],
        ['Banking', 25],
        ['Healthcare', 15],
        ['Consumer', 25]
      ]),
      correlationRisk: 0.65,
      liquidityRisk: 15,
      marginRisk: marginUtilizations.reduce((sum, m) => sum + m.utilizationPercent, 0) / marginUtilizations.length,
      drawdownFromPeak: -12.5,
      riskScore,
      riskLevel
    } as RiskMetrics
  }, [positions, marginUtilizations])

  // Show notifications
  const showNotification = (type: 'success' | 'warning' | 'error' | 'info', message: string) => {
    setNotification({ type, message })
    setTimeout(() => setNotification(null), 5000)
  }

  // Portfolio overview calculations
  const portfolioOverview = useMemo(() => {
    const totalValue = positions.reduce((sum, p) => sum + (p.quantity * p.currentPrice), 0)
    const totalPnl = positions.reduce((sum, p) => sum + p.pnl, 0)
    const dayPnl = positions.reduce((sum, p) => sum + p.dayPnl, 0)
    const totalTax = taxCalculations.reduce((sum, t) => sum + t.totalTax, 0)
    
    return {
      totalValue,
      totalPnl,
      totalPnlPercent: totalValue > 0 ? (totalPnl / totalValue) * 100 : 0,
      dayPnl,
      dayPnlPercent: totalValue > 0 ? (dayPnl / totalValue) * 100 : 0,
      totalTax,
      netPnl: totalPnl - totalTax,
      positionCount: positions.length,
      profitablePositions: positions.filter(p => p.pnl > 0).length,
      losingPositions: positions.filter(p => p.pnl < 0).length
    }
  }, [positions, taxCalculations])

  const tabs = [
    { id: 'overview', label: 'Portfolio Overview', icon: BarChart3, count: positions.length },
    { id: 'tax', label: 'Tax Calculator', icon: Calculator, count: taxCalculations.length },
    { id: 'margin', label: 'Margin Monitor', icon: Shield, count: marginUtilizations.length },
    { id: 'attribution', label: 'Performance', icon: Target, count: performanceAttributions.length },
    { id: 'risk', label: 'Risk Metrics', icon: AlertTriangle, count: 0 },
    { id: 'rebalance', label: 'Rebalancing', icon: Scale, count: 0 },
    { id: 'optimization', label: 'Tax Optimization', icon: Zap, count: 0 }
  ]

  return (
    <div className={`min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 p-6 ${className}`}>
      {/* Notification */}
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -50 }}
            className={`fixed top-4 right-4 z-50 p-4 rounded-xl shadow-lg max-w-md ${
              notification.type === 'success' ? 'bg-green-500/20 border border-green-500/30 text-green-400' :
              notification.type === 'error' ? 'bg-red-500/20 border border-red-500/30 text-red-400' :
              notification.type === 'warning' ? 'bg-yellow-500/20 border border-yellow-500/30 text-yellow-400' :
              'bg-blue-500/20 border border-blue-500/30 text-blue-400'
            }`}
          >
            <div className="flex items-center space-x-2">
              <Bell className="w-5 h-5" />
              <span className="font-medium">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-400 to-cyan-400 bg-clip-text text-transparent mb-2">
              Real-time P&L & Portfolio Integration
            </h1>
            <p className="text-slate-400 text-lg">
              Comprehensive portfolio analysis with tax optimization and risk management
            </p>
          </div>
          
          <div className="flex items-center gap-4">
            {/* Auto-refresh toggle */}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setAutoRefresh(!autoRefresh)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl transition-all ${
                autoRefresh 
                  ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                  : 'bg-slate-800/50 text-slate-400 border border-slate-700/50'
              }`}
            >
              <RefreshCw className={`w-4 h-4 ${autoRefresh ? 'animate-spin' : ''}`} />
              <span className="text-sm">Auto-refresh</span>
            </motion.button>
            
            {/* Manual refresh */}
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={onRefresh}
              disabled={isLoading}
              className="p-3 bg-slate-800/50 text-slate-400 hover:text-white hover:bg-slate-700/50 rounded-xl border border-slate-700/50 transition-all disabled:opacity-50"
            >
              <RefreshCw className={`w-5 h-5 ${isLoading ? 'animate-spin' : ''}`} />
            </motion.button>
          </div>
        </div>

        {/* Key Portfolio Metrics */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
          <div className="glass-card p-6 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500/20 to-blue-600/20">
                <DollarSign className="h-6 w-6 text-blue-400" />
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold text-white">₹{(portfolioOverview.totalValue / 1000).toFixed(0)}K</div>
              </div>
            </div>
            <h3 className="text-blue-400 font-semibold mb-1">Portfolio Value</h3>
            <p className="text-slate-400 text-sm">{portfolioOverview.positionCount} positions</p>
          </div>

          <div className="glass-card p-6 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <div className={`p-3 rounded-xl bg-gradient-to-br ${
                portfolioOverview.totalPnl >= 0 ? 'from-green-500/20 to-green-600/20' : 'from-red-500/20 to-red-600/20'
              }`}>
                {portfolioOverview.totalPnl >= 0 ? <TrendingUp className="h-6 w-6 text-green-400" /> : <TrendingDown className="h-6 w-6 text-red-400" />}
              </div>
              <div className="text-right">
                <div className={`text-2xl font-bold ${portfolioOverview.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                  {portfolioOverview.totalPnl >= 0 ? '+' : ''}₹{(portfolioOverview.totalPnl / 1000).toFixed(1)}K
                </div>
              </div>
            </div>
            <h3 className={`font-semibold mb-1 ${portfolioOverview.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>Total P&L</h3>
            <p className="text-slate-400 text-sm">{portfolioOverview.totalPnlPercent.toFixed(2)}% return</p>
          </div>

          <div className="glass-card p-6 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <div className={`p-3 rounded-xl bg-gradient-to-br ${
                portfolioOverview.dayPnl >= 0 ? 'from-emerald-500/20 to-emerald-600/20' : 'from-orange-500/20 to-orange-600/20'
              }`}>
                <Activity className="h-6 w-6 text-emerald-400" />
              </div>
              <div className="text-right">
                <div className={`text-2xl font-bold ${portfolioOverview.dayPnl >= 0 ? 'text-emerald-400' : 'text-orange-400'}`}>
                  {portfolioOverview.dayPnl >= 0 ? '+' : ''}₹{(portfolioOverview.dayPnl / 1000).toFixed(1)}K
                </div>
              </div>
            </div>
            <h3 className="text-emerald-400 font-semibold mb-1">Day P&L</h3>
            <p className="text-slate-400 text-sm">{portfolioOverview.dayPnlPercent.toFixed(2)}% today</p>
          </div>

          <div className="glass-card p-6 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500/20 to-purple-600/20">
                <Calculator className="h-6 w-6 text-purple-400" />
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold text-white">₹{(portfolioOverview.totalTax / 1000).toFixed(1)}K</div>
              </div>
            </div>
            <h3 className="text-purple-400 font-semibold mb-1">Tax Liability</h3>
            <p className="text-slate-400 text-sm">STT + charges</p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="mb-8">
        <div className="flex flex-wrap gap-2 p-1 bg-slate-800/50 rounded-xl border border-slate-700/50">
          {tabs.map(tab => {
            const Icon = tab.icon
            return (
              <motion.button
                key={tab.id}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center space-x-2 px-4 py-3 rounded-lg font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                    : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="whitespace-nowrap">{tab.label}</span>
                {tab.count > 0 && (
                  <span className="px-2 py-1 text-xs bg-slate-700/50 rounded-full">
                    {tab.count}
                  </span>
                )}
              </motion.button>
            )
          })}
        </div>
      </div>

      {/* Content Area */}
      <div className="space-y-6">
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            {/* Portfolio Overview Tab */}
            {activeTab === 'overview' && (
              <PortfolioOverviewTab 
                positions={positions}
                brokers={brokers}
                portfolioOverview={portfolioOverview}
                selectedTimeframe={selectedTimeframe}
                onTimeframeChange={setSelectedTimeframe}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
              />
            )}
            
            {/* Tax Calculator Tab */}
            {activeTab === 'tax' && (
              <TaxCalculatorTab 
                taxCalculations={taxCalculations}
                positions={positions}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
                onNotification={showNotification}
              />
            )}
            
            {/* Margin Monitor Tab */}
            {activeTab === 'margin' && (
              <MarginMonitorTab 
                marginUtilizations={marginUtilizations}
                brokers={brokers}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
                onNotification={showNotification}
              />
            )}
            
            {/* Performance Attribution Tab */}
            {activeTab === 'attribution' && (
              <PerformanceAttributionTab 
                performanceAttributions={performanceAttributions}
                positions={positions}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
              />
            )}
            
            {/* Risk Metrics Tab */}
            {activeTab === 'risk' && (
              <RiskMetricsTab 
                riskMetrics={riskMetrics}
                positions={positions}
                brokers={brokers}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
                onNotification={showNotification}
              />
            )}
            
            {/* Portfolio Rebalancing Tab */}
            {activeTab === 'rebalance' && (
              <PortfolioRebalancingTab 
                positions={positions}
                brokers={brokers}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
                onNotification={showNotification}
              />
            )}
            
            {/* Tax Optimization Tab */}
            {activeTab === 'optimization' && (
              <TaxOptimizationTab 
                positions={positions}
                taxCalculations={taxCalculations}
                brokerConfigs={brokerConfigs}
                loading={isLoading}
                onNotification={showNotification}
              />
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}

// FRONT-018: Portfolio Overview Tab Component
const PortfolioOverviewTab: React.FC<{
  positions: BrokerPosition[]
  brokers: BrokerConnection[]
  portfolioOverview: any
  selectedTimeframe: string
  onTimeframeChange: (timeframe: any) => void
  brokerConfigs: any
  loading: boolean
}> = ({ positions, brokers, portfolioOverview, selectedTimeframe, onTimeframeChange, brokerConfigs, loading }) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Clock className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  // Group positions by broker
  const brokerGroups = useMemo(() => {
    const groups = new Map()
    brokers.forEach(broker => {
      const brokerPositions = positions.filter(p => p.brokerId === broker.id)
      const totalValue = brokerPositions.reduce((sum, p) => sum + (p.quantity * p.currentPrice), 0)
      const totalPnl = brokerPositions.reduce((sum, p) => sum + p.pnl, 0)
      const dayPnl = brokerPositions.reduce((sum, p) => sum + p.dayPnl, 0)
      
      groups.set(broker.id, {
        broker,
        positions: brokerPositions,
        totalValue,
        totalPnl,
        totalPnlPercent: totalValue > 0 ? (totalPnl / totalValue) * 100 : 0,
        dayPnl,
        dayPnlPercent: totalValue > 0 ? (dayPnl / totalValue) * 100 : 0,
        positionCount: brokerPositions.length
      })
    })
    return groups
  }, [positions, brokers])

  return (
    <div className="space-y-6">
      {/* Timeframe Selector */}
      <div className="flex gap-2">
        {['1d', '1w', '1m', '3m', '1y', 'ytd'].map(timeframe => (
          <motion.button
            key={timeframe}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => onTimeframeChange(timeframe)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              selectedTimeframe === timeframe
                ? 'bg-gradient-to-r from-purple-500 to-cyan-500 text-white shadow-lg'
                : 'bg-slate-800/50 text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
          >
            {timeframe.toUpperCase()}
          </motion.button>
        ))}
      </div>

      {/* Broker-wise Breakdown */}
      <div className="grid gap-6">
        {Array.from(brokerGroups.values()).map(group => (
          <div key={group.broker.id} className="glass-card p-6 rounded-2xl">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{brokerConfigs[group.broker.brokerType]?.icon || '🏢'}</span>
                <div>
                  <h3 className="text-white font-bold text-lg">{group.broker.displayName}</h3>
                  <p className="text-slate-400 text-sm">{group.positionCount} positions</p>
                </div>
              </div>
              
              <div className="text-right">
                <div className="text-white font-bold text-xl">₹{(group.totalValue / 1000).toFixed(0)}K</div>
                <div className={`text-sm font-medium ${
                  group.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'
                }`}>
                  {group.totalPnl >= 0 ? '+' : ''}₹{(group.totalPnl / 1000).toFixed(1)}K ({group.totalPnlPercent.toFixed(1)}%)
                </div>
              </div>
            </div>
            
            {/* Position Details */}
            <div className="grid gap-2 max-h-60 overflow-y-auto custom-scrollbar">
              {group.positions.map((position: any) => (
                <div key={`${position.brokerId}-${position.symbol}`} className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg">
                  <div>
                    <div className="text-white font-semibold">{position.symbol}</div>
                    <div className="text-slate-400 text-sm">{position.quantity} @ ₹{position.currentPrice.toFixed(2)}</div>
                  </div>
                  
                  <div className="text-right">
                    <div className="text-white font-medium">₹{((position.quantity * position.currentPrice) / 1000).toFixed(1)}K</div>
                    <div className={`text-sm ${
                      position.pnl >= 0 ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {position.pnl >= 0 ? '+' : ''}₹{(position.pnl / 1000).toFixed(1)}K ({position.pnlPercent.toFixed(1)}%)
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// FRONT-018: Tax Calculator Tab Component
const TaxCalculatorTab: React.FC<{
  taxCalculations: TaxCalculation[]
  positions: BrokerPosition[]
  brokerConfigs: any
  loading: boolean
  onNotification: (type: string, message: string) => void
}> = ({ taxCalculations, positions, brokerConfigs, loading, onNotification }) => {
  const [selectedCalculation, setSelectedCalculation] = useState<TaxCalculation | null>(null)
  
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Calculator className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  const totalTaxLiability = taxCalculations.reduce((sum, calc) => sum + calc.totalTax, 0)
  const totalBrokerage = taxCalculations.reduce((sum, calc) => sum + calc.brokerage, 0)
  const totalSTT = taxCalculations.reduce((sum, calc) => sum + calc.stt, 0)
  const totalGST = taxCalculations.reduce((sum, calc) => sum + calc.gst, 0)

  return (
    <div className="space-y-6">
      {/* Tax Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="glass-card p-4 rounded-xl">
          <div className="text-center">
            <div className="text-2xl font-bold text-red-400 mb-1">₹{(totalTaxLiability / 1000).toFixed(1)}K</div>
            <p className="text-slate-400 text-sm">Total Tax Liability</p>
          </div>
        </div>
        
        <div className="glass-card p-4 rounded-xl">
          <div className="text-center">
            <div className="text-2xl font-bold text-orange-400 mb-1">₹{(totalSTT / 1000).toFixed(1)}K</div>
            <p className="text-slate-400 text-sm">STT (0.1%)</p>
          </div>
        </div>
        
        <div className="glass-card p-4 rounded-xl">
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-400 mb-1">₹{(totalBrokerage / 1000).toFixed(1)}K</div>
            <p className="text-slate-400 text-sm">Brokerage</p>
          </div>
        </div>
        
        <div className="glass-card p-4 rounded-xl">
          <div className="text-center">
            <div className="text-2xl font-bold text-purple-400 mb-1">₹{(totalGST / 1000).toFixed(1)}K</div>
            <p className="text-slate-400 text-sm">GST (18%)</p>
          </div>
        </div>
      </div>

      {/* Detailed Tax Breakdown */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700/50">
          <h3 className="text-xl font-bold text-white flex items-center">
            <Calculator className="w-5 h-5 mr-2 text-purple-400" />
            Tax Calculation Breakdown ({taxCalculations.length} positions)
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4 text-slate-400 font-medium">Position</th>
                <th className="text-left p-4 text-slate-400 font-medium">Broker</th>
                <th className="text-left p-4 text-slate-400 font-medium">Value</th>
                <th className="text-left p-4 text-slate-400 font-medium">STT</th>
                <th className="text-left p-4 text-slate-400 font-medium">Brokerage</th>
                <th className="text-left p-4 text-slate-400 font-medium">GST</th>
                <th className="text-left p-4 text-slate-400 font-medium">Total Tax</th>
                <th className="text-left p-4 text-slate-400 font-medium">Net Amount</th>
              </tr>
            </thead>
            <tbody>
              {taxCalculations.map((calc, index) => (
                <motion.tr
                  key={`${calc.brokerId}-${calc.symbol}`}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="border-b border-slate-800/50 hover:bg-slate-800/30 cursor-pointer"
                  onClick={() => setSelectedCalculation(calc)}
                >
                  <td className="p-4">
                    <div>
                      <p className="text-white font-medium">{calc.symbol}</p>
                      <p className="text-slate-400 text-sm">{calc.quantity} shares</p>
                    </div>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center gap-2">
                      <span>{brokerConfigs[calc.brokerId as keyof typeof brokerConfigs]?.icon}</span>
                      <span className="text-white text-sm">{calc.brokerName}</span>
                    </div>
                  </td>
                  <td className="p-4">
                    <p className="text-white font-mono">₹{(calc.value / 1000).toFixed(1)}K</p>
                  </td>
                  <td className="p-4">
                    <p className="text-orange-400 font-mono">₹{calc.stt.toFixed(0)}</p>
                    <p className="text-slate-500 text-xs">{(calc.sttRate * 100).toFixed(2)}%</p>
                  </td>
                  <td className="p-4">
                    <p className="text-blue-400 font-mono">₹{calc.brokerage.toFixed(0)}</p>
                    <p className="text-slate-500 text-xs">{(calc.brokerageRate * 100).toFixed(2)}%</p>
                  </td>
                  <td className="p-4">
                    <p className="text-purple-400 font-mono">₹{calc.gst.toFixed(0)}</p>
                    <p className="text-slate-500 text-xs">{(calc.gstRate * 100).toFixed(0)}%</p>
                  </td>
                  <td className="p-4">
                    <p className="text-red-400 font-mono font-bold">₹{calc.totalTax.toFixed(0)}</p>
                  </td>
                  <td className="p-4">
                    <p className="text-green-400 font-mono font-bold">₹{(calc.netAmount / 1000).toFixed(1)}K</p>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

// FRONT-018: Margin Monitor Tab Component  
const MarginMonitorTab: React.FC<{
  marginUtilizations: MarginUtilization[]
  brokers: BrokerConnection[]
  brokerConfigs: any
  loading: boolean
  onNotification: (type: string, message: string) => void
}> = ({ marginUtilizations, brokers, brokerConfigs, loading, onNotification }) => {
  if (loading) {
    return <div className="flex items-center justify-center h-64">
      <Shield className="w-8 h-8 text-purple-400 animate-spin" />
    </div>
  }

  // Check for margin alerts
  const highRiskBrokers = marginUtilizations.filter(m => m.riskStatus === 'danger' || m.riskStatus === 'margin_call')
  
  useEffect(() => {
    if (highRiskBrokers.length > 0) {
      onNotification('warning', `High margin utilization detected in ${highRiskBrokers.length} broker(s)`)
    }
  }, [highRiskBrokers.length, onNotification])

  return (
    <div className="space-y-6">
      {/* Margin Alert Summary */}
      {highRiskBrokers.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 bg-red-500/20 border border-red-500/30 rounded-xl"
        >
          <div className="flex items-center gap-2 text-red-400">
            <AlertTriangle className="w-5 h-5" />
            <span className="font-semibold">Margin Risk Alert</span>
          </div>
          <p className="text-red-300 text-sm mt-1">
            {highRiskBrokers.length} broker(s) have high margin utilization. Monitor positions closely.
          </p>
        </motion.div>
      )}

      {/* Broker Margin Details */}
      <div className="grid gap-6">
        {marginUtilizations.map(margin => {
          const broker = brokers.find(b => b.id === margin.brokerId)
          if (!broker) return null
          
          return (
            <div key={margin.brokerId} className="glass-card p-6 rounded-2xl">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <span className="text-2xl">{brokerConfigs[broker.brokerType]?.icon || '🏢'}</span>
                  <div>
                    <h3 className="text-white font-bold text-lg">{margin.brokerName}</h3>
                    <div className={`px-2 py-1 rounded text-xs font-medium ${
                      margin.riskStatus === 'safe' ? 'bg-green-500/20 text-green-400' :
                      margin.riskStatus === 'warning' ? 'bg-yellow-500/20 text-yellow-400' :
                      margin.riskStatus === 'danger' ? 'bg-orange-500/20 text-orange-400' :
                      'bg-red-500/20 text-red-400'
                    }`}>
                      {margin.riskStatus.replace('_', ' ').toUpperCase()}
                    </div>
                  </div>
                </div>
                
                <div className="text-right">
                  <div className="text-white font-bold text-xl">{margin.utilizationPercent.toFixed(1)}%</div>
                  <div className="text-slate-400 text-sm">Utilization</div>
                </div>
              </div>
              
              {/* Margin Breakdown */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                <div className="text-center">
                  <div className="text-white font-bold">₹{(margin.totalMargin / 1000).toFixed(0)}K</div>
                  <div className="text-slate-400 text-sm">Total Margin</div>
                </div>
                <div className="text-center">
                  <div className="text-red-400 font-bold">₹{(margin.usedMargin / 1000).toFixed(0)}K</div>
                  <div className="text-slate-400 text-sm">Used</div>
                </div>
                <div className="text-center">
                  <div className="text-green-400 font-bold">₹{(margin.availableMargin / 1000).toFixed(0)}K</div>
                  <div className="text-slate-400 text-sm">Available</div>
                </div>
                <div className="text-center">
                  <div className="text-blue-400 font-bold">{margin.exposureMultiple}x</div>
                  <div className="text-slate-400 text-sm">Exposure</div>
                </div>
              </div>
              
              {/* Margin Utilization Bar */}
              <div className="mb-4">
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-slate-400">Margin Utilization</span>
                  <span className={`${
                    margin.utilizationPercent > 80 ? 'text-red-400' :
                    margin.utilizationPercent > 60 ? 'text-yellow-400' : 'text-green-400'
                  }`}>
                    {margin.utilizationPercent.toFixed(1)}%
                  </span>
                </div>
                
                <div className="h-3 bg-slate-700/50 rounded-full overflow-hidden">
                  <motion.div
                    className={`h-full transition-all duration-1000 ${
                      margin.utilizationPercent > 80 ? 'bg-gradient-to-r from-red-500 to-red-600' :
                      margin.utilizationPercent > 60 ? 'bg-gradient-to-r from-yellow-500 to-yellow-600' :
                      'bg-gradient-to-r from-green-500 to-green-600'
                    }`}
                    style={{ width: `${Math.min(margin.utilizationPercent, 100)}%` }}
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.min(margin.utilizationPercent, 100)}%` }}
                    transition={{ duration: 1, delay: 0.5 }}
                  />
                  
                  {/* Risk Level Markers */}
                  <div className="relative -mt-3 h-3">
                    <div className="absolute" style={{ left: '60%' }}>
                      <div className="w-px h-3 bg-yellow-400/50" />
                    </div>
                    <div className="absolute" style={{ left: '80%' }}>
                      <div className="w-px h-3 bg-red-400/50" />
                    </div>
                  </div>
                </div>
                
                <div className="flex justify-between text-xs text-slate-500 mt-1">
                  <span>Safe</span>
                  <span>Warning (60%)</span>
                  <span>Danger (80%)</span>
                  <span>Margin Call</span>
                </div>
              </div>
              
              {/* Additional Details */}
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Day Trading Power:</span>
                    <span className="text-white">₹{(margin.dayTradingPower / 1000).toFixed(0)}K</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Pledged Securities:</span>
                    <span className="text-white">₹{(margin.pledgedSecurities / 1000).toFixed(0)}K</span>
                  </div>
                </div>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Margin Call Level:</span>
                    <span className="text-yellow-400">₹{(margin.marginCallLevel / 1000).toFixed(0)}K</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Liquidation Level:</span>
                    <span className="text-red-400">₹{(margin.liquidationLevel / 1000).toFixed(0)}K</span>
                  </div>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// Placeholder components for remaining tabs
const PerformanceAttributionTab: React.FC<any> = ({ performanceAttributions, loading }) => {
  if (loading) return <div className="flex items-center justify-center h-64"><Target className="w-8 h-8 text-purple-400 animate-spin" /></div>
  return <div className="glass-card p-6 rounded-2xl"><h3 className="text-white text-xl mb-4">Performance Attribution Analysis</h3><p className="text-slate-400">Detailed performance attribution with security selection and allocation effects coming soon...</p></div>
}

const RiskMetricsTab: React.FC<any> = ({ riskMetrics, loading }) => {
  if (loading) return <div className="flex items-center justify-center h-64"><AlertTriangle className="w-8 h-8 text-purple-400 animate-spin" /></div>
  return <div className="glass-card p-6 rounded-2xl"><h3 className="text-white text-xl mb-4">Risk Metrics Dashboard</h3><p className="text-slate-400">Comprehensive risk analysis including VaR, concentration risk, and portfolio volatility coming soon...</p></div>
}

const PortfolioRebalancingTab: React.FC<any> = ({ loading }) => {
  if (loading) return <div className="flex items-center justify-center h-64"><Scale className="w-8 h-8 text-purple-400 animate-spin" /></div>
  return <div className="glass-card p-6 rounded-2xl"><h3 className="text-white text-xl mb-4">Portfolio Rebalancing Tools</h3><p className="text-slate-400">AI-powered rebalancing suggestions with cost optimization coming soon...</p></div>
}

const TaxOptimizationTab: React.FC<any> = ({ loading }) => {
  if (loading) return <div className="flex items-center justify-center h-64"><Zap className="w-8 h-8 text-purple-400 animate-spin" /></div>
  return <div className="glass-card p-6 rounded-2xl"><h3 className="text-white text-xl mb-4">Tax Optimization Assistant</h3><p className="text-slate-400">AI-powered tax optimization with loss harvesting and gain deferral strategies coming soon...</p></div>
}

export default RealTimePnLPortfolioIntegration