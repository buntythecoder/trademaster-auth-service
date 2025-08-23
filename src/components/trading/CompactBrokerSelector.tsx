import React, { useState } from 'react'
import { ChevronDown, CheckCircle, Wifi, WifiOff, Activity, Zap, Building2 } from 'lucide-react'

interface BrokerOption {
  id: string
  name: string
  icon: string
  status: 'connected' | 'disconnected' | 'maintenance'
  accountId: string
  balance: number
  lastSync: string
  capabilities: string[]
  color: string
  responseTime: number
  activeTrades: number
  todayTrades: number
  todayVolume: number
  uptime: number
  dataFeed: 'live' | 'delayed' | 'offline'
  commissionRate: number
}

// Mock admin-configured brokers
const availableBrokers: BrokerOption[] = [
  {
    id: 'zerodha_main',
    name: 'Zerodha',
    icon: '⚡',
    status: 'connected',
    accountId: 'ZD1234',
    balance: 45230,
    lastSync: '2 mins ago',
    capabilities: ['stocks', 'derivatives', 'commodities'],
    color: 'from-blue-500 to-cyan-500',
    responseTime: 45,
    activeTrades: 3,
    todayTrades: 12,
    todayVolume: 2580000,
    uptime: 99.8,
    dataFeed: 'live',
    commissionRate: 0.03
  },
  {
    id: 'upstox_backup',
    name: 'Upstox',
    icon: '📈',
    status: 'connected',
    accountId: 'UP5678',
    balance: 28500,
    lastSync: '5 mins ago',
    capabilities: ['stocks', 'derivatives'],
    color: 'from-orange-500 to-red-500',
    responseTime: 78,
    activeTrades: 1,
    todayTrades: 8,
    todayVolume: 1240000,
    uptime: 99.2,
    dataFeed: 'live',
    commissionRate: 0.05
  },
  {
    id: 'angel_one',
    name: 'Angel One',
    icon: '👼',
    status: 'maintenance',
    accountId: 'AN9012',
    balance: 0,
    lastSync: '2 hours ago',
    capabilities: ['stocks', 'derivatives', 'commodities'],
    color: 'from-purple-500 to-pink-500',
    responseTime: 0,
    activeTrades: 0,
    todayTrades: 0,
    todayVolume: 0,
    uptime: 95.4,
    dataFeed: 'offline',
    commissionRate: 0.04
  },
  {
    id: 'fyers',
    name: 'Fyers',
    icon: '🚀',
    status: 'connected',
    accountId: 'FY3456',
    balance: 15750,
    lastSync: '1 min ago',
    capabilities: ['stocks', 'derivatives'],
    color: 'from-green-500 to-teal-500',
    responseTime: 32,
    activeTrades: 2,
    todayTrades: 15,
    todayVolume: 1890000,
    uptime: 99.9,
    dataFeed: 'live',
    commissionRate: 0.025
  }
]

interface CompactBrokerSelectorProps {
  onBrokerChange?: (broker: BrokerOption) => void
}

