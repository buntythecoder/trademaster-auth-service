import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShieldX, ArrowLeft, Home, AlertTriangle, Lock } from 'lucide-react'
import { FloatingShapes } from '../effects/FloatingShapes'
import { useAuthStore } from '../../stores/auth.store'

export function ForbiddenPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <FloatingShapes />
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-red-500/10 to-orange-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-orange-500/10 to-yellow-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      <div className="relative z-10 max-w-xl mx-auto text-center animate-fade-up">
        {/* Error Icon */}
        <div className="flex items-center justify-center w-24 h-24 mb-8 mx-auto glass-card rounded-3xl">
          <ShieldX className="w-12 h-12 text-orange-400" />
        </div>

        {/* Error Content */}
        <div className="mb-8">
          <h1 className="text-6xl font-bold gradient-text mb-4">403</h1>
          <h2 className="text-3xl font-bold text-white mb-4">Access Forbidden</h2>
          <p className="text-slate-400 text-lg mb-6">
            You don't have permission to access this resource. Your current account level doesn't allow this action.
          </p>
        </div>

        {/* User Info */}
        {user && (
          <div className="glass-card rounded-2xl p-6 mb-8">
            <div className="flex items-center justify-center space-x-3 mb-4">
              <div className="p-2 rounded-xl bg-purple-500/20">
                <Lock className="w-5 h-5 text-purple-400" />
              </div>
              <div>
                <div className="text-white font-semibold">
                  {user.firstName} {user.lastName}
                </div>
                <div className="text-sm text-slate-400">
                  Role: {user.role} • Status: {user.kycStatus || 'Pending'}
                </div>
              </div>
            </div>
            
            <div className="text-sm text-slate-400 space-y-2">
              <p>Current permissions may be limited due to:</p>
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
                <span>KYC verification incomplete</span>
              </div>
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
                <span>Account role restrictions</span>
              </div>
            </div>
          </div>
        )}

        {/* Suggested Actions */}
        <div className="glass-card rounded-2xl p-6 mb-8">
          <h3 className="text-lg font-semibold text-white mb-4">What can you do?</h3>
          <div className="space-y-3 text-sm">
            <Link
              to="/profile"
              className="flex items-center space-x-3 p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors"
            >
              <div className="p-1.5 rounded-lg bg-cyan-500/20">
                <Lock className="w-4 h-4 text-cyan-400" />
              </div>
              <div className="text-left">
                <div className="text-white font-medium">Complete KYC Verification</div>
                <div className="text-slate-400">Upload required documents to unlock features</div>
              </div>
            </Link>
            
            <Link
              to="/dashboard"
              className="flex items-center space-x-3 p-3 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors"
            >
              <div className="p-1.5 rounded-lg bg-purple-500/20">
                <Home className="w-4 h-4 text-purple-400" />
              </div>
              <div className="text-left">
                <div className="text-white font-medium">Return to Dashboard</div>
                <div className="text-slate-400">Access available features</div>
              </div>
            </Link>

            <div className="flex items-center space-x-3 p-3 rounded-xl bg-slate-800/30">
              <div className="p-1.5 rounded-lg bg-yellow-500/20">
                <AlertTriangle className="w-4 h-4 text-yellow-400" />
              </div>
              <div className="text-left">
                <div className="text-white font-medium">Contact Support</div>
                <div className="text-slate-400">Get help with account permissions</div>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-8">
          <Link
            to="/dashboard"
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2"
          >
            <Home className="w-4 h-4" />
            <span>Go to Dashboard</span>
          </Link>
          
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Go Back</span>
          </button>
        </div>

        {/* Contact Info */}
        <div className="text-center">
          <p className="text-slate-500 text-sm">
            Need help? Contact support at{' '}
            <a href="mailto:support@trademaster.com" className="text-purple-400 hover:text-purple-300">
              support@trademaster.com
            </a>
          </p>
        </div>
      </div>
    </div>
  )
}