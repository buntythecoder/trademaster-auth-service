import React, { useState, useEffect } from 'react'
import { Monitor, Smartphone, Globe, Clock, MapPin, Trash2, Shield, AlertTriangle, Power, Settings } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface ActiveSession {
  id: string
  deviceName: string
  deviceType: 'desktop' | 'mobile' | 'tablet'
  browser: string
  os: string
  ipAddress: string
  location: string
  country: string
  lastActivity: string
  loginTime: string
  isCurrentSession: boolean
  status: 'active' | 'idle' | 'expired'
  sessionDuration: string
  userAgent: string
}

interface SessionSettings {
  autoLogoutTime: number // in minutes
  maxConcurrentSessions: number
  allowMultipleDevices: boolean
  requireMFAForNewDevices: boolean
  sessionTimeout: boolean
}

const mockSessions: ActiveSession[] = [
  {
    id: '1',
    deviceName: 'My Laptop - Chrome',
    deviceType: 'desktop',
    browser: 'Chrome 120.0.6099.71',
    os: 'Windows 11',
    ipAddress: '192.168.1.100',
    location: 'Mumbai, Maharashtra',
    country: 'India',
    lastActivity: '2024-01-15T14:30:00Z',
    loginTime: '2024-01-15T08:00:00Z',
    isCurrentSession: true,
    status: 'active',
    sessionDuration: '6h 30m',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
  },
  {
    id: '2',
    deviceName: 'iPhone 13 Pro',
    deviceType: 'mobile',
    browser: 'Safari Mobile',
    os: 'iOS 17.2',
    ipAddress: '192.168.1.101',
    location: 'Mumbai, Maharashtra',
    country: 'India',
    lastActivity: '2024-01-15T12:15:00Z',
    loginTime: '2024-01-15T07:30:00Z',
    isCurrentSession: false,
    status: 'idle',
    sessionDuration: '7h',
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X)'
  },
  {
    id: '3',
    deviceName: 'Work PC - Firefox',
    deviceType: 'desktop',
    browser: 'Firefox 121.0',
    os: 'Windows 10',
    ipAddress: '203.192.1.50',
    location: 'Delhi, Delhi',
    country: 'India',
    lastActivity: '2024-01-14T18:45:00Z',
    loginTime: '2024-01-14T09:00:00Z',
    isCurrentSession: false,
    status: 'expired',
    sessionDuration: '9h 45m',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101'
  }
]

const defaultSettings: SessionSettings = {
  autoLogoutTime: 30,
  maxConcurrentSessions: 5,
  allowMultipleDevices: true,
  requireMFAForNewDevices: true,
  sessionTimeout: true
}

interface SessionManagementProps {
  height?: number
}

