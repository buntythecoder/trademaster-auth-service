import React, { useState, useEffect } from 'react'
import { Shield, Clock, MapPin, AlertTriangle, Eye, Filter, Search, Download, RefreshCw } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface SecurityEvent {
  id: string
  timestamp: string
  event: string
  eventType: 'login' | 'logout' | 'password_change' | 'mfa_setup' | 'suspicious' | 'device_added' | 'failed_attempt'
  ipAddress: string
  location: string
  userAgent: string
  deviceInfo: string
  riskLevel: 'low' | 'medium' | 'high' | 'critical'
  status: 'success' | 'failed' | 'blocked' | 'flagged'
  details?: string
}

const mockSecurityEvents: SecurityEvent[] = [
  {
    id: '1',
    timestamp: '2024-01-15T14:30:00Z',
    event: 'Successful login',
    eventType: 'login',
    ipAddress: '192.168.1.100',
    location: 'Mumbai, India',
    userAgent: 'Chrome 120.0.6099.71',
    deviceInfo: 'Windows 11 - Desktop',
    riskLevel: 'low',
    status: 'success'
  },
  {
    id: '2',
    timestamp: '2024-01-15T09:15:00Z',
    event: 'Failed login attempt',
    eventType: 'failed_attempt',
    ipAddress: '203.192.1.50',
    location: 'Unknown Location',
    userAgent: 'Unknown Browser',
    deviceInfo: 'Unknown Device',
    riskLevel: 'high',
    status: 'failed',
    details: 'Invalid password - 3rd consecutive attempt'
  },
  {
    id: '3',
    timestamp: '2024-01-14T18:45:00Z',
    event: 'Password changed successfully',
    eventType: 'password_change',
    ipAddress: '192.168.1.100',
    location: 'Mumbai, India',
    userAgent: 'Chrome 120.0.6099.71',
    deviceInfo: 'Windows 11 - Desktop',
    riskLevel: 'low',
    status: 'success'
  },
  {
    id: '4',
    timestamp: '2024-01-14T12:30:00Z',
    event: 'MFA enabled - TOTP',
    eventType: 'mfa_setup',
    ipAddress: '192.168.1.101',
    location: 'Mumbai, India',
    userAgent: 'Safari Mobile',
    deviceInfo: 'iOS 17.2 - iPhone',
    riskLevel: 'low',
    status: 'success'
  },
  {
    id: '5',
    timestamp: '2024-01-13T23:45:00Z',
    event: 'Suspicious login attempt blocked',
    eventType: 'suspicious',
    ipAddress: '49.123.45.67',
    location: 'Bangalore, India',
    userAgent: 'Chrome Mobile',
    deviceInfo: 'Android 14 - Mobile',
    riskLevel: 'critical',
    status: 'blocked',
    details: 'Login from new device without MFA verification'
  }
]

interface SecurityAuditLogsProps {
  height?: number
}

