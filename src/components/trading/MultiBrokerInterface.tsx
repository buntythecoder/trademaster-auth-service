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
  Star,
  Lock,
  Target,
  PieChart
} from 'lucide-react'
import { useAuthStore } from '../../stores/auth.store'
import { MultiBrokerService, BrokerConnection, IndianBrokerType } from '../../services/brokerService'
import { MultiBrokerPositionDashboard } from './MultiBrokerPositionDashboard'
import { MultiBrokerPnLDashboard } from './MultiBrokerPnLDashboard'

interface BrokerCredentialsForm {
  brokerType: string
  apiKey: string
  secretKey: string
  accountId: string
  sandbox: boolean
  additionalConfig: Record<string, any>
}

const brokerTypes = [
  { 
    value: 'zerodha', 
    label: 'Zerodha Kite', 
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'currency'],
    icon: '🚀',
    description: 'India\'s largest discount broker with powerful API'
  },
  { 
    value: 'upstox', 
    label: 'Upstox Pro', 
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'currency'],
    icon: '📈',
    description: 'Technology-first broker with low-latency execution'
  },
  { 
    value: 'angel_one', 
    label: 'Angel One', 
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'mutual_funds'],
    icon: '👼',
    description: 'Full-service broker with comprehensive trading solutions'
  },
  { 
    value: 'icici_direct', 
    label: 'ICICI Direct', 
    capabilities: ['stocks', 'futures', 'options', 'mutual_funds', 'bonds'],
    icon: '🏦',
    description: 'Bank-backed broker with institutional-grade platform'
  },
  { 
    value: 'groww', 
    label: 'Groww', 
    capabilities: ['stocks', 'mutual_funds', 'etfs', 'gold'],
    icon: '🌱',
    description: 'Simple and intuitive investing platform'
  },
  { 
    value: 'iifl', 
    label: 'IIFL Securities', 
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'bonds'],
    icon: '💼',
    description: 'Research-driven broker with premium services'
  }
]

export const MultiBrokerInterface: React.FC = () => {
  const { user } = useAuthStore()
  const [brokerConnections, setBrokerConnections] = useState<BrokerConnection[]>([])
  const [positions, setPositions] = useState<BrokerPosition[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<'connections' | 'positions' | 'pnl'>('connections')
  
  // Multi-broker interface is available to all authenticated users
  const [showAddModal, setShowAddModal] = useState(false)
  const [showCredentialsModal, setShowCredentialsModal] = useState(false)
  const [selectedBroker, setSelectedBroker] = useState<BrokerConnection | null>(null)
  const [newCredentials, setNewCredentials] = useState<BrokerCredentialsForm>({
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
      // Initialize mock brokers if none exist
      MultiBrokerService.initializeMockBrokers();
      
      // Get connected brokers from service
      const connections = MultiBrokerService.getConnectedBrokers();
      setBrokerConnections(connections);
      
      // Get aggregated portfolio data which includes positions
      const portfolio = await MultiBrokerService.getAggregatedPortfolio();
      setPositions(portfolio.positions);
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
      // Simulated connection - in real implementation, this would get credentials and connect
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      // Update state to show connected
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
      await MultiBrokerService.disconnectBroker(brokerId);
      const updatedConnections = MultiBrokerService.getConnectedBrokers();
      setBrokerConnections(updatedConnections);
    } catch (error) {
      console.error('Failed to disconnect broker:', error)
    }
  }

  const handleSetDefault = async (brokerId: string) => {
    try {
      const success = MultiBrokerService.setDefaultBroker(brokerId);
      if (success) {
        const updatedConnections = MultiBrokerService.getConnectedBrokers();
        setBrokerConnections(updatedConnections);
      }
    } catch (error) {
      console.error('Failed to set default broker:', error)
    }
  }

  const handleAddBroker = async () => {
    if (!newCredentials.brokerType || !newCredentials.apiKey || !newCredentials.accountId) {
      alert('Please fill in all required fields')
      return
    }

    try {
      const brokerId = `${newCredentials.brokerType}-${Date.now()}`;
      
      const success = await MultiBrokerService.connectBroker({
        brokerId,
        brokerType: newCredentials.brokerType as IndianBrokerType,
        apiKey: newCredentials.apiKey,
        secretKey: newCredentials.secretKey,
        accountId: newCredentials.accountId,
        sandbox: newCredentials.sandbox,
        additionalConfig: newCredentials.additionalConfig
      });

      if (success) {
        const updatedConnections = MultiBrokerService.getConnectedBrokers();
        setBrokerConnections(updatedConnections);
        
        setShowAddModal(false)
        setNewCredentials({
          brokerType: '',
          apiKey: '',
          secretKey: '',
          accountId: '',
          sandbox: true,
          additionalConfig: {}
        })
        
        const brokerInfo = brokerTypes.find(b => b.value === newCredentials.brokerType);
        alert(`${brokerInfo?.label || newCredentials.brokerType} broker connection added successfully!`)
      } else {
        alert('Failed to connect broker. Please check your credentials.')
      }
    } catch (error) {
      console.error('Failed to add broker:', error)
      alert('Failed to add broker connection')
    }
  }

  const handleDeleteBroker = async (brokerId: string) => {
    try {
      await MultiBrokerService.disconnectBroker(brokerId);
      // In a real implementation, you'd also call a delete API endpoint
      const updatedConnections = MultiBrokerService.getConnectedBrokers().filter(b => b.id !== brokerId);
      setBrokerConnections(updatedConnections);
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

  const tabs = [
    {
      id: 'connections',
      label: 'Broker Connections',
      icon: Building2,
      description: 'Manage broker connections'
    },
    {
      id: 'positions',
      label: 'Position Dashboard',
      icon: Target,
      description: 'Aggregated positions across brokers'
    },
    {
      id: 'pnl',
      label: 'P&L Dashboard',
      icon: PieChart,
      description: 'Profit & Loss analysis'
    }
  ]

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
        {activeTab === 'connections' && (
          <button
            onClick={() => setShowAddModal(true)}
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2"
          >
            <Plus className="w-5 h-5" />
            <span>Add Broker</span>
          </button>
        )}
      </div>

      {/* Tab Navigation */}
      <div className="glass-card rounded-2xl p-2">
        <div className="flex space-x-1">
          {tabs.map((tab) => {
            const Icon = tab.icon
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-xl transition-all ${
                  activeTab === tab.id
                    ? 'bg-purple-500/20 text-purple-300'
                    : 'text-slate-400 hover:text-slate-300 hover:bg-slate-700/30'
                }`}
              >
                <Icon className="w-5 h-5" />
                <div className="text-left">
                  <div className="font-semibold text-sm">{tab.label}</div>
                  <div className="text-xs opacity-75">{tab.description}</div>
                </div>
              </button>
            )
          })}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'connections' && (
        <>
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
                          {broker.balance && (
                            <div className="flex items-center space-x-1">
                              <span>₹{broker.balance.availableMargin.toLocaleString('en-IN')} available</span>
                            </div>
                          )}
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
        </>
      )}

      {activeTab === 'positions' && (
        <MultiBrokerPositionDashboard 
          positions={positions}
          brokers={brokerConnections}
          onRefresh={fetchBrokerConnections}
          isLoading={loading}
        />
      )}

      {activeTab === 'pnl' && (
        <MultiBrokerPnLDashboard 
          positions={positions}
          brokers={brokerConnections}
          onRefresh={fetchBrokerConnections}
          isLoading={loading}
        />
      )}

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
                  </div>
                </div>

              {/* Configuration Form */}
              {newCredentials.brokerType && (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold text-white">Connection Configuration</h3>
                  
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