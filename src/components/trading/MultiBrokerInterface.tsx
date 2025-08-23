import React, { useState, useEffect } from 'react'
import { 
  Plus, 
  Settings, 
  Trash2, 
  Wifi, 
  WifiOff, 
  Eye, 
  EyeOff, 
  AlertCircle, 
  CheckCircle, 
  Clock,
  Activity,
  Shield,
  Zap,
  BarChart3,
  TrendingUp,
  X,
  Save,
  Building2,
  Star
} from 'lucide-react'

interface BrokerConnection {
  id: string
  name: string
  brokerType: 'interactive_brokers' | 'td_ameritrade' | 'alpaca' | 'fidelity' | 'schwab' | 'etrade'
  displayName: string
  status: 'connected' | 'disconnected' | 'connecting' | 'error' | 'authenticating'
  isDefault: boolean
  capabilities: string[]
  lastConnected?: Date
  accountId?: string
  apiKey?: string
  connectionConfig: Record<string, any>
  performance: {
    avgExecutionTime: number
    successRate: number
    totalOrders: number
  }
}

interface BrokerCredentials {
  brokerType: string
  apiKey: string
  secretKey: string
  accountId: string
  sandbox: boolean
  additionalConfig: Record<string, any>
}

const brokerTypes = [
  { 
    value: 'interactive_brokers', 
    label: 'Interactive Brokers', 
    capabilities: ['stocks', 'options', 'futures', 'forex'],
    icon: '🏦',
    description: 'Professional trading platform with global markets'
  },
  { 
    value: 'td_ameritrade', 
    label: 'TD Ameritrade', 
    capabilities: ['stocks', 'options', 'etfs'],
    icon: '🏛️',
    description: 'Full-service broker with advanced tools'
  },
  { 
    value: 'alpaca', 
    label: 'Alpaca Markets', 
    capabilities: ['stocks', 'crypto'],
    icon: '🦙',
    description: 'Commission-free API-first trading'
  },
  { 
    value: 'fidelity', 
    label: 'Fidelity', 
    capabilities: ['stocks', 'options', 'mutual_funds'],
    icon: '🛡️',
    description: 'Trusted investment management'
  },
  { 
    value: 'schwab', 
    label: 'Charles Schwab', 
    capabilities: ['stocks', 'options', 'etfs'],
    icon: '⚡',
    description: 'Low-cost trading and investing'
  },
  { 
    value: 'etrade', 
    label: 'E*TRADE', 
    capabilities: ['stocks', 'options'],
    icon: '💎',
    description: 'Online investing and trading'
  }
]

