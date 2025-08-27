import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { TrendingUp, TrendingDown, Activity, Eye, EyeOff } from 'lucide-react'
import { PortfolioData } from '../../hooks/usePortfolioWebSocket'
import { cn } from '../../lib/utils'

interface PortfolioOverviewProps {
  portfolio: PortfolioData | null
}

export function PortfolioOverview({ portfolio }: PortfolioOverviewProps) {
  const [showBalance, setShowBalance] = useState(true)
  const [animateValue, setAnimateValue] = useState(false)
  const [previousValue, setPreviousValue] = useState(0)

  const formatCurrency = (amount: number) => {
    if (!showBalance) return '₹••••••'
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const formatNumber = (num: number, decimals: number = 2) => {
    if (!showBalance && decimals > 0) return '•••'
    return num.toLocaleString('en-IN', { 
      minimumFractionDigits: decimals, 
      maximumFractionDigits: decimals 
    })
  }

  // Animate value changes
  useEffect(() => {
    if (portfolio?.summary.currentValue && portfolio.summary.currentValue !== previousValue) {
      setAnimateValue(true)
      setPreviousValue(portfolio.summary.currentValue)
      const timer = setTimeout(() => setAnimateValue(false), 500)
      return () => clearTimeout(timer)
    }
  }, [portfolio?.summary.currentValue, previousValue])

  if (!portfolio) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-slate-700 rounded w-3/4"></div>
          <div className="h-12 bg-slate-700 rounded w-1/2"></div>
          <div className="grid grid-cols-3 gap-4">
            <div className="h-16 bg-slate-700 rounded"></div>
            <div className="h-16 bg-slate-700 rounded"></div>
            <div className="h-16 bg-slate-700 rounded"></div>
          </div>
        </div>
      </div>
    )
  }

  const { summary } = portfolio
  const isPositive = summary.dayChange >= 0
  const totalReturnIsPositive = summary.unrealizedPnL >= 0
  const goalProgress = summary.currentValue / 1850000 // Target from spec: ₹18,50,000

  return (
    <div className="glass-card rounded-2xl p-6 md:p-8 relative overflow-hidden">
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
                Last updated: {summary.lastUpdated.toLocaleTimeString('en-IN', { 
                  hour: '2-digit', 
                  minute: '2-digit' 
                })}
              </span>
              <div className={cn(
                "px-2 py-1 rounded-full text-xs",
                summary.isMarketOpen 
                  ? "bg-green-500/20 text-green-400" 
                  : "bg-red-500/20 text-red-400"
              )}>
                🔴 {summary.isMarketOpen ? 'Market Open' : 'Market Closed'}
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
            key={summary.currentValue}
            initial={{ scale: 1.05, opacity: 0.8 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            {formatCurrency(summary.currentValue)}
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
                {isPositive ? '+' : ''}{formatCurrency(summary.dayChange)}
              </span>
              <span className="text-sm">
                ({isPositive ? '+' : ''}{formatNumber(summary.dayChangePercent)}%)
              </span>
            </div>
          </div>
        </div>

        {/* Portfolio Statistics Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Total Invested */}
          <div className="text-center md:text-left">
            <div className="text-sm text-slate-400 mb-1">Total Invested</div>
            <div className="text-xl font-semibold text-white">
              {formatCurrency(summary.totalInvested)}
            </div>
          </div>

          {/* Total Returns */}
          <div className="text-center md:text-left">
            <div className="text-sm text-slate-400 mb-1">Total Returns</div>
            <div className={cn(
              "text-xl font-semibold",
              totalReturnIsPositive ? "text-green-400" : "text-red-400"
            )}>
              {totalReturnIsPositive ? '+' : ''}{formatCurrency(summary.unrealizedPnL)}
            </div>
            <div className={cn(
              "text-sm",
              totalReturnIsPositive ? "text-green-400" : "text-red-400"
            )}>
              {totalReturnIsPositive ? '+' : ''}{formatNumber(summary.portfolioReturn)}%
            </div>
          </div>

          {/* Annualized Return */}
          <div className="text-center md:text-left">
            <div className="text-sm text-slate-400 mb-1">Annualized Return</div>
            <div className="text-xl font-semibold text-cyan-400">
              +{formatNumber(summary.annualizedReturn)}%
            </div>
          </div>
        </div>

        {/* Goal Progress */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-sm text-slate-400">Progress to Goal</div>
            <div className="text-sm text-cyan-400">{formatNumber(goalProgress * 100)}% Complete</div>
          </div>
          
          <div className="relative">
            <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
              <motion.div
                className="h-full bg-gradient-to-r from-cyan-500 to-purple-500 rounded-full"
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(goalProgress * 100, 100)}%` }}
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
    </div>
  )
}