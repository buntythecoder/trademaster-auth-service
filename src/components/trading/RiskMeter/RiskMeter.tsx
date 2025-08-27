import React, { useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Shield, 
  AlertTriangle, 
  TrendingUp, 
  TrendingDown,
  Target,
  PieChart,
  BarChart3,
  Info
} from 'lucide-react'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

interface SectorExposure {
  sector: string
  exposure: number
  limit: number
}

interface RiskWarning {
  level: RiskLevel
  message: string
  action: 'BLOCK' | 'WARN' | 'MONITOR'
  suggestions: string[]
}

interface RiskMeterProps {
  currentRisk: RiskLevel
  portfolioExposure: number
  sectorConcentration: SectorExposure[]
  dayTradingLimit: number
  marginUtilization: number
  maxDrawdown: number
  totalPositions: number
  availableBalance: number
  usedMargin: number
  className?: string
}

const RiskGauge: React.FC<{
  level: RiskLevel
  size?: 'sm' | 'md' | 'lg'
}> = ({ level, size = 'md' }) => {
  const riskLevels = {
    'LOW': { value: 1, color: '#22C55E', bgColor: 'bg-green-500' },
    'MEDIUM': { value: 3, color: '#F59E0B', bgColor: 'bg-yellow-500' },
    'HIGH': { value: 5, color: '#EF4444', bgColor: 'bg-orange-500' },
    'CRITICAL': { value: 7, color: '#DC2626', bgColor: 'bg-red-600' }
  }

  const currentLevel = riskLevels[level]
  const totalDots = 7

  const sizeClasses = {
    sm: 'w-2 h-2',
    md: 'w-3 h-3',
    lg: 'w-4 h-4'
  }

  return (
    <div className="flex items-center space-x-1">
      {Array.from({ length: totalDots }, (_, i) => (
        <motion.div
          key={i}
          initial={{ scale: 0 }}
          animate={{ 
            scale: 1,
            backgroundColor: i < currentLevel.value ? currentLevel.color : '#64748B'
          }}
          transition={{ delay: i * 0.1, duration: 0.3 }}
          className={`${sizeClasses[size]} rounded-full`}
        />
      ))}
    </div>
  )
}

