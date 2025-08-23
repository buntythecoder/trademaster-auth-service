import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  MoreHorizontal,
  ChevronDown,
  ChevronUp,
  Target,
  Calendar,
  DollarSign,
  Percent,
  BarChart3,
  Activity,
  AlertTriangle,
  CheckCircle,
  Clock
} from 'lucide-react'
import { usePortfolio } from '@/hooks/useWebSocket'
import { cn } from '@/lib/utils'

interface Position {
  symbol: string
  companyName: string
  quantity: number
  avgPrice: number
  currentPrice: number
  pnl: number
  pnlPercent: number
  marketValue: number
  allocation: number
  sector: string
  lastUpdated: string
  dayChange: number
  dayChangePercent: number
  high52Week: number
  low52Week: number
  pe: number
  marketCap: string
}

interface PositionBreakdownProps {
  showDetailed?: boolean
}

const generateMockPositions = (): Position[] => {
  const positions = [
    {
      symbol: 'RELIANCE',
      companyName: 'Reliance Industries Ltd',
      quantity: 50,
      avgPrice: 2420.50,
      currentPrice: 2547.30,
      sector: 'Energy',
      high52Week: 2856.15,
      low52Week: 2220.80,
      pe: 24.5,
      marketCap: '₹17.2 Lakh Cr'
    },
    {
      symbol: 'TCS',
      companyName: 'Tata Consultancy Services Ltd',
      quantity: 25,
      avgPrice: 3150.00,
      currentPrice: 3642.45,
      sector: 'Information Technology',
      high52Week: 4070.00,
      low52Week: 3000.45,
      pe: 28.2,
      marketCap: '₹13.4 Lakh Cr'
    },
    {
      symbol: 'HDFCBANK',
      companyName: 'HDFC Bank Ltd',
      quantity: 40,
      avgPrice: 1680.20,
      currentPrice: 1567.85,
      sector: 'Financial Services',
      high52Week: 1740.00,
      low52Week: 1363.55,
      pe: 18.7,
      marketCap: '₹11.8 Lakh Cr'
    },
    {
      symbol: 'INFY',
      companyName: 'Infosys Ltd',
      quantity: 35,
      avgPrice: 1320.75,
      currentPrice: 1423.20,
      sector: 'Information Technology',
      high52Week: 1953.90,
      low52Week: 1276.25,
      pe: 25.8,
      marketCap: '₹5.9 Lakh Cr'
    },
    {
      symbol: 'ICICIBANK',
      companyName: 'ICICI Bank Ltd',
      quantity: 60,
      avgPrice: 890.40,
      currentPrice: 950.25,
      sector: 'Financial Services',
      high52Week: 1036.05,
      low52Week: 798.65,
      pe: 16.4,
      marketCap: '₹6.6 Lakh Cr'
    }
  ]

  return positions.map(pos => {
    const marketValue = pos.quantity * pos.currentPrice
    const pnl = (pos.currentPrice - pos.avgPrice) * pos.quantity
    const pnlPercent = ((pos.currentPrice - pos.avgPrice) / pos.avgPrice) * 100
    const dayChange = (Math.random() - 0.5) * 50
    const dayChangePercent = (dayChange / pos.currentPrice) * 100

    return {
      ...pos,
      marketValue,
      pnl: parseFloat(pnl.toFixed(2)),
      pnlPercent: parseFloat(pnlPercent.toFixed(2)),
      allocation: 0, // Will be calculated later
      lastUpdated: new Date().toISOString(),
      dayChange: parseFloat(dayChange.toFixed(2)),
      dayChangePercent: parseFloat(dayChangePercent.toFixed(2))
    }
  })
}

