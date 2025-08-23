import React, { useState, useEffect } from 'react'
import { Building2, CheckCircle, AlertTriangle, Plus, Settings, Wifi, WifiOff, Shield } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'
import { useAuthStore } from '../../stores/auth.store'

interface BrokerAccount {
  id: string
  brokerName: string
  brokerLogo?: string
  accountId: string
  accountType: 'equity' | 'commodity' | 'currency' | 'mutual_fund'
  status: 'active' | 'inactive' | 'maintenance' | 'suspended'
  balance: number
  marginAvailable: number
  connectionStatus: 'connected' | 'disconnected' | 'connecting'
  lastUpdated: string
  features: string[]
  tradingSegments: string[]
  isDefault: boolean
}

interface BrokerSelectorProps {
  onBrokerSelect?: (broker: BrokerAccount) => void
  selectedBrokerId?: string
  showBalances?: boolean
}

const mockBrokers: BrokerAccount[] = [
  {
    id: '1',
    brokerName: 'Zerodha',
    accountId: 'DA1234',
    accountType: 'equity',
    status: 'active',
    balance: 125000.50,
    marginAvailable: 45000.25,
    connectionStatus: 'connected',
    lastUpdated: '2024-01-15T14:30:00Z',
    features: ['Equity', 'F&O', 'Commodity', 'Currency'],
    tradingSegments: ['NSE', 'BSE', 'MCX', 'NCDEX'],
    isDefault: true
  },
  {
    id: '2',
    brokerName: 'Angel One',
    accountId: 'AN5678',
    accountType: 'equity',
    status: 'active',
    balance: 78500.75,
    marginAvailable: 32000.00,
    connectionStatus: 'connected',
    lastUpdated: '2024-01-15T14:28:00Z',
    features: ['Equity', 'F&O', 'IPO'],
    tradingSegments: ['NSE', 'BSE'],
    isDefault: false
  },
  {
    id: '3',
    brokerName: 'ICICI Direct',
    accountId: 'IC9876',
    accountType: 'equity',
    status: 'active',
    balance: 250000.00,
    marginAvailable: 89000.50,
    connectionStatus: 'connecting',
    lastUpdated: '2024-01-15T14:25:00Z',
    features: ['Equity', 'F&O', 'Mutual Funds', 'Insurance'],
    tradingSegments: ['NSE', 'BSE', 'MCX'],
    isDefault: false
  },
  {
    id: '4',
    brokerName: 'Upstox',
    accountId: 'UP4321',
    accountType: 'equity',
    status: 'maintenance',
    balance: 45000.25,
    marginAvailable: 0,
    connectionStatus: 'disconnected',
    lastUpdated: '2024-01-15T13:45:00Z',
    features: ['Equity', 'F&O'],
    tradingSegments: ['NSE', 'BSE'],
    isDefault: false
  }
]