const SectorConcentrationChart: React.FC<{
  sectors: SectorExposure[]
}> = ({ sectors }) => {
  const sortedSectors = sectors.sort((a, b) => b.exposure - a.exposure).slice(0, 5)
  
  return (
    <div className="space-y-3">
      <div className="flex items-center space-x-2 text-sm font-medium text-slate-300">
        <PieChart className="h-4 w-4" />
        <span>Sector Exposure</span>
      </div>
      
      {sortedSectors.map((sector, index) => {
        const isOverLimit = sector.exposure > sector.limit
        const percentage = (sector.exposure / sector.limit) * 100
        
        return (
          <div key={sector.sector} className="space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-sm text-slate-300">{sector.sector}</span>
              <div className="flex items-center space-x-2">
                <span className={`text-sm font-semibold ${
                  isOverLimit ? 'text-red-400' : 'text-white'
                }`}>
                  {sector.exposure.toFixed(1)}%
                </span>
                {isOverLimit && (
                  <AlertTriangle className="h-3 w-3 text-red-400" />
                )}
              </div>
            </div>
            
            <div className="w-full bg-slate-700 rounded-full h-2 overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(percentage, 100)}%` }}
                transition={{ delay: index * 0.1, duration: 0.8 }}
                className={`h-full rounded-full ${
                  isOverLimit ? 'bg-red-500' : 
                  percentage > 80 ? 'bg-yellow-500' : 'bg-green-500'
                }`}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

const RiskRecommendations: React.FC<{
  warnings: RiskWarning[]
}> = ({ warnings }) => {
  if (warnings.length === 0) return null

  return (
    <div className="space-y-3">
      <div className="flex items-center space-x-2 text-sm font-medium text-slate-300">
        <Target className="h-4 w-4" />
        <span>Risk Management</span>
      </div>

      {warnings.map((warning, index) => (
        <motion.div
          key={index}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: index * 0.1 }}
          className={`p-3 rounded-lg border ${
            warning.level === 'CRITICAL' ? 'bg-red-900/20 border-red-700/50' :
            warning.level === 'HIGH' ? 'bg-orange-900/20 border-orange-700/50' :
            warning.level === 'MEDIUM' ? 'bg-yellow-900/20 border-yellow-700/50' :
            'bg-blue-900/20 border-blue-700/50'
          }`}
        >
          <div className="flex items-start space-x-3">
            <div className={`p-1 rounded ${
              warning.level === 'CRITICAL' ? 'bg-red-600' :
              warning.level === 'HIGH' ? 'bg-orange-600' :
              warning.level === 'MEDIUM' ? 'bg-yellow-600' :
              'bg-blue-600'
            }`}>
              {warning.action === 'BLOCK' ? (
                <Shield className="h-3 w-3 text-white" />
              ) : (
                <AlertTriangle className="h-3 w-3 text-white" />
              )}
            </div>
            
            <div className="flex-1">
              <p className={`text-sm font-medium ${
                warning.level === 'CRITICAL' ? 'text-red-300' :
                warning.level === 'HIGH' ? 'text-orange-300' :
                warning.level === 'MEDIUM' ? 'text-yellow-300' :
                'text-blue-300'
              }`}>
                {warning.message}
              </p>
              
              {warning.suggestions.length > 0 && (
                <ul className="mt-2 space-y-1">
                  {warning.suggestions.map((suggestion, i) => (
                    <li key={i} className="text-xs text-slate-400 flex items-start">
                      <span className="mr-2">•</span>
                      <span>{suggestion}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </motion.div>
      ))}
    </div>
  )
}

export const RiskMeter: React.FC<RiskMeterProps> = ({
  currentRisk,
  portfolioExposure,
  sectorConcentration,
  dayTradingLimit,
  marginUtilization,
  maxDrawdown,
  totalPositions,
  availableBalance,
  usedMargin,
  className = ''
}) => {
  // Generate risk warnings based on current metrics
  const riskWarnings = useMemo((): RiskWarning[] => {
    const warnings: RiskWarning[] = []

    // Portfolio concentration risk
    if (portfolioExposure > 80) {
      warnings.push({
        level: 'CRITICAL',
        message: 'Portfolio highly concentrated in few positions',
        action: 'WARN',
        suggestions: [
          'Diversify across different sectors',
          'Consider reducing position sizes',
          'Spread investments over time'
        ]
      })
    } else if (portfolioExposure > 60) {
      warnings.push({
        level: 'HIGH',
        message: 'Portfolio concentration above recommended levels',
        action: 'WARN',
        suggestions: [
          'Add positions in different sectors',
          'Review position sizing strategy'
        ]
      })
    }

    // Sector concentration risk
    const highSectorExposure = sectorConcentration.find(s => s.exposure > s.limit)
    if (highSectorExposure) {
      warnings.push({
        level: highSectorExposure.exposure > highSectorExposure.limit * 1.5 ? 'CRITICAL' : 'HIGH',
        message: `High exposure to ${highSectorExposure.sector} sector (${highSectorExposure.exposure.toFixed(1)}%)`,
        action: 'WARN',
        suggestions: [
          'Reduce positions in this sector',
          'Add positions in other sectors',
          'Set stop losses to limit downside'
        ]
      })
    }

    // Margin utilization risk
    if (marginUtilization > 90) {
      warnings.push({
        level: 'CRITICAL',
        message: 'Margin utilization critically high',
        action: 'BLOCK',
        suggestions: [
          'Close some positions immediately',
          'Add more funds to account',
          'Avoid new leveraged positions'
        ]
      })
    } else if (marginUtilization > 70) {
      warnings.push({
        level: 'HIGH',
        message: 'High margin utilization - monitor closely',
        action: 'WARN',
        suggestions: [
          'Consider reducing leverage',
          'Set tight stop losses',
          'Avoid adding new positions'
        ]
      })
    }

    // Drawdown risk
    if (maxDrawdown > 15) {
      warnings.push({
        level: 'HIGH',
        message: `Portfolio drawdown is ${maxDrawdown.toFixed(1)}%`,
        action: 'WARN',
        suggestions: [
          'Review and improve strategy',
          'Consider reducing position sizes',
          'Implement stricter stop losses'
        ]
      })
    }

    return warnings
  }, [portfolioExposure, sectorConcentration, marginUtilization, maxDrawdown])

  const riskColor = {
    'LOW': 'text-green-400',
    'MEDIUM': 'text-yellow-400', 
    'HIGH': 'text-orange-400',
    'CRITICAL': 'text-red-400'
  }[currentRisk]

  return (
    <motion.div 
      className={`glass-widget-card rounded-2xl overflow-hidden ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6 }}
    >
      {/* Header */}
      <div className="p-6 border-b border-slate-700/50">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Shield className={`h-6 w-6 ${riskColor}`} />
            <div>
              <h2 className="text-xl font-bold text-white">Risk Monitor</h2>
              <div className="flex items-center space-x-3 mt-1">
                <span className={`text-sm font-semibold ${riskColor}`}>
                  {currentRisk} RISK
                </span>
                <RiskGauge level={currentRisk} size="sm" />
              </div>
            </div>
          </div>

          <div className="text-right">
            <div className="text-lg font-bold text-white">
              {portfolioExposure.toFixed(1)}%
            </div>
            <div className="text-sm text-slate-400">Portfolio Exposure</div>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Risk Gauge */}
        <div className="bg-slate-800/50 rounded-xl p-4">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-medium text-slate-300">Overall Risk Level</span>
            <span className={`text-lg font-bold ${riskColor}`}>
              {currentRisk}
            </span>
          </div>
          
          <div className="flex justify-center mb-4">
            <RiskGauge level={currentRisk} size="lg" />
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="text-center">
              <div className="text-white font-semibold">{totalPositions}</div>
              <div className="text-slate-400">Active Positions</div>
            </div>
            <div className="text-center">
              <div className="text-white font-semibold">
                ₹{availableBalance.toLocaleString('en-IN')}
              </div>
              <div className="text-slate-400">Available Balance</div>
            </div>
          </div>
        </div>

        {/* Key Metrics */}
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center space-x-2 mb-2">
              <BarChart3 className="h-4 w-4 text-purple-400" />
              <span className="text-sm font-medium text-slate-300">Margin Usage</span>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-slate-400">Used:</span>
                <span className="text-sm font-semibold text-white">
                  ₹{usedMargin.toLocaleString('en-IN')}
                </span>
              </div>
              <div className="w-full bg-slate-700 rounded-full h-2">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${marginUtilization}%` }}
                  transition={{ duration: 0.8 }}
                  className={`h-full rounded-full ${
                    marginUtilization > 90 ? 'bg-red-500' :
                    marginUtilization > 70 ? 'bg-yellow-500' : 'bg-green-500'
                  }`}
                />
              </div>
              <div className="text-right">
                <span className={`text-sm font-semibold ${
                  marginUtilization > 90 ? 'text-red-400' :
                  marginUtilization > 70 ? 'text-yellow-400' : 'text-green-400'
                }`}>
                  {marginUtilization.toFixed(1)}%
                </span>
              </div>
            </div>
          </div>

          <div className="bg-slate-800/30 rounded-xl p-4">
            <div className="flex items-center space-x-2 mb-2">
              <TrendingDown className="h-4 w-4 text-cyan-400" />
              <span className="text-sm font-medium text-slate-300">Max Drawdown</span>
            </div>
            <div className="space-y-2">
              <div className={`text-2xl font-bold ${
                maxDrawdown > 15 ? 'text-red-400' :
                maxDrawdown > 10 ? 'text-yellow-400' : 'text-green-400'
              }`}>
                {maxDrawdown.toFixed(1)}%
              </div>
              <div className="text-xs text-slate-400">
                Peak to trough decline
              </div>
            </div>
          </div>
        </div>

        {/* Sector Concentration */}
        <div className="bg-slate-800/30 rounded-xl p-4">
          <SectorConcentrationChart sectors={sectorConcentration} />
        </div>

        {/* Risk Warnings */}
        <RiskRecommendations warnings={riskWarnings} />

        {/* Day Trading Limit */}
        <div className="bg-slate-800/30 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-2">
              <Target className="h-4 w-4 text-blue-400" />
              <span className="text-sm font-medium text-slate-300">Day Trading Limit</span>
            </div>
            <Info className="h-4 w-4 text-slate-400" />
          </div>
          
          <div className="flex justify-between items-center">
            <span className="text-sm text-slate-400">Remaining Today:</span>
            <span className="text-lg font-semibold text-white">
              ₹{dayTradingLimit.toLocaleString('en-IN')}
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  )
}