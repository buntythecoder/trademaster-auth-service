import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown,
  AlertTriangle,
  DollarSign,
  BarChart3,
  Activity,
  Target,
  Shield,
  RefreshCw,
  MoreVertical,
  Eye,
  EyeOff,
  Zap,
  Clock,
  Percent
} from 'lucide-react'

export interface LivePosition {
  id: string
  symbol: string
  quantity: number
  averagePrice: number
  currentPrice: number
  lastUpdateTime: Date
  pnl: number
  pnlPercent: number
  dayPnl: number
  dayPnlPercent: number
  marketValue: number
  investedValue: number
  broker: string
  exchange: 'NSE' | 'BSE' | 'NYSE' | 'NASDAQ'
  instrumentType: 'equity' | 'futures' | 'options' | 'commodity'
  sector: string
  stopLoss?: number
  target?: number
  riskLevel: 'low' | 'medium' | 'high'
  isWatched: boolean
}

export interface PositionSummary {
  totalInvestment: number
  currentValue: number
  totalPnl: number
  totalPnlPercent: number
  dayPnl: number
  dayPnlPercent: number
  positions: number
  gainers: number
  losers: number
}

export interface LivePositionTrackerProps {
  positions?: LivePosition[]
  onRefresh?: () => void
  onClosePosition?: (positionId: string) => void
  onSetStopLoss?: (positionId: string, price: number) => void
  onSetTarget?: (positionId: string, price: number) => void
  showRiskAnalysis?: boolean
  compactMode?: boolean
  className?: string
}

