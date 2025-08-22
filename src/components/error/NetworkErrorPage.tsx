import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Wifi, WifiOff, RefreshCw, AlertTriangle, CheckCircle, Globe, Zap } from 'lucide-react'
import { FloatingShapes } from '../effects/FloatingShapes'
import { useToast } from '../../contexts/ToastContext'

export function NetworkErrorPage() {
  const [isOnline, setIsOnline] = useState(navigator.onLine)
  const [isRetrying, setIsRetrying] = useState(false)
  const [retryAttempts, setRetryAttempts] = useState(0)
  const { success, error, info } = useToast()

  useEffect(() => {
    const handleOnlineStatus = () => {
      const online = navigator.onLine
      setIsOnline(online)
      
      if (online) {
        success('Connection Restored', 'You are back online!')
      } else {
        error('Connection Lost', 'Please check your internet connection')
      }
    }

    window.addEventListener('online', handleOnlineStatus)
    window.addEventListener('offline', handleOnlineStatus)

    return () => {
      window.removeEventListener('online', handleOnlineStatus)
      window.removeEventListener('offline', handleOnlineStatus)
    }
  }, [success, error])

  const handleRetry = async () => {
    setIsRetrying(true)
    setRetryAttempts(prev => prev + 1)
    
    info('Checking Connection', 'Testing network connectivity...')
    
    try {
      // Simulate network test
      const response = await fetch('/api/health', { 
        method: 'GET',
        cache: 'no-cache' 
      })
      
      if (response.ok) {
        success('Connection Successful', 'Redirecting to dashboard...')
        setTimeout(() => {
          window.location.href = '/dashboard'
        }, 1500)
      } else {
        throw new Error('Server unreachable')
      }
    } catch (err) {
      error('Connection Failed', 'Unable to reach TradeMaster servers')
    } finally {
      setIsRetrying(false)
    }
  }

  const getTroubleshootingSteps = () => [
    {
      step: 1,
      title: 'Check your internet connection',
      description: 'Ensure your WiFi or mobile data is enabled and working',
      status: isOnline ? 'success' : 'error'
    },
    {
      step: 2,
      title: 'Restart your router/modem',
      description: 'Unplug for 30 seconds, then plug back in',
      status: 'pending'
    },
    {
      step: 3,
      title: 'Try a different network',
      description: 'Switch to mobile data or another WiFi network',
      status: 'pending'
    },
    {
      step: 4,
      title: 'Clear browser cache',
      description: 'Clear cookies and site data for trademaster.com',
      status: 'pending'
    }
  ]

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <FloatingShapes />
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-red-500/10 to-orange-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-orange-500/10 to-yellow-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative z-10 max-w-2xl mx-auto text-center animate-fade-up">
        {/* Network Status Icon */}
        <div className={`flex items-center justify-center w-24 h-24 mb-8 mx-auto glass-card rounded-3xl ${
          isOnline ? 'border-green-500/50' : 'border-red-500/50'
        }`}>
          {isOnline ? (
            <Wifi className="w-12 h-12 text-green-400" />
          ) : (
            <WifiOff className="w-12 h-12 text-red-400" />
          )}
        </div>

        {/* Error Content */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold gradient-text mb-4">
            {isOnline ? 'Server Unreachable' : 'No Internet Connection'}
          </h1>
          <p className="text-slate-400 text-lg mb-6">
            {isOnline 
              ? 'Unable to connect to TradeMaster servers. Please check your connection or try again later.'
              : 'You appear to be offline. Check your internet connection and try again.'
            }
          </p>
        </div>

        {/* Connection Status */}
        <div className="glass-card rounded-2xl p-6 mb-8">
          <div className="flex items-center justify-center space-x-2 mb-4">
            <Globe className="w-6 h-6 text-cyan-400" />
            <h3 className="text-lg font-semibold text-white">Connection Status</h3>
          </div>
          
          <div className="grid gap-3 text-sm">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-green-400' : 'bg-red-400'}`} />
                <span className="text-white">Internet Connection</span>
              </div>
              <span className={`font-medium ${isOnline ? 'text-green-400' : 'text-red-400'}`}>
                {isOnline ? 'Connected' : 'Disconnected'}
              </span>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-red-400" />
                <span className="text-white">TradeMaster Servers</span>
              </div>
              <span className="text-red-400 font-medium">Unreachable</span>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-yellow-400" />
                <span className="text-white">Retry Attempts</span>
              </div>
              <span className="text-yellow-400 font-medium">{retryAttempts}</span>
            </div>
          </div>
        </div>

        {/* Troubleshooting Steps */}
        <div className="glass-card rounded-2xl p-6 mb-8 text-left">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
            <Zap className="w-5 h-5 mr-2 text-orange-400" />
            Troubleshooting Steps
          </h3>
          <div className="space-y-4">
            {getTroubleshootingSteps().map((item) => (
              <div key={item.step} className="flex items-start space-x-4">
                <div className={`flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold ${
                  item.status === 'success' ? 'bg-green-500/20 text-green-400' :
                  item.status === 'error' ? 'bg-red-500/20 text-red-400' :
                  'bg-slate-500/20 text-slate-400'
                }`}>
                  {item.status === 'success' ? (
                    <CheckCircle className="w-3 h-3" />
                  ) : item.status === 'error' ? (
                    <AlertTriangle className="w-3 h-3" />
                  ) : (
                    item.step
                  )}
                </div>
                <div className="flex-1">
                  <div className="text-white font-medium">{item.title}</div>
                  <div className="text-sm text-slate-400">{item.description}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-8">
          <button
            onClick={handleRetry}
            disabled={isRetrying}
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw className={`w-4 h-4 ${isRetrying ? 'animate-spin' : ''}`} />
            <span>{isRetrying ? 'Testing Connection...' : 'Retry Connection'}</span>
          </button>
          
          {isOnline && (
            <Link
              to="/dashboard"
              className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
            >
              <Globe className="w-4 h-4" />
              <span>Try Dashboard</span>
            </Link>
          )}
        </div>

        {/* Offline Features */}
        {!isOnline && (
          <div className="glass-card rounded-2xl p-6 bg-slate-800/30 mb-8">
            <h4 className="text-sm font-semibold text-white mb-3">Available Offline</h4>
            <div className="grid gap-3 text-sm text-slate-400">
              <div className="flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 text-green-400" />
                <span>View cached portfolio data</span>
              </div>
              <div className="flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 text-green-400" />
                <span>Access trading history</span>
              </div>
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
                <span>Real-time data unavailable</span>
              </div>
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
                <span>Trading disabled until online</span>
              </div>
            </div>
          </div>
        )}

        {/* Contact Information */}
        <div className="text-center">
          <p className="text-slate-500 text-sm mb-2">
            Still having trouble? Contact our technical support team.
          </p>
          <div className="flex justify-center space-x-4 text-sm">
            <a href="mailto:tech@trademaster.com" className="text-purple-400 hover:text-purple-300">
              tech@trademaster.com
            </a>
            <span className="text-slate-600">•</span>
            <a href="tel:+911800123456" className="text-cyan-400 hover:text-cyan-300">
              1800-123-456
            </a>
          </div>
        </div>
      </div>
    </div>
  )
}