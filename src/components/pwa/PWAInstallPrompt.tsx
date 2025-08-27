import React from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Download, X, Smartphone, Zap, Shield, Wifi } from 'lucide-react'
import { usePWA } from '@/hooks/usePWA'

interface PWAInstallPromptProps {
  className?: string
}

export const PWAInstallPrompt: React.FC<PWAInstallPromptProps> = ({ 
  className = '' 
}) => {
  const { 
    showInstallPrompt, 
    isInstalled, 
    installApp, 
    dismissInstallPrompt 
  } = usePWA()

  const handleInstall = async () => {
    const success = await installApp()
    if (success) {
      console.log('App installed successfully')
    }
  }

  // Don't show if already installed or not installable
  if (isInstalled || !showInstallPrompt) {
    return null
  }

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: 100 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 100 }}
        className={`fixed bottom-6 left-6 right-6 z-50 ${className}`}
      >
        <div className="glass-card rounded-2xl p-6 border border-purple-400/30 shadow-2xl shadow-purple-600/25 max-w-md mx-auto">
          {/* Header */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-r from-purple-600 to-cyan-500 flex items-center justify-center">
                <Download className="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 className="font-bold text-white">Install TradeMaster</h3>
                <p className="text-sm text-slate-400">Get the full app experience</p>
              </div>
            </div>
            
            <button
              onClick={dismissInstallPrompt}
              className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Features */}
          <div className="space-y-3 mb-6">
            <div className="flex items-center space-x-3 text-sm">
              <Zap className="w-4 h-4 text-yellow-400" />
              <span className="text-slate-300">Lightning-fast performance</span>
            </div>
            
            <div className="flex items-center space-x-3 text-sm">
              <Wifi className="w-4 h-4 text-green-400" />
              <span className="text-slate-300">Works offline with cached data</span>
            </div>
            
            <div className="flex items-center space-x-3 text-sm">
              <Shield className="w-4 h-4 text-blue-400" />
              <span className="text-slate-300">Secure offline trading orders</span>
            </div>
            
            <div className="flex items-center space-x-3 text-sm">
              <Smartphone className="w-4 h-4 text-purple-400" />
              <span className="text-slate-300">Native app-like experience</span>
            </div>
          </div>

          {/* Actions */}
          <div className="flex space-x-3">
            <button
              onClick={handleInstall}
              className="flex-1 cyber-button px-6 py-3 rounded-xl font-semibold text-white transition-all duration-300 flex items-center justify-center space-x-2 hover:scale-105"
            >
              <Download className="w-4 h-4" />
              <span>Install App</span>
            </button>
            
            <button
              onClick={dismissInstallPrompt}
              className="px-6 py-3 rounded-xl font-medium text-slate-400 hover:text-white hover:bg-slate-700/50 transition-all duration-300"
            >
              Maybe Later
            </button>
          </div>

          {/* Privacy note */}
          <p className="text-xs text-slate-500 mt-4 text-center">
            No personal data is shared during installation. 
            <br />
            App can be uninstalled anytime from your device settings.
          </p>
        </div>
      </motion.div>
    </AnimatePresence>
  )
}

export default PWAInstallPrompt