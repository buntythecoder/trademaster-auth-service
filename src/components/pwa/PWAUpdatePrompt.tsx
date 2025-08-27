import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { RefreshCw, AlertCircle, CheckCircle, X } from 'lucide-react'
import { usePWA } from '@/hooks/usePWA'

interface PWAUpdatePromptProps {
  className?: string
}

export const PWAUpdatePrompt: React.FC<PWAUpdatePromptProps> = ({ 
  className = '' 
}) => {
  const { isUpdateAvailable, updateApp } = usePWA()
  const [isUpdating, setIsUpdating] = useState(false)
  const [showPrompt, setShowPrompt] = useState(true)

  const handleUpdate = async () => {
    setIsUpdating(true)
    try {
      await updateApp()
      // App will reload automatically after update
    } catch (error) {
      console.error('Update failed:', error)
      setIsUpdating(false)
    }
  }

  const dismissPrompt = () => {
    setShowPrompt(false)
  }

  // Don't show if no update available or dismissed
  if (!isUpdateAvailable || !showPrompt) {
    return null
  }

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: -100 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -100 }}
        className={`fixed top-6 left-6 right-6 z-50 ${className}`}
      >
        <div className="glass-card rounded-2xl p-4 border border-blue-400/30 shadow-2xl shadow-blue-600/25 max-w-md mx-auto">
          <div className="flex items-center space-x-4">
            {/* Icon */}
            <div className="flex-shrink-0">
              {isUpdating ? (
                <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center">
                  <RefreshCw className="w-5 h-5 text-blue-400 animate-spin" />
                </div>
              ) : (
                <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center">
                  <AlertCircle className="w-5 h-5 text-blue-400" />
                </div>
              )}
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-white text-sm">
                {isUpdating ? 'Updating TradeMaster...' : 'Update Available'}
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                {isUpdating 
                  ? 'Please wait while we update the app with new features and improvements.'
                  : 'A new version of TradeMaster is available with improvements and bug fixes.'
                }
              </p>
            </div>

            {/* Actions */}
            {!isUpdating && (
              <div className="flex items-center space-x-2">
                <button
                  onClick={handleUpdate}
                  className="cyber-button-sm px-3 py-2 rounded-lg font-medium text-white text-xs hover:scale-105 transition-all duration-300"
                  disabled={isUpdating}
                >
                  Update Now
                </button>
                
                <button
                  onClick={dismissPrompt}
                  className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* Loading state */}
            {isUpdating && (
              <div className="text-blue-400 text-xs">
                Updating...
              </div>
            )}
          </div>

          {/* Progress bar for updating state */}
          {isUpdating && (
            <div className="mt-3">
              <div className="w-full bg-slate-700/50 rounded-full h-1.5">
                <div className="bg-gradient-to-r from-blue-400 to-cyan-400 h-1.5 rounded-full animate-pulse" 
                     style={{ width: '100%' }}></div>
              </div>
            </div>
          )}
        </div>
      </motion.div>
    </AnimatePresence>
  )
}

export default PWAUpdatePrompt