export function CompactBrokerSelector({ onBrokerChange }: CompactBrokerSelectorProps) {
  const [selectedBroker, setSelectedBroker] = useState(availableBrokers[0])
  const [isOpen, setIsOpen] = useState(false)
  
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'text-green-400'
      case 'disconnected': return 'text-red-400'  
      case 'maintenance': return 'text-yellow-400'
      default: return 'text-slate-400'
    }
  }
  
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected': return <Wifi className="w-3 h-3" />
      case 'disconnected': return <WifiOff className="w-3 h-3" />
      case 'maintenance': return <Activity className="w-3 h-3 animate-pulse" />
      default: return <WifiOff className="w-3 h-3" />
    }
  }

  const getResponseTimeColor = (responseTime: number) => {
    if (responseTime <= 50) return 'text-green-400'
    if (responseTime <= 100) return 'text-yellow-400'
    return 'text-red-400'
  }

  const getDataFeedIcon = (dataFeed: string) => {
    switch (dataFeed) {
      case 'live': return '🟢'
      case 'delayed': return '🟡'
      case 'offline': return '🔴'
      default: return '⚪'
    }
  }

  const formatVolume = (volume: number) => {
    if (volume >= 10000000) return `₹${(volume / 10000000).toFixed(1)}Cr`
    if (volume >= 100000) return `₹${(volume / 100000).toFixed(1)}L`
    if (volume >= 1000) return `₹${(volume / 1000).toFixed(1)}K`
    return `₹${volume}`
  }

  const handleBrokerSelect = (broker: BrokerOption) => {
    setSelectedBroker(broker)
    setIsOpen(false)
    onBrokerChange?.(broker)
  }

  return (
    <div className="relative">
      {/* Current Selection - Compact Display */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="cyber-button group relative overflow-hidden glass-card-dark rounded-2xl transition-all duration-300 hover:scale-[1.02] hover:shadow-xl w-full border border-slate-700/50 hover:border-purple-500/50 backdrop-blur-xl"
      >
        {/* Gradient Background */}
        <div className={`absolute inset-0 bg-gradient-to-r ${selectedBroker.color} opacity-10 group-hover:opacity-20 transition-opacity`} />
        
        {/* Glass Effect */}
        <div className="p-4 relative backdrop-blur-xl">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              {/* Broker Icon */}
              <div className="text-2xl">{selectedBroker.icon}</div>
              
              {/* Broker Info */}
              <div className="flex-1">
                <div className="flex items-center space-x-2 mb-1">
                  <span className="font-bold text-white">{selectedBroker.name}</span>
                  <div className={`flex items-center space-x-1 ${getStatusColor(selectedBroker.status)}`}>
                    {getStatusIcon(selectedBroker.status)}
                    <span className="text-xs font-medium capitalize">{selectedBroker.status}</span>
                  </div>
                  <span className="text-xs">{getDataFeedIcon(selectedBroker.dataFeed)}</span>
                </div>
                
                <div className="grid grid-cols-2 gap-x-4 text-xs">
                  <div className="flex items-center space-x-2">
                    <span className="text-slate-400">{selectedBroker.accountId}</span>
                    <span className="text-green-400 font-semibold">
                      ₹{selectedBroker.balance.toLocaleString()}
                    </span>
                  </div>
                  
                  <div className="flex items-center space-x-3">
                    <span className={`${getResponseTimeColor(selectedBroker.responseTime)}`}>
                      {selectedBroker.responseTime}ms
                    </span>
                    <span className="text-cyan-400">
                      {selectedBroker.activeTrades} active
                    </span>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Dropdown Arrow */}
            <ChevronDown className={`w-5 h-5 text-slate-400 group-hover:text-white transition-all duration-300 ${
              isOpen ? 'rotate-180' : 'rotate-0'
            }`} />
          </div>
        </div>
      </button>

      {/* Dropdown Options */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-3 z-50">
          <div className="rounded-2xl border border-slate-600/60 max-h-96 overflow-hidden backdrop-blur-xl shadow-2xl bg-slate-900/98">
            <div className="p-3">
              {availableBrokers.map((broker) => (
                <button
                  key={broker.id}
                  onClick={() => handleBrokerSelect(broker)}
                  disabled={broker.status === 'maintenance'}
                  className={`w-full mb-3 last:mb-0 p-4 rounded-xl text-left transition-all duration-300 group relative overflow-hidden hover:scale-[1.02] bg-slate-800/80 backdrop-blur-sm border ${
                    broker.status === 'maintenance' 
                      ? 'opacity-50 cursor-not-allowed border-slate-700/40' 
                      : 'cursor-pointer hover:shadow-lg hover:bg-slate-700/90 hover:border-cyan-500/40 border-slate-700/40'
                  } ${
                    selectedBroker.id === broker.id 
                      ? 'ring-2 ring-purple-400/60 bg-purple-500/25 border-purple-500/50' 
                      : ''
                  }`}
                >
                  {/* Background Gradient */}
                  {broker.status !== 'maintenance' && (
                    <div className={`absolute inset-0 bg-gradient-to-r ${broker.color} opacity-0 group-hover:opacity-20 transition-opacity`} />
                  )}
                  
                  {/* Content */}
                  <div className="relative">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <div className="text-xl">{broker.icon}</div>
                        <div>
                          <div className="flex items-center space-x-2 mb-1">
                            <span className="font-semibold text-white">{broker.name}</span>
                            <div className={`flex items-center space-x-1 ${getStatusColor(broker.status)}`}>
                              {getStatusIcon(broker.status)}
                              <span className="text-xs font-medium capitalize">{broker.status}</span>
                            </div>
                            <span className="text-xs">{getDataFeedIcon(broker.dataFeed)}</span>
                            {selectedBroker.id === broker.id && (
                              <CheckCircle className="w-4 h-4 text-green-400" />
                            )}
                          </div>
                          <div className="flex items-center space-x-3 text-xs text-slate-400">
                            <span>{broker.accountId}</span>
                            <span className="text-slate-500">•</span>
                            <span>{broker.lastSync}</span>
                          </div>
                        </div>
                      </div>
                      
                      {/* Uptime Badge */}
                      <div className="flex items-center space-x-2">
                        <span className={`text-xs px-2 py-1 rounded-full ${
                          broker.uptime >= 99.5 
                            ? 'bg-green-500/20 text-green-400'
                            : broker.uptime >= 98 
                            ? 'bg-yellow-500/20 text-yellow-400'
                            : 'bg-red-500/20 text-red-400'
                        }`}>
                          {broker.uptime}% uptime
                        </span>
                      </div>
                    </div>

                    {/* Performance Metrics */}
                    <div className="grid grid-cols-4 gap-3 mb-3 text-xs">
                      <div className="text-center">
                        <div className={`font-semibold ${getResponseTimeColor(broker.responseTime)}`}>
                          {broker.responseTime || 0}ms
                        </div>
                        <div className="text-slate-500">Response</div>
                      </div>
                      <div className="text-center">
                        <div className="text-cyan-400 font-semibold">{broker.activeTrades}</div>
                        <div className="text-slate-500">Active</div>
                      </div>
                      <div className="text-center">
                        <div className="text-blue-400 font-semibold">{broker.todayTrades}</div>
                        <div className="text-slate-500">Today</div>
                      </div>
                      <div className="text-center">
                        <div className="text-purple-400 font-semibold">{broker.commissionRate}%</div>
                        <div className="text-slate-500">Commission</div>
                      </div>
                    </div>

                    {/* Balance and Volume */}
                    <div className="flex items-center justify-between mb-2">
                      {broker.status === 'connected' && (
                        <span className="text-green-400 font-semibold">
                          Balance: ₹{broker.balance.toLocaleString()}
                        </span>
                      )}
                      <span className="text-orange-400 text-xs">
                        Volume: {formatVolume(broker.todayVolume)}
                      </span>
                    </div>

                    {/* Capabilities */}
                    <div className="flex items-center space-x-1">
                      {broker.capabilities.slice(0, 3).map((cap) => (
                        <span 
                          key={cap} 
                          className="text-xs px-2 py-1 rounded-full bg-slate-700/50 text-slate-300 capitalize"
                        >
                          {cap}
                        </span>
                      ))}
                      {broker.capabilities.length > 3 && (
                        <span className="text-xs text-slate-500">+{broker.capabilities.length - 3} more</span>
                      )}
                    </div>
                  </div>
                </button>
              ))}
            </div>
            
            {/* Footer */}
            <div className="border-t border-slate-600/50 p-4 bg-slate-800/95 backdrop-blur-xl">
              <div className="flex items-center justify-center space-x-2 text-xs text-slate-200">
                <Building2 className="w-3 h-3 text-cyan-400" />
                <span>Admin configured brokers • <span className="text-purple-400 font-medium">Contact support for changes</span></span>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* Overlay to close dropdown */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-40" 
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  )
}