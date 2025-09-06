import React, { useState, useEffect } from 'react'
import { 
  Shield, 
  Key, 
  RefreshCw, 
  CheckCircle, 
  XCircle, 
  AlertTriangle,
  Clock,
  Wifi,
  WifiOff,
  Settings,
  Eye,
  EyeOff,
  ExternalLink,
  Info,
  Zap,
  Activity,
  Lock,
  Timer,
  TrendingUp,
  AlertCircle,
  X
} from 'lucide-react'
import { useAuthStore } from '../../stores/auth.store'

// Enhanced broker configuration with OAuth and API details
interface BrokerConfig {
  id: string
  name: string
  displayName: string
  icon: string
  description: string
  authType: 'oauth' | 'api_key' | 'hybrid'
  apiDocUrl: string
  supportUrl: string
  capabilities: string[]
  rateLimit: {
    ordersPerSecond: number
    requestsPerMinute: number
  }
  oauthConfig?: {
    authUrl: string
    tokenUrl: string
    scope: string[]
    responseType: 'code' | 'token'
  }
  apiConfig?: {
    baseUrl: string
    authHeaders: string[]
    requiredFields: string[]
  }
}

interface ConnectionStatus {
  brokerId: string
  status: 'connected' | 'disconnected' | 'connecting' | 'authenticating' | 'error' | 'token_expired'
  lastConnected?: Date
  tokenExpiry?: Date
  rateLimitUsage: {
    current: number
    limit: number
    resetTime: Date
  }
  healthMetrics: {
    latency: number
    successRate: number
    errorCount: number
    lastError?: string
  }
  sessionInfo?: {
    userId: string
    sessionId: string
    permissions: string[]
  }
}

interface OAuthFlow {
  brokerId: string
  state: string
  authUrl: string
  isInProgress: boolean
  startedAt: Date
}

const BROKER_CONFIGS: BrokerConfig[] = [
  {
    id: 'zerodha',
    name: 'zerodha',
    displayName: 'Zerodha Kite',
    icon: '🚀',
    description: 'India\'s largest discount broker with powerful APIs',
    authType: 'hybrid',
    apiDocUrl: 'https://kite.trade/docs/',
    supportUrl: 'https://support.zerodha.com/',
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'currency'],
    rateLimit: {
      ordersPerSecond: 10,
      requestsPerMinute: 3000
    },
    oauthConfig: {
      authUrl: 'https://kite.zerodha.com/connect/login',
      tokenUrl: 'https://api.kite.trade/session/token',
      scope: ['read', 'write'],
      responseType: 'code'
    },
    apiConfig: {
      baseUrl: 'https://api.kite.trade',
      authHeaders: ['X-Kite-Version', 'Authorization'],
      requiredFields: ['api_key', 'api_secret']
    }
  },
  {
    id: 'upstox',
    name: 'upstox',
    displayName: 'Upstox Pro',
    icon: '📈',
    description: 'Technology-first broker with low-latency execution',
    authType: 'oauth',
    apiDocUrl: 'https://upstox.com/developer/',
    supportUrl: 'https://upstox.com/support/',
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'currency'],
    rateLimit: {
      ordersPerSecond: 5,
      requestsPerMinute: 2000
    },
    oauthConfig: {
      authUrl: 'https://api.upstox.com/oauth/authorization',
      tokenUrl: 'https://api.upstox.com/oauth/token',
      scope: ['read', 'write'],
      responseType: 'code'
    },
    apiConfig: {
      baseUrl: 'https://api.upstox.com/v2',
      authHeaders: ['Authorization', 'Accept'],
      requiredFields: ['client_id', 'client_secret']
    }
  },
  {
    id: 'angel_one',
    name: 'angel_one',
    displayName: 'Angel One',
    icon: '👼',
    description: 'Full-service broker with comprehensive trading solutions',
    authType: 'api_key',
    apiDocUrl: 'https://smartapi.angelbroking.com/',
    supportUrl: 'https://www.angelone.in/support',
    capabilities: ['stocks', 'futures', 'options', 'commodity', 'mutual_funds'],
    rateLimit: {
      ordersPerSecond: 8,
      requestsPerMinute: 2500
    },
    apiConfig: {
      baseUrl: 'https://apiconnect.angelbroking.com',
      authHeaders: ['X-ClientLocalIP', 'X-ClientPublicIP', 'X-MACAddress', 'X-PrivateKey', 'Authorization'],
      requiredFields: ['client_code', 'password', 'api_key']
    }
  },
  {
    id: 'icici_direct',
    name: 'icici_direct',
    displayName: 'ICICI Direct',
    icon: '🏦',
    description: 'Bank-backed broker with institutional-grade platform',
    authType: 'api_key',
    apiDocUrl: 'https://www.icicidirect.com/api-documentation',
    supportUrl: 'https://www.icicidirect.com/support',
    capabilities: ['stocks', 'futures', 'options', 'mutual_funds', 'bonds'],
    rateLimit: {
      ordersPerSecond: 6,
      requestsPerMinute: 1800
    },
    apiConfig: {
      baseUrl: 'https://api.icicidirect.com',
      authHeaders: ['Authorization', 'Content-Type'],
      requiredFields: ['user_id', 'password', 'session_token']
    }
  }
]

