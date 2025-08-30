import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, TrendingDown, DollarSign, Activity, Eye,
  Filter, SortAsc, SortDesc, RefreshCw, AlertCircle,
  Building2, BarChart3, PieChart, Target, Clock,
  ArrowUpRight, ArrowDownRight, Minus
} from 'lucide-react'
import { BrokerPosition, BrokerConnection } from '../../services/brokerService'

interface MultiBrokerPositionDashboardProps {
  positions: BrokerPosition[]
  brokers: BrokerConnection[]
  onRefresh?: () => void
  isLoading?: boolean
  className?: string
}

interface AggregatedPosition {
  symbol: string
  totalQuantity: number
  avgPrice: number
  currentPrice: number
  totalValue: number
  totalPnl: number
  totalPnlPercent: number
  dayPnl: number
  dayPnlPercent: number
  brokerPositions: BrokerPosition[]
}

type SortField = 'symbol' | 'totalValue' | 'totalPnl' | 'totalPnlPercent' | 'dayPnl'
type SortDirection = 'asc' | 'desc'
type FilterType = 'all' | 'profitable' | 'losing' | 'break-even'

export const MultiBrokerPositionDashboard: React.FC<MultiBrokerPositionDashboardProps> = ({
  positions,
  brokers,
  onRefresh,
  isLoading = false,
  className = ''
}) => {
  const [sortField, setSortField] = useState<SortField>('totalPnl')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [filterType, setFilterType] = useState<FilterType>('all')
  const [showBrokerBreakdown, setShowBrokerBreakdown] = useState<string | null>(null)

  // Aggregate positions by symbol across all brokers
  const aggregatedPositions = useMemo(() => {
    const positionMap = new Map<string, AggregatedPosition>()
    
    positions.forEach(position => {
      const existing = positionMap.get(position.symbol)
      
      if (existing) {
        // Calculate weighted average price
        const totalQuantity = existing.totalQuantity + position.quantity
        const totalCost = (existing.avgPrice * existing.totalQuantity) + (position.avgPrice * position.quantity)
        const avgPrice = totalCost / totalQuantity
        
        existing.totalQuantity = totalQuantity
        existing.avgPrice = avgPrice
        existing.currentPrice = position.currentPrice // Assume same current price
        existing.totalValue += position.quantity * position.currentPrice
        existing.totalPnl += position.pnl
        existing.dayPnl += position.dayPnl
        existing.brokerPositions.push(position)
        
        // Recalculate percentages
        existing.totalPnlPercent = ((existing.totalValue - (existing.avgPrice * existing.totalQuantity)) / (existing.avgPrice * existing.totalQuantity)) * 100
        existing.dayPnlPercent = (existing.dayPnl / (existing.totalValue - existing.dayPnl)) * 100
      } else {
        positionMap.set(position.symbol, {
          symbol: position.symbol,
          totalQuantity: position.quantity,
          avgPrice: position.avgPrice,
          currentPrice: position.currentPrice,
          totalValue: position.quantity * position.currentPrice,
          totalPnl: position.pnl,
          totalPnlPercent: position.pnlPercent,
          dayPnl: position.dayPnl,
          dayPnlPercent: position.dayPnlPercent,
          brokerPositions: [position]
        })
      }
    })
    
    return Array.from(positionMap.values())
  }, [positions])

  // Apply filters and sorting
  const filteredAndSortedPositions = useMemo(() => {
    let filtered = aggregatedPositions.filter(position => {
      switch (filterType) {
        case 'profitable':
          return position.totalPnl > 0
        case 'losing':
          return position.totalPnl < 0
        case 'break-even':
          return Math.abs(position.totalPnl) < 100 // Within ₹100 of break-even
        default:
          return true
      }
    })

    return filtered.sort((a, b) => {
      const aValue = a[sortField]
      const bValue = b[sortField]
      const multiplier = sortDirection === 'asc' ? 1 : -1
      return (aValue > bValue ? 1 : -1) * multiplier
    })
  }, [aggregatedPositions, filterType, sortField, sortDirection])

  // Calculate summary metrics
  const summaryMetrics = useMemo(() => {
    const totalValue = aggregatedPositions.reduce((sum, pos) => sum + pos.totalValue, 0)
    const totalPnl = aggregatedPositions.reduce((sum, pos) => sum + pos.totalPnl, 0)
    const totalDayPnl = aggregatedPositions.reduce((sum, pos) => sum + pos.dayPnl, 0)
    
    return {
      totalPositions: aggregatedPositions.length,
      totalValue,
      totalPnl,
      totalPnlPercent: totalValue > 0 ? (totalPnl / (totalValue - totalPnl)) * 100 : 0,
      totalDayPnl,
      totalDayPnlPercent: totalValue > 0 ? (totalDayPnl / (totalValue - totalDayPnl)) * 100 : 0,
      profitablePositions: aggregatedPositions.filter(p => p.totalPnl > 0).length,
      losingPositions: aggregatedPositions.filter(p => p.totalPnl < 0).length
    }
  }, [aggregatedPositions])

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortDirection('desc')
    }
  }

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

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Total Positions</h3>
            <Target className="w-4 h-4 text-blue-400" />
          </div>
          <p className="text-2xl font-bold text-white">{summaryMetrics.totalPositions}</p>
          <p className="text-xs text-slate-400">
            {summaryMetrics.profitablePositions} profitable, {summaryMetrics.losingPositions} losing
          </p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Total Value</h3>
            <DollarSign className="w-4 h-4 text-green-400" />
          </div>
          <p className="text-2xl font-bold text-white">{formatCurrency(summaryMetrics.totalValue)}</p>
          <p className="text-xs text-slate-400">Current market value</p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Total P&L</h3>
            {summaryMetrics.totalPnl >= 0 ? 
              <TrendingUp className="w-4 h-4 text-green-400" /> :
              <TrendingDown className="w-4 h-4 text-red-400" />
            }
          </div>
          <p className={`text-2xl font-bold ${summaryMetrics.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatCurrency(summaryMetrics.totalPnl)}
          </p>
          <p className={`text-xs ${summaryMetrics.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatPercentage(summaryMetrics.totalPnlPercent)}
          </p>
        </div>

        <div className="glass-card rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-sm font-medium text-slate-400">Day P&L</h3>
            {summaryMetrics.totalDayPnl >= 0 ? 
              <ArrowUpRight className="w-4 h-4 text-green-400" /> :
              <ArrowDownRight className="w-4 h-4 text-red-400" />
            }
          </div>
          <p className={`text-2xl font-bold ${summaryMetrics.totalDayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatCurrency(summaryMetrics.totalDayPnl)}
          </p>
          <p className={`text-xs ${summaryMetrics.totalDayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
            {formatPercentage(summaryMetrics.totalDayPnlPercent)}
          </p>
        </div>
      </div>

      {/* Controls */}
      <div className="glass-card rounded-xl p-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between space-y-4 sm:space-y-0">
          <div className="flex items-center space-x-4">
            <h2 className="text-xl font-semibold text-white">Multi-Broker Positions</h2>
            <button
              onClick={onRefresh}
              disabled={isLoading}
              className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors disabled:opacity-50"
              title="Refresh positions"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="flex items-center space-x-3">
            {/* Filter */}
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as FilterType)}
              className="bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm"
            >
              <option value="all">All Positions</option>
              <option value="profitable">Profitable Only</option>
              <option value="losing">Losing Only</option>
              <option value="break-even">Break-even</option>
            </select>

            {/* View Toggle */}
            <button
              onClick={() => setShowBrokerBreakdown(showBrokerBreakdown ? null : 'all')}
              className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                showBrokerBreakdown 
                  ? 'bg-blue-500 text-white' 
                  : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
              }`}
            >
              <Building2 className="w-4 h-4 inline mr-1" />
              Broker Details
            </button>
          </div>
        </div>
      </div>

      {/* Positions Table */}
      <div className="glass-card rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-800/50">
              <tr>
                <th className="text-left p-4">
                  <button
                    onClick={() => handleSort('symbol')}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white transition-colors"
                  >
                    <span>Symbol</span>
                    {sortField === 'symbol' && (
                      sortDirection === 'asc' ? <SortAsc className="w-3 h-3" /> : <SortDesc className="w-3 h-3" />
                    )}
                  </button>
                </th>
                <th className="text-right p-4">
                  <button
                    onClick={() => handleSort('totalValue')}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white transition-colors ml-auto"
                  >
                    <span>Value</span>
                    {sortField === 'totalValue' && (
                      sortDirection === 'asc' ? <SortAsc className="w-3 h-3" /> : <SortDesc className="w-3 h-3" />
                    )}
                  </button>
                </th>
                <th className="text-right p-4">
                  <button
                    onClick={() => handleSort('totalPnl')}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white transition-colors ml-auto"
                  >
                    <span>Total P&L</span>
                    {sortField === 'totalPnl' && (
                      sortDirection === 'asc' ? <SortAsc className="w-3 h-3" /> : <SortDesc className="w-3 h-3" />
                    )}
                  </button>
                </th>
                <th className="text-right p-4">
                  <button
                    onClick={() => handleSort('dayPnl')}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white transition-colors ml-auto"
                  >
                    <span>Day P&L</span>
                    {sortField === 'dayPnl' && (
                      sortDirection === 'asc' ? <SortAsc className="w-3 h-3" /> : <SortDesc className="w-3 h-3" />
                    )}
                  </button>
                </th>
                <th className="text-right p-4">
                  <span className="text-slate-300">Brokers</span>
                </th>
                <th className="text-center p-4">
                  <span className="text-slate-300">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence>
                {filteredAndSortedPositions.map((position) => (
                  <React.Fragment key={position.symbol}>
                    <motion.tr
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -20 }}
                      className="border-t border-slate-700/50 hover:bg-slate-800/30 transition-colors"
                    >
                      <td className="p-4">
                        <div>
                          <p className="font-semibold text-white">{position.symbol}</p>
                          <p className="text-sm text-slate-400">
                            {position.totalQuantity} shares @ ₹{position.avgPrice.toFixed(2)}
                          </p>
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        <p className="font-medium text-white">{formatCurrency(position.totalValue)}</p>
                        <p className="text-sm text-slate-400">₹{position.currentPrice.toFixed(2)}</p>
                      </td>
                      <td className="p-4 text-right">
                        <p className={`font-medium ${position.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {formatCurrency(position.totalPnl)}
                        </p>
                        <p className={`text-sm ${position.totalPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {formatPercentage(position.totalPnlPercent)}
                        </p>
                      </td>
                      <td className="p-4 text-right">
                        <p className={`font-medium ${position.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {formatCurrency(position.dayPnl)}
                        </p>
                        <p className={`text-sm ${position.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                          {formatPercentage(position.dayPnlPercent)}
                        </p>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex justify-end space-x-1">
                          {position.brokerPositions.map((brokerPos) => (
                            <span
                              key={brokerPos.brokerId}
                              className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs font-medium"
                              title={`${brokerPos.brokerName}: ${brokerPos.quantity} shares`}
                            >
                              {brokerPos.brokerName.slice(0, 3).toUpperCase()}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="p-4 text-center">
                        <button
                          onClick={() => setShowBrokerBreakdown(
                            showBrokerBreakdown === position.symbol ? null : position.symbol
                          )}
                          className="p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors"
                          title="Toggle broker breakdown"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </td>
                    </motion.tr>

                    {/* Broker Breakdown Row */}
                    <AnimatePresence>
                      {showBrokerBreakdown === position.symbol && (
                        <motion.tr
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                        >
                          <td colSpan={6} className="p-4 bg-slate-800/20">
                            <div className="space-y-3">
                              <h4 className="font-medium text-white text-sm">Broker Breakdown:</h4>
                              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                                {position.brokerPositions.map((brokerPos) => (
                                  <div
                                    key={brokerPos.brokerId}
                                    className="p-3 bg-slate-700/30 rounded-lg"
                                  >
                                    <div className="flex items-center justify-between mb-2">
                                      <span className="font-medium text-white text-sm">
                                        {brokerPos.brokerName}
                                      </span>
                                      <span className="text-xs text-slate-400">
                                        {brokerPos.quantity} shares
                                      </span>
                                    </div>
                                    <div className="grid grid-cols-2 gap-2 text-xs">
                                      <div>
                                        <p className="text-slate-400">P&L</p>
                                        <p className={brokerPos.pnl >= 0 ? 'text-green-400' : 'text-red-400'}>
                                          {formatCurrency(brokerPos.pnl)}
                                        </p>
                                      </div>
                                      <div>
                                        <p className="text-slate-400">Day P&L</p>
                                        <p className={brokerPos.dayPnl >= 0 ? 'text-green-400' : 'text-red-400'}>
                                          {formatCurrency(brokerPos.dayPnl)}
                                        </p>
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          </td>
                        </motion.tr>
                      )}
                    </AnimatePresence>
                  </React.Fragment>
                ))}
              </AnimatePresence>
            </tbody>
          </table>

          {filteredAndSortedPositions.length === 0 && (
            <div className="p-8 text-center">
              <AlertCircle className="w-12 h-12 mx-auto mb-3 text-slate-500" />
              <h3 className="font-medium text-white mb-1">No positions found</h3>
              <p className="text-sm text-slate-400">
                {filterType === 'all' 
                  ? 'No positions available across all brokers'
                  : `No ${filterType} positions found`
                }
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default MultiBrokerPositionDashboard