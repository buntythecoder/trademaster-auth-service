import React, { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  ArrowUpDown, 
  Filter, 
  Search, 
  TrendingUp, 
  TrendingDown,
  MoreHorizontal,
  Plus,
  Minus,
  Bell,
  Eye,
  ChevronDown,
  ChevronUp
} from 'lucide-react'
import { PortfolioData, HoldingPosition } from '../../hooks/usePortfolioWebSocket'
import { cn } from '../../lib/utils'

interface HoldingsTableProps {
  portfolio: PortfolioData | null
}

type SortField = 'name' | 'value' | 'returns' | 'allocation' | 'dayChange'
type SortOrder = 'asc' | 'desc'
type GroupBy = 'none' | 'sector' | 'asset_type'

export function HoldingsTable({ portfolio }: HoldingsTableProps) {
  const [sortField, setSortField] = useState<SortField>('value')
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc')
  const [groupBy, setGroupBy] = useState<GroupBy>('none')
  const [searchTerm, setSearchTerm] = useState('')
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())
  const [showAllHoldings, setShowAllHoldings] = useState(false)
  const [selectedHolding, setSelectedHolding] = useState<string | null>(null)

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

  // Sort and filter holdings
  const processedHoldings = useMemo(() => {
    if (!portfolio?.holdings) return []

    let filtered = portfolio.holdings.filter(holding =>
      holding.symbol.toLowerCase().includes(searchTerm.toLowerCase()) ||
      holding.companyName.toLowerCase().includes(searchTerm.toLowerCase())
    )

    // Sort holdings
    filtered.sort((a, b) => {
      let aVal: number, bVal: number

      switch (sortField) {
        case 'name':
          return sortOrder === 'asc' 
            ? a.symbol.localeCompare(b.symbol)
            : b.symbol.localeCompare(a.symbol)
        case 'value':
          aVal = a.marketValue
          bVal = b.marketValue
          break
        case 'returns':
          aVal = a.totalReturnPercent
          bVal = b.totalReturnPercent
          break
        case 'allocation':
          aVal = a.allocation
          bVal = b.allocation
          break
        case 'dayChange':
          aVal = a.dayChangePercent
          bVal = b.dayChangePercent
          break
        default:
          aVal = a.marketValue
          bVal = b.marketValue
      }

      return sortOrder === 'asc' ? aVal - bVal : bVal - aVal
    })

    return filtered
  }, [portfolio?.holdings, searchTerm, sortField, sortOrder])

  // Group holdings if needed
  const groupedHoldings = useMemo(() => {
    if (groupBy === 'none') return { 'All Holdings': processedHoldings }

    const groups: { [key: string]: HoldingPosition[] } = {}
    
    processedHoldings.forEach(holding => {
      const groupKey = groupBy === 'sector' ? holding.sector : holding.assetType
      if (!groups[groupKey]) groups[groupKey] = []
      groups[groupKey].push(holding)
    })

    return groups
  }, [processedHoldings, groupBy])

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortOrder('desc')
    }
  }

  const toggleGroup = (groupName: string) => {
    const newExpanded = new Set(expandedGroups)
    if (newExpanded.has(groupName)) {
      newExpanded.delete(groupName)
    } else {
      newExpanded.add(groupName)
    }
    setExpandedGroups(newExpanded)
  }

  const displayedHoldings = showAllHoldings ? processedHoldings : processedHoldings.slice(0, 6)

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-slate-700 rounded w-1/4"></div>
          <div className="space-y-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-16 bg-slate-700 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-2xl p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-white">Holdings ({portfolio.holdings.length} positions)</h3>
          <p className="text-sm text-slate-400">Detailed position breakdown</p>
        </div>

        {/* Controls */}
        <div className="flex items-center space-x-2">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search holdings..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="cyber-input pl-10 pr-3 py-2 text-sm rounded-xl w-48"
            />
          </div>

          <select
            value={`${sortField}-${sortOrder}`}
            onChange={(e) => {
              const [field, order] = e.target.value.split('-') as [SortField, SortOrder]
              setSortField(field)
              setSortOrder(order)
            }}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="value-desc">Value ↓</option>
            <option value="value-asc">Value ↑</option>
            <option value="returns-desc">Returns ↓</option>
            <option value="returns-asc">Returns ↑</option>
            <option value="dayChange-desc">Day Change ↓</option>
            <option value="dayChange-asc">Day Change ↑</option>
            <option value="name-asc">Name A-Z</option>
            <option value="name-desc">Name Z-A</option>
          </select>

          <button className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors">
            <Filter className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Holdings List */}
      <div className="space-y-3">
        <AnimatePresence>
          {displayedHoldings.map((holding, index) => (
            <motion.div
              key={holding.symbol}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ delay: index * 0.05 }}
              className={cn(
                "group relative bg-slate-800/30 border border-slate-700/50 rounded-xl p-4 transition-all cursor-pointer",
                "hover:border-purple-500/50 hover:bg-slate-800/50",
                selectedHolding === holding.symbol && "border-cyan-500/50 bg-slate-800/70"
              )}
              onClick={() => setSelectedHolding(selectedHolding === holding.symbol ? null : holding.symbol)}
            >
              {/* Swipe action indicators */}
              <div className="absolute left-2 top-1/2 transform -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity">
                <div className="flex space-x-1">
                  <div className="w-1 h-8 bg-red-400 rounded-full"></div>
                  <div className="w-1 h-8 bg-yellow-400 rounded-full"></div>
                  <div className="w-1 h-8 bg-green-400 rounded-full"></div>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0 pl-4 lg:pl-0">
                  {/* Symbol and Company */}
                  <div className="flex items-center space-x-3 mb-2">
                    <div className="flex-shrink-0">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-white font-bold text-sm">
                        {holding.symbol.slice(0, 2)}
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-white font-semibold">{holding.symbol}</div>
                      <div className="text-slate-400 text-sm truncate">{holding.companyName}</div>
                    </div>
                    <div className="flex-shrink-0 text-right">
                      <div className="text-white font-medium">{formatCurrency(holding.marketValue)}</div>
                      <div className="text-slate-400 text-sm">({holding.allocation.toFixed(1)}%)</div>
                    </div>
                  </div>

                  {/* Quantity and Price */}
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
                    <div>
                      <div className="text-slate-400">Quantity</div>
                      <div className="text-white">{formatNumber(holding.quantity, 0)}</div>
                    </div>
                    <div>
                      <div className="text-slate-400">Avg Price</div>
                      <div className="text-white">₹{formatNumber(holding.avgPrice)}</div>
                    </div>
                    <div>
                      <div className="text-slate-400">Current Price</div>
                      <div className="text-white">₹{formatNumber(holding.currentPrice)}</div>
                    </div>
                    <div>
                      <div className="text-slate-400">Day Change</div>
                      <div className={cn(
                        "flex items-center space-x-1 font-medium",
                        holding.dayChangePercent >= 0 ? "text-green-400" : "text-red-400"
                      )}>
                        {holding.dayChangePercent >= 0 ? (
                          <TrendingUp className="w-3 h-3" />
                        ) : (
                          <TrendingDown className="w-3 h-3" />
                        )}
                        <span>
                          {holding.dayChangePercent >= 0 ? '+' : ''}{formatNumber(holding.dayChangePercent)}%
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Total Returns */}
                  <div className="mt-3 pt-3 border-t border-slate-700/50">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-4">
                        <div>
                          <div className="text-slate-400 text-sm">Total Return</div>
                          <div className={cn(
                            "font-semibold",
                            holding.totalReturnPercent >= 0 ? "text-green-400" : "text-red-400"
                          )}>
                            {holding.totalReturnPercent >= 0 ? '+' : ''}{formatCurrency(holding.totalReturn)} 
                            <span className="text-sm ml-1">
                              ({holding.totalReturnPercent >= 0 ? '+' : ''}{formatNumber(holding.totalReturnPercent)}%)
                            </span>
                          </div>
                        </div>
                        <div className="text-slate-400 text-sm">
                          {holding.sector} • {holding.assetType}
                        </div>
                      </div>

                      {/* Action Menu */}
                      <div className="flex items-center space-x-2">
                        <button className="p-1.5 rounded-lg hover:bg-slate-700/50 text-slate-400 hover:text-white transition-colors">
                          <Plus className="w-4 h-4" />
                        </button>
                        <button className="p-1.5 rounded-lg hover:bg-slate-700/50 text-slate-400 hover:text-white transition-colors">
                          <Minus className="w-4 h-4" />
                        </button>
                        <button className="p-1.5 rounded-lg hover:bg-slate-700/50 text-slate-400 hover:text-white transition-colors">
                          <Bell className="w-4 h-4" />
                        </button>
                        <button className="p-1.5 rounded-lg hover:bg-slate-700/50 text-slate-400 hover:text-white transition-colors">
                          <MoreHorizontal className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Selection indicator */}
              {selectedHolding === holding.symbol && (
                <motion.div
                  initial={{ scaleX: 0 }}
                  animate={{ scaleX: 1 }}
                  className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-cyan-500 to-purple-500 rounded-b-xl"
                />
              )}
            </motion.div>
          ))}
        </AnimatePresence>

        {/* Show More/Less Button */}
        {portfolio.holdings.length > 6 && (
          <motion.button
            onClick={() => setShowAllHoldings(!showAllHoldings)}
            className="w-full py-3 rounded-xl glass-card text-slate-400 hover:text-white transition-colors flex items-center justify-center space-x-2"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            {showAllHoldings ? (
              <>
                <ChevronUp className="w-4 h-4" />
                <span>Show Less</span>
              </>
            ) : (
              <>
                <ChevronDown className="w-4 h-4" />
                <span>View All {portfolio.holdings.length} Holdings</span>
              </>
            )}
          </motion.button>
        )}
      </div>

      {/* Holdings Summary */}
      <div className="pt-6 border-t border-slate-700/50">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
          <div className="text-center">
            <div className="text-slate-400 mb-1">Best Performer</div>
            {portfolio.holdings.length > 0 && (
              <>
                <div className="text-green-400 font-medium">
                  {portfolio.holdings.reduce((best, holding) => 
                    holding.totalReturnPercent > best.totalReturnPercent ? holding : best
                  ).symbol}
                </div>
                <div className="text-green-400 text-xs">
                  +{portfolio.holdings.reduce((best, holding) => 
                    holding.totalReturnPercent > best.totalReturnPercent ? holding : best
                  ).totalReturnPercent.toFixed(1)}%
                </div>
              </>
            )}
          </div>

          <div className="text-center">
            <div className="text-slate-400 mb-1">Worst Performer</div>
            {portfolio.holdings.length > 0 && (
              <>
                <div className="text-red-400 font-medium">
                  {portfolio.holdings.reduce((worst, holding) => 
                    holding.totalReturnPercent < worst.totalReturnPercent ? holding : worst
                  ).symbol}
                </div>
                <div className="text-red-400 text-xs">
                  {portfolio.holdings.reduce((worst, holding) => 
                    holding.totalReturnPercent < worst.totalReturnPercent ? holding : worst
                  ).totalReturnPercent.toFixed(1)}%
                </div>
              </>
            )}
          </div>

          <div className="text-center">
            <div className="text-slate-400 mb-1">Largest Position</div>
            {portfolio.holdings.length > 0 && (
              <>
                <div className="text-white font-medium">
                  {portfolio.holdings.reduce((largest, holding) => 
                    holding.allocation > largest.allocation ? holding : largest
                  ).symbol}
                </div>
                <div className="text-slate-400 text-xs">
                  {portfolio.holdings.reduce((largest, holding) => 
                    holding.allocation > largest.allocation ? holding : largest
                  ).allocation.toFixed(1)}%
                </div>
              </>
            )}
          </div>

          <div className="text-center">
            <div className="text-slate-400 mb-1">Total Positions</div>
            <div className="text-white font-medium">{portfolio.holdings.length}</div>
            <div className="text-slate-400 text-xs">Active holdings</div>
          </div>
        </div>
      </div>
    </div>
  )
}