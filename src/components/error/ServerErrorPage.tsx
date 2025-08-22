import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Server, ArrowLeft, Home, RefreshCw, AlertTriangle, Zap, Clock } from 'lucide-react'
import { FloatingShapes } from '../effects/FloatingShapes'
import { useToast } from '../../contexts/ToastContext'

export function ServerErrorPage() {
  const navigate = useNavigate()
  const { info } = useToast()
  const [isRefreshing, setIsRefreshing] = useState(false)

  const handleRefresh = async () => {
    setIsRefreshing(true)
    info('Refreshing page...', 'Attempting to reconnect to server')
    
    setTimeout(() => {
      setIsRefreshing(false)
      window.location.reload()
    }, 2000)
  }

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <FloatingShapes />
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-red-500/10 to-pink-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-orange-500/10 to-red-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative z-10 max-w-2xl mx-auto text-center animate-fade-up">
        {/* Error Icon */}
        <div className="flex items-center justify-center w-24 h-24 mb-8 mx-auto glass-card rounded-3xl">
          <Server className="w-12 h-12 text-red-400" />
        </div>

        {/* Error Content */}
        <div className="mb-8">
          <h1 className="text-6xl font-bold gradient-text mb-4">500</h1>
          <h2 className="text-3xl font-bold text-white mb-4">Server Error</h2>
          <p className="text-slate-400 text-lg mb-6">
            Oops! Something went wrong on our servers. Our team has been notified and is working to fix this issue.
          </p>
        </div>

        {/* Status Information */}
        <div className="glass-card rounded-2xl p-6 mb-8">
          <div className="flex items-center justify-center space-x-2 mb-4">
            <AlertTriangle className="w-6 h-6 text-yellow-400" />
            <h3 className="text-lg font-semibold text-white">System Status</h3>
          </div>
          
          <div className="grid gap-4 md:grid-cols-2 text-sm">
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-green-400" />
                <span className="text-white">Trading Platform</span>
              </div>
              <span className="text-green-400 font-medium">Operational</span>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-green-400" />
                <span className="text-white">Market Data</span>
              </div>
              <span className="text-green-400 font-medium">Operational</span>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-red-400 animate-pulse" />
                <span className="text-white">API Services</span>
              </div>
              <span className="text-red-400 font-medium">Degraded</span>
            </div>
            
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/30">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 rounded-full bg-yellow-400" />
                <span className="text-white">User Authentication</span>
              </div>
              <span className="text-yellow-400 font-medium">Monitoring</span>
            </div>
          </div>
        </div>

        {/* What happened */}
        <div className="glass-card rounded-2xl p-6 mb-8 text-left">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
            <Zap className="w-5 h-5 mr-2 text-orange-400" />
            What happened?
          </h3>
          <div className="space-y-3 text-sm text-slate-400">
            <div className="flex items-start space-x-3">
              <Clock className="w-4 h-4 text-cyan-400 mt-0.5 flex-shrink-0" />
              <div>
                <div className="text-white font-medium">Server Overload</div>
                <div>High traffic volume may have caused temporary service disruption</div>
              </div>
            </div>
            <div className="flex items-start space-x-3">
              <Clock className="w-4 h-4 text-purple-400 mt-0.5 flex-shrink-0" />
              <div>
                <div className="text-white font-medium">Database Connection</div>
                <div>Temporary database connectivity issues detected</div>
              </div>
            </div>
            <div className="flex items-start space-x-3">
              <Clock className="w-4 h-4 text-green-400 mt-0.5 flex-shrink-0" />
              <div>
                <div className="text-white font-medium">Recovery Process</div>
                <div>Auto-recovery systems are actively working to restore normal service</div>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-8">
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>{isRefreshing ? 'Refreshing...' : 'Try Again'}</span>
          </button>
          
          <Link
            to="/dashboard"
            className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
          >
            <Home className="w-4 h-4" />
            <span>Go to Dashboard</span>
          </Link>
          
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-cyan-300 transition-colors border border-cyan-500/50 hover:border-cyan-400/70"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Go Back</span>
          </button>
        </div>

        {/* Additional Info */}
        <div className="glass-card rounded-2xl p-6 bg-slate-800/30">
          <h4 className="text-sm font-semibold text-white mb-3">Need Immediate Help?</h4>
          <div className="grid gap-3 text-sm">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Support Email:</span>
              <a href="mailto:support@trademaster.com" className="text-purple-400 hover:text-purple-300">
                support@trademaster.com
              </a>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Emergency Hotline:</span>
              <a href="tel:+911800123456" className="text-cyan-400 hover:text-cyan-300">
                1800-123-456
              </a>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Incident ID:</span>
              <span className="text-white font-mono text-xs">INC-{Date.now().toString(36).toUpperCase()}</span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-8">
          <p className="text-slate-500 text-sm">
            We apologize for the inconvenience. Our engineering team is working to restore service as quickly as possible.
          </p>
        </div>
      </div>
    </div>
  )
}