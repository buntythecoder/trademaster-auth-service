import React, { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  PieChart, 
  Upload,
  CheckCircle,
  Clock,
  AlertTriangle,
  Activity,
  Target,
  BarChart3,
  Brain,
  Settings,
  Maximize2
} from 'lucide-react'
import { usePortfolio, useMarketData, useOrders } from '@/hooks/useWebSocket'
import { useConsolidatedPortfolio } from '@/hooks/useConsolidatedPortfolio'
import { useSmartInsights } from '@/hooks/useSmartInsights'
import { ConnectionStatus } from '@/components/realtime/ConnectionStatus'
import { MultiBrokerPortfolioWidget } from '@/components/widgets/MultiBrokerPortfolioWidget'
import { InteractivePerformanceChart } from '@/components/charts/InteractivePerformanceChart'
import { SmartInsightsPanel } from '@/components/insights/SmartInsightsPanel'
import { EnhancedPortfolioAnalytics } from '@/components/portfolio/EnhancedPortfolioAnalytics'

export function TraderDashboard() {
  const { portfolio, totalValue, dayPnL, dayPnLPercent, positions } = usePortfolio()
  const { orders, pendingOrders, filledOrders } = useOrders()
  const { marketData } = useMarketData(['RELIANCE', 'TCS', 'INFY', 'HDFC', 'ICICIBANK'])
  const { portfolioData: consolidatedPortfolioData } = useConsolidatedPortfolio()
  const { insights } = useSmartInsights()
  const [selectedTimeRange, setSelectedTimeRange] = useState('1D')
  const [showBrokerBreakdown, setShowBrokerBreakdown] = useState(false)
  
  // Use real data if available, otherwise mock data
  const consolidatedPortfolio = useMemo(() => consolidatedPortfolioData || ({
    totalValue: totalValue || 8452300,
    dayChange: dayPnL || 23450,
    dayChangePercent: dayPnLPercent || 0.28,
    brokerBreakdown: [
      {
        brokerId: 'zerodha',
        brokerName: 'Zerodha',
        value: (totalValue || 8452300) * 0.5,
        dayChange: (dayPnL || 23450) * 0.66,
        dayChangePercent: 1.96,
        connectionStatus: 'connected' as const,
        positions: Math.floor((positions?.length || 12) * 0.5),
        lastUpdated: new Date()
      },
      {
        brokerId: 'groww',
        brokerName: 'Groww',
        value: (totalValue || 8452300) * 0.33,
        dayChange: (dayPnL || 23450) * 0.28,
        dayChangePercent: 1.27,
        connectionStatus: 'connected' as const,
        positions: Math.floor((positions?.length || 12) * 0.33),
        lastUpdated: new Date()
      },
      {
        brokerId: 'angel',
        brokerName: 'Angel One',
        value: (totalValue || 8452300) * 0.17,
        dayChange: (dayPnL || 23450) * 0.06,
        dayChangePercent: 0.54,
        connectionStatus: 'connecting' as const,
        positions: Math.floor((positions?.length || 12) * 0.17),
        lastUpdated: new Date()
      }
    ],
    lastUpdated: new Date(),
    connectionCount: 3
  }), [totalValue, dayPnL, dayPnLPercent, positions])

  // Mock portfolio history for performance chart
  const portfolioHistory = useMemo(() => [], []) // Will be populated by InteractivePerformanceChart mock data
  
  // Use real insights data if available, otherwise fallback to mock data
  const smartInsights = useMemo(() => insights || {
    tradingOpportunities: [
      {
        id: 'opp-1',
        symbol: 'RELIANCE',
        action: 'BUY' as const,
        confidence: 87,
        targetPrice: 2650,
        currentPrice: 2547,
        reasoning: 'Strong Q3 results expected, oil prices stabilizing',
        timeframe: '2-3 weeks',
        riskLevel: 'Medium' as const
      },
      {
        id: 'opp-2',
        symbol: 'TCS',
        action: 'HOLD' as const,
        confidence: 73,
        targetPrice: 3800,
        currentPrice: 3642,
        reasoning: 'Consistent performance, but valuations stretched',
        timeframe: '1-2 months',
        riskLevel: 'Low' as const
      }
    ],
    portfolioHealth: {
      diversification: 78,
      riskScore: 65,
      performanceScore: 82,
      liquidityScore: 91
    },
    riskAlerts: [
      {
        id: 'risk-1',
        type: 'concentration',
        severity: 'medium' as const,
        message: 'High concentration in IT sector (35% of portfolio)',
        recommendation: 'Consider diversifying into banking or pharma sectors'
      }
    ],
    marketConditions: {
      trend: 'Bullish' as const,
      volatility: 'Medium',
      sentiment: 'Positive',
      keyEvents: ['RBI Policy Meet', 'Q3 Results Season']
    }
  }, [insights])

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

  return (
    <motion.div 
      className="min-h-screen space-y-8" 
      data-tour="dashboard-main"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.6, staggerChildren: 0.1 }}
    >
      {/* Enhanced Header with Connection Status */}
      <motion.div 
        className="text-center mb-12"
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6 }}
      >
        <div className="flex items-center justify-center gap-4 mb-4">
          <h1 className="text-4xl font-bold gradient-text">Enhanced Trading Dashboard</h1>
          <ConnectionStatus size="sm" />
        </div>
        <p className="text-slate-400 text-lg">
          Unified multi-broker portfolio monitoring with AI-powered insights
        </p>
      </motion.div>

      {/* Enhanced Multi-Broker Portfolio Widget */}
      <motion.div
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, delay: 0.1 }}
      >
        <MultiBrokerPortfolioWidget
          portfolioData={consolidatedPortfolio}
          brokerConnections={consolidatedPortfolio.brokerBreakdown}
          updateInterval={5000}
          compactMode={false}
        />
      </motion.div>
      
      {/* Enhanced Dashboard Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content Area */}
        <div className="lg:col-span-2 space-y-6">
          {/* Interactive Performance Chart */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.2 }}
          >
            <InteractivePerformanceChart
              portfolioHistory={portfolioHistory}
              timeRange={selectedTimeRange}
              brokerComparison={showBrokerBreakdown}
              height={400}
            />
          </motion.div>
          
          {/* KYC Status Banner - Moved and Condensed */}
          <motion.div 
            className="glass-card p-6 rounded-2xl border border-orange-500/50 bg-gradient-to-r from-orange-500/10 to-amber-500/10"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.3 }}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="p-3 rounded-xl bg-orange-500/20">
                  <AlertTriangle className="h-6 w-6 text-orange-400" />
                </div>
                <div>
                  <h3 className="font-bold text-orange-400 text-lg">
                    Complete Your KYC Verification
                  </h3>
                  <p className="text-sm text-slate-400">
                    Upload required documents to unlock full trading capabilities
                  </p>
                </div>
              </div>
              <div className="flex items-center space-x-6">
                <div className="text-center">
                  <div className="text-3xl font-bold text-orange-400 mb-1">75%</div>
                  <div className="text-xs text-slate-400">Complete</div>
                </div>
                <Link 
                  to="/profile" 
                  className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2"
                >
                  <Upload className="w-4 h-4" />
                  <span>Upload Documents</span>
                </Link>
              </div>
            </div>
          </motion.div>

          {/* Portfolio Analytics Preview */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.35 }}
          >
            <div className="glass-card p-6 rounded-2xl">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-white flex items-center">
                  <PieChart className="w-5 h-5 mr-2 text-purple-400" />
                  Portfolio Analytics Preview
                </h3>
                <Link 
                  to="/analytics" 
                  className="text-sm text-purple-400 hover:text-purple-300 transition-colors flex items-center space-x-1"
                >
                  <span>View Full Analytics</span>
                  <Maximize2 className="w-3 h-3" />
                </Link>
              </div>
              <EnhancedPortfolioAnalytics compact={true} />
            </div>
          </motion.div>
        </div>
        
        {/* Right Sidebar */}
        <div className="space-y-6">
          {/* Smart Insights Panel */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
          >
            <SmartInsightsPanel
              insights={smartInsights.tradingOpportunities}
              portfolioHealth={smartInsights.portfolioHealth}
              riskAlerts={smartInsights.riskAlerts}
              marketConditions={smartInsights.marketConditions}
            />
          </motion.div>

          {/* Compact Quick Stats */}
          <motion.div 
            className="grid grid-cols-2 md:grid-cols-4 gap-4"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
          >
            <div className="glass-card p-4 rounded-xl text-center hover:scale-105 transition-all duration-300">
              <div className="text-lg font-bold text-white">
                {positions.length || '12'}
              </div>
              <div className="text-xs text-slate-400">Active Positions</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center hover:scale-105 transition-all duration-300">
              <div className="text-lg font-bold text-green-400">
                {consolidatedPortfolio.brokerBreakdown.filter(b => b.connectionStatus === 'connected').length}
              </div>
              <div className="text-xs text-slate-400">Connected Brokers</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center hover:scale-105 transition-all duration-300">
              <div className="text-lg font-bold text-cyan-400">
                ₹2.1L
              </div>
              <div className="text-xs text-slate-400">Available Cash</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center hover:scale-105 transition-all duration-300">
              <div className="text-lg font-bold text-purple-400">
                {consolidatedPortfolio.lastUpdated.toLocaleTimeString('en-IN', { 
                  hour: '2-digit', 
                  minute: '2-digit' 
                })}
              </div>
              <div className="text-xs text-slate-400">Last Updated</div>
            </div>
          </motion.div>
        </div>
      </div>
      
      {/* Legacy Dashboard Sections - Now at Bottom */}
      <motion.div
        className="grid gap-6 md:grid-cols-2"
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, delay: 0.5 }}
      >
        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <CheckCircle className="w-5 h-5 mr-2 text-green-400" />
            KYC Verification Progress
          </h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Personal Information</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-green-400 h-2 rounded-full w-full"></div>
                </div>
                <CheckCircle className="h-4 w-4 text-green-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Identity Documents</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-green-400 h-2 rounded-full w-full"></div>
                </div>
                <CheckCircle className="h-4 w-4 text-green-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Address Verification</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-yellow-400 h-2 rounded-full w-3/4"></div>
                </div>
                <Clock className="h-4 w-4 text-yellow-400" />
              </div>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <span className="text-sm font-medium text-white">Bank Verification</span>
              <div className="flex items-center space-x-2">
                <div className="w-24 bg-slate-700 rounded-full h-2">
                  <div className="bg-red-400 h-2 rounded-full w-1/4"></div>
                </div>
                <AlertTriangle className="h-4 w-4 text-red-400" />
              </div>
            </div>
          </div>
          
          <button className="cyber-button w-full py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 mt-6">
            <Upload className="w-4 h-4" />
            <span>Complete KYC Process</span>
          </button>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Activity className="w-5 h-5 mr-2 text-cyan-400" />
            Recent Trades
          </h3>
          <div className="space-y-4">
            {[
              { stock: 'RELIANCE', action: 'BUY', quantity: '10', price: '₹2,547', pl: '+₹234', time: '10:30 AM' },
              { stock: 'TCS', action: 'SELL', quantity: '5', price: '₹3,642', pl: '-₹89', time: '11:15 AM' },
              { stock: 'HDFC BANK', action: 'BUY', quantity: '15', price: '₹1,567', pl: '+₹156', time: '2:45 PM' },
              { stock: 'INFY', action: 'SELL', quantity: '8', price: '₹1,423', pl: '+₹92', time: '3:20 PM' },
            ].map((trade, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-colors">
                <div className="flex items-center space-x-3">
                  <div className={`w-2 h-2 rounded-full ${
                    trade.action === 'BUY' ? 'bg-green-400' : 'bg-red-400'
                  }`} />
                  <div>
                    <p className="text-sm font-medium text-white">{trade.stock}</p>
                    <p className="text-xs text-slate-400">
                      {trade.action} {trade.quantity} @ {trade.price}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-xs text-slate-400">{trade.time}</p>
                  <p className={`text-sm font-medium ${
                    trade.pl.startsWith('+') ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {trade.pl}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Enhanced Action Buttons */}
      <motion.div 
        className="flex gap-4 justify-center"
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, delay: 0.6 }}
      >
        <button className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2 hover:scale-105 transition-all duration-300">
          <TrendingUp className="w-4 h-4" />
          <span>Start Trading</span>
        </button>
        <Link 
          to="/analytics" 
          className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70 flex items-center space-x-2 hover:scale-105"
        >
          <BarChart3 className="w-4 h-4" />
          <span>View Full Analytics</span>
        </Link>
        <button className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-cyan-300 transition-colors border border-cyan-500/50 hover:border-cyan-400/70 flex items-center space-x-2 hover:scale-105">
          <Brain className="w-4 h-4" />
          <span>AI Insights</span>
        </button>
        <button className="glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-orange-300 transition-colors border border-orange-500/50 hover:border-orange-400/70 flex items-center space-x-2 hover:scale-105">
          <Upload className="w-4 h-4" />
          <span>Upload Documents</span>
        </button>
      </motion.div>
    </motion.div>
  )
}