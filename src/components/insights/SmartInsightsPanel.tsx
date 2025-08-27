import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Brain, 
  TrendingUp, 
  TrendingDown, 
  AlertTriangle, 
  CheckCircle, 
  Target, 
  Zap,
  Activity,
  DollarSign,
  Shield,
  ArrowRight,
  RefreshCw,
  Lightbulb
} from 'lucide-react'

interface TradingInsight {
  id: string
  type: 'opportunity' | 'risk' | 'recommendation' | 'market-condition'
  title: string
  description: string
  confidenceScore: number
  priority: 'high' | 'medium' | 'low'
  action?: {
    label: string
    type: 'buy' | 'sell' | 'hold' | 'configure'
    data?: any
  }
  metrics?: {
    expectedReturn?: number
    riskLevel?: number
    timeHorizon?: string
  }
  timestamp: Date
}

interface PortfolioHealth {
  overallScore: number
  diversification: number
  riskExposure: number
  performanceConsistency: number
  cashAllocation: number
}

interface RiskAlert {
  id: string
  type: 'concentration' | 'volatility' | 'drawdown' | 'correlation'
  severity: 'critical' | 'warning' | 'info'
  message: string
  recommendation: string
  affectedPositions?: string[]
}

interface SmartInsightsPanelProps {
  insights?: TradingInsight[]
  portfolioHealth?: PortfolioHealth
  riskAlerts?: RiskAlert[]
  compactMode?: boolean
}

const mockInsights: TradingInsight[] = [
  {
    id: '1',
    type: 'opportunity',
    title: 'Tech Sector Opportunity',
    description: 'IT stocks showing strong momentum with low volatility. Consider increasing allocation.',
    confidenceScore: 87,
    priority: 'high',
    action: {
      label: 'View Stocks',
      type: 'buy'
    },
    metrics: {
      expectedReturn: 12.5,
      riskLevel: 3,
      timeHorizon: '3-6 months'
    },
    timestamp: new Date()
  },
  {
    id: '2',
    type: 'risk',
    title: 'Portfolio Concentration Risk',
    description: 'Over 60% allocation in financial sector. Consider diversification.',
    confidenceScore: 92,
    priority: 'high',
    action: {
      label: 'Rebalance',
      type: 'configure'
    },
    metrics: {
      riskLevel: 4,
      timeHorizon: 'immediate'
    },
    timestamp: new Date()
  },
  {
    id: '3',
    type: 'market-condition',
    title: 'Market Volatility Alert',
    description: 'VIX elevated at 18.5. Defensive positioning recommended.',
    confidenceScore: 76,
    priority: 'medium',
    metrics: {
      riskLevel: 4
    },
    timestamp: new Date()
  }
]

const mockPortfolioHealth: PortfolioHealth = {
  overallScore: 78,
  diversification: 65,
  riskExposure: 72,
  performanceConsistency: 84,
  cashAllocation: 85
}

const mockRiskAlerts: RiskAlert[] = [
  {
    id: '1',
    type: 'concentration',
    severity: 'warning',
    message: 'High concentration in Banking sector (45%)',
    recommendation: 'Consider reducing HDFC Bank position by 15%',
    affectedPositions: ['HDFCBANK', 'ICICIBANK', 'KOTAKBANK']
  },
  {
    id: '2',
    type: 'volatility',
    severity: 'info',
    message: 'Portfolio volatility within normal range (12.3%)',
    recommendation: 'Current risk level acceptable for your profile'
  }
]

const HealthMeter: React.FC<{ 
  label: string 
  value: number 
  color?: string
  icon?: React.ReactNode
}> = ({ label, value, color = 'purple', icon }) => {
  const circumference = 2 * Math.PI * 35
  const strokeDashoffset = circumference - (value / 100) * circumference

  return (
    <div className="flex items-center space-x-4">
      <div className="relative w-20 h-20">
        <svg className="w-20 h-20 transform -rotate-90" viewBox="0 0 80 80">
          <circle
            cx="40"
            cy="40"
            r="35"
            stroke="rgb(51, 65, 85)"
            strokeWidth="6"
            fill="transparent"
          />
          <circle
            cx="40"
            cy="40"
            r="35"
            stroke={color === 'purple' ? '#a855f7' : color === 'green' ? '#10b981' : color === 'red' ? '#ef4444' : '#06b6d4'}
            strokeWidth="6"
            fill="transparent"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            className="transition-all duration-1000 ease-out"
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-sm font-bold text-white">{value}%</span>
        </div>
      </div>
      <div className="flex-1">
        <div className="flex items-center space-x-2 mb-1">
          {icon}
          <h4 className="font-semibold text-white">{label}</h4>
        </div>
        <div className="w-full bg-slate-700 rounded-full h-2">
          <div 
            className={`h-2 rounded-full transition-all duration-1000 ${
              value >= 80 ? 'bg-green-400' : value >= 60 ? 'bg-yellow-400' : 'bg-red-400'
            }`}
            style={{ width: `${value}%` }}
          />
        </div>
      </div>
    </div>
  )
}

