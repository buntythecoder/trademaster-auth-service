import React, { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, TrendingDown, DollarSign, Calendar, 
  BarChart3, PieChart, Target, Clock, Activity,
  Building2, ArrowUpRight, ArrowDownRight, Percent,
  Filter, RefreshCw, Eye, EyeOff, ChevronRight
} from 'lucide-react'
import { BrokerConnection, BrokerPosition } from '../../services/brokerService'

interface MultiBrokerPnLDashboardProps {
  positions: BrokerPosition[]
  brokers: BrokerConnection[]
  onRefresh?: () => void
  isLoading?: boolean
  className?: string
}

interface BrokerPnLSummary {
  brokerId: string
  brokerName: string
  brokerType: string
  totalValue: number
  totalPnl: number
  totalPnlPercent: number
  dayPnl: number
  dayPnlPercent: number
  positionCount: number
  profitablePositions: number
  losingPositions: number
  avgPnlPerPosition: number
  topPerformer: string | null
  worstPerformer: string | null
}

interface PnLTimeframe {
  label: string
  value: 'day' | 'week' | 'month' | 'quarter' | 'year' | 'all'
}

const timeframes: PnLTimeframe[] = [
  { label: 'Day', value: 'day' },
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
  { label: 'Quarter', value: 'quarter' },
  { label: 'Year', value: 'year' },
  { label: 'All Time', value: 'all' }
]

