import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronDown, Wifi, WifiOff, TrendingUp, TrendingDown } from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'

interface BrokerBreakdown {
  brokerId: string
  brokerName: string
  value: number
  dayChange: number
  dayChangePercent: number
  connectionStatus: 'connected' | 'disconnected' | 'connecting'
  positions: number
  lastUpdated: Date
}

interface ConsolidatedPortfolio {
  totalValue: number
  dayChange: number
  dayChangePercent: number
  brokerBreakdown: BrokerBreakdown[]
  lastUpdated: Date
  connectionCount: number
}

interface MultiBrokerPortfolioWidgetProps {
  portfolioData: ConsolidatedPortfolio | null
  brokerConnections: BrokerBreakdown[]
  updateInterval?: number
  compactMode?: boolean
  className?: string
}

const BrokerBreakdownItem: React.FC<{ broker: BrokerBreakdown }> = ({ broker }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/30 transition-all duration-300"
    >
      <div className="flex items-center space-x-3">
        <div className={`w-3 h-3 rounded-full ${
          broker.connectionStatus === 'connected' 
            ? 'bg-green-400 animate-pulse shadow-lg shadow-green-400/50' 
            : broker.connectionStatus === 'connecting'
            ? 'bg-yellow-400 animate-pulse shadow-lg shadow-yellow-400/50'
            : 'bg-red-400 shadow-lg shadow-red-400/50'
        }`} />
        <div>
          <p className="text-sm font-medium text-white">{broker.brokerName}</p>
          <p className="text-xs text-slate-400">{broker.positions} positions</p>
        </div>
      </div>
      
      <div className="text-right">
        <p className="text-sm font-semibold text-white">
          ₹{broker.value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
        </p>
        <p className={`text-xs flex items-center ${
          broker.dayChange >= 0 ? 'text-green-400' : 'text-red-400'
        }`}>
          {broker.dayChange >= 0 ? (
            <TrendingUp className="h-3 w-3 mr-1" />
          ) : (
            <TrendingDown className="h-3 w-3 mr-1" />
          )}
          {broker.dayChange >= 0 ? '+' : ''}₹{Math.abs(broker.dayChange).toLocaleString('en-IN')}
        </p>
      </div>
    </motion.div>
  )
}

const AnimatedNumber: React.FC<{
  value: number
  prefix?: string
  className?: string
  duration?: number
}> = ({ value, prefix = '', className = '', duration = 1000 }) => {
  const [displayValue, setDisplayValue] = useState(0)

  useEffect(() => {
    const startTime = Date.now()
    const startValue = displayValue
    const difference = value - startValue

    const updateValue = () => {
      const now = Date.now()
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      
      // Ease out cubic function
      const easeOut = 1 - Math.pow(1 - progress, 3)
      const currentValue = startValue + (difference * easeOut)
      
      setDisplayValue(currentValue)
      
      if (progress < 1) {
        requestAnimationFrame(updateValue)
      }
    }
    
    requestAnimationFrame(updateValue)
  }, [value, duration])

  return (
    <span className={className}>
      {prefix}{Math.round(displayValue).toLocaleString('en-IN')}
    </span>
  )
}

