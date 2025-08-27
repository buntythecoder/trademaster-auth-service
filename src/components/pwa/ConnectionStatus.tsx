import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Wifi, WifiOff, AlertTriangle, CheckCircle, Clock } from 'lucide-react'
import { usePWA } from '@/hooks/usePWA'

interface ConnectionStatusProps {
  className?: string
  showDetails?: boolean
}

export const ConnectionStatus: React.FC<ConnectionStatusProps> = ({ 
  className = '', 
  showDetails = false 
}) => {
  const { isOffline } = usePWA()
  const [showOfflineMessage, setShowOfflineMessage] = useState(false)
  const [queuedOperations, setQueuedOperations] = useState(0)

  // Show offline message when going offline
  useEffect(() => {
    if (isOffline) {
      setShowOfflineMessage(true)
      
      // Auto-hide after 10 seconds
      const timer = setTimeout(() => {
        setShowOfflineMessage(false)
      }, 10000)
      
      return () => clearTimeout(timer)
    } else {
      setShowOfflineMessage(false)
    }
  }, [isOffline])

  // Mock queued operations (in real app, this would come from service worker)
  useEffect(() => {
    if (isOffline) {
      // Simulate queued operations
      setQueuedOperations(prev => prev + Math.floor(Math.random() * 2))
    } else {
      // Clear queue when online
      if (queuedOperations > 0) {
        setQueuedOperations(0)
      }
    }
  }, [isOffline])

  // Compact status indicator
  if (!showDetails) {
    return (
      <motion.div
        className={`flex items-center space-x-2 ${className}`}
        animate={isOffline ? { opacity: [1, 0.6, 1] } : { opacity: 1 }}
        transition={isOffline ? { duration: 2, repeat: Infinity } : {}}
      >
        <div className={`w-2 h-2 rounded-full ${
          isOffline ? 'bg-red-400' : 'bg-green-400'
        }`} />
        
        <span className={`text-xs font-medium ${
          isOffline ? 'text-red-400' : 'text-green-400'
        }`}>
          {isOffline ? 'Offline' : 'Online'}
        </span>
        
        {queuedOperations > 0 && (
          <span className="text-xs text-yellow-400">
            ({queuedOperations} queued)
          </span>
        )}
      </motion.div>
    )
  }

  // Detailed offline message
  return (
    <AnimatePresence>
      {showOfflineMessage && (
        <motion.div
          initial={{ opacity: 0, y: -50, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -50, scale: 0.9 }}
          className={`fixed top-20 left-4 right-4 z-40 ${className}`}
        >
          <div className="glass-card rounded-2xl p-4 border border-orange-400/30 shadow-2xl shadow-orange-600/25 max-w-md mx-auto">
            <div className="flex items-start space-x-4">
              {/* Status Icon */}
              <div className="flex-shrink-0 mt-0.5">
                <div className="w-10 h-10 rounded-full bg-orange-500/20 flex items-center justify-center">
                  <WifiOff className="w-5 h-5 text-orange-400" />
                </div>
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center space-x-2 mb-2">
                  <h3 className="font-semibold text-white text-sm">
                    You're offline
                  </h3>
                  <motion.div
                    animate={{ opacity: [1, 0.3, 1] }}
                    transition={{ duration: 2, repeat: Infinity }}
                    className="w-2 h-2 rounded-full bg-orange-400"
                  />
                </div>
                
                <p className="text-xs text-slate-400 mb-3">
                  No internet connection detected. TradeMaster is running in offline mode 
                  with cached data and queued operations.
                </p>

                {/* Features available offline */}
                <div className="space-y-2">
                  <div className="flex items-center space-x-2 text-xs">
                    <CheckCircle className="w-3 h-3 text-green-400" />
                    <span className="text-slate-300">View cached portfolio data</span>
                  </div>
                  
                  <div className="flex items-center space-x-2 text-xs">
                    <CheckCircle className="w-3 h-3 text-green-400" />
                    <span className="text-slate-300">Browse order history</span>
                  </div>
                  
                  <div className="flex items-center space-x-2 text-xs">
                    <Clock className="w-3 h-3 text-yellow-400" />
                    <span className="text-slate-300">Queue new trading orders</span>
                  </div>
                </div>

                {/* Queued operations */}
                {queuedOperations > 0 && (
                  <div className="mt-3 p-2 bg-yellow-500/10 border border-yellow-500/20 rounded-lg">
                    <div className="flex items-center space-x-2">
                      <AlertTriangle className="w-3 h-3 text-yellow-400" />
                      <span className="text-xs text-yellow-400 font-medium">
                        {queuedOperations} operations queued
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">
                      Will be processed when connection is restored
                    </p>
                  </div>
                )}
              </div>

              {/* Dismiss button */}
              <button
                onClick={() => setShowOfflineMessage(false)}
                className="flex-shrink-0 p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors"
              >
                ×
              </button>
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

// Connection restored notification
export const ConnectionRestoredNotification: React.FC = () => {
  const { isOffline } = usePWA()
  const [showRestored, setShowRestored] = useState(false)
  const [wasOffline, setWasOffline] = useState(isOffline)

  useEffect(() => {
    if (wasOffline && !isOffline) {
      // Connection restored
      setShowRestored(true)
      
      // Auto-hide after 5 seconds
      const timer = setTimeout(() => {
        setShowRestored(false)
      }, 5000)
      
      return () => clearTimeout(timer)
    }
    
    setWasOffline(isOffline)
  }, [isOffline, wasOffline])

  return (
    <AnimatePresence>
      {showRestored && (
        <motion.div
          initial={{ opacity: 0, y: 50, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 50, scale: 0.9 }}
          className="fixed bottom-6 right-6 z-50"
        >
          <div className="glass-card rounded-2xl p-4 border border-green-400/30 shadow-2xl shadow-green-600/25 max-w-sm">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center">
                <Wifi className="w-5 h-5 text-green-400" />
              </div>
              
              <div className="flex-1">
                <h3 className="font-semibold text-white text-sm">
                  Connection restored
                </h3>
                <p className="text-xs text-slate-400">
                  Processing queued operations...
                </p>
              </div>
              
              <motion.div
                animate={{ scale: [1, 1.2, 1] }}
                transition={{ duration: 1, repeat: 2 }}
                className="w-2 h-2 rounded-full bg-green-400"
              />
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

export default ConnectionStatus