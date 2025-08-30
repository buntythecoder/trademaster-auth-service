import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { usePortfolioWebSocket } from '../hooks/usePortfolioWebSocket'
import { useToast } from '../contexts/ToastContext'
import { useAuthStore } from '../stores/auth.store'
import { portfolioAnalyticsService, type PortfolioAnalytics } from '../services/portfolioAnalyticsService'
import { Loader2 } from 'lucide-react'

// Import enhanced portfolio components
import { EnhancedPortfolioAnalytics } from '../components/portfolio/EnhancedPortfolioAnalytics'
import { EnhancedPortfolioOverview } from '../components/portfolio/EnhancedPortfolioOverview'
import { PerformanceChart } from '../components/portfolio/PerformanceChart'
import { AssetAllocation } from '../components/portfolio/AssetAllocation'
import { PerformanceMetrics } from '../components/portfolio/PerformanceMetrics'
import { HoldingsTable } from '../components/portfolio/HoldingsTable'
import { PortfolioGoalTracker } from '../components/portfolio/PortfolioGoalTracker'
import { TaxSummary } from '../components/portfolio/TaxSummary'

export function PortfolioAnalyticsDashboard() {
  const { portfolio, isConnected, lastUpdate } = usePortfolioWebSocket()
  const { user } = useAuthStore()
  const { info } = useToast()
  const [loading, setLoading] = useState(true)
  const [analytics, setAnalytics] = useState<PortfolioAnalytics | null>(null)
  const [selectedTimeRange, setSelectedTimeRange] = useState<'1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL'>('1Y')
  const [error, setError] = useState<string | null>(null)
  const [useEnhancedAnalytics, setUseEnhancedAnalytics] = useState(true)

  // Load enhanced analytics data
  useEffect(() => {
    const loadPortfolioAnalytics = async () => {
      if (!user?.id || !useEnhancedAnalytics) return

      try {
        setLoading(true)
        setError(null)

        const portfolioData = await portfolioAnalyticsService.getPortfolioAnalytics(
          user.id,
          selectedTimeRange
        )
        setAnalytics(portfolioData)
      } catch (err) {
        console.error('Error loading portfolio analytics:', err)
        setError(err instanceof Error ? err.message : 'Failed to load portfolio data')
        // Fallback to original analytics if enhanced fails
        setUseEnhancedAnalytics(false)
      } finally {
        setLoading(false)
      }
    }

    loadPortfolioAnalytics()
  }, [user?.id, selectedTimeRange, useEnhancedAnalytics])

  useEffect(() => {
    if (portfolio && !useEnhancedAnalytics) {
      setLoading(false)
    }
  }, [portfolio, useEnhancedAnalytics])

  useEffect(() => {
    if (isConnected && !loading) {
      info('Portfolio Connected', 'Real-time portfolio updates are now active')
    }
  }, [isConnected, loading, info])

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="w-12 h-12 animate-spin text-purple-400" />
          <p className="text-slate-400">Loading portfolio analytics...</p>
        </div>
      </div>
    )
  }

  // If enhanced analytics are available, show the new comprehensive dashboard
  if (useEnhancedAnalytics && analytics) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
        {/* Header Section */}
        <div className="glass-card-dark border-b border-slate-700/50 backdrop-blur-xl">
          <div className="container mx-auto px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-bold gradient-text">Portfolio Analytics</h1>
                <p className="text-slate-400 text-sm">Comprehensive portfolio insights and performance tracking</p>
              </div>
              
              {/* Time Range Selector */}
              <div className="flex items-center space-x-1 bg-slate-800/50 rounded-xl p-1">
                {(['1M', '3M', '6M', '1Y', '3Y', 'ALL'] as const).map((range) => (
                  <button
                    key={range}
                    onClick={() => setSelectedTimeRange(range)}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                      selectedTimeRange === range
                        ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/20'
                        : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
                    }`}
                  >
                    {range}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="container mx-auto px-6 py-8 space-y-8">
          {/* Top Row: Enhanced Overview & Performance Chart */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-1">
              <EnhancedPortfolioOverview
                totalValue={analytics.performance.totalValue}
                dayChange={analytics.performance.dayChange}
                dayChangePercent={analytics.performance.dayChangePercent}
                totalGainLoss={analytics.performance.totalGainLoss}
                totalGainLossPercent={analytics.performance.totalGainLossPercent}
                cashBalance={analytics.performance.cashBalance}
                investedAmount={analytics.performance.investedAmount}
                isMarketOpen={analytics.performance.isMarketOpen}
                lastUpdated={new Date()}
              />
            </div>
            <div className="lg:col-span-2">
              <PerformanceChart
                historicalData={analytics.performance.historicalData}
                timeRange={selectedTimeRange}
                benchmarkData={analytics.performance.benchmarkComparison}
              />
            </div>
          </div>

          {/* Second Row: Asset Allocation & Performance Metrics */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <AssetAllocation
              assetAllocation={analytics.assetAllocation}
              sectorAllocation={analytics.sectorAllocation}
            />
            <PerformanceMetrics
              riskMetrics={analytics.riskMetrics}
              performance={analytics.performance}
              diversificationScore={analytics.diversificationMetrics.overallScore}
            />
          </div>

          {/* Third Row: Holdings Table */}
          <div className="space-y-6">
            <HoldingsTable
              holdings={analytics.holdings}
              totalValue={analytics.performance.totalValue}
              onSymbolClick={(symbol) => {
                console.log('Navigate to symbol:', symbol)
                // Navigate to market data or detailed stock view
              }}
            />
          </div>

          {/* Fourth Row: Goals & Tax Summary */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <PortfolioGoalTracker
              goals={[
                {
                  id: '1',
                  name: 'Retirement Fund',
                  targetAmount: 1000000,
                  currentAmount: analytics.performance.totalValue * 0.6,
                  targetDate: new Date('2040-12-31'),
                  priority: 'high' as const
                },
                {
                  id: '2',
                  name: 'Emergency Fund',
                  targetAmount: 50000,
                  currentAmount: analytics.performance.cashBalance,
                  targetDate: new Date('2025-12-31'),
                  priority: 'high' as const
                },
                {
                  id: '3',
                  name: 'Vacation Fund',
                  targetAmount: 25000,
                  currentAmount: analytics.performance.totalValue * 0.05,
                  targetDate: new Date('2026-06-01'),
                  priority: 'medium' as const
                }
              ]}
            />
            <TaxSummary
              taxData={{
                currentYearGains: analytics.performance.totalGainLoss > 0 ? analytics.performance.totalGainLoss * 0.7 : 0,
                currentYearLosses: analytics.performance.totalGainLoss < 0 ? Math.abs(analytics.performance.totalGainLoss) * 0.3 : 0,
                unrealizedGains: analytics.performance.totalGainLoss > 0 ? analytics.performance.totalGainLoss * 0.8 : 0,
                unrealizedLosses: analytics.performance.totalGainLoss < 0 ? Math.abs(analytics.performance.totalGainLoss) * 0.2 : 0,
                dividendIncome: analytics.performance.totalValue * 0.025,
                estimatedTaxLiability: Math.max(0, analytics.performance.totalGainLoss * 0.15),
                taxLossHarvestingOpportunity: analytics.performance.totalGainLoss < 0 ? Math.abs(analytics.performance.totalGainLoss) * 0.1 : 0
              }}
            />
          </div>
        </div>

        {/* Connection Status */}
        {!isConnected && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed bottom-4 right-4 bg-red-500/20 border border-red-500/30 rounded-xl p-4 backdrop-blur-sm"
          >
            <div className="flex items-center space-x-2 text-red-400">
              <div className="w-2 h-2 rounded-full bg-red-400 animate-pulse"></div>
              <span className="text-sm">Portfolio service unavailable</span>
            </div>
          </motion.div>
        )}
      </div>
    )
  }

  // Fallback to original analytics if enhanced analytics fail
  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Enhanced Portfolio Analytics */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <EnhancedPortfolioAnalytics />
        </motion.div>

        {/* Connection Status */}
        {!isConnected && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed bottom-4 right-4 bg-red-500/20 border border-red-500/30 rounded-xl p-4 backdrop-blur-sm"
          >
            <div className="flex items-center space-x-2 text-red-400">
              <div className="w-2 h-2 rounded-full bg-red-400 animate-pulse"></div>
              <span className="text-sm">Portfolio service unavailable</span>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  )
}

export default PortfolioAnalyticsDashboard