export const MultiBrokerPortfolioWidget: React.FC<MultiBrokerPortfolioWidgetProps> = ({
  portfolioData,
  brokerConnections,
  updateInterval = 5000,
  compactMode = false,
  className = ''
}) => {
  const [expanded, setExpanded] = useState(false)
  const { isConnected } = useConnectionStatus()
  
  // Mock data when no real data is available
  const mockData: ConsolidatedPortfolio = {
    totalValue: 845230,
    dayChange: 12450,
    dayChangePercent: 1.49,
    brokerBreakdown: [
      {
        brokerId: 'zerodha',
        brokerName: 'Zerodha',
        value: 425000,
        dayChange: 8200,
        dayChangePercent: 1.96,
        connectionStatus: 'connected',
        positions: 12,
        lastUpdated: new Date()
      },
      {
        brokerId: 'groww',
        brokerName: 'Groww',
        value: 280000,
        dayChange: 3500,
        dayChangePercent: 1.27,
        connectionStatus: 'connected',
        positions: 8,
        lastUpdated: new Date()
      },
      {
        brokerId: 'angel',
        brokerName: 'Angel One',
        value: 140230,
        dayChange: 750,
        dayChangePercent: 0.54,
        connectionStatus: 'connecting',
        positions: 5,
        lastUpdated: new Date()
      }
    ],
    lastUpdated: new Date(),
    connectionCount: 3
  }

  const data = portfolioData || mockData
  const connectedBrokers = data.brokerBreakdown.filter(b => b.connectionStatus === 'connected').length

  return (
    <motion.div 
      className={`glass-widget-card p-6 rounded-2xl ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
    >
      {/* Portfolio Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-white mb-1">Multi-Broker Portfolio</h2>
          <div className="flex items-center space-x-3">
            <div className={`flex items-center space-x-2 ${
              isConnected ? 'text-green-400' : 'text-red-400'
            }`}>
              {isConnected ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
              <span className="text-sm font-medium">
                {isConnected ? 'Live' : 'Disconnected'}
              </span>
            </div>
            <div className="w-1 h-1 bg-slate-600 rounded-full" />
            <span className="text-sm text-slate-400">
              {connectedBrokers}/{data.brokerBreakdown.length} brokers
            </span>
          </div>
        </div>
        
        {!compactMode && (
          <button 
            onClick={() => setExpanded(!expanded)}
            className="cyber-button-sm p-2 rounded-lg hover:scale-110 transition-all duration-300"
          >
            <ChevronDown className={`w-5 h-5 transition-transform duration-300 ${
              expanded ? 'rotate-180' : ''
            }`} />
          </button>
        )}
      </div>

      {/* Portfolio Metrics */}
      <div className="grid grid-cols-2 gap-6 mb-6">
        <div className="space-y-2">
          <AnimatedNumber 
            value={data.totalValue}
            className="text-3xl font-bold text-white dashboard-metric-enhanced"
            prefix="₹"
            duration={1000}
          />
          <p className="text-sm text-slate-400">Total Portfolio Value</p>
          <div className="w-full bg-slate-700 rounded-full h-1">
            <div 
              className="bg-gradient-to-r from-purple-500 to-cyan-500 h-1 rounded-full transition-all duration-1000"
              style={{ width: '100%' }}
            />
          </div>
        </div>
        
        <div className="space-y-2">
          <div className={`flex items-center space-x-2 ${
            data.dayChange >= 0 
              ? 'dashboard-change-positive' 
              : 'dashboard-change-negative'
          }`}>
            {data.dayChange >= 0 ? (
              <TrendingUp className="w-5 h-5" />
            ) : (
              <TrendingDown className="w-5 h-5" />
            )}
            <AnimatedNumber 
              value={Math.abs(data.dayChange)}
              className="text-2xl font-bold"
              prefix={data.dayChange >= 0 ? '+₹' : '-₹'}
              duration={800}
            />
          </div>
          <p className="text-sm text-slate-400">Today's Change</p>
          <div className={`text-sm font-medium ${
            data.dayChange >= 0 ? 'text-green-400' : 'text-red-400'
          }`}>
            {data.dayChange >= 0 ? '+' : ''}{data.dayChangePercent.toFixed(2)}% from yesterday
          </div>
        </div>
      </div>

      {/* Quick Stats */}
      {!compactMode && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="glass-card p-4 rounded-xl text-center">
            <div className="text-lg font-bold text-white">
              {data.brokerBreakdown.reduce((sum, broker) => sum + broker.positions, 0)}
            </div>
            <div className="text-xs text-slate-400">Total Positions</div>
          </div>
          <div className="glass-card p-4 rounded-xl text-center">
            <div className="text-lg font-bold text-green-400">
              {connectedBrokers}
            </div>
            <div className="text-xs text-slate-400">Connected Brokers</div>
          </div>
          <div className="glass-card p-4 rounded-xl text-center">
            <div className="text-lg font-bold text-cyan-400">
              {data.lastUpdated.toLocaleTimeString('en-IN', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </div>
            <div className="text-xs text-slate-400">Last Updated</div>
          </div>
        </div>
      )}

      {/* Broker Breakdown */}
      <AnimatePresence>
        {(expanded || compactMode) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
            className="border-t border-slate-700 pt-4 space-y-3"
          >
            <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">
              Broker Breakdown
            </h3>
            {data.brokerBreakdown.map((broker) => (
              <BrokerBreakdownItem key={broker.brokerId} broker={broker} />
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Auto-expand for compact mode */}
      {compactMode && (
        <div className="mt-4 pt-4 border-t border-slate-700">
          <div className="text-xs text-slate-400 text-center">
            Updated {Math.floor((Date.now() - data.lastUpdated.getTime()) / 1000)}s ago
          </div>
        </div>
      )}
    </motion.div>
  )
}