export const MultiBrokerInterface: React.FC = () => {
  const { user } = useAuthStore()
  const [brokerConnections, setBrokerConnections] = useState<BrokerConnection[]>([])
  const [loading, setLoading] = useState(true)
  
  // Admin-only access control
  if (user?.role !== 'ADMIN') {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="glass-card rounded-2xl p-8 max-w-md w-full mx-4 text-center">
          <div className="p-4 rounded-xl bg-red-500/20 mb-6">
            <Lock className="w-12 h-12 text-red-400 mx-auto mb-4" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-4">Access Restricted</h2>
          <p className="text-slate-400 mb-6">
            Broker configuration is restricted to administrators only. 
            Contact your admin to configure broker connections.
          </p>
          <div className="glass-card p-4 rounded-xl bg-slate-800/30">
            <div className="text-sm text-slate-300">
              <strong>Current Role:</strong> {user?.role || 'Unknown'}<br/>
              <strong>Required Role:</strong> ADMIN
            </div>
          </div>
        </div>
      </div>
    )
  }
  const [showAddModal, setShowAddModal] = useState(false)
  const [showCredentialsModal, setShowCredentialsModal] = useState(false)
  const [selectedBroker, setSelectedBroker] = useState<BrokerConnection | null>(null)
  const [newCredentials, setNewCredentials] = useState<BrokerCredentials>({
    brokerType: '',
    apiKey: '',
    secretKey: '',
    accountId: '',
    sandbox: true,
    additionalConfig: {}
  })
  const [showSecrets, setShowSecrets] = useState<Record<string, boolean>>({})
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)

  useEffect(() => {
    fetchBrokerConnections()
  }, [])

  const fetchBrokerConnections = async () => {
    setLoading(true)
    try {
      // Simulated API call - replace with actual API
      const mockConnections: BrokerConnection[] = [
        {
          id: '1',
          name: 'IB-Primary',
          brokerType: 'interactive_brokers',
          displayName: 'Interactive Brokers - Main Account',
          status: 'connected',
          isDefault: true,
          capabilities: ['stocks', 'options', 'futures'],
          lastConnected: new Date(),
          accountId: 'DU123456',
          connectionConfig: {},
          performance: { avgExecutionTime: 250, successRate: 98.5, totalOrders: 1250 }
        },
        {
          id: '2',
          name: 'Alpaca-Test',
          brokerType: 'alpaca',
          displayName: 'Alpaca Markets - Paper Trading',
          status: 'connected',
          isDefault: false,
          capabilities: ['stocks', 'crypto'],
          lastConnected: new Date(Date.now() - 5 * 60 * 1000),
          accountId: 'PA123456',
          connectionConfig: { sandbox: true },
          performance: { avgExecutionTime: 180, successRate: 99.2, totalOrders: 856 }
        },
        {
          id: '3',
          name: 'TD-Secondary',
          brokerType: 'td_ameritrade',
          displayName: 'TD Ameritrade - Options Account',
          status: 'disconnected',
          isDefault: false,
          capabilities: ['stocks', 'options'],
          lastConnected: new Date(Date.now() - 2 * 60 * 60 * 1000),
          accountId: 'TD789012',
          connectionConfig: {},
          performance: { avgExecutionTime: 320, successRate: 97.8, totalOrders: 643 }
        }
      ]
      setBrokerConnections(mockConnections)
    } catch (error) {
      console.error('Failed to fetch broker connections:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleConnect = async (brokerId: string) => {
    setBrokerConnections(prev => 
      prev.map(broker => 
        broker.id === brokerId 
          ? { ...broker, status: 'connecting' }
          : broker
      )
    )

    try {
      // Simulated connection - replace with actual API
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      setBrokerConnections(prev => 
        prev.map(broker => 
          broker.id === brokerId 
            ? { ...broker, status: 'connected', lastConnected: new Date() }
            : broker
        )
      )
    } catch (error) {
      setBrokerConnections(prev => 
        prev.map(broker => 
          broker.id === brokerId 
            ? { ...broker, status: 'error' }
            : broker
        )
      )
    }
  }

  const handleDisconnect = async (brokerId: string) => {
    try {
      setBrokerConnections(prev => 
        prev.map(broker => 
          broker.id === brokerId 
            ? { ...broker, status: 'disconnected' }
            : broker
        )
      )
    } catch (error) {
      console.error('Failed to disconnect broker:', error)
    }
  }

  const handleSetDefault = async (brokerId: string) => {
    try {
      setBrokerConnections(prev => 
        prev.map(broker => ({
          ...broker,
          isDefault: broker.id === brokerId
        }))
      )
    } catch (error) {
      console.error('Failed to set default broker:', error)
    }
  }

  const handleAddBroker = async () => {
    if (!newCredentials.brokerType || !newCredentials.apiKey || !newCredentials.accountId) {
      alert('Please fill in all required fields')
      return
    }

    // For custom brokers, require additional fields
    if (newCredentials.brokerType === 'custom') {
      if (!newCredentials.additionalConfig.customName || !newCredentials.additionalConfig.baseUrl) {
        alert('Please fill in custom broker name and API URL')
        return
      }
    }

    try {
      const brokerInfo = brokerTypes.find(b => b.value === newCredentials.brokerType)
      let displayName = ''
      let capabilities: string[] = []

      if (newCredentials.brokerType === 'custom') {
        displayName = newCredentials.additionalConfig.customName || 'Custom Broker'
        capabilities = newCredentials.additionalConfig.capabilities 
          ? newCredentials.additionalConfig.capabilities.split(',').map(s => s.trim())
          : ['stocks']
      } else {
        displayName = brokerInfo?.label || ''
        capabilities = brokerInfo?.capabilities || []
      }

      const newBroker: BrokerConnection = {
        id: Date.now().toString(),
        name: `${newCredentials.brokerType === 'custom' ? 'custom' : newCredentials.brokerType}-${Date.now()}`,
        brokerType: newCredentials.brokerType as any,
        displayName: displayName,
        status: 'disconnected',
        isDefault: false,
        capabilities: capabilities,
        connectionConfig: {
          apiKey: newCredentials.apiKey,
          accountId: newCredentials.accountId,
          sandbox: newCredentials.sandbox,
          ...(newCredentials.brokerType === 'custom' && {
            customName: newCredentials.additionalConfig.customName,
            baseUrl: newCredentials.additionalConfig.baseUrl,
          })
        },
        accountId: newCredentials.accountId,
        performance: { avgExecutionTime: 0, successRate: 0, totalOrders: 0 }
      }

      setBrokerConnections(prev => [...prev, newBroker])
      setShowAddModal(false)
      setNewCredentials({
        brokerType: '',
        apiKey: '',
        secretKey: '',
        accountId: '',
        sandbox: true,
        additionalConfig: {}
      })
      
      alert(`${displayName} broker connection added successfully!`)
    } catch (error) {
      console.error('Failed to add broker:', error)
      alert('Failed to add broker connection')
    }
  }

  const handleDeleteBroker = async (brokerId: string) => {
    try {
      setBrokerConnections(prev => prev.filter(broker => broker.id !== brokerId))
      setDeleteConfirm(null)
    } catch (error) {
      console.error('Failed to delete broker:', error)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'text-green-400'
      case 'connecting': return 'text-yellow-400'
      case 'error': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'connected': return 'bg-green-500/20'
      case 'connecting': return 'bg-yellow-500/20'
      case 'error': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected': return <CheckCircle className="w-4 h-4" />
      case 'connecting': return <Clock className="w-4 h-4 animate-spin" />
      case 'error': return <AlertCircle className="w-4 h-4" />
      default: return <WifiOff className="w-4 h-4" />
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-3">
          <Clock className="w-6 h-6 text-purple-400 animate-spin" />
          <span className="text-slate-400">Loading broker connections...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Multi-Broker Interface</h1>
          <p className="text-slate-400">Manage your trading broker connections and routing preferences</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2"
        >
          <Plus className="w-5 h-5" />
          <span>Add Broker</span>
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-green-500/20">
              <CheckCircle className="w-6 h-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-green-400">
                {brokerConnections.filter(b => b.status === 'connected').length}
              </div>
              <div className="text-sm text-slate-400">Connected</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Active Brokers</h3>
          <p className="text-slate-400 text-sm">Currently connected</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-purple-500/20">
              <Building2 className="w-6 h-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-purple-400">
                {brokerConnections.length}
              </div>
              <div className="text-sm text-slate-400">Total</div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Total Brokers</h3>
          <p className="text-slate-400 text-sm">Configured brokers</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-blue-500/20">
              <Activity className="w-6 h-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-blue-400">
                {Math.round(brokerConnections.reduce((sum, b) => sum + b.performance.successRate, 0) / brokerConnections.length || 0)}%
              </div>
              <div className="text-sm text-slate-400">Avg Success</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">Success Rate</h3>
          <p className="text-slate-400 text-sm">Order execution</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-orange-500/20">
              <Zap className="w-6 h-6 text-orange-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-orange-400">
                {Math.round(brokerConnections.reduce((sum, b) => sum + b.performance.avgExecutionTime, 0) / brokerConnections.length || 0)}ms
              </div>
              <div className="text-sm text-slate-400">Avg Speed</div>
            </div>
          </div>
          <h3 className="text-orange-400 font-semibold mb-1">Execution Time</h3>
          <p className="text-slate-400 text-sm">Average latency</p>
        </div>
      </div>

      {/* Broker Connections */}
      <div className="space-y-4">
        {brokerConnections.map((broker) => {
          const brokerInfo = brokerTypes.find(b => b.value === broker.brokerType)
          
          return (
            <div key={broker.id} className="glass-card rounded-2xl p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <div className="text-3xl">{brokerInfo?.icon}</div>
                  
                  <div>
                    <div className="flex items-center space-x-3 mb-2">
                      <h3 className="text-xl font-semibold text-white">{broker.displayName}</h3>
                      {broker.isDefault && (
                        <div className="flex items-center space-x-1 px-3 py-1 rounded-full bg-purple-500/20 text-purple-400">
                          <Star className="w-3 h-3" />
                          <span className="text-xs font-medium">Default</span>
                        </div>
                      )}
                      <div className={`flex items-center space-x-2 px-3 py-1 rounded-full ${getStatusBg(broker.status)} ${getStatusColor(broker.status)}`}>
                        {getStatusIcon(broker.status)}
                        <span className="text-xs font-medium capitalize">{broker.status}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4 text-sm text-slate-400">
                      <span>Account: {broker.accountId || 'Not configured'}</span>
                      {broker.lastConnected && (
                        <span>Last: {new Date(broker.lastConnected).toLocaleTimeString()}</span>
                      )}
                      <div className="flex items-center space-x-1">
                        <Activity className="w-3 h-3" />
                        <span>{broker.performance.totalOrders} orders</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="flex items-center space-x-3">
                  {/* Performance Metrics */}
                  <div className="hidden lg:flex space-x-6 text-sm">
                    <div className="text-center">
                      <div className="text-green-400 font-semibold">{broker.performance.successRate}%</div>
                      <div className="text-slate-400 text-xs">Success</div>
                    </div>
                    <div className="text-center">
                      <div className="text-blue-400 font-semibold">{broker.performance.avgExecutionTime}ms</div>
                      <div className="text-slate-400 text-xs">Latency</div>
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center space-x-2">
                    {broker.status === 'connected' ? (
                      <button
                        onClick={() => handleDisconnect(broker.id)}
                        className="px-4 py-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors font-medium"
                      >
                        Disconnect
                      </button>
                    ) : (
                      <button
                        onClick={() => handleConnect(broker.id)}
                        disabled={broker.status === 'connecting'}
                        className="px-4 py-2 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
                      >
                        {broker.status === 'connecting' ? 'Connecting...' : 'Connect'}
                      </button>
                    )}
                    
                    {!broker.isDefault && broker.status === 'connected' && (
                      <button
                        onClick={() => handleSetDefault(broker.id)}
                        className="px-4 py-2 rounded-xl bg-purple-500/20 text-purple-400 hover:bg-purple-500/30 transition-colors font-medium"
                      >
                        Set Default
                      </button>
                    )}
                    
                    <button
                      onClick={() => alert(`Configuration for ${broker.displayName} coming soon! This will allow you to modify API settings, timeouts, and broker-specific options.`)}
                      className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
                      title="Configure"
                    >
                      <Settings className="w-4 h-4" />
                    </button>
                    
                    <button
                      onClick={() => setDeleteConfirm(broker.id)}
                      className="p-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
              
              {/* Capabilities */}
              <div className="mt-4 pt-4 border-t border-slate-700/50">
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-slate-400">Capabilities:</span>
                  <div className="flex flex-wrap gap-2">
                    {broker.capabilities.map((cap, index) => (
                      <span key={index} className="px-2 py-1 rounded-lg bg-slate-800/50 text-xs text-slate-300 capitalize">
                        {cap}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Add Broker Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-slate-700/50 flex-shrink-0">
              <div className="flex items-center space-x-3">
                <Plus className="w-6 h-6 text-purple-400" />
                <div>
                  <h2 className="text-xl font-bold text-white">Add New Broker</h2>
                  <p className="text-slate-400 text-sm">Connect a new trading broker</p>
                </div>
              </div>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-2 hover:bg-slate-700/50 rounded-xl transition-colors"
              >
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>

            {/* Modal Content - Scrollable */}
            <div className="flex-1 overflow-y-auto">
              <div className="p-6 space-y-6">
                {/* Broker Selection */}
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-3">Select Broker Type</label>
                  <div className="grid gap-3 md:grid-cols-2">
                    {/* Predefined Brokers */}
                    {brokerTypes.map((broker) => (
                      <button
                        key={broker.value}
                        onClick={() => setNewCredentials(prev => ({ ...prev, brokerType: broker.value }))}
                        className={`p-4 rounded-xl border-2 transition-all text-left ${
                          newCredentials.brokerType === broker.value
                            ? 'border-purple-500/50 bg-purple-500/10'
                            : 'border-slate-700/50 bg-slate-800/30 hover:border-slate-600/50'
                        }`}
                      >
                        <div className="flex items-start space-x-3">
                          <span className="text-2xl">{broker.icon}</span>
                          <div>
                            <h3 className="font-semibold text-white">{broker.label}</h3>
                            <p className="text-xs text-slate-400 mb-2">{broker.description}</p>
                            <div className="flex flex-wrap gap-1">
                              {broker.capabilities.map((cap, index) => (
                                <span key={index} className="px-2 py-1 rounded bg-slate-700/50 text-xs text-slate-300">
                                  {cap}
                                </span>
                              ))}
                            </div>
                          </div>
                        </div>
                      </button>
                    ))}
                    
                    {/* Custom Broker Option */}
                    <button
                      onClick={() => setNewCredentials(prev => ({ ...prev, brokerType: 'custom' }))}
                      className={`p-4 rounded-xl border-2 transition-all text-left ${
                        newCredentials.brokerType === 'custom'
                          ? 'border-cyan-500/50 bg-cyan-500/10'
                          : 'border-slate-700/50 bg-slate-800/30 hover:border-slate-600/50'
                      }`}
                    >
                      <div className="flex items-start space-x-3">
                        <span className="text-2xl">🔧</span>
                        <div>
                          <h3 className="font-semibold text-white">Custom Broker</h3>
                          <p className="text-xs text-slate-400 mb-2">Configure your own broker connection</p>
                          <div className="flex flex-wrap gap-1">
                            <span className="px-2 py-1 rounded bg-slate-700/50 text-xs text-slate-300">flexible</span>
                            <span className="px-2 py-1 rounded bg-slate-700/50 text-xs text-slate-300">custom</span>
                          </div>
                        </div>
                      </div>
                    </button>
                  </div>
                </div>

              {/* Configuration Form */}
              {newCredentials.brokerType && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold text-white">Connection Configuration</h3>
                  
                  {/* Custom broker name field */}
                  {newCredentials.brokerType === 'custom' && (
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Custom Broker Name</label>
                      <input
                        type="text"
                        value={newCredentials.additionalConfig.customName || ''}
                        onChange={(e) => setNewCredentials(prev => ({ 
                          ...prev, 
                          additionalConfig: { ...prev.additionalConfig, customName: e.target.value }
                        }))}
                        className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-cyan-400/50 focus:ring-1 focus:ring-cyan-400/50"
                        placeholder="Enter custom broker name (e.g., ABC Securities)"
                      />
                    </div>
                  )}

                  {/* Custom broker URL field */}
                  {newCredentials.brokerType === 'custom' && (
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">API Base URL</label>
                      <input
                        type="url"
                        value={newCredentials.additionalConfig.baseUrl || ''}
                        onChange={(e) => setNewCredentials(prev => ({ 
                          ...prev, 
                          additionalConfig: { ...prev.additionalConfig, baseUrl: e.target.value }
                        }))}
                        className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-cyan-400/50 focus:ring-1 focus:ring-cyan-400/50"
                        placeholder="https://api.yourbroker.com/v1"
                      />
                    </div>
                  )}

                  {/* Custom capabilities */}
                  {newCredentials.brokerType === 'custom' && (
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Supported Assets (comma-separated)</label>
                      <input
                        type="text"
                        value={newCredentials.additionalConfig.capabilities || ''}
                        onChange={(e) => setNewCredentials(prev => ({ 
                          ...prev, 
                          additionalConfig: { ...prev.additionalConfig, capabilities: e.target.value }
                        }))}
                        className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-cyan-400/50 focus:ring-1 focus:ring-cyan-400/50"
                        placeholder="stocks, options, futures, crypto"
                      />
                    </div>
                  )}
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">Account ID</label>
                      <input
                        type="text"
                        value={newCredentials.accountId}
                        onChange={(e) => setNewCredentials(prev => ({ ...prev, accountId: e.target.value }))}
                        className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50"
                        placeholder="Enter your account ID"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-slate-300 mb-2">API Key</label>
                      <div className="relative">
                        <input
                          type={showSecrets.apiKey ? 'text' : 'password'}
                          value={newCredentials.apiKey}
                          onChange={(e) => setNewCredentials(prev => ({ ...prev, apiKey: e.target.value }))}
                          className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50 pr-12"
                          placeholder="Enter your API key"
                        />
                        <button
                          type="button"
                          onClick={() => setShowSecrets(prev => ({ ...prev, apiKey: !prev.apiKey }))}
                          className="absolute right-3 top-3 text-slate-400 hover:text-white"
                        >
                          {showSecrets.apiKey ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                        </button>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-300 mb-2">Secret Key</label>
                    <div className="relative">
                      <input
                        type={showSecrets.secretKey ? 'text' : 'password'}
                        value={newCredentials.secretKey}
                        onChange={(e) => setNewCredentials(prev => ({ ...prev, secretKey: e.target.value }))}
                        className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50 pr-12"
                        placeholder="Enter your secret key"
                      />
                      <button
                        type="button"
                        onClick={() => setShowSecrets(prev => ({ ...prev, secretKey: !prev.secretKey }))}
                        className="absolute right-3 top-3 text-slate-400 hover:text-white"
                      >
                        {showSecrets.secretKey ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      id="sandbox"
                      checked={newCredentials.sandbox}
                      onChange={(e) => setNewCredentials(prev => ({ ...prev, sandbox: e.target.checked }))}
                      className="rounded bg-slate-700/50 border-slate-600/50 text-purple-400 focus:ring-purple-400/50"
                    />
                    <label htmlFor="sandbox" className="text-sm text-slate-300">
                      Use sandbox/paper trading environment
                    </label>
                  </div>
                </div>
              )}
              </div>
            </div>

            {/* Modal Footer - Fixed */}
            <div className="flex items-center justify-end space-x-3 p-6 border-t border-slate-700/50 flex-shrink-0">
              <button
                onClick={() => setShowAddModal(false)}
                className="px-6 py-3 text-slate-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleAddBroker}
                disabled={!newCredentials.brokerType || !newCredentials.apiKey || !newCredentials.accountId}
                className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Save className="w-4 h-4" />
                <span>Add Broker</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-2xl max-w-md w-full">
            <div className="p-6">
              <div className="flex items-center space-x-3 mb-4">
                <AlertCircle className="w-6 h-6 text-red-400" />
                <h2 className="text-xl font-bold text-white">Delete Broker</h2>
              </div>
              <p className="text-slate-400 mb-6">
                Are you sure you want to delete this broker connection? This action cannot be undone.
              </p>
              <div className="flex items-center justify-end space-x-3">
                <button
                  onClick={() => setDeleteConfirm(null)}
                  className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleDeleteBroker(deleteConfirm)}
                  className="px-4 py-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors font-medium"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}