export const MultiBrokerPnLDashboard: React.FC<MultiBrokerPnLDashboardProps> = ({
  positions,
  brokers,
  onRefresh,
  isLoading = false,
  className = ''
}) => {
  const [selectedTimeframe, setSelectedTimeframe] = useState<string>('day')
  const [showDetailedBreakdown, setShowDetailedBreakdown] = useState(false)
  const [selectedBroker, setSelectedBroker] = useState<string | null>(null)

  // Calculate broker-wise P&L summaries
  const brokerPnLSummaries = useMemo(() => {
    const brokerMap = new Map<string, BrokerPnLSummary>()
    
    // Initialize all connected brokers
    brokers.forEach(broker => {
      brokerMap.set(broker.id, {
        brokerId: broker.id,
        brokerName: broker.displayName,
        brokerType: broker.brokerType,
        totalValue: 0,
        totalPnl: 0,
        totalPnlPercent: 0,
        dayPnl: 0,
        dayPnlPercent: 0,
        positionCount: 0,
        profitablePositions: 0,
        losingPositions: 0,
        avgPnlPerPosition: 0,
        topPerformer: null,
        worstPerformer: null
      })
    })

    // Aggregate position data
    positions.forEach(position => {
      const summary = brokerMap.get(position.brokerId)
      if (summary) {
        summary.totalValue += position.quantity * position.currentPrice
        summary.totalPnl += position.pnl
        summary.dayPnl += position.dayPnl
        summary.positionCount += 1
        
        if (position.pnl > 0) {
          summary.profitablePositions += 1
        } else if (position.pnl < 0) {
          summary.losingPositions += 1
        }

        // Track best/worst performers
        if (!summary.topPerformer || position.pnlPercent > 
            (positions.find(p => p.symbol === summary.topPerformer && p.brokerId === position.brokerId)?.pnlPercent || -Infinity)) {
          summary.topPerformer = position.symbol
        }
        if (!summary.worstPerformer || position.pnlPercent < 
            (positions.find(p => p.symbol === summary.worstPerformer && p.brokerId === position.brokerId)?.pnlPercent || Infinity)) {
          summary.worstPerformer = position.symbol
        }
      }
    })

    // Calculate percentages and averages
    Array.from(brokerMap.values()).forEach(summary => {
      if (summary.totalValue > 0) {
        summary.totalPnlPercent = (summary.totalPnl / (summary.totalValue - summary.totalPnl)) * 100
        summary.dayPnlPercent = (summary.dayPnl / (summary.totalValue - summary.dayPnl)) * 100
      }
      if (summary.positionCount > 0) {
        summary.avgPnlPerPosition = summary.totalPnl / summary.positionCount
      }
    })

    return Array.from(brokerMap.values()).filter(summary => summary.positionCount > 0)
  }, [positions, brokers])

  // Calculate overall portfolio metrics
  const portfolioMetrics = useMemo(() => {
    const totalValue = brokerPnLSummaries.reduce((sum, broker) => sum + broker.totalValue, 0)
    const totalPnl = brokerPnLSummaries.reduce((sum, broker) => sum + broker.totalPnl, 0)
    const totalDayPnl = brokerPnLSummaries.reduce((sum, broker) => sum + broker.dayPnl, 0)
    const totalPositions = brokerPnLSummaries.reduce((sum, broker) => sum + broker.positionCount, 0)
    
    return {
      totalValue,
      totalPnl,
      totalPnlPercent: totalValue > 0 ? (totalPnl / (totalValue - totalPnl)) * 100 : 0,
      totalDayPnl,
      totalDayPnlPercent: totalValue > 0 ? (totalDayPnl / (totalValue - totalDayPnl)) * 100 : 0,
      totalPositions,
      bestPerformingBroker: brokerPnLSummaries.reduce((best, current) => 
        current.totalPnlPercent > (best?.totalPnlPercent || -Infinity) ? current : best, null as BrokerPnLSummary | null),
      worstPerformingBroker: brokerPnLSummaries.reduce((worst, current) => 
        current.totalPnlPercent < (worst?.totalPnlPercent || Infinity) ? current : worst, null as BrokerPnLSummary | null)
    }
  }, [brokerPnLSummaries])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount)
  }

  const formatPercentage = (percent: number) => {
    return `${percent >= 0 ? '+' : ''}${percent.toFixed(2)}%`
  }

  const getBrokerColor = (brokerType: string) => {
    const colors: Record<string, string> = {
      'zerodha': 'bg-orange-500',
      'upstox': 'bg-blue-500',
      'angel_one': 'bg-red-500',
      'icici_direct': 'bg-purple-500',
      'groww': 'bg-green-500',
      'iifl': 'bg-pink-500'
    }
    return colors[brokerType] || 'bg-slate-500'
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="glass-card rounded-xl p-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between space-y-4 sm:space-y-0">
          <div className="flex items-center space-x-4">
            <h2 className="text-2xl font-bold text-white">Multi-Broker P&L Dashboard</h2>
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors disabled:opacity-50"
              title="Refresh P&L data"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="flex items-center space-x-3">
            <select
              value={selectedTimeframe}
              onChange={(e) => setSelectedTimeframe(e.target.value)}
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm"
            >
              {timeframes.map(tf => (
                <option key={tf.value} value={tf.value}>{tf.label}</option>
              ))}
            </select>
            
            <button
              onClick={() => setShowDetailedBreakdown(!showDetailedBreakdown)}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                showDetailedBreakdown 
                  ? 'bg-blue-500 text-white' 
                  : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
              }`}
            >
              {showDetailedBreakdown ? <EyeOff className="w-4 h-4 inline mr-1" /> : <Eye className="w-4 h-4 inline mr-1" />}
              Details
            </button>
          </div>
        </div>
      </div>

      {/* Portfolio Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Total Portfolio</h3>
            <DollarSign className="w-4 h-4 text-green-400" />
          </div>
          <p className="text-2xl font-bold text-white">{formatCurrency(portfolioMetrics.totalValue)}</p>
          <p className="text-xs text-slate-400">{portfolioMetrics.totalPositions} positions across {brokerPnLSummaries.length} brokers</p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Total P&L</h3>
            {portfolioMetrics.totalPnl >= 0 ? 
              <TrendingUp className="w-4 h-4 text-green-400" /> :
              <TrendingDown className="w-4 h-4 text-red-400" />
            }
          </div>
          <p className={`text-2xl font-bold ${portfolioMetrics.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatCurrency(portfolioMetrics.totalPnl)}
          </p>
          <p className={`text-xs ${portfolioMetrics.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatPercentage(portfolioMetrics.totalPnlPercent)}
          </p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Day P&L</h3>
            {portfolioMetrics.totalDayPnl >= 0 ? 
              <ArrowUpRight className="w-4 h-4 text-green-400" /> :
              <ArrowDownRight className="w-4 h-4 text-red-400" />
            }
          </div>
          <p className={`text-2xl font-bold ${portfolioMetrics.totalDayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatCurrency(portfolioMetrics.totalDayPnl)}
          </p>
          <p className={`text-xs ${portfolioMetrics.totalDayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatPercentage(portfolioMetrics.totalDayPnlPercent)}
          </p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Best Performer</h3>
            <Target className="w-4 h-4 text-blue-400" />
          </div>
          {portfolioMetrics.bestPerformingBroker ? (
            <>
              <p className="text-lg font-bold text-white">{portfolioMetrics.bestPerformingBroker.brokerName}</p>
              <p className="text-xs text-green-400">
                {formatPercentage(portfolioMetrics.bestPerformingBroker.totalPnlPercent)}
              </p>
            </>
          ) : (
            <p className="text-lg text-slate-400">No data</p>
          )}
        </div>
      </div>

      {/* Broker Performance Cards */}
      <div className="glass-card rounded-xl p-6">
        <h3 className="text-xl font-semibold text-white mb-6">Broker Performance</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {brokerPnLSummaries.map((broker) => (
            <motion.div
              key={broker.brokerId}
              whileHover={{ scale: 1.02 }}
              className={`glass-card rounded-xl p-4 cursor-pointer transition-all ${
                selectedBroker === broker.brokerId ? 'ring-2 ring-blue-500' : ''
              }`}
              onClick={() => setSelectedBroker(
                selectedBroker === broker.brokerId ? null : broker.brokerId
              )}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                  <div className={`w-3 h-3 rounded-full ${getBrokerColor(broker.brokerType)}`} />
                  <h4 className="font-semibold text-white">{broker.brokerName}</h4>
                </div>
                <ChevronRight className={`w-4 h-4 text-slate-400 transition-transform ${
                  selectedBroker === broker.brokerId ? 'rotate-90' : ''
                }`} />
              </div>

              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-slate-400">Total Value</span>
                  <span className="font-medium text-white">{formatCurrency(broker.totalValue)}</span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-sm text-slate-400">Total P&L</span>
                  <div className="text-right">
                    <p className={`font-medium ${broker.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {formatCurrency(broker.totalPnl)}
                    </p>
                    <p className={`text-xs ${broker.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {formatPercentage(broker.totalPnlPercent)}
                    </p>
                  </div>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-sm text-slate-400">Day P&L</span>
                  <div className="text-right">
                    <p className={`font-medium ${broker.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {formatCurrency(broker.dayPnl)}
                    </p>
                    <p className={`text-xs ${broker.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                      {formatPercentage(broker.dayPnlPercent)}
                    </p>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-700/50">
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Positions</span>
                    <span className="text-white">{broker.positionCount}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Win Rate</span>
                    <span className="text-white">
                      {broker.positionCount > 0 
                        ? ((broker.profitablePositions / broker.positionCount) * 100).toFixed(0)
                        : 0}%
                    </span>
                  </div>
                </div>
              </div>

              {/* Expanded Details */}
              <AnimatePresence>
                {selectedBroker === broker.brokerId && showDetailedBreakdown && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="mt-4 pt-4 border-t border-slate-700/50"
                  >
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span className="text-slate-400">Avg P&L/Position</span>
                        <span className={broker.avgPnlPerPosition >= 0 ? 'text-green-400' : 'text-red-400'}>
                          {formatCurrency(broker.avgPnlPerPosition)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Profitable</span>
                        <span className="text-green-400">{broker.profitablePositions}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-slate-400">Losing</span>
                        <span className="text-red-400">{broker.losingPositions}</span>
                      </div>
                      {broker.topPerformer && (
                        <div className="flex justify-between">
                          <span className="text-slate-400">Top Stock</span>
                          <span className="text-green-400">{broker.topPerformer}</span>
                        </div>
                      )}
                      {broker.worstPerformer && (
                        <div className="flex justify-between">
                          <span className="text-slate-400">Worst Stock</span>
                          <span className="text-red-400">{broker.worstPerformer}</span>
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>

        {brokerPnLSummaries.length === 0 && (
          <div className="text-center py-8">
            <Building2 className="w-12 h-12 mx-auto mb-3 text-slate-500" />
            <h3 className="font-medium text-white mb-1">No broker data available</h3>
            <p className="text-sm text-slate-400">Connect brokers and start trading to see P&L data</p>
          </div>
        )}
      </div>

      {/* P&L Chart Placeholder */}
      <div className="glass-card rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-semibold text-white">P&L Trend</h3>
          <div className="flex items-center space-x-2">
            <BarChart3 className="w-4 h-4 text-blue-400" />
            <PieChart className="w-4 h-4 text-purple-400" />
          </div>
        </div>
        
        <div className="h-64 flex items-center justify-center border-2 border-dashed border-slate-700 rounded-lg">
          <div className="text-center">
            <Activity className="w-12 h-12 mx-auto mb-3 text-slate-500" />
            <h4 className="font-medium text-white mb-1">P&L Chart Coming Soon</h4>
            <p className="text-sm text-slate-400">Historical P&L trends and broker comparison charts</p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default MultiBrokerPnLDashboard