const InsightCard: React.FC<{ insight: TradingInsight; index: number }> = ({ insight, index }) => {
  const getInsightIcon = () => {
    switch (insight.type) {
      case 'opportunity':
        return <TrendingUp className="w-5 h-5 text-green-400" />
      case 'risk':
        return <AlertTriangle className="w-5 h-5 text-red-400" />
      case 'recommendation':
        return <Lightbulb className="w-5 h-5 text-yellow-400" />
      case 'market-condition':
        return <Activity className="w-5 h-5 text-cyan-400" />
      default:
        return <Brain className="w-5 h-5 text-purple-400" />
    }
  }

  const getPriorityColor = () => {
    switch (insight.priority) {
      case 'high':
        return 'border-red-500/50 bg-red-500/10'
      case 'medium':
        return 'border-yellow-500/50 bg-yellow-500/10'
      case 'low':
        return 'border-green-500/50 bg-green-500/10'
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.1 }}
      className={`glass-card p-4 rounded-xl border ${getPriorityColor()} hover:scale-[1.02] transition-all duration-300`}
    >
      <div className="flex items-start space-x-3">
        <div className="p-2 rounded-lg bg-slate-800/50">
          {getInsightIcon()}
        </div>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-2">
            <h4 className="font-semibold text-white text-sm truncate">{insight.title}</h4>
            <div className="flex items-center space-x-2 ml-2">
              <div className={`text-xs px-2 py-1 rounded-full font-medium ${
                insight.confidenceScore >= 80 
                  ? 'bg-green-500/20 text-green-400'
                  : insight.confidenceScore >= 60
                  ? 'bg-yellow-500/20 text-yellow-400'
                  : 'bg-red-500/20 text-red-400'
              }`}>
                {insight.confidenceScore}%
              </div>
            </div>
          </div>
          
          <p className="text-xs text-slate-400 mb-3 leading-relaxed">
            {insight.description}
          </p>
          
          {insight.metrics && (
            <div className="grid grid-cols-2 gap-2 mb-3">
              {insight.metrics.expectedReturn && (
                <div className="text-xs">
                  <span className="text-slate-400">Return:</span>
                  <span className="text-green-400 ml-1 font-medium">
                    +{insight.metrics.expectedReturn}%
                  </span>
                </div>
              )}
              {insight.metrics.riskLevel && (
                <div className="text-xs">
                  <span className="text-slate-400">Risk:</span>
                  <span className={`ml-1 font-medium ${
                    insight.metrics.riskLevel <= 2 ? 'text-green-400' :
                    insight.metrics.riskLevel <= 3 ? 'text-yellow-400' : 'text-red-400'
                  }`}>
                    {insight.metrics.riskLevel}/5
                  </span>
                </div>
              )}
            </div>
          )}
          
          {insight.action && (
            <button className={`cyber-button-sm px-3 py-1 rounded-lg text-xs font-medium flex items-center space-x-1 ${
              insight.action.type === 'buy' ? 'hover:bg-green-500/20' :
              insight.action.type === 'sell' ? 'hover:bg-red-500/20' :
              'hover:bg-purple-500/20'
            }`}>
              <span>{insight.action.label}</span>
              <ArrowRight className="w-3 h-3" />
            </button>
          )}
        </div>
      </div>
    </motion.div>
  )
}

