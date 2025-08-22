import React, { useState, useEffect } from 'react'
import { Smartphone, Monitor, Tablet, Shield, MapPin, Clock, Trash2, CheckCircle, AlertTriangle, Eye, EyeOff } from 'lucide-react'
import { useToast } from '../../contexts/ToastContext'

interface TrustedDevice {
  id: string
  name: string
  type: 'desktop' | 'mobile' | 'tablet'
  browser: string
  os: string
  location: string
  ipAddress: string
  lastAccess: string
  status: 'active' | 'inactive' | 'blocked'
  isCurrent: boolean
  trusted: boolean
  fingerprint: string
}

interface DeviceTrustProps {
  onDeviceAction?: (action: string, deviceId: string) => void
}

const mockDevices: TrustedDevice[] = [
  {
    id: '1',
    name: 'My Laptop - Chrome',
    type: 'desktop',
    browser: 'Chrome 120.0',
    os: 'Windows 11',
    location: 'Mumbai, India',
    ipAddress: '192.168.1.100',
    lastAccess: '2024-01-15T14:30:00Z',
    status: 'active',
    isCurrent: true,
    trusted: true,
    fingerprint: 'fp_abc123def456'
  },
  {
    id: '2',
    name: 'iPhone 13 Pro',
    type: 'mobile',
    browser: 'Safari Mobile',
    os: 'iOS 17.2',
    location: 'Mumbai, India',
    ipAddress: '192.168.1.101',
    lastAccess: '2024-01-15T12:15:00Z',
    status: 'active',
    isCurrent: false,
    trusted: true,
    fingerprint: 'fp_xyz789ghi012'
  },
  {
    id: '3',
    name: 'Work PC - Firefox',
    type: 'desktop',
    browser: 'Firefox 121.0',
    os: 'Windows 10',
    location: 'Delhi, India',
    ipAddress: '203.192.1.50',
    lastAccess: '2024-01-14T18:45:00Z',
    status: 'inactive',
    isCurrent: false,
    trusted: true,
    fingerprint: 'fp_def456jkl789'
  },
  {
    id: '4',
    name: 'Unknown Device',
    type: 'mobile',
    browser: 'Chrome Mobile',
    os: 'Android 14',
    location: 'Bangalore, India',
    ipAddress: '49.123.45.67',
    lastAccess: '2024-01-13T09:20:00Z',
    status: 'blocked',
    isCurrent: false,
    trusted: false,
    fingerprint: 'fp_mno345pqr678'
  }
]

