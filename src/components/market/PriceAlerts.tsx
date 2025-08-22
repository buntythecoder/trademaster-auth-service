import React, { useState, useEffect } from 'react'
import { Bell, Plus, X, TrendingUp, TrendingDown, Target, Clock, Settings, Trash2 } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface PriceAlert {
  id: string
  symbol: string
  name: string
  currentPrice: number
  targetPrice: number
  condition: 'above' | 'below' | 'crossover'
  status: 'active' | 'triggered' | 'expired'
  createdAt: string
  expiresAt?: string
  triggered?: boolean
  notificationMethod: 'app' | 'email' | 'sms' | 'all'
}

interface PriceAlertsProps {
  height?: number
}

const mockAlerts: PriceAlert[] = [
  {
    id: '1',
    symbol: 'RELIANCE',
    name: 'Reliance Industries',
    currentPrice: 2547.30,
    targetPrice: 2600.00,
    condition: 'above',
    status: 'active',
    createdAt: '2024-01-15T10:30:00Z',
    expiresAt: '2024-01-22T10:30:00Z',
    notificationMethod: 'all'
  },
  {
    id: '2',
    symbol: 'TCS',
    name: 'Tata Consultancy Services',
    currentPrice: 3642.80,
    targetPrice: 3500.00,
    condition: 'below',
    status: 'active',
    createdAt: '2024-01-14T15:45:00Z',
    expiresAt: '2024-01-28T15:45:00Z',
    notificationMethod: 'app'
  },
  {
    id: '3',
    symbol: 'HDFCBANK',
    name: 'HDFC Bank',
    currentPrice: 1567.25,
    targetPrice: 1580.00,
    condition: 'above',
    status: 'triggered',
    createdAt: '2024-01-13T09:15:00Z',
    triggered: true,
    notificationMethod: 'email'
  }
]

