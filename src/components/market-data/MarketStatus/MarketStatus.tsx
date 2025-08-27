import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Clock, TrendingUp, TrendingDown, Activity, AlertCircle, CheckCircle } from 'lucide-react'

interface MarketStatusData {
  isOpen: boolean
  nextSession: {
    type: 'open' | 'close'
    time: Date
  }
  indices: {
    name: string
    value: number
    change: number
    changePercent: number
  }[]
  announcements: {
    type: 'info' | 'warning' | 'success'
    message: string
    time: Date
  }[]
}

interface MarketStatusProps {
  className?: string
  compact?: boolean
}

export const MarketStatus: React.FC<MarketStatusProps> = ({ className = '', compact = false }) => {
  const [marketData, setMarketData] = useState<MarketStatusData | null>(null)
  const [currentTime, setCurrentTime] = useState(new Date())

  useEffect(() => {
    // Update time every second
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)

    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    // Mock market data - in real app would fetch from API
    const generateMarketData = (): MarketStatusData => {
      const now = new Date()
      const hour = now.getHours()
      const isOpen = hour >= 9 && hour < 15 && now.getDay() >= 1 && now.getDay() <= 5

      return {
        isOpen,
        nextSession: {
          type: isOpen ? 'close' : 'open',
          time: isOpen 
            ? new Date(now.getFullYear(), now.getMonth(), now.getDate(), 15, 30) 
            : new Date(now.getFullYear(), now.getMonth(), now.getDate() + (now.getHours() >= 15 ? 1 : 0), 9, 15)
        },
        indices: [
          { name: 'NIFTY 50', value: 19847.50, change: 125.30, changePercent: 0.64 },
          { name: 'SENSEX', value: 66589.90, change: 398.50, changePercent: 0.60 },
          { name: 'BANK NIFTY', value: 44521.85, change: -89.20, changePercent: -0.20 },
          { name: 'NIFTY IT', value: 31245.60, change: 234.80, changePercent: 0.76 }
        ],
        announcements: [
          {
            type: 'info',
            message: 'Market volatility expected due to RBI policy announcement',
            time: new Date(Date.now() - 30 * 60 * 1000)
          },
          {
            type: 'success', 
            message: 'All trading systems operating normally',
            time: new Date(Date.now() - 2 * 60 * 60 * 1000)
          }
        ]
      }
    }

    setMarketData(generateMarketData())

    // Refresh every 30 seconds
    const interval = setInterval(() => {
      setMarketData(generateMarketData())
    }, 30000)

    return () => clearInterval(interval)
  }, [])

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('en-IN', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit' 
    })
  }

  const formatTimeUntil = (targetTime: Date) => {
    const diff = targetTime.getTime() - currentTime.getTime()
    if (diff <= 0) return '00:00:00'
    
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)
    
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  }

  if (!marketData) {
    return (
      <div className={`glass-card p-6 rounded-2xl ${className}`}>
        <div className="animate-pulse flex space-x-4">
          <div className="rounded-full bg-slate-700 h-12 w-12"></div>
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-slate-700 rounded w-3/4"></div>
            <div className="h-4 bg-slate-700 rounded w-1/2"></div>
          </div>
        </div>
      </div>
    )
  }

  if (compact) {
    return (
      <motion.div 
        className={`glass-card p-4 rounded-xl ${className}`}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className={`p-2 rounded-lg ${marketData.isOpen ? 'bg-green-500/20' : 'bg-red-500/20'}`}>
              {marketData.isOpen ? (
                <CheckCircle className="w-4 h-4 text-green-400" />
              ) : (
                <Clock className="w-4 h-4 text-red-400" />
              )}
            </div>
            <div>
              <div className="text-sm font-semibold text-white">
                {marketData.isOpen ? 'Market Open' : 'Market Closed'}
              </div>
              <div className="text-xs text-slate-400">
                {marketData.isOpen ? 'Live Trading' : `Opens in ${formatTimeUntil(marketData.nextSession.time)}`}
              </div>
            </div>
          </div>
          
          <div className="text-right">
            <div className="text-sm font-bold text-white">{formatTime(currentTime)}</div>
            <div className="text-xs text-slate-400">IST</div>
          </div>
        </div>
      </motion.div>
    )
  }

  return (
    <motion.div 
      className={`glass-card p-6 rounded-2xl space-y-6 ${className}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      {/* Market Status Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className={`p-3 rounded-xl ${marketData.isOpen ? 'bg-green-500/20' : 'bg-red-500/20'}`}>
            {marketData.isOpen ? (
              <CheckCircle className="w-6 h-6 text-green-400" />
            ) : (
              <Clock className="w-6 h-6 text-red-400" />
            )}
          </div>
          <div>
            <h3 className="text-xl font-bold text-white">
              {marketData.isOpen ? 'Market Open' : 'Market Closed'}
            </h3>
            <p className="text-slate-400">
              {marketData.isOpen ? 'Live Trading Active' : `Next ${marketData.nextSession.type} in`}
            </p>
          </div>
        </div>
        
        <div className="text-right">
          <div className="text-2xl font-bold text-white">{formatTime(currentTime)}</div>
          <div className="text-sm text-slate-400">Indian Standard Time</div>
          {!marketData.isOpen && (
            <div className="text-lg font-semibold text-cyan-400 mt-1">
              {formatTimeUntil(marketData.nextSession.time)}
            </div>
          )}
        </div>
      </div>

      {/* Market Indices */}
      <div>
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <Activity className="w-5 h-5 mr-2 text-purple-400" />
          Market Indices
        </h4>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {marketData.indices.map((index, i) => (
            <motion.div
              key={index.name}
              className="glass-panel p-4 rounded-xl"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.1 }}
            >
              <div className="text-sm text-slate-400 mb-1">{index.name}</div>
              <div className="text-lg font-bold text-white mb-2">
                {index.value.toLocaleString('en-IN', { 
                  minimumFractionDigits: 2, 
                  maximumFractionDigits: 2 
                })}
              </div>
              <div className={`flex items-center text-sm ${
                index.change >= 0 ? 'text-green-400' : 'text-red-400'
              }`}>
                {index.change >= 0 ? (
                  <TrendingUp className="w-3 h-3 mr-1" />
                ) : (
                  <TrendingDown className="w-3 h-3 mr-1" />
                )}
                {index.change >= 0 ? '+' : ''}{index.change.toFixed(2)} ({index.changePercent.toFixed(2)}%)
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Market Announcements */}
      <div>
        <h4 className="text-lg font-semibold text-white mb-4 flex items-center">
          <AlertCircle className="w-5 h-5 mr-2 text-orange-400" />
          Market Updates
        </h4>
        <div className="space-y-3">
          {marketData.announcements.map((announcement, i) => (
            <motion.div
              key={i}
              className={`p-4 rounded-xl border-l-4 ${
                announcement.type === 'success' 
                  ? 'bg-green-500/10 border-green-400' 
                  : announcement.type === 'warning'
                  ? 'bg-orange-500/10 border-orange-400'
                  : 'bg-blue-500/10 border-blue-400'
              }`}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.1 }}
            >
              <div className="flex items-start justify-between">
                <div className="text-sm text-white">{announcement.message}</div>
                <div className="text-xs text-slate-400 ml-4">
                  {announcement.time.toLocaleTimeString('en-IN', { 
                    hour: '2-digit', 
                    minute: '2-digit' 
                  })}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </motion.div>
  )
}

export default MarketStatus