export function DeviceTrust({ onDeviceAction }: DeviceTrustProps) {
  const [devices, setDevices] = useState<TrustedDevice[]>(mockDevices)
  const [showDetails, setShowDetails] = useState<{ [key: string]: boolean }>({})
  const [selectedDevice, setSelectedDevice] = useState<string | null>(null)
  const { success, error, warning, info } = useToast()

  const getDeviceIcon = (type: string, size = 5) => {
    const className = `w-${size} h-${size}`
    switch (type) {
      case 'desktop': return <Monitor className={className} />
      case 'mobile': return <Smartphone className={className} />
      case 'tablet': return <Tablet className={className} />
      default: return <Monitor className={className} />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'text-green-400'
      case 'inactive': return 'text-slate-400'
      case 'blocked': return 'text-red-400'
      default: return 'text-slate-400'
    }
  }

  const getStatusBg = (status: string) => {
    switch (status) {
      case 'active': return 'bg-green-500/20'
      case 'inactive': return 'bg-slate-500/20'
      case 'blocked': return 'bg-red-500/20'
      default: return 'bg-slate-500/20'
    }
  }

  const handleTrustDevice = (deviceId: string, trust: boolean) => {
    setDevices(prev => prev.map(device =>
      device.id === deviceId
        ? { ...device, trusted: trust, status: trust ? 'active' : 'blocked' as const }
        : device
    ))

    if (trust) {
      success('Device Trusted', 'Device has been added to trusted devices')
    } else {
      warning('Device Untrusted', 'Device has been removed from trusted devices')
    }

    onDeviceAction?.(trust ? 'trust' : 'untrust', deviceId)
  }

  const handleBlockDevice = (deviceId: string) => {
    setDevices(prev => prev.map(device =>
      device.id === deviceId
        ? { ...device, status: 'blocked' as const, trusted: false }
        : device
    ))
    error('Device Blocked', 'Device has been blocked and signed out')
    onDeviceAction?.('block', deviceId)
  }

  const handleRemoveDevice = (deviceId: string) => {
    setDevices(prev => prev.filter(device => device.id !== deviceId))
    info('Device Removed', 'Device has been removed from your account')
    onDeviceAction?.('remove', deviceId)
  }

  const toggleDetails = (deviceId: string) => {
    setShowDetails(prev => ({
      ...prev,
      [deviceId]: !prev[deviceId]
    }))
  }

  const formatLastAccess = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffInMinutes < 60) {
      return `${diffInMinutes} minutes ago`
    } else if (diffInMinutes < 1440) {
      return `${Math.floor(diffInMinutes / 60)} hours ago`
    } else {
      return `${Math.floor(diffInMinutes / 1440)} days ago`
    }
  }

  const trustedDevicesCount = devices.filter(d => d.trusted).length
  const activeDevicesCount = devices.filter(d => d.status === 'active').length
  const blockedDevicesCount = devices.filter(d => d.status === 'blocked').length

  return (
    <div className="glass-card rounded-2xl p-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center">
            <Shield className="w-6 h-6 mr-3 text-cyan-400" />
            Device Trust Management
          </h2>
          <p className="text-slate-400 mt-2">
            Manage your trusted devices and monitor account access
          </p>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid gap-4 md:grid-cols-3 mb-8">
        <div className="glass-card p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-slate-400">Trusted Devices</div>
              <div className="text-2xl font-bold text-green-400">{trustedDevicesCount}</div>
            </div>
            <CheckCircle className="w-8 h-8 text-green-400/50" />
          </div>
        </div>
        
        <div className="glass-card p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-slate-400">Active Sessions</div>
              <div className="text-2xl font-bold text-cyan-400">{activeDevicesCount}</div>
            </div>
            <Monitor className="w-8 h-8 text-cyan-400/50" />
          </div>
        </div>
        
        <div className="glass-card p-4 rounded-xl">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-slate-400">Blocked Devices</div>
              <div className="text-2xl font-bold text-red-400">{blockedDevicesCount}</div>
            </div>
            <AlertTriangle className="w-8 h-8 text-red-400/50" />
          </div>
        </div>
      </div>

      {/* Devices List */}
      <div className="space-y-4">
        {devices.map((device) => (
          <div
            key={device.id}
            className={`p-6 rounded-xl transition-all ${
              device.isCurrent 
                ? 'bg-purple-500/10 border border-purple-500/30' 
                : device.status === 'blocked'
                ? 'bg-red-500/10 border border-red-500/30'
                : 'bg-slate-800/30 border border-slate-700/50'
            }`}
          >
            {/* Device Header */}
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-start space-x-4">
                <div className={`p-3 rounded-xl ${
                  device.trusted ? 'bg-green-500/20' : 'bg-slate-500/20'
                }`}>
                  {getDeviceIcon(device.type, 6)}
                </div>
                
                <div className="flex-1">
                  <div className="flex items-center space-x-3 mb-2">
                    <h3 className="font-semibold text-white">{device.name}</h3>
                    {device.isCurrent && (
                      <span className="px-2 py-1 bg-purple-500/20 text-purple-400 text-xs font-medium rounded">
                        Current Device
                      </span>
                    )}
                    <div className={`px-2 py-1 rounded text-xs font-medium ${
                      getStatusBg(device.status)} ${getStatusColor(device.status)
                    }`}>
                      {device.status}
                    </div>
                    {device.trusted && (
                      <div className="px-2 py-1 bg-green-500/20 text-green-400 text-xs font-medium rounded flex items-center">
                        <Shield className="w-3 h-3 mr-1" />
                        Trusted
                      </div>
                    )}
                  </div>
                  
                  <div className="grid gap-2 md:grid-cols-2 text-sm text-slate-400">
                    <div className="flex items-center space-x-2">
                      <Monitor className="w-4 h-4" />
                      <span>{device.browser} • {device.os}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <MapPin className="w-4 h-4" />
                      <span>{device.location}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Clock className="w-4 h-4" />
                      <span>Last active {formatLastAccess(device.lastAccess)}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs">{device.ipAddress}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Actions */}
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => toggleDetails(device.id)}
                  className="p-2 rounded-lg hover:bg-slate-600/50 text-slate-400 hover:text-white transition-colors"
                  title={showDetails[device.id] ? 'Hide Details' : 'Show Details'}
                >
                  {showDetails[device.id] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
                
                {!device.isCurrent && (
                  <>
                    {device.status !== 'blocked' && (
                      <button
                        onClick={() => handleTrustDevice(device.id, !device.trusted)}
                        className={`p-2 rounded-lg transition-colors ${
                          device.trusted 
                            ? 'hover:bg-orange-500/20 text-slate-400 hover:text-orange-400'
                            : 'hover:bg-green-500/20 text-slate-400 hover:text-green-400'
                        }`}
                        title={device.trusted ? 'Remove from trusted' : 'Add to trusted'}
                      >
                        <Shield className="w-4 h-4" />
                      </button>
                    )}
                    
                    {device.status !== 'blocked' && (
                      <button
                        onClick={() => handleBlockDevice(device.id)}
                        className="p-2 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                        title="Block device"
                      >
                        <AlertTriangle className="w-4 h-4" />
                      </button>
                    )}
                    
                    <button
                      onClick={() => handleRemoveDevice(device.id)}
                      className="p-2 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                      title="Remove device"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </>
                )}
              </div>
            </div>

            {/* Device Details */}
            {showDetails[device.id] && (
              <div className="mt-4 p-4 rounded-xl bg-slate-800/50 border border-slate-700/30">
                <h4 className="font-medium text-white mb-3">Device Details</h4>
                <div className="grid gap-3 md:grid-cols-2 text-sm">
                  <div>
                    <span className="text-slate-400">Device Fingerprint:</span>
                    <div className="font-mono text-slate-300 mt-1">{device.fingerprint}</div>
                  </div>
                  <div>
                    <span className="text-slate-400">First Seen:</span>
                    <div className="text-slate-300 mt-1">
                      {new Date(device.lastAccess).toLocaleDateString('en-IN', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-400">Security Status:</span>
                    <div className={`mt-1 ${device.trusted ? 'text-green-400' : 'text-orange-400'}`}>
                      {device.trusted ? 'Trusted and verified' : 'Requires verification'}
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-400">Risk Level:</span>
                    <div className={`mt-1 ${
                      device.status === 'blocked' ? 'text-red-400' :
                      device.trusted ? 'text-green-400' : 'text-yellow-400'
                    }`}>
                      {device.status === 'blocked' ? 'High Risk - Blocked' :
                       device.trusted ? 'Low Risk' : 'Medium Risk'}
                    </div>
                  </div>
                </div>
                
                {/* Additional Actions */}
                {!device.isCurrent && device.status !== 'blocked' && (
                  <div className="flex space-x-3 mt-4 pt-4 border-t border-slate-700/50">
                    <button
                      onClick={() => info('Sign Out', 'Device will be signed out remotely')}
                      className="flex-1 py-2 px-4 rounded-lg bg-orange-500/20 text-orange-400 hover:bg-orange-500/30 transition-colors text-sm"
                    >
                      Sign Out Device
                    </button>
                    <button
                      onClick={() => info('Security Scan', 'Running security scan on device')}
                      className="flex-1 py-2 px-4 rounded-lg glass-card text-slate-400 hover:text-white transition-colors text-sm"
                    >
                      Security Scan
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Security Recommendations */}
      <div className="mt-8 p-6 rounded-xl bg-blue-500/10 border border-blue-500/30">
        <h4 className="font-semibold text-white mb-4 flex items-center">
          <Shield className="w-5 h-5 mr-2 text-blue-400" />
          Security Recommendations
        </h4>
        <div className="space-y-3 text-sm text-blue-300">
          <div className="flex items-start space-x-3">
            <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
            <div>
              <strong>Regular Review:</strong> Review your trusted devices monthly and remove unused devices
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
            <div>
              <strong>Unknown Devices:</strong> Immediately block any unrecognized devices or login attempts
            </div>
          </div>
          <div className="flex items-start space-x-3">
            <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
            <div>
              <strong>Public Devices:</strong> Never mark shared or public computers as trusted
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}