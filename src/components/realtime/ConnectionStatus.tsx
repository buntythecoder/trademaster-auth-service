import React from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Wifi, 
  WifiOff, 
  Loader2, 
  AlertCircle, 
  CheckCircle,
  Clock,
  Activity
} from 'lucide-react'
import { useConnectionStatus } from '@/hooks/useWebSocket'
import { cn } from '@/lib/utils'

interface ConnectionStatusProps {
  className?: string
  showDetails?: boolean
  size?: 'sm' | 'md' | 'lg'
}

export const ConnectionStatus: React.FC<ConnectionStatusProps> = ({
  className = '',
  showDetails = false,
  size = 'md'
}) => {
  const {
    connectionStatus,
    lastUpdate,
    error,
    getStatusColor,
    getStatusIcon,
    isConnected,
    isReconnecting
  } = useConnectionStatus()

  const getStatusMessage = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return 'Live market data connected'
      case 'RECONNECTING':
        return 'Reconnecting to live data...'
      case 'DISCONNECTED':
        return error || 'Market data disconnected'
      default:
        return 'Checking connection...'
    }
  }

  const getIcon = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return <CheckCircle className={cn('text-green-400', getSizeClass())} />
      case 'RECONNECTING':
        return <Loader2 className={cn('text-yellow-400 animate-spin', getSizeClass())} />
      case 'DISCONNECTED':
        return <AlertCircle className={cn('text-red-400', getSizeClass())} />
      default:
        return <Wifi className={cn('text-gray-400', getSizeClass())} />
    }
  }

  const getSizeClass = () => {
    switch (size) {
      case 'sm': return 'w-3 h-3'
      case 'md': return 'w-4 h-4'
      case 'lg': return 'w-5 h-5'
      default: return 'w-4 h-4'
    }
  }

  const getContainerSizeClass = () => {
    switch (size) {
      case 'sm': return 'px-2 py-1 text-xs'
      case 'md': return 'px-3 py-2 text-sm'
      case 'lg': return 'px-4 py-3 text-base'
      default: return 'px-3 py-2 text-sm'
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className={cn(
        'relative inline-flex items-center gap-2 rounded-lg border transition-all duration-300',
        getContainerSizeClass(),
        isConnected 
          ? 'bg-green-400/10 border-green-400/30 text-green-400' 
          : isReconnecting
          ? 'bg-yellow-400/10 border-yellow-400/30 text-yellow-400'
          : 'bg-red-400/10 border-red-400/30 text-red-400',
        className
      )}
    >
      <div className="flex items-center gap-2">
        {getIcon()}
        
        <div className="flex items-center gap-1">
          <span className="font-medium">
            {connectionStatus.charAt(0) + connectionStatus.slice(1).toLowerCase()}
          </span>
          {isConnected && (
            <motion.div
              animate={{ scale: [1, 1.2, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <Activity className={cn('text-green-400', getSizeClass())} />
            </motion.div>
          )}
        </div>
      </div>

      {showDetails && (
        <AnimatePresence>
          <motion.div
            initial={{ opacity: 0, width: 0 }}
            animate={{ opacity: 1, width: 'auto' }}
            exit={{ opacity: 0, width: 0 }}
            className="flex items-center gap-2 ml-2 pl-2 border-l border-current/30"
          >
            {lastUpdate && (
              <div className="flex items-center gap-1 text-xs opacity-75">
                <Clock className="w-3 h-3" />
                <span>
                  {lastUpdate.toLocaleTimeString([], { 
                    hour12: false, 
                    hour: '2-digit', 
                    minute: '2-digit',
                    second: '2-digit'
                  })}
                </span>
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      )}

      {/* Pulse animation for active connection */}
      {isConnected && (
        <motion.div
          className="absolute inset-0 rounded-lg bg-green-400/20"
          animate={{
            opacity: [0.5, 0.8, 0.5],
            scale: [1, 1.05, 1]
          }}
          transition={{
            duration: 3,
            repeat: Infinity,
            ease: "easeInOut"
          }}
        />
      )}
    </motion.div>
  )
}

// Compact version for headers/navigation
export const ConnectionIndicator: React.FC<{ className?: string }> = ({ className = '' }) => {
  const { connectionStatus, isConnected } = useConnectionStatus()

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <div className={cn(
        'w-2 h-2 rounded-full transition-colors duration-300',
        isConnected ? 'bg-green-400' : 'bg-red-400'
      )}>
        {isConnected && (
          <motion.div
            className="w-2 h-2 rounded-full bg-green-400/50"
            animate={{ scale: [1, 1.5, 1], opacity: [1, 0, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
        )}
      </div>
      <span className="text-xs text-slate-400">
        {connectionStatus === 'CONNECTED' ? 'Live' : 'Offline'}
      </span>
    </div>
  )
}

export default ConnectionStatus