export function SecurityAuditLogs({ height = 600 }: SecurityAuditLogsProps) {
  const [events, setEvents] = useState<SecurityEvent[]>(mockSecurityEvents)
  const [filter, setFilter] = useState<'all' | 'login' | 'security' | 'suspicious'>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedEvent, setSelectedEvent] = useState<SecurityEvent | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const { success, info } = useToast()

  const getRiskColor = (level: string) => {
    switch (level) {
      case 'low': return 'text-green-400'
      case 'medium': return 'text-yellow-400'
      case 'high': return 'text-orange-400'
      case 'critical': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getRiskBg = (level: string) => {
    switch (level) {
      case 'low': return 'bg-green-500/20'
      case 'medium': return 'bg-yellow-500/20'
      case 'high': return 'bg-orange-500/20'
      case 'critical': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const getEventIcon = (eventType: string, status: string) => {
    if (status === 'failed' || status === 'blocked') return AlertTriangle
    switch (eventType) {
      case 'login': return Shield
      case 'logout': return Shield
      case 'mfa_setup': return Shield
      case 'device_added': return Shield
      default: return Clock
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'success': return 'text-green-400'
      case 'failed': return 'text-red-400'
      case 'blocked': return 'text-red-400'
      case 'flagged': return 'text-orange-400'
      default: return 'text-slate-400'
    }
  }

  const filteredEvents = events.filter(event => {
    const matchesFilter = filter === 'all' || 
      (filter === 'login' && ['login', 'logout'].includes(event.eventType)) ||
      (filter === 'security' && ['password_change', 'mfa_setup', 'device_added'].includes(event.eventType)) ||
      (filter === 'suspicious' && ['suspicious', 'failed_attempt'].includes(event.eventType))
    
    const matchesSearch = searchTerm === '' || 
      event.event.toLowerCase().includes(searchTerm.toLowerCase()) ||
      event.ipAddress.includes(searchTerm) ||
      event.location.toLowerCase().includes(searchTerm.toLowerCase())
    
    return matchesFilter && matchesSearch
  })

  const handleRefresh = async () => {
    setIsRefreshing(true)
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
    setIsRefreshing(false)
    success('Logs Refreshed', 'Security audit logs have been updated')
  }

  const exportLogs = () => {
    info('Export Started', 'Security logs are being prepared for download')
  }

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  return (
    <div className="glass-card rounded-2xl p-6" style={{ height }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center">
            <Shield className="w-5 h-5 mr-2 text-cyan-400" />
            Security Audit Logs
          </h3>
          <p className="text-sm text-slate-400 mt-1">
            Monitor account security events and activities
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="p-2 rounded-xl glass-card text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={exportLogs}
            className="cyber-button px-3 py-2 text-sm rounded-xl flex items-center space-x-2"
          >
            <Download className="w-4 h-4" />
            <span>Export</span>
          </button>
        </div>
      </div>

      {/* Filters and Search */}
      <div className="flex flex-wrap items-center gap-4 mb-6">
        <div className="flex items-center space-x-2">
          <Search className="w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search events, IP, location..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="cyber-input px-3 py-2 text-sm rounded-xl w-64"
          />
        </div>
        
        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value as any)}
          className="cyber-input px-3 py-2 text-sm rounded-xl"
        >
          <option value="all">All Events</option>
          <option value="login">Login/Logout</option>
          <option value="security">Security Changes</option>
          <option value="suspicious">Suspicious Activity</option>
        </select>
      </div>

      {/* Events List */}
      <div className="space-y-3 max-h-96 overflow-y-auto custom-scrollbar">
        {filteredEvents.map((event) => {
          const EventIcon = getEventIcon(event.eventType, event.status)
          return (
            <div
              key={event.id}
              onClick={() => setSelectedEvent(event)}
              className={`p-4 rounded-xl cursor-pointer transition-all border ${
                event.riskLevel === 'critical' 
                  ? 'bg-red-500/10 border-red-500/30 hover:border-red-400/50' 
                  : event.riskLevel === 'high'
                  ? 'bg-orange-500/10 border-orange-500/30 hover:border-orange-400/50'
                  : 'bg-slate-800/30 border-slate-700/50 hover:border-purple-500/50'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start space-x-3 flex-1">
                  <div className={`p-2 rounded-lg ${getRiskBg(event.riskLevel)}`}>
                    <EventIcon className={`w-4 h-4 ${getRiskColor(event.riskLevel)}`} />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-3 mb-1">
                      <h4 className="font-medium text-white truncate">{event.event}</h4>
                      <div className={`px-2 py-1 rounded text-xs font-medium ${
                        getRiskBg(event.riskLevel)} ${getRiskColor(event.riskLevel)
                      }`}>
                        {event.riskLevel}
                      </div>
                      <div className={`px-2 py-1 rounded text-xs font-medium ${
                        event.status === 'success' ? 'bg-green-500/20 text-green-400' :
                        event.status === 'failed' ? 'bg-red-500/20 text-red-400' :
                        event.status === 'blocked' ? 'bg-red-500/20 text-red-400' :
                        'bg-slate-500/20 text-slate-400'
                      }`}>
                        {event.status}
                      </div>
                    </div>
                    
                    <div className="grid gap-2 md:grid-cols-2 text-sm text-slate-400">
                      <div className="flex items-center space-x-2">
                        <Clock className="w-3 h-3" />
                        <span>{formatTimestamp(event.timestamp)}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <MapPin className="w-3 h-3" />
                        <span>{event.location}</span>
                      </div>
                      <div className="font-mono text-xs">{event.ipAddress}</div>
                      <div className="text-xs">{event.deviceInfo}</div>
                    </div>
                    
                    {event.details && (
                      <div className="mt-2 p-2 rounded bg-slate-800/50 text-sm text-orange-300">
                        {event.details}
                      </div>
                    )}
                  </div>
                </div>
                
                <button className="p-1 rounded hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors">
                  <Eye className="w-4 h-4" />
                </button>
              </div>
            </div>
          )
        })}
        
        {filteredEvents.length === 0 && (
          <div className="text-center py-8">
            <Shield className="w-12 h-12 text-slate-500 mx-auto mb-4" />
            <p className="text-slate-400">No security events found</p>
            <p className="text-slate-500 text-sm">Try adjusting your filters</p>
          </div>
        )}
      </div>

      {/* Event Details Modal */}
      {selectedEvent && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="glass-card rounded-2xl p-6 max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-bold text-white">Event Details</h3>
              <button
                onClick={() => setSelectedEvent(null)}
                className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>
            
            <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-sm font-medium text-slate-300">Event</label>
                  <div className="text-white font-medium">{selectedEvent.event}</div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Status</label>
                  <div className={`font-medium ${getStatusColor(selectedEvent.status)}`}>
                    {selectedEvent.status.toUpperCase()}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Risk Level</label>
                  <div className={`font-medium ${getRiskColor(selectedEvent.riskLevel)}`}>
                    {selectedEvent.riskLevel.toUpperCase()}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Timestamp</label>
                  <div className="text-white font-mono text-sm">
                    {formatTimestamp(selectedEvent.timestamp)}
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">IP Address</label>
                  <div className="text-white font-mono">{selectedEvent.ipAddress}</div>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-300">Location</label>
                  <div className="text-white">{selectedEvent.location}</div>
                </div>
                <div className="md:col-span-2">
                  <label className="text-sm font-medium text-slate-300">User Agent</label>
                  <div className="text-white font-mono text-sm break-all">{selectedEvent.userAgent}</div>
                </div>
                <div className="md:col-span-2">
                  <label className="text-sm font-medium text-slate-300">Device Information</label>
                  <div className="text-white">{selectedEvent.deviceInfo}</div>
                </div>
                {selectedEvent.details && (
                  <div className="md:col-span-2">
                    <label className="text-sm font-medium text-slate-300">Additional Details</label>
                    <div className="text-orange-300 p-3 rounded-lg bg-orange-500/10 border border-orange-500/30">
                      {selectedEvent.details}
                    </div>
                  </div>
                )}
              </div>
            </div>
            
            <div className="flex space-x-3 mt-6">
              <button
                onClick={() => setSelectedEvent(null)}
                className="flex-1 cyber-button py-2 rounded-xl text-sm"
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