export function PriceAlerts({ height = 500 }: PriceAlertsProps) {
  const [alerts, setAlerts] = useState<PriceAlert[]>(mockAlerts)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [filter, setFilter] = useState<'all' | 'active' | 'triggered' | 'expired'>('all')
  const [newAlert, setNewAlert] = useState({
    symbol: '',
    targetPrice: '',
    condition: 'above' as const,
    expiresIn: '7',
    notificationMethod: 'all' as const
  })
  const { success, error, info } = useToast()

  useEffect(() => {
    // Simulate real-time price updates and alert checking
    const interval = setInterval(() => {
      setAlerts(prevAlerts => 
        prevAlerts.map(alert => {
          if (alert.status === 'triggered') return alert
          
          // Simulate price movement
          const priceChange = (Math.random() - 0.5) * 20
          const newPrice = Math.max(0, alert.currentPrice + priceChange)
          
          // Check if alert should trigger
          const shouldTrigger = 
            (alert.condition === 'above' && newPrice >= alert.targetPrice) ||
            (alert.condition === 'below' && newPrice <= alert.targetPrice)
          
          if (shouldTrigger && !alert.triggered) {
            success(
              `Price Alert Triggered!`, 
              `${alert.symbol} reached ₹${alert.targetPrice.toFixed(2)}`
            )
            return {
              ...alert,
              currentPrice: newPrice,
              status: 'triggered' as const,
              triggered: true
            }
          }
          
          return {
            ...alert,
            currentPrice: newPrice
          }
        })
      )
    }, 5000)

    return () => clearInterval(interval)
  }, [success])

  const filteredAlerts = alerts.filter(alert => 
    filter === 'all' || alert.status === filter
  )

  const handleCreateAlert = () => {
    if (!newAlert.symbol || !newAlert.targetPrice) {
      error('Missing Information', 'Please enter symbol and target price')
      return
    }

    const alert: PriceAlert = {
      id: Date.now().toString(),
      symbol: newAlert.symbol.toUpperCase(),
      name: `${newAlert.symbol.toUpperCase()} Alert`,
      currentPrice: Math.random() * 3000 + 1000, // Mock current price
      targetPrice: parseFloat(newAlert.targetPrice),
      condition: newAlert.condition,
      status: 'active',
      createdAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + parseInt(newAlert.expiresIn) * 24 * 60 * 60 * 1000).toISOString(),
      notificationMethod: newAlert.notificationMethod
    }

    setAlerts(prev => [alert, ...prev])
    setNewAlert({
      symbol: '',
      targetPrice: '',
      condition: 'above',
      expiresIn: '7',
      notificationMethod: 'all'
    })
    setShowCreateForm(false)
    success('Alert Created', `Price alert set for ${alert.symbol}`)
  }

  const handleDeleteAlert = (alertId: string) => {
    setAlerts(prev => prev.filter(alert => alert.id !== alertId))
    info('Alert Deleted', 'Price alert has been removed')
  }

  const getConditionIcon = (condition: string) => {
    switch (condition) {
      case 'above': return TrendingUp
      case 'below': return TrendingDown
      case 'crossover': return Target
      default: return Target
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'text-green-400'
      case 'triggered': return 'text-yellow-400'
      case 'expired': return 'text-slate-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-500/20'
      case 'triggered': return 'bg-yellow-500/20'
      case 'expired': return 'bg-slate-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Bell className="w-5 h-5 mr-2 text-cyan-400" />
            Price Alerts
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            {alerts.filter(a => a.status === 'active').length} active alerts
          </p>
        </div>
        <button
          onClick={() => setShowCreateForm(true)}
          className="cyber-button px-4 py-2 text-sm rounded-xl flex items-center space-x-2"
        >
          <Plus className="w-4 h-4" />
          <span>New Alert</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex space-x-2 mb-6">
        {(['all', 'active', 'triggered', 'expired'] as const).map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={`px-3 py-1.5 text-sm rounded-xl transition-colors ${
              filter === status
                ? 'bg-purple-500/20 text-purple-400'
                : 'text-slate-400 hover:text-white hover:bg-slate-700/50'
            }`}
          >
            {status.charAt(0).toUpperCase() + status.slice(1)}
            {status !== 'all' && (
              <span className="ml-1 text-xs opacity-70">
                ({alerts.filter(a => a.status === status).length})
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Create Alert Form */}
      {showCreateForm && (
        <div className="mb-6 p-4 rounded-xl bg-slate-800/30 border border-purple-500/30">
          <div className="flex items-center justify-between mb-4">
            <h4 className="font-semibold text-white">Create Price Alert</h4>
            <button
              onClick={() => setShowCreateForm(false)}
              className="p-1 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
          
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Symbol</label>
              <input
                type="text"
                value={newAlert.symbol}
                onChange={(e) => setNewAlert(prev => ({ ...prev, symbol: e.target.value.toUpperCase() }))}
                placeholder="RELIANCE"
                className="cyber-input w-full py-2 rounded-xl"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Target Price</label>
              <input
                type="number"
                value={newAlert.targetPrice}
                onChange={(e) => setNewAlert(prev => ({ ...prev, targetPrice: e.target.value }))}
                placeholder="2600.00"
                className="cyber-input w-full py-2 rounded-xl"
                step="0.01"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Condition</label>
              <select
                value={newAlert.condition}
                onChange={(e) => setNewAlert(prev => ({ ...prev, condition: e.target.value as any }))}
                className="cyber-input w-full py-2 rounded-xl"
              >
                <option value="above">Price goes above</option>
                <option value="below">Price goes below</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">Expires In</label>
              <select
                value={newAlert.expiresIn}
                onChange={(e) => setNewAlert(prev => ({ ...prev, expiresIn: e.target.value }))}
                className="cyber-input w-full py-2 rounded-xl"
              >
                <option value="1">1 day</option>
                <option value="7">1 week</option>
                <option value="30">1 month</option>
                <option value="90">3 months</option>
              </select>
            </div>
            
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-300 mb-2">Notification Method</label>
              <select
                value={newAlert.notificationMethod}
                onChange={(e) => setNewAlert(prev => ({ ...prev, notificationMethod: e.target.value as any }))}
                className="cyber-input w-full py-2 rounded-xl"
              >
                <option value="all">All methods</option>
                <option value="app">In-app only</option>
                <option value="email">Email only</option>
                <option value="sms">SMS only</option>
              </select>
            </div>
          </div>
          
          <div className="flex space-x-3 mt-4">
            <button
              onClick={handleCreateAlert}
              className="flex-1 cyber-button py-2 rounded-xl text-sm"
            >
              Create Alert
            </button>
            <button
              onClick={() => setShowCreateForm(false)}
              className="flex-1 glass-card text-slate-400 hover:text-white py-2 rounded-xl text-sm"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Alerts List */}
      <div className="space-y-3 max-h-80 overflow-y-auto custom-scrollbar">
        {filteredAlerts.length === 0 ? (
          <div className="text-center py-8">
            <Bell className="w-12 h-12 text-slate-500 mx-auto mb-4" />
            <p className="text-slate-400">No alerts found</p>
            <p className="text-slate-500 text-sm">Create your first price alert to get started</p>
          </div>
        ) : (
          filteredAlerts.map((alert) => {
            const ConditionIcon = getConditionIcon(alert.condition)
            const progress = alert.condition === 'above' 
              ? (alert.currentPrice / alert.targetPrice) * 100
              : (alert.targetPrice / alert.currentPrice) * 100

            return (
              <div key={alert.id} className="p-4 rounded-xl bg-slate-800/30 border border-slate-700/50">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <div className={`p-1.5 rounded-lg ${getStatusBg(alert.status)}`}>
                        <ConditionIcon className={`w-4 h-4 ${
                          alert.condition === 'above' ? 'text-green-400' : 'text-red-400'
                        }`} />
                      </div>
                      <div>
                        <div className="font-semibold text-white">{alert.symbol}</div>
                        <div className="text-xs text-slate-400">{alert.name}</div>
                      </div>
                      <div className={`px-2 py-1 rounded-lg text-xs font-medium ${
                        getStatusBg(alert.status)} ${getStatusColor(alert.status)
                      }`}>
                        {alert.status}
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <div className="text-slate-400">Current</div>
                        <div className="font-semibold text-white">₹{alert.currentPrice.toFixed(2)}</div>
                      </div>
                      <div>
                        <div className="text-slate-400">Target</div>
                        <div className="font-semibold text-white">₹{alert.targetPrice.toFixed(2)}</div>
                      </div>
                      <div>
                        <div className="text-slate-400">Progress</div>
                        <div className="font-semibold text-white">{Math.min(100, progress).toFixed(1)}%</div>
                      </div>
                    </div>
                    
                    {/* Progress Bar */}
                    <div className="mt-3 mb-2">
                      <div className="w-full bg-slate-700/30 rounded-full h-1.5">
                        <div 
                          className={`h-1.5 rounded-full transition-all duration-500 ${
                            alert.status === 'triggered' ? 'bg-yellow-400' : 
                            alert.condition === 'above' ? 'bg-green-400' : 'bg-red-400'
                          }`}
                          style={{ width: `${Math.min(100, progress)}%` }}
                        />
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between text-xs text-slate-400">
                      <div className="flex items-center space-x-1">
                        <Clock className="w-3 h-3" />
                        <span>Created {new Date(alert.createdAt).toLocaleDateString()}</span>
                      </div>
                      {alert.expiresAt && (
                        <div className="flex items-center space-x-1">
                          <span>Expires {new Date(alert.expiresAt).toLocaleDateString()}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex space-x-2 ml-4">
                    <button
                      onClick={() => info('Settings', 'Alert settings coming soon')}
                      className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                    >
                      <Settings className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteAlert(alert.id)}
                      className="p-2 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}