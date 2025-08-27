import React, { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { PieChart, BarChart3, Grid3X3, AlertTriangle, Target } from 'lucide-react'
import { PortfolioData } from '../../hooks/usePortfolioWebSocket'
import { cn } from '../../lib/utils'

interface AssetAllocationProps {
  portfolio: PortfolioData | null
}

interface AllocationBreakdown {
  category: string
  value: number
  percentage: number
  color: string
  holdings: string[]
  target?: number
  deviation?: number
}

type ChartType = 'donut' | 'pie' | 'treemap' | 'bar'
type GroupBy = 'sector' | 'asset_type' | 'market_cap' | 'geography'

export function AssetAllocation({ portfolio }: AssetAllocationProps) {
  const [chartType, setChartType] = useState<ChartType>('donut')
  const [groupBy, setGroupBy] = useState<GroupBy>('sector')
  const [showPercentages, setShowPercentages] = useState(true)
  const [selectedSegment, setSelectedSegment] = useState<string | null>(null)

  const sectorColors = {
    'Technology': '#3B82F6',
    'Banking & Finance': '#10B981',
    'Healthcare': '#8B5CF6',
    'Consumer Goods': '#F59E0B',
    'Energy': '#EF4444',
    'Manufacturing': '#06B6D4',
    'Pharma': '#EC4899',
    'Others': '#6B7280'
  }

  const assetTypeColors = {
    'EQUITY': '#3B82F6',
    'ETF': '#8B5CF6',
    'MUTUAL_FUND': '#10B981'
  }

  const getTargetAllocation = (category: string, groupType: GroupBy): number | undefined => {
    if (groupType === 'sector') {
      const targets: { [key: string]: number } = {
        'Technology': 30,
        'Banking & Finance': 20,
        'Healthcare': 15,
        'Consumer Goods': 15,
        'Energy': 10,
        'Manufacturing': 10
      }
      return targets[category]
    }
    return undefined
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      notation: amount > 1000000 ? 'compact' : 'standard',
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(amount)
  }

  // Calculate allocation breakdown
  const allocationData = useMemo(() => {
    if (!portfolio?.holdings) return []

    const groups: { [key: string]: AllocationBreakdown } = {}
    const totalValue = portfolio.summary.currentValue

    portfolio.holdings.forEach(holding => {
      const groupKey = groupBy === 'sector' ? holding.sector : holding.assetType
      
      if (!groups[groupKey]) {
        groups[groupKey] = {
          category: groupKey,
          value: 0,
          percentage: 0,
          color: groupBy === 'sector' 
            ? sectorColors[groupKey as keyof typeof sectorColors] || '#6B7280'
            : assetTypeColors[groupKey as keyof typeof assetTypeColors] || '#6B7280',
          holdings: [],
          target: getTargetAllocation(groupKey, groupBy),
        }
      }

      groups[groupKey].value += holding.marketValue
      groups[groupKey].holdings.push(holding.symbol)
    })

    // Calculate percentages and deviations
    Object.values(groups).forEach(group => {
      group.percentage = (group.value / totalValue) * 100
      if (group.target) {
        group.deviation = group.percentage - group.target
      }
    })

    return Object.values(groups).sort((a, b) => b.percentage - a.percentage)
  }, [portfolio, groupBy])

  const getDiversificationScore = () => {
    if (!allocationData.length) return 0
    
    // Calculate diversification using Herfindahl-Hirschman Index
    const hhi = allocationData.reduce((sum, item) => {
      const share = item.percentage / 100
      return sum + (share * share)
    }, 0)
    
    // Convert to 0-10 scale (lower HHI = better diversification)
    return Math.max(0, (1 - hhi) * 10)
  }

  const getRecommendations = () => {
    const recommendations = []
    
    allocationData.forEach(item => {
      if (item.deviation && Math.abs(item.deviation) > 5) {
        if (item.deviation > 0) {
          recommendations.push({
            type: 'warning',
            message: `Consider reducing ${item.category} allocation by ${item.deviation.toFixed(1)}%`,
            category: item.category
          })
        } else {
          recommendations.push({
            type: 'info',
            message: `Consider increasing ${item.category} allocation by ${Math.abs(item.deviation).toFixed(1)}%`,
            category: item.category
          })
        }
      }
    })

    const diversificationScore = getDiversificationScore()
    if (diversificationScore < 6) {
      recommendations.push({
        type: 'warning',
        message: 'Portfolio concentration is high. Consider diversifying across more sectors.',
        category: 'Diversification'
      })
    }

    return recommendations
  }

  const recommendations = getRecommendations()
  const diversificationScore = getDiversificationScore()

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-slate-700 rounded w-1/3"></div>
          <div className="w-48 h-48 bg-slate-700 rounded-full mx-auto"></div>
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex justify-between">
                <div className="h-4 bg-slate-700 rounded w-1/3"></div>
                <div className="h-4 bg-slate-700 rounded w-1/4"></div>
              </div>
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
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-lg bg-purple-500/20">
            <PieChart className="w-5 h-5 text-purple-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">Asset Allocation</h3>
            <p className="text-sm text-slate-400">Portfolio diversification breakdown</p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center space-x-2">
          <select
            value={groupBy}
            onChange={(e) => setGroupBy(e.target.value as GroupBy)}
            className="cyber-input px-3 py-2 text-sm rounded-xl"
          >
            <option value="sector">By Sector</option>
            <option value="asset_type">By Asset Type</option>
            <option value="market_cap">By Market Cap</option>
          </select>

          <div className="flex rounded-xl glass-card p-1">
            <button
              onClick={() => setChartType('donut')}
              className={cn(
                "p-1.5 rounded-lg transition-all",
                chartType === 'donut'
                  ? "bg-purple-500/20 text-purple-300"
                  : "text-slate-400 hover:text-white"
              )}
            >
              <PieChart className="w-4 h-4" />
            </button>
            <button
              onClick={() => setChartType('bar')}
              className={cn(
                "p-1.5 rounded-lg transition-all",
                chartType === 'bar'
                  ? "bg-purple-500/20 text-purple-300"
                  : "text-slate-400 hover:text-white"
              )}
            >
              <BarChart3 className="w-4 h-4" />
            </button>
            <button
              onClick={() => setChartType('treemap')}
              className={cn(
                "p-1.5 rounded-lg transition-all",
                chartType === 'treemap'
                  ? "bg-purple-500/20 text-purple-300"
                  : "text-slate-400 hover:text-white"
              )}
            >
              <Grid3X3 className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Chart Area */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart Visualization */}
        <div className="relative">
          {chartType === 'donut' && (
            <div className="relative w-64 h-64 mx-auto">
              {/* Mock Donut Chart */}
              <svg width="256" height="256" className="transform -rotate-90">
                <defs>
                  {allocationData.map((item, index) => (
                    <linearGradient key={item.category} id={`gradient-${index}`} x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor={item.color} stopOpacity="0.8" />
                      <stop offset="100%" stopColor={item.color} stopOpacity="1" />
                    </linearGradient>
                  ))}
                </defs>

                <circle
                  cx="128"
                  cy="128"
                  r="100"
                  fill="none"
                  stroke="rgb(51, 65, 85)"
                  strokeWidth="60"
                />

                {/* Render segments */}
                {allocationData.map((item, index) => {
                  let cumulativePercentage = 0
                  for (let i = 0; i < index; i++) {
                    cumulativePercentage += allocationData[i].percentage
                  }
                  
                  const startAngle = (cumulativePercentage / 100) * 360
                  const endAngle = ((cumulativePercentage + item.percentage) / 100) * 360
                  
                  const startAngleRad = (startAngle * Math.PI) / 180
                  const endAngleRad = (endAngle * Math.PI) / 180
                  
                  const x1 = 128 + 100 * Math.cos(startAngleRad)
                  const y1 = 128 + 100 * Math.sin(startAngleRad)
                  const x2 = 128 + 100 * Math.cos(endAngleRad)
                  const y2 = 128 + 100 * Math.sin(endAngleRad)
                  
                  const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0
                  
                  const pathData = [
                    `M 128 128`,
                    `L ${x1} ${y1}`,
                    `A 100 100 0 ${largeArcFlag} 1 ${x2} ${y2}`,
                    'Z'
                  ].join(' ')

                  return (
                    <motion.path
                      key={item.category}
                      d={pathData}
                      fill={`url(#gradient-${index})`}
                      className={cn(
                        "cursor-pointer transition-opacity",
                        selectedSegment && selectedSegment !== item.category && "opacity-50"
                      )}
                      onClick={() => setSelectedSegment(
                        selectedSegment === item.category ? null : item.category
                      )}
                      whileHover={{ scale: 1.05 }}
                    />
                  )
                })}
              </svg>

              {/* Center Value */}
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="text-center">
                  <div className="text-2xl font-bold text-white">
                    {formatCurrency(portfolio.summary.currentValue)}
                  </div>
                  <div className="text-sm text-slate-400">Total Value</div>
                </div>
              </div>
            </div>
          )}

          {chartType === 'bar' && (
            <div className="space-y-3">
              {allocationData.map((item, index) => (
                <motion.div
                  key={item.category}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.1 }}
                  className={cn(
                    "relative cursor-pointer",
                    selectedSegment && selectedSegment !== item.category && "opacity-50"
                  )}
                  onClick={() => setSelectedSegment(
                    selectedSegment === item.category ? null : item.category
                  )}
                >
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center space-x-2">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: item.color }}
                      />
                      <span className="text-white text-sm font-medium">{item.category}</span>
                    </div>
                    <span className="text-slate-400 text-sm">{item.percentage.toFixed(1)}%</span>
                  </div>
                  <div className="w-full bg-slate-700 rounded-full h-3">
                    <motion.div
                      className="h-3 rounded-full"
                      style={{ backgroundColor: item.color }}
                      initial={{ width: 0 }}
                      animate={{ width: `${item.percentage}%` }}
                      transition={{ duration: 0.8, delay: index * 0.1 }}
                    />
                  </div>
                </motion.div>
              ))}
            </div>
          )}

          {chartType === 'treemap' && (
            <div className="grid grid-cols-2 gap-2 h-64">
              {allocationData.map((item, index) => (
                <motion.div
                  key={item.category}
                  className={cn(
                    "rounded-xl p-4 cursor-pointer transition-opacity",
                    selectedSegment && selectedSegment !== item.category && "opacity-50"
                  )}
                  style={{ backgroundColor: `${item.color}20`, borderColor: `${item.color}40` }}
                  onClick={() => setSelectedSegment(
                    selectedSegment === item.category ? null : item.category
                  )}
                  whileHover={{ scale: 1.02 }}
                  layout
                >
                  <div className="text-white font-medium text-sm">{item.category}</div>
                  <div className="text-2xl font-bold mt-2" style={{ color: item.color }}>
                    {item.percentage.toFixed(1)}%
                  </div>
                  <div className="text-slate-400 text-xs">{formatCurrency(item.value)}</div>
                </motion.div>
              ))}
            </div>
          )}
        </div>

        {/* Legend and Details */}
        <div className="space-y-4">
          {/* Diversification Score */}
          <div className="glass-card p-4 rounded-xl">
            <div className="flex items-center justify-between mb-3">
              <div className="text-white font-medium">Diversification Score</div>
              <div className={cn(
                "text-2xl font-bold",
                diversificationScore >= 7 ? "text-green-400" : 
                diversificationScore >= 5 ? "text-yellow-400" : "text-red-400"
              )}>
                {diversificationScore.toFixed(1)}/10
              </div>
            </div>
            <div className="w-full bg-slate-700 rounded-full h-2">
              <div
                className={cn(
                  "h-2 rounded-full transition-all duration-1000",
                  diversificationScore >= 7 ? "bg-green-400" :
                  diversificationScore >= 5 ? "bg-yellow-400" : "bg-red-400"
                )}
                style={{ width: `${(diversificationScore / 10) * 100}%` }}
              />
            </div>
          </div>

          {/* Legend */}
          <div className="space-y-3">
            <AnimatePresence>
              {allocationData.map((item, index) => (
                <motion.div
                  key={item.category}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  transition={{ delay: index * 0.05 }}
                  className={cn(
                    "flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all",
                    selectedSegment === item.category 
                      ? "bg-slate-800/70 border border-purple-500/30" 
                      : "hover:bg-slate-800/30"
                  )}
                  onClick={() => setSelectedSegment(
                    selectedSegment === item.category ? null : item.category
                  )}
                >
                  <div className="flex items-center space-x-3">
                    <div
                      className="w-4 h-4 rounded-full flex-shrink-0"
                      style={{ backgroundColor: item.color }}
                    />
                    <div className="min-w-0">
                      <div className="text-white font-medium">{item.category}</div>
                      <div className="text-slate-400 text-sm">
                        {item.holdings.length} holding{item.holdings.length !== 1 ? 's' : ''}
                      </div>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <div className="text-white font-medium">{formatCurrency(item.value)}</div>
                    <div className="text-slate-400 text-sm">{item.percentage.toFixed(1)}%</div>
                    {item.target && (
                      <div className={cn(
                        "text-xs",
                        Math.abs(item.deviation || 0) > 5
                          ? (item.deviation || 0) > 0 ? "text-orange-400" : "text-blue-400"
                          : "text-green-400"
                      )}>
                        Target: {item.target}%
                      </div>
                    )}
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>

          {/* Selected Segment Details */}
          <AnimatePresence>
            {selectedSegment && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="glass-card p-4 rounded-xl"
              >
                <div className="text-white font-medium mb-2">{selectedSegment} Holdings</div>
                <div className="space-y-2">
                  {allocationData
                    .find(item => item.category === selectedSegment)
                    ?.holdings.slice(0, 5)
                    .map(symbol => (
                      <div key={symbol} className="flex justify-between text-sm">
                        <span className="text-slate-300">{symbol}</span>
                        <span className="text-slate-400">
                          {portfolio.holdings.find(h => h.symbol === symbol)?.allocation.toFixed(1)}%
                        </span>
                      </div>
                    ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Recommendations */}
      {recommendations.length > 0 && (
        <div className="pt-6 border-t border-slate-700/50">
          <h4 className="text-white font-medium mb-4 flex items-center space-x-2">
            <Target className="w-4 h-4 text-cyan-400" />
            <span>Rebalancing Suggestions</span>
          </h4>
          <div className="space-y-3">
            {recommendations.slice(0, 3).map((rec, index) => (
              <div
                key={index}
                className={cn(
                  "p-3 rounded-lg border",
                  rec.type === 'warning' 
                    ? "bg-orange-500/10 border-orange-500/20 text-orange-300"
                    : "bg-blue-500/10 border-blue-500/20 text-blue-300"
                )}
              >
                <div className="flex items-start space-x-2">
                  <AlertTriangle className="w-4 h-4 mt-0.5 flex-shrink-0" />
                  <div className="text-sm">{rec.message}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}