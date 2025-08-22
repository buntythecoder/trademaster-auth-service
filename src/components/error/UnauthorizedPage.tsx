import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Lock, ArrowLeft, LogIn, Shield, AlertTriangle } from 'lucide-react'
import { FloatingShapes } from '../effects/FloatingShapes'

export function UnauthorizedPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <FloatingShapes />
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-red-500/10 to-pink-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-yellow-500/10 to-red-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative z-10 max-w-xl mx-auto text-center animate-fade-up">
        {/* Error Icon */}
        <div className="flex items-center justify-center w-24 h-24 mb-8 mx-auto glass-card rounded-3xl">
          <Lock className="w-12 h-12 text-red-400" />
        </div>

        {/* Error Content */}
        <div className="mb-8">
          <h1 className="text-6xl font-bold gradient-text mb-4">401</h1>
          <h2 className="text-3xl font-bold text-white mb-4">Unauthorized Access</h2>
          <p className="text-slate-400 text-lg mb-6">
            You need to be logged in to access this page. Please sign in to continue your trading journey.
          </p>
        </div>

        {/* Security Info */}
        <div className="glass-card rounded-2xl p-6 mb-8">
          <div className="flex items-center justify-center space-x-2 mb-4">
            <Shield className="w-6 h-6 text-cyan-400" />
            <h3 className="text-lg font-semibold text-white">Secure Trading Platform</h3>
          </div>
          <div className="grid gap-4 text-sm text-slate-400">
            <div className="flex items-center space-x-3">
              <div className="w-2 h-2 rounded-full bg-green-400" />
              <span>256-bit SSL encryption</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-2 h-2 rounded-full bg-green-400" />
              <span>Two-factor authentication</span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="w-2 h-2 rounded-full bg-green-400" />
              <span>SEBI compliant security</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-8">
          <Link
            to="/login"
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2"
          >
            <LogIn className="w-4 h-4" />
            <span>Sign In</span>
          </Link>
          
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Go Back</span>
          </button>
        </div>

        {/* Demo Credentials */}
        <div className="glass-card rounded-2xl p-6 bg-slate-800/30">
          <h4 className="text-sm font-semibold text-white mb-3">Demo Access</h4>
          <div className="grid gap-3 text-sm">
            <div className="flex justify-between items-center p-2 rounded-lg bg-slate-700/50">
              <div>
                <div className="text-orange-400 font-medium">Admin</div>
                <div className="text-slate-400">admin@trademaster.com</div>
              </div>
              <div className="text-slate-400 font-mono">admin123</div>
            </div>
            <div className="flex justify-between items-center p-2 rounded-lg bg-slate-700/50">
              <div>
                <div className="text-blue-400 font-medium">Trader</div>
                <div className="text-slate-400">trader@trademaster.com</div>
              </div>
              <div className="text-slate-400 font-mono">trader123</div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-8">
          <p className="text-slate-500 text-sm">
            New to TradeMaster? {' '}
            <Link to="/register" className="text-purple-400 hover:text-purple-300 font-medium">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}