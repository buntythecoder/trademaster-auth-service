import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Shield,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Target,
  Activity,
  Settings,
  Eye,
  Bell,
  CheckCircle,
  XCircle,
  Clock,
  BarChart3,
  PieChart,
  Zap,
  DollarSign,
  Percent,
  AlertCircle
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
}

interface RiskAlert {
  id: string
  type: 'limit_breach' | 'concentration' | 'volatility' | 'correlation' | 'compliance'
  severity: 'low' | 'medium' | 'high' | 'critical'
  message: string
  symbol?: string
  timestamp: Date
  acknowledged: boolean
  resolved: boolean
}

interface ComplianceCheck {
  id: string
  rule: string
  status: 'compliant' | 'violation' | 'warning'
  description: string
  lastChecked: Date
  autoRemediation: boolean
}

const mockRiskLimits: RiskLimit[] = [
  {
    id: '1',
    name: 'Daily P&L Limit',
    type: 'daily',
    limit: 100000,
    current: 15000,
    utilization: 15,
    status: 'safe',
    enabled: true
  },
  {
    id: '2', 
    name: 'Position Concentration',
    type: 'concentration',
    limit: 25,
    current: 32,
    utilization: 128,
    status: 'danger',
    enabled: true
  },
  {
    id: '3',
    name: 'Portfolio VAR (95%)',
    type: 'portfolio',
    limit: 50000,
    current: 38000,
    utilization: 76,
    status: 'warning',
    enabled: true
  },
  {
    id: '4',
    name: 'Max Position Size',
    type: 'position',
    limit: 1000000,
    current: 750000,
    utilization: 75,
    status: 'warning',
    enabled: true
  }
]

const mockRiskAlerts: RiskAlert[] = [
  {
    id: '1',
    type: 'concentration',
    severity: 'high',
    message: 'RELIANCE position exceeds concentration limit (32% vs 25% max)',
    symbol: 'RELIANCE',
    timestamp: new Date(Date.now() - 300000),
    acknowledged: false,
    resolved: false
  },
  {
    id: '2',
    type: 'volatility',
    severity: 'medium',
    message: 'Portfolio volatility increased to 18.5% (above 15% threshold)',
    timestamp: new Date(Date.now() - 900000),
    acknowledged: true,
    resolved: false
  },
  {
    id: '3',
    type: 'correlation',
    severity: 'low',
    message: 'High correlation detected between HDFC and ICICI positions (0.85)',
    timestamp: new Date(Date.now() - 1800000),
    acknowledged: true,
    resolved: true
  }
]

const mockComplianceChecks: ComplianceCheck[] = [
  {
    id: '1',
    rule: 'Position Size Limits',
    status: 'compliant',
    description: 'All positions within regulatory size limits',
    lastChecked: new Date(Date.now() - 300000),
    autoRemediation: false
  },
  {
    id: '2',
    rule: 'Margin Requirements',
    status: 'compliant',
    description: 'Sufficient margin maintained across all positions',
    lastChecked: new Date(Date.now() - 600000),
    autoRemediation: true
  },
  {
    id: '3',
    rule: 'Sector Concentration',
    status: 'warning',
    description: 'Financial sector exposure at 45% (max 50%)',
    lastChecked: new Date(Date.now() - 180000),
    autoRemediation: false
  },
  {
    id: '4',
    rule: 'Trading Hours Compliance',
    status: 'violation',
    description: 'After-hours trading detected without proper authorization',
    lastChecked: new Date(Date.now() - 120000),
    autoRemediation: false
  }
]