export function BrokerSelector({ onBrokerSelect, selectedBrokerId, showBalances = true }: BrokerSelectorProps) {
  const [brokers, setBrokers] = useState<BrokerAccount[]>(mockBrokers)
  const [selectedBroker, setSelectedBroker] = useState<string>(selectedBrokerId || brokers.find(b => b.isDefault)?.id || '')
  const [showAddBroker, setShowAddBroker] = useState(false)
  const { success, error, info } = useToast()
  const { user } = useAuthStore()
  
  // Only active brokers are available for traders
  const availableBrokers = user?.role === 'ADMIN' ? brokers : brokers.filter(b => b.status === 'active')

  useEffect(() => {
    // Simulate real-time connection status updates
    const interval = setInterval(() => {
      setBrokers(prev => prev.map(broker => ({
        ...broker,
        lastUpdated: new Date().toISOString(),
        connectionStatus: broker.status === 'maintenance' ? 'disconnected' : 
                         broker.connectionStatus === 'connecting' ? 'connected' :
                         broker.connectionStatus
      })))
    }, 5000)

    return () => clearInterval(interval)
  }, [])

  const handleBrokerSelect = (brokerId: string) => {
    const broker = brokers.find(b => b.id === brokerId)
    if (!broker) return

    if (broker.status !== 'active') {
      error('Broker Unavailable', `${broker.brokerName} is currently ${broker.status}`)
      return
    }

    if (broker.connectionStatus === 'disconnected') {
      error('Connection Failed', `Unable to connect to ${broker.brokerName}`)
      return
    }

    setSelectedBroker(brokerId)
    onBrokerSelect?.(broker)
    success('Broker Selected', `Switched to ${broker.brokerName} (${broker.accountId})`)
  }

  const setAsDefault = (brokerId: string) => {
    setBrokers(prev => prev.map(broker => ({
      ...broker,
      isDefault: broker.id === brokerId
    })))
    info('Default Broker', 'Default broker updated successfully')
  }

  const refreshConnection = (brokerId: string) => {
    setBrokers(prev => prev.map(broker => 
      broker.id === brokerId 
        ? { ...broker, connectionStatus: 'connecting' as const }
        : broker
    ))
    
    setTimeout(() => {
      setBrokers(prev => prev.map(broker => 
        broker.id === brokerId 
          ? { ...broker, connectionStatus: 'connected' as const }
          : broker
      ))
      success('Connection Restored', 'Broker connection refreshed successfully')
    }, 2000)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'text-green-400'
      case 'inactive': return 'text-slate-400'
      case 'maintenance': return 'text-orange-400'
      case 'suspended': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-500/20'
      case 'inactive': return 'bg-slate-500/20'
      case 'maintenance': return 'bg-orange-500/20'
      case 'suspended': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getConnectionIcon = (status: string) => {
    switch (status) {
      case 'connected': return <Wifi className="w-4 h-4 text-green-400" />
      case 'disconnected': return <WifiOff className="w-4 h-4 text-red-400" />
      case 'connecting': return <div className="w-4 h-4 border-2 border-yellow-400 border-t-transparent rounded-full animate-spin" />
      default: return <WifiOff className="w-4 h-4 text-slate-400" />
    }
  }

  const formatLastUpdated = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffInMinutes < 1) return 'Just now'
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`
    return date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className="glass-card rounded-2xl p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Building2 className="w-5 h-5 mr-2 text-cyan-400" />
            {user?.role === 'ADMIN' ? 'Broker Management' : 'Select Trading Broker'}
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            {user?.role === 'ADMIN' 
              ? `${brokers.filter(b => b.status === 'active').length} active • ${brokers.length} total`
              : `${availableBrokers.length} brokers available for trading`
            }
          </p>
        </div>
        {user?.role === 'ADMIN' && (
          <button
            onClick={() => setShowAddBroker(true)}
            className="cyber-button px-4 py-2 text-sm rounded-xl flex items-center space-x-2"
          >
            <Plus className="w-4 h-4" />
            <span>Add Broker</span>
          </button>
        )}
        {user?.role !== 'ADMIN' && (
          <div className="flex items-center space-x-2 px-3 py-2 rounded-xl bg-slate-800/30 border border-slate-600/30">
            <Shield className="w-4 h-4 text-purple-400" />
            <span className="text-xs text-slate-400">Admin Configured</span>
          </div>
        )}
      </div>

      {/* Broker List */}
      <div className="space-y-3">
        {availableBrokers.map((broker) => (
          <div
            key={broker.id}
            className={`p-4 rounded-xl border transition-all cursor-pointer ${
              selectedBroker === broker.id
                ? 'bg-purple-500/10 border-purple-500/50 ring-1 ring-purple-500/30'
                : broker.status !== 'active'
                ? 'bg-slate-800/20 border-slate-700/30 opacity-60'
                : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600/70 hover:bg-slate-700/30'
            }`}
            onClick={() => handleBrokerSelect(broker.id)}
          >
            <div className="flex items-start justify-between">
              <div className="flex items-start space-x-4 flex-1">
                {/* Broker Logo/Icon */}
                <div className={`p-3 rounded-xl ${
                  selectedBroker === broker.id ? 'bg-purple-500/20' : 'bg-slate-600/50'
                }`}>
                  <Building2 className="w-6 h-6 text-cyan-400" />
                </div>
                
                {/* Broker Details */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-3 mb-2">
                    <h4 className="font-semibold text-white">{broker.brokerName}</h4>
                    {broker.isDefault && (
                      <span className="px-2 py-0.5 bg-purple-500/20 text-purple-400 text-xs font-medium rounded">
                        Default
                      </span>
                    )}
                    <div className={`px-2 py-0.5 rounded text-xs font-medium ${
                      getStatusBg(broker.status)} ${getStatusColor(broker.status)
                    }`}>
                      {broker.status.toUpperCase()}
                    </div>
                  </div>
                  
                  <div className="text-sm text-slate-400 mb-2">
                    Account: <span className="text-white font-mono">{broker.accountId}</span>
                  </div>
                  
                  {showBalances && broker.status === 'active' && (
                    <div className="grid gap-2 md:grid-cols-2 text-sm mb-3">
                      <div>
                        <div className="text-slate-400 text-xs">Available Balance</div>
                        <div className="font-semibold text-green-400">
                          ₹{broker.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </div>
                      </div>
                      <div>
                        <div className="text-slate-400 text-xs">Margin Available</div>
                        <div className="font-semibold text-cyan-400">
                          ₹{broker.marginAvailable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </div>
                      </div>
                    </div>
                  )}
                  
                  <div className="flex items-center space-x-4 text-xs text-slate-400">
                    <div className="flex items-center space-x-1">
                      {getConnectionIcon(broker.connectionStatus)}
                      <span className="capitalize">{broker.connectionStatus}</span>
                    </div>
                    <span>Updated {formatLastUpdated(broker.lastUpdated)}</span>
                  </div>
                  
                  {/* Features */}
                  <div className="flex items-center space-x-2 mt-2">
                    {broker.features.slice(0, 4).map(feature => (
                      <span
                        key={feature}
                        className="px-2 py-0.5 bg-slate-700/50 rounded text-xs text-slate-300"
                      >
                        {feature}
                      </span>
                    ))}
                    {broker.features.length > 4 && (
                      <span className="px-2 py-0.5 bg-slate-700/50 rounded text-xs text-slate-300">
                        +{broker.features.length - 4} more
                      </span>
                    )}
                  </div>
                </div>
              </div>
              
              {/* Actions */}
              <div className="flex items-center space-x-2 ml-4">
                {broker.connectionStatus === 'disconnected' && broker.status === 'active' && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      refreshConnection(broker.id)
                    }}
                    className="p-2 rounded-lg hover:bg-green-500/20 text-slate-400 hover:text-green-400 transition-colors"
                    title="Refresh connection"
                  >
                    <Wifi className="w-4 h-4" />
                  </button>
                )}
                
                {!broker.isDefault && broker.status === 'active' && user?.role !== 'ADMIN' && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      setAsDefault(broker.id)
                    }}
                    className="p-2 rounded-lg hover:bg-purple-500/20 text-slate-400 hover:text-purple-400 transition-colors"
                    title="Set as default"
                  >
                    <CheckCircle className="w-4 h-4" />
                  </button>
                )}
                
                {user?.role === 'ADMIN' && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      info('Broker Settings', 'Broker configuration coming soon')
                    }}
                    className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                    title="Broker settings"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Selected Broker Summary */}
      {selectedBroker && (
        <div className="mt-6 p-4 rounded-xl bg-gradient-to-r from-purple-500/10 to-cyan-500/10 border border-purple-500/30">
          {(() => {
            const broker = brokers.find(b => b.id === selectedBroker)
            if (!broker) return null
            
            return (
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-purple-400 font-medium">Active Broker</div>
                  <div className="text-white font-semibold">
                    {broker.brokerName} ({broker.accountId})
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm text-slate-400">Available for Trading</div>
                  <div className="text-green-400 font-semibold">
                    ₹{broker.marginAvailable.toLocaleString('en-IN')}
                  </div>
                </div>
              </div>
            )
          })()}
        </div>
      )}

      {/* Add Broker Modal */}
      {showAddBroker && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="glass-card rounded-2xl p-8 max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-bold text-white">Add New Broker</h3>
              <button
                onClick={() => setShowAddBroker(false)}
                className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            
            <div className="text-center py-8">
              <Building2 className="w-16 h-16 text-slate-500 mx-auto mb-4" />
              <p className="text-slate-400 mb-4">
                Configure broker connections for your organization
              </p>
              <div className="space-y-3">
                <button className="w-full cyber-button py-3 rounded-xl text-sm">
                  Configure Zerodha
                </button>
                <button className="w-full glass-card py-3 rounded-xl text-sm text-slate-400 hover:text-white transition-colors">
                  Configure Angel One
                </button>
                <button className="w-full glass-card py-3 rounded-xl text-sm text-slate-400 hover:text-white transition-colors">
                  Configure ICICI Direct
                </button>
                <button className="w-full glass-card py-3 rounded-xl text-sm text-slate-400 hover:text-white transition-colors">
                  Other Brokers
                </button>
              </div>
              <p className="text-xs text-slate-500 mt-4">
                Only admins can configure broker connections
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}