export const PositionBreakdown: React.FC<PositionBreakdownProps> = ({ 
  showDetailed = true 
}) => {
  const { positions: livePositions } = usePortfolio()
  const [expandedPosition, setExpandedPosition] = useState<string | null>(null)
  const [sortBy, setSortBy] = useState<'allocation' | 'pnl' | 'pnlPercent' | 'marketValue'>('allocation')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')

  // Use mock data for now, replace with livePositions when available
  const positions = React.useMemo(() => {
    const mockPositions = generateMockPositions()
    const totalValue = mockPositions.reduce((sum, pos) => sum + pos.marketValue, 0)
    
    // Calculate allocations
    return mockPositions.map(pos => ({
      ...pos,
      allocation: parseFloat(((pos.marketValue / totalValue) * 100).toFixed(1))
    }))
  }, [])

  const sortedPositions = React.useMemo(() => {
    return [...positions].sort((a, b) => {
      const aValue = a[sortBy]
      const bValue = b[sortBy]
      
      if (sortOrder === 'asc') {
        return aValue < bValue ? -1 : 1
      } else {
        return aValue > bValue ? -1 : 1
      }
    })
  }, [positions, sortBy, sortOrder])

  const handleSort = (field: typeof sortBy) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')
    } else {
      setSortBy(field)
      setSortOrder('desc')
    }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const formatNumber = (num: number, decimals: number = 2) => {
    return num.toLocaleString('en-IN', { 
      minimumFractionDigits: decimals, 
      maximumFractionDigits: decimals 
    })
  }

  const getSectorColor = (sector: string) => {
    const colors: Record<string, string> = {
      'Information Technology': 'text-blue-400 bg-blue-500/20',
      'Financial Services': 'text-green-400 bg-green-500/20',
      'Energy': 'text-orange-400 bg-orange-500/20',
      'Healthcare': 'text-purple-400 bg-purple-500/20',
      'Consumer Goods': 'text-pink-400 bg-pink-500/20'
    }
    return colors[sector] || 'text-cyan-400 bg-cyan-500/20'
  }

  const totalPortfolioValue = positions.reduce((sum, pos) => sum + pos.marketValue, 0)
  const totalPnL = positions.reduce((sum, pos) => sum + pos.pnl, 0)
  const totalPnLPercent = (totalPnL / (totalPortfolioValue - totalPnL)) * 100

  return (
    <div className="glass-card rounded-2xl p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-xl font-bold text-white mb-2">Position Analysis</h3>
          <p className="text-sm text-slate-400">Detailed breakdown of your holdings</p>
        </div>
        
        {/* Summary Stats */}
        <div className="flex items-center space-x-6">
          <div className="text-right">
            <div className="text-sm text-slate-400">Total Value</div>
            <div className="text-lg font-bold text-white">
              {formatCurrency(totalPortfolioValue)}
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-400">Total P&L</div>
            <div className={`text-lg font-bold ${totalPnL >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {totalPnL >= 0 ? '+' : ''}{formatCurrency(totalPnL)}
            </div>
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-400">Return</div>
            <div className={`text-lg font-bold ${totalPnLPercent >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {totalPnLPercent >= 0 ? '+' : ''}{formatNumber(totalPnLPercent)}%
            </div>
          </div>
        </div>
      </div>

      {/* Sorting Controls */}
      <div className="flex items-center space-x-2 mb-4">
        <span className="text-sm text-slate-400">Sort by:</span>
        {[
          { key: 'allocation' as const, label: 'Allocation' },
          { key: 'pnl' as const, label: 'P&L Amount' },
          { key: 'pnlPercent' as const, label: 'P&L %' },
          { key: 'marketValue' as const, label: 'Market Value' }
        ].map(option => (
          <button
            key={option.key}
            onClick={() => handleSort(option.key)}
            className={cn(
              'px-3 py-1 text-sm rounded-lg transition-colors flex items-center space-x-1',
              sortBy === option.key
                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            )}
          >
            <span>{option.label}</span>
            {sortBy === option.key && (
              sortOrder === 'desc' ? <ChevronDown className="w-3 h-3" /> : <ChevronUp className="w-3 h-3" />
            )}
          </button>
        ))}
      </div>

      {/* Positions Table */}
      <div className="space-y-2">
        {sortedPositions.map((position) => (
          <motion.div
            key={position.symbol}
            layout
            className="bg-slate-800/30 rounded-xl overflow-hidden hover:bg-slate-700/30 transition-all duration-200"
          >
            {/* Main Row */}
            <div className="p-4">
              <div className="grid grid-cols-12 gap-4 items-center">
                {/* Stock Info */}
                <div className="col-span-3">
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0">
                      <div className="w-10 h-10 bg-gradient-to-br from-purple-500/20 to-cyan-500/20 rounded-lg flex items-center justify-center">
                        <span className="text-white font-semibold text-sm">
                          {position.symbol.substring(0, 2)}
                        </span>
                      </div>
                    </div>
                    <div>
                      <div className="font-semibold text-white">{position.symbol}</div>
                      <div className="text-xs text-slate-400">{position.companyName}</div>
                      <div className={cn('text-xs px-2 py-1 rounded-full mt-1 inline-block', getSectorColor(position.sector))}>
                        {position.sector}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Quantity & Avg Price */}
                <div className="col-span-2 text-center">
                  <div className="text-white font-medium">{position.quantity}</div>
                  <div className="text-xs text-slate-400">@ ₹{formatNumber(position.avgPrice, 2)}</div>
                </div>

                {/* Current Price */}
                <div className="col-span-2 text-center">
                  <div className="text-white font-semibold">₹{formatNumber(position.currentPrice, 2)}</div>
                  <div className={cn(
                    'text-xs flex items-center justify-center space-x-1',
                    position.dayChangePercent >= 0 ? 'text-green-400' : 'text-red-400'
                  )}>
                    {position.dayChangePercent >= 0 ? (
                      <TrendingUp className="w-3 h-3" />
                    ) : (
                      <TrendingDown className="w-3 h-3" />
                    )}
                    <span>{position.dayChangePercent >= 0 ? '+' : ''}{formatNumber(position.dayChangePercent)}%</span>
                  </div>
                </div>

                {/* Market Value */}
                <div className="col-span-2 text-center">
                  <div className="text-white font-semibold">{formatCurrency(position.marketValue)}</div>
                  <div className="text-xs text-slate-400">{position.allocation}% of portfolio</div>
                </div>

                {/* P&L */}
                <div className="col-span-2 text-center">
                  <div className={cn(
                    'font-semibold',
                    position.pnl >= 0 ? 'text-green-400' : 'text-red-400'
                  )}>
                    {position.pnl >= 0 ? '+' : ''}{formatCurrency(position.pnl)}
                  </div>
                  <div className={cn(
                    'text-sm',
                    position.pnlPercent >= 0 ? 'text-green-400' : 'text-red-400'
                  )}>
                    ({position.pnlPercent >= 0 ? '+' : ''}{formatNumber(position.pnlPercent)}%)
                  </div>
                </div>

                {/* Actions */}
                <div className="col-span-1 flex justify-end">
                  <button
                    onClick={() => setExpandedPosition(
                      expandedPosition === position.symbol ? null : position.symbol
                    )}
                    className="p-2 hover:bg-slate-600/50 rounded-lg transition-colors"
                  >
                    <MoreHorizontal className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
              </div>
            </div>

            {/* Expanded Details */}
            <AnimatePresence>
              {expandedPosition === position.symbol && showDetailed && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="border-t border-slate-700/50"
                >
                  <div className="p-6 bg-slate-800/50">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                      {/* Performance Metrics */}
                      <div>
                        <h4 className="text-sm font-semibold text-slate-300 mb-3 flex items-center">
                          <Target className="w-4 h-4 mr-2 text-purple-400" />
                          Performance
                        </h4>
                        <div className="space-y-2">
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Current:</span>
                            <span className="text-xs text-white">₹{formatNumber(position.currentPrice, 2)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Avg Price:</span>
                            <span className="text-xs text-white">₹{formatNumber(position.avgPrice, 2)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">P&L:</span>
                            <span className={cn(
                              'text-xs font-medium',
                              position.pnl >= 0 ? 'text-green-400' : 'text-red-400'
                            )}>
                              {position.pnl >= 0 ? '+' : ''}{formatCurrency(position.pnl)}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Market Data */}
                      <div>
                        <h4 className="text-sm font-semibold text-slate-300 mb-3 flex items-center">
                          <BarChart3 className="w-4 h-4 mr-2 text-cyan-400" />
                          Market Data
                        </h4>
                        <div className="space-y-2">
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">52W High:</span>
                            <span className="text-xs text-white">₹{formatNumber(position.high52Week, 2)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">52W Low:</span>
                            <span className="text-xs text-white">₹{formatNumber(position.low52Week, 2)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">P/E Ratio:</span>
                            <span className="text-xs text-white">{position.pe}</span>
                          </div>
                        </div>
                      </div>

                      {/* Portfolio Impact */}
                      <div>
                        <h4 className="text-sm font-semibold text-slate-300 mb-3 flex items-center">
                          <Activity className="w-4 h-4 mr-2 text-green-400" />
                          Portfolio Impact
                        </h4>
                        <div className="space-y-2">
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Allocation:</span>
                            <span className="text-xs text-white">{position.allocation}%</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Market Value:</span>
                            <span className="text-xs text-white">{formatCurrency(position.marketValue)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Market Cap:</span>
                            <span className="text-xs text-white">{position.marketCap}</span>
                          </div>
                        </div>
                      </div>

                      {/* Risk Indicators */}
                      <div>
                        <h4 className="text-sm font-semibold text-slate-300 mb-3 flex items-center">
                          <AlertTriangle className="w-4 h-4 mr-2 text-orange-400" />
                          Risk Profile
                        </h4>
                        <div className="space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-slate-400">Risk Level:</span>
                            <span className={cn(
                              'text-xs px-2 py-1 rounded-full',
                              position.allocation > 20 ? 'bg-red-500/20 text-red-400' :
                              position.allocation > 15 ? 'bg-orange-500/20 text-orange-400' :
                              'bg-green-500/20 text-green-400'
                            )}>
                              {position.allocation > 20 ? 'High' : 
                               position.allocation > 15 ? 'Medium' : 'Low'}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Sector:</span>
                            <span className="text-xs text-white">{position.sector}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-xs text-slate-400">Last Updated:</span>
                            <span className="text-xs text-slate-400">
                              {new Date(position.lastUpdated).toLocaleTimeString('en-IN', { 
                                hour: '2-digit', 
                                minute: '2-digit' 
                              })}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center justify-end space-x-3 mt-6 pt-4 border-t border-slate-700/50">
                      <button className="cyber-button px-4 py-2 text-sm">
                        Buy More
                      </button>
                      <button className="glass-card px-4 py-2 text-sm text-white hover:text-red-300 transition-colors border border-red-500/50 hover:border-red-400/70">
                        Sell
                      </button>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        ))}
      </div>
    </div>
  )
}

export default PositionBreakdown