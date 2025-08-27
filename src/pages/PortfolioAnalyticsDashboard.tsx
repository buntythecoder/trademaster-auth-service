import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { usePortfolioWebSocket } from '../hooks/usePortfolioWebSocket'
import { useToast } from '../contexts/ToastContext'

// Import enhanced portfolio analytics
import { EnhancedPortfolioAnalytics } from '../components/portfolio/EnhancedPortfolioAnalytics'

export function PortfolioAnalyticsDashboard() {
  const { portfolio, isConnected, lastUpdate } = usePortfolioWebSocket()
  const { info } = useToast()
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (portfolio) {
      setLoading(false)
    }
  }, [portfolio])

  useEffect(() => {
    if (isConnected && !loading) {
      info('Portfolio Connected', 'Real-time portfolio updates are now active')
    }
  }, [isConnected, loading, info])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="flex flex-col items-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-400"></div>
          <p className="text-slate-400">Loading portfolio analytics...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-slate-950 p-4 md:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        {/* Enhanced Portfolio Analytics */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <EnhancedPortfolioAnalytics />
        </motion.div>

        {/* Connection Status */}
        {!isConnected && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed bottom-4 right-4 bg-red-500/20 border border-red-500/30 rounded-xl p-4 backdrop-blur-sm"
          >
            <div className="flex items-center space-x-2 text-red-400">
              <div className="w-2 h-2 rounded-full bg-red-400 animate-pulse"></div>
              <span className="text-sm">Portfolio service unavailable</span>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  )
}

export default PortfolioAnalyticsDashboard