const LivePositionTracker: React.FC<LivePositionTrackerProps> = ({
  positions = [],
  onRefresh,
  onClosePosition,
  onSetStopLoss,
  onSetTarget,
  showRiskAnalysis = true,
  compactMode = false,
  className = ''
}) => {
  const [sortBy, setSortBy] = useState<'pnl' | 'pnlPercent' | 'value' | 'symbol'>('pnl')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')
  const [filterRisk, setFilterRisk] = useState<'all' | 'high' | 'medium' | 'low'>('all')
  const [showOnlyWatched, setShowOnlyWatched] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [hidePnL, setHidePnL] = useState(false)

  // Mock data if no positions provided
  const mockPositions: LivePosition[] = useMemo(() => [
    {
      id: 'POS-001',
      symbol: 'RELIANCE',
      quantity: 100,
      averagePrice: 2450.25,
      currentPrice: 2456.75,
      lastUpdateTime: new Date(),
      pnl: 650.00,
      pnlPercent: 0.27,
      dayPnl: 425.50,
      dayPnlPercent: 0.17,
      marketValue: 245675.00,
      investedValue: 245025.00,
      broker: 'Zerodha',
      exchange: 'NSE',
      instrumentType: 'equity',
      sector: 'Oil & Gas',
      stopLoss: 2400.00,
      target: 2500.00,
      riskLevel: 'low',
      isWatched: true
    },
    {
      id: 'POS-002',
      symbol: 'TCS',
      quantity: 50,
      averagePrice: 3800.50,
      currentPrice: 3789.40,
      lastUpdateTime: new Date(),
      pnl: -555.00,
      pnlPercent: -0.29,
      dayPnl: -315.75,
      dayPnlPercent: -0.17,
      marketValue: 189470.00,
      investedValue: 190025.00,
      broker: 'Upstox',
      exchange: 'NSE',
      instrumentType: 'equity',
      sector: 'Information Technology',
      stopLoss: 3750.00,
      riskLevel: 'medium',
      isWatched: false
    },
    {
      id: 'POS-003',
      symbol: 'HDFCBANK',
      quantity: 200,
      averagePrice: 1680.75,
      currentPrice: 1687.25,
      lastUpdateTime: new Date(),
      pnl: 1300.00,
      pnlPercent: 0.39,
      dayPnl: 890.50,
      dayPnlPercent: 0.26,
      marketValue: 337450.00,
      investedValue: 336150.00,
      broker: 'Angel One',
      exchange: 'NSE',
      instrumentType: 'equity',
      sector: 'Banking',
      target: 1750.00,
      riskLevel: 'low',
      isWatched: true
    },
    {
      id: 'POS-004',
      symbol: 'ICICIBANK',
      quantity: 300,
      averagePrice: 995.80,
      currentPrice: 987.35,
      lastUpdateTime: new Date(),
      pnl: -2535.00,
      pnlPercent: -0.85,
      dayPnl: -1247.50,
      dayPnlPercent: -0.42,
      marketValue: 296205.00,
      investedValue: 298740.00,
      broker: 'ICICI Direct',
      exchange: 'NSE',
      instrumentType: 'equity',
      sector: 'Banking',
      stopLoss: 980.00,
      riskLevel: 'high',
      isWatched: true
    },
    {
      id: 'POS-005',
      symbol: 'AAPL',
      quantity: 25,
      averagePrice: 193.50,
      currentPrice: 195.89,
      lastUpdateTime: new Date(),
      pnl: 59.75,
      pnlPercent: 1.23,
      dayPnl: 34.25,
      dayPnlPercent: 0.70,
      marketValue: 4897.25,
      investedValue: 4837.50,
      broker: 'Zerodha',
      exchange: 'NASDAQ',
      instrumentType: 'equity',
      sector: 'Technology',
      target: 210.00,
      riskLevel: 'medium',
      isWatched: false
    }
  ], [])

  const displayPositions = positions.length > 0 ? positions : mockPositions

  // Filter and sort positions
  const filteredPositions = useMemo(() => {
    let filtered = displayPositions

    // Risk filter
    if (filterRisk !== 'all') {
      filtered = filtered.filter(pos => pos.riskLevel === filterRisk)
    }

    // Watched filter
    if (showOnlyWatched) {
      filtered = filtered.filter(pos => pos.isWatched)
    }

    // Sort
    filtered.sort((a, b) => {
      let aVal: number, bVal: number
      
      switch (sortBy) {
        case 'pnl':
          aVal = a.pnl
          bVal = b.pnl
          break
        case 'pnlPercent':
          aVal = a.pnlPercent
          bVal = b.pnlPercent
          break
        case 'value':
          aVal = a.marketValue
          bVal = b.marketValue
          break
        case 'symbol':
          return sortOrder === 'asc' 
            ? a.symbol.localeCompare(b.symbol)
            : b.symbol.localeCompare(a.symbol)
        default:
          aVal = a.pnl
          bVal = b.pnl
      }
      
      return sortOrder === 'asc' ? aVal - bVal : bVal - aVal
    })

    return filtered
  }, [displayPositions, sortBy, sortOrder, filterRisk, showOnlyWatched])

  // Summary calculations
  const summary: PositionSummary = useMemo(() => {
    const totalInvestment = displayPositions.reduce((sum, pos) => sum + pos.investedValue, 0)
    const currentValue = displayPositions.reduce((sum, pos) => sum + pos.marketValue, 0)
    const totalPnl = currentValue - totalInvestment
    const dayPnl = displayPositions.reduce((sum, pos) => sum + pos.dayPnl, 0)
    const gainers = displayPositions.filter(pos => pos.pnl > 0).length
    const losers = displayPositions.filter(pos => pos.pnl < 0).length

    return {
      totalInvestment,
      currentValue,
      totalPnl,
      totalPnlPercent: totalInvestment > 0 ? (totalPnl / totalInvestment) * 100 : 0,
      dayPnl,
      dayPnlPercent: currentValue > 0 ? (dayPnl / currentValue) * 100 : 0,
      positions: displayPositions.length,
      gainers,
      losers
    }
  }, [displayPositions])

  const handleRefresh = async () => {
    setIsRefreshing(true)
    await new Promise(resolve => setTimeout(resolve, 1500))
    onRefresh?.()
    setIsRefreshing(false)
  }

  const formatCurrency = (value: number) => {
    return `₹${value.toLocaleString('en-IN', { 
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`
  }

  const formatPercent = (value: number) => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
  }

  const getRiskColor = (riskLevel: string) => {
    switch (riskLevel) {
      case 'high':
        return 'text-red-400 bg-red-400/10'
      case 'medium':
        return 'text-yellow-400 bg-yellow-400/10'
      case 'low':
        return 'text-green-400 bg-green-400/10'
      default:
        return 'text-slate-400 bg-slate-400/10'
    }
  }

  const PositionCard: React.FC<{ position: LivePosition }> = ({ position }) => {
    const isProfitable = position.pnl >= 0
    const isDayProfitable = position.dayPnl >= 0

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
              <span className="font-semibold text-white">{position.symbol}</span>
              <span className="text-xs px-2 py-0.5 rounded bg-slate-600/50 text-slate-300">
                {position.exchange}
              </span>
              <span className={`px-2 py-0.5 rounded text-xs font-medium ${getRiskColor(position.riskLevel)}`}>
                {position.riskLevel.toUpperCase()}
              </span>
            </div>
            {position.isWatched && (
              <Eye className="w-4 h-4 text-purple-400" />
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-400">{position.broker}</span>
            <button className="p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors">
              <MoreVertical className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Position Size & Current Price */}
        <div className="grid grid-cols-2 gap-4 mb-3">
          <div>
            <div className="text-xs text-slate-400 mb-1">Position</div>
            <div className="text-sm">
              <span className="text-white font-medium">{position.quantity}</span>
              <span className="text-slate-400 ml-1">@ ₹{position.averagePrice.toFixed(2)}</span>
            </div>
            <div className="text-xs text-slate-400 mt-1">
              Invested: {formatCurrency(position.investedValue)}
            </div>
          </div>

          <div>
            <div className="text-xs text-slate-400 mb-1">Current Price</div>
            <div className="text-sm">
              <span className="text-white font-medium">₹{position.currentPrice.toFixed(2)}</span>
              <div className={`text-xs flex items-center mt-1 ${isDayProfitable ? 'text-green-400' : 'text-red-400'}`}>
                {isDayProfitable ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
                {formatPercent(position.dayPnlPercent)}
              </div>
            </div>
          </div>
        </div>

        {/* P&L Display */}
        {!hidePnL && (
          <div className="grid grid-cols-2 gap-4 mb-3">
            <div>
              <div className="text-xs text-slate-400 mb-1">Overall P&L</div>
              <div className={`text-sm font-medium ${isProfitable ? 'text-green-400' : 'text-red-400'}`}>
                <div className="flex items-center">
                  {isProfitable ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
                  {formatCurrency(Math.abs(position.pnl))}
                </div>
                <div className="text-xs mt-1">
                  ({formatPercent(position.pnlPercent)})
                </div>
              </div>
            </div>

            <div>
              <div className="text-xs text-slate-400 mb-1">Day P&L</div>
              <div className={`text-sm font-medium ${isDayProfitable ? 'text-green-400' : 'text-red-400'}`}>
                <div className="flex items-center">
                  {isDayProfitable ? <TrendingUp className="w-3 h-3 mr-1" /> : <TrendingDown className="w-3 h-3 mr-1" />}
                  {formatCurrency(Math.abs(position.dayPnl))}
                </div>
                <div className="text-xs mt-1">
                  ({formatPercent(position.dayPnlPercent)})
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Stop Loss & Target */}
        {(position.stopLoss || position.target) && (
          <div className="flex items-center justify-between text-xs mb-3">
            {position.stopLoss && (
              <div className="flex items-center text-red-400">
                <Shield className="w-3 h-3 mr-1" />
                SL: ₹{position.stopLoss.toFixed(2)}
              </div>
            )}
            {position.target && (
              <div className="flex items-center text-green-400">
                <Target className="w-3 h-3 mr-1" />
                TGT: ₹{position.target.toFixed(2)}
              </div>
            )}
          </div>
        )}

        {/* Market Value & Last Update */}
        <div className="flex items-center justify-between text-xs text-slate-400">
          <div>
            Market Value: <span className="text-white">{formatCurrency(position.marketValue)}</span>
          </div>
          <div className="flex items-center">
            <Clock className="w-3 h-3 mr-1" />
            {position.lastUpdateTime.toLocaleTimeString('en-IN', {
              hour: '2-digit',
              minute: '2-digit'
            })}
          </div>
        </div>
      </motion.div>
    )
  }

  return (
    <div className={`glass-widget-card rounded-2xl ${className}`}>
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-white">Live Positions</h2>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setHidePnL(!hidePnL)}
              className={`cyber-button-sm p-2 rounded-xl transition-all duration-300 ${hidePnL ? 'text-slate-400' : 'text-purple-400'}`}
              title={hidePnL ? 'Show P&L' : 'Hide P&L'}
            >
              {hidePnL ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
            
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="cyber-button-sm p-2 rounded-xl hover:scale-110 transition-all duration-300 disabled:opacity-50"
              title="Refresh Positions"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Summary Stats */}
        {!compactMode && !hidePnL && (
          <div className="grid grid-cols-4 gap-4 mb-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-white">{summary.positions}</div>
              <div className="text-xs text-slate-400">Positions</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">{formatCurrency(summary.currentValue)}</div>
              <div className="text-xs text-slate-400">Market Value</div>
            </div>
            <div className="text-center">
              <div className={`text-2xl font-bold ${summary.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatCurrency(summary.totalPnl)}
              </div>
              <div className="text-xs text-slate-400">
                Total P&L ({formatPercent(summary.totalPnlPercent)})
              </div>
            </div>
            <div className="text-center">
              <div className={`text-2xl font-bold ${summary.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                {formatCurrency(summary.dayPnl)}
              </div>
              <div className="text-xs text-slate-400">
                Day P&L ({formatPercent(summary.dayPnlPercent)})
              </div>
            </div>
          </div>
        )}

        {/* Filters and Sort */}
        <div className="flex items-center space-x-3 overflow-x-auto">
          {/* Sort */}
          <select
            value={`${sortBy}-${sortOrder}`}
            onChange={(e) => {
              const [sort, order] = e.target.value.split('-')
              setSortBy(sort as any)
              setSortOrder(order as any)
            }}
            className="glass-input rounded-lg px-3 py-2 text-sm text-white"
          >
            <option value="pnl-desc">Highest P&L</option>
            <option value="pnl-asc">Lowest P&L</option>
            <option value="pnlPercent-desc">Highest P&L %</option>
            <option value="pnlPercent-asc">Lowest P&L %</option>
            <option value="value-desc">Highest Value</option>
            <option value="symbol-asc">Symbol A-Z</option>
          </select>

          {/* Risk Filter */}
          {showRiskAnalysis && (
            <select
              value={filterRisk}
              onChange={(e) => setFilterRisk(e.target.value as any)}
              className="glass-input rounded-lg px-3 py-2 text-sm text-white"
            >
              <option value="all">All Risk</option>
              <option value="high">High Risk</option>
              <option value="medium">Medium Risk</option>
              <option value="low">Low Risk</option>
            </select>
          )}

          {/* Watched Only */}
          <button
            onClick={() => setShowOnlyWatched(!showOnlyWatched)}
            className={`flex items-center space-x-1 px-3 py-2 rounded-lg text-sm transition-all ${
              showOnlyWatched 
                ? 'bg-purple-600 text-white' 
                : 'bg-slate-700/50 text-slate-400 hover:text-white'
            }`}
          >
            <Eye className="w-4 h-4" />
            <span>Watched</span>
          </button>
        </div>
      </div>

      {/* Position List */}
      <div className="p-6">
        {filteredPositions.length === 0 ? (
          <div className="text-center py-12">
            <Activity className="w-12 h-12 text-slate-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-white mb-2">No Positions Found</h3>
            <p className="text-slate-400">
              {filterRisk !== 'all' || showOnlyWatched
                ? 'Try adjusting your filters to see more positions.'
                : 'Your open positions will appear here once you start trading.'
              }
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            <AnimatePresence mode="popLayout">
              {filteredPositions.map((position) => (
                <PositionCard key={position.id} position={position} />
              ))}
            </AnimatePresence>
          </div>
        )}
      </div>
    </div>
  )
}

export { LivePositionTracker }
export default LivePositionTracker