export function SessionManagement({ height = 700 }: SessionManagementProps) {
  const [sessions, setSessions] = useState<ActiveSession[]>(mockSessions)
  const [settings, setSettings] = useState<SessionSettings>(defaultSettings)
  const [showSettings, setShowSettings] = useState(false)
  const [selectedSession, setSelectedSession] = useState<ActiveSession | null>(null)
  const { success, warning, error } = useToast()

  useEffect(() => {
    // Update session activity every minute
    const interval = setInterval(() => {
      setSessions(prev => prev.map(session => ({
        ...session,
        lastActivity: session.isCurrentSession ? new Date().toISOString() : session.lastActivity,
        status: session.isCurrentSession ? 'active' : session.status
      })))
    }, 60000)

    return () => clearInterval(interval)
  }, [])

  const getDeviceIcon = (type: string) => {
    switch (type) {
      case 'desktop': return Monitor
      case 'mobile': return Smartphone
      case 'tablet': return Globe
      default: return Monitor
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'text-green-400'
      case 'idle': return 'text-yellow-400'
      case 'expired': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-500/20'
      case 'idle': return 'bg-yellow-500/20'
      case 'expired': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const terminateSession = (sessionId: string) => {
    if (sessions.find(s => s.id === sessionId)?.isCurrentSession) {
      error('Cannot Terminate', 'Cannot terminate your current session')
      return
    }
    
    setSessions(prev => prev.filter(session => session.id !== sessionId))
    success('Session Terminated', 'The session has been terminated successfully')
  }

  const terminateAllOtherSessions = () => {
    setSessions(prev => prev.filter(session => session.isCurrentSession))
    success('Sessions Terminated', 'All other sessions have been terminated')
  }

  const extendSession = (sessionId: string) => {
    setSessions(prev => prev.map(session => 
      session.id === sessionId 
        ? { ...session, status: 'active' as const, lastActivity: new Date().toISOString() }
        : session
    ))
    success('Session Extended', 'Session has been extended for 24 hours')
  }

  const updateSettings = (newSettings: Partial<SessionSettings>) => {
    setSettings(prev => ({ ...prev, ...newSettings }))
    success('Settings Updated', 'Session security settings have been saved')
  }

  const formatLastActivity = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffInMinutes < 1) return 'Just now'
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}h ago`
    return `${Math.floor(diffInMinutes / 1440)}d ago`
  }

  const activeSessions = sessions.filter(s => s.status !== 'expired')
  const expiredSessions = sessions.filter(s => s.status === 'expired')

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Shield className="w-5 h-5 mr-2 text-cyan-400" />
            Session Management
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            Active sessions: {activeSessions.length} • Expired: {expiredSessions.length}
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowSettings(!showSettings)}
            className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors"
          >
            <Settings className="w-4 h-4" />
          </button>
          <button
            onClick={terminateAllOtherSessions}
            className="cyber-button px-3 py-2 text-sm rounded-xl flex items-center space-x-2"
          >
            <Power className="w-4 h-4" />
            <span>End All Others</span>
          </button>
        </div>
      </div>

      {/* Session Settings */}
      {showSettings && (
        <div className="mb-6 p-4 rounded-xl bg-slate-800/30 border border-purple-500/30">
          <h4 className="font-semibold text-white mb-4">Security Settings</h4>
          
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Auto-logout after inactivity (minutes)
              </label>
              <input
                type="number"
                value={settings.autoLogoutTime}
                onChange={(e) => updateSettings({ autoLogoutTime: parseInt(e.target.value) })}
                className="cyber-input w-full py-2 rounded-xl"
                min="5"
                max="1440"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-2">
                Maximum concurrent sessions
              </label>
              <input
                type="number"
                value={settings.maxConcurrentSessions}
                onChange={(e) => updateSettings({ maxConcurrentSessions: parseInt(e.target.value) })}
                className="cyber-input w-full py-2 rounded-xl"
                min="1"
                max="10"
              />
            </div>
            
            <div className="md:col-span-2 space-y-3">
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={settings.allowMultipleDevices}
                  onChange={(e) => updateSettings({ allowMultipleDevices: e.target.checked })}
                  className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
                />
                <span className="text-sm text-slate-300">Allow multiple device logins</span>
              </label>
              
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={settings.requireMFAForNewDevices}
                  onChange={(e) => updateSettings({ requireMFAForNewDevices: e.target.checked })}
                  className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
                />
                <span className="text-sm text-slate-300">Require MFA for new devices</span>
              </label>
              
              <label className="flex items-center space-x-3">
                <input
                  type="checkbox"
                  checked={settings.sessionTimeout}
                  onChange={(e) => updateSettings({ sessionTimeout: e.target.checked })}
                  className="w-4 h-4 text-purple-400 bg-slate-700 border-slate-600 rounded focus:ring-purple-500"
                />
                <span className="text-sm text-slate-300">Enable automatic session timeout</span>
              </label>
            </div>
          </div>
        </div>
      )}

      {/* Active Sessions */}
      <div className="space-y-4 max-h-96 overflow-y-auto custom-scrollbar">
        <h4 className="font-semibold text-white">Active Sessions</h4>
        
        {activeSessions.map((session) => {
          const DeviceIcon = getDeviceIcon(session.deviceType)
          return (
            <div
              key={session.id}
              className={`p-4 rounded-xl transition-all border ${
                session.isCurrentSession 
                  ? 'bg-purple-500/10 border-purple-500/30' 
                  : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600/70'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start space-x-4 flex-1">
                  <div className={`p-3 rounded-xl ${
                    session.isCurrentSession ? 'bg-purple-500/20' : 'bg-slate-600/50'
                  }`}>
                    <DeviceIcon className="w-5 h-5 text-cyan-400" />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-3 mb-2">
                      <h5 className="font-medium text-white">{session.deviceName}</h5>
                      {session.isCurrentSession && (
                        <span className="px-2 py-1 bg-purple-500/20 text-purple-400 text-xs font-medium rounded">
                          Current
                        </span>
                      )}
                      <div className={`px-2 py-1 rounded text-xs font-medium ${
                        getStatusBg(session.status)} ${getStatusColor(session.status)
                      }`}>
                        {session.status}
                      </div>
                    </div>
                    
                    <div className="grid gap-2 md:grid-cols-2 text-sm text-slate-400">
                      <div className="flex items-center space-x-2">
                        <Monitor className="w-3 h-3" />
                        <span>{session.browser} • {session.os}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <MapPin className="w-3 h-3" />
                        <span>{session.location}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Clock className="w-3 h-3" />
                        <span>Last active {formatLastActivity(session.lastActivity)}</span>
                      </div>
                      <div className="font-mono text-xs">{session.ipAddress}</div>
                    </div>
                    
                    <div className="mt-2 text-xs text-slate-500">
                      Session duration: {session.sessionDuration} • 
                      Logged in: {new Date(session.loginTime).toLocaleString('en-IN', { 
                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' 
                      })}
                    </div>
                  </div>
                </div>
                
                {/* Actions */}
                <div className="flex items-center space-x-2 ml-4">
                  {!session.isCurrentSession && (
                    <>
                      {session.status === 'idle' && (
                        <button
                          onClick={() => extendSession(session.id)}
                          className="p-2 rounded-lg hover:bg-green-500/20 text-slate-400 hover:text-green-400 transition-colors"
                          title="Extend session"
                        >
                          <Clock className="w-4 h-4" />
                        </button>
                      )}
                      <button
                        onClick={() => terminateSession(session.id)}
                        className="p-2 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                        title="Terminate session"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </>
                  )}
                  <button
                    onClick={() => setSelectedSession(session)}
                    className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                    title="View details"
                  >
                    <Shield className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )
        })}
        
        {/* Expired Sessions */}
        {expiredSessions.length > 0 && (
          <>
            <h4 className="font-semibold text-slate-400 mt-6 mb-4">Expired Sessions</h4>
            {expiredSessions.map((session) => {
              const DeviceIcon = getDeviceIcon(session.deviceType)
              return (
                <div key={session.id} className="p-4 rounded-xl bg-slate-800/20 border border-slate-700/30">
                  <div className="flex items-start justify-between opacity-60">
                    <div className="flex items-start space-x-4 flex-1">
                      <div className="p-3 rounded-xl bg-slate-600/30">
                        <DeviceIcon className="w-5 h-5 text-slate-500" />
                      </div>
                      
                      <div className="flex-1">
                        <div className="flex items-center space-x-3 mb-2">
                          <h5 className="font-medium text-slate-400">{session.deviceName}</h5>
                          <div className="px-2 py-1 bg-red-500/20 text-red-400 text-xs font-medium rounded">
                            Expired
                          </div>
                        </div>
                        
                        <div className="text-sm text-slate-500">
                          {session.location} • {formatLastActivity(session.lastActivity)}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}
          </>
        )}
      </div>

      {/* Session Details Modal */}
      {selectedSession && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="glass-card rounded-2xl p-6 max-w-2xl w-full">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-bold text-white">Session Details</h3>
              <button
                onClick={() => setSelectedSession(null)}
                className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            
            <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-sm font-medium text-slate-300">Device</label>
                  <div className="text-white font-medium">{selectedSession.deviceName}</div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Status</label>
                  <div className={`font-medium ${getStatusColor(selectedSession.status)}`}>
                    {selectedSession.status.toUpperCase()}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">IP Address</label>
                  <div className="text-white font-mono">{selectedSession.ipAddress}</div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Location</label>
                  <div className="text-white">{selectedSession.location}, {selectedSession.country}</div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Login Time</label>
                  <div className="text-white">
                    {new Date(selectedSession.loginTime).toLocaleString('en-IN')}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Last Activity</label>
                  <div className="text-white">
                    {new Date(selectedSession.lastActivity).toLocaleString('en-IN')}
                  </div>
                </div>
                <div className="md:col-span-2">
                  <label className="text-sm font-medium text-slate-300">User Agent</label>
                  <div className="text-white font-mono text-sm break-all mt-1">
                    {selectedSession.userAgent}
                  </div>
                </div>
              </div>
            </div>
            
            <div className="flex space-x-3 mt-6">
              {!selectedSession.isCurrentSession && (
                <button
                  onClick={() => {
                    terminateSession(selectedSession.id)
                    setSelectedSession(null)
                  }}
                  className="flex-1 py-2 px-4 rounded-xl bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
                >
                  Terminate Session
                </button>
              )}
              <button
                onClick={() => setSelectedSession(null)}
                className="flex-1 glass-card py-2 rounded-xl text-slate-400 hover:text-white transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}