export function RiskManagementDashboard() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'limits' | 'alerts' | 'monitoring' | 'compliance'>('limits')
  const [riskLimits, setRiskLimits] = useState(mockRiskLimits)
  const [riskAlerts, setRiskAlerts] = useState(mockRiskAlerts)
  const [complianceChecks] = useState(mockComplianceChecks)
  const [showLimitConfig, setShowLimitConfig] = useState(false)
  const [selectedLimit, setSelectedLimit] = useState<RiskLimit | null>(null)

  const overallRiskScore = Math.round(
    riskLimits.reduce((sum, limit) => sum + Math.min(limit.utilization, 100), 0) / riskLimits.length
  )

  const activeAlerts = riskAlerts.filter(alert => !alert.resolved)
  const criticalAlerts = activeAlerts.filter(alert => alert.severity === 'critical')
  const highAlerts = activeAlerts.filter(alert => alert.severity === 'high')

  const complianceScore = Math.round(
    (complianceChecks.filter(check => check.status === 'compliant').length / complianceChecks.length) * 100
  )

  const acknowledgeAlert = (alertId: string) => {
    setRiskAlerts(prev => 
      prev.map(alert => 
        alert.id === alertId ? { ...alert, acknowledged: true } : alert
      )
    )
  }

  const resolveAlert = (alertId: string) => {
    setRiskAlerts(prev => 
      prev.map(alert => 
        alert.id === alertId ? { ...alert, resolved: true, acknowledged: true } : alert
      )
    )
  }

  const toggleLimit = (limitId: string) => {
    setRiskLimits(prev => 
      prev.map(limit => 
        limit.id === limitId ? { ...limit, enabled: !limit.enabled } : limit
      )
    )
  }

  const getRiskColor = (utilization: number, status: string) => {
    if (status === 'danger' || utilization > 100) return 'text-red-400'
    if (status === 'warning' || utilization > 75) return 'text-yellow-400'
    return 'text-green-400'
  }

  const getRiskBgColor = (utilization: number, status: string) => {
    if (status === 'danger' || utilization > 100) return 'bg-red-500/20'
    if (status === 'warning' || utilization > 75) return 'bg-yellow-500/20'
    return 'bg-green-500/20'
  }

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'critical': return 'text-red-400'
      case 'high': return 'text-orange-400'
      case 'medium': return 'text-yellow-400'
      default: return 'text-blue-400'
    }
  }

  const getSeverityBg = (severity: string) => {
    switch (severity) {
      case 'critical': return 'bg-red-500/20'
      case 'high': return 'bg-orange-500/20'
      case 'medium': return 'bg-yellow-500/20'
      default: return 'bg-blue-500/20'
    }
  }

  const getComplianceColor = (status: string) => {
    switch (status) {
      case 'compliant': return 'text-green-400'
      case 'violation': return 'text-red-400'
      default: return 'text-yellow-400'
    }
  }

  const getComplianceBg = (status: string) => {
    switch (status) {
      case 'compliant': return 'bg-green-500/20'
      case 'violation': return 'bg-red-500/20'
      default: return 'bg-yellow-500/20'
    }
  }

  useEffect(() => {
    // Simulate real-time risk monitoring updates
    const interval = setInterval(() => {
      setRiskLimits(prev => 
        prev.map(limit => ({
          ...limit,
          current: limit.current + (Math.random() - 0.5) * limit.limit * 0.1,
          utilization: Math.max(0, limit.utilization + (Math.random() - 0.5) * 10)
        }))
      )
    }, 30000)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold gradient-text mb-2">Risk Management</h1>
          <p className="text-slate-400">Monitor and manage portfolio risk exposure</p>
        </div>
        <div className="flex items-center space-x-4">
          <button
            onClick={() => setShowLimitConfig(true)}
            className="flex items-center space-x-2 cyber-button px-4 py-2 rounded-xl"
          >
            <Settings className="w-4 h-4" />
            <span>Configure Limits</span>
          </button>
          <button
            onClick={() => navigate('/portfolio')}
            className="flex items-center space-x-2 glass-card px-4 py-2 rounded-xl text-slate-400 hover:text-white transition-colors"
          >
            <Eye className="w-4 h-4" />
            <span>Portfolio View</span>
          </button>
        </div>
      </div>

      {/* Risk Overview Cards */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className={`p-3 rounded-xl ${getRiskBgColor(overallRiskScore, overallRiskScore > 75 ? 'warning' : 'safe')}`}>
              <Shield className={`w-6 h-6 ${getRiskColor(overallRiskScore, overallRiskScore > 75 ? 'warning' : 'safe')}`} />
            </div>
            <div className="text-right">
              <div className={`text-2xl font-bold ${getRiskColor(overallRiskScore, overallRiskScore > 75 ? 'warning' : 'safe')}`}>
                {overallRiskScore}%
              </div>
              <div className="text-sm text-slate-400">Risk Score</div>
            </div>
          </div>
          <h3 className="text-white font-semibold mb-1">Overall Risk</h3>
          <p className="text-slate-400 text-sm">Current risk utilization</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className={`p-3 rounded-xl ${activeAlerts.length > 5 ? 'bg-red-500/20' : activeAlerts.length > 2 ? 'bg-yellow-500/20' : 'bg-green-500/20'}`}>
              <Bell className={`w-6 h-6 ${activeAlerts.length > 5 ? 'text-red-400' : activeAlerts.length > 2 ? 'text-yellow-400' : 'text-green-400'}`} />
            </div>
            <div className="text-right">
              <div className={`text-2xl font-bold ${activeAlerts.length > 5 ? 'text-red-400' : activeAlerts.length > 2 ? 'text-yellow-400' : 'text-green-400'}`}>
                {activeAlerts.length}
              </div>
              <div className="text-sm text-slate-400">Active Alerts</div>
            </div>
          </div>
          <h3 className="text-white font-semibold mb-1">Risk Alerts</h3>
          <p className="text-slate-400 text-sm">
            {criticalAlerts.length} critical, {highAlerts.length} high priority
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className={`p-3 rounded-xl ${getComplianceBg(complianceScore > 90 ? 'compliant' : complianceScore > 70 ? 'warning' : 'violation')}`}>
              <CheckCircle className={`w-6 h-6 ${getComplianceColor(complianceScore > 90 ? 'compliant' : complianceScore > 70 ? 'warning' : 'violation')}`} />
            </div>
            <div className="text-right">
              <div className={`text-2xl font-bold ${getComplianceColor(complianceScore > 90 ? 'compliant' : complianceScore > 70 ? 'warning' : 'violation')}`}>
                {complianceScore}%
              </div>
              <div className="text-sm text-slate-400">Compliance</div>
            </div>
          </div>
          <h3 className="text-white font-semibold mb-1">Compliance Score</h3>
          <p className="text-slate-400 text-sm">Regulatory compliance status</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-purple-500/20">
              <Activity className="w-6 h-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-purple-400">
                Real-time
              </div>
              <div className="text-sm text-slate-400">Monitoring</div>
            </div>
          </div>
          <h3 className="text-white font-semibold mb-1">Live Tracking</h3>
          <p className="text-slate-400 text-sm">Continuous risk assessment</p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex space-x-1 glass-card p-2 rounded-2xl w-fit">
        {[
          { id: 'limits', label: 'Risk Limits', icon: Target },
          { id: 'alerts', label: 'Risk Alerts', icon: Bell },
          { id: 'monitoring', label: 'Live Monitor', icon: Activity },
          { id: 'compliance', label: 'Compliance', icon: Shield }
        ].map(tab => {
          const Icon = tab.icon
          const isActive = activeTab === tab.id
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex items-center space-x-2 px-6 py-3 rounded-xl transition-all ${
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
      </div>

      {/* Tab Content */}
      {activeTab === 'limits' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6 flex items-center">
              <Target className="w-5 h-5 mr-2 text-purple-400" />
              Risk Limit Configuration
            </h3>
            <div className="space-y-4">
              {riskLimits.map(limit => (
                <div key={limit.id} className={`p-4 rounded-xl border ${getRiskBgColor(limit.utilization, limit.status)} border-opacity-50`}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center space-x-3">
                      <h4 className="font-semibold text-white">{limit.name}</h4>
                      <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                        limit.enabled ? 'bg-green-500/20 text-green-400' : 'bg-slate-500/20 text-slate-400'
                      }`}>
                        {limit.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </div>
                    <button
                      onClick={() => toggleLimit(limit.id)}
                      className={`p-2 rounded-lg transition-colors ${
                        limit.enabled ? 'bg-green-500/20 text-green-400' : 'bg-slate-700/50 text-slate-400'
                      }`}
                    >
                      {limit.enabled ? <CheckCircle className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
                    </button>
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-400">Current: {limit.type === 'daily' ? '₹' : ''}{limit.current.toLocaleString()}{limit.type === 'concentration' || limit.type === 'portfolio' ? '%' : ''}</span>
                      <span className="text-slate-400">Limit: {limit.type === 'daily' ? '₹' : ''}{limit.limit.toLocaleString()}{limit.type === 'concentration' || limit.type === 'portfolio' ? '%' : ''}</span>
                    </div>
                    <div className="w-full bg-slate-700/50 rounded-full h-2">
                      <div 
                        className={`h-2 rounded-full transition-all ${
                          limit.utilization > 100 ? 'bg-red-500' :
                          limit.utilization > 75 ? 'bg-yellow-500' : 'bg-green-500'
                        }`}
                        style={{ width: `${Math.min(limit.utilization, 100)}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className={getRiskColor(limit.utilization, limit.status)}>
                        {limit.utilization.toFixed(1)}% utilized
                      </span>
                      <span className={`font-semibold ${getRiskColor(limit.utilization, limit.status)}`}>
                        {limit.status.toUpperCase()}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6">Limit Types</h3>
            <div className="space-y-4">
              {[
                {
                  type: 'Daily P&L',
                  description: 'Maximum daily profit/loss threshold',
                  icon: DollarSign,
                  color: 'text-green-400'
                },
                {
                  type: 'Position Size',
                  description: 'Maximum individual position value',
                  icon: BarChart3,
                  color: 'text-blue-400'
                },
                {
                  type: 'Concentration',
                  description: 'Maximum percentage in single position',
                  icon: PieChart,
                  color: 'text-orange-400'
                },
                {
                  type: 'Portfolio VAR',
                  description: 'Value at Risk calculation',
                  icon: TrendingDown,
                  color: 'text-red-400'
                }
              ].map((item, index) => {
                const Icon = item.icon
                return (
                  <div key={index} className="flex items-center space-x-4 p-3 rounded-xl bg-slate-800/30">
                    <Icon className={`w-5 h-5 ${item.color}`} />
                    <div>
                      <h4 className="font-semibold text-white">{item.type}</h4>
                      <p className="text-sm text-slate-400">{item.description}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'alerts' && (
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Bell className="w-5 h-5 mr-2 text-orange-400" />
            Risk Alerts Management
          </h3>
          <div className="space-y-4">
            {riskAlerts.map(alert => (
              <div key={alert.id} className={`p-4 rounded-xl border border-opacity-50 ${
                alert.resolved ? 'bg-slate-800/30 border-slate-600/50' : 
                `${getSeverityBg(alert.severity)} border-${alert.severity === 'critical' ? 'red' : alert.severity === 'high' ? 'orange' : alert.severity === 'medium' ? 'yellow' : 'blue'}-500/50`
              }`}>
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className={`p-2 rounded-lg ${getSeverityBg(alert.severity)}`}>
                      {alert.severity === 'critical' ? 
                        <AlertTriangle className={`w-4 h-4 ${getSeverityColor(alert.severity)}`} /> :
                        <AlertCircle className={`w-4 h-4 ${getSeverityColor(alert.severity)}`} />
                      }
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-2">
                        <span className={`text-xs px-2 py-1 rounded-full font-medium ${getSeverityBg(alert.severity)} ${getSeverityColor(alert.severity)}`}>
                          {alert.severity.toUpperCase()}
                        </span>
                        <span className="text-xs text-slate-400">
                          {alert.type.replace('_', ' ').toUpperCase()}
                        </span>
                        {alert.symbol && (
                          <span className="text-xs px-2 py-1 bg-blue-500/20 text-blue-400 rounded-full font-mono">
                            {alert.symbol}
                          </span>
                        )}
                      </div>
                      <p className={`text-sm ${alert.resolved ? 'text-slate-400' : 'text-white'} mb-2`}>
                        {alert.message}
                      </p>
                      <div className="flex items-center space-x-4 text-xs text-slate-400">
                        <span className="flex items-center space-x-1">
                          <Clock className="w-3 h-3" />
                          <span>{new Date(alert.timestamp).toLocaleTimeString()}</span>
                        </span>
                        {alert.acknowledged && (
                          <span className="text-green-400">✓ Acknowledged</span>
                        )}
                        {alert.resolved && (
                          <span className="text-blue-400">✓ Resolved</span>
                        )}
                      </div>
                    </div>
                  </div>
                  
                  {!alert.resolved && (
                    <div className="flex space-x-2">
                      {!alert.acknowledged && (
                        <button
                          onClick={() => acknowledgeAlert(alert.id)}
                          className="px-3 py-1 text-xs bg-yellow-500/20 text-yellow-400 rounded-lg hover:bg-yellow-500/30 transition-colors"
                        >
                          Acknowledge
                        </button>
                      )}
                      <button
                        onClick={() => resolveAlert(alert.id)}
                        className="px-3 py-1 text-xs bg-green-500/20 text-green-400 rounded-lg hover:bg-green-500/30 transition-colors"
                      >
                        Resolve
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'monitoring' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6 flex items-center">
              <Activity className="w-5 h-5 mr-2 text-cyan-400" />
              Real-time Risk Metrics
            </h3>
            <div className="space-y-4">
              {[
                { label: 'Portfolio Beta', value: '1.23', change: '+0.05', color: 'text-red-400' },
                { label: 'Sharpe Ratio', value: '2.14', change: '+0.12', color: 'text-green-400' },
                { label: 'Max Drawdown', value: '8.5%', change: '-1.2%', color: 'text-green-400' },
                { label: 'Volatility (30D)', value: '18.7%', change: '+2.3%', color: 'text-red-400' },
                { label: 'Correlation to Market', value: '0.78', change: '+0.03', color: 'text-yellow-400' }
              ].map((metric, index) => (
                <div key={index} className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
                  <span className="text-slate-400">{metric.label}</span>
                  <div className="text-right">
                    <div className="text-white font-semibold">{metric.value}</div>
                    <div className={`text-xs ${metric.color}`}>{metric.change}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card rounded-2xl p-6">
            <h3 className="text-xl font-bold text-white mb-6 flex items-center">
              <Zap className="w-5 h-5 mr-2 text-purple-400" />
              Live Risk Monitoring
            </h3>
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-green-500/10 border border-green-500/30">
                <div className="flex items-center space-x-2 mb-2">
                  <CheckCircle className="w-4 h-4 text-green-400" />
                  <span className="text-green-400 font-semibold">Position Monitoring</span>
                </div>
                <p className="text-sm text-slate-300">All positions within acceptable risk parameters</p>
              </div>

              <div className="p-4 rounded-xl bg-yellow-500/10 border border-yellow-500/30">
                <div className="flex items-center space-x-2 mb-2">
                  <AlertTriangle className="w-4 h-4 text-yellow-400" />
                  <span className="text-yellow-400 font-semibold">Market Volatility</span>
                </div>
                <p className="text-sm text-slate-300">Elevated volatility detected in technology sector</p>
              </div>

              <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/30">
                <div className="flex items-center space-x-2 mb-2">
                  <Activity className="w-4 h-4 text-blue-400" />
                  <span className="text-blue-400 font-semibold">Correlation Analysis</span>
                </div>
                <p className="text-sm text-slate-300">Portfolio correlation within normal ranges</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'compliance' && (
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-xl font-bold text-white mb-6 flex items-center">
            <Shield className="w-5 h-5 mr-2 text-green-400" />
            Compliance Monitoring
          </h3>
          <div className="space-y-4">
            {complianceChecks.map(check => (
              <div key={check.id} className={`p-4 rounded-xl border border-opacity-50 ${getComplianceBg(check.status)}`}>
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className={`p-2 rounded-lg ${getComplianceBg(check.status)}`}>
                      {check.status === 'compliant' ? 
                        <CheckCircle className={`w-4 h-4 ${getComplianceColor(check.status)}`} /> :
                        check.status === 'violation' ?
                        <XCircle className={`w-4 h-4 ${getComplianceColor(check.status)}`} /> :
                        <AlertTriangle className={`w-4 h-4 ${getComplianceColor(check.status)}`} />
                      }
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-2">
                        <h4 className="font-semibold text-white">{check.rule}</h4>
                        <span className={`text-xs px-2 py-1 rounded-full font-medium ${getComplianceBg(check.status)} ${getComplianceColor(check.status)}`}>
                          {check.status.toUpperCase()}
                        </span>
                        {check.autoRemediation && (
                          <span className="text-xs px-2 py-1 bg-blue-500/20 text-blue-400 rounded-full">
                            Auto-Fix
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-slate-300 mb-2">{check.description}</p>
                      <div className="flex items-center space-x-2 text-xs text-slate-400">
                        <Clock className="w-3 h-3" />
                        <span>Last checked: {new Date(check.lastChecked).toLocaleTimeString()}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}