export const SmartInsightsPanel: React.FC<SmartInsightsPanelProps> = ({
  insights = mockInsights,
  portfolioHealth = mockPortfolioHealth,
  riskAlerts = mockRiskAlerts,
  compactMode = false
}) => {
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [selectedTab, setSelectedTab] = useState<'insights' | 'health' | 'risks'>('insights')

  const handleRefresh = async () => {
    setIsRefreshing(true)
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
    setIsRefreshing(false)
  }

  const criticalAlerts = riskAlerts.filter(alert => alert.severity === 'critical')
  const warningAlerts = riskAlerts.filter(alert => alert.severity === 'warning')

  return (
    <motion.div 
      className="glass-card rounded-2xl p-6 h-full"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6, delay: 0.2 }}
    >
      {/* Panel Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-2">
          <Brain className="w-6 h-6 text-purple-400" />
          <h3 className="text-xl font-bold text-white">AI Insights</h3>
        </div>
        
        <button 
          onClick={handleRefresh}
          disabled={isRefreshing}
          className={`cyber-button-sm p-2 rounded-lg transition-all duration-300 ${
            isRefreshing ? 'animate-spin' : 'hover:scale-110'
          }`}
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Tab Navigation */}
      <div className="flex space-x-1 mb-6 bg-slate-800/50 rounded-xl p-1">
        {[
          { id: 'insights', label: 'Insights', icon: <Lightbulb className="w-4 h-4" /> },
          { id: 'health', label: 'Health', icon: <Shield className="w-4 h-4" /> },
          { id: 'risks', label: 'Risks', icon: <AlertTriangle className="w-4 h-4" /> }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setSelectedTab(tab.id as any)}
            className={`flex-1 flex items-center justify-center space-x-2 py-2 px-3 rounded-lg text-sm font-medium transition-all duration-300 ${
              selectedTab === tab.id
                ? 'bg-purple-500 text-white shadow-lg'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
          >
            {tab.icon}
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Content Sections */}
      <AnimatePresence mode="wait">
        {selectedTab === 'insights' && (
          <motion.div
            key="insights"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-4"
          >
            {insights.slice(0, compactMode ? 2 : 4).map((insight, index) => (
              <InsightCard key={insight.id} insight={insight} index={index} />
            ))}
            
            {insights.length > (compactMode ? 2 : 4) && (
              <button className="w-full text-center py-3 text-sm text-slate-400 hover:text-white transition-colors">
                View {insights.length - (compactMode ? 2 : 4)} more insights
              </button>
            )}
          </motion.div>
        )}

        {selectedTab === 'health' && (
          <motion.div
            key="health"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-6"
          >
            {/* Overall Health Score */}
            <div className="text-center mb-6">
              <div className="text-3xl font-bold text-white mb-2">
                {portfolioHealth.overallScore}/100
              </div>
              <div className={`text-sm font-medium ${
                portfolioHealth.overallScore >= 80 ? 'text-green-400' :
                portfolioHealth.overallScore >= 60 ? 'text-yellow-400' : 'text-red-400'
              }`}>
                {portfolioHealth.overallScore >= 80 ? 'Excellent' :
                 portfolioHealth.overallScore >= 60 ? 'Good' : 'Needs Improvement'}
              </div>
            </div>

            {/* Health Metrics */}
            <div className="space-y-4">
              <HealthMeter 
                label="Diversification" 
                value={portfolioHealth.diversification}
                icon={<Target className="w-4 h-4 text-purple-400" />}
              />
              <HealthMeter 
                label="Risk Management" 
                value={portfolioHealth.riskExposure}
                color="green"
                icon={<Shield className="w-4 h-4 text-green-400" />}
              />
              <HealthMeter 
                label="Performance" 
                value={portfolioHealth.performanceConsistency}
                color="cyan"
                icon={<TrendingUp className="w-4 h-4 text-cyan-400" />}
              />
            </div>
          </motion.div>
        )}

        {selectedTab === 'risks' && (
          <motion.div
            key="risks"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="space-y-4"
          >
            {/* Critical Alerts */}
            {criticalAlerts.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-sm font-semibold text-red-400 uppercase tracking-wider">
                  Critical Alerts
                </h4>
                {criticalAlerts.map((alert) => (
                  <div key={alert.id} className="glass-card p-4 rounded-xl border border-red-500/50 bg-red-500/10">
                    <div className="flex items-start space-x-3">
                      <AlertTriangle className="w-5 h-5 text-red-400 mt-0.5" />
                      <div className="flex-1">
                        <p className="text-sm text-white font-medium mb-1">{alert.message}</p>
                        <p className="text-xs text-slate-400">{alert.recommendation}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Warning Alerts */}
            {warningAlerts.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-sm font-semibold text-yellow-400 uppercase tracking-wider">
                  Warnings
                </h4>
                {warningAlerts.map((alert) => (
                  <div key={alert.id} className="glass-card p-4 rounded-xl border border-yellow-500/50 bg-yellow-500/10">
                    <div className="flex items-start space-x-3">
                      <AlertTriangle className="w-5 h-5 text-yellow-400 mt-0.5" />
                      <div className="flex-1">
                        <p className="text-sm text-white font-medium mb-1">{alert.message}</p>
                        <p className="text-xs text-slate-400">{alert.recommendation}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {riskAlerts.length === 0 && (
              <div className="text-center py-8">
                <CheckCircle className="w-12 h-12 text-green-400 mx-auto mb-4" />
                <p className="text-sm text-slate-400">No active risk alerts</p>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}