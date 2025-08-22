import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Home, ArrowLeft, Search, TrendingUp, AlertTriangle } from 'lucide-react'
import { FloatingShapes } from '../effects/FloatingShapes'

export function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen hero-gradient flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <FloatingShapes />
      <div className="absolute inset-0">
        <div className="absolute top-20 left-10 w-32 h-32 bg-gradient-to-br from-red-500/10 to-pink-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-10 w-40 h-40 bg-gradient-to-br from-orange-500/10 to-red-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />
        <div className="absolute top-1/2 left-1/4 w-24 h-24 bg-gradient-to-br from-yellow-500/10 to-orange-500/10 rounded-full blur-2xl animate-pulse" style={{ animationDelay: '4s' }} />
      </div>

      <div className="relative z-10 max-w-2xl mx-auto text-center animate-fade-up">
        {/* Error Icon */}
        <div className="flex items-center justify-center w-24 h-24 mb-8 mx-auto glass-card rounded-3xl">
          <AlertTriangle className="w-12 h-12 text-orange-400" />
        </div>

        {/* Error Code */}
        <div className="mb-6">
          <h1 className="text-8xl font-bold gradient-text mb-4">404</h1>
          <h2 className="text-3xl font-bold text-white mb-2">Page Not Found</h2>
          <p className="text-slate-400 text-lg max-w-md mx-auto">
            Looks like you've wandered into uncharted territory. The page you're looking for doesn't exist in our trading universe.
          </p>
        </div>

        {/* Helpful Links */}
        <div className="glass-card rounded-2xl p-8 mb-8">
          <h3 className="text-lg font-semibold text-white mb-4">Where would you like to go?</h3>
          <div className="grid gap-4 md:grid-cols-2">
            <Link
              to="/dashboard"
              className="flex items-center space-x-3 p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group"
            >
              <div className="p-2 rounded-xl bg-purple-500/20 group-hover:bg-purple-500/30 transition-colors">
                <TrendingUp className="w-5 h-5 text-purple-400" />
              </div>
              <div className="text-left">
                <div className="font-semibold text-white">Trading Dashboard</div>
                <div className="text-sm text-slate-400">View your portfolio and trades</div>
              </div>
            </Link>
            
            <Link
              to="/market-data"
              className="flex items-center space-x-3 p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group"
            >
              <div className="p-2 rounded-xl bg-cyan-500/20 group-hover:bg-cyan-500/30 transition-colors">
                <Search className="w-5 h-5 text-cyan-400" />
              </div>
              <div className="text-left">
                <div className="font-semibold text-white">Market Data</div>
                <div className="text-sm text-slate-400">Real-time market information</div>
              </div>
            </Link>

            <Link
              to="/trading"
              className="flex items-center space-x-3 p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group"
            >
              <div className="p-2 rounded-xl bg-green-500/20 group-hover:bg-green-500/30 transition-colors">
                <TrendingUp className="w-5 h-5 text-green-400" />
              </div>
              <div className="text-left">
                <div className="font-semibold text-white">Trading Interface</div>
                <div className="text-sm text-slate-400">Execute buy and sell orders</div>
              </div>
            </Link>

            <Link
              to="/portfolio"
              className="flex items-center space-x-3 p-4 rounded-xl bg-slate-800/30 hover:bg-slate-700/50 transition-colors group"
            >
              <div className="p-2 rounded-xl bg-orange-500/20 group-hover:bg-orange-500/30 transition-colors">
                <TrendingUp className="w-5 h-5 text-orange-400" />
              </div>
              <div className="text-left">
                <div className="font-semibold text-white">Portfolio Analytics</div>
                <div className="text-sm text-slate-400">Detailed performance analysis</div>
              </div>
            </Link>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center justify-center space-x-2 glass-card px-6 py-3 rounded-xl font-semibold text-white hover:text-purple-300 transition-colors border border-purple-500/50 hover:border-purple-400/70"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Go Back</span>
          </button>
          
          <Link
            to="/"
            className="cyber-button px-6 py-3 rounded-xl font-semibold flex items-center justify-center space-x-2"
          >
            <Home className="w-4 h-4" />
            <span>Home</span>
          </Link>
        </div>

        {/* Help Text */}
        <div className="mt-8 text-center">
          <p className="text-slate-500 text-sm">
            If you believe this is an error, please contact our support team or try refreshing the page.
          </p>
        </div>
      </div>
    </div>
  )
}