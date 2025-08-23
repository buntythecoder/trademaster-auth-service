import React, { useState } from 'react'
import {
  X,
  Settings,
  Target,
  DollarSign,
  Percent,
  BarChart3,
  AlertTriangle,
  Save,
  RotateCcw,
  Plus,
  Trash2
} from 'lucide-react'

interface RiskLimit {
  id: string
  name: string
  type: 'position' | 'portfolio' | 'daily' | 'concentration'
  limit: number
  current: number
  utilization: number
  status: 'safe' | 'warning' | 'danger'
  enabled: boolean
  warningThreshold?: number
  dangerThreshold?: number
  autoEnforce?: boolean
}

interface RiskConfigurationModalProps {
  isOpen: boolean
  onClose: () => void
  limits: RiskLimit[]
  onUpdateLimits: (limits: RiskLimit[]) => void
}

const limitTemplates = [
  {
    name: 'Conservative Daily P&L',
    type: 'daily' as const,
    limit: 50000,
    warningThreshold: 70,
    dangerThreshold: 90
  },
  {
    name: 'Aggressive Daily P&L',
    type: 'daily' as const,
    limit: 200000,
    warningThreshold: 80,
    dangerThreshold: 95
  },
  {
    name: 'Single Position Limit',
    type: 'position' as const,
    limit: 500000,
    warningThreshold: 75,
    dangerThreshold: 90
  },
  {
    name: 'Sector Concentration',
    type: 'concentration' as const,
    limit: 30,
    warningThreshold: 80,
    dangerThreshold: 95
  }
]

