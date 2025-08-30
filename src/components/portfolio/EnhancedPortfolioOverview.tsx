import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { TrendingUp, TrendingDown, DollarSign, PieChart, Activity, Eye, EyeOff } from 'lucide-react'
import { cn } from '../../lib/utils'

interface EnhancedPortfolioOverviewProps {
  totalValue: number
  dayChange: number
  dayChangePercent: number
  totalGainLoss: number
  totalGainLossPercent: number
  cashBalance: number
  investedAmount: number
  isMarketOpen?: boolean
  lastUpdated?: Date
}

export const EnhancedPortfolioOverview: React.FC<EnhancedPortfolioOverviewProps> = ({
  totalValue,
  dayChange,
  dayChangePercent,
  totalGainLoss,
  totalGainLossPercent,
  cashBalance,
  investedAmount,
  isMarketOpen = false,
  lastUpdated = new Date()
}) => {
  const [showBalance, setShowBalance] = useState(true)
  const [animateValue, setAnimateValue] = useState(false)
  const [previousValue, setPreviousValue] = useState(0)

  const formatCurrency = (value: number) => {
    if (!showBalance) return '₹••••••'
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value)
  }

  const formatPercentage = (value: number) => {
    if (!showBalance) return '•••%'
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
  }

  // Animate value changes
  useEffect(() => {
    if (totalValue && totalValue !== previousValue) {
      setAnimateValue(true)
      setPreviousValue(totalValue)
      const timer = setTimeout(() => setAnimateValue(false), 500)
      return () => clearTimeout(timer)
    }
  }, [totalValue, previousValue])

  const isPositive = dayChange >= 0
  const totalReturnIsPositive = totalGainLoss >= 0
  const goalProgress = totalValue / 1850000 // Target: ₹18,50,000

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-card p-6 md:p-8 rounded-2xl relative overflow-hidden"
    >
      {/* Background gradient animation */}
      <div className="absolute inset-0 bg-gradient-to-br from-purple-500/10 via-transparent to-cyan-500/10 opacity-50" />
      
      <div className="relative space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-white mb-1">Portfolio Value</h2>
            <div className="flex items-center space-x-2 text-sm text-slate-400">
              <Activity className="w-4 h-4" />
              <span>
                Last updated: {lastUpdated.toLocaleTimeString('en-IN', { 
                  hour: '2-digit', 
                  minute: '2-digit' 
                })}
              </span>
              <div className={cn(
                "px-2 py-1 rounded-full text-xs flex items-center space-x-1",
                isMarketOpen 
                  ? "bg-green-500/20 text-green-400" 
                  : "bg-red-500/20 text-red-400"
              )}>
                <div className={cn("w-2 h-2 rounded-full", isMarketOpen ? "bg-green-400" : "bg-red-400")} />
                <span>{isMarketOpen ? 'Market Open' : 'Market Closed'}</span>
              </div>
            </div>
          </div>

          <button
            onClick={() => setShowBalance(!showBalance)}
            className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors"
          >
            {showBalance ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>

        {/* Main Portfolio Value */}
        <div className="text-center md:text-left">
          <motion.div
            className={cn(
              "text-4xl md:text-5xl font-bold mb-2 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent",
              animateValue && "animate-pulse"
            )}
            key={totalValue}
            initial={{ scale: 1.05, opacity: 0.8 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            {formatCurrency(totalValue)}
          </motion.div>

          {/* Day Change */}
          <div className="flex items-center justify-center md:justify-start space-x-2 mb-4">
            <div className={cn(
              "flex items-center space-x-1 text-lg font-semibold",
              isPositive ? "text-green-400" : "text-red-400"
            )}>
              {isPositive ? (
                <TrendingUp className="w-5 h-5" />
              ) : (
                <TrendingDown className="w-5 h-5" />
              )}
              <span>
                {formatCurrency(Math.abs(dayChange))}
              </span>
              <span className="text-sm">
                ({formatPercentage(dayChangePercent)})
              </span>
            </div>
          </div>
        </div>

        {/* Portfolio Metrics Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Gain/Loss */}
          <div className="col-span-2 lg:col-span-1">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div className="flex items-center space-x-3">
                <div className={cn(
                  "p-2 rounded-lg",
                  totalReturnIsPositive ? "bg-green-500/20" : "bg-red-500/20"
                )}>
                  {totalReturnIsPositive ? (
                    <TrendingUp className="w-4 h-4 text-green-400" />
                  ) : (
                    <TrendingDown className="w-4 h-4 text-red-400" />
                  )}
                </div>
                <div>
                  <p className="text-sm font-medium text-white">Total Return</p>
                  <p className="text-xs text-slate-400">All time</p>
                </div>
              </div>
              <div className="text-right">
                <p className={cn(
                  "text-lg font-bold",
                  totalReturnIsPositive ? "text-green-400" : "text-red-400"
                )}>
                  {formatCurrency(totalGainLoss)}
                </p>
                <p className={cn(
                  "text-sm",
                  totalReturnIsPositive ? "text-green-400" : "text-red-400"
                )}>
                  {formatPercentage(totalGainLossPercent)}
                </p>
              </div>
            </div>
          </div>

          {/* Cash Balance */}
          <div className="col-span-1">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-blue-500/20">
                  <DollarSign className="w-4 h-4 text-blue-400" />
                </div>
                <div>
                  <p className="text-sm font-medium text-white">Cash</p>
                  <p className="text-xs text-slate-400">Available</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-lg font-bold text-blue-400">
                  {formatCurrency(cashBalance)}
                </p>
                <p className="text-sm text-slate-400">
                  {showBalance ? ((cashBalance / totalValue) * 100).toFixed(1) : '•••'}%
                </p>
              </div>
            </div>
          </div>

          {/* Invested Amount */}
          <div className="col-span-1">
            <div className="flex items-center justify-between p-4 bg-slate-800/30 rounded-xl">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-lg bg-purple-500/20">
                  <PieChart className="w-4 h-4 text-purple-400" />
                </div>
                <div>
                  <p className="text-sm font-medium text-white">Invested</p>
                  <p className="text-xs text-slate-400">In securities</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-lg font-bold text-purple-400">
                  {formatCurrency(investedAmount)}
                </p>
                <p className="text-sm text-slate-400">
                  {showBalance ? ((investedAmount / totalValue) * 100).toFixed(1) : '•••'}%
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Goal Progress */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-sm text-slate-400">Progress to Goal</div>
            <div className="text-sm text-cyan-400">
              {showBalance ? `${(goalProgress * 100).toFixed(1)}% Complete` : '•••% Complete'}
            </div>
          </div>
          
          <div className="relative">
            <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
              <motion.div
                className="h-full bg-gradient-to-r from-cyan-500 to-purple-500 rounded-full"
                initial={{ width: 0 }}
                animate={{ width: showBalance ? `${Math.min(goalProgress * 100, 100)}%` : '0%' }}
                transition={{ duration: 1, ease: "easeOut" }}
              />
            </div>
            
            {/* Progress indicators */}
            <div className="flex justify-between text-xs text-slate-500 mt-1">
              <span>₹0</span>
              <span>Target: ₹18,50,000</span>
            </div>
          </div>

          {/* Goal status */}
          {showBalance && (
            <div className="text-center">
              {goalProgress >= 0.67 ? (
                <div className="text-green-400 text-sm">
                  ✅ You're on track to meet your financial goal!
                </div>
              ) : (
                <div className="text-yellow-400 text-sm">
                  ⚠️ Consider increasing your monthly investments to stay on track
                </div>
              )}
            </div>
          )}
        </div>

        {/* Value Flash Animation Overlay */}
        <AnimatePresence>
          {animateValue && (
            <motion.div
              className={cn(
                "absolute inset-0 rounded-2xl pointer-events-none",
                isPositive ? "bg-green-400/10" : "bg-red-400/10"
              )}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.3 }}
            />
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  )
}