export const BrokerAuthenticationInterface: React.FC = () => {
  const { user } = useAuthStore()
  const [connections, setConnections] = useState<ConnectionStatus[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedBroker, setSelectedBroker] = useState<BrokerConfig | null>(null)
  const [showAuthModal, setShowAuthModal] = useState(false)
  const [oauthFlow, setOAuthFlow] = useState<OAuthFlow | null>(null)
  const [credentials, setCredentials] = useState<Record<string, string>>({})
  const [showSecrets, setShowSecrets] = useState<Record<string, boolean>>({})
  const [isConnecting, setIsConnecting] = useState(false)

  // Initialize mock data
  useEffect(() => {
    initializeMockConnections()
  }, [])

  const initializeMockConnections = () => {
    const mockConnections: ConnectionStatus[] = BROKER_CONFIGS.map(broker => ({
      brokerId: broker.id,
      status: Math.random() > 0.5 ? 'connected' : 'disconnected',
      lastConnected: Math.random() > 0.5 ? new Date(Date.now() - Math.random() * 86400000) : undefined,
      tokenExpiry: new Date(Date.now() + Math.random() * 7 * 24 * 60 * 60 * 1000),
      rateLimitUsage: {
        current: Math.floor(Math.random() * broker.rateLimit.requestsPerMinute),
        limit: broker.rateLimit.requestsPerMinute,
        resetTime: new Date(Date.now() + 60000)
      },
      healthMetrics: {
        latency: Math.floor(Math.random() * 500) + 50,
        successRate: Math.floor(Math.random() * 15) + 85,
        errorCount: Math.floor(Math.random() * 10),
        lastError: Math.random() > 0.7 ? 'Rate limit exceeded' : undefined
      },
      sessionInfo: Math.random() > 0.5 ? {
        userId: `USER_${Math.random().toString(36).substr(2, 9)}`,
        sessionId: Math.random().toString(36),
        permissions: ['read', 'write', 'orders']
      } : undefined
    }))
    
    setConnections(mockConnections)
    setLoading(false)
  }

  const handleOAuthFlow = async (broker: BrokerConfig) => {
    if (!broker.oauthConfig) return

    setIsConnecting(true)
    const state = Math.random().toString(36)
    const authUrl = `${broker.oauthConfig.authUrl}?client_id=YOUR_CLIENT_ID&redirect_uri=${encodeURIComponent(window.location.origin)}/broker-callback&response_type=${broker.oauthConfig.responseType}&state=${state}`
    
    const flow: OAuthFlow = {
      brokerId: broker.id,
      state,
      authUrl,
      isInProgress: true,
      startedAt: new Date()
    }

    setOAuthFlow(flow)
    
    // Simulate OAuth flow completion
    setTimeout(() => {
      handleAuthSuccess(broker.id)
      setOAuthFlow(null)
      setIsConnecting(false)
    }, 3000)

    // In real implementation, this would open a popup or redirect
    window.open(authUrl, '_blank', 'width=600,height=800')
  }

  const handleApiKeyAuth = async (broker: BrokerConfig) => {
    if (!broker.apiConfig) return

    setIsConnecting(true)
    
    // Validate required fields
    const missingFields = broker.apiConfig.requiredFields.filter(field => !credentials[field])
    if (missingFields.length > 0) {
      alert(`Missing required fields: ${missingFields.join(', ')}`)
      setIsConnecting(false)
      return
    }

    // Simulate API key validation
    setTimeout(() => {
      handleAuthSuccess(broker.id)
      setIsConnecting(false)
      setShowAuthModal(false)
      setCredentials({})
    }, 2000)
  }

  const handleAuthSuccess = (brokerId: string) => {
    setConnections(prev => prev.map(conn => 
      conn.brokerId === brokerId 
        ? { 
            ...conn, 
            status: 'connected', 
            lastConnected: new Date(),
            sessionInfo: {
              userId: `USER_${Math.random().toString(36).substr(2, 9)}`,
              sessionId: Math.random().toString(36),
              permissions: ['read', 'write', 'orders']
            }
          }
        : conn
    ))
  }

  const handleDisconnect = (brokerId: string) => {
    setConnections(prev => prev.map(conn => 
      conn.brokerId === brokerId 
        ? { ...conn, status: 'disconnected', sessionInfo: undefined }
        : conn
    ))
  }

  const handleTokenRefresh = async (brokerId: string) => {
    setConnections(prev => prev.map(conn => 
      conn.brokerId === brokerId 
        ? { ...conn, status: 'connecting' }
        : conn
    ))

    // Simulate token refresh
    setTimeout(() => {
      setConnections(prev => prev.map(conn => 
        conn.brokerId === brokerId 
          ? { 
              ...conn, 
              status: 'connected',
              tokenExpiry: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
            }
          : conn
      ))
    }, 1500)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected': return 'text-green-400'
      case 'connecting': case 'authenticating': return 'text-yellow-400'
      case 'error': case 'token_expired': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected': return <CheckCircle className="w-4 h-4" />
      case 'connecting': case 'authenticating': return <Clock className="w-4 h-4 animate-spin" />
      case 'error': case 'token_expired': return <XCircle className="w-4 h-4" />
      default: return <WifiOff className="w-4 h-4" />
    }
  }

  const formatTimeUntilExpiry = (expiry: Date) => {
    const now = new Date()
    const diff = expiry.getTime() - now.getTime()
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const days = Math.floor(hours / 24)
    
    if (days > 0) return `${days}d ${hours % 24}h`
    return `${hours}h`
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-3">
          <Clock className="w-6 h-6 text-blue-400 animate-spin" />
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
          <h1 className="text-3xl font-bold gradient-text mb-2">Broker Authentication & Integration</h1>
          <p className="text-slate-400">Manage your broker API connections and authentication</p>
        </div>
        <button
          onClick={() => setShowAuthModal(true)}
          className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center space-x-2"
        >
          <Shield className="w-5 h-5" />
          <span>Add Connection</span>
        </button>
      </div>

      {/* Connection Status Overview */}
      <div className="grid gap-6 md:grid-cols-4">
        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-green-500/20">
              <CheckCircle className="w-6 h-6 text-green-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-green-400">
                {connections.filter(c => c.status === 'connected').length}
              </div>
              <div className="text-sm text-slate-400">Connected</div>
            </div>
          </div>
          <h3 className="text-green-400 font-semibold mb-1">Active Connections</h3>
          <p className="text-slate-400 text-sm">Ready for trading</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-yellow-500/20">
              <Timer className="w-6 h-6 text-yellow-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-yellow-400">
                {connections.filter(c => c.tokenExpiry && c.tokenExpiry.getTime() - Date.now() < 24 * 60 * 60 * 1000).length}
              </div>
              <div className="text-sm text-slate-400">Expiring Soon</div>
            </div>
          </div>
          <h3 className="text-yellow-400 font-semibold mb-1">Token Alerts</h3>
          <p className="text-slate-400 text-sm">Require attention</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-blue-500/20">
              <Activity className="w-6 h-6 text-blue-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-blue-400">
                {Math.round(connections.reduce((sum, c) => sum + c.healthMetrics.successRate, 0) / connections.length || 0)}%
              </div>
              <div className="text-sm text-slate-400">Avg Success</div>
            </div>
          </div>
          <h3 className="text-blue-400 font-semibold mb-1">API Health</h3>
          <p className="text-slate-400 text-sm">Overall performance</p>
        </div>

        <div className="glass-card p-6 rounded-2xl">
          <div className="flex items-center justify-between mb-4">
            <div className="p-3 rounded-xl bg-purple-500/20">
              <Zap className="w-6 h-6 text-purple-400" />
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-purple-400">
                {Math.round(connections.reduce((sum, c) => sum + c.healthMetrics.latency, 0) / connections.length || 0)}ms
              </div>
              <div className="text-sm text-slate-400">Avg Latency</div>
            </div>
          </div>
          <h3 className="text-purple-400 font-semibold mb-1">Response Time</h3>
          <p className="text-slate-400 text-sm">API performance</p>
        </div>
      </div>

      {/* Broker Connections List */}
      <div className="space-y-4">
        {BROKER_CONFIGS.map((broker) => {
          const connection = connections.find(c => c.brokerId === broker.id)
          if (!connection) return null

          const isExpiringSoon = connection.tokenExpiry && 
            connection.tokenExpiry.getTime() - Date.now() < 24 * 60 * 60 * 1000

          return (
            <div key={broker.id} className="glass-card rounded-2xl p-6">
              <div className="flex items-start justify-between">
                <div className="flex items-start space-x-4 flex-1">
                  <div className="text-3xl">{broker.icon}</div>
                  
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h3 className="text-xl font-semibold text-white">{broker.displayName}</h3>
                      <div className={`flex items-center space-x-2 px-3 py-1 rounded-full bg-slate-800/50 ${getStatusColor(connection.status)}`}>
                        {getStatusIcon(connection.status)}
                        <span className="text-xs font-medium capitalize">
                          {connection.status.replace('_', ' ')}
                        </span>
                      </div>
                      {isExpiringSoon && (
                        <div className="flex items-center space-x-1 px-3 py-1 rounded-full bg-yellow-500/20 text-yellow-400">
                          <Timer className="w-3 h-3" />
                          <span className="text-xs font-medium">Expires Soon</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                      <div>
                        <div className="text-xs text-slate-400">Auth Type</div>
                        <div className="text-sm text-white capitalize">{broker.authType.replace('_', ' ')}</div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-400">Rate Limit</div>
                        <div className="text-sm text-white">
                          {connection.rateLimitUsage.current}/{connection.rateLimitUsage.limit}/min
                        </div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-400">Success Rate</div>
                        <div className="text-sm text-white">{connection.healthMetrics.successRate}%</div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-400">Latency</div>
                        <div className="text-sm text-white">{connection.healthMetrics.latency}ms</div>
                      </div>
                    </div>

                    {/* Token Expiry Warning */}
                    {connection.tokenExpiry && (
                      <div className={`p-3 rounded-lg border ${
                        isExpiringSoon 
                          ? 'bg-yellow-500/10 border-yellow-500/30 text-yellow-300'
                          : 'bg-slate-800/50 border-slate-700/50 text-slate-300'
                      } mb-4`}>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <Timer className="w-4 h-4" />
                            <span className="text-sm">Token expires in {formatTimeUntilExpiry(connection.tokenExpiry)}</span>
                          </div>
                          {isExpiringSoon && (
                            <button
                              onClick={() => handleTokenRefresh(broker.id)}
                              disabled={connection.status === 'connecting'}
                              className="px-3 py-1 rounded-lg bg-yellow-500/20 text-yellow-400 hover:bg-yellow-500/30 transition-colors text-sm"
                            >
                              Refresh
                            </button>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Session Info */}
                    {connection.sessionInfo && (
                      <div className="bg-slate-800/30 rounded-lg p-3 mb-4">
                        <div className="text-xs text-slate-400 mb-2">Session Information</div>
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div>
                            <span className="text-slate-400">User ID:</span>
                            <span className="text-white ml-2 font-mono">{connection.sessionInfo.userId}</span>
                          </div>
                          <div>
                            <span className="text-slate-400">Permissions:</span>
                            <span className="text-white ml-2">{connection.sessionInfo.permissions.join(', ')}</span>
                          </div>
                          <div>
                            <span className="text-slate-400">Last Connected:</span>
                            <span className="text-white ml-2">
                              {connection.lastConnected?.toLocaleTimeString() || 'Never'}
                            </span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Error Display */}
                    {connection.healthMetrics.lastError && (
                      <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 mb-4">
                        <div className="flex items-center space-x-2 text-red-400">
                          <AlertTriangle className="w-4 h-4" />
                          <span className="text-sm">Last Error: {connection.healthMetrics.lastError}</span>
                        </div>
                      </div>
                    )}

                    {/* Capabilities */}
                    <div className="flex flex-wrap gap-2">
                      {broker.capabilities.map((cap, index) => (
                        <span key={index} className="px-2 py-1 rounded bg-slate-700/50 text-xs text-slate-300 capitalize">
                          {cap}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
                
                {/* Action Buttons */}
                <div className="flex items-center space-x-2">
                  {connection.status === 'connected' ? (
                    <>
                      <button
                        onClick={() => handleTokenRefresh(broker.id)}
                        className="p-2 rounded-xl bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 transition-colors"
                        title="Refresh Token"
                      >
                        <RefreshCw className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDisconnect(broker.id)}
                        className="p-2 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                        title="Disconnect"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => {
                        setSelectedBroker(broker)
                        setShowAuthModal(true)
                      }}
                      disabled={connection.status === 'connecting'}
                      className="px-4 py-2 rounded-xl bg-green-500/20 text-green-400 hover:bg-green-500/30 disabled:opacity-50 transition-colors font-medium"
                    >
                      Connect
                    </button>
                  )}
                  
                  <a
                    href={broker.apiDocUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-2 rounded-xl bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600/50 transition-colors"
                    title="API Documentation"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Authentication Modal */}
      {showAuthModal && selectedBroker && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-slate-700/50">
              <div className="flex items-center space-x-3">
                <span className="text-2xl">{selectedBroker.icon}</span>
                <div>
                  <h2 className="text-xl font-bold text-white">Connect to {selectedBroker.displayName}</h2>
                  <p className="text-slate-400 text-sm">{selectedBroker.description}</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowAuthModal(false)
                  setSelectedBroker(null)
                  setCredentials({})
                }}
                className="p-2 hover:bg-slate-700/50 rounded-xl transition-colors"
              >
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>

            {/* Modal Content */}
            <div className="flex-1 overflow-y-auto p-6">
              {/* OAuth Flow */}
              {selectedBroker.authType === 'oauth' && selectedBroker.oauthConfig && (
                <div className="space-y-6">
                  <div className="text-center">
                    <Shield className="w-12 h-12 text-blue-400 mx-auto mb-4" />
                    <h3 className="text-lg font-semibold text-white mb-2">OAuth Authentication</h3>
                    <p className="text-slate-400 text-sm mb-6">
                      You'll be redirected to {selectedBroker.displayName} to authorize the connection securely.
                    </p>
                  </div>

                  <div className="bg-slate-800/30 rounded-xl p-4">
                    <h4 className="text-sm font-semibold text-slate-300 mb-3">Permissions Required</h4>
                    <div className="space-y-2">
                      {selectedBroker.oauthConfig.scope.map((scope, index) => (
                        <div key={index} className="flex items-center space-x-2">
                          <CheckCircle className="w-4 h-4 text-green-400" />
                          <span className="text-sm text-slate-300 capitalize">{scope}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {oauthFlow && oauthFlow.brokerId === selectedBroker.id && (
                    <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-4">
                      <div className="flex items-center space-x-2 text-blue-400 mb-2">
                        <Clock className="w-4 h-4 animate-spin" />
                        <span className="text-sm font-medium">OAuth flow in progress...</span>
                      </div>
                      <p className="text-xs text-blue-300">
                        Complete the authorization in the opened window and return here.
                      </p>
                    </div>
                  )}

                  <button
                    onClick={() => handleOAuthFlow(selectedBroker)}
                    disabled={isConnecting || (oauthFlow && oauthFlow.brokerId === selectedBroker.id)}
                    className="w-full cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    {isConnecting ? (
                      <>
                        <Clock className="w-4 h-4 animate-spin" />
                        <span>Connecting...</span>
                      </>
                    ) : (
                      <>
                        <Shield className="w-4 h-4" />
                        <span>Authorize with {selectedBroker.displayName}</span>
                      </>
                    )}
                  </button>
                </div>
              )}

              {/* API Key Flow */}
              {(selectedBroker.authType === 'api_key' || selectedBroker.authType === 'hybrid') && selectedBroker.apiConfig && (
                <div className="space-y-6">
                  <div className="text-center">
                    <Key className="w-12 h-12 text-purple-400 mx-auto mb-4" />
                    <h3 className="text-lg font-semibold text-white mb-2">API Key Authentication</h3>
                    <p className="text-slate-400 text-sm mb-6">
                      Enter your API credentials to connect to {selectedBroker.displayName}.
                    </p>
                  </div>

                  <div className="space-y-4">
                    {selectedBroker.apiConfig.requiredFields.map((field) => (
                      <div key={field}>
                        <label className="block text-sm font-medium text-slate-300 mb-2 capitalize">
                          {field.replace('_', ' ')}
                        </label>
                        <div className="relative">
                          <input
                            type={showSecrets[field] ? 'text' : 'password'}
                            value={credentials[field] || ''}
                            onChange={(e) => setCredentials(prev => ({ ...prev, [field]: e.target.value }))}
                            className="w-full bg-slate-800/50 border border-slate-700/50 rounded-xl px-4 py-3 text-white focus:border-purple-400/50 focus:ring-1 focus:ring-purple-400/50 pr-12"
                            placeholder={`Enter your ${field.replace('_', ' ')}`}
                          />
                          <button
                            type="button"
                            onClick={() => setShowSecrets(prev => ({ ...prev, [field]: !prev[field] }))}
                            className="absolute right-3 top-3 text-slate-400 hover:text-white"
                          >
                            {showSecrets[field] ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="bg-slate-800/30 rounded-xl p-4">
                    <div className="flex items-start space-x-2">
                      <Info className="w-4 h-4 text-blue-400 mt-0.5" />
                      <div className="text-sm text-slate-300">
                        <p className="mb-2">To get your API credentials:</p>
                        <ol className="list-decimal list-inside space-y-1 text-xs text-slate-400">
                          <li>Log in to your {selectedBroker.displayName} account</li>
                          <li>Navigate to API settings or developer section</li>
                          <li>Generate API key and secret</li>
                          <li>Copy the credentials and paste them here</li>
                        </ol>
                        <a 
                          href={selectedBroker.apiDocUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center space-x-1 text-blue-400 hover:text-blue-300 text-xs mt-2"
                        >
                          <ExternalLink className="w-3 h-3" />
                          <span>View API Documentation</span>
                        </a>
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => handleApiKeyAuth(selectedBroker)}
                    disabled={isConnecting || !selectedBroker.apiConfig.requiredFields.every(field => credentials[field])}
                    className="w-full cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 disabled:opacity-50"
                  >
                    {isConnecting ? (
                      <>
                        <Clock className="w-4 h-4 animate-spin" />
                        <span>Connecting...</span>
                      </>
                    ) : (
                      <>
                        <Key className="w-4 h-4" />
                        <span>Connect with API Key</span>
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default BrokerAuthenticationInterface