export function RiskConfigurationModal({ 
  isOpen, 
  onClose, 
  limits, 
  onUpdateLimits 
}: RiskConfigurationModalProps) {
  const [editingLimits, setEditingLimits] = useState<RiskLimit[]>(limits)
  const [activeTab, setActiveTab] = useState<'limits' | 'templates' | 'advanced'>('limits')
  const [hasChanges, setHasChanges] = useState(false)

  if (!isOpen) return null

  const updateLimit = (id: string, updates: Partial<RiskLimit>) => {
    setEditingLimits(prev => 
      prev.map(limit => 
        limit.id === id ? { ...limit, ...updates } : limit
      )
    )
    setHasChanges(true)
  }

  const addNewLimit = (template?: typeof limitTemplates[0]) => {
    const newLimit: RiskLimit = {
      id: Date.now().toString(),
      name: template?.name || 'New Risk Limit',
      type: template?.type || 'daily',
      limit: template?.limit || 100000,
      current: 0,
      utilization: 0,
      status: 'safe',
      enabled: true,
      warningThreshold: template?.warningThreshold || 75,
      dangerThreshold: template?.dangerThreshold || 90,
      autoEnforce: false
    }
    setEditingLimits(prev => [...prev, newLimit])
    setHasChanges(true)
  }

  const removeLimit = (id: string) => {
    setEditingLimits(prev => prev.filter(limit => limit.id !== id))
    setHasChanges(true)
  }

  const handleSave = () => {
    onUpdateLimits(editingLimits)
    setHasChanges(false)
    onClose()
  }

  const handleReset = () => {
    setEditingLimits(limits)
    setHasChanges(false)
  }

  const getLimitIcon = (type: string) => {
    switch (type) {
      case 'daily': return DollarSign
      case 'position': return BarChart3
      case 'concentration': return Percent
      default: return Target
    }
  }

  const getLimitUnit = (type: string) => {
    switch (type) {
      case 'daily': return '₹'
      case 'position': return '₹'
      case 'concentration': return '%'
      case 'portfolio': return '%'
      default: return ''
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="glass-card rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
          <div className="flex items-center space-x-3">
            <Settings className="w-6 h-6 text-purple-400" />
            <div>
              <h2 className="text-xl font-bold text-white">Risk Configuration</h2>
              <p className="text-slate-400 text-sm">Configure and manage risk limits</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-slate-700/50 rounded-xl transition-colors"
          >
            <X className="w-5 h-5 text-slate-400" />
          </button>
        </div>

        {/* Content */}
        <div className="flex h-[calc(90vh-140px)]">
          {/* Sidebar */}
          <div className="w-64 border-r border-slate-700/50 p-4">
            <nav className="space-y-2">
              {[
                { id: 'limits', label: 'Risk Limits', icon: Target },
                { id: 'templates', label: 'Templates', icon: BarChart3 },
                { id: 'advanced', label: 'Advanced', icon: Settings }
              ].map(tab => {
                const Icon = tab.icon
                const isActive = activeTab === tab.id
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all ${
                      isActive
                        ? 'bg-purple-500/20 text-purple-400'
                        : 'text-slate-400 hover:text-white hover:bg-slate-700/30'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span className="font-medium">{tab.label}</span>
                  </button>
                )
              })}
            </nav>
          </div>

          {/* Main Content */}
          <div className="flex-1 p-6 overflow-y-auto">
            {activeTab === 'limits' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-white">Active Risk Limits</h3>
                  <button
                    onClick={() => addNewLimit()}
                    className="flex items-center space-x-2 cyber-button px-4 py-2 rounded-xl"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Add Limit</span>
                  </button>
                </div>

                <div className="space-y-4">
                  {editingLimits.map(limit => {
                    const Icon = getLimitIcon(limit.type)
                    const unit = getLimitUnit(limit.type)
                    
                    return (
                      <div key={limit.id} className="glass-card p-4 rounded-xl">
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex items-center space-x-3">
                            <Icon className="w-5 h-5 text-purple-400" />
                            <div>
                              <input
                                type="text"
                                value={limit.name}
                                onChange={(e) => updateLimit(limit.id, { name: e.target.value })}
                                className="bg-transparent text-white font-semibold border-none outline-none focus:bg-slate-800/50 px-2 py-1 rounded"
                              />
                              <p className="text-sm text-slate-400 capitalize">{limit.type} limit</p>
                            </div>
                          </div>
                          <div className="flex items-center space-x-2">
                            <label className="flex items-center space-x-2 text-sm">
                              <input
                                type="checkbox"
                                checked={limit.enabled}
                                onChange={(e) => updateLimit(limit.id, { enabled: e.target.checked })}
                                className="rounded bg-slate-700/50 border-slate-600/50 text-purple-400 focus:ring-purple-400/50"
                              />
                              <span className="text-slate-300">Enabled</span>
                            </label>
                            <button
                              onClick={() => removeLimit(limit.id)}
                              className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                          <div>
                            <label className="block text-sm text-slate-400 mb-2">Limit Value</label>
                            <div className="relative">
                              <input
                                type="number"
                                value={limit.limit}
                                onChange={(e) => updateLimit(limit.id, { limit: Number(e.target.value) })}
                                className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50"
                              />
                              {unit && (
                                <span className="absolute right-3 top-2 text-slate-400">{unit}</span>
                              )}
                            </div>
                          </div>

                          <div>
                            <label className="block text-sm text-slate-400 mb-2">Warning Threshold (%)</label>
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={limit.warningThreshold || 75}
                              onChange={(e) => updateLimit(limit.id, { warningThreshold: Number(e.target.value) })}
                              className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white focus:border-yellow-400/50 focus:ring-1 focus:ring-yellow-400/50"
                            />
                          </div>

                          <div>
                            <label className="block text-sm text-slate-400 mb-2">Danger Threshold (%)</label>
                            <input
                              type="number"
                              min="0"
                              max="100"
                              value={limit.dangerThreshold || 90}
                              onChange={(e) => updateLimit(limit.id, { dangerThreshold: Number(e.target.value) })}
                              className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white focus:border-red-400/50 focus:ring-1 focus:ring-red-400/50"
                            />
                          </div>
                        </div>

                        <div className="mt-4">
                          <label className="flex items-center space-x-2 text-sm">
                            <input
                              type="checkbox"
                              checked={limit.autoEnforce || false}
                              onChange={(e) => updateLimit(limit.id, { autoEnforce: e.target.checked })}
                              className="rounded bg-slate-700/50 border-slate-600/50 text-orange-400 focus:ring-orange-400/50"
                            />
                            <span className="text-slate-300">Auto-enforce limit (prevent exceeding trades)</span>
                            <AlertTriangle className="w-4 h-4 text-orange-400" />
                          </label>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {activeTab === 'templates' && (
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-white">Risk Limit Templates</h3>
                <div className="grid gap-4 md:grid-cols-2">
                  {limitTemplates.map((template, index) => {
                    const Icon = getLimitIcon(template.type)
                    const unit = getLimitUnit(template.type)
                    
                    return (
                      <div key={index} className="glass-card p-4 rounded-xl">
                        <div className="flex items-center space-x-3 mb-3">
                          <Icon className="w-5 h-5 text-purple-400" />
                          <div>
                            <h4 className="font-semibold text-white">{template.name}</h4>
                            <p className="text-sm text-slate-400 capitalize">{template.type} limit</p>
                          </div>
                        </div>
                        
                        <div className="space-y-2 mb-4">
                          <div className="flex justify-between text-sm">
                            <span className="text-slate-400">Limit:</span>
                            <span className="text-white">{unit}{template.limit.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-slate-400">Warning:</span>
                            <span className="text-yellow-400">{template.warningThreshold}%</span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-slate-400">Danger:</span>
                            <span className="text-red-400">{template.dangerThreshold}%</span>
                          </div>
                        </div>
                        
                        <button
                          onClick={() => addNewLimit(template)}
                          className="w-full cyber-button py-2 rounded-xl text-sm"
                        >
                          Use Template
                        </button>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {activeTab === 'advanced' && (
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-white">Advanced Risk Settings</h3>
                
                <div className="space-y-4">
                  <div className="glass-card p-4 rounded-xl">
                    <h4 className="font-semibold text-white mb-3">Risk Calculation Settings</h4>
                    <div className="space-y-4">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="block text-sm text-slate-400 mb-2">VAR Confidence Level (%)</label>
                          <select className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white">
                            <option value="95">95%</option>
                            <option value="97.5">97.5%</option>
                            <option value="99">99%</option>
                          </select>
                        </div>
                        
                        <div>
                          <label className="block text-sm text-slate-400 mb-2">Risk Horizon (Days)</label>
                          <select className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white">
                            <option value="1">1 Day</option>
                            <option value="5">5 Days</option>
                            <option value="10">10 Days</option>
                            <option value="22">1 Month</option>
                          </select>
                        </div>
                      </div>
                      
                      <div>
                        <label className="block text-sm text-slate-400 mb-2">Historical Data Period (Days)</label>
                        <input
                          type="number"
                          defaultValue={252}
                          min="30"
                          max="1000"
                          className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="glass-card p-4 rounded-xl">
                    <h4 className="font-semibold text-white mb-3">Alert & Notification Settings</h4>
                    <div className="space-y-3">
                      {[
                        { label: 'Email notifications for limit breaches', checked: true },
                        { label: 'SMS alerts for critical violations', checked: false },
                        { label: 'Real-time dashboard notifications', checked: true },
                        { label: 'Daily risk summary reports', checked: true },
                        { label: 'Auto-pause trading on critical alerts', checked: false }
                      ].map((setting, index) => (
                        <label key={index} className="flex items-center space-x-3">
                          <input
                            type="checkbox"
                            defaultChecked={setting.checked}
                            className="rounded bg-slate-700/50 border-slate-600/50 text-purple-400 focus:ring-purple-400/50"
                          />
                          <span className="text-slate-300">{setting.label}</span>
                        </label>
                      ))}
                    </div>
                  </div>

                  <div className="glass-card p-4 rounded-xl">
                    <h4 className="font-semibold text-white mb-3">Risk Model Settings</h4>
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm text-slate-400 mb-2">Correlation Decay Factor</label>
                        <input
                          type="number"
                          defaultValue={0.94}
                          min="0.5"
                          max="1"
                          step="0.01"
                          className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50"
                        />
                      </div>
                      
                      <div>
                        <label className="block text-sm text-slate-400 mb-2">Volatility Model</label>
                        <select className="w-full bg-slate-800/50 border border-slate-700/50 rounded-lg px-3 py-2 text-white">
                          <option value="ewma">Exponentially Weighted Moving Average</option>
                          <option value="garch">GARCH Model</option>
                          <option value="historical">Historical Volatility</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between p-6 border-t border-slate-700/50">
          <div className="flex items-center space-x-3">
            {hasChanges && (
              <div className="flex items-center space-x-2 text-yellow-400">
                <AlertTriangle className="w-4 h-4" />
                <span className="text-sm">You have unsaved changes</span>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-3">
            <button
              onClick={handleReset}
              disabled={!hasChanges}
              className="flex items-center space-x-2 px-4 py-2 text-slate-400 hover:text-white disabled:text-slate-600 transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Reset</span>
            </button>
            
            <button
              onClick={onClose}
              className="px-6 py-2 text-slate-400 hover:text-white transition-colors"
            >
              Cancel
            </button>
            
            <button
              onClick={handleSave}
              className="flex items-center space-x-2 cyber-button px-6 py-2 rounded-xl"
            >
              <Save className="w-4 h-4" />
              <span>Save Changes</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}